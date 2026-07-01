# VectorDB Interview Prep Q&A - MAANG Sheet

> Track File #25 of 30 - Group 05: Special Interview Rounds
> For: interview prep | Level: beginner to MAANG | Mode: direct Q&A

## 1. What is a vector database?

A database optimized for storing embeddings and retrieving nearest vectors by similarity, usually with metadata filters and ANN indexes.

## 2. What is an embedding?

A fixed-dimensional numeric representation of text, image, audio, code, or structured features.

## 3. Why ANN instead of exact search?

Exact search compares against every vector and becomes expensive at scale. ANN uses an index to trade small recall loss for much lower latency and cost.

## 4. Cosine vs dot product?

Cosine measures angle similarity. Dot product considers magnitude and direction. Follow the embedding model recommendation.

## 5. What is topK?

The number of nearest candidates returned before optional reranking or filtering stages.

## 6. How do you design vector metadata?

Include tenant, ACL, source, document ID, chunk ID, language, timestamps, embedding version, and filterable product fields.

## 7. How do you secure RAG retrieval?

Apply tenant and ACL filters during retrieval, enforce delete propagation, test permission leaks, and avoid sending unauthorized candidates to rerankers or logs.

## 8. How do you evaluate retrieval?

Use golden queries with recall@K, precision@K, MRR, NDCG, citation correctness, stale-content rate, and permission leak tests.

## 9. How do you upgrade an embedding model?

Create a versioned index/namespace, backfill embeddings, run evaluation, shadow/canary traffic, switch gradually, and keep rollback.

## 10. Pinecone vs Qdrant?

Pinecone is a managed vector DB with low ops burden. Qdrant is open-source, local-friendly, and gives more direct operational control.

## 11. Pinecone vs Elasticsearch vector search?

Pinecone is focused on managed vector retrieval. Elasticsearch is strong when lexical search, analyzers, aggregations, and hybrid search in an existing search platform are central.

## 12. Vector DB vs pgvector?

pgvector is excellent for smaller/simple PostgreSQL-centered workloads. A dedicated vector DB is stronger when scale, latency, filtering, replicas, or vector operations grow.

## 13. Design enterprise RAG.

Chunk documents, embed chunks, store metadata for tenant/ACL/source/version, retrieve with filters, rerank, cite sources, evaluate recall/groundedness/security, and monitor freshness.

## 14. Debug low recall.

Check embedding model, metric, chunking, filters, topK, ANN settings, stale vectors, reranker behavior, and golden-set regression.

## 15. Debug high p99 latency.

Check embedding API latency, topK, filters, index health, corpus size, hot tenants, reranker depth, source fetches, and recent reindexing.