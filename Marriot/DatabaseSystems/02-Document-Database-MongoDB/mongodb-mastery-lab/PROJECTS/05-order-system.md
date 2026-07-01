# Project 05: Order System

Difficulty: Intermediate

Build an order service with order creation, status transitions, order history, payment/shipping snapshots, reporting, and transaction discussion.

---

## Goal

Practice aggregate modeling, embedded line items, immutable snapshots, status history, compound indexes, and consistency decisions around checkout.

---

## Schema Design

Embed line items because order detail reads need them and they are bounded. Snapshot product, price, address, and payment metadata for historical correctness.

```javascript
{
  _id: 'ord_1001',
  tenantId: 'tenant_shop',
  orderNumber: 'ORD-2026-0001',
  customerId: 'usr_1001',
  status: 'PAID',
  items: [
    { sku: 'KEY-001', productId: 'prod_keyboard_1', titleSnapshot: 'Mechanical Keyboard', quantity: 1, unitPriceCents: 8999 }
  ],
  totals: { subtotalCents: 8999, taxCents: 720, shippingCents: 0, totalCents: 9719 },
  shippingAddress: { city: 'Dallas', state: 'TX', country: 'US' },
  payment: { provider: 'stripe', paymentIntentId: 'pi_123', status: 'CAPTURED' },
  statusHistory: [{ status: 'PAID', at: ISODate('2026-07-01T10:00:00Z'), actor: 'system' }],
  createdAt: ISODate('2026-07-01T10:00:00Z'),
  updatedAt: ISODate('2026-07-01T10:00:00Z')
}
```

---

## Sample Data

```javascript
db.orders.insertMany([
  {
    _id: 'ord_1001', tenantId: 'tenant_shop', orderNumber: 'ORD-2026-0001', customerId: 'usr_1001', status: 'PAID',
    items: [{ sku: 'KEY-001', productId: 'prod_keyboard_1', titleSnapshot: 'Mechanical Keyboard', quantity: 1, unitPriceCents: 8999 }],
    totals: { subtotalCents: 8999, taxCents: 720, shippingCents: 0, totalCents: 9719 }, shippingAddress: { city: 'Dallas', state: 'TX', country: 'US' },
    payment: { provider: 'stripe', paymentIntentId: 'pi_123', status: 'CAPTURED' }, statusHistory: [{ status: 'PAID', at: new Date(), actor: 'system' }], createdAt: new Date(), updatedAt: new Date()
  },
  {
    _id: 'ord_1002', tenantId: 'tenant_shop', orderNumber: 'ORD-2026-0002', customerId: 'usr_1002', status: 'SHIPPED',
    items: [{ sku: 'MOU-001', productId: 'prod_mouse_1', titleSnapshot: 'Wireless Mouse', quantity: 2, unitPriceCents: 3999 }],
    totals: { subtotalCents: 7998, taxCents: 640, shippingCents: 500, totalCents: 9138 }, shippingAddress: { city: 'Austin', state: 'TX', country: 'US' },
    payment: { provider: 'stripe', paymentIntentId: 'pi_456', status: 'CAPTURED' }, statusHistory: [{ status: 'PAID', at: new Date(), actor: 'system' }, { status: 'SHIPPED', at: new Date(), actor: 'ops' }], createdAt: new Date(), updatedAt: new Date()
  }
])
```

---

## CRUD Operations

Create order:

```javascript
db.orders.insertOne({
  _id: 'ord_1003', tenantId: 'tenant_shop', orderNumber: 'ORD-2026-0003', customerId: 'usr_1001', status: 'CREATED',
  items: [{ sku: 'MON-001', productId: 'prod_monitor_1', titleSnapshot: '27 Inch Monitor', quantity: 1, unitPriceCents: 24999 }],
  totals: { subtotalCents: 24999, taxCents: 2000, shippingCents: 0, totalCents: 26999 }, shippingAddress: { city: 'Dallas', state: 'TX', country: 'US' },
  payment: { provider: 'stripe', paymentIntentId: null, status: 'PENDING' }, statusHistory: [{ status: 'CREATED', at: new Date(), actor: 'usr_1001' }], createdAt: new Date(), updatedAt: new Date()
})
```

Get customer order history:

```javascript
db.orders.find({ tenantId: 'tenant_shop', customerId: 'usr_1001' }).sort({ createdAt: -1 }).limit(20)
```

Status transition:

```javascript
db.orders.updateOne(
  { tenantId: 'tenant_shop', _id: 'ord_1003', status: 'CREATED' },
  { $set: { status: 'PAID', 'payment.status': 'CAPTURED', updatedAt: new Date() }, $push: { statusHistory: { status: 'PAID', at: new Date(), actor: 'system' } } }
)
```

Cancel order:

```javascript
db.orders.updateOne(
  { tenantId: 'tenant_shop', _id: 'ord_1003', status: { $in: ['CREATED', 'PAID'] } },
  { $set: { status: 'CANCELLED', updatedAt: new Date() }, $push: { statusHistory: { status: 'CANCELLED', at: new Date(), actor: 'usr_1001' } } }
)
```

---

## Indexes

```javascript
db.orders.createIndex({ tenantId: 1, orderNumber: 1 }, { unique: true })
db.orders.createIndex({ tenantId: 1, customerId: 1, createdAt: -1 })
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
db.orders.createIndex({ tenantId: 1, 'items.sku': 1, createdAt: -1 })
```

---

## Aggregation Queries

Revenue by day:

```javascript
db.orders.aggregate([
  { $match: { tenantId: 'tenant_shop', status: { $in: ['PAID', 'SHIPPED'] } } },
  { $group: { _id: { $dateTrunc: { date: '$createdAt', unit: 'day' } }, orders: { $sum: 1 }, revenueCents: { $sum: '$totals.totalCents' } } },
  { $sort: { _id: -1 } }
])
```

Top SKUs:

```javascript
db.orders.aggregate([
  { $match: { tenantId: 'tenant_shop', status: { $ne: 'CANCELLED' } } },
  { $unwind: '$items' },
  { $group: { _id: '$items.sku', units: { $sum: '$items.quantity' }, revenueCents: { $sum: { $multiply: ['$items.quantity', '$items.unitPriceCents'] } } } },
  { $sort: { revenueCents: -1 } }
])
```

---

## Performance Considerations

- Store immutable snapshots to avoid product joins on order history.
- Use cursor pagination for customer orders.
- Avoid unbounded `statusHistory`; for noisy workflows, move events to an order events collection.
- Use `explain()` on status dashboard queries.

---

## Scaling Considerations

- Shard by `{ tenantId: 1, orderNumber: 1 }` or `{ tenantId: 1, customerId: 1 }` depending hot reads.
- Use outbox events for order-created, paid, shipped, cancelled.
- Precompute dashboards into daily summary collections.
- Use transactions carefully for checkout inventory/payment invariants.

---

## Security Considerations

- Users can only read their own orders unless privileged.
- Do not store raw card data.
- Audit manual status changes.
- Use idempotency keys for payment callbacks.

---

## Optional API Layer

- `POST /orders`
- `GET /orders/{orderId}`
- `GET /customers/{customerId}/orders?cursor=`
- `POST /orders/{orderId}/cancel`
- `POST /webhooks/payment-captured`

---

## Interview Discussion Points

- Why embed line items in orders?
- Why snapshot prices and addresses?
- When is a transaction needed during checkout?
- Which shard key supports order lookup and history?
- How do you debug a slow order-history endpoint?
