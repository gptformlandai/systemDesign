#!/usr/bin/env bash
set -euo pipefail

base_url="${QDRANT_URL:-http://localhost:6333}"

for _ in $(seq 1 60); do
  if curl -fsS "$base_url/readyz" >/dev/null 2>&1; then
    echo "Qdrant is ready at $base_url"
    exit 0
  fi
  sleep 1
done

echo "Qdrant did not become ready at $base_url" >&2
exit 1