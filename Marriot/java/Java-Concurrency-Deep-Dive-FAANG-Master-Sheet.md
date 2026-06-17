# Java Concurrency Deep Dive FAANG Master Sheet

Target: Java backend interviews where the interviewer expects more than `Thread` basics.

This sheet covers:
- Thread lifecycle and race conditions
- Java Memory Model
- `volatile`, `synchronized`, locks, atomics
- CAS and AQS
- ExecutorService and ThreadPoolExecutor
- BlockingQueue and producer-consumer
- CountDownLatch, CyclicBarrier, Semaphore, Phaser
- ThreadLocal
- Deadlock, starvation, livelock
- Strong interview answers and mini programs

---

## 1. Mental Model

Concurrency means multiple tasks make progress during overlapping time.

Parallelism means tasks literally run at the same time on different CPU cores.

The hard part is not starting threads. The hard part is making shared state correct.

Simple memory picture:

```text
Thread A stack       Thread B stack
     |                    |
     | references          | references
     v                    v
              Shared heap objects
```

If two threads read/write the same object without coordination, the result can be wrong.

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Race condition | Very high | Base failure mode |
| Visibility vs atomicity | Very high | Most candidates mix these |
| `volatile` | Very high | Visibility and ordering |
| `synchronized` | Very high | Mutual exclusion and happens-before |
| `Lock` / `ReentrantLock` | High | Advanced locking control |
| CAS / atomics | High | Lock-free building block |
| ThreadPoolExecutor | Very high | Real backend execution model |
| BlockingQueue | High | Producer-consumer |
| ConcurrentHashMap | High | Shared map choice |
| CountDownLatch / Semaphore | Medium-high | Coordination primitives |
| Deadlock detection | High | Production maturity |
| ThreadLocal | High | Useful but leak-prone |
| AQS | Medium-high | Senior-level internal knowledge |

---

## 3. Core Definitions

| Term | Meaning |
|---|---|
| Thread | Smallest unit of execution scheduled by OS/JVM |
| Shared state | Data accessed by multiple threads |
| Race condition | Output depends on unsafe timing between threads |
| Critical section | Code that must not run concurrently for shared state |
| Visibility | Whether one thread sees another thread's writes |
| Atomicity | Whether an operation completes as one indivisible action |
| Ordering | Whether operations appear in the intended order |
| Happens-before | JMM rule that guarantees visibility and ordering |

Strong answer:

```text
Concurrency correctness requires handling three things: visibility, atomicity, and ordering.
volatile mainly gives visibility and ordering. synchronized gives mutual exclusion plus
visibility. Atomic classes give atomic updates using CAS for specific variables.
```

---

## 4. Thread Lifecycle

Common states:

| State | Meaning |
|---|---|
| NEW | Thread object created, not started |
| RUNNABLE | Eligible to run or currently running |
| BLOCKED | Waiting to enter synchronized monitor |
| WAITING | Waiting indefinitely for signal |
| TIMED_WAITING | Waiting for bounded time |
| TERMINATED | Execution finished |

Example:

```java
Thread t = new Thread(() -> System.out.println("running"));

System.out.println(t.getState()); // NEW
t.start();
```

Trap:

```text
Calling run() directly does not start a new thread. It is just a normal method call.
Calling start() asks the JVM to create a new execution path and then invoke run().
```

---

## 5. Race Condition

Bad counter:

```java
class UnsafeCounter {
    private int count;

    void increment() {
        count++;
    }

    int get() {
        return count;
    }
}
```

`count++` is not one operation.

It is conceptually:

```text
read count
add 1
write count
```

Two threads can read the same old value and overwrite each other.

Fix using `synchronized`:

```java
class SynchronizedCounter {
    private int count;

    synchronized void increment() {
        count++;
    }

    synchronized int get() {
        return count;
    }
}
```

Fix using `AtomicInteger`:

```java
import java.util.concurrent.atomic.AtomicInteger;

class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger();

    int increment() {
        return count.incrementAndGet();
    }

    int get() {
        return count.get();
    }
}
```

Interview line:

```text
AtomicInteger is cleaner for a single counter, while synchronized is more general when
multiple fields must be updated together under one invariant.
```

---

## 6. Java Memory Model

The Java Memory Model defines when writes by one thread become visible to another thread.

Without happens-before, one thread may not see another thread's latest writes.

Important happens-before rules:

