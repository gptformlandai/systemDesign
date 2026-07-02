# Python For Java Developers Gold Sheet

Target: 6-year Java developer who knows Python basics but wants to think in Python, not translated Java.

Goal of this sheet:
- Map every major Java concept to its Python equivalent.
- Highlight where Python behaves differently from what Java experience predicts.
- Call out what Java has that Python does not, and what Python has that Java does not.
- Surface the 20 highest-frequency interview traps for Java developers answering Python questions.

How to use this:
- Read Java column first — you already know it.
- Then read the Python column for the shift.
- Then read the trap — this is where you earn or lose points in interviews.
- Revisit this sheet at the start of every study session until the bridges feel automatic.

---

## 1. Interview Priority Meter

| Area | Priority | Why Java Developers Struggle Here |
|---|---:|---|
| Name binding vs typed variables | Very high | Java devs assume variables have types |
| `is` vs `==` semantics | Very high | Opposite of Java for objects |
| Mutable default arguments | Very high | No Java equivalent; very common Python trap |
| No `new` keyword | High | Easy to fix, but instinctive mistake |
| `self` must be declared | High | Forgetting self causes TypeError |
| No access modifiers | High | Java devs look for public/private |
| No method overloading | High | Java devs write two methods with same name |
| No checked exceptions | High | Java devs over-declare what should be raised |
| GIL and threading | Very high | Java threading intuition is wrong here |
| `None` vs `null` | High | Similar concept, different behavior |
| Duck typing | High | Java devs look for interfaces |
| Comprehensions over streams | High | Java stream mental model does not translate directly |
| `__eq__`/`__hash__` | High | Same contract as Java, different syntax |
| Multiple inheritance / MRO | Medium-high | Java devs default to single inheritance |
| Packaging and imports | High | Very different from Java classpath/Maven |

---

## 2. Execution Model

| Java | Python |
|---|---|
| Write `.java` → compile with `javac` → `.class` bytecode → JVM executes | Write `.py` → CPython compiles at import time → `.pyc` bytecode → PVM executes |
| JIT compiler optimizes hot methods into native code | Default CPython has no HotSpot-style always-on JIT assumption; PyPy has a JIT; Python 3.13+ has experimental CPython JIT work |
| JDK + JRE + JVM are distinct components | Python runtime is just CPython (one binary: `python3`) |
| `java MyClass` to run | `python3 my_script.py` to run |
| REPL: `jshell` | REPL: `python3` or `ipython` |
| Compilation is a separate explicit step | Compilation is implicit and automatic at import |

### What Does Not Exist In Python

- No `javac` separate compile step in normal workflow.
- No JVM-style always-on JIT assumption in default CPython. PyPy has one, and modern CPython has experimental JIT work, but that is version/build-specific.
- No JVM tiered compilation, JIT warmup, or method deoptimization.

### Interview Trap

```text
Java developer says: "Python is compiled to bytecode just like Java so performance is similar."

Correct answer: Python bytecode on default CPython should not be treated like HotSpot JIT
bytecode. Java bytecode is JIT-compiled into native machine code for hot paths, giving Java
strong throughput for CPU-bound work. Modern Python has PyPy and experimental CPython JIT
caveats, but normal backend Python performance comes from architecture, native/vectorized
libraries, async IO, caching, and clear workload separation.
```

---

## 3. Variables And Types

### Side-By-Side

| Concept | Java | Python |
|---|---|---|
| Variable declaration | `int count = 5;` | `count = 5` |
| Type lives on | The variable declaration | The object, not the name |
| Reassign to different type | Compile error | Fully valid |
| Primitive types | `int`, `double`, `boolean`, `char`, `long` | None — everything is an object |
| Null/empty reference | `null` | `None` |
| Type check | `instanceof` | `isinstance(x, SomeType)` |
| Type cast | `(String) obj` | `str(obj)` for conversion; no unsafe cast |
| Type hints | Generics: `List<String>` | `list[str]` (3.9+) or `List[str]` (typing) |
| Static typing enforced | Yes, at compile time | No, type hints are optional and not enforced at runtime |
| `var` (Java 10+) | `var x = 5;` — inferred static type | All Python assignment is implicitly untyped |

### Code Comparison

Java:
```java
int count = 0;
String name = "Alice";
count = count + 1;
// count = "hello";  // compile error
```

Python:
```python
count = 0
name = "Alice"
count = count + 1
count = "hello"   # perfectly valid — name rebound to str object
```

### What Does Not Exist In Python

- Primitive types (`int`, `double`, `boolean` as non-objects).
- Typed variable declarations.
- Compile-time type enforcement.
- `final` keyword on local variables or fields.
- `var` in the Java 10+ sense (Python never had static inference on names).

### What Python Has That Java Does Not

- Dynamic binding — names can point to any type at any time.
- Duck typing — the type of an object matters less than whether it has the required method.
- Type hints that are documentation and static-analysis aids but not runtime constraints.

### Interview Trap

```text
Java developer says: "Python int is like Java Integer, a boxed type."

Correct answer: Python int is an object, but there is no boxed/unboxed distinction at all.
Python has no primitives. Every int, float, bool, and str is an object. CPython does cache
small integers (-5 to 256) for performance, but this is an implementation detail, not
a language primitive model.
```

---

## 4. Strings

### Side-By-Side

