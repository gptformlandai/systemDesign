# Module 3: Lock-Free Programming & Atomics — The Fun, Deep-Dive Explanation

> This document explains what CAS actually is at the CPU level, why `AtomicLong` collapses under contention, why `LongAdder` doesn't, and the infamous ABA problem — like you're a junior dev who's never seen this before.

---

# Part 1: Compare-And-Swap (CAS) — The Foundation of Lock-Free

## 1.1 The Coffee Shop Scoreboard Analogy

Before we dive into CPU instructions, let's understand CAS with a simple analogy:

### The Locked Way (Traditional)

Imagine a coffee shop with a whiteboard showing "Coffees Sold Today: 7"

```
Barista A wants to update the count:

Barista A: "MINE!" (grabs the marker)
           +------------------+
           | Coffees Sold: 7  |
           +------------------+
           Reads 7, writes 8
           "DONE!" (drops marker)

Barista B: (waiting...) "Finally!"
           (grabs marker, updates)
```

**Problem:** Barista B has to WAIT. If Barista A is slow, everyone waits.

### The CAS Way (Lock-Free)

```
Barista A wants to update:
1. Reads the board: "7"
2. Calculates new value: 8
3. Walks up and says: "IF the board still says 7, change it to 8"

Two scenarios:

SCENARIO 1: Board still says 7
+------------------+     +------------------+
| Coffees Sold: 7  | --> | Coffees Sold: 8  |
+------------------+     +------------------+
"Yep, it's 7. Changed to 8. SUCCESS!"

SCENARIO 2: Board now says 9 (Barista B was faster!)
+------------------+
| Coffees Sold: 9  |
+------------------+
"Hmm, it's not 7 anymore. FAILED!"
Barista A: "Let me recalculate... 9 + 1 = 10"
           "IF board says 9, change to 10"
           SUCCESS!
```

**Key insight:** Nobody blocks. Nobody waits. If you fail, you just try again with fresh data.

---

## 1.2 The CPU Instruction — What Actually Happens

Modern CPUs have a single **atomic** instruction that does CAS:

```
Compare-And-Swap (CAS):
1. Read memory location
2. Compare to expected value
3. If equal, write new value
4. Return success/failure

All in ONE atomic operation!
```

### Different CPUs, Same Idea

| CPU Architecture | Instruction | Notes |
|-----------------|-------------|-------|
| **x86/x64** | `LOCK CMPXCHG` | Single instruction with LOCK prefix |
| **ARM** | `LDREX` + `STREX` | Load-exclusive / Store-exclusive pair |
| **ARM v8.1+** | `CASAL` | Single CAS instruction |
| **RISC-V** | `LR` + `SC` | Load-reserved / Store-conditional |

### What Makes It Fast?

```
Traditional Lock:
+--------+     +--------+     +--------+     +--------+
| Thread | --> | Kernel | --> | Context| --> | Kernel | --> Resume
|  calls |     | trap   |     | switch |     | return |
+--------+     +--------+     +--------+     +--------+
                  ~1000+ CPU cycles!

CAS:
+--------+     +--------+
| Thread | --> | Single |
| calls  |     | CPU    |
| CAS    |     | instr  |
+--------+     +--------+
              ~10-50 CPU cycles!
```

**No OS involvement!** No context switch! No kernel/user mode crossing!

The `LOCK` prefix on x86 asserts a **cache-line lock** on that memory address. Other cores see the update atomically. This is why lock-free is fast.

---

## 1.3 The CAS Loop Pattern — Memorize This!

This is the fundamental pattern of ALL lock-free algorithms:

```java
private final AtomicInteger counter = new AtomicInteger();

public void increment() {
    while (true) {
        int current = counter.get();      // 1. Read current value
        int next = current + 1;           // 2. Compute new value
        if (counter.compareAndSet(current, next)) {
            return;                       // 3. SUCCESS! We're done.
        }
        // 4. FAILED! Someone else changed it. Loop and retry.
    }
}
```

### Visual: The CAS Loop in Action

```
Thread A                          Thread B
   |                                 |
   v                                 v
Read counter = 5                  Read counter = 5
   |                                 |
   v                                 v
Compute: 5 + 1 = 6                Compute: 5 + 1 = 6
   |                                 |
   v                                 |
CAS(5, 6) --> SUCCESS!               |
counter is now 6                     |
   |                                 v
   |                              CAS(5, 6) --> FAIL!
   |                              (counter is 6, not 5)
   |                                 |
   |                                 v
   |                              Read counter = 6
   |                                 |
   |                                 v
   |                              Compute: 6 + 1 = 7
   |                                 |
   |                                 v
   |                              CAS(6, 7) --> SUCCESS!
   v                                 v
```

### The Modern Way: `updateAndGet`

You almost never write the raw loop anymore. Since Java 8:

