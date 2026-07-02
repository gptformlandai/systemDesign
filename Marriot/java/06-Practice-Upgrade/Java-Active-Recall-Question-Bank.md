# Java Active Recall Question Bank

Goal: convert the Java track from reading material into retrieval practice.

Use this after reading each sheet.

How to practice:
- Answer without looking at the source note.
- Keep most answers under 90 seconds.
- Write code for any prompt that mentions code.
- Mark each question: green, yellow, or red.
- Revisit red questions after 24 hours and again after 7 days.

Scoring:
- Green: clear, correct, with one example or trade-off.
- Yellow: mostly correct, but missing internals, trap, or production judgment.
- Red: vague, memorized, or unable to answer without notes.

---

## 1. Java Core Hot Interview Master Sheet

Source: `01-Starter-Path/Java-Core-Hot-Interview-Master-Sheet.md`

1. Explain the difference between JDK, JRE, and JVM in one minute.
2. Walk through Java execution from `.java` source to optimized machine code.
3. Where do local primitives, local references, objects, class metadata, and string literals live?
4. Explain heap, stack, metaspace, PC register, and native method stack.
5. What happens when a class is loaded, linked, and initialized?
6. Why is `static` resolved differently from overridden instance methods?
7. Explain `final`, `finally`, and `finalize` without mixing them.
8. What makes a class immutable, and where can immutability still leak?
9. Explain the `equals` and `hashCode` contract using a HashMap failure example.
10. How does HashMap handle hashing, buckets, collision chains, treeification, and resizing?
11. Explain checked vs unchecked exceptions and when each is appropriate.
12. What is type erasure, and why does Java use it?
13. Explain visibility vs atomicity using `volatile int count++`.
14. What does garbage collection reclaim, and what objects are GC roots?
15. Give a strong 60-second answer for "How does Java memory work?"

---

## 2. Java String Deep Dive

Source: `01-Starter-Path/Java-String-Deep-Dive.md`

1. Why is `String` immutable in Java?
2. Explain the string pool using literals and `new String()`.
3. Why can `==` sometimes return true for strings and still be the wrong comparison?
4. What does `intern()` do, and when would you avoid using it casually?
5. Explain compile-time vs runtime string concatenation.
6. How does `final String part = "ja"` affect concatenation folding?
7. Why is `StringBuilder` usually better for loops?
8. When is `StringBuffer` relevant, and why is it less common now?
9. Explain substring memory behavior in modern Java.
10. What are common String interview traps around null, equality, and immutability?
11. Why are strings useful as HashMap keys?
12. Give a 45-second answer for "Why is String immutable?"

---

## 3. Java 8 Plus Concepts Interview Prep

Source: `01-Starter-Path/Java-8-Plus-Concepts-Interview-Prep.md`

1. Explain lambda expressions and what problem they solve.
2. What is a functional interface, and why does `@FunctionalInterface` help?
3. Difference between anonymous class `this` and lambda `this`?
4. Explain `Predicate`, `Function`, `Consumer`, and `Supplier` with examples.
5. When should `Optional` be used, and when should it not be used?
6. Explain `orElse` vs `orElseGet` with a performance trap.
7. What does default method support enable in interfaces?
8. Explain method references and when they improve readability.
9. Why was the Date-Time API introduced, and what did it fix?
10. How does `CompletableFuture` improve over raw `Future`?
11. What are common traps in `CompletableFuture` exception handling?
12. Which Java 8+ features are most useful for backend services?
13. Write a small example using `Optional`, `map`, and `orElseGet`.
14. Explain how functional style can become unreadable.
15. Give a senior answer for "What changed in Java 8?"

---

## 4. Java Streams Interview Prep

Source: `01-Starter-Path/Java-Streams-Interview-Prep.md`

