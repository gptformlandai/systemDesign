# Datadog Mastery Sheet System

## What Is Datadog

Datadog is a cloud-scale observability and security platform that collects metrics, logs, traces, real user monitoring data, synthetic test results, and security signals — all correlated by a unified tagging system.

```text
Infrastructure metrics  -> what is happening (CPU, memory, network)
Application logs        -> what the code said
Distributed traces      -> how a request moved through services
RUM                     -> what the user experienced
Synthetic tests         -> what would happen if a user visited right now
Security signals        -> what threats are active
```

All six signal types share the same tag vocabulary and can be navigated to each other from any view.

---

## The Three Observability Pillars

```text
Metrics  -> numerical measurements over time (aggregatable, alertable, cheap)
Logs     -> event records with free-text and structured fields (verbose, searchable, expensive)
Traces   -> end-to-end request flows across services (diagnostic, causal, targeted)
```

Metrics tell you *something is wrong*. Logs tell you *what happened*. Traces tell you *where and why it happened*.

---

## Signal Decision Map

| Question | Signal |
|---|---|
| Is my API error rate above 1%? | metric or SLO |
| Why did this request fail? | trace + log |
| Which service is slowest? | trace (service map) |
| What did the Java exception say? | log |
| Is my database slow? | trace + Database Monitoring |
| What is my p99 latency? | metric or APM trace |
| Did this pod crash? | log + infrastructure metric |
| Is my SLO at risk? | SLO burn rate alert |
| What did the real user experience? | RUM |
| Is my API endpoint broken? | synthetic test |

---

## Datadog Agent Architecture

```text
Host / Container / Pod
  └── Datadog Agent
       ├── Core Agent        (metrics, service checks, system info)
       ├── Process Agent     (process list, Live Processes)
       ├── Network Agent     (NPM, flow data)
       ├── APM Agent         (trace collection on :8126)
       ├── Logs Agent        (log tail, container log collection)
       └── DogStatsD         (custom metric UDP/UDS on :8125)
```

The agent ships all signals to Datadog's intake endpoint using your API key. Nothing goes outbound without the API key.

---

## Tagging Philosophy

Tags are the foundation of everything. They enable:

- Scoping dashboards, monitors, and queries to a subset of infrastructure
- Correlating metrics with logs with traces
- Cost attribution
- Alert routing by team/service/env

Mandatory tag set for every service:

```text
env:production|staging|development
service:orders-service
version:1.2.3
team:platform
region:us-east-1
```

High-cardinality tags (user IDs, request IDs, IP addresses) should NEVER be used as metric tags — they cause cardinality explosion and cost spikes.

---

## APM Tracing Concepts Summary

```text
Trace     = complete journey of one request through all services
Span      = one unit of work within a trace (a single service, DB call, HTTP call)
traceID   = unique ID for the entire trace (same across all spans)
spanID    = unique ID for one specific span
parentID  = spanID of the parent span (forms the tree structure)
root span = first span (no parentID), represents the entry point
```

---

## Language Instrumentation Summary

| Language | Datadog Library | OTel Support |
|---|---|---|
| Java | dd-java-agent.jar (javaagent) | OTel Java SDK + OTLP exporter |
| Node.js | dd-trace (npm) | OTel JS SDK + OTLP exporter |
| Python | ddtrace (pip) | OTel Python SDK + OTLP exporter |
| Go | dd-trace-go | OTel Go SDK + OTLP exporter |
| Ruby | ddtrace (gem) | OTel Ruby SDK + OTLP exporter |

---

## Log Correlation Pattern

```text
For traces to link to logs:
  1. Application logs must include dd.trace_id and dd.span_id fields
  2. Log pipeline must parse these fields as facets
  3. Logs and traces must share the same service, env, and version tags
```

The Datadog libraries auto-inject trace/span IDs when the logging framework is supported. For unsupported frameworks, inject manually via MDC (Java), contextvars (Python), or async context (Node.js).

---

## Beginner To Pro Loop

| Stage | Focus |
|---|---|
| Beginner | Install agent, send a metric, collect a log, view in UI |
| Intermediate | Instrument an app with APM, correlate logs + traces |
| Practitioner | Build dashboards, add monitors, create SLOs |
| Senior | Kubernetes monitoring, OTel migration, cost governance |
| Pro/MAANG | Production incident resolution, SLO burn rate design, tag governance at scale |
