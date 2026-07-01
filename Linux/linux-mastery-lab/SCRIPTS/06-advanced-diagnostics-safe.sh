#!/usr/bin/env bash
set -euo pipefail

service=${1:-ssh}
pid=${2:-}

print_title() {
  printf '\n## %s\n' "$1"
}

print_title "Tool Availability"
for tool in strace lsof tcpdump perf sysctl coredumpctl; do
  if command -v "$tool" >/dev/null 2>&1; then
    printf '%-12s %s\n' "$tool" "available"
  else
    printf '%-12s %s\n' "$tool" "missing"
  fi
done

print_title "Shell Limits"
ulimit -a 2>/dev/null || true

print_title "Selected Kernel Tunables"
for key in fs.file-max net.core.somaxconn vm.swappiness; do
  sysctl "$key" 2>/dev/null || true
done

print_title "Service Limits: $service"
systemctl show "$service" --property=LimitNOFILE,LimitNPROC,TasksMax 2>/dev/null || true

if [[ -n "$pid" ]]; then
  print_title "PID Open File Count: $pid"
  ls "/proc/$pid/fd" 2>/dev/null | wc -l || true

  print_title "PID Status: $pid"
  sed -n '1,80p' "/proc/$pid/status" 2>/dev/null || true
fi

cat <<'TEXT'

## Safe Next Steps
- Use strace only against a scoped PID or short command reproduction.
- Use tcpdump with host/port/interface filters and packet count limits.
- Use perf sampling for short windows when CPU cause is unclear.
- Treat traces, packet captures, and core dumps as sensitive production data.
TEXT