# Spring Boot Active Recall Question Bank

> Track: Spring Boot Interview Track - Practice Upgrade  
> Mode: answer from memory before checking notes.

Goal: convert Spring Boot notes into interview recall.

---

## 1. How To Use

Rules:

1. Answer aloud without notes.
2. Keep most answers under 90 seconds.
3. Write code/config when the question asks for it.
4. Mark Green, Yellow, or Red.
5. Repeat Red questions after 24 hours and 7 days.

A strong answer includes:

```text
definition -> runtime behavior -> code/config shape -> trap -> production trade-off -> test strategy
```

---

## 2. Spring Core And Boot

### Foundation

1. What is IoC?
2. What is dependency injection?
3. Constructor injection vs field injection?
4. What is a bean?
5. What are common bean scopes?
6. What is ApplicationContext?
7. What is `@SpringBootApplication` composed of?
8. What is auto-configuration?
9. What is a starter?
10. What is the difference between Spring Core, Spring MVC, and Spring Boot?

### Intermediate

1. How does `@Autowired` resolve dependencies?
2. What happens if there are multiple beans of same type?
3. What is `@Primary` vs `@Qualifier`?
4. What is bean lifecycle?
5. What is circular dependency and how do you fix it?
6. What is `@ConfigurationProperties` vs `@Value`?
7. How do profiles work?
8. How do you debug auto-configuration?
9. What is ConditionEvaluationReport?
10. What does auto-configuration backs off mean?

### Senior

1. Explain Boot startup from `main()` to embedded server.
2. BeanDefinition vs bean instance?
3. BeanFactoryPostProcessor vs BeanPostProcessor?
4. Why does self-invocation bypass `@Transactional`?
5. What is proxy stacking?
6. Why can final/private methods break proxy advice?
7. How do AOP proxies relate to transactions, caching, security, and async?
8. How do you diagnose missing bean in production/test?
9. How do you diagnose transaction advice not applying?
10. What changes in native image/AOT thinking?

---

## 3. REST API Design

1. Why should controllers be thin?
2. Why avoid exposing JPA entities as API responses?
3. DTO vs entity vs domain model?
4. What belongs in `@RestControllerAdvice`?
5. What is ProblemDetail?
6. 401 vs 403?
7. 400 vs 409?
8. Offset vs cursor pagination?
9. What is OpenAPI used for?
10. What is validation group?
11. When would you use MapStruct?
12. How do you design idempotent POST?
13. What fields should a validation error response include?
14. How do you version APIs safely?
15. What belongs in controller tests?

---

## 4. JPA And Hibernate

### Foundation

1. JPA vs Hibernate vs Spring Data JPA?
2. What is persistence context?
3. What is entity lifecycle?
4. What is dirty checking?
5. What is first-level cache?
6. Lazy vs eager loading?
7. What is N+1 query problem?
8. What is owning side?
9. What is transaction boundary?
10. What is optimistic locking?

### Intermediate

1. How does `@Transactional` interact with JPA flush?
2. Flush vs commit?
3. How do you fix N+1?
4. Fetch join vs entity graph vs DTO projection?
5. When do you use pessimistic locking?
6. What is `LazyInitializationException`?
7. Why avoid Open Session In View as a crutch?
8. How do you design pagination with JPA?
9. How do indexes influence repository methods?
10. How do you avoid long transactions?

### Senior

1. Prevent double booking with DB constraints/locks.
2. Debug connection pool exhaustion.
3. Explain Hikari metrics.
4. Explain isolation level trade-offs.
5. Why is external HTTP call inside transaction risky?
6. How do you test migration compatibility?
7. How do you choose between JPA and JDBC/native SQL?
8. When is R2DBC relevant?
9. What breaks with reactive transactions vs ThreadLocal assumptions?
10. How do you debug slow JPA query in production?

---

## 5. Spring Security

1. Authentication vs authorization?
2. What is SecurityFilterChain?
3. What is SecurityContextHolder?
4. 401 vs 403?
5. Roles vs authorities?
6. How does JWT validation work?
7. What is issuer, audience, expiry, signature?
8. What is JWKS?
9. What is OAuth2 Resource Server?
10. What is Spring Authorization Server?
11. When should a team build its own authorization server?
12. What is method security?
13. What is CSRF and when does it matter?
14. What is CORS and what does it not solve?
15. How do you enforce tenant isolation?
16. How do you audit sensitive actions?
17. Why is gateway authentication not enough?
18. How do you debug 401 spike after key rotation?
19. How do scopes map to Spring authorities?
20. How do you secure service-to-service calls?

