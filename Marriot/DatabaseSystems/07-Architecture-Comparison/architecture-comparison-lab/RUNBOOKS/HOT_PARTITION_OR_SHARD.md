# Runbook: Hot Partition Or Shard

## Symptoms

- one tenant/user/partition is slow
- cluster average looks healthy
- p99 is bad for a subset of traffic
- current time bucket overloads writes

## Confirm

1. Group latency by tenant/user/partition/shard.
2. Check partition key distribution.
3. Check celebrity or large-tenant traffic.
4. Check time-bucket write concentration.
5. Check shard/index routing.

## Mitigate

- rate limit noisy tenant
- split hot partition
- add bucketing or salting
- move tenant to isolated index/cluster
- cache hot reads if safe

## Durable Fix

- better partition key
- skew monitoring
- per-tenant isolation tier
- write bucketing
- capacity model with hot-key assumptions

## Interview Summary

```text
Scaling fails at the hottest partition, not the average cluster metric.
```