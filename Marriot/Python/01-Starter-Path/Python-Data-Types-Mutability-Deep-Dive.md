# Python Data Types & Mutability Deep Dive

> **Track**: Python Interview Track — Group 1: Starter Path  
> **File**: 3 of 7  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-For-Java-Developers-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| Mutability rules and identity vs equality | ★★★★★ | Java `==` on Strings is already a known trap — Python extends this to all objects |
| Mutable default argument trap | ★★★★★ | **#1 most-tested Python gotcha in interviews** — does not exist in Java |
| `list` internals and `list * n` shared ref | ★★★★☆ | Java `new ArrayList<>()` has no equivalent to `[[]] * 3` |
| `dict` ordering and iteration rules | ★★★★☆ | Java `HashMap` is unordered; Python `dict` is ordered since 3.7 — interviewers ask |
| `tuple` containing mutable — hashability trap | ★★★★☆ | Looks immutable, isn't always hashable |
| `set`/`frozenset` and hashability | ★★★☆☆ | Java `HashSet` is conceptually similar but rules differ |
| `int`/`bool` class hierarchy | ★★★☆☆ | `True + True == 2` surprises Java devs every time |
| `None` semantics and identity test | ★★★☆☆ | Use `is None`, not `== None` — the linter will warn, but interviewers will ask why |
| `bytes` vs `str` distinction | ★★★☆☆ | Java `String` vs `byte[]` — common confusion in networking/encoding code |

---

## 2. The Foundational Rule: Mutability in Python

### Must Know

Everything in Python is an **object**. Every variable is a **name bound to an object**.

```
Java:  int x = 5;          → x IS the value 5 (primitive)
Python: x = 5              → x POINTS TO the int object 5
```

**Mutable**: The object's internal state can be changed after creation.  
**Immutable**: Once created, the object's value can never change. A "change" creates a new object.

### The Two Categories

```
IMMUTABLE (cannot change internal state)    MUTABLE (can change internal state)
─────────────────────────────────────       ─────────────────────────────────────
int, float, complex, bool                   list
str                                         dict
bytes                                       set
tuple (with caveats — see §9)               bytearray
frozenset                                   Custom objects (usually)
NoneType
```

### Why This Matters for Interviews

1. **Mutability determines hashability** — only immutable objects can be dict keys or set members.
2. **Mutable defaults are shared** — the #1 Python interview bug.
3. **Passing to functions** — Python always passes the reference, but whether the caller sees changes depends on whether the object is mutated vs rebound.

### Java Developer Bridge

| Concept | Java | Python |
|---|---|---|
| Primitives | `int`, `long`, `boolean`, `double` — live on stack, fully immutable | **No primitives** — even `5` is an `int` object |
| Immutable types | `String`, `Integer` (wrapper) | `str`, `int`, `float`, `bool`, `tuple`, `frozenset`, `bytes` |
| Mutable types | `StringBuilder`, `ArrayList`, `HashMap` | `list`, `dict`, `set`, `bytearray` |
| Value semantics | primitives naturally have value semantics | Must use immutable types to get value-like behaviour |
| Unmodifiable collections | `Collections.unmodifiableList()` — wrapper | `tuple` is the built-in immutable sequence |

---

## 3. `int` — Integers

### Must Know

- **Arbitrary precision**: Python `int` has no overflow. `2 ** 1000` works. Java `int` is 32-bit; `long` is 64-bit; overflow wraps.
- **Immutable**: Reassigning `x = x + 1` creates a new `int` object and rebinds `x`.
- **Small int cache**: CPython caches integers from **-5 to 256** as singletons. `x is y` is `True` when both are in this range but not guaranteed outside it.

### How It Works (CPython Internals)

```python
# Small int cache — same object
a = 100
b = 100
print(a is b)   # True  — both point to the cached singleton

# Outside cache — different objects
a = 1000
b = 1000
print(a is b)   # False  — two separate int objects
print(a == b)   # True   — value equality still works
```

### Integer Operations

```python
x = 10
print(type(x))          # <class 'int'>
print(isinstance(x, int))  # True
print(x.bit_length())   # 4  (how many bits needed to represent)
print(x.to_bytes(2, 'big'))  # b'\x00\n'  — convert to bytes
print(int.from_bytes(b'\x00\n', 'big'))  # 10

# Arithmetic
print(10 // 3)    # 3   — floor division (Java: 10 / 3 = 3 for ints, same)
print(10 % 3)     # 1   — modulo
print(-10 % 3)    # 2   — DIFFERENT from Java: Python result has sign of divisor
print(2 ** 10)    # 1024 — exponentiation (Java: Math.pow(2, 10))
```

### Trap: `-10 % 3` in Python vs Java

```python
# Python — result has sign of DIVISOR
print(-10 % 3)   # 2   (Python: -10 = 3 * (-4) + 2)

# Java: -10 % 3 = -1   (Java: result has sign of DIVIDEND)
# This matters for hashing, circular buffers, modular arithmetic
```

**Strong Interview Answer**: "Python's `%` operator always returns a non-negative result when the divisor is positive, following mathematical convention. Java's `%` follows C convention and returns a result with the sign of the dividend. For circular buffers or hash functions I always check which behaviour I need."

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Type | `int` (32-bit primitive), `long` (64-bit), `BigInteger` | `int` — arbitrary precision always |
| Overflow | Silent wrap (int), `ArithmeticException` with `Math.addExact` | Never overflows |
| Floor division | `10 / 3 = 3` for ints | `10 // 3 = 3` |
| Modulo with negatives | `-10 % 3 = -1` | `-10 % 3 = 2` |
| Exponentiation | `Math.pow(2, 10)` → double | `2 ** 10` → int |
| Numeric literal separators | `1_000_000` (Java 7+) | `1_000_000` (Python 3.6+) |

