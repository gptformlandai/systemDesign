# Python 2-Week & 4-Week Mastery Roadmaps - Gold Sheet

> **Track File #31 - Group 6: Practice Upgrade**
> For: Java developer | Level: MAANG interview prep | Mode: follow the plan, mark days complete

---

## 1. Before You Start

### Prerequisites Checklist

- [ ] Python 3.12+ installed for broad compatibility, or Python 3.14 for current-version practice (`python -VV`)
- [ ] A working virtualenv (`python -m venv .venv && source .venv/bin/activate`)
- [ ] uv installed or understood well enough to run `uv sync`, `uv add`, and `uv run`
- [ ] VS Code with Python extension OR PyCharm installed
- [ ] Packages installed through the project tool: `uv add httpx pytest pytest-asyncio pydantic fastapi uvicorn` or `python -m pip install ...`
- [ ] Read [Python-Install-CLI-IDE-uv-venv-Gold-Sheet.md](../00-Setup/Python-Install-CLI-IDE-uv-venv-Gold-Sheet.md) if `python`, `pip`, IDE interpreter, or virtualenv behavior feels unclear
- [ ] Read [Python-For-Java-Developers-Gold-Sheet.md](../01-Starter-Path/Python-For-Java-Developers-Gold-Sheet.md) at minimum before Day 1

### Java Developer Acceleration

You have 6 years of Java. Skip the following as "already known concept, just check Python syntax":

| Java Concept | Python Equivalent | Read? |
|---|---|---|
| `interface` / abstract class | `ABC`, `Protocol` | Skim §6 of Python-OOP sheet |
| `HashMap` | `dict` — O(1) average, same as Java | Just know CPython internals differ |
| `synchronized` / `Lock` | `threading.Lock` / `asyncio.Lock` | Read fully |
| `CompletableFuture` | `asyncio.gather` / `Task` | Read fully |
| `Optional<T>` | `Optional[T]` from `typing`, or just `T | None` | Skim |
| JUnit + Mockito | `pytest` + `unittest.mock` | Read fully |
| Maven / Gradle | `pip` + `Poetry` | Skim §3–4 of Modules sheet |
| Checked exceptions | Does not exist in Python | Read the exception sheet |

### Choosing Your Plan

| Situation | Plan |
|---|---|
| Interview in 14 days or fewer | 2-Week Sprint |
| Interview in 3–4 weeks | 4-Week Deep Dive |
| Interview in 7 days | Use 2-Week Sprint, do Days 1–7 only; focus on Groups 1–3 |
| Revisiting after a break | Jump to Weeks 3–4 of the 4-Week plan |

### Optional Day 0 - Setup Stabilization

Use this when you are new to Python or the machine is not trustworthy yet.

| Time | Activity | File |
|---|---|---|
| 0:00-0:30 | Verify interpreter, `sys.executable`, `python -m pip`, and IDE interpreter | Python-Install-CLI-IDE-uv-venv-Gold-Sheet.md |
| 0:30-1:00 | Create `.venv` or uv project, run first script, run first pytest test | Python-Install-CLI-IDE-uv-venv-Gold-Sheet.md |

**Day 0 exit gate:** Terminal, IDE, and test runner use the same Python interpreter.

---

## 2. 2-Week Sprint Plan (Java Developer Edition)

**Total commitment:** ~2 hours/day, 14 days = ~28 hours  
**Goal:** Phone screen + on-site readiness in 2 weeks  
**Rule:** Hard-stop each day at the time limit. Consistency over perfection.

---

### Week 1 — Build the Foundation

#### Day 1 (2 hrs) — Python Mental Model Reset

| Time | Activity | File |
|---|---|---|
| 0:00–0:30 | Read §1–5: Syntax, types, truthiness, unpacking | Python-For-Java-Developers-Gold-Sheet.md |
| 0:30–1:00 | Read §6–10: OOP differences, no interfaces, duck typing | Python-For-Java-Developers-Gold-Sheet.md |
| 1:00–1:30 | Read §1–4: Core tricky facts, `is` vs `==`, interning | Python-Core-Hot-Interview-Master-Sheet.md |
| 1:30–2:00 | Run Lab 01 (mutable default), Lab 02 (late binding) | Python-Runnable-Mini-Labs.md |

**Day 1 exit gate:** Can predict mutable default and late binding output cold.

---

#### Day 2 (2 hrs) — Types & Mutability

