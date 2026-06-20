# Spring Boot Observability Actuator Micrometer Interview Master Sheet

Target: Java Backend / Spring Boot / production and senior interviews.

This sheet covers:
- production readiness
- Spring Boot Actuator
- health checks
- metrics
- Micrometer
- logs and structured logging
- tracing
- correlation IDs
- OpenTelemetry awareness
- dashboards and alerting
- debugging production incidents

Goal:

```text
After reading this sheet, you should be able to explain how to operate a Spring Boot service
in production: health, metrics, logs, traces, alerts, dashboards, and incident debugging.
```

---

## 0. How To Use This Guide By Level

| Level | Focus |
|---|---|
| Beginner | Actuator, health, metrics, logs |
| Intermediate | Micrometer, Prometheus, custom metrics, log correlation |
| Senior | tracing, SLOs, alert design, production debugging |
| MAANG-ready | RED/USE metrics, high-cardinality traps, incident communication |

Strong line:

```text
Observability means I can understand what the service is doing from the outside using
metrics, logs, traces, and health signals.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| Actuator | Very high | Production readiness |
| Health checks | Very high | Kubernetes/load balancer behavior |
| Metrics | Very high | Operations visibility |
| Micrometer | High | Spring metrics abstraction |
| Prometheus endpoint | High | Common monitoring stack |
| Logs | Very high | Debugging |
| Correlation ID | Very high | Trace one request |
| Distributed tracing | High | Microservice debugging |
| OpenTelemetry | Medium-high | Modern observability |
| Custom metrics | High | Business visibility |
| Alerting | Very high | Senior production ownership |
| High-cardinality metrics | High | Common metrics mistake |
| Thread dump/heap dump | Medium | Debugging operations |

---

# 2. Observability Big Picture

Three pillars:

| Signal | Answers |
|---|---|
| Metrics | What is happening numerically? |
| Logs | What happened in this code path? |
| Traces | Where did this request spend time? |

Plus:
- health checks
- dashboards
- alerts
- profiling/dumps
- audit events

Strong answer:

```text
For production Spring Boot services, I expose health and metrics through Actuator, collect
application and JVM metrics through Micrometer, use structured logs with correlation IDs,
and enable tracing for cross-service request flow.
```

---

# 3. Spring Boot Actuator

Actuator provides production-ready endpoints.

Common endpoints:

| Endpoint | Purpose |
|---|---|
| `/actuator/health` | liveness/readiness/dependency health |
| `/actuator/info` | application info |
| `/actuator/metrics` | metrics list/details |
| `/actuator/prometheus` | Prometheus scrape |
| `/actuator/loggers` | view/change log levels |
| `/actuator/env` | environment properties |
| `/actuator/configprops` | configuration properties |
| `/actuator/threaddump` | thread dump |
| `/actuator/heapdump` | heap dump |
| `/actuator/mappings` | MVC mappings |

Strong answer:

```text
Actuator gives operational endpoints for health, metrics, diagnostics, and runtime insight.
Sensitive endpoints must be secured and only exposed intentionally.
```

---

# 4. Actuator Exposure

Example:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
```

Production caution:

```text
Do not expose everything publicly.
```

Sensitive endpoints:
- env
- configprops
- heapdump
- threaddump
- loggers
- shutdown

Best practice:
- expose health publicly if needed
- expose metrics to monitoring network
- protect sensitive endpoints with security
- never expose secrets

---

# 5. Health Checks

Health endpoint tells whether app is healthy.

Example:

```text
GET /actuator/health
```

Health indicators may include:
- database
- disk space
- Redis
- Kafka
- custom dependency

Strong answer:

```text
Health checks help platforms decide whether a service should receive traffic or be restarted.
I separate liveness from readiness when possible.
```

---

# 6. Liveness vs Readiness

| Check | Meaning | Failure Action |
|---|---|---|
| Liveness | app process is alive | restart container |
| Readiness | app can serve traffic | remove from load balancer |

Example:

```text
Database temporarily down:
readiness may fail
liveness should usually not fail immediately
```

Why:

```text
Restarting every pod because DB is down can create a restart storm.
```

Senior answer:

```text
I keep liveness focused on whether the process is stuck, and readiness focused on whether
the service can handle requests.
```

---

# 7. Metrics

Metrics are numeric measurements over time.

Common service metrics:
- request rate
- error rate
- latency
- saturation
- CPU
- memory
- GC
- thread pool usage
- database connection pool
- HTTP client latency
- Kafka consumer lag

RED metrics:

| Letter | Meaning |
|---|---|
| R | Rate |
| E | Errors |
| D | Duration |

USE metrics:

| Letter | Meaning |
|---|---|
| U | Utilization |
| S | Saturation |
| E | Errors |

---

# 8. Micrometer

Micrometer is the metrics facade used by Spring Boot.

It can export to:
- Prometheus
- Datadog
- New Relic
- Graphite
- OTLP
- many others

Strong answer:

```text
Micrometer is like SLF4J for metrics. Application code records metrics through Micrometer,
and the registry exports them to the monitoring backend.
```

---

# 9. Custom Counter

Example:

```java
@Service
class BookingMetrics {
    private final Counter bookingCreatedCounter;

    BookingMetrics(MeterRegistry registry) {
        this.bookingCreatedCounter = Counter.builder("booking.created")
                .description("Number of bookings created")
                .tag("source", "api")
                .register(registry);
    }

    void bookingCreated() {
        bookingCreatedCounter.increment();
    }
}
```

Use counter for:
- total bookings created
- failed payment attempts
- cache evictions
- message failures

Do not use counter for:
- current queue size
- active sessions

Use gauge for current value.

---

# 10. Timer

Use timer for latency.

Example:

```java
Timer.Sample sample = Timer.start(registry);
try {
    bookingService.createBooking(request);
} finally {
    sample.stop(Timer.builder("booking.create.duration")
            .tag("result", "success")
            .register(registry));
}
```

Track:
- average
- percentiles
- max
- count

Interview line:

```text
Latency percentiles are usually more useful than averages because tail latency hurts users.
```

---

# 11. High Cardinality Trap

Bad metric tag:

```java
.tag("userId", userId)
.tag("bookingId", bookingId)
.tag("email", email)
```

Why bad:
- creates millions of time series
- high memory/cost
- slow dashboards
- may leak sensitive data

Good tags:
- endpoint
- status
- method
- exception type
- region
- dependency name

Strong answer:

```text
Metric labels must be low-cardinality. I never tag metrics with user IDs, booking IDs,
emails, or unbounded values.
```

---

# 12. Prometheus

Prometheus commonly scrapes:

```text
/actuator/prometheus
```

Example config:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

Interview line:

```text
Spring Boot plus Micrometer can expose Prometheus-format metrics through Actuator.
```

---

# 13. Logs

Logs explain events.

Good log:

```text
level=INFO traceId=abc123 bookingId=B100 event=booking_confirmed status=CONFIRMED
```

Bad log:

```text
Done
```

Log levels:

| Level | Use |
|---|---|
| ERROR | failed operation requiring attention |
| WARN | unusual but handled |
| INFO | important business/operational event |
| DEBUG | detailed troubleshooting |
| TRACE | very fine internal detail |

---

# 14. Structured Logging

Structured logs are machine-readable.

Example JSON:

```json
{
  "level": "INFO",
  "traceId": "abc123",
  "bookingId": "B100",
  "event": "booking_created",
  "status": "CONFIRMED"
}
```

Benefits:
- easier searching
- dashboards
- alerting
- correlation
- safer parsing

Strong answer:

```text
In production, I prefer structured logs with consistent fields rather than free-form
messages only.
```

---

# 15. Correlation ID

Correlation ID links logs across systems.

Flow:

```text
client request has X-Correlation-Id
API logs it
API passes it to downstream service
downstream logs it
```

If missing:

```text
generate one at edge
```

Use cases:
- trace one user issue
- connect gateway/API/downstream logs
- debug distributed request

Strong answer:

```text
Correlation IDs are essential because one user request may pass through many services.
Every log line and outbound call should carry the same ID.
```

---

# 16. MDC

MDC stores log context per thread.

Example:

```java
try {
    MDC.put("correlationId", correlationId);
    log.info("Creating booking");
} finally {
    MDC.clear();
}
```

