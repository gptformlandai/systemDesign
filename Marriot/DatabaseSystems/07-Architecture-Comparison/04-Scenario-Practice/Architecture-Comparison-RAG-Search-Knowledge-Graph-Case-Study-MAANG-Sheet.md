# Architecture Comparison RAG, Search, and Knowledge Graph Case Study - MAANG Sheet

> Track File #19 of 30 - Group 04: Scenario Practice
> For: GenAI/RAG/search interviews | Level: MAANG | Mode: retrieval architecture

## 1. Workloads

- document ingestion
- chunk retrieval
- keyword search
- semantic search
- entity/relationship traversal
- citations and provenance
- ACL-safe retrieval

---

## 2. Store Choices

| Workflow | Strong Choice | Why |
|---|---|---|
| raw documents | object storage/document DB | source content and blobs |
| metadata/source of truth | SQL/MongoDB | document ownership and permissions |
| full-text search | Elasticsearch/OpenSearch | exact terms, BM25, facets |
| semantic retrieval | Vector DB/Pinecone/Qdrant | embedding similarity |
| entity relationships | Neo4j/graph DB | paths, lineage, entity expansion |
| evaluation logs | warehouse/lakehouse | offline analysis |

---

## 3. Production Risks

- permission leaks
- stale documents
- stale embeddings
- wrong citation
- graph/entity drift
- search/vector disagreement
- high reranker cost

---

## 4. Strong Interview Answer

```text
For RAG, I would keep raw documents and metadata in authoritative stores, build Elasticsearch for lexical search, a vector DB for semantic retrieval, and optionally Neo4j for entity/relationship expansion. All derived stores must receive CDC or ingestion events, enforce tenant/ACL filters during retrieval, and be evaluated with recall, citation correctness, groundedness, freshness, and permission leak tests.
```

---

## 5. Revision Notes

- One-line summary: RAG architecture is source documents plus derived search/vector/graph retrieval.
- Three keywords: ACL, citation, evaluation.
- One trap: sending unauthorized chunks to an LLM or reranker.