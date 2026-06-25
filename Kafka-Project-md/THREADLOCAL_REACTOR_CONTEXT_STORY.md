# 🧵 ThreadLocal, Reactor Context & Async Data Propagation Story Mode

> A technical story explaining how data flows through async code paths without getting lost

---

## The Problem: "Where Did My Data Go?"

Imagine you're debugging this:

```
Request comes in with ID: 12345
Me: "I need this ID in every service call"

Sync World (Easy):
  Thread 1 processes entire request
  → Save ID to ThreadLocal
  → Any service on Thread 1 accesses ID
  → Easy!

Async World (Hard):
  Thread 1 processes start
  → Save ID to ThreadLocal
  → Switch to Thread 2 via .flatMap()
  → Request service on Thread 2
  → Try to access ID from ThreadLocal
  → ERROR: ThreadLocal returns NULL! (different thread)
  → "Where did my ID go??"
```

---

## Story Mode: Why ThreadLocal Exists

### Chapter 1: The Olden Days (Pre-ThreadLocal)

**Year**: 2000s, before `java.lang.ThreadLocal`

```java
// Old servlet code - method parameters everywhere!
public ResponseDto handleRequest(
    String requestId,
    String memberId,
    String clientName,
    String userRole,
    String authToken,
    // ... 20 more parameters
) {
    validateInput(requestId, memberId, userRole, authToken);
    callPesService(requestId, clientName, authToken);
    callCosmosService(requestId, memberId, authToken);
    buildResponse(requestId, clientName);
}
```

**Problem**: Method signatures bloat. Every layer needs every parameter.

### Chapter 2: ThreadLocal to the Rescue

**Year**: JDK 1.2 (1998) introduces ThreadLocal

```java
// New approach - one central place for request data
public class RequestContext {
    private static final ThreadLocal<String> requestIdHolder = new ThreadLocal<>();
    
    public static void set(String requestId) {
        requestIdHolder.set(requestId);
    }
    
    public static String get() {
        return requestIdHolder.get();
    }
}

// Now we can do:
public ResponseDto handleRequest(String requestId) {
    RequestContext.set(requestId);
    // Any method on this thread can access RequestContext.get()
    validateInput();   // Can access requestId
    callPesService(); // Can access requestId
    callCosmos();     // Can access requestId
    return buildResponse();
}
```

**Benefit**: Method signatures stay clean!

### Chapter 3: ThreadLocal Memory Leak Disaster

**Year**: 2010s, enterprise J2EE deployments explode

```
Scenario: Tomcat thread pool with 200 threads

Thread 1: Processes Request 1
  RequestContext.set("REQ-001")
  Request completes, thread returns to pool
  RequestContext.clear() forgotten! ❌

Thread 1 reused: Processes Request 2
  RequestContext.get()  → Returns "REQ-001" ❌
  Wrong request ID used!

Or worse:
Thread 1 processing REQ-001:
  RequestContext.set(LARGE_OBJECT_REFERENCE)
  Thread returns to pool
  LARGE_OBJECT never garbage collected! (ThreadLocal holds reference)

Memory leak!
```

**Lesson**: ALWAYS call `.clear()` in `finally`

```java
try {
    RequestContext.set(requestId);
    // ... process request
} finally {
    RequestContext.clear();  // CRITICAL!
}
```

### Chapter 4: The Reactive Revolution

**Year**: 2015+, Spring WebFlux emerges

```
Old Servlet Model:
  Thread → Request → Response → Thread goes back to pool
  One thread per request
  ThreadLocal works perfectly

New Reactive Model:
  Thread 1 → Start request
    ↓
  Thread 2 → .flatMap() switches threads
    ↓
  Thread 3 → .parallel() switches again
    ↓
  Thread 1 → Back to original thread
  
  ThreadLocal is per-THREAD, not per-REQUEST!
  When we switch threads, ThreadLocal is LOST
```

**Problem**: ThreadLocal doesn't work across thread boundaries

**Solution**: Project Reactor introduces `Context`

---

## Technical Deep Dive: ThreadLocal

### How ThreadLocal Works Internally