| Time | Activity | File |
|---|---|---|
| 0:00–0:45 | Read §1–8: Mutability, interning, copy types | Python-Data-Types-Mutability-Deep-Dive.md |
| 0:45–1:15 | Read §1–6: Functions, `*args`/`**kwargs`, closures | Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md |
| 1:15–1:45 | Run Lab 03 (shallow/deep copy), Lab 04 (generator memory) | Python-Runnable-Mini-Labs.md |
| 1:45–2:00 | Active Recall Q&A: Group 1 questions (10 questions cold) | Python-Active-Recall-Question-Bank.md §2 |

**Day 2 exit gate:** Can explain shallow vs deep copy; can write a generator function.

---

#### Day 3 (2 hrs) — OOP & Collections

| Time | Activity | File |
|---|---|---|
| 0:00–0:40 | Read §1–8: Dataclasses, dunder methods, MRO | Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md |
| 0:40–1:20 | Read §1–8: Comprehensions, itertools, generators | Python-Collections-Comprehensions-Iteration-Gold-Sheet.md |
| 1:20–1:50 | Run Lab 05 (class vs instance), Lab 16 (dataclass vs plain) | Python-Runnable-Mini-Labs.md |
| 1:50–2:00 | Score Dimension 1: Core Language Concepts (target ≥ 3) | Python-Interview-Scoring-Rubrics.md §2 |

**Day 3 exit gate:** Can write a `@dataclass`, explain MRO print order, and predict `__dict__` contents.

---

#### Day 4 (2 hrs) — Data Structures Internals

| Time | Activity | File |
|---|---|---|
| 0:00–0:50 | Read §1–8: dict/list/set internals, deque, complexity | Python-Data-Structures-Internals-Complexity-Gold-Sheet.md |
| 0:50–1:20 | Read §1–5: Type hints, Pydantic basics | Python-Type-Hints-Pydantic-Validation-Gold-Sheet.md |
| 1:20–1:50 | Mock Round 2 timed (15 min) — Data Structures | Python-Mock-Interview-Scripts.md §3 |
| 1:50–2:00 | Score Dimension 2 (target ≥ 3) | Python-Interview-Scoring-Rubrics.md §3 |

**Day 4 exit gate:** Can state O() for all common dict/list/set operations and explain why.

---

#### Day 5 (2 hrs) — Exceptions, I/O, Packaging

| Time | Activity | File |
|---|---|---|
| 0:00–0:40 | Read §1–8: Exception hierarchy, context managers, `__enter__`/`__exit__` | Python-Exception-Handling-Context-Managers-Gold-Sheet.md |
| 0:40–1:10 | Read §1–6: JSON, pickle, file I/O | Python-File-IO-Serialization-JSON-Pickle-Gold-Sheet.md |
| 1:10–1:40 | Run Lab 15 (try/except/else/finally) — trace all paths cold | Python-Runnable-Mini-Labs.md |
| 1:40–2:00 | Active Recall: Group 2 questions (10 questions cold) | Python-Active-Recall-Question-Bank.md §3 |

**Day 5 exit gate:** Can draw the execution path for all 4 `try/except/else/finally` combinations.

---

#### Day 6 (2 hrs) — Concurrency: GIL & Threading

| Time | Activity | File |
|---|---|---|
| 0:00–0:55 | Read §1–8: GIL, thread safety, ThreadPoolExecutor, multiprocessing | Python-Concurrency-Threading-Multiprocessing-MAANG-Master-Sheet.md |
| 0:55–1:30 | Run Lab 06 (GIL benchmark — threads vs multiprocessing) | Python-Runnable-Mini-Labs.md |
| 1:30–2:00 | Read Round 3 Q1–Q2 strong answers; self-answer before reading | Python-Mock-Interview-Scripts.md §4 |

**Day 6 exit gate:** Can explain exactly why threading doesn't speed up CPU-bound work in default CPython, and can mention Python 3.13+ free-threaded builds as a caveat.

---

#### Day 7 (2 hrs) — Async / Await

| Time | Activity | File |
|---|---|---|
| 0:00–0:55 | Read §1–9: Event loop, coroutines, gather, CancelledError, ContextVar | Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md |
| 0:55–1:30 | Run Lab 07 (sequential vs gather timing), Lab 13 (ContextVar isolation) | Python-Runnable-Mini-Labs.md |
| 1:30–1:55 | Mock Round 3 timed (20 min) — Async | Python-Mock-Interview-Scripts.md §4 |
| 1:55–2:00 | Score Dimension 3 (target ≥ 3) | Python-Interview-Scoring-Rubrics.md §4 |

