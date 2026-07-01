# 11. Datadog Monitors: Types, Alerts, Composite Monitors, Notification Channels

## Goal

Create effective monitors for production services: understand monitor types, configure threshold/anomaly/forecast detection, route alerts to the right channels, and build composite monitors.

---

## Monitor Types

| Monitor Type | When To Use |
|---|---|
| Metric | Alert on numeric thresholds (error rate > 1%, CPU > 80%) |
| Log | Alert on log query match count (ERROR log count > 50) |
| APM | Alert on service latency, error rate, or throughput from traces |
| Composite | Combine two monitors with AND/OR logic |
| Outlier | Alert when one host/service behaves differently from peers |
| Anomaly | Alert when metric deviates from predicted baseline |
| Forecast | Alert before metric will breach threshold |
| SLO | Alert on SLO burn rate or error budget exhaustion |
| Process | Alert when a process is running or not |
| Network | Alert on network flow anomalies (NPM) |

---

## Threshold Monitor: Example

```text
# Monitor: Orders API Error Rate

Type: Metric Monitor

Query:
  avg(last_5m):
    sum:trace.http.request.errors{env:production,service:orders-service}.as_rate()
    /
    sum:trace.http.request.hits{env:production,service:orders-service}.as_rate()
    * 100

Alert threshold:   > 2     (error rate above 2% = alert)
Warning threshold: > 1     (error rate above 1% = warning)
Recovery:          < 0.5   (auto-recover when below 0.5%)

Evaluation window: last 5 minutes
Notify no data after: 10 minutes (alert if metrics stop coming in)
```

---

## Anomaly Monitor: Example

```text
# Monitor: Orders Service Latency Anomaly

Type: Metric Monitor
Detection method: Anomaly (Agile or Robust algorithm)

Query:
  avg(last_1h):
    avg:trace.http.request.duration{env:production,service:orders-service}

Algorithm:
  Agile: adapts to seasonal trends (good for daily patterns)
  Robust: does not adjust for trends (good for flat metrics)
  Basic: simple rolling avg/stddev (no seasonality awareness)

Bounds: deviations = 2 (alert when metric is 2 standard deviations from normal)
```

---

## Forecast Monitor: Example

```text
# Monitor: Disk Space Will Fill In 12 Hours

Type: Metric Monitor
Detection method: Forecast

Query:
  avg(next_12h):
    avg:system.disk.in_use{env:production,host:orders-prod-01}

Alert when predicted value > 90% (before disk is full)
```

---

## Log Monitor: Example

```text
# Monitor: NullPointerException Count Spike

Type: Log Monitor

Query:
  logs("service:orders-service env:production @error.type:NullPointerException").rollup("count").last("5m")

Alert: > 10 NPE occurrences in 5 minutes
Warning: > 5
```

---

## APM Monitor: Example

```text
# Monitor: Orders Service P99 Latency

Type: APM Monitor
Service: orders-service
Resource: GET /orders/{id}
Metric: p99 latency

Alert threshold:   > 2000ms
Warning threshold: > 1000ms
Evaluation window: last 5 minutes
```

---

## Composite Monitor

Composite monitors combine two or more monitors with boolean logic.

```text
# Alert only when BOTH conditions are true:
# 1. Error rate is high
# 2. AND latency is also high
# (prevents false positives where only one metric spikes)

Composite formula:
  monitor_A AND monitor_B
  
  monitor_A = "Orders Error Rate > 2%"
  monitor_B = "Orders P99 Latency > 2000ms"

Result: alert fires only if both are in ALERT state simultaneously.
```

Another example:

```text
# Alert for any error spike UNLESS deployment is in progress.
  monitor_A AND NOT monitor_B
  
  monitor_A = "Error rate spike"
  monitor_B = "Deployment in progress" (set by deploy pipeline via API)
```

---

## Notification Message Template

```text
# Monitor notification body with template variables.

## {{monitor.name}} - {{#is_alert}}ALERT{{/is_alert}}{{#is_warning}}WARNING{{/is_warning}}

Service: {{service}}
Environment: {{env}}
Value: {{value}} (threshold: {{threshold}})

## Impact
Error rate has exceeded {{threshold}}%. Active requests may be failing.

## Investigation
- APM: https://app.datadoghq.com/apm/service/{{service}}
- Trace Explorer: https://app.datadoghq.com/apm/traces?service={{service}}&env={{env}}
- Dashboard: https://app.datadoghq.com/dashboard/xxx

## Runbook
https://wiki.example.com/runbooks/orders-service-error-rate

@pagerduty-orders-oncall
@slack-platform-alerts
```

---

## Notification Channels

### Slack Integration

```text
# Monitor notification field.
@slack-your-channel-name

# Message appears in Slack with:
- Monitor name and status
- Metric value vs threshold
- Link to the triggering metric graph
```

### PagerDuty Integration

```text
@pagerduty-your-service-name

Creates/resolves PagerDuty incidents automatically based on monitor state.
Alert = new incident
Recovery = auto-resolve
```

### Email

```text
@user@example.com
@team-list@example.com
```

### Webhook

```text
@webhook-your-webhook-name
# Configure webhook URL in Integration -> Webhooks.
```

---

## Monitor Tags

Apply tags to monitors for filtering and routing:

```text
Tags on the monitor:
  team:backend
  service:orders-service
  env:production
  severity:p1

Use for:
  - Filter monitors in the Monitors list view
  - Route PagerDuty to different escalation policies by tag
  - Suppress during maintenance by tag
```

---

## Muting And Downtime

```text
# Schedule downtime for planned maintenance:
Monitors -> Manage Downtime -> Schedule Downtime
  - Scope: service:orders-service env:production
  - Time: 2024-01-20 02:00 to 03:00 UTC
  - Message: "Planned maintenance - database schema migration"

# This suppresses all monitor notifications for that scope during that window.
```

---

## Interview Sound Bite

Datadog monitors have types for every signal: metric (threshold/anomaly/forecast), log, APM, composite, outlier, SLO, process, and network. Composite monitors prevent alert fatigue by requiring two conditions simultaneously (error rate AND latency both elevated). Notification messages use template variables for dynamic content and @mentions for routing to Slack, PagerDuty, or email. Downtime scheduling suppresses alerts during planned maintenance windows without disabling the monitor.
