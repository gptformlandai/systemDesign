# Spring Boot Production Debugging Runbooks JFR Hikari Platinum Sheet

> Track: Spring Boot Interview Track - MAANG Platinum Scenarios  
> Goal: turn Spring Boot production debugging into runbooks with metrics, commands, and prevention.

Read after Production Debugging Case Studies and Production Runtime.

---

## 1. Incident Mindset

Debugging order:

```text
impact -> scope -> recent change -> metrics/traces/logs -> hypothesis -> mitigation -> root cause -> prevention
```

Do not start with random code edits.

Strong answer:

```text
In production, I first reduce customer impact, then root-cause with evidence. Spring Boot
Actuator, Micrometer, traces, thread dumps, heap dumps, JFR, and database metrics guide the
investigation.
```

---

## 2. First 10 Minutes Runbook

1. Confirm alert and user impact.
2. Identify affected service, region, version, tenant, endpoint.
3. Check recent deploy/config/secret/migration changes.
4. Inspect RED metrics: rate, errors, duration.
5. Inspect saturation: CPU, memory, DB pool, thread pools, queues.
6. Use traces to locate slow/failing spans.
7. Mitigate: rollback, scale, shed load, disable flag, fail over.
8. Preserve evidence for root cause.

Strong answer:

```text
I separate mitigation from root cause. If customer impact is active, rollback or traffic shift
can be correct before fully understanding the bug.
```

---

## 3. Hikari Pool Exhaustion Runbook

Symptoms:

- p99 latency spike
- `Connection is not available, request timed out`
- Hikari pending threads high
- active connections at max
- DB CPU or locks high

Metrics:

- `hikaricp.connections.active`
- `hikaricp.connections.idle`
- `hikaricp.connections.pending`
- `hikaricp.connections.timeout`
- query duration
- transaction duration

Debug path:

1. Check pool active/pending/timeout.
2. Check slow queries and missing indexes.
3. Check long transactions.
4. Check connection leaks.
5. Check recent traffic/deploy.
6. Check DB saturation before raising pool size.

Strong answer:

```text
If Hikari is exhausted, I do not blindly increase pool size. I check whether queries are slow,
transactions are too long, connections leak, or the database is saturated.
```

---

## 4. High CPU Runbook

Symptoms:

- CPU near limit
- high latency
- throttling if containerized
- thread dumps show busy threads

Tools:

- thread dump
- JFR
- async-profiler if available
- Micrometer JVM/process CPU metrics
- Kubernetes CPU throttling metrics

Debug path:

1. Check if CPU is app or GC.
2. Capture thread dump/JFR during spike.
3. Identify hot methods or tight loops.
4. Check serialization, regex, JSON mapping, crypto, compression.
5. Check traffic shape and expensive endpoints.
6. Mitigate with rollback, rate limit, feature flag, scale if CPU-bound and scalable.

---

## 5. Memory Leak Runbook

Symptoms:

- heap grows over time
- GC frequency increases
- OOMKilled or `OutOfMemoryError`
- pod restarts

Tools:

- heap dump
- GC logs
- Micrometer JVM memory metrics
- JFR allocation profile
- container memory events

Likely causes:

- unbounded cache/map
- storing request objects in static collection
- high-cardinality metrics tags
- large response buffering
- thread-local leak
- classloader leak in unusual deployments

Strong answer:

```text
For memory leaks, I compare heap over time, capture a heap dump, inspect dominant retainers,
and check recent changes such as caching, metrics tags, or large payload handling.
```

---

## 6. Thread Pool Starvation Runbook

Symptoms:

- requests hang
- async tasks delayed
- executor queue full
- low CPU but high latency

Check:

- Tomcat/Jetty/Undertow thread usage
- `@Async` executor active/queue/rejected
- scheduler thread pool
- DB pool waits
- blocked downstream calls

Fix options:

- add timeouts
- isolate executors by workload
- reduce blocking work
- backpressure/load shedding
- right-size pool and queue
- move long work to queue/batch

---

## 7. GC Pause Runbook

Symptoms:

- periodic latency spikes
- GC pause metrics high
- allocation rate high
- CPU spent in GC

Check:

- JVM GC metrics
- heap occupancy
- allocation rate
- object churn from JSON/DTO mapping
- large batches/responses
- memory limits

Strong answer:

```text
GC spikes are often allocation or memory-pressure problems. I check allocation rate and heap
behavior before changing collectors blindly.
```

---

## 8. Slow API Runbook

Debug path:

1. Compare p50/p95/p99.
2. Split by endpoint, version, region, tenant.
3. Inspect trace spans.
4. Check DB query time and pool wait.
5. Check downstream HTTP spans.
6. Check serialization payload size.
7. Check cache hit rate.
8. Check CPU throttling/GC.

Common causes:

- N+1 query
- missing index
- DB lock contention
- downstream timeout/retry
- huge payload
- cache miss/stampede
- thread pool starvation

---

## 9. Startup Failure Runbook

Useful evidence:

- first root cause exception
- active profiles
- environment variables
- condition evaluation report
- migration logs
- port binding logs
- dependency readiness

Common causes:

- missing bean
- bad property binding
- invalid profile config
- failed migration
- DB unavailable
- duplicate bean
- classpath mismatch after Boot upgrade

Strong answer:

```text
For startup failures, I read the first meaningful exception and inspect profiles, config,
auto-configuration conditions, migrations, and classpath compatibility.
```

---

## 10. Security Failure Runbook

Symptoms:

- 401 spike
- 403 spike
- only some tenants fail
- after key rotation/deploy

Debug:

- token issuer/audience
- JWKS key id
- clock skew
- scopes/authorities mapping
- method security expression
- gateway vs service behavior
- tenant claim/filter/cache key

Strong answer:

```text
I separate authentication failures from authorization failures. 401 usually means identity
token validation failed; 403 means identity exists but lacks permission.
```

---

## 11. Actuator Endpoint Safety

Useful endpoints:

- health
- metrics
- prometheus
- loggers
- mappings
- conditions
- configprops
- threaddump
- heapdump

Safety rules:

- expose only needed endpoints
- secure sensitive endpoints
- never expose secrets
- restrict heap/thread dump access
- audit runtime log-level changes

---

## 12. JFR In Interviews

Java Flight Recorder helps capture:

- CPU hotspots
- allocation profile
- lock contention
- file/socket I/O
- thread states
- GC pauses

Strong answer:

```text
JFR is useful because it captures low-overhead JVM evidence during production-like incidents,
especially CPU, allocation, lock, and GC behavior.
```

---

## 13. Dashboard Panels For Spring Boot

Minimum panels:

- request rate/error/latency by endpoint and status
- JVM heap/non-heap
- GC pause/allocation
- CPU and throttling
- Hikari active/idle/pending/timeouts
- thread pool active/queue/rejected
- downstream client latency/error
- cache hit/miss/eviction
- Kafka lag/DLT if consumer
- deployment version annotations

---

## 14. Postmortem Quality Bar

A good postmortem includes:

- customer impact
- timeline
- detection signal
- root cause
- contributing factors
- mitigation
- what worked
- what did not work
- prevention actions with owners
- alert/runbook/test improvements

Avoid:

```text
human blame without system fix
```

---

## 15. Strong Closing Answer

```text
For Spring Boot production debugging, I use Actuator/Micrometer for service and JVM signals,
traces for request path, logs for code-level evidence, Hikari metrics for DB pressure, thread
and heap dumps plus JFR for JVM internals, and version/config annotations to connect incidents
to recent changes. I mitigate first, then root-cause and prevent recurrence.
```
