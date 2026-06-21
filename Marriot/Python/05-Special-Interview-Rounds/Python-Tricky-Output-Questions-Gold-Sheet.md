# Python Tricky Output Questions — Gold Sheet

> **Track File #23 of 31 · Group 5: Special Interview Rounds**
> For: Java developer | Level: MAANG tricky-output round | Mode: predict-the-output drills

---

## 1. Interview Priority Meter

| Topic | MAANG Frequency | Java Dev Trap Level |
|---|---|---|
| Mutable default argument output | ★★★★★ | HIGH |
| Late binding in closures | ★★★★★ | HIGH |
| `is` vs `==` identity surprises | ★★★★★ | HIGH — Java `==` is identity |
| `UnboundLocalError` / `global` | ★★★★★ | HIGH |
| Class vs instance attribute mutation | ★★★★★ | HIGH |
| Comprehension scope (Python 3) | ★★★★☆ | MEDIUM |
| `+=` on mutable vs immutable | ★★★★☆ | HIGH |
| Exception `else` / `finally` order | ★★★★☆ | MEDIUM |
| Decorator execution order | ★★★★☆ | MEDIUM |
| Default arg evaluated at `def` time | ★★★★★ | HIGH |
| Augmented assign on class attribute | ★★★★☆ | HIGH |
| `__init__` vs `__new__` | ★★★☆☆ | MEDIUM |
| Generator expression exhaustion | ★★★★★ | HIGH |
| `nonlocal` vs `global` | ★★★★☆ | HIGH |

---

## 2. How to Use This Sheet

Each question follows this format:

```
Q: What does this print / what happens?
[Code]
A: [Exact output]
Why: [Root cause in one sentence]
Java Bridge: [What would Java do / equivalent trap]
```

Work through each question before reading the answer. Mark ones you get wrong — those are your weak spots.

---

## 3. Mutable Default Argument Round

### Q3-A
```python
def push(item, stack=[]):
    stack.append(item)
    return stack

print(push(1))
print(push(2))
print(push(3))
```

**A:**
```
[1]
[1, 2]
[1, 2, 3]
```
**Why:** The default `stack=[]` is evaluated once at `def` time and stored in `push.__defaults__`. All calls without an explicit `stack` argument share the same list.

**Java Bridge:** Java cannot have mutable default expressions — there are no parameter defaults with expressions. This trap is uniquely Python.

---

### Q3-B
```python
def add(val, container={}):
    container[val] = val * 2
    return container

r1 = add("a")
r2 = add("b")
r3 = add("c", {})

print(r1)
print(r2)
print(r3)
```

**A:**
```
{'a': 'aa', 'b': 'bb'}
{'a': 'aa', 'b': 'bb'}
{'c': 'cc'}
```
**Why:** `r1` and `r2` share the default dict (accumulated across calls). `r3` passes an explicit `{}` so it gets its own fresh dict. `r1` and `r2` are references to the SAME dict, so they both show the final accumulated state.

---

### Q3-C
```python
def make_list(n, lst=[]):
    lst.append(n)
    return lst

a = make_list(1)
b = make_list(2)
print(a is b)
print(a)
```

**A:**
```
True
[1, 2]
```
**Why:** `a` and `b` are the same object — both are the shared default list. `a is b` is `True`. Printing either shows `[1, 2]`.

---

## 4. Late Binding Closure Round

### Q4-A
```python
multipliers = [lambda x: x * i for i in range(4)]
print(multipliers[0](10))
print(multipliers[2](10))
print(multipliers[3](10))
```

**A:**
```
30
30
30
```
**Why:** All lambdas capture the variable `i` by reference. After the comprehension, `i = 3`. Every lambda looks up `i` at call time and gets `3`. So `10 * 3 = 30` for all of them.

**Java Bridge:** Java lambdas require captured variables to be effectively final — the compiler catches this. Python silently creates the bug.

---

### Q4-B
```python
funcs = []
for i in range(3):
    def f(n=i):   # default arg captures current value of i
        return n
    funcs.append(f)

print([f() for f in funcs])
```

**A:**
```
[0, 1, 2]
```
**Why:** `n=i` is a default argument — it is evaluated at the time the `def` statement executes. Each iteration captures the current value of `i` into `n`. This is the standard fix for the late binding trap.

---

