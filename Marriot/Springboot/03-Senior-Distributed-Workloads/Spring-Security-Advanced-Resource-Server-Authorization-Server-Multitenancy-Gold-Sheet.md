# Spring Security Advanced Resource Server Authorization Server Multitenancy Gold Sheet

> Track: Spring Boot Interview Track - Senior Distributed Workloads  
> Goal: deepen Spring Security beyond JWT basics into resource server, authorization server awareness, tenant isolation, and production failure modes.

Read after Spring Security JWT OAuth.

---

## 1. Security Architecture In Spring Boot Services

Most modern Spring Boot APIs act as OAuth2 Resource Servers.

```text
Client -> Identity Provider -> access token -> Spring Boot Resource Server -> protected API
```

Resource server responsibilities:

- validate token signature
- validate issuer
- validate audience
- validate expiration
- map claims to authorities
- enforce URL/method/domain authorization
- emit audit logs for sensitive actions

Strong answer:

```text
A resource server does not issue tokens. It validates access tokens and authorizes API calls
based on claims, scopes, roles, and domain rules.
```

---

## 2. OAuth2 Resource Server Setup

Typical config:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://idp.example.com/realms/hotel
```

Spring Security uses issuer metadata to discover JWKS.

Critical checks:

- issuer is trusted
- token is signed by trusted key
- token is not expired
- audience matches this API
- claims map correctly to authorities

Interview trap:

```text
Decoding JWT payload is not validation. Signature, issuer, audience, and expiry matter.
```

---

## 3. JWKS And Key Rotation

JWKS provides public keys for signature validation.

Flow:

```text
Token contains kid -> Resource Server fetches matching public key -> verifies signature
```

Production concerns:

- cache JWKS, do not fetch per request
- support overlapping old/new keys
- alert on unknown `kid` spikes
- handle temporary IdP/JWKS outage with cached keys
- rotate keys before old tokens expire

Strong answer:

```text
Key rotation needs overlap. Resource servers must accept tokens signed by old and new keys
until old tokens expire.
```

---

## 4. Scopes, Roles, Authorities

| Concept | Example | Use |
|---|---|---|
| Scope | `booking:write` | OAuth permission requested by client |
| Role | `ROLE_HOTEL_ADMIN` | coarse business role |
| Authority | `SCOPE_booking:write` | Spring Security granted authority |
| Domain rule | owns booking B123 | service-layer authorization |

Spring maps scopes commonly as:

```text
scope booking:write -> authority SCOPE_booking:write
```

Strong answer:

```text
Scopes and roles are not enough for every decision. A user with booking read scope still
must be checked against booking ownership or tenant boundary.
```

---

## 5. Method Security

Enable method security:

```java
@EnableMethodSecurity
@Configuration
class SecurityConfig {}
```

Examples:

```java
@PreAuthorize("hasAuthority('SCOPE_booking:write')")
public BookingResponse createBooking(CreateBookingCommand command) { }
```

Domain authorization:

```java
@PreAuthorize("@bookingSecurity.canCancel(authentication, #bookingId)")
public void cancelBooking(Long bookingId) { }
```

Strong answer:

```text
URL security is useful, but method security protects service operations and can include
domain-specific checks.
```

---

## 6. Multi-Tenant Security

Tenant isolation is a correctness and security invariant.

Every request should carry tenant context from a trusted source:

- token claim
- resolved account context
- route/domain mapping
- internal service context

Protect:

- database queries
- cache keys
- events
- logs
- search indexes
- admin APIs

Example bug:

```text
Cache key uses bookingId only. Tenant A receives cached BookingResponse for Tenant B.
```

Fix:

```text
Cache key includes tenantId + bookingId, and service verifies tenant ownership before read.
```

---

## 7. Tenant-Aware Data Access

Patterns:

| Pattern | Notes |
|---|---|
| tenant_id column | simple, common, requires query discipline |
| schema per tenant | stronger separation, operational overhead |
| database per tenant | high isolation, high operational cost |
| discriminator filters | convenient but must be tested carefully |

Strong answer:

```text
For most SaaS APIs, tenant_id column plus strict authorization, indexes, query filters,
cache key isolation, and tests is common. Higher-risk tenants may require stronger isolation.
```

---

## 8. Spring Authorization Server Awareness

Spring Authorization Server issues OAuth2/OIDC tokens.

Use when:

- organization needs its own authorization server
- custom OAuth2/OIDC flows are required
- token issuance is part of platform responsibility

Avoid casually:

```text
Running an authorization server is security-critical. Most product teams should use a mature
IdP unless they have a strong platform/security reason.
```

Interview answer:

```text
In most microservice teams, Spring Boot services are resource servers, not authorization
servers. Building an auth server with Spring Authorization Server is a platform-level choice.
```

---

## 9. Service-To-Service Calls

Options:

- client credentials flow
- token exchange / on-behalf-of flow
- mTLS/workload identity at platform layer
- gateway/service mesh identity

Questions:

1. Is service acting as itself or on behalf of a user?
2. Which scopes does downstream require?
3. How are tokens cached and refreshed?
4. How are failures handled when IdP is down?
5. What is audited?

Strong answer:

```text
For service-to-service calls, I distinguish service identity from user delegation. A service
token should not silently grant all user permissions.
```

---

## 10. CSRF, CORS, And Browser APIs

CSRF matters mainly for cookie-based browser sessions.

JWT bearer APIs:

```text
If token is sent in Authorization header and not automatically attached by browser, CSRF
risk is usually different from cookie sessions.
```

CORS:

```text
CORS is a browser policy, not backend authorization. Allowing an origin does not mean the
user is authorized.
```

Strong answer:

```text
I do not confuse CORS with security authorization. CORS controls which browser origins can
call the API; Spring Security still enforces authentication and authorization.
```

---

## 11. Audit Logging

Audit sensitive Spring Security decisions:

- login/token issue if owned by platform
- access denied for sensitive action
- booking cancellation
- payment refund/capture
- admin permission change
- support impersonation
- tenant admin changes

Audit fields:

- actor id
- tenant id
- action
- resource id
- result
- reason
- timestamp
- correlation id
- source IP/client id where safe

---

## 12. Production Failure Scenarios

### JWKS Endpoint Down

Symptoms:

- new pods cannot validate tokens if keys not cached
- authentication failures spike

Mitigation:

- cache keys
- canary deployments
- IdP health dashboards
- fail closed for invalid tokens

### Wrong Audience Accepted

Risk:

```text
Token meant for another API can call this service.
```

Fix:

```text
Validate audience explicitly.
```

### Tenant Leak

Debug:

- check token tenant claim
- check query filters
- check cache keys
- check search index tenant filters
- check logs/audit affected resources

---

## 13. Common Interview Traps

| Trap | Better Answer |
|---|---|
| JWT decoded means valid | verify signature, issuer, audience, expiry |
| gateway auth is enough | service enforces domain authorization |
| scopes solve everything | ownership/tenant/state rules still needed |
| CORS is authorization | CORS is browser-origin control |
| build auth server casually | authorization server is platform/security-critical |
| cache user data by id only | include tenant/user/security dimensions |

---

## 14. Strong Closing Answer

```text
For advanced Spring Security, I treat most services as OAuth2 Resource Servers: validate JWT
properly, map scopes/roles to authorities, enforce method and domain authorization, protect
tenant boundaries in queries/cache/events, audit sensitive actions, and understand that
running Spring Authorization Server is a platform-level responsibility, not a casual app
feature.
```