| Concept | Java | Python |
|---|---|---|
| Immutable | Yes | Yes |
| Pool / interning | String literal pool, `intern()` | Short strings interned automatically; `sys.intern()` for explicit |
| Value comparison | `.equals()` | `==` (Python `==` is value comparison) |
| Reference comparison | `==` | `is` (rarely used; never rely on for strings) |
| Multiline strings | `"""` with `+` or text blocks (Java 13+) | Triple quotes `"""..."""` always available |
| String formatting | `String.format()`, `%s`, `StringBuilder` | f-strings (`f"Hello {name}"`), `.format()`, `%` |
| `null` safe | NPE risk if null | `None` raises `AttributeError` on method calls |
| Concatenation in loops | Use `StringBuilder` | Use `"".join(list)` — never `+` in a loop |
| Length | `.length()` | `len(s)` |
| Substring | `.substring(start, end)` | `s[start:end]` (slice) |
| Upper/lower | `.toUpperCase()` | `.upper()` |
| Split | `.split(regex)` | `.split(sep)` — not regex by default; use `re.split` for regex |
| Contains | `.contains(sub)` | `sub in s` |
| Strip whitespace | `.strip()` | `.strip()` |
| Starts/ends with | `.startsWith()` / `.endsWith()` | `.startswith()` / `.endswith()` |

### F-Strings (Python Preferred)

```python
name = "Aravind"
level = "senior"
message = f"Hello {name}, you are a {level} engineer"

# With expressions
items = [1, 2, 3]
summary = f"Count: {len(items)}, sum: {sum(items)}"
```

### Joining Strings (The Right Way)

```python
# Java developer instinct — do NOT do this in Python
result = ""
for word in words:
    result += word  # creates a new string object each time

# Pythonic — use join
result = " ".join(words)  # single allocation
```

### What Does Not Exist In Python

- String literal pool in the Java sense with `intern()` as the primary tool.
- `StringBuilder` / `StringBuffer` — Python's `"".join()` serves the same role.
- Regex-by-default in `.split()`.

### Interview Trap

```text
Java developer says: "Use == to compare strings in Python just like equals() in Java."

Actually backwards: In Java, == on strings is reference comparison.
In Python, == on strings is value comparison.
Python == is like Java .equals(). Python is is like Java ==.
The correct Python rule: always use == for string value comparison.
Never use is for string comparison (even when CPython interning makes it work — it is
an implementation detail you cannot rely on).
```

---

## 5. Collections: The Full Mapping

### Core Collections Map

| Java | Python | Key Differences |
|---|---|---|
| `ArrayList<T>` | `list` | Python list is dynamically typed; no generics at runtime |
| `LinkedList<T>` | `collections.deque` | Python list has O(1) append; deque for O(1) both ends |
| `HashMap<K,V>` | `dict` | Python dict preserves insertion order (3.7+); Java HashMap does not |
| `LinkedHashMap<K,V>` | `dict` (already ordered) | dict is ordered by default in Python 3.7+ |
| `TreeMap<K,V>` | `dict` with `sorted()` or `SortedDict` | No built-in sorted map; use `sortedcontainers.SortedDict` |
| `HashSet<T>` | `set` | Very similar; Python set uses `in`, not `.contains()` |
| `LinkedHashSet<T>` | No direct equivalent | Use `dict.fromkeys()` to preserve insertion order uniquely |
| `TreeSet<T>` | `sortedcontainers.SortedSet` | Third-party; not in stdlib |
| `PriorityQueue<T>` | `heapq` module | Python heapq is a min-heap on a list; no built-in max-heap |
| `ArrayDeque<T>` | `collections.deque` | Equivalent; O(1) append/appendleft, pop/popleft |
| `Stack<T>` | `list` with `.append()`/`.pop()` | Python list works as stack natively |
| `Queue<T>` | `collections.deque` or `queue.Queue` | `queue.Queue` is thread-safe |
| `ConcurrentHashMap` | No direct equivalent | Use `threading.Lock` + `dict`, or `queue.Queue` for producer-consumer |
| `Collections.unmodifiableList()` | `tuple` or `types.MappingProxyType` | `tuple` for immutable sequence; no built-in frozen list |
| `List.of(1,2,3)` | `[1, 2, 3]` or `(1, 2, 3)` | List literal or tuple |
| `Map.of("k","v")` | `{"k": "v"}` | Dict literal |
| `Set.of(1,2,3)` | `{1, 2, 3}` | Set literal |

### Key Operations Mapping

| Operation | Java | Python |
|---|---|---|
| Add element | `.add()` / `.put()` | `list.append()` / `dict[k] = v` / `set.add()` |
| Remove element | `.remove()` | `list.remove(v)` / `del dict[k]` / `set.discard(v)` |
| Check presence | `.contains()` | `value in collection` |
| Size | `.size()` | `len(collection)` |
| Iterate | for-each | `for item in collection:` |
| Sort in place | `Collections.sort()` | `list.sort()` |
| Get sorted copy | `new ArrayList(sorted)` | `sorted(iterable)` |
| First/last (deque) | `.peekFirst()` / `.peekLast()` | `deque[0]` / `deque[-1]` |
| Empty check | `.isEmpty()` | `if not collection:` or `if len(collection) == 0:` |
| Copy | `new ArrayList(existing)` | `list.copy()` or `list[:]` |

### dict Specifics

```python
# Creation
ages = {"Alice": 30, "Bob": 25}

# Access — KeyError if missing
print(ages["Alice"])

# Safe access
print(ages.get("Charlie"))          # None
print(ages.get("Charlie", 0))       # 0

# Check presence
print("Alice" in ages)              # True

# Iterate
for key in ages:                    # iterates keys
    print(key, ages[key])

for key, value in ages.items():     # iterates key-value pairs
    print(key, value)

# defaultdict — no KeyError on missing key
from collections import defaultdict
counts = defaultdict(int)
counts["a"] += 1                    # no KeyError; int() = 0 default

# Counter — frequency counting
from collections import Counter
freq = Counter(["a", "b", "a", "c", "a"])
print(freq)  # Counter({'a': 3, 'b': 1, 'c': 1})
```

### What Does Not Exist In Python

- `ConcurrentHashMap` — no lock-free segment-based map in stdlib.
- `TreeMap` with guaranteed sorted order in stdlib — use `sortedcontainers`.
- Fail-fast iterators — Python raises `RuntimeError: dictionary changed size during iteration`.
- Typed collections at runtime — `list` holds any mix of types.

