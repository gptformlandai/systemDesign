# Python AsyncIO — Modern Concurrency — MAANG Master Sheet

> **Track**: Python Interview Track — Group 3: Senior MAANG  
> **File**: 2 of 4 (Track File #15)  
> **Audience**: Java developers targeting MAANG-level Python backend interviews  
> **Read after**: Python-Concurrency-Threading-Multiprocessing-MAANG-Master-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| Event loop — single thread, cooperative multitasking | ★★★★★ | Java has thread-per-request; asyncio has one event loop on one thread |
| `async def` / `await` — coroutine mechanics | ★★★★★ | Java `CompletableFuture` is callback-based; Python `await` looks synchronous |
| `asyncio.gather()` — run tasks concurrently | ★★★★★ | Java `CompletableFuture.allOf()` equivalent; most common async pattern |
| `asyncio.create_task()` — fire a background coroutine | ★★★★★ | Java `executor.submit()` equivalent for async code |
| Blocking code in async — the event loop killer | ★★★★★ | Most common async bug; single-threaded means one block stops everything |
| `async with` / `async for` — async context managers and iterators | ★★★★☆ | No Java equivalent in standard syntax; common in DB drivers, HTTP clients |
| `asyncio.Queue` — async producer-consumer | ★★★★☆ | `BlockingQueue` equivalent but async; no thread needed |
| `asyncio.wait_for()` — timeout on a coroutine | ★★★★☆ | Java `future.get(5, TimeUnit.SECONDS)` equivalent |
| `asyncio.Semaphore` — limit concurrent async tasks | ★★★★☆ | Crucial for rate-limiting outbound HTTP calls |
| Coroutine vs Future vs Task — three types | ★★★★☆ | Java has `Future` / `CompletableFuture`; Python has three distinct types |
| `asyncio.run()` — entry point | ★★★★☆ | Java main thread; `asyncio.run()` starts the event loop |
| `httpx.AsyncClient` / `aiohttp` — async HTTP | ★★★★★ | Java WebClient (WebFlux); critical for FastAPI microservice code |
| Running sync code from async — `run_in_executor` | ★★★★☆ | Must know when integrating legacy sync libraries in async code |

---

## 2. The Event Loop — Cooperative Multitasking

### Must Know

```
Java threading model:
  One thread per request (Servlet) or reactive pipeline (WebFlux).
  Threads are preemptively scheduled by the OS — any thread can be paused at any time.
  Multiple threads run truly in parallel (multiple CPU cores).

Python asyncio model:
  ONE thread. ONE event loop. Only one coroutine runs at a time.
  Cooperative multitasking — a coroutine runs until it hits an `await`.
  At `await`, control returns to the event loop, which runs another ready coroutine.
  No preemption — if a coroutine doesn't await, it runs forever (blocks everything).

Practical consequence:
  asyncio achieves HIGH CONCURRENCY (many tasks "in progress" simultaneously)
  without parallelism. Ideal for I/O-bound services: one thread handles thousands
  of simultaneous connections because each connection spends most time waiting on I/O.

  Java WebFlux (Project Reactor) works the same way:
    Mono/Flux pipelines run on an event loop thread pool.
    Non-blocking I/O is essential; blocking code must run on a separate scheduler.
```

### The Event Loop — What Actually Happens

```
Event loop iteration:
  1. Check ready callbacks — run all coroutines that are ready to continue
  2. Poll I/O — check which sockets/file descriptors have data ready
  3. For each ready I/O event, resume the coroutine that was awaiting it
  4. Process scheduled callbacks (asyncio.call_later, etc.)
  5. Repeat

When you `await asyncio.sleep(1)`:
  1. Coroutine suspends itself — yields control back to the event loop
  2. Event loop schedules a timer for 1 second
  3. Event loop runs OTHER coroutines that are ready
  4. After 1 second, event loop resumes this coroutine
  → Other coroutines ran during that 1 second — concurrency achieved!

When you call `time.sleep(1)` (blocking):
  1. Entire OS thread blocks for 1 second
  2. Event loop CANNOT run — it runs on the same thread
  3. ALL other coroutines are frozen for 1 second — NO concurrency!
```

---

## 3. Coroutines — `async def` and `await`

### Anatomy of a Coroutine

```python
import asyncio

# async def creates a coroutine function — calling it returns a coroutine OBJECT
async def fetch_user(user_id: int) -> dict:
    """This is a coroutine function."""
    await asyncio.sleep(0.1)   # Suspend — let event loop run others; resume after 0.1s
    return {"id": user_id, "name": f"User {user_id}"}

# Calling async def does NOT execute the body — returns a coroutine object
coro = fetch_user(42)
print(type(coro))   # <class 'coroutine'>
print(coro)         # <coroutine object fetch_user at 0x...>
# coro is NOT yet running!

# To run a coroutine, you must either:
# 1. await it from another coroutine
# 2. wrap it in a Task with asyncio.create_task()
# 3. run it as the top-level entry point with asyncio.run()

# Entry point
async def main():
    user = await fetch_user(42)   # await executes the coroutine and gets the result
    print(user)

asyncio.run(main())   # Starts event loop, runs main(), closes loop when main() returns
```

### `await` — What It Actually Does

```python
import asyncio

async def step_one() -> str:
    print("step_one: starting")
    await asyncio.sleep(1)       # Suspend here; let others run for 1s
    print("step_one: resumed after 1s")
    return "result_one"

async def step_two() -> str:
    print("step_two: starting")
    await asyncio.sleep(0.5)     # Suspend here for 0.5s
    print("step_two: resumed after 0.5s")
    return "result_two"

async def main():
    # SEQUENTIAL — each waits for the previous
    r1 = await step_one()    # Runs step_one; event loop can run nothing else useful here
    r2 = await step_two()    # Runs step_two after step_one fully completes
    print(r1, r2)
    # Total time: ~1.5s

asyncio.run(main())

# await X is NOT the same as blocking!
# During await asyncio.sleep(), the event loop is FREE to run other coroutines.
# But sequential awaits ARE sequentially ordered — each starts after the previous finishes.
```

---

## 4. `asyncio.create_task()` — Concurrent Execution

### Must Know

```python
import asyncio
import time

async def fetch(n: int) -> str:
    await asyncio.sleep(1)   # Simulates 1s network call
    return f"data_{n}"

# SEQUENTIAL — total ~3 seconds
async def sequential():
    r1 = await fetch(1)
    r2 = await fetch(2)
    r3 = await fetch(3)
    return [r1, r2, r3]

# CONCURRENT with create_task — total ~1 second
async def concurrent():
    # create_task() SCHEDULES the coroutine to run — starts it immediately
    # but does NOT wait for it to finish
    task1 = asyncio.create_task(fetch(1))   # Started, running in background
    task2 = asyncio.create_task(fetch(2))   # Started, running in background
    task3 = asyncio.create_task(fetch(3))   # Started, running in background

    # Now await each task — by the time we await task1, it may already be done
    r1 = await task1
    r2 = await task2
    r3 = await task3
    return [r1, r2, r3]

async def main():
    start = time.perf_counter()
    results_seq = await sequential()
    seq_time = time.perf_counter() - start
    print(f"Sequential: {seq_time:.2f}s")   # ~3.0s

    start = time.perf_counter()
    results_con = await concurrent()
    con_time = time.perf_counter() - start
    print(f"Concurrent: {con_time:.2f}s")   # ~1.0s

asyncio.run(main())
```

### Task Naming and Cancellation

```python
import asyncio

async def long_running(name: str) -> str:
    print(f"Task {name} started")
    try:
        await asyncio.sleep(10)
        return f"{name} done"
    except asyncio.CancelledError:
        print(f"Task {name} was cancelled!")
        raise   # ALWAYS re-raise CancelledError — do not swallow it

async def main():
    task = asyncio.create_task(long_running("A"), name="my-task")
    print(task.get_name())    # "my-task"

    await asyncio.sleep(1)    # Let the task run for 1 second

    task.cancel()             # Request cancellation — raises CancelledError in the task
    try:
        await task            # Wait for the task to finish (with CancelledError)
    except asyncio.CancelledError:
        print("Main: task was cancelled")

    print(f"Task done: {task.done()}")       # True
    print(f"Task cancelled: {task.cancelled()}")  # True

asyncio.run(main())

# TRAP: Never swallow CancelledError without re-raising!
# If you catch it and don't re-raise, the task appears to keep running
# and the event loop / shutdown logic breaks.
```

---

## 5. `asyncio.gather()` — Run Multiple Tasks

### Must Know

```python
import asyncio

async def fetch(n: int) -> str:
    await asyncio.sleep(1)
    return f"data_{n}"

async def main():
    # gather() runs all coroutines CONCURRENTLY; returns list of results IN ORDER
    results = await asyncio.gather(
        fetch(1),
        fetch(2),
        fetch(3),
    )
    print(results)   # ['data_1', 'data_2', 'data_3'] — in input order
    # Total time: ~1 second (all run concurrently)

asyncio.run(main())
```

### `gather()` Error Handling

```python
import asyncio

async def may_fail(n: int) -> str:
    if n == 2:
        raise ValueError(f"Task {n} failed!")
    await asyncio.sleep(0.5)
    return f"result_{n}"

async def main():
    # Default: first exception propagates, cancels remaining tasks
    try:
        results = await asyncio.gather(may_fail(1), may_fail(2), may_fail(3))
    except ValueError as e:
        print(f"Error: {e}")   # "Task 2 failed!" — task 1 and 3 may still complete

    # return_exceptions=True: exceptions are returned as values instead of raised
    # ALL tasks run to completion regardless of failures
    results = await asyncio.gather(
        may_fail(1),
        may_fail(2),
        may_fail(3),
        return_exceptions=True,
    )
    for i, r in enumerate(results, 1):
        if isinstance(r, Exception):
            print(f"Task {i} failed: {r}")
        else:
            print(f"Task {i} succeeded: {r}")
    # Output:
    # Task 1 succeeded: result_1
    # Task 2 failed: Task 2 failed!
    # Task 3 succeeded: result_3
```

### `gather()` vs `create_task()` vs `wait()`

```python
import asyncio
from asyncio import Task, FIRST_COMPLETED, FIRST_EXCEPTION, ALL_COMPLETED

async def slow(n: int) -> int:
    await asyncio.sleep(n)
    return n

async def main():
    # asyncio.gather — run all, return all results (ordered)
    results = await asyncio.gather(slow(1), slow(2), slow(3))

    # asyncio.wait — more control: return on first done, first exception, or all done
    tasks = [asyncio.create_task(slow(n)) for n in [1, 2, 3]]
    done, pending = await asyncio.wait(tasks, return_when=FIRST_COMPLETED)
    for t in done:
        print(f"First finished: {t.result()}")
    for t in pending:
        t.cancel()   # Cancel the rest

    # asyncio.wait with timeout
    tasks2 = [asyncio.create_task(slow(n)) for n in [1, 2, 10]]
    done, pending = await asyncio.wait(tasks2, timeout=1.5)
    print(f"Done: {len(done)}, Pending: {len(pending)}")  # Done: 2, Pending: 1

asyncio.run(main())
```

---

## 6. `asyncio.wait_for()` — Timeouts

```python
import asyncio

async def slow_operation() -> str:
    await asyncio.sleep(5)
    return "done"

async def main():
    # Cancel the coroutine if it takes more than 2 seconds
    try:
        result = await asyncio.wait_for(slow_operation(), timeout=2.0)
    except asyncio.TimeoutError:
        print("Operation timed out!")

    # wait_for with a task — cancels the underlying task on timeout
    task = asyncio.create_task(slow_operation())
    try:
        result = await asyncio.wait_for(task, timeout=2.0)
    except asyncio.TimeoutError:
        print("Task timed out and was cancelled")
        print(f"Task cancelled: {task.cancelled()}")   # True

    # Pattern: wrap all outbound calls with a timeout
    async def safe_fetch(url: str) -> str | None:
        try:
            return await asyncio.wait_for(fetch_url(url), timeout=5.0)
        except asyncio.TimeoutError:
            return None

asyncio.run(main())

# Java equivalent: future.get(2, TimeUnit.SECONDS)
# Raises java.util.concurrent.TimeoutException
```

---

## 7. `async with` and `async for`

### `async with` — Async Context Managers

```python
import asyncio

# async with: calls __aenter__ and __aexit__ which can be coroutines
# Used for: database connections, HTTP clients, file I/O (aiofiles), locks

# asyncio.Lock — async-safe mutex
async def with_lock_example():
    lock = asyncio.Lock()

    async with lock:   # Awaits __aenter__: acquires lock
        print("Inside lock")
        await asyncio.sleep(0.1)
    # Awaits __aexit__: releases lock — even on exception

# Custom async context manager using contextlib
from contextlib import asynccontextmanager

@asynccontextmanager
async def managed_connection(host: str):
    print(f"Connecting to {host}")
    conn = {"host": host, "id": 42}   # Fake connection
    try:
        yield conn       # Provides the connection to the `as` target
    finally:
        print(f"Closing connection to {host}")
        # await conn.aclose() in real code

async def use_connection():
    async with managed_connection("db.example.com") as conn:
        print(f"Using connection {conn['id']}")
    # Connection closed here

asyncio.run(use_connection())
```

### `async for` — Async Iterators

```python
import asyncio

# async for: calls __aiter__ and __anext__ which can be coroutines
# Used for: streaming APIs, database cursor results, WebSocket messages

class AsyncCounter:
    """Async iterator that yields numbers with delay."""

    def __init__(self, stop: int) -> None:
        self.current = 0
        self.stop = stop

    def __aiter__(self):
        return self

    async def __anext__(self) -> int:
        if self.current >= self.stop:
            raise StopAsyncIteration
        await asyncio.sleep(0.1)   # Simulate async data fetch
        value = self.current
        self.current += 1
        return value

async def main():
    async for num in AsyncCounter(5):
        print(f"Got: {num}")

asyncio.run(main())

# Async generator — simpler syntax for async iterators
async def stream_records(limit: int):
    """Async generator — like a regular generator but uses yield in async def."""
    for i in range(limit):
        await asyncio.sleep(0.1)   # Simulate fetching from DB
        yield {"id": i, "data": f"record_{i}"}

async def consume():
    async for record in stream_records(5):
        print(record)

asyncio.run(consume())

# Real-world: streaming large result sets from async DB driver
# async for row in await conn.cursor("SELECT * FROM big_table"):
#     process(row)
```

---

## 8. Asyncio Synchronization Primitives

### `asyncio.Lock` — Mutual Exclusion (Async Version)

```python
import asyncio

shared_resource = 0
lock = asyncio.Lock()

async def safe_increment():
    global shared_resource
    async with lock:            # async-safe acquire and release
        current = shared_resource
        await asyncio.sleep(0)  # Simulate yielding mid-operation
        shared_resource = current + 1

async def main():
    tasks = [asyncio.create_task(safe_increment()) for _ in range(100)]
    await asyncio.gather(*tasks)
    print(shared_resource)   # Always 100

# asyncio.Lock vs threading.Lock:
# threading.Lock: works across threads; can be acquired from different OS threads
# asyncio.Lock: works across coroutines; can ONLY be used inside async code
# NEVER use threading.Lock in async code — it blocks the event loop on acquire!
```

### `asyncio.Semaphore` — Concurrency Limiter

```python
import asyncio
import httpx

# Rate-limit outbound HTTP calls to max 10 concurrent
sem = asyncio.Semaphore(10)

async def fetch_with_limit(client: httpx.AsyncClient, url: str) -> str:
    async with sem:   # At most 10 coroutines in this block simultaneously
        response = await client.get(url)
        return response.text

async def main():
    urls = [f"https://api.example.com/item/{i}" for i in range(100)]
    async with httpx.AsyncClient() as client:
        tasks = [fetch_with_limit(client, url) for url in urls]
        results = await asyncio.gather(*tasks)   # 100 tasks, max 10 concurrent
    print(f"Fetched {len(results)} items")

# This is the MOST IMPORTANT async pattern for backend MAANG interviews:
# "How do you rate-limit 1000 concurrent API calls to max N?"
# Answer: asyncio.Semaphore + asyncio.gather()
```

### `asyncio.Event` and `asyncio.Queue`

```python
import asyncio

# asyncio.Event — async version of threading.Event
event = asyncio.Event()

async def waiter():
    print("Waiter: waiting for event...")
    await event.wait()      # Suspends until event is set
    print("Waiter: event received!")

async def setter():
    await asyncio.sleep(1)
    print("Setter: setting event")
    event.set()

async def main():
    await asyncio.gather(waiter(), setter())

asyncio.run(main())

# asyncio.Queue — async producer-consumer
async def producer(q: asyncio.Queue) -> None:
    for i in range(10):
        await q.put(i)
        print(f"Produced: {i}")
        await asyncio.sleep(0.1)
    await q.put(None)   # Sentinel

async def consumer(q: asyncio.Queue, name: str) -> None:
    while True:
        item = await q.get()    # Suspends until item available (non-blocking to event loop)
        if item is None:
            await q.put(None)   # Re-queue sentinel for other consumers
            break
        print(f"Consumer {name}: got {item}")
        q.task_done()           # Signal item processing complete

async def main2():
    q: asyncio.Queue[int | None] = asyncio.Queue(maxsize=5)   # Bounded queue
    await asyncio.gather(
        producer(q),
        consumer(q, "A"),
        consumer(q, "B"),
    )

asyncio.run(main2())
```

---

## 9. Async HTTP — `httpx` and `aiohttp`

### `httpx.AsyncClient` — Recommended for FastAPI

```python
import asyncio
import httpx
from typing import Any

# httpx is the recommended async HTTP client for FastAPI ecosystems
# httpx.AsyncClient should be created ONCE per application (connection pooling)

# Per-request client (simple but inefficient)
async def fetch_single(url: str) -> dict:
    async with httpx.AsyncClient() as client:
        response = await client.get(url, timeout=10.0)
        response.raise_for_status()   # Raises HTTPStatusError for 4xx/5xx
        return response.json()

# Application-level client (efficient — reuses connections)
class APIClient:
    def __init__(self, base_url: str) -> None:
        self._client = httpx.AsyncClient(
            base_url=base_url,
            timeout=httpx.Timeout(connect=5.0, read=30.0, write=10.0, pool=5.0),
            headers={"User-Agent": "MyService/1.0"},
            limits=httpx.Limits(max_connections=100, max_keepalive_connections=20),
        )

    async def get_user(self, user_id: int) -> dict[str, Any]:
        response = await self._client.get(f"/users/{user_id}")
        response.raise_for_status()
        return response.json()

    async def create_order(self, payload: dict) -> dict[str, Any]:
        response = await self._client.post("/orders", json=payload)
        response.raise_for_status()
        return response.json()

    async def close(self) -> None:
        await self._client.aclose()

# In FastAPI with lifespan:
from contextlib import asynccontextmanager
from fastapi import FastAPI

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.api_client = APIClient("https://api.example.com")
    yield
    await app.state.api_client.close()

app = FastAPI(lifespan=lifespan)
```

### Concurrent HTTP Requests Pattern

```python
import asyncio
import httpx

async def fetch_all_users(user_ids: list[int]) -> list[dict]:
    """Fetch multiple users concurrently with rate limiting."""
    sem = asyncio.Semaphore(20)   # Max 20 concurrent requests

    async def fetch_one(client: httpx.AsyncClient, uid: int) -> dict:
        async with sem:
            resp = await client.get(f"https://api.example.com/users/{uid}")
            resp.raise_for_status()
            return resp.json()

    async with httpx.AsyncClient(timeout=30.0) as client:
        tasks = [fetch_one(client, uid) for uid in user_ids]
        results = await asyncio.gather(*tasks, return_exceptions=True)

    # Separate successes from failures
    users = []
    for uid, result in zip(user_ids, results):
        if isinstance(result, Exception):
            print(f"Failed to fetch user {uid}: {result}")
        else:
            users.append(result)
    return users

# This pattern handles:
# - Concurrency (all requests start together)
# - Rate limiting (max 20 at a time via Semaphore)
# - Per-item error handling (return_exceptions=True)
# - Connection pooling (single AsyncClient reused)
```

### `aiohttp` — Alternative Client/Server

```python
import asyncio
import aiohttp

# aiohttp: older library, also widely used
# httpx is generally preferred for new code (closer to requests API)

async def fetch_with_aiohttp(url: str) -> str:
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as response:
            response.raise_for_status()
            return await response.text()

# aiohttp also has a server component (WSGI-less alternative to Flask/FastAPI)
# but FastAPI + Uvicorn is now the standard
```

---

## 10. Running Blocking Code from Async

### Must Know

```python
import asyncio
import time

# THE MOST COMMON ASYNC BUG:
# Calling blocking code directly in an async function freezes the event loop

async def bad_endpoint() -> str:
    time.sleep(2)        # BLOCKS event loop for 2 seconds — all requests frozen!
    return "done"

# FIX 1: asyncio.to_thread (Python 3.9+) — preferred
async def good_endpoint_v1() -> str:
    result = await asyncio.to_thread(time.sleep, 2)  # Runs in threadpool; event loop free
    return "done"

# FIX 2: loop.run_in_executor — explicit; works on Python 3.7+
async def good_endpoint_v2() -> str:
    loop = asyncio.get_running_loop()
    await loop.run_in_executor(None, time.sleep, 2)   # None = default ThreadPoolExecutor
    return "done"

# FIX 3: custom executor
from concurrent.futures import ThreadPoolExecutor

executor = ThreadPoolExecutor(max_workers=10)

async def good_endpoint_v3() -> str:
    loop = asyncio.get_running_loop()
    await loop.run_in_executor(executor, time.sleep, 2)
    return "done"
```

### What Qualifies as "Blocking"

```python
# BLOCKING — must use run_in_executor or asyncio.to_thread:
# - time.sleep()
# - requests.get()         (use httpx.AsyncClient instead)
# - psycopg2 db queries    (use asyncpg or SQLAlchemy async instead)
# - open() + read/write    (use aiofiles library instead)
# - CPU-intensive loops    (use ProcessPoolExecutor instead)
# - subprocess.run()       (use asyncio.create_subprocess_exec instead)
# - boto3 S3 operations    (use aioboto3 instead)

# NON-BLOCKING — safe to await directly:
# - await asyncio.sleep()
# - await httpx.AsyncClient.get()
# - await asyncpg connection.execute()
# - await aiofiles.open()
# - await asyncio.create_subprocess_exec()

# Detecting blocking code: use asyncio debug mode
import asyncio
asyncio.run(main(), debug=True)   # Warns if a coroutine takes too long without yielding
```

---

## 11. Async Patterns for Production

### Pattern 1 — Retry with Exponential Backoff

```python
import asyncio
import httpx
from typing import TypeVar, Callable, Awaitable
import random

T = TypeVar("T")

async def retry_async(
    func: Callable[[], Awaitable[T]],
    max_retries: int = 3,
    base_delay: float = 1.0,
    jitter: bool = True,
) -> T:
    """Retry an async callable with exponential backoff."""
    last_exception: Exception | None = None
    for attempt in range(max_retries + 1):
        try:
            return await func()
        except (httpx.TransportError, httpx.TimeoutException) as e:
            last_exception = e
            if attempt == max_retries:
                break
            delay = base_delay * (2 ** attempt)
            if jitter:
                delay += random.uniform(0, delay * 0.1)
            print(f"Attempt {attempt + 1} failed: {e}. Retrying in {delay:.2f}s...")
            await asyncio.sleep(delay)
    raise last_exception   # type: ignore

# Usage
async def main():
    async with httpx.AsyncClient() as client:
        result = await retry_async(
            lambda: client.get("https://api.example.com/data"),
            max_retries=3,
        )
```

### Pattern 2 — Task Group (Python 3.11+)

```python
import asyncio

async def fetch(n: int) -> str:
    await asyncio.sleep(1)
    if n == 2:
        raise ValueError("Simulated failure")
    return f"data_{n}"

# TaskGroup: if any task fails, all others are cancelled immediately
# Cleaner than gather() for structured concurrency
async def main():
    try:
        async with asyncio.TaskGroup() as tg:
            task1 = tg.create_task(fetch(1))
            task2 = tg.create_task(fetch(2))   # This fails
            task3 = tg.create_task(fetch(3))   # This is cancelled when task2 fails
        # Reached only if ALL tasks succeed
    except* ValueError as eg:   # except* — exception group syntax (Python 3.11+)
        print(f"Some tasks failed: {eg.exceptions}")

# Java: CompletableFuture.allOf() with exceptional handling
```

### Pattern 3 — Circuit Breaker Async

```python
import asyncio
from enum import Enum
from datetime import datetime, timedelta

class CircuitState(Enum):
    CLOSED = "closed"       # Normal operation
    OPEN = "open"           # Failing — reject all requests
    HALF_OPEN = "half_open" # Testing — allow one request

class AsyncCircuitBreaker:
    def __init__(self, failure_threshold: int = 5, reset_timeout: float = 60.0):
        self.state = CircuitState.CLOSED
        self.failure_count = 0
        self.failure_threshold = failure_threshold
        self.reset_timeout = reset_timeout
        self.last_failure_time: datetime | None = None

    async def call(self, func, *args, **kwargs):
        if self.state == CircuitState.OPEN:
            if self._should_attempt_reset():
                self.state = CircuitState.HALF_OPEN
            else:
                raise Exception("Circuit breaker is OPEN — fast fail")

        try:
            result = await func(*args, **kwargs)
            self._on_success()
            return result
        except Exception as e:
            self._on_failure()
            raise

    def _should_attempt_reset(self) -> bool:
        if self.last_failure_time is None:
            return False
        return datetime.now() > self.last_failure_time + timedelta(seconds=self.reset_timeout)

    def _on_success(self) -> None:
        self.failure_count = 0
        self.state = CircuitState.CLOSED

    def _on_failure(self) -> None:
        self.failure_count += 1
        self.last_failure_time = datetime.now()
        if self.failure_count >= self.failure_threshold:
            self.state = CircuitState.OPEN
```

### Pattern 4 — Bounded Concurrent Processing

```python
import asyncio
from collections.abc import AsyncIterator

async def process_stream(
    items: list[dict],
    max_concurrent: int = 50,
) -> list[dict]:
    """Process items concurrently, bounded by max_concurrent."""
    sem = asyncio.Semaphore(max_concurrent)
    results = []

    async def process_one(item: dict) -> dict:
        async with sem:
            # Simulate async processing
            await asyncio.sleep(0.1)
            return {**item, "processed": True}

    tasks = [asyncio.create_task(process_one(item)) for item in items]
    results = await asyncio.gather(*tasks, return_exceptions=True)

    return [r for r in results if not isinstance(r, Exception)]
```

---

## 12. `asyncio.run()` and Event Loop API

```python
import asyncio

# asyncio.run() — the standard entry point (Python 3.7+)
# Creates a new event loop, runs the coroutine, closes the loop when done
asyncio.run(main())
asyncio.run(main(), debug=True)   # Enables debug mode

# Inside an already-running event loop (e.g., in a Jupyter notebook or FastAPI):
# asyncio.run() cannot be called — use await directly

# Getting the running loop
async def inside_async():
    loop = asyncio.get_running_loop()   # Preferred — raises if no loop running
    loop2 = asyncio.get_event_loop()    # Deprecated pattern — may create a new loop

# Scheduling from sync code (rare but needed in some frameworks)
async def delayed():
    await asyncio.sleep(1)
    print("delayed!")

loop = asyncio.new_event_loop()
loop.run_until_complete(delayed())
loop.close()

# asyncio.current_task() — get the currently running Task object
async def who_am_i():
    task = asyncio.current_task()
    print(f"Running as task: {task.get_name()}")

# asyncio.all_tasks() — get all pending tasks
async def show_tasks():
    tasks = asyncio.all_tasks()
    for t in tasks:
        print(f"  {t.get_name()}: {t.done()}")
```

---

## 13. Java Developer Bridge — Complete Comparison

| Concept | Java (Spring WebFlux / CompletableFuture) | Python asyncio |
|---|---|---|
| Programming model | Reactive streams (Mono/Flux) or CompletableFuture | Coroutines with async/await |
| Syntax style | Callback chains `.thenApply()` / `.flatMap()` | Sequential-looking `await` |
| Run a task | `CompletableFuture.supplyAsync(callable)` | `asyncio.create_task(coro)` |
| Get result | `future.get()` (blocking) | `await task` |
| Run multiple | `CompletableFuture.allOf(f1, f2, f3)` | `await asyncio.gather(c1, c2, c3)` |
| First completed | `CompletableFuture.anyOf(...)` | `await asyncio.wait(..., FIRST_COMPLETED)` |
| Timeout | `future.get(5, SECONDS)` | `await asyncio.wait_for(coro, timeout=5)` |
| Async lock | `ReentrantLock` (thread-based) | `asyncio.Lock()` (coroutine-based) |
| Async semaphore | `Semaphore` (thread-based) | `asyncio.Semaphore(n)` (coroutine-based) |
| Async queue | `LinkedBlockingQueue` (thread-based) | `asyncio.Queue()` (coroutine-based) |
| Stream/pipe | `Flux<T>` | `async for item in async_generator:` |
| Cancel a task | `future.cancel()` | `task.cancel()` → `CancelledError` |
| Blocking I/O guard | `subscribeOn(Schedulers.boundedElastic())` | `asyncio.to_thread(blocking_fn)` |
| HTTP client | `WebClient` (WebFlux) | `httpx.AsyncClient` |
| Event loop | Netty event loop (Project Reactor) | `asyncio` event loop |
| Thread model | Multiple reactor threads | Single event loop thread |
| Entry point | Spring Boot main thread → Netty | `asyncio.run(main())` |
| Debug mode | Reactor Debug Agent | `asyncio.run(main(), debug=True)` |
| Task group | `CompletableFuture.allOf()` | `asyncio.TaskGroup` (Python 3.11+) |
| Exception in gather | One exception propagates | `return_exceptions=True` for all-complete |

---

## 14. Hot Interview Q&A

**Q: How does asyncio achieve concurrency with only one thread?**  
A: Asyncio uses cooperative multitasking. When a coroutine hits an `await` expression on an I/O operation, it voluntarily suspends itself and returns control to the event loop. The event loop then runs any other coroutines that are ready. When the I/O completes (detected by the OS via `select`/`epoll`/`kqueue`), the event loop resumes the suspended coroutine. At any instant, only one coroutine is executing, but many can be "in flight" — suspended while waiting for I/O. This is identical to how Java's Project Reactor and Node.js work.

**Q: What is the difference between `asyncio.gather()` and `asyncio.create_task()`?**  
A: `create_task()` schedules a single coroutine as a background Task and returns the Task object immediately — the coroutine starts running concurrently. You can later `await` the task. `gather()` takes multiple coroutines/tasks, wraps them in tasks if needed, runs them all concurrently, and returns a single awaitable that resolves to a list of all results (in input order). `gather()` is `create_task()` + `await all` in one call. Use `create_task()` when you need individual task control (cancel, check status); use `gather()` when you want all results at once.

**Q: What is the danger of calling `time.sleep()` inside an async function?**  
A: `time.sleep()` blocks the OS thread. Since asyncio runs on a single thread, blocking that thread freezes the entire event loop — no other coroutines can run, no I/O is processed, no requests are served. With 1000 concurrent users, a single `time.sleep(1)` call blocks all 1000. The fix is `await asyncio.sleep(1)` (non-blocking — suspends the coroutine, frees the event loop). For blocking third-party code, use `await asyncio.to_thread(blocking_fn)` to run it in a thread pool.

**Q: How should you handle exceptions in `asyncio.gather()`?**  
A: By default, the first exception from any gathered coroutine is re-raised immediately, and remaining tasks are cancelled. This is appropriate when all tasks must succeed or nothing should proceed. For independent tasks where you want all to run regardless of failures, pass `return_exceptions=True` — exceptions are returned as values in the results list alongside successful returns. You then check `isinstance(result, Exception)` to separate successes from failures. For Python 3.11+, `asyncio.TaskGroup` provides structured concurrency — any failure cancels all tasks and collects all exceptions.

**Q: What is `asyncio.Semaphore` and why is it critical for outbound API calls?**  
A: `asyncio.Semaphore(n)` limits the number of coroutines that can execute a section of code concurrently. Without it, `asyncio.gather()` with 1000 tasks fires all 1000 HTTP requests simultaneously — overwhelming the target service, exhausting local port/socket limits, and hitting rate limits. Wrapping the request in `async with semaphore:` ensures at most `n` requests are in flight at any time. This is the async equivalent of a connection pool or a `ThreadPoolExecutor` with a bounded worker count.

**Q: How do you use a synchronous (blocking) library from async code?**  
A: Wrap it with `await asyncio.to_thread(blocking_func, *args)` (Python 3.9+) or `await loop.run_in_executor(None, blocking_func, *args)` (older style). Both run the blocking function in a thread pool — the event loop is free to run other coroutines while the blocking call executes in a background thread. This is the async equivalent of Spring WebFlux's `subscribeOn(Schedulers.boundedElastic())`. Common cases: boto3 S3 calls, psycopg2 queries, CPU-intensive pure Python code, and any `requests` call.

**Q: What is `CancelledError` and why must you always re-raise it?**  
A: `asyncio.CancelledError` is raised in a coroutine when `task.cancel()` is called. It derives from `BaseException` (not `Exception`) so it is not caught by bare `except Exception:`. When you catch it to do cleanup (close connections, etc.), you MUST re-raise it after cleanup. If you swallow it, the task appears to keep running from the scheduler's perspective, but it's actually stuck — this breaks cooperative cancellation, prevents proper event loop shutdown, and causes resource leaks. This is analogous to Java's `InterruptedException` — always re-interrupt the thread after handling.

**Q: What is the difference between `asyncio.Lock` and `threading.Lock`?**  
A: Both are mutexes — only one acquirer runs the protected section at a time. `threading.Lock` works across OS threads — `lock.acquire()` blocks the calling thread. `asyncio.Lock` works across coroutines — `await lock.acquire()` (or `async with lock:`) suspends the coroutine but does NOT block the event loop. Critically, using `threading.Lock` inside async code is dangerous: `lock.acquire()` blocks the thread, which blocks the event loop, which freezes all coroutines. Always use `asyncio.Lock` inside async code.

---

## 15. Final Revision Checklist

### Event Loop Fundamentals

- [ ] I know asyncio uses a single thread with cooperative multitasking — NOT parallelism
- [ ] I know the event loop runs while coroutines `await` I/O; blocking code freezes it
- [ ] I know `asyncio.run(main())` is the standard entry point — creates and closes the loop

### Coroutines and Tasks

- [ ] I know `async def` returns a coroutine object — calling it does NOT execute it
- [ ] I know `await coro` executes the coroutine and suspends the caller until done
- [ ] I know `asyncio.create_task(coro)` schedules for concurrent execution — returns Task
- [ ] I can cancel a task with `task.cancel()` and always re-raise `CancelledError`

### `gather` and Concurrency

- [ ] I know `await asyncio.gather(c1, c2, c3)` runs all concurrently; returns ordered results
- [ ] I know `return_exceptions=True` lets all gather tasks complete despite failures
- [ ] I know `asyncio.wait_for(coro, timeout=N)` raises `asyncio.TimeoutError` on timeout
- [ ] I know `asyncio.wait(..., return_when=FIRST_COMPLETED)` for partial results

### Async Primitives

- [ ] I use `asyncio.Lock()` (NOT `threading.Lock`) inside async code
- [ ] I use `asyncio.Semaphore(n)` to rate-limit concurrent outbound calls
- [ ] I use `asyncio.Queue` for async producer-consumer — no threads needed
- [ ] I use `asyncio.Event` for coroutine signaling

### HTTP and I/O

- [ ] I use `httpx.AsyncClient` for async HTTP; create ONE client per app (connection pool)
- [ ] I wrap blocking calls with `await asyncio.to_thread(fn)` — never call them directly
- [ ] I use `async with` for connections, locks, HTTP clients
- [ ] I use `async for` for streaming results from DB cursors or HTTP streams

### Patterns

- [ ] I know Semaphore + gather for bounded concurrent processing (rate limiting)
- [ ] I know retry with exponential backoff using `await asyncio.sleep(delay)` in a loop
- [ ] I know `asyncio.TaskGroup` (3.11+) for structured concurrency with auto-cancellation

### Java Developer Reminders

- [ ] `CompletableFuture.supplyAsync()` → `asyncio.create_task()`
- [ ] `CompletableFuture.allOf()` → `await asyncio.gather()`
- [ ] `future.get(5, SECONDS)` → `await asyncio.wait_for(task, timeout=5)`
- [ ] `WebClient` (WebFlux) → `httpx.AsyncClient`
- [ ] `subscribeOn(Schedulers.boundedElastic())` → `await asyncio.to_thread(blocking_fn)`
- [ ] `ReentrantLock` inside async → use `asyncio.Lock()`, NOT `threading.Lock()`

---

*File 2 of 4 — Group 3: Senior MAANG*  
*Next: Python-Performance-Profiling-Debugging-MAANG-Master-Sheet.md*
