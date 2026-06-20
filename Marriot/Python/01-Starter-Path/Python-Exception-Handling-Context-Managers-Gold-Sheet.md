# Python Exception Handling & Context Managers — Gold Sheet

> **Track**: Python Interview Track — Group 1: Starter Path  
> **File**: 7 of 7 — GROUP 1 COMPLETE  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-Collections-Comprehensions-Iteration-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| `try/except/else/finally` — the `else` clause | ★★★★★ | Java has no `else` on try — Python's `else` runs only when no exception was raised |
| `raise ExceptionType from original` — chaining | ★★★★★ | Java's `initCause` equivalent but cleaner; losing context with bare `raise X` is a common bug |
| Custom exception hierarchy design | ★★★★☆ | Asked in system design; Java has checked vs unchecked; Python has only unchecked |
| Context manager protocol (`__enter__`/`__exit__`) | ★★★★☆ | Java try-with-resources; Python's version is more powerful (`__exit__` can suppress exceptions) |
| `@contextmanager` — generator-based | ★★★★☆ | Most Pythonic way to write a context manager; no equivalent in Java |
| `contextlib.suppress` | ★★★☆☆ | Clean alternative to `try/except: pass` — asked in code review questions |
| `ExitStack` — dynamic resource management | ★★★☆☆ | Java has no equivalent; asked for variable-number resource cleanup scenarios |
| Exception `__cause__` vs `__context__` | ★★★☆☆ | `raise X from Y` sets `__cause__`; implicit chaining sets `__context__` |
| `BaseException` vs `Exception` | ★★★☆☆ | `KeyboardInterrupt`, `SystemExit` are `BaseException` not `Exception` — catching `Exception` does not catch them |
| `finally` with `return` — interaction | ★★★☆☆ | `finally` always runs, even if `return` is in `try` — can swallow exceptions |

---

## 2. Python Exception Hierarchy

### Must Know

```
BaseException
├── SystemExit                   ← sys.exit() — do NOT catch in except Exception
├── KeyboardInterrupt            ← Ctrl+C — do NOT catch in except Exception
├── GeneratorExit                ← generator.close() called
└── Exception                   ← Almost everything you will handle
    ├── StopIteration            ← Iterator exhausted
    ├── StopAsyncIteration
    ├── ArithmeticError
    │   ├── ZeroDivisionError    ← 1 / 0
    │   ├── OverflowError
    │   └── FloatingPointError
    ├── AssertionError           ← assert statement fails
    ├── AttributeError           ← obj.nonexistent_attr
    ├── EOFError                 ← input() reached EOF
    ├── ImportError
    │   └── ModuleNotFoundError  ← import unknown_module
    ├── LookupError
    │   ├── IndexError           ← list[999]
    │   └── KeyError             ← dict["missing"]
    ├── MemoryError
    ├── NameError
    │   └── UnboundLocalError    ← local var used before assignment
    ├── OSError (IOError alias)
    │   ├── FileNotFoundError
    │   ├── PermissionError
    │   ├── TimeoutError
    │   └── ConnectionError
    ├── RuntimeError
    │   └── RecursionError
    ├── SyntaxError
    │   └── IndentationError
    ├── TypeError                ← Wrong type for operation
    ├── ValueError               ← Right type, wrong value (int("abc"))
    │   └── UnicodeError
    └── Warning (subhierarchy)
```

### Critical Rule: Catch Specific, Not General

```python
# BAD — catches everything including programming bugs
try:
    result = process_data(data)
except Exception as e:
    log_error(e)    # Swallows IndexError, AttributeError — hides bugs!

# GOOD — catch only what you expect and can handle
try:
    result = process_data(data)
except ValueError as e:
    log_and_retry(e)
except KeyError as e:
    return default_value
# Let unexpected exceptions propagate — they reveal bugs

# WORST — bare except catches SystemExit and KeyboardInterrupt too!
try:
    ...
except:          # Catches EVERYTHING — never do this
    pass
```

### Java Developer Bridge — Exception Types

| Java | Python | Notes |
|---|---|---|
| `Exception` (checked) | Does not exist | Python has no checked exceptions |
| `RuntimeException` (unchecked) | `Exception` — all Python exceptions are unchecked | |
| `NullPointerException` | `AttributeError`, `TypeError` | `None.method()` → AttributeError |
| `ArrayIndexOutOfBoundsException` | `IndexError` | |
| `ClassCastException` | `TypeError` | |
| `IllegalArgumentException` | `ValueError` | Wrong value for a correct type |
| `IllegalStateException` | `RuntimeError` | |
| `IOException` | `OSError` and subclasses | |
| `FileNotFoundException` | `FileNotFoundError` | |
| `NumberFormatException` | `ValueError` | `int("abc")` → ValueError |
| `StackOverflowError` | `RecursionError` | |
| `OutOfMemoryError` | `MemoryError` | |
| `UnsupportedOperationException` | `NotImplementedError` | |
| `InterruptedException` | `KeyboardInterrupt` | |