### Interview Trap

```text
Java developer says: "Python dict is like HashMap, unordered."

Correct answer since Python 3.7: Python dict preserves insertion order as a language
guarantee (not just CPython implementation detail). Java HashMap does not preserve order;
LinkedHashMap does. So Python dict is actually closer to Java LinkedHashMap by default.

Also: Python dict does not allow null keys (None as key is allowed — None is a valid
hashable object). Java HashMap allows null keys.
```

---

## 6. OOP: Classes And Interfaces

### Class Design Comparison

| Concept | Java | Python |
|---|---|---|
| Class definition | `public class User {}` | `class User:` |
| Constructor | `public User(String id) {}` | `def __init__(self, id: str) -> None:` |
| Instance creation | `User user = new User("u1");` | `user = User("u1")` |
| `this` reference | `this` | `self` (must declare in every method) |
| Private field | `private String name;` | `self._name` (convention only, not enforced) |
| Strong private | No true private in Java either | `self.__name` (name mangling to `_ClassName__name`) |
| Static method | `static void method()` | `@staticmethod def method():` |
| Class method (factory) | Static factory methods | `@classmethod def method(cls):` |
| Abstract class | `abstract class Animal` | `from abc import ABC, abstractmethod; class Animal(ABC):` |
| Interface | `interface Printable` | `Protocol` (structural) or `ABC` (nominal) |
| Multiple inheritance | Not for classes; interfaces only | Full multiple inheritance |
| `instanceof` | `obj instanceof MyClass` | `isinstance(obj, MyClass)` |
| Override annotation | `@Override` | No annotation; just redefine the method |
| Final class | `final class Immutable` | No direct equivalent; convention or `__init_subclass__` trick |
| Enum | `enum Status { ACTIVE, INACTIVE }` | `from enum import Enum; class Status(Enum):` |

### Interface Equivalents

Python has two approaches to interface-like behavior:

**ABCs (nominal typing — like Java interfaces):**

```python
from abc import ABC, abstractmethod

class Repository(ABC):
    @abstractmethod
    def find_by_id(self, id: str) -> object:
        ...

    @abstractmethod
    def save(self, entity: object) -> None:
        ...

class InMemoryRepository(Repository):
    def find_by_id(self, id: str) -> object:
        return self._store.get(id)

    def save(self, entity: object) -> None:
        self._store[id(entity)] = entity
```

**Protocols (structural typing — duck typing with static analysis):**

```python
from typing import Protocol

class Printable(Protocol):
    def print_report(self) -> str:
        ...

def render(item: Printable) -> None:
    print(item.print_report())

# Any class with print_report() satisfies Printable
# No explicit inheritance required
class Invoice:
    def print_report(self) -> str:
        return "Invoice report"

render(Invoice())   # works — Invoice satisfies Printable structurally
```

### Access Convention

| Java | Python |
|---|---|
| `public` | No prefix (default, everything is public) |
| `protected` | `_prefix` (convention: internal use) |
| `private` | `__prefix` (name mangling: `_ClassName__field`) |
| No keyword | No enforcement — all convention |

### `@classmethod` And `@staticmethod`

```python
class User:
    user_count = 0

    def __init__(self, name: str) -> None:
        self.name = name
        User.user_count += 1

    @classmethod
    def from_dict(cls, data: dict) -> "User":
        """Factory method — cls is the class itself"""
        return cls(data["name"])

    @staticmethod
    def validate_name(name: str) -> bool:
        """Utility — no access to instance or class"""
        return len(name) > 0

user = User.from_dict({"name": "Aravind"})
print(User.validate_name("Alice"))
```

### What Does Not Exist In Python

- `public`, `private`, `protected` keywords enforced by compiler.
- `@Override` annotation for compile-time override verification.
- Method overloading by parameter types (same name, different signatures).
- `final` class or method keywords.
- `abstract` keyword — use `ABC` instead.
- `new` keyword.
- Checked exceptions on method signatures.

### What Python Has That Java Does Not

- Multiple inheritance for classes.
- `@classmethod` (receives the class as first argument).
- Dunder/magic methods that hook into language operators (`__add__`, `__len__`, etc.).
- Dynamic attribute creation: `obj.new_attr = "value"` at any time.
- `__slots__` for memory-efficient classes without `__dict__`.
- Metaclasses to customize class creation itself.

### Interview Trap

```text
Java developer writes:
  class Animal:
      def speak():          # Missing self
          return "..."

This raises TypeError: speak() takes 0 positional arguments but 1 was given.
Python does not auto-inject self. It must be explicitly declared as the first parameter
of every instance method.
```

---

## 7. Functional Programming: Streams vs Python

### Core Concept Mapping

| Java Streams | Python Equivalent | Key Difference |
|---|---|---|
| `Collection.stream()` | `iter(collection)` or just the collection | Python iterables are already lazy-capable |
| `.filter(predicate)` | `filter(func, iterable)` or list comprehension | Comprehension is more idiomatic |
| `.map(function)` | `map(func, iterable)` or list comprehension | Comprehension is more idiomatic |
| `.flatMap(function)` | Generator expression with nested loops | No single flatMap; use comprehension |
| `.reduce(identity, accumulator)` | `functools.reduce(func, iterable, initial)` | Not commonly used; explicit loops often clearer |
| `.collect(Collectors.toList())` | `list(iterable)` | Just wrap with `list()` |
| `.collect(Collectors.toSet())` | `set(iterable)` | Just wrap with `set()` |
| `.collect(Collectors.groupingBy())` | `itertools.groupby()` or dict comprehension | `groupby` needs sorted input; dict comp is usually clearer |
| `.sorted()` | `sorted(iterable)` | Built-in; no need to collect |
| `.distinct()` | `set(iterable)` or `dict.fromkeys()` | Order-preserving: `dict.fromkeys(iterable)` |
| `.limit(n)` | `itertools.islice(iterable, n)` | Or slice a list |
| `.count()` | `sum(1 for _ in iterable)` or `len(list(...))` | |
| `.anyMatch(pred)` | `any(pred(x) for x in iterable)` | Short-circuits like Java |
| `.allMatch(pred)` | `all(pred(x) for x in iterable)` | Short-circuits like Java |
| `.noneMatch(pred)` | `not any(pred(x) for x in iterable)` | |
| `.findFirst()` | `next((x for x in iterable if cond), None)` | |
| `.forEach(action)` | `for x in iterable: action(x)` | Explicit loop is more idiomatic |
| `.parallelStream()` | `concurrent.futures.ProcessPoolExecutor` | Very different model |
| `.peek(debug)` | No direct equivalent | Print inside comprehension is side-effectful; avoid |

