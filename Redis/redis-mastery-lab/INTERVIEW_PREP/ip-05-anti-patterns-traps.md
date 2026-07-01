# Interview Prep 05: Anti-Patterns And Debugging Traps

## Top Anti-Patterns (Must Know)

**1. KEYS in production**
Blocks Redis event loop for entire scan. Replace with SCAN cursor iteration.

**2. No TTL on cache keys**
Keys grow without bound. Every cache key must have a TTL.

**3. SMEMBERS/HGETALL on large collections**
O(N) and can block Redis for millions of members. Use SSCAN/HSCAN with COUNT, or restructure data.

**4. Long Lua scripts**
Scripts block the single-threaded event loop. Target < 1ms. Break into batches called from the application.

**5. SELECT for namespace isolation**
Does not work in Cluster mode. Use key prefixes instead.

**6. SUBSCRIBE in a connection pool thread**
Pub/Sub subscribes a connection exclusively. Sharing with other commands causes starvation.

**7. Storing large blobs as a single value**
1 MB+ values waste memory and bandwidth on every read. Use hashes for structured data, or compress.

**8. INCR without TTL on first increment**
Counter key stays forever. Check if count == 1, then set EXPIRE.

---

## Common Debugging Traps

**Trap: MONITOR left running**
Doubles Redis CPU load. Identify with CLIENT LIST, kill the MONITOR client.

**Trap: Not restarting after CONFIG REWRITE**
CONFIG SET changes are temporary without CONFIG REWRITE. On restart, old config resumes.

**Trap: Hard-coded primary address**
Breaks silently after Sentinel failover. Always use Sentinel-aware client discovery.

**Trap: RDB save fork latency**
`BGSAVE` forks the process, which can cause latency spikes on large datasets without copy-on-write. Use SSD and tune `save` intervals.

**Trap: AOF delayed fsync**
Check `INFO persistence | grep aof_delayed_fsync`. Spikes indicate write pressure exceeding fsync throughput.

**Trap: Cluster MOVED loop**
Client slot map is stale. Force slot map refresh. Usually self-corrects with cluster-aware client libraries.

---

## Quick Diagnosis Reference

| Symptom | First Command |
|---|---|
| timeouts from application | redis-cli PING; redis-cli --latency |
| OOM errors | INFO memory; redis-cli --bigkeys |
| slow commands | SLOWLOG GET 25 |
| connection failures | INFO clients; CLIENT LIST |
| replica out of sync | INFO replication |
| unexpected eviction | INFO stats; grep evicted_keys |
| high CPU | INFO commandstats; MONITOR short burst |
