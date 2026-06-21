# Python Async, API & Concurrency Scenario Bank — Gold Sheet

> **Track File #21 of 31 · Group 4: Scenario Practice**
> For: Java developer | Level: MAANG scenario depth | Mode: bug hunt + architecture drills

---

## 1. Interview Priority Meter

| Topic | MAANG Frequency | Java Dev Trap Level |
|---|---|---|
| Blocking call inside `async def` | ★★★★★ | HIGH — no Java equivalent confusion |
| `asyncio.gather` vs sequential `await` | ★★★★★ | HIGH — maps to `allOf` vs `thenCompose` |
| Connection pool exhaustion | ★★★★★ | HIGH — same in Java but config differs |
| Task cancellation + cleanup | ★★★★☆ | HIGH — `CancellationException` maps poorly |
| `run_in_executor` for blocking I/O | ★★★★★ | HIGH — maps to `supplyAsync(executor)` |
| `asyncio.Semaphore` for rate limiting | ★★★★☆ | MEDIUM — maps to `Semaphore` |
| `asyncio.timeout` / `wait_for` | ★★★★★ | HIGH — maps to `CompletableFuture.orTimeout` |
| `async for` / `async with` | ★★★★☆ | MEDIUM |
| FastAPI `async def` vs `def` routing | ★★★★★ | HIGH — FastAPI-specific |
| Event loop internals (one thread) | ★★★★☆ | HIGH — opposite of Java thread pool |

---

## 2. The Fundamental Mental Model

### Python asyncio vs Java Concurrency

```
Python asyncio                       Java (Spring/virtual threads)
─────────────────────────────────    ────────────────────────────────────────
Single event loop thread             Threadpool (e.g. 200 tomcat threads)
Cooperative scheduling               Preemptive scheduling
I/O released via await               I/O blocks the thread (or virtual thread yields)
Blocking = freezes ALL requests      Blocking = blocks only that thread
asyncio.gather = run concurrently    CompletableFuture.allOf
asyncio.wait = partial completion    CompletableFuture.anyOf / allOf
asyncio.Semaphore = rate limit       java.util.concurrent.Semaphore
run_in_executor = offload blocking   CompletableFuture.supplyAsync(executor)
```

**The #1 rule:** In Python asyncio, ONE blocking call blocks ALL requests. In Java, one blocking call blocks ONE thread — other requests still run.

---

## 3. Blocking Inside Async — The Core Bug

### Scenario 3-A — `requests` Library in Async Code

**Interviewer:** "Your FastAPI endpoint handles 1 request/second instead of 50. It's hitting an external payment API. What's wrong?"

```python
# BUG — requests is synchronous (blocking)
import requests
from fastapi import FastAPI

app = FastAPI()

@app.get("/charge/{amount}")
async def charge(amount: float):
    # requests.post() blocks the event loop thread entirely!
    response = requests.post(
        "https://payment.api/charge",
        json={"amount": amount}
    )
    return response.json()
```

**What happens:**
- Request 1 arrives → event loop calls `charge()` → `requests.post()` blocks the thread for ~200ms
- No other coroutine can run during those 200ms
- At 200ms per request: max throughput ≈ 5 requests/second
- Under load: requests queue, latency spikes, timeouts cascade

**Fix — Use `httpx.AsyncClient`:**

```python
import httpx
from fastapi import FastAPI

app = FastAPI()

# Reuse client across requests (connection pooling!)
_client: httpx.AsyncClient | None = None

@app.on_event("startup")
async def startup():
    global _client
    _client = httpx.AsyncClient(timeout=10.0)

@app.on_event("shutdown")
async def shutdown():
    await _client.aclose()

@app.get("/charge/{amount}")
async def charge(amount: float):
    response = await _client.post(      # non-blocking — yields to event loop
        "https://payment.api/charge",
        json={"amount": amount}
    )
    return response.json()
```

**Strong Answer:**
> "`requests` is a synchronous library that blocks the OS thread. In asyncio, the event loop and all coroutines share one thread. A blocking call starves the event loop — no other coroutine can proceed until the blocking call returns. The fix is `httpx.AsyncClient` which uses `await` to yield control during I/O, allowing other requests to be served concurrently."

