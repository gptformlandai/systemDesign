# Cheatsheet 04: Admin And Observability Commands

## INFO Sections

```bash
INFO server       # version, uptime, config file
INFO clients      # connected_clients, blocked_clients
INFO memory       # used_memory, fragmentation, maxmemory
INFO stats        # keyspace_hits, misses, evicted_keys, ops/sec
INFO replication  # role, offset, lag, connected_slaves
INFO keyspace     # per-db key count, expires
INFO persistence  # rdb/aof status
INFO commandstats # per-command call count and CPU
INFO latencystats # percentile latency (Redis 7+)
INFO all          # everything
```

## SLOWLOG

```bash
SLOWLOG GET [n]       # get last n slow commands
SLOWLOG LEN           # count of entries
SLOWLOG RESET         # clear log

CONFIG SET slowlog-log-slower-than 10000   # threshold in microseconds
CONFIG SET slowlog-max-len 512
```

## CLIENT Commands

```bash
CLIENT LIST                    # list all clients
CLIENT KILL ID <id>            # kill a client
CLIENT GETNAME                 # current connection name
CLIENT SETNAME name            # set connection name
CLIENT ID                      # current client ID
```

## LATENCY

```bash
LATENCY LATEST             # all latency events
LATENCY HISTORY event      # history of one event
LATENCY RESET [event]      # reset latency data

CONFIG SET latency-monitor-threshold 100   # ms threshold
```

## CONFIG

```bash
CONFIG GET pattern
CONFIG SET parameter value
CONFIG REWRITE               # persist runtime config to file
CONFIG RESETSTAT             # reset INFO stats counters
```

## BGSAVE And BGREWRITEAOF

```bash
BGSAVE                       # background RDB snapshot
BGREWRITEAOF                 # background AOF rewrite
LASTSAVE                     # unix timestamp of last successful BGSAVE
```

## DEBUG (Lab Only)

```bash
DEBUG SLEEP n                # sleep n seconds (blocks Redis)
DEBUG JMAP                   # memory map
DEBUG RELOAD                 # reload dataset from disk
```

## FLUSHDB / FLUSHALL (Destructive)

```bash
FLUSHDB [ASYNC]              # delete all keys in current db
FLUSHALL [ASYNC]             # delete all keys in all databases
```
