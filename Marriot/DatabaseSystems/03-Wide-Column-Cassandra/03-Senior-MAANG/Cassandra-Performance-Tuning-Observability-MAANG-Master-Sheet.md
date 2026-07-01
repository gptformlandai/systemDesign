# Cassandra Performance Tuning and Observability - MAANG Master Sheet

> Track File #13 of 25 - Group 03: Senior / MAANG
> For: backend/database/system design interviews | Level: senior | Mode: p99 debugging, metrics, tracing, runbooks

This sheet builds:
- Cassandra performance debugging workflow
- Metrics and symptoms for hot partitions, tombstones, compaction, disk, and coordinator pressure
- Interview-ready incident response

---

## 1. Performance First Principle

Most Cassandra performance problems are one of these:

```text
bad table model, hot partition, oversized partition, tombstones, compaction backlog, disk pressure, coordinator overload, wrong consistency, or client misuse
```

Do not start by tuning random JVM settings. Start with query/table/workload evidence.

---

## 2. Debugging Workflow

```text
symptom -> affected query/table -> partition/key distribution -> consistency level -> tracing/metrics -> storage signals -> mitigation -> model fix
```

Ask:

- Which endpoint is slow?
- Which CQL statement is slow?
- Is the partition key hot?
- Is the partition too wide?
- Are tombstones high?
- Is compaction pending?
- Is disk IO saturated?
- Are timeouts from coordinators, replicas, or clients?
- Did a deploy change consistency, paging, retries, or query shape?

---

## 3. Useful Signals

| Signal | What It Suggests |
|---|---|
| high p99 read latency | wide partition, tombstones, disk, too many SSTables |
| high write latency | commit log/disk pressure, compaction, overloaded replicas |
| read timeouts | slow replicas, high CL, tombstone scans, coordinator pressure |
| dropped mutations | overload or backpressure issue |
| compaction pending | storage engine falling behind |
| high tombstone warnings | delete/TTL problem or bad partition scan |
| uneven node load | hot tokens, bad partition key, skew |
| high GC pauses | heap pressure, workload/config issue |

---

## 4. Tracing

For targeted investigation:

```sql
TRACING ON;

SELECT *
FROM messages_by_room_day
WHERE room_id = 'room-1'
  AND bucket_day = '2026-07-01'
LIMIT 50;
```

Tracing helps reveal coordinator/replica activity, but do not leave tracing broadly enabled in production.

---

## 5. Common Fixes

| Problem | Fix |
|---|---|
| hot partition | redesign key, add bucket/salt, cache, throttle |
| wide partition | time bucket, split key, archive old data |
| tombstone storm | reduce delete scans, adjust TTL/model, TWCS, repair discipline |
| too many SSTables | compaction tuning, disk capacity, workload smoothing |
| coordinator overload | driver load balancing, prepared statements, paging, node capacity |
| stale reads | consistency-level adjustment and replica health |
| slow global query | new table, search system, or analytics store |

---

## 6. Capacity Thinking

Estimate:

- write rate per partition and per cluster
- read rate per partition and per cluster
- average row size
- TTL/retention window
- replication factor
- compaction overhead
- peak-to-average traffic ratio
- disk headroom and repair/streaming bandwidth

Rough storage formula:

```text
logical data * replication factor * compaction overhead * growth headroom
```

---

## 7. Strong Answer

Question:

> A Cassandra endpoint suddenly has high p99 latency. How do you debug it?

Strong answer:

```text
I start from the endpoint and exact CQL query, then identify the table, partition key distribution, consistency level, and recent deploy/config changes. I check tracing for a sampled query, table metrics for read latency and tombstones, nodetool stats for compaction backlog and SSTables, and node metrics for disk, CPU, GC, dropped messages, and coordinator pressure. If the issue is a hot or wide partition, the durable fix is a data model change such as bucketing or a new table, not just adding nodes.
```

---

## 8. Revision Notes

- One-line summary: Cassandra p99 debugging starts from query/table evidence, not random tuning.
- Three keywords: tracing, tombstones, compaction.
- One interview trap: adding nodes before proving the problem is cluster-wide rather than key-specific.
- Memory trick: slow Cassandra means ask which partition hurt first.