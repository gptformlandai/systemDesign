# Python Internals, Memory, GC & GIL - MAANG Master Sheet

> **Track File #13 of 31 - Group 3: Senior MAANG**
> For: Java developer | Level: senior Python internals | Mode: interview explanation + production debugging

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Developers |
|---|---|---|
| CPython execution pipeline | Very high | Java has JVM/JIT; CPython has interpreter + bytecode without standard JIT |
| Object model: names point to objects | Very high | Java variables feel like typed containers; Python names are bindings |
| Reference counting | Very high | Java GC is tracing-first; CPython frees most objects immediately |
| Cyclic garbage collector | High | Python has refcount + cycle detector, not JVM-style heap tracing only |
| GIL mechanics | Very high | No Java equivalent; affects CPU-bound threading answers |
| Object size and `sys.getsizeof` | Medium | Python object overhead is larger than Java developers expect |
| `__slots__` | Medium | Useful for memory-heavy domain models; unlike Java field declarations |
| `weakref` | Medium | Important for caches, observer lists, parent-child cycles |
| Bytecode and `dis` | Medium | Explains non-atomic operations like `counter += 1` |
| Import cache and module singletons | High | Python module import side effects differ from Java class loading |

**MAANG signal:** You do not need to recite CPython source code. You do need to explain symptoms: memory grows, threads do not speed CPU work, object identity surprises, circular imports, and why a Python service behaves differently from a Java service.

---

## 2. CPython Execution Pipeline

### Must Know

Most production Python code runs on **CPython**, the reference implementation written in C.

High-level flow:

```text
source.py
  -> parser builds AST
  -> compiler turns AST into bytecode
  -> bytecode stored in code objects
  -> CPython virtual machine interprets bytecode
  -> .pyc cache may be written under __pycache__
```

When you run:

```bash
python app.py
```

CPython does not execute raw text line by line. It parses and compiles the module to bytecode first, then interprets the bytecode.

### `.pyc` Files

Python may write cached bytecode files:

```text
__pycache__/
    app.cpython-312.pyc
```

The cache avoids recompiling unchanged modules. It does **not** make Python equivalent to Java bytecode running on a JIT-optimizing JVM.

### Java Developer Bridge

| Java | Python |
|---|---|
| `.java` -> `.class` bytecode -> JVM | `.py` -> code object bytecode -> CPython VM |
| JVM usually has JIT optimization | CPython standard runtime has no JIT |
| Static type checking at compile time | Runtime type checking unless tools like mypy/pyright are used |
| Class loading is explicit JVM machinery | Import system executes module top-level code once and caches module object |

### Interview Answer

> CPython parses Python source into an AST, compiles it to bytecode, then interprets that bytecode in the CPython VM. `.pyc` files cache bytecode compilation, not machine-code optimization. Unlike Java's HotSpot JVM, standard CPython does not use a JIT, so hot loops do not automatically become optimized native code.

---

## 3. Names, Objects, and References

### Foundational Rule

Python variables are **names bound to objects**.

```python
x = [1, 2, 3]
y = x
x.append(4)
print(y)  # [1, 2, 3, 4]
```

`x` and `y` are two names for the same list object.

### Object Identity

```python
x = [1, 2]
y = x
z = [1, 2]

print(x is y)  # True
print(x is z)  # False
print(x == z)  # True
```

- `is` checks identity: same object.
- `==` checks equality: same value according to `__eq__`.

### Function Arguments

Python uses **call by object reference**.

```python
def add_item(items: list[str]) -> None:
    items.append("new")

values = ["old"]
add_item(values)
print(values)  # ['old', 'new']
```

The function receives a reference to the same list object. Mutating the object is visible to the caller.

```python
def reassign(items: list[str]) -> None:
    items = ["new"]  # local name rebinding only

values = ["old"]
reassign(values)
print(values)  # ['old']
```

Rebinding the local name does not affect the caller's name.

### Java Developer Bridge

Java passes object references by value. Python behavior is close to that, but Python names are more fluid because names have no declared type. The object has a type; the name does not.

---

## 4. CPython Object Layout

### Must Know

Every CPython object has metadata.

Conceptually:

```text
PyObject header:
  refcount
  type pointer
  object-specific data
```

