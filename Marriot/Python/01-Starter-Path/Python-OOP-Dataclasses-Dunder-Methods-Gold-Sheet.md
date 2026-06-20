# Python OOP, Dataclasses & Dunder Methods — Gold Sheet

> **Track**: Python Interview Track — Group 1: Starter Path  
> **File**: 5 of 7  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| `__repr__` vs `__str__` | ★★★★★ | Java toString() covers both — Python splits them; every class needs `__repr__` |
| `__eq__` + `__hash__` contract | ★★★★★ | Defining `__eq__` silently disables `__hash__` — #1 OOP hashability trap |
| `@dataclass` — full mechanics | ★★★★★ | Modern Python — asked in every interview; `frozen=True`, `field()`, `__post_init__` |
| `property` decorator | ★★★★☆ | No getter/setter methods in Python — use `@property` |
| `__slots__` — memory and performance | ★★★★☆ | Java devs often unaware; critical for high-volume objects |
| MRO and `super()` in diamond inheritance | ★★★★☆ | Java has no multiple inheritance of classes — C3 linearization is entirely new |
| `Protocol` structural subtyping | ★★★☆☆ | Duck typing made explicit — no Java equivalent (closest: structural interfaces in Kotlin) |
| `__call__` — callable objects | ★★★☆☆ | Java: `FunctionalInterface`; Python: any class with `__call__` |
| `__enter__` / `__exit__` | ★★★☆☆ | Java try-with-resources — Python context manager protocol; covered deeply in File 7 |
| `__new__` vs `__init__` | ★★★☆☆ | Java has no `__new__` equivalent — asked for Singleton pattern interviews |
| ABC vs Protocol | ★★★☆☆ | Java interfaces → two different Python mechanisms |

---

## 2. Class Basics

### Must Know

