# Python Mock Interview Scripts — Gold Sheet

> **Track File #29 of 31 · Group 6: Practice Upgrade**
> For: Java developer | Level: MAANG mock pressure | Mode: timed rounds with follow-up questions

---

## 1. How to Use This Sheet

**Setup:**
- Use a timer app. Hard-stop at each round's limit — interviews are timed.
- Answer out loud or write on paper/whiteboard. Do not type answers in an IDE first.
- After each round, score yourself using the rubric hints.
- Re-run any round you score below 3/5 before proceeding.

**Roles:**
- Interviewer questions are in `> INTERVIEWER:` blocks
- Strong answer guides are in `> STRONG ANSWER:` blocks
- Follow-up escalations are labelled 🔺 (increases difficulty)
- Traps are labelled ⚠️

---

## 2. Round 1 — Core Python (15 min)

**Topic coverage:** Mutability, scope, closures, data types  
**Timer:** Set 15 minutes. Answer Q1–Q4; stop when timer fires.

---

> **INTERVIEWER Q1:** "What is the output of this code? Walk me through it."
>
> ```python
> def f(x=[]):
>     x.append(1)
>     return x
>
> a = f()
> b = f()
> print(a is b)
> print(a)
> ```

> **STRONG ANSWER:** "`True` and `[1, 1]`. Default arguments in Python are evaluated once at function definition time, not at each call. The empty list `[]` is stored in `f.__defaults__`. Both `a` and `b` are the same object — both calls return and modify the same default list. So `a is b` is `True`, and `a` shows `[1, 1]`."

> ⚠️ **Weak answer:** "It prints `False` because each function call creates a new list." — This is wrong and will fail the question.

🔺 **Follow-up 1:** "How do you fix it?"

> **STRONG ANSWER:** "Use `None` as the default sentinel and initialize inside the body: `def f(x=None): if x is None: x = []; x.append(1); return x`. Each call without an explicit argument creates a fresh list."

🔺 **Follow-up 2:** "Where exactly is the default list stored?"

> **STRONG ANSWER:** "In `f.__defaults__` — a tuple that holds all default argument values. You can inspect it: `print(f.__defaults__)` shows the accumulated list."

---

> **INTERVIEWER Q2:** "What is LEGB? Give me an example where it causes an unexpected error."

> **STRONG ANSWER:** "LEGB is the name lookup order: Local → Enclosing → Global → Built-in. The classic error is `UnboundLocalError`. If a function assigns to a variable anywhere in its body, Python treats that variable as local throughout the function — even reads before the assignment. For example:
> ```python
> x = 10
> def f():
>     print(x)  # UnboundLocalError — x is local due to the assignment below
>     x = 20
> ```
> The fix is either `global x` or restructuring to avoid the read-before-assign."

🔺 **Follow-up:** "What is `nonlocal`? When would you use it instead of `global`?"

> **STRONG ANSWER:** "`nonlocal` rebinds a variable in the nearest enclosing (non-global) scope. Use `nonlocal` when implementing closures with mutable state — like a counter in a factory function. Use `global` only for module-level variables. `nonlocal` is narrower and safer."

---

> **INTERVIEWER Q3:** "What is late binding in closures? Show me the bug and two fixes."

> **STRONG ANSWER:** "Late binding means closures capture the variable reference, not its value. In a loop:
> ```python
> funcs = [lambda: i for i in range(5)]
> print(funcs[0]())  # 4, not 0
> ```
> All lambdas look up `i` at call time. After the loop, `i` is 4.
>
> Fix 1 — default argument captures current value:
> `lambda i=i: i`
>
> Fix 2 — `functools.partial`:
> `partial(lambda i: i, i)`"

🔺 **Follow-up:** "In Java, why can't you have this bug with lambdas?"

> **STRONG ANSWER:** "Java requires captured variables in lambdas to be effectively final. The compiler rejects any lambda that captures a variable that is reassigned. Python has no such restriction — the bug is silent at definition time."