1. Explain stream source, intermediate operations, terminal operations, and laziness.
2. Why does a stream pipeline do nothing without a terminal operation?
3. Difference between `map` and `flatMap`?
4. Difference between `filter`, `peek`, and `forEach`?
5. Explain `reduce` and when it becomes hard to read.
6. Write a stream pipeline to group employees by department.
7. Write a pipeline to find top N expensive orders.
8. Explain primitive streams and when to use `mapToInt` or `mapToDouble`.
9. What makes parallel streams dangerous in backend code?
10. Why should stream operations avoid external mutable state?
11. Explain short-circuit operations like `findFirst`, `anyMatch`, and `limit`.
12. How do ordered and unordered streams affect parallel behavior?
13. When is a plain loop better than a stream?
14. Debug a stream pipeline that returns unexpected duplicates.
15. Give a 60-second answer for "How do Java streams work?"

---

## 5. Java Streams Collectors End-to-End Examples

Source: `01-Starter-Path/Java-Streams-Collectors-End-to-End-Examples-Gold-Sheet.md`

1. Explain `collect` vs `Collectors`.
2. Write examples for `toList`, `toSet`, and `joining`.
3. Use `groupingBy` for a one-level grouping problem.
4. Use nested `groupingBy` for department and status.
5. Use `partitioningBy` and explain how it differs from `groupingBy`.
6. Explain the duplicate-key trap in `toMap`.
7. Write `toMap` with a merge function.
8. Use `mapping`, `filtering`, or `collectingAndThen` in a downstream collector.
9. Explain when `summarizingInt` is better than multiple passes.
10. Build a collector pipeline for revenue by category.
11. Identify when a collector solution is overcomplicated.
12. Explain collector mutability and returned collection expectations.
13. Convert a multi-step loop into a collector pipeline.
14. Convert an unreadable collector pipeline back into clearer code.
15. Give a strong answer for "How do collectors work?"

---

## 6. Java Design Patterns Interview Prep

Source: `01-Starter-Path/Java-Design-Patterns-Interview-Prep.md`

1. Explain why design patterns exist.
2. When is Singleton useful, and why is it often overused?
3. Compare Factory Method and Abstract Factory.
4. Explain Strategy using pricing or payment behavior.
5. Explain Builder and when it is better than telescoping constructors.
6. Explain Adapter using an external API integration.
7. Explain Decorator and how it differs from inheritance.
8. Explain Observer and where event-driven systems use the idea.
9. Explain Template Method and its downside.
10. Map common design patterns to Spring concepts.
11. Which patterns help machine-coding rounds the most?
12. How do you avoid pattern-driven overengineering?
13. Refactor a large `if/else` using Strategy.
14. Explain how patterns support testability.
15. Give a senior answer for "Which design pattern did you use recently?"

---

## 7. Java Collections Internals And Concurrent Collections

Source: `02-Intermediate-Backend/Java-Collections-Internals-Concurrent-Collections-FAANG-Master-Sheet.md`

1. Explain List, Set, Queue, Deque, and Map selection in backend code.
2. How does HashMap compute bucket index?
3. What happens during HashMap resize?
4. Explain collision chains and treeification.
5. Why must HashMap keys be effectively immutable?
6. Difference between HashMap, LinkedHashMap, TreeMap, and EnumMap?
7. Difference between HashSet, LinkedHashSet, and TreeSet?
8. Why is PriorityQueue iteration not sorted?
9. Explain fail-fast iterator behavior.
10. Compare fail-fast and weakly consistent iterators.
11. How does ConcurrentHashMap improve over Hashtable?
12. Why does ConcurrentHashMap reject null keys and values?
13. Explain CopyOnWriteArrayList use cases and cost.
14. When is a synchronized wrapper not enough?
15. Give a strong answer for "How does ConcurrentHashMap work?"

---

## 8. Java Concurrency Deep Dive

Source: `02-Intermediate-Backend/Java-Concurrency-Deep-Dive-FAANG-Master-Sheet.md`

