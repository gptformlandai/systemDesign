# Microservices DDD Service Decomposition Boundaries Platinum Sheet

Target: starter, intermediate, senior, and MAANG-level microservices interviews.

This sheet answers the hardest practical question in microservices:

```text
Where should one service end and another service begin?
```

Most bad microservice systems fail because the split is wrong. This guide teaches service
decomposition using business capability, bounded context, data ownership, team ownership,
runtime coupling, and migration safety.

---

## 0. How To Use This Guide By Level

| Level | Focus |
|---|---|
| Starter | Understand monolith vs microservices and business capability split |
| Intermediate | Learn bounded context, data ownership, API ownership, and anti-corruption layer |
| Senior | Reason about coupling, transaction boundaries, team ownership, migration sequence |
| MAANG-ready | Defend service boundaries under scale, incidents, org changes, and product evolution |

Gold rule:

```text
Do not split by tables, controllers, or technical layers. Split by business ownership and
change boundaries.
```

---

# 1. The Core Problem

Microservices are not primarily a technology decision. They are an ownership decision.

Bad split:

```text
UserController Service
BookingRepository Service
PaymentUtil Service
NotificationHelper Service
```

This creates network calls around technical layers and makes every feature distributed.

Good split:

```text
Booking Service
Availability Service
Payment Service
Notification Service
Loyalty Service
Pricing Service
```

Each service owns a business capability, its rules, its data, and its operational health.

---

# 2. Business Capability

## Definition

A business capability is something the business must be able to do, independent of the
software implementation.

Examples:

| Product | Business Capabilities |
|---|---|
| Hotel booking | search hotels, manage availability, create booking, collect payment |
| Food delivery | discover restaurants, manage cart, assign delivery partner, settle payment |
| E-commerce | catalog, cart, order, inventory, payment, shipment |
| Banking | account, transfer, fraud check, statement, notification |

Strong answer:

```text
I start decomposition from business capabilities because they change at different speeds,
have different owners, and usually map to different data invariants.
```

---

# 3. Bounded Context

## Definition

A bounded context is a boundary where a domain model has one clear meaning.

Example:

```text
"Room" in Search Service:
  A searchable offering with price, images, location, filters.

"Room" in Availability Service:
  An inventory unit with date-level availability and reservation constraints.

"Room" in Housekeeping Service:
  A physical space with cleaning state and maintenance state.
```

Same word, different model. That is a signal for separate bounded contexts.

## Interview Point

Do not force one universal model across all services. It creates a distributed monolith.

---

# 4. Service Boundary Checklist

Use this checklist before creating a service.

| Question | Good Signal |
|---|---|
| Does this capability have its own business rules? | yes |
| Does it change for different reasons than other features? | yes |
| Can one team own it end to end? | yes |
| Does it own data and invariants? | yes |
| Can it expose APIs/events instead of sharing tables? | yes |
| Can failures be isolated? | yes |
| Can it be deployed independently without risky coordination? | yes |

If most answers are no, keep it inside the same service or modular monolith.

---

# 5. Data Ownership Rule

The strongest microservice boundary is data ownership.

```text
One service owns one business truth.
Other services may read copies, but they do not directly write the owner's database.
```

Example:

| Data | Owner |
|---|---|
| room inventory | Availability Service |
| booking lifecycle | Booking Service |
| payment authorization | Payment Service |
| notification status | Notification Service |
| loyalty points | Loyalty Service |

Wrong:

```text
Booking Service writes payment.status in Payment DB.
Payment Service writes booking.status in Booking DB.
```

Right:

```text
Booking Service emits BookingCreated.
Payment Service authorizes payment and emits PaymentAuthorized.
Booking Service consumes PaymentAuthorized and confirms booking.
```

---

# 6. Transaction Boundary

If a workflow requires one atomic database transaction across two proposed services, the
boundary may be wrong or the workflow needs saga/eventual consistency.

