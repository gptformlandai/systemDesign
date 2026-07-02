# Python Core Hot Interview Master Sheet

Target: backend Python interviews for a Java developer aiming at MAANG-level depth.

This sheet covers the Python core topics that interviewers repeatedly test:
- How Python executes code: interpreter, bytecode, CPython
- Variables, names, objects, and references
- Memory model: stack frame equivalent, heap, reference counting
- Types, identity, equality, and mutability
- `None`, truthiness, and Python's type system
- OOP: classes, `self`, constructors, `__init__`, inheritance, MRO
- `equals`/`hashCode` equivalent: `__eq__` and `__hash__`
- Exceptions: hierarchy, checked vs unchecked, `try/except/else/finally`
- Garbage collection and memory leaks
- Python tools and ecosystem basics
- Rapid hot questions and traps for Java developers

How to use this:
- Read the must-know answer first.
- Read the Java bridge next — this is where most Java developers go wrong.
- Read the trap.
- Say the answer out loud in 30-60 seconds.
- Type important code snippets at least once.

---

## 1. Interview Priority Meter

| Area | Priority | What They Usually Test |
|---|---:|---|
| Python execution model | Very high | Can you explain how Python runs? |
| Name binding vs typed variables | Very high | Variable semantics, reference vs object |
| Mutability | Very high | List vs tuple, dict mutability, function default trap |
| Identity vs equality | Very high | `is` vs `==`, interning, dict key safety |
| OOP and `self` | Very high | Class design, `__init__`, inheritance |
| `__eq__` and `__hash__` | Very high | Dict/set key correctness, consistency |
| Exception handling | High | Hierarchy, `else`, `finally`, custom exceptions |
| `None` and truthiness | High | Common logic bug source |
| Garbage collection / GIL | High | Internals, memory, concurrency awareness |
| LEGB scope | High | Closure traps, `global`, `nonlocal` |
| Functions as objects | High | First-class functions, closures, lambdas |
| Typing and hints | Medium-high | Modern Python interview expectation |
| Comprehensions and generators | High | Lazy evaluation, memory efficiency |
| Pass-by-object-reference | Very high | Java developer classic mistake |
| Python tools | Medium | `pip`, virtualenv, `PYTHONPATH`, `__init__.py` |

---

## 2. Python Execution Model

### How Python Runs Code

```text
Source code (.py)
    -> Python parser
Abstract Syntax Tree (AST)
    -> CPython compiler
Bytecode (.pyc in __pycache__)
    -> CPython interpreter (PVM)
Executed line by line
```

### Key Terms

| Term | Meaning |
|---|---|
| CPython | The standard, reference implementation of Python written in C |
| Python | The language specification |
| `python` or `python3` | The CPython interpreter binary |
| Bytecode | Intermediate representation compiled from `.py` source |
| `.pyc` file | Cached compiled bytecode in `__pycache__/` |
| PVM | Python Virtual Machine — the loop inside CPython that executes bytecode |
| PyPy | Alternative Python implementation with a JIT compiler |

### Python Execution Flow

```text
.py file  ->  CPython compiler  ->  .pyc bytecode  ->  PVM executes bytecode
```

Default CPython should not be treated like a HotSpot-style JIT runtime. PyPy has a JIT, and Python 3.13+ has experimental CPython JIT work, but normal production answers should not assume JVM-like hot-path compilation.

### Strong Interview Answer

```text
Python source files are compiled by CPython into bytecode, which is cached in .pyc files.
The bytecode is then executed by the Python Virtual Machine, a loop inside the CPython
interpreter. For normal default CPython, I do not assume HotSpot-style JIT optimization
of hot paths. PyPy has a JIT, and modern CPython has experimental JIT work, but that is
version/build-specific and not the baseline interview answer.
```

### Java Developer Bridge

```text
Similar to Java:
  Both Java and Python compile source to bytecode before executing.
  Both have a VM that runs bytecode.

Different in Python:
  Default CPython does not give the same always-on HotSpot-style JIT assumption as the JVM.
  Python bytecode execution is generally slower for CPU-heavy pure Python work.
  Python compiles at import time, not as a separate build step like javac.

Does not exist in Python:
  A universal JVM-style JIT assumption across standard CPython deployments.
  JDK / JRE / JVM distinction. Python has one runtime: CPython (or PyPy/Jython/etc.).
  ClassLoader subsystem with parent delegation model.

Pythonic replacement:
  PyPy if JIT performance is needed.
  For most backend use, CPython is the default and performance is managed at the architecture
  level (async, caching, horizontal scaling) rather than JIT-level optimization.

Interview trap for Java developers:
  Saying "Python compiles to bytecode just like Java so they are equivalent at runtime."
  Default CPython bytecode execution should not be treated like JVM HotSpot JIT execution.
  Java bytecode is JIT-compiled by the JVM into native machine code for hot paths. Python
  is generally slower for pure Python CPU work unless you change the runtime strategy,
  use native/vectorized libraries, or validate a version/build-specific JIT path.
```

