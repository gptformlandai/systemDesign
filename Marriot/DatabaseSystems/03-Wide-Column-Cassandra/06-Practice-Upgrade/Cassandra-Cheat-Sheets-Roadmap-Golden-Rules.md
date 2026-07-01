# Cassandra Cheat Sheets, Roadmap, and Golden Rules - Gold Sheet

> Track File #25 of 25 - Group 06: Practice Upgrade
> For: backend/database/system design interviews | Level: revision and final consolidation | Mode: fast recall, commands, roadmap, rules

This sheet builds:
- Cassandra command and design cheat sheets
- Beginner-to-pro roadmap
- Golden rules and final readiness checklist

---

## 1. CQL Cheat Sheet

```sql
CREATE KEYSPACE app WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
USE app;
CREATE TABLE table_name (... PRIMARY KEY ((pk), ck));
INSERT INTO table_name (...) VALUES (...);
SELECT * FROM table_name WHERE pk = ?;
UPDATE table_name SET col = ? WHERE pk = ? AND ck = ?;
DELETE FROM table_name WHERE pk = ? AND ck = ?;
```

---

## 2. Primary Key Cheat Sheet

```text
partition key = distribution + replica routing + partition size
clustering key = sort order + range scan inside partition
regular columns = stored values, usually not query drivers
```

Good table names:

- `messages_by_room_day`
- `metrics_by_device_hour`
- `orders_by_customer_day`
- `audit_events_by_tenant_day`

---

## 3. Consistency Cheat Sheet

| Need | Common Choice |
|---|---|
| lowest latency | ONE / LOCAL_ONE |
| stronger local correctness | LOCAL_QUORUM read/write |
| strict all replicas | ALL, rarely for hot paths |
| multi-DC local behavior | LOCAL_QUORUM with local routing |

Formula:

```text
quorum = floor(RF / 2) + 1
W + R > RF creates replica overlap
```

---

## 4. Data Modeling Golden Rules

1. Design from access patterns.
2. One important query usually gets one table.
3. Keep partitions bounded.
4. Avoid hot keys.
5. Use clustering columns for ordered reads inside a partition.
6. Duplicate data intentionally.
7. Make multi-table writes idempotent.
8. Avoid `ALLOW FILTERING` on hot paths.
9. Use TTL with tombstone and compaction planning.
10. Export search and analytics to the right system.

---

## 5. Operations Cheat Sheet

| Topic | Watch |
|---|---|
| compaction | pending tasks, disk IO, write/read amplification |
| repair | repair age, failures, streaming load |
| tombstones | warning counts, read latency, TTL/delete patterns |
| disk | utilization, growth, backup space, compaction headroom |
| p99 | table/query-specific latency, timeouts |
| client | consistency, retries, paging, prepared statements |
| cluster | node status, token distribution, dropped messages |

---

## 6. Anti-Pattern Cheat Sheet

| Bad Pattern | Better Pattern |
|---|---|
| SQL-style normalized model | query-shaped denormalized tables |
| unbounded room/user/device partition | time bucket partition key |
| status as partition key | include tenant/time/high-cardinality key |
| global scans with filtering | new table/search/analytics system |
| blind retries | idempotency keys and deterministic writes |
| massive cross-partition batches | async fan-out writes |
| random LWT usage | use only low-contention CAS cases |

---

## 7. Beginner To Pro Roadmap

### Stage 1: Beginner

Topics:

- Cassandra purpose
- keyspace, table, partition, clustering
- CQL basics
- cqlsh and nodetool basics

Project: user session store.

Success criteria:

- You can create a table and explain the primary key.

### Stage 2: Intermediate

Topics:

- access-pattern modeling
- partition keys and clustering keys
- secondary index caution
- read/write path
- app integration

Project: chat or order query tables.

Success criteria:

- You can design multiple tables for multiple queries and explain why.

### Stage 3: Senior

Topics:

- consistency levels
- replication and topology
- compaction, repair, tombstones
- performance and observability
- backup/security

Project: IoT metrics or audit log platform.

Success criteria:

- You can debug p99 latency and stale-read scenarios.

### Stage 4: MAANG / Pro

Topics:

- multi-region design
- incident runbooks
- database tradeoffs
- advanced patterns: LWT, CDC, counters, search integration
- project portfolio and mock interviews

Project: multi-region event ingestion or full activity feed design.

Success criteria:

- You can defend Cassandra and reject it when another database is better.

---

## 8. Final MAANG Checklist

- I can name the exact access pattern before proposing Cassandra.
- I can design the table and primary key on a whiteboard.
- I can explain partition size, skew, and hot-key risk.
- I can use collections, UDTs, and static columns only for bounded, stable, query-aligned data.
- I can create a schema migration plan when a new access pattern appears.
- I can estimate storage growth, partition size, retention cost, and hottest-key behavior.
- I can explain read/write path internals.
- I can choose consistency levels with quorum math.
- I can explain tombstones, repair, and compaction.
- I can debug p99 latency from query to storage metrics.
- I can define SLOs and alerts per access pattern, not only cluster-wide averages.
- I can design paging as a bounded, opaque cursor contract.
- I can design backup/restore and DR with RPO/RTO.
- I can compare Cassandra to PostgreSQL, MongoDB, DynamoDB, Kafka, and search systems.
- I can say when Cassandra is the wrong tool.

---

## 9. Pro Gap-Fill Checklist

Use this after the normal roadmap:

| Gap | What You Must Be Able To Say |
|---|---|
| collections | bounded only; not hidden child tables |
| UDTs | stable small value objects; not document modeling |
| static columns | small partition-level metadata; not parent-table replacement |
| schema evolution | new table, dual-write, backfill, validate, cut over, retire |
| capacity | estimate rows/partition, bytes/partition, RF storage, compaction headroom |
| paging | bounded partition reads with opaque short-lived cursor semantics |
| SLOs | latency/error/tombstone/compaction/repair alerts per table/query |
| design review | query model, key design, correctness, lifecycle, operations, alternatives |

---

## 10. Final Summary

```text
Cassandra is a strong choice when the access patterns are known, write volume is high, partitions can be distributed evenly, and the team can own consistency and operations. It is a poor choice when the workload needs joins, arbitrary filtering, strict relational transactions, or constantly changing query patterns.
```