# Elasticsearch vs SQL, MongoDB, Cassandra, OpenSearch, Solr, and Vector DBs - Tradeoff Analysis Gold Sheet

> Track File #20 of 27 - Group 04: Scenario Practice
> For: backend/search/system design interviews | Level: senior | Mode: tool selection, tradeoffs, alternatives

This sheet builds:
- Clear Elasticsearch comparison language
- When Elasticsearch is right and wrong
- MAANG-level search/database selection judgment

---

## 1. Elasticsearch vs PostgreSQL

| Elasticsearch | PostgreSQL |
|---|---|
| full-text search and faceting at scale | relational source of truth |
| denormalized documents | normalized schema and joins |
| near-real-time indexing | transactional consistency |
| relevance scoring | constraints and ACID transactions |

Use together:

```text
PostgreSQL stores product truth; Elasticsearch serves product search.
```

---

## 2. Elasticsearch vs MongoDB

MongoDB is a document database. Elasticsearch is a search engine.

Use MongoDB for document source-of-truth/application data. Use Elasticsearch when search, relevance, faceting, and analytics queries outgrow database indexes.

---

## 3. Elasticsearch vs Cassandra

Cassandra is a high-scale predictable access-pattern serving store. Elasticsearch is flexible search and analytics over indexed documents.

Use Cassandra for high-write query tables. Use Elasticsearch for text search, facets, logs, and retrieval.

---

## 4. Elasticsearch vs OpenSearch

OpenSearch is a related open-source search/analytics platform with different governance, features, plugins, and compatibility boundaries.

Interview maturity:

```text
I would compare exact versions, licensing, managed-service support, security features, vector capabilities, plugins, and operational tooling rather than treating them as identical.
```

---

## 5. Elasticsearch vs Solr

Both are Lucene-based search platforms. Elasticsearch is often chosen for distributed search, observability ecosystem, and operational tooling; Solr remains strong in some search-heavy organizations.

Choose based on team expertise, ecosystem, feature needs, and operational model.

---

## 6. Elasticsearch vs Vector Databases

Elasticsearch can support vector and hybrid search. Dedicated vector databases may be better when vector workload, indexing algorithm control, scale, or embedding-native features dominate.

Strong answer:

```text
For RAG, Elasticsearch is attractive when I need hybrid lexical plus vector search with metadata filters in one system. I would compare a vector database if vector recall/latency/scale or embedding-specific operations dominate.
```

---

## 7. Revision Notes

- One-line summary: Elasticsearch is a search and retrieval engine, not a universal database replacement.
- Three keywords: source of truth, relevance, alternatives.
- One interview trap: generic NoSQL vs SQL answer.
- Memory trick: use Elasticsearch when search quality is the problem.