# Spring Boot MAANG Scenario Based Architecture Questions Platinum Sheet

Target: fast revision before senior Java backend interviews.

This sheet converts Spring Boot knowledge into practical interview answers. Every scenario
uses a simple structure:

```text
Requirement -> design choice -> code/config shape -> failure handling -> trade-offs
```

---

## 0. Answer Formula

Use this for every Spring Boot architecture question:

```text
1. Clarify requirement and scale.
2. Pick Spring feature/tool.
3. Explain runtime behavior.
4. Add correctness controls.
5. Add failure handling.
6. Add observability and tests.
7. Mention trade-off or alternative.
```

---

# 1. Design A Booking REST API

## Requirements

- create booking
- validate request
- prevent double booking
- return clean errors
- support idempotent retry

## Strong Design

Layers:

```text
Controller -> Service -> Repository -> Database
```

Controls:

- request DTO validation
- service-layer transaction
- unique constraint for room/date booking
- idempotency key table
- global exception handler
- structured logs with correlation ID

Strong answer:

```text
I would keep controller thin, validate DTOs, put business logic in service, and protect
double booking with a database constraint or lock, not only application checks. For retries,
I use idempotency keys. Errors are mapped centrally through ControllerAdvice.
```

---

# 2. Prevent Double Booking Under Concurrency

## Options

| Option | Use When |
|---|---|
| unique constraint | exact business uniqueness can be modeled |
| optimistic locking | conflicts are rare |
| pessimistic lock | conflicts are frequent and must serialize |
| serializable transaction | strongest DB-level protection, lower concurrency |
| distributed lock | only when resource spans systems and DB lock is not enough |

## Spring Shape

```java
@Transactional
public Booking confirm(BookingRequest request) {
    availabilityRepository.lockRoomDate(request.roomId(), request.date());
    availabilityRepository.decrement(request.roomId(), request.date());
    return bookingRepository.save(Booking.confirmed(request));
}
```

Strong answer:

```text
I first protect the invariant in the database using unique constraints or row-level locks.
Spring @Transactional defines the local transaction boundary. If the workflow spans payment
or notification, I use pending state plus saga/outbox instead of one long transaction.
```

---

# 3. Build A Search API With Many Filters

## Requirements

- optional filters
- pagination
- sorting
- performance

Design:

- use DTO for filter request
- build query dynamically with Criteria/Specification/Querydsl or explicit SQL
- avoid giant OR predicates that break indexes
- use cursor pagination for deep pages
- return DTO projection
- add indexes matching filter/sort patterns

Strong answer:

```text
I avoid one huge derived query method. For dynamic filters I use Specification/Criteria or a
query builder, project only needed columns, paginate, and validate with EXPLAIN ANALYZE on
the real database.
```

---

# 4. Handle Payment Provider Timeout

## Bad Design

```text
Open DB transaction -> call payment provider -> wait -> update DB
```

Problem:

- holds DB connection while waiting
- retry can double charge
- unclear state after timeout

Better:

```text
1. Create booking as PENDING_PAYMENT.
2. Save payment attempt with idempotency key.
3. Call provider with timeout.
4. On success, mark authorized.
5. On timeout, keep pending and reconcile with provider webhook/status check.
```

Spring tools:

- `RestClient` or `WebClient`
- configured timeouts
- retry only if idempotent
- circuit breaker
- outbox event

---

# 5. Use Cache For Hotel Details

## Requirement

Hotel details are read-heavy and change rarely.

Design:

- `@Cacheable` for read path
- key includes hotel ID and locale
- TTL at cache provider
- evict on update
- distributed cache if multiple app instances need shared view
- avoid caching user-specific data unless key includes user/tenant/security context

Strong answer:

```text
I use cache-aside for read-heavy hotel details, but I define key design, TTL, eviction, and
stale-data tolerance. Caching is not only adding @Cacheable.
```

---

# 6. Send Email After Booking Confirmed

## Requirement

Booking confirmation should not fail because email provider is slow.

Design choices:

| Choice | Fit |
|---|---|
| Spring event | same app, low durability need |
| `@Async` | fire-and-forget internal async, weak durability |
| Kafka/RabbitMQ | cross-service durable async workflow |
| Outbox | DB change and event publish must be reliable |

Strong answer:

```text
For production booking confirmation, I prefer outbox plus message broker. The booking
transaction commits booking state and outbox event together. A publisher sends the event to
Kafka/RabbitMQ, and Notification Service consumes idempotently.
```

---

# 7. Design A Batch Job For Nightly Settlement

## Requirements

- process millions of rows
- restart safely
- skip bad records
- retry transient errors
- track progress

Spring Batch shape:

```text
Job -> Step -> ItemReader -> ItemProcessor -> ItemWriter
```

Controls:

