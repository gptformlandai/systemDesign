# 36. Database Debugging: Slow Queries, Locks, Transactions, Pools

## Goal

Debug database-backed application failures: slow queries, lock waits, deadlocks, N+1 queries, transaction bugs, connection pool exhaustion, and migration regressions.

---

## First Split

When the app says "database is slow," split the problem:

```text
query execution slow
connection acquisition slow
transaction waiting on lock
network latency to DB
DB CPU/I/O saturated
pool leak in application
schema/index regression
N+1 query pattern
```

Different causes require different fixes.

---

## Evidence To Collect

```text
application:
  trace span duration
  SQL statement or normalized query
  connection pool active/idle/pending
  transaction duration
  request endpoint and version

database:
  active queries
  blocked queries
  lock wait graph
  CPU, memory, disk I/O
  rows scanned vs returned
  execution plan
  slow query log
```

---

## Slow Query Workflow

```text
1. Find slow DB span in trace or slow query log.
2. Extract normalized SQL and parameters shape.
3. Run EXPLAIN / EXPLAIN ANALYZE safely.
4. Compare estimated rows vs actual rows.
5. Check index usage.
6. Check rows scanned, sort/hash operations, joins.
7. Compare with previous deploy/schema.
8. Fix query, index, pagination, or data access pattern.
```

PostgreSQL:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE customer_id = 42 ORDER BY created_at DESC LIMIT 50;
```

MySQL:

```sql
EXPLAIN SELECT * FROM orders WHERE customer_id = 42 ORDER BY created_at DESC LIMIT 50;
```

---

## Lock Wait Debugging

Symptoms:

```text
simple UPDATE/INSERT is slow
DB CPU not high
queries waiting
transaction duration long
timeouts during peak traffic
```

PostgreSQL lock view:

```sql
SELECT
  blocked.pid AS blocked_pid,
  blocked.query AS blocked_query,
  blocking.pid AS blocking_pid,
  blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks ON blocked_locks.pid = blocked.pid
JOIN pg_locks blocking_locks
  ON blocking_locks.locktype = blocked_locks.locktype
 AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
 AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
 AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
 AND blocking_locks.pid != blocked_locks.pid
JOIN pg_stat_activity blocking ON blocking.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted AND blocking_locks.granted;
```

Fixes:

- shorten transactions
- move external calls outside transactions
- add proper index for update/delete predicate
- lock rows in consistent order
- avoid long migration locks during peak

---

## Connection Pool Exhaustion

Symptoms:

```text
application latency high
DB CPU normal
DB active connections at limit
app pool active=max
threads waiting for connection
connection acquisition timeout
```

Questions:

- Are connections returned in finally/try-with-resources?
- Are transactions hanging?
- Is pool size too small for workload?
- Is pool size too large for DB capacity?
- Did retry storm multiply DB calls?
- Are health checks consuming connections?

Java Hikari indicators:

```text
hikaricp.connections.active
hikaricp.connections.idle
hikaricp.connections.pending
hikaricp.connections.timeout
```

---

## N+1 Query Debugging

Pattern:

```text
GET /orders
  SELECT * FROM orders LIMIT 100
  SELECT * FROM customers WHERE id=?   -- repeated 100 times
  SELECT * FROM order_items WHERE order_id=? -- repeated 100 times
```

Symptoms:

- many identical DB spans in one trace
- latency grows with list size
- DB query count per request high

Fix:

- join/fetch join
- `select_related` / `prefetch_related`
- batch load
- cache small reference data
- pagination

---

## Transaction Isolation Bugs

| Symptom | Possible Cause |
|---|---|
| lost update | read-modify-write without locking/version check |
| duplicate processing | missing unique key/idempotency |
| stale read | replica lag or lower isolation |
| deadlock | inconsistent row lock order |
| phantom rows | range query under weaker isolation |

Fix patterns:

- optimistic locking version column
- unique constraints
- idempotency keys
- `SELECT ... FOR UPDATE` when appropriate
- consistent lock order
- transaction retry for deadlock victim

---

## Migration Debugging

Dangerous migrations:

- adding non-null column with default on huge table
- building index without concurrent/online mode
- changing column type with table rewrite
- backfilling too much in one transaction
- dropping columns before old app version is drained

Safe pattern:

```text
expand -> backfill -> dual write/read -> switch -> contract
```

---

## Practical Question

> Checkout latency increased, and traces show a simple `INSERT INTO orders` taking 5 seconds. DB CPU is not high. What do you check?

---

## Strong Answer

If a simple insert is slow but DB CPU is not high, I would suspect lock waits, connection pool exhaustion, disk I/O, or transaction contention rather than query complexity. I would inspect the DB span, active queries, lock wait graph, transaction duration, and app connection pool metrics. I would also check whether a migration or long transaction started at the same time.

If the insert is waiting on a lock, I would identify the blocking query and transaction owner. The fix is usually to shorten the transaction, move external calls outside the transaction, change lock ordering, or move the migration to an online/low-traffic path.

---

## Interview Sound Bite

Database debugging is not just "add an index." First separate query execution, lock wait, pool wait, transaction bugs, network latency, and DB saturation. A slow DB span tells you where to look; `EXPLAIN`, lock graphs, pool metrics, and transaction timing tell you why.
