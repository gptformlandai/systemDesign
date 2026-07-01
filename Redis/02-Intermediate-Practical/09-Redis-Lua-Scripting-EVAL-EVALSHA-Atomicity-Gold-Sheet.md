# 09. Redis Lua Scripting: EVAL, EVALSHA, Atomicity

## Goal

Write atomic read-compute-write operations in Redis using Lua scripts executed on the server.

---

## Why Lua

Redis executes Lua scripts atomically. No other command runs while a script is executing. This enables patterns that MULTI/EXEC cannot: read a value, make a decision, write based on that decision — all in one atomic operation.

---

## EVAL Basics

```bash
# EVAL script numkeys key [key ...] arg [arg ...]
EVAL "return redis.call('GET', KEYS[1])" 1 mykey

# Conditional increment only if value is below limit.
EVAL "
  local current = tonumber(redis.call('GET', KEYS[1]))
  if current == nil then current = 0 end
  if current < tonumber(ARGV[1]) then
    redis.call('INCR', KEYS[1])
    return 1
  end
  return 0
" 1 counter:user:1001 100
```

Keys are accessed via `KEYS[n]`, arguments via `ARGV[n]` (both 1-indexed).

---

## EVALSHA: Cached Scripts

```bash
# Load script and get SHA.
SCRIPT LOAD "return redis.call('GET', KEYS[1])"

# Execute by SHA.
EVALSHA <sha1> 1 mykey

# Check if script is cached.
SCRIPT EXISTS <sha1>

# Flush cached scripts.
SCRIPT FLUSH
```

EVALSHA avoids sending the full script body on every call. In production, load scripts at startup and use EVALSHA in hot paths.

---

## Rate Limiter Lua Example

Sliding window with Lua: atomic check-and-increment.

```lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local window_start = now - window_ms

-- Remove old entries from the sorted set.
redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)

-- Count current entries.
local count = redis.call('ZCARD', key)

if count < limit then
  -- Add current request timestamp.
  redis.call('ZADD', key, now, now)
  redis.call('PEXPIRE', key, window_ms)
  return 1
end
return 0
```

---

## Script Safety Rules

| Rule | Reason |
|---|---|
| keep scripts short | long scripts block Redis event loop |
| no blocking calls inside scripts | will hang all other clients |
| no random() that changes behavior | scripts must be deterministic for replication |
| no external I/O | scripts cannot call APIs, open files, etc. |
| pass keys as KEYS[] | required for Cluster key-slot routing |
| pass values as ARGV[] | keeps scripts reusable |

---

## Error Handling In Lua

```lua
-- Use pcall for handled errors.
local ok, err = pcall(function()
  redis.call('SET', KEYS[1], ARGV[1])
end)
if not ok then
  return redis.error_reply('operation failed: ' .. err)
end
return redis.status_reply('OK')
```

---

## Interview Sound Bite

Lua scripts in Redis execute atomically as a single operation. They are the correct choice when MULTI/EXEC is insufficient because you need to read-decide-write atomically. Always pass keys via KEYS[] and values via ARGV[], keep scripts short to avoid blocking the event loop, and use EVALSHA in production to avoid repeated script transmission.
