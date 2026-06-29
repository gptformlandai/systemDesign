# Python AsyncIO + Database Drivers — Gold Sheet

> **Track**: Python Interview Track — Group 3: Senior MAANG
> **File**: Gap Fill #1 (Track File #19a)
> **Audience**: Java developers targeting MAANG-level Python backend interviews
> **Read after**: Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| asyncpg — pure-async PostgreSQL driver | ★★★★★ | Java uses JDBC (blocking); asyncpg requires a complete async mindset shift |
| SQLAlchemy `AsyncSession` lifecycle | ★★★★★ | Java Spring Data auto-manages sessions; SQLAlchemy async requires explicit control |
| `async_sessionmaker` — factory pattern | ★★★★☆ | Java `EntityManagerFactory`; Python equivalent is newer and less documented |
| FastAPI dependency injection + `AsyncSession` | ★★★★★ | Most common FastAPI interview pattern; wrong implementation leaks sessions |
| Async transactions — begin/commit/rollback | ★★★★☆ | JDBC transactions are synchronous; async transactions have different scoping rules |
| Connection pool sizing for async services | ★★★★☆ | Thread-per-request needs one connection per thread; async can multiplex far fewer |
| N+1 query problem in async ORM | ★★★★☆ | Same problem as Java JPA lazy loading, different async detection pattern |
| `run_sync` — calling sync ORM code in async context | ★★★★☆ | Bridging legacy sync SQLAlchemy code inside async routes |
| Blocking a sync driver in async route — the silent killer | ★★★★★ | psycopg2 (sync) blocks the event loop; asyncpg does not — must know the difference |
| `AsyncEngine` vs `Engine` | ★★★★☆ | Java does not distinguish; Python SQLAlchemy has two completely separate APIs |

---

## 2. The Core Problem — Why Async + DB Is Tricky

### Must Know

```
Java JDBC / Spring Data:
  - Every DB call blocks the calling thread.
  - Thread-per-request model means the blocked thread just sits in the thread pool.
  - Connection pool = one connection per blocking thread in flight.
  - Acceptable because threads are cheap in Java compared to Python.

Python asyncio + DB:
  - The event loop runs on ONE thread.
  - If a DB call blocks (e.g., psycopg2), the ENTIRE event loop freezes.
  - No other requests can be served while the DB call is in progress.
  - Solution: use a driver that is genuinely non-blocking (asyncpg, aiomysql, motor).
  - SQLAlchemy 1.4+ added an async layer wrapping these drivers.

Production consequence:
  A FastAPI service that uses a sync DB driver (psycopg2 with sync SQLAlchemy)
  under asyncio is not just slower — it is BROKEN for concurrent load.
  One slow DB query freezes the entire service for all clients.
```

### Java Developer Bridge

| Java | Python |
|---|---|
| JDBC `DriverManager.getConnection()` | `asyncpg.connect()` / `create_async_engine()` |
| `EntityManager` / `Session` | `AsyncSession` |
| `@Transactional` on a method | `async with session.begin():` |
| Spring Data `Repository.findById()` | `await session.get(User, user_id)` |
| HikariCP connection pool | `AsyncEngine` pool (asyncpg pool under the hood) |
| `EntityManagerFactory` | `async_sessionmaker(engine)` |
| `@PersistenceContext` injection | FastAPI `Depends(get_db)` |
| JPA lazy loading causing N+1 | SQLAlchemy `lazy="select"` in async causing `MissingGreenlet` error |

---

## 3. asyncpg — Pure Async PostgreSQL Driver

### What It Is

`asyncpg` is a PostgreSQL driver written specifically for asyncio. It does **not** use psycopg2 under the hood. It talks directly to PostgreSQL using the binary protocol and releases the GIL during I/O, making it genuinely non-blocking.

```python
import asyncpg

async def fetch_users():
    conn = await asyncpg.connect("postgresql://user:password@localhost/db")
    rows = await conn.fetch("SELECT id, name FROM users WHERE active = $1", True)
    await conn.close()
    return [dict(row) for row in rows]
```

