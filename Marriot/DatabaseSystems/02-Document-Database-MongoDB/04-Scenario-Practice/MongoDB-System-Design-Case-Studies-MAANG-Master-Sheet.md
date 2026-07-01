    # MongoDB System Design Case Studies - MAANG Master Sheet

    > **Track File #21 of 28 - Group 04: Scenario Practice**
    > For: backend/database/system design interviews | Level: MAANG system design | Mode: schema, indexes, queries, scaling, failure scenarios

    This sheet builds:
    - 13 system design case studies
- Schema, indexes, consistency, sharding for each
- Catalog, cart, orders, chat, audit, IoT, RAG, SaaS, feed

Original master-map sections included here:
- 22. MongoDB System Design Patterns

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 22. MongoDB System Design Patterns

### 1. Design E-Commerce Catalog

Schema:

```javascript
{
  _id: "p1",
  tenantId: "t1",
  sku: "SKU-1",
  name: "Wireless Keyboard",
  categoryId: "cat-keyboards",
  brand: "Acme",
  priceCents: 7999,
  attributes: [ { k: "color", v: "black" }, { k: "layout", v: "US" } ],
  variants: [ { sku: "SKU-1-BLK", color: "black", inventoryStatus: "IN_STOCK" } ],
  searchText: "wireless keyboard black",
  averageRating: 4.7,
  reviewCount: 1842,
  updatedAt: ISODate("2026-07-01")
}
```

Indexes:

```javascript
db.products.createIndex({ tenantId: 1, sku: 1 }, { unique: true })
db.products.createIndex({ tenantId: 1, categoryId: 1, priceCents: 1 })
db.products.createIndex({ tenantId: 1, brand: 1 })
db.products.createIndex({ tenantId: 1, "attributes.k": 1, "attributes.v": 1 })
```

Scaling: shard by `{ tenantId, sku }` for multi-tenant catalog; use Atlas Search for product search.

Consistency: price and inventory may be separate services; product stores display snapshot.

Failure: search index lag, stale inventory, product update conflicts.

### 2. Design Shopping Cart

Schema:

```javascript
{
  _id: "cart-u1",
  tenantId: "t1",
  userId: "u1",
  items: [
    { sku: "SKU-1", quantity: 2, priceSnapshotCents: 7999, addedAt: ISODate("2026-07-01") }
  ],
  updatedAt: ISODate("2026-07-01")
}
```

Indexes:

```javascript
db.carts.createIndex({ tenantId: 1, userId: 1 }, { unique: true })
db.carts.createIndex({ updatedAt: 1 }, { expireAfterSeconds: 60 * 60 * 24 * 90 })
```

Queries: get cart by user, update item quantity atomically.

Scaling: cart is single-document aggregate. For very large carts, cap item count.

Consistency: price revalidated at checkout.

Failure: duplicate add requests handled with idempotency or `$inc`.

### 3. Design Order Management

Schema: order embeds immutable line items and snapshots.

Indexes:

```javascript
db.orders.createIndex({ tenantId: 1, orderId: 1 }, { unique: true })
db.orders.createIndex({ tenantId: 1, customerId: 1, createdAt: -1 })
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

Scaling: shard by `{ tenantId, orderId }` or region-aware key.

Consistency: transactions or saga for order/payment/inventory.

Failure: payment timeout, inventory expiration, duplicate order submission.

### 4. Design Chat Application

Schema:

```javascript
{
  _id: ObjectId("..."),
  conversationId: "c1",
  bucketId: "c1:2026-07-01",
  senderId: "u1",
  text: "Hello",
  createdAt: ISODate("2026-07-01T10:00:00Z"),
  status: "SENT"
}
```

Indexes:

```javascript
db.messages.createIndex({ conversationId: 1, createdAt: -1, _id: -1 })
db.messages.createIndex({ senderId: 1, createdAt: -1 })
```

Scaling: shard by `{ conversationId, bucketId }`; large group chats may need bucket or hashed suffix.

Consistency: messages append-only; read receipts separate.

Failure: duplicate send handled by client message ID unique index.

### 5. Design Notification System

Schema:

```javascript
{
  _id: "notif1",
  tenantId: "t1",
  userId: "u1",
  type: "ORDER_SHIPPED",
  title: "Order shipped",
  readAt: null,
  createdAt: ISODate("2026-07-01")
}
```

Indexes:

```javascript
db.notifications.createIndex({ tenantId: 1, userId: 1, readAt: 1, createdAt: -1 })
db.notifications.createIndex({ createdAt: 1 }, { expireAfterSeconds: 60 * 60 * 24 * 365 })
```

Scaling: shard by user/tenant.

Consistency: at-least-once event processing; idempotency key prevents duplicates.

### 6. Design Audit Log System

Schema:

```javascript
{
  _id: ObjectId("..."),
  tenantId: "t1",
  actorId: "u1",
  action: "USER_ROLE_CHANGED",
  target: { type: "USER", id: "u2" },
  ip: "203.0.113.1",
  metadata: { oldRole: "USER", newRole: "ADMIN" },
  createdAt: ISODate("2026-07-01T10:00:00Z")
}
```

Indexes:

```javascript
db.auditLogs.createIndex({ tenantId: 1, createdAt: -1 })
db.auditLogs.createIndex({ tenantId: 1, actorId: 1, createdAt: -1 })
db.auditLogs.createIndex({ tenantId: 1, "target.type": 1, "target.id": 1, createdAt: -1 })
```

Scaling: append-only, shard by tenant plus time or hashed tenant if needed.

Failure: write path should be resilient; critical audit may require majority writes.

### 7. Design IoT Telemetry Platform

Use time-series collection.

Indexes/query: metadata + time.

Scaling: shard by device/tenant depending volume.

Consistency: tolerate delayed events; use ingestion timestamps and measurement timestamps separately.

Failure: device offline, late arrival, duplicate readings, hot devices.

### 8. Design Event Store

Schema:

```javascript
{
  _id: ObjectId("..."),
  aggregateId: "order1",
  sequence: 18,
  type: "ORDER_CONFIRMED",
  payload: {},
  createdAt: ISODate("2026-07-01")
}
```

Indexes:

```javascript
db.events.createIndex({ aggregateId: 1, sequence: 1 }, { unique: true })
db.events.createIndex({ type: 1, createdAt: -1 })
```

Scaling: shard by aggregateId; hot aggregates need mitigation.

Consistency: optimistic concurrency on sequence.

### 9. Design Product Search

Source: `products` in MongoDB.

Search: Atlas Search index or Elasticsearch.

Queries: keyword, filters, facets, sort.

Consistency: search index eventually consistent; product detail reads source of truth.

Failure: fallback to category browse or cached results.

### 10. Design GenAI RAG Document Store

Schema: `ragChunks` with text, embedding, metadata, ACL.

Indexes: tenant/source/chunk unique plus vector index.

Scaling: partition by tenant; archive deleted source docs; re-embed by model version.

Consistency: query must enforce ACL filters before context reaches model.

Failure: embedding job retry, partial ingestion, stale chunks after source update.

### 11. Design Multi-Tenant SaaS

Schema includes `tenantId` on every document.

Indexes start with `tenantId` for tenant-scoped queries.

Shard key avoids tenant-only if tenants are skewed.

Security: tenant filter enforced in repository layer and tests.

Failure: noisy tenant, data leak by missing filter, tenant migration.

### 12. Design Real-Time Analytics Dashboard

Raw events plus summary collection.

Use change streams or stream processor to update summaries.

Indexes:

```javascript
db.dailyStats.createIndex({ tenantId: 1, day: -1, metric: 1 })
```

Consistency: dashboard may lag seconds/minutes.

Failure: projection rebuild from raw events.

### 13. Design Social Feed Backend

Options:

- fanout on read: query posts from followed users
- fanout on write: write feed items to follower feeds
- hybrid: celebrities handled by read fanout, normal users by write fanout

MongoDB schema for feed item:

```javascript
{ userId: "u2", postId: "p1", authorId: "u1", createdAt: ISODate("2026-07-01") }
```

Indexes:

```javascript
db.feedItems.createIndex({ userId: 1, createdAt: -1, _id: -1 })
```

Scaling: shard by `userId`; celebrity fanout requires outlier strategy.

---

---
