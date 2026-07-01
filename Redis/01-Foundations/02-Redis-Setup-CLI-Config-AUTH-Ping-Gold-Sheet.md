# 02. Redis Setup, CLI, Config, AUTH, Ping

## Goal

Get Redis running, connect with redis-cli, verify health, set configuration, and understand the basic operational interface.

---

## Minimal Start

```bash
# Start with defaults.
redis-server

# Start with a config file.
redis-server /etc/redis/redis.conf

# Start with inline overrides.
redis-server --port 6380 --requirepass secret --loglevel verbose
```

---

## redis-cli Essentials

```bash
# Connect default.
redis-cli

# Connect to specific host/port.
redis-cli -h 127.0.0.1 -p 6380

# Authenticate at connect time.
redis-cli -h 127.0.0.1 -p 6380 -a secret

# Ping the server.
redis-cli PING

# Check server info.
redis-cli INFO server | head -20

# Monitor all commands in real time (dangerous in production).
redis-cli MONITOR
```

---

## Health Check Commands

| Command | What It Shows |
|---|---|
| `PING` | server is reachable |
| `INFO server` | version, uptime, config file |
| `INFO memory` | used_memory, maxmemory, eviction |
| `INFO replication` | role, connected replicas, replication lag |
| `INFO clients` | connected clients, blocked clients |
| `INFO stats` | commands processed, keyspace hits/misses |
| `INFO keyspace` | databases and key counts |
| `DBSIZE` | total keys in current database |

---

## AUTH And Security

```bash
# Set password in config.
requirepass your-strong-password

# Authenticate in session.
AUTH your-strong-password

# Redis 6+ ACL - create user.
ACL SETUSER appuser on >password ~app:* &* +@read +@write
ACL WHOAMI
```

Never use `requirepass` without also enabling TLS in untrusted networks.

---

## CONFIG Commands

```bash
# Read config live.
CONFIG GET maxmemory
CONFIG GET maxmemory-policy
CONFIG GET save

# Set config live (not all settings are changeable at runtime).
CONFIG SET maxmemory 1gb
CONFIG SET maxmemory-policy allkeys-lru

# Persist live changes to config file.
CONFIG REWRITE
```

---

## Database Selection

Redis has 16 databases (0-15) by default.

```bash
# Select database.
SELECT 1

# Flush current database (dangerous).
FLUSHDB

# Flush all databases (very dangerous).
FLUSHALL
```

In production, prefer using different Redis instances or key namespaces rather than relying on database indices.

---

## Debugging Tools

```bash
# Check latency.
redis-cli --latency
redis-cli --latency-history

# Introspect a key.
TYPE mykey
TTL mykey
OBJECT ENCODING mykey
MEMORY USAGE mykey

# Scan keys safely (replaces KEYS in production).
SCAN 0 MATCH "user:*" COUNT 100
```

---

## Interview Sound Bite

Production Redis setup starts with CONFIG GET/SET for memory and eviction, AUTH or ACL for access control, TLS for transport security, PING/INFO for health verification, and SCAN instead of KEYS for safe key inspection.
