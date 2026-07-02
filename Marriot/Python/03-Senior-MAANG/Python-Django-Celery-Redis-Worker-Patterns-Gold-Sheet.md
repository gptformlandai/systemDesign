# Python Django, Celery, Redis, and Worker Patterns - Gold Sheet

> **Track File #18f - Group 3: Senior MAANG**
> For: backend Python breadth | Level: production web and background job ecosystems

---

## 1. Why This Sheet Exists

FastAPI is excellent for modern async APIs, but senior Python backend interviews often expect awareness of the wider ecosystem:

- Django for full-stack/product backends and admin-heavy systems
- Celery for durable distributed background jobs
- Redis/RQ/ARQ for lighter queues and async-friendly workloads
- worker reliability patterns: retries, idempotency, dead letters, backoff, visibility timeouts

This sheet fills the "Python backend beyond FastAPI" gap.

---

## 2. Mental Model

```text
Web request path      -> needs low latency and direct user feedback
Background job path   -> needs durable execution, retries, idempotency, and observability
Scheduled job path    -> needs exactly-once illusion through locks/idempotency
Worker fleet          -> needs safe concurrency, backpressure, and failure isolation
```

Senior rule:

> Moving work to a background job does not make correctness disappear. It moves correctness into retries, idempotency, ordering, and observability.

---

## 3. Django

### Where Django Fits

Django is strong when the product needs:

- relational data modeling
- admin UI
- authentication/session features
- server-rendered pages or mixed API/admin workloads
- batteries-included conventions
- ORM-first productivity

FastAPI is often stronger when the service needs:

- high-throughput JSON APIs
- async-first request handling
- explicit service boundaries
- OpenAPI/Pydantic-heavy schemas
- smaller service footprint

### Django Architecture Map

```text
Django URLConf        -> route mapping
View                  -> request handler
Serializer/Form       -> validation and transformation
Model                 -> ORM entity
Manager/QuerySet      -> query behavior
Service layer         -> business use case, explicit by convention
Middleware            -> request/response cross-cutting behavior
Signals               -> lifecycle hooks, use carefully
```

### Django Interview Maturity

Bad:

```text
"Django is just slower Flask."
```

Better:

```text
Django optimizes for integrated product backends: ORM, migrations, admin, auth,
middleware, sessions, and conventions. I would choose it when those built-in pieces
reduce product complexity. For small async service APIs, FastAPI may be the cleaner fit.
```

---

## 4. Django ORM Traps

### N+1 Query

Bad:

```python
orders = Order.objects.all()
for order in orders:
    print(order.customer.email)
```

Better:

```python
orders = Order.objects.select_related("customer")
for order in orders:
    print(order.customer.email)
```

For many-to-many or reverse relations:

```python
orders = Order.objects.prefetch_related("items")
```

### Transaction Scope

```python
from django.db import transaction


@transaction.atomic
def create_order(command: CreateOrder) -> Order:
    order = Order.objects.create(...)
    OutboxEvent.objects.create(...)
    return order
```

Keep transactions short. Do not call slow external APIs inside a DB transaction.

---

## 5. Background Jobs

Use background jobs for:

- email sending
- webhook delivery
- report generation
- image/video processing
- payment reconciliation
- cache warming
- ML inference when request latency would suffer
- event-driven integration

Do not use jobs to hide:

- missing data model
- non-idempotent side effects
- unknown failure behavior
- work that actually needs synchronous confirmation

---

## 6. Celery

Celery is a distributed task queue.

Common pieces:

```text
Producer  -> web app enqueues task
Broker    -> Redis/RabbitMQ transports task
Worker    -> executes task
Backend   -> optional result storage
Beat      -> scheduler for periodic tasks
```

Basic task:

```python
from celery import Celery

app = Celery("orders", broker="redis://localhost:6379/0")


@app.task(
    autoretry_for=(TimeoutError,),
    retry_backoff=True,
    retry_jitter=True,
    max_retries=5,
)
def send_order_email(order_id: str) -> None:
    ...
```

Production concerns:

- task idempotency
- retry policy
- timeout policy
- broker durability
- result backend cleanup
- worker concurrency model
- poison messages
- dead-letter handling

---

## 7. RQ, ARQ, and Lightweight Queues

| Tool | Fit |
|---|---|
| Celery | mature distributed jobs, complex routing/retries, RabbitMQ/Redis |
| RQ | simple Redis-backed sync jobs |
| ARQ | asyncio-friendly Redis jobs |
| Dramatiq | simpler worker model than Celery for some teams |
| cloud queue + worker | SQS/Pub/Sub/Kafka plus Python consumers |

Decision:

```text
If the team already runs Celery well, use it.
If the service is async-first and Redis is acceptable, ARQ can be simpler.
If durability and cloud integration dominate, use managed queues.
```

