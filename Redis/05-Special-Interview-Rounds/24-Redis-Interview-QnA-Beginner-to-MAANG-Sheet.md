# 24. Redis Interview Q&A: Beginner To MAANG

## Structure

For each question: answer in one sentence, then expand with depth. Signal seniority by addressing failure modes, tradeoffs, and production experience.

---

## Beginner Level

**Q: What is Redis?**
A: Redis is an in-memory data structure server that supports strings, lists, hashes, sets, sorted sets, streams, and geospatial data, used for caching, session storage, queues, and real-time analytics.

**Q: What makes Redis fast?**
A: Redis stores data in RAM, uses a single-threaded event loop for command execution (no lock contention), and communicates over a simple binary protocol (RESP).

**Q: What is the difference between EXPIRE and TTL?**
A: EXPIRE sets a key expiry in seconds; TTL returns the remaining time-to-live in seconds, or -1 if no expiry, or -2 if key does not exist.

**Q: What is the difference between KEYS and SCAN?**
A: KEYS is O(N) and blocks Redis for the entire scan; SCAN is cursor-based and iterates in small batches without blocking, making it safe for production use.

**Q: What is the default eviction policy?**
A: `noeviction`: Redis returns an error when memory is full. For caches, change it to `allkeys-lru` or `allkeys-lfu`.

---

## Intermediate Level

**Q: What is the difference between Pub/Sub and Streams?**
A: Pub/Sub is fire-and-forget with no persistence or consumer groups; Streams are a persistent append-only log with consumer groups, at-least-once delivery, acknowledgement, and replay.

**Q: How do you implement a distributed lock in Redis?**
A: Use `SET key uuid NX PX ttl` for atomic acquire-or-fail; release with a Lua script that checks the UUID before deleting to prevent releasing another owner's lock.

**Q: What is the difference between MULTI/EXEC and Lua scripting?**
A: MULTI/EXEC queues commands and executes them atomically but cannot read data mid-transaction; Lua scripts execute atomically and can read values and make decisions within the same operation.

**Q: What is cache stampede and how do you prevent it?**
A: Cache stampede is when many concurrent requests hit a cache miss for the same key simultaneously. Prevention: mutex lock on regeneration, probabilistic early refresh, or stale-while-revalidate pattern.

**Q: How does Redis replication work?**
A: Replicas connect to the primary, receive an initial RDB snapshot, then stream command replication asynchronously. PSYNC enables partial resync from the replication backlog on reconnect.

---

## Senior Level

**Q: What is Redis Sentinel and how does failover work?**
A: Sentinel is a monitoring and orchestration layer. Three or more Sentinels monitor the primary; when quorum agrees the primary is down, a leader Sentinel promotes the best replica and reconfigures other replicas and clients.

**Q: What are the failure modes of Redlock?**
A: GC pauses can cause a client to hold an expired lock; clock drift can allow two clients to hold lock simultaneously; node restart without persistence can create ghost quorum. The fencing token pattern addresses stale-holder safety.

**Q: How does Redis Cluster route requests?**
A: Cluster assigns keys to 16384 hash slots via CRC16 mod 16384. Each node owns a slot range. Clients receive MOVED redirects for permanent rerouting and ASK for in-progress migrations.

**Q: How do you prevent data loss in a Redis Sentinel setup?**
A: Configure `min-replicas-to-write` and `min-replicas-max-lag` on the primary to refuse writes if no replicas are in sync. Use WAIT on critical writes to confirm replica acknowledgement.

**Q: When would you choose Redis Streams over Kafka?**
A: Redis Streams suit moderate-throughput, low-latency event pipelines with simple operational requirements and co-located cache/stream needs. Kafka is preferred for high-throughput, multi-partition, multi-consumer fan-out at massive scale with longer retention and exactly-once semantics.

---

## MAANG Architecture Level

**Q: You have a hot key receiving 500k reads per second. What do you do?**
A: Shard the hot key across N copies (`hot_key:{0-9}`), use local in-process cache in application memory with a short TTL, route reads to read replicas, and break large values into smaller keys.

**Q: Design a session management system for 100 million users.**
A: Store sessions as Redis hashes keyed by session token with rolling EXPIRE on access. Use Redis Cluster for horizontal scale. Set `maxmemory-policy volatile-lru` to evict expired sessions under memory pressure. Token rotation on authentication. Invalidate on logout with DEL.

**Q: How do you migrate a Redis single-node setup to Cluster without downtime?**
A: Run Cluster in parallel, use `CLUSTER MEET` and slot migration, route reads to Cluster nodes progressively, shift writes with blue-green DNS switch, monitor for MOVED errors, then decommission the old node.

---

## Modern Redis Pro Level

**Q: When would you use Redis JSON instead of storing JSON as a string?**
A: Use Redis JSON when you need path-level reads/updates or indexing over document fields. If the value is small, opaque, and always read/written as a whole, a normal string may be simpler and cheaper.

**Q: What is the difference between TAG and TEXT fields in Redis search?**
A: TAG fields are for exact filtering such as tenant, brand, category, or status; TEXT fields are tokenized for full-text search. Choosing TEXT for exact filters wastes index behavior, and choosing TAG for natural language search gives poor search semantics.

**Q: How do you decide whether Redis is a good vector search layer?**
A: Redis is a good fit for hot, low-latency vector search when the corpus and vector index fit in memory. For billion-scale corpora, long retention, complex ranking, or cheaper storage, use a dedicated vector/search platform and keep Redis as cache or hot reranker.

**Q: What is client-side caching in Redis?**
A: Client-side caching is server-assisted near-caching: the app stores local copies of Redis reads and Redis sends invalidation messages when tracked keys change. It reduces hot-key pressure but requires TTLs, size bounds, and cache flush on reconnect or failover.

**Q: Why use Redis Functions instead of EVALSHA everywhere?**
A: Functions are named, persistent, introspectable, and can be deployed as versioned libraries. EVALSHA depends on script cache lifecycle and can fail with NOSCRIPT unless clients reload scripts correctly.

**Q: What do you check before upgrading Redis 7 to Redis 8?**
A: Verify client library support, command compatibility, persistence restore/rollback, Cluster/Sentinel behavior, monitoring/exporter parsing, ACL rules, and managed-provider support. Canary the upgrade and avoid downgrade rollback if new data types or formats are written.

**Q: How is managed Redis different from self-managed Redis in architecture interviews?**
A: Managed Redis reduces infrastructure work but does not remove data-model, TTL, eviction, client, failover, security, cost, or backup responsibility. You still need to test provider failover and understand provider command/version limits.

---

## Interview Sound Bite

Answer with one clear sentence, then add depth: failure modes, tradeoffs, and production decisions. Interviewers at senior levels test whether you have faced these problems in production, not just studied them.
