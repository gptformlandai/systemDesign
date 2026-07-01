# Lab 07: Replication Setup And Inspection

## Objective

Set up a primary-replica pair locally and inspect replication state.

## Setup (Docker)

```bash
# Start primary on 6379.
docker run -d --name redis-primary -p 6379:6379 redis:7-alpine

# Start replica on 6380, pointing to primary.
docker run -d --name redis-replica -p 6380:6379 redis:7-alpine redis-server --replicaof host.docker.internal 6379
```

## Exercises

### Exercise 1: Verify Replication

```bash
# On primary.
redis-cli -p 6379 INFO replication
# Look for: role:master, connected_slaves:1

# On replica.
redis-cli -p 6380 INFO replication
# Look for: role:slave, master_link_status:up
```

### Exercise 2: Write On Primary, Read On Replica

```bash
redis-cli -p 6379 SET replicated:key "hello from primary"
redis-cli -p 6380 GET replicated:key
# Expected: "hello from primary"
```

### Exercise 3: Replica Is Read-Only

```bash
redis-cli -p 6380 SET some:key "try to write"
# Expected: READONLY You can't write against a read only replica.
```

### Exercise 4: Measure Replication Offset

```bash
redis-cli -p 6379 INFO replication | grep master_repl_offset
redis-cli -p 6380 INFO replication | grep slave_repl_offset
# Offsets should match when replica is fully synced.
```

### Exercise 5: WAIT Command

```bash
# Write to primary, then wait for replica confirmation.
redis-cli -p 6379 SET important:key "must be replicated"
redis-cli -p 6379 WAIT 1 1000
# Expected: 1 (one replica confirmed within 1000ms)
```

### Exercise 6: Promote Replica

```bash
redis-cli -p 6380 REPLICAOF NO ONE
redis-cli -p 6380 INFO replication
# Expected: role:master
```

## Reflection

- What does `master_link_status:down` indicate?
- What is PSYNC and when does it fall back to full resync?
- What does `min-replicas-to-write` protect against?