1. Explain concurrency vs parallelism.
2. What is a race condition? Show a counter example.
3. Difference between visibility, atomicity, and ordering?
4. Explain happens-before rules that matter in interviews.
5. What does `volatile` guarantee, and what does it not guarantee?
6. Explain `synchronized` monitor enter and exit semantics.
7. Compare `synchronized`, `ReentrantLock`, and atomics.
8. What is CAS, and what is the ABA problem?
9. Explain AQS at a high level.
10. How do you size a ThreadPoolExecutor queue and pool?
11. Why is an unbounded queue dangerous?
12. Explain producer-consumer with BlockingQueue.
13. Compare CountDownLatch, CyclicBarrier, Semaphore, and Phaser.
14. How do you detect and prevent deadlocks?
15. Give a production answer for "How would you debug blocked threads?"

---

## 9. Java IO, NIO, And Serialization

Source: `02-Intermediate-Backend/Java-IO-NIO-Serialization-FAANG-Master-Sheet.md`

1. Compare byte streams and character streams.
2. Why does buffering improve IO performance?
3. Explain try-with-resources and resource cleanup.
4. Compare blocking IO and NIO.
5. Explain Buffer, Channel, and Selector.
6. When is NIO worth the complexity?
7. Explain file reading choices for small vs large files.
8. What is Java serialization, and why is it risky?
9. Explain `serialVersionUID`.
10. What happens to `transient` and `static` fields during serialization?
11. Explain `readObject`, `writeObject`, and `readResolve` use cases.
12. Why should deserialization from untrusted data be avoided?
13. Compare Java serialization with JSON or protobuf.
14. Debug an API that loads a huge file into memory.
15. Give a strong answer for "Why is serialization dangerous?"

---

## 10. Java Virtual Threads And Modern Concurrency

Source: `03-Senior-FAANG/Java-Virtual-Threads-Modern-Concurrency-FAANG-Master-Sheet.md`

1. Explain platform threads vs virtual threads.
2. What problem do virtual threads solve?
3. Why are virtual threads not a CPU accelerator?
4. What is a carrier thread?
5. Why do we usually avoid pooling virtual threads?
6. Explain pinning and why it matters.
7. What role do DB connection pools still play?
8. When can ThreadLocal become risky with virtual threads?
9. Show basic code using `newVirtualThreadPerTaskExecutor`.
10. Explain structured concurrency in plain language.
11. Explain scoped values and context propagation.
12. How would you migrate a blocking service to virtual threads safely?
13. What metrics would you watch during rollout?
14. When would reactive programming still be useful?
15. Give a senior answer for "Should we switch to virtual threads?"

---

## 11. Java JVM, GC, Performance, And Debugging

Source: `03-Senior-FAANG/Java-JVM-GC-Performance-Debugging-FAANG-Master-Sheet.md`

1. Explain the JVM execution pipeline from bytecode to JIT.
2. What are the main runtime memory areas?
3. Explain class loading and parent delegation.
4. What JIT optimizations should a senior Java engineer know?
5. What are GC roots?
6. Explain young generation, old generation, allocation, and promotion.
7. Compare G1, ZGC, and Shenandoah at an interview level.
8. How do memory leaks happen in garbage-collected languages?
9. How do you debug high CPU in Java?
10. How do you debug a memory leak?
11. How do you read a thread dump?
12. What is JFR, and when would you use it?
13. What do `jcmd`, `jstack`, `jmap`, and `jstat` help with?
14. How do you distinguish CPU, lock, GC, and downstream latency?
15. Give a production answer for "Our Java service is slow. What do you do?"

---

## 12. Java Modern LTS 17, 21, 25

Source: `03-Senior-FAANG/Java-Modern-LTS-17-21-25-FAANG-Master-Sheet.md`

1. Which Java 17 features are most useful in backend code?
2. Explain records and where they should not be used.
3. Explain sealed classes and when they improve modeling.
4. Explain pattern matching and why it reduces boilerplate.
5. What changed in Java 21 that matters for backend services?
6. Explain virtual threads from a modern LTS perspective.
7. What are sequenced collections?
8. How do you discuss Java 25 safely when features may change?
9. Difference between final, preview, incubator, and early-access features?
10. What production checks are needed before adopting a newer JDK?
11. How do frameworks affect Java version upgrades?
12. What build and CI changes should be checked during migration?
13. How do you answer "Why are you still on Java 8 or 11?"
14. Create a migration argument from Java 11 to Java 21.
15. Give a strong answer for "What modern Java features do you use?"

