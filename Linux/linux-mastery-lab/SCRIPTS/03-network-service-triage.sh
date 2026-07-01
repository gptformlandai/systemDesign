#!/usr/bin/env bash
set -euo pipefail

host=${1:-example.com}
url=${2:-https://example.com}

printf '## DNS: %s\n' "$host"
getent hosts "$host" 2>/dev/null || true

printf '\n## Routes\n'
ip route 2>/dev/null || true

printf '\n## Listening TCP Ports\n'
ss -ltnp 2>/dev/null || true

printf '\n## HTTP/TLS Probe: %s\n' "$url"
curl -I -L --max-time 10 "$url" 2>/dev/null || true