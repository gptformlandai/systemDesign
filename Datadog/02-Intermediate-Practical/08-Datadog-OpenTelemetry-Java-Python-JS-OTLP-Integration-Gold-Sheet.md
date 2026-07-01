# 08. Datadog OpenTelemetry: Java, Python, JS, OTLP Integration

## Goal

Understand how to instrument Java, Python, and Node.js applications using the OpenTelemetry SDK and route traces to Datadog via the OTLP exporter.

---

## Why OpenTelemetry With Datadog

OpenTelemetry (OTel) is a vendor-neutral instrumentation standard. Using OTel SDK means:

- No vendor lock-in in application code (swap Datadog for Jaeger/Tempo by changing exporter)
- Same instrumentation API across Java, Python, JS, Go, Ruby
- W3C TraceContext propagation (interoperable with any OTel-compatible system)
- Datadog fully supports OTLP ingest (via Datadog Agent or directly to intake)

```text
Application (OTel SDK)
  -> OTLP exporter (gRPC/HTTP)
  -> Datadog Agent (OTLP receiver on port 4317/4318)
  -> Datadog intake
```

---

## Enable OTLP Receiver On Datadog Agent

```yaml
# In datadog.yaml.
otlp_config:
  receiver:
    protocols:
      grpc:
        endpoint: "0.0.0.0:4317"
      http:
        endpoint: "0.0.0.0:4318"
```

Or via environment variables (Docker/K8s):

```bash
DD_OTLP_CONFIG_RECEIVER_PROTOCOLS_GRPC_ENDPOINT=0.0.0.0:4317
DD_OTLP_CONFIG_RECEIVER_PROTOCOLS_HTTP_ENDPOINT=0.0.0.0:4318
```

---

## Part 1: Java With OpenTelemetry SDK

### Maven Dependencies

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-bom</artifactId>
      <version>1.32.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- Core OTel API and SDK -->
  <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
  </dependency>
  <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
  </dependency>

  <!-- OTLP gRPC exporter -->
  <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
  </dependency>

  <!-- W3C TraceContext propagation -->
  <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-extension-trace-propagators</artifactId>
  </dependency>

  <!-- Auto-instrumentation agent (alternative to manual SDK setup) -->
  <!-- Download: opentelemetry-javaagent.jar -->
</dependencies>
```

### TracerProvider Setup

```java
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

@Configuration
public class OtelConfig {

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource serviceResource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, "orders-service",
                ResourceAttributes.SERVICE_VERSION, "1.2.3",
                ResourceAttributes.DEPLOYMENT_ENVIRONMENT, "production"
            )));

        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://datadog-agent:4317")
            .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
            .setResource(serviceResource)
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(
                TextMapPropagator.composite(
                    W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance()
                )
            ))
            .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("orders-service", "1.2.3");
    }
}
```

### Creating Spans In Java (OTel API)

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@Service
public class OrderService {

    private final Tracer tracer;

    public OrderService(Tracer tracer) {
        this.tracer = tracer;
    }

    public Order createOrder(OrderRequest request) {
        Span span = tracer.spanBuilder("orders.create")
            .setAttribute("order.customer_id", request.getCustomerId())
            .setAttribute("order.total", request.getTotal())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Order order = repository.save(request);
            span.setAttribute("order.id", order.getId());
            return order;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Java Auto-Instrumentation Agent

```bash
# Download the OTel Java agent.
curl -Lo opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Run with the agent (auto-instruments Spring Boot, JDBC, HTTP clients).
java \
  -javaagent:./opentelemetry-javaagent.jar \
  -Dotel.service.name=orders-service \
  -Dotel.exporter.otlp.endpoint=http://datadog-agent:4317 \
  -Dotel.resource.attributes=env=production,version=1.2.3 \
  -Dotel.propagators=tracecontext,baggage \
  -jar orders-service.jar
```

---

## Part 2: Python With OpenTelemetry SDK

### Install

```bash
pip install opentelemetry-api \
            opentelemetry-sdk \
            opentelemetry-exporter-otlp-proto-grpc \
            opentelemetry-instrumentation-fastapi \
            opentelemetry-instrumentation-requests \
            opentelemetry-instrumentation-sqlalchemy