---

> **INTERVIEWER Q4:** "What is the difference between `is` and `==`? When does `is` return `True` for equal integers?"

> **STRONG ANSWER:** "`==` compares values via `__eq__`. `is` compares object identity — whether two names point to the same object in memory. CPython caches small integers from -5 to 256, so `256 is 256` is `True`. Outside that range, `257 is 257` is typically `False` — two separate objects. Never use `is` for value comparison. Always use `==` for values; `is` only for identity checks like `if x is None`."

**Round 1 debrief:** Score 1 point per question you answered fully before reading the strong answer. Target ≥ 3/4.

---

## 3. Round 2 — Data Structures & Complexity (15 min)

**Topic coverage:** dict/list/set internals, complexity, Counter, sorting  
**Timer:** 15 minutes.

---

> **INTERVIEWER Q1:** "What is the time complexity of `in` for a list vs a set? Why?"

> **STRONG ANSWER:** "List `in` is O(n) — linear scan through all elements. Set `in` is O(1) average — sets are implemented as hash tables, so membership testing is a hash lookup. The tradeoff: sets require elements to be hashable, and they use more memory than a list of the same size."

🔺 **Follow-up:** "What makes a Python object hashable?"

> **STRONG ANSWER:** "An object is hashable if it defines `__hash__` and `__eq__`. The built-in immutable types — `int`, `str`, `tuple` (with hashable elements), `frozenset` — are hashable. Mutable types — `list`, `dict`, `set` — are not. User-defined classes are hashable by default (using `id()`), but if you define `__eq__` you must also define `__hash__`, or Python sets `__hash__ = None` automatically."

---

> **INTERVIEWER Q2:** "You have a list of a million log entries. Each entry has a `service_name`. You need to count how many times each service appears. Walk me through your approach."

> **STRONG ANSWER:** "I'd use `collections.Counter`:
> ```python
> from collections import Counter
> counts = Counter(entry['service_name'] for entry in logs)
> top_10 = counts.most_common(10)
> ```
> Counter is O(n) time and O(k) space where k is unique service names. `most_common(10)` uses a heap internally — O(n log 10) rather than sorting all k entries. For a million entries this is very fast."

🔺 **Follow-up:** "What if the list doesn't fit in memory?"

> **STRONG ANSWER:** "Stream from disk using a generator. Read line by line from the file, yielding dicts. Feed that generator directly into Counter. Counter itself only stores the counts dictionary — O(k) space, not O(n). For truly distributed data, I'd use a streaming approach like Kafka consumers accumulating counts per partition, then merge."

---

> **INTERVIEWER Q3:** "Sort this list by department ascending, then by salary descending, in one expression."
>
> ```python
> employees = [
>     {"name": "Alice", "dept": "eng", "salary": 120000},
>     {"name": "Bob",   "dept": "hr",  "salary": 80000},
>     {"name": "Carol", "dept": "eng", "salary": 95000},
> ]
> ```

> **STRONG ANSWER:** "`sorted(employees, key=lambda e: (e['dept'], -e['salary']))`. The key function returns a tuple — Python compares tuples element by element. Negating salary reverses its sort order while keeping dept ascending. This works for numeric fields; for strings you'd need `functools.cmp_to_key`."

🔺 **Follow-up:** "Is Python's sort stable?"

> **STRONG ANSWER:** "Yes. Python uses TimSort — a stable, adaptive merge sort. Stability means equal elements retain their original relative order. This matters when sorting by a secondary key: if two employees have the same department and salary, their original list order is preserved."

---

> **INTERVIEWER Q4:** "What is the difference between `itertools.groupby` and Java's `Collectors.groupingBy`?"

> **STRONG ANSWER:** "`itertools.groupby` is a streaming algorithm — it groups consecutive elements with the same key. If equal keys are not adjacent, you get duplicate groups. You must sort by the key first. Java's `groupingBy` scans the full stream and builds a HashMap — O(n) memory but no pre-sort needed. Choose `groupby` after sort when memory is constrained and data is large; choose `defaultdict` or in-memory grouping when the data fits in memory and you want Java-like behavior without pre-sorting."

