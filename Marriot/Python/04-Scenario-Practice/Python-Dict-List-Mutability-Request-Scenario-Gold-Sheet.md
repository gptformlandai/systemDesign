# Python Dict, List, Mutability & Request-Scope Scenario Bank — Gold Sheet

> **Track File #20 of 31 · Group 4: Scenario Practice**
> For: Java developer | Level: MAANG scenario depth | Mode: bug hunt + root-cause drills

---

## 1. Interview Priority Meter

| Topic | MAANG Frequency | Java Dev Trap Level |
|---|---|---|
| Mutable default argument (deep) | ★★★★★ | HIGH — no Java equivalent |
| Shallow vs deep copy | ★★★★★ | HIGH — `clone()` vs deep copy differs |
| Request-scope shared state | ★★★★★ | HIGH — Spring `@RequestScope` vs Python manual |
| `ContextVar` for isolation | ★★★★☆ | HIGH — maps to `ThreadLocal` |
| Dict mutation during iteration | ★★★★☆ | MEDIUM — Java `ConcurrentModificationException` |
| List aliasing bugs | ★★★★★ | HIGH — assignment is reference, not copy |
| Shared mutable class attributes | ★★★★★ | HIGH — static field mutation |
| Pydantic / dataclass immutability | ★★★☆☆ | MEDIUM |
| `__slots__` and memory footprint | ★★★☆☆ | LOW |
| `frozenset` / `tuple` as dict keys | ★★★★☆ | MEDIUM |

---

## 2. Mutability Fundamentals — What Is Actually Mutable?

### Python Mutability Reference

| Type | Mutable? | Notes |
|---|---|---|
| `list` | YES | `append`, `extend`, index assignment |
| `dict` | YES | key assignment, `pop`, `update` |
| `set` | YES | `add`, `remove`, `discard` |
| `bytearray` | YES | byte-level mutation |
| `str` | NO | every operation returns a new string |
| `tuple` | NO | but elements can be mutable objects! |
| `int`, `float`, `bool` | NO | assignment creates new object |
| `frozenset` | NO | immutable set |
| `bytes` | NO | |

### Tuple Gotcha — Mutable Contents

```python
t = ([1, 2], [3, 4])

# Tuple is immutable — you cannot reassign its elements
# t[0] = [9, 9]   # TypeError: 'tuple' object does not support item assignment

# But if the element IS mutable, you can mutate it
t[0].append(99)
print(t)   # ([1, 2, 99], [3, 4])  ← tuple unchanged, inner list mutated
```

**Trap:** Using a tuple as a dict key fails if it contains mutable objects:

```python
d = {}
d[([1, 2], 3)] = "value"   # TypeError: unhashable type: 'list'
d[((1, 2), 3)] = "value"   # OK — inner tuple is immutable
```

---

## 3. List Aliasing Scenarios

### Scenario 3-A — Assignment Is Reference, Not Copy

**Interviewer:** "Your cart total function is modifying the original cart object and the caller is upset."

```python
# BUG
def apply_discount(cart, discount):
    discounted = cart           # NOT a copy — same list object!
    for i, item in enumerate(discounted):
        discounted[i]["price"] *= (1 - discount)
    return discounted

original = [{"name": "shoe", "price": 100}]
result = apply_discount(original, 0.1)

print(original[0]["price"])  # 90.0  ← BUG: original mutated!
```

**Root Cause:** `discounted = cart` creates another reference to the same list. Mutations through either name affect the same object.

**Fix 1 — Shallow copy (new list, same dicts):**

```python
import copy

def apply_discount(cart, discount):
    discounted = cart[:]   # or list(cart) or copy.copy(cart)
    # ...
```

**Warning:** Shallow copy is still insufficient here — the list is new but `dict` items inside are still shared:

```python
original = [{"name": "shoe", "price": 100}]
shallow = original[:]
shallow[0]["price"] = 999
print(original[0]["price"])   # 999 — inner dict is still shared!
```

**Fix 2 — Deep copy (new list AND new dicts):**

```python
import copy

def apply_discount(cart, discount):
    discounted = copy.deepcopy(cart)   # completely independent copy
    for item in discounted:
        item["price"] *= (1 - discount)
    return discounted
```

**Strong Answer:**
> "Python assignment creates a new reference to the same object, not a copy. Shallow copy creates a new container but shares the inner objects. Deep copy recursively copies everything. For nested structures like list-of-dicts, only `copy.deepcopy()` fully isolates the copy."