For variable-sized objects like list, tuple, dict, and str, CPython uses a larger structure that also tracks size.

### Why `sys.getsizeof` Surprises People

```python
import sys

print(sys.getsizeof(0))        # usually 28 bytes on 64-bit CPython
print(sys.getsizeof([]))       # usually 56 bytes before elements
print(sys.getsizeof([1, 2, 3]))
```

A Python `int` is not a raw 4-byte primitive. It is a full object with metadata.

### Deep Size Trap

`sys.getsizeof(container)` returns the shallow size of the container itself, not the full recursive size of all referenced objects.

```python
import sys

items = [[1] * 1000 for _ in range(1000)]
print(sys.getsizeof(items))  # size of outer list only
```

The inner lists are separate objects.

### Strong Answer

> `sys.getsizeof` is shallow. It tells me the memory footprint of the object container, not the full object graph. For nested data, I need recursive sizing tools or `tracemalloc` snapshots to understand where memory is allocated.

---

## 5. Reference Counting

### Must Know

CPython primarily manages memory using **reference counting**.

Every object tracks how many references point to it. When the count reaches zero, CPython can free the object immediately.

```python
import sys

x = []
print(sys.getrefcount(x))  # includes temporary reference from getrefcount call

y = x
print(sys.getrefcount(x))

del y
print(sys.getrefcount(x))
```

### Immediate Cleanup

```python
class Resource:
    def __del__(self):
        print("freed")

obj = Resource()
del obj
# In CPython, usually prints "freed" immediately.
```

Do not rely on this behavior for resource management across all Python implementations. Use context managers.

### Production Implication

Reference counting means many objects disappear quickly, but it does not solve reference cycles.

```python
class Node:
    def __init__(self, name: str):
        self.name = name
        self.other = None

first = Node("first")
second = Node("second")
first.other = second
second.other = first

del first
del second
# Reference counts do not reach zero because the objects reference each other.
```

The cyclic GC handles this later.

### Java Developer Bridge

| Java GC | CPython Memory Management |
|---|---|
| Tracing GC is primary | Reference counting is primary |
| Objects usually collected later | Many objects freed immediately at refcount zero |
| Cycles are normal for tracing GC | Cycles require CPython's cyclic GC |
| Finalizers are discouraged | `__del__` is also dangerous; use context managers |

---

## 6. Cyclic Garbage Collector

### Must Know

CPython has a cyclic GC for containers that can participate in reference cycles.

It tracks objects such as:

- `list`
- `dict`
- `set`
- user-defined class instances
- closures that hold references

It usually does not need to track simple immutable objects like small integers or strings.

### Inspecting GC

```python
import gc

print(gc.isenabled())
print(gc.get_threshold())
print(gc.get_count())

collected = gc.collect()
print(f"Collected {collected} unreachable objects")
```

### Reference Cycle Example

```python
import gc

class Node:
    def __init__(self, name: str):
        self.name = name
        self.child = None
        self.parent = None

parent = Node("parent")
child = Node("child")
parent.child = child
child.parent = parent

del parent
del child

print(gc.collect())  # cycle can now be collected
```

### `__del__` Trap

Objects with finalizers need careful handling. Modern Python can collect many cycles with `__del__`, but finalization order can still be subtle. Avoid `__del__` for business resources.

Use:

```python
with open("data.txt") as file:
    data = file.read()
```

or:

```python
from contextlib import contextmanager

@contextmanager
def managed_resource():
    resource = acquire()
    try:
        yield resource
    finally:
        release(resource)
```

### Interview Answer

> CPython frees most objects with reference counting. When objects reference each other in a cycle, their reference counts never reach zero. The cyclic GC periodically detects unreachable cycles and collects them. For deterministic cleanup, I should use `with` and context managers rather than relying on `__del__`.

---

## 7. `weakref` - Breaking Cycles and Building Caches

### Must Know

A weak reference points to an object without increasing its reference count.

```python
import weakref

class User:
    pass

user = User()
ref = weakref.ref(user)

print(ref() is user)  # True

del user
print(ref())          # None
```

### Parent-Child Cycle Fix

```python
import weakref

class Parent:
    def __init__(self):
        self.children = []

class Child:
    def __init__(self, parent: Parent):
        self.parent_ref = weakref.ref(parent)

    @property
    def parent(self) -> Parent | None:
        return self.parent_ref()

parent = Parent()
child = Child(parent)
parent.children.append(child)
```

