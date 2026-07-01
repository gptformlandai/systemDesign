# Architecture Comparison SLO, Operations, Backup, DR, and Security - Gold Sheet

> Track File #14 of 30 - Group 03: Senior / MAANG
> For: production architecture interviews | Level: senior | Mode: operability and risk

## 1. Production Questions

For every datastore, ask:

- What is the p99 latency SLO?
- What is the availability target?
- What is the RPO and RTO?
- How are backups tested?
- How are schema/index changes rolled out?
- What data is encrypted?
- What access is audited?
- Who owns incidents?

---

## 2. Backup And Recovery

| Store Type | Recovery Concern |
|---|---|
| SQL | point-in-time restore, transaction logs, replica promotion |
| MongoDB | backups, oplog, sharded restore complexity |
| Cassandra | snapshots, repairs, consistency, tombstones |
| Elasticsearch | snapshots, index templates, rebuild from source |
| Vector DB | rebuild from source embeddings or re-embed pipeline |
| Graph DB | backups plus projection freshness |
| Cache | usually rebuildable from source |
| Warehouse | reload/replay data pipelines |

---

## 3. Security And Compliance

Track:

- encryption at rest and in transit
- key management
- access control and least privilege
- audit logs
- tenant isolation
- data residency
- deletion and retention policies
- PII/PHI/payment data constraints

---

## 4. Observability

Required metrics:

- p50/p95/p99 latency
- QPS and write throughput
- error rate
- replication/indexing lag
- cache hit rate
- disk/memory/CPU
- backup success and restore test status
- stale derived-store rate

---

## 5. Interview Summary

```text
A production datastore decision must include operability: SLOs, backups, restore tests, DR, encryption, access control, audit, data residency, monitoring, and ownership. A database that works in the happy path but cannot be restored, secured, monitored, or operated is not production-ready.
```

---

## 6. Revision Notes

- One-line summary: Operability is part of the database choice.
- Three keywords: RPO, RTO, audit.
- One trap: saying backups exist without restore testing.