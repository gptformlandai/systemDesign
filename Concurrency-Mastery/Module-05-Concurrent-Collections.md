# Module 5 — Concurrent Collections (Pick the Right One, Explain the Internals)

> **Goal:** Given any workload, name the exact `java.util.concurrent` collection you'd pick and *why*. Then explain `ConcurrentHashMap`'s bin-lock design and the `computeIfAbsent` deadlock trap that trips L5 candidates.

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────┐
                     │        MODULE 5 CORE               │
                     └────────────────────────────────────┘
                                    │
       ┌─────────────┬───────────────┬────────────────┬────────────┐
       │             │               │                │            │
    5.1 CHM      5.2 Queues      5.3 Deques &     5.4 CoW       5.5 SkipList
    (bin lock,   (ABQ vs LBQ,    Blocking Q's    (readers      Map/Set
    CAS on empty  SynchronousQ,   (LinkedBQ,     never lock,   (ordered,
    bucket,       DelayQ,         PriorityBQ)    writers copy) lock-free-ish,
    treeify @8)   CLQ)                                          O(log n))
```

---

## Decision Table — "Which Should I Use?"

| Workload | Pick | Why |
|---|---|---|
| Read-heavy key/value | `ConcurrentHashMap` | Bin-level locking; scales with cores |
| Ordered map / navigable | `ConcurrentSkipListMap` | Lock-free-ish, `floorKey/ceilingKey`, iterators are weakly consistent |
| Unbounded lock-free queue | `ConcurrentLinkedQueue` | Michael-Scott CAS-based |
| Bounded producer/consumer | `ArrayBlockingQueue` or `LinkedBlockingQueue` | Backpressure via `put()` blocking |
| Handoff (no storage) | `SynchronousQueue` | Direct producer→consumer; used by `newCachedThreadPool` |
| Schedule-after-delay | `DelayQueue` | Elements expose `getDelay()`; head is the soonest-expiring |
| Priority order | `PriorityBlockingQueue` | Unbounded; comparator-based |
| Rare writes, frequent iteration | `CopyOnWriteArrayList` / `CopyOnWriteArraySet` | Writers copy the whole array; iterators see a snapshot |
| Multi-producer multi-consumer, work-stealing | `LinkedTransferQueue` / `ForkJoinPool` internals | Better than LBQ under high contention |

---

## 5.1 `ConcurrentHashMap` (Java 8+) — Bin-Level Locking

### Real-world analogy

> Old-fashioned Java 7 `HashMap` had one door and one key — every thread queued to enter. Java 8 `ConcurrentHashMap` is like a **hotel**: each hash bucket (bin) is its own **room** with its own key. Two threads writing to *different* rooms never see each other. Only if they land on the same room do they take turns.

### The Java 7 → Java 8 shift (know this)

- **Java 7:** Fixed number of "Segments" (default 16). Each segment = its own `ReentrantLock`. Concurrency capped at 16.
- **Java 8+:** Segments are **gone**. Each bucket (bin) uses `synchronized` on its head node. Concurrency scales with **number of bins** (default 16, grows with resize). Empty-bucket inserts use CAS — no lock at all.

### Internal structure

```
     table[]  ─►  ┌───────────────┬───────────────┬───────────────┐
                  │  bin 0 (null) │  bin 1  ● ─►● │  bin 2  ● ─►● │  ...
                  └───────────────┴───────────────┴───────────────┘
                        │              │               │
                        │              ▼               ▼
                        │         Node chain     TreeBin (red-black tree
                        │         (list, when     when chain > TREEIFY_THRESHOLD=8
                        │          short)                 AND table >= 64)
                        │
                        ▼
                    CAS insert if empty (no lock)
