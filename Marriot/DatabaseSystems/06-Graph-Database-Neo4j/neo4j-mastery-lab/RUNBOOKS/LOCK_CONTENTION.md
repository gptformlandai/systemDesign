# Runbook: Lock Contention

## Symptoms

- write latency spikes
- transaction retries increase
- deadlocks or lock wait errors appear
- imports slow down after adding relationship-heavy writes
- one high-degree node is frequently updated

## Confirm

1. Identify write endpoint, Cypher, and parameters.
2. Check whether many transactions update the same node or relationship pattern.
3. Check transaction retry/deadlock metrics and logs.
4. Check batch size and transaction duration.
5. Check whether `MERGE` touches high-degree nodes or broad patterns.

## Mitigate

- reduce batch size
- retry transient failures with backoff
- serialize writes for one hot entity if correctness requires it
- avoid long-running mixed read/write transactions
- temporarily pause noncritical imports

## Durable Fix

- create constraints so `MERGE` finds anchors directly
- make writes idempotent and smaller
- split hot relationships or add bucket nodes
- redesign high-contention update paths
- separate online writes from bulk import windows

## Interview Summary

```text
Neo4j lock contention is usually a hot-write or broad-MERGE problem. Fix anchors, transaction size, and graph shape before blaming the cluster.
```