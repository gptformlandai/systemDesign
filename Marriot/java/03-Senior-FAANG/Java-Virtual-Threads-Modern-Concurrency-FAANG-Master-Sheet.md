# Java Virtual Threads And Modern Concurrency FAANG Master Sheet

Target: Java 21+ interviews, senior backend rounds, and modern Java architecture discussions.

This sheet covers:
- Virtual threads
- Platform threads vs virtual threads
- Carrier threads
- Pinning
- Blocking IO migration
- ThreadLocal caution
- Executor choices
- Structured concurrency
- Scoped values
- Production adoption checklist

---

## 1. Mental Model

Traditional Java threads are platform threads.

Platform thread:

```text
Java Thread -> OS thread
```

Virtual thread:

```text
Java virtual thread -> scheduled by JVM -> runs on carrier platform thread
```

Simple analogy:

```text
Platform threads are expensive workers.
Virtual threads are cheap tasks that borrow workers only when they are actually running.
```

Virtual threads are best when code spends lots of time waiting:

- HTTP calls
- DB calls
- File/network IO
- RPC calls

They are not a magic CPU accelerator.

---

## 2. Definition

Virtual threads are lightweight Java threads introduced as a stable feature in Java 21.

Core idea:

```text
Keep the simple thread-per-task programming model, but make each thread cheap enough for
high-concurrency blocking workloads.
```

Strong answer:

```text
Virtual threads let Java applications handle many concurrent blocking tasks without creating
one expensive OS thread per task. They are useful for request-per-thread services with lots
of blocking IO, but they do not remove CPU limits, database limits, or synchronization costs.
```

---

## 3. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Platform vs virtual thread | Very high | Basic concept |
| Thread-per-request model | Very high | Main design shift |
| Blocking IO benefit | Very high | Core use case |
| CPU-bound limitation | Very high | Common trap |
| Carrier thread | High | Runtime mental model |
| Pinning | High | Senior-level caveat |
| ThreadLocal caution | High | Production migration risk |
| Executor usage | High | How to code it |
| DB pool limit | Very high | Real-world bottleneck |
| Structured concurrency | Medium-high | Modern concurrency direction |
| Scoped values | Medium-high | Modern context propagation |

---

## 4. Why Virtual Threads Exist

Before virtual threads, Java servers often used fixed platform thread pools.

Problem:

```text
If each request blocks on DB/API calls, thousands of requests need thousands of OS threads.
OS threads are memory-heavy and expensive to schedule.
```

Reactive programming solved this by avoiding blocking, but it made code harder:

- Callback chains
- Reactive operators
- Harder debugging
- Different stack traces
- Different mental model

Virtual threads aim to keep simple blocking code:

```java
User user = userClient.getUser(id);
Orders orders = orderClient.getOrders(id);
return buildResponse(user, orders);
```

But allow far more concurrent waiting tasks.

---

## 5. Platform Thread vs Virtual Thread

| Area | Platform Thread | Virtual Thread |
|---|---|---|
| Backed by OS thread | Yes | Not one-to-one |
| Memory cost | Higher | Much lower |
| Scheduling | OS | JVM scheduler over carriers |
| Best for | CPU work, limited blocking | Many blocking tasks |
| Creation | Relatively expensive | Cheap |
| Pooling needed | Usually yes | Usually no |
| Debug model | Normal thread | Still looks like Thread |

Key line:

```text
Virtual threads are cheap enough that we usually do not pool them. We create one per task.
```

---

## 6. Basic Virtual Thread Example

```java
public class VirtualThreadExample {
    public static void main(String[] args) throws Exception {
        Thread thread = Thread.ofVirtual().start(() -> {
            System.out.println("Running in " + Thread.currentThread());
        });

        thread.join();
    }
}
```

