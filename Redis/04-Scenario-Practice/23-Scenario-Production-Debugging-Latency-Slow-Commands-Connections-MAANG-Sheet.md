# 23. Scenario: Production Debugging — Latency, Slow Commands, Connection Exhaustion

## Scenario

On-call pages you at 3 AM: Redis is causing application timeouts. Walk through your investigation.

---

## Step 1: Confirm Redis Is Alive

```bash
redis-cli PING
# Expected: PONG in < 1ms

redis-cli --latency -i 1
# Measures round-trip latency every second.
```

If ping times out: Redis is overloaded, network issue, or Redis is down. Escalate if down.

---

## Step 2: Check INFO For High-Level Health

```bash
redis-cli INFO all > /tmp/redis-info-$(date +%s).txt

# Critical sections:
INFO clients
# connected_clients: spike?
# blocked_clients: stuck on BLPOP/BRPOP?

INFO memory
# used_memory: near maxmemory?
# mem_fragmentation_ratio: > 1.5?

INFO stats
# rejected_connections: OOM or connection limit?
# instantaneous_ops_per_sec: spike?

INFO replication
# replication lag on primary?
```

---

## Step 3: Check SLOWLOG

```bash
redis-cli SLOWLOG GET 25
```

Look for:

- KEYS command on large keyspace
- SMEMBERS/HGETALL/LRANGE on huge collections
- SORT without a cache
- Lua scripts taking more than a few milliseconds
- XREAD/XREVRANGE on large streams without COUNT

---

## Step 4: Identify Hot Commands With commandstats

```bash
redis-cli INFO commandstats | sort -t= -k2 -rn | head -20
```

Find which commands are consuming the most CPU time.

---

## Step 5: Short-Burst MONITOR

```bash
redis-cli MONITOR
# Ctrl+C after 5-10 seconds
```

Shows every command in real time. Look for unexpected KEYS, large HGETALL, or unknown access patterns. Never leave running more than a few seconds.

---

## Step 6: Connection Exhaustion

```bash
INFO clients
# connected_clients: close to maxclients?

CONFIG GET maxclients
# Default: 10000

# Identify which clients are connected.
CLIENT LIST
```

Connection exhaustion causes:

- connection pool not releasing connections
- application threads holding connections without releasing
- leaked connections after exception paths

Mitigation:

```conf
maxclients 10000
timeout 300
tcp-keepalive 60
```

---

## Step 7: Memory Fragmentation

```bash
INFO memory
# mem_fragmentation_ratio > 2.0 is high
# used_memory_rss >> used_memory

# Enable active defrag if needed.
redis-cli CONFIG SET activedefrag yes
```

---

## Common Patterns And Fixes

| Symptom | Cause | Fix |
|---|---|---|
| SLOWLOG full of KEYS | legacy code scanning keyspace | replace KEYS with SCAN |
| blocked_clients > 0 | workers stuck on BLPOP | check if queue is empty, review consumer logic |
| used_memory near maxmemory | no TTL on keys | add TTL, tune eviction policy |
| rejected_connections | maxclients reached | increase maxclients or fix connection leak |
| high instantaneous_ops | unexpected traffic spike | check for runaway job, rate limit caller |
| latency spikes after BGSAVE | fork latency | use SSD, tune save intervals |

---

## Checklist After Incident

```text
1. Capture redis-cli INFO all snapshot.
2. Capture SLOWLOG GET 100.
3. Capture CLIENT LIST if connection count spiked.
4. Run redis-cli --bigkeys to find large key anomalies.
5. Review application changes deployed in the last 24 hours.
6. Write post-mortem with root cause and prevention.
7. Add dashboard alert: evicted_keys, used_memory, slowlog_len.
```

---

## Interview Sound Bite

Production Redis debugging follows a layered approach: PING and latency test first, then INFO for health snapshot, then SLOWLOG for command-level culprits, then commandstats for aggregate patterns, then short-burst MONITOR for live traffic. Connection exhaustion, missing TTLs, blocking commands, and KEYS on large keyspaces are the most common causes of Redis latency spikes. Always capture evidence before restarting.
