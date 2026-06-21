# SQL PostgreSQL Production Ops MVCC Vacuum Partitioning Runbooks Platinum Sheet

Target: senior backend and MAANG interviews where SQL performance is discussed like a production system, not only a query puzzle.

This sheet fills the PostgreSQL operations layer: MVCC, VACUUM, bloat, statistics, partitioning, index types, materialized views, read replicas, connection pools, slow-query runbooks, and incident response.

---

## 0. Production SQL Mindset

A database incident is rarely only one bad query.

```text
query shape -> plan -> rows read -> locks/waits -> pool pressure -> disk/cache -> replication -> user impact
```

Strong answer:

```text
I start with user impact and query evidence. Then I inspect EXPLAIN ANALYZE, lock waits,
connection pool metrics, table/index bloat, stale stats, replica lag, and recent deployments
before changing indexes or query shape.
```

---

# 1. MVCC Mental Model

PostgreSQL uses MVCC: Multi-Version Concurrency Control.

Core idea:

```text
Readers do not block writers, and writers do not block readers by creating row versions.
```

What happens on update:

1. Old row version remains visible to transactions that started earlier.
2. New row version is written.
3. Later transactions see the new version.
4. Old versions become dead tuples after no transaction can see them.
5. VACUUM removes dead tuples later.

Interview line:

```text
MVCC improves concurrency, but it creates dead tuples that must be cleaned by VACUUM.
```

---

# 2. Dead Tuples And Bloat

Dead tuples come from:

- UPDATE
- DELETE
- rolled-back writes
- high churn tables
- long-running transactions delaying cleanup

Bloat means table/index pages contain too much dead or unused space.

Symptoms:

- table scans read more pages than expected
- index scans become slower
- disk usage grows faster than live rows
- VACUUM cannot keep up

Useful PostgreSQL checks:

```sql
SELECT
    relname,
    n_live_tup,
    n_dead_tup,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC
LIMIT 20;
```

---

# 3. VACUUM And Autovacuum

VACUUM removes dead tuples and helps prevent transaction ID wraparound.

ANALYZE updates planner statistics.

Autovacuum does both automatically based on thresholds.

Manual commands:

```sql
VACUUM orders;
ANALYZE orders;
VACUUM ANALYZE orders;
```

Important warning:

```text
VACUUM FULL rewrites the table and takes a stronger lock. It is not a casual first response.
```

Strong answer:

```text
If performance changed because planner estimates are wrong, I consider ANALYZE. If table
churn is high, I inspect dead tuples and autovacuum behavior. I avoid VACUUM FULL during
traffic unless there is a planned maintenance window.
```

---

# 4. Stale Statistics

The planner chooses plans based on statistics.

Bad estimates cause:

- sequential scan when index scan would help
- wrong join order
- nested loop over too many rows
- wrong hash join memory assumptions

Evidence in `EXPLAIN ANALYZE`:

```text
estimated rows: 100
actual rows: 5,000,000
```

Fix path:

```sql
ANALYZE orders;
```

For correlated columns, consider extended statistics:

```sql
CREATE STATISTICS stats_orders_status_country
ON status, country
FROM orders;

ANALYZE orders;
```

Interview line:

```text
When estimates are far from actual rows, I suspect stale or insufficient statistics before
blindly adding indexes.
```

---

# 5. Index Types Worth Knowing

| Index Type | Strong For |
|---|---|
| B-tree | equality, range, sort, joins, uniqueness |
| GIN | JSONB containment, arrays, full-text search |
| GiST | geometric/range types, exclusion constraints |
| BRIN | very large naturally ordered tables, time-series append-only data |
| Hash | equality only, rarely the first interview choice |

B-tree remains the default answer for most backend APIs.

---

# 6. GIN Index For JSONB

Problem:

```text
Query users by JSONB preferences.
```

Index:

```sql
CREATE INDEX idx_users_preferences_gin
ON users
USING GIN (preferences);
```

Query:

```sql
SELECT user_id
FROM users
WHERE preferences @> '{"newsletter": true}'::jsonb;
```

Trap:

```text
Do not put frequently filtered relational fields only inside JSONB. Extract important fields
into typed columns when they are core query dimensions.
```

---

# 7. BRIN Index For Large Time-Series Tables

BRIN indexes summarize page ranges.

Good fit:

- huge append-only table
- rows naturally ordered by time
- queries filter by time ranges

Example:

```sql
CREATE INDEX idx_events_created_brin
ON events
USING BRIN (created_at);
```

Trade-off:

```text
BRIN is small and cheap, but less precise than B-tree. It helps skip large page ranges rather
than find exact rows.
```

