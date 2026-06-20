# Python Collections, Comprehensions & Iteration — Gold Sheet

> **Track**: Python Interview Track — Group 1: Starter Path  
> **File**: 6 of 7  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| List/dict/set comprehensions | ★★★★★ | Every Python interview has at least one; Java has no direct equivalent |
| Generator expressions vs list comprehensions | ★★★★★ | Memory and laziness distinction — asked in every backend/data interview |
| `yield` and generator functions | ★★★★★ | Completely absent in Java; fundamental to Python's iteration model |
| Iterator protocol (`__iter__`, `__next__`) | ★★★★☆ | Java `Iterator<T>` exists but Python's protocol-based approach is different |
| `itertools` — `chain`, `islice`, `groupby`, `product` | ★★★★☆ | Essential toolkit; Java Streams cover some but `itertools` goes further |
| `collections.deque` — O(1) both ends | ★★★★☆ | `ArrayDeque` equivalent — asked in algorithm interviews |
| `collections.defaultdict`, `Counter` | ★★★★☆ | Java needs manual `getOrDefault` — Python has native solutions |
| `yield from` — delegation | ★★★☆☆ | No Java equivalent; used in recursive generators |
| Generator `.send()` | ★★★☆☆ | Coroutine precursor — senior Python interviews only |
| `collections.ChainMap` | ★★☆☆☆ | Useful for config layering; rarely in Java interviews |

---

## 2. The Iteration Hierarchy

### Must Know

Three related but distinct concepts:

```
Iterable    — Has __iter__() method. Returns an iterator.
              Examples: list, tuple, str, dict, set, generator objects, files

Iterator    — Has both __iter__() and __next__() methods.
              __next__() returns the next value or raises StopIteration.
              Iterators ARE iterables (calling __iter__() returns self).

Generator   — A special kind of iterator created by a generator function (uses yield)
              or a generator expression.
```

### How It Works

```python
# A plain list is ITERABLE but not an ITERATOR
lst = [1, 2, 3]
print(hasattr(lst, '__iter__'))    # True — iterable
print(hasattr(lst, '__next__'))    # False — not an iterator

# Get an iterator from the iterable
it = iter(lst)                     # calls lst.__iter__()
print(hasattr(it, '__next__'))     # True — now it's an iterator
print(next(it))   # 1              # calls it.__next__()
print(next(it))   # 2
print(next(it))   # 3
# next(it)        # StopIteration — exhausted

# for loop does this automatically:
# 1. Calls iter(obj) to get an iterator
# 2. Calls next(iterator) repeatedly until StopIteration
# 3. Catches StopIteration silently
for x in lst:   # Same as the iter/next loop above
    print(x)
```

### Custom Iterator

```python
class CountUp:
    """An iterator that counts from start to stop."""

    def __init__(self, start: int, stop: int):
        self.current = start
        self.stop = stop

    def __iter__(self):
        """Return self — this object IS the iterator."""
        return self

    def __next__(self):
        if self.current >= self.stop:
            raise StopIteration
        value = self.current
        self.current += 1
        return value


counter = CountUp(1, 5)
print(list(counter))        # [1, 2, 3, 4]
for n in CountUp(1, 4):     # Works in for loop
    print(n)                # 1, 2, 3

# Iterators are exhausted after one pass
it = CountUp(1, 3)
print(list(it))   # [1, 2]
print(list(it))   # []  — exhausted! Reuse requires creating a new instance
```

### Iterable vs Iterator — The Key Distinction

```python
# Iterables can be iterated multiple times — they produce fresh iterators
lst = [1, 2, 3]
print(list(lst))   # [1, 2, 3]
print(list(lst))   # [1, 2, 3]  — fresh iteration

# Iterators can only be iterated once — they are stateful
it = iter(lst)
print(list(it))    # [1, 2, 3]
print(list(it))    # []  — exhausted!

# TRAP: passing an iterator where an iterable is expected
it = iter([1, 2, 3, 4, 5])
evens = [x for x in it if x % 2 == 0]   # [2, 4]
odds = [x for x in it if x % 2 != 0]    # []  — iterator already exhausted!
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Iterable | `Iterable<T>` — `iterator()` method | Any object with `__iter__()` |
| Iterator | `Iterator<T>` — `hasNext()`, `next()` | `__next__()` — raises `StopIteration` instead of `hasNext()` |
| Enhanced for | `for (T x : collection)` | `for x in collection:` |
| Manual iteration | `Iterator<T> it = list.iterator(); while(it.hasNext()) it.next()` | `it = iter(lst); next(it)` |
| One-time use | Java iterators also one-time use | Same |
| `Spliterator` | Java parallel iteration | No equivalent (generators are inherently sequential) |

---

## 3. List Comprehensions — Deep Dive

### Must Know

List comprehensions replace `map()` + `filter()` chains in a single readable expression.  
They create a new list. They are eager — all elements computed immediately.

### Syntax

```
[expression for variable in iterable]
[expression for variable in iterable if condition]
[expression for var1 in iter1 for var2 in iter2]  # nested (inner loop first)
```

### How It Works

```python
# Basic — square all numbers
squares = [x**2 for x in range(10)]
# [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]

