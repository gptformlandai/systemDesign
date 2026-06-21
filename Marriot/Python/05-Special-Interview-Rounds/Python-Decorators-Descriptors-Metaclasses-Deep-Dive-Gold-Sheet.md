# Python Decorators, Descriptors & Metaclasses — Deep Dive Gold Sheet

> **Track File #24 of 31 · Group 5: Special Interview Rounds**
> For: Java developer | Level: MAANG internals depth | Mode: framework mechanics + design pattern drills

---

## 1. Interview Priority Meter

| Topic | MAANG Frequency | Java Dev Trap Level |
|---|---|---|
| Function decorator internals | ★★★★★ | HIGH — no Java equivalent |
| `functools.wraps` and introspection | ★★★★★ | HIGH |
| Decorator factory (decorator with args) | ★★★★★ | HIGH |
| Class decorator | ★★★★☆ | MEDIUM |
| `@property` getter/setter/deleter | ★★★★★ | HIGH — maps to Java getter/setter but cleaner |
| Descriptor protocol `__get__`/`__set__` | ★★★★★ | HIGH — no Java equivalent |
| Data vs non-data descriptors | ★★★★☆ | HIGH |
| Metaclass `type` and `__new__` | ★★★★☆ | HIGH — no Java equivalent |
| `__init_subclass__` | ★★★★☆ | MEDIUM |
| Registry pattern with metaclass | ★★★★☆ | MEDIUM |
| How `@dataclass` works internally | ★★★★☆ | MEDIUM |
| `ABCMeta` and abstract methods | ★★★★☆ | MEDIUM — maps to Java interface |

---

## 2. Decorator Internals

### 2-A — What a Decorator Actually Is

A decorator is syntactic sugar for a higher-order function call:

```python
# These two are exactly equivalent
@my_decorator
def greet(name):
    return f"Hello, {name}"

# Same as:
def greet(name):
    return f"Hello, {name}"
greet = my_decorator(greet)
```

**A decorator is a callable that takes a function and returns a replacement callable.**

---

### 2-B — Basic Decorator Anatomy

```python
def timer(func):
    """Measures execution time of func."""
    import time
    import functools

    @functools.wraps(func)          # copies __name__, __doc__, __wrapped__
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"{func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper

@timer
def compute(n):
    """Compute sum of range."""
    return sum(range(n))

compute(1_000_000)
print(compute.__name__)   # "compute" — not "wrapper" (thanks to wraps)
print(compute.__doc__)    # "Compute sum of range."
```

**Without `@functools.wraps`:**
- `compute.__name__` would be `"wrapper"` — breaks logging, introspection, pytest
- `compute.__doc__` would be the wrapper's docstring
- `compute.__wrapped__` would be missing — `inspect.signature()` would show wrapper's signature

---

### 2-C — Why `*args, **kwargs` in Every Wrapper

```python
# Wrong — breaks if greet takes any argument
def log(func):
    def wrapper():          # only works for zero-arg functions!
        print("calling")
        return func()
    return wrapper

# Correct — transparent pass-through
def log(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):   # capture all positional and keyword args
        print(f"calling {func.__name__}")
        return func(*args, **kwargs)
    return wrapper
```

---

### 2-D — Decorator Factory (Decorator with Arguments)

**Interviewer:** "How do you write a decorator that accepts parameters like `@retry(max_attempts=3)`?"

```python
import functools
import time

def retry(max_attempts: int = 3, delay: float = 0.5, exceptions=(Exception,)):
    """Decorator factory — returns the actual decorator."""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            last_error = None
            for attempt in range(1, max_attempts + 1):
                try:
                    return func(*args, **kwargs)
                except exceptions as e:
                    last_error = e
                    if attempt < max_attempts:
                        print(f"Attempt {attempt} failed: {e}. Retrying in {delay}s...")
                        time.sleep(delay)
            raise last_error
        return wrapper
    return decorator

@retry(max_attempts=3, delay=0.1, exceptions=(ConnectionError,))
def fetch_data(url: str) -> dict:
    # simulated unstable call
    import random
    if random.random() < 0.7:
        raise ConnectionError("Network error")
    return {"data": "ok"}
```

