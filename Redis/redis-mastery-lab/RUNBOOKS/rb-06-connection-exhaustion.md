# Runbook 06: Connection Exhaustion

## Symptoms

- Application cannot connect to Redis
- `rejected_connections` in INFO stats is growing
- `connected_clients` is at or near `maxclients`

## Diagnosis

1. Check client count.

```bash
redis-cli INFO clients
# connected_clients: close to maxclients?
# rejected_connections: increasing?
```

2. Check maxclients limit.

```bash
redis-cli CONFIG GET maxclients
# Default: 10000
```

3. Identify connected clients.

```bash
redis-cli CLIENT LIST
# Columns: addr, fd, name, age, idle, cmd, db, flags
# Look for: old age/idle connections not releasing
# Look for: many connections from same IP
```

4. Find idle connections.

```bash
redis-cli CLIENT LIST | awk -F'[ =]' '{for(i=1;i<=NF;i++) if($i=="idle") print $(i+1), $0}' | sort -rn | head -20
```

5. Kill idle connections if needed.

```bash
redis-cli CLIENT KILL ID <id>
```

## Root Causes

| Cause | Fix |
|---|---|
| Connection pool not releasing | Check pool configuration: max_idle, max_active, test_on_borrow |
| Exception path leaking connections | Add finally block to always release connections |
| Long-held SUBSCRIBE connections | Use dedicated connection for Pub/Sub |
| Too-small pool causing queue buildup | Increase pool size within maxclients budget |

## Configuration Fixes

```bash
# Increase maxclients.
redis-cli CONFIG SET maxclients 20000

# Set idle connection timeout.
redis-cli CONFIG SET timeout 300

# Enable TCP keepalive.
redis-cli CONFIG SET tcp-keepalive 60

redis-cli CONFIG REWRITE
```

## Resolution Criteria

- `rejected_connections` stops growing
- `connected_clients` returns to baseline
- Application connections succeed
