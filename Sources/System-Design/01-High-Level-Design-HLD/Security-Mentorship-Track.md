# Security - Mentorship Track

> Goal: build strong intuition and interview-ready depth for designing secure systems, protecting user data, and making practical security trade-offs at scale.

---

## How We Will Use This Sheet

- We will keep this sheet focused on `1.10 Security`.
- We will follow the same learning style used in the asynchronous-system, reliability, and observability notes.
- We will add topics one by one in a repeatable architect-level structure.
- We will include code samples, mini programs, and interview-style answers where they help explain the mechanism.

---

## Roadmap for This Sheet

1. Authentication vs authorization
2. OAuth / JWT conceptual flow
3. Rate limiting
4. DDoS protection
5. Encryption in transit and at rest
6. Secrets management

---

# Topic 1: Authentication vs Authorization

> Track: 1.10 Security
> Scope: identity verification, access decisions, principals, permissions, roles, scopes, policy checks, and secure request flow

---

## 1. Intuition

Think of a hotel.

- Authentication is checking who you are at the front desk.
- Authorization is deciding what you are allowed to access after your identity is known.

If you show a valid ID and reservation, the hotel knows you are a real guest. That is authentication.

But being a guest does not mean you can enter every room, open the manager's office, or access another guest's bill. Those are authorization decisions.

Short memory trick:
- authentication answers: who are you?
- authorization answers: what can you do?

In system design, confusing these two is a major interview red flag. A valid login only proves identity. Every sensitive action still needs an access check.

---

## 2. Definition

- Definition: Authentication is the process of verifying the identity of a user, service, device, or workload.
- Definition: Authorization is the process of deciding whether an authenticated or anonymous principal is allowed to perform a specific action on a specific resource.
- Category: Identity and access control
- Core idea: First establish identity, then evaluate permissions for each protected action.

Interview shortcut:
- authentication creates trust in the caller's identity
- authorization limits what that identity can do
- authentication without authorization is unsafe
- authorization without reliable authentication is weak unless the resource is intentionally public

---

## 3. Why It Exists

Systems need to know both identity and permission.

Identity alone is not enough.

Examples:
- A hotel guest can view their own reservation, but not another guest's reservation.
- A support agent can read a booking, but may not be allowed to refund payment.
- A service account can call the pricing service, but should not call the payroll service.
- An admin can manage hotel inventory, but should still be blocked from accessing raw card data.

Without authentication:
- the system cannot reliably know who is making the request
- attackers can impersonate users or services
- audit logs become weak because actions cannot be tied to a trustworthy identity

Without authorization:
- any logged-in user may access sensitive resources
- horizontal privilege escalation becomes likely
- internal services may overreach their intended permissions
- admin-only actions may accidentally become available to normal users

Security exists at this boundary because most serious business systems are not just asking, "is this a valid user?" They are asking, "is this exact actor allowed to do this exact thing right now?"

---

## 4. Reality

### Authentication and authorization are common in:

- public web and mobile applications
- enterprise SaaS platforms
- payment and booking systems
- internal microservices
- APIs exposed to partners
- admin portals and operational tooling
- data platforms with tenant isolation

### Common authentication mechanisms

- username and password
- multi-factor authentication
- single sign-on through an identity provider
- API keys for machine clients
- client certificates in service-to-service systems
- short-lived tokens issued after login or service identity verification

### Common authorization mechanisms

- role-based access control, or RBAC
- attribute-based access control, or ABAC
- access control lists
- OAuth scopes
- resource ownership checks
- policy engines such as OPA or custom policy services

### Real-world architecture truth

Authentication often happens near the edge, but authorization must happen close to the protected action.

Why:
- a gateway can validate a token and identify the caller
- only the application usually knows whether the caller can access a specific booking, invoice, tenant, or admin action

Another important truth:
- internal services still need access control

Trusting every request because it came from inside the network is an old and dangerous assumption. Modern systems usually prefer zero-trust thinking: verify identity and enforce least privilege even between services.

---

## 5. How It Works

At a high level:

1. A request reaches the edge layer, such as an API gateway, load balancer, or application service.
2. The system extracts credentials, such as a session cookie, bearer token, API key, or client certificate.
3. The authentication layer validates the credential and creates a principal, such as user ID, tenant ID, service name, roles, and scopes.
4. The request reaches the application endpoint.
5. The authorization layer checks whether the principal can perform the requested action on the requested resource.
6. If allowed, the application executes the business operation.
7. If denied, the system returns an appropriate error and records useful audit context.

### Authentication flow

- Validate credentials.
- Confirm the credential is not expired, revoked, malformed, or issued by an untrusted source.
- Produce identity context for the rest of the request.

Authentication answers:
- can we trust who this caller claims to be?

### Authorization flow

- Identify the action, resource, and principal.
- Load the relevant policy or permission model.
- Check role, scope, ownership, tenant boundary, and contextual conditions.
- Allow, deny, or require stronger verification.

Authorization answers:
- is this caller allowed to perform this action on this resource?

### User-to-service flow

Typical request path:
1. User logs in through an identity provider.
2. User receives a session cookie or token.
3. Client sends the credential with requests.
4. Backend validates the credential.
5. Backend checks permissions before reading or changing data.

### Service-to-service flow

Typical request path:
1. Service obtains a workload identity, client certificate, or short-lived service token.
2. Caller sends the credential to the downstream service.
3. Downstream validates the caller identity.
4. Downstream checks whether that service is allowed to call this endpoint or access this resource.

### Failure path

- If authentication fails, return `401 Unauthorized` in HTTP terminology.
- If authentication succeeds but authorization fails, return `403 Forbidden`.
- Avoid leaking sensitive details in the response.
- Log enough context for investigation without logging secrets or full tokens.

### Recovery path

- Expired credentials can be refreshed or reissued.
- Missing permissions can go through an approval or role-assignment flow.
- Misconfigured policies should be fixed centrally and audited.
- Suspicious failures may trigger account lockout, step-up verification, or security investigation.

---

## 6. What Problem It Solves

- Primary problem solved: prevents unauthenticated or unauthorized access to protected users, resources, operations, and data.
- Secondary benefits: tenant isolation, auditability, least privilege, safer internal service boundaries, and stronger compliance posture.
- Systems impact: turns security from a single login screen into request-by-request identity and permission enforcement.

This topic solves three practical problems:
- who is calling the system?
- what are they allowed to do?
- how do we prove and audit that access decisions were enforced correctly?

---

## 7. When to Rely on It

Use explicit authentication and authorization when:
- the system has user accounts
- resources belong to specific users, tenants, teams, or organizations
- APIs expose sensitive operations
- internal services call each other across trust boundaries
- compliance, auditability, or least privilege matters
- admin actions can affect money, data, security, or availability

Especially valuable for:
- booking and payment platforms
- healthcare and financial systems
- enterprise SaaS with many tenants
- partner APIs
- internal platform services
- admin dashboards and support tools

Strong interviewer keywords:
- principal
- identity provider
- session
- token validation
- RBAC
- ABAC
- scopes
- resource ownership
- least privilege
- tenant isolation

---

## 8. When Not to Use It

Do not add heavyweight access control everywhere blindly.

Be careful when:
- the resource is intentionally public
- the endpoint only serves static public assets
- the service is an early prototype with no sensitive data yet
- policy complexity is greater than the actual risk

Avoid these patterns:
- checking authentication only at the UI layer
- trusting user ID from request parameters without verifying ownership
- assuming a valid token means the user can access every resource
- hardcoding admin checks throughout many services
- giving broad permissions to service accounts because it is convenient

Better framing:
- keep public resources explicitly public
- enforce authorization near protected business actions
- centralize policy logic where possible
- use least privilege for both users and services

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Authentication and authorization | Protect identity, data, and operations while enabling auditability and tenant isolation | Add complexity, require careful policy design, and can create outages if identity or policy systems fail poorly |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Security vs user experience:
  stricter authentication such as MFA improves safety, but can add friction to login and recovery flows.
- Centralized policy vs local context:
  central authorization improves consistency, but application services still need resource-specific context such as ownership and tenant.
- Token lifetime vs risk:
  longer-lived credentials reduce refresh frequency, but increase damage if stolen.
- Least privilege vs operational speed:
  narrow permissions reduce blast radius, but require better permission management and onboarding processes.
- Performance vs correctness:
  caching authorization decisions improves latency, but stale permissions can allow access longer than intended.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Confusing login with permission | A logged-in user may still be forbidden from a resource | Check authorization for each protected action |
| Trusting `userId` from the URL | Attackers can change IDs and access another user's data | Derive identity from validated credentials and verify ownership |
| Only enforcing authorization in the frontend | Clients can be bypassed or modified | Enforce authorization on the backend |
| Using one broad admin role | It gives too much power and increases blast radius | Split roles and permissions by job need |
| Giving service accounts global access | A compromised service can access too much | Use service-specific least privilege |
| Caching authorization forever | Permission changes may not take effect quickly | Use short TTLs, versioned policies, or revocation-aware checks |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Access token lifetime:
  often 5 to 60 minutes depending on risk and refresh model
- Refresh token lifetime:
  often days to weeks, with rotation and revocation for higher-risk systems
- Session idle timeout:
  often 15 to 30 minutes for sensitive admin tools, longer for lower-risk consumer apps
- MFA trigger points:
  login from new device, password change, payout change, admin action, or high-value transaction
- Authorization decision latency:
  usually should stay in low milliseconds for hot paths, especially when policies are local or cached
- Audit retention:
  often months to years depending on compliance and business requirements
- Permission propagation delay:
  should be bounded and understood if permissions are cached or distributed

Interview shorthand:
- authenticate identity, authorize action, enforce server-side, audit sensitive decisions

---

## 12. Failure Modes

### Broken object-level authorization

Problem:
- The API validates that the user is logged in but does not verify that the requested booking belongs to the user's account or tenant.

User impact:
- one user can access or modify another user's data by changing an ID in the request

Mitigation:
- enforce resource ownership checks in the backend
- include tenant and owner constraints in data queries
- test authorization boundaries explicitly

### Over-permissive service account

Problem:
- A service account has broad read and write permissions across many systems because it was easier during development.

User impact:
- compromise or bug in one service can affect unrelated data or operations

Mitigation:
- use least privilege
- scope service identities by service and environment
- review permissions regularly

### Policy outage blocks everything

Problem:
- The authorization service becomes unavailable and every request depends on it synchronously.

User impact:
- users may be unable to use the product even though the core application is healthy

Mitigation:
- cache safe policy decisions carefully
- define fail-closed or fail-open behavior by endpoint risk
- keep critical policy paths highly available

### Stale permission cache

Problem:
- A user's role is removed, but cached permissions continue allowing access.

User impact:
- revoked users or services keep access longer than intended

Mitigation:
- use short cache TTLs for sensitive permissions
- include policy version checks
- support explicit revocation for high-risk roles

---

## 13. Scenario

- Product / system: Hotel booking platform with customer accounts, support agents, hotel managers, and internal pricing services
- Requirement:
  customers should access only their own bookings, support agents should access bookings only for assigned queues, hotel managers should update only their own properties, and pricing services should call only the APIs they need
- Good design:
  authenticate users through an identity provider, propagate trusted identity context to backend services, enforce resource-level authorization in each business service, and audit sensitive actions such as refunds, payout changes, and admin updates