---

## 4. `float` — Floating Point

### Must Know

- **IEEE 754 double-precision** — same as Java `double`.
- **Immutable** — same rules as `int`.
- `0.1 + 0.2 != 0.3` in both Python and Java (floating-point representation issue).

### How It Works

```python
# Standard float
x = 3.14
print(type(x))         # <class 'float'>
print(x.is_integer())  # False
print((3.0).is_integer())  # True

# Float precision trap — SAME in Java
print(0.1 + 0.2)       # 0.30000000000000004
print(0.1 + 0.2 == 0.3)  # False

# Use math.isclose for comparisons
import math
print(math.isclose(0.1 + 0.2, 0.3))  # True
print(math.isclose(0.1 + 0.2, 0.3, rel_tol=1e-9))  # True

# Special values
print(float('inf'))    # inf
print(float('-inf'))   # -inf
print(float('nan'))    # nan
print(float('nan') == float('nan'))   # False — NaN != NaN (IEEE 754)
import math
print(math.isnan(float('nan')))       # True
```

### For Decimal Precision Use `decimal.Decimal`

```python
from decimal import Decimal, ROUND_HALF_UP

# Java equivalent: BigDecimal
a = Decimal('0.1')
b = Decimal('0.2')
print(a + b)        # 0.3  — exact!
print(a + b == Decimal('0.3'))  # True

# Rounding
val = Decimal('2.345')
print(val.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP))  # 2.35
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Float type | `float` (32-bit), `double` (64-bit) | `float` — always 64-bit (double) |
| Exact decimal | `BigDecimal` | `decimal.Decimal` |
| Float comparison | `Math.abs(a - b) < epsilon` | `math.isclose(a, b)` |
| Infinity | `Double.POSITIVE_INFINITY` | `float('inf')` |
| NaN check | `Double.isNaN(x)` | `math.isnan(x)` |

---

## 5. `bool` — Boolean (Subclass of `int`)

### Must Know

**`bool` is a subclass of `int` in Python. `True == 1` and `False == 0`.**  
This is the single biggest Python surprise for Java developers.

### How It Works

```python
print(type(True))        # <class 'bool'>
print(isinstance(True, int))  # True — bool IS an int

# Arithmetic with booleans
print(True + True)       # 2
print(True + 1)          # 2
print(False * 100)       # 0
print(True > False)      # True (1 > 0)

# In collections — dangerous
data = [False, True, True]
print(sum(data))         # 2  — counts True values; common interview pattern

# Truthiness — objects evaluated as bool
print(bool(0))           # False
print(bool(""))          # False
print(bool([]))          # False
print(bool(None))        # False
print(bool({}))          # False
print(bool(0.0))         # False

print(bool(1))           # True
print(bool("x"))         # True
print(bool([0]))         # True — list with one item is truthy even if item is falsy!
```

### Truthiness Rules

```
Falsy values:
  None
  False
  0, 0.0, 0j (numeric zero)
  "", b"", ()  (empty sequences)
  [], {}, set()  (empty collections)
  Objects with __bool__ returning False or __len__ returning 0

Everything else is truthy.
```

### Trap: `if x:` vs `if x is not None:`

```python
def process(data=None):
    # BUG: 0 and [] and "" are all falsy — this rejects valid inputs
    if data:
        return data

    # CORRECT: explicit None check
    if data is not None:
        return data
    return "default"

print(process(0))    # BUG returns "default" — 0 is falsy!
print(process([]))   # BUG returns "default" — [] is falsy!
```

**Strong Interview Answer**: "I always use `if x is not None` when `None` is specifically what I'm checking for. `if x` is correct only when I mean 'if x is truthy', which excludes `0`, `""`, and `[]` — all valid values in many contexts."

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Boolean type | `boolean` (primitive), `Boolean` (wrapper) — separate from integers | `bool` is a subclass of `int`; `True == 1` |
| Truthy evaluation | Must use explicit `boolean` — no implicit conversion from int/String | Any object can be used in `if` — uses `__bool__` or `__len__` |
| Counting trues | Must filter and count | `sum(condition_list)` or `sum(1 for x in items if predicate(x))` |
| `null` check | `if (obj != null)` | `if obj is not None` |

---

## 6. `str` — Immutable String

### Must Know

- **Immutable** — every string operation creates a new string object.
- **Unicode by default** — no need for `new String(bytes, charset)` like Java.
- **Indexed by code point** — `"hello"[0]` → `'h'`.
- **Interning** — CPython interns many string literals (similar to Java string pool but not guaranteed for all strings).

### How It Works

```python
s = "hello"
print(type(s))          # <class 'str'>
print(s[0])             # 'h'
print(s[-1])            # 'o'  — negative indexing from end
print(s[1:3])           # 'el'  — slicing [start:stop) exclusive end
print(s[::-1])          # 'olleh'  — reverse

# Immutability
s[0] = 'H'              # TypeError: 'str' object does not support item assignment

# String methods (all return new strings)
print(s.upper())        # 'HELLO'
print(s.replace('l','r'))  # 'herro'
print("  hi  ".strip()) # 'hi'
print("a,b,c".split(","))  # ['a', 'b', 'c']
print(",".join(["a","b","c"]))  # 'a,b,c'
```

### String Formatting — 4 Ways

```python
name = "Alice"
score = 9.5

# 1. f-string (Python 3.6+) — PREFERRED
print(f"Name: {name}, Score: {score:.2f}")   # Name: Alice, Score: 9.50

# 2. str.format() — common in older code
print("Name: {}, Score: {:.2f}".format(name, score))

# 3. % formatting — legacy, avoid in new code
print("Name: %s, Score: %.2f" % (name, score))

