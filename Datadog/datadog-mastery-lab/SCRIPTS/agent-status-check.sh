#!/bin/bash
# agent-status-check.sh
# Verifies Datadog Agent health, APM, logs, and data flow.
# Usage: ./agent-status-check.sh [container-name]

set -euo pipefail

CONTAINER="${1:-datadog-agent}"

echo "=== Datadog Agent Status Check ==="
echo "Container: $CONTAINER"
echo ""

# Check if container is running.
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "ERROR: Container '$CONTAINER' is not running."
  echo "Start it with: docker run -d --name $CONTAINER gcr.io/datadoghq/agent:7 ..."
  exit 1
fi

echo "--- Agent Version ---"
docker exec "$CONTAINER" datadog-agent version 2>/dev/null || echo "Could not get version"

echo ""
echo "--- API Key Status ---"
docker exec "$CONTAINER" datadog-agent status 2>/dev/null \
  | grep -A2 "API Keys status" || echo "Could not check API key"

echo ""
echo "--- APM Agent Status ---"
docker exec "$CONTAINER" datadog-agent status 2>/dev/null \
  | grep -A5 "APM Agent" || echo "APM agent info not found"

echo ""
echo "--- Logs Agent Status ---"
docker exec "$CONTAINER" datadog-agent status 2>/dev/null \
  | grep -A5 "Logs Agent" || echo "Logs agent info not found"

echo ""
echo "--- DogStatsD Status ---"
docker exec "$CONTAINER" datadog-agent status 2>/dev/null \
  | grep -A3 "DogStatsD" || echo "DogStatsD info not found"

echo ""
echo "--- Forwarder Queue ---"
docker exec "$CONTAINER" datadog-agent status 2>/dev/null \
  | grep -A3 "Forwarder" || echo "Forwarder info not found"

echo ""
echo "--- Send Test Metric ---"
echo "datadog.agent.check.test:1|c|#env:dev,check:agent-status-check" \
  | nc -u -w1 localhost 8125 2>/dev/null \
  && echo "Test metric sent to DogStatsD port 8125" \
  || echo "WARNING: Could not send to DogStatsD port 8125 (check port mapping)"

echo ""
echo "=== Check Complete ==="
echo "If agent is healthy, metrics/traces should appear in Datadog within 15-30 seconds."