# With filter — only even squares
even_squares = [x**2 for x in range(10) if x % 2 == 0]
# [0, 4, 16, 36, 64]

# With transformation and filter
words = ["hello", "world", "python", "is", "great"]
long_upper = [w.upper() for w in words if len(w) > 4]
# ['HELLO', 'WORLD', 'PYTHON', 'GREAT']

# Ternary expression in comprehension — conditional value, not filter
labels = ["even" if x % 2 == 0 else "odd" for x in range(6)]
# ['even', 'odd', 'even', 'odd', 'even', 'odd']

# Nested comprehension — matrix transpose
matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
transposed = [[row[i] for row in matrix] for i in range(3)]
# [[1, 4, 7], [2, 5, 8], [3, 6, 9]]

# Flatten nested list — inner loop iterates the nested iterable
nested = [[1, 2], [3, 4], [5, 6]]
flat = [x for sublist in nested for x in sublist]
# [1, 2, 3, 4, 5, 6]
# Read as: for sublist in nested → for x in sublist → collect x
```

### Performance — Comprehension vs Loop vs `map`

```python
import timeit

# list comprehension — fastest in most cases
t1 = timeit.timeit(lambda: [x**2 for x in range(1000)], number=10000)

# for loop with append
t2 = timeit.timeit(lambda: [result := [], [result.append(x**2) for x in range(1000)], result][2], number=10000)

# map — similar speed to comprehension
t3 = timeit.timeit(lambda: list(map(lambda x: x**2, range(1000))), number=10000)

# Comprehension is generally 10-30% faster than equivalent for+append loop
# Choose comprehension for readability; use loop when logic is complex
```

---

## 4. Dictionary Comprehensions

### Must Know

Dict comprehensions create a new dict from an iterable. Both key and value are expressions.

```python
# Basic
squares = {x: x**2 for x in range(6)}
# {0: 0, 1: 1, 2: 4, 3: 9, 4: 16, 5: 25}

# From two lists — zip them
keys = ["a", "b", "c"]
values = [1, 2, 3]
d = {k: v for k, v in zip(keys, values)}
# {'a': 1, 'b': 2, 'c': 3}

# Invert a dict — swap keys and values
original = {"one": 1, "two": 2, "three": 3}
inverted = {v: k for k, v in original.items()}
# {1: 'one', 2: 'two', 3: 'three'}

# With filter — only include if condition met
scores = {"Alice": 95, "Bob": 72, "Carol": 88, "Dave": 61}
passing = {name: score for name, score in scores.items() if score >= 75}
# {'Alice': 95, 'Carol': 88}

# Transform values
normalized = {k: v / 100 for k, v in scores.items()}
# {'Alice': 0.95, 'Bob': 0.72, 'Carol': 0.88, 'Dave': 0.61}

# Grouping with defaultdict + comprehension
from collections import defaultdict
data = [("Alice", 90), ("Bob", 80), ("Alice", 95), ("Bob", 85)]
grouped = defaultdict(list)
for name, score in data:
    grouped[name].append(score)
averages = {name: sum(vals)/len(vals) for name, vals in grouped.items()}
# {'Alice': 92.5, 'Bob': 82.5}
```

---

## 5. Set Comprehensions

```python
# Basic
unique_lengths = {len(word) for word in ["cat", "dog", "elephant", "ant", "bee"]}
# {3, 8}  — unique lengths only

# With filter
long_words_upper = {w.upper() for w in ["cat", "dog", "elephant", "ant"] if len(w) > 3}
# {'ELEPHANT'}

# Deduplication with transformation
data = [1, 2, 2, 3, 3, 3, 4, 4, 4, 4]
unique_squares = {x**2 for x in data}
# {1, 4, 9, 16}  — both unique AND squared

# Set comprehension vs list comprehension
print(type({x for x in range(5)}))   # <class 'set'>
print(type([x for x in range(5)]))   # <class 'list'>
print(type({x: x for x in range(5)}))  # <class 'dict'>
```

---

## 6. Generator Expressions — Lazy Evaluation

### Must Know

Generator expressions look like list comprehensions but use `()` and are **lazy** — elements are computed on demand, one at a time. They do not store all values in memory.

**Key difference**: `[x**2 for x in range(10**9)]` — tries to create a billion-element list (crashes).  
`(x**2 for x in range(10**9))` — creates a generator object instantly, computes on demand.

### How It Works

```python
# List comprehension — eager, all values computed now, stored in memory
lst_comp = [x**2 for x in range(10)]
print(type(lst_comp))    # <class 'list'>
print(lst_comp)          # [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]

