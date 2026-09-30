# Module 5: Concurrent Collections — The Fun, Deep-Dive Explanation

> This document explains how to pick the right concurrent collection for any workload, the internals of `ConcurrentHashMap`, and the infamous `computeIfAbsent` deadlock trap — like you're a junior dev who's never seen this before.

---

## 📑 Table of Contents

- [Part 1: The Big Picture — Which Collection Do I Use?](#part-1-the-big-picture--which-collection-do-i-use)
  - [1.1 The Decision Table](#11-the-decision-table)
  - [1.2 The Mental Model](#12-the-mental-model)
- [Part 2: ConcurrentHashMap — The Star of the Show](#part-2-concurrenthashmap--the-star-of-the-show)
  - [2.1 The Hotel Analogy](#21-the-hotel-analogy)
  - [2.2 Java 8+ Internals](#22-java-8-internals)
  - [2.3 Treeification](#23-treeification)
  - [2.4 Multi-Threaded Resize](#24-multi-threaded-resize)
  - [2.5 Atomic Compound Operations](#25-atomic-compound-operations)
  - [2.6 The computeIfAbsent Deadlock Trap](#26-the-computeifabsent-deadlock-trap)
  - [2.7 size() Is Not Exact](#27-size-is-not-exact)
- [Part 3: Blocking Queues](#part-3-blocking-queues)
  - [3.1 ArrayBlockingQueue](#31-arrayblockingqueue)
  - [3.2 LinkedBlockingQueue](#32-linkedblockingqueue)
  - [3.3 SynchronousQueue](#33-synchronousqueue)
  - [3.4 DelayQueue](#34-delayqueue)
  - [3.5 PriorityBlockingQueue](#35-priorityblockingqueue)
- [Part 4: Lock-Free Collections](#part-4-lock-free-collections)
  - [4.1 ConcurrentLinkedQueue](#41-concurrentlinkedqueue)
  - [4.2 ConcurrentSkipListMap](#42-concurrentskiplistmap)
- [Part 5: Copy-On-Write Collections](#part-5-copy-on-write-collections)
  - [5.1 CopyOnWriteArrayList](#51-copyonwritearraylist)
- [Part 6: Production War Stories](#part-6-production-war-stories)
- [Part 7: Interview Traps & Self-Check](#part-7-interview-traps--self-check)

---

# Part 1: The Big Picture — Which Collection Do I Use?

## 1.1 The Decision Table

Before we dive into internals, here's your cheat sheet:

| Workload | Pick This | Why |
|----------|-----------|-----|
| Read-heavy key/value | `ConcurrentHashMap` | Bin-level locking, scales with cores |
| Ordered map / navigable | `ConcurrentSkipListMap` | Lock-free-ish, `floorKey/ceilingKey` |
| Unbounded lock-free queue | `ConcurrentLinkedQueue` | Michael-Scott CAS-based |
| Bounded producer/consumer | `ArrayBlockingQueue` or `LinkedBlockingQueue` | Backpressure via blocking |
| Direct handoff (no storage) | `SynchronousQueue` | Producer blocks until consumer takes |
| Schedule after delay | `DelayQueue` | Elements have expiration times |
| Priority ordering | `PriorityBlockingQueue` | Comparator-based ordering |
| Rare writes, frequent reads | `CopyOnWriteArrayList` | Writers copy array, readers never lock |

## 1.2 The Mental Model

Think of concurrent collections as **specialized tools**:

```
+------------------------------------------------------------------+
|                    CONCURRENT COLLECTIONS                         |
+------------------------------------------------------------------+
|                                                                   |
|  MAPS (key-value)                                                |
|  +-----------------------------------------------------------+   |
|  | ConcurrentHashMap     | Fast, unordered, bin-level locks  |   |
|  | ConcurrentSkipListMap | Ordered, range queries, lock-free |   |
|  +-----------------------------------------------------------+   |
|                                                                   |
|  QUEUES (producer-consumer)                                      |
|  +-----------------------------------------------------------+   |
|  | ArrayBlockingQueue    | Bounded, one lock, simple         |   |
|  | LinkedBlockingQueue   | Bounded/unbounded, two locks      |   |
|  | ConcurrentLinkedQueue | Unbounded, lock-free, non-blocking|   |
|  | SynchronousQueue      | Zero storage, direct handoff      |   |
|  | DelayQueue            | Time-based release                |   |
|  | PriorityBlockingQueue | Priority-based ordering           |   |
|  +-----------------------------------------------------------+   |
|                                                                   |
|  LISTS (rare writes, many reads)                                 |
|  +-----------------------------------------------------------+   |
|  | CopyOnWriteArrayList  | Snapshot iteration, write = copy  |   |
|  | CopyOnWriteArraySet   | Same, but Set semantics           |   |
|  +-----------------------------------------------------------+   |
|                                                                   |
+------------------------------------------------------------------+
```

---

# Part 2: `ConcurrentHashMap` — The Star of the Show

## 2.1 The Hotel Analogy

Let's understand `ConcurrentHashMap` with a hotel analogy:

### Old Way: Java 7 `ConcurrentHashMap` (Segments)

```
Imagine a hotel with only 16 rooms, and 16 master keys.

+------------------+
|  HOTEL (16 keys) |
+------------------+
| Room 0  [Key 0]  |  Thread A wants Room 0
| Room 1  [Key 1]  |  Thread B wants Room 1
| Room 2  [Key 2]  |  Thread C wants Room 0  <-- Must wait for Thread A!
| ...              |
| Room 15 [Key 15] |
+------------------+

Problem: Only 16 keys! If 100 threads want different rooms,
they still have to share those 16 keys.
Max concurrency = 16 (fixed!)
```

### New Way: Java 8+ `ConcurrentHashMap` (Bin-Level Locking)

```
Imagine a hotel where EACH ROOM has its own lock.

+----------------------------------+
|  HOTEL (each room has own lock)  |
+----------------------------------+
| Room 0  [Own lock]  |  Thread A
| Room 1  [Own lock]  |  Thread B
| Room 2  [Own lock]  |  Thread C
| Room 3  [Own lock]  |  Thread D
| ...                 |
| Room 999 [Own lock] |  Thread E
+----------------------------------+

Better: Each room is independent!
Max concurrency = number of rooms (grows with resize!)

Even better: If a room is EMPTY, you don't even need a key!
Just walk in (CAS insert).
```

---

## 2.2 The Internal Structure

```
ConcurrentHashMap internal structure:

table[] (array of bins)
+-------+-------+-------+-------+-------+-------+-------+-------+
| bin 0 | bin 1 | bin 2 | bin 3 | bin 4 | bin 5 | bin 6 | bin 7 | ...
+-------+-------+-------+-------+-------+-------+-------+-------+
    |       |       |       |       |       |       |       |
    v       v       v       v       v       v       v       v
  null    Node    null    Node    null   TreeBin  null    Node
           |               |               |               |
           v               v               v               v
          Node           null         (red-black        Node
           |                            tree)             |
           v                                              v
          null                                          null


Three types of bins:
1. null         - Empty bin (CAS insert, no lock!)
2. Node chain   - Linked list (short chains)
3. TreeBin      - Red-black tree (when chain > 8 AND table >= 64)
```

---

## 2.3 The Four Cases of `put()` — Memorize This!

When you call `map.put(key, value)`, one of four things happens:

### Case 1: Empty Bin — CAS (No Lock!)

```
Bin is empty (null):

Before:
table[i] = null

Thread A: "I'll just CAS a new node in!"
CAS(table[i], null, newNode)

After:
table[i] -> [Node: key=K, value=V]

No lock needed! This is the fast path.
```

### Case 2: Resize in Progress — Help Transfer

```
Bin contains a ForwardingNode (resize happening):

table[i] = ForwardingNode (points to new table)

Thread A: "Oh, resize is happening. Let me help!"
1. Help move some buckets to new table
2. Then retry my put() on the new table

This is how CHM does multi-threaded resize!
```

### Case 3: List Bin — Synchronized on Head

```
Bin has a linked list:

table[i] -> [Node A] -> [Node B] -> [Node C] -> null

Thread A: synchronized(table[i]) {
    // Walk the chain
    // Insert or update
}

Lock is on the HEAD NODE, not a global lock!
Other bins are unaffected.
```

### Case 4: Tree Bin — Synchronized on TreeBin

```
Bin has a red-black tree (chain was too long):

table[i] -> [TreeBin]
                |
            (red-black tree structure)

Thread A: synchronized(treeBin) {
    // Tree insert/update
    // O(log n) instead of O(n)
}

Same idea: lock only this bin.
```

### The Code Shape

```java
put(key, value):
    hash = spread(key.hashCode())
    
    for (;;) {  // Retry loop
        i = hash & (table.length - 1)  // Which bin?
        f = table[i]                    // What's in the bin?
        
        if (f == null) {
            // CASE 1: Empty bin - CAS insert (no lock!)
            if (CAS(table, i, null, newNode)) 
                break;  // Success!
            // CAS failed, someone else inserted. Loop and retry.
            
        } else if (f == FORWARDING_NODE) {
            // CASE 2: Resize in progress - help transfer
            table = helpTransfer(table, f)
            // Then loop and retry on new table
            
        } else {
            // CASE 3 or 4: Non-empty bin - lock the head
            synchronized (f) {
                if (table[i] == f) {  // Re-check under lock!
                    if (f is Node) {
                        // CASE 3: Walk linked list, insert/update
                    } else if (f is TreeBin) {
                        // CASE 4: Tree insert/update
                    }
                }
            }
            
            // After insert, check if we need to treeify
            if (chainLength >= 8) 
                treeifyBin(table, i)
            break;
        }
    }
    
    addCount(1)  // Update size (uses LongAdder-style striping!)
```

---

## 2.4 Treeify — The Collision Defense

When a bin's chain gets too long, CHM converts it to a red-black tree:

```
BEFORE (linked list, O(n) lookup):
table[i] -> [A] -> [B] -> [C] -> [D] -> [E] -> [F] -> [G] -> [H] -> [I]
            (9 nodes in chain - too long!)

Treeify triggered when:
1. Chain length >= 8 (TREEIFY_THRESHOLD)
2. AND table size >= 64

AFTER (red-black tree, O(log n) lookup):
table[i] -> [TreeBin]
                |
               [E]
              /   \
            [C]   [G]
           /  \   /  \
         [A] [D] [F] [H]
           \         \
           [B]       [I]
```

**Why does this matter?**

Without treeify, a malicious attacker could craft keys that all hash to the same bin, turning your O(1) `get()` into O(n). With treeify, worst case is O(log n).

**When does it un-treeify?**

When a tree bin shrinks below 6 nodes (UNTREEIFY_THRESHOLD), it converts back to a linked list.

---

## 2.5 Multi-Threaded Resize — The Secret Sauce

When the map needs to grow, CHM doesn't stop the world. Instead:

```
Old table (size 16):
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
| 0| 1| 2| 3| 4| 5| 6| 7| 8| 9|10|11|12|13|14|15|
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

New table (size 32):
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
| 0| 1| 2| 3| 4| 5| 6| 7| 8| 9|10|11|12|13|14|15|16|17|18|19|20|21|22|23|24|25|26|27|28|29|30|31|
+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

How it works:
1. Thread A starts resize, claims bins 0-3
2. Thread B does put(), sees resize, helps with bins 4-7
3. Thread C does put(), helps with bins 8-11
4. ...

Each migrated bin gets a ForwardingNode:
Old table[i] = ForwardingNode -> points to new table

Readers seeing ForwardingNode follow it to new table.
Writers help migrate before doing their work.

Result: Resize is spread across many threads!
No stop-the-world pause!
```

---

## 2.6 Reads Are Always Lock-Free!

This is crucial: `get()` NEVER takes a lock!

```java
get(key):
    hash = spread(key.hashCode())
    i = hash & (table.length - 1)
    
    e = table[i]  // Volatile read
    
    if (e == null) 
        return null
    
    if (e is ForwardingNode)
        return e.find(key)  // Follow to new table
    
    // Walk the chain (no lock!)
    while (e != null) {
        if (e.hash == hash && e.key.equals(key))
            return e.value  // Volatile read
        e = e.next  // Volatile read
    }
    
    return null
```

**How is this safe without locks?**

- Node's `val` and `next` fields are effectively `volatile`
- Writes publish before the node is linked
- Readers see a consistent (though possibly stale) view

**Iterators are "weakly consistent":**
- Never throw `ConcurrentModificationException`
- May or may not see concurrent updates
- Don't rely on iteration for a consistent snapshot!

---

## 2.7 The Atomic Compound Operations — What Seniors Use

These are the operations that make CHM powerful:

```java
// Put only if key is absent
map.putIfAbsent(key, value);

// Compute value if absent (lazy initialization)
map.computeIfAbsent(key, k -> expensiveComputation(k));

// Compute new value based on old value
map.compute(key, (k, oldVal) -> oldVal == null ? 1 : oldVal + 1);

// Merge with existing value
map.merge(key, 1, Integer::sum);  // Idiomatic counter increment!
```

**Why these matter:**

```java
// WRONG - Not atomic!
Long count = map.get(key);
if (count == null) count = 0L;
map.put(key, count + 1);
// Another thread could have updated between get and put!

// RIGHT - Atomic!
map.merge(key, 1L, Long::sum);
// The entire read-modify-write is atomic on that bin.
```

---

## 2.8 The `computeIfAbsent` Deadlock Trap — Interview Classic!

This is a famous trap that catches even experienced developers:

```java
ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

map.computeIfAbsent("a", k -> {
    // We're holding the lock on bin for "a"
    
    return map.computeIfAbsent("b", k2 -> "value");
    // This tries to acquire lock on bin for "b"
    // If "a" and "b" hash to the SAME bin... DEADLOCK!
    // Even if different bins, this is WRONG!
});
```

### What Happens

```
Thread 1:
1. computeIfAbsent("a", ...)
2. Acquires lock on bin X (where "a" hashes)
3. Runs the lambda
4. Lambda calls computeIfAbsent("b", ...)
5. Tries to acquire lock on bin Y (where "b" hashes)

If X == Y (same bin):
   DEADLOCK! Thread is waiting for a lock it already holds!
   (Actually, in modern JDKs: IllegalStateException or infinite loop)

If X != Y:
   Still WRONG! You're doing nested locking.
   Another thread could be doing the reverse.
   Classic lock-ordering deadlock risk!
```

### The Rules — Say These Aloud!

1. **NEVER** call back into the same map from inside `compute`/`computeIfAbsent`/`merge`
2. **NEVER** do blocking I/O in the lambda (you'll pin the bin for seconds!)
3. **NEVER** touch two maps under nested computes without a global lock order
4. **DO** keep the lambda pure and fast: compute value, return

### The Right Way

```java
// WRONG
map.computeIfAbsent(key, k -> loadFromDatabase(k));  // DB call holds bin lock!

// RIGHT
String value = loadFromDatabase(key);  // Load OUTSIDE
map.putIfAbsent(key, value);           // Then insert
// Or use a two-phase approach with a placeholder
```

---

## 2.9 `size()` Is Not Exact!

```java
int size = map.size();  // Not an exact snapshot!
```

**Why?**

CHM uses `LongAdder`-style striped counters for size:

```
+--------+--------+--------+--------+
| Cell 0 | Cell 1 | Cell 2 | Cell 3 |
|  +5    |  +3    |  +7    |  +2    |
+--------+--------+--------+--------+

size() = sum of all cells = 17

But while summing:
- Thread A might increment Cell 0
- Thread B might increment Cell 2

So size() might return 17, 18, or 19!
```

**This is fine for most uses.** If you need exact count, you'd need to lock the entire map (rarely worth it).

---

# Part 3: Queues — Blocking, Lock-Free, and Handoff

## 3.1 The Three Queues You Must Know Cold

| Queue | Bounded? | Locks | When to Use |
|-------|----------|-------|-------------|
| `ArrayBlockingQueue` | Yes (fixed) | ONE lock | Small bounded queues, predictable memory |
| `LinkedBlockingQueue` | Optional | TWO locks | Higher throughput, separate put/take |
| `ConcurrentLinkedQueue` | No (unbounded) | NONE (CAS) | Non-blocking, no backpressure |

---

## 3.2 `ArrayBlockingQueue` — Simple and Predictable

### The Structure

```
ArrayBlockingQueue (capacity = 5):

+---+---+---+---+---+
| A | B | C |   |   |
+---+---+---+---+---+
  ^           ^
  |           |
 take        put
 index       index

Circular array!
When put reaches the end, it wraps to the beginning.
```

### One Lock, Two Conditions

```java
class ArrayBlockingQueue<E> {
    final Object[] items;
    int takeIndex, putIndex, count;
    
    final ReentrantLock lock = new ReentrantLock();
    final Condition notEmpty = lock.newCondition();  // Consumers wait here
    final Condition notFull = lock.newCondition();   // Producers wait here
    
    void put(E e) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length)
                notFull.await();  // Wait until not full
            
            items[putIndex] = e;
            if (++putIndex == items.length) putIndex = 0;  // Wrap around
            count++;
            
            notEmpty.signal();  // Wake a consumer
        } finally {
            lock.unlock();
        }
    }
    
    E take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0)
                notEmpty.await();  // Wait until not empty
            
            E e = (E) items[takeIndex];
            items[takeIndex] = null;
            if (++takeIndex == items.length) takeIndex = 0;  // Wrap around
            count--;
            
            notFull.signal();  // Wake a producer
            return e;
        } finally {
            lock.unlock();
        }
    }
}
```

### When to Use ABQ

- Small, fixed-size queues
- Predictable memory usage
- When you want fairness option (`new ArrayBlockingQueue<>(100, true)`)

---

## 3.3 `LinkedBlockingQueue` — Two Locks = Higher Throughput

### The Key Insight: Separate Locks!

```
LinkedBlockingQueue:

        putLock                              takeLock
           |                                    |
           v                                    v
         [tail]                              [head]
           |                                    |
           v                                    v
+------+  +------+  +------+  +------+  +------+
| Node |->| Node |->| Node |->| Node |->| Node |
+------+  +------+  +------+  +------+  +------+
   ^                                        ^
   |                                        |
Producers                               Consumers
add here                                take here

Producers and consumers use DIFFERENT locks!
They rarely contend with each other!
```

### Why Two Locks Is Faster

```
ArrayBlockingQueue (one lock):
Producer: [lock][put][unlock]
Consumer: [lock][take][unlock]
Producer: [lock][put][unlock]
Consumer: [lock][take][unlock]
          ^^^^^
          Contention! Everyone fights for the same lock.

LinkedBlockingQueue (two locks):
Producer: [putLock][put][unlock]     Consumer: [takeLock][take][unlock]
Producer: [putLock][put][unlock]     Consumer: [takeLock][take][unlock]
                                     
Producers and consumers work in parallel!
~2x throughput under contention!
```

### The Footgun: Default Is UNBOUNDED!

```java
// DANGER! This is unbounded!
BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
// Capacity = Integer.MAX_VALUE = 2,147,483,647

// If producers are faster than consumers:
// Queue grows... and grows... and grows...
// OutOfMemoryError!

// ALWAYS specify capacity!
BlockingQueue<Task> queue = new LinkedBlockingQueue<>(1000);
```

---

## 3.4 `SynchronousQueue` — Zero Storage, Direct Handoff

### The Concept

```
Normal queue:
Producer -> [buffer] -> Consumer
            (storage)

SynchronousQueue:
Producer -> [nothing] -> Consumer
            (direct handoff!)

put() blocks until a take() is ready.
take() blocks until a put() is ready.
They meet in the middle!
```

### Visual: The Rendezvous

```
Time -->

Producer calls put("X"):
Producer: [put("X")]----waiting----+
                                   |
                                   | (blocked!)
                                   |
Consumer calls take():             |
Consumer:              [take()]----+
                                   |
                                   v
                            [HANDOFF!]
                            
Producer: returns (put succeeded)
Consumer: returns "X"
```

### Where It's Used

```java
// This is how newCachedThreadPool works!
ExecutorService pool = Executors.newCachedThreadPool();

// Internally:
new ThreadPoolExecutor(
    0, Integer.MAX_VALUE,           // 0 core, unlimited max
    60L, TimeUnit.SECONDS,
    new SynchronousQueue<Runnable>() // Direct handoff!
);

// When you submit a task:
// 1. Try to hand off to an idle thread
// 2. If no idle thread, create a new one
// 3. Never buffer tasks!
```

### When to Use

- Strict handoff: producer must know consumer received the message
- Thread pools where you want immediate execution or new thread
- Rendezvous patterns

---

## 3.5 `DelayQueue` — Time-Based Release

### The Concept

Elements have an expiration time. `take()` only returns elements whose delay has elapsed.

```
DelayQueue:

+------------------+------------------+------------------+
| Element A        | Element B        | Element C        |
| expires: 10:00   | expires: 10:05   | expires: 10:10   |
+------------------+------------------+------------------+
        ^
        |
      HEAD (soonest to expire)

At 9:55:
  take() blocks... waiting for 10:00

At 10:00:
  take() returns Element A!

At 10:01:
  take() blocks... waiting for 10:05
```

### The Interface

```java
interface Delayed extends Comparable<Delayed> {
    long getDelay(TimeUnit unit);  // How much time left?
}

class MyTask implements Delayed {
    final long executeAtNanos;
    
    MyTask(long delayNanos) {
        this.executeAtNanos = System.nanoTime() + delayNanos;
    }
    
    public long getDelay(TimeUnit unit) {
        long remaining = executeAtNanos - System.nanoTime();
        return unit.convert(remaining, TimeUnit.NANOSECONDS);
    }
    
    public int compareTo(Delayed other) {
        return Long.compare(
            this.getDelay(TimeUnit.NANOSECONDS),
            other.getDelay(TimeUnit.NANOSECONDS)
        );
    }
}
```

### Common Uses

- Cache eviction scheduling
- Retry timers with backoff
- Job scheduling with delays
- Rate limiting (tokens that become available after delay)

### The Trap: `poll()` vs `take()`

```java
DelayQueue<MyTask> queue = new DelayQueue<>();
queue.add(new MyTask(5, TimeUnit.SECONDS));  // Expires in 5 seconds

// At time 0:
MyTask task = queue.poll();  // Returns NULL! (not expired yet)
MyTask task = queue.take();  // BLOCKS for 5 seconds, then returns task
```

`poll()` returns `null` if the head hasn't expired, even if the queue is non-empty!

---

## 3.6 `PriorityBlockingQueue` — Priority-Based Ordering

### The Concept

```
PriorityBlockingQueue (min-heap):

                    [Priority 1]
                    /          \
           [Priority 3]    [Priority 2]
           /        \
    [Priority 5]  [Priority 4]

take() always returns the SMALLEST (highest priority) element.
```

### The Trap: Iteration Order!

```java
PriorityBlockingQueue<Integer> queue = new PriorityBlockingQueue<>();
queue.add(5);
queue.add(1);
queue.add(3);
queue.add(2);
queue.add(4);

// WRONG assumption:
for (Integer i : queue) {
    System.out.println(i);  // Does NOT print 1, 2, 3, 4, 5!
}
// Prints in HEAP order, not sorted order!
// Might print: 1, 2, 3, 5, 4

// RIGHT way to get sorted order:
while (!queue.isEmpty()) {
    System.out.println(queue.take());  // Prints 1, 2, 3, 4, 5
}
```

**Only `take()` and `poll()` respect priority order!**

---

## 3.7 `ConcurrentLinkedQueue` — Lock-Free, Non-Blocking

### The Concept

Uses Michael-Scott algorithm: CAS on head and tail, no locks ever.

```java
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

queue.offer("A");  // Never blocks, always succeeds (unbounded)
queue.offer("B");
queue.offer("C");

String s = queue.poll();  // Returns "A" or null if empty (never blocks!)
```

### Key Differences from LinkedBlockingQueue

| Aspect | `ConcurrentLinkedQueue` | `LinkedBlockingQueue` |
|--------|------------------------|----------------------|
| Blocking | Never | `take()` blocks if empty |
| Bounded | No | Optional |
| Locks | None (CAS) | Two locks |
| Use case | Non-blocking buffers | Producer-consumer with backpressure |

### When to Use

- Metrics buffers
- Event queues where you poll periodically
- Anywhere you need to enqueue without ever blocking

---

# Part 4: `CopyOnWriteArrayList` — The Read-Optimized List

## 4.1 The Concept

Every write creates a **new copy** of the entire array:

```
Initial state:
array -> [A, B, C, D, E]

Thread 1 (reader):
snapshot = array  // Grabs reference
// Iterates over [A, B, C, D, E]
// Never sees changes during iteration!

Thread 2 (writer):
add("F")
1. Lock
2. newArray = copy of [A, B, C, D, E] + F = [A, B, C, D, E, F]
3. array = newArray  // Volatile write
4. Unlock

After write:
array -> [A, B, C, D, E, F]  (new array)

Thread 1 still iterating over old array [A, B, C, D, E]!
No ConcurrentModificationException!
```

### Visual

```
BEFORE add("F"):
                    +---+---+---+---+---+
array reference --> | A | B | C | D | E |
                    +---+---+---+---+---+
                      ^
                      |
                    Reader iterating here

AFTER add("F"):
                    +---+---+---+---+---+
(old array)         | A | B | C | D | E |  <-- Reader still here!
                    +---+---+---+---+---+

                    +---+---+---+---+---+---+
array reference --> | A | B | C | D | E | F |  (NEW array)
                    +---+---+---+---+---+---+

Reader finishes with old snapshot.
New readers see new array.
```

## 4.2 When It Wins

**Read:Write ratio > 100:1**

```
Reads: Just grab the volatile reference. No lock!
Writes: Copy entire array. Expensive!

Good for:
- Event listener lists (register once, fire many times)
- Configuration lists (load once, read constantly)
- Plugin registries

Bad for:
- Anything with frequent writes
- Large arrays (copying 10,000 elements on every add!)
```

## 4.3 When It Loses

```java
// DISASTER: Hot-path append
CopyOnWriteArrayList<Request> requests = new CopyOnWriteArrayList<>();

void handleRequest(Request r) {
    requests.add(r);  // COPIES THE ENTIRE ARRAY!
}

// With 10,000 requests:
// - 10,000 array copies
// - Each copy is O(n)
// - Total: O(n^2) work!
// - GC goes crazy
```

## 4.4 The Iterator Trap

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("A");
list.add("B");

Iterator<String> it = list.iterator();

// This throws UnsupportedOperationException!
it.remove();  // Can't modify through iterator!

// Iterators are READ-ONLY snapshots.
```

---

# Part 5: `ConcurrentSkipListMap` — Ordered and Concurrent

## 5.1 What's a Skip List?

A skip list is like a linked list with "express lanes":

```
Level 3:  1 ─────────────────────────────► 21 ─────────────────────────────► ∞
          |                                 |
Level 2:  1 ────────► 8 ─────────────────► 21 ────────► 34 ────────────────► ∞
          |           |                     |            |
Level 1:  1 ──► 4 ──► 8 ──► 13 ──► 17 ──► 21 ──► 27 ──► 34 ──► 42 ──► 50 ──► ∞

To find 27:
1. Start at Level 3: 1 -> 21 (27 > 21, go right)
2. At 21, Level 3 goes to ∞ (27 < ∞, go down)
3. Level 2: 21 -> 34 (27 < 34, go down)
4. Level 1: 21 -> 27 (found!)

Average: O(log n) - like a balanced tree!
```

## 5.2 Why Skip List for Concurrency?

- Each level's pointers can be updated with CAS
- No need to rebalance like a red-black tree
- Naturally lock-free-friendly

## 5.3 When to Use

```java
ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();

// Ordered iteration
for (Map.Entry<Integer, String> e : map.entrySet()) {
    // Keys come out in sorted order!
}

// Range queries
map.subMap(10, 20);      // Keys from 10 to 19
map.headMap(10);         // Keys less than 10
map.tailMap(10);         // Keys >= 10

// Navigation
map.floorKey(15);        // Largest key <= 15
map.ceilingKey(15);      // Smallest key >= 15
map.lowerKey(15);        // Largest key < 15
map.higherKey(15);       // Smallest key > 15
```

**Use when:**
- You need an ordered concurrent map
- You need range queries
- You can tolerate weakly consistent iterators

**Don't use when:**
- You don't need ordering (use `ConcurrentHashMap` - faster!)

---

# Part 6: Production War Stories

## War Story 1: The CHM Lambda That Stalled the App

**The Setup:**
```java
cache.computeIfAbsent(key, k -> loadFromDatabase(k));
```

**What Happened:**
- Database call took 2 seconds under load
- CHM holds the bin's `synchronized` monitor during the lambda
- Every other write to keys in the same bin **serialized behind the DB call**
- Throughput collapsed

**The Fix:**
```java
// Load OUTSIDE the compute
String value = loadFromDatabase(key);
cache.putIfAbsent(key, value);
```

---

## War Story 2: The Unbounded LBQ That Ate Memory

**The Setup:**
```java
ExecutorService pool = Executors.newFixedThreadPool(20);
// Internally uses: new LinkedBlockingQueue<>() -- UNBOUNDED!
```

**What Happened:**
- Downstream service slowed by 3x
- Tasks piled up in the unbounded queue
- Queue grew to 40 million tasks
- GC pauses of 30 seconds
- Eventually OOM

**The Fix:**
```java
ExecutorService pool = new ThreadPoolExecutor(
    20, 20, 0, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(5000),        // BOUNDED!
    new ThreadPoolExecutor.CallerRunsPolicy()  // Backpressure!
);
```

---

## War Story 3: The CopyOnWriteArrayList That OOMed

**The Setup:**
- RPC framework kept per-request listeners in a `CopyOnWriteArrayList`
- Each request added and removed a listener

**What Happened:**
- Every request = 2 array copies (add + remove)
- Array had 20,000 elements
- Allocation rate spiked
- GC ran constantly
- OOM

**The Fix:**
```java
// Changed to:
ConcurrentHashMap<UUID, Listener> listeners = new ConcurrentHashMap<>();
```

---

## War Story 4: The PriorityBlockingQueue "Iteration Bug"

**The Setup:**
- Debug endpoint showed "next 5 jobs by priority"
- Used iterator to get first 5 elements

**What Happened:**
- Users complained ordering was wrong
- Iterator returns heap order, not sorted order!

**The Fix:**
```java
// WRONG
queue.stream().limit(5).forEach(System.out::println);

// RIGHT
queue.stream().sorted().limit(5).forEach(System.out::println);
```

---

## War Story 5: The DelayQueue That Never Fired

**The Setup:**
```java
public long getDelay(TimeUnit unit) {
    return unit.convert(
        expiresAt - System.currentTimeMillis(),  // WRONG!
        TimeUnit.MILLISECONDS
    );
}
```

**What Happened:**
- NTP adjusted the clock backward
- `expiresAt - currentTimeMillis()` became huge positive number
- Elements never expired

**The Fix:**
```java
public long getDelay(TimeUnit unit) {
    return unit.convert(
        expiresAtNanos - System.nanoTime(),  // Monotonic!
        TimeUnit.NANOSECONDS
    );
}
```

`System.nanoTime()` is monotonic — it never goes backward!

---

# Part 7: Interview Traps

| Question | Bad Answer | L5 Answer |
|----------|-----------|-----------|
| "Why is Java 8 CHM faster than Java 7?" | "Different code." | "Bin-level `synchronized` + CAS on empty bin replaces fixed 16 Segments. Concurrency scales with bins, not a constant." |
| "`get()` locks?" | "Yes." | "No — reads are lock-free, using volatile semantics. Iterators are weakly consistent." |
| "`computeIfAbsent` and I/O?" | "Fine." | "Never do blocking I/O inside; you pin the bin. Never recursively touch the same map — deadlock." |
| "`ABQ` vs `LBQ`?" | "One's array, one's linked." | "ABQ has one lock; LBQ has two (put/take) → higher throughput. LBQ is unbounded by default — footgun." |
| "`CopyOnWriteArrayList` — always safe?" | "Yeah, it's concurrent." | "Only cheap when reads dominate 100:1. Every write copies the array." |
| "`size()` on CHM?" | "Returns exact size." | "Uses striped counters. Cheap but not a consistent snapshot." |
| "Iterating `PriorityBlockingQueue`?" | "In priority order." | "NO — iteration is heap order. Only `take()` respects priority." |
| "Default capacity of `new LinkedBlockingQueue<>()`?" | "Some reasonable number." | "`Integer.MAX_VALUE` — effectively unbounded. Always specify capacity!" |

---

# Part 8: Self-Check Questions

1. **Describe the four cases inside `ConcurrentHashMap.put()`:**
   - Empty bin: CAS (no lock)
   - Forwarding node: help transfer, retry
   - List bin: synchronized on head, walk chain
   - Tree bin: synchronized on tree, tree insert

2. **When does CHM treeify a bin?**
   - Chain length >= 8 AND table size >= 64

3. **Why is `computeIfAbsent(k, k -> ioCall(k))` dangerous?**
   - Holds bin lock during I/O
   - Pins the bin for seconds
   - Other writes to same bin serialize

4. **ABQ vs LBQ — three differences:**
   - ABQ: one lock, LBQ: two locks
   - ABQ: fixed size, LBQ: optional bound
   - ABQ: array, LBQ: linked list

5. **Default capacity of `new LinkedBlockingQueue<>()`?**
   - `Integer.MAX_VALUE` — effectively unbounded!

6. **What does `SynchronousQueue.put()` do when no consumer?**
   - Blocks until a consumer calls `take()`

7. **Three use cases for `DelayQueue`:**
   - Cache eviction, retry timers, job scheduling

8. **Break-even read:write ratio for `CopyOnWriteArrayList`?**
   - ~100:1 (reads must dominate heavily)

9. **Iterators on `PriorityBlockingQueue` — what order?**
   - Heap order (NOT sorted order!)

10. **How do CHM iterators differ from HashMap iterators?**
    - CHM: weakly consistent, never throws CME
    - HashMap: fail-fast, throws CME on modification

---

# Summary: The Complete Mental Model

```
+=========================================================================+
|                    MODULE 5: CONCURRENT COLLECTIONS                      |
+=========================================================================+
|                                                                          |
|  ConcurrentHashMap                                                      |
|  +--------------------------------------------------------------------+ |
|  |  Java 8+: bin-level locking (not segments!)                        | |
|  |  Empty bin: CAS (no lock!)                                         | |
|  |  Non-empty: synchronized on head node                              | |
|  |  Treeify at 8 nodes (collision defense)                            | |
|  |  get() is always lock-free                                         | |
|  |  computeIfAbsent: NEVER do I/O or recursive calls!                 | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  Queues                                                                 |
|  +--------------------------------------------------------------------+ |
|  |  ArrayBlockingQueue: one lock, bounded, simple                     | |
|  |  LinkedBlockingQueue: two locks, higher throughput, UNBOUNDED!     | |
|  |  SynchronousQueue: zero storage, direct handoff                    | |
|  |  DelayQueue: time-based release, use nanoTime!                     | |
|  |  PriorityBlockingQueue: only take() respects priority!             | |
|  |  ConcurrentLinkedQueue: lock-free, non-blocking                    | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  CopyOnWriteArrayList                                                   |
|  +--------------------------------------------------------------------+ |
|  |  Every write copies the entire array                               | |
|  |  Readers never lock (snapshot iteration)                           | |
|  |  Only for read:write > 100:1                                       | |
|  |  NOT for hot-path append!                                          | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  ConcurrentSkipListMap                                                  |
|  +--------------------------------------------------------------------+ |
|  |  Ordered, concurrent map                                           | |
|  |  O(log n) operations                                               | |
|  |  Range queries: subMap, floorKey, ceilingKey                       | |
|  |  Use only when you need ordering!                                  | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
+=========================================================================+
```

---

**You've completed Module 5!** You now understand how to pick the right concurrent collection, the internals of `ConcurrentHashMap`, and the traps that catch even experienced developers.

**Next up:** [Module 6 — Thread Pools & Async](./Module-06-ThreadPools-Async-Explained.md)
