# Distributed Cache - End-to-End System Design

> Goal: design a distributed cache that reduces backend load, improves latency, handles hot keys and cache stampedes, and balances consistency, availability, memory cost, and operational complexity.

---

## How To Use This File

- Use this when the interview problem says distributed cache, Redis/Memcached cache layer, shared cache, cache-aside, write-through cache, or backend offload.
- Focus on how cache fits between services and source of truth.
- Keep the core trade-off visible: performance improves, but consistency and invalidation become harder.
- In interviews, discuss hit ratio, TTL, hot keys, stampedes, sharding, replication, and fallback behavior.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the product goal, core requirements, and the user-facing workflow.
2. Second pass: trace the main read/write path through the high-level design.
3. Third pass: study the data model, scaling choices, failures, and trade-offs.
4. Fourth pass: practice the LLD, machine-coding layer, and final interview playbook without looking.

What a starter should master first:

- The one-line purpose of the system.
- The core entities and APIs.
- The main request flow.
- The storage choice and why it fits.
- The biggest bottleneck.
- The failure that most affects users.
- The trade-off you would defend in an interview.

Gold-level self-check:

- You can draw the architecture from memory in 5 minutes.
- You can explain the happy path and one failure path clearly.
- You can justify consistency, latency, availability, and cost choices.
- You can name what you would simplify for an MVP and what you would add at scale.
- You can answer follow-ups about spikes, retries, idempotency, observability, and data growth.

---

# Master Checklist For This Problem

| Layer | Interview signal | Distributed cache focus |
|---|---|---|
| Problem understanding | Can define cache role | shared cache, source of truth, read-through/cache-aside/write-through |
| HLD | Can design cache cluster | clients, cache nodes, shards, replicas, source DB/service |
| LLD | Can model cache operations | `CacheClient`, `CacheKey`, `TTL`, `CachePolicy`, `Loader`, `InvalidationEvent` |
| Machine coding | Can simulate cache-aside | get, miss load, set TTL, invalidate, single-flight |
| Traffic spikes | Can protect origin | hot key replication, request coalescing, stale serve, TTL jitter |
| Billion users | Can scale globally | consistent hashing, replication, multi-region, observability |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Store frequently accessed data in a shared cache cluster.
- Support `get`, `set`, `delete`, and TTL.
- Serve cache hits faster than database/source service.
- On cache miss, load from source of truth and populate cache.
- Support eviction when memory is full.
- Support sharding across cache nodes.
- Support replication or failover.
- Support invalidation when source data changes.
- Emit cache hit/miss, latency, memory, and eviction metrics.

Optional requirements:

- Write-through or write-behind caching.
- Read-through loader abstraction.
- Multi-region cache.
- Stronger consistency for selected keys.
- Hot-key detection and mitigation.
- Negative caching for missing values.
- Compression for large values.
- Local L1 cache plus distributed L2 cache.

Out of scope unless asked:

- Full Redis implementation internals.
- Persistent database replacement.
- Full consensus system.
- Full CDN design.
- Complete stream processing layer.

## 1.2 Non-Functional Requirements

Latency:

- Cache hit should be low-latency, usually sub-millisecond to a few milliseconds depending network.
- Cache miss path can be slower but should be protected from stampedes.
- Client should have tight timeouts.

Availability:

- Cache failure should not always take down the application.
- Source of truth should remain authoritative.
- Cache cluster should support failover and graceful degradation.

Consistency:

- Cached data can be stale unless policy prevents it.
- TTL and invalidation define staleness bounds.
- Writes must decide whether cache update is synchronous or asynchronous.

Cost and operations:

- Memory is expensive.
- Hit ratio must justify cache cost.
- Evictions, hot keys, and uneven shard load must be observable.

## 1.3 Constraints

