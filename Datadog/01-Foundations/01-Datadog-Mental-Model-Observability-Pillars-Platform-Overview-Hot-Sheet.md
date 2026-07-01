# 01. Datadog Mental Model: Observability Pillars, Platform Overview, Agent Architecture

## Goal

Understand what Datadog is, why it exists, and how its components connect before writing a single line of instrumentation.

---

## Why Observability Matters

```text
Traditional monitoring: "Is the service up?"
Observability: "Why is this request slow for users in us-east-1 on version 2.1.3?"
```

Observability is the ability to understand internal system state from external outputs (metrics, logs, traces). Datadog operationalizes observability at scale.

---

## The Three Pillars And What They Answer

| Pillar | Question Answered | Cost | Best For |
|---|---|---|---|
| Metrics | Is something wrong? How much? | Low | Alerting, capacity planning, SLOs |
| Logs | What happened? What did the code say? | Medium-High | Debugging, audit, compliance |
| Traces | Where did the request go? What was slow? | Medium | Latency, error root cause, dependencies |

The fourth pillar in Datadog: **RUM** (what did the user experience?) — frontend-side observability.

---

## Datadog Agent Architecture

```text
                         Datadog Cloud (intake.datadoghq.com)
                                  ^
                                  |  HTTPS + API Key
                         Datadog Agent
                        /     |     |      \
               Core Agent  APM Agent  Logs Agent  DogStatsD
               (metrics)   (traces)   (log tail)   (custom :8125)
               Process Agent  Network Agent
               (live procs)   (NPM flows)
```

The agent runs as a daemon process (systemd service, Docker container, or Kubernetes DaemonSet). It has one API key per agent, and all data flows outbound to `intake.datadoghq.com`.

---

## Signal Flow Summary

```text
Application -> DogStatsD UDP :8125 -> Agent Core -> Datadog (metrics)
Application -> dd-java-agent / dd-trace -> APM Agent :8126 -> Datadog (traces)
Log file / stdout -> Agent Logs -> Datadog (logs)
Browser SDK / RUM SDK -> Datadog Edge -> Datadog (RUM)
Synthetic Tests -> Datadog-managed workers -> Datadog (synthetics)
```

---

## Tagging: The Unifying System

Tags connect all signals. A query for `service:orders env:prod` returns:

- Metrics from that service/env
- Logs from that service/env
- Traces from that service/env

Without consistent tags, signals cannot be correlated.

Unified Service Tagging (UST) — the mandatory minimum:

```text
env:production
service:orders-service
version:1.2.3
```

These three tags must be on the agent, the APM tracer, and the application container/process simultaneously.

---

## Datadog Data Retention Defaults

| Signal | Default Retention |
|---|---|
| Metrics | 15 months |
| Logs | 15 days (configurable with archives) |
| Traces (sampled) | 15 days |
| RUM sessions | 30 days |
| Synthetic results | 12 months |

---

## What Datadog Is Not

| What people expect | Reality |
|---|---|
| Free observability tool | Commercial SaaS; cost tied to host count, metrics, log volume |
| Works without instrumentation | Requires agent and/or SDK installation |
| Can query all logs cheaply | High log volumes need retention filters and exclusion policies |
| APM works automatically | Requires library injection (Java agent, pip package, npm package) |
| Tag taxonomy is auto-generated | You must define and enforce the tag strategy |

---

## Use Case Map

| Use Case | Datadog Feature |
|---|---|
| CPU/memory alerting | Infrastructure Metrics + Monitor |
| API latency tracking | APM Service metrics |
| Error log analysis | Log Explorer with facets |
| Distributed trace debugging | Trace Explorer + flame graph |
| Frontend performance | RUM + Core Web Vitals |
| Uptime verification | Synthetic API test |
| SLA compliance | SLOs + error budgets |
| Kubernetes pod health | Container Monitoring + Live Containers |
| Security threat detection | Cloud SIEM + CSPM |
| Cost attribution | Tag-based metric aggregation |

---

## Interview Sound Bite

Datadog unifies metrics, logs, traces, RUM, and synthetics under a common tag taxonomy. The agent collects all signal types from the host; APM libraries instrument application code for distributed tracing. The three pillars answer different questions: metrics detect anomalies, logs describe events, and traces pinpoint root causes. The Unified Service Tagging standard (`env`, `service`, `version`) is the prerequisite for cross-signal correlation.
