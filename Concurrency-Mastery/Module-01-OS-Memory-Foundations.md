# Module 1 — OS Architecture, Memory Mechanics & The Java Memory Model

> **Goal:** By the end of this module you can explain — to an interviewer or a junior teammate — *how a Java thread actually runs on hardware*, *why `count++` breaks*, and *when to reach for `volatile` vs a lock*. No hand-waving.

---

## 🗺️ Mind Map (redraw this from memory before moving on)

```
                    ┌─────────────────────────────────┐
                    │        MODULE 1 CORE            │
                    └─────────────────────────────────┘
                                   │
        ┌──────────────────┬───────┴────────┬─────────────────┐
        │                  │                │                 │
    1.1 Process       1.2 IPC          1.3 CPU &         1.4 Java Memory
    vs Thread         Basics           Cache             Model (JMM)
        │                  │                │                 │
   ┌────┼────┐        ┌────┼────┐      ┌────┼────┐      ┌────┼──────┐
   │    │    │        │    │    │      │    │    │      │    │      │
  fork  ctx  arch    pipe shm  MQ    line false reord  HB  vol  final
  COW   sw   choice  UDS       (Kafka) 64B share ering rules
```

---

## 1.1 Process vs Thread — The Foundation

### Real-world analogy (tell this to a junior)

> Think of a **process** as an **entire apartment** — its own address, kitchen, plumbing, front door. Two apartments cannot walk into each other's kitchen.
>
> Think of a **thread** as a **person living inside one apartment**. Multiple people share the kitchen, the fridge, the TV. Cheaper (no separate rent), faster to coordinate — but if two people grab the same knife at the same time, someone gets hurt. That's a race condition.

### The technical version

| Aspect | Process | Thread |
|---|---|---|
| Address space | Isolated (own virtual memory) | Shared within process |
| Creation cost | Expensive (`fork` + page tables) | Cheap (just a stack + registers) |
| Communication | IPC (pipe, socket, shared mem) | Just read/write shared memory |
| Crash blast radius | Contained to that process | Kills the whole process |
| Context switch cost | ~1–10 µs (TLB flush) | ~100 ns – 1 µs |

### `fork()` and Copy-On-Write (COW) — the trick that made Unix fast

When you call `fork()`, the kernel does **not** actually copy the parent's memory. It does two clever things:

1. Creates a **new page table** pointing to the **same physical pages**.
2. Marks every page **read-only** in both parent and child.

The first time either side **writes**, the CPU throws a page fault → kernel copies *that one page* → both sides continue. That's Copy-On-Write.

**Why an L5 must know this:**
- Redis's background snapshot (`BGSAVE`) uses `fork()` + COW to snapshot RAM without stopping the main thread.
- If your Redis write load is heavy during `BGSAVE`, memory can nearly double (every page gets copied) → OOM. Real production incident.

### Multi-process vs Multi-threaded architectures

| Architecture | Model | Why they chose it |
|---|---|---|
| **Nginx** | 1 master + N worker **processes** | If one worker crashes, others survive. Zero-downtime reloads via graceful process swap. |
| **PostgreSQL** | 1 backend **process per connection** | Isolation. Bad query in one connection can't corrupt another. But: high mem per conn → why you need PgBouncer. |
| **Redis** (single-node) | 1 main **thread** (event loop) | Data structures need no locks. Simplicity + cache-friendly. |
| **JVM apps** | 1 process, many **threads** | Cheap concurrency, shared heap, shared class metadata. |
| **Envoy / Netty** | 1 process, few **event-loop threads** | High throughput, low latency, non-blocking I/O. |
| **Chrome** | Process **per tab** | Security sandbox + one crashed tab doesn't kill the browser. |

**Junior confusion cleared:** "Why don't we just always use threads — they're faster?" Because **isolation is a feature**. In systems where a crash or memory corruption in one unit must not affect others (browser tabs, DB connections, Nginx workers), processes are the *correct* choice, even at higher cost.

### Context switch cost — what actually happens

When the OS switches from Thread A to Thread B:

