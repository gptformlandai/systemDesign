# Spring Boot Design Coding Mini Labs

> Track: Spring Boot Interview Track - Practice Upgrade  
> Goal: turn Spring Boot concepts into small buildable design/coding exercises.

Each lab should take 45-120 minutes.

---

## 1. Lab Output Rules

For every lab, produce:

1. Short design notes.
2. Code/config sketch.
3. Tests to prove behavior.
4. Failure mode notes.
5. 60-second interview explanation.

---

## 2. Lab 1: Booking REST API Skeleton

Build/sketch:

- `BookingController`
- request/response DTOs
- `BookingService`
- `BookingRepository`
- validation
- `@RestControllerAdvice`
- ProblemDetail errors

Test:

- valid create returns 201
- invalid request returns validation error
- not found returns stable error code

Interview target:

```text
Explain controller/service/repository responsibilities and DTO/entity separation.
```

---

## 3. Lab 2: Idempotent Create Booking

Build/sketch:

- idempotency table
- unique key by user + idempotency key
- request hash check
- return previous response for duplicate

Test:

- same key same payload returns same result
- same key different payload returns conflict
- concurrent same-key requests create one booking

---

## 4. Lab 3: Prevent Double Booking

Build/sketch:

- availability table
- optimistic version or pessimistic lock
- service transaction
- database constraint

Test:

- two concurrent booking attempts for last room
- one succeeds, one fails cleanly

Explain:

```text
Application checks are not enough; the invariant must be protected by DB transaction/constraint.
```

---

## 5. Lab 4: ProblemDetail Error Contract

Build/sketch:

- error code enum
- ProblemDetail handler
- validation error field list
- correlation id property

Test:

- validation error shape
- conflict error shape
- unauthorized/forbidden behavior if security included

---

## 6. Lab 5: Resource Server Security

Build/sketch:

- JWT resource server config
- method security
- booking ownership check
- tenant-aware read

Test:

- no token -> 401
- wrong scope -> 403
- user cannot access another user's booking
- tenant A cannot read tenant B data

---

## 7. Lab 6: Test Pyramid Refactor

Given:

```text
A codebase where every test uses @SpringBootTest.
```

Refactor plan:

- unit tests for business rules
- `@WebMvcTest` for controllers
- `@DataJpaTest` for repositories
- Testcontainers integration test for critical flow
- ArchUnit layer rule

Deliverable:

```text
A table mapping each test to the smallest useful test scope.
```

---

## 8. Lab 7: WireMock Payment Client

Build/sketch:

- `PaymentClient` using RestClient/WebClient
- timeouts
- error mapping
- correlation header propagation

Test with WireMock:

- success
- 400 non-retryable
- 500 retryable
- timeout maps to domain exception

---

## 9. Lab 8: Pact / OpenAPI Contract Gate

Build/sketch:

- one consumer expectation for Payment API
- provider verification idea
- OpenAPI compatibility comparison step

Deliverable:

```text
CI gate design showing where contract compatibility fails the build.
```

---

## 10. Lab 9: Flyway Expand-Contract Migration

Build/sketch:

- migration V1 old table
- migration V2 add nullable column
- app writes old+new
- backfill
- migration V3 make new column required or remove old later

Test:

- migrations apply to clean DB
- old/new app compatibility plan described

---

## 11. Lab 10: Cache Key And Stampede Protection

Build/sketch:

- hotel price cache
- key includes hotel, dates, guests, currency, tenant
- TTL with jitter
- per-key lock/request coalescing idea

Test:

- different currency gets different cache entry
- eviction after price update
- stampede mitigation design explained

---

## 12. Lab 11: Async Executor

Build/sketch:

- named `ThreadPoolTaskExecutor`
- queue capacity
- rejection policy
- MDC/correlation propagation with TaskDecorator
- async exception handler

Test:

- async task uses named executor
- rejection behavior documented
- correlation id appears in async log context

---

## 13. Lab 12: Distributed Scheduled Job

Build/sketch:

- scheduled cleanup job
- ShedLock or Quartz plan
- idempotent job behavior
- metrics for success/failure/duration

Test:

- duplicate run does not corrupt data
- lock prevents two instances from doing same work

---

## 14. Lab 13: Outbox And Kafka Producer

Build/sketch:

- outbox table
- transactional write
- relay
- KafkaTemplate send
- retry and published status

Test:

- booking commit creates outbox row
- relay publishes row
- failed publish retries

---

## 15. Lab 14: Idempotent Kafka Consumer

Build/sketch:

- `@KafkaListener`
- processed_event table
- business side effect
- DLT strategy

Test:

- duplicate event is skipped
- poison event goes to DLT after retries

---

## 16. Lab 15: Spring Batch Settlement Job

Build/sketch:

- Job/Step
- reader/processor/writer
- chunk size
- skip/retry policy
- restartability

Test:

- job can restart after failure
- writer is idempotent
- skipped records audited

---

## 17. Lab 16: Observability Dashboard

Design dashboard panels:

- HTTP rate/errors/duration
- JVM heap/non-heap
- GC pause
- Hikari active/pending/timeouts
- cache hit/miss
- executor queue/rejected
- Kafka lag/DLT
- version annotations

Deliverable:

```text
Dashboard sketch plus alert thresholds for booking p99 and Hikari exhaustion.
```

---

## 18. Lab 17: Kubernetes Runtime Config

Build/sketch:

- Docker/buildpack image plan
- Actuator readiness/liveness config
- graceful shutdown config
- resource requests/limits
- env/config/secrets

Explain:

```text
Why liveness should not fail just because DB has a temporary outage.
```

---

## 19. Lab 18: Boot 3 Migration Plan

Deliverable:

- Java 17 baseline checklist
- Jakarta package migration checklist
- dependency/starter compatibility
- security behavior retest
- Testcontainers suite
- canary rollout

---

## 20. Lab 19: Virtual Threads Experiment

Design experiment:

- MVC blocking endpoint
- platform threads baseline
- virtual threads run
- DB pool fixed
- compare p95/p99, throughput, pool waits, CPU

Expected learning:

```text
Virtual threads improve waiting-thread scalability but DB pool remains a bottleneck.
```

---

## 21. Lab 20: Production Debugging Drill

Given symptom:

```text
GET /bookings p99 jumps from 250 ms to 5 seconds.
```

Produce runbook:

- check version/deploy
- traces
- DB query/pool metrics
- CPU throttling
- GC
- downstream latency
- mitigation
- prevention

---

## 22. Completion Gate

You completed the labs when you can:

1. Build/sketch a complete booking API.
2. Prove transaction/idempotency behavior with tests.
3. Design quality gates around contracts and migrations.
4. Explain cache/async/scheduling operational risks.
5. Design Kafka outbox/consumer reliability.
6. Debug runtime incidents with metrics and JVM evidence.
