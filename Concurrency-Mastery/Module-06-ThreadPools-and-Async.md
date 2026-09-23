# Module 6 — Thread Pools & Async Pipelines

> **Goal:** You can draw the `ThreadPoolExecutor` routing state machine on a whiteboard, size a pool with the CPU/IO formula and justify it, name every rejection policy and pick correctly, and explain why `CompletableFuture.thenApply` without an explicit `Executor` is a production hazard.

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────┐
                     │        MODULE 6 CORE               │
                     └────────────────────────────────────┘
                                    │
       ┌────────────────────┬───────┴─────────┬────────────────────┐
       │                    │                 │                    │
   6.1 ThreadPool-      6.2 Executors.*    6.3 ForkJoinPool    6.4 Completable-
   Executor             factory traps      (work stealing,     Future
   (Core→Queue→Max,     (Fixed/Cached/     LIFO own,           (thenApply,
   sizing, rejection,   Single/Scheduled)  FIFO steal,         thenCompose,
   hooks, shutdown)                        commonPool)         allOf, timeout,
                                                                exception)
```

---

## 6.1 `ThreadPoolExecutor` — The Only Pool You Really Need To Know

### Real-world analogy

> Think of a **restaurant kitchen**.
> - **Core pool size** = the always-scheduled full-time cooks.
> - **Work queue** = the ticket rail.
> - **Max pool size** = full-timers + on-call cooks you'll pull in during a rush.
> - **Keep-alive time** = how long an on-call cook waits idle before you send them home.
> - **Rejection policy** = what you do when the ticket rail is full and there are no more cooks: turn the customer away, hand the order back to the waiter, silently drop it, etc.

Every parameter you pass to the constructor maps to one of these.

### The constructor — memorize the seven parameters

```java
new ThreadPoolExecutor(
    int corePoolSize,          // 1) baseline threads
    int maximumPoolSize,       // 2) hard cap on total threads
    long keepAliveTime,        // 3) idle-to-death for non-core threads
    TimeUnit unit,             // 4)
    BlockingQueue<Runnable>,   // 5) the ticket rail
    ThreadFactory,             // 6) how to name/setup workers
    RejectedExecutionHandler); // 7) what to do when full
```

### The routing state machine (this is *the* interview drawing)

```
             ┌───────────── execute(task) ─────────────┐
             ▼                                          │
     workers.size < core?  ── YES ─►  startWorker(task)│
             │
             │ NO
             ▼
     queue.offer(task)   ── SUCCESS ─►  queued; workers pick it up
             │
             │ FAILURE (queue full)
             ▼
     workers.size < max?  ── YES ─►  startWorker(task)
             │
             │ NO
             ▼
     RejectedExecutionHandler.rejectedExecution(...)
```

Interviewer wants to hear:
1. **Cores fill first.**
2. **Then the queue.**
3. **Only if queue is full do we grow to max.**
4. **If both are full, reject.**

The single most common intuition failure is thinking "we spin up new threads as soon as we go past core." **We don't** — the queue absorbs first. This is *by design* — creating threads is expensive and reused threads keep caches warm.

### Why `newFixedThreadPool` is a trap

`Executors.newFixedThreadPool(n)` boils down to:
```java
new ThreadPoolExecutor(n, n, 0, MS, new LinkedBlockingQueue<>());
```

`LinkedBlockingQueue` with no capacity = `Integer.MAX_VALUE`. Downstream slows, backlog grows silently to millions, GC pauses balloon, heap OOMs. Never use this in production.

Same warning for `newCachedThreadPool` — max is `Integer.MAX_VALUE` threads. Under overload, you'll try to spawn tens of thousands of OS threads → the OS refuses → app dies.

### The production template

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    /* core       */ Runtime.getRuntime().availableProcessors(),
    /* max        */ Runtime.getRuntime().availableProcessors() * 2,
    /* keepAlive  */ 60, TimeUnit.SECONDS,
    /* queue      */ new ArrayBlockingQueue<>(1000),
    /* factory    */ Thread.ofPlatform().name("payment-worker-", 0).factory(),
    /* rejection  */ new ThreadPoolExecutor.CallerRunsPolicy()
);
pool.allowCoreThreadTimeOut(true);   // let idle cores die during troughs
```

### Sizing formulas

For **CPU-bound** work (crypto, compression, JSON parsing on big payloads):

$$
\text{Threads}_{CPU} = \text{Cores} + 1
$$

+1 for the occasional page fault that gives another thread a slot.

For **I/O-bound** work (HTTP calls, DB queries):

