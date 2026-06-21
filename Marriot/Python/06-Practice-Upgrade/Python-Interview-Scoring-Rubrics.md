# Python Interview Scoring Rubrics — Gold Sheet

> **Track File #30 of 31 · Group 6: Practice Upgrade**
> For: Java developer | Level: MAANG self-assessment | Mode: score each dimension after every practice session

---

## 1. How to Use This Sheet

**Scoring scale:** 1 = Not ready → 5 = MAANG-ready  
**When to score:** After each mock round, after reviewing a Gold Sheet section, or after a practice coding problem.  
**Goal:** ≥ 3 on all dimensions before scheduling a real interview. ≥ 4 on core dimensions for senior-level roles.

**Rule:** Be honest. A 2 you know about is more valuable than a 5 you pretend to have. Score what you could deliver in a real interview, under pressure, cold — not after re-reading your notes.

---

## 2. Dimension 1 — Core Language Concepts

**Covers:** Mutability, scope/LEGB, closures, comprehensions, built-in types, exceptions, context managers.

| Score | Behavioral Anchor |
|---|---|
| **1** | Cannot explain mutability vs immutability; confuses `is` with `==`; cannot describe LEGB |
| **2** | Knows the terms but cannot predict output for tricky questions; gets late binding and mutable default wrong |
| **3** | Correctly predicts output for standard tricky questions; explains LEGB, late binding fix, mutable default fix; sometimes needs prompting for edge cases |
| **4** | Answers all Core Python Round 1 questions before reading strong answer; explains `__defaults__`, `functools.wraps`; links to Java differences fluently |
| **5** | Answers follow-up escalations without hesitation; can construct novel tricky questions to explain concepts; articulates CPython implementation details (interning, `__defaults__` tuple) |

**Self-score after:** Python-Core-Hot-Interview-Master-Sheet + Round 1 mock

Current score: ___  
Date: ___  
Target: ≥ 4

---

## 3. Dimension 2 — Data Structures & Complexity

**Covers:** list / dict / set / deque internals, big-O, Counter, sorting, itertools.

| Score | Behavioral Anchor |
|---|---|
| **1** | Cannot state O(1) vs O(n) for dict lookup vs list search; no knowledge of hash table internals |
| **2** | Knows complexity facts but cannot explain why; cannot pick the right data structure for a problem; confuses `groupby` pre-sort requirement |
| **3** | Correctly states complexity for all standard operations; uses `Counter`, `defaultdict`, `heapq` appropriately; explains `groupby` trap when prompted |
| **4** | Proactively chooses optimal DS without prompting; compares `groupby` + sort vs `defaultdict` on memory grounds; discusses dict collision handling and load factor |
| **5** | Derives complexity proofs; discusses CPython's dict compact array implementation (Python 3.7+); suggests streaming alternatives for memory-constrained scenarios; explains TimSort stability and why it matters |

**Self-score after:** Python-Data-Structures-Internals-Complexity + Round 2 mock

Current score: ___  
Date: ___  
Target: ≥ 4

---

## 4. Dimension 3 — Concurrency & Async

**Covers:** GIL, threading vs multiprocessing, asyncio event loop, `gather`, `CancelledError`, `ContextVar`, semaphore, connection pools.

| Score | Behavioral Anchor |
|---|---|
| **1** | Confuses threading and async; does not know what GIL is; cannot explain why `requests.get()` inside `async def` is wrong |
| **2** | Knows GIL exists; knows async def vs def distinction; cannot explain event loop blocking or connection pool exhaustion |
| **3** | Correctly explains GIL impact on CPU-bound threading; describes `asyncio.gather` vs sequential await with timing; knows `run_in_executor` for blocking calls |
| **4** | Answers all Round 3 questions; correctly handles `CancelledError`; explains `ContextVar` vs `threading.local()` for request isolation; designs semaphore rate-limiting |
| **5** | Diagnoses production concurrency bugs from symptoms alone; discusses event loop internals (`select`/`epoll`); designs async architectures with bulkhead patterns; explains `TaskGroup` and structured concurrency |

**Self-score after:** Python-Concurrency + Python-AsyncIO sheets + Round 3 mock

Current score: ___  
Date: ___  
Target: ≥ 4

---

## 5. Dimension 4 — Python Internals

**Covers:** Decorators, descriptors, metaclasses, MRO, GC/weakref, `__slots__`, dunder methods.

