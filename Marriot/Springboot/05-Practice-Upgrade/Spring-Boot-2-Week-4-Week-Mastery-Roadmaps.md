# Spring Boot 2-Week And 4-Week Mastery Roadmaps

> Track: Spring Boot Interview Track - Practice Upgrade  
> Goal: finish the Spring Boot track with a concrete study plan.

Use 2 weeks for revision/interview sprint. Use 4 weeks for deeper mastery.

---

## 1. Daily Study Loop

Every study day:

1. Read one gold sheet section.
2. Answer 20 active recall questions.
3. Do one scenario drill or mini-lab.
4. Explain one topic aloud in 90 seconds.
5. Mark Red/Yellow/Green.

Time split:

| Activity | Sprint | Deep Plan |
|---|---:|---:|
| Reading | 30% | 35% |
| Active recall | 25% | 20% |
| Design/coding drill | 30% | 30% |
| Mock interview/debugging | 15% | 15% |

---

## 2. Two-Week Interview Sprint

Best for:

- already know Java and basic Spring Boot
- preparing for interviews soon
- need fast consolidation

### Week 1: Core Build Path

| Day | Focus | Practice |
|---:|---|---|
| 0 | Setup, Initializr, Maven/Gradle wrapper, first endpoint/test | First app setup lab + wrapper/build explanation |
| 1 | Spring Core, Boot startup, auto-config | Recall Core Boot + mock Round 1 |
| 2 | REST API design, validation, errors | Booking API lab + REST scenario drills |
| 3 | JPA, transactions, locking, N+1 | Double booking lab + JPA mock |
| 4 | Security resource server, BFF/session security, method security, tenancy | Security mock + tenant leak/browser CSRF scenario |
| 5 | Testing pyramid, Testcontainers, WireMock, Pact | Quality gates lab |
| 6 | Cache, async, scheduling, events | Cache/async lab + scheduled job drill |
| 7 | Review Red questions | 90-min mixed mock |

### Week 2: Senior/MAANG Path

| Day | Focus | Practice |
|---:|---|---|
| 8 | REST clients, resilience, WebFlux trade-offs | Payment client lab + retry storm scenario |
| 9 | Kafka, outbox, idempotent consumers | Outbox/consumer labs |
| 10 | Spring Batch and data processing | Batch lab + restart drill |
| 11 | Runtime, Docker, Kubernetes, JVM, supply chain/SBOM | Runtime config lab + SBOM/image scan drill |
| 12 | Production debugging, JFR, Hikari | Debugging mock + runbook lab |
| 13 | Boot 4.1, AOT, native image, virtual threads, Modulith, protocols | Modernization mock + protocol/Modulith decision lab |
| 14 | Capstone | Hotel booking reference project loop + scoring rubric |

Two-week pass gate:

- finish 12+ scenario drills
- finish 10+ mini-labs, including one setup, one security/BFF, and one supply-chain drill
- score 4+ in REST, JPA, Security, Testing, Runtime
- explain the capstone end to end

---

## 3. Four-Week Mastery Plan

Best for:

- serious MAANG-level preparation
- production interview depth
- building a portfolio-grade Spring Boot reference project

### Week 1: Foundations And API Craft

| Day | Focus | Deliverable |
|---:|---|---|
| 1 | Setup, Initializr, Maven/Gradle wrapper, Spring Core/IoC/DI/beans | first app + startup explanation notes |
| 2 | Auto-configuration/config/profiles/build reproducibility | debug auto-config examples + wrapper/CI command |
| 3 | REST controllers/DTOs | Booking API skeleton |
| 4 | Validation/errors/ProblemDetail | error contract tests |
| 5 | Pagination/versioning/OpenAPI | API contract sketch |
| 6 | Review and active recall | 100 recall questions |
| 7 | Mock day | starter + REST mock |

### Week 2: Data Correctness Security Testing

| Day | Focus | Deliverable |
|---:|---|---|
| 8 | JPA persistence context/lazy loading | N+1 fix examples |
| 9 | Transactions/locking/isolation | double booking lab |
| 10 | Migration strategy and data access beyond JPA | Flyway expand-contract lab + JdbcClient/jOOQ/R2DBC decision |
| 11 | Security resource server | JWT/security config sketch |
| 12 | Browser BFF/session security, method security, tenant isolation | CSRF/session/token storage plan + tenant leak prevention tests |
| 13 | Testing strategy | test pyramid + CI gate plan |
| 14 | Mock day | JPA + security + testing mock |

### Week 3: Distributed Workloads

| Day | Focus | Deliverable |
|---:|---|---|
| 15 | Cache design | price cache lab |
| 16 | Async/executor/scheduling | async + ShedLock/Quartz lab |
| 17 | REST clients and resilience | payment client WireMock lab |
| 18 | WebFlux/virtual threads trade-offs | decision matrix |
| 19 | Kafka producer/outbox | outbox lab |
| 20 | Kafka consumer/Batch/protocol modules | idempotent consumer + Batch lab + REST/GraphQL/gRPC/Pulsar decision |
| 21 | Mock day | resilience + Kafka + Batch + protocol mock |

