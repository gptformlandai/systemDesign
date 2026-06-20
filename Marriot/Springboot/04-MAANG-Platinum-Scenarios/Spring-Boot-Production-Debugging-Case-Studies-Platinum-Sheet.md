# Spring Boot Production Debugging Case Studies Platinum Sheet

Target: senior Spring Boot interviews, production support rounds, and MAANG-style backend
ownership discussions.

This sheet teaches how to debug Spring Boot incidents from symptoms to root cause.

---

## 0. Debugging Mindset

Do not start with random code changes. Start with evidence.

```text
Symptom -> blast radius -> recent change -> metrics/logs/traces -> hypothesis -> safe
mitigation -> root cause -> prevention
```

Spring Boot tools:

- Actuator health
- Actuator metrics
- Actuator loggers
- Actuator mappings
- Actuator conditions
- thread dump
- heap dump
- structured logs
- distributed traces
- database slow query logs

---

# 1. Case Study: Application Fails To Start

## Symptom

```text
ApplicationContext failed to start.
```

## Common Causes

| Cause | Signal |
|---|---|
| missing bean | `NoSuchBeanDefinitionException` |
| duplicate bean | `NoUniqueBeanDefinitionException` |
| circular dependency | bean currently in creation |
| bad config | bind/validation exception |
| port in use | embedded server startup failure |
| DB unavailable | DataSource or migration failure |
| failed Flyway/Liquibase migration | migration exception |

## Debug Checklist

1. Read the first meaningful exception, not only the final wrapper.
2. Check active profiles.
3. Check config properties and environment variables.
4. Check component scan package.
5. Check auto-configuration condition report.
6. Check DB/migration dependency.
7. Reproduce locally with same profile/config.

Strong answer:

```text
For startup failures, I read the root cause, verify profile/config/classpath, inspect the
condition report, and check migrations or external dependencies before changing code.
```

---

# 2. Case Study: `@Transactional` Not Working

## Symptom

```text
Data partially commits even though method failed.
```

## Likely Causes

- self-invocation bypassed proxy
- method is private/final
- checked exception thrown without `rollbackFor`
- annotation placed on wrong layer
- transaction ended before async work
- external call inside transaction caused timeout/partial workflow confusion

## Debug Checklist

1. Confirm method call goes through Spring bean proxy.
2. Confirm exception type.
3. Confirm transaction manager.
4. Check logs for transaction begin/commit/rollback.
5. Check database writes and flush behavior.
6. Keep transaction boundary around DB changes only.

Fix example:

```java
@Transactional(rollbackFor = PaymentException.class)
public void confirmBooking(ConfirmRequest request) throws PaymentException {
    bookingRepository.markConfirmed(request.bookingId());
    paymentClient.capture(request.paymentId());
}
```

Better design:

```text
Do not call payment provider inside a DB transaction. Use pending state, outbox, saga, and
idempotency.
```

---

# 3. Case Study: API Is Slow

## Symptom

```text
GET /bookings p99 increased from 250 ms to 4 seconds.
```

## Debug Path

1. Check p50 vs p99.
2. Check traces for slow spans.
3. Check DB query time and row count.
4. Check connection pool wait.
5. Check thread pool saturation.
6. Check serialization payload size.
7. Check downstream HTTP calls.

## Common Causes

| Cause | Evidence |
|---|---|
| N+1 query | many similar SQL queries per request |
| missing index | sequential scan / high rows scanned |
| connection pool exhausted | Hikari pending threads |
| slow downstream | trace span slow |
| huge response body | serialization/network time high |
| lock contention | DB wait/lock metrics |

Mitigation:

- add pagination
- use DTO projection
- fix fetch plan
- add verified index
- tune pool only after query/root cause is known
- cache safe read-heavy data

---

# 4. Case Study: Connection Pool Exhaustion

## Symptom

```text
Requests hang, then fail with timeout waiting for database connection.
```

## Causes

- long transactions
- slow queries
- too many app replicas with too-large pools
- connection leak
- batch job consuming pool
- downstream calls inside transaction

## Debug Checklist

1. Check Hikari active/idle/pending metrics.
2. Check transaction duration.
3. Check slow queries.
4. Check pool size across all replicas.
5. Check DB max connections.
6. Check whether code closes manual connections.

Interview line:

```text
Increasing pool size can make the database incident worse. I first identify why connections
are held too long.
```

---

# 5. Case Study: High CPU

## Symptom

```text
CPU jumps to 95 percent after a traffic spike or deployment.
```

## Debug Path

1. Compare traffic vs CPU.
2. Take thread dump or profile sample.
3. Check hot endpoints.
4. Check JSON serialization, regex, loops, compression.
5. Check GC CPU.
6. Check logging volume.

Common causes:

- inefficient loop
- large object serialization
- expensive regex
- hot logging path
- compression under load
- cryptography/token verification cost
- GC pressure

Mitigation:

