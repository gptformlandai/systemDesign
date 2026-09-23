# Module 10 — Spring Boot & Enterprise Concurrency

> **Goal:** You know exactly why `@Async` sometimes runs synchronously, how to configure a production `ThreadPoolTaskExecutor`, how to propagate `SecurityContext` and MDC trace IDs across async boundaries, and when to pick WebFlux vs Spring MVC + Virtual Threads.

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────┐
                     │        MODULE 10 CORE              │
                     └────────────────────────────────────┘
                                    │
       ┌────────────────────┬───────┴─────────┬────────────────────┐
       │                    │                 │                    │
   10.1 @Async +        10.2 Task           10.3 Context       10.4 WebFlux vs
   AOP proxy            Executor            Propagation        MVC + Virtual
   (self-invocation     (production         (MDC, Security,    Threads
   trap, error          config, graceful    RequestAttributes) (decision matrix)
   handler)             shutdown, decorator)
```

---

## 10.1 `@Async` — The AOP Proxy Trap Every Senior Should Know Cold

### Real-world analogy

> A `@Async` method is like **delegating a task to a coworker via the office intercom system**. The intercom only relays messages coming from **outside your office**. If you shout the request from inside your own office (`this.method()`), the intercom (the AOP proxy) never hears it — you do the task yourself, synchronously.

### How Spring implements `@Async`

1. You annotate a bean method with `@Async`.
2. Spring wraps the bean in a **CGLIB / JDK dynamic proxy**.
3. External callers receive the **proxy**, not the bean.
4. When the proxy method is invoked, the `AsyncExecutionInterceptor` intercepts the call and hands the invocation to a `TaskExecutor`.
5. The interceptor returns immediately (or returns a `CompletableFuture` placeholder if declared).

Enable it:
```java
@Configuration
@EnableAsync                          // Turns on the proxy machinery
public class AsyncConfig {}
```

### 🚨 Trap 1 — Self-invocation

```java
@Service
public class ReportService {

    public void generateAll() {
        for (long id : ids) {
            sendReport(id);           // BUG: this.sendReport → bypasses the proxy → SYNCHRONOUS
        }
    }

    @Async
    public void sendReport(long id) { ... }
}
```

Because `this.sendReport(id)` calls the concrete class directly (not the proxy), the `@Async` interceptor is never triggered. **The code compiles, runs, and silently blocks.**

**Fixes (ranked by cleanliness):**
1. **Inject the bean into itself** and go through the proxy:
   ```java
   @Autowired ReportService self;
   public void generateAll() { for (long id : ids) self.sendReport(id); }
   ```
2. **Split into two beans** — `ReportOrchestrator` calls `ReportSender.sendReport(...)`. Cleaner architecture; no self-injection smell.
3. **AspectJ weaving** (`spring.aop.proxy-target-class=true` alone is NOT enough — you need `@EnableAsync(mode = AdviceMode.ASPECTJ)` plus load-time weaving). Rarely worth the operational cost.

**Interview one-liner:** *"`@Async` works through a Spring proxy, so any call routed via `this.` bypasses it and runs synchronously. Fix by going through the proxy — either self-injection or splitting responsibilities into two beans."*

### 🚨 Trap 2 — `@Async` on `private` or `final` methods

- CGLIB can only proxy **non-`final` public/protected** methods.
- JDK dynamic proxies only proxy **interface methods**.
- Annotating `private` or `final` silently does nothing. No warning at startup.

Rule: **`@Async` methods must be `public` and non-`final`**.

### 🚨 Trap 3 — Return type & exception loss

| Return type | What happens on error |
|---|---|
| `void` | Exception goes to a global `AsyncUncaughtExceptionHandler` (you must register one) |
| `Future<T>` / `CompletableFuture<T>` | Exception is stored in the future; caller sees it on `.get()`/`.join()` |

**If you annotate a `void` method and don't register a handler, exceptions are silently logged and lost.** Register one:

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean("appTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() { … }

    @Override public Executor getAsyncExecutor() { return taskExecutor(); }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            LoggerFactory.getLogger(AsyncConfig.class)
                .error("async void failure in {}: {}", method, ex.getMessage(), ex);
    }
}
```

