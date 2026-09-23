# Module 3 — Lock-Free Programming & Atomic Internals

> **Goal:** You understand *what CAS actually is at the CPU level*, why `AtomicLong` collapses under contention, why `LongAdder` doesn't, and when a `VarHandle` beats an atomic wrapper. Then explain the ABA problem with a real code snippet.

---

## 🗺️ Mind Map

```
                    ┌──────────────────────────────────┐
                    │         MODULE 3 CORE            │
                    └──────────────────────────────────┘
                                   │
       ┌───────────────┬───────────┼───────────┬─────────────┐
       │               │           │           │             │
     3.1 CAS        3.2 ABA     3.3 Atomic  3.4 Striped   3.5 VarHandle
     (cmpxchg)     Problem     wrappers    counters      / Field
       │             │            │        (LongAdder)   Updaters
   ┌───┼───┐     ┌───┼───┐    ┌───┼───┐        │             │
   │       │     │       │    Int Long Ref    Cell[]        cheap
  loop  fail-  root  Stamped/                stripes       memory
  retry retry  cause Markable
```

---

## 3.1 Compare-And-Swap (CAS) — The Foundation

### Real-world analogy

> Imagine the **whiteboard scoreboard** in a coffee shop. Two baristas want to update the same number.
>
> **Locked way:** Barista A yells "MINE!", grabs the marker, updates, yells "DONE!", drops the marker. Barista B waits.
>
> **CAS way:** Barista A reads the current number (say, 7), computes the new one (8), then walks up and says: *"If the board still says 7, change it to 8."* If it does — done. If it says 9 (because B beat A to it) — A recalculates and tries again. Nobody blocks. Nobody waits.

That "if the board still says X, change it to Y" is Compare-And-Swap.

### The CPU instruction

Modern CPUs expose a single **atomic** instruction that:
1. Reads a memory location.
2. Compares it to an *expected* value.
3. If equal, writes a *new* value.
4. Returns whether the swap happened.

- x86: `LOCK CMPXCHG`
- ARM: `LDREX` + `STREX` (load-exclusive / store-exclusive pair) or `CASAL` (ARMv8.1+)
- RISC-V: `LR` + `SC`

The `LOCK` prefix on x86 asserts a **cache-line lock** on that address (fast — no bus lock on modern CPUs), giving true atomicity across cores. **No OS involvement**, no context switch, no kernel/user crossing. That's why lock-free is fast.

### The CAS loop pattern (write this from memory)

```java
private final AtomicInteger counter = new AtomicInteger();

// Naive lock-free increment:
public void increment() {
    while (true) {
        int current = counter.get();
        int next = current + 1;
        if (counter.compareAndSet(current, next)) {
            return;                // success
        }
        // else: someone beat us; loop and retry
    }
}
```

`incrementAndGet()` does exactly this internally. The pattern generalizes:

```java
// Lock-free "update by function":
public int updateAndGet(IntUnaryOperator fn) {
    int prev, next;
    do {
        prev = get();
        next = fn.applyAsInt(prev);
    } while (!compareAndSet(prev, next));
    return next;
}
```

This is the shape of every lock-free algorithm in the JDK.

### Why lock-free ≠ wait-free

- **Lock-free** — the system as a whole makes progress. Some individual thread may retry forever.
- **Wait-free** — every thread makes progress in a bounded number of steps.

Real production code (including `AtomicLong`, `ConcurrentHashMap`) is lock-free, not wait-free. A single thread under heavy contention can loop *many* times before winning a CAS.

### Optimistic vs pessimistic concurrency (interview one-liner)

- **Pessimistic** = *"assume conflict, take a lock first."*
- **Optimistic** = *"assume no conflict, try, verify, retry on collision."* CAS is optimistic.

Optimistic wins when conflicts are rare. Under sustained contention it degenerates to a **CAS spin storm** — high CPU, low throughput. We'll see the fix in 3.4.

### When CAS is a bad choice

| Scenario | Why CAS is wrong |
|---|---|
| Long critical section | You'd be looping forever; take a lock. |
| Multiple correlated fields | CAS is one word at a time. Need a lock, or wrap in an immutable object + one `AtomicReference`. |
| Blocking I/O inside the update | Never do I/O in a CAS loop; retry cost is unbounded. |
| Very high write contention | Retry storms burn CPU. Reach for `LongAdder` or striping. |

---

## 3.2 The ABA Problem

### The trap