Executor version:

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadExecutorExample {
    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> callBlockingService());
            executor.submit(() -> callAnotherBlockingService());
        }
    }

    static String callBlockingService() {
        return "ok";
    }

    static String callAnotherBlockingService() {
        return "ok";
    }
}
```

Important:

```text
The executor creates a new virtual thread per task. It is not a fixed-size platform thread pool.
```

---

## 7. What Happens When A Virtual Thread Blocks

When a virtual thread performs supported blocking IO:

1. Virtual thread starts running on a carrier platform thread.
2. It reaches blocking IO.
3. JVM parks/unmounts the virtual thread.
4. Carrier thread becomes free to run another virtual thread.
5. When IO completes, virtual thread resumes later on a carrier.

Mental model:

```text
The waiting virtual thread does not monopolize an OS thread during most supported blocking waits.
```

This is the central scalability benefit.

---

## 8. When Virtual Threads Help

Strong fits:

- High-concurrency HTTP services.
- Blocking REST clients.
- Blocking database calls.
- Blocking file/network IO.
- Thread-per-request programming style.
- Codebases that want simpler code than reactive stacks.

Example:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<User> user = executor.submit(() -> userClient.fetch(userId));
    Future<List<Order>> orders = executor.submit(() -> orderClient.fetch(userId));

    return new Profile(user.get(), orders.get());
}
```

Why it helps:

```text
The service can wait on multiple blocking calls without consuming many expensive platform threads.
```

---

## 9. When Virtual Threads Do Not Help

They do not solve:

- CPU saturation.
- Slow database queries.
- Small DB connection pool.
- Remote API rate limits.
- Bad locking.
- Long synchronized blocks.
- Memory-heavy per-request objects.

Bad expectation:

```text
We have CPU-heavy image processing, so virtual threads will make it faster.
```

Correct answer:

```text
CPU-bound work is limited by CPU cores. Virtual threads mainly improve scalability of
blocking workloads, not raw CPU throughput.
```

---

## 10. Carrier Threads

Carrier threads are platform threads used by the JVM to run virtual threads.

You usually do not manage them directly.

Key point:

```text
Many virtual threads are multiplexed over fewer carrier threads.
```

Trap:

```text
Virtual threads are not free CPU. At any instant, only as many virtual threads can execute
Java bytecode as available carrier/platform CPU capacity allows.
```

---

## 11. Pinning

Pinning means a virtual thread cannot unmount from its carrier while blocked.

Common causes:

- Blocking inside a `synchronized` block or method.
- Some native/foreign calls.

Example risk:

```java
class SlowService {
    synchronized String call() {
        return blockingRemoteCall();
    }

    private String blockingRemoteCall() {
        return "response";
    }
}
```

Problem:

```text
If blocking happens while holding a monitor, the virtual thread may pin the carrier thread.
Too much pinning reduces the scalability benefit.
```

Better:

```java
class BetterService {
    String call() {
        String request;
        synchronized (this) {
            request = buildRequest();
        }

        return blockingRemoteCall(request);
    }

    private String buildRequest() {
        return "request";
    }

    private String blockingRemoteCall(String request) {
        return "response";
    }
}
```

Rule:

```text
Keep synchronized sections short and avoid blocking IO while holding intrinsic locks.
```

---

## 12. Virtual Threads And Locks

Prefer:

- Short critical sections.
- Immutable data.
- Concurrent collections.
- `ReentrantLock` where appropriate.
- Avoid blocking inside locks.

Do not:

- Hold locks during HTTP calls.
- Hold locks during DB calls.
- Use global locks around request handling.

Strong answer:

```text
Virtual threads increase concurrency, so bad lock design becomes more visible. I would
audit synchronized blocks and avoid blocking IO while holding locks.
```

---

## 13. Virtual Threads And DB Pools

Important production trap:

```text
If the database pool has 30 connections, 10,000 virtual threads cannot execute 10,000
database queries at once. They will queue at the pool.
```

What to control:

- DB connection pool size.
- Query timeout.
- Request timeout.
- Maximum in-flight calls.
- Backpressure.
- Bulkheads/semaphores.

Example:

