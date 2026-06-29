# Python Memory Optimization — __slots__, Object Pooling & Memory Patterns — Gold Sheet

> **Track**: Python Interview Track — Group 3: Senior MAANG
> **File**: Gap Fill #2 (Track File #19b)
> **Audience**: Java developers targeting MAANG-level Python backend interviews
> **Read after**: Python-Internals-Memory-GC-GIL-MAANG-Master-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| `__slots__` — what it is and when to use it | ★★★★★ | Java fields are always slot-like; Python's `__dict__` per object is a surprise |
| `__dict__` overhead — why plain classes are memory-heavy | ★★★★★ | Java devs don't know Python objects carry a hash-map by default |
| Memory savings measurement — `sys.getsizeof` | ★★★★☆ | Java devs assume Python objects are as compact as Java POJOs |
| `__slots__` inheritance gotchas | ★★★★☆ | Java subclassing is straightforward; slots + inheritance has specific rules |
| Object interning — small ints, short strings | ★★★★☆ | Java `String.intern()` is explicit; Python interning is implicit and surprising |
| `weakref` — weak references for caches | ★★★★☆ | Java `WeakReference` exists but is rarely used; Python caches need it |
| Object pooling patterns | ★★★☆☆ | Java has commons-pool2; Python patterns are more manual |
| `__del__` danger — finalizer pitfalls | ★★★★☆ | Java `finalize()` is deprecated; `__del__` in Python has similar problems |
| `tracemalloc` — finding memory leaks | ★★★★☆ | Java has JVM heap dumps; Python has tracemalloc for allocation tracing |
| `objgraph` — visualizing object retention | ★★★☆☆ | No Java equivalent; useful for diagnosing what is keeping objects alive |
| Generator vs list — lazy memory pattern | ★★★★★ | Java streams are lazy by default; Python lists are eager — easy to waste memory |

---

## 2. The `__dict__` Problem — Why Python Objects Are Memory-Heavy

### Must Know

```
Every regular Python object carries a __dict__ — a per-instance hash map
that stores all instance attributes.

Java comparison:
  A Java object's fields are compiled into fixed offsets in the object layout.
  Field access is a memory offset calculation — O(1), compact.

Python default:
  obj.name is stored as obj.__dict__["name"]
  The __dict__ is a full Python dict: hash table, load factor, key objects.
  A dict with zero entries still occupies ~200 bytes.
  Each attribute stored in __dict__ has Python object overhead for both the key
  (the string name) and the value.

Result:
  A Python class instance with 5 attributes uses 400-600 bytes.
  An equivalent Java object uses 40-80 bytes.
  When creating millions of instances (domain objects, cache entries, records),
  this overhead is the difference between a service that fits in RAM and one that OOMs.
```

### Measuring the Problem

```python
import sys

class RegularPoint:
    def __init__(self, x, y, z):
        self.x = x
        self.y = y
        self.z = z

p = RegularPoint(1.0, 2.0, 3.0)
print(sys.getsizeof(p))              # ~48 bytes — the object shell only
print(sys.getsizeof(p.__dict__))     # ~232 bytes — the __dict__ (empty baseline ~200)
print(p.__dict__)                    # {'x': 1.0, 'y': 2.0, 'z': 3.0}

# Total actual memory: ~280 bytes per instance (object + dict)
# Plus: 3 float objects at ~24 bytes each = ~72 bytes
# Total per point: ~352 bytes

# sys.getsizeof is SHALLOW — it does not count nested objects
# To measure true deep size, use memory_profiler or tracemalloc
```

---

## 3. `__slots__` — The Fix

### How `__slots__` Works

```python
class SlottedPoint:
    __slots__ = ("x", "y", "z")   # declare allowed attributes

    def __init__(self, x, y, z):
        self.x = x
        self.y = y
        self.z = z

p = SlottedPoint(1.0, 2.0, 3.0)
print(sys.getsizeof(p))       # ~72 bytes — object with 3 slot descriptors
print(hasattr(p, "__dict__")) # False — no __dict__ at all!

# Attribute access is now a descriptor lookup at a fixed offset — like Java fields
# Cannot add new attributes dynamically:
p.w = 4.0   # AttributeError: 'SlottedPoint' object has no attribute 'w'
```

### Memory Comparison at Scale