CAS asks *"is the value still what I saw?"* It **does not** ask *"has this memory been touched since I looked?"* If a value goes `A → B → A`, CAS thinks nothing changed. Everything you assumed about "state between" is now wrong.

### The concrete example — a lock-free stack that breaks

```java
class UnsafeStack<T> {
    private final AtomicReference<Node<T>> top = new AtomicReference<>();

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

**Attack scenario:**
1. Thread T1 calls `pop()`, reads `top = A`, computes `newTop = A.next = B`. **Preempted.**
2. Thread T2 pops `A` (now recycled), pops `B`, pushes `A` back. Stack is now `A → C`.
3. T1 resumes. CAS: "is `top` still `A`?" → yes. So T1 sets `top = B`. **B is no longer on the stack.** Corruption.

The value was A → B → A. CAS was satisfied. State was silently mutated.

### Where ABA actually bites in production

- Free-list / object-pool implementations where nodes get recycled.
- Lock-free memory allocators.
- Pointer-based concurrent data structures (stacks, queues) with reuse.
- Any protocol that assumes "if the token matches, nothing meaningful happened in between."

### The fixes

**Fix 1 — Version stamp (`AtomicStampedReference`)**

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
```

Every mutation bumps a version integer. Even if the pointer cycles back to A, the stamp differs, so CAS fails and we retry.

**Fix 2 — `AtomicMarkableReference`**
One `boolean` mark instead of a full version stamp. Enough when you only need to signal one logical state change (e.g., "this node is being deleted"). Common in lock-free linked lists.

**Fix 3 — Don't recycle memory**
The JVM has a garbage collector, so pure Java ABA in *reference* CAS is rarer than in C/C++ — the GC prevents an object from being freed while a thread still has a reference. But if you use an **object pool** (or `sun.misc.Unsafe` with raw memory), ABA is back.

**Fix 4 — Use immutable snapshots**
If your CAS updates an entire immutable state object, the whole "A came back" concern usually disappears — a new instance means a new identity.

### Interview one-liner

> "In pointer-CAS on reusable memory, ABA is the case where the value cycles back and CAS falsely succeeds. Fix with a version stamp (`AtomicStampedReference`) or by avoiding memory reuse. In pure-Java reference CAS with GC, ABA is rare because a live reference keeps the old object alive."

---

## 3.3 The Atomic Wrapper Family

### The core wrappers

| Class | State | Common ops |
|---|---|---|
| `AtomicInteger` | `int` | `incrementAndGet`, `getAndAdd`, `compareAndSet`, `updateAndGet` |
| `AtomicLong` | `long` | same |
| `AtomicBoolean` | boolean | `compareAndSet`, `getAndSet` |
| `AtomicReference<V>` | object ref | `compareAndSet`, `updateAndGet(fn)` |
| `AtomicIntegerArray` / `AtomicLongArray` / `AtomicReferenceArray<V>` | array with per-index atomicity | `getAndAdd(i, x)`, `compareAndSet(i, exp, new)` |
| `AtomicStampedReference<V>` | ref + int version | ABA-safe |
| `AtomicMarkableReference<V>` | ref + boolean mark | ABA-safe with a single-bit flag |

### The mental model

An `AtomicX` wrapper is:
- One `volatile` field carrying the value.
- A CAS primitive (`VarHandle`/`Unsafe` under the hood) exposing atomic RMW.
- Nothing more — no lock, no queue, no OS state.

That's why an `AtomicInteger` is ~16 bytes and (uncontended) is nearly as fast as a plain `int` read.

### When to use which — decision tree

```
Need atomicity on a single value?
├── Just visibility? ──────────────────► volatile field
├── Single-var RMW (counter, flag)? ───► AtomicInteger / AtomicBoolean
├── Reference swap (config, cache)? ───► AtomicReference<Immutable>
├── ABA risk with pointer reuse? ──────► AtomicStampedReference
├── High-contention counter? ──────────► LongAdder / LongAccumulator (see 3.4)
├── Multiple correlated fields? ───────► Immutable object + AtomicReference (swap the whole thing)
└── Need blocking / waiting? ──────────► Use a lock; CAS is the wrong tool
```

### `updateAndGet` / `accumulateAndGet` — the modern lock-free style

You almost never write the raw CAS loop anymore. Since Java 8:

```java
AtomicInteger cap = new AtomicInteger(100);

// Cap at 200, floor at 0, decrement by n:
int result = cap.updateAndGet(cur -> Math.max(0, cur - 10));

// Two-arg combine:
AtomicLong max = new AtomicLong(Long.MIN_VALUE);
max.accumulateAndGet(newValue, Math::max);
```

