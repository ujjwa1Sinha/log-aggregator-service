package com.dstrLogAggr.Log_Aggregator.service;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import com.dstrLogAggr.Log_Aggregator.repository.LogBackRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LogKafkaConsumer {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(LogKafkaConsumer.class);

    @Autowired
    private LogBackRepo logRepo;

    private RedisTemplate<String, LogEntry> redisTemplate = new RedisTemplate<>();

    @KafkaListener(topics = "log-data", groupId = "log-group")
    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    @CircuitBreaker(name = "mongoCB", fallbackMethod = "fallbackSaveLog")
    public void listen(String message) throws JsonProcessingException {
        LogEntry entry = mapper.readValue(message, LogEntry.class);

        logRepo.save(entry); // If Mongo is down, this throws

        logger.info("Log saved from Kafka to MongoDB");

        if ("ERROR".equalsIgnoreCase(entry.getLevel())) {
            logger.info("Entered to save log from Kafka to Redis");
            String redisKey = "ERROR_LOG:" + entry.getId();
            redisTemplate.opsForValue().set(redisKey, entry, Duration.ofMinutes(15));
        }

        logger.info("Log saved from Kafka to Redis");
    }


    @Recover
    public void recover(Exception e, String message) throws JsonProcessingException {
        LogEntry entry = mapper.readValue(message, LogEntry.class);
        logger.error("Log failed after retries. Saving fallback: " + entry, e);
        // Optional: Save to file or Redis
    }

    public void fallbackSaveLog(String message, Throwable ex) {
        logger.error("❗ MongoDB is down. Fallback triggered for message: {}", message, ex);
        // Optionally cache in Redis or write to a backup queue
    }
} 