- Why this concept fits:
  the platform has multiple actor types and sensitive resources where login alone is not enough
- What would go wrong without it:
  users or services could cross tenant boundaries, access private data, or perform actions beyond their role

---

## 14. Code Sample

### Server-side authentication and authorization check

```java
import java.util.Set;

public record Principal(String userId, String tenantId, Set<String> roles, Set<String> scopes) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
}

public record Booking(String bookingId, String tenantId, String ownerUserId) {
}

public class BookingAccessPolicy {

    public boolean canViewBooking(Principal principal, Booking booking) {
        if (!principal.tenantId().equals(booking.tenantId())) {
            return false;
        }

        if (principal.hasRole("SUPPORT_AGENT") && principal.hasScope("booking:read")) {
            return true;
        }

        return principal.userId().equals(booking.ownerUserId())
                && principal.hasScope("booking:read");
    }
}
```

Key idea:
- the credential proves the principal, but the application still checks tenant, role, scope, and resource ownership before returning data

---

## 15. Mini Program / Simulation

This mini program shows that a valid login is not enough. Each request still needs a resource-specific authorization check.

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class Principal:
    user_id: str
    tenant_id: str
    roles: set[str]
    scopes: set[str]


@dataclass(frozen=True)
class Booking:
    booking_id: str
    tenant_id: str
    owner_user_id: str


def authenticate(token: str) -> Principal | None:
    known_tokens = {
        "customer-token": Principal("user-100", "tenant-a", {"CUSTOMER"}, {"booking:read"}),
        "agent-token": Principal("agent-7", "tenant-a", {"SUPPORT_AGENT"}, {"booking:read"}),
        "other-tenant-token": Principal("user-200", "tenant-b", {"CUSTOMER"}, {"booking:read"}),
    }

    return known_tokens.get(token)


def can_view_booking(principal: Principal, booking: Booking) -> bool:
    if principal.tenant_id != booking.tenant_id:
        return False

    if "booking:read" not in principal.scopes:
        return False

    if "SUPPORT_AGENT" in principal.roles:
        return True

    return principal.user_id == booking.owner_user_id


def handle_request(token: str, booking: Booking) -> str:
    principal = authenticate(token)
    if principal is None:
        return "401 unauthenticated"

    if not can_view_booking(principal, booking):
        return "403 forbidden"

    return f"200 booking {booking.booking_id} returned"


def main() -> None:
    booking = Booking("booking-900", "tenant-a", "user-100")

    for token in ["customer-token", "agent-token", "other-tenant-token", "unknown-token"]:
        print(f"{token}: {handle_request(token, booking)}")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- invalid credentials fail authentication
- valid credentials can still fail authorization
- tenant boundary and ownership checks are backend responsibilities
- support access and customer access can use different authorization rules

---

## 16. Practical Question

> You are designing a hotel booking platform with customers, support agents, hotel managers, and internal services. How would you design authentication and authorization so users can access only the right bookings and services get only the permissions they need?

---

## 17. Strong Answer

I would separate authentication from authorization explicitly. Authentication would establish the caller identity using a trusted mechanism such as an identity provider for users and workload identity or short-lived service credentials for internal services. The result should be a principal that includes user ID, tenant ID, roles, scopes, and service identity where relevant.

Authorization would happen on the backend near the protected business action. For example, viewing a booking should check not only that the user is logged in, but also that the booking belongs to the same tenant and that the user is either the owner or has an approved support role with the right scope. I would never rely only on frontend checks or request parameters like `userId`.

For services, I would use least privilege so the pricing service can call pricing-related APIs but not unrelated payment or admin APIs. I would audit sensitive actions such as refunds, role changes, and payout updates. I would also define how policies are cached, revoked, and monitored so security does not become either too slow or too stale.

The key principle is simple: authenticate the caller once, but authorize every protected action with resource context.

---

## 18. Revision Notes

- One-line summary: Authentication verifies identity, while authorization decides whether that identity can perform a specific action on a specific resource.
- Three keywords: identity, permission, resource
- One interview trap: assuming a valid login or token automatically allows access to all resources
- One memory trick: ID at the front desk, keycard at the door

---

# Topic 2: OAuth / JWT Conceptual Flow

> Track: 1.10 Security
> Scope: delegated authorization, access tokens, refresh tokens, JWT structure, token validation, scopes, and secure API access

---

## 1. Intuition

Think of checking into a hotel through a trusted travel partner.

- You do not give the hotel your travel-site password.
- The travel partner confirms your booking and gives the hotel proof that you are allowed to check in.
- The hotel trusts the proof because it knows the travel partner.
- The proof may say what you can do, such as check in, modify the booking, or view loyalty details.

That is the basic feeling behind OAuth.

OAuth is not mainly about sharing passwords. It is about delegated access.

JWT is like a signed digital pass.

- It carries claims such as subject, issuer, audience, expiry, roles, and scopes.
- It can be verified by a backend using a trusted signing key.
- If valid, it gives the service identity context without calling the issuer on every request.

Short memory trick:
- OAuth is the permission flow
- JWT is one common token format
- scopes describe allowed access

---

## 2. Definition

- Definition: OAuth 2.0 is an authorization framework that lets a client obtain limited access to protected resources on behalf of a resource owner or service.
- Definition: A JWT, or JSON Web Token, is a compact signed token format that carries claims between parties.
- Category: Delegated authorization and token-based access control
- Core idea: Use a trusted authorization server to issue limited, time-bound tokens that resource servers can validate before serving protected APIs.

Interview shortcut:
- OAuth is a protocol framework
- JWT is a token format
- OpenID Connect adds identity information on top of OAuth
- access tokens are for APIs
- refresh tokens are for getting new access tokens

Important distinction:
- OAuth answers how a client gets delegated access
- JWT answers how token claims can be packaged and verified

---

## 3. Why It Exists

Systems need a secure way to let applications act with limited authority.

Bad old pattern:
- give a third-party app your username and password
- let the app store those credentials
- hope the app uses them safely

That is dangerous because:
- the app can do everything your password allows
- revoking access usually means changing your password
- every integration becomes a credential theft risk
- APIs cannot easily limit access by action, resource, or expiry

OAuth exists to replace password sharing with delegated, limited, revocable access.

JWT exists because distributed systems need a compact way to carry identity and authorization claims.

Without token-based access:
- every API may need to call a central session store for each request
- service-to-service authentication becomes inconsistent
- client integrations become hard to scope and revoke
- mobile and SPA applications struggle with secure API access patterns

These concepts exist so systems can say:
- this caller was authorized by a trusted issuer
- this token is valid only for this audience and time window
- this token grants only these scopes

---

## 4. Reality

### OAuth and JWT are common in:

- single sign-on systems
- public APIs and partner integrations
- mobile and web applications
- API gateways and microservices
- enterprise SaaS platforms
- identity providers such as Okta, Auth0, Azure AD, Cognito, and Keycloak

### Common OAuth actors

- Resource owner: usually the user who owns the data
- Client: the application requesting access
- Authorization server: the system that authenticates the user and issues tokens
- Resource server: the API that validates tokens and serves protected resources

### Common token types

- Access token:
  short-lived token used to call APIs
- Refresh token:
  longer-lived token used to obtain new access tokens
- ID token:
  OpenID Connect token that describes the authenticated user to the client

### Real-world architecture truth

Do not treat JWT as magic security.

A JWT is only trustworthy if:
- the signature is verified
- the issuer is trusted
- the audience matches the receiving service
- the token is not expired
- the algorithm is allowed
- the claims are interpreted correctly

Another important truth:
- JWT validation authenticates token claims, but it does not replace application authorization

The token may prove user identity and scopes. The application still needs to check resource ownership, tenant boundary, and business policy.

---

## 5. How It Works

At a high level for the common authorization-code flow:

1. The user opens a client application.
2. The client redirects the user to the authorization server.
3. The user authenticates with the authorization server.
4. The authorization server asks for consent or applies policy.
5. The authorization server redirects back to the client with an authorization code.
6. The client exchanges the code for tokens through a secure back-channel.
7. The client calls the resource server with an access token.
8. The resource server validates the token and checks authorization before serving the request.

### OAuth flow

- The client asks for limited access, usually through scopes.
- The authorization server authenticates the user or service.
- The authorization server issues a token with limited lifetime and permissions.
- The API trusts the authorization server, not the client blindly.

OAuth answers:
- how did this client get permission to call this API?

### JWT structure

A JWT usually has three parts:

1. Header:
   token type and signing algorithm
2. Payload:
   claims such as subject, issuer, audience, expiry, roles, and scopes
3. Signature:
   proof that the token was signed by a trusted issuer and has not been tampered with

JWT answers:
- what claims are being presented, and can we verify they were issued by someone we trust?

### API validation flow

When an API receives a bearer token:

1. Parse the token.
2. Verify the signature using trusted keys.
3. Validate issuer, audience, expiry, and not-before time.
4. Extract claims such as subject, tenant, roles, and scopes.
5. Check scopes and resource-level authorization.
6. Return the response or reject the request.

### Failure path

- Missing token: return `401 Unauthorized`.
- Invalid signature: return `401 Unauthorized`.
- Expired access token: return `401 Unauthorized`, and the client may refresh.
- Valid token but insufficient scope: return `403 Forbidden`.
- Valid scope but wrong resource owner or tenant: return `403 Forbidden`.

### Recovery path

- Use refresh tokens to obtain new access tokens.
- Rotate signing keys carefully with overlap windows.
- Revoke compromised refresh tokens.
- Reduce token lifetime if stolen-token risk is high.
- Fix scope or policy configuration if access is denied incorrectly.

---

## 6. What Problem It Solves

- Primary problem solved: enables secure, delegated, limited API access without sharing user passwords with every application.
- Secondary benefits: single sign-on, centralized identity, short-lived credentials, scoped access, easier API integration, and distributed token validation.
- Systems impact: separates identity issuance from resource serving while allowing APIs to validate access consistently.

This topic solves three practical problems:
- how does a client get permission to call an API?
- how does an API verify the caller without handling passwords?
- how can permissions be limited by scope, audience, and expiry?

---

## 7. When to Rely on It

Use OAuth and token-based access when:
- users authenticate through a central identity provider
- third-party or partner applications need limited API access
- multiple services need consistent identity propagation
- mobile apps, SPAs, or backend clients call protected APIs
- short-lived tokens and scoped permissions are useful
- the system needs SSO or enterprise identity integration

Especially valuable for:
- SaaS platforms
- public developer APIs
- partner integrations
- mobile applications
- API gateways
- enterprise admin portals

Strong interviewer keywords:
- authorization server
- resource server
- authorization code
- PKCE
- access token
- refresh token
- issuer
- audience
- scope
- JWKS

---

## 8. When Not to Use It

OAuth and JWT are not automatically the right answer for every system.

Be careful when:
- the application is a simple server-rendered app where secure server-side sessions are enough
- tokens would be stored insecurely in a browser or mobile device
- immediate revocation is required but tokens are long-lived and self-contained
- the team does not understand token validation rules deeply enough
- scopes are too broad or poorly modeled

Avoid these patterns:
- storing access tokens in unsafe browser storage
- using JWTs without verifying the signature
- accepting tokens issued for a different audience
- putting secrets or sensitive PII inside JWT payloads
- using long-lived access tokens because refresh flow feels inconvenient
- treating scopes as a replacement for resource ownership checks