$$
\text{Threads}_{IO} = \text{Cores} \times \left(1 + \frac{\text{Wait Time}}{\text{Compute Time}}\right)
$$

Real numbers: if a request spends 90 ms on the network and 10 ms on CPU:

$$
\text{Threads} = 8 \times \left(1 + \frac{90}{10}\right) = 80
$$

Rule of thumb an L5 should give:
- CPU-heavy service: **~N**
- Mostly I/O microservice: **~50–200**
- Mostly waiting on remote calls: **use virtual threads or reactive** (Module 7).

**Say aloud:** *"These formulas are starting points, not answers. In production you measure with a load test and adjust based on the p99 vs saturation curve."*

### Rejection policies — pick correctly

| Policy | Behavior | When |
|---|---|---|
| `AbortPolicy` (default) | Throws `RejectedExecutionException` | Bounded systems where callers must know |
| `CallerRunsPolicy` | Producer thread runs the task itself | **The backpressure default** — slows the producer |
| `DiscardPolicy` | Silently drops the newest task | Best-effort metrics / logs |
| `DiscardOldestPolicy` | Drops the head of the queue, retries offer | Live-only data (market ticks, position updates) |

For an L5 answer: **default to `CallerRunsPolicy`.** It's the only one that actually implements backpressure — the caller pays the cost of overload instead of throwing errors or dropping work.

### Hooks (`beforeExecute` / `afterExecute` / `terminated`)

Subclass `ThreadPoolExecutor` to plug in:
- **MDC propagation** — copy trace IDs into worker thread state (see Module 10).
- **Metrics** — record task latency, active count.
- **Uncaught exception surfacing** — `afterExecute(Runnable r, Throwable t)` surfaces failures from `submit()` (which otherwise swallows them in a `Future`).

```java
@Override
protected void afterExecute(Runnable r, Throwable t) {
    if (t == null && r instanceof Future<?> f && f.isDone()) {
        try { f.get(); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        catch (ExecutionException ee)   { t = ee.getCause(); }
        catch (CancellationException ignored) {}
    }
    if (t != null) logger.error("task failed", t);
}
```

### The `submit` vs `execute` trap

- `execute(Runnable)` — uncaught exceptions propagate to the thread's `UncaughtExceptionHandler`.
- `submit(Runnable/Callable)` — wraps into a `FutureTask`. Exceptions are stored in the `Future`. **If you never call `.get()` on the future, the exception is silently lost.**

L5 diagnostic tip: if your service seems to "just skip work," check if you're using `submit()` without draining futures.

### Shutdown semantics

- `shutdown()` — no new tasks accepted; existing queue drains, running tasks finish.
- `shutdownNow()` — no new tasks; **interrupt** running workers; returns undrained queue.
- `awaitTermination(timeout)` — wait for full stop, up to timeout.

The safe production shutdown:
```java
pool.shutdown();
if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
    List<Runnable> undone = pool.shutdownNow();
    logger.warn("forced shutdown, {} tasks abandoned", undone.size());
    if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
        logger.error("pool did not terminate");
    }
}
```

---

## 6.2 The `Executors.*` Factories (and Why L5 Avoids Them)

| Factory | Under the hood | Real problem |
|---|---|---|
| `newFixedThreadPool(n)` | `TPE(n, n, 0, MS, LinkedBlockingQueue)` | Unbounded queue → OOM |
| `newCachedThreadPool()` | `TPE(0, MAX_VALUE, 60s, SynchronousQueue)` | Unbounded threads → OS refuses |
| `newSingleThreadExecutor()` | `TPE(1, 1, 0, MS, LinkedBlockingQueue)` | Same unbounded queue trap |
| `newScheduledThreadPool(n)` | `ScheduledThreadPoolExecutor` | Fixed core, unbounded delayed queue, uncaught task exceptions **kill the schedule** |

The `ScheduledThreadPoolExecutor` "schedule dies on error" bug is real: if a `scheduleAtFixedRate` task throws once, the future is cancelled and *the task never runs again*. Wrap the body in try/catch or use a resilient scheduler.

### The one factory you might actually use

`newVirtualThreadPerTaskExecutor()` — spawns a virtual thread per task (Module 7). Backed by the ForkJoin common pool at the carrier level. Great for I/O-heavy work.

---

## 6.3 `ForkJoinPool` — Work Stealing

### Real-world analogy

> Every worker gets their **own todo list** (deque). They push and pop from the **top** of their own list — cache-warm, no contention. When a worker's list empties, they steal from **the bottom** of a randomly chosen busy worker's list. That way owners and thieves rarely contend on the same end.

