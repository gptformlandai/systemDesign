# Python Runnable Mini-Labs — Gold Sheet

> **Track File #28 of 31 · Group 6: Practice Upgrade**
> For: Java developer | Level: hands-on exposure | Mode: copy → run → observe → modify

---

## 1. How to Use This Sheet

Each lab is a complete, self-contained Python script.

**Workflow per lab:**
1. Read the **Goal** — predict what will happen
2. Copy the script into a `.py` file and run it
3. Compare actual output with **Expected Output**
4. Complete the **Challenge** — modify the script and predict the new output
5. If stuck, re-read the linked Gold Sheet section

**Setup (one-time):**
```bash
python -m venv .venv
source .venv/bin/activate    # macOS / Linux
# .venv\Scripts\activate   # Windows
pip install httpx pytest pytest-asyncio
```

---

## 2. Lab 01 — Mutable Default Argument Trap

**Goal:** Observe that a list default accumulates across calls.

```python
# lab_01_mutable_default.py

def add_to_history(event, history=[]):
    """BUG: shared mutable default."""
    history.append(event)
    return history

def add_fixed(event, history=None):
    """FIX: sentinel + per-call creation."""
    if history is None:
        history = []
    history.append(event)
    return history

print("=== BUG ===")
print(add_to_history("login"))       # ['login']
print(add_to_history("purchase"))    # ['login', 'purchase']  ← accumulates!
print(add_to_history("logout"))      # ['login', 'purchase', 'logout']

print("\n=== FIX ===")
print(add_fixed("login"))            # ['login']
print(add_fixed("purchase"))         # ['purchase']  ← independent!
print(add_fixed("logout"))           # ['logout']

# Inspect the default object directly
print("\n=== PROOF ===")
print(f"__defaults__: {add_to_history.__defaults__}")
# __defaults__: (['login', 'purchase', 'logout'],)  ← same list object persists
```

**Expected Output:**
```
=== BUG ===
['login']
['login', 'purchase']
['login', 'purchase', 'logout']

=== FIX ===
['login']
['purchase']
['logout']

=== PROOF ===
__defaults__: (['login', 'purchase', 'logout'],)
```

**Challenge:** Call `add_to_history("reset", [])` and then `add_to_history("next")`. What does the second call return? Why?

> Linked sheet: Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md §4

---

## 3. Lab 02 — Late Binding Closure

**Goal:** See that loop lambdas all return the same value; fix with default arg.

```python
# lab_02_late_binding.py

print("=== LATE BINDING BUG ===")
funcs_bad = [lambda: i for i in range(5)]
print([f() for f in funcs_bad])
# All return 4 — the final value of i

print("\n=== FIX: Default Argument Capture ===")
funcs_fixed = [lambda i=i: i for i in range(5)]
print([f() for f in funcs_fixed])
# [0, 1, 2, 3, 4]  — each captures its own value

print("\n=== NONLOCAL COUNTER ===")
def make_counter(start=0):
    count = start
    def increment(by=1):
        nonlocal count
        count += by
        return count
    return increment

c = make_counter(10)
print(c())     # 11
print(c())     # 12
print(c(5))    # 17

# Two independent counters
a = make_counter()
b = make_counter(100)
a()
a()
b()
print(f"a={a()}, b={b()}")   # a=3, b=102
```

**Expected Output:**
```
=== LATE BINDING BUG ===
[4, 4, 4, 4, 4]

=== FIX: Default Argument Capture ===
[0, 1, 2, 3, 4]

=== NONLOCAL COUNTER ===
11
12
17
a=3, b=102
```

**Challenge:** Replace `lambda i=i: i` with `functools.partial(lambda i: i, i)` and verify it gives the same result.

> Linked sheet: Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md §7

---

## 4. Lab 03 — Shallow vs Deep Copy

**Goal:** Observe the difference between alias, shallow copy, and deep copy on nested data.

```python
# lab_03_copy.py
import copy

original = [{"name": "Alice", "scores": [90, 85]}, {"name": "Bob", "scores": [70, 80]}]

alias = original
shallow = original[:]      # or copy.copy(original)
deep = copy.deepcopy(original)

# Mutate original's nested list
original[0]["scores"].append(100)

print("After mutating original[0]['scores']:")
print(f"  alias:   {alias[0]['scores']}")    # shows 100 — same object
print(f"  shallow: {shallow[0]['scores']}")  # shows 100 — inner dict shared!
print(f"  deep:    {deep[0]['scores']}")     # unchanged — fully independent

# Append a new element to the outer list
original.append({"name": "Carol", "scores": [95]})

print("\nAfter appending to original:")
print(f"  alias length:   {len(alias)}")    # 3 — same object
print(f"  shallow length: {len(shallow)}")  # 2 — outer list is independent
print(f"  deep length:    {len(deep)}")     # 2 — fully independent
```