### Q4-C
```python
x = 10

def outer():
    x = 20
    def inner():
        return x
    x = 30         # reassigned AFTER inner defined, BEFORE inner called
    return inner

print(outer()())
```

**A:**
```
30
```
**Why:** `inner` captures the variable `x` from `outer`'s scope, not its value at the time `inner` was defined. By the time `inner()` is called, `x` has been reassigned to `30` in `outer`'s scope.

---

### Q4-D
```python
def counter():
    count = 0
    def inc():
        count += 1     # UnboundLocalError!
        return count
    return inc

c = counter()
print(c())
```

**A:**
```
UnboundLocalError: local variable 'count' referenced before assignment
```
**Why:** `count += 1` expands to `count = count + 1`. The assignment makes Python treat `count` as a local variable in `inc`. Reading it before the assignment raises `UnboundLocalError`. Fix: add `nonlocal count` inside `inc`.

---

## 5. `is` vs `==` Identity Round

### Q5-A
```python
a = 256
b = 256
c = 257
d = 257

print(a is b)
print(c is d)
```

**A:**
```
True
False
```
**Why:** CPython caches small integers from -5 to 256. `256` is within the cache — `a` and `b` are the same object. `257` is outside the cache — each assignment creates a new object.

**Java Bridge:** Java autoboxing caches `Integer` values -128 to 127. Above 127, `Integer.valueOf(257) == Integer.valueOf(257)` is `False` — the same trap, different range.

---

### Q5-B
```python
x = "hello"
y = "hello"
z = "hel" + "lo"
w = "hel"
w += "lo"

print(x is y)
print(x is z)
print(x is w)
```

**A:**
```
True
True
False
```
**Why:** CPython interns string literals and compile-time constants. `"hello"` and `"hel" + "lo"` (folded at compile time) are interned — same object. `w = "hel"` then `w += "lo"` creates a new string at runtime — not interned, different object.

**Never use `is` to compare string values. Always use `==`.**

---

### Q5-C
```python
print(None == False)
print(None is False)
print(0 == False)
print(0 is False)
```

**A:**
```
False
False
True
False
```
**Why:** `None == False` is `False` — they are distinct objects with no equality. `0 == False` is `True` because `bool` is a subclass of `int` and `False == 0` numerically. But `0 is False` is `False` — they are different objects.

**Java Bridge:** Java has no `None`/`null` equality to `False`. In Java, `null == false` is a compile error (incompatible types). Python's `bool` subclassing `int` is unique.

---

### Q5-D
```python
a = []
b = []
c = a

print(a == b)
print(a is b)
print(a is c)

c.append(1)
print(a)
```

**A:**
```
True
False
True
[1]
```
**Why:** `a == b` — both are empty lists, value equal. `a is b` — different list objects. `a is c` — same object. Appending to `c` mutates the same object `a` points to.

---

## 6. Scoping and `global` / `nonlocal` Round

### Q6-A
```python
x = "global"

def foo():
    print(x)

def bar():
    print(x)   # UnboundLocalError!
    x = "local"

foo()
bar()
```

**A:**
```
global
UnboundLocalError: local variable 'x' referenced before assignment
```
**Why:** In `foo`, there is no assignment to `x`, so Python looks it up globally — finds `"global"`. In `bar`, the assignment `x = "local"` makes Python treat `x` as local throughout the entire function. Reading `x` before the assignment raises `UnboundLocalError`.

---

### Q6-B
```python
total = 100

def deduct(amount):
    global total
    total -= amount
    return total

print(deduct(30))
print(deduct(20))
print(total)
```

**A:**
```
70
50
50
```
**Why:** `global total` allows the function to rebind the module-level `total`. Each call modifies the global variable.

---

### Q6-C
```python
def outer():
    x = 1
    def middle():
        x = 2
        def inner():
            nonlocal x
            x += 1
            return x
        return inner
    return middle

f = outer()
g = f()
print(g())
print(g())
```

**A:**
```
3
4
```
**Why:** `nonlocal x` in `inner` refers to `middle`'s `x` (value `2`). First call: `2 + 1 = 3`. Second call: `3 + 1 = 4`. The enclosing `x` persists between calls because `g` (which is `inner`) holds a reference to `middle`'s local scope via its closure.

---

## 7. Class vs Instance Variable Round