**Java Bridge:** Java `ArrayList::clone()` is shallow — same as Python `list[:]`. Java deep copying requires manual implementation or serialization. Python provides `copy.deepcopy()` for free.

---

### Scenario 3-B — Aliasing in Function Arguments

```python
def grow(items):
    items.append("extra")   # mutates the caller's list!

my_list = [1, 2, 3]
grow(my_list)
print(my_list)   # [1, 2, 3, "extra"]  — caller's list changed
```

**Python passes by object reference** — the function receives the same object the caller has. Mutating the object is visible to the caller.

**Contrast with rebinding:**

```python
def replace(items):
    items = [99, 100]   # local rebind — does NOT affect caller
    return items

my_list = [1, 2, 3]
replace(my_list)
print(my_list)   # [1, 2, 3] — unchanged
```

**Rule:** Mutating an argument (`.append`, index assignment) affects the caller. Rebinding the argument variable (`items = ...`) does not.

---

### Scenario 3-C — List Extend vs Reassign

```python
a = [1, 2, 3]
b = a

# Option 1: mutates — b also changes
a.extend([4, 5])
print(b)   # [1, 2, 3, 4, 5]

# Option 2: rebind — b unaffected
a = a + [4, 5]   # creates a new list object
print(b)   # [1, 2, 3]
```

---

## 4. Dict Mutation Scenarios

### Scenario 4-A — Mutating a Dict During Iteration

**Interviewer:** "This cleanup loop raises a RuntimeError. How do you fix it?"

```python
# BUG
cache = {"a": 1, "b": 2, "c": 3, "d": 4}

for key in cache:
    if cache[key] > 2:
        del cache[key]   # RuntimeError: dictionary changed size during iteration
```

**Fix — Iterate over a snapshot of keys:**

```python
for key in list(cache.keys()):   # list() materializes keys before iteration
    if cache[key] > 2:
        del cache[key]

# Or use dict comprehension to build a new dict
cache = {k: v for k, v in cache.items() if v <= 2}
```

**Java Bridge:** Java's `ConcurrentModificationException` is the equivalent — iterating and mutating a collection concurrently fails. Java's fix is `Iterator.remove()` or a copy. Python's fix is `list(dict.keys())` or a comprehension.

---

### Scenario 4-B — dict.update() Merges, Not Replaces Selectively

```python
defaults = {"timeout": 30, "retry": 3, "debug": False}
overrides = {"timeout": 60}

config = defaults.copy()
config.update(overrides)
# {"timeout": 60, "retry": 3, "debug": False}  ← correct merge

# Python 3.9+ merge operator
config = defaults | overrides
# {"timeout": 60, "retry": 3, "debug": False}

# Python 3.9+ in-place merge
config |= overrides
```

---

### Scenario 4-C — Nested Dict Mutation Through Shared Reference

```python
base_config = {
    "db": {"host": "localhost", "port": 5432},
    "cache": {"host": "localhost", "port": 6379}
}

# Shallow copy — inner dicts are still shared
service_a = base_config.copy()
service_a["db"]["port"] = 9999   # mutates base_config too!

print(base_config["db"]["port"])  # 9999 — BUG

# Deep copy — fully isolated
import copy
service_a = copy.deepcopy(base_config)
service_a["db"]["port"] = 9999
print(base_config["db"]["port"])  # 5432 — safe
```

---

### Scenario 4-D — `setdefault` and `defaultdict` for Accumulation

```python
# Manual accumulation — verbose
groups = {}
for item in data:
    key = item["category"]
    if key not in groups:
        groups[key] = []
    groups[key].append(item)

# setdefault — cleaner
groups = {}
for item in data:
    groups.setdefault(item["category"], []).append(item)

# defaultdict — cleanest
from collections import defaultdict
groups = defaultdict(list)
for item in data:
    groups[item["category"]].append(item)
```

---

## 5. Mutable Default Argument — Full Drill

### Scenario 5-A — Why the Bug Exists at the Bytecode Level

```python
def accumulate(value, store=[]):
    store.append(value)
    return store

# Inspect the default
print(accumulate.__defaults__)   # ([],)  ← THE object that accumulates

accumulate(1)   # [1]
accumulate(2)   # [1, 2]   ← same [] from __defaults__
accumulate(3)   # [1, 2, 3]

# The default is stored in the function object itself
# __defaults__ persists as long as the function does
print(accumulate.__defaults__)   # ([1, 2, 3],)
```