---

## 3. `try/except/else/finally` — Full Mechanics

### Must Know

Python adds an `else` clause that **Java does not have**. This is the most-asked Python exception syntax question.

```
try:
    # Code that might raise
except SomeException:
    # Runs ONLY if SomeException was raised in try
else:
    # Runs ONLY if NO exception was raised in try
finally:
    # ALWAYS runs — whether exception happened or not
```

### How It Works — Step by Step

```python
def divide(a, b):
    try:
        result = a / b              # May raise ZeroDivisionError
        print(f"Division successful: {result}")
    except ZeroDivisionError as e:
        print(f"Error: {e}")
        result = None
    else:
        # Only runs if try completed WITHOUT exception
        # Use for code that should only run on success
        print(f"Result is positive: {result > 0}")
    finally:
        # ALWAYS runs — cleanup goes here
        print("divide() finished")
    return result

divide(10, 2)
# Division successful: 5.0
# Result is positive: True
# divide() finished

divide(10, 0)
# Error: division by zero
# divide() finished
```

### Why `else` Matters

```python
# WITHOUT else — ambiguous: did the error come from open() or parse()?
try:
    f = open("data.txt")
    data = parse(f.read())    # If parse raises, we can't distinguish from open failing
except OSError:
    print("File error")       # Might incorrectly catch parse() errors

# WITH else — clear separation: open fails → except; parse fails → uncaught
try:
    f = open("data.txt")
except OSError:
    print("File error")       # ONLY catches open() failures
else:
    data = parse(f.read())    # parse() errors propagate normally
    f.close()
```

**Strong Interview Answer**: "Python's `else` clause on a `try` block runs only when the `try` block completes without raising an exception. This is distinct from `finally` which always runs. The practical value is separating the code that might raise from the code that should only run on success — this makes exception handling more precise and avoids accidentally catching exceptions from code you didn't intend to guard."

### `finally` — Always Runs

```python
def risky():
    try:
        print("try")
        return "from try"       # return is about to happen
    except Exception:
        print("except")
        return "from except"
    finally:
        print("finally")        # Runs BEFORE the return actually happens!
        # If you return here, it OVERRIDES the return from try/except — dangerous!

print(risky())
# try
# finally
# from try   — finally ran but didn't override the return
```

### `finally` with Return Overrides Return — Critical Trap

```python
def bad_finally():
    try:
        return 1
    finally:
        return 2   # This OVERRIDES the return 1 — the exception is also swallowed!

print(bad_finally())   # 2  — not 1!

def also_bad():
    try:
        raise ValueError("original")
    finally:
        return "swallowed"   # return in finally swallows the exception!

print(also_bad())   # "swallowed" — ValueError is gone!
# NEVER use return in finally unless you intentionally want to swallow exceptions
```

---

## 4. Catching Multiple Exceptions

```python
# Catch multiple types in one except
try:
    value = int(input("Enter a number: "))
    result = 10 / value
except (ValueError, ZeroDivisionError) as e:
    print(f"Invalid input: {e}")

# Multiple except blocks — most specific first
try:
    data = fetch_data()
except FileNotFoundError:
    print("File not found")      # More specific — check first
except OSError as e:
    print(f"OS error: {e}")      # More general — checked second
except Exception as e:
    print(f"Unexpected: {e}")    # Catch-all — last resort

# Re-raise after logging
try:
    process()
except Exception as e:
    logger.error("Processing failed", exc_info=True)  # Log with traceback
    raise    # Re-raise the same exception with original traceback intact
    # NOT: raise e  — that loses the original traceback location!
```

### `raise` vs `raise e` — Traceback Preservation

```python
# CORRECT — preserves original traceback
try:
    risky_operation()
except Exception:
    log_error()
    raise    # Re-raises the current exception, original traceback preserved

# WRONG — resets the traceback to this line
try:
    risky_operation()
except Exception as e:
    log_error()
    raise e   # Traceback now points HERE, hiding where the real error was
```

---

## 5. Exception Chaining — `raise ... from`

### Must Know

When you catch one exception and raise another, Python can chain them to preserve the original context. Two forms:

- `raise NewException from original` — **explicit chaining**: sets `__cause__`, prints "The above exception was the direct cause of..."
- `raise NewException` inside an `except` block — **implicit chaining**: sets `__context__`, prints "During handling of the above exception, another exception occurred..."
- `raise NewException from None` — **suppresses chaining**: hides the original exception entirely.

### How It Works

```python
class DatabaseError(Exception):
    pass

def fetch_user(user_id: int):
    try:
        # Simulate a low-level error
        result = {}
        return result[user_id]      # KeyError if not found
    except KeyError as e:
        # CORRECT — chain explicitly: preserves original KeyError as __cause__
        raise DatabaseError(f"User {user_id} not found") from e

try:
    fetch_user(42)
except DatabaseError as e:
    print(f"Error: {e}")
    print(f"Caused by: {e.__cause__}")
    # Error: User 42 not found
    # Caused by: 42
```

### Traceback Output for `raise ... from`

```
Traceback (most recent call last):
  File "...", line 6, in fetch_user
KeyError: 42

The above exception was the direct cause of the following exception:

Traceback (most recent call last):
  File "...", line 10, in <module>
DatabaseError: User 42 not found
```

### Suppressing Original Exception — `raise X from None`

```python
def parse_config(text: str):
    try:
        import json
        return json.loads(text)
    except json.JSONDecodeError as e:
        # We don't want to expose json internals to callers
        raise ValueError(f"Invalid configuration format: {e.msg}") from None
        # 'from None' suppresses the JSONDecodeError — caller only sees ValueError

try:
    parse_config("not valid json {{")
except ValueError as e:
    print(e)   # Just: Invalid configuration format: ...
    # No JSONDecodeError chained — clean API boundary
```

### `__cause__` vs `__context__`

```python
try:
    raise ValueError("original")
except ValueError as e:
    raise RuntimeError("new error") from e   # __cause__ = e, __context__ = e

try:
    raise ValueError("original")
except ValueError:
    raise RuntimeError("new error")   # __context__ = original ValueError, __cause__ = None

# Check programmatically
try:
    fetch_user(42)
except DatabaseError as e:
    print(e.__cause__)     # The KeyError — explicit chain
    print(e.__context__)   # Also the KeyError — implicit chain too
    print(e.__suppress_context__)  # True if 'from None', False otherwise
```

### Java Developer Bridge

| | Java | Python |
|---|---|---|
| Exception chaining | `new Exception("msg", cause)` or `e.initCause(original)` | `raise NewException("msg") from original` |
| Accessing cause | `e.getCause()` | `e.__cause__` |
| Suppress chaining | No equivalent | `raise X from None` |
| Re-raise | `throw;` (in catch) | `raise` (bare, preserves traceback) |
| Checked exceptions | Forces caller to handle | Does not exist in Python |
| Multi-catch | `catch (IOException | SQLException e)` | `except (OSError, ValueError) as e:` |

---

## 6. Custom Exceptions

### Must Know

Design a custom exception hierarchy when:
1. Callers need to catch specific application errors separately.
2. Exceptions need to carry additional data (error codes, field names, etc.).
3. You want a base class for all your library's errors.

### Basic Custom Exception

```python
class AppError(Exception):
    """Base class for all application errors. Catch this to catch any app error."""
    pass


class ValidationError(AppError):
    """Raised when input validation fails."""

    def __init__(self, field: str, message: str, value=None):
        self.field = field
        self.value = value
        super().__init__(f"Validation failed for '{field}': {message}")


class NotFoundError(AppError):
    """Raised when a requested resource does not exist."""

    def __init__(self, resource: str, identifier):
        self.resource = resource
        self.identifier = identifier
        super().__init__(f"{resource} with id={identifier!r} not found")


class AuthorizationError(AppError):
    """Raised when an operation is not permitted."""
    pass
```

### Using the Hierarchy

```python
def get_user(user_id: int):
    if not isinstance(user_id, int) or user_id <= 0:
        raise ValidationError("user_id", "must be a positive integer", user_id)
    if user_id not in database:
        raise NotFoundError("User", user_id)
    return database[user_id]

# Caller can catch specific or general
try:
    user = get_user(user_id)
except ValidationError as e:
    return 400, {"error": str(e), "field": e.field}
except NotFoundError as e:
    return 404, {"error": str(e)}
except AppError as e:
    return 500, {"error": "Internal application error"}
```

### Exception with Additional Context