| Rule | Meaning |
|---|---|
| Program order | Earlier action in one thread happens-before later action in same thread |
| Monitor unlock-lock | Unlocking a monitor happens-before later locking same monitor |
| Volatile write-read | Write to volatile happens-before later read of same volatile |
| Thread start | Actions before `start()` visible to started thread |
| Thread join | Finished thread's actions visible after successful `join()` |
| Final field rule | Properly constructed final fields are safely published |

Strong answer:

```text
The JMM is about visibility and ordering. It prevents us from assuming that writes in one
thread are automatically visible to another thread. We need happens-before relationships
through synchronized, volatile, thread start/join, final fields, or concurrency utilities.
```

---

## 7. `volatile`

`volatile` gives:

- Visibility of latest write.
- Ordering guarantees around volatile read/write.

`volatile` does not give:

- Mutual exclusion.
- Atomic compound operations like `count++`.

Good use:

```java
class StopFlag {
    private volatile boolean running = true;

    void stop() {
        running = false;
    }

    void runLoop() {
        while (running) {
            doWork();
        }
    }

    private void doWork() {
        // work
    }
}
```

Bad use:

```java
class BadCounter {
    private volatile int count;

    void increment() {
        count++; // not atomic
    }
}
```

Interview trap:

```text
volatile makes reads/writes visible, but it does not make read-modify-write operations atomic.
```

---

## 8. `synchronized`

`synchronized` provides:

- Mutual exclusion.
- Visibility.
- Reentrant monitor locking.

Method lock:

```java
class BankAccount {
    private int balance;

    synchronized void deposit(int amount) {
        balance += amount;
    }

    synchronized int balance() {
        return balance;
    }
}
```

Block lock:

```java
class BankAccount {
    private final Object lock = new Object();
    private int balance;

    void deposit(int amount) {
        synchronized (lock) {
            balance += amount;
        }
    }
}
```

Best practice:

```text
Prefer private lock objects instead of locking on public objects like this, String literals,
or Class objects unless that is intentionally part of the API.
```

---

## 9. `wait`, `notify`, And `notifyAll`

These work with intrinsic locks.

Rules:

- Must be called inside `synchronized`.
- `wait()` releases the monitor.
- `sleep()` does not release the monitor.
- Always wait inside a loop.

Pattern:

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }
    // use condition
}
```

Why loop?

```text
Threads can wake up without the condition being true. Also, another thread may consume
the condition before this thread resumes.
```

In modern code, prefer higher-level utilities like `BlockingQueue`, `CountDownLatch`, or `Condition`.

---

## 10. ReentrantLock

`ReentrantLock` gives more control than `synchronized`.

Features:

- `tryLock()`
- Timed lock attempts
- Interruptible lock acquisition
- Fairness option
- Multiple `Condition` objects

Example:

```java
import java.util.concurrent.locks.ReentrantLock;

class Inventory {
    private final ReentrantLock lock = new ReentrantLock();
    private int stock = 10;

    boolean reserve(int qty) {
        lock.lock();
        try {
            if (stock < qty) {
                return false;
            }
            stock -= qty;
            return true;
        } finally {
            lock.unlock();
        }
    }
}
```

Critical rule:

```text
Always unlock in finally.
```

`tryLock` example:

```java
if (lock.tryLock()) {
    try {
        // protected work
    } finally {
        lock.unlock();
    }
} else {
    // fallback
}
```

---

## 11. ReadWriteLock

Use when:

- Many readers.
- Few writers.
- Reads are long enough to benefit.

Example:

```java
import java.util.*;
import java.util.concurrent.locks.*;

class ConfigStore {
    private final ReadWriteLock rw = new ReentrantReadWriteLock();
    private final Map<String, String> config = new HashMap<>();

    String get(String key) {
        rw.readLock().lock();
        try {
            return config.get(key);
        } finally {
            rw.readLock().unlock();
        }
    }

    void put(String key, String value) {
        rw.writeLock().lock();
        try {
            config.put(key, value);
        } finally {
            rw.writeLock().unlock();
        }
    }
}
```

Trade-off:

```text
ReadWriteLock can improve read-heavy workloads, but it adds complexity and can perform
worse than synchronized for tiny critical sections.
```

---

## 12. StampedLock

`StampedLock` supports:

- Write lock
- Read lock
- Optimistic read

Example:

```java
import java.util.concurrent.locks.StampedLock;

