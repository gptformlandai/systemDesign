# VectorDB RAG Retrieval Pipeline - MAANG Master Sheet

> Track File #9 of 30 - Group 02: Intermediate Backend
> For: GenAI/backend/system design interviews | Level: intermediate to senior | Mode: retrieval pipeline, citations, grounding

## 1. RAG Retrieval Flow

```text
user question -> query rewrite -> query embedding -> vector/hybrid retrieval -> ACL filter -> rerank -> context pack -> LLM answer -> citations/eval
```

A vector DB usually handles candidate retrieval, not the full RAG product.

---

## 2. Retrieval Stages

| Stage | Purpose |
|---|---|
| query rewrite | clarify user intent |
| dense retrieval | semantic candidates |
| metadata filter | tenant, ACL, freshness, type |
| sparse retrieval | exact terms and identifiers |
| rerank | improve final order |
| context packing | fit useful evidence into token budget |
| citation mapping | show source and chunk provenance |

---

## 3. RAG Quality Risks

- irrelevant retrieved chunks
- missing authoritative chunk
- stale content
- permission leak
- duplicate chunks
- citation mismatch
- hallucination despite good retrieval

---

## 4. Retrieval Parameters

| Parameter | Tradeoff |
|---|---|
| topK | more candidates vs latency/token cost |
| score threshold | precision vs missed recall |
| chunk size | context vs noise |
| reranker depth | quality vs latency/cost |
| metadata filters | safety/focus vs recall |

---

## 5. Interview Summary

```text
For RAG, I would design vector retrieval as one stage in a larger pipeline: query embedding, tenant/ACL filters, topK retrieval, hybrid signals when needed, reranking, context packing, citations, and evaluation. The production goal is not just nearest vectors; it is grounded, authorized, fresh, useful answers.
```

---

## 6. Revision Notes

- One-line summary: RAG quality depends on retrieval, filtering, reranking, and evaluation.
- Three keywords: retrieval, rerank, citations.
- One trap: measuring only LLM answer quality without retrieval-level metrics.