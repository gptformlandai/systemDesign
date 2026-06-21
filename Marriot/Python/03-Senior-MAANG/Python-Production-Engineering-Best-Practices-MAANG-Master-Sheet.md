# Python Production Engineering & Enterprise FastAPI - MAANG Master Sheet

> **Track File #18 of 31 - Group 3: Senior MAANG**
> For: Java developer | Level: enterprise backend Python | Mode: build MVC-style FastAPI services with production discipline

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters For Java Developers |
|---|---|---|
| FastAPI layered architecture | Very high | Spring MVC maps well, but Python layering is convention-driven |
| Router -> service -> repository flow | Very high | Python has no enforced package boundaries; you must design them |
| Pydantic schemas vs domain models | Very high | Similar to DTO vs entity, but runtime validation differs |
| Dependency injection with `Depends` | Very high | Similar goal to Spring DI; different lifecycle and scope model |
| SQLAlchemy session lifecycle | High | Similar ORM problems to JPA/Hibernate; async session handling is different |
| Alembic migrations | High | Liquibase/Flyway equivalent in Python stacks |
| Configuration and secrets | High | No Spring Boot auto-configuration; explicit settings pattern matters |
| Logging and correlation IDs | High | Need structured logs and request context for production debugging |
| Timeouts, retries, connection pools | Very high | Common senior system reliability questions |
| Middleware and exception handlers | High | Equivalent to filters/interceptors and `@ControllerAdvice` |
| Testing layers | Very high | Service tests, API tests, repository integration tests |
| Packaging/deployment | Medium | Uvicorn/Gunicorn/container startup differs from Java fat JARs |

**MAANG signal:** You can design a Python service that looks small in code but has clear boundaries: API layer is thin, service layer owns business logic, repository layer owns persistence, infrastructure layer owns external systems, tests can replace dependencies.

---

## 2. FastAPI as MVC-Style Architecture

### Must Know

FastAPI is not MVC in the old server-rendered sense. For JSON backend services, the closest enterprise mapping is:

```text
Spring MVC / Clean Architecture       FastAPI Python Service
-------------------------------------------------------------------
Controller                            APIRouter endpoint function
Request DTO                           Pydantic request schema
Response DTO                          Pydantic response schema
Service                               plain Python service class/function
Repository / DAO                      Protocol + SQLAlchemy implementation
Entity / Domain Model                 dataclass/domain object or ORM model
Bean Validation                       Pydantic validation + service invariants
@ControllerAdvice                     exception handlers
Filter / Interceptor                  middleware
Application properties                pydantic-settings / env vars
```

### Core Principle

The route handler should be thin.

Bad route:

```python
@app.post("/orders")
async def create_order(request: CreateOrderRequest):
    # parses request
    # validates business rules
    # opens DB session
    # calls payment gateway
    # mutates domain state
    # writes DB
    # builds response
    # catches every exception
    ...
```

Better route:

```python
@router.post("/orders", response_model=OrderResponse, status_code=201)
async def create_order(
    request: CreateOrderRequest,
    service: OrderService = Depends(get_order_service),
) -> OrderResponse:
    order = await service.create_order(request.to_command())
    return OrderResponse.from_domain(order)
```

The endpoint translates HTTP to application command and delegates.

### Interview Answer

> In an enterprise FastAPI service, I treat routers like controllers: they should parse HTTP input, call the service layer, and map domain results to response schemas. Business rules live in the service/domain layer, not in route functions. Persistence lives behind repositories. This gives me testability, clear dependency direction, and Spring-like separation without needing a heavy framework container.

---

## 3. Enterprise Project Layout

### Recommended Layout

```text
app/
    main.py                     # FastAPI app factory / composition root
    api/
        __init__.py
        v1/
            routes.py           # include_router composition
            orders.py           # APIRouter for /orders
            users.py
        dependencies.py         # get_db, get_services, auth dependencies
        exception_handlers.py
    core/
        config.py               # Settings from environment
        logging.py              # structured logging setup
        security.py             # auth/token helpers
        middleware.py           # request id, timing, CORS, auth middleware
    domain/
        models.py               # dataclass entities/value objects
        errors.py               # domain exceptions
        protocols.py            # Repository/Gateway Protocols
    schemas/
        order.py                # Pydantic request/response models
        user.py
    services/
        order_service.py        # business use cases
        user_service.py
    repositories/
        order_repository.py     # SQLAlchemy repository
        user_repository.py
    infrastructure/
        db.py                   # engine/session setup
        clients/
            payment_client.py
            email_client.py
    tests/
        unit/
        integration/
        api/
    alembic/
        versions/
```