---

### Scenario 3-B — Synchronous ORM in Async Handler

```python
# BUG — SQLAlchemy sync session blocks event loop
from sqlalchemy.orm import Session
from fastapi import FastAPI, Depends

app = FastAPI()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.get("/users/{user_id}")
async def get_user(user_id: int, db: Session = Depends(get_db)):
    # db.query() is synchronous — blocks event loop!
    user = db.query(User).filter(User.id == user_id).first()
    return user
```

**Fix — Use SQLAlchemy async or run in executor:**

```python
# Option 1: SQLAlchemy async (preferred for new code)
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy import select

engine = create_async_engine("postgresql+asyncpg://user:pw@host/db")

@app.get("/users/{user_id}")
async def get_user(user_id: int, db: AsyncSession = Depends(get_async_db)):
    result = await db.execute(select(User).where(User.id == user_id))
    return result.scalar_one_or_none()

# Option 2: run_in_executor for legacy sync ORM
import asyncio

@app.get("/users/{user_id}")
async def get_user(user_id: int):
    loop = asyncio.get_event_loop()
    user = await loop.run_in_executor(
        None,   # default ThreadPoolExecutor
        lambda: sync_db_query(user_id)
    )
    return user
```

---

### Scenario 3-C — CPU-Bound Work in Async Handler

```python
# BUG — heavy computation blocks event loop
import asyncio
from fastapi import FastAPI

app = FastAPI()

def compute_hash(data: bytes) -> str:
    import hashlib
    # Simulate CPU work — blocks event loop thread!
    return hashlib.sha256(data).hexdigest()

@app.post("/hash")
async def hash_data(data: bytes):
    result = compute_hash(data)   # blocks event loop!
    return {"hash": result}
```

**Fix — Offload to executor:**

```python
import asyncio
from concurrent.futures import ProcessPoolExecutor

# Process pool for CPU-bound work (bypasses GIL)
_process_pool = ProcessPoolExecutor(max_workers=4)

@app.post("/hash")
async def hash_data(data: bytes):
    loop = asyncio.get_running_loop()
    result = await loop.run_in_executor(_process_pool, compute_hash, data)
    return {"hash": result}

# For I/O-bound legacy blocking code, ThreadPoolExecutor is sufficient
# For CPU-bound code, ProcessPoolExecutor bypasses GIL
```

---

## 4. Sequential vs Concurrent Async — The Performance Cliff

### Scenario 4-A — Awaiting in a Loop (Sequential)

**Interviewer:** "Fetching 100 user profiles takes 10 seconds. Each profile fetch takes 100ms. Why isn't it faster?"

```python
# SLOW: sequential awaiting — total time = n × latency
async def fetch_all_profiles(user_ids: list[int]) -> list[dict]:
    profiles = []
    for uid in user_ids:
        profile = await fetch_profile(uid)   # waits for each before starting next
        profiles.append(profile)
    return profiles
# 100 users × 100ms = 10 seconds
```

**Fix — `asyncio.gather` for true concurrency:**

```python
# FAST: concurrent — total time ≈ single worst-case latency
async def fetch_all_profiles(user_ids: list[int]) -> list[dict]:
    tasks = [fetch_profile(uid) for uid in user_ids]
    profiles = await asyncio.gather(*tasks)
    return list(profiles)
# 100 users, all started at once ≈ ~100ms
```

**Strong Answer:**
> "Sequential `await` in a loop means each network call waits for the previous one to complete before starting — it's async in syntax but serial in behavior. `asyncio.gather()` launches all coroutines concurrently. They all start immediately and the event loop switches between them during I/O waits, so total time is approximately the worst single-request latency."

**Java Bridge:** `asyncio.gather()` maps to `CompletableFuture.allOf(futures).join()`. Sequential `await` in a loop maps to chaining `.thenCompose()` — it's a pipeline, not parallel.

---

### Scenario 4-B — `gather` with Error Handling

