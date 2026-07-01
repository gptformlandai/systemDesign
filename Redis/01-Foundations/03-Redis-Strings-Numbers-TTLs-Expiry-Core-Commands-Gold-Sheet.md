# 03. Redis Strings, Numbers, TTLs, Expiry, Core Commands

## Goal

Master the foundational string type: the primitive that underlies caching, counters, flags, tokens, and distributed locks.

---

## Strings Are More Than Strings

In Redis, the string type holds text, integers, floats, and binary blobs. Most cache patterns use strings.

```bash
# Basic set and get.
SET user:1001:name "Alice"
GET user:1001:name

# Set with TTL in seconds.
SET session:abc123 "user-data" EX 3600

# Set with TTL in milliseconds.
SET session:abc123 "user-data" PX 3600000

# Set only if not exists.
SET lock:payment SETID NX EX 30
SET lock:payment SETID NX EX 30
# Second call fails if first succeeded.

# Set only if exists.
SET user:1001:name "Bob" XX
```

---

## Expiry Management

```bash
# Set TTL on existing key (seconds).
EXPIRE key 300

# Set TTL on existing key (milliseconds).
PEXPIRE key 300000

# Set TTL as Unix timestamp.
EXPIREAT key 1893456000

# Read remaining TTL in seconds.
TTL key          # -1 means no expiry, -2 means key does not exist

# Read remaining TTL in milliseconds.
PTTL key

# Remove expiry (make persistent).
PERSIST key
```

---

## Atomic Counters

Redis integer operations are atomic without transactions.

```bash
INCR page:home:views
INCRBY page:home:views 5
INCR daily:orders:2026-07-01
DECR active:sessions
DECRBY active:sessions 3
INCRBYFLOAT metrics:latency 0.35
```

Use counters for: page views, request counts, rate limits, distributed sequence numbers, and inventory counts.

---

## Comparison: SET Options

| Option | Meaning |
|---|---|
| `EX seconds` | set TTL in seconds |
| `PX milliseconds` | set TTL in milliseconds |
| `NX` | only set if key does not exist |
| `XX` | only set if key already exists |
| `KEEPTTL` | preserve existing TTL when updating value |
| `GET` | return old value before setting new value |

---

## GETSET And Atomic Patterns

```bash
# Read and update atomically (deprecated but common in legacy code).
GETSET lock:payment newvalue

# Modern equivalent using SET GET option.
SET lock:payment newvalue GET

# Check and set with WATCH (optimistic lock).
WATCH balance:user1001
multi_balance = GET balance:user1001
MULTI
SET balance:user1001 new_value
EXEC
```

---

## Other String Operations

```bash
# Append to a string.
APPEND log:stream:today "event-data "

# String length.
STRLEN user:1001:bio

# Get substring.
GETRANGE user:1001:bio 0 9

# Bulk operations.
MSET k1 v1 k2 v2 k3 v3
MGET k1 k2 k3

# Atomic multi-set only if none exist.
MSETNX k1 v1 k2 v2
```

---

## Production Rules

| Rule | Reason |
|---|---|
| always set TTL on cache keys | prevents memory from growing unbounded |
| use NX for distributed locking | prevents double-set race |
| prefer SCAN over KEYS | KEYS blocks the event loop |
| monitor hit ratio in INFO stats | keyspace_hits / (keyspace_hits + keyspace_misses) |
| namespace keys with colons | `user:1001:name` not `user_1001_name` |

---

## Interview Sound Bite

Redis strings handle text, integers, and binary blobs. Atomic INCR/DECR without transactions, NX for lock semantics, TTL management, and MGET for batched reads are the building blocks of cache, counter, token, and distributed lock patterns.
