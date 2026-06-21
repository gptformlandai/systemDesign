# Microservices Observability SLO OpenTelemetry Deep Dive Gold Sheet

> Track: Microservices Interview Track - Group 3 Senior Path  
> Goal: explain how to instrument, operate, and debug microservices with production-grade observability.

Read after the observability/operations/multi-region master sheet.

---

## 1. Observability Mental Model

Monitoring tells you known symptoms.
Observability helps you ask new questions about unknown failures.

Microservice observability needs three things:

1. Request context across service boundaries.
2. Golden signals for each service and dependency.
3. Business-level signals for user-impacting flows.

Strong answer:

```text
In microservices, I need metrics to detect the issue, traces to locate the failing hop, and
logs to explain the code-path details. All three must share correlation context.
```

---

## 2. Signals And Tools

| Signal | Purpose | Example Tools |
|---|---|---|
| Metrics | aggregate trends and alerts | Prometheus, CloudWatch, Datadog |
| Dashboards | operational visibility | Grafana, Datadog, New Relic |
| Traces | request path and latency breakdown | OpenTelemetry, Jaeger, Tempo, Zipkin |
| Logs | detailed event evidence | ELK, Splunk, Loki, Cloud logging |
| Profiles | CPU/memory hot paths | async-profiler, py-spy, JFR |
| Events | deploys/config/incidents | CI/CD annotations, audit logs |

Interview point:

```text
Tool names matter less than knowing which signal answers which debugging question.
```

---

## 3. OpenTelemetry Basics

OpenTelemetry provides vendor-neutral instrumentation.

Core terms:

| Term | Meaning |
|---|---|
| Trace | end-to-end request journey |
| Span | one operation in a trace |
| Span context | trace id, span id, baggage |
| Propagation | carrying context across HTTP, gRPC, Kafka |
| Collector | receives, processes, exports telemetry |

Flow:

```text
Service instrumentation -> OpenTelemetry Collector -> backend
  -> traces: Jaeger/Tempo/vendor
  -> metrics: Prometheus/vendor
  -> logs: logging backend/vendor
```

Strong answer:

```text
OpenTelemetry lets services emit standardized traces, metrics, and logs without locking the
application code to one observability vendor.
```

---

## 4. Trace Propagation

Every service must propagate trace context.

HTTP headers often include:

```text
traceparent: 00-<trace-id>-<span-id>-01
tracestate: vendor-specific-data
```

Kafka propagation:

```text
event headers include trace context and correlation id
```

Common mistake:

```text
HTTP calls are traced, but async Kafka consumers start new traces with no link to the
original booking request.
```

Better answer:

```text
I propagate trace context through both synchronous calls and message headers so async work
can be linked to the user request or causally related event.
```

---

## 5. Correlation ID vs Trace ID

| ID | Purpose |
|---|---|
| Correlation ID | business/request identifier used in logs and support workflows |
| Trace ID | telemetry identifier used by tracing systems |
| Idempotency Key | duplicate command protection |
| Event ID | duplicate event processing protection |

Do not mix them blindly.

Strong answer:

```text
A trace ID helps observability tools. A correlation ID helps humans and logs. An idempotency
key protects correctness. They can be linked, but they are not the same concept.
```

---

## 6. Golden Signals

For each service, track:

| Signal | Questions |
|---|---|
| Latency | How slow is it? p50/p95/p99? |
| Traffic | How much request/event volume? |
| Errors | How many failures by type/status? |
| Saturation | Which resource is near limit? |

For backend services, add:

- DB pool utilization and wait time
- external dependency latency
- cache hit rate
- thread pool/queue depth
- Kafka consumer lag
- retry count
- circuit breaker state
- DLQ count
- outbox pending age

---

## 7. SLIs, SLOs, SLAs

| Term | Meaning |
|---|---|
| SLI | measured signal, such as successful checkout rate |
| SLO | internal target, such as 99.9 percent successful checkout |
| SLA | external/customer contract, often with penalties |

Example SLO:

```text
99.9 percent of CreateBooking requests complete successfully within 800 ms over 28 days,
excluding client errors.
```

Strong answer:

```text
SLOs should reflect user experience, not only pod health. A booking service can be up while
checkout is failing because payment or inventory is broken.
```

---

## 8. Error Budget

Error budget:

```text
Allowed unreliability under the SLO.
```

If SLO is 99.9 percent over 30 days:

```text
Allowed failure budget = 0.1 percent of valid requests
```

Use error budget to decide:

- freeze risky deploys
- prioritize reliability work
- slow rollout speed
- investigate recurring incidents
- adjust alert thresholds

Interview answer:

```text
Error budgets turn reliability into an engineering trade-off. If we burn budget too fast,
we reduce release risk and fix reliability before adding more change.
```

---

## 9. Burn Rate Alerts

Burn rate asks:

```text
How fast are we consuming the error budget?
```

Example alert strategy:

| Window | Purpose |
|---|---|
| 5 min + 1 hour | fast detection of severe incident |
| 30 min + 6 hour | slower but sustained issue |
| 2 hour + 24 hour | long-running reliability degradation |