```python
class RetryableError(AppError):
    """Error that might succeed if retried."""

    def __init__(self, message: str, retry_after: float = 1.0, attempts: int = 3):
        super().__init__(message)
        self.retry_after = retry_after
        self.attempts = attempts


class ServiceUnavailableError(RetryableError):
    def __init__(self, service: str, retry_after: float = 5.0):
        super().__init__(
            f"Service '{service}' is unavailable",
            retry_after=retry_after,
            attempts=5
        )
        self.service = service
```

### Java Developer Bridge — No Checked Exceptions

```python
# Java: checked exceptions force callers to handle or declare
# public User getUser(int id) throws UserNotFoundException, DatabaseException {

# Python: no checked exceptions — document with docstring and type hints
def get_user(user_id: int) -> "User":
    """
    Fetch user by ID.

    Raises:
        ValidationError: if user_id is invalid.
        NotFoundError: if user does not exist.
        DatabaseError: if the database is unavailable.
    """
    ...
```

---

## 7. Exception Best Practices

```python
# 1. Never use exceptions for flow control
# BAD — using exception as a conditional
def get_item(items, index):
    try:
        return items[index]
    except IndexError:
        return None

# BETTER — check first (LBYL — Look Before You Leap)
def get_item(items, index):
    if 0 <= index < len(items):
        return items[index]
    return None

# ALSO GOOD for dict — EAFP (Easier to Ask Forgiveness than Permission)
# Python community PREFERS EAFP for dict access:
def get_config(key):
    try:
        return config[key]
    except KeyError:
        return default_config[key]

# 2. Avoid broad exception silencing
# BAD
try:
    send_email(user)
except Exception:
    pass   # Silent failure — impossible to debug

# GOOD
try:
    send_email(user)
except SMTPException as e:
    logger.warning(f"Email failed for {user.id}: {e}")

# 3. Use contextlib.suppress for truly ignorable exceptions (see §10)

# 4. Always chain when translating exceptions
# BAD — loses original context
try:
    parse_config(text)
except json.JSONDecodeError:
    raise ValueError("Bad config")    # Original error lost

# GOOD
try:
    parse_config(text)
except json.JSONDecodeError as e:
    raise ValueError("Bad config") from e   # Chain preserves context
```

---

## 8. The Context Manager Protocol

### Must Know

The `with` statement calls `__enter__` at the start and `__exit__` at the end — guaranteed, even if an exception occurs. This is Python's equivalent of Java's try-with-resources.

```
with expression as variable:
    body
```

Is equivalent to:
```python
manager = expression
variable = manager.__enter__()
try:
    body
except:
    if not manager.__exit__(*sys.exc_info()):
        raise   # Re-raise if __exit__ returns falsy
else:
    manager.__exit__(None, None, None)
```

### `__enter__` and `__exit__` Protocol

```python
class ManagedResource:
    def __init__(self, name: str):
        self.name = name

    def __enter__(self):
        """Called when entering the 'with' block.
        Return value is bound to the 'as' variable.
        Can return self, or a different object."""
        print(f"Acquiring {self.name}")
        return self    # 'as' variable gets this

    def __exit__(self, exc_type, exc_val, exc_tb) -> bool:
        """Called when leaving the 'with' block — always called.

        Parameters:
            exc_type  — exception class, or None if no exception
            exc_val   — exception instance, or None
            exc_tb    — traceback object, or None

        Return:
            True  — suppress the exception (swallow it)
            False (or None) — re-raise the exception
        """
        print(f"Releasing {self.name}")
        if exc_type is not None:
            print(f"  Handling exception: {exc_val}")
        return False   # Do not suppress exceptions


with ManagedResource("DatabaseConnection") as res:
    print(f"Using {res.name}")
    # Acquiring DatabaseConnection
    # Using DatabaseConnection
    # Releasing DatabaseConnection

# Exception during the block
with ManagedResource("FileHandle") as res:
    print("Working...")
    raise ValueError("Something went wrong")
    # Acquiring FileHandle
    # Working...
    # Releasing FileHandle
    # Handling exception: Something went wrong
    # ValueError: Something went wrong  (re-raised because __exit__ returned False)
```

### Suppressing Exceptions with `__exit__`

```python
class Suppress:
    """Context manager that suppresses specified exception types."""

    def __init__(self, *exception_types):
        self.exception_types = exception_types

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> bool:
        # Return True to suppress, False to re-raise
        return exc_type is not None and issubclass(exc_type, self.exception_types)


with Suppress(FileNotFoundError):
    os.remove("nonexistent_file.txt")   # Exception suppressed
    print("This still runs")            # NOT printed — execution stops at exception
# Execution continues here
print("After with block")   # Printed
```

