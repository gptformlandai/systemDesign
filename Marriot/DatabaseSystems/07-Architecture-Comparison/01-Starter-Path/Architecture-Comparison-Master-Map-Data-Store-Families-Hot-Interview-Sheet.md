# Architecture Comparison Master Map: Data Store Families - Hot Interview Sheet

> Track File #1 of 30 - Group 01: Starter Path
> For: system design interviews | Level: beginner | Mode: datastore families, significance, fit

## 1. Core Idea

Every datastore is optimized for a shape of work.

```text
database choice = workload shape + correctness need + scale profile + operational budget
```

Bad system design answers start with a favorite database. Strong answers start with requirements.

---

## 2. Data Store Family Map

| Family | Examples | Best At | Weak At |
|---|---|---|---|
| relational/SQL | PostgreSQL, MySQL | transactions, joins, integrity, flexible querying | massive write scale without careful partitioning |
| document | MongoDB | aggregate-oriented JSON documents, flexible schema | complex joins and global relational constraints |
| wide-column | Cassandra, DynamoDB-style models | huge writes, predictable key-based queries, high availability | ad hoc joins, flexible querying, strong multi-row transactions |
| search engine | Elasticsearch, OpenSearch | full-text search, relevance, logs, aggregations | source-of-truth transactions |
| graph | Neo4j | relationship traversal, paths, fraud rings, lineage | simple CRUD at massive scale when relationships are not core |
| vector DB | Pinecone, Qdrant | semantic similarity, RAG retrieval, recommendations | exact transactional data and relational joins |
| cache | Redis, Memcached | low-latency derived reads, counters, sessions | durable source of truth unless designed carefully |
| object storage | S3, GCS | cheap durable blobs, data lake files, backups | low-latency record updates and queries |
| time-series | TimescaleDB, InfluxDB, Prometheus | metrics/events over time | arbitrary relational workloads |
| warehouse/lakehouse | Snowflake, BigQuery, Databricks | analytics, reporting, batch queries | low-latency OLTP transactions |

---

## 3. Significance In System Design

| Requirement | Usually Points Toward |
|---|---|
| money movement | relational ledger, transactions, audit |
| product catalog search | search engine plus source database |
| social graph mutual friends | graph or precomputed relational/store projection |
| RAG retrieval | vector DB plus search plus source documents |
| high-volume time-series metrics | time-series store or log/metrics platform |
| massive append writes by key | Cassandra/wide-column style |
| low-latency hot reads | cache in front of source of truth |

---

## 4. Interview Summary

```text
I would choose the datastore from the access pattern and correctness needs. SQL is strong for transactions and joins, document stores for aggregate documents, wide-column stores for high-scale key-based access, search engines for text/relevance, graph DBs for relationship traversal, vector DBs for semantic similarity, caches for derived low-latency reads, object storage for durable blobs, and warehouses/lakehouses for analytics.
```

---

## 5. Revision Notes

- One-line summary: A datastore is a workload optimizer, not a universal answer.
- Three keywords: access pattern, correctness, operations.
- One trap: picking a database before naming the query pattern.