| Score | Behavioral Anchor |
|---|---|
| **1** | Cannot write a basic decorator; no knowledge of `__get__` or MRO |
| **2** | Can write a single-layer decorator; knows `@property` exists but cannot explain the descriptor protocol; gets MRO order wrong |
| **3** | Writes 3-layer decorator factory with `functools.wraps`; explains descriptor protocol and data vs non-data descriptors; traces MRO with diamond inheritance |
| **4** | Answers all Round 4 questions; explains GC reference cycle issue and weakref fix; discusses `__slots__` memory savings; predicts `__init_subclass__` hook behavior |
| **5** | Implements a custom descriptor from scratch; explains metaclass `__new__` vs `__init__`; discusses CPython object model; knows when to use `__class_getitem__` and `__set_name__` |

**Self-score after:** Python-Decorators-Descriptors-Metaclasses + Round 4 mock

Current score: ___  
Date: ___  
Target: ≥ 3 (3 is enough for most senior roles; 4 for staff/principal)

---

## 6. Dimension 5 — Backend / API Design

**Covers:** FastAPI patterns, sync vs async route handlers, dependency injection, testing strategy, N+1, pagination.

| Score | Behavioral Anchor |
|---|---|
| **1** | Cannot explain FastAPI's dependency injection; no knowledge of N+1; has not written a test for an HTTP endpoint |
| **2** | Uses FastAPI but doesn't understand why `def` vs `async def` matters; writes tests that hit real databases; cannot describe connection pooling |
| **3** | Correctly explains sync vs async route handler thread-pool behavior; uses `dependency_overrides` for testing; identifies N+1 from a code sample and fixes with joinedload |
| **4** | Answers all Round 5 questions; designs pagination with cursor-based approach; explains Pydantic validation layers; discusses async context managers for resource cleanup |
| **5** | Designs complete FastAPI service architecture with middleware, lifespan, dependency scoping, and observability; discusses trade-offs of event sourcing vs REST; suggests load-shedding strategies |

**Self-score after:** Python-Backend-APIs-FastAPI-Flask + Round 5 mock

Current score: ___  
Date: ___  
Target: ≥ 4

---

## 7. Dimension 6 — Production Debugging & Performance

**Covers:** Memory leak diagnosis, latency spikes, cProfile, tracemalloc, py-spy, flame graphs.

| Score | Behavioral Anchor |
|---|---|
| **1** | Has no strategy for debugging a memory leak; cannot read a `cProfile` output |
| **2** | Knows cProfile exists; can describe memory leak in theory but cannot translate to actionable steps; no `tracemalloc` experience |
| **3** | Runs `cProfile` and reads cumulative time column; uses `tracemalloc` to identify top allocation lines; hypothesizes 2 of 3 root causes for a latency spike |
| **4** | Answers all Round 6 production scenarios; sequences investigation steps clearly; uses `py-spy` conceptually; designs monitoring to catch issues before p99 spikes; explains LRU eviction strategy |
| **5** | Has a complete observability stack in their mental model (traces, metrics, logs); diagnoses memory leak from `objgraph` output; explains GC pressure impact on p99; discusses off-heap memory in Python extensions |

**Self-score after:** Python-Performance-Profiling-Debugging + Python-Production-Debugging-Case-Studies + Round 6 mock

Current score: ___  
Date: ___  
Target: ≥ 3 (ideally 4 for senior roles)

---

## 8. Dimension 7 — Coding & Implementation Quality

**Covers:** Clean code under time pressure, edge case handling, correct complexity, testability.

| Score | Behavioral Anchor |
|---|---|
| **1** | Cannot implement rate limiter or similar LLD problem in 30 min; no type hints; no edge case consideration |
| **2** | Produces working code eventually but > 30 min; misses thread safety; no `__repr__` or interface clarity; can't explain complexity |
| **3** | Implements rate limiter (or equivalent) correctly within 25 min; includes thread safety with `Lock`; correctly handles edge cases (first request, expired window); states O(max_requests) space per user |
| **4** | Finishes in under 20 min; adds type hints; explains Redis extension; discusses `maxsize` and LRU eviction for memory management; handles `return_exceptions=True` patterns |
| **5** | Finishes in under 15 min with clean code; anticipates all follow-ups before they're asked; designs the interface for testability; discusses distributed consistency trade-offs unprompted |

**Self-score after:** Python-LLD-Machine-Coding + Round 7 mock

Current score: ___  
Date: ___  
Target: ≥ 3 minimum; ≥ 4 for MAANG

---

## 9. Dimension 8 — Tricky Output & Mental Model

