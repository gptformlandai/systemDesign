    # MongoDB Query Language and Operators - Gold Sheet

    > **Track File #5 of 28 - Group 01: Starter Path**
    > For: backend/database/system design interviews | Level: beginner to intermediate | Mode: filters, nested documents, arrays, regex, query correctness

    This sheet builds:
    - Comparison, logical, array, element, and regex operators
- Dot notation and exact embedded document matches
- Array-of-object query traps

Original master-map sections included here:
- 5. Query Language

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 5. Query Language

MongoDB queries are JSON-like predicate documents. The query language is expressive, but performance depends on index compatibility.

### Comparison Operators

```javascript
// $eq
db.products.find({ status: { $eq: "ACTIVE" } })
db.products.find({ status: "ACTIVE" })

// $ne
db.products.find({ status: { $ne: "DELETED" } })

// $gt / $gte / $lt / $lte
db.orders.find({ totalCents: { $gte: 5000, $lt: 10000 } })

// $in
db.orders.find({ status: { $in: ["PAID", "SHIPPED"] } })

// $nin
db.orders.find({ status: { $nin: ["CANCELLED", "REFUNDED"] } })
```

Performance notes:

- Equality predicates are usually index-friendly.
- Negative predicates like `$ne` and `$nin` are often less selective.
- Large `$in` lists can become expensive; consider batching or different model.

### Logical Operators

```javascript
// $and is usually implicit
db.orders.find({ tenantId: "t1", status: "PAID" })

db.orders.find({
  $and: [
    { tenantId: "t1" },
    { totalCents: { $gte: 10000 } }
  ]
})

// $or
db.users.find({
  $or: [
    { email: "asha@example.com" },
    { phone: "+15551234567" }
  ]
})

// $not
db.products.find({ price: { $not: { $gt: 100 } } })

// $nor
db.products.find({
  $nor: [
    { status: "DELETED" },
    { archived: true }
  ]
})
```

For `$or`, each branch should ideally be indexed.

### Array Operators

```javascript
// Array contains all values
db.products.find({ tags: { $all: ["wireless", "keyboard"] } })

// Array has exact size
db.users.find({ roles: { $size: 2 } })

// Match conditions on same array element
db.orders.find({
  items: {
    $elemMatch: {
      sku: "SKU-1",
      quantity: { $gte: 2 }
    }
  }
})
```

Why `$elemMatch` matters:

```javascript
// Can match sku in one item and quantity in another item, which may be wrong
db.orders.find({ "items.sku": "SKU-1", "items.quantity": { $gte: 2 } })

// Requires both conditions on the same item
db.orders.find({ items: { $elemMatch: { sku: "SKU-1", quantity: { $gte: 2 } } } })
```

### Element Operators

```javascript
// Field exists
db.users.find({ deletedAt: { $exists: false } })

// Type check
db.events.find({ payloadVersion: { $type: "int" } })
```

Use `$exists` carefully. A sparse or partial index may help for optional fields.

### Regex

```javascript
db.products.find({ name: { $regex: /^iphone/i } })
```

Regex performance:

- Prefix regex like `/^abc/` can use an index in some cases.
- Contains regex like `/abc/` usually cannot use a normal index efficiently.
- Case-insensitive regex can be expensive depending on collation and pattern.
- For serious search, use Atlas Search or Elasticsearch.

### Nested Documents and Dot Notation

```javascript
db.users.find({ "address.city": "Dallas" })
```

Exact embedded document match is order-sensitive in practice and brittle:

```javascript
// Avoid depending on exact embedded object shape unless intentional
db.users.find({ address: { city: "Dallas", state: "TX" } })
```

Prefer dot notation:

```javascript
db.users.find({ "address.city": "Dallas", "address.state": "TX" })
```

### Array of Objects Query

```javascript
db.products.find({
  variants: {
    $elemMatch: {
      color: "black",
      size: "M",
      inventory: { $gt: 0 }
    }
  }
})
```

Index idea:

```javascript
db.products.createIndex({ "variants.color": 1, "variants.size": 1, "variants.inventory": 1 })
```

Remember: array indexes become multikey indexes, with special limitations for compound indexes involving multiple array fields.

---

---
