# 34. Redis Client-Side Caching, RESP3, And Smart Clients

## Goal

Understand the client side of production Redis. Redis incidents are often not caused by the server alone; they come from client pooling, retries, stale topology, failover reconnect behavior, and unsafe local caching.

```text
server speed + client correctness + topology awareness + backpressure = production Redis
```

---

## 1. Why Client-Side Redis Matters

Redis can respond quickly, but the application may still fail because:

- connection pools are too small or too large
- clients retry every request during an outage
- Cluster clients do not refresh slot maps
- Sentinel clients keep writing to the old primary
- local caches return stale values
- timeouts are longer than user-facing SLOs
- pipelines create latency cliffs

Senior Redis design includes both server topology and client behavior.

---

## 2. RESP2 vs RESP3

RESP is the Redis Serialization Protocol.

| Protocol | Why You Care |
|---|---|
| RESP2 | widely supported classic protocol |
| RESP3 | richer types and push messages, useful for server-assisted client features |

Client-side caching uses invalidation push messages. That makes RESP3 support important, though some clients also support redirected invalidations in RESP2-style setups.

Check this before enabling advanced client behavior:

```text
Does our Redis client support the server feature, protocol mode, topology, and reconnection behavior we need?
```

---

## 3. Client-Side Caching Mental Model

Client-side caching means the application keeps a local near-cache of Redis reads. Redis tracks which keys a client read and sends invalidation messages when those keys are modified.

```text
app reads key -> stores local copy -> Redis tracks key/client relationship
another writer changes key -> Redis sends invalidation -> app evicts local copy
next read -> app fetches fresh value
```

This reduces Redis round trips and hot-key pressure, but adds consistency complexity.

---

## 4. CLIENT TRACKING Basics

Enable tracking:

```bash
CLIENT TRACKING ON
GET user:1001
```

When `user:1001` changes, Redis can send an invalidation message to the client.

Common modes:

| Mode | Meaning |
|---|---|
| default | Redis tracks keys read by the client |
| BCAST | broadcast invalidations for key prefixes |
| OPTIN | cache only commands explicitly marked cacheable |
| OPTOUT | cache all reads except those opted out |
| REDIRECT | send invalidations to a different client connection |
| NOLOOP | do not send invalidations caused by the same client |

Pattern with prefix broadcast:

```bash
CLIENT TRACKING ON BCAST PREFIX user: PREFIX product:
```

This is useful when the app wants invalidations for a namespace without Redis tracking every exact key read.

---

## 5. Near-Cache Architecture

```text
request
  -> app local cache
      hit -> return
      miss -> Redis GET/MGET/JSON.GET
          -> store in local cache with small TTL
          -> return

Redis invalidation
  -> app removes local cache entry
```

Use local TTL even with invalidations:

```text
Invalidation is a correctness aid. TTL is the safety net.
```

Recommended local TTLs:

| Data | Local TTL |
|---|---:|
| user/session auth decisions | avoid or very short |
| product catalog | 5-60 seconds |
| feature flags | 1-10 seconds unless event-driven |
| leaderboard top-N | 1-5 seconds |
| pricing/inventory | avoid unless stale reads are acceptable |

---

## 6. Consistency Tradeoffs

| Risk | Why It Happens | Mitigation |
|---|---|---|
| stale local read | missed invalidation or reconnect gap | local TTL, reconnect flush |
| memory growth in app | unbounded near-cache | size limit and TTL |
| invalidation storm | high write churn | avoid near-cache for write-hot keys |
| tracking overhead | too many tracked keys | prefix broadcast or smaller cache scope |
| failover gap | connection lost during primary change | flush local cache after reconnect |

Production rule:

```text
On Redis reconnect, topology change, or failover, clear local near-cache unless the client library proves invalidation continuity.
```

---

## 7. Smart Client Requirements

### Standalone

The client needs:

- timeout control
- connection pool
- retry policy
- TLS/auth support
- circuit breaker or graceful fallback

### Sentinel

The client needs:

- Sentinel discovery
- primary address refresh
- reconnect after failover
- write protection against old primary
- retry with jitter

### Cluster

The client needs:

- slot map
- MOVED redirect handling
- ASK redirect handling
- topology refresh
- hash tag support
- multi-key same-slot validation

If a client library does not understand the topology, the application will eventually fail in production.

---

## 8. Connection Pool Sizing

Wrong pool size can harm Redis:

| Pool Mistake | Failure Mode |
|---|---|
| too small | app threads queue, p99 grows |
| too large | Redis hits maxclients, context overhead grows |
| no timeout | requests hang through outage |
| retry storm | outage becomes self-amplifying |

Sizing heuristic:

```text
pool_size ~= concurrent_app_workers_doing_redis_io
```

Start smaller than you think, measure:

```bash
INFO clients
INFO stats
CLIENT LIST
```

Watch:

- connected_clients
- blocked_clients
- rejected_connections
- instantaneous_ops_per_sec
- command latency
- application pool wait time

---

## 9. Timeout And Retry Policy

Bad retry policy:

```text
retry immediately, forever, from every app instance
```

Better:

```text
short timeout -> bounded retries -> exponential backoff with jitter -> circuit breaker -> fallback
```

Example policy:

| Setting | Typical Starting Point |
|---|---:|
| connect timeout | 100-500 ms inside same region |
| command timeout | less than endpoint SLO budget |
| retries | 1-2 for idempotent reads |
| backoff | exponential with jitter |
| circuit breaker | open on sustained failures |

Never blindly retry non-idempotent writes unless the operation has idempotency keys or compare-and-set semantics.

---

## 10. Pipelining And Backpressure

Pipelining reduces round trips but can hide overload.

Good:

```text
Batch 50-500 independent reads/writes when response ordering is enough.
```

Bad:

```text
Queue unlimited pipeline commands during a Redis slowdown.
```

Rules:

- cap pipeline batch size
- cap pending requests per connection
- use deadlines
- do not pipeline commands that require immediate branch decisions
- prefer Lua/Functions for atomic read-modify-write, not pipeline

---

## 11. Read From Replicas

Replica reads can reduce primary load but introduce staleness.

Use replica reads for:

- analytics dashboards
- eventually consistent profile reads
- cacheable content
- non-critical feeds

Avoid replica reads for:

- auth/session decisions requiring latest state
- payments and inventory correctness
- read-after-write flows

Mitigation:

```text
After a write, read from primary for the same request/session when read-your-write matters.
```

---

## 12. Interview Scenario

> Redis p99 increased after traffic doubled, but Redis CPU is only 35 percent. What do you check?

Strong answer:

```text
I do not assume the Redis server is the bottleneck. I check application Redis pool wait time, command timeout, retry count, connected_clients, rejected_connections, and whether pipelines are building up. Then I check Cluster redirect rates and Sentinel failover/reconnect logs. If one key or slot is hot, I consider local near-cache with client-side tracking or key sharding. If Redis is healthy but app pool wait is high, the fix may be pool sizing, batching, or backpressure rather than adding Redis nodes.
```

---

## 13. Revision Notes

- One-line summary: production Redis requires topology-aware, timeout-aware, cache-aware clients.
- Three keywords: tracking, topology, backpressure.
- One interview trap: scaling Redis nodes when the real bottleneck is app connection pool wait.
- One memory trick: near-cache must be invalidated, bounded, and flushed on reconnect.

---

## 14. Official Source Notes

- Redis client-side caching docs: <https://redis.io/docs/latest/develop/reference/client-side-caching/>
- Redis protocol docs: <https://redis.io/docs/latest/develop/reference/protocol-spec/>
- Redis clients docs: <https://redis.io/docs/latest/develop/clients/>
- Redis Cluster specification: <https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/>
