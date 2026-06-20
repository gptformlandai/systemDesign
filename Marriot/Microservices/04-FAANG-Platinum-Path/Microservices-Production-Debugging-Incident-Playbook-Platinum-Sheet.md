# Microservices Production Debugging Incident Playbook Platinum Sheet

Target: senior and MAANG-level backend, platform, and system design interviews.

This sheet teaches how to debug a live microservices incident without guessing. It is built
around symptoms, signals, likely causes, mitigation, root cause analysis, and prevention.

---

## 0. The Production Mindset

During an incident, your first job is not to find the perfect root cause. Your first job is
to reduce user impact safely.

Order:

```text
1. Detect user impact.
2. Stabilize the system.
3. Stop the bleeding.
4. Preserve evidence.
5. Find root cause.
6. Prevent repeat.
```

Strong interview line:

```text
I separate mitigation from root cause. During the incident I protect users; after the
incident I make the system easier to detect, debug, and recover.
```

---

# 1. Universal Debugging Loop

Use this loop for every production issue.

```text
Symptom -> blast radius -> recent change -> golden signals -> dependency map -> mitigation
-> root cause -> prevention
```

Questions:

- Which user journey is failing?
- Is it one region, one service, one tenant, or global?
- Did a deploy/config/schema/traffic change happen?
- Is latency, traffic, error rate, or saturation abnormal?
- Which dependency changed first?
- Can we rollback, disable a flag, shed load, or degrade gracefully?

---

# 2. Golden Signals

| Signal | Ask |
|---|---|
| Latency | Are p50, p95, p99 increasing? |
| Traffic | Did request/event volume change suddenly? |
| Errors | Are 4xx/5xx/timeouts/retries increasing? |
| Saturation | Are CPU, memory, thread pools, connection pools, queues full? |

For event systems, add:

- consumer lag
- DLQ rate
- rebalance frequency
- duplicate processing rate
- publish failure rate

---

# 3. Incident Command Roles

| Role | Responsibility |
|---|---|
| Incident commander | coordinates response and timeline |
| Tech lead | drives diagnosis and mitigation |
| Comms owner | updates stakeholders |
| Scribe | records events, commands, decisions |
| Service owner | owns fix and follow-up |

In interviews:

```text
For a serious incident, I define roles so debugging does not become a noisy group chat.
```

---

# 4. Case Study: API Latency Spike

## Symptom

```text
Booking API p99 increased from 300 ms to 6 seconds.
Error rate is low, but users report slow checkout.
```

## Debug Path

1. Check if latency is global or one region.
2. Compare p50 vs p99.
3. Inspect traces for slow spans.
4. Check downstream services and database calls.
5. Check connection pools, thread pools, and queue wait time.
6. Check recent deployments/config changes.

## Common Causes

| Cause | Signal |
|---|---|
| downstream slow | trace shows Payment span slow |
| DB missing index | SQL span slow, high rows scanned |
| connection pool exhausted | wait time before query |
| retry amplification | more outbound calls than inbound |
| lock contention | DB wait events / long transactions |

## Mitigation

- rollback recent change
- disable expensive feature flag
- increase timeout only if it prevents false failure, not as a blind fix
- shed non-critical traffic
- use cached/degraded response for read path
- pause batch jobs competing for DB

## Prevention

- span-level dashboards
- connection pool metrics
- query performance tests
- load test with p95/p99 tracking
- retry budgets

---

# 5. Case Study: Error Rate Spike After Deploy

## Symptom

```text
Payment Service 5xx increased immediately after deployment.
```

## Debug Path

1. Confirm deploy timestamp.
2. Compare old vs new version error rate.
3. Check logs by version.
4. Inspect config and secret changes.
5. Check dependency compatibility.
6. Rollback if customer impact is active.

## Likely Causes

- missing environment variable
- wrong secret path
- incompatible API contract
- database migration not applied
- feature flag enabled globally
- bad retry timeout defaults

