# Runbook: Stale Derived Store

## Symptoms

- search result shows old price
- vector retrieval returns deleted document
- cache has stale permission
- warehouse report is behind expected freshness

## Confirm

1. Check source-of-truth value.
2. Check CDC/outbox/event emission.
3. Check consumer lag and errors.
4. Check dead-letter queue.
5. Check derived-store update time.
6. Check delete and permission event handling.

## Mitigate

- replay affected events
- invalidate cache
- patch/reindex affected derived records
- temporarily route reads to source if feasible

## Durable Fix

- lag alerts
- idempotent consumers
- rebuild job
- freshness SLO
- delete/permission regression tests

## Interview Summary

```text
Derived stores are allowed to be stale only within an explicit freshness SLO and must be replayable or rebuildable.
```