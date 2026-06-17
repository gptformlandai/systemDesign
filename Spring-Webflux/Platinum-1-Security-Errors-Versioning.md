# Platinum 1 - WebFlux Security, Error Handling, API Versioning

> Goal: make WebFlux safe and interview-ready beyond operators. This sheet covers reactive
> security, error response design, validation, CORS, API versioning, and HTTP caching.

---

## 0. Mental Model First

WebFlux does not remove normal backend responsibilities.

It changes how those responsibilities execute:

- authentication still happens before protected handlers
- authorization still decides whether the caller can perform the action
- validation still protects the boundary
- error handling still shapes client-visible failures
- versioning still protects API evolution
- CORS and caching still affect browser/client behavior

The difference is that all of this must work inside a non-blocking, signal-driven request
flow.

### The Senior Rule

```text
A reactive API is not production-ready just because it returns Mono or Flux.
It is production-ready when auth, validation, errors, versioning, and observability are
designed as carefully as the operator chain.
```

---

## 1. Reactive Security Big Picture

### What Changes In Request Flow

In a secured WebFlux application:

1. Request enters the reactive HTTP stack.
2. `SecurityWebFilterChain` applies security filters.
3. Authentication extracts credentials, token, session, or mTLS identity.
4. Authorization checks route/method permissions.
5. If allowed, the request reaches handler/controller.
6. Handler returns `Mono` or `Flux`.
7. Security context must survive thread hops through reactive context.

### MVC vs WebFlux Security

| Area | Spring MVC | WebFlux |
|---|---|---|
| Filter chain | Servlet filters | Web filters |
| Security config | `SecurityFilterChain` | `SecurityWebFilterChain` |
| Request context | often ThreadLocal-friendly | must be reactive-context aware |
| Method security | supported | supported, but return types can be reactive |
| Common trap | missing authorization rules | relying on ThreadLocal assumptions |

### Code Sample

```java
@Bean
SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers("/actuator/health").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/hotels/**").permitAll()
            .pathMatchers("/api/bookings/**").authenticated()
            .anyExchange().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
}
```

### Interview Trap

"We validate JWT at the gateway, so downstream WebFlux services do not need security."

Wrong. Gateway security is useful, but service-level authorization still matters for defense
in depth, internal callers, and accidental bypass.

---

## 2. Authentication vs Authorization

### Definition

- Authentication: who are you?
- Authorization: what can you do?

### Reactive Request Example

```java
@GetMapping("/api/bookings/{id}")
Mono<BookingView> getBooking(@PathVariable String id, Authentication authentication) {
    return bookingService.findForUser(id, authentication.getName());
}
```

This is still not enough if business ownership rules are complex. Route-level auth says the
caller is logged in. Business-level auth says this booking belongs to that caller or tenant.

### Strong Interview Answer

```text
I separate route security from business authorization. The security filter chain validates
identity and coarse permissions. The service layer checks resource ownership, tenant
boundaries, and domain-specific access rules.
```

---

## 3. 401 vs 403 In WebFlux

| Status | Meaning |
|---|---|
| 401 Unauthorized | caller is not authenticated or token is invalid |
| 403 Forbidden | caller is authenticated but lacks permission |

### Common Debug Flow

```text
If no token or invalid token -> 401.
If token valid but missing scope/role/ownership -> 403.
```

### Interview Trap

Returning 403 for expired or missing token confuses clients. Returning 401 for a valid token
with insufficient role leaks the wrong semantic.

---

## 4. Reactive Method Security

Method security is valuable when route-level rules are too coarse.

```java
@PreAuthorize("hasAuthority('SCOPE_booking:read')")
public Mono<BookingView> findBooking(String bookingId) {
    return repository.findById(bookingId);
}
```

For ownership checks, prefer domain-aware services:

```java
public Mono<BookingView> findBookingForUser(String bookingId, String userId) {
    return repository.findById(bookingId)
        .filter(booking -> booking.userId().equals(userId))
        .switchIfEmpty(Mono.error(new AccessDeniedException("booking not accessible")))
        .map(this::toView);
}
```

### Strong Answer

```text
Annotations are good for coarse permission checks. Domain ownership is often clearer in the
service layer because it depends on data loaded reactively.
```

---

## 5. Security Context and Reactor Context

### Why ThreadLocal Is Risky

