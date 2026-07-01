# Lab 06: Persistence Configuration

## Objective

Understand the impact of RDB, AOF, and hybrid persistence configurations.

## Prerequisites

Access to redis.conf or CONFIG SET/GET. Redis running locally.

## Exercises

### Exercise 1: Inspect Current Persistence Config

```bash
CONFIG GET save
CONFIG GET appendonly
CONFIG GET appendfilename
CONFIG GET appendfsync
CONFIG GET aof-use-rdb-preamble
CONFIG GET dir
CONFIG GET dbfilename
```

### Exercise 2: Manual Snapshot

```bash
# Trigger background save.
BGSAVE
# Expected: Background saving started

# Check last save time.
LASTSAVE
# Expected: Unix timestamp

# Wait for save to complete.
INFO persistence
# Look for: rdb_bgsave_in_progress:0
```

### Exercise 3: Enable AOF At Runtime

```bash
CONFIG SET appendonly yes
CONFIG GET appendonly
# Expected: yes

# Verify AOF rewrite status.
INFO persistence
# Look for: aof_enabled:1
```

### Exercise 4: Trigger AOF Rewrite

```bash
BGREWRITEAOF
# Expected: Background append only file rewriting started

INFO persistence
# Look for: aof_rewrite_in_progress:1 then 0
```

### Exercise 5: Hybrid Persistence

```bash
CONFIG SET aof-use-rdb-preamble yes
CONFIG GET aof-use-rdb-preamble
# Expected: yes

# Trigger rewrite to create hybrid preamble.
BGREWRITEAOF
```

### Exercise 6: Persist Config To File

```bash
# Persist current runtime config to redis.conf.
CONFIG REWRITE
# Expected: OK
# Note: redis.conf must be specified at startup for this to work.
```

### Exercise 7: Disable Persistence (Cache Mode)

```bash
# Disable RDB snapshots.
CONFIG SET save ""

# Disable AOF.
CONFIG SET appendonly no

INFO persistence
# Expected: rdb_last_bgsave_status: ok, aof_enabled:0
```

## Reflection

- What data could be lost with RDB and save interval of 60 seconds?
- What is the tradeoff between `appendfsync always` and `appendfsync everysec`?
- When should persistence be disabled entirely?
