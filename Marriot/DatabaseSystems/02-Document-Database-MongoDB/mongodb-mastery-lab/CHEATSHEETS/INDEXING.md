# Indexing Cheat Sheet

## Core Commands

```javascript
db.users.createIndex({ tenantId: 1, email: 1 }, { unique: true })
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
db.sessions.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
db.orders.getIndexes()
db.orders.dropIndex('tenant_status_created')
db.orders.find(query).sort(sort).explain('executionStats')
```

## ESR Rule

For many compound indexes, start with:

1. Equality fields
2. Sort fields
3. Range fields

Example:

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' })
  .sort({ createdAt: -1 })
  .limit(20)
```

Index:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

## Explain Plan Fields

| Field | Meaning | Good Sign |
|---|---|---|
| `IXSCAN` | Index scan | Hot queries should usually have this |
| `COLLSCAN` | Collection scan | Bad for large hot collections |
| `FETCH` | Fetch documents after index lookup | Expected unless covered query |
| `SORT` | Blocking sort stage | Avoid for large results |
| `keysExamined` | Index keys scanned | Close to returned count |
| `docsExamined` | Documents read | Close to returned count |
| `nReturned` | Returned docs | Compare against docs examined |

## Index Types

| Type | Example | Use |
|---|---|---|
| Single field | `{ email: 1 }` | Simple lookup |
| Compound | `{ tenantId: 1, status: 1 }` | Multi-field filters/sorts |
| Multikey | `{ tags: 1 }` | Arrays |
| Unique | `{ tenantId: 1, email: 1 }` | Business invariant |
| TTL | `{ expiresAt: 1 }` | Expiring data |
| Partial | `{ email: 1 }` with filter | Active docs only |
| Sparse | `{ phone: 1 }` | Only docs with field |
| Text | `{ title: 'text' }` | Basic text search |
| Hashed | `{ userId: 'hashed' }` | Distribution/sharding |
| Geospatial | `{ location: '2dsphere' }` | Location queries |
| Wildcard | `{ 'attributes.$**': 1 }` | Dynamic attributes |

## Anti-Patterns

- Indexing every field.
- Keeping duplicate indexes.
- Low-cardinality standalone indexes for hot queries.
- Wrong compound index order.
- Using regex contains search with normal indexes.
- Deep pagination with `skip`.
- Ignoring sort support.