```java
// Old way (manual loop)
while (true) {
    int cur = counter.get();
    int next = cur + 1;
    if (counter.compareAndSet(cur, next)) return next;
}

// New way (Java 8+)
counter.incrementAndGet();

// Or with custom logic:
counter.updateAndGet(cur -> cur + 1);

// Or with two arguments:
AtomicLong max = new AtomicLong(Long.MIN_VALUE);
max.accumulateAndGet(newValue, Math::max);
```

**Warning:** The lambda must be **PURE** — no side effects, no I/O! It can be called multiple times due to retries.

```java
// WRONG! Side effect in lambda!
counter.updateAndGet(cur -> {
    logger.info("Updating to " + (cur + 1));  // May log 5 times!
    return cur + 1;
});

// CORRECT
int result = counter.incrementAndGet();
logger.info("Updated to " + result);  // Logs once
```

---

## 1.4 Lock-Free vs Wait-Free

These terms come up in interviews:

### Lock-Free

> "The system as a whole makes progress. Some individual thread may retry forever."

```
Thread A: [try][fail][try][fail][try][SUCCESS]
Thread B: [try][SUCCESS]
Thread C: [try][fail][try][fail][try][fail][try][fail]...

System is making progress (A and B succeeded).
But C might be unlucky and retry many times.
```

### Wait-Free

> "Every thread makes progress in a bounded number of steps."

```
Thread A: [try][SUCCESS] (max 1 try)
Thread B: [try][SUCCESS] (max 1 try)
Thread C: [try][SUCCESS] (max 1 try)

Every thread guaranteed to finish quickly.
```

**Reality:** Most production code (`AtomicLong`, `ConcurrentHashMap`) is **lock-free**, not wait-free. Wait-free is much harder to implement.

---

## 1.5 Optimistic vs Pessimistic Concurrency

**Interview one-liner:**

| Approach | Philosophy | Example |
|----------|-----------|---------|
| **Pessimistic** | "Assume conflict, take a lock first" | `synchronized`, `ReentrantLock` |
| **Optimistic** | "Assume no conflict, try, verify, retry on collision" | CAS, lock-free algorithms |

### When Optimistic Wins

```
Low contention (conflicts rare):

Pessimistic:
Thread A: [lock][work][unlock]
Thread B: [lock][work][unlock]
Thread C: [lock][work][unlock]
Overhead: Lock acquisition every time, even when no conflict!

Optimistic (CAS):
Thread A: [try][SUCCESS]
Thread B: [try][SUCCESS]
Thread C: [try][SUCCESS]
No overhead! Just fast CAS.
```

### When Optimistic Loses

```
High contention (conflicts frequent):

CAS with 64 threads:
Thread 1: [try][SUCCESS]
Thread 2: [try][FAIL][try][FAIL][try][SUCCESS]
Thread 3: [try][FAIL][try][FAIL][try][FAIL][try][SUCCESS]
...
Thread 64: [try][FAIL][FAIL][FAIL][FAIL][FAIL]... (many retries!)

This is called a "CAS SPIN STORM"
- High CPU usage
- Low actual throughput
- Cache-line ping-pong between cores
```

**The fix:** Use `LongAdder` (covered in Part 3).

---

## 1.6 When CAS Is the WRONG Choice

| Scenario | Why CAS Is Wrong | What to Use Instead |
|----------|-----------------|---------------------|
| **Long critical section** | You'd be looping forever | Use a lock |
| **Multiple correlated fields** | CAS is one word at a time | Lock, or wrap in immutable object + `AtomicReference` |
| **Blocking I/O inside update** | Retry cost is unbounded | Restructure code |
| **Very high write contention** | Retry storms burn CPU | `LongAdder` or striping |

### Example: Multiple Correlated Fields

```java
// WRONG: Two separate atomics
AtomicInteger x = new AtomicInteger();
AtomicInteger y = new AtomicInteger();

// Thread 1: Move point from (0,0) to (1,1)
x.set(1);
// <-- Thread 2 reads here: sees (1, 0)! Inconsistent!
y.set(1);

// CORRECT: Immutable object + AtomicReference
record Point(int x, int y) {}
AtomicReference<Point> point = new AtomicReference<>(new Point(0, 0));

// Thread 1: Atomic swap
point.set(new Point(1, 1));  // Thread 2 sees either (0,0) or (1,1), never (1,0)
```

---

# Part 2: The ABA Problem — The Sneaky Trap

## 2.1 What Is ABA?

CAS asks: *"Is the value still what I saw?"*

CAS does **NOT** ask: *"Has this memory been touched since I looked?"*

```
The ABA Problem:

Time 0: Value = A
        Thread 1 reads A, gets preempted...

Time 1: Thread 2 changes A -> B

Time 2: Thread 2 changes B -> A

Time 3: Thread 1 wakes up
        Thread 1: "Is value still A? YES!"
        Thread 1: CAS succeeds!
        
But the value went A -> B -> A!
Thread 1 thinks nothing changed, but the WORLD changed!
```

