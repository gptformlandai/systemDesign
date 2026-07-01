    # MongoDB Performance Tuning and Observability - MAANG Master Sheet

    > **Track File #14 of 28 - Group 03: Senior MAANG**
    > For: backend/database/system design interviews | Level: senior production debugging | Mode: slow query, profiling, metrics, operational dashboards

    This sheet builds:
    - Tuning workflow, explain, profiler, slow logs
- Pagination, bulk writes, caching, materialized views
- serverStatus, collStats, indexStats, replica lag, Atlas monitoring

Original master-map sections included here:
- 13. Performance Tuning
- 26. Observability and Operations

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 13. Performance Tuning

### Tuning Workflow

1. Identify slow endpoint or query.
2. Capture exact filter, sort, projection, limit, collection, and cardinality.
3. Run `explain("executionStats")`.
4. Compare `nReturned`, `keysExamined`, `docsExamined`, and stages.
5. Check existing indexes.
6. Add or adjust index for the query pattern.
7. Re-run explain.
8. Consider schema redesign if index cannot fix growth pattern.
9. Load test with realistic data volume.
10. Monitor after deployment.

### Query Profiling

Enable profiler for slow queries:

```javascript
db.setProfilingLevel(1, { slowms: 100 })
db.system.profile.find().sort({ ts: -1 }).limit(5).pretty()
```

Disable:

```javascript
db.setProfilingLevel(0)
```

### Slow Query Logs

MongoDB logs slow operations. In Atlas, use Performance Advisor and Query Profiler.

### Index Tuning

Use:

```javascript
db.collection.getIndexes()
db.collection.dropIndex("index_name")
db.collection.createIndex({ tenantId: 1, createdAt: -1 })
```

Do not keep unused indexes forever.

### Schema Redesign

Indexing cannot always save a bad model.

Bad:

```javascript
{ _id: "product1", reviews: [/* 2 million reviews */] }
```

Good:

```javascript
// product summary
{ _id: "product1", averageRating: 4.7, reviewCount: 2000000, recentReviews: [] }

// reviews collection
{ productId: "product1", rating: 5, createdAt: ISODate("...") }
```

### Connection Pooling

Drivers maintain connection pools.

Node.js example:

```javascript
const client = new MongoClient(uri, {
  maxPoolSize: 100,
  minPoolSize: 5,
  serverSelectionTimeoutMS: 5000
});
```

Rules:

- Create one client per process, reuse it.
- Do not create a new client per request.
- Size pools based on concurrency, latency, and database capacity.
- Watch connection storms during deployments.

### Pagination Strategy

Bad deep pagination:

```javascript
db.posts.find({ tenantId: "t1" }).sort({ createdAt: -1 }).skip(100000).limit(20)
```

Good cursor pagination:

```javascript
db.posts.find({
  tenantId: "t1",
  createdAt: { $lt: lastSeenCreatedAt }
}).sort({ createdAt: -1 }).limit(20)
```

Better tie-breaker:

```javascript
db.posts.find({
  tenantId: "t1",
  $or: [
    { createdAt: { $lt: lastCreatedAt } },
    { createdAt: lastCreatedAt, _id: { $lt: lastId } }
  ]
}).sort({ createdAt: -1, _id: -1 }).limit(20)
```

### Bulk Writes

```javascript
db.products.bulkWrite([
  { updateOne: { filter: { sku: "SKU-1" }, update: { $set: { priceCents: 7999 } }, upsert: true } },
  { updateOne: { filter: { sku: "SKU-2" }, update: { $set: { priceCents: 3999 } }, upsert: true } }
], { ordered: false })
```

Use for ingestion and batch updates.

### Avoiding N+1 Queries

Bad:

1. Query 100 orders.
2. Query user for each order.

Better options:

- embed user snapshot in order
- batch user query with `$in`
- use `$lookup` after limiting
- create read model

### Read/Write Concern Tuning

- Important state changes: `w: "majority"`.
- Low-critical telemetry: lower durability may be acceptable depending on business risk.
- Secondary reads: only for stale-tolerant paths.

### Caching

Use Redis or CDN for:

- hot product pages
- user sessions
- computed dashboards
- read-heavy static reference data

Cache invalidation options:

- TTL
- explicit invalidation on writes
- change streams
- versioned cache keys

### Materialized Views

Use aggregation plus `$merge`:

```javascript
db.orders.aggregate([
  { $match: { createdAt: { $gte: start, $lt: end }, status: "PAID" } },
  { $group: { _id: { tenantId: "$tenantId", day: "$day" }, revenueCents: { $sum: "$totalCents" } } },
  { $merge: { into: "dailyRevenue", on: "_id", whenMatched: "replace", whenNotMatched: "insert" } }
])
```

### Performance Checklist

| Check | Good Sign |
|---|---|
| Explain plan | `IXSCAN`, no huge `COLLSCAN` |
| Docs examined | Close to returned count |
| Sort | Covered by index or small after filter |
| Projection | Only needed fields |
| Arrays | Bounded or split out |
| Index count | Enough, not excessive |
| Write batch | Bulk writes for ingestion |
| Pagination | Cursor-based for deep lists |
| Dashboard | Precomputed if high volume |
| Connections | Reused client and sane pool |

---

---

## 26. Observability and Operations

### Logs

Monitor logs for:

- slow queries
- connection errors
- authentication failures
- replication issues
- elections
- index builds
- storage warnings

### Metrics

| Metric | Why It Matters |
|---|---|
| Query latency | User-facing performance |
| Ops/sec | Workload volume |
| Connections | Pool sizing and leaks |
| Replica lag | Stale reads/failover readiness |
| Cache dirty/used | Memory pressure |
| Disk I/O | Storage bottleneck |
| CPU | Query or compression pressure |
| Lock/queue metrics | Contention |
| Index size | Memory/storage planning |
| Page faults/cache misses | Working set too large |

### Useful Commands

Current operations:

```javascript
db.currentOp()
```

Kill operation:

```javascript
db.killOp(opid)
```

Server status:

```javascript
db.serverStatus()
```

Database stats:

```javascript
db.stats()
```

Collection stats:

```javascript
db.orders.stats()
```

Index stats:

```javascript
db.orders.aggregate([{ $indexStats: {} }])
```

Replica status:

```javascript
rs.status()
```

Collection storage stats:

```javascript
db.runCommand({ collStats: "orders" })
```

Profiler:

```javascript
db.setProfilingLevel(1, { slowms: 100 })
```

### Atlas Monitoring

Atlas provides:

- cluster metrics
- query profiler
- performance advisor
- index suggestions
- alerts
- backup status
- connection/network metrics
- search/vector metrics depending features

### Alert Ideas

- replication lag over threshold
- disk usage above 80 percent
- CPU sustained high
- connections near limit
- cache eviction pressure
- slow query spike
- failed backups
- primary election occurred
- high opcounters/write latency

### Operational Checklist

- Explain reviewed for top queries.
- Indexes documented and monitored.
- Backups enabled and restored in drills.
- Alerts configured.
- Security least privilege enforced.
- Slow query profiler used periodically.
- Capacity plan covers data, indexes, and growth.
- Upgrade plan tested in staging.
- Runbook exists for failover, restore, and hot shard.

---

---
