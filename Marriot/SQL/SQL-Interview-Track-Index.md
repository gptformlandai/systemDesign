# SQL Interview Track Index

Target: starter, intermediate, senior backend, analytics, PostgreSQL production, security, and MAANG-style SQL interviews.

This folder is organized as a complete SQL learning and interview track. The goal is not only
to solve query puzzles. The goal is to write correct SQL, explain output grain, optimize with
evidence, protect data under concurrency, model schemas cleanly, secure tenant/PII access, and
debug database behavior in production.

Current structure:

- 14 topic/practice sheets plus this root index
- 5 learning layers from starter to practice upgrade
- coverage for query writing, analytics, indexing, PostgreSQL operations, concurrency, modeling, security, and active recall

---

## 1. Recommended Study Order

| Order | Document | Why This Order |
|---:|---|---|
| 1 | `01-Starter-Path/SQL-Query-Mastery-Story-Mode.md` | Learn end-to-end SQL fundamentals, joins, aggregation, windows, CTEs, indexing, EXPLAIN, transactions, PostgreSQL features, wrong-query clinic, and capstones |
| 2 | `02-Intermediate-Query-Drills/SQL-Advanced-Query-Drills-Windows-CTE-Analytics-Platinum-Sheet.md` | Practice common interview patterns: latest row, top-N, running totals, retention, gaps/islands, overlapping ranges, duplicates, anti-joins, and conditional aggregation |
| 3 | `02-Intermediate-Query-Drills/SQL-Advanced-Analytics-Window-Recursive-Lateral-Patterns-Platinum-Sheet.md` | Add advanced analytics patterns: window frames, date spine, percentiles, grouping sets, rollup, cube, lateral joins, recursive CTEs, materialized views, cohort, and funnel queries |
| 4 | `03-Senior-Performance-Concurrency/SQL-Performance-Indexing-Explain-Platinum-Sheet.md` | Learn EXPLAIN, indexes, sargability, pagination, joins, aggregation, slow-query debugging, and index trade-offs |
| 5 | `03-Senior-Performance-Concurrency/SQL-Transactions-Locking-Concurrency-Platinum-Sheet.md` | Learn ACID, isolation, locks, deadlocks, idempotency, double booking, payment timeout, and outbox correctness |
| 6 | `03-Senior-Performance-Concurrency/SQL-PostgreSQL-Production-Ops-MVCC-Vacuum-Partitioning-Runbooks-Platinum-Sheet.md` | Add PostgreSQL production depth: MVCC, VACUUM, bloat, stale stats, GIN/BRIN, partitioning, materialized views, replicas, pools, locks, and runbooks |
| 7 | `03-Senior-Performance-Concurrency/SQL-Security-Governance-Tenant-Isolation-Gold-Sheet.md` | Add SQL security and governance: injection defense, prepared statements, grants, least privilege, RLS, tenant isolation, PII masking, encryption, and audit |
| 8 | `04-MAANG-Data-Modeling-Case-Studies/SQL-Data-Modeling-Backend-Analytics-Case-Studies-Platinum-Sheet.md` | Learn OLTP schema design, constraints, audit history, idempotency, catalog modeling, star schema, SCD, soft delete, multitenancy, and backend cases |
| 9 | `05-Practice-Upgrade/SQL-Active-Recall-Question-Bank.md` | Convert notes into retrieval memory across fundamentals, windows, CTEs, indexing, PostgreSQL ops, transactions, modeling, and security |
| 10 | `05-Practice-Upgrade/SQL-Scenario-Drill-Bank.md` | Practice realistic query, performance, concurrency, modeling, security, and MAANG capstone scenarios |
| 11 | `05-Practice-Upgrade/SQL-Coding-Design-Mini-Labs.md` | Build query, schema, index, EXPLAIN, MVCC, partitioning, idempotency, tenant, PII, and capstone mini-labs |
| 12 | `05-Practice-Upgrade/SQL-Mock-Interview-Scripts.md` | Run simulated SQL rounds from fundamentals to MAANG production/database depth |
| 13 | `05-Practice-Upgrade/SQL-Interview-Scoring-Rubrics.md` | Score readiness by fundamentals, coding, analytics, EXPLAIN, ops, concurrency, modeling, security, and capstone delivery |
| 14 | `05-Practice-Upgrade/SQL-2-Week-4-Week-Mastery-Roadmaps.md` | Follow a 2-week sprint or 4-week mastery plan with daily query, lab, and mock loops |