### Visual Timeline

```
Thread 1                    Memory                    Thread 2
   |                          |                          |
   v                          |                          |
Read value = A                |                          |
   |                       [A]                           |
   | (preempted)              |                          |
   |                          |                          v
   |                          |                    Change A -> B
   |                       [B]                           |
   |                          |                          v
   |                          |                    Change B -> A
   |                       [A]                           |
   v                          |                          |
CAS(A, newValue)              |                          |
   |                          |                          |
"Is it A? YES!"            [A]                           |
   |                          |                          |
SUCCESS! (but wrong!)         |                          |
```

---

## 2.2 A Real Bug: Lock-Free Stack That Breaks

Here's a lock-free stack implementation that has the ABA bug:

```java
class UnsafeStack<T> {
    private final AtomicReference<Node<T>> top = new AtomicReference<>();

    static class Node<T> {
        final T value;
        Node<T> next;
        Node(T v) { this.value = v; }
    }

    void push(T v) {
        Node<T> newTop = new Node<>(v);
        Node<T> oldTop;
        do {
            oldTop = top.get();
            newTop.next = oldTop;
        } while (!top.compareAndSet(oldTop, newTop));
    }

    T pop() {
        Node<T> oldTop, newTop;
        do {
            oldTop = top.get();
            if (oldTop == null) return null;
            newTop = oldTop.next;
        } while (!top.compareAndSet(oldTop, newTop));
        return oldTop.value;
    }
}
```

### The Attack Scenario

```
Initial stack: A -> B -> C (top points to A)

Thread 1 (T1): pop()
   |
   v
Reads top = A
Computes newTop = A.next = B
   |
   | (PREEMPTED!)
   |
   
Thread 2 (T2): 
   |
   v
pop() -> removes A (A is now "free")
Stack: B -> C

pop() -> removes B
Stack: C

push(A) -> reuses node A!
Stack: A -> C

   |
   v
T1 wakes up:
CAS(A, B) -- "Is top still A? YES!"
SUCCESS!

Stack is now: B -> ???

But B was already popped! B.next might point to garbage!
CORRUPTION!
```

### Visual: The Corruption

```
Before T1 sleeps:
top -> [A] -> [B] -> [C] -> null

While T1 sleeps, T2 does:
1. pop A:  top -> [B] -> [C] -> null     (A is "free")
2. pop B:  top -> [C] -> null            (B is "free")  
3. push A: top -> [A] -> [C] -> null     (A reused!)

T1 wakes up:
- T1 has oldTop = A (correct, A is back!)
- T1 has newTop = B (STALE! B is not on stack!)
- CAS(A, B) succeeds because top IS A

After T1's CAS:
top -> [B] -> ??? (B was popped! B.next is garbage!)

STACK CORRUPTED!
```

---

## 2.3 Where ABA Actually Bites in Production

| Scenario | Why ABA Happens |
|----------|----------------|
| **Object pools** | Nodes get recycled and reused |
| **Lock-free memory allocators** | Memory blocks returned and reallocated |
| **Pointer-based data structures** | Stacks, queues with node reuse |
| **Token-based protocols** | "If token matches, proceed" |

---

## 2.4 The Fixes

### Fix 1: Version Stamp (`AtomicStampedReference`)

Add a version number that increments on every change:

```java
AtomicStampedReference<Node<T>> top = new AtomicStampedReference<>(null, 0);

void push(T v) {
    int[] stampHolder = new int[1];
    Node<T> oldTop, newTop = new Node<>(v);
    int oldStamp;
    do {
        oldTop = top.get(stampHolder);
        oldStamp = stampHolder[0];
        newTop.next = oldTop;
    } while (!top.compareAndSet(oldTop, newTop, oldStamp, oldStamp + 1));
}

T pop() {
    int[] stampHolder = new int[1];
    Node<T> oldTop, newTop;
    int oldStamp;
    do {
        oldTop = top.get(stampHolder);
        oldStamp = stampHolder[0];
        if (oldTop == null) return null;
        newTop = oldTop.next;
    } while (!top.compareAndSet(oldTop, newTop, oldStamp, oldStamp + 1));
    return oldTop.value;
}
```

**How it works:**

```
Time 0: top = (A, stamp=0)
        T1 reads (A, 0)

Time 1: T2 changes to (B, stamp=1)
Time 2: T2 changes to (A, stamp=2)

Time 3: T1 wakes up
        T1: CAS((A,0), (B,1))
        Current is (A, 2) -- stamp doesn't match!
        CAS FAILS! T1 retries with fresh data.
```

Even though the pointer is back to A, the stamp is different!

### Fix 2: `AtomicMarkableReference`

Like stamped, but with just a boolean flag:

```java
AtomicMarkableReference<Node<T>> ref = new AtomicMarkableReference<>(node, false);

// Mark a node as "being deleted"
ref.attemptMark(node, true);

// CAS only if not marked
ref.compareAndSet(expected, newVal, false, false);
```