- No `new` keyword — calling the class creates an instance.
- `self` is the first parameter of every instance method — explicitly passed by convention.
- `__init__` is the **initializer**, not the constructor (that's `__new__`).
- Python has no `private` or `protected` — naming convention: `_single` (protected-by-convention), `__double` (name-mangled).

### How It Works

```python
class BankAccount:
    # Class variable — shared across all instances
    interest_rate = 0.05

    def __init__(self, owner: str, balance: float = 0.0):
        # Instance variables — unique per instance
        self.owner = owner
        self._balance = balance          # _ means "intended as internal"
        self.__id = id(self)             # __ triggers name mangling → _BankAccount__id

    def deposit(self, amount: float) -> None:
        if amount <= 0:
            raise ValueError(f"Deposit amount must be positive, got {amount}")
        self._balance += amount

    def __repr__(self) -> str:
        return f"BankAccount(owner={self.owner!r}, balance={self._balance:.2f})"

    def __str__(self) -> str:
        return f"{self.owner}'s account: ${self._balance:.2f}"


acc = BankAccount("Alice", 1000.0)   # No 'new' keyword
print(repr(acc))   # BankAccount(owner='Alice', balance=1000.00)
print(str(acc))    # Alice's account: $1000.00
print(acc)         # Alice's account: $1000.00  — print() calls __str__

# Name mangling
print(acc._BankAccount__id)  # works — mangling changes name, doesn't truly hide
```

### Access Conventions

```python
class Example:
    def __init__(self):
        self.public = "everyone"          # Public — use freely
        self._protected = "convention"   # Protected — internal use; subclasses OK
        self.__private = "mangled"       # Name-mangled → _Example__private

e = Example()
print(e.public)           # OK
print(e._protected)       # Works but "shouldn't" — convention only
print(e.__private)        # AttributeError! Name was mangled
print(e._Example__private)  # Works — mangling is not true encapsulation
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Instantiation | `new MyClass()` | `MyClass()` — calling the class |
| `this` | `this.field` | `self.field` — must be explicit |
| Constructor | `MyClass(...)` | `__init__(self, ...)` |
| Private | `private` enforced by compiler | `__name` — convention + name mangling only |
| Protected | `protected` enforced | `_name` — convention only |
| Static field | `static int count;` | Class-level variable: `count = 0` outside any method |
| `toString()` | One method for all representations | `__repr__` (developer) and `__str__` (user) |
| `equals()` | Object method | `__eq__` |
| `hashCode()` | Object method | `__hash__` |
| `instanceof` | `obj instanceof MyClass` | `isinstance(obj, MyClass)` |
| `getClass()` | `obj.getClass()` | `type(obj)` or `obj.__class__` |

---

## 3. Dunder Methods — The Complete Reference

### 3a. Object Representation

```python
class Vector:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __repr__(self) -> str:
        """Developer representation — should be unambiguous and ideally eval()-able.
        Called by: repr(obj), in REPL, in containers (lists, dicts), logging.
        RULE: Every class should have __repr__."""
        return f"Vector(x={self.x!r}, y={self.y!r})"

    def __str__(self) -> str:
        """User-friendly representation — called by str(obj), print(obj), f-strings.
        Falls back to __repr__ if not defined."""
        return f"({self.x}, {self.y})"

    def __format__(self, spec: str) -> str:
        """Called by format(obj, spec) and f'{obj:spec}'.
        spec is the format specification after the colon."""
        if spec == "polar":
            import math
            r = math.sqrt(self.x**2 + self.y**2)
            theta = math.atan2(self.y, self.x)
            return f"({r:.2f}, {theta:.2f}rad)"
        return str(self)


v = Vector(3, 4)
print(repr(v))         # Vector(x=3, y=4)  — __repr__
print(str(v))          # (3, 4)            — __str__
print(f"{v}")          # (3, 4)            — __str__ via format
print(f"{v:polar}")    # (5.00, 0.93rad)   — __format__
print(f"Vector: {v!r}")  # Vector: Vector(x=3, y=4)  — !r forces repr
print([v])             # [Vector(x=3, y=4)]  — containers use repr, not str
```

### 3b. Comparison and Hashing

```python
from functools import total_ordering

@total_ordering   # Fills in __le__, __gt__, __ge__ from __eq__ and __lt__
class Temperature:
    def __init__(self, celsius: float):
        self.celsius = celsius

    def __eq__(self, other) -> bool:
        if not isinstance(other, Temperature):
            return NotImplemented   # Let Python try the other operand's __eq__
        return self.celsius == other.celsius

    def __lt__(self, other) -> bool:
        if not isinstance(other, Temperature):
            return NotImplemented
        return self.celsius < other.celsius

    def __hash__(self) -> int:
        """CRITICAL: if you define __eq__, you MUST also define __hash__
        if you want the object to be hashable.
        Python 3 sets __hash__ = None when __eq__ is defined without __hash__."""
        return hash(self.celsius)

    def __repr__(self):
        return f"Temperature({self.celsius}°C)"


t1 = Temperature(20)
t2 = Temperature(20)
t3 = Temperature(30)

print(t1 == t2)    # True  — __eq__
print(t1 < t3)     # True  — __lt__
print(t1 <= t2)    # True  — __le__ (from @total_ordering)
print(t3 > t1)     # True  — __gt__ (from @total_ordering)

# Hashable because we defined __hash__
temps = {t1, t2, t3}
print(len(temps))  # 2  — t1 and t2 are equal so only one stored

# Sorted works because __lt__ defined
print(sorted([t3, t1, t2]))  # [Temperature(20°C), Temperature(20°C), Temperature(30°C)]
```

**Critical Trap**:

```python
class Broken:
    def __init__(self, x):
        self.x = x

    def __eq__(self, other):
        return self.x == other.x
    # NO __hash__ defined!

b = Broken(1)
# {b: "value"}  # TypeError: unhashable type: 'Broken'
# In Python 3: defining __eq__ without __hash__ implicitly sets __hash__ = None
print(Broken.__hash__)   # None  — unhashable!
```

### 3c. Arithmetic Operators

```python
class Vector:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    # Binary operators: self op other
    def __add__(self, other):
        if isinstance(other, Vector):
            return Vector(self.x + other.x, self.y + other.y)
        return NotImplemented   # Try other.__radd__(self)

    def __sub__(self, other):
        return Vector(self.x - other.x, self.y - other.y)

    def __mul__(self, scalar):
        """Vector * scalar"""
        return Vector(self.x * scalar, self.y * scalar)

    def __rmul__(self, scalar):
        """scalar * Vector — called when left operand doesn't know how to handle"""
        return self.__mul__(scalar)

    def __truediv__(self, scalar):
        return Vector(self.x / scalar, self.y / scalar)

    # In-place operators (augmented assignment)
    def __iadd__(self, other):
        """v += other — modify in place and return self (or new object)"""
        self.x += other.x
        self.y += other.y
        return self    # Must return self for mutable objects

    # Unary operators
    def __neg__(self):
        return Vector(-self.x, -self.y)

    def __abs__(self):
        import math
        return math.sqrt(self.x**2 + self.y**2)

    def __repr__(self):
        return f"Vector({self.x}, {self.y})"


v1 = Vector(1, 2)
v2 = Vector(3, 4)
print(v1 + v2)      # Vector(4, 6)
print(v1 * 3)       # Vector(3, 6)
print(3 * v1)       # Vector(3, 6)  — uses __rmul__
print(-v1)          # Vector(-1, -2)
print(abs(v2))      # 5.0
```

### `NotImplemented` vs `NotImplementedError`

```python
# NotImplemented — a singleton returned from dunder methods to signal
#                  "I don't know how to handle this, try the other operand"
#                  Python sees this and tries the reflected operation

# NotImplementedError — an exception raised in abstract methods to signal
#                       "subclasses must implement this"

# CORRECT use:
def __add__(self, other):
    if not isinstance(other, Vector):
        return NotImplemented   # NOT raise NotImplementedError

# WRONG:
def __add__(self, other):
    if not isinstance(other, Vector):
        raise NotImplementedError   # This prevents Python from trying other.__radd__
```

### 3d. Container Protocol

```python
class NumberList:
    def __init__(self, *numbers):
        self._data = list(numbers)

    def __len__(self) -> int:
        """Called by len(obj). Also used for __bool__ fallback."""
        return len(self._data)

    def __getitem__(self, index):
        """Called by obj[index] and obj[start:stop].
        Implementing this also enables: iteration, in operator, reversed()."""
        return self._data[index]

    def __setitem__(self, index, value):
        """Called by obj[index] = value."""
        self._data[index] = value

    def __delitem__(self, index):
        """Called by del obj[index]."""
        del self._data[index]

    def __contains__(self, item) -> bool:
        """Called by item in obj. Optional — falls back to __getitem__ iteration."""
        return item in self._data

    def __iter__(self):
        """Called by iter(obj), for x in obj.
        Return an iterator. Here we delegate to list's iterator."""
        return iter(self._data)

    def __reversed__(self):
        """Called by reversed(obj)."""
        return reversed(self._data)

    def __repr__(self):
        return f"NumberList{tuple(self._data)}"


nl = NumberList(10, 20, 30, 40, 50)
print(len(nl))          # 5
print(nl[0])            # 10
print(nl[-1])           # 50
print(nl[1:3])          # [20, 30]
print(30 in nl)         # True
print(list(reversed(nl)))  # [50, 40, 30, 20, 10]
for x in nl:            # iteration via __iter__
    print(x)
```

### 3e. `__call__` — Callable Objects

```python
class Multiplier:
    def __init__(self, factor):
        self.factor = factor

    def __call__(self, x):
        """Makes the object callable like a function."""
        return x * self.factor


double = Multiplier(2)
triple = Multiplier(3)

print(double(5))     # 10  — calling the object
print(triple(5))     # 15

print(callable(double))   # True — has __call__
print(callable(5))        # False — int has no __call__

# Use case: stateful callable (like a closure but inspectable)
class RateLimiter:
    def __init__(self, calls_per_second):
        import time
        self.interval = 1.0 / calls_per_second
        self._last_call = 0.0

    def __call__(self, func, *args, **kwargs):
        import time
        now = time.time()
        elapsed = now - self._last_call
        if elapsed < self.interval:
            time.sleep(self.interval - elapsed)
        self._last_call = time.time()
        return func(*args, **kwargs)
```

### 3f. `__bool__` and `__len__`

```python
class Queue:
    def __init__(self):
        self._items = []

    def push(self, item):
        self._items.append(item)

    def __len__(self):
        return len(self._items)

    def __bool__(self):
        """If __bool__ is absent, Python falls back to __len__ (0 = False)."""
        return len(self._items) > 0


q = Queue()
if not q:              # False — __bool__ → __len__ → 0 → False
    print("Queue is empty")
q.push(1)
if q:                  # True
    print("Queue has items")
```

### 3g. `__new__` vs `__init__` — Singleton Pattern

```python
class Singleton:
    _instance = None

    def __new__(cls, *args, **kwargs):
        """__new__ creates the instance. Called BEFORE __init__.
        Returns the new object (or existing one for Singleton)."""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self, value):
        """__init__ initializes the already-created instance."""
        self.value = value


