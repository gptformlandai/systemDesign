#!/usr/bin/env bash
set -euo pipefail

base_url="${QDRANT_URL:-http://localhost:6333}"
collection="${VECTOR_COLLECTION:-vector_lab}"

curl -fsS -X PUT "$base_url/collections/$collection" \
  -H 'Content-Type: application/json' \
  -d '{
    "vectors": {
      "size": 4,
      "distance": "Cosine"
    }
  }'

curl -fsS -X PUT "$base_url/collections/$collection/index" \
  -H 'Content-Type: application/json' \
  -d '{"field_name":"tenant_id","field_schema":"keyword"}' >/dev/null

curl -fsS -X PUT "$base_url/collections/$collection/index" \
  -H 'Content-Type: application/json' \
  -d '{"field_name":"acl_group","field_schema":"keyword"}' >/dev/null

curl -fsS -X PUT "$base_url/collections/$collection/index" \
  -H 'Content-Type: application/json' \
  -d '{"field_name":"doc_type","field_schema":"keyword"}' >/dev/null

echo "Created collection $collection"