# Elasticsearch Shards, Replicas, Cluster, and Scaling - MAANG Master Sheet

> Track File #11 of 27 - Group 03: Senior / MAANG
> For: backend/search/system design interviews | Level: senior | Mode: distributed search, shard sizing, scaling

This sheet builds:
- Shard and replica sizing judgment
- Routing, allocation, cluster state, and hot-shard reasoning
- Senior-level scale and failure-mode language

---

## 1. Shard Mental Model

An Elasticsearch index is split into primary shards. Each primary can have replica shards. Search fans out to shards and merges results.

```text
index -> primary shards -> Lucene segments -> optional replicas
```

Shard count affects:

- write distribution
- search fan-out
- recovery speed
- heap and file handles
- cluster-state overhead
- operational flexibility

---

## 2. Replica Role

Replicas provide:

- high availability after primary loss
- search parallelism
- faster recovery options

Costs:

- more disk
- more indexing work
- more replication traffic

Rule:

```text
Replicas improve availability and search capacity, but they do not reduce primary indexing work.
```

---

## 3. Shard Sizing

There is no universal perfect shard size. Use workload and platform guidance.

Think about:

- index size and growth
- query concurrency
- shard count per node
- recovery time after failure
- merge pressure
- retention and rollover
- hot vs warm vs cold tiers

Practical answer:

```text
I would estimate data volume and retention, choose shard count with target shard sizes and recovery time in mind, validate under load, and use rollover/ILM rather than creating one forever-growing index.
```

---

## 4. Routing And Hot Shards

Routing decides which shard stores a document.

Default routing uses document ID. Custom routing can reduce query fan-out for tenant/user lookups, but it can create hot shards.

Hot-shard symptoms:

- one shard has high CPU/search/write load
- uneven node disk or latency
- one tenant/query dominates
- adding nodes does not fix the hot shard

Fixes:

- better routing strategy
- split by index family or tenant tier
- adjust shard count in next index version
- use aliases and reindex
- throttle noisy tenants

---

## 5. Cluster State

Cluster state stores metadata about indices, mappings, shards, aliases, and templates.

Risks:

- too many indices/shards
- mapping explosion
- frequent mapping updates
- huge alias/filter complexity

Senior phrase:

```text
Shard and mapping design are cluster-state decisions, not just query decisions.
```

---

## 6. Strong Answer

Question:

> How do you choose shard count?

Strong answer:

```text
I start from data volume, retention, query concurrency, indexing rate, and recovery requirements. I avoid both extremes: too few shards can create huge shards and slow recovery, while too many shards increase heap, file, fan-out, and cluster-state overhead. For time-based or growing data, I prefer ILM/rollover so shard sizes stay bounded and operational behavior is predictable.
```

---

## 7. Revision Notes

- One-line summary: Shards are capacity and operations units, not just partitions.
- Three keywords: shard, replica, routing.
- One interview trap: adding shards blindly for performance.
- Memory trick: every shard you create must be searched, stored, recovered, and managed.