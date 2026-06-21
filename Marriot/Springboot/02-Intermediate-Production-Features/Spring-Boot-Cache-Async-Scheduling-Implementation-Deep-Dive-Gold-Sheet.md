# Spring Boot Cache Async Scheduling Implementation Deep Dive Gold Sheet

> Track: Spring Boot Interview Track - Intermediate Production Features  
> Goal: deepen caching, async execution, scheduling, and startup lifecycle into production-ready implementation judgment.

Read after the Cache/Async/Scheduling/Events/Startup master sheet.

---

## 1. Runtime Mental Model

Caching, async execution, scheduling, and events are convenience features until production
load arrives. Then they become resource-management problems.

Strong answer:

```text
Spring annotations like @Cacheable, @Async, and @Scheduled are entry points. Production
readiness comes from key design, executor sizing, locking, idempotency, observability, and
failure handling.
```

---

## 2. Cache Choice: Caffeine vs Redis

| Cache | Best For | Trade-Off |
|---|---|---|
| Caffeine/local | very fast per-instance cache | each pod has different data |
| Redis/distributed | shared cache across instances | network hop and Redis operations |
| HTTP/CDN cache | public/read-heavy responses | invalidation and personalization limits |

Interview answer:

```text
I use Caffeine for small, local, low-latency caches where per-instance inconsistency is OK.
I use Redis when cache state must be shared across service instances.
```

---

## 3. Cache Key Design

Bad key:

```text
hotelId only
```

Better key:

```text
hotelId + checkIn + checkOut + guests + currency + tenantId
```

Rules:

- include all inputs that affect result
- include tenant/user boundary when data differs
- avoid unbounded high-cardinality keys when possible
- define TTL by business freshness need
- track hit/miss/eviction metrics

Strong answer:

```text
Cache correctness depends on key design. Missing a key dimension can leak stale or wrong
data across users, tenants, dates, or currencies.
```

---

## 4. Cache Invalidation

Common strategies:

| Strategy | Use When |
|---|---|
| TTL | data can be stale for bounded time |
| explicit eviction | write path knows affected key |
| event-driven invalidation | writes happen in another service |
| versioned key | schema/data generation changes |
| refresh ahead | read-heavy hot keys need warm data |

Trap:

```text
Cache invalidation is part of the write design, not an afterthought.
```

---

## 5. Cache Stampede

Stampede happens when many requests miss the same key and all recompute/fetch at once.

Mitigations:

- per-key locking
- request coalescing
- jittered TTL
- refresh ahead
- stale-while-revalidate
- rate limit expensive recompute

Strong answer:

```text
For hot keys, I avoid letting every cache miss hit the database or downstream service at
once. I use per-key coalescing, TTL jitter, or refresh-ahead depending on freshness needs.
```

---

## 6. `@Cacheable` Traps

Common traps:

- self-invocation bypasses proxy
- caching null/error responses accidentally
- wrong key expression
- no TTL configured at provider level
- mutable cached values
- caching user-specific data without user/tenant key
- forgetting eviction after writes

Proxy trap:

```text
A method in the same class calling another @Cacheable method does not go through the Spring
proxy, so cache advice may not apply.
```

---

## 7. Async Executor Sizing

Default async executor can be unsafe for production.

Configure intentionally:

```java
@Bean
ThreadPoolTaskExecutor applicationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(16);
    executor.setMaxPoolSize(64);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("app-async-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    return executor;
}
```

Questions to answer:

- Is work CPU-bound or I/O-bound?
- What happens when queue fills?
- How are failures logged/observed?
- Is context propagated?
- Is shutdown graceful?

---

## 8. `@Async` Traps

Traps:

- self-invocation bypasses proxy
- exceptions in `void` async methods get lost unless handler configured
- transaction context does not automatically continue
- SecurityContext/MDC/correlation context may not propagate
- unbounded executor creates memory/latency issues
- async does not make CPU work free

Strong answer:

```text
I use @Async only with a named, bounded executor and clear failure handling. I do not assume
transactions, security context, or MDC automatically behave like the caller thread.
```

---

## 9. Context Propagation

Async work often needs:

- correlation id
- trace context
- security/user context when appropriate
- tenant id
- locale/request metadata if relevant

Options:

- `TaskDecorator`
- Micrometer context propagation
- explicit command object fields
- tracing instrumentation

Strong answer:

```text
For async tasks, I explicitly propagate only the context required for observability and
correctness. I avoid accidentally leaking request-scoped state into background work.
```

---

## 10. Scheduling In One Instance vs Many Instances

`@Scheduled` runs in every app instance.

Problem:

```text
If 5 pods run the same service, the scheduled cleanup may run 5 times.
```

Solutions:

| Solution | Use When |
|---|---|
| ShedLock | simple distributed lock around scheduled method |
| Quartz cluster mode | richer scheduling/retry/history needs |
| Kubernetes CronJob | platform-owned periodic job |
| message queue worker | work can be partitioned/queued |
| single leader election | one active scheduler instance |

Strong answer:

```text
In multi-instance deployments, @Scheduled needs coordination. Otherwise every replica runs
the same job.
```

---

## 11. ShedLock

ShedLock prevents duplicate scheduled job execution across instances.

Concept:

```text
Before job runs, acquire lock in shared store. Only lock holder executes.
```

Good for:

- simple scheduled cleanup
- periodic reconciliation
- low-frequency jobs

Not ideal for:

- complex job orchestration
- heavy parallel processing
- long-running business workflows with retries/history

---

## 12. Quartz

Quartz supports richer scheduling:

- persistent jobs
- triggers
- calendars
- clustered execution
- retry/misfire handling
- job history

Use when:

```text
Scheduling itself is a domain concern or needs persistence, clustering, and operational
control beyond @Scheduled.
```

---

## 13. Transactional Events

`@TransactionalEventListener` can run event handling after transaction phase.

Example phases:

- BEFORE_COMMIT
- AFTER_COMMIT
- AFTER_ROLLBACK
- AFTER_COMPLETION

Important:

```text
If you send email inside a normal event listener before commit, the transaction may roll
back after the email is sent. Use AFTER_COMMIT or outbox for reliable external effects.
```

Strong answer:

```text
For local in-process reactions after commit, TransactionalEventListener can help. For
cross-service reliability, I use outbox rather than relying only on in-memory events.
```

---

## 14. Startup Lifecycle Hooks

Common hooks:

| Hook | Use |
|---|---|
| `ApplicationRunner` | run code after app starts |
| `CommandLineRunner` | command-line startup tasks |
| `SmartLifecycle` | phase-based start/stop |
| `ApplicationReadyEvent` | app ready event |
| `@PostConstruct` | bean initialization, not full app ready |

Avoid:

- heavy network calls in bean constructors
- long blocking work before readiness without clear startup probe
- startup tasks that cannot be retried safely

---

## 15. Observability Metrics

Track:

- cache hit/miss/eviction rate
- Redis latency/error rate
- async executor active threads
- async queue depth
- rejected tasks
- scheduled job duration/failure count
- lock acquisition failures
- startup time
- event listener errors

Strong answer:

```text
Background features need metrics too. A service can look healthy while async queues are full
or scheduled jobs are silently failing.
```

---

## 16. Strong Closing Answer

```text
For Spring Boot cache, async, and scheduling features, I design beyond the annotation: correct
cache keys and invalidation, stampede protection, bounded executors, async context and error
handling, distributed scheduling with ShedLock/Quartz/CronJobs when needed, and metrics for
cache health, executor saturation, and job failures.
```
