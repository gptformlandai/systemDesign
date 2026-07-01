# Performance Cheat Sheet

## Debugging Workflow

1. Capture exact query filter, sort, projection, and limit.
2. Run `explain('executionStats')`.
3. Check `IXSCAN` vs `COLLSCAN`.
4. Compare `keysExamined`, `docsExamined`, and `nReturned`.
5. Check sort stage.
6. Check existing indexes.
7. Add or adjust compound index.
8. Re-test explain plan.
9. Consider schema redesign if the shape is wrong.
10. Monitor after deploy.

## High-Signal Fixes

| Problem | Fix |
|---|---|
| Collection scan | Add matching index |
| Blocking sort | Include sort in compound index |
| Deep skip | Cursor pagination |
| Large documents returned | Projection |
| Repeated dashboard scan | Precompute with `$merge` |
| N+1 queries | Embed summary, batch, or `$lookup` after limit |
| Write slowdown | Drop unused indexes and batch writes |
| Hot counter document | Sharded counters or event aggregation |

## Cursor Pagination

```javascript
db.orders.find({
  tenantId: 't1',
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

## Metrics To Watch

- p95/p99 query latency
- opcounters
- connections
- replication lag
- cache pressure
- disk I/O
- CPU
- slow query count
- index size
- collection growth