### Q7-A
```python
class Dog:
    tricks = []

    def add_trick(self, trick):
        self.tricks.append(trick)

d1 = Dog()
d2 = Dog()
d1.add_trick("roll over")
d2.add_trick("play dead")

print(d1.tricks)
print(d2.tricks)
print(d1.tricks is d2.tricks)
```

**A:**
```
['roll over', 'play dead']
['roll over', 'play dead']
True
```
**Why:** `tricks` is a class variable. `self.tricks.append()` does NOT create an instance attribute — it mutates the class-level list through the instance's reference. Both `d1.tricks` and `d2.tricks` are the same object.

**Java Bridge:** Equivalent to a `static ArrayList` field in Java. Mutating it affects all instances.

---

### Q7-B
```python
class Counter:
    count = 0

    def increment(self):
        self.count += 1   # creates an instance variable!

c1 = Counter()
c2 = Counter()
c1.increment()
c1.increment()
c2.increment()

print(c1.count)
print(c2.count)
print(Counter.count)
```

**A:**
```
2
1
0
```
**Why:** `self.count += 1` expands to `self.count = self.count + 1`. The READ (`self.count`) on the right looks up the class variable (`0`). The WRITE (`self.count =`) creates a new **instance attribute** that shadows the class variable. Each instance gets its own `count`. The class variable stays at `0`.

**This is the opposite of Q7-A** — mutation via `append` modifies in-place (no assignment, no instance var created); `+=` on an immutable int creates an instance variable.

---

### Q7-C
```python
class Config:
    values = {"debug": False}

c1 = Config()
c2 = Config()
c1.values["debug"] = True   # mutates the shared dict in-place

print(c2.values["debug"])
print(Config.values["debug"])
```

**A:**
```
True
True
```
**Why:** `c1.values["debug"] = True` mutates the dict in-place — no assignment to `c1.values` itself, so no instance attribute is created. The class variable dict is mutated, and all instances see the change.

---

## 8. `+=` on Mutable vs Immutable Round

### Q8-A
```python
a = [1, 2, 3]
b = a

a += [4, 5]

print(a)
print(b)
print(a is b)
```

**A:**
```
[1, 2, 3, 4, 5]
[1, 2, 3, 4, 5]
True
```
**Why:** For lists, `+=` calls `__iadd__` which calls `extend` in-place — no new object is created. `a` still points to the same list, and `b` is also that same list, so both show the extended version.

---

### Q8-B
```python
a = (1, 2, 3)
b = a

a += (4, 5)

print(a)
print(b)
print(a is b)
```

**A:**
```
(1, 2, 3, 4, 5)
(1, 2, 3)
False
```
**Why:** Tuples are immutable. `a += (4, 5)` creates a **new tuple** and rebinds `a` to it. `b` still points to the original `(1, 2, 3)`. `a is b` is `False`.

**Contrast:** List `+=` is in-place (identity preserved); tuple `+=` creates new object (identity changes). This is the mutable vs immutable `+=` difference.

---

### Q8-C — The Tuple With a Mutable Element

```python
t = ([1, 2], [3, 4])

try:
    t[0] += [5, 6]
except TypeError as e:
    print(f"Error: {e}")

print(t)
```

**A:**
```
Error: 'tuple' object does not support item assignment
[THEN]
([1, 2, 5, 6], [3, 4])
```
**Why:** `t[0] += [5, 6]` expands to:
1. `temp = t[0].__iadd__([5, 6])` — mutates the inner list successfully → list is now `[1, 2, 5, 6]`
2. `t[0] = temp` — tries to assign to tuple index → `TypeError`

**Both happen.** The list is mutated AND the exception is raised. This is a famous CPython implementation artifact.

---

## 9. Comprehension Scope Round

### Q9-A
```python
x = 10

squares = [x**2 for x in range(5)]

print(x)
print(squares)
```

**A:**
```
10
[0, 1, 4, 9, 16]
```
**Why:** In Python 3, list comprehension variables are scoped to the comprehension — they do NOT leak into the enclosing scope. `x` remains `10`. (In Python 2, `x` would have been `4` — the loop variable leaked.)

**Java Bridge:** Java enhanced-for loop variables are also scoped to the loop — same behavior as Python 3.

---

### Q9-B
```python
result = [y := x**2 for x in range(5)]
print(y)
print(result)
```