The parent strongly owns children. The child weakly references the parent.

### Weak Caches

```python
import weakref

class UserProfile:
    pass

cache = weakref.WeakValueDictionary()
profile = UserProfile()
cache["user-1"] = profile

print(cache.get("user-1") is profile)  # True

del profile
print(cache.get("user-1"))             # None after object is gone
```

Weak caches avoid keeping objects alive just because they are cached.

### Java Developer Bridge

Java has `WeakReference`, `SoftReference`, and `WeakHashMap`. Python's `weakref.ref`, `WeakValueDictionary`, and `WeakKeyDictionary` fill similar roles. The idea is the same: do not let a cache or listener list become the owner of an object by accident.

---

## 8. Python Memory Allocators and Fragmentation

### Must Know

CPython has layers of memory allocation:

```text
Python object allocator
  -> pymalloc for small objects
  -> system malloc for larger blocks
  -> OS memory pages
```

For many small Python objects, CPython uses arenas, pools, and blocks. This is fast, but it means process RSS may not drop immediately after objects are freed.

### Why RSS Does Not Fall

A Python process can free objects internally but keep memory arenas reserved for future Python allocations.

Symptom:

```text
tracemalloc says Python allocations dropped
OS RSS still high
```

This does not always mean a leak. It can be allocator reuse or fragmentation.

### Production Explanation

> If `tracemalloc` shows allocations are stable but process RSS keeps increasing, I would check native extensions, large buffers, memory fragmentation, and off-heap allocations. `tracemalloc` tracks Python allocations, not all native memory.

---

## 9. The GIL - Global Interpreter Lock

### Must Know

The GIL is a mutex in CPython that allows only one thread to execute Python bytecode at a time.

```text
Thread A executing Python bytecode -> holds GIL
Thread B wants to execute bytecode -> waits for GIL
```

### What the GIL Protects

The GIL simplifies memory management by protecting CPython interpreter state, including reference count updates.

Without a global lock, every reference count increment/decrement would need fine-grained synchronization.

### What the GIL Does Not Mean

The GIL does **not** mean Python has no concurrency.

Threading helps when threads spend time waiting on I/O:

- network calls
- file I/O
- database calls
- sleeps
- C extensions that release the GIL

Threading does not speed up pure Python CPU-bound loops.

### CPU-Bound Example

```python
import threading
import time

COUNT = 30_000_000

def work() -> None:
    total = 0
    for i in range(COUNT):
        total += i

start = time.perf_counter()
threads = [threading.Thread(target=work) for _ in range(2)]
for thread in threads:
    thread.start()
for thread in threads:
    thread.join()
print(time.perf_counter() - start)
```

Two threads will usually not be close to 2x faster for pure Python CPU work.

### Interview Answer

> The GIL is a CPython mutex that allows only one thread to execute Python bytecode at a time. It makes reference counting and interpreter internals simpler, but it prevents true parallel execution of CPU-bound Python code in threads. Threads still help for I/O because the GIL is released while waiting on many blocking system calls. For CPU parallelism, use multiprocessing, native extensions that release the GIL, or another runtime strategy.

---

## 10. When the GIL Is Released

### Common Cases

The GIL can be released during:

- blocking file I/O
- socket operations
- many database driver waits
- `time.sleep`
- compression / hashing / numeric C extensions that explicitly release it
- NumPy operations implemented in C

### Why NumPy Can Be Fast

```python
import numpy as np

arr = np.arange(10_000_000)
result = arr * 2
```

The heavy loop runs in optimized C, not Python bytecode. Many native operations can release the GIL while doing CPU work.

### Trap

```python
# Slow Python loop: holds GIL most of the time
result = [x * 2 for x in range(10_000_000)]
```

This loops in Python bytecode.

### Senior Answer

> The GIL limits execution of Python bytecode, not all native CPU instructions. A C extension can release the GIL while doing long-running work. That is why NumPy, compression libraries, and some crypto/hash operations can use CPU efficiently despite CPython's GIL.

---

## 11. Bytecode and `dis`

### Must Know

The `dis` module shows CPython bytecode.

```python
import dis

counter = 0

def increment() -> None:
    global counter
    counter += 1

dis.dis(increment)
```

