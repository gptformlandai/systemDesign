# Cassandra Hands-On Exercises and Runnable Mini Labs

> Track File #23 of 25 - Group 06: Practice Upgrade
> For: backend/database/system design interviews | Level: beginner to pro | Mode: local labs, CQL, modeling, debugging

Use these exercises with a local Docker Cassandra node or a managed sandbox.

---

## Lab 1: First Keyspace And Table

Goal: become comfortable with keyspaces, tables, and primary keys.

Tasks:

1. Start Cassandra with Docker.
2. Create keyspace `app`.
3. Create `user_by_id`.
4. Insert 5 users.
5. Query by `user_id`.

Success criteria:

- You can explain keyspace replication.
- You can distinguish partition key from regular columns.

---

## Lab 2: Query-Shaped Order Tables

Goal: practice one table per query.

Create:

```sql
orders_by_customer_day ((customer_id, order_day), created_at, order_id)
orders_by_status_day ((status, order_day), created_at, order_id)
order_by_id (order_id)
```

Practice:

- insert the same order into all required tables
- query customer order history
- query paid orders by day
- fetch one order by ID

Explain:

```text
Why are there three tables for one business entity?
```

---

## Lab 3: Chat Messages With Bucketing

Goal: avoid unbounded partitions.

Create:

```sql
messages_by_room_day ((room_id, bucket_day), message_ts, message_id)
```

Practice:

- insert messages for multiple rooms and days
- query latest 50 messages
- explain what happens to a very hot room
- propose room/hour bucket or shard suffix mitigation

---

## Lab 4: Consistency Levels

Goal: understand consistency as an operation-level choice.

Practice in `cqlsh`:

```sql
CONSISTENCY ONE;
CONSISTENCY QUORUM;
CONSISTENCY LOCAL_QUORUM;
```

Explain:

- RF=3 quorum math
- stale read risk at ONE
- why local quorum is common in multi-DC designs

---

## Lab 5: TTL And Tombstone Awareness

Goal: see retention as a data-model decision.

Tasks:

- insert session rows with TTL
- query before expiry
- query after expiry
- explain why expired data is not immediately free

Discussion:

```text
How would mass TTL expiry hurt p99 reads?
```

---

## Lab 6: Tracing A Query

Goal: inspect query execution.

Practice:

```sql
TRACING ON;
SELECT * FROM messages_by_room_day
WHERE room_id = 'room-1' AND bucket_day = '2026-07-01'
LIMIT 50;
```

Explain:

- coordinator role
- replica contact
- where latency can appear

---

## Lab 7: Bad Query Clinic

Goal: fix anti-patterns.

Bad queries:

```sql
SELECT * FROM orders_by_customer_day WHERE status = 'PAID' ALLOW FILTERING;
SELECT * FROM messages_by_room_day WHERE room_id = 'room-1';
SELECT * FROM events_by_tenant_day WHERE event_type = 'LOGIN';
```

For each:

1. Explain why it is bad.
2. Design the correct table.
3. Write the correct SELECT.

---

## Lab 8: Incident Drill

Scenario:

```text
The chat API p99 jumped from 80 ms to 2 seconds for one room after a celebrity livestream.
```

Answer:

- likely hot partition
- table and partition key involved
- immediate mitigation
- long-term table redesign
- metrics to confirm

---

## Lab 9: Backup And Restore Drill

Goal: practice recovery thinking.

Tasks:

- define RPO/RTO for audit logs
- explain snapshot vs incremental backup
- explain why replication is not backup
- write restore validation checklist

---

## Lab 10: Mock Design Board

Prompt:

```text
Design a high-volume IoT telemetry store for 20 million devices, 1 event per minute, 30-day raw retention, and dashboard queries by device and hour.
```

Must include:

- table schemas
- partition key and clustering key
- bucket size
- consistency level
- TTL and compaction strategy
- aggregation/export plan
- failure modes

---

## Completion Gate

You finish these labs only when you can explain:

- why each table exists
- what query each table serves
- what partition key was chosen
- what can go wrong at scale
- what metric or runbook catches that failure