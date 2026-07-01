# Python FastAPI With ddtrace: Complete Example

## requirements.txt

```text
fastapi==0.109.0
uvicorn[standard]==0.27.0
ddtrace==2.6.0
python-json-logger==2.0.7
httpx==0.26.0
```

## tracer_setup.py

```python
"""
Initialize ddtrace BEFORE importing FastAPI.
This file must be imported first in main.py.
"""
from ddtrace import tracer, patch_all

# Patch all supported integrations.
patch_all()

# Configure tracer (alternative to environment variables).
tracer.configure(
    hostname="localhost",
    port=8126,
)
```

## logging_setup.py

```python
import logging
import sys
from pythonjsonlogger import jsonlogger
from ddtrace import tracer as dd_tracer


class DatadogJsonFormatter(jsonlogger.JsonFormatter):
    """JSON log formatter that injects dd.trace_id and dd.span_id."""

    def add_fields(self, log_record, record, message_dict):
        super().add_fields(log_record, record, message_dict)

        # Inject trace context.
        span = dd_tracer.current_span()
        if span:
            log_record['dd.trace_id'] = str(span.trace_id)
            log_record['dd.span_id'] = str(span.span_id)
        else:
            log_record['dd.trace_id'] = None
            log_record['dd.span_id'] = None

        # Inject service context.
        import os
        log_record['dd.service'] = os.environ.get('DD_SERVICE', 'orders-api-python')
        log_record['dd.env'] = os.environ.get('DD_ENV', 'dev')
        log_record['dd.version'] = os.environ.get('DD_VERSION', '1.0.0')


def setup_logging():
    handler = logging.StreamHandler(sys.stdout)
    formatter = DatadogJsonFormatter(
        '%(asctime)s %(levelname)s %(name)s %(message)s'
    )
    handler.setFormatter(formatter)

    root_logger = logging.getLogger()
    root_logger.addHandler(handler)
    root_logger.setLevel(logging.INFO)
```

## main.py

```python
# tracer_setup must be first import.
import tracer_setup
from logging_setup import setup_logging

setup_logging()

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional
import logging
from ddtrace import tracer

logger = logging.getLogger(__name__)

app = FastAPI(title="Orders API (Python)")


class OrderRequest(BaseModel):
    customer_id: str
    total: float
    items: list


class OrderResponse(BaseModel):
    order_id: str
    status: str


@app.post("/orders", response_model=OrderResponse, status_code=201)
async def create_order(request: OrderRequest):
    logger.info("Creating order", extra={
        "customer_id": request.customer_id,
        "total": request.total,
    })

    # Add business context to the active APM span.
    span = tracer.current_span()
    if span:
        span.set_tag("order.customer_id", request.customer_id)
        span.set_tag("order.total", request.total)
        span.set_tag("order.items_count", len(request.items))

    # Manual child span for a business operation.
    with tracer.trace("order.validate",
                      service="orders-api-python",
                      resource="OrderValidator.validate") as validate_span:
        validate_span.set_tag("order.customer_id", request.customer_id)
        if request.total <= 0:
            validate_span.error = 1
            raise HTTPException(status_code=400, detail="Total must be positive")

    order_id = f"ORD-{int(__import__('time').time() * 1000)}"
    logger.info("Order created successfully", extra={"order_id": order_id})
    return OrderResponse(order_id=order_id, status="CREATED")


@app.get("/orders/{order_id}", response_model=OrderResponse)
async def get_order(order_id: str):
    logger.info("Fetching order", extra={"order_id": order_id})

    span = tracer.current_span()
    if span:
        span.set_tag("order.id", order_id)

    return OrderResponse(order_id=order_id, status="PENDING")


@app.get("/health")
async def health():
    return {"status": "ok"}
```

## Run Commands

```bash
# Option 1: ddtrace-run (zero-code-change instrumentation).
DD_SERVICE=orders-api-python \
DD_ENV=dev \
DD_VERSION=1.0.0 \
DD_AGENT_HOST=localhost \
DD_LOGS_INJECTION=true \
DD_TRACE_SAMPLE_RATE=1.0 \
ddtrace-run uvicorn main:app --host 0.0.0.0 --port 8000

# Option 2: Run normally (tracer_setup handles patching in code).
DD_SERVICE=orders-api-python \
DD_ENV=dev \
DD_VERSION=1.0.0 \
DD_AGENT_HOST=localhost \
DD_LOGS_INJECTION=true \
uvicorn main:app --host 0.0.0.0 --port 8000
```

## Expected JSON Log Output

```json
{
  "asctime": "2024-01-15 10:23:45,123",
  "levelname": "INFO",
  "name": "__main__",
  "message": "Creating order",
  "customer_id": "CUST-001",
  "total": 99.99,
  "dd.trace_id": "8423012345678901234",
  "dd.span_id": "1234567890123456789",
  "dd.service": "orders-api-python",
  "dd.env": "dev",
  "dd.version": "1.0.0"
}
```

## docker-compose.yml

```yaml
version: "3.8"

services:
  datadog-agent:
    image: gcr.io/datadoghq/agent:7
    environment:
      DD_API_KEY: "${DD_API_KEY}"
      DD_SITE: datadoghq.com
      DD_APM_ENABLED: "true"
      DD_APM_NON_LOCAL_TRAFFIC: "true"
    ports:
      - "8126:8126/tcp"

  orders-api-python:
    build: .
    environment:
      DD_SERVICE: orders-api-python
      DD_ENV: dev
      DD_VERSION: "1.0.0"
      DD_AGENT_HOST: datadog-agent
      DD_TRACE_AGENT_PORT: "8126"
      DD_LOGS_INJECTION: "true"
      DD_TRACE_SAMPLE_RATE: "1.0"
    ports:
      - "8000:8000"
    command: ddtrace-run uvicorn main:app --host 0.0.0.0 --port 8000
```
