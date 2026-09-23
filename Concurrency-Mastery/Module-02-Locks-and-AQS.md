# Module 2 — Locks & AbstractQueuedSynchronizer (AQS)

> **Goal:** You can explain *exactly* what happens when a thread hits `synchronized`, when it hits `ReentrantLock.lock()`, and how AQS coordinates thousands of threads with one `int` and a queue. Then code a fair bounded buffer without notes.

---

## 🗺️ Mind Map (redraw before moving on)

```
                     ┌─────────────────────────────────┐
                     │        MODULE 2 CORE            │
                     └─────────────────────────────────┘
                                    │
          ┌─────────────────────────┼──────────────────────────┐
          │                         │                          │
    2.1 synchronized          2.2 AQS Engine            2.3 Explicit Locks
    (JVM intrinsic)                                    (built on AQS)
          │                         │                          │
   ┌──────┼──────┐          ┌───────┼───────┐         ┌────────┼────────┐
   │      │      │          │       │       │         │        │        │
 Mark   Light   Heavy      state  CLH     park/     Reent    RRW      Cond
 Word    LW      HW        int    queue   unpark    Lock     Lock     ition
        (CAS)  (mutex)                              fair/    (down    (multi
                                                    unfair   grade)   -queue)
          │
       Wait/Notify
        + while()
```

---

## 2.1 `synchronized` — The JVM Intrinsic Lock

### Real-world analogy (for a junior)

> Picture a **single-stall public bathroom** with a lock on the door.
> - The **door lock** = the object's monitor.
> - **You entering + latching** = acquiring the monitor.
> - Someone knocking outside = a thread in the **_EntryList** waiting to get in.
> - You standing at the sink saying *"I'll wait until the soap is refilled"* = you called `wait()` and joined the **_WaitSet**. You *release the door* while waiting.
> - The janitor shouts *"soap refilled!"* = someone called `notifyAll()`. All sink-waiters go back to the door queue (they still have to re-acquire the lock).

### Object layout — where the lock actually lives

Every Java object has a **header**. On a 64-bit JVM (compressed oops):

```
┌──────────────────────────────────────────────────┐
│  Mark Word         (8 bytes)   ← lock state here │
│  Klass Word        (4 bytes)   ← class pointer   │
│  Padding           (variable)  ← align to 8      │
│  ... instance fields ...                         │
└──────────────────────────────────────────────────┘
```

The **Mark Word** encodes the current lock state via bit-tagging:

| State | What's in the Mark Word |
|---|---|
| Unlocked | hash code + age + tag `01` |
| Lightweight locked | pointer to the lock record on the owning thread's **stack** |
| Heavyweight locked (inflated) | pointer to an `ObjectMonitor` in native memory |
| GC-marked | bits used by the collector |

### Lock escalation (the states you'll be asked about)

```
UNLOCKED  ──(1st thread, CAS)──►  LIGHTWEIGHT  ──(contention)──►  HEAVYWEIGHT
                                                                    (OS mutex)
```

1. **Lightweight locking** — first contender does a **CAS on the Mark Word** to point it at a lock record on its own stack. Fast, no kernel involvement.
2. **Heavyweight locking** — if a second thread contends, the JVM **inflates** the monitor: allocates a native `ObjectMonitor`, wires the Mark Word to it, and the loser is **parked via an OS mutex/condition variable**. Now every acquire/release costs a syscall.

> **Note on biased locking:** Older JDKs (≤17) added a "biased" state that skipped CAS if only one thread ever acquired. **Deprecated in JDK 15, disabled by default, removed in JDK 18+.** Mention only if asked; don't lead with it.

### `ObjectMonitor` internals

```
        ObjectMonitor
        ┌────────────────────────────┐
        │ _owner       = Thread A    │  ← holds the lock now
        │ _recursions  = 3           │  ← reentrancy counter
        │ _EntryList   → [ T2, T5 ]  │  ← blocked on lock()
        │ _WaitSet     → [ T3, T4 ]  │  ← called wait()
        │ _cxq         → [ ... ]     │  ← contention queue (impl detail)
        └────────────────────────────┘
```

