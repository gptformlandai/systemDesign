# Microservices Design Mini Labs

> Track: Microservices Interview Track - Group 5 Practice Upgrade  
> Goal: turn concepts into diagrams, tables, pseudocode, and operational checklists.

These are not long projects. Each lab should take 30-90 minutes.

---

## 1. Lab Rules

For every lab, produce:

1. A small diagram or flow.
2. Tables or API/event contracts where needed.
3. Failure handling notes.
4. Observability metrics.
5. A 60-second interview explanation.

Optional implementation language:

- Java/Spring Boot pseudocode
- SQL DDL
- Kafka topic/event examples
- Kubernetes YAML snippets if useful

---

## 2. Lab 1: Service Boundary Review

Task:

```text
Given a hotel booking platform, propose service boundaries and reject bad splits.
```

Deliverables:

- service list
- owned data per service
- invariants per service
- sync/async dependency list
- boundary smell table

Minimum answer:

| Service | Owns | Key Invariant |
|---|---|---|
| Booking | booking lifecycle | valid state transitions |
| Availability | room/date inventory | no oversell |
| Payment | payment audit | no duplicate charge |
| Notification | delivery status | no unbounded retries |
| Loyalty | points ledger | no duplicate award |

Stretch:

```text
Explain why RoomService as a CRUD service is usually a weak split.
```

---

## 3. Lab 2: Booking Idempotency Table

Task:

```text
Design idempotency for CreateBooking.
```

Deliverables:

- SQL table or unique constraint
- request flow
- duplicate handling behavior
- race-condition explanation

Suggested schema:

```sql
CREATE TABLE booking_request (
  user_id           VARCHAR(64) NOT NULL,
  idempotency_key   VARCHAR(128) NOT NULL,
  booking_id        VARCHAR(64) NOT NULL,
  request_hash      VARCHAR(128) NOT NULL,
  response_json     TEXT NOT NULL,
  created_at        TIMESTAMP NOT NULL,
  PRIMARY KEY (user_id, idempotency_key)
);
```

Questions:

1. What if same key is reused with different payload?
2. What if two requests arrive at exactly the same time?
3. How long do you retain idempotency records?
4. What response should duplicate request return?

---

## 4. Lab 3: Transactional Outbox

Task:

```text
Implement the design for reliable BookingConfirmed publication.
```

Deliverables:

- outbox table
- transaction flow
- relay pseudocode
- duplicate publish handling
- monitoring metrics

Required metrics:

- pending outbox rows
- oldest pending event age
- publish success/failure count
- retry count
- relay processing latency

Failure drills:

1. DB commit succeeds but Kafka publish fails.
2. Kafka publish succeeds but relay crashes before marking published.
3. Event payload is invalid for schema registry.
4. Broker is down for 10 minutes.

---

## 5. Lab 4: Idempotent Consumer

Task:

```text
Design Loyalty Service consuming BookingConfirmed without awarding points twice.
```

Deliverables:

- processed_event table
- transaction sequence
- duplicate event behavior
- ledger entry model

Suggested sequence:

```text
1. Receive event.
2. Insert processed_event row.
3. If duplicate, exit.
4. Insert loyalty ledger entry.
5. Commit.
6. Commit Kafka offset.
```

Stretch:

```text
Explain why committing offset before business transaction is dangerous.
```

---

## 6. Lab 5: Saga State Machine

Task:

```text
Design booking saga state transitions.
```

Deliverables:

- state diagram
- saga table
- compensation table
- retry policy
- timeout/reconciliation path

Minimum states:

```text
PENDING
INVENTORY_RESERVED
PAYMENT_AUTHORIZED
CONFIRMED
FAILED
PAYMENT_UNKNOWN
COMPENSATION_REQUIRED
```

Questions:

1. What happens if payment times out?
2. What happens if inventory release fails?
3. Which steps are idempotent?
4. What gets alerted?

---

## 7. Lab 6: API Contract Compatibility

Task:

```text
Add a new booking response field without breaking consumers.
```

Deliverables:

- old JSON example
- new JSON example
- compatibility assessment
- consumer-driven contract test idea
- rollout plan

Example old response:

```json
{
  "bookingId": "B1",
  "bookingStatus": "CONFIRMED"
}
```

Safe new response:

```json
{
  "bookingId": "B1",
  "bookingStatus": "CONFIRMED",
  "status": "CONFIRMED"
}
```

Question:

```text
When can `bookingStatus` be removed?
```

---

## 8. Lab 7: Event Schema Evolution

