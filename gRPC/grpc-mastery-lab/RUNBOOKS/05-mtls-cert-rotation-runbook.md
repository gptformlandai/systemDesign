# Runbook: mTLS Certificate Rotation Failure

## Symptoms

- TLS handshake errors
- `UNAVAILABLE` during connection
- `UNAUTHENTICATED` after identity validation
- failures isolated to zone, namespace, or client version

## Checks

1. Certificate expiry and validity window.
2. SAN or SPIFFE identity.
3. Trust bundle rollout state.
4. Sidecar secret state.
5. Server auth policy.
6. Client cert reload behavior.
7. Clock skew.
8. Failure correlation by pod/zone.

## Mitigations

- restore previous trust bundle
- add overlapping trust
- restart or reload affected clients/sidecars
- roll back identity change
- drain affected pods

## Prevention

Rotation canaries, expiry alerts, overlap windows, and policy tests.