# Cassandra vs SQL, MongoDB, DynamoDB, Kafka, and Search - Tradeoff Analysis Gold Sheet

> Track File #19 of 25 - Group 04: Scenario Practice
> For: backend/database/system design interviews | Level: senior | Mode: tool selection, tradeoffs, alternatives

This sheet builds:
- Clear Cassandra comparison language
- When Cassandra is right and wrong
- MAANG-level database selection judgment

---

## 1. Cassandra vs PostgreSQL

| Cassandra | PostgreSQL |
|---|---|
| predictable partition-key queries | flexible relational queries |
| denormalized query tables | normalized schemas and joins |
| tunable consistency | strong ACID transaction model |
| high write scale across nodes | rich SQL and constraints |
| operational complexity at scale | simpler for many OLTP apps |

Choose PostgreSQL when joins, constraints, transactions, and evolving queries dominate.

Choose Cassandra when write scale, availability, predictable access paths, and distributed retention dominate.

---

## 2. Cassandra vs MongoDB

| Cassandra | MongoDB |
|---|---|
| wide-column, partition-key-first | document/aggregate model |
| query tables by access pattern | flexible nested documents |
| strong for high-write time-series | strong for document-centric apps |
| no general joins | limited joins via aggregation `$lookup` |
| operational focus on compaction/repair | operational focus on indexes/replication/sharding |

Choose MongoDB for document aggregates and flexible application data.

Choose Cassandra for extreme write throughput and predictable large-scale partitioned histories.

---

## 3. Cassandra vs DynamoDB

| Cassandra | DynamoDB |
|---|---|
| self-managed or managed Cassandra options | fully managed AWS-native service |
| tunable topology and operations control | operationally simpler for AWS teams |
| CQL and Cassandra ecosystem | partition/sort key plus GSIs/LSIs |
| repair/compaction/backups need ownership | capacity/cost/throttling model is provider-specific |

Choose DynamoDB when AWS-native managed simplicity and service integration matter more.

Choose Cassandra when open-source portability, multi-cloud/self-managed control, or Cassandra-specific ecosystem is required.

---

## 4. Cassandra vs Kafka

Kafka is a durable event log. Cassandra is a queryable serving store.

Use together:

```text
Kafka ingests ordered event streams.
Cassandra stores query-shaped read models for APIs.
```

Do not use Cassandra as Kafka when you need ordered stream processing and consumer offsets.

Do not use Kafka as Cassandra when you need low-latency point/range query serving over retained data.

---

## 5. Cassandra vs Elasticsearch/OpenSearch

Search engines are built for full-text search, relevance, flexible filters, and inverted indexes.

Cassandra is built for primary-key-shaped access at scale.

Use together:

```text
Cassandra stores source/query tables.
Search engine serves text/filter search.
Events/CDC keep them synchronized.
```

---

## 6. Cassandra vs Time-Series Databases

Cassandra can store time-series data well, especially high-write raw events. Specialized time-series databases may offer better compression, downsampling, query language, and aggregate analytics.

Choose Cassandra when:

- write volume is huge
- access is by entity/time bucket
- retention is simple
- serving reads are predictable

Choose a time-series/OLAP system when:

- rollups and aggregations dominate
- ad hoc time-window analytics are required
- compression and downsampling features matter

---

## 7. Strong Answer

Question:

> Cassandra or PostgreSQL for an order service?

Strong answer:

```text
For the transactional source of truth of orders, I would usually choose PostgreSQL because orders need constraints, transactions, relational integrity, and flexible operational queries. I might use Cassandra as a denormalized read model for high-volume order event history, status timelines, or audit logs where access patterns are predictable. Cassandra is not my default replacement for relational order state.
```

---

## 8. Revision Notes

- One-line summary: Cassandra is excellent for predictable high-scale access paths, not as a universal database replacement.
- Three keywords: query model, availability, alternatives.
- One interview trap: generic NoSQL vs SQL answer.
- Memory trick: choose Cassandra for the access pattern, not for the label NoSQL.