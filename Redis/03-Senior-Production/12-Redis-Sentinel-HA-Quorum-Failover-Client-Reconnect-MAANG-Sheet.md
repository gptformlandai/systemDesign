# 12. Redis Sentinel: HA, Quorum, Failover, Client Reconnect

## Goal

Understand Redis Sentinel for automatic failover and how clients discover the primary after a failover event.

---

## What Sentinel Does

```text
monitors primary and replicas for liveness
detects primary failure via subjective and objective down
elects a new primary when quorum agrees on failure
reconfigures replicas and clients to point to new primary
notifies applications via Pub/Sub events
```

---

## Sentinel Topology

Minimum production deployment: three Sentinel instances.

```text
sentinel-1
sentinel-2
sentinel-3
  watching:
    primary  (redis:6379)
    replica-1 (redis-replica-1:6379)
    replica-2 (redis-replica-2:6379)
```

Three Sentinels allows quorum of 2, tolerating one Sentinel failure.

---

## Sentinel Configuration

```conf
port 26379
sentinel monitor myredis 192.168.1.10 6379 2
sentinel down-after-milliseconds myredis 5000
sentinel failover-timeout myredis 60000
sentinel parallel-syncs myredis 1
```

- `monitor myredis <ip> <port> <quorum>`: quorum is minimum agreeing Sentinels to trigger failover
- `down-after-milliseconds`: how long before primary is considered down
- `parallel-syncs`: how many replicas sync to new primary simultaneously

---

## Failover Steps

```text
1. Sentinel marks primary as subjectively down (SDOWN).
2. Sentinel asks peers: is primary down from your view?
3. When quorum Sentinels agree -> objectively down (ODOWN).
4. Sentinel leader election (Raft-like).
5. Leader promotes best replica to primary.
6. Other replicas reconfigured to replicate from new primary.
7. Clients notified via Pub/Sub on sentinel channel.
8. Old primary (if it recovers) becomes a replica.
```

---

## Client Discovery

Clients must ask Sentinel for the current primary address, not connect directly.

```bash
# Discover primary.
SENTINEL get-master-addr-by-name myredis
# Returns: ["192.168.1.10", "6379"]

# List replicas.
SENTINEL replicas myredis

# List Sentinels.
SENTINEL sentinels myredis

# Check primary status.
SENTINEL master myredis
```

Client libraries (Jedis, LettuCe, redis-py) have built-in Sentinel support. Always use the Sentinel-aware client mode rather than hard-coding the primary address.

---

## Sentinel Pub/Sub Events

```bash
# Subscribe to Sentinel event channel.
SUBSCRIBE +failover-start +failover-end +role-change +odown
```

Applications can subscribe to these events to react to topology changes.

---

## Sentinel vs Cluster

| Feature | Sentinel | Cluster |
|---|---|---|
| HA (automatic failover) | yes | yes |
| horizontal scaling | no | yes |
| data partitioning | no | yes |
| client complexity | medium | high |
| minimum nodes | 1 primary + 2 Sentinels | 6 (3 primary + 3 replica) |
| use case | single-shard HA | large dataset + scale-out |

---

## Interview Sound Bite

Redis Sentinel provides high availability for a single-shard Redis deployment. Three Sentinels are the minimum for quorum-based failover. After failover, clients must re-discover the new primary via Sentinel rather than using a cached address. For horizontal scale-out beyond a single primary, use Redis Cluster.
