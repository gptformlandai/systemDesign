# Runbook: Cache Staleness

## Symptoms

- user sees old profile, price, permission, or inventory
- cache hit rate is high but correctness is poor
- updates do not appear until TTL expires

## Confirm

1. Check source-of-truth value.
2. Check cache key and TTL.
3. Check invalidation event.
4. Check write path and read path consistency.
5. Check whether stale value is safe for the workflow.

## Mitigate

- invalidate affected keys
- lower TTL temporarily
- bypass cache for critical workflow
- refresh cache from source

## Durable Fix

- cache-aside/write-through policy
- event-based invalidation
- versioned keys
- stale-read budget
- avoid caching sensitive correctness state when unsafe

## Interview Summary

```text
Caches improve latency only when staleness is acceptable or explicitly controlled.
```