Reactive execution can move across threads. If code assumes request data lives only in a
ThreadLocal, trace IDs or auth details may disappear after scheduler hops.

### Correct Mental Model

Spring Security integrates with reactive context so security information follows the
publisher chain.

Useful pattern:

```java
Mono<String> currentUser() {
    return ReactiveSecurityContextHolder.getContext()
        .map(ctx -> ctx.getAuthentication().getName());
}
```

### Interview Punchline

```text
In WebFlux, request-scoped security data must be reactive-context aware because thread
affinity is not guaranteed.
```

---

## 6. CSRF In WebFlux

CSRF matters primarily for browser-based cookie/session flows.

For stateless JWT APIs:

```java
http.csrf(ServerHttpSecurity.CsrfSpec::disable);
```

For browser sessions:

- keep CSRF enabled
- expose token to frontend safely
- validate unsafe methods like POST/PUT/PATCH/DELETE

### Interview Trap

"Always disable CSRF in APIs."

Better:

```text
Disable CSRF for stateless token-based APIs when credentials are not automatically attached
by the browser. Keep it for cookie/session browser flows.
```

---

## 7. CORS In WebFlux

CORS controls browser cross-origin access. It is not service-to-service security.

Example:

```java
@Bean
CorsWebFilter corsWebFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.example.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return new CorsWebFilter(source);
}
```

### Common Mistake

```text
allowedOrigins("*") + allowCredentials(true)
```

That combination is unsafe and often invalid depending configuration.

---

## 8. Validation In Reactive Controllers

### Request Body Validation

```java
record BookingRequest(
    @NotBlank String guestId,
    @NotBlank String hotelId,
    @Future LocalDate checkIn,
    @Future LocalDate checkOut
) {}

@PostMapping("/api/bookings")
Mono<BookingResponse> create(@Valid @RequestBody Mono<BookingRequest> requestMono) {
    return requestMono.flatMap(bookingService::create);
}
```

### Important Point

Validation happens when the body is decoded and consumed, not when the method is merely
declared.

### Domain Validation

Bean validation checks shape. Domain validation checks business rules.

```java
return requestMono
    .filter(req -> req.checkOut().isAfter(req.checkIn()))
    .switchIfEmpty(Mono.error(new BadRequestException("checkOut must be after checkIn")))
    .flatMap(bookingService::create);
```

---

## 9. Error Handling Layers

There are three levels:

| Level | Use |
|---|---|
| Operator-level | local fallback in one chain |
| Controller advice | map domain exceptions to API response |
| Global web exception handler | final framework-level safety net |

### Operator-Level Error Handling

```java
return paymentClient.authorize(request)
    .timeout(Duration.ofSeconds(1))
    .onErrorResume(TimeoutException.class, ex -> paymentFallback.pending(request));
```

Use when fallback is part of business behavior.

### API-Level Error Mapping

```java
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(BookingNotFoundException.class)
    Mono<ResponseEntity<ApiError>> notFound(BookingNotFoundException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError("BOOKING_NOT_FOUND", ex.getMessage())));
    }
}
```

Use when exception must become a stable client contract.

---

## 10. Problem Details / Error Response Design

Good error response:

```json
{
  "type": "https://api.example.com/errors/booking-not-found",
  "title": "Booking not found",
  "status": 404,
  "detail": "No booking exists for the supplied id",
  "instance": "/api/bookings/b123",
  "code": "BOOKING_NOT_FOUND",
  "traceId": "9ab2..."
}
```

Senior design:

- stable machine-readable code
- human-readable title/detail
- HTTP status matches semantics
- trace ID included
- no stack trace or sensitive internals
- validation errors include field-level details

### Strong Answer

```text
I keep internal exception classes separate from public API errors. Public errors are stable,
documented, traceable, and safe.
```

---

## 11. Error Handling Anti-Patterns

| Anti-Pattern | Why It Hurts |
|---|---|
| `onErrorResume(Exception.class, fallback)` everywhere | hides real failures |
| returning 200 with error body | breaks client and monitoring semantics |
| exposing stack traces | security and noise |
| mapping every error to 500 | loses business meaning |
| retrying validation errors | useless load |
| swallowing cancellation | leaks resources |

### Interview Trap

"Use `onErrorResume` for all errors so users do not see failures."

Better:

```text
Only fallback when the business can accept degraded behavior. Otherwise map the error
correctly and expose a traceable response.
```

---

## 12. API Versioning

API versioning protects clients while the API evolves.

Common styles:

| Style | Example | Notes |
|---|---|---|
| URI version | `/v1/bookings` | simple, visible |
| Header version | `API-Version: 1` | cleaner URI, more hidden |
| Media type | `application/vnd.company.v1+json` | precise but heavier |
| Query param | `?version=1` | easy but often less preferred |

### WebFlux Route Example

```java
@RestController
@RequestMapping("/api/v1/bookings")
class BookingV1Controller {
    @GetMapping("/{id}")
    Mono<BookingV1Response> find(@PathVariable String id) {
        return service.findV1(id);
    }
}
```

### Versioning Rules

- additive changes are usually safe
- removing or renaming fields is breaking
- changing semantics is breaking even if JSON shape is same
- document deprecation
- monitor version usage before removal

### Strong Answer

```text
I version only when compatibility requires it. I prefer additive evolution first, then
explicit versioning for breaking changes, with deprecation metrics and a migration window.
```

---

## 13. HTTP Caching In WebFlux APIs

Useful for read-heavy resources:

- hotel details
- static configuration
- public content
- rarely changing lookup data

Example:

```java
@GetMapping("/api/hotels/{id}")
Mono<ResponseEntity<HotelView>> hotel(@PathVariable String id) {
    return hotelService.find(id)
        .map(view -> ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .eTag("\"" + view.version() + "\"")
            .body(view));
}
```

Do not cache:

- user-specific data without correct key headers
- authorization-sensitive data unless carefully controlled
- rapidly changing state unless stale reads are acceptable

---

## 14. End-To-End Secure Request Story

Use case:

```text
POST /api/v2/bookings/confirm
```

Flow:

1. CORS handles browser preflight if needed.
2. SecurityWebFilterChain validates JWT.
3. Route authorization checks required scope.
4. Controller receives reactive request body.
5. Bean validation checks shape.
6. Service checks tenant and booking ownership.
7. Reactive chain calls downstream services.
8. Domain errors map to stable problem response.
9. Trace ID appears in logs and error body.
10. Version-specific response shape is returned.

### What Would Go Wrong Without These Concepts?

- invalid token treated as internal error
- unauthorized user reads another booking
- validation errors leak stack trace
- API breaking change silently breaks clients
- CORS misconfiguration blocks browser app
- generic error response makes incidents hard to debug

---

## 15. Interview Hot Questions

### 1. How is Spring Security different in WebFlux?

It uses reactive web filters and `SecurityWebFilterChain`. Security state must be reactive
context aware because request execution can move across threads.

### 2. Where do you put authorization?

Use route/method security for coarse rules and service-layer domain checks for resource
ownership, tenant isolation, and business-specific rules.

### 3. When should you use `onErrorResume`?

Use it for a local business fallback, not as a blanket error-swallowing tool.

### 4. How should WebFlux APIs return errors?

Return stable HTTP status codes and structured error bodies with safe details and trace IDs.

### 5. How do you version APIs?

Prefer backward-compatible additive changes first. Use explicit versioning for breaking
changes and monitor old version usage before deprecation.

### 6. What is the biggest security trap?

Assuming gateway auth alone protects every downstream service and every business resource.

---

## 16. Final Revision Notes

```text
SecurityWebFilterChain protects reactive routes.
401 = not authenticated; 403 = authenticated but not allowed.
ThreadLocal assumptions are dangerous; use reactive security/context support.
Bean validation checks request shape; service validation checks business rules.
Operator fallback is business behavior; ControllerAdvice shapes API errors.
Problem responses need status, code, safe detail, and trace ID.
Version APIs for breaking changes, not every small addition.
CORS is browser policy, not service security.
HTTP caching is useful only when data can safely be reused.
```

---

## 17. Official Source Notes

- Spring WebFlux reference: https://docs.spring.io/spring-framework/reference/web/webflux.html
- Spring Security reactive applications: https://docs.spring.io/spring-security/reference/reactive/index.html
- Spring Framework error responses: https://docs.spring.io/spring-framework/reference/web/webflux/ann-rest-exceptions.html
- Spring Framework API versioning: https://docs.spring.io/spring-framework/reference/web/webflux/controller/ann-requestmapping.html
