# Module 4 — Machine Coding Pack (The L5 Make-Or-Break)

> **This module is 35% of your prep time for a reason.** Every MAANG L5/Staff loop has 1–2 machine coding rounds. Interviewers assume you can *talk* about concurrency — they want to see if you can *write* it cleanly under 45 minutes, no notes, no IDE autocomplete surprises.

Every solution below has a runnable Java file in [`Module-04-Machine-Coding/src/`](./Module-04-Machine-Coding/src/). Compile with Java 21+:

```bash
cd Concurrency-Mastery/Module-04-Machine-Coding
javac -d out src/*.java
java  -cp out BoundedBlockingQueueDemo
```

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────┐
                     │      MODULE 4 — CODE TIER          │
                     └────────────────────────────────────┘
                                    │
    ┌───────────────────────────────┼───────────────────────────────┐
    │                               │                               │
Tier A — Must nail cold      Tier B — Should sketch         Universal patterns
  1. BBQ (2 ways)              7. Custom CountDownLatch      • Lock + Condition
  2. Producer-Consumer         8. Custom Semaphore           • wait/notify guarded
  3. Thread Pool                9. Concurrent LFU              by while()
  4. Concurrent LRU           10. SPSC Ring Buffer           • Try/finally unlock
  5. Token Bucket                                             • Immutable snapshot
  6. LeetCode set                                             • Never I/O in
     (Print in Order,                                          critical section
     FizzBuzz MT, H2O,
     Dining Philosophers,
     Traffic Light)
```

---

## 🎯 The Interview Playbook (do these steps every time)

**Minute 0–3 — Clarify.** Ask:
- Multi-producer / multi-consumer?
- Bounded or unbounded?
- Fair or unfair ordering?
- Blocking or non-blocking (`take` vs `poll`)?
- What's the throughput target — hint about contention profile?

**Minute 3–7 — Design out loud.**
- Which primitive? (`ReentrantLock + Condition` vs `synchronized + wait/notify` vs lock-free).
- What's the *shared state*? Draw it.
- What are the invariants? State them: "size ≥ 0", "count == putIdx - takeIdx", etc.

**Minute 7–35 — Code.** Follow the universal skeleton (below). Talk while typing.

**Minute 35–45 — Walk through races.**
- "What if two producers race?"
- "What if consumer is interrupted mid-`await`?"
- "What if the queue is full and the producer dies?"

**Do not:**
- Use `Thread.sleep()` for coordination.
- Guard `wait()` with `if`.
- Forget `unlock()` in `finally`.
- Call `notifyAll()` when you can prove `signal()` is enough (but *do* default to `notifyAll` if unsure).
- Do I/O or heavy compute inside the critical section.

---

## 🧰 The Universal Skeleton (memorize this)

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition cond      = lock.newCondition();
// ... shared mutable state guarded by `lock` ...

public T operation(...) throws InterruptedException {
    lock.lock();
    try {
        while (!canProceed()) {         // WHILE — spurious wake safe
            cond.await();
        }
        // mutate state
        cond.signal();                  // wake the *specific* waiter (or signalAll if unsure)
        return result;
    } finally {
        lock.unlock();                  // ALWAYS in finally
    }
}
```

Every problem in Tier A is a variation of this shape.

---

# PROBLEM 1 — Bounded Blocking Queue

> Implement a fixed-capacity queue with `put(item)` (blocks when full) and `take()` (blocks when empty). Must be safe for **N producers × M consumers**.

## Version A — `ReentrantLock` + two `Condition`s (the L5 answer)

