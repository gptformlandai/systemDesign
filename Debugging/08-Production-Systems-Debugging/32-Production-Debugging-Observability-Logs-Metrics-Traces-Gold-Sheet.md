# 32. Production Debugging With Observability: Logs, Metrics, Traces

## Goal

Debug production systems when you cannot safely attach an IDE debugger. Use logs, metrics, traces, profiles, dumps, and deployment metadata to isolate root cause.

---

## Core Idea

In local development, the debugger pauses the process.

In production, pausing the process may become the outage.

```text
local debugging      -> pause, inspect variables, step
production debugging -> observe, correlate, compare, mitigate
```

Production debugging is evidence-driven.

---

## Signal Decision Map

| Question | First Signal |
|---|---|
| Is the system unhealthy? | metrics / SLO |
| Which service is unhealthy? | dashboard / service map |
| Which request path is slow? | distributed trace |
| What exception happened? | structured log / error tracker |
| Which version introduced it? | deployment marker / version tag |
| Is CPU/memory the cause? | profiler / runtime metrics |
| Is the process stuck? | thread dump / async task dump |
| Is the issue user-specific? | correlation ID / tenant tag |

---

## Minimum Production Debug Envelope

Every production service should emit:

```text
logs:
  timestamp, level, message, service, env, version
  correlation_id / request_id
  trace_id / span_id when tracing exists
  error stack trace

metrics:
  request rate
  error rate
  latency percentiles
  saturation: CPU, memory, threads, connection pools, queue depth

traces:
  inbound request span
  downstream HTTP/DB/cache/queue spans
  service/env/version/resource tags

events:
  deployment markers
  config changes
  autoscaling events
  incident annotations
```

Without these, production debugging becomes guessing.

---

## Golden Workflow

```text
1. Confirm impact.
   Is this user-facing? Which region/service/path/version?

2. Bound time.
   When did it start? Did it align with a deploy or traffic shift?

3. Split by dimension.
   env, service, version, region, endpoint, tenant, status code.

4. Follow the request.
   Trace from entry service to downstream dependencies.

5. Read the local evidence.
   Logs around the failing trace/span/correlation ID.

6. Check saturation.
   CPU, memory, GC, event loop, thread pool, DB pool, queue backlog.

7. Mitigate first if impact is active.
   rollback, disable flag, scale, drain, fail over, reduce traffic.

8. Verify recovery.
   Error rate, latency, SLO burn, logs, traces, user path.

9. Prevent recurrence.
   test, monitor, runbook, alert, limit, timeout, code fix.
```

---

## Correlation ID Pattern

Generate or accept a correlation ID at the edge.

```text
Incoming request:
  X-Request-ID: req-abc123

Log line:
  {
    "service": "orders-api",
    "env": "production",
    "version": "2.8.1",
    "request_id": "req-abc123",
    "trace_id": "9827319283",
    "level": "ERROR",
    "message": "payment authorization failed",
    "error": "TimeoutException"
  }

Trace:
  request_id=req-abc123
  service=orders-api
  resource=POST /orders
```

Rule: one user report should let you jump to one trace and all related logs.

---

## Metric Triage Patterns

| Pattern | Likely Meaning |
|---|---|
| Errors up, latency normal | validation bug, auth issue, downstream fast failure |
| Latency up, errors normal | slow dependency, saturation, queueing |
| Traffic down, errors normal | routing, DNS, load balancer, client failure |
| CPU high, latency high | CPU-bound code, event loop block, hot loop |
| Memory rising, GC high | leak or retention |
| Thread pool full, CPU normal | blocked I/O or slow dependency |
| DB pool exhausted | leak, long query, transaction stuck |
| Queue depth rising | consumer slower than producer |

---

## Deployment Correlation

Always ask:

```text
What changed?
  - code version
  - config
  - feature flag
  - schema migration
  - dependency version
  - traffic routing
  - infrastructure scaling
  - secret/cert rotation
```

If the error starts exactly after version `v2.8.1`, split all charts by `version`.

```text
error_rate{service:orders-api,version:v2.8.1}
latency_p99{service:orders-api,version:v2.8.1}
```

---

## Dynamic Log Level

Production-safe debugging often means temporarily increasing logs.

Spring Boot:

```bash
curl -X POST http://admin-host:8081/actuator/loggers/com.company.orders \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel":"DEBUG"}'
```

Rules:

- Scope to one package or component.
- Set a time box.
- Avoid PII/secrets.
- Return to normal after evidence is collected.
- Record the change in the incident timeline.

---

## What Not To Do

| Bad Move | Why |
|---|---|
| Attach JDWP/debugpy/inspector publicly | Full process control risk |
| Add broad DEBUG logs everywhere | Cost, noise, possible PII leak |
| Restart before collecting evidence | Destroys process state |
| Trust one graph | Misleading correlation |
| Debug without time bounding | You chase old noise |
| Fix before reproducing or isolating | Easy to patch the wrong cause |

---

## Practical Question

> Checkout errors jumped from 0.2% to 8% after a deploy. You cannot attach a debugger to production. How do you debug?

---

## Strong Answer

I would first confirm the blast radius by splitting error rate by service, endpoint, region, and version. Since it started after a deploy, I would compare old and new versions and inspect traces for failing checkout requests. From a representative failed trace, I would pivot to logs using the trace ID or request ID and identify the exact exception and failing downstream span.

If customer impact is active and the new version is clearly correlated, I would roll back or disable the feature flag first, then continue root cause analysis. I would verify recovery through error rate, p99 latency, SLO burn, and successful checkout traces. Finally, I would add a regression test and improve the monitor/runbook so the failure is easier to catch next time.

---

## Interview Sound Bite

Production debugging replaces pausing with correlation. I start from metrics to confirm impact, traces to locate the failing path, logs to explain local state, deployment markers to identify change, and runtime signals to check saturation. If impact is active, mitigation comes before perfect root cause.
