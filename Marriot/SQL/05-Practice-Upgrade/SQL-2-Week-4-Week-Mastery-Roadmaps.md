# SQL 2-Week And 4-Week Mastery Roadmaps

> Track: SQL Interview Track - Practice Upgrade  
> Goal: turn the SQL folder into a concrete study plan from fundamentals to MAANG-level readiness.

Use 2 weeks for interview sprint. Use 4 weeks for deeper mastery.

---

## 1. Daily Study Loop

Every study day:

1. Read one concept section.
2. Solve 5-10 SQL prompts without notes.
3. Explain one query with grain, correctness, and index notes.
4. Do one scenario or mini-lab.
5. Mark Red/Yellow/Green.

Time split:

| Activity | Sprint | Deep Plan |
|---|---:|---:|
| Reading | 25% | 30% |
| Query writing | 35% | 30% |
| Scenario/lab | 25% | 25% |
| Mock/rubric | 15% | 15% |

---

## 2. Two-Week Interview Sprint

Best for:

- already know SQL basics
- preparing for interviews soon
- need high-yield query and backend SQL readiness

### Week 1: Query Mastery

| Day | Focus | Practice |
|---:|---|---|
| 1 | execution order, grain, joins, NULLs | fundamentals recall + 10 basic queries |
| 2 | aggregation, HAVING, anti-joins, join multiplication | wrong-query clinic + anti-join drills |
| 3 | windows: latest row, top-N, running total | query coding patterns mock |
| 4 | CTEs, recursive CTEs, gaps/islands | recursive and streak labs |
| 5 | advanced analytics: retention, funnel, percentiles | retention/funnel labs |
| 6 | grouping sets, rollup, cube, LATERAL | advanced analytics scenario drills |
| 7 | review Red gaps | 90-minute query mock |

### Week 2: Senior Backend SQL

| Day | Focus | Practice |
|---:|---|---|
| 8 | indexing, sargability, composite indexes | slow orders API lab |
| 9 | EXPLAIN ANALYZE, buffers, stale stats | EXPLAIN walkthrough lab |
| 10 | transactions, locks, deadlocks, idempotency | double booking + payment idempotency labs |
| 11 | data modeling OLTP/OLAP | hotel booking schema mini-lab |
| 12 | PostgreSQL ops: MVCC, VACUUM, partitioning | bloat/partitioning runbook labs |
| 13 | security/governance: injection, tenant isolation, PII | tenant-safe schema + masked view labs |
| 14 | capstone | full mock + scoring rubric |

Two-week pass gate:

- solve 50+ SQL prompts
- complete 8+ mini-labs
- score 4+ in query coding, indexing, transactions, modeling
- explain slow-query and tenant-leak scenarios aloud

---

## 3. Four-Week Mastery Plan

Best for:

- MAANG-level SQL prep
- senior backend/database-heavy rounds
- production ownership depth

### Week 1: Fundamentals And Query Patterns

| Day | Focus | Deliverable |
|---:|---|---|
| 1 | grain, execution order, SELECT/WHERE/ORDER | query explanation notes |
| 2 | joins and NULL traps | join/anti-join drills |
| 3 | grouping/HAVING/conditional aggregation | aggregation drill set |
| 4 | subqueries, EXISTS/IN, set operations | membership query drills |
| 5 | CTEs and query decomposition | multi-CTE query lab |
| 6 | wrong-query clinic | fix 10 broken queries |
| 7 | mock day | fundamentals + pattern mock |

### Week 2: Analytics And Advanced SQL

| Day | Focus | Deliverable |
|---:|---|---|
| 8 | window functions | latest/top-N/running total drills |
| 9 | window frames and moving averages | moving average lab |
| 10 | gaps/islands and overlapping ranges | streak/range labs |
| 11 | date spine, retention, funnel | retention/funnel labs |
| 12 | percentiles, rollups, grouping sets | advanced analytics drills |
| 13 | LATERAL and recursive CTEs | latest 3 orders + org tree labs |
| 14 | mock day | advanced analytics mock |

### Week 3: Performance Correctness Modeling

| Day | Focus | Deliverable |
|---:|---|---|
| 15 | index fundamentals | index design table |
| 16 | composite/covering/partial/functional indexes | orders API index lab |
| 17 | EXPLAIN ANALYZE and buffers | EXPLAIN walkthrough |
| 18 | transactions/isolation/locks | double-booking lab |
| 19 | idempotency/deadlocks/outbox | payment idempotency lab |
| 20 | OLTP data modeling | orders/payments schema |
| 21 | mock day | performance + concurrency mock |

### Week 4: Production Governance Capstone

| Day | Focus | Deliverable |
|---:|---|---|
| 22 | MVCC, VACUUM, bloat, stale stats | production ops runbook |
| 23 | partitioning, materialized views, read replicas | reporting architecture memo |
| 24 | connection pools, lock waits, slow SQL incidents | incident drill |
| 25 | SQL injection, grants, roles | secure search lab |
| 26 | tenant isolation, RLS, PII masking/audit | tenant-safe schema lab |
| 27 | full hotel booking SQL capstone | schema + queries + runbooks |
| 28 | final mock + rubric | MAANG readiness scorecard |

Four-week pass gate:

- solve 100+ SQL prompts
- complete 15+ mini-labs
- run at least 4 mock interviews
- score 4.3 average across rubrics
- deliver capstone without notes

---

## 4. Topic Priority Matrix

| Priority | Topic | Why It Matters |
|---|---|---|
| P0 | grain and query correctness | every SQL answer depends on this |
| P0 | joins/aggregation/windows | highest interview frequency |
| P0 | query patterns | coding round success |
| P0 | indexing/EXPLAIN | senior backend differentiator |
| P0 | transactions/concurrency | production correctness |
| P0 | data modeling | system design plus SQL hybrid rounds |
| P1 | advanced analytics | MAANG analytics depth |
| P1 | PostgreSQL ops | production ownership depth |
| P1 | security/tenant/PII | senior backend trust boundary |
| P2 | partitioning/materialized views | scale/reporting specialization |

---

## 5. Red Gap Repair Plan

| Red Gap | Repair Drill |
|---|---|
| grain unclear | before every query, write "one row per ..." |
| joins weak | solve anti-join, left join filter, join multiplication drills |
| windows weak | latest row/top-N/running total/gaps-islands daily |
| advanced analytics weak | retention/funnel/date-spine labs |
| indexes weak | map 10 API queries to composite indexes |
| EXPLAIN weak | annotate 5 plans: scan, rows, join, sort, buffers |
| transactions weak | double booking/idempotency/deadlock scenarios |
| modeling weak | design orders/payments/bookings/audit schemas |
| ops weak | write MVCC/VACUUM/bloat/lock-wait runbooks |
| security weak | secure search + tenant-safe schema + masked view labs |

---

## 6. Final Capstone Checklist

Before calling SQL complete, explain this system:

```text
Hotel booking SQL layer:
- customers, hotels, room inventory, bookings, payments, refunds
- status history and audit logs
- idempotency keys for retries
- transaction flow preventing double booking
- indexes for customer history, availability, admin search, payment lookup
- analytics star schema or materialized views for reporting
- partitioning/read replica decision for high-volume events
- SQL injection-safe search and sorting
- tenant isolation, RLS/tenant filters, PII masking and audit
- EXPLAIN slow-query runbook
- MVCC/VACUUM/bloat and lock-wait production runbooks
```

If you can design, query, optimize, secure, and debug this aloud, the SQL track is MAANG-ready.
