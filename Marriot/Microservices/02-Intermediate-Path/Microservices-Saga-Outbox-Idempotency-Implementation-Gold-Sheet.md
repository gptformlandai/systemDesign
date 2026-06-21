# Microservices Saga Outbox Idempotency Implementation Gold Sheet

> Track: Microservices Interview Track - Group 2 Intermediate Path  
> Goal: move from naming patterns to implementing reliable cross-service workflows.

Read after the data consistency and event-driven messaging sheets.

---

## 1. Why This Sheet Exists

Many candidates can say "use saga, outbox, and idempotency." Senior interviewers ask the
next question:

```text
What tables exist? What is retried? What is idempotent? What happens if the process crashes
between DB commit and event publish? What if compensation fails?
```

Strong answer:

```text
Saga gives workflow structure, outbox protects event publishing, and idempotency makes
retries safe. They usually work together, not as isolated patterns.
```

---

## 2. The Reliability Trio

| Pattern | Solves | Does Not Solve Alone |
|---|---|---|
| Saga | cross-service workflow coordination | lost event publishing |
| Outbox | atomic local state change + event record | duplicate consumption |
| Idempotency | safe retry/duplicate handling | workflow decision logic |

Memory line:

```text
Saga decides the journey. Outbox makes facts durable. Idempotency makes repetition safe.
```

---

## 3. Booking Workflow Example

```text
CreateBooking command
  -> Booking Service creates PENDING booking
  -> Availability reserves inventory
  -> Payment authorizes payment
  -> Booking confirms booking
  -> Notification and Loyalty react async
```

State machine:

```text
PENDING
  -> INVENTORY_RESERVED
  -> PAYMENT_AUTHORIZED
  -> CONFIRMED

PENDING
  -> INVENTORY_FAILED
  -> FAILED

INVENTORY_RESERVED
  -> PAYMENT_FAILED
  -> INVENTORY_RELEASED
  -> FAILED
```

Interview point:

```text
Durable state matters. A saga that only lives in memory cannot recover cleanly after a crash.
```

---

## 4. Minimal Tables

Booking table:

```sql
CREATE TABLE booking (
  booking_id        VARCHAR(64) PRIMARY KEY,
  user_id           VARCHAR(64) NOT NULL,
  hotel_id          VARCHAR(64) NOT NULL,
  room_type_id      VARCHAR(64) NOT NULL,
  check_in_date     DATE NOT NULL,
  check_out_date    DATE NOT NULL,
  status            VARCHAR(40) NOT NULL,
  idempotency_key   VARCHAR(128) NOT NULL,
  version           BIGINT NOT NULL,
  created_at        TIMESTAMP NOT NULL,
  updated_at        TIMESTAMP NOT NULL,
  UNIQUE (user_id, idempotency_key)
);
```

Outbox table:

```sql
CREATE TABLE outbox_event (
  event_id          VARCHAR(64) PRIMARY KEY,
  aggregate_type    VARCHAR(80) NOT NULL,
  aggregate_id      VARCHAR(64) NOT NULL,
  event_type        VARCHAR(120) NOT NULL,
  payload_json      TEXT NOT NULL,
  status            VARCHAR(40) NOT NULL,
  retry_count       INT NOT NULL DEFAULT 0,
  next_attempt_at   TIMESTAMP NOT NULL,
  created_at        TIMESTAMP NOT NULL,
  published_at      TIMESTAMP NULL
);
```

Processed event table:

```sql
CREATE TABLE processed_event (
  consumer_name     VARCHAR(120) NOT NULL,
  event_id          VARCHAR(64) NOT NULL,
  processed_at      TIMESTAMP NOT NULL,
  PRIMARY KEY (consumer_name, event_id)
);
```

---

## 5. Create Booking With Idempotency

Flow:

```text
1. Client sends idempotency key.
2. Booking Service checks user_id + idempotency_key.
3. If existing booking exists, return existing result.
4. Otherwise create PENDING booking and outbox event in one transaction.
```

Pseudocode:

```java
@Transactional
CreateBookingResponse createBooking(CreateBookingCommand command) {
    Optional<Booking> existing = bookingRepository.findByUserIdAndIdempotencyKey(
        command.userId(), command.idempotencyKey()
    );

    if (existing.isPresent()) {
        return responseFrom(existing.get());
    }

    Booking booking = Booking.pending(command);
    bookingRepository.save(booking);

    outboxRepository.save(OutboxEvent.of(
        booking.id(),
        "BookingCreated",
        bookingCreatedPayload(booking)
    ));

    return responseFrom(booking);
}
```

Trap:

```text
Checking idempotency without a unique constraint can still create duplicates under race.
```

---

## 6. Transactional Outbox Relay

Relay loop:

```text
1. Poll unpublished events where next_attempt_at <= now.
2. Publish event to broker.
3. Mark event PUBLISHED.
4. On publish failure, increment retry_count and schedule next attempt.
```

Pseudocode:

```java
void publishBatch() {
    List<OutboxEvent> events = outboxRepository.lockNextBatch(100);

    for (OutboxEvent event : events) {
        try {
            kafkaTemplate.send(event.topic(), event.key(), event.payload()).get();
            outboxRepository.markPublished(event.id());
        } catch (Exception ex) {
            outboxRepository.markRetry(event.id(), nextBackoff(event.retryCount()));
        }
    }
}
```

Important nuance:

```text
The relay may publish an event and crash before marking it published. Consumers must still
be idempotent because duplicate publish is possible.
```

---

## 7. Consumer Idempotency

Consumer flow:

```text
1. Receive event.
2. Start local transaction.
3. Insert consumer_name + event_id into processed_event.
4. If insert fails due to duplicate key, skip processing.
5. Apply business change.
6. Commit.
7. Commit broker offset after successful processing.
```

Pseudocode:

```java
@Transactional
void handleBookingConfirmed(Event event) {
    boolean firstTime = processedEventRepository.tryInsert("loyalty-service", event.id());
    if (!firstTime) {
        return;
    }

    LoyaltyAward award = LoyaltyAward.from(event);
    loyaltyRepository.save(award);
}
```

Strong answer:

```text
At-least-once delivery is normal, so the consumer records processed event IDs in the same
local transaction as the business side effect.
```

---

## 8. Saga Orchestration Implementation

Orchestrator owns workflow state.

Saga state table:

```sql
CREATE TABLE booking_saga (
  saga_id           VARCHAR(64) PRIMARY KEY,
  booking_id        VARCHAR(64) NOT NULL,
  status            VARCHAR(60) NOT NULL,
  current_step      VARCHAR(80) NOT NULL,
  retry_count       INT NOT NULL DEFAULT 0,
  last_error        TEXT NULL,
  created_at        TIMESTAMP NOT NULL,
  updated_at        TIMESTAMP NOT NULL
);
```

Flow:

```text
BookingSagaOrchestrator
  -> reserve inventory
  -> authorize payment
  -> confirm booking
  -> emit event
```

Good when:

- workflow is business-critical
- compensation is complex
- teams want one place to inspect workflow state
- failure recovery needs clear control

Trade-off:

```text
The orchestrator can become a central dependency if it starts owning too much domain logic.
```

---

## 9. Saga Choreography Implementation

Choreography uses events and local reactions.

```text
BookingCreated
  -> Availability Service reserves inventory
  -> emits InventoryReserved
  -> Payment Service authorizes payment
  -> emits PaymentAuthorized
  -> Booking Service confirms booking
```

Good when:

- workflow is simple
- services can react independently
- no single coordinator is needed

Risk:

```text
The workflow can become hard to understand because control flow is spread across events.
```

Interview answer:

```text
I prefer orchestration for complex booking/payment workflows where status and compensation
must be visible. I use choreography for simpler fan-out side effects.
```

---

## 10. Compensation Design

Compensation is not rollback.

Rollback:

```text
Undo uncommitted work inside one transaction.
```

Compensation:

```text
Commit a new action that semantically reverses or neutralizes a previous committed action.
```

Examples:

| Step | Compensation |
|---|---|
| inventory reserved | release inventory hold |
| payment authorized | void authorization |
| payment captured | refund payment |
| points awarded | debit points ledger |

Senior point:

```text
Compensation can fail. It needs its own retry policy, alerting, and manual recovery path.
```

---

## 11. Retry Policy Matrix

| Operation | Retry? | Notes |
|---|---|---|
| GET availability | yes | bounded timeout and retry if safe |
| reserve inventory | yes with idempotency | same booking id/request id |
| authorize payment | yes with idempotency | timeout may mean unknown |
| send email | yes async | DLQ after bounded attempts |
| capture payment | careful | reconcile with provider |
| non-idempotent external call | no blind retry | add key or manual handling |

Rule:

```text
Retry only when the operation is safe, bounded, observable, and has idempotency controls.
```

---

## 12. DLQ And Replay

DLQ is not a trash can.

DLQ entry should include:

- original event payload
- event id
- topic and partition
- consumer group
- failure reason
- retry count
- first failed time
- last failed time
- correlation id

Replay checklist:

1. Fix the root cause.
2. Confirm replay is idempotent.
3. Replay in bounded batches.
4. Watch error rate, lag, and downstream saturation.
5. Record audit trail.

Strong answer:

```text
DLQ is an operational safety valve. I use it with dashboards, alerts, root-cause fixes, and
controlled replay, not as silent data loss.
```

---

## 13. Workflow Engine Option

Tools such as Temporal or Camunda can manage long-running workflows.

Use when:

- workflow has many steps
- retries and timers are complex
- human/manual steps exist
- workflow state must survive crashes
- observability of workflow progress matters

Avoid when:

- workflow is simple
- team cannot operate the tool
- it becomes a dumping ground for business logic

Interview answer:

```text
For a complex booking/payment workflow, a workflow engine can make retries, timers, and
state recovery explicit. I would still keep domain ownership inside services and avoid
turning the workflow engine into a shared business monolith.
```

---

## 14. Failure Walkthroughs

### Crash After DB Commit Before Publish

Protection:

```text
Outbox row exists. Relay publishes later.
```

### Crash After Publish Before Mark Published

Protection:

```text
Event may publish twice. Consumer idempotency handles duplicate.
```

### Duplicate Client Request

Protection:

```text
Idempotency key returns existing booking result.
```

### Payment Timeout

Protection:

```text
Treat as unknown. Retry with same idempotency key or reconcile with provider.
```

### Compensation Fails

Protection:

```text
Persist compensation state, retry with backoff, alert, and allow manual repair.
```

---

## 15. Interview Checklist

When explaining a cross-service workflow, include:

1. owning service for each invariant
2. local transaction boundary
3. saga style: orchestration or choreography
4. outbox for reliable event publication
5. idempotency for commands and consumers
6. retry and DLQ policy
7. compensation path
8. workflow state visibility
9. reconciliation/manual repair path
10. observability metrics

---

## 16. Strong Closing Answer

```text
For cross-service booking, I do not try to force one distributed transaction. I keep local
transactions inside each owning service, model the workflow as a saga, persist events through
outbox, make commands and consumers idempotent, and design compensation, DLQ, replay,
reconciliation, and alerts for the failure paths.
```