**Day 7 exit gate:** Can explain `asyncio.gather` vs sequential with concrete timing numbers.

---

### Week 1 Checkpoint

Run this self-check at end of Day 7:

- [ ] Dimensions 1, 2, 3 all scored ≥ 3
- [ ] Ran Labs 01, 02, 03, 04, 05, 06, 07, 13, 15, 16
- [ ] Completed mock Rounds 2 and 3 timed
- [ ] Can answer "mutable default", "GIL", "asyncio.gather vs sequential" cold

If any dimension < 3: re-read that day's sheet and re-run the corresponding mock round before continuing.

---

### Week 2 — Senior Depth + Practice

#### Day 8 (2 hrs) — Performance & Profiling

| Time | Activity | File |
|---|---|---|
| 0:00–0:55 | Read §1–10: cProfile, tracemalloc, py-spy, lru_cache | Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md |
| 0:55–1:30 | Run Lab 10 (cProfile), Lab 11 (tracemalloc), Lab 12 (lru_cache) | Python-Runnable-Mini-Labs.md |
| 1:30–2:00 | Read §1–6: Production case studies (memory leak, latency spike) | Python-Production-Debugging-Case-Studies-Gold-Sheet.md |

**Day 8 exit gate:** Can read `cProfile` output and identify the bottleneck function.

---

#### Day 9 (2 hrs) — Decorators, Internals, Testing

| Time | Activity | File |
|---|---|---|
| 0:00–0:45 | Read §1–8: 3-layer decorator, descriptor protocol, `functools.wraps` | Python-Decorators-Descriptors-Metaclasses-Deep-Dive-Gold-Sheet.md |
| 0:45–1:10 | Run Lab 08 (decorator factory), Lab 09 (property/descriptor) | Python-Runnable-Mini-Labs.md |
| 1:10–2:00 | Read §1–10: pytest, fixtures, mock.patch, Testcontainers | Python-Testing-Pytest-Mocking-Testcontainers-Gold-Sheet.md |

**Day 9 exit gate:** Can write a 3-layer retry decorator with `functools.wraps` from memory.

---

#### Day 10 (2 hrs) — FastAPI + Scenarios

| Time | Activity | File |
|---|---|---|
| 0:00–0:50 | Read §1–10: FastAPI patterns, dependency injection, async route handlers | Python-Backend-APIs-FastAPI-Flask-Patterns-Gold-Sheet.md |
| 0:50–1:20 | Read full scenario sheet — dict/list mutability, request context | Python-Dict-List-Mutability-Request-Scenario-Gold-Sheet.md |
| 1:20–2:00 | Mock Round 5 timed (20 min) — Backend + Testing | Python-Mock-Interview-Scripts.md §6 |

**Day 10 exit gate:** Can explain why `requests.get()` inside `async def` is catastrophic; can describe `dependency_overrides`.

---

#### Day 11 (2 hrs) — LLD Machine Coding

| Time | Activity | File |
|---|---|---|
| 0:00–0:30 | Read §1–6: LLD patterns, rate limiter, cache, event bus | Python-LLD-Machine-Coding-Patterns-Gold-Sheet.md |
| 0:30–1:30 | Mock Round 7 timed (30 min) — implement rate limiter cold | Python-Mock-Interview-Scripts.md §8 |
| 1:30–2:00 | Score Dimension 7 (target ≥ 3); fix gaps | Python-Interview-Scoring-Rubrics.md §8 |

**Day 11 exit gate:** Implemented rate limiter in < 30 min, thread-safe, correct complexity.

---

#### Day 12 (2 hrs) — Tricky Output Drill

| Time | Activity | File |
|---|---|---|
| 0:00–0:50 | Read §1–10: All tricky output categories | Python-Tricky-Output-Questions-Gold-Sheet.md |
| 0:50–1:15 | Mock Round 8 timed (10 min) — 5 questions cold, no code | Python-Mock-Interview-Scripts.md §9 |
| 1:15–1:45 | Active Recall: Groups 3, 4, 5 questions (20 questions cold) | Python-Active-Recall-Question-Bank.md §4–6 |
| 1:45–2:00 | Score Dimension 8 (target ≥ 3) | Python-Interview-Scoring-Rubrics.md §9 |

**Day 12 exit gate:** Scored ≥ 3/5 on Round 8 cold.

---

