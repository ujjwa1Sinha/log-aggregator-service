package com.dstrLogAggr.Log_Aggregator.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("log-data")
public class LogEntry {

    @Id
    private String id;

    private String timestamp;
    private String level;
    private String message;
    private String service;
    private String host;
    private String traceId;
    String file;
    String className;
    String line;
    String method;
}