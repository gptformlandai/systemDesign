#!/usr/bin/env bash
set -euo pipefail

base_url="${QDRANT_URL:-http://localhost:6333}"
collection="${VECTOR_COLLECTION:-vector_lab}"

curl -fsS -X POST "$base_url/collections/$collection/points/search" \
  -H 'Content-Type: application/json' \
  -d '{
    "vector": [0.11, 0.88, 0.10, 0.19],
    "limit": 3,
    "with_payload": true
  }'