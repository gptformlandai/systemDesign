# Cassandra Security, Backup, and Disaster Recovery - Gold Sheet

> Track File #14 of 25 - Group 03: Senior / MAANG
> For: backend/database/system design interviews | Level: senior | Mode: production security, backup, restore, DR

This sheet builds:
- Cassandra security checklist
- Backup and restore concepts
- RPO/RTO and multi-DC disaster recovery thinking

---

## 1. Security Checklist

| Area | Practice |
|---|---|
| Authentication | enable auth; no anonymous production access |
| Authorization | least privilege roles per service |
| Network | private subnets, security groups, firewall rules |
| Client TLS | encrypt client-to-node traffic |
| Internode TLS | encrypt node-to-node traffic where required |
| Secrets | use secret manager; rotate credentials |
| Auditing | log privileged and sensitive operations where supported |
| Data protection | encrypt disks/backups; classify PII |

Interview rule:

```text
Cassandra security is not only username/password. Include network boundary, TLS, roles, secrets, backups, and operational audit.
```

---

## 2. Role Design

Examples:

- app read/write role for specific keyspace
- read-only analytics/export role
- admin role restricted to operators
- separate migration role for schema changes

Avoid:

- shared root/admin credentials in app code
- broad cross-keyspace grants
- long-lived credentials without rotation

---

## 3. Backup Types

| Backup Type | Purpose |
|---|---|
| snapshots | point-in-time SSTable snapshot on node |
| incremental backups | copy new SSTables after snapshot |
| managed backups | cloud/provider-controlled backup lifecycle |
| logical export | smaller controlled exports, not full-cluster DR by itself |

Snapshots are node-local. A real backup plan copies snapshots to durable external storage.

---

## 4. Restore Thinking

Restore is harder than backup.

Plan:

1. Define RPO and RTO.
2. Know exact keyspaces/tables to restore.
3. Restore schema and SSTables safely.
4. Validate data checks and application correctness.
5. Rebuild indexes/materialized artifacts if needed.
6. Test restore regularly, not during the incident for the first time.

---

## 5. Disaster Recovery

Multi-DC replication can support regional resilience, but it is not automatic simplicity.

Consider:

- client routing by local DC
- consistency levels during region failure
- cross-DC latency and write patterns
- repair and replica divergence
- failover/failback runbooks
- data residency and compliance

---

## 6. RPO/RTO Language

| Term | Meaning |
|---|---|
| RPO | how much data loss is acceptable |
| RTO | how long recovery can take |

Example answer:

```text
For audit logs, RPO should be near zero, so I need replicated writes and durable backup. RTO depends on whether the app can degrade to append-only regional writes while historical queries recover.
```

---

## 7. Strong Answer

Question:

> How would you secure and back up Cassandra?

Strong answer:

```text
I would enable authentication and least-privilege roles, keep Cassandra on private networks, use TLS where required, rotate secrets through a secret manager, encrypt disks and backups, and audit privileged actions. For backups, I would use snapshots plus incremental backups or managed backups copied to durable storage, then regularly test restore against defined RPO and RTO. In multi-DC setups, I would also document failover, consistency-level behavior, and repair/failback procedures.
```

---

## 8. Revision Notes

- One-line summary: Production Cassandra needs identity, network, encryption, backup, restore, and DR runbooks.
- Three keywords: roles, snapshots, RPO.
- One interview trap: saying replication is backup.
- Memory trick: replicas protect availability; backups protect recovery.