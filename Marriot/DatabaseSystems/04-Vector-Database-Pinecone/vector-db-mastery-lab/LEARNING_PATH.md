# VectorDB Mastery Lab Learning Path

This path turns vector databases from a GenAI buzzword into a retrieval system you can design, operate, debug, and defend in interviews.

---

## Stage 1: Starter Foundations

Read:

- `../01-Starter-Path/VectorDB-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md`
- `../01-Starter-Path/VectorDB-Embeddings-Similarity-Metrics-Gold-Sheet.md`
- `../01-Starter-Path/VectorDB-Pinecone-Qdrant-Setup-And-Core-Concepts-Gold-Sheet.md`
- `../01-Starter-Path/VectorDB-Records-Namespaces-Metadata-CRUD-Gold-Sheet.md`

Run:

```bash
docker compose up -d
bash SCRIPTS/wait-for-qdrant.sh
bash SCRIPTS/reset-lab.sh
```

Lab:

- [LABS/01-first-collection-upsert.md](LABS/01-first-collection-upsert.md)
- [LABS/02-similarity-search.md](LABS/02-similarity-search.md)
- [CHEATSHEETS/API.md](CHEATSHEETS/API.md)

---

## Stage 2: Backend Retrieval Design

Read:

- `../02-Intermediate-Backend/VectorDB-ANN-Indexing-HNSW-IVF-PQ-MAANG-Master-Sheet.md`
- `../02-Intermediate-Backend/VectorDB-Chunking-Metadata-Schema-Design-Gold-Sheet.md`
- `../02-Intermediate-Backend/VectorDB-Upsert-Query-Delete-API-Python-Node-Gold-Sheet.md`
- `../02-Intermediate-Backend/VectorDB-Metadata-Filtering-Hybrid-Sparse-Dense-Search-Gold-Sheet.md`
- `../02-Intermediate-Backend/VectorDB-RAG-Retrieval-Pipeline-MAANG-Master-Sheet.md`
- `../02-Intermediate-Backend/VectorDB-Evaluation-Recall-Precision-MRR-NDCG-Gold-Sheet.md`

Lab:

- [LABS/03-metadata-filtering-acl.md](LABS/03-metadata-filtering-acl.md)
- [LABS/04-rag-retrieval.md](LABS/04-rag-retrieval.md)
- [LABS/05-evaluation.md](LABS/05-evaluation.md)
- [LABS/07-hybrid-fusion-rerank.md](LABS/07-hybrid-fusion-rerank.md)
- [CHEATSHEETS/MODELING.md](CHEATSHEETS/MODELING.md)

---

## Stage 3: Senior Production VectorDB

Read:

- `../03-Senior-MAANG/VectorDB-Scaling-Capacity-Latency-Cost-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/VectorDB-Multi-Tenancy-ACL-Security-Gold-Sheet.md`
- `../03-Senior-MAANG/VectorDB-Freshness-CDC-Sync-Reindexing-Gold-Sheet.md`
- `../03-Senior-MAANG/VectorDB-Observability-SLO-Incident-Response-Gold-Sheet.md`
- `../03-Senior-MAANG/VectorDB-Hybrid-Search-Reranking-Cross-Encoders-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/VectorDB-Vendor-Architecture-Pinecone-Qdrant-Weaviate-Milvus-Gold-Sheet.md`

Lab:

- [LABS/06-incident-debugging.md](LABS/06-incident-debugging.md)
- [RUNBOOKS/LOW_RECALL.md](RUNBOOKS/LOW_RECALL.md)
- [RUNBOOKS/HIGH_P99_LATENCY.md](RUNBOOKS/HIGH_P99_LATENCY.md)
- [RUNBOOKS/STALE_VECTORS.md](RUNBOOKS/STALE_VECTORS.md)
- [RUNBOOKS/METADATA_ACL_LEAK.md](RUNBOOKS/METADATA_ACL_LEAK.md)
- [RUNBOOKS/EMBEDDING_DIMENSION_MISMATCH.md](RUNBOOKS/EMBEDDING_DIMENSION_MISMATCH.md)
- [CHEATSHEETS/OPERATIONS.md](CHEATSHEETS/OPERATIONS.md)

---

## Stage 4: Scenario Design

Read:

- `../04-Scenario-Practice/VectorDB-RAG-Document-Retrieval-MAANG-Master-Sheet.md`
- `../04-Scenario-Practice/VectorDB-Semantic-Product-Search-Recommendations-Gold-Sheet.md`
- `../04-Scenario-Practice/VectorDB-Support-Knowledge-Base-Chatbot-Gold-Sheet.md`
- `../04-Scenario-Practice/VectorDB-Fraud-Risk-Similarity-Identity-Gold-Sheet.md`
- `../04-Scenario-Practice/VectorDB-Multimodal-Image-Audio-Search-Gold-Sheet.md`
- `../04-Scenario-Practice/VectorDB-System-Design-Case-Studies-MAANG-Master-Sheet.md`
- `../04-Scenario-Practice/VectorDB-vs-Elasticsearch-Neo4j-PostgreSQL-MongoDB-Cassandra-Tradeoff-Gold-Sheet.md`

Projects:

- [PROJECTS/01-enterprise-rag.md](PROJECTS/01-enterprise-rag.md)
- [PROJECTS/02-semantic-product-search.md](PROJECTS/02-semantic-product-search.md)
- [PROJECTS/03-support-chatbot.md](PROJECTS/03-support-chatbot.md)
- [PROJECTS/04-multimodal-search.md](PROJECTS/04-multimodal-search.md)

---

## Stage 5: Interview Readiness

Read:

- `../05-Special-Interview-Rounds/VectorDB-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md`
- `../05-Special-Interview-Rounds/VectorDB-Interview-Prep-QA-MAANG-Sheet.md`
- `../06-Practice-Upgrade/VectorDB-Active-Recall-Question-Bank.md`
- `../06-Practice-Upgrade/VectorDB-Hands-On-Exercises-And-Runnable-Mini-Labs.md`
- `../06-Practice-Upgrade/VectorDB-Mini-Projects-Portfolio.md`
- `../06-Practice-Upgrade/VectorDB-Cheat-Sheets-Roadmap-Golden-Rules.md`
- `../06-Practice-Upgrade/VectorDB-Pro-Gap-Fill-Capacity-Evaluation-SLO-Design-Review.md`

MAANG deep-dive gate:

- Defend embedding model, dimension, metric, schema, metadata, filters, and topK.
- Explain ANN recall/latency/cost tradeoffs.
- Debug low recall, high p99, stale vectors, ACL leaks, and dimension mismatches.
- Compare Pinecone, Qdrant, Weaviate, Milvus, pgvector, Elasticsearch/OpenSearch, MongoDB Atlas Vector Search, and Neo4j hybrid patterns.
- Design at least 4 portfolio projects end to end.