### Context Manager for Timing

```python
import time

class Timer:
    def __init__(self, name: str = ""):
        self.name = name

    def __enter__(self):
        self.start = time.perf_counter()
        return self

    def __exit__(self, *args):
        self.elapsed = time.perf_counter() - self.start
        print(f"{self.name}: {self.elapsed:.4f}s")
        return False


with Timer("data processing") as t:
    result = sum(x**2 for x in range(10_000_000))

print(t.elapsed)   # Access elapsed time after the block
```

### Multiple Context Managers

```python
# Old way — nesting
with open("input.txt") as fin:
    with open("output.txt", "w") as fout:
        fout.write(fin.read())

# Modern way — comma-separated (Python 3.1+)
with open("input.txt") as fin, open("output.txt", "w") as fout:
    fout.write(fin.read())

# Parenthesized form for many context managers (Python 3.10+)
with (
    open("a.txt") as fa,
    open("b.txt") as fb,
    open("c.txt", "w") as fc,
):
    fc.write(fa.read() + fb.read())
```

---

## 9. `@contextmanager` — Generator-Based Context Managers

### Must Know

`contextlib.contextmanager` lets you write a context manager using a generator function. This is the most Pythonic and concise way to create context managers without a full class.

**Rule**: `yield` exactly once. Code before `yield` is `__enter__`. Code after `yield` is `__exit__`. The `yield` value becomes the `as` variable.

### How It Works

```python
from contextlib import contextmanager
import time

@contextmanager
def timer(name: str):
    """Context manager using a generator."""
    start = time.perf_counter()
    try:
        yield   # Control passes to the 'with' block here
                # No 'as' variable — yield nothing (or yield some value)
    finally:
        elapsed = time.perf_counter() - start
        print(f"{name}: {elapsed:.4f}s")


with timer("my computation"):
    result = sum(range(10_000_000))
```

### `@contextmanager` with `as` variable

```python
from contextlib import contextmanager

@contextmanager
def managed_connection(host: str, port: int):
    """Yields a connection, ensures cleanup."""
    conn = connect(host, port)     # __enter__ equivalent
    try:
        yield conn                 # conn is bound to 'as' variable
    except ConnectionError as e:
        conn.rollback()
        raise
    finally:
        conn.close()               # __exit__ equivalent — always runs


with managed_connection("localhost", 5432) as conn:
    conn.execute("SELECT 1")
```

### `@contextmanager` with Exception Handling

```python
from contextlib import contextmanager

@contextmanager
def transaction(db):
    """Database transaction context manager."""
    db.begin()
    try:
        yield db
    except Exception:
        db.rollback()    # Rollback on any exception
        raise            # Re-raise — do not suppress
    else:
        db.commit()      # Commit only if no exception
    # finally would also work for common cleanup
```

### `@contextmanager` vs Class-Based — When to Use Which

| | `@contextmanager` | Class-based |
|---|---|---|
| Lines of code | 5-10 lines | 15-25 lines |
| Readability | Higher for simple cases | Better for complex state |
| Exception handling | Wrap `yield` in try/except | In `__exit__` |
| Reuse | Limited | Can inherit, compose |
| Introspection | Harder | Class attributes visible |
| Use when | Simple acquire-release patterns | Complex state, multiple methods needed |

---

## 10. `contextlib` Module — Essential Utilities

### `contextlib.suppress` — Silence Specific Exceptions

```python
from contextlib import suppress
import os

# Without suppress — verbose
try:
    os.remove("temp_file.txt")
except FileNotFoundError:
    pass

# With suppress — clean and explicit
with suppress(FileNotFoundError):
    os.remove("temp_file.txt")

# Multiple exception types
with suppress(FileNotFoundError, PermissionError):
    os.remove("temp_file.txt")

# In a loop — suppress per iteration
for filename in files_to_delete:
    with suppress(FileNotFoundError, OSError):
        os.remove(filename)

# ONLY use suppress when the exception is truly ignorable
# Do not use it to silence errors you haven't thought about
```

### `contextlib.closing` — Close Any Object

```python
from contextlib import closing
import urllib.request

# For objects that have .close() but don't implement __enter__/__exit__
with closing(urllib.request.urlopen("https://example.com")) as page:
    content = page.read()
# page.close() called automatically

# Equivalent to writing:
page = urllib.request.urlopen("https://example.com")
try:
    content = page.read()
finally:
    page.close()
```

### `contextlib.nullcontext` — No-Op Context Manager

