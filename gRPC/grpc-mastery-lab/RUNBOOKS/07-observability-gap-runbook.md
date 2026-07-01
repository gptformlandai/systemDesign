# Runbook: Observability Gap

## Symptom

An incident is happening, but the team cannot prove method, caller, status, or latency source quickly.

## Checks

1. Are metrics tagged by service, method, status, client, and version?
2. Are traces propagated through metadata?
3. Are client and server spans both present?
4. Do logs include request id and final status?
5. Are proxy access logs available?
6. Are high-cardinality labels avoided?
7. Are SLOs defined for critical methods?

## Mitigation

During the incident, add temporary targeted logs if safe. Afterward, fix instrumentation as a product requirement, not a cleanup task.

## Prevention

Observability acceptance criteria for every new gRPC method.