# CRUD Cheat Sheet

## Create

```javascript
db.users.insertOne({ tenantId: 't1', email: 'a@example.com', createdAt: new Date() })

db.products.insertMany([
  { tenantId: 't1', sku: 'SKU-1', name: 'Keyboard' },
  { tenantId: 't1', sku: 'SKU-2', name: 'Mouse' }
], { ordered: false })
```

Use unordered inserts for ingestion where one bad document should not stop the whole batch.

## Read

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' })

db.orders.findOne({ tenantId: 't1', orderId: 'ORD-1001' })

db.orders.find(
  { tenantId: 't1' },
  { _id: 0, orderId: 1, status: 1, totalCents: 1 }
).sort({ createdAt: -1 }).limit(20)
```

Use projections to reduce network and memory cost.

## Update

```javascript
db.users.updateOne(
  { tenantId: 't1', email: 'a@example.com' },
  { $set: { name: 'Asha' }, $currentDate: { updatedAt: true } }
)

db.inventory.updateOne(
  { sku: 'SKU-1', available: { $gte: 2 } },
  { $inc: { available: -2, reserved: 2 } }
)
```

## Upsert

```javascript
db.userSettings.updateOne(
  { tenantId: 't1', userId: 'u1' },
  {
    $set: { theme: 'dark', updatedAt: new Date() },
    $setOnInsert: { createdAt: new Date() }
  },
  { upsert: true }
)
```

## Delete

```javascript
db.sessions.deleteOne({ _id: sessionId })
db.sessions.deleteMany({ expiresAt: { $lt: new Date() } })
```

For large cleanup workloads, prefer TTL indexes, archival, or batched deletes.

## Production Rules

| Rule | Why |
|---|---|
| Always include tenant filters in multi-tenant apps | Prevents cross-tenant data leaks |
| Use projections | Reduces payload and memory |
| Avoid deep `skip` | Scans skipped records |
| Use unique indexes for business uniqueness | Prevents duplicate users/orders/SKUs |
| Make writes idempotent where retries are possible | Prevents duplicate side effects |
