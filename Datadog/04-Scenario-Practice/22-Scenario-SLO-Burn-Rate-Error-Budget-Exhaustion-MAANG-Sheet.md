# 22. Scenario: SLO Burn Rate Alert, Error Budget Exhaustion, Incident Response

## Scenario Setup

```text
PagerDuty incident at 15:10 UTC:
  Monitor: "Orders Service SLO Fast Burn Rate Alert"
  Burn rate: 28.6x (threshold: 14.4x)
  SLO: Orders Service 99.9% Availability (30-day rolling)
  Error budget remaining before alert: 43 minutes total, 32 minutes remaining
  At this burn rate: budget will be exhausted in 32 / 28.6 = ~67 minutes
```

---

## Understanding The Alert

```text
30-day error budget for 99.9% SLO:
  43,200 minutes * 0.001 = 43.2 minutes of allowed errors

Before alert: 11 minutes already consumed (25% used)
  32 minutes remaining.

Burn rate 28.6x means:
  In the last 5-minute window, errors are occurring 28.6x faster than the allowed rate.
  
  Allowed error rate: 1 error per 1000 requests (0.1%)
  Current error rate: 28.6 errors per 1000 requests (2.86%)
  
  If this continues: full error budget exhausted in 67 minutes -> SLO breached.
```

---

## Step 1: Declare Incident And Assemble

```text
Immediately (within 5 minutes of alert):
  1. Acknowledge PagerDuty incident.
  2. Create incident channel: #incident-orders-2024-01-15
  3. Post initial summary: burn rate, service, time started.
  4. Tag stakeholders: @backend-lead @platform-oncall.
  5. Set severity: P1 (SLO at risk of breach within 1 hour).
```

---

## Step 2: Open SLO Dashboard

```text
Monitors -> SLOs -> Orders Service Availability

SLO detail shows:
  Current window (30d): 99.87%  (target: 99.9%)
  Already breaching!
  
  Error budget consumed: 18 minutes of 43 (42% consumed)
  Budget remaining: 25 minutes
  
  Wait — the alert said 32 minutes remaining. It took 8 minutes to respond.
  Update incident channel with revised remaining budget.
```

---

## Step 3: Find The Error Spike

```text
APM -> orders-service

Error rate graph (last 30 minutes):
  15:00: 0.1% (normal)
  15:05: 0.3% (slight increase)
  15:08: 2.1% (sharp spike)
  15:10: 2.9% (alert fires)
  15:15: 3.1% (still climbing)

Timeline: error rate spiked at 15:08.
```

### What Changed At 15:08?

```text
APM -> Deployments -> orders-service

Deployments:
  15:07:45: version 2.8.0 deployed (3-pod rolling deploy started)
  15:08:20: first pod with 2.8.0 became ready (matches start of spike)

New version 2.8.0 was deployed 2 minutes before the error spike.
```

---

## Step 4: Identify The Error

```text
APM -> Trace Explorer

Filter:
  service:orders-service version:2.8.0 status:error env:production
  time: 15:08 to now

Top errors:
  "NullPointerException at DiscountService.applyDiscount(DiscountService.java:87)"
  Count: 1,842 in last 10 minutes

Click one trace -> flame graph:
  POST /checkout                             ERROR
    ├── CartService.getCart                  OK
    ├── DiscountService.applyDiscount        ERROR  <- NullPointerException
    └── (order never created)
```

---

## Step 5: Rollback Immediately

```text
Do not wait for full RCA — budget is burning.

kubectl rollout undo deployment/orders-service -n production
kubectl rollout status deployment/orders-service -n production

Rollback from 2.8.0 to 2.7.5.
All 3 pods replaced: 15:19 UTC.
```

---

## Step 6: Verify Recovery

```text
APM -> orders-service error rate:
  15:19: 3.1% (last of 2.8.0 pods)
  15:20: 1.2% (2 of 3 pods rolled back)
  15:21: 0.2% (all pods on 2.7.5)
  15:22: 0.1% (normal)

SLO Dashboard:
  Burn rate: 0.8x (below 1.0 -> error budget is recovering)
  
Post-incident SLO state:
  Error budget consumed: 27 of 43 minutes (63%)
  SLO still at 99.88% (just under 99.9% target)
  30-day SLO: BREACHED (need to file for exception or accept miss)
```

---

## Step 7: Incident Timeline And Communication

```text
15:10  Alert fires, incident created #incident-orders-2024-01-15
15:12  Team assembled, initial assessment started
15:15  Deployment correlation found (version 2.8.0 at 15:07)
15:17  Rollback initiated
15:21  Error rate normalizing
15:22  P1 resolved, monitor auto-resolved
15:25  SLO breach confirmed (27 min of 43 min budget consumed)

Customer impact:
  Duration: 14 minutes of elevated errors
  Affected requests: ~18,500 checkout attempts failed
  User-facing: HTTP 500 on checkout for users who had discount codes

Post-incident action items:
  1. RCA document within 24 hours
  2. Fix NullPointerException in 2.8.1 with null check on DiscountService
  3. Add integration test for checkout with discount code
  4. Add canary deployment policy for orders-service (1% before 100%)
  5. File SLO miss report with SRE team
```

---

## Error Budget Incident Response Playbook

```text
Burn rate > 14.4x (fast burn):
  -> Page immediately
  -> Check deployment history
  -> Rollback if new deploy correlates
  -> Error rate should recover within 10 minutes

Burn rate 6-14x (slow burn):
  -> Create non-urgent incident ticket
  -> Investigate root cause
  -> Fix within 4 hours

Burn rate 3-6x (creeping burn):
  -> Slack notification only
  -> Monitor closely
  -> Schedule fix within 24 hours

Budget exhausted (SLO breached):
  -> Freeze deployments
  -> File incident report
  -> Postmortem required
  -> 30-day recovery window starts from now
```

---

## Interview Sound Bite

SLO burn rate alerts catch incidents before the error budget is exhausted, not after. A 14.4x burn rate means the budget will be depleted in 30/14.4 = 2 days, so it pages immediately. The first action after acknowledgment is finding what changed (deployment history). If a deployment correlates, roll back immediately without waiting for full RCA — budget is burning. After recovery, confirm the SLO state and whether a breach occurred. A breached SLO triggers a postmortem, deployment freeze, and 30-day recovery observation.
