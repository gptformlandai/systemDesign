# Architecture Comparison Decision Dimensions: CAP, SLO, and Cost - Gold Sheet

> Track File #2 of 30 - Group 01: Starter Path
> For: system design interviews | Level: beginner to intermediate | Mode: decision framework

## 1. Decision Axes

Use these axes for every database decision:

- data model
- access pattern
- consistency and transaction needs
- latency SLO
- write/read scale
- query flexibility
- availability and partition tolerance
- operational ownership
- cost and retention
- security and compliance
- backup and disaster recovery

---

## 2. Correctness Questions

Ask:

- Can stale reads be tolerated?
- Is money, inventory, identity, or permission state involved?
- Do writes need atomic multi-row or multi-document transactions?
- Is read-after-write required?
- Is duplicate or out-of-order processing possible?

Correctness usually narrows the datastore choice before scale does.

---

## 3. SLO Questions

| SLO | Architecture Impact |
|---|---|
| p99 latency | index design, cache, partitioning, replicas |
| availability | replication, failover, multi-region design |
| freshness | CDC, indexing lag, cache invalidation |
| durability | WAL, replication, backup, restore testing |
| recovery | RPO/RTO, snapshots, replay, rebuild path |

---

## 4. Cost Questions

Cost comes from:

- storage volume
- indexes and replicas
- hot vs cold retention
- query CPU/memory
- write amplification
- network transfer
- managed service tier
- operational team time

---

## 5. Interview Summary

```text
I would compare datastores across data model, access pattern, consistency, latency SLO, read/write scale, query flexibility, availability, operations, cost, security, and DR. A strong answer does not optimize only for throughput; it also covers correctness, freshness, recovery, and ownership.
```

---

## 6. Revision Notes

- One-line summary: Database decisions are multi-axis tradeoffs.
- Three keywords: correctness, SLO, cost.
- One trap: choosing based only on scale while ignoring correctness.