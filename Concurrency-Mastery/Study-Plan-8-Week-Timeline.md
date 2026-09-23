# Concurrency Mastery — 8-Week Interview-Ready Study Plan

> **Goal:** From "read the modules" to "walk into an L5 loop and land it" in **8 weeks** at **~15 hours/week** (2 hrs/weekday + 3 hrs Saturday + 2 hrs Sunday).
>
> Two alternate pacings included at the bottom for a 6-week rush or a 12-week sustainable pace.

---

## The Core Idea

Interview readiness is **not** "I read all 12 modules." It's:

1. **Recall** — you can explain any subtopic aloud in 60 seconds.
2. **Draw** — you can whiteboard the mind map from memory.
3. **Code** — you can write Module 4's Tier-A problems cold in 30 minutes.
4. **Diagnose** — you can name the failure mode for a described symptom.
5. **Defend** — you can pick a design (Redlock vs ZK, MVC+VT vs WebFlux) and defend it under pushback.

The schedule below rotates through **learn → drill → simulate** every week, so all five muscles grow together.

---

## Daily Ritual (do this every day)

| Slot | Activity | Time |
|---|---|---|
| 🌅 Morning (or start of session) | Redraw yesterday's mind map from memory on paper | 10 min |
| 📖 Study | New module content or drill (per calendar below) | 60–90 min |
| 💻 Code | One machine-coding problem or code exercise | 30–45 min |
| 🎤 Speak | Answer 3 self-check questions **out loud**, timed to 60s each | 10 min |
| 📝 Log | Note one "aha," one confusion, one thing to revisit | 5 min |

**Total: ~2 hours/day on weekdays; 3–4 hours on weekend days.**

---

## The 8-Week Calendar

### Week 1 — Foundations & Locks

| Day | Focus | Deliverable at end of day |
|---|---|---|
| **Mon** | Module 1.1 Process vs Thread + 1.2 IPC | Explain fork+COW and pick process-vs-thread for Nginx/Postgres/Redis aloud |
| **Tue** | Module 1.3 Cache + 1.4 JMM | Enumerate the 7 happens-before rules from memory |
| **Wed** | Module 2.1 `synchronized` + Object Monitor | Sketch `_EntryList`/`_WaitSet`; explain `notify` vs `notifyAll` failure modes |
| **Thu** | Module 2.2 AQS | Draw the AQS acquire flow; explain state-int semantics for 4 primitives |
| **Fri** | Module 2.3 `ReentrantLock`, RRWLock, Condition | Code the two-condition bounded buffer from memory |
| **Sat** | **Consolidation** — redraw Module 1 & 2 mind maps; answer all 20 self-check qs aloud | Timed self-check |
| **Sun** | **Machine coding warm-up** — Code Print-In-Order and FizzBuzz Multithreaded from Module 4 | Both compile + pass smoke tests |

**Milestone:** You can explain what happens hardware-to-JMM when Thread A does `synchronized (obj)` and Thread B is waiting.

---

### Week 2 — Atomics + Machine Coding Starts

| Day | Focus | Deliverable |
|---|---|---|
| **Mon** | Module 3.1 CAS + 3.2 ABA | Explain ABA with the broken-stack example |
| **Tue** | Module 3.3 Atomic family + 3.4 `LongAdder` | Justify picking `AtomicLong` vs `LongAdder` given a workload |
| **Wed** | Module 3.5 `VarHandle` + field updaters | Explain `getAcquire` vs `getVolatile` cost |
| **Thu** | **Machine coding drill — Problem 1: Bounded Blocking Queue** | Code both versions (RL+2Conds, sync+wait/notifyAll) from scratch, no notes |
| **Fri** | **Machine coding drill — Problem 2: Producer-Consumer + Problem 3: Thread Pool** | Both run; explain shutdown vs shutdownNow |
| **Sat** | **Machine coding drill — Problem 4: Concurrent LRU** | Single-lock version cold; sketch striped version |
| **Sun** | Consolidation + **first mock-code session** — someone (or an LLM) gives you one of: BBQ, LRU, Thread Pool. 45-min timer. | Solution + verbal walkthrough |

**Milestone:** All 4 Tier-A problems code cold in ≤ 45 min each.

---

### Week 3 — Machine Coding Deep Dive + LeetCode Concurrency

