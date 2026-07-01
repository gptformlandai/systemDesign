# 27. Scenario: Java Deadlock — IntelliJ Debug Walkthrough

## Scenario Description

Two bank accounts. Two threads. Each thread transfers money between accounts — but acquires locks in opposite order. Within seconds, both threads are stuck waiting for each other. The application hangs with no output, no errors, no stack trace to the console.

---

## Setup: Reproduce The Bug

```java
// BankAccount.java
public class BankAccount {
    private final String id;
    private double balance;
    
    public BankAccount(String id, double initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }
    
    public double getBalance() { return balance; }
    public String getId() { return id; }
    
    // THIS IS THE BUG: synchronized on 'this', not a shared lock.
    public synchronized void debit(double amount) {
        this.balance -= amount;
    }
    
    public synchronized void credit(double amount) {
        this.balance += amount;
    }
}

// DeadlockDemo.java
public class DeadlockDemo {
    public static void main(String[] args) throws InterruptedException {
        BankAccount accountA = new BankAccount("A", 1000.0);
        BankAccount accountB = new BankAccount("B", 1000.0);
        
        // Thread 1: Transfer from A to B.
        Thread t1 = new Thread(() -> {
            synchronized (accountA) {  // <- acquires lock on accountA
                try { Thread.sleep(100); } catch (InterruptedException e) { }
                synchronized (accountB) {  // <- waits for lock on accountB
                    accountA.debit(100);
                    accountB.credit(100);
                }
            }
        }, "transfer-A-to-B");
        
        // Thread 2: Transfer from B to A.
        Thread t2 = new Thread(() -> {
            synchronized (accountB) {  // <- acquires lock on accountB
                try { Thread.sleep(100); } catch (InterruptedException e) { }
                synchronized (accountA) {  // <- waits for lock on accountA
                    accountB.debit(100);
                    accountA.credit(100);
                }
            }
        }, "transfer-B-to-A");
        
        t1.start();
        t2.start();
        
        // Main thread waits.
        t1.join();
        t2.join();
        
        System.out.println("Done. A=" + accountA.getBalance() + " B=" + accountB.getBalance());
        // This line never prints.
    }
}
```

---

## Step 1: Run The Application — It Hangs

```text
Run the application in debug mode (Ctrl+D in IntelliJ).
After ~100ms, the application stops producing output.
It does not crash. It just... waits.
The progress bar in the Run/Debug console keeps running.
```

---

## Step 2: Open The Threads Panel

```text
IntelliJ Debug window -> Threads tab (if not visible, click the Threads icon).

You see:
  Thread [main]                WAITING
  Thread [transfer-A-to-B]     BLOCKED    <- blocked on a lock
  Thread [transfer-B-to-A]     BLOCKED    <- blocked on a lock
  Thread [GC Thread]           RUNNING
  Thread [Finalizer]           WAITING
```

---

## Step 3: Click Each BLOCKED Thread

### Click transfer-A-to-B

```text
Frames panel shows:
  java.lang.Object.wait(Native Method)
  ...
  DeadlockDemo.lambda$main$0(DeadlockDemo.java:22)
    - locked <0xAAAA1111> (a BankAccount)     <- holds accountA lock
    - waiting to lock <0xBBBB2222> (a BankAccount)  <- wants accountB lock

Thread is holding accountA and waiting for accountB.
```

### Click transfer-B-to-A

```text
Frames panel shows:
  java.lang.Object.wait(Native Method)
  ...
  DeadlockDemo.lambda$main$1(DeadlockDemo.java:32)
    - locked <0xBBBB2222> (a BankAccount)     <- holds accountB lock
    - waiting to lock <0xAAAA1111> (a BankAccount)  <- wants accountA lock

Thread is holding accountB and waiting for accountA.
Circular wait confirmed.
```

---

## Step 4: Get Thread Dump

```text
IntelliJ: Run -> Dump Threads
OR
In terminal: jstack <PID>
```

```text
Found one Java-level deadlock:
=============================
"transfer-A-to-B":
  waiting to lock monitor 0xBBBB2222 (BankAccount instance @0xBBBB...),
  which is held by "transfer-B-to-A"

"transfer-B-to-A":
  waiting to lock monitor 0xAAAA1111 (BankAccount instance @0xAAAA...),
  which is held by "transfer-A-to-B"
```

---

## Step 5: Fix — Lock Ordering

```java
// Fix: always lock the account with the lower hash code first.
public class DeadlockFixed {
    public static void transfer(BankAccount from, BankAccount to, double amount) {
        // Determine a consistent lock order using System.identityHashCode.
        BankAccount first, second;
        
        if (System.identityHashCode(from) < System.identityHashCode(to)) {
            first = from;
            second = to;
        } else {
            first = to;
            second = from;
        }
        
        synchronized (first) {
            synchronized (second) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
```

Now both threads acquire locks in the same object identity order:
- If identityHashCode(A) < identityHashCode(B): both threads lock A then B.
- No circular wait. No deadlock.

---

## Step 6: Verify The Fix

```text
Run the fixed code.
Both transfers complete.
"Done. A=1000.0 B=1000.0" (balances correct because each transfer is reversed by the other).
No hang. No deadlock.
```

---

## Key Takeaways

```text
Deadlock detection checklist:
  1. Application hangs with no error -> suspect deadlock.
  2. Open Threads panel: look for BLOCKED threads.
  3. Click each BLOCKED thread: read "waiting to lock" and "locked" lines.
  4. Find the circular dependency.
  5. Get thread dump via Run -> Dump Threads or jstack.
  6. Fix: consistent lock ordering or use tryLock() with timeout.

Alternative fix with ReentrantLock (with timeout):
  boolean gotFirst = first.tryLock(100, TimeUnit.MILLISECONDS);
  boolean gotSecond = second.tryLock(100, TimeUnit.MILLISECONDS);
  if (!gotFirst || !gotSecond) {
      // Release and retry.
      if (gotFirst) first.unlock();
      if (gotSecond) second.unlock();
  }
```

---

## Interview Sound Bite

A Java deadlock shows as all affected threads in BLOCKED state in the Threads panel. IntelliJ's thread frame shows "waiting to lock 0xBBBB (held by transfer-B-to-A)" and the other thread shows the inverse — confirming the circular wait. `jstack` prints "Found one Java-level deadlock" with the full chain. The fix is lock ordering: always acquire multiple locks in a deterministic order (e.g., by `System.identityHashCode`). The alternative is `ReentrantLock.tryLock()` with a timeout — if the lock isn't acquired in time, release everything and retry.
