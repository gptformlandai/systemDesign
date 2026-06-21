# Python Scenario-Based Quick Revision — Gold Sheet

> **Track File #19 of 31 · Group 4: Scenario Practice**
> For: Java developer | Level: MAANG scenario pressure | Mode: rapid-fire revision

---

## 1. Interview Priority Meter

| Topic | MAANG Frequency | Java Dev Trap Level |
|---|---|---|
| Mutable default arguments | ★★★★★ | HIGH — no Java equivalent |
| Closure / late binding | ★★★★★ | HIGH — lambda semantics differ |
| `is` vs `==` in identity | ★★★★☆ | MEDIUM — similar to `==` vs `.equals()` |
| Generator vs list memory | ★★★★★ | HIGH — Java streams are lazy, generators lazier |
| Class vs instance variable | ★★★★☆ | MEDIUM — maps to static vs instance |
| GIL and threading | ★★★★★ | HIGH — no equivalent in Java |
| async blocking calls | ★★★★★ | HIGH — very different from `CompletableFuture` |
| `*args/**kwargs` forwarding | ★★★★☆ | MEDIUM |
| `__slots__` memory | ★★★☆☆ | LOW — no Java equivalent |
| Import side effects | ★★★★☆ | MEDIUM |

---

## 2. Mutability Scenarios

### Scenario 2-A — Mutable Default Argument Bug

**Interviewer:** "You see this bug in production — every new user gets the previous user's history. What went wrong and how do you fix it?"

```python
# BUG: shared mutable default
def add_to_history(event, history=[]):
    history.append(event)
    return history

# First call
add_to_history("login")    # ["login"]
# Second call — different user!
add_to_history("purchase") # ["login", "purchase"]  ← BUG: shared list
```

**Root Cause:** Default argument `[]` is evaluated ONCE at function definition time. All callers share the same list object.

**Fix:**

```python
def add_to_history(event, history=None):
    if history is None:
        history = []   # new list per call
    history.append(event)
    return history
```

**Strong Answer Template:**
> "Default arguments in Python are bound to the function object at definition time, not at call time. Mutable objects — lists, dicts, sets — accumulate state across calls. The fix is to use `None` as the sentinel and create a new instance inside the function body."

**Java Bridge:** In Java, `new ArrayList<>()` in a parameter default doesn't exist — defaults must be set in the body. Java forces this pattern; Python allows the trap.

---

### Scenario 2-B — Dict Default Argument

```python
# BUG
def update_config(key, value, config={}):
    config[key] = value
    return config

update_config("timeout", 30)   # {"timeout": 30}
update_config("retry", 3)      # {"timeout": 30, "retry": 3} ← leaks across calls
```

**Fix:**

```python
def update_config(key, value, config=None):
    if config is None:
        config = {}
    config[key] = value
    return config
```

---

## 3. Closure and Late Binding Scenarios

### Scenario 3-A — Classic Late Binding Trap

**Interviewer:** "This code is supposed to create 5 functions that print 0–4. Why do they all print 4?"

```python
funcs = []
for i in range(5):
    funcs.append(lambda: i)

for f in funcs:
    print(f())  # prints 4, 4, 4, 4, 4 — NOT 0, 1, 2, 3, 4
```

**Root Cause:** The lambda captures the **variable** `i`, not its **value** at creation. After the loop, `i` is 4. All lambdas look up the same `i` at call time.

**Fix 1 — Default argument capture:**

```python
funcs = [lambda i=i: i for i in range(5)]
```

**Fix 2 — `functools.partial`:**

```python
from functools import partial

def return_val(i):
    return i

funcs = [partial(return_val, i) for i in range(5)]
```

**Strong Answer:**
> "Python closures capture variables by reference, not by value. At call time the function looks up the current value of `i`, which is 4 after the loop completes. The fix is to capture the value at creation time using a default argument, which is evaluated at definition time."

**Java Bridge:** Java lambdas require captured variables to be effectively final — Java catches this at compile time. Python has no such restriction, so the bug is silent.

---

### Scenario 3-B — Closure Counter

```python
def make_counter():
    count = 0
    def increment():
        nonlocal count
        count += 1
        return count
    return increment

c = make_counter()
c()  # 1
c()  # 2
c()  # 3
```