## Strong Answer

```text
If errors start right after deploy, I assume change-related until proven otherwise. I compare
old and new version metrics, rollback or shift traffic away, then inspect logs, config,
schema changes, and dependency contracts.
```

---

# 6. Case Study: Consumer Lag Rising

## Symptom

```text
Kafka consumer lag for Notification Service keeps growing.
```

## Debug Path

1. Check producer rate vs consumer rate.
2. Check consumer error rate.
3. Check rebalance frequency.
4. Check partition count and consumer count.
5. Check slow downstream dependency.
6. Check DLQ/retry topic volume.

## Common Causes

| Cause | Fix |
|---|---|
| downstream email provider slow | circuit breaker, rate limit, queue retries |
| poison message blocks partition | DLQ after bounded retries |
| one hot partition | better key, split topic, rebalance load |
| consumer too slow | batch, parallelize safely, optimize handler |
| frequent rebalances | tune session settings, reduce long processing |

## Prevention

- lag alert with burn rate
- idempotent consumer
- DLQ dashboard
- event processing SLO
- separate retry topics by delay

---

# 7. Case Study: Retry Storm

## Symptom

```text
One downstream service is slow. Upstream services retry aggressively. Traffic multiplies.
```

## What Users See

- slow requests
- intermittent failures
- system-wide degradation

## Root Cause Pattern

```text
timeout too high + retry count too high + no jitter + no circuit breaker + no retry budget
```

## Mitigation

- reduce retry count
- add exponential backoff and jitter
- open circuit for failing dependency
- shed low-priority traffic
- return graceful fallback

## Prevention

```text
Every retry must have timeout, backoff, jitter, idempotency, and a budget.
```

---

# 8. Case Study: Database Saturation

## Symptom

```text
Many services slow at once. Database CPU and connections are high.
```

## Debug Path

1. Identify top queries by time and frequency.
2. Check connection pool wait.
3. Check recent query/schema/index changes.
4. Check batch jobs and reporting queries.
5. Check lock waits and long transactions.
6. Check read replica lag if replicas are used.

## Immediate Actions

- pause non-critical batch/reporting jobs
- rollback query-heavy deploy
- add emergency index only after verifying lock/build impact
- reduce traffic for expensive endpoints
- enable cached read path when safe

## Prevention

- query review for hot APIs
- indexes based on real query patterns
- separate OLTP and analytics
- pool limits per service
- slow query alerts

---

# 9. Case Study: Partial Regional Outage

## Symptom

```text
Only one region has high error rate. Other regions are healthy.
```

## Debug Path

1. Confirm region-specific dashboards.
2. Check cloud/network/load balancer status.
3. Check regional dependency health.
4. Check deployment/config drift.
5. Check DNS/routing changes.
6. Check data replication lag.

## Mitigation

- shift traffic to healthy region if data model allows
- disable writes in bad region if consistency is at risk
- fail over read-only traffic first
- communicate degraded mode

## Key Trade-Off

Active-active gives availability but makes data conflict handling harder. Active-passive is
simpler but has failover delay and lower resource utilization.

---

# 10. Case Study: Duplicate Booking Or Payment

## Symptom

```text
Users are charged twice or booking confirmation happens twice.
```

## Debug Path

1. Check retry logs and idempotency key.
2. Check whether unique constraints exist.
3. Check event consumer idempotency.
4. Check outbox/inbox records.
5. Check external provider webhook retries.

## Correctness Controls

- idempotency key on command
- unique constraint on business key
- outbox for reliable publish
- inbox/processed-message table for consumers
- state machine transition guard
- provider reference uniqueness

Strong answer:

```text
I do not rely on "the client will not retry". I make duplicate commands and duplicate events
safe through idempotency keys, unique constraints, and idempotent consumers.
```

---

# 11. Case Study: Cascading Failure

## Symptom

```text
One slow dependency causes many services to fail.
```

