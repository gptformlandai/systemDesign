# 04. Redis Collection Types: Lists, Hashes, Sets, Sorted Sets

## Goal

Know which collection type fits each domain problem, and which commands operate on it.

---

## Lists

An ordered sequence. Best for queues, activity feeds, history, and stacks.

```bash
# Push to left (head) or right (tail).
LPUSH jobs:queue task1
RPUSH jobs:queue task2 task3

# Pop from left or right.
LPOP jobs:queue
RPOP jobs:queue

# Blocking pop (wait until element available).
BLPOP jobs:queue 10     # blocks up to 10 seconds

# Range read (0 to -1 is all elements).
LRANGE jobs:queue 0 -1
LRANGE activity:user1001 0 19   # last 20 items

# Length.
LLEN jobs:queue

# Trim to fixed size (good for capped feeds).
LTRIM activity:user1001 0 99    # keep only first 100
```

Use lists for: FIFO/LIFO queues, activity feeds with LTRIM, recent-events windows.

---

## Hashes

A map of field-value pairs within one key. Best for object/entity storage.

```bash
# Set fields.
HSET user:1001 name "Alice" email "alice@example.com" plan "pro"

# Get one field.
HGET user:1001 name

# Get all fields.
HGETALL user:1001

# Check existence.
HEXISTS user:1001 email

# Delete a field.
HDEL user:1001 plan

# Increment a numeric field.
HINCRBY user:1001 login_count 1

# Get only field names or values.
HKEYS user:1001
HVALS user:1001
```

Use hashes for: user profiles, configuration objects, entity attributes. Avoid HGETALL on huge hashes in hot paths.

---

## Sets

An unordered collection of unique strings. Best for membership, tagging, and set operations.

```bash
# Add members.
SADD page:home:viewers user:1001 user:1002 user:1003

# Check membership.
SISMEMBER page:home:viewers user:1001

# Remove member.
SREM page:home:viewers user:1002

# All members.
SMEMBERS page:home:viewers    # dangerous on large sets in production

# Cardinality.
SCARD page:home:viewers

# Set operations.
SUNION tags:post:1 tags:post:2          # union
SINTER tags:post:1 tags:post:2          # intersection
SDIFF tags:post:1 tags:post:2           # difference
SUNIONSTORE tags:merged tags:post:1 tags:post:2
```

Use sets for: tags, unique visitors, social connections, and set-algebra operations.

---

## Sorted Sets

An ordered collection with a float score per member. Best for leaderboards, priority queues, and sliding-window rate limiting.

```bash
# Add with score.
ZADD leaderboard:game1 1500 player:alice
ZADD leaderboard:game1 2200 player:bob
ZADD leaderboard:game1 980  player:carol

# Get rank (0-based, ascending).
ZRANK leaderboard:game1 player:alice

# Get rank (0-based, descending).
ZREVRANK leaderboard:game1 player:alice

# Range by rank (descending with scores).
ZREVRANGE leaderboard:game1 0 9 WITHSCORES

# Range by score.
ZRANGEBYSCORE leaderboard:game1 1000 3000 WITHSCORES

# Remove members below score.
ZREMRANGEBYSCORE rate:user1001 -inf 1720000000

# Cardinality.
ZCARD leaderboard:game1

# Score lookup.
ZSCORE leaderboard:game1 player:alice
```

Use sorted sets for: leaderboards, time-series event windows, rate limiters, priority queues.

---

## Data Structure Decision Map

| Need | Structure |
|---|---|
| cache a single value | string |
| store object fields | hash |
| ordered queue/stack | list |
| unique memberships/tags | set |
| ranked scores or time windows | sorted set |
| cardinality approximation | HyperLogLog |
| geospatial proximity | geospatial |
| event log with replay | stream |

---

## Interview Sound Bite

Choosing the right Redis data structure is the first interview signal. Lists for queues/feeds, hashes for entities, sets for memberships/tags, sorted sets for leaderboards and rate limiting, streams for durable event logs. The wrong structure leads to O(N) commands, memory waste, or missing atomicity.