```python
import tracemalloc

N = 1_000_000

tracemalloc.start()
regular_objects = [RegularPoint(i, i, i) for i in range(N)]
snap1 = tracemalloc.take_snapshot()
del regular_objects

tracemalloc.stop()
tracemalloc.start()
slotted_objects = [SlottedPoint(i, i, i) for i in range(N)]
snap2 = tracemalloc.take_snapshot()
del slotted_objects

# Typical results for 1 million instances:
# RegularPoint: ~350 MB
# SlottedPoint: ~56 MB
# Savings: ~85%
```

### Java Developer Bridge

| Java | Python default | Python with `__slots__` |
|---|---|---|
| Field layout determined at compile time | Runtime `__dict__` per instance | Descriptor slots at fixed offsets |
| `Point p = new Point(1,2,3)` — ~32 bytes | `RegularPoint(1,2,3)` — ~350 bytes | `SlottedPoint(1,2,3)` — ~56 bytes |
| Cannot add fields after compilation | Can add arbitrary attributes | Cannot add attributes not in `__slots__` |
| Field access = memory offset | Dict key lookup | Descriptor protocol — near-offset speed |

---

## 4. `__slots__` — Inheritance Rules

### The Gotcha

```python
# Slots work cleanly in single-inheritance chains IF every class declares __slots__

class Animal:
    __slots__ = ("name", "weight")

class Dog(Animal):
    __slots__ = ("breed",)   # only ADD the NEW attributes
    # Do NOT re-declare name or weight — they are inherited from Animal's slots

d = Dog()
d.name = "Rex"       # works — inherited from Animal's slots
d.breed = "Lab"      # works — declared in Dog's slots
d.age = 3            # AttributeError — not in any __slots__

# Correct: Dog has 3 total slots (name, weight from Animal + breed from Dog)
```

### When One Class in the Chain Lacks `__slots__`

```python
# If any class in the hierarchy does NOT declare __slots__, __dict__ is re-introduced.

class Base:
    __slots__ = ("x",)

class Middle:           # no __slots__ — gets __dict__
    pass

class Child(Middle):
    __slots__ = ("y",)  # __slots__ declared, but Middle already has __dict__

c = Child()
c.x   # AttributeError — Base.x not usable because Middle broke the chain
c.z = "anything"  # works — Middle's __dict__ is present on Child too

# Rule: for full slot benefits, EVERY class in the chain must declare __slots__ = (...)
# Even if the base class adds nothing new: class Base: __slots__ = ()
```

### Pickling Slotted Objects

```python
import pickle

class Config:
    __slots__ = ("host", "port")
    def __init__(self, host, port):
        self.host = host
        self.port = port

c = Config("localhost", 5432)
# pickle.dumps(c) → TypeError by default if __getstate__/__setstate__ not defined
# Slots require explicit state handling for pickling

class Config:
    __slots__ = ("host", "port")

    def __init__(self, host, port):
        self.host = host
        self.port = port

    def __getstate__(self):
        return {slot: getattr(self, slot) for slot in self.__slots__}

    def __setstate__(self, state):
        for slot, value in state.items():
            setattr(self, slot, value)

# Now pickling works:
data = pickle.dumps(c)
c2 = pickle.loads(data)
```

---

## 5. Object Interning — Small Integers and Strings

### Small Integer Cache

```python
# CPython pre-creates integer objects for -5 to 256
# All uses of these values reference the SAME object

a = 100
b = 100
print(a is b)    # True — same object (within cached range)

a = 257
b = 257
print(a is b)    # False — each assignment creates a new int object
                 # Note: in interactive mode may vary; in compiled code may be True due to peephole optimizer

# Java parallel:
# Java auto-boxes Integer.valueOf() using a cache for -128 to 127
# Python's range is -5 to 256 (implementation-specific, CPython only)
```

### String Interning

```python
# Short strings that look like identifiers are usually interned automatically
a = "hello"
b = "hello"
print(a is b)    # True — CPython interns these (implementation detail)

# Strings with spaces are NOT automatically interned
a = "hello world"
b = "hello world"
print(a is b)    # False (may vary — not guaranteed)

# Explicit interning
import sys
a = sys.intern("hello world")
b = sys.intern("hello world")
print(a is b)    # True — same object guaranteed

# When to use sys.intern:
# - Large number of repeated strings as dict keys (e.g., parsing fixed-schema logs)
# - Reduces both memory (one object) and dict lookup time (identity comparison before hash)
```

### Interview Trap — `is` vs `==`