# 4. Template strings — for user-supplied templates (safer against injection)
from string import Template
t = Template("Name: $name")
print(t.substitute(name=name))   # Name: Alice
```

### `str` Concatenation Trap

```python
# TRAP: O(n^2) — creates a new string each iteration
parts = []
result = ""
for i in range(10000):
    result += str(i)          # Creates 10000 intermediate strings

# CORRECT: Use join — O(n)
result = "".join(str(i) for i in range(10000))   # One allocation
```

**Strong Interview Answer**: "String concatenation in a loop is O(n²) because strings are immutable and each `+=` creates a new object. The idiomatic Python solution is to collect parts in a list and `"".join()` at the end, which is O(n)."

### String Identity vs Equality

```python
a = "hello"
b = "hello"
c = "hel" + "lo"
d = "".join(["h","e","l","l","o"])

print(a is b)    # True  — interned at compile time
print(a is c)    # True  — CPython also interns compile-time constant expressions
print(a is d)    # False — runtime construction is NOT interned
print(a == d)    # True  — value equality always works

# NEVER rely on `is` for string comparison in real code
# Always use ==
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| String type | `String` — immutable, backed by `char[]` | `str` — immutable, Unicode sequences |
| Null | `null` | `None` |
| Equality | `equals()` for value, `==` for reference | `==` for value (safe!), `is` for identity |
| String pool | `intern()` for explicit interning | CPython interns automatically for simple literals |
| Concatenation in loop | `StringBuilder.append()` | `"".join(list)` |
| Format | `String.format()` | f-string `f"..."` |
| Multiline | No easy multiline | Triple-quoted: `"""line1\nline2"""` |
| Mutable string | `StringBuilder` | No direct equiv — use list of chars → `"".join()` |
| Encoding | `new String(bytes, StandardCharsets.UTF_8)` | `b"bytes".decode("utf-8")` |
| Regex | `Pattern`, `Matcher` | `re` module — `re.compile()`, `re.search()`, `re.findall()` |

---

## 7. `bytes` and `bytearray`

### Must Know

- `bytes` — **immutable** sequence of integers 0–255. Analogue of Java `byte[]` but immutable.
- `bytearray` — **mutable** sequence of integers 0–255.
- Critical for: network I/O, file I/O, encoding/decoding, hashing, cryptography.

### How It Works

```python
# bytes literal
b = b"hello"
print(type(b))         # <class 'bytes'>
print(b[0])            # 104  — integer, not char!

# Encoding str → bytes
s = "café"
encoded = s.encode("utf-8")
print(encoded)         # b'caf\xc3\xa9'
print(type(encoded))   # <class 'bytes'>

# Decoding bytes → str
decoded = encoded.decode("utf-8")
print(decoded)         # café

# bytearray — mutable
ba = bytearray(b"hello")
ba[0] = 72             # ASCII 'H'
print(ba)              # bytearray(b'Hello')

# bytes is hashable (immutable), bytearray is not
d = {b"key": "value"}     # OK
d = {bytearray(b"key"): "value"}  # TypeError: unhashable type: 'bytearray'
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Immutable byte sequence | `byte[]` (mutable) or `ByteBuffer.wrap()` | `bytes` (immutable) |
| Mutable byte sequence | `byte[]` | `bytearray` |
| String → bytes | `str.getBytes(StandardCharsets.UTF_8)` | `str.encode("utf-8")` |
| bytes → String | `new String(bytes, StandardCharsets.UTF_8)` | `bytes.decode("utf-8")` |
| Hex representation | `Integer.toHexString()`, `Hex.encodeHex()` | `bytes.hex()`, `bytes.fromhex()` |

---

## 8. `list` — Mutable Dynamic Array

### Must Know

- **Mutable ordered sequence** — Java `ArrayList<Object>`.
- **Heterogeneous** — can hold objects of different types (though bad practice usually).
- **Dynamic array internally** — amortized O(1) append; O(n) insert/delete in middle.
- **No type parameter** — `list` not `list[int]` at runtime (type hints are documentation).

### How It Works

```python
lst = [1, 2, 3, 4, 5]
print(type(lst))       # <class 'list'>

# Indexing and slicing
print(lst[0])          # 1
print(lst[-1])         # 5  — last element
print(lst[1:3])        # [2, 3]  — [start:stop) exclusive
print(lst[::2])        # [1, 3, 5]  — step
print(lst[::-1])       # [5, 4, 3, 2, 1]  — reverse

# Mutation
lst.append(6)          # [1, 2, 3, 4, 5, 6]
lst.insert(0, 0)       # [0, 1, 2, 3, 4, 5, 6]
lst.pop()              # removes and returns 6
lst.pop(0)             # removes and returns 0
lst.remove(3)          # removes first occurrence of 3

# No IndexError by default with slices
print(lst[100:200])    # []  — empty slice, no error
print(lst[100])        # IndexError  — direct index raises!
```

### List Comprehensions

```python
# Basic
squares = [x**2 for x in range(10)]   # [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]

# With filter
evens = [x for x in range(20) if x % 2 == 0]

# Nested (matrix flatten)
matrix = [[1, 2], [3, 4], [5, 6]]
flat = [val for row in matrix for val in row]   # [1, 2, 3, 4, 5, 6]

# Conditional expression (ternary)
labels = ["even" if x % 2 == 0 else "odd" for x in range(5)]
# ['even', 'odd', 'even', 'odd', 'even']
```

### List Sorting

```python
numbers = [3, 1, 4, 1, 5, 9, 2, 6]

# sorted() — returns new list, original unchanged
print(sorted(numbers))                  # [1, 1, 2, 3, 4, 5, 6, 9]
print(sorted(numbers, reverse=True))    # [9, 6, 5, 4, 3, 2, 1, 1]

