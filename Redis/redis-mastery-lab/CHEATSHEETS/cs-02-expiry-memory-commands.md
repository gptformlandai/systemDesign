# Cheatsheet 02: Expiry And Memory Commands

## Expiry Commands

| Command | Syntax | Returns |
|---|---|---|
| EXPIRE | `EXPIRE key seconds` | 1 if set, 0 if key missing |
| PEXPIRE | `PEXPIRE key ms` | millisecond precision |
| EXPIREAT | `EXPIREAT key unix-ts` | absolute unix timestamp |
| TTL | `TTL key` | seconds remaining; -1 no TTL; -2 missing |
| PTTL | `PTTL key` | milliseconds remaining |
| PERSIST | `PERSIST key` | removes TTL from key |
| EXPIRETIME | `EXPIRETIME key` | absolute expiry timestamp (Redis 7+) |

## TTL Return Values

| Value | Meaning |
|---|---|
| >= 0 | seconds remaining |
| -1 | key exists but has no TTL |
| -2 | key does not exist |

## Memory Commands

| Command | Syntax | Notes |
|---|---|---|
| MEMORY USAGE | `MEMORY USAGE key [SAMPLES n]` | bytes used by key |
| MEMORY DOCTOR | `MEMORY DOCTOR` | analysis and advice |
| MEMORY PURGE | `MEMORY PURGE` | release freed memory to OS |
| MEMORY STATS | `MEMORY STATS` | detailed memory breakdown |

## Eviction Policies

| Policy | Behavior |
|---|---|
| noeviction | error on OOM (default) |
| allkeys-lru | LRU across all keys |
| volatile-lru | LRU on keys with TTL only |
| allkeys-lfu | LFU across all keys |
| volatile-lfu | LFU on keys with TTL only |
| allkeys-random | random key |
| volatile-random | random key from TTL keys |
| volatile-ttl | nearest-expiry key |

## Key Config

```bash
CONFIG SET maxmemory 4gb
CONFIG SET maxmemory-policy allkeys-lru
CONFIG GET maxmemory
CONFIG GET maxmemory-policy
```

## Key Scanning (Never KEYS in production)

```bash
SCAN cursor [MATCH pattern] [COUNT n] [TYPE type]
HSCAN key cursor [MATCH pattern] [COUNT n]
SSCAN key cursor [MATCH pattern] [COUNT n]
ZSCAN key cursor [MATCH pattern] [COUNT n]
```

## Atomic Get Variants (Redis 6.2+)

| Command | Syntax | Notes |
|---|---|---|
| GETDEL | `GETDEL key` | get and delete atomically |
| GETEX | `GETEX key [EX s\|PX ms\|EXAT ts\|PERSIST]` | get and set/remove TTL atomically |
| COPY | `COPY src dest [DB n] [REPLACE]` | copy key without deleting source |

## Diagnostic CLI Flags

```bash
redis-cli --bigkeys          # top largest keys by type
redis-cli --hotkeys          # most-accessed keys (requires LFU policy)
redis-cli --memkeys          # memory usage of keys
redis-cli --stat             # live stats dashboard (1s refresh)
redis-cli --latency          # continuous round-trip latency
redis-cli --latency-history -i 5   # per-interval latency percentiles
redis-cli --latency-dist     # latency distribution chart
```
