# Observability — Structured Logging, MDC, Micrometer, OpenTelemetry — Gold Sheet

> Topic: How production systems emit structured logs, distributed traces, and metrics — the three pillars of observability

---

## 1. Intuition

When a production bug is reported at 2am, two things determine how fast you resolve it: (1) can you find the single trace that shows what went wrong, and (2) did you see the metric spike before the alert fired? Without structured logging, distributed tracing, and metrics, you're reading raw text logs and guessing. Observability is the engineering discipline of making systems self-explaining.

Beginner version:

> Logs tell you what happened. Metrics tell you how much. Traces tell you where in the chain it slowed down. Structured logging, Micrometer, and OpenTelemetry are the tools that wire all three together.

---

## 2. Definition

- **Observability:** The ability to understand the internal state of a system from its external outputs (logs, metrics, traces).
- **Structured logging:** Logs emitted as JSON — machine-readable and searchable without regex.
- **MDC (Mapped Diagnostic Context):** Thread-local key-value store that SLF4J uses to inject traceId/spanId into every log line automatically.
- **Micrometer:** Vendor-neutral metrics facade (like SLF4J for metrics). Spring Boot auto-configures it.
- **OpenTelemetry:** CNCF standard for distributed tracing, metrics, and logs — single agent instruments everything.

---

## 3. SLF4J + Logback — Structured JSON Logging

Default Spring Boot logs are plain text. Production systems need JSON for ELK/Loki/Splunk.

**Dependency (`build.gradle`):**

```groovy
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

**`logback-spring.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="APP_NAME" source="spring.application.name"/>

    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- Rename default "message" field to "msg" (ECS format) -->
            <fieldNames>
                <message>msg</message>
                <logger>logger</logger>
                <thread>thread</thread>
            </fieldNames>
            <!-- Custom static fields -->
            <customFields>{"app":"${APP_NAME}","environment":"${SPRING_PROFILES_ACTIVE}"}</customFields>
            <!-- Include MDC fields automatically -->
            <includeMdc>true</includeMdc>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE"/>
    </root>
</configuration>
```

**Result — one log line looks like:**

```json
{
  "@timestamp": "2026-06-28T14:23:05.123Z",
  "level": "INFO",
  "logger": "com.marriott.booking.BookingService",
  "thread": "http-nio-8080-exec-3",
  "msg": "Booking confirmed for hotel NYCMQ",
  "app": "booking-service",
  "environment": "production",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "hotelCode": "NYCMQ",
  "bookingId": "B-98765",
  "durationMs": 145
}
```

---

## 4. MDC — Mapped Diagnostic Context

MDC injects fields (like traceId) into every log line emitted by the current thread — without passing them as parameters everywhere.

**Manual MDC in a filter (for non-OpenTelemetry setups):**

```java
@Component
@Order(1)
public class RequestContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        // Extract traceId from W3C traceparent header (or generate new)
        String traceId = extractOrGenerateTraceId(request);
        String spanId  = generateSpanId();
        String userId  = resolveUserId(request);

        try {
            MDC.put("traceId", traceId);         // Appears in every log line this thread emits
            MDC.put("spanId",  spanId);
            MDC.put("userId",  userId);

            // Set W3C traceparent on response so downstream services propagate it
            ((HttpServletResponse) res).setHeader("traceparent",
                "00-" + traceId + "-" + spanId + "-01");

            chain.doFilter(req, res);

        } finally {
            MDC.clear();   // CRITICAL — thread pool reuse means stale MDC if not cleared
        }
    }
}
```

**W3C TraceContext header format:**

```
traceparent: 00-{traceId-32hex}-{spanId-16hex}-{flags}

Example:
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
              version  traceId (128-bit)              parentSpanId     sampled