# Generator expression — lazy, computes on demand
gen_exp = (x**2 for x in range(10))
print(type(gen_exp))     # <class 'generator'>
print(gen_exp)           # <generator object <genexpr> at 0x...>

# Consume lazily
print(next(gen_exp))     # 0
print(next(gen_exp))     # 1
print(list(gen_exp))     # [4, 9, 16, 25, 36, 49, 64, 81]  — rest
print(list(gen_exp))     # []  — exhausted!

# Memory comparison — millions of numbers
import sys
lst = [x**2 for x in range(1_000_000)]
gen = (x**2 for x in range(1_000_000))
print(sys.getsizeof(lst))   # ~8 MB
print(sys.getsizeof(gen))   # ~120 bytes  (just the generator object!)
```

### When to Use Which

```python
# USE list comprehension when:
# 1. You need to iterate the result multiple times
# 2. You need len(), indexing, slicing
# 3. Result is small enough to fit in memory

# USE generator expression when:
# 1. Single pass through large data
# 2. Passing to sum(), max(), min(), any(), all() — they consume lazily
# 3. Pipeline of transformations on large dataset
# 4. Infinite sequences

# PASS generator to aggregation functions — no need for extra list
total = sum(x**2 for x in range(1000))   # No intermediate list
maximum = max(len(line) for line in open("file.txt"))  # Process file lazily

# RULE: When passing to a single-use function, use generator expression
# When storing or reusing, use list comprehension
```

---

## 7. Generator Functions — `yield`

### Must Know

A **generator function** uses `yield` instead of `return`. Calling it returns a generator object without executing the body. Each `next()` call runs until the next `yield`, suspends, and returns the yielded value.

### How `yield` Works — Step by Step

```python
def countdown(n: int):
    print(f"Starting countdown from {n}")
    while n > 0:
        yield n          # Suspends here, returns n to caller
        # Execution resumes HERE on next next() call
        print(f"  Resumed after yielding {n}")
        n -= 1
    print("Done!")
    # Reaching end of function raises StopIteration


gen = countdown(3)    # Returns generator object. Body NOT yet executed.
print("Generator created")

print(next(gen))   # Starting countdown from 3 → yields 3
                   # prints: "Starting countdown from 3", then returns 3

print(next(gen))   # Resumed after yielding 3 → yields 2
                   # prints: "  Resumed after yielding 3", then returns 2

print(next(gen))   # Resumed after yielding 2 → yields 1
# next(gen)        # Resumed after yielding 1 → "Done!" → StopIteration
```

### Practical Generator Patterns

```python
# Infinite sequence generator
def naturals(start: int = 0):
    n = start
    while True:
        yield n
        n += 1


# Take first n from an infinite generator
from itertools import islice
first_10 = list(islice(naturals(), 10))   # [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]

# Fibonacci generator — infinite
def fibonacci():
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b


fib = fibonacci()
first_10_fib = [next(fib) for _ in range(10)]   # [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]

# Generator for reading large files — memory efficient
def read_in_chunks(filepath: str, chunk_size: int = 1024):
    with open(filepath, "r") as f:
        while True:
            chunk = f.read(chunk_size)
            if not chunk:
                break
            yield chunk


# Data pipeline using generators
def read_logs(filepath):
    with open(filepath) as f:
        for line in f:
            yield line.strip()

def filter_errors(lines):
    for line in lines:
        if "ERROR" in line:
            yield line

def parse_messages(lines):
    for line in lines:
        parts = line.split("|")
        yield {"level": parts[0], "message": parts[1] if len(parts) > 1 else line}

# Pipeline — no intermediate lists, processes one item at a time
# pipeline = parse_messages(filter_errors(read_logs("app.log")))
# for record in pipeline: ...
```

### Generator State and `return`

```python
def gen_with_return():
    yield 1
    yield 2
    return "final value"   # Value is attached to StopIteration exception

g = gen_with_return()
print(next(g))    # 1
print(next(g))    # 2
try:
    next(g)
except StopIteration as e:
    print(e.value)   # "final value"
```

---

## 8. `yield from` — Delegating to Sub-Generators

### Must Know

`yield from iterable` is equivalent to `for x in iterable: yield x` but:
1. More efficient — avoids the inner loop overhead.
2. Properly passes `.send()` and `.throw()` to the sub-generator.
3. Captures the sub-generator's `return` value.

### How It Works

```python
# Without yield from
def chain_v1(*iterables):
    for it in iterables:
        for item in it:
            yield item

# With yield from — cleaner, same result
def chain_v2(*iterables):
    for it in iterables:
        yield from it   # Delegates to each iterable in turn


print(list(chain_v1([1, 2], [3, 4], [5])))   # [1, 2, 3, 4, 5]
print(list(chain_v2([1, 2], [3, 4], [5])))   # [1, 2, 3, 4, 5]

