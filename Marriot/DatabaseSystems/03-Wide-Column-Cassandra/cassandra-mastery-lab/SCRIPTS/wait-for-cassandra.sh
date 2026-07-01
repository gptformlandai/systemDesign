#!/usr/bin/env bash
set -euo pipefail

container_name="${CASSANDRA_CONTAINER:-cassandra-mastery-lab}"
attempts="${CASSANDRA_WAIT_ATTEMPTS:-40}"

for attempt in $(seq 1 "$attempts"); do
  if docker exec "$container_name" cqlsh -e "DESCRIBE KEYSPACES" >/dev/null 2>&1; then
    echo "Cassandra is ready."
    exit 0
  fi

  echo "Waiting for Cassandra... attempt $attempt/$attempts"
  sleep 5
done

echo "Cassandra did not become ready in time." >&2
exit 1