# 16. Redis Advanced Patterns: Rate Limiting, Locks, Leaderboard, Geo, Bloom, HyperLogLog

## Goal

Implement production-grade advanced patterns using Redis beyond simple caching.

---

## Sliding Window Rate Limiter (Sorted Set)

```lua
-- Lua script for atomic sliding window check.
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window_ms)
local count = redis.call('ZCARD', key)

if count < limit then
  redis.call('ZADD', key, now, now .. '-' .. math.random())
  redis.call('PEXPIRE', key, window_ms)
  return 1
end
return 0
```

Key pattern: `rate:user:{userId}:{action}`

---

## Fixed Window Rate Limiter

```bash
# Increment request count in current window.
INCR rate:user:1001:2026070110   # year-month-day-hour

# Set TTL only on first request.
# Application: if INCR returns 1, set EXPIRE to window size.
```

---

## Distributed Lock (Simple)

```bash
# Acquire lock with NX (set if not exists) and PX (TTL in ms).
SET lock:payment:order:5001 worker-uuid NX PX 10000

# Release only if we own it (Lua for atomicity).
# EVAL:
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
  return redis.call('DEL', KEYS[1])
end
return 0
```

Rules:
- always set a TTL to prevent deadlock on crash
- always use a unique value (UUID) to prevent releasing another owner's lock
- check and delete atomically with Lua

---

## Redlock (Multi-Node Distributed Lock)

Redlock acquires the lock on N independent Redis nodes (typically 5), with majority quorum (3 of 5).

```text
1. Record start time.
2. Acquire lock on each node with SET NX PX.
3. Count successful acquisitions.
4. If acquired on majority (>= N/2+1) within validity time -> lock held.
5. Actual validity = TTL - elapsed time - clock drift margin.
6. Release on all nodes regardless of acquisition result.
```

Redlock is controversial for strict distributed safety (Martin Kleppmann critique). Use for best-effort distributed mutual exclusion with known failure modes understood.

---

## Leaderboard (Sorted Set)

```bash
# Update score (upsert).
ZADD leaderboard:game1 XX INCR 100 player:alice

# Top 10 with scores.
ZREVRANGE leaderboard:game1 0 9 WITHSCORES

# Player rank (0-indexed from top).
ZREVRANK leaderboard:game1 player:alice

# Score for a player.
ZSCORE leaderboard:game1 player:alice

# Players in a score range.
ZRANGEBYSCORE leaderboard:game1 1000 2000 WITHSCORES
```

---

## Session Store

```bash
# Store session as hash.
HSET session:abc123 user_id 1001 email alice@example.com created_at 1720000000

# Rolling TTL on activity.
EXPIRE session:abc123 1800   # 30 minutes

# Read entire session.
HGETALL session:abc123

# Delete session (logout).
DEL session:abc123
```

---

## Geospatial (GEO Commands)

```bash
# Add location.
GEOADD stores:london -0.1276 51.5074 "oxford-street"
GEOADD stores:london -0.1564 51.5081 "westfield"

# Distance between two members.
GEODIST stores:london "oxford-street" "westfield" km

# Nearby search.
GEOSEARCH stores:london FROMMEMBER "oxford-street" BYRADIUS 2 km ASC COUNT 5 WITHCOORD WITHDIST
```

---

## HyperLogLog (Approximate Cardinality)

```bash
# Add elements.
PFADD unique:visitors:2026-07-01 user:1001 user:1002 user:1003

# Count distinct elements (approximate, ~0.81% error).
PFCOUNT unique:visitors:2026-07-01

# Merge multiple HyperLogLogs.
PFMERGE unique:visitors:week unique:visitors:2026-07-01 unique:visitors:2026-07-02
```

HyperLogLog uses ~12 KB per key regardless of cardinality. Use for counting unique events, users, or IPs at scale.

---

## Bloom Filter (RedisBloom Module)

```bash
# Create and add items.
BF.ADD active_sessions "session:abc123"

# Test membership (may return false positive, never false negative).
BF.EXISTS active_sessions "session:abc123"   # 1 = probably yes
BF.EXISTS active_sessions "session:xyz999"   # 0 = definitely no
```

Bloom filters reduce expensive lookups: test membership cheaply before hitting the database.

---

## Pattern Decision Map

| Problem | Redis Pattern |
|---|---|
| rate limiting per user/IP | sorted set sliding window or INCR fixed window |
| mutual exclusion across services | SET NX PX with Lua delete |
| ranked scores with range queries | sorted set |
| user sessions with idle timeout | hash + rolling EXPIRE |
| proximity search | GEO commands |
| unique user count at scale | HyperLogLog |
| membership test before DB hit | Bloom filter |
| durable async event processing | Streams with consumer groups |

---

## Interview Sound Bite

Redis supports a full suite of production patterns beyond caching. Rate limiting uses sorted-set sliding windows or atomic INCR. Distributed locks combine SET NX PX with Lua-based conditional delete. Leaderboards use sorted sets with O(log N) score updates. HyperLogLog counts billions of unique items in 12 KB. Bloom filters gate expensive lookups with no false negatives. Geospatial commands handle proximity search natively.