### 🚨 Trap 4 — The default executor

If you don't specify an executor, Spring Boot 3.x looks for a bean named `taskExecutor` / `applicationTaskExecutor`. Historically, older Spring versions used `SimpleAsyncTaskExecutor` which **creates a new thread per task** — unbounded and catastrophic under load. Always name and inject your own:

```java
@Async("appTaskExecutor")
public CompletableFuture<Report> render(long id) { ... }
```

### 🚨 Trap 5 — Using `@Async` when the caller is already async

If your service is on WebFlux or you're already inside a virtual-thread request, sprinkling `@Async` on random methods just adds thread-hopping overhead and breaks context propagation. Use `@Async` when a **sync caller** needs to fire-and-forget or fork parallel work — not everywhere.

---

## 10.2 `ThreadPoolTaskExecutor` — The Production Configuration

### The Spring wrapper vs raw `ThreadPoolExecutor`

`ThreadPoolTaskExecutor` is a Spring bean-friendly wrapper around `ThreadPoolExecutor` with lifecycle hooks integrated into the Spring context. Same params, plus decorators + graceful-shutdown knobs.

### Production-shaped bean

```java
@Bean("appTaskExecutor")
public ThreadPoolTaskExecutor taskExecutor(MeterRegistry metrics) {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(Runtime.getRuntime().availableProcessors());
    exec.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 4);
    exec.setQueueCapacity(1000);                                        // BOUNDED
    exec.setKeepAliveSeconds(60);
    exec.setAllowCoreThreadTimeOut(true);
    exec.setThreadNamePrefix("app-async-");
    exec.setRejectedExecutionHandler(new CallerRunsPolicy());           // real backpressure
    exec.setWaitForTasksToCompleteOnShutdown(true);                     // drain on ctx close
    exec.setAwaitTerminationSeconds(30);
    exec.setTaskDecorator(new McdSecurityDecorator());                  // context propagation
    exec.initialize();
    // wire metrics — Micrometer has a built-in binder:
    new ExecutorServiceMetrics(exec.getThreadPoolExecutor(), "app-async", List.of()).bindTo(metrics);
    return exec;
}
```

**Every line is deliberate; be ready to defend each in the interview:**
- Bounded queue → no memory blowup.
- `CallerRunsPolicy` → the only rejection policy that implements true backpressure.
- `waitForTasksToCompleteOnShutdown(true) + awaitTerminationSeconds(30)` → graceful shutdown drains the queue before killing workers.
- Named prefix → thread dumps become readable.
- `TaskDecorator` → propagates MDC / Security context (see 10.3).
- Metrics binding → dashboards get pool utilization + queue depth for free.

### Graceful shutdown lifecycle

When Spring closes the context:
1. `stop()` — pool refuses new tasks (via `shutdown()`).
2. If `waitForTasksToCompleteOnShutdown` is true → drains the queue.
3. After `awaitTerminationSeconds` → calls `shutdownNow()`, interrupts stragglers.
4. Container waits until all `SmartLifecycle` beans stop.

Set `spring.lifecycle.timeout-per-shutdown-phase=30s` to align the container's grace window.

### Why not just annotate `@EnableAsync` with defaults?

Because defaults use `SimpleAsyncTaskExecutor` in older versions (or an application-wide shared `TaskExecutor` in newer). Explicit is always better. Own your pool; observe it; size it deliberately.

---

## 10.3 Context Propagation Across Async Boundaries

### The problem

Every request carries invisible thread-local context:
- **MDC** — SLF4J's Mapped Diagnostic Context. Holds `traceId`, `spanId`, `requestId`, `userId`.
- **`SecurityContextHolder`** — the authenticated principal.
- **`RequestContextHolder`** — the current `HttpServletRequest`.
- **Micrometer / OTel context** — trace spans.

