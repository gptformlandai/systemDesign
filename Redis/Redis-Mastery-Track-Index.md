# Redis Mastery Track - Beginner To Pro Index

Redis is a multi-model, in-memory data platform. It is not only a cache. The goal of this track is to teach Redis as a cache, a pub/sub broker, a stream processor, a session store, a rate limiter, a distributed lock server, a leaderboard engine, a geospatial index, a job queue, and a production data infrastructure platform.

```text
use case -> right data structure -> command semantics -> expiry/memory -> persistence -> replication/HA -> cluster topology -> production answer
```

Use this track if:

- You want beginner-to-pro Redis confidence across caching, streaming, pub/sub, locks, queues, and data structures.
- You want to understand Redis memory, eviction, persistence, replication, Sentinel, and Cluster deeply.
- You want MAANG-level answers connecting Redis to system design, real-time pipelines, HA, and production incidents.
- You want hands-on labs, runbooks, portfolio projects, and scenario drills instead of reading-only notes.

---

## 1. Learning Style: Beginner To Pro Loop

Every topic should be learned with this loop:

```text
use case -> data structure -> command + TTL design -> memory model -> failure mode -> fix -> production scenario -> interview explanation
```

Redis mastery is not memorizing `SET`/`GET`. It is understanding which data structure, expiry, persistence, topology, and eviction policy fits the system requirement, and why alternatives fail.

---

## 2. Track Structure

| Group | Folder | Purpose |
|---:|---|---|
| 1 | `01-Foundations` | Redis mental model, CLI, strings/numbers/TTLs, collection types |
| 2 | `02-Intermediate-Practical` | cache patterns, pub/sub, streams, transactions, scripting, persistence |
| 3 | `03-Senior-Production` | replication, Sentinel, Cluster, security, observability, advanced patterns |
| 4 | `04-Scenario-Practice` | rate limiter, stampede, memory pressure, streams, locks, HA incidents |
| 5 | `05-Special-Interview-Rounds` | Q&A, command/data-structure decision map, anti-patterns |
| 6 | `06-Practice-Upgrade` | active recall, drills, mini projects, production readiness checklist |
| Lab | `redis-mastery-lab` | CLI examples, scripts, labs, projects, cheatsheets, interview prep, runbooks |

---

## 3. Foundations Path

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Foundations/01-Redis-Mental-Model-Data-Structure-Server-Event-Loop-Use-Cases-Hot-Sheet.md](01-Foundations/01-Redis-Mental-Model-Data-Structure-Server-Event-Loop-Use-Cases-Hot-Sheet.md) | what Redis is, event loop, single-thread model, use-case map |
| 2 | [01-Foundations/02-Redis-Setup-CLI-Config-AUTH-Ping-Gold-Sheet.md](01-Foundations/02-Redis-Setup-CLI-Config-AUTH-Ping-Gold-Sheet.md) | redis-server, redis-cli, CONFIG, AUTH, basic debugging |
| 3 | [01-Foundations/03-Redis-Strings-Numbers-TTLs-Expiry-Core-Commands-Gold-Sheet.md](01-Foundations/03-Redis-Strings-Numbers-TTLs-Expiry-Core-Commands-Gold-Sheet.md) | SET/GET/INCR/EXPIRE/TTL/PERSIST/SETNX patterns |
| 4 | [01-Foundations/04-Redis-Collection-Types-Lists-Hashes-Sets-SortedSets-Gold-Sheet.md](01-Foundations/04-Redis-Collection-Types-Lists-Hashes-Sets-SortedSets-Gold-Sheet.md) | LPUSH/RPUSH/HSET/SADD/ZADD and when to use each |

Foundation target:

- You can explain what Redis is and what it is not.
- You can choose the right data structure for a use case.
- You can design TTL and expiry patterns confidently.

---

