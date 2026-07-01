#!/usr/bin/env bash
set -euo pipefail

attempts="${NEO4J_WAIT_ATTEMPTS:-40}"

for attempt in $(seq 1 "$attempts"); do
  if docker compose exec -T neo4j cypher-shell -a bolt://localhost:7687 "RETURN 1" >/dev/null 2>&1; then
    echo "Neo4j is ready."
    exit 0
  fi

  echo "Waiting for Neo4j... attempt $attempt/$attempts"
  sleep 5
done

echo "Neo4j did not become ready in time." >&2
exit 1