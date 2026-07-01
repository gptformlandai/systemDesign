# Full Stack Request Lifecycle Mastery

> Goal: become the engineer who can trace one user request from browser/app formation to edge security, gateway, services, caches, queues, databases, observability, CAP trade-offs, and the final response, while explaining which design choices are right, which are wrong, and what fails under load.

---

## How To Use This Pack

This is a beginner-to-mastery pack for full-stack/system-design interviews and senior architecture discussions.

Read it in this order:

| Order | File | What It Builds |
|---:|---|---|
| 1 | [00 - Complete Request Lifecycle Master Map](00-Complete-Request-Lifecycle-Master-Map.md) | One end-to-end mental model from client request to response |
| 2 | [01 - Edge Security Gateway Load Balancing Rate Limiting](01-Edge-Security-Gateway-LoadBalancing-RateLimiting.md) | DNS, CDN, WAF, TLS, gateway, routing, rate limits, overload control |
| 3 | [02 - Backend Caching Queues Workflows](02-Backend-Caching-Queues-Workflows.md) | Service decomposition, cache patterns, async messaging, sagas, outbox |
| 4 | [03 - Data CAP Sharding Transactions](03-Data-CAP-Sharding-Transactions.md) | SQL vs NoSQL, CAP/PACELC, replication, sharding, transactions, ledgers |
| 5 | [04 - Observability Logs Traces Datadog](04-Observability-Logs-Traces-Datadog.md) | Logs, metrics, traces, RUM, APM, Datadog dashboard and incident view |
| 6 | [05 - Availability E-Commerce Eventual Consistency Architecture](05-Availability-Ecommerce-Eventual-Consistency-Architecture.md) | High-traffic commerce system optimized for availability and graceful inconsistency |
| 7 | [06 - Consistency Finance Strict Transactions Architecture](06-Consistency-Finance-Strict-Transactions-Architecture.md) | Finance system optimized for strict consistency, auditability, and correctness |
| 8 | [07 - Decision Matrices And Interview Playbook](07-Decision-Matrices-And-Interview-Playbook.md) | Right choices, wrong choices, failure explanations, final answer templates |

---

## Master Mental Model

A request lifecycle is not only:

```text
client -> server -> database -> response
```

At production scale, it is closer to:

```text
client
  -> DNS
  -> CDN / edge cache
  -> WAF / bot defense
  -> TLS termination
  -> global load balancer
  -> regional load balancer / ingress
  -> API gateway
  -> authentication / authorization
  -> rate limiter / quota / abuse controls
  -> service mesh / backend service
  -> cache
  -> database / search / object store / queue
  -> downstream services
  -> observability pipeline
  -> response path
  -> client render / retry / cache update
```

Senior engineers know where each hop adds:

- latency
- security checks
- failure modes
- retries
- state
- consistency choices
- observability signals
- cost

---

## Two CAP Worlds In This Pack

### World A: High-Traffic E-Commerce Availability

Product promise:

```text
Browsing, search, recommendations, cart, and product pages should remain available even during
traffic spikes, partial failures, stale indexes, and regional degradation.
```

Consistency posture:

- Product catalog reads can be stale.
- Search index can lag.
- Recommendations can be stale.
- Cart can use eventual consistency with conflict handling.
- Inventory display can be approximate.
- Checkout/payment/order creation need stronger correctness boundaries.

Chosen architecture:

```text
AP for read-heavy discovery paths, with bounded staleness and reconciliation.
Strong local transactions for checkout-critical state such as reservation, payment, and order.
```

### World B: Finance Strict Consistency

Product promise:

```text
Money must not disappear, duplicate, or be visible incorrectly. If correctness cannot be guaranteed,
the system should reject, delay, or mark pending instead of pretending success.
```

Consistency posture:

- Ledger writes are strongly consistent.
- Account balance is derived from immutable ledger entries.
- Idempotency is mandatory.
- Auditing is mandatory.
- Reconciliation is mandatory.
- Availability can degrade before correctness is violated.

Chosen architecture:

```text
CP for money movement and ledger writes, with strict transactions, idempotency, immutable audit,
and reconciliation.
```

---

## The Core Interview Skill

Do not say:

```text
I will use Kafka, Redis, Kubernetes, Cassandra, and microservices.
```

Say:

```text
For this request path, product discovery is read-heavy and can tolerate stale data, so I will use
CDN, search indexes, Redis caches, async indexing, and multi-region read availability. Checkout
changes correctness posture: inventory reservation, payment state, and order creation need
idempotent writes and transactional boundaries. I reject a single global transaction across every
service because it harms availability and operational simplicity. I use an orchestrated saga with
transactional outbox for commerce, but for the finance ledger I use strict ACID transactions and
immutable double-entry ledger records.
```

That is the level this pack trains.