---

## 13. Java Production Engineering Best Practices

Source: `03-Senior-FAANG/Java-Production-Engineering-Best-Practices-FAANG-Master-Sheet.md`

1. What makes Java code production-ready beyond passing tests?
2. Explain API boundary validation.
3. How should errors be modeled in service code?
4. What should and should not be logged?
5. Explain timeout, retry, backoff, and circuit breaker trade-offs.
6. Why is idempotency important in backend systems?
7. How do you handle time using `Clock` and `Instant`?
8. Why are mutable shared objects risky?
9. How do you design for observability?
10. What metrics would you expose for a Java service?
11. Explain safe configuration handling.
12. What makes a dependency risky in production?
13. How do you prevent thread-pool and connection-pool starvation?
14. Describe a code review checklist for production Java.
15. Give a senior answer for "What does clean Java mean in production?"

---

## 14. Java Platform, Tooling, Testing, And Security

Source: `03-Senior-FAANG/Java-Platform-Tooling-Testing-Security-FAANG-Master-Sheet.md`

1. Explain classpath vs module path.
2. What problem does JPMS solve, and why is adoption mixed?
3. Compare JAR, WAR, and executable JAR.
4. Explain Maven lifecycle phases that matter.
5. Explain Gradle task basics and dependency management.
6. How do you debug dependency conflicts?
7. What does JUnit 5 provide over older styles?
8. When should Mockito be avoided?
9. What problem does Testcontainers solve?
10. Why is JMH better than naive timing?
11. What is GraalVM Native Image good for?
12. What are Native Image trade-offs?
13. Explain hashing vs encryption vs encoding.
14. What Java security hygiene belongs in CI?
15. Give a strong answer for "How do you ship Java safely?"

---

## 15. Java Testing Patterns And Best Practices

Source: `03-Senior-FAANG/Java-Testing-Patterns-Best-Practices-Gold-Sheet.md`

1. Explain the test pyramid for backend Java.
2. What makes a good unit test?
3. When is an integration test more valuable than a unit test?
4. Compare mocks, stubs, fakes, and spies.
5. Why can excessive mocking make tests brittle?
6. Explain test data builders.
7. What is a contract test?
8. When should Testcontainers be used?
9. How do you diagnose flaky tests?
10. What should not be tested with JMH?
11. How do you test time-dependent code?
12. How do you test concurrency-sensitive code?
13. What belongs in CI vs nightly test jobs?
14. How do you review a test suite's health?
15. Give a senior answer for "What is your testing strategy?"

---

## 16. Java Collectors And Terminal Operators

Source: `04-Scenario-Practice/Java-Collectors-Terminal-Operators-Gold-Sheet.md`

1. Explain terminal operation vs intermediate operation.
2. What does `collect` do?
3. What is a Collector made of conceptually?
4. Explain `Collectors.toMap` duplicate key failure.
5. Use `groupingBy` with downstream `counting`.
6. Use `groupingBy` with downstream `mapping`.
7. When is `forEach` a poor terminal operation?
8. Explain `findFirst` vs `findAny`.
9. Explain `reduce` vs `collect`.
10. Which collectors return mutable collections?
11. Debug a collector that throws on nulls or duplicates.
12. Explain `collectingAndThen` with an immutable result.
13. When should collector code be split into named methods?
14. Build a query-like transformation with streams.
15. Give a final answer for "collect vs Collectors."

---

## 17. Java ConcurrentHashMap Request Scenario

Source: `04-Scenario-Practice/Java-ConcurrentHashMap-Request-Scenario-Gold-Sheet.md`

