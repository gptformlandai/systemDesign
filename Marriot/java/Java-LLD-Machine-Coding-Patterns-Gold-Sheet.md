# Java LLD And Machine Coding Patterns Gold Sheet

Target: Java machine-coding, LLD, and coding-design combo interviews.

This sheet covers:
- How to structure a Java coding-round solution
- Entity/model design
- Service design
- Repository design
- Strategy, factory, validator, state, observer
- Thread-safe machine-coding patterns
- In-memory storage
- Exception handling
- Testability
- Common machine-coding problems

---

## 1. Mental Model

Machine coding is not only about making code run.

It tests:

- Object modeling
- Separation of concerns
- Extensibility
- Thread safety
- Edge cases
- Clean APIs
- Fast implementation

Strong interview line:

```text
In machine coding, I first model the domain, isolate responsibilities, keep APIs small,
handle edge cases, and choose simple extensible patterns instead of overengineering.
```

---

## 2. Recommended Package Shape

For a small Java machine-coding problem:

```text
model/
    User.java
    Booking.java
    Room.java

service/
    BookingService.java
    PaymentService.java

repository/
    BookingRepository.java
    InMemoryBookingRepository.java

strategy/
    PricingStrategy.java
    SurgePricingStrategy.java

exception/
    BookingException.java

Main.java
```

If writing in one file, still keep the mental separation:

```text
models first
repositories next
services next
main/demo last
```

---

## 3. Machine Coding Answer Flow

Use this flow:

1. Clarify requirements.
2. Identify entities.
3. Identify operations.
4. Choose in-memory data structures.
5. Decide thread-safety needs.
6. Implement core happy path.
7. Add validations.
8. Add edge cases.
9. Add demo/test cases.
10. Explain trade-offs.

Never start with random classes.

---

## 4. Entity Design

Prefer immutable models when possible.

Example:

```java
import java.time.LocalDate;
import java.util.Objects;

public final class Booking {
    private final String bookingId;
    private final String roomId;
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    public Booking(String bookingId, String roomId, LocalDate checkIn, LocalDate checkOut) {
        this.bookingId = Objects.requireNonNull(bookingId, "bookingId");
        this.roomId = Objects.requireNonNull(roomId, "roomId");
        this.checkIn = Objects.requireNonNull(checkIn, "checkIn");
        this.checkOut = Objects.requireNonNull(checkOut, "checkOut");

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
    }

    public String bookingId() {
        return bookingId;
    }

    public String roomId() {
        return roomId;
    }

    public LocalDate checkIn() {
        return checkIn;
    }

    public LocalDate checkOut() {
        return checkOut;
    }
}
```

Java 17+ record version:

```java
public record Booking(
    String bookingId,
    String roomId,
    LocalDate checkIn,
    LocalDate checkOut
) {
    public Booking {
        Objects.requireNonNull(bookingId);
        Objects.requireNonNull(roomId);
        Objects.requireNonNull(checkIn);
        Objects.requireNonNull(checkOut);

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
    }
}
```

Interview line:

```text
I keep domain objects valid at construction time so invalid state is hard to represent.
```

---

## 5. Service Layer Pattern

Service contains use-case logic.

Example:

```java
class BookingService {
    private final BookingRepository repository;

    BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    Booking book(String roomId, LocalDate checkIn, LocalDate checkOut) {
        if (repository.hasOverlap(roomId, checkIn, checkOut)) {
            throw new BookingException("room unavailable");
        }

        Booking booking = new Booking(UUID.randomUUID().toString(), roomId, checkIn, checkOut);
        repository.save(booking);
        return booking;
    }
}
```

Rule:

```text
Controller/main should not contain business logic. Service should not know storage internals.
```

---

## 6. Repository Pattern

Repository hides storage.

Interface:

```java
interface BookingRepository {
    void save(Booking booking);

    boolean hasOverlap(String roomId, LocalDate checkIn, LocalDate checkOut);

    List<Booking> findByRoom(String roomId);
}
```

In-memory implementation:

```java
class InMemoryBookingRepository implements BookingRepository {
    private final Map<String, List<Booking>> bookingsByRoom = new HashMap<>();

    @Override
    public void save(Booking booking) {
        bookingsByRoom
            .computeIfAbsent(booking.roomId(), id -> new ArrayList<>())
            .add(booking);
    }

    @Override
    public boolean hasOverlap(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return bookingsByRoom.getOrDefault(roomId, List.of())
            .stream()
            .anyMatch(existing -> overlaps(existing, checkIn, checkOut));
    }

    @Override
    public List<Booking> findByRoom(String roomId) {
        return List.copyOf(bookingsByRoom.getOrDefault(roomId, List.of()));
    }

    private boolean overlaps(Booking existing, LocalDate checkIn, LocalDate checkOut) {
        return checkIn.isBefore(existing.checkOut()) && checkOut.isAfter(existing.checkIn());
    }
}
```

