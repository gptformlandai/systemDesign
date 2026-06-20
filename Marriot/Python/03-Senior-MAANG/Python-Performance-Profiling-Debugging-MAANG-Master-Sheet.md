# Python Performance, Profiling & Debugging — MAANG Master Sheet

> **Track**: Python Interview Track — Group 3: Senior MAANG  
> **File**: 3 of 4 (Track File #16)  
> **Audience**: Java developers targeting MAANG-level Python backend interviews  
> **Read after**: Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| `cProfile` — finding bottlenecks | ★★★★★ | Java has JProfiler/YourKit; Python has simpler stdlib tools; workflow is different |
| `timeit` — micro-benchmarks | ★★★★★ | Java `System.nanoTime()`; Python has a dedicated module with stat significance |
| `__slots__` — memory optimization for classes | ★★★★★ | No Java equivalent; saves 40-60% memory for large numbers of instances |
| `functools.lru_cache` / `cache` — memoization | ★★★★★ | Java Guava Cache; Python built-in decorator; common interview optimization question |
| `dis` module — bytecode inspection | ★★★☆☆ | No Java equivalent; shows why `x is None` is faster than `x == None` |
| `tracemalloc` — memory allocation tracing | ★★★★☆ | Java `-Xmx` / heap dumps; Python `tracemalloc` pinpoints allocation sites |
| `line_profiler` — line-by-line CPU time | ★★★★☆ | JProfiler method-level; `line_profiler` gives line-level resolution |
| `memory_profiler` — line-by-line memory | ★★★☆☆ | Java heap analysis tools; Python tracks memory per line |
| List comprehension vs `map()` vs loop | ★★★★☆ | Java streams vs loops; Python has specific performance characteristics |
| Generator vs list — memory tradeoff | ★★★★☆ | Java streams are lazy; Python generators similar; interview asks when to use each |
| Local variable vs global — CPython optimization | ★★★☆☆ | MAANG-level: LOAD_FAST vs LOAD_GLOBAL bytecodes; measurable difference |
| String concatenation — `join()` vs `+` | ★★★★☆ | Java `StringBuilder`; same principle applies in Python |
| `asyncio` debug mode — slow coroutine detection | ★★★☆☆ | Specific to async Python services |

---

## 2. Python Performance Mental Model

### Must Know

```
Python is interpreted (CPython) — roughly 10-100x slower than Java/C for CPU-bound work.
The performance hierarchy for Python code:

Slowest → Fastest:
  Pure Python loops (interpreted bytecode)
  → Python built-in functions (C-implemented: sum, max, sorted, map)
  → List comprehensions (C-optimized bytecode path)
  → numpy/scipy operations (C/Fortran, GIL-released)
  → Cython / ctypes / cffi (C code called from Python)
  → C extensions (numpy, pandas internals)

Key mindset for MAANG interviews:
  1. Algorithmic complexity first — O(n²) → O(n log n) beats any micro-opt
  2. Use the right data structure — dict O(1) lookup vs list O(n) scan
  3. Leverage built-ins and stdlib — they are C-implemented
  4. Profile before optimizing — "premature optimization is the root of all evil"
  5. numpy for numerical — never pure Python loops over large numeric data

Java developer trap:
  Java JIT compiles hot paths to native code — micro-optimizations matter less.
  Python has no JIT (CPython) — each bytecode instruction is interpreted.
  Small Python loops over millions of items are genuinely slow; use numpy.
```

---

## 3. `timeit` — Measuring Code Performance

### Must Know

```python
import timeit

# timeit runs code many times and returns the MINIMUM time (reduces OS noise)
# Default: 1 million repetitions (number=1_000_000)

# Method 1: timeit.timeit() with string
result = timeit.timeit(
    stmt='",".join(str(n) for n in range(100))',  # Code to time
    number=10_000,                                  # Run 10,000 times
)
print(f"Total: {result:.4f}s, per-run: {result/10_000*1e6:.2f}µs")

# Method 2: timeit.timeit() with callable (preferred — avoids string parsing)
def gen_join():
    return ",".join(str(n) for n in range(100))

def plus_join():
    result = ""
    for n in range(100):
        result += str(n) + ","
    return result

gen_time = timeit.timeit(gen_join, number=10_000)
plus_time = timeit.timeit(plus_join, number=10_000)
print(f"Generator join: {gen_time:.4f}s")
print(f"String +:       {plus_time:.4f}s")   # Usually 2-5x slower

# timeit.repeat() — run multiple trials for statistical confidence
trials = timeit.repeat(gen_join, number=10_000, repeat=5)
print(f"Min: {min(trials):.4f}s, Max: {max(trials):.4f}s")
# Use min() — OS scheduling noise inflates other trials; minimum = best-case pure execution

# Command-line usage
# python -m timeit -n 10000 '",".join(str(n) for n in range(100))'
```

### Common Benchmarks — Know These Results

```python
import timeit

# 1. List comprehension vs map() vs loop
def list_comp():     return [x * 2 for x in range(1000)]
def map_func():      return list(map(lambda x: x * 2, range(1000)))
def loop_func():
    r = []
    for x in range(1000): r.append(x * 2)
    return r

# Typical results: list_comp ≈ map_func (within 10%), loop_func ~20% slower
# (append() call overhead adds up)

# 2. dict lookup vs list scan
items_list = list(range(10_000))
items_set  = set(range(10_000))
items_dict = {i: True for i in range(10_000)}

def check_list(): return 9999 in items_list   # O(n) — scans all items
def check_set():  return 9999 in items_set    # O(1) — hash lookup
def check_dict(): return 9999 in items_dict   # O(1) — hash lookup

# set/dict lookup: microseconds; list scan of 10k items: milliseconds

# 3. String building — join() vs concatenation
def build_with_join(n: int) -> str:
    return "".join(str(i) for i in range(n))

def build_with_plus(n: int) -> str:
    s = ""
    for i in range(n):
        s += str(i)   # Creates new string object each time — O(n²) total copies
    return s

# For n=1000: join is ~5-10x faster; for n=10000: join is ~50x faster
# Java StringBuilder vs + concatenation has the same characteristic
```

---

## 4. `cProfile` — CPU Profiling

### Must Know

```python
import cProfile
import pstats
import io

def compute_heavy(n: int) -> int:
    return sum(i * i for i in range(n))

def outer(n: int) -> None:
    for _ in range(100):
        compute_heavy(n)

# Method 1: Profile to stdout
cProfile.run("outer(1000)")
# Output:
#          ncalls  tottime  percall  cumtime  percall filename:lineno(function)
#               1    0.000    0.000    0.123    0.123 <string>:1(<module>)
#               1    0.002    0.002    0.123    0.123 script.py:7(outer)
#             100    0.121    0.001    0.121    0.001 script.py:4(compute_heavy)
#           100100    0.000    ...    generator exp

# Method 2: Profile to pstats for sorting/filtering
pr = cProfile.Profile()
pr.enable()
outer(1000)
pr.disable()

stream = io.StringIO()
ps = pstats.Stats(pr, stream=stream)
ps.sort_stats(pstats.SortKey.CUMULATIVE)   # Sort by cumulative time
ps.print_stats(10)                          # Top 10 hottest functions
print(stream.getvalue())

# Method 3: Context manager pattern
with cProfile.Profile() as pr:
    outer(1000)
ps = pstats.Stats(pr)
ps.sort_stats("cumtime").print_stats(10)
```

### cProfile Column Guide

```
ncalls    — number of times the function was called
tottime   — total time IN this function (excluding sub-calls) — "self time"
percall   — tottime / ncalls — average self time per call
cumtime   — cumulative time IN this function AND all it called — "wall time"
percall   — cumtime / ncalls — average total time per call
filename  — source location

Focus on:
  HIGH tottime  → function itself is slow (inner loop, computation)
  HIGH cumtime  → function calls slow sub-functions
  HIGH ncalls with high tottime → called too often OR function is individually slow
```

### `snakeviz` — Visual Profile

```bash
# pip install snakeviz
# Generate a .prof file
python -m cProfile -o output.prof my_script.py

# Open interactive flame graph in browser
snakeviz output.prof
# Shows call tree with relative time bars — easy to spot bottlenecks visually
# Java equivalent: JProfiler / YourKit flame graph view
```

---

## 5. `line_profiler` — Line-by-Line Profiling

```python
# pip install line_profiler

# Method 1: @profile decorator (when running with kernprof)
# kernprof -l -v my_script.py
from line_profiler import profile   # or use the magic @profile when running with kernprof

@profile
def process_data(data: list[int]) -> list[int]:
    result = []                              # Line 1: trivial
    for item in data:                        # Line 2: loop
        transformed = item * 2 + 1          # Line 3: math
        if transformed % 3 == 0:            # Line 4: condition
            result.append(transformed)       # Line 5: append
    return result

# Output when run with kernprof:
# Line #  Hits   Time  Per Hit  % Time  Line Contents
#      5     1    2.0      2.0      0.1  result = []
#      6  1000  450.0      0.5     22.5  for item in data:
#      7  1000  800.0      0.8     40.0      transformed = item * 2 + 1
#      8  1000  350.0      0.4     17.5      if transformed % 3 == 0:
#      9   333  398.0      1.2     19.9          result.append(transformed)

# Identify: Line 7 takes 40% of time → optimize the math or vectorize with numpy

# Method 2: Programmatic use
from line_profiler import LineProfiler

lp = LineProfiler()
lp_wrapper = lp(process_data)
lp_wrapper(list(range(10_000)))
lp.print_stats()
```

---

## 6. `tracemalloc` — Memory Tracing

### Must Know

```python
import tracemalloc

tracemalloc.start()   # Begin recording memory allocations

# Code to profile
data = [{"id": i, "value": i * 2} for i in range(100_000)]
processed = [d["value"] for d in data]

snapshot = tracemalloc.take_snapshot()
top_stats = snapshot.statistics("lineno")   # Group by line number

print("Top 10 memory consumers:")
for stat in top_stats[:10]:
    print(stat)
# Output:
# script.py:5: size=7.6 MiB, count=100000, average=80 B
# script.py:6: size=3.1 MiB, count=100000, average=32 B

# Compare two snapshots — find what grew
snapshot1 = tracemalloc.take_snapshot()
data2 = [i * i for i in range(100_000)]
snapshot2 = tracemalloc.take_snapshot()

top_stats = snapshot2.compare_to(snapshot1, "lineno")
for stat in top_stats[:5]:
    print(f"+{stat.size_diff/1024:.1f} KiB at {stat.traceback}")

tracemalloc.stop()
```

### `memory_profiler` — Line-by-Line Memory

```python
# pip install memory_profiler

# Decorate function with @profile and run:
# python -m memory_profiler my_script.py

from memory_profiler import profile

@profile
def load_data(n: int) -> list[dict]:
    result = []                              # MiB: 0.0
    for i in range(n):                       # Loop
        result.append({"id": i, "val": i})   # Grows by ~200 bytes/iter
    return result

# Output:
# Line   Mem usage    Increment   Line Contents
#   6   45.2 MiB    45.2 MiB    def load_data(n: int):
#   7   45.2 MiB     0.0 MiB        result = []
#   8   53.7 MiB     8.5 MiB        for i in range(n):
#   9   53.7 MiB     0.0 MiB            result.append(...)
#  10   53.7 MiB     0.0 MiB        return result
# Total increment: 8.5 MiB for 10,000 dicts
```

---

## 7. `dis` Module — Bytecode Inspection

### Must Know

```python
import dis

# dis.dis() shows the CPython bytecode for a function
# Useful for understanding WHY certain patterns are faster

def version_a(x):
    return x is None   # Identity check — single IS_OP bytecode

def version_b(x):
    return x == None   # Equality check — calls __eq__; may trigger custom logic

dis.dis(version_a)
# LOAD_FAST     0 (x)
# LOAD_CONST    0 (None)
# IS_OP         0
# RETURN_VALUE

dis.dis(version_b)
# LOAD_FAST     0 (x)
# LOAD_CONST    0 (None)
# COMPARE_OP    2 (==)   ← invokes __eq__ — can be overridden!
# RETURN_VALUE

# Key insight: is None is faster than == None
# AND is None is semantically correct (None is a singleton)
# == None can return True for objects that override __eq__

# Local vs global variable speed
x = 10   # Global

def global_access():
    return x   # LOAD_GLOBAL — looks up in module's __dict__

def local_access():
    x = 10     # Local copy
    return x   # LOAD_FAST — looks up in frame's fast-locals array (C array, no hash lookup)

# dis reveals:
# LOAD_GLOBAL: dict lookup in module namespace
# LOAD_FAST: direct array index access — significantly faster in tight loops
```

### Bytecode Optimization Trick — Hoist Globals to Locals

```python
# Pattern used in performance-critical code:
# Copy frequently accessed globals/attributes to local variables

import math

def slow_version(n: int) -> float:
    total = 0.0
    for i in range(1, n + 1):
        total += math.sqrt(i)   # LOAD_GLOBAL 'math' + LOAD_ATTR 'sqrt' — 2 lookups per iteration
    return total

def fast_version(n: int) -> float:
    sqrt = math.sqrt    # Hoist to local — LOAD_FAST in loop
    total = 0.0
    for i in range(1, n + 1):
        total += sqrt(i)  # LOAD_FAST 'sqrt' — 1 fast lookup per iteration
    return total

# For n=1_000_000: fast_version is typically 15-25% faster
# Same principle: hoist len(), append method, dict.__getitem__, etc.

# Even faster: use sum() + generator (C-implemented loop)
def fastest_version(n: int) -> float:
    return sum(math.sqrt(i) for i in range(1, n + 1))
```

---

## 8. `__slots__` — Memory Optimization

### Must Know

By default, every Python class instance stores its attributes in a `__dict__` (a hash map). For classes with millions of instances, this is wasteful. `__slots__` replaces the dict with a fixed C-level array.

```python
import sys

# Default class — instance dict
class PointDict:
    def __init__(self, x: float, y: float) -> None:
        self.x = x
        self.y = y

# Slots class — no instance dict
class PointSlots:
    __slots__ = ("x", "y")

    def __init__(self, x: float, y: float) -> None:
        self.x = x
        self.y = y

p_dict  = PointDict(1.0, 2.0)
p_slots = PointSlots(1.0, 2.0)

print(sys.getsizeof(p_dict))    # ~48-56 bytes (object header)
print(sys.getsizeof(p_slots))   # ~40-48 bytes (object header — smaller)
print(p_dict.__dict__)          # {'x': 1.0, 'y': 2.0}
# print(p_slots.__dict__)       # AttributeError! No __dict__

# The REAL saving is from NOT having __dict__
import sys
print(sys.getsizeof(p_dict.__dict__))   # 232 bytes for the dict itself!
# Total PointDict: ~48 + 232 = ~280 bytes per instance
# Total PointSlots: ~48 bytes per instance
# Savings: ~4-6x fewer bytes

# Create 1 million instances
n = 1_000_000
import tracemalloc
tracemalloc.start()
points_dict = [PointDict(float(i), float(i)) for i in range(n)]
s1 = tracemalloc.take_snapshot()
del points_dict

points_slots = [PointSlots(float(i), float(i)) for i in range(n)]
s2 = tracemalloc.take_snapshot()
# PointDict: ~280 MB; PointSlots: ~56 MB — 5x smaller!
```

### Slots Limitations

```python
class WithSlots:
    __slots__ = ("x", "y")
    # Limitation 1: Cannot add new attributes dynamically
    # p = WithSlots(); p.z = 10  → AttributeError!

    # Limitation 2: No __dict__ — cannot use vars(), default pickle won't work
    # Add __dict__ to __slots__ to allow extra attrs (defeats part of the purpose)
    # __slots__ = ("x", "y", "__dict__")  → allows extra attrs but less memory savings

    # Limitation 3: Inheritance requires care
    # Subclass without __slots__ gets a __dict__ anyway — slots savings lost

class Base:
    __slots__ = ("x",)

class Child(Base):
    __slots__ = ("y",)   # MUST declare slots in child too, or __dict__ appears
    # With this: Child only has x, y — no dict. Correct!

class ChildBad(Base):
    pass   # No __slots__ → __dict__ appears → memory saving from Base's slots lost

# With @dataclass:
from dataclasses import dataclass

@dataclass(slots=True)   # Python 3.10+ — auto-generates __slots__
class Point:
    x: float
    y: float

p = Point(1.0, 2.0)
print(p.__slots__)   # ('x', 'y')
```

---

## 9. `functools.lru_cache` — Memoization

### Must Know

```python
from functools import lru_cache, cache
import timeit

# @lru_cache — memoizes function calls; LRU eviction when maxsize is reached
# @cache — same as @lru_cache(maxsize=None) — unlimited cache (Python 3.9+)

# Classic example: Fibonacci without memoization — O(2^n)
def fib_slow(n: int) -> int:
    if n <= 1:
        return n
    return fib_slow(n - 1) + fib_slow(n - 2)

# With memoization — O(n)
@lru_cache(maxsize=128)
def fib_fast(n: int) -> int:
    if n <= 1:
        return n
    return fib_fast(n - 1) + fib_fast(n - 2)

@cache   # Unlimited — use when cache size doesn't need bounding
def fib_unlimited(n: int) -> int:
    if n <= 1:
        return n
    return fib_unlimited(n - 1) + fib_unlimited(n - 2)

print(timeit.timeit(lambda: fib_slow(30), number=10))    # ~20s
print(timeit.timeit(lambda: fib_fast(30), number=10))    # ~0.0001s — orders faster

# Cache info
print(fib_fast.cache_info())   # CacheInfo(hits=28, misses=31, maxsize=128, currsize=31)

# Clear the cache
fib_fast.cache_clear()
print(fib_fast.cache_info())   # CacheInfo(hits=0, misses=0, maxsize=128, currsize=0)
```

### `lru_cache` Requirements and Traps

```python
from functools import lru_cache

# REQUIREMENT: all arguments must be hashable
@lru_cache
def process(items: tuple[int, ...]) -> int:   # tuple is hashable — OK
    return sum(items)

# FAILS at runtime:
# @lru_cache
# def process(items: list[int]) -> int:   # list is NOT hashable — TypeError on call!
#     return sum(items)

# Workaround for unhashable args — convert to hashable first
def process_list(items: list[int]) -> int:
    return _process_cached(tuple(items))   # Convert list → tuple before cache

@lru_cache
def _process_cached(items: tuple[int, ...]) -> int:
    return sum(items)

# TRAP: lru_cache on a method holds a reference to `self`
# This prevents garbage collection of the instance!
class MyClass:
    @lru_cache(maxsize=100)    # TRAP: self is the cache key — each instance has its own cache
    def compute(self, n: int) -> int:
        return n * n

# Fix: use methodtools.lru_cache or cache per class, not per instance
# Or use functools.cached_property for per-instance computed attributes

from functools import cached_property

class Circle:
    def __init__(self, radius: float) -> None:
        self.radius = radius

    @cached_property
    def area(self) -> float:
        import math
        return math.pi * self.radius ** 2
    # Computed once on first access, cached on the instance — no lru_cache overhead

c = Circle(5.0)
print(c.area)   # Computed
print(c.area)   # Cached — same value returned instantly
```

---

## 10. Python-Specific Optimizations

### String Operations

```python
# Rule: use join() for building strings from many parts — O(n) vs O(n²)
parts = ["part" + str(i) for i in range(1000)]

# BAD: O(n²) — new string object created each iteration
result = ""
for part in parts:
    result += part   # Copies all previous content on each +=

# GOOD: O(n) — join calculates total length first, allocates once, copies once
result = "".join(parts)

# f-string vs format() vs %
name, value = "Alice", 42
s1 = f"Name: {name}, Value: {value}"    # Fastest — bytecode LOAD_FAST + FORMAT_VALUE
s2 = "Name: {}, Value: {}".format(name, value)  # Slightly slower
s3 = "Name: %s, Value: %d" % (name, value)      # Older style; similar to format()

# String interning — Python interns short strings
a = "hello"
b = "hello"
print(a is b)   # True — same object (interned)
a = "hello world"
b = "hello world"
print(a is b)   # False in general (not always interned — depends on string length/content)
# Never use `is` for string comparison — use `==`
```

### List, Dict, and Generator Choices

```python
# List comprehension vs generator — choose based on usage

# Need ALL results immediately → list comprehension
squares_list = [x * x for x in range(1_000_000)]   # 8 MB in memory immediately

# Process items one-by-one → generator (lazy, constant memory)
squares_gen = (x * x for x in range(1_000_000))    # Zero extra memory until iteration

# Counting items: generators are better
total = sum(x * x for x in range(1_000_000))   # Never materializes the full list

# Filter: use generator for pipelines
big_squares = (x * x for x in range(1_000_000) if x % 2 == 0)

# dict.get() vs KeyError handling
config = {"timeout": 30}

# SLOW in hot paths with frequent misses: exception handling is expensive
try:
    val = config["missing_key"]
except KeyError:
    val = "default"

# FAST: dict.get() with default
val = config.get("missing_key", "default")   # No exception overhead

# setdefault — get or insert in one operation
counts = {}
for item in ["a", "b", "a", "c", "b", "a"]:
    counts[item] = counts.get(item, 0) + 1   # Get or default + increment

# Equivalent but shorter with defaultdict
from collections import defaultdict
counts = defaultdict(int)
for item in ["a", "b", "a", "c", "b", "a"]:
    counts[item] += 1   # int() = 0 for missing keys
```

### Attribute Access Optimization

```python
import math

# In tight loops: avoid repeated attribute lookups
def slow_math(n: int) -> float:
    total = 0.0
    for i in range(n):
        total += math.sin(i) + math.cos(i)  # 2 LOAD_GLOBAL + 2 LOAD_ATTR per iteration
    return total

def fast_math(n: int) -> float:
    sin = math.sin   # Hoist — LOAD_FAST in loop (array index, no dict hash)
    cos = math.cos
    total = 0.0
    for i in range(n):
        total += sin(i) + cos(i)   # 2 LOAD_FAST per iteration
    return total

# append method hoisting
def slow_append(n: int) -> list:
    result = []
    for i in range(n):
        result.append(i)   # LOAD_FAST 'result' + LOAD_ATTR 'append' per iteration

def fast_append(n: int) -> list:
    result = []
    append = result.append   # Hoist attribute lookup
    for i in range(n):
        append(i)            # LOAD_FAST 'append' — faster
    return result

# Fastest: list comprehension (C-optimized)
def fastest_append(n: int) -> list:
    return list(range(n))   # All in C — no Python loop overhead
```

---

## 11. Debugging Tools

### `pdb` — Python Debugger

```python
import pdb

def buggy_function(data: list[int]) -> int:
    total = 0
    for item in data:
        pdb.set_trace()   # Drop into interactive debugger here
        # At the (Pdb) prompt:
        # n (next)      — execute next line
        # s (step)      — step into function call
        # c (continue)  — run until next breakpoint
        # p expr        — print expression: p item, p total
        # pp expr       — pretty-print: pp data
        # l (list)      — show source code around current line
        # w (where)     — show call stack (backtrace)
        # b 25          — set breakpoint at line 25
        # b func_name   — set breakpoint at function entry
        # q (quit)      — exit debugger
        total += item
    return total

# Python 3.7+ breakpoint() — uses pdb by default, configurable via PYTHONBREAKPOINT
def modern_debug(data: list[int]) -> int:
    total = 0
    for item in data:
        breakpoint()   # Preferred over pdb.set_trace()
        total += item
    return total

# Disable all breakpoints in production:
# export PYTHONBREAKPOINT=0  → breakpoint() becomes a no-op
```

### `logging` — Structured Logging

```python
import logging
import sys

# Configure logging (do this ONCE at application startup)
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("app.log", encoding="utf-8"),
    ],
)

logger = logging.getLogger(__name__)   # Use module name as logger name

def process_order(order_id: int, amount: float) -> None:
    logger.debug(f"Processing order {order_id}")
    if amount < 0:
        logger.warning("Negative amount: %s for order %s", amount, order_id)
    logger.info("Order %s processed: $%.2f", order_id, amount)

# Level hierarchy:
# DEBUG < INFO < WARNING < ERROR < CRITICAL
# Set logging.WARNING in production to suppress DEBUG/INFO noise

# Structured logging with extra context
logger.info(
    "Request completed",
    extra={"request_id": "abc123", "duration_ms": 45, "status_code": 200}
)

# TRAP: avoid f-strings in log calls when log level may suppress the message
# BAD — f-string always evaluated even if DEBUG is filtered out:
logger.debug(f"Items: {expensive_to_compute()}")

# GOOD — only evaluated if DEBUG is enabled:
logger.debug("Items: %s", expensive_to_compute())   # Old-style lazy formatting
# OR check level first:
if logger.isEnabledFor(logging.DEBUG):
    logger.debug("Items: %s", expensive_to_compute())
```

### `warnings` — Deprecation and Runtime Warnings

```python
import warnings

def old_api(x: int) -> int:
    warnings.warn(
        "old_api() is deprecated; use new_api() instead",
        DeprecationWarning,
        stacklevel=2,   # Points to the caller, not this function
    )
    return x * 2

# Suppress specific warnings
with warnings.catch_warnings():
    warnings.simplefilter("ignore", DeprecationWarning)
    old_api(5)

# Filter warnings at application level
warnings.filterwarnings("error", category=DeprecationWarning)   # Raise as exception
```

### `asyncio` Debug Mode — Slow Coroutine Detection

```python
import asyncio
import logging

# Enable asyncio debug mode — detects:
# 1. Coroutines that take too long without yielding (potential blocking code)
# 2. Tasks that are garbage collected without being awaited
# 3. Coroutines that are never awaited ("forgotten coroutines")

logging.basicConfig(level=logging.DEBUG)

async def main():
    import time
    time.sleep(0.15)   # Blocking call inside async! Debug mode will warn about this.
    await asyncio.sleep(0)

asyncio.run(main(), debug=True)
# Output:
# WARNING:asyncio:Executing <Task 'main' coro=<main()>> took 0.150s; expected < 0.100s

# Set via environment variable (no code change needed):
# PYTHONASYNCIODEBUG=1 python my_service.py
```

---

## 12. Memory Management and Garbage Collection

### Reference Counting

```python
import sys
import gc

# CPython uses reference counting + cyclic garbage collector
# Object is freed immediately when reference count reaches 0

a = [1, 2, 3]
print(sys.getrefcount(a))   # 2 (a + argument to getrefcount)

b = a
print(sys.getrefcount(a))   # 3 (a + b + argument)

del b
print(sys.getrefcount(a))   # 2 (a + argument) — freed immediately when count hits 0

# Circular reference — reference counting alone cannot free
class Node:
    def __init__(self, val: int) -> None:
        self.val = val
        self.next: "Node | None" = None

n1 = Node(1)
n2 = Node(2)
n1.next = n2
n2.next = n1   # Circular reference!

del n1, n2
# Reference counts never hit 0 — Python's cyclic GC handles this periodically
gc.collect()   # Force garbage collection of cycles
```

### Controlling GC

```python
import gc

# GC generations:
# Generation 0: newly created objects — collected most frequently
# Generation 1: survived one collection
# Generation 2: long-lived objects — collected rarely

print(gc.get_threshold())   # (700, 10, 10) — defaults
# Gen 0 collected every 700 new objects; Gen 1 every 10 Gen 0 collections; etc.

# Disable GC temporarily (e.g., during batch processing to avoid stop-the-world pauses)
gc.disable()
# ... batch import thousands of objects ...
gc.enable()
gc.collect()   # Manual collection to catch any cycles

# Check if object is tracked by GC
a = [1, 2, 3]
print(gc.is_tracked(a))   # True — lists are tracked
print(gc.is_tracked(42))  # False — integers < 2^30 are interned, not tracked

# Find objects in a generation
gen0_objects = gc.get_objects(generation=0)
print(f"Gen 0 objects: {len(gen0_objects)}")
```

---

## 13. Profiling Web Services

### FastAPI / Async Service Profiling

```python
# pip install pyinstrument
# pyinstrument is async-aware — cProfile is not; it does not see await points correctly

# Method 1: Middleware for profiling endpoints
from fastapi import FastAPI, Request, Response
from pyinstrument import Profiler

app = FastAPI()

@app.middleware("http")
async def profiling_middleware(request: Request, call_next) -> Response:
    # Only profile when ?profile=1 query param is present
    if request.query_params.get("profile"):
        profiler = Profiler(async_mode="enabled")
        with profiler:
            response = await call_next(request)
        print(profiler.output_text(unicode=True, color=True))
        return response
    return await call_next(request)

# Method 2: Manual profiling in tests
import asyncio
from pyinstrument import Profiler

async def profile_async():
    profiler = Profiler(async_mode="enabled")
    with profiler:
        # Run your async code here
        await some_async_function()
    profiler.print()

asyncio.run(profile_async())
```

---

## 14. Java Developer Bridge

| Concept | Java | Python |
|---|---|---|
| Benchmark tool | `JMH`, `System.nanoTime()` | `timeit.timeit()`, `timeit.repeat()` |
| CPU profiler | JProfiler / YourKit / async-profiler | `cProfile`, `pyinstrument` |
| Visual flame graph | JProfiler flame graph | `snakeviz`, `pyinstrument` HTML output |
| Line profiler | JProfiler method view | `line_profiler` (`@profile`) |
| Memory profiler | VisualVM / Eclipse MAT heap dump | `tracemalloc`, `memory_profiler` |
| Memory per object | `Instrumentation.getObjectSize()` | `sys.getsizeof()` |
| Object pool / cache | Guava Cache | `functools.lru_cache` / `functools.cache` |
| Class memory layout | Object header (mark word + class ptr) | Object header + `__dict__` (or `__slots__`) |
| Memory optimization | Value types / records (Java 16+) | `__slots__` / `@dataclass(slots=True)` |
| Debugger | IDEA debugger, `jdb` | `pdb`, `breakpoint()`, IDE debugger |
| Logging | SLF4J + Logback / Log4j2 | `logging` module (stdlib) |
| GC type | Generational (G1, ZGC, Shenandoah) | Reference counting + cyclic GC |
| GC tuning | `-Xms`, `-Xmx`, GC flags | `gc.set_threshold()`, `gc.disable()` |
| String concat | `StringBuilder` | `"".join([...])` |
| lazy evaluation | Java streams (lazy pipeline) | Generator expressions, `itertools` |
| JIT | HotSpot JIT — hot paths compiled | No JIT in CPython; PyPy has JIT |
| Bytecode viewer | `javap -c MyClass.class` | `dis.dis(function)` |
| Interning | `String.intern()` | Automatic for small ints, short identifiers |
| Method hoisting | JIT auto-hoists (JVM does it) | Manual hoist to local variable (interpreter doesn't JIT) |

---

## 15. Hot Interview Q&A

**Q: How do you profile a slow Python service in production?**  
A: I start with `cProfile` for offline profiling — run the slow code path and sort by `cumtime` to find the hottest call chains. For production without stopping the service, I use `py-spy` (sampling profiler, no code changes, low overhead, works on live processes). For async FastAPI services, `pyinstrument` with `async_mode="enabled"` understands `await` points. For memory issues, `tracemalloc` snapshots before and after the suspected code show what grew. I always profile before optimizing — "measure first, optimize second."

**Q: What is `__slots__` and when should you use it?**  
A: `__slots__` replaces the per-instance `__dict__` with a fixed C-level struct. Every normal Python instance carries a `__dict__` (typically 200-250 bytes overhead) for dynamic attribute storage. With `__slots__ = ("x", "y")`, there is no `__dict__` — attributes are stored in a compact C array. This saves 4-6x memory per instance and speeds up attribute access. Use it when you create millions of instances of the same class (event objects, coordinate objects, nodes in a graph). Don't use it if you need dynamic attributes or when the class is rarely instantiated.

**Q: What are the performance trade-offs between list comprehension, `map()`, and a for loop?**  
A: List comprehension is generally the fastest for simple transformations because the bytecode has a dedicated `LIST_APPEND` instruction optimized in C. `map()` avoids Python-level loop overhead and is comparable or faster when the mapping function is already a C built-in (like `str`, `int`, `abs`) — but slower when it wraps a Python lambda. A for loop with `list.append()` is the slowest of the three because each `append` involves a Python attribute lookup unless you hoist it. For numeric data, all three lose to numpy vectorization by orders of magnitude. The real rule: use list comprehension for clarity; use numpy for performance with numbers.

**Q: How does `functools.lru_cache` work internally?**  
A: `lru_cache` wraps the function and maintains an ordered dictionary (or a doubly-linked list + hash map) mapping arguments to return values. When called with cached arguments, it returns the stored value without executing the function body. On each cache hit, the entry is moved to the "most recently used" end. When the cache is full (`maxsize` reached), the "least recently used" entry is evicted. Arguments must be hashable because they are used as dict keys. `cache` (Python 3.9+) is the same with `maxsize=None` — no eviction, unbounded growth.

**Q: Why is `x is None` preferred over `x == None`?**  
A: `is` is an identity check — it compares object IDs (memory addresses) using a single `IS_OP` bytecode instruction. `==` calls `__eq__`, which dispatches to the object's equality method — potentially invoking user-defined code, triggering attribute lookups, and raising exceptions. `None` is a singleton — there is exactly one `None` object in any Python process, so identity comparison is always correct. Additionally, objects with custom `__eq__` can return truthy values when compared to `None` with `==` but would never be `is None`. The `dis` module reveals that `is None` compiles to a single bytecode vs `==` compiling to a `COMPARE_OP` dispatch.

**Q: What is the Python garbage collector's role if Python uses reference counting?**  
A: Reference counting frees most objects immediately when their count drops to zero. But it cannot free circular references — if object A holds a reference to B, and B holds a reference to A, both counts are always ≥ 1 even after all external references are deleted. CPython's cyclic garbage collector (`gc` module) periodically scans for groups of objects that reference each other but are not reachable from anywhere outside the cycle. It uses a generational algorithm: Gen 0 (new objects, scanned most often), Gen 1 (survived one collection), Gen 2 (long-lived). This is different from Java's tracing GC which starts from GC roots and marks all reachable objects — Java doesn't need reference counting at all.

**Q: How do you find memory leaks in a Python application?**  
A: The standard approach: (1) `tracemalloc` — take a snapshot at two points in time and `compare_to()` to see what allocations grew, grouped by filename and line number. (2) `objgraph` library (`objgraph.show_growth()`) — shows which types of objects increased in count. (3) Monitor RSS memory of the process over time with `psutil.Process().memory_info().rss`. Common causes in Python: accidental global list/dict that grows unboundedly; event listeners / callback references keeping objects alive; `lru_cache` on instance methods holding `self` references; circular references involving `__del__` methods (pre-Python 3.4); large closures capturing outer-scope variables longer than expected.

---

## 16. Final Revision Checklist

### Benchmarking and Profiling

- [ ] I use `timeit.timeit(func, number=N)` for micro-benchmarks; use `min()` of `repeat()` results
- [ ] I know cProfile columns: `tottime` = self time; `cumtime` = total including callees
- [ ] I sort cProfile stats by `cumtime` to find call-chain bottlenecks
- [ ] I use `line_profiler` (`@profile`) for line-level CPU hotspots
- [ ] I use `tracemalloc.take_snapshot()` + `compare_to()` for memory growth analysis
- [ ] I use `pyinstrument` (not cProfile) for async FastAPI services

### Memory Optimization

- [ ] I know `__slots__` eliminates `__dict__` — saves 4-6x memory for many instances
- [ ] I use `@dataclass(slots=True)` for modern slot-based dataclasses
- [ ] I know `sys.getsizeof(obj)` measures size (shallow); must recurse for deep size
- [ ] I know `cached_property` for per-instance computed attributes (no GC trap)

### Memoization

- [ ] I use `@lru_cache(maxsize=N)` for bounded memoization; `@cache` for unbounded
- [ ] I know all arguments must be hashable for `lru_cache`
- [ ] I know `cache_info()` to inspect hits/misses and `cache_clear()` to reset
- [ ] I avoid `@lru_cache` on instance methods (holds `self` — prevents GC)

### Python-Specific Optimizations

- [ ] I use `"".join(parts)` — not `+` — for building strings from many parts
- [ ] I use `dict.get(key, default)` over `try/except KeyError` in hot paths
- [ ] I hoist frequently accessed globals/attributes to local variables in tight loops
- [ ] I use `is None` / `is not None` instead of `== None`
- [ ] I use generators for large sequences when only iterating once
- [ ] I use numpy for numeric data — never pure Python loops over arrays

### Debugging

- [ ] I use `breakpoint()` (Python 3.7+) instead of `pdb.set_trace()`
- [ ] I know pdb commands: `n`, `s`, `c`, `p`, `pp`, `l`, `w`, `b`, `q`
- [ ] I use `logger.debug("msg: %s", value)` — NOT `logger.debug(f"msg: {value}")` in hot paths
- [ ] I use `asyncio.run(main(), debug=True)` to detect blocking calls in async code

### Java Developer Reminders

- [ ] `cProfile` + `snakeviz` ≈ JProfiler/YourKit flame graph
- [ ] `__slots__` ≈ Java record/value type — compact memory layout
- [ ] `lru_cache` ≈ Guava Cache / `@Cacheable` in Spring
- [ ] `"".join(parts)` ≈ `StringBuilder` — same reason: avoids O(n²) string copies
- [ ] CPython has no JIT — manual hoisting of globals to locals is necessary (JVM JIT does this automatically)

---

*File 3 of 4 — Group 3: Senior MAANG*  
*Next: Python-Testing-Pytest-Mocking-Testcontainers-Gold-Sheet.md*