---

## 4. Round 3 — Async & Concurrency (20 min)

**Topic coverage:** Event loop, blocking, gather, CancelledError, connection pools  
**Timer:** 20 minutes.

---

> **INTERVIEWER Q1:** "Explain why `requests.get()` inside an `async def` function is catastrophic in FastAPI."

> **STRONG ANSWER:** "FastAPI's event loop runs on a single OS thread. All `async def` route handlers share that thread. `requests.get()` is a synchronous blocking call — it occupies the OS thread for the full duration of the network round trip (e.g., 200ms). During those 200ms, the event loop cannot process any other requests. With 50 concurrent users, they all queue behind each other. Throughput drops from hundreds of requests/second to roughly `1 / latency`. The fix is `httpx.AsyncClient` — it uses `await` to yield the thread back to the event loop during the network wait, allowing other requests to proceed."

🔺 **Follow-up:** "What if the sync library can't be replaced? What do you do?"

> **STRONG ANSWER:** "Use `loop.run_in_executor(ThreadPoolExecutor, sync_function, args)`. This runs the blocking call in a worker thread and returns an awaitable. The event loop thread is freed while the thread pool executes the blocking call. For I/O-bound blocking code, `ThreadPoolExecutor` is sufficient. For CPU-bound pure Python code on default CPython, `ProcessPoolExecutor` bypasses the per-process GIL. Python 3.13+ free-threaded builds are an advanced caveat only if the deployment explicitly uses them."

---

> **INTERVIEWER Q2:** "What is the difference between `asyncio.gather` and a sequential `await` in a for loop?"

> **STRONG ANSWER:** "Sequential `await` in a loop starts each coroutine only after the previous one completes — the total time is the sum of all individual wait times. `asyncio.gather` starts all coroutines immediately and switches between them during I/O waits — the total time is approximately the longest single wait. For 10 database calls each taking 50ms, sequential takes 500ms; `gather` takes ~50ms. The analogy: sequential is one chef cooking dishes one at a time; `gather` is ten chefs cooking simultaneously."

🔺 **Follow-up:** "What does `asyncio.gather(return_exceptions=True)` do?"

> **STRONG ANSWER:** "Without `return_exceptions=True`, the first exception raised cancels all remaining tasks and re-raises. With `return_exceptions=True`, exceptions are returned as exception objects in the results list at their position — all tasks complete. Use the latter when you want partial successes, like fetching from multiple APIs where some may fail."

---

> **INTERVIEWER Q3:** "What is `CancelledError` and why must you always re-raise it?"

> **STRONG ANSWER:** "When a task is cancelled — via `task.cancel()`, `asyncio.wait_for` timeout, or `TaskGroup` exception — Python sends a `CancelledError` into the coroutine at its next `await` point. The coroutine must re-raise it after any cleanup. If you swallow it, the task's caller never receives confirmation that the task stopped — `wait_for` may deadlock waiting for it to finish. The pattern is: `except asyncio.CancelledError: await cleanup(); raise`."

---

> **INTERVIEWER Q4:** "Your API starts failing after 4 hours with connection pool errors. Describe your diagnosis."

> **STRONG ANSWER:** "I'd check three things: First, pool metrics — `pool.get_size()` vs `pool.get_idle_size()` to see if connections are being exhausted. Second, connection lifecycle — are connections being released? I'd look for `async with pool.acquire()` patterns and ensure no bare `pool.release()` calls without `try/finally`. Third, pool configuration — is `max_size` appropriate for the concurrency? Rule of thumb: `max_size` ≈ expected concurrent DB calls. I'd also add a `Semaphore` upstream to cap concurrent DB access so requests fail fast rather than queuing indefinitely."

---

## 5. Round 4 — Python Internals (15 min)

**Topic coverage:** Decorators, descriptors, metaclasses, GC  
**Timer:** 15 minutes.