---

# 8. Partitioning

Partitioning splits one logical table into physical child tables.

Useful for:

- very large time-based tables
- retention/drop old data
- partition pruning
- reducing maintenance scope

Range partition example:

```sql
CREATE TABLE events (
    event_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    event_name TEXT NOT NULL
) PARTITION BY RANGE (created_at);

CREATE TABLE events_2026_01 PARTITION OF events
FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

Query that prunes partitions:

```sql
SELECT COUNT(*)
FROM events
WHERE created_at >= TIMESTAMP '2026-01-01'
  AND created_at < TIMESTAMP '2026-02-01';
```

Trap:

```text
Partitioning is not a magic speed button. Queries must filter on the partition key to benefit.
```

---

# 9. Partitioning Design Checklist

Ask:

1. Is the table actually large enough?
2. Is there a natural partition key?
3. Do most queries filter by that key?
4. How many partitions will exist?
5. How will new partitions be created?
6. How will old partitions be archived/dropped?
7. Do indexes exist per partition?
8. Are unique constraints compatible with partition key?

Strong answer:

```text
I partition large event/order-history tables by time when retention and time-bounded queries
matter. I avoid partitioning small OLTP tables just because they are important.
```

---

# 10. Materialized Views In Production

Materialized view use cases:

- dashboard summaries
- expensive repeated joins/aggregations
- reports that tolerate staleness

Create:

```sql
CREATE MATERIALIZED VIEW daily_revenue_mv AS
SELECT
    created_at::date AS revenue_date,
    country,
    SUM(total_amount) AS revenue,
    COUNT(*) AS order_count
FROM orders
GROUP BY created_at::date, country;
```

Refresh:

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_revenue_mv;
```

Requirement for concurrent refresh:

```sql
CREATE UNIQUE INDEX idx_daily_revenue_mv_unique
ON daily_revenue_mv (revenue_date, country);
```

Interview line:

```text
The trade-off is freshness versus read performance. I would publish freshness metadata and
monitor refresh duration/failure.
```

---

# 11. Read Replicas And Replica Lag

Read replicas help scale reads and isolate analytics from OLTP writes.

Risks:

- stale reads
- replica lag after write spikes
- read-after-write inconsistency
- slow analytics hurting replica capacity
- failover complexity

Safe usage:

```text
Use primary for read-after-write critical flows; use replicas for dashboards, search, and
less freshness-sensitive reads.
```

Interview line:

```text
Read replicas scale reads, but they do not remove the need to optimize query shape. They add
freshness and routing concerns.
```

---

# 12. Connection Pool Pressure

Symptoms:

- API latency spikes
- Hikari/connection pool pending threads rise
- database active sessions high
- many sessions idle in transaction
- queries wait for locks or CPU/I/O

Useful PostgreSQL query:

```sql
SELECT
    state,
    wait_event_type,
    wait_event,
    COUNT(*)
FROM pg_stat_activity
GROUP BY state, wait_event_type, wait_event
ORDER BY COUNT(*) DESC;
```

Find long transactions:

```sql
SELECT
    pid,
    state,
    now() - xact_start AS transaction_age,
    query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
ORDER BY transaction_age DESC
LIMIT 20;
```

Strong answer:

```text
I do not blindly increase pool size. I check query latency, locks, long transactions, DB CPU,
I/O, and whether the app is holding connections while doing external work.
```

---

# 13. Lock Wait Runbook

Symptoms:

- queries stuck
- high latency on writes
- deadlocks
- migrations hanging

Find blocking:

```sql
SELECT
    blocked.pid AS blocked_pid,
    blocked.query AS blocked_query,
    blocking.pid AS blocking_pid,
    blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks
  ON blocked_locks.pid = blocked.pid
JOIN pg_locks blocking_locks
  ON blocking_locks.locktype = blocked_locks.locktype
 AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
 AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
 AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
 AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
 AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
 AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
 AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
 AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
 AND blocking_locks.pid <> blocked_locks.pid
JOIN pg_stat_activity blocking
  ON blocking.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted
  AND blocking_locks.granted;
```

Mitigation:

- identify blocker
- assess if blocker can be cancelled
- pause rollout/migration if related
- shorten transactions
- add missing index if lock scope is widened by scans

---

# 14. Slow Query Runbook

First 10 minutes:

1. Identify affected endpoint/job/dashboard.
2. Check deploy/migration/config changes.
3. Get exact SQL and bind parameters shape.
4. Run `EXPLAIN (ANALYZE, BUFFERS)` safely on representative environment.
5. Compare estimated vs actual rows.
6. Inspect scan type and join strategy.
7. Check rows removed by filter.
8. Check sort/hash memory pressure.
9. Check locks/waits and connection pool pressure.
10. Mitigate with rollback, feature flag, query limit, index, or traffic shift.

