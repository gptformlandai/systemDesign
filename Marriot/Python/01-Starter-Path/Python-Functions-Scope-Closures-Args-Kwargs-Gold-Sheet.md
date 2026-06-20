# Python Functions, Scope, Closures, Args & Kwargs — Gold Sheet

> **Track**: Python Interview Track — Group 1: Starter Path  
> **File**: 4 of 7  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-Data-Types-Mutability-Deep-Dive.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| Late binding in closures | ★★★★★ | **#1 Python closure bug** — no Java equivalent; will appear in every senior interview |
| `*args` / `**kwargs` semantics | ★★★★★ | Required for decorators, wrappers, and variadic APIs — asked in virtually every interview |
| LEGB scope and `nonlocal` | ★★★★☆ | Java has no `nonlocal`; closure variable capture is fundamentally different |
| Functions as first-class objects | ★★★★☆ | Java requires functional interfaces; Python passes functions directly |
| Decorators — mechanics | ★★★★☆ | Used everywhere in Flask, Django, pytest — must understand `functools.wraps` |
| Keyword-only and positional-only args | ★★★☆☆ | New in Python 3 — no Java equivalent; asked in API design interviews |
| `functools.lru_cache` | ★★★☆☆ | Classic memoization — interviewers love asking about the `maxsize` parameter |
| `functools.partial` | ★★★☆☆ | Currying equivalent — common in callback/event-driven code |
| Lambda limitations | ★★★☆☆ | Java lambdas are powerful; Python lambdas are intentionally limited |
| `functools.reduce` vs loops | ★★☆☆☆ | Rarely the right answer in Python — know why to not use it |

---

## 2. Functions as First-Class Objects

### Must Know

In Python, **functions are objects**. They can be:
- Assigned to variables
- Passed as arguments to other functions
- Returned from functions
- Stored in data structures

This is what makes decorators, callbacks, and higher-order functions possible without Java's `FunctionalInterface` boilerplate.

### How It Works

```python
def greet(name):
    return f"Hello, {name}"

# Assign to variable — same object, different name
say_hello = greet
print(say_hello("Alice"))   # Hello, Alice
print(greet is say_hello)   # True — same object

# Store in a list
ops = [str.upper, str.lower, str.title]
text = "hello WORLD"
for fn in ops:
    print(fn(text))
# HELLO WORLD
# hello world
# Hello World

# Pass as argument
def apply(func, value):
    return func(value)

print(apply(len, "hello"))           # 5
print(apply(str.upper, "hello"))     # HELLO
print(apply(lambda x: x * 2, 5))    # 10

# Return from function
def make_multiplier(n):
    def multiply(x):
        return x * n    # closes over n
    return multiply

double = make_multiplier(2)
triple = make_multiplier(3)
print(double(5))    # 10
print(triple(5))    # 15
```

### Function Attributes and Introspection

```python
def add(a, b):
    """Adds two numbers."""
    return a + b

# All functions have these attributes
print(add.__name__)      # 'add'
print(add.__doc__)       # 'Adds two numbers.'
print(add.__module__)    # '__main__'

import inspect
print(inspect.signature(add))        # (a, b)
print(inspect.getsource(add))        # source code as string
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Passing functions | Requires `FunctionalInterface` (e.g., `Function<T,R>`, `Predicate<T>`) | Pass function directly — no wrapper needed |
| Method reference | `String::toUpperCase` | `str.upper` (unbound method) |
| Lambda | `x -> x * 2` — one expression, limited | `lambda x: x * 2` — one expression, limited |
| Storing functions | `List<Function<String,String>> ops = ...` | `ops = [str.upper, str.lower]` |
| Calling stored function | `ops.get(0).apply("text")` | `ops[0]("text")` |
| Introspection | Reflection API — verbose | `inspect` module — concise |

---

## 3. `def` vs `lambda`

### Must Know

- `def` — creates a named function object, supports multiple statements, docstrings, annotations.
- `lambda` — creates an anonymous function object, **single expression only**, no statements, no docstrings.
- Both produce function objects. `type(lambda: None)` is `<class 'function'>`.

### How It Works

```python
# def — multi-line, full power
def square(x):
    return x ** 2

# lambda — single expression
square_l = lambda x: x ** 2

# Same behaviour
print(square(5))      # 25
print(square_l(5))    # 25

# Lambda use case — sorting key (inline, not worth naming)
pairs = [(1, 'b'), (2, 'a'), (3, 'c')]
pairs.sort(key=lambda pair: pair[1])
print(pairs)   # [(2, 'a'), (1, 'b'), (3, 'c')]

# Lambda with multiple arguments
add = lambda a, b: a + b
print(add(3, 4))   # 7

