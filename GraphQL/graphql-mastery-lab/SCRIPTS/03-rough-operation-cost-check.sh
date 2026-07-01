#!/usr/bin/env bash
set -euo pipefail

operation_file="${1:-GraphQL/graphql-mastery-lab/EXAMPLES/simple-store/operations.graphql}"

if [[ ! -f "$operation_file" ]]; then
  echo "operation file not found: $operation_file" >&2
  exit 2
fi

fields=$(/usr/bin/grep -E '^[[:space:]]+[A-Za-z_][A-Za-z0-9_]*(\(|$|[[:space:]]|\{)' "$operation_file" | /usr/bin/wc -l | /usr/bin/tr -d ' ')
braces=$(/usr/bin/grep -o '[{}]' "$operation_file" | /usr/bin/wc -l | /usr/bin/tr -d ' ')
fragments=$(/usr/bin/grep -E '^fragment ' "$operation_file" | /usr/bin/wc -l | /usr/bin/tr -d ' ')

cat <<MSG
Rough operation cost signals for: $operation_file
- selected field-like lines: $fields
- braces: $braces
- fragments: $fragments

This is not a real GraphQL complexity analyzer.
Use it as a lab prompt: identify nested lists, high-cardinality fields, and fields that call remote services.
MSG