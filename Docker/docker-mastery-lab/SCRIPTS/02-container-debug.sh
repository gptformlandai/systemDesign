#!/usr/bin/env bash
set -euo pipefail

container="${1:-}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

if [[ -z "$container" ]]; then
  echo "Usage: $0 <container-name-or-id>" >&2
  echo "Tip: run 'docker ps -a' first." >&2
  exit 2
fi

section() {
  printf '\n## %s\n' "$1"
}

section "Container State"
docker inspect "$container" --format 'Name={{.Name}} Status={{.State.Status}} ExitCode={{.State.ExitCode}} OOMKilled={{.State.OOMKilled}} Restarting={{.State.Restarting}}' || true

section "Image And Command"
docker inspect "$container" --format 'Image={{.Config.Image}} Entrypoint={{json .Config.Entrypoint}} Cmd={{json .Config.Cmd}} User={{.Config.User}}' || true

section "Ports"
docker port "$container" || true

section "Mounts"
docker inspect "$container" --format '{{json .Mounts}}' || true

section "Networks"
docker inspect "$container" --format '{{json .NetworkSettings.Networks}}' || true

section "Resource Limits"
docker inspect "$container" --format 'Memory={{.HostConfig.Memory}} NanoCPUs={{.HostConfig.NanoCpus}} PidsLimit={{.HostConfig.PidsLimit}} Privileged={{.HostConfig.Privileged}}' || true

section "Recent Logs"
docker logs --tail 120 "$container" || true

section "Top Processes"
docker top "$container" || true