```python
import asyncio

# By default, gather raises the first exception and cancels others
async def risky_gather():
    try:
        results = await asyncio.gather(
            fetch_user(1),
            fetch_user(2),
            fetch_user(999),   # this one fails
        )
    except Exception as e:
        print(f"One failed: {e}")   # other results are lost!

# return_exceptions=True — collect all results/errors
async def safe_gather():
    results = await asyncio.gather(
        fetch_user(1),
        fetch_user(2),
        fetch_user(999),
        return_exceptions=True   # failures become exception objects in results list
    )
    for r in results:
        if isinstance(r, Exception):
            print(f"Failed: {r}")
        else:
            process(r)
```

---

### Scenario 4-C — `asyncio.wait` for Partial Completion

```python
import asyncio

async def first_successful(tasks):
    # Return as soon as ANY task completes
    done, pending = await asyncio.wait(
        tasks,
        return_when=asyncio.FIRST_COMPLETED
    )
    # Cancel remaining tasks
    for task in pending:
        task.cancel()

    winner = done.pop()
    return winner.result()

# Use case: race multiple CDN regions, use fastest response
async def get_asset(asset_id: str):
    tasks = {
        asyncio.create_task(fetch_from_region("us-east", asset_id)),
        asyncio.create_task(fetch_from_region("eu-west", asset_id)),
        asyncio.create_task(fetch_from_region("ap-south", asset_id)),
    }
    return await first_successful(tasks)
```

---

## 5. Timeout Scenarios

### Scenario 5-A — Request Timeout Without Cancellation

**Interviewer:** "Your service starts timing out clients after 5 seconds, but the downstream call keeps running even after the client disconnects. What's the impact?"

```python
# BUG — timeout doesn't cancel downstream work
import asyncio
from fastapi import FastAPI

app = FastAPI()

@app.get("/report")
async def generate_report():
    # Client gives up after 5 seconds (proxy/load balancer timeout)
    # But the coroutine keeps running, consuming DB and CPU resources!
    result = await generate_heavy_report()
    return result
```

**Fix — Enforce timeout and cancel on breach:**

```python
import asyncio
from fastapi import FastAPI, HTTPException

app = FastAPI()

@app.get("/report")
async def generate_report():
    try:
        # asyncio.timeout (Python 3.11+)
        async with asyncio.timeout(4.5):
            result = await generate_heavy_report()
        return result
    except asyncio.TimeoutError:
        raise HTTPException(status_code=504, detail="Report generation timed out")
```

**Pre-3.11 equivalent — `asyncio.wait_for`:**

```python
async def generate_report():
    try:
        result = await asyncio.wait_for(
            generate_heavy_report(),
            timeout=4.5
        )
        return result
    except asyncio.TimeoutError:
        raise HTTPException(status_code=504, detail="Timed out")
```

**What `asyncio.timeout` / `wait_for` does:**
1. Sends a `CancelledError` into the wrapped coroutine
2. The coroutine must handle cleanup in a `try/finally` or `except CancelledError`
3. The task is properly cancelled — no orphan work continues

---

### Scenario 5-B — Per-Request Timeout with httpx

```python
import httpx
import asyncio
from fastapi import FastAPI, HTTPException

app = FastAPI()

@app.get("/external-data")
async def get_external_data():
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(
                "https://slow.api/data",
                timeout=httpx.Timeout(
                    connect=2.0,   # connection timeout
                    read=5.0,      # read timeout
                    write=2.0,     # write timeout
                    pool=1.0       # pool acquisition timeout
                )
            )
            return response.json()
        except httpx.TimeoutException as e:
            raise HTTPException(status_code=504, detail=f"Upstream timeout: {e}")
        except httpx.HTTPStatusError as e:
            raise HTTPException(status_code=502, detail=f"Upstream error: {e.response.status_code}")
```

---

## 6. Task Cancellation Scenarios

### Scenario 6-A — CancelledError Must Not Be Swallowed

