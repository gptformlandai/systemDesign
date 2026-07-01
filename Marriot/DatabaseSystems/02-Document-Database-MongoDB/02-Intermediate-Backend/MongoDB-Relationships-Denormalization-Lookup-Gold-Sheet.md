    # MongoDB Relationships, Denormalization and Lookup - Gold Sheet

    > **Track File #7 of 28 - Group 02: Intermediate Backend**
    > For: backend/database/system design interviews | Level: intermediate backend | Mode: relationship modeling without default SQL joins

    This sheet builds:
    - One-to-one, one-to-many, many-to-many, graph-like relationships
- $lookup acceptability and danger zones
- Consistency implications of duplication

Original master-map sections included here:
- 7. Relationships in MongoDB

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 7. Relationships in MongoDB

MongoDB handles relationships with embedding, references, `$lookup`, denormalization, and application-side joins. The right choice depends on access pattern and growth.

### One-to-One

Embed when owned and read together:

```javascript
{
  _id: "u1",
  email: "asha@example.com",
  profile: {
    displayName: "Asha",
    avatarUrl: "https://cdn.example.com/a.png"
  }
}
```

Reference when lifecycle differs:

```javascript
{ _id: "u1", email: "asha@example.com" }
{ _id: "profile-u1", userId: "u1", bio: "..." }
```

### One-to-Many

Order to line items: embed because line items are bounded and owned.

```javascript
{
  _id: "order1",
  items: [
    { sku: "SKU-1", quantity: 2, priceCents: 1000 }
  ]
}
```

Product to reviews: reference because reviews are unbounded.

```javascript
{ _id: "review1", productId: "p1", userId: "u1", rating: 5 }
```

### Many-to-Many

User to role can be embedded if small:

```javascript
{ _id: "u1", roleIds: ["admin", "editor"] }
```

For large many-to-many, use join collection:

```javascript
{ _id: "u1:team9", userId: "u1", teamId: "team9", role: "OWNER" }
```

### Hierarchical Relationships

Category tree:

```javascript
{
  _id: "cat-keyboards",
  parentId: "cat-accessories",
  ancestors: ["cat-electronics", "cat-accessories"]
}
```

Index:

```javascript
db.categories.createIndex({ parentId: 1 })
db.categories.createIndex({ ancestors: 1 })
```

### Graph-Like Relationships

Social followers are usually separate edge documents:

```javascript
{ followerId: "u1", followeeId: "u2", createdAt: ISODate("2026-07-01") }
```

Indexes:

```javascript
db.follows.createIndex({ followerId: 1, createdAt: -1 })
db.follows.createIndex({ followeeId: 1, createdAt: -1 })
db.follows.createIndex({ followerId: 1, followeeId: 1 }, { unique: true })
```

### Inventory Reservation Model

Inventory reservation often needs atomic conditional updates:

```javascript
db.inventory.updateOne(
  { sku: "SKU-1", available: { $gte: 2 } },
  {
    $inc: { available: -2, reserved: 2 },
    $push: { reservations: { orderId: "o1", quantity: 2, expiresAt: expiry } }
  }
)
```

If reservations can become unbounded, store reservations separately and keep counters on inventory.

### `$lookup`: When Acceptable

Acceptable:

- admin screens
- low-volume reports
- joining small bounded result sets
- aggregation pipelines with indexed foreign keys
- read models where embedding would be wrong

Example:

```javascript
db.orders.aggregate([
  { $match: { tenantId: "t1", status: "PAID" } },
  { $limit: 50 },
  {
    $lookup: {
      from: "users",
      localField: "userId",
      foreignField: "_id",
      as: "user"
    }
  },
  { $unwind: "$user" }
])
```

Dangerous:

- huge input sets before `$lookup`
- unindexed foreign fields
- multi-hop joins mimicking SQL
- latency-sensitive API paths with unpredictable cardinality
- sharded joins without understanding routing behavior

### Denormalization Tradeoffs

| Benefit | Cost |
|---|---|
| Fewer reads | Duplicated data |
| Faster APIs | Stale copies |
| Better locality | More write logic |
| Simpler scaling | Reconciliation jobs needed |

Rule: duplicate data that is stable, small, and read often. Reference data that is large, volatile, or independently owned.

---

---
