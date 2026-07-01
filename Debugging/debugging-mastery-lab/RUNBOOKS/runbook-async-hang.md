# Runbook: Async Hang Investigation (Node.js / Python asyncio)

## When To Use This Runbook

- Request/response hangs indefinitely (no response, no timeout, no error log)
- Background job never completes
- Health check passes but requests do not complete
- Low CPU but no progress

---

## Part A: Node.js Async Hang

### Step 1: Check Event Loop State

```bash
# Enable inspector on running process without restart.
kill -USR1 <PID>

# Open Chrome: chrome://inspect → target → click Profiler tab.
# Start CPU profiling → wait 5 seconds → stop.
# Flame graph: look for function taking 100% of the recorded time.
```

### Step 2: Check For Blocked Event Loop

```bash
# clinic.js provides event loop delay metric.
npm install -g clinic
clinic doctor -- node server.js

# Under load, if event loop delay > 100ms: something is blocking the loop.
# Common causes:
#   JSON.parse() on huge payload
#   Synchronous file read
#   CPU-intensive computation
#   Infinite loop or tight synchronous loop
```

### Step 3: Check For Unresolved Promises

```javascript
// Add to server temporarily for investigation.
let pendingRequests = 0;

app.use((req, res, next) => {
    pendingRequests++;
    res.on('finish', () => pendingRequests--);
    res.on('close', () => pendingRequests--);
    next();
});

// Check via debug console or route:
app.get('/debug/pending', (req, res) => {
    res.json({ pendingRequests });
});
```

### Step 4: Find Unresolved Promises

```javascript
// Node.js tracks active handles and requests.
// Run in debug console:
process._getActiveHandles()  // open handles (sockets, timers, file descriptors)
process._getActiveRequests() // pending libuv requests
```

### Step 5: Fix

```javascript
// Pattern 1: Add timeout to all outgoing requests.
const response = await fetch(url, { signal: AbortSignal.timeout(5000) });

// Pattern 2: Wrap Express async routes.
const asyncHandler = fn => (req, res, next) =>
    Promise.resolve(fn(req, res, next)).catch(next);

// Pattern 3: Add response timeout middleware.
app.use((req, res, next) => {
    res.setTimeout(30000, () => {
        res.status(408).json({ error: 'Request Timeout' });
    });
    next();
});
```

---

## Part B: Python asyncio Hang

### Step 1: Check What The Event Loop Is Doing

```bash
# py-spy shows async Python state without restart.
py-spy dump --pid <PID>
```

Look for the main thread stack:
```text
Thread 0:
  File "asyncio/base_events.py", line 603, in _run_once
  File "asyncio/events.py", line 80, in _run
```
This is the event loop running normally.

If you see:
```text
Thread 0:
  File "time.sleep" (native)
```
The event loop is blocked by a synchronous sleep — serious blocking bug.

### Step 2: List Active Tasks

```python
# Add a debug endpoint to FastAPI/Django:
import asyncio
from fastapi import FastAPI

@app.get("/debug/tasks")
async def list_tasks():
    tasks = asyncio.all_tasks()
    return {
        "count": len(tasks),
        "tasks": [
            {
                "name": t.get_name(),
                "coro": t.get_coro().__name__,
                "done": t.done(),
                "cancelled": t.cancelled()
            }
            for t in tasks
        ]
    }
```

### Step 3: Find Blocking Calls In Async Code

```python
# Common blocking calls in async context (will hang the event loop):
time.sleep(5)              # blocks; use await asyncio.sleep(5)
requests.get(url)          # blocks; use await httpx.AsyncClient().get(url)
open(file).read()          # blocks; use aiofiles
subprocess.run(cmd)        # blocks; use await asyncio.create_subprocess_exec(cmd)
```

### Step 4: Fix

```python
# Use asyncio.wait_for() to add timeouts to coroutines.
try:
    result = await asyncio.wait_for(
        slow_operation(),
        timeout=5.0  # seconds
    )
except asyncio.TimeoutError:
    raise HTTPException(status_code=408, detail="Operation timed out")

# Offload blocking calls to a thread pool.
import asyncio
loop = asyncio.get_event_loop()
result = await loop.run_in_executor(None, blocking_function, arg1, arg2)
# None = use default ThreadPoolExecutor
```

---

## Prevention Checklist

### Node.js

- [ ] All async routes wrapped in try/catch with `next(err)`
- [ ] Timeouts on all outgoing HTTP/DB calls
- [ ] Response timeout middleware active
- [ ] No `JSON.parse(largeString)` in request handlers (offload to worker)
- [ ] `process.on('unhandledRejection')` handler logs and alerts

### Python asyncio

- [ ] No `time.sleep()` in async functions (use `await asyncio.sleep()`)
- [ ] No synchronous `requests` calls in async context (use httpx or aiohttp)
- [ ] `asyncio.wait_for()` used for all operations that might hang
- [ ] `PYTHONASYNCIODEBUG=1` in development to catch blocking calls
- [ ] `/debug/tasks` endpoint available in staging to inspect active tasks
