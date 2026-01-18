# Elasticsearch Configuration and Search - Quick Reference

## 🚀 Quick Start (3 Steps)

### Step 1: Start Elasticsearch
```bash
# Using the provided script (Windows)
.\start-elasticsearch.ps1

# OR manually with docker-compose
docker-compose up -d

# Verify it's running
curl http://localhost:9200
```

### Step 2: Start Your Application
```bash
mvn spring-boot:run
```

### Step 3: Search Logs
```bash
# Search ERROR logs
curl "http://localhost:8080/api/logs/search/level?level=ERROR"

# Full-text search
curl "http://localhost:8080/api/logs/search/text?query=database"
```

---

## 📊 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Your Application                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  @Autowired                                               │   │
│  │  private LogAggregatorClient logClient;                   │   │
│  │                                                            │   │
│  │  logClient.info("User logged in");                        │   │
│  │  logClient.error("Database error", exception);            │   │
│  └──────────────────────┬───────────────────────────────────┘   │
└─────────────────────────┼───────────────────────────────────────┘
                          │
                          ▼
                    ┌──────────┐
                    │  Kafka   │ (Message Queue)
                    └─────┬────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  Log Kafka Consumer   │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │ LogPersistenceService │ (Dual Write)
              └─────┬───────────┬─────┘
                    │           │
        ┌───────────┘           └───────────┐
        ▼                                   ▼
   ┌─────────┐                      ┌──────────────┐
   │ MongoDB │ (Storage)             │ Elasticsearch│ (Search)
   └─────────┘                      └──────┬───────┘
                                           │
                                           ▼
                                  ┌─────────────────┐
                                  │  Search Logs    │
                                  │  via REST API   │
                                  └─────────────────┘
```

---

## 🔍 Search Methods

### Method 1: REST API (Recommended)

| Endpoint | Purpose | Example |
|----------|---------|---------|
| `/api/logs/search/service` | Search by service name | `?name=payment-service` |
| `/api/logs/search/level` | Search by log level | `?level=ERROR` |
| `/api/logs/search/text` | Full-text search | `?query=database+error` |
| `/api/logs/search/trace/{id}` | Distributed tracing | `/trace/abc-123` |
| `/api/logs/search/timerange` | Time-based search | `?start=...&end=...` |
| `/api/logs/search/advanced` | Multi-criteria | `?service=...&level=...` |

### Method 2: Direct Elasticsearch

```bash
# Get all logs
curl http://localhost:9200/logs/_search?pretty

# Search by service
curl -X POST "http://localhost:9200/logs/_search" -H 'Content-Type: application/json' -d'
{
  "query": { "term": { "service": "my-service" } }
}'
```

---

## 🛠️ Configuration

### Minimal Configuration (application.properties)

```properties
# Required
spring.kafka.bootstrap-servers=localhost:9092
spring.application.name=my-service

# Elasticsearch (with defaults)
spring.elasticsearch.uris=http://localhost:9200
```

### Full Configuration

```properties
# Elasticsearch
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.username=
spring.elasticsearch.password=
spring.elasticsearch.connection-timeout=5s
spring.elasticsearch.socket-timeout=30s

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/logs

# Redis
spring.redis.host=localhost
spring.redis.port=6379
```

---

## 📝 Usage Examples

### Example 1: Basic Logging

```java
@Service
public class UserService {
    @Autowired
    private LogAggregatorClient logClient;
    
    public void login(String userId) {
        logClient.info("User logged in: " + userId);
    }
    
    public void processPayment(String orderId) {
        try {
            // Process payment
            logClient.info("Payment processed: " + orderId);
        } catch (Exception e) {
            logClient.error("Payment failed: " + orderId, e);
        }
    }
}
```

### Example 2: Distributed Tracing

```java
@Service
public class OrderService {
    @Autowired
    private LogAggregatorClient logClient;
    
