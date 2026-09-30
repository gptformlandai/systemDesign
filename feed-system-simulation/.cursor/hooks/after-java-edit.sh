#!/usr/bin/env bash
# afterFileEdit — reminds the agent to run tests after Java changes.
# Input: JSON with file_path (and related fields). Output: optional additional_context.

set -euo pipefail

input=$(cat)

if command -v jq >/dev/null 2>&1; then
  file_path=$(echo "$input" | jq -r '.file_path // .path // empty')
else
  file_path=""
fi

if [[ "$file_path" == *.java ]]; then
  cat <<EOF
{
  "additional_context": "A Java file was edited ($file_path). Consider running 'mvn test' if the change is non-trivial."
}
EOF
else
  echo '{}'
fi

exit 0
