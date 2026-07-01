# Indexing Exercises

Run seed data first:

```bash
docker compose up -d
```

## Exercise 1: Tenant Email Lookup

Query:

```javascript
db.users.findOne({ tenantId: 't1', email: 'asha@example.com' })
```

Task:

1. Run explain.
2. Create a unique compound index.
3. Run explain again.

Expected index:

```javascript
db.users.createIndex({ tenantId: 1, email: 1 }, { unique: true })
```

## Exercise 2: Order List by Status

Query:

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' }).sort({ createdAt: -1 }).limit(20)
```

Task: create an ESR-friendly index.

Expected:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

## Exercise 3: Customer Order History

Query:

```javascript
db.orders.find({ tenantId: 't1', customerId: 'u1' }).sort({ createdAt: -1 })
```

Expected:

```javascript
db.orders.createIndex({ tenantId: 1, customerId: 1, createdAt: -1 })
```

## Exercise 4: Product Browse

Query:

```javascript
db.products.find({ tenantId: 't1', categoryId: 'cat-keyboards' }).sort({ priceCents: 1 })
```

Expected:

```javascript
db.products.createIndex({ tenantId: 1, categoryId: 1, priceCents: 1 })
```

## Exercise 5: Audit Log Search

Query:

```javascript
db.auditLogs.find({ tenantId: 't1', actorId: 'u1' }).sort({ createdAt: -1 })
```

Expected:

```javascript
db.auditLogs.createIndex({ tenantId: 1, actorId: 1, createdAt: -1 })
```

## Reflection Questions

- Which indexes are unique and why?
- Which indexes support sort?
- Which indexes may be unnecessary as data or query patterns change?
- What is the write cost of every index you added?
