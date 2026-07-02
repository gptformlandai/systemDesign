# Spring Boot Browser, BFF, Session, And CSRF Security Gold Sheet

> Track: Spring Boot Interview Track - Senior Distributed Workloads  
> Goal: secure browser-facing Spring Boot applications, not only machine-to-machine APIs.

---

## 1. Intuition

API security and browser security are related, but not identical. A backend resource server
validates bearer tokens. A browser-facing app also has cookies, redirects, CSRF, SameSite,
CORS, OAuth2 login, session fixation, logout, and frontend token storage risks.

A BFF is a trusted backend that speaks browser-friendly cookies to the frontend and
service-friendly tokens to downstream APIs.

---

## 2. Definition

- Definition: Browser/BFF security is the Spring Security architecture for web clients
  using sessions, cookies, OAuth2 login, CSRF protection, token relay, and safe frontend
  boundaries.
- Category: application security, identity, web architecture.
- Core idea: do not treat a browser like a trusted server; protect cookies, redirects,
  cross-site requests, and token exposure.

---

## 3. Why It Exists

Pure JWT tutorials often miss browser threats:

- XSS stealing tokens from local storage
- CSRF abusing cookies
- OAuth2 redirect misconfiguration
- permissive CORS pretending to be authentication
- SameSite behavior breaking login or logout
- session fixation after login
- frontend calling every microservice directly
- tenant/user claims trusted without domain checks

---

## 4. Reality

Common patterns:

| App Type | Recommended Security Shape |
|---|---|
| Public REST API | OAuth2 resource server with bearer tokens |
| Browser MVC app | OAuth2 login or form login plus session and CSRF |
| SPA with BFF | HttpOnly SameSite cookie to BFF, BFF token relay downstream |
| Service-to-service | OAuth2 client credentials or mTLS plus authorization |
| Enterprise SSO | OIDC or SAML2 login |
| High-security internal app | session, CSRF, device posture, mTLS, audit |

---

## 5. How It Works

BFF flow:

1. Browser loads SPA from trusted origin.
2. User signs in through OIDC authorization code flow.
3. Spring Security stores authentication in a server-side session or secure token store.
4. Browser receives an HttpOnly, Secure, SameSite cookie.
5. Browser calls BFF endpoints with cookie.
6. CSRF token protects state-changing requests.
7. BFF calls downstream APIs with access token or service credential.
8. Downstream services still enforce scopes, tenant, and domain authorization.
9. Logs and audit events record user, tenant, action, and correlation ID.

Failure path:

- token stored in local storage -> XSS can steal it
- cookie auth without CSRF -> cross-site form can mutate state
- CORS wildcard with credentials -> dangerous exposure
- gateway-only auth -> internal service trusts spoofed headers

Recovery path:

- use HttpOnly cookies for browser session
- enable CSRF for cookie-authenticated writes
- restrict CORS to exact origins
- validate tokens and domain permissions in each sensitive service
- add audit and anomaly detection

---

## 6. What Problem It Solves

- Primary problem solved: secure browser interactions with Spring Boot backends.
- Secondary benefits: cleaner frontend architecture, safer token handling, centralized
  user-session behavior, better downstream token relay.
- Systems impact: reduces XSS/CSRF/token leakage risk and keeps microservices off the browser.

---

## 7. When To Rely On It

Use BFF/session security when:

- frontend is browser-based and talks to backend frequently
- you control both frontend and backend
- tokens should not be exposed to JavaScript
- multiple downstream APIs need token relay or aggregation
- the product has sensitive user actions such as booking, payments, profile changes

Interviewer triggers:

- "Where should a SPA store tokens?"
- "When does CSRF matter?"
- "Is CORS security?"
- "How would you secure a BFF?"
- "What is the difference between resource server and OAuth2 login?"

---

## 8. When Not To Use It

BFF/session security may be unnecessary when:

- the client is a trusted backend service, not a browser
- a public API must serve many independent clients
- a native mobile app uses authorization code with PKCE and platform secure storage
- the architecture requires stateless resource servers only

