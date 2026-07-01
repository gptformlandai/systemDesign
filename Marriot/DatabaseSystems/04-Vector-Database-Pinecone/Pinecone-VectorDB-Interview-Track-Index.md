# Pinecone and Vector Database Interview Track Index

This folder is the vector database track for backend engineers, search engineers, GenAI/RAG builders, data platform engineers, production owners, and MAANG-style system design interviews.

Primary track stance:

- Pinecone is the managed-production reference because it is widely recognized in GenAI/RAG system design conversations.
- Qdrant is the hands-on lab engine because it is open-source, Docker-friendly, and feasible for local practice.
- Weaviate, Milvus, pgvector, Elasticsearch/OpenSearch vector search, MongoDB Atlas Vector Search, and Neo4j vector capabilities are covered as tradeoff alternatives.

Use this track if:

- You want beginner-to-pro mastery of embeddings, vector similarity, ANN indexes, metadata filtering, hybrid search, RAG retrieval, multi-tenancy, ACL safety, evaluation, scaling, and production debugging.
- You want to answer interviews beyond slogans like “store embeddings and do cosine search.”
- You want practical labs that run locally without needing a paid Pinecone account.

---

## 1. Learning Style: Beginner To MAANG Loop

Every topic should be learned with this loop:

```text
use case -> embedding model -> record/chunk schema -> index/metric -> metadata filter -> retrieval/rerank -> evaluation -> scale/failure mode -> interview answer
```

Vector DB mastery is not only ANN search. It is choosing the right embedding model, chunking strategy, vector index, metadata design, freshness pipeline, access-control filter, and evaluation loop for a retrieval product.

---

## 2. Track Structure

| Group | Folder | Purpose |
|---:|---|---|
| 1 | `01-Starter-Path` | vector DB mental model, embeddings, metrics, setup, CRUD, namespaces, metadata |
| 2 | `02-Intermediate-Backend` | ANN indexes, chunking/schema, APIs, filtering, RAG pipeline, evaluation |
| 3 | `03-Senior-MAANG` | capacity, scaling, multi-tenancy, ACLs, freshness, observability, hybrid search, vendor internals |
| 4 | `04-Scenario-Practice` | RAG, semantic search, recommendations, support chatbot, fraud/similarity, multimodal search, system design |
| 5 | `05-Special-Interview-Rounds` | anti-patterns, debugging, internals, interview Q&A |
| 6 | `06-Practice-Upgrade` | active recall, labs, projects, cheat sheets, roadmap, pro design review |
| Lab | `vector-db-mastery-lab` | runnable local Qdrant lab with scripts, sample data, projects, runbooks, and interview prep |

---

## 3. Starter Path

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Starter-Path/VectorDB-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md](01-Starter-Path/VectorDB-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md) | mental model, use cases, product fit, interview vocabulary |
| 2 | [01-Starter-Path/VectorDB-Embeddings-Similarity-Metrics-Gold-Sheet.md](01-Starter-Path/VectorDB-Embeddings-Similarity-Metrics-Gold-Sheet.md) | embeddings, cosine/dot/euclidean, normalization, model choice |
| 3 | [01-Starter-Path/VectorDB-Pinecone-Qdrant-Setup-And-Core-Concepts-Gold-Sheet.md](01-Starter-Path/VectorDB-Pinecone-Qdrant-Setup-And-Core-Concepts-Gold-Sheet.md) | Pinecone/Qdrant concepts, collections/indexes, dimensions, local setup |
| 4 | [01-Starter-Path/VectorDB-Records-Namespaces-Metadata-CRUD-Gold-Sheet.md](01-Starter-Path/VectorDB-Records-Namespaces-Metadata-CRUD-Gold-Sheet.md) | vector records, IDs, payload/metadata, namespaces, upsert/query/delete |

Starter target:

- You can explain what a vector DB stores and why ANN search exists.
- You can choose a similarity metric and understand dimension compatibility.
- You can model records with vectors plus metadata and run basic CRUD/search.

---

