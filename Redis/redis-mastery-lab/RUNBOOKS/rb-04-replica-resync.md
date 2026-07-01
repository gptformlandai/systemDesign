# Runbook 04: Replica Full Resync

## Symptoms

- High network bandwidth spike between primary and replica
- INFO replication shows `master_link_status:down` then `up`
- Primary logs show BGSAVE triggered for replica sync
- Application experiences latency during BGSAVE fork

## When Full Resync Occurs

- Replica reconnects after being offline for too long
- Replica offset is outside the replication backlog
- First connection of a new replica

## Diagnosis

1. Check replica sync state.

```bash
redis-cli -h REPLICA_HOST INFO replication
# loading:1 during initial load
# master_sync_in_progress:1 during RDB transfer
# master_link_status:down while disconnected
```

2. Check primary backlog size.

```bash
redis-cli -h PRIMARY_HOST CONFIG GET repl-backlog-size
redis-cli -h PRIMARY_HOST INFO replication | grep master_repl_offset
```

3. Measure bandwidth.

```bash
redis-cli -h PRIMARY_HOST INFO stats | grep total_net_output_bytes
```

## Resolution: Prevent Future Full Resync

1. Increase replication backlog to cover typical outage duration.

```bash
redis-cli -h PRIMARY_HOST CONFIG SET repl-backlog-size 100mb
redis-cli -h PRIMARY_HOST CONFIG REWRITE
```

2. Keep replica lag low: ensure replica hardware matches primary I/O.

3. Monitor replica lag continuously.

```bash
redis-cli -h PRIMARY_HOST INFO replication | grep -E "lag|offset"
```

## Recovery

If full resync is in progress, wait for it to complete. Do not restart replica mid-sync. Monitor with `INFO replication | grep master_sync_in_progress` until it shows 0.