Use resource server JWT validation for machine/API boundaries. Use OAuth2 login/session
for browser app boundaries.

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Keeps tokens out of JavaScript | BFF must manage sessions/token relay |
| Centralizes browser security | More server-side state |
| Reduces frontend complexity | Requires CSRF design |
| Hides microservices from browser | BFF can become aggregation bottleneck |
| Strong audit point | Needs careful scaling and session storage |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- Gain: safer browser token handling and centralized session control.
- Give up: pure statelessness at the browser boundary.
- Latency: BFF may add one hop, but can reduce frontend fan-out.
- Cost: session store and BFF scaling.
- Complexity: cookie, CSRF, SameSite, CORS, and token relay must align.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Store access token in local storage | XSS can read it | Prefer HttpOnly cookie/BFF |
| Disable CSRF because "we use JWT" | Cookie auth can still be attacked | Enable CSRF for cookie writes |
| Use CORS as authorization | CORS is browser policy, not auth | Validate identity and permissions |
| Trust gateway headers blindly | Headers can be spoofed inside network | Validate token or signed headers |
| Put refresh tokens in browser JS | Long-lived credential exposure | Server-side token storage |
| Skip domain authorization | Scope is not ownership | Check tenant/user/resource relation |

---

## 11. Key Numbers

Useful reasoning points:

- `HttpOnly` prevents JavaScript from reading cookies.
- `Secure` requires HTTPS for cookie transmission.
- `SameSite=Lax` is a safer default for many apps; `None` requires `Secure`.
- CSRF matters when browser automatically sends credentials such as cookies.
- Access tokens are usually short-lived; refresh tokens need stronger protection.
- Session TTL should match product risk and user experience.

---

## 12. Failure Modes

| Failure | User Observes | Root Cause | Mitigation |
|---|---|---|---|
| Random 403 on POST | Writes fail | Missing/invalid CSRF token | Add CSRF token flow |
| Login redirect loop | Cannot sign in | Wrong redirect URI/proxy headers | Configure forwarded headers |
| Token leak | Account takeover risk | Local storage plus XSS | BFF and HttpOnly cookies |
| Tenant data leak | User sees wrong data | Missing domain check | Method/domain authorization |
| Logout incomplete | Session remains active | IdP and app logout not coordinated | Clear session and IdP flow |
| CORS blocked | Browser error | Origin not allowed | Exact CORS policy |

---

## 13. Scenario

- Product/system: hotel admin portal and customer booking SPA.
- Why this concept fits: browser users perform sensitive booking and payment actions.
- What would go wrong without it: access tokens may leak to XSS, or cookie-based writes
  may be exposed to CSRF.

---

## 14. Code Sample

Sketch for a BFF security filter chain:

```java
package com.example.booking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
class BrowserSecurityConfig {

    @Bean
    SecurityFilterChain browserSecurity(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("SCOPE_admin")
                .anyRequest().authenticated())
            .oauth2Login(oauth -> { })
            .oauth2Client(oauth -> { })
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .build();
    }
}
```

Note: `withHttpOnlyFalse()` is often used so frontend code can read the CSRF token cookie
and send it in a header. The session cookie itself should remain HttpOnly.

---

## 15. Mini Program / Simulation

```python
def csrf_required(auth_style, method):
    state_changing = method in {"POST", "PUT", "PATCH", "DELETE"}
    browser_sends_credentials = auth_style in {"session-cookie", "bff-cookie"}
    return state_changing and browser_sends_credentials


for auth, method in [("bearer-token", "POST"), ("session-cookie", "POST"), ("session-cookie", "GET")]:
    print(auth, method, "csrf?", csrf_required(auth, method))
```

---

## 16. Practical Question

> You are building a Spring Boot BFF for a React booking app. Where do tokens live, how do
> you protect writes, and how do downstream APIs know the user is allowed to act?

---

## 17. Strong Answer

I would avoid storing access or refresh tokens in browser local storage. The browser would
authenticate through OIDC authorization code flow, and the BFF would maintain the session
using Secure, HttpOnly, SameSite cookies. For state-changing requests, I would enable CSRF
and have the frontend send the CSRF token header. The BFF would relay an access token or
use a service credential to downstream APIs, but those APIs would still validate identity,
scopes, tenant, and domain ownership. CORS would be restricted to the exact frontend origins,
and audit logs would include user, tenant, action, resource, and correlation ID.

---

## 18. Revision Notes

- One-line summary: browser security needs cookie, CSRF, CORS, redirect, and token-storage
  thinking in addition to JWT validation.
- Three keywords: BFF, CSRF, HttpOnly.
- One interview trap: CORS is not authentication or authorization.
- One memory trick: browser sends cookies automatically, so writes need CSRF protection.