Strong answer:

```text
I prefer SLO burn-rate alerts over simple CPU or one-off error alerts because they connect
alerts to user impact and urgency.
```

---

## 10. Dashboard Design

Service dashboard sections:

1. Request rate, error rate, latency percentiles.
2. Dependency latency/error panels.
3. Saturation: CPU, memory, DB pool, thread pool, queues.
4. Async health: lag, DLQ, retry volume, outbox age.
5. Deploy annotations by version.
6. Business funnel: booking started, confirmed, failed, payment unknown.

Strong answer:

```text
A useful dashboard shows user impact, dependency health, saturation, and recent changes in
one view. It should help decide what to inspect next.
```

---

## 11. Logging Quality Bar

Good logs are structured and searchable.

Include:

- timestamp
- level
- service name
- version
- environment
- trace id
- correlation id
- user/account id when safe
- booking id/payment id when relevant
- error code
- safe message

Do not log:

- passwords
- tokens
- full credit card numbers
- secrets
- unnecessary PII
- huge payloads by default

Strong answer:

```text
Logs should explain decisions and failures without leaking sensitive data or creating noisy,
expensive storage.
```

---

## 12. Observability For Kafka

Producer metrics:

- publish rate
- publish error rate
- send latency
- retry count
- record size

Consumer metrics:

- consumer lag
- processing rate
- processing latency
- rebalance count
- handler error rate
- retry topic volume
- DLQ count

Critical alert:

```text
Oldest unprocessed event age exceeds business tolerance.
```

Why:

```text
Lag count alone can be misleading. Event age tells user-impacting staleness.
```

---

## 13. Observability For Outbox

Track:

- number of pending outbox rows
- age of oldest pending event
- relay publish success/failure
- retry count distribution
- stuck aggregate/event types
- duplicate publish rate if known

Strong answer:

```text
Outbox reliability depends on monitoring the relay. A growing oldest-event age means facts
are committed locally but not reaching other services.
```

---

## 14. Incident Debugging Flow

When alert fires:

```text
1. Confirm user impact and scope.
2. Check recent deploys/config changes.
3. Inspect golden signals by service.
4. Use traces to locate slow/failing dependency.
5. Check saturation metrics.
6. Mitigate first: rollback, disable feature, shed load, fail over.
7. Preserve evidence for root cause.
```

Strong answer:

```text
During an incident, I first reduce customer impact, then root-cause. The system should have
runbooks and dashboards that make the first 10 minutes calm and evidence-driven.
```

---

## 15. Deployment Annotations

Dashboards should show:

- service version
- deployment timestamp
- config change timestamp
- feature flag change
- database migration
- dependency rollout

Why:

```text
Many incidents are change-related. Without deploy annotations, teams waste time guessing.
```

---

## 16. Chaos And Failure Injection

Test resilience before production incidents.

Examples:

- add latency to Payment Service
- force 5xx from Availability Service
- kill a pod during checkout
- block Kafka publishing
- slow database queries
- increase consumer lag
- expire/rotate a secret in test

Rules:

- define hypothesis
- limit blast radius
- monitor SLO impact
- have rollback/stop mechanism
- run in lower environment first, then controlled production if mature

Strong answer:

```text
Chaos testing is useful only when observability and rollback are ready. It should validate
specific failure-handling assumptions, not randomly break production.
```

---

## 17. Load Testing Strategy

Load test should include:

- realistic traffic mix
- p95/p99 latency
- database pool metrics
- queue/consumer lag
- retry behavior
- cache warm/cold state
- autoscaling response
- downstream rate limits

Common mistake:

```text
Only testing average latency at low concurrency.
```

Better:

```text
Test the booking flow near expected peak, failure conditions, and retry amplification.
```

---

## 18. Cost Observability

Microservices can hide cost growth.

Track:

- cost per request
- log volume per service
- trace sampling rate
- cross-region traffic
- overprovisioned CPU/memory
- idle replicas
- expensive retries
- unnecessary fan-out

Strong answer:

```text
Production readiness includes cost visibility. A service can meet latency SLOs while wasting
money through noisy logs, overprovisioning, or retry amplification.
```

---

## 19. Common Interview Traps

| Trap | Better Answer |
|---|---|
| "We have logs, so we are observable" | logs alone do not show distributed latency path |
| "CPU alert means outage" | alert on user-impacting SLOs first |
| "Trace every request forever" | use sampling and protect cost/privacy |
| "Health check means service works" | readiness differs from business success |
| "Consumer lag count is enough" | event age and business impact matter |
| "Dashboard is observability" | instrumentation, alerts, runbooks, and ownership matter |

---

## 20. Strong Closing Answer

```text
For microservices, I instrument every service with metrics, traces, and structured logs using
consistent context propagation. I define SLIs and SLOs around user journeys like checkout,
alert on error-budget burn, monitor async pipelines through lag/DLQ/outbox age, and annotate
deployments so incidents can be mitigated quickly and debugged with evidence.
```
