# Java Interview Track Index

This folder is the Java language track for backend interviews.

Goal:
- Build Java from beginner fundamentals to FAANG-level production judgment.
- Keep each topic modular so revision is easy.
- Make the answer pattern repeatable: mental model, definition, internals, code, traps, strong answer, revision.

Use this index as the reading order.

---

## 1. Starter Path

Read these first if you want the base language to feel clear.

| Order | File | What It Builds |
|---:|---|---|
| 1 | `Java-Core-Hot-Interview-Master-Sheet.md` | JVM basics, memory, OOP, collections, exceptions, generics, threads, GC |
| 2 | `Java-String-Deep-Dive.md` | String pool, immutability, literals, concatenation, `intern()` |
| 3 | `Java-8-Plus-Concepts-Interview-Prep.md` | Lambdas, functional interfaces, Optional, Date-Time, CompletableFuture, modern Java awareness |
| 4 | `Java-Streams-Interview-Prep.md` | Stream chains, collectors, grouping, map/flatMap/reduce, interview coding |
| 5 | `Java-Streams-Collectors-End-to-End-Examples-Gold-Sheet.md` | Complete stream and collector examples from question to answer |
| 6 | `Java-Design-Patterns-Interview-Prep.md` | Patterns, when to use, Spring mapping, design judgment |

Starter target:
- You can explain Java execution.
- You can answer OOP, collections, strings, Java 8, and streams.
- You can write small interview snippets without freezing.

---

## 2. Intermediate Backend Path

After the starter path, read these.

| Order | File | What It Builds |
|---:|---|---|
| 7 | `Java-Collections-Internals-Concurrent-Collections-FAANG-Master-Sheet.md` | HashMap, ConcurrentHashMap, TreeMap, PriorityQueue, iterator behavior |
| 8 | `Java-Concurrency-Deep-Dive-FAANG-Master-Sheet.md` | JMM, locks, CAS, atomics, AQS, thread pools, synchronizers |
| 9 | `Java-IO-NIO-Serialization-FAANG-Master-Sheet.md` | IO streams, NIO buffers/channels/selectors, files, serialization safety |

Intermediate target:
- You can explain how core Java data structures behave internally.
- You can reason about thread safety and visibility.
- You can choose between normal IO, NIO, and async/network patterns.

---

## 3. Senior / FAANG Path

These are the pro sheets.

| Order | File | What It Builds |
|---:|---|---|
| 10 | `Java-Virtual-Threads-Modern-Concurrency-FAANG-Master-Sheet.md` | Virtual threads, pinning, structured concurrency, scoped values, migration judgment |
| 11 | `Java-JVM-GC-Performance-Debugging-FAANG-Master-Sheet.md` | JIT, GC, memory leaks, thread dumps, heap dumps, JFR/JMC, production debugging |
| 12 | `Java-Modern-LTS-17-21-25-FAANG-Master-Sheet.md` | Java 17, 21, 25 LTS features, preview safety, interview-ready modern Java |
| 13 | `Java-Production-Engineering-Best-Practices-FAANG-Master-Sheet.md` | Production coding judgment, API design, validation, logging, timeouts, retries, testing |
| 14 | `Java-Platform-Tooling-Testing-Security-FAANG-Master-Sheet.md` | JPMS, classpath, Maven/Gradle, JUnit, JMH, GraalVM, Java security |
| 15 | `Java-Testing-Patterns-Best-Practices-Gold-Sheet.md` | Test pyramid, JUnit 5, Mockito, Testcontainers, flaky tests, JMH boundaries |

Senior target:
- You can discuss Java not just as syntax, but as a runtime.
- You can debug latency, memory, CPU, GC, blocked threads, and concurrency issues.
- You can explain modern Java adoption with production caution.
- You can describe how to write Java that survives production failure modes.
- You can discuss how Java code is built, packaged, tested, secured, and shipped.

---

## 4. Scenario Practice Path

Use these after the concept sheets. They train fast spoken answers for actual interview prompts.

