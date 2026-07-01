# 23. Scenario: Broken Distributed Trace, Missing Context Propagation Debugging

## Scenario Setup

```text
Team reports:
  "When we add a request to cart and then checkout, Trace Explorer shows TWO separate traces.
   The trace for checkout does not show the context from the cart service call.
   We can see the checkout trace starts fresh (no parent span from cart-service).
   We expected one continuous trace from cart -> checkout -> payment."

Expected trace structure:
  cart-service POST /cart/add  [root span, traceID=abc]
    └── checkout-service POST /checkout  [child span, traceID=abc, parentID=cart-span]
          └── payment-service POST /charge  [child span, traceID=abc]

Actual behavior:
  cart-service POST /cart/add  [root span, traceID=abc]  <- standalone trace

  checkout-service POST /checkout  [new root span, traceID=xyz]  <- separate trace, no parent
    └── payment-service POST /charge  [child span, traceID=xyz]
```

---

## Diagnosis Framework For Broken Traces

```text
Broken traces are caused by one of:
  1. Headers not sent:   caller is not injecting trace headers into outbound request
  2. Headers not read:   callee is not extracting trace headers from inbound request
  3. Headers wrong:      caller uses Datadog headers, callee expects W3C headers (or vice versa)
  4. Missing library:    callee has no tracing instrumentation at all
  5. Async break:        async worker (Kafka consumer, queue) does not propagate context
  6. Version mismatch:   tracer libraries have incompatible propagation formats
```

---

## Step 1: Verify Headers Are Being Sent

```text
APM -> Trace Explorer

Find the cart-service trace (traceID=abc).
Click the exit span: "HTTP POST checkout-service/checkout"

Span metadata:
  http.url: http://checkout-service:8080/checkout
  http.method: POST
  out.host: checkout-service
  http.status_code: 200
  
Check: is there a "propagated" or "inject" tag? If yes -> headers were injected.

If x-datadog-trace-id is NOT in the span tags -> the HTTP client call is NOT instrumented.
```

### Check Logs For Outbound Request

```text
Log Explorer:
  service:cart-service env:production
  @http.url_details.path:/checkout
  @http.status_code:200
  time: last 30 minutes

Check if the log for the outgoing request includes trace_id.
If no trace_id in log -> tracing library not active on this call path.
```

---

## Step 2: Verify Headers Are Being Received

```text
APM -> Trace Explorer

Find the checkout-service trace (traceID=xyz).
Click the root span (entry span).

Span metadata:
  http.url_details.path: /checkout
  http.method: POST
  
Look for:
  _sampling_priority_v1: 1  (set if Datadog headers were extracted)
  
If this span has NO parent but it should have one:
  -> checkout-service is NOT extracting the inbound trace headers
  OR
  -> cart-service is NOT injecting the trace headers
```

---

## Step 3: Check Propagation Format Mismatch

```text
cart-service is a Node.js app using dd-trace.
checkout-service is a Java app using OTel Java SDK.

dd-trace (Node.js) default propagation: Datadog headers (x-datadog-trace-id)
OTel Java SDK default propagation: W3C TraceContext (traceparent)

These are different header formats. Neither service reads the other's format.
-> cart-service sends: x-datadog-trace-id: 8423012345678901234
-> checkout-service expects: traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
-> checkout-service sees no known propagation header -> starts a new trace
```

---

## Step 4: Fix Propagation Format Mismatch

### Option A: Configure dd-trace To Also Send W3C Headers

```javascript
// cart-service tracer.js (Node.js dd-trace)
const tracer = require('dd-trace').init({
  service: 'cart-service',
  propagationStyle: {
    inject: ['datadog', 'tracecontext'],   // send BOTH formats
    extract: ['datadog', 'tracecontext'],  // read BOTH formats
  },
})
```

### Option B: Configure OTel Java To Also Read Datadog Headers

```java
// checkout-service OTel config (Java)
// Add Datadog propagator to OTel propagation chain.
// Requires opentelemetry-extension-incubator dependency.

OpenTelemetrySdk.builder()
    .setPropagators(ContextPropagators.create(
        TextMapPropagator.composite(
            W3CTraceContextPropagator.getInstance(),  // reads traceparent
            // If using dd-java-agent, it handles Datadog headers automatically.
            // If using OTel agent only, add custom Datadog propagator.
        )
    ))
    .build();
```

### Option C: Standardize On W3C Everywhere (Recommended)

```text
Configure all services to use only W3C TraceContext.

Node.js (dd-trace):
  propagationStyle: { inject: ['tracecontext'], extract: ['tracecontext'] }

Java (OTel):
  -Dotel.propagators=tracecontext,baggage

Java (dd-java-agent):
  -Ddd.trace.propagation.style=tracecontext

Python (ddtrace):
  DD_TRACE_PROPAGATION_STYLE=tracecontext

This makes all services speak the same language.
```

---

## Step 5: Verify Fix

```text
After config change and restart:

APM -> Trace Explorer
  Filter: service:cart-service env:production resource:"POST /cart/add"

Click trace:
  cart-service POST /cart/add  [root span, traceID=abc]
    └── checkout-service POST /checkout  [child span, traceID=abc]  <- LINKED!
          └── payment-service POST /charge  [child span, traceID=abc]

Flame graph now shows the complete end-to-end trace from cart to payment.
```

---

## Step 6: Kafka / Async Context Propagation

A common variation: context broken at a Kafka message boundary.

```text
orders-service publishes Kafka event OrderCreated
fulfillment-service consumes the event

Problem: fulfillment-service starts a new trace for the event processing
         (no link to the original checkout trace)
```

### Java Kafka Producer (Manual Inject)

```java
// When publishing to Kafka, inject trace context into headers.
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

ProducerRecord<String, String> record = new ProducerRecord<>("orders", orderId, payload);

// Inject current trace context into Kafka message headers.
GlobalTracer.get().inject(
    GlobalTracer.get().activeSpan().context(),
    Format.Builtin.TEXT_MAP,
    new KafkaHeadersMapAdapter(record.headers())
);

producer.send(record);
```

### Java Kafka Consumer (Manual Extract)

```java
// When consuming from Kafka, extract trace context from headers.
ConsumerRecord<String, String> record = ...;

SpanContext parentContext = GlobalTracer.get().extract(
    Format.Builtin.TEXT_MAP,
    new KafkaHeadersMapAdapter(record.headers())
);

Span span = GlobalTracer.get().buildSpan("fulfillment.process")
    .asChildOf(parentContext)  // link to original trace
    .start();
```

---

## Common Root Causes Summary

| Root Cause | Symptom | Fix |
|---|---|---|
| Header format mismatch | Traces appear separate | Standardize on W3C or configure both formats |
| Uninstrumented HTTP client | No outbound span | Add tracing library or manual inject |
| Kafka/queue async break | Consumer starts new trace | Manual inject/extract on message headers |
| Missing library | Service has no traces at all | Install tracer and restart service |
| Sampling not propagated | Trace exists but sampled out | Set sampling priority in parent to keep=1 |

---

## Interview Sound Bite

Broken traces usually mean context propagation headers are not being sent, received, or understood. The most common production cause is a mixed environment: one service uses Datadog native headers and another uses W3C TraceContext headers. The fix is to configure all services to use the same format (W3C is the standard choice). For Kafka, context must be manually injected into message headers by the producer and extracted by the consumer. Debugging involves checking the entry span of the "disconnected" trace for the absence of a parent context, and checking the exit span of the "source" trace for injected headers.