# Lambda cannot contain statements — these all fail:
# lambda x: if x > 0: return x     # SyntaxError
# lambda x: x = 5                  # SyntaxError
# lambda x: return x               # SyntaxError
```

### When to Use `lambda` vs `def`

```python
# USE lambda — when passing a simple one-off transformation inline
numbers = [3, 1, 4, 1, 5]
numbers.sort(key=lambda x: -x)   # sort descending

# USE lambda — when storing named functions is overkill
from functools import reduce
product = reduce(lambda a, b: a * b, [1, 2, 3, 4, 5])  # 120

# PREFER def — when the logic is complex or reused
# PREFER def — when you need a meaningful name for readability
# PREFER def — when you need a docstring or type annotations

# ANTI-PATTERN: naming a lambda (just use def)
double = lambda x: x * 2    # Linter (PEP 8) warns against this
# Better:
def double(x):               # Use def when naming
    return x * 2
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Named function | Method in a class or static method | `def` at module, class, or function level |
| Anonymous function | Lambda expression (can capture effectively-final variables) | `lambda` (can capture any variable — with late binding caveat) |
| Multi-statement lambda | Not allowed in Java | Not allowed in Python either |
| Lambda as type | Implements a `FunctionalInterface` | Is a `function` object |

---

## 4. LEGB Scope Rule

### Must Know

Python looks up names in this order:
1. **L** — Local: inside the current function
2. **E** — Enclosing: outer function(s) — only relevant in nested functions
3. **G** — Global: module level
4. **B** — Built-in: Python builtins (`len`, `print`, `range`, etc.)

**First match wins. Stops searching once found.**

### How It Works

```python
x = "global"                    # G — module level

def outer():
    x = "enclosing"             # E — outer function's local

    def inner():
        x = "local"             # L — inner function's local
        print(x)                # local — L wins

    inner()
    print(x)                    # enclosing — inner's 'local' is gone

outer()
print(x)                        # global — outer's 'enclosing' is gone
```

### Read vs Assign

```python
x = 10

def read_only():
    print(x)       # READS x from global — works fine

def assign():
    x = 20         # CREATES a new local x — does NOT modify global
    print(x)       # 20 — local

def buggy():
    print(x)       # UnboundLocalError! Python sees the assignment below and
    x = 20         # marks x as local for the whole function — but print comes first

read_only()   # 10
assign()      # 20
print(x)      # 10 — global unchanged
# buggy()     # UnboundLocalError: local variable 'x' referenced before assignment
```

**Strong Interview Answer**: "Python decides at compile time whether a name is local to a function — if the name appears on the left side of an assignment anywhere in the function, it is treated as local throughout that function. Reading a name before its assignment in the same function raises `UnboundLocalError`. This is different from Java where a local variable shadows an outer variable only from its declaration point forward."

### Scope with Comprehensions

```python
x = 10

# List comprehension has its own scope (Python 3)
result = [x for x in range(5)]
print(x)    # 10  — outer x unchanged (Python 3 behaviour)

# In Python 2, comprehension variable leaked into enclosing scope!
# This is a common Python 2 vs 3 difference asked in interviews.

# Generator expression — also own scope
gen = (x for x in range(5))
print(x)    # 10  — unchanged
```

---

## 5. `global` and `nonlocal`

### Must Know

- `global x` — declares that `x` in this function refers to the module-level variable. Allows assignment to global.
- `nonlocal x` — declares that `x` refers to the nearest enclosing scope's variable. Allows assignment to enclosing function's local.
- Neither is needed for **reading** — only for **assigning** to an outer scope variable.

### `global`

```python
count = 0

def increment():
    global count     # Without this, count = count + 1 would UnboundLocalError
    count += 1

increment()
increment()
print(count)   # 2

# Avoid global when possible — use class attributes or pass/return instead
# global is a code smell in most production code but valid for module-level state
```

### `nonlocal`

```python
def make_counter():
    count = 0                  # Enclosing scope variable

    def increment():
        nonlocal count         # Without this, count += 1 would UnboundLocalError
        count += 1
        return count

    def reset():
        nonlocal count
        count = 0

    return increment, reset

inc, reset = make_counter()
print(inc())    # 1
print(inc())    # 2
print(inc())    # 3
reset()
print(inc())    # 1  — counter was reset
```

### Practical Pattern: Counter via Closure

