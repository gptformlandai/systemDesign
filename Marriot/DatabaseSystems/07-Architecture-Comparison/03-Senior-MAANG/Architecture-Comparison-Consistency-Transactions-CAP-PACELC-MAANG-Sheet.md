# Architecture Comparison Consistency, Transactions, CAP, and PACELC - MAANG Sheet

> Track File #11 of 30 - Group 03: Senior / MAANG
> For: senior system design interviews | Level: senior | Mode: correctness, CAP, PACELC

## 1. Core Question

```text
What correctness can the product tolerate under concurrency, failure, and geographic distance?
```

This question often decides the datastore before scale does.

---

## 2. Consistency Spectrum

| Model | Meaning | Common Fit |
|---|---|---|
| strong consistency | latest committed value is visible | money, inventory reservation, permissions |
| read-your-writes | user sees own updates | profiles, settings, document edits |
| monotonic reads | user does not go backward | feeds, sessions, dashboards |
| eventual consistency | replicas converge later | social counters, recommendations, search indexes |

---

## 3. CAP And PACELC

CAP under partition:

```text
choose availability or consistency behavior when network partitions happen
```

PACELC adds normal operation tradeoff:

```text
if partition: availability vs consistency
else: latency vs consistency
```

---

## 4. Store Implications

| Store | Consistency Posture |
|---|---|
| PostgreSQL/MySQL | strong transactional core, replicas may be stale |
| MongoDB | strong for document-level operations, transactions supported |
| Cassandra | tunable consistency, high availability, query-model constraints |
| Elasticsearch | derived index, eventual freshness |
| Vector DB | derived index, freshness and ACL propagation matter |
| Neo4j | transactional graph, scale/partitioning choices matter |
| Redis cache | usually derived, stale data must be acceptable or controlled |

---

## 5. Interview Summary

```text
I would first classify the correctness requirement. Payments, ledgers, inventory reservations, and permissions need strong consistency and auditability. Search, vector retrieval, feeds, recommendations, and analytics can often tolerate eventual consistency if freshness SLOs and rebuild paths are clear. CAP/PACELC matters because global availability and low latency often trade off with strict consistency.
```

---

## 6. Revision Notes

- One-line summary: Correctness decides the datastore before popularity does.
- Three keywords: consistency, latency, partition.
- One trap: using eventual consistency for money movement without compensation and reconciliation.