**Use when:** You only need to signal one logical state change (e.g., "this node is being deleted").

### Fix 3: Don't Recycle Memory

In Java with GC, ABA is **rarer** than in C/C++:

```
Why Java is safer:

T1 has reference to node A
T2 pops A, but T1 still has reference
GC sees: "T1 has reference to A, can't collect A"
A stays alive!
T2 can't reuse A's memory for something else.

So when T1 wakes up, A is still the same A.
```

**But if you use an object pool** (reusing objects manually), ABA is back!

### Fix 4: Immutable Snapshots

If your CAS updates an entire immutable object, ABA usually disappears:

```java
record State(int x, int y, String status) {}

AtomicReference<State> state = new AtomicReference<>(new State(0, 0, "init"));

// Each update creates a NEW object
state.updateAndGet(s -> new State(s.x + 1, s.y, "updated"));

// Even if values are the same, it's a different object instance!
```

### Interview One-Liner

> "In pointer-CAS on reusable memory, ABA is when the value cycles back and CAS falsely succeeds. Fix with a version stamp (`AtomicStampedReference`) or by avoiding memory reuse. In pure-Java with GC, ABA is rare because live references keep objects alive."

---

# Part 3: The Atomic Wrapper Family

## 3.1 The Core Wrappers

| Class | What It Holds | Common Operations |
|-------|--------------|-------------------|
| `AtomicInteger` | `int` | `incrementAndGet`, `getAndAdd`, `compareAndSet`, `updateAndGet` |
| `AtomicLong` | `long` | Same as above |
| `AtomicBoolean` | `boolean` | `compareAndSet`, `getAndSet` |
| `AtomicReference<V>` | Object reference | `compareAndSet`, `updateAndGet(fn)` |
| `AtomicIntegerArray` | `int[]` with per-index atomicity | `getAndAdd(i, x)`, `compareAndSet(i, exp, new)` |
| `AtomicLongArray` | `long[]` with per-index atomicity | Same |
| `AtomicReferenceArray<V>` | `V[]` with per-index atomicity | Same |
| `AtomicStampedReference<V>` | Reference + int version | ABA-safe |
| `AtomicMarkableReference<V>` | Reference + boolean mark | ABA-safe with single bit |

## 3.2 What's Inside an Atomic Wrapper?

An `AtomicX` wrapper is surprisingly simple:

```
+---------------------------+
|     AtomicInteger         |
+---------------------------+
| volatile int value;       |  <-- The actual data
+---------------------------+
| compareAndSet(exp, new)   |  <-- CAS via VarHandle/Unsafe
| incrementAndGet()         |
| updateAndGet(fn)          |
+---------------------------+

That's it! No lock, no queue, no OS state.
Just a volatile field + CAS primitive.
```

**Memory footprint:**
- `AtomicInteger`: ~16 bytes (object header + int)
- Plain `int`: 4 bytes

**Performance (uncontended):**
- Nearly as fast as a plain `int` read/write
- CAS adds ~10-50 CPU cycles

## 3.3 Decision Tree: Which Atomic to Use?

```
Need atomicity on a single value?
|
+-- Just visibility (last-writer-wins)?
|   |
|   +--> Use: volatile field
|
+-- Single-variable read-modify-write (counter, flag)?
|   |
|   +--> Use: AtomicInteger / AtomicLong / AtomicBoolean
|
+-- Reference swap (config, cache)?
|   |
|   +--> Use: AtomicReference<ImmutableObject>
|
+-- ABA risk with pointer reuse?
|   |
|   +--> Use: AtomicStampedReference
|
+-- High-contention counter?
|   |
|   +--> Use: LongAdder / LongAccumulator (see next section!)
|
+-- Multiple correlated fields?
|   |
|   +--> Use: Immutable object + AtomicReference (swap the whole thing)
|
+-- Need blocking / waiting?
    |
    +--> Use: A lock! CAS is the wrong tool.
```

## 3.4 `AtomicReference` vs `volatile` Reference

This confuses many developers:

```java
// Option 1: volatile reference
volatile Config config;

// Option 2: AtomicReference
AtomicReference<Config> config = new AtomicReference<>();
```

**When to use which?**

| Scenario | Use | Why |
|----------|-----|-----|
| Last-writer-wins (just publish new config) | `volatile` | Simpler, no wrapper overhead |
| Conditional swap (only update if still X) | `AtomicReference` | Need `compareAndSet` |
| Read-modify-write on reference | `AtomicReference` | Need `updateAndGet` |

```java
// volatile is enough here:
volatile Config config;
public void updateConfig(Config newConfig) {
    config = newConfig;  // Just publish, don't care about old value
}

// AtomicReference needed here:
AtomicReference<Config> config = new AtomicReference<>();
public boolean updateIfMatch(Config expected, Config newConfig) {
    return config.compareAndSet(expected, newConfig);  // Conditional!
}
```

