# 15. Redis Observability: INFO, SLOWLOG, MONITOR, Keyspace Events, Latency

## Goal

Diagnose Redis health and performance issues using built-in observability tools.

---

## INFO Sections

```bash
INFO all        # everything
INFO server     # version, uptime, config file
INFO clients    # connected_clients, blocked_clients
INFO memory     # used_memory, mem_fragmentation_ratio, maxmemory
INFO stats      # keyspace_hits, keyspace_misses, total_commands_processed
INFO replication # role, master_repl_offset, lag
INFO keyspace   # per-db key count and TTL stats
INFO latencystats # percentile latency per command (Redis 7+)
INFO commandstats # per-command call count, CPU time
```

Key metrics to monitor:

| Metric | Alert Threshold |
|---|---|
| `used_memory` | approaching `maxmemory` |
| `mem_fragmentation_ratio` | above 1.5 indicates fragmentation |
| `keyspace_hits / (hits + misses)` | below 0.9 for caches |
| `connected_clients` | unexpected spike |
| `blocked_clients` | >0 with unexpected BLPOP waits |
| `replication lag` | growing continuously |
| `evicted_keys` | non-zero and increasing |

---

## SLOWLOG

```bash
# Get last 25 slow commands.
SLOWLOG GET 25

# Number of slow entries.
SLOWLOG LEN

# Reset log.
SLOWLOG RESET
```

```conf
# In redis.conf.
slowlog-log-slower-than 10000   # microseconds (10ms)
slowlog-max-len 512
```

SLOWLOG captures commands exceeding the threshold. Common culprits: KEYS on large keyspace, SMEMBERS/HGETALL/LRANGE on large collections, SORT without caching.

---

## MONITOR (Use Sparingly)

```bash
# Stream every command processed by Redis in real time.
MONITOR
```

MONITOR is a debugging tool only. It prints every command to the client, which doubles Redis CPU under load. Never leave MONITOR running in production.

Use for: short burst debugging to see what is being written or read.

---

## Keyspace Notifications

```bash
# Enable keyspace events.
CONFIG SET notify-keyspace-events KEA

# Event types.
# K = keyspace events
# E = keyevent events
# g = generic commands (DEL, EXPIRE, RENAME)
# $ = string commands
# l = list commands
# z = sorted set commands
# x = expired events
# e = evicted events
# A = all events (g$lszxedt)
```

Subscribe to specific events:

```bash
# Get notified when any key expires.
PSUBSCRIBE __keyevent@0__:expired

# Get notified on all operations on a specific key pattern.
PSUBSCRIBE __keyspace@0__:orders:*
```

---

## LATENCY: Per-Command Latency History

```bash
# List latency event names.
LATENCY LATEST

# Show history of a latency event.
LATENCY HISTORY event

# Reset latency data.
LATENCY RESET
```

Configure minimum threshold:

```conf
latency-monitor-threshold 100   # milliseconds
```

---

## MEMORY Diagnostics

```bash
# Memory usage for a specific key.
MEMORY USAGE user:1001

# Memory doctor: analysis and recommendation.
MEMORY DOCTOR

# Purge freed memory back to OS.
MEMORY PURGE
```

---

## Interview Sound Bite

Redis observability starts with INFO sections for a health snapshot. SLOWLOG identifies slow commands without live impact. MONITOR is used for short-burst tracing only. Keyspace notifications enable reactive cache invalidation and TTL monitoring. LATENCY HISTORY tracks command-level latency spikes. MEMORY USAGE and mem_fragmentation_ratio guide memory tuning decisions.
