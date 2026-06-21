# Python Production Debugging Case Studies — Gold Sheet

> **Track File #26 of 31 · Group 5: Special Interview Rounds**
> For: Java developer | Level: MAANG production debugging | Mode: incident → evidence → fix

---

## 1. Interview Priority Meter

| Topic | MAANG Frequency | Java Dev Trap Level |
|---|---|---|
| Memory leak detection (`tracemalloc`) | ★★★★★ | HIGH — no JVM heap dump equivalent in stdlib |
| Reference cycles and `gc` module | ★★★★★ | HIGH — Java GC handles cycles; Python can't by default |
| CPU profiling (`cProfile`, `py-spy`) | ★★★★★ | HIGH — similar to Java async-profiler |
| Blocking-in-async detection | ★★★★★ | HIGH — no Java equivalent concept |
| N+1 query detection | ★★★★★ | MEDIUM — same problem, different tooling |
| Circular import diagnosis | ★★★★☆ | HIGH — no Java equivalent |
| Dependency conflict resolution | ★★★★☆ | MEDIUM — similar to Maven/Gradle |
| Exception swallowing / silent fail | ★★★★★ | HIGH — same anti-pattern in Java |
| `logging` vs `print` in production | ★★★★★ | MEDIUM |
| Connection leak diagnosis | ★★★★★ | MEDIUM |

---

## 2. Case Study Format

Each case follows:
```
INCIDENT: Observable symptom in production
TOOLS: What you'd reach for immediately
INVESTIGATION: Step-by-step diagnosis
ROOT CAUSE: What actually went wrong
FIX: Code change
PREVENTION: Design/process change
```

---

## 3. Case Study — Memory Leak

### INCIDENT
**Symptom:** FastAPI service memory climbs from 150MB to 2GB over 6 hours. Restarting restores it to 150MB. No obvious data growth in DB.

### TOOLS

```bash
# Runtime inspection — attach to running process
pip install memory-profiler objgraph

# Python stdlib — no install needed
python -c "import tracemalloc; help(tracemalloc)"

# Process memory snapshot
ps aux | grep uvicorn
# or
import psutil; psutil.Process().memory_info().rss
```

### INVESTIGATION

```python
# Step 1 — Take snapshots before/after load
import tracemalloc

tracemalloc.start()

# ... run 1000 requests ...

snapshot = tracemalloc.take_snapshot()
top_stats = snapshot.statistics("lineno")

for stat in top_stats[:10]:
    print(stat)
# Output:
# myapp/services.py:87: size=450 MiB, count=9812, average=47 KiB
# ← 9812 objects of 47 KiB each — something is accumulating
```

```python
# Step 2 — Find what types are growing
import objgraph
objgraph.show_most_common_types(limit=10)
# dict                          94820
# list                          78333
# ResultSet                     12045   ← suspicious — should not accumulate
# function                       8921
```

```python
# Step 3 — Trace reference holders of the leaked type
import gc
gc.collect()
objgraph.show_backrefs(
    objgraph.by_type("ResultSet")[0],
    max_depth=5,
    output_filename="leak.png"
)
```

### ROOT CAUSE

```python
# BUG — module-level cache with no eviction, growing forever
class QueryCache:
    _cache: dict[str, list] = {}   # grows without bound!

    @classmethod
    def get_or_fetch(cls, query: str) -> list:
        if query not in cls._cache:
            cls._cache[query] = db.execute(query).fetchall()
        return cls._cache[query]
```

Each unique SQL query string (with different parameters baked in as string templates) generates a new cache key. After 6 hours of traffic, hundreds of thousands of result sets are pinned in memory.

### FIX

```python
from functools import lru_cache
from cachetools import TTLCache   # pip install cachetools

# Fix 1 — LRU with bounded size
_cache: TTLCache = TTLCache(maxsize=1000, ttl=300)   # 1000 entries, 5 min TTL

# Fix 2 — Use parameterized queries + lru_cache on a method
@lru_cache(maxsize=128)
def get_user_config(user_id: int) -> dict:
    return db.fetch_user_config(user_id)

# Fix 3 — WeakValueDictionary for caches that should release when no longer used
import weakref
_cache = weakref.WeakValueDictionary()
```

