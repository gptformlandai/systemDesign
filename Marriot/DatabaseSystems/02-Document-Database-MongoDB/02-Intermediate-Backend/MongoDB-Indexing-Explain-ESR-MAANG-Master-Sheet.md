    # MongoDB Indexing, Explain Plans and ESR - MAANG Master Sheet

    > **Track File #8 of 28 - Group 02: Intermediate Backend**
    > For: backend/database/system design interviews | Level: intermediate to senior | Mode: query planner, index design, performance debugging

    This sheet builds:
    - Index internals mental model
- ESR rule, covered queries, cardinality, selectivity
- Index types and explain output interpretation

Original master-map sections included here:
- 8. Indexing Deep Dive

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 8. Indexing Deep Dive

### What Is an Index?

An index is a data structure that lets MongoDB find documents without scanning every document in a collection.

Simple mental model: an index is like a sorted lookup book. Without it, MongoDB may inspect every document. With it, MongoDB jumps to the matching range.

### Why Indexes Matter

Without a useful index:

```javascript
db.orders.find({ tenantId: "t1", status: "PAID" })
```

MongoDB may do a collection scan (`COLLSCAN`). With an index:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1 })
```

MongoDB can use an index scan (`IXSCAN`).

### Index Tradeoff

| Index Benefit | Index Cost |
|---|---|
| Faster reads | Slower writes |
| Faster sorts | More storage |
| Uniqueness enforcement | More memory pressure |
| Covered queries | More maintenance during updates |

Every insert/update/delete must maintain relevant indexes. Over-indexing is a real production problem.

### Query Planner Basics

MongoDB considers candidate plans, tries them, picks a winning plan, and caches plan choices for similar query shapes.

Key terms:

| Term | Meaning |
|---|---|
| `COLLSCAN` | Scans collection documents |
| `IXSCAN` | Scans index keys |
| `FETCH` | Fetches full documents after index lookup |
| `SORT` | In-memory/blocking sort if index cannot satisfy sort |
| `winningPlan` | Plan selected by optimizer |
| `keysExamined` | Index entries scanned |
| `docsExamined` | Documents inspected |
| `nReturned` | Documents returned |

Healthy pattern: `keysExamined` and `docsExamined` close to `nReturned` for selective queries.

Bad pattern: millions examined, few returned.

### Cardinality and Selectivity

Cardinality: number of distinct values.

Selectivity: how much a predicate narrows the dataset.

| Field | Cardinality | Selectivity Example |
|---|---|---|
| `status` | Low | `PAID` may match 40 percent |
| `tenantId` | Medium/high | One tenant may match 0.1 percent or 20 percent |
| `email` | High | Usually one user |
| `createdAt` | High | Useful with range and sort |

High-selectivity indexes usually help more.

### Covered Queries

A covered query can be answered from the index without fetching documents.

```javascript
db.users.createIndex({ tenantId: 1, email: 1, name: 1 })

db.users.find(
  { tenantId: "t1", email: "asha@example.com" },
  { _id: 0, email: 1, name: 1 }
)
```

If all projected fields are in the index and `_id` is excluded unless indexed, MongoDB can avoid `FETCH`.

### Index Prefix Rule

Compound index:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

Can support:

- `{ tenantId: "t1" }`
- `{ tenantId: "t1", status: "PAID" }`
- `{ tenantId: "t1", status: "PAID" }` sorted by `createdAt`

Less useful for:

- `{ status: "PAID" }` without `tenantId`
- sorting by `createdAt` alone

### ESR Rule: Equality, Sort, Range

For compound indexes, order fields roughly as:

1. Equality filters
2. Sort fields
3. Range filters

Example query:

```javascript
db.orders.find({
  tenantId: "t1",
  status: "PAID",
  createdAt: { $gte: start, $lt: end }
}).sort({ createdAt: -1 })
```

Index:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

If sorting by `createdAt` and also range filtering on `createdAt`, that field can serve both range and sort when ordered correctly.

### Sort With Index

```javascript
db.orders.createIndex({ tenantId: 1, createdAt: -1 })

db.orders.find({ tenantId: "t1" }).sort({ createdAt: -1 }).limit(20)
```

This avoids a blocking sort.

### Index Intersection

MongoDB can sometimes combine multiple indexes for one query, but a well-designed compound index is usually better for common hot paths.

### Index Types

#### 1. Single Field Index

```javascript
db.users.createIndex({ email: 1 })
```

Use for simple lookups and uniqueness.

#### 2. Compound Index

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

Use for multi-field filters and sorts.

#### 3. Multikey Index

Created when indexing arrays:

```javascript
db.products.createIndex({ tags: 1 })
```

Query:

```javascript
db.products.find({ tags: "wireless" })
```

Limitation: compound indexes involving multiple array fields can create combinatorial index entries and may be restricted.

#### 4. Text Index

```javascript
db.articles.createIndex({ title: "text", body: "text" })