Task:

```text
Evolve BookingConfirmed event to include loyalty eligibility.
```

Deliverables:

- old schema
- new schema
- compatibility rule
- consumer migration plan
- replay behavior

Safe approach:

```text
Add optional field `loyaltyEligible` with default behavior for old consumers.
```

Questions:

1. Is this a semantic change?
2. What if consumers assume all confirmed bookings earn points?
3. How do you validate event compatibility in CI?

---

## 9. Lab 8: Consumer Lag Debug Dashboard

Task:

```text
Create a dashboard for Notification Service Kafka consumer health.
```

Panels:

- consumer lag by partition
- oldest unprocessed event age
- processing rate
- handler error rate
- DLQ count
- retry topic volume
- downstream email provider latency/error
- rebalance count

Write a runbook:

```text
If lag rises, check producer rate, consumer errors, downstream latency, hot partition,
rebalance count, and DLQ volume.
```

---

## 10. Lab 9: SLO For Booking Checkout

Task:

```text
Define SLI/SLO/error budget for CreateBooking.
```

Deliverables:

- SLI definition
- SLO target
- excluded errors
- burn-rate alert idea
- dashboard panels

Example:

```text
SLI: percent of valid CreateBooking requests that complete with CONFIRMED or clear FAILED
state within 1 second.
SLO: 99.5 percent over rolling 28 days.
```

Questions:

1. Do client validation errors count against SLO?
2. Does PAYMENT_UNKNOWN count as success?
3. What is the alert threshold for fast burn?

---

## 11. Lab 10: Gateway Bypass Security

Task:

```text
Design service-level authorization for CancelBooking.
```

Deliverables:

- token claims needed
- service identity check
- domain authorization rule
- audit event fields
- negative test cases

Rules:

```text
A normal user can cancel only their own booking if state allows cancellation.
A hotel admin can cancel bookings for hotels they manage.
A support admin needs reason code and audit trail.
```

---

## 12. Lab 11: Secret Rotation Runbook

Task:

```text
Write a runbook for rotating Payment provider credentials safely.
```

Deliverables:

- pre-rotation checklist
- canary plan
- rollback plan
- validation tests
- alerts

Must include:

- dual credential overlap
- avoid logging secret
- verify pods loaded new secret
- synthetic payment auth test
- rollback to previous credential if safe

---

## 13. Lab 12: Kubernetes Probe Design

Task:

```text
Design startup, readiness, and liveness probes for Booking Service.
```

Deliverables:

- endpoint behavior
- what each probe checks
- what each probe avoids checking
- failure scenario explanation

Expected:

```text
Startup: app boot complete.
Readiness: can accept traffic, required dependencies/config ready.
Liveness: app process not stuck.
```

Trap to avoid:

```text
Liveness fails because Payment Service is down.
```

---

## 14. Lab 13: Canary Rollout Plan

Task:

```text
Create a canary plan for a new Booking Service version.
```

Deliverables:

- traffic percentages
- version metrics
- abort criteria
- rollback command concept
- validation checklist

Metrics:

- error rate by version
- p95/p99 by version
- booking success rate
- dependency error rate
- CPU/memory/throttling
- logs by version

Abort example:

```text
Abort if canary 5xx rate is 2x baseline for 5 minutes or checkout SLO burn is fast.
```

---

## 15. Lab 14: Monolith Extraction Plan

Task:

```text
Extract Payment from a booking monolith.
```

Deliverables:

- current-state diagram
- target-state diagram
- anti-corruption layer
- data migration plan
- reconciliation report
- canary plan
- rollback plan

Must answer:

1. How do old and new payment paths coexist?
2. How do you avoid duplicate charges?
3. How do you compare audit records?
4. When is old code removed?

---

## 16. Lab 15: Architecture Review Scorecard

Task:

```text
Score a proposed microservice design from 1 to 5 across the platinum rubric.
```

Rubric areas:

- boundaries
- data ownership
- communication
- consistency
- failure handling
- observability
- security
- testing
- deployment
- ownership

Deliverable:

```text
A one-page architecture review with top 3 risks and top 3 changes.
```

---

## 17. Completion Criteria

You completed this mini-lab set when you can:

1. Draw all core flows without notes.
2. Write basic SQL tables for outbox/idempotency/processed events.
3. Explain failure cases for saga and outbox.
4. Create a dashboard/runbook for lag and checkout latency.
5. Define SLOs and canary abort criteria.
6. Review a design using the platinum rubric.
