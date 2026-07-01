#!/usr/bin/env bash
set -euo pipefail

network="${1:-}"
container="${2:-}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

section() {
  printf '\n## %s\n' "$1"
}

section "Networks"
docker network ls || true

if [[ -n "$network" ]]; then
  section "Network Inspect: $network"
  docker network inspect "$network" || true
else
  section "Network Inspect"
  echo "Pass a network name to inspect it: $0 <network> [container]"
fi

if [[ -n "$container" ]]; then
  section "Container Ports: $container"
  docker port "$container" || true

  section "Container Network Settings: $container"
  docker inspect "$container" --format '{{json .NetworkSettings.Networks}}' || true

  section "Container Listening Sockets: $container"
  docker exec "$container" sh -c 'command -v ss >/dev/null 2>&1 && ss -ltnp || netstat -ltnp 2>/dev/null || true' || true
fi