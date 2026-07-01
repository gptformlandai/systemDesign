# 07 - Decision Matrices And Interview Playbook

> Goal: quickly choose the right design, name rejected options, and explain exactly what fails if the wrong option is chosen.

---

## 1. Master Decision Rule

Start with this:

```text
What user/business promise is this request protecting?
```

Then choose:

| Promise | Architecture Bias |
|---|---|
| fast public reads | CDN/cache/search/eventual |
| user sees own state | session consistency/read-your-writes |
| checkout correctness | idempotency + reservation + workflow |
| money movement | strict ACID ledger |
| analytics | async event pipeline |
| global low latency | regional read models |
| strict global correctness | coordinated writes/distributed SQL |
| high availability during failure | degraded modes and eventual convergence |

---

## 2. Request Lifecycle Cheat Sheet

```text
Client:
  validate, auth context, headers, timeout, idempotency key

DNS/TLS:
  resolve, connect, encrypt, route

CDN/WAF:
  cache public content, block attacks/bots

Load balancer:
  choose region/zone/backend

Gateway:
  auth, quota, route, schema, trace

Service:
  validate domain rules, authorize, idempotency, business logic

Cache:
  accelerate safe reads, never hide source-of-truth needs

Database:
  enforce invariants, transactions, replication, shard strategy

Queue/Event:
  async side effects, fanout, replay, decoupling

Workflow:
  local transaction, saga, orchestration, choreography, 2PC, or reconciliation

Observability:
  logs, metrics, traces, audits, SLOs

Response:
  status code, body, cache headers, retry-after, client rendering
```

---

## 3. CAP Decision Matrix

| Domain | CAP Bias | Why |
|---|---|---|
| product search | AP/eventual | stale results tolerable |
| product detail | AP/bounded stale | validate at checkout |
| recommendations | AP/eventual | derived data |
| cart | AP plus read-your-writes | convenience, merge possible |
| inventory display | AP/stale estimate | final reservation validates |
| inventory reservation | CP/strong per SKU | oversell control |
| order state | CP per order | legal transitions |
| payment state | CP/idempotent | avoid double charge |
| ledger | CP/strict | money correctness |
| notifications | AP/eventual | can retry later |
| analytics | AP/eventual | delayed reporting acceptable |
| permissions | CP or strong enough | access safety |

---

## 4. SQL vs NoSQL Decision Matrix

| Requirement | Good Choice | Bad Choice |
|---|---|---|
| multi-row transaction | PostgreSQL/MySQL/distributed SQL | eventually consistent KV without transaction |
| high-scale user cart | DynamoDB/Cassandra/document/KV | heavily joined normalized schema under huge traffic |
| full-text search | Elasticsearch/OpenSearch | OLTP DB wildcard scans |
| ledger | relational/distributed SQL | cache/search/eventual LWW store |
| product images | object storage + CDN | storing large blobs in hot OLTP rows |
| rate limiter | Redis/gateway limiter | central OLTP DB per request |
| analytics | warehouse/lake | primary OLTP DB for heavy reports |
| graph relationships | graph DB or specialized model | recursive joins at huge scale without plan |

---

## 5. Sharding Required Or Not

Use sharding when:

- single node cannot handle writes
- data volume exceeds operational limits
- hot tenants need isolation
- regional ownership is required
- failure blast radius must be reduced

Do not shard yet when:

- read replicas and indexes solve it
- vertical scaling is enough
- queries require global joins
- team cannot operate resharding
- access patterns are unclear

Wrong option:

```text
Shard because system design interviews expect sharding.
```

What fails:

```text
You add cross-shard transactions, scatter-gather queries, rebalancing, and operational complexity
without solving a real bottleneck.
```

Strong answer:

```text
I would start with a single primary plus replicas/partitioning if scale allows. I introduce sharding
when write throughput, data size, tenant isolation, or regional ownership requires it, and I choose
the shard key from access patterns.
```

---

## 6. Saga vs 2PC vs Local Transaction vs Outbox

| Need | Best Choice | Why |
|---|---|---|
| one DB invariant | local ACID transaction | simplest and strongest |
| internal ledger debit/credit | local/distributed SQL transaction | money invariant |
| commerce checkout across services | orchestrated saga | compensatable workflow |
| notification after order | outbox + event | reliable async side effect |
| search index update | CDC/event stream | derived eventual model |
| external provider settlement | workflow + reconciliation | external uncertainty |
| strict atomicity across supported DB resources | 2PC maybe | accepts blocking/availability trade-off |

Wrong choices:

| Wrong Choice | What Fails |
|---|---|
| saga for ledger debit/credit | money temporarily/permanently inconsistent |
| 2PC with external payment provider | provider does not participate, blocking |
| no outbox after DB write | lost events |
| choreography for complex checkout | invisible workflow and hard debugging |
| local transaction across multiple service DBs | not actually atomic |

---

## 7. Cache Decision Matrix

| Data | Cache? | Policy |
|---|---|---|
| static JS/CSS/images | yes | long TTL immutable |
| public product page | yes | short TTL/stale-while-revalidate |
| product search result | yes maybe | query cache with invalidation limits |
| inventory display | yes short | approximate, final check later |
| cart | maybe | cache/store, user scoped |
| checkout reservation | no as authority | DB/strong KV |
| payment status | no unsafe public cache | authoritative read |
| account balance | avoid or private short projection | never CDN |
| ledger | no cache as source | DB truth |

Wrong option:

```text
Cache everything for performance.
```

What fails:

