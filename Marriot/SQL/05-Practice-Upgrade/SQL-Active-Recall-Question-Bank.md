# SQL Active Recall Question Bank

> Track: SQL Interview Track - Practice Upgrade  
> Mode: answer before reading notes.

Goal: convert SQL knowledge into retrieval speed for interviews.

---

## 1. How To Use

Rules:

1. Answer aloud without notes.
2. State the output grain before writing any query.
3. Write SQL for coding questions.
4. Explain correctness, NULL/tie behavior, and indexes.
5. Mark each question Green, Yellow, or Red.
6. Repeat Red questions after 24 hours and 7 days.

Strong answer shape:

```text
grain -> query shape -> correctness -> tie/null handling -> index/plan -> trade-off
```

---

## 2. SQL Fundamentals

1. What is SQL logical execution order?
2. `WHERE` vs `HAVING`?
3. `INNER JOIN` vs `LEFT JOIN`?
4. `RIGHT JOIN` vs rewriting with `LEFT JOIN`?
5. What does `CROSS JOIN` do?
6. What is a self join?
7. `DISTINCT` vs `GROUP BY`?
8. `UNION` vs `UNION ALL`?
9. `INTERSECT` vs `EXCEPT`?
10. `COUNT(*)` vs `COUNT(column)`?
11. How does SQL handle `NULL` in comparisons?
12. Why is `NOT IN` dangerous with `NULL`?
13. `COALESCE` vs `NULLIF`?
14. What does `CASE` do?
15. What is output grain?

---

## 3. Joins And Aggregation

1. Why can joins multiply rows?
2. How do you prevent aggregation after join multiplication?
3. How do you find customers with no orders?
4. When do you aggregate before joining?
5. How do you count distinct users per month?
6. How do you calculate revenue by product category?
7. How do you filter groups with at least 3 orders?
8. How do you handle missing relationship rows?
9. How do you preserve rows from the left table while filtering right table values?
10. Why can a `WHERE` condition on the right table turn `LEFT JOIN` into `INNER JOIN`?

---

## 4. Window Functions

1. What does a window function do that `GROUP BY` does not?
2. `ROW_NUMBER` vs `RANK` vs `DENSE_RANK`?
3. How do you get latest row per group?
4. How do you get top 3 products per category?
5. How do you calculate running total?
6. How do you calculate month-over-month growth?
7. What do `LAG` and `LEAD` do?
8. What is a window frame?
9. `ROWS` vs `RANGE`?
10. Why should you include deterministic tie-breakers?
11. How do you calculate moving average?
12. How do you find users with consecutive login days?
13. When should you deduplicate before windowing?
14. What happens if window `ORDER BY` is missing?
15. How do you calculate percent of total with a window function?

---

## 5. CTEs Subqueries LATERAL Recursive SQL

1. What is a CTE?
2. CTE vs subquery?
3. Can a CTE improve performance?
4. What is a recursive CTE?
5. What are anchor and recursive members?
6. How do you prevent cycles in recursive CTEs?
7. What is `LATERAL`?
8. When is `LATERAL` better than a window ranking approach?
9. How do you get latest 3 orders per customer using `LATERAL`?
10. How do you debug a multi-CTE query?
11. What is a date spine?
12. Why is date spine important for retention/time-series metrics?
13. How do you calculate p95 using SQL?
14. What are `GROUPING SETS`?
15. `ROLLUP` vs `CUBE`?

---

## 6. Query Pattern Recall

Write SQL from memory:

1. Second highest salary.
2. Nth highest salary.
3. Employees earning more than manager.
4. Customers who never ordered.
5. Duplicate emails.
6. Delete duplicates but keep the newest row.
7. Latest order status per order.
8. Top N per group.
9. Running total by date.
10. Month-over-month growth.
11. Consecutive login streaks.
12. Overlapping reservations.
13. Anti-join missing relationship.
14. Pivot with conditional aggregation.
15. Customer retention by cohort month.
16. Funnel conversion from events.
17. Moving average revenue.
18. Percentile order value by country.
19. Revenue rollup by country/region/city.
20. Latest 3 orders per customer.

---

## 7. Indexing And EXPLAIN