1. Explain ConcurrentHashMap using a real request scenario.
2. Why is Hashtable less scalable?
3. What is bucket-level locking?
4. Where does CAS help?
5. Why is `containsKey` then `put` unsafe?
6. When should `putIfAbsent` be used?
7. When should `compute` or `merge` be used?
8. Why can thread-safe map still have unsafe mutable values?
9. Explain why ConcurrentHashMap is not a distributed lock.
10. Where do database transactions enter the booking scenario?
11. How would you handle idempotency for duplicate requests?
12. What metrics would show contention or misuse?
13. Design an in-memory cache with ConcurrentHashMap and TTL.
14. Explain the limits of per-JVM coordination.
15. Give a strong answer for a booking race-condition question.

---

## 18. Java Intervue Round 2 Concurrency Streams Booking Scenario

Source: `04-Scenario-Practice/Java-Intervue-Round-2-Concurrency-Streams-Booking-Scenario-Gold-Sheet.md`

1. Summarize the booking scenario in 60 seconds.
2. Identify the Java concepts being tested.
3. Solve a stream filtering and grouping requirement for bookings.
4. Explain how race conditions appear in booking flows.
5. Distinguish Java synchronization from database consistency.
6. Explain how to prevent double booking in one JVM.
7. Explain how to prevent double booking across service instances.
8. What role do transactions and unique constraints play?
9. Where can ConcurrentHashMap help, and where can it mislead?
10. How would you test the booking race condition?
11. How would you observe production booking conflicts?
12. What concurrency primitive would you avoid and why?
13. How would you explain JVM memory in this round?
14. What is the strongest closing summary for the interviewer?
15. Run the full mock without looking at notes.

---

## 19. Java Scenario-Based Quick Revision

Source: `04-Scenario-Practice/Java-Scenario-Based-Quick-Revision-Gold-Sheet.md`

1. Use the scenario answer template without notes.
2. Explain `final List` mutability.
3. Explain a mutable object used as a HashMap key.
4. Explain why `String ==` sometimes appears to work.
5. Explain constructor behavior during deserialization.
6. Explain ArrayList vs LinkedList for inserts.
7. Explain PriorityQueue iteration order.
8. Explain why parallel stream is slower in a scenario.
9. Explain `volatile int count++`.
10. Explain executor queue growth.
11. Explain CompletableFuture common pool blocking.
12. Explain ThreadLocal leaks.
13. Explain high CPU debugging.
14. Explain full GC spikes.
15. Pick five random scenarios and answer each in 90 seconds.

---

## 20. Java Tricky Output Questions

Source: `05-Special-Interview-Rounds/Java-Tricky-Output-Questions-Gold-Sheet.md`

1. State the output-question checklist from memory.
2. Predict literal vs `new String()` output.
3. Predict compile-time string concatenation output.
4. Predict runtime string concatenation output.
5. Explain static initialization order.
6. Explain constructor chaining output.
7. Predict overloading vs overriding output.
8. Explain boxing, unboxing, and wrapper cache output.
9. Predict `finally` with return behavior.
10. Predict stream laziness output.
11. Predict generics erasure related behavior.
12. Identify compile-time errors before output.
13. Explain why the answer follows Java rules, not intuition.
14. Create three new output questions from weak areas.
15. Solve ten output questions under a 20-minute timer.

---

## 21. Java Generics, Reflection, And Annotations Deep Dive

Source: `05-Special-Interview-Rounds/Java-Generics-Reflection-Annotations-Deep-Dive-Gold-Sheet.md`

1. Explain type erasure with one code example.
2. Why is `List<String>` not reified at runtime?
3. Explain bounded type parameters.
4. Explain PECS with producer and consumer examples.
5. Difference between `List<?>`, `List<? extends Number>`, and `List<? super Integer>`?
6. What can reflection inspect?
7. What are reflection performance and safety costs?
8. Explain annotation retention policies.
9. Explain target policies for annotations.
10. How do annotations get behavior in frameworks?
11. Explain dynamic proxy basics.
12. How does proxy-based AOP affect method calls?
13. Why can self-invocation bypass Spring proxies?
14. Build a simple custom annotation explanation.
15. Give a strong answer for "How does Spring use reflection and annotations?"

---

## 22. Java LLD And Machine Coding Patterns

Source: `05-Special-Interview-Rounds/Java-LLD-Machine-Coding-Patterns-Gold-Sheet.md`