**Three-layer structure:**
1. `retry(max_attempts=3)` — called at decoration time, returns `decorator`
2. `decorator(func)` — called with the function, returns `wrapper`
3. `wrapper(*args, **kwargs)` — called at function call time

**Java Bridge:** Java annotations like `@Retryable(maxAttempts=3)` from Spring are processed by AOP proxies at runtime — similar effect but very different mechanism. Python decorators are pure Python — no framework required.

---

## 3. Stacking Decorators

### 3-A — Execution Order

```python
import functools

def bold(func):
    @functools.wraps(func)
    def wrapper(*a, **kw):
        return f"<b>{func(*a, **kw)}</b>"
    return wrapper

def italic(func):
    @functools.wraps(func)
    def wrapper(*a, **kw):
        return f"<i>{func(*a, **kw)}</i>"
    return wrapper

@bold
@italic
def greet(name):
    return f"Hello, {name}"

print(greet("Alice"))
# <b><i>Hello, Alice</i></b>

# Equivalent to: greet = bold(italic(greet))
# Call chain: bold_wrapper → italic_wrapper → original greet
```

**Rule:** Bottom decorator applied first (innermost). Top decorator applied last (outermost). At call time, outermost runs first.

---

### 3-B — Decorator Preserving State

```python
import functools

def call_counter(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        wrapper.call_count += 1
        return func(*args, **kwargs)
    wrapper.call_count = 0   # attribute on the wrapper function object
    return wrapper

@call_counter
def process(x):
    return x * 2

process(1)
process(2)
process(3)
print(process.call_count)   # 3
print(process.__name__)     # "process" — wraps preserves name
```

**Attaching state to the wrapper** is cleaner than using `nonlocal`. The wrapper function object is the natural place to store call-level metadata.

---

## 4. Class Decorators

### 4-A — Class Decorator Basics

A class decorator receives the class itself as the argument:

```python
def singleton(cls):
    """Class decorator that enforces single instance."""
    instances = {}
    @functools.wraps(cls, updated=[])
    def get_instance(*args, **kwargs):
        if cls not in instances:
            instances[cls] = cls(*args, **kwargs)
        return instances[cls]
    return get_instance

@singleton
class DatabaseConnection:
    def __init__(self, url):
        self.url = url
        print(f"Connecting to {url}")

db1 = DatabaseConnection("postgresql://host/db")
db2 = DatabaseConnection("postgresql://host/db")
print(db1 is db2)   # True — same instance
```

---

### 4-B — Class Decorator for Auto-Registering Subclasses

```python
REGISTRY = {}

def register(cls):
    REGISTRY[cls.__name__] = cls
    return cls   # return class unchanged

@register
class PostgreSQLDriver:
    pass

@register
class MySQLDriver:
    pass

print(REGISTRY)
# {'PostgreSQLDriver': <class 'PostgreSQLDriver'>, 'MySQLDriver': <class 'MySQLDriver'>}

# Factory function using registry
def get_driver(name: str):
    if name not in REGISTRY:
        raise KeyError(f"Unknown driver: {name}")
    return REGISTRY[name]()
```

---

## 5. `@property` — The Pythonic Getter/Setter

### 5-A — Why Properties Exist

```python
# Java style (wrong in Python — direct attribute access is idiomatic)
class Circle:
    def __init__(self, radius):
        self._radius = radius

    def get_radius(self):
        return self._radius

    def set_radius(self, value):
        if value < 0:
            raise ValueError("Radius cannot be negative")
        self._radius = value

# Python style — @property allows validation with clean attribute syntax
class Circle:
    def __init__(self, radius: float):
        self.radius = radius   # calls the setter on __init__ too!

    @property
    def radius(self) -> float:
        return self._radius

    @radius.setter
    def radius(self, value: float):
        if value < 0:
            raise ValueError(f"Radius cannot be negative: {value}")
        self._radius = value

    @property
    def area(self) -> float:
        import math
        return math.pi * self._radius ** 2

c = Circle(5)
print(c.radius)   # 5 — looks like attribute access, calls getter
print(c.area)     # 78.54... — computed property, no ()

c.radius = -1     # raises ValueError — calls setter
```

