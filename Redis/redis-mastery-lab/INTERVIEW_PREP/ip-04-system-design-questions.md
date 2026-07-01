# Interview Prep 04: System Design Questions

## Q: Design A Rate Limiter Using Redis

Key decisions: sliding window (sorted set + Lua) for smoothness, fixed window (INCR) for simplicity. Key pattern: `rate:{action}:{userId}`. Lua ensures atomic check-and-insert. On Redis failure: fail open with circuit breaker. Memory: ZREMRANGEBYSCORE trims old entries, PEXPIRE bounds key lifetime.

## Q: Design A Global Leaderboard For 100 Million Players

Key decisions: sorted set `ZADD` and `ZINCRBY` for score updates, `ZREVRANGE` for top-N, `ZREVRANK` for player position. Multi-period: separate keys per period with TTL. Scale: Redis Cluster shards by game or region. Hot keys: read replicas for leaderboard reads. Memory: estimate `score (8B) + member_avg_len (20B)` x 100M = ~2.8 GB per leaderboard, within typical instance size.

## Q: Design A Session Management System

Key decisions: hash per session token, rolling EXPIRE on each validation, DEL on logout. Token in a signed cookie, not URL. Token rotation on login prevents fixation. With Redis Cluster, ensure session keys hash to same shard using consistent key prefix. Eviction policy: volatile-lru to evict expired sessions first under memory pressure.

## Q: Design A Job Queue With Redis

Key decisions: list RPUSH for enqueue, BLPOP for consume. RPOPLPUSH for safe in-flight tracking. Retry: re-enqueue with incremented count in JSON payload. Dead letter after N retries. Priority: separate queues, BLPOP checks high-priority queue first. Persistence: use AOF for durability; job loss without persistence if Redis crashes.

## Q: Design A Pub/Sub Notification System vs Streams

Use Pub/Sub when: fire-and-forget, all subscribers are always connected, low-criticality. Use Streams when: at-least-once delivery required, consumers may restart, replay is needed, multiple independent services. For high criticality (payment events, audit): Streams. For UI push notifications: Pub/Sub acceptable.

## Q: How Would You Migrate From Single-Node Redis To Cluster Without Downtime?

1. Start a parallel Redis Cluster.
2. Set up the single node as a replica of one Cluster primary.
3. Gradually shift read traffic to Cluster.
4. When replication is caught up, perform DNS/VIP cutover for writes.
5. Monitor MOVED errors; cluster-aware client handles them.
6. Decommission the old single node.

## Q: How Do You Handle A Redis Memory Crisis In Production?

1. Check `INFO memory` for used_memory vs maxmemory, evicted_keys, fragmentation.
2. Set `maxmemory-policy allkeys-lru` if not set.
3. Run `redis-cli --bigkeys` to find anomalous large objects.
4. SCAN + TTL check to find keys without TTL.
5. Fix application to add TTL to all cache keys.
6. Enable active defrag if fragmentation > 1.5.
7. Scale instance size if usage is legitimately high.
