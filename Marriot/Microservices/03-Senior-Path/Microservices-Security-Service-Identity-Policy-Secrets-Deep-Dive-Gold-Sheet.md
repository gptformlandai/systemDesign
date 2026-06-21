# Microservices Security Service Identity Policy Secrets Deep Dive Gold Sheet

> Track: Microservices Interview Track - Group 3 Senior Path  
> Goal: deepen zero-trust security beyond gateway auth into service identity, authorization, secrets, audit, and incident response.

Read after the security/zero-trust master sheet.

---

## 1. Senior Security Mental Model

In microservices, the internal network is not automatically trusted.

Security layers:

```text
User identity -> edge authentication -> service authorization -> service identity
-> network policy -> secrets management -> data protection -> audit logging
```

Strong answer:

```text
The gateway is the first control, not the only control. Each service must validate identity,
authorize sensitive actions, protect secrets, and emit audit logs for important decisions.
```

---

## 2. Authentication vs Authorization

| Concept | Question |
|---|---|
| Authentication | Who are you? |
| Authorization | Are you allowed to do this? |
| Service identity | Which workload/service is calling? |
| Delegation | Is the service acting on behalf of a user? |
| Audit | Can we prove who did what and when? |

Interview trap:

```text
JWT valid does not mean the user is allowed to cancel this booking.
```

Better:

```text
Validate token authenticity, then check business authorization in the owning service.
```

---

## 3. Edge Authentication Flow

```text
User -> Identity Provider -> token -> API Gateway -> service
```

Gateway responsibilities:

- TLS termination or integration with edge TLS
- token validation
- coarse route protection
- rate limiting
- request metadata propagation
- rejecting obviously invalid requests

Service responsibilities:

- enforce business authorization
- validate important claims if needed
- avoid trusting spoofable headers
- protect sensitive operations
- emit audit events

Strong answer:

```text
The gateway reduces repeated auth work, but services still enforce authorization because
internal routes can be called by jobs, other services, or misconfigured paths.
```

---

## 4. JWT Validation Checklist

Validate:

- signature
- issuer
- audience
- expiration
- not-before if used
- algorithm allowlist
- key id resolution
- required claims
- tenant/account scope if multi-tenant

Avoid:

- accepting `alg=none`
- skipping audience validation
- trusting unverified decoded payload
- logging full tokens
- using long-lived access tokens without rotation strategy

Strong answer:

```text
JWT validation is not just decoding base64. The service or gateway must verify signature,
issuer, audience, expiration, and required claims against trusted keys.
```

---

## 5. JWKS And Key Rotation

JWKS exposes public keys for token verification.

Flow:

```text
1. Identity provider signs token with private key.
2. Token includes key id.
3. Gateway/service fetches matching public key from JWKS.
4. Token signature is verified.
```

Rotation safety:

- cache keys with reasonable TTL
- support old and new keys during transition
- alert on unknown key id spikes
- do not fetch JWKS per request
- handle identity-provider outage gracefully with cached keys

Strong answer:

```text
Key rotation requires overlap. New tokens may use a new key while old tokens are still valid,
so verifiers must support both until old tokens expire.
```

---

## 6. OAuth2 Scopes vs Roles vs Permissions

| Model | Example | Use |
|---|---|---|
| Scope | `booking:write` | coarse API permission |
| Role | `HOTEL_MANAGER` | business grouping |
| Permission | `cancel_booking:hotel_123` | fine-grained decision |
| Policy | ABAC/RBAC rule | complex authorization logic |

Senior answer:

```text
Scopes are useful for coarse API access, but domain services often need fine-grained checks
such as booking ownership, hotel ownership, tenant boundary, and booking state.
```

---

## 7. Service-To-Service Identity

Service identity answers:

```text
Which workload is calling me?
```

Options:

- mTLS certificates
- service mesh identity
- signed service tokens
- workload identity from cloud/platform
- SPIFFE/SPIRE identities

Example identity:

```text
spiffe://prod/hotel-platform/ns/payments/sa/payment-service
```

Strong answer:

```text
For service-to-service calls, I want workload identity, not shared static credentials. That
lets services authorize callers and limits blast radius if one workload is compromised.
```

---

## 8. mTLS

mTLS means both sides authenticate with certificates.

Protects:

- service identity
- traffic encryption
- impersonation risk
- internal network sniffing

Does not solve alone:

- user authorization
- business permissions
- data leakage in logs
- compromised service logic

Strong answer:

```text
mTLS proves service identity and encrypts traffic, but the receiving service still needs
authorization logic for the requested action.
```

---

## 9. Service Mesh Security

Service mesh can provide:

- mTLS between services
- traffic policy
- retries/timeouts
- authorization policy
- observability
- certificate rotation

Examples:

- Istio
- Linkerd
- Consul service mesh
- Envoy-based meshes

Use carefully:

```text
A mesh can standardize cross-cutting controls, but it adds operational complexity. Teams
must understand traffic policies, retries, timeouts, and certificate behavior.
```

---

## 10. Policy As Code

Policy engines such as OPA can externalize authorization decisions.

Example use cases:

- who can deploy to production
- which services may call payment APIs
- whether a user can access a tenant resource
- whether Kubernetes manifests meet security standards

Policy input example:

```json
{
  "subject": "booking-service",
  "action": "capturePayment",
  "resource": "payment-service",
  "environment": "prod"
}
```

Senior answer:

```text
Policy-as-code is useful when authorization rules need review, testing, and consistent
enforcement. I avoid hiding simple domain decisions far away from the owning service unless
central policy gives real value.
```

---

## 11. Secrets Management

Secrets include:

- database passwords
- API keys
- private keys
- OAuth client secrets
- payment provider credentials
- signing keys

