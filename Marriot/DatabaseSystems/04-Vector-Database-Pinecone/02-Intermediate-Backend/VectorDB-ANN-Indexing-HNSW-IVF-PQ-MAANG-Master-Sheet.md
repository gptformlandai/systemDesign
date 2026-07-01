# VectorDB ANN Indexing, HNSW, IVF, and PQ - MAANG Master Sheet

> Track File #5 of 30 - Group 02: Intermediate Backend
> For: backend/search/system design interviews | Level: intermediate to senior | Mode: ANN internals and tradeoffs

## 1. Why ANN Exists

Exact nearest-neighbor search over millions or billions of vectors is expensive.

```text
exact search = compare query vector to every vector
ANN search = use an index to find likely nearest vectors faster
```

ANN trades a little recall for much lower latency and cost.

---

## 2. HNSW

HNSW builds a graph of vectors and searches through neighbors.

| Knob | Meaning |
|---|---|
| M | graph connectivity |
| efConstruction | index build quality/cost |
| efSearch | query recall/latency knob |

High `efSearch` usually means better recall and higher latency.

---

## 3. IVF

IVF clusters the vector space and searches selected clusters.

```text
query -> choose nearest clusters -> scan candidates inside clusters
```

Tradeoff:

- fewer clusters scanned: faster, lower recall
- more clusters scanned: slower, higher recall

---

## 4. Product Quantization

PQ compresses vectors to reduce memory and storage.

Tradeoff:

```text
lower memory/cost, possible accuracy loss
```

PQ matters at very large scale or tight cost budgets.

---

## 5. Recall vs Latency

| Choice | Effect |
|---|---|
| exact search | highest recall, high cost |
| ANN | lower latency, approximate recall |
| higher topK | more candidates, higher downstream cost |
| reranking | improves relevance, adds latency |

---

## 6. Interview Summary

```text
Vector databases use ANN indexes because exact search over large vector sets is too expensive. HNSW uses a navigable graph, IVF searches selected clusters, and PQ compresses vectors. The key interview tradeoff is recall versus latency versus cost, validated with retrieval evaluation rather than assumptions.
```

---

## 7. Revision Notes

- One-line summary: ANN indexes buy speed by making nearest-neighbor search approximate.
- Three keywords: HNSW, recall, latency.
- One trap: optimizing p99 latency without measuring recall regression.