- Cache is not the source of truth for most systems.
- Network calls add latency compared with local cache.
- Cache keys need stable naming and versioning.
- Large values reduce memory efficiency.
- Hot keys can overload one shard.
- Invalidation events can be missed or delayed.
- Multi-region cache consistency is hard.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Peak reads | 5M requests/sec |
| Cache hit ratio target | 80% to 99% depending workload |
| Cache nodes | 100s to 1000s |
| Value size | bytes to hundreds of KBs |
| TTL | seconds to hours/days |
| Latency target | p99 under 5 to 10 ms for cache operations |
| Source load reduction | 5x to 100x |

Back-of-the-envelope:

- If service has 1M reads/sec and hit ratio is 90%, database sees 100K reads/sec.
- Improving hit ratio from 90% to 99% reduces database traffic by 10x.
- A hot key with 100K QPS can overload a single shard if not replicated or locally cached.
- Storing 100M values at 1 KB each needs about 100 GB plus metadata/replication overhead.

## 1.5 Clarifying Questions To Ask

- What data is being cached?
- Is cache the source of truth or only a performance layer?
- What staleness is acceptable?
- What TTL should entries have?
- What happens if cache is down?
- Is cache shared across services or owned by one service?
- Are writes frequent or mostly reads?
- Is multi-region required?
- How large are values and how many keys are expected?

Strong interview framing:

> I will design distributed cache as a shared, sharded, in-memory layer in front of the source of truth. The application uses cache-aside or read-through on misses, TTL and invalidation control freshness, replication improves availability, and hot-key/stampede controls protect both cache and database.

---

# 2. High-Level Design

## 2.1 Architecture

Cache-aside read flow:

```text
Client
  -> Application Service
  -> Distributed Cache
       hit  -> return value
       miss -> Source DB/Service
            -> populate cache with TTL
            -> return value
```

Recommended architecture:

```text
                    +-------------------+
                    | Config / Topology |
                    +---------+---------+
                              |
                              v
Client -> App Service -> Cache Client Library
                              |
                              v
                    +-------------------+
                    | Hash / Router     |
                    +---------+---------+
                              |
           +------------------+------------------+
           v                  v                  v
      +---------+        +---------+        +---------+
      | Shard A |        | Shard B |        | Shard C |
      | primary |        | primary |        | primary |
      +----+----+        +----+----+        +----+----+
           |                  |                  |
           v                  v                  v
      +---------+        +---------+        +---------+
      | replica |        | replica |        | replica |
      +---------+        +---------+        +---------+

Cache miss -> Source DB / Source Service
Writes     -> DB + invalidation/update cache
Metrics    -> Observability platform
```

## 2.2 APIs

### Cache Operations

```text
get(key) -> value or miss
set(key, value, ttl) -> ok
delete(key) -> ok
compareAndSet(key, expectedVersion, value, ttl) -> ok/fail
```

### Cache-Aside Service API

```java
interface CacheClient<K, V> {
    Optional<V> get(K key);
    void set(K key, V value, Duration ttl);
    void delete(K key);
}

interface CacheLoader<K, V> {
    V load(K key);
}
```

### Example Key Format

```text
user-profile:v3:userId:12345
product:v7:country:US:id:sku123
rate-card:v2:tenant:t1:plan:gold
```

Key design rules:

- Include domain and version.
- Include tenant/locale/region if response varies.
- Avoid unbounded raw user input in keys.
- Keep keys short enough for memory efficiency.

## 2.3 Core Components

Think of Distributed Cache as a shared memory layer plus correctness policy.

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| App service | cache usage and fallback | cache cluster internals | request QPS |
| Cache client | routing, timeout, retry, serialization | source-of-truth correctness | client fanout |
| Router/hash ring | key to shard mapping | value semantics | rebalancing |
| Cache shard | in-memory key/value storage | durable truth | memory and hot keys |
| Replica | failover/read scaling | write policy alone | replication lag |
| Source DB/service | authoritative data | cache eviction | backend load |
| Invalidation pipeline | delete/update cache on writes | request serving | event delivery |
| Metrics pipeline | hit/miss/evictions/latency | business data | cardinality |

### Caching Patterns

