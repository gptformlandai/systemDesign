# Runbook: Dual-Write Inconsistency

## Symptoms

- source DB changed but search/cache/vector did not
- one write path succeeded and another failed
- retry created duplicates
- downstream projection disagrees with source

## Confirm

1. Identify all write targets.
2. Check transaction boundary.
3. Check retry/idempotency behavior.
4. Check outbox or CDC availability.
5. Check replay and reconciliation tools.

## Mitigate

- stop unsafe write path
- reconcile from source of truth
- replay missing events
- remove duplicate derived records

## Durable Fix

- transactional outbox
- CDC from source DB
- idempotent consumers
- deterministic IDs
- reconciliation job

## Interview Summary

```text
Avoid direct dual writes for critical state. Commit once to the source of truth, then propagate through outbox, CDC, or replayable events.
```