```

**Async threads and MDC:** MDC is thread-local — if you use `@Async`, `CompletableFuture`, or virtual threads, you must copy the MDC map:

```java
Map<String, String> contextMap = MDC.getCopyOfContextMap();
executor.submit(() -> {
    MDC.setContextMap(contextMap);   // Restore MDC in worker thread
    try {
        // async work
    } finally {
        MDC.clear();
    }
});
```

---

## 5. Micrometer — Metrics Facade

Micrometer is the `slf4j-api` of metrics. Spring Boot auto-configures it with `spring-boot-actuator`. You code against Micrometer's API; it exports to Prometheus, Datadog, CloudWatch, etc.

**Auto-configured metrics (free with Actuator):**
- JVM: heap/nonheap memory, GC pauses, thread count, class loading
- HTTP: `http.server.requests` — by uri, method, status, exception
- DataSource: HikariCP pool metrics (active/idle/pending connections)

**Custom metrics:**

```java
@Service
@RequiredArgsConstructor
public class BookingService {

    private final MeterRegistry registry;

    // Counter — monotonically increasing count
    private final Counter bookingConfirmedCounter;
    private final Counter bookingFailedCounter;

    @PostConstruct
    public void initMetrics() {
        bookingConfirmedCounter = Counter.builder("booking.confirmed")
            .description("Number of successfully confirmed bookings")
            .tag("service", "booking")
            .register(registry);

        bookingFailedCounter = Counter.builder("booking.failed")
            .description("Number of failed booking attempts")
            .register(registry);
    }

    @Transactional
    public Booking confirmBooking(BookingRequest request) {
        // Timer — measures duration with histogram (for P99 latency)
        return Timer.builder("booking.confirm.duration")
            .description("Time to confirm a booking end-to-end")
            .tag("hotelCode", request.getHotelCode())
            .register(registry)
            .recordCallable(() -> {
                try {
                    Booking booking = processBooking(request);
                    bookingConfirmedCounter.increment();
                    return booking;
                } catch (Exception e) {
                    bookingFailedCounter.increment();
                    throw e;
                }
            });
    }
}
```

**Gauge — for current value (queue depth, cache size):**

```java
@PostConstruct
public void registerPendingBookingsGauge() {
    Gauge.builder("booking.pending.count", bookingRepo, repo -> repo.countByStatus(PENDING))
         .description("Current number of pending bookings")
         .register(registry);
}
```

**Prometheus scrape endpoint:**

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

Exposed at `/actuator/prometheus` → Prometheus scrapes this → Grafana visualizes.

---

## 6. OpenTelemetry — Auto-Instrumentation

OpenTelemetry (OTel) is the CNCF standard that unifies traces, metrics, and logs. The Java javaagent automatically instruments Spring Boot, JPA queries, HTTP calls, Kafka producers/consumers — no code changes.

**Run with javaagent:**

```dockerfile
FROM eclipse-temurin:21-jre
COPY opentelemetry-javaagent.jar /otel-agent.jar
COPY booking-service.jar /app.jar

ENV JAVA_OPTS="-javaagent:/otel-agent.jar"
ENV OTEL_SERVICE_NAME=booking-service
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
ENV OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production,service.version=1.4.2
ENV OTEL_TRACES_SAMPLER=parentbased_traceidratio
ENV OTEL_TRACES_SAMPLER_ARG=0.1   # Sample 10% of traces in production

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
```

**What the javaagent instruments automatically:**
- All Spring MVC / Spring WebFlux HTTP requests → spans with HTTP method, URL, status
- JPA/Hibernate queries → spans with SQL statement
- RestTemplate / WebClient outbound calls → span + propagates `traceparent`
- Kafka publish/consume → span context propagated through Kafka headers
- `@Scheduled` tasks → spans
- Redis calls → spans

**Manual span for custom code:**

```java
import io.opentelemetry.api.trace.Tracer;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final Tracer tracer;

    public int calculatePoints(Booking booking) {
        Span span = tracer.spanBuilder("loyalty.calculatePoints")
            .setAttribute("booking.id", booking.getId().toString())
            .setAttribute("hotel.code", booking.getHotelCode())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            int points = computePoints(booking);
            span.setAttribute("points.awarded", points);
            return points;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();   // ALWAYS end the span
        }
    }
}
```

**OTel Collector pipeline:**

```
Application (OTLP gRPC) → OTel Collector → [Jaeger for traces]
                                          → [Prometheus for metrics]
                                          → [Loki for logs]
