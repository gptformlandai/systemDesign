# Module 7 — Virtual Threads & Project Loom

> **Goal:** You can explain how a virtual thread executes on hardware, when it *doesn't* unmount (pinning), why replacing a thread pool with `newVirtualThreadPerTaskExecutor()` can collapse your database, and when Reactive still beats Loom. This is the modern (2024+) L5 topic.

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────┐
                     │        MODULE 7 CORE               │
                     └────────────────────────────────────┘
                                    │
       ┌────────────────────┬───────┴───────────┬────────────────────┐
       │                    │                   │                    │
   7.1 Architecture    7.2 Pinning         7.3 Downstream       7.4 Scoped/
   (M:N, carriers,     (JNI, file I/O,     Exhaustion +         Structured
   mount/unmount)      class init;         Semaphore gating     (ScopedValue,
                       sync no longer                            StructuredTask-
                       pins in JDK 21+)                          Scope)
                                                                    │
                                                                    ▼
                                                             7.5 VT vs Reactive
                                                             (when to pick which)
```

---

## 7.1 Architecture — M:N Scheduling, Carriers, Mount/Unmount

### Real-world analogy

> Imagine a **coworking space** with only 8 desks (your CPU cores) and 10,000 members (your virtual threads).
> - When a member needs to think and type — they sit at a desk (**mount** onto a carrier thread).
> - When they hit a "waiting for coffee delivery" moment — they get up, taking their laptop and papers with them (**unmount** — stack frames spill to the heap). The desk is immediately free for someone else.
> - When their coffee arrives — they queue for the next open desk (get **remounted** on some carrier).
>
> Only their thinking time consumes a desk. Ten thousand members can co-exist on 8 desks so long as most are waiting for something.

### The technical picture

```
    Virtual threads (millions)          Carriers (ForkJoinPool workers)
    ─────────────────────────           ───────────────────────────────
                                                        ┌──────────┐
    ┌──┐ ┌──┐ ┌──┐ ┌──┐ … ┌──┐            mount        │ Carrier1 │◄── OS thread
    │V1│ │V2│ │V3│ │V4│    │Vn│  ─────────────────►    ├──────────┤    (1 per core-ish)
    └──┘ └──┘ └──┘ └──┘    └──┘         ◄─────         │ Carrier2 │
    each has its own      unmount on blocking          ├──────────┤
    stack in the heap                                  │ Carrier3 │
                                                       └──────────┘
```

- **Virtual thread (VT)** — a Java-level `Thread` object whose stack lives in the **heap** as a stack of `Continuation` frames. Cost: ~500 bytes (vs ~1–2 MB for an OS thread).
- **Carrier thread** — a real OS thread that runs VTs. By default, backed by a dedicated `ForkJoinPool` sized to `Runtime.availableProcessors()`.
- **Mount** — carrier temporarily *becomes* the virtual thread: the VT's stack is copied onto the carrier's real stack; JVM starts executing.
- **Unmount** — the continuation captures the current stack back into the heap; carrier is freed to run another VT.

### When does unmounting happen?

**Automatically**, when the VT hits a blocking JDK API:
- `Thread.sleep`
- `java.util.concurrent` blocking (`BlockingQueue.take`, `Semaphore.acquire`, `Lock.lockInterruptibly`, `Condition.await`, `CountDownLatch.await`, `Future.get`)
- Blocking I/O in `java.net`, `java.io` (Socket, InputStream, Files.newBufferedReader, etc.)
- `Object.wait()` (as of JDK 21 the wait unmounts; before that it pinned)
- `synchronized` block `wait` — same story

**Result:** blocking code in the "old imperative style" now scales. You don't have to rewrite it as async chains.

### What VTs are NOT

- **Not lightweight = free.** Each VT still has a Java `Thread` object, a name, a UUID, garbage to collect if you leak them. Cheap doesn't mean free.
- **Not faster.** They don't make individual code paths faster; they make **more of them fit** on the same hardware because they don't hog OS threads while blocked.
- **Not a replacement for CPU parallelism.** For CPU-bound work, you still need N carriers (≈ N cores) — VTs give you concurrency, not compute.
- **Not automatically pool-safe.** `Executors.newVirtualThreadPerTaskExecutor()` creates a *new* VT for every task. There is no reuse.

### Creating them

```java
// One-off
Thread vt = Thread.startVirtualThread(() -> doWork());

