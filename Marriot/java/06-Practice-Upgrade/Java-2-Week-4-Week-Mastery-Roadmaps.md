# Java 2-Week And 4-Week Mastery Roadmaps

Goal: make the Java track usable for real interview preparation, not just one-day revision.

Use these roadmaps with:
- `Java-Active-Recall-Question-Bank.md`
- `Java-Runnable-Mini-Labs.md`
- `Java-Mock-Interview-Scripts.md`
- `Java-Interview-Scoring-Rubrics.md`

Daily rule:
- Read actively.
- Answer from memory.
- Code something.
- Score honestly.
- Retest weak areas.

Color system:
- Green: can answer and code without notes.
- Yellow: understand after seeing notes, but recall is weak.
- Red: cannot explain or apply.

---

## 1. Which Roadmap Should You Use?

| Situation | Use |
|---|---|
| Interview in 10-14 days | 2-week roadmap |
| Interview in 3-5 weeks | 4-week roadmap |
| Interview tomorrow | One-day plan in `Java-Interview-Track-Index.md` |
| Already strong in Java | 2-week roadmap, skip green topics |
| Rusty or moving from basic to senior | 4-week roadmap |

---

## 2. Daily Study Block Template

Use this for each study day.

| Block | Time | Action |
|---|---:|---|
| Read | 45-75 min | Read assigned sheet sections |
| Recall | 30-45 min | Answer active-recall questions without notes |
| Code | 30-60 min | Run lab or write snippets |
| Speak | 15-30 min | Give 60-90 second answers aloud |
| Score | 10 min | Update rubric and red/yellow/green tracker |

Minimum daily output:
- 20 recall questions answered.
- 1 code snippet or lab attempted.
- 3 spoken interview answers.
- 1 red/yellow cleanup note.

---

## 3. Two-Week Java Interview Roadmap

Use this when time is short but you still want depth.

### Week 1. Core, Collections, Streams, Concurrency

| Day | Focus | Read | Practice | Output |
|---:|---|---|---|---|
| 1 | Java runtime and memory | Core master sheet sections on JVM, memory, class loading | Recall Q1; Lab 2 | Explain Java execution and memory in 90 seconds |
| 2 | OOP, String, equality | Core OOP/equality plus String deep dive | Recall Q1-Q2; Lab 1 | Explain String pool and HashMap key failure |
| 3 | Java 8 and Optional | Java 8+ concepts | Recall Q3; write Optional examples | Explain lambda, Optional, Date-Time, CompletableFuture |
| 4 | Streams | Streams interview prep | Recall Q4; Lab 7 | Solve 3 stream transformations |
| 5 | Collectors and patterns | Collectors examples plus design patterns | Recall Q5-Q6; Lab 8 | Explain `toMap`, `groupingBy`, Strategy, Builder |
| 6 | Collections internals | Collections internals sheet | Recall Q7; rerun Lab 2 | Explain HashMap and ConcurrentHashMap internals |
| 7 | Concurrency foundations | Concurrency deep dive through locks/atomics | Recall Q8; Lab 3 | Explain JMM, volatile, synchronized, atomics |

Week 1 checkpoint:
- Run Mock 1: Core Java screen.
- Run Mock 2: Streams and collections round.
- Score with rubric.
- Mark top 10 red/yellow topics.

### Week 2. Senior Java, Production, Scenarios, Special Rounds

| Day | Focus | Read | Practice | Output |
|---:|---|---|---|---|
| 8 | Executors and CHM | Concurrency remaining sections plus CHM scenario | Recall Q8/Q17; Labs 4-6 | Explain executor starvation and CHM compound traps |
| 9 | Virtual threads | Virtual threads sheet | Recall Q10; Lab 9 if JDK 21+ | Explain virtual threads, pinning, DB pool limits |
| 10 | JVM and GC debugging | JVM/GC sheet | Recall Q11; Labs 11-12 | Debug high CPU, leak, deadlock verbally |
| 11 | Modern Java and production engineering | Modern LTS plus production best practices | Recall Q12-Q13 | Explain Java 17/21 adoption and production readiness |
| 12 | Tooling, testing, security | Platform/tooling/testing/security plus testing patterns | Recall Q14-Q15; Lab 13 | Explain testing strategy, JMH, dependency hygiene |
| 13 | Special rounds | Tricky output, generics/reflection, LLD | Recall Q20-Q22; Lab 14 | Solve output prompts and outline machine coding |
| 14 | Full simulation day | Scenario sheets and production debugging cases | Mock 3, Mock 4, Mock 8 | Final score and red-topic retest plan |