#### Day 13 (2 hrs) — Full Mock Day

| Time | Activity | File |
|---|---|---|
| 0:00–0:15 | Mock Round 1 timed — Core Python | Python-Mock-Interview-Scripts.md §2 |
| 0:15–0:35 | Mock Round 6 timed — Production Scenarios | Python-Mock-Interview-Scripts.md §7 |
| 0:35–1:05 | Mock Round 7 timed — LLD (second attempt) | Python-Mock-Interview-Scripts.md §8 |
| 1:05–1:30 | Fill Weekly Self-Assessment Template — all 10 dimensions | Python-Interview-Scoring-Rubrics.md §13 |
| 1:30–2:00 | Java Bridge review: revisit weakest Java↔Python comparisons | Python-For-Java-Developers-Gold-Sheet.md |

**Day 13 exit gate:** Fill in all 10 dimension scores. Identify 2 lowest. Note targeted action.

---

#### Day 14 (2 hrs) — Interview Day Prep

| Time | Activity | Notes |
|---|---|---|
| 0:00–0:30 | Re-run weakest mock round (identified Day 13) | Only the round, timed |
| 0:30–1:00 | Re-run Active Recall on lowest 2 dimension topics | 15 questions cold |
| 1:00–1:20 | Read Composite Readiness Gate | Python-Interview-Scoring-Rubrics.md §12 |
| 1:20–1:40 | Re-run Lab 14 (groupby trap), Lab 02 (late binding) — most common surprises | Python-Runnable-Mini-Labs.md |
| 1:40–2:00 | Mental warm-up: read strong answers for Rounds 1 and 3 out loud | Python-Mock-Interview-Scripts.md §2 §4 |

**Day 14 readiness gate:** 6/6 dimensions at ≥ 3 for phone screen. 8/10 at ≥ 4 for on-site.

---

### 2-Week Sprint Summary

| Week | Days | Focus | Labs Covered | Mock Rounds |
|---|---|---|---|---|
| 1 | 1–7 | Foundation: types, collections, concurrency, async | 01–07, 13, 15, 16 | Rounds 2, 3 |
| 2 | 8–14 | Senior: profiling, internals, backend, LLD, full mock | 08–12 | Rounds 1, 5, 6, 7, 8 |

---

## 3. 4-Week Deep Dive Plan (Java Developer Edition)

**Total commitment:** ~1.5–2 hours/day, 28 days = ~42–56 hours  
**Goal:** Deep mastery + on-site MAANG readiness  
**Pacing:** Weekdays = 1.5 hrs reading + practice. Weekends = 2.5 hrs mock sessions + labs.

---

### Week 1 — Core Python Mastery

#### Day 1 — Python for Java Developers + Core Tricky Facts (1.5 hrs)
- Read: [Python-For-Java-Developers-Gold-Sheet.md](../01-Starter-Path/Python-For-Java-Developers-Gold-Sheet.md) (full)
- Read: [Python-Core-Hot-Interview-Master-Sheet.md](../01-Starter-Path/Python-Core-Hot-Interview-Master-Sheet.md) §1–8
- Task: List 5 Java habits that will cause bugs in Python (write them down)

#### Day 2 — Data Types & Mutability (1.5 hrs)
- Read: [Python-Data-Types-Mutability-Deep-Dive.md](../01-Starter-Path/Python-Data-Types-Mutability-Deep-Dive.md) (full)
- Run: Lab 01 (mutable default), Lab 03 (shallow/deep copy)
- Active Recall: 5 questions cold from §2 of Question Bank

#### Day 3 — Functions: Scope, Closures, Args (1.5 hrs)
- Read: [Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md](../01-Starter-Path/Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md) (full)
- Run: Lab 02 (late binding), Lab 08 (decorator factory)
- Active Recall: 5 questions from §2

#### Day 4 — OOP, Dataclasses, Dunder Methods (1.5 hrs)
- Read: [Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md](../01-Starter-Path/Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md) (full)
- Run: Lab 05 (class vs instance var), Lab 16 (dataclass vs plain)
- Active Recall: 5 more questions

#### Day 5 — Collections, Comprehensions, Generators (1.5 hrs)
- Read: [Python-Collections-Comprehensions-Iteration-Gold-Sheet.md](../01-Starter-Path/Python-Collections-Comprehensions-Iteration-Gold-Sheet.md) (full)
- Run: Lab 04 (generator memory), Lab 14 (groupby trap)
- Active Recall: 5 questions

