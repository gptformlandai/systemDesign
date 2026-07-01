#!/usr/bin/env bash
set -euo pipefail

operation_file="${1:-GraphQL/graphql-mastery-lab/EXAMPLES/simple-store/operations.graphql}"

if [[ ! -f "$operation_file" ]]; then
  echo "operation file not found: $operation_file" >&2
  exit 2
fi

section() {
  printf '\n## %s\n' "$1"
}

section "Operation File"
printf '%s\n' "$operation_file"

section "Named Operations"
/usr/bin/grep -n -E '^(query|mutation|subscription) [A-Za-z0-9_]+' "$operation_file" || true

section "Fragments"
/usr/bin/grep -n -E '^fragment [A-Za-z0-9_]+' "$operation_file" || true

section "Variables"
/usr/bin/grep -n -E '\$[A-Za-z0-9_]+' "$operation_file" || true