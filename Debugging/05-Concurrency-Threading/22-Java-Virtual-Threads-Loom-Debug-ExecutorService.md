# 22. Java Virtual Threads, Loom, CompletableFuture Debug

## Goal

Debug Java 21+ virtual threads (Project Loom), CompletableFuture chains, ForkJoinPool tasks, and structured concurrency.

---

## Project Loom: Virtual Threads (Java 21+)

```text
Platform threads (before Loom):
  1:1 mapping to OS threads.
  OS thread = ~1 MB stack + OS scheduling overhead.
  Thousands of threads = memory and context-switch pressure.

Virtual threads (Java 21+):
  Many-to-many: thousands of virtual threads multiplexed on a few OS threads.
  Managed by the JVM, not the OS.
  When a virtual thread blocks on I/O, the OS thread is released to run another virtual thread.
  Low memory cost: virtual thread stack = small heap allocation, grows as needed.
```

---

## Creating Virtual Threads

```java
// Option 1: Thread.ofVirtual().
Thread vt = Thread.ofVirtual().name("order-processor").start(() -> {
    processOrder("ORD-001");
});

// Option 2: VirtualThreadPerTaskExecutor.
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> processOrder("ORD-001"));

// Option 3: Thread factory for virtual threads.
ThreadFactory factory = Thread.ofVirtual().factory();
Thread t = factory.newThread(() -> processOrder("ORD-001"));
t.start();
```

---

## Debugging Virtual Threads In IntelliJ

Virtual threads appear in the Threads panel alongside platform threads:

```text
Threads panel:
  [1]  main         RUNNABLE    (platform thread)
  [23] virtual-1    WAITING     (virtual thread — paused on I/O or lock)
  [24] virtual-2    RUNNABLE    (virtual thread)
  ...
  (potentially thousands of entries for high-concurrency servers)
```

### Filtering Threads

In IntelliJ, use the filter box in the Threads panel to search by thread name:

```text
Filter: "order-processor"  -> shows only matching threads
```

---

## Pinning: Virtual Thread Stuck On Platform Thread

Virtual threads can be "pinned" to a platform thread, losing the multiplexing benefit:

```java
// Causes pinning:
synchronized (obj) {
    blockingOperation();  // virtual thread cannot be unmounted while holding a monitor
}

// Also causes pinning:
native code call while blocked

// Detect pinning with JVM flag:
// -Djdk.tracePinnedThreads=full
// Prints stack trace when a virtual thread is pinned.
```

IntelliJ diagnostic: look for virtual threads in RUNNING state with platform thread backing them on a synchronized block.

---

## CompletableFuture Chain Debug

```java
CompletableFuture.supplyAsync(() -> fetchOrder("ORD-001"))   // <- breakpoint 1
    .thenApply(order -> enrichOrder(order))                   // <- breakpoint 2
    .thenCompose(order -> chargePayment(order))               // <- breakpoint 3
    .thenAccept(result -> saveResult(result))                 // <- breakpoint 4
    .exceptionally(ex -> {
        // <- breakpoint 5: catches any exception from the chain
        log.error("Pipeline failed", ex);
        return null;
    });
```

### Debugging Steps

```text
1. Set breakpoints inside each lambda.
2. Run in debug mode.
3. Each lambda runs on a ForkJoinPool worker thread.
4. Threads panel shows: ForkJoinPool.commonPool-worker-N
5. Click each worker thread to see its call stack.
```

### IntelliJ Async Stack Trace For CompletableFuture

IntelliJ can show the "logical" async call stack, not just the physical one:

```text
Settings -> Build, Execution, Deployment -> Debugger -> Async Stack Traces
  [x] Enable Async Stack Traces
```

With this enabled, the call stack shows:
```text
enrichOrder  (current frame)
CompletableFuture.thenApply  (async continuation from)
fetchOrder   (origin of the CompletableFuture chain)
```

---

## ForkJoinPool Debug

```java
ForkJoinPool pool = new ForkJoinPool(4);

pool.submit(() -> {
    // ForkJoinTask
    ForkJoinTask<Integer> task = pool.submit(() -> computePartialSum(data));
    // breakpoint: task is scheduled but may not have run yet
    int result = task.join();  // breakpoint: blocks until task completes
    return result;
});
```

In Evaluate Expression:

```java
pool.getActiveThreadCount()   // threads actively running tasks
pool.getQueuedTaskCount()     // tasks waiting in queue
pool.getRunningThreadCount()  // threads not blocked
pool.isQuiescent()            // true if no active work
```

---

## Structured Concurrency (Java 21)

```java
import java.util.concurrent.StructuredTaskScope;

try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    // <- breakpoint: scope opened, tasks about to be submitted
    
    StructuredTaskScope.Subtask<Order> orderTask = 
        scope.fork(() -> fetchOrder("ORD-001"));    // <- breakpoint inside lambda
    
    StructuredTaskScope.Subtask<Payment> paymentTask = 
        scope.fork(() -> fetchPayment("PAY-001")); // <- breakpoint inside lambda
    
    scope.join();           // wait for all tasks
    scope.throwIfFailed();  // throws if any task threw an exception
    
    // breakpoint: both tasks complete
    Order order = orderTask.get();
    Payment payment = paymentTask.get();
}
```

In IntelliJ, each forked task appears as a virtual thread in the Threads panel while it is running.

---

## Common CompletableFuture Bugs

```java
// Bug 1: missing exceptionally() — unhandled exception silently swallowed.
CompletableFuture.supplyAsync(() -> riskyOperation())
    .thenApply(result -> process(result));
// If riskyOperation() throws, the exception is silently dropped.
// Fix: add .exceptionally() or .whenComplete().

// Bug 2: blocking inside thenApply on common pool.
CompletableFuture.supplyAsync(() -> fetchOrder("ORD-001"))
    .thenApply(order -> {
        Thread.sleep(5000);  // blocks a ForkJoinPool common pool thread
        return order;
    });
// Fix: use thenApplyAsync with a dedicated executor.

// Bug 3: forgetting join() — future is garbage collected without completing.
CompletableFuture.runAsync(() -> sendEmail(order));
// No join: if the JVM exits before the email is sent, it's silently dropped.
```

---

## Interview Sound Bite

Virtual threads (Java 21) are JVM-managed fibers multiplexed on OS threads — they park on I/O without blocking the OS thread. IntelliJ shows virtual threads in the Threads panel alongside platform threads. Pinning occurs when a virtual thread holds a synchronized monitor while blocking — detect with `-Djdk.tracePinnedThreads=full`. CompletableFuture chains run on ForkJoinPool workers; set breakpoints in each `.thenApply`/`.thenCompose` lambda to trace the pipeline. Enable IntelliJ's async stack traces to see the full logical chain. Always add `.exceptionally()` to CompletableFuture chains — exceptions are silently swallowed without it.