```

---

## 7. Python Structured Logging with structlog

```python
import structlog
import logging

# One-time config at app startup
structlog.configure(
    processors=[
        structlog.contextvars.merge_contextvars,        # Thread-local context (like MDC)
        structlog.processors.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer(),            # Output as JSON
    ],
    wrapper_class=structlog.make_filtering_bound_logger(logging.INFO),
    context_class=dict,
    logger_factory=structlog.PrintLoggerFactory(),
)

log = structlog.get_logger()

# Usage — bind context once, all subsequent logs include it
log = log.bind(trace_id="4bf92f...", service="booking-service")
log.info("booking_confirmed", booking_id="B-98765", hotel_code="NYCMQ", duration_ms=145)

# Output:
# {"trace_id": "4bf92f...", "service": "booking-service", "event": "booking_confirmed",
#  "booking_id": "B-98765", "hotel_code": "NYCMQ", "duration_ms": 145,
#  "level": "info", "timestamp": "2026-06-28T14:23:05.123Z"}
```

---

## 8. Log Aggregation Pipeline

```
Pod logs (stdout JSON)
    → Fluent Bit DaemonSet (K8s node agent)
        → Parses JSON, adds pod/namespace labels
            → Sends to:
                Elasticsearch / OpenSearch (Kibana for search/dashboards)
                OR
                Grafana Loki (LogQL, co-located with Grafana + Prometheus)

Query examples (Loki LogQL):
  {app="booking-service"} | json | traceId = "4bf92f..."
  {app="booking-service"} | json | level="ERROR" | rate()[5m] > 0
```

---

## 9. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Logging PII (email, credit card, loyalty number) | GDPR/PCI violation in logs | Mask/redact before logging; use `@JsonIgnore`-equivalent for log serialization |
| Not clearing MDC at end of request | Thread pool reuse means next request inherits previous request's traceId | Always clear in `finally` block in the filter |
| Using `System.out.println` instead of SLF4J | No log level control, no MDC, not JSON, pollutes structured log stream | Use `Logger log = LoggerFactory.getLogger(this.getClass())` everywhere |
| Logging in a tight loop | Log volume overwhelms Elasticsearch; disk fills; back-pressure kills the app | Use WARN/ERROR for loop anomalies only; add rate limiting with Logback's DuplicateMessageFilter |
| Missing `span.end()` in finally | OpenTelemetry span leaks — orphan spans in Jaeger; memory leak | Always end spans in finally block |

---

## 10. Interview Insight

Strong answer:

> Our observability stack has three pillars. For logs: SLF4J + Logback with logstash-logback-encoder emits JSON to stdout; a request filter injects traceId/spanId into MDC so every log line from that request carries the trace context — without any code change to service methods. For metrics: Micrometer auto-configures JVM, HTTP, and HikariCP metrics; we add custom Counters and Timers for business events; Prometheus scrapes `/actuator/prometheus`; Grafana displays. For traces: OpenTelemetry javaagent instruments Spring MVC, JPA, and Kafka automatically; spans export to an OTel Collector which fans out to Jaeger. The traceId propagates via W3C `traceparent` header across all microservices, so a single booking request can be traced end-to-end across five services.

Follow-up trap:

> Why not just use log.info("traceId=" + traceId) in every method?

Good answer:

> Two reasons. First, MDC injects the traceId into every log line from that thread automatically — you don't have to pass it through every method call or remember to include it. Second, JSON structured logs let Elasticsearch filter by `traceId` as a field — no regex parsing. If you concatenate it as a string, querying becomes fragile and error-prone.

---

## 11. Revision Notes

- One-line summary: Structured JSON logs with Logback + MDC carry traceId automatically; Micrometer exposes metrics to Prometheus; OpenTelemetry javaagent instruments traces with zero code change; W3C traceparent propagates context across services.
- Three keywords: MDC, Micrometer, OpenTelemetry javaagent.
- One interview trap: MDC is thread-local — async threads lose it unless you explicitly copy and restore the context map.
- Memory trick: Three pillars = Logs (what) + Metrics (how much) + Traces (where). SLF4J+MDC, Micrometer, OTel javaagent — one tool per pillar.
