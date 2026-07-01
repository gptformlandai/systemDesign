# Runbook: Hot Shard

## Symptoms

- one shard or node has much higher CPU, search latency, indexing load, or disk growth
- adding nodes does not improve the overloaded shard
- one tenant, routing key, index, or time bucket dominates traffic
- p99 latency is bad while cluster-level averages look acceptable

## Confirm

1. Identify the slow endpoint and index alias.
2. Inspect shard distribution with `_cat/shards`.
3. Compare node stats for CPU, heap, indexing, search, and disk.
4. Check routing keys, tenant IDs, and time windows.
5. Check whether one aggregation/query pattern targets the same shard repeatedly.

## Mitigate

- throttle or isolate the noisy tenant/query
- move read-heavy analytics away from user-facing search
- roll over to a better shard layout for future writes
- temporarily scale replicas for read-heavy load if the hot path allows it

## Durable Fix

- redesign routing strategy
- split large tenants into dedicated indices
- use ILM/rollover to bound time-index size
- validate shard count with capacity and recovery-time targets
- add per-tenant/query SLO dashboards

## Interview Summary

```text
A hot shard is a data or traffic skew problem. More nodes help only when the shard and routing design let work spread.
```