**Expected Output:**
```
After mutating original[0]['scores']:
  alias:   [90, 85, 100]
  shallow: [90, 85, 100]
  deep:    [90, 85]

After appending to original:
  alias length:   3
  shallow length: 2
  deep length:    2
```

**Challenge:** What happens if you do `shallow[0] = {"name": "NEW", "scores": []}` — does `original[0]` change? Why or why not?

> Linked sheet: Python-Dict-List-Mutability-Request-Scenario-Gold-Sheet.md §6

---

## 5. Lab 04 — Generator Memory Efficiency

**Goal:** Prove generators use O(1) memory vs lists using `sys.getsizeof`.

```python
# lab_04_generator.py
import sys

# List — materializes everything
list_comp = [x ** 2 for x in range(100_000)]
gen_expr = (x ** 2 for x in range(100_000))

print(f"List size:      {sys.getsizeof(list_comp):>10,} bytes")
print(f"Generator size: {sys.getsizeof(gen_expr):>10,} bytes")

# Generator pipeline: read → filter → transform  (all lazy)
def read_numbers(n):
    for i in range(n):
        yield i

def only_even(nums):
    for n in nums:
        if n % 2 == 0:
            yield n

def squared(nums):
    for n in nums:
        yield n ** 2

pipeline = squared(only_even(read_numbers(1_000_000)))

# Count items consumed (pull-based — only runs when we ask)
count = sum(1 for _ in pipeline)
print(f"\nEven squares from range(1M): {count}")  # 500,000

# Generator exhaustion demo
gen = (x for x in range(5))
print("\nFirst pass:", list(gen))   # [0, 1, 2, 3, 4]
print("Second pass:", list(gen))   # []  ← exhausted!
```

**Expected Output:**
```
List size:         824,456 bytes
Generator size:         208 bytes

Even squares from range(1M): 500000

First pass: [0, 1, 2, 3, 4]
Second pass: []
```

**Challenge:** Add a `print("yielding", i)` inside `read_numbers`. Run and observe — generators are truly lazy.

> Linked sheet: Python-Collections-Comprehensions-Iteration-Gold-Sheet.md §9

---

## 6. Lab 05 — Class vs Instance Variable

**Goal:** See the difference between `list.append` and `+=` on class-level attributes.

```python
# lab_05_class_vs_instance.py

class Bag:
    items = []   # class variable — shared
    count = 0    # class variable — integer

    def add_item(self, item):
        self.items.append(item)   # mutates class list in-place (no new instance attr)

    def increment(self):
        self.count += 1            # creates instance attribute count

b1 = Bag()
b2 = Bag()

b1.add_item("apple")
b1.add_item("banana")
b1.increment()
b1.increment()
b1.increment()

print("After b1 mutations:")
print(f"  b1.items:   {b1.items}")   # ['apple', 'banana']
print(f"  b2.items:   {b2.items}")   # ['apple', 'banana'] — SHARED!
print(f"  Bag.items:  {Bag.items}")  # ['apple', 'banana'] — class var mutated
print(f"  b1.count:   {b1.count}")   # 3 — instance attribute
print(f"  b2.count:   {b2.count}")   # 0 — reads class var (unaffected)
print(f"  Bag.count:  {Bag.count}")  # 0 — class var unchanged

# Proof: b1.__dict__ has count, not items
print(f"\n  b1.__dict__: {b1.__dict__}")
print(f"  b2.__dict__: {b2.__dict__}")
```

**Expected Output:**
```
After b1 mutations:
  b1.items:   ['apple', 'banana']
  b2.items:   ['apple', 'banana']
  Bag.items:  ['apple', 'banana']
  b1.count:   3
  b2.count:   0
  Bag.count:  0

  b1.__dict__: {'count': 3}
  b2.__dict__: {}
```

**Challenge:** Fix `Bag` so each instance gets its own `items` list. Verify `b2.items` is empty after `b1.add_item`.