- **`_EntryList`** — threads blocked trying to *enter* the monitor.
- **`_WaitSet`** — threads that entered, then called `wait()` and released the lock. They will not be woken until someone `notify`s them.

### `wait / notify / notifyAll` — the three rules

**Rule 1 — You must hold the lock to call `wait()` or `notify()`.**
Otherwise: `IllegalMonitorStateException`.

**Rule 2 — Always `wait()` inside a `while` loop, never `if`.**
Reasons:
- **Spurious wakeups** — the OS can wake you up for no reason. Real thing, in the JLS.
- **Lost signal** — someone may have grabbed the lock *between* your wakeup and your re-check, changing the condition.

**Rule 3 — Prefer `notifyAll()` over `notify()`** unless you can prove exactly one waiter should wake. `notify()` picks a waiter *arbitrarily* — if the wrong one wakes and the condition doesn't apply to it, it goes back to sleep and the real target never runs. Classic **lost-notification bug**.

### The canonical wait/notify template (memorize this)

```java
public class BoundedBuffer<T> {
    private final Object lock = new Object();
    private final Queue<T> q = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) { this.capacity = capacity; }

    public void put(T item) throws InterruptedException {
        synchronized (lock) {
            while (q.size() == capacity) {       // WHILE, not IF
                lock.wait();
            }
            q.add(item);
            lock.notifyAll();                    // wake everyone; safe
        }
    }

    public T take() throws InterruptedException {
        synchronized (lock) {
            while (q.isEmpty()) {
                lock.wait();
            }
            T item = q.remove();
            lock.notifyAll();
            return item;
        }
    }
}
```

**Junior confusion cleared:**
- "Why `notifyAll()` and not `notify()`?" — with **one** lock guarding **two** conditions (full and empty), `notify()` might wake a waiting *producer* when it was a *consumer* you needed to wake, and the signal is lost.
- The clean fix is Module-2.3's `Condition` with **two** separate wait queues. That's what production code does.

### Reentrancy

`synchronized` is reentrant: the same thread can enter the same monitor multiple times. The monitor's `_recursions` counter increments; the lock is only released when the counter returns to 0. This is why you can call another `synchronized` method from within one without deadlocking yourself.

### Where devs get burned

| Bug | What happens |
|---|---|
| `wait()` outside `synchronized` | `IllegalMonitorStateException` |
| `wait()` inside `if` | Spurious wakeup returns from `wait()` while condition still false → NPE / logic bug |
| `notify()` with multi-condition | Woke the wrong thread → deadlock / stuck consumer |
| `synchronized (someString.intern())` | Interned strings are global — you're locking against unrelated code |
| `synchronized (Boolean.TRUE)` | Same issue — cached objects, global lock scope |
| Locking on a mutable field | `synchronized(this.lock)` where `lock` is reassigned → different threads lock on different objects |

---

## 2.2 AbstractQueuedSynchronizer (AQS) — The Engine of `java.util.concurrent`

### Why AQS exists

Doug Lea observed that **every synchronization primitive** — locks, semaphores, latches, barriers — has the same skeleton:
1. An **integer state** that represents "who holds what."
2. A **FIFO queue** of waiting threads.
3. **CAS** to update state atomically.
4. **`park`/`unpark`** to block/wake threads without OS mutex overhead when possible.

AQS is that skeleton. `ReentrantLock`, `Semaphore`, `CountDownLatch`, `ReentrantReadWriteLock`, `FutureTask`, `SynchronousQueue`, `ThreadPoolExecutor.Worker` — **all** built on AQS.

### The two things AQS owns

```
┌───────────────────────────────────────────────────────┐
│ AQS                                                    │
│                                                        │
│   volatile int state           ← the semantic meaning  │
│                                  is defined by subclass│
│                                                        │
│   CLH-style FIFO wait queue:                           │
│     head ─► Node(T1) ─► Node(T2) ─► Node(T3) ◄─ tail  │
│                                                        │
└───────────────────────────────────────────────────────┘
```

