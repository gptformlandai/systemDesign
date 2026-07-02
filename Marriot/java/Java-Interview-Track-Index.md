# Java Interview Track Index

This folder is the Java language track for backend interviews.

Goal:
- Build Java from beginner fundamentals to FAANG-level production judgment.
- Keep each topic modular so revision is easy.
- Make the answer pattern repeatable: mental model, definition, internals, code, traps, strong answer, revision.

Use this index as the reading order.

---

## 0. Setup Path

Read this first if your Java environment, terminal commands, or build-tool basics are not automatic yet.

| Order | File | What It Builds |
|---:|---|---|
| 0 | `00-Setup/Java-JDK-CLI-IDE-Maven-Gradle-Gold-Sheet.md` | JDK setup, `JAVA_HOME`, CLI commands, IDE parity, Maven/Gradle wrappers, project structure, version discipline |

Setup target:
- You can prove which JDK your terminal, IDE, build tool, CI, and runtime are using.
- You can compile and run a single Java file without the IDE.
- You can explain why wrappers and toolchains prevent "works on my machine" failures.

---

## 1. Starter Path

Read these first if you want the base language to feel clear.

| Order | File | What It Builds |
|---:|---|---|
| 1 | `01-Starter-Path/Java-Core-Hot-Interview-Master-Sheet.md` | JVM basics, memory, OOP, collections, exceptions, generics, threads, GC |
| 2 | `01-Starter-Path/Java-String-Deep-Dive.md` | String pool, immutability, literals, concatenation, `intern()` |
| 3 | `01-Starter-Path/Java-8-Plus-Concepts-Interview-Prep.md` | Lambdas, functional interfaces, Optional, Date-Time, CompletableFuture, modern Java awareness |
| 4 | `01-Starter-Path/Java-Streams-Interview-Prep.md` | Stream chains, collectors, grouping, map/flatMap/reduce, interview coding |
| 5 | `01-Starter-Path/Java-Streams-Collectors-End-to-End-Examples-Gold-Sheet.md` | Complete stream and collector examples from question to answer |
| 6 | `01-Starter-Path/Java-Design-Patterns-Interview-Prep.md` | Patterns, when to use, Spring mapping, design judgment |

Starter target:
- You can explain Java execution.
- You can answer OOP, collections, strings, Java 8, and streams.
- You can write small interview snippets without freezing.

---

## 2. Intermediate Backend Path

After the starter path, read these.

| Order | File | What It Builds |
|---:|---|---|
| 7 | `02-Intermediate-Backend/Java-Collections-Internals-Concurrent-Collections-FAANG-Master-Sheet.md` | HashMap, ConcurrentHashMap, TreeMap, PriorityQueue, iterator behavior |
| 8 | `02-Intermediate-Backend/Java-Concurrency-Deep-Dive-FAANG-Master-Sheet.md` | JMM, locks, CAS, atomics, AQS, thread pools, synchronizers |
| 9 | `02-Intermediate-Backend/Java-IO-NIO-Serialization-FAANG-Master-Sheet.md` | IO streams, NIO buffers/channels/selectors, files, serialization safety |
| 10 | `02-Intermediate-Backend/Java-JDBC-Transactions-Connection-Pooling-Gold-Sheet.md` | Pure JDBC, prepared statements, transactions, connection pools, SQL injection prevention, database correctness |

Intermediate target:
- You can explain how core Java data structures behave internally.
- You can reason about thread safety and visibility.
- You can choose between normal IO, NIO, and async/network patterns.
- You can explain how Java talks to relational databases under Spring/JPA abstractions.

---

## 3. Senior / FAANG Path

These are the pro sheets.