**A:**
```
16
[0, 1, 4, 9, 16]
```
**Why:** The walrus operator `:=` (Python 3.8+) assigns to the **enclosing** scope, not the comprehension scope. After the comprehension, `y` is the last assigned value: `4**2 = 16`.

---

### Q9-C
```python
gen = (i * 2 for i in range(5))
lst = [i * 3 for i in range(5)]

print(type(gen))
print(type(lst))

total = gen + lst
```

**A:**
```
<class 'generator'>
<class 'list'>
TypeError: unsupported operand type(s) for +: 'generator' and 'list'
```
**Why:** `(...)` creates a generator object; `[...]` creates a list. You cannot add a generator and a list with `+`. To combine: `list(gen) + lst`.

---

### Q9-D
```python
gen = (x**2 for x in range(5))
print(list(gen))
print(list(gen))
```

**A:**
```
[0, 1, 4, 9, 16]
[]
```
**Why:** Generators are single-use. After the first `list(gen)` exhausts the generator, the second call returns an empty list. The generator object is not reset.

**Java Bridge:** Java `Stream` is also single-use — calling a terminal operation twice throws `IllegalStateException`. Python silently returns `[]`.

---

## 10. Augmented Assignment on Class Attributes Round

### Q10-A
```python
class A:
    x = []
    y = 0

a = A()

a.x.append(1)   # in-place mutation of class-level list
a.y += 1        # creates instance attribute y

b = A()
print(b.x)
print(b.y)
print(A.x)
print(A.y)
```

**A:**
```
[1]
0
[1]
0
```
**Why:**
- `a.x.append(1)` — mutates the class-level list in-place. `b.x` and `A.x` all see `[1]`.
- `a.y += 1` — creates a new instance attribute `a.y = 1`. `b.y` still reads class attribute `0`. `A.y` is `0`.

---

## 11. Exception Handling Round

### Q11-A
```python
def risky():
    try:
        return "try"
    finally:
        return "finally"

print(risky())
```

**A:**
```
finally
```
**Why:** A `return` in `finally` overrides the `return` in `try`. `finally` always executes and its `return` takes precedence.

**Java Bridge:** Same in Java — `return` in a `finally` block overrides the `try` block `return`. Both languages treat this as a code smell.

---

### Q11-B
```python
def compute():
    try:
        result = 10 / 0
    except ZeroDivisionError:
        print("caught")
        return "error"
    else:
        print("no exception")
        return "ok"
    finally:
        print("always")

print(compute())
```

**A:**
```
caught
always
error
```
**Why:** Exception raised → `except` runs → prints "caught" → `return "error"` is queued → `finally` runs → prints "always" → "error" is returned. The `else` block runs only if NO exception was raised — skipped here.

---

### Q11-C
```python
def compute():
    try:
        result = 10 / 2
    except ZeroDivisionError:
        print("caught")
        return "error"
    else:
        print("no exception")
        return "ok"
    finally:
        print("always")

print(compute())
```

**A:**
```
no exception
always
ok
```
**Why:** No exception — `else` runs, prints "no exception", queues `return "ok"`. `finally` runs, prints "always". Returns `"ok"`.

---

### Q11-D
```python
try:
    x = int("bad")
except ValueError as e:
    err = e

print(err)
print(e)
```

**A:**
```
invalid literal for int() with base 10: 'bad'
NameError: name 'e' is not defined
```
**Why:** The exception variable `e` from `except ValueError as e` is **deleted** from the local namespace after the `except` block ends (Python 3 scoping rule). However, `err = e` was assigned inside the block — `err` persists. Accessing `e` outside raises `NameError`.

**Java Bridge:** In Java, `catch (ValueError e)` — `e` is scoped to the catch block. Same behavior, but Java's is a compilation error while Python's is a runtime `NameError`.

---

## 12. Decorator Execution Order Round

### Q12-A
```python
def decorator_a(func):
    print("A applied")
    def wrapper(*args):
        print("A before")
        result = func(*args)
        print("A after")
        return result
    return wrapper

def decorator_b(func):
    print("B applied")
    def wrapper(*args):
        print("B before")
        result = func(*args)
        print("B after")
        return result
    return wrapper

@decorator_a
@decorator_b
def greet(name):
    print(f"Hello, {name}")

greet("Alice")
```