1. **Save A's CPU registers** to A's kernel stack (~15+ registers on x86-64).
2. **Kernel/user mode transition** (syscall boundary).
3. **Load B's registers** back.
4. If B is in a **different process**: reload page-table base register → **TLB flush**.
5. B starts running with a **cold L1/L2 cache** (the "working set" changed).

Steps 4–5 are why **process context switches cost ~10× thread context switches**. And the *hidden* cost — cache misses on the first thousand instructions after resume — often dwarfs the switch itself.

**Production scenario (real):**
> A team saw p99 latency spike from 20ms → 300ms after adding a metrics-scraping thread that woke every 100ms. Cause: the scraper thread's wakeup evicted the hot code cache. Fix: pinned the hot serving thread to specific cores with `taskset`, isolated the scraper to a different core.

---

## 1.2 IPC — When Threads Aren't an Option

Only enough to talk about it competently. Don't over-index.

### The five mechanisms

| Mechanism | Use case | 1-line intuition |
|---|---|---|
| **Pipe (\|)** | Parent ↔ child streaming | Unidirectional byte stream, kernel buffer |
| **Named pipe (FIFO)** | Unrelated processes on same host | Same, but has a filesystem name |
| **Unix Domain Socket** | High-perf same-host RPC (Docker, Postgres) | Like TCP but no network stack overhead |
| **Shared memory (`mmap`, `shm_open`)** | Zero-copy, high-throughput data (video, DB caches) | Two processes map the **same physical pages** |
| **Message Queue (POSIX, kernel)** | Bounded, prioritized messages | Rarely used directly today; Kafka/RabbitMQ replaced it |

### Where devs get confused

> "Isn't Kafka an IPC mechanism?" — **No.** Kafka is a *distributed* message broker. IPC in the OS sense means processes on the **same host**. Same idea, different scale.

> "TCP loopback vs Unix Domain Socket — does it matter?" — **Yes.** UDS skips the entire TCP stack (no checksums, no window sizing, no packetization). Postgres, Docker daemon, and Envoy all default to UDS on localhost. Measured savings: ~30-50% latency on small messages.

### L5 must-know: shared memory model

```
Process A                         Process B
┌────────────────┐                ┌────────────────┐
│ virt addr 0x40 │────┐      ┌────│ virt addr 0x80 │
└────────────────┘    │      │    └────────────────┘
                      ▼      ▼
                ┌─────────────────┐
                │  Physical Page  │  ← same RAM
                │   (shared)      │
                └─────────────────┘
```

Both processes' virtual addresses point to the **same physical page**. Writes are visible **instantly** — no copy, no syscall. But now you need **your own synchronization** (locks, atomics) since the kernel isn't mediating access. This is exactly like multi-threading, just across process boundaries.

---

## 1.3 CPU & Cache — Why Your Code Is Slower Than It Should Be

### The memory hierarchy (memorize the order of magnitude)

```
Register           ~ 0.3 ns    (1 cycle)
L1 cache           ~ 1   ns    (4 cycles,   ~32 KB)
L2 cache           ~ 4   ns    (12 cycles,  ~256 KB–1 MB)
L3 cache           ~ 12  ns    (40 cycles,  ~8–64 MB, shared across cores)
Main memory (RAM)  ~ 100 ns    (300 cycles)
NVMe SSD           ~ 100 µs    (1,000× slower than RAM)
Network (same DC)  ~ 500 µs
```

**L5 mental model:** If a cache miss to RAM costs you 100 ns, and one clock cycle is ~0.3 ns, then **one miss = ~300 wasted cycles**. Everything about lock-free programming is about *not missing the cache*.

### Cache lines (this is the whole game)

The CPU does not read one byte at a time. It reads **64 bytes at a time** into a **cache line**. If you read `array[0]`, the CPU also pulls `array[1..15]` (assuming 4-byte ints). This is why sequential array iteration is 10× faster than random pointer chasing.

### False sharing — the L5 favorite

**The setup:** Two threads updating two different variables. No shared data. Should be perfectly parallel. But it isn't. Why?