**Interview-Level Explanation:**
> "Python compiles function defaults at the `def` statement, binding them to `function.__defaults__`. This is a tuple of the evaluated default objects. For mutable objects, the same object is reused across all calls that don't provide the argument."

---

### Scenario 5-B — Mutable Default in a Class Method

```python
# BUG
class RequestHandler:
    def process(self, data, results=[]):   # class-level mutable default
        results.append(data)
        return results

h1 = RequestHandler()
h2 = RequestHandler()

h1.process("a")   # ["a"]
h2.process("b")   # ["a", "b"]  ← h2 shares h1's default!
```

**Fix:**

```python
class RequestHandler:
    def process(self, data, results=None):
        if results is None:
            results = []
        results.append(data)
        return results
```

---

### Scenario 5-C — Mutable Default Dict in FastAPI Dependency

```python
# BUG — in production FastAPI
from fastapi import FastAPI, Depends

app = FastAPI()

# This default is evaluated ONCE at startup
def get_settings(overrides: dict = {"debug": False, "timeout": 30}):
    return overrides  # same dict every call!

@app.get("/config")
async def get_config(settings: dict = Depends(get_settings)):
    settings["request_id"] = id(settings)  # mutates the shared default!
    return settings
```

**Fix — Return a fresh dict every call:**

```python
from typing import Optional

def get_settings(overrides: Optional[dict] = None):
    defaults = {"debug": False, "timeout": 30}
    if overrides:
        defaults.update(overrides)
    return defaults
```

---

## 6. Shallow vs Deep Copy — Decision Framework

### When to Use Each

```python
import copy

data = [{"id": 1, "tags": ["python", "backend"]}, {"id": 2, "tags": ["java"]}]

# Assignment — same object, no copy
alias = data                  # any mutation visible everywhere

# Shallow copy — new outer list, same inner dicts
shallow = data[:]             # or copy.copy(data)
shallow.append({"id": 3})     # OK — outer list is independent
shallow[0]["tags"].append("new")  # BUG — inner dict still shared!

# Deep copy — fully independent
deep = copy.deepcopy(data)
deep[0]["tags"].append("new")     # Safe — inner objects are independent
```

### Decision Table

| Need | Method |
|---|---|
| New list, contents shared | `lst[:]`, `list(lst)`, `copy.copy()` |
| New dict, contents shared | `d.copy()`, `copy.copy(d)` |
| New list AND new nested objects | `copy.deepcopy()` |
| New dict AND new nested objects | `copy.deepcopy()` |
| Immutable top-level, fine sharing | assignment |
| Performance-critical, flat data | shallow copy |
| Complex nested configs/models | deep copy |

### `deepcopy` Performance Warning

```python
import copy
import timeit

big = [{"key": i, "data": list(range(100))} for i in range(1000)]

# deepcopy is recursive — can be slow for large objects
t = timeit.timeit(lambda: copy.deepcopy(big), number=100)
print(f"deepcopy: {t:.3f}s")

# For immutable-value dicts, dict comprehension is faster
fast_copy = {k: v for k, v in big[0].items()}
```

---

## 7. Request-Scope Shared State Scenarios

### Scenario 7-A — Module-Level State Bleeds Across Requests

**Interviewer:** "In your Flask API, users are occasionally seeing each other's cart total. The code uses no database for this calculation. What's the bug?"

```python
# BUG — module-level mutable state
from flask import Flask, request as flask_request

app = Flask(__name__)

current_cart = []         # module-level — shared across ALL requests!
cart_total = 0.0

@app.route("/add-item", methods=["POST"])
def add_item():
    item = flask_request.json
    current_cart.append(item)       # mutates shared list!
    cart_total += item["price"]     # ← UnboundLocalError too (global needed)
    return {"cart": current_cart, "total": cart_total}
```

**Three Problems:**
1. `current_cart` is shared — all requests see all users' items
2. `cart_total` reassignment fails without `global` keyword
3. Even with `global`, concurrent requests cause race conditions

**Fix — Derive everything from the request:**

```python
from flask import Flask, request as flask_request

app = Flask(__name__)

@app.route("/add-item", methods=["POST"])
def add_item():
    # State lives only in this request's local scope
    items = flask_request.json.get("items", [])
    total = sum(item["price"] for item in items)
    return {"cart": items, "total": total}
```

