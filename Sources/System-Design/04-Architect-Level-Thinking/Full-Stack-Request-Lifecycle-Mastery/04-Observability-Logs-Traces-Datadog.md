# 04 - Observability Logs Traces Datadog

> Goal: know exactly what should be visible in logs, metrics, traces, dashboards, alerts, and Datadog during a request lifecycle.

---

## 1. Intuition

Observability is the system's memory and nervous system.

When a request fails, you need to answer:

- who was affected?
- where did time go?
- which dependency failed?
- did we retry?
- did we duplicate a write?
- did the DB lock?
- did a cache miss cause it?
- did a queue backlog grow?
- did users feel it?

Beginner line:

```text
Logs explain events, metrics show trends, traces connect service hops, and dashboards/alerts turn
signals into operations decisions.
```

---

## 2. The Three Pillars Plus

| Signal | What It Answers | Example |
|---|---|---|
| logs | what happened? | checkout failed because payment timeout |
| metrics | how often/how much? | p95 latency, error rate, queue lag |
| traces | where did time go? | gateway -> checkout -> payment -> DB |
| profiles | what code consumed CPU/memory? | JSON serialization hot path |
| RUM | what did real users experience? | frontend LCP/INP/error |
| synthetic tests | is critical flow working? | login/checkout probe |
| audit logs | who changed sensitive state? | admin refund approved |

Wrong option:

```text
Only add logs after an incident.
```

What fails:

```text
The failure already happened and you cannot reconstruct the timeline or prove impact.
```

Better:

```text
Design observability fields and dashboards with the request lifecycle from day one.
```

---

## 3. Trace Context Propagation

Trace context headers:

| Header | Purpose |
|---|---|
| `traceparent` | W3C trace ID, span ID, sampling flags |
| `tracestate` | vendor-specific trace state |
| `baggage` | extra contextual key-value data, use carefully |
| `X-Request-ID` | request correlation, often generated at edge/gateway |

Trace flow:

```text
Browser/mobile
  -> CDN/gateway
  -> API service
  -> backend service
  -> cache
  -> database
  -> queue publish
  -> async worker consume
```

Each span should include:

- service name
- operation name
- route/resource
- status/error
- duration
- dependency tags
- region/zone
- deployment version

Wrong option:

```text
Generate a brand new trace ID in every service.
```

What fails:

```text
The distributed trace breaks into unrelated fragments and you cannot see the full request path.
```

Better:

```text
Propagate trace context across HTTP/gRPC/messages and create child spans.
```

---

## 4. Structured Logs

Good log fields:

```json
{
  "timestamp": "2026-07-02T10:15:30.123Z",
  "level": "ERROR",
  "service": "checkout-service",
  "env": "prod",
  "region": "us-east-1",
  "version": "2026.07.02.4",
  "trace_id": "4f8c...",
  "span_id": "9a2b...",
  "request_id": "req_123",
  "user_id_hash": "u_789",
  "order_id": "ord_456",
  "idempotency_key": "idem_abc",
  "route": "POST /checkout",
  "error_code": "PAYMENT_TIMEOUT",
  "dependency": "payment-provider-a",
  "latency_ms": 1840,
  "message": "Payment authorization timed out"
}
```

Do log:

- stable IDs
- status transitions
- dependency failures
- retry attempts
- idempotency hits/conflicts
- queue publish/consume state
- DB timeout/deadlock/slow query metadata

Do not log:

- raw card numbers
- passwords
- full tokens
- sensitive PII
- unbounded request bodies

Wrong option:

```text
Log full request and response bodies for debugging payments.
```

What fails:

```text
Secrets/PII/card data may leak into logs, causing compliance and security incidents.
```

Better:

```text
Log safe identifiers, redacted fields, error codes, and provider references.
```

---

## 5. Metrics

Golden signals:

| Signal | Meaning |
|---|---|
| latency | how long requests take |
| traffic | how many requests |
| errors | failure rate |
| saturation | how full resources are |

Important backend metrics:

| Metric | Why It Matters |
|---|---|
| request count by route/status | traffic and error split |
| p50/p95/p99 latency | user and tail experience |
| dependency latency | external bottlenecks |
| DB connection pool usage | saturation |
| slow query count | database health |
| cache hit ratio | cache effectiveness |
| queue depth/lag | async backlog |
| retry count | hidden failure pressure |
| rate limit count | abuse or capacity signal |
| circuit breaker open count | dependency failure |
| idempotency conflict count | duplicate/client bugs |

Finance metrics:

- ledger transaction commit rate
- failed transfer rate
- reconciliation mismatch count
- pending transaction age
- duplicate idempotency hit rate
- balance projection lag

E-commerce metrics:

- product page latency
- search p95/p99
- add-to-cart rate
- checkout success rate
- payment authorization latency
- inventory reservation failure rate
- oversell count
- order stuck count

Wrong option:

```text
Monitor only average latency.
```