**The API is backward-compatible:** Start with a plain attribute, add `@property` later without changing the caller.

---

### 5-B — `@property` with `@deleter`

```python
class CachedData:
    def __init__(self):
        self._cache = None

    @property
    def data(self):
        if self._cache is None:
            print("Loading data...")
            self._cache = expensive_load()
        return self._cache

    @data.setter
    def data(self, value):
        self._cache = value

    @data.deleter
    def data(self):
        print("Cache cleared")
        self._cache = None

obj = CachedData()
obj.data          # loads
obj.data          # cache hit, no reload
del obj.data      # prints "Cache cleared", resets
obj.data          # loads again
```

---

## 6. Descriptor Protocol — The Engine Behind Properties

### 6-A — What is a Descriptor?

A **descriptor** is any object that defines `__get__`, `__set__`, or `__delete__`. When an attribute lookup finds a descriptor in a class `__dict__`, Python calls the descriptor method instead of returning the object directly.

```
obj.attr access:
1. Python looks in type(obj).__mro__ for the attribute
2. If found and it's a data descriptor (has __set__) → call descriptor.__get__(obj, type(obj))
3. Else: look in obj.__dict__
4. Else: if found in class and non-data descriptor (only __get__) → call descriptor.__get__
5. Else: return the class attribute directly
```

**Data descriptor** (has `__set__` and/or `__delete__`): takes priority over instance `__dict__`.
**Non-data descriptor** (only `__get__`): instance `__dict__` takes priority.

---

### 6-B — Building a Descriptor from Scratch

```python
class Validated:
    """Descriptor that validates value against a validator function."""

    def __set_name__(self, owner, name):
        self.public_name = name
        self.private_name = "_" + name   # store as _fieldname

    def __init__(self, validator=None):
        self.validator = validator

    def __get__(self, obj, objtype=None):
        if obj is None:
            return self   # class-level access returns the descriptor itself
        return getattr(obj, self.private_name, None)

    def __set__(self, obj, value):
        if self.validator and not self.validator(value):
            raise ValueError(f"Invalid value for {self.public_name}: {value}")
        setattr(obj, self.private_name, value)

    def __delete__(self, obj):
        delattr(obj, self.private_name)


class Person:
    name = Validated(lambda v: isinstance(v, str) and len(v) > 0)
    age = Validated(lambda v: isinstance(v, int) and 0 <= v <= 150)

    def __init__(self, name, age):
        self.name = name   # calls Validated.__set__
        self.age = age     # calls Validated.__set__

p = Person("Alice", 30)
print(p.name)   # "Alice" — calls Validated.__get__
p.age = 200     # ValueError: Invalid value for age: 200
```

---

### 6-C — How `@property` is a Descriptor

`@property` is simply a built-in descriptor class:

```python
# @property is equivalent to:
class property:
    def __init__(self, fget=None, fset=None, fdel=None, doc=None):
        self.fget = fget
        self.fset = fset
        self.fdel = fdel
        self.__doc__ = doc or (fget.__doc__ if fget else None)

    def __get__(self, obj, objtype=None):
        if obj is None:
            return self
        if self.fget is None:
            raise AttributeError("unreadable attribute")
        return self.fget(obj)

    def __set__(self, obj, value):
        if self.fset is None:
            raise AttributeError("can't set attribute")
        self.fset(obj, value)

    def __delete__(self, obj):
        if self.fdel is None:
            raise AttributeError("can't delete attribute")
        self.fdel(obj)

    def getter(self, fget): return type(self)(fget, self.fset, self.fdel, self.__doc__)
    def setter(self, fset): return type(self)(self.fget, fset, self.fdel, self.__doc__)
    def deleter(self, fdel): return type(self)(self.fget, self.fset, fdel, self.__doc__)
```

**`@property` is a data descriptor** (has `__set__`) — it takes priority over the instance `__dict__`. This is why setting `obj.radius = 5` calls the setter instead of creating an instance attribute.

---