---

## 2. What Each Layer Builds

### 01-Starter-Path

This layer builds SQL fluency.

You should be able to explain and write:

- SELECT, WHERE, ORDER BY, DISTINCT, LIMIT/OFFSET
- joins, NULL behavior, anti-joins, self joins, set operations
- GROUP BY, HAVING, aggregate functions, conditional aggregation
- subqueries, EXISTS vs IN, CTEs, recursive CTE basics
- windows, ranking, running totals, top-N per group
- indexing, EXPLAIN basics, transactions, normalization, PostgreSQL features
- wrong-query clinic, EXPLAIN walkthroughs, schema mini-cases, and capstones

### 02-Intermediate-Query-Drills

This layer builds query-pattern speed.

You should be able to solve:

- latest row per group
- top-N per group
- running totals and moving averages
- month-over-month growth
- retention and funnels
- gaps and islands
- overlapping date ranges
- duplicate detection and safe deletion
- conditional pivots
- window frames, date spine, percentiles, grouping sets, rollup, cube
- LATERAL joins and recursive CTE hierarchy queries

### 03-Senior-Performance-Concurrency

This layer builds backend ownership.

You should be able to explain and debug:

- B-tree, composite, covering, partial, functional, GIN, BRIN indexes
- sargability and deep pagination
- EXPLAIN ANALYZE, row estimates, join strategies, buffers, temp spills
- ACID, isolation anomalies, locks, deadlocks, lock scope, and idempotency
- double booking, payment retries, and outbox correctness
- PostgreSQL MVCC, VACUUM, bloat, autovacuum, stale stats, partitioning, materialized views
- read replicas, replica lag, connection pool pressure, lock waits, and slow-query runbooks
- SQL injection, dynamic SQL allowlists, grants, roles, RLS, tenant isolation, PII masking, and audit

### 04-MAANG-Data-Modeling-Case-Studies

This layer builds schema and product-system judgment.

You should be able to design:

- OLTP schemas for orders, bookings, payments, refunds, catalog, status history, and idempotency
- constraints that make invalid states hard to store
- indexes based on API access patterns
- audit and append-only history
- OLAP/star schema and slowly changing dimensions
- soft delete and retention behavior
- tenant-aware schema designs

### 05-Practice-Upgrade

This layer turns SQL knowledge into interview performance.

You should use it to:

- answer active recall without notes
- solve timed SQL prompts
- run scenario drills under follow-up pressure
- build design/coding mini-labs
- simulate mock interviews
- score yourself with rubrics
- follow 2-week or 4-week study plans

---

## 3. Level-Wise Learning Plan

### Starter Path

Focus:

- logical execution order
- output grain
- filters, joins, grouping, HAVING
- NULL behavior
- simple subqueries and CTEs

Starter goal:

```text
I can write correct SQL for filters, joins, grouping, and basic interview questions, and I
can explain what one output row represents before writing the query.
```

### Intermediate Path

Add:

- window functions
- CTEs and query decomposition
- top-N per group
- latest row per group
- duplicate detection
- gaps/islands
- retention/funnel analytics
- date spine and missing-period handling

Intermediate goal:

```text
I can solve common backend and product analytics SQL problems without memorizing random
solutions, because I understand grain, windows, grouping, and edge cases.
```

### Senior Backend Path

Add:

- indexes and sargability
- EXPLAIN ANALYZE and BUFFERS
- transactions, isolation, locks, deadlocks
- idempotency and outbox
- schema constraints
- PostgreSQL MVCC/VACUUM/stats/partitioning
- tenant isolation and SQL security

Senior goal:

```text
I can design queries and schemas that stay correct under concurrency, fast under real data
volume, secure under tenant/PII constraints, and debuggable in production.
```

### MAANG-Ready Path

Practice:

- state output grain before writing SQL
- write query and explain why it is correct
- handle ties, NULLs, missing dates, and duplicates
- discuss index and execution plan
- discuss concurrency correctness and constraints
- discuss schema design and lifecycle
- discuss production database behavior
- discuss tenant/security/governance risks
- mention alternatives and trade-offs

MAANG goal:

```text
I can solve SQL problems like a backend owner: correctness first, performance with evidence,
concurrency safety through constraints, and production/security risks always visible.
```

---

## 4. SQL Answer Formula

Use this structure for query questions:

```text
1. Define output grain.
2. Identify source tables and joins.
3. Filter rows before grouping.
4. Aggregate or window at the right level.
5. Handle ties, NULLs, duplicates, and missing periods.
6. Add ordering and pagination.
7. Mention indexes and EXPLAIN plan if relevant.
8. Mention constraints/concurrency/security if relevant.
```

Example:

```text
The output grain is one row per customer per month. I aggregate orders to customer-month
first, then use LAG over each customer to compare with the previous month. I handle missing
months with a date spine if the dashboard requires zero rows. For performance, I index
customer_id and order_date and validate the plan with EXPLAIN ANALYZE.
```

Use this structure for slow-query questions:

```text
symptom -> exact SQL -> EXPLAIN ANALYZE/BUFFERS -> rows/plan/waits -> mitigation -> prevention
```

Use this structure for schema questions:

```text
entities -> constraints -> indexes -> history/audit -> transactions -> security -> analytics
```

---

## 5. Final Coverage Checklist

| Area | Covered By |
|---|---|
| SQL basics | story mode, active recall |
| output grain | story mode, all practice files |
| joins and NULLs | story mode, scenario drills |
| aggregation and HAVING | story mode, query drills |
| window functions | story mode, advanced drills, advanced analytics sheet |
| window frames | advanced analytics sheet, mini-labs |
| CTEs and recursive CTEs | story mode, advanced analytics sheet |
| LATERAL joins | advanced analytics sheet |
| date spine and retention | advanced analytics sheet, scenario drills, mini-labs |
| funnels and cohorts | advanced analytics sheet, practice layer |
| GROUPING SETS/ROLLUP/CUBE | advanced analytics sheet |
| percentiles/median | advanced analytics sheet |
| indexing | story mode, performance sheet, production ops sheet |
| EXPLAIN ANALYZE and BUFFERS | story mode, performance sheet, production ops sheet, labs |
| sargability | performance sheet, labs |
| keyset pagination | story mode, performance sheet, labs |
| MVCC/VACUUM/bloat/stats | production ops sheet |
| partitioning | production ops sheet, labs |
| GIN/BRIN indexes | production ops sheet |
| materialized views | advanced analytics sheet, production ops sheet, labs |
| read replicas and replica lag | production ops sheet |
| connection pool pressure | production ops sheet |
| transactions and locks | concurrency sheet, scenario drills |
| deadlocks and lock scope | concurrency sheet, production ops sheet |
| idempotency/outbox | concurrency sheet, data modeling sheet, labs |
| OLTP modeling | data modeling sheet, mini-labs |
| OLAP/star schema/SCD | data modeling sheet, roadmap |
| multitenancy | data modeling sheet, security sheet |
| SQL injection | security sheet, mini-labs |
| grants/roles/least privilege | security sheet |
| RLS/tenant isolation | security sheet, labs |
| PII masking/audit/governance | security sheet, labs |
| mock interviews and scoring | all `05-Practice-Upgrade` sheets |

---

## 6. Practice Path

After each concept layer, practice immediately.

| Stage | Practice |
|---|---|
| After Starter | answer fundamentals recall and fix wrong-query clinic prompts |
| After Intermediate | solve latest row, top-N, retention, funnel, gaps/islands, and overlap drills |
| After Senior | complete EXPLAIN, index, double-booking, idempotency, MVCC, partitioning, and security labs |
| After Modeling | design hotel booking, orders/payments, marketplace ledger, audit, and star-schema cases |
| Before interviews | run mock scripts and scoring rubrics, then follow 2-week/4-week roadmap gaps |

Minimum MAANG practice set:

1. 100+ active recall questions.
2. 25+ SQL query prompts.
3. 12+ scenario drills.
4. 10+ mini-labs.
5. 3+ mock interview loops.
6. One complete SQL capstone explanation without notes.

---

## 7. Gold Standard Audit Rubric

Use this rubric to judge whether a SQL topic is interview-ready.

| Quality Bar | What It Means |
|---|---|
| Grain clarity | Can state one output row before writing SQL |
| Query correctness | Handles joins, grouping, windows, NULLs, ties, duplicates |
| Pattern speed | Can solve common SQL prompts without notes |
| Performance maturity | Uses indexes and EXPLAIN evidence, not guesses |
| Concurrency awareness | Protects invariants with transactions and constraints |
| Modeling maturity | Designs schemas around lifecycle, history, and access patterns |
| Production debugging | Can reason about MVCC, VACUUM, bloat, locks, pools, replicas |
| Security awareness | Prevents injection, tenant leaks, and PII overexposure |
| Trade-off maturity | Can explain alternatives and when not to use a feature |
| Interview delivery | Can answer in crisp 60 to 120 second structure |