# Recursive generator with yield from
def flatten(nested):
    """Flatten arbitrarily nested lists."""
    for item in nested:
        if isinstance(item, list):
            yield from flatten(item)   # Recurse into sub-list
        else:
            yield item


data = [1, [2, [3, 4], 5], [6, 7]]
print(list(flatten(data)))   # [1, 2, 3, 4, 5, 6, 7]
```

---

## 9. Generator `.send()` — Two-Way Communication

### Must Know

Generators are not just producers — they can also receive values via `.send(value)`. This is the foundation of Python's coroutine model (pre-`async/await`).

```python
def accumulator():
    """Receives values via send(), yields running total."""
    total = 0
    while True:
        value = yield total   # yield sends total OUT; send() pushes value IN
        if value is None:
            break
        total += value


acc = accumulator()
next(acc)         # Prime the generator — advance to first yield
                  # Must call next() once before send() — otherwise TypeError

print(acc.send(10))   # 10
print(acc.send(20))   # 30
print(acc.send(5))    # 35

# .throw() — inject an exception into the generator
# .close() — raises GeneratorExit inside the generator
```

---

## 10. `itertools` Module — Essential for Interviews

### Must Know

`itertools` provides fast, memory-efficient iteration tools. Interviewers ask about these for algorithm and data processing questions. All `itertools` functions return lazy iterators.

### Infinite Iterators

```python
import itertools

# count(start, step) — infinite counting
for i in itertools.islice(itertools.count(10, 2), 5):
    print(i)   # 10, 12, 14, 16, 18

# cycle(iterable) — infinite cycling
colors = itertools.cycle(["red", "green", "blue"])
for _ in range(7):
    print(next(colors))   # red, green, blue, red, green, blue, red

# repeat(value, n) — repeat n times (or infinitely if n omitted)
print(list(itertools.repeat(0, 5)))   # [0, 0, 0, 0, 0]
```

### Combinatoric Iterators

```python
import itertools

items = ["A", "B", "C"]

# product — cartesian product (nested loops)
print(list(itertools.product([0,1], repeat=3)))
# All 3-bit binary numbers: [(0,0,0),(0,0,1),(0,1,0),...,(1,1,1)]

print(list(itertools.product(["H","T"], repeat=3)))
# All outcomes of 3 coin flips

# permutations — ordered arrangements
print(list(itertools.permutations(items, 2)))
# [('A','B'),('A','C'),('B','A'),('B','C'),('C','A'),('C','B')]

# combinations — unordered selections
print(list(itertools.combinations(items, 2)))
# [('A','B'),('A','C'),('B','C')]

# combinations_with_replacement — can repeat elements
print(list(itertools.combinations_with_replacement(items, 2)))
# [('A','A'),('A','B'),('A','C'),('B','B'),('B','C'),('C','C')]
```

### Terminating Iterators

```python
import itertools

# chain — concatenate iterables
print(list(itertools.chain([1,2], [3,4], [5])))       # [1, 2, 3, 4, 5]
print(list(itertools.chain.from_iterable([[1,2],[3,4]])))  # [1, 2, 3, 4]

# islice — slice an iterator (like list[start:stop:step] but lazy)
print(list(itertools.islice(range(100), 5)))          # [0, 1, 2, 3, 4]
print(list(itertools.islice(range(100), 10, 20, 2)))  # [10, 12, 14, 16, 18]

# compress — filter by boolean selectors
data = ["a", "b", "c", "d", "e"]
selectors = [1, 0, 1, 0, 1]
print(list(itertools.compress(data, selectors)))      # ['a', 'c', 'e']

# dropwhile / takewhile
nums = [1, 2, 3, 4, 5, 1, 2]
print(list(itertools.takewhile(lambda x: x < 4, nums)))   # [1, 2, 3]
print(list(itertools.dropwhile(lambda x: x < 4, nums)))   # [4, 5, 1, 2]

# filterfalse — opposite of filter
print(list(itertools.filterfalse(lambda x: x % 2, range(10))))  # [0, 2, 4, 6, 8]

# starmap — map with argument unpacking
pairs = [(1, 2), (3, 4), (5, 6)]
print(list(itertools.starmap(lambda a, b: a + b, pairs)))  # [3, 7, 11]
```

### `groupby` — Group Consecutive Elements

```python
import itertools

# CRITICAL: Input MUST be sorted by the same key first!
data = [
    {"name": "Alice", "dept": "Engineering"},
    {"name": "Bob", "dept": "Engineering"},
    {"name": "Carol", "dept": "Marketing"},
    {"name": "Dave", "dept": "Marketing"},
    {"name": "Eve", "dept": "Engineering"},  # Not grouped with others!
]

# WRONG — not sorted first
for key, group in itertools.groupby(data, key=lambda x: x["dept"]):
    print(key, [g["name"] for g in group])
