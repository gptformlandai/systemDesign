# Python Data Structures Internals & Complexity — Gold Sheet

> **Track**: Python Interview Track — Group 2: Intermediate Backend  
> **File**: 1 of 5 (Track File #8)  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: All 7 files in 01-Starter-Path/

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| `dict` internal hash table — open addressing | ★★★★★ | Java `HashMap` uses chaining; Python `dict` uses open addressing — different collision strategy |
| `list` amortized O(1) append — growth factor | ★★★★★ | Classic interview question: why is `append()` O(1) amortized? |
| Big-O for all built-in operations | ★★★★★ | Must recite O(1)/O(n)/O(log n) without hesitation for any Python type |
| `dict` key ordering and iteration guarantees | ★★★★☆ | Java `HashMap` is unordered; Python `dict` is ordered since 3.7 — why and how |
| `heapq` — min-heap operations | ★★★★☆ | Java `PriorityQueue`; Python uses module-level functions on a list — different interface |
| `set` internals and membership O(1) | ★★★★☆ | Same underlying mechanism as dict — hash table |
| `dict` worst-case O(n) — hash collision attack | ★★★☆☆ | Java has same vulnerability; Python randomizes hash seeds since 3.3 |
| `list` vs `deque` — when O(1) matters | ★★★☆☆ | `list.insert(0, x)` is O(n); `deque.appendleft(x)` is O(1) |
| Space complexity of comprehensions vs generators | ★★★☆☆ | Generators are O(1) space; list comprehensions are O(n) |
| `sortedcontainers.SortedList` — BST alternative | ★★☆☆☆ | Java `TreeSet`/`TreeMap`; Python has no built-in sorted container |

---

## 2. `list` Internals — Dynamic Array

### Must Know

Python `list` is a **dynamic array** (like Java `ArrayList`). Internally it stores a C array of pointers to objects.

- **Append is amortized O(1)** — when the backing array is full, Python allocates a new array roughly 1.125× larger (not exactly 2× like Java's ArrayList), copies all pointers, then appends.
- **Insert/delete in the middle is O(n)** — all elements after the position must shift.
- **Indexing is O(1)** — direct pointer offset.

### Growth Pattern

```python
import sys

lst = []
prev_size = sys.getsizeof(lst)
print(f"Empty list: {prev_size} bytes")

for i in range(20):
    lst.append(i)
    curr_size = sys.getsizeof(lst)
    if curr_size != prev_size:
        print(f"  After append({i}): {curr_size} bytes  ← reallocation")
        prev_size = curr_size

# Empty list: 56 bytes
# After append(0): 88 bytes  ← allocated for 4 pointers
# After append(4): 120 bytes ← allocated for 8 pointers
# After append(8): 184 bytes ← allocated for 16 pointers
# Growth sequence: 0, 4, 8, 16, 25, 35, 46, 58, 72, 88 ...
# Growth factor approaches 1.125 (9/8) — more conservative than Java's 2x
```

### `list` Complexity Reference

| Operation | Average | Worst | Notes |
|---|---|---|---|
| `lst[i]` | O(1) | O(1) | Direct offset |
| `lst[i] = x` | O(1) | O(1) | |
| `lst.append(x)` | **O(1) amortized** | O(n) | O(n) only on reallocation |
| `lst.pop()` | O(1) | O(1) | Remove last |
| `lst.pop(0)` | **O(n)** | O(n) | Shifts all elements |
| `lst.insert(i, x)` | O(n) | O(n) | Shifts elements after i |
| `lst.remove(x)` | O(n) | O(n) | Scan + shift |
| `x in lst` | O(n) | O(n) | Linear scan |
| `lst.index(x)` | O(n) | O(n) | |
| `len(lst)` | O(1) | O(1) | Cached as field |
| `lst.sort()` | O(n log n) | O(n log n) | Timsort |
| `lst.reverse()` | O(n) | O(n) | |
| `lst[a:b]` | O(k) | O(k) | k = b - a |
| `lst + lst2` | O(n+m) | O(n+m) | Creates new list |
| `lst.extend(iter)` | O(k) | O(k) | k = len of iterable |
| `lst.count(x)` | O(n) | O(n) | |
| `min(lst)`, `max(lst)` | O(n) | O(n) | |

### Timsort — Python's Sort Algorithm

```python
# Python uses Timsort — hybrid of merge sort and insertion sort
# Key properties:
# - Stable sort: equal elements preserve original order
# - Best case O(n) for already-sorted or nearly-sorted data (detects natural runs)
# - Worst case O(n log n)
# - Space: O(n) for merge buffers
# - Same algorithm used in Java's Arrays.sort() for objects since Java 7!

# Stable sort guarantees — critical for multi-key sorting
data = [("Alice", 30), ("Bob", 25), ("Carol", 30)]
sorted_by_age = sorted(data, key=lambda x: x[1])
# [('Bob', 25), ('Alice', 30), ('Carol', 30)]  — Alice before Carol (stable)
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Dynamic array | `ArrayList<T>` | `list` |
| Growth factor | 1.5× (OpenJDK) or 2× | ~1.125× — more conservative |
| Sort algorithm | `Arrays.sort()` on primitives: dual-pivot quicksort; on objects: Timsort | Timsort always |
| Sort stability | Timsort for objects (stable), quicksort for primitives (unstable) | Always stable |
| `size()` / `len()` | O(1) — stored as field | O(1) — stored as field |
| Remove at index | `list.remove(int index)` → O(n) | `list.pop(i)` → O(n) |
| `contains()` / `in` | O(n) for `ArrayList` | O(n) for `list` |

---

## 3. `dict` Internals — Open Addressing Hash Table

### Must Know

Python `dict` is a **hash table with open addressing** (specifically compact hash tables since Python 3.6). This is different from Java's `HashMap` which uses **separate chaining** (linked lists/trees at each bucket).

### How It Works Internally

```
Python dict (Python 3.6+ compact format):
┌─────────────────────────────────┐
│  Indices array (sparse)         │  8 slots initially (2/3 load factor)
│  [_, 0, _, _, 2, _, 1, _]       │  _ = empty, number = entry index
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│  Entries array (dense)          │  Only stores actual key-value pairs
│  [(hash, key, val), ...]        │  More cache-friendly than chaining
└─────────────────────────────────┘
```

**Lookup process**:
1. Compute `hash(key)`
2. `slot = hash(key) % len(indices)`
3. If `indices[slot]` is empty → key not found
4. If `indices[slot]` points to an entry with matching key → found
5. If hash collision (slot occupied, different key) → probe next slot (perturbation-based)
6. Repeat until empty slot or match

### Open Addressing vs Chaining

```
Java HashMap (chaining):          Python dict (open addressing):
  slot 0: [A → B → C]              slot 0: [A]
  slot 1: []                        slot 1: [empty]
  slot 2: [D]                       slot 2: [B]   ← B collided with A, probed here
  ...                               slot 3: [C]   ← C collided, probed further
                                    ...

Chaining: extra memory for linked lists; cache-unfriendly
Open addressing: all data in contiguous memory; cache-friendly; needs load factor control
```

### Load Factor and Resizing

```python
import sys

d = {}
prev_size = sys.getsizeof(d)
print(f"Empty dict: {prev_size} bytes")

for i in range(20):
    d[i] = i
    curr_size = sys.getsizeof(d)
    if curr_size != prev_size:
        print(f"  After inserting key {i}: {curr_size} bytes  ← resize")
        prev_size = curr_size

# Python resizes when dict reaches 2/3 full (load factor = 0.667)
# Resize doubles the underlying array: 8 → 16 → 32 → 64 ...
# After resize: O(n) to rebuild all hashes — amortized O(1) for insert
```

### Why Python `dict` Is O(1) Average But O(n) Worst Case

```python
# Hash collision attack — adversary crafts keys that all hash to the same slot
# This degrades lookup from O(1) to O(n)

# Python's defense since 3.3: HASH RANDOMIZATION
# hash("hello") is different on each interpreter startup
import os
# Python sets PYTHONHASHSEED randomly at startup (or use PYTHONHASHSEED=0 to disable)
print(hash("hello"))   # Different every run!

# Integers and floats have stable hashes:
print(hash(1))     # Always 1
print(hash(1.0))   # Always 1  (equal values must have equal hashes: 1 == 1.0)

# Strings and bytes have randomized hashes (PYTHONHASHSEED)
```

### `dict` Complexity Reference

| Operation | Average | Worst | Notes |
|---|---|---|---|
| `d[key]` | O(1) | O(n) | Worst case: many hash collisions |
| `d[key] = val` | O(1) | O(n) | |
| `del d[key]` | O(1) | O(n) | Leaves tombstone |
| `key in d` | O(1) | O(n) | |
| `d.get(key)` | O(1) | O(n) | |
| `len(d)` | O(1) | O(1) | Cached |
| `d.keys()`, `.values()`, `.items()` | O(1) | O(1) | Return view objects |
| Iteration over dict | O(n) | O(n) | |
| `d.copy()` | O(n) | O(n) | Shallow copy |
| `d.update(other)` | O(len(other)) | O(n) | |

### Why `dict` Is Ordered Since Python 3.7

```python
# Python 3.6: compact dict as implementation detail (CPython)
# Python 3.7: insertion-order preservation as language guarantee

# The compact format (index array + dense entries array) naturally preserves
# insertion order because new entries are appended to the dense entries array
# in insertion order.

# Java HashMap is NOT ordered; LinkedHashMap preserves insertion order
d = {}
d["c"] = 3
d["a"] = 1
d["b"] = 2
print(list(d.keys()))   # ['c', 'a', 'b']  — insertion order preserved

# dict equality IGNORES order (unlike OrderedDict)
print({"a": 1, "b": 2} == {"b": 2, "a": 1})   # True
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Hash table type | Separate chaining | Open addressing (compact since 3.6) |
| Load factor default | 0.75 | ~0.667 (2/3) |
| Resize | Doubles capacity | Doubles capacity |
| Ordering | `HashMap` unordered, `LinkedHashMap` ordered | `dict` ordered since 3.7 |
| Hash randomization | `String.hashCode()` is deterministic | Randomized per process (PYTHONHASHSEED) |
| Hash of equal ints/floats | `Integer.hashCode(1) != Double.hashCode(1.0)` | `hash(1) == hash(1.0)` — equal values equal hash |
| Null key | `HashMap` allows one null key | `None` is a valid dict key |

---

## 4. `set` Internals

### Must Know

Python `set` is essentially a `dict` with only keys (no values). It uses the same open addressing hash table. All `dict` complexity rules apply.

```python
# set internally stores only keys, not values
# Same hash table, same O(1) amortized ops

s = {1, 2, 3, 4, 5}

# O(1) operations
s.add(6)            # O(1) amortized
s.discard(3)        # O(1) amortized
print(4 in s)       # O(1) average — hash table lookup
print(len(s))       # O(1)

# O(n) or O(n+m) set operations
a = {1, 2, 3, 4, 5}
b = {3, 4, 5, 6, 7}
print(a | b)        # Union: O(len(a) + len(b))
print(a & b)        # Intersection: O(min(len(a), len(b)))
print(a - b)        # Difference: O(len(a))
print(a ^ b)        # Symmetric diff: O(len(a) + len(b))

# Subset check
print({1, 2}.issubset({1, 2, 3}))    # O(len(smaller))
```

### `set` vs `list` for Membership Testing

```python
import timeit

data_list = list(range(100_000))
data_set = set(range(100_000))
target = 99_999   # Worst case for list

# list membership — O(n) linear scan
t_list = timeit.timeit(lambda: target in data_list, number=10000)

# set membership — O(1) hash lookup
t_set = timeit.timeit(lambda: target in data_set, number=10000)

print(f"list: {t_list:.4f}s")   # ~0.8s
print(f"set:  {t_set:.4f}s")    # ~0.001s  — ~1000x faster

# RULE: If you need repeated membership checks on a large collection, convert to set first
```

### `set` Complexity Reference

| Operation | Average | Worst | Notes |
|---|---|---|---|
| `s.add(x)` | O(1) | O(n) | |
| `s.discard(x)` | O(1) | O(n) | No error if missing |
| `s.remove(x)` | O(1) | O(n) | KeyError if missing |
| `x in s` | O(1) | O(n) | |
| `len(s)` | O(1) | O(1) | |
| `s \| t` (union) | O(len(s)+len(t)) | | |
| `s & t` (intersection) | O(min(len(s),len(t))) | | |
| `s - t` (difference) | O(len(s)) | | |
| `s ^ t` (sym. diff) | O(len(s)+len(t)) | | |
| `s.issubset(t)` | O(len(s)) | | |
| `s.issuperset(t)` | O(len(t)) | | |

---

## 5. `deque` Internals — Doubly-Linked List of Blocks

### Must Know

`collections.deque` is implemented as a **doubly-linked list of fixed-size blocks** (not a doubly-linked list of single elements). This gives:
- O(1) append and pop from **both ends**
- O(n) random access by index
- O(n) insert/delete in the **middle**

```python
from collections import deque
import timeit

lst = list(range(100_000))
dq = deque(range(100_000))

# O(n) for list, O(1) for deque
t_list = timeit.timeit(lambda: lst.insert(0, 0), number=1000)
t_deq = timeit.timeit(lambda: dq.appendleft(0), number=1000)

print(f"list.insert(0): {t_list:.4f}s")    # ~2s
print(f"deque.appendleft: {t_deq:.4f}s")  # ~0.0001s
```

### `deque` Complexity Reference

| Operation | Average | Notes |
|---|---|---|
| `dq.append(x)` | O(1) | Right end |
| `dq.appendleft(x)` | O(1) | Left end |
| `dq.pop()` | O(1) | Right end |
| `dq.popleft()` | O(1) | Left end — the key advantage over list |
| `dq[i]` | O(n) | Random access is O(n) — use list if indexing needed |
| `dq[0]`, `dq[-1]` | O(1) | First/last element access is O(1) |
| `x in dq` | O(n) | Linear scan |
| `len(dq)` | O(1) | |
| `dq.rotate(n)` | O(n) | Rotate n steps |

### When to Use `deque` vs `list`

```python
# USE deque:
# - BFS (breadth-first search): use popleft() as the queue
from collections import deque

def bfs(graph, start):
    visited = set()
    queue = deque([start])
    while queue:
        node = queue.popleft()   # O(1) — deque
        if node not in visited:
            visited.add(node)
            queue.extend(graph[node])
    return visited

# - Sliding window of fixed size (maxlen)
window = deque(maxlen=3)
for x in [1, 2, 3, 4, 5]:
    window.append(x)
    print(list(window))
# [1] → [1,2] → [1,2,3] → [2,3,4] → [3,4,5]

# USE list:
# - When you need frequent random access by index
# - When you only add/remove from the right end
# - When interoperability with slice operations is needed
```

---

## 6. `heapq` — Min-Heap

### Must Know

Python's `heapq` module implements a **min-heap** (smallest element at index 0) as operations on a regular Python `list`. There is no `MaxHeap` class — negate values to simulate max-heap.

Java's `PriorityQueue` is also a min-heap by default. The interface difference: Java has a class with methods; Python has a module with functions that operate on a list.

### How It Works

```python
import heapq

# heapify — turn a list into a heap in-place: O(n)
nums = [5, 3, 8, 1, 9, 2, 7, 4, 6]
heapq.heapify(nums)
print(nums)    # [1, 3, 2, 4, 9, 8, 7, 5, 6] — heap property, NOT sorted!

# heappush — add element: O(log n)
heapq.heappush(nums, 0)
print(nums[0])   # 0 — smallest is always at index 0

# heappop — remove and return smallest: O(log n)
print(heapq.heappop(nums))   # 0
print(heapq.heappop(nums))   # 1

# heappushpop — push then pop (more efficient than separate calls): O(log n)
val = heapq.heappushpop(nums, 10)   # Returns min(pushed, current min)

# heapreplace — pop then push (must not be empty): O(log n)
val = heapq.heapreplace(nums, 0)    # Returns old min, pushes new

# nlargest / nsmallest
data = [5, 3, 8, 1, 9, 2, 7, 4, 6]
print(heapq.nlargest(3, data))     # [9, 8, 7]  — O(n + k log n)
print(heapq.nsmallest(3, data))    # [1, 2, 3]  — O(n + k log n)
# For k close to n, use sorted(); for small k, nlargest/nsmallest are faster
```

### Max-Heap Pattern

```python
import heapq

# Simulate max-heap by negating values
nums = [5, 3, 8, 1, 9, 2]
max_heap = [-x for x in nums]
heapq.heapify(max_heap)

print(-heapq.heappop(max_heap))   # 9  — largest
print(-heapq.heappop(max_heap))   # 8

# Push a new max value
heapq.heappush(max_heap, -10)
print(-heapq.heappop(max_heap))   # 10
```

### Heap with Custom Objects — Use Tuples

```python
import heapq

# Python compares tuples element by element
# Put the sort key FIRST in the tuple
tasks = []
heapq.heappush(tasks, (3, "low priority task"))
heapq.heappush(tasks, (1, "urgent task"))
heapq.heappush(tasks, (2, "medium priority task"))
heapq.heappush(tasks, (1, "another urgent task"))

while tasks:
    priority, task = heapq.heappop(tasks)
    print(f"  [{priority}] {task}")
# [1] urgent task
# [1] another urgent task
# [2] medium priority task
# [3] low priority task

# For objects where comparison is ambiguous, use a tie-breaker counter
import itertools
counter = itertools.count()

@dataclass
class Task:
    name: str
    priority: int

task_heap = []
heapq.heappush(task_heap, (1, next(counter), Task("urgent", 1)))
heapq.heappush(task_heap, (1, next(counter), Task("also urgent", 1)))
# counter ensures no comparison of Task objects (avoids TypeError)
```

### `heapq` Complexity Reference

| Operation | Complexity | Notes |
|---|---|---|
| `heapq.heapify(lst)` | O(n) | Floyd's algorithm |
| `heapq.heappush(heap, x)` | O(log n) | Sift up |
| `heapq.heappop(heap)` | O(log n) | Sift down |
| `heapq.heappushpop(heap, x)` | O(log n) | More efficient than push+pop |
| `heapq.heapreplace(heap, x)` | O(log n) | More efficient than pop+push |
| `heapq.nlargest(k, lst)` | O(n + k log n) | |
| `heapq.nsmallest(k, lst)` | O(n + k log n) | |
| Peek at minimum (no pop) | O(1) | `heap[0]` |

### Java Developer Bridge — `heapq`

| | Java | Python |
|---|---|---|
| Min-heap | `PriorityQueue<T>` (min by default) | `heapq` functions on a `list` |
| Max-heap | `PriorityQueue<>(Comparator.reverseOrder())` | Negate values: `heappush(h, -x)` |
| Custom order | `PriorityQueue<>(Comparator)` | Tuple with sort key first: `(priority, obj)` |
| Peek | `pq.peek()` — O(1) | `heap[0]` — O(1) |
| Pop | `pq.poll()` — O(log n) | `heapq.heappop(heap)` — O(log n) |
| Build from list | `new PriorityQueue<>(list)` — O(n) | `heapq.heapify(lst)` — O(n) |
| Thread safety | `PriorityBlockingQueue` | Use `queue.PriorityQueue` for thread safety |

---

## 7. `Counter` and `defaultdict` — Internal Details

### `Counter` Implementation

```python
from collections import Counter

# Counter is a dict subclass
c = Counter("abracadabra")
print(type(c).__mro__)
# Counter → dict → object

# Internally: just a dict with default 0 for missing keys
# Counter["missing"] returns 0 — it does NOT insert the key (unlike defaultdict)
c2 = Counter()
val = c2["x"]          # Returns 0
print("x" in c2)       # False — key was NOT created (unlike defaultdict)

# Most efficient ways to build Counter
c3 = Counter(["a", "b", "a", "c", "b", "a"])   # from iterable
c4 = Counter(a=3, b=2, c=1)                     # from keyword args
c5 = Counter({"a": 3, "b": 2})                 # from dict

# update() is additive — unlike dict.update() which replaces
c3.update(["a", "b"])
print(c3)  # Counter({'a': 5, 'b': 4, 'c': 1})

# elements() — iterator over elements (repeated count times)
print(list(Counter(a=2, b=3).elements()))  # ['a', 'a', 'b', 'b', 'b']
```

### `defaultdict` Implementation

```python
from collections import defaultdict

# defaultdict is also a dict subclass
# The default_factory is called with NO arguments when a missing key is accessed

dd = defaultdict(list)

# __missing__ is called when key is not found
# defaultdict overrides __missing__ to call default_factory() and store the result
dd["fruits"].append("apple")   # Calls list(), stores [], then appends "apple"
dd["fruits"].append("banana")
print(dd)   # defaultdict(<class 'list'>, {'fruits': ['apple', 'banana']})

# Use callable as default_factory
dd_int = defaultdict(int)      # Default: 0
dd_set = defaultdict(set)      # Default: set()
dd_list = defaultdict(list)    # Default: []
dd_zero = defaultdict(lambda: "unknown")  # Custom default

# TRAP: accessing a key creates it
dd = defaultdict(int)
if dd["new_key"] == 0:   # This line creates "new_key"!
    print("zero")
print(dict(dd))   # {'new_key': 0}

# SAFE check — use 'in' to avoid creating
dd2 = defaultdict(int)
if "new_key" in dd2:    # Does NOT create the key
    print(dd2["new_key"])
```

### Nested `defaultdict`

```python
from collections import defaultdict

# Nested defaultdict for sparse matrix / adjacency list
def make_nested():
    return defaultdict(int)

matrix = defaultdict(make_nested)
matrix[0][0] = 1
matrix[1][2] = 3
print(matrix[0][0])   # 1
print(matrix[1][2])   # 3
print(matrix[5][5])   # 0 — creates nested dicts on access

# Common pattern for adjacency list (graph)
graph = defaultdict(list)
edges = [(0,1), (0,2), (1,2), (2,3)]
for u, v in edges:
    graph[u].append(v)
    graph[v].append(u)   # Undirected
print(dict(graph))
# {0: [1, 2], 1: [0, 2], 2: [0, 1, 3], 3: [2]}
```

---

## 8. `str` Internals

### Must Know

```python
import sys

# Python str uses a tiered internal representation (PEP 393):
# - Latin-1 (1 byte/char) if all chars are in Latin-1 range
# - UCS-2 (2 bytes/char) if any char needs 2 bytes
# - UCS-4 (4 bytes/char) if any char needs 4 bytes (emoji, CJK beyond BMP)

print(sys.getsizeof(""))           # 49 bytes (empty string overhead)
print(sys.getsizeof("a"))          # 50 bytes (Latin-1: +1 byte)
print(sys.getsizeof("a" * 100))    # 149 bytes (+100 bytes)
print(sys.getsizeof("α"))          # 76 bytes (UCS-2: +2 bytes per char)
print(sys.getsizeof("🐍"))          # 80 bytes (UCS-4: +4 bytes per char)

# String interning — CPython automatically interns:
# 1. All string literals that look like identifiers (letters, digits, underscore)
# 2. Compile-time constant expressions

a = "hello"
b = "hello"
print(a is b)   # True — interned

a = "hello world"   # Contains space — may or may not be interned
b = "hello world"
print(a is b)   # True in CPython (literal interning), but NOT guaranteed

# Explicit interning with sys.intern
import sys
a = sys.intern("a string with spaces")
b = sys.intern("a string with spaces")
print(a is b)   # True — explicitly interned
```

### String Complexity

| Operation | Complexity | Notes |
|---|---|---|
| `s[i]` | O(1) | Direct offset |
| `len(s)` | O(1) | Cached |
| `s + t` | O(n+m) | Creates new string |
| `s * n` | O(n*len(s)) | Creates new string |
| `x in s` | O(n*m) | Substring search (Boyer-Moore-Horspool in CPython) |
| `s.find(sub)` | O(n*m) | |
| `s.split()` | O(n) | |
| `s.join(lst)` | O(n) | One allocation — fastest for building strings |
| `s.upper()`, `s.lower()` | O(n) | |
| `s == t` | O(n) | Compares char by char (after length check) |
| Concatenation in loop | O(n²) | `s += x` in loop creates n intermediate strings |

---

## 9. Complete Big-O Reference for Python Built-ins

### All Operations at a Glance

```
LIST
  Append right:     O(1) amortized
  Pop right:        O(1)
  Insert at i:      O(n)
  Pop at i:         O(n)
  Get item:         O(1)
  Set item:         O(1)
  Delete item:      O(n)
  Iteration:        O(n)
  Contains (in):    O(n)
  len():            O(1)
  Copy:             O(n)
  Extend:           O(k) — k = len of added items
  Sort:             O(n log n) — Timsort
  Reverse:          O(n)
  Min/Max:          O(n)
  Get slice:        O(k)

DICT
  Get item:         O(1) avg, O(n) worst
  Set item:         O(1) avg, O(n) worst
  Delete item:      O(1) avg, O(n) worst
  Contains (in):    O(1) avg, O(n) worst
  len():            O(1)
  Keys/values/items: O(1) — returns a view
  Iteration:        O(n)
  Copy:             O(n)
  Update:           O(k) avg

SET
  Add:              O(1) avg, O(n) worst
  Discard/Remove:   O(1) avg, O(n) worst
  Contains (in):    O(1) avg, O(n) worst
  len():            O(1)
  Union (|):        O(n+m)
  Intersection (&): O(min(n,m))
  Difference (-):   O(n)
  Symmetric diff:   O(n+m)
  issubset:         O(n)

DEQUE
  Append right:     O(1)
  Append left:      O(1)
  Pop right:        O(1)
  Pop left:         O(1)
  Get/set by index: O(n)  ← unlike list!
  Get first/last:   O(1)
  Contains (in):    O(n)
  len():            O(1)
  Rotate:           O(k)

STR
  Concatenate (+):  O(n+m)
  Slice:            O(k)
  Find/contains:    O(n*m)
  Join list:        O(n)
  Split:            O(n)
  len():            O(1)
```

---

## 10. `sortedcontainers` — The Missing Java TreeSet/TreeMap

### Must Know

Python has no built-in sorted container (Java has `TreeSet`, `TreeMap`). The third-party `sortedcontainers` library provides pure-Python sorted containers with O(log n) operations.

```python
from sortedcontainers import SortedList, SortedDict, SortedSet

# SortedList — always sorted, O(log n) add/remove/find
sl = SortedList([5, 1, 3, 2, 4])
print(sl)           # SortedList([1, 2, 3, 4, 5])
sl.add(3)           # O(log n)
sl.add(6)
print(sl)           # SortedList([1, 2, 3, 3, 4, 5, 6])
print(sl[0])        # 1  — smallest
print(sl[-1])       # 6  — largest
sl.discard(3)       # Removes one 3
print(sl.count(3))  # 1

# Binary search operations
print(sl.bisect_left(4))   # 3 — index of first element >= 4
print(sl.bisect_right(4))  # 4 — index of first element > 4

# SortedDict — dict with sorted keys
sd = SortedDict({"b": 2, "a": 1, "c": 3})
print(sd.keys())    # SortedKeysView(['a', 'b', 'c'])
print(sd.peekitem(0))   # ('a', 1) — smallest key
print(sd.peekitem(-1))  # ('c', 3) — largest key

# SortedSet
ss = SortedSet([3, 1, 4, 1, 5, 9, 2, 6])
print(ss)   # SortedSet([1, 2, 3, 4, 5, 6, 9])
```

### Java Developer Bridge — Sorted Containers

| Java | Python | Notes |
|---|---|---|
| `TreeSet<T>` | `SortedSet` (sortedcontainers) | O(log n) add/remove/contains |
| `TreeMap<K,V>` | `SortedDict` (sortedcontainers) | O(log n) put/get |
| `TreeSet.first()` | `sorted_set[0]` | O(1) min |
| `TreeSet.last()` | `sorted_set[-1]` | O(1) max |
| `TreeSet.subSet(a, b)` | `sorted_list.irange(a, b)` | O(log n + k) |
| `TreeMap.headMap(k)` | `sorted_dict.irange(None, k)` | O(log n + k) |
| `Collections.binarySearch()` | `bisect.bisect_left(sorted_list, x)` | O(log n) |

### `bisect` — Binary Search on Sorted Lists

```python
import bisect

# bisect works on any SORTED list — O(log n)
nums = [1, 2, 4, 5, 8, 10]

# bisect_left — index where x should be inserted (leftmost)
print(bisect.bisect_left(nums, 5))    # 3 — index of 5
print(bisect.bisect_left(nums, 6))    # 4 — where 6 would be inserted

# bisect_right — index after last occurrence of x
print(bisect.bisect_right(nums, 5))   # 4 — after the 5

# insort — insert in sorted order: O(log n) search + O(n) insert
bisect.insort(nums, 6)
print(nums)   # [1, 2, 4, 5, 6, 8, 10]

# Practical: find closest number
def closest(sorted_list, target):
    i = bisect.bisect_left(sorted_list, target)
    if i == 0:
        return sorted_list[0]
    if i == len(sorted_list):
        return sorted_list[-1]
    before = sorted_list[i-1]
    after = sorted_list[i]
    return before if abs(before - target) <= abs(after - target) else after
```

---

## 11. Space Complexity — Python-Specific Patterns

```python
import sys

# Comparing space costs
n = 1000

# list — O(n) — stores n pointers (8 bytes each on 64-bit)
lst = list(range(n))
print(sys.getsizeof(lst))   # ~8056 bytes = 56 header + 8*n

# generator — O(1) — just the generator object
gen = (x for x in range(n))
print(sys.getsizeof(gen))   # ~112 bytes regardless of n

# dict — O(n) — roughly 200-300 bytes per entry
d = {i: i for i in range(100)}
print(sys.getsizeof(d))    # ~4696 bytes

# set — O(n) — same as dict without values
s = {i for i in range(100)}
print(sys.getsizeof(s))    # ~4264 bytes

# Dataclass vs dict vs tuple — space comparison
from dataclasses import dataclass

@dataclass
class Point:
    x: float
    y: float

p_class = Point(1.0, 2.0)
p_dict = {"x": 1.0, "y": 2.0}
p_tuple = (1.0, 2.0)

print(sys.getsizeof(p_class))   # ~56 bytes (+ __dict__ ~232 bytes = ~288 total)
print(sys.getsizeof(p_dict))    # ~232 bytes
print(sys.getsizeof(p_tuple))   # ~56 bytes

# Use __slots__ + @dataclass(slots=True, Python 3.10+) for minimal memory
@dataclass(slots=True)
class PointSlots:
    x: float
    y: float

ps = PointSlots(1.0, 2.0)
print(sys.getsizeof(ps))   # ~56 bytes — no __dict__
```

---

## 12. Java Developer Bridge — Full Complexity Comparison

| Operation | Java `ArrayList` | Python `list` |
|---|---|---|
| Add to end | O(1) amortized | O(1) amortized |
| Add at index | O(n) | O(n) |
| Remove at index | O(n) | O(n) |
| Get/set by index | O(1) | O(1) |
| Contains | O(n) | O(n) |
| Size/len | O(1) | O(1) |
| Sort | O(n log n) Timsort | O(n log n) Timsort |

| Operation | Java `HashMap` | Python `dict` |
|---|---|---|
| Get/set/delete | O(1) avg, O(n) worst | O(1) avg, O(n) worst |
| Contains key | O(1) avg | O(1) avg |
| Iteration | O(n+capacity) | O(n) — compact format |
| Collision resolution | Separate chaining (trees since Java 8) | Open addressing |
| Load factor | 0.75 | ~0.667 |

| Operation | Java `PriorityQueue` | Python `heapq` on `list` |
|---|---|---|
| Add | O(log n) | O(log n) `heappush` |
| Remove min | O(log n) | O(log n) `heappop` |
| Peek min | O(1) `peek()` | O(1) `heap[0]` |
| Build from list | O(n) | O(n) `heapify` |
| Max-heap | Reverse comparator | Negate values |
| Interface | Class with methods | Module functions on list |

---

## 13. Hot Interview Q&A

**Q: Why is `list.append()` O(1) amortized but not strictly O(1)?**  
A: `list` is a dynamic array. When it is full, Python allocates a new, larger array and copies all elements — an O(n) operation. Because the array grows by a factor each time (~1.125×), the frequency of expensive copies decreases exponentially. Summing the total copy cost over n appends and dividing by n gives O(1) per operation on average — amortized O(1).

**Q: What is the difference between Python `dict` and Java `HashMap` internally?**  
A: Java `HashMap` uses **separate chaining** — each bucket holds a linked list (or red-black tree for long chains since Java 8). Python `dict` uses **open addressing with a compact format** — two arrays: a sparse indices array and a dense entries array. Python's format is more cache-friendly (fewer pointer dereferences) and preserves insertion order as a natural side effect.

**Q: Why is `x in my_list` O(n) but `x in my_set` O(1)?**  
A: `list` stores elements in a contiguous array with no hash structure — the only way to find an element is a linear scan. `set` uses a hash table — `hash(x)` maps directly to a slot in O(1), making membership testing O(1) average.

**Q: How does Python prevent hash collision attacks on dicts?**  
A: Since Python 3.3, hash values for `str`, `bytes`, and `datetime` objects are randomized per process using a random seed (`PYTHONHASHSEED`). This means an attacker who crafts inputs to all hash to the same slot in one process will not succeed in another process. `int` and `float` hashes are deterministic because equal numeric values must have equal hashes.

**Q: When should you use `deque` instead of `list`?**  
A: Use `deque` when you need O(1) operations on both ends — BFS queues (popleft), sliding windows (maxlen), implementing stacks and queues simultaneously. Use `list` when you need O(1) random access by index, since `deque[i]` is O(n). Never use `list.pop(0)` or `list.insert(0, x)` in a loop — these are O(n) and create O(n²) algorithms.

**Q: What is `heapq` and how is it different from Java's `PriorityQueue`?**  
A: Both implement a min-heap. The interface difference: Java's `PriorityQueue` is a class with instance methods (`offer`, `poll`, `peek`). Python's `heapq` is a module of functions that operate on a plain `list` — you pass the list explicitly. Python has no max-heap class; negate values to simulate one. Both `heapify` and `PriorityQueue` construction from a list are O(n).

**Q: What is the time complexity of `sorted()` vs `list.sort()` in Python?**  
A: Both are O(n log n) using Timsort. `sorted()` allocates a new list — O(n) extra space. `list.sort()` sorts in-place — O(log n) extra space for the merge buffers. For nearly-sorted data, Timsort degrades to O(n) — it detects natural runs.

**Q: Why is `dict` iteration O(n) in Python but O(n + capacity) in Java's `HashMap`?**  
A: Java's `HashMap` iterates over all buckets including empty ones — O(capacity). With a load factor of 0.75 and typical use, capacity can be ~4/3 × n, but in worst case it can be much larger. Python's compact dict format since 3.6 stores entries in a dense array and only iterates actual entries — O(n) regardless of unused slots.

**Q: What is amortized analysis? Give an example.**  
A: Amortized analysis distributes the cost of occasional expensive operations over many cheap ones. Example: `list.append()` is usually O(1) (just write a pointer). Rarely, it triggers a reallocation (O(n)). Over n appends, total work is O(n) — each element is copied at most O(log n) times across all reallocations. Dividing total work O(n) by n operations gives O(1) amortized cost per append.

---

## 14. Final Revision Checklist

### `list` Internals

- [ ] I can explain why `append` is O(1) amortized using the dynamic array growth argument
- [ ] I know `list.pop(0)` and `list.insert(0, x)` are O(n) — never use in a loop
- [ ] I know Python's sort is Timsort — stable, O(n log n) worst, O(n) best

### `dict` Internals

- [ ] I know Python `dict` uses open addressing, Java `HashMap` uses separate chaining
- [ ] I know `dict` is ordered since Python 3.7 — insertion order preserved by design
- [ ] I know `hash(1) == hash(1.0)` — equal values must have equal hashes
- [ ] I know hash randomization (PYTHONHASHSEED) prevents collision attacks on string keys

### `set` and Membership

- [ ] I know `x in set` is O(1); `x in list` is O(n) — always convert to set for repeated lookups
- [ ] I know set intersection is O(min(n,m)), not O(n*m)

### `deque`

- [ ] I know deque gives O(1) on both ends; list gives O(n) on the left end
- [ ] I know `deque[i]` is O(n) — deque does NOT support O(1) random access

### `heapq`

- [ ] I know `heapq` is a min-heap; negate values for max-heap
- [ ] I know `heapify` is O(n), not O(n log n)
- [ ] I know to use tuples `(priority, value)` for ordered heap with custom objects
- [ ] I know `heap[0]` peeks the minimum in O(1) without removing it

### Space Complexity

- [ ] I know a generator expression uses O(1) space; list comprehension uses O(n)
- [ ] I know `__slots__` eliminates `__dict__` overhead for instances

### Java Developer Reminders

- [ ] Python `dict` is open addressing; Java `HashMap` is separate chaining — different collision behavior
- [ ] Python has no built-in `TreeSet`/`TreeMap` — use `sortedcontainers` (third party)
- [ ] `bisect` module provides binary search on sorted lists — O(log n)
- [ ] `heapq` is a module of functions, not a class — you pass the list explicitly

---

*File 1 of 5 — Group 2: Intermediate Backend*  
*Next: Python-Type-Hints-Pydantic-Validation-Gold-Sheet.md*
