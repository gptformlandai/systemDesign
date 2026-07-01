    # MongoDB Cheat Sheets, Roadmap and Golden Rules - Gold Sheet

    > **Track File #28 of 28 - Group 06: Practice Upgrade**
    > For: backend/database/system design interviews | Level: revision and final consolidation | Mode: fast recall, commands, roadmap, rules

    This sheet builds:
    - 12 cheat sheets
- Beginner-to-pro roadmap
- Golden rules, mistakes, commands, interview points

Original master-map sections included here:
- 31. MongoDB Cheat Sheets
- 32. Beginner to Pro Roadmap
- 33. Final Summary

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 31. MongoDB Cheat Sheets

### 1. CRUD Cheat Sheet

```javascript
db.users.insertOne(doc)
db.users.insertMany([doc1, doc2], { ordered: false })
db.users.find(filter, projection)
db.users.findOne(filter)
db.users.updateOne(filter, update, { upsert: true })
db.users.updateMany(filter, update)
db.users.replaceOne(filter, replacement)
db.users.deleteOne(filter)
db.users.deleteMany(filter)
```

### 2. Query Operator Cheat Sheet

| Type | Operators |
|---|---|
| Comparison | `$eq`, `$ne`, `$gt`, `$gte`, `$lt`, `$lte`, `$in`, `$nin` |
| Logical | `$and`, `$or`, `$not`, `$nor` |
| Array | `$all`, `$elemMatch`, `$size` |
| Element | `$exists`, `$type` |
| Regex | `$regex` |

### 3. Update Operator Cheat Sheet

| Operator | Use |
|---|---|
| `$set` | Set field |
| `$unset` | Remove field |
| `$inc` | Increment/decrement |
| `$push` | Append to array |
| `$pull` | Remove from array |
| `$addToSet` | Add unique array value |
| `$pop` | Remove first/last |
| `$rename` | Rename field |
| `$currentDate` | Current date timestamp |
| `$setOnInsert` | Set only during upsert insert |

### 4. Aggregation Stage Cheat Sheet

| Stage | Use |
|---|---|
| `$match` | Filter |
| `$project` | Select/compute fields |
| `$group` | Aggregate by key |
| `$sort` | Sort |
| `$limit` | Limit |
| `$skip` | Offset |
| `$unwind` | Expand arrays |
| `$lookup` | Join |
| `$set` / `$addFields` | Add fields |
| `$unset` | Remove fields |
| `$facet` | Multiple sub-pipelines |
| `$bucket` | Fixed buckets |
| `$merge` | Write merged output |
| `$out` | Replace output collection |
| `$setWindowFields` | Window functions |
| `$graphLookup` | Recursive lookup |

### 5. Indexing Cheat Sheet

```javascript
db.users.createIndex({ email: 1 })
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
db.users.createIndex({ tenantId: 1, email: 1 }, { unique: true })
db.sessions.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
db.users.getIndexes()
db.users.dropIndex("index_name")
```

Rules: ESR, cover hot queries, avoid over-indexing, explain everything important.

### 6. Schema Design Cheat Sheet

| Use Embed | Use Reference |
|---|---|
| Read together | Large child data |
| One-to-few | Many-to-many |
| Bounded arrays | Unbounded growth |
| Same lifecycle | Independent lifecycle |
| Atomic update needed | Shared entity |

### 7. Transactions Cheat Sheet

```javascript
const session = client.startSession();
await session.withTransaction(async () => {
  await collection.updateOne(filter, update, { session });
});
await session.endSession();
```

Use for cross-document invariants. Prefer aggregate design when possible.

### 8. Replication Cheat Sheet

```javascript
rs.initiate()
rs.status()
rs.conf()
rs.stepDown(60)
```

Terms: primary, secondary, election, oplog, majority write, replication lag.

### 9. Sharding Cheat Sheet

```javascript
sh.enableSharding("app")
sh.shardCollection("app.orders", { tenantId: 1, orderId: 1 })
sh.status()
```

Good shard key: high cardinality, even distribution, query-aligned, stable, no hotspots.

### 10. Performance Tuning Cheat Sheet

