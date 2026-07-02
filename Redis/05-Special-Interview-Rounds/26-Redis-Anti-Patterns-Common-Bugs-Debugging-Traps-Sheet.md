# 26. Redis Anti-Patterns, Common Bugs, And Debugging Traps

## Goal

Know what NOT to do and why. These are the patterns that cause production incidents.

---

## Anti-Pattern 1: Using KEYS In Production

```bash
# NEVER in production.
KEYS user:*

# Why: O(N) and blocks Redis single thread for entire scan.
# 100,000 keys = ~100ms block.

# Correct: SCAN with cursor and COUNT hint.
SCAN 0 MATCH user:* COUNT 100
```

---

## Anti-Pattern 2: Keys Without TTL

```bash
# Bad: cache key with no expiry.
SET product:1001 '{"name":"widget"}'

# Good: always set TTL on cache keys.
SET product:1001 '{"name":"widget"}' EX 300
```

Keys without TTL grow without bound, eventually causing OOM or eviction of important keys.

---

## Anti-Pattern 3: SMEMBERS/HGETALL On Large Collections

```bash
# Dangerous: blocks if set has 100,000+ members.
SMEMBERS all:user:ids
HGETALL user:1001:attributes

# Correct: SSCAN or HSCAN with cursor.
SSCAN all:user:ids 0 COUNT 100
HSCAN user:1001:attributes 0 COUNT 50
```

---

## Anti-Pattern 4: Long-Running Lua Scripts

```lua
-- BAD: loop over 10,000 items in one script.
for i=1,10000 do
  redis.call('SET', 'key:' .. i, 'val')
end

-- Good: batch in small chunks, call from application.
-- Or use pipeline for independent writes.
```

Long Lua scripts block Redis. Target under 1ms script execution.

---

## Anti-Pattern 5: SELECT For Namespace Isolation

```bash
# Bad: using SELECT to isolate applications.
SELECT 1
SET cache:key1 value1
SELECT 2
SET app2:key1 value1

# Why: SELECT does not work in Cluster mode (only db 0 exists).
# All databases share maxmemory.
# Use separate Redis instances or key prefixes instead.
```

---

## Anti-Pattern 6: SUBSCRIBE In A Connection Pool Thread

```text
Bad: SUBSCRIBE uses a dedicated connection that blocks.
Taking a connection from the shared pool for SUBSCRIBE
starves application connections.

Good: use a dedicated, separate connection for Pub/Sub.
Never share the subscribed connection with other commands.
```

---

## Anti-Pattern 7: Storing Large Objects As Single Values

```bash
# Bad: 1MB JSON blob as a string.
SET user:1001:full_profile '{"name":...1MB...}'

# Problems:
# - MEMORY USAGE returns 1MB+
# - Single read fetches entire blob even for one field
# - Network bandwidth waste

# Better: hash for structured fields.
HSET user:1001 name "Alice" email "alice@example.com" plan "pro"

# Or: compress the blob before storing.
```

---

## Anti-Pattern 8: INCR Race Without Expiry

```bash
# Bug: set INCR counter without expiry on first increment.
count = INCR rate:user:1001:requests
# If application crashes before setting EXPIRE,
# key stays forever with no TTL.

# Fix: always use SET ... NX PX or check TTL after first INCR.
```

---

## Anti-Pattern 9: Ignoring Cluster Slot Constraints

```bash
# Bug in Cluster mode: multi-key commands across slots.
MSET user:1001 alice user:2002 bob   # CROSSSLOT error

# Fix: use hash tags to co-locate.
MSET {user}.1001 alice {user}.2002 bob   # same slot for both
```

---

## Anti-Pattern 10: Not Monitoring Eviction

```bash
INFO stats | grep evicted_keys
# evicted_keys:0  -> good
# evicted_keys:5000 -> urgent: data is being lost
```

Evictions silently lose data. Cache-aside applications may not notice immediately; queue-based applications may lose work items.

---

## Anti-Pattern 11: Indexing Every JSON Field

```text
Bad: create a search index over every field because "we might query it later."

Why it hurts:
- every indexed field consumes memory
- writes become more expensive
- rebuilds and migrations take longer

Better:
- index only access-pattern fields
- separate exact filters (TAG) from full-text fields (TEXT)
- capacity-plan index memory before production
```

---

## Anti-Pattern 12: Vector Search Without Memory Math

```text
Bad: store millions of 1536-dimensional FLOAT32 vectors without estimating RAM.

Raw memory:
1,000,000 * 1536 * 4 bytes ~= 6.1 GB before index overhead.

Better:
- estimate raw vector and index overhead
- version embeddings by model
- expire or archive cold vectors
- use a dedicated vector/search platform if corpus is too large
```

---

## Anti-Pattern 13: Near-Cache Without Invalidation Discipline

```text
Bad: local in-process Redis cache with no invalidation, no TTL, and no reconnect flush.

Better:
- use CLIENT TRACKING where supported
- enforce local TTL and max size
- flush local cache after reconnect/failover
- avoid near-cache for write-hot or correctness-critical keys
```

---

## Anti-Pattern 14: Functions As Heavy Business Logic

```text
Bad: Redis Function loops through thousands of records or performs complex domain workflow.

Why it hurts:
- FCALL blocks Redis command execution while running
- deployment and rollback become Redis operations
- Cluster cross-slot constraints surprise teams

Better:
- keep functions short and bounded
- version function libraries
- keep heavy workflow in application workers
```

---

## Debugging Traps

| Trap | Symptom | Investigation |
|---|---|---|
| connection leak | connected_clients grows | CLIENT LIST, fix pool release |
| MONITOR left running | latency doubles | redis-cli CLIENT LIST, kill MONITOR client |
| RDB save causing latency | latency spike every N minutes | check LASTSAVE frequency, tune save intervals |
| Cluster MOVED in hot path | slow commands | client slot map stale, force refresh |
| AOF fsync=always blocking | high write latency | INFO persistence, check aof_delayed_fsync |
| replica full resync loop | bandwidth spike | increase repl-backlog-size |
| stale near-cache | app returns old value | verify invalidations, TTL, reconnect flush |
| slow FCALL | Redis-wide latency | SLOWLOG, function review, bound work |
| index memory growth | OOM/eviction | FT.INFO, MEMORY STATS, index field audit |

---

## Interview Sound Bite

The most common Redis production bugs are KEYS in application code (immediate block), missing TTLs (silent OOM over time), SMEMBERS on large sets (latency spikes), and connection pool misconfiguration (connection exhaustion). Know these patterns by name and be able to diagnose them from INFO output and SLOWLOG.