```

### The write algorithm (memorize the four cases)

```java
put(key, value):
    hash = spread(key.hashCode())
    for (;;) {
        n = table.length
        i = hash & (n - 1)
        f = table[i]
        if (f == null) {
            // CASE 1: empty bin. CAS a new Node in; no lock.
            if (CAS(table, i, null, newNode)) break
        } else if (f == FORWARDING_NODE) {
            // CASE 2: resize in progress. Help transfer, then retry.
            table = helpTransfer(table, f)
        } else {
            // CASE 3 & 4: bin non-empty. Lock the head node.
            synchronized (f) {
                if (table[i] == f) {                          // re-check under lock
                    if (list) walk chain; insert/update
                    else if (treeBin) tree.putTreeVal(...)
                }
            }
            if (binCount >= TREEIFY_THRESHOLD) treeifyBin(table, i)
            break
        }
    }
    addCount(1)          // uses LongAdder-style striped cells
```

Four cases you should be ready to explain:
1. **Empty bin** — CAS on `table[i]`. Lock-free happy path.
2. **Resizing** — help move entries to the new table. Multi-threaded resize is the CHM secret sauce.
3. **List bin** — `synchronized(head)` and walk the chain.
4. **Tree bin** — same lock, but red-black tree for O(log n) collision handling.

### Treeify — the collision defense

- Chain reaches **8** entries **and** table size ≥ **64** → convert the bin to a red-black tree.
- Chain drops below **6** on a remove → convert back to a list.
- Reason: worst-case O(chain length) → O(log n) for lookup, so pathological hash collisions (or a hostile attacker) can't turn `get()` into a linear scan.

### Resize — multi-threaded transfer

When `size > threshold`:
1. A new table is allocated (2×).
2. The moving thread claims a range of buckets and re-hashes entries into the new table.
3. Other threads doing `put()`/`get()` may **join** the transfer via `helpTransfer` — they migrate a stride of buckets before resuming their own work.
4. Migrated buckets are marked with a `ForwardingNode` so `get()` from readers is redirected to the new table.

Effect: resize is spread across writer threads. No stop-the-world.

### The read side — always lock-free

- `get()` walks the bin without a lock.
- Nodes are (essentially) `volatile` — writes to `val` and `next` are visible to readers.
- Iterators are **weakly consistent**: they never `ConcurrentModificationException`. They may or may not reflect concurrent updates. Never rely on iteration for a consistent snapshot.

### The atomic compound ops (this is what senior devs use)

```java
map.putIfAbsent(k, v);
map.computeIfAbsent(k, key -> expensiveLoad(key));
map.compute(k, (key, old) -> old == null ? 1 : old + 1);
map.merge(k, 1, Integer::sum);        // idiomatic counter increment
```

All of these are **atomic on that bin**. `map.get(k) + 1 → map.put(k, sum)` is *not* atomic. `merge` is.

### 🚨 The `computeIfAbsent` deadlock trap (interview classic)

```java
ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
m.computeIfAbsent("a", k -> {
    return m.computeIfAbsent("b", k2 -> "value");   // BOOM
});
```

The bin lock for `"a"` is held while the lambda runs. If the lambda tries to acquire *another* bin lock — and especially the *same* bin — you can deadlock or, worse, produce a `StackOverflowError` / infinite loop (depending on hash and version). Even mapping `"a"` from within its own compute can throw `IllegalStateException` in modern JDKs.

**Rules to say aloud:**
- **Never** call back into the same map (any key) from inside `compute/computeIfAbsent/merge`.
- **Never** do blocking I/O in the lambda — you'll pin a bin.
- **Never** touch two maps under nested computes without a global lock order.
- **Do** keep the lambda pure and fast: compute value, return.

### Sizing tricks

- Constructor: `new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel)`. The `concurrencyLevel` is only a **hint** for the initial table size in Java 8+ (no more Segments).
- `size()` uses a striped `LongAdder`-style counter → cheap but **not** an exact snapshot. If you must have exact count, iterate under a global lock (rarely worth it).

---

## 5.2 Queues — Blocking, Lock-Free, and Handoff

### The three you must know cold

| Queue | Bounded? | Locks | Shape | When |
|---|---|---|---|---|
| `ArrayBlockingQueue` | ✅ (fixed capacity) | **One** lock, two Conditions | Circular array | Small bounded queues; predictable memory; fairness knob |
| `LinkedBlockingQueue` | Optional (default `Integer.MAX_VALUE`) | **Two** locks (`putLock`, `takeLock`) | Linked list | Higher throughput; producers and consumers rarely contend |
| `ConcurrentLinkedQueue` | ❌ unbounded | **None** (Michael-Scott CAS) | Linked list | Non-blocking, unbounded; no backpressure |

### `ArrayBlockingQueue` vs `LinkedBlockingQueue` — the "why two locks" answer

- **ABQ**: a single `ReentrantLock` guards the whole ring. Every `put` and `take` contends on the same lock. Simple; often preferred for small bounded queues.
- **LBQ**: separate `putLock` and `takeLock`. A producer inserting at the tail does not touch the head lock and vice versa. Under contention, LBQ can be **~2× faster**.

**But LBQ has a footgun**: default constructor is unbounded. `new LinkedBlockingQueue<>()` in a `newFixedThreadPool` = memory leak under sustained overload. **Always pass a capacity.**

### `SynchronousQueue` — the zero-storage queue

```
   Producer.put(x)  ─────────┐             ┌───── Consumer.take()
                             ▼             ▼
                       [ direct handoff ]
                         (no storage)
