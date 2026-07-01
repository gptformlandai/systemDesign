#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS -X DELETE "$base_url/products-v2" >/dev/null || true

curl -sS -X PUT "$base_url/products-v2" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "autocomplete_analyzer": {
          "tokenizer": "autocomplete_tokenizer",
          "filter": ["lowercase"]
        }
      },
      "tokenizer": {
        "autocomplete_tokenizer": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 12,
          "token_chars": ["letter", "digit"]
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "product_id": { "type": "keyword" },
      "tenant_id": { "type": "keyword" },
      "title": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" },
          "autocomplete": { "type": "text", "analyzer": "autocomplete_analyzer", "search_analyzer": "standard" }
        }
      },
      "brand": { "type": "keyword" },
      "category": { "type": "keyword" },
      "description": { "type": "text" },
      "price": { "type": "double" },
      "in_stock": { "type": "boolean" },
      "rating": { "type": "double" },
      "popularity": { "type": "integer" },
      "created_at": { "type": "date" },
      "updated_at": { "type": "date" },
      "search_version": { "type": "keyword" }
    }
  }
}
JSON

curl -sS -X POST "$base_url/_reindex?refresh=true" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "source": { "index": "products-v1" },
  "dest": { "index": "products-v2" },
  "script": {
    "lang": "painless",
    "source": "ctx._source.search_version = 'v2'"
  }
}
JSON

curl -sS -X POST "$base_url/_aliases" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "actions": [
    { "remove": { "index": "*", "alias": "products-read", "must_exist": false } },
    { "remove": { "index": "*", "alias": "products-write", "must_exist": false } },
    { "add": { "index": "products-v2", "alias": "products-read" } },
    { "add": { "index": "products-v2", "alias": "products-write" } }
  ]
}
JSON

curl -sS "$base_url/_cat/aliases/products-*?v"
curl -sS "$base_url/products-read/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d '{"query":{"term":{"search_version":"v2"}},"_source":["product_id","title","search_version"]}'