# Engineering ['Alice', 'Bob']
# Marketing ['Carol', 'Dave']
# Engineering ['Eve']   ← Eve is in a new group!

# CORRECT — sort first
sorted_data = sorted(data, key=lambda x: x["dept"])
for key, group in itertools.groupby(sorted_data, key=lambda x: x["dept"]):
    print(key, [g["name"] for g in group])
# Engineering ['Alice', 'Bob', 'Eve']
# Marketing ['Carol', 'Dave']
```

### `accumulate` — Running Totals

```python
import itertools
import operator

# Running sum (default)
print(list(itertools.accumulate([1, 2, 3, 4, 5])))
# [1, 3, 6, 10, 15]

# Running max
print(list(itertools.accumulate([3, 1, 4, 1, 5, 9, 2, 6], max)))
# [3, 3, 4, 4, 5, 9, 9, 9]

# Running product
print(list(itertools.accumulate([1, 2, 3, 4, 5], operator.mul)))
# [1, 2, 6, 24, 120]

# With initial value (Python 3.8+)
print(list(itertools.accumulate([1, 2, 3], initial=100)))
# [100, 101, 103, 106]
```

### `zip_longest` and `pairwise`

```python
import itertools

# zip_longest — pads shorter iterable
a = [1, 2, 3]
b = ["a", "b"]
print(list(itertools.zip_longest(a, b, fillvalue=None)))
# [(1, 'a'), (2, 'b'), (3, None)]

# pairwise (Python 3.10+) — consecutive pairs
print(list(itertools.pairwise([1, 2, 3, 4, 5])))
# [(1, 2), (2, 3), (3, 4), (4, 5)]

# Pre-3.10 equivalent:
def pairwise_old(iterable):
    a, b = itertools.tee(iterable)
    next(b, None)
    return zip(a, b)
```

### `tee` — Clone an Iterator

```python
import itertools

# tee creates n independent iterators from one
# WARNING: If one iterator advances far ahead of others, memory is used to buffer
gen = (x**2 for x in range(5))
it1, it2 = itertools.tee(gen, 2)
print(list(it1))   # [0, 1, 4, 9, 16]
print(list(it2))   # [0, 1, 4, 9, 16]  — independent copy
# gen is now partially/fully consumed — do not use gen after tee!
```

---

## 11. `collections` Module — Specialized Containers

### `collections.deque` — Double-Ended Queue

```python
from collections import deque

# O(1) append and pop from BOTH ends (list is O(n) for left operations)
dq = deque([1, 2, 3])

dq.append(4)           # Right end: [1, 2, 3, 4]
dq.appendleft(0)       # Left end: [0, 1, 2, 3, 4]
dq.pop()               # Remove right: 4  → [0, 1, 2, 3]
dq.popleft()           # Remove left: 0   → [1, 2, 3]

dq.extend([4, 5])       # Right: [1, 2, 3, 4, 5]
dq.extendleft([0, -1])  # Left (each prepended): [-1, 0, 1, 2, 3, 4, 5]

dq.rotate(2)           # Rotate right by 2
dq.rotate(-2)          # Rotate left by 2

# maxlen — fixed-size sliding window
last_3 = deque(maxlen=3)
for x in range(10):
    last_3.append(x)
print(last_3)   # deque([7, 8, 9], maxlen=3) — oldest auto-evicted

# Use deque for:
# - BFS queue (popleft)
# - Sliding window (maxlen)
# - Stack + Queue operations on same structure
```

### `collections.Counter`

```python
from collections import Counter

# Create
c = Counter("mississippi")
print(c)   # Counter({'i': 4, 's': 4, 'p': 2, 'm': 1})

c2 = Counter(["cat", "dog", "cat", "bird", "cat"])
print(c2)  # Counter({'cat': 3, 'dog': 1, 'bird': 1})

c3 = Counter(a=3, b=2, c=1)

# Common operations
print(c2.most_common(2))      # [('cat', 3), ('dog', 1)]  — top 2
print(c2["cat"])               # 3
print(c2["elephant"])          # 0  — no KeyError!
print(c2.total())              # 5  — Python 3.10+

# Arithmetic
c4 = Counter(["cat", "fish"])
print(c2 + c4)    # Counter({'cat': 4, 'dog': 1, 'bird': 1, 'fish': 1})
print(c2 - c4)    # Counter({'dog': 1, 'bird': 1}) — removes non-positive
print(c2 & c4)    # Counter({'cat': 1})  — intersection (minimum)
print(c2 | c4)    # Counter({'cat': 3, 'dog': 1, 'bird': 1, 'fish': 1})  — union (max)

# Update (additive) vs subtract
c2.update(["cat", "cat"])   # Adds to existing counts
c2.subtract(["cat"])        # Subtracts (can go negative)
```

### `collections.defaultdict`

```python
from collections import defaultdict