### Python Tools To Know

| Tool | Use |
|---|---|
| `python3` | Run Python scripts or interactive REPL |
| `python3 -m py_compile file.py` | Compile to bytecode without running |
| `python3 -m dis file.py` | Disassemble bytecode for inspection |
| `python3 -m cProfile script.py` | Profile execution |
| `pip` | Install packages from PyPI |
| `virtualenv` / `venv` | Isolated package environments |
| `python3 -m venv .venv` | Create a virtual environment |
| `pip install -r requirements.txt` | Install from lockfile |
| `poetry` | Modern dependency and packaging manager |
| `pytest` | Test runner |
| `mypy` | Static type checker |

---

## 3. Variables, Names, And Objects

### The Most Important Mental Shift For Java Developers

In Java:

```java
int count = 5;
String name = "Alice";
```

- `count` is a typed box holding a primitive integer value.
- `name` is a typed reference pointing to a String object.

In Python:

```python
count = 5
name = "Alice"
```

- `count` is just a **name** in a namespace.
- That name currently **points to** the integer object `5`.
- The name has no type. The **object** has a type.
- Reassigning the name to a different type is normal and valid.

### Name Binding

```python
count = 5        # name 'count' binds to int object 5
count = "hello"  # name 'count' now binds to str object "hello"
count = [1, 2]   # name 'count' now binds to a list object
```

No error. The object types change. The name just points to whatever object exists.

### Multiple Names, Same Object

```python
a = [1, 2, 3]
b = a            # b and a point to the SAME list object
b.append(4)
print(a)         # [1, 2, 3, 4]
```

Mutating through `b` is visible via `a` because both names point to the same object.

### Strong Interview Answer

```text
In Python, variables are names bound to objects. The name has no type — the object has a type.
When you assign a name, Python creates or reuses an object and makes the name point to it.
Multiple names can point to the same object. If the object is mutable, mutations through any
name are visible to all other names pointing to that object.
```

### Java Developer Bridge

```text
Similar to Java:
  Java reference variables and Python names both point to objects on the heap.
  Both can have multiple references/names pointing to the same object.

Different in Python:
  In Java, a reference variable has a declared type: List<String> list.
  In Python, names have no type. Only objects have types.
  Python allows reassigning a name to a completely different type without error.

Does not exist in Python:
  Primitive types (int, double, boolean in Java sense).
  Typed variable declarations.
  Static typing enforced at runtime.

Pythonic replacement:
  Use type hints for documentation and static analysis, but not for runtime enforcement.
  count: int = 5  # hint only; int object is still an object not a primitive

Interview trap for Java developers:
  Saying Python int is a primitive. Python int is always an object. Everything in Python
  is an object, including integers, floats, booleans, and functions.
```

---

## 4. Python Memory Model

### Where Things Live

Python does not have a strict heap/stack/metaspace split the way the JVM does.

Conceptually:

| Area | What Lives There |
|---|---|
| Heap (Python managed) | All objects: integers, strings, lists, dicts, class instances, functions |
| Frame stack | Call stack frames with local name bindings |
| Module namespace (global scope) | Module-level names and their bindings |
| Class namespace | Class attributes and method definitions |
| Built-in namespace | `len`, `print`, `type`, `range`, etc. |

### Call Stack Frame

Every function call creates a frame:

```python
def add(x, y):
    result = x + y
    return result

total = add(3, 4)
```

```text
Frame for add():
  x -> int object 3
  y -> int object 4
  result -> int object 7

Frame for module level:
  total -> int object 7
```

Frames are pushed when a function is called and popped when it returns.

### Object Identity

Every object in Python has:
- A **type** (`type(obj)`)
- An **id** — unique identity number, often the memory address in CPython (`id(obj)`)
- A **value**

```python
x = 42
print(type(x))   # <class 'int'>
print(id(x))     # some integer, e.g. 140234567891760
```

### Reference Counting

