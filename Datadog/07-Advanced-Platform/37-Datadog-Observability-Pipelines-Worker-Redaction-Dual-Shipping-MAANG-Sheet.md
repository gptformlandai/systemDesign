# 37. Datadog Observability Pipelines: Worker, Redaction, Routing, Dual Shipping

## Goal

Understand Observability Pipelines as a pre-ingestion processing layer for logs and metrics before data reaches Datadog or other destinations.

---

## Mental Model

Datadog log pipelines process data after it reaches Datadog.

Observability Pipelines process data inside your infrastructure before final routing.

```text
app logs -> Observability Pipelines Worker -> redact/enrich/filter/route -> Datadog + archive + SIEM
```

It is telemetry traffic control.

---

## Why It Exists

At scale, teams need to:

- Reduce log volume before ingestion.
- Redact sensitive data before it leaves the network.
- Route different logs to different destinations.
- Dual ship during vendor migration.
- Generate metrics from logs.
- Enforce tag governance centrally.
- Keep archive copies even when Datadog indexing is reduced.

---

## Datadog Log Pipeline vs Observability Pipeline

| Capability | Log Pipeline | Observability Pipeline |
|---|---:|---:|
| Runs inside Datadog | Yes | No |
| Runs in your infrastructure | No | Yes |
| Pre-ingestion redaction | Limited | Yes |
| Route to multiple destinations | Limited | Yes |
| Dual shipping | No | Yes |
| Volume control before Datadog billing | No/partial | Yes |
| UI-managed processing | Yes | Yes |

---

## Architecture

```text
Application / Fluent Bit / Syslog / Agent
  -> Observability Pipelines Worker
      -> parse
      -> enrich
      -> redact
      -> sample
      -> route
      -> generate metrics
  -> Datadog Logs
  -> S3 archive
  -> Security data lake
  -> Other downstream tool
```

The Worker is the data plane. Datadog UI manages the pipeline configuration.

---

## Common Pipeline Patterns

| Pattern | Use |
|---|---|
| Archive all logs | Send raw or normalized logs to S3/cloud storage |
| Dual ship | Send same stream to Datadog and another platform |
| Redact sensitive data | Remove PII/secrets before egress |
| Volume control | Drop/sample noisy logs before billing |
| Split logs | Route security logs differently from app logs |
| Enrichment | Add team/service/env/cloud/account metadata |
| Log-based metrics | Convert high-value event counts into metrics |

---

## Redaction Example

```text
Input log:
  user_email=jane@example.com card=4111111111111111 token=secret-abc

Pipeline:
  - detect email
  - detect credit card
  - detect token field
  - redact or hash

Output log:
  user_email=j***@example.com card=**** token=[REDACTED]
```

For regulated systems, redact before data leaves your VPC or cluster.

---

## Volume Control Example

```text
Rules:
  - drop health check logs
  - sample INFO logs at 10%
  - keep 100% of ERROR logs
  - keep 100% of security audit logs
  - archive full stream to S3
```

This controls Datadog indexing and ingestion cost while preserving compliance archives.

---

## Dual Shipping Migration

```text
Current:
  apps -> old logging platform

Migration:
  apps -> Observability Pipelines Worker
       -> old logging platform
       -> Datadog Logs

After validation:
  apps -> Observability Pipelines Worker
       -> Datadog Logs
       -> S3 archive
```

Dual shipping reduces migration risk.

---

## Pipeline Design Checklist

- What sources feed the Worker?
- Which fields are sensitive?
- Which logs must never be dropped?
- Which logs can be sampled?
- What destination receives full fidelity?
- What destination receives reduced fidelity?
- Which tags are mandatory?
- How is Worker health monitored?
- What happens if destination is unavailable?

---

## Worker Health Monitors

Monitor the pipeline itself:

```text
Worker CPU and memory
input event rate
output event rate
dropped event count
delivery error count
buffer utilization
destination latency
configuration version
```

Telemetry pipelines are production infrastructure. They need SLOs too.

---

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| Worker down | Logs stop flowing | HA deployment and buffer |
| Destination unavailable | Backpressure or drops | Retry, buffer, alternate route |
| Bad redaction rule | PII leak or over-redaction | Test rules and audit samples |
| Aggressive sampling | Missing incident evidence | Never sample errors/security logs blindly |
| No archive | Compliance gap | Send full stream to low-cost archive |

---

## Practical Question

> Log volume doubled after a new release, and Datadog log cost is rising quickly. The security team also requires PII redaction before logs leave the VPC. What design would you propose?

---

## Strong Answer

I would place Observability Pipelines Worker inside our infrastructure between log producers and destinations. The Worker would redact PII and secrets before egress, enrich logs with service/team/env/account tags, drop low-value health checks, sample high-volume INFO logs, keep all ERROR and security audit logs, and route a complete copy to S3 archive.

Datadog would receive the high-value operational stream, while the archive preserves full fidelity for compliance. I would monitor the Worker with input/output rate, drops, delivery errors, buffer usage, and latency. This gives cost control without losing incident or audit evidence.

---

## Interview Sound Bite

Datadog log pipelines process data after it arrives. Observability Pipelines process data before it arrives, inside your infrastructure, so you can redact, enrich, sample, route, archive, and dual ship telemetry at scale.
