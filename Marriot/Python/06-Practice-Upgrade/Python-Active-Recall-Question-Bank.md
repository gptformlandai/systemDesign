# Python Active Recall Question Bank — Gold Sheet

> **Track File #27 of 31 · Group 6: Practice Upgrade**
> For: Java developer | Level: MAANG interview readiness | Mode: answer from memory, no notes

---

## 1. How to Use This Sheet

**Rules:**
1. Cover the page below each question. Answer out loud or write on paper.
2. Only uncover the answer section **after** you've attempted an answer.
3. Mark each question: ✅ (got it cold), ⚠️ (partial), ❌ (blank).
4. Revisit ❌ items the next day; ⚠️ items in 3 days.
5. A concept isn't mastered until you score ✅ three times in a row with no notes.

**Difficulty tiers:**
- 🟢 **Foundation** — must answer without hesitation
- 🟡 **Intermediate** — should answer with some thought
- 🔴 **MAANG** — can explain mechanics and edge cases cold

---

## 2. Core Python & Data Types

### 2-A Foundation

🟢 **Q1:** Name all 8 falsy values in Python.

🟢 **Q2:** Which built-in types are immutable? Which are mutable? Name at least 5 of each.

🟢 **Q3:** What is the difference between `is` and `==`?

🟢 **Q4:** What does `a = b` do when `b` is a list? Does it copy?

🟢 **Q5:** What is `None` in Python? How should you compare to it?

🟢 **Q6:** What does `10 / 3` return in Python 3? What about `10 // 3`?

🟢 **Q7:** What does `[1, 2] * 3` produce? What about `[[]] * 3`?

### 2-B Intermediate

🟡 **Q8:** What is the difference between `copy.copy()` and `copy.deepcopy()`? When is each needed?

🟡 **Q9:** Why is `bool` a subclass of `int`? What are the consequences?

🟡 **Q10:** What does CPython integer caching mean? What range is cached?

🟡 **Q11:** What is a tuple with a mutable element? Can the tuple be mutated?

🟡 **Q12:** What does `[[]] * 3` share that `[[] for _ in range(3)]` does not?

### 2-C MAANG

🔴 **Q13:** A colleague writes `t = ([1,2],); t[0] += [3]`. What happens — does it raise? Does the list change? Explain both.

🔴 **Q14:** Explain Python's memory model for small integers from -5 to 256. Why does `256 is 256` return `True` but `257 is 257` may return `False`?

> **Answer reference:** Python-Core-Hot-Interview-Master-Sheet.md, Python-Data-Types-Mutability-Deep-Dive.md

---

## 3. Functions, Scope, Closures

### 3-A Foundation

🟢 **Q1:** What is LEGB? List the four levels.

🟢 **Q2:** What happens if you try to read a variable before assigning it in a function that also assigns it?

🟢 **Q3:** What keyword lets a nested function rebind a variable in the enclosing scope?

🟢 **Q4:** What is the mutable default argument bug? Write the buggy version and the fix.

### 3-B Intermediate

🟡 **Q5:** What is late binding in Python closures? Show a loop-lambda example and fix it.

🟡 **Q6:** What is `*args` and `**kwargs`? What types do they produce inside the function?

🟡 **Q7:** What is `functools.partial`? Give a use case.

🟡 **Q8:** What is a keyword-only argument? How do you declare one?

🟡 **Q9:** What is a positional-only argument (Python 3.8+)? How is it declared?

### 3-C MAANG

🔴 **Q10:** Walk through why `def f(x, store=[]): store.append(x); return store` accumulates state. Mention `__defaults__` in your answer.

🔴 **Q11:** A closure captures variable `i`. All closures return the same value. Why? What are two fixes?

🔴 **Q12:** What is `nonlocal` vs `global`? Describe a case where each is correct and each is an anti-pattern.

> **Answer reference:** Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md

---

## 4. OOP, Dataclasses, Dunder Methods

### 4-A Foundation

🟢 **Q1:** What is the difference between a class variable and an instance variable?

🟢 **Q2:** What does `__repr__` vs `__str__` do? Which one does `print()` call? Which does `repr()` call? Which does a list use when displaying its elements?

🟢 **Q3:** What does `@dataclass` auto-generate? Name at least 3 methods.

🟢 **Q4:** What does `frozen=True` do in a dataclass?

### 4-B Intermediate