---

# Part 4: High-Contention Counters — `LongAdder` & `LongAccumulator`

## 4.1 The Problem: `AtomicLong` Under Contention

Imagine 64 threads all incrementing the same `AtomicLong`:

```java
AtomicLong counter = new AtomicLong();

// 64 threads all doing:
counter.incrementAndGet();
```

**What happens:**

```
Thread 1: Read counter = 100
Thread 2: Read counter = 100
Thread 3: Read counter = 100
...
Thread 64: Read counter = 100

Thread 1: CAS(100, 101) --> SUCCESS!
Thread 2: CAS(100, 101) --> FAIL! (counter is now 101)
Thread 3: CAS(100, 101) --> FAIL!
...
Thread 64: CAS(100, 101) --> FAIL!

63 threads failed! They all retry...

Thread 2: Read counter = 101
Thread 3: Read counter = 101
...

Thread 2: CAS(101, 102) --> SUCCESS!
Thread 3: CAS(101, 102) --> FAIL!
...

62 threads failed! They all retry...

This is a CAS RETRY STORM!
```

**But wait, it gets worse!**

Every CAS **invalidates the cache line** on every other core:

```
Core 0: [counter in L1 cache]
Core 1: [counter in L1 cache]
Core 2: [counter in L1 cache]
...
Core 63: [counter in L1 cache]

Core 0 does CAS:
Core 0: [counter UPDATED]
Core 1: [INVALIDATED!] Must fetch from L3/RAM
Core 2: [INVALIDATED!] Must fetch from L3/RAM
...
Core 63: [INVALIDATED!] Must fetch from L3/RAM

Core 1 does CAS:
Core 0: [INVALIDATED!]
Core 1: [counter UPDATED]
Core 2: [INVALIDATED!]
...

PING... PONG... PING... PONG...
Cache lines bouncing between cores!
```

**Result:**
- High CPU usage (cores are busy retrying)
- Low actual throughput (most CAS attempts fail)
- This is called **cache-line ping-pong**

---

## 4.2 The Solution: `LongAdder` — Striping

Instead of one hot memory location, keep an **array of cells**:

```
AtomicLong (single hot spot):
+-------------------+
|    value: 100     | <-- ALL 64 threads fight for this!
+-------------------+
Cache-line ping-pong!


LongAdder (striped cells):
+------+------+------+------+------+------+------+------+
| Cell | Cell | Cell | Cell | Cell | Cell | Cell | Cell |
|  12  |  15  |  11  |  14  |  13  |  12  |  11  |  12  |
+------+------+------+------+------+------+------+------+
   ^      ^      ^      ^      ^      ^      ^      ^
   |      |      |      |      |      |      |      |
  T1     T2     T3     T4     T5     T6     T7     T8
  
Each thread hashes to its own cell!
Different cache lines = no contention!

sum() = 12 + 15 + 11 + 14 + 13 + 12 + 11 + 12 = 100
```

**How it works:**

1. Each thread hashes to a cell based on thread ID
2. Thread increments **its own cell** (no contention!)
3. Each cell is on its own **cache line** (no ping-pong!)
4. `sum()` walks all cells and adds them up

```java
LongAdder counter = new LongAdder();

// Thread 1 (hashes to cell 0)
counter.increment();  // cell[0]++

// Thread 2 (hashes to cell 1)
counter.increment();  // cell[1]++

// Thread 3 (hashes to cell 2)
counter.increment();  // cell[2]++

// No contention! Each thread has its own cell!

// When you need the total:
long total = counter.sum();  // Walks all cells, adds them up
```

---

## 4.3 `AtomicLong` vs `LongAdder` — When to Use Which

| Aspect | `AtomicLong` | `LongAdder` |
|--------|-------------|-------------|
| **Memory** | ~16 bytes | ~16 bytes + up to N * 64 bytes (cells) |
| **Low-contention writes** | Fast | Fast (uses base value) |
| **High-contention writes** | **COLLAPSES** | Scales near-linearly |
| **Reads** | O(1), exact value | O(cells), **NOT** a consistent snapshot |
| **Best for** | Sequence generators, IDs, rate limits | Metrics, counters, throughput stats |

### The Critical Difference: `sum()` Is Not Atomic!

```java
LongAdder counter = new LongAdder();

// Thread 1: counter.increment();
// Thread 2: counter.increment();
// Thread 3: long total = counter.sum();

// Thread 3's sum() might or might not include
// Thread 1 and Thread 2's increments!
// It's NOT a consistent snapshot!
```

**Why?** `sum()` walks the cells one by one. While it's walking, other threads might be updating cells it already visited or hasn't visited yet.