| Order | File | What It Builds |
|---:|---|---|
| 11 | `03-Senior-FAANG/Java-Virtual-Threads-Modern-Concurrency-FAANG-Master-Sheet.md` | Virtual threads, pinning, structured concurrency, scoped values, migration judgment |
| 12 | `03-Senior-FAANG/Java-JVM-GC-Performance-Debugging-FAANG-Master-Sheet.md` | JIT, GC, memory leaks, thread dumps, heap dumps, JFR/JMC, production debugging |
| 13 | `03-Senior-FAANG/Java-Modern-LTS-17-21-25-FAANG-Master-Sheet.md` | Java 17, 21, 25 LTS features, preview safety, interview-ready modern Java |
| 14 | `03-Senior-FAANG/Java-Production-Engineering-Best-Practices-FAANG-Master-Sheet.md` | Production coding judgment, API design, validation, logging, timeouts, retries, testing |
| 15 | `03-Senior-FAANG/Java-Platform-Tooling-Testing-Security-FAANG-Master-Sheet.md` | JPMS, classpath, Maven/Gradle, JUnit, JMH, GraalVM, Java security |
| 16 | `03-Senior-FAANG/Java-Testing-Patterns-Best-Practices-Gold-Sheet.md` | Test pyramid, JUnit 5, Mockito, Testcontainers, flaky tests, JMH boundaries |
| 17 | `03-Senior-FAANG/Java-Security-OWASP-Supply-Chain-FAANG-Master-Sheet.md` | OWASP Top 10 Java mappings, XXE, unsafe deserialization (CWE-502), SpEL injection, JWT vulnerabilities, Spring Security method security, secrets management, SBOM/supply chain |
| 18 | `03-Senior-FAANG/Java-Profiling-JFR-AsyncProfiler-Runbooks-Gold-Sheet.md` | Command-first runbooks for high CPU, memory leaks, GC pauses, thread dumps, JFR, async-profiler, native memory |
| 19 | `03-Senior-FAANG/Java-Data-Formats-Jackson-Protobuf-Serialization-Gold-Sheet.md` | JSON/Jackson, DTOs, Protobuf, Avro, schema evolution, unsafe deserialization, data contracts |

Senior target:
- You can discuss Java not just as syntax, but as a runtime.
- You can debug latency, memory, CPU, GC, blocked threads, and concurrency issues.
- You can explain modern Java adoption with production caution.
- You can describe how to write Java that survives production failure modes.
- You can discuss how Java code is built, packaged, tested, secured, and shipped.
- You can choose safe data formats and collect runtime evidence before tuning.

---

## 4. Scenario Practice Path

Use these after the concept sheets. They train fast spoken answers for actual interview prompts.

| Order | File | What It Builds |
|---:|---|---|
| 20 | `04-Scenario-Practice/Java-Collectors-Terminal-Operators-Gold-Sheet.md` | Collector clarity, `collect` vs `Collectors`, grouping, `toMap` traps |
| 21 | `04-Scenario-Practice/Java-ConcurrentHashMap-Request-Scenario-Gold-Sheet.md` | Request-level ConcurrentHashMap explanation, atomic methods, mutable-value trap |
| 22 | `04-Scenario-Practice/Java-Intervue-Round-2-Concurrency-Streams-Booking-Scenario-Gold-Sheet.md` | Full mock Round 2 flow: streams, concurrency, booking, JVM, CHM |
| 23 | `04-Scenario-Practice/Java-Scenario-Based-Quick-Revision-Gold-Sheet.md` | Rapid scenario answers across core Java, streams, concurrency, JVM, modern Java |

Scenario target:
- You can answer quickly under pressure.
- You can connect Java mechanics to backend systems.
- You can explain where Java in-memory tools stop and production consistency begins.
- You can revise high-frequency scenarios in one short sitting.

---

## 5. Special Interview Rounds

Use these when preparing for targeted interview formats: tricky output rounds, deep Java internals, LLD/machine coding, and production-debugging discussions.

| Order | File | What It Builds |
|---:|---|---|
| 24 | `05-Special-Interview-Rounds/Java-Tricky-Output-Questions-Gold-Sheet.md` | Output prediction traps: String, static, overloading, finally, boxing, streams, generics |
| 25 | `05-Special-Interview-Rounds/Java-Generics-Reflection-Annotations-Deep-Dive-Gold-Sheet.md` | Type erasure, PECS, wildcards, reflection, annotations, proxies, Spring internals |
| 26 | `05-Special-Interview-Rounds/Java-Annotation-Processing-Code-Generation-Gold-Sheet.md` | Annotation processors, generated sources, Lombok, MapStruct, reflection vs compile time, native-image compatibility |
| 27 | `05-Special-Interview-Rounds/Java-LLD-Machine-Coding-Patterns-Gold-Sheet.md` | Machine-coding structure, complete examples, repositories, patterns, thread safety |
| 28 | `05-Special-Interview-Rounds/Java-Production-Debugging-Case-Studies-Gold-Sheet.md` | High CPU, memory leak, deadlock, GC spikes, pool starvation, classpath issues |

