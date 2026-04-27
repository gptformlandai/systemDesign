# Intervue Round 2 - Java, Concurrency, Streams, Booking Scenarios Top Answers

> Goal: answer scenario-heavy Java backend interview questions clearly, technically, and confidently.

This sheet focuses on the exact areas likely for your second round:

- Java 8+ functional programming and streams
- Optional best practices
- Java 17/21 features
- Executor framework and concurrency
- CompletableFuture
- Locks and real booking-system consistency
- Thread-safe room booking task
- JVM memory, GC, heap dump debugging
- ConcurrentHashMap internals

---

# 0. How To Answer In Interview

Use this structure for every question:

```text
1. Define the concept.
2. Explain how it works internally.
3. Give real backend use case.
4. Mention trade-off or failure case.
5. Give one clean example.
```

Example:

```text
Streams are useful for readable data transformation, but I avoid very complex chains
or side effects because they become hard to debug.
```

That one line shows both knowledge and maturity.

---

# 1. Intermediate vs Terminal Stream Operations

## Question

Explain the difference between `map` / `filter` and `collect` / `reduce`.

When would you choose `collect` over `reduce`?

---

## Simple Explanation

A stream pipeline has two parts:

```text
Source -> intermediate operations -> terminal operation
```

Intermediate operations transform or filter the stream but do not execute immediately.

Terminal operations produce the final result and trigger execution.

---

## Intermediate Operations

Examples:

```java
filter()
map()
flatMap()
distinct()
sorted()
skip()
limit()
peek()
```

They are lazy.

Example:

```java
employees.stream()
    .filter(emp -> emp.getSalary() > 100000)
    .map(Employee::getName);
```

Nothing executes yet because there is no terminal operation.

---

## Terminal Operations

Examples:

```java
collect()
reduce()
count()
findFirst()
anyMatch()
forEach()
min()
max()
```

They trigger the pipeline.

Example:

```java
List<String> names = employees.stream()
    .filter(emp -> emp.getSalary() > 100000)
    .map(Employee::getName)
    .collect(Collectors.toList());
```

Here `collect` triggers execution.

---

## `collect` vs `reduce`

### `reduce`

Use `reduce` when you want to combine stream elements into one value.

Examples:

- sum
- product
- max custom value
- combined string

```java
int totalSalary = employees.stream()
    .map(Employee::getSalary)
    .reduce(0, Integer::sum);
```

Better numeric version:

```java
int totalSalary = employees.stream()
    .mapToInt(Employee::getSalary)
    .sum();
```

### `collect`

Use `collect` when you want to accumulate elements into a container or grouped result.

Examples:

- `List`
- `Set`
- `Map`
- grouping
- partitioning
- joining

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));
```

---

## Strong Interview Answer

```text
Intermediate operations like filter, map, flatMap, distinct, and sorted are lazy. They
build the pipeline but do not execute it. Terminal operations like collect, reduce, count,
findFirst, and forEach trigger execution and produce a result.

I use reduce when I want to collapse elements into a single immutable value, like sum or
max. I use collect when I want a mutable result container or complex aggregation, like
List, Map, groupingBy, partitioningBy, or joining.
```

---

## Common Trap

Bad:

```java
List<String> list = new ArrayList<>();
employees.stream()
    .map(Employee::getName)
    .forEach(list::add);
```

Better:

```java
List<String> list = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

Why:

```text
Use collect for collecting. Avoid side effects in forEach.
```

---

# 2. Parallel Streams and ForkJoinPool

## Question

How do `parallelStream()` and `ForkJoinPool` work under the hood?

When should you avoid parallel streams?

---

## Simple Explanation

`parallelStream()` splits data into chunks and processes chunks on multiple threads.

By default, it uses the common `ForkJoinPool`.

Mental model:

```text
List of 1,000,000 items
    -> split into smaller chunks
    -> process chunks in worker threads
    -> combine partial results
```

---

## ForkJoinPool Basics

ForkJoinPool is designed for divide-and-conquer work.

It uses:

- worker threads
- task splitting
- work stealing

Work stealing means:

```text
If one worker finishes early, it can steal tasks from another worker's queue.
```

---

## Example

```java
int total = numbers.parallelStream()
    .filter(n -> n % 2 == 0)
    .mapToInt(Integer::intValue)
    .sum();
```

This may process chunks in parallel.

---

## When Parallel Stream Helps

Good fit:

- large data set
- CPU-bound processing
- independent operations
- no shared mutable state
- expensive per-element computation

