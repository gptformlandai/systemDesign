# 05. Redis As Cache: Eviction, Cache Patterns, TTL Strategy

## Goal

Design Redis caching correctly with bounded memory, proper patterns, and stampede prevention.

---

## Eviction Policies

Set `maxmemory` and `maxmemory-policy` before production traffic.

| Policy | Behavior |
|---|---|
| `noeviction` | return error when memory full (default) |
| `allkeys-lru` | evict least recently used across all keys |
| `volatile-lru` | evict LRU only among keys with TTL set |
| `allkeys-lfu` | evict least frequently used across all keys |
| `volatile-lfu` | evict LFU only among keys with TTL set |
| `allkeys-random` | evict random key from all keys |
| `volatile-random` | evict random key from TTL-bearing keys |
| `volatile-ttl` | evict key with nearest expiry first |

Production defaults:

- pure cache with no persistence: `allkeys-lru` or `allkeys-lfu`
- mixed data (some keys must survive): `volatile-lru`
- never use `noeviction` for a cache

---

## Cache Patterns

### Cache-Aside (Lazy Loading)

```text
1. Application checks Redis.
2. Cache miss -> application queries database.
3. Application writes result to Redis with TTL.
4. Future reads hit cache.
```

Pros: only load what is needed, simple.
Cons: cache miss is expensive, cold start, consistency lag.

### Write-Through

```text
1. Application writes to Redis and database in the same operation.
2. Cache is always current.
```

Pros: low read miss rate.
Cons: write latency increases, unused keys consume memory.

### Write-Behind (Write-Back)

```text
1. Application writes to Redis only.
2. Redis (or async worker) flushes to database later.
```

Pros: very low write latency.
Cons: data loss risk if Redis fails before flush, complex failure handling.

### Read-Through

```text
1. Cache layer is responsible for loading from database on miss.
2. Application only talks to cache.
```

Pros: simpler application code.
Cons: cache library or proxy required.

---

## TTL Strategy

| Situation | Strategy |
|---|---|
| user session | 30 min idle TTL, refresh on activity |
| product catalog | 5-15 min, accept slight staleness |
| high-traffic query | 30-60 seconds, prevent stampede |
| idempotency key | match request processing window, then expire |
| rate limit window | match window size exactly |
| lock key | operation-length TTL with safety margin |

Key rule: all cache keys must have a TTL. Keys without TTL grow without bound.

---

## Cache Miss Rate And Hit Ratio

```bash
INFO stats
# keyspace_hits / (keyspace_hits + keyspace_misses)
```

A good cache hit ratio for hot-path caches is usually above 90-95%.

Low hit ratio causes:

- too-short TTLs
- key naming inconsistency
- cold start
- eviction pressure

---

## Cache Warming

After restart or failover, cold cache causes a stampede. Options:

- pre-populate from a dump or batch job
- probabilistic early refresh: randomly refresh before TTL expires
- background refresh with stale-while-revalidate
- use mutex/lock to prevent simultaneous regeneration

---

## Interview Sound Bite

Production caching starts with the right eviction policy and TTL on every key. Cache-aside is the most common pattern. Write-through adds consistency at write latency cost. Write-behind is fastest but requires durable async flush. Stampede prevention is a separate layer: probabilistic refresh or distributed lock on regeneration.
