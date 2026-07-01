# 07. Datadog APM: Node.js and Python Instrumentation (Express, Flask, FastAPI)

## Goal

Instrument Node.js (Express/Fastify) and Python (Flask/FastAPI/Django) applications with Datadog APM for distributed tracing, manual spans, and custom tags.

---

## Part 1: Node.js Instrumentation With dd-trace

### Install

```bash
npm install dd-trace
```

### Init Pattern (Must Be First Import)

```javascript
// tracer.js - initialize BEFORE any other import
'use strict'

const tracer = require('dd-trace').init({
  service: 'orders-service',
  env: process.env.DD_ENV || 'production',
  version: process.env.DD_VERSION || '1.0.0',
  hostname: process.env.DD_AGENT_HOST || 'localhost',
  port: 8126,
  logInjection: true,        // inject trace_id into log output
  runtimeMetrics: true,      // enable Node.js runtime metrics
  profiling: true,           // enable continuous profiling
  sampleRate: 1.0,           // 100% for dev; reduce in production
})

module.exports = tracer
```

```javascript
// server.js - import tracer first.
require('./tracer')
const express = require('express')
// ... rest of app
```

### Express Auto-Instrumentation

Once `dd-trace` is initialized, Express is auto-instrumented:

```javascript
require('./tracer')
const express = require('express')
const app = express()

// All routes are automatically traced.
app.get('/orders/:id', async (req, res) => {
  const order = await db.findOrder(req.params.id)
  res.json(order)
})

app.listen(3000)
```

Auto-instrumented libraries: Express, Fastify, Koa, HTTP/HTTPS, mysql/mysql2, pg, mongodb, redis/ioredis, Kafka.js, gRPC, AWS SDK.

### Manual Spans In Node.js

```javascript
const tracer = require('dd-trace')

async function processOrder(orderId) {
  const span = tracer.startSpan('order.process', {
    childOf: tracer.scope().active(),
    tags: {
      'order.id': orderId,
      'service': 'orders-service',
      'resource.name': 'OrderProcessor.processOrder',
    }
  })

  try {
    const result = await doWork(orderId)
    span.setTag('order.status', result.status)
    return result
  } catch (err) {
    span.setTag('error', true)
    span.setTag('error.message', err.message)
    span.setTag('error.type', err.constructor.name)
    throw err
  } finally {
    span.finish()
  }
}
```

### Async Context Propagation

Datadog dd-trace handles async context automatically via AsyncLocalStorage. No manual propagation needed.

```javascript
// Context propagates correctly through promises and async/await.
app.get('/checkout', async (req, res) => {
  // span created by dd-trace for this request
  const order = await orderService.create(req.body)   // creates child span
  const payment = await paymentService.charge(order)  // creates child span
  res.json({ order, payment })
})
```

### Environment Variables (Node.js)

```bash
DD_SERVICE=orders-service
DD_ENV=production
DD_VERSION=1.0.0
DD_AGENT_HOST=localhost
DD_TRACE_AGENT_PORT=8126
DD_LOGS_INJECTION=true
DD_RUNTIME_METRICS_ENABLED=true
```

---

## Part 2: Python Instrumentation With ddtrace

### Install

```bash
pip install ddtrace
```

### Method 1: ddtrace-run Command (Zero Code Change)

```bash
DD_SERVICE=orders-service \
DD_ENV=production \
DD_VERSION=1.0.0 \
DD_AGENT_HOST=localhost \
DD_LOGS_INJECTION=true \
ddtrace-run python app.py
```

`ddtrace-run` is a wrapper that patches all supported libraries automatically before starting the Python process.

### Method 2: In-Code Init

```python
# tracer.py - must be the FIRST import.
from ddtrace import tracer, patch_all

# Patch all supported libraries.
patch_all()

tracer.configure(
    hostname="localhost",
    port=8126,
    settings={
        "ANALYTICS_ENABLED": True,
    }
)
```

### Flask Auto-Instrumentation

```python
# app.py
from ddtrace import patch_all
patch_all()

from flask import Flask
app = Flask(__name__)

# All routes are automatically traced.
@app.route('/orders/<order_id>')
def get_order(order_id):
    order = db.query(f"SELECT * FROM orders WHERE id='{order_id}'")
    return jsonify(order)
```

### FastAPI Auto-Instrumentation

```python
# main.py
from ddtrace import patch_all
patch_all()

from fastapi import FastAPI
app = FastAPI()

@app.get("/orders/{order_id}")
async def get_order(order_id: str):
    order = await db.find_order(order_id)
    return order
```

### Manual Spans In Python

```python
from ddtrace import tracer

def process_order(order_id: str):
    with tracer.trace("order.process",
                      service="orders-service",
                      resource="OrderProcessor.process") as span:
        span.set_tag("order.id", order_id)
        span.set_tag("order.source", "web")

        try:
            result = do_work(order_id)
            span.set_tag("order.status", result.status)
            return result
        except Exception as e:
            span.error = 1
            span.set_tag("error.message", str(e))
            span.set_tag("error.type", type(e).__name__)
            raise
```

### Async FastAPI Manual Spans

```python
from ddtrace import tracer

@app.post("/orders")
async def create_order(order: OrderRequest):
    with tracer.trace("orders.create") as span:
        span.set_tag("order.customer_id", order.customer_id)
        span.set_tag("order.total", float(order.total))

        order_id = await order_repository.save(order)
        await event_bus.publish("order.created", order_id)

        span.set_tag("order.id", order_id)
        return {"order_id": order_id}
```

### Auto-Instrumented Libraries (Python)

| Library | Integration Name |
|---|---|
| Flask | `flask` |
| FastAPI / Starlette | `fastapi`, `starlette` |
| Django | `django` |
| requests | `requests` |
| aiohttp | `aiohttp` |
| SQLAlchemy | `sqlalchemy` |
| psycopg2 | `psycopg` |
| redis (py-redis) | `redis` |
| kafka-python | `kafka` |
| boto3 / AWS SDK | `botocore` |
| celery | `celery` |
| grpcio | `grpc` |

---

## Part 3: Common Patterns Both Languages

### Adding Custom Tags From Request Context

Node.js:
```javascript
const tracer = require('dd-trace')

function addRequestContext(req) {
  const span = tracer.scope().active()
  if (span) {
    span.setTag('user.id', req.user?.id)
    span.setTag('user.tier', req.user?.tier)
    span.setTag('request.region', req.headers['x-region'])
  }
}
```

Python:
```python
from ddtrace import tracer

def add_request_context(request):
    span = tracer.current_span()
    if span:
        span.set_tag("user.id", request.state.user_id)
        span.set_tag("user.tier", request.state.user_tier)
        span.set_tag("request.region", request.headers.get("x-region"))
```

---

## Interview Sound Bite

Node.js uses dd-trace initialized before all other imports; it auto-instruments Express, HTTP clients, databases, and Kafka via monkey-patching. Python uses ddtrace with either ddtrace-run (zero-code-change) or patch_all() in-process. Both support manual span creation for business operations not covered by auto-instrumentation. Async context propagation is handled automatically by both libraries. The same five env vars apply: DD_SERVICE, DD_ENV, DD_VERSION, DD_AGENT_HOST, DD_LOGS_INJECTION.
