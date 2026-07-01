# Sharding Design Guide

## Why Shard

Shard when one replica set cannot handle:

- data size
- write throughput
- read throughput
- working set memory
- region/data residency requirements

## Components

| Component | Role |
|---|---|
| Shard | Stores subset of data |
| `mongos` | Query router |
| Config server | Cluster metadata |
| Chunk | Range of shard key values |
| Balancer | Moves chunks |
| Zone | Region/hardware placement |

## Good Shard Key Checklist

- high cardinality
- even distribution
- appears in common queries
- avoids write hotspots
- stable value
- supports important range queries if needed
- handles tenant skew

## Bad Shard Keys

| Key | Problem |
|---|---|
| `createdAt` | monotonic write hotspot |
| `status` | low cardinality |
| `tenantId` only | huge tenant imbalance |
| random UUID only | poor query targeting |

## Case: Orders

Access patterns:

- get order by tenant/orderId
- list customer orders
- list tenant orders by status/date

Candidate:

```javascript
{ tenantId: 1, orderId: 1 }
```

If huge tenant skew exists, add bucket/entity distribution.

## Case: Chat

Access patterns:

- list messages by conversation/time
- append messages

Candidate:

```javascript
{ conversationId: 1, bucketId: 1 }
```

For very hot conversations, use bucket/suffix strategy.

## Case: IoT

Avoid timestamp-only. Candidate:

```javascript
{ tenantId: 1, deviceId: 1, ts: 1 }
```

## Case: Logs

Candidate:

```javascript
{ tenantId: 1, dayBucket: 1, _id: 1 }
```

## Strong Interview Answer

Shard key choice is a tradeoff between distribution and query targeting. I evaluate cardinality, tenant skew, write pattern, range needs, and common filters. I avoid timestamp-only and low-cardinality keys, and I explicitly discuss scatter-gather risk.
