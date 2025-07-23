package com.dstrLogAggr.Log_Aggregator.service;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import com.dstrLogAggr.Log_Aggregator.repository.LogBackRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LogKafkaConsumer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private LogBackRepo logRepo;

    @KafkaListener(topics = "log-data", groupId = "log-group")
    public void listen(String message) {
        try {
            LogEntry entry = mapper.readValue(message, LogEntry.class);
            logRepo.save(entry);
            System.out.println("Log saved from Kafka: ");
        } catch (Exception e) {
            System.err.println("Error parsing Kafka message: " + e.getMessage());
        }
    }
} 