The JVM inlines the CAS loop. **Cleaner, and it's what interviewers expect** as your first cut.

⚠️ **Watch:** the lambda must be **pure** — no side effects, no I/O. It can be called many times because of retries.

---

## 3.4 High-Contention Counters — `LongAdder` & `LongAccumulator`

### The problem `LongAdder` exists to solve

Take an `AtomicLong` counter incremented by 64 threads at once. Every thread:
1. Reads the counter.
2. Computes +1.
3. CAS.

Only **one** wins per round. 63 fail and retry. Meanwhile every CAS **invalidates the cache line** on every other core. This is **cache-line ping-pong**, plus **CAS retry storm**, and throughput collapses.

### The `LongAdder` idea — striping

Instead of one hot memory location, keep an array of `Cell`s (each on its own cache line). Each thread hashes to a Cell and adds to *its own* Cell — different cache lines, no contention. `sum()` walks the array and adds everything up.

```
         AtomicLong                              LongAdder
     ┌───────────────┐                    ┌────┬────┬────┬────┐
     │   value: 12   │◄── all 64 ──►      │ 3  │ 5  │ 1  │ 3  │ ◄── each thread
     └───────────────┘   threads          └────┴────┴────┴────┘     hits its own cell
     cache line ping-pong                  independent cache lines
```

### API

```java
LongAdder counter = new LongAdder();

counter.increment();          // hot path: cell.add(1)
counter.add(42);
long total = counter.sum();   // walks cells, sums them — not "instantaneous"
counter.reset();              // best-effort reset
```

### `AtomicLong` vs `LongAdder` — pick the right one

| Metric | `AtomicLong` | `LongAdder` |
|---|---|---|
| Memory footprint | ~16 B | ~16 B + up to `N * 64 B` cells |
| Low-contention writes | Fast | Fast (falls back to a base value) |
| High-contention writes | **Collapses** | Scales near-linearly |
| Reads (`sum` / `get`) | O(1) exact | O(cells), **not** a consistent snapshot under concurrent writes |
| Best for | Sequence generators, IDs, rate limits (need exact monotone value) | Metrics counters, event totals, throughput stats |

### The trap in reads

`LongAdder.sum()` is **not** an atomic snapshot. Writes racing with `sum()` may or may not be included. If you need an exact instantaneous value (say, an ID generator), stick with `AtomicLong`. If you're counting requests per second, `LongAdder` is what you want.

### `LongAccumulator` — the generalized cousin

Same striped-cell idea but with a user-supplied binary function:

```java
// Track the maximum observed request latency without lock contention:
LongAccumulator maxLatency = new LongAccumulator(Math::max, Long.MIN_VALUE);
maxLatency.accumulate(currentSampleNs);
```

Works with any associative, side-effect-free function.

### Real production numbers

Doug Lea's original benchmarks showed 64-thread `AtomicLong.incrementAndGet` throughput collapsing to a fraction of `LongAdder`'s. In modern production metrics libraries (Micrometer, Dropwizard), **counters are `LongAdder`-backed by default** — for exactly this reason.

---

## 3.5 `VarHandle` and Field Updaters — The Underlying Machinery

### The two problems they solve

1. **`AtomicX` wrappers are objects.** If you have 10 million entries and each needs an atomic field, using `AtomicLong` adds 10 million extra allocations, headers, and indirection.
2. **You need finer memory-order control** than `volatile` (acquire/release, opaque, plain) — this is what lock-free algorithm authors use.

### Field updaters (the legacy solution)

```java
class Node {
    volatile long value;   // must be volatile

    private static final AtomicLongFieldUpdater<Node> V =
        AtomicLongFieldUpdater.newUpdater(Node.class, "value");

    boolean tryUpdate(long expected, long next) {
        return V.compareAndSet(this, expected, next);
    }
}
```

No wrapper allocation per instance — the CAS goes straight to the raw `long` field. Cheap, but stringly-typed and reflection-based.

### `VarHandle` (Java 9+, the modern answer)

```java
class Node {
    volatile long value;

    private static final VarHandle V;
    static {
        try {
            V = MethodHandles.lookup().findVarHandle(Node.class, "value", long.class);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }

    boolean tryUpdate(long expected, long next) {
        return V.compareAndSet(this, expected, next);
    }

    long observeAcquire()          { return (long) V.getAcquire(this); }
    void publishRelease(long v)    { V.setRelease(this, v); }
    long plainRead()               { return (long) V.get(this); }
}
```