s1 = Singleton(10)
s2 = Singleton(20)
print(s1 is s2)     # True — same object
print(s1.value)     # 20 — __init__ ran again and updated value!

# Note: this simple singleton has a flaw — __init__ runs every time
# Production singleton usually uses a lock and only initializes once
```

### 3h. `__getattr__` vs `__getattribute__`

```python
class SmartDict:
    def __init__(self, data):
        self._data = data

    def __getattr__(self, name):
        """Called ONLY when normal attribute lookup fails.
        Use this for 'attribute not found' fallback.
        TRAP: Avoid infinite recursion by not accessing self.anything here."""
        if name in self._data:
            return self._data[name]
        raise AttributeError(f"'{type(self).__name__}' has no attribute '{name}'")

    # __getattribute__ is called for EVERY attribute access — dangerous to override
    # def __getattribute__(self, name): ...  # Intercepts ALL attribute access including self.x


sd = SmartDict({"host": "localhost", "port": 8080})
print(sd.host)   # 'localhost'  — __getattr__ fallback
print(sd.port)   # 8080
# sd.missing     # AttributeError
```

---

## 4. `property` — Computed Attributes

### Must Know

`@property` makes a method callable without parentheses — like a computed attribute.  
Use for: validation on set, lazy computation, backward-compatible API changes.

**Never write `get_x()` / `set_x()` methods in Python** — use `@property`.

### How It Works

```python
class Circle:
    def __init__(self, radius: float):
        self._radius = radius   # Store in "private" attribute

    @property
    def radius(self) -> float:
        """Getter — accessed as obj.radius (no parentheses)."""
        return self._radius

    @radius.setter
    def radius(self, value: float) -> None:
        """Setter — called on obj.radius = value."""
        if value < 0:
            raise ValueError(f"Radius cannot be negative: {value}")
        self._radius = value

    @radius.deleter
    def radius(self) -> None:
        """Deleter — called on del obj.radius."""
        del self._radius

    @property
    def area(self) -> float:
        """Read-only computed property — no setter defined."""
        import math
        return math.pi * self._radius ** 2

    @property
    def diameter(self) -> float:
        return self._radius * 2

    @diameter.setter
    def diameter(self, value: float) -> None:
        self.radius = value / 2   # Delegates to radius setter (includes validation)


c = Circle(5.0)
print(c.radius)    # 5.0  — calls getter
c.radius = 10.0    # calls setter
print(c.area)      # 314.159...  — read-only
c.diameter = 6.0   # calls diameter setter → radius setter
print(c.radius)    # 3.0
# c.area = 50      # AttributeError — no setter for area
```

### Lazy Property Pattern

```python
class DataProcessor:
    def __init__(self, data):
        self._data = data
        self._expensive_result = None   # Lazy cache

    @property
    def result(self):
        if self._expensive_result is None:
            print("Computing...")
            self._expensive_result = sum(x**2 for x in self._data)
        return self._expensive_result


