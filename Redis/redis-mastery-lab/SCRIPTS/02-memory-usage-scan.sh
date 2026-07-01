#!/bin/bash
# 02-memory-usage-scan.sh
# Scan keys matching a pattern and report memory usage per key.
# Usage: ./02-memory-usage-scan.sh [pattern] [host] [port]
# Example: ./02-memory-usage-scan.sh "cache:product:*"

set -euo pipefail

PATTERN="${1:-*}"
HOST="${2:-127.0.0.1}"
PORT="${3:-6379}"
CURSOR=0
COUNT=0
TOTAL_BYTES=0
OUTFILE="/tmp/redis-memory-scan-$(/bin/date +%Y%m%d-%H%M%S).tsv"

echo "Scanning pattern: ${PATTERN} on ${HOST}:${PORT}"
echo "Output: ${OUTFILE}"
echo -e "key\tbytes\ttype" > "${OUTFILE}"

while true; do
  RESULT=$(redis-cli -h "${HOST}" -p "${PORT}" SCAN "${CURSOR}" MATCH "${PATTERN}" COUNT 100)
  CURSOR=$(echo "${RESULT}" | /usr/bin/awk 'NR==1{print $1}')
  KEYS=$(echo "${RESULT}" | /usr/bin/awk 'NR>1{print $0}')

  while IFS= read -r KEY; do
    [ -z "${KEY}" ] && continue
    BYTES=$(redis-cli -h "${HOST}" -p "${PORT}" MEMORY USAGE "${KEY}" 2>/dev/null || echo 0)
    TYPE=$(redis-cli -h "${HOST}" -p "${PORT}" TYPE "${KEY}" 2>/dev/null || echo "unknown")
    echo -e "${KEY}\t${BYTES}\t${TYPE}" >> "${OUTFILE}"
    TOTAL_BYTES=$((TOTAL_BYTES + BYTES))
    COUNT=$((COUNT + 1))
  done <<< "${KEYS}"

  [ "${CURSOR}" = "0" ] && break
done

echo ""
echo "Keys scanned: ${COUNT}"
echo "Total bytes:  ${TOTAL_BYTES}"
echo "Results in:   ${OUTFILE}"
