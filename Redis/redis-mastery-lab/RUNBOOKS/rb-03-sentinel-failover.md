# Runbook 03: Sentinel Failover Recovery

## When To Use

- Redis primary is unresponsive
- Sentinel has triggered failover
- Application cannot connect to Redis

## Steps

1. Check Sentinel state from any Sentinel node.

```bash
redis-cli -p 26379 SENTINEL master myredis
# Look for: flags:master (not s_down, o_down, slave)
```

2. Discover new primary address.

```bash
redis-cli -p 26379 SENTINEL get-master-addr-by-name myredis
# Returns: ["new-primary-ip", "6379"]
```

3. Verify new primary is healthy.

```bash
redis-cli -h NEW_PRIMARY_IP INFO replication
# Expected: role:master, connected_slaves:N
```

4. Verify replicas are resyncing.

```bash
redis-cli -h NEW_PRIMARY_IP INFO replication | grep slave
# Look for: slave0:ip=...,state=online,offset=...,lag=0
```

5. Verify application clients reconnected.

- Check application logs for "reconnected to Redis"
- Check hit ratio recovering: INFO stats keyspace_hits increasing

6. Check old primary (if recovered).

```bash
redis-cli -h OLD_PRIMARY_IP INFO replication
# Expected: role:slave (demoted by Sentinel)
```

7. Verify all Sentinels agree on new primary.

```bash
redis-cli -p 26379 SENTINEL sentinels myredis
redis-cli -p 26380 SENTINEL get-master-addr-by-name myredis
redis-cli -p 26381 SENTINEL get-master-addr-by-name myredis
# All should return same primary address
```

## Post-Failover

- Review data loss window: compare `master_repl_offset` vs `slave_repl_offset` before failover
- Update monitoring dashboards to new primary address
- Document incident: time from SDOWN to failover-end
