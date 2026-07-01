# Lab 09: Observability — INFO, SLOWLOG, MONITOR

## Objective

Navigate Redis observability tools and extract meaningful insights.

## Exercises

### Exercise 1: INFO Sections

```bash
# Server info.
INFO server

# Memory info.
INFO memory
# Note: used_memory, mem_fragmentation_ratio, maxmemory_policy

# Stats.
INFO stats
# Note: keyspace_hits, keyspace_misses, evicted_keys

# Replication.
INFO replication

# Keyspace.
INFO keyspace
```

### Exercise 2: Calculate Hit Ratio

```bash
INFO stats | grep keyspace
# keyspace_hits:N
# keyspace_misses:M
# hit_ratio = N / (N + M)
```

### Exercise 3: SLOWLOG Configuration

```bash
CONFIG SET slowlog-log-slower-than 1   # 1 microsecond to capture everything in lab
CONFIG SET slowlog-max-len 100

# Generate some commands.
for i in $(seq 1 50); do redis-cli SET "slowlog:key:$i" "value"; done

SLOWLOG GET 10
SLOWLOG LEN
```

### Exercise 4: MONITOR Short Burst

```bash
# Terminal 1: start monitor.
redis-cli MONITOR

# Terminal 2: run some commands.
redis-cli SET monitor:test "value"
redis-cli GET monitor:test

# Terminal 1: see both commands appear in real time.
# Ctrl+C to stop MONITOR.
```

### Exercise 5: CLIENT LIST

```bash
CLIENT LIST
# Shows all connected clients: addr, fd, name, age, idle, cmd
```

### Exercise 6: MEMORY Diagnostics

```bash
# Memory analysis for a key.
SET sample:key "some test value here"
MEMORY USAGE sample:key

# Doctor report.
MEMORY DOCTOR

# Fragmentation.
INFO memory | grep fragmentation
```

### Exercise 7: LATENCY Monitoring

```bash
CONFIG SET latency-monitor-threshold 1   # 1ms for lab

# Trigger a long operation (DEBUG SLEEP in lab only).
DEBUG SLEEP 0.01

LATENCY LATEST
LATENCY HISTORY command
LATENCY RESET
```

### Exercise 8: commandstats

```bash
INFO commandstats | grep -E "^cmdstat_(get|set|hset|zadd)" | head -10
# Shows call count and CPU time per command.
```

## Reflection

- What does `mem_fragmentation_ratio: 0.8` mean?
- When should you NOT use MONITOR in production?
- What does a high `keyspace_misses` count indicate for a cache?