```java
// Simplified implementation
public class ThreadLocal<T> {
    private int hashCode;
    
    public void set(T value) {
        Thread currentThread = Thread.currentThread();
        ThreadLocalMap map = currentThread.threadLocals;  // Hidden map in Thread object
        map.set(this, value);  // Store value in thread's private map
    }
    
    public T get() {
        Thread currentThread = Thread.currentThread();
        ThreadLocalMap map = currentThread.threadLocals;
        return map.get(this);  // Retrieve from thread's private map
    }
}
```

**Each Thread has a hidden map**:

```
Thread 1:
  ├─ threadLocals = {
  │    ThreadLocal_1 → "value-1",
  │    ThreadLocal_2 → 12345,
  │    ThreadLocal_3 → USER_OBJECT
  │  }
  
Thread 2:
  ├─ threadLocals = {
  │    ThreadLocal_1 → "value-2",  // DIFFERENT value!
  │    ThreadLocal_2 → 67890,
  │  }
  
Thread 3:
  └─ threadLocals = null  // No values set
```

**Critical**: ThreadLocal_1 returns DIFFERENT values on different threads!

### ThreadLocal Lifecycle

```
Step 1: Thread starts
  └─ threadLocals = null

Step 2: Code calls ThreadLocal.set()
  └─ threadLocals = {key1 → value1}

Step 3: Code calls ThreadLocal.get()
  └─ Returns value1

Step 4: Code calls ThreadLocal.clear()
  └─ threadLocals = null (reference removed)

Step 5: Thread returns to pool
  └─ threadLocals is empty ✓

Step 6: Thread reused for new request
  └─ threadLocals = null (fresh start) ✓
```

**Without clear()**:

```
Step 4: Code forgot to call ThreadLocal.clear()
  └─ threadLocals = {key1 → value1}  (NOT removed)

Step 5: Thread returns to pool
  └─ threadLocals = {key1 → value1}  (STILL there)

Step 6: Thread reused for new request
  └─ threadLocals.get(key1) returns OLD VALUE ❌
  └─ Memory not freed if value is large ❌
```

### ThreadLocal Use Cases (Work Well)

```
✅ HTTP Request handling (servlet per request)
   └─ Same thread handles entire request
   └─ ThreadLocal available everywhere
   └─ Clear at end of request

✅ Spring Transaction management
   └─ ThreadLocal holds Transaction context
   └─ Same thread executes entire transaction
   └─ Auto-cleared by TransactionManager

✅ Security context (Spring Security)
   └─ ThreadLocal holds current user
   └─ Security checks access on any layer
   └─ Auto-cleared at request end

✅ Logging context (SLF4J MDC)
   └─ ThreadLocal holds request ID for logs
   └─ All logs in request include request ID
   └─ Works great for traditional servlets
```

---

## Technical Deep Dive: Reactor Context

### Why Reactor Context Exists

Spring WebFlux needs context that survives thread switches:

```
Problem with ThreadLocal in reactive:

  .flatMap() switches threads
    │
    └─ ThreadLocal data is LOST
    
Solution with Reactor Context:

  .flatMap() switches threads
    │
    ├─ Reactor Runtime intercepts thread switch
    ├─ Manages Context propagation
    ├─ Makes context available on new thread
    └─ deferContextual() accesses context on any thread
```

### How Reactor Context Works

```java
Mono.just(data)
    .contextWrite(Context.of("KEY", value))
    .flatMap(x -> {
        // Different thread here, but Reactor manages it
        return Mono.deferContextual(ctx -> {
            String value = ctx.get("KEY");  // Available!
        });
    })
```

**Internal Flow**:

```
Thread 1:
  ├─ Create Context { "KEY" → value }
  ├─ Attach to Mono/Flux
  └─ flatMap() triggers

Reactor Runtime:
  ├─ Switches thread to Thread 2
  ├─ Attaches Context to Thread 2's execution
  └─ deferContextual() can access it

Thread 2:
  ├─ deferContextual(ctx -> { ... })
  ├─ ctx.get("KEY") finds value
  └─ Works!
```

### Reactor Context API

