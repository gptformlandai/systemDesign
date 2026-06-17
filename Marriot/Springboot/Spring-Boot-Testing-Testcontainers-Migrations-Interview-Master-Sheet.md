# Spring Boot Testing Testcontainers Migrations Interview Master Sheet

Target: Java Backend / Spring Boot / production-quality interviews.

This sheet covers:
- Spring Boot testing strategy
- unit vs slice vs integration tests
- `@SpringBootTest`
- `@WebMvcTest`
- `@DataJpaTest`
- `@MockBean` / `@MockitoBean`
- MockMvc and web tests
- Testcontainers
- database migration with Flyway and Liquibase
- migration safety in CI/CD

Goal:

```text
After reading this sheet, you should be able to design a practical Spring Boot test pyramid,
choose the right test annotation, use Testcontainers for realistic dependencies, and explain
safe database migrations using Flyway or Liquibase.
```

---

## 0. How To Use This Guide By Level

| Level | Focus |
|---|---|
| Beginner | unit tests, `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest` |
| Intermediate | test slices, mocks, MockMvc, transaction rollback |
| Senior | Testcontainers, migration testing, CI pipeline, contract boundaries |
| MAANG-ready | fast test pyramid, reliable integration tests, backward-compatible DB changes |

Strong line:

```text
Good Spring Boot testing is about choosing the smallest test scope that gives confidence.
Not every test should start the whole application context.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| Test pyramid | Very high | Engineering maturity |
| Unit tests | Very high | Fast feedback |
| `@SpringBootTest` | Very high | Full context testing |
| Test slices | Very high | Faster focused tests |
| `@WebMvcTest` | High | Controller testing |
| `@DataJpaTest` | High | Repository testing |
| MockMvc | High | MVC request testing |
| Mockito mocks | Very high | Isolated unit tests |
| Testcontainers | Very high | Real dependency testing |
| Transaction rollback in tests | High | JPA test behavior |
| Flyway | High | SQL migrations |
| Liquibase | Medium-high | Enterprise migrations |
| Migration compatibility | Very high | Production safety |
| CI strategy | High | Senior readiness |

---

# 2. Test Pyramid

```text
             few
        E2E / system tests
      integration tests
   slice tests
unit tests
             many
```

Goal:
- many fast unit tests
- focused slice tests
- enough integration tests for wiring and real infrastructure
- few expensive end-to-end tests

Strong answer:

```text
I prefer a test pyramid. Most business rules are unit-tested without Spring. Controllers,
repositories, and serialization use focused slice tests. Critical flows use integration
tests with Spring context and Testcontainers when real database behavior matters.
```

---

# 3. Unit Tests

Unit tests do not need Spring context.

Example:

```java
class PriceCalculatorTest {
    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void appliesWeekendSurcharge() {
        Money result = calculator.calculate(basePrice(), weekendDate());

        assertThat(result).isEqualTo(Money.of("120.00"));
    }
}
```

Best for:
- business rules
- validators
- mappers
- calculators
- pure services with mocked dependencies

Interview line:

```text
If I do not need dependency injection, HTTP, JPA, or configuration, I do not start Spring.
```

---

# 4. Mockito Service Test

Example:

```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock
    BookingRepository bookingRepository;

    @Mock
    PaymentClient paymentClient;

    @InjectMocks
    BookingService bookingService;

    @Test
    void confirmsBookingAfterPayment() {
        when(paymentClient.charge(any())).thenReturn(PaymentResult.success());

        bookingService.confirmBooking(100L);

        verify(bookingRepository).save(any(Booking.class));
    }
}
```

Pros:
- fast
- isolated
- easy failure pinpointing

Cons:
- does not verify Spring wiring
- can over-mock implementation details

---

# 5. `@SpringBootTest`

`@SpringBootTest` loads the full application context.

Example:

```java
@SpringBootTest
class BookingApplicationTest {
    @Test
    void contextLoads() {
    }
}
```

Use when:
- testing full wiring
- testing multiple layers together
- verifying configuration
- testing real transaction behavior
- bootstrapping application-level integration tests

Avoid for:
- simple business logic
- every controller test
- every repository test

Strong answer:

```text
@SpringBootTest is powerful but expensive. I use it for integration tests, not as the default
for every test.
```

---

# 6. Web Environment Options

Common modes:

| Mode | Meaning |
|---|---|
| `MOCK` | mock servlet environment, no real server |
| `RANDOM_PORT` | starts embedded server on random port |
| `DEFINED_PORT` | starts on configured port |
| `NONE` | non-web application context |

Example:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingApiIntegrationTest {
    @Autowired
    TestRestTemplate restTemplate;
}
```

