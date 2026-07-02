# Microservices FinOps, Cost, Capacity, And Unit Economics Gold Sheet

> Track: Microservices Interview Track - Senior Path  
> Goal: make cost a first-class architecture dimension alongside latency, reliability, and correctness.

---

## 1. Intuition

Microservices can hide waste because cost spreads across many services:

```text
one request -> gateway -> 8 services -> database -> cache -> Kafka -> logs -> traces -> cross-region traffic
```

A service can meet its SLO and still be poorly designed if it wastes money.

---

## 2. Definition

- Definition: FinOps for microservices is the practice of measuring, attributing, optimizing,
  and governing cloud/runtime cost per service, team, tenant, product flow, and business unit.
- Category: production architecture, capacity planning, platform governance.
- Core idea: cost must be observable at the same level as reliability.

---

## 3. Cost Drivers

Common cost drivers:

- overprovisioned pods/containers
- idle replicas
- high-cardinality metrics
- verbose logs
- unsampled traces
- cross-region traffic
- retry storms
- chatty synchronous calls
- expensive fan-out
- Kafka retention
- DLQ/replay volume
- database read/write amplification
- cache overuse
- search index growth
- NAT gateway/egress
- cold-start mitigation capacity

Interview line:

```text
Microservice cost is often an interaction problem, not just a CPU problem.
```

---

## 4. Unit Economics

Track cost by business unit:

| Unit | Example Metric |
|---|---|
| request | cost per API request |
| booking | cost per completed booking |
| search | cost per search result page |
| tenant | cost per tenant |
| partner | cost per partner API consumer |
| event | cost per event processed |
| workflow | cost per completed workflow |

Hotel example:

```text
cost per confirmed booking = compute + DB + cache + events + observability + provider calls
```

Why it matters:

- search may be high volume but low revenue
- booking may be lower volume but business critical
- partner APIs may need plan-based quotas
- one tenant can drive disproportionate cost

---

## 5. Required Cost Tags

Every service should emit/own tags:

| Tag | Example |
|---|---|
| `service` | `booking-service` |
| `team` | `reservations` |
| `env` | `prod` |
| `region` | `us-east-1` |
| `tenant` | optional/high-cardinality caution |
| `product` | `hotel-booking` |
| `cost-center` | `travel-platform` |
| `version` | deployment version |

Warning:

```text
Do not blindly put high-cardinality values into metrics. Use logs/traces/sampled analytics
when metric cardinality would explode cost.
```

---

## 6. Capacity Planning

Basic capacity equation:

```text
required_instances = peak_qps * avg_service_time_seconds / target_utilization
```

Example:

```text
Peak booking QPS = 500
Average service time = 80 ms = 0.08 sec
Target utilization = 0.60
Required concurrency capacity = 500 * 0.08 / 0.60 = about 67 concurrent requests
```

Capacity checklist:

- QPS/RPS
- p95/p99 latency
- CPU and memory
- DB connection pool
- queue lag/age
- downstream quota
- retry amplification
- peak-to-average ratio
- regional traffic split
- failure-mode traffic

---

## 7. Right-Sizing

Right-size:

- CPU requests/limits
- memory requests/limits
- replica count
- autoscaling metric
- DB instance size
- cache size
- Kafka partitions/retention
- log retention
- trace sampling

Kubernetes warning:

```text
CPU limits can cause throttling and p99 latency spikes. Right-sizing is not only reducing
requests; it is protecting latency and cost together.
```

---

## 8. Retry Cost

Retries multiply cost and load.

Example:

```text
1 original request + 2 retries across 5 upstream callers = traffic amplification
```

Cost-safe retry policy:

- timeout budget
- max attempts
- exponential backoff
- jitter
- retry only retryable errors
- idempotency keys for writes
- retry budget
- circuit breaker
- load shedding under saturation

Interview line:

```text
Retries are not free reliability. They spend capacity and money.
```

---

## 9. Observability Cost

Observability can become a major cost center.

Controls:

- log sampling for noisy success paths
- structured logs with useful fields
- avoid PII in logs
- metric cardinality budgets
- trace sampling by route/tenant/status
- keep full traces for errors/high latency
- shorter retention for debug logs
- longer retention for audit logs where required

Do not cut:

- incident-critical logs
- SLO metrics
- audit records
- error traces
- deployment annotations

---

## 10. Cross-Region Cost

Cross-region designs add:

- data transfer
- replicated databases
- duplicated compute
- duplicated observability
- standby capacity
- failover testing cost
- consistency complexity

Decision line:

```text
Multi-region is justified by RTO/RPO, revenue risk, compliance, and user geography. It is
not automatically worth the cost for every service.
```

---

## 11. Cost Dashboard

A microservices cost dashboard should show:

- total cost by service
- cost per request
- cost per booking
- top services by growth
- idle capacity
- log volume by service
- trace volume by service
- cross-region data transfer
- retry volume
- queue backlog cost
- DB read/write cost
- cost by tenant/partner where safe

Add deploy annotations:

```text
cost changed after version v42 or config rollout
```

---

## 12. Cost Failure Scenarios

| Scenario | Cause | Fix |
|---|---|---|
| cost doubles overnight | retry storm | circuit breaker and retry budget |
| observability bill spikes | high-cardinality metric | cardinality budget and aggregation |
| cross-region bill grows | chatty service calls | regionalize workflow/read model |
| Kafka cost grows | infinite retention | retention policy and tiering decision |
| DB cost grows | N+1/fan-out reads | read model/cache/query optimization |
| idle services cost too much | fixed replicas | right-size or scale-to-zero if safe |

---

## 13. Interview Question

> Booking platform traffic grows 5x. Latency is acceptable, but cloud cost grows 12x. What do you check?

Strong answer:

```text
I would break cost down by service, request path, dependency, and recent changes. I would
check overprovisioned pods, DB/query cost, log and trace volume, retry amplification,
cross-region traffic, Kafka retention, cache hit rate, and fan-out count. Then I would define
unit economics such as cost per search and cost per confirmed booking. Optimization should
protect SLOs, not blindly reduce resources.
```

---

## 14. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| optimize cost without SLO | causes outages | optimize within reliability target |
| no service tags | cannot attribute cost | mandatory cost tags |
| trace everything forever | expensive/noisy | smart sampling |
| ignore retry cost | hidden amplification | retry budgets |
| scale app only | bottleneck stays DB/provider | scale dependency budget |
| no unit metric | cost has no business meaning | cost per booking/search |

---

## 15. Strong Closing Answer

```text
For microservices, cost is a production signal. I track cost by service, team, region, and
business unit such as cost per booking. I look for hidden multipliers: retries, fan-out,
cross-region traffic, logs, traces, overprovisioning, and idle replicas. The goal is not the
cheapest system; it is the least wasteful system that still meets SLOs and business risk.
```

