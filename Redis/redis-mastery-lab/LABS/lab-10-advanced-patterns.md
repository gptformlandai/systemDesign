# Lab 10: Advanced Patterns — Rate Limiter, HLL, Geo, Streams Event Bus

## Objective

Implement advanced Redis patterns end-to-end in the CLI.

## Exercises

### Exercise 1: Sliding Window Rate Limiter

```bash
LUA='
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
redis.call("ZREMRANGEBYSCORE", key, "-inf", now - window_ms)
local count = redis.call("ZCARD", key)
if count < limit then
  redis.call("ZADD", key, now, now .. math.random())
  redis.call("PEXPIRE", key, window_ms)
  return 1
end
return 0'

# 3 requests per 10 seconds.
for i in 1 2 3 4 5; do
  NOW=$(date +%s000)
  RES=$(redis-cli EVAL "$LUA" 1 rate:lab:user1 3 10000 "$NOW")
  echo "Request $i: $([ "$RES" = '1' ] && echo ALLOWED || echo DENIED)"
done
```

### Exercise 2: HyperLogLog Unique Visitors

```bash
PFADD unique:visitors:2026-07-01 user:1001 user:1002 user:1003 user:1004 user:1001
PFCOUNT unique:visitors:2026-07-01
# Expected: ~4 (1001 counted once)

PFADD unique:visitors:2026-07-02 user:1003 user:1004 user:1005 user:1006
PFMERGE unique:visitors:week unique:visitors:2026-07-01 unique:visitors:2026-07-02
PFCOUNT unique:visitors:week
# Expected: ~6
```

### Exercise 3: Geospatial Search

```bash
DEL stores:london
GEOADD stores:london -0.1276 51.5074 "oxford-street"
GEOADD stores:london -0.1564 51.5081 "westfield"
GEOADD stores:london -0.0982 51.5195 "islington"

GEODIST stores:london "oxford-street" "westfield" km
GEOSEARCH stores:london FROMMEMBER "oxford-street" BYRADIUS 3 km ASC WITHCOORD WITHDIST
```

### Exercise 4: Distributed Lock Pattern

```bash
# Acquire.
SET lock:resource:5001 "worker-uuid-abc" NX PX 10000
# Expected: OK

# Attempt re-acquire (should fail).
SET lock:resource:5001 "worker-uuid-xyz" NX PX 10000
# Expected: (nil)

# Release only if we own it.
EVAL "
  if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
  end
  return 0
" 1 lock:resource:5001 "worker-uuid-abc"
# Expected: 1

# Lock gone.
EXISTS lock:resource:5001
# Expected: 0
```

### Exercise 5: Event Bus With Streams

```bash
DEL events:orders
XGROUP CREATE events:orders notifications 0 MKSTREAM
XGROUP CREATE events:orders analytics 0 MKSTREAM

# Publish 3 events.
XADD events:orders * order_id 5001 event order_placed
XADD events:orders * order_id 5002 event order_shipped
XADD events:orders * order_id 5001 event order_cancelled

# notifications group reads all 3.
XREADGROUP GROUP notifications worker-1 COUNT 10 STREAMS events:orders >

# analytics group reads independently (its own offset).
XREADGROUP GROUP analytics worker-a COUNT 10 STREAMS events:orders >

XINFO GROUPS events:orders
# Both groups at same stream length.
```

## Reflection

- What is the memory cost of adding 1 million items to a HyperLogLog vs a Set?
- Why does GEOADD use longitude before latitude?
- Can two consumer groups process the same stream event independently?