CPython uses reference counting as its primary memory management.

Every object has a reference count.
When you assign a name to an object, the reference count increases.
When a name is deleted or rebound, the reference count decreases.
When the count reaches zero, CPython immediately frees the object.

```python
import sys

x = []
print(sys.getrefcount(x))  # 2 (one for x, one for getrefcount's argument)

y = x
print(sys.getrefcount(x))  # 3
```

### Strong Interview Answer

```text
Python's memory model is based on objects on a managed heap. Every object has a type, an
identity, and a reference count. CPython frees objects when their reference count drops to
zero using reference counting. For cycles that reference counting cannot handle, CPython also
runs a cyclic garbage collector. Unlike the JVM, Python does not separate heap into
generations for GC by default in the same way, and there is no metaspace or PermGen.
```

### Java Developer Bridge

```text
Similar to Java:
  Objects live on a managed heap.
  Local name bindings in function calls are frame-scoped.
  Memory is managed by the runtime, not the programmer.

Different in Python:
  Python uses reference counting as primary GC, not a generational GC.
  There is no JVM-style heap/stack/metaspace separation.
  There is no StackOverflowError — Python raises RecursionError.
  Python has a default recursion limit (sys.getrecursionlimit(), usually 1000).

Does not exist in Python:
  JVM heap generations (young/old generation model).
  Metaspace / PermGen.
  Stack overflow error in JVM sense — Python uses RecursionError.

Interview trap for Java developers:
  Saying Python has stack and heap like Java. Python has a call frame stack conceptually,
  but the implementation and terminology is different from JVM memory areas.
```

---

## 5. Identity vs Equality

### `is` vs `==`

| Operator | What It Checks |
|---|---|
| `is` | Identity: are both names pointing to the exact same object? |
| `==` | Equality: do the objects have the same value? (`__eq__` method) |

```python
a = [1, 2, 3]
b = [1, 2, 3]
c = a

print(a == b)   # True  — same value
print(a is b)   # False — different objects in memory
print(a is c)   # True  — same object
```

### `None` Comparison

```python
# Correct
if value is None:
    ...

if value is not None:
    ...

# Avoid for None checks
if value == None:  # works but not idiomatic
    ...
```

Use `is` for `None`, `True`, and `False` comparisons. These are singletons.

### Integer Interning

CPython caches small integers (typically -5 to 256):

```python
a = 100
b = 100
print(a is b)   # True — same cached object

a = 1000
b = 1000
print(a is b)   # False — not cached, separate objects
```

### String Interning

Short strings that look like identifiers are often interned:

```python
a = "hello"
b = "hello"
print(a is b)   # Usually True — interned

a = "hello world"
b = "hello world"
print(a is b)   # May be False — not always interned
```

Never rely on `is` for string value comparison. Always use `==`.

### Strong Interview Answer

```text
In Python, == checks equality by calling __eq__, which compares values. The is operator
checks identity: whether both names point to the exact same object in memory. For None,
True, and False, always use is because they are singletons. For strings and small integers,
CPython interns common values so is can appear to work, but it is an implementation detail
you should never rely on for value comparison.
```

### Java Developer Bridge

```text
Similar to Java:
  Java's == on objects is identity comparison. Python's is is identity comparison.
  Java's .equals() is value comparison. Python's == is value comparison.

Different in Python:
  Python's == for most types calls __eq__, which you can customize.
  Python None is a singleton — use is None, not == None.
  Python has no NullPointerException; accessing attributes on None raises AttributeError.

Does not exist in Python:
  Java's separate equals() and hashCode() methods as required overrides for HashMap.
  Python uses __eq__ and __hash__ dunder methods instead.

Interview trap for Java developers:
  Using == to compare strings like in Java: 'hello' == 'hello'. This works because Python
  == is value comparison. But saying "Python == is like Java ==" is wrong — Java == is
  reference equality for objects. Python == is value equality. This is the opposite of the
  Java default for objects.
```

---

## 6. Mutability

### Mutable vs Immutable Types

| Immutable | Mutable |
|---|---|
| `int` | `list` |
| `float` | `dict` |
| `bool` | `set` |
| `str` | Most class instances by default |
| `tuple` | `bytearray` |
| `frozenset` | |
| `bytes` | |

### What Immutable Means

You cannot change the object's value. You can rebind the name to a new object.

```python
s = "hello"
s = s + " world"    # creates a new string object; s now points to it
```

