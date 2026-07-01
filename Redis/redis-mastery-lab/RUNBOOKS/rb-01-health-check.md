# Runbook 01: Redis Health Check

## When To Use

Routine health check before deployments, after restarts, or as first step in any incident.

## Steps

1. Verify Redis responds.

```bash
redis-cli -h HOST -p PORT PING
# Expected: PONG in < 5ms
```

2. Check latency.

```bash
redis-cli -h HOST -p PORT --latency -i 1
# Expected: avg < 1ms
# Abort after 5 samples with Ctrl+C
```

3. Capture INFO snapshot.

```bash
redis-cli -h HOST -p PORT INFO all > /tmp/redis-health-$(date +%s).txt
```

4. Check memory.

```bash
grep -E "used_memory_human|maxmemory_human|maxmemory_policy|mem_fragmentation" /tmp/redis-health-*.txt
```

5. Check eviction.

```bash
grep evicted_keys /tmp/redis-health-*.txt
# Expected: evicted_keys:0
```

6. Check replication.

```bash
grep -E "role:|connected_slaves:|master_link_status:" /tmp/redis-health-*.txt
```

7. Check slow log length.

```bash
redis-cli -h HOST -p PORT SLOWLOG LEN
# Alert if > 100 since last reset
```

## Pass Criteria

- PING returns PONG
- avg latency < 1ms
- used_memory < 80% of maxmemory
- evicted_keys == 0
- master_link_status:up on replicas