Example:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM orders
WHERE customer_id = 42
ORDER BY created_at DESC
LIMIT 20;
```

Strong answer:

```text
I optimize evidence, not vibes. I look at actual rows, buffer reads, join strategy, sort cost,
lock waits, and whether the query shape matches existing indexes.
```

---

# 15. EXPLAIN BUFFERS Signals

Useful signals:

| Signal | Meaning |
|---|---|
| high shared read blocks | reading from disk/cache miss |
| high shared hit blocks | many buffers touched, maybe too many rows |
| temp read/write | sort/hash spilled to disk |
| actual rows far above estimate | stale stats or bad selectivity estimate |
| loops high | nested operation repeated many times |

Interview line:

```text
`BUFFERS` helps separate CPU/query-shape problems from I/O-heavy plans.
```

---

# 16. Migration Safety Runbook

Dangerous migration patterns:

- adding NOT NULL with full table scan on large table
- adding default that rewrites old versions on older databases
- creating index without concurrent mode during traffic
- dropping columns before old app versions are gone
- long transaction wrapping many DDL statements

Safer patterns:

```sql
CREATE INDEX CONCURRENTLY idx_orders_customer_created
ON orders (customer_id, created_at DESC);
```

Expand-contract flow:

1. Add nullable column/table/index.
2. Deploy app writing old and new shape.
3. Backfill in batches.
4. Deploy app reading new shape.
5. Add constraints after validation.
6. Drop old shape later.

---

# 17. Production Dashboard For Database Health

Useful panels:

- query p95/p99 by route/job
- database CPU/I/O
- active connections
- pool active/idle/pending
- lock waits
- deadlocks
- slow query count
- replica lag
- transaction age
- dead tuples/autovacuum recency
- table/index size growth
- cache hit ratio

Alert examples:

```text
Connection pool pending > 0 for 5 minutes
Deadlocks > 0 in 10 minutes
Replica lag > business freshness threshold
Oldest transaction age > 10 minutes for OLTP service
```

---

# 18. Common Production Mistakes

| Mistake | Better Approach |
|---|---|
| Add index without checking plan | inspect query shape and EXPLAIN |
| Increase connection pool blindly | find slow queries/locks/DB saturation |
| Run heavy analytics on primary | use replica/warehouse/materialized view |
| Ignore stale stats | run/analyze and inspect estimates |
| Use OFFSET for deep pages | keyset pagination |
| Add partitions to small table | partition only with clear query/retention benefit |
| Use JSONB for core query fields | typed columns plus JSONB for flexible attributes |
| Let transactions stay open during network calls | keep DB transactions short |

---

# 19. Interview Scenario: Slow Orders API

Prompt:

```text
Orders API went from 200 ms to 5 seconds after traffic growth.
```

Strong answer:

```text
I would first confirm endpoint scope and recent changes. Then I would capture the query and
run EXPLAIN ANALYZE with BUFFERS. I would check whether the plan reads too many rows, uses a
sequential scan, sorts too much, has bad estimates, or waits on locks. I would inspect pool
pending threads and pg_stat_activity for long transactions. If the endpoint uses OFFSET
pagination, I would move to keyset pagination and support it with an index like
(customer_id, created_at desc, order_id desc). If analytics are hitting the primary, I would
move them to a replica or summary table.
```

---

# 20. Final Rapid Revision

- MVCC creates row versions.
- VACUUM cleans dead tuples.
- ANALYZE updates planner stats.
- Bad estimates cause bad plans.
- B-tree is default; GIN for JSONB/arrays/text; BRIN for huge ordered time tables.
- Partitioning helps when queries and retention align with partition key.
- Materialized views trade freshness for speed.
- Read replicas scale reads but introduce lag.
- Connection pool exhaustion is usually symptom, not root cause.
- Slow-query debugging starts with evidence: plan, rows, buffers, locks, waits.

---

# 21. Official Source Notes

- PostgreSQL MVCC: https://www.postgresql.org/docs/current/mvcc.html
- PostgreSQL Routine Vacuuming: https://www.postgresql.org/docs/current/routine-vacuuming.html
- PostgreSQL EXPLAIN: https://www.postgresql.org/docs/current/using-explain.html
- PostgreSQL Partitioning: https://www.postgresql.org/docs/current/ddl-partitioning.html
- PostgreSQL Index Types: https://www.postgresql.org/docs/current/indexes-types.html