### Week 4: Production And Platinum

| Day | Focus | Deliverable |
|---:|---|---|
| 22 | Actuator/Micrometer/OpenTelemetry | dashboard design |
| 23 | Docker/Kubernetes/runtime | probes/graceful shutdown config |
| 24 | Hikari/JVM/JFR debugging | incident runbooks |
| 25 | Supply chain/SBOM and Boot 4.1 migration | SBOM/image scan gate + Boot 4.1 upgrade checklist |
| 26 | AOT/native image/virtual threads/Modulith | modernization and module-boundary decision memo |
| 27 | Capstone build/design | end-to-end booking architecture |
| 28 | Final mock + rubric | MAANG readiness scorecard |

Four-week pass gate:

- finish all active recall sections
- finish 22+ mini-labs
- run at least 4 mock interviews
- score 4.3 average across rubric categories
- explain failure modes for every major Spring feature used

---

## 4. Topic Priority Matrix

| Priority | Topic | Why It Matters |
|---|---|---|
| P0 | Setup/build reproducibility | every project and CI pipeline starts here |
| P0 | REST API design | every Spring Boot backend interview touches this |
| P0 | JPA/transactions | most production bugs hide here |
| P0 | Security | common senior interview filter |
| P0 | Testing/Testcontainers/contracts | separates builder from production engineer |
| P0 | Runtime/debugging | MAANG-level signal |
| P1 | Data access beyond JPA | shows mature workload-based persistence choices |
| P1 | Browser/BFF/session security | avoids common SPA/token/CSRF interview traps |
| P1 | Cache/async/scheduling | common production foot-guns |
| P1 | Resilience/WebFlux/clients | distributed service readiness |
| P1 | Kafka/Batch | senior backend depth |
| P1 | Supply chain/SBOM | modern production security and release-readiness signal |
| P2 | GraphQL/gRPC/Pulsar/Spring Integration | protocol choice maturity |
| P2 | Spring Modulith | mature monolith vs microservices reasoning |
| P2 | AOT/native image | modern platform knowledge |
| P2 | Boot 4.1 readiness | current platform maturity |

---

## 5. Red Gap Repair Plan

If score is below 4:

| Red Gap | Repair Drill |
|---|---|
| Setup/build unclear | generate first app, explain wrapper, starter, package layout, and CI command |
| Boot startup unclear | draw startup flow from `main()` to controller ready |
| Proxy traps unclear | explain why self-invocation breaks transaction/cache/async/security |
| JPA weak | implement double-booking and N+1 labs |
| Data access choices weak | write JPA vs JdbcClient vs jOOQ vs R2DBC vs Redis/Search decision matrix |
| Security weak | implement resource server + tenant tests |
| Browser security weak | explain BFF, HttpOnly cookies, CSRF, SameSite, CORS, and token storage |
| Testing weak | replace one full-context test with slice/unit/integration mix |
| Runtime weak | run Hikari/high CPU/memory debug scripts aloud |
| Kafka weak | design outbox + idempotent consumer from memory |
| Supply chain weak | create SBOM/image scanning CI gate and CVE response plan |
| Protocol choice weak | choose REST/GraphQL/gRPC/Pulsar/SSE for hotel platform with trade-offs |
| Modulith weak | draw booking/inventory/payment module boundaries and event flow |
| Modern Boot weak | write Boot 3.x -> 4.1 migration checklist |

---

## 6. Final Capstone Checklist

Before calling the Spring Boot track complete, explain this system:

```text
Hotel booking service:
- reproducible setup with JDK, Maven/Gradle wrapper, starters, and CI verify command
- REST API with DTOs, validation, ProblemDetail
- JPA with transaction boundaries, locks/constraints, migrations
- JPA vs JdbcClient/jOOQ/R2DBC/Redis/Search choices explained
- JWT resource server, BFF/session security, method security, tenant isolation
- Testcontainers, WireMock, contracts, ArchUnit
- cache, async notifications, distributed scheduled cleanup
- outbound payment client with resilience controls
- REST/GraphQL/gRPC/Pulsar/SSE protocol choices defended
- outbox + Kafka + idempotent loyalty consumer
- Spring Batch settlement job
- Actuator/Micrometer/OpenTelemetry dashboard
- SBOM, dependency/image scanning, and release gate
- Docker/Kubernetes runtime with probes and graceful shutdown
- Spring Modulith module boundaries
- Boot 4.1 upgrade and virtual-thread/native-image decision
```

If you can design, test, debug, and defend this aloud, the track is MAANG-ready.