Example:

```java
List<Result> results = largeInput.parallelStream()
    .map(this::heavyCpuCalculation)
    .collect(Collectors.toList());
```

---

## When To Avoid Parallel Stream

Avoid when:

- data set is small
- operation is I/O-bound
- operation calls database/API
- shared mutable state is involved
- order matters strongly
- running inside web server request path without control
- common pool starvation is possible

Bad example:

```java
orders.parallelStream()
    .map(order -> paymentClient.callPaymentApi(order))
    .collect(Collectors.toList());
```

Why bad:

```text
This blocks ForkJoinPool threads on I/O and can starve unrelated parallel tasks.
```

---

## Strong Interview Answer

```text
parallelStream splits the source into chunks and processes them using ForkJoinPool,
usually the common pool. It works best for large CPU-bound, independent operations.
I avoid it for blocking I/O like DB calls or REST calls, small collections, shared mutable
state, or request-path code where I need explicit control over thread pools.
```

---

## Production Caveat

If you need parallel I/O, prefer controlled executors:

```java
ExecutorService executor = Executors.newFixedThreadPool(20);
```

or use reactive/non-blocking clients where appropriate.

---

# 3. Optional Best Practices

## Question

How do you avoid Optional abuse?

Explain `ifPresentOrElse` and `flatMap` with Optional.

---

## What Optional Is

`Optional<T>` represents a value that may or may not be present.

It helps avoid null checks when used properly.

---

## Good Usage

Good:

```java
Optional<User> user = userRepository.findById(id);
```

Then:

```java
User user = userRepository.findById(id)
    .orElseThrow(() -> new UserNotFoundException(id));
```

or:

```java
String email = userRepository.findById(id)
    .map(User::getEmail)
    .orElse("unknown@example.com");
```

---

## Optional Abuse

Bad:

```java
Optional<User> user = userRepository.findById(id);
if (user.isPresent()) {
    return user.get().getName();
}
return "Unknown";
```

Better:

```java
return userRepository.findById(id)
    .map(User::getName)
    .orElse("Unknown");
```

Worst:

```java
User user = userRepository.findById(id).get();
```

Why bad:

```text
get() throws NoSuchElementException if value is absent.
```

---

## `ifPresentOrElse`

Use when you want to perform actions for both present and absent cases.

```java
userRepository.findById(id)
    .ifPresentOrElse(
        user -> System.out.println("Found: " + user.getName()),
        () -> System.out.println("User not found")
    );
```

Use for side effects, not for returning values.

---

## `map` vs `flatMap` With Optional

Use `map` when mapper returns a normal value.

```java
Optional<String> email = userOptional.map(User::getEmail);
```

Use `flatMap` when mapper already returns Optional.

```java
Optional<Address> address = userOptional
    .flatMap(User::getAddressOptional);
```

Without `flatMap`, you get nested Optional:

```java
Optional<Optional<Address>>
```

---

## Strong Interview Answer

```text
I use Optional mainly as a return type to represent absence. I avoid calling get directly,
avoid Optional fields/parameters in most cases, and use map, flatMap, orElse, orElseGet,
and orElseThrow. ifPresentOrElse is useful for side effects when I need both present and
absent branches. flatMap avoids nested Optional when the mapping function already returns
an Optional.
```

---

## Important Trap: `orElse` vs `orElseGet`

`orElse` evaluates fallback immediately.

```java
User user = optionalUser.orElse(createDefaultUser());
```

`createDefaultUser()` runs even if optional has value.

`orElseGet` is lazy.

```java
User user = optionalUser.orElseGet(() -> createDefaultUser());
```

Strong line:

```text
Use orElseGet when fallback creation is expensive or has side effects.
```

---

# 4. Java 17/21 Features: Records, Sealed Classes, Virtual Threads

## Question

What are Records, Sealed Classes, and Virtual Threads?

How do they improve backend performance or readability?

---

## Records

Records are compact immutable data carriers.

Example:

```java
public record BookingRequest(
    String hotelId,
    String roomId,
    LocalDate checkIn,
    LocalDate checkOut
) {
}
```

Java automatically provides:

- constructor
- getters as accessor methods
- `equals`
- `hashCode`
- `toString`

Usage:

```java
BookingRequest request = new BookingRequest("H1", "R1", checkIn, checkOut);
System.out.println(request.hotelId());
```

## Where Records Help

Good for:

- DTOs
- API request/response models
- immutable data
- event payloads
- simple value objects

Avoid records when:

- object needs mutable state
- JPA entity needs proxy/no-arg constructor behavior
- complex lifecycle behavior exists

Strong answer:

```text
Records reduce boilerplate for immutable DTOs and value objects. They improve readability,
especially in API request/response classes and event payloads.
```

---

## Sealed Classes

Sealed classes restrict which classes can extend or implement them.

Example:

```java
public sealed interface PaymentResult
        permits PaymentSuccess, PaymentFailed, PaymentPending {
}

public record PaymentSuccess(String transactionId) implements PaymentResult {
}

public record PaymentFailed(String reason) implements PaymentResult {
}

public record PaymentPending(String referenceId) implements PaymentResult {
}
```

Why useful:

- controlled inheritance
- clearer domain modeling
- safer switch handling
- prevents random subclasses

Backend example:

```java
String message = switch (result) {
    case PaymentSuccess success -> "Paid: " + success.transactionId();
    case PaymentFailed failed -> "Failed: " + failed.reason();
    case PaymentPending pending -> "Pending: " + pending.referenceId();
};
```

Strong answer:

```text
Sealed classes let me model a closed set of domain outcomes, like payment success,
failure, and pending. They improve correctness because the compiler knows the allowed
subtypes.
```

---

## Virtual Threads

Virtual threads are lightweight threads introduced as a stable feature in Java 21.

They are useful for high-concurrency blocking I/O workloads.

Example:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i -> {
        executor.submit(() -> callBlockingApi(i));
    });
}
```

Mental model:

```text
Platform thread = expensive OS thread
Virtual thread  = lightweight JVM-managed thread
```

Virtual threads allow many concurrent blocking tasks without creating one OS thread per task.

## Where Virtual Threads Help

Good for:

- blocking HTTP calls
- JDBC calls
- file I/O
- many concurrent request handlers
- thread-per-request style with less thread cost

## Where They Do Not Help

Not magic for:

- CPU-bound work
- slow database itself
- limited DB connection pool
- poor locking
- bad downstream latency

Important:

```text
Virtual threads improve scalability of blocking workloads, but downstream limits still matter.
If DB pool has 20 connections, 10,000 virtual threads cannot run 10,000 DB queries at once.
```

Strong answer:

```text
Virtual threads make blocking I/O concurrency cheaper. They improve throughput for services
that spend time waiting on DB or HTTP calls, while keeping code simple. But they do not
make CPU faster and they do not remove the need for connection pool limits, timeouts, and
backpressure.
```

---

# 5. Executor Framework

## Question

Why is `ExecutorService` preferred over manual thread management?

Explain `FixedThreadPool`, `CachedThreadPool`, and `ScheduledThreadPool`.

---

## Why ExecutorService

Manual thread creation:

```java
new Thread(task).start();
```

Problems:

- no pooling
- no lifecycle management
- hard to limit concurrency
- hard to collect results
- hard to schedule tasks
- can create too many threads

ExecutorService provides:

- thread reuse
- task queue
- controlled concurrency
- `Future`
- shutdown management
- scheduling support

---

## FixedThreadPool

```java
ExecutorService executor = Executors.newFixedThreadPool(10);
```

Meaning:

```text
At most 10 worker threads.
Extra tasks wait in queue.
```

Use for:

- bounded concurrency
- CPU-bound tasks with fixed size
- controlling load on downstream service

Caution:

```text
Executors.newFixedThreadPool uses an unbounded queue internally. In high-load systems,
prefer ThreadPoolExecutor with a bounded queue.
```

---

## CachedThreadPool

```java
ExecutorService executor = Executors.newCachedThreadPool();
```

Meaning:

```text
Creates threads as needed and reuses idle threads.
```

Use for:

- short-lived async tasks
- bursty workloads when upper bound is controlled externally

Caution:

```text
CachedThreadPool can create many threads under load. This can exhaust memory or CPU.
```

---

## ScheduledThreadPool

```java
ScheduledExecutorService scheduler =
    Executors.newScheduledThreadPool(5);