# list.sort() — in-place, returns None
numbers.sort()

# Key function
words = ["banana", "apple", "cherry"]
print(sorted(words, key=len))           # ['apple', 'banana', 'cherry']
print(sorted(words, key=lambda x: x[-1]))  # sort by last char

# Complex sort — multiple keys using tuple
data = [("Alice", 30), ("Bob", 25), ("Charlie", 30)]
data.sort(key=lambda x: (x[1], x[0]))  # sort by age, then name
```

### BIG TRAP #1: Mutable Default Argument

```python
# DANGEROUS — default list is created ONCE and shared across all calls
def add_item(item, container=[]):    # DO NOT DO THIS
    container.append(item)
    return container

print(add_item(1))    # [1]
print(add_item(2))    # [2]  — EXPECTED
                      # [1, 2]  — ACTUAL (same list!)

# CORRECT — use None as default, create inside
def add_item(item, container=None):
    if container is None:
        container = []
    container.append(item)
    return container

print(add_item(1))    # [1]
print(add_item(2))    # [2]  ← now correct
```

**Strong Interview Answer**: "Default arguments in Python are evaluated **once at function definition time**, not each time the function is called. For mutable types like `list` and `dict`, this means all calls share the same object. The idiomatic fix is to default to `None` and create the mutable object inside the function body."

### BIG TRAP #2: `list * n` Creates Shared References

```python
# TRAP — [[]] * 3 creates one list, shared 3 times
grid = [[0] * 3] * 3       # Looks like 3x3 grid
grid[0][0] = 1
print(grid)
# [[1, 0, 0], [1, 0, 0], [1, 0, 0]]  — ALL rows changed!

# CORRECT — list comprehension creates independent inner lists
grid = [[0] * 3 for _ in range(3)]
grid[0][0] = 1
print(grid)
# [[1, 0, 0], [0, 0, 0], [0, 0, 0]]  ← correct
```

### BIG TRAP #3: Shallow Copy

```python
a = [[1, 2], [3, 4]]

# Shallow copy — inner lists still shared
b = a.copy()    # or a[:]  or list(a)
b[0][0] = 99
print(a[0][0])  # 99  — a was also changed!

# Deep copy — independent at all levels
import copy
c = copy.deepcopy(a)
c[0][0] = 0
print(a[0][0])  # 99  — a unchanged
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Type | `ArrayList<T>` | `list` |
| Add to end | `list.add(item)` | `list.append(item)` |
| Add at index | `list.add(i, item)` | `list.insert(i, item)` |
| Remove by value | `list.remove(obj)` | `list.remove(val)` |
| Remove by index | `list.remove(i)` | `list.pop(i)` |
| Size | `list.size()` | `len(list)` |
| Sort | `Collections.sort(list)` | `list.sort()` or `sorted(list)` |
| Reverse | `Collections.reverse(list)` | `list.reverse()` or `list[::-1]` |
| Shallow copy | `new ArrayList<>(list)` | `list.copy()` or `list[:]` |
| Deep copy | `SerializationUtils.clone()` | `copy.deepcopy(list)` |
| Sublist | `list.subList(a, b)` | `list[a:b]` |
| Contains | `list.contains(item)` | `item in list` |
| Stream filter | `stream().filter(p).collect(toList())` | `[x for x in lst if p(x)]` |
| Stream map | `stream().map(f).collect(toList())` | `[f(x) for x in lst]` |

---

## 9. `tuple` — Immutable Sequence

### Must Know

- **Immutable ordered sequence** — like `list` but cannot be changed after creation.
- **Hashable only if all elements are hashable** — `(1, [2, 3])` is NOT hashable.
- Use tuples for: heterogeneous records (like a struct row), function return values, dictionary keys.
- Use lists for: homogeneous sequences that may change.

### How It Works

```python
# Creation
t = (1, 2, 3)
t_single = (42,)      # ONE-element tuple needs trailing comma!
t_empty = ()

# TRAP: (42) is not a tuple — it's just 42 in parentheses
not_a_tuple = (42)
print(type(not_a_tuple))   # <class 'int'>  ← NOT tuple!

# Packing / unpacking
t = 1, 2, 3            # Parentheses optional for packing
a, b, c = t            # Unpacking
a, *rest = t           # Star unpacking: a=1, rest=[2, 3]
first, *middle, last = (1, 2, 3, 4, 5)  # first=1, middle=[2,3,4], last=5

# Tuple as dict key (valid because tuple is immutable)
d = {(0, 0): "origin", (1, 0): "right"}
print(d[(0, 0)])       # 'origin'
```

### The Tuple Mutability Trap

```python
# A tuple containing a mutable object IS NOT fully immutable
t = (1, [2, 3], 4)
t[1].append(99)        # This works! The list inside is still mutable
print(t)               # (1, [2, 3, 99], 4)

# Consequence: tuple with mutable element is NOT hashable
d = {}
d[t] = "value"         # TypeError: unhashable type: 'list'

# Only tuples containing ALL hashable elements are hashable
t_hashable = (1, 2, 3)
d[t_hashable] = "value"   # OK
```

**Strong Interview Answer**: "A tuple's immutability means you can't rebind its elements — `t[0] = 5` raises `TypeError`. But if an element is itself mutable, like a list, its internal state can still change. This means `tuple` is not unconditionally hashable; CPython checks at hash time and raises `TypeError: unhashable type` if any element is unhashable."

### Named Tuples

