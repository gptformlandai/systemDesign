# VectorDB Evaluation, Recall, Precision, MRR, and NDCG - Gold Sheet

> Track File #10 of 30 - Group 02: Intermediate Backend
> For: search/GenAI/system design interviews | Level: intermediate | Mode: retrieval evaluation

## 1. Why Evaluation Matters

Vector search can look impressive in demos and fail in production.

Evaluation answers:

```text
Did we retrieve the right evidence for real user questions?
```

---

## 2. Offline Metrics

| Metric | Meaning |
|---|---|
| recall@K | did expected relevant item appear in top K? |
| precision@K | how many retrieved items were relevant? |
| MRR | how high the first relevant result appears |
| NDCG | ranking quality with graded relevance |
| coverage | how often system has retrievable evidence |

---

## 3. RAG-Specific Metrics

- groundedness
- citation correctness
- answer completeness
- permission leak rate
- stale content rate
- refusal correctness
- user success rate

---

## 4. Golden Set

A golden set contains:

- representative queries
- expected relevant documents/chunks
- disallowed documents for ACL tests
- expected answer traits
- freshness/version expectations

Use it before changing embedding model, chunking, filters, or ANN settings.

---

## 5. Interview Summary

```text
I would evaluate vector retrieval with a golden query set and metrics like recall@K, precision@K, MRR, NDCG, citation correctness, stale content rate, and permission leak tests. Any embedding, chunking, index, or reranker change should pass offline evaluation before rollout.
```

---

## 6. Revision Notes

- One-line summary: Vector retrieval quality must be measured with golden queries.
- Three keywords: recall, MRR, groundedness.
- One trap: shipping a new embedding model without regression tests.