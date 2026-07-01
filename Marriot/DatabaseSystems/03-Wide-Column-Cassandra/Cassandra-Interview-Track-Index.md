# Cassandra Interview Track Index

This folder is the Apache Cassandra wide-column database track for backend, database, distributed systems, production, and MAANG-style system design interviews.

Audience:
- You are a software engineer who wants beginner-to-pro Cassandra depth.
- You want practical backend readiness, not only CQL syntax.
- You want MAANG-level mastery of data modeling, partition keys, clustering keys, quorum consistency, replication, compaction, repair, tombstones, observability, security, and large-scale architecture judgment.

Goal:
- Build Cassandra from first principles to production ownership.
- Keep each topic modular so revision is fast.
- Make the answer pattern repeatable: access pattern, table model, primary key, consistency, failure mode, operational cost, tradeoff, strong interview answer.
- Connect Cassandra decisions to real systems: time-series metrics, chat, feeds, IoT, event ingestion, audit logs, multi-region platforms, and high-write microservices.

Use this index as the reading order.

---

## How To Read These Notes As A Backend Engineer

Before diving in, accept these five reframes:

### 1. Cassandra is query-model-first, not entity-model-first

In Cassandra, you usually design one table per important query. You do not start with normalized entities and expect joins to rescue the model later.

### 2. The partition key is the scaling decision

The partition key decides where data lives, how evenly writes spread, how reads route, and whether one hot customer, device, tenant, or room can overload a node.

### 3. Clustering columns are your on-disk sort order

Clustering columns decide how rows are sorted inside a partition. They are how you model time windows, latest messages, event history, and range scans.

### 4. Consistency is chosen per operation

Cassandra does not give one global consistency behavior. You choose write and read consistency levels, usually with quorum math, based on correctness and latency requirements.

### 5. Operations are part of the data model

TTL, tombstones, compaction, repair, anti-entropy, disk pressure, p99 latency, and node replacement are not afterthoughts. A table design that ignores them is incomplete.

---

## Relational Developer Bridge Pattern

Every important Cassandra topic should be translated through this pattern:

```text
Relational Developer Bridge

Similar to SQL:
  What concept maps cleanly.

Different in Cassandra:
  What works differently and why.

Does not exist or is weaker:
  SQL feature that Cassandra does not provide in the same way.

Cassandra replacement:
  Query tables, partition keys, clustering keys, denormalization, materialized read models, or application invariants.

Interview trap:
  The SQL-shaped assumption that leads to a bad Cassandra design.
```

---

## Learning Style: Beginner To MAANG Loop

Do not learn Cassandra as isolated commands. Learn every topic through this repeatable loop:

```text
access pattern -> table shape -> partition key -> clustering order -> consistency level -> operational failure mode -> interview answer
```

Use this style at each level:

| Level | How To Learn | Output You Must Produce |
|---|---|---|
| Beginner | Read the concept sheet, run CQL basics, explain the SQL-to-Cassandra bridge | A correct table, insert, select, update, delete, and 2-minute explanation |
| Intermediate | Start from API/query requirements, design one table per query, choose primary keys, and reject bad queries | A table model, sample CQL, partition-size reasoning, and query limitations |
| Senior | Add consistency, replication, repair, compaction, tombstones, observability, security, and failure cases | A production-ready design with quorum math and operations risk visible |
| MAANG / Pro | Answer as a system owner: requirements, write/read path, table family, scale path, failure modes, alternatives, and incident response | A whiteboard-ready architecture answer plus debugging and follow-up responses |

Daily study rhythm:

1. Read one concept sheet for 30-45 minutes.
2. Write one table schema and one query from an access pattern.
3. Explain one tradeoff out loud: partition width, hot key risk, consistency level, compaction strategy, TTL/tombstone cost, or repair cost.
4. Answer five active-recall questions without notes.
5. Finish with one system design or production-debugging prompt.

MAANG answer rule:

```text
Never stop at "Cassandra can scale writes".
Say which access pattern it serves, what partition key you chose, how consistency works, what fails, how you observe it, and what alternative you rejected.
```

---

## Track Structure

| Group | Purpose |
|---|---|
| 1. Starter Path | Fundamentals, architecture, setup, CQL, basic query modeling |
| 2. Intermediate Backend Path | Data modeling, primary keys, indexes, read/write path, app integration, time-series patterns |
| 3. Senior / MAANG Path | Consistency, replication, topology, compaction, repair, performance, security, cloud, testing |
| 4. Scenario Practice Path | Microservices, system design cases, Cassandra vs SQL/MongoDB/DynamoDB tradeoffs |
| 5. Special Interview Rounds | Anti-patterns, internals, debugging, direct Q&A |
| 6. Practice Upgrade Path | Active recall, hands-on labs, mini projects, cheat sheets, roadmap |
| 7. Runnable Lab Guide | Docker setup, cqlsh practice, table labs, incident drills, and project sequence |