The `state` int means **whatever the subclass says it means**:
- `ReentrantLock` → 0 = unlocked, N = held N times by owner.
- `Semaphore` → number of available permits.
- `CountDownLatch` → count down to 0.
- `ReadWriteLock` → high 16 bits = reader count, low 16 bits = writer count.

### The node states

Each waiter is a `Node` with a status:

| State | Meaning |
|---|---|
| `0` (default) | Fresh node, waiting |
| `SIGNAL (-1)` | The *next* node in the queue needs a wake-up when this one releases |
| `CANCELLED (1)` | Thread was interrupted or timed out; skip it |
| `CONDITION (-2)` | Node is parked on a `Condition` wait queue, not the main queue |
| `PROPAGATE (-3)` | Shared-mode release should propagate to next node |

You don't memorize the numbers; you memorize **why they exist**: to make wakeups O(1) instead of scanning.

### Exclusive vs Shared mode

- **Exclusive mode** — only one thread holds `state` (e.g., `ReentrantLock`). Uses `acquire()` / `release()`.
- **Shared mode** — many threads can hold the resource simultaneously (e.g., `Semaphore` with permits > 1, `CountDownLatch` when count = 0). Uses `acquireShared()` / `releaseShared()`.

### The acquire flow (this is the interview answer)

```
Thread T calls lock():
    │
    ▼
tryAcquire()  ← subclass hook: "can I grab state via CAS?"
    │                       │
    │ success               │ fail
    ▼                       ▼
   return              enqueue Node(T) at tail via CAS
                            │
                            ▼
                       predecessor's status == SIGNAL?
                            │             │
                            │ no          │ yes
                            ▼             ▼
                     set pred to SIGNAL   park(T)
                            │             │
                            └─► loop ──►  (asleep until unpark)
                                          │
                                          ▼
                                     wake, retry tryAcquire()
```

**The release flow:**
```
unlock():
  set state = 0 (or decrement)
  read head node
  if head.next.status == SIGNAL:
      unpark(head.next.thread)
```

### The two clever bits Doug Lea baked in

1. **Enqueue is lock-free** — a new tail is spliced via a single CAS on `tail`. Even under heavy contention, the queue never blocks itself.
2. **Wakeup is one thread at a time (fair mode) or best-effort barging (unfair mode)** — no thundering herd.

### The subclass contract (what you implement if you build your own primitive)

Override any of these on `AbstractQueuedSynchronizer`:
- `tryAcquire(int)` / `tryRelease(int)` — exclusive
- `tryAcquireShared(int)` / `tryReleaseShared(int)` — shared
- `isHeldExclusively()` — required for `Condition` support

Everything else — queueing, parking, cancellation cleanup, condition queues — AQS handles.

### Micro-example: a custom binary semaphore in ~15 lines

```java
public class OneShotLatch {
    private final Sync sync = new Sync();

    public void signal()          { sync.releaseShared(1); }
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    private static final class Sync extends AbstractQueuedSynchronizer {
        @Override protected int tryAcquireShared(int ignored) {
            return getState() == 1 ? 1 : -1;   // >=0 means "acquired"
        }
        @Override protected boolean tryReleaseShared(int ignored) {
            setState(1);
            return true;                       // notify waiters
        }
    }
}
```

That's a **one-shot latch** — like `CountDownLatch(1)` — built with 4 real lines of AQS. This is the pattern L5 interviewers want you to sketch.

---

## 2.3 Explicit Locks (Built on AQS)

### 2.3.1 `ReentrantLock`

Same *semantics* as `synchronized` (reentrant, mutual exclusion), **but** with superpowers:

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Reentrancy | ✅ | ✅ |
| Fairness option | ❌ (always unfair) | ✅ (`new ReentrantLock(true)`) |
| `tryLock()` | ❌ | ✅ (non-blocking + timeout variant) |
| Interruptible acquire | ❌ | ✅ (`lockInterruptibly()`) |
| Multiple wait conditions | ❌ (one implicit) | ✅ (`newCondition()` — many) |
| Explicit release control | ❌ (scope-based) | ✅ (must `unlock()` in `finally`) |