## 4. Intermediate Backend Path

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Backend/VectorDB-ANN-Indexing-HNSW-IVF-PQ-MAANG-Master-Sheet.md](02-Intermediate-Backend/VectorDB-ANN-Indexing-HNSW-IVF-PQ-MAANG-Master-Sheet.md) | HNSW, IVF, PQ, recall/latency tradeoffs, index build/update cost |
| 6 | [02-Intermediate-Backend/VectorDB-Chunking-Metadata-Schema-Design-Gold-Sheet.md](02-Intermediate-Backend/VectorDB-Chunking-Metadata-Schema-Design-Gold-Sheet.md) | chunking, document schema, payload design, metadata filter planning |
| 7 | [02-Intermediate-Backend/VectorDB-Upsert-Query-Delete-API-Python-Node-Gold-Sheet.md](02-Intermediate-Backend/VectorDB-Upsert-Query-Delete-API-Python-Node-Gold-Sheet.md) | SDK/API usage, batching, idempotency, retries, API boundaries |
| 8 | [02-Intermediate-Backend/VectorDB-Metadata-Filtering-Hybrid-Sparse-Dense-Search-Gold-Sheet.md](02-Intermediate-Backend/VectorDB-Metadata-Filtering-Hybrid-Sparse-Dense-Search-Gold-Sheet.md) | metadata filters, sparse vectors, BM25/vector hybrid, filter selectivity |
| 9 | [02-Intermediate-Backend/VectorDB-RAG-Retrieval-Pipeline-MAANG-Master-Sheet.md](02-Intermediate-Backend/VectorDB-RAG-Retrieval-Pipeline-MAANG-Master-Sheet.md) | RAG retrieval flow, chunk retrieval, reranking, citations, grounding |
| 10 | [02-Intermediate-Backend/VectorDB-Evaluation-Recall-Precision-MRR-NDCG-Gold-Sheet.md](02-Intermediate-Backend/VectorDB-Evaluation-Recall-Precision-MRR-NDCG-Gold-Sheet.md) | offline/online retrieval evaluation, golden sets, metrics, drift checks |

Intermediate target:

- You can design chunking and metadata so retrieval answers product questions.
- You can explain ANN recall versus latency tradeoffs.
- You can evaluate retrieval quality instead of trusting vibes.

---

## 5. Senior / MAANG Path

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-MAANG/VectorDB-Scaling-Capacity-Latency-Cost-MAANG-Master-Sheet.md](03-Senior-MAANG/VectorDB-Scaling-Capacity-Latency-Cost-MAANG-Master-Sheet.md) | capacity math, vector memory/storage, QPS, p99, replicas, cost |
| 12 | [03-Senior-MAANG/VectorDB-Multi-Tenancy-ACL-Security-Gold-Sheet.md](03-Senior-MAANG/VectorDB-Multi-Tenancy-ACL-Security-Gold-Sheet.md) | tenant isolation, namespaces, metadata ACLs, encryption, permission leaks |
| 13 | [03-Senior-MAANG/VectorDB-Freshness-CDC-Sync-Reindexing-Gold-Sheet.md](03-Senior-MAANG/VectorDB-Freshness-CDC-Sync-Reindexing-Gold-Sheet.md) | ingestion freshness, CDC, deletes, embedding upgrades, reindex rollout |
| 14 | [03-Senior-MAANG/VectorDB-Observability-SLO-Incident-Response-Gold-Sheet.md](03-Senior-MAANG/VectorDB-Observability-SLO-Incident-Response-Gold-Sheet.md) | p95/p99, recall checks, filter misses, ingestion lag, runbooks |
| 15 | [03-Senior-MAANG/VectorDB-Hybrid-Search-Reranking-Cross-Encoders-MAANG-Master-Sheet.md](03-Senior-MAANG/VectorDB-Hybrid-Search-Reranking-Cross-Encoders-MAANG-Master-Sheet.md) | dense+sparse retrieval, reranking, learning-to-rank, relevance tuning |
| 16 | [03-Senior-MAANG/VectorDB-Vendor-Architecture-Pinecone-Qdrant-Weaviate-Milvus-Gold-Sheet.md](03-Senior-MAANG/VectorDB-Vendor-Architecture-Pinecone-Qdrant-Weaviate-Milvus-Gold-Sheet.md) | Pinecone vs Qdrant vs Weaviate vs Milvus vs pgvector vs Elasticsearch |

Senior gap-fill addendum:

- [03-Senior-MAANG/VectorDB-Pinecone-Managed-Production-Gap-Fill-MAANG-Sheet.md](03-Senior-MAANG/VectorDB-Pinecone-Managed-Production-Gap-Fill-MAANG-Sheet.md) - Pinecone managed production architecture, serverless/provisioned capacity thinking, namespaces, index lifecycle, API-style examples, and interview traps.

