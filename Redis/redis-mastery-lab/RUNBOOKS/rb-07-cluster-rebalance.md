# Runbook 07: Cluster Rebalance And Node Addition

## When To Use

- Adding a new node to an existing Redis Cluster
- Rebalancing slots after node addition
- Redistributing slots after node removal

## Prerequisites

- redis-cli 7.x with --cluster flag
- Access to all cluster nodes
- Maintenance window or blue-green deployment if risky

## Steps: Add A Node

1. Start the new Redis instance.

```bash
# On new node.
redis-server --cluster-enabled yes --cluster-config-file nodes.conf --port 6380
```

2. Add to cluster.

```bash
redis-cli --cluster add-node NEW_HOST:6380 EXISTING_HOST:6379
```

3. If adding as replica of a specific primary.

```bash
redis-cli --cluster add-node NEW_HOST:6380 EXISTING_HOST:6379 --cluster-slave --cluster-master-id <primary-node-id>
```

## Steps: Rebalance Slots

1. Check current distribution.

```bash
redis-cli --cluster check EXISTING_HOST:6379
```

2. Reshard slots interactively.

```bash
redis-cli --cluster reshard EXISTING_HOST:6379
# Follow prompts: slots to move, destination node ID, source nodes
```

3. Rebalance automatically.

```bash
redis-cli --cluster rebalance EXISTING_HOST:6379 --cluster-threshold 1
```

4. Verify after rebalance.

```bash
redis-cli --cluster check EXISTING_HOST:6379
redis-cli CLUSTER INFO | grep cluster_state
# Expected: cluster_state:ok
```

## During Rebalance

- MOVED and ASK redirects increase temporarily. This is expected.
- Monitor: `redis-cli CLUSTER INFO | grep cluster_slots_assigned`
- Do not remove nodes mid-rebalance.

## Remove A Node

1. Move all slots off the node.

```bash
redis-cli --cluster reshard EXISTING_HOST:6379
# Source: node to remove
# Destination: other nodes
```

2. Remove the empty node.

```bash
redis-cli --cluster del-node EXISTING_HOST:6379 <node-id>
```
