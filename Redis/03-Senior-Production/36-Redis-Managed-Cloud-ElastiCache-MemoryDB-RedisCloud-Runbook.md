# 36. Managed Redis Cloud: ElastiCache, MemoryDB, Redis Cloud, And Provider Runbooks

## Goal

Understand how Redis changes when you do not operate the process directly. Managed Redis removes some operational toil, but it introduces provider-specific limits, failover behavior, scaling controls, security integrations, and cost tradeoffs.

```text
self-managed Redis knowledge + provider constraints + runbooks = cloud production Redis
```

---

## 1. Managed Redis Mental Model

Self-managed Redis:

```text
you manage process, OS, disk, backups, patching, failover, networking
```

Managed Redis:

```text
provider manages infrastructure, but you still own data model, command choices, TTLs, clients, cost, and incident response
```

The biggest mistake:

```text
"Managed" does not mean "no Redis architecture required."
```

---

## 2. Service Decision Map

| Need | Typical Fit |
|---|---|
| simple managed cache | ElastiCache / Redis Cloud cache deployment |
| Redis-compatible durable primary database on AWS | MemoryDB style architecture |
| fully managed variable capacity | serverless cache offering when supported |
| multi-cloud or Redis vendor features | Redis Cloud |
| strict OSS self-control | self-managed Redis or Kubernetes operator |

Cloud service names and capabilities change. In interviews, explain the decision criteria rather than reciting one provider's UI.

---

## 3. ElastiCache-Style Cache Architecture

Use when:

- Redis is a cache/session/rate-limit layer
- source of truth is RDS, DynamoDB, S3, or another durable system
- low latency matters
- managed failover and backups are desired

Core decisions:

| Decision | What To Choose |
|---|---|
| cluster mode | disabled for single-shard simplicity, enabled for horizontal scale |
| node size | memory + network + CPU headroom |
| replicas | at least one per shard for production |
| Multi-AZ | enable for production failover |
| encryption | TLS in transit, encryption at rest if supported |
| auth | auth token or ACL/IAM integration depending on provider |
| subnet group | private subnets only |
| security group | allow app security group, not broad CIDRs |

Production warning:

```text
If Redis is only a cache, design the database to survive a cache miss storm.
```

---

## 4. MemoryDB-Style Durable Redis Architecture

Use when:

- Redis-compatible API is desired as primary data store
- single-digit millisecond latency is needed
- stronger durability than cache Redis is required
- app access pattern fits Redis data structures

Be careful:

- Redis data modeling is not relational modeling
- memory cost is high
- complex queries and joins still do not fit
- persistence/durability does not remove need for backups
- client idempotency still matters

Interview answer:

```text
I only use durable managed Redis as source of truth when the access pattern is naturally key/value or data-structure oriented and the business accepts Redis-style constraints. Otherwise I keep Redis as cache and use a database as source of truth.
```

---

## 5. Cloud HA And Failover Runbook

During failover:

```text
primary unavailable -> replica promoted -> DNS/endpoint/topology updates -> clients reconnect -> writes resume
```

Application requirements:

- short connect and command timeouts
- retry with jitter
- topology-aware client
- idempotent writes
- local near-cache flush after reconnect
- no hardcoded node IPs

Evidence to collect:

- provider event timeline
- Redis `INFO replication`
- application reconnect logs
- error rate and timeout rate
- p99 latency
- DNS resolution behavior if endpoint changed
- client topology refresh logs for Cluster

---

## 6. Backup And Restore Runbook

Backups are not real until restore is tested.

Checklist:

- automated backup schedule configured
- retention matches RPO
- manual snapshot before risky migration
- restore into staging tested
- restore time measured
- application can point to restored endpoint
- backup encryption and permissions verified

RPO/RTO framing:

| Term | Redis Meaning |
|---|---|
| RPO | how much recent data can be lost |
| RTO | how long app can tolerate Redis recovery |

For cache-only Redis:

```text
RPO may be near irrelevant; warm-up and DB protection matter more.
```

For Redis as durable store:

```text
RPO and restore testing are business-critical.
```

---

## 7. Scaling Runbook

Scale up when:

- CPU high
- network bandwidth high
- memory near limit
- fork/persistence overhead too high

Scale out when:

- one shard memory is high
- write throughput exceeds one primary
- hot key can be redesigned into multiple keys
- cluster mode is acceptable

Before scaling out:

- audit multi-key commands
- audit Lua/Functions key usage
- add hash tags intentionally
- test MOVED/ASK client handling
- run load test with production key distribution

Cluster migration trap:

```text
Moving from non-cluster to cluster can break SELECT, cross-slot MGET/MSET, multi-key Lua, and transaction assumptions.
```

---

## 8. Security Checklist

Managed Redis production baseline:

- private subnet placement
- security group allows only app tier
- TLS in transit enabled
- encryption at rest enabled where supported
- auth token or ACL configured
- credential rotation runbook
- no public endpoint
- secrets stored in a secret manager
- least-privilege admin access
- provider audit logs enabled where available

Application baseline:

- credentials not logged
- Redis URL not committed
- TLS verification enabled
- timeouts configured
- circuit breaker configured

---

## 9. Cost Controls

Redis cloud cost grows through:

- RAM size
- replicas
- shards
- multi-AZ
- backup storage
- cross-AZ traffic
- over-indexing Search/vector/JSON
- retaining time-series data too long

Cost questions:

1. What is used_memory vs provisioned memory?
2. What is average key size?
3. What percent of keys have TTL?
4. How much memory belongs to indexes?
5. How much memory belongs to stale keys?
6. Are replicas serving reads or only sitting idle for HA?
7. Can cold data move to DB/object storage?

---

## 10. Provider-Specific Questions To Ask

Ask these for any managed Redis provider:

- Which Redis-compatible version is supported?
- Are Redis Stack commands supported?
- Are Functions supported?
- Is Cluster mode supported?
- How does failover work and how long does it usually take?
- Are backups point-in-time or snapshot only?
- Can we restore across region/account/project?
- How are upgrades performed?
- Is TLS mandatory or optional?
- Are ACLs supported?
- What is the max item size?
- What is the max client connection count?
- What metrics and logs are exported?

---

## 11. Common Managed Redis Mistakes

| Mistake | Better Approach |
|---|---|
| treating managed Redis as source of truth accidentally | explicitly classify cache vs durable store |
| hardcoding node endpoint | use configuration/provider endpoint/client discovery |
| no failover test | run staging failover drill |
| no TTL discipline | require TTL ownership per key pattern |
| ignoring cross-AZ traffic | model network cost and latency |
| enabling Cluster without client audit | test multi-key commands and hash tags |
| assuming backups are enough | restore-test and measure RTO |
| over-indexing JSON/vector/search | capacity plan index memory |

---

## 12. Strong Architecture Answer

> Would you use ElastiCache or MemoryDB for a shopping cart?

Strong answer:

```text
If the cart is recoverable from the primary database or acceptable to lose during rare cache failure, I use ElastiCache as a session/cart cache with TTL and write-through/invalidation to the database. If the cart itself must be Redis-compatible, low-latency, and durable as primary state, I consider MemoryDB-style durable Redis, but I validate cost, backup/restore, and data model constraints. In both cases, the app needs idempotent writes, retry with jitter, private networking, TLS/auth, and tested failover behavior.
```

---

## 13. Revision Notes

- One-line summary: managed Redis reduces infrastructure toil, not architecture responsibility.
- Three keywords: provider limits, failover, cost.
- One interview trap: saying "managed Redis handles everything."
- One memory trick: provider owns the box; you still own the keys.

---

## 14. Official Source Notes

- Redis latest docs: <https://redis.io/docs/latest/>
- Amazon ElastiCache docs: <https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/WhatIs.html>
- Amazon MemoryDB docs: <https://docs.aws.amazon.com/memorydb/latest/devguide/what-is-memorydb-for-redis.html>
- Redis Cloud docs: <https://redis.io/docs/latest/operate/rc/>