Gold rule:

```text
Before writing SQL, know the grain. Before optimizing SQL, know the plan. Before trusting SQL
under concurrency, know the constraint. Before exposing data, know the tenant and permission.
```

---

## 8. End-To-End Capstone Roadmap

Build one imaginary system while studying every sheet:

```text
Hotel Booking SQL Layer
```

Use each area like this:

| Area | Capstone Work |
|---|---|
| Fundamentals | booking/customer/payment queries with clear grain |
| Advanced query drills | latest status, top-N hotels, retention, funnel, overlap checks |
| Advanced analytics | cohorts, date spine, p95 latency/order value, rollups, materialized views |
| Indexing/EXPLAIN | API query indexes, keyset pagination, slow-query evidence |
| Transactions | double-booking prevention, payment idempotency, deadlock strategy |
| PostgreSQL ops | MVCC/VACUUM/bloat/stats/partitioning/replica/pool runbooks |
| Security | prepared statements, grants, tenant filters/RLS, PII masking, audit |
| Data modeling | OLTP schema, status history, idempotency, star schema, SCD, soft delete |
| Practice | recall, drills, labs, mocks, rubrics, 2-week/4-week roadmap |

Final capstone interview prompt:

```text
Design and operate the SQL layer for a hotel booking platform that supports customer booking,
room inventory, payment authorization, refunds, status history, idempotency, tenant isolation,
analytics dashboards, SQL security, and production slow-query debugging.
```

Strong answer must include:

- output grain for important queries
- normalized OLTP schema
- constraints and tenant-aware keys
- transaction flow for double-booking prevention
- payment idempotency
- status/audit history
- indexes for API access patterns
- EXPLAIN slow-query runbook
- MVCC/VACUUM/bloat/stats awareness
- partitioning/materialized view/read replica decisions
- SQL injection-safe dynamic search
- least-privilege roles and grants
- PII masking/audit/retention
- analytics star schema or read model

---

## 9. Final Completeness Statement

This track now covers the SQL knowledge expected across:

- entry-level SQL query interviews
- intermediate analytics SQL rounds
- senior backend database-performance discussions
- schema design and data modeling rounds
- PostgreSQL production ownership conversations
- SQL security, governance, and multitenancy reviews
- MAANG-style correctness, scale, failure, and operations checks

One-stop answer:

```text
Yes. This is now a one-stop SQL track for MAANG-level preparation. It covers SQL fundamentals,
joins, aggregation, windows, CTEs, advanced analytics, LATERAL, recursive CTEs, grouping sets,
rollups, percentiles, indexing, EXPLAIN, sargability, transactions, locking, idempotency,
PostgreSQL MVCC, VACUUM, bloat, stats, partitioning, materialized views, replicas, connection
pools, schema modeling, OLTP/OLAP, multitenancy, SQL injection defense, grants, RLS, PII
governance, scenario drills, mini-labs, mocks, rubrics, and 2-week/4-week mastery roadmaps.
```

Optional future add-ons only if a role explicitly needs them:

- vendor-specific MySQL/SQL Server/Oracle comparison sheet
- data warehouse-specific SQL: BigQuery, Snowflake, Redshift
- full runnable PostgreSQL Docker lab with seed data
- stored procedures/triggers deep dive

These are optional because the current track now covers the high-frequency SQL interview surface
and the senior backend production ownership depth expected in serious database-heavy rounds.

---

## 10. Official Source Notes

- PostgreSQL EXPLAIN: https://www.postgresql.org/docs/current/using-explain.html
- PostgreSQL indexes: https://www.postgresql.org/docs/current/indexes.html
- PostgreSQL transaction isolation: https://www.postgresql.org/docs/current/transaction-iso.html
- PostgreSQL MVCC: https://www.postgresql.org/docs/current/mvcc.html
- PostgreSQL routine vacuuming: https://www.postgresql.org/docs/current/routine-vacuuming.html
- PostgreSQL partitioning: https://www.postgresql.org/docs/current/ddl-partitioning.html
- PostgreSQL row security: https://www.postgresql.org/docs/current/ddl-rowsecurity.html
