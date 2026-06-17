# SQL Transactions Locking Concurrency Platinum Sheet

Target: backend engineers who need SQL correctness under real concurrent traffic.

This sheet focuses on ACID, isolation, locks, deadlocks, idempotency, and practical backend
cases like double booking and duplicate payments.

---

## 0. The Core Idea

SQL correctness is not only query syntax. It is also what happens when two users do the
same thing at the same time.

```text
If concurrent requests can break the business rule, the business rule belongs in the
database constraint/transaction design, not only in Java code.
```

---

# 1. ACID In Interview Language

| Property | Meaning |
|---|---|
| Atomicity | all changes happen or none happen |
| Consistency | constraints and invariants remain valid |
| Isolation | concurrent transactions do not corrupt each other |
| Durability | committed data survives failure |

Strong answer:

```text
ACID means the database gives a safe boundary for related changes, but isolation level and
locking strategy determine how concurrent transactions interact.
```

---

# 2. Transaction Basics

```sql
BEGIN;

UPDATE accounts
SET balance = balance - 100
WHERE account_id = 1;

UPDATE accounts
SET balance = balance + 100
WHERE account_id = 2;

COMMIT;
```

If failure happens before commit:

```sql
ROLLBACK;
```

Backend rule:

```text
Keep transactions short. Do not hold database locks while calling slow external services.
```

---

# 3. Isolation Anomalies

| Anomaly | Meaning |
|---|---|
| Dirty read | read uncommitted data |
| Non-repeatable read | same row read twice gives different committed value |
| Phantom read | same predicate returns new rows on re-run |
| Lost update | two transactions overwrite each other's update |
| Write skew | separate rows individually valid, together invalid |

Interview line:

```text
Isolation is about what each transaction is allowed to observe while others are running.
```

---

# 4. Isolation Levels

| Level | General Meaning |
|---|---|
| READ COMMITTED | each statement sees committed data |
| REPEATABLE READ | transaction sees stable snapshot in many databases |
| SERIALIZABLE | strongest, prevents many anomalies by acting like serial execution |

Important:

```text
Exact behavior varies by database. Always know the database you use in production.
```

Senior answer:

```text
I do not blindly set SERIALIZABLE. I first model constraints, use row locks or optimistic
locking where appropriate, and keep transactions short.
```

---

# 5. Pessimistic Locking

Use when conflicts are frequent and you want one transaction to wait.

```sql
BEGIN;

SELECT available_rooms
FROM room_inventory
WHERE hotel_id = 10
  AND room_type = 'KING'
  AND stay_date = DATE '2026-07-01'
FOR UPDATE;

UPDATE room_inventory
SET available_rooms = available_rooms - 1
WHERE hotel_id = 10
  AND room_type = 'KING'
  AND stay_date = DATE '2026-07-01'
  AND available_rooms > 0;

COMMIT;
```

Pros:

- simple correctness
- serializes conflicting updates

Cons:

- waiting
- deadlock risk
- lower concurrency

---

# 6. Optimistic Locking

Use when conflicts are rare.

Table:

```sql
CREATE TABLE room_inventory (
    inventory_id BIGINT PRIMARY KEY,
    available_rooms INT NOT NULL,
    version INT NOT NULL
);
```

Update:

```sql
UPDATE room_inventory
SET available_rooms = available_rooms - 1,
    version = version + 1
WHERE inventory_id = 101
  AND version = 7
  AND available_rooms > 0;
```

If affected rows = 0:

```text
Conflict or no availability. Retry or return failure.
```

Pros:

- no waiting locks for normal reads
- good for rare conflicts

Cons:

- retries under high contention
- caller must handle conflict

---

# 7. Unique Constraint As Business Protection

Application check is not enough:

```sql
SELECT COUNT(*)
FROM bookings
WHERE room_id = 10
  AND stay_date = DATE '2026-07-01';

-- two users both see zero, both insert
```

Better:

```sql
CREATE UNIQUE INDEX uq_booking_room_date
ON bookings (room_id, stay_date)
WHERE status IN ('CONFIRMED', 'PENDING');
```

Then duplicate insert fails safely.

Strong answer:

```text
I let the database enforce uniqueness because concurrent application checks can race.
```

---

# 8. Double Booking Case

## Requirement

Two users must not book the same last room.

## Strong Design

```text
1. Start transaction.
2. Lock inventory row or use optimistic version update.
3. Decrement only if available_rooms > 0.
4. Insert booking with unique/idempotency constraints.
5. Commit.
6. Call external side effects outside long DB transaction or via saga/outbox.
```

SQL pattern:

```sql
UPDATE room_inventory
SET available_rooms = available_rooms - 1
WHERE hotel_id = 10
  AND room_type = 'KING'
  AND stay_date = DATE '2026-07-01'
  AND available_rooms > 0;
```

