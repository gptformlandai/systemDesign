#!/usr/bin/env bash
set -euo pipefail

compose_file="${1:-compose.yaml}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

if [[ ! -f "$compose_file" ]]; then
  echo "compose file not found: $compose_file" >&2
  exit 2
fi

section() {
  printf '\n## %s\n' "$1"
}

section "Compose Config"
docker compose -f "$compose_file" config || true

section "Compose Services"
docker compose -f "$compose_file" ps || true

section "Recent Logs"
docker compose -f "$compose_file" logs --tail=120 || true