### The **golden** template — memorize the shape

```java
private final ReentrantLock lock = new ReentrantLock();

lock.lock();
try {
    // critical section
} finally {
    lock.unlock();     // ALWAYS in finally; else an exception leaks the lock
}
```

### Fair vs Unfair — the tradeoff

- **Unfair (default)** — new arrival tries CAS on state *before* joining the queue. This is called **barging**. Higher throughput (~5–10× faster in benchmarks) because a cache-warm thread often grabs the lock before the scheduler wakes a queued waiter.
- **Fair** — new arrival always goes to the tail of the queue. No barging. Predictable, no starvation, but slower.

> **When to pick fair?** Only when starvation is a real risk *and* fairness matters more than throughput — e.g., a request quota lock in a multi-tenant system where one greedy tenant could starve others. Default to unfair.

### `tryLock` patterns (this is why we use `ReentrantLock` over `synchronized`)

**Non-blocking attempt (skip if busy):**
```java
if (lock.tryLock()) {
    try { /* do the optional work */ }
    finally { lock.unlock(); }
} else {
    // couldn't get it; skip, log, degrade
}
```

**Timed acquire (bounded backpressure):**
```java
if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    try { /* critical section */ }
    finally { lock.unlock(); }
} else {
    throw new TimeoutException("held too long");
}
```

**Deadlock-safe multi-lock acquire (the L5 answer):**
```java
while (true) {
    lockA.lock();
    if (lockB.tryLock()) {
        try { /* work */ }
        finally { lockB.unlock(); lockA.unlock(); }
        return;
    }
    lockA.unlock();                // back off
    Thread.yield();                // let the other side make progress
}
```

### 2.3.2 `ReentrantReadWriteLock`

Same idea but splits the state:
- **Read lock** — multiple readers can hold simultaneously.
- **Write lock** — exclusive; no readers, no other writers.

Internally the AQS state is packed:
```
┌──────────────┬──────────────┐
│  reader cnt  │  writer cnt  │
│  (high 16)   │  (low 16)    │
└──────────────┴──────────────┘
```

