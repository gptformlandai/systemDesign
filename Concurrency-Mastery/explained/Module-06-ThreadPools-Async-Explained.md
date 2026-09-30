# Module 6: Thread Pools & Async Pipelines — The Fun, Deep-Dive Explanation

> This document explains `ThreadPoolExecutor` internals, pool sizing formulas, rejection policies, and `CompletableFuture` composition — like you're a junior dev who's never seen this before.

---

# Part 1: `ThreadPoolExecutor` — The Only Pool You Really Need

## 1.1 The Restaurant Kitchen Analogy

Before we dive into code, let's understand thread pools with a restaurant analogy:

```
+------------------------------------------------------------------+
|                    THE RESTAURANT KITCHEN                         |
+------------------------------------------------------------------+
|                                                                   |
|  CORE POOL SIZE = Full-time cooks (always scheduled)             |
|  +-------+  +-------+  +-------+  +-------+                      |
|  | Cook 1|  | Cook 2|  | Cook 3|  | Cook 4|  (4 full-timers)     |
|  +-------+  +-------+  +-------+  +-------+                      |
|                                                                   |
|  WORK QUEUE = The ticket rail                                    |
|  +------+------+------+------+------+------+------+------+       |
|  |Order1|Order2|Order3|Order4|Order5|Order6|Order7|Order8|       |
|  +------+------+------+------+------+------+------+------+       |
|  (Orders waiting to be cooked)                                   |
|                                                                   |
|  MAX POOL SIZE = Full-timers + On-call cooks                     |
|  +-------+  +-------+  +-------+  +-------+  +-------+  +-------+|
|  | Cook 1|  | Cook 2|  | Cook 3|  | Cook 4|  | Cook 5|  | Cook 6||
|  +-------+  +-------+  +-------+  +-------+  +-------+  +-------+|
|  (4 full-time)                    (2 on-call, called during rush)|
|                                                                   |
|  KEEP-ALIVE TIME = How long on-call cooks wait before going home |
|                                                                   |
|  REJECTION POLICY = What to do when kitchen is FULL:             |
|  - Turn customer away (AbortPolicy)                              |
|  - Make the waiter cook it themselves (CallerRunsPolicy)         |
|  - Silently throw the order away (DiscardPolicy)                 |
|  - Throw away the oldest order (DiscardOldestPolicy)             |
|                                                                   |
+------------------------------------------------------------------+
```

Every parameter in `ThreadPoolExecutor` maps to one of these concepts!

---

## 1.2 The Seven Parameters — Memorize These!

```java
new ThreadPoolExecutor(
    int corePoolSize,           // 1) Baseline threads (full-time cooks)
    int maximumPoolSize,        // 2) Hard cap on total threads
    long keepAliveTime,         // 3) Idle time before non-core threads die
    TimeUnit unit,              // 4) Time unit for keepAlive
    BlockingQueue<Runnable>,    // 5) The work queue (ticket rail)
    ThreadFactory,              // 6) How to create/name threads
    RejectedExecutionHandler    // 7) What to do when full
);
```

