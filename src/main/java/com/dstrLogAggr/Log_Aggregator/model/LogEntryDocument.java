package com.dstrLogAggr.Log_Aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.Instant;

/**
 * Elasticsearch document for log entries.
 * Provides full-text search and analytics capabilities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "log-data")
public class LogEntryDocument {

    @Id
    private String id;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    private String level;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String message;

    @Field(type = FieldType.Keyword)
    private String service;

    @Field(type = FieldType.Keyword)
    private String host;

    @Field(type = FieldType.Keyword)
    private String traceId;

    @Field(type = FieldType.Keyword)
    private String file;

    @Field(type = FieldType.Keyword)
    private String className;

    @Field(type = FieldType.Keyword)
    private String line;

    @Field(type = FieldType.Keyword)
    private String method;

    /**
     * Convert from MongoDB LogEntry to Elasticsearch document
     */
    public static LogEntryDocument fromLogEntry(LogEntry logEntry) {
        return LogEntryDocument.builder()
                .id(logEntry.getId())
                .timestamp(parseTimestamp(logEntry.getTimestamp()))
                .level(logEntry.getLevel())
                .message(logEntry.getMessage())
                .service(logEntry.getService())
                .host(logEntry.getHost())
                .traceId(logEntry.getTraceId())
                .file(logEntry.getFile())
                .className(logEntry.getClassName())
                .line(logEntry.getLine())
                .method(logEntry.getMethod())
                .build();
    }

    private static Instant parseTimestamp(String timestamp) {
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
