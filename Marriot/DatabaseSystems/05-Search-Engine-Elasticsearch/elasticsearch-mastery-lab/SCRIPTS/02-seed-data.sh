#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

curl -sS -X POST "$base_url/_bulk?refresh=true" \
  -H 'Content-Type: application/x-ndjson' \
  --data-binary @- <<'NDJSON'
{ "index": { "_index": "products-write", "_id": "p1" } }
{ "product_id": "p1", "tenant_id": "t1", "title": "Mechanical keyboard", "brand": "keylab", "category": "electronics", "description": "RGB mechanical keyboard with tactile switches", "price": 129.99, "in_stock": true, "rating": 4.7, "popularity": 950, "created_at": "2026-07-01T10:00:00Z", "updated_at": "2026-07-01T10:00:00Z" }
{ "index": { "_index": "products-write", "_id": "p2" } }
{ "product_id": "p2", "tenant_id": "t1", "title": "Wireless running headphones", "brand": "soundmax", "category": "electronics", "description": "Sweat-resistant Bluetooth headphones for running", "price": 79.99, "in_stock": true, "rating": 4.4, "popularity": 1200, "created_at": "2026-07-01T10:05:00Z", "updated_at": "2026-07-01T10:05:00Z" }
{ "index": { "_index": "products-write", "_id": "p3" } }
{ "product_id": "p3", "tenant_id": "t1", "title": "Trail running shoes", "brand": "northpeak", "category": "apparel", "description": "Water-resistant running shoes for trail workouts", "price": 139.99, "in_stock": false, "rating": 4.6, "popularity": 800, "created_at": "2026-07-01T10:10:00Z", "updated_at": "2026-07-01T10:10:00Z" }
{ "index": { "_index": "logs-app-000001", "_id": "l1" } }
{ "@timestamp": "2026-07-01T10:00:00Z", "service": "checkout", "level": "ERROR", "trace_id": "trace-1", "tenant_id": "t1", "message": "Payment authorization failed", "status_code": 502, "duration_ms": 842 }
{ "index": { "_index": "logs-app-000001", "_id": "l2" } }
{ "@timestamp": "2026-07-01T10:01:00Z", "service": "catalog", "level": "INFO", "trace_id": "trace-2", "tenant_id": "t1", "message": "Product search completed", "status_code": 200, "duration_ms": 118 }
{ "index": { "_index": "rag-chunks-v1", "_id": "c1" } }
{ "chunk_id": "c1", "document_id": "d1", "tenant_id": "t1", "acl_ids": ["eng", "search"], "title": "Elasticsearch runbook", "body_chunk": "Slow search debugging starts with query DSL, shard fan-out, slow logs, heap, and disk watermarks.", "source_uri": "runbooks/search", "indexed_at": "2026-07-01T10:00:00Z" }
{ "index": { "_index": "rag-chunks-v1", "_id": "c2" } }
{ "chunk_id": "c2", "document_id": "d2", "tenant_id": "t1", "acl_ids": ["eng"], "title": "Search relevance guide", "body_chunk": "Product search relevance should be evaluated with golden queries, synonyms, boosts, zero-result rate, and conversion metrics.", "source_uri": "guides/relevance", "indexed_at": "2026-07-01T10:05:00Z" }
NDJSON

curl -sS "$base_url/_cat/count/products-v1?v"
curl -sS "$base_url/_cat/count/logs-app-000001?v"
curl -sS "$base_url/_cat/count/rag-chunks-v1?v"