Week 2 final gate:
- Mock 3 average score: 4.0+
- Mock 4 average score: 4.0+
- Mock 8 no critical correctness miss
- 80%+ on tricky output round
- Can complete Lab 15 outline in 30 minutes or full build in 90 minutes

---

## 4. Four-Week Java Mastery Roadmap

Use this for deeper retention and senior readiness.

### Week 1. Foundations That Must Become Automatic

Goal: no hesitation on runtime, memory, OOP, strings, equality, Java 8 basics.

| Day | Focus | Assignments |
|---:|---|---|
| 1 | Track setup | Read index; skim all file titles; prepare red/yellow/green tracker |
| 2 | JVM/JDK/JRE and execution | Core sheet JVM sections; answer recall Q1 first 5 questions |
| 3 | Memory model basics | Core memory sections; draw heap/stack/metaspace examples |
| 4 | OOP and immutability | Core OOP sections; write immutable class with defensive copy |
| 5 | equals/hashCode | Core equality sections; run Lab 2; explain bug aloud |
| 6 | String deep dive | String sheet; run Lab 1; solve 10 output-style string prompts |
| 7 | Java 8 basics | Java 8+ sheet lambdas/functional interfaces/Optional/Date-Time |

Week 1 checkpoint:
- Mock 1, but allow one pause per answer.
- Score core Java rubric.
- Retest all red topics on Day 8 before moving on.

### Week 2. Streams, Collections, And Backend Coding Fluency

Goal: become comfortable transforming data and choosing collections under interview pressure.

| Day | Focus | Assignments |
|---:|---|---|
| 8 | Red-topic cleanup | Re-answer Week 1 red/yellow questions |
| 9 | Streams basics | Streams prep; run Lab 7; solve map/filter/sort tasks |
| 10 | Advanced streams | flatMap, reduce, primitive streams, short-circuiting |
| 11 | Collectors | Collectors examples; run Lab 8; solve grouping/toMap tasks |
| 12 | Design patterns | Patterns sheet; refactor if/else into Strategy |
| 13 | Collections internals | HashMap, TreeMap, PriorityQueue, iterators |
| 14 | Concurrent collections | ConcurrentHashMap, CopyOnWriteArrayList, weak consistency |

Week 2 checkpoint:
- Mock 2 under normal timing.
- Build a mini order-reporting stream exercise from scratch.
- Score collections and streams rubric.

### Week 3. Concurrency, JVM, And Production Debugging

Goal: move from Java syntax to runtime and incident reasoning.

| Day | Focus | Assignments |
|---:|---|---|
| 15 | Race conditions and JMM | Concurrency sheet; run Lab 3 |
| 16 | Locks and atomics | volatile, synchronized, Lock, CAS, AQS basics |
| 17 | Executors and queues | ThreadPoolExecutor; run Lab 4 |
| 18 | Coordination and ThreadLocal | latch/semaphore/barrier/phaser; run Lab 10 |
| 19 | ConcurrentHashMap scenarios | CHM request scenario; run Labs 5-6 |
| 20 | Virtual threads | Virtual threads sheet; run Lab 9 if available |
| 21 | JVM, GC, and tools | JVM/GC sheet; run Labs 11-12 carefully |

Week 3 checkpoint:
- Mock 3 and Mock 4.
- Explain high CPU, memory leak, deadlock, and pool starvation without notes.
- Score concurrency and JVM rubrics.

### Week 4. Senior Polish, Scenarios, LLD, And Final Simulation

Goal: make answers crisp, practical, and interview-ready.