### asyncpg Connection Pool (Production Pattern)

```python
import asyncpg
from contextlib import asynccontextmanager

# Global pool — created once at startup, shared across all requests
pool: asyncpg.Pool | None = None

async def init_pool():
    global pool
    pool = await asyncpg.create_pool(
        dsn="postgresql://user:password@localhost/db",
        min_size=5,       # always-open connections
        max_size=20,      # maximum concurrent connections
        command_timeout=30,
    )

async def close_pool():
    await pool.close()

# Per-request usage
async def get_user(user_id: int) -> dict | None:
    async with pool.acquire() as conn:           # borrows a connection from pool
        row = await conn.fetchrow(
            "SELECT id, name, email FROM users WHERE id = $1", user_id
        )
        return dict(row) if row else None
```

### asyncpg vs psycopg2 — Interview Answer

```text
psycopg2: synchronous, blocks the OS thread. Fine for Django/Flask sync views.
asyncpg: async, non-blocking, releases GIL during network I/O.
          Works with asyncio. Required for FastAPI/aiohttp async performance.

psycopg3 now supports both sync and async modes.

Key rule: never use psycopg2 inside an async FastAPI route.
          It will block the event loop and freeze all concurrent requests.
```

---

## 4. SQLAlchemy AsyncSession — Full Lifecycle

### AsyncEngine Setup

```python
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

# Async engine uses asyncpg driver (note: postgresql+asyncpg://)
engine = create_async_engine(
    "postgresql+asyncpg://user:password@localhost/db",
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True,       # test connections before use — avoids stale connection errors
    echo=False,               # set True only in development to log SQL
)

# Session factory — equivalent to Java EntityManagerFactory
AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    expire_on_commit=False,   # CRITICAL for async — objects stay readable after commit
    class_=AsyncSession,
)
```

### `expire_on_commit=False` — The Most Important Setting

```python
# Java developers coming from Spring Data do NOT expect this issue.

# Default SQLAlchemy behavior: after commit, all attributes expire.
# In sync SQLAlchemy, accessing an expired attribute re-queries lazily.
# In async SQLAlchemy, lazy loading is NOT SUPPORTED — it raises MissingGreenlet.

# Wrong — default expire_on_commit=True:
async def create_user(session: AsyncSession, name: str) -> User:
    user = User(name=name)
    session.add(user)
    await session.commit()
    return user             # User attributes are expired!
                            # Accessing user.id outside session raises MissingGreenlet

# Correct — expire_on_commit=False:
# Attributes remain loaded after commit; safe to access outside the session.
```

### ORM Models

```python
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import String, Integer

class Base(DeclarativeBase):
    pass

class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    email: Mapped[str] = mapped_column(String(255), unique=True)
    active: Mapped[bool] = mapped_column(default=True)
```

### CRUD Patterns

```python
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

# CREATE
async def create_user(session: AsyncSession, name: str, email: str) -> User:
    user = User(name=name, email=email)
    session.add(user)
    await session.commit()
    await session.refresh(user)   # reload from DB to get server-set fields (id, created_at)
    return user

# READ single by primary key
async def get_user(session: AsyncSession, user_id: int) -> User | None:
    return await session.get(User, user_id)

# READ with filter
async def get_active_users(session: AsyncSession) -> list[User]:
    result = await session.execute(
        select(User).where(User.active == True).order_by(User.name)
    )
    return result.scalars().all()   # scalars() unwraps Row tuples to model instances

# UPDATE
async def deactivate_user(session: AsyncSession, user_id: int) -> None:
    user = await session.get(User, user_id)
    if user:
        user.active = False
        await session.commit()

# DELETE
async def delete_user(session: AsyncSession, user_id: int) -> None:
    user = await session.get(User, user_id)
    if user:
        await session.delete(user)
        await session.commit()
```

