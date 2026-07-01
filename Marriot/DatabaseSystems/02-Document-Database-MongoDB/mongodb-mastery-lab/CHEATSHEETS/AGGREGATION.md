# Aggregation Cheat Sheet

## Pipeline Mental Model

```text
collection -> stage -> stage -> stage -> result
```

Each stage receives documents and emits documents.

## Common Stages

| Stage | Use |
|---|---|
| `$match` | Filter documents |
| `$project` | Select or compute fields |
| `$set` / `$addFields` | Add computed fields |
| `$unset` | Remove fields |
| `$group` | Aggregate by key |
| `$sort` | Sort |
| `$limit` | Limit |
| `$skip` | Offset, avoid deep pages |
| `$unwind` | Expand arrays |
| `$lookup` | Join collection |
| `$facet` | Multiple sub-pipelines |
| `$bucket` | Fixed ranges |
| `$merge` | Write/update output collection |
| `$out` | Replace output collection |
| `$setWindowFields` | Window functions |
| `$graphLookup` | Recursive traversal |

## Sales Report

```javascript
db.orders.aggregate([
  { $match: { tenantId: 't1', status: { $in: ['PAID', 'SHIPPED'] } } },
  { $unwind: '$items' },
  {
    $group: {
      _id: '$items.category',
      units: { $sum: '$items.quantity' },
      revenueCents: { $sum: { $multiply: ['$items.quantity', '$items.priceCents'] } }
    }
  },
  { $sort: { revenueCents: -1 } }
])
```

## Join After Limiting

```javascript
db.orders.aggregate([
  { $match: { tenantId: 't1' } },
  { $sort: { createdAt: -1 } },
  { $limit: 50 },
  { $lookup: { from: 'users', localField: 'customerId', foreignField: '_id', as: 'customer' } },
  { $unwind: '$customer' }
])
```

## Performance Rules

- Put `$match` early.
- Put `$sort` before `$group` only when it is index-supported or the data is already small.
- Use `$project` to reduce large documents.
- Avoid huge `$lookup` inputs.
- Avoid unbounded `$group` on high-cardinality fields.
- Use `allowDiskUse` as a safety valve, not as the primary design.
- Precompute repeated dashboards with `$merge`.