dp = DataProcessor(range(1000))
print(dp.result)    # Computing... 332833500
print(dp.result)    # 332833500  (no "Computing..." — cached)
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Getter | `getBalance()` | `@property def balance(self):` |
| Setter | `setBalance(double v)` | `@balance.setter def balance(self, v):` |
| Read-only property | `final` field + getter, no setter | `@property` with no setter |
| Computed field | Method call: `circle.area()` | Property: `circle.area` (no parens) |
| Lazy initialization | `if (result == null) compute()` | Same pattern behind `@property` |

---

## 5. `@classmethod` and `@staticmethod`

### Must Know

- `@classmethod` — receives `cls` (the class) as first arg. Used for alternative constructors, factory methods.
- `@staticmethod` — receives no implicit first arg. Just a regular function namespaced in the class. No access to class or instance.

### How It Works

```python
class Date:
    def __init__(self, year: int, month: int, day: int):
        self.year = year
        self.month = month
        self.day = day

    @classmethod
    def from_string(cls, date_string: str) -> "Date":
        """Alternative constructor — factory method pattern.
        cls is the class itself, so subclasses work correctly."""
        year, month, day = map(int, date_string.split("-"))
        return cls(year, month, day)    # cls not Date — subclass-friendly

    @classmethod
    def today(cls) -> "Date":
        import datetime
        d = datetime.date.today()
        return cls(d.year, d.month, d.day)

    @staticmethod
    def is_valid_date(year: int, month: int, day: int) -> bool:
        """No access to class or instance. Pure utility."""
        return 1 <= month <= 12 and 1 <= day <= 31

    def __repr__(self):
        return f"Date({self.year}-{self.month:02d}-{self.day:02d})"


d1 = Date(2024, 1, 15)
d2 = Date.from_string("2024-06-20")   # Classmethod factory
d3 = Date.today()                     # Classmethod factory
print(Date.is_valid_date(2024, 13, 1))  # False — staticmethod
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Static method | `static void helper()` | `@staticmethod def helper():` |
| Factory method | `static MyClass fromString(String s)` | `@classmethod def from_string(cls, s):` |
| Class reference in factory | Returns `new MyClass(...)` — hardcoded | Returns `cls(...)` — subclass-friendly |
| `this` equivalent | `this` | `self` |
| Class reference | `MyClass.class` | `cls` in classmethod, or `type(self)` in instance method |

---

## 6. `__slots__` — Memory Optimization

### Must Know

By default, Python stores instance attributes in a `__dict__` (a dictionary). `__slots__` replaces this with fixed-size slots — like Java fields at the bytecode level.

**Benefits of `__slots__`**:
1. ~50–60% less memory per instance (no per-instance `__dict__`).
2. Faster attribute access (direct offset, not dict lookup).
3. Prevents accidental creation of new attributes.

**Costs of `__slots__`**:
1. Cannot add new instance attributes dynamically.
2. Requires each class in the hierarchy to define `__slots__` for full benefit.
3. Cannot use `weakref` by default unless `__weakref__` is in slots.

### How It Works

```python
class PointWithDict:
    def __init__(self, x, y):
        self.x = x
        self.y = y


class PointWithSlots:
    __slots__ = ("x", "y")   # Declares allowed attributes — no __dict__

    def __init__(self, x, y):
        self.x = x
        self.y = y


# Memory comparison
import sys
p1 = PointWithDict(1, 2)
p2 = PointWithSlots(1, 2)

print(sys.getsizeof(p1))           # ~56 bytes (object header)
print(sys.getsizeof(p1.__dict__))  # ~232 bytes (the dict itself)
print(sys.getsizeof(p2))           # ~72 bytes (with slots — no dict)
# p2 has no __dict__ attribute!

# Cannot add new attributes with slots
p1.z = 3    # Works — p1 has __dict__
# p2.z = 3  # AttributeError: 'PointWithSlots' object has no attribute 'z'

# Slots with inheritance — subclass must also define __slots__ for full benefit
class Point3D(PointWithSlots):
    __slots__ = ("z",)   # Only the NEW slots; inherits x, y from parent

    def __init__(self, x, y, z):
        super().__init__(x, y)
        self.z = z
```

### When to Use `__slots__`

```python
# Good use cases for __slots__:
# 1. Large number of instances (thousands+) — e.g., particles, events, records
# 2. Performance-critical tight loops
# 3. Value objects that shouldn't have dynamic attributes

# Don't use __slots__:
# 1. When you need __dict__ for dynamic attributes
# 2. When using some ORM or serialization frameworks that need __dict__
# 3. When simplicity matters more than memory
```

---

## 7. Inheritance and MRO

### Must Know

- Python supports **multiple inheritance** — Java does not (for classes).
- **MRO** (Method Resolution Order) — C3 Linearization algorithm determines which method is called when multiple parent classes define the same method.
- Use `ClassName.__mro__` or `ClassName.mro()` to see the resolution order.

### Single Inheritance

```python
class Animal:
    def __init__(self, name: str):
        self.name = name

    def speak(self) -> str:
        return "..."

    def __repr__(self):
        return f"{type(self).__name__}(name={self.name!r})"


class Dog(Animal):
    def speak(self) -> str:
        return "Woof!"

    def fetch(self) -> str:
        return f"{self.name} fetches the ball!"


class GoldenRetriever(Dog):
    def speak(self) -> str:
        return "Woof woof!"    # Overrides Dog's speak


d = Dog("Rex")
g = GoldenRetriever("Buddy")
print(d.speak())     # Woof!
print(g.speak())     # Woof woof!
print(g.fetch())     # Buddy fetches the ball!  — inherited from Dog