```java
import java.util.concurrent.Semaphore;

class OrderRepositoryGateway {
    private final Semaphore dbPermits = new Semaphore(30);

    Order fetch(String id) throws InterruptedException {
        dbPermits.acquire();
        try {
            return queryDatabase(id);
        } finally {
            dbPermits.release();
        }
    }

    private Order queryDatabase(String id) {
        return new Order(id);
    }
}

record Order(String id) {}
```

Interview line:

```text
Virtual threads remove thread scarcity, not downstream scarcity.
```

---

## 14. Virtual Threads And ThreadLocal

ThreadLocal still works with virtual threads.

But caution:

- Millions of virtual threads with heavy ThreadLocal values can increase memory.
- InheritableThreadLocal can accidentally copy data too widely.
- ThreadLocal-based context propagation can become messy.

Use ThreadLocal for:

- Small request ID
- Security context when framework supports it

Avoid:

- Large objects
- Caches
- Mutable request-wide bags

Modern alternative:

```text
Scoped values provide safer immutable context sharing within a bounded execution scope.
```

---

## 15. Scoped Values

Scoped values allow immutable data to be shared within a bounded dynamic scope.

Use cases:

- Request ID
- Tenant ID
- Auth principal
- Trace context

Conceptual example:

```java
// Conceptual modern Java example. Exact API availability depends on JDK version.
// ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
//
// ScopedValue.where(REQUEST_ID, "req-123").run(() -> {
//     service.handle();
// });
```

Why they matter:

```text
They are designed for structured, bounded context propagation and fit virtual-thread style
better than mutable ThreadLocal bags.
```

Interview safety:

```text
Check whether Scoped Values are final or preview in your exact JDK before claiming production use.
```

---

## 16. Structured Concurrency

Structured concurrency treats related concurrent subtasks as one unit.

Problem it solves:

```text
When a request starts multiple child tasks, cancellation, failure, and joining should be
managed together instead of scattering futures everywhere.
```

Conceptual shape:

```text
parent request
    -> child task A
    -> child task B
    -> child task C
join all or fail/cancel together
```

Strong answer:

```text
Structured concurrency makes concurrent code easier to reason about by giving child tasks
a clear lifetime under a parent scope. It improves cancellation and error handling, but I
would verify whether the API is stable or preview in the JDK used by the project.
```

---

## 17. Virtual Threads vs CompletableFuture

| Area | Virtual Threads | CompletableFuture |
|---|---|---|
| Style | Blocking, sequential-looking | Async composition |
| Best for | Many blocking tasks | Composing async pipelines |
| Error handling | Normal try/catch | Completion stages |
| Debugging | Normal stack style | Async chain complexity |
| Backpressure | Must design separately | Must design separately |

Practical rule:

```text
Virtual threads are great when the code is naturally blocking. CompletableFuture is still
useful for async composition, especially when APIs already return futures.
```

---

## 18. Virtual Threads vs Reactive

| Area | Virtual Threads | Reactive |
|---|---|---|
| Programming model | Simple blocking style | Non-blocking event pipelines |
| Learning curve | Lower | Higher |
| Memory per task | Low | Low |
| Best for | Blocking Java apps modernizing | End-to-end non-blocking systems |
| Debugging | Easier | Can be harder |

Balanced answer:

```text
Virtual threads reduce the need for reactive programming when the main reason for reactive
was avoiding thread-per-request scaling limits. Reactive can still be valuable for streaming,
backpressure-heavy pipelines, and ecosystems already built around non-blocking APIs.
```

---

## 19. Migration Checklist

Before switching to virtual threads:

- Confirm project runs Java 21+.
- Check framework support.
- Check JDBC driver behavior.
- Audit synchronized blocks.
- Audit ThreadLocal usage.
- Check connection pools.
- Set timeouts everywhere.
- Monitor pinned threads.
- Load test realistic traffic.
- Compare latency, throughput, CPU, memory, and downstream saturation.

Do not migrate blindly.

Strong migration answer:

```text
I would start with a bounded pilot path, measure under load, watch pinned threads and
downstream pool saturation, and only then expand. Virtual threads simplify blocking
concurrency but still need production controls.
```

---

## 20. Spring Boot Awareness