| Day | Focus | Assignments |
|---:|---|---|
| 22 | Modern Java | Java 17/21/25 sheet; answer migration questions |
| 23 | Production engineering | Production best practices; create production code review checklist |
| 24 | Tooling/testing/security | Platform/tooling/security and testing sheets; run Lab 13 |
| 25 | Tricky output and generics | Output sheet plus generics/reflection/annotations; run Lab 14 |
| 26 | Machine coding | LLD sheet; implement or outline Lab 15 |
| 27 | Booking scenario day | Intervue booking scenario plus quick revision sheet |
| 28 | Final mock day | Mock 6, Mock 7, Mock 8; final rubric scoring |

Week 4 final gate:
- Core Java: 4.0+
- Collections/streams: 4.0+
- Concurrency: 4.0+
- JVM/debugging: 4.0+
- Modern Java/tooling/testing: 3.8+
- LLD/machine coding: 3.8+
- Tricky output: 85%+
- Senior scenario: no critical invariant miss

---

## 5. One-Day Emergency Revision Upgrade

Use only when the interview is tomorrow.

| Time | Focus | Action |
|---|---|---|
| Hour 1 | Core and String | Read index checklist; answer 20 recall questions |
| Hour 2 | HashMap, collections, streams | Solve 3 stream tasks; explain HashMap |
| Hour 3 | Concurrency | Explain JMM, volatile, synchronized, executor, CHM |
| Hour 4 | JVM/GC | Explain high CPU, memory leak, deadlock, GC spike |
| Hour 5 | Modern Java/testing/tooling | Explain Java 17/21, testing strategy, JMH, dependency conflicts |
| Hour 6 | Scenarios | Run booking scenario and CHM scenario aloud |
| Hour 7 | Special rounds | Solve tricky output and generics prompts |
| Hour 8 | Mock and patch | Run weakest mock; review red topics only |

Emergency rule:
- Do not reread everything.
- Attack the checklist and red topics.
- Speak answers aloud.
- Sleep if possible.

---

## 6. Spaced Repetition Plan

After finishing either roadmap, maintain for two weeks.

| Day After Finish | Action |
|---:|---|
| 1 | Re-answer all red questions |
| 2 | Run one lab from weak area |
| 3 | Mock 1 or Mock 2 |
| 5 | Mock 3 or Mock 4 |
| 7 | Tricky output timed set |
| 10 | Machine-coding drill outline |
| 14 | Senior scenario mock |

Rule:
- Green topics need light review.
- Yellow topics need explanation practice.
- Red topics need code or diagrams.

---

## 7. Topic Pairing With Other Tracks

Java alone is not full backend readiness. Pair it with these after the Java roadmap.

| Java Topic | Pair With |
|---|---|
| JDBC, transactions, booking correctness | SQL track transactions and locking |
| REST/API production engineering | Spring Boot track |
| Async processing and retries | Kafka and Microservices tracks |
| Observability and debugging | System design observability notes |
| LLD and machine coding | DSA and design-pattern practice |
| Virtual threads and blocking IO | Spring Boot, WebFlux, and performance notes |

---

## 8. Final Readiness Checklist

Before the interview, confirm:

- I can answer the final confidence checklist in the index without notes.
- I completed at least 8 runnable labs.
- I completed at least 3 mock interviews.
- I scored at least 4.0 on concurrency or know exactly what is weak.
- I can explain one-JVM vs distributed correctness.
- I can debug high CPU, memory leak, deadlock, GC spike, and pool starvation.
- I can solve output questions by rule, not guessing.
- I can structure a machine-coding solution in 10 minutes.
- I can connect Java concepts to Spring, SQL, Kafka, and production systems.

---

## 9. Weekly Scorecard

| Week | Core | Collections/Streams | Concurrency | JVM/Debugging | Modern/Testing | Scenarios/LLD | Notes |
|---:|---:|---:|---:|---:|---:|---:|---|
| 1 |  |  |  |  |  |  |  |
| 2 |  |  |  |  |  |  |  |
| 3 |  |  |  |  |  |  |  |
| 4 |  |  |  |  |  |  |  |
