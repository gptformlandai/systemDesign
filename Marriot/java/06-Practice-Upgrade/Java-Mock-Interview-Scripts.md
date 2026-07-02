# Java Mock Interview Scripts

Goal: simulate real Java backend interview pressure.

How to run a mock:
- Set a timer.
- Answer aloud.
- Do not pause to read notes.
- Record red/yellow topics after the round.
- Use `Java-Interview-Scoring-Rubrics.md` to grade.

Interviewer rule: ask follow-ups until the candidate proves internals, code intuition, and production judgment.

---

## Mock 1. 30-Minute Core Java Screen

Best after reading:
- `01-Starter-Path/Java-Core-Hot-Interview-Master-Sheet.md`
- `01-Starter-Path/Java-String-Deep-Dive.md`
- `01-Starter-Path/Java-8-Plus-Concepts-Interview-Prep.md`

### Interview Flow

| Time | Segment | Goal |
|---:|---|---|
| 0-3 min | Warm-up | Java execution and runtime basics |
| 3-10 min | Memory and OOP | Heap/stack, immutability, equality |
| 10-17 min | Collections | HashMap and key correctness |
| 17-23 min | Java 8+ | lambdas, Optional, Date-Time, CompletableFuture basics |
| 23-28 min | Traps | String, final/static, exception handling |
| 28-30 min | Candidate summary | Concise closing answer |

### Questions

1. Walk me through how Java code runs from `.java` to execution.
2. What is the difference between JDK, JRE, and JVM?
3. Where do objects, references, class metadata, and string literals live?
4. Explain `final`, `finally`, and `finalize`.
5. What makes a class immutable?
6. Why must `equals` and `hashCode` be consistent?
7. How does HashMap work internally?
8. What happens if a mutable object is used as a HashMap key?
9. Why is String immutable?
10. Why does `String a = "java"; String b = new String("java"); a == b` return false?
11. What problem did lambdas solve?
12. When should you avoid Optional?
13. Explain `orElse` vs `orElseGet`.
14. What is a checked exception? When would you create one?
15. Give me a 60-second summary of Java core concepts that matter in backend interviews.

### Follow-Up Pressure

Use these if the first answer is too shallow:
- What exactly is stored in stack vs heap?
- How does HashMap find the bucket?
- What changes after Java 8 collision treeification?
- How would you make a defensive copy?
- Why is `final List` still mutable?
- How can `String ==` sometimes return true?
- What is the production bug caused by bad `hashCode`?

### Strong Closing Pattern

```text
For backend Java interviews, I anchor core Java around runtime, memory, object equality,
collections, exceptions, and modern language features. I try to connect each concept to a
bug class: bad HashMap keys, String comparison mistakes, mutable shared state, resource leaks,
or unclear exception boundaries.
```

---

## Mock 2. 45-Minute Streams And Collections Round

Best after reading:
- `01-Starter-Path/Java-Streams-Interview-Prep.md`
- `01-Starter-Path/Java-Streams-Collectors-End-to-End-Examples-Gold-Sheet.md`
- `02-Intermediate-Backend/Java-Collections-Internals-Concurrent-Collections-FAANG-Master-Sheet.md`
- `04-Scenario-Practice/Java-Collectors-Terminal-Operators-Gold-Sheet.md`

### Interview Flow

| Time | Segment | Goal |
|---:|---|---|
| 0-5 min | Collection choices | Pick correct data structures |
| 5-15 min | HashMap internals | Buckets, collisions, resizing |
| 15-30 min | Streams coding | map/filter/group/sort/reduce |
| 30-38 min | Collectors traps | duplicate keys, grouping, downstream collectors |
| 38-43 min | Parallel stream judgment | performance and safety |
| 43-45 min | Summary | Design trade-off answer |

### Coding Data Model

Use this model during the round:

```java
record Order(String id, String customerId, String category, double amount, boolean paid) {}
```

### Questions

1. When would you choose ArrayList, LinkedList, HashSet, TreeSet, HashMap, TreeMap, or PriorityQueue?
2. Why is PriorityQueue iteration not sorted?
3. Explain HashMap resize and collision handling.
4. What is a fail-fast iterator?
5. What does weakly consistent iteration mean?
6. Given a list of orders, group paid orders by category.
7. Find total paid amount by customer.
8. Find top three categories by revenue.
9. Convert a list of orders into a map by id. What happens with duplicate ids?
10. Use `toMap` with a merge function.
11. Use `partitioningBy` to split paid and unpaid orders.
12. Explain `map` vs `flatMap` using an order with line items.
13. Explain `reduce` vs `collect`.
14. Why can parallel streams be slower?
15. When would you choose a loop over a stream?