### The technical picture

```
       Worker 1                Worker 2                Worker 3
    ┌───────────┐            ┌───────────┐          ┌───────────┐
    │  push/pop │  own tasks │  push/pop │  own    │  push/pop │
    │  (LIFO)   │            │  (LIFO)   │  tasks  │  (LIFO)   │
    │           │            │           │          │           │
    │  ...      │            │  ...      │◄──steal──│    …      │
    │           │            │           │   (FIFO) │           │
    │           │            │           │──────────►    …      │
    └───────────┘            └───────────┘          └───────────┘
     idle → steal              busy                   busy
```

- **Own end: LIFO** — the most recently pushed task is likely still hot in cache.
- **Thief end: FIFO** — thieves take the oldest, coarsest task, giving them enough work to justify the steal.

### When to use

- **Recursive divide-and-conquer** work: `RecursiveTask`, `RecursiveAction`. Split until small enough, then compute.
- **Parallel streams** (they run on `ForkJoinPool.commonPool` by default).
- **CompletableFuture default executor** (also `commonPool`).

### The commonPool trap (an L5 favorite)

`ForkJoinPool.commonPool()` is shared by:
- Parallel streams.
- `CompletableFuture.thenApplyAsync(fn)` (no explicit executor).
- Anyone else in the JVM who defaults to it.

Its parallelism defaults to `Runtime.availableProcessors() - 1`. If your business logic dumps a blocking DB call into a parallel stream or into `thenApplyAsync` **without an explicit executor**, you starve the commonPool and every unrelated parallel operation across your app slows down.

**Rules to say aloud:**
- Never do blocking I/O inside a parallel stream.
- Never call `thenApplyAsync(fn)` without an explicit `Executor` unless the lambda is CPU-only and short.
- If you must, wrap blocking work in `ForkJoinPool.ManagedBlocker` — it lets the pool spin up a compensation thread.

### Sizing `ForkJoinPool`

Override commonPool size:
```
-Djava.util.concurrent.ForkJoinPool.common.parallelism=16
```

Or construct a dedicated one for your workload — the L5 default.

---

## 6.4 `CompletableFuture` — Async Composition Done Right

### The mental model

A `CompletableFuture<T>` is a **placeholder** for a value that will exist later. You attach continuations (`thenApply`, `thenCompose`, `thenCombine`, `whenComplete`, etc.) — each returns a *new* stage that runs when the previous is done.

Think **pipes**, not callbacks. This is Reactive-Streams-lite baked into the JDK.

### The three async modes (this catches candidates)

| Method | Where the continuation runs |
|---|---|
| `thenApply(fn)` | The thread that **completed** the previous stage — could be the caller, could be the async worker |
| `thenApplyAsync(fn)` | `ForkJoinPool.commonPool` |
| `thenApplyAsync(fn, executor)` | Your executor |

The subtle bug: `thenApply(heavyWork)` may run on the caller's thread (the one that just returned from `CompletableFuture.supplyAsync(...)`). If the caller was a hot request-serving thread, you just blocked it. Always use `thenApplyAsync(fn, myExecutor)` when the continuation is non-trivial.

### The composition vocabulary

| Op | Shape | Use |
|---|---|---|
| `thenApply(fn)` | `T → U` | Map |
| `thenAccept(cn)` | `T → void` | Side effect at the end |
| `thenCompose(fn)` | `T → CF<U>` | `flatMap`; chain async → async |
| `thenCombine(other, bi)` | `T, U → V` | Zip two independent async results |
| `applyToEither(other, fn)` | first-completed `T → U` | Race two sources |
| `allOf(...)` | `CF<Void>` — completes when **all** finish | Fan-out |
| `anyOf(...)` | `CF<Object>` — completes when **any** finishes | Race |
| `handle((v, ex) -> ...)` | Handles both success and failure | Unified recovery |
| `exceptionally(ex -> fallback)` | Recovery only on error | Fallback |
| `whenComplete((v, ex) -> ...)` | Peek without changing result | Logging |
| `orTimeout(t, unit)` (Java 9+) | Fail if not done in time | Deadlines |
| `completeOnTimeout(v, t, unit)` | Default value on timeout | Fallback |

### The "always pass an executor" rule