class Point {
    private final StampedLock lock = new StampedLock();
    private double x;
    private double y;

    double distanceFromOrigin() {
        long stamp = lock.tryOptimisticRead();
        double currentX = x;
        double currentY = y;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    void move(double newX, double newY) {
        long stamp = lock.writeLock();
        try {
            x = newX;
            y = newY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
```

Interview caution:

```text
StampedLock is powerful but not reentrant. Use it only when optimistic reads clearly help.
```

---

## 13. CAS And Atomic Classes

CAS means Compare-And-Swap.

Mental model:

```text
If current value is still expectedValue, replace it with newValue.
Otherwise retry or fail.
```

Atomic example:

```java
import java.util.concurrent.atomic.AtomicReference;

class AtomicStatus {
    private final AtomicReference<String> status = new AtomicReference<>("NEW");

    boolean start() {
        return status.compareAndSet("NEW", "RUNNING");
    }
}
```

Pros:

- Avoids blocking for simple updates.
- Good under light/moderate contention.

Cons:

- Can spin under heavy contention.
- ABA problem can occur.
- Hard for multi-variable invariants.

ABA problem:

```text
Thread A reads value A.
Thread B changes A -> B -> A.
Thread A sees A and thinks nothing changed.
```

Mitigation:

- `AtomicStampedReference`
- Version numbers
- Locks for complex state

---

## 14. LongAdder vs AtomicLong

`AtomicLong`:

- Single atomic value.
- Better for low contention.
- Exact current value is straightforward.

`LongAdder`:

- Spreads updates across cells.
- Better under high contention counters.
- Sum is eventually aggregated.

Example:

```java
import java.util.concurrent.atomic.LongAdder;

class MetricsCounter {
    private final LongAdder requests = new LongAdder();

    void increment() {
        requests.increment();
    }

    long count() {
        return requests.sum();
    }
}
```

Interview line:

```text
For high-throughput metrics counters, LongAdder is often better than AtomicLong because it
reduces contention. For precise compare-and-set logic, AtomicLong is the right tool.
```

---

## 15. AQS

AQS means AbstractQueuedSynchronizer.

It is the framework behind many Java synchronizers:

- `ReentrantLock`
- `Semaphore`
- `CountDownLatch`
- `ReentrantReadWriteLock`

Mental model:

```text
AQS manages a state integer and a FIFO wait queue. Synchronizers define how to acquire
and release that state.
```

You usually do not implement AQS in normal backend work, but knowing it helps explain internals.

Strong answer:

```text
AQS is a foundation for building blocking locks and synchronizers. It uses a volatile state
plus CAS and a wait queue to manage threads that cannot acquire the synchronizer immediately.
```

---

## 16. ExecutorService

Do not create raw threads for every request.

Use ExecutorService:

```java
import java.util.concurrent.*;

public class ExecutorExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        Future<String> result = executor.submit(() -> "done");

        System.out.println(result.get());
        executor.shutdown();
    }
}
```

Why:

- Reuses threads.
- Controls concurrency.
- Separates task submission from execution.
- Provides lifecycle management.

---

## 17. ThreadPoolExecutor

Important constructor:

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    4,
    8,
    60,
    TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

Parameters:

| Parameter | Meaning |
|---|---|
| corePoolSize | Threads kept normally |
| maximumPoolSize | Upper limit if queue fills |
| keepAliveTime | Extra idle thread timeout |
| workQueue | Waiting tasks |
| threadFactory | Thread creation customization |
| rejectionHandler | What to do when overloaded |

Rejection policies:

| Policy | Behavior |
|---|---|
| AbortPolicy | Throws exception |
| CallerRunsPolicy | Caller executes task, provides backpressure |
| DiscardPolicy | Silently drops task |
| DiscardOldestPolicy | Drops oldest queued task |

Strong answer:

```text
Thread pool sizing depends on workload. CPU-bound pools are close to CPU core count.
Blocking IO pools can be larger, but must still respect downstream limits such as DB
connection pools and remote API capacity.
```

---

## 18. CPU-Bound vs IO-Bound Sizing

CPU-bound:

```text
threads ~= number of cores
```

IO-bound:

```text
threads can be higher because many threads wait on IO
```

But:

```text
More threads do not create more database connections, CPU, or remote service capacity.
```

Interview maturity line:

```text
I size thread pools together with queue capacity, rejection policy, timeout, and downstream
resource limits. An unbounded queue can hide overload until latency explodes.
```

---

## 19. BlockingQueue

BlockingQueue is the clean producer-consumer tool.

Example:

```java
import java.util.concurrent.*;

public class ProducerConsumerExample {
    public static void main(String[] args) {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

        Thread producer = new Thread(() -> {
            try {
                queue.put("job-1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                String job = queue.take();
                System.out.println(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
    }
}
```

Queue choices:

| Queue | Use |
|---|---|
| ArrayBlockingQueue | Bounded fixed-size queue |
| LinkedBlockingQueue | Optionally bounded linked queue |
| PriorityBlockingQueue | Priority-based tasks |
| SynchronousQueue | Direct handoff, no storage |
| DelayQueue | Delayed tasks |

---

## 20. CountDownLatch

Use when one or more threads wait for N events to complete.

```java
import java.util.concurrent.CountDownLatch;

public class LatchExample {
    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    System.out.println("worker done");
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("all workers done");
    }
}
```

Trap:

```text
CountDownLatch cannot be reset. Use CyclicBarrier or Phaser for reusable coordination.
```

---

## 21. CyclicBarrier

Use when N threads wait for each other at a common barrier.

```java
import java.util.concurrent.CyclicBarrier;

public class BarrierExample {
    public static void main(String[] args) {
        CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("phase complete"));

        Runnable task = () -> {
            try {
                System.out.println("ready");
                barrier.await();
                System.out.println("go");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        new Thread(task).start();
        new Thread(task).start();
        new Thread(task).start();
    }
}
```

Latch vs barrier:

| CountDownLatch | CyclicBarrier |
|---|---|
| One-time countdown | Reusable |
| One or more waiters | Participating threads wait |
| Good for startup completion | Good for phased work |

---

## 22. Semaphore

Use to limit concurrent access.

```java
import java.util.concurrent.Semaphore;

class ApiLimiter {
    private final Semaphore permits = new Semaphore(10);

    void call() throws InterruptedException {
        permits.acquire();
        try {
            callRemoteApi();
        } finally {
            permits.release();
        }
    }

    private void callRemoteApi() {
        // remote call
    }
}
```

Use cases:

- Limit DB-heavy work.
- Limit third-party API concurrency.
- Protect CPU-heavy or memory-heavy sections.

---

## 23. Phaser

Phaser is a flexible reusable barrier.

Use when:

- Number of parties can change.
- Multiple phases exist.
- More flexible than CyclicBarrier.

Example:

```java
import java.util.concurrent.Phaser;

public class PhaserExample {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(1);

        for (int i = 0; i < 3; i++) {
            phaser.register();
            new Thread(() -> {
                System.out.println("phase 1 work");
                phaser.arriveAndAwaitAdvance();
                System.out.println("phase 2 work");
                phaser.arriveAndDeregister();
            }).start();
        }

        phaser.arriveAndDeregister();
    }
}
```

Interview priority:

```text
Know CountDownLatch and Semaphore deeply. Know Phaser as an advanced flexible barrier.
```

---

## 24. ThreadLocal

ThreadLocal stores data per thread.

Common uses:

- Request context
- Correlation ID
- Tenant ID
- Security context
- Date formatter in old code

Example:

```java
class RequestContext {
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    static void set(String requestId) {
        REQUEST_ID.set(requestId);
    }

    static String get() {
        return REQUEST_ID.get();
    }

    static void clear() {
        REQUEST_ID.remove();
    }
}
```

Critical rule:

```text
Always remove ThreadLocal values in thread pools. Threads are reused, so old request data
can leak into future requests or prevent objects from being garbage collected.
```

---

## 25. Deadlock

Deadlock happens when threads wait forever for each other's locks.

Necessary conditions:

- Mutual exclusion
- Hold and wait
- No preemption
- Circular wait

Bad example:

```java
class DeadlockRisk {
    private final Object a = new Object();
    private final Object b = new Object();

    void first() {
        synchronized (a) {
            synchronized (b) {
                // work
            }
        }
    }

    void second() {
        synchronized (b) {
            synchronized (a) {
                // work
            }
        }
    }
}
```

Fix:

```text
Acquire locks in a consistent global order.
```

Production detection:

- Thread dump
- `jstack`
- JFR/JMC
- JVM deadlock detection output

---

## 26. Starvation And Livelock

Starvation:

```text
A thread never gets enough CPU/lock access to progress.
```

Livelock:

```text
Threads keep reacting to each other but no useful progress happens.
```

Examples:

- Too many high-priority tasks starving low-priority tasks.
- Retry loops that constantly back off in sync.
- Unfair locks under heavy contention.

Mitigation:

- Fair locks if needed.
- Backoff with jitter.
- Bounded queues.
- Rate limiting.
- Avoid long critical sections.

---

## 27. CompletableFuture And Executors

CompletableFuture is covered in the Java 8+ sheet, but this is the concurrency judgment.

Bad:

```java
CompletableFuture.supplyAsync(() -> callDatabase());
```

Why:

```text
Without an executor, it uses the common ForkJoinPool by default. Blocking DB calls can
starve unrelated async work.
```

Better:

```java
ExecutorService ioPool = Executors.newFixedThreadPool(20);

CompletableFuture<String> user = CompletableFuture.supplyAsync(
    () -> callUserService(),
    ioPool
);
```

Interview line:

```text
For blocking IO, I prefer a dedicated executor or virtual threads, and I still enforce
timeouts, bulkheads, and downstream limits.
```

---

## 28. Mini Program: Thread-Safe Rate Limiter

Simple fixed-window limiter:

```java
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

class FixedWindowRateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private volatile long windowStart;
    private final AtomicInteger count = new AtomicInteger();

    FixedWindowRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.windowStart = Instant.now().toEpochMilli();
    }

    boolean allow() {
        long now = Instant.now().toEpochMilli();

        if (now - windowStart >= windowMillis) {
            synchronized (this) {
                if (now - windowStart >= windowMillis) {
                    windowStart = now;
                    count.set(0);
                }
            }
        }

        return count.incrementAndGet() <= maxRequests;
    }
}
```

Discussion:

```text
This is simple but has burstiness at window boundaries. For smoother control, use sliding
window or token bucket. For distributed systems, store state in Redis or another shared store.
```

---

## 29. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| `volatile count++` | Visibility but not atomicity | Use AtomicInteger or lock |
| Unbounded executor queue | Latency and memory can explode | Bounded queue + rejection policy |
| Ignoring InterruptedException | Breaks cancellation | Restore interrupt using `Thread.currentThread().interrupt()` |
| Locking on String literal | Shared globally through pool | Use private final lock |
| Forgetting unlock | Permanent blocking | unlock in finally |
| ThreadLocal without remove | Request leaks in thread pool | clear in finally/filter |
| Parallel stream for blocking IO | Starves common pool | Dedicated executor or virtual threads |
| Too much shared mutable state | Hard to reason | Immutability, confinement, message passing |
| Assuming ConcurrentHashMap makes everything atomic | Compound logic can still race | Use `compute`, `merge`, locks, or transactions |

---

## 30. FAANG-Level Practical Question

Question:

> You are building a Java service that calls three downstream services for every request. How would you handle concurrency safely?

Strong answer:

```text
I would first identify whether the calls are independent and blocking. If they are
independent, I can run them concurrently using CompletableFuture with a dedicated executor
or virtual threads depending on the project's Java version. I would not use the common
ForkJoinPool for blocking IO. I would set timeouts, use bulkheads or semaphores to cap
downstream concurrency, and return partial or fallback responses where the product allows it.
Shared state should be immutable or guarded. Metrics should track latency, timeout count,
pool saturation, queue size, and downstream errors.
```

---

## 31. Rapid Revision

Must-say lines:

```text
Concurrency correctness is about visibility, atomicity, and ordering.
```

```text
volatile gives visibility and ordering, not mutual exclusion.
```

```text
synchronized gives mutual exclusion and happens-before visibility.
```

```text
Atomic classes use CAS and are great for simple independent variables.
```

```text
Thread pools need bounded queues, rejection policies, and sizing based on workload.
```

```text
ThreadLocal must be removed in thread pools.
```

```text
Deadlocks are usually diagnosed with thread dumps.
```

---

## 32. Official Source Notes

Use these for refreshing:

- Java concurrency API docs: `https://docs.oracle.com/en/java/javase/`
- Java Language Specification, memory model: `https://docs.oracle.com/javase/specs/`
- OpenJDK JDK project pages: `https://openjdk.org/projects/jdk/`

Interview safety line:

```text
I prefer high-level concurrency utilities where possible. I use low-level locking only when
I need exact control over shared state and invariants.
```
