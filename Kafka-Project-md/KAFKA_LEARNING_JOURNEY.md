# 🚀 Kafka Learning Journey: PRE-NG Latency Intelligence System

**Goal**: Build an intelligent asynchronous latency tracking and LLM analysis system using Kafka that enables real-time performance insights without impacting request latency.

**Current Date**: June 22, 2026

---

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Why Kafka for Latency Tracking?](#why-kafka-for-latency-tracking)
3. [Core Concepts Introduced](#core-concepts-introduced)
4. [File-by-File Technical Deep Dive](#file-by-file-technical-deep-dive)
5. [Context Propagation & ThreadLocal Magic](#context-propagation--threadlocal-magic)
6. [Kafka Message Flow](#kafka-message-flow)
7. [Complete Technical Concepts](#complete-technical-concepts)
8. [Design Patterns Used](#design-patterns-used)
9. [Performance Considerations](#performance-considerations)
10. [Common Pitfalls & Solutions](#common-pitfalls--solutions)

---

## System Architecture Overview

### High-Level Vision

```
┌──────────────────────────────────────────────────────────────────────┐
│                      PRE-NG Application                             │
│                   (Member Provider Search)                          │
│                                                                      │
│  GPS Request → [PES Call] → [COSMOS Call] → [DROOLS Call] → Response│
│                    ↓             ↓              ↓                    │
│            Latency: 4373ms  Latency: 2466ms  Latency: 849ms         │
│                    ↓             ↓              ↓                    │
│            ┌────────────────────────────────────────────────┐        │
│            │ LatencyIntelligenceService (Async Handler)    │        │
│            │  → Serializes event to JSON                   │        │
│            │  → Publishes to Kafka (FIRE & FORGET)         │        │
│            │  → NO blocking of request thread              │        │
│            └────────────────────────────────────────────────┘        │
│                              ↓                                       │
│                     Topic: pre-ng.latency.events                     │
│                              ↓                                       │
│          ┌────────────────────────────────────────────────┐          │
│          │  Python LLM Consumer Microservice              │          │
│          │  → Consumes latency events                     │          │
│          │  → Analyzes patterns with LLM                  │          │
│          │  → Identifies bottlenecks and optimizations    │          │
│          │  → Produces intelligence insights              │          │
│          └────────────────────────────────────────────────┘          │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Why Kafka for Latency Tracking?

### Problem We're Solving

**Before Kafka**:
- External service calls (PES, COSMOS, DROOLS) were not instrumented for latency tracking
- No way to correlate which calls took longest across the entire request pipeline
- Performance issues were reactive: wait for Splunk logs, then investigate
- No machine learning on latency patterns

**After Kafka**:
- ✅ Every external call is automatically tracked with rich context
- ✅ Requests are correlated via requestId across all services
- ✅ Data flows asynchronously → zero impact on request latency
- ✅ LLM can analyze patterns in real-time and suggest optimizations
- ✅ Full history of JVM state when slowness occurred

### Key Advantages of Async Queue Approach

```
┌──────────────────────────────────┐
│  Request Thread                  │
│  (reactor-http-nio-3)            │
│                                  │
│  1. Call PES → 4373ms            │
│  2. Call COSMOS → 2466ms         │
│  3. Call DROOLS → 849ms          │
│  4. reportLatency(event) ← calls this
│     ├─ Grabs requestId from ThreadLocal (~0.1ms)
│     └─ Submits to ExecutorService queue (~0.05ms)
│        → Thread returns IMMEDIATELY
│                                  │
│  5. Build response → 5ms         │
│  6. Send to client → 2ms         │
│                                  │
│  ⏱️ TOTAL: 7295ms                │
└──────────────────────────────────┘

┌──────────────────────────────────┐
│  Kafka-Latency Thread Pool       │
│  (kafka-latency-<nanoTime>)      │
│                                  │
│  [Waiting for work queue items]  │
│                                  │
│  When Kafka event arrives:       │
│  1. Truncate payload → 5ms       │
│  2. Capture JVM metrics → 0.5ms  │
│  3. Serialize to JSON → 10ms     │
│  4. Send to Kafka → 2ms          │
│                                  │
│  ⏱️ TOTAL: 17.5ms (NOT blocking│
│             request thread)      │
└──────────────────────────────────┘
```

**Result**: Request completes in 7295ms instead of 7313ms
**Impact on User**: **NONE** (18ms difference is imperceptible)
**Benefit**: Full latency intelligence for LLM analysis

---

## Core Concepts Introduced

### 1️⃣ **Kafka Producer-Consumer Model**

```
                  Kafka Broker
                ┌─────────────────────┐
                │ Topic: pre-ng.       │
                │ latency.events       │
                │                     │
                │ [Partition 0]       │
                │ [Partition 1]       │
                │ [Partition 2]       │
                └─────────────────────┘
                    ↑          ↓
        ┌───────────┴──────────┴──────────┐
        │                                 │
    PRODUCER                         CONSUMER
  (PRE-NG Java                   (Python LLM)
   App sends                      reads and
   events)                        analyzes)
```

**Key Concepts**:
- **Topic**: `pre-ng.latency.events` — single topic for ALL events
- **Partitions**: 3 partitions for parallel consumption
- **Key**: `requestId` — ensures all events for same request go to same partition
- **Value**: JSON string containing complete LatencyEvent

### 2️⃣ **Request ID Context Propagation**

The requestId must flow through:
1. Controller (HTTP entry point)
2. All service calls (PES, COSMOS, DROOLS)
3. Kafka event publishing
4. Python consumer for correlation

**Two Mechanisms**:

**ThreadLocal** (for synchronous code):
```java
// Set at request start
RequestIdContext.set(requestId);  // stores in ThreadLocal

// Access in any service on same thread
Long requestId = RequestIdContext.get();  // retrieves from ThreadLocal

// Clear at request end
RequestIdContext.clear();  // removes from ThreadLocal
```

**Reactor Context** (for async/reactive code):
```java
// Set in reactive flow
return Mono.just(result)
    .contextWrite(Context.of(REQUEST_ID_KEY, requestId))
    .doFinally(signal -> RequestIdContext.clear());

// Access in reactive code
Mono.deferContextual(ctx -> {
    Long requestId = RequestIdContext.getFromContext(ctx);
    // ...
});
```

### 3️⃣ **Async Offloading with ExecutorService**

```
┌────────────────────────────────────────────────────────┐
│  ExecutorService Thread Pool                           │
│  • Core threads: 2                                     │
│  • Max threads: 4                                      │
│  • Queue capacity: 10,000                              │
│  • Keep-alive: 60 seconds                              │
│  • Rejection policy: DiscardOldest                     │
│  • Daemon threads: YES (won't prevent JVM shutdown)    │
└────────────────────────────────────────────────────────┘

Kafka Executor Lifecycle:
├─ @PreDestroy → Graceful shutdown
│  ├─ Calls executor.shutdown() → no new tasks
│  ├─ awaitTermination(5s) → waits for queue to drain
│  └─ If timeout → shutdownNow() → forces shutdown
└─ Queue grows from 0 to ~10,000 items during peak
   └─ Each item = ~1 LatencyEvent (~10KB JSON)
```

### 4️⃣ **ThreadLocal vs Reactor Context: The Complexity**

**Problem**: Spring WebFlux is reactive (non-blocking), but ThreadLocal is thread-specific.

When you do:
```java
reactor.http.nio-1 calls service
  → .parallel() switches to boundedElastic-1
    → ThreadLocal is LOST! (different thread)
```

**Solution**: Use Reactor Context
```java
Mono.just(data)
    .contextWrite(Context.of(REQUEST_ID_KEY, requestId))
    // Context is propagated across thread switches!
    .flatMap(v -> Mono.deferContextual(ctx -> {
        Long requestId = ctx.get(REQUEST_ID_KEY);
        // Can access requestId here even on different thread
    }))
```

### 5️⃣ **Fire-and-Forget with Graceful Handling**

```java
// Producer sends message
kafkaTemplate.send(topic, key, value)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.warn("Failed to send: {}", ex.getMessage());
            // Don't throw! Just log and continue
        } else {
            log.debug("Sent to partition {}, offset {}",
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
        }
    });
```

**Why "fire-and-forget"?**
- Request thread doesn't wait for Kafka broker ACK
- If Kafka is slow, request completes anyway
- Message may be delivered asynchronously (best-effort)
- Errors are logged, never thrown to caller

### 6️⃣ **Payload Truncation Strategy**

Not all payloads fit in Kafka messages:

```
Request Payload:    Small (~1-5KB)    ✅ Send full
Response Payload:   Large (~50-350KB) ⚠️  Truncate at 500KB
                    Huge  (~1MB+)     ❌ Truncate & warn

Truncation Logic:
├─ Store originalResponseSizeBytes (actual size)
├─ Set responseWasTruncated = true
├─ Keep first 250KB + truncation marker
└─ Python consumer can identify truncated data
```

---

## File-by-File Technical Deep Dive

### 1. **LatencyEvent.java** — The Data Structure

**Purpose**: Represents a single external service call (PES, COSMOS, DROOLS).

**Field Categories**:

```java
// 1. IDENTITY — What happened?
String serviceName;           // PES, COSMOS, DROOLS, MAPS
String serviceUrl;            // Actual URL hit
String clientName;            // GPS, IIM, DSNP, etc.

// 2. TIMING — When and how long?
long latencyMs;               // Call duration
String timestamp;             // ISO-8601 instant
String dayOfWeek;             // MONDAY, TUESDAY, etc.
int hourOfDay;                // 0-23 (Central Time)
long thresholdMs;             // Threshold that was exceeded

// 3. CONTEXT — What was requested?
String requestContext;        // Key=value pairs: "zip=85143,div=PNX"
String requestPayload;        // Full request JSON (usually small)

// 4. RESULT — What came back?
boolean success;              // Did call succeed?
int httpStatusCode;           // 200, 503, 429, etc.
String errorMessage;          // If failed, why?
String responsePayload;       // Full response JSON (may be truncated)
int responseItemCount;        // Count of items in response

// 5. CORRELATION — Link to other events
Long requestId;               // Database audit request ID
String pipelineStage;         // "PES_SEARCH", "COSMOS_VALIDATION", etc.

// 6. DIAGNOSTICS — What might explain slowness?
int retryAttempt;             // 0=first try, 1=first retry, etc.
int requestItemCount;         // How many items in request
int searchRadiusMiles;        // PES search radius
int filteredOutCount;         // Items removed by filters

// 7. ENVIRONMENT — JVM state at time of call
double heapUsagePercent;      // 0-100
int activeThreadCount;        // Current thread count
String eventType;             // "EXTERNAL_CALL" or "PIPELINE_SUMMARY"
```

**Why so many fields?**

LLM needs context to provide actionable insights:

```
WITHOUT context:
  "PES was slow (4373ms)"
  → No actionable info

WITH context:
  "PES took 4373ms with 50 providers requested
   on Monday at 7am, zip=85143 (dense urban area),
   heapUsage=92%, only 1 active thread (others blocked),
   this is the 3rd retry"
  → LLM can say: "Heap pressure likely caused GC pauses;
                 consider caching provider density maps"
```

---

### 2. **LatencyIntelligenceService.java** — The Brain

**Purpose**: Decides WHEN to publish events and handles all async work.

#### Key Methods:

```java
// SIMPLE API — minimal parameters
reportLatency(String service, long latencyMs, String client, String context)

// RICH API — full LLM-optimized event
reportLatency(LatencyEvent event)

// RAW OBJECTS API — prevents blocking reactor thread
reportLatencyWithRawPayloads(LatencyEvent event, Object rawRequest, Object rawResponse)

// FAILURE API — always sent regardless of threshold
reportFailure(String service, long latencyMs, String client, String context, String error)

// PIPELINE API — end-to-end summary
reportPipelineSummary(PipelineSummaryEvent summary)
```

#### Async Execution Model:

```java
public void reportLatency(LatencyEvent event) {
    // STEP 1: On caller thread (~1ms)
    if (event.getRequestId() == null) {
        event.setRequestId(RequestIdContext.get());  // grab from ThreadLocal
    }
    log.debug("Queueing event: service={}, latency={}ms", 
              event.getServiceName(), event.getLatencyMs());

    // STEP 2: Submit to async executor
    kafkaExecutor.execute(() -> {
        // This runs on kafka-latency thread, NOT reactor thread
        try {
            processAndSendEvent(event);  // Heavy lifting
        } catch (Exception e) {
            log.warn("Async processing failed: {}", e.getMessage());
        }
    });
    // Caller thread returns IMMEDIATELY after submit
}

private void processAndSendEvent(LatencyEvent event) {
    // All heavy work happens here on kafka-latency thread:
    
    // 1. Payload truncation
    handlePayloadTruncation(event);           // ~5ms
    handleRequestPayloadTruncation(event);    // ~2ms
    
    // 2. JVM metrics
    if (event.getHeapUsagePercent() == 0) {
        captureJvmMetrics(event);             // ~0.5ms
    }
    
    // 3. JSON serialization
    String eventJson = objectMapper.writeValueAsString(event);  // ~10ms
    
    // 4. Kafka send (fire-and-forget)
    String kafkaKey = event.getRequestId().toString();
    kafkaProducer.send(kafkaKey, eventJson);
}
```

#### ExecutorService Configuration:

```java
ExecutorService kafkaExecutor = new ThreadPoolExecutor(
    2,                              // core pool size (always running)
    4,                              // max pool size (under load)
    60L, TimeUnit.SECONDS,          // idle thread keep-alive
    new LinkedBlockingQueue<>(10_000),  // queue capacity
    r -> {
        Thread t = new Thread(r, "kafka-latency-" + System.nanoTime());
        t.setDaemon(true);          // Won't prevent JVM shutdown
        return t;
    },
    new ThreadPoolExecutor.DiscardOldestPolicy()  // Drop oldest if queue full
);
```

**Thread Naming**: `kafka-latency-<nanoTime>`
- Makes debugging easier in thread dumps
- Each thread gets unique ID
- Can filter logs: `grep "kafka-latency"`

**Queue Capacity**: 10,000 events × ~10KB = ~100MB worst-case memory
**Rejection Policy**: `DiscardOldestPolicy`
- Under extreme load, drop oldest events first
- Recent events are more valuable than stale ones
- Prevents memory runaway

#### Graceful Shutdown:

```java
@PreDestroy
public void shutdown() {
    log.info("Shutting down kafka-latency executor...");
    kafkaExecutor.shutdown();  // Stop accepting new tasks
    
    try {
        // Wait up to 5 seconds for queue to drain
        if (!kafkaExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.warn("Did not drain in 5s — {} events may be lost",
                    ((ThreadPoolExecutor) kafkaExecutor).getQueue().size());
            kafkaExecutor.shutdownNow();  // Force shutdown
        }
    } catch (InterruptedException e) {
        kafkaExecutor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

**Why important?**
- Without graceful shutdown, pending Kafka events are dropped
- With this, up to 5 seconds worth of events are flushed to Kafka

---

### 3. **LatencyKafkaProducer.java** — The Sender

**Purpose**: Sends events to Kafka. Fire-and-forget pattern.

```java
public void send(String key, String value) {
    sendToTopic(KafkaTopicConfig.LATENCY_EVENTS_TOPIC, key, value);
}

private void sendToTopic(String topic, String key, String value) {
    try {
        kafkaTemplate.send(topic, key, value)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Failed to send to {}: {}", topic, ex.getMessage());
                } else {
                    log.debug("Sent to partition: {}, offset: {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    } catch (Exception e) {
        log.warn("Exception sending to Kafka: {}", e.getMessage());
    }
}
```

**Key Points**:
- Uses `KafkaTemplate.send()` which is async
- `.whenComplete()` callback handles success/failure
- Errors are **never thrown** — just logged
- Request thread doesn't wait for completion

---

### 4. **RequestIdContext.java** — Context Holder

**Purpose**: Store and retrieve requestId in ThreadLocal or Reactor Context.

```java
public class RequestIdContext {
    private static final String REQUEST_ID_KEY = "requestId";
    private static final ThreadLocal<Long> requestIdHolder = new ThreadLocal<>();

    // For synchronous code
    public static void set(Long requestId) {
        requestIdHolder.set(requestId);
    }

    public static Long get() {
        return requestIdHolder.get();
    }

    // For reactive code
    public static Long getFromContext(Context context) {
        return context.getOrDefault(REQUEST_ID_KEY, null);
    }

    public static void clear() {
        requestIdHolder.remove();
    }
}
```

**Usage Pattern**:

```java
// In Controller (entry point)
final Long requestId = auditRequestAndLog(request);  // Get from DB audit
RequestIdContext.set(requestId);  // Store in ThreadLocal

// In Service (downstream)
Long requestId = RequestIdContext.get();  // Retrieve from ThreadLocal
// Use for Kafka event

// At end of request
RequestIdContext.clear();  // Clean up ThreadLocal
```

---

### 5. **KafkaTopicConfig.java** — Topic Setup

**Purpose**: Auto-create Kafka topic on application startup.

```java
@Configuration
public class KafkaTopicConfig {
    public static final String LATENCY_EVENTS_TOPIC = "pre-ng.latency.events";

    @Bean
    public NewTopic latencyEventsTopic() {
        return TopicBuilder.name(LATENCY_EVENTS_TOPIC)
                .partitions(3)      // 3 partitions for parallelism
                .replicas(1)        // Local dev: 1 replica
                .build();
    }
}
```

**Why 3 partitions?**
```
Partitioning Strategy:
├─ Partition 0 → Requests with requestId % 3 = 0
├─ Partition 1 → Requests with requestId % 3 = 1
└─ Partition 2 → Requests with requestId % 3 = 2

Benefit:
- Python consumer can run 3 parallel processes
- Each consumes from one partition
- All events for same requestId go to same partition → ordered
```

**Why 1 replica locally?**
- Local dev doesn't need redundancy
- Prod would use 3 replicas for fault tolerance

---

### 6. **LatencyEventConsumer.java** — Local Debug Listener

**Purpose**: Local-only consumer that prints received events.

```java
@Log4j2
@Service
@Profile("local")  // Only runs when spring.profiles.active=local
public class LatencyEventConsumer {

    @KafkaListener(
        topics = KafkaTopicConfig.LATENCY_EVENTS_TOPIC,
        groupId = "pre-ng-local-debugger"
    )
    public void onLatencyEvent(ConsumerRecord<String, String> record) {
        // Distinguish event type
        String eventType = record.value().contains("PIPELINE_SUMMARY") 
            ? "📊 PIPELINE_SUMMARY" 
            : "⚡ EXTERNAL_CALL";

        log.info("{} ========== EVENT RECEIVED ==========", eventType);
        log.info("   Topic:          {}", record.topic());
        log.info("   Key (requestId):{}", record.key());
        log.info("   Partition:      {}", record.partition());
        log.info("   Offset:         {}", record.offset());
        log.info("   Value (payload):{}", record.value());
    }
}
```

**Why @Profile("local")?**
- In production, Python LLM consumer handles consumption
- In development, this Spring service helps debug
- Prevents duplicate consumption in prod

---

### 7. **PipelineTracker.java** — Accumulator

**Purpose**: Collect timing data throughout request, produce summary at end.

#### Initialization:

```java
PipelineTracker tracker = new PipelineTracker("GPS", requestId);
// Starts clock: this.startTimeNanos = System.nanoTime();
```

#### Recording Stages:

```java
tracker.recordStage("PES_SEARCH", 4373, 50);     // 4373ms, 50 items
tracker.recordStage("COSMOS_CALL", 2466, 1);    // 2466ms, 1 batch
tracker.recordStage("DROOLS_CALL", 849, 1);     // 849ms, 1 batch
```

#### Recording Provider Funnel:

```java
tracker.recordPesResult(50, 40);     // PES returned 50, 40 after medicare filter
tracker.recordCosmosResult(35);      // COSMOS kept 35 valid
tracker.recordDroolsResult(35);      // DROOLS kept 35
tracker.recordFinalCount(10);        // Top 10 sent to client
```

#### Building Summary:

```java
PipelineSummaryEvent summary = tracker.buildSummary();
// Automatically calculates:
// - totalPipelineMs = current time - start time
// - externalCallsTotalMs = 4373 + 2466 + 849 = 7688ms
// - internalProcessingMs = totalPipelineMs - externalCallsTotalMs
// - Captures comprehensive JVM metrics
```

#### JVM Metrics Captured:

```
Memory:
├─ heapUsagePercent (0-100)
├─ heapUsedMb, heapCommittedMb, heapMaxMb
├─ nonHeapUsedMb
└─ memoryPoolUsageMb (per pool breakdown)

Garbage Collection:
├─ gcTotalCollections
├─ gcTotalTimeMs
└─ gcCollectorStats (per collector breakdown)

Threads:
├─ activeThreadCount
├─ peakThreadCount
├─ totalStartedThreadCount
└─ threadStateDistribution (RUNNABLE, BLOCKED, WAITING, etc.)

CPU:
├─ systemCpuLoad (1-min average)
├─ processCpuLoad (JVM only)
└─ availableProcessors

Class Loading:
├─ loadedClassCount
├─ totalLoadedClassCount
└─ unloadedClassCount

Uptime:
└─ jvmUptimeSeconds
```

**Why so much detail?**

```
Example: LLM detects latency spike

Without JVM metrics:
  "Request took 15 seconds"
  → Can't diagnose

With JVM metrics:
  "Request took 15 seconds
   gcTotalCollections increased by 5 (expensive)
   systemCpuLoad = 7.8 on 8-core (saturated)
   heapUsagePercent = 91% (triggering Full GC)
   threadStateDistribution: 100 BLOCKED (lock contention?)"
  → LLM diagnoses: "Full GC pause + CPU saturation
                    → increase heap or reduce garbage creation"
```

---

### 8. **PipelineSummaryEvent.java** — Complete Picture

**Purpose**: Represents the full end-to-end request breakdown.

**Sections**:

```java
// IDENTITY
String eventType;          // "PIPELINE_SUMMARY"
String clientName;         // GPS, IIM, etc.
Long requestId;            // Correlates to LatencyEvents

// TIMING BREAKDOWN
long totalPipelineMs;      // 9858ms total
long externalCallsTotalMs; // 7489ms (PES + COSMOS + DROOLS)
long internalProcessingMs; // 2369ms (DB, filtering, etc.)

// INDIVIDUAL CALL BREAKDOWN
long pesLatencyMs;         // 3949ms
long cosmosLatencyMs;      // 2740ms
long droolsLatencyMs;      // 800ms
long mapsLatencyMs;        // 0ms (if called)

// PROVIDER FUNNEL
int pesReturnedCount;           // 50
int afterMedicareFilterCount;   // 40
int afterCosmosCount;           // 35
int afterDroolsCount;           // 35
int finalRecommendationCount;   // 10

// FLOW DECISION PATH
String flowPath;           // "FRESH_SEARCH", "PRIOR_PCP_LOOKBACK", etc.
int externalCallCount;     // Number of external calls made
boolean hadFallback;       // Did any search fail and fallback?

// REQUEST CHARACTERISTICS
String memberZip;          // 85143
String memberStateCode;    // AZ
String memberDiv;          // PNX
String memberPlanName;     // MRA, GPS, etc.
int searchRadiusMiles;     // 35
boolean hadPriorPcp;       // Prior coverage?
boolean hadRequestedProviderId;  // Specific NPI requested?
boolean hadRequestedProviderName; // Name search?

// OUTCOME
boolean success;           // Did request complete successfully?
String errorMessage;       // If failed, why?
String outcomeDescription; // "PCP assigned", "No PCPs found"
String finalResponsePayload;      // Full response JSON
long finalResponseSizeBytes;      // 347759 bytes
boolean finalResponseWasTruncated; // true if > 500KB

// JVM METRICS (see section 7 above)
double heapUsagePercent;
long heapUsedMb;
// ... 20+ more metrics

// STAGE TIMINGS (detailed breakdown)
List<StageTimingEntry> stageTimings;
// [
//   {stage: "DB_AUDIT_REQUEST", durationMs: 45},
//   {stage: "CONTROLLER_VALIDATION", durationMs: 3},
//   {stage: "PODM_LOOKUP", durationMs: 15},
//   {stage: "NDAR_LOOKUP", durationMs: 10},
//   {stage: "PES_REQUEST_MAPPING", durationMs: 2},
//   {stage: "PES_SEARCH", durationMs: 3949},
//   ...
// ]
```

---

### 9. **PipelineTrackerContext.java** — ThreadLocal Wrapper

**Purpose**: Make PipelineTracker accessible from ANY service without passing as parameter.

```java
public class PipelineTrackerContext {
    private static final ThreadLocal<PipelineTracker> TRACKER = new ThreadLocal<>();
    private static final ThreadLocal<long[]> CONTROLLER_TIMINGS = new ThreadLocal<>();

    // Store/retrieve tracker
    public static void set(PipelineTracker tracker) { TRACKER.set(tracker); }
    public static PipelineTracker get() { return TRACKER.get(); }
    public static void clear() { TRACKER.remove(); }

    // Convenience: record stage without retrieving tracker first
    public static void recordStage(String stageName, long durationMs) {
        PipelineTracker tracker = TRACKER.get();
        if (tracker != null) {
            tracker.recordInternalStage(stageName, durationMs);
        }
    }

    // Similar for external stages...
}
```

**Why this pattern?**

Without it:
```java
// Method signature grows with every new dependency
public void doSearch(
    String zip,
    PipelineTracker tracker,    // passed through 5+ method layers
    LatencyService latency,
    CosmosService cosmos,
    // ... 10 more parameters
) { }
```

With ThreadLocal:
```java
// Method stays clean
public void doSearch(String zip) {
    long start = System.nanoTime();
    // ... work ...
    long ms = (System.nanoTime() - start) / 1_000_000;
    PipelineTrackerContext.recordStage("SEARCH_LOGIC", ms);  // Access from ThreadLocal
}
```

---

## Context Propagation & ThreadLocal Magic

### The Challenge: Thread Switching in Reactive Code

```
Traditional Servlet Code:
  Thread 1 handles entire request → ThreadLocal works

Reactive Code with Spring WebFlux:
  Thread 1: HTTP handler
    → Thread 2: .parallel() switches threads
       → Thread 3: .flatMap() switches again
          → ThreadLocal is LOST at each switch!
```

### Solution 1: Reactor Context (For Async/Reactive)

```java
// In Controller
Long requestId = 12345;

return Mono.just(request)
    .contextWrite(Context.of(REQUEST_ID_KEY, requestId))
    .flatMap(req -> pesService.callPes(req))  // Different thread
    .doFinally(signal -> RequestIdContext.clear());

// In Service (different thread, but context still accessible)
Mono.deferContextual(ctx -> {
    Long requestId = RequestIdContext.getFromContext(ctx);  // Retrieved!
    LatencyEvent event = LatencyEvent.builder()
        .requestId(requestId)
        .build();
    return Mono.just(event);
})
```

**How Reactor Context Works**:
```
Thread 1
  ├─ contextWrite(Context.of("requestId", 12345))
  ├─ Creates immutable context
  └─ Propagates context to child operations
  
Thread 2 (after .flatMap())
  ├─ Reactor manages context
  ├─ deferContextual knows about "requestId"
  └─ Accesses value even on different thread
```

### Solution 2: ThreadLocal (For Synchronous)

```java
// At request start
RequestIdContext.set(requestId);

// In any service on same thread
Long requestId = RequestIdContext.get();

// At request end
RequestIdContext.clear();
```

**Problem with ThreadLocal in reactive**:
```
Thread 1: RequestIdContext.set(12345)
Thread 2: RequestIdContext.get()  // Returns NULL! (different thread)
```

### Solution 3: Hybrid Approach (What PRE-NG Uses)

```java
public void reportLatency(LatencyEvent event) {
    // STEP 1: Try to get requestId from MULTIPLE sources
    if (event.getRequestId() == null) {
        Long contextRequestId = RequestIdContext.get();  // Try ThreadLocal
        if (contextRequestId == null) {
            contextRequestId = tryGetFromReactorContext();  // Try Reactor Context
        }
        if (contextRequestId != null) {
            event.setRequestId(contextRequestId);
        }
    }
    
    // STEP 2: Submit to async executor
    // ⚠️ At this point, neither ThreadLocal nor Reactor Context
    //    will be available on the kafka-latency thread!
    //    That's why we grabbed requestId above
    kafkaExecutor.execute(() -> {
        // This thread has neither ThreadLocal nor Reactor Context
        // But we already have requestId stored in the event
        processAndSendEvent(event);
    });
}
```

### Kafka Thread Pool Consideration

```
Main Thread (reactor-http-nio-3)
  └─ Has RequestIdContext.get() available
  └─ Has Reactor Context available
  └─ Call reportLatency()
     ├─ Grab requestId from ThreadLocal/Context ✓
     ├─ Store in event.requestId ✓
     └─ Submit to kafkaExecutor.execute()

Kafka Thread (kafka-latency-1234567890)
  ├─ NO ThreadLocal (different thread)
  ├─ NO Reactor Context (not part of reactive chain)
  ├─ But HAS requestId in event object ✓
  └─ Proceed with serialization and send
```

---

## Kafka Message Flow

### Sequence: From External Call to Kafka

```
Step 1: External call completes
├─ Latency measurement: 4373ms
├─ Response captured: {...provider data...}
└─ Success: true

Step 2: reportLatency() called
├─ On: reactor-http-nio-3 thread
├─ Pulls requestId from RequestIdContext.get()
├─ Creates LatencyEvent object
└─ Submits to kafkaExecutor queue

Step 3: Async processing on kafka-latency thread
├─ Pops event from queue
├─ Truncates payload if > 500KB
├─ Captures JVM metrics (heap, threads, GC)
├─ Serializes to JSON
└─ Ready to send

Step 4: JSON serialization
├─ LatencyEvent → JSON string
├─ All fields included (296 lines of LatencyEvent.java)
└─ Size: typically 5-50KB per event

Step 5: Kafka send (fire-and-forget)
├─ Key: requestId (partitioning)
├─ Value: JSON string
├─ Topic: pre-ng.latency.events
├─ Broker ACKs (async)
└─ kafka-latency thread continues immediately

Step 6: Event persisted to Kafka
├─ Partition: based on requestId hash
├─ Offset: sequential
└─ Retention: 7 days (default)

Step 7: Python consumer reads
├─ Subscribes to pre-ng.latency.events
├─ Receives: [LatencyEvent1, LatencyEvent2, LatencyEvent3, PipelineSummaryEvent]
├─ All for same requestId → same partition
├─ Ordered consumption ✓
└─ LLM analyzes patterns
```

### Message Example

```json
{
  "serviceName": "PES",
  "serviceUrl": "https://gateway-stage-dmz.optum.com/api/stage/pdr/pes",
  "clientName": "GPS",
  "latencyMs": 4373,
  "timestamp": "2026-03-30T12:36:30.364777Z",
  "thresholdMs": 500,
  "dayOfWeek": "MONDAY",
  "hourOfDay": 7,
  "requestContext": "zip=85143,div=PNX",
  "requestPayload": "{\"app-nm\":\"pre.pcp\",...}",
  "success": true,
  "httpStatusCode": 0,
  "errorMessage": null,
  "responsePayload": "{\"ProfessionalSummary0006Response\":[...]}",
  "originalResponseSizeBytes": 7160,
  "responseWasTruncated": false,
  "responseItemCount": 1,
  "requestId": 121265,
  "pipelineStage": "PES_SEARCH",
  "retryAttempt": 0,
  "requestItemCount": 0,
  "searchRadiusMiles": 0,
  "heapUsagePercent": 1.43,
  "activeThreadCount": 29,
  "eventType": "EXTERNAL_CALL",
  "filteredOutCount": 0
}
```

---

## Complete Technical Concepts

### Concept 1: Partitioning by Key

```
Kafka Partitioning Strategy:

Topic: pre-ng.latency.events
├─ Partition 0: requestIds [1, 4, 7, 10, 13, ...]
├─ Partition 1: requestIds [2, 5, 8, 11, 14, ...]
└─ Partition 2: requestIds [3, 6, 9, 12, 15, ...]

For requestId = 121265:
  hash(121265) % 3 = 0
  → Goes to Partition 0

All events for same requestId:
  PES event → hash → Partition 0, Offset 1000
  COSMOS event → hash → Partition 0, Offset 1001
  DROOLS event → hash → Partition 0, Offset 1002
  PIPELINE_SUMMARY → hash → Partition 0, Offset 1003

GUARANTEE: Events for same requestId are ordered!
```

### Concept 2: Topic Retention

```
Kafka Retention Policy:

Default: 7 days
Location: /var/kafka/logs/pre-ng.latency.events-0/

Cleanup Interval: 5 minutes

Scenario:
  Monday 9am: LatencyEvent published
  Sunday 9am: Event still available
  Monday 10am: Event deleted (7 days old)

Implication:
  Python consumer MUST consume within 7 days
  or messages are lost
```

### Concept 3: Consumer Offset Management

```
Consumer Group: pre-ng-local-debugger

Offset Tracking:

Partition 0:
  ├─ Committed offset: 1050
  │  (Events 0-1049 already processed)
  ├─ Latest offset: 1200
  │  (Total messages published)
  └─ Lag: 150
     (Events 1050-1199 waiting to be consumed)
```

### Concept 4: Batching in Producers

```
Default Kafka Producer:

linger.ms: 0 (default)
batch.size: 16384 bytes (default)

PRE-NG Configuration:

acks: -1 (all replicas ACK)
enable.idempotence: true
max.in.flight.requests.per.connection: 5

What this means:
  ├─ Each message: wait for ALL replicas to acknowledge
  ├─ Retries: automatic if broker doesn't ACK
  ├─ Ordering: maintained across retries
  └─ Duplicates: prevented by idempotence
```

### Concept 5: Compression

```
Kafka Compression Options:

None (default):
  ├─ 1000 events × 10KB = 10MB
  └─ Network throughput: 10MB

Gzip:
  ├─ 1000 events × 10KB → ~3MB (70% compression)
  └─ Network throughput: 3MB
  └─ CPU overhead: moderate

SNAPPY:
  ├─ 1000 events × 10KB → ~5MB (50% compression)
  └─ Network throughput: 5MB
  └─ CPU overhead: low

PRE-NG: Using compression.type: none
Reason: Local dev, throughput not bottleneck
```

---

## Design Patterns Used

### Pattern 1: Async Offloading

```
Problem: JSON serialization blocks request thread

Before:
  requestThread {
    serialize(pesResponse)  // 10-20ms BLOCKING
    send to Kafka           // 5ms BLOCKING
    continue processing     // whole request delayed
  }

After:
  requestThread {
    submit to async queue   // 0.05ms (instant return)
    continue processing     // unblocked!
  }
  
  asyncThread {
    serialize(pesResponse)  // 10-20ms (on different thread)
    send to Kafka           // 5ms (on different thread)
  }
```

### Pattern 2: Fire-and-Forget

```
Don't wait for broker ACK:

kafkaTemplate.send(topic, key, value)
    .whenComplete((result, ex) -> {
        // This runs later, asynchronously
        if (ex == null) {
            log.debug("Sent successfully");
        }
    });

Advantage: Request doesn't wait
Cost: Message might not be persisted yet (but will be)
```

### Pattern 3: Context Propagation

```
Pass data through async call chain without parameters:

Method 1: ThreadLocal
  set() → store
  get() → retrieve
  Problem: Doesn't work across thread boundaries

Method 2: Reactor Context
  contextWrite() → propagate context
  deferContextual() → retrieve context
  Benefit: Works across thread switches

Method 3: Event Object
  Store in LatencyEvent.requestId
  Pass entire event through async calls
```

### Pattern 4: Graceful Degradation

```
If Kafka fails, main request still succeeds:

kafkaProducer.send()
  .whenComplete((result, ex) -> {
      if (ex != null) {
          log.warn("Kafka send failed: {}", ex);
          // But don't throw!
          // Request already completed successfully
      }
  });
```

### Pattern 5: Queue Overflow Handling

```
If Kafka queue gets full (> 10,000 items):

kafkaExecutor = new ThreadPoolExecutor(
    ...,
    new LinkedBlockingQueue<>(10_000),
    new ThreadPoolExecutor.DiscardOldestPolicy()
    // Drop oldest items if queue full
);

Rationale:
  Old events are stale
  Recent events are more valuable
  Prevents memory runaway
```

---

## Performance Considerations

### Latency Overhead

```
Baseline request: 7295ms (PES + COSMOS + DROOLS)

Kafka instrumentation overhead:

Per-call overhead:
├─ Grab requestId from ThreadLocal: 0.02ms
├─ Create LatencyEvent object: 0.05ms
├─ Submit to executor queue: 0.05ms
└─ Subtotal: 0.12ms per call

For 3 calls (PES, COSMOS, DROOLS): 0.36ms

PERCENTAGE: 0.36 / 7295 = 0.005% overhead
PRACTICAL: Imperceptible to users
```

### Memory Overhead

```
Per LatencyEvent (JSON-serialized):
├─ Simple event (no response): ~1KB
├─ With request payload: ~5KB
├─ With response payload: ~10-50KB
├─ Worst case (350KB response): ~350KB

Queue at capacity (10,000 events):
├─ Best case: 10,000 × 1KB = 10MB
├─ Typical: 10,000 × 10KB = 100MB
├─ Worst case: 10,000 × 350KB = 3.5GB

Practical limit: 1GB heap is fine for PRE-NG
(Queue won't stay full for long)
```

### Thread Pool Sizing

```
Why 2 core / 4 max threads?

Kafka send is I/O-bound (network wait):
├─ Thread 1: Sends message 1
├─ Broker is slow, thread waits
├─ Thread 2: Sends message 2 (while Thread 1 waits)
├─ Parallelism from 2+ threads
└─ More threads = more overhead, not more throughput

Result:
├─ 2 threads: sufficient for typical load
├─ 4 threads: handles brief spikes
├─ More than 4: diminishing returns
```

### Throughput

```
Typical PRE-NG usage:

Requests per second: 100
External calls per request: 3 (PES + COSMOS + DROOLS)
Total Kafka events: 300/sec + summaries

Event size: 10KB average
Throughput: 300 × 10KB = 3MB/sec

Kafka can handle: 1-10GB/sec easily
Overhead: < 1%
```

---

## Common Pitfalls & Solutions

### Pitfall 1: ThreadLocal Leaks in Thread Pools

```
Problem:
  RequestIdContext.set(12345)
  // Thread 1 finishes request
  // Thread 1 returns to pool
  // Thread 1 reused for new request
  // RequestIdContext.get() returns 12345 (from PREVIOUS request!)

Solution:
  ALWAYS call RequestIdContext.clear() in finally/doFinally
  
  try {
      // process request
  } finally {
      RequestIdContext.clear();  // Remove ThreadLocal
  }
```

### Pitfall 2: Losing RequestId in Async

```
Problem:
  kafkaExecutor.execute(() -> {
      Long requestId = RequestIdContext.get();  // Returns NULL!
      // (Different thread, ThreadLocal is gone)
  });

Solution:
  Grab requestId on caller thread, store in event:
  
  if (event.getRequestId() == null) {
      event.setRequestId(RequestIdContext.get());  // On caller thread
  }
  kafkaExecutor.execute(() -> {
      // requestId is already in event object
      String key = event.getRequestId().toString();
  });
```

### Pitfall 3: Blocking Reactor Thread

```
Problem:
  String json = objectMapper.writeValueAsString(largeResponse);  // 14ms
  // Called on reactor-http-nio thread
  // All requests on that thread blocked for 14ms

Solution:
  Use reportLatencyWithRawPayloads():
  
  latencyService.reportLatencyWithRawPayloads(
      event,
      pesRequest,      // Not JSON yet
      pesResponse      // Not JSON yet
  );
  // Serialization happens on kafka-latency thread
```

### Pitfall 4: Kafka Queue Overflow

```
Problem:
  Kafka slow to send (broker down, network congested)
  Queue fills up: 10,000 items
  New events dropped silently
  DiscardOldestPolicy kicks in
  Old events lost

Solution:
  Monitor queue size in production:
  
  int queueSize = ((ThreadPoolExecutor) kafkaExecutor)
      .getQueue().size();
  if (queueSize > 5000) {
      log.warn("Kafka queue backing up: {}", queueSize);
  }
```

### Pitfall 5: RequestId Missing in Kafka Events

```
Problem:
  LatencyEvent published with requestId = null
  Python consumer can't correlate events
  "This PES event belongs to which request?"

Root Cause:
  RequestIdContext.get() returned null
  (ThreadLocal wasn't set)

Solution:
  Ensure RequestIdContext.set() called at request start:
  
  In Controller:
    Long requestId = auditRequestAndLog(request);
    RequestIdContext.set(requestId);  // ← DON'T FORGET
    
  Verify with logs:
    "Sending Kafka event: service=PES, requestId=12345"
    If requestId=null, investigate why set() wasn't called
```

### Pitfall 6: Payload Truncation Loss

```
Problem:
  PES response: 347KB
  Truncated to: 250KB + marker
  Python consumer: "Response was truncated, can't analyze it"

Solution:
  Always check responseWasTruncated flag:
  
  if (event.getResponseWasTruncated()) {
      log.warn("Response truncated: original={}bytes", 
               event.getOriginalResponseSizeBytes());
  }
  
  Analyze the problem:
  ├─ Why is response so large?
  ├─ Cache provider results?
  ├─ Batch requests instead?
```

---

## Summary: How It All Fits Together

### The Complete Flow

```
1. User Calls PRE-NG API
   ↓
2. MemberProviderRecommendationsController Receives Request
   ├─ Audit to database → requestId = 121265
   ├─ Store in ThreadLocal: RequestIdContext.set(121265)
   └─ Create PipelineTracker
   ↓
3. Service Calls (PES, COSMOS, DROOLS)
   ├─ Each call measured
   ├─ LatencyEvent created with requestId (from ThreadLocal)
   ├─ Submitted to kafkaExecutor
   └─ Request thread continues IMMEDIATELY
   ↓
4. Async Processing (kafka-latency thread)
   ├─ Truncate payload if needed
   ├─ Capture JVM metrics
   ├─ Serialize to JSON
   └─ Send to Kafka (fire-and-forget)
   ↓
5. Kafka Broker Persists Message
   ├─ Partition 0 (based on requestId hash)
   ├─ Offset 12345
   └─ Retention: 7 days
   ↓
6. Python LLM Consumer Reads
   ├─ Receives: [PES event, COSMOS event, DROOLS event, PIPELINE_SUMMARY]
   ├─ All events for requestId 121265 (same partition, ordered)
   └─ Analyzes latency patterns with LLM
   ↓
7. Request Completes (No Latency Impact)
   └─ Kafka work happened asynchronously
```

### Key Takeaways

1. **Async = No Impact**: Kafka work doesn't slow down main request
2. **Context Propagation**: RequestId flows through ThreadLocal/Reactor Context
3. **Fire-and-Forget**: Errors in Kafka never break the request
4. **Rich Data**: LLM gets all context needed for intelligent analysis
5. **Graceful Degradation**: If Kafka fails, PRE-NG still works
6. **Ordered Events**: Same partition = ordered consumption per request

---

## Next Steps for Production

- [ ] Set up Kafka cluster (not just local broker)
- [ ] Configure replication factor = 3
- [ ] Set up Python LLM consumer microservice
- [ ] Implement monitoring for queue depth
- [ ] Add alerting for Kafka broker failures
- [ ] Test graceful degradation scenarios
- [ ] Profile with production-scale traffic