---

## 1. Starter Path

Read these first. They build Cassandra intuition from zero to useful backend fluency.

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Starter-Path/Cassandra-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md](01-Starter-Path/Cassandra-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md) | Cassandra roadmap, where it fits, what it is good at, and what it is not good at |
| 2 | [01-Starter-Path/Cassandra-Core-Architecture-Data-Model-Gold-Sheet.md](01-Starter-Path/Cassandra-Core-Architecture-Data-Model-Gold-Sheet.md) | Keyspace, table, partition, row, column, node, token ring, coordinator, replica |
| 3 | [01-Starter-Path/Cassandra-Installation-Tools-CQLSH-Gold-Sheet.md](01-Starter-Path/Cassandra-Installation-Tools-CQLSH-Gold-Sheet.md) | Docker/local setup, cqlsh, nodetool, basic inspection commands |
| 4 | [01-Starter-Path/Cassandra-CQL-CRUD-Querying-Gold-Sheet.md](01-Starter-Path/Cassandra-CQL-CRUD-Querying-Gold-Sheet.md) | CREATE KEYSPACE/TABLE, INSERT, SELECT, UPDATE, DELETE, TTL, batches, query limits |

Starter target:
- You can explain what Cassandra is and when to choose it.
- You can create keyspaces and tables and run basic CQL safely.
- You understand why Cassandra queries must match the primary key design.

---

## 2. Intermediate Backend Path

After the starter path, read these to learn how Cassandra becomes a real backend database instead of a high-write key-value store.

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Backend/Cassandra-Data-Modeling-Partition-Key-Clustering-MAANG-Master-Sheet.md](02-Intermediate-Backend/Cassandra-Data-Modeling-Partition-Key-Clustering-MAANG-Master-Sheet.md) | Access-pattern-first modeling, partition keys, clustering keys, bucketing, denormalization |
| 6 | [02-Intermediate-Backend/Cassandra-Primary-Keys-Indexes-Materialized-Views-Gold-Sheet.md](02-Intermediate-Backend/Cassandra-Primary-Keys-Indexes-Materialized-Views-Gold-Sheet.md) | Primary key design, secondary indexes, SAI/SASI cautions, materialized view risks |
| 7 | [02-Intermediate-Backend/Cassandra-Read-Write-Path-LSM-SSTables-MAANG-Master-Sheet.md](02-Intermediate-Backend/Cassandra-Read-Write-Path-LSM-SSTables-MAANG-Master-Sheet.md) | Commit log, memtable, SSTables, compaction, bloom filters, partition index, read repair concepts |
| 8 | [02-Intermediate-Backend/Cassandra-Application-Development-Java-Spring-Python-Gold-Sheet.md](02-Intermediate-Backend/Cassandra-Application-Development-Java-Spring-Python-Gold-Sheet.md) | Java driver, Spring Data Cassandra, Python driver, prepared statements, paging, retries, idempotency |
| 9 | [02-Intermediate-Backend/Cassandra-Time-Series-TTL-Bucketing-Patterns-Gold-Sheet.md](02-Intermediate-Backend/Cassandra-Time-Series-TTL-Bucketing-Patterns-Gold-Sheet.md) | Time-series tables, time buckets, TTL, tombstone control, latest-N access patterns |

Intermediate target:
- You can design tables from queries instead of entities.
- You can choose partition and clustering keys with size, distribution, and sort-order reasoning.
- You can explain why most ad hoc filtering, joins, and global secondary indexes are dangerous.

---

## 3. Senior / MAANG Path

These are the production and distributed-systems sheets.