```python
from collections import namedtuple
from typing import NamedTuple

# Old style — namedtuple factory
Point = namedtuple('Point', ['x', 'y'])
p = Point(1, 2)
print(p.x, p.y)     # 1 2
print(p[0])         # 1  — still works as regular tuple

# New style — class syntax (preferred)
class Point(NamedTuple):
    x: float
    y: float
    label: str = "unknown"   # default value

p = Point(1.0, 2.0)
print(p.x)         # 1.0
print(p._asdict()) # {'x': 1.0, 'y': 2.0, 'label': 'unknown'}
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Immutable sequence | No direct equivalent — `List.of()` is closest | `tuple` |
| Returning multiple values | `Pair<A,B>`, custom record, array | Return tuple: `return x, y` then `a, b = func()` |
| Record/value object | Java 16+ `record` | `NamedTuple` or `@dataclass(frozen=True)` |
| Heterogeneous row | `Object[]` or custom class | `tuple` or `NamedTuple` |

---

## 10. `dict` — Ordered Mutable Mapping

### Must Know

- **Mutable mapping** — Java `HashMap`, but **ordered since Python 3.7** (insertion order preserved).
- **Keys must be hashable** — immutable types only.
- **O(1) average** get, set, delete — backed by hash table.
- Default value access via `.get()` avoids `KeyError`.

### How It Works

```python
d = {"name": "Alice", "age": 30, "active": True}

# Access
print(d["name"])           # 'Alice'  — KeyError if missing
print(d.get("name"))       # 'Alice'
print(d.get("salary"))     # None  — no KeyError
print(d.get("salary", 0))  # 0   — default value

# Mutation
d["age"] = 31              # update
d["email"] = "a@b.com"     # insert
del d["active"]            # delete
popped = d.pop("email", None)  # remove and return, default if missing

# Iteration — three views
for key in d:              # iterates keys
    pass
for key in d.keys():       # same, explicit
    pass
for val in d.values():     # iterate values
    pass
for k, v in d.items():     # iterate key-value pairs  ← most common
    print(k, v)

# Membership test is on keys
print("name" in d)         # True
print("Alice" in d)        # False  — 'in' checks keys, not values!
```

### Dict Comprehensions and Merging

```python
# Dict comprehension
squares = {x: x**2 for x in range(5)}   # {0:0, 1:1, 2:4, 3:9, 4:16}
inverted = {v: k for k, v in squares.items()}

# Merge (Python 3.9+) — | operator
d1 = {"a": 1, "b": 2}
d2 = {"b": 3, "c": 4}
merged = d1 | d2          # {"a": 1, "b": 3, "c": 4}  — d2 wins on conflict
d1 |= d2                  # in-place merge

# Python 3.5+ way
merged = {**d1, **d2}     # same effect, still works everywhere 3.5+
```

### `collections.defaultdict` and `Counter`

```python
from collections import defaultdict, Counter

# defaultdict — no KeyError on missing key
word_count = defaultdict(int)
for word in ["cat", "dog", "cat", "bird", "dog", "cat"]:
    word_count[word] += 1   # No need to check if key exists
print(dict(word_count))  # {'cat': 3, 'dog': 2, 'bird': 1}

# Counter — specialized for counting
word_count = Counter(["cat", "dog", "cat", "bird", "dog", "cat"])
print(word_count.most_common(2))  # [('cat', 3), ('dog', 2)]
print(word_count["cat"])          # 3
print(word_count["elephant"])     # 0  — no KeyError!

# Counter arithmetic
c1 = Counter(["a", "b", "a"])
c2 = Counter(["b", "b", "c"])
print(c1 + c2)   # Counter({'a': 2, 'b': 3, 'c': 1})
print(c1 - c2)   # Counter({'a': 2})  — removes non-positive
```

### `collections.OrderedDict` (Legacy Note)

```python
# Before Python 3.7, dict did NOT guarantee insertion order
# OrderedDict was the solution
from collections import OrderedDict
od = OrderedDict()
od["first"] = 1
od["second"] = 2

# In Python 3.7+ regular dict preserves order, so OrderedDict is rarely needed
# But OrderedDict has move_to_end() and equality checks order (regular dict doesn't)
od1 = OrderedDict([("a", 1), ("b", 2)])
od2 = OrderedDict([("b", 2), ("a", 1)])
print(od1 == od2)           # False  — order matters for OrderedDict equality
d1 = {"a": 1, "b": 2}
d2 = {"b": 2, "a": 1}
print(d1 == d2)             # True   — order does NOT matter for regular dict equality
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Type | `HashMap<K,V>` (unordered), `LinkedHashMap` (ordered) | `dict` (ordered since 3.7) |
| Safe get | `map.getOrDefault(key, def)` | `d.get(key, default)` |
| KeyError | `NullPointerException` or no result | `KeyError` |
| Iterate entries | `for (Map.Entry<K,V> e : map.entrySet())` | `for k, v in d.items()` |
| Merge maps | `putAll()` | `d1 \| d2` or `{**d1, **d2}` |
| Count occurrences | Manual `getOrDefault(k,0)+1` | `Counter(iterable)` |
| Default value on missing | `computeIfAbsent()` | `defaultdict(factory)` |
| Stream to map | `.collect(Collectors.toMap(...))` | `{k: v for ...}` dict comprehension |

---

## 11. `set` and `frozenset`

### Must Know

- **`set`**: mutable, unordered collection of **unique** hashable objects. O(1) membership test.
- **`frozenset`**: immutable set — hashable, can be used as dict key or set element.
- No duplicates. Order is not preserved. No indexing.

### How It Works

