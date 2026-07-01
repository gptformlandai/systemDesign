# Lab 01: CQL Basics

Goal: create a keyspace, create query-shaped tables, seed data, and run primary-key-shaped reads.

---

## Run

```bash
docker compose up -d
bash SCRIPTS/wait-for-cassandra.sh
bash SCRIPTS/run-cqlsh.sh SCRIPTS/00-create-keyspace.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/01-schema.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/02-seed-data.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/03-lab-queries.cql
```

---

## What To Observe

- `messages_by_room_day` serves room/day reads.
- `messages_by_sender_day` serves sender/day reads.
- The same message is duplicated because Cassandra optimizes by query.
- Every hot query includes the partition key.

---

## Explain Out Loud

```text
Why is there no single messages table that supports every possible query?
```

Strong answer:

```text
Cassandra tables are designed around access patterns. A room/day timeline and a sender/day lookup have different partition keys, so they need different tables if both are hot production queries.
```

---

## Completion Gate

- You can create and seed the keyspace.
- You can explain every table name as a query.
- You can identify the partition key for each SELECT.