```java
// BAD — non-obvious pool, easy to starve commonPool
CompletableFuture.supplyAsync(this::loadUser)
    .thenApplyAsync(this::enrichUser)                       // commonPool
    .thenAcceptAsync(this::sendResponse);                   // commonPool

// GOOD — every stage explicitly bound
CompletableFuture.supplyAsync(this::loadUser, ioPool)
    .thenApplyAsync(this::enrichUser, cpuPool)
    .thenAcceptAsync(this::sendResponse, ioPool);
```

### Error handling — pick `handle` or `exceptionally`

```java
CompletableFuture<UserProfile> profile = fetchAsync(userId, ioPool)
    .thenApplyAsync(this::normalize, cpuPool)
    .exceptionally(ex -> {
        metrics.error("profile_fetch_failed", ex);
        return UserProfile.EMPTY;
    });
```

`handle((v, ex) -> ...)` runs on **both** success and failure — use it when you want a single terminal transform.

### Timeouts (Java 9+)

```java
CompletableFuture<Response> resp = callDownstream(req, ioPool)
    .orTimeout(300, TimeUnit.MILLISECONDS)                  // TimeoutException on stall
    .exceptionally(ex -> Response.degraded());
```

Before Java 9 you had to use `CompletableFuture.anyOf(cf, timeoutCf)` — clunky. `orTimeout`/`completeOnTimeout` are the modern way.

### `allOf` — fan-out with correct result plumbing

`CompletableFuture.allOf(...)` returns `CF<Void>`. To pull the child results out:

```java
List<CompletableFuture<Order>> futures = ids.stream()
    .map(id -> CompletableFuture.supplyAsync(() -> loadOrder(id), ioPool))
    .toList();

CompletableFuture<List<Order>> all = CompletableFuture
    .allOf(futures.toArray(CompletableFuture[]::new))
    .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
```

`.join()` after `allOf` is safe **because** all are already complete — no blocking. This is the idiomatic L5 pattern.

### Cancellation — the honest answer

`CompletableFuture.cancel(true)` marks the future as `CancellationException` but **does not interrupt** the running thread. Because CF stages don't own their threads, cancellation propagates *forward* through the pipeline (downstream stages get a `CancellationException`), not *backward* into the running I/O. If you need real cancellation of the I/O, hold a handle to the underlying `Future` from your `ExecutorService`.

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | Bad answer | L5 answer |
|---|---|---|
| "What's the routing order in `TPE`?" | "Threads first, then queue." | "Fill **core**, then **queue**, then grow to **max**, then **reject**. Grow-to-max only happens on queue-full." |
| "Which factory should I use?" | "`Executors.newFixedThreadPool`." | "None of them. Build a `TPE` directly with a bounded queue + `CallerRunsPolicy`." |
| "How do you size a pool?" | "N cores." | "Depends on the profile. CPU-bound ≈ N+1. IO-bound ≈ N × (1 + wait/compute). Confirm with a load test, tune from p99." |
| "Best rejection policy?" | "Abort — surface the error." | "`CallerRunsPolicy` — it's the only one that implements real backpressure." |
| "`thenApply` vs `thenApplyAsync`?" | "One's async." | "`thenApply` runs on the completing thread — could be your caller. `thenApplyAsync` runs on an executor. Always pass one explicitly." |
| "What runs on `ForkJoinPool.commonPool` by default?" | "Not sure." | "Parallel streams, `thenApplyAsync` without executor, and anyone else in the JVM. Sharing = starvation risk." |
| "How does `submit()` differ from `execute()`?" | "Returns a future." | "Also **swallows exceptions** into the future. If nobody calls `.get()`, the failure is silent." |

---

## 💻 L5-Grade Code Examples

### Example 1 — Production-shaped `ThreadPoolExecutor`

```java
public final class Pools {
    public static ThreadPoolExecutor io(String name, int queueCap) {
        int n = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(
                n, n * 4,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCap),
                Thread.ofPlatform().name(name + "-", 0).factory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        tpe.allowCoreThreadTimeOut(true);
        return tpe;
    }

    public static ThreadPoolExecutor cpu(String name) {
        int n = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                n, n,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(256),
                Thread.ofPlatform().name(name + "-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
```

Talking points:
- **Named threads** — critical for thread dumps and profiling.
- **Bounded queue everywhere** — no unbounded backlog.
- **IO pool uses `CallerRunsPolicy`** — backpressure.
- **CPU pool uses `AbortPolicy`** — surfaces the problem instead of silently degrading.

### Example 2 — Surfacing exceptions from `submit()` in a subclassed pool

