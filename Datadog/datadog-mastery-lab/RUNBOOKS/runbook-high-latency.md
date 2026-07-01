# Runbook: High Latency Incident Response

## Trigger

Monitor: `[service] P99 Latency > [threshold]ms` for 5 minutes.

## Severity

P1 if SLO burn rate > 14.4x. P2 if latency is elevated but SLO burn rate < 6x.

---

## Step 1: Scope (T+0 to T+3 min)

Open APM → [service] → Service Page.

```text
Check:
  Request rate: [up/down/normal]?
  Error rate: [normal or elevated]?
  P50 latency: [value]ms  <-- if P50 is high, ALL users affected (structural problem)
  P99 latency: [value]ms  <-- if only P99 is high, tail latency (outlier/timeout)
  
  Started at: check the metric graph for when the latency spike began.
```

---

## Step 2: Filter To Slow Traces (T+3 to T+8 min)

APM → Traces → Filter:

```text
service:[service-name]
env:production
@duration:>2000000000   (> 2 seconds, in nanoseconds)
Sort by duration descending.
```

Open the slowest trace. Open the flame graph.

---

## Step 3: Identify Bottleneck Span

```text
Look for:
  - Widest horizontal bar (most time)
  - Red bars (errors)
  - DB spans (check for N+1 pattern: many identical small spans)
  - HTTP client spans (check for timeout pattern: all around the same duration)
  - Missing spans (orphan: downstream service not traced = not instrumented)
```

---

## Step 4: Correlate With Infrastructure

Based on bottleneck span type:

```text
DB span slow:
  -> Infrastructure -> Host for that DB host
  -> Check: CPU, disk I/O, active connections, slow query log

HTTP client timeout:
  -> APM -> downstream service
  -> Check: downstream service latency, error rate, pod restarts

Queue consumer slow:
  -> Check queue depth metric for that topic/queue
  -> Check consumer lag
```

---

## Step 5: Check Recent Changes

APM → Service → Deployments tab.

```text
Any version deployed in the last 2 hours?
  If yes: compare latency before and after deployment.
  If latency spiked after deployment -> probable regression.
```

---

## Decision Tree

```text
P50 high AND new deployment -> rollback deployment
P99 high only AND DB slow   -> check for lock/migration/query plan regression
P99 high only AND HTTP timeout -> check downstream service health + circuit breaker
P50 high AND no deployment  -> check infrastructure (CPU, disk, network saturation)
```

---

## Step 6: Mitigation Options

```text
Rollback deployment:
  kubectl rollout undo deployment/[service] -n production

Kill runaway DB query:
  SELECT pg_cancel_backend(pid) FROM pg_stat_activity
  WHERE state = 'active' AND query_start < now() - interval '5 minutes';

Increase timeout/retry:
  (temporary config change to reduce error propagation while root cause is fixed)

Scale up:
  kubectl scale deployment/[service] --replicas=[N] -n production
  (only if the issue is resource saturation, not a code bug)
```

---

## Step 7: Verify Recovery

```text
Monitor auto-resolves.
P99 latency returns to within 20% of baseline.
SLO burn rate drops to < 1.0x.
```

---

## Post-Incident

If SLO breached: file postmortem within 24 hours.

If latency spike recurs: add a more targeted alert on the specific span type that was the bottleneck.