### Follow-Up Pressure

- What is the Big-O of your chosen operation?
- Is the returned collection mutable?
- Does your stream preserve encounter order?
- What if amount is `BigDecimal` instead of `double`?
- What if orders are huge and cannot fit in memory?
- What if the transformation has side effects?
- What if this is inside a request path with high traffic?

### Strong Closing Pattern

```text
I use collections for data-shape choices and streams for clear transformations. For interview
code, I keep streams readable, handle duplicate keys explicitly, avoid side effects, and avoid
parallel streams unless the workload is CPU-heavy, independent, large enough, and measured.
```

---

## Mock 3. 60-Minute Java Concurrency Round

Best after reading:
- `02-Intermediate-Backend/Java-Concurrency-Deep-Dive-FAANG-Master-Sheet.md`
- `02-Intermediate-Backend/Java-Collections-Internals-Concurrent-Collections-FAANG-Master-Sheet.md`
- `03-Senior-FAANG/Java-Virtual-Threads-Modern-Concurrency-FAANG-Master-Sheet.md`
- `04-Scenario-Practice/Java-ConcurrentHashMap-Request-Scenario-Gold-Sheet.md`

### Interview Flow

| Time | Segment | Goal |
|---:|---|---|
| 0-8 min | Foundations | thread state, race, JMM |
| 8-20 min | Locks and atomics | volatile, synchronized, Lock, CAS |
| 20-35 min | Executors | pool sizing, queues, rejection, CompletableFuture |
| 35-45 min | Concurrent collections | CHM internals and traps |
| 45-55 min | Virtual threads | modern migration judgment |
| 55-60 min | Production scenario | blocked request or booking race |

### Questions

1. Explain concurrency vs parallelism.
2. What is a race condition? Code a simple counter bug.
3. Explain visibility, atomicity, and ordering.
4. What does the Java Memory Model guarantee?
5. What does `volatile` guarantee, and why does it not fix `count++`?
6. What does `synchronized` provide beyond mutual exclusion?
7. Compare `synchronized`, `ReentrantLock`, and `AtomicInteger`.
8. What is CAS?
9. Explain CountDownLatch, Semaphore, CyclicBarrier, and BlockingQueue.
10. How would you size a ThreadPoolExecutor?
11. Why is an unbounded executor queue dangerous?
12. What happens when the queue is full?
13. Explain CompletableFuture common-pool risks.
14. How does ConcurrentHashMap avoid full-map locking?
15. Why are `get` then `put` unsafe as a compound update?
16. What is the mutable-value trap inside ConcurrentHashMap?
17. What problem do virtual threads solve?
18. Why do virtual threads not remove DB bottlenecks?
19. What is pinning?
20. A booking service double-books rooms under load. Walk me from Java-level cause to database-level fix.

### Follow-Up Pressure

- Show a happens-before relationship.
- What would a thread dump show for deadlock?
- What metric tells you executor starvation is happening?
- What is the risk of ThreadLocal in pools?
- What would you change first in a production incident?
- Is your solution safe across multiple JVMs?

### Strong Closing Pattern

```text
Concurrency interviews are about correctness first: visibility, atomicity, ordering, and
invariants. I pick the smallest primitive that protects the invariant, measure contention,
and avoid mistaking in-memory thread safety for distributed consistency.
```

---

## Mock 4. 60-Minute JVM, GC, And Production Debugging Round

Best after reading:
- `03-Senior-FAANG/Java-JVM-GC-Performance-Debugging-FAANG-Master-Sheet.md`
- `03-Senior-FAANG/Java-Production-Engineering-Best-Practices-FAANG-Master-Sheet.md`
- `05-Special-Interview-Rounds/Java-Production-Debugging-Case-Studies-Gold-Sheet.md`

### Interview Flow

| Time | Segment | Goal |
|---:|---|---|
| 0-10 min | JVM pipeline | class loading, interpreter, JIT |
| 10-22 min | Memory and GC | heap, GC roots, collectors |
| 22-35 min | Debugging tools | dumps, JFR, jcmd, jstack, jmap |
| 35-50 min | Incident scenarios | high CPU, memory leak, GC spikes, deadlock |
| 50-57 min | Production judgment | evidence, mitigation, postmortem |
| 57-60 min | Summary | Senior debugging answer |