> Linked sheet: Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md §5

---

## 7. Lab 06 — GIL: Threading vs Multiprocessing

**Goal:** Show that threading doesn't speed up CPU-bound work; multiprocessing does.

```python
# lab_06_gil.py
import threading
import multiprocessing
import time

def cpu_work(n):
    """Pure CPU work — affected by GIL in threads."""
    total = 0
    for i in range(n):
        total += i * i
    return total

N = 5_000_000
WORKERS = 4

# Baseline — single process, single thread
t0 = time.perf_counter()
cpu_work(N * WORKERS)
baseline = time.perf_counter() - t0
print(f"Single thread:      {baseline:.3f}s")

# Threading — GIL prevents true parallelism for CPU work
t0 = time.perf_counter()
threads = [threading.Thread(target=cpu_work, args=(N,)) for _ in range(WORKERS)]
for t in threads: t.start()
for t in threads: t.join()
thread_time = time.perf_counter() - t0
print(f"Threading ({WORKERS} threads): {thread_time:.3f}s  (ratio: {thread_time/baseline:.2f}x)")

# Multiprocessing — each process has its own GIL
t0 = time.perf_counter()
with multiprocessing.Pool(processes=WORKERS) as pool:
    pool.map(cpu_work, [N] * WORKERS)
mp_time = time.perf_counter() - t0
print(f"Multiprocessing ({WORKERS} procs): {mp_time:.3f}s  (ratio: {mp_time/baseline:.2f}x)")

print("\nConclusion:")
print(f"  Threading speedup: {baseline/thread_time:.2f}x (close to 1.0 — GIL blocks)")
print(f"  MP speedup:        {baseline/mp_time:.2f}x (should be > 1.0 on multi-core)")
```

**Expected Output (approximate — hardware dependent):**
```
Single thread:        4.120s
Threading (4 threads): 4.350s  (ratio: 1.06x)
Multiprocessing (4 procs): 1.280s  (ratio: 0.31x)

Conclusion:
  Threading speedup: 0.95x (close to 1.0 — GIL blocks)
  MP speedup:        3.22x (should be > 1.0 on multi-core)
```

**Challenge:** Replace `cpu_work` with an I/O-bound task using `time.sleep(0.1)`. Does threading now outperform single-thread? Why?

> Linked sheet: Python-Concurrency-Threading-Multiprocessing-MAANG-Master-Sheet.md §3

---

## 8. Lab 07 — Async: Sequential vs Concurrent

**Goal:** Measure the wall-clock difference between sequential `await` vs `asyncio.gather`.

```python
# lab_07_async_gather.py
import asyncio
import time

async def fake_io(task_id: int, delay: float) -> str:
    """Simulates an I/O-bound async call (e.g., HTTP, DB)."""
    await asyncio.sleep(delay)
    return f"task-{task_id} done"

async def sequential():
    results = []
    for i in range(5):
        result = await fake_io(i, 0.2)   # waits each before starting next
        results.append(result)
    return results

async def concurrent():
    tasks = [fake_io(i, 0.2) for i in range(5)]
    return await asyncio.gather(*tasks)   # all start immediately

async def main():
    print("=== Sequential (total ≈ 5 × 0.2s = 1.0s) ===")
    t0 = time.perf_counter()
    r = await sequential()
    elapsed = time.perf_counter() - t0
    print(f"Results: {r}")
    print(f"Time: {elapsed:.3f}s\n")

    print("=== Concurrent gather (total ≈ 0.2s) ===")
    t0 = time.perf_counter()
    r = await concurrent()
    elapsed = time.perf_counter() - t0
    print(f"Results: {r}")
    print(f"Time: {elapsed:.3f}s")

asyncio.run(main())
```

**Expected Output:**
```
=== Sequential (total ≈ 5 × 0.2s = 1.0s) ===
Results: ['task-0 done', 'task-1 done', 'task-2 done', 'task-3 done', 'task-4 done']
Time: 1.002s

=== Concurrent gather (total ≈ 0.2s) ===
Results: ['task-0 done', 'task-1 done', 'task-2 done', 'task-3 done', 'task-4 done']
Time: 0.201s
```

**Challenge:** Add `time.sleep(0.2)` (not `await`) inside `fake_io`. Run and observe that gather no longer speeds things up. Why?

> Linked sheet: Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md §4

---

## 9. Lab 08 — Decorator Factory