```java
// Write context
Mono<T> result = Mono.just(data)
    .contextWrite(Context.of(
        "userId", 12345,
        "userName", "alice",
        "role", "ADMIN"
    ))
    .contextWrite(Context.of("traceId", "trace-001"));

// Read context
Mono<T> process = Mono.deferContextual(ctx -> {
    Long userId = ctx.get("userId");
    String traceId = ctx.get("traceId");
    // Use both values
    return Mono.just(doSomething(userId, traceId));
});

// Multiple writes are merged
.contextWrite(Context.of("a", 1))
.contextWrite(Context.of("b", 2))
// Result: Context has BOTH a=1 and b=2

// Order matters: later contextWrite() takes precedence
.contextWrite(Context.of("key", "value1"))
.contextWrite(Context.of("key", "value2"))
// Final value: "value2"
```

### Context Immutability

```java
// Context is IMMUTABLE
Context ctx = Context.of("key", "value");
ctx.get("key");      // Returns "value"
ctx.put("key2", 42); // Returns NEW context with both values
                     // Original ctx unchanged

// Implication:
Mono.just(x)
    .contextWrite(Context.of("a", 1))
    .flatMap(y -> 
        Mono.just(y)
            .contextWrite(Context.of("b", 2))  // Adds to context
    )
    .flatMap(z ->
        Mono.deferContextual(ctx -> {
            ctx.get("a");  // Available
            ctx.get("b");  // Available
        })
    )
```

---

## Story Mode: Mono.deferContextual() Magic

### What is defer()?

```java
// Regular Mono
Mono.just(value)  // Evaluated IMMEDIATELY

// Deferred Mono
Mono.defer(() -> Mono.just(value))  // Evaluated when SUBSCRIBED
```

**Why defer?**

```java
// Example: Get user ID from database

Mono<User> getUser() {
    Long userId = getFromThreadLocal();  // Might be null!
    return Mono.just(getFromDatabase(userId));  // ERROR if userId = null
}

// Better: Defer the evaluation
Mono<User> getUser() {
    return Mono.defer(() -> {
        Long userId = getFromThreadLocal();  // Evaluated on subscription
        return Mono.just(getFromDatabase(userId));
    });
}
```

### What is deferContextual()?

```java
// Combine defer + context access

Mono.deferContextual(ctx -> {
    // ctx contains all propagated context values
    Long userId = ctx.get("userId");
    String traceId = ctx.get("traceId");
    
    // Now evaluate the mono
    return Mono.just(doSomething(userId, traceId));
})
```

**When is this evaluated?**

```
Thread 1:
  ├─ contextWrite(Context.of("userId", 12345))
  ├─ deferContextual(ctx -> ...)  // NOT evaluated yet!
  └─ Subscribe

Subscription triggers:
  └─ deferContextual() lambda is NOW called
     ├─ ctx.get("userId") available
     ├─ Lambda executes
     └─ Returns Mono to execute
```

### Real-World Example: PRE-NG RequestIdContext

```java
// In RequestIdContext.java
public static Long getFromContext(Context context) {
    return context.getOrDefault(REQUEST_ID_KEY, null);
}

// Usage in service
return Mono.deferContextual(ctx -> {
    Long requestId = RequestIdContext.getFromContext(ctx);
    
    return pesService.search(params)
        .doOnSuccess(result -> {
            // Log or send to Kafka with requestId
            latencyService.reportLatency(
                LatencyEvent.builder()
                    .requestId(requestId)
                    .build()
            );
        });
})
```

---

## Story Mode: PRE-NG's Hybrid Approach

### The Challenge

```
PRE-NG uses BOTH:
├─ Reactive code (Spring WebFlux)
├─ ThreadLocal for RequestIdContext
└─ Executor thread pool for Kafka

All three don't always play together!
```

### Solution: Multi-Source Fallback

```java
public void reportLatency(LatencyEvent event) {
    // SOURCE 1: Try ThreadLocal (works in sync/traditional paths)
    if (event.getRequestId() == null) {
        Long contextRequestId = RequestIdContext.get();
        if (contextRequestId != null) {
            event.setRequestId(contextRequestId);
        }
    }
    
    // SOURCE 2: Try Reactor Context (works in reactive paths)
    if (event.getRequestId() == null) {
        // This won't work in .execute() because not in reactive chain
        // But might work in service layer
    }
    
    // SOURCE 3: Already in event (caller provided it)
    // if event.requestId != null, use it
    
    // At this point, requestId is stored in event object
    // Safe to pass to executor thread
    kafkaExecutor.execute(() -> {
        // Don't try to access ThreadLocal or Reactor Context here!
        // requestId is in event
        String key = event.getRequestId().toString();
        kafkaProducer.send(key, eventJson);
    });
}
```

