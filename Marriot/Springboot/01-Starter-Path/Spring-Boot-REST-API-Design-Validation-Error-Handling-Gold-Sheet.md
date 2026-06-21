# Spring Boot REST API Design Validation Error Handling Gold Sheet

> Track: Spring Boot Interview Track - Starter Path  
> Goal: build production-quality REST API judgment beyond controllers and annotations.

Read after Spring Boot Core and before Testing.

---

## 1. Why This Sheet Exists

Many candidates can write `@RestController`. Strong backend candidates can design a stable
API contract with DTO boundaries, validation, error responses, pagination, versioning, and
OpenAPI documentation.

Strong answer:

```text
A Spring Boot REST API is not only a controller method. It needs clear DTOs, validation,
consistent errors, transaction boundaries, security checks, pagination, observability, and
contract documentation.
```

---

## 2. Controller Responsibility

A controller should handle HTTP concerns:

- route mapping
- request DTO binding
- validation trigger
- authentication principal extraction when needed
- response status and headers
- delegating business work to service layer

Avoid in controllers:

- transaction-heavy business logic
- JPA entity mutation directly
- complex authorization rules
- downstream client orchestration
- SQL or repository logic

Strong answer:

```text
I keep controllers thin. They translate HTTP requests into application commands and translate
service results or exceptions into HTTP responses.
```

---

## 3. DTO vs Entity Boundary

Do not expose JPA entities directly from public APIs.

Reasons:

- leaks persistence model
- lazy-loading serialization surprises
- accidental sensitive fields
- bidirectional relationship recursion
- difficult API versioning
- persistence changes break clients

Pattern:

```text
HTTP request -> Request DTO -> Service command -> Entity/domain model -> Response DTO
```

Example:

```java
record CreateBookingRequest(
    @NotNull Long hotelId,
    @NotNull Long roomTypeId,
    @Future LocalDate checkIn,
    @Future LocalDate checkOut
) {}

record BookingResponse(
    Long bookingId,
    String status,
    Instant createdAt
) {}
```

Strong answer:

```text
Entities are persistence models. DTOs are API contracts. Keeping them separate prevents
lazy-loading bugs, data leaks, and accidental API breaking changes.
```

---

## 4. Mapping Choices

| Mapper | Use When | Trade-Off |
|---|---|---|
| Manual mapping | small/simple APIs | explicit but repetitive |
| MapStruct | many DTO mappings | compile-time mapping, extra tool |
| Constructor/projection | query-specific reads | efficient but less reusable |
| ModelMapper-style reflection | quick prototypes | runtime surprises, less explicit |

Interview line:

```text
For critical backend APIs, I prefer explicit mapping, often manual or MapStruct, because API
shape should be intentional.
```

---

## 5. Validation Basics

Use Bean Validation on request DTOs.

Common annotations:

| Annotation | Purpose |
|---|---|
| `@NotNull` | value must exist |
| `@NotBlank` | non-empty string |
| `@Size` | length/collection size |
| `@Min` / `@Max` | numeric range |
| `@Email` | email format |
| `@Future` / `@Past` | date constraints |
| `@Valid` | validate nested object |

Controller example:

```java
@PostMapping("/bookings")
ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
}
```

---

## 6. Validation Groups

Validation groups allow different rules for create vs update.

Example:

```java
interface Create {}
interface Update {}

record HotelRequest(
    @Null(groups = Create.class) Long id,
    @NotNull(groups = Update.class) Long updateId,
    @NotBlank String name
) {}
```

Use carefully:

```text
Validation groups are useful, but if they make DTOs hard to read, separate request DTOs for
create/update are often cleaner.
```

---

## 7. Business Validation

Bean Validation checks shape. Service layer checks business rules.

Examples:

| Rule | Location |
|---|---|
| field is non-empty | DTO validation |
| check-out after check-in | DTO custom validator or service |
| room is available | service + database constraint/lock |
| user can cancel booking | service authorization rule |
| payment can be captured | service/domain state check |

Strong answer:

```text
DTO validation protects request shape. Business validation belongs in the service/domain
layer where repositories, ownership, and state transitions are available.
```

---

## 8. ProblemDetail Error Responses

Spring Framework 6 / Spring Boot 3 supports `ProblemDetail` for RFC 7807-style errors.

Example:

