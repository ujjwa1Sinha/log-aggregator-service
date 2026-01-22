package com.dstrLogAggr.Log_Aggregator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Log Aggregator.
 * These can be customized in application.properties or application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "log-aggregator")
public class LogAggregatorProperties {

    private Kafka kafka = new Kafka();
    private Mongodb mongodb = new Mongodb();
    private Elasticsearch elasticsearch = new Elasticsearch();
    private Redis redis = new Redis();
    private FaultTolerance faultTolerance = new FaultTolerance();

    public static class Kafka {
        private String bootstrapServers = "localhost:9092";
        private String topic = "log-data";
        private String groupId = "log-group";
        private int batchSize = 100;

        // Getters and Setters
        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class Mongodb {
        private String uri = "mongodb://localhost:27017/log-data";
        private String database = "log-data";

        // Getters and Setters
        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }
    }

    public static class Elasticsearch {
        private String uris = "http://localhost:9200";
        private String username;
        private String password;
        private boolean enabled = true;

        // Getters and Setters
        public String getUris() {
            return uris;
        }

        public void setUris(String uris) {
            this.uris = uris;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;

        // Getters and Setters
        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }
    }

    public static class FaultTolerance {
        private CircuitBreaker circuitBreaker = new CircuitBreaker();
        private Retry retry = new Retry();
        private RateLimiter rateLimiter = new RateLimiter();
        private Bulkhead bulkhead = new Bulkhead();

        public static class CircuitBreaker {
            private int slidingWindowSize = 10;
            private int failureRateThreshold = 50;
            private int waitDurationInOpenState = 10000;

            public int getSlidingWindowSize() {
                return slidingWindowSize;
            }

            public void setSlidingWindowSize(int slidingWindowSize) {
                this.slidingWindowSize = slidingWindowSize;
            }

            public int getFailureRateThreshold() {
                return failureRateThreshold;
            }

            public void setFailureRateThreshold(int failureRateThreshold) {
                this.failureRateThreshold = failureRateThreshold;
            }

            public int getWaitDurationInOpenState() {
                return waitDurationInOpenState;
            }

            public void setWaitDurationInOpenState(int waitDurationInOpenState) {
                this.waitDurationInOpenState = waitDurationInOpenState;
            }
        }

        public static class Retry {
            private int maxAttempts = 3;
            private long delay = 2000;
            private double multiplier = 2.0;

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public long getDelay() {
                return delay;
            }

            public void setDelay(long delay) {
                this.delay = delay;
            }

            public double getMultiplier() {
                return multiplier;
            }

            public void setMultiplier(double multiplier) {
                this.multiplier = multiplier;
            }
        }

        public static class RateLimiter {
            private int limitForPeriod = 100;
            private int limitRefreshPeriod = 1000;
            private int timeoutDuration = 500;

            public int getLimitForPeriod() {
                return limitForPeriod;
            }

            public void setLimitForPeriod(int limitForPeriod) {
                this.limitForPeriod = limitForPeriod;
            }

            public int getLimitRefreshPeriod() {
                return limitRefreshPeriod;
            }

            public void setLimitRefreshPeriod(int limitRefreshPeriod) {
                this.limitRefreshPeriod = limitRefreshPeriod;
            }

            public int getTimeoutDuration() {
                return timeoutDuration;
            }

            public void setTimeoutDuration(int timeoutDuration) {
                this.timeoutDuration = timeoutDuration;
            }
        }

        public static class Bulkhead {
            private int maxConcurrentCalls = 25;
            private int maxWaitDuration = 500;

            public int getMaxConcurrentCalls() {
                return maxConcurrentCalls;
            }

            public void setMaxConcurrentCalls(int maxConcurrentCalls) {
                this.maxConcurrentCalls = maxConcurrentCalls;
            }

            public int getMaxWaitDuration() {
                return maxWaitDuration;
            }

            public void setMaxWaitDuration(int maxWaitDuration) {
                this.maxWaitDuration = maxWaitDuration;
            }
        }

        public CircuitBreaker getCircuitBreaker() {
            return circuitBreaker;
        }

        public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public RateLimiter getRateLimiter() {
            return rateLimiter;
        }

        public void setRateLimiter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        public Bulkhead getBulkhead() {
            return bulkhead;
        }

        public void setBulkhead(Bulkhead bulkhead) {
            this.bulkhead = bulkhead;
        }
    }

    // Main getters and setters
    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public Mongodb getMongodb() {
        return mongodb;
    }

    public void setMongodb(Mongodb mongodb) {
        this.mongodb = mongodb;
    }

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public void setElasticsearch(Elasticsearch elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public FaultTolerance getFaultTolerance() {
        return faultTolerance;
    }

    public void setFaultTolerance(FaultTolerance faultTolerance) {
        this.faultTolerance = faultTolerance;
    }
}