### PREVENTION
- Never use an unbounded module-level dict as a cache
- Always set `maxsize` on `lru_cache`
- Add cache size metrics to your monitoring dashboard
- Use `TTLCache` or Redis with TTL for production caches

---

## 4. Case Study — Reference Cycle Memory Leak

### INCIDENT
**Symptom:** Objects of a custom class are never collected. `gc.collect()` doesn't help. Heap grows proportionally with request count.

### ROOT CAUSE — Reference Cycle

```python
# BUG — mutual references prevent reference-counting GC
class Request:
    def __init__(self, handler):
        self.handler = handler
        handler.request = self   # circular reference!

class RequestHandler:
    def __init__(self):
        self.request = None

# After the function returns, both objects have ref count > 0
# CPython's reference counter can't collect them — needs cycle collector
handler = RequestHandler()
req = Request(handler)
# req.handler = handler, handler.request = req — cycle!
```

### FIX — Break the Cycle

```python
import weakref

class Request:
    def __init__(self, handler):
        self.handler = weakref.ref(handler)   # weak reference — doesn't prevent GC

class RequestHandler:
    def __init__(self):
        self.request = None   # holds a strong reference (only one direction)

# Accessing weak reference:
handler = req.handler()   # call the weakref to get the object (None if GC'd)
if handler is not None:
    handler.process()
```

### How Python GC Works

```python
import gc

# Reference counting handles most cases (fast, immediate)
# Cyclic garbage collector handles reference cycles (runs periodically)

gc.collect()              # force cycle collection
gc.get_count()            # (gen0, gen1, gen2) counts
gc.set_debug(gc.DEBUG_LEAK)   # print detected leaks

# Check if object is tracked by GC
print(gc.is_tracked({}))   # True — dicts can participate in cycles
print(gc.is_tracked(1))    # False — ints can't
```

**Java Bridge:** Java's GC handles reference cycles automatically — the JVM uses reachability analysis from GC roots, not reference counting. Python's CPython uses reference counting + an optional cycle collector. If you use `__del__` methods, cycles involving `__del__` objects were not collected at all in Python < 3.4.

---

## 5. Case Study — High CPU / Slow Service

### INCIDENT
**Symptom:** API response time p99 spikes to 8 seconds under load. CPU usage is 100% on one core. Database queries are fast (checked via slow query log).

### TOOLS

```bash
# py-spy — attach to running Python process (no code changes needed)
pip install py-spy
py-spy top --pid 12345               # live top-like view
py-spy record -o profile.svg --pid 12345 --duration 30   # flame graph
```

```python
# cProfile — programmatic, for targeted functions
import cProfile
import pstats
import io

pr = cProfile.Profile()
pr.enable()
# ... run the slow code ...
pr.disable()

s = io.StringIO()
ps = pstats.Stats(pr, stream=s).sort_stats("cumulative")
ps.print_stats(20)
print(s.getvalue())
```

### INVESTIGATION — Reading the Flame Graph

```
py-spy flame graph output shows:
  process_batch (80% of time)
    └── json.loads (40%)
        └── _decode_string (40%)
```

**Finding:** `json.loads` is being called in a tight loop with large JSON strings.

```python
# BUG — re-parsing JSON on every call
class UserService:
    def get_permissions(self, user_id: int) -> list:
        raw = self._redis.get(f"user:{user_id}:perms")
        return json.loads(raw)   # parsing on every call, even for the same user!

    def has_permission(self, user_id: int, perm: str) -> bool:
        return perm in self.get_permissions(user_id)   # called 100x per request!
```

### FIX

