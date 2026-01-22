package com.dstrLogAggr.Log_Aggregator.service;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import com.dstrLogAggr.Log_Aggregator.model.LogEntryDocument;
import com.dstrLogAggr.Log_Aggregator.repository.ElasticsearchLogRepository;
import com.dstrLogAggr.Log_Aggregator.repository.LogBackRepo;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced log persistence service with dual-write strategy.
 * Writes logs to both MongoDB (primary storage) and Elasticsearch
 * (search/analytics).
 * Includes comprehensive fault tolerance mechanisms.
 */
@Service
public class LogPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(LogPersistenceService.class);

    @Autowired
    private LogBackRepo mongoRepository;

    @Autowired
    private ElasticsearchLogRepository elasticsearchRepository;

    @Autowired
    private RedisTemplate<String, LogEntry> redisTemplate;

    /**
     * Save log entry to both MongoDB and Elasticsearch with fault tolerance
     */
    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @CircuitBreaker(name = "logPersistenceCB", fallbackMethod = "fallbackSaveLog")
    @Bulkhead(name = "logPersistenceBulkhead")
    public void saveLog(LogEntry logEntry) {
        try {
            // Primary storage: MongoDB
            mongoRepository.save(logEntry);
            logger.info("Log saved to MongoDB: {}", logEntry.getId());

            // Async write to Elasticsearch for search
            saveToElasticsearchAsync(logEntry);

            // Cache ERROR logs in Redis for quick access
            if ("ERROR".equalsIgnoreCase(logEntry.getLevel())) {
                cacheErrorLog(logEntry);
            }

        } catch (Exception e) {
            logger.error("Error saving log entry: {}", logEntry.getId(), e);
            throw e;
        }
    }

    /**
     * Asynchronously save to Elasticsearch to avoid blocking
     */
    @Async
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "fallbackElasticsearchSave")
    public CompletableFuture<Void> saveToElasticsearchAsync(LogEntry logEntry) {
        try {
            LogEntryDocument document = LogEntryDocument.fromLogEntry(logEntry);
            elasticsearchRepository.save(document);
            logger.info("Log indexed in Elasticsearch: {}", logEntry.getId());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Failed to index log in Elasticsearch: {}", logEntry.getId(), e);
            throw e;
        }
    }

    /**
     * Cache ERROR logs in Redis for quick access and alerting
     */
    private void cacheErrorLog(LogEntry logEntry) {
        try {
            String redisKey = "ERROR_LOG:" + logEntry.getId();
            redisTemplate.opsForValue().set(redisKey, logEntry, Duration.ofMinutes(30));
            logger.info("ERROR log cached in Redis: {}", logEntry.getId());
        } catch (Exception e) {
            logger.warn("Failed to cache ERROR log in Redis: {}", logEntry.getId(), e);
            // Don't throw - caching is not critical
        }
    }

    /**
     * Fallback when primary log persistence fails
     * Stores in Redis as temporary backup
     */
    private void fallbackSaveLog(LogEntry logEntry, Throwable ex) {
        logger.error("❗ Primary storage failed. Using fallback for log: {}", logEntry.getId(), ex);
        try {
            String fallbackKey = "FALLBACK_LOG:" + logEntry.getId();
            redisTemplate.opsForValue().set(fallbackKey, logEntry, Duration.ofHours(24));
            logger.info("Log saved to Redis fallback storage: {}", logEntry.getId());
        } catch (Exception redisEx) {
            logger.error("❌ Critical: Both primary and fallback storage failed for log: {}", logEntry.getId(), redisEx);
            // Last resort: could write to local file or send to DLQ
        }
    }

    /**
     * Fallback when Elasticsearch indexing fails
     */
    private CompletableFuture<Void> fallbackElasticsearchSave(LogEntry logEntry, Throwable ex) {
        logger.warn("⚠️ Elasticsearch indexing failed for log: {}. Log still available in MongoDB.", logEntry.getId(),
                ex);
        // Log is already in MongoDB, so search functionality is degraded but data is
        // safe
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Batch save for high-throughput scenarios
     */
    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @CircuitBreaker(name = "logPersistenceCB", fallbackMethod = "fallbackBatchSave")
    @Bulkhead(name = "logPersistenceBulkhead")
    public void batchSaveLogs(Iterable<LogEntry> logEntries) {
        try {
            // Save to MongoDB
            mongoRepository.saveAll(logEntries);
            logger.info("Batch saved logs to MongoDB");

            // Async batch index to Elasticsearch
            logEntries.forEach(this::saveToElasticsearchAsync);

        } catch (Exception e) {
            logger.error("Error in batch save operation", e);
            throw e;
        }
    }

    private void fallbackBatchSave(Iterable<LogEntry> logEntries, Throwable ex) {
        logger.error("❗ Batch save failed. Attempting individual saves", ex);
        logEntries.forEach(logEntry -> {
            try {
                fallbackSaveLog(logEntry, ex);
            } catch (Exception e) {
                logger.error("Failed to save log even in fallback: {}", logEntry.getId(), e);
            }
        });
    }
}