Better framing:
- use OAuth for delegated API authorization
- use OpenID Connect when you need login identity
- use short-lived access tokens
- keep refresh tokens protected and revocable
- validate every token claim that matters

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| OAuth with JWT access tokens | Enables delegated access, SSO, scoped permissions, and distributed token validation | Adds protocol complexity, requires careful token storage and validation, and self-contained tokens can be hard to revoke immediately |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Stateless validation vs revocation:
  JWTs can be verified locally, but immediate revocation is harder than with central sessions.
- Short token lifetime vs user experience:
  short-lived access tokens reduce stolen-token risk, but require refresh logic.
- Central identity vs dependency risk:
  an identity provider improves consistency, but login and token issuance depend on that provider's availability.
- Rich claims vs token bloat:
  more claims reduce lookup needs, but increase token size and risk stale authorization data.
- Broad scopes vs least privilege:
  broad scopes simplify integrations, but increase blast radius when tokens leak.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Confusing OAuth with login | OAuth is authorization; login identity usually comes from OpenID Connect | Use OIDC for authentication and OAuth scopes for API access |
| Not validating audience | A token meant for another API may be accepted incorrectly | Validate `aud` for every resource server |
| Trusting unsigned or weak-algorithm tokens | Attackers may forge or tamper with claims | Allow only approved algorithms and verify signatures |
| Storing secrets in JWT payload | JWT payload is encoded, not encrypted by default | Store only non-sensitive claims or use encrypted tokens when needed |
| Using long-lived access tokens | Stolen tokens remain useful for too long | Use short-lived access tokens and protected refresh tokens |
| Skipping backend authorization | Token claims may not prove access to a specific resource | Check scopes, tenant, ownership, and policy at the API |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Access token lifetime:
  often 5 to 60 minutes, shorter for high-risk APIs
- Refresh token lifetime:
  often days to weeks, with rotation and revocation
- Authorization code lifetime:
  usually very short, often around 1 to 10 minutes
- Clock skew tolerance:
  often around 30 to 120 seconds when validating `exp` and `nbf`
- JWKS cache TTL:
  often minutes to hours, depending on key rotation policy
- JWT size:
  keep tokens small enough to avoid header bloat and proxy limits
- Key rotation overlap:
  old and new signing keys often overlap temporarily so in-flight tokens still validate

Interview shorthand:
- OAuth flow, JWT claims, validate issuer and audience, short-lived access token, protected refresh token

---

## 12. Failure Modes

### Token audience confusion

Problem:
- Service A accepts a token that was issued for Service B.

User impact:
- a token with limited intended use can access unintended APIs

Mitigation:
- validate the `aud` claim strictly
- use separate audiences for different APIs
- reject tokens with missing or unexpected audience

### Stolen refresh token

Problem:
- A long-lived refresh token is stolen from a client device.

User impact:
- an attacker can continue obtaining fresh access tokens

Mitigation:
- rotate refresh tokens
- bind tokens to device or client where possible
- support revocation and anomaly detection
- require reauthentication for sensitive actions

### Key rotation failure

Problem:
- The authorization server rotates signing keys, but resource servers have stale key caches.

User impact:
- valid users suddenly receive authentication failures

Mitigation:
- publish keys through JWKS
- cache keys with sensible TTLs
- overlap old and new keys during rotation
- monitor token-validation failures during rotation

### Overloaded identity provider

Problem:
- Every API request introspects tokens synchronously against the identity provider.

User impact:
- login or API access becomes slow or unavailable when the identity provider is under load

Mitigation:
- validate JWTs locally where appropriate
- cache introspection results safely
- reserve synchronous introspection for high-risk or opaque tokens

---

## 13. Scenario

- Product / system: Hotel booking platform with mobile app, web app, partner travel agencies, and internal booking APIs
- Requirement:
  users should sign in through a trusted identity provider, partners should receive limited access to booking APIs, and backend services should validate requests without handling passwords directly
- Good design:
  use OAuth authorization-code flow with PKCE for public clients, issue short-lived access tokens with booking-related scopes, validate JWT issuer and audience at APIs, and enforce resource ownership in booking services
- Why this concept fits:
  the platform needs delegated API access, centralized identity, and short-lived credentials across many clients and services
- What would go wrong without it:
  partners might need user passwords, APIs might trust weak credentials, and permissions would be hard to limit or revoke

---

## 14. Code Sample

### Validating token claims before serving an API request

```java
import java.time.Instant;
import java.util.Set;

public record TokenClaims(
        String issuer,
        String audience,
        String subject,
        String tenantId,
        Set<String> scopes,
        Instant expiresAt) {
}

public class AccessTokenPolicy {

    private static final String TRUSTED_ISSUER = "https://identity.example.com";
    private static final String EXPECTED_AUDIENCE = "hotel-booking-api";

    public boolean canUseForBookingRead(TokenClaims claims, String bookingTenantId) {
        if (!TRUSTED_ISSUER.equals(claims.issuer())) {
            return false;
        }

        if (!EXPECTED_AUDIENCE.equals(claims.audience())) {
            return false;
        }

        if (claims.expiresAt().isBefore(Instant.now())) {
            return false;
        }

        if (!claims.scopes().contains("booking:read")) {
            return false;
        }

        return claims.tenantId().equals(bookingTenantId);
    }
}
```

Key idea:
- token validation is not just decoding a JWT; the API must validate issuer, audience, expiry, scope, and resource context

---

## 15. Mini Program / Simulation

This mini program simulates simple JWT-claim validation without implementing cryptography. In real systems, signature verification is mandatory.