### 6-D — Non-Data Descriptor: How Methods Work

```python
class Greeter:
    def hello(self, name):
        return f"Hello, {name}"

g = Greeter()
g.hello("Alice")   # works — but how?
```

**Functions are non-data descriptors.** When you access `g.hello`, Python finds `hello` in `Greeter.__dict__`, sees it has `__get__`, and calls `hello.__get__(g, Greeter)` — which returns a bound method with `g` as the first argument. This is how `self` is injected.

```python
# Equivalent to:
bound = Greeter.hello.__get__(g, Greeter)
bound("Alice")   # "Hello, Alice"

# Unbound (class access):
Greeter.hello(g, "Alice")   # same result
```

---

### 6-E — `__slots__` as Descriptors

```python
class Point:
    __slots__ = ("x", "y")   # creates member descriptors

    def __init__(self, x, y):
        self.x = x
        self.y = y

p = Point(1, 2)
print(type(Point.x))   # <class 'member_descriptor'>
print(Point.x.__get__(p, Point))   # 1

# No __dict__ on instances
print(hasattr(p, "__dict__"))   # False — slots replace instance dict
```

**`__slots__` benefits:**
- ~3-5× less memory per instance (no per-instance `__dict__`)
- Slightly faster attribute access (C-level member descriptors)
- Prevents adding arbitrary attributes at runtime

**`__slots__` limitations:**
- Cannot pickle easily without extra work
- Multiple inheritance requires all classes to define `__slots__`
- No dynamic attribute assignment (`p.z = 3` raises `AttributeError`)

---

## 7. Metaclasses

### 7-A — `type` Is the Metaclass of All Classes

```python
# In Python, a class is itself an instance of type
print(type(int))     # <class 'type'>
print(type(str))     # <class 'type'>
print(type(list))    # <class 'type'>

class MyClass:
    pass

print(type(MyClass))   # <class 'type'>
```

**`type` creates classes dynamically:**

```python
# These two are exactly equivalent
class Dog:
    species = "Canis lupus"
    def bark(self): return "Woof"

Dog = type("Dog", (object,), {"species": "Canis lupus", "bark": lambda self: "Woof"})
```

`type(name, bases, namespace)` — name: class name, bases: tuple of parent classes, namespace: dict of attributes/methods.

---

### 7-B — Custom Metaclass

```python
class SingletonMeta(type):
    """Metaclass that enforces Singleton pattern for all classes using it."""
    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            # __call__ on type creates the instance — super().__call__ runs __new__ + __init__
            cls._instances[cls] = super().__call__(*args, **kwargs)
        return cls._instances[cls]

class DatabasePool(metaclass=SingletonMeta):
    def __init__(self, size: int = 10):
        self.size = size
        self.connections = []
        print(f"Creating pool with {size} connections")

db1 = DatabasePool(10)
db2 = DatabasePool(20)
print(db1 is db2)   # True — same instance
print(db1.size)     # 10 — second __init__ never called
```

**Metaclass `__call__` intercepts `ClassName(args)`** — the point where a class "calls" its `__new__` and `__init__`. Override it to control instance creation.

---

### 7-C — Metaclass for Auto-Validation

```python
class ValidatedMeta(type):
    """Metaclass that checks all methods have return type annotations."""

    def __new__(mcs, name, bases, namespace):
        for attr_name, attr_value in namespace.items():
            if callable(attr_value) and not attr_name.startswith("_"):
                hints = getattr(attr_value, "__annotations__", {})
                if "return" not in hints:
                    raise TypeError(
                        f"{name}.{attr_name} must have a return type annotation"
                    )
        return super().__new__(mcs, name, bases, namespace)

class MyService(metaclass=ValidatedMeta):
    def greet(self, name: str) -> str:   # has return annotation — OK
        return f"Hello, {name}"

    def broken(self, x: int):            # TypeError at class definition!
        return x * 2
```

**Metaclass `__new__` runs at class definition time** — the class body has been executed and the namespace dict is ready. This is how you validate or transform a class before it's finalized.

---

### 7-D — `__prepare__` for Custom Namespace

