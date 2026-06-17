# Java Production Engineering Best Practices FAANG Master Sheet

Target: senior Java interviews where the interviewer asks how you write reliable production code, not just syntax.

This sheet covers:
- API design
- Immutability and defensive copying
- Null handling and Optional
- Exception strategy
- Logging and MDC
- Timeouts, retries, idempotency
- Resource lifecycle
- Validation
- Testing
- Performance habits
- Code review checklist

---

## 1. Mental Model

Production Java code must be:

- Correct
- Readable
- Observable
- Testable
- Resource-safe
- Failure-aware
- Easy to change

Strong interview line:

```text
Good Java code is not just code that compiles. It must be easy to reason about under load,
failure, concurrency, and future change.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Immutability | Very high | Safer objects and concurrency |
| Defensive copying | High | Prevents hidden mutation |
| Optional judgment | High | Null handling maturity |
| Exception strategy | Very high | API and failure clarity |
| Logging | High | Production debugging |
| Timeouts | Very high | Prevents stuck requests |
| Retries | High | Must avoid retry storms |
| Idempotency | High | Safe repeated operations |
| Resource lifecycle | Very high | Prevents leaks |
| Testing | Very high | Reliability |
| Performance profiling | High | Avoids guesswork |
| Code review checklist | Medium-high | Senior habit |

---

## 3. Immutability

Immutable objects are easier to reason about.

Benefits:

- Thread-safe by default if deeply immutable.
- Safe as map keys.
- Easier testing.
- No accidental mutation.

Example:

```java
import java.util.List;

public final class BookingSummary {
    private final String bookingId;
    private final List<String> guestNames;

    public BookingSummary(String bookingId, List<String> guestNames) {
        this.bookingId = bookingId;
        this.guestNames = List.copyOf(guestNames);
    }

    public String bookingId() {
        return bookingId;
    }

    public List<String> guestNames() {
        return guestNames;
    }
}
```

Interview line:

```text
Immutability removes many classes of bugs, especially around shared state and defensive API design.
```

---

## 4. Defensive Copying

Bad:

```java
class BadOrder {
    private final List<String> items;

    BadOrder(List<String> items) {
        this.items = items;
    }

    List<String> items() {
        return items;
    }
}
```

Problem:

```text
Caller can mutate the original list or returned list.
```

Better:

```java
class GoodOrder {
    private final List<String> items;

    GoodOrder(List<String> items) {
        this.items = List.copyOf(items);
    }