Use `RANDOM_PORT` for:
- real HTTP integration tests
- filters/security/client behavior
- embedded server behavior

---

# 7. Test Slices

Spring Boot test slices load only part of the application.

| Annotation | Focus |
|---|---|
| `@WebMvcTest` | MVC controllers |
| `@DataJpaTest` | JPA repositories/entities |
| `@JsonTest` | JSON serialization/deserialization |
| `@RestClientTest` | REST clients |
| `@WebFluxTest` | WebFlux controllers |

Strong answer:

```text
Test slices are faster because they load only the beans needed for a layer. They are ideal
when I want Spring behavior without paying for the full application context.
```

---

# 8. `@WebMvcTest`

Use for controller tests.

Example:

```java
@WebMvcTest(BookingController.class)
class BookingControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BookingService bookingService;

    @Test
    void returnsBooking() throws Exception {
        when(bookingService.getBooking(100L))
                .thenReturn(new BookingResponse(100L, "CONFIRMED"));

        mockMvc.perform(get("/api/bookings/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
```

Use for:
- request mapping
- validation
- JSON shape
- status codes
- controller advice

Not for:
- repository queries
- full service integration
- real server behavior

Note:

```text
Older code commonly uses @MockBean. Newer Spring Boot versions have moved toward
Mockito-specific test bean annotations. In interviews, understand the purpose: replace
a Spring bean with a Mockito mock inside the test context.
```

---

# 9. MockMvc

MockMvc tests Spring MVC without starting a real HTTP server.

Example:

```java
mockMvc.perform(post("/api/bookings")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "hotelId": 10,
              "checkIn": "2026-07-01",
              "checkOut": "2026-07-03"
            }
            """))
        .andExpect(status().isCreated());
```

Good for:
- request/response behavior
- validation errors
- exception handling
- security filters if included

---

# 10. `@DataJpaTest`

Use for JPA repository and mapping tests.

Example:

```java
@DataJpaTest
class BookingRepositoryTest {
    @Autowired
    BookingRepository bookingRepository;

    @Test
    void findsConfirmedBookings() {
        bookingRepository.save(new Booking("B1", BookingStatus.CONFIRMED));

        List<Booking> result = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        assertThat(result).hasSize(1);
    }
}
```

What it loads:
- entities
- repositories
- JPA infrastructure
- test transaction support

Default behavior:

```text
@DataJpaTest usually rolls back test transaction after each test.
```

Strong answer:

```text
@DataJpaTest is ideal for repository queries, entity mappings, constraints, and JPA behavior
without loading the whole application.
```

---

# 11. H2 Trap

Common mistake:

```text
Repository tests pass on H2 but fail on PostgreSQL.
```

Why:
- SQL dialect differences
- JSON/array functions
- case sensitivity
- locking behavior
- index behavior
- timestamp/time zone differences

Better:

```text
Use Testcontainers with the same database engine as production for important repository
and migration tests.
```

---

# 12. Testcontainers

Testcontainers runs real dependencies in Docker during tests.

Common uses:
- PostgreSQL
- MySQL
- Kafka
- RabbitMQ
- Redis
- LocalStack

Example:

```java
@SpringBootTest
@Testcontainers
class BookingIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

Strong answer:

```text
Testcontainers gives realistic integration tests by using the same type of dependency as
production, such as PostgreSQL instead of H2.
```

---

# 13. Spring Boot Testcontainers Service Connections

Modern Spring Boot supports service connection style for supported containers.

Example:

```java
@SpringBootTest
@Testcontainers
class BookingIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");
}
```

Benefit:

```text
Spring Boot can auto-configure connection properties from the container.
```

Interview line:

```text
Service connections reduce boilerplate, but the core idea is still the same: tests run
against realistic infrastructure.
```

---

# 14. When To Use Testcontainers

Use Testcontainers for:
- database-specific SQL
- JPA locking behavior
- migrations
- Kafka/Rabbit integration
- Redis cache behavior
- transaction isolation tests

Avoid using it for:
- pure unit tests
- simple mapping logic
- every tiny test

Trade-off:

```text
Testcontainers increases realism but costs startup time.
```

---

# 15. External API Testing

Do not call real third-party APIs in normal CI tests.

Use:
- WireMock
- MockWebServer
- contract tests
- fake server container

Example idea:

```text
Booking service calls payment API.
Test uses fake payment server returning success, timeout, and 500 responses.
```

Strong answer:

```text
I avoid relying on real external APIs in CI. I use fake servers or contract tests to verify
HTTP behavior deterministically.
```

---

# 16. Spring Security Testing

Example:

```java
@Test
@WithMockUser(roles = "ADMIN")
void adminCanCancelBooking() throws Exception {
    mockMvc.perform(delete("/api/bookings/100"))
            .andExpect(status().isNoContent());
}
```

JWT style:

```java
mockMvc.perform(get("/api/bookings/100")
        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_booking.read"))))
        .andExpect(status().isOk());
```

Test:
- 401 without token
- 403 wrong role/scope
- 200 for correct authority
- ownership checks

---

# 17. Flyway

Flyway uses versioned SQL migrations.

Example files:

```text
db/migration/V1__create_booking_table.sql
db/migration/V2__add_booking_status.sql
db/migration/V3__create_payment_table.sql
```

Example:

```sql
create table bookings (
    id bigint generated always as identity primary key,
    booking_number varchar(50) not null unique,
    status varchar(30) not null,
    created_at timestamp not null
);
```

Strong answer:

```text
Flyway tracks schema versions and applies SQL migrations in order. It is simple and works
well when teams prefer plain SQL migrations.
```

---

# 18. Liquibase

Liquibase uses changelogs and changesets.

Example:

```yaml
databaseChangeLog:
  - changeSet:
      id: 001-create-bookings
      author: aravind
      changes:
        - createTable:
            tableName: bookings
            columns:
              - column:
                  name: id
                  type: bigint
                  constraints:
                    primaryKey: true
              - column:
                  name: status
                  type: varchar(30)
```

Strong answer:

```text
Liquibase provides structured changelogs and supports rollback metadata. It is common in
enterprises that want database-independent change definitions and richer tracking.
```

---

# 19. Flyway vs Liquibase

| Flyway | Liquibase |
|---|---|
| SQL-first | changelog-first |
| simple versioned migrations | richer changeset model |
| easy to read for DBAs | supports rollback metadata |
| great for app-owned schemas | common in enterprise governance |

Interview answer:

```text
Both solve database versioning. I choose based on team preference and governance. Flyway is
simple and SQL-friendly. Liquibase is richer for structured changesets and enterprise workflows.
```

---

# 20. Migration Safety

Safe migration principles:
- never edit an already-applied migration in shared environments
- make migrations backward-compatible
- separate schema change from code behavior change when needed
- avoid long locks on big tables
- backfill large tables in chunks
- add nullable column first, backfill, then make not-null
- deploy expand-contract for breaking changes
- test migrations on production-like database

Strong answer:

```text
Production database migrations should be forward-only, tested, and backward-compatible with
rolling deployments. For risky changes, I use expand-contract instead of one big breaking change.
```

---

# 21. Expand-Contract Pattern

Problem:

```text
Rename column customer_name to guest_name without downtime.
```

Safe approach:

1. Expand: add new `guest_name` column nullable.
2. Deploy code that writes both columns and reads fallback.
3. Backfill old rows.
4. Deploy code that reads only `guest_name`.
5. Contract: remove old `customer_name` later.

Interview line:

```text
For zero-downtime deployments, schema and code changes must overlap safely.
```

---

# 22. Migration Testing With Testcontainers

Test:
- migrations apply from empty database
- app starts after migrations
- repository queries work on migrated schema
- rollback strategy is documented
- old data transforms correctly

Example:

```java
@SpringBootTest
@Testcontainers
class MigrationSmokeTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Test
    void applicationStartsWithRealPostgres() {
    }
}
```

Strong answer:

```text
I like testing migrations against the same database engine using Testcontainers because
H2 cannot catch many production dialect and locking issues.
```

---

# 23. CI Testing Strategy

Typical CI stages:

```text
compile
unit tests
slice tests
integration tests with Testcontainers
migration validation
security/static checks
package image
deploy to staging
smoke tests
```

Balance:
- unit tests on every commit
- integration tests on PR
- full suite before deploy
- flaky tests treated as production risk

---

# 24. Production Scenario: Booking API Test Strategy

Requirement:

```text
Build reliable tests for a booking API with controller, service, JPA repository,
PostgreSQL, security, and payment client.
```

Test plan:
1. Unit test pricing and booking validation.
2. Unit test service with mocked repository/payment client.
3. `@WebMvcTest` controller validation and error responses.
4. Security tests for 401/403/ownership.
5. `@DataJpaTest` repository queries with PostgreSQL Testcontainer.
6. Migration smoke test with Flyway/Liquibase.
7. Integration test for create booking flow with fake payment server.
8. Contract test for payment client request/response.

Strong interview answer:

```text
I would not use @SpringBootTest for everything. Business rules get fast unit tests,
controllers get @WebMvcTest, repositories use @DataJpaTest with PostgreSQL Testcontainers
where database behavior matters, and critical booking flows get integration tests with fake
external services. Migrations are tested against a real database engine in CI.
```

---

# 25. Hot Interview Questions

### Q1. When do you use `@SpringBootTest`?

```text
When I need the full application context or multiple layers wired together. It is powerful
but slower than unit or slice tests.
```

### Q2. What is a test slice?

```text
A focused Spring Boot test that loads only part of the context, such as MVC or JPA, making
tests faster and more targeted.
```

### Q3. `@WebMvcTest` vs `@SpringBootTest`?

```text
@WebMvcTest loads MVC components for controller tests. @SpringBootTest loads the full
application context for integration tests.
```

### Q4. Why use Testcontainers?

```text
To test against real infrastructure such as PostgreSQL, Kafka, Redis, or RabbitMQ instead
of in-memory substitutes that behave differently.
```

### Q5. Why can H2 be risky for repository tests?

```text
It may not match production database dialect, functions, locking, indexing, JSON support,
or time zone behavior.
```

### Q6. Flyway vs Liquibase?

```text
Flyway is simple and SQL-version based. Liquibase uses structured changelogs and richer
changeset metadata. Both manage schema evolution.
```

### Q7. What is expand-contract?

```text
A zero-downtime migration strategy where we first add new schema, deploy compatible code,
backfill data, switch reads, then remove old schema later.
```

---

# 26. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| `@SpringBootTest` for every test | slow and noisy | use unit/slice tests |
| Mocking everything | no wiring confidence | add focused integration tests |
| Testing PostgreSQL logic on H2 only | false confidence | use Testcontainers |
| Calling real external API in CI | flaky and slow | fake server/contract test |
| Ignoring 401/403 tests | security regressions | explicit security tests |
| Editing old migration | breaks shared environments | add new migration |
| Big blocking migration | downtime risk | chunk/backfill/expand-contract |
| No migration test | deploy-time failure | test against real DB |
| Flaky tests ignored | trust erosion | fix or quarantine intentionally |

---

# 27. One-Hour Revision Plan

### First 15 Minutes: Test Strategy

Revise:
- test pyramid
- unit tests
- integration tests
- when not to start Spring

Must say:

```text
I choose the smallest test scope that proves the behavior.
```

### Next 15 Minutes: Spring Test Annotations

Revise:
- `@SpringBootTest`
- `@WebMvcTest`
- `@DataJpaTest`
- MockMvc
- mocks

Must say:

```text
Slice tests are faster because they load only the layer under test.
```

### Next 15 Minutes: Testcontainers

Revise:
- real database tests
- dynamic properties
- service connections
- Kafka/Redis containers

Must say:

```text
Testcontainers gives realism where in-memory dependencies are not enough.
```

### Final 15 Minutes: Migrations

Revise:
- Flyway
- Liquibase
- expand-contract
- migration testing
- CI pipeline

Must say:

```text
Database migrations must be backward-compatible during rolling deployments.
```

---

# 28. Final Rapid Revision Sheet

| Need | Tool |
|---|---|
| Fast business rule test | JUnit + Mockito |
| Full app context | `@SpringBootTest` |
| Controller layer | `@WebMvcTest` |
| Repository layer | `@DataJpaTest` |
| MVC request simulation | MockMvc |
| Real DB in tests | Testcontainers |
| Replace dependency bean | Mockito test bean |
| SQL migrations | Flyway |
| Structured changelogs | Liquibase |
| Safe breaking DB change | expand-contract |
| External API fake | WireMock / MockWebServer |
| Security user mock | Spring Security Test |

---

# 29. Strong Closing Answer

If interviewer asks:

```text
How do you test a Spring Boot application?
```

Say:

```text
I follow a test pyramid. I keep business logic in fast unit tests, use slice tests like
@WebMvcTest and @DataJpaTest for web and repository layers, and reserve @SpringBootTest
for integration flows that need real wiring. For database-specific behavior, I prefer
Testcontainers with the same database engine as production. I also test security paths,
external clients with fake servers, and database migrations using Flyway or Liquibase in CI.
```

---

# 30. Official Source Notes

Useful official references:

- Spring Boot Testing: https://docs.spring.io/spring-boot/reference/testing/index.html
- Spring Boot Test Slices: https://docs.spring.io/spring-boot/appendix/test-auto-configuration/slices.html
- Spring Boot Testcontainers: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Flyway: https://documentation.red-gate.com/fd
- Liquibase: https://docs.liquibase.com/