# Never raises KeyError — creates default value on first access
word_count = defaultdict(int)     # Default: 0 (int())
word_count["apple"] += 1
word_count["banana"] += 2
print(dict(word_count))   # {'apple': 1, 'banana': 2}

# Group by using defaultdict(list)
by_length = defaultdict(list)
for word in ["cat", "dog", "ant", "elephant", "bee"]:
    by_length[len(word)].append(word)
print(dict(by_length))   # {3: ['cat', 'dog', 'ant', 'bee'], 8: ['elephant']}

# Nested defaultdict (dict of dicts)
matrix = defaultdict(lambda: defaultdict(int))
matrix["row1"]["col1"] = 10
matrix["row1"]["col2"] = 20
print(dict(matrix["row1"]))   # {'col1': 10, 'col2': 20}

# TRAP: defaultdict creates the key on access — even just reading!
dd = defaultdict(int)
print("x" in dd)   # False — 'in' does NOT create the key
_ = dd["x"]        # This DOES create the key with default 0
print("x" in dd)   # True — now it exists
```

### `collections.OrderedDict`

```python
from collections import OrderedDict

# Python 3.7+ regular dict preserves insertion order
# OrderedDict is mostly legacy, but has two unique features:

od = OrderedDict([("a", 1), ("b", 2), ("c", 3)])

# 1. move_to_end()
od.move_to_end("a")          # Move "a" to end
od.move_to_end("c", last=False)  # Move "c" to front
print(list(od.keys()))   # ['c', 'b', 'a']

# 2. Equality considers order
od1 = OrderedDict([("a", 1), ("b", 2)])
od2 = OrderedDict([("b", 2), ("a", 1)])
print(od1 == od2)    # False — order matters for OrderedDict equality

d1 = {"a": 1, "b": 2}
d2 = {"b": 2, "a": 1}
print(d1 == d2)      # True — regular dict equality ignores order

# LRU cache implementation pattern
class LRUCache:
    def __init__(self, capacity: int):
        self.capacity = capacity
        self.cache = OrderedDict()

    def get(self, key: int) -> int:
        if key not in self.cache:
            return -1
        self.cache.move_to_end(key)   # Mark as recently used
        return self.cache[key]

    def put(self, key: int, value: int) -> None:
        if key in self.cache:
            self.cache.move_to_end(key)
        self.cache[key] = value
        if len(self.cache) > self.capacity:
            self.cache.popitem(last=False)   # Evict least recently used
```

### `collections.namedtuple`

```python
from collections import namedtuple

# Factory function approach (legacy)
Point = namedtuple("Point", ["x", "y"])
p = Point(3, 4)
print(p.x, p.y)    # 3 4
print(p[0])        # 3 — still a tuple
print(p._asdict()) # {'x': 3, 'y': 4}

# Modern approach: NamedTuple class (see also OOP file)
from typing import NamedTuple

class Employee(NamedTuple):
    name: str
    department: str
    salary: float = 0.0

e = Employee("Alice", "Engineering", 90000)
print(e.name)       # Alice
print(e._replace(salary=95000))  # Returns new instance with changed field
```

### `collections.ChainMap`

```python
from collections import ChainMap

# Layered lookup — searches maps in order, first match wins
defaults = {"color": "blue", "timeout": 30, "debug": False}
config_file = {"timeout": 60}
env_vars = {"debug": True}
cli_args = {}

# Priority: cli_args > env_vars > config_file > defaults
settings = ChainMap(cli_args, env_vars, config_file, defaults)
print(settings["color"])    # 'blue'   — from defaults
print(settings["timeout"])  # 60       — from config_file overrides default
print(settings["debug"])    # True     — from env_vars overrides default

# Writes go to the first map
settings["color"] = "red"
print(cli_args)   # {'color': 'red'}  — written to first map
print(defaults)   # {'color': 'blue', ...}  — original unchanged

# Common use: scope management, config layering
```

---

## 12. Advanced Iteration Patterns

### Star Unpacking in Assignment

```python
# Assign first and last, collect middle in a list
first, *middle, last = [1, 2, 3, 4, 5]
print(first)    # 1
print(middle)   # [2, 3, 4]
print(last)     # 5

# Head and tail
head, *tail = [10, 20, 30, 40]
print(head)    # 10
print(tail)    # [20, 30, 40]

# Common pattern: process first, recur on rest
def my_sum(lst):
    if not lst:
        return 0
    first, *rest = lst
    return first + my_sum(rest)
```

### `zip` and Unzip

```python
keys = ["a", "b", "c"]
values = [1, 2, 3]

# Zip
pairs = list(zip(keys, values))   # [('a', 1), ('b', 2), ('c', 3)]