| Order | File | What It Builds |
|---:|---|---|
| 10 | [03-Senior-MAANG/Cassandra-Consistency-Replication-Quorum-MAANG-Master-Sheet.md](03-Senior-MAANG/Cassandra-Consistency-Replication-Quorum-MAANG-Master-Sheet.md) | Tunable consistency, quorum math, read/write concerns, hinted handoff, read repair, read-your-write |
| 11 | [03-Senior-MAANG/Cassandra-Ring-Tokens-Snitches-Topology-MAANG-Master-Sheet.md](03-Senior-MAANG/Cassandra-Ring-Tokens-Snitches-Topology-MAANG-Master-Sheet.md) | Token ring, vnodes, snitches, racks, data centers, NetworkTopologyStrategy |
| 12 | [03-Senior-MAANG/Cassandra-Compaction-Repair-Tombstones-MAANG-Master-Sheet.md](03-Senior-MAANG/Cassandra-Compaction-Repair-Tombstones-MAANG-Master-Sheet.md) | STCS, LCS, TWCS, anti-entropy repair, tombstone storms, gc_grace_seconds |
| 13 | [03-Senior-MAANG/Cassandra-Performance-Tuning-Observability-MAANG-Master-Sheet.md](03-Senior-MAANG/Cassandra-Performance-Tuning-Observability-MAANG-Master-Sheet.md) | p99 debugging, tracing, metrics, partition size, cache, coordinator pressure, JVM and disk signals |
| 14 | [03-Senior-MAANG/Cassandra-Security-Backup-Disaster-Recovery-Gold-Sheet.md](03-Senior-MAANG/Cassandra-Security-Backup-Disaster-Recovery-Gold-Sheet.md) | Auth, authorization, TLS, encryption, snapshots, incremental backups, restore, RPO/RTO |
| 15 | [03-Senior-MAANG/Cassandra-Advanced-Patterns-LWT-CDC-Counters-Search-Gold-Sheet.md](03-Senior-MAANG/Cassandra-Advanced-Patterns-LWT-CDC-Counters-Search-Gold-Sheet.md) | Lightweight transactions, compare-and-set, CDC, counters, search integrations, guardrails |
| 16 | [03-Senior-MAANG/Cassandra-Cloud-Kubernetes-Testing-Operations-Gold-Sheet.md](03-Senior-MAANG/Cassandra-Cloud-Kubernetes-Testing-Operations-Gold-Sheet.md) | Astra/managed Cassandra, Kubernetes cautions, Testcontainers, upgrade and capacity planning |

Senior target:
- You can reason about consistency, replication, failure, and tail latency.
- You can operate Cassandra with compaction, repair, backups, and observability.
- You can defend Cassandra in system design rounds and explain where it is the wrong tool.

---

## 4. Scenario Practice Path

Use these after the concept sheets to train interview and architecture answers.

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/Cassandra-Microservices-Event-Driven-Production-Patterns-Gold-Sheet.md](04-Scenario-Practice/Cassandra-Microservices-Event-Driven-Production-Patterns-Gold-Sheet.md) | Cassandra in microservices, event ingestion, idempotent writes, outbox/read model patterns |
| 18 | [04-Scenario-Practice/Cassandra-System-Design-Case-Studies-MAANG-Master-Sheet.md](04-Scenario-Practice/Cassandra-System-Design-Case-Studies-MAANG-Master-Sheet.md) | 12 design cases: metrics, chat, feeds, audit logs, IoT, notifications, sessions, fraud signals |
| 19 | [04-Scenario-Practice/Cassandra-vs-SQL-MongoDB-DynamoDB-Tradeoff-Analysis-Gold-Sheet.md](04-Scenario-Practice/Cassandra-vs-SQL-MongoDB-DynamoDB-Tradeoff-Analysis-Gold-Sheet.md) | Cassandra vs PostgreSQL, MongoDB, DynamoDB, Kafka, Elasticsearch, and time-series databases |

Scenario target:
- You can answer system design prompts with table models, query patterns, consistency levels, scale path, and failure modes.
- You can compare Cassandra with other databases without shallow NoSQL slogans.

---

## 5. Special Interview Rounds

Use these for debugging, internals, anti-patterns, and direct interview prep.

| Order | File | What It Builds |
|---:|---|---|
| 20 | [05-Special-Interview-Rounds/Cassandra-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md](05-Special-Interview-Rounds/Cassandra-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md) | Anti-patterns and fixes: hot partitions, wide partitions, ALLOW FILTERING, tombstone storms, repair gaps |
| 21 | [05-Special-Interview-Rounds/Cassandra-Interview-Prep-QA-MAANG-Sheet.md](05-Special-Interview-Rounds/Cassandra-Interview-Prep-QA-MAANG-Sheet.md) | Beginner, intermediate, senior, and MAANG Q&A with crisp answers and follow-ups |

Special-round target:
- You can identify bad table designs and production failure patterns.
- You can answer Cassandra interview questions from beginner to MAANG level.

---

## 6. Practice Upgrade Path

Use these to convert reading into active recall, labs, projects, and revision.

