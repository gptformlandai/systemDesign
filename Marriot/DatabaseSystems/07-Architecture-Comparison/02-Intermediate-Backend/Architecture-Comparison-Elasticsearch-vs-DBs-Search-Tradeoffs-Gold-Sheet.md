# Architecture Comparison Elasticsearch vs Databases Search Tradeoffs - Gold Sheet

> Track File #8 of 30 - Group 02: Intermediate Backend
> For: search/system design interviews | Level: intermediate | Mode: search engine vs source database

## 1. Use Elasticsearch/OpenSearch When

- full-text search matters
- relevance ranking matters
- autocomplete or fuzzy matching matters
- log search and aggregations matter
- faceted navigation is needed
- keyword plus filters plus sorting dominate

---

## 2. Do Not Use It As

- primary financial transaction store
- source of truth for orders/payments
- replacement for relational constraints
- long-term cheap blob archive without lifecycle planning

---

## 3. Common Architecture

```text
source database -> CDC/events -> Elasticsearch index -> search API
```

The production question is index freshness and rebuildability.

---

## 4. Tradeoff Table

| Dimension | Elasticsearch | SQL/MongoDB |
|---|---|---|
| full-text relevance | excellent | limited/basic |
| aggregations/facets | strong | varies |
| source-of-truth correctness | weak fit | strong fit |
| indexing lag | expected risk | usually current primary state |
| operational risks | shards, mappings, hot shards | query/index/schema risks |

---

## 5. Interview Summary

```text
I would use Elasticsearch as a derived search index when users need full-text relevance, facets, autocomplete, fuzzy search, or log exploration. I would keep transactional correctness in a source database and synchronize changes through CDC/events with monitoring for lag, mapping errors, and rebuild paths.
```

---

## 6. Revision Notes

- One-line summary: Elasticsearch is a search engine, not the canonical transaction store.
- Three keywords: relevance, index, freshness.
- One trap: dual-writing to Elasticsearch without replay/rebuild strategy.