**Goal:** Build a `retry` decorator factory and observe it in action.

```python
# lab_08_decorator_factory.py
import functools
import random

def retry(max_attempts=3, exceptions=(Exception,)):
    """Decorator factory: retry on specified exceptions."""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            last_err = None
            for attempt in range(1, max_attempts + 1):
                try:
                    return func(*args, **kwargs)
                except exceptions as e:
                    last_err = e
                    print(f"  Attempt {attempt}/{max_attempts} failed: {e}")
            raise last_err
        return wrapper
    return decorator

# Control randomness for reproducible demo
random.seed(42)

@retry(max_attempts=4, exceptions=(ValueError,))
def flaky_api_call(n):
    """Fails 70% of the time with ValueError."""
    if random.random() < 0.7:
        raise ValueError("API timeout")
    return f"Success with n={n}"

print("Calling flaky_api_call(42):")
try:
    result = flaky_api_call(42)
    print(f"  Result: {result}")
except ValueError as e:
    print(f"  All retries exhausted: {e}")

# Verify functools.wraps preserved metadata
print(f"\nFunction name: {flaky_api_call.__name__}")   # should be 'flaky_api_call'
print(f"Wrapped:       {flaky_api_call.__wrapped__}")  # original function
```

**Expected Output:**
```
Calling flaky_api_call(42):
  Attempt 1/4 failed: API timeout
  Attempt 2/4 failed: API timeout
  Attempt 3/4 failed: API timeout
  Result: Success with n=42

Function name: flaky_api_call
Wrapped:       <function flaky_api_call at 0x...>
```

**Challenge:** Remove `@functools.wraps(func)` and check `flaky_api_call.__name__` again. What changes?

> Linked sheet: Python-Decorators-Descriptors-Metaclasses-Deep-Dive-Gold-Sheet.md §2

---

## 10. Lab 09 — `@property` and Descriptor

**Goal:** Observe `@property` enforcing validation; compare descriptor lookup priority.

```python
# lab_09_property_descriptor.py

class Temperature:
    def __init__(self, celsius: float):
        self.celsius = celsius   # calls setter on __init__

    @property
    def celsius(self) -> float:
        return self._celsius

    @celsius.setter
    def celsius(self, value: float):
        if value < -273.15:
            raise ValueError(f"Temperature below absolute zero: {value}")
        self._celsius = value

    @property
    def fahrenheit(self) -> float:
        return self._celsius * 9/5 + 32

t = Temperature(100)
print(f"Boiling: {t.celsius}°C = {t.fahrenheit}°F")

t.celsius = 0
print(f"Freezing: {t.celsius}°C = {t.fahrenheit}°F")

try:
    t.celsius = -300
except ValueError as e:
    print(f"Error: {e}")

# Descriptor priority: data descriptor wins over instance __dict__
print(f"\nt.__dict__: {t.__dict__}")
# _celsius is stored, not celsius — property intercepted the write
```

**Expected Output:**
```
Boiling: 100°C = 212.0°F
Freezing: 0°C = 32.0°F
Error: Temperature below absolute zero: -300

t.__dict__: {'_celsius': 0}
```

**Challenge:** Add a `kelvin` computed property. Test `Temperature(-273.15).kelvin == 0.0`.

> Linked sheet: Python-Decorators-Descriptors-Metaclasses-Deep-Dive-Gold-Sheet.md §5–6

---

## 11. Lab 10 — CPU Profiling with cProfile

**Goal:** Find which function is the bottleneck using `cProfile`.

```python
# lab_10_profiling.py
import cProfile
import pstats
import io

def slow_function(n):
    """Intentionally slow: sum of squares."""
    return sum(i * i for i in range(n))

def medium_function(n):
    """Moderately expensive: list operations."""
    return sorted([i % 17 for i in range(n)])

def fast_function(n):
    """Cheap: simple arithmetic."""
    return n * (n + 1) // 2

def main():
    for _ in range(100):
        slow_function(50_000)
    for _ in range(1000):
        medium_function(5_000)
    for _ in range(100_000):
        fast_function(100)

pr = cProfile.Profile()
pr.enable()
main()
pr.disable()

# Print top 10 functions by cumulative time
stream = io.StringIO()
ps = pstats.Stats(pr, stream=stream).sort_stats("cumulative")
ps.print_stats(10)
print(stream.getvalue())
```