### Memory-order modes (this is what senior candidates get asked)

| Mode | Semantics | Cost | When |
|---|---|---|---|
| `get` / `set` (plain) | No ordering guarantees | Cheapest | Hot inner loops with your own barriers |
| `getOpaque` / `setOpaque` | Bitwise atomic; no ordering | Cheap | Progress signals |
| `getAcquire` / `setRelease` | One-way barriers (acquire loads, release stores) | Medium | Handoff patterns |
| `getVolatile` / `setVolatile` | Full sequential consistency | Full fence | Same as `volatile` field |
| `compareAndSet` | Volatile-order CAS | Full fence | Usual CAS |
| `weakCompareAndSet` | May spuriously fail | Cheaper on ARM | Loop-based CAS |

**Interview one-liner:** *"`VarHandle` gives me the same primitive as `Unsafe.compareAndSwap`, without breaking encapsulation, and lets me choose the memory-order strength I actually need."*

### When to reach for these

- Custom concurrent data structures (linked lists, ring buffers).
- Reducing per-object memory in high-fanout systems.
- Emulating Java 21 `Atomic*` semantics on custom fields.

If you're not writing a data structure, you don't need `VarHandle`. `AtomicLong` and `LongAdder` cover 95% of application code.

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | Bad answer | L5 answer |
|---|---|---|
| "What is CAS?" | "A lock." | "A single atomic CPU instruction (x86 `LOCK CMPXCHG`) that swaps if the value equals expected. No OS involved." |
| "Why is CAS fast?" | "It just is." | "No syscall, no context switch, no queue. Fails cheaply and lets the caller retry. Contention degrades to cache-line ping-pong though." |
| "What is ABA?" | "Something recycled." | "CAS validates value equality, not history. A→B→A satisfies the check while state silently changed. Fix: version stamp." |
| "Why prefer `LongAdder`?" | "It's faster." | "Striped cells eliminate cache-line contention under many-writer workloads. Tradeoff: `sum()` isn't a consistent snapshot." |
| "`AtomicReference<Config>` vs `volatile Config`?" | "Same thing." | "`volatile` = plain publication. `AtomicReference` adds CAS for atomic swap-on-condition. Use plain volatile if you only need last-writer-wins." |
| "Can I put I/O in a CAS loop?" | "Yeah, why not." | "No — retries are unbounded. Restructure so the CAS body is pure and cheap." |
| "Difference: `VarHandle.setRelease` vs `setVolatile`?" | "Volatile is stronger." | "Release stops earlier stores from reordering below it (one-way). Volatile is full sequential consistency (two-way). Release is cheaper on weakly-ordered CPUs like ARM." |

---

## 💻 L5-Grade Code Examples

### Example 1 — Lock-free bounded counter with saturation

Classic CAS-loop; interviewers use this to test the update-and-verify idiom.

```java
public final class SaturatingCounter {
    private final AtomicLong value = new AtomicLong();
    private final long max;

    public SaturatingCounter(long max) { this.max = max; }

    public boolean tryIncrement() {
        while (true) {
            long cur = value.get();
            if (cur >= max) return false;                     // saturated
            if (value.compareAndSet(cur, cur + 1)) return true;
        }
    }

    public long get() { return value.get(); }
}
```

Talking points:
- No lock, no `synchronized`.
- Bounded work in the loop; no I/O, no allocation.
- `tryIncrement` returns false immediately at cap — no busy-wait.
- Modern form: `value.updateAndGet(v -> v < max ? v + 1 : v)` plus a return-value check.

### Example 2 — ABA-safe versioned reference

```java
public final class VersionedRef<T> {
    private final AtomicStampedReference<T> ref;

    public VersionedRef(T initial) { this.ref = new AtomicStampedReference<>(initial, 0); }

    public T get() { return ref.getReference(); }

    public boolean update(T expected, T next) {
        int[] stampHolder = new int[1];
        while (true) {
            T current = ref.get(stampHolder);
            int stamp  = stampHolder[0];
            if (current != expected) return false;
            if (ref.compareAndSet(current, next, stamp, stamp + 1)) return true;
            // else stamp advanced; retry
        }
    }
}
```

### Example 3 — Metrics counter using `LongAdder` (production shape)

