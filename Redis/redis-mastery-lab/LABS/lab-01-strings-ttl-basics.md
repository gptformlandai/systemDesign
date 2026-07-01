# Lab 01: Strings, TTL, And Basic Key Operations

## Objective

Practice setting, reading, and expiring string keys. Verify TTL behavior.

## Prerequisites

Redis running locally on port 6379.

## Exercises

### Exercise 1: Basic SET/GET

```bash
SET greeting "hello redis"
GET greeting
# Expected: "hello redis"

SET greeting "updated"
GET greeting
# Expected: "updated"
```

### Exercise 2: SET With Options

```bash
# NX: only set if not exists.
SET mykey "first" NX
SET mykey "second" NX
GET mykey
# Expected: "first" (second SET was rejected)

# XX: only set if exists.
SET newkey "value" XX
GET newkey
# Expected: (nil) - key did not exist

# GET flag: return old value while setting new.
SET mykey "third" GET
# Expected: "first" (old value returned)
GET mykey
# Expected: "third"
```

### Exercise 3: Numeric Operations

```bash
SET counter 0
INCR counter
INCR counter
INCRBY counter 10
GET counter
# Expected: "12"

INCRBYFLOAT price 10.50
INCRBYFLOAT price 5.25
GET price
# Expected: "15.75"
```

### Exercise 4: TTL Operations

```bash
SET temp:data "will expire" EX 10
TTL temp:data
# Expected: 9 or 10

PERSIST temp:data
TTL temp:data
# Expected: -1 (no expiry)

EXPIRE temp:data 5
TTL temp:data
# Expected: 4 or 5

# Wait 6 seconds...
TTL temp:data
# Expected: -2 (key gone)
GET temp:data
# Expected: (nil)
```

### Exercise 5: MSET/MGET

```bash
MSET user:1001:name "Alice" user:1001:email "alice@example.com" user:1001:plan "pro"
MGET user:1001:name user:1001:email user:1001:plan
# Expected: ["Alice","alice@example.com","pro"]
```

## Reflection

- What is the difference between TTL returning -1 vs -2?
- Why should all cache keys have a TTL?
- When would you use SETNX vs SET ... NX?