Senior target:

- You can defend production vector search under latency, recall, freshness, cost, and security constraints.
- You can compare managed and self-hosted vector DB choices with concrete tradeoffs.

---

## 6. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/VectorDB-RAG-Document-Retrieval-MAANG-Master-Sheet.md](04-Scenario-Practice/VectorDB-RAG-Document-Retrieval-MAANG-Master-Sheet.md) | enterprise RAG, citations, ACLs, chunk freshness, retrieval eval |
| 18 | [04-Scenario-Practice/VectorDB-Semantic-Product-Search-Recommendations-Gold-Sheet.md](04-Scenario-Practice/VectorDB-Semantic-Product-Search-Recommendations-Gold-Sheet.md) | semantic commerce search, recommendations, personalization, catalog filters |
| 19 | [04-Scenario-Practice/VectorDB-Support-Knowledge-Base-Chatbot-Gold-Sheet.md](04-Scenario-Practice/VectorDB-Support-Knowledge-Base-Chatbot-Gold-Sheet.md) | support KB retrieval, versioning, entitlement, answer quality |
| 20 | [04-Scenario-Practice/VectorDB-Fraud-Risk-Similarity-Identity-Gold-Sheet.md](04-Scenario-Practice/VectorDB-Fraud-Risk-Similarity-Identity-Gold-Sheet.md) | approximate similarity for fraud/risk/entity matching |
| 21 | [04-Scenario-Practice/VectorDB-Multimodal-Image-Audio-Search-Gold-Sheet.md](04-Scenario-Practice/VectorDB-Multimodal-Image-Audio-Search-Gold-Sheet.md) | image/audio/video embeddings, multimodal retrieval, safety filters |
| 22 | [04-Scenario-Practice/VectorDB-System-Design-Case-Studies-MAANG-Master-Sheet.md](04-Scenario-Practice/VectorDB-System-Design-Case-Studies-MAANG-Master-Sheet.md) | 12 case studies across RAG, search, recommendations, multimodal, fraud, observability |
| 23 | [04-Scenario-Practice/VectorDB-vs-Elasticsearch-Neo4j-PostgreSQL-MongoDB-Cassandra-Tradeoff-Gold-Sheet.md](04-Scenario-Practice/VectorDB-vs-Elasticsearch-Neo4j-PostgreSQL-MongoDB-Cassandra-Tradeoff-Gold-Sheet.md) | choosing vector DB vs search engine, graph DB, SQL, document DB, wide-column DB |

Scenario target:

- You can answer vector DB system design prompts with retrieval flow, index design, metadata, evaluation, scaling, security, and alternatives.

---

## 7. Special Interview Rounds

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/VectorDB-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md](05-Special-Interview-Rounds/VectorDB-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md) | anti-patterns and fixes: wrong embeddings, bad chunks, filter leaks, recall regressions, hot tenants |
| 25 | [05-Special-Interview-Rounds/VectorDB-Interview-Prep-QA-MAANG-Sheet.md](05-Special-Interview-Rounds/VectorDB-Interview-Prep-QA-MAANG-Sheet.md) | beginner, intermediate, senior, and MAANG Q&A with crisp answers and follow-ups |

Special-round target:

- You can debug bad retrieval quality and explain vector DB internals clearly.

---

## 8. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 26 | [06-Practice-Upgrade/VectorDB-Active-Recall-Question-Bank.md](06-Practice-Upgrade/VectorDB-Active-Recall-Question-Bank.md) | foundation, intermediate, senior, and MAANG recall prompts by topic |
| 27 | [06-Practice-Upgrade/VectorDB-Hands-On-Exercises-And-Runnable-Mini-Labs.md](06-Practice-Upgrade/VectorDB-Hands-On-Exercises-And-Runnable-Mini-Labs.md) | beginner-to-pro labs for embeddings, upsert, search, filters, RAG, evaluation, incidents |
| 28 | [06-Practice-Upgrade/VectorDB-Mini-Projects-Portfolio.md](06-Practice-Upgrade/VectorDB-Mini-Projects-Portfolio.md) | practical projects with schema, queries, evaluation, scaling, security, and interview discussion |
| 29 | [06-Practice-Upgrade/VectorDB-Cheat-Sheets-Roadmap-Golden-Rules.md](06-Practice-Upgrade/VectorDB-Cheat-Sheets-Roadmap-Golden-Rules.md) | cheat sheets, roadmap, golden rules, mistakes, readiness checklist |
| 30 | [06-Practice-Upgrade/VectorDB-Pro-Gap-Fill-Capacity-Evaluation-SLO-Design-Review.md](06-Practice-Upgrade/VectorDB-Pro-Gap-Fill-Capacity-Evaluation-SLO-Design-Review.md) | pro gaps: capacity worksheet, recall tests, ACL review, cost model, design review |

