#!/usr/bin/env bash
set -euo pipefail

run() {
  local title=$1
  shift
  printf '\n## %s\n' "$title"
  "$@" 2>/dev/null || printf 'unavailable or permission denied: %s\n' "$*"
}

run "Kernel" uname -a
run "Uptime" uptime
run "Current User" id
run "Disk Usage" df -h
run "Inode Usage" df -ih
run "Memory" free -h
run "Listening TCP Ports" ss -ltnp
run "Failed systemd Units" systemctl --failed --no-pager