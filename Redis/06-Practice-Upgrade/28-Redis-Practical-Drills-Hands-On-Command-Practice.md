# 28. Redis Practical Drills: Hands-On Command Practice

## Instructions

Run each drill against a local Redis instance. Verify output matches expected. If expected output differs, diagnose the discrepancy.

---

## Drill 1: String And TTL Mechanics

```bash
SET session:abc123 "user:1001" EX 30
GET session:abc123
TTL session:abc123

SET counter:requests 0
INCR counter:requests
INCRBY counter:requests 5
GET counter:requests
# Expected: "6"

SET lock:job:5001 "worker-uuid-abc" NX PX 10000
# Expected: OK (first call)
SET lock:job:5001 "worker-uuid-xyz" NX PX 10000
# Expected: nil (already locked)
```

---

## Drill 2: List As Queue

```bash
DEL jobs:queue
RPUSH jobs:queue task1 task2 task3
LLEN jobs:queue
# Expected: 3
LPOP jobs:queue
# Expected: "task1"
LRANGE jobs:queue 0 -1
# Expected: ["task2","task3"]

RPUSH activity:user1001 "event-a" "event-b" "event-c" "event-d" "event-e"
LTRIM activity:user1001 0 2
LRANGE activity:user1001 0 -1
# Expected: ["event-a","event-b","event-c"]
```

---

## Drill 3: Hash As User Object

```bash
DEL user:1001
HSET user:1001 name "Alice" email "alice@example.com" plan "pro" login_count 0
HGETALL user:1001
HINCRBY user:1001 login_count 1
HGET user:1001 login_count
# Expected: "1"
HDEL user:1001 plan
HEXISTS user:1001 plan
# Expected: 0
```

---

## Drill 4: Set For Unique Membership

```bash
DEL online:users
SADD online:users user:1001 user:1002 user:1003
SISMEMBER online:users user:1001
# Expected: 1
SISMEMBER online:users user:9999
# Expected: 0
SCARD online:users
# Expected: 3
SREM online:users user:1002
SMEMBERS online:users
# Expected: {"user:1001","user:1003"}

SADD tags:post:1 redis cache backend
SADD tags:post:2 redis kafka streaming
SINTER tags:post:1 tags:post:2
# Expected: {"redis"}
```

---

## Drill 5: Sorted Set Leaderboard

```bash
DEL leaderboard:game1
ZADD leaderboard:game1 1500 player:alice 2200 player:bob 980 player:carol
ZREVRANGE leaderboard:game1 0 -1 WITHSCORES
# Expected: player:bob 2200, player:alice 1500, player:carol 980
ZREVRANK leaderboard:game1 player:alice
# Expected: 1
ZINCRBY leaderboard:game1 300 player:carol
ZSCORE leaderboard:game1 player:carol
# Expected: "1280"
```

---

## Drill 6: Expiry And Eviction Inspection

```bash
CONFIG GET maxmemory-policy
CONFIG SET maxmemory-policy allkeys-lru
CONFIG GET maxmemory-policy
# Expected: allkeys-lru

SET temp:key1 "value1" EX 5
TTL temp:key1
# Expected: 4 or 5

# Wait 6 seconds...
TTL temp:key1
# Expected: -2 (key missing)
```

---

## Drill 7: Streams Basic Flow

```bash
DEL events:test
XADD events:test * action login user_id 1001
XADD events:test * action logout user_id 1001
XLEN events:test
# Expected: 2
XRANGE events:test - +

XGROUP CREATE events:test audit 0 MKSTREAM
XREADGROUP GROUP audit worker-1 COUNT 10 STREAMS events:test >
# Capture the entry ID.
XACK events:test audit <id-from-above>
XPENDING events:test audit - + 100
# Expected: empty
```

---

## Drill 8: HyperLogLog

```bash
DEL unique:visitors:today
PFADD unique:visitors:today user:1001 user:1002 user:1003 user:1001 user:1002
PFCOUNT unique:visitors:today
# Expected: ~3 (approximate, may be exactly 3)

PFADD unique:visitors:yesterday user:1003 user:1004 user:1005
PFMERGE unique:visitors:week unique:visitors:today unique:visitors:yesterday
PFCOUNT unique:visitors:week
# Expected: ~5
```

---

## Drill 9: MULTI/EXEC Transaction

```bash
MULTI
INCR orders:count
INCR revenue:total
EXEC
# Expected: [1, 1] (or incremented values)

# Test WATCH abort.
SET watched:key 100
WATCH watched:key
# In another terminal: SET watched:key 200
MULTI
INCR watched:key
EXEC
# Expected: nil (transaction aborted because watched key changed)
```

---

## Drill 10: Lua Atomic Operation

```bash
# Conditional increment only if below limit.
EVAL "
  local current = tonumber(redis.call('GET', KEYS[1]))
  if current == nil then current = 0 end
  if current < tonumber(ARGV[1]) then
    return redis.call('INCR', KEYS[1])
  end
  return -1
" 1 counter:limit:test 3

# Run 4 times. First 3 should return 1, 2, 3. Fourth should return -1.
```

---

## Drill 11: Pub/Sub

```bash
# Terminal 1: subscribe.
SUBSCRIBE events:orders

# Terminal 2: publish.
PUBLISH events:orders '{"order_id":"5001","status":"placed"}'

# Terminal 1: should receive:
# 1) "message"
# 2) "events:orders"
# 3) '{"order_id":"5001","status":"placed"}'
```

---

## Drill 12: Modern Redis Compatibility Check

```bash
INFO server
COMMAND INFO JSON.SET
COMMAND INFO FT.CREATE
COMMAND INFO FUNCTION
COMMAND INFO CLIENT
COMMAND INFO TS.ADD
COMMAND INFO BF.ADD
```

Expected:

```text
Supported commands return command metadata.
Unsupported commands return empty output.
```

Reflection:

- Which modern Redis features does your local/serverless/managed Redis support?
- Which application features would break if you assumed JSON/Search/TimeSeries existed?
- Which client library version is needed for Functions or client-side caching?

---

## Drill 13: Function Deployment Smoke Test

Create a small function library and load it:

```lua
#!lua name=hello_v1

redis.register_function('hello', function(keys, args)
  return 'hello ' .. args[1]
end)
```

```bash
FUNCTION LOAD "$(cat hello_v1.lua)"
FCALL hello 0 redis
# Expected: "hello redis"

FUNCTION LIST
FUNCTION DELETE hello_v1
```

Reflection:

- How would you version `hello_v2`?
- Who should be allowed to run `FUNCTION LOAD` in production?
- What monitoring would catch a slow `FCALL`?