1. State the machine-coding answer flow from memory.
2. Given a problem, identify entities first.
3. Design a repository interface for in-memory storage.
4. Design a service method with validation and exceptions.
5. Decide whether models should be immutable records or classes.
6. Use Strategy for replaceable behavior.
7. Use Factory only when creation complexity justifies it.
8. Add thread safety to an in-memory repository.
9. Explain when ConcurrentHashMap alone is insufficient.
10. Add test cases for happy path and edge cases.
11. Explain how you would demo the solution.
12. Refactor a large method into model, repository, and service layers.
13. Define extension points without overengineering.
14. Handle invalid input cleanly.
15. Build one complete machine-coding solution under 90 minutes.

---

## 23. Java Production Debugging Case Studies

Source: `05-Special-Interview-Rounds/Java-Production-Debugging-Case-Studies-Gold-Sheet.md`

1. Debug high CPU step by step.
2. Debug memory leak step by step.
3. Debug deadlock step by step.
4. Debug GC pause spikes step by step.
5. Debug thread-pool starvation step by step.
6. Debug connection-pool exhaustion step by step.
7. Debug classpath or dependency conflicts step by step.
8. What data do you collect before changing code?
9. How do you avoid making production worse during debugging?
10. What does a thread dump reveal?
11. What does a heap dump reveal?
12. What does JFR reveal?
13. How do you form and test a hypothesis?
14. How do you write a post-incident learning summary?
15. Give a senior answer for "Tell me about a production issue you solved."

---

## 24. Java JDK, CLI, IDE, Maven, And Gradle

Source: `00-Setup/Java-JDK-CLI-IDE-Maven-Gradle-Gold-Sheet.md`

1. Explain JDK, JRE, JVM, `javac`, and `java` without mixing them.
2. What should `JAVA_HOME` point to?
3. How do you prove which JDK the terminal is using?
4. How do you compile and run a single Java file?
5. Why can IntelliJ pass while CI fails?
6. What problem do Maven and Gradle wrappers solve?
7. What is the standard Java project layout?
8. How do toolchains prevent Java version drift?
9. What causes `UnsupportedClassVersionError`?
10. How would you align IDE, build tool, CI, and container runtime?
11. Why should local commands match CI commands?
12. When is a single-file Java exercise enough?
13. When do you need a real build tool?
14. What should a Java project README include for setup?
15. Give a strong answer for "It works locally but fails in CI."

---

## 25. Java JDBC, Transactions, And Connection Pooling

Source: `02-Intermediate-Backend/Java-JDBC-Transactions-Connection-Pooling-Gold-Sheet.md`

1. Explain JDBC in one minute.
2. What roles do `DataSource`, `Connection`, `PreparedStatement`, and `ResultSet` play?
3. Why are prepared statements safer than string concatenation?
4. How does try-with-resources prevent connection leaks?
5. What does auto-commit do?
6. Walk through a manual transaction with commit and rollback.
7. Why must connection state be restored before returning to a pool?
8. What causes connection-pool exhaustion?
9. Why can increasing pool size make the database slower?
10. What metrics would you check for HikariCP?
11. How do transaction isolation and database locks relate to Java service correctness?
12. What Maven scope should a JDBC driver usually use?
13. How do JDBC batching and transaction boundaries interact?
14. Why does `ConcurrentHashMap` not solve multi-instance booking correctness?
15. Give a senior answer for "Our API waits for DB connections."

---

## 26. Java Profiling, JFR, Async-Profiler, And JVM Runbooks

Source: `03-Senior-FAANG/Java-Profiling-JFR-AsyncProfiler-Runbooks-Gold-Sheet.md`

1. Map high CPU to the right Java diagnostic steps.
2. How do you map an OS thread to a Java thread dump `nid`?
3. Why are multiple thread dumps better than one?
4. What does JFR capture?
5. When would you use async-profiler?
6. What is a flamegraph?
7. How do you debug memory retention?
8. What is the difference between allocation pressure and retained memory?
9. When is a heap dump risky?
10. How do GC logs and JFR complement each other?
11. How do you debug native memory growth?
12. What evidence points to lock contention?
13. What evidence points to downstream pool starvation?
14. What mitigation can you apply before root-cause fix?
15. Give a production answer for "Do not guess, collect evidence."

