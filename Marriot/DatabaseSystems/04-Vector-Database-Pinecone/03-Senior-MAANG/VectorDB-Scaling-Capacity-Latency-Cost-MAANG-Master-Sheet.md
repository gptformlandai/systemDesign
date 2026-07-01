# VectorDB Scaling, Capacity, Latency, and Cost - MAANG Master Sheet

> Track File #11 of 30 - Group 03: Senior / MAANG
> For: senior backend/search/system design interviews | Level: senior | Mode: capacity math, p99, cost

## 1. Capacity Inputs

Ask for:

- number of vectors
- dimension
- bytes per dimension
- metadata size
- replicas
- QPS
- topK
- filter selectivity
- ingestion rate
- freshness SLO

Rough raw vector memory:

```text
vectors * dimension * 4 bytes
```

Example:

```text
100M vectors * 1536 dimensions * 4 bytes ~= 614 GB raw vector data
```

Indexes, metadata, replicas, WALs, and overhead can multiply this significantly.

---

## 2. Latency Budget

Typical retrieval path:

```text
embed query -> vector search -> metadata filter -> rerank -> fetch source -> context pack
```

If p99 target is 500 ms, every stage needs a budget.

---

## 3. Cost Drivers

| Driver | Why It Matters |
|---|---|
| vector count | storage and memory |
| dimension | storage and CPU per comparison |
| replicas | availability and read scale |
| topK | candidate count and rerank cost |
| metadata filters | index/filter execution overhead |
| embedding model | query and ingestion cost |
| reranker | high latency/cost if overused |

---

## 4. Scaling Patterns

- partition by tenant or namespace
- separate hot and cold corpora
- use replicas/read scaling
- compress vectors when acceptable
- tune topK and reranker depth
- precompute high-volume recommendations
- use hybrid search only where it improves quality

---

## 5. Interview Summary

```text
For vector DB capacity, I would estimate vector storage from count, dimension, and data type, then add index, metadata, replica, and operational overhead. For latency, I would budget query embedding, ANN search, filtering, reranking, and source fetch. Cost is driven by vector volume, dimension, replicas, topK, filters, embedding, and reranking.
```

---

## 6. Revision Notes

- One-line summary: Vector DB scale is mostly vectors, dimensions, replicas, topK, filters, and rerank cost.
- Three keywords: dimension, p99, cost.
- One trap: forgetting metadata/index overhead in capacity estimates.