## Why It Happens

- no timeout
- thread pools blocked
- queues grow without limit
- retries amplify load
- every service depends synchronously on the failing service

## Mitigation

- timeout failing calls
- circuit-break failing dependency
- isolate thread pools by dependency
- shed non-critical load
- return fallback or cached data

## Prevention

- dependency bulkheads
- graceful degradation
- chaos testing for critical paths
- dependency SLOs
- call graph review

---

# 12. Debugging Tools By Layer

| Layer | What To Check |
|---|---|
| Gateway | route errors, auth failures, rate limits, upstream latency |
| Service | logs, metrics, traces, thread pools, heap, config |
| Database | top queries, locks, connections, replica lag |
| Kafka | lag, rebalances, DLQ, producer errors |
| Kubernetes | pod restarts, readiness, CPU/memory throttling, events |
| Network | DNS, TLS, load balancer, regional routing |
| Release | deploy, config, feature flags, schema migration |

---

# 13. Metrics Every Microservice Should Expose

Application:

- request rate
- error rate
- latency percentiles
- dependency latency
- thread pool utilization
- connection pool active/idle/wait
- cache hit ratio

Messaging:

- events produced
- events consumed
- consumer lag
- retries
- DLQ count
- processing latency

Business:

- booking created
- booking confirmed
- payment authorized
- payment failed
- inventory reservation failed

---

# 14. Incident Timeline Template

```text
Incident:
Start time:
Detected by:
Customer impact:
Affected services:
Affected regions:
Recent changes:
Mitigation:
Rollback/flag action:
Root cause:
Prevention items:
Owner:
Due date:
```

---

# 15. Postmortem Quality Bar

A strong postmortem includes:

- impact
- timeline
- root cause
- contributing factors
- why detection was late, if applicable
- what worked
- what failed
- action items with owners
- no blame language

Weak postmortem:

```text
Developer made a mistake.
```

Strong postmortem:

```text
The migration allowed incompatible schema usage because CI did not run backward
compatibility checks and canary alarms were based on CPU instead of checkout error rate.
```

---

# 16. Interview Question

> Booking checkout latency suddenly increases and payment errors rise. How do you debug?

Strong answer:

```text
I first determine blast radius: region, version, tenant, or global. Then I check golden
signals and traces for the checkout path. Since payment errors are rising, I inspect Payment
Service latency, provider calls, retries, circuit breaker state, and connection/thread pools.
If this started after deploy or config change, I rollback or disable the feature flag. I
protect users by opening the circuit, reducing retries, and marking bookings pending rather
than failing permanently. After stabilizing, I perform RCA and add alerts for payment error
rate, retry volume, and checkout p99.
```

---

# 17. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Debugging without checking recent changes | check deploy/config/schema/traffic first |
| Increasing timeout blindly | find queueing/downstream cause |
| Unlimited retries | retry budget with backoff and jitter |
| Alerting on CPU only | alert on user-facing SLOs |
| No correlation ID | propagate trace/request ID |
| No rollback plan | canary, blue-green, feature flags |
| No business metrics | track user journey outcomes |

---

# 18. Final Rapid Revision

```text
Latency spike -> traces, dependency spans, pools, DB queries.
Error spike after deploy -> rollback, compare versions, config/schema/contracts.
Consumer lag -> producer rate, consumer errors, poison messages, partitions.
Retry storm -> timeout, backoff, jitter, circuit breaker, retry budget.
DB saturation -> top queries, locks, connections, batch/reporting jobs.
Duplicate side effect -> idempotency, unique constraint, inbox/outbox.
Cascading failure -> bulkhead, load shedding, graceful degradation.
```

---

# 19. Official Source Notes

- OpenTelemetry traces: https://opentelemetry.io/docs/concepts/signals/traces/
- Kubernetes probes and pod lifecycle: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/
- PostgreSQL EXPLAIN: https://www.postgresql.org/docs/current/using-explain.html
