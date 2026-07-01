# 31. Pro Gap-Fill: Bitmaps, Encoding Internals, Redis 7 Commands

## Goal

Cover three areas missing from the main 30 sheets that appear in MAANG interviews: Bitmap commands, memory encoding internals with `OBJECT ENCODING`, and Redis 7 notable additions.

---

## Part A: Bitmaps

Redis bitmaps are string values treated as bit arrays. They are not a separate type — they use the string type with bit-level commands.

### Why Bitmaps

- O(1) set/get per bit
- Dense boolean tracking: 1 million flags = 125 KB (vs 1 million string keys = several hundred MB)
- Native population count: BITCOUNT in O(N) over a byte range

### Core Commands

```bash
# Set a bit at offset.
SETBIT user:activity:2026-07-01 1001 1
# 1 = mark user 1001 as active on this date

# Get a bit at offset.
GETBIT user:activity:2026-07-01 1001
# Expected: 1

# Count set bits in entire bitmap.
BITCOUNT user:activity:2026-07-01
# Expected: count of active users

# Count set bits in byte range [0..10].
BITCOUNT user:activity:2026-07-01 0 10

# Find first set bit (first 1) starting from bit 0.
BITPOS user:activity:2026-07-01 1

# Find first clear bit (first 0) starting from bit 0.
BITPOS user:activity:2026-07-01 0

# Bitwise operations across multiple bitmaps.
# BITOP AND|OR|XOR|NOT destkey key [key...]
BITOP AND active:both day1:users day2:users    # users active on both days
BITOP OR  active:either day1:users day2:users   # users active on either day
BITOP XOR active:changed day1:users day2:users  # changed between days
```

### Use Cases

| Use Case | Pattern |
|---|---|
| Daily active users | `SETBIT active:{date} {userId} 1` |
| Feature flag per user | `SETBIT feature:dark-mode {userId} {0 or 1}` |
| Attendance/presence tracking | `SETBIT attendance:{classId}:{date} {studentId} 1` |
| A/B test assignment | `SETBIT ab:test:{testId} {userId} {0 or 1}` |
| Visited pages per user | `SETBIT visited:{userId}:{pageId-bucket} {pageId} 1` |

### Memory Estimation

```text
Max offset in your bitmap determines allocation:
  offset 1000000 -> allocates 125001 bytes (~122 KB) regardless of how many bits are set

Rule: if max user ID is 100 million, bitmap is 12.5 MB per date.
Pre-allocate with SETBIT key 99999999 0 to trigger allocation early.
```

### Limitations

- Not cluster-friendly: huge bitmaps hash to one node
- Offset determines size: sparse high-offset bitmaps waste memory
- Workaround: shard by `{userId mod N}` ranges into separate bitmap keys

---

## Part B: Memory Encoding Internals

Redis uses different internal encodings per data type depending on size. Understanding this is critical for memory tuning.

### OBJECT ENCODING

```bash
# Check internal encoding of a key.
OBJECT ENCODING mykey

# Other OBJECT subcommands.
OBJECT REFCOUNT mykey       # reference count (internal)
OBJECT IDLETIME mykey       # seconds since last access
OBJECT FREQ mykey           # access frequency (LFU eviction)
OBJECT HELP                 # list all subcommands
```

### Encoding Transitions

| Type | Small encoding | Large encoding | Threshold config |
|---|---|---|---|
| string | int (for integers) | embstr / raw | embstr at <= 44 bytes |
| list | listpack | quicklist | `list-max-listpack-size` |
| hash | listpack | hashtable | `hash-max-listpack-entries` (128), `hash-max-listpack-value` (64) |
| set | listpack or intset | hashtable | `set-max-intset-entries` (512), `set-max-listpack-entries` (128) |
| sorted set | listpack | skiplist + hashtable | `zset-max-listpack-entries` (128), `zset-max-listpack-value` (64) |

### Why This Matters

```bash
# Small hash using listpack encoding (memory-efficient).
HSET small:profile name "Alice" age "30"
OBJECT ENCODING small:profile
# Expected: listpack (compact, cache-friendly)

# Large hash after threshold crossed.
# After adding 129+ fields or any field > 64 bytes:
OBJECT ENCODING large:profile
# Expected: hashtable (more memory, faster for large sizes)
```

Listpack is more memory-efficient but requires linear scan. Hashtable is faster for large collections. Redis automatically promotes when thresholds are crossed.

### Tuning For Memory

