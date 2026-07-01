# Neo4j Clustering, Replication, High Availability, and Operations - Gold Sheet

> Track File #12 of 30 - Group 03: Senior / MAANG
> For: backend/data/system design interviews | Level: senior | Mode: production operations and HA

This sheet builds:
- Neo4j production topology language
- Read/write routing and failover concepts
- Operational maturity for graph systems

---

## 1. Production Topology Mental Model

Neo4j production deployments commonly separate:

- write-capable core/primary members
- read scaling replicas or read endpoints
- drivers that route reads/writes
- backup and monitoring processes

Exact names and capabilities vary by version and edition, but the design questions remain stable.

---

## 2. What HA Solves

HA helps with:

- node failure
- planned maintenance
- read scaling
- availability during rolling upgrades

HA does not automatically solve:

- bad graph model
- hot nodes
- unbounded traversals
- data corruption from bad writes
- missing backups

---

## 3. Driver Routing

Applications should use routing-aware drivers where appropriate.

Ask:

- Is this a read or write transaction?
- Does read-after-write require bookmarks?
- Are analytics reads isolated?
- What happens during leader/failover changes?

---

## 4. Operational Signals

Watch:

- query latency and slow queries
- transaction retries/failures
- heap and page cache
- disk growth
- checkpoints and transaction logs
- lock waits and deadlocks
- cluster member health
- backup success and restore drills

---

## 5. Strong Answer

```text
For production Neo4j, I would use a topology that supports the availability and read-scaling requirements, configure routing-aware drivers, keep write transactions small and retryable, monitor slow queries, heap, page cache, disk, transaction logs, lock waits, and cluster health, and test backup/restore. HA keeps the database available, but it does not fix bad traversal design or missing operational guardrails.
```

---

## 6. Revision Notes

- One-line summary: Neo4j HA protects availability, while query/model discipline protects latency.
- Three keywords: routing, failover, backup.
- One interview trap: assuming clustering fixes slow traversals.
- Memory trick: HA keeps nodes alive; modeling keeps queries alive.