package com.dstrLogAggr.Log_Aggregator.repository;

import com.dstrLogAggr.Log_Aggregator.model.LogEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogBackRepo extends MongoRepository<LogEntry, String> {

    // Custom query examples:

    List<LogEntry> findByService(String service);

    List<LogEntry> findByLevel(String level);

    List<LogEntry> findByTraceId(String traceId);

    List<LogEntry> findByMessageContaining(String keyword);
}
