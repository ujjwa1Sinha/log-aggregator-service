package com.dstrLogAggr.Log_Aggregator.service;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import com.dstrLogAggr.Log_Aggregator.repository.LogBackRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LogKafkaConsumer {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(LogKafkaConsumer.class);

    @Autowired
    private LogBackRepo logRepo;

    @Autowired
    private RedisTemplate redisTemplate;

    @KafkaListener(topics = "log-data", groupId = "log-group")
    public void listen(String message) {
        try {
            LogEntry entry = mapper.readValue(message, LogEntry.class);
            logRepo.save(entry);

            logger.info("Log saved from Kafka to MongoDB");

            // Save ERROR logs to Redis with 15 min TTL
            if ("ERROR".equalsIgnoreCase(entry.getLevel())) {
                logger.info("Entered to save log from Kafka to Redis");
                String redisKey = "ERROR_LOG:" + entry.getId(); // Use a unique key
                redisTemplate.opsForValue().set(redisKey, entry, Duration.ofMinutes(15));
            }

            logger.info("Log saved from Kafka to Redis");

        } catch (Exception e) {
            logger.error("Error parsing Kafka message: " + e.getMessage());
        }
    }
} 