```java
class Counters {
    long a;   // thread 1 writes this
    long b;   // thread 2 writes this
}
```

`a` and `b` are 8 bytes each, sitting **on the same 64-byte cache line**. When Thread 1 writes `a`, the CPU cache coherency protocol **invalidates that cache line in Thread 2's core**. Thread 2 then re-reads the line from L3/RAM to touch `b`. Ping. Pong. Ping. Pong. **10× slowdown, zero contention on the actual data.**

### The fix

**Option 1 — manual padding** (old-school):
```java
class PaddedCounter {
    long p1, p2, p3, p4, p5, p6, p7;  // 56 bytes of padding before
    volatile long value;
    long q1, q2, q3, q4, q5, q6, q7;  // 56 bytes after (guards next line)
}
```

**Option 2 — `@Contended`** (JDK 8+, use `-XX:-RestrictContended`):
```java
import jdk.internal.vm.annotation.Contended;

class Counter {
    @Contended
    volatile long value;   // JVM pads this to its own cache line
}
```

**How you'd know in production:**
- Symptoms: threads doing "nothing shared" show high CPU but low throughput.
- Diagnosis: `perf c2c` on Linux, or async-profiler with `--event cache-misses`.
- Real case: `LongAdder`'s `Cell[]` array exists **precisely** because `AtomicLong` false-shares under high contention. We'll cover this in Module 3.

### Memory reordering — the "wait, that shouldn't be possible" moment

Both the **compiler** and the **CPU** are allowed to reorder your instructions as long as *single-threaded* behavior is preserved. Multi-threaded programs pay the price.

**Classic example (this actually happens):**
```java
// Shared:
int x = 0, y = 0;
int a = 0, b = 0;

// Thread 1        Thread 2
x = 1;             y = 1;
a = y;             b = x;
```

After both threads finish, you would expect `(a, b)` to be one of `(0, 1)`, `(1, 0)`, or `(1, 1)`. But on real hardware (x86 included), you can also observe `(0, 0)` — because each CPU has a **store buffer**, and the store `x = 1` may not be globally visible when Thread 1 reads `y`.

**The fix in Java:** declare `x` and `y` as `volatile`. That inserts memory barriers that prevent reordering across the read/write.

> **Junior mistake:** "I'll just use `volatile` on everything and I'm safe." Wrong. `volatile` gives you *visibility + ordering*, **not atomicity**. `count++` on a `volatile int` is still broken because `++` is read-modify-write.

---

## 1.4 Java Memory Model (JMM) — The Rules That Actually Govern You

The JMM is a **contract** between you and the JVM: "If you follow rule X, I guarantee memory outcome Y." It exists because different CPUs (x86, ARM, POWER) have wildly different memory-ordering guarantees, and Java needs to run identically on all of them.

### The Happens-Before (HB) relation — the ONE thing to memorize

If action A **happens-before** action B, then:
- All memory writes made by A are **visible** to B.
- The compiler/CPU may not reorder them past each other.

**HB rules you must know:**

1. **Program order** — within one thread, earlier statements HB later ones.
2. **Monitor lock** — `unlock` HB every subsequent `lock` on the same monitor.
3. **`volatile`** — write to a `volatile` field HB every subsequent read of that field.
4. **`Thread.start()`** — the call HB the first action of the new thread.
5. **`Thread.join()`** — the last action of the joined thread HB the return of `join`.
6. **`final` fields** — safe publication: once a constructor finishes, any thread that sees the reference sees the correctly-initialized `final` fields.
7. **Transitivity** — if A HB B and B HB C, then A HB C.

### The `volatile` guarantee, precisely

`volatile` guarantees:
- **Visibility** — a write is immediately visible to other threads.
- **Ordering** — no reordering across the volatile access.
- **Atomicity of the single read/write** — but only for that one operation.

It does **NOT** give you:
- Atomicity of `x++` (read, add, write — 3 ops, not 1).
- Any form of "lock."
- Compound conditional updates ("if x < 10 then x++").

### The `count++` trap (the interview classic)

