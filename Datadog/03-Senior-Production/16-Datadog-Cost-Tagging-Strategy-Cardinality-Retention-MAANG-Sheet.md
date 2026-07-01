# 16. Datadog Cost, Tagging Strategy, Cardinality Control, Retention

## Goal

Understand how Datadog pricing works, why custom metric cardinality is the primary cost driver, and how to govern tags, exclusion filters, and retention policies to control cost at scale.

---

## Datadog Pricing Model (Overview)

| Feature | Billing Driver |
|---|---|
| Infrastructure | Per host (agent installed on host) per month |
| APM | Per host running instrumented service |
| Log Management | Per GB ingested per month + per GB indexed |
| Custom Metrics | Per metric time series above free quota |
| RUM | Per session |
| Synthetics | Per test run |
| Security | Per host |

Infrastructure and APM are the largest base costs. Log ingestion and custom metrics are the most variable and most controllable costs.

---

## Custom Metric Cardinality: The Primary Cost Risk

A custom metric is any metric NOT included in the base infrastructure integration.

```text
Metric: orders.payment.duration
Tags on this metric:
  env (3 values: prod, staging, dev)
  service (5 values: orders, payments, inventory, users, notifications)
  region (3 values: us-east, eu-west, ap-south)
  status_code (10 values: 200,201,400,401,403,404,500,502,503,504)

Total time series = 3 * 5 * 3 * 10 = 450 time series for ONE metric.
```

### Cardinality Explosion Example

```text
GOOD: tags with controlled cardinality
  env:production, service:orders, region:us-east

BAD: tags with unbounded cardinality
  user_id:usr-12345       (millions of users = millions of series)
  request_id:req-abc123   (every request = one series = infinite cardinality)
  ip_address:10.0.1.45    (thousands of IPs = thousands of series per metric)
```

Rule: Never use high-cardinality values (user IDs, request IDs, IP addresses, timestamps) as metric tags.

---

## Tag Governance Policy

Enforce a standard tag vocabulary across all teams:

```text
Mandatory tags (must be present on all resources and metrics):
  env:production|staging|dev
  service:<service-name>
  version:<semver>
  team:<owning-team>
  region:<aws-region>

Optional enrichment tags:
  datacenter:<dc-name>
  availability_zone:<az>
  component:api|worker|scheduler
  tier:frontend|backend|data

Prohibited tags (blocked at tagging enforcement layer):
  user_id
  request_id
  session_id
  trace_id (belongs in logs/traces, not metrics)
  ip_address
```

---

## Log Cost Management

### Ingestion vs Indexing

```text
Ingestion: all logs received (billed per GB)
Indexing: logs stored for query (billed per GB, higher rate)

Strategy: ingest everything, index only what you query.
  -> Use archives to preserve all logs in S3 at low cost.
  -> Use exclusion filters to drop logs from indexes (not from archives).
```

### Exclusion Filters By Volume

```text
DEBUG/TRACE logs: typically 60-70% of log volume, rarely useful
  Filter: level:(DEBUG OR TRACE)
  Exclusion rate: 100%

Health check endpoints: high frequency, zero value
  Filter: @http.url_details.path:/health OR @http.url_details.path:/readiness
  Exclusion rate: 100%

Static asset requests: typically 40-50% of web server logs
  Filter: @http.url_details.path:(/static/* OR *.css OR *.js OR *.png OR *.ico)
  Exclusion rate: 100%

INFO logs (sampling): reduce volume while keeping representative sample
  Filter: level:INFO
  Exclusion rate: 80%  (keep 20% of INFO logs, discard 80%)
```

---

## Span Ingestion Controls

Reduce trace ingestion with sampling rules in the Datadog Agent:

```yaml
# In datadog.yaml.
apm_config:
  max_traces_per_second: 50    # default adaptive sampling target
  
  # Per-service rules.
  sampler:
    extra_sample_rate: 0       # no additional sampling
    target_traces_per_second: 50
```

Or per-service via environment variable:

```bash
DD_TRACE_SAMPLING_RULES='[
  {"service":"orders-service","sample_rate":1.0},
  {"service":"health-check-service","sample_rate":0.0},
  {"sample_rate":0.1}
]'
```

### Retention Filters

After ingestion, keep only the spans you need:

```text
APM -> Traces -> Trace Retention Filters -> Add Filter

Filter 1: Keep all error spans
  Query: @error:true
  Retention: 15 days

Filter 2: Keep all slow spans (>1s)
  Query: @duration:>1000000000
  Retention: 15 days

Filter 3: Keep sampled normal spans
  Query: env:production
  Retention: 3 days
  Sample rate: 20%
```

---

## Monitoring Your Own Datadog Usage

```text
# Metrics provided by Datadog for self-monitoring.
datadog.estimated_usage.metrics.custom           # custom metric count
datadog.estimated_usage.logs.ingested_bytes      # log bytes ingested
datadog.estimated_usage.logs.ingested_events     # log event count
datadog.estimated_usage.apm.host_count           # APM hosts billed
datadog.estimated_usage.rum.sessions.count       # RUM sessions billed

# Monitor your usage:
Monitor: sum:datadog.estimated_usage.metrics.custom{*} > 500000
Alert: "Custom metric count exceeds 500K - investigate cardinality"
```

---

## Cost Attribution By Team

Tag all metrics, monitors, dashboards, and logs with `team:` to enable per-team cost reports:

```text
# Usage Attribution in Datadog:
Plan and Usage -> Usage Attribution

Configure by tag:
  Primary: team
  Secondary: env

Generates monthly report:
  team:backend    -> 40% of custom metrics cost
  team:frontend   -> 25% of log indexing cost
  team:data       -> 35% of APM host cost
```

---

## Interview Sound Bite

Datadog cost is driven by four levers: infrastructure hosts, APM hosts, log indexing volume, and custom metric cardinality. Cardinality is the most dangerous: each unique tag combination is a separate time series, so adding user_id or request_id as metric tags can cause exponential cost explosion. Governance requires mandatory tags (env, service, version, team), prohibited high-cardinality tags, log exclusion filters to drop debug/health-check logs, and span retention filters to keep only errors and slow traces long-term. Usage Attribution with team tags enables per-team cost chargebacks.