```text
Privacy leaks, stale money/order state, incorrect checkout, and hard invalidation.
```

Better:

```text
Cache by data sensitivity, freshness tolerance, and source-of-truth boundary.
```

---

## 8. Protocol Decision Matrix

| Protocol | Use |
|---|---|
| HTTP/JSON | public APIs, browser-friendly |
| gRPC/protobuf | internal low-latency typed service calls |
| GraphQL | frontend aggregation and flexible reads |
| WebSocket | bidirectional realtime |
| SSE | server-to-client streaming |
| Kafka protocol/event stream | async durable event pipeline |
| SQL wire protocol | DB access |
| S3/object API | blobs and documents |

Wrong option:

```text
Use WebSocket for every request because it is realtime.
```

What fails:

```text
Operational complexity, connection scaling, load balancing, backpressure, and unnecessary statefulness.
```

Better:

```text
Use request/response HTTP for normal APIs, event streams/queues for async, and WebSocket/SSE only
when realtime push is actually needed.
```

---

## 9. Observability Decision Matrix

| Question | Signal |
|---|---|
| Which users are affected? | logs/RUM/business metrics |
| Where is latency? | distributed trace |
| Which route is failing? | metrics by route/status |
| Which dependency is slow? | APM spans/dependency metrics |
| Is DB saturated? | DB pool, lock, slow query metrics |
| Did queue fall behind? | lag age, consumer rate, DLQ |
| Did a duplicate happen? | idempotency logs/metrics |
| Is ledger correct? | invariant/reconciliation metrics |
| Did deployment cause it? | version tags/deploy markers |

Wrong option:

```text
Only monitor CPU and memory.
```

What fails:

```text
User-visible request failures, dependency latency, queue backlogs, and business invariant failures
remain invisible.
```

Better:

```text
Monitor golden signals, business metrics, dependency health, and invariants.
```

---

## 10. E-Commerce Final Design In 90 Seconds

```text
I split e-commerce into discovery and checkout. Discovery is read-heavy, so I use CDN, search
indexes, product caches, read replicas, and async CDC/event updates. This path chooses availability
and low latency with bounded staleness. Cart is user-scoped and can use a highly available KV/document
store with read-your-writes behavior and conflict merge.

Checkout is different. I require idempotency keys, recalculate price, reserve inventory through a
conditional write or reservation authority, use a payment intent state machine, and create an order
through a state machine. I use an orchestrated saga because payment, inventory, and fulfillment are
multi-step and compensatable, but I avoid 2PC across external providers. I use transactional outbox
so downstream notifications, search, analytics, and fulfillment events are reliable. Observability
tracks checkout funnel, payment timeouts, inventory conflicts, stuck orders, queue lag, and trace
waterfalls.
```

---

## 11. Finance Final Design In 90 Seconds

```text
For finance, I do not optimize the core money movement for eventual consistency. The transfer API
requires authentication, authorization, risk/limit checks, and an idempotency key. The ledger service
uses a strict ACID transaction to write balanced debit and credit entries, update a balance projection
if needed, write audit records, and create an outbox event. If the commit cannot be proven, the
system returns pending or rejects; it does not pretend success.

Notifications, reporting, fraud analytics, and external settlement are async after the ledger commit.
External providers are modeled with pending/settled/rejected states and reconciled later. I reject
cache-as-source-of-truth, last-write-wins stores, and saga for the internal debit/credit invariant.
Datadog should show transfer latency, ledger commit latency, DB locks, idempotency replays, pending
age, reconciliation mismatches, and the invariant that committed ledger transactions are balanced.
```

---

## 12. Common Interview Traps

| Trap | Better Answer |
|---|---|
| "Use Kafka to make it scalable" | explain which events, delivery semantics, idempotency, and consumers |
| "Use Redis for speed" | say whether cache or source of truth |
| "Use sharding" | name shard key, access pattern, resharding, hot partition |
| "Use eventual consistency" | define stale tolerance and reconciliation |
| "Use strong consistency" | name latency/availability cost |
| "Use Saga" | explain compensation and where it is unsafe |
| "Use 2PC" | mention blocking and participant support |
| "Use microservices" | justify ownership, scale, failure boundary |
| "Use Datadog" | name dashboards, traces, metrics, logs, tags, alerts |
| "Retry on failure" | explain idempotency and unknown outcomes |

---

## 13. Final Master Answer Template

```text
I will trace the request from client to response. The client sends auth context, trace headers, and
an idempotency key for writes. DNS/CDN/WAF/load balancers route and protect the request. The gateway
handles auth, quotas, schema, and routing, but not domain workflow. Backend services validate domain
rules and choose cache, database, queue, or workflow based on correctness.

For high-traffic e-commerce, I choose availability and eventual consistency for discovery paths:
CDN, caches, search indexes, read replicas, and async propagation. But checkout uses stronger
boundaries: inventory reservation, payment state, order state, idempotency, and outbox.

For finance, I choose strict consistency for money movement: ACID ledger, immutable double-entry
entries, idempotency, audit, and reconciliation. If correctness cannot be proven, I return pending
or reject. I use observability across the lifecycle: structured logs, trace IDs, APM spans, metrics,
SLOs, dashboards, and audit logs.
```

---

## 14. Revision Notes

- One-line summary: The right architecture follows the business promise of each request path.
- Three keywords: lifecycle, CAP, evidence.
- One interview trap: choosing one pattern for every part of the system.
- Memory trick: ask "what breaks if this answer is stale, duplicated, delayed, or lost?"