### Dependency Direction

```text
api -> services -> domain protocols
repositories -> domain models/protocols
infrastructure -> external systems
main.py wires concrete implementations
```

Avoid reverse imports:

```text
BAD: service imports FastAPI Request
BAD: domain imports SQLAlchemy Session
BAD: repository imports APIRouter
BAD: domain model imports Pydantic request schema
```

### Why This Matters

If business logic imports framework objects everywhere, every unit test becomes an API test. Keep business logic plain Python.

---

## 4. App Factory and Composition Root

### Why Use an App Factory

An app factory makes startup testable and configurable.

```python
# app/main.py
from fastapi import FastAPI
from app.api.v1.routes import api_router
from app.api.exception_handlers import register_exception_handlers
from app.core.config import Settings, get_settings
from app.core.logging import configure_logging
from app.core.middleware import register_middleware


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()
    configure_logging(settings)

    app = FastAPI(
        title=settings.service_name,
        version=settings.version,
        docs_url="/docs" if settings.enable_docs else None,
    )

    app.state.settings = settings
    register_middleware(app, settings)
    register_exception_handlers(app)
    app.include_router(api_router, prefix="/api/v1")
    return app


app = create_app()
```

### Java Developer Bridge

This is similar to Spring Boot's application context bootstrap, but explicit. Python does not scan packages for beans by default. You decide where objects are created and how dependencies are wired.

### Interview Answer

> I prefer an app factory because it makes the service easy to test with different settings and dependency overrides. `main.py` acts as the composition root: it wires routers, middleware, exception handlers, settings, and infrastructure.

---

## 5. Pydantic Schemas vs Domain Models

### Must Know

Do not use one class for everything.

```text
Pydantic schema:
  - validates external input
  - controls API response shape
  - may contain field aliases and serialization rules

Domain model:
  - represents business state and invariants
  - should not depend on HTTP or database details

ORM model:
  - maps to database tables
  - persistence concern, not necessarily your domain model
```

### Example

```python
# schemas/order.py
from pydantic import BaseModel, Field
from uuid import UUID

class CreateOrderItemRequest(BaseModel):
    product_id: UUID
    quantity: int = Field(gt=0, le=100)

class CreateOrderRequest(BaseModel):
    user_id: UUID
    items: list[CreateOrderItemRequest]

    def to_command(self) -> "CreateOrderCommand":
        return CreateOrderCommand(
            user_id=str(self.user_id),
            items=[{"product_id": str(i.product_id), "quantity": i.quantity} for i in self.items],
        )

class OrderResponse(BaseModel):
    id: UUID
    status: str
    total_cents: int
```

```python
# domain/models.py
from dataclasses import dataclass

@dataclass
class OrderItem:
    product_id: str
    quantity: int
    unit_price_cents: int

    def subtotal_cents(self) -> int:
        return self.quantity * self.unit_price_cents

@dataclass
class Order:
    id: str
    user_id: str
    items: list[OrderItem]
    status: str = "PENDING"

    def total_cents(self) -> int:
        return sum(item.subtotal_cents() for item in self.items)

    def confirm(self) -> None:
        if not self.items:
            raise ValueError("Cannot confirm empty order")
        self.status = "CONFIRMED"
```

### Java Developer Bridge

| Java/Spring | FastAPI/Python |
|---|---|
| Request DTO | Pydantic request model |
| Response DTO | Pydantic response model |
| JPA Entity | SQLAlchemy ORM model |
| Domain entity | dataclass/plain Python class |
| Bean Validation | Pydantic `Field`, validators, service invariants |

### Trap

Do not let Pydantic schemas become your entire domain model. API schemas change because clients change. Domain models change because business rules change. Those are different reasons.

---

## 6. Dependency Injection with FastAPI

### Must Know

