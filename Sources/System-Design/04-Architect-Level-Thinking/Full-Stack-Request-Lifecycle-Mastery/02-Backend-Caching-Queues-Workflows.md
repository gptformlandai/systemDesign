# 02 - Backend Caching Queues Workflows

> Goal: understand what happens after the gateway: service boundaries, synchronous calls, caching, queues, event streams, sagas, orchestration, choreography, 2PC, outbox, idempotency, and failure recovery.

---

## 1. Intuition

Backend architecture is a factory floor.

- Services own specific jobs.
- Caches keep hot items near workers.
- Queues buffer work that can happen later.
- Workflows coordinate multi-step business processes.
- Databases preserve truth.
- Observability lets you prove what happened.

Beginner line:

```text
The backend should keep fast request paths fast, protect correctness-critical writes, and move
non-critical side effects to reliable async flows.
```

---

## 2. Service Boundary Options

| Architecture | Best For | What Fails If Misused |
|---|---|---|
| monolith | early product, small team | can become tangled and hard to scale by domain |
| modular monolith | strong code boundaries with simple deployment | still one deploy/runtime blast radius |
| microservices | independent ownership/scale/failure isolation | distributed complexity if split too early |
| serverless functions | event/API tasks, bursty workloads | cold starts, orchestration complexity |
| BFF | frontend-specific aggregation | can become duplicated business logic |
| service mesh | mTLS, traffic policy, observability | operational complexity if app is simple |

Decision rule:

```text
Split services when ownership, scale, data model, release cadence, or failure isolation justifies
the network boundary.
```

Wrong option:

```text
Split every entity into its own microservice: UserService, AddressService, CartLineService,
CouponLineService, PriceService, TaxLineService.
```

What fails:

```text
Every user action becomes a distributed transaction or chatty network graph. Latency, debugging,
and consistency become painful.
```

Better:

```text
Split by business capability and transaction boundary: catalog, cart, pricing, checkout, order,
payment, fulfillment.
```

---

## 3. Synchronous Backend Calls

Use synchronous calls when:

- caller needs answer now
- operation is short
- dependency is reliable enough
- user is waiting
- result affects immediate decision

Examples:

- validate session
- fetch cart
- reserve inventory during checkout
- authorize payment
- read account balance

Risks:

- cascading failures
- latency accumulation
- retry storms
- tight coupling

Mitigations:

- timeouts
- retries only when safe
- circuit breakers
- bulkheads
- fallback responses
- request hedging only for idempotent reads

Wrong option:

```text
Make checkout synchronously call email, analytics, recommendation, search indexing, warehouse,
fraud batch export, and notification services before responding.
```

What fails:

```text
Checkout latency and availability become the product of every dependency. Any non-critical service
can block revenue.
```

Better:

```text
Keep critical path minimal. Commit order/payment state, then emit durable events for async side effects.
```

---

## 4. Cache Patterns

Cache-aside:

```text
app reads cache
if miss: read DB, populate cache
on write: update DB, invalidate/update cache
```

Read-through:

```text
app asks cache
cache loads from DB on miss
```

Write-through:

```text
write goes to cache and DB together
```

Write-behind:

```text
write cache now, persist later
```

Stale-while-revalidate:

```text
serve stale quickly, refresh in background
```

Best fits:

| Data | Cache Pattern |
|---|---|
| product details | CDN/Redis cache-aside with TTL |
| product images | CDN immutable cache |
| inventory display | short TTL or stale estimate |
| checkout reservation | no cache as authority |
| account balance | avoid cache as authority |
| feature flags/config | local cache with versioning |
| rate limits | Redis token bucket |
| sessions | Redis/session store or signed token strategy |

Wrong option:

```text
Use write-behind cache for payment ledger entries.
```

What fails:

```text
Cache loss or async persistence failure can lose money movement records.
```

Better:

```text
Commit ledger entries synchronously to durable transactional storage. Use cache only for derived reads.
```

---

## 5. Cache Failure Modes

| Failure | User/System Impact | Mitigation |
|---|---|---|
| cache stampede | DB overloaded after hot key expiry | jittered TTL, request coalescing, locks |
| hot key | one cache shard overloaded | key splitting, local cache, replication |
| stale cache | wrong UI state | TTL, invalidation, versioning |
| cache penetration | misses for nonexistent keys hit DB | negative caching, bloom filters |
| cache avalanche | many keys expire together | TTL jitter, warmup |
| cache outage | backend overload | graceful degradation, circuit breaker |

Wrong option:

```text
If Redis is down, send all traffic directly to the database with no limits.
```

What fails:

```text
Database receives sudden uncached load and may fail, turning a cache outage into full outage.
```

Better:

```text
Use circuit breakers, local stale cache for safe data, admission control, and degraded responses.
```

---

## 6. Queues And Event Streams

Queue:

```text
Work item is consumed by one worker or consumer group.
```

Pub/sub:

```text
Event is broadcast to multiple subscribers.
```

Event log:

```text
Events are retained and replayable by offset.
```

Tools:

| Tool | Strong Fit |
|---|---|
| Kafka | durable high-throughput event log, replay, stream processing |
| SQS | simple managed job queue |
| SNS/PubSub | fanout |
| RabbitMQ | routing and work queues |
| Pulsar | multi-tenant geo-replicated messaging |
| Kinesis | AWS streaming ingestion |
| Redis streams | lightweight stream within Redis ecosystem |

Use async for:

- notifications
- analytics
- search indexing
- cache invalidation
- fulfillment tasks
- fraud scoring that can return later
- image processing
- report generation
- reconciliation

Do not use async blindly for:

- immediate authorization result
- strict balance update
- checkout inventory commit unless designed as reservation workflow
- permission decisions that must be current

Wrong option:

```text
Put payment charge requests on a queue and immediately show "paid" to the user.
```

What fails:

```text
Payment may fail later, creating incorrect order state and user trust issues.
```

Better:

```text
Return pending/processing if async payment is required, or synchronously create a payment intent and
only mark paid after confirmed authorization/capture.
```

---

## 7. Delivery Guarantees

| Guarantee | Meaning | Reality |
|---|---|---|
| at-most-once | may lose, no duplicates | risky for important business events |
| at-least-once | no loss if system works, duplicates possible | common and practical |
| exactly-once | effect occurs once | usually achieved by idempotent processing and transactions |

Senior truth:

```text
Most production systems should assume at-least-once delivery and build idempotent consumers.
```

Consumer idempotency:

```text
1. Read event with eventId
2. Check processed_event table/store
3. If seen, ack and skip
4. If new, process in transaction
5. Record eventId as processed
6. Ack message
```

Wrong option:

```text
Assume Kafka/SQS guarantees the business side effect only happens once.
```

What fails:

```text
Consumers crash after side effect but before ack, messages are redelivered, and side effects repeat.
```

Better:

```text
Make side effects idempotent using event IDs, natural keys, unique constraints, and state machines.
```

---

## 8. Transactional Outbox And Inbox

Problem:

```text
You need to update database state and publish an event. If these are separate operations, one can
succeed while the other fails.
```

Transactional outbox:

```text
1. Begin DB transaction
2. Write business state
3. Write outbox row/event in same transaction
4. Commit
5. Relay reads outbox and publishes to broker
6. Mark outbox published or rely on idempotent publish
```

Inbox:

```text
Consumer records received event ID before/with side effect to prevent duplicate processing.
```

Use for:

- order created events
- payment state changes
- inventory reserved events
- ledger notification events
- shipping task events

Wrong option:

```text
Publish event first, then write database state.
```

What fails:

```text
Downstream consumers see an event for state that does not exist if the DB write fails.
```

Better:

```text
Write state and outbox atomically, then publish from the outbox.
```

---

## 9. Workflow Coordination Options

| Option | Best For | Weakness |
|---|---|---|
| local DB transaction | single service/database invariant | cannot span many services cleanly |
| 2-phase commit | strict atomic commit across participants | blocking, fragile, poor availability |
| saga orchestration | multi-step business workflows with compensation | eventual consistency, coordinator complexity |
| saga choreography | decentralized event reactions | hard to reason/debug at scale |
| workflow engine | durable long-running processes | platform dependency and learning curve |
| manual reconciliation | external/legacy processes | delayed correction |

Workflow engines:

- Temporal
- Cadence
- Camunda
- Step Functions
- Conductor

---

## 10. Saga Pattern

Saga:

```text
A sequence of local transactions where each step has a compensating action if later steps fail.
```

E-commerce checkout saga:

```text
1. Validate cart
2. Reserve inventory
3. Authorize payment
4. Create order
5. Confirm inventory
6. Capture payment
7. Start fulfillment

Compensations:
- release inventory
- void authorization/refund payment
- cancel order
```

When saga fits:

- business process spans services
- each step can commit locally
- compensation is possible
- temporary inconsistency is acceptable
- workflow is long-running

When saga is wrong:

- invariant must be atomically true at every instant
- compensation cannot undo harm
- money ledger must never temporarily violate accounting rules

Wrong option:

```text
Use saga to debit one bank account now and maybe credit another later.
```

What fails:

```text
Money can disappear temporarily or permanently if compensation/retry fails. Audit and regulatory
requirements may fail.
```

Better:

```text
For internal ledger movement, use an atomic transaction that writes balanced debit/credit entries.
Use saga only for external settlement workflows around the ledger.
```

---

## 11. Orchestration vs Choreography

Orchestration:

```text
One coordinator decides next step.
```

Pros:

- easier to understand
- centralized state machine
- easier retries/timeouts
- better for checkout/payment workflows

Cons:

- coordinator is critical dependency
- can become too powerful

Choreography:

```text
Services publish events and react independently.
```

Pros:

- loose coupling
- independent services
- natural for fanout side effects

Cons:

- hard to trace
- event storms
- hidden workflow
- cyclic dependencies

Decision:

| Workflow | Better Choice |
|---|---|
| checkout order workflow | orchestration |
| notification fanout | choreography |
| search index update | choreography |
| payment state machine | orchestration plus provider webhooks |
| finance ledger transaction | local strict transaction, not saga |

Wrong option:

```text
Use pure choreography for a complex checkout flow with inventory, payment, order, fulfillment, and refunds.
```

What fails:

```text
No single place knows the checkout state. Debugging stuck orders becomes painful.
```

Better:

```text
Use an orchestrator for the critical workflow and events for non-critical fanout.
```

---

## 12. Two-Phase Commit

2PC flow:

```text
1. Coordinator asks all participants to prepare
2. Participants lock resources and vote yes/no
3. If all yes, coordinator sends commit
4. Otherwise coordinator sends abort
```

Use when:

- strict atomicity across resources is mandatory
- participants support XA/2PC correctly
- lower availability is acceptable
- transaction duration is short

Avoid when:

- high-availability distributed services
- long-running workflows
- external providers
- user-facing high-latency flows
- participants cannot hold locks safely

Wrong option:

```text
Use 2PC across inventory service, payment gateway, order service, and shipping provider.
```

What fails:

```text
External providers do not participate reliably, locks are held too long, coordinator failure blocks
participants, and availability suffers.
```

Better:

```text
Use local transactions plus orchestrated saga for commerce workflow. Use strict DB transaction only
inside a bounded service/data store that truly needs atomicity.
```

---

## 13. Idempotency Everywhere

Idempotency means:

```text
Repeating the same logical operation produces the same final effect.
```

Required for:

- checkout submit
- payment charge/capture/refund
- transfer request
- webhook processing
- order creation
- inventory reservation
- queue consumers

Common design:

```text
idempotency_key
actor_id
operation_type
request_hash
status
response_body
created_at
expires_at
```

Rules:

- same key + same request = return same result
- same key + different request = reject conflict
- store result for retry window
- protect with unique constraint
- include business ID where possible

Wrong option:

```text
Client retries POST /orders after timeout without idempotency.
```

What fails:

```text
Duplicate orders or duplicate charges can happen.
```

Better:

```text
Require idempotency key for externally retried write APIs.
```

---

## 14. E-Commerce Backend Choice

Chosen critical split:

| Path | Design |
|---|---|
| browse/search | CDN/search/cache, eventual consistency |
| cart | fast store, read-your-writes preferred, conflict merge |
| checkout | orchestrated workflow |
| inventory reservation | conditional write/transaction per SKU |
| payment | payment intent state machine and idempotent provider calls |
| order | durable order state machine |
| notifications | async events |
| analytics/recommendations | event stream |

Rejected:

| Wrong Choice | Failure |
|---|---|
| one giant synchronous checkout chain | low availability |
| pure eventual inventory commit | oversell |
| 2PC across all services | poor availability and provider incompatibility |
| cache as order source of truth | lost/corrupt order state |

---

## 15. Finance Backend Choice

Chosen critical split:

| Path | Design |
|---|---|
| transfer request | strict validation and idempotency |
| ledger write | ACID transaction with balanced entries |
| balance read | derived from ledger or transactionally maintained projection |
| external settlement | async workflow with reconciliation |
| notifications | async after commit |
| reporting | append-only events/warehouse |

Rejected:

| Wrong Choice | Failure |
|---|---|
| saga for internal debit/credit | temporary or permanent imbalance |
| cache-first balance | stale/wrong money display |
| at-most-once events | lost audit/reporting updates |
| no reconciliation | provider/internal drift remains hidden |

---

## 16. Interview Answer Template

```text
In the backend, I separate immediate user-facing decisions from side effects. The request service
validates, authorizes, checks idempotency, and commits the minimum durable state needed. Cache is
used for read acceleration, not as correctness authority. Non-critical side effects go through a
durable queue or event stream. For multi-service commerce workflows, I prefer an orchestrated saga
with transactional outbox and compensations. I reject 2PC across external providers because it hurts
availability and often is not supported. For finance ledger movement, I do not use saga for the
core debit/credit invariant; I use a strict ACID transaction and immutable ledger entries.
```

---

## 17. Revision Notes

- One-line summary: Backend mastery is knowing what must be synchronous, what can be cached, and what should become durable async work.
- Three keywords: idempotency, outbox, workflow.
- One interview trap: using saga where an atomic ledger transaction is required.
- Memory trick: commit truth first, publish consequences second, observe everything.