# Unzip — zip(*zipped) transposes the structure
unzipped_keys, unzipped_values = zip(*pairs)
print(list(unzipped_keys))    # ['a', 'b', 'c']
print(list(unzipped_values))  # [1, 2, 3]

# Matrix transpose using zip(*matrix)
matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
transposed = list(zip(*matrix))
# [(1, 4, 7), (2, 5, 8), (3, 6, 9)]
```

### `enumerate` with `start`

```python
items = ["apple", "banana", "cherry"]

for i, item in enumerate(items):
    print(f"{i}: {item}")    # 0: apple, 1: banana, 2: cherry

for i, item in enumerate(items, start=1):
    print(f"{i}. {item}")    # 1. apple, 2. banana, 3. cherry

# Create index dict
item_to_index = {item: i for i, item in enumerate(items)}
# {'apple': 0, 'banana': 1, 'cherry': 2}
```

### `sorted` with Complex Keys

```python
# Multiple sort keys using tuple
employees = [
    {"name": "Alice", "dept": "Eng", "salary": 90000},
    {"name": "Bob", "dept": "Eng", "salary": 85000},
    {"name": "Carol", "dept": "Marketing", "salary": 92000},
    {"name": "Dave", "dept": "Marketing", "salary": 85000},
]

# Sort by dept ascending, then salary descending
result = sorted(employees, key=lambda e: (e["dept"], -e["salary"]))

