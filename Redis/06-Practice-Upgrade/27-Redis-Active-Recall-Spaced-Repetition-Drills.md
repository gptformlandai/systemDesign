# 27. Redis Active Recall And Spaced Repetition Drills

## Instructions

Cover each question. Write your answer before revealing the answer below. Revisit missed questions the next day.

---

## Round 1: Fundamentals

1. What is the time complexity of GET? SET? HGETALL? KEYS?
2. Name all eight Redis data structure types.
3. What does `volatile-lru` eviction mean versus `allkeys-lru`?
4. What does PERSIST do?
5. What is the difference between SAVE and BGSAVE?
6. What does AOF `appendfsync everysec` mean for durability?
7. What does `mem_fragmentation_ratio > 1.5` indicate?
8. What is the RESP protocol?

---

## Round 2: Intermediate

1. Explain the cache-aside pattern in three steps.
2. What is the difference between MULTI/EXEC and Lua scripting atomicity?
3. When does WATCH abort a transaction?
4. How does XREADGROUP `>` differ from reading with a specific ID?
5. What does XACK do?
6. How does PFADD differ from SADD in memory usage?
7. What is a hash tag in Redis Cluster and why is it used?
8. What does `notify-keyspace-events KEA` enable?

---

## Round 3: Senior Production

1. What is replication lag and how do you measure it?
2. What does WAIT 2 5000 do and what does it return?
3. What minimum Sentinel count is needed for quorum 2?
4. What is ODOWN vs SDOWN?
5. How many hash slots does Redis Cluster have?
6. What does `CLUSTER KEYSLOT` return?
7. What is a MOVED redirect versus an ASK redirect?
8. Name three dangerous commands to disable in production.

---

## Round 4: Architecture And Tradeoffs

1. When would you use Pub/Sub versus Streams?
2. What are the two main failure modes of Redlock?
3. What is a fencing token and why does it matter?
4. How does a stampede differ from a hot key problem?
5. What is the fix for KEYS found in SLOWLOG?
6. What eviction policy would you set for a pure cache with no persistence?
7. Name two ways to reduce memory usage for large sorted sets.
8. How does Redis Cluster handle a primary failure?

---

## Answer Key: Round 1

1. GET O(1), SET O(1), HGETALL O(N), KEYS O(N) and blocks.
2. string, list, hash, set, sorted set, stream, HyperLogLog, geospatial.
3. volatile-lru evicts from TTL-bearing keys only; allkeys-lru evicts from all keys.
4. PERSIST removes TTL from a key, making it persist indefinitely.
5. SAVE blocks Redis until snapshot complete; BGSAVE forks and writes in background.
6. Redis may lose up to 1 second of writes if it crashes.
7. Memory fragmentation: allocator holding freed pages; use active defrag.
8. Redis Serialization Protocol: simple text-based command protocol.

---

## Answer Key: Round 2

1. Read cache. On miss, read DB. Write DB result to cache with TTL.
2. Both atomic. Lua can read and decide mid-script; MULTI cannot read between queued commands.
3. WATCH aborts if any watched key is modified by another client before EXEC.
4. `>` means deliver next undelivered entries; specific ID re-delivers a specific range.
5. XACK removes the entry from the pending entries list (PEL) for a consumer group.
6. HyperLogLog uses ~12KB for any cardinality; SADD uses O(N) memory per member.
7. Hash tag: `{user}:1001` and `{user}:1002` hash to same slot for co-location.
8. Enables keyspace and keyevent notifications for all event types.

---

## Answer Key: Round 3

1. Replication lag: primary write offset minus replica write offset. Measure via INFO replication: `master_repl_offset - slave_repl_offset`.
2. WAIT blocks until 2 replicas confirm sync or 5000ms elapses; returns count of confirming replicas.
3. Three Sentinels minimum for quorum 2 (tolerate 1 Sentinel failure).
4. SDOWN: subjective down (one Sentinel's view). ODOWN: objective down (quorum agrees).
5. 16384 hash slots.
6. The slot number for the key.
7. MOVED = permanent redirect, update slot map. ASK = temporary, migration in progress.
8. FLUSHALL, DEBUG, KEYS (in production context), CONFIG.

---

## Answer Key: Round 4

1. Pub/Sub for fire-and-forget broadcast. Streams when persistence, replay, or consumer groups needed.
2. GC pause allowing expired lock to remain held; clock drift enabling dual quorum.
3. Monotonically increasing token returned with lock grant; protected resource rejects stale tokens.
4. Stampede: mass concurrent miss on expired key. Hot key: single key overloaded with reads continuously.
5. Replace KEYS with SCAN in application code.
6. `allkeys-lru` or `allkeys-lfu`.
7. Use ziplist/listpack encoding for small sorted sets; trim with ZREMRANGEBYSCORE regularly.
8. Replica promoted automatically; if no replica, slot range becomes unavailable.