### Questions

1. Explain how JVM executes bytecode.
2. What does the JIT compiler optimize?
3. Explain class loading and parent delegation.
4. What is stored in heap, stack, and metaspace?
5. What are GC roots?
6. How do objects move from young to old generation?
7. Compare G1, ZGC, and Shenandoah at a high level.
8. Why can Java have memory leaks despite garbage collection?
9. How do you debug high CPU?
10. How do you debug memory leak?
11. How do you debug deadlock?
12. How do you debug GC pause spikes?
13. What is the difference between thread dump, heap dump, and JFR?
14. What commands or tools would you use first?
15. A Java service has high p99 latency. Walk me through your investigation.
16. How do you avoid changing too much during an incident?
17. What would you put in the post-incident summary?

### Follow-Up Pressure

- What if CPU is low but latency is high?
- What if many threads are WAITING?
- What if heap dump shows many cached objects?
- What if GC logs show frequent full GC?
- What if JFR points to lock contention?
- What if the issue only appears after deployment?

### Strong Closing Pattern

```text
I debug Java production issues by collecting evidence first: metrics, logs, thread dumps,
heap data, GC logs, and JFR. Then I separate CPU, memory, GC, lock contention, executor
starvation, and downstream latency before changing code or configuration.
```

---

## Mock 5. 45-Minute Modern Java, Tooling, Testing, And Security Round

Best after reading:
- `03-Senior-FAANG/Java-Modern-LTS-17-21-25-FAANG-Master-Sheet.md`
- `03-Senior-FAANG/Java-Platform-Tooling-Testing-Security-FAANG-Master-Sheet.md`
- `03-Senior-FAANG/Java-Testing-Patterns-Best-Practices-Gold-Sheet.md`

### Interview Flow

| Time | Segment | Goal |
|---:|---|---|
| 0-10 min | Modern Java | records, sealed classes, pattern matching, virtual threads |
| 10-18 min | Build and runtime | Maven/Gradle, classpath, modules |
| 18-30 min | Testing strategy | unit, integration, contract, Testcontainers |
| 30-38 min | Benchmarking and security | JMH, dependency hygiene, secrets |
| 38-45 min | Migration judgment | Java version adoption plan |

### Questions

1. Which Java 17 features are useful in backend code?
2. When should you use records?
3. When should you avoid records?
4. What problem do sealed classes solve?
5. Explain pattern matching benefits.
6. What Java 21 features matter most?
7. How do you discuss Java 25 safely?
8. Explain preview vs stable features.
9. Explain classpath vs module path.
10. How do you debug dependency conflicts?
11. What Maven lifecycle phases matter most?
12. What does a good backend testing strategy look like?
13. When should you use Mockito?
14. When is a fake better than a mock?
15. Why use Testcontainers?
16. What is a flaky test and how do you debug it?
17. Why is JMH needed for microbenchmarks?
18. What security checks belong in CI?
19. How would you plan a Java 11 to Java 21 migration?

### Follow-Up Pressure

- How do framework versions affect JDK migration?
- What breaks with reflection under native images?
- What should not be benchmarked with JMH?
- What test would you move from unit to integration?
- What secret-handling mistake do you watch for in Java apps?

### Strong Closing Pattern

```text
Modern Java adoption should be practical: use stable LTS features that reduce boilerplate and
improve correctness, verify framework and build support, update tests and CI, and avoid betting
production on preview features without a deliberate rollout plan.
```

---

## Mock 6. 90-Minute Java LLD And Machine-Coding Round

Best after reading:
- `05-Special-Interview-Rounds/Java-LLD-Machine-Coding-Patterns-Gold-Sheet.md`
- `04-Scenario-Practice/Java-Intervue-Round-2-Concurrency-Streams-Booking-Scenario-Gold-Sheet.md`
- `06-Practice-Upgrade/Java-Runnable-Mini-Labs.md` Lab 15

### Problem

Design and implement an in-memory hotel room booking system.

### Requirements

- Add a room with room id, room type, and capacity.
- Search available rooms for a date range and capacity.
- Book a room for a guest.
- Reject overlapping bookings for the same room.
- Cancel a booking.
- List bookings by guest.
- Keep domain objects valid.
- Keep service logic separate from storage.
- Include demo cases.

### Time Plan