```python
from functools import lru_cache

class UserService:
    @lru_cache(maxsize=512)
    def _get_cached_permissions(self, user_id: int) -> frozenset:
        raw = self._redis.get(f"user:{user_id}:perms")
        return frozenset(json.loads(raw))   # frozenset is hashable, lru_cache-friendly

    def has_permission(self, user_id: int, perm: str) -> bool:
        return perm in self._get_cached_permissions(user_id)
        # Now O(1) for cached users — parse once, check many times
```

---

## 6. Case Study — Blocking Call in Async Service

### INCIDENT
**Symptom:** FastAPI endpoint handles 1–2 requests/second under load. Single requests are fast (50ms). Adding more workers doesn't scale.

### TOOLS

```python
# Detect blocking calls at runtime
import asyncio
import time
import logging

async def detect_blocking_middleware(request, call_next):
    loop = asyncio.get_running_loop()
    start = loop.time()
    response = await call_next(request)
    elapsed = loop.time() - start
    if elapsed > 0.1:   # more than 100ms without yielding
        logging.warning(f"Slow handler: {request.url.path} took {elapsed:.3f}s")
    return response
```

```bash
# aiohttp-devtools / fastapi debug toolbar
# Or use py-spy to see the event loop thread stalled

py-spy top --pid <uvicorn_pid>
# All time in: requests.adapters.send — synchronous requests library!
```

### ROOT CAUSE

```python
# BUG — synchronous requests inside async handler
import requests
from fastapi import FastAPI

app = FastAPI()

@app.get("/enrich/{user_id}")
async def enrich_user(user_id: int):
    # This blocks the event loop thread for the full HTTP round-trip
    response = requests.get(f"https://slow.vendor.api/user/{user_id}")
    return response.json()
```

### FIX

```python
import httpx
from contextlib import asynccontextmanager
from fastapi import FastAPI

_http_client: httpx.AsyncClient | None = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global _http_client
    _http_client = httpx.AsyncClient(timeout=5.0)
    yield
    await _http_client.aclose()

app = FastAPI(lifespan=lifespan)

@app.get("/enrich/{user_id}")
async def enrich_user(user_id: int):
    response = await _http_client.get(f"https://slow.vendor.api/user/{user_id}")
    return response.json()
```

### Blocking Detection Helper

```python
import asyncio
import functools

def assert_never_blocks(func):
    """Development decorator — warns if a coroutine blocks > 50ms."""
    @functools.wraps(func)
    async def wrapper(*args, **kwargs):
        loop = asyncio.get_running_loop()
        t0 = loop.time()
        result = await func(*args, **kwargs)
        t1 = loop.time()
        if (t1 - t0) > 0.05:
            import warnings
            warnings.warn(f"{func.__name__} blocked event loop for {(t1-t0)*1000:.1f}ms")
        return result
    return wrapper
```

---

## 7. Case Study — N+1 Query Problem

### INCIDENT
**Symptom:** Listing 100 orders takes 3 seconds. The SQL slow query log shows 101 queries: one for the order list, then 100 individual user lookups.

### ROOT CAUSE

```python
# BUG — N+1 in a loop
@app.get("/orders")
async def list_orders():
    orders = await db.fetch_all("SELECT * FROM orders LIMIT 100")
    result = []
    for order in orders:
        user = await db.fetch_one(             # 1 query per order!
            "SELECT name, email FROM users WHERE id = $1",
            order["user_id"]
        )
        result.append({**order, "user": user})
    return result
# Total: 1 (orders) + 100 (users) = 101 queries
```

### FIX 1 — JOIN

```python
@app.get("/orders")
async def list_orders():
    rows = await db.fetch_all("""
        SELECT o.*, u.name as user_name, u.email as user_email
        FROM orders o
        JOIN users u ON u.id = o.user_id
        LIMIT 100
    """)
    return [dict(r) for r in rows]
# Total: 1 query
```

### FIX 2 — Batch Fetch (when JOIN is not appropriate)

```python
@app.get("/orders")
async def list_orders():
    orders = await db.fetch_all("SELECT * FROM orders LIMIT 100")
    user_ids = list({o["user_id"] for o in orders})

    users = await db.fetch_all(
        "SELECT id, name, email FROM users WHERE id = ANY($1)",
        user_ids
    )
    user_map = {u["id"]: dict(u) for u in users}

    return [
        {**dict(o), "user": user_map.get(o["user_id"])}
        for o in orders
    ]
# Total: 2 queries regardless of N
```