---

## 8. Idempotency In Workers

A job may run:

- once
- twice
- after timeout
- after worker crash
- after broker redelivery
- concurrently with a duplicate

Design every task as at-least-once unless you have proven otherwise.

Bad:

```python
def charge_card(order_id: str) -> None:
    payment_gateway.charge(order_id)
    mark_paid(order_id)
```

Better:

```python
def charge_card(order_id: str) -> None:
    order = load_order(order_id)
    if order.payment_status == "PAID":
        return

    result = payment_gateway.charge(
        amount_cents=order.amount_cents,
        idempotency_key=f"charge:{order.id}",
    )
    mark_paid(order.id, result.payment_id)
```

---

## 9. Retries and Backoff

Retry only transient failures:

- timeout
- connection reset
- 429 rate limit
- 503 downstream unavailable

Do not retry:

- validation failure
- unauthorized
- malformed payload
- permanent business rule violation

Retry policy:

```text
exponential backoff + jitter + max attempts + timeout + dead-letter path
```

Why jitter:

```text
Without jitter, many workers retry at the same time and create a retry storm.
```

---

## 10. Dead Letters and Poison Messages

A poison message always fails.

Examples:

- missing required field
- deleted referenced entity
- non-retryable business state
- dependency bug for a specific payload

Handling:

- classify exception as retryable/non-retryable
- send exhausted tasks to dead-letter storage
- alert on dead-letter rate
- provide replay tooling after fix
- preserve enough context to debug

---

## 11. Outbox Pattern

Problem:

```text
DB write succeeds, event publish fails.
Or event publish succeeds, DB transaction rolls back.
```

Outbox solution:

```text
Inside DB transaction:
  write business row
  write outbox event row

Worker:
  reads unsent outbox events
  publishes event
  marks event sent
```

Benefits:

- event emission becomes transactionally tied to domain change
- worker can retry safely
- no dual-write hole

---

## 12. Scheduling

Periodic jobs need:

- single active scheduler or distributed lock
- idempotent task body
- clock/timezone clarity
- missed-run policy
- observability

Examples:

- Celery Beat
- cron + job runner
- Kubernetes CronJob
- Airflow/Dagster for data workflows

Do not run periodic business jobs from random web process startup hooks.

---

## 13. Worker Concurrency

| Workload | Better Model |
|---|---|
| blocking I/O | threads or worker processes |
| CPU-bound pure Python | processes |
| async I/O | asyncio worker or async queue |
| heavy native extension work | benchmark threads vs processes |

Celery pools:

- prefork process pool is common default
- thread/gevent/eventlet pools have workload-specific trade-offs

Senior rule:

```text
Choose worker concurrency based on task behavior, not framework defaults.
```

---

## 14. Backpressure

Signals:

- queue depth rising
- oldest message age rising
- worker CPU maxed
- dependency latency rising
- retry count rising

Responses:

- scale workers
- reduce producer rate
- add rate limit
- shed low-priority work
- split queues by priority
- fix slow dependency
- add bulkheads

---

## 15. Observability For Workers

Track:

- task enqueue rate
- task success/failure count
- retry count
- dead-letter count
- task duration percentiles
- oldest message age
- queue depth
- worker heartbeats
- dependency latency

Every task log should include:

- task id
- correlation/request id
- entity id
- attempt number
- exception class

---

## 16. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Non-idempotent task body | Duplicate side effects | idempotency key and state check |
| Infinite retries | Retry storm | max attempts + DLQ |
| External API call in DB transaction | locks held too long | outbox or call before/after with clear consistency |
| One queue for everything | slow jobs block urgent jobs | priority/domain queues |
| No queue age metric | hidden backlog | alert on oldest message age |
| Treating Celery result backend as database | unbounded storage/cleanup problems | persist domain results in app DB |

---

## 17. Practical Question

> An order API must send confirmation email, notify inventory, and publish an order-created event. What runs in the request and what runs in workers?

Strong answer:

> In the request path I would validate input, create the order in a DB transaction, and write outbox events for side effects. I would not call email, inventory, and event broker directly inside the transaction. Workers would process the outbox: send email, notify inventory, and publish the event with idempotency keys. Each task would have bounded retries with backoff and jitter, classify permanent failures, and move exhausted tasks to a dead-letter path. I would monitor queue age, retries, task duration, and dead-letter count.

---

## 18. Revision Notes

- One-line summary: background work is at-least-once distributed execution, so idempotency and observability are mandatory.
- Three keywords: idempotency, retries, outbox.
- One interview trap: saying "put it in Celery" without explaining duplicate execution.
- One memory trick: every job can run twice, late, or never unless the design handles it.
