# Microservices Security Zero Trust FAANG Master Sheet

Target: starter, intermediate, senior, and FAANG-level microservices interviews.

This sheet covers:
- zero trust
- edge authentication
- service-level authorization
- OAuth2/JWT
- service-to-service identity
- mTLS
- secrets management
- network policies
- data protection
- audit logging
- threat modeling

Goal:

```text
After reading this sheet, you should be able to explain how to secure microservices from
edge to service-to-service traffic, protect secrets, enforce authorization, and debug common
security failures.
```

---

## 0. How To Use This Guide By Level

| Level | What To Focus On |
|---|---|
| Starter | auth vs authorization, JWT, API Gateway |
| Intermediate | service authorization, OAuth2 scopes, secrets |
| Senior | zero trust, mTLS, service identity, network policies |
| FAANG-ready | threat modeling, least privilege, auditability, incident response |

Must-say line:

```text
In microservices, security must be enforced at the edge and inside services. The gateway
alone is not enough.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Auth vs authorization | Very high | Foundation |
| API Gateway auth | Very high | Edge security |
| Service-level authorization | Very high | Prevent gateway bypass risk |
| JWT validation | Very high | Common backend auth |
| OAuth2 scopes | High | Permission model |
| mTLS | High | Service-to-service trust |
| Service identity | High | Zero trust |
| Secrets management | Very high | Credential safety |
| Least privilege | Very high | Blast radius reduction |
| Network policy | Medium-high | Service isolation |
| Audit logs | High | Compliance and forensics |
| Data classification | High | Sensitive data protection |

---

# 2. Zero Trust

Zero trust means:

```text
Do not trust a request just because it is inside the network.
```

Principles:
- authenticate every caller
- authorize every action
- use least privilege
- encrypt traffic
- verify service identity
- log sensitive actions
- rotate secrets
- minimize blast radius

Strong answer:

```text
In zero trust, internal traffic is not automatically trusted. Each service validates identity,
authorizes access, and communicates over secured channels.
```

---

# 3. Edge Authentication

At the edge:
- user logs in
- identity provider issues token
- gateway validates token
- request routed to service

Gateway can enforce:
- valid token
- coarse route access
- rate limiting
- CORS
- request size

Gateway should not be the only line of defense.

Strong answer:

```text
The gateway is a strong first layer, but services still enforce business authorization
because internal bypass or service-to-service calls can happen.
```

---

# 4. Service Authorization

Service authorization answers:

```text
Can this authenticated caller perform this business action?
```

Examples:
- customer can view own booking
- support can view assigned customer booking
- admin can cancel any booking
- payment service can update payment status

Strong answer:

```text
URL-level authorization is not enough. Sensitive operations need business-level checks such
as ownership, tenant, role, or scope validation.
```

---

# 5. JWT In Microservices

JWT should be validated for:
- signature
- expiry
- issuer
- audience
- not-before
- scopes/roles

Do not put:
- passwords
- credit card data
- secrets
- sensitive PII

Strong answer:

```text
JWT is signed, not automatically encrypted. Services can validate claims locally, but token
revocation needs extra design like short expiry, refresh token revocation, denylist, or
opaque token introspection.
```

---

# 6. OAuth2 Scopes And Roles

Scopes represent permissions:

```text
booking.read
booking.write
payment.authorize
```

Roles represent user category:

```text
CUSTOMER
SUPPORT
ADMIN
```

Strong answer:

```text
Scopes are useful for API permissions, while roles describe user or service category. Many
systems use both.
```

---

# 7. Service-To-Service Authentication

Options:
- OAuth2 client credentials
- mTLS certificate identity
- service mesh identity
- signed service tokens
- cloud workload identity

Client credentials example:

```text
booking-service obtains token with scope payment.authorize
payment-service validates token and scope
```

Strong answer:

```text
Service-to-service calls should use service identity, not shared user credentials or static
hardcoded secrets.
```

---

# 8. mTLS

mTLS means both client and server prove identity using certificates.

Benefits:
- encrypts traffic
- authenticates services
- prevents simple impersonation
- supports zero-trust networks

Costs:
- certificate management
- rotation complexity
- debugging complexity
- platform support needed

Strong answer:

```text
mTLS is valuable for service-to-service trust, especially when managed by a service mesh or
platform that handles certificate rotation.
```

---

# 9. Service Mesh Security

Service mesh can provide:
- mTLS
- service identity
- traffic policy
- retries/timeouts
- authorization policy
- telemetry

Examples:
- Istio
- Linkerd
- Consul

Senior caution:

```text
Service mesh is powerful, but it adds operational complexity. Use it when the platform can
operate it well.
```

---

# 10. Secrets Management

Secrets include:
- DB passwords
- API keys
- OAuth client secrets
- signing keys
- encryption keys
- certificates

Do not:
- commit secrets to Git
- log secrets
- put secrets in plain config
- share one secret across many services

Use:
- Vault
- cloud secrets manager
- Kubernetes secrets with external secret operator
- KMS
- short-lived credentials
- rotation policy

Strong answer:

```text
Secrets should come from a secrets manager with access control, audit, and rotation. Config
and secrets are not the same thing.
```

---

# 11. Secret Rotation

Rotation plan:
1. Add new secret version.
2. Deploy services that can read new version.
3. Switch traffic/use.
4. Revoke old secret.
5. Monitor failures.

Senior line:

```text
Secrets must be rotatable without emergency redeploy chaos. Rotation should be practiced,
not only documented.
```

---

# 12. Least Privilege

Least privilege means every service gets only permissions it needs.

Examples:
- Booking Service can create payment authorization request, not refund all payments.
- Notification Service can read notification templates, not booking payment data.
- Reporting Service reads replicated data, not production write database.

Strong answer:

```text
Least privilege reduces blast radius. If one service is compromised, it cannot access or
change everything.
```

---

# 13. Network Policy

Network policy restricts which services can talk.

Example:

```text
Only booking-service can call payment-service authorization endpoint.
Notification-service cannot call payment-service.
```

Useful in:
- Kubernetes
- service mesh
- cloud security groups

Strong answer:

```text
Network policy is not a replacement for application authorization, but it reduces the
reachable attack surface.
```

---

# 14. Data Protection

Protect data:
- in transit with TLS
- at rest with encryption
- field-level encryption for highly sensitive data
- tokenization for payment data
- masking in logs
- access controls
- retention policies

PII examples:
- email
- phone
- address
- passport/ID
- payment metadata

Strong answer:

```text
I classify data first. Sensitive data needs stricter storage, transport, logging, access,
and retention controls.
```

---

# 15. Audit Logging

Audit logs record security-sensitive actions.

Examples:
- admin cancels booking
- support views customer PII
- refund initiated
- role changed
- secret rotated
- failed login attempts

Audit log should include:
- who
- what
- when
- where
- target resource
- result
- correlation ID

Strong answer:

```text
Audit logs are not normal debug logs. They must be reliable, tamper-resistant, searchable,
and safe from sensitive payload leakage.
```

---

# 16. Threat Modeling

Ask:
1. Who can call this?
2. What data is exposed?
3. What if token is stolen?
4. What if service is compromised?
5. What if gateway is bypassed?
6. What if event is replayed?
7. What if logs leak?
8. What if secret is old or shared?

Common threats:
- broken access control
- token leakage
- SSRF
- replay attack
- insecure direct object reference
- over-permissive service account
- secret exposure
- dependency vulnerability

---

# 17. Event Security

Events can leak data.

Rules:
- do not publish sensitive payload unnecessarily
- encrypt sensitive topics if needed
- control topic ACLs
- include correlation ID, not raw secrets
- use schema governance
- restrict replay access

Strong answer:

```text
Events are data contracts and data leaks if designed poorly. I publish only what consumers
need and protect topics with ACLs and retention policies.
```

---

# 18. Production Scenario: Gateway Bypass

Problem:

```text
Internal caller directly calls Booking Service admin endpoint, bypassing gateway checks.
```

Defense:
1. Service validates JWT/service token.
2. Method-level authorization checks admin/support role.
3. Network policy limits callers.
4. Audit log records admin action.
5. Gateway remains first layer, not only layer.

Strong answer:

```text
I never rely only on gateway authorization for sensitive operations. Services must enforce
authorization themselves.
```

---

# 19. Common Mistakes

| Mistake | Why Wrong | Better |
|---|---|---|
| Trust internal network | lateral movement risk | zero trust |
| Gateway-only auth | bypass risk | service-level auth |
| Secrets in Git | credential leak | secrets manager |
| Long-lived shared secrets | high blast radius | short-lived/rotated credentials |
| JWT contains PII/secrets | readable payload | minimal claims |
| No audience validation | token confusion | validate aud/iss |
| No audit logs | no forensics | audit sensitive actions |
| Overbroad service account | compromise impact | least privilege |
| Logs contain tokens | token theft | mask headers |
| Event has sensitive payload | topic data leak | minimize event data |

---

# 20. Hot Interview Questions

### Q1. What is zero trust?

```text
Do not automatically trust internal traffic. Authenticate and authorize every request.
```

### Q2. Is gateway auth enough?

```text
No. Services must still enforce business authorization and ownership checks.
```

### Q3. How do services authenticate each other?

```text
OAuth2 client credentials, mTLS, service mesh identity, or cloud workload identity.
```

### Q4. How do you manage secrets?

```text
Use a secrets manager with access control, audit, and rotation. Do not put secrets in Git.
```

### Q5. What is mTLS?

```text
Mutual TLS where both client and server authenticate with certificates.
```

---

# 21. Final Rapid Revision

| Need | Concept |
|---|---|
| Edge auth | API Gateway + IdP |
| Business permission | service authorization |
| Internal identity | service identity |
| Secure service traffic | mTLS |
| Permission minimization | least privilege |
| Secret storage | secrets manager |
| Secret lifecycle | rotation |
| Reduce network reach | network policy |
| Sensitive action history | audit logs |
| Security analysis | threat model |

---

# 22. Strong Closing Answer

If interviewer asks:

```text
How do you secure microservices?
```

Say:

```text
I use defense in depth. The gateway validates external identity and handles edge concerns,
but each service still enforces business authorization. Service-to-service traffic uses
service identity, often OAuth2 client credentials or mTLS. Secrets come from a secrets
manager with rotation and audit. I apply least privilege, network policies, sensitive data
classification, token masking, audit logs, and threat modeling for high-risk flows.
```

---

# 23. Official Source Notes

Useful references:

- OWASP API Security Top 10: https://owasp.org/API-Security/
- Kubernetes Secrets: https://kubernetes.io/docs/concepts/configuration/secret/
- Kubernetes Network Policies: https://kubernetes.io/docs/concepts/services-networking/network-policies/
- Istio Security: https://istio.io/latest/docs/concepts/security/
- SPIFFE/SPIRE: https://spiffe.io/docs/latest/spiffe-about/overview/

