# Runbook: `DEADLINE_EXCEEDED`

## Meaning

The caller's time budget expired.

## Checks

1. Confirm caller deadline.
2. Compare client and server trace spans.
3. Verify whether the server handler started.
4. Check proxy route timeout.
5. Inspect dependency latency.
6. Review retry attempts and amplification.
7. Check payload size changes.
8. Check rollout timeline.

## Mitigations

- roll back latency regression
- align route timeout and caller deadline
- disable unsafe retries
- reduce fan-out or payload size
- scale or fix slow dependency

## Prevention

Deadline policy, latency SLOs, trace coverage, load testing, and retry review.