```python
from collections import OrderedDict

class OrderedMeta(type):
    """Metaclass that records the order attributes were defined."""

    @classmethod
    def __prepare__(mcs, name, bases):
        return OrderedDict()   # use OrderedDict as class namespace

    def __new__(mcs, name, bases, namespace):
        cls = super().__new__(mcs, name, bases, dict(namespace))
        cls._field_order = list(namespace.keys())
        return cls

class Record(metaclass=OrderedMeta):
    first_name: str = ""
    last_name: str = ""
    age: int = 0

print(Record._field_order)
# ['__module__', '__qualname__', 'first_name', 'last_name', 'age', ...]
```

**`__prepare__` returns the dict-like object used as the class body's local namespace.** Python 3.7+ dicts are ordered by insertion, so `__prepare__` is less critical now — but it's still how frameworks like ORMs control the attribute capture process.

---

## 8. `__init_subclass__` — Lightweight Metaclass Alternative

```python
class Plugin:
    """Base class that auto-registers all subclasses."""
    _registry: dict[str, type] = {}

    def __init_subclass__(cls, plugin_name: str = None, **kwargs):
        super().__init_subclass__(**kwargs)
        name = plugin_name or cls.__name__
        Plugin._registry[name] = cls
        print(f"Registered plugin: {name}")

class CSVPlugin(Plugin, plugin_name="csv"):
    def process(self): return "csv"

class JSONPlugin(Plugin, plugin_name="json"):
    def process(self): return "json"

print(Plugin._registry)
# {'csv': <class 'CSVPlugin'>, 'json': <class 'JSONPlugin'>}

# Factory
def get_plugin(name: str) -> Plugin:
    return Plugin._registry[name]()
```

**`__init_subclass__` is called on the base class whenever a subclass is defined.** It is simpler than a metaclass for registration and validation patterns and avoids metaclass conflict issues.

**Java Bridge:** Java uses `ServiceLoader` for plugin discovery — reads `META-INF/services/`. Python's `__init_subclass__` achieves the same at class definition time with pure Python.

---

## 9. How Frameworks Use These Internals

### 9-A — How `@dataclass` Works

```python
from dataclasses import dataclass, fields

@dataclass
class Point:
    x: float
    y: float = 0.0

# @dataclass inspects __annotations__ and generates:
# - __init__(self, x: float, y: float = 0.0)
# - __repr__(self)
# - __eq__(self, other)
# (and optionally __hash__, __lt__, __post_init__, etc.)

# Equivalent to:
class Point:
    def __init__(self, x: float, y: float = 0.0):
        self.x = x
        self.y = y

    def __repr__(self):
        return f"Point(x={self.x!r}, y={self.y!r})"

    def __eq__(self, other):
        if other.__class__ is self.__class__:
            return (self.x, self.y) == (other.x, other.y)
        return NotImplemented
```

**`@dataclass` uses `__annotations__` and `field()` objects (which are descriptors) to generate code at class definition time.**

---

### 9-B — How `ABCMeta` Enforces Abstract Methods

```python
from abc import ABC, abstractmethod

class Shape(ABC):
    @abstractmethod
    def area(self) -> float: ...

    @abstractmethod
    def perimeter(self) -> float: ...

class Square(Shape):
    def __init__(self, side: float):
        self.side = side

    def area(self) -> float:
        return self.side ** 2

    # Missing perimeter!

s = Square(5)
# TypeError: Can't instantiate abstract class Square
# with abstract method perimeter
```

**How `ABCMeta` works:**
1. `ABCMeta.__new__` collects all `@abstractmethod` functions into `cls.__abstractmethods__` (a `frozenset`)
2. `type.__call__` (which creates instances) checks `cls.__abstractmethods__` — if non-empty, raises `TypeError`

---

### 9-C — SQLAlchemy-Style Declarative ORM (Metaclass Pattern)

