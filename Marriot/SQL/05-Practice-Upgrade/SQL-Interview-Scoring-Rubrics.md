# SQL Interview Scoring Rubrics

> Track: SQL Interview Track - Practice Upgrade  
> Goal: measure SQL readiness honestly for backend, analytics, and MAANG-style interviews.

Use after mock rounds and scenario drills.

---

## 1. Score Scale

| Score | Meaning | Signal |
|---:|---|---|
| 1 | Fragile | guesses syntax, no grain/correctness thinking |
| 2 | Basic | solves simple queries but misses NULLs, ties, indexes |
| 3 | Solid | writes common queries and explains them |
| 4 | Senior | handles performance, concurrency, modeling, and production trade-offs |
| 5 | MAANG-ready | handles ambiguity, scale, failure, security, and edge cases under pressure |

Passing targets:

- Junior/backend starter: mostly 3s.
- Mid-level: 3.5 average, no 1s.
- Senior: 4 average, no gaps in query writing, indexing, transactions, modeling.
- MAANG senior: 4.3+ average with strong production/security depth.

---

## 2. Fundamentals Rubric

| Score | Evidence |
|---:|---|
| 1 | cannot explain logical execution order |
| 2 | knows clauses but makes join/NULL mistakes |
| 3 | explains joins, grouping, NULLs, CTEs, set operations |
| 4 | catches output grain, join multiplication, anti-join, pagination traps |
| 5 | explains correctness and query shape under changing requirements |

Must-have topics:

- logical execution order
- output grain
- joins
- aggregation
- NULL behavior
- set operations

---

## 3. Query Coding Rubric

| Score | Evidence |
|---:|---|
| 1 | cannot solve common interview patterns |
| 2 | memorizes patterns but breaks on variants |
| 3 | solves latest row, top-N, duplicates, running totals, anti-joins |
| 4 | handles ties, missing data, expected outputs, and indexes |
| 5 | solves complex analytics and explains alternatives cleanly |

Must-have patterns:

- latest row per group
- top-N per group
- month-over-month
- gaps/islands
- overlapping ranges
- retention
- funnel
- percentiles

---

## 4. Advanced Analytics Rubric

| Score | Evidence |
|---:|---|
| 1 | only knows basic windows |
| 2 | uses windows but ignores frames/ties/missing periods |
| 3 | handles `LAG`, `ROW_NUMBER`, moving averages, date spine |
| 4 | handles `LATERAL`, recursive CTEs, percentiles, grouping sets, rollups |
| 5 | designs analytics queries and read models with correctness/performance trade-offs |

Must-have topics:

- window frames
- date spine
- `LATERAL`
- recursive CTEs
- `GROUPING SETS`/`ROLLUP`/`CUBE`
- materialized views

---

## 5. Indexing And EXPLAIN Rubric

| Score | Evidence |
|---:|---|
| 1 | says "add index" without plan evidence |
| 2 | knows basic indexes but not composite order/sargability |
| 3 | designs simple indexes and reads common EXPLAIN terms |
| 4 | reasons about row estimates, join types, buffers, pagination, write cost |
| 5 | debugs production slow queries with evidence and trade-offs |

Must-have topics:

- B-tree indexes
- composite index order
- covering/partial/functional indexes
- sargability
- EXPLAIN ANALYZE
- buffers/temp spill
- stale stats
- keyset pagination

---

## 6. PostgreSQL Production Ops Rubric

| Score | Evidence |
|---:|---|
| 1 | only checks query text |
| 2 | knows EXPLAIN but not MVCC/VACUUM/pool pressure |
| 3 | explains MVCC, VACUUM, ANALYZE, pool basics |
| 4 | debugs bloat, lock waits, stale stats, replica lag, partitioning decisions |
| 5 | runs DB incidents with mitigation-first production judgment |

Must-have topics:

- MVCC
- dead tuples/bloat
- VACUUM/autovacuum
- ANALYZE/statistics
- GIN/BRIN basics
- partitioning
- read replicas/lag
- connection pool pressure
- lock blockers

---

## 7. Transactions Concurrency Rubric

| Score | Evidence |
|---:|---|
| 1 | relies on application checks only |
| 2 | knows ACID but misses race conditions |
| 3 | explains isolation, locks, unique constraints, idempotency |
| 4 | designs double-booking/payment correctness with transactions and constraints |
| 5 | handles deadlocks, retry strategy, outbox, lock scope, high concurrency trade-offs |

Must-have topics:

- ACID
- isolation anomalies
- pessimistic/optimistic locking
- unique constraints
- idempotency table
- deadlocks
- outbox

---

## 8. Data Modeling Rubric

| Score | Evidence |
|---:|---|
| 1 | creates tables without constraints or lifecycle thinking |
| 2 | basic normalization but weak history/concurrency |
| 3 | models OLTP entities with keys, FKs, constraints, indexes |
| 4 | adds audit/status history, idempotency, tenant design, analytics schema |
| 5 | designs for correctness, evolution, reporting, security, and operations |

Must-have topics:

- OLTP vs OLAP
- normalization/denormalization
- constraints
- status history/audit
- idempotency
- star schema
- SCD
- soft delete
- multitenancy

---

## 9. Security Governance Rubric

| Score | Evidence |
|---:|---|
| 1 | unaware of SQL injection or tenant leak risks |
| 2 | knows prepared statements but not dynamic SQL/roles |
| 3 | uses parameters, allowlists, least privilege, tenant filters |
| 4 | designs tenant-aware constraints/indexes, RLS/views, PII masking, audit |
| 5 | treats database as trust boundary with layered controls and governance |

Must-have topics:

- SQL injection
- prepared statements
- dynamic SQL allowlists
- least privilege
- runtime vs migration roles
- tenant isolation
- RLS
- PII masking/audit

---

## 10. Self-Assessment Sheet

Fill after each mock:

| Area | Score | Evidence | Red Gap | Next Drill |
|---|---:|---|---|---|
| Fundamentals |  |  |  |  |
| Query Coding |  |  |  |  |
| Advanced Analytics |  |  |  |  |
| Indexing/EXPLAIN |  |  |  |  |
| PostgreSQL Ops |  |  |  |  |
| Transactions/Concurrency |  |  |  |  |
| Data Modeling |  |  |  |  |
| Security/Governance |  |  |  |  |
| Capstone Delivery |  |  |  |  |

---

## 11. MAANG Readiness Gate

You are ready when:

1. Average score is 4.3 or higher.
2. No category is below 4.
3. You can solve 10 query patterns without notes.
4. You can debug slow SQL with EXPLAIN and production metrics.
5. You can design schemas with constraints, history, idempotency, and tenant safety.
6. You can explain MVCC, VACUUM, partitioning, replica lag, and pool pressure.
7. You can handle SQL injection, grants, PII, masking, audit, and RLS questions.
8. You can deliver the hotel booking SQL capstone end to end.