| Time | Segment | Output |
|---:|---|---|
| 0-8 min | Clarify requirements | Assumptions and operations |
| 8-18 min | Model | Room, Booking, Guest or guest id |
| 18-28 min | Repository interfaces | RoomRepository, BookingRepository |
| 28-55 min | Core implementation | add, search, book, cancel, list |
| 55-68 min | Edge cases | overlap, invalid dates, missing room, duplicate cancel |
| 68-78 min | Thread safety | one-JVM protection strategy |
| 78-86 min | Demo tests | main method scenarios |
| 86-90 min | Explain trade-offs | production migration |

### Clarifying Questions Candidate Should Ask

1. Are dates inclusive or checkout-exclusive?
2. Can one guest have multiple bookings?
3. Is room capacity exact or minimum capacity?
4. Should cancellation remove or mark cancelled?
5. Does the system run in one JVM or multiple instances?
6. Is persistence required?
7. Are prices required?

### Evaluation Follow-Ups

- Where did you enforce date validity?
- How did you detect overlap?
- What is the time complexity of search?
- Is your repository thread-safe?
- Is the service method atomic?
- What would fail in multiple service instances?
- How would a database unique constraint help?
- What tests would you write first?
- Where would Strategy fit if pricing is added?
- Where would Factory be unnecessary?

### Strong Closing Pattern

```text
My in-memory design separates domain validity, storage, and use-case logic. It is safe only
within one JVM if I protect the booking invariant with synchronization or per-room locking.
For production, I would move the invariant to database transactions and constraints, then keep
Java locks only as an optimization, not the source of truth.
```

---

## Mock 7. 30-Minute Tricky Output Round

Best after reading:
- `05-Special-Interview-Rounds/Java-Tricky-Output-Questions-Gold-Sheet.md`
- `05-Special-Interview-Rounds/Java-Generics-Reflection-Annotations-Deep-Dive-Gold-Sheet.md`

### Rules

- Predict output before explaining.
- If it does not compile, say so first.
- Explain the Java rule, not just the result.
- Keep each answer under two minutes.

### Prompt Categories

1. String literal vs `new String()`.
2. Compile-time vs runtime concatenation.
3. Static initialization order.
4. Constructor and superclass order.
5. Overloading vs overriding.
6. Static method hiding.
7. Field hiding vs method overriding.
8. Boxing and unboxing.
9. Integer cache.
10. Numeric promotion.
11. `finally` with return.
12. Try-with-resources close order.
13. Stream laziness.
14. Parallel stream ordering.
15. Generic type erasure.
16. Wildcard compile-time errors.
17. `equals` without `hashCode`.
18. Mutable HashMap key.
19. `volatile int count++`.
20. Lambda `this` vs anonymous class `this`.

### Scoring

- 2 points: correct output or compile-time failure.
- 2 points: correct rule.
- 1 point: trap explained clearly.

Target: 80+ out of 100.

### Strong Closing Pattern

```text
For output questions, I slow down and classify the rule first: reference vs object,
compile-time vs runtime, overload vs override, initialization order, boxing, exception flow,
streams laziness, generics erasure, or concurrency visibility.
```

---

## Mock 8. 75-Minute Senior Backend Java Scenario Round

Best after reading all scenario and senior sheets.

### Scenario

A booking API handles high traffic. During a sale, users report duplicate bookings, high p99 latency, and occasional timeouts. The service uses Java 17, Spring Boot, a fixed thread pool, a database connection pool, and an in-memory ConcurrentHashMap cache for room status.

### Interview Flow

| Time | Segment | Goal |
|---:|---|---|
| 0-10 min | Clarify symptoms | duplicate booking, latency, timeout scope |
| 10-20 min | Java concurrency | executor, CHM, locks, mutable values |
| 20-32 min | Database correctness | transactions, unique constraints, isolation |
| 32-45 min | JVM diagnostics | thread dump, heap, GC, JFR |
| 45-58 min | API resilience | timeout, retry, idempotency, backpressure |
| 58-68 min | Testing | race tests, integration tests, load tests |
| 68-75 min | Final architecture answer | fixes and trade-offs |

### Questions