```python
# Simplified illustration of how SQLAlchemy's declarative base works
class Column:
    def __init__(self, col_type, primary_key=False, nullable=True):
        self.col_type = col_type
        self.primary_key = primary_key
        self.nullable = nullable
        self.name = None   # set by __set_name__

    def __set_name__(self, owner, name):
        self.name = name

    def __get__(self, obj, objtype=None):
        if obj is None: return self
        return obj.__dict__.get(self.name)

    def __set__(self, obj, value):
        obj.__dict__[self.name] = value

class ModelMeta(type):
    def __new__(mcs, name, bases, namespace):
        columns = {k: v for k, v in namespace.items() if isinstance(v, Column)}
        cls = super().__new__(mcs, name, bases, namespace)
        cls._columns = columns
        cls._table_name = name.lower() + "s"
        return cls

class Model(metaclass=ModelMeta):
    pass

class User(Model):
    id = Column(int, primary_key=True)
    name = Column(str, nullable=False)
    email = Column(str)

print(User._columns)
# {'id': <Column>, 'name': <Column>, 'email': <Column>}
print(User._table_name)
# "users"
```

---

## 10. Descriptor Lookup Priority Summary

```
Attribute lookup for obj.attr where type(obj) is MyClass:

Priority 1 (highest): Data descriptor in MyClass (or any ancestor class)
    — has __set__ AND/OR __delete__
    — Example: @property, Validated descriptor, Column descriptor

Priority 2: Instance __dict__
    — obj.__dict__["attr"]
    — Direct instance attributes

Priority 3: Non-data descriptor in MyClass
    — has only __get__ (no __set__, no __delete__)
    — Example: regular methods (functions), staticmethod, classmethod

Priority 4 (lowest): Class dict (non-descriptor attributes)
    — class-level constants, plain class attributes
```

```python
class MyClass:
    x = property(lambda self: "property")   # data descriptor

    def __init__(self):
        self.__dict__["x"] = "instance"   # instance dict

m = MyClass()
print(m.x)   # "property" — data descriptor wins over instance dict!

# Non-data descriptor: instance dict wins
class NDDesc:
    def __get__(self, obj, cls): return "nd_desc"

class MyClass2:
    x = NDDesc()

    def __init__(self):
        self.x = "instance"   # sets instance dict

m2 = MyClass2()
print(m2.x)   # "instance" — instance dict wins over non-data descriptor
```

---

## 11. Java Developer Bridge

| Python Concept | Java Equivalent | Key Difference |
|---|---|---|
| `@decorator` | AOP / `@Aspect` + `@Around` | Python: pure Python, no framework; Java: Spring AOP |
| Decorator factory `@retry(n=3)` | `@Retryable(maxAttempts=3)` | Java: annotation processed by AOP proxy; Python: closure |
| `functools.wraps` | N/A (Java reflection preserves metadata) | Python must manually copy metadata or it's lost |
| `@property` getter | `getRadius()` | Python: transparent attribute syntax; Java: explicit method call |
| `@property` setter | `setRadius(v)` | Python: `obj.radius = v` looks like assignment |
| Descriptor `__get__`/`__set__` | Java annotations + reflection | No direct equivalent; Python more direct |
| `type(name, bases, ns)` | `ClassLoader` / runtime bytecode | Python: pure Python dict manipulation |
| Custom metaclass | No equivalent | Java generates bytecode; Python inspects dicts |
| `__init_subclass__` | `ServiceLoader` | Python: pure in-language; Java: file-based discovery |
| `ABCMeta` + `@abstractmethod` | `interface` / `abstract class` | Both enforce at instantiation; Java at compile time |
| `__slots__` | N/A | Python optimization; Java fields are always slot-like |
| Descriptor lookup order | JVM field access rules | Different but conceptually similar priority chain |
| `@dataclass` | Java 14+ `record` (limited) | Python generates full `__init__`, `__eq__`, `__repr__` |
| Class decorator | Reflection + annotation processing | Python: manipulates the class object directly |

---

## 12. Hot Interview Q&A

**Q1: What is a decorator and what does `functools.wraps` do?**
> A decorator is a callable that takes a function and returns a replacement callable. `functools.wraps(func)` copies the original function's `__name__`, `__qualname__`, `__doc__`, `__annotations__`, `__dict__`, and `__module__` to the wrapper, and sets `__wrapped__` to the original. Without it, introspection tools (logging, pytest, `help()`) see the wrapper instead of the original function.

