#!/usr/bin/env bash
set -euo pipefail

base_url="{{ELASTICSEARCH_URL}}"

echo "Authorized retrieval for user with acl_ids: eng, search"
curl -sS -X GET "$base_url/rag-chunks-v1/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "query": {
    "bool": {
      "must": [{ "match": { "body_chunk": "slow search runbook" } }],
      "filter": [
        { "term": { "tenant_id": "t1" } },
        { "terms": { "acl_ids": ["eng", "search"] } }
      ]
    }
  },
  "_source": ["chunk_id", "document_id", "title", "acl_ids", "source_uri"]
}
JSON

echo "Unauthorized retrieval for user with acl_ids: finance"
curl -sS -X GET "$base_url/rag-chunks-v1/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "query": {
    "bool": {
      "must": [{ "match": { "body_chunk": "slow search runbook" } }],
      "filter": [
        { "term": { "tenant_id": "t1" } },
        { "terms": { "acl_ids": ["finance"] } }
      ]
    }
  },
  "_source": ["chunk_id", "document_id", "title", "acl_ids", "source_uri"]
}
JSON