---

## 6. Testing And Quality Gates

1. Unit test vs slice test vs integration test?
2. When should you avoid `@SpringBootTest`?
3. What does `@WebMvcTest` load?
4. What does `@DataJpaTest` load?
5. MockMvc vs TestRestTemplate vs REST Assured?
6. When do you use Testcontainers?
7. When do you use WireMock?
8. What does Pact protect?
9. What does ArchUnit protect?
10. How do you test Flyway/Liquibase migrations?
11. What is expand-contract migration?
12. How do you test OpenAPI compatibility?
13. What should CI quality gates include?
14. Why are too many E2E tests risky?
15. How do you keep tests fast but meaningful?

---

## 7. Cache Async Scheduling Events

1. Caffeine vs Redis?
2. What makes a good cache key?
3. How do you invalidate cache?
4. What is cache stampede?
5. How do you prevent cache stampede?
6. What are `@Cacheable` proxy traps?
7. How do you size `@Async` executor?
8. What happens to exceptions in async methods?
9. Does transaction context flow into async thread?
10. What is TaskDecorator?
11. Why does `@Scheduled` run in every pod?
12. What is ShedLock?
13. When would you use Quartz?
14. What is `@TransactionalEventListener`?
15. When is outbox better than in-process event?

---

## 8. REST Clients WebFlux Resilience

1. RestTemplate vs RestClient vs WebClient?
2. What timeouts should an HTTP client configure?
3. Retry vs circuit breaker?
4. What is backoff and jitter?
5. What is bulkhead?
6. What is rate limiter?
7. What is fallback?
8. Why is blocking inside WebFlux dangerous?
9. Mono vs Flux?
10. When is WebFlux a good fit?
11. When are virtual threads better than WebFlux?
12. What is R2DBC?
13. How do you test HTTP client error handling?
14. How do you propagate correlation IDs in clients?
15. How do you avoid retrying non-idempotent operations?

---

## 9. Messaging And Batch

1. Kafka topic, partition, offset, consumer group?
2. RabbitMQ exchange, queue, routing key?
3. What does Kafka order guarantee?
4. What is Spring Kafka listener container?
5. What are ack modes?
6. What is DLT?
7. How do you design idempotent consumer?
8. What is outbox pattern?
9. How do you debug consumer lag?
10. What limits useful listener concurrency?
11. What is schema registry?
12. What is Spring Batch Job/Step/JobRepository?
13. What is chunk processing?
14. What makes a batch job restartable?
15. Skip vs retry in Spring Batch?
16. What is partitioning?
17. Kafka vs Batch: when do you choose each?
18. How do you monitor batch jobs?

---

## 10. Observability Runtime Production

1. What is Actuator?
2. Liveness vs readiness?
3. What is Micrometer?
4. What is OpenTelemetry?
5. Logs vs metrics vs traces?
6. What is SLO?
7. What is error budget?
8. What is high-cardinality metric?
9. What should a Spring Boot dashboard include?
10. How do you secure Actuator endpoints?
11. What is graceful shutdown?
12. How do CPU limits cause throttling?
13. How do you size JVM in containers?
14. What is JFR useful for?
15. How do you debug memory leak?
16. How do you debug high CPU?
17. How do you debug Hikari pool exhaustion?
18. How do you debug startup failure?

---

## 11. Setup And Build Reproducibility

1. What JDK, build tool, and wrapper files should a new Spring Boot project pin?
2. Why should CI use `./mvnw` or `./gradlew` instead of a globally installed tool?
3. What does Spring Initializr generate for you?
4. What is a Spring Boot starter?
5. What is the difference between Boot parent and Boot BOM?
6. Why should the main application class sit at the root package?
7. What belongs in `application.yml` vs environment variables?
8. What is the smallest useful first test for a controller?
9. How do you debug "controller endpoint returns 404" in a new project?
10. How do you explain a first Spring Boot app from build file to embedded server startup?

---

## 12. Data Access Beyond JPA