---

> **INTERVIEWER Q1:** "Explain what `functools.wraps` does and why it matters."

> **STRONG ANSWER:** "When you write a decorator that wraps a function, the wrapper replaces the original. Without `functools.wraps`, `wrapped.__name__` returns the wrapper's name, `wrapped.__doc__` returns the wrapper's docstring, and `wrapped.__annotations__` is wrong. `functools.wraps(original_func)` copies `__name__`, `__qualname__`, `__doc__`, `__dict__`, `__module__`, `__annotations__`, and sets `__wrapped__` to the original. This matters for logging (correct function names), pytest (correct test names), `help()`, `inspect.signature()`, and `functools.singledispatch`."

🔺 **Follow-up:** "How do you write a decorator that accepts arguments — like `@retry(max_attempts=3)`?"

> **STRONG ANSWER:** "Three layers. The outermost callable accepts the decorator arguments and returns the actual decorator. The middle layer is the decorator — it accepts the function and returns the wrapper. The innermost is the wrapper — it accepts the original call arguments. Called as `@retry(3)`: `retry(3)` returns `decorator`; `@decorator` wraps the function; calling the function invokes `wrapper`."

---

> **INTERVIEWER Q2:** "What is the descriptor protocol? How does `@property` use it?"

> **STRONG ANSWER:** "Any object that defines `__get__`, `__set__`, or `__delete__` is a descriptor. When Python performs attribute lookup on an instance, it checks the class MRO for descriptors. If a data descriptor is found (has `__set__`), it takes priority over the instance `__dict__`. `@property` is a built-in data descriptor — it stores `fget`, `fset`, `fdel` functions and implements `__get__` (calls fget), `__set__` (calls fset), `__delete__` (calls fdel). Being a data descriptor means `self.x = value` calls the setter, not `instance.__dict__['x'] = value`."

---

> **INTERVIEWER Q3:** "How does Python's garbage collector handle reference cycles? How do you prevent a cycle from being a memory leak?"

> **STRONG ANSWER:** "CPython uses reference counting as the primary mechanism — objects are freed when their reference count hits zero. Reference cycles (A → B → A) never reach zero by reference counting alone. Python's cyclic GC periodically scans for cycle-participating objects and collects them. However, objects with `__del__` methods in cycles were not collected before Python 3.4. The prevention: use `weakref.ref()` to hold one side of a mutual reference. A `weakref` doesn't increment the reference count, so it doesn't prevent GC. Accessing a dead weakref returns `None`."

---

## 6. Round 5 — Backend System Design & Testing (20 min)

**Topic coverage:** FastAPI patterns, testing, architecture  
**Timer:** 20 minutes.

---

> **INTERVIEWER Q1:** "You have a FastAPI endpoint that calls a database. How do you write a unit test that doesn't hit the real database?"

> **STRONG ANSWER:** "Use FastAPI's `app.dependency_overrides`. Define the DB dependency:
> ```python
> async def get_db() -> AsyncSession:
>     async with AsyncSession(engine) as session:
>         yield session
> ```
> In the test, replace it:
> ```python
> app.dependency_overrides[get_db] = lambda: fake_session
> with TestClient(app) as client:
>     resp = client.get('/users/1')
> app.dependency_overrides.clear()
> ```
> Or use pytest fixtures with `httpx.AsyncClient` for async tests. The service logic can also be tested in isolation by injecting a mock repository — no HTTP layer needed at all."

---

> **INTERVIEWER Q2:** "What is the difference between `def` and `async def` route handlers in FastAPI? Which should you choose?"

> **STRONG ANSWER:** "FastAPI automatically wraps `def` (sync) route handlers in a thread pool — so blocking code in a `def` route doesn't block the event loop. `async def` routes run directly on the event loop thread. Rule: if all I/O in your handler is async — `await`-based DB calls, `httpx.AsyncClient` — use `async def`. If you're using a synchronous library that you can't easily replace — like a sync ORM or SDK — use `def`, letting FastAPI offload to the thread pool. Never use `async def` with blocking code — that's the worst combination: you block the only event loop thread with no thread-pool safety net."