```python
import asyncio

# BUG — swallowing CancelledError prevents proper cancellation
async def worker():
    try:
        await asyncio.sleep(10)
    except Exception as e:
        print(f"Error: {e}")
        # CancelledError is a BaseException (not Exception in 3.8+)
        # But this except catches it in Python 3.7 — task appears cancelled but isn't
```

**Python 3.8+:** `asyncio.CancelledError` inherits from `BaseException`, not `Exception`. `except Exception` no longer catches it — this is intentional.

```python
# CORRECT — handle cancellation explicitly
async def worker():
    try:
        await asyncio.sleep(10)
    except asyncio.CancelledError:
        # Cleanup (close files, release locks, etc.)
        await cleanup()
        raise   # MUST re-raise so the cancellation propagates!
    except Exception as e:
        print(f"Other error: {e}")
```

**Strong Answer:**
> "Catching and swallowing `CancelledError` breaks the cooperative cancellation contract. The task's caller (or `wait_for`) waits for the task to actually finish — if you swallow the error without re-raising, the task appears to complete normally and the timeout machinery fails. Always re-raise `CancelledError` after cleanup."

**Java Bridge:** Java's `InterruptedException` has the same rule — always restore the interrupt flag or re-throw. Swallowing either is an anti-pattern in both languages.

---

### Scenario 6-B — Cleanup with `asyncio.shield`

```python
import asyncio

async def save_progress(data):
    """Must complete even if the outer task is cancelled."""
    await asyncio.sleep(0.1)   # simulate DB write
    print("Progress saved")

async def long_operation(data):
    try:
        result = await process(data)
        return result
    except asyncio.CancelledError:
        # Shield save_progress from cancellation — it MUST finish
        await asyncio.shield(save_progress(data))
        raise
```

**`asyncio.shield(coro)`** protects the inner coroutine from being cancelled when the outer task is cancelled. The outer task is cancelled; the shielded coroutine runs to completion.

---

### Scenario 6-C — Task Group for Structured Concurrency (Python 3.11+)

```python
import asyncio

async def fetch_dashboard(user_id: int):
    async with asyncio.TaskGroup() as tg:
        task_profile = tg.create_task(fetch_profile(user_id))
        task_orders = tg.create_task(fetch_orders(user_id))
        task_recs = tg.create_task(fetch_recommendations(user_id))
    # All tasks complete (or all cancelled on first exception) before exiting the block

    return {
        "profile": task_profile.result(),
        "orders": task_orders.result(),
        "recommendations": task_recs.result(),
    }
```

**`TaskGroup` guarantees:**
- If any task raises, ALL other tasks are cancelled
- The exception propagates out of the `async with` block
- No orphaned tasks — structured concurrency

**Java Bridge:** Java 21's `StructuredTaskScope` is the direct equivalent of `asyncio.TaskGroup` — structured concurrency arrived in Java later than Python.

---

## 7. Connection Pool Scenarios

### Scenario 7-A — Pool Exhaustion Under Load

**Interviewer:** "At 100 concurrent requests, your API starts returning 'connection timeout' errors. The database is healthy. What's the bottleneck?"

```python
# BUG — pool too small for concurrency
from databases import Database

DATABASE_URL = "postgresql://user:pw@host/db"
database = Database(DATABASE_URL, min_size=1, max_size=5)   # only 5 connections!

@app.get("/users")
async def list_users():
    # 6th concurrent request waits for a free connection
    # Default pool timeout is low — raises TimeoutError
    rows = await database.fetch_all("SELECT * FROM users")
    return rows
```

**What happens at 100 concurrent requests:**
1. 5 connections checked out immediately
2. Requests 6–100 wait for a free slot
3. Pool acquisition timeout triggers → requests fail

**Fix — Tune pool size:**

```python
from databases import Database

# Rule of thumb: pool_max = num_cores × 2-4 for I/O-bound
# For Postgres: conservative is 10-20 per service instance
database = Database(
    DATABASE_URL,
    min_size=5,
    max_size=20,
)

# With explicit pool timeout for fast-fail
database = Database(
    DATABASE_URL,
    min_size=5,
    max_size=20,
)
```

**Semaphore as soft cap before pool:**