```python
def make_counter(start=0, step=1):
    value = [start]   # Use list to avoid nonlocal in Python 2 (historical trick)
    # In Python 3, just use nonlocal:
    value2 = start

    def counter():
        nonlocal value2
        current = value2
        value2 += step
        return current

    return counter

c = make_counter(10, 5)
print(c())   # 10
print(c())   # 15
print(c())   # 20
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Outer variable access | Captured variables must be `final` or effectively final — read only | Python reads freely from any outer scope |
| Outer variable mutation | Not allowed for captured variables in lambdas/anonymous classes | `nonlocal` keyword allows it |
| Module-level mutation | Static fields are mutable directly | `global` keyword required for assignment |
| Scope of loop variable | Loop variable scoped to the loop block | Loop variable leaks into enclosing scope (NOT comprehension variable in Python 3) |

---

## 6. Closures

### Must Know

A **closure** is a function that **remembers the variables from its enclosing scope** even after that scope has finished executing.

Three conditions for a closure:
1. There must be a nested function (function inside function).
2. The nested function must refer to a variable in the enclosing function.
3. The enclosing function must return the nested function (or pass it out).

### How It Works

```python
def outer(msg):
    # 'msg' is a free variable — part of the closure
    def inner():
        print(msg)   # 'msg' comes from enclosing scope
    return inner

hello = outer("Hello, World!")
hello()    # Hello, World!  — 'outer' is done but 'msg' lives on in the closure

# Inspecting closures
print(hello.__closure__)                     # (<cell at 0x...>,)
print(hello.__closure__[0].cell_contents)    # 'Hello, World!'
print(hello.__code__.co_freevars)            # ('msg',)
```

### Real Use Case: Parameterized Functions

```python
def make_validator(min_val, max_val):
    def validate(x):
        if not (min_val <= x <= max_val):
            raise ValueError(f"{x} must be between {min_val} and {max_val}")
        return x
    return validate

validate_age = make_validator(0, 150)
validate_score = make_validator(0, 100)

print(validate_age(25))      # 25
# validate_age(200)          # ValueError: 200 must be between 0 and 150
print(validate_score(85))    # 85
```

---

## 7. The Late Binding Trap — Critical Interview Topic

### Must Know

**Closures capture the variable name, not its value at the time of closure creation.**

When the closure is called (not when it's defined), Python looks up the variable in the enclosing scope. If the variable's value has changed by then, the closure sees the current value.

### The Classic Bug

```python
# TRAP — all functions end up using i=4 (the final value of i)
functions = []
for i in range(5):
    def f():
        return i    # 'i' is looked up when f() is CALLED, not when defined
    functions.append(f)

print([f() for f in functions])   # [4, 4, 4, 4, 4]  — NOT [0, 1, 2, 3, 4]!
```

### Why This Happens

```
Loop iteration 0: i=0, f is created, f closes over the NAME 'i' (not the value 0)
Loop iteration 1: i=1, new f is created, also closes over 'i'
...
Loop ends: i=4
When we call f(), Python looks up 'i' in the enclosing scope — finds i=4
ALL functions see i=4
```

### Fix 1: Default Argument Captures Value at Definition Time

```python
functions = []
for i in range(5):
    def f(i=i):   # default arg is evaluated at definition time — captures value!
        return i
    functions.append(f)

print([f() for f in functions])   # [0, 1, 2, 3, 4]  ← correct
```

### Fix 2: `functools.partial`

```python
from functools import partial

def f(i):
    return i

functions = [partial(f, i) for i in range(5)]
print([f() for f in functions])   # [0, 1, 2, 3, 4]  ← correct
```

### Fix 3: Factory Function (Most Readable)

```python
def make_f(i):
    def f():
        return i    # 'i' is local to make_f — each call gets its own 'i'
    return f

functions = [make_f(i) for i in range(5)]
print([f() for f in functions])   # [0, 1, 2, 3, 4]  ← correct
```

### Late Binding in Lambdas Too

```python
# TRAP — same issue with lambdas
multipliers = [lambda x: x * i for i in range(5)]
print([m(2) for m in multipliers])   # [8, 8, 8, 8, 8]  — NOT [0, 2, 4, 6, 8]!

# Fix with default argument
multipliers = [lambda x, i=i: x * i for i in range(5)]
print([m(2) for m in multipliers])   # [0, 2, 4, 6, 8]  ← correct
```

**Strong Interview Answer**: "Python closures capture variable names by reference, not values by copy. In a loop, all closures share the same loop variable. By the time any closure is called, the loop has finished and the variable holds its final value. The fix is to force early binding using a default argument (`i=i`) — default arguments are evaluated at function definition time, capturing the current value."

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Variable capture | Only `final` or effectively-final — value is captured at lambda creation | Any variable — name is captured, value looked up at call time |
| Loop variable in lambda | Compile error if loop variable is mutated after capture | Silently captures name — late binding bug |
| Fix for closure over loop var | No issue — effectively-final enforced | Default arg trick, factory function, or `functools.partial` |
| Capture time | At lambda/anonymous class creation | At function call time |

---

## 8. `*args` and `**kwargs`

### Must Know

- `*args` — collects positional arguments into a **tuple**.
- `**kwargs` — collects keyword arguments into a **dict**.
- Names `args` and `kwargs` are convention only — `*numbers` and `**options` are equally valid.
- Order in signature: `positional, *args, keyword_only, **kwargs`.

### `*args` — Variable Positional Arguments

```python
def add(*args):
    print(type(args))   # <class 'tuple'>
    return sum(args)