🟡 **Q5:** What is `__slots__`? What are its memory and behavioral tradeoffs?

🟡 **Q6:** What is MRO? What algorithm does Python use to compute it?

🟡 **Q7:** What does `super()` follow when called in a class with multiple inheritance?

🟡 **Q8:** What is `__new__` vs `__init__`? When would you override `__new__`?

🟡 **Q9:** What dunder methods make an object work with `with` statements?

### 4-C MAANG

🔴 **Q10:** Class `Dog` has `tricks = []` at class level. `d1.tricks.append("roll")` vs `d1.count += 1` (where `count = 0` is a class variable). What instance attributes are created in each case?

🔴 **Q11:** Explain what `__call__` does. Give a production use case.

🔴 **Q12:** Implement `__eq__` and `__hash__` for a custom class. What rule must they follow together?

> **Answer reference:** Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md

---

## 5. Collections, Comprehensions, Iteration

### 5-A Foundation

🟢 **Q1:** What does `collections.defaultdict(list)` do differently from a regular dict?

🟢 **Q2:** What is the time complexity of `dict` lookup, `list.append`, `set.add`?

🟢 **Q3:** What is the difference between a list comprehension and a generator expression?

🟢 **Q4:** What does `sorted(data, key=lambda x: x["age"])` do?

### 5-B Intermediate

🟡 **Q5:** What is `collections.Counter`? What does `.most_common(3)` return?

🟡 **Q6:** What is `collections.deque`? When is it better than a list?

🟡 **Q7:** What is `itertools.groupby` and what must you do before using it?

🟡 **Q8:** How do you sort by multiple keys with mixed directions (one ascending, one descending)?

🟡 **Q9:** What is `zip` vs `zip_longest`?

### 5-C MAANG

🔴 **Q10:** What is `itertools.tee`? What is its memory risk?

🔴 **Q11:** You have 1M records in a file. Describe a generator pipeline that filters, transforms, and batch-inserts them with O(1) memory.

🔴 **Q12:** `itertools.groupby` vs `collections.defaultdict` for grouping — when would you choose each and why?

> **Answer reference:** Python-Collections-Comprehensions-Iteration-Gold-Sheet.md, Python-Data-Processing-Interview-Scenarios-Gold-Sheet.md

---

## 6. Exception Handling, Context Managers

### 6-A Foundation

🟢 **Q1:** What is the order of `try` / `except` / `else` / `finally`? When does `else` run?

🟢 **Q2:** What does `finally` guarantee?

🟢 **Q3:** What keyword do you use to throw a custom exception with a preserved original cause?

🟢 **Q4:** What dunder methods does a context manager need?

### 6-B Intermediate

🟡 **Q5:** A `return` statement is inside `try`. The `finally` also has a `return`. What is returned?

🟡 **Q6:** What is `contextlib.contextmanager` and how do you use it?

🟡 **Q7:** Why is `except:` (bare) dangerous? What should you use instead?

🟡 **Q8:** What happens to the variable `e` in `except ValueError as e:` after the `except` block ends?

### 6-C MAANG

🔴 **Q9:** Implement a context manager using `__enter__` and `__exit__` that measures execution time.

🔴 **Q10:** What is exception chaining? What is the difference between `raise X from Y` and `raise X from None`?

🔴 **Q11:** How does `with` protect against resource leaks? Describe what happens when an exception is thrown inside a `with` block.

> **Answer reference:** Python-Exception-Handling-Context-Managers-Gold-Sheet.md

---

## 7. Concurrency, GIL, Async

### 7-A Foundation

🟢 **Q1:** What is the GIL in default CPython? Which CPU-bound task type is affected by it?

🟢 **Q2:** For CPU-bound parallelism, which module do you use? For I/O-bound concurrency?

🟢 **Q3:** What does `async def` define? What does `await` do?

🟢 **Q4:** What is `asyncio.gather` used for?

### 7-B Intermediate

🟡 **Q5:** What is the difference between `asyncio.gather` and sequential `await` in a loop?

🟡 **Q6:** Why does `time.sleep()` in an `async def` break your entire API?

🟡 **Q7:** What is `run_in_executor`? When do you use `ThreadPoolExecutor` vs `ProcessPoolExecutor`?

🟡 **Q8:** What does `asyncio.wait_for(coro, timeout=5)` do when the timeout expires?

🟡 **Q9:** What is `asyncio.Semaphore`? Give a rate-limiting use case.