db.articles.find({ $text: { $search: "mongodb indexing" } })
```

Use for basic text search. For relevance, autocomplete, fuzzy, and analyzers, use Atlas Search.

#### 5. Hashed Index

```javascript
db.events.createIndex({ userId: "hashed" })
```

Use for hashed shard keys or evenly distributed equality lookups. Not useful for range queries.

#### 6. Geospatial Index

```javascript
db.places.createIndex({ location: "2dsphere" })

db.places.find({
  location: {
    $near: {
      $geometry: { type: "Point", coordinates: [-96.7970, 32.7767] },
      $maxDistance: 5000
    }
  }
})
```

#### 7. TTL Index

```javascript
db.sessions.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
```

Use for sessions, temporary tokens, short-lived events. TTL deletion is not instant; it runs in the background.

#### 8. Partial Index

```javascript
db.users.createIndex(
  { tenantId: 1, email: 1 },
  { unique: true, partialFilterExpression: { deletedAt: { $exists: false } } }
)
```

Use when only a subset needs indexing.

#### 9. Sparse Index

```javascript
db.users.createIndex({ phone: 1 }, { sparse: true })
```

Indexes only documents containing the field. Partial indexes are often clearer and more flexible.

#### 10. Unique Index

```javascript
db.users.createIndex({ tenantId: 1, email: 1 }, { unique: true })
```

Use for business invariants.

#### 11. Wildcard Index

```javascript
db.products.createIndex({ "attributes.$**": 1 })
```

Use for flexible attributes where keys vary. Avoid as a lazy replacement for modeling hot queries.

#### 12. Clustered Collection Concept

A clustered collection stores documents ordered by a clustered index key. It can improve locality for time-series or key-ordered access patterns. Use when access naturally follows the clustered key and you understand write distribution.

#### 13. Vector Search Index

Atlas Vector Search indexes embedding arrays for similarity search.

Conceptual index definition:

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "tenantId"
    }
  ]
}
```

### `explain()` Examples

```javascript
db.orders.find({ tenantId: "t1", status: "PAID" })
  .sort({ createdAt: -1 })
  .limit(20)
  .explain("executionStats")
```

Look for:

```javascript
{
  executionStats: {
    nReturned: 20,
    totalKeysExamined: 20,
    totalDocsExamined: 20
  },
  queryPlanner: {
    winningPlan: {
      stage: "LIMIT",
      inputStage: { stage: "FETCH", inputStage: { stage: "IXSCAN" } }
    }
  }
}
```

Bad signs:

- `COLLSCAN`
- `SORT` stage for large result sets
- `totalDocsExamined` much greater than `nReturned`
- many rejected plans
- index not matching filter/sort shape

### Case Study 1: Optimize User Lookup

Query:

```javascript
db.users.findOne({ tenantId: "t1", email: "asha@example.com" })
```

Index:

```javascript
db.users.createIndex({ tenantId: 1, email: 1 }, { unique: true })
```

Why: tenant-scoped uniqueness and fast login lookup.

### Case Study 2: Orders by Status and Date

Query:

```javascript
db.orders.find({ tenantId: "t1", status: "PAID" })
  .sort({ createdAt: -1 })
  .limit(50)
```

Index:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

Why: equality, equality, sort.

### Case Study 3: Tenant + CreatedAt Search

Query:

```javascript
db.auditLogs.find({
  tenantId: "t1",
  createdAt: { $gte: start, $lt: end }
}).sort({ createdAt: -1 })
```

Index:

```javascript
db.auditLogs.createIndex({ tenantId: 1, createdAt: -1 })
```

### Case Study 4: Cursor Pagination

Index:

```javascript
db.posts.createIndex({ tenantId: 1, createdAt: -1, _id: -1 })
```

Query:

```javascript
db.posts.find({
  tenantId: "t1",
  $or: [
    { createdAt: { $lt: lastCreatedAt } },
    { createdAt: lastCreatedAt, _id: { $lt: lastId } }
  ]
}).sort({ createdAt: -1, _id: -1 }).limit(20)
```

### Case Study 5: Dashboard Query

Bad:

```javascript
db.orders.aggregate([
  { $match: { tenantId: "t1" } },
  { $group: { _id: "$status", count: { $sum: 1 }, revenue: { $sum: "$totalCents" } } }
])
```

This may scan too many orders repeatedly.

Better:

```javascript
{
  _id: "t1:2026-07-01:PAID",
  tenantId: "t1",
  day: ISODate("2026-07-01"),
  status: "PAID",
  count: 12093,
  revenueCents: 99444011
}
```

Index:

```javascript
db.orderDailyStats.createIndex({ tenantId: 1, day: -1, status: 1 })
```

### Index Anti-Patterns

| Anti-Pattern | Why Bad | Fix |
|---|---|---|
| Index every field | Write and memory overhead | Index query patterns |
| Duplicate indexes | Waste storage and writes | Audit with `getIndexes()` |
| Wrong compound order | Cannot serve filter/sort | Use ESR and explain |
| Low-cardinality standalone index | Poor selectivity | Combine with selective fields |
| Deep skip pagination | Scans skipped records | Cursor pagination |
| Regex contains search | Cannot use normal index well | Atlas Search |
| Huge wildcard index | Memory/storage pressure | Target hot attributes |

---

---