```java
public final class RequestMetrics {
    private final LongAdder total   = new LongAdder();
    private final LongAdder success = new LongAdder();
    private final LongAdder failure = new LongAdder();
    private final LongAccumulator maxLatencyNanos =
        new LongAccumulator(Math::max, Long.MIN_VALUE);

    public void record(long latencyNs, boolean ok) {
        total.increment();
        (ok ? success : failure).increment();
        maxLatencyNanos.accumulate(latencyNs);
    }

    public Snapshot snapshot() {
        // Not a consistent snapshot across counters — acceptable for metrics.
        return new Snapshot(total.sum(), success.sum(), failure.sum(),
                            Math.max(0, maxLatencyNanos.get()));
    }

    public record Snapshot(long total, long success, long failure, long maxLatencyNs) {}
}
```

Points to say aloud:
- Metrics is exactly the workload `LongAdder` was designed for — many writers, occasional reader.
- Snapshot is *not* atomic across the four counters — that's a normal, intentional metrics tradeoff.

### Example 4 — Lock-free linked-list head insert with `VarHandle`

```java
public final class LockFreeQueueHead<T> {
    private static final class Node<T> {
        final T value;
        volatile Node<T> next;
        Node(T v) { this.value = v; }
    }

    private volatile Node<T> head;

    private static final VarHandle HEAD;
    static {
        try {
            HEAD = MethodHandles.lookup()
                .findVarHandle(LockFreeQueueHead.class, "head", Node.class);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }

    public void push(T v) {
        Node<T> node = new Node<>(v);
        Node<T> old;
        do {
            old = head;
            node.next = old;
        } while (!HEAD.compareAndSet(this, old, node));
    }

    public T pop() {
        Node<T> old, next;
        do {
            old = head;
            if (old == null) return null;
            next = old.next;
        } while (!HEAD.compareAndSet(this, old, next));
        return old.value;
    }
}
```

Interview grade points:
- `VarHandle` used instead of `AtomicReference` — no extra wrapper allocation.
- Reads are plain (`head` field is `volatile` for publication).
- Note the ABA hazard: this is safe **only because Java GC keeps popped nodes alive** as long as another thread has a reference. In C++ or with a Node pool, you'd need `AtomicStampedReference`-style versioning.

---

## 🏭 Production War Stories

**1. `AtomicLong` counter that killed a service.**
A billing team used a shared `AtomicLong` request counter across 200 threads on a 64-core box. Under peak load, the box hit 80% CPU while doing very little real work. Flame graph showed the increment call at the top. Root cause: cache-line ping-pong + CAS retry storm. Fix: replaced with `LongAdder`. CPU dropped, throughput doubled.

**2. Silent ABA in a lock-free object pool.**
An in-house pool used a lock-free stack (`AtomicReference<Node>`) for connection reuse. Under load, connections started leaking one-by-one. Root cause: a connection object was popped, returned, then repushed while another thread was mid-pop with a stale `next` pointer. Fix: switched to `AtomicStampedReference` and stopped reusing Node objects.

**3. Lambda side effects in `updateAndGet`.**
A dev put a `logger.info("adjusting to " + newVal)` **inside** the `updateAndGet` lambda. Under contention it logged 3–5 times per successful update because the lambda was retried on CAS failure. Log volume tripled; log-shipper backpressured; unrelated latency spiked. Fix: lambda pure; log outside the loop.

**4. `VarHandle` memory savings on a hot cache.**
A per-key stats structure on a hot cache had `AtomicLong hits, misses, evictions` per entry. On a 20M-entry cache, that meant 60M `AtomicLong` objects — ~2 GB of overhead. Migration to raw `volatile long` + `VarHandle.compareAndSet` cut it to ~500 MB.

---

## 🎯 Self-Check

1. What CPU instruction backs CAS on x86? What about ARM?
2. Write the CAS-loop increment from scratch, no `AtomicInteger.increment`.
3. Difference between lock-free and wait-free — one sentence each.
4. Give a concrete code path where ABA causes a real bug.
5. Two fixes for ABA — when do you pick which?
6. When does `AtomicLong` beat `LongAdder`? Give an actual use case.
7. Why is `LongAdder.sum()` not a consistent snapshot?
8. Explain `VarHandle` `getAcquire` vs `getVolatile` — cost and semantics.
9. Give three cases where CAS is the wrong tool.
10. Read the lambda in `AtomicInteger.updateAndGet(fn)` — what invariants must `fn` obey?

Any hesitation? Re-read that section.

---

## ➡️ Next

Move to **Module 4 — Machine Coding Pack** (the L5 make-or-break: bounded blocking queue, thread pool, LRU, rate limiter, LeetCode concurrency set).