The original `"hello"` object is not modified. A new `"hello world"` object is created.

### What Mutable Means

You can change the object's content in place.

```python
items = [1, 2, 3]
items.append(4)     # modifies the same list object
print(items)        # [1, 2, 3, 4]
```

### Aliasing Trap

```python
a = [1, 2, 3]
b = a
b.append(4)
print(a)    # [1, 2, 3, 4] — both names point to the same object
```

To make a copy:

```python
b = a[:]          # shallow copy
b = list(a)       # shallow copy
import copy
b = copy.deepcopy(a)  # deep copy
```

### Immutable Tuples Can Contain Mutable Objects

```python
t = ([1, 2], [3, 4])
t[0].append(99)
print(t)    # ([1, 2, 99], [3, 4])
```

The tuple itself did not change (same two list objects inside). The list inside changed.

### Strong Interview Answer

```text
In Python, mutability refers to whether an object's content can change after creation.
Immutable types like int, str, and tuple cannot be modified in place. Mutable types like
list, dict, and set can. When two names point to the same mutable object, a mutation through
one name is visible through the other. This is different from creating a new binding.
Shallow and deep copies are used to avoid unintended aliasing.
```

### Java Developer Bridge

```text
Similar to Java:
  Java String is immutable. Python str is immutable. Both create new objects on modification.
  Java has the concept of defensive copying for mutable objects. Same need exists in Python.

Different in Python:
  Python has no final keyword on variables. Immutability is a property of the object type,
  not the variable declaration.
  Python tuples are immutable sequences but can contain mutable objects inside.

Does not exist in Python:
  final keyword for variables or fields.
  Compile-time enforcement of immutability on custom classes.

Pythonic replacement:
  Use frozen=True on dataclasses for immutable value objects.
  Use NamedTuple for immutable record-like structures.
  Use __slots__ with careful design for lightweight classes.

Interview trap for Java developers:
  Assuming tuple is like Java's unmodifiable list. A tuple is immutable at the tuple level,
  but if it holds mutable objects, those inner objects can still be mutated.
```

---

## 7. None, Truthiness, And Type Coercion

### `None`

`None` is Python's null. It is a singleton.

```python
x = None

if x is None:
    print("no value")
```

`None` is not `0`, not `False`, not `""`. It is its own type: `NoneType`.

### Truthiness

Every Python object is truthy or falsy. You do not need an explicit boolean check.

Falsy values:
- `None`
- `False`
- `0`, `0.0`, `0j`
- `""` (empty string)
- `[]` (empty list)
- `{}` (empty dict)
- `()` (empty tuple)
- `set()` (empty set)

```python
items = []

if not items:
    print("empty list")
```

Truthy: anything not in the falsy list.

### Checking Type

```python
x = 42
print(type(x))           # <class 'int'>
print(isinstance(x, int))  # True
print(isinstance(x, (int, float)))  # True — checks against tuple of types
```

Prefer `isinstance` over `type(x) == int` in production and interviews.

### Strong Interview Answer

```text
Python's None is a singleton representing the absence of a value. It is always compared with
is, not ==. Python evaluates any object in a boolean context using truthiness rules. Empty
sequences, zero, None, and False are all falsy. isinstance is the idiomatic way to check
types because it handles subclasses correctly.
```

### Java Developer Bridge

```text
Similar to Java:
  None is conceptually like Java null.

Different in Python:
  Python None cannot cause a NullPointerException. Calling a method on None raises
  AttributeError.
  Python uses truthiness in conditions rather than requiring explicit boolean expressions.
  Java requires boolean expressions: if (list.isEmpty()) — Python: if not items.

Does not exist in Python:
  NullPointerException. Python raises AttributeError when accessing attributes on None.
  Checked null handling via Optional<T> at runtime. Python type hints Optional[T]
  are static analysis only.

Interview trap for Java developers:
  Writing if items != None or if items != [] explicitly. The idiomatic form is if items
  (truthy) or if not items (falsy).
```

---

## 8. OOP: Classes, `self`, And `__init__`

### Class Definition

```python
class User:
    def __init__(self, user_id: str, name: str) -> None:
        self.user_id = user_id
        self.name = name

    def greet(self) -> str:
        return f"Hello, {self.name}"
```

### `self`

`self` is the reference to the current instance, like `this` in Java.

Python requires `self` to be declared explicitly as the first parameter of instance methods. It is not a keyword — you could technically name it anything — but `self` is the universal convention.

### Object Creation