---

## 5. Transactions in Async SQLAlchemy

### Explicit Transaction Control

```python
# Pattern 1: begin() as context manager — auto commit on success, rollback on exception
async def transfer_credits(
    session: AsyncSession, from_id: int, to_id: int, amount: int
) -> None:
    async with session.begin():
        from_user = await session.get(User, from_id)
        to_user = await session.get(User, to_id)
        if from_user.credits < amount:
            raise ValueError("Insufficient credits")
        from_user.credits -= amount
        to_user.credits += amount
        # session.begin() auto-commits here if no exception, auto-rollbacks on exception

# Pattern 2: manual commit/rollback
async def risky_operation(session: AsyncSession) -> None:
    try:
        user = User(name="test")
        session.add(user)
        await session.commit()
    except Exception:
        await session.rollback()
        raise
```

### Savepoints (Nested Transactions)

```python
async def with_savepoint(session: AsyncSession) -> None:
    async with session.begin():
        user = User(name="outer")
        session.add(user)

        # Savepoint — partial rollback within a transaction
        async with session.begin_nested():
            risky = User(name="risky")
            session.add(risky)
            # If this block raises, only savepoint rolls back
            # outer transaction continues

        await session.commit()   # commits "outer" user only if savepoint failed
```

---

## 6. FastAPI Dependency Injection Pattern

### The Standard Pattern — Get It Right

```python
from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from typing import AsyncGenerator

app = FastAPI()

# Dependency — yields one session per request, cleans up after
async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with AsyncSessionLocal() as session:
        try:
            yield session
        except Exception:
            await session.rollback()
            raise

# Router endpoint
@app.get("/users/{user_id}")
async def read_user(
    user_id: int,
    session: AsyncSession = Depends(get_db),    # session injected by FastAPI
) -> dict:
    user = await session.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return {"id": user.id, "name": user.name, "email": user.email}
```

### Common Mistake — Session Shared Across Requests

```python
# WRONG — module-level session shared across all requests
session = AsyncSessionLocal()   # created once, shared = race conditions and dirty reads

@app.get("/users/{user_id}")
async def read_user(user_id: int):
    return await session.get(User, user_id)   # NOT safe

# CORRECT — Depends(get_db) creates a fresh session per request
```

### Service Layer Pattern (Clean Architecture)

```python
# service.py
class UserService:
    def __init__(self, session: AsyncSession):
        self.session = session

    async def get_by_id(self, user_id: int) -> User | None:
        return await self.session.get(User, user_id)

    async def create(self, name: str, email: str) -> User:
        user = User(name=name, email=email)
        self.session.add(user)
        await self.session.commit()
        await self.session.refresh(user)
        return user

# router.py
async def get_user_service(session: AsyncSession = Depends(get_db)) -> UserService:
    return UserService(session)

@app.post("/users")
async def create_user(
    payload: UserCreateRequest,
    service: UserService = Depends(get_user_service),
):
    user = await service.create(name=payload.name, email=payload.email)
    return {"id": user.id, "name": user.name}
```

---

## 7. N+1 Query Problem in Async SQLAlchemy

### The Problem

```python
# Classic N+1 with relationships
class Order(Base):
    __tablename__ = "orders"
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    user: Mapped["User"] = relationship("User", lazy="select")  # lazy load

# WRONG — causes N+1 in async:
async def get_orders_with_users(session: AsyncSession) -> list[Order]:
    result = await session.execute(select(Order))
    orders = result.scalars().all()
    for order in orders:
        print(order.user.name)   # RAISES MissingGreenlet in async!
                                 # SQLAlchemy cannot lazily load in async context
```

### The Fix — Eager Loading