**Rules to memorize:**
1. **Write starvation** — with unfair mode + a busy reader stream, a writer can wait forever. Use `new ReentrantReadWriteLock(true)` if writers must not starve.
2. **Downgrade is allowed** — hold write, acquire read, release write → still holding read.
3. **Upgrade is NOT allowed** — hold read, try to acquire write → deadlock (you're waiting on yourself).
4. **Reentrant reads/writes** — same thread can re-acquire either without blocking.

### The correct downgrade pattern

```java
private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
private final Lock r = rw.readLock();
private final Lock w = rw.writeLock();
private volatile Data cache;

public Data get() {
    r.lock();
    try {
        if (cache != null) return cache;
    } finally { r.unlock(); }

    w.lock();
    try {
        if (cache == null) {
            cache = loadFromDb();
        }
        r.lock();                 // downgrade: acquire read BEFORE releasing write
    } finally { w.unlock(); }

    try {
        return cache;
    } finally { r.unlock(); }
}
```

### When to actually use RWLock

- **Read-heavy** workloads: reads:writes ≥ 10:1.
- **Non-trivial critical sections**: for tiny critical sections, RWLock overhead exceeds the win.
- If your reads are truly cheap → prefer a `volatile` reference to an **immutable** snapshot (see Module 1's `ConfigHolder` pattern) — often faster than RWLock.

### 2.3.3 `Condition` — the modern `wait/notify`

`Condition` is the AQS-based replacement for `Object.wait()/notify()`. It solves the **multiple wait queues on one lock** problem. Classic use: bounded buffer with separate `notFull` and `notEmpty` queues.

```java
public class BoundedBuffer<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final Object[] items;
    private int count, putIdx, takeIdx;

    public BoundedBuffer(int capacity) { this.items = new Object[capacity]; }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) notFull.await();     // WHILE
            items[putIdx] = item;
            if (++putIdx == items.length) putIdx = 0;
            count++;
            notEmpty.signal();                                 // only wake ONE consumer
        } finally { lock.unlock(); }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) notEmpty.await();
            T item = (T) items[takeIdx];
            items[takeIdx] = null;
            if (++takeIdx == items.length) takeIdx = 0;
            count--;
            notFull.signal();
            return item;
        } finally { lock.unlock(); }
    }
}
```

Compare with the `synchronized` version in 2.1: because we have **two conditions**, `signal()` wakes exactly the right kind of thread. No lost-signal bug, no need for `notifyAll()`.

**API cheat-sheet:**

| `Object` monitor | `Condition` | Difference |
|---|---|---|
| `wait()` | `await()` | Same semantics |
| `wait(timeout)` | `await(t, unit)`, `awaitNanos()`, `awaitUntil()` | Richer timing |
| `notify()` | `signal()` | Wake one waiter |
| `notifyAll()` | `signalAll()` | Wake all waiters |
| — | `awaitUninterruptibly()` | Ignore interrupts |

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | The Bad Answer | The L5 Answer |
|---|---|---|
| "How does `synchronized` work?" | "It's a lock." | "CAS on Mark Word for lightweight; if contended, JVM inflates to an `ObjectMonitor` with `_EntryList`/`_WaitSet` and OS-level parking." |
| "Why `while` around `wait()`?" | "Because... best practice." | "Spurious wakeups + lost signals when another thread grabs the lock and mutates state between wake and re-check." |
| "Why `notifyAll` over `notify`?" | "Safer." | "With one monitor guarding multiple conditions, `notify` can wake the wrong waiter, dropping the signal. Better: use `Condition` with separate queues." |
| "Diff between `synchronized` and `ReentrantLock`?" | "One's a keyword." | "Same reentrancy semantics, but `ReentrantLock` adds `tryLock`, timed acquire, interruptibility, fairness knob, and multiple `Condition`s." |
| "How does AQS work?" | "It's a base class." | "State int + FIFO CLH queue + CAS enqueue + `park`/`unpark`. Subclass defines what `state` means via `tryAcquire`/`tryRelease`." |
| "Read-lock upgrade to write?" | "Sure, just acquire it." | "Not allowed — deadlocks with yourself. Only *downgrade* is safe: hold write, take read, release write." |
| "Fair mode good, right?" | "Fairness is always better." | "Fair mode kills throughput 5–10×; only use when actual starvation is measured." |

---

## 💻 L5-Grade Code Examples

### Example 1 — A custom `CountDownLatch` from raw `wait/notify`

Shows you understand the primitive without the AQS layer.

```java
public class ManualCountDownLatch {
    private final Object lock = new Object();
    private int count;

    public ManualCountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException();
        this.count = count;
    }

    public void countDown() {
        synchronized (lock) {
            if (count == 0) return;
            if (--count == 0) lock.notifyAll();     // wake everyone waiting on 0
        }
    }

    public void await() throws InterruptedException {
        synchronized (lock) {
            while (count > 0) lock.wait();          // WHILE, spurious-safe
        }
    }

    public boolean await(long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        synchronized (lock) {
            while (count > 0) {
                long remainingMs = (deadline - System.nanoTime()) / 1_000_000L;
                if (remainingMs <= 0) return false;
                lock.wait(remainingMs);
            }
            return true;
        }
    }
}
```

Points an interviewer will grade:
- `while` loop guard ✅
- `notifyAll` (multiple awaiters possible) ✅
- Correct **remaining-time** re-computation across spurious wakeups ✅
- Idempotent `countDown()` at 0 ✅

### Example 2 — Fair token dispenser via `ReentrantLock + Condition`

Realistic: allow N concurrent workers, block the rest fairly.

```java
public class FairTokenDispenser {
    private final ReentrantLock lock = new ReentrantLock(true);   // fair
    private final Condition available = lock.newCondition();
    private int free;

    public FairTokenDispenser(int permits) { this.free = permits; }

    public void acquire() throws InterruptedException {
        lock.lock();
        try {
            while (free == 0) available.await();
            free--;
        } finally { lock.unlock(); }
    }

    public void release() {
        lock.lock();
        try {
            free++;
            available.signal();                      // exactly one waiter needs to run
        } finally { lock.unlock(); }
    }
}
```

### Example 3 — Custom AQS-based non-reentrant mutex (the interviewer's favorite)

```java
public final class Mutex {
    private final Sync sync = new Sync();

    public void lock()                              { sync.acquire(1); }
    public boolean tryLock()                        { return sync.tryAcquire(1); }
    public void unlock()                            { sync.release(1); }
    public boolean isLocked()                       { return sync.isHeldExclusively(); }
    public Condition newCondition()                 { return sync.newCondition(); }

    private static final class Sync extends AbstractQueuedSynchronizer {
        @Override protected boolean isHeldExclusively() { return getState() == 1; }

        @Override protected boolean tryAcquire(int ignored) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override protected boolean tryRelease(int ignored) {
            if (!isHeldExclusively()) throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        Condition newCondition() { return new ConditionObject(); }
    }
}
```

Talking points to say aloud:
- `compareAndSetState` handles the fast path.
- AQS enqueues losers on the CLH queue automatically.
- Overriding `isHeldExclusively` unlocks `Condition` support.
- No reentrancy on purpose — a second `lock()` from the same thread deadlocks. Contrast: `ReentrantLock` increments `state` instead.

---

## 🏭 Production War Stories

**1. The `synchronized(Boolean.TRUE)` incident.**
A caching library used `synchronized(cacheKey)` where `cacheKey` was sometimes `Boolean.TRUE`. Since `Boolean.TRUE` is a JVM-global singleton, **unrelated code paths across the app locked on the same object**. Sporadic latency spikes when otherwise-unrelated modules briefly serialized. Fix: `synchronized(new Object())` per shard, or a striped-lock helper.

**2. The write-starved cache.**
A read-heavy config service used `ReentrantReadWriteLock` with the default (unfair) mode. Under sustained read traffic, config refreshes (writes) never got the lock. TTL expired, stale config served for hours. Fix: switched to fair mode + moved to the copy-on-write pattern (`volatile` reference to immutable snapshot).

**3. The lost `notify()`.**
A team implemented a producer/consumer using one monitor and `notify()`. Every so often the queue would stall. Root cause: `notify()` woke a *producer* waiter (which was still full), which just went back to sleep — while a consumer sat unaware. Fix: two `Condition`s (`notFull`, `notEmpty`) on one `ReentrantLock`.

**4. The AQS parked-thread mystery.**
A service showed hundreds of threads in `WAITING (parking) at ...ReentrantLock$NonfairSync` in a thread dump. The team blamed AQS. Real cause: a single external HTTP call inside the critical section held the lock for seconds. AQS was doing its job. Fix: shorten critical section, use `tryLock(timeout)`, and add async fallback.

---

## 🎯 Self-Check (say the answers aloud)

1. What's inside an object's Mark Word, and how does it change across lock states?
2. Draw the `ObjectMonitor` structure and label `_EntryList` vs `_WaitSet`.
3. Why is `while (cond) wait()` mandatory? Name both reasons.
4. When would `notify()` beat `notifyAll()`? When is `notify()` unsafe?
5. What does the AQS `state` int mean for: `ReentrantLock`, `Semaphore`, `CountDownLatch`, `RRWLock`?
6. Trace the AQS acquire flow when 3 threads contend for a `ReentrantLock`.
7. Give one production reason to pick fair mode. What does it cost?
8. Why is read-lock upgrade forbidden but downgrade allowed?
9. Write the golden `lock/try/finally/unlock` block from memory.
10. Sketch a custom AQS non-reentrant mutex in ≤ 20 lines.

Any hesitation? Re-read that section.

---

## ➡️ Next

Move to **Module 3 — Lock-Free & Atomics** (CAS, ABA, `LongAdder`, `VarHandle`).