- Run `explain("executionStats")`.
- Avoid `COLLSCAN` on hot paths.
- Keep docs examined close to returned.
- Use projection.
- Use cursor pagination.
- Batch writes.
- Precompute dashboards.
- Avoid unbounded arrays.
- Monitor slow queries.
- Revisit schema before adding many indexes.

### 11. Security Cheat Sheet

- auth enabled
- least privilege roles
- TLS
- IP allowlist/private network
- secrets manager
- encrypted backups
- audit logs
- no credentials in code
- field encryption for sensitive PII
- rotate users/passwords

### 12. Interview Cheat Sheet

- MongoDB stores BSON documents.
- Model around access patterns.
- Embed bounded data read together.
- Reference large/shared/unbounded data.
- Index filters and sorts with ESR.
- Use explain plans.
- Replica sets provide HA.
- Sharding requires careful shard key.
- Transactions exist but are not a modeling substitute.
- Change streams enable real-time reactions.
- Atlas Search and Vector Search support search/GenAI patterns.

---

---

## 32. Beginner to Pro Roadmap

### Stage 1: Beginner

Topics:

- document model
- databases, collections, documents
- BSON and ObjectId
- CRUD
- basic filters and projections

Exercises:

- create collections
- insert sample documents
- query nested fields
- update arrays

Project: user profile service.

Success criteria:

- You can explain document vs row.
- You can perform CRUD without guessing syntax.
- You know why `_id` exists.

Common mistakes:

- thinking MongoDB is just JSON files
- inconsistent field types
- no indexes for lookups

### Stage 2: Intermediate

Topics:

- schema design
- embedding vs referencing
- indexes
- aggregation
- app integration

Exercises:

- model orders and products
- create compound indexes
- run explain plans
- build sales aggregation

Project: e-commerce catalog and order API.

Success criteria:

- You choose embed/reference from access patterns.
- You can build useful compound indexes.
- You can write aggregation pipelines.

Common mistakes:

- embedding unbounded arrays
- using `$lookup` everywhere
- ignoring sort indexes

### Stage 3: Advanced

Topics:

- transactions
- replication
- change streams
- performance tuning
- time-series

Exercises:

- local replica set
- transaction workflow
- change stream listener
- slow query optimization

Project: real-time notification pipeline.

Success criteria:

- You understand majority writes and replication lag.
- You know when transactions are appropriate.
- You can debug slow queries with explain.

Common mistakes:

- overusing transactions
- reading from secondaries without staleness awareness
- treating change streams as magic queues

### Stage 4: Production

Topics:

- security
- backup/restore
- monitoring
- Atlas
- operations
- disaster recovery

Exercises:

- create least-privilege users
- configure backups
- restore test
- set slow query profiler

Project: production-ready multi-tenant backend.

Success criteria:

- You can define RPO/RTO.
- You can design secure connection patterns.
- You can build an operational checklist.

Common mistakes:

- no restore drills
- credentials in code
- no monitoring on replica lag or disk

### Stage 5: MAANG-Level

Topics:

- sharding
- system design
- tradeoffs
- failure modes
- large-scale architecture
- GenAI/vector patterns

Exercises:

- choose shard keys under traffic assumptions
- design chat/order/audit systems
- design RAG store with ACLs
- debug bad schema and hot shard scenarios

Project: globally scalable order system using MongoDB.

Success criteria:

- You explain shard key tradeoffs clearly.
- You design for failure and operational recovery.
- You compare MongoDB vs SQL vs search/vector systems pragmatically.

Common mistakes:

- tenantId-only shard key with skew
- timestamp-only shard key for writes
- missing consistency story
- no plan for reindexing, resharding, or archival

---

---

## 33. Final Summary

### 20 MongoDB Golden Rules

1. Model around access patterns.
2. Embed data that is bounded and read together.
3. Reference data that is large, shared, or independently changing.
4. Avoid unbounded arrays.
5. Use schema validation for important collections.
6. Create indexes for hot filters and sorts.
7. Use compound indexes intentionally.
8. Follow ESR as a starting point.
9. Validate with `explain("executionStats")`.
10. Keep documents below practical size, not just below 16 MB.
11. Use cursor pagination for deep lists.
12. Precompute high-volume dashboards.
13. Use majority write concern for critical data.
14. Understand stale reads before using secondaries.
15. Use transactions only when the invariant crosses documents.
16. Choose shard keys with cardinality, distribution, and query targeting.
17. Secure MongoDB with auth, TLS, least privilege, and network controls.
18. Test restores, not just backups.
19. Monitor slow queries, replica lag, disk, cache, and connections.
20. Treat MongoDB as a production database, not a flexible dumping ground.

