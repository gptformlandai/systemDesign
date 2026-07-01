#!/bin/bash
# 03-slowlog-report.sh
# Fetch and display the Redis SLOWLOG with human-readable output.
# Usage: ./03-slowlog-report.sh [count] [host] [port]

set -euo pipefail

COUNT="${1:-50}"
HOST="${2:-127.0.0.1}"
PORT="${3:-6379}"
OUTFILE="/tmp/redis-slowlog-$(/bin/date +%Y%m%d-%H%M%S).txt"

echo "Fetching last ${COUNT} SLOWLOG entries from ${HOST}:${PORT}"
echo "Output: ${OUTFILE}"

{
  echo "Redis SLOWLOG Report"
  echo "Host: ${HOST}:${PORT}"
  echo "Time: $(/bin/date)"
  echo "Count: ${COUNT}"
  echo "==="
  redis-cli -h "${HOST}" -p "${PORT}" SLOWLOG GET "${COUNT}"
  echo ""
  echo "SLOWLOG LEN: $(redis-cli -h "${HOST}" -p "${PORT}" SLOWLOG LEN)"
  echo "Config slowlog-log-slower-than: $(redis-cli -h "${HOST}" -p "${PORT}" CONFIG GET slowlog-log-slower-than | /usr/bin/awk 'NR==2')"
} > "${OUTFILE}"

echo ""
echo "Slowlog threshold (microseconds): $(redis-cli -h "${HOST}" -p "${PORT}" CONFIG GET slowlog-log-slower-than | /usr/bin/awk 'NR==2')"
echo "Total slowlog entries: $(redis-cli -h "${HOST}" -p "${PORT}" SLOWLOG LEN)"
echo "Report saved to: ${OUTFILE}"