| Day | Focus | Deliverable |
|---|---|---|
| **Mon** | **Problem 5: Token Bucket Rate Limiter** — lock-free version, then walk through follow-ups (leaky-bucket, distributed) | Code from memory; explain SCALE-fixed-point |
| **Tue** | **LC: Print in Order + Fizz Buzz MT** — code both, then explain each primitive choice | Both pass smoke tests |
| **Wed** | **LC: H2O + Dining Philosophers** — semaphore+barrier composition; lock ordering | Both pass; state the 3 dining-philosopher fixes |
| **Thu** | **LC: Traffic Light** + custom-CountDownLatch (Module 2 example) + custom-Semaphore (Tier B) | All three pass |
| **Fri** | Re-do Problem 1 (BBQ) and Problem 3 (Thread Pool) cold, timed | Both pass in ≤ 30 min |
| **Sat** | **Second mock-code session** — 60 min, 2 problems back-to-back (BBQ + LRU, or Thread Pool + Rate Limiter) | Both complete |
| **Sun** | **War-story rehearsal** — pick 5 stories across Modules 1–3 and Module 4; tell each aloud in 60 s | Recorded (voice memo) |

**Milestone:** You can pick up any Tier-A problem and code it in 30 min with clean invariants said aloud.

---

### Week 4 — Concurrent Collections + Thread Pools + Async

| Day | Focus | Deliverable |
|---|---|---|
| **Mon** | Module 5.1 CHM internals (all 4 put cases, treeify, resize) | Trace through concurrent puts on the same bin |
| **Tue** | Module 5.2 Queues (ABQ vs LBQ vs SynchronousQ vs DelayQ vs PBQ vs CLQ) | Pick the queue for 5 workload descriptions |
| **Wed** | Module 5.3–5.5 (Deques, CoW list, SkipListMap) | Explain read:write break-even for CoW |
| **Thu** | Module 6.1 `ThreadPoolExecutor` — state machine, sizing formulas, rejection policies, hooks, submit vs execute | Draw the core→queue→max→reject diagram; size a pool for a given workload |
| **Fri** | Module 6.2 factories + 6.3 ForkJoinPool + 6.4 `CompletableFuture` | Code the `allOf + join` fan-out pattern with explicit executor |
| **Sat** | **Machine-coding review week** — re-do BBQ, Thread Pool, LRU, Rate Limiter back-to-back in one 3-hour block | All 4 pass |
| **Sun** | **Third mock session** — machine coding + 1 verbal design ("size a pool for a 500ms-p99 payment service") | Both delivered |

**Milestone:** You choose the right collection and the right pool config for any workload described in the interview, with sizing justified aloud.

---

### Week 5 — Loom + Diagnostics

| Day | Focus | Deliverable |
|---|---|---|
| **Mon** | Module 7.1–7.2 VT architecture + pinning | Explain unmount, list JDK 21 vs 24 pinning changes |
| **Tue** | Module 7.3 Downstream exhaustion + 7.4 `ScopedValue` / `StructuredTaskScope` | Code VT-per-request with `Semaphore` gating and a `StructuredTaskScope` example |
| **Wed** | Module 7.5 VT vs Reactive decision + migration playbook | Defend the 2026 default aloud |
| **Thu** | Module 9.1 Failure modes (deadlock, livelock, starvation, thread-pool starvation DL, `ThreadLocal` leak) | State Coffman conditions + sketch the pool-starvation DL |
| **Fri** | Module 9.2 Thread dumps — capture, read BLOCKED vs WAITING, find wait-graph | Take 3 dumps of the running Module 4 demo and read them |
| **Sat** | Module 9.3 Profilers + JFR + GC/safepoint | Run `async-profiler` in `cpu`, `wall`, `lock` modes on the Module 4 demos |
| **Sun** | Module 9.4 Playbooks A–E from memory | Say each playbook aloud, timed to 90s |

**Milestone:** Given a described incident ("service hangs, 0% CPU"), you pick the right playbook and know the exact commands.

---

### Week 6 — Distributed Concurrency (Core)

