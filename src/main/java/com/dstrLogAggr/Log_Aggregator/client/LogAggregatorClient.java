package com.dstrLogAggr.Log_Aggregator.client;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Client library for sending logs to the Log Aggregator system.
 * This is the main entry point for applications that want to use the log
 * aggregator as a dependency.
 * 
 * Usage example:
 * 
 * <pre>
 * LogAggregatorClient client = LogAggregatorClient.builder()
 *         .kafkaTemplate(kafkaTemplate)
 *         .serviceName("my-service")
 *         .build();
 * 
 * client.info("Application started successfully");
 * client.error("Database connection failed", exception);
 * </pre>
 */
public class LogAggregatorClient {

    private static final Logger logger = LoggerFactory.getLogger(LogAggregatorClient.class);
    private static final String KAFKA_TOPIC = "log-data";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceName;
    private final String hostName;

    private LogAggregatorClient(Builder builder) {
        this.kafkaTemplate = builder.kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.serviceName = builder.serviceName;
        this.hostName = builder.hostName != null ? builder.hostName : getDefaultHostName();
    }

    /**
     * Log an INFO level message
     */
    public void info(String message) {
        log(LogLevel.INFO, message, null, null);
    }

    /**
     * Log an INFO level message with any arbitary data
     */
    public void info(String message, String anyData) {
        log(LogLevel.INFO, message + " " + anyData, null, null);
    }

    /**
     * Log an INFO level message with metadata
     */
    public void info(String message, Map<String, String> metadata) {
        log(LogLevel.INFO, message, metadata, null);
    }

    /**
     * Log a WARN level message
     */
    public void warn(String message) {
        log(LogLevel.WARN, message, null, null);
    }

    /**
     * Log a WARN level message with any arbitary data
     */
    public void warn(String message, String anyData) {
        log(LogLevel.WARN, message + " " + anyData, null, null);
    }

    /**
     * Log a WARN level message with metadata
     */
    public void warn(String message, Map<String, String> metadata) {
        log(LogLevel.WARN, message, metadata, null);
    }

    /**
     * Log an ERROR level message
     */
    public void error(String message) {
        log(LogLevel.ERROR, message, null, null);
    }

    /**
     * Log a ERROR level message with any arbitary data
     */
    public void error(String message, String anyData) {
        log(LogLevel.ERROR, message + " " + anyData, null, null);
    }

    /**
     * Log an ERROR level message with exception
     */
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, null, throwable);
    }

    /**
     * Log an ERROR level message with metadata and exception
     */
    public void error(String message, Map<String, String> metadata, Throwable throwable) {
        log(LogLevel.ERROR, message, metadata, throwable);
    }

    /**
     * Log a DEBUG level message
     */
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null, null);
    }

    /**
     * Log a DEBUG level message with any arbitary data
     */
    public void debug(String message, String anyData) {
        log(LogLevel.DEBUG, message + " " + anyData, null, null);
    }

    /**
     * Log a DEBUG level message with metadata
     */
    public void debug(String message, Map<String, String> metadata) {
        log(LogLevel.DEBUG, message, metadata, null);
    }

    /**
     * Log with trace ID for distributed tracing
     */
    public void logWithTrace(LogLevel level, String message, String traceId) {
        LogEntry logEntry = createLogEntry(level, message, null, null);
        logEntry.setTraceId(traceId);
        sendLog(logEntry);
    }

    /**
     * Core logging method
     */
    private void log(LogLevel level, String message, Map<String, String> metadata, Throwable throwable) {
        LogEntry logEntry = createLogEntry(level, message, metadata, throwable);
        sendLog(logEntry);
    }

    /**
     * Create a LogEntry object
     */
    private LogEntry createLogEntry(LogLevel level, String message, Map<String, String> metadata, Throwable throwable) {
        LogEntry logEntry = new LogEntry();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setTimestamp(Instant.now().toString());
        logEntry.setLevel(level.name());
        logEntry.setMessage(enrichMessage(message, throwable));
        logEntry.setService(serviceName);
        logEntry.setHost(hostName);

        // Extract stack trace information if available
        if (throwable != null) {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                StackTraceElement element = stackTrace[0];
                logEntry.setClassName(element.getClassName());
                logEntry.setMethod(element.getMethodName());
                logEntry.setFile(element.getFileName());
                logEntry.setLine(String.valueOf(element.getLineNumber()));
            }
        }

        return logEntry;
    }

    /**
     * Enrich message with exception details if present
     */
    private String enrichMessage(String message, Throwable throwable) {
        if (throwable == null) {
            return message;
        }
        return message + " | Exception: " + throwable.getClass().getName() + " - " + throwable.getMessage();
    }

    /**
     * Send log to Kafka
     */
    private void sendLog(LogEntry logEntry) {
        try {
            String jsonLog = objectMapper.writeValueAsString(logEntry);
            kafkaTemplate.send(KAFKA_TOPIC, logEntry.getId(), jsonLog);
            logger.debug("Log sent to aggregator: {}", logEntry.getId());
        } catch (Exception e) {
            logger.error("Failed to send log to aggregator", e);
            // Fallback: log locally
            logger.info("Fallback log - Level: {}, Message: {}", logEntry.getLevel(), logEntry.getMessage());
        }
    }

    /**
     * Get default hostname
     */
    private String getDefaultHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }

    /**
     * Builder for LogAggregatorClient
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private KafkaTemplate<String, String> kafkaTemplate;
        private String serviceName = "default-service";
        private String hostName;

        public Builder kafkaTemplate(KafkaTemplate<String, String> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder hostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        public LogAggregatorClient build() {
            if (kafkaTemplate == null) {
                throw new IllegalStateException("KafkaTemplate is required");
            }
            return new LogAggregatorClient(this);
        }
    }

    /**
     * Log levels
     */
    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }
}