```python
user = User("u1", "Aravind")
print(user.greet())   # Hello, Aravind
```

No `new` keyword. Calling the class creates an instance.

### Instance Variables vs Class Variables

```python
class Counter:
    count = 0              # class variable — shared by all instances

    def __init__(self) -> None:
        self.value = 0     # instance variable — unique per instance

    def increment(self) -> None:
        self.value += 1
        Counter.count += 1
```

Class variables are shared. Modifying through `self` creates an instance variable shadowing the class variable.

```python
c1 = Counter()
c2 = Counter()
c1.count = 10       # creates instance variable 'count' on c1 only
print(Counter.count)  # 0 — class variable unchanged
print(c1.count)     # 10 — instance variable shadows class variable
print(c2.count)     # 0 — c2 still sees class variable
```

### Strong Interview Answer

```text
Python classes are defined with the class keyword. Instances are created by calling the
class. The __init__ method initializes the instance. self is the reference to the instance
and must be the first parameter of every instance method. Instance variables are set on self
and are unique per instance. Class variables are shared across all instances and can be
accessed via the class or via self, but reassigning via self creates a new instance variable
that shadows the class variable.
```

### Java Developer Bridge

```text
Similar to Java:
  __init__ is equivalent to a Java constructor.
  Instance variables set on self are equivalent to Java instance fields.
  Class variables are equivalent to Java static fields.
  Calling a class like User() is equivalent to new User() in Java.

Different in Python:
  No new keyword.
  self must be declared explicitly in every instance method.
  Python has no access modifiers (public, private, protected). Convention uses _ prefix for
  internal/private and __ for name-mangled pseudo-private.
  Class variables are shared but can be shadowed per instance in a way Java static fields
  cannot be.

Does not exist in Python:
  Access modifiers enforced by the compiler.
  Method overloading by different parameter types. Python allows only one method with a name;
  use default parameters or *args/**kwargs for flexible signatures.

Interview trap for Java developers:
  Forgetting self in method definitions: def greet(): instead of def greet(self):
  Missing self causes TypeError when called.
  Confusing class variables with instance variables when using self.varname.
```

---

## 9. `__eq__` And `__hash__`

### Why They Matter

In Python, `dict` and `set` use `__hash__` to find the bucket and `__eq__` to resolve collisions — exactly like Java's `hashCode` and `equals` for `HashMap` and `HashSet`.

The contract:
- If `a == b` then `hash(a) == hash(b)`.
- If two objects are equal, they must produce the same hash.
- If an object is used as a dict key or in a set, it must be hashable.

### Default Behavior

By default, Python uses object identity for `__eq__` and `id`-based `__hash__`.

When you define `__eq__`, Python automatically sets `__hash__` to `None`, making the class unhashable. You must define both.

```python
class User:
    def __init__(self, user_id: str) -> None:
        self.user_id = user_id

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, User):
            return NotImplemented
        return self.user_id == other.user_id

    def __hash__(self) -> int:
        return hash(self.user_id)
```

### Mutable Object As Dict Key Trap

```python
class BadKey:
    def __init__(self, val: int) -> None:
        self.val = val

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, BadKey):
            return NotImplemented
        return self.val == other.val

    def __hash__(self) -> int:
        return hash(self.val)

key = BadKey(1)
d = {key: "A"}
key.val = 2        # mutate key after insertion
print(d[key])      # KeyError — hash changed, wrong bucket
```

### Strong Interview Answer

```text
In Python, dict and set use __hash__ to find the bucket and __eq__ to check equality within
the bucket. The contract is the same as Java: equal objects must have the same hash. If you
define __eq__, Python sets __hash__ to None, making the class unhashable by default, so you
must also define __hash__. Mutable objects should not be used as dict keys because mutation
can change the hash and make existing entries unreachable.
```

### Java Developer Bridge

```text
Similar to Java:
  __hash__ corresponds to Java's hashCode().
  __eq__ corresponds to Java's equals().
  The contract is identical: equals objects must have equal hash codes.
  The mutable-key trap exists in both Java HashMap and Python dict.

Different in Python:
  Python enforces the contract by setting __hash__ to None when __eq__ is defined
  without __hash__.
  Python's hash() is built-in. Java uses .hashCode() as a method call.
  Python frozenset and tuple are hashable. Python list and dict are not.

Interview trap for Java developers:
  Defining __eq__ without __hash__ and then trying to use instances as dict keys.
  Python will raise TypeError: unhashable type.
```

