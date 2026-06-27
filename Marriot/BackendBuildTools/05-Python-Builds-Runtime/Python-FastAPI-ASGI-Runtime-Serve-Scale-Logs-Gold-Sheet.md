# Python FastAPI, ASGI Runtime, Serve, Scale, Logs - Gold Sheet

> Goal: understand what happens after Python code is packaged and the backend service actually starts serving traffic.

---

## 1. Intuition

FastAPI is your application logic. Uvicorn or another ASGI server is the runtime server that speaks to the network.

```text
Client
  -> TCP socket
  -> ASGI server
  -> ASGI messages
  -> FastAPI middleware
  -> route handler
  -> response
```

Beginner mental model:

> FastAPI defines what should happen for `/orders`. Uvicorn listens on a port and calls FastAPI when a request arrives.

---

## 2. Definition

- Definition: ASGI is an asynchronous Python server/application interface for HTTP, WebSocket, and other protocols.
- Category: Runtime interface.
- Core idea: protocol servers and Python apps communicate through async events.

---

## 3. ASGI vs WSGI

| Area | WSGI | ASGI |
|---|---|---|
| Model | synchronous request/response | asynchronous event messages |
| WebSockets | not native | native-friendly |
| Long-lived connections | awkward | designed for it |
| Frameworks | Flask, Django classic | FastAPI, Starlette, Django Channels |
| Server examples | Gunicorn sync workers, uWSGI | Uvicorn, Hypercorn, Daphne |

Why ASGI matters:

- supports async request handlers
- supports WebSockets
- handles long-lived connections better
- matches modern event-driven Python servers

Mistake:

- Thinking `async def` automatically makes slow code fast. If the handler calls blocking I/O, it can still block the event loop.

---

## 4. FastAPI Request Lifecycle

```text
1. Client sends request
2. Uvicorn accepts socket
3. Uvicorn parses HTTP protocol
4. Uvicorn creates ASGI scope
5. FastAPI receives scope, receive, send
6. Middleware runs
7. Router matches path and method
8. Dependencies are resolved
9. Request body is validated by Pydantic
10. Handler executes
11. Response model is serialized
12. Middleware post-processing runs
13. Uvicorn writes response to socket
```

Flow diagram:

```text
HTTP request
  -> Uvicorn
  -> ASGI scope
  -> FastAPI app
  -> middleware
  -> dependency injection
  -> endpoint function
  -> response serialization
  -> ASGI send
  -> network response
```

---

## 5. Minimal FastAPI App

```python
from fastapi import FastAPI

app = FastAPI()


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/orders/{order_id}")
async def get_order(order_id: str):
    return {"order_id": order_id, "status": "created"}
```

Run locally:

```bash
uvicorn app.main:app --reload --port 8000
```

Meaning:

```text
app.main:app
  app/main.py file
  app variable inside that file
```

Common mistake:

```bash
uvicorn main.py
```

Better:

```bash
uvicorn app.main:app
```

The ASGI server needs an import path to the application object, not just a filename.

---

## 6. Startup and Shutdown Lifespan

Production services often need setup and cleanup:

- DB connection pools
- Redis clients
- model loading
- config validation
- telemetry initialization
- graceful shutdown

Example:

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.ready = False
    app.state.db = await create_db_pool()
    app.state.ready = True
    try:
        yield
    finally:
        await app.state.db.close()


app = FastAPI(lifespan=lifespan)
```

Lifecycle:

```text
process starts
  -> import app
  -> run lifespan startup
  -> service becomes ready
  -> handle requests
  -> receive shutdown signal
  -> stop accepting new work
  -> run lifespan cleanup
  -> process exits
```

Mistakes:

- initializing expensive clients inside every request
- not closing DB pools
- marking service ready before required dependencies connect
- running migrations in every replica during startup without coordination

---

## 7. Reading FastAPI and Uvicorn Startup Logs

Typical local startup:

```text
INFO:     Will watch for changes in these directories: ['/app']
INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
INFO:     Started reloader process [71234] using WatchFiles
INFO:     Started server process [71236]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
```

How to read it:

| Log line | Meaning |
|---|---|
| `Will watch for changes` | reload mode is enabled |
| `Started reloader process` | parent process monitors file changes |
| `Started server process` | actual app process started |
| `Waiting for application startup` | lifespan startup is running |
| `Application startup complete` | app is ready to serve |
| `Uvicorn running on ...` | server bound to host and port |

Production startup:

```text
INFO:     Started server process [1]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8000
```

If logs stop at:

```text
Waiting for application startup.
```

Likely issues:

- DB connection hanging
- secret manager timeout
- model loading too slow
- migration lock
- blocking call in lifespan

If logs show:

```text
ERROR:    Error loading ASGI app. Could not import module "app.main".
```

Check:

- working directory
- package `__init__.py`
- module path
- Docker `WORKDIR`
- dependency installation

If logs show:

```text
ERROR:    [Errno 98] Address already in use
```

Meaning:

- another process already uses the port
- in Kubernetes, the container command may be starting two servers

---

## 8. Runtime Modes

### Development

```bash
uvicorn app.main:app --reload --port 8000
```

Properties:

- file watching
- automatic restart
- extra process for reloader
- not production safe

### Production single worker

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Good for:

- small services
- container orchestrators that scale replicas horizontally
- async I/O-heavy apps

### Production multi-worker

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

Gunicorn is also common in production, but check the current Uvicorn docs for the worker package. Newer Uvicorn guidance recommends the external `uvicorn-worker` package instead of depending on the deprecated `uvicorn.workers` module.

```bash
python -m pip install uvicorn-worker
gunicorn app.main:app \
  -k uvicorn_worker.UvicornWorker \
  --workers 4 \
  --bind 0.0.0.0:8000