**Key Point:** `nonlocal` allows the inner function to rebind the enclosing variable. Without `nonlocal`, `count += 1` would raise `UnboundLocalError` because assignment creates a new local.

---

## 4. Identity vs Equality Scenarios

### Scenario 4-A — `is` vs `==`

```python
a = [1, 2, 3]
b = [1, 2, 3]
c = a

print(a == b)   # True  — same values
print(a is b)   # False — different objects
print(a is c)   # True  — same object
```

### Scenario 4-B — Small Integer Caching

```python
x = 256
y = 256
print(x is y)   # True — CPython caches -5 to 256

x = 257
y = 257
print(x is y)   # False — new objects outside cache range
```

**Strong Answer:**
> "`is` checks object identity (memory address). `==` checks value equality via `__eq__`. CPython caches small integers -5 to 256 and interned strings, so `is` can return `True` for equal values — but this is an implementation detail. Always use `==` for value comparison."

**Java Bridge:** Java `==` on objects checks reference identity (same as Python `is`). Java `.equals()` checks value equality (same as Python `==`). The naming is flipped — a common source of bugs for Java devs reading Python code.

---

### Scenario 4-C — String Interning

```python
a = "hello"
b = "hello"
print(a is b)   # True — CPython interns short strings

a = "hello world"
b = "hello world"
print(a is b)   # False (usually) — not guaranteed to be interned
```

**Never rely on string `is` for correctness. Always use `==`.**

---

## 5. Scope and LEGB Scenarios

### Scenario 5-A — UnboundLocalError Trap

```python
total = 0

def add(x):
    total += x   # UnboundLocalError!
    return total
```

**Root Cause:** Python sees `total =` (the `+=` expands to `total = total + x`) and marks `total` as local. But it's read before assignment, causing `UnboundLocalError`.

**Fixes:**

```python
# Fix 1: global
def add(x):
    global total
    total += x

# Fix 2: return value (preferred)
def add(total, x):
    return total + x

# Fix 3: class attribute (best for stateful ops)
class Accumulator:
    def __init__(self):
        self.total = 0
    def add(self, x):
        self.total += x
```

---

### Scenario 5-B — LEGB Resolution Order

```python
x = "global"

def outer():
    x = "enclosing"
    def inner():
        x = "local"
        print(x)   # local
    inner()
    print(x)       # enclosing

outer()
print(x)           # global
```

**LEGB = Local → Enclosing → Global → Built-in**

---

## 6. OOP Scenarios

### Scenario 6-A — Class vs Instance Variable Bug

**Interviewer:** "Every Cart object is sharing the same items list. How?"

```python
# BUG
class Cart:
    items = []   # CLASS variable — shared across all instances!

    def add(self, item):
        self.items.append(item)  # mutates the CLASS list

c1 = Cart()
c2 = Cart()
c1.add("apple")
print(c2.items)   # ["apple"] — Bug: c2 sees c1's item
```

**Fix:**

```python
class Cart:
    def __init__(self):
        self.items = []   # INSTANCE variable — each Cart gets its own list

    def add(self, item):
        self.items.append(item)
```

**Strong Answer:**
> "Class variables are shared across all instances. Mutable class variables like lists and dicts are particularly dangerous — any mutation is visible to every instance. The fix is to initialize the mutable attribute in `__init__`, creating a per-instance copy."

**Java Bridge:** In Java, `static` fields are class-level. Python class variables are equivalent to Java `static` fields. Java devs know not to make `static` fields mutable collections — apply the same rule in Python.

---

### Scenario 6-B — MRO (Method Resolution Order)

```python
class A:
    def greet(self): return "A"

class B(A):
    def greet(self): return "B"

class C(A):
    def greet(self): return "C"

class D(B, C):
    pass

print(D.__mro__)
# (<class 'D'>, <class 'B'>, <class 'C'>, <class 'A'>, <class 'object'>)
print(D().greet())   # "B" — B is before C in MRO
```

**MRO Rule:** Python uses C3 linearization. The order follows left-to-right depth-first, but deduplicating so each class appears once as late as needed.

---