print(isinstance(g, GoldenRetriever))  # True
print(isinstance(g, Dog))              # True
print(isinstance(g, Animal))           # True
print(issubclass(GoldenRetriever, Animal))  # True
```

### Multiple Inheritance and MRO

```python
class A:
    def method(self):
        return "A"

class B(A):
    def method(self):
        return "B"

class C(A):
    def method(self):
        return "C"

class D(B, C):    # Diamond inheritance
    pass


d = D()
print(d.method())         # "B"  — B comes before C in MRO
print(D.__mro__)
# (<class 'D'>, <class 'B'>, <class 'C'>, <class 'A'>, <class 'object'>)
# MRO: D → B → C → A → object
# Rule: C3 linearization — depth-first, left-to-right, then check consistency
```

### MRO Visualization

```
        A
       / \
      B   C
       \ /
        D

Java: impossible (multiple class inheritance not allowed)
Python: D.method() → B.method() (B before C per left-to-right in D(B, C))
MRO order: D → B → C → A → object
```

---

## 8. `super()` — Cooperative Multiple Inheritance

### Must Know

`super()` does not mean "call my parent class directly." It means **"call the next class in the MRO."**  
This is critical for cooperative multiple inheritance to work correctly.

### How It Works

```python
class Base:
    def __init__(self, **kwargs):
        print(f"Base.__init__ kwargs={kwargs}")
        super().__init__(**kwargs)   # Must pass remaining kwargs up the chain!


class LogMixin:
    def __init__(self, log_level="INFO", **kwargs):
        print(f"LogMixin.__init__ log_level={log_level}")
        self.log_level = log_level
        super().__init__(**kwargs)


class CacheMixin:
    def __init__(self, cache_size=128, **kwargs):
        print(f"CacheMixin.__init__ cache_size={cache_size}")
        self.cache_size = cache_size
        super().__init__(**kwargs)


class Service(LogMixin, CacheMixin, Base):
    def __init__(self, name, **kwargs):
        self.name = name
        super().__init__(**kwargs)


# MRO: Service → LogMixin → CacheMixin → Base → object
s = Service("MyService", log_level="DEBUG", cache_size=256)
# Service.__init__: name=MyService
# LogMixin.__init__: log_level=DEBUG
# CacheMixin.__init__: cache_size=256
# Base.__init__: kwargs={}
```

**Key Rule**: In cooperative inheritance, every `__init__` must call `super().__init__(**kwargs)` and accept `**kwargs` to pass unrecognized arguments up the chain.

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Single inheritance | `class Dog extends Animal` | `class Dog(Animal):` |
| Multiple inheritance | Not supported for classes (interfaces only) | Supported — use MRO rules |
| `super()` | Calls direct parent class | Calls next in MRO — may not be the direct parent! |
| Interface default method conflict | Compiler forces override | MRO determines which method wins |
| Mixin pattern | `default` methods in interfaces (limited) | Mixin classes — full implementation, cooperative `super()` |

---

## 9. Abstract Base Classes (ABCs)

### Must Know

ABCs enforce that subclasses implement specific methods. Like Java interfaces with `abstract` methods.  
Use `abc.ABC` or `abc.ABCMeta` to create abstract classes.

### How It Works

```python
from abc import ABC, abstractmethod

class Shape(ABC):
    @abstractmethod
    def area(self) -> float:
        """Must be implemented by subclasses."""
        ...

    @abstractmethod
    def perimeter(self) -> float:
        ...

    def describe(self) -> str:
        """Concrete method — available to all subclasses."""
        return f"Shape with area={self.area():.2f} and perimeter={self.perimeter():.2f}"


# Shape()          # TypeError: Can't instantiate abstract class Shape
#                  # with abstract methods area, perimeter

class Circle(Shape):
    def __init__(self, radius: float):
        self.radius = radius

    def area(self) -> float:
        import math
        return math.pi * self.radius ** 2

    def perimeter(self) -> float:
        import math
        return 2 * math.pi * self.radius


class Rectangle(Shape):
    def __init__(self, width: float, height: float):
        self.width = width
        self.height = height

    def area(self) -> float:
        return self.width * self.height

    def perimeter(self) -> float:
        return 2 * (self.width + self.height)


c = Circle(5)
r = Rectangle(4, 6)
print(c.describe())   # Shape with area=78.54 and perimeter=31.42
print(r.describe())   # Shape with area=24.00 and perimeter=20.00
```

### Abstract Class with `abstractproperty`

```python
from abc import ABC, abstractmethod

class Vehicle(ABC):
    @property
    @abstractmethod
    def max_speed(self) -> float:
        """Read-only abstract property — subclasses must implement."""
        ...

    @abstractmethod
    def start(self) -> None:
        ...


class Car(Vehicle):
    @property
    def max_speed(self) -> float:
        return 200.0

    def start(self) -> None:
        print("Car engine started")
```

### `collections.abc` — Register Custom Types

```python
from collections.abc import Iterable, Sized, Mapping

# Check if something implements a protocol
print(isinstance([1, 2, 3], Iterable))   # True
print(isinstance("hello", Iterable))     # True
print(isinstance(42, Iterable))          # False

# Implement Mapping (dict-like) without inheriting from dict
from collections.abc import MutableMapping

