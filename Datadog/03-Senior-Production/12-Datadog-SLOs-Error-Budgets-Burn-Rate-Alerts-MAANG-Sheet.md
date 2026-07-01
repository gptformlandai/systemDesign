# 12. Datadog SLOs: Error Budgets, Burn Rate Alerts, Multi-Window Strategy

## Goal

Design SLOs for production services, calculate error budgets, configure burn rate alerts, and respond to error budget exhaustion.

---

## SLO Concepts

| Concept | Definition |
|---|---|
| SLI | Service Level Indicator: the metric measured (e.g., request success rate) |
| SLO | Service Level Objective: the target for the SLI (e.g., 99.9% success rate) |
| Error budget | Allowed failure margin: 100% - SLO target |
| Burn rate | How fast error budget is being consumed vs normal |

---

## Error Budget Math

```text
SLO target:    99.9% availability over 30 days

Total time:    30 days * 24 hours * 60 minutes = 43200 minutes
Error budget:  43200 * (1 - 0.999) = 43.2 minutes of downtime allowed

If 43.2 minutes of errors occur -> error budget is exhausted -> SLO is breached.
```

| SLO Target | Monthly error budget |
|---|---|
| 99% | 7 hours 18 minutes |
| 99.5% | 3 hours 39 minutes |
| 99.9% | 43 minutes 12 seconds |
| 99.95% | 21 minutes 36 seconds |
| 99.99% | 4 minutes 19 seconds |

---

## SLO Types In Datadog

### Metric-Based SLO

Uses two metric queries: good events and total events.

```text
Good events:  sum:trace.http.request.hits{env:production,service:orders-service,!http.status_code:5*}
Total events: sum:trace.http.request.hits{env:production,service:orders-service}

SLI = good / total * 100
SLO = 99.9% over 30 days
```

### Monitor-Based SLO

Uses an existing monitor's up/down time.

```text
Underlying monitor: "Orders Service Availability Check"
  -> UP when monitor is OK
  -> DOWN when monitor is in ALERT

SLO = 99.9% uptime over 30 days
```

Metric-based SLOs are more precise. Monitor-based SLOs are simpler to set up.

---

## Creating A Metric-Based SLO

```text
SLOs -> New SLO -> Metric Based

Numerator (good):
  sum:trace.http.request.hits{env:production,service:orders-service,http.status_code:2*}

Denominator (total):
  sum:trace.http.request.hits{env:production,service:orders-service}

Target:
  7-day: 99.9%
  30-day: 99.9%
  90-day: 99.5%

Name: Orders Service Availability
Tags: team:backend service:orders-service env:production
```

---

## Burn Rate Alerts

Burn rate = how fast error budget is burning relative to normal rate.

```text
burn_rate = 1.0 means: consuming error budget at normal rate
            (if SLO is 99.9% over 30 days, budget lasts exactly 30 days)

burn_rate = 14.4 means: consuming budget 14.4x faster than normal
            (budget exhausted in 30 / 14.4 = 2 days)
```

### Multi-Window Multi-Burn-Rate Strategy (Google SRE Workbook)

```text
Alert 1: Fast burn (page immediately)
  Short window: 5 minutes, burn_rate > 14.4
  Long window:  1 hour,   burn_rate > 14.4
  -> Fires when 2% of monthly budget consumed in 1 hour
  -> Wake someone up now

Alert 2: Slow burn (ticket, no page)
  Short window: 30 minutes, burn_rate > 6
  Long window:  6 hours,   burn_rate > 6
  -> Fires when 5% of budget consumed in 6 hours
  -> Create an incident ticket, monitor closely

Alert 3: Crawling burn (low priority)
  Short window: 2 hours, burn_rate > 3
  Long window:  24 hours, burn_rate > 3
  -> Fires when 10% of budget consumed in 24 hours
  -> Non-urgent notification
```

---

## Configuring Burn Rate Alert In Datadog

```text
SLO -> Edit -> Set Alert Conditions -> Burn Rate Alert

Alert type: Burn Rate
Target: 99.9% (30-day)

Alert:
  Burn rate > 14.4 in the last 5 minutes AND last 1 hour
  Notify: @pagerduty-orders-p1 @slack-platform-oncall

Warning:
  Burn rate > 6 in the last 30 minutes AND last 6 hours
  Notify: @slack-platform-team
```

---

## SLO Dashboard Widget

Add the SLO to a dashboard:

```text
Widget type: SLO
Select SLO: Orders Service Availability
Time window: 30d
Display: current SLO % + remaining error budget (time)
```

---

## Error Budget Policy

Spend error budget intentionally:

```text
Error budget remaining > 50%: Deploy freely, run experiments, take risks
Error budget remaining 25-50%: Normal deployment cadence, extra testing
Error budget remaining 10-25%: Careful mode, hotfixes only
Error budget remaining < 10%: Freeze deployments, only emergency patches
Error budget = 0 (SLO breached): Full freeze, incident declared, postmortem required
```

---

## SLO Reporting

Datadog SLO reports can be exported as PDF or sent via email weekly.

```text
Monitors -> SLOs -> Select SLO -> Export -> PDF Report
or
SLO -> Schedule Weekly Email Report -> recipients@example.com
```

---

## Interview Sound Bite

An SLO defines the acceptable reliability target for a service. Error budget is the allowed failure margin: for a 99.9% SLO over 30 days, that is 43 minutes. Burn rate measures how fast error budget is consumed relative to normal. Multi-window multi-burn-rate alerts (fast burn over 5m/1h, slow burn over 30m/6h) catch both sudden outages and gradual degradations. Metric-based SLOs in Datadog use good/total event counts from trace metrics. Error budget policies define deployment gates: when budget is below 10%, freeze non-emergency deployments.
