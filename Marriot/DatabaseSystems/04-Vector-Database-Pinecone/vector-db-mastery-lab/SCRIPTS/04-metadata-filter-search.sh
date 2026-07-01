#!/usr/bin/env bash
set -euo pipefail

base_url="${QDRANT_URL:-http://localhost:6333}"
collection="${VECTOR_COLLECTION:-vector_lab}"

curl -fsS -X POST "$base_url/collections/$collection/points/search" \
  -H 'Content-Type: application/json' \
  -d '{
    "vector": [0.77, 0.12, 0.22, 0.11],
    "limit": 5,
    "with_payload": true,
    "filter": {
      "must": [
        {"key": "tenant_id", "match": {"value": "t1"}},
        {"key": "acl_group", "match": {"value": "commerce"}}
      ]
    }
  }'