#### Day 6 (Weekend) — Exception Handling + Week 1 Mock (2.5 hrs)
- Read: [Python-Exception-Handling-Context-Managers-Gold-Sheet.md](../01-Starter-Path/Python-Exception-Handling-Context-Managers-Gold-Sheet.md) (full) — 1 hr
- Run: Lab 15 (try/except/else/finally full trace) — 30 min
- Mock Round 1 timed (15 min) — Core Python
- Mock Round 8 timed (10 min) — Tricky Output (first run)
- Score Dimensions 1, 8, 9 — record baseline

#### Day 7 (Weekend) — Week 1 Deep Revision (2.5 hrs)
- Revisit any Day 1–5 sheet with score < 3
- Re-run 2 labs of your choice
- Write from memory: 3 Python vs Java differences for each of: closures, OOP, exceptions
- Score Dimension 10 (Communication) from how you answered the write-from-memory task

**Week 1 milestone:** Dimensions 1, 8 ≥ 3. Can run all Group 1 labs without looking at expected output.

---

### Week 2 — Intermediate Backend

#### Day 8 — Data Structures Internals (1.5 hrs)
- Read: [Python-Data-Structures-Internals-Complexity-Gold-Sheet.md](../02-Intermediate-Backend/Python-Data-Structures-Internals-Complexity-Gold-Sheet.md) (full)
- Active Recall: §3 questions (Data Structures group)
- Challenge: state O() for 10 operations from memory before looking

#### Day 9 — Type Hints & Pydantic (1.5 hrs)
- Read: [Python-Type-Hints-Pydantic-Validation-Gold-Sheet.md](../02-Intermediate-Backend/Python-Type-Hints-Pydantic-Validation-Gold-Sheet.md) (full)
- Task: Write a Pydantic model with validators from scratch; run it
- Note Java equivalents: `@Valid`, Bean Validation, `@NotNull`

#### Day 10 — Packaging, Modules, Venv (1.5 hrs)
- Read: [Python-Modules-Packaging-Venv-Pip-Poetry-Gold-Sheet.md](../02-Intermediate-Backend/Python-Modules-Packaging-Venv-Pip-Poetry-Gold-Sheet.md) §1–10
- Task: Create a `pyproject.toml` from scratch; understand `__init__.py` role
- Note Java equivalents: Maven, POM, classpath

#### Day 11 — File I/O & Serialization (1.5 hrs)
- Read: [Python-File-IO-Serialization-JSON-Pickle-Gold-Sheet.md](../02-Intermediate-Backend/Python-File-IO-Serialization-JSON-Pickle-Gold-Sheet.md) (full)
- Task: Write a generator that streams a 100MB NDJSON file line by line without loading it

#### Day 12 — FastAPI & Backend Patterns (1.5 hrs)
- Read: [Python-Backend-APIs-FastAPI-Flask-Patterns-Gold-Sheet.md](../02-Intermediate-Backend/Python-Backend-APIs-FastAPI-Flask-Patterns-Gold-Sheet.md) (full)
- Task: Write a FastAPI route with a `get_db` dependency + `dependency_overrides` test skeleton

#### Day 13 (Weekend) — Data Structures Mock + Scenario (2.5 hrs)
- Mock Round 2 timed (15 min) — Data Structures & Complexity
- Read: [Python-Data-Processing-Interview-Scenarios-Gold-Sheet.md](../04-Scenario-Practice/Python-Data-Processing-Interview-Scenarios-Gold-Sheet.md) §1–8
- Read: [Python-Dict-List-Mutability-Request-Scenario-Gold-Sheet.md](../04-Scenario-Practice/Python-Dict-List-Mutability-Request-Scenario-Gold-Sheet.md) §1–8
- Score Dimensions 2, 5 — record progress

#### Day 14 (Weekend) — Backend Mock + Scoring Review (2.5 hrs)
- Mock Round 5 timed (20 min) — Backend & Testing
- Re-run Round 1 if Dimension 1 still < 4
- Fill Weekly Self-Assessment for Dimensions 1–5

**Week 2 milestone:** Dimensions 2, 5 ≥ 3. Can explain N+1 fix, `dependency_overrides`, `Counter.most_common`.

---

### Week 3 — Senior MAANG Depth