| Order | File | What It Builds |
|---:|---|---|
| 16 | `Java-Collectors-Terminal-Operators-Gold-Sheet.md` | Collector clarity, `collect` vs `Collectors`, grouping, `toMap` traps |
| 17 | `Java-ConcurrentHashMap-Request-Scenario-Gold-Sheet.md` | Request-level ConcurrentHashMap explanation, atomic methods, mutable-value trap |
| 18 | `Java-Intervue-Round-2-Concurrency-Streams-Booking-Scenario-Gold-Sheet.md` | Full mock Round 2 flow: streams, concurrency, booking, JVM, CHM |
| 19 | `Java-Scenario-Based-Quick-Revision-Gold-Sheet.md` | Rapid scenario answers across core Java, streams, concurrency, JVM, modern Java |

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
| 20 | `Java-Tricky-Output-Questions-Gold-Sheet.md` | Output prediction traps: String, static, overloading, finally, boxing, streams, generics |
| 21 | `Java-Generics-Reflection-Annotations-Deep-Dive-Gold-Sheet.md` | Type erasure, PECS, wildcards, reflection, annotations, proxies, Spring internals |
| 22 | `Java-LLD-Machine-Coding-Patterns-Gold-Sheet.md` | Machine-coding structure, complete examples, repositories, patterns, thread safety |
| 23 | `Java-Production-Debugging-Case-Studies-Gold-Sheet.md` | High CPU, memory leak, deadlock, GC spikes, pool starvation, classpath issues |

Special-round target:
- You can solve tricky output questions by rule, not guesswork.
- You can explain Java framework mechanics behind generics, annotations, reflection, and proxies.
- You can design clean Java code quickly in machine-coding rounds.
- You can discuss production incidents with evidence-driven debugging.

---

## 6. Interview Answer Pattern

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

## 7. What A Gold-Level Java Learner Should Master

### Language Fundamentals

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
- JUnit, Mockito, Testcontainers.
- JMH benchmarking.
- GraalVM Native Image trade-offs.
- Java security basics and dependency hygiene.

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
- Proxy-based framework behavior.
- Machine-coding structure and clean extensibility.
- Evidence-driven production debugging.

---

## 8. One-Day Revision Plan

### Hours 1-2

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
- Modern Java LTS.
- Production engineering best practices.
- Platform, tooling, testing, and security.
- Testing patterns and best practices.
- Scenario practice sheets.
- Special interview rounds.
- Practice strong answers aloud.

---

## 9. Final Confidence Checklist

You are ready when you can answer these without notes:

- How does Java code execute from `.java` to machine code?
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

---

## 10. Gold Standard Coverage Map

| Level | What This Track Covers | Status |
|---|---|---|
| Beginner | Syntax-adjacent fundamentals, OOP, memory basics, collections, strings | Gold |
| Intermediate | Java 8, streams, exceptions, generics, design patterns, IO, serialization | Gold |
| Senior | concurrency, JMM, locks, executors, JVM, GC, diagnostics, production coding | Gold |
| FAANG | runtime trade-offs, virtual threads, JFR/JMC, low-latency GC, platform tooling, security, modern LTS, scenario delivery, special rounds | Gold |

What makes this one-stop:

- Every major Java interview area has a dedicated sheet.
- The index gives a learning order instead of random notes.
- Each advanced topic includes traps, trade-offs, and production judgment.
- The modern Java section separates stable LTS features from preview/incubator/EA features.
- The platform sheet adds build, benchmarking, security, and GraalVM awareness.
- The dedicated testing sheet adds JUnit, Mockito, Testcontainers, builders, flaky tests, and test strategy.
- The special-round sheets cover output traps, framework internals, machine coding, and production debugging.

---

## 11. Official Source Notes

Use these sources when refreshing modern Java details:

- OpenJDK JDK 17: `https://openjdk.org/projects/jdk/17/`
- OpenJDK JDK 21: `https://openjdk.org/projects/jdk/21/`
- OpenJDK JDK 25: `https://openjdk.org/projects/jdk/25/`
- JDK builds and GA/EA status: `https://jdk.java.net/`
- Java Language Specification: `https://docs.oracle.com/javase/specs/`
- Java API documentation: `https://docs.oracle.com/en/java/javase/`
- JUnit User Guide: `https://docs.junit.org/`
- Mockito documentation: `https://site.mockito.org/`
- Testcontainers documentation: `https://testcontainers.com/`
- OpenJDK JMH: `https://openjdk.org/projects/code-tools/jmh/`
- GraalVM Native Image: `https://www.graalvm.org/latest/reference-manual/native-image/`

Interview safety line:

```text
I separate stable LTS features from preview, incubator, and early-access features.
Before recommending a modern Java feature for production, I check the project JDK,
framework support, build flags, and runtime distribution.
```
