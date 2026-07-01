# 13. Redis Cluster: Hash Slots, Sharding, Resharding, Cross-Slot

## Goal

Understand how Redis Cluster distributes data across shards, handles redirects, and manages topology changes.

---

## Hash Slot Model

Redis Cluster divides keyspace into 16384 hash slots.

```text
slot = CRC16(key) mod 16384

example:
  node A: slots 0-5460
  node B: slots 5461-10922
  node C: slots 10923-16383
```

Each node owns a range of slots. Data lives on the node owning the slot for the key.

---

## Minimum Topology

Production minimum: 6 nodes (3 primaries + 3 replicas, one replica per primary).

```text
primary-A (slots 0-5460)       replica-A
primary-B (slots 5461-10922)   replica-B
primary-C (slots 10923-16383)  replica-C
```

---

## Cluster Commands

```bash
# Cluster info.
CLUSTER INFO
CLUSTER NODES

# Which slot holds a key.
CLUSTER KEYSLOT mykey

# Keys in a slot (for migration).
CLUSTER GETKEYSINSLOT 1234 10
```

---

## MOVED And ASK Redirects

When a client sends a command to the wrong node, the node returns a redirect.

```text
client -> SET user:1001 "alice" -> node A
node A: this key lives on node B slot 7392
-> MOVED 7392 192.168.1.12:6379
client: reconnect to node B and retry
```

MOVED: permanent redirect, update local slot map.
ASK: temporary redirect during slot migration, do not update slot map.

Cluster-aware clients handle these redirects automatically.

---

## Hash Tags: Co-locating Keys

Keys inside `{}` use only the bracketed part for slot assignment.

```bash
# Both hash to the same slot.
MSET {user:1001}.name alice {user:1001}.email alice@example.com

# MSET across different slots fails with CROSSSLOT error.
# MSET user:1001 alice user:2002 bob  -> CROSSSLOT error
```

Use hash tags to co-locate related keys that need to be accessed together atomically.

---

## Cross-Slot Limitations

In cluster mode, multi-key commands require all keys to be in the same slot.

| Command | Cluster behavior |
|---|---|
| MSET/MGET with different slots | CROSSSLOT error |
| SUNION/SUNIONSTORE across slots | CROSSSLOT error |
| MULTI/EXEC spanning slots | disallowed |
| Lua EVAL with keys in different slots | CROSSSLOT error |

Design key names with hash tags to avoid cross-slot operations.

---

## Resharding And Adding Nodes

```bash
# Add node to cluster.
redis-cli --cluster add-node new-host:6379 existing-host:6379

# Reshard slots from one node to another.
redis-cli --cluster reshard existing-host:6379

# Rebalance all slots.
redis-cli --cluster rebalance existing-host:6379
```

Resharding migrates slots by sending MIGRATE commands. The cluster handles MOVED/ASK redirects during migration transparently.

---

## Cluster Failure Handling

- if a primary fails, its replica is promoted automatically
- if a primary fails and has no replica, the entire slot range becomes unavailable
- partial cluster failures: cluster can still serve available slot ranges

Configure `cluster-require-full-coverage no` to serve available slots even when some primaries are down.

---

## Interview Sound Bite

Redis Cluster shards across 16384 hash slots distributed across primaries. Clients receive MOVED redirects when hitting the wrong node and update their slot map accordingly. Multi-key operations across slots are not allowed; use hash tags to co-locate related keys. Resharding migrates slots live with MOVED/ASK redirect transparency. Cluster provides both horizontal scale-out and HA through per-shard replicas.
