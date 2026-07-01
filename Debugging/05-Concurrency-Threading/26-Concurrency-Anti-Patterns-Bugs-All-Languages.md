# 26. Concurrency Anti-Patterns and Bugs Across Java, Python, Node.js

## Goal

Recognize and fix the five concurrency anti-patterns (race condition, deadlock, starvation, livelock, memory visibility) across all three languages.

---

## Anti-Pattern 1: Race Condition

A race condition occurs when the outcome of a computation depends on the non-deterministic ordering of concurrent operations.

### Java

```java
// NOT thread-safe: read-modify-write on a shared field.
public class Counter {
    private int count = 0;
    
    public void increment() {
        count++;  // compiled to: read count, add 1, write count
                  // another thread can read between read and write
    }
}

// Fix 1: synchronized method.
public synchronized void increment() {
    count++;
}

// Fix 2: AtomicInteger.
private AtomicInteger count = new AtomicInteger(0);
public void increment() {
    count.incrementAndGet();  // compare-and-swap, atomic
}
```

### Python

```python
# NOT thread-safe: counter += 1 is multiple bytecodes.
counter = 0

def increment():
    global counter
    counter += 1  # LOAD_GLOBAL, INPLACE_ADD, STORE_GLOBAL (3 ops, GIL can switch between them)

# Fix: threading.Lock.
import threading
lock = threading.Lock()

def safe_increment():
    global counter
    with lock:
        counter += 1
```

### Node.js

```javascript
// "Race condition" in Node.js is different: single-threaded, but shared state
// can be mutated between async operations.
let counter = 0;

async function incrementAsync() {
    const current = counter;           // read
    await someAsyncOperation();        // another coroutine can run here
    counter = current + 1;            // write — using stale value
}

// Fix: do not read-then-write across async boundaries.
// Or: use a queue to serialize access.
async function safePath() {
    // sequence operations so only one runs at a time
}
```

---

## Anti-Pattern 2: Deadlock

Two or more threads/processes permanently wait for each other to release resources.

### Java

```java
// Thread A holds lock1, waits for lock2.
// Thread B holds lock2, waits for lock1.
// Circular wait = deadlock.

// Fix: lock ordering — always acquire locks in same order.
private static final Comparator<Object> LOCK_ORDER = 
    Comparator.comparingInt(System::identityHashCode);

// Acquire lower identity hash code first.
Object first = LOCK_ORDER.compare(lockA, lockB) < 0 ? lockA : lockB;
Object second = first == lockA ? lockB : lockA;
synchronized (first) {
    synchronized (second) {
        // safe: deterministic order prevents circular wait
    }
}
```

### Python

```python
import threading

lock_a = threading.Lock()
lock_b = threading.Lock()

# Thread 1: acquires a then b.
# Thread 2: acquires b then a.
# Deadlock if they interleave.

# Fix: always acquire in alphabetical or consistent order.
def transfer(from_acc, to_acc, amount):
    first, second = sorted([from_acc.lock, to_acc.lock], key=id)
    with first:
        with second:
            from_acc.balance -= amount
            to_acc.balance += amount
```

---

## Anti-Pattern 3: Starvation

A thread never gets CPU time because higher-priority threads always preempt it.

### Java

```java
// If many high-priority threads constantly run, low-priority threads wait forever.

// Check thread priorities in jstack:
// "background-worker" prio=1  <- low priority, may starve

// Fix: use fair locks (FIFO ordering).
private final Lock fairLock = new ReentrantLock(true);  // true = fair mode
// Fair mode: threads acquire lock in request order, preventing starvation.
// Tradeoff: slightly lower throughput vs unfair mode.
```

### Python / Node.js

```text
Python: GIL is released every ~5ms; all threads get turns eventually.
  Starvation less common but possible with busy-wait loops.

Node.js: single-threaded event loop. 
  Starvation = a synchronous task never yields to the event loop.
  Fix: break long operations into setImmediate() chunks.
```

---

## Anti-Pattern 4: Livelock

Threads are active (not blocked) but make no progress because they keep reacting to each other.

```java
// Classic livelock example: two "polite" threads.
class Worker {
    private boolean active = true;
    
    public void work(Worker other) {
        while (active) {
            // If other is active, "be polite" and deactivate.
            if (other.isActive()) {
                active = false;
                System.out.println(Thread.currentThread().getName() + " deactivated");
                continue;
            }
            // If other is inactive, reactivate.
            active = true;
            System.out.println(Thread.currentThread().getName() + " activated");
        }
    }
}
// Thread A deactivates when B is active. Thread B deactivates when A is active.
// Both keep switching states. No work is done.

// Fix: introduce randomness or a leader/arbiter pattern.
```

### Detecting Livelock

```text
jstack: threads are in RUNNABLE state (not BLOCKED).
  But they are consuming CPU without making progress.
  Check CPU usage: high CPU + no progress = livelock (or spin wait).
  
Thread dump will NOT show them as BLOCKED — they are actively looping.
Use CPU profiler or py-spy top to see what they are doing.
```

---

## Anti-Pattern 5: Memory Visibility

A thread's write is not visible to another thread due to CPU cache or JIT optimization.

### Java

```java
// Without volatile: flag may be cached in Thread 1's CPU register.
// Thread 2's write might never be seen.
private boolean running = true;

// With volatile: writes immediately flushed to main memory.
private volatile boolean running = true;

// volatile happens-before guarantee:
// A write to a volatile variable happens-before any read of that variable by another thread.
```

### Python

```text
GIL provides memory visibility: when a thread releases the GIL, 
all its writes are visible to the next thread that acquires the GIL.
Python does NOT have the Java Memory Model visibility problem.
```

### Node.js

```text
Single-threaded: no memory visibility issues between coroutines.
Worker threads use SharedArrayBuffer for shared memory.
SharedArrayBuffer requires Atomics for safe access (same as Java volatile + synchronized).
```

```javascript
const sab = new SharedArrayBuffer(4);
const arr = new Int32Array(sab);

// Atomic write (visible to other workers).
Atomics.store(arr, 0, 42);

// Atomic read.
const value = Atomics.load(arr, 0);

// Compare-and-exchange (atomic read-modify-write).
Atomics.compareExchange(arr, 0, 42, 100);  // if arr[0]==42, set to 100
```

---

## Summary Table

| Anti-Pattern | Java | Python | Node.js |
|---|---|---|---|
| **Race condition** | Use synchronized or Atomic* | Use threading.Lock | Avoid shared state across await |
| **Deadlock** | Lock ordering, ReentrantLock timeout | Lock ordering, timeout= on acquire | Not applicable (single thread) |
| **Starvation** | Fair ReentrantLock | Not common (GIL fairness) | Don't block event loop |
| **Livelock** | Randomize retry, arbiter | Same pattern | Same pattern |
| **Memory visibility** | volatile, synchronized | GIL handles it | Atomics for SharedArrayBuffer |

---

## Interview Sound Bite

The five concurrency anti-patterns: race condition (non-atomic compound operations), deadlock (circular lock wait), starvation (low-priority threads never scheduled), livelock (active threads make no progress), memory visibility (CPU cache preventing write visibility between threads). Python's GIL provides memory visibility but not atomicity. Node.js is single-threaded so race conditions only occur across async boundaries (state mutation between awaits). Java requires `volatile` for visibility and `synchronized`/`AtomicInteger` for atomicity. Livelock is the hardest to detect because threads show as RUNNABLE in thread dumps but consume CPU without progress.