```python
import asyncio

# Prevent overwhelming the pool — limit in-flight DB calls
_db_semaphore = asyncio.Semaphore(15)

@app.get("/users")
async def list_users():
    async with _db_semaphore:
        rows = await database.fetch_all("SELECT * FROM users")
    return rows
```

---

### Scenario 7-B — HTTP Client Pool — Don't Create New Client Per Request

```python
# BUG — new client per request, no connection reuse
@app.get("/proxy/{path}")
async def proxy(path: str):
    async with httpx.AsyncClient() as client:   # new client = new TCP connections every time!
        resp = await client.get(f"https://backend/{path}")
    return resp.json()
```

**Each `httpx.AsyncClient()` creates a fresh connection pool with no pre-existing connections. Under load this means:**
- TCP handshake overhead per request
- TLS handshake overhead per request (especially costly)
- `Too many open files` errors if the client isn't closed quickly

**Fix — Reuse a single shared client:**

```python
from contextlib import asynccontextmanager
import httpx
from fastapi import FastAPI

http_client: httpx.AsyncClient = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global http_client
    http_client = httpx.AsyncClient(
        timeout=httpx.Timeout(5.0),
        limits=httpx.Limits(
            max_connections=100,
            max_keepalive_connections=20,
            keepalive_expiry=30,
        )
    )
    yield
    await http_client.aclose()

app = FastAPI(lifespan=lifespan)

@app.get("/proxy/{path}")
async def proxy(path: str):
    resp = await http_client.get(f"https://backend/{path}")
    return resp.json()
```

---

### Scenario 7-C — `asyncio.Semaphore` for Rate Limiting External Calls

**Interviewer:** "Your service calls a third-party API that allows only 10 concurrent requests. How do you enforce this without a queue?"

```python
import asyncio
import httpx

CONCURRENCY_LIMIT = 10
_semaphore = asyncio.Semaphore(CONCURRENCY_LIMIT)

async def call_rate_limited_api(item_id: int) -> dict:
    async with _semaphore:   # blocks here if 10 calls already in-flight
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"https://thirdparty.api/items/{item_id}")
            return resp.json()

async def process_all(item_ids: list[int]) -> list[dict]:
    tasks = [call_rate_limited_api(iid) for iid in item_ids]
    return await asyncio.gather(*tasks)
```

**How it works:**
- `asyncio.Semaphore(10)` maintains an internal counter = 10
- `async with _semaphore` decrements counter; if 0, awaits until incremented
- Exiting the `async with` block increments counter — next waiter is unblocked
- All 1000 tasks are created immediately; only 10 run concurrently at any time

**Java Bridge:** Java `Semaphore(10)` with `acquire()` / `release()` is identical in concept. Python's `async with` is the `try/finally` + `acquire/release` pattern made ergonomic.

---

## 8. `run_in_executor` — Bridging Sync and Async

### Scenario 8-A — Legacy Blocking Library in Async App

```python
import asyncio
from concurrent.futures import ThreadPoolExecutor
from fastapi import FastAPI
import boto3   # synchronous AWS SDK

app = FastAPI()
_thread_pool = ThreadPoolExecutor(max_workers=10)

def _sync_s3_upload(bucket: str, key: str, data: bytes) -> str:
    """Purely synchronous — must run in a thread."""
    s3 = boto3.client("s3")
    s3.put_object(Bucket=bucket, Key=key, Body=data)
    return f"s3://{bucket}/{key}"

@app.post("/upload")
async def upload(key: str, data: bytes):
    loop = asyncio.get_running_loop()
    url = await loop.run_in_executor(
        _thread_pool,
        _sync_s3_upload,
        "my-bucket", key, data
    )
    return {"url": url}
```

**`run_in_executor` contract:**
- Runs the callable in a thread pool
- Returns an awaitable — event loop continues serving other requests
- ThreadPoolExecutor: I/O-bound blocking (DB calls, file I/O, legacy HTTP)
- ProcessPoolExecutor: CPU-bound work (image processing, data transformation)

---