```python
s = {1, 2, 3, 3, 2, 1}
print(s)             # {1, 2, 3}  — duplicates removed

# TRAP: {} is an empty dict, NOT an empty set
empty_set = set()    # Correct
empty_dict = {}      # Dict!

# Operations
s.add(4)             # {1, 2, 3, 4}
s.discard(10)        # No error if missing  ← DIFFERENT from remove()
s.remove(1)          # KeyError if missing
popped = s.pop()     # Removes and returns an arbitrary element

# Set operations — same as mathematical sets
a = {1, 2, 3, 4}
b = {3, 4, 5, 6}
print(a | b)         # {1, 2, 3, 4, 5, 6}  — union
print(a & b)         # {3, 4}              — intersection
print(a - b)         # {1, 2}              — difference (in a but not b)
print(a ^ b)         # {1, 2, 5, 6}        — symmetric difference

# Methods (same as operators)
print(a.union(b))
print(a.intersection(b))
print(a.difference(b))
print(a.symmetric_difference(b))

# Subset / superset
print({1, 2}.issubset({1, 2, 3}))    # True
print({1, 2, 3}.issuperset({1, 2}))  # True
```

### Set Comprehensions and Deduplication

```python
# Set comprehension
unique_lengths = {len(word) for word in ["cat", "dog", "elephant", "ant"]}
# {3, 8}  — set means unique lengths only

# Fastest deduplication while preserving order (Python 3.7+ dict is ordered)
items = [3, 1, 4, 1, 5, 9, 2, 6, 5, 3]
unique_ordered = list(dict.fromkeys(items))   # [3, 1, 4, 5, 9, 2, 6]

# If order doesn't matter
unique = list(set(items))
```

### frozenset — Hashable Set

```python
fs = frozenset({1, 2, 3})
print(type(fs))    # <class 'frozenset'>

# Can be used as dict key
graph = {
    frozenset({0, 1}): "edge A",
    frozenset({1, 2}): "edge B",
}
print(graph[frozenset({0, 1})])   # 'edge A'

# Can be element in another set
set_of_sets = {frozenset({1, 2}), frozenset({3, 4})}
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Mutable set | `HashSet<T>` (unordered), `LinkedHashSet` (insertion-ordered) | `set` (unordered) |
| Immutable set | `Set.of(...)` or `Collections.unmodifiableSet()` | `frozenset` (truly immutable + hashable) |
| Add | `set.add(item)` | `s.add(item)` |
| Remove safe | `set.remove(item)` (no exception in Java) | `s.discard(item)` (no error), `s.remove(item)` (KeyError) |
| Contains | `set.contains(item)` | `item in s` |
| Empty set literal | `new HashSet<>()` | `set()` — NOT `{}` |
| Set operations | Manual intersection/union | `&`, `\|`, `-`, `^` operators |

---

## 12. `None` — The Null Singleton

### Must Know

- `None` is **the** null value in Python. There is **exactly one** `None` object (singleton).
- Always test with `is None` or `is not None`, **never** `== None`.
- Type is `NoneType`. `bool(None)` is `False`.

### How It Works

```python
x = None
print(type(x))         # <class 'NoneType'>
print(x is None)       # True  — CORRECT way to check
print(x == None)       # True  — also works but triggers linter warning (PEP 8)
print(bool(None))      # False

# Functions without explicit return return None
def do_nothing():
    pass

result = do_nothing()
print(result)          # None
print(result is None)  # True

# None in collections
items = [1, None, 3, None, 5]
non_null = [x for x in items if x is not None]  # [1, 3, 5]
```

### Why `is None` and not `== None`?

```python
class Evil:
    def __eq__(self, other):
        return True   # Claims to equal EVERYTHING including None

obj = Evil()
print(obj == None)     # True  — overridden __eq__ is called
print(obj is None)     # False — identity check bypasses __eq__
```

**Strong Interview Answer**: "`None` is a singleton — there is only one `None` object in a Python process. Using `is None` tests object identity, which is always correct and cannot be overridden by `__eq__`. Using `== None` calls `__eq__`, which can theoretically be overridden in custom classes. PEP 8 and all linters require `is None`."

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Null | `null` — not an object, type-less | `None` — an object of type `NoneType` |
| Null check | `obj == null` or `Objects.isNull(obj)` | `obj is None` |
| Optional pattern | `Optional<T>` | Return `None` directly — no `Optional` class needed |
| NullPointerException | `NullPointerException` | `AttributeError` or `TypeError` when calling on `None` |

---

## 13. Mutability Rules Summary Table

| Type | Mutable? | Hashable? | Notes |
|---|---|---|---|
| `int` | No | Yes | |
| `float` | No | Yes | |
| `bool` | No | Yes | Subclass of `int` |
| `str` | No | Yes | |
| `bytes` | No | Yes | |
| `tuple` | No (container) | **Conditionally** | Only if all elements are hashable |
| `frozenset` | No | Yes | |
| `NoneType` | No | Yes | |
| `list` | Yes | **No** | Cannot be dict key or set element |
| `dict` | Yes | **No** | |
| `set` | Yes | **No** | |
| `bytearray` | Yes | **No** | |

**The Rule**: Only immutable objects can be hashable. Hashable means the object can be used as a dict key or set member.

---

## 14. Hashability Deep Dive

### Must Know

An object is hashable if it has:
1. A `__hash__()` method that returns an integer.
2. `__hash__()` is consistent for the object's lifetime.
3. If two objects are equal (`a == b`), then `hash(a) == hash(b)`.

### Custom Class Hashability

```python
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __eq__(self, other):
        return self.x == other.x and self.y == other.y

    # If you define __eq__ without __hash__, Python sets __hash__ = None
    # This makes the class UNHASHABLE

p = Point(1, 2)
# {p: "origin"}   # TypeError: unhashable type: 'Point'

# FIXED: also define __hash__
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __eq__(self, other):
        return isinstance(other, Point) and self.x == other.x and self.y == other.y

    def __hash__(self):
        return hash((self.x, self.y))   # Hash of a tuple — standard pattern

