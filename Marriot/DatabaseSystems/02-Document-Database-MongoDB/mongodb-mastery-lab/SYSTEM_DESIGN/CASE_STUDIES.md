# MongoDB System Design Case Studies

## 1. E-Commerce Catalog

Collections:

- `products`
- `reviews`
- `inventory`
- `categories`

Schema strategy:

- product embeds bounded variants and recent reviews
- full reviews are separate
- inventory can be separate if updated frequently

Indexes:

```javascript
db.products.createIndex({ tenantId: 1, sku: 1 }, { unique: true })
db.products.createIndex({ tenantId: 1, categoryId: 1, priceCents: 1 })
db.reviews.createIndex({ tenantId: 1, productId: 1, createdAt: -1 })
```

Failure modes:

- search index lag
- stale inventory display
- product review explosion

## 2. Order Management

Collections:

- `orders`
- `orderEvents`
- `inventory`
- `paymentAttempts`
- `outboxEvents`

Schema strategy:

- order embeds items and address snapshots
- events are append-only
- payment/inventory workflows use saga or transaction depending boundary

Indexes:

```javascript
db.orders.createIndex({ tenantId: 1, orderId: 1 }, { unique: true })
db.orders.createIndex({ tenantId: 1, customerId: 1, createdAt: -1 })
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

## 3. Chat Storage

Collections:

- `conversations`
- `messages`
- `readReceipts`

Schema strategy:

- messages are separate documents
- cursor pagination by conversation/time
- read receipts separate to avoid rewriting messages

Index:

```javascript
db.messages.createIndex({ conversationId: 1, createdAt: -1, _id: -1 })
```

## 4. Audit Logs

Collections:

- `auditLogs`

Schema strategy:

- append-only
- tenant/time indexed
- archive or TTL depending compliance

Indexes:

```javascript
db.auditLogs.createIndex({ tenantId: 1, createdAt: -1 })
db.auditLogs.createIndex({ tenantId: 1, actorId: 1, createdAt: -1 })
```

## 5. RAG Store

Collections:

- `sourceDocuments`
- `ragChunks`
- `ingestionJobs`

Schema strategy:

- chunks store text, embedding, metadata, ACL, model version
- vector index in Atlas
- regular indexes for tenant/source/tag filters

Security:

- enforce tenant and ACL filters before retrieval
- log source chunks used in answers

## 6. Multi-Tenant SaaS

Rules:

- every tenant-owned document has `tenantId`
- unique indexes are tenant-scoped
- repositories enforce tenant filter
- shard key accounts for tenant skew

Failure mode: one noisy tenant can dominate workload. Mitigate with rate limits, shard design, and tenant-level observability.
