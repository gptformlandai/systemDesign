# 04. IntelliJ Java: Threads View, Multithreading Debug, Deadlock Detection

## Goal

Use IntelliJ's Threads panel to inspect, control, and debug multithreaded Java applications — find deadlocks, race conditions, and thread starvation.

---

## Threads Panel

When the debugger is paused, open the Threads tab in the Debug tool window.

```text
Debug Window -> Threads tab

Shows:
  Thread ID  Thread Name              Status
  [23]  main                          RUNNING
  [24]  order-processor-1             WAITING  <- waiting on a lock
  [25]  order-processor-2             WAITING  <- waiting on a lock
  [26]  payment-processor-1           BLOCKED
  [27]  http-nio-8080-exec-1          RUNNABLE
  [28]  GC Thread                     RUNNING
```

Each thread has its own call stack. Click a thread to switch to its frame view.

---

## Switching Between Threads

```text
1. Set a breakpoint in code that runs on multiple threads.
2. When execution pauses on thread A:
   -> See thread A's call stack in the Frames panel.
3. Click thread B in the Threads panel.
   -> Frames panel updates to show thread B's current call stack.
4. You can inspect both threads' state simultaneously.
```

---

## Thread Status Codes

| Status | Meaning |
|---|---|
| RUNNABLE | Thread is executing (or ready to execute) |
| WAITING | Thread called wait(), join(), or LockSupport.park() |
| TIMED_WAITING | Thread called wait(N), sleep(N), or join(N) |
| BLOCKED | Thread is waiting to acquire a monitor lock (synchronized block) |
| NEW | Thread created but not started |
| TERMINATED | Thread has finished |

BLOCKED = the most important for deadlock investigation.

---

## Freezing And Thawing Threads

IntelliJ lets you freeze specific threads while others continue running.

```text
In Threads panel:
  Right-click thread A -> "Freeze"
    Thread A is suspended; all other threads continue.
  Right-click thread A -> "Thaw"
    Thread A resumes.
```

Use case: you want to observe thread B without thread A interfering.

```text
Thread suspension scope:
  "All" (default): all threads pause when any breakpoint fires.
  "Thread":        only the thread that hit the breakpoint pauses.

Change in Edit Breakpoint -> Suspend: All / Thread
```

Suspend="Thread" is essential for multithreaded debugging where pausing all threads masks timing bugs.

---

## Detecting Deadlocks

### What A Deadlock Looks Like

```text
Thread order-processor-1:
  BLOCKED waiting to acquire lock on Object@1234 (held by payment-processor-1)
  
Thread payment-processor-1:
  BLOCKED waiting to acquire lock on Object@5678 (held by order-processor-1)
  
Circular wait = deadlock.
```

### Method 1: IntelliJ Threads View At Breakpoint

```text
1. Add a conditional breakpoint: Thread.currentThread().getName().contains("processor")
2. When it fires, open Threads panel.
3. Look for threads in BLOCKED state.
4. Click each BLOCKED thread -> Frames panel shows "waiting to lock" in the call stack.
5. Cross-reference which threads hold which locks.
```

### Method 2: Trigger Thread Dump From Running Process

```text
With process attached in debug mode:
  Pause Program (Pause button in debug toolbar)
  -> IntelliJ pauses all threads
  -> All thread stacks are now visible

OR

  Main menu -> Run -> "Dump Threads"
  -> Outputs full thread dump to the debug console
  -> Look for "DEADLOCK DETECTED" section
```

### Thread Dump Deadlock Markers

```text
Found one Java-level deadlock:
=============================
"order-processor-1":
  waiting to lock monitor 0x00007f8b3c001b20 (object 0x00000007c0003a40, a java.lang.Object),
  which is held by "payment-processor-1"

"payment-processor-1":
  waiting to lock monitor 0x00007f8b3c001c30 (object 0x00000007c0005b50, a java.lang.Object),
  which is held by "order-processor-1"
```

---

## Reading A Thread Stack Frame

When a thread is BLOCKED or WAITING, its stack tells you WHY:

```text
"order-processor-1" thread:
  Frame 0: java.lang.Object.wait(Native Method)
  Frame 1: com.example.orders.OrderQueue.take(OrderQueue.java:45)
  Frame 2: com.example.orders.OrderProcessor.run(OrderProcessor.java:22)
  
Reading: thread is inside Object.wait() called from OrderQueue.take().
It is waiting for a notify() from another thread.
Navigate to OrderQueue.java:45 to understand what it is waiting for.
```

---

## Multithreading Debug Patterns

### Pattern 1: Reproduce Race Condition

```text
Race conditions are timing-dependent — they may not occur when you add a breakpoint
because suspending one thread changes thread interleaving.

Use log breakpoints instead:
  Breakpoint on shared resource access
  Non-suspending, with message: Thread.currentThread().getName() + " accessing: " + sharedValue

Watch the log output for concurrent access from different threads.
```

### Pattern 2: Forced Race With Thread Suspend

```text
1. Set breakpoint in Thread A just before it reads the shared resource.
2. Change breakpoint to "Suspend: Thread" (only pauses thread A, not others).
3. When thread A pauses, switch to thread B in Threads panel.
4. Let thread B run and modify the shared resource (click Resume for thread B only).
5. Resume thread A -> it now reads the stale/modified value.
6. This simulates a race condition deterministically.
```

### Pattern 3: Inspect Lock Owners

```text
When debugging a BLOCKED thread:
  The stack trace shows "waiting to lock 0xABCD"
  To find who holds that lock:
    Search other thread stacks for "locked 0xABCD"
    That thread is the lock holder.
```

---

## Useful Thread-Aware Evaluations

```java
// Evaluate in debugger to inspect thread state.

// See all threads.
Thread.getAllStackTraces().keySet().stream()
    .map(t -> t.getName() + " -> " + t.getState())
    .collect(java.util.stream.Collectors.toList())

// Find BLOCKED threads.
Thread.getAllStackTraces().keySet().stream()
    .filter(t -> t.getState() == Thread.State.BLOCKED)
    .map(Thread::getName)
    .collect(java.util.stream.Collectors.toList())

// Find current thread.
Thread.currentThread().getName() + " id=" + Thread.currentThread().getId()

// Thread dump via ManagementFactory (in Evaluate Expression).
java.lang.management.ManagementFactory.getThreadMXBean().findDeadlockedThreads()
```

---

## Interview Sound Bite

IntelliJ's Threads panel shows all JVM threads and their states. BLOCKED means waiting to acquire a synchronized lock — the most important state for deadlock investigation. Clicking a thread shows its independent call stack. Thread dump (Run → Dump Threads) shows "Found Java-level deadlock" sections with the full circular-wait chain. Setting breakpoint suspension to "Thread" instead of "All" prevents pausing other threads, enabling reproduction of race conditions. Log breakpoints on shared resource access are better than suspending breakpoints for race condition investigation because they don't change thread scheduling.
