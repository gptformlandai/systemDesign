# 20. Scenario: Auth, mTLS, And Certificate Failure

## Incident

After a certificate rotation, 20% of calls from `checkout-api` to `payment-api` fail. Clients see a mix of `UNAVAILABLE` and `UNAUTHENTICATED`.

---

## Interpret The Status

| Status | Meaning In This Scenario |
|---|---|
| `UNAVAILABLE` | connection/TLS handshake may fail before the request reaches application auth |
| `UNAUTHENTICATED` | request reached auth logic but identity/token/cert was rejected |
| `PERMISSION_DENIED` | identity is valid but policy denies method/resource |

---

## Evidence Path

1. Check client and server TLS handshake errors.
2. Compare certificate SAN/SPIFFE identity before and after rotation.
3. Verify trust bundle rollout in all zones/namespaces.
4. Check Envoy/mesh sidecar certificate state.
5. Confirm server auth interceptor sees expected identity.
6. Check policy engine logs for denied principals.
7. Identify whether failures correlate with a subset of pods or nodes.

---

## Common Causes

- partial trust-bundle rollout
- certificate SAN or SPIFFE ID changed unexpectedly
- old and new CA not both trusted during transition
- sidecar has stale secret
- server policy references old identity
- clock skew makes cert appear not yet valid or expired
- a subset of clients did not reload certs

---

## Mitigation

- restore previous trust bundle if safe
- add overlapping trust for old and new CA
- restart/reload affected sidecars or clients
- roll back identity change
- update auth policy after verifying intended identity
- drain bad pods while cert state refreshes

---

## Prevention

- staged certificate rotation
- expiry and rotation alerts
- trust-bundle canary
- auth policy tests using new identities
- runbook for mTLS failures
- dashboard by client, server, identity, zone, and status

---

## Interview Sound Bite

I separate transport failure from auth decision failure. `UNAVAILABLE` often points to connection or TLS before app logic, while `UNAUTHENTICATED` means identity validation failed. I check cert identity, trust bundles, sidecars, policy logs, rollout scope, and clock skew.