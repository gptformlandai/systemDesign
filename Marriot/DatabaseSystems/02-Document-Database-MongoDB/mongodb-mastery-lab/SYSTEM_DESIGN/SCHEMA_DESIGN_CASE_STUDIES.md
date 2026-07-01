# Schema Design Case Studies

Use these to practice the most important MongoDB design skill: shaping documents around access patterns.

---

## Case Study 1: Order With Line Items

## Access Patterns

- Get order by `tenantId + orderId`.
- Show order details with line items.
- List customer orders by newest first.
- Update order status.

## Recommended Model

Embed line items because they are bounded, owned by the order, and read with the order.

```javascript
{
  tenantId: 't1',
  orderId: 'ORD-1001',
  customerId: 'u1',
  status: 'PAID',
  items: [
    { sku: 'SKU-1', productName: 'Keyboard', quantity: 1, priceCents: 7999 }
  ],
  totalCents: 7999,
  createdAt: new Date()
}
```

## Indexes

```javascript
db.orders.createIndex({ tenantId: 1, orderId: 1 }, { unique: true })
db.orders.createIndex({ tenantId: 1, customerId: 1, createdAt: -1 })
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

## Why

The order detail page needs the full aggregate. Embedding avoids extra queries and gives single-document atomicity for order-level updates.

## Avoid

Do not reference every line item unless line items are huge, shared, or independently updated.

---

## Case Study 2: Product Reviews

## Access Patterns

- Show product detail with average rating and a few recent reviews.
- Paginate all reviews.
- Add a review.
- Filter reviews by rating/time.

## Recommended Model

Use subset pattern: product stores summary and recent reviews; full reviews live separately.

```javascript
{
  _id: 'p1',
  tenantId: 't1',
  name: 'Wireless Keyboard',
  averageRating: 4.7,
  reviewCount: 1842,
  recentReviews: [
    { reviewId: 'r1', rating: 5, text: 'Excellent', createdAt: new Date() }
  ]
}
```

```javascript
{
  _id: 'r1',
  tenantId: 't1',
  productId: 'p1',
  userId: 'u1',
  rating: 5,
  text: 'Excellent',
  createdAt: new Date()
}
```

## Indexes

```javascript
db.reviews.createIndex({ tenantId: 1, productId: 1, createdAt: -1 })
db.reviews.createIndex({ tenantId: 1, productId: 1, rating: 1 })
```

## Why

Reviews are unbounded. Embedding all reviews in product risks document bloat and the 16 MB limit.

---

## Case Study 3: User Roles in SaaS

## Access Patterns

- Check a user's roles during request authorization.
- List users in a tenant.
- Change a user's role.

## Recommended Model

For small role sets, embed role IDs in the user document.

```javascript
{
  tenantId: 't1',
  userId: 'u1',
  email: 'asha@example.com',
  roleIds: ['admin', 'billing']
}
```

## Indexes

```javascript
db.users.createIndex({ tenantId: 1, userId: 1 }, { unique: true })
db.users.createIndex({ tenantId: 1, email: 1 }, { unique: true })
```

## Use Reference When

Use a membership/role assignment collection when roles are large, many-to-many, heavily audited, or need independent lifecycle.

---

## Case Study 4: Chat Messages

## Access Patterns

- Append a message.
- List latest messages by conversation.
- Paginate older messages.
- Store read receipts.

## Recommended Model

Messages should be separate documents, not an unbounded array inside conversation.

```javascript
{
  conversationId: 'c1',
  senderId: 'u1',
  text: 'Hello',
  createdAt: new Date(),
  clientMessageId: 'client-uuid'
}
```

## Indexes

```javascript
db.messages.createIndex({ conversationId: 1, createdAt: -1, _id: -1 })
db.messages.createIndex({ conversationId: 1, clientMessageId: 1 }, { unique: true })
```

## Why

Conversation messages grow forever. Separate documents enable pagination, archival, and sharding.

---

## Case Study 5: RAG Chunks

## Access Patterns

- Retrieve chunks by vector similarity.
- Filter by tenant and ACL.
- Delete chunks when source document is removed.
- Re-embed chunks when model changes.

## Recommended Model

```javascript
{
  tenantId: 't1',
  sourceDocumentId: 'doc-123',
  chunkId: 'doc-123:0007',
  text: '...',
  embedding: [0.012, -0.044],
  metadata: {
    title: 'MongoDB Guide',
    page: 12,
    tags: ['mongodb'],
    acl: ['team-db']
  },
  embeddingModel: 'demo-embedding-model',
  contentHash: 'sha256:...',
  createdAt: new Date()
}
```

## Indexes

```javascript
db.ragChunks.createIndex({ tenantId: 1, sourceDocumentId: 1, chunkId: 1 }, { unique: true })
db.ragChunks.createIndex({ tenantId: 1, 'metadata.tags': 1 })
```

Vector index is configured in Atlas Vector Search.

## Why

RAG needs metadata and authorization as much as embeddings. Tenant and ACL filters are mandatory before sending context to the LLM.