class EnvMap(MutableMapping):
    """Case-insensitive dict — must implement the abstract methods."""
    def __init__(self, data=None):
        self._store = {}
        if data:
            self.update(data)

    def __setitem__(self, key, value):
        self._store[key.lower()] = value

    def __getitem__(self, key):
        return self._store[key.lower()]

    def __delitem__(self, key):
        del self._store[key.lower()]

    def __iter__(self):
        return iter(self._store)

    def __len__(self):
        return len(self._store)


env = EnvMap({"HOST": "localhost", "PORT": "8080"})
print(env["host"])   # localhost  — case insensitive
print(env["PORT"])   # 8080
```

---

## 10. Structural Subtyping — `Protocol` (Python 3.8+)

### Must Know

`Protocol` enables **structural subtyping** (duck typing made explicit).  
A class satisfies a `Protocol` **without inheriting from it** — it just needs to have the right methods.

Java's `interface` is **nominal** — must explicitly `implements`. Python `Protocol` is **structural** — just have the methods.

### How It Works

```python
from typing import Protocol, runtime_checkable

@runtime_checkable
class Drawable(Protocol):
    def draw(self) -> None:
        ...

    def resize(self, factor: float) -> None:
        ...


class Circle:
    def draw(self) -> None:
        print("Drawing circle")

    def resize(self, factor: float) -> None:
        self.radius *= factor


class Square:
    def draw(self) -> None:
        print("Drawing square")

    def resize(self, factor: float) -> None:
        self.side *= factor


# Neither Circle nor Square inherits from Drawable!
# But they satisfy the Protocol structurally
def render_all(shapes: list["Drawable"]) -> None:
    for shape in shapes:
        shape.draw()

render_all([Circle(), Square()])   # Works! Duck typing with type safety

# @runtime_checkable enables isinstance checks
c = Circle()
print(isinstance(c, Drawable))    # True — has draw and resize
print(isinstance(42, Drawable))   # False — int has no draw

```

### `Protocol` vs `ABC`

| | `ABC` | `Protocol` |
|---|---|---|
| Subtyping | Nominal — must `class MyClass(MyABC):` | Structural — just have the methods |
| Enforcement | Instantiation fails if abstract methods missing | Type checkers only; no runtime enforcement (unless `@runtime_checkable`) |
| Inheritance required | Yes | No |
| Use case | Shared implementation (template method pattern) | Define an interface for type checking only |
| Java equivalent | `abstract class` or `interface` with `implements` | Closest: Go interfaces or Kotlin structural types |

---

## 11. `@dataclass` — The Modern Way

### Must Know

`@dataclass` auto-generates `__init__`, `__repr__`, `__eq__` (and optionally `__hash__`, `__lt__`, etc.) from field annotations. Introduced in Python 3.7 — the standard for data-holding classes.

### Basic Usage

```python
from dataclasses import dataclass, field
from typing import ClassVar

@dataclass
class Point:
    x: float
    y: float
    label: str = "unnamed"   # Default value


p1 = Point(1.0, 2.0)
p2 = Point(1.0, 2.0)
p3 = Point(3.0, 4.0, label="origin")

print(p1)              # Point(x=1.0, y=2.0, label='unnamed')  — __repr__
print(p1 == p2)        # True  — __eq__ compares all fields
print(p1 == p3)        # False
print(p1 is p2)        # False  — different objects

# Unhashable by default (mutable + __eq__ defined)
# {p1: "value"}  # TypeError: unhashable type: 'Point'
```

### `frozen=True` — Immutable Dataclass

```python
@dataclass(frozen=True)
class FrozenPoint:
    x: float
    y: float


fp = FrozenPoint(1.0, 2.0)
# fp.x = 5.0    # FrozenDataclassError: cannot assign to field 'x'

# frozen=True makes the class hashable!
d = {fp: "origin"}    # Works — frozen dataclass is hashable
s = {fp}              # Works

print(hash(fp))    # Consistent hash based on (x, y)
```

### `field()` — Fine-Grained Control

```python
from dataclasses import dataclass, field
import time

@dataclass
class Order:
    item: str
    quantity: int

    # Field with factory — avoids mutable default trap
    tags: list[str] = field(default_factory=list)  # Fresh list each time
    metadata: dict = field(default_factory=dict)

    # Exclude from __repr__ and __eq__ and __init__
    created_at: float = field(default_factory=time.time, repr=False, compare=False)

    # init=False — not in __init__, set in __post_init__
    total: float = field(init=False, repr=True)

    def __post_init__(self):
        """Called after __init__ — for derived fields and validation."""
        if self.quantity <= 0:
            raise ValueError(f"Quantity must be positive, got {self.quantity}")
        self.total = self.quantity * 10.0   # Computed field (price hardcoded for demo)


o = Order("Widget", 5, tags=["urgent"])
print(o)
# Order(item='Widget', quantity=5, tags=['urgent'], metadata={}, total=50.0)

# Default factory prevents mutable default trap
o1 = Order("A", 1)
o2 = Order("B", 2)
o1.tags.append("tag1")
print(o1.tags)   # ['tag1']
print(o2.tags)   # []  — independent list, not shared!
```

### `ClassVar` — Class-Level Fields

```python
from dataclasses import dataclass
from typing import ClassVar

@dataclass
class Config:
    VERSION: ClassVar[str] = "1.0"   # Class variable — not in __init__ or __eq__
    host: str = "localhost"
    port: int = 8080