```bash
# Increase listpack threshold for small entity caching (saves memory).
CONFIG SET hash-max-listpack-entries 256
CONFIG SET hash-max-listpack-value 128

# Increase intset threshold for small integer sets.
CONFIG SET set-max-intset-entries 1024

# Check encoding in monitoring.
OBJECT ENCODING user:1001
# If promoted to hashtable earlier than expected, check entry count and value size.
```

### intset Encoding For Sets

If all members of a set are integers, Redis uses a compact integer array (intset):

```bash
SADD intset:test 1 2 3 4 5
OBJECT ENCODING intset:test
# Expected: intset

SADD intset:test "string-member"
OBJECT ENCODING intset:test
# Expected: hashtable (promoted on adding non-integer)
```

---

## Part C: Redis 7 Notable Additions

### LMPOP and ZMPOP (Redis 7.0)

Pop from multiple keys, taking the first non-empty one:

```bash
# Pop 2 elements from the first non-empty list.
LMPOP 3 jobs:high jobs:medium jobs:low LEFT COUNT 2

# Pop 2 elements from the first non-empty sorted set (ascending scores).
ZMPOP 2 leaderboard:active leaderboard:archive MIN COUNT 2
```

Use case: priority queue consumer that checks multiple queues atomically.

### SINTERCARD (Redis 7.0)

Count intersection cardinality without materializing the full intersection:

```bash
# Count members in common between two sets (limit to 100).
SINTERCARD 2 tags:post:1 tags:post:2 LIMIT 100
# Returns count only, not the members.
```

More memory-efficient than SINTER when you only need the count.

### LPOS (Redis 6.0.6)

Find position of an element in a list:

```bash
# Find first occurrence.
LPOS mylist "target-value"

# Find all occurrences (RANK = 1 means first from head).
LPOS mylist "target-value" RANK 1 COUNT 0

# Skip first 2 occurrences, find next.
LPOS mylist "target-value" RANK 3
```

### GETDEL and GETEX (Redis 6.2)

Atomic get-and-delete and get-and-set-expiry:

```bash
# Get and delete in one atomic operation.
GETDEL idempotency:req:abc123
# Use for one-time-use token patterns.

# Get and set new TTL atomically.
GETEX session:abc123 EX 1800
# Rolling TTL in one round-trip instead of GET + EXPIRE.

# Get and persist (remove TTL).
GETEX session:abc123 PERSIST

# Get and set absolute expiry.
GETEX session:abc123 EXAT 1754000000
```

`GETEX` replaces the common pattern `GET + EXPIRE` for rolling TTLs with a single atomic command.

### COPY (Redis 6.2)

Copy a key to a new key without deleting the source:

```bash
# Copy key to destination.
COPY source:key destination:key

# Copy across databases.
COPY source:key destination:key DB 1

# Replace destination if it exists.
COPY source:key destination:key REPLACE
```

Use for: cloning template objects, database-to-database migration, snapshot-before-modify patterns.

### Functions (Redis 7.0): FUNCTION vs EVAL

Functions are a first-class alternative to Lua scripts loaded via EVAL:

```bash
# Load a function library.
FUNCTION LOAD "#!lua name=mylib\nredis.register_function('myfunc', function(keys, args) return redis.call('GET', keys[1]) end)"

# Call a function.
FCALL myfunc 1 mykey

# List functions.
FUNCTION LIST

# Delete a function.
FUNCTION DELETE mylib
```

Key difference from EVAL: Functions are stored persistently in Redis (survive restarts), versioned, and introspectable. EVAL requires re-sending the script on every restart.

---

## Diagnostic CLI Flags (Enhancement)

```bash
# Top largest keys (scans entire keyspace).
redis-cli --bigkeys

# Top most-accessed keys (requires LFU eviction enabled).
redis-cli --hotkeys

# Memory usage of keys matching a pattern.
redis-cli --memkeys

# Live stats dashboard (refreshes every second).
redis-cli --stat

# Continuous latency measurement.
redis-cli --latency

# Latency history (per-interval percentiles).
redis-cli --latency-history -i 5

# Test max throughput with pipelining.
redis-cli --pipe

# Simulate latency distribution.
redis-cli --latency-dist
```

---

## Interview Sound Bite

Bitmaps provide bit-level set/get with native population count, perfect for active-user tracking at massive scale. Memory encoding internals — listpack vs hashtable, intset vs hashtable — directly affect memory usage; check with OBJECT ENCODING and tune thresholds in CONFIG. Redis 7 added LMPOP/ZMPOP for multi-queue atomic pop, GETEX for single-RTT rolling TTL, GETDEL for one-time token patterns, SINTERCARD for cardinality-only intersection, and Functions for persistent server-side scripting. These appear in MAANG interviews and distinguish candidates who have operated Redis in production.
