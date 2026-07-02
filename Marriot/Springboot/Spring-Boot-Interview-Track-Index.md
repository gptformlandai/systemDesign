# Spring Boot Interview Track Index

Target: starter, intermediate, senior, and MAANG-level Java backend preparation.

This folder is organized as a complete Spring Boot learning and interview track. The goal is
not to memorize annotations. The goal is to explain how Spring Boot applications are built,
secured, tested, deployed, observed, debugged, and evolved in production.

Current structure:

- 39 topic/practice sheets plus this root index
- 7 learning layers from setup to capstone
- coverage for fundamentals, production engineering, distributed workloads, modern Boot 4.1 readiness, supply-chain security, MAANG scenarios, active recall, and capstone design

---

## 1. Recommended Study Order

| Order | Document | Why This Order |
|---:|---|---|
| 0 | `00-Setup/Spring-Boot-Install-Initializr-Maven-Gradle-First-App-Gold-Sheet.md` | Build the first clean local Spring Boot app: JDK, Initializr, Maven/Gradle wrapper, starters, package layout, first endpoint, first test |
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
| 13 | `03-Senior-Distributed-Workloads/Spring-Kafka-Schema-Registry-Exactly-Once-Gold-Sheet.md` | Production Kafka: exactly-once semantics, Schema Registry, rebalancing, offset management, lag monitoring, and high-scale tuning |
| 14 | `03-Senior-Distributed-Workloads/Spring-Boot-Kafka-Batch-Advanced-Operations-Gold-Sheet.md` | Add senior Kafka/Batch operations: listener containers, ack modes, lag debugging, schema evolution, and restartable jobs |
| 15 | `03-Senior-Distributed-Workloads/Spring-Data-JPA-Advanced-Locking-Performance-Gold-Sheet.md` | Advanced JPA: optimistic/pessimistic locking, N+1 fixes, batch operations, projections, stateless sessions, read-only transactions |
| 16 | `03-Senior-Distributed-Workloads/Spring-Boot-Data-Access-Beyond-JPA-JdbcClient-jOOQ-R2DBC-NoSQL-Gold-Sheet.md` | Learn when to use JdbcClient, jOOQ, R2DBC, Redis, MongoDB, Elasticsearch, Neo4j, Cassandra, or plain JPA |
| 17 | `03-Senior-Distributed-Workloads/Spring-Security-Advanced-Resource-Server-Authorization-Server-Multitenancy-Gold-Sheet.md` | Add advanced resource server, authorization server awareness, service-to-service auth, tenant isolation, and auditing |
| 18 | `03-Senior-Distributed-Workloads/Spring-Boot-Browser-BFF-Session-Security-Gold-Sheet.md` | Cover browser-specific security: BFF, sessions, cookies, CSRF, SameSite, OAuth2 login, SAML2 awareness, and token relay |
| 19 | `03-Senior-Distributed-Workloads/Spring-Cloud-Microservices-Interview-Master-Sheet.md` | Learn Spring Cloud Config, Gateway, OpenFeign, discovery, load balancing, and Kubernetes alternatives |
| 20 | `03-Senior-Distributed-Workloads/Spring-Boot-Production-Runtime-Docker-Kubernetes-JVM-Gold-Sheet.md` | Learn Docker/buildpacks, JVM containers, probes, graceful shutdown, resource limits, config/secrets, and runtime diagnostics |
| 21 | `03-Senior-Distributed-Workloads/Spring-Boot-Supply-Chain-SBOM-Dependency-Security-Gold-Sheet.md` | Add SBOM, dependency scanning, image scanning, signed/provenance-aware artifacts, Actuator SBOM, and release gates |
| 22 | `03-Senior-Distributed-Workloads/Spring-Boot-Protocol-Modules-GraphQL-gRPC-Pulsar-Integration-Gold-Sheet.md` | Cover GraphQL, gRPC, Pulsar, Spring Integration, SSE/WebSocket/RSocket, and protocol decision trade-offs |
| 23 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Internals-AutoConfiguration-Proxies-Transactions-Platinum-Sheet.md` | Learn Boot internals, conditions, proxies, transaction traps, and diagnostics |
| 24 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Advanced-Internals-Proxies-Transactions-Diagnostics-Platinum-Sheet.md` | Add deeper BeanDefinition, post-processor, advisor ordering, propagation, isolation, lazy-loading, and transaction debugging depth |
| 25 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Production-Debugging-Case-Studies-Platinum-Sheet.md` | Learn production case studies around startup, DB, memory, cache, security, and slow APIs |
| 26 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Production-Debugging-Runbooks-JFR-Hikari-Platinum-Sheet.md` | Add incident runbooks for first 10 minutes, Hikari, CPU, memory, thread starvation, GC, JFR, and postmortems |
| 27 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Modern-3-4-AOT-GraalVM-Virtual-Threads-Platinum-Sheet.md` | Learn Boot 3/4, Jakarta migration, AOT, GraalVM native image, virtual threads, WebFlux, R2DBC, and Modulith trade-offs |
| 28 | `04-MAANG-Platinum-Scenarios/Spring-Boot-4-1-Modern-Platform-Update-Platinum-Sheet.md` | Update current platform facts: Boot 4.1.0, Java/Framework/build baselines, Servlet/GraalVM requirements, OTel, SBOM, gRPC, GraphQL, Pulsar |
| 29 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Modulith-Domain-Events-Boundaries-Platinum-Sheet.md` | Learn modular monolith boundaries, domain events, module tests, and when to split into microservices |
| 30 | `04-MAANG-Platinum-Scenarios/Spring-Boot-MAANG-Scenario-Based-Architecture-Questions-Platinum-Sheet.md` | Practice full scenario answers for booking, payment, cache, batch, security, and production design rounds |
| 31 | `04-MAANG-Platinum-Scenarios/Spring-Boot-Distributed-Architecture-Saga-CQRS-EventSourcing-Platinum-Sheet.md` | Distributed transactions: Saga, CQRS, Event Sourcing, eventual consistency UI patterns, correlation IDs, idempotency/deduplication |
| 32 | `05-Practice-Upgrade/Spring-Boot-Active-Recall-Question-Bank.md` | Convert notes into retrieval memory with question banks across every major topic |
| 33 | `05-Practice-Upgrade/Spring-Boot-Scenario-Drill-Bank.md` | Practice realistic design, debugging, security, testing, Kafka, Batch, and runtime scenarios |
| 34 | `05-Practice-Upgrade/Spring-Boot-Design-Coding-Mini-Labs.md` | Build small labs for APIs, idempotency, transactions, security, Testcontainers, WireMock, cache, async, outbox, Batch, runtime, supply chain, and modernization |
| 35 | `05-Practice-Upgrade/Spring-Boot-Mock-Interview-Scripts.md` | Run simulated interview rounds from starter fundamentals to MAANG production debugging |
| 36 | `05-Practice-Upgrade/Spring-Boot-Interview-Scoring-Rubrics.md` | Score readiness by area and identify Red gaps honestly |
| 37 | `05-Practice-Upgrade/Spring-Boot-2-Week-4-Week-Mastery-Roadmaps.md` | Follow a 2-week sprint or 4-week mastery plan with daily practice loops |
| 38 | `06-Capstone/Spring-Boot-Hotel-Booking-Production-Reference-Project.md` | Tie the entire track together in one end-to-end production-style hotel booking reference project |