`Depends()` is FastAPI's dependency system. It is function-based, explicit, and request-aware.

```python
# api/dependencies.py
from collections.abc import AsyncIterator
from sqlalchemy.ext.asyncio import AsyncSession
from app.infrastructure.db import async_session_factory
from app.repositories.order_repository import SqlAlchemyOrderRepository
from app.services.order_service import OrderService

async def get_db_session() -> AsyncIterator[AsyncSession]:
    async with async_session_factory() as session:
        yield session

async def get_order_service(
    session: AsyncSession = Depends(get_db_session),
) -> OrderService:
    repo = SqlAlchemyOrderRepository(session)
    return OrderService(repo=repo)
```

```python
# api/v1/orders.py
@router.post("/orders")
async def create_order(
    request: CreateOrderRequest,
    service: OrderService = Depends(get_order_service),
):
    return await service.create_order(request.to_command())
```

### Dependency Lifecycle

Dependencies can:

- parse request headers
- authenticate users
- open DB sessions
- construct service objects
- yield resources and clean them up
- be overridden in tests

### Testing Override

```python
from fastapi.testclient import TestClient


def test_create_order(app):
    fake_service = FakeOrderService()
    app.dependency_overrides[get_order_service] = lambda: fake_service

    with TestClient(app) as client:
        response = client.post("/api/v1/orders", json={"user_id": "...", "items": []})

    app.dependency_overrides.clear()
    assert response.status_code in {200, 201, 422}
```

### Java Developer Bridge

FastAPI `Depends()` is conceptually similar to Spring injection, but it is not classpath scanning. You write dependency functions explicitly. This is less magical and easier to override in tests, but you must maintain wiring discipline yourself.

---

## 7. SQLAlchemy Session and Repository Pattern

### Must Know

A repository hides persistence details from the service layer.

```python
# domain/protocols.py
from typing import Protocol
from app.domain.models import Order

class OrderRepository(Protocol):
    async def save(self, order: Order) -> None: ...
    async def get_by_id(self, order_id: str) -> Order | None: ...
```

```python
# repositories/order_repository.py
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.domain.models import Order
from app.infrastructure.orm_models import OrderORM

class SqlAlchemyOrderRepository:
    def __init__(self, session: AsyncSession):
        self._session = session

    async def save(self, order: Order) -> None:
        orm = OrderORM.from_domain(order)
        self._session.add(orm)
        await self._session.flush()

    async def get_by_id(self, order_id: str) -> Order | None:
        result = await self._session.execute(select(OrderORM).where(OrderORM.id == order_id))
        orm = result.scalar_one_or_none()
        return orm.to_domain() if orm else None
```

```python
# services/order_service.py
class OrderService:
    def __init__(self, repo: OrderRepository):
        self._repo = repo

    async def create_order(self, command: CreateOrderCommand) -> Order:
        order = Order.create(command)
        await self._repo.save(order)
        return order
```

### Transaction Boundary

For most services, transaction boundary should be explicit at service/use-case level.

```python
async def create_order_endpoint(
    request: CreateOrderRequest,
    session: AsyncSession = Depends(get_db_session),
):
    repo = SqlAlchemyOrderRepository(session)
    service = OrderService(repo)
    try:
        order = await service.create_order(request.to_command())
        await session.commit()
        return OrderResponse.from_domain(order)
    except Exception:
        await session.rollback()
        raise
```

You can wrap this in a Unit of Work abstraction for larger services.

### Java Developer Bridge

| Java/Spring | Python/FastAPI |
|---|---|
| `@Repository` | repository class implementing Protocol |
| `EntityManager` / Hibernate Session | SQLAlchemy `Session` / `AsyncSession` |
| `@Transactional` | explicit `commit` / `rollback`, dependency, or Unit of Work |
| JPA entity | SQLAlchemy ORM model |
| Spring Data Repository | custom repository or SQLAlchemy query layer |

### Trap

Do not keep a global SQLAlchemy session. Create a session per request or per unit of work. Sessions are not generic thread-safe global objects.

---

## 8. Alembic Migrations

### Must Know

Alembic is the standard migration tool for SQLAlchemy.

```bash
alembic init alembic
alembic revision --autogenerate -m "create orders table"
alembic upgrade head
alembic downgrade -1
```

