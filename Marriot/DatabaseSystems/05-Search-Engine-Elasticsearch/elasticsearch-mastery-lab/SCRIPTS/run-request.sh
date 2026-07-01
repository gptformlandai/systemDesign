#!/usr/bin/env bash
set -euo pipefail

base_url="${ELASTICSEARCH_URL:-http://localhost:9200}"

if [[ $# -eq 0 ]]; then
  if command -v jq >/dev/null 2>&1; then
    curl -sS "$base_url" | jq .
  else
    curl -sS "$base_url"
    echo
  fi
  exit 0
fi

script_path="$1"

if [[ ! -f "$script_path" ]]; then
  echo "Request script not found: $script_path" >&2
  exit 1
fi

sed "s#{{ELASTICSEARCH_URL}}#$base_url#g" "$script_path" | bash