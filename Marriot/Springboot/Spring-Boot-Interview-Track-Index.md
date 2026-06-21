# Spring Boot Interview Track Index

Target: starter, intermediate, senior, and MAANG-level Java backend preparation.

This folder is organized as a complete Spring Boot learning and interview track. The goal is
not to memorize annotations. The goal is to explain how Spring Boot applications are built,
secured, tested, deployed, observed, debugged, and evolved in production.

Current structure:

- 28 topic/practice sheets plus this root index
- 5 learning layers from starter to practice upgrade
- coverage for fundamentals, production engineering, distributed workloads, MAANG scenarios, and active recall

---

## 1. Recommended Study Order

| Order | Document | Why This Order |
|---:|---|---|
| 1 | `01-Starter-Path/Spring-Boot-Core-Interview-Master-Sheet.md` | Learn IoC, DI, beans, MVC, AOP, transactions, validation, and request flow |
| 2 | `01-Starter-Path/Spring-Boot-REST-API-Design-Validation-Error-Handling-Gold-Sheet.md` | Turn core Spring knowledge into clean API design, DTOs, ProblemDetail, pagination, idempotency, and OpenAPI |
| 3 | `01-Starter-Path/Spring-Data-JPA-Hibernate-Interview-Master-Sheet.md` | Learn persistence context, mappings, lazy loading, N+1, transactions, locking, and performance |
| 4 | `01-Starter-Path/Spring-Security-JWT-OAuth-Interview-Sheet.md` | Learn Spring Security foundations, JWT, OAuth2, resource server basics, CORS, CSRF, and method security |
| 5 | `01-Starter-Path/Spring-Boot-Testing-Testcontainers-Migrations-Interview-Master-Sheet.md` | Learn unit, slice, integration tests, Testcontainers, Flyway/Liquibase, and migration confidence |
| 6 | `02-Intermediate-Production-Features/Spring-Boot-API-Contracts-Testing-Quality-Gates-Gold-Sheet.md` | Add contract testing, WireMock, Pact/OpenAPI compatibility, ArchUnit, Modulith, and CI quality gates |
| 7 | `02-Intermediate-Production-Features/Spring-Boot-Cache-Async-Scheduling-Events-Startup-Interview-Master-Sheet.md` | Learn caching, async execution, scheduling, Spring events, and startup lifecycle |
| 8 | `02-Intermediate-Production-Features/Spring-Boot-Cache-Async-Scheduling-Implementation-Deep-Dive-Gold-Sheet.md` | Add production depth for cache keys, stampede control, executor sizing, context propagation, and distributed jobs |
| 9 | `02-Intermediate-Production-Features/Spring-Boot-REST-Clients-WebFlux-Resilience-Interview-Master-Sheet.md` | Learn RestClient/WebClient, timeouts, retries, circuit breakers, WebFlux, and blocking traps |
| 10 | `02-Intermediate-Production-Features/Spring-Boot-Observability-Actuator-Micrometer-Interview-Master-Sheet.md` | Learn Actuator, health, metrics, logs, traces, correlation IDs, SLOs, and alerting basics |
| 11 | `03-Senior-Distributed-Workloads/Spring-Batch-Interview-Master-Sheet.md` | Learn Job/Step, chunk processing, restartability, skip/retry, partitioning, and batch monitoring |
| 12 | `03-Senior-Distributed-Workloads/Spring-Boot-Messaging-Kafka-RabbitMQ-Interview-Master-Sheet.md` | Learn Kafka/RabbitMQ fundamentals, ordering, retries, DLQ/DLT, idempotency, and outbox |
| 13 | `03-Senior-Distributed-Workloads/Spring-Boot-Kafka-Batch-Advanced-Operations-Gold-Sheet.md` | Add senior Kafka/Batch operations: listener containers, ack modes, lag debugging, schema evolution, and restartable jobs |
| 14 | `03-Senior-Distributed-Workloads/Spring-Security-Advanced-Resource-Server-Authorization-Server-Multitenancy-Gold-Sheet.md` | Add advanced resource server, authorization server awareness, service-to-service auth, tenant isolation, and auditing |
| 15 | `03-Senior-Distributed-Workloads/Spring-Cloud-Microservices-Interview-Master-Sheet.md` | Learn Spring Cloud Config, Gateway, OpenFeign, discovery, load balancing, and Kubernetes alternatives |
| 16 | `03-Senior-Distributed-Workloads/Spring-Boot-Production-Runtime-Docker-Kubernetes-JVM-Gold-Sheet.md` | Learn Docker/buildpacks, JVM containers, probes, graceful shutdown, resource limits, config/secrets, and runtime diagnostics |
| 17 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Internals-AutoConfiguration-Proxies-Transactions-Platinum-Sheet.md` | Learn Boot internals, conditions, proxies, transaction traps, and diagnostics |
| 18 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Advanced-Internals-Proxies-Transactions-Diagnostics-Platinum-Sheet.md` | Add deeper BeanDefinition, post-processor, advisor ordering, propagation, isolation, lazy-loading, and transaction debugging depth |
| 19 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Production-Debugging-Case-Studies-Platinum-Sheet.md` | Learn production case studies around startup, DB, memory, cache, security, and slow APIs |
| 20 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Production-Debugging-Runbooks-JFR-Hikari-Platinum-Sheet.md` | Add incident runbooks for first 10 minutes, Hikari, CPU, memory, thread starvation, GC, JFR, and postmortems |
| 21 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Modern-3-4-AOT-GraalVM-Virtual-Threads-Platinum-Sheet.md` | Learn Boot 3/4, Jakarta migration, AOT, GraalVM native image, virtual threads, WebFlux, R2DBC, and Modulith trade-offs |
| 22 | `04-MAANG-Platinum-Scenarios/Spring-Boot-MAANG-Scenario-Based-Architecture-Questions-Platinum-Sheet.md` | Practice full scenario answers for booking, payment, cache, batch, security, and production design rounds |
| 23 | `05-Practice-Upgrade/Spring-Boot-Active-Recall-Question-Bank.md` | Convert notes into retrieval memory with question banks across every major topic |
| 24 | `05-Practice-Upgrade/Spring-Boot-Scenario-Drill-Bank.md` | Practice realistic design, debugging, security, testing, Kafka, Batch, and runtime scenarios |
| 25 | `05-Practice-Upgrade/Spring-Boot-Design-Coding-Mini-Labs.md` | Build small labs for APIs, idempotency, transactions, security, Testcontainers, WireMock, cache, async, outbox, Batch, runtime, and modernization |
| 26 | `05-Practice-Upgrade/Spring-Boot-Mock-Interview-Scripts.md` | Run simulated interview rounds from starter fundamentals to MAANG production debugging |
| 27 | `05-Practice-Upgrade/Spring-Boot-Interview-Scoring-Rubrics.md` | Score readiness by area and identify Red gaps honestly |
| 28 | `05-Practice-Upgrade/Spring-Boot-2-Week-4-Week-Mastery-Roadmaps.md` | Follow a 2-week sprint or 4-week mastery plan with daily practice loops |

---

## 2. What Each Layer Builds

### 01-Starter-Path

This layer builds implementation confidence.

You should be able to explain and build:

- Spring container, IoC, DI, beans, scopes, lifecycle
- MVC request flow, validation, filters, interceptors, exceptions
- REST APIs with DTO boundaries and consistent error contracts
- JPA/Hibernate persistence, transactions, locking, lazy loading, and N+1 fixes
- Spring Security basics, JWT/OAuth2, resource server, roles, authorities, 401/403
- testing basics, Testcontainers, Flyway/Liquibase, and migration safety

### 02-Intermediate-Production-Features

This layer builds production feature maturity.

You should be able to explain and design:

- API contract quality gates with Pact/OpenAPI, WireMock, ArchUnit, and CI checks
- cache key design, invalidation, stampede prevention, and Caffeine vs Redis trade-offs
- async executor sizing, rejection policy, exception handling, and context propagation
- scheduled jobs in Kubernetes with ShedLock, Quartz, or CronJob alternatives
- outbound HTTP clients with timeouts, retries, circuit breakers, bulkheads, and fallbacks
- WebFlux basics, blocking traps, and virtual-thread trade-offs
- Actuator, Micrometer, OpenTelemetry, logs, metrics, traces, SLOs, and alerts

### 03-Senior-Distributed-Workloads

This layer builds senior backend ownership.

You should be able to explain and operate:

- Spring Batch jobs, chunk processing, restartability, skip/retry, and partitioning
- Kafka/RabbitMQ listeners, ack modes, retries, DLT, ordering, schema evolution, and lag debugging
- outbox pattern and idempotent consumers for reliable business effects
- advanced resource server security, tenant isolation, auditing, and service-to-service auth
- Spring Cloud Gateway, Config, OpenFeign, discovery, load balancing, and Kubernetes replacements
- Docker/buildpacks, JVM containers, probes, graceful shutdown, config/secrets, CPU limits, and Hikari tuning

### 04-MAANG-Platinum-Scenarios

This layer builds interviewer-grade depth.

You should be able to explain:

- auto-configuration internals and diagnostic reports
- BeanDefinition vs bean instance
- BeanFactoryPostProcessor vs BeanPostProcessor
- AOP proxies, advisor order, self-invocation, final/private method traps
- transaction propagation, isolation, rollback, flush vs commit, and external-call traps
- production incident triage with JFR, heap dumps, thread dumps, Hikari metrics, DB plans, and traces
- Spring Boot 3/4, Jakarta migration, AOT, native image, virtual threads, WebFlux, R2DBC, and Modulith
- MAANG-style system design plus implementation answers

### 05-Practice-Upgrade

This layer turns knowledge into interview performance.

You should use it to:

- answer active recall without notes
- practice scenario drills under pressure
- build mini-labs that prove runtime behavior
- simulate interview rounds
- score readiness with rubrics
- follow 2-week or 4-week study plans

---

## 3. Level-Wise Learning Plan

### Starter Path

Read:

1. Core Spring Boot
2. REST API design, validation, and error handling
3. JPA/Hibernate
4. Spring Security basics
5. testing and migrations basics

Starter goal:

```text
I can build a Spring Boot REST API with controller, service, repository, DTOs, validation,
exception handling, database access, basic security, and tests.
```

### Intermediate Path

Add:

1. API contracts and quality gates
2. cache, async, scheduling, events, and startup lifecycle
3. REST clients, WebFlux basics, and resilience
4. observability with Actuator, Micrometer, logs, traces, and SLOs

Intermediate goal:

```text
I can build a production-like Spring Boot service with tests, migrations, caching,
background jobs, outbound calls, resilience, and useful health/metrics/logs/traces.
```

### Senior Path

Add:

1. Spring Batch
2. Kafka/RabbitMQ messaging and outbox
3. advanced security and multitenancy
4. Spring Cloud and Kubernetes-era alternatives
5. Docker/Kubernetes/JVM runtime operations
6. Boot internals, proxies, transaction diagnostics, and production debugging

Senior goal:

```text
I can design reliable services that handle async workflows, batch jobs, messaging,
downstream failure, tenant/security boundaries, deployment safety, and production debugging.
```

### MAANG-Ready Path

For every topic, practice explaining:

- definition and why it exists
- runtime behavior inside Spring
- code/config shape
- when not to use it
- common traps and production failure modes
- debugging strategy
- testing strategy
- scaling and operational trade-offs
- security and data correctness concerns

MAANG goal:

```text
I can reason like an owner: correctness first, then security, reliability, performance,
observability, operability, cost, and rollout safety.
```

---

## 4. Interview Answer Formula

Use this formula for almost every Spring Boot question:

```text
1. Define the concept in one clean line.
2. Explain why it exists.
3. Explain how it works internally or at runtime.
4. Give a small code/config example.
5. Mention common traps.
6. Mention production trade-offs.
7. Explain how to test and observe it.
8. Close with when you would or would not use it.
```

Example:

```text
@Transactional is a Spring AOP feature that runs a method inside a transaction. It works
through proxies, so the call must go through the proxy for transaction advice to apply.
The main traps are self-invocation, private/final methods, checked exception rollback,
long external calls inside transactions, and assuming flush equals commit.
```

Debugging formula:

```text
symptom -> blast radius -> recent change -> metrics/traces/logs -> hypothesis -> mitigation -> prevention
```

Design formula:

```text
requirements -> model/API -> transaction/security boundaries -> failure handling -> observability -> tests -> rollout
```

---

## 5. Final Coverage Checklist

| Area | Covered By |
|---|---|
| Core Spring container | Core sheet, internals platinum sheets |
| REST API development | Core sheet, REST API design sheet, scenario drills, labs |
| Validation and error handling | REST API design sheet, ProblemDetail labs |
| DTO/entity/API boundaries | REST API design sheet, capstone labs |
| JPA/Hibernate | JPA sheet, internals diagnostics sheets, production debugging sheets |
| Transactions and locking | JPA sheet, internals sheets, scenario drills, double-booking lab |
| Security/JWT/OAuth2 | Security sheet, advanced resource server sheet, security mock rounds |
| Multitenancy and domain auth | advanced security sheet, scenario drills, mini-labs |
| Testing | testing sheet, API contracts sheet, mini-labs, scoring rubrics |
| Testcontainers | testing sheet, labs, quality gates sheet |
| WireMock/Pact/OpenAPI compatibility | API contracts sheet, labs, mock scripts |
| Flyway/Liquibase migrations | testing sheet, API contracts sheet, migration lab |
| Caching | cache/async master sheet, implementation deep dive, labs |
| Async execution | cache/async sheets, async executor lab |
| Scheduling | cache/async sheets, ShedLock/Quartz/CronJob drills |
| Spring events | cache/async sheets, outbox comparison |
| Batch processing | Batch master sheet, Kafka/Batch advanced sheet, Batch lab |
| REST clients | REST/WebFlux/Resilience sheet, WireMock payment client lab |
| WebFlux/R2DBC | REST/WebFlux/Resilience sheet, modern Boot sheet |
| Resilience4j patterns | REST/WebFlux/Resilience sheet, scenario drills |
| Kafka | Messaging sheet, Kafka/Batch advanced operations sheet, labs |
| RabbitMQ | Messaging sheet |
| Outbox and idempotent consumers | Messaging sheet, Kafka/Batch sheet, labs |
| Actuator | Observability sheet, production runtime sheet |
| Metrics/tracing/logging | Observability sheet, debugging runbooks, dashboard lab |
| Spring Cloud | Spring Cloud sheet |
| Kubernetes runtime | production runtime sheet, runtime lab |
| JVM diagnostics | production runtime sheet, debugging runbooks |
| Boot internals | internals platinum sheets |
| Auto-configuration diagnostics | internals platinum sheets |
| Proxy/self-invocation traps | internals platinum sheets |
| Production incidents | production debugging sheets, JFR/Hikari runbooks |
| Boot 3/4 and modern Spring | modern Boot platinum sheet, modernization lab |
| Active recall and mocks | all `05-Practice-Upgrade` sheets |

---

## 6. Practice Path

After each concept layer, practice immediately.

| Stage | Practice |
|---|---|
| After Starter | answer Core, REST, JPA, Security, Testing recall questions |
| After Intermediate | run cache/async/client/observability scenario drills |
| After Senior | complete Kafka, Batch, security, runtime labs |
| After Platinum | run production debugging and modernization mock rounds |
| Before interviews | use scoring rubrics and 2-week/4-week roadmaps |

Minimum MAANG practice set:

1. 150+ active recall questions.
2. 15+ scenario drills.
3. 10+ design/coding mini-labs.
4. 3+ mock interview loops.
5. One complete capstone explanation without notes.

---

## 7. Gold Standard Audit Rubric

Use this rubric to judge whether a Spring Boot topic is interview-ready.

| Quality Bar | What It Means |
|---|---|
| Beginner clarity | Can explain the idea in simple words |
| Runtime understanding | Can explain what Spring does internally |
| Code confidence | Can write or sketch a minimal example |
| Trade-off maturity | Can say when not to use it |
| Failure awareness | Can explain what breaks in production |
| Debuggability | Can troubleshoot common incidents |
| Testing strategy | Can test the behavior properly |
| Security awareness | Can identify auth/data risks |
| Scalability awareness | Can explain limits and scaling path |
| Interview delivery | Can answer in crisp 60 to 120 second structure |

Gold rule:

```text
If a learner can explain definition, runtime flow, code/config, trade-offs, failure modes,
debugging, observability, and testing for a topic, the topic is interview-ready.
```

---

## 8. End-To-End Capstone Roadmap

Build one imaginary system while studying every sheet:

```text
Hotel Booking Platform
```

Use each area like this:

| Area | Capstone Work |
|---|---|
| Core | REST API, service layer, validation, exceptions |
| REST API design | DTOs, ProblemDetail, pagination, OpenAPI, idempotency |
| JPA | booking/payment/customer entities, constraints, locks, N+1 fixes |
| Security | JWT/OAuth2, scopes, roles, ownership checks, tenant isolation |
| Testing | unit, slice, integration, Testcontainers, contracts, migrations |
| Cache/Async | hotel/rate cache, confirmation async, distributed scheduled cleanup |
| REST clients | payment/inventory clients with timeout, retry, circuit breaker, bulkhead |
| Messaging | BookingCreated events, Kafka/Rabbit, outbox, DLT, idempotent consumer |
| Batch | nightly settlement and reconciliation with restartability |
| Observability | health, metrics, logs, traces, dashboards, alerts |
| Spring Cloud/runtime | gateway/config/discovery awareness, Kubernetes probes, graceful shutdown |
| Internals | auto-configuration, proxies, transactions, self-invocation diagnostics |
| Production Debugging | startup failures, slow APIs, Hikari, JVM, memory, CPU, security, cache |
| Modern Boot | Boot 3/4, AOT, native image, virtual threads, WebFlux/R2DBC trade-offs |

Final capstone interview prompt:

```text
Design and implement a Spring Boot hotel booking platform that supports customer booking,
payment authorization, room inventory, email notification, nightly settlement, JWT security,
observability, and production-safe deployments.
```

Strong answer must include:

- clear layered architecture
- DTO boundaries
- service transaction boundaries
- database constraints and locking
- security at URL, method, tenant, and domain level
- idempotency for retries
- outbox for reliable events
- idempotent consumers
- Testcontainers for real DB tests
- WireMock/Pact/OpenAPI quality gates
- Flyway/Liquibase expand-contract migrations
- cache invalidation and stampede strategy
- downstream timeout/retry/circuit breaker/bulkhead
- Actuator/Micrometer/OpenTelemetry/tracing
- Docker/Kubernetes readiness, liveness, graceful shutdown, and rollback safety
- Boot 3/4 modernization and virtual-thread/native-image decision

---

## 9. Final Completeness Statement

This track now covers the Spring Boot knowledge expected across:

- entry-level Java backend interviews
- intermediate Spring Boot project rounds
- senior backend production discussions
- system design plus implementation hybrid rounds
- MAANG-style depth checks around correctness, security, scale, failure, and operations

One-stop answer:

```text
Yes. This is now a one-stop Spring Boot track for MAANG-level preparation. It covers
fundamentals, REST API design, JPA/transactions, Security, testing, migrations, contracts,
cache, async, scheduling, REST clients, WebFlux, resilience, Kafka, RabbitMQ, Batch,
Spring Cloud, observability, Docker/Kubernetes/JVM runtime, Boot internals, production
debugging, Boot 3/4 modernization, AOT/native image, virtual threads, scenario drills,
mini-labs, mocks, rubrics, and 2-week/4-week mastery roadmaps.
```

Optional future add-ons only if a role explicitly needs them:

- Spring GraphQL
- gRPC with Spring Boot
- Spring Integration deep dive
- R2DBC deep dive beyond the modern Boot overview
- full runnable reference project

These are optional because the current track already covers the high-frequency Spring Boot
interview surface and the senior production ownership depth expected in serious backend rounds.