---

## 10. Inheritance And MRO

### Basic Inheritance

```python
class Animal:
    def speak(self) -> str:
        return "..."

class Dog(Animal):
    def speak(self) -> str:
        return "Woof"

class Cat(Animal):
    def speak(self) -> str:
        return "Meow"
```

### `super()`

```python
class Vehicle:
    def __init__(self, brand: str) -> None:
        self.brand = brand

class Car(Vehicle):
    def __init__(self, brand: str, model: str) -> None:
        super().__init__(brand)
        self.model = model
```

### Multiple Inheritance And MRO

Python supports multiple inheritance. Method Resolution Order (MRO) determines which method is called.

```python
class A:
    def hello(self) -> str:
        return "A"

class B(A):
    def hello(self) -> str:
        return "B"

class C(A):
    def hello(self) -> str:
        return "C"

class D(B, C):
    pass

d = D()
print(d.hello())   # B

print(D.__mro__)
# (<class 'D'>, <class 'B'>, <class 'C'>, <class 'A'>, <class 'object'>)
```

Python uses the C3 linearization algorithm for MRO. Read left to right, parents before base.

### `object`

All Python classes implicitly inherit from `object`. Every class has access to `__init__`, `__eq__`, `__hash__`, `__str__`, `__repr__`, etc. from `object`.

### Strong Interview Answer

```text
Python supports single and multiple inheritance. The Method Resolution Order determines
which version of a method is called when a class inherits from multiple parents. Python uses
the C3 linearization algorithm to compute MRO, which you can inspect with Class.__mro__.
super() is used to call parent methods and follows the MRO, not the direct parent, which
is important in multiple inheritance scenarios.
```

### Java Developer Bridge

```text
Similar to Java:
  Python super() is equivalent to Java's super keyword.
  Python's implicit object base is equivalent to Java's implicit Object base.
  Overriding parent methods works the same way.

Different in Python:
  Python supports multiple inheritance natively. Java does not for classes.
  Python's MRO (C3 linearization) is different from Java's single-parent chain.
  Python has no @Override annotation or compile-time override check.

Does not exist in Python:
  Java interface-style multiple implementation inheritance. Python uses Protocols and ABCs.
  @Override annotation to signal intentional override.

Interview trap for Java developers:
  Not calling super().__init__() in a subclass constructor when the parent has initialization
  logic. In Python, unlike Java, the parent __init__ is not automatically called.
```

---

## 11. Exceptions

### Exception Hierarchy

All Python exceptions inherit from `BaseException`.
Most you handle inherit from `Exception`.

```text
BaseException
├── SystemExit
├── KeyboardInterrupt
├── GeneratorExit
└── Exception
    ├── ArithmeticError (ZeroDivisionError, OverflowError)
    ├── LookupError (IndexError, KeyError)
    ├── TypeError
    ├── ValueError
    ├── AttributeError
    ├── NameError
    ├── OSError (FileNotFoundError, PermissionError)
    ├── RuntimeError (RecursionError, NotImplementedError)
    └── StopIteration
```

### try / except / else / finally

```python
def read_config(path: str) -> dict:
    try:
        with open(path) as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Config not found: {path}")
        return {}
    except json.JSONDecodeError as e:
        print(f"Invalid JSON: {e}")
        return {}
    else:
        # Runs only if no exception occurred in try
        print("Config loaded successfully")
    finally:
        # Always runs
        print("Done reading config")
```

The `else` clause is a Python-specific addition. It runs only when the `try` block completed without raising an exception.

### Raising Exceptions

```python
def validate_age(age: int) -> None:
    if age < 0:
        raise ValueError(f"Age cannot be negative: {age}")
```

### Custom Exceptions

```python
class BookingError(Exception):
    def __init__(self, message: str, booking_id: str) -> None:
        super().__init__(message)
        self.booking_id = booking_id

raise BookingError("Room unavailable", booking_id="B101")
```

### No Checked Exceptions

Python has no checked exceptions. Every exception in Python is unchecked (runtime).

### Strong Interview Answer

```text
Python exceptions all inherit from BaseException. Exceptions you handle in application code
typically inherit from Exception. The try/except/else/finally block handles errors. The else
clause is Python-specific and runs only when no exception occurred. Python has no checked
exceptions — all exceptions are unchecked, meaning the compiler does not force you to handle
them. Custom exceptions inherit from Exception and add context.
```

### Java Developer Bridge