### Scenario 8-B — FastAPI `def` vs `async def` Route

```python
from fastapi import FastAPI

app = FastAPI()

# async def — runs on the event loop thread
# Use when: all I/O uses await (async DB, httpx, etc.)
@app.get("/fast")
async def fast_endpoint():
    result = await async_db.fetch("SELECT 1")
    return result

# def (sync) — FastAPI automatically runs it in a threadpool
# Use when: using synchronous libraries you cannot easily replace
@app.get("/legacy")
def legacy_endpoint():
    result = sync_db.query("SELECT 1").fetchone()   # blocks, but in a thread
    return result
```

**FastAPI rule:**
- `async def` handlers run directly on the event loop — blocking them is catastrophic
- `def` (sync) handlers are automatically wrapped in `run_in_executor` by FastAPI — they run in a threadpool and do not block the event loop
- Choosing `async def` with blocking code is **worse** than `def` — `def` at least gets its own thread

---

## 9. Async Context Manager and Iterator Scenarios

### Scenario 9-A — `async with` for Resource Management

```python
import asyncio
import aiofiles

# File I/O — async context manager
async def read_file(path: str) -> str:
    async with aiofiles.open(path, "r") as f:
        return await f.read()

# Database transaction
async def transfer(from_id: int, to_id: int, amount: float):
    async with db.transaction():
        await db.execute(
            "UPDATE accounts SET balance = balance - $1 WHERE id = $2",
            [amount, from_id]
        )
        await db.execute(
            "UPDATE accounts SET balance = balance + $1 WHERE id = $2",
            [amount, to_id]
        )
```

---

### Scenario 9-B — `async for` Streaming Response

```python
# Streaming large dataset from DB — don't load all rows into memory
async def stream_users(request: Request):
    async def generate():
        async for row in db.iterate("SELECT * FROM users ORDER BY id"):
            if await request.is_disconnected():
                break   # client disconnected — stop generating
            yield json.dumps(dict(row)) + "\n"

    return StreamingResponse(generate(), media_type="application/x-ndjson")
```

---

### Scenario 9-C — Async Generator vs Coroutine

```python
# Coroutine — runs once, returns one value
async def fetch_one() -> dict:
    await asyncio.sleep(0.1)
    return {"data": "single"}

# Async generator — yields multiple values lazily
async def fetch_stream():
    for i in range(10):
        await asyncio.sleep(0.1)
        yield {"item": i}

# Consuming async generator
async def consume():
    async for item in fetch_stream():
        process(item)
```

---

## 10. Event Loop Architecture Scenarios

### Scenario 10-A — Never Block `asyncio.get_event_loop()` in Sync Code

```python
# BUG — calling async from sync can create nested loop issues
import asyncio

def sync_wrapper():
    # In Python 3.10+, asyncio.get_event_loop() may not have a running loop
    loop = asyncio.get_event_loop()
    result = loop.run_until_complete(async_function())   # Works in scripts
    return result

# BUG in FastAPI/production: there's already a running loop!
# loop.run_until_complete() raises RuntimeError if a loop is already running
```

**Fix — Use `asyncio.run()` only at the top level:**

```python
# In scripts / CLI
asyncio.run(main())   # creates a fresh event loop, runs, closes

# In Jupyter / nested event loop contexts — use nest_asyncio
import nest_asyncio
nest_asyncio.apply()   # allows nested event loops (for development only)

# In production async frameworks — never call run_until_complete
# Just await the coroutine from your async handler
```

---

### Scenario 10-B — Long CPU Task Starving the Event Loop

```python
import asyncio

# BUG — CPU-bound loop starves other coroutines
async def cpu_heavy():
    total = 0
    for i in range(10_000_000):
        total += i   # never yields — event loop is frozen
    return total

# Fix — periodically yield to event loop
async def cpu_heavy_yielding():
    total = 0
    for i in range(10_000_000):
        total += i
        if i % 10_000 == 0:
            await asyncio.sleep(0)   # yield to event loop every 10K iterations
    return total
```