---

## 8. Case Study — Circular Import

### INCIDENT
**Symptom:** `ImportError: cannot import name 'UserService' from partially initialized module 'app.services'`

### ROOT CAUSE

```
# File structure that causes circular import:
app/models.py      → imports from app/services.py (for type hints)
app/services.py    → imports from app/models.py   (for User model)

Python import sequence:
1. Import app.models → starts executing models.py
2. models.py hits "from app.services import UserService"
3. Python starts importing app.services
4. services.py hits "from app.models import User"
5. app.models is partially initialized — User not yet defined → ImportError
```

### DIAGNOSIS

```python
# Add to the top of each module to trace import order
import sys

def trace_imports():
    import importlib
    original = importlib.__import__

    def traced(name, *args, **kwargs):
        print(f"Importing: {name}")
        return original(name, *args, **kwargs)

    importlib.__import__ = traced

# Or simply run:
python -v -c "from app.models import User" 2>&1 | head -50
```

### FIX 1 — Deferred Import (inside function)

```python
# app/models.py
from __future__ import annotations   # PEP 563 — all annotations are strings, not evaluated

from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from app.services import UserService   # only imported during type-checking, not runtime
```

### FIX 2 — Restructure to Break the Cycle

```
# Better structure — no circular dependency:
app/
  domain/
    models.py     # pure data models — imports NOTHING from app
  services/
    user_service.py   # imports from domain.models (one direction only)
  api/
    routes.py         # imports from services (one direction only)
```

### FIX 3 — Move Import Inside Function

```python
# app/services.py
def create_audit_entry(user):
    from app.models import AuditLog   # local import breaks the cycle at module level
    return AuditLog(user_id=user.id)
```

---

## 9. Case Study — Dependency Conflict

### INCIDENT
**Symptom:** `pip install` succeeds but `import mypackage` raises `AttributeError: module 'X' has no attribute 'Y'`. Works locally, fails in CI.

### DIAGNOSIS

```bash
# What's actually installed?
pip show requests
pip list | grep requests

# Check for conflicting installations
pip check                      # lists dependency conflicts
pip show -f requests           # shows exact files installed

# Is the right Python/venv active?
which python
python -c "import sys; print(sys.executable)"
python -c "import sys; print(sys.path)"

# Check for multiple versions
find / -name "requests" -type d 2>/dev/null
```

### ROOT CAUSE SCENARIOS

```bash
# Scenario A — Wrong virtual environment
# Developer activated wrong venv or no venv at all
source .venv/bin/activate      # always check this first
which python                   # should point into .venv/

# Scenario B — System Python has older version that shadows venv
python -c "import requests; print(requests.__version__)"
# 2.18.0 ← too old, despite installing 2.31.0

# Scenario C — requirements.txt is unpinned
pip install -r requirements.txt   # installs latest — breaks on next run
# Fix: pin all deps
pip freeze > requirements.txt    # exact pinned versions

# Scenario D — Poetry lock file not committed
poetry install    # installs from pyproject.toml, not lockfile
poetry install --no-root   # use poetry.lock
git add poetry.lock   # MUST commit the lockfile!
```

### BEST PRACTICE

```bash
# Always use virtual environments
python -m venv .venv
source .venv/bin/activate

# Always pin dependencies
pip install requests==2.31.0   # exact pin
# or poetry (recommended)
poetry add requests@^2.31.0

# Lock before deploying
poetry lock
poetry install --sync   # installs exactly what's in lockfile, removes extras

# Docker — always copy lockfile before install
COPY poetry.lock pyproject.toml ./
RUN poetry install --no-dev --no-root
```

---

## 10. Case Study — Exception Swallowing / Silent Failure

### INCIDENT
**Symptom:** Payment processing appears to succeed (HTTP 200), but money is not charged. No errors in logs.