```

Use for:

- delayed tasks
- periodic jobs
- cleanup tasks
- retry scheduling

Example:

```java
scheduler.schedule(
    () -> releaseExpiredRoomHolds(),
    5,
    TimeUnit.MINUTES
);
```

---

## Strong Interview Answer

```text
ExecutorService is preferred because it separates task submission from thread management.
It reuses threads, controls concurrency, supports Future results, and provides lifecycle
methods like shutdown. FixedThreadPool gives a fixed number of workers, CachedThreadPool
grows as needed and can be risky under load, and ScheduledThreadPool is used for delayed
or periodic tasks.
```

---

## Production Version

For real systems, prefer explicit `ThreadPoolExecutor`:

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,
    20,
    60,
    TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(1000),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

Why:

```text
Bounded queue + rejection policy protects the service under load.
```

---

# 6. CompletableFuture

## Question

How do you chain multiple asynchronous tasks?

How do you handle exceptions using `exceptionally` or `handle`?

---

## Basic Async Task

```java
CompletableFuture<User> userFuture =
    CompletableFuture.supplyAsync(() -> userService.getUser(userId));
```

---

## `thenApply`

Use when next step transforms result synchronously.

```java
CompletableFuture<String> nameFuture = userFuture
    .thenApply(User::getName);
```

Meaning:

```text
User -> String
```

---

## `thenCompose`

Use when next step returns another CompletableFuture.

```java
CompletableFuture<LoyaltyProfile> profileFuture =
    userFuture.thenCompose(user ->
        loyaltyClient.getProfileAsync(user.getId())
    );
```

Meaning:

```text
CompletableFuture<User> -> CompletableFuture<LoyaltyProfile>
```

Use `thenCompose` to avoid nested futures:

```java
CompletableFuture<CompletableFuture<LoyaltyProfile>>
```

---

## `thenCombine`

Use when two independent async calls can run in parallel and then combine.

```java
CompletableFuture<User> userFuture =
    CompletableFuture.supplyAsync(() -> userService.getUser(userId));

CompletableFuture<List<Booking>> bookingsFuture =
    CompletableFuture.supplyAsync(() -> bookingService.getBookings(userId));

CompletableFuture<UserBookingSummary> summaryFuture =
    userFuture.thenCombine(bookingsFuture,
        (user, bookings) -> new UserBookingSummary(user, bookings)
    );
```

---

## `allOf`

Use when many futures must complete.

```java
CompletableFuture<Void> all = CompletableFuture.allOf(
    userFuture,
    bookingsFuture
);

all.join();
```

To collect results:

```java
User user = userFuture.join();
List<Booking> bookings = bookingsFuture.join();
```

---

## Exception Handling: `exceptionally`

Use for fallback on exception.

```java
CompletableFuture<User> userFuture =
    CompletableFuture.supplyAsync(() -> userService.getUser(userId))
        .exceptionally(ex -> {
            log.error("Failed to fetch user", ex);
            return User.guest();
        });
```

Meaning:

```text
If pipeline fails, return fallback value.
```

---

## Exception Handling: `handle`

Use when you need access to both result and exception.

```java
CompletableFuture<String> result = CompletableFuture
    .supplyAsync(() -> paymentService.charge(request))
    .handle((paymentResult, ex) -> {
        if (ex != null) {
            return "PAYMENT_FAILED";
        }
        return paymentResult.status();
    });
```

Difference:

| Method | Use |
|---|---|
| `exceptionally` | Only handles exception and returns fallback |
| `handle` | Handles both success and failure |
| `whenComplete` | Observes success/failure but does not recover |

---

## Strong Interview Answer

```text
I use thenApply for simple transformations, thenCompose when the next call is also async,
thenCombine for independent parallel calls, and allOf when waiting for multiple tasks.
For exceptions, exceptionally gives a fallback, handle lets me process both result and
exception, and whenComplete is useful for logging without changing the result.
```

---

# 7. Locking: synchronized vs ReentrantLock

## Question

Compare `synchronized` blocks vs `ReentrantLock`.

When would you use `ReadWriteLock` or optimistic locking?

---

## `synchronized`

Simple built-in locking.

```java
public synchronized void book() {
    // critical section
}
```

or:

```java
synchronized (lock) {
    // critical section
}
```

Pros:

- simple
- automatic unlock
- readable for basic cases

Cons:

- cannot try lock
- cannot interrupt waiting easily
- less flexible
- one monitor lock

---

## ReentrantLock

More flexible explicit lock.

```java
private final ReentrantLock lock = new ReentrantLock();

public void book() {
    lock.lock();
    try {
        // critical section
    } finally {
        lock.unlock();
    }
}
```

Pros:

- `tryLock`
- interruptible lock
- timed lock
- fairness option
- multiple conditions

Cons:

- must unlock in finally
- more verbose
- easier to misuse

---

## Strong Comparison

```text
synchronized is simpler and safer for basic mutual exclusion because unlock happens
automatically. ReentrantLock is better when I need advanced features like tryLock,
timeout, interruptible waiting, fairness, or multiple condition variables.
```

---

## ReadWriteLock

Use when reads are frequent and writes are rare.

```java
private final ReadWriteLock lock = new ReentrantReadWriteLock();

