# Architecture Comparison Active Recall Question Bank

> Track File #26 of 30 - Group 06: Practice Upgrade
> For: retrieval practice | Level: beginner to MAANG | Mode: recall and weak-spot detection

Answer without notes first.

---

## 1. Beginner Recall

1. What is a source of truth?
2. What is a derived store?
3. When do you use SQL?
4. When do you use MongoDB?
5. When do you use Cassandra?
6. When do you use Elasticsearch?
7. When do you use Neo4j?
8. When do you use a vector DB?
9. When do you use Redis?
10. When do you use object storage?

---

## 2. Intermediate Recall

1. PostgreSQL vs MongoDB for product catalog?
2. Cassandra vs PostgreSQL for message storage?
3. Elasticsearch vs MongoDB text search?
4. Vector DB vs Elasticsearch for RAG?
5. Neo4j vs SQL joins for permissions?
6. Redis cache invalidation strategies?
7. OLTP vs OLAP?
8. What is a materialized view?
9. What is dual-write risk?
10. What is CDC?

---

## 3. Senior Recall

1. Explain CAP and PACELC in datastore choice.
2. Design source-of-truth and derived stores for ecommerce.
3. Design datastore choices for chat and feeds.
4. Design RAG retrieval architecture.
5. Design observability storage for logs, metrics, and traces.
6. Design fraud risk storage with graph and vector signals.
7. Design payments and ledger storage.
8. Design analytics pipeline from OLTP to warehouse.
9. Debug stale search/vector/cache results.
10. Create a data-store architecture SLO plan.

---

## 4. MAANG Recall

1. Defend PostgreSQL as source of truth plus Elasticsearch as derived index.
2. Defend Cassandra for massive time-series writes and explain tombstone risk.
3. Defend Neo4j for fraud rings and explain when SQL is enough.
4. Defend VectorDB for RAG and explain when Elasticsearch is also needed.
5. Compare Redis cache-aside, write-through, and write-behind.
6. Compare data warehouse, lakehouse, and search engine for analytics.
7. Create a migration plan from one database to a polyglot architecture.
8. Explain how you would rebuild a corrupted derived store.
9. Explain how permission changes propagate to search/vector/graph indexes.
10. Score a system design database choice with pros, cons, and failure modes.

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
MAANG-ready = mostly 3s and 4s across source-of-truth, derived stores, access patterns, consistency, scale, SLOs, cost, security, sync, and scenario design.
```