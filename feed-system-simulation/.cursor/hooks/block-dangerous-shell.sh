#!/usr/bin/env bash
# beforeShellExecution — asks the user to confirm potentially destructive shell commands.
# Requires: jq (install with brew install jq / apt install jq)
# Input: JSON with .command field. Output: permission JSON.

set -euo pipefail

input=$(cat)

if ! command -v jq >/dev/null 2>&1; then
  echo '{ "permission": "allow" }'
  exit 0
fi

command=$(echo "$input" | jq -r '.command // empty')

if [[ "$command" =~ rm[[:space:]]+-rf|docker[[:space:]]+system[[:space:]]+prune|DROP[[:space:]]+TABLE ]]; then
  cat <<EOF
{
  "permission": "ask",
  "user_message": "This command may be destructive. Please review before continuing.",
  "agent_message": "A project hook flagged a potentially destructive shell command."
}
EOF
  exit 0
fi

echo '{ "permission": "allow" }'
exit 0