```python
from sqlalchemy.orm import selectinload, joinedload

# selectinload — two queries (SELECT orders, then SELECT users WHERE id IN (...))
async def get_orders_eager(session: AsyncSession) -> list[Order]:
    result = await session.execute(
        select(Order).options(selectinload(Order.user))
    )
    return result.scalars().all()

# joinedload — single JOIN query
async def get_orders_joined(session: AsyncSession) -> list[Order]:
    result = await session.execute(
        select(Order).options(joinedload(Order.user))
    )
    return result.unique().scalars().all()   # unique() needed with joinedload to deduplicate

# Interview rule:
# - selectinload for one-to-many (avoids cartesian product explosion)
# - joinedload for many-to-one (single row per child)
```

---

## 8. Connection Pool Sizing — Async vs Thread-per-Request

### Must Know

```
Thread-per-request model (Django, Flask sync):
  - Each request thread needs a connection for its lifetime.
  - Pool size = expected concurrent requests.
  - 100 concurrent users → 100 pool connections minimum.

Async model (FastAPI + asyncpg):
  - One event loop thread handles many concurrent requests.
  - A connection is only needed during the actual DB query (microseconds to milliseconds).
  - During network I/O wait, the connection is idle but held by the session.
  - Pool size = expected CONCURRENT DB queries, not concurrent users.
  - 1000 concurrent users but average 5ms DB time out of 100ms request → ~50 connections.

Rule of thumb for async:
  pool_size = CPU_cores * 2   (for asyncpg)
  max_overflow = pool_size * 2

  Overprovisioning connections is wasteful and can hit PostgreSQL max_connections.
```

### Pool Configuration

```python
engine = create_async_engine(
    "postgresql+asyncpg://user:password@localhost/db",
    pool_size=10,           # maintained connections
    max_overflow=20,        # extra connections allowed under peak load
    pool_timeout=30,        # seconds to wait for a connection before raising
    pool_recycle=1800,      # recycle connections after 30 min (avoids stale connections)
    pool_pre_ping=True,     # test connection health before use
)
```

---

## 9. run_sync — Bridging Legacy Sync SQLAlchemy Code

### When You Need It

```python
# Some ORM operations don't have async equivalents.
# run_sync runs a sync callable in a thread pool, bridging sync and async.

from sqlalchemy.ext.asyncio import AsyncSession, AsyncConnection

# Bulk inserts with sync Core style
async def bulk_insert(session: AsyncSession, users: list[dict]) -> None:
    await session.run_sync(
        lambda sync_session: sync_session.bulk_insert_mappings(User, users)
    )
    await session.commit()

# Schema creation (typically at startup, not per request)
async def create_tables(engine):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
```

---

## 10. Common Traps — Interview Landmines

### Trap 1 — psycopg2 in Async Route

```python
# WRONG — psycopg2 is a sync driver; it blocks the event loop
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

engine = create_engine("postgresql://user:password@localhost/db")  # sync engine!
Session = sessionmaker(bind=engine)

@app.get("/users")
async def get_users():
    with Session() as session:               # blocks the event loop
        return session.query(User).all()    # FREEZES all concurrent requests

# CORRECT — use create_async_engine with asyncpg driver
```

### Trap 2 — Forgetting `expire_on_commit=False`

```python
# Session factory WITHOUT expire_on_commit=False:
AsyncSessionLocal = async_sessionmaker(engine)   # default: expire_on_commit=True

async def create_and_return_user(session: AsyncSession) -> dict:
    user = User(name="Alice")
    session.add(user)
    await session.commit()
    # user.id is now EXPIRED — accessing it causes MissingGreenlet error in async
    return {"id": user.id}   # RAISES: greenlet_spawn has not been called

# Fix: expire_on_commit=False OR call session.refresh(user) after commit
```

### Trap 3 — Async Context Manager Scope

```python
# WRONG — session used after context manager exits
async def get_user_wrong(user_id: int) -> User:
    async with AsyncSessionLocal() as session:
        user = await session.get(User, user_id)
    return user   # session closed; user attributes may be expired/inaccessible

# CORRECT — access attributes inside session scope, or use expire_on_commit=False
async def get_user_correct(user_id: int) -> dict:
    async with AsyncSessionLocal() as session:
        user = await session.get(User, user_id)
        return {"id": user.id, "name": user.name}   # extracted inside scope
```