### Enterprise Rules

- Review autogenerated migrations before committing.
- Never run destructive migrations casually in production.
- Use expand/contract for zero-downtime schema changes.
- Keep migrations backward-compatible during rolling deploys.
- Store migration scripts in version control.
- Run migrations in CI against a real database container.

### Expand/Contract Example

```text
Deploy 1: add nullable column new_status
Deploy 2: app writes both old_status and new_status
Backfill: populate new_status for old rows
Deploy 3: app reads new_status
Deploy 4: drop old_status after all versions are gone
```

### Java Developer Bridge

Alembic is closest to Flyway/Liquibase. SQLAlchemy ORM is closest to JPA/Hibernate, but Python developers often write more explicit queries and migrations rather than relying on framework magic.

---

## 9. Configuration, Secrets, and Environments

### Settings Pattern

Use `pydantic-settings` for typed environment configuration.

```python
# core/config.py
from functools import lru_cache
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    service_name: str = "orders-api"
    version: str = "1.0.0"
    environment: str = Field(default="local", validation_alias="APP_ENV")
    database_url: str
    redis_url: str | None = None
    enable_docs: bool = False
    log_level: str = "INFO"
    request_timeout_seconds: float = 2.0

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

@lru_cache
def get_settings() -> Settings:
    return Settings()
```

### Rules

- Never hard-code secrets.
- Do not commit `.env` files with real values.
- Validate required env vars at startup.
- Keep local defaults safe.
- Use secret managers in real deployments.
- Cache settings object, but allow override in tests.

### Java Developer Bridge

This maps to Spring `application.yml`, profiles, and `@ConfigurationProperties`. Python has no built-in Spring Boot-style config system; use typed settings explicitly.

---

## 10. Structured Logging and Correlation IDs

### Why It Matters

Enterprise services need logs that can be searched by request, user, tenant, order, or trace.

### Request ID with ContextVar

```python
# core/request_context.py
from contextvars import ContextVar

request_id_var: ContextVar[str] = ContextVar("request_id", default="-")
```

```python
# core/middleware.py
import uuid
from starlette.middleware.base import BaseHTTPMiddleware
from app.core.request_context import request_id_var

class RequestIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        request_id = request.headers.get("x-request-id", str(uuid.uuid4()))
        token = request_id_var.set(request_id)
        try:
            response = await call_next(request)
            response.headers["x-request-id"] = request_id
            return response
        finally:
            request_id_var.reset(token)
```

### JSON Logging

```python
import json
import logging
from datetime import datetime, timezone
from app.core.request_context import request_id_var

class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        return json.dumps({
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "request_id": request_id_var.get(),
            "module": record.module,
            "line": record.lineno,
        })
```

### Logging Rules

- Use structured logs, not random print statements.
- Include request ID, trace ID, user ID where safe.
- Never log passwords, tokens, full cards, or secrets.
- Use `logger.exception` in exception handlers.
- Use `%s` style lazy formatting in hot code.

### Java Developer Bridge

`ContextVar` gives FastAPI a similar role to SLF4J MDC in Java. `threading.local()` is not enough for async request isolation.

---

## 11. Middleware and Exception Handling

### Middleware Uses

- request ID
- timing metrics
- authentication pre-checks
- CORS
- security headers
- body size checks
- access logging

### Timing Middleware

```python
import time
from starlette.middleware.base import BaseHTTPMiddleware

class TimingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        start = time.perf_counter()
        response = await call_next(request)
        elapsed_ms = (time.perf_counter() - start) * 1000
        response.headers["x-process-time-ms"] = f"{elapsed_ms:.2f}"
        return response
```

### Exception Mapping

```python
# domain/errors.py
class OrderNotFound(Exception):
    pass

class InvalidOrderState(Exception):
    pass
```

```python
# api/exception_handlers.py
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from app.domain.errors import OrderNotFound, InvalidOrderState


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(OrderNotFound)
    async def order_not_found_handler(request: Request, exc: OrderNotFound):
        return JSONResponse(status_code=404, content={"detail": str(exc)})

    @app.exception_handler(InvalidOrderState)
    async def invalid_order_handler(request: Request, exc: InvalidOrderState):
        return JSONResponse(status_code=422, content={"detail": str(exc)})
```

