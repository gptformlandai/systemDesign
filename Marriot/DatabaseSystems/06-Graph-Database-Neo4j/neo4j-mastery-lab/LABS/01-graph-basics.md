# Lab 01: Graph Basics

Goal: create a sample graph and run basic traversals.

---

## Run

```bash
docker compose up -d
bash SCRIPTS/wait-for-neo4j.sh
bash SCRIPTS/run-cypher.sh SCRIPTS/01-schema.cypher
bash SCRIPTS/run-cypher.sh SCRIPTS/02-seed-data.cypher
bash SCRIPTS/run-cypher.sh SCRIPTS/03-basic-traversals.cypher
```

---

## What To Observe

- users, products, accounts, devices, documents, chunks, and entities are nodes
- follows, purchases, shared signals, mentions, and document chunks are relationships
- traversals start from specific anchors like `userId` or `accountId`

---

## Explain Out Loud

```text
Why is `(:User)-[:BOUGHT]->(:Product)` different from a SQL join table?
```

Strong answer:

```text
The relationship is stored and traversed directly as a first-class connection. The graph model makes the relationship meaningful and queryable as a path, not just as a row used for joining tables.
```