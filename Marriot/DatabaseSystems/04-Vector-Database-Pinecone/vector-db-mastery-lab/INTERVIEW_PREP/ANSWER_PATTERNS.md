# VectorDB Answer Patterns

## Design Answer

```text
I start from the retrieval use case. I choose an embedding model and metric, define a vector record schema with stable IDs and metadata, enforce tenant and ACL filters during retrieval, tune topK and reranking, evaluate with golden queries and recall/MRR/citation metrics, and monitor freshness, p99 latency, cost, and security.
```

## Debugging Answer

```text
I start with the exact query, query embedding, metadata filter, topK, collection/index, model version, and reranker. Then I compare against golden-set recall and check recent changes to chunking, embedding model, ANN settings, source freshness, and permissions.
```

## Tradeoff Answer

```text
Vector DBs are strong for semantic similarity retrieval. Elasticsearch is stronger for lexical search and analytics, Neo4j for relationship traversal, pgvector for simple PostgreSQL-centered workloads, and object storage plus batch jobs for offline embedding archives.
```