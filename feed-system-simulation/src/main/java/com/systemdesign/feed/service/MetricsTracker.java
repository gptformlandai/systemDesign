package com.systemdesign.feed.service;

import org.springframework.stereotype.Service;
import lombok.Data;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Service
@Data
public class MetricsTracker {
    private final AtomicLong dbWrites = new AtomicLong(0);
    private final AtomicLong dbReads = new AtomicLong(0);
    private final AtomicLong kafkaEventsPublished = new AtomicLong(0);
    private final AtomicLong kafkaEventsConsumed = new AtomicLong(0);
    
    // Timings in milliseconds for feed retrieval per strategy
    private final ConcurrentHashMap<String, List<Long>> readLatencies = new ConcurrentHashMap<>();
    
    // Timings in milliseconds for write operations
    private final ConcurrentHashMap<String, List<Long>> writeLatencies = new ConcurrentHashMap<>();

    public void incrementDbWrites(long delta) {
        dbWrites.addAndGet(delta);
    }

    public void incrementDbReads(long delta) {
        dbReads.addAndGet(delta);
    }

    public void incrementKafkaPublished() {
        kafkaEventsPublished.incrementAndGet();
    }

    public void incrementKafkaConsumed() {
        kafkaEventsConsumed.incrementAndGet();
    }

    public void recordReadLatency(String strategy, long latencyMs) {
        readLatencies.computeIfAbsent(strategy, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(latencyMs);
    }

    public void recordWriteLatency(String operation, long latencyMs) {
        writeLatencies.computeIfAbsent(operation, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(latencyMs);
    }

    public void reset() {
        dbWrites.set(0);
        dbReads.set(0);
        kafkaEventsPublished.set(0);
        kafkaEventsConsumed.set(0);
        readLatencies.clear();
        writeLatencies.clear();
    }
}