### Scenario 6-C — `super()` in Diamond Inheritance

```python
class Animal:
    def __init__(self, name):
        self.name = name
        print(f"Animal.__init__({name})")

class Flyable(Animal):
    def __init__(self, name):
        super().__init__(name)
        print(f"Flyable.__init__({name})")

class Swimmable(Animal):
    def __init__(self, name):
        super().__init__(name)
        print(f"Swimmable.__init__({name})")

class Duck(Flyable, Swimmable):
    def __init__(self, name):
        super().__init__(name)

Duck("Donald")
# Animal.__init__(Donald)   ← only called ONCE
# Swimmable.__init__(Donald)
# Flyable.__init__(Donald)
```

**Key Point:** `super()` follows MRO. With cooperative multiple inheritance, `Animal.__init__` is only called once even though both `Flyable` and `Swimmable` call `super()`.

---

## 7. Generator and Memory Scenarios

### Scenario 7-A — Generator vs List

**Interviewer:** "You need to process 10 million log lines. Why is your teammate's solution crashing with OOM?"

```python
# Teammate's code — OOM at 10M lines
def process_logs(filename):
    with open(filename) as f:
        lines = f.readlines()    # loads ALL lines into memory
    return [parse(line) for line in lines]

# Your fix — constant memory
def process_logs(filename):
    with open(filename) as f:
        for line in f:           # lazy iteration
            yield parse(line)    # generator: one line in memory at a time
```

**Strong Answer:**
> "A list comprehension materializes all items immediately. A generator yields one item at a time — memory stays constant at O(1) regardless of file size. For 10 million lines, the generator approach uses a few KB; the list approach loads gigabytes."

---

### Scenario 7-B — Generator Exhaustion

```python
def gen():
    yield 1
    yield 2
    yield 3

g = gen()
list(g)   # [1, 2, 3]
list(g)   # []  ← generator is exhausted!
```

**Trap:** Generators are single-use. To reuse, call the generator function again or wrap in a class with `__iter__`.

**Java Bridge:** Java `Stream` is also single-use — can't call `.toList()` twice. Same concept.

---

### Scenario 7-C — `itertools` in Production

```python
import itertools

# Batch data in chunks of 1000
def batched(iterable, n):
    it = iter(iterable)
    while True:
        batch = list(itertools.islice(it, n))
        if not batch:
            break
        yield batch

# Process 1M records in batches
for batch in batched(records, 1000):
    db.bulk_insert(batch)
```

**Use `itertools.islice`, `itertools.chain`, `itertools.groupby` for lazy, memory-efficient iteration.**

---

## 8. Async Scenarios

### Scenario 8-A — Blocking Inside Async

**Interviewer:** "Your FastAPI endpoint is slow even though it's `async def`. Why?"

```python
import time
from fastapi import FastAPI

app = FastAPI()

@app.get("/users/{user_id}")
async def get_user(user_id: int):
    time.sleep(2)   # BLOCKS the event loop for 2 seconds!
    return {"user_id": user_id}
```

**Root Cause:** `time.sleep()` is a blocking call. Inside `async def`, it blocks the event loop thread, preventing ALL other requests from being served for 2 seconds.

**Fix:**

```python
import asyncio

@app.get("/users/{user_id}")
async def get_user(user_id: int):
    await asyncio.sleep(2)   # yields control — other requests can run
    return {"user_id": user_id}
```

**Strong Answer:**
> "Python's asyncio event loop is single-threaded. A blocking call like `time.sleep()` or a synchronous DB call blocks the entire event loop, effectively making the server single-request until it completes. You must use `await asyncio.sleep()`, async DB drivers, or `loop.run_in_executor()` for truly blocking I/O."

---

### Scenario 8-B — Sequential vs Concurrent Async

```python
import asyncio
import httpx

async def fetch(url):
    async with httpx.AsyncClient() as client:
        r = await client.get(url)
        return r.json()

# SLOW: sequential — total time = sum of all waits
async def fetch_all_slow(urls):
    results = []
    for url in urls:
        results.append(await fetch(url))
    return results

# FAST: concurrent — total time = longest single wait
async def fetch_all_fast(urls):
    return await asyncio.gather(*[fetch(url) for url in urls])
```