```python
# is checks IDENTITY (same object in memory)
# == checks EQUALITY (same value)

# Interning makes is True for some strings — but this is an implementation detail!
a = "test"
b = "test"
print(a is b)    # True (CPython intern artifact — NOT guaranteed by language spec)

# Never use `is` to compare string values in production code
# Always use == for value comparison
# Only use `is` for:
#   - None: `if x is None`
#   - Sentinel objects you explicitly created
#   - Boolean: `if flag is True` (rare — usually just `if flag`)
```

---

## 6. `weakref` — Weak References for Caches

### The Problem Without weakref

```python
class Cache:
    def __init__(self):
        self._store = {}    # strong references — objects live as long as cache lives

cache = Cache()

class BigObject:
    def __init__(self, data):
        self.data = data   # large data

obj = BigObject(b"x" * 10_000_000)   # 10 MB object
cache._store["key"] = obj             # cache holds strong reference

del obj    # user deletes their reference
# obj is NOT freed! cache._store["key"] still holds a strong reference.
# Memory leak until cache entry is explicitly removed.
```

### The Fix — `weakref`

```python
import weakref

class WeakCache:
    def __init__(self):
        self._store = weakref.WeakValueDictionary()   # values are weak references

cache = WeakCache()

obj = BigObject(b"x" * 10_000_000)
cache._store["key"] = obj

del obj    # reference count drops to 0 — object IS freed immediately
# cache._store["key"] is now gone (WeakValueDictionary removes dead entries)

result = cache._store.get("key")   # returns None — entry was cleaned up
```

### `lru_cache` vs `WeakValueDictionary`

```python
# functools.lru_cache — holds strong references up to maxsize
# Good for: pure function results, immutable cached values
# Risk: cache holds objects alive even after all other code drops them

from functools import lru_cache

@lru_cache(maxsize=256)
def get_config(env: str) -> dict:
    return load_config_from_file(env)

# WeakValueDictionary — holds weak references, objects freed when no other ref exists
# Good for: caching live objects (ORM models, connection objects) that may legitimately die
# Risk: cache misses increase because entries disappear when objects are freed
```

---

## 7. `__del__` — Finalizer Pitfalls

### Why `__del__` Is Dangerous

```python
# Java finalize() is deprecated for the same reasons.

class Connection:
    def __init__(self, host):
        self.host = host
        self._socket = open_socket(host)

    def __del__(self):
        self._socket.close()   # DANGEROUS — multiple problems:

# Problem 1: Not called in a deterministic time
#   Reference counting usually calls __del__ promptly, but cyclic garbage
#   collection may delay it indefinitely.

# Problem 2: Exceptions in __del__ are silently suppressed
#   If __del__ raises, Python prints a warning but does not propagate the exception.

# Problem 3: __del__ may run when the interpreter is shutting down
#   Global names may be None during shutdown; __del__ referencing globals will fail.

# Problem 4: Resurrects the object temporarily
#   If __del__ stores self somewhere, the object survives its "death" cycle.

# Correct pattern: use context managers instead
class Connection:
    def __init__(self, host):
        self.host = host
        self._socket = open_socket(host)

    def close(self):
        self._socket.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
        return False

with Connection("localhost") as conn:
    conn.query(...)
# conn.close() called deterministically at end of with block — even on exception
```

---

## 8. Object Pooling Patterns

### When Object Pooling Helps in Python

```
Java commons-pool2:
  Pools of expensive objects (DB connections, HTTP clients, thread pools).
  Java garbage collector has GC pressure from rapid allocation/deallocation.
  Pooling reduces allocation and GC work.

Python context:
  CPython uses reference counting — objects are freed immediately when ref count = 0.
  GC pressure for short-lived objects is lower than Java.
  Pooling still helps for:
    1. Objects that are EXPENSIVE to create (network connections, subprocesses)
    2. Objects that have SETUP/TEARDOWN costs (crypto contexts, parser state)
    3. Reducing memory fragmentation in long-running services
```

### Simple Object Pool Implementation

