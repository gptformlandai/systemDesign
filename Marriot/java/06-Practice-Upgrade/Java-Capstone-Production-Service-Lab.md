# Java Capstone Production Service Lab

Target: convert the Java track into a single end-to-end project that proves beginner-to-pro mastery.

Time box:
- Short pass: 2-3 hours.
- Full pass: 1-2 days.
- Interview simulation pass: explain the design in 15 minutes, then code the core in 60-90 minutes.

---

## 1. Intuition

The capstone is where separate Java topics stop being isolated flashcards.

```text
Core Java
    -> models and validation
Collections
    -> in-memory indexes
Concurrency
    -> one-JVM correctness
JDBC
    -> durable correctness
Data formats
    -> external contracts
Testing
    -> proof
Profiling
    -> runtime evidence
Packaging
    -> real execution
```

Beginner line:

```text
Build a small booking service twice: first in memory to learn Java mechanics, then with JDBC
thinking to learn production correctness.
```

---

## 2. Definition

- Definition: A capstone is a focused project that combines multiple concepts into one runnable, testable, explainable system.
- Category: Practice upgrade and interview simulation.
- Core idea: prove Java mastery through a small service with clean models, safe state changes, tests, diagnostics, and production trade-offs.

---

## 3. Why It Exists

Reading notes builds recognition. Capstones build recall and execution.

Without a capstone:

- You may know HashMap but not choose the right index.
- You may know `synchronized` but not protect an invariant.
- You may know JDBC but not explain where transactions belong.
- You may know DTOs but still expose domain objects directly.
- You may know JFR but not say when to collect it.

---

## 4. Reality

Interviewers do not only ask definitions. They ask:

- "Design a booking system."
- "Make it thread-safe."
- "What breaks with multiple instances?"
- "How would you persist it?"
- "How would you test it?"
- "How would you debug p99 latency?"
- "How would you expose this API?"

This lab gives one concrete answer path.

---

## 5. How It Works

Build in layers:

1. Domain model.
2. Repository abstraction.
3. In-memory repository.
4. Booking service.
5. DTO/API contract layer.
6. Tests.
7. Optional JDBC persistence design.
8. Profiling/debugging runbook.
9. Packaging command.

Failure path to discuss:

1. Two threads book the same room/date.
2. In-memory check and insert are not atomic.
3. Duplicate booking appears.
4. Add one-JVM synchronization.
5. Then explain why database constraints/transactions are still required across instances.

Recovery path:

1. Protect invariant in service/repository.
2. Add unit and concurrency tests.
3. Add database unique/exclusion constraint in production design.
4. Add idempotency key for retry safety.
5. Add metrics and logs.

---

## 6. What Problem It Solves

- Primary problem solved: bridges Java concept knowledge and system behavior.
- Secondary benefits: interview confidence, code organization, production language.
- Systems impact: teaches where Java-level safety ends and database/distributed safety begins.

---

## 7. When To Rely On It

Use this lab:

- After finishing the Java starter/intermediate path.
- Before machine-coding interviews.
- Before senior Java rounds.
- When you can explain concepts but freeze while coding.
- When you need one project story to discuss in interviews.

---

## 8. When Not To Overbuild It

Do not turn this into a full Spring Boot product unless that is the explicit goal.

Keep the first version plain Java:

- No database dependency required for the first pass.
- No web framework required for the first pass.
- No ORM required.
- No distributed system claims.

Then explain the production migration.

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Connects many Java topics | Takes longer than reading |
| Creates interview story | Requires honest testing |
| Reveals weak spots fast | Can tempt overengineering |
| Practices clean code | Needs time box |
| Builds production judgment | Plain Java version is not full production |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- In-memory implementation is fast and interview-friendly, but not multi-instance safe.
- Coarse locking is simple, but may reduce concurrency.
- Fine-grained locking is faster, but more bug-prone.
- JDBC persistence improves durability, but adds schema, transactions, and operational concerns.
- DTOs add mapping work, but protect contracts.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Put everything in `main` | Hard to test and explain | Model/service/repository split |
| Use `ConcurrentHashMap` alone | Check-then-act still risky | Atomic critical section |
| Ignore date overlap edge cases | Duplicate bookings | Test overlap boundaries |
| Claim in-memory is production safe | Multiple instances break it | Use database constraints/transactions |
| Use `double` for price | Precision risk | Use `BigDecimal` or cents |
| No tests | Cannot prove invariant | Unit plus concurrency tests |