// Builder
Thread vt = Thread.ofVirtual().name("worker-", 0).start(() -> doWork());

// Task-per-VT executor (great with try-with-resources)
try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        exec.submit(this::handleRequest);
    }
} // waits for all submitted tasks to finish
```

That `try (ExecutorService …)` idiom — auto-close waits for all submitted tasks. **Do not** pre-size a "pool" of VTs; the whole point is on-demand.

---

## 7.2 Pinning — When VTs Fail To Unmount

**Pinning** = a VT is stuck on its carrier and can't unmount, even though it's blocked. The carrier is wasted. If enough VTs pin, you run out of carriers → deadlock or throughput collapse.

### What pins today (JDK 21 / 23 baseline)

| Situation | Pins? | Notes |
|---|---|---|
| Blocking I/O via `java.net.*` | ❌ No | JDK rewired to unmount |
| `LockSupport.park`, `Thread.sleep` | ❌ No | Unmounts |
| `ReentrantLock.lock` (blocked) | ❌ No | Unmounts |
| `synchronized` blocking on the monitor | ❌ **No** in JDK 24+ (JEP 491); ✅ Yes in JDK 21 | This is the huge modern shift |
| `Object.wait()` inside `synchronized` | ❌ **No** in JDK 24+; ✅ Yes in JDK 21 | Same story |
| Blocking inside a JNI call (native code) | ✅ Yes | Carrier stuck until native returns |
| Class initializer (`<clinit>`) that blocks | ✅ Yes | Rare, but real |
| Certain filesystem calls on Linux local disk | ✅ Yes | Kernel does not offer async equivalents; JDK falls back |

**Interview one-liner:** *"In JDK 21 the big pinner was `synchronized`. JEP 491 (JDK 24) fixed it, so today the residual pinning cases are JNI, class init, and some local-disk file I/O."*

### Detecting pinning in production

**Flag on startup:**
```
-Djdk.tracePinnedThreads=full
```
Prints the stack of every pinned VT. Use `short` for a summary.

**JFR event:**
`jdk.VirtualThreadPinned` — surfaces in flight-recorder recordings and in JMC. This is how you find pinning at scale.

**Fixes (in order of preference):**
1. Upgrade to JDK 24+ where `synchronized` no longer pins.
2. Replace `synchronized` with `ReentrantLock` inside the hot path (works on JDK 21).
3. Push blocking work onto a dedicated `ExecutorService` of platform threads.
4. If it's a JNI call — no VT for that path; use a platform-thread executor.

---

## 7.3 The Downstream Exhaustion Trap (The L5 Killer)

### The mistake

> "We swapped `newFixedThreadPool(200)` for `newVirtualThreadPerTaskExecutor()` and now our app handles 100× more concurrent requests." — followed by everything catching fire.

### Why it burns

Platform-thread pool sizes were doing something you didn't realize: **rate-limiting your dependencies**. When you allow 100,000 concurrent VTs, each one may:

- Grab a **HikariCP connection** (default pool size: 10). VTs beyond that block in `getConnection()` — many blocking = timeout storm → app dies.
- Fire an **HTTP call** to a downstream service. That service's own thread pool now sees 100k concurrent → it OOMs first.
- Hold **memory** for the request context (a few KB × 100k = hundreds of MB).
- Consume **file descriptors** (one per socket). Default ulimit 65k → hard cap.

### The correct pattern — explicit resource gating

```java
// You allow unlimited VTs at the request-acceptance layer.
try (ExecutorService requests = Executors.newVirtualThreadPerTaskExecutor()) {

    // But every downstream resource has an explicit permit budget.
    Semaphore dbPermits          = new Semaphore(20);     // matches HikariCP
    Semaphore partnerApiPermits  = new Semaphore(50);     // matches partner SLA
    Semaphore paymentGwPermits   = new Semaphore(10);     // matches payment vendor

    for (HttpRequest req : incoming) {
        requests.submit(() -> {
            dbPermits.acquire();
            try { userRow = jdbcTemplate.query(...); }
            finally { dbPermits.release(); }

            partnerApiPermits.acquire();
            try { partnerResp = httpClient.send(...); }
            finally { partnerApiPermits.release(); }

            // …
        });
    }
}
```

### The rule an L5 says out loud

> "Virtual threads make concurrency cheap at the **application** layer, but every **downstream dependency** still has a finite capacity. You must gate each of them explicitly with a `Semaphore` or a queue. The pool size that was implicitly protecting your DB is now your responsibility."

### Connection pool sizing after VT

- HikariCP default: 10.
- With VTs, the pool size should still match the **database's** ability to handle concurrent connections — usually well under 100. Do **not** raise HikariCP to 10,000 just because VTs are cheap. Postgres will die.

---

## 7.4 `ScopedValue` and `StructuredTaskScope`

### The `ThreadLocal` problem under VTs

`ThreadLocal` is a `Map<Thread, Value>`. With 100,000 VTs, you can accumulate 100,000 map entries — sometimes carrying large objects (security contexts, MDC dictionaries, JDBC connections). Memory bloat + no automatic cleanup.

### `ScopedValue` (Java 21 preview, standardized in 25) — the modern replacement

```java
public static final ScopedValue<AuthContext> AUTH = ScopedValue.newInstance();