```

- No internal buffer. `put()` blocks until a matching `take()` shows up. Rendezvous semantics.
- Backs `Executors.newCachedThreadPool()`: submitted tasks are handed off directly to an idle thread; if none exist, a new thread is spun up.
- Also useful for **strict handoff** patterns where you want the producer to know its message was actually received.

### `DelayQueue`

Elements implement `Delayed`. `take()` returns only when the head element's delay has elapsed. Used for:
- Cache eviction scheduling.
- Job scheduling with delays (before you reach for Quartz).
- Retry timers with backoff.

Talking point: **`poll()` returns `null` when the head is not yet expired even if the queue is non-empty**. Trips juniors.

### `PriorityBlockingQueue`

Unbounded, elements ordered by `Comparator` or natural ordering. `take()` returns the smallest. Uses a binary heap under one lock.

Common use: rate-limited job runners where higher-priority tasks jump the queue.

Trap: **iterators do not traverse in priority order** — only `take()` does.

### `ConcurrentLinkedQueue` (Michael-Scott)

Lock-free, unbounded. Every `offer`/`poll` is a CAS on head/tail. Uses in production: metrics buffers, dropwizard's async histogram inputs, anywhere you need to enqueue without ever blocking.

Don't confuse with `LinkedBlockingQueue`:
- `CLQ` never blocks — `poll()` returns `null` if empty.
- `LBQ` has `take()` that waits.

If you need "block when empty" **and** high throughput, look at `LinkedTransferQueue`.

### `LinkedTransferQueue` (worth 30 seconds)

Combines `SynchronousQueue`-style rendezvous with normal queueing. Producers can choose:
- `put()` — buffer if no consumer.
- `transfer()` — block until a consumer picks it up.

Used inside `ForkJoinPool` and heavy concurrent messaging pipelines.

---

## 5.3 `Deque`s and Special-Purpose Queues

- **`ConcurrentLinkedDeque`** — lock-free, both ends. Rarely needed; know it exists.
- **`LinkedBlockingDeque`** — bounded, double-ended, blocking. Backs work-stealing patterns where a worker pushes to its own tail and thieves steal from its head.

---

## 5.4 `CopyOnWriteArrayList` / `CopyOnWriteArraySet`

### The idea

Every write allocates a **new copy** of the underlying array with the change applied. The reference to the array is `volatile`; readers grab the current reference and iterate over an immutable snapshot.

```
       state: volatile Object[] array
                     │
   ┌─────────────────┴──────────────────┐
   │                                    │
Readers: snapshot = array;             Writers: lock; new = copy(array);
         iterate — never lock.                  new[i] = v; array = new;
