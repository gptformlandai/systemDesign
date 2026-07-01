# VectorDB Active Recall Question Bank

> Track File #26 of 30 - Group 06: Practice Upgrade
> For: retrieval practice | Level: beginner to MAANG | Mode: recall and weak-spot detection

Use this after reading. Answer without notes first.

---

## 1. Beginner Recall

1. What is a vector database?
2. What is an embedding?
3. What is vector dimension?
4. Cosine vs dot product?
5. What is topK?
6. What is metadata filtering?
7. What is a namespace?
8. Why does model version matter?
9. When is a vector DB a bad fit?
10. Pinecone index vs Qdrant collection?

---

## 2. Intermediate Recall

1. Why does ANN exist?
2. Explain HNSW.
3. Explain IVF.
4. What is PQ?
5. How does chunk size affect RAG?
6. What metadata fields should a RAG chunk store?
7. How do dense and sparse search differ?
8. What is hybrid search?
9. What does a reranker do?
10. How do you evaluate retrieval quality?

---

## 3. Senior Recall

1. Estimate raw storage for 100M 1536-dimensional float32 vectors.
2. Design tenant isolation for 10 large tenants and 10,000 small tenants.
3. How do you enforce ACLs safely?
4. How do deletes propagate from source to vector index?
5. How do you upgrade an embedding model?
6. What metrics belong on a vector DB dashboard?
7. Debug low recall after chunking change.
8. Debug high p99 after increasing topK.
9. Compare Pinecone, Qdrant, Weaviate, Milvus, pgvector, and Elasticsearch.
10. Design a vector DB SLO.

---

## 4. MAANG Recall

1. Design enterprise RAG document retrieval.
2. Design semantic product search.
3. Design a support chatbot retrieval layer.
4. Design fraud similarity search.
5. Design multimodal image search.
6. Design vector search with tenant ACL safety.
7. Create a capacity and cost model.
8. Create a golden-set evaluation plan.
9. Create an embedding-model migration plan.
10. Explain when not to use a vector DB.

---

## 5. Scorecard

| Score | Meaning |
|---:|---|
| 0 | I cannot answer without notes |
| 1 | I know the definition only |
| 2 | I can explain with an example |
| 3 | I can explain tradeoffs and failure modes |
| 4 | I can answer follow-ups and compare alternatives |

Target:

```text
MAANG-ready = mostly 3s and 4s across embeddings, schema, ANN, filters, RAG, evaluation, scale, security, freshness, and vendor tradeoffs.
```