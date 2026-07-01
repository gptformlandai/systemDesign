# Lab 02: Collection Types

## Objective

Practice lists, hashes, sets, and sorted sets with real-world key patterns.

## Exercises

### Exercise 1: List As Job Queue

```bash
DEL jobs:email:queue
RPUSH jobs:email:queue '{"to":"alice","subject":"welcome"}'
RPUSH jobs:email:queue '{"to":"bob","subject":"order"}'
RPUSH jobs:email:queue '{"to":"carol","subject":"reset"}'
LLEN jobs:email:queue
# Expected: 3

LPOP jobs:email:queue
# Expected: {"to":"alice"...}

LRANGE jobs:email:queue 0 -1
# Expected: 2 remaining items
```

### Exercise 2: Capped Activity Feed

```bash
DEL activity:user:1001
RPUSH activity:user:1001 "login" "view:product:1" "view:product:2" "add:cart:1" "checkout"
LLEN activity:user:1001
# Expected: 5

LTRIM activity:user:1001 0 2
LRANGE activity:user:1001 0 -1
# Expected: ["login","view:product:1","view:product:2"]
```

### Exercise 3: User Object As Hash

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
HEXISTS user:1001 name
# Expected: 1
```

### Exercise 4: Tag Set Operations

```bash
SADD tags:post:1 redis cache backend database
SADD tags:post:2 redis kafka streaming backend
SADD tags:post:3 python data-science machine-learning

SCARD tags:post:1
# Expected: 4

SINTER tags:post:1 tags:post:2
# Expected: {"redis","backend"}

SUNION tags:post:1 tags:post:2
# Expected: {"redis","cache","backend","database","kafka","streaming"}

SDIFF tags:post:1 tags:post:2
# Expected: {"cache","database"}
```

### Exercise 5: Sorted Set Leaderboard

```bash
DEL leaderboard:lab
ZADD leaderboard:lab 100 alice
ZADD leaderboard:lab 250 bob
ZADD leaderboard:lab 75  carol

ZREVRANGE leaderboard:lab 0 -1 WITHSCORES
# Expected: bob 250, alice 100, carol 75

ZREVRANK leaderboard:lab alice
# Expected: 1

ZINCRBY leaderboard:lab 200 carol
ZSCORE leaderboard:lab carol
# Expected: "275"

ZREVRANGE leaderboard:lab 0 -1 WITHSCORES
# Expected: carol 275, bob 250, alice 100
```

## Reflection

- When would you use LRANGE vs LTRIM?
- What is the risk of SMEMBERS on a set with 1 million members?
- Why does a sorted set fit leaderboards better than a list?