```

### Where it wins

- **Read:Write ratio > 100:1**: reading is a `volatile` pointer read; no lock, no synchronization overhead.
- **Iterator safety**: iterators are stable snapshots. No `ConcurrentModificationException`. This is why the Spring event listener list, Java's `AWT` listener list, and many observer patterns use `CopyOnWriteArrayList`.

### Where it loses

- **Any non-trivial write rate** — copying a 10,000-element array on every add is a memory-and-GC disaster.
- **`iterator.remove()` throws `UnsupportedOperationException`** — iterators are read-only.
- **`indexOf`/`contains` are O(n)** — no hashing.

### Interview one-liner

> "`CopyOnWriteArrayList` is the answer when reads dominate 100:1 and mutations happen at configuration time — event listener lists, subscribers, plugin registries. Never for hot-path append."

---

## 5.5 `ConcurrentSkipListMap` / `ConcurrentSkipListSet`

### Why skip lists

A **skip list** is a probabilistic balanced structure: a base linked list plus higher "express lanes" that skip over multiple elements. O(log n) average for get/put, and it's naturally lock-free-friendly because each level's pointers can be updated with CAS.

```
Level 3:  1 ──────────────► 21 ──────────────► ∞
Level 2:  1 ────► 8 ─────► 21 ──────► 34 ────► ∞
Level 1:  1 ► 4 ► 8 ► 13 ► 21 ► 27 ► 34 ► 42 ► ∞
```

### When to use

- You need an **ordered** concurrent map (like `TreeMap`, but thread-safe).
- Range queries: `subMap`, `headMap`, `tailMap`, `floorKey`, `ceilingKey`.
- Iteration matters and you can tolerate **weakly consistent** iterators.

### Not the default

If you don't need ordering, `ConcurrentHashMap` is faster and simpler.

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | Bad answer | L5 answer |
|---|---|---|
| "Why is Java 8 CHM faster than Java 7?" | "Different code." | "Bin-level `synchronized` + CAS on empty bin replaces fixed 16 Segments. Concurrency now scales with bins, not a constant." |
| "`get()` locks?" | "Yes." | "No — reads are lock-free, using volatile semantics on node fields. Iterators are weakly consistent." |
| "`computeIfAbsent` and I/O?" | "Fine." | "Never do blocking I/O inside; you pin the bin. Also, never recursively touch the same map — deadlock or infinite loop." |
| "`ABQ` vs `LBQ`?" | "One's array, one's linked." | "ABQ has one lock; LBQ has two (put/take) → higher throughput under contention. LBQ is unbounded by default — footgun." |
| "`CopyOnWriteArrayList` — always safe?" | "Yeah, it's concurrent." | "Only cheap when reads dominate 100:1. Every write copies the array. Not for hot-path append." |
| "How does `size()` on CHM work?" | "Returns exact size." | "Uses striped counters (`LongAdder`-style). Cheap; not a consistent snapshot under concurrent writes." |
| "Iterating a `PriorityBlockingQueue`?" | "In priority order." | "**No** — iteration order is unspecified; only `take()` respects priority." |

---

## 💻 L5-Grade Code Examples

### Example 1 — Idiomatic concurrent counter map (right way)

```java
ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();

public void hit(String key) {
    counters.computeIfAbsent(key, k -> new LongAdder()).increment();
}

public Map<String, Long> snapshot() {
    Map<String, Long> out = new HashMap<>();
    counters.forEach((k, v) -> out.put(k, v.sum()));
    return out;
}
```

Points:
- `computeIfAbsent` gives atomic "create-if-missing." No double-instantiation race.
- `LongAdder` per key avoids CHM's own contention becoming the bottleneck.
- Lambda is pure and cheap — no I/O, no re-entry.

### Example 2 — Wrong-way and right-way counter

```java
// WRONG — read/modify/write is not atomic
counters.put(key, counters.getOrDefault(key, 0L) + 1);

// WRONG — atomic on the bin but returns Long (boxing garbage under load)
counters.merge(key, 1L, Long::sum);

// RIGHT — `LongAdder` version above, no boxing.
```

### Example 3 — Bounded work queue with proper backpressure

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(1000);

ExecutorService pool = new ThreadPoolExecutor(
    4, 8, 60, TimeUnit.SECONDS,
    queue,
    new ThreadPoolExecutor.CallerRunsPolicy()   // producer runs when full → real backpressure
);
```

Why: `ArrayBlockingQueue` is bounded; `CallerRunsPolicy` propagates load back to the caller — the classic production-safe combo.

### Example 4 — Listener registry with `CopyOnWriteArrayList`