Good practices:

- store secrets in secret manager/vault
- short-lived credentials where possible
- rotate regularly
- least privilege per service
- no secrets in git, images, logs, or tickets
- audit secret access
- alert on unusual access

Strong answer:

```text
Secrets should be centrally managed, rotated, least-privileged, and injected at runtime. A
service should not share broad credentials with other services.
```

---

## 12. Secret Rotation Failure Scenario

Scenario:

```text
Payment Service starts failing after secret rotation.
```

Debug path:

1. Check deploy/config/secret rotation timeline.
2. Compare old and new pod versions.
3. Check secret mount/env refresh behavior.
4. Verify provider accepted new credential.
5. Roll back or restore previous credential if allowed.
6. Add rotation test and runbook.

Prevention:

- dual credentials during rotation
- canary new secret
- automated validation
- clear rollback plan
- alert before expiration

Strong answer:

```text
Secret rotation should be tested like deployment. I prefer overlapping credentials and
canary validation so rotation does not become an outage.
```

---

## 13. Network Policy

Network policy restricts which workloads can talk.

Example intent:

```text
Only Booking Service and reconciliation jobs can call Payment Service.
Only Payment Service can access payment database.
```

Why:

```text
Even if a service is compromised, network policy reduces lateral movement.
```

Strong answer:

```text
Network policy is a blast-radius control. It should reflect real service dependencies, not
allow every pod to call every other pod.
```

---

## 14. Multi-Tenant Security

Tenant isolation checks:

- every request has tenant/account context
- database queries include tenant boundary
- cache keys include tenant id
- events include tenant id where needed
- logs avoid leaking tenant data
- authorization checks enforce tenant ownership
- rate limits and quotas can be tenant-specific

Trap:

```text
A valid token from tenant A must never read booking data from tenant B.
```

Strong answer:

```text
In multi-tenant systems, tenant isolation is a correctness and security invariant. I enforce
it in authorization, data access, cache keys, events, and audit logs.
```

---

## 15. Data Protection And PII

Classify data:

| Data | Treatment |
|---|---|
| public hotel info | normal controls |
| booking id | internal/customer data |
| email/phone | PII, minimize exposure |
| payment token | highly sensitive |
| full card data | avoid storing unless PCI scope requires controls |
| auth token | secret, never log |

Controls:

- encryption in transit
- encryption at rest
- field-level masking/tokenization for sensitive data
- retention policy
- access audit
- least privilege queries

---

## 16. Audit Logging

Audit sensitive actions:

- booking cancellation
- refund initiation
- payment capture/refund
- loyalty point adjustment
- role/permission change
- secret access
- admin user impersonation

Audit event fields:

- actor type and id
- action
- resource type and id
- tenant/account
- timestamp
- result
- reason/context
- source service
- correlation id

Strong answer:

```text
Audit logs are not normal debug logs. They are durable evidence for sensitive actions and
must be protected from tampering and accidental deletion.
```

---

## 17. Threat Modeling Checklist

For each service, ask:

1. What data does it protect?
2. Who can call it?
3. What happens if caller identity is spoofed?
4. What happens if token is expired or wrong audience?
5. What secrets does it use?
6. What is the blast radius if compromised?
7. Which actions need audit?
8. Which logs could leak sensitive data?
9. What rate limits prevent abuse?
10. What dependency failures create unsafe behavior?

Use STRIDE lightly:

| Threat | Example |
|---|---|
| Spoofing | fake service identity |
| Tampering | modified event payload |
| Repudiation | no audit for refund |
| Information disclosure | token in logs |
| Denial of service | unbounded expensive API calls |
| Elevation of privilege | user acts as admin |

---

## 18. Gateway Bypass Scenario

Scenario:

```text
An internal caller hits Booking Service directly and cancels another user's booking.
```

Bad design:

```text
Only gateway checked permissions.
Booking Service trusted internal traffic.
```

Good design:

- gateway validates edge token
- Booking Service checks booking ownership/admin permission
- service identity is verified
- audit log records cancellation
- network policy limits direct callers

Strong answer:

```text
Gateway bypass is exactly why services enforce authorization locally. Internal network does
not equal trusted user permission.
```

---

## 19. Security Incident Response

If token/secret compromise is suspected:

1. Contain: revoke/rotate credential, block suspicious traffic.
2. Scope: identify affected services, tenants, data, time window.
3. Preserve evidence: audit logs, access logs, deploy/config changes.
4. Recover: rotate keys/secrets, patch root cause, validate behavior.
5. Notify per policy/compliance.
6. Prevent recurrence: detection, tests, least privilege, runbook updates.

Strong answer:

```text
Security incidents need containment and evidence preservation. I rotate credentials, reduce
blast radius, identify affected data, and improve controls after root cause.
```

---

## 20. Common Interview Traps

| Trap | Better Answer |
|---|---|
| "Gateway auth is enough" | services enforce business authorization |
| "mTLS solves security" | it proves service identity, not user permission |
| "JWT is safe if decoded" | signature/issuer/audience/expiry must be verified |
| "Internal traffic is trusted" | zero trust validates internal callers too |
| "Put secrets in env and forget" | rotation, least privilege, audit, refresh matter |
| "Log everything for debugging" | protect tokens, secrets, PII, payment data |
| "Roles solve all auth" | domain ownership and tenant checks still matter |

---

## 21. Strong Closing Answer

```text
For microservices security, I layer controls: edge authentication, service-level authorization,
service-to-service identity through mTLS or workload identity, network policy for blast
radius, centralized secret management with rotation, data classification, and audit logs for
sensitive actions. I treat internal traffic as untrusted until identity and authorization are
verified.
```