### ROOT CAUSE

```python
# BUG — bare except swallows everything
def charge_payment(user_id: str, amount: float) -> dict:
    try:
        result = payment_gateway.charge(user_id, amount)
        return {"status": "success", "transaction_id": result.id}
    except:                          # catches EVERYTHING including KeyboardInterrupt!
        return {"status": "success"} # silently returns success without charging!

# Also bad — too broad exception handling
def charge_payment(user_id: str, amount: float) -> dict:
    try:
        result = payment_gateway.charge(user_id, amount)
        return {"status": "success", "transaction_id": result.id}
    except Exception:
        return {"status": "success"} # still hides the failure!
```

### FIX

```python
import logging

logger = logging.getLogger(__name__)

class PaymentError(Exception):
    """Domain-specific exception for payment failures."""

def charge_payment(user_id: str, amount: float) -> dict:
    try:
        result = payment_gateway.charge(user_id, amount)
        return {"status": "success", "transaction_id": result.id}
    except payment_gateway.InsufficientFundsError as e:
        logger.warning(f"Insufficient funds for user {user_id}: {e}")
        raise PaymentError("Insufficient funds") from e
    except payment_gateway.NetworkError as e:
        logger.error(f"Payment gateway network error: {e}", exc_info=True)
        raise PaymentError("Payment service unavailable") from e
    # No bare except — let unexpected exceptions propagate
```

### Rules for Exception Handling

```python
# NEVER do this
except:
except Exception: pass

# NEVER silently return success on failure
except SomeError:
    return {"status": "ok"}   # lying to the caller

# ALWAYS:
# 1. Catch specific exception types
# 2. Log with exc_info=True for unexpected errors
# 3. Either re-raise, raise a domain exception, or handle explicitly
# 4. Use "raise X from original_e" to preserve the cause chain

try:
    result = risky_call()
except SpecificError as e:
    logger.error("Expected failure", exc_info=True)
    raise DomainException("Human-readable message") from e
```

---

## 11. Case Study — Connection Leak

### INCIDENT
**Symptom:** After running for 4 hours, the DB starts rejecting connections with "too many connections". Restarting the service fixes it temporarily.

### ROOT CAUSE

```python
# BUG — connection not closed on exception
def get_users():
    conn = db.get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM users")
    results = cursor.fetchall()
    # Exception here? Neither cursor nor conn is closed!
    conn.close()
    return results

# BUG 2 — async version: connection not returned to pool
async def get_user(user_id: int):
    conn = await pool.acquire()
    row = await conn.fetchrow("SELECT * FROM users WHERE id = $1", user_id)
    # Exception here? Connection never returned to pool!
    await pool.release(conn)
    return row
```

### FIX

```python
# Sync: always use context manager
def get_users():
    with db.get_connection() as conn:      # __exit__ closes even on exception
        with conn.cursor() as cursor:
            cursor.execute("SELECT * FROM users")
            return cursor.fetchall()

# Async: pool context manager
async def get_user(user_id: int):
    async with pool.acquire() as conn:     # __aexit__ releases back to pool
        return await conn.fetchrow(
            "SELECT * FROM users WHERE id = $1", user_id
        )

# FastAPI with databases library
@app.get("/users/{user_id}")
async def get_user(user_id: int):
    async with database.transaction():    # auto-commit or rollback
        return await database.fetch_one(
            "SELECT * FROM users WHERE id = :id",
            {"id": user_id}
        )
```

### Monitoring Connection Pool Health

```python
import asyncpg

pool = await asyncpg.create_pool(
    dsn="postgresql://...",
    min_size=5,
    max_size=20,
)

# Inspect pool status
print(f"Pool size: {pool.get_size()}")
print(f"Idle connections: {pool.get_idle_size()}")
print(f"Min size: {pool.get_min_size()}")
print(f"Max size: {pool.get_max_size()}")
```

---

## 12. Case Study — Logging Correctly in Production

### Common Anti-Patterns