**Java Bridge:** `asyncio.gather()` is similar to `CompletableFuture.allOf()`. Sequential `await` in a loop is like chaining `.thenCompose()` — it's sequential, not concurrent.

---

## 9. Backend / Production Scenarios

### Scenario 9-A — Request State Bleeding Between Requests

**Interviewer:** "Users are sometimes seeing each other's data in your Flask app. You're not using a database bug. What's happening?"

```python
# BUG: global mutable state in Flask
from flask import Flask, g

app = Flask(__name__)
current_user = {}   # BUG: module-level dict shared across requests

@app.route("/profile")
def profile():
    current_user["name"] = request.args.get("name")
    return current_user["name"]
```

**Root Cause:** Module-level mutable variables persist across requests in the same process. Under concurrent requests, one request can overwrite another's data.

**Fix — Use Flask's `g` or request context:**

```python
from flask import Flask, g, request

app = Flask(__name__)

@app.route("/profile")
def profile():
    g.current_user = {"name": request.args.get("name")}
    return g.current_user["name"]
```

**Or use `contextvars.ContextVar` (Python 3.7+):**

```python
from contextvars import ContextVar

current_user: ContextVar[dict] = ContextVar("current_user", default={})

@app.route("/profile")
def profile():
    current_user.set({"name": request.args.get("name")})
    return current_user.get()["name"]
```

---

### Scenario 9-B — Import Side Effects

```python
# database.py
print("Connecting to DB...")           # side effect on import!
connection = create_db_connection()    # expensive — runs at import time

# any_file.py
import database   # triggers connection immediately, even in tests
```

**Fix — Lazy initialization:**

```python
# database.py
_connection = None

def get_connection():
    global _connection
    if _connection is None:
        _connection = create_db_connection()
    return _connection
```

**Java Bridge:** Python modules are singletons — importing runs the module body once. Java class static blocks run once at class load time. Same concept; Python is more permissive about what you run at module level.

---

### Scenario 9-C — Memory Leak from Unclosed Resources

```python
# BUG: file handle leaked if exception thrown
def read_file(path):
    f = open(path)
    data = f.read()
    # exception here? f never closed!
    f.close()
    return data

# Fix: context manager guarantees close on exception
def read_file(path):
    with open(path) as f:
        return f.read()
```

**Extended — DB connection leak:**

```python
# BUG
conn = db.connect()
cursor = conn.cursor()
cursor.execute(query)
results = cursor.fetchall()
# exception above? connection leaked!

# Fix
with db.connect() as conn:
    with conn.cursor() as cursor:
        cursor.execute(query)
        return cursor.fetchall()
```

---

## 10. Tricky Output Scenarios

### Scenario 10-A — `list * n` Shallow Copy

```python
matrix = [[0] * 3] * 3
matrix[0][0] = 9

print(matrix)
# [[9, 0, 0], [9, 0, 0], [9, 0, 0]]  ← Bug: all rows are same object!
```

**Root Cause:** `[[0]*3] * 3` creates 3 references to the **same inner list**, not 3 separate lists.

**Fix:**

```python
matrix = [[0] * 3 for _ in range(3)]   # 3 distinct lists
matrix[0][0] = 9
print(matrix)
# [[9, 0, 0], [0, 0, 0], [0, 0, 0]]  ← Correct
```

---

### Scenario 10-B — `sorted()` vs `.sort()`

```python
nums = [3, 1, 4, 1, 5]

# sorted() — returns new list, original unchanged
new = sorted(nums)   # [1, 1, 3, 4, 5]
# nums is still [3, 1, 4, 1, 5]

# .sort() — in-place, returns None
result = nums.sort()
# result is None — common bug: reassigning to None
```

---

### Scenario 10-C — Dictionary Ordering (Python 3.7+)

```python
d = {"b": 2, "a": 1, "c": 3}
list(d.keys())   # ["b", "a", "c"] — insertion order preserved
```

**Python 3.7+ dicts guarantee insertion order.** This is a CPython implementation detail promoted to a language spec. Do NOT rely on this for sorting logic — use `sorted(d.items())` explicitly.

---

### Scenario 10-D — `zip` Truncation