1. When is JPA the right default?
2. When should you use JdbcClient instead of JPA?
3. When is jOOQ a strong fit?
4. When is R2DBC relevant?
5. Why does R2DBC require end-to-end reactive discipline?
6. When should Redis be a cache instead of source of truth?
7. When is Elasticsearch/OpenSearch appropriate?
8. Why should search indexes usually be projections?
9. How do you test database-specific SQL safely?
10. How do you choose between JPA, JdbcClient, jOOQ, R2DBC, Redis, MongoDB, and search?

---

## 13. Browser BFF Session Security

1. Resource server vs OAuth2 login?
2. What is a BFF?
3. Why avoid storing access tokens in local storage?
4. What does HttpOnly protect?
5. What does SameSite protect?
6. When does CSRF matter?
7. Why is CORS not authorization?
8. How should a BFF call downstream APIs?
9. How do you coordinate logout for app session and identity provider?
10. How do method/domain authorization relate to browser login?

---

## 14. Supply Chain, SBOM, And Release Security

1. What is an SBOM?
2. CycloneDX vs SPDX at a high level?
3. Why scan both JAR dependencies and container images?
4. How do you find services affected by a new critical CVE?
5. What should block a production release?
6. Why can dependency scanner results be noisy?
7. What is artifact provenance?
8. Why use an internal artifact repository proxy?
9. How do you avoid leaking secrets in image layers?
10. How should Actuator SBOM exposure be secured?

---

## 15. Protocol Modules

1. When is REST the best answer?
2. When is GraphQL a good fit?
3. What GraphQL failure mode resembles N+1?
4. When is gRPC a good internal protocol?
5. What operational support does gRPC need?
6. Kafka vs Pulsar at a high level?
7. When would you use Spring Integration?
8. SSE vs WebSocket?
9. Why should durable events use outbox/idempotent consumers?
10. How do you choose protocols for hotel search, booking, pricing, events, and live status?

---

## 16. Modern Boot Platinum

1. What changed in Spring Boot 3?
2. Why does Jakarta namespace matter?
3. What is AOT?
4. What is native image?
5. Native image trade-offs?
6. What are virtual threads?
7. When do virtual threads help Spring MVC?
8. Virtual threads vs WebFlux?
9. What remains a bottleneck with virtual threads?
10. What official platform facts matter for Spring Boot 4.1?
11. What Java, Spring Framework, Maven, Gradle, Servlet, and GraalVM baselines matter for Boot 4.1?
12. What does Boot 4.1 readiness mean?
13. How do you plan a major Boot upgrade?
14. How do you verify Actuator, metrics, traces, and SBOM behavior after upgrade?
15. Why should a Boot upgrade use canary and rollback planning?

---

## 17. Spring Modulith And Capstone Thinking

1. What is a modular monolith?
2. What problem does Spring Modulith solve?
3. How do module boundary tests help?
4. Why should one module not call another module's repository?
5. Domain event vs integration event?
6. When is in-process event enough?
7. When do you need outbox?
8. When should a module become a microservice?
9. What are good modules in a hotel booking platform?
10. Explain the full capstone request flow from `POST /bookings` to event consumer.

---

## 18. Final Readiness Gate

You are ready when you can answer without notes:

1. Generate and explain a first Spring Boot app with wrapper, starter, endpoint, config, and test.
2. Explain Spring Boot startup and auto-configuration.
3. Design a booking REST API with DTOs, validation, ProblemDetail, and idempotency.
4. Prevent double booking with transaction and DB constraints.
5. Choose JPA vs JdbcClient vs jOOQ vs R2DBC vs Redis/Search for different workloads.
6. Secure API with JWT resource server, BFF/session security, CSRF, method/domain authorization.
7. Build test pyramid with Testcontainers, WireMock, Pact, ArchUnit, migrations.
8. Design cache/async/scheduling safely in Kubernetes.
9. Configure safe outbound client with timeout/retry/circuit breaker.
10. Explain Kafka listener ack/retry/DLT/idempotent consumer.
11. Choose REST/GraphQL/gRPC/Pulsar/SSE/WebSocket intentionally.
12. Debug p99, Hikari exhaustion, memory leak, high CPU, and startup failure.
13. Explain SBOM, dependency/image scanning, and CVE response.
14. Explain Boot 3/4/4.1, AOT/native image, virtual threads, WebFlux trade-offs.
15. Explain Spring Modulith boundaries and the full capstone request lifecycle.