---

> **INTERVIEWER Q3:** "Walk me through the N+1 query problem. How do you detect it and fix it?"

> **STRONG ANSWER:** "N+1 occurs when you fetch N records then execute one additional query per record — N+1 total queries. Classic ORM example: fetch 100 orders, then lazy-load the user for each order. Detection: enable SQL query logging (`echo=True` in SQLAlchemy), or use a query counter middleware. Fix 1: JOIN — fetch orders and users in a single query. Fix 2: batch fetch — get all order user IDs, fetch all users in one `WHERE id = ANY($1)`, build a dict, join in Python. Fix 3: ORM eager loading — SQLAlchemy's `selectinload` or `joinedload`."

---

> **INTERVIEWER Q4:** "What is `asyncio.Semaphore` and when would you use it in a FastAPI service?"

> **STRONG ANSWER:** "`asyncio.Semaphore(n)` maintains a counter. `async with semaphore` decrements the counter; if zero, the coroutine suspends until another releases. Use cases: (1) Rate limiting calls to a third-party API that allows only 10 concurrent requests — `Semaphore(10)` before the HTTP call. (2) Capping concurrent DB operations before pool exhaustion — `Semaphore(15)` acts as a soft cap above the pool. (3) Bulkhead pattern — separate semaphores for different downstream services so one slow service doesn't starve all others."

---

## 7. Round 6 — Senior Production Scenarios (20 min)

**Topic coverage:** Debugging, architecture decisions, trade-offs  
**Timer:** 20 minutes.

---

> **INTERVIEWER Q1:** "Memory is growing from 200MB to 2GB over 8 hours in your FastAPI service. Walk me through your investigation."

> **STRONG ANSWER:** "Step 1 — Baseline: start `tracemalloc` at startup, expose a debug endpoint that takes a snapshot. Step 2 — Compare: take snapshot at 200MB and at 1GB; diff the statistics by lineno to find what's growing. Step 3 — Identify type: `objgraph.show_most_common_types()` to see which object types are accumulating. Step 4 — Trace root: `objgraph.show_backrefs()` on a leaked object to find what's holding the reference. Common culprits: unbounded module-level cache, reference cycles with large objects, event handler registrations that keep closures alive, accumulated asyncio tasks that were never awaited. Fix: add `maxsize` to `lru_cache`, use `TTLCache` with eviction, break cycles with `weakref`."

---

> **INTERVIEWER Q2:** "Your service's p99 latency spikes to 8 seconds under load, but single requests complete in 50ms. What are your top 3 hypotheses?"

> **STRONG ANSWER:** "First hypothesis: event loop blocking — a synchronous call inside `async def` is starving the loop under concurrency. Tool: `py-spy` attached to the uvicorn PID; if you see the event loop thread stuck in a sync call, that's it. Fix: `httpx.AsyncClient` or `run_in_executor`. Second: connection pool exhaustion — requests are queuing for a DB connection. Tool: check `pool.get_idle_size()` under load. Fix: increase pool size or add a Semaphore as soft cap. Third: CPU-bound work in the hot path (serialization, hashing, data transformation). Tool: `cProfile` or `py-spy` flame graph. Fix: cache the result, offload to `ProcessPoolExecutor`, or optimize the algorithm."

---

> **INTERVIEWER Q3:** "You need to design a Python service that processes 10 million records daily from S3. What architecture do you propose?"

> **STRONG ANSWER:** "Generator pipeline architecture for memory efficiency. Stage 1: stream S3 object line by line using `boto3`'s streaming API or `s3fs` — never load the full file. Stage 2: parse NDJSON with `json.loads` per line in a generator. Stage 3: filter/validate in a lazy generator. Stage 4: enrich with a lookup (cache the lookup table in memory since it's small). Stage 5: batch sink — collect 500 records, bulk INSERT to DB. Run in a ProcessPoolExecutor with 4 workers for CPU-bound transform, coordinated by an asyncio event loop for I/O. This processes 10M records with O(batch_size) memory — roughly 1-5 MB regardless of total file size."

