# 09. Datadog Log Correlation: TraceID and SpanID Injection (Java, Python, Node.js)

## Goal

Inject Datadog trace IDs and span IDs into application logs so that log entries link directly to their corresponding traces in the Datadog UI.

---

## Why Log Correlation Matters

Without correlation:

```text
Log: "Order processing failed for ORD-99001" -- timestamp only
Trace: high latency on orders-service -- spans only
  -> You cannot connect these without manual time-based searching.
```

With correlation:

```text
Log: "Order processing failed" dd.trace_id=8423012345678901234 dd.span_id=1234567890
  -> Click trace_id in Log Explorer
  -> Jump directly to the failing span in the flame graph
  -> See exact code path, SQL queries, and downstream call that failed
```

---

## Prerequisites For Log Correlation

1. Application logs must include `dd.trace_id` and `dd.span_id` fields.
2. Logs must be in JSON format (or have a pipeline that extracts trace fields from text).
3. Logs and traces must share the same `service`, `env`, and `version` tags.

---

## Part 1: Java Log Correlation

### Logback With JSON (Spring Boot)

The dd-java-agent automatically injects trace IDs into the MDC (Mapped Diagnostic Context) when `DD_LOGS_INJECTION=true`.

#### pom.xml (Logback JSON encoder)

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>7.4</version>
</dependency>
```

#### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <!-- Include MDC fields (dd.trace_id, dd.span_id are injected by dd-java-agent) -->
      <includeMdcKeyName>dd.trace_id</includeMdcKeyName>
      <includeMdcKeyName>dd.span_id</includeMdcKeyName>
      <includeMdcKeyName>dd.service</includeMdcKeyName>
      <includeMdcKeyName>dd.env</includeMdcKeyName>
      <includeMdcKeyName>dd.version</includeMdcKeyName>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>
</configuration>
```

#### Output JSON log

```json
{
  "@timestamp": "2024-01-15T10:23:45.123Z",
  "level": "ERROR",
  "message": "Order processing failed",
  "logger_name": "com.example.OrderService",
  "dd.trace_id": "8423012345678901234",
  "dd.span_id": "1234567890123456789",
  "dd.service": "orders-service",
  "dd.env": "production",
  "dd.version": "1.2.3"
}
```

### Log4j2 With JSON (Alternative)

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-layout-template-json</artifactId>
</dependency>
```

```json
// log4j2-layout-config.json
{
  "@timestamp": {
    "$resolver": "timestamp",
    "pattern": { "format": "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" }
  },
  "level": { "$resolver": "level", "field": "name" },
  "message": { "$resolver": "message", "stringified": true },
  "dd.trace_id": { "$resolver": "threadContext", "key": "dd.trace_id" },
  "dd.span_id": { "$resolver": "threadContext", "key": "dd.span_id" },
  "dd.service": { "$resolver": "threadContext", "key": "dd.service" },
  "dd.env": { "$resolver": "threadContext", "key": "dd.env" },
  "dd.version": { "$resolver": "threadContext", "key": "dd.version" }
}
```

### Manual MDC Injection (Without dd-java-agent Auto-Injection)

```java
import org.slf4j.MDC;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

// In a filter or interceptor:
public void injectTraceContext() {
    Span activeSpan = GlobalTracer.get().activeSpan();
    if (activeSpan != null) {
        String traceId = activeSpan.context().toTraceId();
        String spanId = activeSpan.context().toSpanId();
        MDC.put("dd.trace_id", traceId);
        MDC.put("dd.span_id", spanId);
        MDC.put("dd.service", "orders-service");
        MDC.put("dd.env", System.getenv("DD_ENV"));
        MDC.put("dd.version", System.getenv("DD_VERSION"));
    }
}

// Always clean up MDC after request.
MDC.clear();
```

---

## Part 2: Python Log Correlation

### Automatic Injection With ddtrace

```python
# Install.
# pip install ddtrace

import logging
from ddtrace import tracer
from ddtrace.contrib.logging import patch as patch_logging

# Auto-patch the logging module to inject trace context.
patch_logging()
logging.basicConfig(format='%(asctime)s %(levelname)s [%(name)s] %(message)s '
                           '[dd.trace_id=%(dd.trace_id)s dd.span_id=%(dd.span_id)s '
                           'dd.service=%(dd.service)s dd.env=%(dd.env)s '
                           'dd.version=%(dd.version)s]')