```java
public final class EventBus {
    private final CopyOnWriteArrayList<Consumer<Event>> listeners = new CopyOnWriteArrayList<>();

    public void register(Consumer<Event> listener)    { listeners.add(listener); }
    public void unregister(Consumer<Event> listener)  { listeners.remove(listener); }

    public void publish(Event e) {
        // Iterator sees a stable snapshot even if register/unregister happens concurrently.
        for (Consumer<Event> l : listeners) l.accept(e);
    }
}
```

Registration is rare; publishing is frequent → CoW is the correct tradeoff.

### Example 5 — Delay-based cache eviction

```java
static final class Expiring<K> implements Delayed {
    final K key;
    final long expiresAtNanos;
    Expiring(K k, long ttlNanos) {
        this.key = k;
        this.expiresAtNanos = System.nanoTime() + ttlNanos;
    }
    public long getDelay(TimeUnit unit) {
        return unit.convert(expiresAtNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
    }
    public int compareTo(Delayed o) {
        return Long.compare(this.expiresAtNanos, ((Expiring<?>) o).expiresAtNanos);
    }
}

DelayQueue<Expiring<String>> expiryQ = new DelayQueue<>();

// Evictor thread:
Thread.ofPlatform().daemon().start(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        try {
            Expiring<String> e = expiryQ.take();     // blocks until head expires
            cache.remove(e.key);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
    }
});
```

Classic use of `DelayQueue`. Zero polling; the queue wakes only when work is actually due.

---

## 🏭 Production War Stories

**1. The CHM lambda that stalled the app.**
A dev used `computeIfAbsent(k, key -> loadFromDatabase(key))`. The DB call took 2 seconds under load. Because CHM holds the bin's `synchronized` monitor across the lambda, every other write to keys hashing to the same bin *serialized behind this DB call*. Fix: load outside compute, then `putIfAbsent` the result.

**2. The unbounded LBQ that ate memory.**
Team wired `newFixedThreadPool(20)` — internally that's `LinkedBlockingQueue` with `Integer.MAX_VALUE` capacity. Downstream slowed by 3×. Queue grew to 40M tasks. GC pauses of 30s. Fix: switched to `ThreadPoolExecutor(20, 20, 0, SECONDS, new ArrayBlockingQueue<>(5000), new CallerRunsPolicy())`.

**3. The `CopyOnWriteArrayList` that OOMed.**
An RPC framework kept per-request listeners in a `CopyOnWriteArrayList`. Each request added and removed a listener — meaning every request allocated a **new copy** of a 20k-element array. Allocation rate spiked, GC ran constantly. Fix: switched hot-path to `ConcurrentHashMap<UUID, Listener>`.

**4. The `PriorityBlockingQueue` "iteration bug."**
Debug endpoint iterated the pending queue to show "next 5 jobs by priority." Users complained the ordering was wrong. Root cause: iterator order on PBQ is **heap order**, not sorted. Fix: `queue.stream().sorted().limit(5)` for the debug view.

**5. The DelayQueue that never fired.**
`getDelay` used `System.currentTimeMillis()`. When NTP adjusted the clock backward, the head element's delay became *negative-negative* and the compareTo produced nonsense ordering. Fix: switch to `System.nanoTime()` (monotonic).

---

## 🎯 Self-Check

1. Describe the four cases inside `ConcurrentHashMap.put()`. Which uses CAS? Which uses `synchronized`?
2. When does CHM treeify a bin? Two conditions.
3. Why is `computeIfAbsent(k, k -> ioCall(k))` dangerous?
4. `ArrayBlockingQueue` vs `LinkedBlockingQueue` — three concrete differences.
5. Default capacity of `new LinkedBlockingQueue<>()`? Why is that a footgun?
6. What does `SynchronousQueue.put()` do when no consumer is waiting?
7. Name three actual use cases for `DelayQueue`.
8. Break-even read:write ratio for `CopyOnWriteArrayList`. Why?
9. Iterators on `PriorityBlockingQueue` — what order?
10. How do CHM iterators differ from `HashMap` iterators?

---

## ➡️ Next

Move to **Module 6 — Thread Pools & Async** (`ThreadPoolExecutor` internals, sizing formulas, `ForkJoinPool`, `CompletableFuture` composition).
