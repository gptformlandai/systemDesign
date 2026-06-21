# SQL Coding Design Mini Labs

> Track: SQL Interview Track - Practice Upgrade  
> Goal: build hands-on SQL readiness through small query, schema, performance, and production exercises.

Each lab should take 45-120 minutes.

---

## 1. Lab Output Rules

For every lab, produce:

1. Output grain.
2. SQL/schema/runbook.
3. Correctness explanation.
4. Edge cases: NULLs, ties, missing data, duplicate rows.
5. Performance/index notes.
6. 60-second interview explanation.

---

## 2. Lab 1: Query Pattern Dataset

Create a small mental or real dataset:

```sql
customers(customer_id, email, country, signup_at)
orders(order_id, customer_id, status, total_amount, created_at)
order_items(order_item_id, order_id, product_id, category_id, quantity, unit_price)
products(product_id, category_id, product_name)
order_status_history(status_history_id, order_id, status, changed_at)
user_activity(user_id, activity_at, event_name, session_id)
```

Deliverable:

- 10 sample rows per table
- expected outputs for at least 5 labs

---

## 3. Lab 2: Latest Row Per Group

Write:

- latest order status per order with `ROW_NUMBER`
- PostgreSQL `DISTINCT ON` version

Must handle:

- same `changed_at` tie with `status_history_id`
- orders with no history if joined from `orders`

Index:

```sql
CREATE INDEX idx_osh_order_changed
ON order_status_history (order_id, changed_at DESC, status_history_id DESC);
```

---

## 4. Lab 3: Top N Per Group With Ties

Write:

- top 3 products by revenue per category
- version with no ties using `ROW_NUMBER`
- version with ties using `RANK`

Explain:

```text
ROW_NUMBER gives exactly N rows per group. RANK can return more than N rows when tied.
```

---

## 5. Lab 4: Retention Cohort Query

Build:

- cohort month from `signup_at`
- activity month from `activity_at`
- month number offset
- retention rate

Must handle:

- duplicate events in same month
- divide by zero
- missing months if dashboard requires zero rows

---

## 6. Lab 5: Funnel Query

Build:

- viewed_product -> added_to_cart -> paid funnel
- session-aware version
- first event timestamp per step

Must handle:

- paid before cart should not count as completed funnel
- missing steps
- multiple sessions per user

---

## 7. Lab 6: Gaps And Islands

Build:

- consecutive login streaks per user
- find streaks >= 5 days

Must handle:

- duplicate same-day logins
- date casting
- output start date, end date, length

---

## 8. Lab 7: Overlapping Date Ranges

Build:

- query to find overlapping room reservations
- check overlap condition

Overlap rule:

```sql
new_check_in < existing_check_out
AND new_check_out > existing_check_in
```

Deliverable:

- explain why this catches partial, full, and boundary overlaps

---

## 9. Lab 8: Slow Orders API Index Design

Given query:

```sql
SELECT order_id, status, total_amount, created_at
FROM orders
WHERE customer_id = :customer_id
ORDER BY created_at DESC, order_id DESC
LIMIT 20;
```

Design:

```sql
CREATE INDEX idx_orders_customer_created_id
ON orders (customer_id, created_at DESC, order_id DESC);
```

Deliverable:

- explain why this supports filter, sort, and limit
- add keyset pagination version
- describe expected EXPLAIN improvement

---

## 10. Lab 9: Non-Sargable Query Repair

Given:

```sql
SELECT *
FROM orders
WHERE DATE(created_at) = DATE '2026-06-21';
```

Fix:

```sql
SELECT *
FROM orders
WHERE created_at >= TIMESTAMP '2026-06-21 00:00:00'
  AND created_at < TIMESTAMP '2026-06-22 00:00:00';
```

Deliverable:

- explain sargability
- design index
- identify other non-sargable patterns

---

## 11. Lab 10: EXPLAIN Walkthrough

Take any query and write a fake or real EXPLAIN review:

- scan type
- estimated rows vs actual rows
- join type
- sort/hash behavior
- loops
- buffers
- improvement proposal

Deliverable:

```text
Before: why slow
After: what changed
Risk: write cost/storage/plan regression
```

---

## 12. Lab 11: MVCC And Bloat Investigation

Write a runbook for:

```text
Table size grows but live rows stay stable.
```

Include:

- dead tuples query
- autovacuum recency
- long transaction check
- safe mitigation
- what not to do during peak traffic

---

## 13. Lab 12: Partitioned Events Table

Design:

```text
events table with 2 years of high-volume data and common time-range queries.
```

Deliverable:

- range partition DDL sketch
- partition creation strategy
- retention/drop strategy
- index per partition strategy
- query that benefits from pruning

---

## 14. Lab 13: Materialized View Dashboard

Build:

- daily revenue materialized view
- unique index for concurrent refresh
- refresh schedule
- freshness metadata

Deliverable:

```text
Dashboard uses MV because expensive aggregation can be slightly stale. OLTP primary is
protected from repeated heavy scans.
```

---

## 15. Lab 14: Double Booking Correctness

Design:

- `room_inventory` table
- atomic decrement query
- booking table
- transaction flow

Test mentally:

- two concurrent requests
- one room left
- one update affected row succeeds
- the other gets zero affected rows

---

## 16. Lab 15: Payment Idempotency

Design:

```sql
CREATE TABLE idempotency_keys (
    key_id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    response_body JSONB,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (tenant_id, idempotency_key)
);
```

Deliverable:

- same key/same payload behavior
- same key/different payload conflict
- transaction boundary
- cleanup/retention

---

## 17. Lab 16: Marketplace Ledger

Design:

- ledger accounts
- ledger entries
- transaction group id
- debit/credit direction
- idempotency key
- reconciliation reference

Rules:

- append-only money movement
- no destructive updates
- constraints for valid amount
- indexes by account/time

---

## 18. Lab 17: Tenant-Safe Schema

Take `orders` and make it tenant safe:

- composite primary key or unique key including `tenant_id`
- tenant-aware foreign keys
- tenant-aware indexes
- tenant predicate examples
- cache key format
- reporting query test

Deliverable:

```text
A tenant leak test case that fails when tenant_id filter is missing.
```

---

## 19. Lab 18: SQL Injection Safe Search

Build a safe search API query design:

- bind parameters for values
- allowlist sort columns
- allowlist sort direction
- page size cap
- no raw identifier input

Deliverable:

- malicious input examples
- expected safe behavior

---

## 20. Lab 19: PII Masked Support View

Design:

- customers table with PII
- support view with masked email/phone
- read-only grant
- audit event for support lookup

Deliverable:

```text
Support can help users without raw broad PII table access.
```

---

## 21. Lab 20: Full SQL Capstone

Design the SQL layer for:

```text
Hotel booking platform with customers, hotels, room inventory, bookings, payments, refunds,
status history, idempotency, tenant isolation, reporting, and production runbooks.
```

Must include:

- OLTP schema
- constraints
- transaction flow
- indexes
- query examples
- analytics/read model
- security/governance controls
- slow-query and lock-wait runbook

---

## 22. Completion Gate

You completed the labs when you can:

1. Write 8 query patterns without notes.
2. Explain 4 index/EXPLAIN improvements.
3. Design 3 correctness schemas with constraints.
4. Handle tenant/PII/security concerns.
5. Write one full production runbook for slow SQL.
6. Defend the capstone under follow-up questions.
