# 21. Java Concurrency Debug: Deadlock, Race Conditions, Synchronized

## Goal

Debug Java concurrency bugs using IntelliJ, jstack, and thread dumps — deadlocks with synchronized blocks, race conditions with volatile and atomic variables.

---

## Java Memory Model: What Threads See

```text
Each thread has its own:
  - Stack (local variables, method frames)
  - PC register (current instruction)

Shared by all threads:
  - Heap (objects, arrays)
  - Static fields

Without synchronization:
  - Thread A's write to a heap object is NOT guaranteed to be visible to Thread B.
  - This is the Java Memory Model (JMM).
```

---

## Monitor Locks (Intrinsic Locks)

Every Java object has an intrinsic lock (monitor):

```java
// Synchronized method acquires lock on 'this'.
public synchronized void deposit(double amount) {
    this.balance += amount;
}

// Synchronized block acquires lock on specified object.
public void withdraw(double amount) {
    synchronized (this) {
        this.balance -= amount;
    }
}

// Static synchronized acquires lock on the Class object.
public static synchronized void incrementCount() {
    count++;
}
```

IntelliJ shows which lock an object holds in the Threads panel.

---

## Deadlock: Classic Pattern

```java
public class BankAccount {
    private double balance;
    private final Object lock = new Object();  // lock A
    
    public static void transfer(BankAccount from, BankAccount to, double amount) {
        synchronized (from.lock) {  // Thread 1 holds lock A, waits for lock B
            synchronized (to.lock) {  // Thread 2 holds lock B, waits for lock A
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }
}

// Deadlock scenario:
// Thread 1: transfer(accountA, accountB, 100)  -> locks A, tries to lock B
// Thread 2: transfer(accountB, accountA, 50)   -> locks B, tries to lock A
// Both threads wait forever.
```

---

## Detecting Deadlock: IntelliJ Threads Panel

```text
1. Run the deadlock scenario in debug mode.
2. Application hangs.
3. In IntelliJ debug window -> Threads tab:
   
   Thread order-processor-1: BLOCKED
     waiting to lock 0x00000007c0003a40 (held by order-processor-2)
     
   Thread order-processor-2: BLOCKED
     waiting to lock 0x00000007c0005b50 (held by order-processor-1)
   
4. This is a deadlock: each thread holds one lock and waits for the other.
```

---

## Detecting Deadlock: jstack

```bash
# Get PID of running Java process.
jps -l
# 12345 com.example.BankApplication

# Dump all threads.
jstack 12345

# Or dump to file.
jstack 12345 > /tmp/thread-dump.txt
cat /tmp/thread-dump.txt | grep -A 30 "deadlock"
```

### jstack Deadlock Output

```text
Found one Java-level deadlock:
=============================
"order-processor-1":
  waiting to lock monitor 0x00007f8b3c001b20 (object 0x00000007c0003a40, a java.lang.Object),
  which is held by "order-processor-2"
"order-processor-2":
  waiting to lock monitor 0x00007f8b3c001c30 (object 0x00000007c0005b50, a java.lang.Object),
  which is held by "order-processor-1"

Java stack information for the threads listed above:
===================================================
"order-processor-1":
        at com.example.BankAccount.transfer(BankAccount.java:18)
        - waiting to lock <0x00000007c0003a40>
        - locked <0x00000007c0005b50>
        at ...

"order-processor-2":
        at com.example.BankAccount.transfer(BankAccount.java:18)
        - waiting to lock <0x00000007c0005b50>
        - locked <0x00000007c0003a40>
        at ...
```

---

## Fixing Deadlock: Lock Ordering

```java
// Fix: always lock in a consistent order (lower ID first).
public static void transfer(BankAccount from, BankAccount to, double amount) {
    BankAccount first = from.id < to.id ? from : to;
    BankAccount second = from.id < to.id ? to : from;
    
    synchronized (first.lock) {
        synchronized (second.lock) {
            from.balance -= amount;
            to.balance += amount;
        }
    }
}
// Both threads acquire locks in the same order -> no circular wait -> no deadlock.
```

---

## Race Condition: Compound Read-Modify-Write

```java
// Not thread-safe: read balance, compute new value, write balance.
// Three separate steps — another thread can interleave between any two.
public void deposit(double amount) {
    this.balance = this.balance + amount;  // <- three bytecodes, not atomic
}

// Fix 1: synchronized method.
public synchronized void deposit(double amount) {
    this.balance += amount;
}

// Fix 2: AtomicReference with CAS.
private AtomicReference<Double> balance = new AtomicReference<>(0.0);

public void deposit(double amount) {
    balance.updateAndGet(current -> current + amount);
}
```

---

## volatile: Visibility Without Atomicity

```java
// volatile guarantees: writes are immediately visible to all threads.
// volatile does NOT guarantee: atomicity.
private volatile boolean running = true;

// Thread 1 loop:
while (running) {
    // process
}

// Thread 2:
running = false;  // guaranteed to be seen by Thread 1 immediately
// Without volatile: Thread 1 might never see the update (JMM allows caching).
```

### volatile Does Not Fix Race Conditions

```java
private volatile int counter = 0;

// STILL not thread-safe:
public void increment() {
    counter++;  // read then write — still two operations, not atomic
}

// Fix: AtomicInteger.
private AtomicInteger counter = new AtomicInteger(0);
public void increment() {
    counter.incrementAndGet();  // atomic: compare-and-swap
}
```

---

## ReentrantLock (Explicit Locking)

```java
import java.util.concurrent.locks.ReentrantLock;

private final ReentrantLock lock = new ReentrantLock();

public void deposit(double amount) {
    lock.lock();
    try {
        this.balance += amount;
    } finally {
        lock.unlock();  // always unlock in finally
    }
}
```

In IntelliJ Evaluate Expression:

```java
// Check lock state.
lock.isLocked()          // true if any thread holds it
lock.isHeldByCurrentThread()  // true if current thread holds it
lock.getQueueLength()    // number of threads waiting for this lock
lock.getHoldCount()      // how many times current thread has locked it (ReentrantLock allows re-entry)
```

---

## Interview Sound Bite

Java deadlocks occur when two threads each hold a lock the other needs — circular wait. Detect with IntelliJ Threads panel (BLOCKED threads) or `jstack <PID>` which prints the "Found Java-level deadlock" section with the full lock-wait chain. Fix with lock ordering: always acquire multiple locks in a deterministic order. Race conditions on compound operations (read-modify-write) require synchronized methods, AtomicInteger/AtomicReference, or explicit ReentrantLock. `volatile` only provides visibility guarantees, not atomicity — `counter++` is still not thread-safe with volatile.