**A:**
```
B applied
A applied
A before
B before
Hello, Alice
B after
A after
```
**Why:**
- Decorators apply **bottom-up** at definition time: `decorator_b` is applied first ("B applied"), then `decorator_a` ("A applied").
- Wrappers execute **top-down** at call time: `A before` → `B before` → `greet` → `B after` → `A after`.

**Rule:** Bottom decorator is innermost; top decorator is outermost. Think of it as `greet = decorator_a(decorator_b(greet))`.

---

### Q12-B
```python
def log(func):
    print(f"Registering {func.__name__}")
    return func   # returns original function unchanged

@log
def add(a, b):
    return a + b

@log
def sub(a, b):
    return a - b

print(add(1, 2))
```

**A:**
```
Registering add
Registering sub
3
```
**Why:** The print in `log` runs at **decoration time** (when `@log` is applied, i.e., at import/class-definition time), not at call time. Both registrations happen before `add(1, 2)` is called.

---

## 13. `*args` and `**kwargs` Unpacking Round

### Q13-A
```python
def f(a, b, c):
    return a + b + c

args = (1, 2)
kwargs = {"c": 3}

print(f(*args, **kwargs))
print(f(1, *args))
```

**A:**
```
6
TypeError: f() got multiple values for argument 'a'
```
**Why:** `f(*args, **kwargs)` → `f(1, 2, c=3)` → `6`. `f(1, *args)` → `f(1, 1, 2)` → `a=1` provided twice (once positionally, once from `*args`) → `TypeError`.

---

### Q13-B
```python
def show(*args, **kwargs):
    print(args)
    print(kwargs)

show(1, 2, x=3, y=4)
show(*(1, 2), **{"x": 3})
```

**A:**
```
(1, 2)
{'x': 3, 'y': 4}
(1, 2)
{'x': 3}
```

---

### Q13-C
```python
def greet(name, /, greeting="Hello"):
    return f"{greeting}, {name}!"

print(greet("Alice"))
print(greet("Bob", greeting="Hi"))
print(greet(name="Carol"))
```

**A:**
```
Hello, Alice!
Hi, Bob!
TypeError: greet() got some positional-only arguments passed as keyword arguments: 'name'
```
**Why:** `/` in the parameter list marks everything before it as **positional-only**. `name` cannot be passed as a keyword argument.

**Java Bridge:** Java has no positional-only parameters. Python 3.8+ added `/` to match C extension conventions and allow API flexibility.

---

## 14. Integer and Boolean Arithmetic Round

### Q14-A
```python
print(True + True)
print(True * 10)
print(False + 1)
print(isinstance(True, int))
print(True == 1 == 1.0)
```

**A:**
```
2
10
1
True
True
```
**Why:** `bool` is a subclass of `int` in Python. `True == 1` and `False == 0` numerically. Chained comparisons work left-to-right: `True == 1` is `True`, `1 == 1.0` is `True`, both are `True` so the whole expression is `True`.

---

### Q14-B
```python
print(10 / 2)
print(10 // 3)
print(-10 // 3)
print(10 % 3)
print(-10 % 3)
```

**A:**
```
5.0
3
-4
1
2
```
**Why:**
- `10 / 2 = 5.0` — always float in Python 3
- `10 // 3 = 3` — floor division
- `-10 // 3 = -4` — floor division rounds toward negative infinity (`-3.33` → `-4`)
- `-10 % 3 = 2` — Python modulo satisfies `a == (a//b)*b + a%b`: `-10 == (-4)*3 + 2`

**Java Bridge:** Java `-10 / 3 = -3` (truncates toward zero). Python `-10 // 3 = -4` (floors toward negative infinity). The sign of the result differs for negative numbers!

---

### Q14-C
```python
x = None
print(x or "default")
print(x and "something")
print(not x)

y = 0
print(y or "fallback")
print(bool(y))
```

**A:**
```
default
None
True
fallback
False
```
**Why:** `or` returns the first truthy value or the last value. `and` returns the first falsy value or the last value. `None`, `0`, `""`, `[]`, `{}` are all falsy.

---

## 15. Inheritance and `super()` Round

### Q15-A
```python
class A:
    def method(self):
        print("A")

class B(A):
    def method(self):
        super().method()
        print("B")

class C(A):
    def method(self):
        super().method()
        print("C")

class D(B, C):
    def method(self):
        super().method()
        print("D")

D().method()
```

