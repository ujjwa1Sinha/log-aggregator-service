package com.dstrLogAggr.Log_Aggregator.controller;

import com.dstrLogAggr.Log_Aggregator.model.LogEntryDocument;
import com.dstrLogAggr.Log_Aggregator.service.LogSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST API controller for searching logs using Elasticsearch.
 * Provides various endpoints for querying and analyzing logs.
 */
@RestController
@RequestMapping("/api/logs")
public class LogSearchController {

    @Autowired
    private LogSearchService searchService;

    /**
     * Search logs by service name
     * GET /api/logs/search/service?name=my-service&page=0&size=20
     */
    @GetMapping("/search/service")
    public ResponseEntity<Page<LogEntryDocument>> searchByService(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LogEntryDocument> results = searchService.searchByService(name, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Search logs by level (INFO, WARN, ERROR, DEBUG)
     * GET /api/logs/search/level?level=ERROR&page=0&size=20
     */
    @GetMapping("/search/level")
    public ResponseEntity<Page<LogEntryDocument>> searchByLevel(
            @RequestParam String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LogEntryDocument> results = searchService.searchByLevel(level, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Search logs by trace ID for distributed tracing
     * GET /api/logs/search/trace/abc-123-def
     */
    @GetMapping("/search/trace/{traceId}")
    public ResponseEntity<List<LogEntryDocument>> searchByTraceId(@PathVariable String traceId) {
        List<LogEntryDocument> results = searchService.searchByTraceId(traceId);
        return ResponseEntity.ok(results);
    }

    /**
     * Full-text search in log messages
     * GET /api/logs/search/text?query=database+error&page=0&size=20
     */
    @GetMapping("/search/text")
    public ResponseEntity<Page<LogEntryDocument>> fullTextSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LogEntryDocument> results = searchService.fullTextSearch(query, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Search logs within a time range
     * GET
     * /api/logs/search/timerange?start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z&page=0&size=20
     */
    @GetMapping("/search/timerange")
    public ResponseEntity<Page<LogEntryDocument>> searchByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LogEntryDocument> results = searchService.searchByTimeRange(start, end, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Advanced search with multiple criteria
     * GET
     * /api/logs/search/advanced?service=my-service&level=ERROR&start=2024-01-01T00:00:00Z&end=2024-01-02T00:00:00Z&page=0&size=20
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<Page<LogEntryDocument>> advancedSearch(
            @RequestParam String service,
            @RequestParam String level,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LogEntryDocument> results = searchService.advancedSearch(service, level, start, end, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Search logs by service and level
     * GET
     * /api/logs/search/service-level?service=my-service&level=ERROR&page=0&size=20
     */
    @GetMapping("/search/service-level")
    public ResponseEntity<Page<LogEntryDocument>> searchByServiceAndLevel(
            @RequestParam String service,
            @RequestParam String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LogEntryDocument> results = searchService.searchByServiceAndLevel(service, level, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Health check endpoint
     * GET /api/logs/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Log Search API is running");
    }
}