- rollback bad deploy
- reduce expensive feature
- add rate limiting
- cache repeated computation
- optimize hot path after profiling

---

# 6. Case Study: Memory Leak Or OOM

## Symptom

```text
Pod restarts with OOMKilled or JVM OutOfMemoryError.
```

## Debug Path

1. Check heap vs container memory limit.
2. Check GC logs and heap usage trend.
3. Capture heap dump if safe.
4. Check caches, maps, queues, request buffers.
5. Check large file reads into memory.
6. Check native memory/direct buffers/thread count.

Common causes:

- unbounded cache
- unbounded queue
- storing request objects in static collection
- loading huge file into memory
- too many threads
- response aggregation without limits

Prevention:

- bounded caches
- queue limits
- streaming file processing
- load tests with heap monitoring
- container-aware memory sizing

---

# 7. Case Study: 401 vs 403 Confusion

## Symptom

```text
Users with valid token cannot access endpoint.
```

## Debug Path

1. Is token missing/invalid/expired? If yes, 401.
2. Is token valid but authority insufficient? If yes, 403.
3. Check SecurityFilterChain order.
4. Check request matcher.
5. Check role prefix `ROLE_`.
6. Check method security.
7. Check CORS only if browser blocks response.

Strong answer:

```text
401 means unauthenticated. 403 means authenticated but not authorized. I inspect filter
chain, token validation, authorities, matchers, and method security.
```

---

# 8. Case Study: Kafka Consumer Reprocessing

## Symptom

```text
Same event processed multiple times after restart.
```

## Causes

- offset committed after processing and crash happened before commit
- handler not idempotent
- retry topic sends duplicate
- rebalance during long processing

Fixes:

- idempotent consumer with processed event table
- unique constraint on business action
- commit strategy understood
- bounded processing time
- DLQ after retries

Interview line:

```text
At-least-once delivery means duplicate processing is normal. Business handler must be
idempotent.
```

---

# 9. Case Study: Cache Returning Stale Data

## Symptom

```text
Booking status changed, but API still returns old status.
```

## Causes

- missing eviction
- wrong cache key
- local cache in multi-instance app
- update and cache not in same consistency model
- event-driven invalidation delayed

Fixes:

- define TTL
- evict on write path
- use distributed cache for multi-instance consistency
- include tenant/user/version in key
- tolerate stale reads only where business allows

---

# 10. Case Study: Actuator Health Causes Restart Storm

## Symptom

```text
Temporary database outage causes all pods to restart repeatedly.
```

## Cause

Liveness probe depends on database health.

Better:

```text
Liveness: JVM/app process is alive.
Readiness: app can serve traffic and critical dependencies are available.
```

Strong answer:

```text
A dependency outage should usually make readiness fail, not liveness. Restarting healthy
processes during a DB outage amplifies the incident.
```

---

# 11. Case Study: Native Image Works Locally, Fails In Prod

## Symptom

```text
Native image starts fast, but reflection-based library or serialization fails.
```

## Causes

- reflection metadata missing
- resource not included
- dynamic proxy not configured
- library not native-image friendly
- runtime classpath scanning assumption

Debug:

- compare JVM vs native behavior
- inspect native image hints
- add runtime hints where needed
- add native integration tests

Interview line:

```text
Native image is a deliberate deployment choice. I validate reflection, proxies, resources,
and integration behavior with native tests.
```

---

# 12. Production Debugging Checklist

```text
Startup failure -> root cause, profiles, properties, condition report.
Transaction issue -> proxy path, exception type, rollback rule.
Slow API -> traces, SQL, pool wait, payload, downstream.
DB pool exhausted -> long transactions, slow queries, pool math.
High CPU -> profile, hot endpoint, serialization, regex, GC.
Memory leak -> heap dump, unbounded structures, container limits.
Security issue -> 401 vs 403, filter chain, matchers, authorities.
Kafka duplicate -> idempotency and offset behavior.
Stale cache -> key, TTL, eviction, multi-instance behavior.
Probe issue -> readiness vs liveness separation.
```

---

# 13. Interview Question

> A Spring Boot booking API became slow after release. How would you debug?

Strong answer:

```text
I would first identify blast radius by endpoint, version, region, and tenant. Then I would
check p50/p95/p99, traces, logs, and recent deploy/config/schema changes. For a booking API,
I would inspect SQL spans for N+1 or missing indexes, Hikari pool wait, downstream payment or
availability calls, and payload size. If the new version caused it, I would rollback or
disable the feature flag. After mitigation, I would add a regression test, dashboard, and
alert on p99 latency and pool wait.
```

---

# 14. Official Source Notes

- Spring Boot Actuator endpoints: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Boot auto-configuration diagnostics: https://docs.spring.io/spring-boot/reference/using/auto-configuration.html
- Spring Framework transactions: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
- Spring Boot native images: https://docs.spring.io/spring-boot/reference/packaging/native-image/index.html
