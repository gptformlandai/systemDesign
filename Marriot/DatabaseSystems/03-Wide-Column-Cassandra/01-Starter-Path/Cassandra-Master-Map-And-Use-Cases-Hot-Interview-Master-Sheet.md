# Cassandra Master Map and Use Cases - Hot Interview Master Sheet

> Track File #1 of 25 - Group 01: Starter Path
> For: backend/database/system design interviews | Level: beginner to senior | Mode: mental model, use cases, interview framing

This sheet builds:
- What Cassandra is and why it exists
- Where Cassandra is strong, weak, and risky
- The complete beginner-to-pro learning map
- The first MAANG-level answer shape

---

## 1. What Cassandra Is

Apache Cassandra is a distributed wide-column database built for high write throughput, horizontal scale, multi-node replication, and high availability across failures.

Simple version:

```text
Cassandra stores rows by partition key across a cluster so writes and reads can be distributed across many nodes.
```

Professional version:

```text
Cassandra is a partitioned, replicated, LSM-tree-based database with tunable consistency and query-model-first table design.
```

---

## 2. What It Is Good At

Use Cassandra when the system has:

| Need | Why Cassandra Fits |
|---|---|
| Very high write volume | Writes are append-friendly and distributed by partition key |
| Predictable query patterns | Tables can be optimized for exact access paths |
| Large time-series/event data | Wide rows, clustering order, TTL, and bucketing fit append-heavy histories |
| High availability | No single primary node for the whole cluster |
| Multi-region active-active style workloads | NetworkTopologyStrategy and tunable consistency can support regional writes |
| Operational tolerance for eventual consistency | Correctness can be designed with quorum, idempotency, and reconciliation |

Common product fits:

- metrics and observability samples
- IoT telemetry
- chat message history
- activity feeds
- audit logs
- fraud/risk signals
- notification delivery logs
- user sessions
- high-volume event read models

---

## 3. What It Is Not Good At

Avoid Cassandra when the system needs:

- ad hoc joins
- arbitrary filtering
- complex transactions across many entities
- strong relational constraints
- frequent global aggregations
- small-data operational simplicity
- query patterns that change weekly
- delete-heavy workloads without tombstone discipline

Interview trap:

```text
Bad answer: Cassandra is NoSQL, so it is automatically good for scale.
Strong answer: Cassandra is strong for predictable high-scale access patterns where data can be partitioned evenly and queried by primary-key-shaped access paths.
```

---

## 4. Mental Model

Think of Cassandra as many sorted maps spread across machines.

```text
partition key -> chooses node replicas
clustering keys -> sort rows inside that partition
columns -> values stored for each row
consistency level -> how many replicas must respond
compaction/repair -> background work that keeps storage and replicas healthy
```

The design question is always:

```text
For this query, what partition do I read, in what order, and how large can that partition become?
```

---

## 5. Beginner To Pro Learning Map

| Stage | Mastery Target |
|---|---|
| Beginner | Explain keyspace, table, partition key, clustering key, CQL, and basic CRUD |
| Intermediate | Design one table per query and choose primary keys safely |
| Senior | Explain write path, read path, replication, quorum, compaction, repair, tombstones, and p99 behavior |
| MAANG / Pro | Design Cassandra-backed systems with tradeoffs, failure modes, operational runbooks, and alternatives |

---

## 6. Core Flow

```text
client request
-> coordinator node
-> partition key hash
-> replica nodes for token range
-> write/read at chosen consistency level
-> memtable/SSTables/commit log or read merge
-> response to coordinator
-> client result
```

During interviews, connect every answer to:

1. Access pattern.
2. Partition key.
3. Clustering order.
4. Consistency level.
5. Failure mode.
6. Operational cost.

---

## 7. Strong Starter Answer

Question:

> When would you choose Cassandra?

Strong answer:

```text
I would choose Cassandra for predictable, high-volume workloads where reads and writes can be modeled around known access patterns, such as IoT metrics, chat history, audit logs, or event timelines. I would design tables around queries, choose partition keys that distribute load evenly, use clustering keys for time-ordered reads, and choose consistency levels based on correctness needs. I would avoid Cassandra for ad hoc joins, complex relational transactions, and workloads where query patterns are not stable.
```

---

## 8. Revision Notes

- One-line summary: Cassandra is a distributed, partitioned, replicated database for predictable high-scale access patterns.
- Three keywords: partition key, quorum, compaction.
- One interview trap: treating Cassandra as a generic SQL replacement.
- Memory trick: if you cannot name the access pattern, you cannot design the Cassandra table.