**A:**
```
A
C
B
D
```
**Why:** MRO for D: `D → B → C → A`. `super()` follows MRO:
- `D.method` calls `B.method`
- `B.method` calls `C.method` (next in MRO after B)
- `C.method` calls `A.method`
- `A.method` prints "A"
- Unwinds: prints "C", then "B", then "D"

---

### Q15-B
```python
class Base:
    def __init__(self):
        print("Base __init__")
        self.x = 10

class Child(Base):
    def __init__(self):
        self.y = 20
        super().__init__()
        print(f"Child: x={self.x}, y={self.y}")

c = Child()
```

**A:**
```
Base __init__
Child: x=10, y=20
```
**Why:** `Child.__init__` sets `self.y = 20` first, then calls `super().__init__()` which sets `self.x = 10`. By the time the print runs, both are set.

---

## 16. String Formatting and Comparison Round

### Q16-A
```python
name = "Alice"
score = 95.5

s1 = "Name: %s, Score: %.1f" % (name, score)
s2 = "Name: {}, Score: {:.1f}".format(name, score)
s3 = f"Name: {name}, Score: {score:.1f}"

print(s1 == s2 == s3)
print(s1)
```

**A:**
```
True
Name: Alice, Score: 95.5
```

---

### Q16-B
```python
words = ["banana", "apple", "cherry", "date"]

# sort alphabetically
print(sorted(words))

# sort by length, then alphabetically
print(sorted(words, key=lambda w: (len(w), w)))
```

**A:**
```
['apple', 'banana', 'cherry', 'date']
['date', 'apple', 'banana', 'cherry']
```
**Why:** Length first: date(4), apple(5), banana(6), cherry(6). For ties, alphabetical: banana < cherry.

---

## 17. `__repr__` vs `__str__` Round

### Q17-A
```python
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __repr__(self):
        return f"Point({self.x}, {self.y})"

    def __str__(self):
        return f"({self.x}, {self.y})"

p = Point(1, 2)
print(p)
print(repr(p))
print(f"{p}")
print(f"{p!r}")
print([p])
```

**A:**
```
(1, 2)
Point(1, 2)
(1, 2)
Point(1, 2)
[Point(1, 2)]
```
**Why:**
- `print(p)` → `str(p)` → `__str__` → `(1, 2)`
- `repr(p)` → `__repr__` → `Point(1, 2)`
- `f"{p}"` → `str(p)` → `(1, 2)`
- `f"{p!r}"` → `repr(p)` → `Point(1, 2)`
- Inside a list, Python uses `__repr__` for elements → `[Point(1, 2)]`

---

## 18. `None` and Empty Falsy Values Round

### Q18-A
```python
values = [0, "", [], None, False, 0.0, {}, set()]
truthy = [v for v in values if v]
falsy = [v for v in values if not v]

print(len(truthy))
print(len(falsy))
```

**A:**
```
0
8
```
**Why:** All of `0`, `""`, `[]`, `None`, `False`, `0.0`, `{}`, `set()` are falsy. None of them pass `if v`.

---

### Q18-B
```python
def first_truthy(*args):
    return next((a for a in args if a), None)

print(first_truthy(0, "", None, "hello", "world"))
print(first_truthy(0, False, []))
```

**A:**
```
hello
None
```
**Why:** `next(gen, default)` returns the first item from the generator or the default if exhausted. First call: `"hello"` is the first truthy value. Second call: all are falsy — generator exhausted — returns `None`.

---

## 19. Java Developer Bridge — Full Output Trap Map

