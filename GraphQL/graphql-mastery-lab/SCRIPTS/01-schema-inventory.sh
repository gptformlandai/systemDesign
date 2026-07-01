#!/usr/bin/env bash
set -euo pipefail

schema_file="${1:-GraphQL/graphql-mastery-lab/EXAMPLES/simple-store/schema.graphql}"

if [[ ! -f "$schema_file" ]]; then
  echo "schema file not found: $schema_file" >&2
  exit 2
fi

section() {
  printf '\n## %s\n' "$1"
}

section "Schema File"
printf '%s\n' "$schema_file"

section "Object Types"
/usr/bin/grep -n '^type ' "$schema_file" || true

section "Input Types"
/usr/bin/grep -n '^input ' "$schema_file" || true

section "Enums And Interfaces"
/usr/bin/grep -n -E '^(enum|interface|union) ' "$schema_file" || true

section "Deprecated Fields"
/usr/bin/grep -n '@deprecated' "$schema_file" || true

section "Unbounded List Smell"
/usr/bin/grep -n -E ': \[[A-Za-z0-9_!]+\][!]?$' "$schema_file" || true