#### Day 15 — Concurrency: GIL, Threading, Multiprocessing (1.5 hrs)
- Read: [Python-Concurrency-Threading-Multiprocessing-MAANG-Master-Sheet.md](../03-Senior-MAANG/Python-Concurrency-Threading-Multiprocessing-MAANG-Master-Sheet.md) (full)
- Run: Lab 06 (GIL CPU benchmark) — measure on your machine
- Note: Java thread model vs default CPython GIL — write the comparison, then add the Python 3.13+ free-threaded caveat

#### Day 16 — Async/Await Deep Dive (1.5 hrs)
- Read: [Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md](../03-Senior-MAANG/Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md) (full)
- Run: Lab 07 (sequential vs gather timing), Lab 13 (ContextVar isolation)
- Note: `CompletableFuture.allOf` vs `asyncio.gather` — write the comparison

#### Day 17 — Performance, Profiling, Debugging (1.5 hrs)
- Read: [Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md](../03-Senior-MAANG/Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md) (full)
- Run: Lab 10 (cProfile), Lab 11 (tracemalloc), Lab 12 (lru_cache speedup)
- Task: Profile a function you write yourself and identify the bottleneck

#### Day 18 — Testing: Pytest, Mock, Testcontainers (1.5 hrs)
- Read: [Python-Testing-Pytest-Mocking-Testcontainers-Gold-Sheet.md](../03-Senior-MAANG/Python-Testing-Pytest-Mocking-Testcontainers-Gold-Sheet.md) (full)
- Task: Write a `pytest` fixture using `tmp_path`; mock `httpx.AsyncClient.get`
- Note: JUnit 5 `@ExtendWith` vs pytest fixtures — write the comparison

#### Day 19 — Decorators, Descriptors, Metaclasses (1.5 hrs)
- Read: [Python-Decorators-Descriptors-Metaclasses-Deep-Dive-Gold-Sheet.md](../05-Special-Interview-Rounds/Python-Decorators-Descriptors-Metaclasses-Deep-Dive-Gold-Sheet.md) (full)
- Run: Lab 08 (decorator factory), Lab 09 (property/descriptor)
- Challenge: Write `@memoize` from scratch without `functools.lru_cache`

#### Day 20 (Weekend) — Concurrency + Internals Mock (2.5 hrs)
- Mock Round 3 timed (20 min) — Async & Concurrency
- Mock Round 4 timed (15 min) — Python Internals
- Score Dimensions 3, 4 — record progress
- Active Recall: Group 3 questions (15 cold) from Question Bank

#### Day 21 (Weekend) — Production Debugging + Scenario (2.5 hrs)
- Read: [Python-Production-Debugging-Case-Studies-Gold-Sheet.md](../05-Special-Interview-Rounds/Python-Production-Debugging-Case-Studies-Gold-Sheet.md) (full) — 1 hr
- Read: [Python-Async-API-Concurrency-Scenario-Gold-Sheet.md](../04-Scenario-Practice/Python-Async-API-Concurrency-Scenario-Gold-Sheet.md) §1–8 — 30 min
- Mock Round 6 timed (20 min) — Senior Production Scenarios
- Score Dimension 6 — record progress

**Week 3 milestone:** Dimensions 3, 4, 6 >= 3. Core labs run at least once; labs 17-20 can be completed during gap-fill review.

---

### Week 4 — Integration, LLD, and Final Readiness

#### Day 22 — LLD Machine Coding (1.5 hrs)
- Read: [Python-LLD-Machine-Coding-Patterns-Gold-Sheet.md](../05-Special-Interview-Rounds/Python-LLD-Machine-Coding-Patterns-Gold-Sheet.md) (full)
- Task: Implement in-memory LRU cache from scratch (no `functools.lru_cache`)
- Note: Java `LinkedHashMap(capacity, 0.75, true)` LRU trick vs Python doubly-linked dict approach

#### Day 23 — Tricky Output Mastery (1.5 hrs)
- Read: [Python-Tricky-Output-Questions-Gold-Sheet.md](../05-Special-Interview-Rounds/Python-Tricky-Output-Questions-Gold-Sheet.md) (full)
- Mock Round 8 timed (10 min) — second cold attempt
- Target: ≥ 4/5 without running code
- Score Dimension 8

#### Day 24 — Scenario-Based Quick Revision (1.5 hrs)
- Read: [Python-Scenario-Based-Quick-Revision-Gold-Sheet.md](../04-Scenario-Practice/Python-Scenario-Based-Quick-Revision-Gold-Sheet.md) (full)
- Active Recall: Groups 4 + 5 questions (20 questions cold)
- Focus on: correct data structure choice + complexity justification

