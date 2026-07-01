# VectorDB Cheat Sheets, Roadmap, and Golden Rules

> Track File #29 of 30 - Group 06: Practice Upgrade
> For: revision | Level: beginner to pro | Mode: cheat sheet and roadmap

## 1. Core Cheat Sheet

| Concept | One-Line Meaning |
|---|---|
| embedding | numeric meaning representation |
| dimension | vector length |
| metric | similarity calculation |
| ANN | approximate nearest-neighbor search |
| topK | number of candidates returned |
| metadata filter | structured retrieval constraint |
| namespace | logical partition |
| reranker | second-stage relevance scorer |
| golden set | labeled retrieval test set |

---

## 2. Golden Rules

1. Choose embedding model before index dimension.
2. Store tenant, ACL, source, timestamp, and model version metadata.
3. Apply permissions during retrieval.
4. Evaluate recall before changing chunking, model, filters, or ANN settings.
5. Track delete propagation.
6. Do not use vector DB as the primary transactional database.
7. Use hybrid search when exact terms matter.
8. Budget reranker latency and cost.
9. Keep rollback path for embedding upgrades.
10. Compare alternatives honestly.

---

## 3. 4-Week Roadmap

| Week | Focus |
|---:|---|
| 1 | embeddings, metrics, records, Pinecone/Qdrant concepts |
| 2 | ANN, chunking, APIs, filters, RAG, evaluation |
| 3 | scale, security, freshness, observability, reranking, vendors |
| 4 | scenarios, anti-patterns, labs, projects, design reviews |

---

## 4. Final Readiness Checklist

- I can explain embeddings and metrics.
- I can design vector metadata for RAG.
- I can explain ANN recall/latency tradeoffs.
- I can design tenant/ACL-safe retrieval.
- I can evaluate recall, MRR, groundedness, citations, and permission leaks.
- I can debug low recall and high p99 latency.
- I can compare Pinecone, Qdrant, Weaviate, Milvus, pgvector, and Elasticsearch.
- I can run the local lab and map it to Pinecone concepts.