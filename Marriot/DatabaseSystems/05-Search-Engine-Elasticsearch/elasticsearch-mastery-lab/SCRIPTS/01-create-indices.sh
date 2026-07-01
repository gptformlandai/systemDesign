#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS -X DELETE "$base_url/products-v1" >/dev/null || true
curl -sS -X DELETE "$base_url/logs-app-000001" >/dev/null || true
curl -sS -X DELETE "$base_url/rag-chunks-v1" >/dev/null || true

curl -sS -X PUT "$base_url/products-v1" \
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
      "updated_at": { "type": "date" }
    }
  }
}
JSON

curl -sS -X POST "$base_url/_aliases" \
  -H 'Content-Type: application/json' \
  -d '{"actions":[{"add":{"index":"products-v1","alias":"products-read"}},{"add":{"index":"products-v1","alias":"products-write"}}]}'

curl -sS -X PUT "$base_url/logs-app-000001" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
  "mappings": {
    "dynamic": false,
    "properties": {
      "@timestamp": { "type": "date" },
      "service": { "type": "keyword" },
      "level": { "type": "keyword" },
      "trace_id": { "type": "keyword" },
      "tenant_id": { "type": "keyword" },
      "message": { "type": "text" },
      "status_code": { "type": "integer" },
      "duration_ms": { "type": "integer" }
    }
  }
}
JSON

curl -sS -X PUT "$base_url/rag-chunks-v1" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "chunk_id": { "type": "keyword" },
      "document_id": { "type": "keyword" },
      "tenant_id": { "type": "keyword" },
      "acl_ids": { "type": "keyword" },
      "title": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "body_chunk": { "type": "text" },
      "source_uri": { "type": "keyword" },
      "indexed_at": { "type": "date" }
    }
  }
}
JSON

curl -sS "$base_url/_cat/indices?v"