# 17. Scenario: Design A Rate Limiter Using Redis

## Scenario

You are a senior engineer. The team needs a distributed rate limiter for an API gateway. Explain your design, Redis usage, failure modes, and tradeoffs.

---

## Requirements Clarification

Before answering, clarify:

- what is the window type? fixed or sliding?
- what is the granularity? per user, per IP, per route?
- what is the action on limit exceeded? block or throttle?
- what consistency is required? approximate or exact?
- what is the expected volume?

---

## Option 1: Fixed Window (INCR)

```text
key: rate:{userId}:{window}
window = floor(now / windowSize)

algorithm:
  count = INCR key
  if count == 1: EXPIRE key windowSize
  if count <= limit: allow
  else: reject
```

Implementation:

```bash
# Pseudocode in commands.
INCR rate:user:1001:1720000
# if count == 1: EXPIRE rate:user:1001:1720000 60
# if count <= 100: allow
```

Tradeoff: simple and fast. Boundary burst: 2x limit possible at window edges.

---

## Option 2: Sliding Window (Sorted Set + Lua)

```lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window_ms)
local count = redis.call('ZCARD', key)

if count < limit then
  redis.call('ZADD', key, now, now .. math.random())
  redis.call('PEXPIRE', key, window_ms)
  return 1
end
return 0
```

Tradeoff: smooth and exact, but more memory per user. Each request adds a sorted-set entry.

---

## Option 3: Token Bucket (Lua)

```lua
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])  -- tokens per second
local now = tonumber(ARGV[3])

local data = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(data[1]) or capacity
local last_refill = tonumber(data[2]) or now

local elapsed = (now - last_refill) / 1000
tokens = math.min(capacity, tokens + elapsed * refill_rate)

if tokens >= 1 then
  tokens = tokens - 1
  redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
  redis.call('PEXPIRE', key, math.ceil(capacity / refill_rate) * 1000 * 2)
  return 1
end
return 0
```

Tradeoff: allows bursting up to bucket capacity, then smooth refill.

---

## Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| Redis down | rate limiter fails open or closed | fail open with circuit breaker, monitor |
| Redis slow | increased latency on every request | async rate check, degraded mode |
| Clock skew across app servers | window boundaries off | use server-side timestamp in Lua |
| Hot key (single user, high traffic) | contention on sorted set | shard by sub-key or use pipeline |
| Memory growth | sorted set entries accumulate | ZREMRANGEBYSCORE + MAXLEN trim |

---

## Architecture Answer

```text
API Gateway -> Lua EVAL on Redis -> allow/reject
key pattern: rate:{type}:{identifier}:{optional-granularity}

Redis config:
- maxmemory with allkeys-lru
- no persistence for rate limiting (ephemeral)
- Sentinel or Cluster for HA

on Redis unavailability:
- fail open with flag: emergency_bypass = true
- alert on-call immediately
```

---

## Interview Sound Bite

A Redis rate limiter uses sorted sets for sliding windows or INCR for fixed windows, with Lua for atomicity. Key design decisions are: window type, granularity of the key, behavior on Redis failure (fail open vs closed), and memory growth management. In production, the rate limiter should be decoupled so failure degrades gracefully rather than blocking all traffic.
