# Python Testing: pytest, Coverage, Testcontainers, and FastAPI Testing — Gold Sheet

> Topic: pytest fixtures, parametrize, async testing, FastAPI test client, coverage.py, Testcontainers

---

## 1. Intuition

Python testing with pytest is expressive and composable. The same test tools work for unit tests (no I/O), integration tests (real database), and API tests (FastAPI test client). The key is structuring fixtures to control setup/teardown at the right scope — function, module, or session.

Beginner version:

> pytest fixtures are reusable setup/teardown that you inject into tests like function arguments.

---

## 2. Definition

- Definition: Python backend testing uses pytest fixtures for composable test setup, `coverage.py` for measuring code coverage, Testcontainers for real-infrastructure integration tests, and FastAPI's `TestClient` / `httpx.AsyncClient` for HTTP-layer tests.
- Category: Python backend quality engineering.
- Core idea: fixture scope determines test speed; use real databases for integration tests.

---

## 3. pytest Basics

```python
# tests/test_order_service.py
import pytest
from decimal import Decimal
from app.services.order_service import OrderService
from app.models import Order, OrderStatus


def test_calculate_total_with_discount():
    order = Order(items=[{"sku": "ITEM-1", "price": Decimal("100.00"), "qty": 2}])
    total = OrderService.calculate_total(order, discount_pct=0.10)
    assert total == Decimal("180.00")


def test_raises_when_discount_exceeds_100_percent():
    order = Order(items=[{"sku": "ITEM-1", "price": Decimal("50.00"), "qty": 1}])
    with pytest.raises(ValueError, match="Discount cannot exceed 100%"):
        OrderService.calculate_total(order, discount_pct=1.5)
```

---

## 4. Fixtures — Composable Setup

```python
# tests/conftest.py — shared fixtures across all test files
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.database import Base


@pytest.fixture
def sample_order():
    """Simple order for unit tests — no database."""
    return Order(
        customer_id="CUST-1",
        items=[{"sku": "ITEM-1", "price": Decimal("50.00"), "qty": 2}],
    )


@pytest.fixture(scope="module")   # created once per test module
def db_engine():
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    yield engine
    Base.metadata.drop_all(engine)


@pytest.fixture
def db_session(db_engine):        # created fresh for each test
    Session = sessionmaker(bind=db_engine)
    session = Session()
    yield session
    session.rollback()            # roll back after each test → isolated
    session.close()
```

**Fixture scopes:**

| Scope | Created | Destroyed | Use For |
|---|---|---|---|
| `function` (default) | Before each test | After each test | Isolated per-test setup |
| `class` | Before first test in class | After last test in class | Shared per class |
| `module` | Before first test in module | After last test in module | DB connections per file |
| `session` | Once per pytest run | End of pytest run | Docker containers, expensive setup |

---

## 5. Parametrize — Data-Driven Tests

```python
@pytest.mark.parametrize("discount,expected_total", [
    (0.0,  Decimal("100.00")),
    (0.10, Decimal("90.00")),
    (0.25, Decimal("75.00")),
    (1.0,  Decimal("0.00")),
])
def test_discount_calculation(sample_order, discount, expected_total):
    total = OrderService.calculate_total(sample_order, discount_pct=discount)
    assert total == expected_total


@pytest.mark.parametrize("status", [OrderStatus.PAID, OrderStatus.CANCELLED])
def test_cannot_modify_finalized_order(status, sample_order):
    sample_order.status = status
    with pytest.raises(OrderError, match="Order is finalized"):
        OrderService.add_item(sample_order, "ITEM-2")
```

---

## 6. pytest-asyncio — Testing FastAPI Async Code

```python
# pyproject.toml
[tool.pytest.ini_options]
asyncio_mode = "auto"   # auto-apply asyncio to all async tests
```

```python
# tests/test_order_service_async.py
import pytest
from app.services.order_service import AsyncOrderService


async def test_create_order_async(db_session):
    service = AsyncOrderService(db_session)
    order = await service.create_order(customer_id="CUST-1", items=[...])
    assert order.id is not None
    assert order.status == OrderStatus.PENDING
```

---

## 7. FastAPI TestClient — Sync

```python
# tests/test_orders_api.py
import pytest
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, patch
from app.main import app


@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c


def test_create_order_returns_201(client):
    response = client.post("/orders", json={
        "customer_id": "CUST-1",
        "items": [{"sku": "ITEM-1", "price": 50.0, "qty": 2}]
    })
    assert response.status_code == 201
    assert response.json()["status"] == "PENDING"


def test_get_order_returns_404_when_missing(client):
    response = client.get("/orders/00000000-0000-0000-0000-000000000000")
    assert response.status_code == 404


def test_create_order_validates_items(client):
    response = client.post("/orders", json={"customer_id": "CUST-1", "items": []})
    assert response.status_code == 422          # Pydantic validation error
    assert "items" in response.json()["detail"][0]["loc"]
```

---