**`await asyncio.sleep(0)` — suspends the current coroutine** for zero time. The event loop handles any pending I/O callbacks, then resumes the coroutine. This is the idiomatic way to yield within a long CPU loop.

**Better fix for real CPU work:** use `run_in_executor(ProcessPoolExecutor, ...)`.

---

## 11. Production Patterns Summary

### Pattern 11-A — Startup / Shutdown Resource Management

```python
from contextlib import asynccontextmanager
import httpx
from databases import Database
from fastapi import FastAPI

database: Database = None
http_client: httpx.AsyncClient = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global database, http_client
    # Startup
    database = Database("postgresql+asyncpg://user:pw@host/db", min_size=5, max_size=20)
    await database.connect()
    http_client = httpx.AsyncClient(timeout=5.0, limits=httpx.Limits(max_connections=50))

    yield   # app is running

    # Shutdown
    await database.disconnect()
    await http_client.aclose()

app = FastAPI(lifespan=lifespan)
```

---

### Pattern 11-B — Bulkhead: Isolate Slow Dependencies

```python
import asyncio

# Separate semaphores for different downstream services
_payment_semaphore = asyncio.Semaphore(5)     # payment API is slow
_inventory_semaphore = asyncio.Semaphore(20)  # inventory is fast

async def checkout(cart_id: int):
    # Run inventory check and payment concurrently but with separate limits
    async with asyncio.TaskGroup() as tg:
        inv_task = tg.create_task(_check_inventory(cart_id))
        pay_task = tg.create_task(_process_payment(cart_id))

    return {"inventory": inv_task.result(), "payment": pay_task.result()}

async def _check_inventory(cart_id: int):
    async with _inventory_semaphore:
        return await inventory_api.check(cart_id)

async def _process_payment(cart_id: int):
    async with _payment_semaphore:
        return await payment_api.charge(cart_id)
```

---

## 12. Java Developer Bridge — Async Mapping

| Asyncio Concept | Java Equivalent | Key Difference |
|---|---|---|
| `async def` | N/A (every method can block) | Python: one thread; Java: many threads |
| `await coro()` | `future.get()` or `.join()` | Java blocks the thread; Python yields |
| `asyncio.gather(*tasks)` | `CompletableFuture.allOf(...).join()` | Both wait for all; gather preserves order |
| `asyncio.wait(FIRST_COMPLETED)` | `CompletableFuture.anyOf(...)` | Both return fastest result |
| `asyncio.Semaphore(n)` | `java.util.concurrent.Semaphore(n)` | Same concept, different API |
| `asyncio.wait_for(coro, timeout)` | `future.orTimeout(n, SECONDS)` | Both cancel on breach |
| `asyncio.timeout(n)` (3.11+) | `future.orTimeout(n, SECONDS)` | More ergonomic in Python 3.11+ |
| `run_in_executor(ThreadPool)` | `CompletableFuture.supplyAsync(exec)` | Both offload to a thread pool |
| `run_in_executor(ProcessPool)` | N/A (Java threads share heap) | Bypasses GIL; Java has no GIL |
| `asyncio.TaskGroup` | `StructuredTaskScope` (Java 21) | Structured concurrency; Java arrived later |
| `CancelledError` | `CancellationException` / `InterruptedException` | Both must be re-raised after cleanup |
| `asyncio.shield(coro)` | No direct equivalent | Protects inner coro from cancellation |
| `async with` resource | `try-with-resources` | Both guarantee cleanup on exception |
| `async for` stream | `Stream.forEach` / reactive `Flux` | Python pulls; reactive is push |
| `asyncio.run()` | Spring Boot main entrypoint | Top-level entry point only |
| FastAPI `def` route | `@GetMapping` in thread pool | FastAPI wraps in executor automatically |
| FastAPI `async def` route | Reactor/Webflux handler | Both must not block |

---

## 13. Hot Interview Q&A

**Q1: Why does `time.sleep()` in an `async def` function break your entire API?**
> asyncio runs all coroutines on a single OS thread. `time.sleep()` is a blocking OS call that occupies the thread, preventing the event loop from processing any other callbacks or coroutines. Under 100 concurrent requests with 100ms sleeps, only 10 requests can be served per second. The fix is `await asyncio.sleep()` which suspends the coroutine and returns control to the event loop.