    public void processOrder(String orderId) {
        String traceId = UUID.randomUUID().toString();
        
        logClient.logWithTrace(LogLevel.INFO, "Order received: " + orderId, traceId);
        
        // Call other services with same traceId
        validateOrder(orderId, traceId);
        processPayment(orderId, traceId);
        shipOrder(orderId, traceId);
        
        logClient.logWithTrace(LogLevel.INFO, "Order completed: " + orderId, traceId);
    }
    
    private void validateOrder(String orderId, String traceId) {
        logClient.logWithTrace(LogLevel.DEBUG, "Validating order: " + orderId, traceId);
        // Validation logic
    }
}
```

### Example 3: Searching Logs

```java
@RestController
public class LogAnalyticsController {
    @Autowired
    private LogSearchService searchService;
    
    @GetMapping("/analytics/errors")
    public List<LogEntryDocument> getRecentErrors() {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant now = Instant.now();
        
        return searchService.searchByTimeRange(oneHourAgo, now, 0, 100)
                           .getContent();
    }
    
    @GetMapping("/analytics/trace/{traceId}")
    public List<LogEntryDocument> getTraceLog(@PathVariable String traceId) {
        return searchService.searchByTraceId(traceId);
    }
}
```

---

## 🧪 Testing

### Test Script (Windows)
```powershell
.\test-elasticsearch.ps1
```

### Manual Tests

```bash
# 1. Check Elasticsearch
curl http://localhost:9200

# 2. Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# 3. List indices
curl http://localhost:9200/_cat/indices?v

# 4. Count logs
curl http://localhost:9200/logs/_count

# 5. Search logs
curl "http://localhost:8080/api/logs/search/level?level=ERROR"
```

---

## 🎯 Common Use Cases

### Use Case 1: Monitor Application Errors

```bash
# Get all ERROR logs from last hour
curl "http://localhost:8080/api/logs/search/level?level=ERROR&size=100"
```

### Use Case 2: Debug a Specific Request

```bash
# Use trace ID to see all logs for a request
curl "http://localhost:8080/api/logs/search/trace/abc-123-def"
```

### Use Case 3: Analyze Service Performance

```bash
# Get all logs for a specific service
curl "http://localhost:8080/api/logs/search/service?name=payment-service&size=1000"
```

### Use Case 4: Find Specific Errors

```bash
# Search for specific error messages
curl "http://localhost:8080/api/logs/search/text?query=NullPointerException"
```

---

## 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| Elasticsearch not starting | Check Docker logs: `docker logs elasticsearch` |
| No search results | Verify logs are being indexed: `curl http://localhost:9200/logs/_count` |
| Connection timeout | Increase timeout in application.properties |
| Circuit breaker open | Check health: `curl http://localhost:8080/actuator/health` |

---

## 📚 Resources

- **Detailed Guide**: `ELASTICSEARCH_GUIDE.md`
- **Full Documentation**: `README.md`
- **Refactoring Summary**: `REFACTORING_SUMMARY.md`
- **Usage Example**: `USAGE_EXAMPLE.java`

---

## 🎬 Complete Workflow

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Verify Elasticsearch
curl http://localhost:9200

# 3. Build application
mvn clean install

# 4. Start application
mvn spring-boot:run

# 5. Send test logs (in your app)
logClient.info("Test log");
logClient.error("Test error");

# 6. Search logs
curl "http://localhost:8080/api/logs/search/text?query=Test"

# 7. View in Elasticsearch
curl "http://localhost:9200/logs/_search?pretty"
```

---

## 💡 Pro Tips

1. **Use trace IDs** for distributed tracing across microservices
2. **Set up Kibana** for visual log exploration (see ELASTICSEARCH_GUIDE.md)
3. **Monitor circuit breakers** via `/actuator/health`
4. **Use time-range searches** for performance analysis
5. **Leverage full-text search** for debugging specific issues

---

**Need Help?** Check the detailed guide: `ELASTICSEARCH_GUIDE.md`