Practice target:

- You can run labs, build mini projects, evaluate retrieval, and rehearse senior design reviews.

---

## 9. Runnable Lab

Use the consolidated lab when you want runnable practice instead of reading-only notes:

- [vector-db-mastery-lab/README.md](vector-db-mastery-lab/README.md)
- [vector-db-mastery-lab/LEARNING_PATH.md](vector-db-mastery-lab/LEARNING_PATH.md)

Lab target:

- You can run a local vector DB with Docker.
- You can upsert vectors, query by similarity, filter by metadata, evaluate retrieval, and debug incidents.
- You can translate the local Qdrant patterns into Pinecone production answers.

---

## 10. Interview Answer Pattern

For most vector DB interview answers, use this shape:

```text
1. Use case:
   What semantic retrieval problem are we solving?

2. Embedding model:
   What model creates vectors, what dimension does it produce, and how do we handle versioning?

3. Record schema:
   What ID, text/chunk, vector, metadata, tenant, ACL, and source fields are stored?

4. Index and metric:
   Which metric and ANN index fit recall/latency/cost constraints?

5. Retrieval flow:
   How do query embedding, metadata filters, topK, hybrid search, and reranking work?

6. Evaluation:
   How do recall, precision, MRR/NDCG, groundedness, citations, and online metrics prove quality?

7. Operations:
   How do freshness, deletes, reindexing, p99 latency, QPS, capacity, security, and cost work?

8. Alternative:
   When should we use Elasticsearch/OpenSearch, pgvector, graph DB, SQL, or object storage instead?
```

---

## 11. Recommended Study Orders

### 2-Week Practical Path

1. Starter Path files 1-4.
2. ANN indexing, chunking/schema, API, metadata filtering files 5-8.
3. RAG pipeline and evaluation files 9-10.
4. Active recall and hands-on labs.

### 4-Week MAANG Path

1. Week 1: vector DB basics, embeddings, metrics, setup, records, namespaces, metadata.
2. Week 2: ANN indexes, chunking, APIs, filters, hybrid retrieval, RAG pipeline, evaluation.
3. Week 3: scaling, multi-tenancy, ACLs, freshness, observability, reranking, Pinecone managed production, vendor tradeoffs.
4. Week 4: scenario sheets, anti-pattern debugging, interview Q&A, projects, pro design review.

### Production Debugging Path

1. Read embeddings, records, ANN indexes, filters, RAG, and evaluation.
2. Read scaling, security, freshness, observability, and vendor architecture.
3. Practice incidents: low recall, high p99, stale vectors, metadata leak, hot tenant, embedding-dimension mismatch, bad reranker.
4. Score yourself with active recall and interview prep.

---

## 12. Readiness Gate

You are vector DB interview-ready when you can do all of this without notes:

- Explain embeddings, vector dimensions, similarity metrics, and ANN search.
- Design a vector record schema with tenant, ACL, source, chunk, model version, and metadata fields.
- Explain Pinecone concepts like index, namespace, serverless/pod-style capacity, upsert, query, filter, and delete.
- Run local vector DB labs with Qdrant and map the same concepts to Pinecone.
- Explain HNSW, IVF, PQ, recall/latency/cost, and index build/update tradeoffs.
- Design RAG retrieval with chunking, metadata filters, reranking, citations, groundedness, and evaluation.
- Debug low recall, irrelevant results, stale embeddings, slow queries, filter leaks, hot tenants, and bad chunking.
- Compare Pinecone, Qdrant, Weaviate, Milvus, pgvector, Elasticsearch/OpenSearch, MongoDB Atlas Vector Search, and graph/vector hybrid systems.
- Give a system design answer that includes embedding model, vector schema, index/metric, retrieval flow, evaluation, scaling, security, freshness, failure modes, and alternatives.