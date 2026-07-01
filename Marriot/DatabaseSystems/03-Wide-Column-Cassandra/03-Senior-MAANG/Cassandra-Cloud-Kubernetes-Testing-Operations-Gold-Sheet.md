# Cassandra Cloud, Kubernetes, Testing, and Operations - Gold Sheet

> Track File #16 of 25 - Group 03: Senior / MAANG
> For: backend/database/system design interviews | Level: senior | Mode: managed platforms, Kubernetes, testing, upgrades

This sheet builds:
- Managed Cassandra and Kubernetes tradeoffs
- Testcontainers and integration testing approach
- Upgrade, capacity, and operations maturity

---

## 1. Managed Cassandra

Managed services can reduce operational load, but they do not remove data modeling responsibility.

Check:

- supported Cassandra version and feature set
- consistency-level behavior
- backup/restore options
- metrics and slow-query visibility
- network/private endpoint support
- limits on compaction, repair, and node sizing
- pricing model for reads/writes/storage/egress

Interview phrase:

```text
Managed Cassandra reduces operational surface area, but partition-key design, query modeling, consistency choices, and workload behavior remain application responsibilities.
```

---

## 2. Kubernetes Caution

Cassandra can run on Kubernetes, but stateful databases need mature operations.

Evaluate:

- persistent volume performance and topology
- pod disruption budgets
- anti-affinity across nodes/zones
- backup and restore automation
- repair and compaction operations
- operator maturity
- upgrade and rollback process
- observability integration

Bad answer:

```text
Put Cassandra in Kubernetes because everything runs there.
```

Strong answer:

```text
Only run Cassandra on Kubernetes when storage, networking, operator maturity, disruption controls, and restore drills are production-ready.
```

---

## 3. Testing Strategy

| Test Type | Purpose |
|---|---|
| unit tests | business logic and query construction |
| repository integration tests | real CQL behavior against Cassandra/Testcontainers |
| schema migration tests | table compatibility and rollout safety |
| load tests | partition key distribution and p99 behavior |
| failure tests | timeout/retry/idempotency behavior |

Testcontainers is useful for verifying driver behavior, table schemas, CQL syntax, and repository methods.

---

## 4. Schema Migration Discipline

Cassandra schema changes should be planned.

Safer rollout:

1. Add new table/columns.
2. Deploy dual-write if needed.
3. Backfill carefully with throttling.
4. Switch reads gradually.
5. Monitor p99, errors, and storage.
6. Retire old table after retention and rollback window.

Avoid frequent schema churn caused by unstable access patterns.

---

## 5. Operations Checklist

- capacity model exists
- compaction and repair strategy exists
- backup restore tested
- dashboards for latency, timeouts, tombstones, compaction, disk, GC
- alerts have runbooks
- driver settings reviewed
- consistency levels documented
- data retention and TTL strategy documented
- node replacement and region-failure drills practiced

---

## 6. Strong Answer

Question:

> How would you test Cassandra-backed services?

Strong answer:

```text
I would unit test domain logic, then use real Cassandra integration tests through Testcontainers for CQL, repositories, schema, paging, and consistency-sensitive behavior. I would load test realistic partition-key distributions because correctness on tiny data does not prove production p99. For critical write paths, I would test retries and idempotency because timeouts can happen after a write reaches replicas.
```

---

## 7. Revision Notes

- One-line summary: Managed/cloud/Kubernetes choices do not replace data modeling, testing, and operations discipline.
- Three keywords: managed limits, Testcontainers, rollout.
- One interview trap: assuming Kubernetes makes stateful operations easy.
- Memory trick: if restore is untested, backup is a theory.