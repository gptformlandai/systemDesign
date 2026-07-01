#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS -X GET "$base_url/products-read/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "size": 0,
  "query": { "term": { "tenant_id": "t1" } },
  "aggs": {
    "brands": { "terms": { "field": "brand", "size": 10 } },
    "categories": { "terms": { "field": "category", "size": 10 } },
    "price_ranges": {
      "range": {
        "field": "price",
        "ranges": [{ "to": 100 }, { "from": 100, "to": 150 }, { "from": 150 }]
      }
    }
  }
}
JSON

curl -sS -X GET "$base_url/logs-app-000001/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "size": 0,
  "aggs": {
    "events_over_time": { "date_histogram": { "field": "@timestamp", "fixed_interval": "1m" } },
    "by_level": { "terms": { "field": "level" } }
  }
}
JSON