## 4. Intermediate Practical Path

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Practical/05-Redis-As-Cache-Eviction-Policies-Cache-Patterns-TTL-Strategy-Gold-Sheet.md](02-Intermediate-Practical/05-Redis-As-Cache-Eviction-Policies-Cache-Patterns-TTL-Strategy-Gold-Sheet.md) | cache-aside, write-through, write-behind, eviction, cache warming |
| 6 | [02-Intermediate-Practical/06-Redis-PubSub-Channels-Patterns-Fanout-Limitations-Gold-Sheet.md](02-Intermediate-Practical/06-Redis-PubSub-Channels-Patterns-Fanout-Limitations-Gold-Sheet.md) | PUBLISH/SUBSCRIBE, pattern subscriptions, fan-out, vs Streams |
| 7 | [02-Intermediate-Practical/07-Redis-Streams-XADD-XREAD-ConsumerGroups-ACK-Gold-Sheet.md](02-Intermediate-Practical/07-Redis-Streams-XADD-XREAD-ConsumerGroups-ACK-Gold-Sheet.md) | append-only log, consumer groups, ACK, at-least-once, trimming |
| 8 | [02-Intermediate-Practical/08-Redis-Transactions-MULTI-EXEC-WATCH-Pipelining-Gold-Sheet.md](02-Intermediate-Practical/08-Redis-Transactions-MULTI-EXEC-WATCH-Pipelining-Gold-Sheet.md) | MULTI/EXEC/DISCARD, WATCH optimistic locking, pipeline batching |
| 9 | [02-Intermediate-Practical/09-Redis-Lua-Scripting-EVAL-EVALSHA-Atomicity-Gold-Sheet.md](02-Intermediate-Practical/09-Redis-Lua-Scripting-EVAL-EVALSHA-Atomicity-Gold-Sheet.md) | atomic multi-command scripts, EVALSHA, SCRIPT LOAD, blockers |
| 10 | [02-Intermediate-Practical/10-Redis-Persistence-RDB-AOF-Hybrid-BGSAVE-Gold-Sheet.md](02-Intermediate-Practical/10-Redis-Persistence-RDB-AOF-Hybrid-BGSAVE-Gold-Sheet.md) | RDB snapshots, AOF append log, AOF-RDB hybrid, fsync, restore |

Practical target:

- You can design Redis for caching, pub/sub, stream processing, and transactional patterns.
- You can choose the right persistence model.

---

## 5. Senior Production Path

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-Production/11-Redis-Replication-Primary-Replica-Lag-WAIT-MAANG-Sheet.md](03-Senior-Production/11-Redis-Replication-Primary-Replica-Lag-WAIT-MAANG-Sheet.md) | async replication, REPLICAOF, replication lag, WAIT, partial resync |
| 12 | [03-Senior-Production/12-Redis-Sentinel-HA-Quorum-Failover-Client-Reconnect-MAANG-Sheet.md](03-Senior-Production/12-Redis-Sentinel-HA-Quorum-Failover-Client-Reconnect-MAANG-Sheet.md) | Sentinel topology, quorum, failover, client discovery, split-brain |
| 13 | [03-Senior-Production/13-Redis-Cluster-Hash-Slots-Sharding-Resharding-Cross-Slot-MAANG-Sheet.md](03-Senior-Production/13-Redis-Cluster-Hash-Slots-Sharding-Resharding-Cross-Slot-MAANG-Sheet.md) | 16384 hash slots, MOVED/ASK, resharding, cross-slot limits |
| 14 | [03-Senior-Production/14-Redis-Security-ACL-TLS-AUTH-Command-Disable-MAANG-Sheet.md](03-Senior-Production/14-Redis-Security-ACL-TLS-AUTH-Command-Disable-MAANG-Sheet.md) | ACL, TLS, requirepass, RENAME/DISABLE dangerous commands |
| 15 | [03-Senior-Production/15-Redis-Observability-INFO-SLOWLOG-MONITOR-Keyspace-Latency-Gold-Sheet.md](03-Senior-Production/15-Redis-Observability-INFO-SLOWLOG-MONITOR-Keyspace-Latency-Gold-Sheet.md) | INFO sections, SLOWLOG, MONITOR, keyspace events, latency histogram |
| 16 | [03-Senior-Production/16-Redis-Advanced-Patterns-RateLimiting-Locks-Leaderboard-Geo-Bloom-MAANG-Sheet.md](03-Senior-Production/16-Redis-Advanced-Patterns-RateLimiting-Locks-Leaderboard-Geo-Bloom-MAANG-Sheet.md) | rate limiter, Redlock, sessions, leaderboard, geo, HyperLogLog, Bloom |
| 31 | [03-Senior-Production/31-Redis-Pro-Gap-Fill-Bitmaps-Encoding-Internals-Redis7-MAANG-Sheet.md](03-Senior-Production/31-Redis-Pro-Gap-Fill-Bitmaps-Encoding-Internals-Redis7-MAANG-Sheet.md) | bitmaps, OBJECT ENCODING internals, Redis 7 commands, diagnostic CLI flags |

