# Elasticsearch Interview Prep Q&A - MAANG Sheet

> Track File #22 of 27 - Group 05: Special Interview Rounds
> For: backend/search/system design interviews | Level: beginner to MAANG | Mode: direct Q&A, follow-ups, strong answers

This sheet builds:
- Fast interview answers
- Follow-up hooks
- Beginner, intermediate, senior, and MAANG readiness

---

## Beginner Questions

### 1. What is Elasticsearch?

Elasticsearch is a distributed Lucene-based search and analytics engine for indexing JSON documents and querying them with full-text search, filters, aggregations, and retrieval APIs.

### 2. What is an index?

An index is a logical collection of documents with mappings and settings. Physically it is split into shards.

### 3. What is an inverted index?

An inverted index maps terms to documents containing those terms. It is the core structure behind fast full-text search.

### 4. What is a shard?

A shard is a Lucene index that stores part of an Elasticsearch index. Primary shards hold original data; replicas copy primaries for availability and search capacity.

---

## Intermediate Questions

### 5. What is the difference between `text` and `keyword`?

`text` is analyzed for full-text search. `keyword` is exact-value indexing for filters, sorting, and aggregations.

### 6. What is an analyzer?

An analyzer transforms text into tokens using character filters, a tokenizer, and token filters. It controls how text is indexed and searched.

### 7. Query context vs filter context?

Query context scores relevance. Filter context applies exact constraints without scoring and can be cached more effectively.

### 8. Why is deep pagination risky?

`from + size` requires shards to collect and sort skipped hits. Use `search_after` and point-in-time for deep, stable pagination.

---

## Senior Questions

### 9. How do you keep Elasticsearch in sync?

Use source-of-truth DB events, outbox, CDC, Kafka, or controlled application events. Index with deterministic IDs through bulk APIs, monitor failed items and lag, and use aliases for reindexing.

### 10. How do you debug slow search?

Start with query/index alias, shard fan-out, mapping, slow logs, profile API, heap/GC, thread-pool rejections, merges, disk watermarks, and hot shards.

### 11. What is ILM?

Index Lifecycle Management automates rollover, tier movement, and deletion for growing indices such as logs and events.

### 12. Why are replicas not backups?

Replicas protect availability from node/shard failure. Snapshots protect recovery from delete mistakes, bad reindexing, corruption, or cluster-level disaster.

---

## MAANG Deep-Dive Questions

### 13. Design product search.

Use denormalized product documents, explicit mappings, analyzers, bool queries, facets, synonyms, boosts, freshness SLOs for price/inventory, aliases for reindexing, and relevance evaluation.

### 14. Design log analytics.

Use data streams with `@timestamp`, controlled mappings, ILM, date histograms, terms aggregations, dashboards, retention, and ingest monitoring.

### 15. Design RAG retrieval.

Index chunks with lexical fields, dense vectors, tenant/ACL metadata, source info, and version. Apply filters before retrieval, use hybrid search/reranking, and evaluate recall, groundedness, latency, and ACL leak tests.

### 16. Elasticsearch or PostgreSQL for search?

PostgreSQL is source of truth and supports some full-text search. Elasticsearch is better for dedicated search, relevance tuning, facets, scaling search traffic, and analytics-heavy retrieval, but it needs a sync pipeline.

---

## Interview Closing Formula

```text
For Elasticsearch I would first name the search use case. Then I would design the document model, mapping, analyzer, query DSL, relevance signals, sync pipeline, shard/ILM strategy, SLOs, and failure modes. I would also state what remains in the source of truth and when another system is better.
```

---

## Revision Notes

- One-line summary: Elasticsearch interview strength is precise mapping, query, relevance, sync, and operations reasoning.
- Three keywords: mapping, query DSL, shard.
- One interview trap: saying search without explaining relevance and freshness.
- Memory trick: a search answer is not complete until it has sync and ranking.