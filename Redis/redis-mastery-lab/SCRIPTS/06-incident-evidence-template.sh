#!/bin/bash
# 06-incident-evidence-template.sh
# Capture a comprehensive Redis evidence bundle for incident postmortems.
# Usage: ./06-incident-evidence-template.sh [host] [port] [incident_id]

set -euo pipefail

HOST="${1:-127.0.0.1}"
PORT="${2:-6379}"
INCIDENT="${3:-INC-$(date +%Y%m%d-%H%M%S)}"
OUTDIR="/tmp/redis-incident-${INCIDENT}"

/bin/mkdir -p "${OUTDIR}"
echo "Capturing Redis incident evidence for ${INCIDENT}"
echo "Host: ${HOST}:${PORT}"
echo "Output: ${OUTDIR}/"
echo ""

echo "=== 1. INFO ALL ===" | /usr/bin/tee "${OUTDIR}/01-info-all.txt"
redis-cli -h "${HOST}" -p "${PORT}" INFO all >> "${OUTDIR}/01-info-all.txt"

echo "=== 2. SLOWLOG ===" | /usr/bin/tee "${OUTDIR}/02-slowlog.txt"
redis-cli -h "${HOST}" -p "${PORT}" SLOWLOG GET 100 >> "${OUTDIR}/02-slowlog.txt"

echo "=== 3. CLIENT LIST ===" | /usr/bin/tee "${OUTDIR}/03-client-list.txt"
redis-cli -h "${HOST}" -p "${PORT}" CLIENT LIST >> "${OUTDIR}/03-client-list.txt"

echo "=== 4. LATENCY LATEST ===" | /usr/bin/tee "${OUTDIR}/04-latency.txt"
redis-cli -h "${HOST}" -p "${PORT}" LATENCY LATEST >> "${OUTDIR}/04-latency.txt"

echo "=== 5. MEMORY DOCTOR ===" | /usr/bin/tee "${OUTDIR}/05-memory-doctor.txt"
redis-cli -h "${HOST}" -p "${PORT}" MEMORY DOCTOR >> "${OUTDIR}/05-memory-doctor.txt"

echo "=== 6. CONFIG snapshot ===" | /usr/bin/tee "${OUTDIR}/06-config.txt"
redis-cli -h "${HOST}" -p "${PORT}" CONFIG GET maxmemory >> "${OUTDIR}/06-config.txt"
redis-cli -h "${HOST}" -p "${PORT}" CONFIG GET maxmemory-policy >> "${OUTDIR}/06-config.txt"
redis-cli -h "${HOST}" -p "${PORT}" CONFIG GET maxclients >> "${OUTDIR}/06-config.txt"
redis-cli -h "${HOST}" -p "${PORT}" CONFIG GET appendonly >> "${OUTDIR}/06-config.txt"
redis-cli -h "${HOST}" -p "${PORT}" CONFIG GET save >> "${OUTDIR}/06-config.txt"

echo "=== 7. BIGKEYS sample ===" | /usr/bin/tee "${OUTDIR}/07-bigkeys.txt"
redis-cli -h "${HOST}" -p "${PORT}" --bigkeys >> "${OUTDIR}/07-bigkeys.txt" 2>&1 || true

echo ""
echo "Evidence bundle complete: ${OUTDIR}/"
echo "Files:"
/bin/ls -lh "${OUTDIR}/"