When your `@Async` method executes on a different thread, **none of this comes along**. Logs lose their trace IDs; downstream calls fail auth checks; distributed traces break.

### The clean fix — `TaskDecorator`

Spring's `TaskDecorator` interface wraps every submitted `Runnable` before it's queued. Capture the context on the submitting thread; restore it on the worker; clean up in `finally`.

```java
public class ContextCopyingDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> mdc                       = MDC.getCopyOfContextMap();
        SecurityContext   security                    = SecurityContextHolder.getContext();
        RequestAttributes attributes                  = RequestContextHolder.getRequestAttributes();

        return () -> {
            Map<String, String> prevMdc = MDC.getCopyOfContextMap();
            SecurityContext   prevSec   = SecurityContextHolder.getContext();
            RequestAttributes prevAttr  = RequestContextHolder.getRequestAttributes();
            try {
                if (mdc != null) MDC.setContextMap(mdc); else MDC.clear();
                SecurityContextHolder.setContext(security);
                RequestContextHolder.setRequestAttributes(attributes);
                task.run();
            } finally {
                if (prevMdc != null) MDC.setContextMap(prevMdc); else MDC.clear();
                SecurityContextHolder.setContext(prevSec);
                RequestContextHolder.setRequestAttributes(prevAttr);
            }
        };
    }
}
```

Wire it into the executor:
```java
exec.setTaskDecorator(new ContextCopyingDecorator());
```

**Why `finally` restore matters:** on a pool thread, if we don't restore, the *next* task inherits our leaked context. Classic tenant-leak bug.

### Spring Security's own async support

- `@EnableAsync` + `SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)` — inherits at thread creation. **Does not work with pooled threads** (they were created before the request). Do not rely on it in a pool context.
- `DelegatingSecurityContextExecutor` — wraps an executor and propagates security context. Fine, but doesn't cover MDC or `RequestAttributes`. `TaskDecorator` is the general solution.

### Micrometer 1.10+ Context Propagation (`ContextSnapshot`)

The modern unified API for any thread-local-based context (MDC, security, trace, tenant):

```java
Runnable wrapped = ContextSnapshotFactory.builder().build().captureAll().wrap(task);
```

Works across `ExecutorService`, `Reactor`, and virtual threads. If your Spring Boot is 3.x+ with Micrometer 1.10+, use this instead of hand-rolled decorators.

### Virtual threads — do you still need this?

**Yes.** Even VTs are still `Thread`s, and `ThreadLocal` (including MDC and SecurityContext) is still per-thread. Only `ScopedValue` (Module 7) makes context inheritance automatic across VTs launched inside a `StructuredTaskScope`. Until Spring's context types migrate to `ScopedValue`, you still need `TaskDecorator` / `ContextSnapshot` on VT executors.

---

## 10.4 WebFlux vs Spring MVC + Virtual Threads

### Snapshot of the choice today (2026)

| Dimension | Spring MVC + Virtual Threads | Spring WebFlux (Reactor) |
|---|---|---|
| Programming model | Imperative, blocking-style | Reactive streams (`Mono`, `Flux`) |
| Throughput for typical CRUD | Excellent | Excellent |
| Native backpressure | ❌ (manual `Semaphore`) | ✅ `request(n)` protocol |
| Streaming (SSE, WebSocket, chunked) | Adequate | Excellent |
| Debuggability / stack traces | Native, familiar | Async fragments; needs Reactor operators |
| Library ecosystem | Every JDBC / JMS / REST client works | Requires reactive drivers (`R2DBC`, `WebClient`) |
| Learning curve | Zero | Steep |
| Best for | Request/response services, most CRUD APIs | Streaming APIs, event processing, gateway/proxy |

### The 2026 default

For **typical microservices** (request/response, DB + a few downstream calls):
> Spring Boot 3.x + `spring.threads.virtual.enabled=true` + Spring MVC + JDBC/JPA. Keep your imperative code. Gate downstreams with `Semaphore` (see Module 7).

