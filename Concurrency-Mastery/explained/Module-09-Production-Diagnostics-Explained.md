# Module 9 — Production Diagnostics, Performance Profiling & Root-Cause Analysis (The Deep Dive)

> **Welcome to the Real World!** 🔧 This is where theory meets production. Given a "the service is stuck / burning CPU / slowly leaking" incident, you'll know the likely failure class, capture the right artifact, read it fluently, and point at the offending line of code. **This is the L5 differentiator.**

---

## 📑 Table of Contents

- [🎯 What You'll Master](#-what-youll-master)
- [🗺️ The Journey Ahead](#️-the-journey-ahead)
- [Part 1: The Concurrency Failure Modes](#part-1-the-concurrency-failure-modes)
  - [1.1 The Five You MUST Know](#-the-five-you-must-know)
  - [1.2 Deadlock — The Classic Diamond](#-deadlock--the-classic-diamond)
  - [1.3 Livelock — The Hallway Dance](#-livelock--the-hallway-dance)
  - [1.4 Starvation — The Unfair Queue](#-starvation--the-unfair-queue)
  - [1.5 Priority Inversion](#-priority-inversion)
  - [1.6 Thread-Pool Starvation Deadlock](#-thread-pool-starvation-deadlock)
  - [1.7 ThreadLocal Leaks](#-threadlocal-leaks)
- [Part 2: Thread Dumps — Reading the Crime Scene](#part-2-thread-dumps--reading-the-crime-scene)
  - [2.1 Capture Methods](#-capture-methods)
  - [2.2 Thread States](#-thread-states)
  - [2.3 Anatomy of a Thread Dump](#-anatomy-of-a-thread-dump)
  - [2.4 Deadlock Detection](#-deadlock-detection)
  - [2.5 Triage Patterns](#-triage-patterns)
- [Part 3: Profilers — async-profiler](#part-3-profilers--async-profiler)
  - [3.1 CPU Flame Graphs](#-cpu-flame-graphs)
  - [3.2 Wall-Clock Flame Graphs](#-wall-clock-flame-graphs)
  - [3.3 Lock Flame Graphs](#-lock-flame-graphs)
  - [3.4 Allocation Flame Graphs](#-allocation-flame-graphs)
- [Part 4: JFR — Java Flight Recorder](#part-4-jfr--java-flight-recorder)
  - [4.1 Always-On Profiling](#-always-on-profiling)
  - [4.2 Key Events](#-key-events)
  - [4.3 GC vs Safepoint Pause](#-gc-vs-safepoint-pause)
- [Part 5: Live-System Playbooks](#part-5-live-system-playbooks)
  - [5.1 Service Stuck](#-service-stuck)
  - [5.2 High CPU](#-high-cpu)
  - [5.3 Latency Spikes](#-latency-spikes)
  - [5.4 Memory Leak](#-memory-leak)
  - [5.5 Thread Explosion](#-thread-explosion)
- [Part 6: Production War Stories](#part-6-production-war-stories)
- [Part 7: Interview Traps & Self-Check](#part-7-interview-traps--self-check)

---

## 🎯 What You'll Master

By the end of this module, you'll be able to:
- Name and distinguish the five concurrency failure modes
- Capture and read thread dumps like a book
- Use async-profiler and JFR to find hotspots
- Follow live-system playbooks for common incidents
- Avoid the traps that trip up most candidates

---

## 🗺️ The Journey Ahead

```
                 ┌──────────────────────────────────────────────┐
                 │         MODULE 9 - PRODUCTION DIAGNOSTICS    │
                 │      "When Things Go Wrong at 3 AM"          │
                 └──────────────────────────────────────────────┘
                                    │
    ┌───────────┬──────────┬───────┴────────┬──────────┬───────────┐
    │           │          │                │          │           │
    ▼           ▼          ▼                ▼          ▼           ▼
┌───────┐  ┌────────┐  ┌────────┐     ┌─────────┐ ┌────────┐ ┌─────────┐
│ 9.1   │  │ 9.2    │  │ 9.3    │     │  9.4    │ │ War    │ │Playbooks│
│Failure│  │Thread  │  │Profiler│     │  JFR    │ │Stories │ │  Live   │
│ Modes │  │ Dumps  │  │Flame   │     │ Events  │ │        │ │ System  │
│       │  │        │  │Graphs  │     │         │ │        │ │         │
└───────┘  └────────┘  └────────┘     └─────────┘ └────────┘ └─────────┘
```

---

# Part 1: The Concurrency Failure Modes

## 🎯 The Five You MUST Know

Every concurrency bug falls into one of these categories. Learn to recognize them instantly!

```
The Five Failure Modes:
═══════════════════════════════════════════════════════════════

┌─────────────────┬─────────────────────────┬─────────────────────────┐
│    Failure      │     1-Line Intuition    │        Symptom          │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ DEADLOCK        │ Two threads each hold   │ Stuck forever           │
│                 │ a lock the other wants  │ 0% CPU on those threads │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ LIVELOCK        │ Threads keep reacting   │ 100% CPU                │
│                 │ to each other, no       │ Throughput ≈ 0          │
│                 │ progress                │                         │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ STARVATION      │ Some threads never get  │ Some requests always    │
│                 │ scheduled or get lock   │ slow; others fine       │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ PRIORITY        │ Low-priority thread     │ High-priority tasks     │
│ INVERSION       │ holds lock high-        │ blocked behind low-     │
│                 │ priority wants          │ priority ones           │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ THREAD-POOL     │ Parent task blocks on   │ Pool 100% utilized      │
│ STARVATION      │ child task in same      │ Queue growing forever   │
│ DEADLOCK        │ bounded pool            │ Throughput 0            │
└─────────────────┴─────────────────────────┴─────────────────────────┘
```

---

## 💀 Deadlock — The Classic Diamond

```
The Classic Deadlock:
═══════════════════════════════════════════════════════════════

Thread A                         Thread B
─────────                        ─────────
lock(X) ✓                        lock(Y) ✓
   │                                │
   │ wants Y                        │ wants X
   │                                │
   ▼                                ▼
BLOCKED!                         BLOCKED!
(Y is held by B)                 (X is held by A)

   ╔═══════════════════════════════════════════════════════╗
   ║  DEADLOCK! Both threads parked FOREVER!               ║
   ║  CPU usage on these threads: 0%                       ║
   ║  They're not spinning, they're SLEEPING.              ║
   ╚═══════════════════════════════════════════════════════╝
```

### Coffman's Four Conditions (MEMORIZE!)

For a deadlock to occur, ALL FOUR must be true:

```
Coffman's Four Conditions:
═══════════════════════════════════════════════════════════════

1. MUTUAL EXCLUSION
┌─────────────────────────────────────────────────────────────┐
│  The resources cannot be shared.                           │
│  Only one thread can hold the lock at a time.              │
└─────────────────────────────────────────────────────────────┘

2. HOLD AND WAIT
┌─────────────────────────────────────────────────────────────┐
│  A thread holds one resource while waiting for another.    │
│  Thread A has X, wants Y.                                  │
└─────────────────────────────────────────────────────────────┘

3. NO PREEMPTION
┌─────────────────────────────────────────────────────────────┐
│  Resources cannot be forcibly taken away.                  │
│  You can't steal a lock from another thread.               │
└─────────────────────────────────────────────────────────────┘

4. CIRCULAR WAIT
┌─────────────────────────────────────────────────────────────┐
│  A cycle of threads, each waiting on the next.             │
│  A → B → C → A                                             │
└─────────────────────────────────────────────────────────────┘

BREAK ANY ONE = DEADLOCK IMPOSSIBLE!
```

### How to Break Each Condition

```
Breaking Deadlock Conditions:
═══════════════════════════════════════════════════════════════

Break #1 (Mutual Exclusion):
┌─────────────────────────────────────────────────────────────┐
│  Use lock-free data structures                             │
│  Use read-write locks (readers can share)                  │
│  Often not practical for writes                            │
└─────────────────────────────────────────────────────────────┘

Break #2 (Hold and Wait):  ← PRACTICAL!
┌─────────────────────────────────────────────────────────────┐
│  Use tryLock() with timeout and backoff                    │
│  If you can't get all locks, release what you have         │
│                                                             │
│  if (!lock1.tryLock(100, MILLISECONDS)) {                  │
│      return; // Don't hold and wait!                       │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘

Break #3 (No Preemption):
┌─────────────────────────────────────────────────────────────┐
│  Requires OS/runtime support                               │
│  Java doesn't support lock preemption                      │
│  Not practical in Java                                     │
└─────────────────────────────────────────────────────────────┘

Break #4 (Circular Wait):  ← MOST COMMON FIX!
┌─────────────────────────────────────────────────────────────┐
│  GLOBAL LOCK ORDERING                                      │
│  Always acquire locks in the same order everywhere         │
│                                                             │
│  // Always lock lower ID first!                            │
│  if (account1.id < account2.id) {                         │
│      lock(account1); lock(account2);                       │
│  } else {                                                   │
│      lock(account2); lock(account1);                       │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Livelock — Deadlock's Uglier Cousin

```
Livelock Visualization:
═══════════════════════════════════════════════════════════════

Two people trying to pass in a hallway:

Person A                         Person B
─────────                        ─────────
"Oh, you go first"               "Oh, you go first"
*steps left*                     *steps left*
                                 
"Oops, still blocked"            "Oops, still blocked"
*steps right*                    *steps right*

"Oops, still blocked"            "Oops, still blocked"
*steps left*                     *steps left*

... FOREVER ...

Both are MOVING (not stuck like deadlock)
But neither makes PROGRESS!
CPU: 100%  Throughput: 0%
```

### Classic Code Example

```java
// Two threads with the same random seed!
while (true) {
    if (resource.isLocked()) {
        Thread.sleep(random.nextInt(100));  // Same seed = same sleep!
        continue;  // Both wake up at the same time, collide again!
    }
    // Try to acquire...
}
```

### The Fix: Randomized Jitter

```java
// Different random instances, or bounded retry with fallback
Random random = ThreadLocalRandom.current();  // Thread-local!
int retries = 0;

while (retries < MAX_RETRIES) {
    if (tryAcquire()) {
        return SUCCESS;
    }
    // Exponential backoff with JITTER
    Thread.sleep(baseDelay * (1 << retries) + random.nextInt(jitter));
    retries++;
}
return FALLBACK;
```

---

## 😢 Starvation — Someone Always Gets Served Last

```
Starvation Scenario:
═══════════════════════════════════════════════════════════════

Unfair lock with heavy contention:

Thread A: [====RUNNING====][====RUNNING====][====RUNNING====]
Thread B: [====RUNNING====][====RUNNING====][====RUNNING====]
Thread C: [====RUNNING====][====RUNNING====][====RUNNING====]
Thread D: [waiting........][waiting........][waiting........]
Thread E: [waiting........][waiting........][waiting........]

Threads D and E NEVER get the lock!
They're not deadlocked — they're STARVED.

Symptom: Some requests always slow, others fine.
```

### Root Causes

```
Starvation Causes:
═══════════════════════════════════════════════════════════════

1. Unfair ReentrantLock under sustained contention
┌─────────────────────────────────────────────────────────────┐
│  new ReentrantLock(false)  // false = unfair (default!)    │
│  Barging allowed — new arrivals can skip the queue         │
└─────────────────────────────────────────────────────────────┘

2. Unfair ReentrantReadWriteLock with heavy readers
┌─────────────────────────────────────────────────────────────┐
│  Readers keep arriving → writer NEVER gets in              │
│  "Writer starvation"                                       │
└─────────────────────────────────────────────────────────────┘

3. Priority-scheduled pools
┌─────────────────────────────────────────────────────────────┐
│  Low-priority tasks never win the scheduler                │
│  High-priority tasks always cut in line                    │
└─────────────────────────────────────────────────────────────┘
```

### The Fix

```java
// Use fair lock when starvation is a concern
ReentrantLock lock = new ReentrantLock(true);  // true = FAIR

// Or partition workload
// Don't mix hot and cold traffic on the same pool!
Executor hotPool = Executors.newFixedThreadPool(10);
Executor coldPool = Executors.newFixedThreadPool(5);
```

---

## 🚀 Priority Inversion — The Mars Pathfinder Bug

This is a REAL bug that happened on Mars in 1997!

```
Priority Inversion on Mars:
═══════════════════════════════════════════════════════════════

Three threads with different priorities:

HIGH:   Communications thread (talks to Earth!)
MEDIUM: Data processing thread
LOW:    Meteorological thread (weather data)

The Bug:
┌─────────────────────────────────────────────────────────────┐
│  1. LOW grabs a mutex                                      │
│  2. HIGH wants the mutex → BLOCKED (waiting for LOW)       │
│  3. MEDIUM runs indefinitely (higher priority than LOW!)   │
│  4. LOW never gets scheduled → never releases mutex        │
│  5. HIGH is stuck → watchdog timer fires → REBOOT!         │
│                                                             │
│  The spacecraft kept rebooting on Mars!                    │
└─────────────────────────────────────────────────────────────┘

The Irony:
HIGH is blocked behind LOW
But MEDIUM (which doesn't even need the mutex) runs instead!
```

### The Fix: Priority Inheritance

```
Priority Inheritance:
═══════════════════════════════════════════════════════════════

When HIGH blocks on a mutex held by LOW:
OS temporarily BOOSTS LOW to HIGH's priority!

Now LOW runs (at HIGH priority)
LOW finishes quickly, releases mutex
HIGH gets the mutex and runs

Java doesn't expose this directly at the language level.
Workaround: Isolate hot paths onto dedicated pools.
```

---

## 🏊 Thread-Pool Starvation Deadlock (The L5 Favorite!)

This is the **MOST COMMON** production deadlock pattern!

```
The Trap:
═══════════════════════════════════════════════════════════════

Executor pool = Executors.newFixedThreadPool(4);  // Only 4 slots!

Future<List<Result>> outer = pool.submit(() -> {
    // This task HOLDS a slot while waiting!
    Future<Result> a = pool.submit(() -> loadA());
    Future<Result> b = pool.submit(() -> loadB());
    return List.of(a.get(), b.get());  // BLOCKS waiting for children!
});
```

### Visual: How It Deadlocks

```
Thread-Pool Starvation Deadlock:
═══════════════════════════════════════════════════════════════

Pool has 4 slots. 4 outer tasks arrive.

Slot 1: [Outer Task 1] ──── waiting for child A1 ────►
Slot 2: [Outer Task 2] ──── waiting for child A2 ────►
Slot 3: [Outer Task 3] ──── waiting for child A3 ────►
Slot 4: [Outer Task 4] ──── waiting for child A4 ────►

Queue: [Child A1] [Child A2] [Child A3] [Child A4] [Child B1]...

   ╔═══════════════════════════════════════════════════════════╗
   ║  ALL SLOTS OCCUPIED by outer tasks!                       ║
   ║  Children are in the QUEUE but can't run!                 ║
   ║  Outer tasks are WAITING for children that can't start!   ║
   ║                                                            ║
   ║  Pool utilization: 100%                                   ║
   ║  Queue depth: Growing forever                             ║
   ║  Throughput: 0                                            ║
   ║                                                            ║
   ║  DEADLOCK! (But JVM won't detect it — no lock cycle!)    ║
   ╚═══════════════════════════════════════════════════════════╝
```

### The Fixes

```java
// FIX 1: Never Future.get() on same pool
// Use DIFFERENT pools for parent and child!
Executor parentPool = Executors.newFixedThreadPool(4);
Executor childPool = Executors.newFixedThreadPool(8);

Future<List<Result>> outer = parentPool.submit(() -> {
    Future<Result> a = childPool.submit(() -> loadA());  // Different pool!
    Future<Result> b = childPool.submit(() -> loadB());
    return List.of(a.get(), b.get());
});

// FIX 2: Use CompletableFuture chaining (no blocking!)
CompletableFuture.supplyAsync(() -> loadA(), pool)
    .thenCombine(
        CompletableFuture.supplyAsync(() -> loadB(), pool),
        (a, b) -> List.of(a, b)
    );
// The continuation doesn't HOLD a slot while waiting!
```

---

## 🧵 ThreadLocal Leaks — The Silent Memory Eater

```
The Setup:
═══════════════════════════════════════════════════════════════

static final ThreadLocal<HugeCache> CACHE = 
    ThreadLocal.withInitial(HugeCache::new);

public void handleRequest(Request r) {
    CACHE.get().process(r);  // Uses the cache
    // Oops! Forgot to call CACHE.remove()!
}
```

### Why It Leaks

```
ThreadLocal Memory Model:
═══════════════════════════════════════════════════════════════

Each Thread has a ThreadLocalMap:

┌─────────────────────────────────────────────────────────────┐
│  Thread "pool-1-thread-1"                                  │
│                                                             │
│  ThreadLocalMap:                                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Key (WeakRef)     │  Value (STRONG REF!)           │   │
│  ├────────────────────┼────────────────────────────────┤   │
│  │  CACHE (weak)      │  HugeCache instance (STRONG!)  │   │
│  └────────────────────┴────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

The KEY is a weak reference (can be GC'd)
The VALUE is a STRONG reference (won't be GC'd!)

On a POOL thread:
• Thread lives forever (reused)
• Value stays forever (strong ref from living thread)
• Memory grows: poolSize × cacheSizePerRequest
```

### The Especially Bad Case: ClassLoader Leaks

```
ClassLoader Leak Scenario:
═══════════════════════════════════════════════════════════════

Web app redeploy scenario:

Deploy 1: ThreadLocal holds object → object holds ClassLoader1
Deploy 2: ThreadLocal holds object → object holds ClassLoader2
Deploy 3: ThreadLocal holds object → object holds ClassLoader3
...
Deploy 43: OutOfMemoryError: Metaspace

Each redeploy leaks one ClassLoader!
ClassLoaders hold ALL classes they loaded!
Metaspace slowly balloons → OOM!
```

### The Fix

```java
// ALWAYS call remove() in finally!
private static final ThreadLocal<Cache> CACHE = 
    ThreadLocal.withInitial(Cache::new);

public Response handle(Request r) {
    try {
        CACHE.get().warm(r);
        return process(r);
    } finally {
        CACHE.remove();  // ESSENTIAL on pool threads!
    }
}

// Or use ScopedValue (Java 21+, Loom)
// Automatically cleaned up when scope exits!
```

---

# Part 2: Thread Dumps — Reading Them Like a Book

## 📸 How to Capture (MEMORIZE!)

```
Thread Dump Capture Methods:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────┬───────────────────────────────────┐
│           Command               │             Notes                 │
├─────────────────────────────────┼───────────────────────────────────┤
│ jcmd <pid> Thread.print         │ Modern, no extra tools needed     │
├─────────────────────────────────┼───────────────────────────────────┤
│ jstack <pid>                    │ Classic. Add -l for lock owners   │
├─────────────────────────────────┼───────────────────────────────────┤
│ jstack -l <pid>                 │ Shows synchronizer/lock owners    │
├─────────────────────────────────┼───────────────────────────────────┤
│ kill -3 <pid>                   │ Prints to JVM stdout (no tools!)  │
├─────────────────────────────────┼───────────────────────────────────┤
│ jcmd <pid> Thread.print -e      │ JDK 21+, includes virtual threads │
└─────────────────────────────────┴───────────────────────────────────┘
```

### The Golden Rule: THREE DUMPS!

```
Why Three Dumps?
═══════════════════════════════════════════════════════════════

ONE dump = a snapshot (threads might just be temporarily waiting)
THREE dumps = a movie (shows which threads are TRULY stuck)

# Capture 3 dumps, 10 seconds apart
jcmd <pid> Thread.print > dump-1.txt
sleep 10
jcmd <pid> Thread.print > dump-2.txt
sleep 10
jcmd <pid> Thread.print > dump-3.txt

# Diff to see what moved
diff dump-1.txt dump-3.txt

Threads that DON'T CHANGE across all 3 = TRULY STUCK!
```

---

## 🚦 The Thread States You MUST Know

```
Thread States:
═══════════════════════════════════════════════════════════════

┌─────────────────┬─────────────────────────┬─────────────────────────┐
│     State       │        Meaning          │     Where You See It    │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ RUNNABLE        │ Executing bytecode      │ Hot paths, CAS loops    │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ BLOCKED         │ Trying to enter a       │ Contention on a         │
│                 │ synchronized block      │ monitor (synchronized)  │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ WAITING         │ LockSupport.park,       │ AQS locks, Future.get,  │
│ (parking)       │ Condition.await,        │ empty queue poll        │
│                 │ Object.wait (no timeout)│                         │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ TIMED_WAITING   │ Same as WAITING but     │ sleep(), wait(timeout), │
│ (parking)       │ with a deadline         │ poll(timeout)           │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ NEW             │ Not started yet         │ Rarely useful           │
├─────────────────┼─────────────────────────┼─────────────────────────┤
│ TERMINATED      │ Finished                │ Rarely useful           │
└─────────────────┴─────────────────────────┴─────────────────────────┘
```

### BLOCKED vs WAITING — The Critical Difference!

```
BLOCKED vs WAITING:
═══════════════════════════════════════════════════════════════

BLOCKED:
┌─────────────────────────────────────────────────────────────┐
│  Trying to enter a SYNCHRONIZED block                      │
│  Someone else holds the monitor                            │
│                                                             │
│  synchronized (lock) {  // <-- BLOCKED here!              │
│      // can't get in                                       │
│  }                                                          │
│                                                             │
│  Diagnosis: Monitor contention                             │
│  Fix: Reduce critical section, use finer-grained locks    │
└─────────────────────────────────────────────────────────────┘

WAITING (parking):
┌─────────────────────────────────────────────────────────────┐
│  Voluntarily parked, waiting for a signal                  │
│                                                             │
│  • ReentrantLock.lock() → AQS parks the thread            │
│  • Future.get() → parks until result ready                │
│  • BlockingQueue.take() → parks until item available      │
│  • Condition.await() → parks until signaled               │
│                                                             │
│  Diagnosis: Waiting for work or a signal                  │
│  Fix: Depends on what it's waiting FOR                    │
└─────────────────────────────────────────────────────────────┘

VERY DIFFERENT DIAGNOSES!
```

---

## 📖 Anatomy of a Thread Dump Stanza

```
Reading a Thread Dump Entry:
═══════════════════════════════════════════════════════════════

"http-nio-8080-exec-42" #197 daemon prio=5 os_prio=0 cpu=15.60ms
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.acme.CacheLoader.load(CacheLoader.java:87)
        - waiting to lock <0x00000007c1a2f480> (a com.acme.CacheLoader)
        at com.acme.CacheLoader.getOrLoad(CacheLoader.java:53)
        - locked <0x00000007c1a2b010> (a com.acme.RequestContext)
        at com.acme.RequestHandler.handle(RequestHandler.java:41)

   Locked ownable synchronizers:
        - <0x00000007c1a2b010> (a com.acme.RequestContext)

Let's decode this:
═══════════════════════════════════════════════════════════════

LINE 1: Thread metadata
┌─────────────────────────────────────────────────────────────┐
│  "http-nio-8080-exec-42"  ← Thread name                    │
│  #197                     ← Thread ID                      │
│  daemon                   ← Daemon thread                  │
│  prio=5                   ← Java priority                  │
│  cpu=15.60ms              ← CPU time consumed              │
└─────────────────────────────────────────────────────────────┘

LINE 2: Current state
┌─────────────────────────────────────────────────────────────┐
│  BLOCKED (on object monitor)                               │
│  This thread is trying to enter a synchronized block!      │
└─────────────────────────────────────────────────────────────┘

STACK TRACE: Read top to bottom
┌─────────────────────────────────────────────────────────────┐
│  at CacheLoader.load:87                                    │
│  - waiting to lock <0x00000007c1a2f480>  ← CAN'T GET THIS │
│                                                             │
│  at CacheLoader.getOrLoad:53                               │
│  - locked <0x00000007c1a2b010>           ← ALREADY HOLDS  │
└─────────────────────────────────────────────────────────────┘

LOCKED OWNABLE SYNCHRONIZERS:
┌─────────────────────────────────────────────────────────────┐
│  AQS-based locks this thread holds                         │
│  (ReentrantLock, Semaphore, etc.)                         │
└─────────────────────────────────────────────────────────────┘
```

### The Hex Address Trick

```
Cross-Referencing Threads:
═══════════════════════════════════════════════════════════════

The hex addresses (0x00000007c1a2f480) are the KEY!

Thread A:
  - waiting to lock <0x00000007c1a2f480>  ← Wants this!

Thread B:
  - locked <0x00000007c1a2f480>           ← Has it!

MATCH! Thread A is waiting for Thread B!

Build the wait-for graph by matching:
  "waiting to lock <hex>" → "locked <hex>"
```

---

## 🔍 Detecting a Real Deadlock

```
JVM-Detected Deadlock:
═══════════════════════════════════════════════════════════════

Modern jcmd/jstack prints an EXPLICIT deadlock report:

Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f1a08006e58 
    (object 0x00000000ee6ea940, a java.lang.Object),
  which is held by "Thread-2"

"Thread-2":
  waiting to lock monitor 0x00007f1a0800bda8 
    (object 0x00000000ee6ea950, a java.lang.Object),
  which is held by "Thread-1"

Java stack information for the threads listed above:
===================================================
"Thread-1":
        at DeadlockDemo.methodA(DeadlockDemo.java:15)
        - waiting to lock <0x00000000ee6ea940>
        - locked <0x00000000ee6ea950>
"Thread-2":
        at DeadlockDemo.methodB(DeadlockDemo.java:25)
        - waiting to lock <0x00000000ee6ea950>
        - locked <0x00000000ee6ea940>

Found 1 deadlock.

If the JVM detects a cycle, it LABELS it for you!
```

### When JVM Doesn't Detect It

```
Undetected Deadlocks:
═══════════════════════════════════════════════════════════════

JVM's classic detector only sees MONITORS (synchronized)!

If your deadlock mixes:
• synchronized (monitor) with
• ReentrantLock (AQS-based)

The JVM WON'T detect the cycle!

You have to build the graph MANUALLY:
1. Find "waiting to lock <hex>" entries
2. Find "locked <hex>" entries
3. Check "Locked ownable synchronizers" for AQS locks
4. Draw the graph yourself
```

---

## 🎯 Quick Triage Patterns

```
Thread Dump Triage Cheat Sheet:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────┬───────────────────────────────────┐
│     What You See                │        Likely Diagnosis           │
├─────────────────────────────────┼───────────────────────────────────┤
│ Many threads BLOCKED on         │ Hot synchronized block            │
│ the SAME monitor address        │ → Split it or use lock-free      │
├─────────────────────────────────┼───────────────────────────────────┤
│ Many threads WAITING in         │ Contended ReentrantLock           │
│ AQS.acquire                     │ → Same treatment                  │
├─────────────────────────────────┼───────────────────────────────────┤
│ All pool workers waiting on     │ Thread-pool starvation deadlock   │
│ Future.get(), nothing running   │ → Separate pools for stages       │
├─────────────────────────────────┼───────────────────────────────────┤
│ One thread RUNNABLE deep in     │ CAS spin storm                    │
│ CAS loop with high CPU          │ → Use LongAdder or striping       │
├─────────────────────────────────┼───────────────────────────────────┤
│ Threads with big stacks that    │ Genuinely stuck                   │
│ never appear RUNNABLE           │ → Look for infinite while(true)   │
├─────────────────────────────────┼───────────────────────────────────┤
│ synchronized(someString.intern) │ Global-lock accident!             │
│ or synchronized(Boolean.TRUE)   │ → Fix the lock object             │
└─────────────────────────────────┴───────────────────────────────────┘
```

---

# Part 3: Profilers, Flame Graphs, and JFR

## 🔥 async-profiler — The Default Weapon

Low-overhead, safepoint-bias-free, allocation & lock-contention aware!

```
async-profiler Commands:
═══════════════════════════════════════════════════════════════

# CPU profile for 30s, produce flame graph
./profiler.sh -e cpu -d 30 -f cpu.html <pid>

# Lock contention profile (parked/blocked time)
./profiler.sh -e lock -d 30 -f locks.html <pid>

# Allocation profile (where are objects created?)
./profiler.sh -e alloc -d 30 -f alloc.html <pid>

# Wall-clock (find where thread is WAITING, not just running)
./profiler.sh -e wall -t -d 30 -f wall.html <pid>
```

---

## 📊 Reading a Flame Graph

```
Flame Graph Anatomy:
═══════════════════════════════════════════════════════════════

       [ HANDLE ─────────────────────────────────────────── ]
             [ processRequest ────────────────────── ]
                   [ Cache.load ───── ][ dbQuery ─── ]
                       [ hash ][ read ][ send ][ wait ]

X-AXIS = Share of samples (WIDTH)
         NOT time order!
         Wider = more samples = more time spent here

Y-AXIS = Call depth
         Root at bottom (or top, depending on tool)
         
WIDE PLATEAUS AT TOP = The code doing actual work
WIDE BARS AT DEEP FRAMES = Your hotspot!
                          Follow them UP to understand context
```

### The Four Flame Graph Modes

```
Flame Graph Modes:
═══════════════════════════════════════════════════════════════

1. CPU MODE (-e cpu)
┌─────────────────────────────────────────────────────────────┐
│  Where is the CPU being spent?                             │
│  Shows: Hot code paths, allocation churn, reflection       │
│  Use when: High CPU, want to optimize                      │
└─────────────────────────────────────────────────────────────┘

2. WALL-CLOCK MODE (-e wall)
┌─────────────────────────────────────────────────────────────┐
│  Where is the thread SITTING (on-CPU or waiting)?          │
│  Shows: Waits, sleeps, I/O, locks                         │
│  Use when: Latency debugging, thread seems slow           │
│                                                             │
│  THIS IS THE ONE INTERVIEWERS LOVE!                        │
│  "CPU vs wall-clock — when do you pick each?"             │
└─────────────────────────────────────────────────────────────┘

3. LOCK MODE (-e lock)
┌─────────────────────────────────────────────────────────────┐
│  Cumulative parked time per stack                          │
│  Shows: Which locks are most contended                    │
│  Use when: Lock contention debugging                      │
└─────────────────────────────────────────────────────────────┘

4. ALLOCATION MODE (-e alloc)
┌─────────────────────────────────────────────────────────────┐
│  Bytes allocated per stack                                 │
│  Shows: Where objects are being created                   │
│  Use when: GC pressure debugging, memory optimization     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📼 JFR (JDK Flight Recorder) — Always-On Production Profiling

Cheap (~1% overhead), built into the JDK, no attach needed!

```
JFR Commands:
═══════════════════════════════════════════════════════════════

# Continuous recording (production-safe)
java -XX:StartFlightRecording=disk=true,\
     dumponexit=true,\
     maxsize=500m,\
     maxage=6h,\
     filename=app.jfr \
     -jar myapp.jar

# Ad-hoc grab (60 seconds)
jcmd <pid> JFR.start duration=60s filename=snap.jfr settings=profile

# Dump current recording
jcmd <pid> JFR.dump filename=now.jfr
```

### Key JFR Events for Concurrency

```
JFR Concurrency Events:
═══════════════════════════════════════════════════════════════

┌─────────────────────────┬───────────────────────────────────────┐
│        Event            │           What It Captures            │
├─────────────────────────┼───────────────────────────────────────┤
│ jdk.JavaMonitorEnter    │ Time waiting to enter synchronized    │
├─────────────────────────┼───────────────────────────────────────┤
│ jdk.JavaMonitorWait     │ Time in Object.wait()                 │
├─────────────────────────┼───────────────────────────────────────┤
│ jdk.ThreadPark          │ Time parked (AQS, Future.get)         │
├─────────────────────────┼───────────────────────────────────────┤
│ jdk.VirtualThreadPinned │ Every pinned VT event (Loom!)         │
├─────────────────────────┼───────────────────────────────────────┤
│ jdk.CPULoad             │ System CPU load                       │
│ jdk.ThreadCPULoad       │ CPU per thread                        │
├─────────────────────────┼───────────────────────────────────────┤
│ jdk.GCPause             │ GC pause events                       │
│ jdk.SafepointBegin      │ ALL safepoint events (not just GC!)  │
└─────────────────────────┴───────────────────────────────────────┘
```

### Command-Line JFR Inspection

```bash
# Summary of recording
jfr summary snap.jfr

# Find long monitor waits (> 100ms)
jfr print --events jdk.JavaMonitorEnter --json snap.jfr \
  | jq '.[] | select(.duration > 100000000)'

# Or open in JDK Mission Control (JMC)
# Click "Java Application → Lock Contention" for ranked view
```

---

## ⚠️ GC Pause vs Safepoint Pause (The Confused-Candidate Trap!)

```
GC Pause vs Safepoint Pause:
═══════════════════════════════════════════════════════════════

SAFEPOINT PAUSE:
┌─────────────────────────────────────────────────────────────┐
│  JVM asks ALL threads to stop at a known-safe point        │
│  for ANY reason:                                           │
│                                                             │
│  • GC (garbage collection)                                 │
│  • Biased-lock revocation (legacy)                        │
│  • Deoptimization                                          │
│  • Thread dump                                             │
│  • Class redefinition                                      │
│  • JFR sample                                              │
│  • Thread.getAllStackTraces() ← COMMON CULPRIT!           │
└─────────────────────────────────────────────────────────────┘

GC PAUSE:
┌─────────────────────────────────────────────────────────────┐
│  A SUBSET of safepoint pauses                              │
│  Specifically for garbage collection                       │
└─────────────────────────────────────────────────────────────┘

THE TRAP:
Latency spike with NO GC EVENT in the log?
It's probably a NON-GC safepoint pause!

Diagnose with:
-XX:+PrintSafepointStatistics
-XX:+UnlockDiagnosticVMOptions

Or JFR jdk.SafepointBegin events
```

---

# Part 4: Live-System Playbooks (Say These In Interviews!)

## 📋 Playbook A: "Service Seems Stuck"

```
Playbook A: Service Stuck
═══════════════════════════════════════════════════════════════

STEP 1: Capture 3 thread dumps, 10 seconds apart
┌─────────────────────────────────────────────────────────────┐
│  jcmd <pid> Thread.print > dump-1.txt                      │
│  sleep 10                                                   │
│  jcmd <pid> Thread.print > dump-2.txt                      │
│  sleep 10                                                   │
│  jcmd <pid> Thread.print > dump-3.txt                      │
└─────────────────────────────────────────────────────────────┘

STEP 2: Look for the deadlock header
┌─────────────────────────────────────────────────────────────┐
│  grep -A5 "Found one Java-level deadlock" dump-1.txt       │
│                                                             │
│  If found → you have your answer!                          │
└─────────────────────────────────────────────────────────────┘

STEP 3: If no deadlock header, find stuck threads
┌─────────────────────────────────────────────────────────────┐
│  # Find threads that don't change state across dumps       │
│  diff <(grep 'Thread.State' dump-1.txt) \                  │
│       <(grep 'Thread.State' dump-3.txt)                    │
│                                                             │
│  Threads that DON'T CHANGE = truly stuck                   │
└─────────────────────────────────────────────────────────────┘

STEP 4: Group BLOCKED/WAITING threads by lock address
┌─────────────────────────────────────────────────────────────┐
│  grep -E "waiting to lock|locked" dump-1.txt | sort | uniq -c │
│                                                             │
│  The address with highest waiter count = your bottleneck   │
└─────────────────────────────────────────────────────────────┘

STEP 5: Cross-reference
┌─────────────────────────────────────────────────────────────┐
│  Which thread OWNS that lock?                              │
│  What is it DOING?                                         │
│  → ROOT CAUSE!                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 Playbook B: "CPU is 100%"

```
Playbook B: High CPU
═══════════════════════════════════════════════════════════════

STEP 1: Find hot OS thread IDs
┌─────────────────────────────────────────────────────────────┐
│  top -H -p <pid>                                           │
│                                                             │
│  Note the TID (thread ID) of hot threads                   │
│  Example: TID 12345 using 99% CPU                          │
└─────────────────────────────────────────────────────────────┘

STEP 2: Convert to hex and find in thread dump
┌─────────────────────────────────────────────────────────────┐
│  printf '%x\n' 12345                                       │
│  → 3039                                                    │
│                                                             │
│  grep -A20 "nid=0x3039" dump-1.txt                        │
│  → Shows the Java stack of the hot thread!                │
└─────────────────────────────────────────────────────────────┘

STEP 3: Analyze the stack
┌─────────────────────────────────────────────────────────────┐
│  If top frame is deep in CAS loop or hashCode/equals:     │
│  → Lock-free contention storm or infinite loop            │
│                                                             │
│  If top frame is in your business code:                   │
│  → Algorithmic issue (O(n²) loop, etc.)                   │
└─────────────────────────────────────────────────────────────┘

STEP 4: In parallel, run CPU flame graph
┌─────────────────────────────────────────────────────────────┐
│  ./profiler.sh -e cpu -d 30 -f cpu.html <pid>             │
│  open cpu.html                                             │
│                                                             │
│  Wide bars at top = your hotspot                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 Playbook C: "Latency P99 Spiking, P50 Fine"

```
Playbook C: P99 Latency Spikes
═══════════════════════════════════════════════════════════════

STEP 1: Check GC log first
┌─────────────────────────────────────────────────────────────┐
│  Do spikes align with GC pauses?                           │
│                                                             │
│  grep "pause" gc.log | tail -20                            │
│                                                             │
│  If YES → GC tuning needed                                 │
└─────────────────────────────────────────────────────────────┘

STEP 2: If GC, check allocation rate
┌─────────────────────────────────────────────────────────────┐
│  ./profiler.sh -e alloc -d 30 -f alloc.html <pid>         │
│                                                             │
│  Find where objects are being created                      │
│  Reduce allocation → reduce GC pressure                   │
└─────────────────────────────────────────────────────────────┘

STEP 3: If NOT GC, check safepoints
┌─────────────────────────────────────────────────────────────┐
│  JFR jdk.SafepointBegin events                            │
│                                                             │
│  Non-GC safepoint? (Thread.getAllStackTraces, etc.)       │
│  → Find and fix the culprit                               │
└─────────────────────────────────────────────────────────────┘

STEP 4: If neither, wall-clock profile
┌─────────────────────────────────────────────────────────────┐
│  ./profiler.sh -e wall -t -d 30 -f wall.html <pid>        │
│                                                             │
│  You're WAITING on something:                              │
│  • A downstream service                                    │
│  • A lock                                                  │
│  • A queue                                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 Playbook D: "Memory Slowly Leaking"

```
Playbook D: Memory Leak
═══════════════════════════════════════════════════════════════

STEP 1: Capture heap dump
┌─────────────────────────────────────────────────────────────┐
│  jcmd <pid> GC.heap_dump /tmp/heap.hprof                  │
│                                                             │
│  WARNING: This pauses the JVM! Do during low traffic.     │
└─────────────────────────────────────────────────────────────┘

STEP 2: Open in Eclipse MAT or VisualVM
┌─────────────────────────────────────────────────────────────┐
│  Eclipse MAT: File → Open Heap Dump                        │
│  Run "Leak Suspects" report                                │
│  Look at dominator tree                                    │
└─────────────────────────────────────────────────────────────┘

STEP 3: Common culprits
┌─────────────────────────────────────────────────────────────┐
│  • ThreadLocal on pool threads (no remove())              │
│  • Unbounded queues                                        │
│  • Static maps used as caches (no eviction)               │
│  • Listeners never unregistered                           │
│  • ClassLoader leaks (web app redeploys)                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📋 Playbook E: "Thread Count Exploding"

```
Playbook E: Thread Explosion
═══════════════════════════════════════════════════════════════

STEP 1: Thread dump and count by name prefix
┌─────────────────────────────────────────────────────────────┐
│  jcmd <pid> Thread.print | grep "^\"" | \                  │
│    sed 's/".*//' | sort | uniq -c | sort -rn | head       │
│                                                             │
│  Example output:                                           │
│  1523 "pool-                                               │
│    42 "http-nio-                                           │
│    10 "kafka-                                              │
└─────────────────────────────────────────────────────────────┘

STEP 2: If one prefix dominates
┌─────────────────────────────────────────────────────────────┐
│  Unbounded factory!                                        │
│                                                             │
│  • newCachedThreadPool() under load                       │
│  • Bug creating fresh pool per request                    │
│  • Executor not being reused                              │
└─────────────────────────────────────────────────────────────┘

STEP 3: Check memory impact
┌─────────────────────────────────────────────────────────────┐
│  jcmd <pid> VM.flags | grep Xss                           │
│                                                             │
│  Stack size × thread count = stack memory                 │
│  1MB × 1000 threads = 1GB just for stacks!               │
└─────────────────────────────────────────────────────────────┘
```

---

# Part 5: Production War Stories

## 💥 War Story 1: The Undetected Deadlock (Mixed Lock Types)

**Setup:** Service froze but `jstack` reported no deadlock.

**What Happened:**
```
The cycle mixed:
• synchronized monitor (detected by JVM)
• ReentrantLock (AQS-based, NOT detected!)

JVM's classic detector only sees monitors!
```

**How We Found It:**
```
Read the dump manually:
• Thread A: "waiting to lock <0x123>" (monitor)
• Thread B: "locked <0x123>" but also in "Locked ownable synchronizers"
            showing it held a ReentrantLock that Thread A needed

The AQS lock owner was under "Locked ownable synchronizers"!
```

**Fix:** Enforced global lock order across BOTH primitives.

---

## 💥 War Story 2: The Safepoint Pause That Wasn't GC

**Setup:** P99 latency spiked every 60 seconds in perfect lockstep. GC logs were clean!

**What Happened:**
```
A metrics agent called Thread.getAllStackTraces() on a 60s tick.

The JVM enters a SAFEPOINT to service that call.
ALL threads freeze while it collects stacks!

Not a GC pause — a safepoint pause!
```

**Fix:** Switched to JFR-based metrics instead.

---

## 💥 War Story 3: The CAS Storm That Looked Like CPU-Bound Work

**Setup:** Service ran at 90% CPU but throughput was flat.

**What Happened:**
```
top -H showed 20 hot threads.
Thread dumps showed them all in AtomicLong.incrementAndGet.
Flame graph confirmed — one shared counter under contention.

All 20 threads spinning on CAS, failing, retrying!
Lots of CPU work, zero progress!
```

**Fix:** Replaced `AtomicLong` with `LongAdder`. CPU dropped to 30%, throughput 4×.

---

## 💥 War Story 4: The Pool-Starvation Deadlock That Survived Load Tests

**Setup:** Small load tests passed. Production failed.

**What Happened:**
```
Small load tests: Queue stayed empty, children got slots.
Production sustained load: Queue filled up.

Every worker slot filled by outer tasks.
Each outer blocked on .get() of inner task.
Inner tasks queued but couldn't run.

Utilization 100%, throughput 0.
```

**Fix:** Separate pools for pipeline stages. Updated load tests to include sustained saturation.

---

## 💥 War Story 5: The ThreadLocal ClassLoader Leak

**Setup:** Framework stored per-request context in ThreadLocal. Value held ClassLoader reference.

**What Happened:**
```
Deploy 1: ThreadLocal → Context → ClassLoader1
Deploy 2: ThreadLocal → Context → ClassLoader2
...
Deploy 43: OutOfMemoryError: Metaspace

Each redeploy leaked one ClassLoader!
42 ClassLoader instances retained in heap dump.
```

**Fix:** 
- Added `remove()` in servlet filter's `finally`
- Global CI check: no ThreadLocal without remove()

---

# Part 6: Interview Traps & L5 Answers

## 🎯 Quick Reference Table

| Trap | Bad Answer | L5 Answer |
|------|------------|-----------|
| "How do you find a deadlock?" | "Restart the service." | "Capture 3 thread dumps 10s apart; look for `Found one Java-level deadlock` header, or build wait-for graph from `waiting to lock <hex>` and `locked <hex>`." |
| "Difference: BLOCKED vs WAITING?" | "Same thing." | "BLOCKED = trying to enter synchronized monitor. WAITING = parked (AQS, Object.wait, Future.get). Very different diagnoses." |
| "How would you catch a CAS spin storm?" | "Read the code." | "Async-profiler CPU flame graph — deep frames in AtomicLong.compareAndSet wide at top. Fix with LongAdder or striping." |
| "Symptom of thread-pool starvation deadlock?" | "Deadlock." | "Pool utilization 100%, queue depth growing forever, all workers WAITING on FutureTask.get, no forward progress. Fix: separate pools." |
| "GC pause vs safepoint pause?" | "Same." | "GC is one safepoint reason. -XX:+PrintSafepointStatistics or JFR jdk.SafepointBegin show all reasons: metrics scraping, deopt, class redefine." |
| "How detect ThreadLocal leaks?" | "Wait for OOM." | "Heap dump → MAT → look at retained heap by ThreadLocalMap$Entry. Fix: remove() in finally, or migrate to ScopedValue." |
| "async-profiler mode for latency?" | "CPU." | "Wall-clock — captures where thread is WAITING, not just running. Lock mode ranks contended monitors specifically." |

---

## 🔧 L5-Grade Snippets

### Snippet 1: Diagnosing a Deadlock

```bash
# 1. Capture
jcmd <pid> Thread.print > dump-1.txt; sleep 10
jcmd <pid> Thread.print > dump-2.txt; sleep 10
jcmd <pid> Thread.print > dump-3.txt

# 2. Look for JVM-detected header
grep -A2 "Found one Java-level deadlock" dump-1.txt

# 3. If undetected: extract waiting-on / locked pairs
grep -E "^\"|waiting to lock|- locked" dump-1.txt | less

# 4. Diff to confirm nothing moved
diff <(grep 'java.lang.Thread.State' dump-1.txt) \
     <(grep 'java.lang.Thread.State' dump-3.txt)
```

### Snippet 2: Finding the "Hot Lock"

```bash
# JFR ad-hoc capture (60s)
jcmd <pid> JFR.start duration=60s filename=lock.jfr settings=profile
sleep 65

# Top monitors by cumulative wait time
jfr print --events jdk.JavaMonitorEnter --json lock.jfr \
  | jq -r '.events[] | "\(.duration) \(.stackTrace.frames[0].method)"' \
  | sort -rn | head
```

### Snippet 3: Wall-Clock Profile

```bash
# Find where threads are WAITING (not just CPU)
./profiler.sh -e wall -t -d 30 -f wall.html <pid>
open wall.html

# The -t flag splits per-thread — essential for finding one hot thread
```

### Snippet 4: Correct ThreadLocal Hygiene

```java
private static final ThreadLocal<Cache> CACHE = 
    ThreadLocal.withInitial(Cache::new);

public Response handle(Request r) {
    try {
        CACHE.get().warm(r);
        return process(r);
    } finally {
        CACHE.remove();  // ESSENTIAL on pool threads!
    }
}
```

### Snippet 5: Detecting Pool Starvation in Code Review

```java
// SMELL: submit + get on the same pool
Future<X> child = executor.submit(() -> compute());
X value = child.get();  // BLOCKS holding a slot!

// SAFE: chain, don't block
CompletableFuture.supplyAsync(() -> compute(), executor)
    .thenApply(this::consume);  // Continuation doesn't hold slot!
```

---

## 🎯 Self-Check Questions

1. Coffman's four deadlock conditions — and which you'd break in practice.
2. BLOCKED vs WAITING — what code paths produce each?
3. Command that prints a JVM thread dump — three ways.
4. How do you cross-reference which thread owns a monitor another thread is blocked on?
5. Wall-clock vs CPU profiling — when do you pick each?
6. Two JFR events that surface lock contention.
7. GC pause vs safepoint pause — how do you distinguish?
8. Where does ThreadLocal retain memory, and what's the fix on pool threads?
9. Sketch the thread-pool starvation deadlock pattern in 5 lines of code.
10. Steps of Playbook A ("service is stuck") from memory.

---

## 🎓 The Production Diagnostics Mantras

> *"Three dumps, ten seconds apart. One dump is a snapshot; three tell a story."*

> *"BLOCKED is monitor contention. WAITING is voluntary parking. Different diagnoses!"*

> *"CPU flame graph for hotspots. Wall-clock flame graph for waits."*

> *"Thread-pool starvation: 100% utilization, 0% throughput, all workers waiting on Future.get()."*

> *"ThreadLocal on pool threads: always remove() in finally, or use ScopedValue."*

---

## ➡️ Next Module

Move to **Module 10 — Spring Concurrency** (`@Async` AOP self-invocation trap, `ThreadPoolTaskExecutor`, MDC / SecurityContext propagation, WebFlux vs VT decision).

---

*"The best debugger is a good night's sleep. The second best is a thread dump."* 🔧