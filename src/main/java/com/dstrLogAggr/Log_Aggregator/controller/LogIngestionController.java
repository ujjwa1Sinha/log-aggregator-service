package com.dstrLogAggr.Log_Aggregator.controller;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import com.dstrLogAggr.Log_Aggregator.repository.LogBackRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LogIngestionController {

    @Autowired
    private LogBackRepo logRepository;

    private static final Logger logger = LoggerFactory.getLogger(LogIngestionController.class);

    @PostMapping("/log-ingestor")
    public ResponseEntity<?> ingestLog(@RequestBody LogEntry logEntry) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();

        System.out.println("Request body: "+ mapper.writeValueAsString(logEntry));

        logger.debug("Entered ingestLog method to save data in MongoDB");

        logRepository.save(logEntry);

        logger.debug("Data saved in MongoDB");

        return ResponseEntity.ok("Log stored");
    }

//    @PostMapping("/log-ingestor")
//    public ResponseEntity<?> ingestLogRaw(@RequestBody String raw) throws JsonProcessingException {
//        System.out.println("Raw request: " + raw);
//        ObjectMapper mapper = new ObjectMapper();
//        LogEntry entry = mapper.readValue(raw, LogEntry.class);
//        System.out.println("Mapped: " + entry);
//        return ResponseEntity.ok("Log stored");
//    }

}