## 8. httpx.AsyncClient — Async FastAPI Tests

```python
import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app


@pytest.fixture
async def async_client():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as c:
        yield c


async def test_get_orders_returns_list(async_client):
    response = await async_client.get("/orders?customer_id=CUST-1")
    assert response.status_code == 200
    assert isinstance(response.json(), list)
```

---

## 9. Testcontainers for Python

```python
# tests/conftest.py — PostgreSQL for integration tests
import pytest
from testcontainers.postgres import PostgresContainer
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.database import Base


@pytest.fixture(scope="session")  # one container for the entire test session
def postgres_container():
    with PostgresContainer("postgres:16") as pg:
        yield pg


@pytest.fixture(scope="session")
def db_engine(postgres_container):
    engine = create_engine(postgres_container.get_connection_url())
    Base.metadata.create_all(engine)
    yield engine
    Base.metadata.drop_all(engine)


@pytest.fixture
def db_session(db_engine):
    Session = sessionmaker(bind=db_engine)
    session = Session()
    yield session
    session.rollback()
    session.close()
```

```bash
pip install testcontainers[postgres]
```

---

## 10. Coverage.py Configuration

```toml
# pyproject.toml
[tool.coverage.run]
source = ["app"]                          # only measure app code
omit = ["app/migrations/*", "tests/*"]
branch = true                             # measure branch coverage too

[tool.coverage.report]
fail_under = 80                           # fail if coverage drops below 80%
show_missing = true
exclude_lines = [
    "pragma: no cover",
    "if TYPE_CHECKING:",
    "raise NotImplementedError",
]
```

```bash
# Run tests with coverage
pytest --cov=app --cov-report=xml --cov-report=term-missing

# CI: generate XML for SonarQube/Codecov import
pytest --cov=app --cov-report=xml:coverage.xml
```

---

## 11. Mocking External Dependencies

```python
# tests/test_payment_service.py
from unittest.mock import AsyncMock, patch, MagicMock


async def test_process_payment_calls_gateway():
    mock_gateway = AsyncMock()
    mock_gateway.charge.return_value = {"charge_id": "CHG-123", "success": True}

    service = PaymentService(gateway=mock_gateway)
    result = await service.process_payment(order_id="ORD-1", amount=100.0)

    mock_gateway.charge.assert_called_once_with(amount=100.0, currency="USD")
    assert result.charge_id == "CHG-123"


async def test_process_payment_raises_on_gateway_failure():
    mock_gateway = AsyncMock()
    mock_gateway.charge.side_effect = GatewayTimeoutError("timeout after 5s")

    service = PaymentService(gateway=mock_gateway)
    with pytest.raises(PaymentFailedError, match="timeout"):
        await service.process_payment(order_id="ORD-1", amount=100.0)


# Patching at the usage site (not the definition site)
async def test_send_confirmation_email():
    with patch("app.services.payment_service.EmailClient.send") as mock_send:
        mock_send.return_value = None
        await PaymentService().send_confirmation("user@example.com", "ORD-1")
        mock_send.assert_called_once()
```

---

## 12. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| `scope="function"` for database container | New container per test — slow (5-10s per test) | `scope="session"` for containers; rollback per test |
| Not rolling back after each test | Test data leaks between tests, order-dependent failures | `session.rollback()` in fixture teardown |
| Patching at definition site | Mock doesn't take effect | Patch at the module where the name is used, not where it's defined |
| `TestClient` for async lifespan events | Lifespan (startup/shutdown) not triggered | Use `async with AsyncClient(transport=ASGITransport(app=app))` |
| Testing only happy paths | Gaps in error handling | Explicitly test 4xx responses, timeout paths, validation errors |

---

## 13. Interview Insight

Strong answer:

> I use pytest fixtures for composable test setup, scoped appropriately — `session` scope for expensive resources like Testcontainers database containers, `function` scope for database sessions so each test gets a clean rollback. For FastAPI, `TestClient` works for sync tests and `httpx.AsyncClient` with `ASGITransport` is the right approach for async-aware tests that need lifespan events to run. Testcontainers in Python eliminates the SQLite-vs-PostgreSQL mismatch problem — tests run against the same database engine as production. I configure `branch=true` in coverage.py to measure branch coverage, not just line coverage.

Follow-up trap:

> Why is patching at the definition site wrong?

Good answer:

> Python's `patch` replaces the name in a specific module's namespace. If `payment_service.py` does `from app.email import EmailClient` and I patch `app.email.EmailClient`, the payment_service module already has its own reference to the original `EmailClient` — the patch doesn't affect it. I must patch `app.services.payment_service.EmailClient` — the name as it exists in the module under test.

---

## 14. Revision Notes

- One-line summary: pytest fixtures compose test setup by scope; Testcontainers + AsyncClient give real-infrastructure FastAPI integration tests.
- Three keywords: fixture, scope, rollback.
- One interview trap: patch at the usage site, not the definition site.
- Memory trick: Fixtures are lego bricks — snap them together in each test.
