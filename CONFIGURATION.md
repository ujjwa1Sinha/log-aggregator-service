# Log Aggregator Configuration Guide

This guide explains what configuration is required vs. optional when using the Log Aggregator library as a dependency.

## 🚀 Quick Start (Minimal Configuration)

The library uses **Spring Boot Auto-Configuration**, so most settings have sensible defaults. Here's the **absolute minimum** you need:

### Required Configuration

```properties
# 1. Your application name (used to identify logs)
spring.application.name=my-awesome-service

# 2. Kafka connection (required - no default)
spring.kafka.bootstrap-servers=localhost:9092

# 3. MongoDB connection (required - environment specific)
spring.data.mongodb.uri=mongodb://localhost:27017/logs
```

That's it! With just these 3 properties, the library will work with all default settings.

---

## 📋 Configuration Levels

### Level 1: Minimum (Required)

These **must** be configured - the library cannot function without them:

```properties
# Application identification
spring.application.name=my-service

# Kafka (message queue for log ingestion)
spring.kafka.bootstrap-servers=localhost:9092

# MongoDB (primary log storage)
spring.data.mongodb.uri=mongodb://localhost:27017/logs
```

### Level 2: Recommended (Production)

Add these for production deployments:

```properties
# === Required (from Level 1) ===
spring.application.name=my-service
spring.kafka.bootstrap-servers=kafka-prod.example.com:9092
spring.data.mongodb.uri=mongodb://user:pass@mongo-prod.example.com:27017/logs

# === Elasticsearch (for search features) ===
spring.elasticsearch.uris=http://elasticsearch:9200
# Optional: if your Elasticsearch requires auth
spring.elasticsearch.username=elastic
spring.elasticsearch.password=changeme

# === Redis (for caching ERROR logs) ===
spring.data.redis.host=redis-prod.example.com
spring.data.redis.port=6379
spring.data.redis.password=your-redis-password
```

### Level 3: Advanced (Custom Tuning)

Fine-tune fault tolerance and performance:

```properties
# === All from Level 2 ===

# === Kafka Advanced ===
log-aggregator.kafka.topic=custom-log-topic
log-aggregator.kafka.group-id=custom-log-group
log-aggregator.kafka.batch-size=200

# === Circuit Breaker ===
resilience4j.circuitbreaker.instances.mongoCB.sliding-window-size=20
resilience4j.circuitbreaker.instances.mongoCB.failure-rate-threshold=60
resilience4j.circuitbreaker.instances.mongoCB.wait-duration-in-open-state=15s

resilience4j.circuitbreaker.instances.elasticsearchCB.sliding-window-size=10
resilience4j.circuitbreaker.instances.elasticsearchCB.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.elasticsearchCB.wait-duration-in-open-state=10s

# === Rate Limiting ===
resilience4j.ratelimiter.instances.searchRateLimiter.limit-for-period=200
resilience4j.ratelimiter.instances.searchRateLimiter.limit-refresh-period=1s
resilience4j.ratelimiter.instances.searchRateLimiter.timeout-duration=500ms

# === Bulkhead (Thread Pool Isolation) ===
resilience4j.bulkhead.instances.searchBulkhead.max-concurrent-calls=50
resilience4j.bulkhead.instances.searchBulkhead.max-wait-duration=1000ms

# === Retry ===
resilience4j.retry.instances.mongoRetry.max-attempts=5
resilience4j.retry.instances.mongoRetry.wait-duration=3s
resilience4j.retry.instances.mongoRetry.exponential-backoff-multiplier=2

# === Disable Elasticsearch (if not needed) ===
log-aggregator.elasticsearch.enabled=false
```

---

## 🎯 Default Values Reference

If you don't specify these properties, the library uses these defaults:

| Property | Default Value | Description |
|----------|--------------|-------------|
| **Kafka** | | |
| `log-aggregator.kafka.topic` | `log-data` | Kafka topic name |
| `log-aggregator.kafka.group-id` | `log-group` | Consumer group ID |
| `log-aggregator.kafka.batch-size` | `100` | Batch size for processing |
| **MongoDB** | | |
| `log-aggregator.mongodb.database` | `log-data` | Database name |
| **Elasticsearch** | | |
| `log-aggregator.elasticsearch.enabled` | `true` | Enable/disable Elasticsearch |
| `spring.elasticsearch.uris` | `http://localhost:9200` | Elasticsearch URL |
| **Redis** | | |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `spring.data.redis.database` | `0` | Redis database index |
| **Circuit Breaker** | | |
| `sliding-window-size` | `10` | Number of calls to track |
| `failure-rate-threshold` | `50` | % failures to open circuit |
| `wait-duration-in-open-state` | `10s` | Wait before retry |
| **Rate Limiter** | | |
| `limit-for-period` | `100` | Max requests per period |
| `limit-refresh-period` | `1s` | Period duration |
| **Bulkhead** | | |
| `max-concurrent-calls` | `25` | Max parallel operations |
| **Retry** | | |
| `max-attempts` | `3` | Retry attempts |
| `wait-duration` | `2s` | Initial delay |
| `exponential-backoff-multiplier` | `2.0` | Backoff multiplier |

---