### Example with Real Values

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    4,                              // 4 core threads (always running)
    8,                              // max 8 threads total
    60, TimeUnit.SECONDS,           // idle threads die after 60s
    new ArrayBlockingQueue<>(100),  // queue holds 100 tasks
    Executors.defaultThreadFactory(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

---

## 1.3 The Routing State Machine — THE Interview Drawing!

This is the most important thing to understand about `ThreadPoolExecutor`:

```
                    execute(task)
                         |
                         v
            +------------------------+
            | workers.size < core?   |
            +------------------------+
                    |           |
                   YES          NO
                    |           |
                    v           v
            +-----------+   +------------------------+
            | Start new |   | queue.offer(task)      |
            | worker    |   +------------------------+
            | with task |           |           |
            +-----------+       SUCCESS       FAIL
                               (queued)    (queue full)
                                   |           |
                                   v           v
                              Workers     +------------------------+
                              will pick   | workers.size < max?    |
                              it up       +------------------------+
                                                  |           |
                                                 YES          NO
                                                  |           |
                                                  v           v
                                          +-----------+   +-----------+
                                          | Start new |   | REJECT!   |
                                          | worker    |   | (policy)  |
                                          | with task |   +-----------+
                                          +-----------+
```

### The Key Insight — Queue BEFORE Max!

**Most people get this wrong!**

```
WRONG mental model:
"When we have more tasks than core threads, we create more threads up to max."

CORRECT mental model:
1. Fill CORE threads first
2. Then fill the QUEUE
3. Only when queue is FULL do we grow to MAX
4. Only when both are full do we REJECT
```

**Why this design?**

Creating threads is expensive! Reusing threads keeps CPU caches warm. The queue absorbs bursts without the overhead of thread creation.

### Visual Timeline

```
Tasks arriving over time:

Task 1: workers=0, core=4  --> Create worker 1 (workers < core)
Task 2: workers=1, core=4  --> Create worker 2 (workers < core)
Task 3: workers=2, core=4  --> Create worker 3 (workers < core)
Task 4: workers=3, core=4  --> Create worker 4 (workers < core)
Task 5: workers=4, core=4  --> Queue task 5 (workers >= core, queue not full)
Task 6: workers=4, core=4  --> Queue task 6
Task 7: workers=4, core=4  --> Queue task 7
...
Task 104: workers=4, queue=100 --> Queue FULL!
Task 105: workers=4, max=8     --> Create worker 5 (queue full, workers < max)
Task 106: workers=5, max=8     --> Create worker 6
Task 107: workers=6, max=8     --> Create worker 7
Task 108: workers=7, max=8     --> Create worker 8
Task 109: workers=8, max=8     --> REJECT! (queue full AND workers >= max)
```

---

## 1.4 Why `Executors.newFixedThreadPool` Is a Trap

Let's look at what the factory actually creates:

```java
// What you write:
ExecutorService pool = Executors.newFixedThreadPool(10);

// What you get:
new ThreadPoolExecutor(
    10,                           // core = 10
    10,                           // max = 10 (same as core)
    0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue<>()   // UNBOUNDED QUEUE!
);
```

**The Problem: Unbounded Queue**

```
LinkedBlockingQueue with no capacity = Integer.MAX_VALUE = 2,147,483,647

What happens under load:

Normal:
Queue: [task1][task2][task3]  (3 tasks waiting)

Downstream slows down:
Queue: [task1][task2]...[task1000000]  (1 million tasks!)

Memory usage: EXPLODES
GC pauses: 30+ seconds
Eventually: OutOfMemoryError!
```

### The Same Problem with Other Factories

| Factory | The Trap |
|---------|----------|
| `newFixedThreadPool(n)` | Unbounded queue → OOM |
| `newCachedThreadPool()` | Unbounded threads (MAX_VALUE) → OS refuses to create more |
| `newSingleThreadExecutor()` | Unbounded queue → OOM |
| `newScheduledThreadPool(n)` | Unbounded delayed queue + exceptions kill the schedule |

---

## 1.5 The Production Template — What You Should Actually Use

```java
ThreadPoolExecutor pool = new ThreadPoolExecutor(
    // Core: number of CPUs
    Runtime.getRuntime().availableProcessors(),
    
    // Max: 2x CPUs (for I/O-bound work)
    Runtime.getRuntime().availableProcessors() * 2,
    
    // Keep-alive: 60 seconds
    60, TimeUnit.SECONDS,
    
    // BOUNDED queue!
    new ArrayBlockingQueue<>(1000),
    
    // Named threads (critical for debugging!)
    Thread.ofPlatform().name("payment-worker-", 0).factory(),
    
    // Backpressure policy
    new ThreadPoolExecutor.CallerRunsPolicy()
);

// Let idle core threads die during low traffic
pool.allowCoreThreadTimeOut(true);
```

### Why Each Choice Matters

```
+------------------------------------------------------------------+
| BOUNDED QUEUE (ArrayBlockingQueue)                                |
| - Prevents memory explosion                                       |
| - Forces backpressure when overloaded                            |
+------------------------------------------------------------------+
| NAMED THREADS                                                     |
| - Thread dumps show "payment-worker-3" instead of "pool-1-thread-3"|
| - Critical for debugging production issues                        |
+------------------------------------------------------------------+
| CallerRunsPolicy                                                  |
| - When queue is full, the CALLER runs the task                   |
| - This slows down the producer = natural backpressure            |
+------------------------------------------------------------------+
| allowCoreThreadTimeOut(true)                                      |
| - During low traffic, even core threads can die                  |
| - Saves resources during off-peak hours                          |
+------------------------------------------------------------------+
```

---

## 1.6 Pool Sizing Formulas

### For CPU-Bound Work

Work that's mostly computation: crypto, compression, JSON parsing, image processing.

```
Threads = Number of CPUs + 1

Why +1? 
- Occasional page faults or GC pauses
- Another thread can use the CPU while one is briefly blocked
```

```java
int cpuThreads = Runtime.getRuntime().availableProcessors() + 1;
// On an 8-core machine: 9 threads
```

### For I/O-Bound Work

Work that waits a lot: HTTP calls, database queries, file I/O.

```
Threads = CPUs × (1 + Wait Time / Compute Time)
```

**Example:**
```
A request spends:
- 90ms waiting for database
- 10ms doing CPU work

Threads = 8 × (1 + 90/10) = 8 × 10 = 80 threads

Why so many?
While one thread waits for the DB, 9 others can use that CPU!
```

### Rule of Thumb

| Workload Type | Thread Count |
|---------------|--------------|
| CPU-heavy (crypto, compression) | ~N (number of CPUs) |
| Mixed (typical web service) | ~2N to 4N |
| I/O-heavy (lots of HTTP calls) | ~50-200 |
| Mostly waiting (external APIs) | Consider virtual threads! |

### The Honest Answer

> "These formulas are starting points, not answers. In production, you measure with a load test and adjust based on the p99 latency vs saturation curve."

---

## 1.7 Rejection Policies — Pick Correctly!

When both the queue AND max threads are full, what do we do?

| Policy | What It Does | When to Use |
|--------|-------------|-------------|
| `AbortPolicy` | Throws `RejectedExecutionException` | When callers MUST know about rejection |
| `CallerRunsPolicy` | Caller's thread runs the task | **DEFAULT CHOICE** — implements backpressure |
| `DiscardPolicy` | Silently drops the task | Best-effort work (metrics, logs) |
| `DiscardOldestPolicy` | Drops oldest queued task, retries | Live-only data (stock ticks) |

### Why `CallerRunsPolicy` Is Usually Best

```
Without CallerRunsPolicy:
+----------+     +-------+     +---------+
| Producer | --> | Queue | --> | Workers |
| (fast)   |     | FULL! |     | (slow)  |
+----------+     +-------+     +---------+
     |
     v
  REJECTED! (or silently dropped)
  
  
With CallerRunsPolicy:
+----------+     +-------+     +---------+
| Producer | --> | Queue | --> | Workers |
| (fast)   |     | FULL! |     | (slow)  |
+----------+     +-------+     +---------+
     |
     v
  Producer runs the task itself!
  Producer is now SLOW (busy doing work)
  Upstream naturally slows down
  = BACKPRESSURE!
```

**This is the only policy that implements real backpressure.** The producer pays the cost of overload instead of throwing errors or dropping work.

---

## 1.8 The `submit()` vs `execute()` Trap

This catches many developers:

```java
// execute() - exceptions propagate to UncaughtExceptionHandler
pool.execute(() -> {
    throw new RuntimeException("Boom!");
});
// Exception is logged/handled by the thread's exception handler

// submit() - exceptions are SWALLOWED into the Future!
pool.submit(() -> {
    throw new RuntimeException("Boom!");
});
// NO EXCEPTION VISIBLE! It's stored in the Future.
// If you never call future.get(), you'll never know it failed!
```

### The Silent Failure Problem

```java
// This code has a bug that you'll never see:
for (Task task : tasks) {
    pool.submit(task);  // Some tasks throw exceptions
}
// No errors logged! No exceptions thrown!
// Tasks silently fail and you have no idea.

// Fix 1: Use execute() instead
for (Task task : tasks) {
    pool.execute(task);  // Exceptions go to UncaughtExceptionHandler
}

// Fix 2: Always call get() on the Future
for (Task task : tasks) {
    Future<?> f = pool.submit(task);
    futures.add(f);
}
for (Future<?> f : futures) {
    f.get();  // Now exceptions are thrown!
}

// Fix 3: Subclass ThreadPoolExecutor with afterExecute logging
```

---

## 1.9 Shutdown Semantics

```java
// GRACEFUL shutdown
pool.shutdown();
// - No new tasks accepted
// - Existing queued tasks will run
// - Running tasks will finish

// FORCEFUL shutdown
List<Runnable> abandoned = pool.shutdownNow();
// - No new tasks accepted
// - INTERRUPTS running workers
// - Returns tasks that were queued but never started

// Wait for completion
boolean finished = pool.awaitTermination(30, TimeUnit.SECONDS);
```

### The Production Shutdown Pattern

```java
public void shutdownGracefully(ExecutorService pool) {
    // Step 1: Stop accepting new tasks
    pool.shutdown();
    
    try {
        // Step 2: Wait for existing tasks to finish
        if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
            // Step 3: Force shutdown if still running
            List<Runnable> abandoned = pool.shutdownNow();
            logger.warn("Forced shutdown, {} tasks abandoned", abandoned.size());
            
            // Step 4: Wait a bit more for tasks to respond to interrupt
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.error("Pool did not terminate!");
            }
        }
    } catch (InterruptedException e) {
        // Step 5: If we're interrupted, force shutdown
        pool.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

---

## 1.10 Hooks: `beforeExecute` / `afterExecute` / `terminated`

You can subclass `ThreadPoolExecutor` to add custom behavior:

```java
public class InstrumentedPool extends ThreadPoolExecutor {
    
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        // Copy MDC context (trace IDs) to worker thread
        MDC.setContextMap(((MdcAwareRunnable) r).getContext());
    }
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        
        // Surface exceptions from submit() that would otherwise be swallowed
        if (t == null && r instanceof Future<?> f && f.isDone()) {
            try {
                f.get();
            } catch (ExecutionException e) {
                t = e.getCause();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (CancellationException ignored) {
            }
        }
        
        if (t != null) {
            logger.error("Task failed", t);
            metrics.increment("pool.task.failures");
        }
        
        // Clear MDC
        MDC.clear();
    }
    
    @Override
    protected void terminated() {
        super.terminated();
        logger.info("Pool terminated");
    }
}
```

---

# Part 2: `ForkJoinPool` — Work Stealing

## 2.1 The Todo List Analogy

Imagine an office where each worker has their own todo list:

```
+------------------------------------------------------------------+
|                    THE WORK-STEALING OFFICE                       |
+------------------------------------------------------------------+
|                                                                   |
|  Worker 1's Desk          Worker 2's Desk          Worker 3's Desk|
|  +-------------+          +-------------+          +-------------+|
|  | Task A      | (top)    | Task D      |          | Task G      ||
|  | Task B      |          | Task E      |          | Task H      ||
|  | Task C      | (bottom) | Task F      |          | (empty!)    ||
|  +-------------+          +-------------+          +-------------+|
|        ^                        ^                        |        |
|        |                        |                        |        |
|   Works on own              Works on own            "I'm idle!   |
|   tasks (LIFO)              tasks (LIFO)             Let me      |
|   Push/pop from top         Push/pop from top        STEAL from  |
|                                                      Worker 2!"  |
|                                                           |       |
|                                                           v       |
|                                    Worker 3 steals Task F (FIFO) |
|                                    from the BOTTOM of Worker 2   |
|                                                                   |
+------------------------------------------------------------------+

