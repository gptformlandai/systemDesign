# Python Testing — Pytest, Mocking & Testcontainers — Gold Sheet

> **Track**: Python Interview Track — Group 3: Senior MAANG  
> **File**: 4 of 4 (Track File #17)  
> **Audience**: Java developers targeting MAANG-level Python backend interviews  
> **Read after**: Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| `pytest` fixtures — `conftest.py`, scope, dependency injection | ★★★★★ | JUnit `@BeforeEach`/`@AfterAll` = pytest fixtures; Python's approach is more powerful |
| `@pytest.mark.parametrize` — data-driven tests | ★★★★★ | JUnit `@ParameterizedTest` equivalent; fewer lines, more expressive |
| `unittest.mock.patch` — mocking dependencies | ★★★★★ | Mockito `@Mock`/`when().thenReturn()` equivalent; where to patch is the common trap |
| `MagicMock` vs `Mock` vs `AsyncMock` | ★★★★★ | Java has no AsyncMock equivalent — async testing is unique to Python |
| `monkeypatch` fixture — patching without `with` | ★★★★☆ | Unique to pytest; no direct Java equivalent — cleaner than `patch` for simple cases |
| `pytest` fixture scopes — function/class/module/session | ★★★★☆ | JUnit `@BeforeEach` vs `@BeforeAll`; scope controls resource lifecycle |
| `conftest.py` — shared fixture discovery | ★★★★☆ | JUnit `@ExtendWith` + shared base class; pytest auto-discovers conftest |
| Testing FastAPI with `TestClient` + `AsyncClient` | ★★★★★ | Spring `MockMvc` / `WebTestClient`; dependency override pattern critical |
| `pytest-asyncio` — async test functions | ★★★★☆ | JUnit 5 has no direct async equivalent; asyncio tests need special runner |
| Testcontainers — real DB/Redis in tests | ★★★★☆ | Java Testcontainers is identical API; Python port works the same way |
| `pytest-cov` — coverage reporting | ★★★★☆ | JaCoCo equivalent; threshold enforcement in CI |
| `pytest.raises` and `pytest.approx` | ★★★★☆ | JUnit `assertThrows` + `assertEquals(expected, actual, delta)` |

---

## 2. pytest Fundamentals

### Test Discovery and Structure

```python
# pytest discovers tests by:
# 1. Files matching test_*.py or *_test.py
# 2. Classes starting with Test
# 3. Functions starting with test_

# test_user.py
def test_add_basic():          # Discovered — function starts with test_
    assert 1 + 1 == 2

class TestUserService:         # Discovered — class starts with Test
    def test_create_user(self): # Discovered — method starts with test_
        assert True

    def helper_method(self):   # NOT discovered — doesn't start with test_
        pass

# Run tests:
# pytest                        # All tests in current directory and below
# pytest test_user.py           # Specific file
# pytest test_user.py::TestUserService::test_create_user  # Specific test
# pytest -v                     # Verbose output
# pytest -x                     # Stop after first failure
# pytest -k "user or order"     # Run tests matching keyword expression
# pytest --tb=short             # Shorter traceback format
# pytest -s                     # Disable output capture (show print statements)
# pytest -n 4                   # Run 4 workers in parallel (pytest-xdist)
```

### Assertions — pytest's Rewriting Magic

```python
# pytest rewrites `assert` statements to provide detailed failure messages
# No need for assertEqual, assertIsNone, etc. — plain assert works everywhere

def test_assertions():
    # All standard Python comparisons work
    assert 2 + 2 == 4
    assert "hello".upper() == "HELLO"
    assert [1, 2, 3] == [1, 2, 3]
    assert {"a": 1} == {"a": 1}
    assert 5 in [1, 3, 5, 7]
    assert "world" in "hello world"
    assert "world" not in "hello there"

    # On failure, pytest shows the actual values:
    # assert [1, 2, 3] == [1, 2, 4]
    # E   AssertionError: assert [1, 2, 3] == [1, 2, 4]
    # E     At index 2 diff: 3 != 4

# pytest.approx — floating point comparisons
import pytest

def test_float_comparison():
    assert 0.1 + 0.2 == pytest.approx(0.3)           # Passes! (within 1e-6 relative)
    assert 0.1 + 0.2 == pytest.approx(0.3, rel=1e-9) # Stricter tolerance
    assert 0.1 + 0.2 == pytest.approx(0.3, abs=1e-6) # Absolute tolerance

    # Works with sequences and dicts too
    assert [0.1, 0.2] == pytest.approx([0.1, 0.2])
    assert {"x": 0.1 + 0.2} == pytest.approx({"x": 0.3})

# Java: assertEquals(0.3, 0.1 + 0.2, 1e-9)
```

### `pytest.raises` — Exception Testing

```python
import pytest

def divide(a: int, b: int) -> float:
    if b == 0:
        raise ZeroDivisionError("Cannot divide by zero")
    return a / b

def test_raises_basic():
    with pytest.raises(ZeroDivisionError):
        divide(1, 0)   # This must raise ZeroDivisionError or test FAILS

def test_raises_with_message():
    with pytest.raises(ZeroDivisionError, match="Cannot divide by zero"):
        divide(1, 0)   # match is a regex against the exception message

def test_raises_capture_exception():
    with pytest.raises(ValueError) as exc_info:
        raise ValueError("Error code: 42")
    # exc_info.value is the actual exception object
    assert "42" in str(exc_info.value)
    assert exc_info.type is ValueError

# Test that a function does NOT raise — just call it without pytest.raises
def test_no_exception():
    result = divide(10, 2)   # If it raises, the test fails automatically
    assert result == 5.0

# Java: assertThrows(ZeroDivisionException.class, () -> divide(1, 0))
```

---

## 3. Fixtures — The Pytest DI System

### Must Know

```python
# Fixtures are functions decorated with @pytest.fixture
# They provide setup/teardown and dependency injection for tests
# Test functions declare their needed fixtures as function parameters — pytest injects them

import pytest

@pytest.fixture
def sample_user() -> dict:
    """Provides a sample user dict to tests."""
    return {"id": 1, "name": "Alice", "email": "alice@example.com"}

def test_user_name(sample_user: dict):
    # pytest sees 'sample_user' param — finds the fixture — injects its return value
    assert sample_user["name"] == "Alice"

def test_user_email(sample_user: dict):
    assert "@" in sample_user["email"]

# Java equivalent:
# @BeforeEach void setUp() { this.user = new User(...); }
# But pytest fixtures are more powerful — any test function can declare any fixture
```

### Setup and Teardown with `yield`

```python
import pytest
import tempfile
from pathlib import Path

@pytest.fixture
def temp_dir() -> Path:
    """Provides a temporary directory; deletes it after the test."""
    with tempfile.TemporaryDirectory() as tmpdir:
        yield Path(tmpdir)   # Everything before yield = setup; after = teardown
    # TemporaryDirectory.__exit__ runs here — directory deleted

def test_file_creation(temp_dir: Path):
    f = temp_dir / "output.txt"
    f.write_text("hello", encoding="utf-8")
    assert f.read_text(encoding="utf-8") == "hello"
# Temp dir is deleted after test, even if test fails

# Database connection fixture with yield
@pytest.fixture
def db_connection():
    conn = create_connection("sqlite:///:memory:")
    conn.execute("CREATE TABLE users (id INTEGER, name TEXT)")
    yield conn                    # Test runs here
    conn.execute("DROP TABLE users")
    conn.close()                  # Teardown — always runs

# Java equivalent:
# @BeforeEach void setUp() + @AfterEach void tearDown()
```

### Fixture Scope — Lifecycle Control

```python
import pytest

# scope controls how often the fixture is created
# "function" (default) — new instance per test function
# "class"              — one instance per test class
# "module"             — one instance per test file
# "session"            — one instance for the entire test session

@pytest.fixture(scope="function")   # Default — new per test
def fresh_list():
    return []

@pytest.fixture(scope="module")     # Once per file — expensive setup shared
def db_schema():
    print("Creating schema...")
    schema = {"tables": ["users", "orders"]}
    yield schema
    print("Dropping schema...")

@pytest.fixture(scope="session")    # Once per entire test run — very expensive setup
def app_client():
    """Spin up test server once for entire session."""
    from fastapi.testclient import TestClient
    from myapp.main import app
    with TestClient(app) as client:
        yield client

def test_one(app_client):    # Uses session-scoped client
    resp = app_client.get("/health")
    assert resp.status_code == 200

def test_two(app_client):    # Same client instance — no re-creation
    resp = app_client.get("/version")
    assert resp.status_code == 200

# Java: @BeforeEach = function, @BeforeAll = module/session
```

### Fixture Dependencies (Fixture-of-Fixtures)

```python
import pytest

@pytest.fixture
def base_config() -> dict:
    return {"host": "localhost", "port": 5432}

@pytest.fixture
def test_db(base_config: dict):  # Fixture that uses another fixture!
    url = f"postgresql://{base_config['host']}:{base_config['port']}/testdb"
    db = connect(url)
    yield db
    db.close()

@pytest.fixture
def user_service(test_db):       # Fixture chain: user_service → test_db → base_config
    return UserService(db=test_db)

def test_create_user(user_service):  # Gets fully initialized UserService
    user = user_service.create("Alice", "alice@example.com")
    assert user.id is not None
```

### `conftest.py` — Shared Fixtures

```python
# conftest.py is auto-loaded by pytest — no imports needed
# Fixtures defined here are available to ALL tests in the same directory and subdirectories

# tests/conftest.py
import pytest
from fastapi.testclient import TestClient
from myapp.main import app, get_db
from myapp.db import get_test_db_session

@pytest.fixture(scope="session")
def test_app():
    """Override DB dependency for all tests."""
    app.dependency_overrides[get_db] = get_test_db_session
    yield app
    app.dependency_overrides.clear()

@pytest.fixture(scope="session")
def client(test_app):
    with TestClient(test_app) as c:
        yield c

# tests/unit/conftest.py — only applies to tests/unit/ subdirectory
@pytest.fixture
def mock_email_service(mocker):   # Uses pytest-mock's mocker fixture
    return mocker.patch("myapp.services.email.send_email")
```

---

## 4. `@pytest.mark.parametrize` — Data-Driven Tests

### Must Know

```python
import pytest

def add(a: int, b: int) -> int:
    return a + b

# Single parameter
@pytest.mark.parametrize("n,expected", [
    (0, 0),
    (1, 1),
    (-1, -1),
    (100, 100),
])
def test_identity(n, expected):
    assert add(n, 0) == expected

# Multiple parameters
@pytest.mark.parametrize("a,b,expected", [
    (1, 2, 3),
    (0, 0, 0),
    (-1, 1, 0),
    (100, 200, 300),
])
def test_add(a, b, expected):
    assert add(a, b) == expected

# Generates 4 test cases:
# test_add[1-2-3]
# test_add[0-0-0]
# test_add[-1-1-0]
# test_add[100-200-300]

# Parametrize with IDs for readable test names
@pytest.mark.parametrize("email,is_valid", [
    ("alice@example.com", True),
    ("not-an-email", False),
    ("@missing-local.com", False),
    ("missing-at-domain.com", False),
], ids=["valid", "no-at", "no-local", "no-at-sign"])
def test_email_validation(email: str, is_valid: bool):
    from myapp.validators import validate_email
    assert validate_email(email) == is_valid

# Stacked parametrize — cartesian product
@pytest.mark.parametrize("method", ["GET", "POST"])
@pytest.mark.parametrize("endpoint", ["/users", "/orders"])
def test_endpoints(client, method, endpoint):
    # Generates 4 tests: GET/users, GET/orders, POST/users, POST/orders
    pass

# Java equivalent: @ParameterizedTest + @CsvSource or @MethodSource
```

### `pytest.param` — Skip or XFail Individual Cases

```python
import pytest

@pytest.mark.parametrize("n,expected", [
    (2, 4),
    (3, 9),
    pytest.param(0, 0, id="zero"),
    pytest.param(-1, 1, marks=pytest.mark.xfail(reason="negative not supported yet")),
    pytest.param(1000, 1_000_000, marks=pytest.mark.skip(reason="slow")),
])
def test_square(n: int, expected: int):
    assert n * n == expected
```

---

## 5. Markers — Categorizing Tests

```python
import pytest

# Built-in markers
@pytest.mark.skip(reason="Not implemented yet")
def test_future_feature():
    pass

@pytest.mark.xfail(reason="Known bug #1234", strict=True)
def test_known_broken():
    assert False   # Expected to fail; strict=True means FAIL if it unexpectedly passes

@pytest.mark.slow
def test_database_migration():
    pass

# Register custom markers in pytest.ini or pyproject.toml to avoid warnings:
# [tool.pytest.ini_options]
# markers = [
#     "slow: marks tests as slow (deselect with '-m not slow')",
#     "integration: marks tests as integration tests",
#     "unit: marks tests as unit tests",
# ]

# Running with marker filter:
# pytest -m "not slow"        — skip slow tests
# pytest -m "integration"     — only integration tests
# pytest -m "unit and not db" — unit tests that don't need DB

# Conditional skip
import sys

@pytest.mark.skipif(sys.platform == "win32", reason="Posix path test")
def test_unix_path():
    from pathlib import PurePosixPath
    p = PurePosixPath("/home/alice")
    assert str(p) == "/home/alice"
```

---

## 6. Mocking with `unittest.mock`

### Must Know — Where to Patch

```python
# CRITICAL RULE: patch where the name is USED, not where it is DEFINED
# This is the #1 mocking mistake in Python

# myapp/services/email.py
import smtplib   # <-- defined here

def send_email(to: str, subject: str) -> bool:
    smtp = smtplib.SMTP("mail.example.com")
    smtp.sendmail("from@example.com", to, subject)
    return True

# myapp/services/order.py
from myapp.services.email import send_email   # <-- USED here (imported name)

def place_order(user_email: str, item: str) -> dict:
    # ... create order ...
    send_email(user_email, f"Order confirmed: {item}")   # Uses imported send_email
    return {"status": "confirmed"}

# WRONG: patching where smtplib is defined — doesn't affect myapp.services.order
# mock.patch("smtplib.SMTP")   → Does not work for order.py tests

# WRONG: patching in the email module
# mock.patch("myapp.services.email.send_email")  → Might not affect order.py's copy

# CORRECT: patch in the MODULE WHERE IT IS USED
# mock.patch("myapp.services.order.send_email")  → Patches the name in order.py
```

### `patch` as Context Manager and Decorator

```python
from unittest.mock import patch, MagicMock, call
import pytest

# Context manager form
def test_send_email_called():
    with patch("myapp.services.order.send_email") as mock_email:
        mock_email.return_value = True   # configure what it returns
        result = place_order("alice@example.com", "Widget")
        assert result["status"] == "confirmed"
        mock_email.assert_called_once_with("alice@example.com", "Order confirmed: Widget")

# Decorator form
@patch("myapp.services.order.send_email")
def test_send_email_decorator(mock_email):
    mock_email.return_value = True
    place_order("bob@example.com", "Gadget")
    mock_email.assert_called_once()

# Multiple patches — stacked decorators (applied bottom-up, passed top-down)
@patch("myapp.services.order.create_db_record")
@patch("myapp.services.order.send_email")
def test_full_order(mock_email, mock_db):   # Order: bottom decorator = first arg
    mock_db.return_value = {"id": 42}
    mock_email.return_value = True
    result = place_order("alice@example.com", "Widget")
    assert mock_db.called
    assert mock_email.called
```

### `MagicMock` — Inspecting Calls

```python
from unittest.mock import MagicMock, call

mock = MagicMock()
mock.return_value = "result"
mock.some_attr = "value"
mock.method.return_value = 42

# Call the mock
mock("arg1", "arg2", key="value")
mock.method(1, 2)
mock.method(3, 4)

# Inspection
print(mock.called)         # True
print(mock.call_count)     # 1
print(mock.call_args)      # call('arg1', 'arg2', key='value')
print(mock.call_args_list) # [call('arg1', 'arg2', key='value')]
print(mock.method.call_count)   # 2
print(mock.method.call_args_list)   # [call(1, 2), call(3, 4)]

# Assert helpers
mock.assert_called()                          # At least once
mock.assert_called_once()                     # Exactly once
mock.assert_called_with("arg1", "arg2", key="value")   # Last call args
mock.assert_called_once_with("arg1", "arg2", key="value")
mock.method.assert_any_call(1, 2)             # Called with these args at some point
mock.method.assert_has_calls([call(1, 2), call(3, 4)])  # Called in this order
mock.assert_not_called()                      # Never called

# Side effects — raise exception or return different values
mock_db = MagicMock()
mock_db.query.side_effect = ConnectionError("DB down")
with pytest.raises(ConnectionError):
    mock_db.query("SELECT 1")

# Return different values on successive calls
mock_counter = MagicMock()
mock_counter.get.side_effect = [10, 20, 30]   # First call=10, second=20, third=30
print(mock_counter.get())   # 10
print(mock_counter.get())   # 20
print(mock_counter.get())   # 30
```

### `spec` — Type-Safe Mocks

```python
from unittest.mock import MagicMock, create_autospec

class UserService:
    def get_user(self, user_id: int) -> dict: ...
    def create_user(self, name: str, email: str) -> dict: ...

# Without spec — ANY attribute/method call succeeds (too permissive)
mock = MagicMock()
mock.nonexistent_method()   # Passes! Silently creates attribute — masks bugs

# With spec — only real attributes/methods are accessible
mock_service = MagicMock(spec=UserService)
mock_service.get_user(1)         # OK — exists on UserService
mock_service.nonexistent()       # AttributeError! Caught at test time, not production

# create_autospec — also validates argument signatures!
mock_service = create_autospec(UserService)
mock_service.get_user(1)                   # OK
mock_service.get_user(1, "extra_arg")      # TypeError! Wrong number of args

# Java: Mockito.mock(UserService.class) — already type-safe due to Java generics
```

### `patch.object` — Patching Specific Instances

```python
from unittest.mock import patch, MagicMock

class PaymentGateway:
    def charge(self, amount: float) -> bool:
        # Real HTTP call to payment processor
        import requests
        r = requests.post("https://payments.example.com/charge", json={"amount": amount})
        return r.status_code == 200

gateway = PaymentGateway()

def test_successful_charge():
    with patch.object(gateway, "charge", return_value=True) as mock_charge:
        result = gateway.charge(99.99)
        assert result is True
        mock_charge.assert_called_once_with(99.99)

# patch.dict — patching dictionaries / os.environ
import os
from unittest.mock import patch

def test_with_env_var():
    with patch.dict(os.environ, {"DATABASE_URL": "sqlite:///:memory:", "DEBUG": "true"}):
        from myapp.config import settings
        # settings reads DATABASE_URL from os.environ
        assert settings.database_url == "sqlite:///:memory:"
```

---

## 7. `AsyncMock` — Testing Async Code

### Must Know

```python
import pytest
import asyncio
from unittest.mock import AsyncMock, patch, MagicMock

# AsyncMock — mock for async functions (coroutines)
# Regular MagicMock CANNOT be awaited — must use AsyncMock

async def fetch_user(user_id: int) -> dict:
    import httpx
    async with httpx.AsyncClient() as client:
        resp = await client.get(f"https://api.example.com/users/{user_id}")
        return resp.json()

async def process_user(user_id: int) -> str:
    user = await fetch_user(user_id)   # Calls async fetch_user
    return f"Processed: {user['name']}"

@pytest.mark.asyncio
async def test_process_user():
    # patch fetch_user with an AsyncMock
    with patch("mymodule.fetch_user", new=AsyncMock(return_value={"name": "Alice"})) as mock:
        result = await process_user(1)
        assert result == "Processed: Alice"
        mock.assert_awaited_once_with(1)   # assert_awaited_* — async-specific

# AsyncMock assertion methods:
# mock.assert_awaited()              — was awaited at least once
# mock.assert_awaited_once()         — was awaited exactly once
# mock.assert_awaited_once_with(...) — awaited once with specific args
# mock.assert_awaited_with(...)      — last await had these args
# mock.assert_not_awaited()          — never awaited
# mock.await_count                   — number of times awaited
# mock.await_args                    — args from last await
# mock.await_args_list               — all await calls
```

### `pytest-asyncio` — Running Async Tests

```python
# pip install pytest-asyncio

# pyproject.toml:
# [tool.pytest.ini_options]
# asyncio_mode = "auto"   # Automatically handle async test functions — no @pytest.mark.asyncio needed

# With asyncio_mode = "auto", any async def test_ function runs as an async test
import pytest
import asyncio

async def test_auto_async():   # Runs automatically as async test with asyncio_mode=auto
    await asyncio.sleep(0)
    assert True

# Without auto mode, mark explicitly:
@pytest.mark.asyncio
async def test_explicit_async():
    await asyncio.sleep(0)
    assert True

# Async fixture
@pytest.fixture
async def async_client():
    import httpx
    from myapp.main import app
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

@pytest.mark.asyncio
async def test_with_async_client(async_client):
    resp = await async_client.get("/health")
    assert resp.status_code == 200

# Note: ASGITransport does not trigger startup/shutdown lifespan events by itself.
# Use asgi-lifespan's LifespanManager when the test depends on app lifespan setup.
```

---

## 8. `monkeypatch` — pytest's Built-in Patcher

```python
import pytest
import os

# monkeypatch is a pytest fixture — auto-reverts patches after each test (no cleanup needed)

def test_env_var(monkeypatch):
    # Set environment variable for this test only
    monkeypatch.setenv("DATABASE_URL", "sqlite:///:memory:")
    monkeypatch.setenv("DEBUG", "1")
    assert os.environ["DATABASE_URL"] == "sqlite:///:memory:"
    # Automatically unset after test

def test_delete_env_var(monkeypatch):
    monkeypatch.delenv("SOME_VAR", raising=False)   # raising=False: no error if not set

# Patch a function or attribute
def test_patch_function(monkeypatch):
    def mock_time():
        return 1700000000.0

    monkeypatch.setattr("time.time", mock_time)
    import time
    assert time.time() == 1700000000.0

# Patch on an object
def test_patch_method(monkeypatch):
    import myapp.config as config
    monkeypatch.setattr(config, "DEBUG", True)
    assert config.DEBUG is True

# Patch dict — add/modify items (original entries preserved and restored)
def test_patch_dict(monkeypatch):
    original = {"key": "original"}
    monkeypatch.setitem(original, "key", "patched")
    monkeypatch.setitem(original, "new_key", "new_value")
    assert original["key"] == "patched"
    assert original["new_key"] == "new_value"
    # After test: original == {"key": "original"}

# syspath manipulation — add directory to Python path for test
def test_sys_path(monkeypatch, tmp_path):
    monkeypatch.syspath_prepend(str(tmp_path))
    import sys
    assert str(tmp_path) == sys.path[0]

# monkeypatch vs patch:
# monkeypatch: pytest fixture, cleaner syntax, auto-reverts, great for env/attr/dict
# patch: unittest.mock, works as decorator or context manager, better for inspecting calls
```

---

## 9. Testing FastAPI

### `TestClient` — Synchronous Tests

```python
# tests/conftest.py
import pytest
from fastapi.testclient import TestClient
from myapp.main import app
from myapp.dependencies import get_current_user, get_db

def override_get_db():
    db = get_test_db()
    try:
        yield db
    finally:
        db.close()

def override_get_current_user():
    return {"user_id": 1, "email": "test@example.com", "roles": ["user"]}

@pytest.fixture(scope="module")
def client():
    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user] = override_get_current_user
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()   # Always clean up!

# tests/test_users.py
def test_get_user(client):
    resp = client.get("/users/1")
    assert resp.status_code == 200
    data = resp.json()
    assert "id" in data
    assert "email" in data

def test_create_user(client):
    payload = {"name": "Bob", "email": "bob@example.com", "age": 25}
    resp = client.post("/users", json=payload)
    assert resp.status_code == 201
    data = resp.json()
    assert data["name"] == "Bob"

def test_create_user_invalid(client):
    # Missing required field — expect 422
    resp = client.post("/users", json={"name": "Bob"})
    assert resp.status_code == 422
    errors = resp.json()["detail"]
    assert any(e["loc"][-1] == "email" for e in errors)

def test_unauthorized(client):
    # Clear overrides to test auth
    saved = app.dependency_overrides.copy()
    app.dependency_overrides.clear()
    try:
        resp = client.get("/protected")
        assert resp.status_code == 401
    finally:
        app.dependency_overrides = saved

def test_with_headers(client):
    resp = client.get(
        "/secure",
        headers={"Authorization": "Bearer test-token", "X-Request-ID": "abc123"},
    )
    assert resp.status_code == 200

def test_with_cookies(client):
    resp = client.get("/profile", cookies={"session_id": "test-session"})
    assert resp.status_code == 200
```

### `AsyncClient` — Async Tests for FastAPI

```python
import pytest
import httpx
from httpx import AsyncClient
from myapp.main import app

@pytest.mark.asyncio
async def test_async_endpoint():
    transport = httpx.ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        resp = await client.get("/health")
        assert resp.status_code == 200
        assert resp.json()["status"] == "ok"

# Fixture version
@pytest.fixture
async def async_client():
    transport = httpx.ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

@pytest.mark.asyncio
async def test_create_with_async(async_client):
    resp = await async_client.post(
        "/users",
        json={"name": "Alice", "email": "a@b.com", "age": 30}
    )
    assert resp.status_code == 201

# Note: ASGITransport does not run lifespan events. If your app creates DB pools
# or HTTP clients in lifespan, wrap the app with LifespanManager in the fixture.
```

---

## 10. Testcontainers — Real Infrastructure in Tests

### Must Know

```python
# pip install testcontainers[postgresql,redis]
# Requires Docker running locally or in CI

# Testcontainers spins up REAL Docker containers for tests
# No mocking of DB queries — actual SQL/Redis operations
# Java Testcontainers API is nearly identical

import pytest
from testcontainers.postgres import PostgresContainer
from testcontainers.redis import RedisContainer
import sqlalchemy
import redis

# Session-scoped — one container for entire test session
@pytest.fixture(scope="session")
def postgres():
    with PostgresContainer("postgres:15") as pg:
        yield pg   # pg.get_connection_url() returns the connection URL

@pytest.fixture(scope="session")
def db_engine(postgres):
    engine = sqlalchemy.create_engine(postgres.get_connection_url())
    yield engine
    engine.dispose()

@pytest.fixture
def db_session(db_engine):
    """Fresh session per test — rolls back after each test."""
    with sqlalchemy.orm.Session(db_engine) as session:
        with session.begin():
            yield session
            session.rollback()   # Roll back transaction — no test data persists

def test_insert_user(db_session):
    db_session.execute(
        sqlalchemy.text("INSERT INTO users (name, email) VALUES (:n, :e)"),
        {"n": "Alice", "e": "a@b.com"},
    )
    result = db_session.execute(
        sqlalchemy.text("SELECT * FROM users WHERE name = :n"), {"n": "Alice"}
    ).fetchone()
    assert result is not None
    assert result.name == "Alice"

# Redis container
@pytest.fixture(scope="session")
def redis_client():
    with RedisContainer("redis:7") as rc:
        client = redis.Redis(
            host=rc.get_container_host_ip(),
            port=rc.get_exposed_port(6379),
            decode_responses=True,
        )
        yield client

def test_redis_set_get(redis_client):
    redis_client.set("key:1", "value_one", ex=60)
    assert redis_client.get("key:1") == "value_one"
    assert redis_client.ttl("key:1") > 0
```

### Testcontainers in CI

```yaml
# .github/workflows/test.yml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    # No services: block needed — Testcontainers uses Docker-in-Docker automatically
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: "3.11"
      - run: pip install -e ".[dev]"
      - run: pytest --cov=myapp --cov-report=xml -v
      - uses: codecov/codecov-action@v3

# Testcontainers auto-detects Docker socket in GitHub Actions
# No special configuration needed for most CI providers
```

---

## 11. `pytest-cov` — Coverage

```bash
# pip install pytest-cov

# Run tests with coverage
pytest --cov=myapp                      # Coverage for myapp package
pytest --cov=myapp --cov-report=term    # Report in terminal
pytest --cov=myapp --cov-report=html    # HTML report in htmlcov/
pytest --cov=myapp --cov-report=xml     # XML for CI tools (codecov, SonarQube)

# Fail if coverage below threshold
pytest --cov=myapp --cov-fail-under=80  # Exit code 1 if < 80% coverage

# Show missing lines
pytest --cov=myapp --cov-report=term-missing

# Exclude files from coverage
# .coveragerc or pyproject.toml:
# [tool.coverage.run]
# omit = ["*/tests/*", "*/migrations/*", "*/conftest.py"]
# source = ["myapp"]
#
# [tool.coverage.report]
# fail_under = 80
# exclude_lines = [
#     "pragma: no cover",
#     "def __repr__",
#     "if TYPE_CHECKING:",
#     "raise NotImplementedError",
# ]
```

```python
# pragma: no cover — exclude specific lines/blocks from coverage
def debug_dump(data):   # pragma: no cover
    import json
    print(json.dumps(data, indent=2))

class AbstractBase:
    def process(self):
        raise NotImplementedError   # pragma: no cover
```

---

## 12. `pytest-mock` — Cleaner Mocking

```python
# pip install pytest-mock
# Provides mocker fixture — wrapper around unittest.mock

def test_with_mocker(mocker):
    # mocker.patch — same as patch() but auto-reverts after test (like monkeypatch)
    mock_send = mocker.patch("myapp.services.order.send_email")
    mock_send.return_value = True

    place_order("alice@example.com", "Widget")
    mock_send.assert_called_once_with("alice@example.com", "Order confirmed: Widget")

def test_mock_class(mocker):
    # Mock an entire class
    MockDB = mocker.patch("myapp.services.user_service.Database")
    instance = MockDB.return_value   # The instance returned when Database() is called
    instance.get_user.return_value = {"id": 1, "name": "Alice"}

    service = UserService()
    user = service.get_user(1)
    assert user["name"] == "Alice"

def test_spy(mocker):
    # spy — wraps the real function; lets it run but also records calls
    spy = mocker.spy(math, "sqrt")
    result = math.sqrt(4)
    assert result == 2.0                 # Real function ran
    spy.assert_called_once_with(4)       # Also recorded

def test_stub(mocker):
    # Create a stub without patching a real function
    stub = mocker.stub(name="my_stub")
    stub.return_value = 42
    assert stub() == 42
```

---

## 13. Test Organization Best Practices

```
tests/
├── conftest.py              ← session + module scoped fixtures; DB containers; TestClient
├── unit/
│   ├── conftest.py          ← unit-specific fixtures; no DB
│   ├── test_models.py
│   ├── test_validators.py
│   └── test_services.py
├── integration/
│   ├── conftest.py          ← Testcontainers fixtures; real DB
│   ├── test_api_users.py
│   ├── test_api_orders.py
│   └── test_db_queries.py
└── e2e/
    ├── conftest.py
    └── test_full_flows.py

pyproject.toml:
[tool.pytest.ini_options]
testpaths = ["tests"]
asyncio_mode = "auto"
markers = [
    "unit: fast unit tests",
    "integration: requires DB/cache",
    "slow: takes > 5 seconds",
]
addopts = "--strict-markers -v"

# CI: run all
# Local fast feedback: pytest -m "unit"
# Pre-deploy check: pytest -m "unit or integration"
```

---

## 14. Java Developer Bridge

| Concept | Java (JUnit 5 + Mockito) | Python (pytest + unittest.mock) |
|---|---|---|
| Test framework | JUnit 5 | pytest |
| Test discovery | `@Test` annotation | `test_` prefix in function/file name |
| Setup per test | `@BeforeEach void setUp()` | `@pytest.fixture` (function scope) |
| Teardown per test | `@AfterEach void tearDown()` | `yield` in fixture + cleanup after `yield` |
| Setup once per class | `@BeforeAll static void` | `@pytest.fixture(scope="class")` |
| Setup once per module | N/A (test class is the unit) | `@pytest.fixture(scope="module")` |
| Setup once per session | N/A | `@pytest.fixture(scope="session")` |
| Shared fixtures | `@ExtendWith` + base class | `conftest.py` — auto-discovered |
| Parameterized test | `@ParameterizedTest @CsvSource` | `@pytest.mark.parametrize` |
| Assert throws | `assertThrows(Exc.class, () -> ...)` | `with pytest.raises(Exc):` |
| Assert float | `assertEquals(expected, actual, delta)` | `assert actual == pytest.approx(expected)` |
| Skip test | `@Disabled` | `@pytest.mark.skip` |
| Expected failure | `@Disabled` or test ignoring | `@pytest.mark.xfail` |
| Category/tag filter | `@Tag("integration")` | `@pytest.mark.integration` + `-m` |
| Mocking library | Mockito | `unittest.mock` / `pytest-mock` |
| Create mock | `Mockito.mock(Service.class)` | `MagicMock(spec=Service)` / `create_autospec(Service)` |
| Stub return value | `when(mock.method()).thenReturn(val)` | `mock.method.return_value = val` |
| Verify call | `verify(mock).method(args)` | `mock.method.assert_called_once_with(args)` |
| Raise exception | `when(mock.m()).thenThrow(exc)` | `mock.method.side_effect = Exception(...)` |
| Spy (wrap real) | `Mockito.spy(realObject)` | `mocker.spy(obj, "method_name")` |
| Capture arguments | `ArgumentCaptor` | `mock.call_args_list` |
| Inject mock | `@InjectMocks` | Constructor injection or `patch()` |
| Patch env vars | `EnvironmentVariables` rule | `monkeypatch.setenv()` |
| Real DB test | Spring `@DataJpaTest` + H2 | `testcontainers` + real Postgres |
| REST test (sync) | `MockMvc.perform(get("/"))` | `TestClient(app).get("/")` |
| REST test (async) | `WebTestClient.get("/")` | `AsyncClient(transport=ASGITransport(app=app)).get("/")` |
| Override DI | `@MockBean` | `app.dependency_overrides[dep] = mock_dep` |
| Coverage | JaCoCo | `pytest-cov` |

---

## 15. Hot Interview Q&A

**Q: What is a pytest fixture and how is it different from JUnit's `@BeforeEach`?**  
A: A pytest fixture is a function decorated with `@pytest.fixture` that test functions declare as parameters — pytest injects the return value. Key differences: (1) Scope — fixtures can be function, class, module, or session-scoped; JUnit has only `@BeforeEach` (per test) and `@BeforeAll` (per class). (2) Composability — fixtures can depend on other fixtures, creating dependency chains automatically. (3) Setup + teardown in one function — `yield` splits setup (before) from teardown (after). (4) Shared discovery — `conftest.py` makes fixtures available to all tests in the directory without any import or inheritance. (5) DI-like — multiple test functions can use the same fixture independently without inheritance.

**Q: What is the "where to patch" rule and why does it matter?**  
A: Always patch where the name is *used*, not where it is *defined*. When you do `from myapp.email import send_email` in `myapp.order`, Python copies the reference to `send_email` into `myapp.order`'s namespace. Patching `myapp.email.send_email` replaces it in the email module, but `myapp.order.send_email` still points to the original. Patching `myapp.order.send_email` replaces it in the namespace where the code actually runs. This is the single most common mocking bug in Python — Java's `@Mock` + `@InjectMocks` avoids this by working through Spring's DI container, which always redirects through the same reference.

**Q: What is the difference between `Mock`, `MagicMock`, and `AsyncMock`?**  
A: `Mock` is the base class — all attribute access and method calls return new `Mock` objects. `MagicMock` extends `Mock` with implementations of all magic methods (`__len__`, `__iter__`, `__str__`, `__enter__`, `__exit__`, etc.) — use `MagicMock` for objects used in `with` statements, `len()`, `for` loops, etc. `AsyncMock` (Python 3.8+) is for mocking coroutine functions — its return value is awaitable; calling `await mock()` works correctly. Using a regular `MagicMock` where an `AsyncMock` is needed raises `TypeError: object MagicMock can't be used in 'await' expression`.

**Q: How do you test a FastAPI endpoint that has dependencies like a DB session?**  
A: Use `app.dependency_overrides` to replace the real dependency with a test double. Before the test (in a fixture), set `app.dependency_overrides[get_db] = get_test_db` where `get_test_db` returns an in-memory SQLite session or a Testcontainers PostgreSQL session. After the test, call `app.dependency_overrides.clear()`. The `TestClient` then exercises the full middleware + routing + validation stack with the fake database injected. This is equivalent to Spring Boot's `@MockBean` or `@DataJpaTest` with replaced repository beans.

**Q: When would you use Testcontainers instead of mocking the database?**  
A: Testcontainers is preferred when: (1) You want to test actual SQL behavior — raw SQL queries, transactions, constraints, stored procedures — which mocks cannot replicate. (2) Testing ORM mappings — SQLAlchemy/Alembic migration correctness requires a real DB. (3) Testing Redis pipelines, Lua scripts, or cluster behavior. (4) Integration tests that need the full stack. Use mocks when: testing service layer logic in isolation (the DB call is an implementation detail); the test should be fast (Testcontainers adds ~5-10s startup per container); you're testing error handling that would require complicated DB state setup.

**Q: How does `@pytest.mark.parametrize` compare to JUnit's `@ParameterizedTest`?**  
A: Both generate multiple test cases from a data table. pytest's `parametrize` is more concise — a single decorator with inline data. JUnit requires separate `@CsvSource`, `@MethodSource`, or `@ValueSource` annotations. pytest's `pytest.param()` lets you mark individual cases as `xfail` or `skip` inline. Stacking two `@parametrize` decorators creates a cartesian product — all combinations — which JUnit requires `@MethodSource` with explicit cross-product logic for. pytest IDs are auto-generated from values, making test names readable without extra annotation.

**Q: How do you test async endpoints in FastAPI?**  
A: Two approaches: (1) `TestClient` from `fastapi.testclient` — wraps the ASGI app in a synchronous client using `anyio`. The test function is a normal `def` (not async). This covers most cases — it exercises the full async path internally but tests are written synchronously. (2) `httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test")` with `@pytest.mark.asyncio` — the test function is `async def` and uses `await`. Required when testing streaming responses or when you need to await other async fixtures alongside the request. HTTPX's `ASGITransport` does not trigger startup/shutdown lifespan events by itself, so use `asgi-lifespan`'s `LifespanManager` when the test depends on lifespan resources. Always clear `dependency_overrides` after the test to prevent state leakage between test modules.

---

## 16. Final Revision Checklist

### pytest Core

- [ ] I know pytest discovers `test_*.py` files and `test_` functions/methods automatically
- [ ] I use `assert` directly — pytest rewrites it to show actual vs expected values
- [ ] I use `pytest.approx()` for float comparisons — not `==` with floats directly
- [ ] I use `with pytest.raises(Exc, match="pattern"):` for exception testing

### Fixtures

- [ ] I declare fixtures as function parameters — pytest injects them by name
- [ ] I use `yield` in fixtures for setup/teardown — teardown always runs (like try/finally)
- [ ] I know the four scopes: `function` (default), `class`, `module`, `session`
- [ ] I put shared fixtures in `conftest.py` — no import needed, auto-discovered

### Parametrize and Markers

- [ ] I use `@pytest.mark.parametrize("a,b,expected", [...])` for data-driven tests
- [ ] I use `ids=["name1","name2"]` or `pytest.param(..., id="name")` for readable names
- [ ] I know `@pytest.mark.skip`, `@pytest.mark.xfail`, `@pytest.mark.skipif`
- [ ] I use `-m "not slow"` and `-k "user"` for test filtering

### Mocking

- [ ] I know the "patch where used" rule — patch `mymodule.name`, not `original_module.name`
- [ ] I use `MagicMock(spec=MyClass)` — not bare `MagicMock()` — for type-safe mocks
- [ ] I use `AsyncMock` for mocking coroutine functions — `MagicMock` cannot be awaited
- [ ] I use `mock.assert_called_once_with(...)` and `mock.assert_awaited_once_with(...)` for verification
- [ ] I use `side_effect` for exceptions or successive return values

### FastAPI Testing

- [ ] I use `app.dependency_overrides[dep] = mock_dep` to inject test doubles
- [ ] I always call `app.dependency_overrides.clear()` after the test (ideally in fixture teardown)
- [ ] I use `TestClient` for sync tests; `httpx.AsyncClient` + `ASGITransport` + `pytest-asyncio` for async tests
- [ ] I know `ASGITransport` does not run lifespan events by itself; use `LifespanManager` when startup resources matter

### Testcontainers and Coverage

- [ ] I use `testcontainers[postgresql]` for integration tests that need real SQL
- [ ] I scope Testcontainers fixtures as `session` to avoid restarting containers per test
- [ ] I use `pytest --cov=myapp --cov-fail-under=80` for coverage gate in CI
- [ ] I add `# pragma: no cover` to debug/unreachable branches

### Java Developer Reminders

- [ ] `@BeforeEach` → `@pytest.fixture(scope="function")` with `yield`
- [ ] `@BeforeAll` → `@pytest.fixture(scope="session")`
- [ ] `conftest.py` → Spring `@ExtendWith` + abstract test base class
- [ ] `@MockBean` → `app.dependency_overrides[dep] = mock`
- [ ] `Mockito.when(m.f()).thenReturn(v)` → `mock.f.return_value = v`
- [ ] `verify(m).f(args)` → `mock.f.assert_called_once_with(args)`

---

*File 4 of 4 — Group 3: Senior MAANG — GROUP 3 COMPLETE*  
*Next Group: 04-Scenario-Practice — MAANG-level scenario drill bank files*