### Why This Works

```
Flow:

Method A (on reactor-http-nio-3):
  ├─ Calls reportLatency()
  ├─ RequestIdContext.get() returns value from ThreadLocal ✓
  └─ Stores in event.requestId

Method B (on kafka-latency-xxx):
  ├─ Receives event
  ├─ event.requestId already populated ✓
  ├─ Can't access ThreadLocal (different thread)
  ├─ Doesn't need to (has value in event)
  └─ Proceeds safely
```

---

## Conceptual Model: Context Propagation Patterns

### Pattern 1: Synchronous (ThreadLocal Only)

```
┌─────────────────────────────────────────┐
│ Thread 1                                │
├─────────────────────────────────────────┤
│ RequestIdContext.set(12345)            │
│ ├─ Service.doWork()                    │
│ │ ├─ RequestIdContext.get() → 12345 ✓  │
│ │ └─ Sub-service.process()             │
│ │   └─ RequestIdContext.get() → 12345 ✓│
│ └─ RequestIdContext.clear()            │
└─────────────────────────────────────────┘
```

**Guarantee**: Always works within single thread

### Pattern 2: Reactive (Reactor Context)

```
┌──────────────────────────────────┐
│ Thread 1                         │
├──────────────────────────────────┤
│ contextWrite(Context.of(...))    │
│ .flatMap() → switch to Thread 2  │
└──────────────────────────────────┘
                ↓
         Reactor Runtime
         (manages context)
                ↓
┌──────────────────────────────────┐
│ Thread 2                         │
├──────────────────────────────────┤
│ deferContextual(ctx → {          │
│   ctx.get(...) → value ✓         │
│ })                              │
└──────────────────────────────────┘
```

**Guarantee**: Context survives thread switches (within Mono/Flux chain)

### Pattern 3: Executor (Event Object)

```
┌────────────────────────────────────────────┐
│ Thread 1                                   │
├────────────────────────────────────────────┤
│ Long id = RequestIdContext.get()          │
│ LatencyEvent event = new LatencyEvent()    │
│ event.setRequestId(id)  ← Store in object │
│ executor.execute(() -> { ... })           │
└────────────────────────────────────────────┘
                ↓
┌────────────────────────────────────────────┐
│ Thread 2 (executor thread)                │
├────────────────────────────────────────────┤
│ // Can't access ThreadLocal or Context    │
│ // But event object still has requestId ✓ │
│ String key = event.getRequestId()         │
│ kafkaProducer.send(key, ...)              │
└────────────────────────────────────────────┘
```

**Guarantee**: Data is preserved in object passed between threads

---

## The Gotchas: Common Mistakes

### Gotcha 1: ThreadLocal in Thread Pool

```java
// ❌ WRONG
ExecutorService executor = Executors.newFixedThreadPool(10);

executor.execute(() -> {
    RequestIdContext.set("REQ-001");  // Set on executor thread
    // Process request
    // Don't clear!
});

executor.execute(() -> {
    Long id = RequestIdContext.get();  // Gets old value from thread reuse!
});
```

**Fix**:
```java
// ✅ CORRECT
try {
    RequestIdContext.set(requestId);  // On CALLER thread
    // ...
    executor.execute(() -> {
        // event.requestId already set
        // Don't try to access ThreadLocal!
    });
} finally {
    RequestIdContext.clear();  // On CALLER thread
}
```

### Gotcha 2: Forgetting contextWrite()

```java
// ❌ WRONG
Mono.just(data)
    // Missing .contextWrite(Context.of("userId", 12345))
    .flatMap(x -> 
        Mono.deferContextual(ctx -> {
            ctx.get("userId")  // Returns null!
        })
    )
```

**Fix**:
```java
// ✅ CORRECT
Mono.just(data)
    .contextWrite(Context.of("userId", 12345))
    .flatMap(x -> 
        Mono.deferContextual(ctx -> {
            ctx.get("userId")  // Returns 12345 ✓
        })
    )
```

### Gotcha 3: Reactor Context Beyond Flux