```

Good for:

- CPU isolation across processes
- better use of multi-core hosts
- process-level failure isolation

Trade-off:

- each worker has its own memory
- each worker may open its own DB pool
- too many workers can overload downstream systems

---

## 9. Async Performance Model

Async helps when work is mostly waiting:

```text
API request
  -> wait for DB
  -> wait for another service
  -> wait for Redis
  -> return response
```

Async does not magically speed CPU-heavy work:

```text
API request
  -> parse huge file
  -> run ML inference on CPU
  -> compress large payload
```

Decision:

| Workload | Better approach |
|---|---|
| Many concurrent I/O waits | async handlers |
| CPU-heavy computation | worker pool, background job, separate service |
| Long-running jobs | queue plus worker |
| Real-time WebSocket | ASGI works well |

Mistake:

```python
import time

@app.get("/slow")
async def slow():
    time.sleep(5)
    return {"ok": True}
```

Better:

```python
import asyncio

@app.get("/slow")
async def slow():
    await asyncio.sleep(5)
    return {"ok": True}
```

For real blocking libraries, use async-compatible libraries or move blocking work off the event loop.

---

## 10. Database Pooling and Scaling

Each process can have its own DB pool:

```text
4 pods
  x 4 workers per pod
  x 10 DB connections per worker
  = 160 possible DB connections
```

Scaling mistake:

> Increasing workers without checking DB connection limits.

Better:

- calculate total connection budget
- use pooling intentionally
- cap worker count
- use read replicas if needed
- avoid per-request connection creation
- expose readiness only after DB pool is healthy

---

## 11. Health, Readiness, and Liveness

### Health endpoint

```python
@app.get("/health")
async def health():
    return {"status": "ok"}
```

### Readiness endpoint

```python
@app.get("/ready")
async def ready():
    if not app.state.ready:
        return {"status": "starting"}
    return {"status": "ready"}
```

Difference:

| Endpoint | Question |
|---|---|
| Liveness | Should the process be restarted? |
| Readiness | Should traffic be sent here? |
| Health | Is the app basically responding? |

Mistake:

- making readiness depend on every optional downstream system, causing unnecessary traffic drops
- making liveness too strict, causing restart loops during temporary dependency issues

---

## 12. Serving and Scaling Architecture

```text
Internet
  -> CDN or load balancer
  -> ingress / reverse proxy
  -> FastAPI container
  -> Uvicorn worker
  -> app code
  -> DB / cache / message broker
```

Scale levers:

- more pods or instances
- more workers per instance
- async concurrency
- DB pool tuning
- caching
- queueing long work
- CDN for static responses
- rate limiting

When not to scale app workers:

- DB is bottleneck
- external API rate limit is bottleneck
- CPU-bound job belongs in worker queue
- memory is already near limit

---

## 13. Real-World Example: Booking API

Pipeline:

```text
Build:
  uv sync --frozen
  uv run pytest
  uv build

Package:
  Docker image with app and locked dependencies

Runtime:
  Uvicorn/Gunicorn starts app.main:app

Serve:
  Load balancer sends requests to /orders

Scale:
  Kubernetes increases replicas under traffic
```

Critical decisions:

- async DB driver or sync DB in threadpool?
- one worker or multiple workers?
- readiness should check critical dependencies
- startup logs must prove config and DB pool loaded correctly
- background order processing should use a queue

---

## 14. Interview Questions

### Question

> Explain what happens when a FastAPI app starts.

Strong answer:

1. The process starts and imports the ASGI application object.
2. The ASGI server binds a host and port.
3. The application lifespan startup runs.
4. The service initializes dependencies like DB pools and telemetry.
5. Once startup completes, the server handles HTTP requests through ASGI messages.
6. On shutdown, it stops accepting traffic and runs cleanup.

### Question

> How would you scale a FastAPI service?

Strong answer:

> I would first identify the bottleneck. For I/O-heavy APIs, async handlers and horizontal pod scaling help. For CPU-heavy work, I would move work to background workers or separate services. I would calculate worker count against DB pool limits, expose readiness checks, and observe latency, error rate, CPU, memory, and downstream saturation.

### Question

> What startup log line proves the app is ready?

Strong answer:

> `Application startup complete` means the FastAPI lifespan startup finished. In production, I also want readiness probe success and dependency initialization logs, because binding a port is not the same as being ready for traffic.

---

## 15. Common Failure Modes

| Symptom | Likely cause | Fix |
|---|---|---|
| Import module error | wrong app path, wrong working directory | fix `uvicorn package.module:app` and Docker `WORKDIR` |
| Hangs during startup | DB, secret, migration, blocking startup | add timeouts and structured startup logs |
| Port already in use | duplicate process or wrong port | inspect process/container command |
| High latency under load | blocking I/O in async handler | use async libs or offload work |
| DB connection exhaustion | too many workers/pods/pools | calculate connection budget |
| CrashLoopBackOff | startup exception or failing probe | inspect previous container logs and events |

---

## 16. Revision Notes

- One-line summary: FastAPI app code runs inside an ASGI server that converts network traffic into async application events.
- Three keywords: ASGI, lifespan, Uvicorn.
- One interview trap: saying async improves CPU-heavy work automatically.
- One memory trick: server binds the port, app startup proves readiness.
