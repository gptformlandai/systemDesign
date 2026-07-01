#!/usr/bin/env bash
set -euo pipefail

image="${1:-}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found" >&2
  exit 1
fi

if [[ -z "$image" ]]; then
  echo "Usage: $0 <image>" >&2
  exit 2
fi

section() {
  printf '\n## %s\n' "$1"
}

section "Image Identity"
docker image inspect "$image" --format 'Id={{.Id}} RepoDigests={{json .RepoDigests}} Size={{.Size}} User={{.Config.User}}' || true

section "Entrypoint And Command"
docker image inspect "$image" --format 'Entrypoint={{json .Config.Entrypoint}} Cmd={{json .Config.Cmd}} WorkingDir={{.Config.WorkingDir}}' || true

section "Environment Keys"
docker image inspect "$image" --format '{{range .Config.Env}}{{println .}}{{end}}' || true

section "Layer History"
docker history --no-trunc "$image" || true

section "Optional Docker Scout Scan"
if docker scout version >/dev/null 2>&1; then
  docker scout cves "$image" || true
else
  echo "docker scout not available. Use your approved scanner if required."
fi