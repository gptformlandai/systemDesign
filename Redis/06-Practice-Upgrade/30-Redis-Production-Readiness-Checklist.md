# 30. Redis Production Readiness Checklist

## Before Going To Production

This checklist gates production readiness. Every item has a rationale.

---

## Memory Configuration

- [ ] `maxmemory` is set (never leave at 0 in production)
- [ ] `maxmemory-policy` is set to `allkeys-lru` for pure cache, or `volatile-lru` for mixed
- [ ] All cache keys have TTL set in application code
- [ ] Long-lived keys are tracked and justified
- [ ] `MEMORY DOCTOR` output reviewed
- [ ] `mem_fragmentation_ratio` baseline established

---

## Persistence Configuration

- [ ] Persistence choice documented: RDB, AOF, hybrid, or off
- [ ] If cache only: `save ""` and `appendonly no` configured
- [ ] If durability required: AOF with `appendfsync everysec` minimum
- [ ] `BGSAVE` tested and latency measured
- [ ] Recovery procedure documented and tested

---

## Security

- [ ] `requirepass` or ACL users configured
- [ ] ACL users have least-privilege: command category + key pattern restrictions
- [ ] `FLUSHALL`, `DEBUG`, `KEYS` disabled or renamed
- [ ] `bind` directive limits to private network interface
- [ ] TLS configured if Redis is not on same host as application
- [ ] `protected-mode yes` confirmed

---

## High Availability

- [ ] Replication configured: at least one replica per primary
- [ ] Sentinel deployment: minimum 3 Sentinels with quorum 2
- [ ] Or: Cluster mode with minimum 6 nodes (3 primaries + 3 replicas)
- [ ] Client libraries configured for Sentinel-aware discovery
- [ ] `min-replicas-to-write` and `min-replicas-max-lag` set on primary
- [ ] Failover tested in staging

---

## Connection Management

- [ ] `maxclients` configured and understood
- [ ] Connection pool sized correctly: not too small (queue build-up), not too large (maxclients breach)
- [ ] `timeout` set to release idle connections
- [ ] `tcp-keepalive` configured for connection health

---

## Observability

- [ ] INFO metrics exported to monitoring (Prometheus Redis exporter or similar)
- [ ] Alerts configured:
  - `used_memory` approaching `maxmemory`
  - `evicted_keys` > 0 and growing
  - `rejected_connections` > 0
  - `replication lag` growing
  - `blocked_clients` unexpected spike
- [ ] `SLOWLOG` reviewed after load testing
- [ ] `latency-monitor-threshold` configured
- [ ] LATENCY HISTORY baseline captured

---

## Application Code Audit

- [ ] No `KEYS` in application code (replaced with SCAN)
- [ ] No `SMEMBERS`/`HGETALL`/`LRANGE` without result-size bound
- [ ] All MULTI/EXEC blocks checked for runtime error handling
- [ ] Lua scripts tested for correctness and timing under load
- [ ] Connection error handling: retry with backoff, circuit breaker for Redis down
- [ ] No hardcoded primary address (Sentinel-aware client discovery)
- [ ] Redis client version supports the server version and required protocol/features
- [ ] Reconnect/failover behavior tested under load
- [ ] Local/near-cache is bounded, TTL-based, invalidated, and flushed on reconnect

---

## Cluster-Specific (If Using Cluster)

- [ ] All multi-key operations use hash tags for co-location
- [ ] No SELECT calls (Cluster only supports db 0)
- [ ] Client handles MOVED and ASK redirects
- [ ] Slot migration tested
- [ ] `cluster-require-full-coverage` set appropriately
- [ ] Functions/Lua scripts pass all keys explicitly and stay within one slot

---

## Modern Redis Features (If Using JSON/Search/Vector/TimeSeries)

- [ ] Server/provider supports required commands (`COMMAND INFO` verified)
- [ ] Client library supports required command surface and protocol mode
- [ ] Indexed fields are tied to known access patterns
- [ ] Index memory overhead estimated
- [ ] Vector memory estimated: count x dimensions x bytes plus index overhead
- [ ] Embedding model versioning and rebuild plan documented
- [ ] Time-series retention and rollup policy configured
- [ ] Probabilistic error bounds documented and not used as final authority for critical decisions
- [ ] Query limits, pagination, and broad-query guardrails implemented

---

## Redis Functions (If Using FCALL)

- [ ] Function source stored in git
- [ ] Function libraries versioned
- [ ] Staging load and latency test completed
- [ ] Old function version kept through rollback window
- [ ] `FUNCTION LOAD`/`FUNCTION DELETE` limited to deploy/admin role
- [ ] `FCALL` latency and errors monitored
- [ ] Functions avoid unbounded loops and cross-slot key access

---

## Capacity Planning

- [ ] Memory estimate documented: key count x avg key size x growth rate
- [ ] Write throughput measured against Redis throughput limits
- [ ] Replication bandwidth measured
- [ ] Backup and restore time tested
- [ ] Provider/node reserved memory, fork overhead, replication backlog, and network limits reviewed

---

## Managed Redis (If Using Cloud Provider)

- [ ] Cache vs durable source-of-truth role explicitly documented
- [ ] Private subnet/network placement confirmed
- [ ] TLS/auth/ACL configured according to provider support
- [ ] Backup schedule and restore test completed
- [ ] Failover event tested with real application clients
- [ ] Upgrade process and rollback process documented
- [ ] Provider limits documented: max connections, item size, shards, command support
- [ ] Cost drivers reviewed: RAM, replicas, shards, backup storage, cross-AZ traffic, index memory

---

## Operational Runbooks

- [ ] Runbook: OOM or high eviction
- [ ] Runbook: Sentinel failover recovery
- [ ] Runbook: Replica full resync
- [ ] Runbook: SLOWLOG investigation
- [ ] Runbook: Redis unreachable (application failover procedure)
- [ ] Runbook: ACL key rotation

---

## Interview Sound Bite

Production Redis readiness is not only about configuration: it is the combination of correct maxmemory and eviction policy, TTL on every cache key, security hardening (ACL + TLS), HA topology (Sentinel or Cluster), Sentinel-aware clients, observability dashboards, and application code free of blocking commands. Any missing item is a potential on-call incident.
