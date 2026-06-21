# Microservices Architecture Review Capstone Case Studies Platinum Sheet

> Track: Microservices Interview Track - Group 4 FAANG Platinum Path  
> Goal: practice owner-level architecture review, boundary defense, migration plans, and trade-off discussion.

Use this after the DDD boundaries, production debugging, and Kubernetes/platform sheets.

---

## 1. Platinum Mindset

A senior-plus microservices answer is not just a diagram.

It must defend:

- business boundaries
- data ownership
- consistency model
- communication style
- failure handling
- observability
- security
- deployment safety
- migration path
- operational ownership
- cost and scale trade-offs

Strong answer:

```text
I judge a microservice design by whether it can be owned, changed, deployed, operated, and
recovered independently without violating business invariants.
```

---

## 2. Architecture Review Rubric

Score each area from 1 to 5.

| Area | 1 - Weak | 5 - Strong |
|---|---|---|
| Boundaries | service per entity/table | capability and invariant based |
| Data ownership | shared DB/direct writes | one owner per business truth |
| Communication | sync everywhere | sync for immediate need, async for side effects |
| Consistency | vague eventual consistency | invariant-specific consistency model |
| Failure handling | retries only | timeouts, idempotency, circuit breaker, DLQ, compensation |
| Observability | logs only | metrics, traces, logs, dashboards, runbooks, SLOs |
| Security | gateway only | edge + service auth + identity + secrets + audit |
| Testing | E2E only | unit, component, integration, contract, smoke |
| Deployment | big bang | canary, rollback, compatibility, migration gates |
| Ownership | unclear teams | explicit service owners and on-call |

Readiness gate:

```text
A design below 4 in data ownership, consistency, observability, or failure handling is not
senior-ready.
```

---

## 3. Review Question Bank

Ask these during any design review:

1. What business capability does each service own?
2. What data does each service own exclusively?
3. Which operations require strong consistency?
4. Which operations tolerate eventual consistency?
5. What happens when each dependency is slow or down?
6. Which calls are on the user-critical path?
7. Where are idempotency keys stored?
8. How are events published reliably?
9. How are duplicate events handled?
10. What is the rollback plan for each deploy?
11. How are contracts validated before deployment?
12. What dashboards prove the flow is healthy?
13. What SLO measures user impact?
14. Which actions need audit logs?
15. Who owns each service in production?

---

## 4. Capstone 1: Hotel Booking Checkout

Prompt:

```text
Design hotel booking checkout where guests reserve a room, authorize payment, receive
confirmation, and earn loyalty points. Prevent duplicate bookings and duplicate charges.
```

Expected services:

- Booking Service
- Availability Service
- Payment Service
- Notification Service
- Loyalty Service
- Reporting Service

Strong design:

```text
Booking owns lifecycle. Availability owns inventory invariant. Payment owns payment audit.
The checkout flow uses a saga. Booking starts PENDING, inventory reserves with idempotency,
payment authorizes with idempotency, Booking confirms, and outbox publishes BookingConfirmed.
Notification and Loyalty consume events asynchronously. DLQ, replay, traces, and dashboards
cover failure handling.
```

Critical trade-offs:

| Decision | Trade-Off |
|---|---|
| sync inventory/payment | immediate correctness but higher latency |
| async notification/loyalty | better checkout latency but eventual consistency |
| orchestration saga | visible workflow but central coordinator |
| outbox | reliable events but extra relay/monitoring |
| idempotency | retry safety but more state and constraints |

Red flags:

- Booking Service directly updates inventory DB
- payment retry without idempotency key
- notification blocks checkout confirmation
- no payment timeout/reconciliation path
- no outbox after booking confirmation

---

## 5. Capstone 2: Payment Extraction From Monolith

Prompt:

```text
A monolith contains booking and payment code. Extract Payment Service without breaking
checkout or financial audit.
```

Migration plan:

```text
1. Identify payment domain boundary and data ownership.
2. Add anti-corruption layer inside monolith.
3. Create Payment Service with its own DB/audit model.
4. Mirror or migrate payment data with validation.
5. Route small percentage of payment authorization through new service.
6. Keep fallback to monolith path during canary.
7. Move capture/refund/reconciliation flows.
8. Remove old code after metrics and audit match.
```

Controls:

- contract tests
- reconciliation reports
- idempotency keys across old/new paths
- audit trail comparison
- canary by tenant/hotel/region
- rollback plan

Strong answer:

```text
I would not big-bang extract payment. I would build an anti-corruption layer, run the new
service in parallel or canary, reconcile financial records, and only cut over after behavior,
audit, and rollback paths are proven.
```

---

## 6. Capstone 3: Search Read Model At Scale

Prompt:

```text
Search must be fast and filter by hotel, room type, price, location, availability, and rating.
Writes come from multiple services.
```

Design:

```text
Hotel Service -> HotelUpdated events
Pricing Service -> PriceChanged events
Availability Service -> AvailabilityChanged events
Review Service -> RatingUpdated events
  -> Search Indexer -> Search index/read model
```

