# Spring Boot Interview Scoring Rubrics

> Track: Spring Boot Interview Track - Practice Upgrade  
> Goal: measure readiness honestly for Spring Boot interviews.

Use this after mock interviews and scenario drills.

---

## 1. Score Scale

| Score | Meaning | Signal |
|---:|---|---|
| 1 | Fragile | memorized words, little runtime understanding |
| 2 | Basic | knows annotations but misses trade-offs/failures |
| 3 | Solid | can build normal features and test them |
| 4 | Senior | designs for correctness, reliability, observability |
| 5 | MAANG-ready | handles ambiguity, scale, failure, and production debugging |

Passing targets:

- Junior/backend starter: mostly 3s.
- Mid-level: 3.5 average, no 1s.
- Senior: 4 average, no major gaps in transactions/security/testing/runtime.
- MAANG senior: 4.3+ average with strong scenario/debugging depth.

---

## 2. Core Spring Boot Fundamentals

| Score | Evidence |
|---:|---|
| 1 | says Boot is only "less boilerplate" |
| 2 | explains annotations but not startup/auto-config |
| 3 | explains IoC, DI, starters, embedded server, profiles, and wrapper-based setup |
| 4 | debugs auto-config/bean conflicts using reports and conditions |
| 5 | explains BeanDefinition, post-processors, proxy behavior, AOT impact |

Must-have topics:

- JDK, Initializr, Maven/Gradle wrapper
- `@SpringBootApplication`
- auto-configuration
- bean lifecycle
- profiles/config
- actuator basics
- proxy limitations

---

## 3. REST API Design

| Score | Evidence |
|---:|---|
| 1 | puts logic in controller and exposes entities |
| 2 | basic CRUD with weak validation/errors |
| 3 | DTOs, validation, service boundaries, status codes |
| 4 | ProblemDetail, idempotency, pagination, OpenAPI, ownership checks |
| 5 | handles versioning, compatibility, large-scale API evolution |

Must-have topics:

- DTO/entity separation
- validation
- consistent errors
- idempotency
- pagination
- API contract testing

---

## 4. JPA Transactions And Data Correctness

| Score | Evidence |
|---:|---|
| 1 | treats JPA as magic repository calls |
| 2 | knows `@Transactional` but misses flush/commit/proxy traps |
| 3 | handles common CRUD, lazy loading, N+1, transaction placement |
| 4 | protects invariants with DB constraints/locks and tests concurrency |
| 5 | explains isolation, pool exhaustion, query plans, rollout-safe migrations |

Must-have topics:

- persistence context
- dirty checking
- flush vs commit
- N+1 fixes
- optimistic/pessimistic locking
- Hikari diagnostics

---

## 5. Security

| Score | Evidence |
|---:|---|
| 1 | only knows `permitAll`/`authenticated` |
| 2 | can configure basic JWT but misses authorization details |
| 3 | resource server, scopes/roles, 401/403, method security, basic CSRF/CORS |
| 4 | tenant isolation, BFF/session security, audit, service-to-service auth, key rotation debugging |
| 5 | designs zero-trust service auth, browser security, and domain authorization under scale |

Must-have topics:

- SecurityFilterChain
- JWT/JWKS validation
- scopes/authorities
- method security
- BFF, HttpOnly cookies, SameSite, CSRF/CORS
- OAuth2 login vs resource server
- tenant isolation

---

## 5b. Data Access Beyond JPA

| Score | Evidence |
|---:|---|
| 1 | forces every data problem through JPA |
| 2 | knows JDBC/Redis names but cannot choose intentionally |
| 3 | chooses JPA, JdbcClient, Redis, or search for common cases |
| 4 | explains jOOQ, R2DBC, NoSQL/search projections, consistency, and tests |
| 5 | designs polyglot persistence with clear invariants, ownership, observability, and failure modes |

Must-have topics:

- JPA default vs explicit SQL
- JdbcClient and jOOQ use cases
- R2DBC end-to-end non-blocking constraint
- Redis cache vs source of truth
- Elasticsearch/OpenSearch projection
- Testcontainers with real engines

---

## 6. Testing And Quality Gates

| Score | Evidence |
|---:|---|
| 1 | only writes happy-path unit tests |
| 2 | overuses `@SpringBootTest` |
| 3 | uses unit/slice/integration tests appropriately |
| 4 | adds Testcontainers, WireMock, migration tests, contracts, CI gates |
| 5 | designs fast trustworthy release gates for multi-service evolution |

Must-have topics:

- MockMvc/WebTestClient/TestRestTemplate/REST Assured
- Testcontainers
- WireMock
- Pact/OpenAPI compatibility
- Flyway/Liquibase tests
- ArchUnit/Spring Modulith checks

---

## 7. Cache Async Scheduling Events

| Score | Evidence |
|---:|---|
| 1 | uses annotations without knowing proxy/runtime behavior |
| 2 | basic cache/async/scheduled setup but unsafe in Kubernetes |
| 3 | correct keys, executor config, basic scheduled jobs |
| 4 | invalidation, stampede control, context propagation, distributed lock |
| 5 | designs operationally safe async/cache/job systems with metrics and failure recovery |

Must-have topics:

- Caffeine vs Redis
- cache key/invalidation
- stampede
- executor sizing/rejection
- context propagation
- ShedLock/Quartz/CronJob

---

## 8. REST Clients Resilience WebFlux

| Score | Evidence |
|---:|---|
| 1 | calls downstream without timeouts |
| 2 | adds retry but no idempotency/backoff thinking |
| 3 | configures timeout, retry, error mapping, client tests |
| 4 | circuit breaker, bulkhead, rate limit, trace propagation, fallback design |
| 5 | avoids retry storms and chooses MVC/WebFlux/virtual threads based on workload |

Must-have topics:

- RestClient/WebClient
- timeouts
- retries/backoff/jitter
- circuit breaker
- bulkhead
- WebFlux vs MVC/virtual threads

---

## 9. Messaging And Batch

| Score | Evidence |
|---:|---|
| 1 | thinks Kafka gives exactly-once business effects automatically |
| 2 | basic listener but weak retry/DLT/idempotency |
| 3 | listener containers, ack modes, DLT, basic Batch jobs |
| 4 | outbox, idempotent consumer, schema evolution, lag debugging, restartable Batch |
| 5 | designs event/batch flows under replay, poison messages, schema changes, and high throughput |

Must-have topics:

- topic/partition/consumer group
- ack modes
- DLT
- idempotent consumer
- outbox
- Spring Batch restartability

---

## 10. Observability Runtime Production

| Score | Evidence |
|---:|---|
| 1 | says "check logs" only |
| 2 | knows Actuator but weak metric/debugging flow |
| 3 | uses logs/metrics/traces, probes, basic dashboards |
| 4 | debugs Hikari, CPU, memory, GC, startup, downstream failures |
| 5 | runs incidents with mitigation-first thinking and strong postmortems |

Must-have topics:

- Actuator endpoint safety
- Micrometer/OpenTelemetry
- liveness/readiness
- Hikari metrics
- JVM/JFR/thread/heap diagnostics
- canary/rollback

---

## 11. Supply Chain, Protocols, And Modular Architecture

| Score | Evidence |
|---:|---|
| 1 | ignores dependency/image/security release risks and protocol trade-offs |
| 2 | knows SBOM/GraphQL/gRPC terms but cannot apply them |
| 3 | explains SBOM, scans, REST vs GraphQL vs gRPC basics, and simple module boundaries |
| 4 | designs CI release gates, protocol choices, outbox-backed events, and Modulith boundary tests |
| 5 | handles CVE response, artifact provenance, protocol operations, and service extraction decisions |

Must-have topics:

- SBOM and CVE response
- dependency and image scanning
- artifact provenance/signing awareness
- REST, GraphQL, gRPC, Pulsar/Kafka, SSE/WebSocket trade-offs
- Spring Integration awareness
- Spring Modulith and module boundary tests

---

## 12. Modern Spring Boot

| Score | Evidence |
|---:|---|
| 1 | unaware of Boot 3/Jakarta changes |
| 2 | knows Boot 3 exists but cannot plan upgrade |
| 3 | explains Java 17/Jakarta and dependency migration |
| 4 | evaluates Boot 4.1 baseline, AOT/native image/virtual threads/WebFlux trade-offs |
| 5 | designs Boot 4.1 upgrade/modernization path with tests, performance evidence, SBOM/observability checks, and rollout safety |

Must-have topics:

- Boot 3 migration
- Boot 4.1 readiness
- Java/Framework/Maven/Gradle/Servlet/GraalVM baselines
- AOT/native image
- virtual threads
- WebFlux/R2DBC trade-offs
- canary and rollback

---

## 13. Self-Assessment Sheet

Fill after each mock:

| Area | Score | Evidence | Red Gap | Next Drill |
|---|---:|---|---|---|
| Setup/Build Reproducibility |  |  |  |  |
| Core Boot |  |  |  |  |
| REST APIs |  |  |  |  |
| JPA/Transactions |  |  |  |  |
| Data Access Beyond JPA |  |  |  |  |
| Security |  |  |  |  |
| Testing |  |  |  |  |
| Cache/Async/Scheduling |  |  |  |  |
| Clients/Resilience/WebFlux |  |  |  |  |
| Kafka/Batch |  |  |  |  |
| Runtime/Debugging |  |  |  |  |
| Supply Chain/Protocols/Modulith |  |  |  |  |
| Modern Boot |  |  |  |  |
| Capstone Lifecycle |  |  |  |  |

---

## 14. MAANG Readiness Gate

You are interview-ready when:

1. Average score is 4.3 or higher.
2. No category is below 4.
3. You can solve the booking capstone without notes.
4. You can debug p99, Hikari, CPU, memory, and security incidents aloud.
5. You can defend trade-offs between MVC, WebFlux, virtual threads, Kafka/Pulsar, Batch, JPA, JdbcClient/jOOQ/R2DBC, and native image.
6. You can explain BFF/session security, CSRF, CORS, and token storage clearly.
7. You can explain SBOM, dependency/image scanning, and CVE patch rollout.
8. You can choose REST/GraphQL/gRPC/SSE/WebSocket intentionally.
9. You can explain how tests and quality gates prevent production regressions.