### Trap 4 — No Connection Pool Limit

```python
# WRONG — no max_overflow means unbounded connections under load
engine = create_async_engine("postgresql+asyncpg://...")  # default: 5 pool, 10 overflow

# Under spike load, this exhausts PostgreSQL's max_connections (default 100)
# and all new connection attempts block indefinitely or raise OperationalError

# CORRECT — set explicit pool_timeout and max_overflow
engine = create_async_engine(
    "postgresql+asyncpg://...",
    pool_size=10,
    max_overflow=20,
    pool_timeout=5,   # fail fast instead of hanging
)
```

---

## 11. Alembic Migrations with Async Engine

### Setup for Async

```python
# env.py in Alembic (async-compatible setup)
from sqlalchemy.ext.asyncio import create_async_engine

def run_migrations_online():
    connectable = create_async_engine(config.get_main_option("sqlalchemy.url"))

    async def run_async_migrations():
        async with connectable.connect() as connection:
            await connection.run_sync(do_run_migrations)

    asyncio.run(run_async_migrations())
```

### Interview Note

```text
Alembic itself is synchronous. When using an async engine, you wrap the
synchronous migration logic in run_sync to bridge async connection to sync
Alembic operations. This is a common interview question about how you handle
DB migrations in a FastAPI async project.
```

---

## 12. Strong Interview Answers

### "How does database access work in an async FastAPI service?"

```text
FastAPI routes are async coroutines running on an asyncio event loop. For database
access, I use SQLAlchemy's async layer with asyncpg as the PostgreSQL driver.
asyncpg releases the GIL during I/O so the event loop can serve other requests
while waiting for the database. I create a single AsyncEngine at startup with a
connection pool, and use a FastAPI dependency — a get_db generator — to yield one
AsyncSession per request. The session is scoped to the request lifetime and rolled
back or closed on exceptions. For relationships, I use selectinload or joinedload
to avoid the N+1 problem, since lazy loading is not supported in async SQLAlchemy
and raises MissingGreenlet. I set expire_on_commit=False on the session factory so
ORM objects remain readable after commit without triggering lazy loads.
```

### "What happens if you use psycopg2 in an async FastAPI route?"

```text
psycopg2 is a synchronous driver — every database call blocks the calling OS thread.
In an asyncio service, the event loop runs on a single thread. If that thread is
blocked by psycopg2 waiting for the database, the event loop cannot schedule any
other coroutine. All concurrent requests stall for the duration of every database
query. The fix is asyncpg or psycopg3 in async mode, which release the GIL and
yield control back to the event loop during I/O waits.
```

### "What is the connection pool size strategy for async vs sync?"

```text
In a thread-per-request sync service, every in-flight request holds a DB connection
for its full duration, so pool size must match peak concurrent requests. In an async
service, a connection is only held during the actual database operation, not while
waiting for upstream calls or processing response logic. This means async services
need far fewer connections for the same request rate. A reasonable starting point is
pool_size equal to CPU count times two, with max_overflow for burst traffic. Over-
provisioning connections wastes PostgreSQL resources and can hit max_connections limits.
```

---

## 13. Revision Checklist

- [ ] Can explain why sync DB drivers break async event loops
- [ ] Can write an AsyncEngine + async_sessionmaker setup from memory
- [ ] Can write a FastAPI `get_db` dependency that correctly scopes sessions
- [ ] Knows why `expire_on_commit=False` is required and what breaks without it
- [ ] Can explain and fix the N+1 problem with selectinload/joinedload
- [ ] Can size a connection pool for async vs sync services
- [ ] Can explain how Alembic migrations work with async SQLAlchemy
- [ ] Can name the difference between asyncpg and psycopg2