    List<String> items() {
        return items;
    }
}
```

If mutable return is needed:

```java
return new ArrayList<>(items);
```

---

## 5. Null Handling

Rules:

- Validate required inputs early.
- Return empty collection instead of null collection.
- Use Optional mainly for return values.
- Avoid Optional fields and parameters in most cases.
- Avoid direct `Optional.get()`.

Good:

```java
Optional<User> findUser(String id) {
    return repository.findById(id);
}
```

Bad:

```java
void update(Optional<String> name) {
    // awkward API
}
```

Better:

```java
void update(String name) {
    Objects.requireNonNull(name, "name");
}
```

Interview line:

```text
Optional is best as an explicit return type for maybe-absent values, not as a universal
replacement for null everywhere.
```

---

## 6. Exception Strategy

Exception types:

| Type | Use |
|---|---|
| Checked exception | Caller is expected to handle/recover |
| Runtime exception | Programming/config/business invariant violation |
| Custom exception | Adds domain meaning |

Good custom exception:

```java
class PaymentFailedException extends RuntimeException {
    PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Wrap with context:

```java
try {
    gateway.charge(request);
} catch (IOException e) {
    throw new PaymentFailedException("Payment gateway call failed for order " + orderId, e);
}
```

Do not:

```java
catch (Exception e) {
    // ignore
}
```

Strong answer:

```text
I preserve the cause, add useful context, avoid swallowing exceptions, and choose checked
or unchecked based on whether the caller can reasonably recover.
```

---

## 7. Logging

Good logs answer:

- What happened?
- For which entity/request?
- Why did it fail?
- Is action needed?
- Can it be correlated?

Bad:

```java
log.error("error");
```

Better:

```java
log.error("Payment failed for orderId={} userId={}", orderId, userId, exception);
```

Rules:

- Log with context.
- Do not log secrets.
- Avoid noisy logs in hot loops.
- Use structured logging where available.
- Include correlation/request ID.

MDC awareness:

```text
MDC stores logging context per thread. With thread pools and virtual threads, verify
context propagation and cleanup.
```

---

## 8. Timeouts

Every remote call needs a timeout.

Without timeout:

```text
One bad dependency can hang threads, queues, and request paths.
```

Timeout categories:

- Connection timeout
- Read/socket timeout
- Request timeout
- Pool acquisition timeout
- Transaction timeout

Strong answer:

```text
Timeouts are part of correctness. Without them, failure can become resource exhaustion.
```

---

## 9. Retries

Retries help transient failures but can amplify incidents.

Retry only when:

- Operation is safe or idempotent.
- Failure is likely transient.
- There is a limit.
- There is backoff and jitter.
- There is a timeout budget.

Bad:

```text
Retry forever immediately.
```

Better:

```text
Retry at most 2-3 times with exponential backoff and jitter, within request deadline.
```

Interview line:

```text
Retries must be bounded and paired with timeouts, idempotency, and backoff to avoid retry storms.
```

---

## 10. Idempotency

Idempotency means repeating an operation has the same effect as doing it once.

Important for:

- Payments
- Orders
- Booking
- Message consumers
- Retryable APIs

Example:

```text
POST /payments
Idempotency-Key: order-123-payment-1
```

Server stores result by key:

```text
If same idempotency key arrives again, return previous result instead of charging twice.
```

Strong answer:

```text
For retryable write operations, idempotency keys protect against duplicate side effects.
```

---

## 11. Resource Lifecycle

Resources to close:

- Files
- Streams
- Sockets
- DB connections
- HTTP responses
- Executors
- Watch services

Use:

```java
try (InputStream in = Files.newInputStream(path)) {
    return in.readAllBytes();
}
```

Executor lifecycle:

```java
ExecutorService executor = Executors.newFixedThreadPool(4);
try {
    executor.submit(task);
} finally {
    executor.shutdown();
}
```

Production note:

```text
In application frameworks, lifecycle may be container-managed. Still know who owns shutdown.
```

---

## 12. Validation

Validate at boundaries:

- Controller/API boundary
- Message consumer boundary
- File import boundary
- Public method boundary

Example:

```java
class BookingRequest {
    private final String hotelId;
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    BookingRequest(String hotelId, LocalDate checkIn, LocalDate checkOut) {
        this.hotelId = Objects.requireNonNull(hotelId, "hotelId");
        this.checkIn = Objects.requireNonNull(checkIn, "checkIn");
        this.checkOut = Objects.requireNonNull(checkOut, "checkOut");

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
    }
}
```

Strong answer:

```text
Validate early at system boundaries and keep domain invariants inside the domain model.
```

---

## 13. API Design

Good Java API:

- Clear method names.
- Small parameter list.
- Immutable request objects for complex input.
- No surprising side effects.
- Explicit return type.
- Clear failure behavior.

Bad:

```java
process(String a, String b, String c, boolean d, int e)
```

Better:

```java
processBooking(BookingCommand command)
```

Command:

```java
record BookingCommand(
    String hotelId,
    String userId,
    LocalDate checkIn,
    LocalDate checkOut
) {}
```

---

## 14. Testing Strategy

Test pyramid:

```text
Unit tests
    -> Component/integration tests
        -> Contract tests
            -> End-to-end tests
```

Java-specific must test:

- equals/hashCode
- boundary dates/times
- null handling
- exception paths
- concurrent behavior where applicable
- serialization/deserialization if used
- stream logic with empty/single/multiple values

Concurrency testing caution:

```text
Thread bugs can be timing-sensitive. Use deterministic design, small critical sections,
and stress tests for risky code.
```

---

## 15. Performance Habits

Rules:

- Measure before optimizing.
- Prefer clear code unless profiling says otherwise.
- Avoid unnecessary object creation in hot paths.
- Avoid regex in tight loops unless precompiled.
- Avoid repeated string concatenation in loops.
- Use primitive streams for numeric aggregation when useful.
- Keep logs out of hot paths or guard expensive log creation.
- Use bounded collections/caches.

Example:

```java
private static final Pattern ORDER_ID_PATTERN = Pattern.compile("[A-Z]{3}-\\d+");
```

Instead of compiling pattern repeatedly.

---

## 16. Date And Time Production Rules

Use `java.time`.

Rules:

- Use `Instant` for machine timestamp.
- Use `LocalDate` for date-only business concepts.
- Use `ZonedDateTime` when timezone matters.
- Store UTC unless domain requires local time.
- Avoid old `Date`/`Calendar` in new code.

Example:

```java
Instant createdAt = Instant.now();
LocalDate checkIn = LocalDate.of(2026, 6, 17);
```

Interview line:

```text
I choose date/time types based on meaning, not convenience.
```

---

## 17. Equality And Ordering

If using objects in hash collections:

- Implement equals/hashCode.
- Keep fields immutable.
- Do not use mutable fields in hashCode.

If sorting:

- Comparator must be consistent.
- Avoid subtraction comparator overflow.

Bad:

```java
Comparator<Order> byAmount = (a, b) -> a.amount() - b.amount();
```

Better:

```java
Comparator<Order> byAmount = Comparator.comparingInt(Order::amount);
```

---

## 18. Thread Safety In Production Code

Prefer:

- Stateless services.
- Immutable objects.
- Local variables.
- Concurrent collections when needed.
- Database transactions for shared durable state.

Avoid:

- Shared mutable static fields.
- Mutable singleton state.
- Unbounded in-memory queues.
- ThreadLocal without cleanup.

Strong answer:

```text
My first thread-safety strategy is to avoid shared mutable state. If sharing is required,
I guard invariants with appropriate synchronization or use concurrency utilities.
```

---

## 19. Code Review Checklist

Before approving Java code, check:

- Are inputs validated?
- Is null handled intentionally?
- Are collections bounded if they can grow?
- Are resources closed?
- Are timeouts configured?
- Are retries bounded?
- Is logging useful and safe?
- Are secrets excluded from logs?
- Is shared state thread-safe?
- Are exceptions preserved with cause?
- Are tests covering failure paths?
- Is the code simpler than the abstraction it introduces?

---

## 20. FAANG-Level Question

Question:

> How do you write Java code that is production-ready?

Strong answer:

```text
I focus on clear APIs, immutable data where possible, defensive copying at boundaries,
explicit validation, safe exception handling, and resource lifecycle through try-with-resources
or container-managed shutdown. For distributed calls, I always think about timeouts, retries,
idempotency, and observability. For concurrency, I avoid shared mutable state first and use
the right synchronization or concurrent collection when needed. I also rely on tests and
profiling instead of guessing.
```

---

## 21. Rapid Revision

Must-say lines:

```text
Production Java needs correctness, readability, observability, and resource safety.
```

```text
Use Optional mainly for return values, not everywhere.
```

```text
Timeouts are part of correctness.
```

```text
Retries need idempotency, limits, backoff, and jitter.
```

```text
ThreadLocal must be cleaned up in thread pools.
```

```text
Measure before optimizing.
```

---

## 22. Official Source Notes

Use official sources when refreshing:

- Java API docs: `https://docs.oracle.com/en/java/javase/`
- Java Language Specification: `https://docs.oracle.com/javase/specs/`
- OpenJDK project pages: `https://openjdk.org/`

Interview safety line:

```text
I optimize for boring, clear, observable Java code because production systems fail more
often from unclear ownership and missing limits than from lack of clever syntax.
```
