# Neo4j Active Recall Question Bank

> Track File #26 of 30 - Group 06: Practice Upgrade
> For: backend/data/system design interviews | Level: beginner to MAANG | Mode: retrieval practice and weak-spot detection

Use this sheet after reading topic files. Answer without notes first, then check the source sheets.

---

## 1. Beginner Recall

1. What is Neo4j?
2. What is a node?
3. What is a label?
4. What is a relationship?
5. Why does relationship direction matter?
6. What is a property?
7. What is Cypher?
8. `CREATE` vs `MERGE`?
9. Why should graph queries start from selective anchors?
10. When is Neo4j a bad fit?

---

## 2. Intermediate Recall

1. How do you model from domain questions?
2. What makes a good relationship type?
3. What is a supernode?
4. Why are constraints important?
5. What do indexes do in Neo4j?
6. What is `OPTIONAL MATCH`?
7. What is `UNWIND` used for?
8. What causes Cartesian products?
9. What do `EXPLAIN` and `PROFILE` show?
10. How do you ingest data idempotently?

---

## 3. Senior Recall

1. How do graph algorithms differ from normal traversals?
2. What are centrality, community detection, and pathfinding used for?
3. How do bookmarks help clustered applications?
4. How do you design multi-tenant Neo4j?
5. Why are replicas not backups?
6. How do you scale graph workloads?
7. What makes graph partitioning hard?
8. How do you secure relationship/path data?
9. How do you debug hot nodes?
10. How do you test graph model correctness?

---

## 4. MAANG Recall

1. Design social recommendations with Neo4j.
2. Design fraud ring detection.
3. Design an access-control graph.
4. Design a supply-chain risk graph.
5. Design service-dependency blast-radius analysis.
6. Design data lineage impact analysis.
7. Design GraphRAG with Neo4j and vector search.
8. Debug a slow variable-length traversal.
9. Debug a Cartesian product query plan.
10. Compare Neo4j and PostgreSQL.
11. Compare Neo4j and Elasticsearch for GraphRAG.
12. Design a graph ingestion pipeline from Kafka/CDC.
13. Create a graph SLO and dashboard plan.

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
MAANG-ready = mostly 3s and 4s across modeling, Cypher, constraints, query plans, operations, algorithms, permissions, lineage, GraphRAG, and system design.
```