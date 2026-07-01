# SLO Math Cheatsheet

## Error Budget Formula

```text
error_budget_minutes = window_minutes * (1 - slo_target)

window_minutes (30 days) = 30 * 24 * 60 = 43,200 minutes

Examples:
  99%    -> 43200 * 0.010 = 432.0 minutes (7h 12m)
  99.5%  -> 43200 * 0.005 = 216.0 minutes (3h 36m)
  99.9%  -> 43200 * 0.001 = 43.2 minutes
  99.95% -> 43200 * 0.0005 = 21.6 minutes
  99.99% -> 43200 * 0.0001 = 4.32 minutes
```

## Burn Rate Formula

```text
burn_rate = (actual_error_rate / allowed_error_rate)

allowed_error_rate = (1 - slo_target)

Example (SLO 99.9%, actual error rate 2%):
  allowed = 0.001  (0.1%)
  actual  = 0.020  (2%)
  burn_rate = 0.020 / 0.001 = 20x
```

## Time To Budget Exhaustion

```text
time_to_exhaustion = remaining_budget_minutes / (burn_rate * allowed_rate_per_minute)

Simplified: remaining_budget / burn_rate * (window_days / 1)

Example (99.9%, 30 days, burn rate 14.4x):
  time_exhausted = 30_days / 14.4 = 2.08 days = ~50 hours
```

## Multi-Window Burn Rate Thresholds

| Alert Level | Burn Rate | Short Window | Long Window | Budget Consumed In |
|---|---|---|---|---|
| P1 Page | 14.4x | 5 min | 1 hour | 2 days |
| P2 Ticket | 6x | 30 min | 6 hours | 5 days |
| P3 Low | 3x | 2 hours | 24 hours | 10 days |

## Quick Reference

```text
1 nine   = 99%     = 7h 12m/month   (432 min)
2 nines  = 99%     (same, 99 = 1 nine)
3 nines  = 99.9%   = 43 min/month
4 nines  = 99.99%  = 4 min/month
5 nines  = 99.999% = 26 sec/month
```