```python
from contextlib import nullcontext

def process(data, lock=None):
    """Process data, optionally with a lock."""
    context = lock if lock is not None else nullcontext()
    with context:
        return _process_impl(data)

# Use nullcontext when a function optionally accepts a context manager
# Avoids: if lock: with lock: ... else: ...
```

### `contextlib.redirect_stdout` / `redirect_stderr`

```python
from contextlib import redirect_stdout
import io

# Capture stdout from code that prints
captured = io.StringIO()
with redirect_stdout(captured):
    print("This goes to captured, not console")
    help(len)   # Capture help output

output = captured.getvalue()
print(f"Captured {len(output)} chars")
```

---

## 11. `contextlib.ExitStack` — Dynamic Resource Management

### Must Know

`ExitStack` manages a variable number of context managers. Essential when you don't know at write time how many resources you need to clean up.

### How It Works

```python
from contextlib import ExitStack

# Open a variable number of files
filenames = ["a.txt", "b.txt", "c.txt"]

with ExitStack() as stack:
    # Enter context managers dynamically
    files = [stack.enter_context(open(f)) for f in filenames]
    # All files will be closed when the with block exits
    for f in files:
        print(f.readline())
# All 3 files closed here, in reverse order

# Cleanup callbacks — register arbitrary callables
with ExitStack() as stack:
    conn = create_connection()
    stack.callback(conn.close)   # Will be called on exit (even on exception)
    stack.callback(lambda: print("Cleanup done"))

    # Can also push context managers
    lock = threading.Lock()
    stack.enter_context(lock)
    do_work()
```

### ExitStack for Conditional Resources

```python
from contextlib import ExitStack

def process_files(input_path, output_path=None):
    with ExitStack() as stack:
        infile = stack.enter_context(open(input_path, "r"))

        if output_path:
            outfile = stack.enter_context(open(output_path, "w"))
        else:
            import sys
            outfile = sys.stdout   # Not a context manager — don't push

        for line in infile:
            outfile.write(line.upper())
```

### ExitStack for Connection Pool

```python
from contextlib import ExitStack

def acquire_locks(*resources):
    """Acquire multiple locks safely — release all if any acquisition fails."""
    stack = ExitStack()
    try:
        for resource in resources:
            stack.enter_context(resource.lock)
        return stack
    except Exception:
        stack.close()   # Release all already-acquired locks
        raise

# Usage
with acquire_locks(resource_a, resource_b, resource_c):
    do_atomic_operation()
```

---

## 12. Common Exception Patterns in Python Production Code

### EAFP vs LBYL

```python
# EAFP — Easier to Ask Forgiveness than Permission (Pythonic)
# Try it and handle the exception
def get_user_name(data: dict, user_id: int) -> str:
    try:
        return data[user_id]["name"]
    except (KeyError, TypeError):
        return "Unknown"

# LBYL — Look Before You Leap
# Check before attempting
def get_user_name_lbyl(data: dict, user_id: int) -> str:
    if user_id in data and "name" in data[user_id]:
        return data[user_id]["name"]
    return "Unknown"

# Python community prefers EAFP for attribute/key access
# LBYL is better when the check is cheap and failures are common
```

### Exception Logging Pattern

```python
import logging

logger = logging.getLogger(__name__)

def process_record(record):
    try:
        result = transform(record)
        validate(result)
        save(result)
    except ValidationError as e:
        logger.warning("Skipping invalid record %s: %s", record.id, e)
        # Don't re-raise — just skip this record
    except DatabaseError as e:
        logger.error("Database failure for record %s", record.id, exc_info=True)
        raise   # Re-raise — this is a systemic failure
    except Exception as e:
        logger.critical("Unexpected error for record %s", record.id, exc_info=True)
        raise   # Always re-raise unexpected exceptions
```

### Context Manager for Database Transaction

```python
from contextlib import contextmanager
from typing import Generator

@contextmanager
def database_transaction(session) -> Generator:
    """
    Provides a transactional scope around a series of operations.

    Usage:
        with database_transaction(session) as txn:
            txn.add(user)
            txn.add(order)
    """
    try:
        yield session
        session.commit()
        logger.debug("Transaction committed")
    except Exception:
        session.rollback()
        logger.warning("Transaction rolled back")
        raise
    finally:
        session.close()
```

---

## 13. Java Developer Bridge — Full Exception Comparison

