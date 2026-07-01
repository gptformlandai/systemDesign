#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS -X GET "$base_url/products-read/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "query": {
    "bool": {
      "must": [{ "multi_match": { "query": "running headphones", "fields": ["title^3", "description"] } }],
      "filter": [{ "term": { "tenant_id": "t1" } }, { "term": { "in_stock": true } }]
    }
  },
  "highlight": { "fields": { "title": {}, "description": {} } }
}
JSON

curl -sS -X GET "$base_url/products-read/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "query": { "match": { "title.autocomplete": "mech key" } },
  "size": 5
}
JSON