**Expected Output (approximate):**
```
   ncalls  tottime  percall  cumtime  percall filename:lineno(function)
        1    0.002    0.002    3.450    3.450 lab_10_profiling.py:18(main)
      100    2.980    0.030    3.420    0.034 lab_10_profiling.py:5(slow_function)
     1000    0.420    0.000    0.420    0.000 lab_10_profiling.py:10(medium_function)
   100000    0.048    0.000    0.048    0.000 lab_10_profiling.py:14(fast_function)
```

**Challenge:** Replace `sum(i*i for i in range(n))` with `sum(range(n))` in `slow_function`. Does the cumtime change? Why?

> Linked sheet: Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md §3

---

## 12. Lab 11 — `tracemalloc` Memory Snapshot

**Goal:** Find which line of code allocates the most memory.

```python
# lab_11_tracemalloc.py
import tracemalloc

tracemalloc.start()

# Simulate a memory leak — unbounded cache
cache = {}
for i in range(10_000):
    cache[f"user:{i}"] = {"id": i, "data": "x" * 1000}  # 1KB per entry

# Take snapshot
snapshot = tracemalloc.take_snapshot()
top = snapshot.statistics("lineno")

print("Top 5 memory allocations:")
for stat in top[:5]:
    print(f"  {stat}")

# Check total allocated
current, peak = tracemalloc.get_traced_memory()
print(f"\nCurrent: {current / 1024 / 1024:.2f} MB")
print(f"Peak:    {peak / 1024 / 1024:.2f} MB")

tracemalloc.stop()
```

**Expected Output:**
```
Top 5 memory allocations:
  lab_11_tracemalloc.py:9: size=10.2 MiB, count=10000, average=1.0 KiB
  ...

Current: 10.26 MB
Peak:    10.30 MB
```

**Challenge:** Add `cache.clear()` before taking the snapshot. What does the memory report show?

> Linked sheet: Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md §5

---

## 13. Lab 12 — LRU Cache Performance

**Goal:** Measure cache miss vs hit speed with `functools.lru_cache`.

```python
# lab_12_lru_cache.py
import functools
import time

def fib_naive(n):
    """Exponential time — O(2^n)."""
    if n <= 1:
        return n
    return fib_naive(n - 1) + fib_naive(n - 2)

@functools.lru_cache(maxsize=None)
def fib_cached(n):
    """Linear time with memoization — O(n)."""
    if n <= 1:
        return n
    return fib_cached(n - 1) + fib_cached(n - 2)

# Compare
N = 35

t0 = time.perf_counter()
result_naive = fib_naive(N)
naive_time = time.perf_counter() - t0
print(f"fib_naive({N})  = {result_naive}  [{naive_time:.4f}s]")

t0 = time.perf_counter()
result_cached = fib_cached(N)
cached_time = time.perf_counter() - t0
print(f"fib_cached({N}) = {result_cached} [{cached_time:.6f}s]")

print(f"\nSpeedup: {naive_time / cached_time:.0f}x")
print(f"Cache info: {fib_cached.cache_info()}")

# Second call — all hits
t0 = time.perf_counter()
fib_cached(N)
hit_time = time.perf_counter() - t0
print(f"Second call (all hits): {hit_time:.8f}s")
```

**Expected Output:**
```
fib_naive(35)  = 9227465  [1.8231s]
fib_cached(35) = 9227465  [0.000041s]

Speedup: 44,466x
Cache info: CacheInfo(hits=33, misses=36, maxsize=None, currsize=36)
Second call (all hits): 0.00000120s
```

**Challenge:** Set `maxsize=5`. What happens to performance for `fib_cached(35)`? Why?

> Linked sheet: Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md §7

---

## 14. Lab 13 — ContextVar Request Isolation

**Goal:** Prove `ContextVar` isolates state between asyncio tasks.

```python
# lab_13_contextvar.py
import asyncio
from contextvars import ContextVar

request_id: ContextVar[str] = ContextVar("request_id", default="none")

async def handle_request(rid: str):
    token = request_id.set(rid)
    try:
        print(f"[Task {rid}] start: request_id = {request_id.get()}")
        await asyncio.sleep(0.1)   # yield to event loop
        print(f"[Task {rid}] after sleep: request_id = {request_id.get()}")
    finally:
        request_id.reset(token)

async def main():
    # Launch 3 concurrent tasks — each gets its own context copy
    await asyncio.gather(
        handle_request("req-AAA"),
        handle_request("req-BBB"),
        handle_request("req-CCC"),
    )
    print(f"\nAfter all tasks: request_id = {request_id.get()}")

asyncio.run(main())
```

