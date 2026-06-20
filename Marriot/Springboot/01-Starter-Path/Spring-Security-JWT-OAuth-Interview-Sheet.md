# Spring Security JWT OAuth Interview Sheet

> Goal: master Spring Security from interview and production angles: filter chain, authentication, authorization, JWT, OAuth2, CSRF, CORS, stateless APIs, method security, and common traps.

This sheet is optimized for Java/Spring Boot backend interviews, especially roles that expect REST APIs, microservices, security basics, production debugging, and OAuth/JWT awareness.

---

# 0. Interview Command Center

## Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Authentication vs authorization | Very high | First-level security question |
| Spring Security filter chain | Very high | Core internal mechanism |
| `SecurityFilterChain` config | Very high | Modern Spring Security style |
| `SecurityContextHolder` | Very high | Current user storage |
| `Authentication` and authorities | Very high | Used everywhere |
| `UserDetailsService` | High | Username/password flow |
| `PasswordEncoder` / BCrypt | Very high | Password storage |
| JWT structure and validation | Very high | Most repeated token topic |
| OAuth2 roles and flows | Very high | JWT/OAuth clarity |
| Resource Server | Very high | Microservice API security |
| Stateless session config | Very high | REST API security |
| CSRF | Very high | Tricky interview topic |
| CORS | High | Common frontend/backend issue |
| 401 vs 403 | Very high | Interview and production debugging |
| Role vs authority | Very high | Common confusion |
| Method security | High | `@PreAuthorize` |
| Refresh token | High | Real auth flows |
| Token revocation | Medium-high | JWT limitation |
| Custom JWT filter | Medium-high | Common project implementation |
| OAuth2 client credentials | Medium-high | Service-to-service |
| Security testing | Medium-high | Practical quality |

---

## One-Line Definition

Spring Security is a framework that protects Java applications by applying authentication, authorization, and security protections through a configurable servlet filter chain.

Strong answer:

> Spring Security secures requests through filters. The filters authenticate the user, store the result in `SecurityContextHolder`, and then authorization rules decide whether the request can access a URL or method.

---

## Modern Spring Security Style

Older projects used:

```java
extends WebSecurityConfigurerAdapter
```