---

> **INTERVIEWER Q4:** "What is the difference between `threading.local()` and `ContextVar` in an async FastAPI service?"

> **STRONG ANSWER:** "`threading.local()` gives each OS thread its own namespace. In an async server with one event loop thread, all concurrent requests share the same thread — `threading.local()` would give them all the same data. That's a bug: one user's request_id would leak into another. `ContextVar` is designed for async contexts. Each `asyncio.Task` inherits a copy of its parent's context at creation. Setting a `ContextVar` in a middleware doesn't affect other tasks' contexts — full isolation. `ContextVar` is the correct solution for per-request state in async Python."

---

## 8. Round 7 — LLD Machine Coding (30 min)

**Topic coverage:** Design + implement under time pressure  
**Timer:** 30 minutes. Design 5 min, implement 20 min, test 5 min.

---

> **INTERVIEWER:** "Design and implement a thread-safe, in-memory rate limiter that supports multiple users. Use the sliding window algorithm. The limiter should allow `max_requests` per `window_seconds`. You have 30 minutes."

**Approach guide (work through this yourself first):**

Step 1 — Clarify (2 min):
- Thread-safe? Yes → need `threading.Lock`
- Per-user? Yes → dict keyed by user_id
- Algorithm? Sliding window log
- Interface? `is_allowed(user_id: str) -> bool`

Step 2 — Design (3 min):
```
SlidingWindowRateLimiter
  - max_requests: int
  - window_seconds: float
  - _logs: dict[str, deque[float]]
  - _lock: threading.Lock
  + is_allowed(key: str) -> bool
```

Step 3 — Implement (20 min):

```python
# Expected implementation
import threading
from collections import deque
import time

class SlidingWindowRateLimiter:
    def __init__(self, max_requests: int, window_seconds: float):
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self._logs: dict[str, deque] = {}
        self._lock = threading.Lock()

    def is_allowed(self, key: str) -> bool:
        now = time.monotonic()
        with self._lock:
            if key not in self._logs:
                self._logs[key] = deque()
            log = self._logs[key]
            cutoff = now - self.window_seconds
            while log and log[0] <= cutoff:
                log.popleft()
            if len(log) >= self.max_requests:
                return False
            log.append(now)
            return True
```

Step 4 — Test (5 min):

```python
limiter = SlidingWindowRateLimiter(max_requests=3, window_seconds=1.0)

for i in range(5):
    result = limiter.is_allowed("user-alice")
    print(f"Request {i+1}: {'ALLOWED' if result else 'BLOCKED'}")
# 1: ALLOWED, 2: ALLOWED, 3: ALLOWED, 4: BLOCKED, 5: BLOCKED

import time
time.sleep(1.1)
print(f"After 1.1s: {'ALLOWED' if limiter.is_allowed('user-alice') else 'BLOCKED'}")
# ALLOWED — window has reset
```

**Interviewer follow-ups:**

🔺 "What happens if two threads call `is_allowed` concurrently for the same user?"

> **STRONG ANSWER:** "The `threading.Lock` ensures atomicity. Both threads can't be inside the `with self._lock` block simultaneously. Without the lock, two threads could both read `len(log) == 2` (below max_requests=3), both append, resulting in 4 entries — bypassing the limit. The lock prevents this race."

🔺 "How would you make this work across multiple server instances?"

> **STRONG ANSWER:** "Replace the in-memory deque with Redis. Use a Redis sorted set per user, where the score is the timestamp. On each request: ZREMRANGEBYSCORE to evict old timestamps, ZCARD to count, ZADD to add current timestamp — all in a Lua script for atomicity. Redis MULTI/EXEC or Lua ensures the check-and-update is atomic across all server instances."