### Rule

Service layer raises domain exceptions. API layer maps domain exceptions to HTTP.

### Java Developer Bridge

This is equivalent to Spring `@ControllerAdvice`. Keep HTTP-specific mapping outside the domain/service layer.

---

## 12. Timeouts, Retries, and External Clients

### HTTP Client Pattern

```python
import httpx

class PaymentClient:
    def __init__(self, base_url: str, timeout_seconds: float):
        self._client = httpx.AsyncClient(
            base_url=base_url,
            timeout=httpx.Timeout(timeout_seconds),
        )

    async def charge(self, user_id: str, amount_cents: int) -> str:
        response = await self._client.post(
            "/charges",
            json={"user_id": user_id, "amount_cents": amount_cents},
        )
        response.raise_for_status()
        return response.json()["transaction_id"]

    async def close(self) -> None:
        await self._client.aclose()
```

### Rules

- Always set timeouts.
- Reuse clients; do not create a new client per request.
- Retry only idempotent operations or operations with idempotency keys.
- Use exponential backoff with jitter.
- Add circuit breaking/bulkhead behavior for fragile dependencies.
- Cap concurrency with `asyncio.Semaphore` when downstream is limited.

### Lifespan Cleanup

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.payment_client = PaymentClient(base_url="https://payments", timeout_seconds=2.0)
    try:
        yield
    finally:
        await app.state.payment_client.close()

app = FastAPI(lifespan=lifespan)
```

### Java Developer Bridge

This maps to configuring a shared `WebClient` or `RestTemplate` bean with timeouts, connection pools, and lifecycle management. Creating an HTTP client per request is bad in both ecosystems.

---

## 13. Observability: Metrics, Traces, Health

### Health Endpoints

```python
@router.get("/health/live")
async def liveness() -> dict:
    return {"status": "alive"}

@router.get("/health/ready")
async def readiness(session: AsyncSession = Depends(get_db_session)) -> dict:
    await session.execute(text("SELECT 1"))
    return {"status": "ready"}
```

- Liveness: process is running.
- Readiness: process can serve traffic.

### Metrics to Track

| Metric | Why |
|---|---|
| request count by route/status | traffic and error rate |
| p50/p95/p99 latency | user experience and saturation |
| DB pool active/idle/wait time | connection starvation |
| external client latency/error rate | downstream health |
| event loop lag | blocking-in-async detection |
| process RSS and CPU | memory/CPU issues |
| queue depth | backpressure indicator |

### Tracing

Use OpenTelemetry for distributed traces. Propagate trace IDs and include them in logs.

### Java Developer Bridge

This maps to Micrometer, Actuator, Prometheus, OpenTelemetry, and Sleuth/MDC style correlation.

---

## 14. Security and Enterprise API Concerns

### Security Checklist

- Validate all external input with Pydantic.
- Enforce auth in dependencies or middleware.
- Use scopes/roles for authorization.
- Do not log secrets or tokens.
- Use HTTPS at ingress.
- Set CORS narrowly.
- Use rate limiting for public endpoints.
- Pin dependencies and scan vulnerabilities.
- Keep docs disabled or protected in production if needed.
- Return generic errors for auth failures.

### Auth Dependency Shape

```python
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

security = HTTPBearer()

async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
) -> CurrentUser:
    token = credentials.credentials
    user = await verify_token(token)
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")
    return user
```

### Authorization in Service Layer

Authentication can happen at API boundary. Authorization rules that are business-specific often belong in service/domain logic.

```python
await order_service.cancel_order(order_id, actor=current_user)
```

---

## 15. Testing Enterprise FastAPI Applications

### Test Pyramid

```text
Many unit tests:
  service layer, domain logic, pure functions

Some integration tests:
  repository against real Postgres container
  migration tests
  external client contract tests

Few API tests:
  FastAPI route + dependency overrides + auth behavior
```

### Service Unit Test

```python
import pytest
from unittest.mock import AsyncMock

@pytest.mark.asyncio
async def test_create_order_saves_order():
    repo = AsyncMock()
    service = OrderService(repo=repo)

    order = await service.create_order(CreateOrderCommand(user_id="u1", items=[]))

    repo.save.assert_awaited_once()
