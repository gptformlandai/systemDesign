# 32. Redis 8 Modern Features And Upgrade Readiness

## Goal

Bring the Redis track up to date beyond Redis 7. A senior engineer should not only know classic Redis commands, but also know how to reason about new server capabilities, client compatibility, upgrade safety, and production rollout.

Redis 8 mastery is less about memorizing every new command and more about this loop:

```text
new feature -> client support -> data model impact -> operational risk -> migration plan -> rollback plan
```

---

## 1. Redis Version Mental Model

Redis has two kinds of knowledge:

| Layer | Changes Often? | Examples |
|---|---:|---|
| Stable core | low | strings, hashes, sets, sorted sets, streams, TTL, RDB/AOF, replication, Cluster |
| Modern surface | medium | Functions, JSON, Search, Vector Sets, Time Series, probabilistic structures |
| Operational behavior | medium | eviction, observability, security defaults, client protocol behavior |
| Managed provider behavior | high | ElastiCache, MemoryDB, Redis Cloud, backup windows, scaling limits |

Classic Redis skill gets you to senior. Redis 8 skill gets you to production architect.

---

## 2. What Redis 8 Adds To The Learning Surface

Redis 8 expands the official Redis learning surface beyond "cache plus core data structures." Current Redis docs emphasize these areas:

| Area | Why It Matters |
|---|---|
| JSON | Store and update document-like values without rewriting whole string blobs |
| Search and query | Secondary indexes, text search, tag filters, numeric filters, aggregations |
| Vector search and Vector Sets | Similarity search, semantic cache, recommendation, RAG retrieval |
| Time Series | Metrics, IoT samples, rollups, retention, downsampling |
| Probabilistic structures | Bloom, Cuckoo, Count-Min Sketch, Top-K, t-digest |
| Functions | Persistent server-side logic as a production replacement for ad-hoc EVAL |
| Client-side caching | Server-assisted near-cache invalidation with RESP3 push messages |

Interview implication:

```text
Old answer: "Redis is cache + pub/sub + streams."
Pro answer: "Redis is an in-memory data platform; I still choose features by durability, memory, query, topology, and client support."
```

---

## 3. Redis 8 Feature Intake Checklist

Redis 8-era documentation may expose commands and data types that are not available in every local Docker image or managed provider. Treat new features through this checklist:

| Feature Family | What To Learn | How To Adopt Safely |
|---|---|---|
| expiration-aware counters such as `INCREX` | atomic counter plus expiry semantics | verify `COMMAND INFO`; compare to Lua/Functions fallback |
| arrays and newer structured values | when ordered nested data beats strings/lists/JSON | prototype locally; check memory and client support |
| stream idempotency improvements | reducing duplicate producer/consumer pain | keep app-level idempotency keys anyway |
| hot-key and latency diagnostics | finding shard/key imbalance faster | validate exporter/dashboard support |
| TLS/auth/security additions | stronger identity and certificate posture | test rotation and client compatibility |
| observability additions | richer server-side evidence | update exporters before relying on new fields |

Rule:

```text
New Redis feature adoption starts with COMMAND INFO and client-library support, not with copy-pasting docs into production.
```

---

## 4. Redis 8 Upgrade Questions

Before upgrading, answer these:

1. Which Redis version do our clients officially support?
2. Are we using modules or Redis Stack commands that changed packaging or compatibility?
3. Do our libraries support RESP3, client-side caching, Functions, JSON, or Search?
4. Are persistence files compatible with downgrade/rollback?
5. Does Cluster resharding or failover behavior change under the new version?
6. Do ACL categories or command names change access requirements?
7. Does monitoring parse new INFO fields safely?
8. Can we run a shadow node or staging restore before production?

Do not treat a Redis upgrade like a stateless app upgrade. Redis has memory, persistence, replication, client protocol, and data encoding concerns.

---

## 5. Compatibility Matrix

| Concern | What To Verify |
|---|---|
| Client library | supported Redis version, RESP2/RESP3 behavior, reconnect behavior |
| Cluster client | MOVED/ASK handling, topology refresh, hash-tag support |
| Sentinel client | discovery, failover reconnect, stale primary handling |
| Scripts | EVAL compatibility, script cache reload, migration to Functions |
| Functions | library naming, FCALL support, ACL category, persistence behavior |
| JSON/Search/Vector | command support, index rebuild behavior, memory overhead |
| Monitoring | INFO parsing, exporter version, dashboards, alert labels |
| Persistence | RDB/AOF restore test, fork latency, disk throughput |
| Security | ACL rules, TLS, auth rotation, blocked dangerous commands |

---

## 6. Modern Counter Pattern: Atomic Increment With Expiry

Classic pattern:

```bash
INCR rate:user:1001:minute:29200001
EXPIRE rate:user:1001:minute:29200001 60
```

Bug:

```text
If the process crashes after INCR but before EXPIRE, the key may live forever.
```

Classic fix:

```bash
# Application checks INCR result.
# If result == 1, set expiry.
INCR rate:user:1001:minute:29200001
EXPIRE rate:user:1001:minute:29200001 60
```

Better fix when available:

```text
Use a Redis version/client command that increments and manages expiry atomically, such as `INCREX` when your server supports it, or wrap the operation in Lua/Functions.
```

Production rule:

```text
Every counter that represents a time window must have expiration coupled to the write.
```

---

## 7. Modern Eviction Thinking

Classic Redis eviction policies:

| Policy | Meaning |
|---|---|
| noeviction | reject writes when memory is full |
| allkeys-lru | evict any key approximating least recently used |
| volatile-lru | evict only TTL-bearing keys approximating least recently used |
| allkeys-lfu | evict any key approximating least frequently used |
| volatile-lfu | evict TTL-bearing keys approximating least frequently used |
| allkeys-random | evict random keys |
| volatile-random | evict random TTL-bearing keys |
| volatile-ttl | evict keys with shortest remaining TTL first |

Modern Redis learning also requires asking:

- Is the key read-hot or write-hot?
- Does the workload prefer recency, frequency, or time-window freshness?
- Are we evicting cache keys only, or mixing cache and source-of-truth keys?
- Does the provider reserve memory for failover, fork, and replication buffers?
- Are we alerting before memory reaches the cliff?

Production answer:

```text
For a pure cache, I start with allkeys-lfu for stable hot sets or allkeys-lru for recency-driven traffic. For mixed Redis, I avoid mixing cache and durable keys in one instance unless eviction boundaries are extremely clear.
```

---

## 8. Stream Idempotency Upgrade

Redis Streams provide at-least-once delivery. That means a consumer may process the same event more than once.

Required pattern:

```text
event_id -> idempotency key -> processing record -> XACK only after durable side effect
```

Example:

```bash
# Producer chooses an application-level event id.
XADD orders:events * event_id evt-5001 order_id 5001 type OrderPaid

# Consumer records processed event id before ACK.
SET processed:orders:evt-5001 1 NX EX 604800
```

If `SET ... NX` fails, the event was already handled. The consumer can safely `XACK`.

Pro details:

- Use application event IDs, not only stream IDs, for business idempotency.
- Keep idempotency TTL longer than the replay window.
- Put side effects behind idempotent writes where possible.
- Poison messages go to a dead-letter stream after retry budget is exhausted.
- Monitor pending entries, idle time, retry count, and oldest unacked message.

---

## 9. Hot Key Detection Runbook

A hot key is a key receiving disproportionate traffic. Redis can be healthy globally while one key overloads one CPU core or one Cluster shard.

Symptoms:

- high CPU on one node
- one shard has much higher ops/sec
- p99 latency spikes while memory is fine
- SLOWLOG may be clean because each command is individually fast
- application endpoints tied to one object are slow

Evidence path:

```bash
INFO commandstats
INFO stats
INFO keyspace
redis-cli --hotkeys
redis-cli --bigkeys
SLOWLOG GET 25
LATENCY LATEST
```

Mitigations:

| Cause | Fix |
|---|---|
| one celebrity/profile/product key | local near-cache with short TTL |
| one leaderboard read by everyone | cache rendered top-N separately |
| one counter written constantly | shard counter and aggregate |
| one stream with one consumer group bottleneck | partition into multiple streams |
| one Cluster slot overloaded | redesign hash tags or split keys |

Hot key sharding:

```text
views:video:9001:shard:0
views:video:9001:shard:1
views:video:9001:shard:2
...
```

Read path sums shards. Write path chooses a shard by request ID, user ID, or random.

---

## 10. Redis 8 Production Rollout Plan

Use this upgrade path:

1. Inventory commands used by application and scripts.
2. Inventory Redis clients and versions.
3. Restore latest production backup into staging Redis 8.
4. Run application integration tests against staging.
5. Run memory and latency benchmark with production-like data.
6. Validate RDB/AOF restore and rollback story.
7. Validate Sentinel/Cluster failover with clients.
8. Canary one low-risk service or shard.
9. Watch error rate, reconnects, latency, evictions, CPU, memory fragmentation.
10. Roll forward gradually; keep rollback plan time-boxed.

Rollback trap:

```text
If new commands or data structures write data that old Redis cannot read, downgrade rollback may not be safe. Prefer restore-from-backup rollback or blue-green migration for major feature adoption.
```

---

## 11. Interview Scenario

> You are upgrading a production Redis 7 cache and stream cluster to Redis 8. What do you check before rollout?

Strong answer:

```text
I start by separating compatibility from performance. First I list client libraries, commands, Lua scripts, Functions, Cluster/Sentinel behavior, and any Redis Stack commands. Then I restore a production backup into staging Redis 8 and run integration tests plus failover tests. I verify monitoring and ACLs because command categories and INFO fields can affect tools. For rollout, I canary one service or shard, watch latency, reconnects, evictions, replication lag, and memory fragmentation, then expand gradually. I do not depend on downgrade rollback if new data types or persistence formats are introduced; I keep backup restore or blue-green as the safe recovery path.
```

---

## 12. Revision Notes

- One-line summary: Redis 8 mastery is upgrade-safe feature adoption, not command-name trivia.
- Three keywords: compatibility, canary, rollback.
- One interview trap: ignoring client library support during a Redis server upgrade.
- One memory trick: server upgrade = data + clients + protocol + persistence + topology.

---

## 13. Official Source Notes

- Redis latest docs: <https://redis.io/docs/latest/>
- Redis what's new: <https://redis.io/docs/latest/develop/whats-new/>
- Redis commands reference: <https://redis.io/docs/latest/commands/>
