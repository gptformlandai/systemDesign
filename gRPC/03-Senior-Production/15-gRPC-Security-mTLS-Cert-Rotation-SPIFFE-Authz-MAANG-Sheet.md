# 15. Security: mTLS, Cert Rotation, SPIFFE, Authz

## Goal

Build a production security model for service-to-service gRPC.

```text
workload identity -> encrypted transport -> credential validation -> method/resource authorization -> audit evidence
```

---

## Security Layers

| Layer | Control |
|---|---|
| transport | TLS or mTLS |
| workload identity | certificates, SPIFFE IDs, service accounts |
| user/application identity | JWT/OAuth metadata |
| authorization | per-method and resource policy |
| network policy | restrict who can connect |
| observability | audit logs and auth metrics |

---

## SPIFFE/SPIRE Mental Model

SPIFFE gives workloads stable identities such as:

```text
spiffe://company.internal/ns/payments/sa/payment-api
```

SPIRE can issue and rotate workload certificates. A mesh or application runtime can use those identities for mTLS and authorization policy.

---

## Cert Rotation

Production cert rotation needs:

- automated issuance
- short-lived certs where practical
- overlap between old and new trust bundles
- hot reload or fast restart strategy
- alerts before expiry
- rollback plan for bad CA/config rollout

---

## Authz Pattern

```text
transport identity: payment-api service
request identity: user or batch job claims
method: payments.v1.PaymentService/CapturePayment
resource: merchant/account/payment id
policy: service can call method, user/job can access resource
```

Strong systems check both caller identity and resource-level permissions.

---

## Failure Modes

| Symptom | Likely Cause |
|---|---|
| TLS handshake failure | expired cert, wrong SAN, bad trust bundle, protocol mismatch |
| `UNAUTHENTICATED` | missing/invalid identity or token |
| `PERMISSION_DENIED` | identity valid but lacks policy permission |
| intermittent failures | partial rollout of certs, sidecars, trust bundles, or DNS |
| audit gap | auth decision not logged or trace context missing |

---

## Interview Sound Bite

For gRPC security, I use TLS/mTLS for transport and workload identity, automate cert rotation, validate request credentials in interceptors, enforce per-method and resource authorization, and keep audit evidence without logging secrets or sensitive metadata.