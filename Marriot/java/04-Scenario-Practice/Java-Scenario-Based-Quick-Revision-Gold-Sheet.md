# Java Scenario-Based Quick Revision Gold Sheet

Target: last-mile Java interview revision.

Use this when you have limited time and want scenario-based answers that sound practical, not memorized.

How to use:

1. Read the scenario.
2. Say the answer out loud in 45-60 seconds.
3. Check the trap.
4. Repeat the weak ones.

---

## 1. Answer Template For Any Java Scenario

Use this structure:

```text
1. Identify the problem.
2. Pick the Java tool.
3. Explain the internal reason.
4. Mention the trap.
5. Add production caution.
```

Example:

```text
For a shared counter, I would not use volatile int because count++ is not atomic. I would
use AtomicInteger for a simple counter or LongAdder for high-contention metrics. If multiple
fields must change together, I would use a lock because atomics protect only one variable
at a time.
```

---

## 2. Core Java Scenarios

### Scenario 1: `final List` Is Modified

Question:

> If a list is declared final, can its elements still be changed?

Strong answer:

```text
Yes. final prevents reassignment of the reference, not mutation of the object. A final
List can still add or remove elements unless the list itself is immutable.
```

Trap:

```text
final reference is not the same as immutable object.
```

---

### Scenario 2: Object Used As HashMap Key Stops Working

Question:

> You put an object into HashMap as a key. Later get returns null. Why?

Strong answer:

```text
The key may have been mutated after insertion. HashMap uses hashCode to find the bucket.
If fields used in equals/hashCode changed, lookup may go to a different bucket and fail.
Keys should be immutable or at least fields used in equality should not change.
```

Trap:

```text
Mutable keys break hash-based collections.
```

---

### Scenario 3: `equals()` Works But HashSet Has Duplicates

Question:

> Two objects are equal by business meaning, but HashSet stores both. Why?

Strong answer:

```text
Most likely equals was overridden but hashCode was not. HashSet uses HashMap internally,
so equal objects must have the same hashCode. Override equals and hashCode together.
```

Trap:

```text
equals without hashCode breaks hash collections.
```

---

### Scenario 4: `String ==` Sometimes Works

Question:

> Why does `"java" == "ja" + "va"` return true?

Strong answer:

```text
The compiler folds compile-time constant string expressions, so both sides can refer to
the same pooled literal. But == compares references, not content. For content comparison,
always use equals.
```

Trap:

```text
Do not trust == for String content.
```

---

### Scenario 5: Constructor Is Not Called During Deserialization

Question:

> Why did constructor validation not run when object was deserialized?

Strong answer:

```text
For Serializable objects, normal constructors of the serializable class are not called
during deserialization. Java reconstructs object state from the stream. If validation is
needed, use custom deserialization hooks or avoid native serialization for external data.
```

Trap:

```text
Deserialization can bypass normal construction logic.
```

---

## 3. Collections Scenarios

### Scenario 6: ArrayList vs LinkedList For Frequent Inserts

Question:

> Which is better if we insert frequently?

Strong answer:

```text
It depends where insertion happens. LinkedList is O(1) only if we already have the node.
If we search by index first, traversal is O(n). ArrayList is often better in practice due
to cache locality and simpler memory layout.
```

Trap:

```text
LinkedList is not automatically faster for insertion.
```

---

### Scenario 7: PriorityQueue Iteration Is Not Sorted

Question:

> Why does iterating PriorityQueue not print sorted elements?

Strong answer:

```text
PriorityQueue guarantees only that poll/peek gives the highest-priority head. Its iterator
does not expose fully sorted order. To get sorted order, repeatedly poll or copy and sort.
```

Trap:

```text
Heap order is not full sorted order.
```

---

### Scenario 8: ConcurrentHashMap Allows Race?

Question:

> We used ConcurrentHashMap but still saw wrong booking data. How?

Strong answer:

```text
ConcurrentHashMap makes individual map operations thread-safe, not the entire business
workflow. Check-then-act logic can still race unless we use atomic methods like compute.
Also, mutable values stored inside the map, like ArrayList, may not be thread-safe.
```

