# Architecture Comparison VectorDB vs Search vs Graph vs SQL - Gold Sheet

> Track File #10 of 30 - Group 02: Intermediate Backend
> For: GenAI/search/system design interviews | Level: intermediate | Mode: vector DB tradeoffs

## 1. Use VectorDB When

- semantic similarity is the core retrieval pattern
- RAG needs chunk embeddings
- recommendations use embedding similarity
- multimodal search is needed
- approximate nearest neighbor search is acceptable

---

## 2. Compare To Alternatives

| Need | Better Fit |
|---|---|
| exact keyword and filters | Elasticsearch/OpenSearch |
| explicit relationships and paths | Neo4j |
| transactions and source of truth | SQL/PostgreSQL |
| document source data | MongoDB/object storage |
| simple small vector workload in app DB | pgvector |

---

## 3. VectorDB Risks

- wrong embedding model
- stale embeddings
- missing ACL filters
- poor chunking
- low recall hidden by demos
- high reranker cost
- model migration problems

---

## 4. Interview Summary

```text
I would use a vector database when semantic nearest-neighbor retrieval is central, such as RAG, recommendations, or multimodal search. I would not use it as the source of truth. For production, I would pair it with a source store, metadata filters, evaluation, freshness pipeline, and reranking or hybrid search when exact terms matter.
```

---

## 5. Revision Notes

- One-line summary: Vector DBs retrieve meaning-similar candidates, not authoritative state.
- Three keywords: embedding, topK, evaluation.
- One trap: using vector search where exact filters or graph paths are the real problem.