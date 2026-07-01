# Runbook: Bad Database Choice

## Symptoms

- query pattern fights the data model
- every feature needs a workaround
- p99 latency cannot meet SLO
- operational cost grows unexpectedly
- correctness bugs appear under concurrency

## Confirm

1. Name exact access pattern.
2. Identify source of truth.
3. Check whether the store is optimized for that access pattern.
4. Identify consistency and transaction gaps.
5. Compare one simpler alternative and one specialized alternative.

## Mitigate

- add a derived store for specialized reads
- change index/schema/partitioning if enough
- migrate source of truth only with careful dual-read/dual-write plan

## Durable Fix

- architecture decision record
- migration plan
- backfill and validation
- rollback path
- ownership and SLO update

## Interview Summary

```text
Bad database choices are fixed by returning to access patterns, correctness, and operations, not by adding random technology.
```