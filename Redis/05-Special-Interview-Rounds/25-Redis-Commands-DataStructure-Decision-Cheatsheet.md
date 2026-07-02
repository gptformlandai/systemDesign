# 25. Redis Commands And Data Structure Decision Cheatsheet

## Data Structure Selection

| Problem | Data Structure | Key Pattern |
|---|---|---|
| cache a single value | string | `cache:{entity}:{id}` |
| store object attributes | hash | `user:{id}` |
| FIFO/LIFO queue | list | `queue:{name}` |
| capped activity feed | list + LTRIM | `activity:{userId}` |
| unique membership test | set | `online:users` |
| tag-based filtering | set | `tags:{postId}` |
| leaderboard | sorted set | `leaderboard:{gameId}` |
| sliding window rate limit | sorted set | `rate:{userId}:{action}` |
| priority queue | sorted set | `jobs:priority` |
| approximate unique count | HyperLogLog | `unique:visits:{date}` |
| proximity search | geo | `stores:{city}` |
| durable event log | stream | `events:{domain}` |
| fire-and-forget broadcast | Pub/Sub channel | `notifications:{topic}` |
| nested document | JSON | `product:{id}` |
| full-text / faceted search | Search index | `idx:products` |
| semantic similarity | vector field / Vector Set | `doc:{id}` |
| metrics with retention | Time Series | `metrics:{service}:{name}` |
| approximate membership | Bloom/Cuckoo | `bf:seen:{name}` |
| reusable server-side logic | Function | `library_name:function_name` |
| local near-cache invalidation | client-side caching | `CLIENT TRACKING` |

---

## Essential Commands Quick Reference

### Strings

```bash
SET key value [EX|PX|NX|XX|KEEPTTL|GET]
GET key
MSET k1 v1 k2 v2
MGET k1 k2
INCR key
INCRBY key delta
INCRBYFLOAT key delta
APPEND key value
STRLEN key
```

### Expiry

```bash
EXPIRE key seconds
PEXPIRE key milliseconds
TTL key          # -1: no expiry, -2: key missing
PERSIST key      # remove TTL
EXPIREAT key unix-timestamp
```

### Lists

```bash
LPUSH key val [val...]
RPUSH key val [val...]
LPOP key [count]
RPOP key [count]
BLPOP key [key...] timeout
LRANGE key start stop
LLEN key
LTRIM key start stop
```

### Hashes

```bash
HSET key field val [field val...]
HGET key field
HMGET key f1 f2
HGETALL key
HDEL key field
HEXISTS key field
HINCRBY key field delta
HKEYS key
HVALS key
```

### Sets

```bash
SADD key member [member...]
SREM key member [member...]
SISMEMBER key member
SMEMBERS key
SCARD key
SUNION key [key...]
SINTER key [key...]
SDIFF key [key...]
SUNIONSTORE dest key [key...]
```

### Sorted Sets

```bash
ZADD key [NX|XX|GT|LT] [CH] [INCR] score member
ZREM key member
ZSCORE key member
ZRANK key member
ZREVRANK key member
ZRANGE key start stop [WITHSCORES] [REV]
ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]
ZREVRANGE key start stop [WITHSCORES]
ZCARD key
ZCOUNT key min max
ZREMRANGEBYSCORE key min max
ZINCRBY key delta member
```

### Streams

```bash
XADD stream [MAXLEN [~] count] * field value [field value...]
XREAD [COUNT n] [BLOCK ms] STREAMS stream id
XREADGROUP GROUP group consumer [COUNT n] [BLOCK ms] STREAMS stream >
XACK stream group id [id...]
XLEN stream
XRANGE stream - + [COUNT n]
XREVRANGE stream + - [COUNT n]
XINFO STREAM stream
XINFO GROUPS stream
XPENDING stream group - + count
XAUTOCLAIM stream group consumer min-idle-ms start [COUNT n]
XGROUP CREATE stream group id [MKSTREAM]
XTRIM stream MAXLEN [~] count
```

### Pub/Sub

```bash
PUBLISH channel message
SUBSCRIBE channel [channel...]
PSUBSCRIBE pattern [pattern...]
UNSUBSCRIBE [channel...]
PUBSUB CHANNELS [pattern]
PUBSUB NUMSUB [channel...]
```

### Transactions

```bash
MULTI
EXEC
DISCARD
WATCH key [key...]
```

### Scripting

```bash
EVAL script numkeys [key...] [arg...]
EVALSHA sha1 numkeys [key...] [arg...]
SCRIPT LOAD script
SCRIPT EXISTS sha1 [sha1...]
SCRIPT FLUSH
```

### Functions

```bash
FUNCTION LOAD library-code
FUNCTION LIST
FCALL function numkeys [key...] [arg...]
FCALL_RO function numkeys [key...] [arg...]
FUNCTION DELETE library-name
```

### JSON

```bash
JSON.SET key path json
JSON.GET key [path...]
JSON.DEL key [path]
JSON.NUMINCRBY key path number
JSON.ARRAPPEND key path value [value...]
```

### Search And Query

```bash
FT.CREATE index ON JSON|HASH PREFIX n prefix SCHEMA field AS alias TYPE
FT.SEARCH index query [RETURN n field...] [LIMIT offset count]
FT.INFO index
FT.DROPINDEX index [DD]
```

### Time Series And Probabilistic

```bash
TS.CREATE key [RETENTION ms] [LABELS label value...]
TS.ADD key timestamp value
TS.RANGE key from to [AGGREGATION type bucket]

BF.ADD key item
BF.EXISTS key item
CMS.INCRBY key item increment
TOPK.ADD key item [item...]
TDIGEST.ADD key value [value...]
```

### Client-Side Caching

```bash
HELLO 3
CLIENT TRACKING ON
CLIENT TRACKING ON BCAST PREFIX user: PREFIX product:
CLIENT TRACKING OFF
```

### Admin

```bash
INFO [section]
CONFIG GET pattern
CONFIG SET parameter value
CONFIG REWRITE
SLOWLOG GET [count]
SLOWLOG RESET
DEBUG SLEEP 0
MONITOR
CLIENT LIST
CLIENT KILL
MEMORY USAGE key
MEMORY DOCTOR
BGSAVE
BGREWRITEAOF
LASTSAVE
SCAN cursor [MATCH pattern] [COUNT count] [TYPE type]
DEBUG JMAP
```

### Cluster

```bash
CLUSTER INFO
CLUSTER NODES
CLUSTER KEYSLOT key
CLUSTER GETKEYSINSLOT slot count
```

### ACL

```bash
ACL LIST
ACL WHOAMI
ACL SETUSER username [rules...]
ACL GETUSER username
ACL DELUSER username
ACL LOG [RESET]
ACL CAT [category]
```

---

## Complexity Quick Reference

| Command | Complexity |
|---|---|
| GET/SET/HGET/HSET | O(1) |
| LRANGE | O(N) on result size |
| SMEMBERS | O(N) — dangerous on large sets |
| HGETALL | O(N) — dangerous on large hashes |
| SUNION/SINTER/SDIFF | O(N+M) |
| ZADD/ZRANK | O(log N) |
| ZRANGE/ZRANGEBYSCORE | O(log N + M) |
| KEYS | O(N) — blocks, avoid in production |
| SORT | O(N+M log M) |
| FT.SEARCH | depends on index/query/result size |
| JSON.GET/JSON.SET | depends on document/path size |
| FCALL | depends on function body; blocks while running |
