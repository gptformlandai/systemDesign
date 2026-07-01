# VectorDB Observability, SLO, and Incident Response - Gold Sheet

> Track File #14 of 30 - Group 03: Senior / MAANG
> For: production/search/GenAI interviews | Level: senior | Mode: metrics, SLOs, incidents

## 1. What To Monitor

| Area | Metrics |
|---|---|
| latency | p50, p95, p99 for search, upsert, delete |
| quality | recall@K, MRR, zero-result rate, click/success rate |
| ingestion | lag, failures, queue depth, reindex progress |
| security | permission leak tests, ACL mismatch, denied-result rate |
| cost | vector count, dimension, storage, replicas, query volume |
| health | node/container health, memory, disk, errors |

---

## 2. SLO Examples

| Workflow | Example SLO |
|---|---|
| RAG retrieval | p99 < 700 ms and recall@10 above baseline |
| product search | p95 < 250 ms and click-through stable |
| permission update | ACL propagation < 2 minutes |
| delete | deleted content unretrievable < 5 minutes |

---

## 3. Incident Categories

- low recall after deploy
- p99 latency spike
- stale or deleted content retrieved
- cross-tenant result leak
- embedding dimension mismatch
- ingestion backlog
- high reranker cost

---

## 4. Debug Order

```text
symptom -> exact query/filter -> recent deploys -> embedding model/version -> index health -> ANN/filter settings -> reranker -> source freshness -> golden-set regression
```

---

## 5. Interview Summary

```text
Vector DB observability must include both system metrics and retrieval-quality metrics. I would monitor p95/p99, QPS, upsert/delete failures, ingestion lag, recall@K, zero-result rate, stale content, permission leaks, and cost. Incidents should be debugged from exact query/filter and recent changes before tuning the index blindly.
```

---

## 6. Revision Notes

- One-line summary: Vector DB health means latency plus quality plus freshness plus security.
- Three keywords: p99, recall, freshness.
- One trap: dashboards that show only infrastructure health.