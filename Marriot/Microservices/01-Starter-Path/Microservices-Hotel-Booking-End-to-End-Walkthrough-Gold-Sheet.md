# Microservices Hotel Booking End To End Walkthrough Gold Sheet

> Track: Microservices Interview Track - Group 1 Starter Path  
> Goal: connect every microservice pattern to one concrete hotel booking flow.

Use this after the distributed systems foundations sheet and before the individual pattern
sheets.

---

## 1. Capstone System

System:

```text
Hotel Booking Platform
```

Primary user flow:

```text
Search hotel -> check availability -> create pending booking -> reserve inventory
-> authorize payment -> confirm booking -> publish event -> notify guest -> award points
```

Core services:

| Service | Owns |
|---|---|
| Search Service | hotel search index and filters |
| Availability Service | room/date inventory and reservation holds |
| Booking Service | booking lifecycle and user-visible booking state |
| Payment Service | authorization, capture, refunds, payment audit |
| Notification Service | email/SMS/push delivery status |
| Loyalty Service | points ledger and rewards rules |
| Pricing Service | pricing rules, discounts, taxes, fees |
| Identity Service | users, auth, tokens, roles |
| Reporting Service | analytics/read-only aggregates |

---

## 2. Request Flow At A Glance

```text
Client
  -> API Gateway
  -> Booking Service
      -> Availability Service
      -> Payment Service
      -> Booking DB
      -> Outbox table
  -> Kafka booking-events
      -> Notification Service
      -> Loyalty Service
      -> Reporting Service
```

Design principle:

```text
Only user-critical decisions stay synchronous. Non-critical side effects move to events.
```

---

## 3. Search Flow

Search is read-heavy.

```text
Client -> Gateway -> Search Service -> Search index/read model -> response
```

Good design:

- use a denormalized search index
- update search index from hotel, pricing, and availability events
- tolerate slightly stale search results
- do final availability check during booking

Why:

```text
Search needs high availability and fast reads. Exact room availability is validated later
by Availability Service before creating a booking.
```

Strong answer:

```text
I do not make the search page strongly consistent with every inventory update. I use a read
model for speed and perform the correctness check when the guest attempts to reserve.
```

---

## 4. Availability Check Flow

Availability is correctness-critical.

```text
Booking Service -> Availability Service -> Availability DB
```

Availability owns:

- room inventory
- date-level capacity
- holds/reservations
- rules that prevent double booking

Example local transaction:

```text
1. Find room/date inventory row.
2. Acquire row-level lock or optimistic version check.
3. Verify remaining capacity.
4. Create reservation hold.
5. Commit.
```

Strong answer:

```text
The invariant "do not oversell rooms" belongs inside Availability Service. Other services
can request a reservation, but they should not directly update inventory tables.
```

---

## 5. Pending Booking Flow

Booking Service creates the user-visible booking lifecycle.

States:

```text
PENDING -> CONFIRMED -> CANCELLED
PENDING -> FAILED
CONFIRMED -> CANCEL_REQUESTED -> CANCELLED
```

Initial transaction:

```text
Booking Service local transaction:
  insert booking status=PENDING
  insert outbox event BookingCreated
  commit
```

Why pending matters:

```text
A cross-service workflow cannot be one simple local transaction. PENDING gives the workflow
a durable state while inventory and payment complete.
```

---

## 6. Payment Authorization Flow

Payment Service owns payment correctness and audit.

```text
Booking Service -> Payment Service -> Payment provider
```

Payment rules:

- use idempotency key for authorization request
- store provider request/response audit
- do not double-charge on retry
- separate authorization from capture when business flow needs it
- model refund as its own auditable action

Strong answer:

```text
Payment calls need idempotency because client retries, gateway retries, or service retries
can repeat the same operation. I store the idempotency key and previous result so duplicate
requests return the same outcome instead of charging twice.
```

---

## 7. Saga For Booking Confirmation

A booking crosses multiple services, so use a saga.

Orchestration option:

```text
Booking Orchestrator:
  1. create pending booking
  2. reserve inventory
  3. authorize payment
  4. confirm booking
  5. publish BookingConfirmed
```

Compensation:

| Failed Step | Compensation |
|---|---|
| inventory reserve failed | mark booking failed |
| payment auth failed | release inventory hold, mark failed |
| confirmation failed | retry safely using idempotency |
| notification failed | retry async, DLQ if poison |

Strong answer:

```text
I use saga because booking spans independent data owners. Each service commits locally, and
failures are handled by compensating actions and durable workflow state.
```

---

## 8. Outbox For Reliable Events

Problem:

```text
Booking DB commit succeeds, but publishing BookingConfirmed to Kafka fails.
```

Outbox solution:

```text
Same local transaction:
  update booking status=CONFIRMED
  insert outbox event BookingConfirmed
  commit

Separate relay:
  read unpublished outbox rows
  publish to Kafka
  mark published
```

Why it works:

```text
The database update and event record commit atomically. Event publishing can be retried
without losing the fact that the event should exist.
```

---

## 9. Idempotency Across The Flow

Idempotency prevents duplicate side effects.

