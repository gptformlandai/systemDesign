# 03. CRUD and Query Language

## Create

```javascript
db.users.insertOne({ tenantId: 't1', email: 'a@example.com', createdAt: new Date() })

db.products.insertMany([
  { tenantId: 't1', sku: 'SKU-1', name: 'Keyboard' },
  { tenantId: 't1', sku: 'SKU-2', name: 'Mouse' }
], { ordered: false })
```

`ordered: false` is useful for bulk ingestion where one duplicate should not block the whole batch.

## Read

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' })
  .sort({ createdAt: -1 })
  .limit(20)
```

Projection:

```javascript
db.orders.find(
  { tenantId: 't1' },
  { _id: 0, orderId: 1, status: 1, totalCents: 1 }
)
```

## Update

```javascript
db.users.updateOne(
  { tenantId: 't1', email: 'a@example.com' },
  { $set: { name: 'Asha' }, $currentDate: { updatedAt: true } }
)
```

Upsert:

```javascript
db.settings.updateOne(
  { tenantId: 't1', userId: 'u1' },
  { $set: { theme: 'dark' }, $setOnInsert: { createdAt: new Date() } },
  { upsert: true }
)
```

## Delete

```javascript
db.sessions.deleteMany({ expiresAt: { $lt: new Date() } })
```

For expiration, prefer TTL indexes where possible.

## Query Operators

Comparison:

```javascript
{ totalCents: { $gte: 10000, $lt: 50000 } }
{ status: { $in: ['PAID', 'SHIPPED'] } }
```

Logical:

```javascript
{ $or: [{ email: 'a@example.com' }, { phone: '+15551234567' }] }
```

Arrays:

```javascript
{ tags: { $all: ['wireless', 'keyboard'] } }
{ items: { $elemMatch: { sku: 'SKU-1', quantity: { $gte: 2 } } } }
```

Nested fields:

```javascript
{ 'profile.city': 'Dallas' }
```

## Pagination

Bad for deep pages:

```javascript
db.orders.find({ tenantId: 't1' }).sort({ createdAt: -1 }).skip(100000).limit(20)
```

Better:

```javascript
db.orders.find({
  tenantId: 't1',
  $or: [
    { createdAt: { $lt: lastCreatedAt } },
    { createdAt: lastCreatedAt, _id: { $lt: lastId } }
  ]
}).sort({ createdAt: -1, _id: -1 }).limit(20)
```

## Production Notes

- Always index hot filters and sorts.
- Always cap result sizes.
- Never expose arbitrary user query documents directly to MongoDB.
- Keep tenant filters mandatory in multi-tenant systems.