// At the request boundary:
ScopedValue.where(AUTH, ctx).run(() -> {
    // AUTH.get() works anywhere inside this scope, including any VT spawned here
    handleRequest();
});
```

**Why it's better than `ThreadLocal`:**
- **Immutable** — set once at the scope entry, cannot be `set()` again mid-flight. No accidental mutation bugs.
- **Scoped** — value is automatically dropped when the scope exits. No `remove()` boilerplate. No leaks.
- **Inherited** by child VTs launched within a `StructuredTaskScope`.
- **Faster reads** than `ThreadLocal` (special JVM support).

Use for: security context, tenant ID, trace ID, request ID.

### `StructuredTaskScope` — safe fan-out

The problem it solves: "I want to launch 3 subtasks in parallel, cancel them all if one fails, propagate exceptions, and never leak a thread."

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<User>    user    = scope.fork(() -> loadUser(id));
    Subtask<Address> address = scope.fork(() -> loadAddress(id));
    Subtask<Orders>  orders  = scope.fork(() -> loadOrders(id));

    scope.join();               // waits for all
    scope.throwIfFailed();      // rethrows if any failed → siblings already cancelled

    return new Profile(user.get(), address.get(), orders.get());
}
```

### Two flavors that matter

- **`ShutdownOnFailure`** — first failure cancels the rest (all-or-nothing).
- **`ShutdownOnSuccess`** — first success cancels the rest (race semantics; use for latency-sensitive fanout).

### Why this beats `CompletableFuture.allOf(...)`

- Automatic cancellation of siblings on failure — with plain `CF`, siblings keep running and burn resources.
- Ownership is explicit — the try-with-resources guarantees no orphan tasks.
- Errors carry stack traces from the actual origin, not `CompletionException` wrapping.
- Cleaner debugging: JFR treats the scope as a coherent tree.

### The status of these APIs

- `ScopedValue` — final in **Java 25**, preview earlier.
- `StructuredTaskScope` — final in **Java 25** with the `Subtask` API surface. If your interviewer targets JDK 21, be honest: "It was preview then, standardized in 25."

---

## 7.5 Virtual Threads vs Reactive (WebFlux, Reactor)

### The core comparison

| Dimension | Virtual Threads | Reactive (Reactor, Rx) |
|---|---|---|
| Programming model | Imperative, blocking-style code | Declarative flows (`Mono`, `Flux`, operators) |
| Backpressure | Implicit (blocking queues / semaphores you add) | First-class (`request(n)` protocol) |
| Learning curve | Familiar to any Java dev | Steep; whole new mental model |
| Debuggability | Stack traces "just work" | Async stack fragments; needs Reactor context |
| Throughput per core | Very high for I/O-bound blocking work | Very high for streaming/pipeline work |
| Best fit | Request/response services with lots of I/O | Streaming, event-processing, tail-latency-critical |
| Downside | Downstream exhaustion trap; pinning edge cases | Complexity; harder to reason about |