```python
from dataclasses import dataclass
from time import time


@dataclass(frozen=True)
class Token:
    issuer: str
    audience: str
    subject: str
    tenant_id: str
    scopes: set[str]
    expires_at: int
    signature_valid: bool


def validate_token(token: Token, expected_audience: str, required_scope: str, resource_tenant: str) -> str:
    if not token.signature_valid:
        return "401 invalid signature"

    if token.issuer != "https://identity.example.com":
        return "401 invalid issuer"

    if token.audience != expected_audience:
        return "401 invalid audience"

    if token.expires_at <= int(time()):
        return "401 expired token"

    if required_scope not in token.scopes:
        return "403 missing scope"

    if token.tenant_id != resource_tenant:
        return "403 wrong tenant"

    return "200 token accepted"


def main() -> None:
    now = int(time())
    tokens = [
        Token("https://identity.example.com", "hotel-booking-api", "user-1", "tenant-a", {"booking:read"}, now + 300, True),
        Token("https://identity.example.com", "payments-api", "user-1", "tenant-a", {"booking:read"}, now + 300, True),
        Token("https://identity.example.com", "hotel-booking-api", "user-2", "tenant-b", {"booking:read"}, now + 300, True),
        Token("https://identity.example.com", "hotel-booking-api", "user-3", "tenant-a", {"profile:read"}, now + 300, True),
    ]

    for token in tokens:
        result = validate_token(token, "hotel-booking-api", "booking:read", "tenant-a")
        print(f"subject={token.subject} result={result}")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- a valid-looking token can still be rejected for wrong audience
- scopes are necessary but not enough
- tenant checks still happen at the resource server
- signature validation is a required first gate

---

## 16. Practical Question

> You are designing login and partner API access for a hotel booking platform. How would you use OAuth and JWTs safely, and what validations should each backend service perform before serving protected booking APIs?

---

## 17. Strong Answer

I would use OAuth 2.0 for delegated API authorization and OpenID Connect if the client needs user login identity. For public clients like a mobile app or browser SPA, I would prefer authorization-code flow with PKCE. The authorization server would issue short-lived access tokens and protected refresh tokens. Access tokens would include only necessary claims such as subject, issuer, audience, expiry, tenant, and scopes.

Each backend API would validate the token before trusting it. That means verifying the signature using trusted keys, checking issuer, audience, expiry, and required scopes. After that, the service still needs business authorization. For example, `booking:read` means the token may read bookings, but the booking service must still check that the booking belongs to the same tenant and that the user or partner is allowed to access that specific booking.

I would keep access tokens short-lived, rotate signing keys safely, protect refresh tokens, and avoid placing sensitive PII or secrets inside JWT payloads. If immediate revocation is required, I would either keep token lifetimes short, use refresh-token revocation, or use token introspection for higher-risk paths. The core idea is delegated, time-bound, scoped access with strict validation at every resource server.

---

## 18. Revision Notes

- One-line summary: OAuth defines how limited API access is delegated, while JWT is a signed token format often used to carry claims for APIs to validate.
- Three keywords: issuer, audience, scope
- One interview trap: decoding a JWT and trusting its claims without verifying signature, expiry, issuer, and audience
- One memory trick: OAuth gets the pass, JWT carries the signed pass

---

# Topic 3: Rate Limiting

> Track: 1.10 Security
> Scope: abuse control, quota enforcement, API protection, tenant fairness, traffic shaping, and overload prevention

---

## 1. Intuition

Think of the front desk at a hotel during a busy holiday weekend.

- A normal guest may ask a few questions and check in.
- A travel agency may send many booking requests, but only within its contracted capacity.
- A suspicious caller repeatedly asking for every guest's booking details should be slowed or blocked.

The hotel needs a way to prevent one person or partner from consuming all staff attention.

That is what rate limiting does in systems.

- It controls how many requests a caller can make in a time window.
- It protects shared capacity from abuse, bugs, and traffic spikes.
- It keeps one tenant, user, IP, or client from harming everyone else.

Short memory trick:
- who is calling?
- how often are they calling?
- what should happen when they exceed the limit?

---

## 2. Definition

- Definition: Rate limiting is a control mechanism that restricts the number of requests or actions a caller can perform within a defined time period.
- Category: Traffic control, abuse prevention, and reliability protection
- Core idea: Enforce fair and safe usage by applying quotas to identities such as user, IP, API key, tenant, endpoint, or service.

Interview shortcut:
- rate limiting protects systems from too much legitimate or suspicious traffic
- throttling slows or rejects excess traffic
- quota defines the allowed amount
- key choice matters as much as the algorithm

---

## 3. Why It Exists

Systems have finite capacity.

Without rate limiting:
- one buggy client can overload an API
- one tenant can consume shared resources unfairly
- attackers can brute-force login, scrape data, or enumerate IDs
- expensive endpoints can drain database and downstream capacity
- retry storms can amplify an incident

Rate limiting exists because not all traffic is equally safe.

Examples:
- login attempts should be limited to reduce credential stuffing
- password-reset requests should be limited to prevent abuse
- partner APIs should enforce contractual quotas
- search endpoints should prevent scraping and expensive fan-out
- internal services should protect dependencies from retry storms

The important idea:
- rate limiting is both a security control and a reliability control

It protects against malicious actors, but it also protects against accidental overload from normal clients, bad deploys, and runaway retries.

---

## 4. Reality

### Rate limiting is common in:

- API gateways
- public REST and GraphQL APIs
- login and signup flows
- payment and checkout APIs
- search and inventory systems
- partner integrations
- internal microservice calls
- cloud platforms and SaaS products

### Common rate-limit keys

- IP address
- user ID
- tenant ID
- API key or client ID
- endpoint or route
- device ID
- service identity
- combinations such as tenant plus endpoint

### Common implementation layers

- CDN or edge layer for coarse IP and geographic protection
- API gateway for client, tenant, and endpoint limits
- application service for business-aware limits
- database or queue layer for backpressure and concurrency limits

### Real-world architecture truth

Rate limiting is only useful if the key matches the risk.

Examples:
- IP-based limits are useful at the edge but weak when many real users share one NAT IP.
- User-based limits are better after authentication, but do not help before login.
- Tenant-based limits protect fairness in multi-tenant systems.
- Endpoint-specific limits protect expensive operations better than one global limit.

Another important truth:
- rate limiting must be observable

Teams need to know who is being limited, why, and whether the limits are protecting the system or hurting legitimate users.

---

## 5. How It Works

At a high level:

1. A request reaches the edge, gateway, or service.
2. The system identifies the rate-limit key, such as IP, user, API key, or tenant.
3. The system identifies the policy for the endpoint or operation.
4. The system checks recent usage for that key.
5. If usage is under the limit, the request proceeds and usage is recorded.
6. If usage exceeds the limit, the request is rejected, delayed, or downgraded.
7. The response includes useful feedback, often with `429 Too Many Requests` and retry guidance.

### Policy flow

- Define limits by risk and cost.
- Apply stricter limits to login, password reset, checkout, and expensive search.
- Apply tenant or client-specific quotas for partner APIs.
- Allow higher limits for trusted internal services with separate safeguards.

Policy answers:
- how much traffic is safe for this caller and operation?

### Enforcement flow

- At the edge, use coarse controls to reduce obvious abuse.
- At the gateway, enforce client and tenant quotas.
- In the app, enforce business-specific limits such as bookings per minute or refund attempts per hour.

Enforcement answers:
- where should this traffic be slowed or rejected?

### Response flow

Common behavior when limit is exceeded:
- return `429 Too Many Requests`
- include `Retry-After` where appropriate
- avoid expensive downstream work
- emit metrics and structured logs
- optionally challenge, block, or require stronger verification for suspicious behavior

### Failure path

- If the rate-limit store is unavailable, the system must choose fail-open or fail-closed.
- For low-risk reads, fail-open may preserve availability.
- For login, payment, or sensitive admin actions, fail-closed or degraded limits may be safer.

### Recovery path

- Rate-limit counters expire naturally as windows move forward.
- Clients retry after backoff.
- Operators tune policies if legitimate traffic is being blocked.
- Abuse systems can temporarily block extreme offenders at the edge.

---

## 6. What Problem It Solves

- Primary problem solved: protects APIs and shared infrastructure from abusive, excessive, or unfair request patterns.
- Secondary benefits: tenant fairness, cost control, brute-force protection, scraping resistance, retry-storm mitigation, and graceful overload handling.
- Systems impact: turns uncontrolled demand into bounded, observable, and policy-driven traffic.

This topic solves three practical problems:
- how do we stop one caller from overwhelming shared capacity?
- how do we enforce fair usage between tenants and partners?
- how do we slow suspicious behavior before it becomes an outage or breach?

---

## 7. When to Rely on It

Use rate limiting when:
- the API is public or partner-facing
- endpoints are expensive or security-sensitive
- multiple tenants share infrastructure
- clients may retry aggressively
- brute-force, scraping, or enumeration risk exists
- downstream dependencies have limited capacity

Especially valuable for:
- login and MFA verification
- password reset
- search APIs
- booking and checkout APIs
- payment authorization
- file upload
- notification sending
- partner API quotas

Strong interviewer keywords:
- `429 Too Many Requests`
- quota
- token bucket
- sliding window
- per-tenant limit
- per-user limit
- retry-after
- abuse prevention
- backpressure

---

## 8. When Not to Use It

Rate limiting should not be the only protection.

Be careful when:
- limits are so strict that normal users are blocked
- one global limit punishes all tenants for one tenant's behavior
- IP-based limits block large offices, hotels, or mobile carriers using shared NAT
- rate limiting hides a deeper scaling or database design issue
- important internal systems need backpressure rather than simple rejection

Avoid these patterns:
- using one limit for every endpoint
- applying only IP limits after login when user identity is known
- returning vague errors that cause clients to retry harder
- failing open on sensitive login or payment abuse checks without fallback
- storing rate-limit counters in a single hot database row

Better framing:
- rate limit by caller and operation
- combine edge, gateway, and app-level controls
- use clear retry guidance
- monitor both blocked traffic and user impact

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Rate limiting | Protects APIs from overload and abuse, improves tenant fairness, and controls cost | Can block legitimate traffic if tuned poorly, needs distributed state at scale, and can be bypassed if keys are weak |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Safety vs availability:
  stricter limits protect the system but may reject legitimate bursts.
- Accuracy vs latency:
  centralized counters are more accurate but add network calls and possible bottlenecks.
- Local speed vs global correctness:
  per-node limits are fast but may allow total traffic above the intended global quota.
- Simplicity vs fairness:
  one global limit is easy, but per-user, per-tenant, and per-endpoint limits are fairer.
- Security vs user friction:
  aggressive login limits reduce brute force but can frustrate real users during account recovery.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Using only IP-based limits | Many users may share one IP, and attackers can rotate IPs | Combine IP, user, device, API key, and tenant signals |
| Applying the same limit to all endpoints | A cheap profile read and expensive search fan-out have different cost | Set endpoint-specific and operation-specific limits |
| No retry guidance | Clients may retry immediately and worsen overload | Return `429` with `Retry-After` or documented backoff behavior |
| Not rate limiting login attempts | Brute force and credential stuffing become easier | Apply account, IP, device, and risk-based limits |
| Storing counters in a fragile central store | Rate limiting can become a dependency outage | Design for sharding, replication, local fallback, or degraded mode |
| Forgetting observability | Operators cannot tell whether limits help or hurt | Emit limit-hit metrics by policy, endpoint, and caller class |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Login attempts:
  often limited per account, IP, and device over short windows such as minutes
- Public API quota:
  often defined per API key or client per minute, hour, or day
- Burst allowance:
  token bucket style policies often allow short bursts above steady rate
- Retry response:
  `429 Too Many Requests` is the common HTTP response for exceeded limits
- Counter TTL:
  should align with the rate-limit window and cleanup strategy
- Distributed-counter latency:
  should be very low for hot request paths, often single-digit milliseconds when externalized
- False-positive budget:
  legitimate traffic blocked by rate limits should be tracked and kept very low for critical user flows

Interview shorthand:
- key, policy, window, action, `429`, retry-after, observe and tune

---

## 12. Failure Modes

### Weak key choice

Problem:
- The system limits only by IP address for authenticated API traffic.

User impact:
- attackers rotate IPs, while legitimate users behind shared NAT may get blocked unfairly

Mitigation:
- combine IP with user ID, API key, tenant ID, and endpoint
- use risk-based policies for unauthenticated flows

### Global unfairness

Problem:
- One tenant sends a huge traffic burst and consumes the shared global quota.

User impact:
- other tenants are rejected even though they behaved normally

Mitigation:
- enforce per-tenant quotas
- reserve capacity for critical tenants or operations
- isolate heavy tenants where needed

### Rate-limit store outage

Problem:
- Redis or another counter store becomes unavailable.

User impact:
- the API either rejects too much traffic or allows too much traffic during abuse

Mitigation:
- define fail-open or fail-closed by endpoint risk
- use local emergency limits
- replicate or shard the counter store
- monitor rate-limiter dependency health

### Retry amplification

Problem:
- Clients receive failures and retry immediately without backoff.

User impact:
- traffic increases during an incident and makes recovery harder

Mitigation:
- return explicit retry guidance
- require exponential backoff with jitter for clients
- combine rate limiting with circuit breakers and backpressure

---

## 13. Scenario

- Product / system: Hotel search and booking API used by customers, mobile apps, and travel-agency partners
- Requirement:
  prevent scraping and login abuse, keep tenant usage fair, and protect expensive search and booking endpoints during traffic spikes
- Good design:
  enforce IP-based coarse limits at the edge, API-key and tenant limits at the gateway, user and endpoint-specific limits in the application, and stricter policies for login, password reset, checkout, and partner search APIs
- Why this concept fits:
  the platform has shared capacity, expensive endpoints, and abuse-prone flows
- What would go wrong without it:
  one partner, attacker, or buggy client could overload the search system or brute-force accounts while harming normal users

---

## 14. Code Sample

### Simple fixed-window rate limiter

```java
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class FixedWindowRateLimiter {

    private record Counter(long windowStartEpochSecond, int count) {
    }

    private final Map<String, Counter> counters = new HashMap<>();
    private final int maxRequests;
    private final long windowSeconds;

    public FixedWindowRateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public boolean allow(String key) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - (now % windowSeconds);
        Counter current = counters.get(key);

        if (current == null || current.windowStartEpochSecond() != windowStart) {
            counters.put(key, new Counter(windowStart, 1));
            return true;
        }

        if (current.count() >= maxRequests) {
            return false;
        }

        counters.put(key, new Counter(windowStart, current.count() + 1));
        return true;
    }
}
```

Key idea:
- rate limiting tracks usage by a policy key; in real distributed systems, this counter usually needs a shared, sharded, or gateway-managed implementation

---

## 15. Mini Program / Simulation

This mini program shows per-tenant rate limiting so one tenant does not consume the full API capacity.

```python
from dataclasses import dataclass, field


@dataclass
class TenantLimiter:
    max_requests: int
    counters: dict[str, int] = field(default_factory=dict)

    def allow(self, tenant_id: str) -> bool:
        used = self.counters.get(tenant_id, 0)
        if used >= self.max_requests:
            return False

        self.counters[tenant_id] = used + 1
        return True


def main() -> None:
    limiter = TenantLimiter(max_requests=3)
    requests = [
        "tenant-a",
        "tenant-a",
        "tenant-b",
        "tenant-a",
        "tenant-a",
        "tenant-b",
        "tenant-b",
        "tenant-b",
    ]

    for tenant_id in requests:
        if limiter.allow(tenant_id):
            print(f"{tenant_id}: allowed")
        else:
            print(f"{tenant_id}: 429 too many requests")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- limits should often be applied per tenant or caller
- one noisy tenant should not consume capacity for everyone
- rejecting early avoids expensive downstream work
- real systems need rolling windows, distributed counters, and observability

---

## 16. Practical Question

> You are designing public hotel search and booking APIs used by customers and partner agencies. How would you design rate limiting so the system is protected from abuse while still allowing legitimate traffic bursts?

---

## 17. Strong Answer

I would rate limit at multiple layers. At the edge, I would use coarse IP and geographic rules to block obvious abuse before it reaches the application. At the API gateway, I would enforce API-key, client, and tenant quotas, especially for partner agencies. Inside the application, I would add business-aware limits for sensitive or expensive flows like login, password reset, search, checkout, and refund attempts.

The rate-limit key matters. Before authentication, I may use IP, device fingerprint, and risk signals. After authentication, I would prefer user ID, tenant ID, API key, endpoint, and operation type. For partners, I would define contractual quotas and burst allowances. For expensive endpoints, I would use stricter endpoint-specific policies rather than one global limit.

When a caller exceeds a limit, I would return `429 Too Many Requests` with retry guidance where appropriate and avoid calling downstream services. I would monitor limit hits by endpoint, tenant, and policy so I can tune false positives. If the rate-limit store fails, I would choose fail-open or fail-closed based on endpoint risk. Login and payment abuse checks should fail safer than low-risk read APIs.

---

## 18. Revision Notes

