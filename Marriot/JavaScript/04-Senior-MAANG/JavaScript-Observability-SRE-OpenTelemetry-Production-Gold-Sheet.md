# JavaScript Observability, SRE, And OpenTelemetry Production Gold Sheet

> Track: JavaScript Interview Track - Senior / MAANG  
> Goal: debug JavaScript production systems with evidence across browser, Node.js, edge, APIs, and builds.

---

## 1. Intuition

Observability is the difference between "users say it is slow" and "checkout p95 increased
because payment dependency latency rose in one region after version `abc123`." JavaScript
systems need browser RUM, server metrics, logs, traces, source maps, and release metadata.

---

## 2. Definition

- Logs: discrete events with context.
- Metrics: numeric time-series for trends and alerts.
- Traces: request paths across services.
- RUM: real-user monitoring in browsers.
- Profiles: CPU/memory evidence.
- Source maps: map minified production errors back to source.
- SLO: reliability target users can feel.

---

## 3. Why It Exists

JavaScript production issues often span layers:

```text
browser route -> bundle -> hydration -> API -> Node BFF -> dependency -> database -> queue
```

Without observability:

- minified stack traces are unreadable
- frontend and backend errors cannot be correlated
- p99 hides under average latency
- event-loop lag is invisible
- bundle regressions ship silently
- high-cardinality metrics explode cost
- service-worker or cache bugs look like random frontend failures

---

## 4. Browser Observability

Track:

- Core Web Vitals: LCP, INP, CLS
- route/page load timing
- JavaScript errors
- unhandled promise rejections
- resource loading failures
- long tasks
- user action breadcrumbs
- release version
- device/network class
- source-mapped stack traces

Browser event shape:

```json
{
  "type": "frontend_error",
  "route": "/checkout",
  "release": "web-2026.07.02.1",
  "message": "ChunkLoadError",
  "userTier": "anonymous",
  "traceId": "abc123"
}
```

Do not log raw PII or secrets.

---

## 5. Node Observability

Track:

- request rate, errors, duration
- p50/p95/p99 latency
- event-loop delay
- heap, RSS, external memory
- GC pauses
- CPU usage
- active handles/requests
- DB pool wait and dependency latency
- queue depth and job age
- unhandled rejections and uncaught exceptions
- process restarts and OOMs

Node event-loop metric sketch:

```js
import { monitorEventLoopDelay } from "node:perf_hooks";

const histogram = monitorEventLoopDelay({ resolution: 20 });
histogram.enable();

setInterval(() => {
  const p99Ms = histogram.percentile(99) / 1_000_000;
  console.log(JSON.stringify({ metric: "event_loop_delay_p99_ms", value: p99Ms }));
  histogram.reset();
}, 10_000);
```

---

## 6. Trace Context

Trace propagation:

```text
browser request id -> BFF request id -> downstream traceparent -> logs/metrics/errors
```

Node tools:

- `AsyncLocalStorage` for request context
- OpenTelemetry auto-instrumentation
- `diagnostics_channel` for structured diagnostic events
- structured logging with request ID, route, tenant, release

Good log fields:

```json
{
  "level": "info",
  "message": "payment_authorized",
  "route": "POST /bookings",
  "requestId": "req-123",
  "traceId": "trace-456",
  "bookingId": "B100",
  "durationMs": 184,
  "release": "api-2026.07.02.1"
}
```

---

## 7. Alerting And SLOs

Good alerts:

- user-facing error rate is high
- checkout p95/p99 exceeds SLO
- event-loop delay p99 crosses threshold
- queue age grows
- memory climbs over time
- frontend JS error rate spikes after release
- LCP/INP regresses for real users

Bad alerts:

- every single 500
- average latency only
- raw URL/user ID as metric labels
- alerts with no owner or runbook

---

## 8. Source Maps And Release Evidence

Production readiness requires:

- release ID embedded in frontend and backend
- source maps uploaded privately to error tooling
- build artifact tied to commit SHA
- bundle analyzer output or budget
- dependency scan where required
- rollback artifact available

Trap:

```text
Public source maps may expose source. Private upload to observability tooling is safer for many apps.
```

---

## 9. Runbook Formula

Use this for browser and Node incidents:

```text
symptom -> blast radius -> recent release -> user impact -> key metrics -> traces/logs
-> profile/snapshot if needed -> mitigation -> root cause -> prevention
```

Examples:

| Symptom | First Evidence |
|---|---|
| slow page | RUM by route/device, waterfall, long tasks |
| API p99 spike | route latency, traces, dependency spans |
| memory leak | heap/RSS trend, heap snapshot diff |
| high CPU | CPU profile, event-loop delay |
| bad bundle | release diff, analyzer, source-mapped errors |
| queue delay | queue age, worker errors, dependency latency |

---

## 10. Practical Question

> Checkout is slow for mobile users after a JavaScript release. How do you debug it?

---

## 11. Strong Answer

I would first identify whether the regression is browser, network, API, or dependency. In
the browser I would check RUM by route, device, and release: LCP, INP, JS errors, resource
failures, and long tasks. I would compare bundle size and route chunks before and after the
release and use source maps for stack traces. Then I would follow the trace/request ID into
the Node API and check p95/p99 latency, event-loop delay, dependency spans, DB pool wait,
and logs. I would mitigate by rollback, disabling a feature flag, or reducing traffic to the
bad path, then fix the measured root cause and add a budget, alert, test, or runbook.

---

## 12. Revision Notes

- One-line summary: JavaScript observability connects browser experience, server behavior,
  release metadata, and source-mapped evidence.
- Three keywords: RUM, traces, release.
- One interview trap: averages hide p99 user pain.
- One memory trick: metric tells where, trace tells path, profile tells why.

