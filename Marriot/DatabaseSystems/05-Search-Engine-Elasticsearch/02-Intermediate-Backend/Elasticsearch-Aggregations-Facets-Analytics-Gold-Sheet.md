# Elasticsearch Aggregations, Facets, and Analytics - Gold Sheet

> Track File #7 of 27 - Group 02: Intermediate Backend
> For: backend/search/system design interviews | Level: intermediate | Mode: aggregations, facets, analytics patterns

This sheet builds:
- Bucket and metric aggregations
- Faceted navigation
- Analytics tradeoffs and memory risks

---

## 1. Aggregation Mental Model

Aggregations compute summaries over matching documents.

```text
query selects documents -> aggregations bucket/measure those documents
```

Use cases:

- product facets
- log counts over time
- top categories
- average latency
- error-rate breakdown
- price histograms

---

## 2. Terms Aggregation

```json
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": {
        "field": "category",
        "size": 10
      }
    }
  }
}
```

Use `keyword` fields for terms aggregations.

---

## 3. Date Histogram

```json
{
  "size": 0,
  "aggs": {
    "events_over_time": {
      "date_histogram": {
        "field": "@timestamp",
        "fixed_interval": "1h"
      }
    }
  }
}
```

Good for logs, metrics-like events, dashboards, and time-window analysis.

---

## 4. Faceted Search Pattern

```json
{
  "query": {
    "bool": {
      "must": [{ "match": { "title": "running shoes" } }],
      "filter": [{ "term": { "status": "active" } }]
    }
  },
  "aggs": {
    "brands": { "terms": { "field": "brand", "size": 20 } },
    "sizes": { "terms": { "field": "sizes", "size": 20 } },
    "price_ranges": {
      "range": {
        "field": "price",
        "ranges": [
          { "to": 50 },
          { "from": 50, "to": 100 },
          { "from": 100 }
        ]
      }
    }
  }
}
```

---

## 5. Composite Aggregation

Use composite aggregation for paginating buckets.

```text
terms aggregation returns top buckets; composite aggregation lets you page through many buckets.
```

Useful for exports or backfills, not usually interactive user facets.

---

## 6. Risks

- high-cardinality terms can be expensive
- aggregating on wrong field type can fail or use heavy fielddata
- broad queries over many shards can hurt latency
- dashboard workloads can compete with user-facing search
- approximate counts can appear in distributed terms aggregation

---

## 7. Strong Answer

Question:

> How do facets work in Elasticsearch?

Strong answer:

```text
Facets are usually aggregations over the documents matching the current query and filters. For product search, I would run the user query with filters and add terms/range aggregations on keyword or numeric fields such as brand, category, size, and price. I would watch high-cardinality fields and shard fan-out because expensive facets can dominate search latency.
```

---

## 8. Revision Notes

- One-line summary: Aggregations summarize matching documents; facets are user-facing aggregations.
- Three keywords: terms, histogram, composite.
- One interview trap: aggregating on analyzed `text` fields.
- Memory trick: query narrows; aggregation summarizes.