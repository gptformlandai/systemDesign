# Java Testing Patterns And Best Practices Gold Sheet

Target: Java backend interviews, production readiness, and coding rounds where testing maturity matters.

This sheet covers:
- Test strategy and test pyramid judgment
- JUnit 5 essentials
- Mockito usage and common traps
- Test data builders
- Fakes, stubs, mocks, and spies
- Integration testing boundaries
- Testcontainers patterns
- Contract and database testing awareness
- Concurrency and time-sensitive tests
- JMH and benchmarking boundaries
- Strong interview answers

---

## 1. Mental Model

Testing is not only checking that code runs once.

Testing proves behavior under clear conditions:

```text
Given state
    -> when action happens
    -> then expected behavior is observable
```

Good tests are:

- Fast enough to run often.
- Deterministic.
- Focused on behavior, not private implementation.
- Clear when they fail.
- Close enough to production for the risk being tested.

Strong interview line:

```text
I choose the lightest test that gives confidence for the risk. Unit tests protect logic,
integration tests protect wiring and real dependencies, contract tests protect service
boundaries, and performance tests protect latency or throughput assumptions.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Test pyramid | Very high | Shows practical judgment |
| JUnit 5 | Very high | Daily Java testing baseline |
| Mockito | Very high | Dependency isolation and trap area |
| Integration tests | Very high | Backend correctness with real infrastructure |
| Testcontainers | High | Real DB/broker tests without shared environments |
| Test data builders | High | Keeps tests readable and maintainable |
| Boundary tests | High | Prevents edge-case bugs |
| Flaky test diagnosis | High | Production engineering maturity |
| Concurrency tests | Medium-high | Needed for shared-state code |
| JMH | Medium-high | Avoids fake performance conclusions |
| Contract tests | Medium | Useful for microservice boundaries |
| Coverage percentage | Medium | Useful signal, not final quality metric |

---

## 3. Testing Pyramid For Java Backend

Simple shape:

```text
          few end-to-end tests
       integration / contract tests
    many fast unit and component tests
```

| Test Type | Scope | Speed | Good For |
|---|---|---:|---|
| Unit test | One class or small function | Very fast | Business rules, edge cases |
| Component test | Small module with fakes | Fast | Service behavior without real infra |
| Integration test | Real DB, broker, HTTP client, framework wiring | Medium | Persistence, transactions, serialization, config |
| Contract test | API/provider-consumer boundary | Medium | Compatibility across services |
| End-to-end test | Full system path | Slow | Critical user journeys |
| Performance test | Latency/throughput/resource use | Varies | Capacity and regression checks |

Interview answer:

```text
I do not try to test everything through end-to-end tests. I keep most logic covered by fast
unit tests, then add integration tests where the risk is in wiring, SQL, transaction behavior,
serialization, or external infrastructure.
```

---

## 4. Test Naming And Structure

Prefer names that describe behavior.

Good:

```java
@Test
void rejectsBookingWhenCheckOutIsBeforeCheckIn() {
}
```

Weak:

```java
@Test
void testBooking() {
}
```

Use Arrange, Act, Assert:

```java
@Test
void calculatesTotalForConfirmedItems() {
    // arrange
    Order order = OrderBuilder.anOrder()
        .withItem("room", 200)
        .withItem("tax", 20)
        .build();

    // act
    int total = calculator.total(order);

    // assert
    assertEquals(220, total);
}
```

Memory line:

```text
A good test reads like a small executable requirement.
```

---

## 5. JUnit 5 Essentials

Common annotations:

| Annotation | Use |
|---|---|
| `@Test` | Marks a test method |
| `@BeforeEach` | Runs before each test |
| `@AfterEach` | Runs after each test |
| `@BeforeAll` | Runs once before all tests |
| `@AfterAll` | Runs once after all tests |
| `@DisplayName` | Human-readable test name |
| `@Nested` | Groups related test cases |
| `@ParameterizedTest` | Runs same test with different inputs |
| `@CsvSource` | Inline parameter data |
| `@TempDir` | Temporary directory managed by JUnit |

Basic example:

```java
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceCalculatorTest {
    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void appliesWeekendMarkup() {
        int total = calculator.weekendPrice(100, 2);

        assertEquals(240, total);
    }
}
```

---

## 6. Assertions

Common assertions:

```java
assertEquals(expected, actual);
assertNotNull(value);
assertTrue(condition);
assertFalse(condition);
assertThrows(IllegalArgumentException.class, () -> service.book(command));
assertAll(
    () -> assertEquals("B101", booking.id()),
    () -> assertEquals("CONFIRMED", booking.status())
);
```

Exception example:

```java
@Test
void rejectsInvalidDateRange() {
    InvalidBookingException exception = assertThrows(
        InvalidBookingException.class,
        () -> service.book("R101", LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 9))
    );

    assertEquals("checkOut must be after checkIn", exception.getMessage());
}
```

Trap:

```text
Do not assert only that an exception happened. Assert the important message, code, or field
when that is part of the contract.
```

---

## 7. Parameterized Tests

Use parameterized tests when one behavior has many inputs.

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscountPolicyTest {
    private final DiscountPolicy policy = new DiscountPolicy();

    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "99, 0",
        "100, 10",
        "500, 50"
    })
    void appliesTenPercentDiscountAboveThreshold(int amount, int expectedDiscount) {
        assertEquals(expectedDiscount, policy.discount(amount));
    }
}
```