```
sum() walks cells:
+------+------+------+------+
|  10  |  20  |  30  |  40  |
+------+------+------+------+
   ^
   |
sum reads 10

Meanwhile, Thread X increments cell[0]:
+------+------+------+------+
|  11  |  20  |  30  |  40  |
+------+------+------+------+

sum continues:
+------+------+------+------+
|  11  |  20  |  30  |  40  |
+------+------+------+------+
          ^
          |
sum reads 20

sum() returns 10 + 20 + 30 + 40 = 100
But actual total is 11 + 20 + 30 + 40 = 101!

sum() missed Thread X's increment!
```

**This is fine for metrics!** You don't need exact counts for "requests per second."

**This is NOT fine for IDs!** Use `AtomicLong` if you need exact, monotonic values.

---

## 4.4 `LongAccumulator` — The Generalized Cousin

Same striped-cell idea, but with a custom function:

```java
// Track maximum observed latency:
LongAccumulator maxLatency = new LongAccumulator(Math::max, Long.MIN_VALUE);

// Each thread records its latency:
maxLatency.accumulate(currentLatencyNs);

// Get the maximum:
long max = maxLatency.get();
```

**Works with any associative, side-effect-free function:**

```java
// Maximum
new LongAccumulator(Math::max, Long.MIN_VALUE);

// Minimum
new LongAccumulator(Math::min, Long.MAX_VALUE);

// Sum (same as LongAdder)
new LongAccumulator(Long::sum, 0);

// Product
new LongAccumulator((a, b) -> a * b, 1);
```

---

## 4.5 Real Production Numbers

Doug Lea's benchmarks showed:

```
64-thread increment throughput:

AtomicLong:  ~5 million ops/sec (collapsed!)
LongAdder:   ~500 million ops/sec (100x faster!)
```

**That's why modern metrics libraries (Micrometer, Dropwizard) use `LongAdder` by default!**

---

# Part 5: `VarHandle` and Field Updaters — The Underlying Machinery

## 5.1 The Two Problems They Solve

### Problem 1: Wrapper Overhead

```java
class CacheEntry {
    AtomicLong hits;    // Extra object allocation!
    AtomicLong misses;  // Extra object allocation!
}

// With 10 million cache entries:
// 10M * 2 * 16 bytes = 320 MB just for AtomicLong wrappers!
```

### Problem 2: Fine-Grained Memory Control

Sometimes you need more control than `volatile` gives you:

```java
volatile long value;  // Full sequential consistency (expensive!)

// But what if I only need:
// - Acquire semantics on read?
// - Release semantics on write?
// - Plain access in hot loops?
```

---

## 5.2 Field Updaters (Legacy Solution)

```java
class Node {
    volatile long value;  // Must be volatile!
    
    private static final AtomicLongFieldUpdater<Node> VALUE =
        AtomicLongFieldUpdater.newUpdater(Node.class, "value");
    
    boolean tryUpdate(long expected, long next) {
        return VALUE.compareAndSet(this, expected, next);
    }
}
```

**Pros:**
- No wrapper allocation per instance
- CAS goes straight to the raw field

**Cons:**
- String-based field name (typos not caught at compile time)
- Reflection-based (slower initialization)

---

## 5.3 `VarHandle` (Java 9+, The Modern Answer)

```java
class Node {
    volatile long value;
    
    private static final VarHandle VALUE;
    static {
        try {
            VALUE = MethodHandles.lookup()
                .findVarHandle(Node.class, "value", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    boolean tryUpdate(long expected, long next) {
        return VALUE.compareAndSet(this, expected, next);
    }
    
    // Fine-grained memory control:
    long readAcquire()        { return (long) VALUE.getAcquire(this); }
    void writeRelease(long v) { VALUE.setRelease(this, v); }
    long plainRead()          { return (long) VALUE.get(this); }
}
```

---

## 5.4 Memory-Order Modes — What Seniors Get Asked

| Mode | Semantics | Cost | When to Use |
|------|-----------|------|-------------|
| `get` / `set` (plain) | No ordering guarantees | Cheapest | Hot inner loops with your own barriers |
| `getOpaque` / `setOpaque` | Bitwise atomic, no ordering | Cheap | Progress signals |
| `getAcquire` / `setRelease` | One-way barriers | Medium | Handoff patterns |
| `getVolatile` / `setVolatile` | Full sequential consistency | Full fence | Same as `volatile` field |
| `compareAndSet` | Volatile-order CAS | Full fence | Usual CAS |
| `weakCompareAndSet` | May spuriously fail | Cheaper on ARM | Loop-based CAS |

### Visual: Acquire/Release vs Volatile

```
Acquire (one-way barrier):
                    |
[operations above]  |  Can't move below the barrier
                    v
-------- ACQUIRE --------
                    
[operations below]  Can move anywhere


Release (one-way barrier):

[operations above]  Can move anywhere
                    
-------- RELEASE --------
                    ^
[operations below]  |  Can't move above the barrier
                    |


Volatile (two-way barrier):
                    |
[operations above]  |  Can't move below
                    v
-------- VOLATILE --------
                    ^
[operations below]  |  Can't move above
                    |
```