- One-line summary: Rate limiting bounds how often a caller can perform actions so one user, tenant, client, or attacker cannot overload or abuse shared systems.
- Three keywords: quota, key, `429`
- One interview trap: using only a global or IP-based limit and calling the system protected
- One memory trick: count by caller, endpoint, and time window

---

# Topic 4: DDoS Protection

> Track: 1.10 Security
> Scope: volumetric attacks, application-layer floods, bot traffic, edge defense, origin protection, WAF rules, autoscaling limits, and incident response

---

## 1. Intuition

Think of a hotel phone line during a fake booking campaign.

- Thousands of fake callers flood the front desk.
- Real guests cannot get through.
- Hiring more receptionists helps only up to a point.
- The better defense is to filter fake calls before they reach the hotel.

That is the core idea of DDoS protection.

A distributed denial-of-service attack tries to make a system unavailable by overwhelming its network, edge, application, or downstream dependencies with traffic from many sources.

Short memory trick:
- absorb at the edge
- filter before origin
- protect expensive paths
- keep real users working

---

## 2. Definition

- Definition: DDoS protection is the set of architectural and operational defenses used to detect, absorb, filter, and mitigate distributed traffic floods that aim to degrade or take down a service.
- Category: Availability security and traffic-defense architecture
- Core idea: Push defense outward to the edge, distinguish legitimate from abusive traffic, and preserve origin capacity for real users.

Interview shortcut:
- DDoS is an availability attack
- defense is layered: DNS, CDN, WAF, rate limiting, load balancers, app controls, and runbooks
- do not let attack traffic reach expensive origin systems if it can be stopped earlier

---

## 3. Why It Exists

Internet-facing systems are exposed to traffic they did not invite.

Attackers may try to:
- saturate network bandwidth
- overwhelm load balancers
- exhaust application threads or connection pools
- trigger expensive database queries
- flood login, search, checkout, or file-upload endpoints
- hide malicious traffic inside normal-looking requests

Without DDoS protection:
- real users cannot reach the service
- autoscaling may increase cost without solving the attack
- downstream dependencies may fail from overload
- incident responders may be forced to block traffic manually under pressure
- recovery may take longer because attack and legitimate traffic are mixed together

DDoS protection exists because availability is part of security.

The goal is not just to keep servers alive. The goal is to keep useful service available to legitimate users while absorbing or filtering hostile traffic.

---

## 4. Reality

### DDoS protection is common in:

- public websites and APIs
- payment and booking platforms
- gaming and real-time systems
- SaaS login pages
- public search and catalog APIs
- media and file-serving platforms
- cloud-native systems behind global load balancers

### Common attack categories

- Volumetric attacks:
  flood bandwidth with huge traffic volume
- Protocol attacks:
  abuse TCP, UDP, SYN, or connection-state behavior
- Application-layer attacks:
  send expensive HTTP or API requests that look legitimate
- Bot and credential attacks:
  flood login, signup, password reset, or checkout paths

### Common defense layers

- DDoS-protected DNS and global traffic management
- CDN and edge caching
- cloud-provider DDoS protection
- WAF rules and bot detection
- rate limiting and request shaping
- origin shielding
- load shedding and graceful degradation
- application-level abuse detection

### Real-world architecture truth

You cannot autoscale your way out of every DDoS attack.

Why:
- bandwidth can be saturated before new servers help
- databases and third-party services may not scale with the web tier
- application-layer attacks may target expensive operations
- autoscaling can create a large cloud bill while the attacker keeps pushing

Another important truth:
- DDoS defense must be ready before the attack

During an attack, it is much harder to introduce new DNS, CDN, WAF, and routing controls safely.

---

## 5. How It Works

At a high level:

1. Traffic enters through protected DNS, CDN, or cloud edge.
2. Edge systems absorb large volumetric traffic and drop obvious bad packets.
3. CDN serves cacheable content without hitting origin.
4. WAF and bot systems inspect HTTP requests for known bad patterns.
5. Rate limits and request-shaping policies slow suspicious clients.
6. Load balancers route remaining traffic to healthy origins.
7. Application services protect expensive endpoints with authentication, quotas, caching, and load shedding.
8. Operators monitor attack signals and adjust rules during the incident.

### Edge-defense flow

- Use providers with large network capacity.
- Drop traffic that is malformed, clearly hostile, or from blocked sources.
- Challenge suspicious traffic before it reaches the origin.
- Cache static and semi-static content where possible.

Edge defense answers:
- how much bad traffic can we stop before our infrastructure pays for it?

### Origin-protection flow

- Keep origin IPs private where possible.
- Allow traffic only from trusted CDN or load balancer ranges.
- Use origin shielding so many edge nodes do not stampede the origin.
- Separate critical APIs from public static traffic when needed.

Origin protection answers:
- can attackers bypass the edge and hit the application directly?

### Application-defense flow

- Rate limit expensive and abuse-prone endpoints.
- Require authentication or proof-of-work style friction for sensitive paths when appropriate.
- Cache safe reads.
- Degrade non-critical features.
- Queue or shed low-priority work before core flows fail.

Application defense answers:
- how do we protect the most expensive business operations?

### Failure path

- If edge filters are too loose, origin gets overwhelmed.
- If filters are too strict, legitimate users are blocked.
- If origin IPs are exposed, attackers can bypass CDN defenses.
- If downstream dependencies are unprotected, they become the real bottleneck.

### Recovery path

- Tighten rules temporarily during the attack.
- Shift traffic through scrubbing centers or protected providers.
- Block or challenge high-risk traffic classes.
- Preserve core user journeys through degradation.
- After the incident, review logs, update rules, and close bypass paths.

---

## 6. What Problem It Solves

- Primary problem solved: preserves service availability during malicious or abusive traffic floods.
- Secondary benefits: protects origin capacity, reduces cloud-cost blast radius, improves bot resistance, and gives operators prepared incident controls.
- Systems impact: moves defense from individual application servers to layered traffic filtering across edge, network, gateway, and application layers.

This topic solves three practical problems:
- how do we keep the service reachable during hostile traffic?
- how do we prevent attack traffic from reaching expensive systems?
- how do we distinguish legitimate spikes from abusive floods quickly enough to act?

---

## 7. When to Rely on It

Use DDoS protection when:
- the service is internet-facing
- availability is business-critical
- login, search, checkout, or payment flows are public
- the system has expensive endpoints attackers can abuse
- downtime has revenue, trust, or compliance impact
- the platform may be targeted during launches, events, or public incidents

Especially valuable for:
- booking platforms
- payment systems
- public APIs
- media and content platforms
- gaming systems
- SaaS login and admin portals
- government or high-visibility websites

Strong interviewer keywords:
- CDN
- WAF
- bot detection
- origin shielding
- scrubbing
- traffic shaping
- Anycast
- load shedding
- volumetric attack
- application-layer DDoS

---

## 8. When Not to Use It

Do not treat every traffic problem as DDoS.

Be careful when:
- the traffic spike is legitimate demand that needs capacity planning
- a bad release creates excessive internal retries
- a slow database query makes normal traffic look like an attack
- aggressive WAF rules block real users or partners
- mitigation cost exceeds the risk for a private, low-exposure system

Avoid these patterns:
- relying only on autoscaling
- exposing origin IPs publicly
- adding WAF rules without monitoring false positives
- using one global block that harms entire countries or partner networks unnecessarily
- protecting the web tier while leaving search, database, or auth dependencies fragile

Better framing:
- classify the traffic problem first
- absorb and filter at the edge
- protect origin and expensive paths
- degrade non-critical features before core flows fail

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Layered DDoS protection | Preserves availability, blocks bad traffic early, protects origin capacity, and gives operators incident controls | Adds cost and configuration complexity, can create false positives, and requires ongoing tuning and testing |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Strict filtering vs false positives:
  aggressive rules stop more attacks but may block legitimate users.
- Edge caching vs freshness:
  caching absorbs traffic well, but some content needs fresh origin reads.
- Automation vs human judgment:
  automated mitigation reacts quickly, but poor rules can block important traffic.
- Availability vs feature completeness:
  graceful degradation keeps core flows alive but may temporarily disable non-critical features.
- Cost vs protection level:
  stronger DDoS protection and scrubbing capacity cost more, but reduce outage risk.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Relying only on autoscaling | Attack traffic can overwhelm bandwidth, dependencies, or budget | Use edge filtering, CDN, WAF, and origin protection |
| Exposing origin directly | Attackers can bypass CDN and hit the application | Restrict origin to trusted edge and load-balancer sources |
| No runbook | Engineers make risky manual changes during an incident | Prepare mitigation playbooks and escalation paths |
| Blocking too broadly | Real users and partners may be denied service | Use targeted rules, challenges, and observability |
| Ignoring application-layer attacks | HTTP floods can look like normal traffic while hitting expensive paths | Rate limit and protect high-cost endpoints specifically |
| Forgetting downstreams | The web tier survives but auth, database, or payment providers fail | Protect dependencies with quotas, caching, and load shedding |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Volumetric attack scale:
  large attacks can reach hundreds of Gbps or more, so provider-level protection matters
- HTTP flood scale:
  requests per second may be less dramatic but more expensive per request
- CDN cache hit ratio:
  higher cache hit ratio means fewer requests reach origin during traffic spikes
- WAF false-positive rate:
  should be monitored carefully, especially on login, booking, and partner APIs
- Mitigation time objective:
  serious platforms often aim to detect and mitigate within minutes
- Origin exposure:
  direct public access to origin should be avoided when CDN or edge protection is required
- Rate-limit response:
  application-layer floods often combine WAF rules with `429` and temporary challenges

Interview shorthand:
- absorb at edge, hide origin, filter bots, rate limit expensive APIs, degrade gracefully

---

## 12. Failure Modes

### Origin bypass

Problem:
- Attackers discover the origin IP and send traffic directly to it.

User impact:
- CDN and WAF protections are bypassed, and the application becomes unavailable

Mitigation:
- restrict origin access to trusted edge networks
- rotate exposed origins
- keep origin addresses private where possible
- monitor direct-to-origin traffic

### Application-layer flood

Problem:
- Attackers send normal-looking HTTP requests to expensive search or checkout endpoints.

User impact:
- databases, caches, or downstream services are overwhelmed even though network bandwidth looks fine

Mitigation:
- rate limit expensive endpoints
- cache safe reads
- require authentication or challenge suspicious clients
- use query cost controls and load shedding

### Overblocking real users

Problem:
- A mitigation rule blocks an entire ISP, country, or partner network.

User impact:
- legitimate users cannot access the platform during the incident

Mitigation:
- use targeted rules
- watch false-positive metrics
- maintain allowlists for critical partners where appropriate
- provide manual override processes

### Retry storm mistaken for DDoS

Problem:
- A service degradation causes clients to retry aggressively, creating internal traffic amplification.

User impact:
- the system experiences DDoS-like pressure from legitimate clients

Mitigation:
- use exponential backoff with jitter
- apply circuit breakers and rate limits
- distinguish internal retry storms from external malicious traffic in telemetry

---

## 13. Scenario

- Product / system: Public hotel booking platform during a major holiday sale
- Requirement:
  keep search, login, and booking flows available while traffic includes legitimate sale demand, bots scraping prices, and possible application-layer attack traffic