Good uses:

- Boundary values.
- Validation rules.
- Mapping tables.
- Enum behavior.
- Pricing rules.

Avoid:

```text
Huge unreadable parameter tables where each row needs a different explanation.
```

---

## 8. Test Data Builders

Problem:

```java
Booking booking = new Booking("B1", "R101", "U1", LocalDate.of(2026, 7, 1),
    LocalDate.of(2026, 7, 3), BookingStatus.CONFIRMED, 200, true, "NYC");
```

This is noisy. The important part is hidden.

Better builder:

```java
class BookingTestBuilder {
    private String bookingId = "B1";
    private String roomId = "R101";
    private String userId = "U1";
    private LocalDate checkIn = LocalDate.of(2026, 7, 1);
    private LocalDate checkOut = LocalDate.of(2026, 7, 3);
    private BookingStatus status = BookingStatus.CONFIRMED;

    static BookingTestBuilder aBooking() {
        return new BookingTestBuilder();
    }

    BookingTestBuilder withRoom(String roomId) {
        this.roomId = roomId;
        return this;
    }

    BookingTestBuilder withDates(LocalDate checkIn, LocalDate checkOut) {
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        return this;
    }

    Booking build() {
        return new Booking(bookingId, roomId, userId, checkIn, checkOut, status);
    }
}
```

Test usage:

```java
Booking booking = BookingTestBuilder.aBooking()
    .withRoom("R205")
    .withDates(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5))
    .build();
```

Strong line:

```text
Test builders make the default case obvious and highlight only the field relevant to the test.
```

---

## 9. Fakes, Stubs, Mocks, And Spies

| Type | Meaning | Example |
|---|---|---|
| Dummy | Passed but not used | Placeholder object |
| Stub | Returns predefined data | Fake client returns one response |
| Fake | Working lightweight implementation | In-memory repository |
| Mock | Verifies interaction | Verify email sender called |
| Spy | Wraps real object and observes/overrides part | Rarely needed |

Good default:

```text
Use a fake when behavior matters. Use a mock when interaction matters.
```

Example fake:

```java
class InMemoryBookingRepository implements BookingRepository {
    private final Map<String, Booking> bookings = new HashMap<>();

    public void save(Booking booking) {
        bookings.put(booking.id(), booking);
    }

    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(bookings.get(id));
    }
}
```

---

## 10. Mockito Basics

Mockito is useful when the class under test depends on collaborators.

```java
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BookingServiceTest {
    private final BookingRepository repository = mock(BookingRepository.class);
    private final NotificationClient notifications = mock(NotificationClient.class);
    private final BookingService service = new BookingService(repository, notifications);

    @Test
    void sendsNotificationAfterBookingIsSaved() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = service.book("R101", "U1");

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(notifications).sendConfirmation(captor.capture());
        assertEquals(booking.id(), captor.getValue().id());
    }
}
```

Useful methods:

```java
mock(Type.class)
when(client.call()).thenReturn(value)
when(client.call()).thenThrow(exception)
verify(client).call()
verify(client, never()).call()
verify(client, times(2)).call()
ArgumentCaptor.forClass(Type.class)
```

---

## 11. Mockito Traps

### Trap 1: Mocking The Class Under Test

Bad:

```java
BookingService service = mock(BookingService.class);
```

Better:

```java
BookingRepository repository = mock(BookingRepository.class);
BookingService service = new BookingService(repository);
```

Reason:

```text
Test the real class. Mock its dependencies.
```

### Trap 2: Verifying Implementation Details

Bad:

```java
verify(repository).findById("B1");
verify(repository).save(any());
```

This can make tests brittle if behavior is correct but implementation changes.

Better:

```java
Booking booking = service.confirm("B1");
assertEquals(CONFIRMED, booking.status());
```

Verify interactions mainly for side effects:

- Email sent.
- Audit event published.
- Payment client called once.
- Retry client called expected number of times.

### Trap 3: Too Much Mocking

If a test has ten mocks, the design may be too coupled.

Interview line:

```text
Mocks are useful, but too many mocks often reveal that the class has too many collaborators.
```

### Trap 4: `any()` With Nulls And Types

Matchers must be used consistently:

```java
when(client.get(eq("R101"), any(LocalDate.class))).thenReturn(room);
```

Do not mix raw values and matchers in the same invocation unless wrapped with `eq(...)`.

### Trap 5: Mocking Value Objects

Do not mock simple domain objects like `Booking`, `Room`, or `Money`.

Create real instances instead.

---

## 12. Integration Tests

Use integration tests when the risk is outside pure logic.

Good targets:

- SQL query correctness.
- Transaction rollback.
- Optimistic locking.
- JSON serialization/deserialization.
- HTTP client configuration.
- Message publishing/consuming.
- Framework wiring.
- Real validation behavior.

Example boundary:

```text
Unit test: BookingService rejects overlapping dates using a fake repository.
Integration test: BookingRepository query finds overlaps correctly in the real database.
```

Strong interview answer:

```text
If logic is pure, I use a unit test. If correctness depends on the database, transaction
manager, serializer, container config, or external protocol, I add an integration test.
```

---

## 13. Testcontainers Pattern

Testcontainers runs real infrastructure in containers for tests.

Common use cases:

- PostgreSQL/MySQL integration tests.
- Kafka consumer/producer tests.
- Redis/cache tests.
- LocalStack-style cloud service tests.

PostgreSQL example shape:

```java
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class BookingRepositoryIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("booking")
        .withUsername("test")
        .withPassword("test");

    @Test
    void savesAndLoadsBooking() {
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        BookingRepository repository = new JdbcBookingRepository(jdbcUrl, username, password);

        repository.save(new Booking("B1", "R101"));

        assertEquals("R101", repository.findById("B1").orElseThrow().roomId());
    }
}
```

Production caution:

```text
Testcontainers gives realistic dependencies, but tests are slower than unit tests. Use them
for integration boundaries, not every branch of business logic.
```

---

## 14. Database Testing Strategy

For repository/database tests, cover:

- Insert and read.
- Update and delete.
- Unique constraints.
- Null constraints.
- Transaction rollback.
- Optimistic locking/version conflict.
- Pagination and ordering.
- Time-zone sensitive fields.
- Query plans for expensive queries when performance matters.

Common trap:

```text
An in-memory DB can behave differently from production PostgreSQL/MySQL. For important SQL
behavior, prefer the same database engine through Testcontainers.
```

---

## 15. Contract Testing Awareness

Contract tests protect service boundaries.

Useful when:

- Many consumers depend on one API.
- Producer and consumer deploy independently.
- JSON shape compatibility matters.
- Breaking changes are expensive.

Simple contract idea:

```text
Consumer expectation:
GET /rooms/R101 returns roomId, status, price

Provider test:
Verify provider still returns that shape for the agreed scenario.
```

Interview line:

```text
For microservices, unit tests do not prove API compatibility. I use contract tests or strong
schema/versioning checks when independent services must evolve safely.
```

---

## 16. Testing Time, Randomness, And IDs