### The honest L5 answer

> "For a typical request/response microservice hitting a DB and a couple of downstream APIs — virtual threads win now: keep the imperative code, get scalability for free, sidestep the debugging tax of Reactor. For streaming / event-driven pipelines with true backpressure (Kafka streams, SSE broadcasts, WebSocket fanout) — Reactor's operator model is still superior. Not because it's faster, but because backpressure and composition are first-class."

### The migration playbook

1. Start VT-per-request at the servlet layer (`Tomcat` supports `virtual` threads via `spring.threads.virtual.enabled=true`).
2. Audit every downstream: DB pool, HTTP clients, message brokers. Gate each with a semaphore.
3. Replace `ThreadLocal`-based context (Security, MDC) with `ScopedValue` where possible.
4. Turn on `-Djdk.tracePinnedThreads=short` in staging for a week. Watch JFR `jdk.VirtualThreadPinned`.
5. Load-test at 5× your baseline. Watch downstream latency and connection pool wait times.

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | Bad answer | L5 answer |
|---|---|---|
| "What's a virtual thread?" | "A lightweight thread." | "A `Thread` whose stack lives in the heap as a continuation; scheduled M:N on a small set of carrier threads via ForkJoinPool. Unmounts on JDK blocking calls." |
| "Should I pool virtual threads?" | "Yes, N=200." | "No. Create one per task. Use `newVirtualThreadPerTaskExecutor()`. Reuse defeats the purpose." |
| "What pins today?" | "`synchronized`." | "In JDK 24+ (JEP 491) `synchronized` no longer pins. Residual pinners: JNI, class init, and some local-disk file I/O." |
| "Any downside of switching?" | "None, it's a drop-in." | "Downstream exhaustion. Anything you were implicitly limiting via pool size — DB connections, downstream APIs — must now be explicitly gated with `Semaphore`s." |
| "`ThreadLocal` in a VT world?" | "Same as always." | "Memory hazard at 100k+ VTs. Use `ScopedValue` — immutable, auto-cleaned, inheritable in `StructuredTaskScope`." |
| "VT vs Reactive?" | "VT is better now." | "Depends on the workload. VT for imperative request/response with I/O. Reactor for true streaming and first-class backpressure." |
| "Where do VTs run?" | "OS." | "On a dedicated `ForkJoinPool` of carrier platform threads (default: `availableProcessors()` carriers)." |

---

## 💻 L5-Grade Code Examples

### Example 1 — VT-per-request with downstream gating

```java
public final class OrderService {
    private final DataSource ds;
    private final HttpClient http;
    private final Semaphore dbPermits         = new Semaphore(20);
    private final Semaphore inventoryPermits  = new Semaphore(50);

    public void serve(List<Long> orderIds) throws Exception {
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Long id : orderIds) {
                vt.submit(() -> processOrder(id));
            }
        }
    }

    private void processOrder(long id) {
        try {
            dbPermits.acquire();
            Order o;
            try (Connection c = ds.getConnection()) { o = loadOrder(c, id); }
            finally { dbPermits.release(); }

            inventoryPermits.acquire();
            try { verifyStock(o); }
            finally { inventoryPermits.release(); }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("order {} failed", id, e);
        }
    }
}
```

Points to say aloud:
- Unlimited VTs at the entry.
- **Every** external resource is gated.
- `try (ExecutorService …)` guarantees join before the method returns.

### Example 2 — Structured fan-out with automatic cancel

```java
public UserProfile buildProfile(long userId) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        var user     = scope.fork(() -> userRepo.load(userId));
        var address  = scope.fork(() -> addressRepo.load(userId));
        var orders   = scope.fork(() -> orderRepo.loadRecent(userId));

        scope.join();
        scope.throwIfFailed();

        return new UserProfile(user.get(), address.get(), orders.get());
    }
}
```

Contrast with a naive `CompletableFuture.allOf` — if `addressRepo` throws, `userRepo` and `orderRepo` **keep running to completion** and burn resources. The structured scope cancels siblings.

