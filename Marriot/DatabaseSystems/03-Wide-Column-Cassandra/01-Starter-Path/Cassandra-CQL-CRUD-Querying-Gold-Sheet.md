# Cassandra CQL CRUD and Querying - Gold Sheet

> Track File #4 of 25 - Group 01: Starter Path
> For: backend/database/system design interviews | Level: beginner to intermediate | Mode: CQL syntax, query limits, safe patterns

This sheet builds:
- CREATE KEYSPACE and CREATE TABLE basics
- INSERT, SELECT, UPDATE, DELETE, TTL, batches
- Why CQL looks like SQL but behaves differently

---

## 1. CQL Looks Like SQL, But It Is Not SQL

CQL intentionally resembles SQL, but Cassandra does not support relational joins, arbitrary filtering, or foreign-key constraints.

Core difference:

```text
SQL asks: what data do I want?
Cassandra asks: which partition and clustering range can serve this query efficiently?
```

---

## 2. Create A Query-Shaped Table

```sql
CREATE KEYSPACE IF NOT EXISTS app
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE app;

CREATE TABLE orders_by_customer_day (
  customer_id text,
  order_day date,
  created_at timestamp,
  order_id uuid,
  status text,
  total_cents bigint,
  PRIMARY KEY ((customer_id, order_day), created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC, order_id ASC);
```

Query this table by partition:

```sql
SELECT *
FROM orders_by_customer_day
WHERE customer_id = 'c1'
  AND order_day = '2026-07-01'
LIMIT 20;
```

---

## 3. Basic CRUD

```sql
INSERT INTO orders_by_customer_day (
  customer_id, order_day, created_at, order_id, status, total_cents
) VALUES (
  'c1', '2026-07-01', toTimestamp(now()), uuid(), 'PAID', 12999
);

SELECT *
FROM orders_by_customer_day
WHERE customer_id = 'c1' AND order_day = '2026-07-01';

UPDATE orders_by_customer_day
SET status = 'REFUNDED'
WHERE customer_id = 'c1'
  AND order_day = '2026-07-01'
  AND created_at = '2026-07-01T10:00:00Z'
  AND order_id = 11111111-1111-1111-1111-111111111111;

DELETE FROM orders_by_customer_day
WHERE customer_id = 'c1'
  AND order_day = '2026-07-01'
  AND created_at = '2026-07-01T10:00:00Z'
  AND order_id = 11111111-1111-1111-1111-111111111111;
```

Important:

```text
Updates and deletes usually need the full primary key or a valid partition/clustering restriction.
```

---

## 4. TTL

```sql
INSERT INTO sessions_by_user (
  user_id, session_id, created_at, expires_at
) VALUES (
  'u1', uuid(), toTimestamp(now()), '2026-07-02T00:00:00Z'
) USING TTL 86400;
```

TTL creates tombstones after expiry. Good for session/event data, dangerous when massive expiry waves create read and compaction pressure.

---

## 5. Batches

Use batches carefully.

Good use:

```text
Atomic-ish updates inside the same partition when you need grouped mutation semantics.
```

Bad use:

```text
Using a huge batch as a bulk loader across many partitions.
```

Example:

```sql
BEGIN BATCH
  INSERT INTO user_by_id (user_id, email) VALUES ('u1', 'asha@example.com');
  INSERT INTO user_email_lookup (email, user_id) VALUES ('asha@example.com', 'u1');
APPLY BATCH;
```

Interview nuance:

```text
Cross-partition logged batches coordinate extra work and can harm performance. They are not a replacement for relational transactions.
```

---

## 6. Query Restrictions

Cassandra queries should usually:

- include the full partition key
- optionally restrict clustering columns in order
- avoid arbitrary non-key filters
- avoid `ALLOW FILTERING` on production hot paths
- avoid unbounded partition scans

Bad query:

```sql
SELECT * FROM orders_by_customer_day WHERE status = 'PAID' ALLOW FILTERING;
```

Better design:

```sql
CREATE TABLE orders_by_status_day (
  status text,
  order_day date,
  created_at timestamp,
  order_id uuid,
  customer_id text,
  total_cents bigint,
  PRIMARY KEY ((status, order_day), created_at, order_id)
) WITH CLUSTERING ORDER BY (created_at DESC, order_id ASC);
```

---

## 7. Strong Answer

Question:

> Why is `ALLOW FILTERING` risky?

Strong answer:

```text
ALLOW FILTERING tells Cassandra to read more data than the primary-key-shaped query can directly target, then filter after reading. It may work on small data but becomes unpredictable at production scale. In Cassandra, the better answer is usually to create a table that matches the access pattern, such as orders_by_status_day, rather than scanning orders_by_customer_day by status.
```

---

## 8. Revision Notes

- One-line summary: CQL is query-shaped; efficient SELECTs must match the primary key design.
- Three keywords: primary key, TTL, ALLOW FILTERING.
- One interview trap: using batches as bulk import transactions.
- Memory trick: if the WHERE clause does not name the partition, Cassandra may have to search the cluster.