You will see multiple bytecode operations: load value, add, store value. This is why `counter += 1` is not a safe compound operation across threads.

### Example Output Shape

```text
LOAD_GLOBAL counter
LOAD_CONST 1
BINARY_OP +=
STORE_GLOBAL counter
```

Actual bytecode names vary by Python version.

### Race Condition Explanation

Thread A and Thread B can interleave between bytecode operations:

```text
A loads counter = 10
B loads counter = 10
A stores 11
B stores 11
```

Expected two increments, got one.

### Fix

```python
import threading

counter = 0
lock = threading.Lock()

def increment() -> None:
    global counter
    with lock:
        counter += 1
```

### Java Developer Bridge

Java developers already know `i++` is not atomic. Python's `counter += 1` has the same conceptual issue, despite the GIL. The GIL does not make multi-step business operations atomic.

---

## 12. `__slots__` and Memory-Friendly Models

### Must Know

By default, most Python objects store instance attributes in a per-object `__dict__`.

```python
class User:
    def __init__(self, user_id: str, email: str):
        self.user_id = user_id
        self.email = email
```

This is flexible but has memory overhead.

`__slots__` removes the per-instance `__dict__` and restricts attributes to a fixed set.

```python
class User:
    __slots__ = ("user_id", "email")

    def __init__(self, user_id: str, email: str):
        self.user_id = user_id
        self.email = email
```

### Dataclass Slots

```python
from dataclasses import dataclass

@dataclass(slots=True)
class User:
    user_id: str
    email: str
```

### When to Use

Use `slots=True` when:

- you create millions of small objects
- attribute set is stable
- memory footprint matters
- you want to prevent accidental attributes

Avoid it when:

- you need dynamic attributes
- frameworks expect `__dict__`
- you use multiple inheritance heavily
- you need weak references but forgot `weakref_slot=True` in dataclasses

### Interview Answer

> `__slots__` trades flexibility for memory. It removes each instance's dynamic `__dict__` and stores attributes in a fixed layout. It is useful for millions of small objects or hot data models, but it can surprise frameworks that expect dynamic attributes.

---

## 13. Import System and Module Singletons

### Must Know

Importing a Python module executes its top-level code once and stores the module object in `sys.modules`.

```python
# settings.py
print("loading settings")
VALUE = 42
```

```python
# app.py
import settings
import settings
# "loading settings" prints once
```

### `sys.modules`

```python
import sys
import settings

print(sys.modules["settings"] is settings)  # True
```

### Singleton Trap

A module-level object is effectively a process-local singleton.

```python
# cache.py
cache = {}
```

Every import of `cache` receives the same module object and therefore the same dictionary. This can be intentional, but in web services it can create shared mutable state bugs.

### Circular Import Trap

```python
# service.py
from repository import UserRepository

# repository.py
from service import UserService
```

During import, one module may see the other only partially initialized.

### Fixes

1. Move shared types to a lower-level module.
2. Use `from __future__ import annotations` and `TYPE_CHECKING` for type-only imports.
3. Move local imports inside functions only when restructuring is not worth it.
4. Keep dependency direction one-way: API -> service -> repository -> infrastructure.

---

## 14. Enterprise Debugging Implications

### Memory Leak Investigation

Senior sequence:

```text
1. Confirm: process RSS is increasing over time.
2. Separate Python allocations from native memory.
3. Start tracemalloc early.
4. Take snapshot at baseline and after growth.
5. Compare snapshots by lineno and traceback.
6. Inspect object types with objgraph if needed.
7. Find ownership path: cache, task list, closure, listener, global module state.
8. Fix ownership, add eviction, or use weak references.
```

### GIL Investigation

Symptoms:

- CPU at 100 percent on one core
- threads added but throughput does not improve
- p99 latency spikes under CPU-heavy endpoint

Likely causes:

- pure Python loop in request path
- JSON serialization of huge payloads
- CPU-heavy validation or transformation
- compression/hash work not offloaded

Fixes:

- optimize algorithm
- cache result
- move CPU work to `ProcessPoolExecutor`
- use native/vectorized libraries
- move work out of request path

### Import Investigation

Symptoms:

- `ImportError: cannot import name X from partially initialized module`
- app starts locally but fails in production startup order
- circular dependency between service and repository modules

