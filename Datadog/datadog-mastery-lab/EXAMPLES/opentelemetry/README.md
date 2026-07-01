# OpenTelemetry Examples: Java, Python, Node.js With Datadog

## Overview

These examples show how to use the OpenTelemetry SDK in each language with the OTLP exporter routed to Datadog Agent.

```text
Application (OTel SDK)
  -> OTLP gRPC exporter (port 4317)
  -> Datadog Agent (OTLP receiver)
  -> Datadog intake
```

---

## Datadog Agent OTLP Config

```yaml
# datadog.yaml
otlp_config:
  receiver:
    protocols:
      grpc:
        endpoint: "0.0.0.0:4317"
      http:
        endpoint: "0.0.0.0:4318"
```

Or in docker-compose:

```yaml
datadog-agent:
  image: gcr.io/datadoghq/agent:7
  environment:
    DD_API_KEY: "${DD_API_KEY}"
    DD_OTLP_CONFIG_RECEIVER_PROTOCOLS_GRPC_ENDPOINT: "0.0.0.0:4317"
    DD_OTLP_CONFIG_RECEIVER_PROTOCOLS_HTTP_ENDPOINT: "0.0.0.0:4318"
  ports:
    - "4317:4317"
    - "4318:4318"
```

---

## Java: OTel Auto-Instrumentation Agent

```bash
# Download OTel Java agent.
curl -Lo opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Run Spring Boot with OTel agent.
java \
  -javaagent:./opentelemetry-javaagent.jar \
  -Dotel.service.name=orders-otel-java \
  -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
  -Dotel.resource.attributes=env=dev,version=1.0.0 \
  -Dotel.propagators=tracecontext,baggage \
  -Dotel.traces.exporter=otlp \
  -Dotel.metrics.exporter=none \
  -jar orders-service.jar
```

---

## Python: OTel SDK With FastAPI

```bash
pip install opentelemetry-api \
            opentelemetry-sdk \
            opentelemetry-exporter-otlp-proto-grpc \
            opentelemetry-instrumentation-fastapi \
            opentelemetry-instrumentation-httpx \
            opentelemetry-instrumentation-sqlalchemy
```

```python
# otel_setup.py
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource, SERVICE_NAME, SERVICE_VERSION
from opentelemetry.propagate import set_global_textmap
from opentelemetry.propagators.composite import CompositePropagator
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator
from opentelemetry.baggage.propagation import W3CBaggagePropagator
import os


def setup_otel():
    resource = Resource.create({
        SERVICE_NAME: os.environ.get("OTEL_SERVICE_NAME", "orders-otel-python"),
        SERVICE_VERSION: os.environ.get("DD_VERSION", "1.0.0"),
        "deployment.environment": os.environ.get("DD_ENV", "dev"),
    })

    exporter = OTLPSpanExporter(
        endpoint=os.environ.get("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317"),
        insecure=True,
    )

    provider = TracerProvider(resource=resource)
    provider.add_span_processor(BatchSpanProcessor(exporter))
    trace.set_tracer_provider(provider)

    set_global_textmap(CompositePropagator([
        TraceContextTextMapPropagator(),
        W3CBaggagePropagator(),
    ]))
```

```python
# main.py
from otel_setup import setup_otel
setup_otel()

from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from fastapi import FastAPI
from opentelemetry import trace

app = FastAPI()
FastAPIInstrumentor.instrument_app(app)

tracer = trace.get_tracer("orders-otel-python", "1.0.0")


@app.post("/orders")
async def create_order(order: dict):
    with tracer.start_as_current_span("orders.create") as span:
        span.set_attribute("order.customer_id", order.get("customerId"))
        span.set_attribute("order.total", order.get("total", 0))
        return {"orderId": "ORD-001", "status": "CREATED"}
```

---

## Node.js: OTel SDK

```bash
npm install @opentelemetry/api \
            @opentelemetry/sdk-node \
            @opentelemetry/exporter-trace-otlp-grpc \
            @opentelemetry/auto-instrumentations-node \
            @opentelemetry/resources \
            @opentelemetry/semantic-conventions
```

```javascript
// tracing.js - must be first.
const { NodeSDK } = require('@opentelemetry/sdk-node')
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-grpc')
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node')
const { resourceFromAttributes } = require('@opentelemetry/resources')
const { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } = require('@opentelemetry/semantic-conventions')

const sdk = new NodeSDK({
  resource: resourceFromAttributes({
    [ATTR_SERVICE_NAME]: process.env.OTEL_SERVICE_NAME || 'orders-otel-node',
    [ATTR_SERVICE_VERSION]: process.env.DD_VERSION || '1.0.0',
    'deployment.environment': process.env.DD_ENV || 'dev',
  }),
  traceExporter: new OTLPTraceExporter({
    url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT || 'http://localhost:4317',
  }),
  instrumentations: [
    getNodeAutoInstrumentations({
      '@opentelemetry/instrumentation-fs': { enabled: false },
    }),
  ],
})

sdk.start()

process.on('SIGTERM', async () => {
  await sdk.shutdown()
  process.exit(0)
})
```

```javascript
// server.js
require('./tracing')  // OTel must be first

const express = require('express')
const { trace } = require('@opentelemetry/api')

const app = express()
app.use(express.json())

const tracer = trace.getTracer('orders-otel-node', '1.0.0')

app.post('/orders', (req, res) => {
  const span = tracer.startSpan('orders.create')
  span.setAttribute('order.customer_id', req.body.customerId)
  span.setAttribute('order.total', req.body.total)
  span.end()
  res.status(201).json({ orderId: 'ORD-001', status: 'CREATED' })
})

app.listen(3000, () => console.log('Server on port 3000'))
```

---

## Key Difference: OTel vs Datadog Native SDK

| Aspect | Datadog Native (dd-trace/ddtrace) | OpenTelemetry SDK |
|---|---|---|
| Propagation default | Datadog headers | W3C TraceContext |
| Vendor lock-in | Datadog-specific API | Vendor-neutral CNCF standard |
| Auto-instrumentation | dd-java-agent / ddtrace-run | OTel agent / auto-instrumentations package |
| Log injection | Automatic with DD_LOGS_INJECTION=true | Manual (read trace context and inject into log) |
| Sampling | Datadog adaptive sampling | OTel sampler config |
| Switch to other backend | Requires code changes | Change exporter config only |
