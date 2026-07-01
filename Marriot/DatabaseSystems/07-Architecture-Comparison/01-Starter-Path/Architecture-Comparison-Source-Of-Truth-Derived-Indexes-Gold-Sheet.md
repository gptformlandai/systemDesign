# Architecture Comparison Source Of Truth and Derived Indexes - Gold Sheet

> Track File #4 of 30 - Group 01: Starter Path
> For: system design interviews | Level: beginner to intermediate | Mode: ownership, CDC, derived stores

## 1. Core Idea

Not every datastore owns canonical state.

```text
source of truth = system that owns correctness
derived store = optimized copy for a query pattern
```

Search indexes, caches, vector indexes, graph projections, and warehouses are often derived.

---

## 2. Examples

| Workflow | Source Of Truth | Derived Stores |
|---|---|---|
| ecommerce orders | PostgreSQL/orders service | Elasticsearch for search, warehouse for analytics |
| product catalog | PostgreSQL or MongoDB | Elasticsearch, Redis cache, vector DB for semantic search |
| RAG docs | object storage/document DB | vector DB, search index, graph projection |
| social feed | user/content DB | Redis cache, feed materialization store |
| observability | log/metric ingestion stream | search index, time-series store, cold object storage |

---

## 3. Derived Store Failure Modes

- stale index
- dual-write inconsistency
- cache serving deleted data
- vector index missing permission update
- search index mapping drift
- warehouse report behind reality

---

## 4. Sync Patterns

| Pattern | Notes |
|---|---|
| CDC | reliable source-to-derived propagation |
| event stream | services publish change events |
| batch rebuild | useful for analytics/search/vector rebuilds |
| cache-aside | app populates cache on read miss |
| write-through | app writes cache and source path carefully |

---

## 5. Interview Summary

```text
I would make the transactional database or authoritative object store the source of truth, then use search, cache, vector, graph, or warehouse systems as derived stores for specialized access patterns. The key production question is how changes, deletes, permissions, and rebuilds propagate safely.
```

---

## 6. Revision Notes

- One-line summary: Derived stores optimize reads but introduce freshness risk.
- Three keywords: source, derived, CDC.
- One trap: dual-writing to two databases without a recovery plan.