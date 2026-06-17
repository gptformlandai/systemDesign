# Microservices Data Consistency Transactions FAANG Master Sheet

Target: starter, intermediate, senior, and FAANG-level microservices interviews.

This sheet covers:
- database per service
- shared database anti-pattern
- joins across services
- distributed transactions
- Saga
- Outbox
- CDC
- idempotency
- eventual consistency UX
- read-your-writes
- compensation failure
- reporting and analytics
- schema evolution and data migration

Goal:

```text
After reading this sheet, you should be able to explain how microservices own data, why
distributed transactions are hard, how to design reliable workflows, and how to make
eventual consistency understandable to users.
```

---

## 0. How To Use This Guide By Level

| Level | What To Focus On |
|---|---|
| Starter | database per service, shared DB anti-pattern, local transactions |
| Intermediate | Saga, Outbox, idempotency, CQRS/read models |
| Senior | compensation failure, CDC, reporting, read-your-writes |
| FAANG-ready | invariants, consistency models, migration safety, operational recovery |

Must-say line:

```text
Microservices do not remove transactions. They force us to decide where strong consistency
is required and where eventual consistency is acceptable.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Database per service | Very high | Core microservice rule |
| Shared DB anti-pattern | Very high | Distributed monolith signal |
| Local transaction | Very high | Correctness boundary |
| Saga | Very high | Cross-service workflow |
| Outbox | Very high | Reliable event publishing |
| Idempotency | Very high | Retry safety |
| CQRS/read model | High | Cross-service query solution |
| CDC | High | Event publishing and replication |
| Eventual consistency | Very high | User experience and correctness |
| Read-your-writes | High | UX expectation |
| Compensation failure | Very high | Senior-level workflow reality |
| Reporting | High | Cross-service data aggregation |
| Schema evolution | High | Independent deploy safety |

---

# 2. Data Ownership Rule

Each service owns its data.

Example:

| Service | Owns |
|---|---|
| Booking Service | booking lifecycle |
| Availability Service | room inventory |
| Payment Service | payment authorization/capture |
| Loyalty Service | points ledger |
| Notification Service | notification status |

Rule:

```text
Only the owning service writes its database.
```

Strong answer:

```text
Database per service protects autonomy. Other services cannot directly write or join against
another service's tables; they must use APIs, events, or replicated read models.
```

---

# 3. Shared Database Anti-Pattern

Shared database:

```text
Booking Service -> shared tables
Payment Service -> shared tables
Loyalty Service -> shared tables
```

Problems:
- tight coupling
- schema changes break other services
- hidden dependencies
- no clear ownership
- deployment coupling
- difficult scaling
- hard security boundaries

Temporary exception:

```text
During migration, shared DB may exist briefly behind an anti-corruption layer or strangler
plan, but it should not be the target architecture.
```

Strong answer:

```text
If many services share and write the same database, we probably have a distributed monolith.
The real boundary is data ownership, not just code deployment.
```

---

# 4. Local Transaction Boundary

Inside one service:

```text
Booking Service transaction:
  insert booking
  insert booking status history
  insert outbox event
commit
```

This is strong consistency within one database.

Across services:

```text
Booking DB + Payment DB + Inventory DB
```

No single simple local transaction covers all three.

Senior line:

```text
I keep strongly consistent invariants inside one service boundary whenever possible.
```

---

# 5. Distributed Transaction Problem

Problem:

```text
Reserve room in Availability Service.
Authorize payment in Payment Service.
Confirm booking in Booking Service.
```

Failures:
- inventory reserved, payment fails
- payment succeeds, booking update fails
- network timeout but downstream completed
- retry duplicates side effect
- compensation fails

Strong answer:

```text
Distributed workflows need explicit state machines, idempotency, retries, and compensation.
I avoid pretending a remote call behaves like a local database transaction.
```

---

# 6. Two-Phase Commit

Two-phase commit coordinates multiple resources.

Phases:
1. prepare
2. commit/rollback

Why avoided in many microservice systems:
- blocking
- coordinator dependency
- reduced availability
- operational complexity
- tight resource coupling
- poor fit for independent services

Interview answer:

```text
Two-phase commit gives stronger atomicity, but it hurts availability and autonomy. In most
microservices, Saga with compensation is preferred unless the domain truly requires atomic
distributed commit.
```

---

# 7. Saga Pattern

Saga coordinates a distributed workflow using local transactions and compensating actions.

Booking saga:

```text
1. Create pending booking
2. Reserve inventory
3. Authorize payment
4. Confirm booking
5. Publish BookingConfirmed
```

If payment fails:

```text
release inventory
mark booking failed
```

Strong answer:

```text
Saga is the standard answer for distributed business workflows. It accepts that each step
commits locally and uses compensation to handle later failures.
```

---

# 8. Choreography vs Orchestration

| Choreography | Orchestration |
|---|---|
| services react to events | central orchestrator commands steps |
| less central control | easier workflow visibility |
| can become event spaghetti | orchestrator can become bottleneck |
| good for simple flows | good for complex workflows |

Strong answer:

```text
For simple flows, choreography can work. For complex booking/payment/inventory workflows,
orchestration is often easier to debug and reason about.
```

---

# 9. Compensation Failure

Senior interview trap:

```text
What if compensation also fails?
```

Example:

```text
Payment failed, but release inventory call fails.
```

Controls:
- retry compensation with backoff
- idempotent compensation command
- store saga state
- alert operations
- reconciliation job
- manual repair workflow
- timeout pending reservations

Strong answer:

```text
Compensation is not magic. It also needs retry, idempotency, observability, and sometimes
manual or scheduled reconciliation.
```

---

# 10. Transactional Outbox

Problem:

```text
Save booking, then publish Kafka event.
```

Failure:
- DB commit succeeds but event publish fails
- event publish succeeds but DB rolls back

Outbox solution:

```text
same transaction:
  save booking
  save outbox event