### Comprehension vs Stream

Java:
```java
List<String> names = employees.stream()
    .filter(e -> e.getDepartment().equals("engineering"))
    .map(Employee::getName)
    .sorted()
    .collect(Collectors.toList());
```

Python:
```python
names = sorted(
    e.name
    for e in employees
    if e.department == "engineering"
)
```

Python:
```python
# Or with explicit comprehension
names = sorted([e.name for e in employees if e.department == "engineering"])
```

### groupingBy Equivalent

Java:
```java
Map<String, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment));
```

Python:
```python
from collections import defaultdict

by_dept: dict[str, list] = defaultdict(list)
for e in employees:
    by_dept[e.department].append(e)
```

Or with dict comprehension:
```python
by_dept = {}
for e in employees:
    by_dept.setdefault(e.department, []).append(e)
```

### What Does Not Exist In Python

- `.parallelStream()` equivalent — Python list comprehensions run single-threaded.
- `Collectors.joining()` equivalent — use `"sep".join(...)`.
- `.peek()` for debugging without side effects — Python has no lazy pipeline peek.
- Method references in stream style (`Employee::getName`) — Python uses `lambda e: e.name` or `attrgetter`.

### Interview Trap

```text
Java developer writes a streaming pipeline using map() and filter() functions:
  result = filter(lambda x: x > 0, numbers)

This works, but is not idiomatic Python.
Idiomatic Python:
  result = [x for x in numbers if x > 0]  # list comprehension
  result = (x for x in numbers if x > 0)  # generator expression (lazy)

The interviewer expects you to know that comprehensions are the Pythonic choice,
not chained map/filter calls.
```

---

## 8. Exception Handling

### Syntax Comparison

Java:
```java
try {
    result = riskyOperation();
} catch (IOException e) {
    log.error("IO error", e);
    throw new ServiceException("failed", e);
} catch (IllegalArgumentException e) {
    throw e;
} finally {
    cleanup();
}
```

Python:
```python
try:
    result = risky_operation()
except OSError as e:
    logger.error("IO error: %s", e)
    raise ServiceError("failed") from e
except ValueError:
    raise
else:
    # Only runs if no exception occurred — no Java equivalent
    record_success()
finally:
    cleanup()
```

### Key Differences

| Concept | Java | Python |
|---|---|---|
| No exception raised | try block completes, no special clause | `else` clause runs |
| Re-raise | `throw e;` or `throw;` | `raise` (bare — re-raises current exception) |
| Chained exception | `throw new Exc("msg", cause)` | `raise NewExc("msg") from original_exc` |
| Checked exceptions | Declared on method signature | Does not exist |
| Multiple catch | `catch (A \| B e)` | `except (A, B) as e:` |
| Custom exception | `class MyEx extends RuntimeException` | `class MyError(Exception):` |
| Try-with-resources | `try (Resource r = new Resource())` | `with open(path) as f:` |
| `finally` always runs | Yes | Yes |

### No Checked Exceptions

Python has only unchecked exceptions. The equivalent of Java's checked exceptions is:

- Docstrings documenting what a function may raise.
- Type hints with `raises` in docstrings (PEP 257 style).
- Some frameworks use return types like `Result[T, Error]` for explicit error modeling.

```python
def load_config(path: str) -> dict:
    """
    Load configuration from a JSON file.

    Raises:
        FileNotFoundError: if the file does not exist.
        json.JSONDecodeError: if the file is not valid JSON.
    """
    with open(path) as f:
        return json.load(f)
```

### What Does Not Exist In Python

- Checked exceptions.
- `throws` declaration on method signatures.
- `try`-with-resources syntax (use `with` statement instead).

### Interview Trap

```text
Java developer says: "Python has try/catch like Java."

The keyword in Python is except, not catch.
Python also adds an else clause (no Java equivalent): runs when no exception occurred.
Never catch bare Exception or BaseException without a plan.
BaseException also catches SystemExit and KeyboardInterrupt, which you almost never want.
```

---

## 9. Concurrency: The Most Critical Mental Shift

### The GIL Changes Everything

| Concept | Java | Python |
|---|---|---|
| True CPU parallelism with threads | Yes | Not in default GIL-enabled CPython for Python bytecode |
| IO-bound concurrency with threads | Yes | Yes — GIL released during IO waits |
| CPU-bound parallelism | `ThreadPoolExecutor` | `multiprocessing.Pool` |
| Lightweight concurrent tasks | `CompletableFuture` / virtual threads | `asyncio` coroutines |
| Thread creation | `new Thread(runnable).start()` | `threading.Thread(target=func).start()` |
| Executor service | `ExecutorService` | `concurrent.futures.ThreadPoolExecutor` |
| Process pool | Separate JVM or heavy framework | `concurrent.futures.ProcessPoolExecutor` |
| Async programming | Reactive (`CompletableFuture`) | `async`/`await` with `asyncio` |
| Mutex / lock | `synchronized` / `ReentrantLock` | `threading.Lock()` |
| Atomic integer | `AtomicInteger` | No direct equivalent; use `threading.Lock` |
| Thread-safe queue | `BlockingQueue` | `queue.Queue` |
| Counting semaphore | `Semaphore` | `threading.Semaphore` |
| `volatile` visibility | `volatile int x` | No direct equivalent; use locks/events/queues for synchronization |

