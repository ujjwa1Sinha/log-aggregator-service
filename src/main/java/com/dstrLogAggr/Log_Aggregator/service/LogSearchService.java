package com.dstrLogAggr.Log_Aggregator.service;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import com.dstrLogAggr.Log_Aggregator.model.LogEntryDocument;
import com.dstrLogAggr.Log_Aggregator.repository.ElasticsearchLogRepository;
import com.dstrLogAggr.Log_Aggregator.repository.LogBackRepo;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for searching and analyzing logs using Elasticsearch with intelligent
 * fallback.
 * 
 * Search Strategy:
 * - ERROR logs: Redis (cache) → Elasticsearch → MongoDB (3-tier fallback)
 * - Other logs: Elasticsearch → MongoDB (2-tier fallback)
 * 
 * Includes fault tolerance mechanisms: Circuit Breaker, Rate Limiter, and
 * Bulkhead.
 */
@Service
public class LogSearchService {

    private static final Logger logger = LoggerFactory.getLogger(LogSearchService.class);
    private static final String REDIS_ERROR_LOG_PREFIX = "ERROR_LOG:";

    @Autowired
    private ElasticsearchLogRepository elasticsearchRepository;

    @Autowired
    private LogBackRepo mongoRepository;

    @Autowired
    private RedisTemplate<String, LogEntry> redisTemplate;