- Good design:
  serve static and cacheable content through a CDN, protect the origin behind trusted edge networks, use WAF and bot rules for suspicious requests, rate limit login and expensive search APIs, prioritize checkout traffic, and degrade non-critical features such as recommendations if capacity is strained
- Why this concept fits:
  the platform is public, revenue-critical, and exposed to both real traffic spikes and hostile traffic
- What would go wrong without it:
  bots or attackers could overwhelm search and login, causing real customers to fail before they can book

---

## 14. Code Sample

### Classifying traffic for layered mitigation

```java
public record RequestSignal(
        String ipReputation,
        boolean authenticated,
        int requestsPerMinute,
        boolean hitsExpensiveEndpoint,
        boolean knownPartner) {
}

public enum MitigationAction {
    ALLOW,
    RATE_LIMIT,
    CHALLENGE,
    BLOCK
}

public class DdosMitigationPolicy {

    public MitigationAction evaluate(RequestSignal signal) {
        if ("bad".equals(signal.ipReputation()) && !signal.knownPartner()) {
            return MitigationAction.BLOCK;
        }

        if (!signal.authenticated() && signal.requestsPerMinute() > 300) {
            return MitigationAction.CHALLENGE;
        }

        if (signal.hitsExpensiveEndpoint() && signal.requestsPerMinute() > 120) {
            return MitigationAction.RATE_LIMIT;
        }

        return MitigationAction.ALLOW;
    }
}
```

Key idea:
- DDoS mitigation is usually layered and risk-based, not a single yes-or-no firewall rule

---

## 15. Mini Program / Simulation

This mini program simulates edge filtering before traffic reaches the origin.

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class Request:
    source: str
    ip_reputation: str
    requests_per_minute: int
    path: str


def classify(request: Request) -> str:
    expensive_paths = {"/search", "/checkout", "/login"}

    if request.ip_reputation == "bad":
        return "block at edge"

    if request.requests_per_minute > 500:
        return "challenge at WAF"

    if request.path in expensive_paths and request.requests_per_minute > 100:
        return "rate limit before origin"

    return "allow to origin"


def main() -> None:
    requests = [
        Request("normal-user", "good", 12, "/search"),
        Request("scraper", "unknown", 250, "/search"),
        Request("botnet-node", "bad", 30, "/"),
        Request("flooder", "unknown", 900, "/login"),
    ]

    for request in requests:
        print(f"{request.source}: {classify(request)}")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- bad traffic should be handled before it reaches origin
- unknown traffic may be challenged or rate limited
- expensive paths need stricter controls
- not all high traffic is treated the same way

---

## 16. Practical Question

> You are designing a public hotel booking platform that expects a major sale event and may face bot traffic or DDoS attempts. How would you protect availability without blocking legitimate customers?

---

## 17. Strong Answer

I would design DDoS protection as a layered defense. First, I would put the platform behind protected DNS, CDN, and cloud DDoS services so large volumetric traffic is absorbed before it reaches my origin. I would keep origin access restricted to trusted edge and load-balancer sources so attackers cannot bypass the CDN.

At the HTTP layer, I would use WAF and bot-detection rules to block known bad traffic, challenge suspicious clients, and rate limit high-risk paths. I would protect login, search, checkout, and partner APIs differently because their cost and risk are different. Search may need query-cost controls and caching, while checkout should be prioritized and protected from nonessential traffic.

Inside the application, I would use rate limiting, load shedding, caching, and graceful degradation. During an attack or sale spike, I might temporarily disable recommendations or non-critical personalization while preserving search and booking. I would monitor cache hit ratio, origin traffic, WAF blocks, rate-limit hits, login success, booking success, and downstream saturation. The key is to filter early, protect the origin, and keep legitimate user journeys working.

---

## 18. Revision Notes

- One-line summary: DDoS protection preserves availability by absorbing and filtering hostile traffic at multiple layers before it overwhelms origin systems.
- Three keywords: edge, WAF, origin
- One interview trap: saying autoscaling alone solves DDoS
- One memory trick: stop the flood at the city gate, not at the hotel room door

---

# Topic 5: Encryption in Transit and at Rest

> Track: 1.10 Security
> Scope: TLS, mTLS, data-at-rest encryption, envelope encryption, KMS, key rotation, storage threats, and practical data-protection boundaries

---

## 1. Intuition

Think of hotel guest information moving through the business.

- When a guest tells the front desk their card details, the conversation should be private.
- When the hotel stores the reservation in a locked archive, the stored record should also be protected.

Those are two different moments.

- Encryption in transit protects data while it moves across networks.
- Encryption at rest protects data while it is stored on disk, database, object storage, backups, or snapshots.

Short memory trick:
- in transit = protect the road
- at rest = protect the vault

Both matter because attackers can target either the communication path or the stored data.

---

## 2. Definition

- Definition: Encryption in transit protects data as it moves between clients, services, networks, and dependencies, commonly using TLS.
- Definition: Encryption at rest protects stored data in databases, files, object storage, disks, backups, logs, and snapshots.
- Category: Data confidentiality and cryptographic protection
- Core idea: Encrypt sensitive data across network boundaries and storage layers, then manage keys separately and carefully.

Interview shortcut:
- TLS protects data over the wire
- storage encryption protects data on disk
- KMS protects and controls encryption keys
- encryption does not replace authorization, auditing, or data minimization

---

## 3. Why It Exists

Sensitive data moves and rests in many places.

Examples:
- browser to API gateway
- API gateway to backend service
- service to database
- service to third-party payment provider
- database to backup storage
- logs to centralized log storage
- object files to archival storage

Without encryption in transit:
- network observers may read sensitive data
- attackers may tamper with traffic
- users may connect to impostor services
- internal traffic can be exposed if the network is compromised

Without encryption at rest:
- stolen disks, snapshots, backups, or object-store exports may expose data
- accidental storage exposure becomes more damaging
- compliance requirements may be missed
- operators with low-level storage access may see data they should not see

Encryption exists to reduce the blast radius of data exposure.

Important maturity point:
- encryption is a control, not a complete security design

If an attacker has valid application credentials and authorization to read data, storage encryption alone will not stop the application from returning plaintext. That is why encryption must work with access control, secrets management, audit logging, and data minimization.

---

## 4. Reality

### Encryption in transit is common in:

- public HTTPS APIs
- mobile and web applications
- service-to-service communication
- database connections
- Kafka, queues, and event streams
- admin tools and internal dashboards
- third-party integrations

### Encryption at rest is common in:

- relational and NoSQL databases
- object storage
- block volumes and disks
- backups and snapshots
- logs and analytics stores
- data warehouses
- file uploads and document storage

### Common mechanisms

- TLS for client-to-server and service-to-service traffic
- mTLS when both client and server authenticate with certificates
- database or storage-engine encryption
- object-store server-side encryption
- application-level field encryption for especially sensitive fields
- envelope encryption using data keys and master keys
- cloud KMS or HSM-backed key management

### Real-world architecture truth

Encrypting the disk is not the same as protecting the field.

Database-level or disk-level encryption helps if storage media, backups, or snapshots leak. But once the application reads the data, it usually sees plaintext.

For highly sensitive values, teams may use field-level or application-level encryption so only specific services can decrypt the value.

Another important truth:
- keys are the real security boundary

If encryption keys are stored next to encrypted data with broad access, the design is weak. Strong systems separate encrypted data from key access and audit key usage.

---

## 5. How It Works

At a high level:

1. Classify data by sensitivity, such as public, internal, confidential, or regulated.
2. Use TLS for network communication across client, edge, service, and dependency boundaries.
3. Use storage encryption for databases, object storage, disks, backups, and snapshots.
4. Use application-level or field-level encryption for the most sensitive data when storage-level encryption is not enough.
5. Store and manage keys in a dedicated KMS or HSM-backed system.
6. Control which services and operators can use which keys.
7. Rotate keys and audit encryption/decryption usage.

### Encryption-in-transit flow

- Client connects to server using TLS.
- Server presents a certificate.
- Client validates the certificate chain and hostname.
- A secure session is negotiated.
- Data is encrypted and integrity-protected while crossing the network.

Transit encryption answers:
- can someone on the network read or tamper with this traffic?

### mTLS flow

- Server proves its identity to the client.
- Client also proves its identity to the server using a certificate.
- Both sides validate certificates before exchanging application data.

mTLS answers:
- do both services know who is on the other side of the connection?

### Encryption-at-rest flow

- Data is encrypted before or while being written to storage.
- The encryption key is managed separately from the stored ciphertext.
- When authorized services read data, the storage system or application decrypts it using approved key access.

At-rest encryption answers:
- if storage media, backups, or snapshots leak, is the data still protected?

### Envelope-encryption flow

1. Generate a data encryption key for a file, record, tenant, or dataset.
2. Encrypt the data using the data key.
3. Encrypt the data key using a master key stored in KMS.
4. Store encrypted data and encrypted data key together.
5. To decrypt, ask KMS to unwrap the data key, then decrypt the data.

Envelope encryption answers:
- how can we encrypt lots of data without exposing the master key everywhere?

### Failure path

- Expired TLS certificates can break clients or service calls.
- Missing KMS permissions can make data unreadable.
- Key deletion can permanently destroy access to encrypted data.
- Weak key separation can make encryption meaningless after a breach.

### Recovery path

- Monitor certificate expiry and rotate before deadlines.
- Use staged key rotation with old-key read support and new-key write behavior.
- Back up key metadata and key policies according to provider guidance.
- Test restore and decrypt paths, not only encrypt paths.

---

## 6. What Problem It Solves

- Primary problem solved: protects data confidentiality and integrity when data moves across networks or is stored in persistent systems.
- Secondary benefits: reduces breach blast radius, supports compliance, protects backups and snapshots, improves service identity, and enables stronger tenant/data isolation.
- Systems impact: makes data protection a cross-cutting design concern across clients, services, storage, backups, and key management.

This topic solves three practical problems:
- how do we protect data on the network?
- how do we protect data if storage is exposed?
- how do we manage keys so encryption remains meaningful?

---

## 7. When to Rely on It

Use encryption in transit when:
- data crosses public or private networks
- clients call APIs
- services call each other
- services connect to databases, queues, caches, or third parties
- traffic carries credentials, tokens, PII, payment, or business-sensitive data

Use encryption at rest when:
- databases store sensitive data
- object storage contains uploads or documents
- backups and snapshots exist
- logs or analytics contain sensitive identifiers
- compliance requires data protection
- cloud or shared infrastructure is used

Especially valuable for:
- payments and booking platforms
- healthcare and financial systems
- SaaS platforms with tenant data
- internal admin systems
- file upload systems
- data lakes and warehouses

Strong interviewer keywords:
- TLS
- mTLS
- certificate rotation
- KMS
- envelope encryption
- key rotation
- field-level encryption
- backups
- snapshots
- data classification

---

## 8. When Not to Use It

Do not use encryption as a magic answer to every data problem.

Be careful when:
- encryption adds complexity but data is already public
- application-level encryption prevents needed indexing or searching
- key access is so broad that encryption gives little real protection
- encrypted fields are logged in plaintext elsewhere
- backups are encrypted but operational exports are not

Avoid these patterns:
- storing encryption keys in the same database as encrypted data
- logging plaintext secrets or sensitive data after decryption
- using outdated TLS versions or weak cipher suites
- forgetting backups, snapshots, caches, and logs
- deleting old keys before old data has been re-encrypted or retired
- assuming encryption replaces authorization

