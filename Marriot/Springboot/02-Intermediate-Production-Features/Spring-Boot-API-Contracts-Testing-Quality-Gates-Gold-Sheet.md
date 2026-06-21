# Spring Boot API Contracts Testing Quality Gates Gold Sheet

> Track: Spring Boot Interview Track - Intermediate Production Features  
> Goal: make Spring Boot services safe to change through contract, integration, and architecture tests.

Read after Testing/Testcontainers/Migrations and REST API Design.

---

## 1. Why This Sheet Exists

Spring Boot testing interviews often stop at `@SpringBootTest`, `@WebMvcTest`, and
Testcontainers. Senior rounds ask how you prevent broken APIs, bad architecture boundaries,
regression bugs, and unsafe database or client changes from reaching production.

Strong answer:

```text
I use the smallest test scope that gives confidence, then add contract tests, integration
containers, API smoke tests, and architecture rules as quality gates in CI.
```

---

## 2. Test Types By Risk

| Risk | Test Type |
|---|---|
| business rule bug | unit test |
| controller validation/error shape | `@WebMvcTest` + MockMvc |
| repository query/mapping | `@DataJpaTest` + Testcontainers when DB-specific |
| full service wiring | `@SpringBootTest` |
| outbound HTTP client behavior | WireMock or mock web server |
| API contract compatibility | Pact / OpenAPI compatibility check |
| critical external API smoke | REST Assured or TestRestTemplate |
| architecture drift | ArchUnit |
| migration safety | Flyway/Liquibase migration test |

---

## 3. MockMvc vs TestRestTemplate vs REST Assured

| Tool | Use |
|---|---|
| MockMvc | MVC tests without real server socket |
| TestRestTemplate | Spring Boot integration tests with embedded server |
| WebTestClient | WebFlux or servlet tests with fluent API |
| REST Assured | black-box HTTP API tests, good for readable E2E/smoke tests |

Strong answer:

```text
For controller slices I use MockMvc. For full HTTP behavior with server port, I use
TestRestTemplate, WebTestClient, or REST Assured depending on style and team standard.
```

---

## 4. WireMock For Outbound Clients

Use WireMock when testing HTTP clients.

Example test shape:

```java
@SpringBootTest
class PaymentClientTest {
    @Test
    void mapsPaymentTimeoutToRetryableException() {
        stubFor(post("/payments/authorize")
            .willReturn(aResponse().withFixedDelay(3000)));

        assertThatThrownBy(() -> paymentClient.authorize(request()))
            .isInstanceOf(PaymentTimeoutException.class);
    }
}
```

What to test:

- success response mapping
- 4xx vs 5xx handling
- timeout behavior
- retryable vs non-retryable errors
- auth headers
- correlation headers
- invalid JSON

Strong answer:

```text
I use WireMock to test client behavior against realistic HTTP responses without depending
on a live downstream service.
```

---

## 5. Pact / Consumer-Driven Contracts

Contract tests protect provider-consumer compatibility.

Flow:

```text
Consumer defines expected request/response.
Provider verifies it still satisfies active consumer contracts.
CI blocks incompatible provider changes.
```

Use for:

- cross-team REST APIs
- independently deployed services
- critical provider changes

Not enough for:

- full business workflows
- performance
- database correctness
- security edge cases

Strong answer:

```text
Contract tests verify compatibility between services. They do not replace integration tests;
they prevent independent deployments from breaking consumers.
```

---

## 6. OpenAPI Compatibility Gate

For provider APIs, compare new OpenAPI spec to previous released spec.

Breaking examples:

- removed endpoint
- removed response field
- changed field type
- required a previously optional field
- removed error response shape

Safe examples:

- added optional response field
- added new endpoint
- added optional request field with default behavior

CI gate:

```text
Build -> generate OpenAPI -> compare with baseline -> fail on breaking change unless approved
```

---

## 7. Database Migration Test Gate

Migration pipeline should test:

1. start old schema
2. apply new migration
3. app starts against migrated schema
4. representative queries work
5. rollback/forward-fix plan exists
6. expand-contract compatibility is preserved

Expand-contract example:

```text
1. Add nullable column.
2. Deploy app writing both old and new fields.
3. Backfill.
4. Deploy app reading new field.
5. Remove old field later.
```

Strong answer:

```text
I avoid destructive schema changes in the same deployment that changes code. I use
expand-contract migrations so old and new app versions can coexist during rollout.
```

---

## 8. Testcontainers Strategy

Use Testcontainers for behavior that H2 or mocks cannot represent well:

- PostgreSQL syntax and indexes
- transaction isolation
- JSONB/arrays/vendor-specific types
- Kafka broker behavior
- Redis TTL/cache behavior
- Flyway/Liquibase migrations

Avoid using Testcontainers for every tiny unit test.

Strong answer:

```text
Testcontainers is for realistic integration confidence. I still keep unit and slice tests
fast, then use containers for infrastructure behavior that mocks hide.
```

---

## 9. Architecture Tests With ArchUnit

ArchUnit checks code structure.

Examples:

```java
@ArchTest
static final ArchRule controllers_should_not_access_repositories =
    noClasses().that().resideInAPackage("..controller..")
        .should().accessClassesThat().resideInAPackage("..repository..");
```

Useful rules:

- controllers do not call repositories directly
- domain does not depend on web layer
- services are not cyclically dependent
- packages follow module boundaries
- adapters depend inward, not outward

Strong answer:

```text
ArchUnit helps enforce architecture boundaries automatically, especially in large Spring
Boot codebases where layer violations creep in over time.
```

---

## 10. Spring Modulith Awareness

Spring Modulith helps structure a modular monolith.

Use when:

- service boundaries are not ready for microservices
- you want explicit application modules
- you want module boundary verification
- internal events are useful

Strong answer:

```text
Before extracting microservices, Spring Modulith can help enforce module boundaries inside
one Spring Boot application. It supports a modular monolith path before distributed complexity.
```

---

## 11. CI Quality Gate Stack

Recommended gate order:

```text
compile
unit tests
slice tests
architecture tests
integration tests with Testcontainers
contract verification
OpenAPI compatibility
migration tests
image build
smoke tests
canary metrics after deploy
```

Do not put every slow test in every local run. Use layers:

- local fast tests
- PR confidence tests
- nightly expensive tests
- pre-release migration/performance tests

---

## 12. Common Mistakes

| Mistake | Better Approach |
|---|---|
| every test uses `@SpringBootTest` | use smallest useful scope |
| H2 for all DB tests | use real DB container for vendor-specific behavior |
| only mocks for HTTP clients | add WireMock tests for protocol/error behavior |
| no contract testing | provider can break active consumers |
| no architecture tests | layer boundaries silently decay |
| migration tested manually | run migrations in CI |
| E2E tests for every edge | keep E2E few and critical |

---

## 13. Strong Closing Answer

```text
For Spring Boot quality gates, I combine fast unit and slice tests with Testcontainers for
real infrastructure behavior, WireMock for outbound clients, Pact/OpenAPI checks for
contract compatibility, ArchUnit for architecture boundaries, and Flyway/Liquibase migration
tests so code, contracts, and schema evolve safely.
```