### 20 Most Common Mistakes

1. No access pattern analysis.
2. Missing indexes.
3. Indexing every field.
4. Wrong compound index order.
5. Deep `skip` pagination.
6. Unbounded arrays.
7. Giant documents.
8. Too many `$lookup` stages.
9. Using transactions to hide poor modeling.
10. Ignoring write concern.
11. Reading stale data accidentally.
12. Bad shard key.
13. TenantId-only shard key with large tenant skew.
14. Timestamp-only shard key for high-volume writes.
15. Arbitrary user-defined queries.
16. No schema validation.
17. No slow query monitoring.
18. No backup restore tests.
19. Credentials in source code.
20. Using MongoDB for heavy relational reporting without read models or OLAP.

### 20 Must-Know Commands

```javascript
show dbs
use appdb
show collections
db.createCollection("users")
db.users.insertOne({})
db.users.insertMany([])
db.users.find({})
db.users.findOne({})
db.users.updateOne({}, { $set: {} })
db.users.updateMany({}, { $set: {} })
db.users.deleteOne({})
db.users.deleteMany({})
db.users.createIndex({ field: 1 })
db.users.getIndexes()
db.users.dropIndex("name")
db.users.find({}).explain("executionStats")
db.users.aggregate([])
db.setProfilingLevel(1, { slowms: 100 })
rs.status()
sh.status()
```

### 20 Must-Know Interview Points

1. MongoDB stores BSON documents.
2. `_id` is unique and indexed.
3. Single-document writes are atomic.
4. Flexible schema still needs governance.
5. Embedding improves locality and atomicity.
6. Referencing avoids unbounded growth and duplication.
7. Indexes speed reads but slow writes.
8. Compound index order matters.
9. Covered queries avoid document fetch.
10. Explain plans reveal actual execution.
11. Aggregation is a document pipeline.
12. `$lookup` is useful but not a license to model like SQL.
13. Replica sets provide high availability.
14. Oplog powers replication and change streams.
15. Majority write concern improves failover safety.
16. Transactions exist but have costs.
17. Sharding scales horizontally but depends on shard key quality.
18. Scatter-gather queries can be expensive.
19. Change streams enable real-time event reactions.
20. MongoDB vs SQL choice is about data shape, access patterns, and constraints.

### 10 System Design Patterns Using MongoDB

1. Aggregate document for order/cart/profile.
2. Subset pattern for product reviews and previews.
3. Bucket pattern for telemetry/messages/events.
4. Computed pattern for counters and ratings.
5. Extended reference for display snapshots.
6. Outbox pattern for reliable event publishing.
7. CQRS read model for fast APIs.
8. Time-series collection for metrics.
9. Atlas Search for product/content search.
10. Vector search plus metadata for RAG.

### 10 Performance Rules

1. Measure before changing.
2. Use explain plans.
3. Avoid collection scans on hot paths.
4. Index filters and sorts together.
5. Keep result sets bounded.
6. Use projection.
7. Avoid deep skip.
8. Batch writes when ingesting.
9. Preaggregate repeated reports.
10. Redesign schema when indexes cannot fix the shape.

### 10 Schema Design Rules

1. Start with queries, not entities.
2. Define aggregate roots.
3. Embed one-to-one and one-to-few owned data.
4. Reference unbounded one-to-many data.
5. Duplicate stable read-heavy fields intentionally.
6. Store snapshots for historical facts.
7. Keep arrays bounded.
8. Version documents during evolution.
9. Validate critical fields and types.
10. Revisit schema when traffic patterns change.

---

### Closing Mental Model

MongoDB is strongest when you treat a document as an application aggregate: a carefully shaped unit of data that matches how your system reads, writes, scales, and fails. Beginner MongoDB is CRUD. Professional MongoDB is schema design plus indexes. Senior MongoDB is operational behavior, consistency, sharding, and tradeoffs. Interview-level MongoDB is explaining why each choice fits the workload and what breaks when assumptions change.

---
