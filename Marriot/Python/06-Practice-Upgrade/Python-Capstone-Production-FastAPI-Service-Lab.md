# Python Capstone: Production FastAPI Service Lab

> **Track File #32 - Group 6: Practice Upgrade**
> For: beginner-to-pro consolidation | Level: build, test, observe, secure, and explain

---

## 1. Goal

Build a small but production-shaped Python backend service.

The capstone forces these topics to work together:

- uv project setup
- FastAPI
- Pydantic v2
- SQLAlchemy async
- Alembic migrations
- pytest
- Testcontainers
- structured logging
- OpenTelemetry-ready tracing
- metrics
- security basics
- idempotency
- time/money correctness
- Docker/CI thinking
- interview explanation

The target product:

```text
Order Service
```

Core use cases:

- create order
- fetch order
- cancel order
- list customer orders
- idempotent create-order retry

---

## 2. Architecture

```text
HTTP API
  -> Pydantic request/response schemas
  -> service layer
  -> domain model
  -> repository protocol
  -> SQLAlchemy async repository
  -> Postgres
```

Recommended layout:

```text
order-service/
    pyproject.toml
    uv.lock
    app/
        main.py
        api/
            routes.py
            dependencies.py
            exception_handlers.py
        core/
            config.py
            logging.py
            middleware.py
            security.py
        domain/
            models.py
            errors.py
            protocols.py
        schemas/
            orders.py
        services/
            order_service.py
        repositories/
            sqlalchemy_orders.py
        infrastructure/
            db.py
            telemetry.py
        workers/
            outbox_worker.py
    tests/
        unit/
        integration/
        api/
    alembic/
        versions/
```

---

## 3. Setup

```bash
uv init order-service
cd order-service
uv python pin 3.14
uv add fastapi uvicorn sqlalchemy asyncpg alembic pydantic pydantic-settings httpx
uv add --dev pytest pytest-asyncio pytest-cov testcontainers ruff mypy
```

Baseline commands:

```bash
uv run python -VV
uv run ruff check .
uv run mypy app
uv run pytest
```

---

## 4. Domain Model

```python
from dataclasses import dataclass
from datetime import UTC, datetime
from enum import StrEnum
from uuid import UUID


class OrderStatus(StrEnum):
    CREATED = "CREATED"
    CANCELLED = "CANCELLED"


@dataclass(frozen=True)
class Money:
    amount_cents: int
    currency: str

    def __post_init__(self) -> None:
        if self.amount_cents <= 0:
            raise ValueError("amount must be positive")
        if len(self.currency) != 3:
            raise ValueError("currency must be ISO-4217 style code")


@dataclass
class Order:
    id: UUID
    customer_id: UUID
    sku: str
    quantity: int
    total: Money
    status: OrderStatus
    created_at: datetime

    def cancel(self) -> None:
        if self.status == OrderStatus.CANCELLED:
            return
        self.status = OrderStatus.CANCELLED


def utc_now() -> datetime:
    return datetime.now(UTC)
```

Learning target:

- domain model does not import FastAPI
- money is not float
- time is timezone-aware UTC
- status transition is explicit

---

## 5. API Schemas

```python
from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field


class CreateOrderRequest(BaseModel):
    customer_id: UUID
    sku: str = Field(min_length=1, max_length=64)
    quantity: int = Field(gt=0, le=100)
    amount_cents: int = Field(gt=0)
    currency: str = Field(min_length=3, max_length=3)


class OrderResponse(BaseModel):
    id: UUID
    customer_id: UUID
    sku: str
    quantity: int
    amount_cents: int
    currency: str
    status: str
    created_at: datetime
```

Learning target:

- Pydantic validates external data.
- Domain still owns business invariants.
- Response schema is explicit.

---

## 6. Repository Protocol

```python
from typing import Protocol
from uuid import UUID

from app.domain.models import Order


class OrderRepository(Protocol):
    async def save(self, order: Order) -> None:
        ...

    async def get(self, order_id: UUID) -> Order | None:
        ...

    async def list_by_customer(self, customer_id: UUID) -> list[Order]:
        ...
```

Learning target:

- service depends on protocol, not SQLAlchemy directly
- unit tests can use fake repository

---

## 7. Service Layer

```python
from uuid import UUID, uuid4

from app.domain.models import Money, Order, OrderStatus, utc_now
from app.domain.protocols import OrderRepository


class OrderNotFound(Exception):
    pass


class OrderService:
    def __init__(self, repository: OrderRepository) -> None:
        self.repository = repository

    async def create_order(
        self,
        customer_id: UUID,
        sku: str,
        quantity: int,
        amount_cents: int,
        currency: str,
    ) -> Order:
        order = Order(
            id=uuid4(),
            customer_id=customer_id,
            sku=sku,
            quantity=quantity,
            total=Money(amount_cents, currency.upper()),
            status=OrderStatus.CREATED,
            created_at=utc_now(),
        )
        await self.repository.save(order)
        return order

    async def cancel_order(self, order_id: UUID) -> Order:
        order = await self.repository.get(order_id)
        if order is None:
            raise OrderNotFound(str(order_id))
        order.cancel()
        await self.repository.save(order)
        return order
```

Learning target:

- route handlers stay thin
- service owns use cases
- repository owns persistence

---

## 8. Idempotency Requirement

Create order must accept:

```text
Idempotency-Key: <uuid>
```

Behavior:

| Situation | Result |
|---|---|
| first request with key | create order and store response |
| retry same body same key | return stored response |
| same key different body | return 409 conflict |
| missing key | return 400 or require client contract |

