# 18. Scenario: Cache Stampede, Hot Key, Cold Start

## Scenario

Your Redis cache just experienced a mass expiry of popular keys. Requests are flooding the database. How do you prevent this in the future?

---

## Cache Stampede (Thundering Herd)

```text
Situation:
  TTL expires on a high-traffic key.
  1000 concurrent requests all hit cache miss.
  All 1000 queries hit database simultaneously.
  Database struggles under load.
  Redis writes 1000 identical values.
```

---

## Prevention: Mutex Lock On Regeneration

```bash
# Pseudocode.
value = GET product:1001

if value == nil:
  acquired = SET lock:product:1001 worker-uuid NX PX 5000
  if acquired:
    value = query_database(1001)
    SET product:1001 value EX 300
    DEL lock:product:1001
  else:
    sleep(50ms)
    value = GET product:1001   # retry, another worker is regenerating
```

Only one worker regenerates. Others wait and retry. Works for moderate concurrency.

---

## Prevention: Probabilistic Early Refresh

```text
Before TTL expires, randomly decide to refresh early.
Probability increases as TTL decreases.

probability_of_refresh = exp(-delta * beta * log(ttl_remaining))
delta = time spent regenerating last time
beta = tuning constant (1.0 default)
```

Staggered refresh: different workers hit slightly different natural expiry times, spreading the load.

---

## Prevention: Stale-While-Revalidate

```text
Cache serves stale value on miss.
Background goroutine/thread regenerates asynchronously.
Next request gets fresh value.
```

Tradeoff: briefly serves stale data. Acceptable for non-critical freshness requirements.

---

## Hot Key Problem

```text
Situation:
  A single Redis key receives extremely high read traffic.
  Single-threaded Redis becomes a bottleneck.
  Network bandwidth or CPU maxes out on that shard.
```

Mitigations:

| Strategy | Description |
|---|---|
| local in-process cache | hold hot keys in application memory for N seconds |
| read replicas | spread reads across replicas for read-heavy keys |
| key sharding | split into `hot_key:{0-9}`, randomly read from one shard |
| smaller values | break large value into parts, read only needed part |

```bash
# Key sharding example.
shard = random.randint(0, 9)
GET hot:config:shard:{shard}
```

---

## Cold Start Problem

```text
Situation:
  Redis restarts or fails over.
  All keys are evicted.
  All traffic hits database simultaneously.
```

Solutions:

| Strategy | Description |
|---|---|
| warm cache before routing | background job populates top N keys before traffic |
| gradual traffic ramp | shift traffic slowly from original instance |
| fallback with circuit breaker | database handles load, circuit closes as cache warms |
| persistence-based recovery | RDB + AOF allows Redis to reload state after restart |

---

## Combined Failure Story

```text
Production incident flow:
1. Redis OOM -> eviction triggers.
2. 10,000 keys evicted.
3. Traffic spike -> stampede on database.
4. Database query time increases 10x.
5. Rate limiter also backed by Redis -> fails open.
6. All traffic hits database unthrottled.

Recovery:
1. Enable circuit breaker on database calls.
2. Emergency scale database read replicas.
3. Fix Redis maxmemory: allkeys-lru, increase memory.
4. Pre-warm top 1000 keys from database.
5. Gradually re-enable rate limiting.
```

---

## Interview Sound Bite

Cache stampede is caused by synchronized mass expiry. Prevention options are mutex regeneration, probabilistic early refresh, and stale-while-revalidate. Hot key requires local caching, read replicas, or key sharding. Cold start requires pre-warming before routing traffic. Production resilience means these failure modes are anticipated in the design, not discovered in an incident.