### 7-C MAANG

🔴 **Q10:** A counter is incremented by 4 threads with `counter += 1`. Is it thread-safe? Why or why not despite the GIL?

🔴 **Q11:** What is `CancelledError`? Why must it always be re-raised?

🔴 **Q12:** What is `asyncio.TaskGroup`? How does it differ from `asyncio.gather`?

🔴 **Q13:** What is `ContextVar`? How does it differ from `threading.local()`?

🔴 **Q14:** What changed with Python 3.13+ free-threaded CPython builds, and why should you still answer GIL questions with default CPython first?

> **Answer reference:** Python-Concurrency-Threading-Multiprocessing-MAANG-Master-Sheet.md, Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md, Python-Async-API-Concurrency-Scenario-Gold-Sheet.md

---

## 8. Performance, Profiling, Testing

### 8-A Foundation

🟢 **Q1:** What tool do you use to profile function-level CPU time in Python's stdlib?

🟢 **Q2:** What does `timeit` measure?

🟢 **Q3:** What is a pytest fixture? What is its scope?

🟢 **Q4:** What does `@pytest.mark.parametrize` do?

### 8-B Intermediate

🟡 **Q5:** What is `tracemalloc`? What two operations do you need to diff memory usage?

🟡 **Q6:** What does `py-spy` do that `cProfile` cannot?

🟡 **Q7:** What is the "patch where used" rule in Python mocking?

🟡 **Q8:** What is `MagicMock` vs `AsyncMock`? When do you use each?

🟡 **Q9:** What is `pytest-asyncio` used for?

### 8-C MAANG

🔴 **Q10:** Explain `__slots__` and when it gives measurable memory benefit in practice.

🔴 **Q11:** How do Testcontainers work in pytest? What scope should the container fixture use?

🔴 **Q12:** `logger.error(f"msg {val}")` vs `logger.error("msg %s", val)` — which is better and why?

🔴 **Q13:** How do you test a FastAPI endpoint that has a DB dependency, without hitting the DB?

> **Answer reference:** Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md, Python-Testing-Pytest-Mocking-Testcontainers-Gold-Sheet.md

---

## 9. Type Hints, Pydantic, Packaging

### 9-A Foundation

🟢 **Q1:** What is the difference between `list[int]` and `List[int]`?

🟢 **Q2:** What does `Optional[str]` mean? What is the Python 3.10+ syntax for it?

🟢 **Q3:** What does Pydantic `BaseModel` do when you pass invalid data?

🟢 **Q4:** What is a virtual environment and why is it needed?

### 9-B Intermediate

🟡 **Q5:** What is `TypeVar`? Give a use case with a generic function.

🟡 **Q6:** What does `@validator` (Pydantic v1) or `@field_validator` (Pydantic v2) do?

🟡 **Q7:** What is the difference between `pip install` and `poetry add`?

🟡 **Q8:** What is `pyproject.toml`? What did it replace?

🟡 **Q9:** What is `Protocol` vs `ABC`?

### 9-C MAANG

🔴 **Q10:** What does `from __future__ import annotations` do and why is it used?

🔴 **Q11:** Explain the difference between `Literal["GET", "POST"]` and `str` in a type annotation.

🔴 **Q12:** How does `poetry.lock` guarantee reproducible builds? What happens if you don't commit it?

🔴 **Q13:** Are Pydantic v2 models immutable by default? What do `frozen=True` and `validate_assignment=True` each change?

> **Answer reference:** Python-Type-Hints-Pydantic-Validation-Gold-Sheet.md, Python-Modules-Packaging-Venv-Pip-Poetry-Gold-Sheet.md

---

## 10. Decorators, Descriptors, Metaclasses

### 10-A Foundation

🟢 **Q1:** What is a decorator syntactically equivalent to?

🟢 **Q2:** What does `functools.wraps` copy? Name 3 attributes.

🟢 **Q3:** What does `@property` allow you to do?

🟢 **Q4:** What is a metaclass?

### 10-B Intermediate

🟡 **Q5:** How do you write a decorator that accepts arguments (e.g., `@retry(max_attempts=3)`)?

🟡 **Q6:** What is the order of execution when two decorators are stacked: `@A` above `@B`?

🟡 **Q7:** What is the descriptor protocol? Name the three methods.

🟡 **Q8:** What is the difference between a data descriptor and a non-data descriptor in terms of lookup priority?