```java
@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(BookingNotFoundException.class)
    ProblemDetail handleNotFound(BookingNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Booking not found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", "BOOKING_NOT_FOUND");
        return problem;
    }
}
```

Good error response includes:

- HTTP status
- stable error code
- human-safe message
- field errors for validation
- correlation/request id
- no stack trace or secrets

Strong answer:

```text
I use centralized exception handling so API errors are consistent, safe, and useful to
clients. In modern Spring, ProblemDetail is a good standard shape.
```

---

## 9. HTTP Status Code Judgment

| Situation | Status |
|---|---:|
| created resource | 201 |
| successful read/update | 200 |
| successful delete with no body | 204 |
| invalid request shape | 400 |
| validation failure | 400 or 422 by API convention |
| unauthenticated | 401 |
| authenticated but forbidden | 403 |
| missing resource | 404 |
| conflict with current state | 409 |
| rate limited | 429 |
| server error | 500 |
| dependency unavailable | 503 |

Interview trap:

```text
Do not return 200 with an error body for normal API failures.
```

---

## 10. Pagination And Sorting

Offset pagination:

```text
?page=0&size=20&sort=createdAt,desc
```

Good for:

- admin pages
- small/medium result sets
- simple UI pages

Problems:

- deep page gets slow
- data shifts while paging

Cursor pagination:

```text
?after=2026-06-21T10:00:00Z_B123&limit=20
```

Good for:

- large feeds
- infinite scroll
- stable next-page behavior

Strong answer:

```text
I use offset pagination for simple admin-style pages, but for high-volume ordered lists I
prefer cursor pagination to avoid deep offset cost and shifting data.
```

---

## 11. Filtering And Query Design

Avoid one huge query that destroys indexes.

Options:

- Spring Data Specifications
- Querydsl
- Criteria API
- explicit SQL/native query
- read-model/search index for heavy search

Production rule:

```text
API filter design must match database index strategy. A flexible filter API can become a
performance problem if every combination causes table scans.
```

---

## 12. API Versioning

Prefer backward-compatible changes first.

Safe changes:

- add optional response field
- add new endpoint
- add optional request field with default

Breaking changes:

- remove/rename field
- change field type
- make optional field required
- change semantic meaning

Versioning options:

| Style | Example |
|---|---|
| URI | `/api/v2/bookings` |
| Header | `Accept: application/vnd.company.booking.v2+json` |
| Additive evolution | no new version for compatible changes |

Strong answer:

```text
I do not version every small change. I keep APIs backward compatible when possible and use
versioning only when clients cannot safely coexist on one contract.
```

---

## 13. OpenAPI / Swagger

OpenAPI documents the API contract.

Use it for:

- endpoint discovery
- request/response examples
- client generation when appropriate
- API review
- contract testing seed

Caution:

```text
Generated docs are only useful if DTOs, validation, status codes, and examples are accurate.
```

Strong answer:

```text
OpenAPI is part of the contract. I use it to communicate API shape, but I still protect
compatibility with tests and review.
```

---

## 14. Idempotent Create APIs

For retry-safe create operations:

```text
POST /bookings
Idempotency-Key: abc-123
```

Server behavior:

1. Check key for user/action.
2. If existing result, return same response.
3. If new, process and store key/result atomically.
4. Reject same key with different payload.

Strong answer:

```text
For payment or booking creation, idempotency keys protect against duplicate side effects
when clients retry after timeout or network failure.
```

---

## 15. API Security Touchpoints

Every API design should define:

- authentication requirement
- authorization rule
- tenant/account boundary
- rate limit
- audit log need
- sensitive fields to mask
- CORS policy if browser clients call it

Example:

```text
Cancel booking: authenticated user can cancel only their own booking unless they have support
or hotel-admin permissions. The action is audited with reason code.
```

---

## 16. Controller Testing Checklist

Use `@WebMvcTest` for controller behavior:

- request validation
- status code
- response body
- error body
- security rules if configured
- JSON serialization

Use full integration tests for:

- real DB behavior
- real security filter chain
- migrations
- transaction behavior
- end-to-end critical flow

---

## 17. Strong Closing Answer

```text
For a production Spring Boot REST API, I keep controllers thin, expose DTOs instead of
entities, validate requests, centralize errors with ProblemDetail, document contracts with
OpenAPI, choose pagination carefully, protect create operations with idempotency, and test
controller behavior separately from full integration behavior.
```