```java
public final class LoggingPool extends ThreadPoolExecutor {
    private static final Logger log = LoggerFactory.getLogger(LoggingPool.class);

    public LoggingPool(int core, int max, BlockingQueue<Runnable> q) {
        super(core, max, 60, TimeUnit.SECONDS, q,
              Thread.ofPlatform().name("lp-", 0).factory(),
              new CallerRunsPolicy());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?> f && f.isDone()) {
            try { f.get(); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            catch (ExecutionException ee)   { t = ee.getCause(); }
            catch (CancellationException ignored) {}
        }
        if (t != null) log.error("task failed", t);
    }
}
```

### Example 3 — `CompletableFuture` fan-out with explicit pool and timeout

```java
Executor io = Pools.io("orders-io", 500);

public CompletableFuture<List<Order>> loadAll(List<Long> ids) {
    List<CompletableFuture<Order>> futures = ids.stream()
        .map(id -> CompletableFuture
            .supplyAsync(() -> repository.load(id), io)
            .orTimeout(200, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> Order.missing(id)))
        .toList();

    return CompletableFuture
        .allOf(futures.toArray(CompletableFuture[]::new))
        .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
}
```

Points to say:
- Each fetch has its own timeout — one slow shard doesn't sink the batch.
- On failure, fallback to `Order.missing(id)` — the caller still gets a full list.
- `join()` after `allOf` is safe: everything is already done.

### Example 4 — Wrapping blocking work with `ManagedBlocker`

```java
// Runs inside a ForkJoinPool worker; tells the pool "I'm about to block, feel free to compensate."
Object result = ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
    Object value;
    public boolean block() throws InterruptedException {
        value = blockingCall();
        return true;
    }
    public boolean isReleasable() { return value != null; }
});
```

Only reach for this if you must run blocking work inside a fork-join context (e.g., custom parallel algorithms). The cleaner answer is usually to move blocking work to a dedicated pool.

---

## 🏭 Production War Stories

**1. The `newFixedThreadPool` OOM.**
Order-processing team wired `Executors.newFixedThreadPool(50)`. Payment gateway slowed from 50 ms to 5 s during a partner outage. The unbounded `LinkedBlockingQueue` grew to 12 million tasks. Heap OOMed. Fix: `ArrayBlockingQueue(2000)` + `CallerRunsPolicy` = HTTP requests naturally slowed, no memory blowup.

**2. The `thenApplyAsync` starvation.**
Payments team called `.thenApplyAsync(this::encryptWithVault)` — no executor. `encryptWithVault` did a blocking Vault HTTP call. All 15 commonPool threads got stuck. Unrelated parallel streams (metrics aggregation) hung. Fix: dedicated `vaultPool` (200 threads) and explicit `thenApplyAsync(fn, vaultPool)`.

**3. The `scheduleAtFixedRate` that stopped scheduling.**
Health-check task threw once (NPE from a config reload). `ScheduledThreadPoolExecutor` cancelled the recurring schedule and never ran the health check again. Nagios lit up hours later. Fix: wrapped body in try/catch that logs and swallows.

**4. The `submit()` that hid a P0.**
Batch job did `pool.submit(job)` and never called `.get()`. Every third job threw `DataIntegrityViolationException`. Silently lost for weeks — no logs, no metrics — until a downstream count didn't match. Fix: switched to `execute()` with a `Thread.UncaughtExceptionHandler`, or subclassed `TPE` with `afterExecute` logging.

**5. The pool that "wasn't scaling."**
Team set `core=10, max=100, queue=LinkedBlockingQueue()`. Under load, only 10 threads ran; max was never reached. Root cause: unbounded queue always accepts, so we never hit the "queue full → grow to max" path. Fix: bounded queue.

---

## 🎯 Self-Check

1. Draw the `TPE` routing diagram from memory. Where do we grow to max?
2. Why is `Executors.newFixedThreadPool(n)` unsafe in production?
3. Sizing an IO-bound service: what formula, what would you measure to tune it?
4. Rank the four rejection policies by production suitability and explain.
5. Difference between `submit()` and `execute()` in exception handling.
6. What runs on `ForkJoinPool.commonPool` implicitly? Why is that a hazard?
7. LIFO vs FIFO in work-stealing deques — which is which and why?
8. `thenApply(fn)` vs `thenApplyAsync(fn)` — where does the continuation run?
9. Show the `allOf` + `join` pattern to collect fan-out results.
10. Give one production war story where a rejection policy choice would have prevented it.

---

## ➡️ Next

Move to **Module 7 — Virtual Threads / Loom** (M:N scheduling, carrier pinning, downstream exhaustion, `ScopedValue`, `StructuredTaskScope`).