| Concept | Java | Python |
|---|---|---|
| Try block | `try { ... }` | `try:` |
| Catch | `catch (ExceptionType e) { ... }` | `except ExceptionType as e:` |
| Multi-catch | `catch (A \| B e)` | `except (A, B) as e:` |
| Finally | `finally { ... }` | `finally:` |
| Success-only block | Does not exist | `else:` (runs only if no exception) |
| Throw | `throw new MyException("msg")` | `raise MyException("msg")` |
| Re-throw | `throw;` | `raise` (bare — preserves traceback) |
| Re-throw with cause | `throw new B("msg", e)` | `raise B("msg") from e` |
| Suppress original | No equivalent | `raise B("msg") from None` |
| Custom exception | `class MyException extends RuntimeException` | `class MyException(Exception):` |
| Checked exceptions | Compiler-enforced `throws` declaration | Does not exist |
| Try-with-resources | `try (Resource r = new Resource())` | `with Resource() as r:` |
| AutoCloseable | `Closeable` / `AutoCloseable` interface | `__enter__` / `__exit__` protocol |
| `null` suppression | `Optional.ofNullable(x).ifPresent(...)` | `with suppress(AttributeError):` |
| Variable resources | No built-in — manual try/finally chains | `contextlib.ExitStack` |
| NullPointerException | Most common exception | `AttributeError` (on `None.method()`) |
| Exception hierarchy | `Throwable → Error / Exception → RuntimeException` | `BaseException → Exception → everything else` |
| Catching base type | Catches all subclasses | Same — `except Exception` catches all `Exception` subclasses |
| Exception info | `e.getMessage()`, `e.getCause()`, `e.printStackTrace()` | `str(e)`, `e.__cause__`, `traceback.print_exc()` |

---

## 14. Hot Interview Q&A

**Q: What does the `else` clause on a `try` block do? When does it run?**  
A: The `else` clause runs only if the `try` block completes without raising any exception. It is distinct from `finally` — `finally` always runs, `else` only runs on success. The practical use is to separate the code that might raise (in `try`) from the code that should only execute when there's no error (in `else`), making exception handling more precise and avoiding accidentally catching exceptions you didn't intend to guard.

**Q: What is the difference between `raise` and `raise e` inside an except block?**  
A: Bare `raise` re-raises the current exception with its original traceback intact — the traceback points to where the exception actually occurred. `raise e` resets the traceback to the current line, hiding the original source. Always use bare `raise` when you want to re-raise after logging or cleanup.

**Q: What is `raise X from Y` and when do you use it?**  
A: It creates an explicit exception chain. `Y` is set as `X.__cause__`, and the traceback shows "The above exception was the direct cause of the following exception." Use it when you catch a low-level exception and raise a higher-level one at an API boundary — it preserves the debugging context. `raise X from None` suppresses the chain, useful when exposing the original exception would leak implementation details.

**Q: What do `__enter__` and `__exit__` return?**  
A: `__enter__` returns the object that is bound to the `as` variable — typically `self` or a managed resource. `__exit__` receives three arguments (`exc_type`, `exc_val`, `exc_tb`) and returns a boolean. Returning `True` suppresses the exception; returning `False` or `None` allows the exception to propagate. Never return `True` from `__exit__` unless deliberately suppressing the exception.

**Q: How does `@contextmanager` work?**  
A: `@contextmanager` converts a generator function into a context manager. The code before `yield` runs as `__enter__`. The value yielded is the `as` variable. The code after `yield` runs as `__exit__`. Wrap the `yield` in `try/finally` to handle both successful and exceptional exits.

**Q: What is `contextlib.suppress` and when should you use it?**  
A: `contextlib.suppress(*exception_types)` is a context manager that silences the specified exception types, equivalent to `try: ... except ExceptionType: pass`. Use it when an exception is genuinely ignorable — like `FileNotFoundError` when deleting a file that might not exist. Avoid using it as a lazy way to silence errors you haven't analyzed.

**Q: Why does `except Exception` not catch `KeyboardInterrupt`?**  
A: `KeyboardInterrupt` and `SystemExit` are subclasses of `BaseException`, not `Exception`. Python's hierarchy deliberately separates "user code errors" (`Exception`) from "system signals" (`BaseException`). Catching `Exception` will not intercept Ctrl+C or `sys.exit()`, which is the correct behaviour — you almost never want to suppress those. Bare `except:` catches everything including `BaseException` — avoid it.

**Q: What is `ExitStack` and why would you use it?**  
A: `ExitStack` manages a dynamic number of context managers. Use it when you don't know at write time how many resources you need — for example, opening a variable-length list of files. You call `stack.enter_context(cm)` in a loop, and all context managers are exited in reverse order when the `with` block ends, even if some exits fail.