Better framing:
- encrypt sensitive data in transit and at rest
- classify data first
- keep keys separate and access controlled
- minimize sensitive data storage where possible
- combine encryption with access control, auditing, and retention policies

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Encryption in transit and at rest | Protects data across network and storage exposure, reduces breach blast radius, and supports compliance | Adds key-management complexity, can affect search/indexing, and does not stop authorized misuse through the application |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Security vs operational complexity:
  stronger encryption and key separation improve protection, but require rotation, monitoring, and recovery planning.
- Field-level privacy vs queryability:
  encrypted fields are safer, but harder to search, index, aggregate, or debug.
- Short key rotation interval vs reliability:
  frequent rotation reduces long-term key exposure, but increases operational risk if poorly automated.
- mTLS identity vs certificate management:
  mTLS strengthens service identity, but certificates must be issued, rotated, and revoked correctly.
- Local performance vs KMS control:
  KMS provides centralized control and audit, but direct KMS calls on hot paths can add latency if not designed well.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Encrypting only the primary database | Backups, logs, exports, and snapshots may still expose data | Include every storage copy in the data-flow review |
| Storing keys beside ciphertext | Anyone who gets the data may also get the key | Use KMS or HSM-backed key management with separate access policies |
| Assuming TLS means authorization is solved | TLS protects the connection, not whether the caller may access data | Combine TLS with authentication and authorization |
| Logging decrypted sensitive data | Logs become a new plaintext data store | Redact, tokenize, or avoid sensitive logs |
| Breaking key rotation | Old data may become unreadable or old keys may remain trusted forever | Use staged rotation with key versions and tested rollback |
| Using application encryption without planning queries | Encrypted fields may not support normal search or uniqueness checks | Design tokenization, hashing, or separate searchable indexes carefully |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- TLS certificate lifetime:
  commonly months, with automated renewal strongly preferred
- Access-token and secret transport:
  should always use TLS, even on internal networks
- Key rotation interval:
  often months to a year depending on policy, risk, and compliance
- KMS latency:
  avoid per-record KMS calls on hot paths; use envelope encryption and cache unwrapped data keys carefully when appropriate
- Backup retention:
  encrypted backups may live for weeks, months, or years, so key availability must match retention requirements
- Decryption audit logs:
  sensitive key use should be auditable, especially for regulated or high-risk data
- Plaintext exposure window:
  minimize where and how long decrypted sensitive data exists in memory, logs, queues, and downstream systems

Interview shorthand:
- TLS for transit, encryption for storage, KMS for keys, rotation for lifecycle

---

## 12. Failure Modes

### Certificate expiry outage

Problem:
- TLS certificates expire for an API, gateway, or internal service.

User impact:
- clients or services fail to connect even though the application is otherwise healthy

Mitigation:
- automate certificate renewal
- alert well before expiry
- test certificate rotation in staging
- monitor TLS handshake errors

### Key deletion or permission mistake

Problem:
- A KMS key is deleted, disabled, or access is removed from a service that needs it.

User impact:
- encrypted data becomes unreadable and critical flows may fail

Mitigation:
- use deletion waiting periods and approvals
- separate disable from delete operations
- test restore paths
- monitor decrypt failures and key-policy changes

### Plaintext leak after decryption

Problem:
- The application decrypts sensitive data and writes it to logs, analytics events, or error reports.

User impact:
- encrypted storage is bypassed because plaintext appears in secondary systems

Mitigation:
- redact sensitive fields
- classify logs and events
- enforce safe logging libraries
- scan for sensitive data in observability pipelines

### Search/indexing conflict

Problem:
- A team encrypts a field that must be searched or deduplicated.

User impact:
- product functionality breaks or engineers add unsafe plaintext indexes

Mitigation:
- design search-safe patterns such as deterministic tokens, hashes for exact match, or separate protected search services
- avoid storing sensitive fields unless necessary

---

## 13. Scenario

- Product / system: Hotel booking platform storing guest profiles, reservations, invoices, payment references, and support notes
- Requirement:
  protect sensitive data in browser-to-API traffic, service-to-service calls, database storage, object uploads, backups, and logs
- Good design:
  require HTTPS at the edge, use TLS or mTLS for internal service calls where appropriate, enable database and object-store encryption at rest, use KMS-managed keys, apply field-level encryption or tokenization for especially sensitive values, and redact logs before they leave the application
- Why this concept fits:
  booking platforms handle PII, payment-adjacent data, partner integrations, and long-lived backups
- What would go wrong without it:
  network observers, exposed backups, leaked snapshots, or unsafe logs could reveal sensitive customer data

---

## 14. Code Sample

### Envelope encryption decision model

```java
public record DataRecord(String recordId, String tenantId, String encryptedPayload, String encryptedDataKey) {
}

public interface KeyManagementService {
    String generateEncryptedDataKey(String keyId);

    String decryptDataKey(String encryptedDataKey);
}

public class SensitiveDataReader {

    private final KeyManagementService keyManagementService;

    private final CryptoService cryptoService;

    public SensitiveDataReader(KeyManagementService keyManagementService, CryptoService cryptoService) {
        this.keyManagementService = keyManagementService;
        this.cryptoService = cryptoService;
    }

    public String readPlaintext(DataRecord record, String expectedTenantId) {
        if (!record.tenantId().equals(expectedTenantId)) {
            throw new SecurityException("tenant mismatch");
        }

        String dataKey = keyManagementService.decryptDataKey(record.encryptedDataKey());
        return cryptoService.decrypt(record.encryptedPayload(), dataKey);
    }
}

interface CryptoService {
    String decrypt(String ciphertext, String dataKey);
}
```

Key idea:
- encryption still needs authorization context; decrypting the data key should be controlled and audited

---

## 15. Mini Program / Simulation

This mini program simulates data classification and choosing the right protection level.

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class DataField:
    name: str
    classification: str
    needs_search: bool


def protection_plan(field: DataField) -> str:
    if field.classification == "public":
        return "TLS in transit; normal storage controls"

    if field.classification == "confidential" and field.needs_search:
        return "TLS, storage encryption, access control, and carefully designed searchable token"

    if field.classification == "regulated":
        return "TLS, storage encryption, field-level encryption, KMS key control, audit decrypts"

    return "TLS and encryption at rest"


def main() -> None:
    fields = [
        DataField("hotel_name", "public", True),
        DataField("guest_email", "confidential", True),
        DataField("payment_reference", "regulated", False),
        DataField("support_note", "confidential", False),
    ]

    for field in fields:
        print(f"{field.name}: {protection_plan(field)}")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- encryption choices should follow data classification
- searchable sensitive data needs special design
- regulated data usually needs stronger key control and auditing
- public data does not need the same protection as payment-adjacent data

---

## 16. Practical Question

> You are designing a hotel booking platform that stores guest PII, reservations, uploaded documents, and payment references. How would you design encryption in transit and at rest, and how would you handle keys safely?

---

## 17. Strong Answer

I would start with data classification. Public hotel content, guest PII, payment-adjacent references, support notes, logs, and backups do not all need the same controls. For data in transit, I would require HTTPS from clients to the edge and TLS for service-to-service and service-to-database traffic. For higher-trust internal calls, especially between critical services, I would consider mTLS so both sides authenticate each other.

For data at rest, I would enable database, object-store, disk, backup, and snapshot encryption. For highly sensitive fields, I would consider application-level field encryption or tokenization so storage admins or broad database readers cannot casually see the data. I would manage keys in KMS rather than storing them with the data. For large datasets, I would use envelope encryption so data is encrypted with data keys and those data keys are protected by KMS-managed master keys.

I would also plan key rotation, certificate renewal, audit logging for decrypt operations, and recovery testing. Encryption should not be the only control. The services still need authentication, authorization, redaction, retention policies, and careful logging so plaintext does not leak after decryption.

---

## 18. Revision Notes

- One-line summary: Encryption in transit protects data on the network, encryption at rest protects stored data, and key management determines whether that protection is meaningful.
- Three keywords: TLS, KMS, rotation
- One interview trap: saying data is safe because the disk is encrypted while logs, backups, keys, or app-level access remain exposed
- One memory trick: protect the road, protect the vault, protect the key

---

# Topic 6: Secrets Management

> Track: 1.10 Security
> Scope: API keys, database passwords, private keys, tokens, secret stores, injection, rotation, least privilege, CI/CD secrets, and leak response

---

## 1. Intuition

Think of hotel master keys.

- They should not be taped under the front desk.
- They should not be copied into every employee's notebook.
- Only the right staff should get the right key for the right time.
- If a key is lost, the hotel should be able to replace it quickly and know what it opened.

Secrets in software are the same kind of problem.

Secrets include values that prove trust or grant access:
- database passwords
- API keys
- OAuth client secrets
- private keys
- signing keys
- service tokens
- webhook secrets

Short memory trick:
- do not hardcode
- store centrally
- inject safely
- rotate and audit

---

## 2. Definition

- Definition: Secrets management is the discipline of securely storing, distributing, rotating, auditing, and revoking sensitive credentials used by applications, services, pipelines, and operators.
- Category: Credential lifecycle and operational security
- Core idea: Keep secrets out of source code and casual visibility, grant them only to authorized workloads, and rotate or revoke them when risk changes.

Interview shortcut:
- secrets should not live in code, images, logs, tickets, or chat
- services should receive secrets through controlled runtime mechanisms
- rotation and revocation must be designed before a leak happens

---

## 3. Why It Exists

Modern systems depend on credentials everywhere.

Examples:
- API service connects to a database
- payment service calls a payment provider
- notification service uses email or SMS provider keys
- CI/CD pipeline deploys infrastructure
- webhook receiver validates signatures
- service signs or verifies tokens

Without secrets management:
- secrets get committed to Git
- credentials are copied into config files, Docker images, or scripts
- too many people and services know the same powerful secret
- rotation becomes risky and manual
- leaks are hard to detect and contain
- old credentials keep working long after they should have expired

Secrets management exists because secrets have a lifecycle.

They are created, distributed, used, rotated, revoked, audited, and eventually deleted. A mature system treats every one of those steps intentionally.

---

## 4. Reality

### Secrets management is common in:

- microservices platforms
- Kubernetes workloads
- CI/CD pipelines
- cloud infrastructure
- payment and booking systems
- SaaS platforms
- data pipelines
- third-party integrations

### Common secret types

- database usernames and passwords
- API keys
- OAuth client secrets
- private TLS keys
- JWT signing keys
- encryption keys or wrapped data keys
- webhook signing secrets
- SSH deploy keys
- cloud access keys

### Common tools and mechanisms

- cloud secret managers such as AWS Secrets Manager, GCP Secret Manager, and Azure Key Vault
- HashiCorp Vault
- Kubernetes Secrets with external secret operators
- KMS-backed encryption
- workload identity and IAM roles
- short-lived credentials generated dynamically
- CI/CD secret stores

### Real-world architecture truth

The best secret is often no long-lived secret at all.

Where possible, prefer workload identity, IAM roles, service accounts, or dynamic credentials over static keys copied into applications.

Another important truth:
- storing secrets safely is only half the problem

Secrets also leak through logs, crash dumps, metrics labels, browser bundles, container images, shell history, support tickets, and CI output. Mature systems treat secret exposure as a data-flow problem, not only a storage problem.

---

## 5. How It Works

At a high level:

1. Identify all secrets used by services, humans, and pipelines.
2. Store secrets in a dedicated secret manager or identity system, not in source code.
3. Grant access using least privilege based on service identity, environment, and purpose.
4. Inject secrets at runtime through secure mechanisms such as environment variables, mounted files, sidecars, or SDK calls.
5. Rotate secrets on a schedule and immediately after suspected exposure.
6. Audit access to sensitive secrets.
7. Revoke unused or compromised credentials quickly.

### Storage flow

- Secret is created in a central secret manager.
- Secret is encrypted using KMS or provider-managed encryption.
- Access policies define which workloads or operators can read it.
- Metadata tracks owner, environment, rotation policy, and usage.

Storage answers:
- where does this secret live, and who can retrieve it?

### Runtime injection flow

- The workload proves its identity.
- The platform retrieves the secret or grants temporary credentials.
- The secret is made available only to the process that needs it.
- The application avoids printing, exposing, or persisting it elsewhere.

Runtime injection answers:
- how does the service get the credential without embedding it in code?

### Rotation flow

1. Create a new secret version.
2. Configure the dependency to accept both old and new credentials temporarily.
3. Update services to use the new version.
4. Verify traffic works with the new credential.
5. Revoke the old version.
6. Audit failures and retry lagging clients.

Rotation answers:
- can we change this credential without outage?

### Leak response flow

- Identify which secret leaked.
- Revoke or rotate it immediately.
- Search logs, Git history, artifacts, and images for copies.
- Check audit logs for suspicious use.
- Reduce future blast radius with narrower permissions or shorter lifetimes.

### Failure path

- Secret manager outage may prevent services from starting or refreshing credentials.
- Bad rotation can break production if dependencies do not accept the new secret.
- Over-broad credentials can turn a small leak into a large breach.
- Logging secrets can bypass the secret manager entirely.

### Recovery path

- Cache secrets carefully for already-running services where appropriate.
- Use staged rotation and rollback plans.
- Keep emergency break-glass access controlled and audited.
- Practice leak-response drills for high-risk credentials.

---

## 6. What Problem It Solves

- Primary problem solved: prevents sensitive credentials from being exposed, over-shared, or impossible to rotate safely.
- Secondary benefits: least privilege, auditability, faster incident response, safer CI/CD, reduced credential sprawl, and smaller breach blast radius.
- Systems impact: turns credentials from scattered static strings into managed, auditable, revocable security assets.

This topic solves three practical problems:
- where should secrets be stored?
- how should services receive them safely?
- how do we rotate or revoke them without breaking production?

---

## 7. When to Rely on It

Use secrets management when:
- applications connect to databases or third-party APIs
- services sign or verify tokens
- CI/CD pipelines deploy infrastructure
- private keys or certificates exist
- multiple environments need different credentials
- credentials must be rotated, audited, or revoked
- compliance requires controlled access to sensitive credentials

Especially valuable for:
- payment systems
- booking platforms
- enterprise SaaS
- Kubernetes workloads
- cloud-native microservices
- data pipelines
- partner integrations

Strong interviewer keywords:
- secret manager
- KMS
- Vault
- workload identity
- dynamic credentials
- rotation
- least privilege
- revocation
- audit logs
- break-glass access

---

## 8. When Not to Use It

Do not overcomplicate harmless configuration.

Be careful when:
- the value is public configuration, not a secret
- the team labels everything secret and makes operations unnecessarily hard
- environment variables are used without controlling process dumps, logs, and access
- a secret manager becomes a hard startup dependency without caching or fallback strategy
- secrets are injected into client-side code where users can inspect them

Avoid these patterns:
- committing secrets to Git
- baking secrets into container images
- sharing one database password across every service
- using production secrets in local development
- printing secrets in CI logs
- relying on manual rotation steps nobody practices

Better framing:
- separate secrets from normal config
- prefer short-lived or dynamic credentials where possible
- scope each secret narrowly
- rotate with automation and compatibility windows
- treat leaks as expected operational events, not surprises

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Centralized secrets management | Keeps secrets out of code, enables access control and audit, supports rotation, and reduces credential sprawl | Adds platform dependency, policy complexity, rotation coordination, and operational work |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Central control vs availability dependency:
  a secret manager improves security, but services need a plan if it is temporarily unavailable.
- Static secrets vs dynamic credentials:
  static secrets are simpler, but dynamic credentials reduce leak lifetime and blast radius.
- Fast rotation vs compatibility:
  immediate revocation is safer after a leak, but planned rotation often needs overlap to avoid downtime.
- Environment variables vs mounted files or SDK access:
  environment variables are easy, but can leak through process inspection or dumps in some setups.
- Least privilege vs operational convenience:
  narrow secrets are safer, but require better ownership and permission management.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Hardcoding secrets | Source code, Git history, and build artifacts preserve the secret | Store secrets in a secret manager and inject at runtime |
| Baking secrets into images | Every image copy contains the credential | Keep images generic and bind secrets at deployment time |
| Sharing one credential widely | One leak compromises many services | Create service-specific and environment-specific credentials |
| No rotation plan | Leaked or stale secrets remain valid too long | Automate rotation and test it regularly |
| Logging secret values | Logs become a secret store with broad access | Redact secrets and use safe logging utilities |
| Using production secrets in development | Local machines and test systems become production-risk surfaces | Use separate development credentials and synthetic data |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Secret rotation interval:
  often 30 to 180 days for static credentials, depending on risk and compliance
- Dynamic credential lifetime:
  often minutes to hours for high-security systems
- CI/CD secret exposure:
  should be masked in logs and scoped to the smallest pipeline or environment possible
- Secret access latency:
  avoid fetching secrets on every request; retrieve at startup or refresh on a controlled interval unless dynamic per-request credentials are required
- Incident response time:
  high-risk leaked credentials should be revocable within minutes
- Environment separation:
  production, staging, and development should use different secrets
- Audit retention:
  secret access and policy changes are often retained for months or years depending on compliance needs

Interview shorthand:
- store centrally, inject at runtime, scope narrowly, rotate automatically, audit access

---

## 12. Failure Modes

### Secret committed to Git

Problem:
- A developer accidentally commits an API key or private key.

User impact:
- anyone with repository or artifact access may use the credential, and Git history preserves it

Mitigation:
- rotate the secret immediately
- remove it from active history where appropriate
- scan repositories and CI artifacts
- add pre-commit and server-side secret scanning

### Rotation outage

Problem:
- A database password is rotated before all services are updated.

User impact:
- some services fail to connect to the database

Mitigation:
- use two-user or dual-secret rotation patterns
- support old and new credentials during a transition window
- monitor authentication failures during rotation

### Secret manager dependency outage

Problem:
- Services cannot retrieve secrets during startup because the secret manager is unavailable.

User impact:
- deployments or restarts fail even though already-running services may still have valid credentials

Mitigation:
- cache secrets for running services carefully
- avoid unnecessary restarts during secret-manager incidents
- make the secret manager highly available
- define startup behavior by service criticality

### Over-broad cloud key

Problem:
- A cloud access key has permissions across many resources and environments.

User impact:
- compromise can affect databases, storage, queues, and infrastructure far beyond one service

Mitigation:
- replace static keys with workload identity where possible
- scope permissions by service and environment
- enforce least privilege and regular access review

---

## 13. Scenario

- Product / system: Hotel booking platform with booking, pricing, payment, notification, and partner-integration services
- Requirement:
  services need database credentials, provider API keys, webhook secrets, token-signing keys, and CI/CD deployment credentials without exposing them in source code or logs
- Good design:
  store secrets in a managed secret store, grant access through workload identity, inject secrets at runtime, use service-specific credentials, rotate provider and database credentials with overlap windows, audit secret access, and scan repositories plus CI logs for leaks
- Why this concept fits:
  the platform has many services and third-party integrations where one leaked key can create serious data, money, or availability risk
- What would go wrong without it:
  credentials would spread through code, images, scripts, and logs, making leaks hard to detect and rotation dangerous

---

## 14. Code Sample

### Loading a secret through an abstraction instead of hardcoding it

```java
public interface SecretProvider {
    String getSecret(String secretName);
}

public class PaymentClientFactory {

    private final SecretProvider secretProvider;

    public PaymentClientFactory(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    public PaymentClient create() {
        String apiKey = secretProvider.getSecret("payment-provider-api-key");
        return new PaymentClient(apiKey);
    }
}

class PaymentClient {
    private final String apiKey;

    PaymentClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean authorize(String paymentReference) {
        return apiKey != null && !apiKey.isBlank() && paymentReference != null;
    }
}
```

Key idea:
- application code should depend on a secret-provider abstraction, while the deployment platform controls where the secret comes from and who can read it

---

## 15. Mini Program / Simulation

This mini program simulates a secret rotation where both old and new values are accepted during a transition window.

```python
from dataclasses import dataclass


@dataclass
class SecretVersion:
    value: str
    active: bool


class ProviderCredential:
    def __init__(self) -> None:
        self.versions = {
            "v1": SecretVersion("old-key", True),
            "v2": SecretVersion("new-key", False),
        }

    def enable_new_version(self) -> None:
        self.versions["v2"].active = True

    def disable_old_version(self) -> None:
        self.versions["v1"].active = False

    def accepts(self, value: str) -> bool:
        return any(version.active and version.value == value for version in self.versions.values())


def main() -> None:
    credential = ProviderCredential()

    print(f"old works before rotation: {credential.accepts('old-key')}")
    credential.enable_new_version()
    print(f"new works during overlap: {credential.accepts('new-key')}")
    print(f"old still works during overlap: {credential.accepts('old-key')}")
    credential.disable_old_version()
    print(f"old works after revocation: {credential.accepts('old-key')}")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- safe rotation often needs an overlap window
- new credentials should be verified before old ones are revoked
- old credentials must eventually be disabled
- rotation should be a normal operational path, not an emergency-only skill

---

## 16. Practical Question

> You are designing a hotel booking platform with many microservices and third-party integrations. How would you manage database passwords, payment-provider API keys, webhook secrets, and CI/CD credentials safely?

---

## 17. Strong Answer

I would keep secrets out of source code, container images, scripts, and logs. Secrets should live in a managed secret store such as a cloud secret manager or Vault, encrypted with KMS-backed controls. Services should access only the secrets they need, ideally through workload identity or service-specific IAM rather than shared static keys.

At runtime, secrets can be injected through controlled mechanisms such as mounted files, environment variables with restricted access, sidecars, or SDK calls. I would avoid fetching secrets on every request, but I would support controlled refresh so rotation does not require risky manual restarts. Production, staging, and development should use separate credentials.

For rotation, I would use compatibility windows where the dependency accepts old and new credentials temporarily, then verify the new version before revoking the old one. For a leaked secret, I would immediately revoke or rotate it, search Git history, logs, CI artifacts, and images for copies, and review audit logs for suspicious use. The main principle is to treat secrets as managed lifecycle assets: scoped, injected, rotated, revoked, and audited.

---

## 18. Revision Notes

- One-line summary: Secrets management keeps sensitive credentials out of code, delivers them safely to authorized workloads, and supports rotation, revocation, and audit.
- Three keywords: store, inject, rotate
- One interview trap: saying environment variables solve secrets management while ignoring storage, access control, logs, rotation, and leak response
- One memory trick: no keys in notebooks, give the right key at runtime, change it when lost