```python
# NEVER in production
print("User created:", user_id)           # no timestamp, no level, no context

# NEVER log inside a hot loop
for item in million_items:
    logger.debug(f"Processing {item}")    # even if level is INFO, f-string is evaluated!

# Fix — lazy evaluation
for item in million_items:
    if logger.isEnabledFor(logging.DEBUG):   # check first
        logger.debug("Processing %s", item)  # or use %s (lazy format)
```

### Correct Structured Logging Setup

```python
import logging
import json
from datetime import datetime

class JSONFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        return json.dumps({
            "timestamp": datetime.utcnow().isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "module": record.module,
            "line": record.lineno,
            **getattr(record, "extra", {}),
        })

# Setup
handler = logging.StreamHandler()
handler.setFormatter(JSONFormatter())

logging.basicConfig(handlers=[handler], level=logging.INFO)
logger = logging.getLogger(__name__)

# Usage with context
logger.info(
    "Payment processed",
    extra={"user_id": user_id, "amount": amount, "transaction_id": txn_id}
)
```

### Exception Logging

```python
try:
    result = process(data)
except ValueError as e:
    logger.exception("Validation failed")    # exc_info=True automatically
    # OR
    logger.error("Validation failed: %s", e, exc_info=True)
    raise
```

---

## 13. Production Debugging Toolkit Summary

| Tool | Purpose | When to Use |
|---|---|---|
| `tracemalloc` | Memory allocation snapshot diff | Memory growing unexpectedly |
| `objgraph` | Object count by type, backref graph | Finding what's accumulating |
| `gc.collect()` + `gc.get_count()` | Force cycle collection, check generation counts | Reference cycle suspects |
| `weakref` | Weak references that don't prevent GC | Breaking reference cycles |
| `cProfile` + `pstats` | CPU profiling — per-function time | Slow functions, CPU hotspots |
| `py-spy` | Attach to live process, flame graph | Production CPU profiling, no restart |
| `asyncio.get_event_loop().time()` | Measure event loop latency | Blocking-in-async detection |
| `pip check` | Find dependency conflicts | `AttributeError` / wrong version |
| `pip show -f <pkg>` | Find installed files | Wrong package location |
| `logging.exception()` | Log with full traceback | Any `except` block |
| `psutil` | Process memory, CPU, file descriptors | Process-level resource monitoring |
| `pool.get_size()` | Connection pool health | Connection exhaustion debugging |

---

## 14. Java Developer Bridge — Debugging Mapping

| Incident Type | Java Tooling | Python Tooling |
|---|---|---|
| Memory leak | JVM heap dump + Eclipse MAT / VisualVM | `tracemalloc` + `objgraph` |
| Reference cycle | Handled by GC automatically | `gc.collect()` + `weakref` needed |
| CPU profiling | async-profiler, JFR, YourKit | `cProfile`, `py-spy` flame graph |
| Thread CPU hotspot | JFR thread profiling | `py-spy` + `threading` analysis |
| Blocking in reactive | Reactor's `BlockHound` | asyncio event loop latency measurement |
| N+1 query | Hibernate `SHOW SQL`, Spring Data logging | SQL query logging in db driver |
| Exception swallowing | SonarQube lint rule | Code review + specific exception catching |
| Circular import | N/A (no circular class loading) | `python -v` import trace |
| Dependency conflict | Maven `mvn dependency:tree` | `pip check`, `pipdeptree` |
| Connection leak | HikariCP leak detection | Pool metrics + context managers |
| Structured logging | Logback + SLF4J MDC | `logging` + `JSONFormatter` + extra |
| Silent failure | Checked exceptions enforce handling | No checked exceptions — must discipline yourself |

---

## 15. Hot Interview Q&A

**Q1: How do you diagnose a memory leak in a Python service without restarting it?**
> Attach `py-spy` or use `tracemalloc` programmatically (start it at app startup, expose a `/debug/memory` endpoint that takes a snapshot). Compare snapshots before and after load to identify growing allocations. Use `objgraph.show_most_common_types()` to find what object types are accumulating. Then use `objgraph.show_backrefs()` to trace what's holding references.