Special-round target:
- You can solve tricky output questions by rule, not guesswork.
- You can explain Java framework mechanics behind generics, annotations, reflection, annotation processors, generated code, and proxies.
- You can design clean Java code quickly in machine-coding rounds.
- You can discuss production incidents with evidence-driven debugging.

---

## 6. Practice Upgrade Path

Use these after or alongside the concept sheets. They convert the Java track from passive reading into active recall, runnable labs, timed mocks, and measurable readiness.

| Order | File | What It Builds |
|---:|---|---|
| 29 | `06-Practice-Upgrade/Java-Active-Recall-Question-Bank.md` | Topic-by-topic recall questions mapped to every Java sheet |
| 30 | `06-Practice-Upgrade/Java-Runnable-Mini-Labs.md` | Hands-on Java labs for strings, collections, concurrency, streams, JVM, debugging, serialization, and LLD |
| 31 | `06-Practice-Upgrade/Java-Mock-Interview-Scripts.md` | Timed mock rounds for core Java, streams, concurrency, JVM, modern Java, LLD, tricky output, and senior scenarios |
| 32 | `06-Practice-Upgrade/Java-Interview-Scoring-Rubrics.md` | 1-5 scoring rubrics for concepts, coding, scenarios, production debugging, and readiness gates |
| 33 | `06-Practice-Upgrade/Java-2-Week-4-Week-Mastery-Roadmaps.md` | Realistic 2-week and 4-week study plans with daily practice and score checkpoints |
| 34 | `06-Practice-Upgrade/Java-Capstone-Production-Service-Lab.md` | End-to-end booking capstone tying models, collections, concurrency, JDBC thinking, DTOs, tests, profiling, and production migration |

Practice target:
- You can answer from memory, not just recognize notes.
- You can run small Java programs that expose the traps.
- You can score your answers honestly and retest weak areas.
- You can handle timed interview pressure with follow-up questions.

---

## 7. Interview Answer Pattern

Use this structure for most Java answers:

1. Give a crisp definition.
2. Explain why it exists.
3. Explain how it works internally.
4. Give a small code example.
5. Mention the trap.
6. Mention production judgment.
7. Close with a trade-off.

Example:

```text
ConcurrentHashMap is a thread-safe hash table for concurrent reads and updates.
It avoids locking the whole map for most operations. Reads are usually non-blocking,
updates synchronize only the affected bin/tree area, and compound actions still need
atomic map methods like compute or merge. I would use it for shared in-memory lookup
state, but not as a replacement for a database or distributed cache.
```

---

## 8. What A Gold-Level Java Learner Should Master

### Language Fundamentals

- JDK, `JAVA_HOME`, CLI compile/run flow, IDE/build parity.
- OOP principles and where they fail.
- Immutability and defensive copying.
- `equals` / `hashCode` contract.
- Generics, type erasure, wildcards, PECS.
- Exceptions and resource handling.
- Annotations and reflection basics.

### Collections

- Big-O behavior.
- HashMap resizing, collision handling, treeification.
- List vs Set vs Map choices.
- Concurrent collections and iterator semantics.
- PriorityQueue, TreeMap, LinkedHashMap, EnumMap.

### Concurrency

- Thread lifecycle.
- Race conditions.
- Visibility vs atomicity.
- `synchronized`, `volatile`, locks, atomics.
- ExecutorService and queue sizing.
- AQS synchronizers.
- CompletableFuture composition.
- Virtual threads.

### JVM

- Heap, stack, metaspace.
- Class loading.
- JIT and hot code.
- Escape analysis.
- GC roots and collectors.
- Thread dumps, heap dumps, JFR, JMC.
- Memory leak patterns.
- Command-first production runbooks for CPU, GC, heap, native memory, and lock issues.