print(add(1, 2, 3))         # 6
print(add(1, 2, 3, 4, 5))   # 15
print(add())                 # 0

# Mixed: fixed positionals before *args
def log(level, *messages):
    for msg in messages:
        print(f"[{level}] {msg}")

log("INFO", "Server started", "Listening on port 8080")
# [INFO] Server started
# [INFO] Listening on port 8080
```

### `**kwargs` — Variable Keyword Arguments

```python
def configure(**kwargs):
    print(type(kwargs))   # <class 'dict'>
    for key, value in kwargs.items():
        print(f"  {key} = {value}")

configure(host="localhost", port=8080, debug=True)
# host = localhost
# port = 8080
# debug = True

# Mixed: fixed params before **kwargs
def create_user(name, email, **extra):
    user = {"name": name, "email": email}
    user.update(extra)
    return user

user = create_user("Alice", "a@b.com", role="admin", active=True)
print(user)
# {'name': 'Alice', 'email': 'a@b.com', 'role': 'admin', 'active': True}
```

### Combined Signature Order

```python
def full_signature(pos1, pos2, *args, kw_only, **kwargs):
    print(f"pos1={pos1}, pos2={pos2}")
    print(f"args={args}")
    print(f"kw_only={kw_only}")
    print(f"kwargs={kwargs}")

full_signature(1, 2, 3, 4, 5, kw_only="required", extra="bonus")
# pos1=1, pos2=2
# args=(3, 4, 5)
# kw_only=required
# kwargs={'extra': 'bonus'}
```

### Unpacking Operators in Calls (`*` and `**`)

```python
# * unpacks an iterable into positional arguments
def add(a, b, c):
    return a + b + c

nums = [1, 2, 3]
print(add(*nums))        # 6  — same as add(1, 2, 3)
print(add(*range(3)))    # 3  — same as add(0, 1, 2)

# ** unpacks a dict into keyword arguments
def greet(first, last, greeting="Hello"):
    return f"{greeting}, {first} {last}!"

info = {"first": "Alice", "last": "Smith"}
print(greet(**info))                    # Hello, Alice Smith!
print(greet(**info, greeting="Hi"))     # Hi, Alice Smith!

# Unpacking in collections (Python 3.5+)
a = [1, 2, 3]
b = [4, 5, 6]
combined = [*a, *b]      # [1, 2, 3, 4, 5, 6]

d1 = {"x": 1}
d2 = {"y": 2}
merged = {**d1, **d2}    # {"x": 1, "y": 2}
```

### Pass-Through Wrapper Pattern — Used in Decorators

```python
# *args + **kwargs lets you forward all arguments unchanged
def wrapper(func, *args, **kwargs):
    print("Before call")
    result = func(*args, **kwargs)
    print("After call")
    return result

def add(a, b):
    return a + b

print(wrapper(add, 3, 4))    # Before call → After call → 7
print(wrapper(add, a=3, b=4))  # Also works with keyword args
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Variable positional args | `void f(String... args)` — varargs | `def f(*args):` |
| Variable keyword args | No equivalent (use `Map<String,Object>`) | `def f(**kwargs):` |
| Calling with list | `f(list.toArray(new String[0]))` | `f(*my_list)` |
| Calling with map | Manual unpacking | `f(**my_dict)` |
| Forwarding all args | `method.invoke(obj, args)` — reflection | `func(*args, **kwargs)` — no reflection |

---

## 9. Keyword-Only and Positional-Only Arguments

### Must Know

- **Keyword-only args**: after `*` or `*args` — **must** be passed by name.
- **Positional-only args** (Python 3.8+): before `/` — **must** be passed by position, cannot be named.
- Both patterns create clearer, more stable APIs.

### Keyword-Only Arguments (after `*`)

```python
# Everything after * must be passed by keyword
def connect(host, port, *, timeout=30, retries=3):
    print(f"Connecting to {host}:{port} timeout={timeout} retries={retries}")

connect("localhost", 8080)                       # uses defaults
connect("localhost", 8080, timeout=60)           # OK
connect("localhost", 8080, 60)                   # TypeError! timeout must be keyword

# Common pattern: use *args to drain positionals, then keyword-only
def process(*data, separator=",", header=None):
    pass
```