**Or use Flask's `g` for request-scoped state:**

```python
from flask import Flask, g, request as flask_request

app = Flask(__name__)

@app.before_request
def init_request_state():
    g.cart = []
    g.total = 0.0

@app.route("/add-item", methods=["POST"])
def add_item():
    item = flask_request.json
    g.cart.append(item)
    g.total += item["price"]
    return {"cart": g.cart, "total": g.total}
```

**`flask.g` is request-scoped** — created fresh for each request, destroyed when the request ends.

---

### Scenario 7-B — FastAPI Shared State via Mutable Dependency

```python
# BUG — dependency returns shared mutable object
from fastapi import FastAPI, Depends

app = FastAPI()

_shared_headers = {"X-Request-Id": ""}   # module-level dict

def get_headers():
    return _shared_headers   # same dict every request!

@app.get("/process")
async def process(headers: dict = Depends(get_headers)):
    headers["X-Request-Id"] = "req-123"   # mutates shared state!
    return headers
```

**Fix — Dependency returns new object each call:**

```python
import uuid
from fastapi import FastAPI, Depends

app = FastAPI()

def get_request_headers():
    return {"X-Request-Id": str(uuid.uuid4())}   # new dict per call

@app.get("/process")
async def process(headers: dict = Depends(get_request_headers)):
    return headers
```

---

### Scenario 7-C — ContextVar for True Request Isolation

**`contextvars.ContextVar` is the production-correct way** to store per-request state in both threading and async contexts.

```python
from contextvars import ContextVar
from fastapi import FastAPI, Request
import uuid

app = FastAPI()

# ContextVar — each asyncio task / thread gets its own copy
request_id_var: ContextVar[str] = ContextVar("request_id", default="")
current_user_var: ContextVar[dict] = ContextVar("current_user", default={})

@app.middleware("http")
async def set_request_context(request: Request, call_next):
    token_id = request_id_var.set(str(uuid.uuid4()))
    token_user = current_user_var.set({})
    try:
        response = await call_next(request)
        return response
    finally:
        request_id_var.reset(token_id)     # restore previous value
        current_user_var.reset(token_user)

@app.get("/profile")
async def get_profile():
    req_id = request_id_var.get()
    user = current_user_var.get()
    return {"request_id": req_id, "user": user}
```

**Why `ContextVar` over globals:**
- Thread-safe (no shared state between threads)
- Async-safe (each `asyncio.Task` gets its own context copy)
- Properly reset via `.reset(token)` — no leakage

**Java Bridge:** Java uses `ThreadLocal<T>` for per-thread storage. `ContextVar` is the Python equivalent, but it also works across async tasks — `ThreadLocal` does not propagate to spawned threads; `ContextVar` propagates to spawned tasks via context inheritance.

---

### Scenario 7-D — Singleton Service with Mutable State

```python
# BUG — singleton service accumulates state across requests
class UserSessionService:
    _instance = None
    
    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance
    
    def __init__(self):
        self.active_users = {}   # mutable — shared across ALL requests!

    def add_user(self, user_id, data):
        self.active_users[user_id] = data   # OK for a cache, but often misused as request state
```

**When Singleton is Correct:** Stateless services, connection pools, caches with explicit TTL.
**When Singleton is Wrong:** Per-request data, per-user temp state without eviction.

**Fix — Separate request state from shared service:**

```python
from contextvars import ContextVar

active_request_user: ContextVar[dict] = ContextVar("active_request_user", default={})

class UserSessionService:
    _instance = None

    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def set_current_user(self, user: dict):
        active_request_user.set(user)   # per-context, not shared

    def get_current_user(self) -> dict:
        return active_request_user.get()
```

---

## 8. Thread Safety and Shared State

### Scenario 8-A — Counter Race Condition

```python
# BUG — not thread-safe
import threading

counter = 0

def increment():
    global counter
    for _ in range(100_000):
        counter += 1   # read-modify-write: NOT atomic!

threads = [threading.Thread(target=increment) for _ in range(4)]
for t in threads: t.start()
for t in threads: t.join()

print(counter)   # Expected: 400,000. Actual: ~250,000-380,000 (race condition)
```

**Root Cause:** `counter += 1` compiles to three bytecode ops: LOAD, ADD, STORE. The GIL is released between opcodes, so threads can interleave these steps.

**Fix 1 — `threading.Lock`:**

