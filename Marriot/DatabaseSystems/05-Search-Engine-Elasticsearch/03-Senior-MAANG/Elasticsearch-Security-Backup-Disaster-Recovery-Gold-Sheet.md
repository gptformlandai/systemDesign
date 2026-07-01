# Elasticsearch Security, Backup, and Disaster Recovery - Gold Sheet

> Track File #14 of 27 - Group 03: Senior / MAANG
> For: backend/search/system design interviews | Level: senior | Mode: production security, snapshots, restore, DR

This sheet builds:
- Elasticsearch security checklist
- Snapshot and restore concepts
- RPO/RTO and search-degradation thinking

---

## 1. Security Checklist

| Area | Practice |
|---|---|
| Authentication | no anonymous production access |
| Authorization | least privilege roles per service/user |
| TLS | encrypt client and node traffic as required |
| API keys | scoped service credentials |
| Field/document security | protect tenant/PII access where licensed/supported |
| Network | private endpoints, allowlists, security groups |
| Secrets | secret manager and rotation |
| Audit | log sensitive/admin access |

Rule:

```text
Tenant and ACL filters must be enforced before results leave Elasticsearch, especially for RAG and document search.
```

---

## 2. Snapshot And Restore

Snapshots back up indices and cluster metadata to a repository.

Use for:

- disaster recovery
- migration
- rollback after bad indexing/mapping changes
- long-term archival strategy

Snapshots are incremental at segment level and should be tested through restores.

---

## 3. Replicas Are Not Backups

Replicas protect availability from node/shard loss. They do not protect against:

- accidental delete
- bad reindex
- mapping mistake
- corrupt ingestion pipeline
- malicious change
- cluster-wide failure

Interview phrase:

```text
Replicas keep the service available; snapshots let you recover from bad data or disaster.
```

---

## 4. RPO/RTO Language

| Term | Meaning |
|---|---|
| RPO | acceptable data loss window |
| RTO | acceptable recovery time |

Search-specific nuance:

```text
If Elasticsearch can be rebuilt from the source of truth, the DR plan may prefer rebuild/reindex over restoring every index, but the time and freshness impact must be explicit.
```

---

## 5. Strong Answer

Question:

> How would you secure and back up Elasticsearch?

Strong answer:

```text
I would enable authentication, least-privilege roles or API keys, TLS, private network access, secret rotation, and audit logging. For tenant or document search, authorization filters must be enforced in the search query, not after results are shown. For backups, I would use snapshot repositories, test restore regularly, and define whether recovery comes from snapshot restore or rebuilding from the source of truth based on RPO/RTO.
```

---

## 6. Revision Notes

- One-line summary: Elasticsearch security needs identity, query-time authorization, snapshots, and restore drills.
- Three keywords: API key, snapshot, RPO.
- One interview trap: confusing replicas with backups.
- Memory trick: replicas save uptime; snapshots save history.