**Covers:** Predicting output cold, MRO, `try/else/finally`, mutable defaults, late binding, `+=` on tuples vs lists.

| Score | Behavioral Anchor |
|---|---|
| **1** | Gets majority of tricky output questions wrong; has no mental model for execution order |
| **2** | Gets standard questions right (basic MRO, basic exceptions) but fails on compound scenarios (late binding + closure, `+=` on tuples) |
| **3** | Gets 3/5 tricky output questions in Round 8 without running code; can explain the wrong answer |
| **4** | Gets 4/5 or 5/5 cold; can construct a mental execution trace step-by-step when asked; explains the Java equivalent for each trap |
| **5** | Gets 5/5 instantly; can create novel tricky questions to test others; explains CPython bytecode reason behind each behavior |

**Self-score after:** Python-Tricky-Output-Questions + Round 8 mock

Current score: ___  
Date: ___  
Target: ≥ 4

---

## 10. Dimension 9 — Java Developer Bridge Fluency

**Covers:** Can you explain Python vs Java differences fluently? Do you default to Java patterns and get caught?

| Score | Behavioral Anchor |
|---|---|
| **1** | Cannot articulate any Python vs Java differences; writes Java-style code in Python (getter/setter methods, explicit returns for void, semicolons in mind) |
| **2** | Knows Python is different but uses Java mental models under pressure; misuses classes where functions suffice; forgets `self`; writes verbose Java-style OOP |
| **3** | Proactively mentions Java differences when asked; uses dataclasses instead of verbose classes; uses list comprehensions instead of loops; knows GIL has no Java equivalent |
| **4** | Flips between both perspectives fluently; uses Python idioms naturally under pressure; bridges every answer with "In Java you'd... In Python the idiomatic way is..."; explains why Python's duck typing changes design |
| **5** | Teaches the difference; constructs examples that would catch a Java developer off guard; has deeply internalized Python's data model vs Java's type system; discusses GIL, descriptor protocol, and generator protocol with Java equivalents |

**Self-score after:** Python-For-Java-Developers + entire track practice

Current score: ___  
Date: ___  
Target: ≥ 4

---

## 11. Dimension 10 — Communication & Explanation Quality

**Covers:** Can you explain your thinking, trade-offs, and choices clearly without rambling?

| Score | Behavioral Anchor |
|---|---|
| **1** | Answers are one word or one line; cannot explain reasoning; silently codes without narrating |
| **2** | Provides answers but doesn't explain why; can't discuss trade-offs; gives correct answers with wrong justification |
| **3** | Explains the approach before coding; mentions 1–2 trade-offs; can answer follow-up questions on choices made |
| **4** | Structures answer as: context → approach → implementation → trade-offs → alternatives; volunteers limitations; explicitly mentions complexity before being asked |
| **5** | Interview-polished delivery; uses precise vocabulary; pre-empts follow-up questions; turns every question into a mini-lecture; pauses to ask clarifying questions before jumping to solutions |

**Self-score after:** any mock round

Current score: ___  
Date: ___  
Target: ≥ 4

---

## 12. Composite Readiness Gate

### Minimum Readiness (Phone Screen / LC-style Round)

All of the following must be true:

| Dimension | Minimum Score |
|---|---:|
| Core Language Concepts | ≥ 3 |
| Data Structures & Complexity | ≥ 3 |
| Concurrency & Async | ≥ 3 |
| Coding & Implementation | ≥ 3 |
| Tricky Output | ≥ 3 |
| Communication | ≥ 3 |

**Gate: 6/6 at ≥ 3** → Clear for a phone screen.

---

### MAANG Senior Readiness (On-site / Virtual On-site)

All of the following must be true:

| Dimension | Minimum Score |
|---|---:|
| Core Language Concepts | ≥ 4 |
| Data Structures & Complexity | ≥ 4 |
| Concurrency & Async | ≥ 4 |
| Python Internals | ≥ 3 |
| Backend / API Design | ≥ 4 |
| Production Debugging | ≥ 3 |
| Coding & Implementation | ≥ 4 |
| Tricky Output | ≥ 4 |
| Java Bridge Fluency | ≥ 4 |
| Communication | ≥ 4 |

**Gate: 8/10 at ≥ 4 (all 10 at ≥ 3)** → Clear for on-site.

---

## 13. Weekly Self-Assessment Template

Use this table weekly. Fill it out after running a complete mock session.

