# Concurrency Mastery — L5 / Staff MAANG Blueprint

A right-sized concurrency prep track: nothing overcooked, nothing missing.
Each module explains concepts as if teaching a junior dev, with real-world analogies, production war stories, common confusions, and L5-grade code examples.

---

## Module Map

| # | Module | Focus | Status |
|---|---|---|---|
| 1 | [OS & Memory Foundations](./Module-01-OS-Memory-Foundations.md) | Process/thread, IPC, cache, JMM | 🟢 |
| 2 | [Locks & AQS](./Module-02-Locks-and-AQS.md) | `synchronized`, `ReentrantLock`, AQS internals | 🟢 |
| 3 | [Lock-Free & Atomics](./Module-03-LockFree-and-Atomics.md) | CAS, ABA, `LongAdder`, `VarHandle` | 🟢 |
| 4 | [Machine Coding Pack](./Module-04-Machine-Coding.md) | BBQ, thread pool, LRU, rate limiter, LeetCode set | 🟢 |
| 5 | [Concurrent Collections](./Module-05-Concurrent-Collections.md) | CHM, CLQ, blocking queues, CoW, SkipListMap | 🟢 |
| 6 | [Thread Pools & Async](./Module-06-ThreadPools-and-Async.md) | TPE sizing, ForkJoin, `CompletableFuture` | 🟢 |
| 7 | [Virtual Threads / Loom](./Module-07-Virtual-Threads-Loom.md) | M:N, pinning, downstream exhaustion, `ScopedValue` | 🟢 |
| 8 | [Distributed Concurrency](./Module-08-Distributed-Concurrency.md) | Redlock, fencing tokens, MVCC, isolation | 🟢 |
| 8B | [Distributed Coordination, Consensus & Events](./Module-08B-Distributed-Coordination-Consensus-Events.md) | Raft, PACELC, Sagas, Kafka EOS, Outbox, single-flight, circuit breakers | 🟢 |
| 8C | [Advanced Distributed Appendix](./Module-08C-Advanced-Distributed-Appendix.md) | Spanner/TrueTime, HLC, consistent hashing, SWIM, CRDTs | 🟢 |
| 9 | [Production Diagnostics](./Module-09-Production-Diagnostics.md) | Thread dumps, JFR, async-profiler | 🟢 |
| 10 | [Spring Concurrency](./Module-10-Spring-Concurrency.md) | `@Async`, MDC propagation, WebFlux vs VT | 🟢 |

📅 **[8-Week Study Plan & Timeline →](./Study-Plan-8-Week-Timeline.md)** — daily calendar, machine-coding rotation, weekly milestones, "you-are-ready" checklist, plus 6-week rush and 12-week sustainable alternates.

---

## How To Use

1. **Read the module** end-to-end once.
2. **Redraw the mind map** on paper from memory.
3. **Code the L5 examples** without looking.
4. **Answer the "Interview Traps"** section aloud.
5. Only then move to the next module.

---

## Time Budget (Recommended)

```
Module 4 (Machine Coding)     ████████████████ 35%
Modules 2 + 3 (Locks/Atomics) ████████         20%
Module 6 (Pools/Async)        ██████           15%
Modules 8 + 8B (Distributed)  ██████           15%
Module 9 (Diagnostics)        ████             10%
Modules 1 + 5 + 7 + 10        ███              5%
```
