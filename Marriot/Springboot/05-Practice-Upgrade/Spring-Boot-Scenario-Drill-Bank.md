# Spring Boot Scenario Drill Bank

> Track: Spring Boot Interview Track - Practice Upgrade  
> Goal: practice realistic Spring Boot interview prompts with follow-up pressure.

Use this after reading the concept sheets.

---

## 1. Answer Format

For each scenario, answer in this order:

```text
1. Clarify requirement.
2. Choose Spring feature/design.
3. Explain runtime behavior.
4. Add correctness controls.
5. Add failure handling.
6. Add observability and tests.
7. Mention trade-off or alternative.
```

---

## 2. REST API Scenarios

### Scenario 1: Booking API

Prompt:

```text
Design a Spring Boot create-booking API with validation, security, idempotency, and clean errors.
```

Must include:

- DTO request/response
- `@Valid`
- controller -> service -> repository
- `@Transactional` in service
- DB constraint/lock for availability
- idempotency key
- `@RestControllerAdvice` + ProblemDetail
- security ownership check
- tests with MockMvc and integration test

---

### Scenario 2: Error Handling Standardization

Prompt:

```text
Different controllers return different error shapes. Fix it.
```

Answer:

- centralize with `@RestControllerAdvice`
- use ProblemDetail or stable error DTO
- map validation errors consistently
- add correlation id
- no stack traces/secrets
- test error contract

---

### Scenario 3: Slow Search Endpoint

Prompt:

```text
Search API supports many optional filters and p99 is bad.
```

Answer:

- inspect query plan
- avoid unindexed dynamic ORs
- use Specifications/Querydsl/explicit SQL carefully
- DTO projection
- pagination/cursor for deep pages
- indexes matching filter/sort
- maybe search read model

---

## 3. JPA And Transaction Scenarios

### Scenario 4: Double Booking

Prompt:

```text
Two users book the last room at the same time.
```

Answer:

- protect invariant in DB
- unique constraint or row-level lock/optimistic version
- service-layer transaction
- no check-then-insert race
- test with concurrent integration test if needed

---

### Scenario 5: Transaction Not Rolling Back

Prompt:

```text
A checked PaymentException is thrown but DB changes commit.
```

Answer:

- Spring rolls back RuntimeException by default
- add `rollbackFor`
- ensure call goes through proxy
- do not swallow exception
- keep external call outside long DB transaction if possible

---

### Scenario 6: N+1 Query

Prompt:

```text
GET /bookings returns 100 bookings and triggers 501 SQL queries.
```

Answer:

- identify with SQL logs/traces
- fetch join/entity graph/DTO projection
- pagination
- avoid serializing lazy entity graph
- test query count if important

---

## 4. Security Scenarios

### Scenario 7: 401 Spike After Key Rotation

Prompt:

```text
After IdP key rotation, many requests fail with 401.
```

Answer:

- check JWKS/kid
- cached keys
- issuer/audience
- clock skew
- resource server config
- IdP availability
- rollout overlap

---

### Scenario 8: Tenant Data Leak

Prompt:

```text
Tenant A sees Tenant B booking data.
```

Answer:

- verify tenant claim
- service authorization check
- DB tenant filter
- cache key includes tenant
- search index tenant filter
- audit affected data
- add tests

---

### Scenario 9: Gateway Auth Bypass

Prompt:

```text
Internal caller bypasses gateway and calls Booking Service directly.
```

Answer:

- service must validate token/service identity
- method/domain authorization
- network policy/service mesh if available
- audit sensitive action

---

## 5. Testing Scenarios

### Scenario 10: Slow Test Suite

Prompt:

```text
Every test uses @SpringBootTest and PR builds take 45 minutes.
```

Answer:

- move business logic to unit tests
- controller slices with `@WebMvcTest`
- repository slices with `@DataJpaTest`
- limited full integration tests
- Testcontainers for real infra behavior
- tag slow tests

---

### Scenario 11: Provider Breaks Consumer

Prompt:

```text
Payment API removes a response field and Booking breaks at runtime.
```

Answer:

- contract test missing
- OpenAPI/Pact compatibility gate
- additive changes first
- deprecation window
- canary rollout

---

### Scenario 12: Migration Breaks Rollback

Prompt:

```text
DB column is dropped and old app version cannot run during rollback.
```

Answer:

- expand-contract migration missing
- add first, backfill, deploy readers, remove later
- test old/new app compatibility

---

## 6. Cache Async Scheduling Scenarios

### Scenario 13: Wrong Cache Result

Prompt:

```text
Users see prices for wrong currency or tenant.
```

Answer:

- cache key missing currency/tenant
- fix key
- evict polluted entries
- add tests/metrics

---

### Scenario 14: Cache Stampede

