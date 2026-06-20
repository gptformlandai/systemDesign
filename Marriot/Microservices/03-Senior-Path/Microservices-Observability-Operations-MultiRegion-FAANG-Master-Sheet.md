# Microservices Observability Operations MultiRegion FAANG Master Sheet

Target: starter, intermediate, senior, and FAANG-level microservices interviews.

This sheet covers:
- logs, metrics, traces
- correlation IDs
- production debugging
- health checks
- runbooks
- deployment safety
- incident response
- multi-region architecture
- disaster recovery
- RTO/RPO
- failover and data replication

Goal:

```text
After reading this sheet, you should be able to operate microservices in production, debug
incidents across services, deploy safely, and explain multi-region trade-offs.
```

---

## 0. How To Use This Guide By Level

| Level | What To Focus On |
|---|---|
| Starter | logs, metrics, health checks |
| Intermediate | tracing, dashboards, alerts, deployment patterns |
| Senior | incident response, runbooks, SLOs, multi-region failover |
| FAANG-ready | DR trade-offs, RTO/RPO, regional consistency, operational excellence |

Must-say line:

```text
Microservices without observability become impossible to debug because one user request can
cross many services, queues, databases, and regions.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Centralized logging | Very high | Debugging |
| Metrics | Very high | Trends and alerts |
| Distributed tracing | Very high | Request path visibility |
| Correlation ID | Very high | Connect logs across services |
| Health checks | Very high | Routing and restarts |
| Dashboards | High | Operational visibility |
| Alerts | Very high | Incident detection |
| Runbooks | High | Incident response |
| Canary/blue-green | High | Safe deployments |
| Rollback | High | Recovery |
| Multi-region | High | Availability and latency |
| RTO/RPO | High | DR expectations |
| Failover | High | Continuity |

---

# 2. Three Pillars Of Observability

| Signal | Answers |
|---|---|
| Logs | What happened? |
| Metrics | How much/how often/how slow? |
| Traces | Where did the request spend time? |

Strong answer:

```text
Metrics tell me there is a problem, traces show where the request slowed or failed, and
logs explain what happened in that code path.
```

---

# 3. Correlation ID

Correlation ID links all work for one request.

Flow:

```text
Gateway -> Booking -> Payment -> Inventory -> Kafka event -> Notification
```

Each service logs:

```text
correlationId=c-123
```

Strong answer:

```text
Correlation IDs are mandatory in microservices because request context crosses process and
network boundaries.
```

---

# 4. Distributed Tracing

Trace:

```text
end-to-end request
```

Span:

```text
one operation inside the trace
```

Example:

```text
Trace: create booking
  gateway span 20ms
  booking service span 80ms
  inventory call span 120ms
  payment call span 300ms
  outbox write span 10ms