Version caveat:
- Python 3.13+ supports optional free-threaded CPython builds where the GIL can be disabled.
- Most production deployments and most interviews still assume default GIL-enabled CPython unless the interviewer asks specifically about no-GIL Python.

### When To Use What

```text
IO-bound (DB, HTTP, files, network):
  threading.Thread or ThreadPoolExecutor    — releases GIL during IO
  asyncio (async/await)                     — single thread, cooperative scheduling, no GIL needed

CPU-bound (computation, parsing, encoding):
  multiprocessing.Pool or ProcessPoolExecutor — separate processes, separate GIL each
  free-threaded CPython build — advanced Python 3.13+ option; validate dependency support first

Both IO and CPU:
  multiprocessing for CPU parts
  asyncio or threading for IO parts
```

### Code Comparison

Java thread pool:
```java
ExecutorService executor = Executors.newFixedThreadPool(4);
Future<String> future = executor.submit(() -> callService());
String result = future.get();
executor.shutdown();
```

Python thread pool:
```python
from concurrent.futures import ThreadPoolExecutor

with ThreadPoolExecutor(max_workers=4) as executor:
    future = executor.submit(call_service)
    result = future.result()
```

Java CompletableFuture:
```java
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> callService())
    .thenApply(result -> transform(result))
    .exceptionally(ex -> "default");
```

Python asyncio:
```python
import asyncio

async def main() -> str:
    try:
        result = await call_service_async()
        return transform(result)
    except Exception:
        return "default"

asyncio.run(main())
```

### What Does Not Exist In Python

- True CPU-parallel threading in default GIL-enabled CPython. Python 3.13+ free-threaded builds are the advanced caveat, not the baseline assumption.
- `synchronized` keyword on methods.
- `volatile` keyword.
- `AtomicInteger`, `AtomicReference`.
- Java Memory Model (`happens-before`) as a formal construct. Use explicit synchronization in Python; do not treat the GIL as a replacement for locks around business invariants.
- `CompletableFuture` chaining style for async — Python uses `async/await`.
- Virtual threads (Java 21+). Python has no equivalent in CPython.

### Interview Trap

```text
Java developer says: "I would use Python threads to parallelize CPU work like in Java."

Correct answer: In default CPython, Python threading does not achieve CPU parallelism for
Python bytecode due to the GIL. For CPU-bound parallelism, use multiprocessing, native
extensions that release the GIL, or validate whether a Python 3.13+ free-threaded build is
appropriate for the environment. For IO-bound concurrency, threading works or asyncio is
more scalable. For high-concurrency blocking IO services, asyncio with async/await is the
Python equivalent of Java virtual threads in terms of programming model, but the
implementation is fundamentally different: asyncio is single-threaded cooperative scheduling,
not lightweight OS threads.
```

---

## 10. Generics vs Type Hints

### Side-By-Side

| Concept | Java | Python |
|---|---|---|
| Generic class | `class Box<T>` | `from typing import Generic, TypeVar; T = TypeVar("T"); class Box(Generic[T]):` |
| Generic method | `<T> T identity(T value)` | `def identity(value: T) -> T:` with TypeVar |
| Bounded type | `<T extends Number>` | `TypeVar("T", bound=Number)` |
| Wildcard | `List<? extends Animal>` | `list[Animal]` or covariant `Sequence[Animal]` |
| Erased at runtime | Yes (type erasure) | Yes (type hints are stripped at runtime by default) |
| Enforced at runtime | Yes (compilation) | No (type hints are hints only) |
| Optional type | `Optional<T>` | `Optional[T]` from typing or `T \| None` (3.10+) |
| List of strings | `List<String>` | `list[str]` (3.9+) |
| Map | `Map<String, Integer>` | `dict[str, int]` |

### Type Hints Usage

```python
from typing import Optional

def find_user(user_id: str) -> Optional[str]:
    if user_id in store:
        return store[user_id]
    return None

# Python 3.10+ syntax
def find_user(user_id: str) -> str | None:
    ...
```

### What Type Hints Do Not Do

```python
def add(x: int, y: int) -> int:
    return x + y

add("hello", "world")   # No runtime error — type hints are not enforced
```

### Protocol For Structural Typing

```python
from typing import Protocol

class Drawable(Protocol):
    def draw(self) -> None:
        ...

def render_all(items: list[Drawable]) -> None:
    for item in items:
        item.draw()
```

Any object with a `draw` method satisfies `Drawable` without declaring it.

### Interview Trap

```text
Java developer says: "Python type hints are enforced like Java generics."

Correct answer: Python type hints are not enforced at runtime by default. They are
annotations used by static type checkers like mypy and pyright, and by IDEs.
A function declared to accept int will not raise a TypeError if passed a string
at runtime — the type hint is documentation and static analysis, not a runtime guard.
Pydantic is commonly used to enforce validation at system boundaries.
```

---

## 11. Design Patterns In Python

### Pattern Mapping

| Pattern | Java | Python Equivalent |
|---|---|---|
| Singleton | Enum or static holder | Module-level instance (modules are singletons) |
| Factory Method | Abstract factory class | Function returning object, `@classmethod` |
| Builder | Builder class with `build()` | `dataclass` or keyword arguments |
| Strategy | Interface + implementations | Functions passed as arguments, or class with callable |
| Observer | Interface + listener list | Callbacks, event libraries, `weakref` for listeners |
| Decorator | Wrapper class | Python `@decorator` syntax — much simpler |
| Template Method | Abstract class + hook | `ABC` with abstract methods |
| Adapter | Wrapper class | Wrapper class or duck-typing shim |
| Repository | Interface + implementation | ABC + concrete class with in-memory or DB backend |
| Proxy | Dynamic proxy, cglib | `__getattr__` delegation or `functools.wraps` |