KEY INSIGHT:
- Owner works from TOP (LIFO) - most recent task, still in cache!
- Thief steals from BOTTOM (FIFO) - oldest, coarsest task
- Owner and thief rarely touch the same end = minimal contention!
```

## 2.2 Why LIFO for Owner, FIFO for Thief?

### Owner Uses LIFO (Last In, First Out)

```
Worker pushes tasks as they split work:

Push Task A (big)
+-------+
| A     |
+-------+

Push Task B (medium, split from A)
+-------+
| B     | <-- top
| A     |
+-------+

Push Task C (small, split from B)
+-------+
| C     | <-- top (most recent)
| B     |
| A     |
+-------+

Worker pops from top: gets Task C
- Task C was just created
- Task C's data is still in CPU cache
- FAST!
```

### Thief Uses FIFO (First In, First Out)

```
Thief steals from bottom:

+-------+
| C     | <-- owner working here
| B     |
| A     | <-- thief steals from here
+-------+

Thief gets Task A (the oldest, biggest task)
- Task A is coarse-grained (lots of work)
- Worth the overhead of stealing
- Thief won't need to steal again soon
```

## 2.3 The Technical Picture

```
       Worker 1 Deque           Worker 2 Deque           Worker 3 Deque
    +------------------+     +------------------+     +------------------+
    |                  |     |                  |     |                  |
    |  [Task] <- push  |     |  [Task] <- push  |     |  [Task] <- push  |
    |  [Task]    pop   |     |  [Task]    pop   |     |  [Task]    pop   |
    |  [Task]          |     |  [Task]          |     |                  |
    |  [Task]          |     |  [Task]          |     |    (empty!)      |
    |  [Task] <- steal |     |  [Task] <- steal |     |                  |
    |                  |     |       ^          |     |                  |
    +------------------+     +-------+----------+     +------------------+
                                     |
                                     |
                              Worker 3 steals here!
                              (random victim selection)
