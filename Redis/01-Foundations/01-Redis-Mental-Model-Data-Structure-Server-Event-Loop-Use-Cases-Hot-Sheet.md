# 01. Redis Mental Model: Data Structure Server, Event Loop, Use Cases

## Core Idea

Redis is a single-threaded, in-memory data structure server with optional persistence and a TCP-based client protocol.

```text
client sends command -> Redis event loop processes it -> data structure is mutated atomically -> optional replication/AOF append -> response returned
```

The key insight: Redis is not a simple key-value store. It is a data structure server where keys map to typed objects, and those objects have their own command semantics.

---

## Single-Thread Model

Redis uses a single thread for command execution.

This means:

- every command is atomic by default
- no locks needed for individual command operations
- slow commands block all other clients
- latency is proportional to command time, not concurrent load
- KEYS, SORT, SMEMBERS on large sets, and HGETALL on huge hashes are dangerous in production

The event loop uses I/O multiplexing to handle many client connections efficiently. Since Redis 6, threaded I/O for network reads/writes is optional, but command execution remains serialized.

---

## Memory Is The Primary Constraint

Everything lives in RAM. Disk (RDB/AOF) is only for durability or recovery.

Production implications:

- use TTLs to bound memory
- choose eviction policies deliberately
- key naming conventions affect memory fragmentation
- large values are expensive to serialize and replicate
- monitor `used_memory` and `maxmemory` in `INFO memory`

---

## Redis Is Not Only A Cache

| Use Case | Redis Capability |
|---|---|
| Cache | strings/hashes with TTL and eviction |
| Session store | hashed user data with TTL |
| Pub/Sub | fire-and-forget fan-out |
| Stream processing | append-only log with consumer groups |
| Distributed lock | atomic SET NX with expiry |
| Rate limiter | INCR counters or sorted-set sliding window |
| Leaderboard | sorted set with ZADD/ZRANK |
| Job queue | LPUSH/BRPOP or streams |
| Geospatial | GEOADD/GEORADIUS/GEODIST |
| Cardinality count | HyperLogLog approximate unique count |
| Membership test | Bloom filter via RedisBloom module |
| Full-text search | RediSearch module |
| Time series | RedisTimeSeries module |

---

## What Redis Is Good At

- microsecond read/write latency
- atomic multi-step operations (MULTI/EXEC, Lua scripts)
- typed data structures with O(1) or O(log N) commands
- flexible TTL per key
- fan-out and streaming

---

## What Redis Is Not Good At

- datasets larger than RAM
- complex joins across multiple data structures
- strong consistency across cluster nodes (eventual/partial)
- long-running transactions
- full SQL semantics

---

## Redis vs Alternatives

| Scenario | Consider |
|---|---|
| cache + HA | Redis Sentinel |
| cache + scale | Redis Cluster |
| stream durability | Kafka if Redis durability is not enough |
| complex queries | dedicated database; use Redis as cache layer |
| ML-scale similarity | dedicated vector DB; RediSearch for smaller workloads |

---

## Interview Sound Bite

Redis is an in-memory data structure server. Its single-threaded model makes individual commands atomic, but it also means slow commands block. Production Redis means choosing the right data structure, bounding memory with TTL and eviction, selecting the right persistence model, and deploying with Sentinel or Cluster for HA and scale.
