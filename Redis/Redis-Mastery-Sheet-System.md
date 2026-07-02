# Redis Mastery Sheet System

Redis mastery means knowing which data structure, command, expiry, persistence model, and topology fit each production problem, and being able to explain every tradeoff.

```text
right data structure + TTL design + persistence model + replication topology + security + observability = production Redis
```

---

## 1. What Redis Is

Redis is a single-threaded, in-memory data structure server with optional persistence.

It is strong for problems that need microsecond latency, typed data operations, atomic multi-step commands, and flexible expiry.

---

## 2. Core Mental Model

```text
client command -> event loop -> data structure operation -> optional persistence -> optional replication -> response
```

Redis is single-threaded in its command execution. This means:

- commands are atomic by default
- long commands (KEYS, SMEMBERS on large sets, SORT) block all clients
- latency comes from command time, not concurrency
- memory is the primary resource constraint

---

## 3. Redis Is Not Only A Cache

Redis serves many roles:

| Role | What Redis Provides |
|---|---|
| Cache | fast key-value reads with TTL and eviction |
| Session store | fast user-session reads with TTL |
| Pub/Sub broker | fire-and-forget message fan-out |
| Stream processor | append-only log with consumer groups |
| Distributed lock | atomic SET NX with expiry |
| Rate limiter | counters and sorted sets with sliding windows |
| Leaderboard | sorted set ranking |
| Job queue | list-based or stream-based task queues |
| Geospatial index | radius searches and proximity queries |
| Probabilistic store | HyperLogLog for cardinality, Bloom for membership |
| Full-text search | RediSearch module |
| Time series | RedisTimeSeries module |
| JSON document store | nested documents and partial path updates |
| Vector search | semantic similarity, recommendations, semantic cache |
| Server-side functions | persistent, versioned Lua function libraries |
| Near-cache coordinator | client-side caching invalidation via tracking |

---

## 4. Data Structure Map

| Structure | Best For |
|---|---|
| string | cache values, counters, tokens, flags |
| hash | object/entity storage by field |
| list | ordered queues, activity feeds, event logs |
| set | unique memberships, tags, union/intersection |
| sorted set | leaderboards, time-ordered events, rate limiting |
| bitmap | compact boolean flags, user activity |
| HyperLogLog | approximate unique count |
| geospatial | proximity search, location data |
| stream | append-only event log with consumer groups |
| JSON | nested documents and indexed product/profile data |
| vector | embeddings and similarity search when corpus fits in memory |
| probabilistic | approximate membership, frequency, top-k, percentiles |

---

## 5. Beginner To Pro Learning Loop

```text
use case -> data structure -> command -> TTL/expiry -> memory cost -> persistence -> topology -> failure mode -> fix -> interview answer
```

For every Redis topic, ask:

1. What is the access pattern?
2. Which data structure fits?
3. What TTL and expiry policy keeps memory bounded?
4. What persistence durability is acceptable?
5. What topology provides the required HA/scale?
6. What can fail in memory, persistence, replication, or cluster?
7. What evidence in INFO, SLOWLOG, or metrics proves the state?
8. What design or operational control prevents recurrence?

---

## 6. Senior Interview Framing

Strong Redis answers connect:

- data structure choice to access pattern
- TTL and eviction to memory budget
- persistence model to durability requirement
- Sentinel vs Cluster to HA vs scale need
- pub/sub vs Streams to delivery guarantee
- Redlock tradeoffs to fencing token alternatives
- hot-key and stampede mitigations to production scale
- INFO/SLOWLOG/keyspace to incident evidence
- modern Redis features to memory, index, client, and provider constraints

Weak answers only describe `SET`/`GET` or mention Redis as a cache without tradeoffs.

---

## 7. Fast Recall

```text
Redis is an in-memory data structure server.
Single-threaded command execution makes commands atomic but slow commands block.
Persistence is optional: RDB for snapshots, AOF for durability, hybrid for both.
Sentinel provides HA; Cluster provides horizontal scale.
Beyond cache: pub/sub, streams, locks, queues, leaderboards, geospatial, HLL, Bloom, JSON, Search, vectors, Functions.
```

---

## 8. Start Here

1. Open [Redis-Mastery-Track-Index.md](Redis-Mastery-Track-Index.md).
2. Complete `01-Foundations` in order.
3. Practice `02-Intermediate-Practical` with the lab.
4. Study `03-Senior-Production` before system design interviews.
5. Study Sheets 32-36 for modern Redis and managed-cloud production depth.
6. Use scenarios and runbooks until incident debugging is automatic.