1. What information do you ask for first?
2. How can duplicate bookings happen despite ConcurrentHashMap?
3. What business invariant must be protected?
4. Where should that invariant live in production?
5. What role does a database transaction play?
6. What unique constraint would you consider?
7. How can retry behavior create duplicate writes?
8. How would idempotency keys help?
9. How can a fixed thread pool cause p99 latency?
10. How can a DB connection pool bottleneck show up?
11. What would you look for in a thread dump?
12. When would you collect JFR?
13. What metrics matter during the incident?
14. What immediate mitigation would you apply?
15. What code-level fix would you propose?
16. What database-level fix would you propose?
17. What tests prove the race is fixed?
18. Would virtual threads fix this? Why or why not?
19. How would you roll out the fix safely?
20. Give your final recommendation in two minutes.

### Strong Closing Pattern

```text
I would treat duplicate booking as a correctness issue first, not a cache issue. Java locks or
ConcurrentHashMap can reduce in-JVM races, but the production invariant belongs in database
transactions and constraints with idempotent API behavior. For latency, I would inspect executor,
DB pool, downstream calls, GC, and lock contention using metrics, dumps, and JFR before changing
architecture.
```

---

## Mock 9. 90-Minute Java Capstone Production Round

Best after:
- `00-Setup/Java-JDK-CLI-IDE-Maven-Gradle-Gold-Sheet.md`
- `02-Intermediate-Backend/Java-JDBC-Transactions-Connection-Pooling-Gold-Sheet.md`
- `03-Senior-FAANG/Java-Data-Formats-Jackson-Protobuf-Serialization-Gold-Sheet.md`
- `03-Senior-FAANG/Java-Profiling-JFR-AsyncProfiler-Runbooks-Gold-Sheet.md`
- `05-Special-Interview-Rounds/Java-Annotation-Processing-Code-Generation-Gold-Sheet.md`
- `06-Practice-Upgrade/Java-Capstone-Production-Service-Lab.md`

### Scenario

You built a plain Java in-memory booking service. The interviewer now asks you to make it production-ready enough for a real backend team.

### Interview Flow

| Time | Segment | Goal |
|---:|---|---|
| 0-8 min | Environment and build | JDK, wrapper, CI/runtime alignment |
| 8-22 min | Domain and invariant | booking model, date range, overlap |
| 22-38 min | Concurrency | one-JVM locking and limits |
| 38-52 min | JDBC and persistence | transactions, constraints, pool sizing |
| 52-64 min | Data contracts | DTOs, JSON/event schema, compatibility |
| 64-74 min | Code generation | annotations, MapStruct/Lombok trade-offs |
| 74-84 min | Profiling and incidents | p99, JFR, thread dump, pool pressure |
| 84-90 min | Final answer | concise production migration plan |

### Questions

1. What JDK and build setup would you require for the project?
2. How do you prove local and CI use the same Java version?
3. What is the core booking invariant?
4. How do you test date overlap boundaries?
5. How do you make in-memory booking safe in one JVM?
6. Why does that not solve multi-instance correctness?
7. What database transaction or constraint would you add?
8. How do prepared statements prevent SQL injection?
9. What connection pool metrics matter during load?
10. Why should API DTOs differ from persistence entities?
11. How would you evolve a booking-created event safely?
12. When would JSON be enough, and when would Protobuf/Avro be better?
13. Where would MapStruct help, and where might Lombok hurt clarity?
14. Why can generated classes be missing in CI?
15. How would you debug p99 latency in this service?
16. When would you collect JFR?
17. What would a thread dump show during DB pool starvation?
18. What tests belong in unit, integration, and load categories?
19. What observability would you add before launch?
20. Give the final production migration plan in two minutes.

### Strong Closing Pattern

```text
I would keep the plain Java domain and service boundary, then make production correctness
database-backed with transactions, constraints, and idempotency. I would expose DTOs instead
of entities, evolve event schemas additively, and keep generated code such as mappers in the
build path. For runtime confidence, I would align JDK/build/CI/runtime versions, add tests,
metrics, logs, and use JFR/thread dumps/pool metrics for p99 incidents before tuning.
```

---

## Mock Schedule Recommendation

| Week | Mocks |
|---|---|
| Week 1 | Mock 1, Mock 2, Mock 3 |
| Week 2 | Mock 4, Mock 5, Mock 7 |
| Week 3 | Mock 6, Mock 8 |
| Week 4 | Mock 9 plus repeat weakest two mocks under stricter timing |

---

## Post-Mock Review Template

```text
Date:
Mock:
Score:
Strong areas:
Weak areas:
Questions missed:
Concept gaps:
Code gaps:
Production-judgment gaps:
Next 3 actions:
Retest date:
```
