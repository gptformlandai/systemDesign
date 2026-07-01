#!/usr/bin/env bash
set -euo pipefail

script="${1:-GraphQL/graphql-mastery-lab/EXAMPLES/simple-store/resolver-simulation.js}"

if [[ ! -f "$script" ]]; then
  echo "simulation script not found: $script" >&2
  exit 2
fi

if ! command -v node >/dev/null 2>&1; then
  echo "node command not found; read $script and run it where Node.js is available" >&2
  exit 1
fi

node "$script"