#### Day 25 — Java Bridge Final Calibration (1.5 hrs)
- Active Recall: all Java Bridge questions from Groups 1–6 in Question Bank
- For each weak answer: re-read the linked Gold Sheet section
- Score Dimension 9 (Java Bridge Fluency) — target ≥ 4

#### Day 26 — Full Mock Interview Day 1 (2 hrs)
- Mock Round 1 (15 min) + Round 2 (15 min) + Round 7 LLD (30 min)
- After: self-score all 3 rounds against rubric anchors
- Write down top 3 weak spots observed

#### Day 27 (Weekend) — Full Mock Interview Day 2 (2.5 hrs)
- Mock Round 3 (20 min) + Round 5 (20 min) + Round 6 (20 min)
- After: fill complete Weekly Self-Assessment all 10 dimensions
- Identify any dimension still < 4; re-read that day's sheet
- Score Composite Readiness Gate — record: phone screen cleared? on-site cleared?

#### Day 28 (Weekend) — Final Interview Prep Day (2 hrs)

| Time | Activity |
|---|---|
| 0:00–0:20 | Re-run weakest mock round (identified Day 27) |
| 0:20–0:45 | Active Recall: all 10 dimensions' hardest questions (30 cold) |
| 0:45–1:10 | Re-read Python-For-Java-Developers §10–16 (idiomatic patterns) |
| 1:10–1:30 | Re-run Labs 02, 06, 07 (most interview-frequent surprises) |
| 1:30–1:50 | Read strong answers for Rounds 3 and 6 aloud |
| 1:50–2:00 | Check Composite Readiness Gate — final go/no-go assessment |

**Day 28 readiness gate:** 8/10 dimensions at ≥ 4; all 10 at ≥ 3.

---

### 4-Week Deep Dive Summary

| Week | Days | Focus | Key Deliverables |
|---|---|---|---|
| 1 | 1–7 | Core Python foundation | All Group 1 sheets + 9 labs + Round 1, 8 mocks |
| 2 | 8–14 | Intermediate backend | All Group 2 sheets + Rounds 2, 5 mocks |
| 3 | 15-21 | Senior MAANG depth | Groups 3, 5 sheets + core labs + Rounds 3, 4, 6 mocks |
| 4 | 22–28 | Integration + full mock | LLD, tricky output, 3 full mock days + readiness gate |

---

## 4. Interview Week Protocol

Use this in the 7 days before the actual interview.

| Day Before | Activity |
|---|---|
| Day -7 | Run full 4-round mock (Rounds 1, 3, 7, 8); score all dimensions |
| Day -5 | Re-read 2 weakest Gold Sheets; run 3 labs from those topics |
| Day -3 | Mock Round 6 (Production Scenarios) + Round 5 (Backend); check gate |
| Day -2 | Light review only: Active Recall 20 questions cold, no new material |
| Day -1 | Rest. Read strong answers for Rounds 1 and 3 — 30 min max. Sleep. |
| Interview day | 30-min warm-up: 10 active recall questions cold; re-read Java Bridge §1–3 |

---

## 5. Common Pitfalls for Java Developers (Week-by-Week)

### Week 1 Pitfalls
- **Defaulting to getters/setters** — use `@property` or `@dataclass`
- **Forgetting `self`** — every instance method needs `self` explicitly
- **Using `==` for identity** — reserve `is` only for `None`, `True`, `False`

### Week 2 Pitfalls
- **Writing Java-style loops** — prefer list comprehensions and generators
- **Ignoring type hints** — they are not enforced at runtime; Pydantic enforces them
- **Using `dict.get()` when a `KeyError` is correct** — don't silence errors unnecessarily

### Week 3 Pitfalls
- **Thinking threads bypass the GIL** — they do not for CPU work on default CPython
- **Using `threading.local()` in async code** — use `ContextVar` instead
- **Not re-raising `CancelledError`** — this causes silent deadlocks in async code

### Week 4 Pitfalls
- **Over-engineering LLD solutions** — MAANG interviewers want clean, correct, explained; not every pattern applied
- **Forgetting `functools.wraps`** — always add it; missing it is an easy gotcha
- **Not stating complexity before coding** — say O() before writing a single line

---

## 6. Quick-Reference: Track Files by Day