# Sort stability — Python sort is stable (equal keys preserve original order)
# Can use two-pass sort for stable multi-key sorting
by_salary = sorted(employees, key=lambda e: e["salary"])
by_dept = sorted(by_salary, key=lambda e: e["dept"])  # Stable — preserves salary order within dept
```

---

## 13. Java Developer Bridge — Iteration Summary

| Java Concept | Python Equivalent | Key Difference |
|---|---|---|
| `Iterable<T>` | Any object with `__iter__` | Protocol-based, no inheritance needed |
| `Iterator<T>` with `hasNext()`/`next()` | `__next__()` raises `StopIteration` | Python: exception signals end; Java: boolean test |
| `for (T x : collection)` | `for x in collection:` | Same semantics |
| `Stream<T>` | Generator expression or generator function | Python generators are inherently lazy like Streams |
| `Stream.filter(pred)` | `(x for x in iterable if pred(x))` | Generator expression |
| `Stream.map(f)` | `(f(x) for x in iterable)` | Generator expression |
| `Stream.flatMap(f)` | `(y for x in iterable for y in f(x))` | Nested generator expression |
| `Stream.collect(toList())` | `list(generator)` | Materialize lazy sequence |
| `Stream.limit(n)` | `itertools.islice(gen, n)` | |
| `IntStream.range(a, b)` | `range(a, b)` | |
| `Collections.frequency()` | `Counter(iterable)` | Python Counter is much richer |
| `ArrayList` with `addAll` | `list.extend()` or `[*a, *b]` | |
| `ArrayDeque` | `collections.deque` | Python deque has `rotate()`, `maxlen` |
| `HashMap.getOrDefault()` | `dict.get(k, default)` or `defaultdict` | |
| `LinkedHashMap` insertion-order | Regular `dict` | Python 3.7+ dict is already ordered |
| No equivalent | `itertools.groupby` | SQL-style grouping on sorted data |
| `Collections.nCopies(n, obj)` | `[obj] * n` (beware mutable!) or `itertools.repeat(obj, n)` | |
| Manual zip | `zip(a, b)` | Built-in, lazy |
| No equivalent | `itertools.combinations`, `itertools.product` | Combinatorics built-in |

---

## 14. Hot Interview Q&A

**Q: What is the difference between a list comprehension and a generator expression?**  
A: A list comprehension `[x**2 for x in range(n)]` is eager — all elements are computed immediately and stored in memory as a list. A generator expression `(x**2 for x in range(n))` is lazy — it creates a generator object that computes elements on demand, one at a time. The generator uses O(1) memory regardless of `n`, while the list uses O(n). Use generator expressions when passing to single-use consumers like `sum()`, `max()`, or `any()`.

**Q: What happens when you exhaust a generator?**  
A: Once a generator raises `StopIteration` (either by reaching the end of the function body or by executing a bare `return`), it is exhausted. All subsequent `next()` calls also raise `StopIteration`. The generator cannot be reset — you must create a new one.

**Q: What is the difference between an iterable and an iterator?**  
A: An iterable has `__iter__()` and produces a fresh iterator each time. A list is iterable — you can loop over it many times. An iterator has both `__iter__()` (returns `self`) and `__next__()`. It is stateful and can only be consumed once. All iterators are iterables, but not all iterables are iterators.

**Q: Must `itertools.groupby` input be sorted?**  
A: Yes — `groupby` only groups **consecutive** equal elements. If elements with the same key are not adjacent, they appear in separate groups. Always `sorted(data, key=keyfunc)` before `groupby(sorted_data, key=keyfunc)` using the same key.

**Q: Why is `collections.deque` preferred over `list` for a queue?**  
A: `list.insert(0, item)` and `list.pop(0)` are O(n) because all elements must shift. `deque.appendleft()` and `deque.popleft()` are O(1) because deque is a doubly-linked list of fixed-size blocks. For BFS, sliding window, and FIFO queues always use `deque`.

**Q: What does `defaultdict(list)` do and what is the trap?**  
A: `defaultdict(list)` creates an empty list as the default value when a missing key is accessed. This allows `d[key].append(item)` without checking if the key exists. The trap is that merely reading a missing key (`val = d[missing_key]`) creates the key with the default value — different from regular dict where that raises `KeyError`.

**Q: How is `Counter` different from a regular dict?**  
A: `Counter` never raises `KeyError` for missing keys — returns `0` instead. It has `most_common()`, supports arithmetic (`+`, `-`, `&`, `|`), `update()` (additive, not replacing), and `subtract()` (can produce negative counts). It's also initialized from any iterable and counts occurrences automatically.

**Q: What is `yield from` and why use it?**  
A: `yield from iterable` is syntactic sugar for `for x in iterable: yield x`, but also correctly handles `.send()` and `.throw()` pass-through to sub-generators, and captures the sub-generator's `return` value. It's essential for recursive generators and for building coroutine chains.

**Q: What is the memory difference between `list(range(10**8))` and `range(10**8)`?**  
A: `list(range(10**8))` allocates ~800 MB. `range(10**8)` is a range object — it stores only `start`, `stop`, `step` and computes values on demand, using just ~48 bytes regardless of size.

**Q: What does `zip(*matrix)` do?**  
A: `zip(*matrix)` transposes a matrix. The `*` operator unpacks the outer list into separate arguments to `zip`, so `zip([[1,2],[3,4],[5,6]])` becomes `zip([1,2], [3,4], [5,6])` which pairs first elements, then second elements: `[(1,3,5), (2,4,6)]`.

---

## 15. Final Revision Checklist

### Iterator Protocol

- [ ] I know the difference between iterable (`__iter__`) and iterator (`__iter__` + `__next__`)
- [ ] I know all iterators are iterables but not vice versa
- [ ] I can implement a custom iterator class with `__iter__` returning `self` and `__next__` raising `StopIteration`
- [ ] I know iterators are one-time use — re-iterating gives empty results

### Comprehensions

- [ ] I can write list, dict, and set comprehensions with filter conditions
- [ ] I know the comprehension variable scope is local in Python 3 (doesn't leak)
- [ ] I can write nested comprehensions (inner loop reads left to right in the expression)
- [ ] I know `{}` without `:` is a set comprehension, not a dict; empty `{}` is a dict

### Generator Expressions vs List Comprehensions

- [ ] I know generators are lazy (O(1) memory); list comprehensions are eager (O(n) memory)
- [ ] I know generators are exhausted after one pass — cannot re-iterate
- [ ] I use generator expressions when passing to `sum()`, `max()`, `any()`, `all()`

### Generator Functions

- [ ] I can write a generator function with `yield` and explain the suspension/resume model
- [ ] I know calling a generator function returns a generator object — body not yet run
- [ ] I can write an infinite generator and consume it with `itertools.islice`
- [ ] I understand `yield from` for delegation and recursive generators

### `itertools`

- [ ] I know `chain`, `islice`, `product`, `permutations`, `combinations` by heart
- [ ] I know `groupby` requires sorted input — always sort first with same key
- [ ] I can use `accumulate` for running totals
- [ ] I know `tee` clones an iterator but buffers data — do not use original after tee

### `collections`

- [ ] I know `deque` is O(1) for both ends vs `list` which is O(n) for left operations
- [ ] I can use `deque(maxlen=n)` for a sliding window
- [ ] I know `Counter["missing_key"]` returns 0, not KeyError
- [ ] I know `defaultdict` creates the key on access — even reading creates it
- [ ] I know when to use `OrderedDict` vs regular dict (move_to_end, order-sensitive equality)
- [ ] I can implement LRU cache with `OrderedDict.move_to_end()` and `popitem(last=False)`

### Java Developer Reminders

- [ ] Python generators replace Java Streams for lazy processing — but generators are one-time use
- [ ] `Counter` replaces manual `HashMap.getOrDefault(k, 0) + 1` patterns
- [ ] `defaultdict(list)` replaces Java `computeIfAbsent(k, k -> new ArrayList<>())`
- [ ] `deque.popleft()` is O(1); `list.pop(0)` is O(n) — never use list as a queue!
- [ ] `zip(*matrix)` transposes — no Stream equivalent; Java needs nested loops

---

*File 6 of 7 — Group 1: Starter Path*  
*Next: Python-Exception-Handling-Context-Managers-Gold-Sheet.md*
