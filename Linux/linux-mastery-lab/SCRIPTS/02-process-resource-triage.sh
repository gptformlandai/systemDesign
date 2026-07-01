#!/usr/bin/env bash
set -euo pipefail

printf '## Load\n'
uptime 2>/dev/null || true

printf '\n## Top CPU Processes\n'
ps aux --sort=-%cpu 2>/dev/null | head -10 || true

printf '\n## Top Memory Processes\n'
ps aux --sort=-%mem 2>/dev/null | head -10 || true

printf '\n## Memory\n'
free -h 2>/dev/null || true

printf '\n## VM Stats\n'
vmstat 1 3 2>/dev/null || true

printf '\n## Kernel OOM Clues\n'
dmesg -T 2>/dev/null | grep -i -E 'oom|killed process' | tail -20 || true