---

## 2. What Each Layer Builds

### 00-Setup

This layer builds first-mile confidence.

You should be able to explain and build:

- local JDK, Spring Initializr, Maven/Gradle wrapper, and first app setup
- starter dependencies, Boot BOM/parent, wrapper reproducibility, and package layout
- first controller, first validation rule, first slice test, and first `application.yml`
- how to run, debug, and verify a Spring Boot app locally and in CI

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
- data access beyond JPA: JdbcClient, jOOQ, R2DBC, Redis, MongoDB, Elasticsearch/OpenSearch, Neo4j, Cassandra
- advanced resource server security, tenant isolation, auditing, and service-to-service auth
- browser/BFF security: sessions, cookies, CSRF, SameSite, OAuth2 login, SAML2 awareness, and token relay
- Spring Cloud Gateway, Config, OpenFeign, discovery, load balancing, and Kubernetes replacements
- Docker/buildpacks, JVM containers, probes, graceful shutdown, config/secrets, CPU limits, and Hikari tuning
- supply-chain security: SBOM, dependency/image scanning, provenance, signed artifacts, and CI release gates
- protocol modules: GraphQL, gRPC, Pulsar, Spring Integration, SSE/WebSocket/RSocket, and protocol trade-offs

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
- Spring Boot 4.1 platform facts: Java/Framework/build baselines, Servlet/GraalVM requirements, SBOM, OTel, gRPC, GraphQL, Pulsar
- modular monolith design with Spring Modulith, domain events, module boundary tests, and extraction decisions
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

### 06-Capstone

This layer turns the whole track into one end-to-end system.

You should use it to:

- connect REST, security, JPA, idempotency, payment, outbox, messaging, batch, observability, runtime, and supply chain
- practice one full hotel booking platform explanation without notes
- identify any weak area that still breaks the request lifecycle story

---

## 3. Level-Wise Learning Plan

### Setup Path

Read:

1. Install/Initializr/Maven/Gradle/first app setup
2. Backend build tools Maven and Gradle cross-links if build tooling is weak

Setup goal:

```text
I can generate, run, test, and explain a minimal Spring Boot app with a pinned JDK,
wrapper, starter dependencies, package layout, config, and CI command.
```

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
3. data access beyond JPA and workload-based persistence choices
4. advanced resource server, browser/BFF security, and multitenancy
5. Spring Cloud and Kubernetes-era alternatives
6. Docker/Kubernetes/JVM runtime operations
7. SBOM, dependency/image scanning, and release security
8. GraphQL/gRPC/Pulsar/Spring Integration protocol choices
9. Boot internals, proxies, transaction diagnostics, and production debugging

Senior goal:

```text
I can design reliable services that handle async workflows, batch jobs, messaging,
downstream failure, tenant/security boundaries, data access choices, protocol choices,
deployment safety, supply-chain security, and production debugging.
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
| First-mile setup | setup gold sheet, BackendBuildTools Maven/Gradle cross-links |
| Maven/Gradle wrapper and reproducible builds | setup gold sheet, BackendBuildTools Java/JVM build sheets |
| Core Spring container | Core sheet, internals platinum sheets |
| REST API development | Core sheet, REST API design sheet, scenario drills, labs |
| Validation and error handling | REST API design sheet, ProblemDetail labs |
| DTO/entity/API boundaries | REST API design sheet, capstone labs |
| JPA/Hibernate | JPA sheet, internals diagnostics sheets, production debugging sheets |
| Transactions and locking | JPA sheet, internals sheets, scenario drills, double-booking lab |
| Data access beyond JPA | non-JPA data access sheet, REST/WebFlux/R2DBC sheet, capstone roadmap |
| Security/JWT/OAuth2 | Security sheet, advanced resource server sheet, security mock rounds |
| Browser/BFF/session security | BFF/session security sheet, security sheet, capstone roadmap |
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
| WebFlux/R2DBC | REST/WebFlux/Resilience sheet, modern Boot sheet, non-JPA data access sheet, adjacent `Spring-Webflux/index.md` deep track |
| Resilience4j patterns | REST/WebFlux/Resilience sheet, scenario drills |
| Kafka | Messaging sheet, Kafka/Batch advanced operations sheet, labs |
| RabbitMQ | Messaging sheet |
| Outbox and idempotent consumers | Messaging sheet, Kafka/Batch sheet, labs |
| GraphQL/gRPC/Pulsar/Spring Integration | protocol modules sheet, Boot 4.1 update sheet |
| Actuator | Observability sheet, production runtime sheet |
| Metrics/tracing/logging | Observability sheet, debugging runbooks, dashboard lab |
| SBOM and supply-chain security | supply-chain/SBOM sheet, Boot 4.1 update sheet, BackendBuildTools Docker/image scanning sheet |
| Spring Cloud | Spring Cloud sheet |
| Kubernetes runtime | production runtime sheet, runtime lab |
| JVM diagnostics | production runtime sheet, debugging runbooks |
| Boot internals | internals platinum sheets |
| Auto-configuration diagnostics | internals platinum sheets |
| Proxy/self-invocation traps | internals platinum sheets |
| Production incidents | production debugging sheets, JFR/Hikari runbooks |
| Boot 3/4 and modern Spring | modern Boot platinum sheet, Boot 4.1 update sheet, modernization lab |
| Spring Modulith and module boundaries | Modulith platinum sheet, API contracts sheet, capstone roadmap |
| Active recall and mocks | all `05-Practice-Upgrade` sheets |
| Capstone architecture | capstone reference project, roadmap, labs, mocks |

---

## 6. Practice Path

After each concept layer, practice immediately.

| Stage | Practice |
|---|---|
| After Setup | create and explain a minimal app with wrapper, starter, first endpoint, first test |
| After Starter | answer Core, REST, JPA, Security, Testing recall questions |
| After Intermediate | run cache/async/client/observability scenario drills |
| After Senior | complete Kafka, Batch, data-access, BFF security, supply-chain, protocol, and runtime labs |
| After Platinum | run production debugging, Boot 4.1, Modulith, and modernization mock rounds |
| After Capstone | explain the hotel booking platform from request to deployment without notes |
| Before interviews | use scoring rubrics and 2-week/4-week roadmaps |

Minimum MAANG practice set:

1. 220+ active recall questions.
2. 18+ scenario drills.
3. 15+ design/coding mini-labs.
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
| Setup | JDK, wrapper, Initializr, package layout, first endpoint, first test |
| Core | REST API, service layer, validation, exceptions |
| REST API design | DTOs, ProblemDetail, pagination, OpenAPI, idempotency |
| JPA | booking/payment/customer entities, constraints, locks, N+1 fixes |
| Data access beyond JPA | JdbcClient/jOOQ reporting, Redis price cache, search projection, R2DBC decision |
| Security | JWT/OAuth2, BFF/session security, CSRF, scopes, roles, ownership checks, tenant isolation |
| Testing | unit, slice, integration, Testcontainers, contracts, migrations |
| Cache/Async | hotel/rate cache, confirmation async, distributed scheduled cleanup |
| REST clients | payment/inventory clients with timeout, retry, circuit breaker, bulkhead |
| Protocols | REST for commands, GraphQL for flexible search if needed, gRPC for internal pricing if justified, SSE/WebSocket for live status |
| Messaging | BookingCreated events, Kafka/Rabbit, outbox, DLT, idempotent consumer |
| Batch | nightly settlement and reconciliation with restartability |
| Observability | health, metrics, logs, traces, dashboards, alerts |
| Spring Cloud/runtime | gateway/config/discovery awareness, Kubernetes probes, graceful shutdown |
| Supply chain | SBOM, dependency scanning, image scanning, artifact provenance, release gates |
| Internals | auto-configuration, proxies, transactions, self-invocation diagnostics |
| Production Debugging | startup failures, slow APIs, Hikari, JVM, memory, CPU, security, cache |
| Modern Boot | Boot 3/4/4.1, AOT, native image, virtual threads, WebFlux/R2DBC trade-offs |
| Modulith | booking/inventory/payment/notification/settlement module boundaries and domain events |

Final capstone interview prompt:

```text
Design and implement a Spring Boot hotel booking platform that supports customer booking,
payment authorization, room inventory, email notification, nightly settlement, JWT/BFF
security, observability, SBOM-backed release safety, and production-safe deployments.
```

Strong answer must include:

- clear layered architecture
- clean setup and reproducible build with wrapper
- DTO boundaries
- service transaction boundaries
- workload-based data access decisions
- database constraints and locking
- security at URL, method, browser/session, tenant, and domain level
- idempotency for retries
- outbox for reliable events
- protocol choices with REST/GraphQL/gRPC/messaging/realtime trade-offs
- idempotent consumers
- Testcontainers for real DB tests
- WireMock/Pact/OpenAPI quality gates
- Flyway/Liquibase expand-contract migrations
- cache invalidation and stampede strategy
- downstream timeout/retry/circuit breaker/bulkhead
- Actuator/Micrometer/OpenTelemetry/tracing
- SBOM, dependency/image scanning, and artifact provenance
- Docker/Kubernetes readiness, liveness, graceful shutdown, and rollback safety
- Spring Modulith boundaries and extraction decision
- Boot 3/4/4.1 modernization and virtual-thread/native-image decision

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
setup, fundamentals, REST API design, JPA/transactions, data access beyond JPA, Security,
BFF/session security, testing, migrations, contracts, cache, async, scheduling, REST clients,
WebFlux, resilience, GraphQL, gRPC, Pulsar, Spring Integration, Kafka, RabbitMQ, Batch,
Spring Cloud, observability, SBOM/supply-chain security, Docker/Kubernetes/JVM runtime,
Boot internals, production debugging, Boot 3/4/4.1 modernization, AOT/native image, virtual
threads, Spring Modulith, scenario drills, mini-labs, mocks, rubrics, roadmaps, and an
end-to-end capstone reference project.
```

Adjacent deep tracks to use when the interview or role asks for extra depth:

- `Spring-Webflux/index.md` for deep reactive, HTTP interface, RSocket, SSE/WebSocket, and WebTestClient practice
- `Marriot/BackendBuildTools/02-Java-JVM-Builds` for Maven, Gradle, Java artifacts, Spring Boot config, Hikari/JPA, and JVM build/runtime depth
- `Marriot/BackendBuildTools/06-Production-CICD-Logs` for Docker, image scanning, CI/CD, structured logging, Micrometer, and OpenTelemetry depth

The current Spring Boot track now covers the high-frequency interview surface and the senior
production ownership depth expected in serious backend rounds.
