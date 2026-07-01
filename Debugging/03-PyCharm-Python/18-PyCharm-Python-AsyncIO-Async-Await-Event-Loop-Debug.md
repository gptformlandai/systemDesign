# 18. PyCharm: asyncio, Async/Await, Event Loop Debug

## Goal

Debug Python async code — asyncio debug mode, stepping through coroutines, inspecting the event loop, and debugging FastAPI async routes.

---

## asyncio Debug Mode

Enable asyncio debug mode to get extra logging and validation:

```python
import asyncio
import logging

logging.basicConfig(level=logging.DEBUG)
asyncio.run(main(), debug=True)
```

Or via environment:

```bash
PYTHONASYNCIODEBUG=1 python app.py
```

asyncio debug mode enables:
- Warning for slow callbacks (>100ms)
- Traceback for coroutines that are never awaited
- Warning for unawaited coroutines

---

## PyCharm Run Config With asyncio Debug

```json
Environment variables in PyCharm Run/Debug Configuration:
  PYTHONASYNCIODEBUG=1
  PYTHONTRACEMALLOC=1
```

---

## Stepping Through Coroutines

```python
import asyncio

async def fetch_order(order_id: str):
    # <- breakpoint: pauses in this coroutine
    await asyncio.sleep(0.1)  # simulates I/O
    return {'id': order_id, 'status': 'PENDING'}

async def process_orders(ids: list[str]):
    # <- breakpoint: inspect ids list
    tasks = [fetch_order(oid) for oid in ids]
    results = await asyncio.gather(*tasks)
    # <- breakpoint: inspect results after all tasks complete
    return results

asyncio.run(process_orders(['ORD-1', 'ORD-2', 'ORD-3']))
```

### What PyCharm Shows In The Frames Panel

When paused inside `fetch_order`:

```text
Frames:
  fetch_order (OrderService.py:10)       <- current coroutine frame
  process_orders (OrderService.py:16)    <- caller coroutine (awaiting)
  asyncio.run (asyncio/runners.py)
  <module> (main.py:1)
```

This is the async call stack — PyCharm shows coroutine frames, not thread frames.

---

## asyncio.gather Debug

```python
async def process_orders():
    # gather runs all coroutines concurrently.
    results = await asyncio.gather(
        fetch_order('ORD-1'),   # <- set breakpoint inside each
        fetch_order('ORD-2'),
        fetch_order('ORD-3'),
    )
    return results
```

Each call to `fetch_order` runs as a separate Task on the event loop. PyCharm shows the frame of whichever task is currently paused.

---

## asyncio.create_task vs await

```python
async def main():
    # Option 1: create_task (schedules immediately, runs when event loop is free).
    task = asyncio.create_task(fetch_order('ORD-1'))
    # <- here: task is SCHEDULED but may not have run yet
    
    # Option 2: await directly (runs now, waits for result).
    result = await fetch_order('ORD-2')
    # <- here: ORD-2 is complete
    
    # Wait for the task created earlier.
    result1 = await task
    # <- here: ORD-1 is complete
```

In PyCharm Evaluate Expression when paused:

```python
# Inspect all running tasks.
asyncio.all_tasks()  # set of all scheduled Tasks

# Inspect current task.
asyncio.current_task()  # the Task currently executing
asyncio.current_task().get_name()

# Get coro name.
asyncio.current_task().get_coro().__name__
```

---

## Event Loop State Inspection

```python
# In Evaluate Expression:
loop = asyncio.get_event_loop()
loop.is_running()          # True when called from inside a coroutine
loop.is_closed()           # True after loop.close()
loop._ready               # Queue of callbacks ready to run (private)
loop._scheduled           # Heap of scheduled callbacks
len(asyncio.all_tasks())  # Number of active tasks
```

---

## FastAPI Async Route Debug

```python
from fastapi import FastAPI
app = FastAPI()

@app.get("/orders/{order_id}")
async def get_order(order_id: str):
    # <- breakpoint: runs in an async context inside uvicorn's event loop
    order = await order_service.get(order_id)
    return order
```

PyCharm Run/Debug Configuration for FastAPI:

```text
+ -> Python

Script path: leave blank
Module name: uvicorn
Parameters:  main:app --host 0.0.0.0 --port 8000
  (do NOT add --reload)
Environment: PYTHONPATH=/path/to/project
```

Or via Run Configuration type if you have the FastAPI plugin.

---

## Async Generator Debug

```python
async def order_stream(start_id: int):
    for i in range(start_id, start_id + 10):
        await asyncio.sleep(0.01)
        yield {'id': f'ORD-{i}'}  # <- breakpoint: inspect each yielded item

async def consume():
    async for order in order_stream(1):
        # <- breakpoint: inspect each consumed order
        process(order)
```

---

## Debugging Unawaited Coroutines

A common asyncio bug is calling a coroutine without `await`:

```python
async def save_order(order):
    await db.insert(order)

async def handler(request):
    order = parse_request(request)
    save_order(order)   # BUG: missing await — coroutine never runs
    return Response(200)
```

With `PYTHONASYNCIODEBUG=1`:

```text
RuntimeWarning: coroutine 'save_order' was never awaited
  Coroutine created at save_order (handler.py:10)
```

---

## Interview Sound Bite

Python asyncio uses a single-threaded event loop that multiplexes coroutines — not threads. PyCharm shows coroutine frames in the Frames panel when paused inside an async function. `asyncio.all_tasks()` and `asyncio.current_task()` in Evaluate Expression show the complete task state. FastAPI must not use `--reload` because it forks a subprocess not connected to the debugger. Enable `PYTHONASYNCIODEBUG=1` to catch unawaited coroutines and slow callbacks — the most common asyncio bugs. `asyncio.gather` runs coroutines concurrently; step over it to see all results at once; step into individual coroutines to trace each.
