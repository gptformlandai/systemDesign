#!/bin/bash
# 01-redis-info-snapshot.sh
# Capture a full INFO snapshot to a timestamped file.
# Usage: ./01-redis-info-snapshot.sh [host] [port]

set -euo pipefail

HOST="${1:-127.0.0.1}"
PORT="${2:-6379}"
TIMESTAMP=$(/bin/date +%Y%m%d-%H%M%S)
OUTFILE="/tmp/redis-info-${HOST}-${PORT}-${TIMESTAMP}.txt"

echo "Capturing Redis INFO snapshot: ${HOST}:${PORT}"
echo "Output: ${OUTFILE}"

redis-cli -h "${HOST}" -p "${PORT}" INFO all > "${OUTFILE}"

echo ""
echo "=== Key Metrics ==="
/usr/bin/grep -E "^(used_memory:|used_memory_human:|maxmemory:|maxmemory_human:|maxmemory_policy:|mem_fragmentation_ratio:|evicted_keys:|connected_clients:|blocked_clients:|instantaneous_ops_per_sec:|keyspace_hits:|keyspace_misses:|role:|master_repl_offset:|slave_repl_offset:)" "${OUTFILE}"

echo ""
echo "=== Keyspace ==="
/usr/bin/grep -E "^db" "${OUTFILE}" || echo "(no keyspace data)"

echo ""
echo "Full snapshot saved to: ${OUTFILE}"