```python
import threading

counter = 0
lock = threading.Lock()

def increment():
    global counter
    for _ in range(100_000):
        with lock:
            counter += 1
```

**Fix 2 — Use `queue.Queue` or `threading.local` by design:**

```python
from collections import Counter
from threading import local

thread_local = local()

def increment():
    if not hasattr(thread_local, "count"):
        thread_local.count = 0
    for _ in range(100_000):
        thread_local.count += 1   # each thread has its own count

# Aggregate at join time
```

**Java Bridge:** Java `AtomicInteger` provides lock-free atomic increment. Python has no built-in atomic integer — use `threading.Lock` or `queue.Queue`. Python's `queue.Queue` is fully thread-safe.

---

### Scenario 8-B — Shared List in Multithreaded Code

```python
# BUG — list.append is not atomic for multi-step uses
results = []

def worker(data):
    processed = process(data)
    results.append(processed)   # append itself is GIL-safe
    # but reading + appending + reading in sequence is not safe

# queue.Queue is the correct producer-consumer structure
import queue
result_queue = queue.Queue()

def worker(data):
    result_queue.put(process(data))   # thread-safe

threads = [threading.Thread(target=worker, args=(d,)) for d in data]
for t in threads: t.start()
for t in threads: t.join()

results = []
while not result_queue.empty():
    results.append(result_queue.get())
```

---

## 9. Pydantic and Dataclass Immutability Scenarios

### Scenario 9-A — Pydantic Prevents Mutation (v2)

```python
from pydantic import BaseModel

class UserRequest(BaseModel):
    model_config = {"frozen": True}   # immutable model

    user_id: int
    name: str

req = UserRequest(user_id=1, name="Alice")
req.name = "Bob"   # ValidationError: Instance is frozen
```

**Use frozen Pydantic models for request DTOs** to prevent accidental mutation inside handlers.

---

### Scenario 9-B — Dataclass with `frozen=True`

```python
from dataclasses import dataclass

@dataclass(frozen=True)
class Point:
    x: float
    y: float

p = Point(1.0, 2.0)
p.x = 9.0   # FrozenInstanceError: cannot assign to field 'x'

# Frozen dataclasses are hashable — can be used as dict keys
grid = {Point(0, 0): "origin", Point(1, 0): "x-axis"}
```

---

### Scenario 9-C — Mutable Default in Dataclass

```python
from dataclasses import dataclass, field

# BUG
@dataclass
class Cart:
    items: list = []   # TypeError at class creation!
    # dataclass prevents this to protect you

# Fix — use field(default_factory=list)
@dataclass
class Cart:
    items: list = field(default_factory=list)   # creates new list per instance
    metadata: dict = field(default_factory=dict)
```

**`field(default_factory=...)` is Python's enforced version of `None` sentinel.** Dataclasses raise `ValueError` if you try to use a mutable default directly.

---

## 10. Production Checklist — Mutation Safety

### Function Signature Audit

```python
# Every function signature — check for mutable defaults
def process(data, options={}):    # FAIL
def process(data, options=None):  # PASS

def enqueue(item, queue=[]):      # FAIL
def enqueue(item, queue=None):    # PASS

# If you see dict or list in __defaults__, it's a bug
print(process.__defaults__)   # ({},) ← evidence
```

### Request-Scope Audit

```python
# Module-level mutable vars reachable from request handlers? → BUG
# Class-level mutable attrs on a singleton reachable from handlers? → BUG
# Dependency function returning same dict/list every call? → BUG
# No ContextVar or flask.g for per-request state? → Risk
```

### Copy Strategy Checklist

```python
# Before returning a shared object from a function:
# 1. Is the caller allowed to mutate it?
#    No  → return a copy
# 2. Is the object nested (list-of-dicts, dict-of-lists)?
#    Yes → return deep copy
# 3. Is the object flat (list of ints, dict of primitives)?
#    Yes → shallow copy is sufficient
```

---

## 11. Java Developer Bridge — Full Comparison

