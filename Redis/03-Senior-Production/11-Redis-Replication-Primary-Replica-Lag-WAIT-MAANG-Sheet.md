# 11. Redis Replication: Primary/Replica, Lag, WAIT, PSYNC

## Goal

Understand how Redis async replication works, how to measure replica lag, and how to enforce consistency guarantees.

---

## Replication Topology

```text
primary (read+write)
  |-- replica-1 (read-only by default)
  |-- replica-2 (read-only by default)
       |-- replica-3 (chained/cascaded)
```

Replicas receive the primary's write stream asynchronously. There is no synchronous replication by default.

---

## Setting Up Replication

```bash
# On replica at startup.
REPLICAOF primary-host 6379

# Promote replica to standalone (stop replication).
REPLICAOF NO ONE

# Read-only replica (default true, keep it).
CONFIG SET replica-read-only yes
```

---

## Replication Mechanics

1. On first connection, replica requests PSYNC (partial sync).
2. Primary checks its replication backlog for the replica's offset.
3. If offset is within backlog: sends only the missing commands (partial resync).
4. If offset is outside backlog (too far behind): full resync via BGSAVE + RDB transfer.

```bash
# On replica: check replication offset and lag.
INFO replication
# master_repl_offset
# slave_repl_offset
# lag
```

---

## WAIT: Synchronous Write Confirmation

```bash
# Wait for at least 1 replica to confirm write, up to 1000ms.
SET user:1001:email "alice@example.com"
WAIT 1 1000
# Returns number of replicas that confirmed before timeout.
```

WAIT does not guarantee durability: the replica may still crash before persisting. But it makes replication lag visible and enforced.

Use WAIT when:

- a write that must be visible to another service immediately
- testing replica lag bounds under load
- conditional cross-service consistency (best effort)

---

## Replication Backlog

```conf
# Size of the circular replication backlog.
repl-backlog-size 1mb
```

If a replica reconnects after being offline longer than the backlog can cover, it triggers a full resync, which is expensive. Increase backlog size for high-throughput primaries or replicas that may have short-lived disconnects.

---

## Replication Lag Scenarios

| Scenario | Impact |
|---|---|
| network latency between nodes | increased lag |
| large write throughput | replica falls behind |
| disk-based RDB snapshot on primary | replica waits for BGSAVE |
| slow replica disk | replica applies slower than primary writes |
| many replicas | each competes for primary bandwidth |

Mitigation: monitor `replication_offset - slave_repl_offset`, set alert thresholds, use WAIT on critical writes.

---

## Interview Sound Bite

Redis replication is asynchronous by default, which means the primary can acknowledge a write before any replica has recorded it. PSYNC enables partial resync from the replication backlog on reconnect. The WAIT command adds synchronous confirmation up to N replicas with a timeout, but it is not a hard durability guarantee. Replica lag is monitored via INFO replication and backlog tuning prevents full-resync storms.