```java
public final class BoundedBlockingQueue<T> {
    private final Object[] items;
    private final ReentrantLock lock     = new ReentrantLock();
    private final Condition    notFull   = lock.newCondition();
    private final Condition    notEmpty  = lock.newCondition();
    private int putIdx, takeIdx, count;

    public BoundedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.items = new Object[capacity];
    }

    public void put(T item) throws InterruptedException {
        Objects.requireNonNull(item);
        lock.lockInterruptibly();
        try {
            while (count == items.length) notFull.await();
            items[putIdx] = item;
            if (++putIdx == items.length) putIdx = 0;
            count++;
            notEmpty.signal();                  // one waiting consumer
        } finally { lock.unlock(); }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == 0) notEmpty.await();
            T item = (T) items[takeIdx];
            items[takeIdx] = null;
            if (++takeIdx == items.length) takeIdx = 0;
            count--;
            notFull.signal();                   // one waiting producer
            return item;
        } finally { lock.unlock(); }
    }

    public int size() { lock.lock(); try { return count; } finally { lock.unlock(); } }
}
```

### Why this is L5-clean
- **Two `Condition`s** → `signal()` wakes exactly the right kind of thread. No wasted wakeups. No lost-signal bug.
- **Circular indices** → no array copies on wraparound.
- **`lockInterruptibly()`** → shutdown-safe.
- **`Objects.requireNonNull`** → keeps the invariant that `null` means "empty slot."
- No `size()` outside the lock — `count` is not `volatile` and reading it racily can return stale values.

## Version B — `synchronized` + `wait/notifyAll` (fall-back if not allowed `j.u.c`)

```java
public final class BoundedBlockingQueueMonitor<T> {
    private final Object[] items;
    private int putIdx, takeIdx, count;

    public BoundedBlockingQueueMonitor(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.items = new Object[capacity];
    }

    public synchronized void put(T item) throws InterruptedException {
        Objects.requireNonNull(item);
        while (count == items.length) wait();
        items[putIdx] = item;
        if (++putIdx == items.length) putIdx = 0;
        count++;
        notifyAll();                            // one monitor, both roles wait here
    }

    @SuppressWarnings("unchecked")
    public synchronized T take() throws InterruptedException {
        while (count == 0) wait();
        T item = (T) items[takeIdx];
        items[takeIdx] = null;
        if (++takeIdx == items.length) takeIdx = 0;
        count--;
        notifyAll();
        return item;
    }
}
```

### Say this out loud in the interview
> "With a single monitor, `notify()` can wake the wrong role — a full producer might wake another producer. So we `notifyAll()`. That works but wakes both roles unnecessarily. The AQS version with two `Condition`s is strictly better under real load."

---

# PROBLEM 2 — Producer / Consumer (N × M)

Not a separate data structure — it's the **usage pattern** on top of the BBQ. Interviewers ask it as a wire-up problem.

```java
BoundedBlockingQueue<Task> q = new BoundedBlockingQueue<>(1000);

// N producers
for (int i = 0; i < N; i++) {
    Thread.startVirtualThread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try { q.put(produce()); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    });
}

// M consumers
for (int i = 0; i < M; i++) {
    Thread.startVirtualThread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try { consume(q.take()); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    });
}
```

### Interview points
- **Backpressure is free** because `put()` blocks on full → producers naturally slow down.
- **Graceful shutdown** = poison-pill pattern: producers put a sentinel `Task`; consumers exit when they receive it. Or send `interrupt()`; the `lockInterruptibly()` above cooperates.
- **Never** use `while(true)` without an interrupt check — otherwise you can't stop the thread.

---

# PROBLEM 3 — Thread Pool From Scratch

> Build a fixed-size thread pool that accepts `Runnable`s, has a bounded queue, and supports `shutdown()` (drain) and `shutdownNow()` (abort).

```java
public final class FixedThreadPool implements Executor {
    private final BoundedBlockingQueue<Runnable> queue;
    private final Thread[] workers;
    private volatile boolean running = true;

    private static final Runnable POISON = () -> {};

    public FixedThreadPool(int nThreads, int queueCapacity) {
        this.queue   = new BoundedBlockingQueue<>(queueCapacity);
        this.workers = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            workers[i] = new Thread(this::workerLoop, "pool-worker-" + i);
            workers[i].start();
        }
    }

    @Override
    public void execute(Runnable task) {
        Objects.requireNonNull(task);
        if (!running) throw new RejectedExecutionException("shut down");
        try { queue.put(task); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(e);
        }
    }

    private void workerLoop() {
        while (true) {
            try {
                Runnable task = queue.take();
                if (task == POISON) return;
                try { task.run(); }
                catch (Throwable t) {
                    // Never let a task kill the worker.
                    System.err.println("task failed: " + t);
                }
            } catch (InterruptedException e) {
                if (!running) return;         // shutdownNow path
            }
        }
    }

    public void shutdown() throws InterruptedException {
        running = false;
        for (int i = 0; i < workers.length; i++) queue.put(POISON);
        for (Thread w : workers) w.join();
    }

    public void shutdownNow() {
        running = false;
        for (Thread w : workers) w.interrupt();
    }
}
```

