# Python Observability, OpenTelemetry, Logging, and Metrics - Gold Sheet

> **Track File #18e - Group 3: Senior MAANG**
> For: production Python backends | Level: senior debugging and operating skill

---

## 1. Why This Sheet Exists

Production Python services fail in ways local tests rarely show:

- event loop stalls
- connection pool exhaustion
- slow SQL
- memory leaks
- retry storms
- thread pool saturation
- unstructured logs that cannot answer incident questions

Observability is how you make the service explain itself.

---

## 2. Observability Mental Model

```text
Logs    -> what happened
Metrics -> how often and how much
Traces  -> where time went across services
Profiles -> what code consumed CPU/memory
```

Senior rule:

```text
Do not wait for an incident to add observability.
Every production service should ship with request logs, RED metrics, tracing, health checks,
and a profiling/debugging runbook.
```

---

## 3. What To Measure

### RED Metrics

For request/response services:

- Rate: requests per second
- Errors: error count/rate by status and exception class
- Duration: latency percentiles

### USE Metrics

For resources:

- Utilization
- Saturation
- Errors

Examples:

- DB pool checked-out connections
- thread pool queue depth
- CPU
- memory
- event loop lag
- queue depth

---

## 4. Structured Logging

Bad:

```python
logger.info(f"Created order {order_id} for user {user_id}")
```

Better:

```python
logger.info(
    "order_created",
    extra={
        "order_id": order_id,
        "user_id": user_id,
        "request_id": request_id,
    },
)
```

Benefits:

- searchable fields
- dashboards
- incident correlation
- fewer regex games

---

## 5. Request Correlation With ContextVar

`ContextVar` works across asyncio task context better than thread-local storage.

```python
from contextvars import ContextVar
from uuid import uuid4

from starlette.middleware.base import BaseHTTPMiddleware

request_id_var: ContextVar[str] = ContextVar("request_id", default="-")


class RequestIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        request_id = request.headers.get("x-request-id", str(uuid4()))
        token = request_id_var.set(request_id)
        try:
            response = await call_next(request)
            response.headers["x-request-id"] = request_id
            return response
        finally:
            request_id_var.reset(token)
```

Logging filter:

```python
import logging


class RequestIdFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.request_id = request_id_var.get()
        return True
```

---

## 6. FastAPI Request Logging Middleware

```python
import logging
import time

from starlette.middleware.base import BaseHTTPMiddleware

logger = logging.getLogger("app.requests")


class AccessLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        start = time.perf_counter()
        try:
            response = await call_next(request)
        except Exception:
            duration_ms = (time.perf_counter() - start) * 1000
            logger.exception(
                "request_failed",
                extra={
                    "method": request.method,
                    "path": request.url.path,
                    "duration_ms": round(duration_ms, 2),
                },
            )
            raise

        duration_ms = (time.perf_counter() - start) * 1000
        logger.info(
            "request_completed",
            extra={
                "method": request.method,
                "path": request.url.path,
                "status_code": response.status_code,
                "duration_ms": round(duration_ms, 2),
            },
        )
        return response
```

Production additions:

- route template instead of raw path when possible
- redaction
- request size
- response size
- client/service identity

---

## 7. Metrics

Prometheus-style dimensions:

```text
http_requests_total{method, route, status}
http_request_duration_seconds_bucket{method, route}
db_pool_checked_out
db_query_duration_seconds_bucket{operation}
external_http_duration_seconds_bucket{client, host, status}
event_loop_lag_seconds
```

Avoid high-cardinality labels:

- raw user ID
- raw path with IDs
- email
- full URL
- request ID

Use route template:

```text
/orders/{order_id}
```

not:

```text
/orders/017f8b1e-...
```

---

## 8. Tracing With OpenTelemetry

Tracing answers:

```text
Where did this request spend time?
Which downstream call failed?
Which service created the error?
```

Typical spans:

```text
HTTP request span
    -> service method span
        -> SQL query span
        -> Redis span
        -> HTTP client span
```

What to instrument:

- FastAPI/Starlette
- HTTPX/requests
- SQLAlchemy/asyncpg/psycopg
- Redis
- Celery/background workers