Bad:

```java
class BookingService {
    Booking book(String roomId) {
        return new Booking(UUID.randomUUID().toString(), roomId, LocalDate.now());
    }
}
```

Hard to test because time and ID are hidden.

Better:

```java
class BookingService {
    private final Clock clock;
    private final Supplier<String> idGenerator;

    BookingService(Clock clock, Supplier<String> idGenerator) {
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    Booking book(String roomId) {
        return new Booking(idGenerator.get(), roomId, LocalDate.now(clock));
    }
}
```

Test:

```java
Clock fixedClock = Clock.fixed(Instant.parse("2026-06-20T10:00:00Z"), ZoneOffset.UTC);
BookingService service = new BookingService(fixedClock, () -> "B1");
```

Strong line:

```text
I inject time and ID generation when deterministic behavior matters.
```

---

## 17. Concurrency Tests

Concurrency tests are hard because timing is nondeterministic.

Prefer testing invariants with coordinated start:

```java
@Test
void allowsOnlyOneBookingForSameRoomAndDate() throws Exception {
    BookingService service = new BookingService(new ThreadSafeBookingRepository());
    int threads = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threads);
    AtomicInteger success = new AtomicInteger();

    for (int i = 0; i < threads; i++) {
        executor.submit(() -> {
            try {
                start.await();
                service.book("R101", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3));
                success.incrementAndGet();
            } catch (RoomUnavailableException expected) {
                // expected for losing requests
            } finally {
                done.countDown();
            }
        });
    }

    start.countDown();
    assertTrue(done.await(5, TimeUnit.SECONDS));
    assertEquals(1, success.get());
    executor.shutdownNow();
}
```

Caution:

```text
A passing concurrency test does not prove there is no race forever. It is still useful when
combined with code review, proper locks/atomics, and stress testing.
```

---

## 18. Performance Tests And JMH Boundaries

Naive timing is often wrong:

```java
long start = System.nanoTime();
methodUnderTest();
long elapsed = System.nanoTime() - start;
```

Problems:

- JIT warmup not handled.
- Dead-code elimination.
- GC noise.
- CPU frequency changes.
- One run is not statistically meaningful.
- Benchmark and production workload differ.

Use JMH for microbenchmarks:

```java
@Benchmark
public int sum() {
    return values.stream().mapToInt(Integer::intValue).sum();
}
```

Interview line:

```text
I avoid making performance claims from naive timing. For microbenchmarks I use JMH, and for
service performance I prefer load tests with realistic data, concurrency, dependencies, and
observability.
```

Boundary:

| Need | Better Tool |
|---|---|
| Compare two small algorithms | JMH |
| Validate endpoint latency | Load test |
| Find CPU hotspots | JFR/profiler |
| Check SQL query cost | DB explain plan |
| Measure memory retention | Heap dump/JFR |

---

## 19. Flaky Test Debugging

Common causes:

- Real time instead of fixed clock.
- Shared mutable static state.
- Test order dependency.
- Parallel tests using same resource.
- Random ports or files not isolated.
- Async code without proper waiting.
- External service dependency.
- Too-strict timing assertions.

Fixes:

- Inject `Clock`.
- Use `@TempDir`.
- Reset static/shared state.
- Use unique test data.
- Wait for observable condition, not sleep.
- Prefer Testcontainers over shared dev databases.
- Avoid relying on test order.

Strong answer:

```text
A flaky test is a production signal. I first identify whether the nondeterminism comes from
time, shared state, async behavior, external dependency, or environment. Then I make the test
isolated and deterministic.
```

---

## 20. Coverage Judgment

Coverage is useful, but not enough.

Bad target:

```text
We have 90% coverage, so the code is safe.
```

Better:

```text
Coverage tells me what executed, not whether the right behavior was asserted.
```

Good coverage includes:

- Happy path.
- Boundary values.
- Invalid inputs.
- Empty/null cases where allowed.
- Duplicate/conflict cases.
- Authorization or permission cases.
- Timeout/retry behavior.
- Idempotency behavior.
- Persistence and transaction behavior when relevant.

Interview line:

```text
I care more about meaningful assertions on critical behavior than chasing a number alone.
```

---