Good local transaction:

```text
Booking Service:
  create pending booking
  insert outbox event
  commit
```

Distributed workflow:

```text
Booking pending -> inventory reserved -> payment authorized -> booking confirmed
```

Interview maturity:

```text
I keep strong consistency inside one service boundary. Across service boundaries I use saga,
idempotency, outbox, and compensating actions.
```

---

# 7. Coupling Types

| Coupling | Meaning | Risk |
|---|---|---|
| Runtime coupling | Service A needs Service B online for every request | latency and availability impact |
| Data coupling | Services need each other's database tables | broken ownership |
| Deployment coupling | Services must deploy together | fake independence |
| Semantic coupling | One change changes another service's meaning | hidden contract risk |
| Team coupling | Every feature requires many teams | delivery slows down |

Platinum answer:

```text
I do not judge boundaries only by code shape. I check runtime, data, deployment, semantic,
and team coupling.
```

---

# 8. Good vs Bad Service Splits

## Bad: Entity-Based Split

```text
User Service
Order Service
OrderItem Service
Address Service
Payment Service
```

Problem:

- each business request needs many network calls
- transactions become distributed
- entities are not business capabilities

## Better: Capability-Based Split

```text
Customer Account Service
Order Management Service
Payment Service
Fulfillment Service
Notification Service
```

Why better:

- fewer cross-service calls for core workflows
- clearer ownership
- easier incident routing
- stronger local invariants

---

# 9. Modular Monolith First

Not every system should start with microservices.

Use modular monolith when:

- team is small
- domain boundaries are unclear
- traffic is modest
- deployment coordination is acceptable
- product is still changing heavily

Structure:

```text
src/main/java/com/company/booking
src/main/java/com/company/payment
src/main/java/com/company/availability
```

Rules:

- separate packages/modules
- no direct cross-module repository access
- communicate through interfaces/events inside the process
- keep database ownership logical even if physically same database at first

Strong answer:

```text
I would start with a modular monolith if boundaries are not stable. Once one capability has
clear ownership, scale, or deployment needs, I extract it using the strangler pattern.
```

---

# 10. Anti-Corruption Layer

When extracting from a legacy monolith, do not let the new service inherit the old model
directly.

```text
New Booking Service -> Anti-Corruption Layer -> Legacy Reservation Module
```

The ACL translates:

- old field names to new domain names
- legacy statuses to clean states
- old error codes to new domain errors
- legacy synchronous behavior to new workflow behavior

Use it when:

- legacy model is messy
- external provider model does not match your domain
- migration must be gradual

Do not overuse it for simple DTO mapping.

---

# 11. Domain Events

Domain events are facts that already happened.

Good names:

```text
BookingCreated
InventoryReserved
PaymentAuthorized
BookingConfirmed
BookingCancelled
```

Bad names:

```text
CreateBooking
ReserveInventory
AuthorizePayment
```

Commands ask for work. Events announce facts.

Interview line:

```text
Events should be named in past tense because consumers should react to facts, not control
the owner's internal workflow.
```

---

# 12. Ownership Matrix

For every service, define this before implementation.

| Area | Owner |
|---|---|
| Code | team name |
| Database | service team |
| API contract | service team with consumers consulted |
| Event schema | producer owns schema, consumers validate compatibility |
| SLO | service team |
| Dashboard | service team |
| Runbook | service team |
| On-call escalation | service team |

If no team owns these, the service is not production-ready.

---

# 13. Boundary Smell Catalog

| Smell | What It Means | Fix |
|---|---|---|
| Chatty service calls | boundary too fine-grained | merge or add read model |
| Shared database | ownership unclear | move writes behind owner API |
| Common library with domain logic | hidden coupling | move logic to owner service |
| Same deploy window | deployment coupling | stabilize contracts |
| Every request calls five services | workflow split badly | use orchestration/read model |
| Large "common service" | dumping ground | split by business capability |
| Consumer needs producer internals | weak contract | publish better API/event |