```python
from queue import Queue
from contextlib import contextmanager
from typing import TypeVar, Generic, Callable

T = TypeVar("T")

class ObjectPool(Generic[T]):
    """Thread-safe object pool using a bounded queue."""

    def __init__(self, factory: Callable[[], T], size: int):
        self._pool: Queue[T] = Queue(maxsize=size)
        for _ in range(size):
            self._pool.put(factory())

    @contextmanager
    def acquire(self):
        obj = self._pool.get()    # blocks until an object is available
        try:
            yield obj
        finally:
            self._pool.put(obj)   # always return to pool

# Usage
import re

# Expensive: compiled regex patterns (already pooled by Python's re cache, but illustrates pattern)
pool = ObjectPool(lambda: re.compile(r"\d{4}-\d{2}-\d{2}"), size=10)

with pool.acquire() as pattern:
    match = pattern.match("2024-01-15")
```

### Async Object Pool

```python
import asyncio
from contextlib import asynccontextmanager

class AsyncObjectPool:
    def __init__(self, factory, size: int):
        self._queue: asyncio.Queue = asyncio.Queue(maxsize=size)
        self._factory = factory
        self._size = size
        self._initialized = False

    async def initialize(self):
        for _ in range(self._size):
            obj = await self._factory()
            await self._queue.put(obj)
        self._initialized = True

    @asynccontextmanager
    async def acquire(self):
        obj = await self._queue.get()
        try:
            yield obj
        finally:
            await self._queue.put(obj)

# Note: asyncpg and SQLAlchemy async engine already have built-in pools.
# Custom async pools are needed for non-standard resources (SMTP connections, etc.).
```

---

## 9. Memory Profiling Tools

### `tracemalloc` — Built-in Allocation Tracing

```python
import tracemalloc

tracemalloc.start()

# ... run the code you want to profile ...
data = [dict(i=i, val=i * 2) for i in range(100_000)]

snapshot = tracemalloc.take_snapshot()
top_stats = snapshot.statistics("lineno")

print("Top 5 memory allocations:")
for stat in top_stats[:5]:
    print(stat)

# Output example:
# file.py:5: size=45.8 MiB, count=100000, average=480 B

tracemalloc.stop()
```

### `memory_profiler` — Line-by-Line Memory Usage

```python
# Install: pip install memory-profiler

from memory_profiler import profile

@profile
def allocate():
    data = [0] * 1_000_000      # allocates ~8 MB
    result = sum(data)
    del data                     # freed here
    return result

allocate()

# Output:
# Line #    Mem usage    Increment   Line Contents
# ================================================
#      4     52.3 MiB     52.3 MiB   def allocate():
#      5     59.9 MiB      7.6 MiB       data = [0] * 1_000_000
#      6     59.9 MiB      0.0 MiB       result = sum(data)
#      7     52.3 MiB     -7.6 MiB       del data
#      8     52.3 MiB      0.0 MiB       return result
```

### `objgraph` — What Is Keeping Objects Alive

```python
# Install: pip install objgraph

import objgraph

# Show most common object types in memory
objgraph.show_most_common_types(limit=10)
# dict       15234
# list        8901
# function    4512
# ...

# Find what is keeping a specific object alive
leak = []
x = {"keep_alive": leak}
leak.append(x)   # cycle: x → leak → x

objgraph.show_backrefs(x, max_depth=3)   # renders a graph showing what holds x
```

---

## 10. Generator vs List — The Most Common Memory Win

### Must Know

```python
# Eager list — allocates all N items immediately
def all_records_list(n: int) -> list[dict]:
    return [{"id": i, "val": i * 2} for i in range(n)]

data = all_records_list(10_000_000)   # 10M dicts in memory — may OOM

# Lazy generator — allocates one item at a time
def all_records_gen(n: int):
    for i in range(n):
        yield {"id": i, "val": i * 2}

for record in all_records_gen(10_000_000):   # constant memory
    process(record)

# Generator expressions — same laziness, inline syntax
total = sum(i * 2 for i in range(10_000_000))   # constant memory
# vs
total = sum([i * 2 for i in range(10_000_000)])  # list comprehension — allocates 10M ints

# Interview rule:
#   If you only iterate once: generator.
#   If you need random access or iterate multiple times: list.
#   If you pass to a function that accepts an iterable: generator.
#   If the consumer calls len(): list (generators have no len).
```

---

## 11. Common Memory Anti-Patterns in Production

### Anti-Pattern 1 — Accumulating Without Limits

```python
# WRONG — unbounded accumulation
events = []   # module-level list

def handle_event(event: dict) -> None:
    events.append(event)   # grows forever; never drained → OOM

# CORRECT — use a bounded deque or flush/emit regularly
from collections import deque
events = deque(maxlen=10_000)   # automatically evicts oldest; bounded memory
```

