#!/usr/bin/env bash
# sessionStart — injects lightweight project context at the start of an agent session.
# Input: JSON on stdin (session metadata). Output: optional JSON on stdout.

set -euo pipefail

cat <<'EOF'
{
  "additional_context": "Project: feed-system-simulation (Spring Boot + Kafka). See AGENTS.md and README.md for architecture. Prefer mvn and docker compose for local runs."
}
EOF

exit 0