For **streaming pipelines** (real-time feeds, SSE broadcast, WebSocket fanout, API gateway with adaptive load-shedding):
> Spring WebFlux. Backpressure is a first-class concern; Reactor's operator model is the right tool.

### The migration honesty

- Moving MVC → WebFlux is a **whole-app rewrite**. Every layer, from repositories to filters, must be reactive-aware. `.block()` in a reactive chain is a P0 outage.
- Moving to VT-enabled MVC is **one property flag** plus downstream-gating audits. Immediate scalability win.

### Reactive still shines when…

- You need **fan-in from many sources with backpressure** (Kafka, SSE, WebSocket).
- Your workload is **naturally a pipeline of transformations** (Flux operators are cleaner than for-loops).
- You already have deep Reactor expertise in the team.

### Common WebFlux mistakes to name

| Mistake | Effect |
|---|---|
| `.block()` inside a reactive chain | Occupies an event-loop thread → whole app stalls |
| Using JDBC / JPA inside `Mono.fromCallable` without `.subscribeOn(Schedulers.boundedElastic())` | Blocks event-loop |
| `subscribeOn` vs `publishOn` mix-ups | Threading model becomes unpredictable |
| Not carrying context — Reactor uses `Context`, not `ThreadLocal` | Traces / MDC lost |

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | Bad answer | L5 answer |
|---|---|---|
| "Why isn't my `@Async` running async?" | "It should be." | "Self-invocation — `this.method()` bypasses the proxy. Fix by going through the proxy: self-injection or splitting the bean." |
| "Best default executor for `@Async`?" | "Spring's default." | "Never the default. Register a bounded `ThreadPoolTaskExecutor` with `CallerRunsPolicy`, named prefix, graceful shutdown, and a `TaskDecorator`." |
| "How to propagate MDC to `@Async`?" | "It's automatic." | "It isn't. Use a `TaskDecorator` (or Micrometer `ContextSnapshot`) that captures MDC/SecurityContext/RequestAttributes on the submitter and restores in `finally` on the worker." |
| "Where do `@Async void` exceptions go?" | "Bubble up." | "Nowhere by default — they get swallowed. Register an `AsyncUncaughtExceptionHandler`, or return `CompletableFuture` and handle on `.get()`." |
| "MVC vs WebFlux today?" | "WebFlux, it's newer." | "For request/response CRUD, MVC + virtual threads is the default in 2026. WebFlux for streaming and true backpressure." |
| "`SimpleAsyncTaskExecutor` OK for prod?" | "Sure." | "No — historically it created a new thread per task, unbounded. Always configure a bounded pool bean." |
| "Can I put `@Async` on a `private` method?" | "Yes." | "No — CGLIB can't proxy `private`/`final`. Silent no-op." |

---

## 💻 L5-Grade Code Examples

### Example 1 — Complete `@Async` configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean("appTaskExecutor")
    public ThreadPoolTaskExecutor appTaskExecutor(MeterRegistry metrics) {
        var exec = new ThreadPoolTaskExecutor();
        int cores = Runtime.getRuntime().availableProcessors();
        exec.setCorePoolSize(cores);
        exec.setMaxPoolSize(cores * 4);
        exec.setQueueCapacity(1000);
        exec.setKeepAliveSeconds(60);
        exec.setAllowCoreThreadTimeOut(true);
        exec.setThreadNamePrefix("app-async-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.setTaskDecorator(new ContextCopyingDecorator());
        exec.initialize();
        new ExecutorServiceMetrics(exec.getThreadPoolExecutor(), "app-async", List.of())
            .bindTo(metrics);
        return exec;
    }

    @Override public Executor getAsyncExecutor() { return appTaskExecutor(null); }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        var log = LoggerFactory.getLogger(AsyncConfig.class);
        return (ex, method, params) ->
            log.error("async void failure in {}#{}: {}",
                method.getDeclaringClass().getSimpleName(), method.getName(),
                ex.getMessage(), ex);
    }
}
```

### Example 2 — Fixing self-invocation

**Bad:**
```java
@Service
public class Orchestrator {
    @Async("appTaskExecutor")
    public CompletableFuture<Void> send(long id) { … }

