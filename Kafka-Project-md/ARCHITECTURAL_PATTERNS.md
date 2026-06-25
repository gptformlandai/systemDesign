# 🏗️ Architectural Patterns & Design Decisions: PRE-NG Kafka System

---

## Table of Contents

1. [System Design Decisions](#system-design-decisions)
2. [Architectural Patterns](#architectural-patterns)
3. [Trade-offs Analysis](#trade-offs-analysis)
4. [Scalability Considerations](#scalability-considerations)
5. [Interview-Ready Explanations](#interview-ready-explanations)

---

## System Design Decisions

### Decision 1: Single Topic vs Multiple Topics

#### What We Chose: Single Topic (`pre-ng.latency.events`)

```
All messages go to ONE topic:
├─ eventType = "EXTERNAL_CALL"     (individual service calls)
├─ eventType = "PIPELINE_SUMMARY"  (end-to-end summary)
└─ Both use same topic, different event types in JSON
```

#### Why This Decision?

**Pros of Single Topic**:
```
✅ Simpler topology
   └─ Consumer subscribes to ONE topic
   
✅ Guaranteed ordering per requestId
   └─ All events for same request go to same partition
   └─ Always arrive in call sequence: PES → COSMOS → DROOLS → SUMMARY
   
✅ No topology confusion
   └─ Python consumer doesn't need to know about "LATENCY" vs "PIPELINE" topics
   
✅ Easy debugging
   └─ All relevant data for a request in one place
   
✅ Flexible for future
   └─ Can add new event types without changing topic architecture
```

**Cons of Single Topic**:
```
❌ Mixed message types
   └─ Consumer must check eventType to handle differently
   
❌ Less separation of concerns
   └─ One topic does two things
```

#### Alternative: Multiple Topics

```
Topic 1: pre-ng.latency.external-calls
  └─ Only EXTERNAL_CALL events

Topic 2: pre-ng.pipeline.summaries
  └─ Only PIPELINE_SUMMARY events
```

**Why We Rejected This**:
- Two consumers instead of one
- No guaranteed ordering across topics
- EXTERNAL_CALL events and SUMMARY events wouldn't correlate
- Python consumer would need to join data from two sources

---

### Decision 2: Partitioning Strategy (Key = RequestId)

#### What We Chose: Partition by RequestId

```
Partitioning Formula:
  partition = hash(requestId) % 3

RequestId 121265:
  hash(121265) % 3 = 0 → Partition 0

All events for requestId 121265:
  PES event        → Partition 0, Offset 1000
  COSMOS event     → Partition 0, Offset 1001
  DROOLS event     → Partition 0, Offset 1002
  PIPELINE_SUMMARY → Partition 0, Offset 1003
```

#### Why RequestId (Not ClientName)?

**Original Approach** (rejected): Partition by ClientName
```
Partitioning Formula:
  partition = hash(clientName) % 3

Problem:
  GPS requests → Partition 0
  IIM requests → Partition 1
  DSNP requests → Partition 2
  
  One client dominates traffic
  → One partition gets overloaded
  → Others idle
  → Unbalanced consumption
```

**Better Approach** (chosen): Partition by RequestId
```
Partitioning Formula:
  partition = hash(requestId) % 3

Benefit:
  Each request independently hashed
  → Even distribution across partitions
  → No single hotspot
  → All events for same request together
```

#### How This Affects Consumption

```
Three Python Consumers (one per partition):

Consumer 1 (reads Partition 0):
  └─ Gets all events for requestIds: 1, 4, 7, 10, 13, ...
  └─ Each requestId's events are ordered
  └─ Can correlate PES → COSMOS → DROOLS → SUMMARY

Consumer 2 (reads Partition 1):
  └─ Gets all events for requestIds: 2, 5, 8, 11, 14, ...
  └─ Each requestId's events are ordered
  └─ Can correlate independently

Consumer 3 (reads Partition 2):
  └─ Gets all events for requestIds: 3, 6, 9, 12, 15, ...
  └─ Each requestId's events are ordered
  └─ Can correlate independently

Parallelism:
  All three consumers process in parallel
  No coordination needed
  → Horizontal scalability!
```

---

### Decision 3: Async Offloading vs Synchronous Send

#### What We Chose: Fire-and-Forget Async

```java
// On caller thread (reactor-http-nio-3): ~0.2ms
event = LatencyEvent.builder()...build();
if (event.getRequestId() == null) {
    event.setRequestId(RequestIdContext.get());
}
kafkaExecutor.execute(() -> {
    // Offloaded to kafka-latency thread
    processAndSendEvent(event);  // ~17ms on different thread
});
// Caller thread returns IMMEDIATELY
```

#### Why NOT Synchronous Send?

**Synchronous approach** (rejected):
```java
// On caller thread: ~20ms blocking!
String json = objectMapper.writeValueAsString(event);
KafkaFuture<RecordMetadata> future = kafkaTemplate.send(topic, key, json);
future.get();  // BLOCKING WAIT for broker ACK

// Caller thread can't proceed until Kafka responds
// If Kafka is slow (500ms), request latency increases by 500ms!
```

**Cost Analysis**:
```
Without async:
  Request latency: 7295ms (PES + COSMOS + DROOLS)
  Kafka overhead: +500ms (broker slow)
  Total: 7795ms (6.8% slower)
  
With async:
  Request latency: 7295ms (PES + COSMOS + DROOLS)
  Kafka overhead: 0ms (happens asynchronously)
  Total: 7295ms (same!)
```

---

### Decision 4: Executor Thread Pool Configuration

#### What We Chose

```java
ExecutorService kafkaExecutor = new ThreadPoolExecutor(
    2,                              // core threads
    4,                              // max threads
    60L, TimeUnit.SECONDS,          // keep-alive
    new LinkedBlockingQueue<>(10_000),  // queue size
    r -> {
        Thread t = new Thread(r, "kafka-latency-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    },
    new ThreadPoolExecutor.DiscardOldestPolicy()
);
```

#### Why These Specific Numbers?

**Core Threads: 2**
```
Kafka send is I/O-bound (waiting for network):
├─ Thread 1: Sends event 1 → waits for broker
├─ Thread 2: Sends event 2 → waits for broker
├─ While both wait, they don't consume CPU
├─ More threads won't help (bottleneck is network, not CPU)

2 threads is enough for typical throughput
└─ 100-300 events/sec = 1-3 events per thread
```

**Max Threads: 4**
```
Handles traffic spikes:
├─ Normal: 2 threads active
├─ Spike: Grows to 3-4 threads temporarily
├─ Spike handled, shrink back to 2

Don't set too high:
├─ 100 threads would thrash
├─ Thread context switching overhead
├─ Diminishing returns
```

**Keep-Alive: 60 seconds**
```
If threads idle for 60s:
├─ Shrink back to core size (2)
├─ Free resources
├─ Next spike will expand again
```

**Queue: 10,000 items**
```
Buffer for brief traffic bursts:
├─ 100 events/sec = 100 items/sec
├─ Queue holds 100 seconds worth of events
├─ Plenty of buffer for momentary spikes

If queue fills:
├─ DiscardOldestPolicy drops oldest events
├─ Recent events are more valuable
├─ Prevents memory runaway
```

**Daemon Threads: true**
```
When JVM shuts down:
├─ daemon=true → JVM doesn't wait for these threads
├─ daemon=false → JVM waits for these threads (can hang shutdown)

Why daemon=true for Kafka?
├─ Kafka is "nice to have" (non-critical)
├─ If JVM shutting down, Kafka isn't priority
├─ Graceful shutdown() handles any pending events anyway
```

---

### Decision 5: Payload Truncation at 500KB

#### What We Chose

```java
private static final int MAX_PAYLOAD_SIZE_BYTES = 500 * 1024;  // 500KB

// Kafka broker max.request.size default: 1MB
// Safety margin: 500KB leaves room for:
//   - Event metadata (~1KB)
//   - Multiple messages in batch
//   - Other overhead
```

#### Why 500KB?

```
Kafka Constraint:
├─ max.request.size: 1MB (default)
├─ Must fit one full message in request

PRE-NG response sizes:
├─ PES response: typical 10-50KB, max ~350KB
├─ COSMOS response: typical 5-30KB
├─ DROOLS response: typical 1-5KB
├─ So why 500KB? Safety!

┌─────────────────────────────────────┐
│ Kafka Message Structure             │
├─────────────────────────────────────┤
│ Headers: ~1KB                       │
│ Key (requestId): ~10 bytes          │
│ Value (LatencyEvent JSON): ???      │
│ Metadata: ~100 bytes                │
└─────────────────────────────────────┘

Safety calculation:
├─ Max broker size: 1MB
├─ Reserved for headers/metadata: 50KB
├─ Reserved for batching: 200KB
├─ Available for payload: 750KB
├─ We use: 500KB (conservative)
└─ Safety margin: 250KB (16% buffer)
```

#### When Truncation Happens

```
Example: PES response = 347KB

Normal size, fits in limit:
├─ NOT truncated
├─ Full response sent
├─ originalResponseSizeBytes: 347000
├─ responseWasTruncated: false
└─ Python consumer gets complete data ✓

Hypothetical: PES response = 600KB

Too large:
├─ Truncated to 500KB
├─ Message sent successfully
├─ originalResponseSizeBytes: 600000
├─ responseWasTruncated: true
└─ Python consumer sees truncation marker
   └─ Can investigate why response so large
```

---

## Architectural Patterns

### Pattern 1: Event Sourcing + Async Processing

```
Event Sourcing:
├─ Every significant action produces an event
├─ Event is immutable record of what happened
├─ Can replay events to reconstruct state

PRE-NG Implementation:
├─ External call happens
├─ LatencyEvent created (immutable)
├─ Published to Kafka (event store)
├─ Never modified or deleted

Benefit:
├─ Complete audit trail
├─ Can replay to debug
├─ LLM has access to all facts
```

### Pattern 2: Publish-Subscribe (Observer Pattern)

```
Observer Pattern (Classic):
├─ Subject: Data changes
├─ Observers notified immediately
├─ Tightly coupled

Publish-Subscribe (Decoupled):
├─ Publisher: PRE-NG publishes to topic
├─ Broker: Kafka stores messages
├─ Subscriber: Python consumer reads when ready
├─ NO direct coupling

PRE-NG Benefit:
├─ PRE-NG doesn't know about Python consumer
├─ Python consumer can be deployed/redeployed independently
├─ New consumers can subscribe without PRE-NG changes
```

### Pattern 3: Actor Model (via Async Executors)

```
Actor Model Principles:
├─ Actors receive messages
├─ Process independently
├─ Send messages to other actors
├─ No shared mutable state

PRE-NG Kafka Thread:
├─ "Actor" is the kafka-latency thread
├─ "Message" is LatencyEvent in queue
├─ Processes: truncate, serialize, send
├─ All independently
├─ No shared state between threads

Benefit:
├─ Thread-safe by design
├─ No locks needed
├─ Scalable to many threads
```

### Pattern 4: Command Query Responsibility Segregation (CQRS)

```
Traditional Pattern:
├─ Receive request
├─ Update database (write)
├─ Query results
├─ Send response (read-heavy from same model)

CQRS Pattern:
├─ Write Model: PRE-NG updates database
├─ Read Model: Python consumer reads Kafka
├─ Two separate models for writing and reading

PRE-NG Implementation:
├─ Write: PRE-NG writes latency events to Kafka
├─ Read: Python consumer reads and analyzes patterns
├─ Separated by message broker
├─ Different processing logic for each

Benefit:
├─ Can scale writer and reader independently
├─ Python consumer can be slow without blocking PRE-NG
├─ Easy to add new readers (dashboards, alerts, etc.)
```

### Pattern 5: Dead Letter Queue (DLQ) Pattern

```
Current Implementation:
├─ If Kafka send fails
├─ Log warning
├─ Continue (no DLQ)

Improvement Opportunity:
├─ Failed messages → secondary topic (pre-ng.latency.deadletter)
├─ Separate consumer processes DLQ
├─ Retries or alerts

Not implemented yet because:
├─ Local dev (fire-and-forget acceptable)
├─ Errors are logged (can investigate)
└─ Production would benefit from real DLQ
```

---

## Trade-offs Analysis

### Trade-off 1: Latency vs Throughput

```
Queue Size = 10,000:

                    10,000 item queue
                    
Large:              Small:
├─ Buffer more     ├─ Process faster
├─ Handle spikes   ├─ Fresh data
├─ More memory     ├─ Less memory
└─ 100s of MB      └─ 10s of MB

PRE-NG Choice: Large (10,000)
Reasoning:
├─ Kafka latency unpredictable
├─ Spikes happen (restart, GC pause, network)
├─ Can absorb 100 seconds of events
├─ Memory not constraint on production servers
```

### Trade-off 2: Fire-and-Forget vs Guaranteed Delivery

```
Fire-and-Forget:
├─ Don't wait for broker ACK
├─ Message might be lost (rare)
├─ Fast, no blocking

Example:
├─ Send to Kafka
├─ Network hiccup
├─ Broker crashes before persisting
├─ Message lost

Guaranteed Delivery:
├─ Wait for broker ACK
├─ Message definitely persisted
├─ Slow, blocking

PRE-NG Choice: Fire-and-Forget
Reasoning:
├─ Events are "nice to have" (not critical)
├─ Occasional lost event acceptable
├─ If all latency events lost, system still works
├─ Alternative: If message lost, re-send on retry
```

### Trade-off 3: Detailed Metrics vs Message Size

```
LatencyEvent Fields: 40+

Simple Option:
{
  "serviceName": "PES",
  "latencyMs": 4373,
  "requestId": 121265
}
└─ Size: ~60 bytes
└─ Throughput: ~16KB/sec per event

Rich Option:
{
  "serviceName": "PES",
  "latencyMs": 4373,
  "requestId": 121265,
  "heap_usage": 92%,
  "thread_count": 180,
  "gc_collections": 5,
  "responseItemCount": 50,
  ...40 more fields
}
└─ Size: ~1-5KB
└─ Throughput: ~300-1500 bytes/sec per event

PRE-NG Choice: Rich Option
Reasoning:
├─ LLM needs context for analysis
├─ 1-5KB per event × 300 events/sec = 300KB-1.5MB/sec
├─ Network easily handles this
├─ Disk easily handles this
├─ Better insights worth the storage
```

### Trade-off 4: Sync vs Async Serialization

```
Synchronous (on reactor thread):
├─ objectMapper.writeValueAsString(largeResponse)
├─ 10-50ms blocking
├─ Blocks all requests on that thread
├─ Request latency +10-50ms

Asynchronous (on kafka-latency thread):
├─ objectMapper.writeValueAsString(largeResponse)
├─ 10-50ms on different thread
├─ Request continues immediately
├─ Request latency +0ms

PRE-NG Choice: Asynchronous
Reasoning:
├─ No impact on request latency
├─ Cost is extra thread pool
├─ Worth it!
```

---

## Scalability Considerations

### Scenario 1: Traffic Doubles (100 → 200 req/sec)

```
Current:
├─ 3 partitions
├─ 1 PRE-NG instance
├─ 1 Kafka broker

With 2x traffic:
├─ PRE-NG processes 200 requests/sec
├─ 200 × 3 calls (PES, COSMOS, DROOLS) = 600 events/sec
├─ Kafka queue grows to ~200-300 items
├─ Still well under 10,000 capacity

No changes needed!
```

### Scenario 2: Traffic 10x (100 → 1000 req/sec)

```
Problem:
├─ PRE-NG queue grows rapidly
├─ Events might get dropped (DiscardOldestPolicy)
├─ Python consumer can't keep up

Solution:
├─ Add more Kafka brokers (replication)
├─ Increase queue size to 50,000
├─ Increase thread pool to 4-8 threads
├─ Add multiple Python consumers per partition

Not needed yet:
└─ Current load ~100 req/sec
```

### Scenario 3: Response Payload Explosion

```
Problem:
├─ PES starts returning 1MB responses
├─ Event size: 1MB
├─ Kafka message fails (> 1MB limit)

Solution:
├─ Increase Kafka max.request.size to 5MB
├─ Keep truncation at 2MB (safety margin)
├─ Investigate why response so large
  ├─ Bug in provider count?
  ├─ Unbounded data growth?
  └─ Need pagination?
```

---

## Interview-Ready Explanations

### Q: "Why async offloading for Kafka?"

**Strong Answer**:
```
"We measured that JSON serialization of large responses 
(50-350KB) takes 10-50ms. If this happened on the request 
thread, we'd add unacceptable latency to every API call.

By offloading to a dedicated thread pool, the request thread 
returns immediately after queuing the work. The Kafka 
serialization and send happen on a separate thread at no 
cost to the user experience.

This is especially critical for high-throughput systems 
where every millisecond matters."
```

### Q: "How do you handle context (requestId) across thread boundaries?"

**Strong Answer**:
```
"This is the key challenge in reactive systems. We use 
three mechanisms:

1. ThreadLocal for synchronous code paths - works within 
   a single thread

2. Reactor Context for reactive code that uses WebFlux - 
   survives thread switches within Mono/Flux chains

3. Event object properties - requestId is stored in the 
   LatencyEvent itself before submitting to the executor

When reportLatency() is called, we grab the requestId 
from ThreadLocal (or Reactor Context if available), store 
it in the event object, then submit to async executor. 
By the time the async thread runs, it has the requestId 
in the object, doesn't need ThreadLocal or Context.

This hybrid approach works reliably across synchronous, 
reactive, and async code paths."
```

### Q: "How do you prevent Kafka failures from breaking the main request?"

**Strong Answer**:
```
"Fire-and-forget pattern. All Kafka operations are wrapped 
in try-catch blocks. If serialization fails, if broker is 
down, if network is congested - we log a warning and continue.

The main request never waits for Kafka confirmation. This 
means:
- Kafka slow? Request unaffected.
- Kafka down? Request still completes.
- Message lost? Acceptable tradeoff.

We consider latency intelligence a 'nice to have' that 
improves operations over time, not a critical path. 
This design philosophy makes the system resilient."
```

### Q: "Why single topic instead of separate topics for different event types?"

**Strong Answer**:
```
"We considered separate topics (latency.external-calls, 
latency.pipeline-summaries) but chose a single topic.

Key reasons:

1. Ordering guarantee: All events for the same requestId 
   go to the same partition (via requestId as key). 
   This guarantees we see PES → COSMOS → DROOLS → SUMMARY 
   in order.

2. Consumer simplicity: Python consumer subscribes to ONE 
   topic, not juggling multiple. No need to correlate 
   data across topics.

3. Query flexibility: All relevant data for a request is 
   accessible together. Easy for the LLM to reason about.

The downside (mixed message types) is handled by the 
eventType field in the JSON, so the consumer can 
distinguish message types if needed."
```

### Q: "What's your monitoring strategy for this system?"

**Strong Answer**:
```
"We monitor several metrics:

1. Queue depth: 
   - Alert if queue > 5000 (approaching capacity)
   - Indicates consumers are slow or producers fast

2. Event latency:
   - Time from event creation to Kafka broker receipt
   - Should be < 50ms typically

3. Broker health:
   - Partition replication lag
   - Broker availability
   - Disk space

4. Consumer lag:
   - How far behind consumers are
   - If lag keeps growing, consumers can't keep up

5. Kafka throughput:
   - Events/sec published
   - Bytes/sec published

For production, we'd have dashboards and alerting. For now, 
we have basic logging and manual checks."
```

---

## Conclusion: Why This Design Works

```
PRE-NG's Kafka system succeeds because:

✅ Async offloading
   └─ Zero impact on request latency

✅ Intelligent context propagation
   └─ Works across all code patterns (sync, async, reactive)

✅ Fire-and-forget reliability
   └─ Kafka failures never break the main system

✅ Rich event data
   └─ LLM has all context needed for analysis

✅ Ordered consumption per request
   └─ Python consumer sees complete request lifecycle

✅ Graceful degradation
   └─ Works with or without Python consumer running

This is "architect-level" design: minimal overhead, 
maximum benefit, extreme resilience.
```


