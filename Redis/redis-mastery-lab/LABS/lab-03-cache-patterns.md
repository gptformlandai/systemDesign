# Lab 03: Cache Patterns And Eviction

## Objective

Practice cache-aside pattern and observe eviction policy behavior.

## Exercises

### Exercise 1: Cache-Aside Simulation

```bash
# Simulate: check cache.
EXISTS cache:product:5001
# Expected: 0 (cache miss)

# Simulate: database returned data, write to cache.
SET cache:product:5001 '{"id":5001,"name":"widget","price":1999}' EX 60
GET cache:product:5001
TTL cache:product:5001
# Expected: value present, TTL ~60s
```

### Exercise 2: Cache Invalidation On Write

```bash
# Write-through invalidation pattern.
SET cache:product:5001 '{"id":5001,"name":"widget-v2","price":2199}' EX 60

# Or: invalidate on write.
DEL cache:product:5001
# Application would then re-populate on next read.
```

### Exercise 3: Eviction Policy Inspection

```bash
CONFIG GET maxmemory
CONFIG GET maxmemory-policy

# Change to allkeys-lru for a cache-only Redis.
CONFIG SET maxmemory-policy allkeys-lru
CONFIG GET maxmemory-policy
# Expected: allkeys-lru

# Revert to noeviction if using mixed data.
CONFIG SET maxmemory-policy noeviction
```

### Exercise 4: Observe Eviction Counter

```bash
INFO stats | grep evicted_keys
# Expected: evicted_keys:0 if no OOM

# Simulate low maxmemory (use only in lab, not production).
CONFIG SET maxmemory 1mb

# Add keys until eviction starts.
for i in $(seq 1 500); do
  redis-cli SET "fill:${i}" "$(head -c 2048 /dev/urandom | base64)" EX 300
done

INFO stats | grep evicted_keys
# Expected: non-zero if maxmemory was breached

# Reset.
CONFIG SET maxmemory 0
```

### Exercise 5: Key Without TTL Audit

```bash
SET no-ttl-key "persistent value"
TTL no-ttl-key
# Expected: -1 (no TTL, will never expire)

SET with-ttl-key "cache value" EX 60
TTL with-ttl-key
# Expected: ~60

# Rule: every cache key must have a TTL.
EXPIRE no-ttl-key 300
TTL no-ttl-key
# Expected: ~300
```

## Reflection

- What is the difference between `volatile-lru` and `allkeys-lru`?
- What would happen if `noeviction` is set and maxmemory is reached?
- When would you choose `allkeys-lfu` over `allkeys-lru`?
