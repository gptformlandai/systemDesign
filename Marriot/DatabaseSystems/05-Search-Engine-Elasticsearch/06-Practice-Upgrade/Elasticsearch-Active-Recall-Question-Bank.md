# Elasticsearch Active Recall Question Bank

> Track File #23 of 27 - Group 06: Practice Upgrade
> For: backend/search/system design interviews | Level: beginner to MAANG | Mode: retrieval practice, weak-spot detection

Use this sheet after reading topic files. Answer without notes first, then check the source sheets.

---

## 1. Beginner Recall

1. What is Elasticsearch?
2. What is an index?
3. What is a document?
4. What is a shard?
5. What is a replica?
6. What is an inverted index?
7. What does near-real-time mean?
8. Why is Elasticsearch usually not the source of truth?
9. What is Kibana Dev Tools used for?
10. What does `_analyze` show?

---

## 2. Intermediate Recall

1. Explain `text` vs `keyword`.
2. What is an analyzer?
3. What are multi-fields?
4. When do you use `nested`?
5. Query context vs filter context?
6. `match` vs `term`?
7. Why is deep pagination risky?
8. How do facets work?
9. Why are high-cardinality aggregations expensive?
10. How do aliases help reindexing?
11. Why are wildcard and regex queries risky on hot paths?
12. What does field collapse solve?
13. When would a runtime field be acceptable?

---

## 3. Senior Recall

1. How do you choose shard count?
2. What causes hot shards?
3. What is ILM?
4. What are data streams?
5. How do refresh interval and merge pressure affect performance?
6. How do you debug slow search?
7. What is mapping explosion?
8. Why are replicas not backups?
9. How do you secure multi-tenant search?
10. How do you keep Elasticsearch synced with the source of truth?

---

## 4. MAANG Recall

1. Design e-commerce product search.
2. Design log analytics with data streams and ILM.
3. Design RAG hybrid retrieval with ACL filters.
4. Debug a relevance regression.
5. Debug high heap after a deployment.
6. Debug stale price/inventory search results.
7. Compare Elasticsearch and PostgreSQL for search.
8. Compare Elasticsearch and a vector database for RAG.
9. Design a zero-downtime reindexing migration.
10. Create a search SLO and dashboard plan.
11. Design autocomplete/typeahead with latency guardrails.
12. Design geospatial place search with radius and privacy limits.
13. Design multi-tenant search with tenant isolation and ACL filters.
14. Explain how you would test RAG retrieval for permission leaks.

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
MAANG-ready = mostly 3s and 4s across mappings, query DSL, relevance, sync, shards, operations, and system design.
```