Senior target:

- You can design Redis for high availability, horizontal scale, security, and observability.
- You can explain advanced Redis patterns and their tradeoffs.

---

## 6. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/17-Scenario-Rate-Limiter-Design-Redis-MAANG-Sheet.md](04-Scenario-Practice/17-Scenario-Rate-Limiter-Design-Redis-MAANG-Sheet.md) | rate limiter design with sorted sets and Lua |
| 18 | [04-Scenario-Practice/18-Scenario-Cache-Stampede-Hot-Key-Cold-Start-MAANG-Sheet.md](04-Scenario-Practice/18-Scenario-Cache-Stampede-Hot-Key-Cold-Start-MAANG-Sheet.md) | stampede, probabilistic early refresh, hot key sharding |
| 19 | [04-Scenario-Practice/19-Scenario-OOM-Eviction-Misconfiguration-Memory-Leak-MAANG-Sheet.md](04-Scenario-Practice/19-Scenario-OOM-Eviction-Misconfiguration-Memory-Leak-MAANG-Sheet.md) | OOM, eviction policy, key expiry drift, memory fragmentation |
| 20 | [04-Scenario-Practice/20-Scenario-Streams-Event-Bus-Design-MAANG-Sheet.md](04-Scenario-Practice/20-Scenario-Streams-Event-Bus-Design-MAANG-Sheet.md) | event bus design, consumer groups, DLQ, trim, ordering |
| 21 | [04-Scenario-Practice/21-Scenario-Redlock-Failures-Distributed-Lock-Safety-MAANG-Sheet.md](04-Scenario-Practice/21-Scenario-Redlock-Failures-Distributed-Lock-Safety-MAANG-Sheet.md) | Redlock, clock drift, fencing tokens, failure modes |
| 22 | [04-Scenario-Practice/22-Scenario-Sentinel-Failover-Client-Reconnect-MAANG-Sheet.md](04-Scenario-Practice/22-Scenario-Sentinel-Failover-Client-Reconnect-MAANG-Sheet.md) | Sentinel failover, quorum, stale replica, client reconnect |
| 23 | [04-Scenario-Practice/23-Scenario-Production-Debugging-Latency-Slow-Commands-Connections-MAANG-Sheet.md](04-Scenario-Practice/23-Scenario-Production-Debugging-Latency-Slow-Commands-Connections-MAANG-Sheet.md) | on-call Redis incident response framework |

Scenario target:

- You can diagnose and resolve real Redis incidents with a repeatable evidence path.

---

## 7. Special Interview Rounds

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/24-Redis-Interview-QnA-Beginner-to-MAANG-Sheet.md](05-Special-Interview-Rounds/24-Redis-Interview-QnA-Beginner-to-MAANG-Sheet.md) | Redis Q&A from beginner to MAANG |
| 25 | [05-Special-Interview-Rounds/25-Redis-Commands-DataStructure-Decision-Cheatsheet.md](05-Special-Interview-Rounds/25-Redis-Commands-DataStructure-Decision-Cheatsheet.md) | command map, data structure decision guide |
| 26 | [05-Special-Interview-Rounds/26-Redis-Anti-Patterns-Common-Bugs-Debugging-Traps-Sheet.md](05-Special-Interview-Rounds/26-Redis-Anti-Patterns-Common-Bugs-Debugging-Traps-Sheet.md) | unsafe Redis patterns and debugging traps |