Database table:

```text
idempotency_keys
    key
    request_hash
    response_body
    status_code
    created_at
```

Learning target:

- retries are safe
- duplicate orders are prevented
- distributed systems correctness enters Python backend design

---

## 9. Database and Alembic

Minimum tables:

```text
orders
    id UUID primary key
    customer_id UUID not null
    sku text not null
    quantity int not null check quantity > 0
    amount_cents bigint not null check amount_cents > 0
    currency char(3) not null
    status text not null
    created_at timestamptz not null

idempotency_keys
    key UUID primary key
    request_hash text not null
    response_body jsonb not null
    status_code int not null
    created_at timestamptz not null
```

Learning target:

- invariants are enforced in app and DB
- migrations are versioned
- async SQLAlchemy session lifecycle is tested

---

## 10. FastAPI Routes

Routes:

```text
POST   /v1/orders
GET    /v1/orders/{order_id}
POST   /v1/orders/{order_id}/cancel
GET    /v1/customers/{customer_id}/orders
```

Route principle:

```python
@router.post("/orders", response_model=OrderResponse, status_code=201)
async def create_order(
    request: CreateOrderRequest,
    idempotency_key: UUID = Header(alias="Idempotency-Key"),
    service: OrderService = Depends(get_order_service),
) -> OrderResponse:
    order = await service.create_order(...)
    return OrderResponse(...)
```

Learning target:

- HTTP translation only in route
- request validation in schema
- use case in service
- persistence behind repository

---

## 11. Testing Plan

### Unit Tests

Test:

- money rejects invalid amount
- service creates order
- cancel is idempotent
- repository protocol can be faked

### API Tests

Test:

- create order returns 201
- invalid quantity returns 422
- missing idempotency key returns 400/422 depending design
- retry with same key returns same order
- same key different body returns 409

### Integration Tests

Use Testcontainers Postgres:

- migrations apply
- repository saves and loads order
- constraints reject invalid row
- transaction rollback behavior is correct

---

## 12. Observability Requirements

Add:

- structured request logs
- request ID middleware
- p95 latency metric
- request count by route/status
- DB query timing
- trace ID in logs
- health endpoints

Minimum incident question the service must answer:

```text
For request_id X, which route ran, how long did it take, which DB calls happened,
and what error occurred?
```

---

## 13. Security Requirements

Add:

- request size limit
- no secrets in logs
- explicit CORS if browser-facing
- parameterized SQL/SQLAlchemy expressions
- dependency audit in CI
- auth placeholder or API key dependency
- object-level authorization note for customer order listing

Security tests:

- path values do not become raw SQL
- invalid UUIDs fail validation
- idempotency key mismatch returns conflict
- logs do not include authorization header

---

## 14. CI Gates

Minimum:

```bash
uv sync --frozen
uv run ruff check .
uv run mypy app
uv run pytest --cov=app
uv run pip-audit
```

Optional:

```bash
uv run bandit -r app
```

---

## 15. Docker Runtime Checklist

- non-root user
- lockfile install
- no reload in production
- explicit host/port
- health check
- graceful shutdown
- environment-based config

Example command:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

Worker count depends on:

- CPU cores
- memory per worker
- blocking vs async workload
- DB pool capacity
- container limits

---

## 16. Completion Rubric

| Level | Evidence |
|---|---|
| Beginner complete | app runs, endpoints work, unit tests pass |
| Intermediate complete | DB persistence, Alembic, API tests, clean layering |
| Senior complete | idempotency, observability, security checks, CI gates |
| MAANG complete | can explain trade-offs, failure modes, concurrency, pool sizing, and incident debugging |

---

## 17. Interview Walkthrough

Use this 90-second answer:

> I built the service with FastAPI routes as thin HTTP adapters, Pydantic schemas for boundary validation, a service layer for use cases, domain models for invariants, and a repository protocol backed by async SQLAlchemy. I used timezone-aware UTC timestamps and integer minor units for money. Create order is idempotent with an `Idempotency-Key`, so client retries do not duplicate orders. Tests are split into unit tests with fakes, API tests, and Testcontainers integration tests. Production readiness includes structured logs with request IDs, metrics, traces, health checks, dependency scanning, and CI gates. For scaling, I would tune Uvicorn workers, DB pool size, and downstream timeouts based on load test data instead of guessing.

---

## 18. Failure Modes To Explain

| Failure | Expected Design Response |
|---|---|
| DB unavailable | readiness fails, requests return controlled 503/500, logs/traces show DB error |
| duplicate client retry | idempotency returns original response |
| same idempotency key with different body | 409 conflict |
| slow DB query | trace span and DB metric expose latency |
| event loop blocked | event loop lag and py-spy reveal sync call |
| invalid payload | Pydantic returns 422 |
| bad auth/resource access | dependency returns 401/403 |

---

## 19. Extension Challenges

Add one at a time:

- outbox table for order-created events
- background worker with retry and dead-letter behavior
- Redis cache for read-heavy customer order list
- OpenTelemetry exporter
- Prometheus metrics endpoint
- property-based tests for money and idempotency
- load test with `k6` or `hey`
- deployment manifest with readiness/liveness probes

---

## 20. Revision Notes

- One-line summary: the capstone proves you can build Python, not just answer Python.
- Three keywords: layering, idempotency, observability.
- One interview trap: writing a FastAPI demo with business logic directly inside route handlers.
- One memory trick: route adapts, service decides, repository persists, telemetry explains.