## 21. Mini Program: Testable Booking Service

Production code shape:

```java
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

record Booking(String id, String roomId, LocalDate checkIn, LocalDate checkOut) {
    Booking {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
    }
}

interface BookingRepository {
    void save(Booking booking);
    Optional<Booking> findById(String id);
}

class InMemoryBookingRepository implements BookingRepository {
    private final Map<String, Booking> bookings = new HashMap<>();

    public void save(Booking booking) {
        bookings.put(booking.id(), booking);
    }

    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(bookings.get(id));
    }
}

class BookingService {
    private final BookingRepository repository;
    private final Supplier<String> idGenerator;

    BookingService(BookingRepository repository, Supplier<String> idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    Booking book(String roomId, LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking(idGenerator.get(), roomId, checkIn, checkOut);
        repository.save(booking);
        return booking;
    }
}
```

Test shape:

```java
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingServiceTest {
    @Test
    void savesBookingWithGeneratedId() {
        InMemoryBookingRepository repository = new InMemoryBookingRepository();
        BookingService service = new BookingService(repository, () -> "B1");

        Booking booking = service.book(
            "R101",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 3)
        );

        assertEquals("B1", booking.id());
        assertEquals(booking, repository.findById("B1").orElseThrow());
    }

    @Test
    void rejectsInvalidDates() {
        BookingService service = new BookingService(new InMemoryBookingRepository(), () -> "B1");

        assertThrows(IllegalArgumentException.class, () -> service.book(
            "R101",
            LocalDate.of(2026, 7, 3),
            LocalDate.of(2026, 7, 1)
        ));
    }
}
```

---

## 22. Common Testing Mistakes

| Mistake | Better Approach |
|---|---|
| Testing only happy path | Add boundary and failure cases |
| Mocking everything | Use real value objects and fakes where possible |
| Mocking class under test | Instantiate real class, mock dependencies |
| Verifying private implementation | Assert observable behavior |
| Shared mutable test state | Fresh state per test |
| Sleeping in async tests | Wait for condition with timeout |
| Using production DB in tests | Use isolated DB/Testcontainers |
| Ignoring transaction tests | Test commit/rollback when important |
| Chasing coverage only | Assert critical behavior clearly |
| Naive performance timing | Use JMH/load testing/profilers |

---

## 23. FAANG-Level Question

> How do you design a testing strategy for a Java booking service?

Strong answer:

```text
I start with fast unit tests for booking rules: date validation, overlap checks, pricing, and
status transitions. I use test data builders to keep those tests readable. For dependencies,
I prefer fakes for repositories when behavior matters and Mockito for side-effect boundaries
like notifications or payment clients.

Then I add integration tests for the database because booking correctness depends on unique
constraints, transactions, and overlap queries. I would use Testcontainers with the same DB
engine as production. For APIs, I add contract tests if other services depend on the JSON
shape. For concurrency-sensitive booking, I test the invariant that only one conflicting
booking succeeds, but I still rely on DB constraints or locks for the actual guarantee.
```

---

## 24. Rapid Revision

- Unit tests protect pure logic.
- Integration tests protect real dependency behavior.
- Contract tests protect API compatibility.
- Testcontainers gives real infrastructure in isolated tests.
- Mockito is for collaborators, not the class under test.
- Prefer real value objects over mocked domain models.
- Use test data builders when object setup becomes noisy.
- Inject time, IDs, randomness, and external clients for deterministic tests.
- Do not trust coverage percentage alone.
- Do not trust naive performance timing.
- Use JMH for microbenchmarks.
- Use load tests/profilers for service performance.

---

## 25. Official Source Notes

Use these sources when refreshing details:

- JUnit 5 User Guide: `https://docs.junit.org/`
- Mockito documentation: `https://site.mockito.org/`
- Testcontainers documentation: `https://testcontainers.com/`
- JMH project: `https://openjdk.org/projects/code-tools/jmh/`
- Java API documentation: `https://docs.oracle.com/en/java/javase/`

Interview safety line:

```text
I do not choose one testing style for everything. I match the test type to the risk: unit for
logic, integration for real infrastructure, contract for boundaries, and JMH or load tests
for performance claims.
```