## 🔧 Auto-Configuration Details

### What's Auto-Configured?

When you add this library as a dependency, Spring Boot automatically configures:

✅ **LogAggregatorClient** - Main logging client  
✅ **All Services** - LogSearchService, LogConsumerService, etc.  
✅ **All Repositories** - MongoDB and Elasticsearch repositories  
✅ **Fault Tolerance** - Circuit breakers, retries, rate limiters  
✅ **Component Scanning** - All library components are discovered  

### How Auto-Configuration Works

1. **Detection**: Spring Boot detects `META-INF/spring.factories`
2. **Conditional Loading**: Only loads if Kafka is on classpath
3. **Bean Creation**: Creates `LogAggregatorClient` bean automatically
4. **Property Binding**: Binds your `application.properties` to configuration classes

### Disabling Auto-Configuration

If you want to disable the library temporarily:

```properties
log-aggregator.enabled=false
```

Or exclude from auto-configuration:

```java
@SpringBootApplication(exclude = LogAggregatorAutoConfiguration.class)
public class MyApplication {
    // ...
}
```

---

## 📝 Configuration Examples

### Example 1: Local Development

```properties
spring.application.name=user-service
spring.kafka.bootstrap-servers=localhost:9092
spring.data.mongodb.uri=mongodb://localhost:27017/logs
```

### Example 2: Docker Compose

```properties
spring.application.name=order-service
spring.kafka.bootstrap-servers=kafka:9092
spring.data.mongodb.uri=mongodb://mongo:27017/logs
spring.elasticsearch.uris=http://elasticsearch:9200
spring.data.redis.host=redis
```

### Example 3: Production (Kubernetes)

```properties
spring.application.name=${SERVICE_NAME}
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS}
spring.data.mongodb.uri=${MONGODB_URI}
spring.elasticsearch.uris=${ELASTICSEARCH_URIS}
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.password=${REDIS_PASSWORD}

# Production tuning
resilience4j.circuitbreaker.instances.mongoCB.failure-rate-threshold=70
resilience4j.ratelimiter.instances.searchRateLimiter.limit-for-period=500
```

### Example 4: Minimal (No Elasticsearch/Redis)

```properties
spring.application.name=simple-service
spring.kafka.bootstrap-servers=localhost:9092
spring.data.mongodb.uri=mongodb://localhost:27017/logs

# Disable optional features
log-aggregator.elasticsearch.enabled=false
```

---

## 🎓 YAML Configuration

Prefer YAML? Here's the equivalent:

```yaml
spring:
  application:
    name: my-service
  kafka:
    bootstrap-servers: localhost:9092
  data:
    mongodb:
      uri: mongodb://localhost:27017/logs
    redis:
      host: localhost
      port: 6379
  elasticsearch:
    uris: http://localhost:9200

log-aggregator:
  kafka:
    topic: log-data
    group-id: log-group
  elasticsearch:
    enabled: true

resilience4j:
  circuitbreaker:
    instances:
      mongoCB:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
  ratelimiter:
    instances:
      searchRateLimiter:
        limit-for-period: 100
        limit-refresh-period: 1s
```

---

## ❓ FAQ

### Q: Do I need to configure everything?

**A:** No! Only 3 properties are required:
- `spring.application.name`
- `spring.kafka.bootstrap-servers`
- `spring.data.mongodb.uri`

Everything else has sensible defaults.

### Q: What if I don't have Elasticsearch?

**A:** The library will work fine! Search operations will automatically fall back to MongoDB. You can explicitly disable it:
```properties
log-aggregator.elasticsearch.enabled=false
```

### Q: What if I don't have Redis?

**A:** Redis is optional. The library uses it for caching ERROR logs, but will work without it.

### Q: Can I use environment variables?

**A:** Yes! Spring Boot supports environment variables:
```bash
export SPRING_APPLICATION_NAME=my-service
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/logs
```

### Q: How do I know if auto-configuration worked?

**A:** Check your application logs on startup:
```
Auto-configuration report:
   LogAggregatorAutoConfiguration matched:
      - @ConditionalOnClass found required class 'KafkaTemplate'
```

Or inject the client and verify:
```java
@Autowired
private LogAggregatorClient logClient; // Should not be null
```

---

## 🔍 Troubleshooting

### Issue: "KafkaTemplate not found"

**Solution:** Add Kafka dependency to your project:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### Issue: "LogAggregatorClient bean not found"

**Cause:** Auto-configuration didn't run

**Solution:** Ensure:
1. Spring Boot version is 2.7+ or 3.x
2. `META-INF/spring.factories` is in the JAR
3. Kafka is on classpath
4. `log-aggregator.enabled` is not set to `false`

### Issue: Logs not appearing

**Checklist:**
1. ✅ Kafka is running and accessible
2. ✅ MongoDB is running and accessible
3. ✅ `spring.kafka.bootstrap-servers` is correct
4. ✅ `spring.data.mongodb.uri` is correct
5. ✅ Check application logs for errors

---

## 📚 Related Documentation

- [README.md](README.md) - Main documentation
- [API Reference](README.md#api-reference) - Client API details
- [Fault Tolerance](README.md#fault-tolerance) - Circuit breakers and fallbacks