If affected rows = 1, reserve succeeded. If 0, sold out.

---

# 9. Idempotency Table

Problem:

```text
Client retries POST /payments after timeout. Server must not charge twice.
```

Table:

```sql
CREATE TABLE idempotency_keys (
    key VARCHAR(100) PRIMARY KEY,
    request_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Flow:

```text
1. Client sends idempotency key.
2. Server inserts key.
3. If insert succeeds, process request.
4. If key exists, return stored response if same request hash.
5. Reject if same key has different request hash.
```

PostgreSQL-style insert:

```sql
INSERT INTO idempotency_keys (key, request_hash, status)
VALUES (:key, :hash, 'PROCESSING')
ON CONFLICT (key) DO NOTHING;
```

---

# 10. Deadlocks

Deadlock example:

```text
Transaction A locks row 1, waits for row 2.
Transaction B locks row 2, waits for row 1.
```

Prevention:

- lock rows in consistent order
- keep transactions short
- index predicates so fewer rows are locked
- avoid user/external calls inside transactions
- retry deadlock loser

Strong answer:

```text
Deadlocks are not always bugs in the database. They often mean transactions lock resources
in inconsistent order. I fix ordering and add retry for deadlock loser.
```

---

# 11. Lock Scope And Indexes

Bad index design can increase lock impact.

Example:

```sql
UPDATE bookings
SET status = 'EXPIRED'
WHERE status = 'PENDING'
  AND expires_at < CURRENT_TIMESTAMP;
```

Useful index:

```sql
CREATE INDEX idx_bookings_pending_expiry
ON bookings (expires_at)
WHERE status = 'PENDING';
```

Why:

```text
The database finds target rows faster and touches fewer rows/pages.
```

---

# 12. Long Transaction Smells

Avoid inside transaction:

- remote HTTP calls
- sending email
- waiting for user input
- large file processing
- huge batch update without chunks
- slow report query

Better:

- pending state
- outbox event
- chunking
- saga/reconciliation

---

# 13. Payment Timeout Case

Scenario:

```text
Payment provider times out after charge may have succeeded.
```

Bad:

```text
Retry blindly and charge twice.
```

Better:

```text
1. Store payment_attempt with idempotency key.
2. Send provider request with provider idempotency key.
3. If timeout, mark UNKNOWN/PENDING.
4. Reconcile using webhook or provider status API.
5. Update final status idempotently.
```

SQL controls:

- unique provider reference
- unique idempotency key
- state transition rules

---

# 14. Outbox Transaction

Problem:

```text
DB commit succeeds but message publish fails.
```

Solution:

```sql
BEGIN;

INSERT INTO bookings (...);

INSERT INTO outbox_events (event_id, aggregate_id, event_type, payload, status)
VALUES (:eventId, :bookingId, 'BookingCreated', :payload, 'NEW');

COMMIT;
```

Then publisher reads outbox and publishes.

Strong answer:

```text
Outbox makes database change and event creation atomic. Publishing is retried separately.
Consumers must still be idempotent.
```

---

# 15. Interview Question

> How do you prevent double booking in SQL?

Strong answer:

```text
I protect the invariant in the database. If inventory is count-based, I run an atomic update
that decrements only when available_rooms > 0 and check affected rows. If inventory is one
booking per resource/date, I add a unique constraint on resource and date for active booking
statuses. For high contention I may use SELECT FOR UPDATE; for low contention I can use
optimistic locking with version. I keep the transaction short and handle payment/notification
through pending state and outbox/saga.
```

---

# 16. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Check then insert without constraint | unique constraint or lock |
| Long transaction around HTTP call | pending state and saga |
| Ignore deadlock retry | retry deadlock loser safely |
| Raise isolation blindly | choose targeted lock/constraint |
| No idempotency for POST | idempotency key table |
| Consumer assumes exactly once | idempotent writes |
| Missing index on update predicate | index to reduce touched rows |

---

# 17. Final Rapid Revision

```text
Correctness under concurrency belongs in DB constraints/transactions.
Pessimistic lock waits. Optimistic lock retries.
Unique constraints beat application-only checks.
Atomic update with WHERE available > 0 prevents oversell.
Deadlocks need consistent lock order and retry.
Long transactions cause locks, pool exhaustion, and incidents.
Idempotency prevents duplicate POST side effects.
Outbox solves DB commit plus event publish reliability.
```

---

# 18. Official Source Notes

- PostgreSQL transaction isolation: https://www.postgresql.org/docs/current/transaction-iso.html
- PostgreSQL explicit locking: https://www.postgresql.org/docs/current/explicit-locking.html
- PostgreSQL indexes: https://www.postgresql.org/docs/current/indexes.html
