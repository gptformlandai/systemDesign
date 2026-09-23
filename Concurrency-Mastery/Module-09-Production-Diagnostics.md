# Module 9 — Production Diagnostics, Performance Profiling & Root-Cause Analysis

> **Goal:** Given a real "the service is stuck / burning CPU / slowly leaking" incident, you can name the likely failure class, capture the right artifact (thread dump / JFR / async-profiler flame graph), read it fluently, and point at the offending line of code. This is the L5 differentiator.

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────┐
                     │        MODULE 9 CORE               │
                     └────────────────────────────────────┘
                                    │
       ┌──────────────────┬─────────┴──────────┬────────────────────┐
       │                  │                    │                    │
   9.1 Failure         9.2 Thread Dumps    9.3 Profilers &     9.4 Live-System
   Modes               (jstack, jcmd,      Flame Graphs        Playbooks
   (deadlock,          BLOCKED vs          (async-profiler,    (high CPU / stuck /
   livelock,           WAITING vs          JFR events,         memory leak /
   starvation,         TIMED_WAITING)      GC vs safepoint)    latency)
   priority inv.,
   pool-starve DL,
   ThreadLocal leak)
```

---

## 9.1 The Concurrency Failure Modes

### The five you must be able to name and distinguish

| Failure | 1-line intuition | Symptom |
|---|---|---|
| **Deadlock** | Two threads each hold a lock the other wants | Stuck forever; 0% CPU on the involved threads |
| **Livelock** | Threads keep reacting to each other and making no progress | 100% CPU, throughput ≈ 0 |
| **Starvation** | Some threads never get scheduled or never get the lock | Some requests always slow; others fine |
| **Priority inversion** | Low-priority thread holds a lock a high-priority thread wants | High-priority tasks blocked behind low-priority ones |
| **Thread-pool starvation deadlock** | Parent task blocks waiting for a child task on the same bounded pool | Pool utilization 100%; queue growing forever |

### Deadlock — the classic diamond

```
Thread A: lock(X) → wants Y
Thread B: lock(Y) → wants X       → both parked forever
```

**Four conditions (Coffman) — memorize:**
1. **Mutual exclusion** — the resources cannot be shared.
2. **Hold and wait** — a thread holds one resource while waiting for another.
3. **No preemption** — resources cannot be forcibly taken away.
4. **Circular wait** — a cycle of threads each waiting on the next.

Break any one and deadlock is impossible. In practice, we break #4 by **global lock ordering** or #2 by **`tryLock` with backoff**.

### Livelock — deadlock's uglier cousin

Two threads keep politely stepping aside for each other. Retry loops with unbounded backoff, CAS storms where nobody wins, "two people trying to pass in a hallway."

Classic example — two threads each detect a conflict and back off with the same random seed → they retry in lockstep and collide again forever. Fix: randomized jitter, or a bounded retry with fallback.

### Starvation

Someone always gets served last. Root causes:
- Unfair `ReentrantLock` under sustained contention.
- Unfair `ReentrantReadWriteLock` with heavy reader traffic → writer never gets in.
- Priority-scheduled pools where low-priority never wins the scheduler.

Fix: fairness flag when it matters (see Module 2), or partition workload so no single pool serves both hot and cold traffic.

### Priority inversion

Real Mars Pathfinder bug (1997): a **low-priority** meteorological thread grabbed a mutex; a **high-priority** communications thread wanted the mutex; a **medium-priority** thread ran indefinitely because the OS scheduled it over the low-priority mutex holder. The high-priority thread stalled → watchdog rebooted the spacecraft.

Fix: **priority inheritance** (the OS temporarily boosts the lock holder to the highest waiter's priority). Java doesn't expose this directly at the language level; you sidestep it by isolating hot paths onto dedicated pools.

### Thread-pool starvation deadlock (the L5 favorite)

The trap:
```java
Executor pool = Executors.newFixedThreadPool(4);