Modern Spring Boot versions can use virtual threads for request handling when running on a compatible JDK and server stack.

Interview answer:

```text
In Spring Boot, virtual threads can reduce pressure from servlet request threads for
blocking workloads. I would still tune connection pools, timeouts, and bulkheads. I would
also check whether security context, MDC, tracing, and ThreadLocal-based libraries behave
correctly with the chosen setup.
```

Areas to verify:

- Web server integration.
- JDBC pool limits.
- Transaction behavior.
- MDC/logging context.
- Tracing instrumentation.
- Security context propagation.

---

## 21. Mini Program: Parallel Blocking Calls

```java
import java.util.concurrent.*;

public class VirtualThreadAggregator {
    public static void main(String[] args) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> profile = executor.submit(() -> fetchProfile("u1"));
            Future<String> orders = executor.submit(() -> fetchOrders("u1"));
            Future<String> recommendations = executor.submit(() -> fetchRecommendations("u1"));

            String response = profile.get() + " | " + orders.get() + " | " + recommendations.get();
            System.out.println(response);
        }
    }

    static String fetchProfile(String userId) throws InterruptedException {
        Thread.sleep(100);
        return "profile";
    }

    static String fetchOrders(String userId) throws InterruptedException {
        Thread.sleep(100);
        return "orders";
    }

    static String fetchRecommendations(String userId) throws InterruptedException {
        Thread.sleep(100);
        return "recommendations";
    }
}
```

Explanation:

```text
This keeps the code readable while allowing independent blocking calls to wait concurrently.
In production, add timeouts, fallbacks, cancellation, metrics, and downstream concurrency limits.
```

---

## 22. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| Saying virtual threads make CPU faster | CPU still limited by cores | Use for blocking IO scalability |
| Keeping huge platform thread pools plus virtual threads | Confused model | Use per-task virtual executor for blocking tasks |
| Ignoring DB pool size | Requests just queue elsewhere | Tune downstream limits |
| Blocking inside synchronized | Can pin carrier | Keep locks short, avoid blocking in locks |
| Heavy ThreadLocal per virtual thread | Memory blow-up | Keep context small, consider scoped values |
| Migrating without load test | Surprises in drivers/frameworks | Pilot and measure |
| Replacing all async code blindly | Some async APIs are still right | Choose based on API and workload |
| No timeouts | More concurrency can amplify hangs | Add timeouts and cancellation |

---

## 23. FAANG-Level Question

Question:

> Your Java 21 service has 300 platform request threads and becomes thread-starved during downstream latency spikes. Would virtual threads help?

Strong answer:

```text
They may help if the bottleneck is many request threads blocked on IO. Virtual threads can
preserve the simple blocking style while allowing many more waiting requests without one
OS thread each. But I would first verify the bottleneck using thread dumps and metrics.
If downstream DB or API capacity is the actual limit, virtual threads alone will just move
the queue. I would add timeouts, bulkheads, pool limits, and monitor pinning, ThreadLocal
usage, carrier utilization, and downstream saturation during a load test.
```

---

## 24. Rapid Revision

Must-say lines:

```text
Virtual threads are lightweight Java threads scheduled by the JVM over carrier platform threads.
```

```text
They help most for high-concurrency blocking IO workloads.
```

```text
They do not make CPU-bound work faster.
```

```text
They remove thread scarcity, not database or remote-service scarcity.
```

```text
Avoid blocking IO inside synchronized sections because pinning can reduce scalability.
```

```text
Use load tests and production metrics before broad migration.
```

---

## 25. Official Source Notes

Use official sources when refreshing:

- OpenJDK JDK 21: `https://openjdk.org/projects/jdk/21/`
- OpenJDK JDK 25: `https://openjdk.org/projects/jdk/25/`
- JDK builds and GA/EA status: `https://jdk.java.net/`
- Java API docs: `https://docs.oracle.com/en/java/javase/`

Safety line:

```text
Virtual threads are stable from Java 21, but related APIs such as structured concurrency
and scoped values must be checked against the exact JDK version because some may be preview
or have changed across releases.
```