---

# 14. Hotel Booking Decomposition

## Candidate Services

| Service | Owns | Why Separate |
|---|---|---|
| Search | search index, filters | high read traffic and denormalized views |
| Availability | date-level inventory | strict concurrency and reservation rules |
| Booking | booking lifecycle | main customer order state |
| Payment | authorization, capture, refund | security and provider integration |
| Pricing | rates, discounts, taxes | business rules change often |
| Notification | email/SMS/push delivery | async side effect |
| Loyalty | points and tiers | separate business policy |

## Flow

```text
1. Search Service returns hotel options from read model.
2. Availability Service checks room inventory.
3. Booking Service creates pending booking.
4. Availability Service reserves inventory.
5. Payment Service authorizes payment.
6. Booking Service confirms booking.
7. Notification and Loyalty react asynchronously.
```

## Strong Answer

```text
I split hotel booking by business capability. Availability owns inventory because that is
the core concurrency invariant. Booking owns lifecycle because the customer sees booking
state. Payment is separate because it has security and provider integration concerns. Search
uses a denormalized read model because it is read-heavy and does not need to own inventory.
```

---

# 15. When Not To Split

Avoid creating a separate service when:

- it has no independent owner
- it is just a CRUD table
- it is always deployed with another service
- it cannot own data
- it has no independent scaling need
- failure cannot be isolated
- every feature requires cross-service transaction

Use:

- package/module
- library for pure technical utilities
- modular monolith boundary
- database schema separation

---

# 16. Interview Decision Tree

```text
Is it a business capability?
  No -> keep as module/library.
  Yes -> continue.

Does it own business data and invariants?
  No -> probably not a service yet.
  Yes -> continue.

Does it change/scale/fail independently?
  No -> modular monolith may be enough.
  Yes -> service candidate.

Can cross-boundary consistency be eventual?
  No -> recheck boundary or keep inside one transaction.
  Yes -> use API/events/saga/outbox.
```

---

# 17. Practical Interview Question

> You are designing a hotel booking platform. How would you split services and how would
> you decide whether Availability and Booking should be one service or two?

Strong answer:

```text
I would first identify business invariants. Availability owns inventory by hotel, room type,
and date. Booking owns customer-visible lifecycle. If the product is early and the same team
owns both, I may keep them in one modular monolith module or one service to preserve local
transaction simplicity. If availability has high concurrency, separate scaling, and a clear
team owner, I split it into its own service. The booking workflow then becomes a saga:
create pending booking, reserve inventory, authorize payment, confirm booking. I protect
correctness with idempotency keys, unique constraints, outbox events, and compensation for
payment or inventory failures.
```

---

# 18. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Splitting by table | creates CRUD microservices | split by capability |
| Shared DB | breaks service autonomy | owner API/events |
| Too many tiny services | latency and ops overhead | modular monolith first |
| Ignoring teams | no real ownership | align service to team |
| Universal domain model | semantic coupling | bounded contexts |
| Sync everything | cascading failures | async where side effects can lag |
| No migration path | risky big bang | strangler and ACL |

---

# 19. One-Hour Revision Plan

First 15 minutes:

- business capability
- bounded context
- data ownership

Next 15 minutes:

- transaction boundary
- coupling types
- modular monolith first

Next 15 minutes:

- hotel booking decomposition
- domain events
- anti-corruption layer

Final 15 minutes:

- smell catalog
- decision tree
- strong answer practice

---

# 20. Final Memory Trick

```text
Boundary = Business + Data + Team + Change + Failure.
```

If a proposed service does not have these five things, it is probably not a real microservice
yet.

---

# 21. Official Source Notes

- Spring microservices overview: https://spring.io/microservices
- Kubernetes services: https://kubernetes.io/docs/concepts/services-networking/service/
- Kubernetes probes and lifecycle: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/