### Why Keyword-Only?

```python
# Without keyword-only: order matters — easy to pass wrong argument
def create(name, email, active):
    pass

create("Alice", "alice@b.com", True)   # Easy to confuse True meaning

# With keyword-only: forced clarity
def create(name, email, *, active):
    pass

# create("Alice", "alice@b.com", True)   # TypeError
create("Alice", "alice@b.com", active=True)   # Must be explicit
```

### Positional-Only Arguments (before `/`)

```python
# Python 3.8+
# Everything before / must be passed positionally — cannot be named
def greet(first, last, /, greeting="Hello"):
    return f"{greeting}, {first} {last}!"

greet("Alice", "Smith")                    # OK
greet("Alice", "Smith", greeting="Hi")    # OK
# greet(first="Alice", last="Smith")      # TypeError! first and last are positional-only

# Why positional-only? API stability — caller cannot depend on parameter names
# If you rename 'first' to 'given_name' later, positional callers are unaffected
```

### Full Signature — All Parameter Types

```python
def full(pos_only, /, normal, *args, kw_only, **kwargs):
    pass

# pos_only: positional only (before /)
# normal: positional or keyword
# *args: extra positionals
# kw_only: keyword only (after *)
# **kwargs: extra keywords
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Keyword-only args | No equivalent — all args positional | `*` in signature forces keyword |
| Positional-only args | All Java method args are positional-only by default | `/` syntax in Python 3.8+ |
| Builder pattern | Used to simulate named parameters | Use keyword args directly — no builder needed |
| Method overloading | Multiple methods with same name, different signatures | Not allowed in Python — use default args or `*args`/`**kwargs` |

---

## 10. `functools` Module

### Must Know

`functools` provides higher-order function utilities. The four most important for interviews:

| Function | Purpose | Java Equivalent |
|---|---|---|
| `functools.wraps` | Preserves wrapped function metadata in decorators | `@Override` annotation (conceptual) |
| `functools.lru_cache` | Memoization with LRU eviction | Guava `CacheBuilder` |
| `functools.partial` | Partially apply arguments — create specialized functions | `Function.andThen` / currying |
| `functools.reduce` | Fold a sequence — left fold | `Stream.reduce()` |

### `functools.wraps` — Essential for Decorators

```python
import functools

def my_decorator(func):
    @functools.wraps(func)    # Preserves func's __name__, __doc__, __annotations__
    def wrapper(*args, **kwargs):
        print(f"Calling {func.__name__}")
        return func(*args, **kwargs)
    return wrapper

@my_decorator
def add(a, b):
    """Adds two numbers."""
    return a + b

print(add.__name__)    # 'add'   ← preserved by @wraps
print(add.__doc__)     # 'Adds two numbers.'  ← preserved

# Without @functools.wraps:
# print(add.__name__)  →  'wrapper'  (broken introspection, broken pytest, broken logging)
```

### `functools.lru_cache` — Memoization

```python
import functools

# maxsize=None means unlimited cache (like a plain dict)
# maxsize=128 (default) — LRU evicts least-recently-used when full
@functools.lru_cache(maxsize=128)
def fibonacci(n):
    if n < 2:
        return n
    return fibonacci(n - 1) + fibonacci(n - 2)

print(fibonacci(50))    # 12586269025 — fast! Without cache: exponential time

# Cache info
print(fibonacci.cache_info())
# CacheInfo(hits=48, misses=51, maxsize=128, currsize=51)

# Clear cache
fibonacci.cache_clear()

# Python 3.9+: @functools.cache is @lru_cache(maxsize=None)
@functools.cache
def factorial(n):
    return n * factorial(n - 1) if n else 1
```

### `functools.partial` — Partial Application

```python
from functools import partial

def power(base, exponent):
    return base ** exponent

square = partial(power, exponent=2)    # fix exponent=2
cube = partial(power, exponent=3)      # fix exponent=3

print(square(5))    # 25
print(cube(3))      # 27

# Real use: pre-configure a function for callbacks
import os
join_to_base = partial(os.path.join, "/app/data")
print(join_to_base("users.csv"))    # /app/data/users.csv
print(join_to_base("logs", "app.log"))  # /app/data/logs/app.log
```

### `functools.reduce` — Fold

```python
from functools import reduce

# reduce(f, iterable, initial)
# Applies f cumulatively: f(f(f(initial, x0), x1), x2), ...

