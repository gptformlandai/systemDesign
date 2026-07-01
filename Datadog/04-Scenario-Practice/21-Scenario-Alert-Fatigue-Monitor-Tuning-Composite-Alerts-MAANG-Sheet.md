# 21. Scenario: Alert Fatigue, Monitor Tuning, Composite Alerts

## Scenario Setup

```text
Oncall team situation:
  - 45-60 alerts per day
  - ~70% are false positives or "noisy" non-actionable alerts
  - Team has started ignoring alerts ("cry wolf" effect)
  - Missed a real incident last week because it was buried in noise
  - Request: reduce alert volume by 80% without missing real incidents
```

---

## Alert Fatigue Root Causes (Common Patterns)

```text
1. Threshold too low: monitor fires on normal traffic spikes
2. No evaluation window: 1-minute window catches momentary blips
3. Static thresholds: ignore time-of-day traffic patterns
4. No composite logic: fires on a single metric without context
5. Alerts on symptoms: many redundant alerts for the same root cause
6. No deduplication: one incident creates 15 alerts across services
7. Wrong recipients: waking up wrong team for cross-team issues
8. Missing recovery threshold: constantly flapping between OK and ALERT
```

---

## Step 1: Audit Current Monitors

```text
Monitors -> Manage Monitors

Export to spreadsheet and categorize:
  - Last triggered: date
  - True positive rate: did it lead to action?
  - Volume: how many times per week?
  - Evaluation window: 1min/5min/30min?

Categories found:
  Group A: 12 monitors for individual service metrics (all overlap with Group C)
  Group B: 8 monitors firing on CPU > 70% (fires 20x/day during normal usage)
  Group C: 3 monitors that are actually actionable
  Group D: 15 monitors nobody knows who owns
```

### Delete Or Mute Group D First

```text
For each monitor in Group D:
  - Check last triggered: if never triggered in 90 days -> delete
  - If triggered but no action taken -> verify with team -> delete or mute

Use tag: team:unknown to find ownerless monitors.
```

---

## Step 2: Fix Thresholds And Windows

```text
Before (too sensitive):
  Query: avg(last_1m):avg:system.cpu.user{env:production} > 70
  Result: fires 20+ times/day on normal spikes

After (practical):
  Query: avg(last_15m):avg:system.cpu.user{env:production} > 85
  Result: fires only when CPU is sustained above 85% for 15 minutes
```

### Recovery Threshold (Prevents Flapping)

```text
Alert:    > 85%
Warning:  > 70%
Recovery: < 60%   # <- hysteresis: must drop well below threshold to clear
```

Without recovery threshold, a metric bouncing between 84% and 86% causes constant alert/recovery cycles (flapping).

---

## Step 3: Replace Simple Monitors With Composite Monitors

```text
Problem: "Orders service error rate > 2%" fires during every canary deployment
         (canary runs at 1% of traffic, errors on 1% of traffic look like a 2% error rate)

Before (too noisy):
  Monitor A: error rate > 2%  -> fires during every deployment

After (contextual):
  Monitor A: error rate > 2%  
  Monitor B: version is NOT canary (deployment in progress flag from API)
  
  Composite: Monitor A AND NOT Monitor B
  -> Only fires when error rate is high AND it is NOT a canary deployment
```

### Another Composite: Validate Two Signals Agree

```text
False positive scenario:
  - Single host CPU spike causes latency alert
  - But only one host out of 10 is affected
  - Not a real incident

Fix:
  Monitor A: p99 latency > 2000ms  (at service level, across all hosts)
  Monitor B: error rate > 1%       (service-level)
  
  Composite: Monitor A AND Monitor B
  -> Only alert when BOTH latency AND errors are elevated
  -> Eliminates single-metric noise
```

---

## Step 4: Use Anomaly Detection For Variable Metrics

```text
Before (static threshold):
  Query: avg(last_5m):avg:trace.http.request.hits{env:production} < 100
  Problem: fires every night at 3am (low traffic = low hits, but normal)

After (anomaly detection):
  Query: avg(last_1h):anomalies(avg:trace.http.request.hits{env:production}, 'agile', 2)
  Problem solved: learns daily/weekly pattern, alerts only on unexpected deviations
```

---

## Step 5: Deduplicate With Alert Routing

```text
Problem: One database failure creates alerts from:
  - orders-service monitor
  - payments-service monitor
  - inventory-service monitor
  - database connectivity monitor
  (4 PagerDuty incidents for one root cause)

Fix with alert grouping:
  1. Create a single "database tier health" composite monitor.
  2. Route all downstream service monitors to "Warning" only (not page).
  3. Only the database monitor pages PagerDuty.
  4. Downstream monitors send to Slack only.
```

---

## Step 6: Silence During Planned Events

```text
# Create downtime for every deployment.
Monitors -> Manage Downtime -> New Downtime
  Scope: env:production team:backend
  Recurring: Mon-Fri 10:00-11:00 UTC (deployment window)
  Message: "Scheduled deployment window - errors expected"
  
# Create downtime via API (from CI/CD pipeline).
curl -X POST "https://api.datadoghq.com/api/v1/downtime" \
  -H "DD-API-KEY: ${DD_API_KEY}" \
  -H "DD-APPLICATION-KEY: ${DD_APP_KEY}" \
  -d '{
    "scope": ["service:orders-service"],
    "message": "Deploying version 2.5.0",
    "start": 1705323600,
    "end": 1705325400
  }'
```

---

## Step 7: Reduce Alert Volume Results

```text
Before tuning: 52 alerts/day
  - 12 alerts deleted (ownerless)
  - 8 CPU alerts replaced with 1 sustained CPU monitor (15-min window)
  - 3 service monitors replaced with 1 composite monitor
  - 5 monitors silenced during deployment window
  - 4 static threshold monitors converted to anomaly detection

After tuning: 8 alerts/day (85% reduction)
  - All 8 actionable and lead to investigation
  - Zero missed incidents in 30 days after tuning
```

---

## Interview Sound Bite

Alert fatigue comes from: thresholds too low, evaluation windows too short, static thresholds that ignore traffic patterns, and many monitors for the same root cause. Composite monitors eliminate false positives by requiring two conditions simultaneously. Recovery thresholds prevent flapping. Anomaly detection replaces static thresholds for metrics with daily/weekly patterns. Deduplication via alert routing ensures one database failure creates one PagerDuty incident, not five. The goal is every alert requiring an action, not just awareness.
