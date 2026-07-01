#!/bin/bash
# incident-evidence-template.sh
# Captures current Datadog state context for an active incident.
# Outputs a structured evidence template to fill during investigation.

set -euo pipefail

SERVICE="${1:-orders-service}"
ENV="${2:-production}"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
INCIDENT_ID="INC-$(date +%Y%m%d-%H%M)"

cat << EOF
=== INCIDENT EVIDENCE TEMPLATE ===
Generated: ${TIMESTAMP}
Incident ID: ${INCIDENT_ID}
Service: ${SERVICE}
Environment: ${ENV}

--- 1. ALERT DETAILS ---
Monitor name: [FILL IN]
Alert value: [FILL IN]
Alert threshold: [FILL IN]
Alert fired at: [FILL IN]
SLO impact: [ ] None  [ ] Budget consuming  [ ] SLO breached

--- 2. INITIAL ASSESSMENT (T+0 to T+5 min) ---
APM Service page metrics at time of alert:
  Request rate: [FILL IN] req/min (normal: [FILL IN])
  Error rate: [FILL IN]%  (normal: [FILL IN]%)
  P99 latency: [FILL IN]ms (normal: [FILL IN]ms)
  P50 latency: [FILL IN]ms

Is this a traffic issue OR error issue OR latency issue?
  [ ] Traffic drop  [ ] Error spike  [ ] Latency spike  [ ] Combined

--- 3. TRACE INVESTIGATION ---
Trace Explorer filter used: service:${SERVICE} env:${ENV} status:error
Top error message: [FILL IN]
Error count in last 15 min: [FILL IN]
Trace ID of representative error: [FILL IN]
Flame graph bottleneck span: [FILL IN]
Bottleneck span duration: [FILL IN]ms

--- 4. CHANGE CORRELATION ---
Last deployment:
  Version: [FILL IN]
  Deployed at: [FILL IN]
  By: [FILL IN]
  Correlated with incident start? [ ] Yes  [ ] No

--- 5. DOWNSTREAM DEPENDENCIES ---
Check APM Service Map for ${SERVICE}:
  Downstream service 1: [service name] - Status: [ ] OK  [ ] Degraded  [ ] Down
  Downstream service 2: [service name] - Status: [ ] OK  [ ] Degraded  [ ] Down
  Database: [db name] - CPU: [FILL IN]%  Connections: [FILL IN]/[FILL IN]

--- 6. IMMEDIATE ACTIONS TAKEN ---
Time    Action                          Result
[HH:MM] [action taken]                 [outcome]
[HH:MM] [action taken]                 [outcome]

--- 7. MITIGATION ---
Mitigation applied: [FILL IN]
Applied at: [FILL IN]
Error rate after mitigation: [FILL IN]%
Monitor auto-resolved at: [FILL IN]

--- 8. SLO IMPACT SUMMARY ---
Error budget consumed during incident: [FILL IN] minutes
SLO status post-incident:
  7-day: [FILL IN]% (target: [FILL IN]%)
  30-day: [FILL IN]% (target: [FILL IN]%)

--- 9. ROOT CAUSE (preliminary) ---
Root cause: [FILL IN]
Contributing factors: [FILL IN]

--- 10. NEXT STEPS ---
[ ] RCA document by: [date]
[ ] Fix in version: [FILL IN]
[ ] Monitor/SLO tuning needed: [ ] Yes  [ ] No
[ ] Postmortem required: [ ] Yes  [ ] No (if SLO breached: mandatory)

=== END OF EVIDENCE TEMPLATE ===
EOF