### Say this out loud
- **`POISON` sentinel** = the canonical clean shutdown. One poison per worker.
- **Catch `Throwable` inside the worker** — a NPE in one task must not kill the worker forever.
- **`shutdownNow` uses `interrupt()`** — the `queue.take()` throws `InterruptedException` and the worker checks `running`.
- **`volatile running`** for cross-thread visibility.
- **Rejection policy** is minimal here — real `ThreadPoolExecutor` has AbortPolicy/CallerRuns/etc. (See Module 6.)

### The interview twist
> "How would you extend this to `newFixedThreadPool` behavior with an unbounded queue?" — Answer: change the queue to `LinkedBlockingQueue` with no capacity. **But warn**: unbounded queue = memory leak under sustained overload = the classic `newFixedThreadPool` OOM.

---

# PROBLEM 4 — Concurrent LRU Cache

> Design a thread-safe `Cache<K, V>` with `get` / `put`, O(1) both, capacity `N`, evicting least-recently-used.

## The mental model

Data structure = `HashMap<K, Node>` + doubly-linked list. `get` moves node to front; `put` inserts at front, evicts tail if over cap.

## Version A — single lock (correct, clean, always accepted)

```java
public final class LRUCache<K, V> {
    private static final class Node<K, V> {
        final K key;
        V value;
        Node<K, V> prev, next;
        Node(K k, V v) { this.key = k; this.value = v; }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> index = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null);   // sentinel
    private final Node<K, V> tail = new Node<>(null, null);   // sentinel
    private final ReentrantLock lock = new ReentrantLock();

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        lock.lock();
        try {
            Node<K, V> n = index.get(key);
            if (n == null) return null;
            moveToFront(n);
            return n.value;
        } finally { lock.unlock(); }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            Node<K, V> n = index.get(key);
            if (n != null) { n.value = value; moveToFront(n); return; }
            n = new Node<>(key, value);
            index.put(key, n);
            addToFront(n);
            if (index.size() > capacity) {
                Node<K, V> lru = tail.prev;
                remove(lru);
                index.remove(lru.key);
            }
        } finally { lock.unlock(); }
    }

    private void addToFront(Node<K, V> n) {
        n.prev = head; n.next = head.next;
        head.next.prev = n; head.next = n;
    }
    private void remove(Node<K, V> n) {
        n.prev.next = n.next; n.next.prev = n.prev;
    }
    private void moveToFront(Node<K, V> n) { remove(n); addToFront(n); }
}
```

### L5 talking points
- **Sentinel head/tail** → no null checks in the linked-list ops.
- **All shared state under one lock** → simple, correct, easy to reason about.
- **`get` mutates the list** (moves to front). That's why it takes the write lock, not a read lock.

## Version B — striped locking (say this if asked "how would you scale?")

Split the cache into `N` shards, each with its own `LRUCache` + lock. Hash the key to a shard. Contention drops N×. **This is what production caches (Caffeine, Guava) do internally.**

```java
public final class StripedLRU<K, V> {
    private final LRUCache<K, V>[] shards;
    private final int mask;

    @SuppressWarnings("unchecked")
    public StripedLRU(int totalCapacity, int shardCount) {
        int n = Integer.highestOneBit(shardCount - 1) << 1;    // next power of 2
        this.shards = new LRUCache[n];
        for (int i = 0; i < n; i++) shards[i] = new LRUCache<>(totalCapacity / n);
        this.mask = n - 1;
    }
    private LRUCache<K, V> shard(K k) { return shards[spread(k.hashCode()) & mask]; }
    private static int spread(int h) { return (h ^ (h >>> 16)); }

    public V get(K k)               { return shard(k).get(k); }
    public void put(K k, V v)       { shard(k).put(k, v); }
}
```

