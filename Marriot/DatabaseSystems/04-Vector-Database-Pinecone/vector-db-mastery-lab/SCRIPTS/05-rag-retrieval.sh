#!/usr/bin/env bash
set -euo pipefail

base_url="${QDRANT_URL:-http://localhost:6333}"
collection="${VECTOR_COLLECTION:-vector_lab}"

curl -fsS -X POST "$base_url/collections/$collection/points/search" \
  -H 'Content-Type: application/json' \
  -d '{
    "vector": [0.11, 0.88, 0.10, 0.19],
    "limit": 2,
    "with_payload": true,
    "filter": {
      "must": [
        {"key": "tenant_id", "match": {"value": "t1"}},
        {"key": "acl_group", "match": {"value": "support"}},
        {"key": "doc_type", "match": {"value": "policy"}}
      ]
    }
  }'