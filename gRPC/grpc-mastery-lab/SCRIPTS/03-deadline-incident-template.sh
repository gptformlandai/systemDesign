#!/usr/bin/env bash
set -euo pipefail

method="${1:-unknown.Service/Method}"

cat <<TEMPLATE
# gRPC Deadline Incident Template

Method: ${method}
Symptom: DEADLINE_EXCEEDED spike
Impact:
Start time:
Client services affected:
Regions/zones affected:

## Evidence
- Client deadline:
- Client span latency:
- Server span latency:
- Did server handler run:
- Proxy route timeout:
- Retry attempts:
- Dependency latency:
- Payload size change:
- Recent deployment/config/proto change:

## Likely Cause

## Mitigation

## Prevention
- deadline budget review
- trace coverage
- method latency SLO
- retry policy review
TEMPLATE