# VectorDB Pro Gap Fill: Capacity, Evaluation, SLO, and Design Review

> Track File #30 of 30 - Group 06: Practice Upgrade
> For: staff-level design review | Level: pro / MAANG | Mode: capacity, eval, security, cost

## 1. Capacity Worksheet

| Input | Example |
|---|---:|
| vectors | 100,000,000 |
| dimensions | 1536 |
| bytes per dimension | 4 |
| raw vector data | about 614 GB |
| replicas | 2-3 |
| topK | 10-100 |
| query QPS | product-specific |

Always add overhead for ANN index, metadata, logs, replicas, compaction/segments, snapshots, and growth.

---

## 2. Evaluation Gate

Before rollout, answer:

- Did recall@K improve or stay above baseline?
- Did MRR/NDCG improve?
- Did citation correctness pass?
- Did permission leak tests pass?
- Did stale content rate stay inside SLO?
- Did p99 latency and cost stay acceptable?

---

## 3. SLO Review

| Workflow | SLO Example | Notes |
|---|---|---|
| RAG retrieval | p99 < 700 ms | includes vector search and rerank budget |
| delete propagation | < 5 min | security and compliance sensitive |
| ACL update | < 2 min | stale permissions are dangerous |
| recall regression | no worse than baseline | evaluated on golden set |
| ingestion lag | < 10 min | depends on product freshness needs |

---

## 4. Design Review Questions

1. What embedding model and dimension are used?
2. What metric is used and why?
3. What is the record ID strategy?
4. Which metadata fields are mandatory?
5. How are tenant and ACL filters enforced?
6. What is topK and reranker depth?
7. How are deletes and permission updates propagated?
8. How is recall measured?
9. How is p99 latency measured?
10. What is the rollback path for embedding upgrades?
11. What is the cost model?
12. What alternative system would be better if requirements change?

---

## 5. Staff-Level Answer

```text
I would not approve a production vector DB design until it has a capacity model, tenant/ACL enforcement plan, freshness and delete propagation SLOs, golden-set evaluation, p99 latency budget, embedding version rollout path, cost estimate, and explicit alternatives. Vector retrieval quality must be measured, secured, and operated like a production search system.
```

---

## 6. Revision Notes

- One-line summary: Pro vector DB design is quality, security, freshness, latency, and cost under one review.
- Three keywords: capacity, evaluation, SLO.
- One trap: treating vector DB rollout as only a library integration.