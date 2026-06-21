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

## 11. Modern Boot Platinum

1. What changed in Spring Boot 3?
2. Why does Jakarta namespace matter?
3. What is AOT?
4. What is native image?
5. Native image trade-offs?
6. What are virtual threads?
7. When do virtual threads help Spring MVC?
8. Virtual threads vs WebFlux?
9. What remains a bottleneck with virtual threads?
10. What is Spring Modulith?
11. What does Boot 4 readiness mean?
12. How do you plan a major Boot upgrade?

---

## 12. Final Readiness Gate

You are ready when you can answer without notes:

1. Explain Spring Boot startup and auto-configuration.
2. Design a booking REST API with DTOs, validation, ProblemDetail, and idempotency.
3. Prevent double booking with transaction and DB constraints.
4. Secure API with JWT resource server and method/domain authorization.
5. Build test pyramid with Testcontainers, WireMock, Pact, ArchUnit, migrations.
6. Design cache/async/scheduling safely in Kubernetes.
7. Configure safe outbound client with timeout/retry/circuit breaker.
8. Explain Kafka listener ack/retry/DLT/idempotent consumer.
9. Debug p99, Hikari exhaustion, memory leak, high CPU, and startup failure.
10. Explain Boot 3/4, AOT/native image, virtual threads, WebFlux trade-offs.