| Pattern | How it works | Pros | Cons | Use case |
|---|---|---|---|---|
| cache-aside | app loads DB on miss and sets cache | simple, common | app handles misses | most services |
| read-through | cache layer loads on miss | hides miss logic | cache needs loader | platform cache |
| write-through | write DB/cache synchronously | fresher cache | write latency | read-heavy stable data |
| write-behind | write cache first, DB async | fast writes | data loss/ordering risk | special cases only |
| refresh-ahead | refresh before TTL expires | avoids misses | extra work | hot keys |

Default recommendation:

- Use cache-aside for most interview designs.
- Add single-flight and TTL jitter.
- Use explicit invalidation for writes where stale data matters.
- Keep DB/source as truth.

### Sharding

Shard choices:

| Strategy | Pros | Cons |
|---|---|---|
| modulo hashing | simple | many keys move when node count changes |
| consistent hashing | fewer remapped keys | ring management complexity |
| rendezvous hashing | simple client-side remap | compute cost across nodes |
| proxy/router | central routing | router can become bottleneck |

Recommended:

- Use consistent hashing or managed cluster slots.
- Replicate shards for availability.
- Keep topology updates versioned in clients.

### Freshness And Invalidation

| Method | How it works | Trade-off |
|---|---|---|
| TTL | expire after time | stale until expiry |
| delete-on-write | remove cache after DB write | next read reloads |
| update-on-write | write DB and cache | race conditions possible |
| versioned keys | new writes use new key version | old keys expire later |
| pub/sub invalidation | notify app/local caches | missed messages possible |

### Failure Policy

When cache is down:

| Option | Behavior | Risk |
|---|---|---|
| bypass cache | go to DB/source | DB overload |
| serve stale local copy | preserve availability | stale data |
| fail request | protect source | user-visible errors |
| shed traffic | reject low-priority requests | degraded UX |

One-stop interview answer:

> I would use cache-aside with a sharded Redis/Memcached-like cache cluster. The app checks cache first, loads source on miss, stores value with TTL, and invalidates or updates cache on writes. Sharding and replication scale capacity, while TTL jitter, single-flight, hot-key replication, and stale serving protect against spikes and failures.

---

# 3. Low-Level Design

LLD goal:

> Model cache operations around keys, policies, loaders, invalidation, and fallback.

Starter map:

| LLD question | Distributed cache answer |
|---|---|
| Main key object | `CacheKey` |
| Value wrapper | `CacheEntry` |
| Policy | `CachePolicy` with TTL/stale rules |
| Client | `CacheClient` |
| Miss loader | `CacheLoader` |
| Routing | `ShardRouter` |
| Invalidation | `InvalidationEvent` |
| Fallback | `CacheFallbackPolicy` |

Beginner-friendly design order:

1. Define key naming and serialization.
2. Define `get`, `set`, `delete` client.
3. Implement cache-aside read method.
4. Add TTL and negative caching.
5. Add single-flight for miss coalescing.
6. Add invalidation on writes.
7. Add shard router.
8. Add metrics and fallback behavior.

Interview sentence:

> For LLD, I will keep the cache client generic and put cache-aside behavior in a reusable service method: check cache, coalesce misses, load source, set with TTL, and return value with clear fallback when cache is unavailable.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `CacheKey` | stable key identity | includes version and vary dimensions |
| `CacheEntry` | value plus metadata | TTL/created time explicit |
| `CachePolicy` | TTL, stale, negative cache settings | domain-specific |
| `CacheClient` | get/set/delete | timeouts are bounded |
| `CacheLoader` | load from source | idempotent where possible |
| `SingleFlightGroup` | coalesce misses per key | one load per key at a time |
| `ShardRouter` | key to cache node | stable during topology version |
| `InvalidationEvent` | cache delete/update signal | idempotent |

## 3.2 Class Sketch

```java
interface CacheClient<K, V> {
    Optional<V> get(K key);
    void set(K key, V value, Duration ttl);
    void delete(K key);
}

interface CacheLoader<K, V> {
    V load(K key);
}

final class CachePolicy {
    private final Duration ttl;
    private final Duration staleTtl;
    private final boolean negativeCachingEnabled;
}
```

## 3.3 Sequence Diagram

Cache-aside read:

```text
Client -> AppService: get user profile
AppService -> CacheClient: get(profile key)
CacheClient --> AppService: miss
AppService -> SingleFlight: acquire key load
AppService -> UserDB: load profile
UserDB --> AppService: profile
AppService -> CacheClient: set(profile key, profile, ttl)
AppService --> Client: profile
```

Write with invalidation:

```text
Client -> AppService: update profile
AppService -> UserDB: write profile
UserDB --> AppService: ok
AppService -> CacheClient: delete(profile key)
AppService -> EventBus: publish invalidation
AppService --> Client: ok
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Cache-Aside | app controls miss load |
| Proxy | cache client hides cluster details |
| Strategy | TTL, routing, fallback, eviction policies |
| Adapter | Redis/Memcached clients behind interface |
| Decorator | metrics/tracing around cache calls |
| Single Flight | coalesce concurrent miss loads |
| Circuit Breaker | stop waiting on unhealthy cache |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| cache miss | load source and populate cache |
| cache down | bypass, stale serve, or fail by policy |
| DB down and cache hit | serve if policy allows |
| DB down and cache miss | error or stale fallback |
| stale entry after write | invalidate/update cache after source write |
| hot key | local cache, replica reads, request coalescing |
| large value | avoid caching or compress/chunk |
| negative result | cache short TTL to protect source |
| topology change | use consistent hashing and gradual rebalance |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
distributedcache/
  CacheClient.java
  CachePolicy.java
  CacheEntry.java
  CacheAsideService.java
  SingleFlightGroup.java
  ShardRouter.java
  CacheStats.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from dataclasses import dataclass
from time import time
from typing import Callable, TypeVar

T = TypeVar("T")


@dataclass
class CacheEntry:
    value: object
    expires_at: float


class InMemoryDistributedCache:
    def __init__(self) -> None:
        self.entries: dict[str, CacheEntry] = {}

    def get(self, key: str):
        entry = self.entries.get(key)
        if entry is None:
            return None
        if entry.expires_at <= time():
            self.entries.pop(key, None)
            return None
        return entry.value

    def set(self, key: str, value: object, ttl_seconds: int) -> None:
        self.entries[key] = CacheEntry(value=value, expires_at=time() + ttl_seconds)

    def delete(self, key: str) -> None:
        self.entries.pop(key, None)


def cache_aside_get(cache: InMemoryDistributedCache, key: str, ttl: int, loader: Callable[[], T]) -> T:
    cached = cache.get(key)
    if cached is not None:
        return cached

    value = loader()
    cache.set(key, value, ttl)
    return value


cache = InMemoryDistributedCache()
value = cache_aside_get(cache, "user:v1:123", 60, lambda: {"id": 123, "name": "Ava"})
print(value)
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| key/value store | hash table |
| TTL expiration | min-heap, timing wheel, lazy expiration |
| eviction | LRU/LFU/size-aware policy |
| sharding | consistent hash ring or slot map |
| replication | primary-replica metadata |
| miss coalescing | key to in-flight promise/future |
| invalidation | event stream or pub/sub channel |
| metrics | counters and histograms |

## 4.4 Concurrency

- Cache client should use connection pooling.
- Requests should have bounded timeouts.
- Miss loading should be coalesced per key.
- Writes and invalidations should be ordered where needed.
- Shard failover should avoid thundering herd reconnects.
- `get` and `set` races can produce stale data unless versioned.

## 4.5 Testing Checklist

- Cache hit returns cached value.
- Cache miss calls loader once.
- TTL expiry causes reload.
- Delete invalidates value.
- Negative cache protects source for missing key.
- Cache failure falls back according to policy.
- Concurrent misses coalesce.
- Shard router maps key consistently.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Cache Stampede

Problem:

- Hot key expires and many requests load DB simultaneously.

Handling:

- Single-flight per key.
- TTL jitter.
- Soft TTL and background refresh.
- Serve stale while refreshing if safe.
- Refresh-ahead for hot keys.

## 5.2 Hot Key Overload

Problem:

- One key maps to one shard and receives massive QPS.

Handling:

- Local L1 cache in app process.
- Replicate hot keys across shards.
- Client-side request coalescing.
- Split key if value can be partitioned.
- Track hot keys and auto-promote them.

## 5.3 Cache Cluster Outage

Handling:

- Short client timeouts.
- Circuit breaker.
- Bypass cache carefully.
- Shed low-priority traffic to protect DB.
- Serve stale local values where safe.
- Gradual reconnect/backoff after recovery.

## 5.4 Eviction Storm

Problem:

- Memory pressure evicts many hot keys, causing source load spike.

Handling:

- Increase memory or reduce value size.
- Tune eviction policy.
- Add admission policy.
- Use compression for large values.
- Alert on eviction rate and hit-ratio drop.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning

| Data | Partition key | Notes |
|---|---|---|
| cached object | hash of cache key | common sharding method |
| tenant data | tenant + key | supports tenant isolation |
| regional data | region + key | improves locality |
| hot keys | replicated key ID | avoid single-shard overload |
| invalidation events | domain/entity ID | order by entity where needed |

## 6.2 Replication

Options:

| Model | Pros | Cons |
|---|---|---|
| primary only | simple | node failure causes misses |
| primary-replica | better availability | replication lag |
| multi-primary | regional writes | conflict/staleness complexity |
| client-side dual write | simple concept | partial failure/race risk |

Recommendation:

- Use primary-replica within a region for availability.
- Use regional caches for low latency.
- Avoid strong global cache consistency unless absolutely required.

## 6.3 Multi-Region Strategy

Patterns:

- independent regional caches,
- regional cache warmed from local traffic,
- async invalidation across regions,
- global cache only for rare shared metadata,
- local L1 plus regional L2.

Trade-off:

> Multi-region caches are usually eventually consistent. If data must be strongly consistent, go to the source of truth or use version checks.

## 6.4 Observability

Track:

- cache hit ratio,
- byte hit ratio,
- p50/p95/p99 cache latency,
- timeout/error rate,
- evictions,
- memory usage,
- shard QPS,
- hot keys,
- miss load to DB,
- stampede coalescing count,
- invalidation lag,
- stale serve count,
- topology version by client.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify what data is cached and staleness tolerance.
2. State cache is not source of truth unless explicitly designed so.
3. Choose cache-aside as default pattern.
4. Draw app -> cache cluster -> DB/source flow.
5. Explain TTL, key design, invalidation, and eviction.
6. Add sharding and replication.
7. Add stampede, hot-key, and outage handling.
8. Close with metrics and memory/cost trade-offs.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| pattern | cache-aside | write-through | cache-aside default, write-through for stricter freshness |
| consistency | TTL only | explicit invalidation | combine based on staleness tolerance |
| scope | local cache | distributed cache | local for fastest, distributed for shared capacity |
| sharding | modulo | consistent hashing/slots | consistent hashing/slots |
| outage | bypass cache | fail/serve stale | depends on DB capacity and correctness risk |

## 7.3 Common Mistakes

- Treating cache as source of truth accidentally.
- Forgetting TTL and invalidation.
- Ignoring cache stampede.
- Letting cache outage overload the database.
- Using unstable or overly long keys.
- Caching huge values without memory analysis.
- Ignoring hot keys.
- Not measuring hit ratio.

## 7.4 Strong Closing

> A distributed cache is a shared in-memory performance layer, not a magic database replacement. I would use cache-aside with stable versioned keys, TTL, explicit invalidation where needed, sharding for capacity, replication for availability, and stampede/hot-key protections so the cache improves latency without making the source of truth fragile.

---

# 8. Fast Recall Rules

- Distributed cache is shared L2 cache.
- Cache is usually not source of truth.
- Cache-aside is the default interview pattern.
- TTL bounds staleness.
- Invalidation handles writes and deletes.
- Stable versioned keys prevent many bugs.
- Consistent hashing/slots distribute keys.
- Hot keys can overload one shard.
- Stampede protection is mandatory at scale.
- Always track hit ratio, latency, evictions, hot keys, and DB miss load.