### The interview follow-up
> "Why not `Collections.synchronizedMap(new LinkedHashMap(cap, 0.75f, true))`?" — Answer: it works, but every operation grabs one global lock and iteration/eviction is coupled to the same monitor. Striping scales; `Caffeine` gives you async batched eviction (better still).

---

# PROBLEM 5 — Token Bucket Rate Limiter

> Allow up to `capacity` operations, refilling at `refillRate` tokens/second. `tryAcquire()` returns whether the caller is admitted.

## Lock-free version (the L5 answer)

```java
public final class TokenBucketRateLimiter {
    private final long capacity;
    private final double refillPerNano;              // tokens per nanosecond
    private final AtomicLong stateRef;               // packed: hi=lastRefillNanos, lo=tokens (scaled)

    // We store both fields in one AtomicLong-backed record to CAS them together.
    private static final class State {
        final long lastNanos;
        final long tokensScaled;                     // tokens * SCALE for precision
        State(long t, long s) { this.lastNanos = t; this.tokensScaled = s; }
    }
    private static final long SCALE = 1_000_000L;
    private final AtomicReference<State> state;

    public TokenBucketRateLimiter(long capacity, double refillPerSecond) {
        if (capacity <= 0 || refillPerSecond <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1_000_000_000d;
        this.stateRef = null;                        // unused; kept for clarity
        this.state = new AtomicReference<>(new State(System.nanoTime(), capacity * SCALE));
    }

    public boolean tryAcquire() { return tryAcquire(1); }

    public boolean tryAcquire(long permits) {
        long cost = permits * SCALE;
        while (true) {
            State cur = state.get();
            long now = System.nanoTime();
            long elapsed = Math.max(0, now - cur.lastNanos);
            long refilled = (long) (elapsed * refillPerNano * SCALE);
            long available = Math.min(capacity * SCALE, cur.tokensScaled + refilled);
            if (available < cost) return false;
            State next = new State(now, available - cost);
            if (state.compareAndSet(cur, next)) return true;
            // else another thread advanced state; retry
        }
    }
}
```

### Why this beats "just synchronized"
- No lock, no queueing, no blocking. `tryAcquire` is a bounded CAS loop → deterministic latency.
- Immutable `State` record makes the CAS trivial and correct.
- `SCALE` avoids floating-point drift over long uptime.

### The interview follow-ups you should be ready for
| Q | A |
|---|---|
| "What if I want the caller to *wait*?" | Compute the deficit, park with `LockSupport.parkNanos`, retry after wake. Or use a `ScheduledExecutor` to signal a `Condition`. |
| "Distributed rate limiter?" | Redis + Lua script (atomic INCR/EXPIRE) or a token dispensing service (see Module 8). |
| "Leaky bucket vs token bucket?" | Leaky = **smooth** output rate (queue drains at fixed speed). Token = **burst-tolerant** (bucket accumulates during idle). Pick token for user-facing APIs, leaky for downstream protection. |

---

# PROBLEM 6 — The LeetCode Concurrency Set

These are classic Amazon/Meta phone-screen warm-ups. Master all 5. Runnable versions live in [`src/`](./Module-04-Machine-Coding/src/).

## 6.1 Print in Order (LC 1114)

> Three methods `first()`, `second()`, `third()` are called by three threads in arbitrary order. Must print `first` → `second` → `third`.

```java
public final class Foo {
    private final Semaphore s2 = new Semaphore(0);
    private final Semaphore s3 = new Semaphore(0);

    public void first(Runnable print)  { print.run(); s2.release(); }
    public void second(Runnable print) throws InterruptedException {
        s2.acquire(); print.run(); s3.release();
    }
    public void third(Runnable print) throws InterruptedException {
        s3.acquire(); print.run();
    }
}
```

Why semaphores? Each is a **one-shot signal**. Simplest primitive for "wait for permission."