public Room getRoom(String id) {
    lock.readLock().lock();
    try {
        return rooms.get(id);
    } finally {
        lock.readLock().unlock();
    }
}

public void updateRoom(Room room) {
    lock.writeLock().lock();
    try {
        rooms.put(room.getId(), room);
    } finally {
        lock.writeLock().unlock();
    }
}
```

Meaning:

```text
Multiple readers can read together, but writer gets exclusive access.
```

Use for:

- cache reads
- configuration reads
- room inventory read-heavy store

Avoid when:

- writes are frequent
- lock contention is low
- complexity is not justified

---

## Optimistic Locking

Optimistic locking assumes conflicts are rare.

In JPA:

```java
@Version
private Long version;
```

Flow:

```text
1. Transaction reads row with version 1.
2. Another transaction updates row to version 2.
3. First transaction tries update with version 1.
4. Update fails with OptimisticLockException.
```

Use when:

- conflicts are possible but not constant
- you want better concurrency
- database consistency matters

Booking example:

```text
Two users try to book same room.
Only one transaction updates the availability row successfully.
Other gets optimistic lock failure and must retry or show unavailable.
```

Strong answer:

```text
For booking systems, I prefer database-level correctness using unique constraints,
transactions, and optimistic or pessimistic locking. In-memory locks alone are not enough
in multi-instance microservices.
```

---

# 8. CountDownLatch, CyclicBarrier, Semaphore

## Question

Explain real-world use cases for `CountDownLatch`, `CyclicBarrier`, and `Semaphore`.

---

## CountDownLatch

One or more threads wait until other tasks complete.

It is one-time use.

Example:

```java
CountDownLatch latch = new CountDownLatch(3);

executor.submit(() -> {
    loadHotels();
    latch.countDown();
});

executor.submit(() -> {
    loadRooms();
    latch.countDown();
});

executor.submit(() -> {
    loadPrices();
    latch.countDown();
});

latch.await();
System.out.println("All data loaded");
```

Use cases:

- wait for multiple startup tasks
- wait for parallel API calls in test
- coordinate test threads

Strong line:

```text
CountDownLatch is a one-time gate that opens after count reaches zero.
```

---

## CyclicBarrier

Multiple threads wait for each other at a common point.

Reusable after all parties arrive.

Example:

```java
CyclicBarrier barrier = new CyclicBarrier(3, () ->
    System.out.println("All services reached checkpoint")
);

Runnable task = () -> {
    prepare();
    barrier.await();
    executeTogether();
};
```

Use cases:

- simulation
- batch phases
- parallel workers that must start next phase together

Strong line:

```text
CyclicBarrier is reusable and waits until all participating threads reach the barrier.
```

---

## Semaphore

Semaphore limits concurrent access to a resource.

Example:

```java
Semaphore semaphore = new Semaphore(10);

