# SQL Scenario Drill Bank

> Track: SQL Interview Track - Practice Upgrade  
> Goal: practice SQL as query writing, backend correctness, database performance, and production ownership.

Use this after reading the concept sheets.

---

## 1. Scenario Answer Shape

For query scenarios:

```text
clarify output grain -> write SQL -> handle NULL/ties/missing data -> explain correctness -> index/plan
```

For production scenarios:

```text
symptom -> blast radius -> recent change -> exact SQL/plan -> waits/locks/pool -> mitigation -> prevention
```

For schema scenarios:

```text
entities -> constraints -> indexes -> history/audit -> concurrency -> access/security -> analytics path
```

---

## 2. Query Writing Scenarios

### Scenario 1: Latest Status Per Order

Prompt:

```text
Given order_status_history, return current status for every order.
```

Must include:

- output grain: one row per order
- deterministic tie-breaker
- `ROW_NUMBER` or PostgreSQL `DISTINCT ON`
- index idea: `(order_id, changed_at desc, status_history_id desc)`

---

### Scenario 2: Top 3 Products Per Category

Prompt:

```text
Return top 3 products by revenue inside each category for the last 30 days.
```

Must include:

- pre-aggregate product revenue
- rank inside category
- decide tie behavior with `ROW_NUMBER`, `RANK`, or `DENSE_RANK`
- date filter before aggregation

---

### Scenario 3: Month-Over-Month Growth

Prompt:

```text
Calculate monthly revenue and percent growth from previous month.
```

Must include:

- month grain
- `date_trunc`
- `LAG`
- `NULLIF` for divide-by-zero
- missing month consideration

---

### Scenario 4: Consecutive Login Streaks

Prompt:

```text
Find users with 5 or more consecutive login days.
```

Must include:

- deduplicate user-day first
- `ROW_NUMBER`
- date minus row-number island key
- group by island

---

### Scenario 5: Funnel Conversion

Prompt:

```text
Calculate viewed_product -> added_to_cart -> paid conversion.
```

Must include:

- user/session grain decision
- ordered event timestamps
- handle missing steps
- avoid counting paid before cart

---

### Scenario 6: Cohort Retention

Prompt:

```text
Calculate month 0, 1, 2 retention by signup month.
```

Must include:

- signup cohort grain
- activity user-month deduplication
- month difference calculation
- divide by cohort size

---

## 3. Performance Scenarios

### Scenario 7: Slow Customer Orders API

Prompt:

```text
GET /customers/{id}/orders is slow after order table grows to 200M rows.
```

Answer should include:

- exact SQL and bind shape
- EXPLAIN ANALYZE
- check full scan/sort/deep OFFSET
- composite index: `(customer_id, created_at desc, order_id desc)`
- keyset pagination
- avoid SELECT * if response only needs few columns

---

### Scenario 8: Dashboard Query Slows Primary DB

Prompt:

```text
Revenue dashboard query spikes CPU on primary during business hours.
```

Answer should include:

- isolate analytics from OLTP
- materialized view/summary table
- read replica/warehouse
- refresh freshness SLA
- indexes on summary table

---

### Scenario 9: Bad Planner Estimate

Prompt:

```text
EXPLAIN estimates 100 rows but actual rows are 5 million.
```

Answer should include:

- stale stats
- skewed distribution
- correlated columns
- `ANALYZE`
- extended statistics if needed

---

### Scenario 10: Query Spills To Disk

Prompt:

```text
EXPLAIN BUFFERS shows temp read/write for sort/hash.
```

Answer should include:

- large sort/hash aggregation
- reduce rows earlier
- supporting index for sort
- pre-aggregation
- work memory awareness without blindly tuning globally

---

## 4. PostgreSQL Ops Scenarios

### Scenario 11: Table Bloat

Prompt:

```text
Orders table disk usage grows quickly although live row count is stable.
```

Answer should include:

- MVCC dead tuples
- UPDATE/DELETE churn
- autovacuum lag
- long transactions preventing cleanup
- inspect `pg_stat_user_tables`
- maintenance plan, not casual `VACUUM FULL`

---

### Scenario 12: Replica Lag

Prompt:

```text
User creates order, then order history page served from replica does not show it.
```

Answer should include:

- read-after-write inconsistency
- route critical reads to primary
- replica lag monitoring
- user/session stickiness if needed
- freshness SLA

---

### Scenario 13: Connection Pool Exhaustion

Prompt:

```text
Application pool has many pending threads and DB active sessions are high.
```

Answer should include:

- do not blindly raise pool size
- inspect slow queries, locks, long transactions
- check external calls inside transaction
- DB CPU/I/O saturation
- pool timeout metrics

---

### Scenario 14: Migration Hangs

Prompt:

```text
Deployment hangs while adding an index to a large hot table.
```

Answer should include:

- lock behavior
- use `CREATE INDEX CONCURRENTLY`
- migration outside app transaction if needed
- backfill in batches
- expand-contract release flow

---

## 5. Concurrency Correctness Scenarios

### Scenario 15: Double Booking

Prompt:

```text
Two users reserve the last room at the same time.
```

Answer should include:

- invariant protected in database
- atomic update or lock
- check affected rows
- unique/check constraints
- transaction boundary

---

### Scenario 16: Duplicate Payment Processing

Prompt:

```text
Payment API retries after timeout and inserts duplicate payment.
```

Answer should include:

- idempotency key table
- unique constraint
- request hash
- safe retry behavior
- payment state machine

---

### Scenario 17: Deadlock

Prompt:

```text
Two transactions update account rows in opposite order and deadlock.
```

Answer should include:

- consistent lock ordering
- shorter transactions
- proper indexes
- retry transaction on deadlock
- monitoring deadlock count

---

## 6. Data Modeling Scenarios

### Scenario 18: Orders And Payments

Prompt:

```text
Design schema for e-commerce orders, order items, payments, and refunds.
```

Must include:

- item price snapshot
- payment attempts
- refund records
- constraints
- indexes for customer order history
- audit/status history

---

### Scenario 19: Marketplace Ledger

Prompt:

```text
Design ledger tables for marketplace buyer payment, seller payout, and platform fee.
```

Must include:

- append-only ledger entries
- debit/credit direction
- idempotency
- reconciliation keys
- no destructive updates for money movement

---

### Scenario 20: Hotel Availability

Prompt:

```text
Design schema to prevent overselling rooms by date and room type.
```

Must include:

- inventory by hotel/room_type/date
- check constraints
- atomic decrement or lock
- booking status history
- cancellation flow

---

### Scenario 21: Analytics Star Schema

Prompt:

```text
Design star schema for order analytics.
```

Must include:

- fact table grain
- dimensions
- slowly changing dimension strategy
- partitioning by date if large
- dashboard query patterns

---

## 7. Security Governance Scenarios

### Scenario 22: SQL Injection Risk

Prompt:

```text
Search API accepts filter, sortBy, and direction.
```

Answer should include:

- bind parameters for values
- allowlist for sort columns/direction
- limit/page size cap
- least-privilege DB role
- malicious input tests

---

### Scenario 23: Tenant Data Leak

Prompt:

```text
A reporting query returns rows from another tenant.
```

Answer should include:

- disable/rollback report
- investigate access/audit logs
- tenant predicate from trusted context
- tenant-aware indexes/constraints
- tests for tenant filtering
- consider RLS/views for defense in depth

---

### Scenario 24: Support Tool PII Exposure

Prompt:

```text
Support role can query raw customer email and address.
```

Answer should include:

- masked views
- read-only role
- audit PII access
- data minimization
- approval/retention for exports

---

## 8. MAANG Capstone Scenarios

### Scenario 25: Booking Platform SQL Layer

Prompt:

```text
Design SQL for booking platform: customers, hotels, inventory, bookings, payments, status
history, idempotency, and analytics.
```

Strong answer includes:

- OLTP normalized schema
- DB constraints for invariants
- transaction/lock plan for booking
- idempotency for payment
- audit/status history
- tenant isolation if B2B
- indexes for API paths
- analytics star schema/read model
- migration strategy

---

### Scenario 26: Feed Analytics

Prompt:

```text
Compute daily active users, session funnels, top posts, and retention for a social feed.
```

Strong answer includes:

- event table grain
- date spine
- window functions
- funnel ordering
- cohort retention
- partitioning/read replica/warehouse for scale

---

### Scenario 27: Fraud Risk SQL

Prompt:

```text
Find suspicious users based on payment velocity, failed attempts, device changes, and location changes.
```

Strong answer includes:

- rolling windows
- conditional aggregation
- `LAG` for location/device changes
- indexes/partitioning for time filters
- explain false-positive trade-offs

---

## 9. Completion Gate

You are ready when you can solve:

1. 6 query-writing scenarios without notes.
2. 4 performance/ops scenarios with EXPLAIN language.
3. 3 concurrency scenarios with constraints and transactions.
4. 3 modeling scenarios with indexes and history.
5. 2 security scenarios with tenant/PII controls.
6. 1 full capstone from schema to production runbook.
