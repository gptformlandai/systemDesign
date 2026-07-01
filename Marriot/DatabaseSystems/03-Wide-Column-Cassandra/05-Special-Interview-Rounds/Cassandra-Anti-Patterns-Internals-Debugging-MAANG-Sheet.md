# Cassandra Anti-Patterns, Internals, and Debugging - MAANG Sheet

> Track File #20 of 25 - Group 05: Special Interview Rounds
> For: backend/database/system design interviews | Level: senior / MAANG | Mode: traps, fixes, production debugging

This sheet builds:
- Cassandra anti-pattern recognition
- Production debugging playbooks
- Interview follow-up confidence

---

## 1. Top Anti-Patterns

| Anti-Pattern | Why It Fails | Better Approach |
|---|---|---|
| modeling like SQL | joins/filtering do not scale | one table per query |
| partition key with low cardinality | hot partitions | high-cardinality bounded key |
| unbounded partition | wide reads, tombstones, repair pain | time/entity bucketing |
| `ALLOW FILTERING` on hot path | unpredictable cluster scans | query-shaped table |
| secondary indexes everywhere | poor selectivity and p99 risk | denormalized tables/search |
| delete-heavy wide rows | tombstone storms | TTL/retention/model redesign |
| huge cross-partition batches | coordinator overload | async/idempotent writes |
| CL=ONE for correctness-critical reads | stale reads | quorum strategy |
| LWT on hot path | high latency/contention | idempotency or different store |

---

## 2. Debug: Hot Partition

Symptoms:

- one endpoint slow despite healthy cluster average
- uneven node load
- high traffic for one tenant/user/room/device
- adding nodes does not help much

Fixes:

- add bucket/salt component
- split table by time/window/entity
- cache hot reads
- throttle noisy tenant
- change product behavior for extreme keys

---

## 3. Debug: Wide Partition

Symptoms:

- slow reads for one partition
- high row count per partition
- large scans and memory pressure
- long repair/compaction for table

Fixes:

- time bucket partition key
- separate latest table from history table
- archive old buckets
- reduce query range

---

## 4. Debug: Tombstone Storm

Symptoms:

- tombstone warnings
- read timeouts
- p99 spikes after TTL expiry or deletes
- compaction pressure

Fixes:

- avoid reading over expired ranges
- use TWCS for time-windowed TTL workloads
- reduce range deletes
- tune retention with repair discipline
- redesign table if delete-heavy pattern is inherent

---

## 5. Debug: Stale Reads

Causes:

- weak consistency level
- cross-DC lag
- failed replicas
- read immediately after timeout ambiguity
- client reads from different region

Fixes:

- use LOCAL_QUORUM read/write where needed
- route clients consistently
- inspect replica health and repair age
- design idempotency and reconciliation

---

## 6. Strong Debugging Answer

```text
I start with the exact query and table. Cassandra debugging is rarely abstract; the table model decides the failure. I check partition key distribution, partition size, consistency level, tombstone metrics, compaction backlog, node resource signals, and recent client/deploy changes. Then I choose mitigation for the incident and a model-level fix if the table shape is wrong.
```

---

## 7. Revision Notes

- One-line summary: Cassandra anti-patterns are usually table-modeling or operations-modeling failures.
- Three keywords: hot partition, tombstone, ALLOW FILTERING.
- One interview trap: fixing every issue with more nodes.
- Memory trick: find the query, then find the partition.