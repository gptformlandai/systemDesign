# Cheatsheet 01: Data Structure Commands

## Strings

| Command | Syntax | Notes |
|---|---|---|
| SET | `SET key value [EX|PX] [NX|XX]` | atomic set with options |
| GET | `GET key` | O(1) |
| MSET | `MSET k1 v1 k2 v2` | multi-set |
| MGET | `MGET k1 k2` | multi-get |
| INCR | `INCR key` | atomic increment |
| INCRBY | `INCRBY key n` | increment by n |
| INCRBYFLOAT | `INCRBYFLOAT key f` | float increment |
| APPEND | `APPEND key value` | append to string |

## Lists

| Command | Syntax | Notes |
|---|---|---|
| LPUSH | `LPUSH key v [v...]` | push to head |
| RPUSH | `RPUSH key v [v...]` | push to tail |
| LPOP | `LPOP key [n]` | pop from head |
| RPOP | `RPOP key [n]` | pop from tail |
| BLPOP | `BLPOP key [key...] timeout` | blocking pop |
| LRANGE | `LRANGE key 0 -1` | range read |
| LLEN | `LLEN key` | length |
| LTRIM | `LTRIM key start stop` | trim in-place |

## Hashes

| Command | Syntax | Notes |
|---|---|---|
| HSET | `HSET key f v [f v...]` | set fields |
| HGET | `HGET key field` | get one field |
| HMGET | `HMGET key f1 f2` | get multiple |
| HGETALL | `HGETALL key` | all fields (O(N)) |
| HDEL | `HDEL key field` | delete field |
| HEXISTS | `HEXISTS key field` | check existence |
| HINCRBY | `HINCRBY key field n` | increment field |

## Sets

| Command | Syntax | Notes |
|---|---|---|
| SADD | `SADD key m [m...]` | add members |
| SREM | `SREM key m [m...]` | remove members |
| SISMEMBER | `SISMEMBER key m` | test membership |
| SMEMBERS | `SMEMBERS key` | all members (O(N)) |
| SCARD | `SCARD key` | count |
| SUNION | `SUNION k1 k2` | union |
| SINTER | `SINTER k1 k2` | intersection |
| SDIFF | `SDIFF k1 k2` | difference |

## Sorted Sets

| Command | Syntax | Notes |
|---|---|---|
| ZADD | `ZADD key [NX|GT|LT] score m` | add with score |
| ZINCRBY | `ZINCRBY key delta m` | increment score |
| ZSCORE | `ZSCORE key m` | get score |
| ZRANK | `ZRANK key m` | rank ascending |
| ZREVRANK | `ZREVRANK key m` | rank descending |
| ZRANGE | `ZRANGE key start stop [WITHSCORES] [REV]` | range by rank |
| ZRANGEBYSCORE | `ZRANGEBYSCORE key min max [WITHSCORES]` | range by score |
| ZREVRANGE | `ZREVRANGE key start stop [WITHSCORES]` | reverse range |
| ZREM | `ZREM key m` | remove member |
| ZCARD | `ZCARD key` | count |
| ZREMRANGEBYSCORE | `ZREMRANGEBYSCORE key min max` | remove by score |