## 6.2 Fizz Buzz Multithreaded (LC 1195)

> Four threads: fizz, buzz, fizzbuzz, number. Each prints for `i = 1..n`.

```java
public final class FizzBuzz {
    private final int n;
    private int i = 1;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition turn = lock.newCondition();

    public FizzBuzz(int n) { this.n = n; }

    public void fizz(Runnable printFizz) throws InterruptedException {
        run(() -> i % 3 == 0 && i % 5 != 0, printFizz);
    }
    public void buzz(Runnable printBuzz) throws InterruptedException {
        run(() -> i % 5 == 0 && i % 3 != 0, printBuzz);
    }
    public void fizzbuzz(Runnable printFB) throws InterruptedException {
        run(() -> i % 15 == 0, printFB);
    }
    public void number(IntConsumer printNumber) throws InterruptedException {
        lock.lock();
        try {
            while (i <= n) {
                if (i % 3 != 0 && i % 5 != 0) { printNumber.accept(i); i++; turn.signalAll(); }
                else turn.await();
            }
            turn.signalAll();                 // wake everyone so they can exit
        } finally { lock.unlock(); }
    }

    private void run(BooleanSupplier isMine, Runnable action) throws InterruptedException {
        lock.lock();
        try {
            while (i <= n) {
                if (isMine.getAsBoolean()) { action.run(); i++; turn.signalAll(); }
                else turn.await();
            }
        } finally { lock.unlock(); }
    }
}
```

Key idea: **one lock, one condition, one shared counter `i`**. Everyone checks their predicate and advances or waits.

## 6.3 Building H2O (LC 1117)

> Two hydrogen threads + one oxygen thread must call `releaseHydrogen`/`releaseOxygen` in groups of 2H+1O per molecule.

```java
public final class H2O {
    private final Semaphore h = new Semaphore(2);
    private final Semaphore o = new Semaphore(0);
    private final CyclicBarrier barrier = new CyclicBarrier(3, () -> {
        // molecule complete — release next batch of hydrogen permits
        h.release(2);
    });

    public void hydrogen(Runnable releaseH) throws InterruptedException {
        h.acquire();
        releaseH.run();
        try { barrier.await(); } catch (BrokenBarrierException e) { throw new InterruptedException(); }
    }

    public void oxygen(Runnable releaseO) throws InterruptedException {
        releaseO.run();
        try { barrier.await(); } catch (BrokenBarrierException e) { throw new InterruptedException(); }
    }
}
```

Two primitives combined:
- **Semaphore** gates hydrogen (2 at a time).
- **CyclicBarrier(3)** synchronizes the two H's and one O — the barrier action refills hydrogen permits.

## 6.4 Dining Philosophers (LC 1226)

> 5 philosophers sit at a round table, each needs the fork on their left and right. Prevent deadlock.

**Standard fix: enforce a global lock order** — always pick up the **lower-numbered** fork first.

```java
public final class DiningPhilosophers {
    private final ReentrantLock[] forks = new ReentrantLock[5];

    public DiningPhilosophers() {
        for (int i = 0; i < 5; i++) forks[i] = new ReentrantLock();
    }

    public void wantsToEat(int philosopher,
                           Runnable pickLeft, Runnable pickRight,
                           Runnable eat,
                           Runnable putLeft, Runnable putRight) throws InterruptedException {
        int left  = philosopher;
        int right = (philosopher + 1) % 5;
        int first = Math.min(left, right);
        int second = Math.max(left, right);

        forks[first].lockInterruptibly();
        try {
            forks[second].lockInterruptibly();
            try {
                pickLeft.run(); pickRight.run(); eat.run(); putLeft.run(); putRight.run();
            } finally { forks[second].unlock(); }
        } finally { forks[first].unlock(); }
    }
}
```

Alternative fixes to mention:
- **Semaphore-limited seating** (only 4 philosophers may try at once → at least one always progresses).
- **Chandy/Misra**: forks carry a "dirty/clean" state and passing rules. Advanced; mention if asked.

## 6.5 Traffic Light Controlled Intersection (LC 1279)

