#!/usr/bin/env bash
set -euo pipefail

base_url="${QDRANT_URL:-http://localhost:6333}"
collection="${VECTOR_COLLECTION:-vector_lab}"

curl -fsS -X DELETE "$base_url/collections/$collection" >/dev/null 2>&1 || true
bash SCRIPTS/01-create-collection.sh
bash SCRIPTS/02-upsert-documents.sh

echo "Reset $collection"