| Track File | File Name | 2-Week Day | 4-Week Day |
|---:|---|---:|---:|
| #1 | Python-Core-Hot-Interview-Master-Sheet | 1 | 1 |
| #2 | Python-For-Java-Developers-Gold-Sheet | 1 | 1 |
| #3 | Python-Data-Types-Mutability-Deep-Dive | 2 | 2 |
| #4 | Python-Functions-Scope-Closures-Args-Kwargs | 2 | 3 |
| #5 | Python-OOP-Dataclasses-Dunder-Methods | 3 | 4 |
| #6 | Python-Collections-Comprehensions-Iteration | 3 | 5 |
| #7 | Python-Exception-Handling-Context-Managers | 5 | 6 |
| #8 | Python-Data-Structures-Internals-Complexity | 4 | 8 |
| #9 | Python-Type-Hints-Pydantic-Validation | 4 | 9 |
| #10 | Python-Modules-Packaging-Venv-Pip-Poetry | 5 | 10 |
| #11 | Python-File-IO-Serialization-JSON-Pickle | 5 | 11 |
| #12 | Python-Backend-APIs-FastAPI-Flask-Patterns | 10 | 12 |
| #12a | Python-Data-Engineering-Pandas-Polars | 10 | 14 |
| #12b | Python-Pattern-Matching-Match-Case | 11 | 14 |
| #12c | Python-Time-Money-UUID-Locale | 11 | 14 |
| #13 | Python-Internals-Memory-GC-GIL | 6 | 15 |
| #14 | Python-Concurrency-Threading-Multiprocessing | 6 | 15 |
| #15 | Python-AsyncIO-Modern-Concurrency | 7 | 16 |
| #16 | Python-Performance-Profiling-Debugging | 8 | 17 |
| #17 | Python-Testing-Pytest-Mocking-Testcontainers | 9 | 18 |
| #18 | Python-Production-Engineering-Best-Practices | 10 | 21 |
| #18a | Python-AsyncIO-Database-Drivers | 10 | 21 |
| #18b | Python-Memory-Optimization-Slots-Object-Pooling | 8 | 17 |
| #18c | Python-Modern-3-12-3-13-3-14-3-15 | 11 | 20 |
| #18d | Python-Security-OWASP-Supply-Chain | 11 | 20 |
| #18e | Python-Observability-OpenTelemetry-Logging-Metrics | 11 | 21 |
| #18f | Python-Django-Celery-Redis-Worker-Patterns | 11 | 22 |
| #19 | Python-Scenario-Based-Quick-Revision | 12 | 24 |
| #20 | Python-Dict-List-Mutability-Request-Scenario | 10 | 13 |
| #21 | Python-Async-API-Concurrency-Scenario | 10 | 21 |
| #22 | Python-Data-Processing-Interview-Scenarios | 10 | 13 |
| #23 | Python-Tricky-Output-Questions | 12 | 23 |
| #24 | Python-Decorators-Descriptors-Metaclasses | 9 | 19 |
| #25 | Python-LLD-Machine-Coding-Patterns | 11 | 22 |
| #26 | Python-Production-Debugging-Case-Studies | 8 | 21 |
| #27 | Python-Active-Recall-Question-Bank | Daily | Daily |
| #28 | Python-Runnable-Mini-Labs | Daily | Daily |
| #29 | Python-Mock-Interview-Scripts | Rounds | Rounds |
| #30 | Python-Interview-Scoring-Rubrics | Weekly | Weekly |
| #31 | Python-2-Week-4-Week-Mastery-Roadmaps | This file | This file |
| #32 | Python-Capstone-Production-FastAPI-Service-Lab | Stretch | Week 4 |

---

## 7. Final Revision Checklist

- [ ] Chose the right plan (2-week or 4-week) and confirmed daily time commitment
- [ ] Completed Prerequisites Checklist before Day 1
- [ ] Reached Week 1 milestone on or before scheduled date
- [ ] Reached Week 2 milestone on or before scheduled date
- [ ] All 20 labs run at least once
- [ ] All 8 mock rounds completed at least once timed
- [ ] Gap-fill sheets completed: setup, modern Python, security, observability, workers, pattern matching, time/money
- [ ] Capstone lab either built or explained end to end from architecture, tests, security, and observability
- [ ] Weekly Self-Assessment filled in at least twice
- [ ] Identified and re-ran lowest 2 dimensions at least once
- [ ] Phone screen readiness gate cleared (6/6 at ≥ 3)
- [ ] On-site readiness gate cleared (8/10 at ≥ 4) — if targeting on-site
- [ ] Interview Week Protocol scheduled in calendar