### Example 3 — `ScopedValue` for request context

```java
public final class RequestContext {
    public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> TENANT   = ScopedValue.newInstance();
}

public void handle(HttpRequest req) {
    ScopedValue
        .where(RequestContext.TRACE_ID, req.header("X-Trace-Id"))
        .where(RequestContext.TENANT,   req.header("X-Tenant"))
        .run(() -> processInScope(req));
}

private void processInScope(HttpRequest req) {
    // Anywhere inside — including in forked VTs — RequestContext.TRACE_ID.get() works.
    logger.info("handling in trace={}", RequestContext.TRACE_ID.get());
}
```

Key: no `try/finally { ThreadLocal.remove(); }` cleanup boilerplate. Scope exit does it for you.

### Example 4 — Detecting pinning with a JFR one-liner

```bash
java -XX:StartFlightRecording=duration=60s,filename=app.jfr \
     -Djdk.tracePinnedThreads=short \
     -jar app.jar
```
Then:
```bash
jfr summary app.jfr | grep -i pinned
jfr print --events jdk.VirtualThreadPinned app.jfr | head
```

Combine with `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints` for cleaner stacks. **Say this in the interview** — pinning detection is exactly the kind of thing L5 is expected to know at the tooling level.

---

## 🏭 Production War Stories

**1. The Hikari meltdown after switching to VTs.**
Team enabled `spring.threads.virtual.enabled=true` on Boot 3.2. Load test spiked from 500 concurrent requests to 50k. HikariCP (size 10) queue times exploded to 10 s+, connection acquire timeouts fired everywhere. **Fix**: added a request-level `Semaphore(15)` and reset expectations — "VTs give scalability at the app layer, not at the DB."

**2. The synchronized pinning that only showed under load.**
Legacy code path used `synchronized` around a shared config cache. In light load, everything fine. Under 20k concurrent VTs, all carriers ended up pinned on that monitor → app deadlocked. This was JDK 21. **Fix (immediate)**: replaced `synchronized` with `ReentrantLock`. **Fix (permanent)**: upgraded to JDK 24 (JEP 491).

**3. The ThreadLocal memory leak.**
A tenancy-context library set a `ThreadLocal<TenantCtx>` at request start with a small map. With platform threads (pool 200), overhead was 200 × ~2 KB = trivial. After switching to VTs, average concurrent requests jumped to 30k, so 30k × 2 KB = 60 MB of dead context objects held until GC. **Fix**: migrated to `ScopedValue`; heap footprint dropped to ~0.

**4. The JNI pinning surprise.**
An in-process ML inference call ran a native `predict()` via JNI (~100 ms). Under VT load, all 8 carriers got pinned to those calls, throughput dropped to 80 req/s. **Fix**: routed native calls through a dedicated platform-thread `ExecutorService` (`Executors.newFixedThreadPool(16)`). VTs `await` on its `Future`s.

**5. The `newFixedThreadPool` of virtual threads.**
Someone did `Executors.newFixedThreadPool(1000, Thread.ofVirtual().factory())`. That's a pool of VTs — defeating the whole design. Under load, tasks queued on the pool's `LinkedBlockingQueue` while VTs sat idle. **Fix**: `newVirtualThreadPerTaskExecutor()`.

---

## 🎯 Self-Check

1. What happens (in memory / on the CPU) when a VT calls `Socket.getInputStream().read()`?
2. What is a carrier thread? How many exist by default?
3. Name three things that still pin a VT in modern JDK.
4. What flag prints pinning stacks at runtime? What JFR event should you watch?
5. Explain the downstream exhaustion problem in ≤ 3 sentences.
6. Why is `ScopedValue` safer than `ThreadLocal` under VT load?
7. Compare `StructuredTaskScope` to `CompletableFuture.allOf` — what does it do better?
8. When would you pick Reactor over VTs today?
9. Should you pool virtual threads? What's the idiomatic executor?
10. You see 90% of your `jdk.VirtualThreadPinned` events pointing to one `synchronized` block. What are your two fixes?

---

## ➡️ Next

Move to **Module 8 — Distributed Concurrency** (Redlock, fencing tokens, MVCC, isolation levels).
