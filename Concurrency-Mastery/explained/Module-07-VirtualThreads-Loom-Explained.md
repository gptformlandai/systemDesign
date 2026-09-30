# Module 7: Virtual Threads & Project Loom — The Fun, Deep-Dive Explanation

> This document explains how virtual threads work, when they fail to unmount (pinning), the downstream exhaustion trap, and when Reactive still beats Loom — like you're a junior dev who's never seen this before.

---

## 📑 Table of Contents

- [Part 1: What Are Virtual Threads?](#part-1-what-are-virtual-threads)
  - [1.1 The Coworking Space Analogy](#11-the-coworking-space-analogy)
  - [1.2 The Technical Picture](#12-the-technical-picture)
  - [1.3 Creating Virtual Threads](#13-creating-virtual-threads)
- [Part 2: Pinning — When VTs Get Stuck](#part-2-pinning--when-vts-get-stuck)
  - [2.1 What Causes Pinning](#21-what-causes-pinning)
  - [2.2 Detecting Pinning](#22-detecting-pinning)
  - [2.3 Fixing Pinning](#23-fixing-pinning)
- [Part 3: The Downstream Exhaustion Trap](#part-3-the-downstream-exhaustion-trap)
  - [3.1 The Problem](#31-the-problem)
  - [3.2 The Fix: Semaphore Gating](#32-the-fix-semaphore-gating)
- [Part 4: ThreadLocal vs ScopedValue](#part-4-threadlocal-vs-scopedvalue)
  - [4.1 The ThreadLocal Problem](#41-the-threadlocal-problem)
  - [4.2 ScopedValue](#42-scopedvalue)
- [Part 5: StructuredTaskScope](#part-5-structuredtaskscope)
  - [5.1 Safe Fan-Out](#51-safe-fan-out)
  - [5.2 Automatic Cancellation](#52-automatic-cancellation)
- [Part 6: Virtual Threads vs Reactive](#part-6-virtual-threads-vs-reactive)
  - [6.1 The Decision Matrix](#61-the-decision-matrix)
  - [6.2 When Reactive Still Wins](#62-when-reactive-still-wins)
- [Part 7: Production War Stories](#part-7-production-war-stories)
- [Part 8: Interview Traps & Self-Check](#part-8-interview-traps--self-check)

---

# Part 1: What Are Virtual Threads?

## 1.1 The Coworking Space Analogy

Imagine a **coworking space** with only 8 desks (your CPU cores) and 10,000 members (your virtual threads):

```
+------------------------------------------------------------------+
|                    THE COWORKING SPACE                            |
+------------------------------------------------------------------+
|                                                                   |
|  8 DESKS (Carrier Threads / CPU Cores)                           |
|  +------+  +------+  +------+  +------+                          |
|  |Desk 1|  |Desk 2|  |Desk 3|  |Desk 4|                          |
|  +------+  +------+  +------+  +------+                          |
|  +------+  +------+  +------+  +------+                          |
|  |Desk 5|  |Desk 6|  |Desk 7|  |Desk 8|                          |
|  +------+  +------+  +------+  +------+                          |
|                                                                   |
|  10,000 MEMBERS (Virtual Threads)                                |
|  [M1] [M2] [M3] [M4] [M5] ... [M9999] [M10000]                   |
|                                                                   |
+------------------------------------------------------------------+

HOW IT WORKS:

Member needs to THINK and TYPE (CPU work):
+------+
|Desk 1|  <-- Member sits down (MOUNT onto carrier)
| [M1] |      Does their work
+------+

Member waiting for COFFEE DELIVERY (I/O wait):
+------+
|Desk 1|  <-- Member gets up (UNMOUNT from carrier)
|      |      Takes laptop and papers (stack saved to heap)
+------+      Desk is FREE for someone else!

Member's coffee arrives:
[M1] joins the queue for next available desk
Gets assigned to Desk 3 (REMOUNT on different carrier)
+------+
|Desk 3|
| [M1] |
+------+

KEY INSIGHT:
Only THINKING time consumes a desk!
10,000 members can coexist on 8 desks
as long as most are waiting for something.
```

## 1.2 The Technical Picture

```
    Virtual Threads (millions possible)     Carrier Threads (few, ~8)
    ================================        ========================
    
    +--+  +--+  +--+  +--+  ... +--+            +------------+
    |V1|  |V2|  |V3|  |V4|      |Vn|            | Carrier 1  | <-- Real OS thread
    +--+  +--+  +--+  +--+      +--+            +------------+
      |     |     |     |         |             | Carrier 2  | <-- Real OS thread
      |     |     |     |         |             +------------+
      |     |     |     |         |             | Carrier 3  | <-- Real OS thread
      +-----+-----+-----+---------+             +------------+
                  |                             | ...        |
                  v                             +------------+
           Mount/Unmount                        | Carrier 8  | <-- Real OS thread
                                                +------------+
    
    Each VT has its own stack                   Backed by ForkJoinPool
    stored in the HEAP (~500 bytes)             (default: availableProcessors())
```

### Key Terms

| Term | What It Means |
|------|---------------|
| **Virtual Thread (VT)** | A Java `Thread` whose stack lives in the **heap** as continuation frames. Cost: ~500 bytes |
| **Carrier Thread** | A real OS thread that runs VTs. Default: `ForkJoinPool` with ~8 threads |
| **Mount** | Carrier "becomes" the VT: VT's stack copied onto carrier's real stack, execution begins |
| **Unmount** | VT's stack saved back to heap, carrier freed to run another VT |

### Platform Thread vs Virtual Thread

```
PLATFORM THREAD (traditional):
+------------------+
| OS Thread        |  ~1-2 MB stack
| (1:1 with Java)  |  Expensive to create
|                  |  Limited by OS (thousands)
+------------------+

VIRTUAL THREAD (new):
+------------------+
| Java Thread obj  |  ~500 bytes
| Stack in heap    |  Cheap to create
| Scheduled by JVM |  Millions possible!
+------------------+
     |
     v
Runs on carrier when needed
```

## 1.3 When Does Unmounting Happen?

**Automatically** when the VT hits a blocking JDK API:

```
VT calls blocking operation:
     |
     v
+--------------------+
| Thread.sleep()     | --> UNMOUNT! Carrier freed.
| BlockingQueue.take | --> UNMOUNT! Carrier freed.
| Socket.read()      | --> UNMOUNT! Carrier freed.
| Future.get()       | --> UNMOUNT! Carrier freed.
| Lock.lock()        | --> UNMOUNT! Carrier freed.
| Semaphore.acquire  | --> UNMOUNT! Carrier freed.
+--------------------+

When the blocking operation completes:
     |
     v
VT gets scheduled on a carrier (maybe different one!)
Continues execution.
```

**This is the magic:** Blocking code in the "old imperative style" now scales! You don't have to rewrite it as async chains.

## 1.4 What Virtual Threads Are NOT

| Myth | Reality |
|------|---------|
| "VTs are free" | Each VT still has a Thread object, name, UUID. Cheap ≠ free. |
| "VTs are faster" | They don't make code faster. They let MORE code fit on same hardware. |
| "VTs replace parallelism" | For CPU-bound work, you still need N cores. VTs give concurrency, not compute. |
| "VTs are pooled" | `newVirtualThreadPerTaskExecutor()` creates a NEW VT per task. No reuse! |

## 1.5 Creating Virtual Threads

```java
// One-off virtual thread
Thread vt = Thread.startVirtualThread(() -> doWork());

// Builder pattern with naming
Thread vt = Thread.ofVirtual()
    .name("worker-", 0)
    .start(() -> doWork());

// Task-per-VT executor (the idiomatic way!)
try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        exec.submit(this::handleRequest);
    }
} // Auto-close waits for all tasks to finish!
```

**Important:** Do NOT pre-size a "pool" of VTs. The whole point is on-demand creation!

---

# Part 2: Pinning — When VTs Fail to Unmount

## 2.1 What Is Pinning?

**Pinning** = A VT is stuck on its carrier and CAN'T unmount, even though it's blocked.

```
NORMAL (unmounting):
+------------+
| Carrier 1  |  VT blocks on I/O
| [VT-1]     |  --> VT unmounts
+------------+      Carrier is FREE!
      |
      v
+------------+
| Carrier 1  |  Another VT can use it
| [VT-2]     |
+------------+


PINNED (stuck!):
+------------+
| Carrier 1  |  VT blocks but CAN'T unmount
| [VT-1]     |  --> VT stays on carrier
| (PINNED!)  |      Carrier is WASTED!
+------------+

If enough VTs pin, you run out of carriers!
--> Deadlock or throughput collapse
```

## 2.2 What Causes Pinning?

| Situation | Pins in JDK 21? | Pins in JDK 24+? | Notes |
|-----------|-----------------|------------------|-------|
| Blocking I/O (`java.net.*`) | No | No | JDK rewired to unmount |
| `Thread.sleep()` | No | No | Unmounts |
| `ReentrantLock.lock()` | No | No | Unmounts |
| `synchronized` block | **YES!** | **No** (JEP 491) | Big change in JDK 24! |
| `Object.wait()` | **YES!** | **No** (JEP 491) | Same fix |
| JNI (native code) | **YES** | **YES** | Carrier stuck until native returns |
| Class initializer (`<clinit>`) | **YES** | **YES** | Rare but real |
| Some local disk file I/O | **YES** | **YES** | Kernel doesn't offer async |

### The Big News: JDK 24 Fixed `synchronized`!

```
JDK 21:
synchronized (lock) {
    blockingCall();  // PINS the carrier!
}

JDK 24+ (JEP 491):
synchronized (lock) {
    blockingCall();  // UNMOUNTS properly!
}
```

**Interview one-liner:**
> "In JDK 21, the big pinner was `synchronized`. JEP 491 (JDK 24) fixed it. Today's residual pinners are JNI, class init, and some local-disk file I/O."

## 2.3 Detecting Pinning

### Runtime Flag

```bash
java -Djdk.tracePinnedThreads=full -jar app.jar
# Prints stack trace of every pinned VT

java -Djdk.tracePinnedThreads=short -jar app.jar
# Prints summary
```

### JFR Event

```bash
# Record for 60 seconds
java -XX:StartFlightRecording=duration=60s,filename=app.jfr -jar app.jar

# Check for pinning events
jfr print --events jdk.VirtualThreadPinned app.jfr
```

## 2.4 Fixing Pinning

**In order of preference:**

1. **Upgrade to JDK 24+** where `synchronized` no longer pins

2. **Replace `synchronized` with `ReentrantLock`** (works on JDK 21)
   ```java
   // Before (pins on JDK 21)
   synchronized (lock) {
       blockingCall();
   }
   
   // After (doesn't pin)
   lock.lock();
   try {
       blockingCall();
   } finally {
       lock.unlock();
   }
   ```

3. **Use platform threads for JNI calls**
   ```java
   // Dedicated platform thread pool for native calls
   ExecutorService nativePool = Executors.newFixedThreadPool(16);
   
   // VT submits to platform pool and waits
   Future<Result> future = nativePool.submit(() -> nativeCall());
   Result result = future.get();  // VT unmounts while waiting!
   ```

---

# Part 3: The Downstream Exhaustion Trap — The L5 Killer

## 3.1 The Mistake Everyone Makes

> "We swapped `newFixedThreadPool(200)` for `newVirtualThreadPerTaskExecutor()` and now our app handles 100x more concurrent requests!"
> 
> *...followed by everything catching fire.*

## 3.2 Why It Burns

Your old platform thread pool was doing something you didn't realize: **rate-limiting your dependencies!**

```
BEFORE (Platform Threads):
+------------------+     +------------------+     +------------------+
| 200 Threads      | --> | HikariCP         | --> | Database         |
| (implicit limit) |     | (10 connections) |     | (can handle 10)  |
+------------------+     +------------------+     +------------------+

Max 200 concurrent requests.
Max 10 concurrent DB queries.
Everything balanced!


AFTER (Virtual Threads):
+------------------+     +------------------+     +------------------+
| 100,000 VTs      | --> | HikariCP         | --> | Database         |
| (no limit!)      |     | (10 connections) |     | (can handle 10)  |
+------------------+     +------------------+     +------------------+

100,000 concurrent requests!
All trying to get 10 connections!
99,990 VTs blocked waiting!
Connection timeout storm!
DATABASE DIES!
```

### What Gets Exhausted

| Resource | Default Limit | What Happens |
|----------|---------------|--------------|
| HikariCP connections | 10 | Timeout storm, app dies |
| Downstream HTTP service | Their thread pool | They OOM first |
| Memory per request | ~KB each | 100k × KB = hundreds of MB |
| File descriptors | 65k (ulimit) | Hard cap, connections refused |

## 3.3 The Correct Pattern: Explicit Resource Gating

```java
// Unlimited VTs at the request layer
try (ExecutorService requests = Executors.newVirtualThreadPerTaskExecutor()) {

    // BUT: Every downstream resource has an explicit permit budget!
    Semaphore dbPermits = new Semaphore(20);          // Matches HikariCP
    Semaphore partnerApiPermits = new Semaphore(50);  // Matches partner SLA
    Semaphore paymentPermits = new Semaphore(10);     // Matches payment vendor

    for (HttpRequest req : incoming) {
        requests.submit(() -> {
            // Gate the database
            dbPermits.acquire();
            try {
                userRow = jdbcTemplate.query(...);
            } finally {
                dbPermits.release();
            }

            // Gate the partner API
            partnerApiPermits.acquire();
            try {
                partnerResp = httpClient.send(...);
            } finally {
                partnerApiPermits.release();
            }
        });
    }
}
```

### Visual: The Gating Pattern

```
+------------------+
| 100,000 VTs      |
+--------+---------+
         |
         v
+------------------+
| Semaphore(20)    |  <-- Only 20 can proceed to DB
+--------+---------+
         |
         v (max 20 concurrent)
+------------------+
| HikariCP (20)    |
+------------------+
         |
         v
+------------------+
| Database         |  <-- Happy! Only sees 20 concurrent.
+------------------+
```

## 3.4 The Rule to Say Out Loud

> "Virtual threads make concurrency cheap at the **application** layer, but every **downstream dependency** still has finite capacity. You must gate each of them explicitly with a `Semaphore` or queue. The pool size that was implicitly protecting your DB is now YOUR responsibility."

## 3.5 Connection Pool Sizing After VT

**Don't raise HikariCP to 10,000 just because VTs are cheap!**

```
HikariCP default: 10 connections
With VTs: Still ~10-50 connections!

Why? The DATABASE is the bottleneck, not your app.
Postgres can't handle 10,000 concurrent queries.
It will die.

Match pool size to what the DATABASE can handle.
```

---

# Part 4: `ScopedValue` and `StructuredTaskScope`

## 4.1 The `ThreadLocal` Problem

With platform threads (pool of 200), `ThreadLocal` was fine:
```
200 threads × 2 KB context = 400 KB (trivial)
```

With virtual threads (100,000 concurrent):
```
100,000 VTs × 2 KB context = 200 MB of dead context objects!
```

`ThreadLocal` is a `Map<Thread, Value>`. With 100k VTs, you get 100k map entries!

## 4.2 `ScopedValue` — The Modern Replacement

```java
// Declare (usually static final)
public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();
public static final ScopedValue<String> TENANT = ScopedValue.newInstance();

// Set at request boundary
ScopedValue
    .where(TRACE_ID, request.getHeader("X-Trace-Id"))
    .where(TENANT, request.getHeader("X-Tenant"))
    .run(() -> {
        // TRACE_ID.get() works anywhere inside this scope!
        // Including in forked VTs!
        handleRequest();
    });

// Automatic cleanup when scope exits!
// No try/finally { threadLocal.remove(); } needed!
```

### Why `ScopedValue` Is Better

| Aspect | `ThreadLocal` | `ScopedValue` |
|--------|---------------|---------------|
| Mutability | Can `set()` anytime | Immutable once set |
| Cleanup | Manual `remove()` | Automatic on scope exit |
| Inheritance | Explicit `InheritableThreadLocal` | Automatic in `StructuredTaskScope` |
| Performance | HashMap lookup | Special JVM support (faster) |
| Memory | Leaks if not removed | No leaks possible |

## 4.3 `StructuredTaskScope` — Safe Fan-Out

**The problem:** "I want to launch 3 subtasks in parallel, cancel them all if one fails, and never leak a thread."

**Old way with `CompletableFuture`:**
```java
// If addressFuture fails, userFuture and ordersFuture KEEP RUNNING!
// They burn resources even though we don't need them anymore.
CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> loadUser(id));
CompletableFuture<Address> addressFuture = CompletableFuture.supplyAsync(() -> loadAddress(id));
CompletableFuture<Orders> ordersFuture = CompletableFuture.supplyAsync(() -> loadOrders(id));

CompletableFuture.allOf(userFuture, addressFuture, ordersFuture).join();
```

**New way with `StructuredTaskScope`:**
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<User> user = scope.fork(() -> loadUser(id));
    Subtask<Address> address = scope.fork(() -> loadAddress(id));
    Subtask<Orders> orders = scope.fork(() -> loadOrders(id));

    scope.join();           // Wait for all
    scope.throwIfFailed();  // If ANY failed, siblings already CANCELLED!

    return new Profile(user.get(), address.get(), orders.get());
}
// Scope guarantees no orphan tasks!
```

### Two Flavors

| Flavor | Behavior | Use Case |
|--------|----------|----------|
| `ShutdownOnFailure` | First failure cancels the rest | All-or-nothing operations |
| `ShutdownOnSuccess` | First success cancels the rest | Race semantics, latency-sensitive |

---

# Part 5: Virtual Threads vs Reactive

## 5.1 The Comparison

| Dimension | Virtual Threads | Reactive (WebFlux, Reactor) |
|-----------|-----------------|----------------------------|
| **Programming model** | Imperative, blocking-style | Declarative flows (`Mono`, `Flux`) |
| **Backpressure** | Implicit (you add semaphores) | First-class (`request(n)`) |
| **Learning curve** | Familiar to any Java dev | Steep, new mental model |
| **Debugging** | Stack traces "just work" | Async fragments, hard to trace |
| **Best fit** | Request/response with I/O | Streaming, event processing |

## 5.2 The Honest L5 Answer

> "For a typical request/response microservice hitting a DB and a couple of downstream APIs — **virtual threads win now**: keep the imperative code, get scalability for free, sidestep the debugging tax of Reactor.
>
> For streaming / event-driven pipelines with true backpressure (Kafka streams, SSE broadcasts, WebSocket fanout) — **Reactor's operator model is still superior**. Not because it's faster, but because backpressure and composition are first-class."

## 5.3 When to Pick Which

```
Request/Response Service:
+------------------+     +------------------+     +------------------+
| HTTP Request     | --> | DB Query         | --> | HTTP Response    |
+------------------+     +------------------+     +------------------+

--> USE VIRTUAL THREADS
    Simple, familiar, debuggable


Streaming Pipeline:
+------------------+     +------------------+     +------------------+
| Kafka Consumer   | --> | Transform        | --> | Kafka Producer   |
| (backpressure!)  |     | (operators)      |     | (backpressure!)  |
+------------------+     +------------------+     +------------------+

--> USE REACTIVE
    Backpressure is first-class
```

---

# Part 6: Production War Stories

## War Story 1: The Hikari Meltdown

**Setup:** Team enabled `spring.threads.virtual.enabled=true` on Spring Boot 3.2.

**What Happened:**
- Load test spiked from 500 to 50,000 concurrent requests
- HikariCP (size 10) queue times exploded to 10+ seconds
- Connection acquire timeouts everywhere
- App died

**Fix:** Added `Semaphore(15)` at request level to gate DB access.

---

## War Story 2: The `synchronized` Pinning

**Setup:** Legacy code used `synchronized` around a shared config cache. JDK 21.

**What Happened:**
- Light load: fine
- 20k concurrent VTs: all carriers pinned on that monitor
- App deadlocked

**Fix (immediate):** Replaced `synchronized` with `ReentrantLock`
**Fix (permanent):** Upgraded to JDK 24 (JEP 491)

---

## War Story 3: The ThreadLocal Memory Leak

**Setup:** Tenancy library set `ThreadLocal<TenantCtx>` at request start.

**What Happened:**
- Platform threads (200): 200 × 2 KB = 400 KB (trivial)
- After VTs (30k concurrent): 30k × 2 KB = 60 MB of dead context!
- Heap bloat, GC pressure

**Fix:** Migrated to `ScopedValue`. Heap footprint dropped to ~0.

---

## War Story 4: The JNI Pinning Surprise

**Setup:** In-process ML inference via JNI (~100ms per call).

**What Happened:**
- All 8 carriers got pinned to native calls
- Throughput dropped to 80 req/s

**Fix:** Routed native calls through dedicated platform thread pool. VTs `await` on futures.

---

# Part 7: Interview Traps

| Question | Bad Answer | L5 Answer |
|----------|-----------|-----------|
| "What's a virtual thread?" | "A lightweight thread." | "A Thread whose stack lives in the heap as a continuation; scheduled M:N on carrier threads via ForkJoinPool. Unmounts on JDK blocking calls." |
| "Should I pool VTs?" | "Yes, N=200." | "No. Create one per task. Use `newVirtualThreadPerTaskExecutor()`. Pooling defeats the purpose." |
| "What pins today?" | "`synchronized`." | "In JDK 24+ `synchronized` no longer pins. Residual pinners: JNI, class init, some local-disk file I/O." |
| "Any downside of switching?" | "None, drop-in." | "Downstream exhaustion. Anything implicitly limited by pool size must now be explicitly gated with Semaphores." |
| "`ThreadLocal` in VT world?" | "Same as always." | "Memory hazard at 100k+ VTs. Use `ScopedValue` — immutable, auto-cleaned, inheritable." |
| "VT vs Reactive?" | "VT is better now." | "Depends. VT for imperative request/response. Reactor for streaming with first-class backpressure." |

---

# Part 8: Self-Check Questions

1. **What happens when a VT calls `Socket.read()`?**
   - VT unmounts, stack saved to heap, carrier freed

2. **What is a carrier thread? How many by default?**
   - Real OS thread that runs VTs. Default: `availableProcessors()` (~8)

3. **Three things that still pin in modern JDK?**
   - JNI, class initializer, some local disk I/O

4. **Flag for pinning stacks? JFR event?**
   - `-Djdk.tracePinnedThreads=short`
   - `jdk.VirtualThreadPinned`

5. **Downstream exhaustion in 3 sentences?**
   - VTs remove implicit rate limiting from thread pools. Downstream resources (DB, APIs) have finite capacity. You must explicitly gate with Semaphores.

6. **Why is `ScopedValue` safer than `ThreadLocal`?**
   - Immutable, auto-cleaned on scope exit, no leaks

7. **`StructuredTaskScope` vs `CompletableFuture.allOf`?**
   - Automatic cancellation of siblings on failure, no orphan tasks

8. **When pick Reactor over VTs?**
   - Streaming pipelines with first-class backpressure

9. **Should you pool VTs?**
   - No. Use `newVirtualThreadPerTaskExecutor()`.

10. **90% of pinning from one `synchronized` block. Fixes?**
    - Replace with `ReentrantLock`, or upgrade to JDK 24+

---

# Summary: The Complete Mental Model

```
+=========================================================================+
|                    MODULE 7: VIRTUAL THREADS                             |
+=========================================================================+
|                                                                          |
|  Architecture                                                           |
|  +--------------------------------------------------------------------+ |
|  |  VT = Thread with stack in heap (~500 bytes)                       | |
|  |  Carrier = Real OS thread (default: ~8, ForkJoinPool)              | |
|  |  Mount = VT runs on carrier                                        | |
|  |  Unmount = VT saves stack to heap, carrier freed                   | |
|  |  Blocking JDK calls trigger automatic unmount                      | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  Pinning                                                                |
|  +--------------------------------------------------------------------+ |
|  |  JDK 21: synchronized pins!                                        | |
|  |  JDK 24+: synchronized fixed (JEP 491)                             | |
|  |  Still pins: JNI, class init, some local disk I/O                  | |
|  |  Detect: -Djdk.tracePinnedThreads, jdk.VirtualThreadPinned JFR     | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  Downstream Exhaustion                                                  |
|  +--------------------------------------------------------------------+ |
|  |  VTs remove implicit rate limiting                                 | |
|  |  Must explicitly gate: Semaphore per downstream resource           | |
|  |  Don't raise DB pool to 10k! Match DB capacity.                    | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  Modern APIs                                                            |
|  +--------------------------------------------------------------------+ |
|  |  ScopedValue: replaces ThreadLocal (immutable, auto-cleanup)       | |
|  |  StructuredTaskScope: safe fan-out with auto-cancellation          | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
+=========================================================================+
```

---

**You've completed Module 7!** You now understand virtual threads, pinning, downstream exhaustion, and when to use VTs vs Reactive.

**Next up:** [Module 8 — Distributed Concurrency](./Module-08-Distributed-Concurrency-Explained.md)