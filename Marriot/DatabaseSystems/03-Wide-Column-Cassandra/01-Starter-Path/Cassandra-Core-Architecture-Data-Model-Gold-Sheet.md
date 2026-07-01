# Cassandra Core Architecture and Data Model - Gold Sheet

> Track File #2 of 25 - Group 01: Starter Path
> For: backend/database/system design interviews | Level: beginner to intermediate | Mode: architecture, terminology, data model

This sheet builds:
- Cassandra architecture vocabulary
- Keyspace, table, partition, row, column, token, coordinator, replica
- The bridge from SQL tables to Cassandra query tables

---

## 1. Core Terms

| Term | Meaning | Interview Importance |
|---|---|---|
| Cluster | Group of Cassandra nodes | Scale and availability boundary |
| Node | Server running Cassandra | Stores token ranges and serves reads/writes |
| Data center | Logical group of nodes | Multi-region and replication strategy boundary |
| Rack | Fault domain inside a data center | Replica placement should avoid same rack concentration |
| Keyspace | Namespace plus replication settings | Similar to database/schema plus replication config |
| Table | Query-optimized row store | Usually designed for one access pattern |
| Partition key | Primary-key part used to distribute data | Most important scaling decision |
| Clustering columns | Sort rows inside one partition | Enables ordered/ranged reads within a partition |
| Coordinator | Node receiving the client request | Contacts replicas and merges responses |
| Replica | Node holding a copy of the partition | Durability and availability unit |
| Token ring | Hash-space ownership model | Maps partition keys to replicas |

---

## 2. SQL To Cassandra Bridge

| SQL Thinking | Cassandra Thinking |
|---|---|
| Normalize entities | Duplicate data by query |
| Join at read time | Precompute query table |
| Flexible WHERE clause | WHERE must match primary key/index rules |
| One canonical table | Multiple tables for different access patterns |
| Global constraints | Application invariants plus idempotency |
| ACID transaction by default | Tunable consistency and limited LWT when needed |

Interview trap:

```text
Do not design customers, orders, order_items as normalized tables and then expect Cassandra to join them.
Design tables like orders_by_customer_day or order_by_id based on the exact query.
```

---

## 3. Primary Key Anatomy

Example:

```sql
CREATE TABLE messages_by_room_day (
  room_id text,
  bucket_day date,
  message_ts timestamp,
  message_id uuid,
  sender_id text,
  body text,
  PRIMARY KEY ((room_id, bucket_day), message_ts, message_id)
) WITH CLUSTERING ORDER BY (message_ts DESC, message_id ASC);
```

Meaning:

| Piece | Role |
|---|---|
| `(room_id, bucket_day)` | Composite partition key; routes data and caps partition size by day |
| `message_ts` | First clustering column; sorts messages by time |
| `message_id` | Tie-breaker clustering column; makes row identity unique |

---

## 4. How A Write Routes

```text
client sends INSERT
-> coordinator receives request
-> partition key is hashed to token
-> token maps to replica nodes
-> replicas append to commit log and update memtable
-> chosen consistency level decides when coordinator returns success
```

Important detail:

```text
The coordinator can be any node. The replicas are chosen by the partition key token and replication strategy.
```

---

## 5. How A Read Routes

```text
client sends SELECT with partition key
-> coordinator finds replicas
-> replicas search memtable/SSTables
-> bloom filters and indexes reduce disk checks
-> coordinator reconciles versions by timestamp
-> chosen consistency level decides response
```

Strong reads depend on:

- table design matching query
- partition not being too large
- avoiding excessive tombstones
- enough replicas responding for chosen consistency level

---

## 6. Data Model Rules

- Model by query, not by entity.
- Keep partitions bounded.
- Avoid hot partition keys.
- Use clustering keys for sort/range inside a partition.
- Duplicate data intentionally when queries differ.
- Make writes idempotent when duplicate retries can happen.
- Keep table names query-shaped: `events_by_tenant_day`, `messages_by_room_day`.

---

## 7. Strong Answer

Question:

> What is the difference between partition key and clustering key?

Strong answer:

```text
The partition key decides which replicas store the data and therefore controls distribution and hot-key risk. The clustering keys sort rows inside that partition and support range scans within the partition. For example, in messages_by_room_day, room_id plus bucket_day can be the partition key, while message_ts sorts messages newest first inside that room-day partition.
```

---

## 8. Revision Notes

- One-line summary: Partition key distributes data; clustering keys organize rows inside one partition.
- Three keywords: coordinator, replica, token.
- One interview trap: assuming CQL tables behave like relational tables.
- Memory trick: partition key = where; clustering key = order within where.