| Trap | Java Behavior | Python Behavior |
|---|---|---|
| Mutable default `def f(x=[])` | Impossible | Shared across calls — silent accumulation |
| Closure `lambda: i` in loop | Effectively final — compile error | Late binding — all see final loop value |
| `257 is 257` | N/A (`==` for values) | May be `False` — CPython int cache ends at 256 |
| `True + True` | `true + true` compile error | `2` — bool is int subclass |
| `-10 // 3` | `-3` (truncate toward zero) | `-4` (floor toward −∞) |
| `list += [x]` | new list returned | in-place extend, same object |
| `tuple += (x,)` | N/A | new tuple, rebinds variable |
| Comprehension loop var | Scoped to loop | Scoped to comprehension (Python 3) |
| `except E as e` after block | `e` is scoped to catch block | `e` deleted after block — `NameError` |
| `finally return` | overrides try return | Same — overrides try return |
| Decorator order `@A @B` | N/A | B applied first; A wraps B |
| `self.x += 1` on class attr | N/A | Creates instance attr; class attr unchanged |
| `self.x.append()` on class list | N/A | Mutates class attr; no instance attr created |
| Generator exhaustion `list(g)` twice | `IllegalStateException` | Empty list silently |
| `e` after `except ... as e:` | Scoped to catch | Deleted from namespace — `NameError` |

---

## 20. Hot Interview Q&A

**Q1: What prints when you call a lambda defined inside a loop, all after the loop ends?**
> All lambdas print the same value — the final value of the loop variable. Python closures capture variables by reference, not value. Fix: use a default argument `lambda i=i: i` to capture the current value at definition time.

**Q2: What is the difference between `list += [x]` and `tuple += (x,)` in terms of identity?**
> List `+=` calls `__iadd__` which extends in-place — the variable still points to the same list object (`is` remains `True`). Tuple `+=` creates a new tuple (tuples are immutable) and rebinds the variable — `is` becomes `False`.

**Q3: Why does `except ValueError as e` make `e` unavailable after the block?**
> Python 3 deletes the exception variable from the local namespace at the end of the `except` block to break reference cycles (exception objects hold tracebacks which can hold the entire frame). If you need the exception outside, assign it to a separate variable: `err = e` inside the block.

**Q4: What is the execution order when two decorators are stacked?**
> Decorators apply bottom-up at decoration time: the bottom decorator runs first, wrapping the function. Then the top decorator wraps the result. At call time, wrappers execute top-down: the outermost wrapper (top decorator) runs first, delegating to the inner wrapper (bottom decorator).

**Q5: What is the difference between `self.items.append(x)` and `self.items += [x]` on a class-level list?**
> Both mutate in-place for lists — no new list created. However, `self.items.append(x)` always mutates the class-level list if no instance-level `items` exists. `self.items += [x]` also calls `__iadd__` in-place for lists, BUT Python then executes an assignment `self.items = result`, which creates an instance attribute. The behavior differs for the second `+=` call if the instance attribute now shadows the class attribute.

**Q6: In Python 3, does a list comprehension's loop variable leak into the enclosing scope?**
> No. In Python 3, the loop variable in a list comprehension is scoped to the comprehension. The enclosing scope variable with the same name is unchanged. (In Python 2, it did leak — this was a known incompatibility.)

**Q7: What happens when you access an exception variable after the `except` block in Python 3?**
> The variable is deleted. `except Exception as e:` deletes `e` when the block exits. Accessing `e` after raises `NameError: name 'e' is not defined`. Assign to a second variable inside the block if you need it later.

---

## 21. Final Revision Checklist

- [ ] Can predict output of mutable default accumulation across 3 calls
- [ ] Can explain why all loop-lambda closures return the same value
- [ ] Can fix late binding with default argument and explain why it works
- [ ] Can explain CPython integer cache range and why `257 is 257` may be `False`
- [ ] Can explain why `False + 1 = 1` and `True * 10 = 10`
- [ ] Can distinguish `-10 // 3 = -4` (Python) vs `-10 / 3 = -3` (Java)
- [ ] Can predict `list +=` keeps identity but `tuple +=` creates new object
- [ ] Can explain the tuple-with-mutable-element `+=` TypeError + mutation both occurring
- [ ] Can explain why comprehension loop variable does NOT leak in Python 3
- [ ] Can predict walrus operator `:=` leaks to enclosing scope
- [ ] Can explain the class-level list `append` vs `+=` instance attribute difference
- [ ] Can predict `finally return` overrides `try return`
- [ ] Can predict `except … else` block only runs when no exception is raised
- [ ] Can explain `except E as e` variable deletion after block ends
- [ ] Can predict decorator application order (bottom-up) vs call order (top-down)
- [ ] Can predict MRO output for diamond inheritance with `super()`
- [ ] Can explain `print(p)` vs `repr(p)` vs `[p]` using `__str__` vs `__repr__`
- [ ] Can list all 8 falsy values without hesitation
