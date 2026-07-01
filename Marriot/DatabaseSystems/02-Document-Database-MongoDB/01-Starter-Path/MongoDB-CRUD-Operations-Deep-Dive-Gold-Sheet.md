    # MongoDB CRUD Operations Deep Dive - Gold Sheet

    > **Track File #4 of 28 - Group 01: Starter Path**
    > For: backend/database/system design interviews | Level: beginner to production-aware | Mode: daily backend operations with performance notes

    This sheet builds:
    - insertOne, insertMany, ordered/unordered writes
- find, projection, sorting, pagination
- update operators, upsert, delete patterns

Original master-map sections included here:
- 4. CRUD Operations Deep Dive

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 4. CRUD Operations Deep Dive

### Create

#### `insertOne`

```javascript
db.users.insertOne({
  email: "asha@example.com",
  name: "Asha",
  roles: ["USER"],
  createdAt: new Date()
})
```

Why: inserts one document. MongoDB guarantees atomicity for the inserted document.

#### `insertMany`

```javascript
db.products.insertMany([
  { sku: "SKU-1", name: "Keyboard", price: 79.99 },
  { sku: "SKU-2", name: "Mouse", price: 39.99 }
])
```

#### Ordered vs Unordered Inserts

Ordered insert stops at first error:

```javascript
db.products.insertMany(docs, { ordered: true })
```

Unordered continues after errors where possible:

```javascript
db.products.insertMany(docs, { ordered: false })
```

Use unordered for bulk ingestion when one bad document should not block the batch.

Common mistakes:

| Mistake | Why Bad | Fix |
|---|---|---|
| Inserting inconsistent field types | Breaks queries and indexes | Validate schema |
| Inserting huge arrays | Document growth and 16 MB risk | Reference or bucket |
| No unique indexes | Duplicate business entities | Add unique constraints |

### Read

#### `find`

```javascript
db.users.find({ roles: "USER" })
```

#### `findOne`

```javascript
db.users.findOne({ email: "asha@example.com" })
```

#### Filters

```javascript
db.orders.find({
  tenantId: "t1",
  status: "PAID",
  createdAt: { $gte: ISODate("2026-07-01T00:00:00Z") }
})
```

#### Projections

Projection reduces network and memory cost:

```javascript
db.users.find(
  { tenantId: "t1" },
  { email: 1, name: 1, _id: 0 }
)
```

#### Sort, Limit, Skip

```javascript
db.orders.find({ tenantId: "t1" })
  .sort({ createdAt: -1 })
  .limit(20)
```

Avoid deep skip pagination:

```javascript
// Bad for page 10000
db.orders.find({ tenantId: "t1" }).sort({ createdAt: -1 }).skip(200000).limit(20)
```

Use cursor pagination:

```javascript
db.orders.find({
  tenantId: "t1",
  $or: [
    { createdAt: { $lt: lastCreatedAt } },
    { createdAt: lastCreatedAt, _id: { $lt: lastId } }
  ]
}).sort({ createdAt: -1, _id: -1 }).limit(20)
```

Index:

```javascript
db.orders.createIndex({ tenantId: 1, createdAt: -1, _id: -1 })
```

### Update

#### `updateOne`

```javascript
db.users.updateOne(
  { email: "asha@example.com" },
  { $set: { lastLoginAt: new Date() } }
)
```

#### `updateMany`

```javascript
db.users.updateMany(
  { tenantId: "t1", active: true },
  { $set: { plan: "standard" } }
)
```

#### `replaceOne`

Replaces the whole document except `_id`:

```javascript
db.users.replaceOne(
  { _id: ObjectId("64f000000000000000000001") },
  { email: "new@example.com", name: "New Name", createdAt: new Date() }
)
```

Use carefully because omitted fields are removed.

#### Upsert

```javascript
db.userSettings.updateOne(
  { userId: "u1" },
  {
    $set: { theme: "dark", updatedAt: new Date() },
    $setOnInsert: { createdAt: new Date() }
  },
  { upsert: true }
)
```

### Update Operators

| Operator | Use | Example |
|---|---|---|
| `$set` | Set field | `{ $set: { status: "PAID" } }` |
| `$unset` | Remove field | `{ $unset: { temp: "" } }` |
| `$inc` | Increment numeric field | `{ $inc: { loginCount: 1 } }` |
| `$push` | Append to array | `{ $push: { tags: "new" } }` |
| `$pull` | Remove matching array values | `{ $pull: { tags: "old" } }` |
| `$addToSet` | Add if absent | `{ $addToSet: { roles: "ADMIN" } }` |
| `$pop` | Remove first/last array element | `{ $pop: { messages: -1 } }` |
| `$rename` | Rename field | `{ $rename: { fullname: "fullName" } }` |
| `$currentDate` | Set current date | `{ $currentDate: { updatedAt: true } }` |

Examples:

```javascript
db.accounts.updateOne(
  { _id: "acct1" },
  { $inc: { balanceCents: -5000 }, $currentDate: { updatedAt: true } }
)
```

```javascript
db.posts.updateOne(
  { _id: postId },
  { $addToSet: { likedByUserIds: userId } }
)
```

```javascript
db.posts.updateOne(
  { _id: postId },
  { $pull: { likedByUserIds: userId } }
)
```

### Delete

#### `deleteOne`

```javascript
db.sessions.deleteOne({ _id: sessionId })
```

#### `deleteMany`

```javascript
db.sessions.deleteMany({ expiresAt: { $lt: new Date() } })
```

Performance notes:

- Deletes need indexes too.
- Large deletes can create replication lag and I/O spikes.
- Prefer TTL indexes for automatic expiration when appropriate.
- For massive purges, delete in batches or archive by collection/time partition.

---

---
