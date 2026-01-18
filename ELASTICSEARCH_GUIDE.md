# Elasticsearch Configuration and Usage Guide

## Table of Contents
1. [Starting Elasticsearch](#starting-elasticsearch)
2. [Configuration](#configuration)
3. [Verifying Elasticsearch](#verifying-elasticsearch)
4. [Searching Logs](#searching-logs)
5. [Using Kibana (Optional)](#using-kibana-optional)
6. [Troubleshooting](#troubleshooting)

---

## Starting Elasticsearch

### Option 1: Using Docker Compose (Recommended)

The easiest way is to use the provided docker-compose.yml:

```bash
# Start all services including Elasticsearch
docker-compose up -d

# Check if Elasticsearch is running
docker ps | grep elasticsearch

# View Elasticsearch logs
docker logs elasticsearch
```

### Option 2: Standalone Docker

```bash
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0
```

### Option 3: Local Installation

Download from: https://www.elastic.co/downloads/elasticsearch

```bash
# Extract and run
cd elasticsearch-8.11.0
bin/elasticsearch
```

---

## Configuration

### Application Configuration

Your `application.properties` should have:

```properties
# Elasticsearch Configuration
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.username=
spring.elasticsearch.password=
spring.elasticsearch.connection-timeout=5s
spring.elasticsearch.socket-timeout=30s
```

### Environment Variables (for Docker)

```bash
# In docker-compose.yml or .env file
ELASTICSEARCH_URIS=http://elasticsearch:9200
```

### For Production (with Security)

```properties
spring.elasticsearch.uris=https://your-elasticsearch-host:9200
spring.elasticsearch.username=elastic
spring.elasticsearch.password=your-secure-password
```

---

## Verifying Elasticsearch

### 1. Check Elasticsearch Health

```bash
# Using curl
curl http://localhost:9200

# Expected response:
{
  "name" : "elasticsearch",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "...",
  "version" : {
    "number" : "8.11.0",
    ...
  },
  "tagline" : "You Know, for Search"
}
```

### 2. Check Cluster Health

```bash
curl http://localhost:9200/_cluster/health?pretty

# Expected response:
{
  "cluster_name" : "docker-cluster",
  "status" : "green",  # Should be green or yellow
  "number_of_nodes" : 1,
  ...
}
```

### 3. List All Indices

```bash
curl http://localhost:9200/_cat/indices?v

# You should see the 'logs' index after some logs are ingested
```

---

## Searching Logs

### Method 1: Using the REST API (Recommended)

The application provides convenient search endpoints:

#### 1. Search by Service Name

```bash
curl "http://localhost:8080/api/logs/search/service?name=my-service&page=0&size=20"
```

#### 2. Search by Log Level

```bash
# Get all ERROR logs
curl "http://localhost:8080/api/logs/search/level?level=ERROR&page=0&size=20"

# Get all INFO logs
curl "http://localhost:8080/api/logs/search/level?level=INFO&page=0&size=20"
```

#### 3. Full-Text Search

```bash
# Search for logs containing "database"
curl "http://localhost:8080/api/logs/search/text?query=database&page=0&size=20"

# Search for logs containing "error" or "exception"
curl "http://localhost:8080/api/logs/search/text?query=error+exception&page=0&size=20"
```

#### 4. Search by Trace ID (Distributed Tracing)

```bash
curl "http://localhost:8080/api/logs/search/trace/your-trace-id-here"
```

#### 5. Time Range Search

```bash
curl "http://localhost:8080/api/logs/search/timerange?start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z&page=0&size=20"
```

#### 6. Advanced Search (Multiple Criteria)

```bash
curl "http://localhost:8080/api/logs/search/advanced?service=my-service&level=ERROR&start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z&page=0&size=20"
```

### Method 2: Direct Elasticsearch Queries

#### Get All Logs

```bash
curl -X GET "http://localhost:9200/logs/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match_all": {}
  },
  "size": 10,
  "sort": [
    {
      "timestamp": {
        "order": "desc"
      }
    }
  ]
}
'
```

#### Search by Service

```bash
curl -X GET "http://localhost:9200/logs/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {
      "service": "my-service"
    }
  }
}
'
```

#### Search by Log Level

```bash
curl -X GET "http://localhost:9200/logs/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {
      "level": "ERROR"
    }
  }
}
'
```

#### Full-Text Search in Messages

```bash
curl -X GET "http://localhost:9200/logs/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match": {
      "message": "database connection failed"
    }
  }
}
'
```

#### Time Range Query

```bash
curl -X GET "http://localhost:9200/logs/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "range": {
      "timestamp": {
        "gte": "2024-01-01T00:00:00Z",
        "lte": "2024-01-02T00:00:00Z"
      }
    }
  }
}
'
```

#### Complex Query (Multiple Conditions)

```bash
curl -X GET "http://localhost:9200/logs/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        { "term": { "service": "my-service" } },
        { "term": { "level": "ERROR" } }
      ],
      "filter": [
        {
          "range": {
            "timestamp": {
              "gte": "now-1h"
            }
          }
        }
      ]
    }
  }
}
'
```

#### Aggregation Query (Count by Level)

```bash
curl -X GET "http://localhost:9200/logs/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "size": 0,
  "aggs": {
    "logs_by_level": {
      "terms": {
        "field": "level",
        "size": 10
      }
    }
  }
}
'
```

### Method 3: Using PowerShell (Windows)

```powershell
# Search by service
Invoke-RestMethod -Uri "http://localhost:8080/api/logs/search/service?name=my-service" -Method Get

# Search ERROR logs
Invoke-RestMethod -Uri "http://localhost:8080/api/logs/search/level?level=ERROR" -Method Get

# Full-text search
Invoke-RestMethod -Uri "http://localhost:8080/api/logs/search/text?query=database" -Method Get
```

---

## Using Kibana (Optional)

Kibana provides a powerful UI for Elasticsearch. To add it:

### 1. Add Kibana to docker-compose.yml

```yaml
  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - app-network
```

### 2. Start Kibana

```bash
docker-compose up -d kibana
```

### 3. Access Kibana

Open your browser: http://localhost:5601

### 4. Create Index Pattern

1. Go to **Management** → **Stack Management** → **Index Patterns**
2. Click **Create index pattern**
3. Enter `logs*` as the pattern
4. Select `timestamp` as the time field
5. Click **Create index pattern**

### 5. Discover Logs

1. Go to **Analytics** → **Discover**
2. Select the `logs*` index pattern
3. Use the search bar to query logs
4. Apply filters and time ranges

---

## Practical Examples

### Example 1: Find All Errors in the Last Hour

```bash
curl "http://localhost:8080/api/logs/search/level?level=ERROR" | jq '.content[] | select(.timestamp > (now - 3600))'
```

### Example 2: Track a Request Across Services

```bash
# Use the same trace ID across all services
TRACE_ID="abc-123-def"
curl "http://localhost:8080/api/logs/search/trace/${TRACE_ID}"
```

### Example 3: Monitor a Specific Service

```bash
# Get recent logs for a service
curl "http://localhost:8080/api/logs/search/service?name=payment-service&page=0&size=50"
```

### Example 4: Debug an Issue

```bash
# Search for specific error message
curl "http://localhost:8080/api/logs/search/text?query=NullPointerException"
```

---

## Testing the Setup

### 1. Send Test Logs

Use the LogAggregatorClient in your application:

```java
@Autowired
private LogAggregatorClient logClient;

public void testLogging() {
    logClient.info("Test INFO log");
    logClient.warn("Test WARNING log");
    logClient.error("Test ERROR log");
    
    // With trace ID
    String traceId = UUID.randomUUID().toString();
    logClient.logWithTrace(LogLevel.INFO, "Test with trace", traceId);
}
```

### 2. Verify in Elasticsearch

```bash
# Wait a few seconds for indexing, then search
curl "http://localhost:8080/api/logs/search/text?query=Test"
```

### 3. Check Index Statistics

```bash
curl "http://localhost:9200/logs/_stats?pretty"
```

---

## Troubleshooting

### Issue 1: Elasticsearch Not Starting

**Check logs:**
```bash
docker logs elasticsearch
```

**Common fixes:**
- Increase Docker memory to at least 4GB
- Check port 9200 is not in use: `netstat -ano | findstr :9200`
- Verify disk space is available

### Issue 2: No Data in Elasticsearch

**Check if logs are being indexed:**
```bash
curl "http://localhost:9200/logs/_count?pretty"
```

**Verify application is connected:**
```bash
# Check application logs for Elasticsearch connection errors
docker logs log-aggregator
```

**Check circuit breaker status:**
```bash
curl "http://localhost:8080/actuator/health"
```

### Issue 3: Search Returns Empty Results

**Verify index exists:**
```bash
curl "http://localhost:9200/_cat/indices?v"
```

**Check index mapping:**
```bash
curl "http://localhost:9200/logs/_mapping?pretty"
```

**Refresh index:**
```bash
curl -X POST "http://localhost:9200/logs/_refresh"
```

### Issue 4: Connection Timeout

**Increase timeout in application.properties:**
```properties
spring.elasticsearch.connection-timeout=10s
spring.elasticsearch.socket-timeout=60s
```

---

## Performance Tips

### 1. Bulk Indexing

For high-volume scenarios, logs are automatically batched by Kafka.

### 2. Index Lifecycle Management

Set up index rotation to manage disk space:

```bash
# Create index template with lifecycle policy
curl -X PUT "http://localhost:9200/_index_template/logs_template" -H 'Content-Type: application/json' -d'
{
  "index_patterns": ["logs*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    }
  }
}
'
```

### 3. Query Optimization

- Use filters instead of queries when possible
- Limit result size with pagination
- Use specific field searches instead of full-text when possible

---

## Monitoring

### Check Elasticsearch Performance

```bash
# Node stats
curl "http://localhost:9200/_nodes/stats?pretty"

# Index stats
curl "http://localhost:9200/logs/_stats?pretty"

# Cluster health
curl "http://localhost:9200/_cluster/health?pretty"
```

### Application Health

```bash
# Check all circuit breakers
curl "http://localhost:8080/actuator/health"

# Check metrics
curl "http://localhost:8080/actuator/metrics"
```

---

## Quick Reference

### Essential URLs

- **Elasticsearch**: http://localhost:9200
- **Kibana** (if installed): http://localhost:5601
- **Application API**: http://localhost:8080
- **Search API**: http://localhost:8080/api/logs/search/*
- **Health Check**: http://localhost:8080/actuator/health

### Common Commands

```bash
# Start services
docker-compose up -d

# Check Elasticsearch
curl http://localhost:9200

# Search ERROR logs
curl "http://localhost:8080/api/logs/search/level?level=ERROR"

# View all indices
curl "http://localhost:9200/_cat/indices?v"

# Stop services
docker-compose down
```

---

## Next Steps

1. **Start Elasticsearch**: `docker-compose up -d`
2. **Verify it's running**: `curl http://localhost:9200`
3. **Send some test logs** using LogAggregatorClient
4. **Search for logs**: Use the REST API endpoints
5. **(Optional) Install Kibana** for visual exploration

For more details, see the main README.md file.