| Scenario | Java Behavior | Python Behavior | Lesson |
|---|---|---|---|
| Function mutable defaults | Impossible | `def f(x=[])` — shared | Use `None` + `if None` |
| Assignment semantics | Object references (reference semantics) | Same — reference semantics | Identical concept |
| List copy | `new ArrayList<>(original)` = shallow | `list(l)` / `l[:]` = shallow | Same depth |
| Deep copy | Manual / serialization | `copy.deepcopy()` built-in | Python easier |
| Dict mutation in loop | `ConcurrentModificationException` | `RuntimeError: changed size` | Same error, different name |
| Thread-safe counter | `AtomicInteger` | `threading.Lock` + `int` | No atomic int in Python |
| Thread-local state | `ThreadLocal<T>` | `threading.local()` | Same concept |
| Async/request-scoped state | Request scope beans / `@RequestScope` | `ContextVar` | ContextVar > ThreadLocal for async |
| Immutable value object | `record` (Java 14+) | `@dataclass(frozen=True)` | Very similar |
| Mutable default in class | Static field mutation | Class variable mutation | Same risk |
| Singleton with mutable state | Anti-pattern — use Spring bean | Anti-pattern — use `ContextVar` | Same fix |
| Shallow copy warning | `clone()` is shallow | `.copy()` is shallow | Same — must go deeper manually |

---

## 12. Hot Interview Q&A

**Q1: What is the difference between `copy.copy()` and `copy.deepcopy()`?**
> `copy.copy()` creates a new container (list, dict) but the elements inside are still the same objects as in the original. `copy.deepcopy()` recursively creates completely independent copies of all nested objects. For a flat list of integers, both are equivalent. For a list-of-dicts, only `deepcopy` gives full isolation.

**Q2: Why can you mutate a list inside a tuple?**
> Tuple immutability means you cannot rebind its elements (no `t[0] = new_value`). But if an element is a mutable object like a list, you can mutate that object through the reference the tuple holds. The tuple's reference is fixed; the list's contents are not.

**Q3: How do you fix `RuntimeError: dictionary changed size during iteration`?**
> Iterate over a snapshot of keys using `list(d.keys())` or build a new dict with a comprehension: `{k: v for k, v in d.items() if condition}`. Never delete from a dict (or add keys to it) while iterating directly over it.

**Q4: How do you store per-request state safely in FastAPI?**
> Use `contextvars.ContextVar`. Set a value at the start of each request (typically in middleware) and reset it at the end using the token returned by `.set()`. This is safe for both concurrent async requests and multi-threaded workers because each asyncio Task inherits its own copy of the context.

**Q5: Why is `list.append()` thread-safe but a counter increment is not?**
> Python's GIL ensures each bytecode instruction executes atomically. `list.append()` is a single C-level operation — one bytecode instruction under the GIL. `counter += 1` expands to LOAD_FAST + BINARY_ADD + STORE_FAST — three operations. Between LOAD and STORE, the GIL can switch to another thread, causing a lost update.

**Q6: What is `field(default_factory=list)` in a dataclass and why is it needed?**
> Dataclasses prohibit mutable default values directly (e.g., `items: list = []`) because that would create a class-level shared list — the same mutable default argument bug. `field(default_factory=list)` instructs the dataclass machinery to call `list()` for each new instance, creating a fresh list every time.

**Q7: What is the difference between `threading.local()` and `ContextVar`?**
> `threading.local()` gives each thread its own namespace, but it does not propagate to spawned threads and does not work correctly in async code where many coroutines share one thread. `ContextVar` works correctly in both async and threaded code. In asyncio, each `Task` inherits a copy of its parent's context at creation time, so `ContextVar` values set in middleware automatically propagate to the handler.

---

## 13. Final Revision Checklist

- [ ] Can explain why `def f(x=[])` is a bug without notes — default evaluated at `def` time
- [ ] Can explain the difference between mutating an argument vs rebinding it
- [ ] Can distinguish shallow copy from deep copy with a concrete nested example
- [ ] Can identify when `list[:]` is sufficient vs when `copy.deepcopy()` is needed
- [ ] Can fix `RuntimeError: dictionary changed size during iteration`
- [ ] Can explain why `counter += 1` has a race condition even with the GIL
- [ ] Can implement thread-safe counter with `threading.Lock`
- [ ] Can explain when to use `flask.g` vs module-level variable for request state
- [ ] Can implement request-scoped state in FastAPI using `ContextVar`
- [ ] Can explain the difference between `threading.local()` and `ContextVar` in async context
- [ ] Can fix a Pydantic/dataclass mutable default using `field(default_factory=...)`
- [ ] Can explain why a frozen dataclass is hashable and how to use it as a dict key
- [ ] Can identify a singleton with mutable state as a request-scope bug
- [ ] Can write a `copy.deepcopy` deep copy and explain when NOT to use it (performance)
