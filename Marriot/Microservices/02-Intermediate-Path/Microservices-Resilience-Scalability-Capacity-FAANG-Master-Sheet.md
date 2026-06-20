# Microservices Resilience Scalability Capacity FAANG Master Sheet

Target: starter, intermediate, senior, and FAANG-level microservices interviews.

This sheet covers:
- timeout
- retry with backoff
- circuit breaker
- bulkhead
- rate limiter
- load shedding
- backpressure
- capacity planning
- scaling bottlenecks
- SLI/SLO thinking
- cascading failure
- graceful degradation

Goal:

```text
After reading this sheet, you should be able to design microservices that survive slow
dependencies, traffic spikes, partial outages, retry storms, and capacity limits.
```

---

## 0. How To Use This Guide By Level

| Level | What To Focus On |
|---|---|
| Starter | timeout, retry, circuit breaker |
| Intermediate | bulkhead, rate limiter, DLQ, fallback |
| Senior | retry storms, load shedding, capacity math, SLOs |
| FAANG-ready | cascading failure prevention, overload control, graceful degradation |

Must-say line:

```text
Resilience is not one pattern. It is a system of timeouts, retries, isolation, limits,
fallbacks, observability, and capacity planning.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Timeout | Very high | Prevent stuck threads |
| Retry | Very high | Transient failure recovery |
| Backoff and jitter | Very high | Prevent retry storms |
| Circuit breaker | Very high | Stop cascading failure |
| Bulkhead | High | Resource isolation |
| Rate limiter | High | Protect services |
| Load shedding | High | Survive overload |
| Backpressure | High | Control producer speed |
| Fallback | High | Graceful degradation |
| Capacity planning | Very high | Scaling realism |
| SLO/SLI | High | Production ownership |
| Cascading failure | Very high | Senior incident topic |

---

# 2. Cascading Failure

Cascading failure happens when one failing dependency causes other services to fail.

Example:

```text
Payment service slows down.
Booking service threads wait.
API Gateway requests pile up.
Clients retry.
Traffic increases.
Booking service fails.
```

Controls:
- timeout
- circuit breaker
- bulkhead
- rate limiter
- load shedding
- fallback
- queue limits
- client retry limits

Strong answer:

```text
Cascading failure is prevented by failing fast, isolating resources, limiting retries, and
shedding load before the whole system is exhausted.
```

---

# 3. Timeout

Timeout bounds how long caller waits.

Types:
- connection timeout
- read/response timeout
- write timeout
- overall request deadline

Bad:

```text
No timeout on payment call.
```

Result:

```text
threads wait until exhausted
```

Strong answer:

```text
Every network call needs a timeout. Timeout should be based on the caller's latency budget,
not random defaults.
```

---

# 4. Deadline Budget

If user API SLO is:

```text
p95 < 500ms
```

Budget:

```text
Gateway: 30ms
Booking logic: 100ms
Inventory call: 120ms
Payment call: 200ms
Buffer: 50ms
```

Senior line:

```text
Timeouts should fit inside end-to-end request deadline. A downstream timeout longer than
the caller timeout is usually wasteful.
```

---

# 5. Retry

Retry tries again after a failure.

Retry only:
- transient network timeout
- 503
- 429 with retry-after/backoff
- deadlock loser
- temporary broker/DB issue

Do not retry:
- validation error
- 401/403
- duplicate business command without idempotency
- permanent 404
- payment charge without idempotency key

Strong answer:

```text
Retry is safe only for transient failures and idempotent operations. Otherwise it can
duplicate side effects or amplify outages.
```

---

# 6. Backoff And Jitter

Bad:

```text
all clients retry after exactly 1 second
```

Problem:

```text
retry wave hits dependency again together
```

Better:

```text
exponential backoff + jitter
```

Example:

```text
100ms + random
300ms + random
900ms + random
stop
```

Strong answer:

```text
Backoff and jitter turn synchronized retry spikes into smoother recovery traffic.
```

---

# 7. Retry Storm

Retry storm:

```text
normal traffic = 10k rps
each request retries 3 times
dependency now sees up to 40k attempts
```

Controls:
- retry budget
- max attempts
- exponential backoff
- circuit breaker
- rate limiter
- client-side timeout
- server load shedding

Senior answer:

```text
Retries consume capacity. I use retry budgets and circuit breakers so retries do not turn
a partial outage into a full outage.
```

---

# 8. Circuit Breaker

Circuit breaker fails fast when dependency is unhealthy.

States:
- closed
- open
- half-open

Flow:

```text
calls fail -> failure rate crosses threshold -> circuit opens
-> fail fast/fallback -> wait -> test limited calls -> close if healthy
```

Strong answer:

```text
Circuit breaker protects the caller and dependency by stopping calls during an outage and
allowing limited probes during recovery.
```

---

# 9. Bulkhead

Bulkhead isolates resources.

Example:

```text
Payment calls get max 50 threads.
Recommendation calls get max 20 threads.
```

If recommendations fail, payment path still has capacity.

Where:
- thread pools
- connection pools
- queues
- consumer groups
- database pools

Strong answer:

```text
Bulkheads prevent one slow dependency or feature from consuming all shared resources.
```

---

# 10. Rate Limiter

Rate limiter controls request rate.

Use for:
- public APIs
- per-user abuse control
- downstream quota protection
- expensive endpoints
- partner APIs

Algorithms:
- token bucket
- leaky bucket
- fixed window
- sliding window

Strong answer:

```text
Rate limiting protects systems before overload happens. The key can be user, API key,
tenant, route, or IP depending on the business need.
```

---

# 11. Load Shedding

Load shedding rejects low-priority work during overload.

Examples:
- return 429/503
- reject optional recommendation calls
- skip non-critical refresh
- stop accepting background jobs
- degrade to cached data

Strong answer:

```text
When a system is overloaded, it is better to reject some work quickly than accept everything
and fail slowly for everyone.
```

---

# 12. Backpressure

Backpressure makes producers slow down when consumers cannot keep up.

Examples:
- bounded queues
- HTTP 429
- Kafka lag-based scaling
- pause/resume consumers
- stream demand control

Strong answer:

```text
Backpressure prevents unbounded queues and memory growth by signaling that downstream cannot
accept more work at the current rate.
```

---

# 13. Fallback

Fallback returns alternate result.

Good fallback:
- cached hotel details
- stale recommendations
- hide optional section
- queue work for later

Bad fallback:
- pretend payment succeeded
- invent inventory availability
- bypass security decision

Strong answer:

```text
Fallback is only safe for optional or degradeable behavior. For correctness-critical flows,
fail clearly.
```

---

# 14. Graceful Degradation

Graceful degradation keeps core business working.

Example:

```text
Hotel booking still works.
Recommendations disabled.
Loyalty points shown later.
Email delayed.
```

Design:
- classify features critical vs optional
- define fallback per dependency
- communicate pending state
- monitor degraded mode

---

# 15. Capacity Planning Basics

Basic formulas:

```text
required instances = peak_rps / safe_rps_per_instance
```

```text
concurrency = rps * average_latency_seconds
```

Example:

```text
2,000 rps
average latency 200ms = 0.2s
concurrency around 400 in-flight requests
```

Add headroom:
- traffic spikes
- deployments
- AZ failure
- downstream slowness
- GC pauses

Strong answer:

```text
I capacity-plan from peak traffic, latency, resource usage, and failure headroom, not only
average load.
```

---

# 16. Scaling Bottlenecks

App scaling may not help if bottleneck is:
- database connections
- lock contention
- hot partition
- downstream quota
- Kafka partitions
- cache single key hotspot
- CPU-heavy serialization
- slow external API

Interview line:

```text
Before adding instances, I identify the bottleneck. Horizontal scaling helps only when the
bottleneck is horizontally scalable.
```

---

# 17. Hotspots

Hotspot examples:
- one celebrity hotel searched by everyone
- one Kafka key gets too much traffic
- one DB row locked frequently
- one tenant generates most traffic
- one cache key expires and stampedes

Controls:
- sharding
- cache
- key redesign
- batching
- per-tenant limits
- single-flight refresh
- split hot aggregate carefully

---

# 18. SLI And SLO

SLI:

```text
measured reliability indicator
```

SLO:

```text
target for that indicator
```

Examples:
- availability
- p95 latency
- error rate
- successful booking rate
- payment authorization success
- event processing delay

Strong answer:

```text
SLOs define what reliability means for users. Resilience patterns should support those
targets, not exist as decoration.
```

---

# 19. Resilience Pattern Combination

For outbound call:

```text
timeout -> retry with backoff if safe -> circuit breaker -> fallback if safe -> metrics
```

For resource isolation:

```text
bulkhead + bounded queue + rate limiter + load shedding
```

For async processing:

```text
consumer lag monitoring + retry topic + DLQ + idempotent consumer
```

---

# 20. Production Scenario: Payment Service Slow

Symptom:

```text
Booking API latency jumps from 300ms to 5s.
```

Debug:
1. Check p95/p99 latency.
2. Check payment client latency.
3. Check thread pool usage.
4. Check retry count.
5. Check circuit breaker state.
6. Check error rate and timeout rate.
7. Reduce retry, open circuit, or degrade if safe.
8. Communicate incident and protect core path.

Strong answer:

```text
I would first check whether Payment Service latency is consuming Booking Service threads.
Then I would verify timeouts, retries, circuit breaker, and bulkhead isolation. If payment
is required, I fail clearly; if optional, I degrade.
```

---

# 21. Common Mistakes

| Mistake | Why Wrong | Better |
|---|---|---|
| No timeout | stuck resources | strict deadlines |
| Retry every error | duplicates and load | retry only transient safe errors |
| No jitter | synchronized retry waves | backoff with jitter |
| No bulkhead | shared resource exhaustion | isolate pools |
| Infinite queue | memory blowup | bounded queue |
| Fallback fake success | corrupt business state | fallback only optional data |
| Scaling app blindly | bottleneck remains | find bottleneck first |
| Alert only on CPU | misses user impact | SLO-based alerts |
| No load shedding | everyone times out | reject low-priority work |

---

# 22. Hot Interview Questions

### Q1. Timeout vs retry?

```text
Timeout bounds waiting. Retry attempts again after failure. Retry must be bounded and fit
inside the caller deadline.
```

### Q2. Retry vs circuit breaker?

```text
Retry handles transient failures. Circuit breaker stops calls when dependency is broadly
unhealthy.
```

### Q3. What is bulkhead?

```text
Resource isolation so one dependency cannot consume all threads, connections, or queues.
```

### Q4. What is load shedding?

```text
Rejecting or dropping lower-priority work during overload to keep the system alive.
```

### Q5. How do you capacity plan?

```text
Use peak RPS, latency, resource usage, bottlenecks, and failure headroom.
```

---

# 23. Final Rapid Revision

| Problem | Pattern |
|---|---|
| Slow dependency | timeout |
| Temporary failure | retry with backoff |
| Dependency outage | circuit breaker |
| Shared resource exhaustion | bulkhead |
| Too much traffic | rate limiter |
| Overload survival | load shedding |
| Consumer cannot keep up | backpressure |
| Optional dependency down | fallback |
| Capacity estimate | RPS and latency math |
| User reliability target | SLO |

---

# 24. Strong Closing Answer

If interviewer asks:

```text
How do you make microservices resilient?
```

Say:

```text
I start with timeouts on every network call, then add bounded retries with backoff and jitter
only for transient idempotent failures. Circuit breakers fail fast during dependency outages,
bulkheads isolate resources, rate limiters protect capacity, and load shedding keeps the
system alive during overload. I monitor latency, errors, saturation, retry counts, circuit
state, and SLOs so we can detect and recover from incidents.
```

---

# 25. Official Source Notes

Useful references:

- Google SRE Book: https://sre.google/sre-book/table-of-contents/
- Google SRE Workbook: https://sre.google/workbook/table-of-contents/
- Resilience4j: https://resilience4j.readme.io/docs/getting-started
- Envoy Circuit Breaking: https://www.envoyproxy.io/docs/envoy/latest/intro/arch_overview/upstream/circuit_breaking
- Kubernetes Resource Management: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/