Future<List<Result>> outer = pool.submit(() -> {
    Future<Result> a = pool.submit(() -> loadA());
    Future<Result> b = pool.submit(() -> loadB());
    return List.of(a.get(), b.get());       // deadlock if pool saturates
});
```

Every worker slot is filled by an **outer** task. Each outer holds the slot while waiting on a **child** submitted to the same pool. Children never get a slot. Nobody progresses. Pool utilization 100%, throughput 0.

**Fixes:**
- Never `Future.get()` on a task submitted to the same pool.
- Use **different pools** for parent and child stages.
- Or restructure with `CompletableFuture` chaining (the continuation doesn't hold a worker slot while waiting).

### `ThreadLocal` leaks — the silent memory eater

```java
static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);
```

Set in a request handler running on a **pool** thread. The pool thread is reused. Because `ThreadLocalMap` uses **weak references to keys** but **strong references to values**, the `Cache` instance stays until:
- The thread dies (never, in a pool), OR
- Someone calls `CACHE.remove()`.

Symptom: heap slowly grows in proportion to `poolSize × cacheSizePerRequest`. Especially bad when the value is a `ClassLoader` (classic in web apps → `PermGen`/`Metaspace` OOM after redeploys).

Fixes:
- Always call `remove()` in `finally` at the request boundary.
- Prefer `ScopedValue` under Loom (Module 7).
- For webapps: never store user-scoped state in `ThreadLocal` on pool threads.

---

## 9.2 Thread Dumps — Reading Them Like a Book

### How to capture (memorize)

| Command | Notes |
|---|---|
| `jcmd <pid> Thread.print` | Modern, no extra tools. Same output as `jstack`. |
| `jstack <pid>` | Classic. Also `jstack -l <pid>` to see synchronizer/lock owners. |
| `kill -3 <pid>` | Prints to the JVM's stdout — useful when tools aren't available. |
| `jcmd <pid> Thread.print -e` | JDK 21+, includes virtual threads. |
| `jcmd <pid> JFR.start duration=60s filename=x.jfr` | Rich profile including lock contention. |

**Best practice:** capture **3 dumps 10 seconds apart** and diff. One dump only shows a snapshot — three tell you which threads are moving and which are truly stuck.

### The thread states you MUST know

| State | Meaning | Where you'll see it |
|---|---|---|
| `RUNNABLE` | Executing bytecode | Hot paths, or spinning CAS loops |
| `BLOCKED` | Trying to enter a `synchronized` block | Contention on a monitor |
| `WAITING` (parking) | `LockSupport.park`, `Condition.await`, `Object.wait` without timeout | AQS locks, `CompletableFuture.get()`, empty queue |
| `TIMED_WAITING` (parking) | Same, with a deadline | `sleep`, `wait(timeout)`, `poll(timeout)` |
| `NEW` / `TERMINATED` | Not started / finished | Rarely useful |

### Anatomy of a stuck-thread stanza

```
"http-nio-8080-exec-42" #197 daemon prio=5 os_prio=0 cpu=15.60ms
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.acme.CacheLoader.load(CacheLoader.java:87)
        - waiting to lock <0x00000007c1a2f480> (a com.acme.CacheLoader)
        at com.acme.CacheLoader.getOrLoad(CacheLoader.java:53)
        - locked <0x00000007c1a2b010> (a com.acme.RequestContext)
        at com.acme.RequestHandler.handle(RequestHandler.java:41)

   Locked ownable synchronizers:
        - <0x00000007c1a2b010> (a com.acme.RequestContext)
```

Read it top-to-bottom:
- **State line** — tells you the immediate cause.
- **`waiting to lock <0x...>`** — the monitor you can't enter.
- **`locked <0x...>`** — monitors this thread already holds.
- **Locked ownable synchronizers** — AQS-based locks (`ReentrantLock`, `Semaphore`).

The pointer values (`0x00000007c1a2f480`) are the trick: they let you **cross-reference across threads** to build the lock-wait graph.

### Detecting a real deadlock

Modern `jcmd` / `jstack` prints an explicit deadlock report at the top:

```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f1a08006e58 (object 0x00000000ee6ea940, a java.lang.Object),
  which is held by "Thread-2"
"Thread-2":
  waiting to lock monitor 0x00007f1a0800bda8 (object 0x00000000ee6ea950, a java.lang.Object),
  which is held by "Thread-1"
