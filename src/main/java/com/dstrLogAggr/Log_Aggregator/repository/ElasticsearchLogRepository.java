package com.dstrLogAggr.Log_Aggregator.repository;

import com.dstrLogAggr.Log_Aggregator.model.LogEntryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch repository for advanced log searching and analytics
 */
@Repository
public interface ElasticsearchLogRepository extends ElasticsearchRepository<LogEntryDocument, String> {

    /**
     * Find logs by service name
     */
    Page<LogEntryDocument> findByService(String service, Pageable pageable);

    /**
     * Find logs by level
     */
    Page<LogEntryDocument> findByLevel(String level, Pageable pageable);

    /**
     * Find logs by trace ID for distributed tracing
     */
    List<LogEntryDocument> findByTraceId(String traceId);

    /**
     * Full-text search in log messages
     */
    @Query("{\"match\": {\"message\": \"?0\"}}")
    Page<LogEntryDocument> searchByMessage(String searchTerm, Pageable pageable);

    /**
     * Find logs within a time range
     */
    Page<LogEntryDocument> findByTimestampBetween(Instant start, Instant end, Pageable pageable);

    /**
     * Find logs by service and level
     */
    Page<LogEntryDocument> findByServiceAndLevel(String service, String level, Pageable pageable);

    /**
     * Find logs by service within a time range
     */
    Page<LogEntryDocument> findByServiceAndTimestampBetween(String service, Instant start, Instant end,
            Pageable pageable);

    /**
     * Advanced search with multiple criteria
     */
    @Query("{\"bool\": {\"must\": [{\"match\": {\"service\": \"?0\"}}, {\"match\": {\"level\": \"?1\"}}, {\"range\": {\"timestamp\": {\"gte\": \"?2\", \"lte\": \"?3\"}}}]}}")
    Page<LogEntryDocument> advancedSearch(String service, String level, Instant startTime, Instant endTime,
            Pageable pageable);
}
