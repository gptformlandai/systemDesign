# SQL Performance Indexing EXPLAIN Platinum Sheet

Target: backend and MAANG SQL interviews where query performance matters.

This sheet teaches how to reason about slow SQL using query shape, indexes, sargability,
pagination, joins, and EXPLAIN.

---

## 0. Performance Mindset

Do not optimize by guessing.

```text
Query shape -> data volume -> selectivity -> index match -> execution plan -> measured
runtime.
```

Strong answer:

```text
I first make the query correct, then inspect EXPLAIN ANALYZE to see rows read, join strategy,
scan type, sort/hash cost, and actual time.
```

---

# 1. The Four Things That Make Queries Slow

| Cause | Example |
|---|---|
| Reads too many rows | missing selective WHERE/index |
| Joins too much data | join before filtering/aggregating |
| Sorts too much data | ORDER BY without supporting index |
| Waits too long | locks, connection pool, disk, network |

Memory:

```text
Most slow queries either read too much, join too much, sort too much, or wait too much.
```

---

# 2. Index Mental Model

An index is a sorted access path.

Good for:

- selective filters
- joins
- ordering
- uniqueness
- covering frequent reads

Not free:

- consumes storage
- slows writes
- must be maintained
- can be ignored if query shape does not match

---

# 3. B-Tree Index

Best for:

- equality
- range
- prefix sort
- joins on keys

Example:

```sql
CREATE INDEX idx_orders_customer_date
ON orders (customer_id, order_date);
```

Helps:

```sql
WHERE customer_id = 42
ORDER BY order_date DESC
```

May not help:

```sql
WHERE order_date >= DATE '2026-01-01'
```

because `customer_id` is the leftmost column.

---

# 4. Composite Index Order

Rule of thumb:

```text
Equality columns first, then range/sort columns, then covering columns if supported.
```

Example query:

```sql
SELECT order_id, total_amount
FROM orders
WHERE customer_id = 42
  AND status = 'PAID'
  AND order_date >= DATE '2026-01-01'
ORDER BY order_date DESC
LIMIT 20;
```

Good index:

```sql
CREATE INDEX idx_orders_customer_status_date
ON orders (customer_id, status, order_date DESC);
```

Why:

- equality on customer/status
- range and sort on order_date
- LIMIT can stop early

---

# 5. Covering Index / Index-Only Scan

If the index contains all columns needed by the query, the database may answer from the
index with fewer table reads.

Example:

```sql
CREATE INDEX idx_orders_customer_date_total
ON orders (customer_id, order_date DESC, total_amount);
```

Query:

```sql
SELECT order_date, total_amount
FROM orders
WHERE customer_id = 42
ORDER BY order_date DESC
LIMIT 10;
```

Trade-off:

```text
Covering indexes speed reads but increase index size and write cost.
```

---

# 6. Partial Index

Useful when most queries target a subset.

```sql
CREATE INDEX idx_orders_pending_created
ON orders (created_at)
WHERE status = 'PENDING';
```

Good for:

```sql
SELECT *
FROM orders
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 100;
```

Why:

```text
The index is smaller because it contains only pending rows.
```

---

# 7. Functional Index

If query must use a function, create a matching index.

Bad without functional index:

```sql
SELECT *
FROM users
WHERE LOWER(email) = LOWER('A@Example.com');
```

Fix:

```sql
CREATE INDEX idx_users_lower_email
ON users (LOWER(email));
```

Alternative:

- store normalized email in separate column
- use database-specific case-insensitive type if appropriate

---

# 8. Sargability

A predicate is sargable when it can use an index efficiently.

Bad:

```sql
WHERE DATE(created_at) = DATE '2026-06-17'
```

Better:

```sql
WHERE created_at >= TIMESTAMP '2026-06-17 00:00:00'
  AND created_at <  TIMESTAMP '2026-06-18 00:00:00'
```

Bad:

```sql
WHERE amount + tax > 100
```

Better:

```sql
WHERE amount > 100 - tax
```

when business semantics allow it.

---

# 9. EXPLAIN Terms To Recognize

| Term | Meaning |
|---|---|
| Seq Scan | reads table sequentially |
| Index Scan | uses index then visits table rows |
| Index Only Scan | uses index without table visit when possible |
| Bitmap Index Scan | index finds pages, then table pages read |
| Nested Loop | good for small outer set with indexed inner lookup |
| Hash Join | builds hash table for join |
| Merge Join | joins sorted inputs |
| Sort | explicit sort operation |
| Rows Removed by Filter | rows read but filtered later |
| Actual Time | measured runtime with EXPLAIN ANALYZE |

Interview line:

```text
I compare estimated rows with actual rows. Large mismatch often means stale stats or poor
selectivity estimation.
```

---

# 10. Slow Query Debug Template

Use this answer:

```text
I run EXPLAIN ANALYZE and inspect scan type, actual rows, estimated vs actual rows, join
strategy, sort/hash operations, and whether filters are applied as index conditions or only
after scanning. Then I match indexes to WHERE, JOIN, ORDER BY, and LIMIT shape.
```

Checklist:

1. Is the query reading too many rows?
2. Are predicates sargable?
3. Are joins on indexed keys?
4. Are filters applied before joins?
5. Is ORDER BY using an index?
6. Is LIMIT paired with useful order/index?
7. Are statistics stale?
8. Is the query blocked on locks?
9. Is pagination deep OFFSET?
10. Is a batch/report query hitting OLTP?

---

# 11. Pagination

Bad for deep pages:

```sql
SELECT *
FROM orders
ORDER BY created_at DESC
OFFSET 100000 LIMIT 20;
```

The database still must walk/sort many rows.

Better cursor pagination:

```sql
SELECT *
FROM orders
WHERE created_at < TIMESTAMP '2026-06-17 10:00:00'
ORDER BY created_at DESC
LIMIT 20;
```

Index:

```sql
CREATE INDEX idx_orders_created_at
ON orders (created_at DESC);
```

For stable ordering:

```sql
WHERE (created_at, order_id) < (TIMESTAMP '2026-06-17 10:00:00', 98765)
ORDER BY created_at DESC, order_id DESC
LIMIT 20;
```

---

# 12. Join Performance

Bad:

```sql
SELECT c.customer_id, SUM(o.total_amount)
FROM customers c
JOIN orders o ON o.customer_id = c.customer_id
WHERE c.country = 'US'
GROUP BY c.customer_id;
```

Can be fine, but if orders is huge and only some customers are US, ensure:

- customer filter is selective
- join key indexed
- orders.customer_id indexed
- grouping happens after necessary filtering

Often better:

```sql
WITH us_customers AS (
    SELECT customer_id
    FROM customers
    WHERE country = 'US'
)
SELECT uc.customer_id, SUM(o.total_amount)
FROM us_customers uc
JOIN orders o ON o.customer_id = uc.customer_id
GROUP BY uc.customer_id;
```

The optimizer may transform both similarly, but the second makes the intended grain clear.

---

# 13. Aggregation Performance

For dashboards over huge data:

- pre-aggregate if exact freshness is not needed
- use materialized views or summary tables
- keep OLTP and analytics workloads separate
- partition by time for large fact tables where appropriate
- index filter columns

Bad:

```text
Every dashboard request scans 5 years of orders.
```

Better:

```text
Daily revenue summary table updated by batch/stream.
```

---

# 14. Write Performance And Over-Indexing

Every insert/update/delete must update indexes.

Avoid:

- indexing every column
- duplicate indexes with same prefix
- unused indexes
- large covering indexes on write-heavy tables without evidence

Strong answer:

```text
I create indexes from query patterns and validate them with EXPLAIN. I also watch write cost,
storage, and unused indexes.
```

---

# 15. Backend Scenario: Slow Orders API

Problem:

```sql
SELECT order_id, total_amount, created_at
FROM orders
WHERE customer_id = ?
  AND status = 'PAID'
ORDER BY created_at DESC
LIMIT 20;
```

Good index:

```sql
CREATE INDEX idx_orders_customer_status_created
ON orders (customer_id, status, created_at DESC);
```

Answer:

```text
The query filters by customer_id and status, sorts by created_at, and limits to 20. I would
use a composite index in that order, then validate with EXPLAIN ANALYZE that the plan reads
few rows and avoids a large sort.
```

---

# 16. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Add index without query shape | design index from WHERE/JOIN/ORDER BY |
| Ignore write cost | check insert/update overhead |
| Use function on indexed column | rewrite predicate or functional index |
| Deep OFFSET pagination | cursor pagination |
| Select `*` in hot path | project needed columns |
| Missing tie-breaker in ORDER BY | add stable key |
| Guess plan | use EXPLAIN ANALYZE |

---

# 17. Final Rapid Revision

```text
Correct first, measure second.
Index equality -> range/sort -> covering.
Keep predicates sargable.
EXPLAIN ANALYZE shows actual work.
Large rows read means poor filtering/index.
Deep OFFSET is expensive.
Too many indexes hurt writes.
Dashboard scans may need summaries/materialized views.
```

---

# 18. Official Source Notes

- PostgreSQL EXPLAIN: https://www.postgresql.org/docs/current/using-explain.html
- PostgreSQL indexes: https://www.postgresql.org/docs/current/indexes.html
