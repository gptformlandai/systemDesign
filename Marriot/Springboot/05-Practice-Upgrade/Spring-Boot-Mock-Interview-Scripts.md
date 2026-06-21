# Spring Boot Mock Interview Scripts

> Track: Spring Boot Interview Track - Practice Upgrade  
> Goal: simulate real Spring Boot interview rounds from junior to MAANG senior.

Use these scripts aloud. Time each round.

---

## 1. Universal Answer Shape

Use this structure for most answers:

```text
Concept -> Spring mechanism -> production concern -> testing/observability -> trade-off
```

For debugging:

```text
Symptom -> blast radius -> recent change -> metrics/traces/logs -> hypothesis -> mitigation -> prevention
```

---

## 2. Round 1: Starter Spring Boot Fundamentals

Time: 30 minutes

### Questions

1. What problem does Spring Boot solve?
2. Explain `@SpringBootApplication`.
3. What is dependency injection?
4. Constructor injection vs field injection?
5. What is auto-configuration?
6. What is a starter dependency?
7. What is the embedded server?
8. How do profiles work?
9. What is Actuator?
10. How would you create a basic REST endpoint?

### Strong Signal

Candidate explains runtime behavior, not just annotations.

### Weak Signal

Candidate only says "Spring Boot reduces boilerplate" and cannot explain how.

---

## 3. Round 2: REST API And Validation

Time: 45 minutes

### Prompt

```text
Design a create-booking API in Spring Boot.
```

### Interviewer Follow-Ups

1. What should the request DTO look like?
2. Where do you put validation?
3. How do you handle validation errors?
4. What status codes do you return?
5. Why not return entity directly?
6. How do you add pagination to list bookings?
7. How do you make create idempotent?
8. How do you document the API?
9. How do you test controller behavior?
10. How do you keep errors consistent across services?

### Excellent Answer Includes

- DTOs
- `@Valid`
- ProblemDetail
- idempotency key
- `@RestControllerAdvice`
- OpenAPI
- MockMvc/WebTestClient tests

---

## 4. Round 3: JPA Transactions Deep Dive

Time: 60 minutes

### Prompt

```text
Two users try to reserve the last available room at the same time.
```

### Interviewer Follow-Ups

1. Where should `@Transactional` be placed?
2. Why is DB constraint/locking needed?
3. Optimistic vs pessimistic locking?
4. What isolation level would you use?
5. What is flush vs commit?
6. What if payment call happens inside transaction?
7. Why can `@Transactional` fail due to self-invocation?
8. How do you test this behavior?
9. How do you debug connection pool exhaustion?
10. How do you avoid N+1 queries in the booking list?

### Excellent Answer Includes

- service transaction boundary
- invariant protected by DB
- lock/unique constraint
- external side effect separated or made reliable
- Testcontainers/concurrency test
- Hikari diagnostics awareness

---

## 5. Round 4: Spring Security

Time: 45 minutes

### Prompt

```text
Secure the Booking API for customers, admins, and service-to-service calls.
```

### Interviewer Follow-Ups

1. Resource server or session login?
2. How do you validate JWT?
3. What claims matter?
4. How do you map scopes/roles?
5. 401 vs 403?
6. Where should domain authorization live?
7. How do you enforce tenant isolation?
8. What about CORS and CSRF?
9. What happens during key rotation?
10. How do you audit sensitive actions?

### Excellent Answer Includes

- OAuth2 Resource Server
- issuer/audience/signature/expiry validation
- method security
- tenant-aware repository/service filters
- audit logging
- key rotation/JWKS troubleshooting

---

## 6. Round 5: Testing Strategy

Time: 45 minutes

### Prompt

```text
Create a quality strategy for this Spring Boot service before it goes to production.
```

### Interviewer Follow-Ups

1. What belongs in unit tests?
2. When do you use `@WebMvcTest`?
3. When do you use `@DataJpaTest`?
4. What does Testcontainers prove?
5. When do you use WireMock?
6. When do you use Pact?
7. How do you test database migrations?
8. What should CI fail on?
9. What is ArchUnit useful for?
10. How do you keep tests fast?

### Excellent Answer Includes