**Expected Output:**
```
[Task req-AAA] start: request_id = req-AAA
[Task req-BBB] start: request_id = req-BBB
[Task req-CCC] start: request_id = req-CCC
[Task req-AAA] after sleep: request_id = req-AAA
[Task req-BBB] after sleep: request_id = req-BBB
[Task req-CCC] after sleep: request_id = req-CCC

After all tasks: request_id = none
```

**Key observation:** Even though all tasks share the same event loop thread, each task's `ContextVar` is isolated. The `after sleep` values are still correct despite interleaving.

**Challenge:** Replace `ContextVar` with a plain module-level `dict` called `_ctx = {}`. Use `_ctx["request_id"]` and observe the corruption.

> Linked sheet: Python-Dict-List-Mutability-Request-Scenario-Gold-Sheet.md §7

---

## 15. Lab 14 — itertools Groupby Pre-Sort Trap

**Goal:** See why `itertools.groupby` requires pre-sorted data.

```python
# lab_14_groupby.py
from itertools import groupby
from collections import defaultdict

data = [
    {"dept": "eng", "name": "Alice"},
    {"dept": "hr", "name": "Bob"},
    {"dept": "eng", "name": "Carol"},   # eng appears again!
    {"dept": "hr", "name": "Dave"},
]

print("=== groupby WITHOUT sort (BUG) ===")
for dept, members in groupby(data, key=lambda x: x["dept"]):
    names = [m["name"] for m in members]
    print(f"  {dept}: {names}")

print("\n=== groupby WITH sort (FIX) ===")
for dept, members in groupby(sorted(data, key=lambda x: x["dept"]), key=lambda x: x["dept"]):
    names = [m["name"] for m in members]
    print(f"  {dept}: {names}")

print("\n=== defaultdict groupby (no sort needed) ===")
groups = defaultdict(list)
for item in data:
    groups[item["dept"]].append(item["name"])
for dept, names in sorted(groups.items()):
    print(f"  {dept}: {names}")
```

**Expected Output:**
```
=== groupby WITHOUT sort (BUG) ===
  eng: ['Alice']
  hr: ['Bob']
  eng: ['Carol']
  hr: ['Dave']

=== groupby WITH sort (FIX) ===
  eng: ['Alice', 'Carol']
  hr: ['Bob', 'Dave']

=== defaultdict groupby (no sort needed) ===
  eng: ['Alice', 'Carol']
  hr: ['Bob', 'Dave']
```

**Challenge:** For a 1M-record dataset, which approach is more memory-efficient — `groupby` after sort or `defaultdict`? Why?

> Linked sheet: Python-Data-Processing-Interview-Scenarios-Gold-Sheet.md §5

---

## 16. Lab 15 — Exception Handling: `else`, `finally`, Chaining

**Goal:** Trace the exact execution path of `try/except/else/finally`.

```python
# lab_15_exceptions.py

def demo(raise_error: bool, return_in_try: bool = False):
    print(f"\n--- raise={raise_error}, return_in_try={return_in_try} ---")
    try:
        print("  try: executing")
        if raise_error:
            raise ValueError("oops")
        if return_in_try:
            print("  try: returning 'early'")
            return "early"
        print("  try: completed normally")
    except ValueError as e:
        print(f"  except: caught {e}")
    else:
        print("  else: runs only when no exception")
    finally:
        print("  finally: ALWAYS runs")
    return "normal"

result1 = demo(raise_error=False)
result2 = demo(raise_error=True)
result3 = demo(raise_error=False, return_in_try=True)

print(f"\nResults: {result1!r}, {result2!r}, {result3!r}")

# Exception chaining
print("\n--- Exception Chaining ---")
try:
    try:
        int("bad")
    except ValueError as e:
        raise RuntimeError("Conversion failed") from e
except RuntimeError as e:
    print(f"Caught: {e}")
    print(f"Cause: {e.__cause__}")
```

**Expected Output:**
```
--- raise=False, return_in_try=False ---
  try: executing
  try: completed normally
  else: runs only when no exception
  finally: ALWAYS runs

--- raise=True, return_in_try=False ---
  try: executing
  except: caught oops
  finally: ALWAYS runs

--- raise=False, return_in_try=True ---
  try: executing
  try: returning 'early'
  finally: ALWAYS runs

Results: 'normal', 'normal', 'early'

--- Exception Chaining ---
Caught: Conversion failed
Cause: invalid literal for int() with base 10: 'bad'
```