```

### TracerProvider Setup

```python
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource, SERVICE_NAME, SERVICE_VERSION
from opentelemetry.propagate import set_global_textmap
from opentelemetry.propagators.composite import CompositePropagator
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator
from opentelemetry.baggage.propagation import W3CBaggagePropagator

def setup_tracing():
    resource = Resource.create({
        SERVICE_NAME: "orders-service",
        SERVICE_VERSION: "1.2.3",
        "deployment.environment": "production",
    })

    exporter = OTLPSpanExporter(
        endpoint="http://datadog-agent:4317",
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

### FastAPI With OTel Auto-Instrumentation

```python
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor

setup_tracing()

FastAPIInstrumentor.instrument()
RequestsInstrumentor.instrument()
SQLAlchemyInstrumentor.instrument(engine=engine)

app = FastAPI()
```

### Manual Spans In Python (OTel API)

```python
from opentelemetry import trace

tracer = trace.get_tracer("orders-service", "1.2.3")

async def process_order(order_id: str):
    with tracer.start_as_current_span("orders.process") as span:
        span.set_attribute("order.id", order_id)
        span.set_attribute("order.source", "api")

        try:
            result = await do_work(order_id)
            span.set_attribute("order.status", result.status)
            return result
        except Exception as e:
            span.record_exception(e)
            span.set_status(trace.StatusCode.ERROR, str(e))
            raise
```

---

## Part 3: Node.js With OpenTelemetry SDK

### Install

```bash
npm install @opentelemetry/api \
            @opentelemetry/sdk-node \
            @opentelemetry/exporter-trace-otlp-grpc \
            @opentelemetry/auto-instrumentations-node
```

### TracerProvider Setup

```javascript
// tracing.js - import BEFORE anything else.
const { NodeSDK } = require('@opentelemetry/sdk-node')
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-grpc')
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node')
const { resourceFromAttributes } = require('@opentelemetry/resources')
const { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } = require('@opentelemetry/semantic-conventions')

const sdk = new NodeSDK({
  resource: resourceFromAttributes({
    [ATTR_SERVICE_NAME]: 'orders-service',
    [ATTR_SERVICE_VERSION]: '1.0.0',
    'deployment.environment': 'production',
  }),
  traceExporter: new OTLPTraceExporter({
    url: 'http://datadog-agent:4317',
  }),
  instrumentations: [getNodeAutoInstrumentations()],
})

sdk.start()

process.on('SIGTERM', () => sdk.shutdown())
```

### Manual Spans In Node.js (OTel API)

```javascript
const { trace } = require('@opentelemetry/api')
const tracer = trace.getTracer('orders-service', '1.0.0')

async function processOrder(orderId) {
  const span = tracer.startSpan('orders.process', {
    attributes: {
      'order.id': orderId,
      'order.source': 'api',
    }
  })

  const ctx = trace.setSpan(context.active(), span)

  try {
    const result = await context.with(ctx, () => doWork(orderId))
    span.setAttribute('order.status', result.status)
    return result
  } catch (err) {
    span.recordException(err)
    span.setStatus({ code: SpanStatusCode.ERROR, message: err.message })
    throw err
  } finally {
    span.end()
  }
}
```

---

## W3C TraceContext Headers

```text
traceparent: 00-{traceId}-{spanId}-{flags}
  00: version (always 00)
  traceId: 32-char hex (128-bit)
  spanId:  16-char hex (64-bit)
  flags:   01=sampled, 00=not sampled

Example:
  traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01

tracestate: key=value pairs for vendor-specific data
  tracestate: dd=p:b7ad6b7169203331;s:1
```

---

## Interview Sound Bite

OpenTelemetry provides a vendor-neutral instrumentation API for Java, Python, and Node.js. The OTel SDK sends spans via OTLP (gRPC port 4317 or HTTP port 4318) to the Datadog Agent, which forwards them to Datadog. The key setup is TracerProvider + BatchSpanProcessor + OTLPSpanExporter + W3C TraceContext propagator. Auto-instrumentation agents/packages handle framework-level spans; manual spans use tracer.start_span() or tracer.startSpan(). OTel's main advantage is portability: the same application code works with Datadog, Jaeger, or any OTLP-compatible backend.