total = reduce(lambda acc, x: acc + x, [1, 2, 3, 4, 5], 0)   # 15
product = reduce(lambda acc, x: acc * x, [1, 2, 3, 4, 5], 1)  # 120

# In Python, prefer built-ins over reduce when possible:
# sum([1,2,3,4,5])       → more readable than reduce(+, ...)
# max([1,2,3,4,5])       → more readable than reduce(max, ...)
# "".join(["a","b","c"]) → more readable than reduce(+, ...)

# reduce shines when there's no built-in:
# Compose a list of functions
def compose(f, g):
    return lambda x: f(g(x))

pipeline = reduce(compose, [str.strip, str.lower, str.title])
print(pipeline("  hello WORLD  "))  # 'Hello World'
```

### `functools.total_ordering` — Comparison Methods

```python
from functools import total_ordering

@total_ordering
class Student:
    def __init__(self, name, gpa):
        self.name = name
        self.gpa = gpa

    def __eq__(self, other):
        return self.gpa == other.gpa

    def __lt__(self, other):
        return self.gpa < other.gpa

    # @total_ordering fills in: __le__, __gt__, __ge__ automatically

students = [Student("Alice", 3.8), Student("Bob", 3.5), Student("Carol", 3.9)]
print(sorted(students, key=lambda s: s.gpa))  # sorted by gpa
print(max(students).name)   # Carol
```

---

## 11. Decorators — Mechanics

### Must Know

A decorator is a function that **takes a function and returns a function** (or any callable). The `@decorator` syntax is pure syntactic sugar.

```python
@decorator
def func():
    ...

# Is EXACTLY equivalent to:
def func():
    ...
func = decorator(func)
```

### How It Works — Step by Step

```python
import functools
import time