```python
a = [1, 2, 3, 4]
b = ["a", "b"]

list(zip(a, b))   # [(1, 'a'), (2, 'b')] — truncates to shortest!

# Fix for unequal lengths:
from itertools import zip_longest
list(zip_longest(a, b, fillvalue=None))
# [(1, 'a'), (2, 'b'), (3, None), (4, None)]
```

---

## 11. GIL Scenarios

### Scenario 11-A — Threading Does Not Parallelize CPU

**Interviewer:** "We added threading to speed up our image processing pipeline, but performance didn't improve. Why?"

```python
import threading

def cpu_heavy(data):
    # intensive computation
    result = sum(x**2 for x in data)
    return result

# Threads don't run truly in parallel for CPU-bound work
threads = [threading.Thread(target=cpu_heavy, args=(chunk,)) for chunk in chunks]
for t in threads: t.start()
for t in threads: t.join()
```

**Root Cause:** The GIL (Global Interpreter Lock) allows only one Python thread to execute bytecode at a time. CPU-bound threads take turns rather than running in parallel.

**Fix — Use `multiprocessing`:**

```python
from multiprocessing import Pool

with Pool(processes=4) as pool:
    results = pool.map(cpu_heavy, chunks)
```

**Strong Answer:**
> "CPython's GIL prevents true thread parallelism for CPU-bound work. I/O-bound threads do benefit from threading because the GIL is released during I/O waits. For CPU-bound parallelism, `multiprocessing` spawns separate processes, each with its own GIL."

**Java Bridge:** Java has no GIL — threads share the heap and run truly in parallel on multiple cores. This is one of Python's biggest differences from Java for performance-critical code.

---

### Scenario 11-B — Threading Wins for I/O

```python
import threading
import requests

urls = ["https://api.example.com/item/1", ...]

def fetch(url):
    response = requests.get(url)   # GIL released during network wait
    return response.json()

# Threads help here — GIL is released during network I/O
threads = [threading.Thread(target=fetch, args=(url,)) for url in urls]
for t in threads: t.start()
for t in threads: t.join()
```

**GIL Rule of Thumb:**
- CPU-bound → `multiprocessing` or C extension
- I/O-bound (network, disk) → `threading` or `asyncio`

---

## 12. Type Coercion Scenarios

### Scenario 12-A — Integer Division

```python
# Python 3
10 / 3    # 3.3333...  always float
10 // 3   # 3          floor division
10 % 3    # 1          modulo

# Python 2 (legacy reference)
10 / 3    # 3          integer division — this trips Python 2 devs
```

**Java Bridge:** Java `int / int = int` (floor). Python 3 `int / int = float`. When you need integer division in Python 3, explicitly use `//`.

---

### Scenario 12-B — `None` Comparisons

```python
# Never do this:
if x == None:   # works, but wrong style

# Always do this:
if x is None:   # correct — None is a singleton
if x is not None:
```

**`None` is a singleton. `is None` checks identity. `== None` calls `__eq__` and can be overridden by custom classes to return unexpected values.**

---

## 13. Comprehension Scenarios

### Scenario 13-A — Dict Comprehension

```python
# Invert a dict
original = {"a": 1, "b": 2, "c": 3}
inverted = {v: k for k, v in original.items()}
# {1: "a", 2: "b", 3: "c"}

# Filter while transforming
squared_evens = {x: x**2 for x in range(10) if x % 2 == 0}
# {0: 0, 2: 4, 4: 16, 6: 36, 8: 64}
```

---

### Scenario 13-B — Set Comprehension for Dedup

```python
data = [1, 2, 2, 3, 3, 3, 4]
unique = {x for x in data}   # {1, 2, 3, 4}

# One-liner dedup preserving order (Python 3.7+ dict guaranteed ordered)
unique_ordered = list(dict.fromkeys(data))   # [1, 2, 3, 4]
```

---

### Scenario 13-C — Nested Comprehension

```python
# Flatten 2D list
matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
flat = [x for row in matrix for x in row]
# [1, 2, 3, 4, 5, 6, 7, 8, 9]

# Read as: "for each row in matrix, for each x in row, yield x"
# Order of for-clauses is the SAME as nested for-loops
```