Fix:

- enforce layered imports
- avoid top-level side effects
- keep dependency construction in a composition root

---

## 15. Java Developer Bridge - Internals Summary

| Concern | Java Mental Model | Python / CPython Mental Model |
|---|---|---|
| Runtime | JVM with JIT | CPython interpreter, bytecode, no standard JIT |
| Variables | Typed local slots / fields | Names bound to objects |
| Primitives | `int`, `long`, `boolean` primitives | Everything is an object |
| Memory management | Tracing GC | Reference counting + cyclic GC |
| Finalization | `finalize` deprecated, try-with-resources | `__del__` discouraged, use context managers |
| Threading | True CPU parallelism | GIL blocks parallel Python bytecode in threads |
| CPU scaling | More threads can use more cores | Use multiprocessing or native extensions |
| Object fields | Declared fields | Dynamic `__dict__` unless `__slots__` |
| Class loading | JVM class loader | Import executes modules and caches in `sys.modules` |
| Weak references | `WeakReference`, `WeakHashMap` | `weakref.ref`, `WeakValueDictionary` |

### Interview Trap for Java Developers

Do not say: "Python has garbage collection like Java, so memory works the same."

Say:

> CPython combines reference counting with a cyclic garbage collector. Many objects are freed immediately when their reference count reaches zero, but cycles require the cyclic GC. This gives Python different memory behavior from Java, especially around object lifetimes, finalizers, and RSS not dropping after allocation bursts.

---

## 16. Hot Interview Q&A

**Q1: What happens when you run a Python file?**
> CPython parses source into an AST, compiles it to bytecode, creates code objects, and interprets that bytecode in the VM. It may cache bytecode in `__pycache__`, but that cache is not a JIT-optimized native binary.

**Q2: How does CPython free memory?**
> Primarily with reference counting. Every object tracks references. When the count hits zero, CPython can immediately deallocate it. Cycles are handled by the cyclic garbage collector.

**Q3: Why can reference cycles leak memory?**
> In a cycle, objects keep each other's reference counts above zero. CPython's cyclic GC usually collects unreachable cycles, but cycles involving finalizers, external resources, caches, or global references can still cause retained memory. The design fix is usually ownership cleanup or weak references.

**Q4: Does the GIL make Python thread-safe?**
> No. The GIL protects CPython interpreter internals, not business invariants. Compound operations like check-then-update are still race-prone. Use locks, queues, or higher-level synchronization.

**Q5: Why does threading help I/O but not CPU work?**
> I/O waits release the GIL so another thread can run while one thread waits. Pure Python CPU loops need the GIL to execute bytecode, so threads take turns instead of running Python bytecode in parallel.

**Q6: What does `sys.getsizeof` measure?**
> The shallow size of one object, not the recursive size of everything it references. For nested structures, use `tracemalloc` or recursive sizing tools.

**Q7: When would you use `__slots__`?**
> For many small objects with stable attributes where memory footprint matters. It removes per-instance `__dict__`, but reduces flexibility and can affect framework compatibility.

**Q8: How do circular imports happen?**
> Python executes modules top-down during import. If module A imports B and B imports A before A finishes initializing, one side sees a partially initialized module. Fix with dependency direction, lower-level shared modules, or type-only imports.

---

## 17. Final Revision Checklist

- [ ] Can explain `.py` -> AST -> bytecode -> CPython VM
- [ ] Can explain `.pyc` cache without calling it a JIT
- [ ] Can explain names vs objects vs references
- [ ] Can explain CPython object metadata and why Python objects are large
- [ ] Can explain shallow `sys.getsizeof` vs recursive memory size
- [ ] Can explain reference counting and when objects are freed
- [ ] Can explain why cycles need cyclic GC
- [ ] Can use `gc.collect()`, `gc.get_count()`, and `tracemalloc` conceptually
- [ ] Can explain weak references and where they prevent leaks
- [ ] Can explain the GIL and its impact on CPU-bound threading
- [ ] Can explain when the GIL is released
- [ ] Can use `dis` to show why `counter += 1` is not atomic
- [ ] Can explain `__slots__` and dataclass `slots=True`
- [ ] Can explain module import caching and circular import failure
- [ ] Can bridge every internals topic back to Java accurately