---

## 11. Key Numbers

| Item | Target |
|---|---|
| Core classes | 8-12 |
| Unit tests | 8+ |
| Concurrency test | 1+ |
| Public service methods | 4-6 |
| Main demo cases | 5+ |
| Explanation time | 10-15 minutes |
| Machine-coding core | 60-90 minutes |

---

## 12. Failure Modes

| Failure | User Observes | Fix |
|---|---|---|
| Overlap bug | duplicate room/date booking | correct interval overlap logic |
| Race condition | intermittent duplicate booking | lock around check+insert |
| Mutable returned list | caller corrupts repository | defensive copy |
| Invalid model state | impossible booking exists | constructor/factory validation |
| Retry duplicate | same request creates two bookings | idempotency key |
| Multi-instance duplicate | two app nodes accept same booking | DB transaction/constraint |
| Slow p99 | API waits on lock/DB | metrics, profiling, indexes |

---

## 13. Scenario

- Product / system: Hotel room booking service.
- Why this concept fits: it naturally exercises object modeling, collections, concurrency, transactions, DTOs, and testing.
- What would go wrong without it: Java concepts remain memorized but not executable.

---

## 14. Code Sample

Core overlap rule:

```java
import java.time.LocalDate;

public record DateRange(LocalDate startInclusive, LocalDate endExclusive) {
    public DateRange {
        if (startInclusive == null || endExclusive == null) {
            throw new IllegalArgumentException("dates are required");
        }
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("start must be before end");
        }
    }

    public boolean overlaps(DateRange other) {
        return startInclusive.isBefore(other.endExclusive)
            && other.startInclusive.isBefore(endExclusive);
    }
}
```

Why this matters:

- `[Jan 1, Jan 3)` and `[Jan 3, Jan 5)` do not overlap.
- `[Jan 1, Jan 4)` and `[Jan 3, Jan 5)` do overlap.
- The rule is small enough to test thoroughly.

---

## 15. Mini Program / Simulation

Minimum class shape:

```text
booking/
  model/
    Booking.java
    BookingStatus.java
    DateRange.java
    Room.java
  repository/
    BookingRepository.java
    InMemoryBookingRepository.java
  service/
    BookingService.java
    BookingRequest.java
    BookingResult.java
  app/
    BookingDemo.java
```

Repository contract:

```java
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Optional<Booking> findById(String bookingId);
    List<Booking> findByRoomId(String roomId);
    Booking save(Booking booking);
    void cancel(String bookingId);
}
```

Service invariant:

```java
public Booking book(String roomId, DateRange range, String userId) {
    synchronized (lockFor(roomId)) {
        boolean overlap = repository.findByRoomId(roomId).stream()
            .filter(booking -> booking.status() == BookingStatus.CONFIRMED)
            .anyMatch(booking -> booking.range().overlaps(range));

        if (overlap) {
            throw new BookingConflictException(roomId, range);
        }

        Booking booking = Booking.confirmed(newBookingId(), roomId, userId, range);
        return repository.save(booking);
    }
}
```

Debrief:

1. What invariant is protected?
2. Why is check+insert inside the same critical section?
3. Why is this still not enough across multiple app instances?
4. What database constraint or transaction would you add?

---

## 16. Practical Question

> You built an in-memory Java booking service. How would you evolve it into production without losing correctness?

---

## 17. Strong Answer

I would keep the domain model and service boundary, but move the source of truth from in-memory collections to a database. The one-JVM lock protects only a single process, so production correctness needs a transaction plus a database-level constraint or locking strategy that prevents overlapping bookings. I would add idempotency keys for retried requests, DTOs for API contracts, validation at boundaries, structured logs with bookingId/roomId, metrics for conflict rate and latency, and integration tests using a real database container. I would also define a rollback path and inspect p99 latency with traces and JFR if the booking path becomes slow.

---

## 18. Revision Notes

- One-line summary: the capstone proves Java mastery by protecting a real invariant from model to runtime to production design.
- Three keywords: invariant, transaction, evidence.
- One interview trap: one-JVM thread safety is not distributed correctness.
- One memory trick: model cleanly, protect locally, enforce durably, observe in production.
