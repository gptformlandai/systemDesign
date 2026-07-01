# Cheatsheet 05: Cluster And Sentinel Commands

## Sentinel Commands

```bash
# Connect to Sentinel.
redis-cli -p 26379

# Discover primary.
SENTINEL get-master-addr-by-name myredis

# List replicas.
SENTINEL replicas myredis

# List Sentinels.
SENTINEL sentinels myredis

# Primary status.
SENTINEL master myredis

# Subscribe to Sentinel events.
SUBSCRIBE +failover-start +failover-end +role-change +odown +sdown
```

## Replication Commands

```bash
# Configure replica (on replica).
REPLICAOF primary-host 6379

# Promote to primary.
REPLICAOF NO ONE

# Wait for replica confirmation.
WAIT num-replicas timeout-ms

# Replication info.
INFO replication
```

## Cluster Commands

```bash
# Cluster state.
CLUSTER INFO
CLUSTER NODES

# Slot assignment.
CLUSTER KEYSLOT key

# Keys in a slot.
CLUSTER GETKEYSINSLOT slot count

# Meet new node.
CLUSTER MEET ip port

# Add node via redis-cli.
redis-cli --cluster add-node new-host:6379 existing-host:6379

# Reshard slots.
redis-cli --cluster reshard existing-host:6379

# Rebalance.
redis-cli --cluster rebalance existing-host:6379

# Check cluster health.
redis-cli --cluster check existing-host:6379
```

## Hash Tag For Co-location

```bash
# Keys {user}:1001 and {user}:1002 hash to same slot.
MSET {user}.1001 alice {user}.1002 bob

# Verify slot.
CLUSTER KEYSLOT "{user}.1001"
CLUSTER KEYSLOT "{user}.1002"
```

## WAIT Command

```bash
# Require at least 1 replica to confirm within 1s.
WAIT 1 1000
# Returns count of confirming replicas.
```
