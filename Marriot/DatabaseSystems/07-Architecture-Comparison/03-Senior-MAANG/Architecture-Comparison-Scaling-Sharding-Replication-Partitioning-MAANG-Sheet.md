# Architecture Comparison Scaling, Sharding, Replication, and Partitioning - MAANG Sheet

> Track File #12 of 30 - Group 03: Senior / MAANG
> For: senior system design interviews | Level: senior | Mode: scale, hot keys, partitioning

## 1. Scaling Questions

Ask:

- What is read QPS and write QPS?
- Is traffic global or regional?
- What is the hot key risk?
- Is data naturally partitioned by tenant, user, region, time, or entity?
- Are queries single-partition or cross-partition?
- What is the p99 latency SLO?

---

## 2. Partitioning Patterns

| Pattern | Fit | Risk |
|---|---|---|
| user_id partition | user-centric reads | celebrity/hot user |
| tenant_id partition | B2B SaaS isolation | large tenant skew |
| region partition | data residency and latency | cross-region workflows |
| time partition | logs/events/metrics | hot current bucket |
| hash partition | even distribution | weak locality |

---

## 3. Replication Patterns

| Pattern | Use | Risk |
|---|---|---|
| leader-follower | read scale, failover | replica lag |
| multi-leader | multi-region writes | conflict resolution |
| quorum replication | tunable consistency | latency and operational complexity |
| async derived replication | search/cache/vector/analytics | staleness |

---

## 4. Store Implications

- SQL can scale with replicas, partitioning, sharding, and careful transaction boundaries.
- Cassandra-style systems require partition-key-first modeling.
- Elasticsearch scales by shards but can suffer hot shards.
- Vector DBs scale with vector count, dimension, replicas, topK, and filter selectivity.
- Graph DBs need bounded traversals and careful hot-node handling.

---

## 5. Interview Summary

```text
I would scale by first identifying the natural partition key and hot-key risks. SQL can scale with replicas and partitioning but gets complex for cross-shard transactions. Cassandra fits high-scale partition-key workloads. Search/vector/graph systems need their own shard, index, fan-out, and hot-node controls. The key is keeping the critical query bounded to the smallest necessary partition or index scope.
```

---

## 6. Revision Notes

- One-line summary: Scaling is partitioning plus hot-key control plus replication semantics.
- Three keywords: shard, replica, hot key.
- One trap: choosing a partition key without checking skew.