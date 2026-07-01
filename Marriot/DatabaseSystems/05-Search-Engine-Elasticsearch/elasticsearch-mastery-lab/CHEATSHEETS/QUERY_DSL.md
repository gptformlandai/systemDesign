# Elasticsearch Query DSL Cheatsheet

## Bool Query

```json
{
  "query": {
    "bool": {
      "must": [{ "match": { "title": "keyboard" } }],
      "filter": [{ "term": { "tenant_id": "t1" } }]
    }
  }
}
```

## Aggregation

```json
{
  "size": 0,
  "aggs": {
    "brands": { "terms": { "field": "brand" } }
  }
}
```

## Deep Pagination

```json
{
  "size": 20,
  "sort": [{ "updated_at": "desc" }, { "product_id": "asc" }],
  "search_after": ["2026-07-01T10:00:00Z", "p1"]
}
```