# 10. Datadog Dashboards: Widgets, Frontend RUM, Backend APM, SLO Display

## Goal

Build Datadog dashboards that show frontend user experience metrics alongside backend service health, correlate them visually, and display SLO status.

---

## Dashboard Types

| Type | Purpose |
|---|---|
| Timeboard | Time-synchronized widgets; good for time-based debugging |
| Screenboard | Free-form layout; good for NOC displays and overviews |
| Dashboard (current) | Unified type; supports both layout styles |

---

## Creating A Dashboard

1. Dashboards → New Dashboard.
2. Name it: `Orders Service - Full Stack Health`.
3. Use template variables for dynamic scoping.
4. Add widgets with metric/log/trace/RUM queries.

---

## Template Variables

Template variables let one dashboard cover multiple environments, services, or regions:

```text
# Add template variables in dashboard settings.
$env    -> default: production  -> values: production,staging,dev
$service -> default: orders-service -> values: (auto-discovered from tags)
$region  -> default: us-east-1  -> values: us-east-1,eu-west-1,ap-south-1
```

Use in widget queries:

```text
avg:http.server.duration.ms{env:$env,service:$service} by {endpoint}
```

---

## Widget Types And Use Cases

| Widget | Best For |
|---|---|
| Timeseries | Metric trends over time (latency, error rate, throughput) |
| Query Value | Current single value (current error rate, active users) |
| Top List | Ranked comparison (slowest endpoints, top errors) |
| Table | Multi-metric tabular view (per-service latency/errors/requests) |
| Heatmap | Distribution of values over time (latency percentile heat) |
| Bar Chart | Compare across dimensions (requests per service) |
| Funnel | Conversion rates (frontend user flows) |
| Geo Map | Geographic distribution (requests by country) |
| Log Stream | Live or recent log feed filtered to context |
| Trace List | Recent traces (errors, slowest) from APM |
| SLO Widget | SLO status, remaining error budget, burn rate |
| Iframe | Embed external URLs |
| Note | Documentation blocks inside dashboard |

---

## Section 1: Backend APM Metrics Widgets

### Requests Per Second

```text
Query: sum:trace.http.request.hits{env:$env,service:$service}.as_rate()
Widget: Timeseries
Title: Requests per Second
```

### Error Rate

```text
Query: 100 * sum:trace.http.request.errors{env:$env,service:$service}.as_rate()
      / sum:trace.http.request.hits{env:$env,service:$service}.as_rate()
Widget: Query Value + threshold coloring (red > 1%)
Title: HTTP Error Rate (%)
```

### P99 Latency

```text
Query: p99:trace.http.request.duration{env:$env,service:$service}
Widget: Timeseries
Title: P99 Request Duration
Y-axis: milliseconds
```

### Slowest Endpoints

```text
Query: avg:trace.http.request.duration{env:$env,service:$service} by {resource_name}
Sort: descending
Widget: Top List
Title: Slowest Endpoints (Avg Duration)
```

### Per-Service Error Rate Table

```text
Columns:
  - service (group by)
  - requests/sec: sum:trace.http.request.hits{env:$env}.as_rate() by {service}
  - error rate: same error rate formula grouped by {service}
  - p99 latency: p99:trace.http.request.duration{env:$env} by {service}
Widget: Table
```

---

## Section 2: Frontend RUM Metrics Widgets

### Core Web Vitals

```text
LCP (Largest Contentful Paint):
  Widget: Query Value
  Query: avg:rum.lcp{env:$env,service:$service}
  Threshold: green < 2500ms, yellow < 4000ms, red > 4000ms

FID (First Input Delay) / INP (Interaction to Next Paint):
  Widget: Query Value
  Query: avg:rum.fid{env:$env}

CLS (Cumulative Layout Shift):
  Widget: Query Value
  Query: avg:rum.cls{env:$env}
  Threshold: green < 0.1, yellow < 0.25, red > 0.25
```

### Page Load Time Trend

```text
Query: avg:rum.loading_time{env:$env} by {view.name}
Widget: Timeseries
Title: Page Load Time by View
```

### Session Error Rate

```text
Query: 100 * sum:rum.error.count{env:$env} / sum:rum.view.count{env:$env}
Widget: Query Value
Title: Frontend Session Error Rate (%)
```

### Top Pages By Load Time

```text
Query: avg:rum.loading_time{env:$env} by {view.name}
Widget: Top List
Title: Slowest Pages (Avg Load Time)
```

### User Sessions By Country

```text
Query: sum:rum.session.count{env:$env} by {geo.country}
Widget: Geo Map
Title: Sessions By Country
```

---

## Section 3: Infrastructure Metrics Widgets

### CPU And Memory By Service

```text
CPU:
  avg:system.cpu.user{env:$env,service:$service} by {host}

Memory:
  avg:system.mem.used{env:$env,service:$service} by {host}
```

### JVM Heap Utilization (Java Services)

```text
avg:jvm.heap_memory{env:$env,service:$service} by {host}
avg:jvm.heap_memory_committed{env:$env,service:$service} by {host}
Widget: Timeseries (two lines overlaid)
```

### Node.js Event Loop Lag

```text
avg:runtime.node.event_loop.delay.max{env:$env,service:$service}
Widget: Timeseries
Threshold annotation at 100ms
```

---

## Section 4: SLO Status Widgets

### SLO Widget Configuration

1. Add widget → SLO.
2. Select the SLO (e.g., `Orders Service Availability`).
3. Choose time window: 7d / 30d / 90d.
4. Show: remaining error budget + burn rate.

```text
SLO widget displays:
  - Current SLO achievement (e.g., 99.87% vs target 99.9%)
  - Remaining error budget (e.g., 12 minutes remaining of 43 minutes)
  - Status: OK / Warning / Breached
```

---

## Section 5: Log Stream Widget

```text
Query: service:$service env:$env status:error
Widget: Log Stream
Title: Recent Errors
Columns: timestamp, service, message, dd.trace_id
```

---

## Dashboard As Code

Use Terraform to manage dashboards version-controlled:

```hcl
resource "datadog_dashboard" "orders_health" {
  title       = "Orders Service Health"
  layout_type = "ordered"
  is_read_only = false

  template_variable {
    name    = "env"
    default = "production"
    prefix  = "env"
  }

  widget {
    timeseries_definition {
      title = "Requests Per Second"
      request {
        q            = "sum:trace.http.request.hits{env:$env,service:orders-service}.as_rate()"
        display_type = "line"
      }
    }
  }
}
```

---

## Interview Sound Bite

Datadog dashboards use template variables to scope widgets across environments and services. Backend APM widgets show requests/sec, error rate, and p99 latency using trace.http.request metrics. Frontend RUM widgets show Core Web Vitals (LCP, FID, CLS), page load times, and session error rates from the RUM SDK. SLO widgets display error budget remaining and current achievement. Dashboards can be managed as code via Terraform for version control and team-wide governance.