c1 = Config()
c2 = Config("production", 443)
print(Config.VERSION)   # 1.0
print(c1.VERSION)       # 1.0 — accessible from instance too
```

### `@dataclass` Parameters Summary

```python
@dataclass(
    init=True,       # Generate __init__
    repr=True,       # Generate __repr__
    eq=True,         # Generate __eq__ and __ne__
    order=False,     # Generate __lt__, __le__, __gt__, __ge__ (requires eq=True)
    frozen=False,    # Make immutable (enables __hash__)
    unsafe_hash=False,  # Force __hash__ even if mutable (not recommended)
    slots=False,     # Generate __slots__ (Python 3.10+)
    kw_only=False,   # All fields keyword-only in __init__ (Python 3.10+)
    match_args=True, # Generate __match_args__ for structural pattern matching (Python 3.10+)
)
class MyData:
    ...
```

### `@dataclass` vs `NamedTuple` vs Plain Class

| | `@dataclass` | `NamedTuple` | Plain class |
|---|---|---|---|
| Mutable | Yes (default) | No | Yes |
| Hashable | With `frozen=True` | Yes | Only with `__hash__` |
| Tuple unpacking | No | Yes — `a, b = point` | No |
| Indexed access | No | Yes — `point[0]` | No |
| Default values | Yes | Yes | Manual |
| Inheritance | Full | Limited | Full |
| Use case | Mutable records, configuration, DTOs | Lightweight immutable records, dict keys | Business logic objects |

### Java Developer Bridge

| Java | Python |
|---|---|
| POJO / JavaBean | `@dataclass` |
| Lombok `@Data` | `@dataclass` |
| Lombok `@Value` (immutable) | `@dataclass(frozen=True)` |
| Java 16+ `record` | `@dataclass(frozen=True)` or `NamedTuple` |
| `equals()` + `hashCode()` | Auto-generated by `@dataclass` |
| Builder pattern | `@dataclass` with defaults + `field()` |
| `null` field | `Optional[T] = None` or just `field: T = None` |
| Validated constructor | `__post_init__` |

---

## 12. Complete Dunder Reference Card

| Dunder | Called by | Purpose |
|---|---|---|
| `__init__` | `MyClass(args)` | Initialize after creation |
| `__new__` | `MyClass(args)` | Create the instance (before __init__) |
| `__del__` | Garbage collection | Destructor (rarely needed) |
| `__repr__` | `repr(obj)`, REPL, logging | Unambiguous developer string |
| `__str__` | `str(obj)`, `print(obj)` | User-friendly string |
| `__format__` | `format(obj, spec)`, f-string | Custom format spec |
| `__bytes__` | `bytes(obj)` | Byte representation |
| `__bool__` | `bool(obj)`, `if obj:` | Truthiness |
| `__len__` | `len(obj)` | Length; fallback for `__bool__` |
| `__hash__` | `hash(obj)`, dict key, set member | Hash value |
| `__eq__` | `obj == other` | Equality |
| `__ne__` | `obj != other` | Inequality (auto from `__eq__`) |
| `__lt__` | `obj < other` | Less than |
| `__le__` | `obj <= other` | Less than or equal |
| `__gt__` | `obj > other` | Greater than |
| `__ge__` | `obj >= other` | Greater than or equal |
| `__add__` | `obj + other` | Addition |
| `__radd__` | `other + obj` (other doesn't know) | Reflected addition |
| `__iadd__` | `obj += other` | In-place addition |
| `__sub__`, `__mul__`, `__truediv__` | `-`, `*`, `/` | Arithmetic |
| `__floordiv__` | `obj // other` | Floor division |
| `__mod__` | `obj % other` | Modulo |
| `__pow__` | `obj ** other` | Power |
| `__neg__` | `-obj` | Negation (unary) |
| `__abs__` | `abs(obj)` | Absolute value |
| `__getitem__` | `obj[key]` | Item access |
| `__setitem__` | `obj[key] = val` | Item assignment |
| `__delitem__` | `del obj[key]` | Item deletion |
| `__contains__` | `item in obj` | Membership test |
| `__iter__` | `iter(obj)`, `for x in obj:` | Return iterator |
| `__next__` | `next(obj)` | Next value from iterator |
| `__reversed__` | `reversed(obj)` | Reverse iterator |
| `__call__` | `obj(args)` | Callable object |
| `__enter__` | `with obj as x:` | Context manager enter |
| `__exit__` | End of `with` block | Context manager exit |
| `__getattr__` | `obj.name` (normal lookup fails) | Fallback attribute access |
| `__setattr__` | `obj.name = val` | Attribute assignment |
| `__delattr__` | `del obj.name` | Attribute deletion |
| `__getattribute__` | `obj.name` (every access) | Intercept all attribute access |
| `__class_getitem__` | `MyClass[int]` | Generic subscript |
| `__init_subclass__` | When class is subclassed | Hook on subclass creation |

---

## 13. Hot Interview Q&A

**Q: What is the difference between `__repr__` and `__str__`?**  
A: `__repr__` is for developers — it should be unambiguous, ideally `eval()`-able, and is used in the REPL, `repr()`, and when objects appear inside containers. `__str__` is for end users — used by `print()`, `str()`, and f-strings. Every class should define `__repr__`. If only `__repr__` is defined, `str()` falls back to it. If only `__str__` is defined, `repr()` falls back to the default `<ClassName at 0x...>`.