---

## 14. Java Developer Bridge — Full Scenario Map

| Scenario Topic | Java Behavior | Python Behavior | Risk |
|---|---|---|---|
| Mutable defaults | Impossible — no default expressions | `def f(x=[])` — shared object | HIGH — silent bug |
| Lambda capture | Effectively final required | Captures variable by ref (late binding) | HIGH |
| `==` vs `is` | `==` refs, `.equals()` value | `==` value, `is` identity | MEDIUM (naming reversal) |
| Integer division | `int/int = int` | `int/int = float`, use `//` | MEDIUM |
| Class variable | `static` field | Class body attribute | HIGH — mutation is shared |
| Generator | `Stream` (single-use) | `generator` (single-use, lazy) | MEDIUM |
| Thread parallelism | True parallel | GIL blocks CPU-bound | HIGH |
| Async blocking | `CompletableFuture` + threadpool | `await` required; blocking = deadlock | HIGH |
| `None` | `null` | `None` singleton | LOW |
| Module import | Class static init | Module body runs on first import | MEDIUM |
| `*args` / `**kwargs` | Varargs / no equivalent | Both supported | MEDIUM |
| `sorted()` vs `.sort()` | Returns sorted (Streams) / in-place | `sorted()` new / `.sort()` in-place returns `None` | MEDIUM |

---

## 15. Hot Interview Q&A

**Q1: What is the mutable default argument trap and how do you fix it?**
> Default arguments are bound to the function object at definition time. Mutable defaults like `[]` or `{}` accumulate state across calls. Fix: use `None` as default and initialize inside the function.

**Q2: What is late binding in Python closures?**
> Closures capture the variable itself, not its value. At call time, the function looks up the current value of the variable in the enclosing scope. In loops, all closures see the final loop value unless you use a default argument to capture the current value.

**Q3: Why does `is` sometimes return `True` for equal strings and integers?**
> CPython caches small integers (-5 to 256) and interns certain strings. `is` checks memory address (identity), not value. Never use `is` for value comparison — use `==`.

**Q4: When does threading help in Python, and when doesn't it?**
> Threading helps for I/O-bound work because the GIL is released during I/O waits, allowing other threads to run. For CPU-bound work, the GIL prevents parallel execution — use `multiprocessing` instead.

**Q5: What is a generator and why is it memory-efficient?**
> A generator is an iterator that produces values one at a time using `yield`. It never materializes the full sequence in memory. Memory usage is O(1) regardless of sequence length, making it ideal for large files, streams, and infinite sequences.

**Q6: Why can an `async def` function be slower than a regular function if you use `time.sleep()`?**
> `time.sleep()` blocks the event loop thread. No other coroutines can run until the sleep completes. Use `await asyncio.sleep()` or `await` an async I/O call instead. Blocking in async code is worse than non-async code because it blocks all concurrent requests, not just the current one.

**Q7: What is the difference between a class variable and an instance variable in Python?**
> Class variables are defined in the class body and shared by all instances. Instance variables are defined in `__init__` on `self` and are unique per instance. Mutating a class-level list or dict affects all instances — equivalent to mutating a Java `static` field.

---

## 16. Final Revision Checklist

- [ ] Can explain mutable default argument bug and fix it without notes
- [ ] Can explain late binding trap in closures with a fix using default args
- [ ] Can distinguish `is` vs `==` and explain CPython integer caching
- [ ] Can identify class vs instance variable bug and fix it
- [ ] Can explain GIL impact on CPU-bound vs I/O-bound threading
- [ ] Can rewrite blocking `async def` to use proper `await`
- [ ] Can replace OOM list comprehension with a generator
- [ ] Can explain generator exhaustion and the single-use rule
- [ ] Can use `asyncio.gather()` for concurrent async operations
- [ ] Can explain module-level side effects and lazy initialization
- [ ] Can identify and fix resource leaks using context managers
- [ ] Can use `zip_longest` when sequences may have unequal lengths
- [ ] Can explain dict ordering guarantee (Python 3.7+)
- [ ] Can use `{x for x in data}` set comprehension for dedup
- [ ] Can flatten a 2D list with a nested comprehension