- chunk processing
- JobRepository
- restartable reader
- idempotent writer
- skip policy for bad data
- retry policy for transient DB/API errors
- partitioning for scale

Strong answer:

```text
I would not write one giant loop in a scheduled method. For large restartable processing,
Spring Batch gives JobRepository, chunk transactions, skip/retry, restartability, and
partitioning.
```

---

# 8. Secure A REST API With JWT

## Requirements

- validate JWT
- protect endpoints by role/scope
- return 401/403 correctly
- support method-level authorization

Design:

- configure SecurityFilterChain
- resource server validates JWT signature/issuer/audience
- map scopes/roles to authorities
- use method security for business authorization
- do not trust client-sent user ID

Strong answer:

```text
Authentication proves who the caller is. Authorization decides what they can do. In a JWT
resource server, I validate token signature and claims, map authorities, and enforce access
at both route and method/business layers where needed.
```

---

# 9. Build A Resilient Downstream Client

## Requirements

- call Inventory Service
- avoid hanging threads
- handle temporary failure
- avoid retry storm

Design:

- timeout
- retry only idempotent calls
- exponential backoff and jitter
- circuit breaker
- fallback if business allows
- metrics per downstream

Strong answer:

```text
Every outbound call needs a timeout. Retries need idempotency, backoff, jitter, and a retry
budget. Circuit breaker protects the caller when dependency failure is sustained.
```

---

# 10. Expose Production Health And Metrics

## Requirements

- Kubernetes readiness/liveness
- dashboards
- alerting
- debug during incidents

Design:

- Actuator health groups
- readiness for serving traffic
- liveness for process health
- Micrometer metrics
- Prometheus scrape
- tracing
- structured logs

Strong answer:

```text
I expose readiness and liveness separately. Readiness can depend on critical dependencies;
liveness should not restart the app for temporary DB outages. Metrics include request rate,
errors, latency, JVM, DB pool, cache, and downstream clients.
```

---

# 11. Migrate A Database Schema Safely

## Expand-Contract Pattern

```text
1. Expand: add nullable/new column/table.
2. Deploy app that writes both old and new.
3. Backfill data.
4. Deploy app that reads new.
5. Contract: remove old column after no users.
```

Tools:

- Flyway or Liquibase
- migration tests
- backward-compatible deploys
- rollback plan

Strong answer:

```text
In rolling deployments, old and new app versions run together. I use expand-contract
migrations so both versions remain compatible.
```

---

# 12. Choose MVC vs WebFlux

| MVC | WebFlux |
|---|---|
| simpler imperative model | non-blocking reactive model |
| great for typical CRUD APIs | useful for high concurrency I/O |
| easier debugging | harder context/debugging |
| blocking libraries are fine | blocking calls can break benefits |

Strong answer:

```text
I choose MVC for most standard Spring Boot CRUD services. I choose WebFlux when the service
is mostly non-blocking I/O and the team is ready for reactive complexity.
```

---

# 13. Design Multi-Tenant Spring Boot Service

Requirements:

- tenant isolation
- tenant-aware queries
- tenant-aware cache keys
- authorization by tenant
- audit logs

Controls:

- tenant ID resolved from trusted token/header
- repository filters include tenant ID
- DB constraints include tenant ID when relevant
- cache key includes tenant ID
- logs include tenant ID
- admin operations require explicit authorization

Trap:

```text
Never trust tenant ID from request body alone.
```

---

# 14. Common Scenario Answer Closers

Use these one-liners:

- "I protect correctness in the database, not only in Java code."
- "I keep remote calls outside long DB transactions."
- "I make retry paths idempotent."
- "I use outbox when DB commit and event publish must be consistent."
- "I validate performance with realistic data and EXPLAIN."
- "I separate readiness from liveness."
- "I prefer simple MVC unless reactive gives clear value."
- "I test with the same database dialect using Testcontainers."

---

# 15. Final Rapid Revision

```text
REST API -> controller/service/repository, validation, advice.
Double booking -> transaction + DB constraint/lock + idempotency.
Search filters -> dynamic query + projection + index + pagination.
Payment timeout -> pending state + idempotency + reconciliation.
Cache -> key, TTL, eviction, stale tolerance.
Email/event -> outbox + broker for durable async.
Batch -> Spring Batch, chunks, restart, skip/retry.
JWT -> validate token, map authorities, 401 vs 403.
Downstream -> timeout, retry budget, circuit breaker.
Observability -> actuator, metrics, traces, logs.
Migration -> expand-contract.
MVC/WebFlux -> simplicity vs non-blocking complexity.
```

---

# 16. Official Source Notes

- Spring Boot reference: https://docs.spring.io/spring-boot/reference/index.html
- Spring Boot Actuator endpoints: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- Spring Framework transactions: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