1. What is an index?
2. What is a B-tree index good for?
3. Composite index order rule?
4. What is the leftmost prefix rule?
5. What is a covering index?
6. What is a partial index?
7. What is a functional index?
8. What is sargability?
9. Give an example of non-sargable SQL.
10. How do you fix `LOWER(email) = LOWER(?)`?
11. When should you not add an index?
12. What does `EXPLAIN ANALYZE` show?
13. Estimated rows vs actual rows?
14. Sequential scan vs index scan?
15. Nested loop vs hash join vs merge join?
16. What does `BUFFERS` add?
17. What does temp read/write imply?
18. What does stale statistics mean?
19. How do you debug slow query?
20. Cursor pagination vs OFFSET pagination?

---

## 8. PostgreSQL Production Ops

1. What is MVCC?
2. Why do updates create dead tuples?
3. What does VACUUM do?
4. What does ANALYZE do?
5. What is bloat?
6. Why are long transactions dangerous?
7. What is autovacuum?
8. When is `VACUUM FULL` risky?
9. What are extended statistics?
10. B-tree vs GIN vs BRIN?
11. When would you use a GIN index?
12. When would you use a BRIN index?
13. When does partitioning help?
14. Why must queries filter by partition key?
15. What is replica lag?
16. When should read-after-write use primary DB?
17. Why is connection pool exhaustion usually a symptom?
18. What does `pg_stat_activity` show?
19. How do you find lock blockers?
20. How do you design DB health dashboards?

---

## 9. Transactions And Concurrency

1. What is ACID?
2. Dirty read vs non-repeatable read vs phantom read?
3. Read committed vs repeatable read vs serializable?
4. Pessimistic vs optimistic locking?
5. What does `SELECT FOR UPDATE` do?
6. How do unique constraints protect business rules?
7. How do you prevent double booking?
8. What is an idempotency table?
9. What causes deadlocks?
10. How do you reduce deadlocks?
11. Why keep transactions short?
12. Why avoid external API calls inside DB transaction?
13. How do indexes affect lock scope?
14. What is outbox pattern?
15. How do you make payment retry safe?

---

## 10. Data Modeling

1. OLTP vs OLAP?
2. Normalization vs denormalization?
3. What is a primary key?
4. What is a foreign key?
5. What is a unique constraint?
6. What is a check constraint?
7. What is a star schema?
8. Fact table vs dimension table?
9. Slowly changing dimension?
10. How do you model order items with price snapshot?
11. How do you model booking availability?
12. How do you model audit/status history?
13. How do you model idempotency keys?
14. How do you model product catalog price history?
15. Tenant shared table vs schema per tenant vs DB per tenant?
16. Why should tenant ID be in indexes?
17. Soft delete pros/cons?
18. How do you enforce valid state transitions?
19. What is a ledger table?
20. How do you model append-only events?

---

## 11. Security And Governance

1. What is SQL injection?
2. Why do prepared statements help?
3. Why are dynamic column names dangerous?
4. How do you safely implement `sortBy`?
5. What is least privilege?
6. Runtime DB role vs migration DB role?
7. How do grants work at a high level?
8. What is row-level security?
9. RLS benefits and traps?
10. What is tenant isolation?
11. How can cache keys leak tenant data?
12. How should PII be classified?
13. Masking vs encryption?
14. What belongs in audit log?
15. Why is soft-deleted data still sensitive?
16. How do secure reporting roles work?
17. What is data minimization?
18. How do you audit PII access?
19. How do you prevent tenant leak in reports?
20. Why is encryption at rest not enough?

---

## 12. Final Readiness Gate

You are ready when you can answer without notes:

1. Solve latest row, top-N, retention, funnel, gaps/islands, and overlapping ranges.
2. Debug a slow query from SQL text to EXPLAIN evidence.
3. Design indexes for API access patterns.
4. Explain MVCC, VACUUM, bloat, stale stats, and partitioning.
5. Prevent double booking and payment duplicate processing.
6. Design tenant-safe schemas and queries.
7. Explain SQL injection defense and least privilege.
8. Model OLTP orders/bookings/payments and OLAP star schemas.
9. Design a production dashboard and slow-query runbook.
10. Explain trade-offs with confidence under follow-up pressure.