### 10-C MAANG

🔴 **Q9:** How does `@property` work as a data descriptor? Why does setting `obj.x = v` call the setter and not create an instance attribute?

🔴 **Q10:** How do functions use `__get__` to inject `self`? Explain bound vs unbound methods.

🔴 **Q11:** What is `__init_subclass__`? How is it different from a metaclass for plugin registration?

🔴 **Q12:** List the 4-level descriptor lookup priority order from highest to lowest.

> **Answer reference:** Python-Decorators-Descriptors-Metaclasses-Deep-Dive-Gold-Sheet.md

---

## 11. Backend, FastAPI, APIs

### 11-A Foundation

🟢 **Q1:** What is the difference between `async def` and `def` route handlers in FastAPI?

🟢 **Q2:** What does `Depends()` do in FastAPI?

🟢 **Q3:** What is Pydantic used for in a FastAPI route?

🟢 **Q4:** What HTTP status code should a successful `POST /resources` return?

### 11-B Intermediate

🟡 **Q5:** How do you override a FastAPI dependency in tests?

🟡 **Q6:** What does `lifespan` do in FastAPI? How is it different from `@app.on_event("startup")`?

🟡 **Q7:** How does `httpx.AsyncClient` differ from `requests`?

🟡 **Q8:** What is a `BackgroundTask` in FastAPI?

### 11-C MAANG

🔴 **Q9:** Describe the full lifecycle of a request in a FastAPI app — from socket to response.

🔴 **Q10:** How do you design a FastAPI app with separate DB sessions per test? Walk through the dependency override.

🔴 **Q11:** What is connection pool exhaustion? How do you diagnose and prevent it?

> **Answer reference:** Python-Backend-APIs-FastAPI-Flask-Patterns-Gold-Sheet.md, Python-Async-API-Concurrency-Scenario-Gold-Sheet.md

---

## 12. Tricky Output Quick Round

Answer each in < 10 seconds without running the code.

```python
# Q1
x = []
y = x
x.append(1)
print(y)
```

```python
# Q2
def f(a=[]):
    a.append(1)
    return a

print(f() == f())
print(f() is f())
```

```python
# Q3
print(True + True + False)
print(bool(0.0))
print(1 == True == 1.0)
```

```python
# Q4
funcs = [lambda: i for i in range(3)]
print([f() for f in funcs])
```

```python
# Q5
class A:
    x = 0
a = A()
a.x += 1
print(A.x)
print(a.x)
```

```python
# Q6
try:
    x = 1 / 0
except ZeroDivisionError:
    pass
else:
    print("no error")
finally:
    print("always")
```

```python
# Q7
gen = (i ** 2 for i in range(5))
list(gen)
print(list(gen))
```

```python
# Q8
a = (1, 2, 3)
a += (4,)
b = a
a += (5,)
print(b)
```