```

## 2.4 When to Use ForkJoinPool

### Good For: Recursive Divide-and-Conquer

```java
class SumTask extends RecursiveTask<Long> {
    private final long[] array;
    private final int start, end;
    private static final int THRESHOLD = 1000;
    
    SumTask(long[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }
    
    @Override
    protected Long compute() {
        int length = end - start;
        
        // Base case: small enough to compute directly
        if (length <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        }
        
        // Recursive case: split and fork
        int mid = start + length / 2;
        SumTask left = new SumTask(array, start, mid);
        SumTask right = new SumTask(array, mid, end);
        
        left.fork();  // Push to deque, another worker might steal it
        long rightResult = right.compute();  // Compute right half directly
        long leftResult = left.join();  // Wait for left half
        
        return leftResult + rightResult;
    }
}

// Usage
ForkJoinPool pool = new ForkJoinPool();
long sum = pool.invoke(new SumTask(array, 0, array.length));
```

### Also Used By

- **Parallel Streams** — `list.parallelStream()` runs on `ForkJoinPool.commonPool()`
- **CompletableFuture** — `thenApplyAsync(fn)` without executor uses `commonPool()`

## 2.5 The `commonPool` Trap — L5 Favorite!

`ForkJoinPool.commonPool()` is a **shared, JVM-wide pool**:

```
+------------------------------------------------------------------+
|                    ForkJoinPool.commonPool()                      |
|                    (shared by EVERYONE!)                          |
+------------------------------------------------------------------+
|                                                                   |
|  Parallel Streams:                                               |
|  list.parallelStream().map(x -> process(x))...                   |
|                                                                   |
|  CompletableFuture (no explicit executor):                       |
|  CompletableFuture.supplyAsync(() -> loadData())                 |
|      .thenApplyAsync(data -> transform(data))  // Uses commonPool!
|                                                                   |
|  Your colleague's code:                                          |
|  someLibrary.doParallelThing()  // Also uses commonPool!         |
|                                                                   |
+------------------------------------------------------------------+
|                                                                   |
|  Default parallelism = Runtime.availableProcessors() - 1         |
|  On an 8-core machine: only 7 threads!                           |
|                                                                   |
+------------------------------------------------------------------+
```

### The Starvation Problem

```java
// Your code:
CompletableFuture.supplyAsync(() -> {
    return httpClient.get("http://slow-api.com");  // BLOCKS for 5 seconds!
}).thenApplyAsync(response -> {
    return parse(response);
});

// This BLOCKS a commonPool thread for 5 seconds!
// If you do this 7 times, ALL commonPool threads are blocked!
// Now parallel streams in UNRELATED code stop working!
```

```
commonPool threads (7 total):

Thread 1: [BLOCKED on HTTP call...]
Thread 2: [BLOCKED on HTTP call...]
Thread 3: [BLOCKED on HTTP call...]
Thread 4: [BLOCKED on HTTP call...]
Thread 5: [BLOCKED on HTTP call...]
Thread 6: [BLOCKED on HTTP call...]
Thread 7: [BLOCKED on HTTP call...]

Meanwhile, somewhere else in your app:
list.parallelStream().map(x -> compute(x))...
// "Why is my parallel stream so slow?!"
// Because all commonPool threads are blocked by YOUR HTTP calls!
```

### The Rules

1. **NEVER** do blocking I/O inside a parallel stream
2. **NEVER** call `thenApplyAsync(fn)` without an explicit executor if `fn` does I/O
3. **ALWAYS** use a dedicated pool for blocking work

```java
// BAD
CompletableFuture.supplyAsync(() -> blockingHttpCall());

// GOOD
Executor ioPool = Executors.newFixedThreadPool(100);
CompletableFuture.supplyAsync(() -> blockingHttpCall(), ioPool);
```

---

# Part 3: `CompletableFuture` — Async Composition

## 3.1 The Mental Model

A `CompletableFuture<T>` is a **placeholder** for a value that will exist later:

```
+------------------------------------------------------------------+
|                    CompletableFuture<User>                        |
+------------------------------------------------------------------+
|                                                                   |
|  State: INCOMPLETE                                               |
|  +------------------+                                            |
|  |  (waiting...)    |                                            |
|  +------------------+                                            |
|                                                                   |
|  Continuations attached:                                         |
|  - thenApply(user -> user.getName())                            |
|  - thenAccept(user -> log.info("Got user: {}", user))           |
|                                                                   |
+------------------------------------------------------------------+

Later, when the async operation completes:

+------------------------------------------------------------------+
|                    CompletableFuture<User>                        |
+------------------------------------------------------------------+
|                                                                   |
|  State: COMPLETED                                                |
|  +------------------+                                            |
|  |  User("Alice")   |                                            |
|  +------------------+                                            |
|                                                                   |
|  Continuations RUN:                                              |
|  - thenApply runs -> returns "Alice"                            |
|  - thenAccept runs -> logs "Got user: Alice"                    |
|                                                                   |
+------------------------------------------------------------------+
```

Think **pipes**, not callbacks!

## 3.2 The Three Async Modes — This Catches Candidates!

| Method | Where Does the Continuation Run? |
|--------|----------------------------------|
| `thenApply(fn)` | The thread that **completed** the previous stage |
| `thenApplyAsync(fn)` | `ForkJoinPool.commonPool()` |
| `thenApplyAsync(fn, executor)` | Your specified executor |

### The Subtle Bug

```java
CompletableFuture.supplyAsync(() -> loadUser(), ioPool)
    .thenApply(user -> heavyComputation(user));  // WHERE DOES THIS RUN?
```

**Answer:** It runs on whatever thread completed `loadUser()`!

```
Scenario 1: loadUser() completes BEFORE thenApply is attached
- The calling thread (your main thread!) runs heavyComputation
- Your main thread is now blocked!

Scenario 2: loadUser() completes AFTER thenApply is attached
- The ioPool thread that finished loadUser() runs heavyComputation
- Your ioPool thread is now doing CPU work!
```

### The Rule: Always Be Explicit!

```java
// BAD - non-obvious which thread runs what
CompletableFuture.supplyAsync(this::loadUser)
    .thenApplyAsync(this::enrichUser)      // commonPool (might block it!)
    .thenAcceptAsync(this::sendResponse);  // commonPool

// GOOD - every stage explicitly bound
CompletableFuture.supplyAsync(this::loadUser, ioPool)
    .thenApplyAsync(this::enrichUser, cpuPool)
    .thenAcceptAsync(this::sendResponse, ioPool);
```

## 3.3 The Composition Vocabulary

### Transforming Values

```java
// thenApply: T -> U (like map)
CompletableFuture<String> name = userFuture.thenApply(user -> user.getName());

// thenAccept: T -> void (side effect)
userFuture.thenAccept(user -> log.info("Got user: {}", user));

// thenRun: void -> void (just run something after)
userFuture.thenRun(() -> log.info("User loading complete"));
```

### Chaining Async Operations

```java
// thenCompose: T -> CompletableFuture<U> (like flatMap)
CompletableFuture<Order> order = userFuture
    .thenCompose(user -> loadOrderAsync(user.getId()));
    
// Without thenCompose, you'd get CompletableFuture<CompletableFuture<Order>>!
```

### Combining Multiple Futures

```java
// thenCombine: Combine two independent futures
CompletableFuture<String> greeting = 
    userFuture.thenCombine(configFuture, (user, config) -> 
        config.getGreeting() + ", " + user.getName());

// allOf: Wait for ALL to complete
CompletableFuture<Void> all = CompletableFuture.allOf(future1, future2, future3);

// anyOf: Wait for ANY to complete (race)
CompletableFuture<Object> first = CompletableFuture.anyOf(future1, future2, future3);
```

### Error Handling

```java
// exceptionally: Handle errors with fallback
CompletableFuture<User> user = loadUserAsync(id)
    .exceptionally(ex -> {
        log.error("Failed to load user", ex);
        return User.ANONYMOUS;
    });

// handle: Handle both success and failure
CompletableFuture<String> result = loadUserAsync(id)
    .handle((user, ex) -> {
        if (ex != null) return "Error: " + ex.getMessage();
        return "User: " + user.getName();
    });

// whenComplete: Peek at result without changing it
loadUserAsync(id)
    .whenComplete((user, ex) -> {
        if (ex != null) metrics.increment("user.load.failure");
        else metrics.increment("user.load.success");
    });
```

### Timeouts (Java 9+)

```java
// orTimeout: Fail if not done in time
CompletableFuture<Response> response = callApi()
    .orTimeout(500, TimeUnit.MILLISECONDS);  // Throws TimeoutException

// completeOnTimeout: Default value on timeout
CompletableFuture<Response> response = callApi()
    .completeOnTimeout(Response.DEFAULT, 500, TimeUnit.MILLISECONDS);
```

## 3.4 The `allOf` + `join` Pattern — Idiomatic Fan-Out

`CompletableFuture.allOf(...)` returns `CompletableFuture<Void>`. How do you get the actual results?

```java
// Load multiple orders in parallel
List<Long> orderIds = List.of(1L, 2L, 3L, 4L, 5L);

// Step 1: Create a future for each order
List<CompletableFuture<Order>> futures = orderIds.stream()
    .map(id -> CompletableFuture.supplyAsync(() -> loadOrder(id), ioPool))
    .toList();

// Step 2: Wait for ALL to complete
CompletableFuture<Void> allDone = CompletableFuture.allOf(
    futures.toArray(CompletableFuture[]::new)
);

// Step 3: When all done, collect the results
CompletableFuture<List<Order>> allOrders = allDone.thenApply(v -> 
    futures.stream()
        .map(CompletableFuture::join)  // Safe! Already complete!
        .toList()
);
```

**Why is `join()` safe here?**

After `allOf` completes, ALL futures are guaranteed to be done. `join()` returns immediately without blocking.

## 3.5 Error Handling Patterns

### Pattern 1: Fallback on Error

```java
CompletableFuture<User> user = loadUserAsync(id)
    .exceptionally(ex -> User.ANONYMOUS);
```

### Pattern 2: Retry on Error

```java
public CompletableFuture<Response> callWithRetry(int maxRetries) {
    return callApi()
        .exceptionallyCompose(ex -> {
            if (maxRetries > 0) {
                return callWithRetry(maxRetries - 1);
            }
            return CompletableFuture.failedFuture(ex);
        });
}
```

### Pattern 3: Timeout with Fallback

```java
CompletableFuture<Response> response = callSlowApi()
    .orTimeout(500, TimeUnit.MILLISECONDS)
    .exceptionally(ex -> {
        if (ex instanceof TimeoutException) {
            return Response.TIMEOUT;
        }
        return Response.ERROR;
    });
```

## 3.6 Cancellation — The Honest Answer

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // Long running operation
    return slowOperation();
});

future.cancel(true);  // Cancel the future
```

**What actually happens:**

- The future is marked as `CancellationException`
- Downstream stages get `CancellationException`
- **BUT:** The running thread is NOT interrupted!

```
CompletableFuture doesn't own its threads!
It can't interrupt them.

If you need real cancellation of I/O:
- Hold a reference to the underlying Future from ExecutorService
- Or use a cancellable HTTP client
- Or check Thread.interrupted() in your code
```

---

# Part 4: Production War Stories

## War Story 1: The `newFixedThreadPool` OOM

**The Setup:**
```java
ExecutorService pool = Executors.newFixedThreadPool(50);
```

**What Happened:**
- Payment gateway slowed from 50ms to 5 seconds during partner outage
- Unbounded `LinkedBlockingQueue` grew to 12 million tasks
- Heap OOMed
- Service crashed

**The Fix:**
```java
new ThreadPoolExecutor(
    50, 50, 0, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(2000),  // BOUNDED!
    new CallerRunsPolicy()           // Backpressure!
);
```

---

## War Story 2: The `thenApplyAsync` Starvation

**The Setup:**
```java
CompletableFuture.supplyAsync(this::loadData)
    .thenApplyAsync(this::encryptWithVault);  // No executor!
```

**What Happened:**
- `encryptWithVault` did a blocking HTTP call to Vault
- All 7 commonPool threads got stuck on Vault calls
- Unrelated parallel streams (metrics aggregation) hung
- Entire app slowed down

**The Fix:**
```java
Executor vaultPool = Executors.newFixedThreadPool(200);

CompletableFuture.supplyAsync(this::loadData, ioPool)
    .thenApplyAsync(this::encryptWithVault, vaultPool);  // Explicit pool!
```

---

## War Story 3: The Schedule That Stopped

**The Setup:**
```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    healthCheck();  // Throws NPE once during config reload
}, 0, 30, TimeUnit.SECONDS);
```

**What Happened:**
- Health check threw NPE once
- `ScheduledThreadPoolExecutor` cancelled the recurring schedule
- Health check never ran again
- Nagios alerts fired hours later

**The Fix:**
```java
scheduler.scheduleAtFixedRate(() -> {
    try {
        healthCheck();
    } catch (Exception e) {
        log.error("Health check failed", e);
        // Don't rethrow! Schedule continues.
    }
}, 0, 30, TimeUnit.SECONDS);
```

---

## War Story 4: The Silent `submit()` Failures

**The Setup:**
```java
for (Job job : jobs) {
    pool.submit(job);  // Never called .get()!
}
```

**What Happened:**
- Every third job threw `DataIntegrityViolationException`
- Silently lost for weeks
- No logs, no metrics
- Discovered when downstream counts didn't match

**The Fix:**
```java
// Option 1: Use execute() with UncaughtExceptionHandler
for (Job job : jobs) {
    pool.execute(job);
}

// Option 2: Subclass TPE with afterExecute logging
// (See code example above)
```

---

## War Story 5: The Pool That "Wasn't Scaling"

**The Setup:**
```java
new ThreadPoolExecutor(
    10,                           // core
    100,                          // max
    60, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>()   // UNBOUNDED!
);
```

**What Happened:**
- Under load, only 10 threads ever ran
- Max of 100 was never reached
- Team thought the pool was broken

**Root Cause:**
- Unbounded queue ALWAYS accepts tasks
- We never hit "queue full → grow to max"
- The routing logic never triggered thread creation beyond core!

**The Fix:**
```java
new ThreadPoolExecutor(
    10, 100, 60, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(500)  // BOUNDED! Now max threads can be created.
);
```

---

# Part 5: Interview Traps

| Question | Bad Answer | L5 Answer |
|----------|-----------|-----------|
| "What's the routing order in TPE?" | "Threads first, then queue." | "Fill CORE, then QUEUE, then grow to MAX, then REJECT. Grow-to-max only happens on queue-full." |
| "Which factory should I use?" | "`Executors.newFixedThreadPool`" | "None of them. Build a TPE directly with bounded queue + CallerRunsPolicy." |
| "How do you size a pool?" | "N cores." | "Depends on workload. CPU-bound ≈ N+1. IO-bound ≈ N × (1 + wait/compute). Confirm with load test." |
| "Best rejection policy?" | "Abort — surface the error." | "CallerRunsPolicy — it's the only one that implements real backpressure." |
| "`thenApply` vs `thenApplyAsync`?" | "One's async." | "`thenApply` runs on the completing thread. `thenApplyAsync` runs on an executor. Always pass one explicitly." |
| "What runs on commonPool?" | "Not sure." | "Parallel streams, `thenApplyAsync` without executor, and anyone else. Sharing = starvation risk." |
| "`submit()` vs `execute()`?" | "Returns a future." | "Also SWALLOWS exceptions into the future. If nobody calls `.get()`, failures are silent." |

---

# Part 6: Self-Check Questions

1. **Draw the TPE routing diagram. Where do we grow to max?**
   - Only when queue is FULL and workers < max

2. **Why is `Executors.newFixedThreadPool(n)` unsafe?**
   - Unbounded queue → OOM under load

3. **Sizing an IO-bound service: formula and what to measure?**
   - Threads = N × (1 + wait/compute)
   - Measure p99 latency vs throughput, adjust

4. **Rank rejection policies by production suitability:**
   - CallerRunsPolicy (backpressure) > AbortPolicy (explicit failure) > DiscardOldestPolicy (live data) > DiscardPolicy (best effort)

5. **`submit()` vs `execute()` exception handling?**
   - `execute`: goes to UncaughtExceptionHandler
   - `submit`: swallowed into Future, silent if not `.get()`

6. **What runs on commonPool implicitly?**
   - Parallel streams, `thenApplyAsync` without executor

7. **LIFO vs FIFO in work-stealing?**
   - Owner: LIFO (cache-warm)
   - Thief: FIFO (coarse-grained)

8. **`thenApply(fn)` vs `thenApplyAsync(fn)` — where does continuation run?**
   - `thenApply`: completing thread
   - `thenApplyAsync`: commonPool (or specified executor)

9. **`allOf` + `join` pattern:**
   ```java
   CompletableFuture.allOf(futures.toArray(CF[]::new))
       .thenApply(v -> futures.stream().map(CF::join).toList());
   ```

10. **War story where rejection policy would have helped?**
    - `newFixedThreadPool` OOM — CallerRunsPolicy would have slowed producers

---

# Summary: The Complete Mental Model

```
+=========================================================================+
|                    MODULE 6: THREAD POOLS & ASYNC                        |
+=========================================================================+
|                                                                          |
|  ThreadPoolExecutor                                                     |
|  +--------------------------------------------------------------------+ |
|  |  Routing: CORE -> QUEUE -> MAX -> REJECT                           | |
|  |  Factories are traps (unbounded queues/threads)                    | |
|  |  Always: bounded queue + CallerRunsPolicy + named threads          | |
|  |  Sizing: CPU-bound ≈ N+1, IO-bound ≈ N × (1 + wait/compute)       | |
|  |  submit() swallows exceptions! Use execute() or check futures.     | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  ForkJoinPool                                                           |
|  +--------------------------------------------------------------------+ |
|  |  Work-stealing: owner LIFO, thief FIFO                             | |
|  |  commonPool is shared by parallel streams + CompletableFuture      | |
|  |  NEVER block in commonPool! Use dedicated pools for I/O.           | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  CompletableFuture                                                      |
|  +--------------------------------------------------------------------+ |
|  |  thenApply: runs on completing thread (maybe YOUR thread!)         | |
|  |  thenApplyAsync: runs on commonPool (starvation risk!)             | |
|  |  thenApplyAsync(fn, executor): runs on YOUR executor (GOOD!)       | |
|  |  allOf + join: idiomatic fan-out pattern                           | |
|  |  cancel() doesn't interrupt! CF doesn't own threads.               | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
+=========================================================================+
```

---

**You've completed Module 6!** You now understand `ThreadPoolExecutor` internals, pool sizing, rejection policies, `ForkJoinPool` work-stealing, and `CompletableFuture` composition.

**Next up:** [Module 7 — Virtual Threads / Loom](./Module-07-VirtualThreads-Explained.md)