### Decorator Pattern — Python Native

```python
import functools
import time
from typing import Callable, TypeVar, Any

F = TypeVar("F", bound=Callable[..., Any])

def timer(func: F) -> F:
    @functools.wraps(func)
    def wrapper(*args: Any, **kwargs: Any) -> Any:
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = time.perf_counter() - start
        print(f"{func.__name__} took {elapsed:.4f}s")
        return result
    return wrapper  # type: ignore[return-value]

@timer
def process_orders(orders: list) -> int:
    return sum(o.amount for o in orders)
```

### Strategy Pattern — Python Idiomatic

```python
from typing import Callable

def apply_discount(amount: float, strategy: Callable[[float], float]) -> float:
    return strategy(amount)

def ten_percent_off(amount: float) -> float:
    return amount * 0.9

def flat_five_off(amount: float) -> float:
    return max(0, amount - 5)

print(apply_discount(100.0, ten_percent_off))   # 90.0
print(apply_discount(100.0, flat_five_off))      # 95.0
```

Functions as first-class objects remove the need for a Strategy interface + ConcreteStrategy class hierarchy.

### Singleton — The Pythonic Way

```python
# Module-level instance — the simplest singleton
# config.py
_instance = None

def get_config():
    global _instance
    if _instance is None:
        _instance = Config()
    return _instance
```

Or: modules themselves are singletons. Importing `config` twice returns the same module object.

### Interview Trap

```text
Java developer implements a full Singleton with double-checked locking in Python.

Python answer: modules are singletons. Define a module-level instance.
If you need lazy initialization with thread safety, use a Lock.
Double-checked locking is a Java pattern solving a Java problem. Python's import system
already handles module-level singletons with thread safety via import locks.
```

---

## 12. Testing

### Framework Mapping

| Concept | Java | Python |
|---|---|---|
| Test framework | JUnit 5 | `pytest` |
| Test annotation | `@Test` | Function name starts with `test_` |
| Assertions | `assertEquals(expected, actual)` | `assert actual == expected` or `pytest.raises()` |
| Expected exception | `@Test(expected = ...)` or `assertThrows` | `with pytest.raises(ExceptionType):` |
| Before each test | `@BeforeEach` | `pytest` fixture with function scope |
| Before all tests | `@BeforeAll` | `pytest` fixture with module or session scope |
| Setup fixture | `@Before` / `@BeforeEach` | `@pytest.fixture` |
| Parameterized test | `@ParameterizedTest` | `@pytest.mark.parametrize` |
| Mocking | Mockito | `unittest.mock` / `pytest-mock` |
| Spy | `Mockito.spy()` | `unittest.mock.MagicMock(wraps=real_obj)` |
| Verify call | `verify(mock).method()` | `mock.method.assert_called_once_with(...)` |
| Stubbing | `when(mock.method()).thenReturn(val)` | `mock.method.return_value = val` |
| Testcontainers | Testcontainers Java | `testcontainers` Python library |

### Pytest Example

```python
import pytest
from booking_service import BookingService, BookingError

@pytest.fixture
def service():
    return BookingService()

def test_book_room_success(service):
    booking = service.book("R101", "2025-01-10", "2025-01-15")
    assert booking.room_id == "R101"
    assert booking.status == "CONFIRMED"

def test_book_room_overlap_raises(service):
    service.book("R101", "2025-01-10", "2025-01-15")
    with pytest.raises(BookingError, match="unavailable"):
        service.book("R101", "2025-01-12", "2025-01-17")

@pytest.mark.parametrize("check_in,check_out", [
    ("2025-01-10", "2025-01-15"),
    ("2025-02-01", "2025-02-03"),
])
def test_multiple_bookings(service, check_in, check_out):
    booking = service.book("R102", check_in, check_out)
    assert booking is not None
```

### Mocking Example

```python
from unittest.mock import MagicMock, patch
import pytest

def test_send_email_called(booking_service):
    mock_emailer = MagicMock()
    booking_service.emailer = mock_emailer

    booking_service.confirm_booking("B101")

    mock_emailer.send.assert_called_once_with(
        to="guest@example.com",
        subject="Booking confirmed"
    )
```

### What Does Not Exist In Python Testing

- JUnit 5 lifecycle annotations (`@BeforeEach`, `@AfterAll`). Use pytest fixtures.
- `@Test(expected = ...)` — use `pytest.raises` context manager.
- Mockito-style fluent stubs — Python mocking is attribute/return-value assignment.

---

## 13. Build And Packaging

### Tool Mapping

| Concept | Java | Python |
|---|---|---|
| Build tool | Maven / Gradle | `pip` + `pyproject.toml` / Poetry |
| Dependency file | `pom.xml` / `build.gradle` | `requirements.txt` / `pyproject.toml` |
| Lockfile | None by default | `poetry.lock` / `pip-compile` output |
| Package repository | Maven Central | PyPI (Python Package Index) |
| Local isolated environment | Not native | `virtualenv` / `venv` |
| Compile step | `mvn compile` / `gradle build` | No separate compile step |
| Run tests | `mvn test` | `pytest` |
| Package artifact | JAR / WAR | Wheel (`.whl`) or sdist (`.tar.gz`) |
| Executable artifact | Uber JAR / Spring Boot JAR | Python Docker image or PyInstaller binary |
| Version management | Maven versions plugin | `pip install package==version` / Poetry version |
| Import a module | `import com.example.MyClass;` | `from my_package.my_module import MyClass` |
| Classpath | JVM classpath | `sys.path` + `PYTHONPATH` |

### Virtual Environment

