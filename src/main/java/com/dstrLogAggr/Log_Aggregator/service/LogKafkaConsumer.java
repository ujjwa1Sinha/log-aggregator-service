package com.dstrLogAggr.Log_Aggregator.service;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for log messages with enhanced fault tolerance.
 * Consumes log messages from Kafka and persists them using
 * LogPersistenceService.
 */
@Component
public class LogKafkaConsumer {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(LogKafkaConsumer.class);

    @Autowired
    private LogPersistenceService logPersistenceService;

    /**
     * Listen to Kafka log messages and persist them with fault tolerance
     */
    @KafkaListener(topics = "log-data", groupId = "log-group", containerFactory = "kafkaListenerContainerFactory")
    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    @CircuitBreaker(name = "kafkaConsumerCB", fallbackMethod = "fallbackKafkaConsume")
    public void listen(String message, Acknowledgment acknowledgment) throws JsonProcessingException {
        try {
            logger.debug("Received Kafka message: {}", message);

            LogEntry entry = mapper.readValue(message, LogEntry.class);

            // Use the enhanced persistence service for dual-write
            logPersistenceService.saveLog(entry);

            logger.info("Log processed successfully from Kafka: {}", entry.getId());

            // Manual acknowledgment after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            logger.error("Error processing Kafka message", e);
            throw e;
        }
    }

    /**
     * Overloaded method for backward compatibility (without acknowledgment)
     */
    public void listen(String message) throws JsonProcessingException {
        listen(message, null);
    }

    /**
     * Recovery method after all retries are exhausted
     */
    @Recover
    public void recover(Exception e, String message, Acknowledgment acknowledgment) throws JsonProcessingException {
        LogEntry entry = mapper.readValue(message, LogEntry.class);
        logger.error("❌ Log processing failed after all retries. Log ID: {}", entry.getId(), e);

        // Send to Dead Letter Queue or alternative storage
        // For now, we'll let the fallback handle it

        // Don't acknowledge - message will be reprocessed or sent to DLQ by Kafka
    }

    /**
     * Circuit breaker fallback method
     */
    public void fallbackKafkaConsume(String message, Acknowledgment acknowledgment, Throwable ex) {
        logger.error("⚠️ Circuit breaker activated for Kafka consumer. Message processing degraded.", ex);

        try {
            LogEntry entry = mapper.readValue(message, LogEntry.class);
            // The LogPersistenceService has its own fallback mechanisms
            logger.warn("Message will be retried or sent to DLQ: {}", entry.getId());
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse message in fallback", e);
        }

        // Don't acknowledge - let Kafka retry
    }
}