Trap:

```text
MDC uses ThreadLocal, so async/reactive/thread-pool execution needs context propagation.
```

---

# 17. Distributed Tracing

Tracing shows request path across services.

Trace:

```text
one end-to-end request
```

Span:

```text
one operation inside trace
```

Example:

```text
Trace booking-create
  span API controller 40ms
  span DB insert 20ms
  span payment-service call 300ms
  span Kafka publish 10ms
```

Strong answer:

```text
Tracing helps find where time is spent across services. Logs tell what happened, metrics
show trends, traces connect the path.
```

---

# 18. OpenTelemetry Awareness

OpenTelemetry is a vendor-neutral standard for telemetry.

It can carry:
- traces
- metrics
- logs, depending setup

Why it matters:

```text
It reduces vendor lock-in and standardizes instrumentation.
```

Interview line:

```text
Modern Spring observability often integrates with Micrometer and OpenTelemetry-based
pipelines.
```

---

# 19. Business Metrics

Technical metrics are not enough.

Useful business metrics:
- bookings created
- booking failures by reason
- payment authorization success rate
- cancellation count
- search-to-booking conversion
- inventory conflict count
- refund processing delay

Strong answer:

```text
I combine technical metrics with business metrics because a service can be technically up
while business outcomes are broken.
```

---

# 20. Alerting

Good alerts:
- actionable
- tied to user impact
- have clear owner
- avoid noisy thresholds
- include dashboard/runbook links

Bad alert:

```text
CPU > 70% for 1 minute
```

Better alert:

```text
booking API 5xx rate > 2% for 10 minutes and request volume > minimum threshold
```

Strong answer:

```text
I prefer alerts based on symptoms users feel, such as high error rate or latency, not only
low-level resource signals.
```

---

# 21. SLI, SLO, SLA

| Term | Meaning |
|---|---|
| SLI | measured reliability indicator |
| SLO | internal target |
| SLA | external promise/contract |

Example:

```text
SLI: successful booking requests / total booking requests
SLO: 99.9% monthly success rate
SLA: contractual customer commitment
```

---

# 22. Production Debugging Flow

When API is slow:

1. Check request rate and latency percentiles.
2. Check error rate.
3. Check downstream latency.
4. Check DB pool usage.
5. Check thread pool saturation.
6. Check GC/memory.
7. Use traces for slow request path.
8. Use logs with correlation ID.
9. Compare deploy/change timeline.

When API returns 500:

1. Find error spike dashboard.
2. Search logs by exception and trace ID.
3. Identify endpoint and dependency.
4. Check recent deploy/config change.
5. Check downstream health.
6. Mitigate, then root-cause.

---

# 23. Actuator Debugging Endpoints

Useful in protected environments:

| Endpoint | Use |
|---|---|
| health | dependency health |
| metrics | metric values |
| loggers | adjust log level temporarily |
| mappings | see registered endpoints |
| beans | inspect beans |
| conditions | debug auto-configuration |
| configprops | inspect config binding |
| env | inspect environment |
| threaddump | stuck thread analysis |
| heapdump | memory leak analysis |

Security line:

```text
Powerful diagnostic endpoints must be secured because they can expose sensitive details.
```

---

# 24. Production Scenario: Slow Booking API

Symptom:

```text
P95 latency for POST /bookings increased from 300ms to 3s.
```

Debug path:
1. Metrics show high latency but low 5xx.
2. Traces show payment-service span increased.
3. HTTP client metrics show payment timeout rate rising.
4. Thread pool metrics show request threads waiting.
5. Logs with correlation ID show retries.
6. Circuit breaker dashboard shows half-open/open transitions.
7. Mitigation: reduce retry count, open circuit/fallback if safe, contact payment team.
8. Long-term: tune timeout/retry/bulkhead, add alert.

Strong interview answer:

```text
I would start with metrics to identify rate, errors, and duration. Then I would use traces
to locate the slow dependency and logs with correlation ID to inspect specific failures.
If payment-service is slow, I would check timeout, retry, circuit breaker, and thread pool
metrics before changing code.
```

---

# 25. Hot Interview Questions