Trap:

```text
This repository is not thread-safe yet. Say that if concurrency matters.
```

---

## 7. Thread-Safe Repository Pattern

For single-JVM machine coding:

```java
class ThreadSafeBookingRepository implements BookingRepository {
    private final Map<String, List<Booking>> bookingsByRoom = new HashMap<>();
    private final Object lock = new Object();

    @Override
    public void save(Booking booking) {
        synchronized (lock) {
            bookingsByRoom
                .computeIfAbsent(booking.roomId(), id -> new ArrayList<>())
                .add(booking);
        }
    }

    @Override
    public boolean hasOverlap(String roomId, LocalDate checkIn, LocalDate checkOut) {
        synchronized (lock) {
            return bookingsByRoom.getOrDefault(roomId, List.of())
                .stream()
                .anyMatch(existing -> overlaps(existing, checkIn, checkOut));
        }
    }

    @Override
    public List<Booking> findByRoom(String roomId) {
        synchronized (lock) {
            return List.copyOf(bookingsByRoom.getOrDefault(roomId, List.of()));
        }
    }
}
```

Better for booking:

```text
Lock around check + insert together in service or repository, not separately.
```

Why:

```text
If hasOverlap and save are separate locked calls, another thread can insert between them.
```

---

## 8. Atomic Booking Method

Better repository:

```java
interface BookingRepository {
    Booking saveIfAvailable(String roomId, LocalDate checkIn, LocalDate checkOut);
}
```

Implementation:

```java
class AtomicBookingRepository implements BookingRepository {
    private final Map<String, List<Booking>> bookingsByRoom = new HashMap<>();
    private final Object lock = new Object();

    @Override
    public Booking saveIfAvailable(String roomId, LocalDate checkIn, LocalDate checkOut) {
        synchronized (lock) {
            List<Booking> bookings = bookingsByRoom.computeIfAbsent(roomId, id -> new ArrayList<>());

            boolean overlap = bookings.stream()
                .anyMatch(existing -> checkIn.isBefore(existing.checkOut())
                    && checkOut.isAfter(existing.checkIn()));

            if (overlap) {
                throw new BookingException("room unavailable");
            }

            Booking booking = new Booking(UUID.randomUUID().toString(), roomId, checkIn, checkOut);
            bookings.add(booking);
            return booking;
        }
    }
}
```

Interview line:

```text
For thread safety, check and write must be in the same critical section.
```

---

## 9. Strategy Pattern

Use when behavior changes by type.

Example:

```java
interface PricingStrategy {
    int price(Room room, int nights);
}

class RegularPricingStrategy implements PricingStrategy {
    public int price(Room room, int nights) {
        return room.basePrice() * nights;
    }
}

class WeekendPricingStrategy implements PricingStrategy {
    public int price(Room room, int nights) {
        return (int) (room.basePrice() * nights * 1.2);
    }
}
```

Use for:

- Pricing
- Discounts
- Payments
- Matching
- Ranking
- Validation rules

Interview line:

```text
Strategy removes algorithm-specific if-else from the service and makes behavior extensible.
```

---

## 10. Factory Pattern

Use when object creation depends on type.

Example:

```java
class PaymentProcessorFactory {
    private final Map<PaymentType, PaymentProcessor> processors;

    PaymentProcessorFactory(List<PaymentProcessor> processorList) {
        this.processors = processorList.stream()
            .collect(Collectors.toMap(PaymentProcessor::type, Function.identity()));
    }

    PaymentProcessor get(PaymentType type) {
        PaymentProcessor processor = processors.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("unsupported payment type");
        }
        return processor;
    }
}
```

Trap:

```text
Factory creates/selects object. Strategy represents behavior.
```

---

## 11. Validator Chain

Use for ordered validations.

```java
interface BookingValidator {
    void validate(BookingCommand command);
}

class DateValidator implements BookingValidator {
    public void validate(BookingCommand command) {
        if (!command.checkOut().isAfter(command.checkIn())) {
            throw new IllegalArgumentException("invalid dates");
        }
    }
}

class RoomValidator implements BookingValidator {
    public void validate(BookingCommand command) {
        if (command.roomId() == null || command.roomId().isBlank()) {
            throw new IllegalArgumentException("room required");
        }
    }
}

class BookingValidationChain {
    private final List<BookingValidator> validators;

    BookingValidationChain(List<BookingValidator> validators) {
        this.validators = List.copyOf(validators);
    }

    void validate(BookingCommand command) {
        validators.forEach(validator -> validator.validate(command));
    }
}
```

Interview line:

```text
Validator chain keeps each rule small and makes adding/removing rules easier.
```

---

## 12. State Pattern

Use when behavior depends on object state.

Example states:

```text
CREATED -> PAYMENT_PENDING -> CONFIRMED -> CANCELLED
```

Simple enum transition:

```java
enum BookingStatus {
    CREATED,
    PAYMENT_PENDING,
    CONFIRMED,
    CANCELLED
}
```

Transition method:

```java
BookingStatus confirm(BookingStatus current) {
    if (current != BookingStatus.PAYMENT_PENDING) {
        throw new IllegalStateException("only payment pending booking can be confirmed");
    }
    return BookingStatus.CONFIRMED;
}
```

Senior line:

```text
For simple state transitions, enum plus validation is enough. Use State pattern only when
each state has complex behavior.
```

---

## 13. Observer Pattern

Use when events notify multiple listeners.

Example:

```java
interface BookingListener {
    void onBookingConfirmed(Booking booking);
}

class BookingEventPublisher {
    private final List<BookingListener> listeners = new CopyOnWriteArrayList<>();

    void register(BookingListener listener) {
        listeners.add(listener);
    }

    void publishConfirmed(Booking booking) {
        listeners.forEach(listener -> listener.onBookingConfirmed(booking));
    }
}
```

Use for:

- Notifications
- Audit events
- Metrics
- Email/SMS triggers

Trap:

```text
In real systems, durable events often need a message broker or outbox, not only in-memory observers.
```

---

## 14. Command Pattern

Use when request data should be explicit.

```java
record BookingCommand(
    String roomId,
    LocalDate checkIn,
    LocalDate checkOut,
    String userId
) {
}
```

Benefits:

- Cleaner method signatures.
- Easier validation.
- Easier tests.
- Easier future fields.

Bad:

```java
book(String roomId, String userId, LocalDate checkIn, LocalDate checkOut, boolean notify, int retry)
```

Better:

```java
book(BookingCommand command)
```

---

## 15. Exception Pattern

Use domain exceptions for business failures.

```java
class BookingException extends RuntimeException {
    BookingException(String message) {
        super(message);
    }
}
```

Examples:

- `RoomUnavailableException`
- `InvalidBookingDateException`
- `PaymentFailedException`

Rule:

```text
Do not throw generic RuntimeException everywhere. Use meaningful exceptions when they help.
```

---

## 16. Testability Pattern

Good service:

```java
class BookingService {
    private final BookingRepository repository;
    private final Clock clock;

    BookingService(BookingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }
}
```

Why inject Clock:

```text
Tests can control current time.
```

Bad:

```java
LocalDate.now()
```

inside core logic everywhere.

Interview line:

```text
I inject time, randomness, and external dependencies when deterministic tests matter.
```

---

## 17. Machine Coding Problems And Patterns

| Problem | Useful Patterns |
|---|---|
| Parking Lot | Strategy, repository, state, factory |
| Elevator | State, scheduler strategy, priority queue |
| Rate Limiter | Strategy, concurrency, atomic counters |
| Splitwise | Graph/accounting, repository |
| BookMyShow | Locks, repository, seat hold expiry |
| Food Delivery | Strategy, matching, state machine |
| Logger | Chain of responsibility, levels, appenders |
| Cache | LRU, LinkedHashMap, eviction strategy |
| Snake and Ladder | Model, dice strategy, game loop |
| Chess | Strategy, state, factory, validation |

---

## 18. Thread-Safety Choices In Machine Coding

| Need | Simple Choice |
|---|---|
| Single counter | AtomicInteger |
| High-contention metric | LongAdder |
| Shared map | ConcurrentHashMap |
| Check + update invariant | synchronized/ReentrantLock |
| Limit concurrency | Semaphore |
| Producer-consumer | BlockingQueue |
| Listener list | CopyOnWriteArrayList |
| Read-heavy config | volatile immutable reference |

Senior line:

```text
Concurrent collections help with data structure safety, but business invariants often
still need a lock or transaction.
```

---

## 19. Common Machine Coding Mistakes

| Mistake | Better Approach |
|---|---|
| Starting without requirements | Clarify operations and constraints |
| God class | Split model/service/repository |
| Public mutable fields | Encapsulate and validate |
| No edge cases | Add invalid input and conflict handling |
| No thread-safety statement | Say assumptions clearly |
| Pattern overuse | Use patterns only where they simplify |
| Hardcoded time/randomness | Inject Clock/ID generator if needed |
| No demo/test | Show 3-5 scenario runs |
| No error handling | Use meaningful exceptions |

---

## 20. Strong Closing Answer

If interviewer asks:

> How do you approach a Java machine-coding problem?

Say:

```text
I start by clarifying requirements and identifying entities, operations, and constraints.
Then I design small model classes, a service for business logic, and repositories for storage.
I keep objects valid, use interfaces where behavior may vary, and add thread safety only
where required by the problem. I implement the happy path first, then edge cases, then a
few demo/test cases. I avoid overengineering but leave the design extensible.
```

---

## 21. Final Memory Trick

```text
Model the nouns.
Service the use cases.
Repository the storage.
Strategy the changing behavior.
Lock the invariant.
Test the edge cases.
```
