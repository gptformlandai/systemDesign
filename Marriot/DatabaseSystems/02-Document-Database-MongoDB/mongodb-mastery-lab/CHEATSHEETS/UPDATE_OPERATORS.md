# Update Operator Cheat Sheet

| Operator | Purpose | Example |
|---|---|---|
| `$set` | Set or replace field value | `{ $set: { status: 'PAID' } }` |
| `$unset` | Remove field | `{ $unset: { temporaryFlag: '' } }` |
| `$inc` | Increment/decrement number | `{ $inc: { loginCount: 1 } }` |
| `$mul` | Multiply number | `{ $mul: { priceCents: 0.9 } }` |
| `$min` | Set only if new value is smaller | `{ $min: { lowestPriceCents: 7999 } }` |
| `$max` | Set only if new value is larger | `{ $max: { highScore: 9000 } }` |
| `$rename` | Rename field | `{ $rename: { fullname: 'fullName' } }` |
| `$currentDate` | Set field to current date | `{ $currentDate: { updatedAt: true } }` |
| `$setOnInsert` | Set only during upsert insert | `{ $setOnInsert: { createdAt: new Date() } }` |

## Array Updates

| Operator | Purpose | Example |
|---|---|---|
| `$push` | Append value | `{ $push: { tags: 'new' } }` |
| `$addToSet` | Append only if missing | `{ $addToSet: { roles: 'ADMIN' } }` |
| `$pull` | Remove matching values | `{ $pull: { tags: 'old' } }` |
| `$pop` | Remove first or last element | `{ $pop: { messages: -1 } }` |

## Positional Updates

```javascript
db.orders.updateOne(
  { orderId: 'ORD-1001', 'items.sku': 'SKU-MOUSE-1-BLK' },
  { $inc: { 'items.$.quantity': 1 } }
)
```

## Array Filters

```javascript
db.orders.updateOne(
  { orderId: 'ORD-1001' },
  { $set: { 'items.$[item].discountApplied': true } },
  { arrayFilters: [{ 'item.category': 'mice' }] }
)
```

## Conditional Inventory Update

```javascript
db.inventory.updateOne(
  { sku: 'SKU-1', available: { $gte: 2 } },
  { $inc: { available: -2, reserved: 2 }, $currentDate: { updatedAt: true } }
)
```

This pattern prevents overselling without needing a transaction if the invariant is local to one inventory document.