Trap:

```text
Thread-safe map does not mean thread-safe business flow or thread-safe values.
```

---

### Scenario 9: `toMap` Throws Exception

Question:

> Why does Collectors.toMap throw IllegalStateException?

Strong answer:

```text
Duplicate keys appeared and no merge function was provided. toMap needs to know whether
to keep the old value, keep the new value, combine values, or fail.
```

Fix:

```java
Collectors.toMap(
    Employee::department,
    Function.identity(),
    (oldValue, newValue) -> oldValue
)
```

---

### Scenario 10: HashMap vs ConcurrentHashMap Nulls

Question:

> Why does ConcurrentHashMap not allow null?

Strong answer:

```text
In concurrent access, null from get would be ambiguous. It could mean key is absent or
key maps to null. ConcurrentHashMap avoids that ambiguity by disallowing null keys and values.
```

Trap:

```text
HashMap allows null; ConcurrentHashMap does not.
```

---

## 4. Streams Scenarios

### Scenario 11: Stream Pipeline Prints Nothing

Question:

> Why does peek not print anything?

Strong answer:

```text
Intermediate stream operations are lazy. peek is intermediate, so nothing runs until a
terminal operation like collect, count, or forEach is called.
```

Trap:

```text
No terminal operation means no execution.
```

---

### Scenario 12: `map` vs `flatMap`

Question:

> Employees have List<String> skills. How do you get all unique skills?

Strong answer:

```java
List<String> skills = employees.stream()
    .flatMap(employee -> employee.skills().stream())
    .distinct()
    .toList();
```

Explanation:

```text
map would produce Stream<List<String>>. flatMap converts each list into a stream and
flattens everything into Stream<String>.
```

---

### Scenario 13: Parallel Stream Is Slower

Question:

> Why did parallelStream make performance worse?

Strong answer:

```text
Parallel streams add splitting, scheduling, and combining overhead. They help mostly for
large CPU-bound independent work. They can hurt for small data, blocking IO, shared mutable
state, ordering requirements, or common ForkJoinPool contention.
```

Trap:

```text
parallel does not mean faster.
```

---

### Scenario 14: External List Mutation In Stream

Question:

> Is this good?

```java
List<String> names = new ArrayList<>();
employees.stream().forEach(e -> names.add(e.name()));
```

Strong answer:

```text
No. This uses side effects and can be unsafe with parallel streams. Use map and collect
to express transformation.
```

Better:

```java
List<String> names = employees.stream()
    .map(Employee::name)
    .toList();
```

---

### Scenario 15: `orElse` Is Calling Expensive Method

Question:

> Why is expensive default method called even when Optional has value?

Strong answer:

```text
orElse evaluates its argument eagerly. Use orElseGet when default computation should happen
only if Optional is empty.
```

Example:

```java
user.orElseGet(() -> loadDefaultUser());
```

---

## 5. Concurrency Scenarios

### Scenario 16: `volatile int count++`

Question:

> Is volatile enough for a counter?

Strong answer:

```text
No. volatile gives visibility and ordering, but count++ is read-modify-write and not atomic.
Use AtomicInteger, LongAdder, or a lock depending on the invariant.
```

Trap:

```text
Visibility is not atomicity.
```

---

### Scenario 17: `sleep()` vs `wait()`

Question:

> Does sleep release the lock?

Strong answer:

```text
No. sleep pauses the current thread but does not release the monitor. wait must be called
inside synchronized and releases the monitor while waiting.
```

Trap:

```text
sleep keeps lock; wait releases lock.
```

---

### Scenario 18: Executor Queue Keeps Growing

Question:

> Thread pool active count is maxed and queue keeps growing. What is happening?

Strong answer:

```text
The executor is saturated. Tasks arrive faster than they complete. I would check whether
tasks are blocked on DB/API calls, whether pool and queue sizes are bounded, and whether
timeouts, rejection policy, and backpressure are configured.
```

Production line:

```text
Unbounded queues hide overload until latency or memory explodes.
```

---