**Q: What is the difference between `__cause__` and `__context__`?**  
A: `__cause__` is set by explicit chaining (`raise B from A`) and signals intentional causation. `__context__` is set automatically whenever an exception is raised inside an `except` block — it records the implicit context. When `__cause__` is set, Python shows "The above exception was the direct cause...". When only `__context__` is set, it shows "During handling of the above exception...". `raise B from None` sets `__suppress_context__ = True`, hiding the context entirely.

**Q: What happens if `return` is used inside a `finally` block?**  
A: The `return` in `finally` executes and returns that value, overriding any `return` in the `try` or `except` block. Worse, if an exception was propagating, the `return` in `finally` silently swallows it — the exception is lost and the function returns normally. This is a severe bug. Never use `return` (or `break`, `continue`) inside `finally`.

---

## 15. Final Revision Checklist

### Exception Handling Mechanics

- [ ] I know `else` runs only on success (no exception) — distinct from `finally` which always runs
- [ ] I know bare `raise` preserves the original traceback; `raise e` resets it to the current line
- [ ] I never use `except Exception` to swallow all errors — I catch only what I can handle
- [ ] I never use bare `except:` — it catches `SystemExit` and `KeyboardInterrupt`
- [ ] I know `finally` with `return` silently swallows propagating exceptions — never do it

### Exception Chaining

- [ ] I use `raise NewException("msg") from original` to chain explicitly
- [ ] I use `raise NewException("msg") from None` to suppress the chain at API boundaries
- [ ] I know `__cause__` (explicit) vs `__context__` (implicit) and how each is displayed

### Custom Exceptions

- [ ] I define a base `AppError(Exception)` for my library's exception hierarchy
- [ ] I add structured attributes (field name, error code) to custom exceptions
- [ ] I document exceptions in docstrings — Python has no checked exceptions

### Context Managers

- [ ] I know `__enter__` returns the `as` variable and `__exit__` receives `(exc_type, exc_val, exc_tb)`
- [ ] I know `__exit__` returning `True` suppresses the exception
- [ ] I can write a context manager with `@contextmanager` using try/yield/finally
- [ ] I use `with cm1, cm2:` instead of nested `with` blocks

### `contextlib`

- [ ] I use `contextlib.suppress(ExcType)` instead of `try: ... except ExcType: pass`
- [ ] I know `nullcontext()` is a no-op context manager for optional context manager parameters
- [ ] I can use `ExitStack` to manage a dynamic number of resources

### Java Developer Reminders

- [ ] Python has no checked exceptions — I document what my functions raise in docstrings
- [ ] Python's `else` on `try` has no Java equivalent — it's the "try succeeded" block
- [ ] `with resource:` is Python's try-with-resources — any class with `__enter__`/`__exit__` works
- [ ] `@contextmanager` has no Java equivalent — it's a generator-based context manager
- [ ] `contextlib.ExitStack` replaces the Java `try { try { try { ... } finally { } } finally { } }` nesting pattern

---

## Group 1 Complete — What You Have Now

All 7 files in `01-Starter-Path/` are done:

| # | File | Lines | Key Topics |
|---|---|---|---|
| 1 | Python-Core-Hot-Interview-Master-Sheet.md | 1281 | Execution model, identity, mutability, OOP basics, GC |
| 2 | Python-For-Java-Developers-Gold-Sheet.md | 1436 | Complete Java→Python mapping, 20 traps, quick-ref card |
| 3 | Python-Data-Types-Mutability-Deep-Dive.md | 1251 | All types, hashability, Big 5 traps, type mapping table |
| 4 | Python-Functions-Scope-Closures-Args-Kwargs-Gold-Sheet.md | 1134 | LEGB, closures, late binding, `*args`/`**kwargs`, decorators |
| 5 | Python-OOP-Dataclasses-Dunder-Methods-Gold-Sheet.md | 1308 | 40 dunders, MRO, ABCs, Protocols, `@dataclass` |
| 6 | Python-Collections-Comprehensions-Iteration-Gold-Sheet.md | 1072 | Comprehensions, generators, `itertools`, `collections` |
| 7 | Python-Exception-Handling-Context-Managers-Gold-Sheet.md | — | Exceptions, chaining, context managers, `contextlib` |

**Total Group 1: ~8,500+ lines of MAANG-level Python material with Java bridges throughout.**

*Next: Group 2 — `02-Intermediate-Path/` (concurrency, async/await, type hints, testing)*

---

*File 7 of 7 — Group 1: Starter Path COMPLETE*  
*Next group: Python-Concurrency-Threading-AsyncIO-Gold-Sheet.md (Group 2, File 1)*
