# Rate Limiter Lua Script Examples

## Sliding Window Rate Limiter

```lua
-- sliding-window-rate-limiter.lua
-- KEYS[1]: rate limit key, e.g. rate:user:1001:api
-- ARGV[1]: max requests per window
-- ARGV[2]: window size in milliseconds
-- ARGV[3]: current timestamp in milliseconds
-- Returns: 1 = allowed, 0 = denied

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local window_start = now - window_ms

-- Remove entries outside the window.
redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)

-- Count current entries in window.
local count = redis.call('ZCARD', key)

if count < limit then
  -- Add this request.
  redis.call('ZADD', key, now, now .. '-' .. redis.call('INCR', key .. ':seq'))
  redis.call('PEXPIRE', key, window_ms)
  return 1
end
return 0
```

---

## Fixed Window Rate Limiter

```lua
-- fixed-window-rate-limiter.lua
-- KEYS[1]: rate limit key with window embedded, e.g. rate:user:1001:2026070110
-- ARGV[1]: max requests per window
-- ARGV[2]: window TTL in seconds
-- Returns: current count if allowed, -1 if denied

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_ttl = tonumber(ARGV[2])

local count = redis.call('INCR', key)
if count == 1 then
  redis.call('EXPIRE', key, window_ttl)
end

if count <= limit then
  return count
end
return -1
```

---

## Token Bucket Rate Limiter

```lua
-- token-bucket.lua
-- KEYS[1]: token bucket key, e.g. bucket:user:1001
-- ARGV[1]: bucket capacity
-- ARGV[2]: refill rate (tokens per second)
-- ARGV[3]: current timestamp in milliseconds
-- Returns: 1 = allowed, 0 = denied

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local data = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(data[1])
local last_refill = tonumber(data[2])

if tokens == nil then
  tokens = capacity
  last_refill = now
end

-- Calculate refill.
local elapsed_seconds = (now - last_refill) / 1000
local refilled = elapsed_seconds * refill_rate
tokens = math.min(capacity, tokens + refilled)

if tokens >= 1 then
  tokens = tokens - 1
  local ttl_ms = math.ceil(capacity / refill_rate) * 1000 * 2
  redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
  redis.call('PEXPIRE', key, ttl_ms)
  return 1
end
redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
return 0
```

---

## Usage Examples

```bash
# Test sliding window: 10 requests per 60 seconds.
EVAL "$(cat sliding-window-rate-limiter.lua)" 1 rate:user:1001:api 10 60000 1720000000000

# Test token bucket: capacity 10, refill 1/second.
EVAL "$(cat token-bucket.lua)" 1 bucket:user:1001 10 1 1720000000000
```
