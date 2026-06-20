# Python Type Hints, Pydantic & Validation — Gold Sheet

> **Track**: Python Interview Track — Group 2: Intermediate Backend  
> **File**: 2 of 5 (Track File #9)  
> **Audience**: Java developers learning Python for MAANG-level interviews  
> **Read after**: Python-Data-Structures-Internals-Complexity-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| Type hints are NOT enforced at runtime | ★★★★★ | Java generics enforce types at compile time; Python type hints are documentation only |
| `Optional[X]` vs `X \| None` vs just `X = None` | ★★★★★ | Most common typing question in code reviews; Java `Optional<T>` is very different |
| `list[int]` vs `List[int]` — old vs new style | ★★★★☆ | Both exist in codebases; new style (3.9+) is preferred; interviews ask the difference |
| `TypeVar` — generic functions | ★★★★☆ | Java generics `<T extends Comparable<T>>`; Python equivalent is `TypeVar` |
| Pydantic `BaseModel` — validation on instantiation | ★★★★★ | Core to FastAPI; asked in every Python backend interview; no Java equivalent |
| Pydantic `field_validator` and `model_validator` | ★★★★☆ | Custom validation — when annotation constraints are not enough |
| `TypedDict` — typed dicts for existing code | ★★★☆☆ | Java has no equivalent; useful for API response typing |
| `Protocol` — structural typing | ★★★☆☆ | Covered in OOP file; brief here as a type hint concept |
| `Literal` — enum-like value constraints | ★★★☆☆ | More precise than `str` type hint for restricted values |
| `TYPE_CHECKING` — import cycle avoidance | ★★★☆☆ | Java imports always resolve at compile time; Python has circular import issues |
| mypy/pyright — static type checking mindset | ★★★☆☆ | Understanding what a type checker accepts vs rejects |

---

## 2. Python's Type Hint Philosophy

### Must Know

```
Java:   Types are ENFORCED at compile time and runtime (generics are erased but still checked)
Python: Type hints are DOCUMENTATION only — they are NOT checked or enforced at runtime
```

Python's type system is:
- **Optional** — you don't have to annotate anything
- **Gradual** — you can annotate some things and leave others untyped
- **Static analysis only** — tools like `mypy`, `pyright`, `ruff` check types; the Python interpreter ignores them at runtime

```python
# Python will run this without error:
def add(a: int, b: int) -> int:
    return a + b

result: int = add("hello", "world")   # Type hint says int, but...
print(result)   # "helloworld" — Python ran it fine! No error.
print(type(result))   # <class 'str'>

# Type hints are stored in __annotations__ but not checked
print(add.__annotations__)   # {'a': <class 'int'>, 'b': <class 'int'>, 'return': <class 'int'>}
```

### When Type Hints Matter

```python
# Type hints provide value in these scenarios:
# 1. IDE autocompletion (VS Code, PyCharm read annotations)
# 2. Static analysis (mypy, pyright catch bugs before runtime)
# 3. Documentation (clearer than docstrings for parameter types)
# 4. Runtime validation frameworks (Pydantic, FastAPI READ annotations at runtime)
# 5. Code generation tools

# FastAPI uses annotations to generate request parsing and OpenAPI docs:
from fastapi import FastAPI
app = FastAPI()

@app.get("/users/{user_id}")
def get_user(user_id: int, active: bool = True):  # FastAPI parses and validates these!
    ...
```

---

## 3. Basic Type Annotations

### Variable Annotations

```python
# Variable annotation syntax (PEP 526)
name: str = "Alice"
age: int = 30
score: float = 9.5
active: bool = True
nothing: None = None     # Rarely useful — use for type hints in functions

# Annotation without assignment — declares type but does not create the variable
unset: int               # Note: 'unset' is NOT created! Only in __annotations__
# print(unset)           # NameError!

# Class variable annotations
class Config:
    host: str             # Annotation only — not a class variable until assigned
    port: int = 8080      # This IS a class variable with value
    debug: bool = False
```

### Function Annotations

```python
def greet(name: str, times: int = 1) -> str:
    return (f"Hello, {name}!\n") * times

# Return type None
def log(message: str) -> None:
    print(message)

# No return annotation = implicitly Any (avoid — type checkers warn)
def process(data):   # Untyped
    ...

# Complex return types
def parse(text: str) -> tuple[int, str]:
    return 1, "parsed"

def find(items: list[str], target: str) -> int | None:
    try:
        return items.index(target)
    except ValueError:
        return None
```

---

## 4. `Optional` and `Union` — Nullable Types

### Must Know

`Optional[X]` means the value can be `X` or `None`. It is exactly equivalent to `Union[X, None]` and `X | None` (Python 3.10+).

```python
from typing import Optional, Union

# Three equivalent ways to say "str or None"
def find_user(user_id: int) -> Optional[str]:    # Old style — still common
    ...

def find_user(user_id: int) -> Union[str, None]: # Explicit — verbose
    ...

def find_user(user_id: int) -> str | None:       # New style (Python 3.10+) — preferred
    ...

# Optional does NOT mean "parameter is optional"
# Optional[str] means the VALUE can be None, not that the parameter can be omitted
# Optional parameter = has a default value:
def greet(name: str, suffix: Optional[str] = None) -> str:
    if suffix:
        return f"Hello, {name} {suffix}"
    return f"Hello, {name}"
```

### `Union` — Multiple Possible Types

```python
from typing import Union

# A value that can be int OR str OR float
def process(value: Union[int, str, float]) -> str:
    return str(value)

# Python 3.10+ syntax — preferred
def process(value: int | str | float) -> str:
    return str(value)

# Never use Optional[Optional[X]] or Union[None, None] — redundant
# Union[X, None] is the same as Optional[X]

# Use isinstance checks inside the function for type narrowing
def process_v2(value: int | str) -> str:
    if isinstance(value, int):
        return f"int: {value * 2}"   # Type narrowed to int here
    return f"str: {value.upper()}"  # Type narrowed to str here
```

### `Any` — Opt Out of Type Checking

```python
from typing import Any

# Any is compatible with every type — use sparingly
def legacy_function(data: Any) -> Any:
    return data

# Common use: when interfacing with untyped libraries
import json
def parse_config(text: str) -> dict[str, Any]:
    return json.loads(text)   # json.loads returns Any — realistic annotation

# TYPE HINT ANTI-PATTERN: using Any everywhere defeats the purpose
# If you don't know the type, use Any temporarily but add a TODO comment
```

---

## 5. Container Types — Old vs New Style

### Must Know

Before Python 3.9, you had to import container types from `typing`. Since Python 3.9, built-in types support subscript syntax directly. Both styles still appear in codebases.

```python
# OLD STYLE (Python 3.5-3.8) — must import from typing
from typing import List, Dict, Tuple, Set, FrozenSet, Deque, Type

def process(items: List[int]) -> Dict[str, List[int]]:
    ...

# NEW STYLE (Python 3.9+) — use built-in types directly
def process(items: list[int]) -> dict[str, list[int]]:
    ...

# At runtime:
# Python 3.9+: list[int] creates a GenericAlias object
# Python 3.7-3.8: list[int] raises TypeError — must use List[int]

# For Python 3.7+ compatibility with new syntax, use:
from __future__ import annotations   # Defers evaluation of all annotations to strings
# This allows new-style hints even on older Python!
```

### Common Container Annotations

```python
from __future__ import annotations   # Best practice for compatibility

# List
names: list[str] = ["Alice", "Bob"]
matrix: list[list[int]] = [[1, 2], [3, 4]]

# Dict
config: dict[str, int] = {"timeout": 30}
nested: dict[str, dict[str, list[int]]] = {}

# Tuple — fixed length, specific types per position
point: tuple[float, float] = (1.0, 2.0)
rgb: tuple[int, int, int] = (255, 128, 0)

# Tuple — variable length, all same type
scores: tuple[int, ...] = (90, 85, 92)   # ... means "zero or more int"

# Set and frozenset
unique_ids: set[int] = {1, 2, 3}
immutable_tags: frozenset[str] = frozenset({"python", "backend"})

# Optional and Union
result: int | None = None             # Python 3.10+
result_old: Optional[int] = None      # Python 3.7+

# Mapping, Sequence, Iterable — abstract types from collections.abc
from collections.abc import Mapping, Sequence, Iterable, Iterator, Generator

def accept_any_dict(mapping: Mapping[str, int]) -> None: ...
def accept_any_list(seq: Sequence[int]) -> None: ...
def accept_any_iter(items: Iterable[str]) -> None: ...
```

### `Sequence` vs `list` — When to Use Each

```python
from collections.abc import Sequence

# Use list[T] when you need a mutable list specifically
def append_item(lst: list[int], item: int) -> None:
    lst.append(item)   # Mutation — needs list specifically

# Use Sequence[T] when you accept any sequence (list, tuple, str, etc.)
def get_first(items: Sequence[int]) -> int | None:
    return items[0] if items else None   # Read-only — accepts list or tuple

# Prefer abstract types in function signatures — more flexible
# tuple[int, ...] is also a Sequence[int] and can be passed to Sequence params
```

---

## 6. `TypeVar` — Generic Functions and Classes

### Must Know

`TypeVar` defines a type variable — a placeholder that represents a consistent type across a function or class signature. Like Java's `<T>`.

```python
from typing import TypeVar

T = TypeVar("T")   # T can be any type

def identity(x: T) -> T:
    """Returns the same type that was passed in."""
    return x

result_int: int = identity(42)      # T = int
result_str: str = identity("hello") # T = str
# Type checker knows the return type matches the input type

# Without TypeVar, you'd use Any:
def identity_any(x: Any) -> Any:    # Loses type information — return is Any
    return x

result = identity_any(42)
result.upper()   # Type checker cannot warn — result is Any
```

### Bounded `TypeVar` — Constrain to Subtype

```python
from typing import TypeVar
from numbers import Number

# T must be int, float, or a subclass
Numeric = TypeVar("Numeric", int, float)   # Constrained to specific types

def add(a: Numeric, b: Numeric) -> Numeric:
    return a + b

add(1, 2)      # OK — int
add(1.0, 2.0)  # OK — float
# add(1, 2.0)  # Type error — T cannot be both int and float in same call

# Bounded TypeVar — T must be a subclass of the bound
from typing import TypeVar

Comparable = TypeVar("Comparable", bound="Comparable")  # Self-referential

# More practical bound:
class Animal:
    name: str

AnimalT = TypeVar("AnimalT", bound=Animal)

def get_name(animal: AnimalT) -> str:
    return animal.name   # Type checker knows animal has .name
```

### Generic Classes

```python
from typing import TypeVar, Generic

T = TypeVar("T")

class Stack(Generic[T]):
    def __init__(self) -> None:
        self._items: list[T] = []

    def push(self, item: T) -> None:
        self._items.append(item)

    def pop(self) -> T:
        return self._items.pop()

    def peek(self) -> T | None:
        return self._items[-1] if self._items else None

    def __len__(self) -> int:
        return len(self._items)


int_stack: Stack[int] = Stack()
int_stack.push(1)
int_stack.push(2)
val: int = int_stack.pop()   # Type checker knows this is int

str_stack: Stack[str] = Stack()
str_stack.push("hello")
```

### Java Developer Bridge — Generics

| | Java | Python |
|---|---|---|
| Type parameter | `class Box<T>` | `class Box(Generic[T]):` |
| Bounded type | `<T extends Number>` | `TypeVar("T", bound=Number)` |
| Wildcard | `List<?>` (read-only) | `list[Any]` or `Sequence[T]` |
| Constrained | `<T extends A & B>` | `TypeVar("T", bound=Protocol)` |
| Runtime generics | Erased to raw type at runtime | `list[int]` is `list` at runtime |
| `instanceof T` | Not possible (erased) | `isinstance(x, T)` — T must be a concrete type |

---

## 7. `Callable`, `Iterator`, `Generator`

```python
from collections.abc import Callable, Iterator, Generator
from typing import TypeVar

T = TypeVar("T")

# Callable — function or callable object
# Callable[[arg1_type, arg2_type], return_type]
def apply(func: Callable[[int], str], value: int) -> str:
    return func(value)

# Callable with no args
def run(callback: Callable[[], None]) -> None:
    callback()

# Callable with any args
def execute(func: Callable[..., int]) -> int:
    return func()

# Iterator — anything you can call next() on
def first_n(it: Iterator[int], n: int) -> list[int]:
    return [next(it) for _ in range(n)]

# Generator — function that uses yield
# Generator[YieldType, SendType, ReturnType]
def count_up(n: int) -> Generator[int, None, None]:
    for i in range(n):
        yield i

# Simple generator: yield int, no send, no return value
from collections.abc import Generator

def fibonacci() -> Generator[int, None, None]:
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b
```

---

## 8. `Literal`, `Final`, `ClassVar`

### `Literal` — Restrict to Specific Values

```python
from typing import Literal

# Literal constrains to exact values — like a type-safe enum
Direction = Literal["north", "south", "east", "west"]
HTTPMethod = Literal["GET", "POST", "PUT", "DELETE", "PATCH"]
LogLevel = Literal["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]

def move(direction: Direction) -> None:
    print(f"Moving {direction}")

move("north")    # OK
move("up")       # Type error! "up" is not in Literal

def make_request(method: HTTPMethod, url: str) -> None:
    ...

# Useful for flags
def open_file(path: str, mode: Literal["r", "w", "a", "rb", "wb"]) -> None:
    ...

# Literal with integers
def set_verbosity(level: Literal[0, 1, 2, 3]) -> None:
    ...
```

### `Final` — Prevent Reassignment

```python
from typing import Final

# Final marks a variable as constant — type checker warns if reassigned
MAX_SIZE: Final = 1000
MAX_SIZE = 2000   # Type error! Cannot reassign Final

# Final in a class — prevents override in subclass
class Config:
    VERSION: Final = "1.0"
    MAX_CONNECTIONS: Final[int] = 100

class SubConfig(Config):
    VERSION = "2.0"   # Type error! Cannot override Final
```

### `ClassVar` — Distinguish Class vs Instance Variables

```python
from typing import ClassVar

class Counter:
    # ClassVar — belongs to the class, not instances
    # @dataclass will NOT include ClassVar in __init__
    count: ClassVar[int] = 0
    total_created: ClassVar[int] = 0

    # Instance variable
    value: int

    def __init__(self, value: int) -> None:
        self.value = value
        Counter.count += 1
        Counter.total_created += 1

c1 = Counter(10)
c2 = Counter(20)
print(Counter.count)   # 2
```

---

## 9. `TypedDict` — Typed Dictionaries

### Must Know

`TypedDict` provides type hints for dictionaries with a fixed set of string keys. Useful when working with JSON APIs, configs, or legacy code that uses dicts instead of classes.

```python
from typing import TypedDict, Required, NotRequired

# Basic TypedDict — all keys required by default
class UserDict(TypedDict):
    name: str
    age: int
    email: str

# Usage
user: UserDict = {"name": "Alice", "age": 30, "email": "a@b.com"}
# user["phone"] = "..."  # Type error — "phone" not in TypedDict

# TypedDict with optional keys
class UserDictPartial(TypedDict, total=False):
    name: str
    age: int
    email: str

# Mix required and optional (Python 3.11+ or typing_extensions)
class UserDictMixed(TypedDict):
    name: str                        # Required
    age: int                         # Required
    email: NotRequired[str]          # Optional
    phone: NotRequired[str | None]   # Optional

# Inheritance
class AdminDict(UserDict):
    permissions: list[str]

# At runtime TypedDict is just a dict — no enforcement!
admin: AdminDict = {"name": "Bob", "age": 25, "email": "b@c.com", "permissions": ["read"]}
print(type(admin))   # <class 'dict'>
```

### `TypedDict` vs `@dataclass` vs Pydantic

| | `TypedDict` | `@dataclass` | Pydantic `BaseModel` |
|---|---|---|---|
| Runtime enforcement | No — just a dict | No | Yes — validates on create |
| Serialization | Dict literal | Manual or `asdict()` | `.model_dump()` |
| JSON | Already a dict | Need to call `asdict()` | `.model_dump_json()` |
| Mutation | Yes | Yes (unless frozen) | Configurable |
| Schema generation | No | No | Yes (JSON Schema) |
| Use case | Annotating existing dicts/APIs | Data containers, configs | API request/response, settings |

---

## 10. `Protocol` — Structural Typing (Brief)

```python
from typing import Protocol, runtime_checkable

# Protocol says "anything with these methods qualifies"
# No inheritance required — structural subtyping

@runtime_checkable
class Serializable(Protocol):
    def to_json(self) -> str: ...
    def to_dict(self) -> dict: ...

class User:
    def __init__(self, name: str):
        self.name = name

    def to_json(self) -> str:
        import json
        return json.dumps(self.to_dict())

    def to_dict(self) -> dict:
        return {"name": self.name}

# User satisfies Serializable without inheriting it
def save(obj: Serializable) -> None:
    data = obj.to_json()
    print(f"Saving: {data}")

save(User("Alice"))    # Works — structural match
print(isinstance(User("Alice"), Serializable))  # True (runtime_checkable)
```

---

## 11. Advanced: `overload`, `cast`, `TYPE_CHECKING`

### `@overload` — Multiple Signatures

```python
from typing import overload

@overload
def process(x: int) -> str: ...

@overload
def process(x: str) -> int: ...

def process(x: int | str) -> str | int:
    """Actual implementation handles both cases."""
    if isinstance(x, int):
        return str(x)
    return len(x)

# Type checker knows:
result1: str = process(42)       # Matches first overload
result2: int = process("hello")  # Matches second overload
```

### `cast` — Inform Type Checker Without Runtime Check

```python
from typing import cast

# cast tells the type checker "trust me, this is X" — no runtime effect
data: dict[str, object] = {"count": 42}
count = cast(int, data["count"])   # Type checker sees int, not object
print(count + 1)   # No type warning

# Common use: after isinstance checks in complex code
def process(value: object) -> None:
    if isinstance(value, list):
        lst = cast(list[int], value)   # Tell checker it's list[int] specifically
        total = sum(lst)               # sum requires iterable of numbers
```

### `TYPE_CHECKING` — Avoid Circular Imports

```python
from __future__ import annotations   # REQUIRED for deferred evaluation
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from myapp.models import User   # Only imported during static analysis, NOT at runtime

class UserService:
    def get(self, user_id: int) -> "User":   # String annotation avoids runtime import
        ...

# Why: circular imports are common in large codebases
# User imports UserService; UserService imports User → circular!
# TYPE_CHECKING is False at runtime → import skipped
# Type checkers set TYPE_CHECKING = True → they see the import for analysis
```

### `from __future__ import annotations` — Always Add This

```python
# Place at top of every file — defers all annotations to strings
# This allows:
# 1. New-style generics (list[int]) on Python 3.7/3.8
# 2. Forward references without quotes
# 3. Self-referential types

from __future__ import annotations

class TreeNode:
    def __init__(self, val: int, left: TreeNode | None = None):  # No quotes needed!
        self.val = val
        self.left = left

# Without from __future__ import annotations:
# def __init__(self, val: int, left: "TreeNode" | None = None):  # Needs quotes
```

---

## 12. mypy/pyright — The Type Checker Mindset

### Must Know

```bash
# Install
pip install mypy
pip install pyright   # or: npx pyright

# Run
mypy src/             # Check all Python files in src/
mypy --strict myapp.py  # Strictest mode — no implicit Any

# Common mypy flags
# --ignore-missing-imports  — don't error on untyped third-party libs
# --disallow-untyped-defs   — all functions must have annotations
# --strict                  — maximum strictness
```

### Common Type Errors and Fixes

```python
# Error: Item "None" has no attribute "upper"
def bad(name: str | None) -> str:
    return name.upper()   # mypy error — name might be None!

def good(name: str | None) -> str:
    if name is None:
        return ""
    return name.upper()   # Type narrowed to str here — OK

# Error: Argument 1 has incompatible type "str"; expected "int"
def add(a: int, b: int) -> int:
    return a + b

add("1", 2)   # mypy catches this

# Error: Return type "None" incompatible with "int"
def multiply(a: int, b: int) -> int:
    if a == 0:
        return     # Returns None! Missing return value

# Error: Cannot determine type of variable — assign None without annotation
x = None   # mypy infers None type — cannot assign str later
x = "hello"  # mypy error

# Fix
x: str | None = None
x = "hello"   # Now OK
```

### Gradual Typing Strategy

```python
# Strategy for adding types to an existing codebase:
# 1. Start with function signatures of public APIs
# 2. Add typing to utility functions and data models
# 3. Use mypy --ignore-missing-imports initially
# 4. Progressively eliminate Any
# 5. Add py.typed marker file to mark the package as typed

# py.typed marker (PEP 561)
# Create an empty file named py.typed in your package root
# This tells type checkers the package has type information
```

---

## 13. Pydantic — `BaseModel`

### Must Know

Pydantic is a data validation library that uses Python type annotations to **enforce types and validate data at runtime**. This is the crucial difference from plain type hints — Pydantic actually checks and coerces values.

Used heavily in **FastAPI** for request/response models and **Django Ninja**, **Litestar**, and anywhere JSON data needs to be validated.

### Basic `BaseModel`

```python
from pydantic import BaseModel, Field
from datetime import datetime

class User(BaseModel):
    name: str
    age: int
    email: str
    active: bool = True           # Default value
    created_at: datetime = Field(default_factory=datetime.now)

# Validation on instantiation
user = User(name="Alice", age=30, email="alice@example.com")
print(user)
# name='Alice' age=30 email='alice@example.com' active=True created_at=datetime(...)

# Type coercion — Pydantic tries to coerce to the declared type
user2 = User(name="Bob", age="25", email="bob@example.com")  # age is a string!
print(user2.age)   # 25 (int) — Pydantic coerced "25" → 25
print(type(user2.age))  # <class 'int'>

# Validation failure
try:
    bad = User(name="Charlie", age="not-a-number", email="c@c.com")
except Exception as e:
    print(e)   # ValidationError with details

# Access fields
print(user.name)         # Alice
print(user.model_fields) # All field definitions

# Immutable by default in Pydantic v2 (for model instances you can configure this)
```

### `Field` — Per-Field Configuration

```python
from pydantic import BaseModel, Field
from typing import Annotated

class Product(BaseModel):
    id: int = Field(gt=0, description="Product ID — must be positive")
    name: str = Field(min_length=1, max_length=100, description="Product name")
    price: float = Field(gt=0, le=10000, description="Price in USD")
    tags: list[str] = Field(default_factory=list, description="Category tags")
    discount: float = Field(default=0.0, ge=0.0, le=1.0)  # 0-100% range

# Field constraints:
# gt = greater than (exclusive)
# ge = greater than or equal (inclusive)
# lt = less than (exclusive)
# le = less than or equal (inclusive)
# min_length / max_length for strings
# pattern = regex pattern for strings
# min_items / max_items for lists

p = Product(id=1, name="Widget", price=29.99, tags=["gadget", "electronics"])
print(p.model_dump())
# {'id': 1, 'name': 'Widget', 'price': 29.99, 'tags': ['gadget', 'electronics'], 'discount': 0.0}
```

### `Annotated` — Type + Validation Together

```python
from typing import Annotated
from pydantic import BaseModel, Field

# Annotated attaches metadata (like Field) to a type annotation
# This is cleaner for reusable types
PositiveInt = Annotated[int, Field(gt=0)]
NonEmptyStr = Annotated[str, Field(min_length=1)]
Percentage = Annotated[float, Field(ge=0.0, le=100.0)]

class Employee(BaseModel):
    id: PositiveInt
    name: NonEmptyStr
    salary: PositiveInt
    bonus_pct: Percentage = 0.0
```

### Nested Models and Relationships

```python
from pydantic import BaseModel
from typing import Optional

class Address(BaseModel):
    street: str
    city: str
    country: str = "US"
    zip_code: str

class Order(BaseModel):
    item: str
    quantity: int
    price: float

class Customer(BaseModel):
    name: str
    email: str
    address: Address           # Nested model — validated recursively
    orders: list[Order] = []   # List of nested models

# Creating with nested data (dict is automatically coerced to Address)
customer = Customer(
    name="Alice",
    email="alice@example.com",
    address={"street": "123 Main St", "city": "Boston", "zip_code": "02101"},
    orders=[{"item": "Widget", "quantity": 2, "price": 29.99}]
)

print(customer.address.city)     # Boston
print(customer.orders[0].item)   # Widget
```

---

## 14. Pydantic — Validators

### `field_validator` — Single Field Validation (Pydantic v2)

```python
from pydantic import BaseModel, field_validator, ValidationError

class User(BaseModel):
    name: str
    email: str
    age: int
    username: str

    @field_validator("email")
    @classmethod
    def email_must_contain_at(cls, v: str) -> str:
        if "@" not in v:
            raise ValueError("Email must contain @")
        return v.lower()   # Return the (possibly transformed) value

    @field_validator("age")
    @classmethod
    def age_must_be_adult(cls, v: int) -> int:
        if v < 18:
            raise ValueError("Must be 18 or older")
        return v

    @field_validator("username")
    @classmethod
    def username_alphanumeric(cls, v: str) -> str:
        if not v.isalnum():
            raise ValueError("Username must be alphanumeric")
        return v.lower()

# Test validation
try:
    user = User(name="Alice", email="NOT_AN_EMAIL", age=16, username="alice!")
except ValidationError as e:
    print(e)
    # 3 validation errors for User
    # email: Value error, Email must contain @
    # age: Value error, Must be 18 or older
    # username: Value error, Username must be alphanumeric

# All errors collected and reported at once (unlike exceptions which stop at first)
```

### `model_validator` — Cross-Field Validation (Pydantic v2)

```python
from pydantic import BaseModel, model_validator
from typing import Self   # Python 3.11+, or use 'Any' in 3.9/3.10

class DateRange(BaseModel):
    start_date: str
    end_date: str
    max_days: int = 30

    @model_validator(mode="after")
    def check_date_range(self) -> "DateRange":
        """Called after all fields are validated.
        'self' is the already-constructed model instance."""
        from datetime import datetime
        start = datetime.fromisoformat(self.start_date)
        end = datetime.fromisoformat(self.end_date)
        if start > end:
            raise ValueError("start_date must be before end_date")
        days = (end - start).days
        if days > self.max_days:
            raise ValueError(f"Date range {days} days exceeds max {self.max_days}")
        return self

    @model_validator(mode="before")
    @classmethod
    def normalize_dates(cls, data: dict) -> dict:
        """Called BEFORE individual field validation.
        Receives raw input data — can normalize before validation."""
        if isinstance(data, dict):
            for key in ("start_date", "end_date"):
                if key in data and data[key]:
                    data[key] = str(data[key]).strip()
        return data
```

### `BeforeValidator` and `AfterValidator` with `Annotated`

```python
from typing import Annotated
from pydantic import BaseModel
from pydantic.functional_validators import BeforeValidator, AfterValidator

def strip_str(v: str) -> str:
    return v.strip()

def to_upper(v: str) -> str:
    return v.upper()

CleanStr = Annotated[str, BeforeValidator(strip_str)]
UpperStr = Annotated[str, AfterValidator(to_upper)]

class Form(BaseModel):
    code: CleanStr    # Stripped before validation
    label: UpperStr   # Uppercased after validation

f = Form(code="  ABC  ", label="hello")
print(f.code)    # "ABC" — stripped
print(f.label)   # "HELLO" — uppercased
```

---

## 15. Pydantic v2 — Serialization and Model Configuration

### Serialization

```python
from pydantic import BaseModel
from datetime import datetime

class Event(BaseModel):
    name: str
    timestamp: datetime
    tags: list[str] = []

e = Event(name="Deploy", timestamp=datetime.now(), tags=["prod", "v2"])

# model_dump — to dict
d = e.model_dump()
print(type(d))   # dict

# model_dump_json — to JSON string
j = e.model_dump_json()
print(type(j))   # str

# Exclude fields
d_no_tags = e.model_dump(exclude={"tags"})

# Include only specific fields
d_name_only = e.model_dump(include={"name"})

# By alias (if using alias)
d_alias = e.model_dump(by_alias=True)

# model_validate — create from dict (deserialize)
data = {"name": "Rollback", "timestamp": "2024-01-15T10:00:00", "tags": ["prod"]}
e2 = Event.model_validate(data)
print(e2.timestamp)   # datetime(2024, 1, 15, 10, 0, 0) — coerced from string

# model_validate_json — create from JSON string
e3 = Event.model_validate_json('{"name": "Test", "timestamp": "2024-01-15T10:00:00"}')
```

### `model_config` — Model-Level Configuration

```python
from pydantic import BaseModel, ConfigDict

class StrictUser(BaseModel):
    model_config = ConfigDict(
        strict=True,             # No coercion — "25" won't become 25
        frozen=True,             # Immutable — no field reassignment
        extra="forbid",          # Error if extra fields passed
        str_strip_whitespace=True,  # Auto-strip strings
        populate_by_name=True,   # Allow both alias and field name
        validate_default=True,   # Validate default values too
    )
    name: str
    age: int

# strict=True — no coercion
try:
    u = StrictUser(name="Alice", age="25")  # ValidationError — "25" not coerced to int
except Exception as e:
    print(e)

# extra="forbid"
try:
    u = StrictUser(name="Alice", age=25, unknown_field="x")  # ValidationError
except Exception as e:
    print(e)

# frozen=True
u = StrictUser(name="Alice", age=25)
try:
    u.name = "Bob"   # ValidationError — instance is immutable
except Exception as e:
    print(e)
```

### Pydantic for Settings Management

```python
from pydantic_settings import BaseSettings
from typing import Literal

class AppSettings(BaseSettings):
    """Reads from environment variables automatically."""

    model_config = ConfigDict(env_file=".env", env_file_encoding="utf-8")

    app_name: str = "MyApp"
    debug: bool = False
    database_url: str
    secret_key: str
    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"
    max_connections: int = 100

# Usage
settings = AppSettings()   # Reads from .env file and environment variables
print(settings.database_url)   # From DATABASE_URL env var

# Environment variables are case-insensitive by default
# DATABASE_URL, database_url, Database_Url all work
```

---

## 16. Java Developer Bridge — Complete Comparison

| Concept | Java | Python |
|---|---|---|
| Type enforcement | Compile time — hard enforcement | Runtime enforcement only via Pydantic/etc. |
| Type hints | Generics — `List<T>`, `Map<K,V>` | `list[T]`, `dict[K,V]` — documentation only |
| Null safety | `Optional<T>` wraps nullable | `Optional[T]` = `T \| None` — value CAN be None |
| `Optional` meaning | `Optional<T>` means "might be absent" | `Optional[T]` = `T \| None` — same thing different syntax |
| Generics | `class Box<T>` | `class Box(Generic[T]):` |
| Bounded generics | `<T extends Number>` | `TypeVar("T", bound=Number)` |
| Wildcard | `List<? extends Animal>` | `list[Animal]` with covariance (advanced) |
| Generic erasure | Types erased at runtime | Types are strings/objects at runtime — not enforced |
| `instanceof` check | `obj instanceof List<String>` | `isinstance(obj, list)` — cannot check generic params |
| Data class + validation | Lombok + Bean Validation (`@NotNull`, `@Min`) | Pydantic `BaseModel` with `Field` constraints |
| `@NotNull` | `@NotNull` annotation | Non-`Optional` field + Pydantic |
| `@Min`, `@Max` | `@Min(0)`, `@Max(100)` | `Field(ge=0, le=100)` |
| `@Pattern` | `@Pattern(regexp="...")` | `Field(pattern="...")` |
| `@Valid` (nested) | `@Valid` on nested objects | Pydantic validates nested models automatically |
| DTO / Value Object | Java bean + getters/setters | Pydantic `BaseModel` |
| Object → JSON | Jackson `objectMapper.writeValueAsString()` | `model.model_dump_json()` |
| JSON → Object | Jackson `objectMapper.readValue()` | `Model.model_validate_json(json_str)` |
| `@JsonProperty` | `@JsonProperty("name")` | `Field(alias="name")` |
| Schema validation | JSON Schema + Jackson | Pydantic generates JSON Schema automatically |
| Static analysis | Compiler + Checkstyle | mypy / pyright |
| Checked exceptions on validation | `ConstraintViolationException` | `pydantic.ValidationError` |

---

## 17. Hot Interview Q&A

**Q: Are Python type hints enforced at runtime?**  
A: No. Python's type hints are purely for static analysis tools (mypy, pyright) and IDEs. The Python interpreter ignores them at runtime. `def f(x: int) -> str: return x` runs fine even if `x` is a float and the return is also a float. Frameworks like Pydantic and FastAPI are the exception — they explicitly read annotations at runtime using `inspect` and `typing.get_type_hints()` and perform actual validation.

**Q: What is the difference between `Optional[str]` and `str` with a default of `None`?**  
A: `Optional[str]` (or `str | None`) declares that the type of the value can be either `str` or `None`. A parameter with `default=None` has an optional parameter (it can be omitted when calling). Both can coexist: `def f(name: Optional[str] = None)` means the parameter can be omitted AND its value can be None.

**Q: What is the difference between `List[int]` (old) and `list[int]` (new)?**  
A: Functionally identical for type checking purposes. `List[int]` requires `from typing import List` and works on Python 3.5+. `list[int]` is built-in syntax added in Python 3.9 — no import needed. In Python 3.7/3.8 you can use `list[int]` in annotations if you add `from __future__ import annotations` (annotations become strings, not evaluated at import time).

**Q: What does Pydantic do that plain dataclasses don't?**  
A: Pydantic validates and coerces data at instantiation time. If you pass `age="25"` to a Pydantic model with `age: int`, it coerces `"25"` to `25`. If you pass `age="abc"`, it raises a `ValidationError` with a detailed message. Plain `@dataclass` does not validate or coerce — it accepts anything regardless of the annotation. Pydantic also provides JSON serialization/deserialization, JSON Schema generation, and nested model validation.

**Q: What is the difference between `model_validator(mode="before")` and `mode="after"`?**  
A: `mode="before"` is a classmethod that receives raw input data (usually a dict) before any field parsing — use it to normalize or transform the raw input. `mode="after"` is an instance method called after all fields have been validated and the model is constructed — use it for cross-field validation (e.g., checking that `start_date < end_date`).

**Q: What is `TypeVar` and when do you need it?**  
A: `TypeVar` is a type variable — a placeholder that represents a consistent type across a signature. Use it when you want to express that the return type is the same as (or related to) the input type. Without `TypeVar`, you'd use `Any`, which loses type information. Example: `def identity(x: T) -> T` tells the type checker that whatever type goes in comes out — `identity(42)` returns `int`, not `Any`.

**Q: When would you use `TypedDict` vs `@dataclass` vs Pydantic?**  
A: Use `TypedDict` when you receive a dict from an external source (JSON API, legacy code) and want type hints without changing the dict structure — the value IS a dict at runtime. Use `@dataclass` for simple data containers where you control the data and don't need validation. Use Pydantic when you need runtime validation, coercion, serialization, or are building FastAPI endpoints — Pydantic provides the most safety but has a dependency cost.

**Q: What is `from __future__ import annotations` and why add it?**  
A: It defers evaluation of all annotations in the file — they become strings instead of being evaluated at import time. This allows: new-style generics (`list[int]`) on Python 3.7/3.8, forward references without quotes (reference a class before it's defined), and avoiding circular import issues. It is widely recommended to add this to all files in Python 3.7-3.9 codebases.

---

## 18. Final Revision Checklist

### Type Hints Fundamentals

- [ ] I know type hints are NOT enforced at runtime — they are documentation for static tools
- [ ] I know `Optional[X]` = `Union[X, None]` = `X | None` — three equivalent notations
- [ ] I know `Optional` does NOT mean the parameter is optional — it means the value can be None
- [ ] I use `from __future__ import annotations` at the top of every file

### Container and Generic Types

- [ ] I know `list[int]` (Python 3.9+) vs `List[int]` (old import style) — same semantics
- [ ] I use `Sequence[T]` for read-only, `list[T]` for mutable lists in function params
- [ ] I can define a `TypeVar` and use it in a generic function or class
- [ ] I know `Callable[[int], str]` for function types and `Generator[Y, S, R]` for generators

### Advanced Typing

- [ ] I know `Literal["x", "y"]` restricts values — stricter than `str`
- [ ] I know `Final` prevents reassignment; `ClassVar` marks class-level attributes
- [ ] I can write a `TypedDict` for an API response dict
- [ ] I know `TYPE_CHECKING` avoids circular imports at runtime while allowing type checker imports
- [ ] I know `@overload` provides multiple signatures for the same function
- [ ] I know `cast()` is a hint only — no runtime check

### Pydantic

- [ ] I know Pydantic DOES enforce types at runtime — unlike plain type hints
- [ ] I know Pydantic coerces compatible types (e.g., `"25"` → `25` for `int` fields)
- [ ] I can use `Field(gt=0, min_length=1, pattern="...")` for per-field constraints
- [ ] I can write a `@field_validator` for single-field custom validation
- [ ] I can write a `@model_validator(mode="after")` for cross-field validation
- [ ] I know `model_dump()` → dict, `model_dump_json()` → JSON string
- [ ] I know `Model.model_validate(dict)` and `Model.model_validate_json(str)` for deserialization
- [ ] I know `model_config = ConfigDict(frozen=True, extra="forbid")` options

### Java Developer Reminders

- [ ] Java `Optional<T>` = "might not be present" (container); Python `Optional[T]` = "can be None" (union type)
- [ ] Java generics are erased at runtime; Python type hints are also not enforced — but Pydantic IS
- [ ] Pydantic `BaseModel` = Java bean + Lombok `@Data` + Bean Validation (`@NotNull`, `@Min`) combined
- [ ] `model_dump_json()` = Jackson `writeValueAsString()`, `model_validate_json()` = Jackson `readValue()`

---

*File 2 of 5 — Group 2: Intermediate Backend*  
*Next: Python-Modules-Packaging-Venv-Pip-Poetry-Gold-Sheet.md*
