#!/usr/bin/env bash
set -euo pipefail

container_name="${CASSANDRA_CONTAINER:-cassandra-mastery-lab}"

if [[ $# -eq 0 ]]; then
  docker exec -it "$container_name" cqlsh
  exit 0
fi

script_path="$1"

if [[ ! -f "$script_path" ]]; then
  echo "CQL script not found: $script_path" >&2
  exit 1
fi

docker exec -i "$container_name" cqlsh < "$script_path"