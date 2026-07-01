# Cassandra Mastery Lab Learning Path

This path turns Cassandra from a database you have heard of into a system you can design, operate, debug, and defend in interviews.

---

## Stage 1: Starter Foundations

Goal: understand Cassandra's shape and run basic CQL.

Read:

- `../01-Starter-Path/Cassandra-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md`
- `../01-Starter-Path/Cassandra-Core-Architecture-Data-Model-Gold-Sheet.md`
- `../01-Starter-Path/Cassandra-Installation-Tools-CQLSH-Gold-Sheet.md`
- `../01-Starter-Path/Cassandra-CQL-CRUD-Querying-Gold-Sheet.md`

Run:

```bash
docker compose up -d
bash SCRIPTS/wait-for-cassandra.sh
bash SCRIPTS/run-cqlsh.sh SCRIPTS/00-create-keyspace.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/01-schema.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/02-seed-data.cql
```

Lab:

- [LABS/01-cql-basics.md](LABS/01-cql-basics.md)
- [CHEATSHEETS/CQL.md](CHEATSHEETS/CQL.md)

Success criteria:

- You can explain keyspace, table, partition key, clustering key, coordinator, and replica.
- You can create a table and query it by partition key.

---

## Stage 2: Intermediate Data Modeling

Goal: design tables for real APIs.

Read:

- `../02-Intermediate-Backend/Cassandra-Data-Modeling-Partition-Key-Clustering-MAANG-Master-Sheet.md`
- `../02-Intermediate-Backend/Cassandra-Primary-Keys-Indexes-Materialized-Views-Gold-Sheet.md`
- `../02-Intermediate-Backend/Cassandra-Time-Series-TTL-Bucketing-Patterns-Gold-Sheet.md`

Practice:

- model chat messages
- model orders by customer/day
- model IoT metrics by device/hour
- explain why each query needs its own table

Lab:

- [LABS/02-query-modeling.md](LABS/02-query-modeling.md)
- [CHEATSHEETS/MODELING.md](CHEATSHEETS/MODELING.md)

Success criteria:

- You can defend partition keys and clustering keys.
- You can reject bad low-cardinality and unbounded partition designs.

---

## Stage 3: Internals And Application Integration

Goal: connect table modeling to real performance and app behavior.

Read:

- `../02-Intermediate-Backend/Cassandra-Read-Write-Path-LSM-SSTables-MAANG-Master-Sheet.md`
- `../02-Intermediate-Backend/Cassandra-Application-Development-Java-Spring-Python-Gold-Sheet.md`

Practice:

- explain write path from client to commit log and memtable
- explain read path from coordinator to SSTables
- write driver-style prepared statement examples
- explain timeout and retry ambiguity

Lab:

- [SCRIPTS/03-lab-queries.cql](SCRIPTS/03-lab-queries.cql)
- [INTERVIEW_PREP/ANSWER_PATTERNS.md](INTERVIEW_PREP/ANSWER_PATTERNS.md)

Success criteria:

- You can describe why writes are fast and why reads can become expensive.
- You can explain idempotent writes and paging.

---

## Stage 4: Senior Production Cassandra

Goal: understand correctness, failure, scale, and operations.

Read:

- `../03-Senior-MAANG/Cassandra-Consistency-Replication-Quorum-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Cassandra-Ring-Tokens-Snitches-Topology-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Cassandra-Compaction-Repair-Tombstones-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Cassandra-Performance-Tuning-Observability-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Cassandra-Security-Backup-Disaster-Recovery-Gold-Sheet.md`

Practice:

- compute quorum for RF=3 and RF=5
- debug a stale-read scenario
- debug a tombstone storm
- write an RPO/RTO backup answer

Lab:

- [LABS/03-consistency-tracing.md](LABS/03-consistency-tracing.md)
- [LABS/04-ttl-tombstones.md](LABS/04-ttl-tombstones.md)
- [RUNBOOKS/TOMBSTONE_STORM.md](RUNBOOKS/TOMBSTONE_STORM.md)

Success criteria:

- You can explain LOCAL_QUORUM tradeoffs.
- You can connect tombstones, compaction, repair, and p99 latency.

---

## Stage 5: Advanced And Scenario Design

Goal: use Cassandra correctly in larger architectures.

Read:

- `../03-Senior-MAANG/Cassandra-Advanced-Patterns-LWT-CDC-Counters-Search-Gold-Sheet.md`
- `../03-Senior-MAANG/Cassandra-Cloud-Kubernetes-Testing-Operations-Gold-Sheet.md`
- `../04-Scenario-Practice/Cassandra-Microservices-Event-Driven-Production-Patterns-Gold-Sheet.md`
- `../04-Scenario-Practice/Cassandra-System-Design-Case-Studies-MAANG-Master-Sheet.md`
- `../04-Scenario-Practice/Cassandra-vs-SQL-MongoDB-DynamoDB-Tradeoff-Analysis-Gold-Sheet.md`

Practice:

- design chat history
- design IoT metrics
- design audit logs
- compare Cassandra and PostgreSQL for orders
- compare Cassandra and DynamoDB for AWS-native scale

Projects:

- [PROJECTS/01-chat-message-history.md](PROJECTS/01-chat-message-history.md)
- [PROJECTS/02-iot-telemetry-store.md](PROJECTS/02-iot-telemetry-store.md)
- [PROJECTS/03-audit-log-platform.md](PROJECTS/03-audit-log-platform.md)

Runbooks:

- [RUNBOOKS/HOT_PARTITION.md](RUNBOOKS/HOT_PARTITION.md)

Success criteria:

- You can name when Cassandra is right and when it is wrong.

---

## Stage 6: Interview Readiness

Read:

- `../05-Special-Interview-Rounds/Cassandra-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md`
- `../05-Special-Interview-Rounds/Cassandra-Interview-Prep-QA-MAANG-Sheet.md`
- `../06-Practice-Upgrade/Cassandra-Active-Recall-Question-Bank.md`
- `../06-Practice-Upgrade/Cassandra-Hands-On-Exercises-And-Runnable-Mini-Labs.md`
- `../06-Practice-Upgrade/Cassandra-Mini-Projects-Portfolio.md`
- `../06-Practice-Upgrade/Cassandra-Cheat-Sheets-Roadmap-Golden-Rules.md`
- `../06-Practice-Upgrade/Cassandra-Pro-Gap-Fill-Capacity-Schema-Evolution-SLO-Design-Review.md`

Lab practice:

- [LABS/05-performance-incident-drills.md](LABS/05-performance-incident-drills.md)
- [INTERVIEW_PREP/QUESTIONS.md](INTERVIEW_PREP/QUESTIONS.md)
- [INTERVIEW_PREP/ANSWER_PATTERNS.md](INTERVIEW_PREP/ANSWER_PATTERNS.md)
- [CHEATSHEETS/OPERATIONS.md](CHEATSHEETS/OPERATIONS.md)

MAANG deep-dive gate:

- Defend partition key and clustering choices.
- Explain consistency levels and quorum math.
- Debug hot partitions, wide partitions, tombstones, p99 latency, and stale reads.
- Explain compaction, repair, backup, restore, and DR.
- Estimate capacity, partition size, storage growth, and hottest-key behavior.
- Explain schema evolution, paging-state safety, collections, UDTs, static columns, and per-query SLOs.
- Compare Cassandra with SQL, MongoDB, DynamoDB, Kafka, search, and time-series systems.
- Design at least 3 portfolio projects end to end.