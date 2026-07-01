# Cassandra Interview Prep Q&A - MAANG Sheet

> Track File #21 of 25 - Group 05: Special Interview Rounds
> For: backend/database/system design interviews | Level: beginner to MAANG | Mode: direct Q&A, follow-ups, strong answers

This sheet builds:
- Fast interview answers
- Follow-up hooks
- Beginner, intermediate, senior, and MAANG readiness

---

## Beginner Questions

### 1. What is Cassandra?

Cassandra is a distributed wide-column database designed for high write throughput, horizontal scale, replication, and high availability for predictable access patterns.

### 2. What is a keyspace?

A keyspace is a namespace for tables and replication settings, similar to a database/schema plus replication configuration.

### 3. What is a partition key?

The partition key is the primary-key component used to hash and route data to replica nodes. It controls distribution, hot-key risk, and which data a query can target efficiently.

### 4. What are clustering columns?

Clustering columns sort rows inside a partition and support ordered/ranged reads within that partition.

---

## Intermediate Questions

### 5. Why design one table per query?

Cassandra does not support general joins and arbitrary filtering like a relational database. Tables should be shaped so the query can target a partition and clustering range directly.

### 6. Why is `ALLOW FILTERING` dangerous?

It can require Cassandra to read broad data and filter after reading, leading to unpredictable latency and cluster load. A query-shaped table is usually safer.

### 7. How do you avoid wide partitions?

Use bounded partition keys, often by adding time buckets or another sharding component, and estimate maximum rows/bytes per partition under peak retention.

### 8. Why use prepared statements?

They avoid query parsing overhead on hot paths and safely bind parameters, but they do not fix bad table design.

---

## Senior Questions

### 9. Explain the write path.

The coordinator sends the write to replicas. Replicas append to the commit log, update memtables, and acknowledge according to the consistency level. Memtables flush to immutable SSTables, and compaction later merges SSTables.

### 10. Explain the read path.

The coordinator contacts replicas, which check memtables and SSTables using bloom filters and indexes. Data and tombstones may be merged across SSTables, versions are reconciled, and the coordinator returns after the consistency level is satisfied.

### 11. What is quorum?

For RF=3, quorum is 2. If read consistency plus write consistency is greater than RF, reads and writes overlap on at least one replica, giving stronger consistency behavior.

### 12. What are tombstones?

Tombstones are markers for deletes or TTL expiry. They protect delete propagation but increase read and compaction work until safely removed.

---

## MAANG Deep-Dive Questions

### 13. Design chat message storage.

Use `messages_by_room_day` with `(room_id, bucket_day)` as partition key and `message_ts, message_id` as clustering columns. Use bucketing for partition safety, idempotent message IDs, quorum where needed, and a separate search system for text search.

### 14. A Cassandra service has high p99 read latency. What do you do?

Start with exact query/table, inspect partition distribution, partition size, tombstones, consistency level, tracing, compaction backlog, disk/GC/coordinator metrics, recent deploy changes, and then choose incident mitigation plus table-model fix.

### 15. Cassandra or PostgreSQL for orders?

PostgreSQL is usually better for order source-of-truth because of transactions and constraints. Cassandra can be a read model or event/audit timeline store if access patterns are predictable and high-volume.

### 16. How do you choose consistency levels?

Choose based on correctness, latency, availability, and locality. LOCAL_QUORUM read/write with RF=3 in the same DC is a common stronger local consistency pattern. ONE is lower latency but accepts stale reads.

### 17. Why can adding nodes fail to fix performance?

If the problem is a hot partition, all requests for that partition still target a limited replica set. You need partition-key redesign, bucketing, caching, throttling, or product-level changes.

### 18. When would you use LWT?

Use LWT for low-contention compare-and-set or uniqueness cases. Avoid it as a general transaction replacement or high-throughput hot-path mechanism.

---

## Interview Closing Formula

```text
For Cassandra I would first name the access pattern. Then I would design the table, partition key, clustering keys, and consistency levels. I would check partition size and skew, explain compaction/TTL/tombstone cost, name failure modes, and state when another database would be better.
```

---

## Revision Notes

- One-line summary: Cassandra interview strength is precise access-pattern, key-design, and operations reasoning.
- Three keywords: primary key, quorum, tombstones.
- One interview trap: giving generic NoSQL answers.
- Memory trick: answer with table shape before tool slogans.