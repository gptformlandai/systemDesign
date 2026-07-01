# Cassandra Mastery Lab

A practical beginner-to-pro Cassandra lab for backend engineers, distributed systems interviews, production debugging, and MAANG-style database/system design rounds.

This lab is designed to be used alongside the modular Cassandra track. It includes Docker Compose, cqlsh helper scripts, schema/seed CQL, guided labs, projects, cheatsheets, interview prep, and production runbooks.

---

## Suggested Local Setup

Prerequisites:

- Docker Desktop
- basic terminal comfort
- optional Java/Python if you want to connect with drivers later

Start Cassandra and seed the lab:

```bash
docker compose up -d
bash SCRIPTS/wait-for-cassandra.sh
bash SCRIPTS/run-cqlsh.sh SCRIPTS/00-create-keyspace.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/01-schema.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/02-seed-data.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/03-lab-queries.cql
```

Reset everything from scratch:

```bash
bash SCRIPTS/reset-lab.sh
```

Open interactive `cqlsh`:

```bash
bash SCRIPTS/run-cqlsh.sh
```

If port `9042` is already in use, change the port mapping in `docker-compose.yml` or stop the conflicting container.

---

## Repository-Style Learning Areas

Use the main track files as the source of truth and this folder for hands-on practice:

```text
cassandra-mastery-lab/
  README.md
  LEARNING_PATH.md
  docker-compose.yml
  SCRIPTS/
    00-create-keyspace.cql
    01-schema.cql
    02-seed-data.cql
    03-lab-queries.cql
    04-tracing-consistency.cql
    05-ttl-tombstone-demo.cql
    06-cleanup.cql
    run-cqlsh.sh
    reset-lab.sh
    wait-for-cassandra.sh
  LABS/
    01-cql-basics.md
    02-query-modeling.md
    03-consistency-tracing.md
    04-ttl-tombstones.md
    05-performance-incident-drills.md
  PROJECTS/
    01-chat-message-history.md
    02-iot-telemetry-store.md
    03-audit-log-platform.md
  CHEATSHEETS/
    CQL.md
    MODELING.md
    OPERATIONS.md
  INTERVIEW_PREP/
    QUESTIONS.md
    ANSWER_PATTERNS.md
  RUNBOOKS/
    HOT_PARTITION.md
    TOMBSTONE_STORM.md
```

---

## What You Will Practice

1. Create keyspaces and tables.
2. Design query-shaped schemas.
3. Insert and query with primary-key-shaped access.
4. Use TTL and explain tombstones.
5. Use tracing for targeted queries.
6. Explain consistency levels.
7. Build table families for chat, IoT, audit logs, sessions, and feeds.
8. Debug hot partitions, wide partitions, tombstone storms, and stale reads.

---

## First Session

1. Run `bash SCRIPTS/reset-lab.sh`.
2. Open [LABS/01-cql-basics.md](LABS/01-cql-basics.md).
3. Run [SCRIPTS/03-lab-queries.cql](SCRIPTS/03-lab-queries.cql).
4. Explain why `messages_by_room_day` and `messages_by_sender_day` are separate tables.
5. Open [LABS/02-query-modeling.md](LABS/02-query-modeling.md) and fix the bad-query examples.
6. Run [SCRIPTS/04-tracing-consistency.cql](SCRIPTS/04-tracing-consistency.cql).
7. Run [SCRIPTS/05-ttl-tombstone-demo.cql](SCRIPTS/05-ttl-tombstone-demo.cql) and explain tombstones.

---

## Learning Outcomes

By the end, you should be able to:

- model Cassandra tables from access patterns
- choose partition keys and clustering keys
- explain why Cassandra avoids joins and arbitrary filtering
- use consistency levels correctly
- explain read/write path internals
- handle TTL, tombstones, compaction, and repair at interview depth
- debug production latency and stale-read incidents
- compare Cassandra with SQL, MongoDB, DynamoDB, Kafka, and search systems
- defend Cassandra choices in MAANG-style system design interviews

---

## Suggested Practice Loop

```text
run script -> inspect rows -> explain partition key -> name failure mode -> answer interview prompt
```

Use this lab in short loops. Cassandra sticks when every CQL table is tied to an access pattern and every access pattern has a failure story.