| Operation | Idempotency Key |
|---|---|
| create booking | client request id |
| reserve inventory | booking id + room/date |
| authorize payment | booking id + payment attempt |
| consume BookingConfirmed | event id |
| send notification | booking id + channel + template |

Strong answer:

```text
Retries are safe only when commands and consumers are idempotent. I store request keys or
processed event IDs so repeating work does not duplicate bookings, charges, or emails.
```

---

## 10. Notification And Loyalty As Async Side Effects

Not everything should block checkout.

After confirmation:

```text
BookingConfirmed event
  -> Notification Service sends email
  -> Loyalty Service awards points
  -> Reporting Service updates read model
```

If Notification fails:

- retry with backoff
- send to DLQ after bounded attempts
- alert if DLQ grows
- allow support/manual replay

User experience:

```text
Booking confirmed. Email may arrive later.
```

---

## 11. Read Models And Reporting

Reporting should not join live service databases.

Better options:

- consume events into analytics store
- use CDC into warehouse
- create query-specific read models
- expose APIs for small operational lookups

Strong answer:

```text
For cross-service reporting, I avoid direct joins against production service databases. I
use replicated read models or analytics pipelines so reporting does not break service data
ownership.
```

---

## 12. Failure Scenario: Payment Timeout

Scenario:

```text
Payment authorization times out after 2 seconds.
```

Do not blindly retry forever.

Good handling:

1. Check if timeout is safe to retry with same idempotency key.
2. Keep booking in PENDING_PAYMENT or PAYMENT_UNKNOWN.
3. Reconcile with payment provider if result is unknown.
4. Release inventory if payment definitively failed.
5. Alert if unknown state exceeds threshold.

Strong answer:

```text
A timeout does not mean failure. For payment, I treat it as unknown until reconciliation or
idempotent retry confirms the final state.
```

---

## 13. Failure Scenario: Duplicate Booking Request

Scenario:

```text
Client submits booking, network fails before response, client retries.
```

Controls:

- client-generated idempotency key
- unique constraint on idempotency key per user/business action
- return existing booking result for duplicate request
- trace both attempts under same correlation id if possible

Strong answer:

```text
Duplicate requests are normal in distributed systems. The create booking command should be
idempotent so retrying returns the same booking instead of creating a second booking.
```

---

## 14. Failure Scenario: Consumer Lag

Scenario:

```text
Notification Service lag grows for booking-events.
```

Debug:

1. Compare producer rate and consumer processing rate.
2. Check error rate and DLQ volume.
3. Check downstream email provider latency.
4. Check hot partitions and key distribution.
5. Check rebalance frequency.
6. Scale consumers only if partitions and downstream capacity allow it.

Strong answer:

```text
Consumer lag is not solved by scaling blindly. I check whether the bottleneck is partition
parallelism, handler speed, downstream dependency, poison messages, or rebalancing.
```

---

## 15. Observability For The Flow

Every request needs:

- correlation id
- trace id
- span per downstream call
- structured logs
- metrics for latency, errors, saturation
- dashboard for booking funnel states
- DLQ and outbox lag alerts

Minimum dashboard:

| Signal | Metric |
|---|---|
| user path | booking create rate, confirm rate, fail rate |
| latency | p50/p95/p99 by endpoint |
| dependencies | payment and availability latency/error |
| async health | outbox lag, consumer lag, DLQ count |
| correctness | duplicate payment attempts, compensation failures |

---

## 16. Security For The Flow

Security model:

```text
Client token -> Gateway validates -> Services authorize actions -> service-to-service identity
```

Important controls:

- user authentication at edge
- service-level authorization inside Booking/Payment
- do not trust only gateway headers
- protect payment data and PII
- mTLS or service identity for internal calls
- secrets in vault/secret manager
- audit logs for payment and booking state changes

Strong answer:

```text
The gateway is not the only security boundary. Each service must authorize sensitive actions
and protect service-to-service traffic and secrets.
```

---

## 17. Interview Whiteboard Flow

Draw this in order:

```text
Client -> Gateway -> Booking Service
                  -> Availability Service -> Availability DB
                  -> Payment Service -> Provider
                  -> Booking DB + Outbox
                  -> Kafka
                      -> Notification
                      -> Loyalty
                      -> Reporting Read Model
```

Then say:

```text
Availability protects inventory. Booking owns lifecycle. Payment owns payment audit. Outbox
protects event publishing. Idempotency protects retries. Events handle non-critical side
effects. Traces and metrics make the flow debuggable.
```

---

## 18. Active Recall Check

1. Which service owns the double-booking invariant?
2. Why does Search use a read model?
3. Why is Booking initially PENDING?
4. What does Outbox protect against?
5. Where do idempotency keys appear in the flow?
6. Why should notification be async?
7. What does payment timeout mean: failure or unknown?
8. What metrics show a broken booking flow?
9. Why should reporting not join service databases?
10. Which parts require audit logging?

---

## 19. Strong Closing Answer

```text
In the hotel booking flow, I keep correctness-critical decisions with the owning services:
Availability owns inventory, Payment owns payment audit, and Booking owns lifecycle. The
cross-service workflow uses saga, outbox, idempotency, bounded retries, DLQ, traces, metrics,
and service-level security. This gives independent ownership while protecting the business
from duplicate bookings, lost events, and unobservable failures.
```