### Anti-Pattern 2 — Module-Level Mutable State (Hidden Leaks)

```python
# WRONG — registry grows unboundedly
_registry: dict[str, object] = {}

def register(name: str, obj: object) -> None:
    _registry[name] = obj   # strong reference; objects never freed

# CORRECT — use WeakValueDictionary if registry should not own object lifetime
import weakref
_registry: weakref.WeakValueDictionary = weakref.WeakValueDictionary()
```

### Anti-Pattern 3 — Reading Entire File Into Memory

```python
# WRONG
with open("large.json") as f:
    data = json.load(f)   # entire file in memory

# CORRECT for JSONL (newline-delimited JSON)
with open("large.jsonl") as f:
    for line in f:               # one line at a time
        record = json.loads(line)
        process(record)

# CORRECT for streaming large JSON with ijson
import ijson
with open("large.json", "rb") as f:
    for item in ijson.items(f, "records.item"):
        process(item)
```

---

## 12. `__slots__` Decision Guide

```
Use __slots__ when ALL of the following are true:
  1. You will create a very large number of instances (> ~10,000)
  2. The attribute set is FIXED and known at class definition time
  3. You do NOT need arbitrary dynamic attributes on instances
  4. You do NOT need the class to be easily picklable without custom __getstate__
  5. You are willing to handle inheritance carefully

Do NOT use __slots__ when:
  1. The class is used occasionally (optimization not worth the complexity)
  2. You need dynamic attribute assignment (e.g., a config bag class)
  3. You are prototyping (add __slots__ only after profiling confirms the need)
  4. The class uses multiple inheritance from classes that don't also use __slots__

Practical targets:
  - Domain model value objects (Point, Color, Coordinate, Event, Metric)
  - Parser/lexer token classes created in bulk
  - Message objects in high-throughput message processing pipelines
```

---

## 13. Strong Interview Answers

### "What is `__slots__` and when would you use it?"

```text
By default, every Python instance carries a __dict__ — a hash map that stores its
attributes. This gives Python the flexibility to add attributes to any object at
runtime, but at significant memory cost: a bare dict starts at around 200 bytes,
and each attribute adds the overhead of a Python string key and the value object.

__slots__ replaces __dict__ with a fixed set of descriptors at class definition time,
similar to how Java fields are compiled into fixed offsets in the object layout.
The instance no longer carries a dict, cutting per-instance memory by 70 to 85 percent
for attribute-heavy objects.

I would use __slots__ for domain objects created in large numbers — for example, event
objects in a stream processor or coordinate objects in a spatial index. The trade-off
is that you lose dynamic attribute assignment, and inheritance requires every class in
the chain to declare __slots__. I would only add __slots__ after profiling confirms
the memory cost is actually a problem — not as premature optimization.
```

### "How do you diagnose a Python memory leak in production?"

```text
I start with tracemalloc, which is built into the standard library. I take two
snapshots with a time delay between them and compare the statistics by lineno.
The lines showing the highest allocation delta are the leak candidates.

For identifying what is retaining objects, I use objgraph. Its show_backrefs
function renders the reference chain keeping a set of objects alive — often
revealing a module-level list or dict that is accumulating objects indefinitely.

Common causes I look for first: module-level collections growing without a cap,
event handlers or callbacks registered but never removed, closures capturing large
objects, and caches backed by strong references where weakref would be correct.
For async services, I also check for task objects that were created but never
awaited or cancelled — these accumulate in the event loop's internal task list.
```

---

## 14. Revision Checklist

- [ ] Can explain why Python objects are memory-heavy by default (`__dict__`)
- [ ] Can write a class with `__slots__` and explain what changes
- [ ] Knows `__slots__` + inheritance rule (every class in chain must declare it)
- [ ] Can explain the pickle limitation and implement `__getstate__`/`__setstate__`
- [ ] Knows small integer cache range (-5 to 256) and string interning behavior
- [ ] Can explain `is` vs `==` and why `is` for strings is an antipattern
- [ ] Can write a `WeakValueDictionary`-backed cache and explain when it wins
- [ ] Knows why `__del__` is dangerous and what to use instead (`__exit__`)
- [ ] Can use `tracemalloc` to find the top memory allocation sites
- [ ] Can explain generator vs list memory trade-off
- [ ] Can identify unbounded accumulation and registry patterns as leak sources