```

Strong answer:

```text
Tracing is how I find the slow or failing hop in a distributed request.
```

---

# 5. Metrics

Core metrics:
- request rate
- error rate
- latency percentiles
- saturation
- CPU/memory
- DB connection pool
- queue depth
- Kafka consumer lag
- retry count
- circuit breaker state
- DLQ count

RED:
- rate
- errors
- duration

USE:
- utilization
- saturation
- errors

Senior line:

```text
Percentiles matter because averages hide tail latency.
```

---

# 6. Logs

Good logs include:
- timestamp
- level
- service name
- trace/correlation ID
- user/customer/tenant ID when safe
- resource ID
- event name
- status
- error code

Avoid:
- passwords
- tokens
- full credit card data
- excessive payloads
- unbounded debug logs

Strong answer:

```text
I prefer structured logs with consistent fields so they can be searched and correlated
across services.
```

---

# 7. Health Checks

Types:

| Check | Meaning |
|---|---|
| Liveness | process should be restarted if failing |
| Readiness | service should receive traffic if passing |
| Startup | app has finished initialization |

Trap:

```text
Liveness check depends on database.
```

Why bad:

```text
temporary DB outage restarts every pod and makes incident worse
```

Strong answer:

```text
Readiness should reflect ability to serve traffic. Liveness should usually reflect whether
the process is stuck, not whether every dependency is healthy.
```

---

# 8. Alerting

Good alerts are:
- user-impacting
- actionable
- owned
- not too noisy
- tied to runbooks

Alert examples:
- booking 5xx rate > 2 percent for 10 minutes
- p95 booking latency > 1s
- payment timeout rate > threshold
- Kafka consumer lag age > 10 minutes
- DLQ count increasing
- canary error rate higher than baseline

Strong answer:

```text
I alert on symptoms users feel, then use dashboards and traces to find causes.
```

---

# 9. Runbooks

Runbook contains:
- symptom
- dashboard links
- commands
- owner
- rollback steps
- escalation path
- known mitigations

Example:

```text
Symptom: Booking API 5xx high
Check: gateway 5xx, booking logs, DB pool, payment latency
Mitigate: disable optional features, rollback latest deploy, open circuit for optional dependency
Escalate: booking on-call, payment on-call
```

---

# 10. Production Debugging Playbook

When user says booking failed:

1. Get correlation ID or booking ID.
2. Check gateway logs.
3. Check Booking Service trace.
4. Check downstream spans.
5. Check error logs.
6. Check DB transaction result.
7. Check outbox/event publication.
8. Check consumer processing and DLQ.
9. Check deployment timeline.
10. Check regional/platform incidents.

Strong answer:

```text
I debug from the user symptom backward through traces, logs, metrics, events, and recent
deployments.
```

---

# 11. Deployment Patterns

| Pattern | Meaning | Use |
|---|---|---|
| Rolling | replace instances gradually | normal deployments |
| Blue-green | switch from old environment to new | fast rollback |
| Canary | small traffic to new version | risk control |
| Shadow | duplicate traffic without affecting response | test behavior |
| Feature flag | enable behavior dynamically | safe rollout |

Strong answer:

```text
Canary plus metrics gives early signal before exposing all users to a bad version.
```

---

# 12. Safe Rollback

Rollback requires:
- backward-compatible APIs
- backward-compatible DB migrations
- old and new event schemas compatible
- feature flags
- image/version pinning
- data migration awareness

Trap:

```text
Code rollback after irreversible database migration.
```

Better:

```text
Use expand-contract migrations and deploy code in phases.
```

---

# 13. Incident Response

Incident stages:
1. Detect
2. Triage
3. Mitigate
4. Communicate
5. Resolve
6. Postmortem
7. Prevent recurrence

Good postmortem:
- blameless
- timeline
- impact
- root causes
- contributing factors
- action items
- owners and dates

Senior line:

```text
The first goal is mitigation, not perfect root cause.
```

---

# 14. Multi-Region Why

Reasons:
- lower latency
- disaster recovery
- data residency
- high availability
- traffic isolation

Costs:
- data consistency complexity
- failover complexity
- higher infrastructure cost
- operational complexity
- harder debugging

Strong answer:

```text
Multi-region is not free availability. It trades simpler single-region operations for
latency, resilience, and DR capabilities with much more data and failover complexity.
```

---

# 15. RTO And RPO

RTO:

```text
How quickly must service recover?
```

RPO:

```text
How much data loss is acceptable?
```

Example:

```text
RTO = 15 minutes
RPO = 1 minute
```

Design impact:
- backup frequency
- replication mode
- failover automation
- cost
- testing frequency

---

# 16. Active-Passive vs Active-Active

| Model | Meaning | Trade-off |
|---|---|---|
| Active-passive | one region serves, another standby | simpler, slower failover |
| Active-active | multiple regions serve traffic | lower latency, harder consistency |

Active-passive:
- easier data ownership
- simpler conflict handling
- failover needs practice

Active-active:
- lower latency
- regional resilience
- conflict resolution needed
- harder writes

---

# 17. Data Replication Trade-Offs

Synchronous replication:
- lower data loss
- higher latency
- regional dependency

Asynchronous replication:
- lower latency
- possible data loss
- lag during failover

Strong answer:

```text
Replication choice depends on RPO, latency budget, and consistency needs. Stronger
cross-region consistency usually costs latency and availability.
```

---

# 18. Regional Routing

Routing strategies:
- geo DNS
- global load balancer
- active-passive failover
- latency-based routing
- user home region
- tenant-based routing

Need:
- health checks
- failover criteria
- data locality
- session/token behavior
- cache invalidation

---

# 19. Multi-Region Booking Challenge

Hard problem:

```text
Two users in different regions try to book same room.
```

Options:
- single writer region for inventory
- partition inventory by hotel region
- strongly consistent global database, if latency acceptable
- reservation token from home inventory region
- avoid active-active writes for same inventory shard

Strong answer:

```text
For strict inventory correctness, I prefer a single writer per inventory shard or hotel
region. Active-active writes to the same room inventory require conflict resolution that
may be unacceptable for booking.
```

---

# 20. Disaster Recovery Testing

DR must be tested.

Test:
- restore from backup
- region failover
- DNS/global LB switch
- database replica promotion
- event replay
- cache warmup
- application startup in DR region
- runbook accuracy

Interview line:

```text
Untested DR is hope, not a plan.
```

---

# 21. Production Scenario: Region Outage

Scenario:

```text
Primary region for booking platform is unavailable.
```

Response:
1. Declare incident.
2. Check global traffic routing.
3. Promote standby DB or route to active region.
4. Confirm RPO data lag.
5. Disable risky writes if needed.
6. Warm caches/read models.
7. Monitor error rate and latency.
8. Communicate user impact.
9. Reconcile data after recovery.

Strong answer:

```text
Failover is not just routing traffic. I must understand data replication lag, write ownership,
cache/read model freshness, and reconciliation after the primary recovers.
```

---

# 22. Common Mistakes

| Mistake | Why Wrong | Better |
|---|---|---|
| No correlation ID | cannot trace request | propagate ID |
| Only logs, no metrics | no trend/alert | dashboards |
| Only metrics, no traces | hard root cause | distributed tracing |
| Alert on everything | alert fatigue | actionable SLO alerts |
| Liveness checks DB | restart storm | separate liveness/readiness |
| Rollback ignores DB | broken old code | expand-contract |
| Multi-region without RTO/RPO | unclear design | define targets first |
| Active-active all writes | conflicts | shard/single-writer |
| Untested DR | false confidence | regular game days |

---

# 23. Hot Interview Questions

### Q1. Logs vs metrics vs traces?

```text
Metrics show trends, logs show details, traces show request flow across services.
```

### Q2. Liveness vs readiness?

```text
Liveness restarts broken process. Readiness removes service from traffic.
```

### Q3. Canary vs blue-green?

```text
Canary gradually shifts small traffic to new version. Blue-green switches between two full
environments.
```

### Q4. RTO vs RPO?

```text
RTO is recovery time target. RPO is acceptable data loss window.
```

### Q5. Active-active challenge?

```text
Active-active improves latency and availability, but writes to same data need conflict
resolution or partitioning.
```

---

# 24. Final Rapid Revision

| Need | Concept |
|---|---|
| Request path | trace |
| Connect logs | correlation ID |
| Trend/alert | metrics |
| Detailed event | logs |
| Receive traffic | readiness |
| Restart process | liveness |
| Safe small rollout | canary |
| Fast environment switch | blue-green |
| Recovery time | RTO |
| Data loss window | RPO |
| Region failover | DR plan |
| Cross-region writes | consistency trade-off |

---

# 25. Strong Closing Answer

If interviewer asks:

```text
How do you operate microservices in production?
```

Say:

```text
I make every service observable with structured logs, metrics, traces, health checks, and
correlation IDs. Alerts are tied to user-impacting SLOs, and incidents have runbooks and
rollback plans. Deployments use canary, blue-green, or rolling strategies with backward
compatible APIs and migrations. For multi-region, I define RTO/RPO first, then choose
active-passive or active-active based on latency, consistency, cost, and failover complexity.
```

---

# 26. Official Source Notes

Useful references:

- OpenTelemetry Observability Primer: https://opentelemetry.io/docs/concepts/observability-primer/
- Kubernetes Probes: https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/
- Kubernetes Deployments: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
- Google SRE Book: https://sre.google/sre-book/table-of-contents/
- AWS Disaster Recovery Whitepaper: https://docs.aws.amazon.com/whitepapers/latest/disaster-recovery-workloads-on-aws/disaster-recovery-options-in-the-cloud.html

