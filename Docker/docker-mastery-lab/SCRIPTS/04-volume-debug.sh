#!/usr/bin/env bash
set -euo pipefail

volume="${1:-}"
container="${2:-}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

section() {
  printf '\n## %s\n' "$1"
}

section "Volumes"
docker volume ls || true

if [[ -n "$volume" ]]; then
  section "Volume Inspect: $volume"
  docker volume inspect "$volume" || true
else
  section "Volume Inspect"
  echo "Pass a volume name to inspect it: $0 <volume> [container]"
fi

if [[ -n "$container" ]]; then
  section "Container Mounts: $container"
  docker inspect "$container" --format '{{json .Mounts}}' || true

  section "Container User: $container"
  docker exec "$container" id || true
fi