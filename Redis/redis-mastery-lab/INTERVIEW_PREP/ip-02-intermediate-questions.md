# Interview Prep 02: Intermediate Questions

## Questions And Answers

**Q: Explain the cache-aside pattern.**
On read: check Redis. On miss, query the database, write result to Redis with TTL, return. On write: write to database, invalidate or update the cache key.

**Q: What is the difference between MULTI/EXEC and Lua scripting?**
MULTI/EXEC queues commands and executes them atomically, but cannot read values mid-transaction. Lua scripts execute atomically and can read, compute, and write in a single operation.

**Q: What is WATCH?**
WATCH marks keys to observe. If any watched key is modified before EXEC, the transaction is aborted and returns nil. It implements optimistic locking.

**Q: What is cache stampede?**
When many concurrent requests hit a cache miss for the same key and all query the database simultaneously. Prevention: mutex lock on regeneration, probabilistic early refresh, or stale-while-revalidate.

**Q: What eviction policy should a pure cache use?**
`allkeys-lru` or `allkeys-lfu`. Never `noeviction` for a cache.

**Q: What is the difference between Pub/Sub and Streams?**
Pub/Sub is fire-and-forget: no persistence, no consumer groups, no delivery guarantee. Streams are a persistent append-only log with consumer groups, acknowledgement, and replay.

**Q: What is XREADGROUP `>` vs a specific ID?**
`>` means deliver the next undelivered entries for this consumer group. A specific ID re-delivers entries from that offset (for recovery).

**Q: What does XACK do?**
Removes an entry from the pending entries list (PEL) for a consumer group, signaling successful processing.

**Q: What is pipelining?**
Batching multiple commands into a single network round-trip. It is a client-side optimization that reduces round-trips but does not provide atomicity.

**Q: What is BLPOP?**
Blocking list pop. The client blocks until an element is available or the timeout expires. Used for event-driven job queues without polling.

**Q: What does `mem_fragmentation_ratio` indicate?**
1.0-1.5 is normal. Above 1.5 indicates memory fragmentation: the allocator holds freed pages. Enable active defrag (`CONFIG SET activedefrag yes`) to recover.
