# 10. Redis Persistence: RDB, AOF, Hybrid, Recovery

## Goal

Understand how Redis persists data to disk and how to tune persistence for durability vs performance.

---

## Two Persistence Mechanisms

| Mechanism | Description |
|---|---|
| RDB (snapshot) | point-in-time snapshot written to disk at intervals |
| AOF (append-only file) | every write command appended to log file |
| Hybrid | RDB snapshot plus AOF tail for faster restart |

---

## RDB (Snapshot)

```conf
# Write snapshot if N keys changed in M seconds.
save 900 1
save 300 10
save 60 10000

# Snapshot file location.
dir /var/redis/data
dbfilename dump.rdb
```

```bash
# Manual synchronous snapshot (blocks Redis).
SAVE

# Manual background snapshot (recommended).
BGSAVE

# Check last successful save.
LASTSAVE
```

Behavior:

- fork() creates a child process to write the snapshot
- parent continues serving requests
- child writes snapshot to a temp file, then atomically renames
- data written after fork is not included in the snapshot

RDB tradeoff:

- fast restart: loads snapshot at startup
- potential data loss: events after the last snapshot are lost on crash

---

## AOF (Append-Only File)

```conf
appendonly yes
appendfilename appendonly.aof
appendfsync everysec
```

appendfsync options:

| Setting | Durability | Performance |
|---|---|---|
| `always` | highest (no data loss) | lowest (fsync on every write) |
| `everysec` | up to 1 second data loss | good (fsync once per second) |
| `no` | OS-controlled (variable loss) | highest |

```bash
# Rewrite AOF to compact it.
BGREWRITEAOF
```

AOF rewrite:

- Redis forks a child to write a minimal AOF representing current state
- avoids log growing unboundedly over time
- configure auto-rewrite threshold:

```conf
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
```

---

## Hybrid Persistence (Redis 4.0+)

```conf
aof-use-rdb-preamble yes
```

At AOF rewrite, Redis writes an RDB snapshot as the preamble, then appends new AOF commands after it. Restart reads the RDB preamble fast, then applies the AOF tail.

This gives faster restart than pure AOF with better durability than pure RDB.

---

## Data Recovery

```bash
# Recovery order on startup.
# 1. if AOF enabled, load AOF.
# 2. if AOF disabled, load RDB (dump.rdb).
# 3. if neither present, start empty.

# To recover from AOF file manually.
redis-check-aof --fix appendonly.aof

# To recover from RDB file manually.
redis-check-rdb dump.rdb
```

---

## Persistence Decision Matrix

| Use Case | Config |
|---|---|
| pure cache (no durability needed) | persistence off: `save ""`, `appendonly no` |
| cache + best-effort durability | RDB only with `save` intervals |
| tolerate up to 1 second data loss | AOF with `appendfsync everysec` |
| near-zero data loss | AOF with `appendfsync always` |
| fast restart with good durability | hybrid RDB+AOF preamble |

---

## Interview Sound Bite

RDB gives fast restarts with periodic snapshots and potential data loss proportional to the save interval. AOF logs every command with configurable fsync, trading performance for durability. Hybrid persistence combines both. In production, always tune maxmemory and choose persistence to match RPO. Disable persistence entirely for pure ephemeral caches.