```

If the JVM detects a cycle, it labels it. If not, you have to build the graph yourself by matching `waiting to lock <hex>` against `locked <hex>` in other threads.

### Quick triage patterns

| What you see in the dump | Likely diagnosis |
|---|---|
| Many threads `BLOCKED` on the same monitor address | Hot `synchronized` block — instrument it, split it, or replace with a lock-free structure |
| Many threads `WAITING (parking)` in `AQS.acquire` | Contended `ReentrantLock` — same treatment |
| All pool workers waiting on `Future.get()`, and no other tasks running | Thread-pool starvation deadlock |
| One thread `RUNNABLE` deep in a CAS loop with high CPU | CAS spin storm → consider `LongAdder` or striping |
| Threads with big stacks that never appear in RUNNABLE across dumps | Genuinely stuck — walk the stack, look for infinite `while(true)` waits |
| `synchronized (someString.intern())` or `synchronized (Boolean.TRUE)` in the stack | Global-lock accident (Module 2) |

---

## 9.3 Profilers, Flame Graphs, and JFR

### async-profiler — the default weapon

Low-overhead, safepoint-bias-free, allocation & lock-contention aware. Ships as an `.so`/`.dylib`; attach via:

```bash
# CPU profile for 30s, produce flame graph
./profiler.sh -e cpu -d 30 -f cpu.html <pid>

# Lock contention profile (parked/blocked time)
./profiler.sh -e lock -d 30 -f locks.html <pid>

# Allocation profile
./profiler.sh -e alloc -d 30 -f alloc.html <pid>

# Wall-clock (find where the thread is *waiting*, not just where it's running)
./profiler.sh -e wall -t -d 30 -f wall.html <pid>
```

### Reading a flame graph

```
       [ HANDLE ─────────────────────────────────────────── ]
             [ processRequest ────────────────────── ]
                   [ Cache.load ───── ][ dbQuery ─── ]
                       [ hash ][ read ][ send ][ wait ]
