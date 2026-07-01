# VectorDB vs Elasticsearch, Neo4j, PostgreSQL, MongoDB, and Cassandra - Tradeoff Gold Sheet

> Track File #23 of 30 - Group 04: Scenario Practice
> For: architecture interviews | Level: senior | Mode: database tradeoffs

## 1. Vector DB vs Elasticsearch/OpenSearch

Use vector DB when semantic ANN retrieval is the center of the workload.

Use Elasticsearch/OpenSearch when lexical search, analyzers, aggregations, observability logs, and hybrid search in existing search infrastructure are central.

---

## 2. Vector DB vs Neo4j

Use vector DB for semantic similarity.

Use Neo4j when explicit relationships, paths, graph traversal, lineage, or knowledge graph reasoning dominate.

Hybrid GraphRAG may use both.

---

## 3. Vector DB vs PostgreSQL/pgvector

Use pgvector when data size/QPS are modest and operational simplicity matters.

Use a dedicated vector DB when scale, latency, filtering, replicas, managed operations, or vector-specific features outgrow PostgreSQL.

---

## 4. Vector DB vs MongoDB Atlas Vector Search

Use MongoDB vector search when vectors are closely tied to document data already in MongoDB.

Use dedicated vector DB when vector retrieval is a separate high-scale service with specialized ops and evaluation needs.

---

## 5. Vector DB vs Cassandra

Cassandra is strong for massive write-heavy key-value/time-series access patterns.

Vector DBs are strong for nearest-neighbor similarity retrieval.

They solve different problems.

---

## 6. Interview Summary

```text
I would choose a vector DB when semantic nearest-neighbor retrieval is the primary access pattern. I would choose Elasticsearch/OpenSearch for rich lexical search and analytics, Neo4j for explicit graph traversal, pgvector for simple PostgreSQL-based vector workloads, MongoDB vector search when document locality matters, and Cassandra for high-scale partition-key access rather than semantic similarity.
```

---

## 7. Revision Notes

- One-line summary: Vector DBs optimize semantic similarity, not every data problem.
- Three keywords: semantic, lexical, graph.
- One trap: using vector DB as the source of truth for transactional data.