separate publisher:
  reads outbox
  publishes event
  marks published
```

Strong answer:

```text
Outbox solves the dual-write problem by storing the event in the same database transaction
as the business change. A separate publisher sends it later.
```

---

# 11. CDC

CDC means Change Data Capture.

CDC reads database changes and publishes them.

Common use:

```text
Outbox table -> CDC connector -> Kafka topic
```

Benefits:
- avoids application poller
- preserves commit order per database log constraints
- reliable integration with event stream

Costs:
- connector operations
- schema management
- replay planning
- database log dependency

Interview line:

```text
Outbox defines what to publish. CDC is one way to publish it reliably from the database log.
```

---

# 12. Idempotency

Idempotency means same request/event can be processed multiple times without duplicate side effects.

Patterns:
- idempotency key table
- unique business constraints
- processed event table
- status transition checks
- natural unique keys

Example:

```sql
create table idempotency_keys (
    key varchar(100) primary key,
    request_hash varchar(200) not null,
    response_body text,
    status varchar(30) not null,
    created_at timestamp not null
);
```

Strong answer:

```text
Idempotency is required because clients retry, messages redeliver, and networks timeout.
I enforce it with durable keys or constraints, not only in memory.
```

---

# 13. Cross-Service Joins

Problem:

```sql
select *
from bookings b
join payments p on b.payment_id = p.id;
```

This breaks database-per-service if tables belong to different services.

Alternatives:
- API composition
- materialized read model
- CQRS projection
- data warehouse/lake for analytics
- search index
- replicated read-only data

Strong answer:

```text
For operational queries, I use APIs or read models. For analytics, I use a warehouse or
streaming pipeline. I do not join directly across service-owned databases.
```

---

# 14. CQRS And Read Models

CQRS separates writes from reads.

Write model:

```text
Booking Service owns booking command model.
```

Read model:

```text
BookingSummaryView combines booking, hotel, payment, and loyalty fields.
```

Use when:
- reads need data from many services
- query traffic is high
- eventual consistency is acceptable
- API composition is too slow

Avoid when:
- simple CRUD
- strict real-time consistency required
- team cannot operate projections

---

# 15. Eventual Consistency UX

Users need understandable states.

Bad UX:

```text
Click book -> spinner forever -> maybe booked
```

Better UX:

```text
Booking pending
Payment processing
Booking confirmed
Booking failed, inventory released
```

Patterns:
- explicit state machine
- pending status
- progress page
- notifications
- retry-safe refresh
- reconciliation status

Strong answer:

```text
Eventual consistency must be visible in product states. I design statuses like pending,
confirmed, failed, and compensating instead of pretending everything is instant.
```

---

# 16. Read-Your-Writes

Read-your-writes means user sees their own recent change.

Problem:

```text
User creates booking.
Read model updates asynchronously.
User refreshes and booking is missing.
```

Solutions:
- read from write service for owner immediately
- sticky session to region/leader, in some architectures
- return created object from command response
- client-side optimistic state
- wait for projection for critical read
- show pending status

Strong answer:

```text
For user-created data, I often read from the write service immediately or return the created
state, while async read models catch up for broader queries.
```

---

# 17. Reporting And Analytics

Do not overload service databases with global reporting queries.

Better:
- event stream to warehouse
- CDC to lake
- ETL/ELT pipeline
- materialized reporting store
- search/index store

Example:

```text
Booking events + payment events + loyalty events -> data warehouse -> dashboards
```

Strong answer:

```text
For cross-service analytics, I use a warehouse or reporting pipeline. Service databases
remain optimized for operational ownership.
```

---

# 18. Schema Evolution

Rules:
- additive changes first
- keep old fields until consumers migrate
- backfill large data safely
- do not change meaning silently
- use expand-contract
- version events and APIs when needed

Expand-contract:

```text
add new field -> write both -> migrate reads -> backfill -> remove old field later
```

Strong answer:

```text
Schema evolution must support rolling deploys. I avoid breaking old code while new and old
versions run together.
```

---

# 19. Data Consistency Decision Table

| Requirement | Design |
|---|---|
| Must be instantly correct | keep inside one service/database |
| Cross-service workflow | Saga |
| DB update must publish event | Outbox |
| Duplicate requests/events | Idempotency |
| Cross-service query | Read model/API composition |
| Analytics | Warehouse/lake |
| User must see own write | read from write model or pending state |
| High-volume projection | CDC/event stream |

---

# 20. Production Scenario: Prevent Double Booking

Requirement:

```text
Two users should not book the same room for overlapping dates.
```

Design:
1. Availability Service owns inventory.
2. Use database constraint or lock to protect overlap invariant.
3. Booking Service creates pending booking.
4. Saga requests inventory reservation.
5. Payment is authorized after reservation.
6. If payment fails, release reservation.
7. Reservation has TTL for abandoned flows.
8. Idempotency key protects client retry.
9. Reconciliation job fixes stuck pending reservations.

Strong answer:

```text
The double-booking invariant must live where inventory is owned, usually Availability
Service. I protect it with database constraints or locking, then coordinate booking/payment
through a Saga. Retries use idempotency keys, and stuck reservations are cleaned by TTL or
reconciliation.
```

---

# 21. Hot Interview Questions

### Q1. Why database per service?

```text
It gives each service ownership, independent schema evolution, and deployment autonomy.
```

### Q2. How do you join data across services?

```text
Use API composition, read models, search indexes, or analytics pipelines. Do not directly
join service-owned databases.
```

### Q3. Saga vs 2PC?

```text
2PC gives atomic commit but hurts availability and autonomy. Saga uses local transactions
and compensation, which fits most microservice workflows.
```

### Q4. What does Outbox solve?

```text
It solves the dual-write problem between database commit and event publish.
```

### Q5. What if compensation fails?

```text
Retry it idempotently, track saga state, alert operations, and use reconciliation/manual
repair if needed.
```

---

# 22. Common Mistakes

| Mistake | Why Wrong | Better |
|---|---|---|
| Shared DB forever | distributed monolith | database per service |
| Remote calls inside DB transaction | long locks and false atomicity | local transaction + saga |
| No idempotency | duplicate side effects | durable keys/constraints |
| Assume event publish is atomic with DB | lost event | outbox/CDC |
| Hide eventual consistency | confused users | explicit states |
| Direct reporting from service DBs | load/coupling | warehouse/read model |
| Make every workflow async | poor UX for required answer | choose based on requirement |
| No compensation monitoring | stuck business state | saga state and alerts |

---

# 23. Final Rapid Revision

| Interviewer Says | Think |
|---|---|
| Each service owns data | database per service |
| Shared tables | anti-pattern |
| Cross-service transaction | Saga |
| Publish event with DB update | Outbox |
| Outbox publishing at scale | CDC |
| Duplicate message | idempotent consumer |
| Search across services | read model |
| User cannot see new booking | read-your-writes |
| Payment failed after inventory reserve | compensation |
| Global reports | data warehouse |

---

# 24. Strong Closing Answer

If interviewer asks:

```text
How do you handle data consistency in microservices?
```

Say:

```text
I first identify which invariants require strong consistency and keep those inside one
service/database when possible. For cross-service workflows, I use Saga with explicit states
and compensation. For reliable events, I use Outbox, often with CDC. Since retries and
messages can duplicate, I design idempotency using durable keys or constraints. For queries
across services, I use API composition or read models, and for analytics I use a warehouse.
```

---

# 25. Official Source Notes

Useful references:

- Database per Service: https://microservices.io/patterns/data/database-per-service.html
- Saga Pattern: https://microservices.io/patterns/data/saga.html
- Transactional Outbox: https://microservices.io/patterns/data/transactional-outbox.html
- CQRS: https://microservices.io/patterns/data/cqrs.html
- Change Data Capture: https://microservices.io/patterns/data/transaction-log-tailing.html