🔺 "What is the memory footprint of your implementation at 1 million users?"

> **STRONG ANSWER:** "One deque per user, with at most `max_requests` float timestamps. At `max_requests=100` and 8 bytes per float: 800 bytes per user. At 1M users: ~800MB. This is manageable but would need LRU eviction for inactive users — use `cachetools.TTLCache` to wrap `_logs`, or add a background cleanup task to remove users with empty deques."

---

## 9. Round 8 — Tricky Output (10 min)

**Timer:** 10 minutes. Answer each in < 60 seconds, no running code.

> **Q1:**
> ```python
> class A:
>     def method(self): print("A")
> class B(A):
>     def method(self): super().method(); print("B")
> class C(A):
>     def method(self): super().method(); print("C")
> class D(B, C):
>     def method(self): super().method(); print("D")
> D().method()
> ```

> **ANSWER:** `A`, `C`, `B`, `D`. MRO: D→B→C→A. `super()` follows MRO so calls chain: D→B→C→A (prints A), unwinds: C (prints C), B (prints B), D (prints D).

---

> **Q2:**
> ```python
> x = [1, 2, 3]
> y = x
> x += [4, 5]
> print(y)
>
> a = (1, 2, 3)
> b = a
> a += (4, 5)
> print(b)
> ```

> **ANSWER:** `[1, 2, 3, 4, 5]` and `(1, 2, 3)`. List `+=` is in-place (extends, same object — `y` sees it). Tuple `+=` creates a new tuple and rebinds `a` — `b` still points to the original.

---

> **Q3:**
> ```python
> try:
>     result = 10 / 2
> except ZeroDivisionError:
>     print("divided by zero")
> else:
>     print("success")
> finally:
>     print("done")
> print(result)
> ```

> **ANSWER:** `success`, `done`, `5.0`. No exception → `else` runs. `finally` always runs. `result = 5.0` is accessible.

---

> **Q4:**
> ```python
> d = {"a": 1, "b": 2, "c": 3}
> for k in list(d.keys()):
>     if d[k] > 1:
>         del d[k]
> print(d)
> ```

> **ANSWER:** `{"a": 1}`. `list(d.keys())` snapshots keys before the loop. Deleting during iteration over the snapshot is safe. Keys `"b"` and `"c"` are deleted.

---

> **Q5:**
> ```python
> def outer():
>     results = []
>     for x in range(3):
>         results.append(lambda: x)
>     return results
>
> fns = outer()
> print([f() for f in fns])
> ```

> **ANSWER:** `[2, 2, 2]`. All lambdas close over the same `x` variable in `outer`'s scope. After the loop, `x=2`. Fix: `lambda x=x: x`.

---

## 10. Post-Round Scoring Summary

| Round | Topic | Target Score | My Score |
|---:|---|---:|---|
| 1 | Core Python | ≥ 3/4 | ___ |
| 2 | Data Structures | ≥ 3/4 | ___ |
| 3 | Async & Concurrency | ≥ 3/4 | ___ |
| 4 | Python Internals | ≥ 2/3 | ___ |
| 5 | Backend & Testing | ≥ 3/4 | ___ |
| 6 | Senior Scenarios | ≥ 3/4 | ___ |
| 7 | LLD Machine Coding | ≥ implement + 1 follow-up | ___ |
| 8 | Tricky Output | ≥ 4/5 | ___ |

**Readiness gate:** Score ≥ target on 6 of 8 rounds before your interview date.

---

## 11. Final Revision Checklist

- [ ] Completed Round 1–8 with timer running (no pausing)
- [ ] Answered all questions out loud, not silently reading
- [ ] Did not look at strong answer before attempting
- [ ] Identified at least 2 rounds to repeat
- [ ] Can explain the Java Bridge difference for every strong answer
- [ ] Implemented Rate Limiter in Round 7 in under 25 min
- [ ] Scored ≥ 4/5 on Tricky Output Round cold
- [ ] Ready to re-run any round scoring < target