### Modern Java

- Java 8 functional style.
- Java 11 library improvements.
- Java 17 records, sealed classes, pattern matching, text blocks.
- Java 21 virtual threads, sequenced collections, pattern matching for switch.
- Java 25 LTS awareness and preview-feature safety.

### Platform And Tooling

- Classpath vs module path.
- JPMS and `module-info.java`.
- JAR, WAR, executable JAR.
- Maven and Gradle lifecycle basics.
- Dependency conflict diagnosis.
- Build wrappers, Java toolchains, and local/CI/runtime version alignment.
- Pure JDBC, prepared statements, transactions, batching, connection pools, and SQL injection prevention.
- JUnit, Mockito, Testcontainers.
- JMH benchmarking.
- GraalVM Native Image trade-offs.
- Java security basics and dependency hygiene.

### Data Contracts

- DTOs vs entities.
- Jackson JSON mapping and unknown-field handling.
- Money, timestamps, locale, and precision traps.
- Protobuf and Avro trade-offs.
- Schema evolution and contract testing.
- Unsafe Java serialization and polymorphic deserialization risks.

### Testing

- Test pyramid judgment.
- JUnit 5 assertions and parameterized tests.
- Mockito vs fakes and common mocking traps.
- Test data builders.
- Integration tests and Testcontainers.
- Contract testing awareness.
- Flaky test diagnosis.
- JMH vs naive timing.

### Special Interview Skills

- Tricky output reasoning.
- Overloading vs overriding traps.
- Boxing/unboxing and wrapper cache behavior.
- Generics erasure and PECS.
- Annotation retention and runtime reflection.
- Annotation processing, generated sources, Lombok, MapStruct, and build-tool configuration.
- Proxy-based framework behavior.
- Machine-coding structure and clean extensibility.
- Evidence-driven production debugging.
- Capstone-level explanation from Java model to database correctness.

---

## 9. One-Day Revision Plan

### Hours 1-2

- Setup/toolchain sheet if environment is weak.
- Java Core master sheet.
- String deep dive.
- Collections internals.

### Hours 3-4

- Streams.
- Java 8+.
- Design patterns.

### Hours 5-6

- Concurrency deep dive.
- Virtual threads.
- CompletableFuture.

### Hours 7-8

- JVM, GC, and debugging.
- Profiling/JFR/async-profiler runbooks.
- Modern Java LTS.
- Production engineering best practices.
- Platform, tooling, testing, and security.
- JDBC and data-format sheets if backend integration comes up.
- Testing patterns and best practices.
- Annotation processing/code generation if framework internals come up.
- Scenario practice sheets.
- Special interview rounds.
- Practice upgrade recall/mocks for weakest areas.
- Capstone lab outline.
- Practice strong answers aloud.

---

## 10. Final Confidence Checklist

You are ready when you can answer these without notes:

- How does Java code execute from `.java` to machine code?
- How do you set up and verify JDK, `JAVA_HOME`, IDE, build tool, CI, and runtime version alignment?
- Where do objects, references, class metadata, and string literals live?
- How does HashMap work internally?
- Why must `equals` and `hashCode` be consistent?
- What is the Java Memory Model?
- Difference between visibility and atomicity?
- `volatile` vs `synchronized` vs `Lock`?
- How does ConcurrentHashMap avoid full-map locking?
- When would you use CompletableFuture?
- When would you avoid parallel streams?
- What problem do virtual threads solve?
- What are virtual thread pinning and carrier threads?
- How do you debug high CPU in Java?
- How do you debug memory leak in Java?
- How do you read a thread dump?
- Which GC would you consider for low latency?
- What are records, sealed classes, and pattern matching?
- How do you talk about preview features safely?
- How do you debug a dependency conflict?
- How do you design a Java testing strategy?
- How do you choose unit vs integration vs contract tests?
- When should you use Mockito, and when is a fake better?
- Why do Testcontainers help backend integration tests?
- Why is JMH better than naive timing?
- When would you consider GraalVM Native Image?
- How does plain JDBC work under Spring/JPA abstractions?
- How do you use `PreparedStatement`, transactions, and connection pools safely?
- How do you choose JSON vs Protobuf vs Avro?
- How do you evolve a DTO/event schema without breaking consumers?
- How do annotation processors differ from runtime reflection?
- How do Lombok and MapStruct generated sources affect CI and IDE setup?
- How would you collect JFR or async-profiler evidence for high CPU?
- What is the difference between hashing and encryption?
- How do you explain `collect` vs `Collectors`?
- Why can ConcurrentHashMap still fail if the stored value is mutable?
- How do you answer a booking race-condition scenario from Java to DB correctness?
- Can you solve Java output traps without guessing?
- Can you explain type erasure and PECS clearly?
- How do annotations get behavior in frameworks?
- How do Java dynamic proxies relate to Spring AOP?
- How do you structure a Java machine-coding solution?
- How do you debug high CPU, memory leak, deadlock, and GC pause spikes?
- Can you answer active-recall questions without reading the source sheet?
- Can you complete runnable labs and explain the observed behavior?
- Can you score yourself with the rubrics and identify red/yellow topics?
- Can you pass timed mock rounds without memorized wording?
- Can you explain the capstone from in-memory Java correctness to database-backed production correctness?

---

## 11. Gold Standard Coverage Map

| Level | What This Track Covers | Status |
|---|---|---|
| Beginner | Syntax-adjacent fundamentals, OOP, memory basics, collections, strings | Gold |
| Intermediate | Java 8, streams, exceptions, generics, design patterns, IO, serialization, JDBC | Gold |
| Senior | concurrency, JMM, locks, executors, JVM, GC, diagnostics, production coding, data contracts | Gold |
| FAANG | runtime trade-offs, virtual threads, JFR/JMC, async-profiler runbooks, low-latency GC, platform tooling, security, modern LTS, scenario delivery, special rounds | Gold |
| Practice | active recall, runnable labs, timed mocks, rubrics, roadmaps, capstone lab | Gold |

What makes this one-stop:

- Every major Java interview area has a dedicated sheet.
- The index gives a learning order instead of random notes.
- Each advanced topic includes traps, trade-offs, and production judgment.
- The setup sheet makes terminal, IDE, build, CI, and runtime version alignment explicit.
- The JDBC sheet anchors database correctness below framework abstractions.
- The modern Java section separates stable LTS features from preview/incubator/EA features.
- The platform sheet adds build, benchmarking, security, and GraalVM awareness.
- The data-format sheet covers DTOs, Jackson, Protobuf, Avro, and schema evolution.
- The dedicated testing sheet adds JUnit, Mockito, Testcontainers, builders, flaky tests, and test strategy.
- The special-round sheets cover output traps, framework internals, annotation processing, machine coding, and production debugging.
- The practice upgrade path adds active recall, hands-on labs, mock interviews, scoring rubrics, realistic mastery plans, and a capstone.

---

## 12. Official Source Notes

Use these sources when refreshing modern Java details:

- OpenJDK JDK 17: `https://openjdk.org/projects/jdk/17/`
- OpenJDK JDK 21: `https://openjdk.org/projects/jdk/21/`
- OpenJDK JDK 25: `https://openjdk.org/projects/jdk/25/`
- JDK builds and GA/EA status: `https://jdk.java.net/`
- Java Language Specification: `https://docs.oracle.com/javase/specs/`
- Java API documentation: `https://docs.oracle.com/en/java/javase/`
- JDBC API: `https://docs.oracle.com/en/java/javase/`
- JUnit User Guide: `https://docs.junit.org/`
- Mockito documentation: `https://site.mockito.org/`
- Testcontainers documentation: `https://testcontainers.com/`
- OpenJDK JMH: `https://openjdk.org/projects/code-tools/jmh/`
- JDK Mission Control: `https://openjdk.org/projects/jmc/`
- GraalVM Native Image: `https://www.graalvm.org/latest/reference-manual/native-image/`

Interview safety line:

```text
I separate stable LTS features from preview, incubator, and early-access features.
Before recommending a modern Java feature for production, I check the project JDK,
framework support, build flags, and runtime distribution.
```