```

- **X axis** = share of samples (width). **Not time order.**
- **Y axis** = call depth. Root at the bottom or top depending on tool orientation.
- **Wide plateaus at the top** = the code doing the actual work you sampled.
- **Wide bars at deep frames** = your hotspot. Follow them up to their caller to understand context.

### The four flame-graph modes you should know

1. **CPU** — where the CPU is spent. Fixes: hot code paths, allocation churn, unintended reflection.
2. **Wall-clock** — where the thread is *sitting*, whether on-CPU or waiting. Best for latency debugging.
3. **Lock** — cumulative parked time per stack. Best for lock-contention debugging.
4. **Allocation** — bytes allocated per stack. Best for GC pressure debugging.

Interviewers love hearing you distinguish CPU vs wall-clock.

### JFR (JDK Flight Recorder) — always-on production profiling

Cheap (~1% overhead), built into the JDK, no attach needed if enabled at start.

**Continuous recording (production-safe):**
```
-XX:StartFlightRecording=disk=true,dumponexit=true,maxsize=500m,maxage=6h,filename=app.jfr
```

**Ad-hoc grab:**
```
jcmd <pid> JFR.start duration=60s filename=snap.jfr settings=profile
jcmd <pid> JFR.dump filename=now.jfr
```

### Key JFR events for concurrency

| Event | What it captures |
|---|---|
| `jdk.JavaMonitorEnter` | Time waiting to enter a `synchronized` block |
| `jdk.JavaMonitorWait` | Time in `Object.wait()` |
| `jdk.ThreadPark` | Time parked (AQS, `CompletableFuture.get`) |
| `jdk.VirtualThreadPinned` | Every pinned VT event (Module 7) |
| `jdk.CPULoad` / `jdk.ThreadCPULoad` | CPU per thread |
| `jdk.GCPause`, `jdk.SafepointBegin` | Distinguish GC pauses from safepoint pauses |
| `jdk.NativeMethodSample` | JNI-heavy stacks |

**Command-line inspection:**
```bash
jfr summary snap.jfr
jfr print --events jdk.JavaMonitorEnter --json snap.jfr | jq '.[] | select(.duration > 100000000)'
```

Or open in **JDK Mission Control (JMC)** — click through "Java Application → Lock Contention" for a ranked view.

### GC pause vs safepoint pause (the confused-candidate trap)

- **GC pause** — the collector stops all mutator threads to move objects. A subset of safepoint pauses.
- **Safepoint pause** — the JVM asks all threads to stop at a known-safe point for **any** reason: GC, biased-lock revocation (legacy), deopt, thread dump, class redefinition, JFR sample.

Symptom of a non-GC safepoint pause: latency spike with **no GC event** in the log. Diagnose with:
```
-XX:+PrintSafepointStatistics -XX:+UnlockDiagnosticVMOptions -XX:PrintSafepointStatisticsCount=1
```
or JFR `jdk.SafepointBegin` events. Very often the culprit is `Thread.getAllStackTraces()` in a metrics library.

---

## 9.4 Live-System Playbooks (Say These In Interviews)

### Playbook A — "Service seems stuck"

1. `jcmd <pid> Thread.print` × 3, 10s apart.
2. Look for the deadlock header.
3. If none: find threads that don't change state across the three dumps.
4. Group `BLOCKED`/`WAITING` threads by lock address. The address with the highest waiter count is your bottleneck.
5. Cross-reference: which thread owns that lock? What is it doing? → root cause.

### Playbook B — "CPU is 100%"

1. `top -H -p <pid>` → find hot OS thread IDs.
2. Convert to hex; grep in a thread dump — that gives the Java stack.
3. If the top frame is deep inside a CAS loop or `hashCode()`/`equals()` → likely a lock-free contention storm or an infinite loop.
4. In parallel, run `./profiler.sh -e cpu -d 30 <pid>` → flame graph.

### Playbook C — "Latency P99 spiking, P50 fine"

1. Check GC log first — spikes align with pauses?
2. If yes → check heap sizing, allocation rate (`-e alloc` flame graph).
3. If no → JFR `jdk.SafepointBegin` (non-GC safepoint?).
4. If neither → wall-clock profile. You're waiting on something — a downstream, a lock, a queue.

### Playbook D — "Memory slowly leaking"

1. Heap dump: `jcmd <pid> GC.heap_dump /tmp/heap.hprof`.
2. Open in **Eclipse MAT** or **VisualVM**.
3. Run "Leak Suspects" report; look at dominator tree.
4. Common culprits: `ThreadLocal` on pool threads, unbounded queues, static maps used as caches.

### Playbook E — "Thread count exploding"

1. Thread dump; count threads by name prefix.
2. If one prefix dominates: unbounded factory (e.g., `newCachedThreadPool` under load, or a bug creating a fresh pool per request).
3. `jcmd <pid> VM.flags` → check `-Xss` (stack size) × thread count vs available memory.

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | Bad answer | L5 answer |
|---|---|---|
| "How do you find a deadlock?" | "Restart the service." | "Capture 3 thread dumps 10s apart; look for the `Found one Java-level deadlock` header, or build the wait-for graph from `waiting to lock <hex>` and `locked <hex>`." |
| "Difference: `BLOCKED` vs `WAITING`?" | "Same thing." | "`BLOCKED` = trying to enter a `synchronized` monitor. `WAITING` = parked (AQS, `Object.wait`, `CompletableFuture.get`). Very different diagnoses." |
| "How would you catch a CAS spin storm?" | "Read the code." | "Async-profiler CPU flame graph — deep frames in `AtomicLong.compareAndSet`/`incrementAndGet` wide at the top. Fix with `LongAdder` or striping." |
| "Symptom of thread-pool starvation deadlock?" | "Deadlock." | "Pool utilization 100%, queue depth growing forever, all workers `WAITING` on `FutureTask.get`, no forward progress. Fix: separate pools for parent and child." |
| "GC pause vs safepoint pause?" | "Same." | "GC is one safepoint reason. `-XX:+PrintSafepointStatistics` or JFR `jdk.SafepointBegin` show all reasons: metrics scraping, deopt, class redefine, JFR itself." |
| "How do you detect `ThreadLocal` leaks?" | "Wait for OOM." | "Heap dump → MAT → look at retained heap by `ThreadLocalMap$Entry`. Fix: `remove()` in `finally`, or migrate to `ScopedValue`." |
| "async-profiler mode for latency?" | "CPU." | "Wall-clock — captures where the thread is *waiting*, not just where it's running. Lock mode ranks contended monitors specifically." |

---

## 💻 L5-Grade Snippets (What You Actually Type)

### Snippet 1 — Diagnosing a deadlock

```bash
# 1. Capture
jcmd <pid> Thread.print > dump-1.txt; sleep 10
jcmd <pid> Thread.print > dump-2.txt; sleep 10
jcmd <pid> Thread.print > dump-3.txt

# 2. Look for the JVM-detected header
grep -A2 "Found one Java-level deadlock" dump-1.txt

# 3. If undetected: extract waiting-on / locked pairs
grep -E "^\"|waiting to lock|- locked" dump-1.txt | less