```text
Similar to Java:
  try/finally in Java has the same purpose as Python try/finally.
  Custom exceptions inherit from a base class (Exception in both).
  raise is equivalent to Java's throw.
  except ExceptionType as e is equivalent to catch(ExceptionType e).

Different in Python:
  Python adds an else clause — runs when no exception occurred.
  Python has no checked exceptions. Java distinguishes checked and unchecked.
  Python's exception hierarchy is simpler. No IOException, SQLException hierarchy.

Does not exist in Python:
  Checked exceptions. Python has no compiler-enforced exception handling.
  Multi-catch syntax like Java's catch (IOException | SQLException e).
  Python equivalent: except (IOError, ValueError) as e — uses tuple.

Interview trap for Java developers:
  Catching bare Exception without re-raising when you cannot handle it.
  Catching BaseException accidentally catches SystemExit and KeyboardInterrupt.
  Never catch bare BaseException unless you know what you are doing.
```

---

## 12. Garbage Collection And Memory Leaks

### Reference Counting

CPython counts references to each object.

```python
import sys

x = [1, 2, 3]
y = x
print(sys.getrefcount(x))  # at least 3 (x, y, getrefcount arg)

del y
print(sys.getrefcount(x))  # decreases
```

When count reaches zero, memory is freed immediately.

### Cyclic GC

Reference counting fails for cycles:

```python
class Node:
    def __init__(self) -> None:
        self.ref = None

a = Node()
b = Node()
a.ref = b
b.ref = a

del a
del b
# Neither freed by reference counting alone — cycle!
```

CPython's cyclic garbage collector handles these cycles.

### Memory Leak Patterns In Python

| Pattern | Example |
|---|---|
| Global caches growing forever | Module-level dict accumulating without eviction |
| Unclosed file handles or connections | Not using `with` for files |
| Circular references without `__del__` care | Custom classes with back-references |
| Large objects in closures | Lambda or function holding reference to large data |
| Event listeners not removed | Observer pattern without cleanup |

### `__del__`

Python's `__del__` is called when an object is about to be garbage collected, but it is unreliable for resource cleanup. Use `with` and context managers instead.

### Strong Interview Answer

```text
CPython uses reference counting as its primary memory management. When a reference count
drops to zero, the object is freed immediately. For cycles that reference counting cannot
break, CPython has a cyclic garbage collector. Memory leaks in Python typically occur through
reachable-but-unused objects: global caches that grow without eviction, unclosed resources,
or closures holding large objects. The gc module can diagnose cycles.
```

### Java Developer Bridge

```text
Similar to Java:
  Both Python and Java use a managed heap with garbage collection.
  Both can have memory leaks through reachable-but-unused references.
  Both have mechanisms for detecting leaks (heap dump vs tracemalloc/gc module).

Different in Python:
  Python uses reference counting first; the JVM uses generational GC.
  Python frees objects immediately when the count reaches zero (no GC pause for non-cycles).
  Python's GIL interacts with reference counting in the threading model.

Does not exist in Python:
  JVM heap generations (young/old) in CPython GC.
  JFR / heap dump tools built into the runtime. Python uses tracemalloc and objgraph.

Interview trap for Java developers:
  Assuming Python has no memory leaks because it has GC. Memory leaks happen through
  reachable references just like Java. GC only helps with unreachable objects.
```

---

## 13. Pass-By-Object-Reference

### The Correct Mental Model

Python uses **pass-by-object-reference** (also called pass-by-assignment).

- When you call a function with an argument, the function parameter is a new name bound to the same object.
- If the object is mutable, the function can mutate it and the caller will see the change.
- If the function rebinds the parameter to a new object, the caller does not see the change.

```python
def try_to_replace(items: list) -> None:
    items = [99, 100]    # rebinds local name; caller unchanged

def mutate(items: list) -> None:
    items.append(99)     # mutates the same object; caller sees change

original = [1, 2, 3]

try_to_replace(original)
print(original)          # [1, 2, 3]

mutate(original)
print(original)          # [1, 2, 3, 99]
```

### Strong Interview Answer

```text
Python passes object references by assignment. When you call a function, the argument name
inside the function is bound to the same object as the caller's name. If the object is
mutable and the function mutates it, the caller sees the change. If the function rebinds
its local name to a new object, the caller is unaffected. This is not the same as
Java's pass-by-value for primitives or pass-by-reference as in C++.
```

### Java Developer Bridge