### Q1. What is Actuator?

```text
Actuator provides production endpoints for health, metrics, diagnostics, and operational
inspection.
```

### Q2. Metrics vs logs vs traces?

```text
Metrics show trends and numbers, logs show events/details, traces show request flow across
services.
```

### Q3. What is Micrometer?

```text
Micrometer is Spring's metrics facade that records metrics and exports them to monitoring
systems like Prometheus.
```

### Q4. Liveness vs readiness?

```text
Liveness tells whether the process should be restarted. Readiness tells whether it should
receive traffic.
```

### Q5. What is correlation ID?

```text
An ID carried across logs and downstream calls to connect all work for one request.
```

### Q6. What is high cardinality?

```text
Metric labels with too many unique values, such as userId or bookingId, creating too many
time series.
```

### Q7. What should you alert on?

```text
User-impacting symptoms such as high error rate, high latency, failed business operations,
or dependency saturation.
```

---

# 26. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Expose all Actuator endpoints publicly | leaks sensitive data | secure and limit exposure |
| Only logs, no metrics | no trend visibility | RED/USE dashboards |
| Only metrics, no logs | hard root cause | structured logs |
| No correlation ID | cannot trace request | propagate ID |
| User ID as metric tag | high cardinality | low-cardinality labels |
| Alert on noisy CPU only | alert fatigue | symptom-based alerts |
| Liveness checks DB | restart storm during DB outage | separate readiness |
| No business metrics | hidden business failure | track domain outcomes |
| Debug logs always on | cost/noise/sensitive data | temporary log level changes |

---

# 27. One-Hour Revision Plan

### First 15 Minutes: Actuator

Revise:
- health
- metrics
- info
- loggers
- endpoint exposure

Must say:

```text
Actuator endpoints are powerful and must be secured in production.
```

### Next 15 Minutes: Metrics

Revise:
- Micrometer
- counters
- timers
- gauges
- Prometheus
- high cardinality

Must say:

```text
Metrics labels must be low-cardinality.
```

### Next 15 Minutes: Logs And Traces

Revise:
- structured logs
- correlation ID
- MDC
- trace/span
- OpenTelemetry

Must say:

```text
Metrics tell me there is a problem; traces and logs help find where and why.
```

### Final 15 Minutes: Operations

Revise:
- liveness/readiness
- alerts
- SLOs
- incident debugging
- dashboards

Must say:

```text
Alerts should map to user impact and have a clear response.
```

---

# 28. Final Rapid Revision Sheet

| Need | Concept |
|---|---|
| Production endpoints | Actuator |
| App can receive traffic | readiness |
| App process alive | liveness |
| Numeric time-series | metrics |
| Metrics facade | Micrometer |
| Prometheus scrape | `/actuator/prometheus` |
| Request event detail | logs |
| Machine-readable logs | structured logging |
| Link logs across services | correlation ID |
| End-to-end request path | trace |
| Single operation in trace | span |
| Vendor-neutral telemetry | OpenTelemetry |
| User-impact target | SLO |
| Too many metric labels | high cardinality |

---

# 29. Strong Closing Answer

If interviewer asks:

```text
How do you make a Spring Boot service production observable?
```

Say:

```text
I enable Actuator for health, metrics, and controlled diagnostics, expose Prometheus metrics
through Micrometer, and define dashboards around request rate, errors, latency, and saturation.
I use structured logs with correlation IDs and distributed tracing to follow requests across
services. I separate liveness and readiness, secure sensitive Actuator endpoints, avoid
high-cardinality metric tags, and create alerts based on user-impacting symptoms and SLOs.
```

---

# 30. Official Source Notes

Useful official references:

- Spring Boot Actuator: https://docs.spring.io/spring-boot/reference/actuator/index.html
- Spring Boot Observability: https://docs.spring.io/spring-boot/reference/actuator/observability.html
- Spring Boot Metrics: https://docs.spring.io/spring-boot/reference/actuator/metrics.html
- Spring Boot Tracing: https://docs.spring.io/spring-boot/reference/actuator/tracing.html
- Micrometer: https://micrometer.io/docs
- OpenTelemetry: https://opentelemetry.io/docs/

