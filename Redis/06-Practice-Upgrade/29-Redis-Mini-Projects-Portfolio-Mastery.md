# 29. Redis Mini Projects For Portfolio And Mastery

## Instructions

Each project covers one or more Redis patterns. Build it locally, document it, and be able to explain it in an interview.

---

## Project 1: API Rate Limiter Service

**What to build:**
A rate limiting middleware that enforces per-user limits using Redis sorted sets (sliding window).

**Features:**
- Configurable limit and window size per route
- Sliding window via sorted set + Lua script
- Returns headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
- Fall-through behavior when Redis is unavailable (fail open)

**Redis patterns:** sorted set, EVAL Lua, PEXPIRE, ZREMRANGEBYSCORE
**Interviewable:** explain why Lua atomicity matters, what happens at window boundaries.

---

## Project 2: Leaderboard Service

**What to build:**
A global and per-period leaderboard with ranking, score updates, and top-N queries.

**Features:**
- Score update via ZINCRBY
- Top 10 leaderboard via ZREVRANGE
- Player rank lookup via ZREVRANK
- Daily and all-time leaderboards with separate keys
- TTL on daily keys for automatic expiry

**Redis patterns:** sorted set, ZINCRBY, ZREVRANGE, TTL
**Interviewable:** explain cardinality impact on memory, rolling period strategy.

---

## Project 3: Session Store With Rolling TTL

**What to build:**
A session management module that stores user sessions in Redis hashes with rolling TTL on each access.

**Features:**
- Create session: HSET + EXPIRE
- Validate session: HGETALL + EXPIRE refresh (rolling TTL)
- Invalidate session: DEL
- Session data: user_id, email, roles, created_at, last_active
- Configurable TTL: 30 minute idle timeout

**Redis patterns:** hash, EXPIRE, DEL
**Interviewable:** explain why rolling TTL matters, how session fixation is prevented.

---

## Project 4: Job Queue With Worker

**What to build:**
A background job queue where producers enqueue jobs and workers process with BLPOP.

**Features:**
- Enqueue: RPUSH jobs:queue job-payload-json
- Worker: BLPOP jobs:queue timeout-seconds (blocking wait)
- Retry: on failure, RPUSH jobs:retry after N seconds
- Dead letter: after 3 retries, RPUSH jobs:dead
- Priority queue variant: use sorted set with priority score

**Redis patterns:** list, RPUSH, BLPOP, sorted set
**Interviewable:** explain why BLPOP is preferred over polling, RPOPLPUSH pattern for safe queues.

---

## Project 5: Real-Time Event Feed Using Streams

**What to build:**
An event feed where multiple services consume independently with at-least-once delivery.

**Features:**
- Producer: XADD events:feed with MAXLEN cap
- Consumer groups: notifications, analytics, audit
- Worker per group: XREADGROUP, process, XACK
- Dead-letter: XAUTOCLAIM stuck entries, move to dlq stream after 3 retries
- Monitoring: XINFO GROUPS to track consumer lag

**Redis patterns:** streams, XADD, XREADGROUP, XACK, XAUTOCLAIM, XINFO
**Interviewable:** explain PEL, why MAXLEN matters, difference from Pub/Sub and Kafka.

---

## Stretch Project: Redis-Backed Feature Flags

**What to build:**
A lightweight feature flag service backed by Redis hashes.

**Features:**
- Flags stored as hash: HSET flags:global dark_mode off payments_v2 on
- Per-user overrides: HSET flags:user:1001 dark_mode on
- Read: check user override, fall back to global
- Keyspace notifications to invalidate local cache on flag change

**Redis patterns:** hash, keyspace notifications, Pub/Sub
**Interviewable:** explain cache invalidation via keyspace events, fallback pattern.
