# 📊 Log Aggregator Library

> **A production-ready, fault-tolerant distributed logging library for Java/Spring Boot applications with advanced search capabilities and intelligent MongoDB fallback.**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.11.0-blue.svg)](https://www.elastic.co/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🌟 Overview

Log Aggregator is an enterprise-grade logging library that transforms how Java applications handle distributed logging. Built with resilience and performance in mind, it provides seamless log aggregation, real-time search, and intelligent fallback mechanisms to ensure your logs are always accessible—even during infrastructure failures.

### Why Log Aggregator?

- **🚀 Zero Configuration Hassle** - Spring Boot auto-configuration means you're up and running with just 3 properties
- **🔍 Powerful Search** - Elasticsearch-powered full-text search with MongoDB fallback for 100% uptime
- **💪 Battle-Tested Resilience** - Circuit breakers, retries, rate limiting, and bulkhead patterns built-in
- **📊 Dual Storage Strategy** - MongoDB for persistence, Elasticsearch for lightning-fast search
- **⚡ High Performance** - Asynchronous operations and intelligent batching
- **🔄 Distributed Tracing** - First-class support for microservices with trace ID correlation
- **🛡️ Production Ready** - Redis caching, dead letter queues, and comprehensive monitoring

---

## ✨ Key Features

### Core Capabilities

| Feature | Description |
|---------|-------------|
| **Auto-Configuration** | Spring Boot auto-configuration with sensible defaults |
| **Dual Storage** | MongoDB for persistence + Elasticsearch for search |
| **Intelligent Fallback** | Automatic MongoDB fallback when Elasticsearch is unavailable |
| **Full-Text Search** | Powerful search across all log fields with pagination |
| **Distributed Tracing** | Trace ID support for tracking requests across microservices |
| **REST API** | Built-in search API with 7 endpoints |
| **Real-Time Ingestion** | Kafka-based asynchronous log processing |
| **Redis Caching** | ERROR logs cached for quick access |

### Fault Tolerance

| Mechanism | Purpose | Default Configuration |
|-----------|---------|----------------------|
| **Circuit Breakers** | Prevent cascading failures | 50% failure threshold, 10s wait |
| **Retry Logic** | Automatic retry with exponential backoff | 3 attempts, 2s initial delay |
| **Rate Limiting** | Protect against overload | 100 requests/second |
| **Bulkhead Pattern** | Thread pool isolation | 25 concurrent calls |
| **MongoDB Fallback** | Search continuity during ES outages | Automatic activation |

---

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Spring Boot 3.x
- Kafka (for log ingestion)
- MongoDB (for log storage)
- Elasticsearch 8.x (optional, for search)
- Redis (optional, for caching)

### Installation

#### Step 1: Add Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.dstrLogAggr</groupId>
    <artifactId>Log-Aggregator</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

#### Step 2: Minimal Configuration

Add to `application.properties` (only 3 required properties):

```properties
# Your application name
spring.application.name=my-awesome-service

# Kafka connection
spring.kafka.bootstrap-servers=localhost:9092

# MongoDB connection
spring.data.mongodb.uri=mongodb://localhost:27017/logs
```

#### Step 3: Start Logging!

```java
import com.dstrLogAggr.Log_Aggregator.client.LogAggregatorClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    @Autowired
    private LogAggregatorClient logClient;
    
    public void createUser(User user) {
        // Simple logging
        logClient.info("Creating new user: " + user.getName());
        
        try {
            // Your business logic
            userRepository.save(user);
            logClient.info("User created successfully");
        } catch (Exception e) {
            // Error logging with exception
            logClient.error("Failed to create user", e);
        }
    }
}
```

**That's it!** No manual configuration, no bean definitions, no setup classes. The library auto-configures everything.

---

## 📖 Complete Usage Guide

### Basic Logging

```java
@Autowired
private LogAggregatorClient logClient;

// Info level
logClient.info("Application started successfully");

// Warning level
logClient.warn("High memory usage detected");

// Error level
logClient.error("Database connection failed");

// Error with exception
try {
    riskyOperation();
} catch (Exception e) {
    logClient.error("Operation failed", e);
}

// Debug level
logClient.debug("Processing request with ID: " + requestId);
```

### Advanced Logging with Metadata

```java
// Log with custom metadata
Map<String, String> metadata = new HashMap<>();
metadata.put("userId", "12345");
metadata.put("action", "login");
metadata.put("ip", request.getRemoteAddr());

logClient.info("User logged in", metadata);
```

### Distributed Tracing

```java
// Generate trace ID for request tracking
String traceId = UUID.randomUUID().toString();

// Log with trace ID across multiple services
logClient.logWithTrace(LogLevel.INFO, "Order received", traceId);
// ... call other services with same traceId ...
logClient.logWithTrace(LogLevel.INFO, "Payment processed", traceId);
logClient.logWithTrace(LogLevel.INFO, "Order completed", traceId);

// Later, search all logs for this trace
List<LogEntryDocument> trace = searchService.searchByTraceId(traceId);
```

---

## 🔍 Searching Logs

### Using the Search Service

```java
@Autowired
private LogSearchService searchService;

// Search by service name
Page<LogEntryDocument> logs = searchService.searchByService("user-service", 0, 20);

// Search by log level
Page<LogEntryDocument> errors = searchService.searchByLevel("ERROR", 0, 20);

// Full-text search
Page<LogEntryDocument> results = searchService.fullTextSearch("database timeout", 0, 20);

// Search by trace ID (distributed tracing)
List<LogEntryDocument> trace = searchService.searchByTraceId("abc-123-def");

// Time range search
Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
Instant end = Instant.now();
Page<LogEntryDocument> recent = searchService.searchByTimeRange(start, end, 0, 20);

// Advanced multi-criteria search
Page<LogEntryDocument> filtered = searchService.advancedSearch(
    "payment-service",  // service
    "ERROR",            // level
    start,              // start time
    end,                // end time
    0,                  // page
    50                  // size
);
```

### Using the REST API

The library includes a built-in REST API for log searching:

```bash
# Search by service
GET /api/logs/search/service?name=user-service&page=0&size=20

# Search by level
GET /api/logs/search/level?level=ERROR&page=0&size=20

# Search by trace ID
GET /api/logs/search/trace/abc-123-def

# Full-text search
GET /api/logs/search/text?query=database+error&page=0&size=20

# Time range search
GET /api/logs/search/timerange?start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z&page=0&size=20

# Advanced search
GET /api/logs/search/advanced?service=payment-service&level=ERROR&start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z&page=0&size=20

# Combined service + level search
GET /api/logs/search/service-level?service=user-service&level=WARN&page=0&size=20

# Health check
GET /api/logs/health
```

---

## 🏗️ Architecture

### System Architecture

```
┌─────────────────┐
│  Your Spring    │
│  Boot App       │
└────────┬────────┘
         │ @Autowired LogAggregatorClient
         ▼
┌─────────────────────────────────────────────────────────┐
│              Log Aggregator Library                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Client     │  │   Services   │  │ Repositories │  │
│  │   - Logger   │  │   - Search   │  │   - MongoDB  │  │
│  │   - Builder  │  │   - Persist  │  │   - Elastic  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                  │           │
│         │    ┌────────────┴──────────────┐   │           │
│         │    │   Fault Tolerance Layer   │   │           │
│         │    │  - Circuit Breakers       │   │           │
│         │    │  - Retries                │   │           │
│         │    │  - Rate Limiting          │   │           │
│         │    │  - Bulkhead               │   │           │
│         │    └───────────────────────────┘   │           │
└─────────┼────────────────────────────────────┼───────────┘
          │                                    │
          ▼                                    ▼
    ┌──────────┐                    ┌─────────────────┐
    │  Kafka   │                    │    Storage      │
    │  Topic   │                    │  ┌───────────┐  │
    │ log-data │                    │  │  MongoDB  │  │
    └────┬─────┘                    │  └───────────┘  │
         │                          │  ┌───────────┐  │
         │ Async Consumption        │  │Elasticsearch│
         │                          │  └───────────┘  │
         ▼                          │  ┌───────────┐  │
    ┌──────────────┐                │  │   Redis   │  │
    │ Log Consumer │────────────────▶  └───────────┘  │
    │   Service    │                └─────────────────┘
    └──────────────┘
```

### Data Flow

#### 1. **Log Ingestion Flow**
```
Application → LogAggregatorClient → Kafka → Consumer → MongoDB + Elasticsearch
                                                      ↓
                                                   Redis (ERROR logs only)
```

#### 2. **Search Flow with Intelligent Fallback**
```
Search Request → LogSearchService → Elasticsearch (Primary)
                                         │
                                         ├─ Success → Return Results
                                         │
                                         └─ Failure (Circuit Breaker Opens)
                                              │
                                              ▼
                                         MongoDB (Fallback)
                                              │
                                              ├─ Success → Return Results
                                              │
                                              └─ Failure → Empty Result
```

---

## 🛡️ Fault Tolerance in Detail

### Multi-Layer Resilience

#### 1. Circuit Breakers

Prevents cascading failures by opening circuits when services are unhealthy:

```
Elasticsearch Circuit Breaker:
├─ Sliding Window: 10 calls
├─ Failure Threshold: 50%
├─ Open State Duration: 10 seconds
└─ Fallback: MongoDB search

MongoDB Circuit Breaker:
├─ Sliding Window: 10 calls
├─ Failure Threshold: 50%
├─ Open State Duration: 10 seconds
└─ Fallback: Redis temporary storage
```

#### 2. Intelligent MongoDB Fallback

**NEW FEATURE** ✨ - All search operations automatically fall back to MongoDB when Elasticsearch fails:

```java
// Your code remains the same
Page<LogEntryDocument> results = searchService.searchByService("my-service", 0, 20);

// Behind the scenes:
// 1. Try Elasticsearch (fast, optimized)
// 2. If fails → Try MongoDB (slower but reliable)
// 3. If both fail → Return empty result

// Performance:
// - Elasticsearch: ~10-50ms
// - MongoDB Fallback: ~100-500ms
// - Both Failed: Immediate empty result
```

**Supported Fallback Operations:**
- ✅ Search by service name
- ✅ Search by log level
- ✅ Search by trace ID
- ✅ Full-text search in messages
- ✅ Time range queries
- ✅ Advanced multi-criteria search
- ✅ Combined service + level search

#### 3. Retry Mechanism

Automatic retries with exponential backoff:

```
Attempt 1: Immediate
Attempt 2: Wait 2 seconds
Attempt 3: Wait 4 seconds
Give up → Trigger fallback
```

#### 4. Rate Limiting

Protects services from overload:

```
Search Operations: 100 requests/second
Configurable per operation type
Excess requests queued or rejected
```

#### 5. Bulkhead Pattern

Thread pool isolation prevents resource exhaustion:

```
Search Operations: 25 concurrent threads
Log Persistence: 50 concurrent threads
Independent thread pools prevent interference
```

---

## ⚙️ Configuration

### Minimal Configuration (Required)

```properties
spring.application.name=my-service
spring.kafka.bootstrap-servers=localhost:9092
spring.data.mongodb.uri=mongodb://localhost:27017/logs
```

### Recommended Configuration (Production)

```properties
# === Required ===
spring.application.name=my-service
spring.kafka.bootstrap-servers=kafka-prod:9092
spring.data.mongodb.uri=mongodb://user:pass@mongo-prod:27017/logs

# === Search Features ===
spring.elasticsearch.uris=http://elasticsearch:9200
spring.elasticsearch.username=elastic
spring.elasticsearch.password=changeme

# === Caching ===
spring.data.redis.host=redis-prod
spring.data.redis.port=6379
spring.data.redis.password=your-password

# === Kafka Advanced ===
log-aggregator.kafka.topic=production-logs
log-aggregator.kafka.group-id=log-consumer-group
log-aggregator.kafka.batch-size=200
```

### Advanced Tuning

```properties
# Circuit Breakers
resilience4j.circuitbreaker.instances.elasticsearchCB.sliding-window-size=20
resilience4j.circuitbreaker.instances.elasticsearchCB.failure-rate-threshold=60
resilience4j.circuitbreaker.instances.elasticsearchCB.wait-duration-in-open-state=15s

# Rate Limiting
resilience4j.ratelimiter.instances.searchRateLimiter.limit-for-period=200
resilience4j.ratelimiter.instances.searchRateLimiter.limit-refresh-period=1s

# Bulkhead
resilience4j.bulkhead.instances.searchBulkhead.max-concurrent-calls=50
resilience4j.bulkhead.instances.searchBulkhead.max-wait-duration=1000ms

# Retry
resilience4j.retry.instances.mongoRetry.max-attempts=5
resilience4j.retry.instances.mongoRetry.wait-duration=3s

# Disable Elasticsearch (MongoDB-only mode)
log-aggregator.elasticsearch.enabled=false
```

📖 **See [CONFIGURATION.md](CONFIGURATION.md) for complete configuration guide with all options and defaults.**

---

## 🐳 Running the Infrastructure

### Using Docker Compose

The project includes a complete Docker Compose setup with all required services:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

**Included Services:**
- **Kafka** (port 9092) - Message queue
- **Kafka UI** (port 8085) - Kafka management interface
- **MongoDB** (cloud-hosted) - Log storage
- **Elasticsearch** (port 9200) - Search engine
- **Kibana** (port 5601) - Elasticsearch UI
- **Redis** (cloud-hosted) - Caching layer
- **Nginx** (port 80) - API gateway

### Manual Setup

If you prefer manual setup:

```bash
# Kafka
docker run -d -p 9092:9092 bitnami/kafka:3.6

# MongoDB
docker run -d -p 27017:27017 mongo:latest

# Elasticsearch
docker run -d -p 9200:9200 -e "discovery.type=single-node" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# Redis
docker run -d -p 6379:6379 redis:latest
```

---

## 📊 Monitoring & Observability

### Health Endpoints

The library automatically registers health indicators:

```bash
# Overall health
GET /actuator/health

# Detailed health
GET /actuator/health/mongo
GET /actuator/health/elasticsearch
GET /actuator/health/kafka
GET /actuator/health/redis

# Circuit breaker states
GET /actuator/circuitbreakers
GET /actuator/circuitbreakerevents
```

### Metrics

Available metrics (via Spring Boot Actuator):

- Log ingestion rate
- Search query latency
- Circuit breaker states
- Retry attempts
- Rate limiter stats
- Cache hit/miss ratios

### Logging

The library uses SLF4J for internal logging:

```properties
# Enable debug logging
logging.level.com.dstrLogAggr.Log_Aggregator=DEBUG

# Log circuit breaker events
logging.level.io.github.resilience4j=DEBUG
```

---

## 🔧 API Reference

### LogAggregatorClient

```java
// Info level
void info(String message)
void info(String message, Map<String, String> metadata)

// Warning level
void warn(String message)
void warn(String message, Map<String, String> metadata)

// Error level
void error(String message)
void error(String message, Throwable throwable)
void error(String message, Map<String, String> metadata, Throwable throwable)

// Debug level
void debug(String message)
void debug(String message, Map<String, String> metadata)

// With trace ID
void logWithTrace(LogLevel level, String message, String traceId)
```

### LogSearchService

```java
Page<LogEntryDocument> searchByService(String service, int page, int size)
Page<LogEntryDocument> searchByLevel(String level, int page, int size)
List<LogEntryDocument> searchByTraceId(String traceId)
Page<LogEntryDocument> fullTextSearch(String searchTerm, int page, int size)
Page<LogEntryDocument> searchByTimeRange(Instant start, Instant end, int page, int size)
Page<LogEntryDocument> advancedSearch(String service, String level, Instant start, Instant end, int page, int size)
Page<LogEntryDocument> searchByServiceAndLevel(String service, String level, int page, int size)
```

---

## 🐛 Troubleshooting

### Logs not appearing in Elasticsearch

**Symptoms:** Logs stored in MongoDB but not searchable in Elasticsearch

**Diagnosis:**
```bash
# Check Elasticsearch health
curl http://localhost:9200/_cluster/health

# Check circuit breaker state
curl http://localhost:8080/actuator/circuitbreakers
```

**Solution:**
1. Verify Elasticsearch is running
2. Check network connectivity
3. Review application logs for errors
4. **Note:** Logs remain searchable via MongoDB fallback

### Search operations using MongoDB fallback

**Symptoms:**
- Slower search response times (100-500ms vs 10-50ms)
- Warning logs: `"Elasticsearch unavailable... Falling back to MongoDB"`

**Diagnosis:**
```bash
# Check Elasticsearch health
curl http://localhost:9200/_cluster/health

# Check circuit breaker state
curl http://localhost:8080/actuator/circuitbreakers/elasticsearchCB
```

**Solution:**
1. Restart Elasticsearch service
2. Verify network connectivity
3. Check Elasticsearch disk space
4. Wait for circuit breaker to close (default: 10 seconds)

**Temporary Workaround:**
- MongoDB fallback provides full search functionality
- Performance is degraded but service remains available
- No action required if MongoDB is healthy

### Build failures

**Issue:** `FileSystemException: The process cannot access the file`

**Solution:**
```bash
# Stop Java processes
Get-Process java | Stop-Process -Force

# Rebuild
mvn clean install -DskipTests
```

### High latency

**Diagnosis:**
1. Check if MongoDB fallback is active (indicates Elasticsearch issues)
2. Review bulkhead and rate limiter settings
3. Monitor Kafka consumer lag

**Solution:**
1. Scale Elasticsearch cluster
2. Increase bulkhead limits
3. Adjust rate limiter thresholds
4. Add Kafka partitions

---

## 🏗️ Building from Source

### Prerequisites

- JDK 17 or higher
- Maven 3.6+
- Docker (for running infrastructure)

### Build Steps

```bash
# Clone the repository
git clone https://github.com/your-org/log-aggregator.git
cd log-aggregator

# Build the library
mvn clean install -DskipTests

# Build with tests
mvn clean install

# Build Docker image
docker build -t log-aggregator-service:latest .
```

### Artifacts Generated

```
target/
├── Log-Aggregator-0.0.1-SNAPSHOT.jar          # Main library
├── Log-Aggregator-0.0.1-SNAPSHOT-sources.jar  # Source code
└── Log-Aggregator-0.0.1-SNAPSHOT-javadoc.jar  # Documentation
```

### Installing to Local Maven Repository

```bash
mvn clean install
```

Artifacts will be installed to:
```
~/.m2/repository/com/dstrLogAggr/Log-Aggregator/0.0.1-SNAPSHOT/
```

---

## 📚 Documentation

- 📖 **[Configuration Guide](CONFIGURATION.md)** - Complete guide to all configuration options, defaults, and examples
- 🚀 **[Quick Configuration](QUICK_CONFIG.md)** - TL;DR configuration summary
- 🔍 **[Elasticsearch Guide](ELASTICSEARCH_GUIDE.md)** - Elasticsearch setup and optimization
- 📝 **[API Reference](#api-reference)** - Complete API documentation
- 🔧 **[Fault Tolerance](#fault-tolerance-in-detail)** - Resilience patterns explained
- 🐛 **[Troubleshooting](#troubleshooting)** - Common issues and solutions

---

## 🎯 Use Cases

### Microservices Architecture

```java
// Order Service
logClient.logWithTrace(INFO, "Order created", traceId);

// Payment Service
logClient.logWithTrace(INFO, "Payment processed", traceId);

// Shipping Service
logClient.logWithTrace(INFO, "Order shipped", traceId);

// Later, trace the entire flow
List<LogEntryDocument> orderFlow = searchService.searchByTraceId(traceId);
```

### Error Monitoring

```java
try {
    processPayment(order);
} catch (PaymentException e) {
    Map<String, String> context = new HashMap<>();
    context.put("orderId", order.getId());
    context.put("amount", order.getTotal().toString());
    context.put("userId", order.getUserId());
    
    logClient.error("Payment processing failed", context, e);
}

// Search all payment errors
Page<LogEntryDocument> paymentErrors = searchService.advancedSearch(
    "payment-service", "ERROR", startTime, endTime, 0, 100
);
```

### Performance Monitoring

```java
long startTime = System.currentTimeMillis();
processRequest(request);
long duration = System.currentTimeMillis() - startTime;

Map<String, String> metrics = new HashMap<>();
metrics.put("duration", String.valueOf(duration));
metrics.put("endpoint", request.getEndpoint());

if (duration > 1000) {
    logClient.warn("Slow request detected", metrics);
}
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup

```bash
# Clone repository
git clone https://github.com/your-org/log-aggregator.git

# Start infrastructure
docker-compose up -d

# Run tests
mvn test

# Build
mvn clean install
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

Built with:
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Elasticsearch](https://www.elastic.co/) - Search engine
- [MongoDB](https://www.mongodb.com/) - Document database
- [Apache Kafka](https://kafka.apache.org/) - Message broker
- [Resilience4j](https://resilience4j.readme.io/) - Fault tolerance library
- [Redis](https://redis.io/) - Caching layer

---

## 📞 Support

For issues, questions, or feature requests:

- 🐛 **Bug Reports:** [Open an issue](https://github.com/your-org/log-aggregator/issues)
- 💡 **Feature Requests:** [Open an issue](https://github.com/your-org/log-aggregator/issues)
- 📧 **Email:** support@example.com
- 💬 **Discussions:** [GitHub Discussions](https://github.com/your-org/log-aggregator/discussions)

---

<div align="center">

**Made with ❤️ for the Java community**

[⬆ Back to Top](#-log-aggregator-library)

</div>