| Day | Focus | Deliverable |
|---|---|---|
| **Mon** | Module 8.1 Distributed locks (Redis / ZK / etcd) | Compare backends; explain fencing token in 60 s |
| **Tue** | Module 8.2 DB concurrency (optimistic, pessimistic, `SKIP LOCKED`) | Write the retry loop; write the SKIP LOCKED job-queue query |
| **Wed** | Module 8.2 (cont.) — Isolation levels, write skew, MVCC | Draw xmin/xmax; give the doctor-on-call write skew story |
| **Thu** | Module 8.3–8.4 Scheduling + Idempotency & Fencing | Explain the L5 "effectively-once" mantra |
| **Fri** | Module 8B.1 Raft (three diagrams) + 8B.2 CAP/PACELC | Draw election, log replication; state PACELC quadrant for 5 systems |
| **Sat** | Module 8B.3 2PC → Saga → TCC | Design a payment saga in orchestration form aloud |
| **Sun** | Module 8B.4 Kafka concurrency (EOS-v2, rebalance, group.instance.id) | Explain idempotent producer + transactional producer config |

**Milestone:** You can defend a distributed-lock choice AND design a saga AND explain Kafka EOS in a single system-design round.

---

### Week 7 — Advanced Distributed + Spring

| Day | Focus | Deliverable |
|---|---|---|
| **Mon** | Module 8B.5 Outbox + CDC (Debezium) + 8B.6 Distributed rate limiting | Sketch the outbox schema + Lua rate-limit script |
| **Tue** | Module 8B.7 Cache concurrency (single-flight, thundering herd) + 8B.8 Backpressure (CB, bulkhead, adaptive) | Code the single-flight cache + Resilience4j chain |
| **Wed** | Module 8B.9 Split brain + STONITH + 8B.10 Leader election patterns | Pick leader-election for 4 environments |
| **Thu** | Module 8C.1 Spanner + TrueTime, 8C.2 Lamport/Vector/HLC | Explain external consistency in one minute |
| **Fri** | Module 8C.3 Consistent + rendezvous hashing, 8C.4 SWIM/phi accrual, 8C.5 CRDTs | Pick 5 real systems using each |
| **Sat** | Module 10.1 `@Async` traps + 10.2 `ThreadPoolTaskExecutor` config | Write the full production `AsyncConfig` cold |
| **Sun** | Module 10.3 Context propagation + 10.4 WebFlux vs VT decision | Write `TaskDecorator` from memory; defend MVC+VT 2026 default |

**Milestone:** Whole curriculum consumed. You've seen every concept once.

---

### Week 8 — Consolidation, Mocks & Behavioral Wiring

| Day | Focus | Deliverable |
|---|---|---|
| **Mon** | **Full mind-map redraw day** — draw Modules 1, 2, 3 mind maps from memory on paper. Score yourself. | 3 correct mind maps |
| **Tue** | **Full mind-map redraw day** — Modules 4, 5, 6 | 3 correct mind maps |
| **Wed** | **Full mind-map redraw day** — Modules 7, 8+8B+8C consolidated, 9, 10 | 4 correct mind maps |
| **Thu** | **Mock machine-coding round** — 45 min, unseen problem (ask a friend or use LC concurrency section) | Pass a fresh problem |
| **Fri** | **Mock system-design round** — "Design a distributed rate limiter for 10M rps" or "Design an at-least-once payment processor" | Delivered aloud, 45 min |
| **Sat** | **Mock diagnostics round** — someone reads a fake incident; you pick the playbook | 3 incidents, correct playbooks |
| **Sun** | **War-story consolidation** — pick your top 12 war stories (1 per module); rehearse each in 60 s | Voice-recorded set |

**Milestone:** You've simulated every round type. You know your weak areas. You have a war-story deck.

---

## Machine-Coding Rotation (never stop)

From Week 2 onward, **one machine-coding problem per weekday** in rotation:

| Weekday | Problem set |
|---|---|
| Mon | Bounded Blocking Queue (both versions) |
| Tue | Thread Pool from scratch |
| Wed | Concurrent LRU |
| Thu | Token Bucket Rate Limiter |
| Fri | Rotate through LeetCode 5 (Print in Order → Traffic Light) |

Goal: by Week 8, each Tier-A problem takes ≤ 25 minutes to code cleanly.

---

## Weekly Review Ritual (every Sunday evening)

- [ ] Redraw next week's target mind maps on paper.
- [ ] Skim the "Interview Traps" table for the week's modules — say the L5 answer aloud.
- [ ] Update the **Mistakes Log** — write down 3 things you got wrong this week.
- [ ] Pick 3 war stories to re-tell aloud (voice memo).
- [ ] Cross out completed items in this document.

---

## "You Are Ready" Checklist

Interview-ready means **all** of these are true:

**Recall (say aloud in 60s):**
- [ ] Happens-before rules (Module 1)
- [ ] `ObjectMonitor` internals (Module 2)
- [ ] CAS + ABA + fix (Module 3)
- [ ] `TPE` routing state machine (Module 6)
- [ ] Downstream exhaustion trap (Module 7)
- [ ] Coffman's 4 deadlock conditions (Module 9)
- [ ] Fencing tokens why & how (Module 8)
- [ ] Raft's 3 sub-problems (Module 8B)

**Draw (from memory):**
- [ ] All 12 mind maps
- [ ] Full `TPE` core→queue→max→reject diagram
- [ ] AQS acquire flow
- [ ] Raft election + log replication
- [ ] Outbox pattern

**Code (cold, ≤ 30 min each):**
- [ ] Bounded Blocking Queue (RL + 2 Conditions version)
- [ ] Thread Pool from scratch
- [ ] Concurrent LRU
- [ ] Token Bucket Rate Limiter
- [ ] All 5 LeetCode concurrency problems

**Diagnose (given symptom → playbook):**
- [ ] Stuck service → Playbook A
- [ ] High CPU → Playbook B
- [ ] p99 spikes → Playbook C
- [ ] Memory leak → Playbook D
- [ ] Thread count exploding → Playbook E

**Defend (pick and hold under pushback):**
- [ ] `CallerRunsPolicy` for the rejection default
- [ ] MVC + VT over WebFlux for a CRUD service in 2026
- [ ] Saga over 2PC for microservices transactions
- [ ] Bin-lock CHM over Java 7 Segments
- [ ] `LongAdder` over `AtomicLong` under contention

**War stories (12 in your pocket):**
- [ ] One from each module — see Module war-story sections

---

## Alternate Pacings

### The 6-Week Rush (~20 hrs/week)

For a tight timeline. Compress by:
- **Week 1:** Modules 1 + 2 + 3 (foundations blitz)
- **Week 2:** Module 4 machine coding (all Tier A cold)
- **Week 3:** Modules 5 + 6 + 9
- **Week 4:** Modules 7 + 10
- **Week 5:** Modules 8 + 8B (+ 8C skim)
- **Week 6:** Full mock week (as Week 8 above)

Skip Module 8C deep-read; scan for names + one-liners. Skip Tier B machine-coding problems.

### The 12-Week Sustainable (~10 hrs/week)

For a longer runway. Expand by:
- **Weeks 1–2:** Module 1 + 2 with extra AQS deep-dive practice
- **Weeks 3–4:** Module 3 + 4 (whole month on machine coding — every Tier-A problem coded 3× and every Tier-B once)
- **Weeks 5–6:** Modules 5 + 6
- **Weeks 7–8:** Modules 7 + 9
- **Weeks 9–10:** Modules 8 + 8B
- **Week 11:** Modules 8C + 10
- **Week 12:** Consolidation + mocks

Add an extra weekly "systems reading" hour — Kleppmann's *Designing Data-Intensive Applications* chapters 5, 7, 8, 9 aligned with Modules 5/8/8B/8C.

---

## Tools You'll Actually Use

- **Paper + pen** for mind maps. No exceptions.
- **A timer** for the 60-second oral answers and 30-minute code drills.
- **A voice recorder** (phone memo app) for war stories.
- **A whiteboard or a big notebook** for system-design mocks.
- **`javac` + terminal** to run the Module 4 code.
- **`jcmd`, `async-profiler`, JMC** on your laptop for Module 9 tooling practice.
- **One markdown file** — `Mistakes.md` — to log every wrong answer, forever.

---

## Common Failure Modes of This Plan

| Failure | Fix |
|---|---|
| "I keep re-reading modules instead of drilling." | Set a hard rule: after Week 1, no module gets a second full read. Only mind-map + self-check. |
| "I code Module 4 problems easily *with notes*." | You're not interview-ready. Take notes away. Timer at 30 min. |
| "I know the answer but freeze speaking it aloud." | Voice recorder daily. Play back. Cringe. Improve. |
| "I skip Week 8." | Don't. Week 8 is where all the pieces integrate. Cut Week 7 topics instead if you must. |
| "Weekends slip." | Move the weekend blocks to weekday evenings; do fewer topics per weekday. Never skip mocks. |

---

## Final Reminder

Interviews don't test the modules. They test **whether you can conjure the right piece of any module on demand while under mild stress**. That's what the drills, timers, and speak-aloud rituals are for.

Follow the calendar. Score yourself honestly. Adjust one week at a time.

Good luck.
