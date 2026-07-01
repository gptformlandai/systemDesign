# Architecture Comparison Cost, Capacity, Latency, and Storage - MAANG Sheet

> Track File #15 of 30 - Group 03: Senior / MAANG
> For: senior system design interviews | Level: senior | Mode: capacity and cost modeling

## 1. Capacity Inputs

Ask for:

- records per day
- average record size
- retention period
- read QPS
- write QPS
- peak-to-average ratio
- replication factor
- index count and size
- hot vs cold data
- p99 latency target

---

## 2. Cost Drivers By Store

| Store | Cost Drivers |
|---|---|
| SQL | storage, indexes, replicas, CPU-heavy queries, backups |
| MongoDB | document size, indexes, working set, shards, backups |
| Cassandra | replicas, compaction, tombstones, storage growth |
| Elasticsearch | shards, replicas, doc count, mappings, hot storage |
| Vector DB | vector count, dimension, replicas, topK, reranking |
| Neo4j | graph size, traversal fan-out, memory, backups |
| Redis | memory, eviction, replication, persistence |
| Object storage | storage class, requests, egress, lifecycle |
| Warehouse | scanned bytes, compute slots/warehouses, storage |

---

## 3. Latency Budget

Break end-to-end latency into stages:

```text
API -> cache -> primary DB -> derived index -> external model/reranker -> response
```

Do not attribute all latency to the database if the architecture includes embedding calls, rerankers, downstream joins, or cache misses.

---

## 4. Interview Summary

```text
I would estimate capacity from record count, record size, retention, replication, indexes, and growth. Cost differs by store: SQL pays for indexes and compute-heavy queries, Elasticsearch for shards and hot storage, vector DBs for dimensions and topK, Redis for memory, and warehouses for scanned data and compute. Latency must be budgeted across every stage, not guessed at the database boundary.
```

---

## 5. Revision Notes

- One-line summary: Cost and latency depend on the whole data path.
- Three keywords: retention, replicas, p99.
- One trap: estimating raw data size but ignoring indexes and replicas.