**Q2: How do you write a decorator that accepts arguments?**
> Add one more layer of nesting: the outermost callable accepts the decorator arguments and returns the actual decorator. The actual decorator takes the function and returns the wrapper. Three levels: `decorator_factory(args)` → `decorator(func)` → `wrapper(*args, **kwargs)`.

**Q3: What is a descriptor and how does `@property` use it?**
> A descriptor is any object with `__get__`, `__set__`, or `__delete__`. When an attribute lookup finds a descriptor in a class's MRO, Python calls the descriptor method instead of returning the object. `@property` is a built-in data descriptor — it defines `__get__` (getter), `__set__` (setter), and `__delete__` (deleter). Being a data descriptor means it takes priority over instance `__dict__`, so assignment calls the setter rather than creating an instance attribute.

**Q4: What is the difference between a data descriptor and a non-data descriptor?**
> A data descriptor has `__set__` and/or `__delete__`. It takes priority over the instance `__dict__` in attribute lookup. A non-data descriptor has only `__get__`. The instance `__dict__` takes priority over a non-data descriptor. Functions (regular methods) are non-data descriptors — that's why you can shadow a method by setting an instance attribute with the same name.

**Q5: What is a metaclass and when would you use one?**
> A metaclass is the class of a class — it controls how a class is created. `type` is the default metaclass. Custom metaclasses override `__new__` (runs before class is created) to inspect or modify the class body, add methods, enforce constraints, or register the class. Use cases: ORMs (SQLAlchemy), validation frameworks, plugin registries, ABCMeta. For most registration patterns, prefer `__init_subclass__` — simpler and avoids metaclass conflict.

**Q6: What is `__init_subclass__` and why is it preferred over metaclasses for simple cases?**
> `__init_subclass__` is a class method called on the base class whenever a subclass is defined. It receives the new subclass as `cls` plus any keyword arguments from the class statement. It's simpler than a metaclass for registration and enforcement because it requires no metaclass inheritance chain and avoids metaclass conflicts with other libraries (e.g., when a class inherits from both your class and a third-party class with its own metaclass).

**Q7: How do methods work under the descriptor protocol?**
> Functions are non-data descriptors — they implement `__get__`. When you access `obj.method`, Python finds the function in the class `__dict__`, calls `function.__get__(obj, type(obj))`, and gets back a **bound method** with `obj` pre-filled as the first argument. This is the mechanism by which `self` is automatically passed. Class-level access `MyClass.method` returns the function unbound (or `__get__(None, MyClass)` which returns the function itself in Python 3).

---

## 13. Final Revision Checklist

- [ ] Can implement a timing decorator with `functools.wraps` from memory
- [ ] Can explain what `functools.wraps` copies and why it matters for introspection
- [ ] Can implement a decorator factory (three-layer closure) with configurable arguments
- [ ] Can explain decorator stacking order: bottom-up apply, top-down call
- [ ] Can implement `@property` getter + setter + deleter with validation
- [ ] Can explain why `@property` being a data descriptor means setting calls the setter
- [ ] Can implement a custom descriptor with `__get__`, `__set__`, `__set_name__`
- [ ] Can explain the 4-level descriptor lookup priority order from memory
- [ ] Can explain the difference between data and non-data descriptors with an example
- [ ] Can explain how methods use the descriptor protocol to inject `self`
- [ ] Can explain `__slots__` as descriptors and their memory/speed tradeoffs
- [ ] Can explain what `type(name, bases, ns)` does
- [ ] Can implement a custom metaclass with `__new__` for class-level validation
- [ ] Can explain how `ABCMeta` enforces abstract methods via `__abstractmethods__`
- [ ] Can implement `__init_subclass__` for a plugin registration system
- [ ] Can explain when to use `__init_subclass__` vs metaclass
- [ ] Can sketch how `@dataclass` uses `__annotations__` to generate `__init__`
- [ ] Can explain how SQLAlchemy-style ORMs use metaclasses to inspect Column descriptors
