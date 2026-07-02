# Datadog Mastery Sheet System

## What Is Datadog

Datadog is a cloud-scale observability, security, service management, and engineering platform that collects metrics, logs, traces, real user monitoring data, synthetic test results, security signals, CI/CD signals, cost signals, and service ownership metadata - all correlated by a unified tagging system.

```text
Infrastructure metrics  -> what is happening (CPU, memory, network)
Application logs        -> what the code said
Distributed traces      -> how a request moved through services
RUM                     -> what the user experienced
Synthetic tests         -> what would happen if a user visited right now
Security signals        -> what threats are active
CI/CD signals           -> how code moved from commit to production
Cost signals            -> where cloud and observability spend comes from
Catalog metadata        -> who owns each service and what standards it meets
```

All platform signals share the same tag vocabulary and can be navigated to each other from any view.

---

## Full Platform Mental Model

```text
Runtime observability:
  metrics + logs + traces + RUM + synthetics + profiles + data streams

Ownership and operations:
  Software Catalog + scorecards + incidents + on-call + workflow automation

Delivery:
  CI Visibility + test optimization + deployment tracking + Error Tracking

Security:
  Cloud SIEM + CSPM + CWS + App/API Protection + Code Security + Sensitive Data Scanner

Cost:
  Datadog usage attribution + Cloud Cost Management + unit economics

Governance:
  RBAC + SSO/SCIM + audit trail + Terraform/API as code + tag policy
```

The senior Datadog mindset is not "which dashboard do I open?" It is "which signal, owner, workflow, and control loop will reduce time to detect, time to understand, time to mitigate, and time to prevent?"

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
| Which team owns this failing service? | Software Catalog |
| Which uninstrumented service is talking to my app? | Universal Service Monitoring |
| Which method is burning CPU? | Continuous Profiler |
| Can I add a debug log without redeploying? | Dynamic Instrumentation |
| Is this exception new after the deploy? | Error Tracking |
| Which pipeline or test is slowing delivery? | CI Visibility |
| Should this log be redacted before ingestion? | Observability Pipelines |
| Is Lambda latency caused by cold starts or throttles? | Serverless Monitoring |
| Where is my Kafka/SQS pipeline lagging? | Data Streams Monitoring |
| Which service/team caused the cloud cost spike? | Cloud Cost Management |
| Is this API under attack or exposing sensitive behavior? | App/API Protection |
| Which incident workflow should run? | Incident Management / On-Call |
| Why did LLM token cost spike? | LLM Observability |

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

High-cardinality tags (user IDs, request IDs, IP addresses) should NEVER be used as metric tags - they cause cardinality explosion and cost spikes.

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
| Pro | Production incident resolution, SLO burn rate design, tag governance at scale |
| Platform/MAANG | Service catalog ownership, USM discovery, profiler/debugging, CI-to-prod correlation, pipelines, serverless, streams, FinOps, AppSec, incident automation, AI observability, governance as code |
