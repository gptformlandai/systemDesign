# VectorDB Hybrid Search, Reranking, and Cross Encoders - MAANG Master Sheet

> Track File #15 of 30 - Group 03: Senior / MAANG
> For: search/GenAI/system design interviews | Level: senior | Mode: relevance tuning and reranking

## 1. Why Hybrid Search

Dense embeddings are strong for meaning but can miss:

- exact error codes
- product SKUs
- names
- rare terms
- legal clauses
- numeric identifiers

Sparse search catches exact lexical signals. Hybrid retrieval combines both.

---

## 2. Common Pipeline

```text
dense vector candidates + sparse/BM25 candidates -> merge -> rerank -> top final context
```

Fusion methods:

- weighted score combination
- reciprocal rank fusion
- learned ranking

---

## 3. Reranking

A reranker scores query-document pairs more deeply than ANN retrieval.

Types:

- cross-encoder reranker
- LLM reranker
- domain-specific classifier
- business-rule reranker

Tradeoff:

```text
better relevance, higher latency and cost
```

---

## 4. Reranker Safety

Only authorized candidates should reach reranking.

Also track:

- reranker latency
- reranker timeout fallback
- result diversity
- citation quality
- cost per query

---

## 5. Interview Summary

```text
I would use hybrid search when semantic embeddings alone miss exact terms, IDs, or rare keywords. Dense and sparse candidates can be fused, then reranked with a cross-encoder or LLM reranker. Reranking improves relevance but adds latency and cost, so I would measure quality lift and enforce ACL filters before reranking.
```

---

## 6. Revision Notes

- One-line summary: Hybrid plus reranking often beats pure vector search in production.
- Three keywords: dense, sparse, rerank.
- One trap: reranking unauthorized candidates.