    /**
     * Search logs by service name with pagination
     */
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "fallbackSearchByService")
    @RateLimiter(name = "searchRateLimiter")
    @Bulkhead(name = "searchBulkhead")
    public Page<LogEntryDocument> searchByService(String service, int page, int size) {
        logger.info("Searching logs for service: {}", service);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return elasticsearchRepository.findByService(service, pageable);
    }

    /**
     * Search logs by level (INFO, ERROR, WARN, DEBUG)
     * For ERROR logs: Uses 3-tier fallback (Redis → Elasticsearch → MongoDB)
     * For other logs: Uses 2-tier fallback (Elasticsearch → MongoDB)
     */
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "fallbackSearchByLevel")
    @RateLimiter(name = "searchRateLimiter")
    @Bulkhead(name = "searchBulkhead")
    public Page<LogEntryDocument> searchByLevel(String level, int page, int size) {
        logger.info("Searching logs with level: {}", level);

        // For ERROR logs, try Redis cache first for faster retrieval
        if ("ERROR".equalsIgnoreCase(level)) {
            Page<LogEntryDocument> redisResults = searchErrorLogsInRedis(page, size);
            if (redisResults != null && redisResults.hasContent()) {
                logger.info("Found {} ERROR logs in Redis cache (fast path)", redisResults.getNumberOfElements());
                return redisResults;
            }
            logger.info("No ERROR logs in Redis cache, falling back to Elasticsearch");
        }

        // Standard Elasticsearch search for all levels (including ERROR if not in
        // Redis)
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return elasticsearchRepository.findByLevel(level, pageable);
    }

    /**
     * Search logs by trace ID for distributed tracing
     */
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "fallbackSearchByTraceId")
    @RateLimiter(name = "searchRateLimiter")
    @Bulkhead(name = "searchBulkhead")
    public List<LogEntryDocument> searchByTraceId(String traceId) {
        logger.info("Searching logs for trace ID: {}", traceId);
        return elasticsearchRepository.findByTraceId(traceId);
    }

    /**
     * Full-text search in log messages
     */
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "fallbackFullTextSearch")
    @RateLimiter(name = "searchRateLimiter")
    @Bulkhead(name = "searchBulkhead")
    public Page<LogEntryDocument> fullTextSearch(String searchTerm, int page, int size) {
        logger.info("Full-text search for: {}", searchTerm);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return elasticsearchRepository.searchByMessage(searchTerm, pageable);
    }

    /**
     * Search logs within a time range
     */
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "fallbackSearchByTimeRange")
    @RateLimiter(name = "searchRateLimiter")
    @Bulkhead(name = "searchBulkhead")
    public Page<LogEntryDocument> searchByTimeRange(Instant startTime, Instant endTime, int page, int size) {
        logger.info("Searching logs between {} and {}", startTime, endTime);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return elasticsearchRepository.findByTimestampBetween(startTime, endTime, pageable);
    }

    /**
     * Advanced search with multiple criteria
     */
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "fallbackAdvancedSearch")
    @RateLimiter(name = "searchRateLimiter")
    @Bulkhead(name = "searchBulkhead")
    public Page<LogEntryDocument> advancedSearch(String service, String level, Instant startTime, Instant endTime,
            int page, int size) {
        logger.info("Advanced search - Service: {}, Level: {}, Time range: {} to {}", service, level, startTime,
                endTime);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return elasticsearchRepository.advancedSearch(service, level, startTime, endTime, pageable);
    }

    /**
     * Search logs by service and level
     */
    @CircuitBreaker(name = "elasticsearchCB", fallbackMethod = "fallbackSearchByServiceAndLevel")
    @RateLimiter(name = "searchRateLimiter")
    @Bulkhead(name = "searchBulkhead")
    public Page<LogEntryDocument> searchByServiceAndLevel(String service, String level, int page, int size) {
        logger.info("Searching logs for service: {} with level: {}", service, level);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return elasticsearchRepository.findByServiceAndLevel(service, level, pageable);
    }

    // ==================== Fallback Methods ====================

    private Page<LogEntryDocument> fallbackSearchByService(String service, int page, int size, Throwable ex) {
        logger.warn("Elasticsearch unavailable for service search. Falling back to MongoDB. Service: {}", service);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<LogEntry> mongoResults = mongoRepository.findByService(service, pageable);
            return convertToDocumentPage(mongoResults);
        } catch (Exception mongoEx) {
            logger.error("MongoDB fallback also failed for service search", mongoEx);
            return Page.empty();
        }
    }

    private Page<LogEntryDocument> fallbackSearchByLevel(String level, int page, int size, Throwable ex) {
        logger.warn("Elasticsearch unavailable for level search. Level: {}", level);

        // For ERROR logs, try Redis first (3-tier fallback: Redis → MongoDB → Empty)
        if ("ERROR".equalsIgnoreCase(level)) {
            logger.info("Attempting Redis fallback for ERROR logs...");
            try {
                Page<LogEntryDocument> redisResults = searchErrorLogsInRedis(page, size);
                if (redisResults != null && redisResults.hasContent()) {
                    logger.info("Found {} ERROR logs in Redis fallback", redisResults.getNumberOfElements());
                    return redisResults;
                }
                logger.info("No ERROR logs in Redis, falling back to MongoDB");
            } catch (Exception redisEx) {
                logger.warn("Redis fallback failed, trying MongoDB", redisEx);
            }
        }

        // Standard MongoDB fallback for all levels (including ERROR if Redis failed)
        logger.info("Attempting MongoDB fallback for {} logs...", level);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<LogEntry> mongoResults = mongoRepository.findByLevel(level, pageable);
            if (mongoResults.hasContent()) {
                logger.info("Found {} logs in MongoDB fallback", mongoResults.getNumberOfElements());
            }
            return convertToDocumentPage(mongoResults);
        } catch (Exception mongoEx) {
            logger.error("MongoDB fallback also failed for level search", mongoEx);
            return Page.empty();
        }
    }

    private List<LogEntryDocument> fallbackSearchByTraceId(String traceId, Throwable ex) {
        logger.warn("Elasticsearch unavailable for trace ID search. Falling back to MongoDB. TraceId: {}", traceId);
        try {
            List<LogEntry> mongoResults = mongoRepository.findByTraceId(traceId);
            return mongoResults.stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());
        } catch (Exception mongoEx) {
            logger.error("MongoDB fallback also failed for trace ID search", mongoEx);
            return Collections.emptyList();
        }
    }

    private Page<LogEntryDocument> fallbackFullTextSearch(String searchTerm, int page, int size, Throwable ex) {
        logger.warn("Elasticsearch unavailable for full-text search. Falling back to MongoDB. Search term: {}",
                searchTerm);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<LogEntry> mongoResults = mongoRepository.findByMessageContaining(searchTerm, pageable);
            return convertToDocumentPage(mongoResults);
        } catch (Exception mongoEx) {
            logger.error("MongoDB fallback also failed for full-text search", mongoEx);
            return Page.empty();
        }
    }

    private Page<LogEntryDocument> fallbackSearchByTimeRange(Instant startTime, Instant endTime, int page, int size,
            Throwable ex) {
        logger.warn("Elasticsearch unavailable for time range search. Falling back to MongoDB. Range: {} to {}",
                startTime, endTime);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            // Convert Instant to String format that matches MongoDB storage
            String startTimeStr = DateTimeFormatter.ISO_INSTANT.format(startTime);
            String endTimeStr = DateTimeFormatter.ISO_INSTANT.format(endTime);
            Page<LogEntry> mongoResults = mongoRepository.findByTimestampBetween(startTimeStr, endTimeStr, pageable);
            return convertToDocumentPage(mongoResults);
        } catch (Exception mongoEx) {
            logger.error("MongoDB fallback also failed for time range search", mongoEx);
            return Page.empty();
        }
    }

    private Page<LogEntryDocument> fallbackAdvancedSearch(String service, String level, Instant startTime,
            Instant endTime, int page, int size, Throwable ex) {
        logger.warn("Elasticsearch unavailable for advanced search. Falling back to MongoDB");
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            String startTimeStr = DateTimeFormatter.ISO_INSTANT.format(startTime);
            String endTimeStr = DateTimeFormatter.ISO_INSTANT.format(endTime);
            Page<LogEntry> mongoResults = mongoRepository.findByServiceAndLevelAndTimestampBetween(service, level,
                    startTimeStr, endTimeStr, pageable);
            return convertToDocumentPage(mongoResults);
        } catch (Exception mongoEx) {
            logger.error("MongoDB fallback also failed for advanced search", mongoEx);
            return Page.empty();
        }
    }

    private Page<LogEntryDocument> fallbackSearchByServiceAndLevel(String service, String level, int page, int size,
            Throwable ex) {
        logger.warn(
                "Elasticsearch unavailable for service and level search. Falling back to MongoDB. Service: {}, Level: {}",
                service, level);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<LogEntry> mongoResults = mongoRepository.findByServiceAndLevel(service, level, pageable);
            return convertToDocumentPage(mongoResults);
        } catch (Exception mongoEx) {
            logger.error("MongoDB fallback also failed for service and level search", mongoEx);
            return Page.empty();
        }
    }

    /**
     * Search ERROR logs in Redis cache (fast path)
     * Returns paginated results from Redis cache
     */
    private Page<LogEntryDocument> searchErrorLogsInRedis(int page, int size) {
        try {
            // Get all ERROR log keys from Redis
            Set<String> errorLogKeys = redisTemplate.keys(REDIS_ERROR_LOG_PREFIX + "*");

            if (errorLogKeys == null || errorLogKeys.isEmpty()) {
                logger.debug("No ERROR logs found in Redis cache");
                return null;
            }

            // Retrieve all ERROR logs from Redis
            List<LogEntry> errorLogs = new ArrayList<>();
            for (String key : errorLogKeys) {
                LogEntry logEntry = redisTemplate.opsForValue().get(key);
                if (logEntry != null) {
                    errorLogs.add(logEntry);
                }
            }

            // Sort by timestamp (newest first)
            errorLogs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, errorLogs.size());

            if (start >= errorLogs.size()) {
                return Page.empty();
            }

            List<LogEntry> paginatedLogs = errorLogs.subList(start, end);
            List<LogEntryDocument> documents = paginatedLogs.stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());

            Pageable pageable = PageRequest.of(page, size);
            return new PageImpl<>(documents, pageable, errorLogs.size());

        } catch (Exception e) {
            logger.warn("Failed to search ERROR logs in Redis cache", e);
            return null;
        }
    }

    /**
     * Convert MongoDB LogEntry to Elasticsearch LogEntryDocument
     */
    private LogEntryDocument convertToDocument(LogEntry logEntry) {
        LogEntryDocument doc = new LogEntryDocument();
        doc.setId(logEntry.getId());
        doc.setTimestamp(Instant.parse(logEntry.getTimestamp()));
        doc.setLevel(logEntry.getLevel());
        doc.setMessage(logEntry.getMessage());
        doc.setService(logEntry.getService());
        doc.setHost(logEntry.getHost());
        doc.setTraceId(logEntry.getTraceId());
        doc.setFile(logEntry.getFile());
        doc.setClassName(logEntry.getClassName());
        doc.setLine(logEntry.getLine());
        doc.setMethod(logEntry.getMethod());
        return doc;
    }

    /**
     * Convert a Page of MongoDB LogEntry to a Page of Elasticsearch
     * LogEntryDocument
     */
    private Page<LogEntryDocument> convertToDocumentPage(Page<LogEntry> logEntryPage) {
        return logEntryPage.map(this::convertToDocument);
    }
}