p = Point(1, 2)
d = {p: "origin"}   # Now works
s = {p}             # Now works
```

### The `__eq__` + `__hash__` Contract

```python
# The contract: if a == b, then hash(a) == hash(b)
# The converse is NOT required: hash(a) == hash(b) does NOT mean a == b (collision is allowed)

# Violation of contract — never do this:
class Bad:
    def __init__(self, x):
        self.x = x
    def __eq__(self, other):
        return self.x == other.x
    def __hash__(self):
        return 42   # constant hash — technically valid but O(n) dict performance

# Best practice for immutable objects:
class ImmutablePoint:
    __slots__ = ('x', 'y')   # Memory optimization + prevents attribute addition

    def __init__(self, x, y):
        object.__setattr__(self, 'x', x)
        object.__setattr__(self, 'y', y)

    def __setattr__(self, name, value):
        raise AttributeError("ImmutablePoint is immutable")

    def __eq__(self, other):
        return isinstance(other, ImmutablePoint) and (self.x, self.y) == (other.x, other.y)

    def __hash__(self):
        return hash((self.x, self.y))
```

---

## 15. Identity vs Equality Per Type

### The `is` vs `==` Rule

```
is   → Object identity. Same memory address. Uses id(a) == id(b). Cannot be overridden.
==   → Value equality. Calls __eq__. Can be overridden.
```

| Type | Safe to use `is`? | Use `==` for? |
|---|---|---|
| `None` | Yes — always use `is None` | Never use `== None` |
| `True` / `False` | Technically fine but unnecessary | `if x:` not `if x == True:` |
| `int` (-5 to 256) | Only with CPython and only for educational purposes | Always use `==` in real code |
| `str` | **No** — interning not guaranteed for all strings | Always use `==` |
| `list`, `dict`, `set` | Only when checking "is it the same object" (aliasing check) | Use `==` for value comparison |
| Custom objects | For identity check | For value comparison |

```python
# Correct patterns
if x is None:         # checking for None
if x is not None:     # not None
if x is True:         # valid but use `if x:` instead
if a is b:            # checking for aliasing (two names point to same object)
if a == b:            # checking for equal values
```

---

## 16. The Big 5 Mutability Traps

### Trap 1: Mutable Default Argument (Already covered — the #1 trap)

```python
# WRONG
def append_to(element, to=[]):
    to.append(element)
    return to

# RIGHT
def append_to(element, to=None):
    if to is None:
        to = []
    to.append(element)
    return to
```

### Trap 2: Aliasing — Not Making a Copy

```python
a = [1, 2, 3]
b = a             # b is an ALIAS, not a copy
b.append(4)
print(a)          # [1, 2, 3, 4]  ← a was also modified!

# Fix: shallow copy
b = a.copy()      # or a[:]
b.append(4)
print(a)          # [1, 2, 3]  ← a unchanged

# Fix: deep copy for nested structures
import copy
b = copy.deepcopy(a)
```

### Trap 3: `list * n` with Mutable Inner Objects

```python
# WRONG: 3 rows all point to the same list object
matrix = [[0] * 3] * 3
matrix[0][1] = 99
print(matrix)   # [[0, 99, 0], [0, 99, 0], [0, 99, 0]]

# RIGHT: list comprehension creates independent row lists
matrix = [[0] * 3 for _ in range(3)]
matrix[0][1] = 99
print(matrix)   # [[0, 99, 0], [0, 0, 0], [0, 0, 0]]
```

### Trap 4: Tuple Containing Mutable Hashing Surprise

```python
t = ([1, 2], [3, 4])   # Tuple of lists — mutable inside!
s = {t}                # TypeError: unhashable type: 'list'

# Fix: use frozenset or ensure all elements are hashable
t = (frozenset([1, 2]), frozenset([3, 4]))
s = {t}   # OK
```

### Trap 5: Mutating a Collection While Iterating It

```python
# WRONG — RuntimeError: dictionary changed size during iteration
d = {"a": 1, "b": 2, "c": 3}
for key in d:
    if d[key] == 2:
        del d[key]    # RuntimeError!

# WRONG for lists — silently skips elements
lst = [1, 2, 3, 4, 5]
for i, v in enumerate(lst):
    if v % 2 == 0:
        lst.pop(i)    # Skips elements!

# CORRECT — iterate over a copy
for key in list(d.keys()):     # snapshot of keys
    if d[key] == 2:
        del d[key]

