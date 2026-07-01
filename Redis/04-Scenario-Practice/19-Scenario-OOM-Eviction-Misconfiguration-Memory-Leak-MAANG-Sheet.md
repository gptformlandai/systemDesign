# 19. Scenario: Redis OOM, Eviction Misconfiguration, Memory Leak

## Scenario

Redis is throwing OOM errors or unexpectedly evicting keys. Walk through diagnosis and resolution.

---

## Detecting OOM

```bash
INFO memory
# used_memory: bytes currently in use
# used_memory_rss: OS-level allocation
# mem_fragmentation_ratio: rss / used_memory
# maxmemory: configured limit
# maxmemory_policy: eviction policy

INFO stats
# evicted_keys: total keys evicted since start
```

Signs of OOM:

- application receives `OOM command not allowed when used memory > 'maxmemory'`
- `evicted_keys` counter is growing
- `used_memory` is close to `maxmemory`

---

## Fragmentation

```text
mem_fragmentation_ratio = used_memory_rss / used_memory

ratio < 1.0: OS memory reuse (can happen after deletion)
ratio 1.0-1.5: normal
ratio > 1.5: high fragmentation, jemalloc holding freed pages
```

Fix fragmentation:

```bash
# Trigger active defragmentation (Redis 4.0+).
CONFIG SET activedefrag yes
CONFIG SET active-defrag-ignore-bytes 100mb
CONFIG SET active-defrag-threshold-lower 10
```

---

## Common OOM Root Causes

| Root Cause | Diagnosis | Fix |
|---|---|---|
| no maxmemory set | `CONFIG GET maxmemory` returns 0 | set maxmemory before production |
| wrong eviction policy | `CONFIG GET maxmemory-policy` is noeviction | change to allkeys-lru or allkeys-lfu |
| keys without TTL | `INFO keyspace` shows high key count | audit key patterns, add TTL |
| large value objects | `MEMORY USAGE key` returns large number | break into smaller keys or compress |
| key explosion (unbounded sets) | SCARD/LLEN returns huge count | cap size with LTRIM, SREM old members |
| forgotten streams | XLEN stream returns huge count | add MAXLEN to XADD, trim with XTRIM |
| sorted sets growing without trim | ZCARD returns large count | add ZREMRANGEBYSCORE cleanup |

---

## Diagnosing Key Patterns

```bash
# Scan keys matching a pattern without blocking.
SCAN 0 MATCH orders:* COUNT 100

# Check memory of a specific key.
MEMORY USAGE orders:stream

# Type of key.
TYPE orders:stream
```

For bulk key analysis, use redis-cli with SCAN pipeline:

```bash
redis-cli --scan --pattern "orders:*" | xargs -n 1 redis-cli MEMORY USAGE
```

---

## Eviction Audit

```bash
# How many keys have been evicted since start.
INFO stats | grep evicted_keys

# How many keys have no TTL.
INFO keyspace
# db0:keys=50000,expires=30000,avg_ttl=86400000
# keys - expires = 20000 keys with no TTL
```

---

## Resolution Checklist

```text
1. Set maxmemory if not set:
   CONFIG SET maxmemory 4gb

2. Set eviction policy:
   CONFIG SET maxmemory-policy allkeys-lru

3. Identify large keys:
   redis-cli --bigkeys

4. Identify keys with no TTL:
   SCAN + TTL check pipeline

5. Fix application to set TTL on all cache keys.

6. Enable active defrag if fragmentation > 1.5.

7. Persist config to redis.conf to survive restart.
   CONFIG REWRITE
```

---

## redis-cli Tools

```bash
# Find top largest keys.
redis-cli --bigkeys

# Get memory usage sample.
redis-cli --memkeys

# Perform latency test.
redis-cli --latency -i 1
```

---

## Interview Sound Bite

Redis OOM starts with diagnosing INFO memory: used_memory vs maxmemory, evicted_keys growth, and fragmentation ratio. The most common causes are missing maxmemory, noeviction policy, and keys without TTL. Fix by setting maxmemory with allkeys-lru, auditing all key patterns for missing TTLs, and using redis-cli --bigkeys to find anomalous objects. Active defrag handles fragmentation after large deletion events.