| Order | File | What It Builds |
|---:|---|---|
| 22 | [06-Practice-Upgrade/Cassandra-Active-Recall-Question-Bank.md](06-Practice-Upgrade/Cassandra-Active-Recall-Question-Bank.md) | Foundation, intermediate, advanced, and MAANG recall prompts by topic |
| 23 | [06-Practice-Upgrade/Cassandra-Hands-On-Exercises-And-Runnable-Mini-Labs.md](06-Practice-Upgrade/Cassandra-Hands-On-Exercises-And-Runnable-Mini-Labs.md) | Beginner-to-pro labs for CQL, modeling, consistency, tracing, TTL, repair, and incident drills |
| 24 | [06-Practice-Upgrade/Cassandra-Mini-Projects-Portfolio.md](06-Practice-Upgrade/Cassandra-Mini-Projects-Portfolio.md) | 12 practical Cassandra projects with schemas, queries, scaling concerns, and interview discussion points |
| 25 | [06-Practice-Upgrade/Cassandra-Cheat-Sheets-Roadmap-Golden-Rules.md](06-Practice-Upgrade/Cassandra-Cheat-Sheets-Roadmap-Golden-Rules.md) | Cheat sheets, beginner-to-pro roadmap, golden rules, mistakes, and final readiness checklist |
| 26 | [06-Practice-Upgrade/Cassandra-Pro-Gap-Fill-Capacity-Schema-Evolution-SLO-Design-Review.md](06-Practice-Upgrade/Cassandra-Pro-Gap-Fill-Capacity-Schema-Evolution-SLO-Design-Review.md) | Final pro gaps: collections, UDTs, static columns, schema evolution, capacity worksheet, SLOs, paging, design review |

Practice target:
- You can answer from memory, run labs, build mini projects, and revise with cheat sheets.
- You can measure readiness instead of passively rereading notes.
- You can run a staff-level design review that covers capacity, migration, SLOs, paging, and operations.

---

## 7. Runnable Lab Guide

Use the consolidated lab guide when you want runnable practice instead of reading-only notes:

- [cassandra-mastery-lab/README.md](cassandra-mastery-lab/README.md)
- [cassandra-mastery-lab/LEARNING_PATH.md](cassandra-mastery-lab/LEARNING_PATH.md)

Lab target:
- You can run Cassandra locally with Docker.
- You can practice CQL, primary key design, consistency levels, tracing, TTL, and table modeling.
- You can build and discuss senior-level Cassandra projects from metrics to chat to multi-region event ingestion.

---

## 8. Interview Answer Pattern

For most Cassandra interview answers, use this shape:

```text
1. Access pattern:
   What query or write path are we serving?

2. Table design:
   What table, partition key, and clustering keys support it?

3. Distribution:
   How does the partition key spread load and avoid hot partitions?

4. Consistency:
   What read/write consistency level do we choose and why?

5. Example:
   Show CQL, a sample row, or a production scenario.

6. Operational cost:
   What compaction, repair, TTL, tombstone, storage, or p99 risk exists?

7. Tradeoff and alternative:
   What gets faster/slower, simpler/harder, safer/riskier, and what would we use instead?
```

---

## 9. Recommended Study Orders

### 2-Week Practical Path

1. Starter Path files 1-4.
2. Data modeling, primary keys, read/write path, and app integration files 5-8.
3. Consistency, compaction, repair, performance files 10-13.
4. Active recall and hands-on labs.

### 4-Week MAANG Path

1. Week 1: Starter + query-model-first thinking.
2. Week 2: partition keys, clustering keys, indexes, read/write path, app integration, time-series.
3. Week 3: consistency, replication, topology, compaction, repair, performance, security, cloud/testing.
4. Week 4: system design cases, anti-pattern debugging, interview Q&A, projects.
5. Final pass: pro gap-fill appendix, capacity worksheet, schema migration, SLOs, and design-review checklist.

### Production Debugging Path

1. Read architecture, read/write path, and consistency.
2. Read compaction/repair/tombstones and performance/observability.
3. Practice incidents: p99 spike, hot partition, tombstone storm, node replacement, stale reads, repair backlog.
4. Score yourself with the Q&A and active recall sheets.

---

## 10. Readiness Gate

You are Cassandra interview-ready when you can do all of this without notes:

- Explain why Cassandra is query-model-first.
- Design tables for latest messages, user feed, IoT metrics, audit logs, and tenant events.
- Choose partition keys that avoid hot partitions and oversized partitions.
- Explain clustering order and time bucketing.
- Explain consistency levels, quorum math, read-your-write behavior, and stale reads.
- Walk through write path and read path with commit log, memtable, SSTable, bloom filter, and compaction.
- Explain tombstones, TTL, gc_grace_seconds, and why delete-heavy workloads hurt.
- Choose STCS, LCS, or TWCS for a workload.
- Debug p99 latency, hot partition, repair backlog, tombstone storm, and disk pressure.
- Compare Cassandra with PostgreSQL, MongoDB, DynamoDB, Kafka, Elasticsearch, and time-series databases.
- Design backup/restore and multi-DC behavior with RPO/RTO language.
- Give a system design answer that includes table models, consistency levels, failure modes, and operations.
- Estimate partition size, storage growth, and hot-key risk using a worksheet.
- Explain collections, UDTs, static columns, schema evolution, paging-state safety, and per-query SLOs.