**Q2: What is the difference between `asyncio.gather` and `asyncio.wait`?**
> `gather(*coros)` returns all results in order when all tasks complete; it raises the first exception by default (or collects all with `return_exceptions=True`). `wait(tasks, return_when=...)` gives you fine-grained control — you can stop at `FIRST_COMPLETED`, `FIRST_EXCEPTION`, or `ALL_COMPLETED`, and you get `(done, pending)` sets to manage remaining tasks manually.

**Q3: When should you use `run_in_executor` with a `ThreadPoolExecutor` vs `ProcessPoolExecutor`?**
> `ThreadPoolExecutor` for I/O-bound blocking code — legacy HTTP clients, synchronous ORMs, file I/O. Threads share the GIL but the GIL is released during I/O. `ProcessPoolExecutor` for CPU-bound work — image processing, hashing, data transformation. Separate processes have their own GIL, enabling true parallelism.

**Q4: Why must `CancelledError` always be re-raised?**
> asyncio cancellation is cooperative — a `CancelledError` is sent into the coroutine at the next `await` point. If you catch it and don't re-raise, the task's caller (e.g., `wait_for`, `TaskGroup`, or a parent task) never receives the cancellation signal and may deadlock waiting for the task to end. Always do cleanup then `raise`.

**Q5: What is connection pool exhaustion and how do you prevent it?**
> A connection pool has a fixed maximum number of connections. If all connections are checked out and a new request needs one, it waits. If the wait exceeds the pool timeout, the request fails. Prevention: size the pool to match expected concurrency, add a `Semaphore` to cap in-flight DB calls before pool acquisition, and ensure connections are always returned via `async with` or `try/finally`.

**Q6: What is the difference between `async def` and `def` routes in FastAPI?**
> FastAPI wraps synchronous `def` routes in a `ThreadPoolExecutor` automatically, so blocking code doesn't block the event loop. `async def` routes run directly on the event loop thread — any blocking call in them freezes all requests. Rule: if you must use a sync library, prefer `def`; if all I/O is async, use `async def`.

**Q7: How does `asyncio.Semaphore` differ from a connection pool?**
> A connection pool manages actual reusable resources (TCP connections, DB sessions). A `Semaphore` is just a counter — it limits concurrency but doesn't hold or reuse resources. Use a `Semaphore` to cap how many coroutines simultaneously access a rate-limited external API or to prevent thundering herd against a downstream service.

---

## 14. Final Revision Checklist

- [ ] Can explain in one sentence why `requests.get()` in `async def` breaks the event loop
- [ ] Can rewrite a blocking HTTP call to use `httpx.AsyncClient` with proper lifecycle
- [ ] Can explain sequential `await` in a loop vs `asyncio.gather` with time difference
- [ ] Can use `asyncio.gather(return_exceptions=True)` to handle partial failures
- [ ] Can implement `asyncio.wait(FIRST_COMPLETED)` with proper task cancellation
- [ ] Can use `asyncio.wait_for` and `asyncio.timeout` for deadline enforcement
- [ ] Can explain why `CancelledError` must be re-raised after cleanup
- [ ] Can use `asyncio.TaskGroup` for structured concurrent fetches
- [ ] Can explain connection pool exhaustion and fix with pool size + `Semaphore`
- [ ] Can explain why shared `httpx.AsyncClient` is better than per-request client
- [ ] Can use `asyncio.Semaphore` to rate-limit concurrent calls to a third-party API
- [ ] Can use `run_in_executor` to safely call a blocking library from async code
- [ ] Can explain the difference between `ThreadPoolExecutor` and `ProcessPoolExecutor` for executors
- [ ] Can explain FastAPI `def` vs `async def` route behavior and when to use each
- [ ] Can use `await asyncio.sleep(0)` to yield control in a long CPU loop
- [ ] Can implement full startup/shutdown lifecycle with `lifespan` context manager