Modern Spring Security uses bean-based configuration:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/public/**").permitAll()
            .anyRequest().authenticated()
        )
        .build();
}
```

Interview note:

> In modern Spring Security, we expose a `SecurityFilterChain` bean instead of extending `WebSecurityConfigurerAdapter`.

---

# 1. Authentication vs Authorization

## Authentication

Authentication answers:

```text
Who are you?
```

Examples:

- username/password login
- JWT validation
- OAuth2 login
- API key validation
- mTLS certificate validation

## Authorization

Authorization answers:

```text
What are you allowed to do?
```

Examples:

- only admin can delete booking
- only customer can view own booking
- only service account can call internal endpoint
- user must have `booking:read` scope

## Interview Comparison

| Area | Authentication | Authorization |
|---|---|---|
| Question | Who are you? | What can you access? |
| Input | Credentials/token | Roles/authorities/scopes |
| Failure | 401 Unauthorized | 403 Forbidden |
| Example | Valid JWT? | Has `ROLE_ADMIN`? |

## Strong Answer

> Authentication verifies identity. Authorization checks permissions. In Spring Security, authentication creates an `Authentication` object, and authorization uses its authorities to decide access.

---

# 2. 401 vs 403

## 401 Unauthorized

Means:

```text
User is not authenticated.
```

Examples:

- missing token
- expired token
- invalid token signature
- not logged in

## 403 Forbidden

Means:

```text
User is authenticated but not allowed.
```

Examples:

- logged-in customer calls admin API
- valid JWT but missing scope
- user tries to access someone else's booking

## Strong Answer

> 401 means authentication failed or is missing. 403 means authentication succeeded, but authorization failed.

---

# 3. Spring Security Big Picture

## Request Flow

```text
Client Request
   |
Servlet Container
   |
DelegatingFilterProxy
   |
FilterChainProxy
   |
SecurityFilterChain
   |
Security Filters
   |
DispatcherServlet
   |
Controller
```

## What Happens

1. Request enters servlet filter chain.
2. Spring Security filters inspect the request.
3. Authentication filter extracts credentials/token.
4. Authentication is validated.
5. `SecurityContextHolder` stores authenticated user.
6. Authorization filter checks URL/method rules.
7. If allowed, request reaches controller.
8. Security context is cleared after request.

## Strong Answer

> Spring Security works mainly through servlet filters. These filters run before controllers, authenticate the request, store the authentication in the security context, and enforce authorization rules.

---

# 4. DelegatingFilterProxy

## Definition

`DelegatingFilterProxy` is a servlet filter registered with the servlet container. It delegates work to a Spring-managed bean.

## Why It Exists

Servlet container manages filters.

Spring manages beans.

`DelegatingFilterProxy` connects both worlds.

## Mental Model

```text
Servlet container knows DelegatingFilterProxy
DelegatingFilterProxy knows Spring bean
Spring bean is FilterChainProxy
```

## Strong Answer

> `DelegatingFilterProxy` bridges the servlet container and Spring application context. It lets a servlet filter delegate to Spring-managed security beans.

---

# 5. FilterChainProxy and SecurityFilterChain

## FilterChainProxy

`FilterChainProxy` is the main Spring Security filter that delegates to one or more `SecurityFilterChain`s.

## SecurityFilterChain

`SecurityFilterChain` defines:

- which requests it applies to
- which security filters run
- what rules are enforced

## Multiple Chains Example

```java
@Bean
@Order(1)
SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/api/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt())
        .build();
}

@Bean
@Order(2)
SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/public/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
}
```

## Interview Trap

Mistake:

> Spring Security is only annotations.

Better:

> Spring Security is primarily filter-chain based for web requests, and annotation/proxy based for method security.

---

# 6. Important Security Filters

## Common Filters To Recognize

| Filter | Purpose |
|---|---|
| `SecurityContextHolderFilter` | Loads/saves security context |
| `UsernamePasswordAuthenticationFilter` | Handles form login username/password |
| `BasicAuthenticationFilter` | Handles HTTP Basic auth |
| `BearerTokenAuthenticationFilter` | Handles bearer tokens for resource server |
| `AnonymousAuthenticationFilter` | Provides anonymous authentication |
| `ExceptionTranslationFilter` | Converts security exceptions to 401/403 |
| `AuthorizationFilter` | Applies authorization rules |
| `CsrfFilter` | CSRF protection |
| `CorsFilter` | CORS handling |

## Interview Answer

> Spring Security has multiple filters. For JWT resource server, the important one is bearer token authentication. For form login, username/password filter is used. Authorization happens later after authentication is available.

---

# 7. SecurityContextHolder

## Definition

`SecurityContextHolder` stores the `SecurityContext`, which contains the current `Authentication`.

## What It Holds

```text
SecurityContextHolder
   -> SecurityContext
      -> Authentication
         -> principal
         -> credentials
         -> authorities
```

## Example

```java
Authentication authentication =
    SecurityContextHolder.getContext().getAuthentication();

String username = authentication.getName();
Collection<? extends GrantedAuthority> authorities =
    authentication.getAuthorities();
```

## ThreadLocal Behavior

For servlet applications, `SecurityContextHolder` commonly uses `ThreadLocal`.

This means:

- current request thread can access authentication
- security context should be cleared after request
- async/thread-pool work needs special care

## Strong Answer

> `SecurityContextHolder` stores the currently authenticated user for the request. In servlet apps it usually uses `ThreadLocal`, so the security context is available within the request thread.

---

# 8. Authentication Object

## Definition

`Authentication` represents either:

- credentials before authentication
- authenticated principal after authentication

## Important Methods

```java
authentication.getPrincipal();
authentication.getCredentials();
authentication.getAuthorities();
authentication.isAuthenticated();
authentication.getName();
```

## Example Types

| Type | Use |
|---|---|
| `UsernamePasswordAuthenticationToken` | Username/password auth |
| `JwtAuthenticationToken` | JWT resource server auth |
| `BearerTokenAuthenticationToken` | Raw bearer token before validation |
| `AnonymousAuthenticationToken` | Anonymous user |

## Strong Answer

> `Authentication` is the central object representing the current user and authorities. After successful authentication, Spring stores it in the `SecurityContext`.

---

# 9. AuthenticationManager and AuthenticationProvider

## AuthenticationManager

`AuthenticationManager` defines authentication.

Most common implementation:

```text
ProviderManager
```

## AuthenticationProvider

`AuthenticationProvider` performs a specific authentication type.

Examples:

- username/password provider
- JWT provider
- LDAP provider
- custom API key provider

## Flow

```text
Filter extracts credentials
   |
AuthenticationManager.authenticate(authentication)
   |
ProviderManager delegates to AuthenticationProvider
   |
Provider validates credentials
   |
Authenticated Authentication returned
   |
Stored in SecurityContextHolder
```

## Strong Answer

> `AuthenticationManager` is the authentication entry point. Its common implementation, `ProviderManager`, delegates to one or more `AuthenticationProvider`s, each supporting a specific authentication mechanism.

---

# 10. UserDetails and UserDetailsService

## UserDetails

Represents user information needed by Spring Security.

Important methods:

```java
String getUsername();
String getPassword();
Collection<? extends GrantedAuthority> getAuthorities();
boolean isAccountNonExpired();
boolean isAccountNonLocked();
boolean isCredentialsNonExpired();
boolean isEnabled();
```

## UserDetailsService

Loads user by username.

```java
UserDetails loadUserByUsername(String username)
    throws UsernameNotFoundException;
```

## Example

```java
@Service
class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPasswordHash())
            .authorities(user.getAuthorities())
            .build();
    }
}
```

## Strong Answer

> `UserDetailsService` loads user information from a database or other source. It does not validate the password by itself; authentication provider uses it along with `PasswordEncoder`.

---

# 11. PasswordEncoder and BCrypt

## Why PasswordEncoder Exists

Passwords should not be stored as plain text.

They should be stored using a slow one-way password hashing algorithm.

## Common Encoders

| Encoder | Use |
|---|---|
| `BCryptPasswordEncoder` | Common production choice |
| `Argon2PasswordEncoder` | Strong memory-hard option |
| `Pbkdf2PasswordEncoder` | Standards-friendly option |
| `SCryptPasswordEncoder` | Memory-hard option |
| `NoOpPasswordEncoder` | Never use in production |

## BCrypt Example

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

## DelegatingPasswordEncoder

Modern Spring Security supports storing password hash with an algorithm prefix:

```text
{bcrypt}$2a$10$...
{noop}password
{pbkdf2}...
```

Why useful:

- supports multiple encoders
- allows gradual migration
- can upgrade algorithms later

## Strong Answer

> Passwords should be stored using a one-way adaptive hash like BCrypt, not encryption or plain text. `PasswordEncoder` verifies a raw password against the stored hash.

## Interview Trap

Mistake:

> We decrypt password and compare.

Better:

> Passwords should not be decrypted. We hash the input password and compare using `PasswordEncoder.matches`.

---

# 12. Roles vs Authorities

## Authority

An authority is a granted permission.

Examples:

```text
booking:read
booking:write
SCOPE_booking.read
ROLE_ADMIN
```

## Role

Role is a special authority conventionally prefixed with:

```text
ROLE_
```

Example:

```text
ROLE_ADMIN
ROLE_USER
```

## `hasRole` vs `hasAuthority`

```java
.requestMatchers("/admin/**").hasRole("ADMIN")
```

This checks:

```text
ROLE_ADMIN
```

```java
.requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
```

This checks exact authority:

```text
ROLE_ADMIN
```

## Strong Answer

> Roles are usually authorities with `ROLE_` prefix. `hasRole("ADMIN")` checks for `ROLE_ADMIN`, while `hasAuthority` checks the exact string.

---

# 13. Basic Security Configuration

## Public + Protected APIs

```java
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

## Rule Order Matters

Specific rules should come before broad rules.

Good:

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/**").authenticated()
```

Bad:

```java
.requestMatchers("/api/**").authenticated()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

## Strong Answer

> Authorization rules are matched in order. I keep specific rules first and fallback rules later.

---

# 14. Stateless REST API Security

## Statefulness

Traditional web login:

```text
User logs in
Server creates session
Browser stores session cookie
Server uses session to remember user
```

Stateless JWT API:

```text
Client sends Authorization: Bearer <token>
Server validates token on every request
No server session required
```

## Stateless Config

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .build();
}
```

## Strong Answer

> For stateless REST APIs, the server does not keep login state in HTTP session. Each request carries a token, and the server validates it every time.

---

# 15. JWT Basics

## Definition

JWT means JSON Web Token.

It is a compact token format commonly used to carry identity and authorization claims.

## Structure

```text
header.payload.signature
```

## Header

Contains metadata:

```json
{
  "alg": "RS256",
  "typ": "JWT"
}
```

## Payload

Contains claims:

```json
{
  "sub": "user123",
  "iss": "https://auth.example.com",
  "aud": "booking-api",
  "exp": 1760000000,
  "iat": 1759990000,
  "scope": "booking:read booking:write"
}
```

## Signature

Proves the token was issued by a trusted party and not modified.

## Strong Answer

> JWT has header, payload, and signature. The payload is readable but not trusted unless the signature and claims are validated.

---

# 16. JWT Is Not Encryption

## Key Point

Normal JWT is signed, not encrypted.

Anyone who has the token can Base64 decode header and payload.

Do not store sensitive secrets in JWT payload.

Bad payload:

```json
{
  "password": "secret",
  "creditCard": "4111111111111111"
}
```

Better payload:

```json
{
  "sub": "user123",
  "scope": "booking:read",
  "exp": 1760000000
}
```

## Strong Answer

> JWT payload is not hidden. It is Base64URL encoded and signed. Signature protects integrity, not confidentiality.

---

# 17. JWT Validation

## What Must Be Validated

| Check | Why |
|---|---|
| Signature | Token was not tampered |
| Expiration `exp` | Token is not expired |
| Not before `nbf` | Token is valid now |
| Issuer `iss` | Trusted authorization server |
| Audience `aud` | Token intended for this API |
| Algorithm | Avoid algorithm confusion |
| Scopes/roles | Authorization |

## Strong Answer

> JWT validation is not just decoding. We must verify signature, expiration, issuer, audience, and required scopes/authorities.

---

# 18. JWT Flow In REST API

## Login Flow

```text
1. Client sends username/password to auth endpoint
2. Server validates credentials
3. Server creates access token
4. Server optionally creates refresh token
5. Client stores token securely
6. Client sends Authorization: Bearer token on API calls
```

## API Request Flow

```text
1. Request enters Spring Security filter chain
2. JWT filter/resource server extracts bearer token
3. Token signature and claims are validated
4. Authentication object is created
5. Authentication is stored in SecurityContextHolder
6. Authorization rules check authorities/scopes
7. Controller executes if allowed
```

## Strong Answer

> With JWT, authentication happens on every request by validating the bearer token. If valid, Spring creates an `Authentication` and authorization checks roles or scopes.

---

# 19. OAuth2 Basics

## What OAuth2 Is

OAuth2 is an authorization framework.

It allows a client application to access protected resources with tokens.

## OAuth2 Roles

| Role | Meaning |
|---|---|
| Resource Owner | User |
| Client | Application requesting access |
| Authorization Server | Issues tokens |
| Resource Server | API that validates token |

## Example

```text
User logs in through authorization server
Client gets access token
Client calls Booking API
Booking API validates token
```

## Strong Answer

> OAuth2 is about delegated authorization using tokens. In a microservice API, our Spring Boot service usually acts as a resource server that validates access tokens.

---

# 20. OAuth2 vs JWT

## Difference

| OAuth2 | JWT |
|---|---|
| Authorization framework | Token format |
| Defines flows and roles | Defines token structure |
| Can use JWT or opaque tokens | Can be used outside OAuth2 |
| Example: auth code flow | Example: signed access token |

## Strong Answer

> OAuth2 is a framework for issuing and using access tokens. JWT is one possible token format. OAuth2 can use JWT tokens or opaque tokens.

---

# 21. OAuth2 Grant Types

## Authorization Code + PKCE

Best for:

- web apps
- mobile apps
- single-page apps

Flow:

```text
User redirected to authorization server
User authenticates
Client receives authorization code
Client exchanges code for tokens
PKCE protects public clients
```

## Client Credentials

Best for:

- service-to-service calls
- machine-to-machine auth

Flow:

```text
Service authenticates with client_id/client_secret
Authorization server returns access token
Service calls another API
```

## Refresh Token

Used to obtain a new access token without making user log in again.

## Password Grant

Avoid for modern systems.

It requires client to handle user password directly and is generally not recommended.

## Strong Answer

> For user login, authorization code with PKCE is preferred. For service-to-service communication, client credentials is common. I avoid password grant in modern systems.

---

# 22. Access Token vs Refresh Token

## Access Token

Used to call APIs.

Properties:

- short-lived
- sent to resource server
- contains or represents permissions

## Refresh Token

Used to get new access token.

Properties:

- longer-lived
- should be stored more securely
- sent only to authorization server
- can usually be revoked/rotated

## Strong Answer

> Access token is used to access APIs and should be short-lived. Refresh token is used to obtain new access tokens and must be protected carefully.

---

# 23. Spring OAuth2 Resource Server

## What It Means

A resource server is an API that validates access tokens.

In Spring Boot, a backend service can be configured as OAuth2 Resource Server.

## Maven Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

## JWT Resource Server Config

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://idp.example.com/issuer
```

## SecurityFilterChain

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt())
        .build();
}
```

## What Spring Validates

Spring Resource Server validates:

- token signature using JWK/public key
- expiration
- issuer
- not-before
- authorities/scopes mapping

## Strong Answer

> In microservices, Spring Boot APIs often act as OAuth2 resource servers. They validate bearer JWTs using issuer or JWK configuration and then map claims to authorities for authorization.

---

# 24. `issuer-uri` vs `jwk-set-uri`

## `issuer-uri`

Spring uses issuer metadata discovery.

It can discover:

- JWK Set URI
- supported algorithms
- issuer validation

Example:

```yaml
spring.security.oauth2.resourceserver.jwt.issuer-uri: https://idp.example.com/issuer
```

## `jwk-set-uri`

Direct URL to public keys.

Example:

```yaml
spring.security.oauth2.resourceserver.jwt.jwk-set-uri: https://idp.example.com/.well-known/jwks.json
```

## Strong Answer

> `issuer-uri` lets Spring discover metadata and validate issuer. `jwk-set-uri` points directly to keys used to verify JWT signatures. Many systems use issuer URI when the identity provider supports discovery.

---

# 25. Scope To Authority Mapping

## Default Mapping

OAuth2 Resource Server commonly maps scopes to authorities with prefix:

```text
SCOPE_
```

JWT claim:

```json
{
  "scope": "booking.read booking.write"
}
```

Authorities:

```text
SCOPE_booking.read
SCOPE_booking.write
```

## Authorization Example

```java
.requestMatchers(HttpMethod.GET, "/api/bookings/**")
    .hasAuthority("SCOPE_booking.read")
.requestMatchers(HttpMethod.POST, "/api/bookings/**")
    .hasAuthority("SCOPE_booking.write")
```

## Method Security Example

```java
@PreAuthorize("hasAuthority('SCOPE_booking.read')")
public BookingResponse getBooking(String id) {
    return bookingService.getBooking(id);
}
```

## Strong Answer

> In Spring Resource Server, OAuth scopes are often mapped to authorities with `SCOPE_` prefix. So scope `booking.read` becomes `SCOPE_booking.read`.

---

# 26. Custom JWT Claims To Authorities

## Problem

Some identity providers store roles differently.

Example:

```json
{
  "roles": ["ADMIN", "SUPPORT"]
}
```

But Spring expects authorities.

## Solution

Use a JWT authentication converter.

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        )
        .build();
}

Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    scopes.setAuthorityPrefix("SCOPE_");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        List<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
        }

        return authorities;
    });

    return converter;
}
```

## Strong Answer

> If roles or permissions are in custom JWT claims, I configure a `JwtAuthenticationConverter` to map those claims into Spring authorities.

---

# 27. Custom JWT Filter

## When You See This

Many projects implement their own JWT login and validation.

Common custom filter:

```text
OncePerRequestFilter
```

## Typical Flow

```text
1. Read Authorization header
2. Check Bearer prefix
3. Extract token
4. Validate signature and claims
5. Load user if needed
6. Create Authentication
7. Store in SecurityContextHolder
8. Continue filter chain
```

## Example Skeleton

```java
class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    JwtAuthenticationFilter(JwtService jwtService,
                            UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        String username = jwtService.extractUsername(token);

        if (username != null &&
            SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails user = userDetailsService.loadUserByUsername(username);

            if (jwtService.isValid(token, user)) {
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                    );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

## Registering Filter

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http,
                                        JwtAuthenticationFilter jwtFilter)
        throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

## Interview Caution

For enterprise OAuth2 systems, prefer Resource Server support instead of hand-rolling token validation.

## Strong Answer

> A custom JWT filter extracts bearer token, validates it, creates an `Authentication`, and sets it in `SecurityContextHolder`. But if we use a standard OAuth2 provider, Spring Resource Server is usually cleaner and safer.

---

# 28. OAuth2 Login vs Resource Server

## OAuth2 Login

Used when the app logs in users through an identity provider.

Example:

```text
Spring MVC web app login with Google/Okta/Azure AD
```

## Resource Server

Used when the app exposes APIs protected by access tokens.

Example:

```text
Booking API validates bearer JWT from Authorization header
```

## Strong Answer

> OAuth2 Login is for logging users into an application. OAuth2 Resource Server is for protecting APIs that receive access tokens.

---

# 29. JWT vs Opaque Token

## JWT

Self-contained token.

Pros:

- no introspection call needed every time
- good for distributed APIs
- carries claims

Cons:

- harder immediate revocation
- token can grow large
- claims can become stale

## Opaque Token

Random-looking token. Resource server introspects it with authorization server.

Pros:

- easier revocation
- less data exposed to client
- central control

Cons:

- network call for introspection
- auth server dependency
- latency

## Strong Answer

> JWT is self-contained and good for scalable APIs, but revocation is harder. Opaque tokens require introspection, which gives central control but adds network dependency.

---

# 30. Token Expiry and Revocation

## JWT Expiry

JWTs should be short-lived.

Example:

```text
Access token: 5-15 minutes
Refresh token: hours/days depending on risk
```

## Revocation Problem

JWT is valid until expiration if signature and claims are valid.

Immediate revocation is hard unless you add:

- blacklist/denylist
- token version
- short expiry
- introspection
- rotate refresh tokens

## Strong Answer

> JWT revocation is difficult because JWT is self-contained. I reduce risk with short-lived access tokens, refresh token rotation, and denylist/token-version checks for high-risk systems.

---

# 31. CSRF

## Definition

CSRF means Cross-Site Request Forgery.

It tricks a browser into sending an authenticated request to another site using existing cookies.

## Why It Happens

Browsers automatically attach cookies to matching domains.

If authentication uses cookies/session, CSRF matters.

## Example

User is logged into banking app.

Attacker page causes browser to submit:

```text
POST /transfer
```

Browser sends bank cookies automatically.

## CSRF In Stateless JWT APIs

If JWT is sent in:

```text
Authorization: Bearer <token>
```

and not automatically attached by browser, CSRF risk is lower.

That is why many stateless REST APIs disable CSRF:

```java
.csrf(csrf -> csrf.disable())
```

## But Be Careful

If JWT is stored in cookie and browser sends it automatically, CSRF can still matter.

## Strong Answer

> CSRF mainly affects cookie-based browser authentication because cookies are sent automatically. For stateless APIs using Authorization header bearer tokens, CSRF is commonly disabled. But if tokens are stored in cookies, CSRF protection should be reconsidered.

---

# 32. CORS

## Definition

CORS means Cross-Origin Resource Sharing.

It is a browser security mechanism controlling whether JavaScript from one origin can call another origin.

## Example

Frontend:

```text
https://app.example.com
```

Backend:

```text
https://api.example.com
```

Browser treats this as cross-origin.

## Preflight Request

Browser may send:

```text
OPTIONS /api/bookings
```

before actual request.

## Important Spring Security Point

CORS must be handled before authentication because preflight may not contain cookies or auth headers.

## Config Example

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .anyRequest().authenticated()
        )
        .build();
}
```

## CORS Source Example

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.example.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

## Strong Answer

> CORS is enforced by browsers, not by backend-to-backend calls. In Spring Security, CORS should be configured so preflight requests are allowed before authentication blocks them.

---

# 33. AuthenticationEntryPoint and AccessDeniedHandler

## AuthenticationEntryPoint

Handles unauthenticated requests.

Returns:

```text
401
```

Example:

```text
Missing/invalid token
```

## AccessDeniedHandler

Handles authenticated but unauthorized requests.

Returns:

```text
403
```

Example:

```text
User has token but lacks admin role
```

## Config Example

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"unauthorized\"}");
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"forbidden\"}");
            })
        )
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .build();
}
```

## Strong Answer

> `AuthenticationEntryPoint` handles 401 unauthenticated cases. `AccessDeniedHandler` handles 403 authenticated-but-not-authorized cases.

---

# 34. Method Security

## Why Method Security

URL security protects endpoints.

Method security protects service methods.

Useful when:

- same method called from multiple controllers
- authorization depends on method parameter
- domain-level authorization is needed

## Enable

```java
@Configuration
@EnableMethodSecurity
class MethodSecurityConfig {
}
```

## Examples

```java
@PreAuthorize("hasRole('ADMIN')")
public void cancelAnyBooking(String bookingId) {
}
```

```java
@PreAuthorize("#customerId == authentication.name")
public List<Booking> getCustomerBookings(String customerId) {
}
```

```java
@PreAuthorize("hasAuthority('SCOPE_booking.write')")
public Booking createBooking(CreateBookingRequest request) {
}
```

## Common Annotations

| Annotation | Meaning |
|---|---|
| `@PreAuthorize` | Check before method execution |
| `@PostAuthorize` | Check after method execution |
| `@PreFilter` | Filter input collection |
| `@PostFilter` | Filter output collection |

## Strong Answer

> Method security allows fine-grained authorization at service method level. I use `@PreAuthorize` when authorization depends on roles, scopes, or method parameters.

---

# 35. URL Security vs Method Security

## URL Security

Good for:

- broad endpoint rules
- public/private/admin URLs
- edge-level protection

Example:

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

## Method Security

Good for:

- business rules
- ownership checks
- parameter-based checks
- service-level protection

Example:

```java
@PreAuthorize("#booking.customerId == authentication.name")
```

## Strong Answer

> I use URL security for request-level rules and method security for business-level authorization. They complement each other.

---

# 36. Stateless Logout

## Session-Based Logout

Server can invalidate session.

```text
JSESSIONID removed/invalidated
```

## JWT Logout

JWT is stateless, so server cannot simply delete session.

Options:

- client deletes token
- use short access token expiry
- revoke refresh token
- maintain access token denylist
- use token version in DB

## Strong Answer

> In stateless JWT systems, logout usually means deleting token on client and revoking refresh token. If immediate access-token invalidation is required, we need a denylist or token version check.

---

# 37. Refresh Token Rotation

## Definition

Refresh token rotation issues a new refresh token every time one is used.

Old refresh token becomes invalid.

## Why It Helps

If stolen refresh token is reused, system can detect reuse.

## Strong Answer

> Refresh token rotation reduces risk. Every refresh call returns a new refresh token and invalidates the old one. Reuse of an old refresh token can indicate compromise.

---

# 38. API Key vs JWT vs OAuth2

| Mechanism | Best For | Notes |
|---|---|---|
| API key | Simple service/client identity | Weak user context |
| JWT | Stateless signed claims | Needs expiry/revocation strategy |
| OAuth2 | Delegated authorization | Standard for enterprise auth |
| Session cookie | Browser web apps | CSRF protection needed |

## Strong Answer

> API keys identify clients, JWT carries signed claims, OAuth2 defines token-based authorization flows, and sessions are server-side login state usually used with browser apps.

---

# 39. Securing Microservices

## Common Pattern

```text
Client -> API Gateway -> Microservice
```

Gateway:

- authenticates external request
- may validate token
- applies rate limiting
- forwards token or internal identity

Microservice:

- validates token or trusted internal token
- enforces service-level authorization
- never blindly trusts user input

## Service-To-Service Auth

Options:

- OAuth2 client credentials
- mTLS
- service mesh identity
- signed internal JWT

## Strong Answer

> Gateway security is not enough for sensitive systems. Individual services should still enforce authorization, especially for business decisions.

---

# 40. Common Production Security Headers

## Useful Headers

| Header | Purpose |
|---|---|
| `Strict-Transport-Security` | Enforce HTTPS |
| `X-Content-Type-Options` | Prevent MIME sniffing |
| `Content-Security-Policy` | Reduce XSS impact |
| `X-Frame-Options` | Clickjacking protection |
| `Cache-Control` | Prevent sensitive response caching |

## Interview Answer

> For APIs, HTTPS is mandatory. For browser-facing apps, security headers like HSTS, CSP, and frame options reduce common attack risks.

---

# 41. Common Attack Concepts

## XSS

Attacker injects malicious script into page.

Mitigation:

- output encoding
- content security policy
- avoid storing tokens in unsafe places

## CSRF

Attacker tricks browser into sending authenticated request.

Mitigation:

- CSRF token for cookie-based auth
- SameSite cookies
- avoid cookie-based auth for stateless APIs unless protected

## SQL Injection

Attacker injects SQL through input.

Mitigation:

- parameterized queries
- JPA parameters
- no string-concatenated SQL

## Broken Access Control

User accesses data they should not.

Mitigation:

- method-level authorization
- ownership checks
- server-side authorization

## Strong Answer

> Authentication alone is not enough. We must also prevent broken access control by checking whether the user can access that specific resource.

---

# 42. JWT Storage On Client

## Options

| Storage | Pros | Cons |
|---|---|---|
| Memory | Safer from persistent theft | Lost on refresh |
| Local storage | Easy | XSS risk |
| Session storage | Tab scoped | XSS risk |
| HttpOnly cookie | JS cannot read | CSRF needs handling |

## Strong Answer

> Token storage is a frontend/security design decision. Local storage is convenient but vulnerable to XSS. HttpOnly cookies reduce JS theft but require CSRF protection.

---

# 43. Security Testing

## Unit/Integration Testing Helpers

Common Spring Security test support:

- `@WithMockUser`
- `@WithUserDetails`
- `spring-security-test`
- MockMvc with security

## Example

```java
@Test
@WithMockUser(roles = "ADMIN")
void adminCanCancelBooking() throws Exception {
    mockMvc.perform(delete("/api/admin/bookings/B1"))
        .andExpect(status().isNoContent());
}
```

## JWT Test Example

```java
mockMvc.perform(get("/api/bookings/B1")
        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_booking.read"))))
    .andExpect(status().isOk());
```

## Strong Answer

> Security rules should be tested. I test success and failure paths: unauthenticated gets 401, authenticated without permission gets 403, and correct role/scope gets success.

---

# 44. Production Debugging Checklist

## When API Returns 401

Check:

- token missing?
- wrong `Authorization` header format?
- expired token?
- wrong issuer?
- wrong audience?
- bad signature?
- resource server cannot fetch JWK?
- clock skew?

## When API Returns 403

Check:

- token valid but missing role/scope?
- `hasRole` vs `hasAuthority` prefix issue?
- custom converter missing?
- method security blocking?
- ownership check failed?

## When Browser Fails Before API

Check:

- CORS preflight blocked?
- OPTIONS request secured?
- allowed origin mismatch?
- missing allowed header `Authorization`?
- credentials with wildcard origin?

## Strong Answer

> For 401 I debug token validity and authentication. For 403 I debug authorities and authorization rules. For browser failures I check CORS preflight first.

---

# 45. Marriott Booking API Security Mapping

## Requirements

```text
Public users can search hotels
Logged-in users can create bookings
Users can view only their own bookings
Support can view customer bookings
Admin can cancel any booking
Internal services can publish booking status updates
```

## URL Rules

```java
.requestMatchers(HttpMethod.GET, "/api/hotels/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/bookings").hasAuthority("SCOPE_booking.write")
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

## Method Rules

```java
@PreAuthorize("hasRole('SUPPORT') or #customerId == authentication.name")
public List<BookingResponse> getBookings(String customerId) {
    return bookingService.getBookings(customerId);
}
```

## Token Claims

```json
{
  "sub": "customer-123",
  "scope": "booking.read booking.write",
  "roles": ["CUSTOMER"],
  "iss": "https://identity.marriott-example.com",
  "aud": "booking-api",
  "exp": 1760000000
}
```

## Strong Interview Answer

> I would keep hotel search public, require JWT for booking APIs, map scopes like `booking.read` and `booking.write` to authorities, and use method security for ownership checks so users can only access their own bookings unless they have support/admin role.

---

# 46. Hot Interview Questions And Strong Answers

## Q1. What is Spring Security?

Spring Security is a framework that secures Java applications using authentication, authorization, and filter-chain based request protection.

## Q2. How does Spring Security work internally?

For web apps, requests pass through Spring Security filters before reaching controllers. Filters authenticate the request, populate `SecurityContextHolder`, and enforce authorization.

## Q3. Authentication vs authorization?

Authentication verifies identity. Authorization checks what the authenticated user is allowed to access.

## Q4. 401 vs 403?

401 means unauthenticated. 403 means authenticated but forbidden.

## Q5. What is `SecurityContextHolder`?

It stores the current `SecurityContext`, including the current `Authentication`.

## Q6. What is `Authentication`?

It represents the principal, credentials, authorities, and authentication status.

## Q7. What is `UserDetailsService`?

It loads user details by username. Password validation is done by authentication provider with `PasswordEncoder`.

## Q8. Why use `PasswordEncoder`?

To securely hash and verify passwords. We should store password hashes, not plain text.

## Q9. BCrypt vs encryption?

BCrypt is one-way password hashing. Encryption is reversible. Passwords should be hashed, not encrypted.

## Q10. Role vs authority?

Authority is any permission string. Role is a convention usually represented as authority with `ROLE_` prefix.

## Q11. `hasRole` vs `hasAuthority`?

`hasRole("ADMIN")` checks `ROLE_ADMIN`. `hasAuthority("ROLE_ADMIN")` checks exact authority.

## Q12. What is JWT?

JWT is a signed token format with header, payload, and signature. It carries claims and can be validated by resource servers.

## Q13. Is JWT encrypted?

Usually no. It is signed, not encrypted. Payload is readable, so do not store secrets in it.

## Q14. What do you validate in JWT?

Signature, expiry, issuer, audience, not-before, algorithm, and required authorities/scopes.

## Q15. OAuth2 vs JWT?

OAuth2 is an authorization framework. JWT is a token format.

## Q16. What is resource server?

A resource server is an API that validates access tokens and protects resources.

## Q17. What is authorization code flow?

A user-facing OAuth2 flow where the client receives an authorization code and exchanges it for tokens. With PKCE, it is safer for public clients.

## Q18. What is client credentials flow?

OAuth2 flow for service-to-service authentication where a machine client gets an access token using client credentials.

## Q19. Why disable CSRF in JWT APIs?

If tokens are sent in Authorization header and not automatically attached by browser, CSRF risk is lower. But cookie-based auth still needs CSRF protection.

## Q20. What is CORS?

CORS is a browser mechanism that controls whether frontend JavaScript from one origin can call another origin.

## Q21. Why does CORS preflight fail with Spring Security?

Preflight OPTIONS request may not include auth credentials. If security blocks it before CORS handling, browser call fails.

## Q22. What is method security?

Method security uses annotations like `@PreAuthorize` to enforce authorization at method level.

## Q23. What is refresh token?

Refresh token is used to obtain a new access token without forcing user login again.

## Q24. How do you revoke JWT?

Use short expiry, refresh token revocation, denylist, token version, or opaque tokens with introspection.

## Q25. How would you secure a booking API?

Use JWT/OAuth2 resource server, protect booking endpoints with scopes, enforce ownership using method security, and use admin/support roles for privileged operations.

---

# 47. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Storing plain passwords | Credential leak disaster | Use `PasswordEncoder` |
| Saying JWT is encrypted | Payload is readable | Say signed, not encrypted |
| Putting secrets in JWT | Client can decode payload | Store only non-sensitive claims |
| Confusing OAuth2 and JWT | One is framework, one is token format | Explain separately |
| Using `hasRole("ROLE_ADMIN")` | Prefix duplicated | Use `hasRole("ADMIN")` |
| Not validating issuer/audience | Token from wrong source accepted | Validate claims |
| Long-lived access tokens | Hard revocation | Short access token + refresh |
| Disabling CSRF blindly | Cookie auth may be exposed | Disable only when appropriate |
| Ignoring CORS preflight | Browser calls fail | Configure CORS and OPTIONS |
| Trusting gateway only | Internal bypass risk | Enforce service authorization |
| No method ownership check | Broken access control | Check resource owner |
| Logging full token | Token leak | Mask sensitive headers |

---

# 48. Final Rapid Revision

## If Interviewer Says X, Think Y

| Interviewer Says | Think |
|---|---|
| Who are you? | Authentication |
| What can you access? | Authorization |
| Missing token | 401 |
| Valid token, wrong role | 403 |
| Current user | `SecurityContextHolder` |
| Password storage | `PasswordEncoder`, BCrypt |
| JWT parts | header, payload, signature |
| JWT validation | signature, exp, iss, aud |
| OAuth2 role of API | Resource Server |
| Service-to-service OAuth | Client credentials |
| Browser preflight issue | CORS |
| Cookie-based attack | CSRF |
| Business ownership access | Method security |
| Scope mapping | `SCOPE_` authority |
| Admin role check | `ROLE_ADMIN` |

---

# 49. One-Hour Revision Plan

## First 15 Minutes: Foundation

Revise:

- authentication vs authorization
- 401 vs 403
- filter chain
- `SecurityContextHolder`
- `Authentication`

## Next 15 Minutes: User/Password Flow

Revise:

- `UserDetailsService`
- `AuthenticationManager`
- `AuthenticationProvider`
- `PasswordEncoder`
- roles vs authorities

## Next 15 Minutes: JWT/OAuth2

Revise:

- JWT structure
- JWT validation
- OAuth2 roles
- resource server
- access token vs refresh token
- scope mapping

## Final 15 Minutes: API Security

Revise:

- stateless config
- CSRF
- CORS
- method security
- 401/403 debugging
- Marriott booking API mapping

---

# 50. Strong Closing Answer

If asked:

> How would you secure a Spring Boot microservice?

Say:

> I would configure the service as an OAuth2 Resource Server if the organization uses an identity provider. The API would validate bearer JWTs using issuer or JWK configuration, map scopes or roles to Spring authorities, and keep the service stateless. I would secure endpoints with `authorizeHttpRequests` and use method security for business-level checks like booking ownership. For password-based login, I would store only hashed passwords using `PasswordEncoder`, preferably BCrypt or a delegated encoder. I would handle 401 and 403 separately, configure CORS for browser clients, disable CSRF only for stateless Authorization-header APIs, and avoid logging tokens or sensitive claims.

---

# 51. Official Source Notes

Useful official references:

- Spring Security OAuth2 Resource Server JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Spring Security OAuth2 overview: https://docs.spring.io/spring-security/reference/servlet/oauth2/
- Spring Security authentication architecture: https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html
- Spring Security password storage: https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
- Spring Security method security: https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
- Spring Security CSRF: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- Spring Security CORS: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html

---

# 52. How To Use This Guide By Level

| Level | What To Master |
|---|---|
| Starter | authentication vs authorization, 401 vs 403, password hashing, basic config |
| Intermediate | filter chain, `SecurityContextHolder`, roles/authorities, JWT validation |
| Senior | OAuth2 resource server, custom claims, CSRF/CORS, method ownership checks |
| MAANG-ready | threat modeling, token revocation strategy, multi-service auth, production debugging |

Starter target:

```text
I can secure REST APIs with authenticated and public routes, hashed passwords, and role or
authority checks.
```

Senior target:

```text
I can explain how Spring Security authenticates through filters, stores Authentication in
SecurityContextHolder, maps JWT claims to authorities, and enforces URL and method security.
```

---

# 53. Advanced Security Production Scenarios

## Scenario 1: Valid JWT Still Gets 403

Check:
- token is authenticated but missing required authority
- scope mapped as `SCOPE_x`
- role prefix mismatch between `hasRole` and `hasAuthority`
- custom claim converter not configured
- method security has stricter rule than URL security

Strong answer:

```text
For 403, authentication already succeeded. I inspect the Authentication object and granted
authorities, then compare them with URL and method security rules.
```

## Scenario 2: Browser Request Fails Before Controller

Check:
- CORS preflight blocked
- OPTIONS request requires authentication
- frontend origin not allowed
- Authorization header not allowed
- credentials and wildcard origin misconfigured

Strong answer:

```text
If the controller is never reached, I check CORS and preflight first. Browsers enforce CORS,
and Spring Security must allow CORS processing before authentication blocks OPTIONS.
```

## Scenario 3: Logout In Stateless JWT System

Reality:

```text
Server cannot simply delete a JWT that is already issued unless it tracks revocation state.
```

Options:
- short-lived access tokens
- refresh token rotation
- refresh token revocation
- denylist for high-risk access tokens
- token version stored server-side
- opaque tokens with introspection

Strong answer:

```text
JWT logout is usually handled by expiring access tokens quickly and revoking refresh tokens.
For high-risk systems, I add denylist or token-version checks.
```

---

# 54. Capstone Practice Questions

## Capstone 1: Secure Booking API

Prompt:

```text
Secure booking APIs where customers can view their own bookings, support can view customer
bookings, and admins can cancel any booking.
```

Strong answer should mention:
- OAuth2 resource server
- JWT issuer/JWK validation
- scopes like `booking.read`, `booking.write`
- roles like `ROLE_SUPPORT`, `ROLE_ADMIN`
- method security for ownership
- separate 401 and 403 handlers
- audit sensitive actions

## Capstone 2: Design JWT And Refresh Token Flow

Strong answer should mention:
- short-lived access token
- longer-lived refresh token
- refresh token stored server-side or with rotation tracking
- refresh token reuse detection
- logout revokes refresh token
- no secrets in JWT payload
- validate signature, expiry, issuer, audience

## Capstone 3: Debug Auth Incident

Prompt:

```text
After deployment, all frontend API calls fail. Backend logs show no controller hit.
```

Strong answer should mention:
- inspect browser network tab
- check preflight OPTIONS
- verify CORS config
- verify allowed origins and headers
- verify gateway/security filter order
- test direct API call with token
- distinguish CORS failure from 401/403

---

# 55. Security Gold Checklist

You are strong in Spring Security if you can explain:

- authentication vs authorization
- 401 vs 403
- servlet filter chain
- `DelegatingFilterProxy`
- `FilterChainProxy`
- `SecurityContextHolder`
- `Authentication`
- `AuthenticationManager` and provider flow
- `UserDetailsService`
- password hashing with `PasswordEncoder`
- roles vs authorities
- stateless JWT API setup
- JWT is signed, not encrypted
- JWT validation claims
- OAuth2 roles and grant types
- resource server config
- scope-to-authority mapping
- custom claim converter
- CSRF and CORS differences
- method security and ownership checks
- refresh token rotation
- token revocation limitations
- safe token storage trade-offs
- security testing and debugging