```

### API Test With Override

```python
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_create_order_api(app):
    fake_service = FakeOrderService()
    app.dependency_overrides[get_order_service] = lambda: fake_service

    async with AsyncClient(app=app, base_url="http://test") as client:
        response = await client.post("/api/v1/orders", json={"user_id": "u1", "items": []})

    app.dependency_overrides.clear()
    assert response.status_code in {200, 201, 422}
```

### Repository Integration Test

Use Testcontainers or a Dockerized database in CI. Do not mock SQLAlchemy for repository behavior; test against real SQL semantics.

### Java Developer Bridge

| Java | Python |
|---|---|
| JUnit | pytest |
| Mockito | `unittest.mock` / `pytest-mock` |
| Spring MockMvc | FastAPI `TestClient` / `httpx.AsyncClient` |
| Testcontainers Java | testcontainers-python |
| `@SpringBootTest` | full app API test with dependency overrides |

---

## 16. Deployment and Runtime Model

### Common Production Stack

```text
container
  -> gunicorn process manager
      -> uvicorn workers
          -> FastAPI app
```

Example:

```bash
gunicorn app.main:app \
  -k uvicorn.workers.UvicornWorker \
  --workers 4 \
  --bind 0.0.0.0:8080 \
  --timeout 30
```

For smaller services, direct Uvicorn may be acceptable, but Gunicorn gives process management and multiple workers.

### Worker Count

Starting point:

```text
workers = 2 * CPU cores + 1
```

But tune based on workload:

- async I/O-heavy services may use fewer workers with high concurrency
- CPU-heavy services need more processes or separate workers
- DB pool sizes must account for worker count

### Startup Rules

- Fail fast if required config is missing.
- Warm critical clients if needed.
- Do not perform long migrations inside app startup unless deployment controls it.
- Expose readiness only after DB/client checks pass.
- Gracefully close DB pools and HTTP clients on shutdown.

### Java Developer Bridge

This is different from a Spring Boot fat JAR. Python commonly uses ASGI server + process manager + app object import. Multiple workers are multiple processes, not threads inside one shared heap.

---

## 17. Enterprise Anti-Patterns

| Anti-Pattern | Why It Hurts | Better Approach |
|---|---|---|
| Business logic inside route functions | Hard to test, framework-coupled | Thin routers + service layer |
| Pydantic schema used as ORM/domain/API all at once | Couples API, persistence, business model | Separate schema/domain/ORM models |
| Global DB session | Leaks connections, unsafe lifecycle | session per request/unit of work |
| Creating `httpx.AsyncClient` per request | connection churn, slow, leaks | app-lifespan shared client |
| `requests.get()` inside `async def` | blocks event loop | `httpx.AsyncClient` or executor |
| Catching `Exception` and returning 500 silently | hides root cause | domain exceptions + structured logging |
| No request ID | impossible to trace incidents | ContextVar + middleware + JSON logs |
| Importing API layer from services | circular imports and framework coupling | one-way dependency direction |
| Running migrations casually at startup | startup race / destructive deploy risk | deployment-controlled Alembic step |
| Mocking every repository query | false confidence | repository integration tests |

---

## 18. Java Developer Bridge - Enterprise Summary

| Spring Boot Enterprise Concept | Python/FastAPI Equivalent | Key Difference |
|---|---|---|
| Controller | `APIRouter` endpoint | Function-based, explicit dependencies |
| Service | plain class/function | No annotation needed |
| Repository | Protocol + SQLAlchemy implementation | Structural typing instead of Java interface inheritance |
| DTO | Pydantic schema | Runtime validation and serialization built in |
| Entity | SQLAlchemy ORM model or domain dataclass | Often separate from schema |
| `@Autowired` | `Depends()` or explicit constructor injection | No classpath scanning by default |
| `@Transactional` | explicit session commit/rollback or Unit of Work | You own transaction boundary |
| `@ControllerAdvice` | FastAPI exception handlers | Similar mapping role |
| Filter/interceptor | middleware | Starlette middleware chain |
| `application.yml` | `pydantic-settings` + env vars | Explicit, typed config |
| Actuator | custom health/metrics endpoints | Use OpenTelemetry/Prometheus libs |
| Flyway/Liquibase | Alembic | Review autogenerated migrations |
| SLF4J MDC | `ContextVar` request context | Async-safe request-local state |
| WebClient bean | shared `httpx.AsyncClient` | close in lifespan shutdown |
| JUnit/Mockito | pytest / unittest.mock | pytest fixtures replace much setup |

### Strong Interview Position

> I design enterprise FastAPI services using the same separation of concerns I would use in Spring, but with Python idioms: routers are controllers, Pydantic models are API schemas, services hold use-case logic, repositories implement Protocols, SQLAlchemy handles persistence, and FastAPI dependencies wire request-scoped objects. I keep the framework at the edges so domain logic remains plain Python and easy to unit test.

---

## 19. Hot Interview Q&A

**Q1: Does FastAPI support MVC architecture?**
> FastAPI does not force MVC, but it supports MVC-style or clean architecture very well. `APIRouter` functions act as controllers, Pydantic schemas act as request/response DTOs, service classes hold business logic, and repositories hide persistence. The key is discipline: keep route handlers thin and push business rules into services/domain objects.

**Q2: How do you structure a production FastAPI application?**
> I split it into `api`, `schemas`, `services`, `domain`, `repositories`, `infrastructure`, and `core`. The API layer handles HTTP. Schemas validate external data. Services run use cases. Domain models hold business rules. Repositories talk to storage. Infrastructure owns DB/client setup. Core owns config, logging, middleware, and security.

**Q3: What is the difference between Pydantic models and SQLAlchemy models?**
> Pydantic models validate and serialize API data. SQLAlchemy models map Python classes to database tables. They have different reasons to change. I avoid using one class for API schema, domain entity, and persistence model in serious services because it couples client contracts to database structure.

**Q4: How do you manage DB sessions in FastAPI?**
> Use a dependency that creates a session per request or per unit of work, yields it, then closes it. Commit/rollback should be explicit or managed by a Unit of Work. Never use a global session.

**Q5: How do you test FastAPI services without hitting the real database?**
> Unit test the service layer by injecting a fake or mock repository. For API tests, use `app.dependency_overrides` to replace service dependencies. For repository tests, use a real database through Testcontainers or a test database because SQL behavior should not be mocked.

**Q6: How do you add request correlation to logs?**
> Add middleware that reads or creates an `x-request-id`, stores it in a `ContextVar`, and includes it in a JSON log formatter. `ContextVar` is async-safe, unlike `threading.local()` in an event-loop based service.

**Q7: What is the biggest enterprise FastAPI anti-pattern?**
> Putting all business logic inside route functions. It works for demos but fails in real services because it is hard to test, hard to reuse, and tightly coupled to HTTP. Thin router, service layer, repository layer is the scalable pattern.

---

## 20. Final Revision Checklist

- [ ] Can map Spring MVC concepts to FastAPI enterprise architecture
- [ ] Can explain why route handlers should be thin
- [ ] Can design `api/schemas/services/domain/repositories/infrastructure/core` layout
- [ ] Can separate Pydantic schema, domain model, and SQLAlchemy ORM model
- [ ] Can implement dependency injection with `Depends`
- [ ] Can override FastAPI dependencies in tests
- [ ] Can explain SQLAlchemy session-per-request lifecycle
- [ ] Can explain explicit transaction boundaries and Unit of Work
- [ ] Can explain Alembic migration workflow and expand/contract deploys
- [ ] Can implement typed settings with `pydantic-settings`
- [ ] Can describe structured JSON logging with request ID
- [ ] Can explain why `ContextVar` is needed for async request context
- [ ] Can map domain exceptions to HTTP errors with exception handlers
- [ ] Can configure shared async HTTP clients with timeouts and lifespan cleanup
- [ ] Can list key metrics for an enterprise FastAPI service
- [ ] Can describe security basics: auth, CORS, dependency scanning, secret safety
- [ ] Can design the test pyramid for FastAPI services
- [ ] Can explain Gunicorn/Uvicorn worker deployment model
- [ ] Can identify the top enterprise FastAPI anti-patterns
- [ ] Can bridge every concept back to Spring Boot accurately