> Two roads cross. Cars ask to cross; only one road may cross at a time; light "flips" when a car from the other road arrives.

```java
public final class TrafficLight {
    private final ReentrantLock lock = new ReentrantLock();
    private int greenRoad = 1;

    public void carArrived(int carId, int roadId, int direction,
                           Runnable turnGreen, Runnable crossCar) {
        lock.lock();
        try {
            if (roadId != greenRoad) {
                turnGreen.run();
                greenRoad = roadId;
            }
            crossCar.run();
        } finally { lock.unlock(); }
    }
}
```

Deceptively simple. The interviewer is testing whether you can spot that **the light state must be held under the lock across both the check and the switch**. Two-phase read/write from outside the lock → race.

---

# Tier B — Sketch-Level (know the shape)

## 7. Custom `CountDownLatch` — see Module 2 Example 1 (`wait/notify` version).

## 8. Custom `Semaphore` — one `ReentrantLock` + one `Condition`, `permits` counter, `acquire` waits while `permits == 0`, `release` increments and signals.

## 9. Concurrent LFU — same as LRU but replace "recency" order with a **frequency bucket + doubly-linked list of same-freq nodes**. Increment on `get`. Evict from the lowest-freq bucket, LRU within that bucket. Say aloud: *"O(1) with two levels of linked lists"* — that's what interviewers want to hear.

## 10. SPSC Ring Buffer (Disruptor-lite intuition)
- Fixed-size power-of-2 array.
- Two counters: `producerSeq`, `consumerSeq` — each on its own cache line (`@Contended`).
- Producer: writes at `producerSeq & mask` if `producerSeq - consumerSeq < capacity`.
- Consumer: reads at `consumerSeq & mask` if `consumerSeq < producerSeq`.
- No locks. Only volatile sequence updates. Beats every lock-based queue at high throughput.

---

## 🏭 Production War Stories

**1. The BBQ that leaked memory.** A team used `new LinkedBlockingQueue<>()` (unbounded) as their thread-pool queue. Under a downstream slowdown, the queue grew until OOM. Fix: bounded queue + `CallerRunsPolicy` (backpressure — the producer thread executes the task itself when the pool is full).

**2. The LRU that had a data race.** A junior wrote `LRUCache` around `ConcurrentHashMap` but did `get + move-to-front` **without a lock**. Two threads racing on the same key corrupted the linked list. `NullPointerException` in production. Fix: single lock (Version A above). Only fine-grained approach that works: full striping.

**3. The rate limiter that drifted.** A homegrown limiter used `double tokens` and `System.currentTimeMillis`. Over 30 days of uptime, floating-point accumulation drift + clock adjustments made the limit off by 8%. Fix: `long` fixed-point (the `SCALE` trick above) + `System.nanoTime` (monotonic).

**4. The thread pool that stopped.** A team's custom pool caught only `Exception`, not `Throwable`, in the worker loop. An `OutOfMemoryError` on one task killed the worker silently. Pool degraded to 0 workers. Traffic queued forever. Fix: catch `Throwable`, log, continue.

---

## 🎯 Self-Check

1. Code a BBQ with `ReentrantLock + Condition` in 5 minutes from memory. No lookups.
2. Explain **why two Conditions**, not `notifyAll`.
3. Write the worker loop of a thread pool. Include exception + shutdown handling.
4. Draw the LRU node structure. Explain why `get` writes.
5. Write the token-bucket CAS loop. Explain drift and the `SCALE` trick.
6. Trace two producers + two consumers against your BBQ. Where do they contend?
7. What's the deadlock condition in Dining Philosophers and the 3 fixes?
8. How would you shard LRUCache to reduce contention? What tradeoff?
9. `poll(timeout)` — how would you add it to your BBQ? (Hint: `await(nanos)` returns remaining nanos.)
10. Say aloud: "here's what happens if a producer is interrupted mid-`put`."

---

## ➡️ Next

Move to **Module 5 — Concurrent Collections** (`ConcurrentHashMap` internals, blocking queues, `SkipListMap`, `CopyOnWriteArrayList`).