**Q2: Why doesn't Python's garbage collector handle all memory leaks automatically?**
> CPython uses reference counting as its primary mechanism — objects are freed immediately when their reference count drops to zero. However, reference cycles (A → B → A) never reach zero. The cyclic garbage collector handles this, but it only runs periodically and can miss objects with `__del__` methods (in Python < 3.4, objects with `__del__` in cycles were never collected). `weakref` breaks cycles at the design level and is the correct fix.

**Q3: How do you detect a blocking call inside an `async def` function in production?**
> Add middleware that measures wall-clock time between yielding points. If a handler takes longer than expected without `await` points, it's blocking. Tools: `py-spy top` on the uvicorn PID will show the event loop thread stalled inside a synchronous call. `aiohttp-devtools` has a similar blocking detection. The fix is always either `await` the async version of the library or `run_in_executor` for unavoidable sync code.

**Q4: What is the N+1 query problem and how do you fix it in Python?**
> N+1 occurs when you fetch a list of N items then make one additional query per item — N+1 total queries. Fix 1: use a SQL JOIN to fetch both in one query. Fix 2: batch-fetch the related records with `WHERE id = ANY($1)` and build an in-memory dict for lookup. Fix 3: use ORM eager loading (`selectinload`, `joinedload`). SQLAlchemy's `echo=True` mode logs all queries — use it to detect N+1 during development.

**Q5: How do you fix a circular import error in Python?**
> Three approaches: (1) Use `from __future__ import annotations` and `TYPE_CHECKING` — imports inside `if TYPE_CHECKING:` are only used by type checkers, not at runtime. (2) Move the import inside the function body — deferred to call time, breaking the module-level cycle. (3) Restructure — put models in a separate module that imports nothing from services, then have services import from models (one-way dependency).

**Q6: What is the difference between `logger.error("msg %s", val)` and `logger.error(f"msg {val}")`?**
> The f-string is evaluated eagerly — even if the log level is INFO and the DEBUG/ERROR message would be discarded, the string is formatted. The `%s` form is lazy — the `LogRecord` stores the format string and arguments separately, only formatting when actually emitting the log. In hot loops with disabled log levels, f-strings add measurable overhead. The `%s` form is O(1) for disabled levels; f-strings are always O(len) for the format.

**Q7: How do you prevent connection leaks in async Python code?**
> Always use `async with pool.acquire()` — the async context manager guarantees `pool.release()` is called even if an exception is raised. Never call `pool.acquire()` and `pool.release()` manually without `try/finally`. For sync code, use `with db.connection()`. In FastAPI, declare DB sessions as dependencies with `yield` — FastAPI calls cleanup code in the `finally` block automatically.

---

## 16. Final Revision Checklist

- [ ] Can describe the `tracemalloc` snapshot workflow: start → snapshot → diff
- [ ] Can use `objgraph.show_most_common_types()` to find accumulating objects
- [ ] Can explain why reference cycles can cause leaks and how `weakref` fixes them
- [ ] Can explain CPython's reference counting + cyclic GC two-phase model
- [ ] Can attach `py-spy` to a running process and read a flame graph
- [ ] Can use `cProfile` + `pstats` to find a CPU hotspot function
- [ ] Can diagnose blocking-in-async via event loop latency measurement
- [ ] Can diagnose N+1 with SQL query logging and fix with JOIN or batch fetch
- [ ] Can explain three approaches to breaking circular imports
- [ ] Can explain why bare `except:` is dangerous and what to do instead
- [ ] Can fix a connection leak using `async with pool.acquire()`
- [ ] Can explain why `logger.error("msg %s", val)` is better than f-string in hot loops
- [ ] Can set up structured JSON logging with request context fields
- [ ] Can use `pip check` and `pip show` to diagnose dependency conflicts
- [ ] Can explain `poetry.lock` and why it must be committed to version control
- [ ] Can explain the difference between Java's GC (reachability) and Python's (refcount + cycle GC)