**Acquire/Release is cheaper** because it's a one-way fence. Volatile is a two-way fence.

### Interview One-Liner

> "`VarHandle` gives me the same primitive as `Unsafe.compareAndSwap`, without breaking encapsulation, and lets me choose the memory-order strength I actually need."

---

## 5.5 When to Use VarHandle

**Use VarHandle when:**
- Building custom concurrent data structures
- Reducing per-object memory in high-fanout systems
- Need fine-grained memory ordering control

**Don't use VarHandle when:**
- `AtomicLong` or `LongAdder` would work
- You're writing application code (not library code)

**Rule of thumb:** If you're not writing a data structure, you don't need `VarHandle`. `AtomicLong` and `LongAdder` cover 95% of application code.

---

# Part 6: Production War Stories

## War Story 1: `AtomicLong` Counter That Killed a Service

**The Setup:**
- Billing service with shared `AtomicLong` request counter
- 200 threads on a 64-core box
- Counter incremented on every request

**What Happened:**

```
Under peak load:
- CPU: 80%
- Actual work done: Very little!
- Flame graph: incrementAndGet() at the top

Root cause:
- Cache-line ping-pong
- CAS retry storm
- 199 threads failing CAS, retrying constantly
```

**The Fix:**

```java
// Before
AtomicLong counter = new AtomicLong();
counter.incrementAndGet();

// After
LongAdder counter = new LongAdder();
counter.increment();
```

**Result:** CPU dropped, throughput doubled.

---

## War Story 2: Silent ABA in a Lock-Free Object Pool

**The Setup:**
- In-house connection pool using lock-free stack
- `AtomicReference<Node>` for the stack top
- Connections reused (returned to pool after use)

**What Happened:**

```
Under load, connections started leaking one by one.

Root cause:
1. Thread A starts pop(), reads top = Node(conn1)
2. Thread A preempted
3. Thread B pops conn1, uses it, returns it (push)
4. Thread B pops conn1 again, uses it, returns it (push)
5. Thread A wakes up, CAS succeeds (top is still Node(conn1))
6. But conn1.next now points to wrong node!

Stack corrupted. Connections leaked.
```

**The Fix:**

```java
// Before
AtomicReference<Node> top = new AtomicReference<>();

// After
AtomicStampedReference<Node> top = new AtomicStampedReference<>(null, 0);
```

Also stopped reusing Node objects.

---

## War Story 3: Lambda Side Effects in `updateAndGet`

**The Setup:**

```java
counter.updateAndGet(cur -> {
    logger.info("Adjusting to " + (cur + 1));  // SIDE EFFECT!
    return cur + 1;
});
```

**What Happened:**

```
Under contention:
- Lambda called 3-5 times per successful update (retries!)
- Log volume tripled
- Log shipper backpressured
- Unrelated latency spiked
```

**The Fix:**

```java
// Lambda must be PURE
int result = counter.updateAndGet(cur -> cur + 1);
logger.info("Adjusted to " + result);  // Log OUTSIDE
```

---

## War Story 4: `VarHandle` Memory Savings

**The Setup:**
- Hot cache with 20 million entries
- Each entry had `AtomicLong hits, misses, evictions`
- 60 million `AtomicLong` objects!

**The Problem:**

```
60M AtomicLong objects * ~16 bytes each = ~1 GB overhead!
Plus object headers, alignment, GC pressure...
Total overhead: ~2 GB
```

**The Fix:**

```java
// Before
class CacheEntry {
    AtomicLong hits = new AtomicLong();
    AtomicLong misses = new AtomicLong();
    AtomicLong evictions = new AtomicLong();
}

// After
class CacheEntry {
    volatile long hits;
    volatile long misses;
    volatile long evictions;
    
    private static final VarHandle HITS = ...;
    private static final VarHandle MISSES = ...;
    private static final VarHandle EVICTIONS = ...;
    
    void recordHit() {
        HITS.getAndAdd(this, 1L);
    }
}
```

**Result:** Memory dropped from ~2 GB to ~500 MB.

---

# Part 7: Interview Traps

