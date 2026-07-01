# Runbook: `UNAVAILABLE`

## Meaning

The service or path is temporarily unavailable. This can happen before the handler runs.

## Checks

1. Resolve service name from client environment.
2. Check server process and listening port.
3. Check Kubernetes Service and EndpointSlice.
4. Check readiness and health status.
5. Verify TLS/plaintext settings.
6. Inspect proxy/mesh cluster health.
7. Check recent deployment or config changes.
8. Review client channel/subchannel state.

## Mitigations

- roll back bad deployment
- drain unhealthy endpoints
- restore cert/trust config
- fix service discovery or route config
- pause rollout

## Prevention

Health checks, connection draining, canaries, service discovery tests, and method-level alerts.