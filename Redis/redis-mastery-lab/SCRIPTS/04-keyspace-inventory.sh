#!/bin/bash
# 04-keyspace-inventory.sh
# Report key count, TTL distribution, and type breakdown per database.
# Usage: ./04-keyspace-inventory.sh [host] [port]

set -euo pipefail

HOST="${1:-127.0.0.1}"
PORT="${2:-6379}"

echo "Keyspace Inventory: ${HOST}:${PORT}"
echo "Time: $(/bin/date)"
echo ""

echo "=== INFO keyspace ==="
redis-cli -h "${HOST}" -p "${PORT}" INFO keyspace

echo ""
echo "=== Key Type Sample (SCAN first 500 keys in db0) ==="

CURSOR=0
declare -A TYPE_COUNT
SAMPLE_TOTAL=0
MAX_SAMPLE=500

while true; do
  RESULT=$(redis-cli -h "${HOST}" -p "${PORT}" SCAN "${CURSOR}" COUNT 100)
  CURSOR=$(echo "${RESULT}" | /usr/bin/awk 'NR==1{print $1}')
  KEYS=$(echo "${RESULT}" | /usr/bin/awk 'NR>1{print $0}')

  while IFS= read -r KEY; do
    [ -z "${KEY}" ] && continue
    [ "${SAMPLE_TOTAL}" -ge "${MAX_SAMPLE}" ] && break 2
    TYPE=$(redis-cli -h "${HOST}" -p "${PORT}" TYPE "${KEY}" 2>/dev/null || echo "unknown")
    TYPE_COUNT["${TYPE}"]=$((${TYPE_COUNT["${TYPE}"]:-0} + 1))
    SAMPLE_TOTAL=$((SAMPLE_TOTAL + 1))
  done <<< "${KEYS}"

  [ "${CURSOR}" = "0" ] && break
done

echo "Keys sampled: ${SAMPLE_TOTAL}"
echo ""
echo "Type breakdown:"
for type in "${!TYPE_COUNT[@]}"; do
  echo "  ${type}: ${TYPE_COUNT[$type]}"
done

echo ""
echo "=== Keys Without TTL (SCAN sample, first 100) ==="
CURSOR=0
NO_TTL=0
CHECKED=0
while true; do
  RESULT=$(redis-cli -h "${HOST}" -p "${PORT}" SCAN "${CURSOR}" COUNT 100)
  CURSOR=$(echo "${RESULT}" | /usr/bin/awk 'NR==1{print $1}')
  KEYS=$(echo "${RESULT}" | /usr/bin/awk 'NR>1{print $0}')

  while IFS= read -r KEY; do
    [ -z "${KEY}" ] && continue
    [ "${CHECKED}" -ge 100 ] && break 2
    TTL_VAL=$(redis-cli -h "${HOST}" -p "${PORT}" TTL "${KEY}" 2>/dev/null || echo "-2")
    [ "${TTL_VAL}" = "-1" ] && NO_TTL=$((NO_TTL + 1))
    CHECKED=$((CHECKED + 1))
  done <<< "${KEYS}"

  [ "${CURSOR}" = "0" ] && break
done

echo "Keys without TTL (in ${CHECKED} sample): ${NO_TTL}"
