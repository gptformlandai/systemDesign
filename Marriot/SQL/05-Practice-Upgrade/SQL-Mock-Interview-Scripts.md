# SQL Mock Interview Scripts

> Track: SQL Interview Track - Practice Upgrade  
> Goal: simulate SQL interview rounds from fundamentals to MAANG senior backend/database depth.

Use these scripts aloud. Time each round.

---

## 1. Universal Answer Shapes

Query answer:

```text
grain -> SQL -> correctness -> edge cases -> performance/index -> alternative
```

Performance answer:

```text
symptom -> exact query -> EXPLAIN evidence -> rows/plan/waits -> mitigation -> prevention
```

Schema answer:

```text
entities -> constraints -> indexes -> history -> concurrency -> security -> analytics
```

---

## 2. Round 1: SQL Fundamentals

Time: 30 minutes

### Questions

1. Explain SQL logical execution order.
2. `WHERE` vs `HAVING`?
3. `INNER JOIN` vs `LEFT JOIN`?
4. Why can `LEFT JOIN` accidentally become `INNER JOIN`?
5. `COUNT(*)` vs `COUNT(column)`?
6. What is output grain?
7. Why is `NOT IN` dangerous with `NULL`?
8. `UNION` vs `UNION ALL`?
9. What is a CTE?
10. What is the difference between `GROUP BY` and window functions?

### Strong Signal

Candidate defines output grain before writing SQL and catches NULL/join traps.

---

## 3. Round 2: Query Coding Patterns

Time: 45 minutes

### Prompts

1. Latest row per group.
2. Top 3 per group.
3. Running total.
4. Month-over-month growth.
5. Customers who never ordered.
6. Duplicate detection.
7. Delete duplicates safely.
8. Consecutive login days.
9. Overlapping date ranges.
10. Pivot with conditional aggregation.

### Follow-Ups

- How do ties change your query?
- What happens with NULL values?
- What index would help?
- How would you test this query?

### Excellent Answer Includes

- correct grain
- CTEs/window functions
- deterministic ordering
- anti-join awareness
- index notes

---

## 4. Round 3: Advanced Analytics

Time: 45 minutes

### Prompt

```text
Write SQL for retention, funnel conversion, p95 order value, and revenue rollups.
```

### Interviewer Follow-Ups

1. How do you handle missing days/months?
2. What is a date spine?
3. How do window frames work?
4. `ROWS` vs `RANGE`?
5. How do you compute p95?
6. `GROUPING SETS` vs `ROLLUP` vs `CUBE`?
7. When would you use `LATERAL`?
8. When would you use a materialized view?
9. What is the performance risk of dashboard SQL?
10. How do you validate output correctness?

### Excellent Answer Includes

- date spine for missing time buckets
- deduplication at the correct grain
- window frame/tie awareness
- materialized view/read model trade-offs

---

## 5. Round 4: Indexing And EXPLAIN

Time: 60 minutes

### Prompt

```text
An orders API query is slow on a 200M-row table.
```

### Interviewer Follow-Ups

1. What information do you ask for first?
2. How do you read `EXPLAIN ANALYZE`?
3. What does estimated vs actual rows tell you?
4. What index would help this query?
5. Composite index order rule?
6. What is sargability?
7. Why is deep OFFSET slow?
8. When should you not add an index?
9. What does `BUFFERS` tell you?
10. How do stale stats affect plans?

### Excellent Answer Includes

- exact query shape
- plan evidence
- index trade-off
- keyset pagination
- stats awareness

---

## 6. Round 5: Transactions And Concurrency

Time: 45 minutes

### Prompt

```text
Prevent double booking and duplicate payment processing.
```

### Interviewer Follow-Ups

1. What invariant must the database protect?
2. Pessimistic vs optimistic locking?
3. How does atomic update prevent oversell?
4. How does unique constraint help?
5. What is isolation level?
6. What causes deadlock?
7. How do you design idempotency keys?
8. Why keep transactions short?
9. What is outbox pattern?
10. How do indexes affect lock scope?

### Excellent Answer Includes

- constraints over app-only checks
- atomic update/locks
- idempotency table
- deadlock mitigation
- short transactions

---

## 7. Round 6: Data Modeling

Time: 60 minutes

### Prompt

```text
Design SQL schema for an e-commerce or hotel booking platform.
```

### Interviewer Follow-Ups

1. What are the entities?
2. What is the lifecycle?
3. What needs history?
4. What must be unique?
5. What constraints protect business rules?
6. What indexes support API paths?
7. How do payments/refunds work?
8. How do you support analytics?
9. How do you handle soft delete?
10. How do you handle multitenancy?

### Excellent Answer Includes

- normalized OLTP schema
- constraints/checks/FKs
- status history/audit
- idempotency
- star schema/read model for analytics

---

## 8. Round 7: PostgreSQL Production Ops

Time: 60 minutes

### Prompt

```text
Database p99 latency spikes and app connection pool is exhausted.
```

### Interviewer Follow-Ups

1. What do you check first?
2. What is MVCC?
3. What are dead tuples?
4. What does VACUUM do?
5. What is bloat?
6. How do stale stats hurt plans?
7. What is partitioning good for?
8. B-tree vs GIN vs BRIN?
9. What is replica lag?
10. How do you find lock blockers?

### Excellent Answer Includes

- `pg_stat_activity` thinking
- plan/lock/pool correlation
- autovacuum/stats awareness
- no blind pool increase
- safe mitigation first

---

## 9. Round 8: Security Governance

Time: 45 minutes

### Prompt

```text
Review database access for SQL injection, tenant isolation, and PII exposure.
```

### Interviewer Follow-Ups

1. How do prepared statements work?
2. How do you safely allow sort fields?
3. What is least privilege?
4. Runtime role vs migration role?
5. Shared-table multitenancy risks?
6. What is row-level security?
7. How do cache keys leak tenant data?
8. How do you mask PII?
9. What belongs in audit logs?
10. Why is encryption at rest not enough?

### Excellent Answer Includes

- parameters and allowlists
- tenant-aware constraints/indexes
- least-privilege grants
- masked views
- audit and retention

---

## 10. Round 9: MAANG SQL Capstone

Time: 75 minutes

### Prompt

```text
Design and operate the SQL layer for a hotel booking platform with OLTP writes, analytics,
security, concurrency, and production debugging.
```

### Must Cover

- customers, hotels, room inventory, bookings, payments, refunds
- idempotency and status history
- double-booking prevention
- indexes for API query paths
- dashboard/read model/star schema
- SQL injection and tenant isolation
- PII masking/audit
- EXPLAIN slow-query debugging
- partitioning/materialized view decisions
- migration strategy

### Pass Criteria

- correctness before performance
- constraints before app-only checks
- EXPLAIN evidence before optimization claims
- tenant/PII safety visible
- production runbook thinking

---

## 11. Full SQL Interview Loop

Run these back-to-back:

1. 15 min fundamentals lightning.
2. 45 min query coding patterns.
3. 45 min advanced analytics.
4. 45 min indexing/EXPLAIN.
5. 45 min transactions/concurrency.
6. 45 min schema design.
7. 45 min production ops/security.

Pass criteria:

- no query without grain
- no performance claim without plan/index thinking
- no concurrency answer without constraints/transactions
- no multitenancy answer without tenant-aware keys and filters
- can explain one capstone end to end
