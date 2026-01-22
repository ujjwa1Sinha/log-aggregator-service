package com.dstrLogAggr.Log_Aggregator.repository;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogBackRepo extends MongoRepository<LogEntry, String> {

    // Custom query examples with pagination support for fallback scenarios:

    Page<LogEntry> findByService(String service, Pageable pageable);

    Page<LogEntry> findByLevel(String level, Pageable pageable);

    List<LogEntry> findByTraceId(String traceId);

    Page<LogEntry> findByMessageContaining(String keyword, Pageable pageable);

    // Time-based queries (timestamp is stored as String in MongoDB)
    @Query("{ 'timestamp' : { $gte: ?0, $lte: ?1 } }")
    Page<LogEntry> findByTimestampBetween(String startTime, String endTime, Pageable pageable);

    // Combined queries for advanced search
    Page<LogEntry> findByServiceAndLevel(String service, String level, Pageable pageable);

    @Query("{ 'service': ?0, 'level': ?1, 'timestamp': { $gte: ?2, $lte: ?3 } }")
    Page<LogEntry> findByServiceAndLevelAndTimestampBetween(String service, String level, String startTime,
            String endTime, Pageable pageable);
}