public void callPartnerApi() {
    semaphore.acquire();
    try {
        partnerClient.call();
    } finally {
        semaphore.release();
    }
}
```

Use cases:

- limit API calls
- limit DB-heavy operations
- limit file uploads
- limit room booking attempts per hotel

Strong line:

```text
Semaphore is useful for throttling. It allows only a fixed number of concurrent permits.
```

---

## Quick Difference

| Utility | Purpose | Reusable |
|---|---|---|
| CountDownLatch | Wait until count reaches zero | No |
| CyclicBarrier | Wait until all parties reach barrier | Yes |
| Semaphore | Limit concurrent access | Yes |

---

# 9. Idempotency In Booking Systems

## Question

How do you ensure idempotency when multiple threads or services process the same booking request?

---

## Problem

Same booking request can come multiple times because of:

- user double-click
- network retry
- client timeout retry
- message redelivery
- multiple service instances

Without idempotency:

```text
same request -> two bookings
same payment -> double charge
same event -> duplicate loyalty points
```

---

## Correct Approach

Use an idempotency key.

Example request:

```http
POST /bookings
Idempotency-Key: abc-123
```

Store it in database:

```sql
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    booking_id VARCHAR(100),
    status VARCHAR(30),
    response_body JSONB,
    created_at TIMESTAMP
);
```

Flow:

```text
1. Request comes with idempotency key.
2. Try to insert key with PROCESSING status.
3. If insert succeeds, process booking.
4. Save final response against key.
5. If same key comes again, return stored response.
```

---

## Why Database Constraint Matters

In microservices, multiple app instances may receive same request.

In-memory lock only protects one JVM.

Database unique constraint protects all instances.

Strong line:

```text
For idempotency in distributed systems, I rely on a durable unique key in the database,
not only in-memory synchronization.
```

---

## Strong Interview Answer

```text
I make booking APIs idempotent using an idempotency key and database unique constraint.
The first request stores the key and processes the booking. If the same request is retried,
the system returns the existing result instead of creating another booking. For event
consumers, I store processed event IDs to avoid duplicate processing.
```

---

# 10. Practical Coding Task: Thread-Safe Room Booking

## Question

Write a thread-safe method to book a room.

Use `ConcurrentHashMap` or proper locking to prevent overbooking.

Handle overlapping dates using Java 8 `LocalDate`.

---

## Important Clarification

This in-memory solution is good for coding interview.

But in real microservices, use database transactions and constraints because there may be multiple service instances.

Say this in interview:

```text
For the coding task I will make it thread-safe inside one JVM. In production, I would also
enforce this with database constraints/locking because multiple application instances may
process bookings.
```

---

## Date Overlap Rule

Two ranges overlap if:

```text
newStart < existingEnd AND newEnd > existingStart
```

For hotel stays:

```text
checkIn inclusive
checkOut exclusive
```

Example:

```text
Booking A: Apr 10 -> Apr 12
Booking B: Apr 12 -> Apr 15
```

No overlap, because first guest checks out on Apr 12 and second checks in on Apr 12.

---

## Code

```java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class RoomInventoryManager {

    private final Map<String, List<Booking>> bookingsByRoom = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locksByRoom = new ConcurrentHashMap<>();

    public boolean bookRoom(String roomId,
                            String bookingId,
                            LocalDate checkIn,
                            LocalDate checkOut) {

        validate(roomId, bookingId, checkIn, checkOut);

        ReentrantLock lock = locksByRoom.computeIfAbsent(roomId, id -> new ReentrantLock());

        lock.lock();
        try {
            List<Booking> bookings = bookingsByRoom.computeIfAbsent(
                roomId,
                id -> new ArrayList<>()
            );

            boolean overlaps = bookings.stream()
                .anyMatch(existing -> overlaps(
                    checkIn,
                    checkOut,
                    existing.checkIn(),
                    existing.checkOut()
                ));

            if (overlaps) {
                return false;
            }

            bookings.add(new Booking(bookingId, roomId, checkIn, checkOut));
            return true;
        } finally {
            lock.unlock();
        }
    }

    private boolean overlaps(LocalDate newStart,
                             LocalDate newEnd,
                             LocalDate existingStart,
                             LocalDate existingEnd) {
        return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
    }

    private void validate(String roomId,
                          String bookingId,
                          LocalDate checkIn,
                          LocalDate checkOut) {
        if (roomId == null || roomId.isBlank()) {
            throw new IllegalArgumentException("roomId is required");
        }
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("bookingId is required");
        }
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("dates are required");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("checkIn must be before checkOut");
        }
    }

    public record Booking(
        String bookingId,
        String roomId,
        LocalDate checkIn,
        LocalDate checkOut
    ) {
    }
}
```

---

## Why This Is Thread-Safe

```text
ConcurrentHashMap protects map-level concurrent access.
Per-room ReentrantLock ensures only one thread checks and inserts bookings for the same
room at a time.
Different rooms can still be booked concurrently.
```

This is better than one global lock.

Bad:

```text
lock entire hotel inventory
```

Better:

```text
lock only room R101 while booking R101
```

---

## Edge Cases Covered

- null room ID
- blank booking ID
- null dates
- check-in after check-out
- same-day invalid booking
- overlapping dates
- back-to-back booking allowed
- concurrent booking attempts for same room
- concurrent booking attempts for different rooms

---

## Strong Interview Explanation

```text
I use ConcurrentHashMap for concurrent access and a per-room ReentrantLock to protect the
check-then-insert critical section. The overlap check uses checkIn inclusive and checkOut
exclusive logic: newStart < existingEnd and newEnd > existingStart. This prevents two
threads from booking the same room for overlapping dates while still allowing different
rooms to be booked in parallel.
```

---

## Production Answer

```text
In production, I would not rely only on in-memory locks because we may have multiple
service instances. I would enforce correctness at the database level using transactions,
unique constraints or exclusion constraints for date ranges, optimistic/pessimistic locking,
and idempotency keys.
```

---

# 11. JVM Architecture, Memory, GC, Heap Dumps

## Question

Deep dive into memory management, G1 vs ZGC, and identifying memory leaks using heap dumps.

---

## JVM Memory Areas

| Area | Stores |
|---|---|
| Heap | Objects and arrays |
| Stack | Method frames, local variables, references |
| Metaspace | Class metadata |
| PC Register | Current instruction pointer per thread |
| Native Method Stack | Native method execution |

---

## Heap

Heap stores objects.

Example:

```java
Employee emp = new Employee();
```

`emp` reference may be on stack, but object is on heap.

---

## Stack

Each thread has its own stack.

Stores:

- method calls
- local variables
- references
- partial results

Stack overflow usually happens due to deep recursion.

---

## Metaspace

Stores class metadata.

Examples:

- class structure
- method metadata
- runtime constant pool metadata

Metaspace is native memory, not heap.

---

## Garbage Collection Basics

GC removes unreachable objects.

Object is eligible for GC when it is no longer reachable from GC roots.

GC roots include:

- local variables in active threads
- static fields
- JNI references
- active class loaders

---

## G1 GC

G1 means Garbage First.

Key ideas:

- region-based heap
- tries to meet pause-time goals
- collects regions with most garbage first
- good default for many backend services

Good for:

- general server applications
- medium/large heaps
- balanced throughput and latency

Strong line:

```text
G1 is a region-based collector designed to provide predictable pause times while maintaining
good throughput.
```

---

## ZGC

ZGC is a low-latency garbage collector.

Key ideas:

- mostly concurrent work
- very short pause times
- designed for large heaps and low latency

Good for:

- latency-sensitive systems
- large heaps
- services where long GC pauses are unacceptable

Trade-off:

```text
ZGC may use more CPU overhead to keep pauses very low.
```

Strong line:

```text
ZGC focuses on very low pause times by doing most GC work concurrently. It is useful when
latency is more important than maximum throughput.
```

---

## G1 vs ZGC

| Area | G1 | ZGC |
|---|---|---|
| Goal | Balanced throughput and pause control | Very low latency |
| Heap | Region-based | Region-based, highly concurrent |
| Pause | Low to moderate | Very low |
| CPU overhead | Usually moderate | Can be higher |
| Use case | General backend services | Latency-sensitive large services |

Interview answer:

```text
For most Spring Boot services, G1 is a good default. If the service has strict latency
requirements or very large heaps where GC pauses hurt user experience, ZGC can be considered.
But GC choice should be based on metrics, GC logs, and latency requirements.
```

---

## Memory Leak In Java

Java memory leak means objects are no longer useful but still reachable.

Common causes:

- static collections
- unbounded caches
- ThreadLocal not cleared
- listeners not removed
- open resources
- classloader leaks
- queues growing forever
- storing request objects accidentally

---

## Heap Dump Debugging Flow

1. Observe symptoms:
   - high heap usage
   - frequent full GC
   - `OutOfMemoryError`
   - increasing memory after traffic stabilizes

2. Capture heap dump:

```bash
jcmd <pid> GC.heap_dump /tmp/app.hprof
```

or JVM option:

```text
-XX:+HeapDumpOnOutOfMemoryError
```

3. Analyze using tools:
   - Eclipse MAT
   - VisualVM
   - YourKit/JProfiler

4. Look at:
   - dominator tree
   - retained heap
   - biggest object graphs
   - GC roots
   - duplicate strings/collections

5. Find why objects are still reachable.

---

## Strong Interview Answer

```text
To debug a memory leak, I first confirm memory growth using metrics and GC logs. Then I
capture a heap dump and analyze retained heap using MAT or VisualVM. I look for large
collections, ThreadLocal values, static references, unbounded caches, or queues. The key
is not just finding large objects but finding the GC root path that keeps them reachable.
```

---

# 12. ConcurrentHashMap Internals

## Question

How does ConcurrentHashMap achieve high concurrency compared to Hashtable?

Explain segment locking vs bucket-level locking.

---

## Hashtable

Hashtable synchronizes most public methods.

Simplified:

```java
public synchronized V get(Object key) {
    // ...
}