---

## 27. Java Data Formats, Jackson, Protobuf, And Serialization

Source: `03-Senior-FAANG/Java-Data-Formats-Jackson-Protobuf-Serialization-Gold-Sheet.md`

1. Explain why data formats are contracts.
2. Why should APIs use DTOs instead of JPA entities?
3. When is JSON a good choice?
4. When is Protobuf a good choice?
5. When is Avro and schema registry a good choice?
6. Why is Java native serialization risky?
7. How do you represent money safely?
8. How do you represent timestamps safely?
9. Why are unknown JSON fields useful for compatibility?
10. What makes polymorphic deserialization dangerous?
11. How do you evolve an event schema safely?
12. What Protobuf field-number rule should you remember?
13. What is a poison-pill message?
14. How would contract tests catch a breaking payload change?
15. Give a strong answer for JSON vs Protobuf vs Avro.

---

## 28. Java Annotation Processing And Code Generation

Source: `05-Special-Interview-Rounds/Java-Annotation-Processing-Code-Generation-Gold-Sheet.md`

1. Explain annotations, reflection, and annotation processing.
2. Compare `SOURCE`, `CLASS`, and `RUNTIME` retention.
3. Why can runtime reflection not see source-retention annotations?
4. What does an annotation processor do during compilation?
5. Why can generated code be faster at runtime than reflection?
6. What does Lombok generate, and what are the risks?
7. What does MapStruct generate, and why is it useful?
8. Where do generated sources usually appear?
9. Why can code build in IDE but fail in CI with generated classes missing?
10. How do Maven/Gradle annotation processor paths matter?
11. How do annotation processors relate to native-image/AOT?
12. What generated-code choices can harm readability?
13. How would you inspect generated code during debugging?
14. How do you explain Spring annotations vs compile-time generation?
15. Give a strong answer for "Reflection discovers, processors generate."

---

## 29. Java Capstone Production Service Lab

Source: `06-Practice-Upgrade/Java-Capstone-Production-Service-Lab.md`

1. Explain the capstone architecture in 90 seconds.
2. What is the core booking invariant?
3. How do you model a date range safely?
4. Why is `[start, end)` usually easier than closed ranges?
5. Where should validation live?
6. What belongs in the service layer vs repository layer?
7. Why is check-then-insert unsafe without a critical section?
8. How would you make the in-memory version one-JVM safe?
9. Why is one-JVM safety not enough in production?
10. What database transaction or constraint would enforce correctness?
11. How would you add idempotency?
12. Which DTOs would you expose externally?
13. What tests prove overlap and concurrency correctness?
14. How would you debug p99 latency in the capstone service?
15. Give a senior answer for evolving the capstone into production.

---

## Weekly Recall Rotation

Use this when revising the full Java track.

| Day | Focus | Rule |
|---|---|---|
| Monday | Core, String, Java 8 | Answer 30 questions, code 3 snippets |
| Tuesday | Streams, Collectors, Patterns | Answer 30 questions, solve 3 transformations |
| Wednesday | Collections, Concurrency, IO, JDBC | Answer 40 questions, code 2 concurrency/database snippets |
| Thursday | JVM, GC, Virtual Threads, Profiling | Answer 35 questions, explain 2 incidents |
| Friday | Production, Tooling, Testing, Data Formats | Answer 35 questions, design 1 test strategy and 1 payload contract |
| Saturday | Scenarios, Annotation Processing, Special Rounds | Run timed mocks and output questions |
| Sunday | Red-question cleanup | Re-answer only red/yellow questions |

---

## Mastery Gate

Before calling a topic done, verify:

- I can define it simply.
- I can explain why it exists.
- I can describe internals or mechanics.
- I can write or read code using it.
- I know the main trap.
- I can connect it to backend production behavior.
- I can answer follow-up questions without memorized wording.
