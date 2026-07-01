#!/usr/bin/env bash
set -euo pipefail

service=${1:-ssh}

printf '## Service Status: %s\n' "$service"
systemctl status "$service" --no-pager 2>/dev/null || true

printf '\n## Unit File: %s\n' "$service"
systemctl cat "$service" 2>/dev/null || true

printf '\n## Recent Logs: %s\n' "$service"
journalctl -u "$service" -n 100 --no-pager 2>/dev/null || true

printf '\n## Failed Units\n'
systemctl --failed --no-pager 2>/dev/null || true