# Runbook: Tombstone Storm

## Symptoms

- read timeouts
- tombstone warnings
- high p99 latency after deletes or TTL expiry
- compaction backlog
- disk IO pressure

## Confirm

1. Identify table and query.
2. Check TTL/delete patterns.
3. Check whether reads scan expired ranges.
4. Inspect compaction strategy.
5. Review repair age and `gc_grace_seconds` assumptions.

## Mitigate

- avoid reading tombstone-heavy ranges
- lower request fan-out or page size
- move traffic away from affected query if possible
- let compaction catch up if safe

## Durable Fix

- redesign TTL tables around time windows
- use TWCS for matching time-series workloads
- reduce range deletes
- avoid unbounded partitions
- tune retention with repair discipline

## Interview Summary

```text
Tombstones are delete markers. They protect correctness across replicas but make reads expensive when the table design scans many deleted or expired cells.
```