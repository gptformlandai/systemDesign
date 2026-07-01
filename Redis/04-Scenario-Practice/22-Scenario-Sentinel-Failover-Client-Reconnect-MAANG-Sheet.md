# 22. Scenario: Sentinel Failover And Client Reconnect

## Scenario

Your Redis primary goes down at 2 AM. Walk through exactly what happens, what the application experiences, and how clients recover.

---

## Timeline Of A Sentinel Failover

```text
T+0s:  Primary stops responding.
T+5s:  Sentinel-1 marks primary as SDOWN (subjectively down).
       Sentinel-2 and Sentinel-3 also mark SDOWN independently.
T+5s:  Sentinel-1 broadcasts: "I think primary is ODOWN (objectively down)."
       Quorum = 2 of 3 Sentinels agree -> ODOWN confirmed.
T+6s:  Sentinel leader election (Raft-style).
T+7s:  Sentinel-1 elected leader.
T+8s:  Leader evaluates replicas: priority, offset, run ID.
       Chooses replica-1 (best offset).
T+9s:  SLAVEOF NO ONE sent to replica-1 -> promoted to primary.
T+10s: Replica-2 reconfigured to replicate from new primary.
T+10s: Sentinel publishes +failover-end event on Pub/Sub.
T+12s: Old primary (if it recovers) reconfigured as replica.
```

Total time: ~10-30 seconds depending on `down-after-milliseconds` setting.

---

## What The Application Experiences

```text
- Writes to old primary -> ECONNREFUSED or timeout for ~10-30s.
- If connection pool retries, reconnects may go to old primary until client re-queries Sentinel.
- If client is Sentinel-aware, it queries Sentinel for new primary address and reconnects.
- Reads on replicas: unaffected unless replica was promoted.
```

---

## Sentinel Client Reconnect Protocol

Sentinel-aware clients (Jedis, Lettuce, redis-py):

```text
1. Client has list of Sentinel addresses (not primary addresses).
2. On connection failure, client queries each Sentinel:
   SENTINEL get-master-addr-by-name myredis
3. First Sentinel that responds returns new primary address.
4. Client reconnects to new primary.
5. Client caches new primary address in local slot map.
```

---

## Configuration For Fast Failover

```conf
# In sentinel.conf.
sentinel down-after-milliseconds myredis 5000    # 5s before SDOWN
sentinel failover-timeout myredis 30000          # max time for failover
sentinel parallel-syncs myredis 1                # sync one replica at a time to new primary
```

```conf
# In redis.conf on replicas.
replica-lazy-flush yes      # async flush on full resync
```

---

## Avoiding Data Loss During Failover

```bash
# Require minimum replicas before accepting writes on primary.
CONFIG SET min-replicas-to-write 1
CONFIG SET min-replicas-max-lag 10
```

If primary cannot reach at least 1 replica within 10 seconds, it stops accepting writes. This prevents writes to an isolated primary that will be deposed.

---

## Partial Data Loss Is Still Possible

```text
Scenario:
1. Primary receives 500 writes.
2. Replica has replicated 490 of them.
3. Primary crashes.
4. Sentinel promotes replica (with 490 writes).
5. 10 writes are permanently lost.
```

This is the cost of asynchronous replication. If zero-loss is required, use synchronous replication systems or WAIT on critical writes.

---

## Post-Failover Health Check

```bash
# Check Sentinel state.
redis-cli -p 26379 SENTINEL master myredis
redis-cli -p 26379 SENTINEL replicas myredis
redis-cli -p 26379 SENTINEL sentinels myredis

# Check new primary.
redis-cli -h new-primary-host INFO replication

# Check replica is syncing.
INFO replication | grep master_link_status
# master_link_status:up
```

---

## Interview Sound Bite

Redis Sentinel failover takes 10-30 seconds depending on `down-after-milliseconds`. Sentinel leader election is Raft-like and requires quorum. Clients must be Sentinel-aware to reconnect to the new primary; hard-coded primary addresses cause extended outages. The last 10-30 seconds of writes before failover may be lost due to async replication. Use `min-replicas-to-write` and `min-replicas-max-lag` to prevent isolated-primary write loss.