```
Date: _______________
Session: Mock Round ___ / Lab set ___ / Gold Sheet review ___

| # | Dimension               | Score (1-5) | Notes / Gap to close          |
|---|-------------------------|-------------|-------------------------------|
| 1 | Core Language           |             |                               |
| 2 | Data Structures         |             |                               |
| 3 | Concurrency & Async     |             |                               |
| 4 | Python Internals        |             |                               |
| 5 | Backend / API Design    |             |                               |
| 6 | Production Debugging    |             |                               |
| 7 | Coding & Implementation |             |                               |
| 8 | Tricky Output           |             |                               |
| 9 | Java Bridge Fluency     |             |                               |
|10 | Communication           |             |                               |

Lowest 2 dimensions this week:
1. _______________________________________________
2. _______________________________________________

Targeted action for next session:
1. Re-run: ____________________________________
2. Read: ______________________________________
3. Implement: _________________________________

Phone screen gate cleared? YES / NO
On-site gate cleared?    YES / NO
```

---

## 14. Score Calibration Examples

These examples help you self-calibrate honestly.

### Calibration: Dimension 3 — Concurrency & Async

**Score 2 example answer** to "Why is `requests.get()` bad in FastAPI?":
> "Because FastAPI uses async and `requests` doesn't support async."

**Score 3 example answer:**
> "FastAPI is async. `requests.get()` is blocking — it holds up the thread."

**Score 4 example answer:**
> "FastAPI's event loop runs on a single OS thread. `requests.get()` blocks that thread for the full network round trip — typically 50–200ms. During that time, the event loop can't process any other requests. Under 50 concurrent users this can take your throughput from thousands of requests per second to single digits. Fix: use `httpx.AsyncClient` which `await`s the I/O and yields the thread back to the loop."

**Score 5 example answer (adds):**
> "...If the sync library can't be replaced, use `loop.run_in_executor(ThreadPoolExecutor(...), sync_fn, *args)` — this offloads to a thread pool so the event loop thread is never blocked. Set the pool size to roughly match your expected concurrent blocking calls. For CPU-heavy blocking code, `ProcessPoolExecutor` to bypass the GIL."

---

### Calibration: Dimension 7 — Coding Quality

**Score 2 answer** to rate limiter LLD:
```python
class RateLimiter:
    def __init__(self, limit, window):
        self.limit = limit
        self.window = window
        self.calls = {}

    def check(self, user):
        import time
        now = time.time()
        if user not in self.calls:
            self.calls[user] = []
        self.calls[user] = [t for t in self.calls[user] if now - t < self.window]
        if len(self.calls[user]) >= self.limit:
            return False
        self.calls[user].append(now)
        return True
```
Missing: no thread safety, list comprehension is O(n) instead of deque popleft, no type hints.

**Score 4 answer additions:** `threading.Lock`, `deque` with `popleft`, `time.monotonic()`, type hints, docstring, Redis extension plan.

---

## 15. Per-Topic Linked Resources

| Dimension | Primary Gold Sheet | Lab | Mock Round |
|---|---|---|---|
| Core Language | Python-Core-Hot-Interview-Master-Sheet | Lab 01–03, 15 | Round 1 |
| Data Structures | Python-Data-Structures-Internals-Complexity | Lab 04, 14 | Round 2 |
| Concurrency & Async | Python-Concurrency + Python-AsyncIO | Lab 06–07, 13 | Round 3 |
| Python Internals | Python-Decorators-Descriptors-Metaclasses | Lab 08–09 | Round 4 |
| Backend / API | Python-Backend-APIs-FastAPI-Flask | — | Round 5 |
| Production Debug | Python-Performance-Profiling + Python-Production-Debugging | Lab 10–11 | Round 6 |
| Coding Quality | Python-LLD-Machine-Coding | All labs | Round 7 |
| Tricky Output | Python-Tricky-Output-Questions | Lab 02, 05, 15, 16 | Round 8 |
| Java Bridge | Python-For-Java-Developers | All labs | All rounds |
| Communication | Python-Mock-Interview-Scripts | — | All rounds |

---

## 16. Final Revision Checklist

- [ ] Scored all 10 dimensions after a complete mock session
- [ ] Identified lowest 2 dimensions and have a targeted plan
- [ ] Phone screen readiness gate: 6/6 at ≥ 3
- [ ] On-site readiness gate: 8/10 at ≥ 4
- [ ] Ran calibration examples for at least Dimensions 3 and 7
- [ ] Filled in Weekly Self-Assessment Template at least once
- [ ] Confirmed no dimension is still at score 1
- [ ] Scheduled next mock session to re-measure lowest dimensions
