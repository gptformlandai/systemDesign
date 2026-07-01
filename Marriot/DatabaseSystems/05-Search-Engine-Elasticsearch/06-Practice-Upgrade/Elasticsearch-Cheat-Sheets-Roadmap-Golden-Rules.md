# Elasticsearch Cheat Sheets, Roadmap, and Golden Rules - Gold Sheet

> Track File #26 of 27 - Group 06: Practice Upgrade
> For: backend/search/system design interviews | Level: revision and final consolidation | Mode: fast recall, commands, roadmap, rules

This sheet builds:
- Elasticsearch command and design cheat sheets
- Beginner-to-pro roadmap
- Golden rules and final readiness checklist

---

## 1. REST Cheat Sheet

```http
GET _cluster/health
GET _cat/indices?v
PUT products-v1
PUT products-v1/_doc/p1
POST products-v1/_search
POST _bulk
POST _analyze
POST _reindex
GET products-v1/_mapping
GET _cat/shards?v
```

---

## 2. Mapping Cheat Sheet

| Need | Type |
|---|---|
| full-text search | `text` |
| exact filter/sort/agg | `keyword` |
| timestamp | `date` |
| price/count/latency | numeric |
| object arrays with per-object match | `nested` |
| arbitrary metadata | `flattened` |
| location | `geo_point` |
| embeddings | dense vector field |

---

## 3. Query Cheat Sheet

| Query | Use |
|---|---|
| `match` | full-text search |
| `term` | exact match |
| `range` | number/date range |
| `bool.must` | required scoring clause |
| `bool.filter` | required non-scoring clause |
| `multi_match` | multiple fields |
| `search_after` | deep pagination |
| `match_phrase` | ordered phrase search |
| `nested` query | same-object matching inside arrays |
| `geo_distance` | radius-based location filtering |
| `collapse` | group top hits by a field |
| `_explain` / profile | scoring and query-cost debugging |

---

## 4. Golden Rules

1. Keep a source of truth outside Elasticsearch.
2. Use explicit mappings for important indexes.
3. Use `text` for search and `keyword` for exact filters/sorts/aggs.
4. Use filters for mandatory constraints and security.
5. Avoid deep pagination with `from`.
6. Use aliases for zero-downtime reindexing.
7. Define freshness SLOs.
8. Measure relevance with golden queries.
9. Use ILM/data streams for growing time-series data.
10. Monitor shards, heap, slow logs, rejects, merges, and disk.
11. Use dedicated fields for autocomplete instead of wildcard hot paths.
12. Enforce tenant and ACL filters before retrieval, especially for RAG.
13. Treat runtime fields and scripts as temporary or low-volume tools unless proven safe.
14. Use geo radius caps and privacy guardrails for location search.

---

## 5. Beginner To Pro Roadmap

### Stage 1: Beginner

Topics:

- index, document, shard, replica
- REST APIs
- indexing and simple search
- near-real-time refresh

Project: basic product search.

### Stage 2: Intermediate

Topics:

- mappings/analyzers
- query DSL
- aggregations/facets
- ingest/sync
- relevance basics

Project: faceted product search or log search.

### Stage 3: Senior

Topics:

- shards/replicas
- ILM/data streams
- performance debugging
- security/snapshots
- app integration

Project: production log analytics or multi-tenant search.

### Stage 4: MAANG / Pro

Topics:

- vector/hybrid search
- relevance evaluation
- incident runbooks
- capacity planning
- design reviews

Project: RAG retrieval or e-commerce search platform.

---

## 6. Final MAANG Checklist

- I can explain inverted indexes and doc values.
- I can design mappings and analyzers for search requirements.
- I can write bool queries, filters, aggregations, and pagination safely.
- I can tune relevance and evaluate quality.
- I can design sync from a source of truth.
- I can use aliases and reindex without downtime.
- I can explain shards, replicas, refresh, segments, merges, and ILM.
- I can debug slow search, mapping explosion, hot shard, heap pressure, and stale results.
- I can secure tenant and ACL-filtered search.
- I can compare Elasticsearch with SQL, MongoDB, Cassandra, OpenSearch, Solr, and vector databases.

---

## 7. Final Summary

```text
Elasticsearch is a strong choice when search quality, filtering, facets, retrieval, and analytics over indexed documents matter. It is a poor choice as the default transactional source of truth or as a replacement for relational constraints, arbitrary joins, or heavy warehouse analytics.
```