Prompt:

```text
At 9 AM, hotel cache expires and DB traffic spikes.
```

Answer:

- TTL jitter
- request coalescing/per-key lock
- refresh ahead
- stale-while-revalidate
- protect DB with rate limit

---

### Scenario 15: Scheduled Job Runs Five Times

Prompt:

```text
After scaling to five pods, nightly cleanup runs five times.
```

Answer:

- `@Scheduled` runs per instance
- use ShedLock/Quartz/Kubernetes CronJob
- make job idempotent
- monitor job result

---

## 7. REST Client / WebFlux / Resilience Scenarios

### Scenario 16: Payment Client Timeout

Prompt:

```text
Payment provider is slow and checkout threads pile up.
```

Answer:

- set connect/read/response timeouts
- bounded retry with backoff only if idempotent
- circuit breaker
- bulkhead
- fallback/unknown payment state
- metrics/traces

---

### Scenario 17: Blocking In WebFlux

Prompt:

```text
A WebFlux endpoint calls blocking JPA repository and event loop stalls.
```

Answer:

- JPA is blocking
- use MVC/virtual threads or schedule blocking on boundedElastic carefully
- true reactive path needs R2DBC/non-blocking clients
- monitor event loop blocking

---

### Scenario 18: Retry Storm

Prompt:

```text
Resilience4j retry config causes downstream traffic to triple.
```

Answer:

- retry budget
- backoff/jitter
- circuit breaker
- do not retry non-idempotent calls
- coordinate mesh/app retries

---

## 8. Messaging / Batch Scenarios

### Scenario 19: Kafka Lag Rising

Prompt:

```text
BookingConfirmed consumer lag keeps growing.
```

Answer:

- producer vs consumer rate
- handler latency/error
- partitions vs concurrency
- hot key
- rebalance frequency
- DB/downstream bottleneck
- DLT volume

---

### Scenario 20: Duplicate Loyalty Points

Prompt:

```text
BookingConfirmed is consumed twice and points are awarded twice.
```

Answer:

- idempotent consumer missing
- processed_event table
- ledger unique key
- offset after successful transaction

---

### Scenario 21: Batch Job Fails Midway

Prompt:

```text
Nightly settlement job fails after processing 60 percent.
```

Answer:

- check JobRepository metadata
- restart from checkpoint
- idempotent writer
- skip/retry policy
- reconcile output counts

---

## 9. Runtime / Production Debugging Scenarios

### Scenario 22: Hikari Exhaustion

Prompt:

```text
API p99 spikes and Hikari pending threads are high.
```

Answer:

- check active/pending/timeouts
- slow queries/locks/long transactions
- connection leaks
- DB saturation
- do not blindly raise pool size

---

### Scenario 23: OOMKilled

Prompt:

```text
Pods restart with OOMKilled after deploy.
```

Answer:

- container memory limit
- heap/non-heap/native/thread memory
- cache growth
- high-cardinality metrics
- heap dump/JFR
- rollback if impact active

---

### Scenario 24: CPU Throttling

Prompt:

```text
p99 latency rises but average CPU looks normal.
```

Answer:

- inspect throttling metrics
- CPU limits
- JFR/thread dump
- GC vs app CPU
- tune resources/load test

---

## 10. Modern Boot Scenarios

### Scenario 25: Boot 2 To 3 Migration

Prompt:

```text
Upgrade service from Boot 2 to Boot 3.
```

Answer:

- Java 17
- Jakarta namespace
- dependency compatibility
- security changes
- tests/migrations
- canary rollout

---

### Scenario 26: Virtual Threads Decision

Prompt:

```text
High-concurrency blocking MVC service. Use WebFlux or virtual threads?
```

Answer:

- ask workload and stack
- virtual threads fit blocking I/O simplicity
- WebFlux fits non-blocking end-to-end
- DB pool/downstream limits remain

---

### Scenario 27: Native Image Decision

Prompt:

```text
Should this Spring Boot service use GraalVM native image?
```

Answer:

- startup/memory goals
- build complexity
- reflection/proxy compatibility
- test native image
- peak throughput trade-off

---

## 11. Capstone Scenario

Prompt:

```text
Design and implement a Spring Boot hotel booking service with REST API, JPA, security,
Testcontainers, outbox/Kafka, cache, observability, Docker/Kubernetes runtime, and safe rollout.
```

Must include:

- layered architecture
- DTO/entity separation
- ProblemDetail errors
- transaction boundaries
- DB constraints/locks
- JWT resource server + method security
- Testcontainers + WireMock/Pact where needed
- outbox/idempotent consumer
- cache key/invalidation
- Actuator/Micrometer/OpenTelemetry
- graceful shutdown and probes
- canary/rollback
