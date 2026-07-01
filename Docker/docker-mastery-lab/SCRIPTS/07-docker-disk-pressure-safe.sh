#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

section() {
  printf '\n## %s\n' "$1"
}

section "Docker Disk Usage"
docker system df || true

section "Verbose Docker Disk Usage"
docker system df -v || true

section "Build Cache Usage"
docker builder du || true

section "Stopped Containers"
docker ps -a --filter status=exited || true

section "Images"
docker images || true

section "Dangling Images"
docker images --filter dangling=true || true

section "Volumes"
docker volume ls || true

section "Safety Reminder"
cat <<'MSG'
This script is read-only.
Before cleanup, confirm rollback images, stopped-container ownership, and volume backup status.
Avoid docker system prune -a, docker volume prune, or docker compose down -v during incidents unless the owner and blast radius are clear.
MSG