Correctness rule:

```text
Search results may be slightly stale. Final availability and price are checked during booking.
```

Key concerns:

- event ordering by hotel/room/date key
- idempotent index updates
- rebuild/replay strategy
- freshness SLO
- index lag dashboard
- schema evolution for search documents

Strong answer:

```text
Search is a read model optimized for query speed, not the owner of booking correctness. I
track index freshness and validate price/availability at checkout.
```

---

## 7. Capstone 4: Multi-Region Booking

Prompt:

```text
The platform must survive regional outage while preventing duplicate room sales.
```

Key question:

```text
Which data can be active-active, and which data needs a single writer or conflict strategy?
```

Possible design:

| Domain | Multi-Region Strategy |
|---|---|
| search | active-active with replicated index |
| hotel content | active-active/eventual consistency |
| notifications | regional workers with retry |
| loyalty | ledger with conflict-safe design |
| booking/inventory | single writer per hotel/region or strong coordination |
| payment | provider-dependent, reconciliation required |

Strong answer:

```text
I do not casually make inventory active-active if duplicate booking is unacceptable. I would
partition ownership by hotel/region or use a strongly consistent reservation path, while
allowing search and non-critical reads to be eventually consistent.
```

---

## 8. Capstone 5: Loyalty Points Ledger

Prompt:

```text
Award, redeem, reverse, and audit loyalty points across booking, cancellation, and customer
support workflows.
```

Good model:

```text
Use an append-only ledger, not a mutable points balance as the source of truth.
```

Events:

- PointsAwarded
- PointsRedeemed
- PointsReversed
- PointsExpired
- PointsAdjustedBySupport

Invariants:

- no negative balance if business rule forbids it
- every adjustment has reason and actor
- reversal references original ledger entry
- duplicate booking event does not award twice

Strong answer:

```text
For loyalty, I prefer a ledger because auditability matters. The current balance is a derived
read model, while the ledger is the source of truth.
```

---

## 9. Capstone 6: API Gateway Becoming A Monolith

Prompt:

```text
The API Gateway now handles pricing rules, booking orchestration, loyalty checks, and custom
client responses. Deployments are risky.
```

Diagnosis:

```text
The gateway has become a domain monolith.
```

Fix:

- move domain rules back to owning services
- use BFF only for client-specific shaping
- use orchestrator/service for workflow if needed
- keep gateway focused on edge concerns
- add ownership boundaries and review process

Strong answer:

```text
A gateway should route, authenticate, rate limit, and handle edge concerns. It should not
own pricing, booking, or payment domain rules.
```

---

## 10. Migration Review Template

Use this for any monolith-to-microservice migration:

```text
Current state:
Target boundary:
Data owner:
APIs/events:
Compatibility plan:
Data migration plan:
Dual-write avoidance plan:
Validation/reconciliation:
Canary strategy:
Rollback strategy:
Observability:
Ownership/on-call:
Deprecation cleanup:
```

Strong migration rule:

```text
A migration is not done when traffic moves. It is done when old paths are removed, ownership
is clear, and dashboards/runbooks are in place.
```

---

## 11. Boundary Smell Scoring

Score each proposed service split.

| Smell | Score Impact |
|---|---|
| service maps to table name | high risk |
| shared database writes | high risk |
| every request needs 5 services | high risk |
| teams must deploy together | high risk |
| service has no clear owner | high risk |
| no independent data invariant | medium risk |
| only CRUD with no business logic | medium risk |
| unclear SLO/runbook | medium risk |

Decision:

```text
If the split has many smells, keep it as module inside monolith until boundary is clearer.
```

---

## 12. Trade-Off Language For Interviews

Use these phrases:

```text
I would start with the invariant.
I would keep this strongly consistent inside one owner.
I would move non-critical side effects to events.
I would avoid fan-out on the critical path.
I would make retries safe with idempotency.
I would validate compatibility before deployment.
I would monitor the async path, not only the HTTP path.
I would keep a rollback path until consumers migrate.
```

Avoid:

```text
Just use Kafka.
Just use Saga.
Just split by table.
Just make it active-active.
Just retry.
```

---

## 13. Final Architecture Review Checklist

A platinum answer must include:

1. boundaries by business capability
2. explicit data ownership
3. local transaction and saga boundaries
4. sync/async decision per operation
5. idempotency and duplicate handling
6. outbox/event reliability
7. contract and schema evolution plan
8. observability and SLOs
9. security and audit controls
10. deployment/migration/rollback strategy
11. operational ownership
12. cost and scale trade-offs

---

## 14. Strong Closing Answer

```text
When reviewing a microservice architecture, I start with business invariants and ownership,
not technology. I check whether each service owns data, can deploy independently, handles
partial failure, exposes compatible contracts, is observable and secure, and has a migration
and rollback plan. That is the difference between drawing microservices and owning them in
production.
```
