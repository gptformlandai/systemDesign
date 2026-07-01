#!/usr/bin/env bash
set -euo pipefail

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "docker command not found" >&2
    exit 1
  fi
}

section() {
  printf '\n## %s\n' "$1"
}

require_docker

section "Docker Version"
docker version || true

section "Docker Info Summary"
docker info --format 'Server={{.ServerVersion}} Driver={{.Driver}} CgroupDriver={{.CgroupDriver}} CgroupVersion={{.CgroupVersion}}' || true

section "Contexts"
docker context ls || true

section "Disk Usage"
docker system df || true

section "Running Containers"
docker ps || true

section "Recent Containers"
docker ps -a --last 10 || true

section "Images"
docker images || true

section "Networks"
docker network ls || true

section "Volumes"
docker volume ls || true