**Expected answers (cover until you've answered):**

| Q | Answer | Why |
|---|---|---|
| Q1 | `[1]` | `y` and `x` are same object; append mutates in-place |
| Q2 | `True`, `True` | Both calls return the same default list object |
| Q3 | `2`, `False`, `True` | bool is int; 0.0 is falsy; chained comparison |
| Q4 | `[2, 2, 2]` | Late binding — all lambdas see final `i=2` |
| Q5 | `A.x=0`, `a.x=1` | `a.x += 1` creates instance attr; class attr unchanged |
| Q6 | `always` | Exception caught → `else` skipped → `finally` runs |
| Q7 | `[]` | Generator exhausted after first `list(gen)` |
| Q8 | `(1, 2, 3, 4)` | Tuple `+=` creates new object; `b` points to old tuple |

---

## 13. Production Debugging Quick Round

Answer aloud before reading on.

🔴 **Q1:** Memory grows from 200MB to 3GB over 8 hours. First three things you check?

🔴 **Q2:** API handles 2 req/s instead of 200 req/s. Single requests are fast. CPU shows one core at 100%. What do you do first?

🔴 **Q3:** `ImportError: cannot import name 'X' from partially initialized module`. What is this called? Name two fixes.

🔴 **Q4:** After deploying, `AttributeError: module 'requests' has no attribute 'Session'`. What caused this?

🔴 **Q5:** Payment endpoint returns HTTP 200 but no charge occurs. No errors in logs. What anti-pattern do you suspect first?

🔴 **Q6:** DB starts rejecting connections after 4 hours. No DB errors, just "too many connections". Root cause?

🔴 **Q7:** You add 4 threads to a CPU-heavy computation. No speedup. Why?

> **Answer reference:** Python-Production-Debugging-Case-Studies-Gold-Sheet.md

---

## 14. LLD Design Quick Round

Sketch each answer in 30 seconds.

🔴 **Q1:** Define the minimal Python interface (Protocol) for a `UserRepository`. Include `save`, `find_by_id`, `delete`.

🔴 **Q2:** How do you implement a singleton in Python using a metaclass?

🔴 **Q3:** What are the three layers of a `retry(max_attempts=3)` decorator factory? Name each layer's purpose.

🔴 **Q4:** You need to support multiple fee strategies in a parking lot (hourly, flat, peak). Which pattern? What does the `FeeStrategy` interface look like?

🔴 **Q5:** Design an `EventBus` with `subscribe(event, handler)` and `publish(event, payload)`. What data structure holds subscribers?

🔴 **Q6:** `OrderedDict` LRU Cache — what method moves a key to MRU? What method evicts LRU?

> **Answer reference:** Python-LLD-Machine-Coding-Patterns-Gold-Sheet.md

---

## 15. Topic-to-File Cross-Reference

| Topic | Primary File | Group |
|---|---|---|
| Python for Java devs | Python-For-Java-Developers-Gold-Sheet.md | 01 |
| Core Python interview | Python-Core-Hot-Interview-Master-Sheet.md | 01 |
| Mutability / data types | Python-Data-Types-Mutability-Deep-Dive.md | 01 |
| Functions / closures | Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md | 01 |
| OOP / dataclasses | Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md | 01 |
| Collections / iteration | Python-Collections-Comprehensions-Iteration-Gold-Sheet.md | 01 |
| Exception / context mgr | Python-Exception-Handling-Context-Managers-Gold-Sheet.md | 01 |
| DS internals / complexity | Python-Data-Structures-Internals-Complexity-Gold-Sheet.md | 02 |
| Type hints / Pydantic | Python-Type-Hints-Pydantic-Validation-Gold-Sheet.md | 02 |
| Modules / packaging | Python-Modules-Packaging-Venv-Pip-Poetry-Gold-Sheet.md | 02 |
| File I/O / serialization | Python-File-IO-Serialization-JSON-Pickle-Gold-Sheet.md | 02 |
| Backend / FastAPI | Python-Backend-APIs-FastAPI-Flask-Patterns-Gold-Sheet.md | 02 |
| Concurrency / threading | Python-Concurrency-Threading-Multiprocessing-MAANG-Master-Sheet.md | 03 |
| AsyncIO | Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md | 03 |
| Performance / profiling | Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md | 03 |
| Testing / pytest | Python-Testing-Pytest-Mocking-Testcontainers-Gold-Sheet.md | 03 |
| Scenario quick revision | Python-Scenario-Based-Quick-Revision-Gold-Sheet.md | 04 |
| Mutability scenarios | Python-Dict-List-Mutability-Request-Scenario-Gold-Sheet.md | 04 |
| Async scenarios | Python-Async-API-Concurrency-Scenario-Gold-Sheet.md | 04 |
| Data processing | Python-Data-Processing-Interview-Scenarios-Gold-Sheet.md | 04 |
| Tricky output | Python-Tricky-Output-Questions-Gold-Sheet.md | 05 |
| Decorators/descriptors | Python-Decorators-Descriptors-Metaclasses-Deep-Dive-Gold-Sheet.md | 05 |
| LLD / machine coding | Python-LLD-Machine-Coding-Patterns-Gold-Sheet.md | 05 |
| Production debugging | Python-Production-Debugging-Case-Studies-Gold-Sheet.md | 05 |

---

## 16. Final Revision Checklist

- [ ] Completed all Foundation questions without notes across all 13 topics
- [ ] Completed all Intermediate questions with only minor hesitation
- [ ] Attempted all MAANG questions — identified weak spots
- [ ] Scored all 8 Tricky Output questions without running the code
- [ ] Answered all 7 Production Debugging quick-round questions
- [ ] Answered all 6 LLD quick-round questions
- [ ] Marked every ❌ item for next-day revisit
- [ ] Can explain every Java Bridge difference in at least 6 topic areas
- [ ] Can name the correct sheet to revisit for any weak topic using the cross-reference table