public synchronized V put(K key, V value) {
    // ...
}
```

Problem:

```text
Only one thread can access the map at a time for synchronized operations.
```

This limits concurrency.

---

## ConcurrentHashMap

ConcurrentHashMap is designed for concurrent access.

Important features:

- no global lock for most operations
- concurrent reads
- fine-grained locking for updates
- CAS where possible
- does not allow null keys or values

---

## Segment Locking

Older Java versions used segment-based locking.

Mental model:

```text
ConcurrentHashMap
  Segment 1 -> buckets
  Segment 2 -> buckets
  Segment 3 -> buckets
```

Different threads could update different segments concurrently.

---

## Java 8+ Bucket-Level Locking

Java 8 changed the internal design.

It no longer uses fixed segment array in the same old way.

It uses:

- CAS for empty bucket insert
- synchronized on bucket/bin node for collisions/updates
- volatile reads for visibility
- tree bins for high collisions

Simplified flow for `put`:

```text
1. Calculate hash.
2. Find bucket.
3. If bucket empty, use CAS to insert.
4. If bucket not empty, lock that bucket/bin.
5. Update linked list or tree.
```

This means different buckets can be updated concurrently.

---

## Why Null Is Not Allowed

ConcurrentHashMap does not allow null key or null value.

Reason:

```text
In concurrent context, null return from get could mean key not present. If null values
were allowed, it would be ambiguous.
```

---

## Strong Interview Answer

```text
Hashtable synchronizes entire methods, so it effectively uses one lock for the whole map.
ConcurrentHashMap uses much finer-grained concurrency. Older versions used segment locking.
Java 8+ uses CAS for empty buckets and locks only the affected bucket/bin for updates.
Reads are mostly non-blocking. This allows multiple threads to access and update different
buckets concurrently.
```

---

# 13. Final Rapid Revision

## If Interviewer Says X, Answer With Y

| Question | Key Answer |
|---|---|
| Intermediate vs terminal | Intermediate lazy, terminal executes |
| collect vs reduce | collect for containers/grouping, reduce for single value |
| parallelStream | ForkJoinPool, good for CPU-bound large independent work |
| avoid parallelStream | Avoid blocking I/O, small data, shared mutable state |
| Optional abuse | Avoid get, use map/flatMap/orElseThrow |
| Records | Immutable DTO/value carrier, less boilerplate |
| Sealed class | Restricted hierarchy, safer domain modeling |
| Virtual threads | Cheap blocking I/O concurrency, not CPU magic |
| ExecutorService | Thread reuse, lifecycle, controlled concurrency |
| FixedThreadPool | Fixed workers, queue extra tasks |
| CachedThreadPool | Grows as needed, risky under load |
| ScheduledThreadPool | Delayed/periodic execution |
| CompletableFuture chain | thenApply, thenCompose, thenCombine, allOf |
| CompletableFuture exception | exceptionally for fallback, handle for success/failure |
| synchronized | Simple automatic lock |
| ReentrantLock | tryLock, timeout, fairness, interruptible |
| ReadWriteLock | Many reads, few writes |
| Optimistic locking | Version check, good for booking conflicts |
| CountDownLatch | One-time wait for tasks |
| CyclicBarrier | Reusable phase barrier |
| Semaphore | Limit concurrent access |
| Idempotency | Unique key + stored response/result |
| Room booking | Per-room lock + overlap check |
| JVM leak | Reachable but unused objects |
| Heap dump | Analyze retained heap and GC roots |
| G1 | Balanced low-pause default server GC |
| ZGC | Very low latency GC |
| ConcurrentHashMap | CAS + bucket/bin locking, not whole-map locking |

---

# 14. Five Answers To Memorize Before Joining

## 1. Streams

```text
Intermediate stream operations are lazy and return another stream. Terminal operations
trigger execution. I use collect for building collections or grouping data, and reduce
for combining elements into one value.
```

## 2. Parallel Stream

```text
parallelStream uses ForkJoinPool and works best for large CPU-bound independent tasks.
I avoid it for blocking DB/API calls, small collections, and shared mutable state.
```

## 3. Booking Idempotency

```text
For booking idempotency, I use an idempotency key with a database unique constraint.
If the same request is retried, I return the already stored result instead of creating
another booking or charging twice.
```

## 4. Thread-Safe Booking

```text
Inside one JVM, I can use ConcurrentHashMap plus a per-room lock so only one thread checks
and inserts bookings for the same room at a time. In production, I also enforce correctness
with DB transactions and locking because multiple service instances may run.
```

## 5. JVM Memory Leak

```text
A Java memory leak happens when unused objects are still reachable. I debug it by checking
metrics and GC logs, taking a heap dump, and analyzing retained heap and GC root paths for
static collections, ThreadLocal leaks, unbounded caches, or growing queues.
```