    public void run() {
        ids.forEach(this::send);      // SYNC — bypasses proxy
    }
}
```

**Good:**
```java
@Service
public class Orchestrator {
    @Autowired private Sender sender;                  // separate bean

    public void run() {
        List<CompletableFuture<Void>> futures = ids.stream()
            .map(sender::send).toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }
}

@Service
public class Sender {
    @Async("appTaskExecutor")
    public CompletableFuture<Void> send(long id) { … return CompletableFuture.completedFuture(null); }
}
```

### Example 3 — Propagating tenant + trace context

```java
public class ContextCopyingDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> mdc      = MDC.getCopyOfContextMap();
        SecurityContext security     = SecurityContextHolder.getContext();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        return () -> {
            var prevMdc  = MDC.getCopyOfContextMap();
            var prevSec  = SecurityContextHolder.getContext();
            var prevAttr = RequestContextHolder.getRequestAttributes();
            try {
                if (mdc != null) MDC.setContextMap(mdc); else MDC.clear();
                SecurityContextHolder.setContext(security);
                RequestContextHolder.setRequestAttributes(attributes);
                task.run();
            } finally {
                if (prevMdc != null) MDC.setContextMap(prevMdc); else MDC.clear();
                SecurityContextHolder.setContext(prevSec);
                RequestContextHolder.setRequestAttributes(prevAttr);
            }
        };
    }
}
```

Modern equivalent with Micrometer Context Propagation:
```java
exec.setTaskDecorator(task ->
    ContextSnapshotFactory.builder().build().captureAll().wrap(task));
```

### Example 4 — Enabling virtual threads in Spring Boot 3.2+

```properties
spring.threads.virtual.enabled=true
```

That single flag routes Tomcat's request-executor to VTs. Then audit every downstream:

```java
@Bean
public Semaphore hikariGate(DataSourceProperties props) {
    return new Semaphore(props.getHikari().getMaximumPoolSize());
}

@Service
public class UserService {
    @Autowired Semaphore hikariGate;
    @Autowired JdbcTemplate jdbc;