```java
volatile int counter = 0;   // still broken

// 100 threads run:
for (int i = 0; i < 1000; i++) counter++;

// Expected: 100000. Actual: ~65432, non-deterministic.
```

**Why:** `counter++` compiles to:
```
1. tmp = counter        // read
2. tmp = tmp + 1        // modify
3. counter = tmp        // write
```

Two threads can both read `counter = 42`, both write `43`. One increment lost.

**Fixes (ranked by preference):**
1. `AtomicInteger` — CAS-based, lock-free.
2. `LongAdder` — high contention, striped counters.
3. `synchronized` block — heavier but simple.

We'll code all of these in Modules 2 and 3.

### Safe publication — the "worked in single-thread, broke in multi-thread" trap

```java
class Config {
    int timeout;
    String url;
    Config() { timeout = 5000; url = "https://…"; }
}

// Thread 1
config = new Config();      // A

// Thread 2 (unrelated thread)
if (config != null) {
    System.out.println(config.timeout);   // could see 0!
}
```

**Why:** The write to `config` (assigning the reference) can be reordered *before* the constructor's stores to `timeout` and `url`. Thread 2 sees the reference but not the fields.

**Safe publication idioms (any of these work):**
- Assign to a `volatile` reference.
- Use `final` fields (JMM guarantees they're visible once the constructor returns).
- Publish via a `synchronized` block.
- Publish through a `java.util.concurrent` collection (they have internal barriers).
- Static initializer (JVM guarantees ordering).

### `final` — the free lunch

```java
class ImmutablePoint {
    final int x, y;
    ImmutablePoint(int x, int y) { this.x = x; this.y = y; }
}
```

Once construction is finished, any thread that gets a reference to this object sees `x` and `y` correctly. No `volatile`, no lock. This is why immutable classes are the concurrency dev's best friend.

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | The Bad Answer | The Good Answer |
|---|---|---|
| "What does `volatile` do?" | "Makes it thread-safe." | "Gives visibility + ordering, not atomicity. `count++` still races." |
| "Isn't x86 strongly ordered? Why do we need `volatile`?" | "I dunno, JVM abstraction." | "Compiler still reorders. Also JMM must work on ARM/POWER which are weakly ordered." |
| "How would you fix false sharing?" | "Add a lock." | "Cache-line padding or `@Contended`. Adding a lock makes it *worse*." |
| "Why does Redis use one thread?" | "It's simpler." | "Data structures are lock-free; single-threaded event loop keeps L1 cache hot; scale by sharding across processes." |
| "What guarantees does `final` give?" | "Can't reassign." | "Also: safe publication under the JMM — no `volatile`/lock needed once the constructor completes normally." |

---

## 💻 L5-Grade Code Examples

### Example 1 — Safe publication with `final`

```java
public final class ImmutableConfig {
    private final int timeoutMs;
    private final String endpoint;
    private final Map<String, String> headers;

    public ImmutableConfig(int timeoutMs, String endpoint, Map<String, String> headers) {
        this.timeoutMs = timeoutMs;
        this.endpoint = endpoint;
        this.headers = Map.copyOf(headers);   // defensive copy; also immutable
    }

    public int timeoutMs()             { return timeoutMs; }
    public String endpoint()           { return endpoint; }
    public Map<String,String> headers(){ return headers; }
}

// Shared, hot-swappable config:
public class ConfigHolder {
    private volatile ImmutableConfig current;   // volatile ref, immutable content

    public ImmutableConfig get()               { return current; }
    public void update(ImmutableConfig next)   { this.current = next; }
}
```

Why this is L5-clean:
- `final` fields ⇒ safe publication of the object.
- `volatile` reference ⇒ visibility of *replacement*.
- Readers never lock. Writers just swap the reference.

### Example 2 — Demonstrating false sharing

```java
public class FalseSharingDemo {
    static final int ITERATIONS = 100_000_000;

    static class Naive {
        volatile long a;
        volatile long b;
    }

    static class Padded {
        @jdk.internal.vm.annotation.Contended volatile long a;
        @jdk.internal.vm.annotation.Contended volatile long b;
    }

    public static void main(String[] args) throws Exception {
        run(new Naive() {}, "naive");
        run(new Padded() {}, "padded");
    }

    static void run(Object holder, String label) throws Exception {
        var f1 = holder.getClass().getDeclaredField("a");
        var f2 = holder.getClass().getDeclaredField("b");
        f1.setAccessible(true); f2.setAccessible(true);

        long start = System.nanoTime();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                try { f1.setLong(holder, i); } catch (Exception ignored) {}
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                try { f2.setLong(holder, i); } catch (Exception ignored) {}
            }
        });
        t1.start(); t2.start(); t1.join(); t2.join();
        System.out.printf("%s: %d ms%n", label, (System.nanoTime() - start) / 1_000_000);
    }
}
// Run with:  --add-opens java.base/jdk.internal.vm.annotation=ALL-UNNAMED
// Typical result: naive is 3–6× slower than padded on a multi-core machine.
```

### Example 3 — Reordering caught in the wild (double-checked locking done right)

```java
public class LazySingleton {
    // volatile is the fix for the classic broken DCL pattern.
    private static volatile LazySingleton INSTANCE;

    public static LazySingleton get() {
        LazySingleton local = INSTANCE;          // read once
        if (local == null) {
            synchronized (LazySingleton.class) {
                local = INSTANCE;
                if (local == null) {
                    local = new LazySingleton(); // constructor stores must publish before assignment
                    INSTANCE = local;
                }
            }
        }
        return local;
    }
}
```

Without `volatile`, another thread could see `INSTANCE != null` while the constructor's writes to inner fields have not yet been made visible → NPE or partial-object access. The `volatile` inserts a barrier that publishes the constructor's writes atomically with the reference assignment.

Modern preferred alternative — the **Initialization-on-Demand Holder** (no `volatile` needed, JMM guarantees class-init ordering):
```java
public class LazySingleton {
    private LazySingleton() {}
    private static class Holder { static final LazySingleton INSTANCE = new LazySingleton(); }
    public static LazySingleton get() { return Holder.INSTANCE; }
}
```

---

## 🏭 Production War Stories (tell these in interviews)

**1. The Redis fork storm.**
Team ran `BGSAVE` on a 40 GB Redis instance with heavy writes. `fork()` was cheap, but under write load, COW copied nearly the entire heap → RSS doubled → OOM-killer took Redis down. Fix: schedule `BGSAVE` during traffic troughs; enable `vm.overcommit_memory=1`.

**2. The Postgres connection blowup.**
Service opened 5,000 direct connections. Each is a *process* on the DB (\~10 MB RSS). DB host swapped, then crashed. Fix: PgBouncer transaction-pooling → 5,000 clients on 50 real backend processes.

**3. The metrics thread that killed p99.**
A "harmless" scraper thread waking every 100 ms was evicting the hot code cache from L1, spiking p99 from 20 ms to 300 ms on latency-critical serving. Fix: pinned the serving thread with `taskset -c 2,3` and the scraper to another core.

**4. The false-sharing counter.**
A metrics library used adjacent `volatile long` fields in a shared struct. Under load, throughput plateaued at 30% of theoretical. `perf c2c` showed cache-line ping-pong. Replacing with `LongAdder` → 4× throughput.

---

## 🎯 Self-Check (say the answers aloud)

1. What does `fork()` actually copy, and when?
2. Why is a process context switch more expensive than a thread context switch?
3. What is a cache line and why is 64 bytes the magic number?
4. Give a real code snippet where `volatile` is *not enough*, and explain why.
5. State the 7 happens-before rules.
6. Why can `(a, b) = (0, 0)` occur in the store-buffer example?
7. What's the difference between visibility and atomicity?
8. Why does `final` give you safe publication for free?
9. Give three architectures and explain their process/thread choice.
10. How would you diagnose false sharing in production?

If any answer takes more than 30 seconds — reread that section.

---

## ➡️ Next

Move to **[Module 2 — Locks & AQS](./Module-02-Locks-and-AQS.md)** (coming next).
