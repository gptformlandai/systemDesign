#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: bash SCRIPTS/run-cypher.sh SCRIPTS/<file>.cypher" >&2
  exit 1
fi

script_path="$1"

if [[ ! -f "$script_path" ]]; then
  echo "Cypher script not found: $script_path" >&2
  exit 1
fi

docker compose exec -T neo4j cypher-shell -a bolt://localhost:7687 --format plain < "$script_path"