```bash
# Create
python3 -m venv .venv

# Activate (macOS/Linux)
source .venv/bin/activate

# Activate (Windows)
.venv\Scripts\activate

# Install packages
pip install fastapi pydantic

# Save dependencies
pip freeze > requirements.txt

# Restore
pip install -r requirements.txt
```

### Poetry (Modern Python Packaging)

```bash
# Create project
poetry new my-service

# Add dependency
poetry add fastapi pydantic

# Add dev dependency
poetry add --group dev pytest mypy

# Install all
poetry install

# Run tests
poetry run pytest
```

### Import System

```python
# Absolute import (preferred)
from mypackage.models import User
from mypackage.services.booking import BookingService

# Relative import (within package)
from .models import User
from ..utils import format_date

# Import module
import json
import os
```

### What Does Not Exist In Python

- Central build lifecycle like Maven's phases (compile, test, package, install, deploy).
- Class path resolution as a runtime concern.
- WAR file / application server deployment model.

### Interview Trap

```text
Java developer says: "Python packages are like Java packages."

Python packages are directories with __init__.py. Java packages are namespace declarations
at the top of each file mapped to directory structure. Python's __init__.py controls what
is exported from a package. In Java, access modifiers control visibility. In Python,
convention uses _ prefix.

Also: Python virtual environments are essential. Without one, pip installs globally.
Java developers are used to per-project dependency isolation via Maven/Gradle; in Python
you must create and activate a venv explicitly.
```

---

## 14. Memory And Resource Management

### Comparison

| Concept | Java | Python |
|---|---|---|
| Memory management | Generational GC | Reference counting + cyclic GC |
| Object deallocation | GC decides timing | Immediate on refcount=0 (except cycles) |
| `try-with-resources` | `try (Resource r = ...)` | `with resource as r:` |
| `finalize()` | Deprecated; `Cleaner` preferred | `__del__` — unreliable; avoid for cleanup |
| Resource safety | `AutoCloseable` + try-with-resources | Context manager (`__enter__`/`__exit__`) |
| Memory leak | Reachable references in collections | Same pattern; global caches, event listeners |
| Heap dump | `jmap`, `jcmd`, JFR | `tracemalloc`, `objgraph`, `memory_profiler` |
| GC tuning | `-XX:+UseG1GC`, `-Xmx`, `-Xms` | `gc.set_threshold()`, `gc.collect()`, or PyPy for performance |
| Memory for thread | Per-thread stack size (`-Xss`) | Per-frame Python call stack; no per-thread JVM stack size |

### Context Manager

```python
# File handling
with open("data.txt") as f:
    content = f.read()
# f.close() called automatically even if exception

# Custom context manager with class
class DatabaseConnection:
    def __enter__(self):
        self.conn = connect()
        return self.conn

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.conn.close()
        return False  # do not suppress exceptions

with DatabaseConnection() as conn:
    conn.execute("SELECT 1")

# Custom context manager with contextlib
from contextlib import contextmanager

@contextmanager
def managed_transaction(conn):
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
```

---

## 15. The 20 Highest-Frequency Interview Traps For Java Developers

### Trap 1: Mutable Default Argument

```python
# Bug
def add_tag(tag, tags=[]):
    tags.append(tag)
    return tags

add_tag("python")   # ['python']
add_tag("java")     # ['python', 'java'] — same list reused!

# Fix
def add_tag(tag, tags=None):
    if tags is None:
        tags = []
    tags.append(tag)
    return tags
```

### Trap 2: `is` vs `==` On Strings

```python
a = "hello"
b = "hello"
print(a is b)    # May be True (interned) — do NOT rely on this
print(a == b)    # True — always correct for value comparison
```

### Trap 3: Late Binding In Closures

```python
# Bug
functions = [lambda: i for i in range(5)]
print(functions[0]())   # 4 — all lambdas capture 'i' by reference, not by value

# Fix
functions = [lambda i=i: i for i in range(5)]
print(functions[0]())   # 0 — each lambda captures its own i
```

### Trap 4: Forgetting `self`

```python
class MyClass:
    def greet():        # Missing self
        return "hello"

obj = MyClass()
obj.greet()   # TypeError: greet() takes 0 positional arguments but 1 was given
```

### Trap 5: `+=` On Immutable vs Mutable

```python
# Immutable — rebinds name
x = 5
x += 1      # x now points to new int object 6

# Mutable — mutates in place
items = [1, 2]
items += [3]    # items.extend([3]) — same object
```

### Trap 6: Class Variable vs Instance Variable Mutation

```python
class Team:
    members = []        # class variable — SHARED

team1 = Team()
team2 = Team()
team1.members.append("Alice")
print(team2.members)    # ["Alice"] — unexpected!

# Fix: create in __init__
class Team:
    def __init__(self):
        self.members = []    # instance variable — unique per instance
```

### Trap 7: `except Exception` Too Broadly

```python
# Risky
try:
    do_something()
except Exception:
    pass        # silently swallows all errors including programming bugs

# Better
try:
    do_something()
except ValueError as e:
    logger.warning("bad input: %s", e)
except OSError as e:
    logger.error("IO failure: %s", e)
    raise
```

### Trap 8: `dict.keys()` Is A View, Not A Copy

```python
d = {"a": 1, "b": 2}
keys = d.keys()          # view object, not a list copy

d["c"] = 3
print(list(keys))        # ["a", "b", "c"] — view reflects change

# If you need a snapshot
keys_copy = list(d.keys())
```

### Trap 9: Iterating And Modifying A Collection

```python
items = [1, 2, 3, 4, 5]

# Bug — skips elements
for item in items:
    if item % 2 == 0:
        items.remove(item)   # modifies list while iterating

# Fix — iterate a copy
for item in items[:]:
    if item % 2 == 0:
        items.remove(item)

# Or use comprehension
items = [item for item in items if item % 2 != 0]
```