**Q: Why does defining `__eq__` make a class unhashable?**  
A: When you define `__eq__`, Python 3 automatically sets `__hash__ = None`. This enforces the contract: if `a == b`, then `hash(a) == hash(b)`. Since a user-defined `__eq__` changes what "equal" means, the old hash (based on `id()`) would violate this contract. Python forces you to explicitly define `__hash__` if you want the object to remain hashable after customizing equality.

**Q: What is MRO and how does Python resolve it?**  
A: MRO (Method Resolution Order) determines which method is called in a class hierarchy. Python uses the C3 linearization algorithm: depth-first, left-to-right traversal of the inheritance graph, respecting the declared order and ensuring each class appears only once and parents appear after all their subclasses. You can inspect it with `MyClass.__mro__`.

**Q: What is the difference between `@classmethod` and `@staticmethod`?**  
A: `@classmethod` receives the class (`cls`) as its first argument and is used for factory methods and alternative constructors — it works correctly in subclasses because `cls` refers to the actual subclass. `@staticmethod` receives no implicit argument and is just a regular function grouped in the class namespace for logical organization — no access to class or instance state.

**Q: What does `frozen=True` do in a dataclass?**  
A: It prevents modification of fields after creation (raises `FrozenInstanceError`) and enables `__hash__` generation, making the dataclass usable as a dict key or set member. It's the Python equivalent of a Java `record` or immutable value object.

**Q: When would you use `__slots__`?**  
A: When creating a large number of instances (thousands or millions) and memory is a concern — `__slots__` replaces the per-instance `__dict__` with fixed-size slots, saving roughly 50% memory per object. Also useful to prevent accidental attribute creation. The trade-off is losing the ability to add attributes dynamically.

**Q: What is the difference between `Protocol` and `ABC`?**  
A: `ABC` uses nominal subtyping — a class must explicitly inherit from the ABC. `Protocol` uses structural subtyping — a class satisfies a `Protocol` simply by having the required methods, with no inheritance needed. `ABC` is for shared implementation (template method); `Protocol` is for defining an interface for type-checking without coupling.

**Q: What does `super()` call in multiple inheritance?**  
A: In Python, `super()` does not necessarily call the direct parent class. It calls the **next class in the MRO**. In diamond inheritance, this ensures each class in the hierarchy is called exactly once if all classes use cooperative `super()` and pass `**kwargs` up the chain.

**Q: What is `NotImplemented` (not `NotImplementedError`) and when do you return it?**  
A: `NotImplemented` is a singleton value returned from dunder methods (like `__add__`, `__eq__`) to signal that the operation is not supported for this type. Python then tries the reflected operation on the other operand. Raising `NotImplementedError` instead blocks this fallback and is incorrect for operator methods.

**Q: What is `__post_init__` in a dataclass?**  
A: `__post_init__` is called automatically after the generated `__init__` finishes. It's used for field validation, computing derived fields marked with `field(init=False)`, and any initialization logic that can't be expressed as a simple default value.

---

## 14. Final Revision Checklist

### Dunder Methods

- [ ] I can explain `__repr__` vs `__str__` and when each is called
- [ ] I know defining `__eq__` without `__hash__` makes the class unhashable
- [ ] I can implement `__eq__` + `__hash__` correctly using a tuple hash
- [ ] I know what `NotImplemented` is and why to return it (not raise) in operator dunders
- [ ] I can implement `__len__`, `__getitem__`, `__contains__`, `__iter__` to make a custom container
- [ ] I know `__new__` creates before `__init__` initializes — used for Singleton

### Properties and Class Methods

- [ ] I never write `get_x()` / `set_x()` — I use `@property`
- [ ] I can add validation in a `@property` setter
- [ ] I know `@classmethod` receives `cls` — subclass-friendly factory; `@staticmethod` receives nothing

### `__slots__`

- [ ] I can explain the memory benefit of `__slots__` and when to use it
- [ ] I know `__slots__` prevents dynamic attribute creation and removes `__dict__`

### Inheritance

- [ ] I can explain MRO with a diamond inheritance example
- [ ] I know `super()` follows MRO, not necessarily the direct parent
- [ ] I can write cooperative multiple inheritance with `**kwargs` passed up the chain

### ABC and Protocol

- [ ] I know `ABC` is nominal (must inherit); `Protocol` is structural (just have the methods)
- [ ] I can write a simple `Protocol` and know it works without inheritance

### `@dataclass`

- [ ] I can write a `@dataclass` with defaults, `field(default_factory=...)`, and `__post_init__`
- [ ] I know `frozen=True` makes it immutable and hashable
- [ ] I know `ClassVar` fields are excluded from `__init__` and `__eq__`
- [ ] I know `field(default_factory=list)` solves the mutable default problem for dataclasses
- [ ] I can compare `@dataclass` vs `NamedTuple` and choose the right one

### Java Developer Reminders

- [ ] Python has no `private` — use `_name` (convention) and `__name` (name mangling)
- [ ] Python supports multiple inheritance of classes — Java does not
- [ ] `super()` in Python follows MRO, not just the direct parent
- [ ] `@dataclass` is Python's Lombok — replaces manual `equals`, `hashCode`, `toString`, constructors
- [ ] `Protocol` is structural typing — closest to Go interfaces, not Java interfaces

---

*File 5 of 7 — Group 1: Starter Path*  
*Next: Python-Collections-Comprehensions-Iteration-Gold-Sheet.md*