| Question | Bad Answer | L5 Answer |
|----------|-----------|-----------|
| "What is CAS?" | "A lock." | "A single atomic CPU instruction (x86 `LOCK CMPXCHG`) that swaps if value equals expected. No OS involved." |
| "Why is CAS fast?" | "It just is." | "No syscall, no context switch, no queue. Fails cheaply and lets caller retry. Contention degrades to cache-line ping-pong though." |
| "What is ABA?" | "Something recycled." | "CAS validates value equality, not history. A->B->A satisfies the check while state silently changed. Fix: version stamp." |
| "Why prefer `LongAdder`?" | "It's faster." | "Striped cells eliminate cache-line contention under many-writer workloads. Tradeoff: `sum()` isn't a consistent snapshot." |
| "`AtomicReference<Config>` vs `volatile Config`?" | "Same thing." | "`volatile` = plain publication. `AtomicReference` adds CAS for atomic swap-on-condition. Use volatile if you only need last-writer-wins." |
| "Can I put I/O in a CAS loop?" | "Yeah, why not." | "No — retries are unbounded. Restructure so the CAS body is pure and cheap." |
| "`VarHandle.setRelease` vs `setVolatile`?" | "Volatile is stronger." | "Release stops earlier stores from reordering below it (one-way). Volatile is full sequential consistency (two-way). Release is cheaper on ARM." |
| "Lock-free vs wait-free?" | "Same thing." | "Lock-free: system makes progress, individual threads may retry forever. Wait-free: every thread finishes in bounded steps." |

---

# Part 8: Self-Check Questions

Answer these aloud before your interview:

1. **What CPU instruction backs CAS on x86? ARM?**
   - x86: `LOCK CMPXCHG`
   - ARM: `LDREX`/`STREX` or `CASAL`

2. **Write the CAS-loop increment from scratch:**
   ```java
   while (true) {
       int cur = counter.get();
       if (counter.compareAndSet(cur, cur + 1)) return;
   }
   ```

3. **Lock-free vs wait-free — one sentence each:**
   - Lock-free: System makes progress, individual threads may retry forever
   - Wait-free: Every thread completes in bounded steps

4. **Give a concrete code path where ABA causes a bug:**
   - Lock-free stack pop with node reuse
   - Thread reads top=A, preempted, A popped and repushed, CAS succeeds with stale next pointer

5. **Two fixes for ABA — when to pick which:**
   - `AtomicStampedReference`: When you need full version history
   - `AtomicMarkableReference`: When you only need one-bit state (e.g., "deleted")

6. **When does `AtomicLong` beat `LongAdder`?**
   - When you need exact, monotonic values (ID generators, sequence numbers)
   - When reads are frequent and must be consistent

7. **Why is `LongAdder.sum()` not a consistent snapshot?**
   - Walks cells one by one
   - Other threads may update cells during the walk

8. **`VarHandle` `getAcquire` vs `getVolatile`:**
   - `getAcquire`: One-way barrier, cheaper, prevents reordering below
   - `getVolatile`: Two-way barrier, full sequential consistency

9. **Three cases where CAS is the wrong tool:**
   - Long critical sections
   - Multiple correlated fields
   - I/O inside the update

10. **What invariants must the lambda in `updateAndGet(fn)` obey?**
    - Must be pure (no side effects)
    - Must be cheap (may be called multiple times)
    - No I/O, no logging, no external state changes

---

# Summary: The Complete Mental Model

```
+=========================================================================+
|                    MODULE 3: LOCK-FREE & ATOMICS                         |
+=========================================================================+
|                                                                          |
|  CAS (Compare-And-Swap)                                                 |
|  +--------------------------------------------------------------------+ |
|  |  CPU instruction: x86 LOCK CMPXCHG, ARM LDREX/STREX               | |
|  |  Pattern: read -> compute -> CAS -> retry if failed               | |
|  |  Fast: no syscall, no context switch                              | |
|  |  Degrades under contention: cache-line ping-pong, retry storm     | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  ABA Problem                                                            |
|  +--------------------------------------------------------------------+ |
|  |  CAS checks value equality, not history                           | |
|  |  A -> B -> A looks unchanged to CAS                               | |
|  |  Fix: AtomicStampedReference (version stamp)                      | |
|  |  In Java with GC: rare (live refs keep objects alive)             | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  Atomic Wrappers                                                        |
|  +--------------------------------------------------------------------+ |
|  |  AtomicInteger/Long/Boolean/Reference: single-value CAS           | |
|  |  volatile: just visibility, no CAS                                | |
|  |  AtomicReference: when you need compareAndSet on references       | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  High-Contention Counters                                               |
|  +--------------------------------------------------------------------+ |
|  |  AtomicLong: collapses under contention (ping-pong + retry storm) | |
|  |  LongAdder: striped cells, each thread has own cell               | |
|  |  Tradeoff: sum() is not a consistent snapshot                     | |
|  |  Use LongAdder for metrics, AtomicLong for IDs                    | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  VarHandle                                                              |
|  +--------------------------------------------------------------------+ |
|  |  CAS on raw fields (no wrapper allocation)                        | |
|  |  Fine-grained memory ordering: plain, opaque, acquire/release     | |
|  |  Use for: custom data structures, memory optimization             | |
|  |  Don't use for: application code (AtomicLong is fine)             | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
+=========================================================================+
```

---

**You've completed Module 3!** You now understand CAS at the CPU level, the ABA problem, when to use `LongAdder` vs `AtomicLong`, and the power of `VarHandle`.

**Next up:** [Module 4 — Machine Coding Pack](./Module-04-Machine-Coding-Explained.md)