What fails:

```text
Tail users can suffer badly while average looks fine.
```

Better:

```text
Monitor percentiles, especially p95/p99, by route, region, dependency, and client type.
```

---

## 6. Datadog APM View

In Datadog APM, you should expect to see:

Service map:

```text
web-frontend
  -> api-gateway
  -> checkout-service
  -> inventory-service
  -> payment-service
  -> order-service
  -> postgres/orders
  -> redis/cart
  -> kafka/order-events
```

Trace waterfall:

```text
POST /checkout                         2.4s
  api-gateway auth/rate-limit           25ms
  checkout-service validate cart        40ms
  redis get cart                        8ms
  pricing-service quote                 70ms
  inventory-service reserve             160ms
  postgres update inventory             120ms
  payment-service authorize             1.7s
  order-service create                  180ms
  kafka publish outbox relay            20ms
```

Good tags:

- `env:prod`
- `service:checkout-service`
- `version:...`
- `region:...`
- `route:POST /checkout`
- `tenant:...` where safe
- `payment_provider:...`
- `db_shard:...`
- `cache_result:hit|miss`

Wrong option:

```text
Use high-cardinality raw values like full user email, full URL with tokens, or raw request body as metric tags.
```

What fails:

```text
Cost explodes, dashboards slow down, and sensitive data may leak.
```

Better:

```text
Use bounded-cardinality tags and keep raw identifiers in logs/traces only when safe and controlled.
```

---

## 7. Datadog Dashboard For E-Commerce Availability

Dashboard sections:

| Section | Widgets |
|---|---|
| global health | availability, RPS, error rate, p95/p99 |
| edge | CDN hit rate, WAF blocks, 4xx/5xx, region split |
| search/catalog | search latency, index lag, cache hit ratio |
| cart | read/write latency, conflict merges, Redis health |
| checkout | success funnel, reservation failures, payment pending |
| inventory | hot SKUs, lock wait, conditional write failures |
| payment | provider latency/error, unknown outcomes |
| async | Kafka lag, DLQ count, outbox backlog |
| infrastructure | CPU, memory, pods, DB connections |

Key SLOs:

| SLO | Example |
|---|---|
| browse availability | 99.99% |
| search p95 | < 200 ms internal, product dependent |
| checkout success | business-defined |
| payment unknown age | bounded |
| order stuck count | near zero |

What you see during flash-sale overload:

```text
RPS spikes
CDN hit ratio may stay high
WAF/bot blocks increase
search/cache latency okay
inventory hot SKU conditional failures increase
checkout p99 rises
payment provider timeout rises
rate-limit/admission-control count increases
queue lag increases for notifications/analytics
```

Correct response:

- protect checkout DB/inventory
- throttle bot/hot SKU
- keep browse available
- degrade recommendations
- pause non-critical consumers if needed
- monitor stuck orders/payment unknowns

---

## 8. Datadog Dashboard For Finance Strict Consistency

Dashboard sections:

| Section | Widgets |
|---|---|
| transfer API | RPS, 4xx/5xx, p95/p99, idempotency hits |
| auth/risk | auth failures, device risk blocks |
| ledger DB | commit latency, deadlocks, lock waits, replication lag |
| ledger invariants | unbalanced transaction count, should be zero |
| reconciliation | mismatch count, unresolved age |
| queue/outbox | pending notifications/events, DLQ |
| external rails | bank/provider latency, timeout, reject codes |
| audit | admin actions, privilege changes |
| saturation | DB connections, CPU, storage, WAL/log growth |

Key SLOs:

| SLO | Example |
|---|---|
| no unbalanced ledger commits | 100% invariant |
| transfer API availability | high but below correctness priority |
| reconciliation mismatch resolution | within business SLA |
| pending transfer age | bounded |
| audit log completeness | mandatory |

What you see during DB lock contention:

```text
transfer p99 rises
ledger DB lock wait rises
deadlock/retry count rises
connection pool saturation rises
some transfers return pending/retryable failure
no unbalanced ledger entries should appear
```

Correct response:

- reduce write concurrency per hot account
- inspect lock queries
- ensure retries are idempotent
- consider account-level serialization
- never bypass ledger transaction for availability

---

## 9. Alerts And Monitors

Good alerts:

- SLO burn rate
- p99 latency by critical route
- error-rate spike by route/status
- payment provider timeout spike
- DB connection saturation
- queue lag age, not just depth
- DLQ nonzero for critical topics
- reconciliation mismatch count
- cache hit ratio collapse
- WAF/bot anomaly

Bad alerts:

- CPU > 60% for 5 minutes without user impact
- any single 500
- queue depth high without age or consumer rate
- average latency only
- alerts with no owner/runbook

Wrong option:

```text
Page humans for every warning-level metric change.
```

What fails:

```text
Alert fatigue causes real incidents to be missed.
```

Better:

```text
Page on user-impacting symptoms and fast SLO burn. Route lower signals to tickets/dashboards.
```

---

## 10. What Logs Look Like In Failure Scenarios

Payment timeout:

```json
{
  "level": "WARN",
  "service": "payment-service",
  "trace_id": "trace_1",
  "order_id": "ord_101",
  "payment_intent_id": "pi_555",
  "idempotency_key": "idem_777",
  "provider": "provider_a",
  "attempt": 1,
  "error_code": "PROVIDER_TIMEOUT",
  "next_state": "AUTH_UNKNOWN",
  "message": "Payment provider timed out; status will be reconciled"
}
```

Inventory reservation conflict:

```json
{
  "level": "INFO",
  "service": "inventory-service",
  "trace_id": "trace_2",
  "sku_id": "sku_hot_1",
  "requested_qty": 1,
  "available_before": 0,
  "result": "OUT_OF_STOCK",
  "message": "Conditional reservation failed"
}
```

Finance idempotency replay:

```json
{
  "level": "INFO",
  "service": "transfer-service",
  "trace_id": "trace_3",
  "transfer_id": "tr_123",
  "idempotency_key": "idem_abc",
  "result": "REPLAYED_RESPONSE",
  "message": "Duplicate transfer request returned original response"
}
```

Ledger invariant violation should be critical:

```json
{
  "level": "CRITICAL",
  "service": "ledger-service",
  "trace_id": "trace_4",
  "ledger_transaction_id": "ltx_999",
  "error_code": "UNBALANCED_LEDGER_ATTEMPT",
  "blocked": true,
  "message": "Rejected ledger transaction because debit and credit totals differ"
}
```

---

## 11. Incident Walkthrough - Checkout Is Slow

Question:

```text
Checkout p99 jumped from 800 ms to 6 seconds. What do you inspect?
```

Investigation:

1. Check dashboard: route p99, error rate, RPS.
2. Open APM trace for slow checkout.
3. Identify slow span: payment provider, DB, inventory, Redis, gateway?
4. Compare by region, version, provider, SKU, tenant.
5. Check deploy timeline.
6. Check DB pool and slow queries.
7. Check queue/outbox lag.
8. Check rate-limit and retry counts.
9. Check logs by trace ID and order ID.
10. Mitigate: failover provider, reduce retries, enable circuit breaker, throttle hot SKU, rollback.

Strong answer:

```text
I would avoid guessing. I would use APM to find the slow span, metrics to see blast radius, logs to
understand business state, and dashboards to decide mitigation. I would protect order/payment
correctness before optimizing latency.
```

---

## 12. Incident Walkthrough - Finance Transfer Mismatch

Question:

```text
A reconciliation job found mismatch between internal ledger and external settlement report.
```

Investigation:

1. Identify settlement reference and transfer ID.
2. Pull trace/logs for original transfer.
3. Check idempotency key and duplicate attempts.
4. Inspect ledger transaction and entries.
5. Inspect provider webhook order and payload.
6. Check outbox/inbox processed event IDs.
7. Check manual/admin actions.
8. Classify mismatch: provider-only, internal-only, amount mismatch, currency, timing.
9. Create correction entry if needed, never mutate old ledger record destructively.
10. Add monitor/test if failure class was missing.

Strong answer:

```text
For finance, reconciliation is not optional. The logs and ledger should let me reconstruct the exact
state transitions without relying on memory or mutable balance fields.
```

---

## 13. Observability Anti-Patterns

| Anti-Pattern | What Fails | Better |
|---|---|---|
| no trace propagation | cannot connect services | W3C trace context/OpenTelemetry |
| string-only logs | cannot query reliably | structured logs |
| high-cardinality metric tags | cost/cardinality explosion | bounded tags |
| no business IDs | cannot debug orders/payments | safe business correlation IDs |
| logs contain secrets | security/compliance breach | redaction/tokenization |
| dashboards only infra | user impact hidden | route/SLO/business dashboards |
| no async trace link | queue processing invisible | propagate trace/event IDs |
| no audit logs | cannot prove sensitive actions | immutable audit records |

---

## 14. Interview Answer Template

```text
I instrument the request at every boundary. The gateway creates or propagates request and trace IDs.
Each service emits spans for validation, cache, DB, queue, and downstream calls. Logs are structured
with safe business identifiers like orderId or transferId, never secrets. Metrics track latency,
traffic, errors, saturation, cache hit ratio, DB pool usage, queue lag, idempotency conflicts, and
business invariants. In Datadog I would expect service maps, trace waterfalls, route dashboards,
SLO burn alerts, and drill-down from a failed checkout or transfer to the exact dependency and state
transition.
```

---

## 15. Revision Notes

- One-line summary: Observability turns a request lifecycle into evidence.
- Three keywords: trace ID, structured logs, SLO.
- One interview trap: dashboards without business invariants.
- Memory trick: metrics show smoke, traces show path, logs show story, audit proves truth.