    public User load(long id) throws InterruptedException {
        hikariGate.acquire();
        try { return jdbc.queryForObject(...); }
        finally { hikariGate.release(); }
    }
}
```

Say aloud in the interview: *"VTs make the servlet layer scale, but downstream Hikari is unchanged. The Semaphore gates VT concurrency to whatever the DB can actually handle."*

### Example 5 — WebFlux JDBC-in-Mono done correctly

```java
public Mono<User> loadUser(long id) {
    return Mono.fromCallable(() -> jdbc.queryForObject(SQL, mapper, id))
               .subscribeOn(Schedulers.boundedElastic());        // never on the event loop
}
```

Without `.subscribeOn(boundedElastic())`, this call blocks a Reactor event-loop thread → whole app stalls. Classic mistake.

---

## 🏭 Production War Stories

**1. The `@Async` that ran on the servlet thread.**
Team wrote `orchestrator.send(id)` inside a loop calling `this::send`. Every "async" batch actually blocked the request thread for 8 seconds. Users saw 30-second timeouts. Fix: split into two beans; batch fanned out on the pool for real.

**2. The `SimpleAsyncTaskExecutor` OOM.**
Legacy Spring app had `@EnableAsync` with no custom executor. `SimpleAsyncTaskExecutor` (default in that version) created one **new** thread per task. Under a downstream slowdown, the app spawned 12,000 threads. Native memory (thread stacks × 1 MB) exceeded RSS budget → JVM killed by OOM-killer. Fix: replaced default with a bounded `ThreadPoolTaskExecutor`.

**3. The tenant-context leak on pool threads.**
Payment service had a multi-tenant `TenantContext` (a `ThreadLocal`). No `TaskDecorator`. A worker finished tenant A's request without clearing; next request for tenant B inherited A's tenant, wrote to the wrong DB shard. Discovered only after a customer support ticket. Fix: `TaskDecorator` restore-in-`finally`; added an audit log check.

**4. The MDC that vanished mid-request.**
Log-searching for trace ID `abc-123` returned only half the expected lines. Cause: mid-request `@Async` fanout on a pool with no `TaskDecorator` — MDC didn't propagate. Trace was silently broken. Fix: `TaskDecorator` on every executor bean; standardized filter that puts `traceId` in MDC on entry.

**5. The WebFlux `.block()` incident.**
A WebFlux service called a legacy library that internally used `.block()`. Under low load, fine. Under 500 rps, Reactor's 8 event-loop threads all sat in `.block()` waits → total stall. p99 latency went from 20 ms to 30 s. Fix: wrapped the legacy call in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.

**6. The graceful shutdown that wasn't.**
K8s SIGTERM. Spring context started closing. Pool didn't have `setWaitForTasksToCompleteOnShutdown(true)`. In-flight tasks were interrupted mid-DB-txn → orphaned rows. Fix: enabled the flag, aligned `spring.lifecycle.timeout-per-shutdown-phase` with K8s' `terminationGracePeriodSeconds`.

---

## 🎯 Self-Check

1. Why does `this.asyncMethod()` run synchronously? Two fixes.
2. What visibility must an `@Async` method have? Why?
3. Where does an exception in an `@Async void` go by default? How do you catch it?
4. List 5 non-default settings you'd put on a production `ThreadPoolTaskExecutor`.
5. Which rejection policy gives real backpressure? What's the tradeoff?
6. How do you propagate MDC + SecurityContext to an async pool thread?
7. Why is `SimpleAsyncTaskExecutor` dangerous in production?
8. Give the 2026 decision rule for MVC + VT vs WebFlux.
9. Why is `.block()` in WebFlux a P0?
10. Enabling virtual threads in Spring Boot — what property, and what must you audit afterwards?

---

## 🏁 Course Complete

You've now covered the entire L5 concurrency curriculum:

| # | Module | Focus |
|---|---|---|
| 1 | [OS & Memory Foundations](./Module-01-OS-Memory-Foundations.md) | Process/thread, IPC, cache, JMM |
| 2 | [Locks & AQS](./Module-02-Locks-and-AQS.md) | `synchronized`, `ReentrantLock`, AQS internals |
| 3 | [Lock-Free & Atomics](./Module-03-LockFree-and-Atomics.md) | CAS, ABA, `LongAdder`, `VarHandle` |
| 4 | [Machine Coding Pack](./Module-04-Machine-Coding.md) | BBQ, thread pool, LRU, rate limiter, LC set |
| 5 | [Concurrent Collections](./Module-05-Concurrent-Collections.md) | CHM, blocking queues, CoW, SkipListMap |
| 6 | [Thread Pools & Async](./Module-06-ThreadPools-and-Async.md) | TPE sizing, ForkJoin, `CompletableFuture` |
| 7 | [Virtual Threads / Loom](./Module-07-Virtual-Threads-Loom.md) | M:N, pinning, downstream exhaustion, `ScopedValue` |
| 8 | [Distributed Concurrency](./Module-08-Distributed-Concurrency.md) | Redlock, fencing tokens, MVCC, isolation |
| 9 | [Production Diagnostics](./Module-09-Production-Diagnostics.md) | Thread dumps, JFR, async-profiler |
| 10 | [Spring Concurrency](./Module-10-Spring-Concurrency.md) | `@Async` traps, MDC propagation, MVC+VT vs WebFlux |

### Suggested review cadence

- **Daily (30 min):** one machine-coding problem from Module 4, from scratch, no notes.
- **Weekly:** redraw one module's mind map on paper; answer all its self-check questions aloud.
- **Bi-weekly:** pick a war story from any module and defend the fix.
- **Before the loop:** speed-run the mind maps for Modules 1, 2, 3, 6, 9. These are your fundamentals.

Good luck — you've now got the material an L5 loop actually asks for, no more and no less.
