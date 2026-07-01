# Elasticsearch Core Architecture: Index, Document, and Shard - Gold Sheet

> Track File #2 of 27 - Group 01: Starter Path
> For: backend/search/system design interviews | Level: beginner to intermediate | Mode: architecture, terminology, data model

This sheet builds:
- Elasticsearch vocabulary
- Cluster, node, index, document, shard, replica, segment, inverted index
- The bridge from SQL tables to search indexes

---

## 1. Core Terms

| Term | Meaning | Interview Importance |
|---|---|---|
| Cluster | Group of Elasticsearch nodes | Scale and availability boundary |
| Node | Server process in the cluster | Stores shards, coordinates queries, performs roles |
| Index | Logical collection of documents | Similar to table plus search structures |
| Document | JSON object stored and indexed | Searchable unit returned to application |
| Field | Named document attribute | Mapping decides type and indexing behavior |
| Primary shard | Partition of an index | Determines distribution and write target |
| Replica shard | Copy of primary shard | Search parallelism and high availability |
| Segment | Immutable Lucene index file | Search unit; merged over time |
| Inverted index | term -> document postings | Core full-text search structure |
| Doc values | columnar field storage | Sorting, aggregations, scripts |

---

## 2. SQL To Elasticsearch Bridge

| SQL Thinking | Elasticsearch Thinking |
|---|---|
| table | index or index family |
| row | document |
| column | field with mapping |
| B-tree index | inverted index / BKD / doc values depending on type |
| JOIN | denormalized document, nested, parent-child rarely, or app-side join |
| WHERE | query/filter DSL |
| GROUP BY | aggregations |
| transaction | source-of-truth write elsewhere, then sync |

Interview trap:

```text
Do not promise relational joins and transactional constraints in Elasticsearch. Design denormalized search documents and keep a real source of truth.
```

---

## 3. Index And Shard Flow

```text
client index request
-> coordinating node
-> routing decides primary shard
-> primary indexes document into Lucene segment buffer
-> replicas receive operation
-> refresh makes document searchable near-real-time
```

Important distinction:

```text
indexed is not always immediately searchable until refresh. Elasticsearch is near-real-time, not strictly instant search.
```

---

## 4. Search Flow

```text
client search request
-> coordinating node
-> query sent to target shards
-> each shard searches local Lucene segments
-> shard returns top results/aggregations
-> coordinator merges and sorts global response
```

Costs depend on:

- number of shards queried
- query selectivity
- sort/aggregation fields
- segment count and merge pressure
- heap and filesystem cache
- result window and pagination strategy

---

## 5. Primary Shards And Replicas

Primary shards partition data. Replica shards copy data and can serve searches.

Tradeoffs:

| More Shards | Fewer Shards |
|---|---|
| more parallelism potential | less overhead |
| smaller shard size | fewer files/segments/cluster-state entries |
| more coordination overhead | less distribution flexibility |

Rule:

```text
Shard count is a capacity and operations decision, not a number to guess casually.
```

---

## 6. Strong Answer

Question:

> What happens when Elasticsearch searches an index?

Strong answer:

```text
The request hits a coordinating node, which sends the query to the relevant shards. Each shard searches its local Lucene segments using structures such as inverted indexes and doc values, returns top hits or aggregation partials, and the coordinator merges the shard results into the final response. That means query latency depends on shard count, shard health, query shape, sort/aggregation cost, cache behavior, and coordinator merge work.
```

---

## 7. Revision Notes

- One-line summary: Elasticsearch indexes documents into Lucene shards and merges shard-local search results globally.
- Three keywords: shard, segment, inverted index.
- One interview trap: assuming one index equals one physical file or node.
- Memory trick: index is logical; shard is distributed; segment is Lucene's search unit.