def timer(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)        # call original function
        elapsed = time.perf_counter() - start
        print(f"{func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper

@timer
def slow_sum(n):
    return sum(range(n))

print(slow_sum(10_000_000))
# slow_sum took 0.3421s
# 49999995000000
```

### Decorator with Arguments — Factory Pattern

```python
def retry(max_attempts=3, delay=1.0):
    """Decorator factory — returns a decorator."""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(1, max_attempts + 1):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    if attempt == max_attempts:
                        raise
                    print(f"Attempt {attempt} failed: {e}. Retrying...")
                    time.sleep(delay)
        return wrapper
    return decorator

@retry(max_attempts=3, delay=0.5)
def flaky_api_call():
    import random
    if random.random() < 0.7:
        raise ConnectionError("Timeout")
    return "Success"
```

### Stacking Decorators

```python
def bold(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        return "<b>" + func(*args, **kwargs) + "</b>"
    return wrapper

def italic(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        return "<i>" + func(*args, **kwargs) + "</i>"
    return wrapper

@bold
@italic
def greet(name):
    return f"Hello, {name}"

# Equivalent to: greet = bold(italic(greet))
# Decorators apply bottom-up (innermost first), execute top-down (outermost first)
print(greet("Alice"))   # <b><i>Hello, Alice</i></b>
```

### Class-Based Decorators

```python
class CountCalls:
    def __init__(self, func):
        functools.update_wrapper(self, func)
        self.func = func
        self.count = 0

    def __call__(self, *args, **kwargs):
        self.count += 1
        print(f"Call #{self.count}")
        return self.func(*args, **kwargs)

@CountCalls
def say_hello():
    print("Hello!")

say_hello()   # Call #1 → Hello!
say_hello()   # Call #2 → Hello!
print(say_hello.count)   # 2
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Decorator pattern | AOP (AspectJ), Spring `@Aspect`, dynamic proxy | First-class language feature via `@decorator` |
| `@Override` | Enforces correct override | No equivalent (duck typing) |
| `@Transactional`, `@Cacheable` | Spring annotation + proxy | Pure Python decorator — no framework needed |
| Method interception | `InvocationHandler`, `MethodInterceptor` | Decorator with `*args, **kwargs` |
| Caching | Guava `@CacheResult`, Spring `@Cacheable` | `@functools.lru_cache` |

---

## 12. Higher-Order Functions

### `map`, `filter`, `zip`, `enumerate`

```python
numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

# map — apply function to each element (returns lazy iterator)
doubled = list(map(lambda x: x * 2, numbers))    # [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]
# Prefer list comprehension in Python:
doubled = [x * 2 for x in numbers]               # more Pythonic

# filter — keep elements where function returns True (lazy iterator)
evens = list(filter(lambda x: x % 2 == 0, numbers))   # [2, 4, 6, 8, 10]
# Prefer:
evens = [x for x in numbers if x % 2 == 0]

# zip — pair elements from multiple iterables (stops at shortest)
names = ["Alice", "Bob", "Carol"]
scores = [95, 87, 92]
paired = list(zip(names, scores))   # [('Alice', 95), ('Bob', 87), ('Carol', 92)]
for name, score in zip(names, scores):   # unpack in loop
    print(f"{name}: {score}")

# zip_longest — pads shorter iterables with fillvalue
from itertools import zip_longest
list(zip_longest([1, 2, 3], [4, 5], fillvalue=0))   # [(1,4), (2,5), (3,0)]

# enumerate — get index + value
for i, name in enumerate(names):
    print(f"{i}: {name}")   # 0: Alice, 1: Bob, 2: Carol

for i, name in enumerate(names, start=1):   # start index at 1
    print(f"{i}. {name}")
```

### `any` and `all` — Aggregations

```python
scores = [85, 92, 78, 96, 55]

# all — True only if ALL elements are truthy
print(all(s >= 60 for s in scores))    # False — 55 fails
print(all(s > 0 for s in scores))      # True

# any — True if AT LEAST ONE element is truthy
print(any(s >= 95 for s in scores))    # True — 96 qualifies
print(any(s > 100 for s in scores))    # False

# Short-circuit: any stops on first True; all stops on first False
# Works with generator expressions — no list created in memory
```

### Java Developer Bridge

| Java Stream | Python Equivalent | Notes |
|---|---|---|
| `.map(f)` | `[f(x) for x in lst]` or `map(f, lst)` | Prefer comprehension in Python |
| `.filter(pred)` | `[x for x in lst if pred(x)]` or `filter(pred, lst)` | Prefer comprehension |
| `.reduce(f, identity)` | `functools.reduce(f, lst, identity)` | Use `sum`, `max`, `min` when applicable |
| `.forEach(f)` | `for x in lst: f(x)` | Simple for loop, not functional |
| `.collect(toList())` | No collect needed — comprehension returns list | |
| `.anyMatch(pred)` | `any(pred(x) for x in lst)` | |
| `.allMatch(pred)` | `all(pred(x) for x in lst)` | |
| `.noneMatch(pred)` | `not any(pred(x) for x in lst)` | |
| `.findFirst()` | `next((x for x in lst if pred(x)), None)` | |
| `.count()` | `sum(1 for x in lst if pred(x))` | |
| `.sorted(Comparator)` | `sorted(lst, key=lambda x: ...)` | |
| `.distinct()` | `list(dict.fromkeys(lst))` | Preserves order |
| `.limit(n)` | `lst[:n]` or `itertools.islice(gen, n)` | |
| `.peek(f)` | No direct equivalent — use loop or custom generator | |
| `.flatMap(f)` | `[item for sublist in map(f, lst) for item in sublist]` | |

---

## 13. Java Developer Bridge — Function System Summary

| Concept | Java | Python |
|---|---|---|
| Function definition | Method in a class (static or instance) | `def` anywhere — module, class, inside another function |
| First-class functions | Requires `FunctionalInterface` wrapper | Native — functions are objects |
| Anonymous function | Lambda with single expression | `lambda` with single expression (same limitation!) |
| No-op function | `() -> {}` or empty method | `lambda: None` or `def noop(): pass` |
| Variable arity | `T... args` → array | `*args` → tuple |
| Keyword arguments | No equivalent | `**kwargs` → dict |
| Method references | `String::toUpperCase`, `obj::method` | `str.upper` (unbound) or `obj.method` (bound) |
| Currying / partial application | No built-in | `functools.partial` |
| Memoization | Guava `@CacheResult`, Spring `@Cacheable` | `@functools.lru_cache` |
| Decorator/interceptor | Spring AOP `@Aspect` | Pure Python `@decorator` |
| Closure | Lambda captures effectively-final | Nested `def` — `nonlocal` for mutation |
| Variable capture | Value at creation (effectively final) | Name at call time — late binding! |
| Overloading | Multiple methods same name, different signature | Not supported — use `*args`, default args, or `isinstance` checks |

---

## 14. Hot Interview Q&A

**Q: What is a closure and when does it capture its variables?**  
A: A closure is a function that retains references to variables from its enclosing scope after that scope has finished. Python captures the **variable name** (a reference to the cell object), not the value. The value is looked up each time the closure is called. This causes the late binding trap in loops.

**Q: What is the late binding trap? Give an example and fix.**  
A: [Give the `[lambda: i for i in range(5)]` example returning `[4,4,4,4,4]`. Fix: `lambda i=i: i` forces value capture at definition time via default argument evaluation.]

**Q: What is the difference between `*args` and `**kwargs`?**  
A: `*args` collects extra positional arguments into a tuple. `**kwargs` collects extra keyword arguments into a dict. They are used together to create wrappers that forward all arguments to another function without knowing the signature in advance.

**Q: Why must `functools.wraps` be used inside a decorator?**  
A: Without `@functools.wraps(func)`, the wrapper function replaces the original's metadata (`__name__`, `__doc__`, `__annotations__`). This breaks debugging, logging, pytest test discovery, and anything that inspects the function's name or docstring. `@functools.wraps` copies these attributes from the wrapped function to the wrapper.

**Q: What is the LEGB rule?**  
A: Python resolves names in Local → Enclosing → Global → Built-in order. The first scope where the name is found wins. Assignment in a function creates a local variable for that entire function body — reading the name before the assignment in the same function raises `UnboundLocalError`.

**Q: What is `nonlocal` and when do you need it?**  
A: `nonlocal x` declares that `x` in the current function refers to the variable in the nearest enclosing function's scope. It's needed when you want to **assign** to an enclosing variable. Reading from an enclosing scope works without `nonlocal`.

**Q: What does `@functools.lru_cache` do? What is `maxsize`?**  
A: It memoizes a function's results using a dict-like cache keyed by the arguments. `maxsize` limits cache entries and enables LRU eviction (discards least-recently-used when full). `maxsize=None` disables LRU and gives unlimited caching (same as `@functools.cache` in 3.9+). The function's arguments must be hashable.

**Q: Can you override a function in Python? How does Python handle multiple functions with the same name?**  
A: Python does not support method overloading. Defining a second `def add(...)` at the same scope simply rebinds the name `add` — the first function is lost. To handle multiple call signatures, use default arguments, `*args`/`**kwargs`, or explicit `isinstance` checks inside a single function.

**Q: What is the difference between `sorted(list)` and `list.sort()`?**  
A: `sorted()` returns a new sorted list and can take any iterable. `list.sort()` sorts in-place and returns `None`. Both accept `key` and `reverse` parameters.

**Q: What does `any(pred(x) for x in items)` short-circuit mean?**  
A: `any()` stops iterating as soon as it finds the first truthy value. `all()` stops on the first falsy value. Using a generator expression (not a list) ensures no unnecessary evaluation — critical for expensive predicates or infinite iterables.

---

## 15. Final Revision Checklist

### Functions as First-Class Objects

- [ ] I can assign a function to a variable, pass it as an argument, and return it from a function
- [ ] I understand why `str.upper` is an unbound method that takes `self` as first arg
- [ ] I know `type(lambda: None)` is `<class 'function'>` — lambda and def produce the same type

### LEGB and Scope

- [ ] I can trace LEGB resolution for a nested function example
- [ ] I know why `print(x); x = 5` in a function raises `UnboundLocalError`
- [ ] I understand `global` vs `nonlocal` and when each is needed

### Closures and Late Binding

- [ ] I can explain the late binding trap in a loop: why `[lambda: i for i in range(5)]` returns `[4,4,4,4,4]`
- [ ] I know all three fixes: default arg, factory function, `functools.partial`
- [ ] I can inspect a closure with `func.__closure__` and `func.__code__.co_freevars`

### `*args` and `**kwargs`

- [ ] I know `*args` is a tuple and `**kwargs` is a dict
- [ ] I can write the correct parameter order: `positional, *args, keyword_only, **kwargs`
- [ ] I can use `*list` and `**dict` to unpack into function calls
- [ ] I understand the pass-through wrapper pattern used in decorators

### Keyword-Only and Positional-Only

- [ ] I know `def f(a, *, b)` forces `b` to be keyword-only
- [ ] I know `def f(a, /, b)` forces `a` to be positional-only (Python 3.8+)
- [ ] I understand why keyword-only args improve API clarity and stability

### `functools` and Decorators

- [ ] I can write a decorator that preserves function metadata with `@functools.wraps`
- [ ] I can apply `@functools.lru_cache` and explain what `maxsize` controls
- [ ] I can use `functools.partial` to create a specialized version of a function
- [ ] I can write a decorator factory (decorator with arguments)
- [ ] I know stacking decorators applies bottom-up (innermost first)

### Java Developer Reminders

- [ ] Python has no method overloading — same name = rebinding
- [ ] Python closures capture names (late binding) vs Java captures values (effectively final)
- [ ] `functools.partial` is the Python way to do what Java does with method references + partial application
- [ ] `@functools.lru_cache` replaces Guava/Spring caching for pure functions
- [ ] `any()` / `all()` replace Java `Stream.anyMatch()` / `Stream.allMatch()`

---

*File 4 of 7 — Group 1: Starter Path*  
*Next: Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md*