# 4. Diff to confirm nothing moved
diff <(grep 'java.lang.Thread.State' dump-1.txt) \
     <(grep 'java.lang.Thread.State' dump-3.txt)
```

### Snippet 2 — Finding the "hot lock"

```bash
# JFR ad-hoc capture (60s)
jcmd <pid> JFR.start duration=60s filename=lock.jfr settings=profile
sleep 65
# Top monitors by cumulative wait time
jfr print --events jdk.JavaMonitorEnter --json lock.jfr \
  | jq -r '.events[] | "\(.duration) \(.stackTrace.frames[0].method.type.name)#\(.stackTrace.frames[0].method.name)"' \
  | sort -rn | head
```

### Snippet 3 — Wall-clock profile (find waits, not just CPU)

```bash
./profiler.sh -e wall -t -d 30 -f wall.html <pid>
open wall.html
```

The `-t` flag splits per-thread — essential for finding one hot thread among many.

### Snippet 4 — Correct `ThreadLocal` hygiene

```java
private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);

public Response handle(Request r) {
    try {
        CACHE.get().warm(r);
        return process(r);
    } finally {
        CACHE.remove();      // essential on pool threads
    }
}
```

### Snippet 5 — Detecting thread-pool starvation in code review

```java
// SMELL: submit + get on the same pool
Future<X> child = executor.submit(() -> compute());
X value = child.get();

// SAFE: chain, don't block
CompletableFuture.supplyAsync(() -> compute(), executor)
                 .thenApply(this::consume);
```

---

## 🏭 Production War Stories

**1. The undetected deadlock (mixed lock types).**
Service froze but `jstack` reported no deadlock. Reason: the cycle mixed a `synchronized` monitor with a `ReentrantLock` (AQS-based). JVM's classic detector only sees monitors. Fix: read the dump manually — the AQS lock owner showed under "Locked ownable synchronizers." Enforced global lock order across both primitives.

**2. The safepoint pause that wasn't GC.**
p99 latency spiked every 60s in perfect lockstep — but GC logs were clean. Cause: a metrics agent called `Thread.getAllStackTraces()` on a 60s tick. The JVM enters a **safepoint** to service that call, freezing all threads. Fix: JFR-based metrics instead.

**3. The CAS storm that looked like CPU-bound work.**
Service ran at 90% CPU but throughput was flat. `top -H` pointed to 20 hot threads. Thread dumps showed them in `AtomicLong.incrementAndGet`. Flame graph confirmed — one shared counter under contention. Fix: `LongAdder`. CPU dropped to 30%, throughput 4×.

**4. The pool-starvation deadlock that survived load tests.**
Small load tests kept the queue empty. In prod, sustained load filled the pool with outer tasks, each blocked on `.get()` of an inner task submitted to the same pool. Utilization 100%, throughput 0. Fix: separate pools for pipeline stages. Load test scenarios updated to include sustained saturation.

**5. The `ThreadLocal` classloader leak.**
A framework stored a per-request context in a `ThreadLocal`. The value transitively held the current `ClassLoader`. Every redeploy leaked one classloader → Metaspace slowly ballooned → OOM on the 43rd redeploy. Diagnosed via MAT dominator tree showing 42 `ClassLoader` instances retained. Fix: `remove()` in the servlet filter's `finally`, plus a global CI check that no user code uses `ThreadLocal` without `remove()`.

---

## 🎯 Self-Check

1. Coffman's four deadlock conditions — and which you'd break in practice.
2. `BLOCKED` vs `WAITING` — what code paths produce each?
3. Command that prints a JVM thread dump — three ways.
4. How do you cross-reference which thread owns a monitor another thread is blocked on?
5. Wall-clock vs CPU profiling — when do you pick each?
6. Two JFR events that surface lock contention.
7. GC pause vs safepoint pause — how do you distinguish?
8. Where does `ThreadLocal` retain memory, and what's the fix on pool threads?
9. Sketch the thread-pool starvation deadlock pattern in 5 lines of code.
10. Steps of Playbook A ("service is stuck") from memory.

---

## ➡️ Next

Move to **Module 10 — Spring Concurrency** (`@Async` AOP self-invocation trap, `ThreadPoolTaskExecutor`, MDC / SecurityContext propagation, WebFlux vs VT decision).