**Challenge:** Add `return "from_finally"` inside the `finally` block of `demo`. What do `result1`, `result2`, `result3` become?

> Linked sheet: Python-Exception-Handling-Context-Managers-Gold-Sheet.md §4

---

## 17. Lab 16 — Dataclass vs Plain Class

**Goal:** Compare boilerplate, equality, and `field(default_factory=...)`.

```python
# lab_16_dataclass.py
from dataclasses import dataclass, field
from typing import List

# Plain class
class PlainPoint:
    def __init__(self, x: float, y: float):
        self.x = x
        self.y = y
    def __repr__(self):
        return f"PlainPoint(x={self.x}, y={self.y})"
    def __eq__(self, other):
        return isinstance(other, PlainPoint) and self.x == other.x and self.y == other.y

# Dataclass — generates __init__, __repr__, __eq__ automatically
@dataclass
class Point:
    x: float
    y: float

@dataclass
class Polygon:
    name: str
    vertices: List[Point] = field(default_factory=list)  # correct mutable default

# Equality comparison
p1 = Point(1.0, 2.0)
p2 = Point(1.0, 2.0)
p3 = Point(3.0, 4.0)

print(f"p1 == p2: {p1 == p2}")   # True — dataclass __eq__ compares fields
print(f"p1 is p2: {p1 is p2}")   # False — different objects
print(f"p1 == p3: {p1 == p3}")   # False

print(f"\nRepr: {p1}")

# Mutable default works correctly
poly1 = Polygon("triangle")
poly2 = Polygon("square")
poly1.vertices.append(Point(0, 0))
print(f"\npoly1.vertices: {poly1.vertices}")
print(f"poly2.vertices: {poly2.vertices}")   # empty — not shared!

# Frozen dataclass — immutable and hashable
@dataclass(frozen=True)
class ImmutablePoint:
    x: float
    y: float

ip = ImmutablePoint(1.0, 2.0)
d = {ip: "origin area"}   # hashable — can be dict key
print(f"\nFrozen point as dict key: {d[ip]}")

try:
    ip.x = 9.0
except Exception as e:
    print(f"Cannot mutate frozen: {type(e).__name__}")
```

**Expected Output:**
```
p1 == p2: True
p1 is p2: False
p1 == p3: False

Repr: Point(x=1.0, y=2.0)

poly1.vertices: [Point(x=0.0, y=0.0)]
poly2.vertices: []

Frozen point as dict key: origin area
Cannot mutate frozen: FrozenInstanceError
```

**Challenge:** Try `@dataclass` without `field(default_factory=list)` — use `vertices: List[Point] = []` directly. What error do you get?

> Linked sheet: Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md §8

---

## 18. Final Revision Checklist

- [ ] Lab 01 — Can predict mutable default accumulation; explained `__defaults__`
- [ ] Lab 02 — Can predict late binding output; fixed with default arg and `nonlocal`
- [ ] Lab 03 — Can predict shallow vs deep copy behavior on nested lists
- [ ] Lab 04 — Can explain O(1) generator memory; observed exhaustion
- [ ] Lab 05 — Can predict `append` vs `+=` on class attribute
- [ ] Lab 06 — Observed GIL blocking threads for CPU; multiprocessing bypasses it
- [ ] Lab 07 — Measured sequential vs gather speedup; understand why
- [ ] Lab 08 — Can build 3-layer decorator factory; verified `functools.wraps`
- [ ] Lab 09 — Observed property setter validation; understood `_celsius` in `__dict__`
- [ ] Lab 10 — Read `cProfile` output; identified slowest function
- [ ] Lab 11 — Used `tracemalloc` to find large allocation source
- [ ] Lab 12 — Observed `lru_cache` speedup; understood cache info stats
- [ ] Lab 13 — Observed `ContextVar` isolation across concurrent tasks
- [ ] Lab 14 — Observed `groupby` duplicate groups without sort; fixed both ways
- [ ] Lab 15 — Traced `else`/`finally` execution paths; observed exception chaining
- [ ] Lab 16 — Compared plain class vs `@dataclass`; observed `frozen=True` hashability