# Or build new collection
d = {k: v for k, v in d.items() if v != 2}   # comprehension
lst = [v for v in lst if v % 2 != 0]         # comprehension
```

---

## 17. Java Developer Bridge — Complete Type Mapping

| Java Type | Python Equivalent | Key Difference |
|---|---|---|
| `int` (32-bit) | `int` | Python int is arbitrary precision, never overflows |
| `long` (64-bit) | `int` | Same — Python has only one integer type |
| `double` | `float` | Same IEEE 754, same precision traps |
| `BigDecimal` | `decimal.Decimal` | Use for exact financial arithmetic |
| `boolean` | `bool` | Python `bool` is subclass of `int`; `True == 1` |
| `char` | `str` (length-1) | No separate char type in Python |
| `String` | `str` | `==` is safe in Python; Java needs `.equals()` |
| `StringBuilder` | `list` + `"".join()` | No mutable string class |
| `byte[]` | `bytes` (immutable) or `bytearray` (mutable) | `bytes` is immutable unlike Java `byte[]` |
| `null` | `None` | `None` is a singleton object, not a missing reference |
| `ArrayList<T>` | `list` | Python list is heterogeneous, no generics at runtime |
| `LinkedList<T>` | `collections.deque` | O(1) append/prepend from both ends |
| `ArrayDeque<T>` | `collections.deque` | Same — Python `deque` supports popleft() in O(1) |
| `HashMap<K,V>` | `dict` | Python dict preserves insertion order since 3.7 |
| `LinkedHashMap<K,V>` | `dict` | Regular dict is already ordered in Python 3.7+ |
| `TreeMap<K,V>` | `dict` + `sorted()` or `sortedcontainers.SortedDict` | No built-in sorted map; use third-party library |
| `HashSet<T>` | `set` | |
| `LinkedHashSet<T>` | `dict.fromkeys()` pattern | Use `dict.fromkeys(items)` to deduplicate with order |
| `TreeSet<T>` | `sortedcontainers.SortedList` | Third-party; no built-in sorted set |
| `int[]` | `list` or `array.array('i', ...)` | `array.array` for typed arrays; `list` for general use |
| `Optional<T>` | Return `None` directly | Python idiom doesn't use Optional wrapper |
| `Pair<A,B>` | `tuple` | `return a, b` unpacked as `x, y = func()` |
| `Record` (Java 16+) | `NamedTuple` or `@dataclass(frozen=True)` | |
| `enum` | `enum.Enum` | Python enums support methods and values |
| `Iterator<T>` | iterator protocol (`__iter__`, `__next__`) | Duck typing — no explicit interface |
| `Iterable<T>` | Any object with `__iter__` | |
| `Comparable<T>` | `__lt__`, `__le__`, `__gt__`, `__ge__`, `__eq__` | Or use `functools.total_ordering` decorator |

---

## 18. Hot Interview Q&A

**Q: What happens when you run `x = 5; y = x; x = 10; print(y)`?**
A: Prints `5`. `y` still points to the `int` object `5`. Reassigning `x = 10` rebinds `x` to a new object; it does not affect `y`. `int` is immutable — there is no object mutation happening.

**Q: What is the output of `print([] == [])` and `print([] is [])`?**
A: `True` then `False`. `==` checks value equality (both are empty lists, equal values). `is` checks identity — they are two different list objects in memory.

**Q: Why can't you use a list as a dictionary key?**
A: Dictionary keys must be hashable. `list` is mutable — if you could hash a list and then mutate it, the hash would change and you'd never be able to find the key again. Python enforces this contract by raising `TypeError: unhashable type: 'list'`.

**Q: What is the output of this code?**
```python
def f(x=[]):
    x.append(1)
    return x
print(f())
print(f())
print(f())
```
A: `[1]`, `[1, 1]`, `[1, 1, 1]`. The default list is created once at function definition. Each call mutates the same list object.

**Q: What is the difference between `list.sort()` and `sorted(list)`?**
A: `list.sort()` sorts in-place and returns `None`. `sorted(list)` returns a new sorted list and leaves the original unchanged. `sorted()` works on any iterable; `.sort()` is only on lists.

**Q: Can a tuple be used as a dict key? Always?**
A: Only if all its elements are hashable. `(1, 2, "hello")` is hashable. `(1, [2, 3])` is not hashable because `list` is mutable.

**Q: What is the difference between `remove()` and `discard()` on a set?**
A: `remove(x)` raises `KeyError` if `x` is not present. `discard(x)` silently does nothing if `x` is missing. For safe removal, prefer `discard()`.

**Q: Is Python `dict` ordered?**
A: Yes, as of Python 3.7, regular `dict` preserves insertion order as part of the language specification (CPython 3.6 did it as an implementation detail). Before 3.7, you needed `collections.OrderedDict` for ordering guarantees. However, `dict` equality does NOT consider order — two dicts with same key-value pairs but different insertion order are equal.

**Q: What is the output of `print(True == 1)` and `print(True is 1)`?**
A: `True` then (in Python 3.8+) `False` with a `SyntaxWarning`. `bool` is a subclass of `int`, so `True == 1` is `True`. But `True` and `1` are different objects — `True` is the boolean singleton, `1` is an `int` object.

**Q: What is `bool([0])`?**
A: `True`. A list with one element is truthy regardless of what that element is. Only an **empty** list is falsy. `bool([])` is `False`; `bool([0])` is `True`.

---

## 19. Final Revision Checklist

### Data Types Mastery

- [ ] I can state which Python types are mutable and which are immutable from memory
- [ ] I know the hashability rule: only immutable objects are hashable (with the tuple caveat)
- [ ] I can explain why `True == 1` and `True + True == 2` with the class hierarchy
- [ ] I can demonstrate the mutable default argument trap and its fix
- [ ] I can explain why `[[]] * 3` creates shared inner lists and fix it with a comprehension
- [ ] I know the three ways to copy a list and when to use `deepcopy`
- [ ] I can write a custom class with correct `__eq__` and `__hash__` implementation

### Java Developer Specific

- [ ] I remember that `dict` is ordered in Python 3.7+ but Java's `HashMap` is NOT
- [ ] I know `bytes` is immutable in Python (unlike Java `byte[]`)
- [ ] I remember Python `int` never overflows (no equivalent to `Integer.MAX_VALUE` overflow)
- [ ] I know `None` check is `is None`, not `== null` translated to `== None`
- [ ] I can replace Java's `Optional<T>` with Python's `None` return pattern
- [ ] I understand that `set()` creates an empty set but `{}` creates an empty `dict`

### Interview Traps

- [ ] Mutable default argument — know why it happens and always fix it
- [ ] `list * n` with nested mutables — 2D grid setup trap
- [ ] Aliasing vs copying — `b = a` vs `b = a.copy()`
- [ ] Mutating a collection during iteration — know the safe patterns
- [ ] Tuple containing list is not hashable — `(1, [2, 3])` raises TypeError as dict key
- [ ] `is` vs `==` — only use `is` for `None`, `True`, `False`; use `==` for everything else

---

*File 3 of 7 — Group 1: Starter Path*  
*Next: Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md*