Senior rule:

```text
Trace IDs must appear in logs so logs and traces join during incidents.
```

---

## 9. Async Observability

Async bugs often look like:

- p95 latency spike
- CPU low
- DB pool saturated
- event loop blocked by sync call
- throughput collapses under concurrency

Measure:

- event loop lag
- in-flight requests
- DB pool wait time
- external HTTP duration
- task cancellation count
- thread pool queue depth if using executors

Common async issue:

```python
@app.get("/users")
async def users():
    response = requests.get("https://example.com")  # blocks event loop
    return response.json()
```

Fix:

```python
import httpx


@app.get("/users")
async def users():
    async with httpx.AsyncClient(timeout=2.0) as client:
        response = await client.get("https://example.com")
    return response.json()
```

---

## 10. SQLAlchemy Observability

Track:

- query count per request
- slow query duration
- pool size
- checked-out connections
- pool wait time
- transaction duration
- N+1 query patterns

Incident smell:

```text
Requests are slow but CPU is low.
DB pool checked-out is maxed.
New requests wait for connections.
```

Fix candidates:

- reduce transaction scope
- add indexes
- use `selectinload`/`joinedload`
- increase pool only after understanding bottleneck
- add timeouts
- cap concurrency

---

## 11. Profiling Runbook

### High CPU

```bash
py-spy top --pid <pid>
py-spy record --pid <pid> --output profile.svg
```

Then:

- identify hot functions
- check accidental quadratic work
- check JSON serialization cost
- check regex loops
- check compression/encryption/hash work

### Memory Growth

```python
import tracemalloc

tracemalloc.start()
snapshot = tracemalloc.take_snapshot()
for stat in snapshot.statistics("lineno")[:10]:
    print(stat)
```

Then:

- compare snapshots over time
- inspect caches
- inspect global lists/dicts
- check task leaks
- check ORM session retention

### Async Stall

Use:

- access logs with duration
- event loop lag metric
- `py-spy top`
- dependency latency metrics
- DB pool metrics

---

## 12. Health Checks

Separate:

| Endpoint | Meaning |
|---|---|
| `/livez` | process is alive |
| `/readyz` | service can receive traffic |
| `/healthz` | optional aggregate health |

Readiness may check:

- DB connectivity
- migration compatibility
- critical dependency reachability
- queue connection

Liveness should be cheap and should not depend on every downstream service.

---

## 13. Alerting

Alert on user impact, not noise.

Good alerts:

- p95 latency above SLO
- 5xx error rate above threshold
- queue age too high
- DB pool saturated for sustained period
- memory approaching container limit
- event loop lag sustained

Bad alerts:

- every single exception
- CPU spike for 10 seconds
- one failed dependency call that retry handled

---

## 14. Logging Security

Never log:

- passwords
- tokens
- cookies
- full Authorization header
- credit card numbers
- secrets
- unnecessary PII

Redact at:

- middleware
- logger filter
- exception handler
- external client wrapper

---

## 15. Dashboard Checklist

Minimum FastAPI dashboard:

- request rate by route
- p50/p95/p99 latency by route
- 4xx/5xx by route
- top exception classes
- DB pool usage
- DB query latency
- external dependency latency
- event loop lag
- CPU/memory
- restarts
- queue depth if workers exist

---

## 16. Practical Question

> A FastAPI service has p95 latency spikes under load, CPU is low, and logs show no errors. How do you debug?

Strong answer:

> I would first check whether time is spent waiting rather than computing: DB pool saturation, external HTTP latency, event loop lag, and in-flight request count. I would use traces to see slow spans and metrics to confirm pool wait or downstream latency. Because it is async Python, I would also look for blocking synchronous calls inside `async def` using py-spy and event loop lag. If DB pool is saturated, I would inspect transaction scope, query count, N+1 patterns, and pool wait time before blindly increasing pool size.

---

## 17. Revision Notes

- One-line summary: logs tell what happened, metrics tell scale, traces tell where time went, profiles tell which code burned resources.
- Three keywords: logs, metrics, traces.
- One interview trap: adding logs but no correlation ID.
- One memory trick: every incident needs a timeline, a dimension, and a culprit span.
