# Project 04: Shopping Cart

Difficulty: Intermediate

Build a shopping cart that supports item add/remove, quantity updates, price snapshots, promotions, expiration, and conversion to an order.

---

## Goal

Practice single-document atomicity, embedded cart items, conditional updates, cart expiration, and the boundary between cart, product catalog, and order services.

---

## Schema Design

Use one cart document per active user or session. Embed items because carts are bounded and updated together.

```javascript
{
  _id: 'cart_1001',
  tenantId: 'tenant_shop',
  userId: 'usr_1001',
  status: 'ACTIVE',
  items: [
    {
      sku: 'KEY-001',
      productId: 'prod_keyboard_1',
      titleSnapshot: 'Mechanical Keyboard',
      quantity: 1,
      unitPriceCents: 8999,
      addedAt: ISODate('2026-07-01T10:00:00Z')
    }
  ],
  totals: { subtotalCents: 8999, discountCents: 0, totalCents: 8999 },
  expiresAt: ISODate('2026-07-08T10:00:00Z'),
  createdAt: ISODate('2026-07-01T10:00:00Z'),
  updatedAt: ISODate('2026-07-01T10:00:00Z')
}
```

---

## Sample Data

```javascript
db.carts.insertMany([
  {
    _id: 'cart_1001', tenantId: 'tenant_shop', userId: 'usr_1001', status: 'ACTIVE',
    items: [{ sku: 'KEY-001', productId: 'prod_keyboard_1', titleSnapshot: 'Mechanical Keyboard', quantity: 1, unitPriceCents: 8999, addedAt: new Date() }],
    totals: { subtotalCents: 8999, discountCents: 0, totalCents: 8999 }, expiresAt: ISODate('2026-07-08T10:00:00Z'), createdAt: new Date(), updatedAt: new Date()
  },
  {
    _id: 'cart_1002', tenantId: 'tenant_shop', userId: 'usr_1002', status: 'ACTIVE',
    items: [], totals: { subtotalCents: 0, discountCents: 0, totalCents: 0 }, expiresAt: ISODate('2026-07-08T11:00:00Z'), createdAt: new Date(), updatedAt: new Date()
  }
])
```

---

## CRUD Operations

Create active cart if absent:

```javascript
db.carts.updateOne(
  { tenantId: 'tenant_shop', userId: 'usr_1003', status: 'ACTIVE' },
  { $setOnInsert: { _id: 'cart_1003', items: [], totals: { subtotalCents: 0, discountCents: 0, totalCents: 0 }, expiresAt: ISODate('2026-07-08T10:00:00Z'), createdAt: new Date() }, $set: { updatedAt: new Date() } },
  { upsert: true }
)
```

Add item:

```javascript
db.carts.updateOne(
  { tenantId: 'tenant_shop', _id: 'cart_1001', status: 'ACTIVE', 'items.sku': { $ne: 'MOU-001' } },
  { $push: { items: { sku: 'MOU-001', productId: 'prod_mouse_1', titleSnapshot: 'Wireless Mouse', quantity: 1, unitPriceCents: 3999, addedAt: new Date() } }, $inc: { 'totals.subtotalCents': 3999, 'totals.totalCents': 3999 }, $set: { updatedAt: new Date() } }
)
```

Update quantity:

```javascript
db.carts.updateOne(
  { tenantId: 'tenant_shop', _id: 'cart_1001', 'items.sku': 'KEY-001' },
  { $set: { 'items.$.quantity': 2, updatedAt: new Date() } }
)
```

Checkout transition:

```javascript
db.carts.updateOne(
  { tenantId: 'tenant_shop', _id: 'cart_1001', status: 'ACTIVE' },
  { $set: { status: 'CHECKED_OUT', checkedOutAt: new Date(), updatedAt: new Date() } }
)
```

---

## Indexes

```javascript
db.carts.createIndex({ tenantId: 1, userId: 1, status: 1 })
db.carts.createIndex({ tenantId: 1, updatedAt: -1 })
db.carts.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
```

---

## Aggregation Queries

Abandoned cart value:

```javascript
db.carts.aggregate([
  { $match: { tenantId: 'tenant_shop', status: 'ACTIVE', updatedAt: { $lt: ISODate('2026-07-01T00:00:00Z') } } },
  { $group: { _id: null, carts: { $sum: 1 }, valueCents: { $sum: '$totals.totalCents' } } }
])
```

Most common cart items:

```javascript
db.carts.aggregate([
  { $match: { tenantId: 'tenant_shop', status: 'ACTIVE' } },
  { $unwind: '$items' },
  { $group: { _id: '$items.sku', carts: { $sum: 1 }, units: { $sum: '$items.quantity' } } },
  { $sort: { units: -1 } }
])
```

---

## Performance Considerations

- Keep cart item count bounded.
- Use single-document updates for cart mutation.
- Recalculate totals server-side; do not trust client totals.
- Use TTL for abandoned anonymous carts.

---

## Scaling Considerations

- Shard by `{ tenantId: 1, userId: 1 }` for user-scoped cart lookups.
- Keep cart and inventory reservation concerns separate at high scale.
- Use idempotency keys for checkout requests.
- Convert cart to order with a transaction only if strict cross-document invariants require it.

---

## Security Considerations

- Derive price from product service, not client input.
- Validate product availability before checkout.
- Prevent users from reading other users' carts.
- Audit promotion abuse and suspicious checkout retries.

---

## Optional API Layer

- `GET /cart`
- `POST /cart/items`
- `PATCH /cart/items/{sku}`
- `DELETE /cart/items/{sku}`
- `POST /cart/checkout`

---

## Interview Discussion Points

- Why is a cart a good document aggregate?
- What happens when carts contain hundreds of items?
- How do you prevent price tampering?
- When would checkout need a transaction?
- How would you handle anonymous-to-authenticated cart merge?