```

### JSON Logging With python-json-logger

```python
# pip install python-json-logger

import logging
from pythonjsonlogger import jsonlogger
from ddtrace import tracer

class DatadogJsonFormatter(jsonlogger.JsonFormatter):
    def add_fields(self, log_record, record, message_dict):
        super().add_fields(log_record, record, message_dict)
        span = tracer.current_span()
        if span:
            log_record['dd.trace_id'] = str(span.trace_id)
            log_record['dd.span_id'] = str(span.span_id)
        log_record['dd.service'] = 'orders-service'
        log_record['dd.env'] = 'production'
        log_record['dd.version'] = '1.2.3'

handler = logging.StreamHandler()
handler.setFormatter(DatadogJsonFormatter('%(asctime)s %(levelname)s %(message)s'))
logging.getLogger().addHandler(handler)
```

### Output

```json
{
  "asctime": "2024-01-15 10:23:45.123",
  "levelname": "ERROR",
  "message": "Order processing failed",
  "dd.trace_id": "8423012345678901234",
  "dd.span_id": "1234567890123456789",
  "dd.service": "orders-service",
  "dd.env": "production",
  "dd.version": "1.2.3"
}
```

### Manual Context Extraction

```python
from ddtrace import tracer

def get_trace_context():
    span = tracer.current_span()
    if span:
        return {
            "dd.trace_id": str(span.trace_id),
            "dd.span_id": str(span.span_id),
        }
    return {}
```

---

## Part 3: Node.js Log Correlation

### Automatic Injection With Winston

`dd-trace` with `logInjection: true` automatically injects into Winston:

```javascript
// tracer.js
const tracer = require('dd-trace').init({
  logInjection: true,  // enables automatic trace injection into Winston/Pino/Bunyan
})

module.exports = tracer
```

```javascript
// logger.js
require('./tracer')   // must init tracer first
const winston = require('winston')

const logger = winston.createLogger({
  format: winston.format.json(),  // JSON format required for trace injection
  transports: [new winston.transports.Console()],
})

module.exports = logger
```

#### Output (Winston with dd-trace injection)

```json
{
  "level": "error",
  "message": "Order processing failed",
  "dd.trace_id": "8423012345678901234",
  "dd.span_id": "1234567890123456789",
  "dd.service": "orders-service",
  "dd.env": "production",
  "dd.version": "1.0.0"
}
```

### Pino Integration

```javascript
const tracer = require('dd-trace').init({ logInjection: true })
const pino = require('pino')

const logger = pino({
  level: 'info',
  // Pino is auto-patched by dd-trace when logInjection is true.
})
```

### Manual Injection (Bunyan / Custom)

```javascript
const tracer = require('dd-trace')

function getTraceContext() {
  const span = tracer.scope().active()
  if (span) {
    const context = span.context()
    return {
      'dd.trace_id': context.toTraceId(),
      'dd.span_id': context.toSpanId(),
      'dd.service': 'orders-service',
      'dd.env': process.env.DD_ENV,
      'dd.version': process.env.DD_VERSION,
    }
  }
  return {}
}

// In route handler:
logger.info({
  ...getTraceContext(),
  message: 'Order created',
  orderId,
})
```

---

## Log Explorer: Using Trace IDs To Jump To Traces

1. Open Log Explorer in Datadog.
2. Find the error log with `dd.trace_id`.
3. Click the `dd.trace_id` value.
4. Select "View related trace" from the panel.
5. Datadog opens the Trace Explorer at that exact trace.

---

## Interview Sound Bite

Log correlation works by injecting dd.trace_id and dd.span_id into log output alongside every log entry. In Java, dd-java-agent populates MDC fields automatically when DD_LOGS_INJECTION=true; Logback/Log4j2 JSON encoders read MDC and include these in the log JSON. In Python, ddtrace.contrib.logging patches the logging module to inject trace context via format string interpolation. In Node.js, dd-trace with logInjection=true patches Winston and Pino automatically. All three require JSON-format logs and matching service/env/version tags to enable the click-to-trace jump from Log Explorer.