```java
// ❌ WRONG
Mono.just(data)
    .contextWrite(Context.of("userId", 12345))
    .subscribe(value -> {
        // Subscription happens, but we left the reactive chain
        // Now on a different thread!
        executor.execute(() -> {
            Mono.deferContextual(ctx -> {
                ctx.get("userId")  // Might be null!
                // (Executor thread is not part of reactive chain)
            })
        });
    });
```

**Fix**: Store in object before leaving reactive chain

```java
// ✅ CORRECT
Mono.just(data)
    .contextWrite(Context.of("userId", 12345))
    .flatMap(value ->
        Mono.deferContextual(ctx -> {
            Long userId = ctx.get("userId");  // Get while in chain
            
            return Mono.fromRunnable(() -> {
                executor.execute(() -> {
                    // userId is captured in closure
                    processWithExecutor(userId);  // Use the captured value
                });
            });
        })
    )
    .subscribe();
```

---

## Summary Table: When to Use What

| Approach | Use Case | Thread Safety | Complexity |
|----------|----------|---------------|-----------|
| **ThreadLocal** | Traditional servlet | Single thread only | Low |
| **Reactor Context** | Spring WebFlux | Multi-thread (via Reactor) | Medium |
| **Event Object** | Async executor | Always safe | Low |
| **All Three** | Hybrid (like PRE-NG) | Best coverage | Medium-High |

---

## PRE-NG's Genius: Why It Works So Well

```
PRE-NG Characteristics:
├─ Spring WebFlux (reactive)
├─ External calls (PES, COSMOS, DROOLS)
├─ Async Kafka publishing (executor)
└─ Needs context throughout

PRE-NG Solution:
├─ RequestIdContext for ThreadLocal fallback
├─ contextWrite/deferContextual for reactive
├─ LatencyEvent.requestId for executor thread
└─ reportLatency() intelligently handles all three

Result:
├─ ✅ Works in sync paths
├─ ✅ Works in async/reactive paths
├─ ✅ Works in executor threads
├─ ✅ No thread boundary breaks context
└─ ✅ Minimal overhead
```

---

## Visualization: Complete Data Flow

```
HTTP Request (Thread: reactor-http-nio-3)
  ├─ MemberProviderRecommendationsController
  │ ├─ requestId = 121265 (from DB audit)
  │ ├─ RequestIdContext.set(121265)  ← ThreadLocal
  │ ├─ contextWrite(Context.of(REQUEST_ID_KEY, 121265))  ← Reactor
  │ └─ Call PesService.search()
  │
  ├─ PesService (reactive, might be on different thread)
  │ ├─ External call: HTTP to PES
  │ ├─ reportLatency(event)  ← Called on HTTP callback thread
  │ │ ├─ RequestIdContext.get()  ← Try ThreadLocal
  │ │ ├─ If null, use Reactor Context
  │ │ ├─ Store in event.requestId
  │ │ └─ kafkaExecutor.execute(() -> {...})
  │ └─ Return to main flow
  │
  ├─ KafkaLatencyThread (Thread: kafka-latency-xxx)
  │ ├─ NO ThreadLocal available (different thread)
  │ ├─ NO Reactor Context (not in reactive chain)
  │ ├─ BUT event.requestId = 121265 ✓
  │ ├─ Serialize to JSON
  │ ├─ Send to Kafka
  │ └─ Done
  │
  ├─ KafkaBroker
  │ ├─ Partition: hash(121265) % 3 = 0
  │ ├─ Offset: 12345
  │ └─ Retained for 7 days
  │
  └─ PythonLLMConsumer
    ├─ Reads from partition 0
    ├─ Gets event with requestId = 121265
    ├─ Correlates with other events for same requestId
    ├─ Sees full pipeline: PES → COSMOS → DROOLS → SUMMARY
    ├─ Analyzes patterns with LLM
    └─ Produces insights
```

---

## Final Thoughts

Context propagation in modern Java is about **matching the tool to the execution model**:

- **Traditional Servlet?** → ThreadLocal (simple, well-understood)
- **Spring WebFlux?** → Reactor Context (follows reactive principles)
- **Thread Pools?** → Event Objects (data travels with the work)
- **Hybrid?** → Smart fallbacks (try all, use what works)

PRE-NG's approach is production-ready because it doesn't bet on one mechanism—it uses all available tools intelligently.


