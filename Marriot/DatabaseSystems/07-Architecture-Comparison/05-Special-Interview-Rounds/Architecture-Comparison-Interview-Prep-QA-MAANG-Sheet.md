# Architecture Comparison Interview Prep Q&A - MAANG Sheet

> Track File #25 of 30 - Group 05: Special Interview Rounds
> For: interview prep | Level: beginner to MAANG | Mode: direct Q&A

## 1. How do you choose a database in system design?

Start from requirements, access patterns, consistency needs, latency SLO, scale, query flexibility, operations, cost, and failure modes. Then choose source of truth and derived stores.

## 2. SQL vs NoSQL?

SQL is strong for transactions, joins, constraints, and flexible queries. NoSQL families are strong for specialized access patterns like document aggregates, massive partition-key writes, search, graph traversal, or vector similarity.

## 3. PostgreSQL vs MongoDB?

PostgreSQL when relationships, joins, constraints, and transactional correctness dominate. MongoDB when document aggregates and flexible JSON schema fit the access pattern.

## 4. Cassandra vs SQL?

Cassandra when high-volume partition-key reads/writes and availability dominate. SQL when transactions, joins, constraints, and ad hoc querying matter.

## 5. Elasticsearch vs database?

Use Elasticsearch as a derived search index for full-text relevance, facets, logs, and aggregations. Do not make it canonical transactional state.

## 6. Neo4j vs SQL joins?

Use Neo4j when multi-hop relationship traversal and path explanation are core. SQL joins are enough for moderate relational joins.

## 7. Vector DB vs Elasticsearch?

Vector DBs retrieve semantic similarity. Elasticsearch retrieves lexical relevance and structured search. Production RAG often uses both.

## 8. Redis cache vs database?

Redis improves hot-read latency and ephemeral workflows. It is usually derived from a durable source unless designed with persistence and recovery.

## 9. Object storage vs database?

Object storage is for durable cheap blobs, backups, and data lake files. Databases are for indexed records and queryable state.

## 10. Warehouse vs OLTP database?

Warehouses/lakehouses are for analytics scans and aggregations. OLTP databases are for low-latency application transactions.

## 11. How do you handle multiple stores?

Declare source of truth, publish changes through outbox/CDC/events, make derived consumers idempotent, monitor lag, propagate deletes/permissions, and keep rebuild paths.

## 12. What is the safest answer for payments?

Use a transactional relational source of truth with idempotency, constraints, append-only ledger, audit, reconciliation, and derived search/cache/analytics only for reads.

## 13. What is the safest answer for RAG?

Use source documents plus metadata as truth, vector/search/graph as derived retrieval stores, enforce ACLs during retrieval, evaluate quality, monitor freshness, and preserve citations.

## 14. What is the strongest architecture comparison pattern?

Name the source of truth, then name the derived stores and exactly how they sync, fail, and recover.

## 15. What makes an answer MAANG-level?

It covers access pattern, correctness, scale, SLO, failure mode, cost, security, backup/DR, data sync, and why alternatives are weaker.