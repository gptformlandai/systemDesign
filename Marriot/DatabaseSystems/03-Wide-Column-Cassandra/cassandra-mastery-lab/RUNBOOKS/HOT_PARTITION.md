# Runbook: Hot Partition

## Symptoms

- p99 latency spike for one tenant, room, user, or device.
- Uneven load across nodes.
- Adding nodes does not materially improve the hot access path.

## Confirm

1. Identify exact endpoint and CQL query.
2. Identify partition key values for slow requests.
3. Check whether one key dominates traffic.
4. Check table read/write latency and timeouts.
5. Review recent traffic or product events.

## Mitigate

- cache hot reads
- throttle noisy callers
- reduce page size or query frequency
- route noncritical reads to stale/cache path

## Durable Fix

- add time bucket or shard suffix
- split latest table from history table
- redesign fan-out/fan-in model
- add product-level limits for extreme keys

## Interview Summary

```text
A hot partition is not fixed by generic cluster scaling because one key still maps to a limited replica set. The durable fix is partition-key redesign or workload shaping.
```