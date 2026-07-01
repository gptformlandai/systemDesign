# 08. Replication and Sharding

## Replication

Replication keeps copies of data across MongoDB nodes. A production MongoDB deployment usually runs as a replica set.

```text
Client -> Primary -> Oplog -> Secondaries
```

## Replica Set Terms

| Term | Meaning |
|---|---|
| Primary | Accepts writes |
| Secondary | Replicates primary data |
| Arbiter | Voting-only member, no data, use sparingly |
| Oplog | Capped operation log |
| Election | Process to choose a new primary |
| Failover | Moving primary role after failure |

## Why Majority Write Concern Matters

With `w: 'majority'`, a write is acknowledged only after most voting members have it. This reduces rollback risk during failover.

## Replication Lag

Replication lag is delay between primary writes and secondary application.

Causes:

- slow disk
- network issues
- write bursts
- heavy index builds
- overloaded secondaries

Impact:

- stale secondary reads
- delayed failover readiness
- possible resync if oplog window is exceeded

## Sharding

Sharding horizontally partitions data across shards.

Components:

| Component | Role |
|---|---|
| Shard | Stores subset of data |
| `mongos` | Query router |
| Config servers | Store sharding metadata |
| Chunk | Range of shard key values |
| Balancer | Moves chunks |
| Shard key | Field(s) deciding distribution |
| Zone | Region/hardware placement rule |

## Shard Key Selection

A good shard key has:

- high cardinality
- even distribution
- query isolation
- write distribution
- stable value
- no hotspots

Bad shard keys:

- timestamp only
- status only
- tenantId only with skewed tenants
- field not included in common queries

## Targeted vs Scatter-Gather

Targeted query includes shard key fields and routes to fewer shards.

Scatter-gather query lacks shard key fields and fans out to many shards.

## Example Shard Keys

| Workload | Candidate |
|---|---|
| Orders | `{ tenantId: 1, orderId: 1 }` |
| Chat | `{ conversationId: 1, bucketId: 1 }` |
| IoT | `{ tenantId: 1, deviceId: 1, ts: 1 }` |
| Logs | `{ tenantId: 1, dayBucket: 1, _id: 1 }` |
| SaaS | `{ tenantId: 1, entityId: 1 }` with skew analysis |

## Interview Answer

Replication solves availability and durability. Sharding solves horizontal scale. A strong answer separates the two and explains that sharding success depends heavily on shard key choice, query targeting, and avoiding hotspots.
