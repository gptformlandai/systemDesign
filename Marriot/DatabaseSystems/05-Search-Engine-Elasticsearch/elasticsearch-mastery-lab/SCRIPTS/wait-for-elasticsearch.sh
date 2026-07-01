#!/usr/bin/env bash
set -euo pipefail

base_url="${ELASTICSEARCH_URL:-http://localhost:9200}"
attempts="${ELASTICSEARCH_WAIT_ATTEMPTS:-40}"

for attempt in $(seq 1 "$attempts"); do
  if curl -fsS "$base_url/_cluster/health" >/dev/null 2>&1; then
    echo "Elasticsearch is ready."
    exit 0
  fi

  echo "Waiting for Elasticsearch... attempt $attempt/$attempts"
  sleep 5
done

echo "Elasticsearch did not become ready in time." >&2
exit 1