### Scenario 19: CompletableFuture Blocks Common Pool

Question:

> Why avoid CompletableFuture.supplyAsync for DB calls without executor?

Strong answer:

```text
Without a custom executor, supplyAsync uses the common ForkJoinPool. Blocking DB/API calls
can occupy common-pool threads and starve unrelated async work. Use a dedicated executor
or virtual threads for blocking workloads.
```

---

### Scenario 20: Deadlock In Booking Service

Question:

> Two booking requests hang forever. What can cause it?

Strong answer:

```text
A deadlock can happen if two threads acquire locks in different orders, such as room lock
then user lock in one path and user lock then room lock in another. Fix by using consistent
global lock ordering, reducing lock scope, or redesigning the critical section.
```

Debug:

```text
Take thread dump and inspect BLOCKED threads and deadlock section.
```

---

### Scenario 21: Semaphore For Downstream API

Question:

> How do you prevent 1000 concurrent requests from hitting a slow third-party API?

Strong answer:

```text
Use a Semaphore or bulkhead to limit concurrent calls, plus timeouts and fallback. Semaphore
controls local concurrency; distributed rate limiting needs shared state like Redis or API
gateway support.
```

---

### Scenario 22: ThreadLocal Data Leaks

Question:

> User A's request ID appears in User B's logs. Why?

Strong answer:

```text
ThreadLocal was probably not cleared in a thread pool. Threads are reused, so old request
context can leak into another request. Always remove ThreadLocal values in finally or use
framework-supported context cleanup.
```

---

## 6. Virtual Thread Scenarios

### Scenario 23: Virtual Threads For CPU Work

Question:

> Will virtual threads speed up CPU-heavy computation?

Strong answer:

```text
No. CPU-bound work is limited by CPU cores. Virtual threads help high-concurrency blocking
IO workloads by reducing platform-thread scarcity.
```

Trap:

```text
Virtual threads improve blocking scalability, not CPU speed.
```

---

### Scenario 24: Virtual Threads But DB Still Slow

Question:

> We enabled virtual threads but throughput did not improve. Why?

Strong answer:

```text
The bottleneck may be the database, connection pool, query latency, remote API, or locks.
Virtual threads remove thread scarcity, not downstream scarcity. Check DB pool wait time,
query time, timeouts, and downstream saturation.
```

---

### Scenario 25: Pinning

Question:

> What is virtual thread pinning?

Strong answer:

```text
Pinning happens when a virtual thread cannot unmount from its carrier thread, commonly when
blocking inside synchronized code or native calls. Too much pinning reduces virtual-thread
scalability.
```

Rule:

```text
Avoid blocking IO while holding synchronized monitors.
```

---

## 7. JVM And Debugging Scenarios

### Scenario 26: High CPU

Question:

> Java service CPU is 95%. How do you debug?

Strong answer:

```text
Find the hot Java process, inspect per-thread CPU, convert the native thread ID to hex,
take a thread dump, and match nid. Then confirm with repeated dumps or JFR to see whether
CPU is in app code, GC, serialization, regex, locks, or native calls.
```

Tools:

```text
top -H, jstack, jcmd, JFR
```

---

### Scenario 27: Memory Leak

Question:

> Heap usage keeps growing even after GC. What do you do?

Strong answer:

```text
I check GC logs/JFR, take a heap dump, inspect retained size and GC root paths, and look for
static collections, unbounded caches, ThreadLocal leaks, queues, listeners, or classloader leaks.
```

Trap:

```text
Java leaks are reachable-but-unused objects.
```

---

### Scenario 28: Full GC Spikes

Question:

> p99 latency spikes every few minutes and logs show Full GC. What next?

Strong answer:

```text
I would inspect allocation rate, old-generation growth, humongous objects, heap sizing,
and memory leaks using GC logs and JFR. Then I would reduce allocation pressure, fix retention,
or tune collector/heap based on evidence.
```

---

### Scenario 29: Thread Dump Shows Many WAITING Threads

Question:

> Is WAITING always bad?

Strong answer:

```text
No. Threads waiting in pools or blocking queues can be normal. I look for patterns: many
request threads waiting on the same lock, DB pool, remote call, CountDownLatch, or condition.
The stack trace context matters.
```

---

### Scenario 30: `OutOfMemoryError: unable to create native thread`

Question:

> Heap looks okay, but JVM cannot create native thread. Why?

Strong answer:

```text
This is native/OS thread exhaustion, not heap exhaustion. Causes include too many platform
threads, large thread stacks, OS limits, or thread leaks. Check thread count, executor usage,
ulimits, and thread dumps.
```

---

## 8. Modern Java Scenarios

### Scenario 31: Records For JPA Entity

Question:

> Should we use records for JPA entities?

Strong answer:

```text
Usually no. Records are immutable data carriers, while JPA entities often need no-arg
constructors, proxies, identity, and mutable lifecycle. Records are better for DTOs,
responses, projections, and value-like objects.
```

---

### Scenario 32: Sealed Class For Payment

Question:

> When would sealed classes help?

Strong answer:

```text
They help when the domain has a closed set of known subtypes, like CardPayment, UpiPayment,
and WalletPayment. The compiler can enforce allowed implementations and pattern matching
can become safer.
```

---

### Scenario 33: Preview Feature In Production

Question:

> Would you use preview features in production?

Strong answer:

```text
Not by default. Preview APIs can change and require explicit build/runtime flags. I would
use them only if the team accepts the risk, tooling supports it, and there is a migration plan.
```

---

## 9. Production Java Scenarios

### Scenario 34: Retry Created Duplicate Payment

Question:

> Retry logic charged user twice. What was missing?

Strong answer:

```text
Idempotency was missing. For retryable writes, use an idempotency key, store the result,
and enforce uniqueness in the database so repeated requests return the same outcome instead
of repeating the side effect.
```

---

### Scenario 35: Timeout Missing

Question:

> Remote API hangs and your service threads are exhausted. What should have been done?

Strong answer:

```text
Every remote call needs connection and read/request timeouts. Without timeouts, failures
turn into resource exhaustion. Add timeout budgets, retries only where safe, and bulkheads.
```

---

### Scenario 36: Bad Logging

Question:

> Production logs say only "error occurred". What is wrong?

Strong answer:

```text
The log has no context. Good logs include operation, entity IDs, request/correlation ID,
safe error details, and exception stack trace. Do not log secrets.
```

---

### Scenario 37: Native Serialization For API

Question:

> Should Java native serialization be used for public APIs?

Strong answer:

```text
No. Native serialization has security and versioning risks, especially with untrusted data.
Use explicit formats like JSON, Protobuf, or Avro with validation.
```

---

### Scenario 38: Naive Microbenchmark

Question:

> Candidate compares methods using System.nanoTime in a loop. Is that reliable?

Strong answer:

```text
Not for serious Java microbenchmarks. JIT warm-up, dead code elimination, GC, and CPU noise
can mislead. Use JMH for reliable benchmarking.
```

---

## 10. Final Rapid Fire

| Scenario | Strong One-Liner |
|---|---|
| `volatile count++` | Visibility yes, atomicity no |
| HashMap key lost | Key mutated after insertion |
| `toMap` fails | Duplicate key needs merge function |
| parallel stream slow | Overhead or wrong workload |
| Optional default runs | `orElse` is eager, use `orElseGet` |
| ThreadLocal leak | Remove in thread pools |
| Virtual threads slow | Bottleneck is not thread count |
| Full GC latency | Inspect allocation and retention |
| CHM wrong value | Mutable value or multi-step flow raced |
| Booking double insert | Need DB constraint/idempotency |
| Records for entity | Prefer records for DTOs, not JPA entities |
| Native serialization | Avoid for untrusted/external data |

---

## 11. Final Memory Trick

```text
Java interview scenarios are usually testing one of five things:
correctness, concurrency, memory, API design, or production failure behavior.
```

Best closing line:

```text
I choose the Java tool for in-process correctness, then verify whether production correctness
also needs database constraints, transactions, timeouts, idempotency, and observability.
```