### Trap 10: `__eq__` Without `__hash__`

```python
class User:
    def __init__(self, id):
        self.id = id

    def __eq__(self, other):
        return self.id == other.id
    # No __hash__ defined — Python sets __hash__ = None

u = User("u1")
s = {u}     # TypeError: unhashable type: 'User'
```

### Trap 11: Threading Does Not Give CPU Parallelism

```python
# Java developer instinct for parallel CPU work
import threading

def heavy_compute():
    result = sum(range(10_000_000))

threads = [threading.Thread(target=heavy_compute) for _ in range(4)]
for t in threads: t.start()
for t in threads: t.join()
# NOT faster than sequential on default CPython — GIL prevents parallel CPU execution

# Fix for CPU-bound
from multiprocessing import Pool

with Pool(4) as p:
    p.map(heavy_compute, range(4))
```

### Trap 12: Blocking In Async

```python
import asyncio
import time

async def bad_handler():
    time.sleep(2)            # blocks the event loop! no other coroutine runs
    return "done"

async def good_handler():
    await asyncio.sleep(2)   # releases event loop; other coroutines can run
    return "done"
```

### Trap 13: `None` Return Without Explicit Return

```python
def process(items):
    for item in items:
        item.transform()
    # no return statement — returns None implicitly

result = process(data)
result.count()    # AttributeError: 'NoneType' object has no attribute 'count'
```

### Trap 14: `list * n` Creates Shared References

```python
# Bug — inner lists are the SAME object
matrix = [[0] * 3] * 3
matrix[0][1] = 9
print(matrix)   # [[0, 9, 0], [0, 9, 0], [0, 9, 0]] — all rows changed

# Fix — create independent lists
matrix = [[0] * 3 for _ in range(3)]
matrix[0][1] = 9
print(matrix)   # [[0, 9, 0], [0, 0, 0], [0, 0, 0]] — only first row changed
```

### Trap 15: Method Overloading Does Not Exist

```python
# Java developer writes:
class Processor:
    def process(self, items: list) -> None:
        ...

    def process(self, count: int) -> None:   # silently REPLACES the first!
        ...

# Only the second process() exists

# Fix: use default parameters or type checking
class Processor:
    def process(self, data):
        if isinstance(data, list):
            ...
        elif isinstance(data, int):
            ...
```

### Trap 16: Generator Is Exhausted After One Iteration

```python
gen = (x * 2 for x in range(5))
print(list(gen))    # [0, 2, 4, 6, 8]
print(list(gen))    # [] — generator is exhausted; cannot rewind
```

### Trap 17: Shallow Copy Trap

```python
import copy

original = [[1, 2], [3, 4]]
shallow = original[:]           # or list(original)
deep = copy.deepcopy(original)

original[0].append(99)
print(shallow[0])   # [1, 2, 99] — shallow copy shares inner lists
print(deep[0])      # [1, 2] — deep copy is independent
```

### Trap 18: `sort()` vs `sorted()`

```python
items = [3, 1, 4, 1, 5]

sorted_items = sorted(items)    # returns new list; items unchanged
items.sort()                    # modifies items in place; returns None

result = items.sort()
print(result)   # None — sort() returns None, not the list
```

### Trap 19: String Formatting Types

```python
value = 42

# Old style (like C printf — still valid but not preferred)
msg = "Value is %d" % value

# .format() style
msg = "Value is {}".format(value)

# f-string (preferred — modern Python)
msg = f"Value is {value}"

# Bad in loops (creates many objects)
result = ""
for part in parts:
    result = result + part  # each + creates a new string
# Fix
result = "".join(parts)
```

### Trap 20: Module Import Side Effects

```python
# If a .py file runs code at import time, it executes when imported
# Java developers assume imports are declarations only

# Bad — side effects at import time
import my_module    # if my_module.py has top-level print or DB calls, they run here

# Pythonic fix: guard with __main__
# In my_module.py:
if __name__ == "__main__":
    main()    # only runs when the script is executed directly, not imported
```

---

## 16. Quick-Reference Summary Card

Keep this for last-minute revision before an interview.

| I think of Java... | In Python it is... | Watch out for... |
|---|---|---|
| `int x = 5` | `x = 5` (name binding) | Name has no type; object does |
| `==` on objects (reference) | `is` (identity) | Python `==` is value; `is` is identity |
| `.equals()` | `==` | Reversed from Java |
| `null` | `None` | Use `is None`, not `== None` |
| `ArrayList` | `list` | Dynamic typing; any element type |
| `HashMap` | `dict` | Ordered (3.7+); no null-key confusion |
| `HashSet` | `set` | `in` operator; not `.contains()` |
| `implements Interface` | Inherit from `ABC` or just have the method | Duck typing; explicit interface opt-in |
| `new User()` | `User()` | No `new` keyword |
| `this` | `self` (must declare it) | Missing `self` causes TypeError |
| `static` method | `@staticmethod` or `@classmethod` | `@classmethod` receives the class, not instance |
| `synchronized` | `threading.Lock()` | No keyword; explicit lock objects |
| `ExecutorService` (CPU) | `ProcessPoolExecutor` | Threads do not parallel CPU in Python |
| `ExecutorService` (IO) | `ThreadPoolExecutor` or `asyncio` | OK for IO; GIL released |
| `CompletableFuture` | `asyncio.Task` / `async/await` | Cooperative scheduling; single thread |
| `@Override` | Just redefine the method | No compile-time override verification |
| `throws IOException` | No declaration needed | No checked exceptions |
| `try (res = new R())` | `with Resource() as res:` | Context managers |
| Maven/Gradle | pip + venv / Poetry | Always use a virtual environment |
| JUnit 5 | pytest | Fixtures instead of annotations |
| Mockito | `unittest.mock` | Attribute assignment style |
| Generics at runtime | Type hints — erased at runtime | Hints are for analysis, not enforcement |