```text
Similar to Java:
  Java passes object references by value. Python passes object references by assignment.
  The behavioral effect is the same: mutations through a received reference are visible to
  the caller; reassignment of the local name is not.

Different in Python:
  Python has no primitive types, so there is no distinction between primitive and reference
  semantics. All values are objects.

Interview trap for Java developers:
  Saying Python passes primitives by value and objects by reference — there are no primitives
  in Python. Every value is an object. The distinction is mutation vs rebinding.
```

---

## 14. Hot Questions And Traps Summary

### Q1. What is the difference between `is` and `==`?

```text
is checks identity: same object in memory.
== checks equality: same value via __eq__.
Always use == for value comparison.
Use is only for None, True, False singletons.
```

### Q2. Why is this a bug?

```python
def add_item(item, items=[]):
    items.append(item)
    return items
```

```text
Default argument values are evaluated once at function definition time.
The same list object is reused across all calls that do not pass items.
Fix: def add_item(item, items=None): if items is None: items = []
```

### Q3. What does `if not items:` mean?

```text
It checks truthiness. Empty list, empty dict, empty string, None, and 0 are all falsy.
This is idiomatic Python. It is equivalent to items.isEmpty() style in Java but more general.
```

### Q4. Is Python pass-by-value or pass-by-reference?

```text
Neither exactly. Python is pass-by-object-reference: arguments receive a reference to the
same object. Mutations are visible to the caller. Rebinding is not.
```

### Q5. What is the GIL?

```text
In default CPython, the Global Interpreter Lock is a mutex that allows only one thread to
execute Python bytecode at a time. This means Python threads do not achieve true CPU
parallelism for pure Python CPU work. For IO-bound work, threads still help because the GIL
is released during IO waits. For CPU-bound parallelism, use multiprocessing, native
extensions that release the GIL, or an explicitly chosen Python 3.13+ free-threaded build.
```

### Q6. How does Python import work?

```text
Python looks for modules in sys.path: the current directory, installed packages, and
paths set by PYTHONPATH. When a package is imported, Python runs its __init__.py.
Modules are cached in sys.modules after first import, so they are not re-executed.
```

### Q7. What is the difference between `__str__` and `__repr__`?

```text
__repr__ is for developers: should be unambiguous and ideally eval()-able to recreate the object.
__str__ is for end users: readable and friendly.
If only __repr__ is defined, Python uses it for both str() and repr().
```

```python
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __repr__(self):
        return f"Point({self.x}, {self.y})"

    def __str__(self):
        return f"({self.x}, {self.y})"
```

### Q8. What is `RecursionError`?

```text
Python has a default recursion limit (sys.getrecursionlimit() = 1000).
Infinite or very deep recursion raises RecursionError.
This is CPython's equivalent of Java's StackOverflowError.
```

### Q9. What is the difference between `list`, `tuple`, and `frozenset`?

| Type | Ordered | Mutable | Hashable |
|---|---|---|---|
| `list` | Yes | Yes | No |
| `tuple` | Yes | No | Yes (if elements hashable) |
| `set` | No | Yes | No |
| `frozenset` | No | No | Yes |

### Q10. What does `@property` do?

```text
@property creates a getter method that is accessed like an attribute.
It is Python's equivalent of Java's getters, but more concise.
```

```python
class Circle:
    def __init__(self, radius: float) -> None:
        self._radius = radius

    @property
    def radius(self) -> float:
        return self._radius

    @radius.setter
    def radius(self, value: float) -> None:
        if value < 0:
            raise ValueError("Radius cannot be negative")
        self._radius = value
```

---

## 15. Final One-Page Revision Checklist

Before the interview, verify you can answer:

- How does CPython execute Python code?
- What is the difference between CPython, Python, and PyPy?
- What is name binding and why does Python not have typed variables?
- Explain identity vs equality in Python.
- What is integer interning and string interning?
- What is the mutable default argument trap?
- What is the aliasing trap with mutable objects?
- Explain `__eq__` and `__hash__` and why they must be consistent.
- What happens if you define `__eq__` without `__hash__`?
- Explain `self` and why Python requires it explicitly.
- What is MRO and why does it matter for multiple inheritance?
- Explain the exception hierarchy and the `else` clause in try/except.
- What is reference counting and when does the cyclic GC run?
- What is the GIL and when does threading help in Python?
- Explain pass-by-object-reference clearly.
- What is the difference between `__str__` and `__repr__`?
- What is `RecursionError` and how is it different from Java's `StackOverflowError`?
