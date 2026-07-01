# Neo4j Security, Multi-Tenancy, Backup, and Disaster Recovery - Gold Sheet

> Track File #13 of 30 - Group 03: Senior / MAANG
> For: backend/data/security interviews | Level: senior | Mode: access control, backups, DR

This sheet builds:
- Neo4j security checklist
- Multi-tenant graph design options
- Backup, restore, RPO/RTO language

---

## 1. Security Checklist

| Area | Practice |
|---|---|
| Authentication | no anonymous production access |
| Authorization | least-privilege roles and database permissions |
| Network | private access, firewall/security groups |
| TLS | encrypt client and cluster traffic as required |
| Secrets | secret manager and rotation |
| Query API | no raw user Cypher execution without strict controls |
| Audit | monitor admin and sensitive graph access |

---

## 2. Multi-Tenancy Options

| Strategy | Fit | Risk |
|---|---|---|
| tenant label/property | many small tenants | every query must enforce tenant filter |
| database per tenant | stronger isolation | operational overhead |
| cluster per large tenant | regulated/high-scale tenants | cost and management complexity |
| derived tenant graphs | analytics/RAG use cases | sync/freshness complexity |

Rule:

```text
Tenant filters must be part of the graph query boundary, not post-processing.
```

---

## 3. Backups Are Not Replicas

Replication protects availability. Backups protect recoverability from:

- accidental delete
- bad import
- corrupt sync job
- malicious write
- cluster-level failure

Always test restore.

---

## 4. RPO/RTO Language

| Term | Meaning |
|---|---|
| RPO | acceptable data loss window |
| RTO | acceptable recovery time |

Graph-specific nuance:

```text
If Neo4j is a derived graph, recovery may be rebuild from source. If it is primary, restore drills and transaction-log policy become critical.
```

---

## 5. Strong Answer

```text
I would secure Neo4j with authentication, least-privilege roles, private networking, TLS where required, secrets management, and audited access. For multi-tenancy, I would choose property/label, database, or cluster isolation based on tenant count, regulation, and noisy-neighbor risk. For DR, I would define whether Neo4j is primary or derived, then set backup, restore, RPO, RTO, and rebuild policies accordingly.
```

---

## 6. Revision Notes

- One-line summary: Graph security must protect relationships, paths, and derived knowledge, not only nodes.
- Three keywords: tenant, backup, RPO.
- One interview trap: confusing replication with backup.
- Memory trick: relationships can leak secrets too.