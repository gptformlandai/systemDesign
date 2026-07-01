# Runbook: Load Balancing Rollout Incident

## Symptoms

- uneven backend distribution
- `UNAVAILABLE` during deploy
- one zone or pod receives most traffic
- streams break during termination

## Checks

1. Client LB policy.
2. Resolver output.
3. EndpointSlice membership.
4. Readiness probe timing.
5. Backend request distribution.
6. Envoy/mesh cluster health.
7. Connection draining settings.
8. Pod termination timeline.

## Mitigations

- fix readiness
- drain endpoints gracefully
- use appropriate LB policy
- refresh stale clients/proxies
- pause rollout

## Prevention

Canary deploys, traffic distribution dashboards, graceful shutdown tests, and resolver/LB config review.