- test pyramid
- contract compatibility
- migration testing
- CI gates
- focused E2E tests

---

## 7. Round 6: Cache Async Scheduling

Time: 45 minutes

### Prompt

```text
The service uses cache, async notification, and scheduled cleanup. Review it for production.
```

### Interviewer Follow-Ups

1. Caffeine or Redis?
2. What goes into cache key?
3. How do you invalidate?
4. What is cache stampede?
5. How do you size async executor?
6. What happens to transaction/security/MDC context in async code?
7. What happens to scheduled jobs with multiple pods?
8. ShedLock vs Quartz vs CronJob?
9. What metrics do you add?
10. What are common proxy traps?

### Excellent Answer Includes

- cache key correctness
- invalidation plan
- executor sizing/rejection
- context propagation
- distributed job lock/idempotency

---

## 8. Round 7: REST Clients Resilience WebFlux

Time: 45 minutes

### Prompt

```text
Checkout calls Payment, Fraud, and Loyalty services. Make it resilient.
```

### Interviewer Follow-Ups

1. Which client API do you choose?
2. What timeouts are mandatory?
3. When do you retry?
4. What is circuit breaker?
5. What is bulkhead?
6. How do you avoid retry storm?
7. How do you handle partial failure?
8. When is WebFlux appropriate?
9. Virtual threads vs WebFlux?
10. How do you test client behavior?

### Excellent Answer Includes

- connect/read/response timeouts
- bounded retries with backoff/jitter
- idempotency awareness
- circuit breaker and bulkhead
- traces/correlation IDs
- WireMock tests

---

## 9. Round 8: Kafka And Batch

Time: 60 minutes

### Prompt

```text
After booking confirmation, publish event and process loyalty points reliably.
```

### Interviewer Follow-Ups

1. Why not publish Kafka event before DB commit?
2. Explain outbox.
3. How do you make consumer idempotent?
4. What ack mode do you choose?
5. How do retries and DLT work?
6. What causes consumer lag?
7. How do partitions affect concurrency?
8. How do you handle schema evolution?
9. When would you use Spring Batch instead of Kafka?
10. How do you make a batch job restartable?

### Excellent Answer Includes

- transactional outbox
- idempotent consumer
- DLT and retry policy
- lag diagnostics
- schema compatibility
- batch restartability

---

## 10. Round 9: Production Debugging

Time: 60 minutes

### Prompt

```text
Booking API p99 jumps to 5 seconds after deployment.
```

### Interviewer Follow-Ups

1. What do you check in first 10 minutes?
2. How do you find blast radius?
3. What metrics matter?
4. What traces matter?
5. How do you inspect Hikari?
6. How do you inspect JVM CPU/memory?
7. What if pods are CPU throttled?
8. What if DB is slow?
9. What if downstream is slow?
10. When do you rollback?

### Excellent Answer Includes

- version/deploy correlation
- RED/USE metrics
- traces and exemplar paths
- DB pool/query plan
- JFR/thread dump/heap dump when needed
- mitigation before root cause perfection

---

## 11. Round 10: Modern Spring Boot Platinum

Time: 45 minutes

### Prompt

```text
How would you modernize this service for Spring Boot 3/4, AOT, native image, and virtual threads?
```

### Interviewer Follow-Ups

1. What changed in Boot 3?
2. What is Jakarta migration?
3. What is AOT?
4. What breaks native image?
5. When is native image worth it?
6. What do virtual threads improve?
7. What remains bottlenecked?
8. WebFlux vs virtual threads?
9. How do you validate an upgrade?
10. How do you roll it out safely?

### Excellent Answer Includes

- Java 17+/Jakarta awareness
- dependency compatibility
- native-image trade-offs
- virtual thread limits
- canary and rollback plan

---

## 12. Full MAANG Loop

Run these back-to-back:

1. 15 min fundamentals lightning.
2. 45 min booking API design.
3. 45 min transaction/JPA drill.
4. 45 min security drill.
5. 45 min testing and quality gates.
6. 45 min resilience/Kafka drill.
7. 45 min production debugging.

Pass criteria:

- no annotation-only explanations
- always ties feature to runtime behavior
- mentions failure modes
- includes tests/observability
- can defend trade-offs
