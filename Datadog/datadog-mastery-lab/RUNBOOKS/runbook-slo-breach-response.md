# Runbook: SLO Breach Response

## Trigger

SLO burn rate > 14.4x (fast burn) OR error budget exhausted (SLO breach confirmed).

---

## Immediate Response (T+0 to T+5 min)

```text
1. Acknowledge PagerDuty incident.

2. Create incident channel: #incident-[service]-[YYYY-MM-DD]

3. Post initial message:
   "P1 INCIDENT - [service] SLO at risk
    Burn rate: [X]x  Budget remaining: [N] minutes
    Investigating now. Do not deploy until further notice."

4. Announce deployment freeze:
   "FREEZE: No deployments to [service] in production until incident resolved."
```

---

## Step 1: Check SLO Dashboard (T+1 min)

```text
Monitors -> SLOs -> [Service] SLO

Note:
  Current achievement: [X]%  (target: [Y]%)
  Budget consumed: [N] of [M] minutes
  Budget remaining: [N] minutes
  At current burn rate, exhausted in: [N] hours
```

---

## Step 2: Identify Error Spike Source (T+2 to T+8 min)

```text
APM -> [service] -> Error rate graph

Find: when did the error rate spike start?
Check: did a deployment happen at that time? (APM Deployments view)

If deployment correlates -> rollback is the fastest mitigation.
If no deployment -> investigate downstream failures.
```

---

## Step 3: Rollback If Deployment Correlates (T+5 to T+10 min)

```bash
# Check current deployment version.
kubectl get deployment [service] -n production -o jsonpath='{.spec.template.spec.containers[0].image}'

# Rollback.
kubectl rollout undo deployment/[service] -n production

# Monitor rollback progress.
kubectl rollout status deployment/[service] -n production
```

---

## Step 4: Verify Recovery (T+10 to T+15 min)

```text
APM -> [service] -> error rate:
  Should return to < 0.5% within 5 minutes of rollback completing.

SLO burn rate:
  Should drop to < 1.0x within 5 minutes.

If not recovering after 10 minutes:
  -> Rollback may not have worked (check if rollback image is different)
  -> Investigate downstream dependency as root cause
```

---

## Step 5: Determine SLO Breach Severity

```text
After error rate normalizes, check final SLO state:

Monitors -> SLOs -> [service]

SLO still at/above target (e.g., 99.9%):
  -> "Near miss" - file incident report, no formal breach
  -> Error budget > 50% remaining: normal deployment resume

SLO below target (e.g., 99.87% vs 99.9% target):
  -> SLO breach confirmed
  -> Mandatory postmortem within 48 hours
  -> Deployment freeze continues until SLO recovers to target
  -> 30-day recovery window starts now
```

---

## Post-Incident Actions

```text
Immediate (within 2 hours):
  [ ] Update incident channel with resolution summary
  [ ] Confirm deployment freeze lifted (if resolved)
  [ ] Update SLO dashboard with incident annotation

Within 24 hours:
  [ ] Draft initial RCA document
  [ ] File SLO miss report with SRE team (if breach)
  [ ] Identify fix for root cause

Within 48 hours:
  [ ] Postmortem meeting (if SLO breached)
  [ ] Fix deployed to staging
  [ ] Monitor/alert improvements documented

Within 1 week:
  [ ] Fix deployed to production
  [ ] Post-deployment verification
  [ ] Monitor tuning applied
  [ ] Postmortem published
```

---

## Communication Template

```text
[T+0] Incident opened: "Orders service SLO fast burn. Investigating."
[T+10] Update: "Root cause identified: version 2.8.0 NullPointerException on checkout with promo codes. Rolling back."
[T+15] Update: "Rollback complete. Error rate normalizing."
[T+20] Resolution: "Incident resolved. Error rate < 0.5%. SLO breach confirmed (99.87% vs 99.9% target). Postmortem scheduled for tomorrow."
```
