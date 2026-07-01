# Architecture Comparison Cheat Sheets, Roadmap, and Golden Rules

> Track File #29 of 30 - Group 06: Practice Upgrade
> For: revision | Level: beginner to pro | Mode: cheat sheet and roadmap

## 1. Fast Choice Cheat Sheet

| Need | First Candidate |
|---|---|
| transactions and constraints | SQL/PostgreSQL/MySQL |
| JSON aggregate documents | MongoDB/document DB |
| massive key-based writes | Cassandra/wide-column |
| full-text relevance | Elasticsearch/OpenSearch |
| semantic similarity | Vector DB/Pinecone/Qdrant |
| relationship traversal | Neo4j/graph DB |
| hot low-latency reads | Redis/cache |
| blobs and backups | object storage |
| metrics over time | time-series store |
| analytics and reporting | warehouse/lakehouse |

---

## 2. Golden Rules

1. Start with access pattern, not database name.
2. Name the source of truth.
3. Derived stores must be rebuildable.
4. Do not use search/vector/cache as canonical money state.
5. Every derived store needs freshness monitoring.
6. Every cache needs invalidation or staleness policy.
7. Every partition key needs skew analysis.
8. Every multi-store design needs delete and permission propagation.
9. Every production choice needs backup/DR and restore testing.
10. Every interview answer should name one rejected alternative.

---

## 3. 4-Week Roadmap

| Week | Focus |
|---:|---|
| 1 | datastore families, access patterns, source of truth, decision axes |
| 2 | SQL, MongoDB, Cassandra, search, graph, vector comparisons |
| 3 | consistency, scaling, polyglot persistence, SLOs, cost, CDC |
| 4 | scenario designs, anti-patterns, interview Q&A, projects, pro review |

---

## 4. Final Readiness Checklist

- I can choose a datastore from access pattern and correctness needs.
- I can explain pros and cons for SQL, MongoDB, Cassandra, Elasticsearch, Neo4j, VectorDB, Redis, object storage, time-series, and warehouses.
- I can name source of truth and derived stores.
- I can explain sync, CDC, replay, rebuild, and freshness.
- I can cover SLOs, cost, security, backup, DR, and operations.
- I can answer ecommerce, chat/feed, RAG/search, observability, fraud, payments, and analytics scenarios.