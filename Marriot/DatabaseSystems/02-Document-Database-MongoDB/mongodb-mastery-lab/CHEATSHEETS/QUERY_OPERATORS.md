# Query Operator Cheat Sheet

## Comparison

```javascript
{ status: { $eq: 'PAID' } }
{ status: { $ne: 'CANCELLED' } }
{ totalCents: { $gt: 10000 } }
{ totalCents: { $gte: 10000, $lt: 50000 } }
{ status: { $in: ['PAID', 'SHIPPED'] } }
{ status: { $nin: ['CANCELLED', 'REFUNDED'] } }
```

Negative predicates such as `$ne` and `$nin` often have weak selectivity.

## Logical

```javascript
{ tenantId: 't1', status: 'PAID' }

{ $or: [{ email: 'a@example.com' }, { phone: '+15551234567' }] }

{ $and: [{ tenantId: 't1' }, { totalCents: { $gte: 10000 } }] }

{ $nor: [{ status: 'DELETED' }, { archived: true }] }
```

For `$or`, each branch should have useful index support.

## Arrays

```javascript
{ tags: 'wireless' }
{ tags: { $all: ['wireless', 'keyboard'] } }
{ roles: { $size: 2 } }
{ items: { $elemMatch: { sku: 'SKU-1', quantity: { $gte: 2 } } } }
```

Use `$elemMatch` when multiple predicates must match the same array element.

## Nested Documents

```javascript
{ 'profile.city': 'Dallas' }
{ 'shippingAddress.state': 'TX' }
```

Prefer dot notation over exact embedded document matches unless the full object shape and field order are intentional.

## Element

```javascript
{ deletedAt: { $exists: false } }
{ payloadVersion: { $type: 'int' } }
```

## Regex

```javascript
{ name: { $regex: /^wireless/i } }
```

Regex rules:

- Prefix regex may use an index in some cases.
- Contains regex usually cannot use a normal index efficiently.
- Use Atlas Search for serious text search, autocomplete, fuzzy search, scoring, and facets.
