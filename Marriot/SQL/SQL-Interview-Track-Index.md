# SQL Interview Track Index

Target: starter, intermediate, senior backend, analytics, and MAANG-style SQL interviews.

This folder is organized as a complete SQL learning and revision track. The main story-mode
sheet teaches SQL end to end. The platinum sheets isolate the areas that usually decide
senior interview outcomes: performance, concurrency, advanced query writing, and schema
design.

---

## 1. Recommended Study Order

| Order | Document | Why This Order |
|---:|---|---|
| 1 | `01-Starter-Path/SQL-Query-Mastery-Story-Mode.md` | End-to-end SQL fundamentals, joins, aggregation, windows, indexes, transactions |
| 2 | `02-Intermediate-Query-Drills/SQL-Advanced-Query-Drills-Windows-CTE-Analytics-Platinum-Sheet.md` | Fast practice for windows, CTEs, analytics, gaps/islands, latest row, top-N |
| 3 | `03-Senior-Performance-Concurrency/SQL-Performance-Indexing-Explain-Platinum-Sheet.md` | EXPLAIN, indexes, sargability, pagination, slow-query debugging |
| 4 | `03-Senior-Performance-Concurrency/SQL-Transactions-Locking-Concurrency-Platinum-Sheet.md` | ACID, isolation, locks, deadlocks, idempotency, backend correctness |
| 5 | `04-MAANG-Data-Modeling-Case-Studies/SQL-Data-Modeling-Backend-Analytics-Case-Studies-Platinum-Sheet.md` | OLTP schema design, constraints, audit history, star schema, backend cases |

---

## 2. Level-Wise Learning Plan

### Starter Path

Focus:

- SELECT, WHERE, ORDER BY
- joins
- GROUP BY and HAVING
- NULL behavior
- basic subqueries

Starter goal:

```text
I can write correct SQL for filters, joins, grouping, and simple interview questions.
```

### Intermediate Path

Add:

- window functions
- CTEs
- EXISTS vs IN
- top-N per group
- duplicate detection
- running totals
- query pattern playbook

Intermediate goal:

```text
I can solve common product analytics and backend query problems without memorizing random
solutions.
```

### Senior Backend Path

Add:

- indexes
- EXPLAIN ANALYZE
- sargability
- transactions
- isolation levels
- locks and deadlocks
- idempotent inserts
- schema constraints

Senior goal:

```text
I can design queries and schemas that stay correct under concurrency and fast under real
data volume.
```

### MAANG-Ready Path

Practice:

- state output grain before writing SQL
- write query
- explain why it is correct
- discuss index and execution plan
- discuss concurrency correctness
- discuss schema constraints
- mention alternative approach

MAANG goal:

```text
I can solve SQL problems like a backend owner: correctness first, performance second, and
concurrency safety always visible.
```

---

## 3. SQL Answer Formula

Use this structure:

```text
1. Define output grain.
2. Identify source tables and joins.
3. Filter rows before grouping.
4. Aggregate/window at the right level.
5. Handle ties and NULLs.
6. Add ordering/pagination.
7. Mention indexes/constraints/concurrency if relevant.
```

Example:

```text
The output grain is one row per customer per month. I first aggregate orders by customer and
month, then use LAG over each customer to compare with the previous month. For performance,
I would index customer_id and order_date and validate with EXPLAIN ANALYZE.
```

---

## 4. Platinum Coverage Map

| Skill | Covered By |
|---|---|
| SQL basics | Story mode |
| joins and NULLs | Story mode |
| aggregation | Story mode |
| window functions | Story mode and advanced drills |
| CTEs | Story mode and advanced drills |
| query patterns | Story mode and advanced drills |
| indexing | Story mode and performance sheet |
| EXPLAIN | Story mode and performance sheet |
| slow query debugging | Performance sheet |
| transaction correctness | Story mode and concurrency sheet |
| locking/deadlocks | Concurrency sheet |
| idempotency | Story mode and concurrency sheet |
| OLTP modeling | Data modeling sheet |
| analytics/star schema | Story mode and data modeling sheet |
| backend case studies | Story mode and data modeling sheet |

---

## 5. One-Day Revision Plan

First 60 minutes:

- read command center and query-solving framework in story mode
- revise execution order, joins, GROUP BY, HAVING, NULL

Next 60 minutes:

- solve advanced query drills
- focus on windows, top-N, latest row, gaps/islands

Next 45 minutes:

- revise performance sheet
- practice EXPLAIN answer template and index design

Next 45 minutes:

- revise concurrency sheet
- practice double booking, deadlock, idempotency answers

Final 30 minutes:

- revise data modeling cases
- practice schema design for orders, bookings, audit logs, payments

---

## 6. Final Completeness Statement

This SQL track now covers:

- beginner query writing
- intermediate analytics queries
- advanced window/CTE patterns
- query debugging and indexing
- transactions and locking
- backend correctness patterns
- schema design and data modeling
- MAANG-style explanation quality

The main rule:

```text
Before writing SQL, know the grain. Before optimizing SQL, know the plan. Before trusting
SQL under concurrency, know the constraint.
```

---

## 7. Official Source Notes

- PostgreSQL EXPLAIN: https://www.postgresql.org/docs/current/using-explain.html
- PostgreSQL indexes: https://www.postgresql.org/docs/current/indexes.html
- PostgreSQL transaction isolation: https://www.postgresql.org/docs/current/transaction-iso.html