---

## 8. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 27 | [06-Practice-Upgrade/27-Redis-Active-Recall-Spaced-Repetition-Drills.md](06-Practice-Upgrade/27-Redis-Active-Recall-Spaced-Repetition-Drills.md) | recall prompts across beginner to pro topics |
| 28 | [06-Practice-Upgrade/28-Redis-Practical-Drills-Hands-On-Command-Practice.md](06-Practice-Upgrade/28-Redis-Practical-Drills-Hands-On-Command-Practice.md) | command and design drills |
| 29 | [06-Practice-Upgrade/29-Redis-Mini-Projects-Portfolio-Mastery.md](06-Practice-Upgrade/29-Redis-Mini-Projects-Portfolio-Mastery.md) | portfolio-ready Redis projects |
| 30 | [06-Practice-Upgrade/30-Redis-Production-Readiness-Checklist.md](06-Practice-Upgrade/30-Redis-Production-Readiness-Checklist.md) | senior readiness checklist and scoring rubric |

---

## 9. Redis Mastery Lab

- [redis-mastery-lab/README.md](redis-mastery-lab/README.md)
- [redis-mastery-lab/LEARNING_PATH.md](redis-mastery-lab/LEARNING_PATH.md)

Lab covers: CLI drills, cache-aside example, rate limiter example, stream consumer example, scripts, labs, projects, cheatsheets, interview prep, and runbooks.

---

## 10. Interview Answer Pattern

For Redis design and debugging answers, use this shape:

```text
1. Use case: what problem is Redis solving here?
2. Data structure: which type fits the access pattern, size, and TTL?
3. Command: which commands implement the behavior atomically?
4. Memory: what expiry, eviction, and size bounds protect the instance?
5. Persistence: what durability tradeoff is acceptable?
6. Topology: standalone, Sentinel, or Cluster?
7. Evidence: INFO, SLOWLOG, keyspace, latency histogram, or metric?
8. Prevention: what key design, TTL policy, or topology change prevents recurrence?
```

---

## 11. Recommended Study Orders

### 2-Week Practical Path

1. Foundations 1-4.
2. Practical 5-10.
3. Scenarios 17-23.
4. Cheat sheet, exercises, and interview Q&A.

### 4-Week Pro Path

1. Week 1: mental model, data structures, TTL, persistence.
2. Week 2: cache patterns, pub/sub, streams, transactions, scripting.
3. Week 3: replication, Sentinel, Cluster, security, observability, advanced patterns.
4. Week 4: scenarios, runbooks, projects, interview practice.

### Production Operator Path

1. Learn INFO, SLOWLOG, MONITOR, keyspace events.
2. Practice memory pressure, eviction, hot-key, and failover scenarios.
3. Add cluster, security, and persistence controls.
4. Write RCA notes from each incident scenario.

---

## 12. Readiness Gate

You are Redis interview-ready when you can do all of this without notes:

- Explain Redis as a data structure server, not just a cache.
- Choose strings, hashes, lists, sets, sorted sets, streams, bitmaps, HyperLogLog, and geospatial for the right problems.
- Design TTL, expiry, and eviction policies for bounded memory.
- Explain RDB, AOF, and hybrid persistence tradeoffs.
- Explain pub/sub vs Streams tradeoffs and consumer group semantics.
- Design cache patterns including cache-aside, write-through, and stampede prevention.
- Explain replication, Sentinel HA, and Cluster sharding with hash slots.
- Implement rate limiting, distributed locks, leaderboards, and geospatial patterns.
- Implement bitmap-based presence/activity tracking with SETBIT/BITCOUNT/BITOP.
- Explain memory encoding internals (listpack, hashtable, intset) and OBJECT ENCODING.
- Name Redis 7 additions: LMPOP, ZMPOP, GETEX, GETDEL, SINTERCARD, COPY, Functions.
- Explain security with ACL, TLS, and command restrictions.
- Operate Redis with INFO, SLOWLOG, keyspace events, and latency monitoring.
- Handle production Redis incidents: OOM, eviction storms, hot keys, Sentinel failover, cluster MOVED errors.
