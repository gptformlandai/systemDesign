    # MongoDB Data Modeling and Schema Design - MAANG Master Sheet

    > **Track File #6 of 28 - Group 02: Intermediate Backend**
    > For: backend/database/system design interviews | Level: intermediate to senior | Mode: schema patterns, tradeoffs, access-pattern thinking

    This sheet builds:
    - Schema-flexible, not schema-less thinking
- Embedding, referencing, hybrid modeling
- 14 schema patterns with examples and tradeoffs

Original master-map sections included here:
- 6. Data Modeling and Schema Design

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 6. Data Modeling and Schema Design

MongoDB performance is mostly modeling plus indexing. A bad schema can make MongoDB look slow even when the database is healthy.

### Core Principle

MongoDB is schema-flexible, not schema-less.

Schema-flexible means:

- you can evolve documents without table-wide migrations
- different document subtypes can coexist
- optional fields are natural
- validation can still enforce contracts

Schema-less as a mindset is dangerous because it leads to inconsistent types, impossible queries, and broken indexes.

### Design Around Query Patterns

Start with access patterns:

| Question | Example |
|---|---|
| What is fetched by ID? | Get order details by `orderId` |
| What is listed? | List orders by `tenantId`, `status`, `createdAt` |
| What is searched? | Search products by text, category, attributes |
| What is updated atomically? | Add item to cart, reserve inventory |
| What grows forever? | Logs, messages, comments, events |
| What is high volume? | IoT metrics, audit events |
| What is security-filtered? | RAG chunks by tenant and ACL |

### Read-Heavy vs Write-Heavy Modeling

| Workload | Modeling Bias |
|---|---|
| Read-heavy | Denormalize, embed summaries, precompute, index reads |
| Write-heavy | Keep documents small, avoid excessive indexes, batch writes |
| Mixed | Hybrid model, event stream plus materialized views |
| Analytical | Pre-aggregate or export to OLAP system |

### Document Growth

Document growth happens when updates make a document larger over time.

Risk examples:

- appending every login event to `user.logins`
- storing all chat messages inside one conversation document
- storing all product reviews inside product document

Safer alternatives:

- separate child collection
- bucket pattern
- subset pattern
- rolling window embedded in parent plus full history elsewhere

### Bounded vs Unbounded Arrays

| Array Type | Example | Embed? |
|---|---|---|
| Bounded | Order line items, profile addresses | Usually yes |
| Soft bounded | Top 5 recent login devices | Yes with cap |
| Unbounded | Comments, messages, audit events | Usually no |

### Embedding

Use embedding when:

- data is read together
- relationship is one-to-one or one-to-few
- child data is bounded
- child data is owned by parent
- atomic update is needed within one aggregate

Example:

```javascript
{
  _id: ObjectId("..."),
  orderId: "ORD-1001",
  userId: "u1",
  status: "PAID",
  items: [
    { sku: "SKU-1", name: "Keyboard", quantity: 1, priceCents: 7999 },
    { sku: "SKU-2", name: "Mouse", quantity: 1, priceCents: 3999 }
  ],
  shippingAddress: {
    line1: "123 Main St",
    city: "Dallas",
    state: "TX"
  },
  totalCents: 11998
}
```

Why it works: the order details page can fetch one document. Items belong to the order and do not grow forever.

Do not embed when:

- the child grows without bound
- the child is independently queried often
- the child changes much more frequently than parent
- many parents share the same child entity

### Referencing

Use references when:

- child data grows independently
- relationship is many-to-many
- child data is large
- child is frequently updated separately
- child is shared by many parents

Example:

```javascript
// users
{ _id: "u1", email: "asha@example.com", name: "Asha" }

// orders
{ _id: "o1", userId: "u1", status: "PAID", totalCents: 11998 }
```

Do not reference blindly. Every reference may require another query or `$lookup`.

### Hybrid Modeling

Hybrid models duplicate small, stable summaries and reference full details.

```javascript
{
  _id: "order1",
  userId: "u1",
  userSnapshot: {
    name: "Asha",
    email: "asha@example.com"
  },
  items: [
    { sku: "SKU-1", productName: "Keyboard", priceCents: 7999, quantity: 1 }
  ]
}
```

Why: order history should show what the customer saw at purchase time, even if product name later changes.

### Schema Pattern 1: Embedded Document Pattern

Problem: multiple fields are always read together.

Solution: store child fields inside parent.

```javascript
{
  _id: "u1",
  name: "Asha",
  address: {
    line1: "123 Main St",
    city: "Dallas",
    state: "TX"
  }
}
```

Use when: one-to-one or bounded one-to-few.

Avoid when: child is unbounded or shared.

Tradeoff: faster reads, less normalization, possible duplication.

### Schema Pattern 2: Reference Pattern

Problem: related data is large or shared.

Solution: store IDs and query related collection when needed.

```javascript
{ _id: "review1", productId: "p1", userId: "u1", rating: 5, text: "Great" }
```

Use when: one-to-many or many-to-many with independent lifecycle.

Avoid when: every read needs many referenced documents.

Tradeoff: flexible growth, more joins or app-side fetches.

### Schema Pattern 3: Subset Pattern

Problem: parent needs a small preview of many children.

Solution: embed recent/top subset, store full data separately.

```javascript
// products
{
  _id: "p1",
  name: "Keyboard",
  recentReviews: [
    { reviewId: "r9", rating: 5, text: "Excellent", createdAt: ISODate("2026-07-01") }
  ],
  reviewCount: 1842,
  averageRating: 4.7
}

// reviews
{ _id: "r9", productId: "p1", rating: 5, text: "Excellent" }
```

Use when: list/detail pages need summary but full child list is separate.

Avoid when: embedded subset must always be perfect and frequent updates are too costly.

Tradeoff: faster common reads, duplication maintenance.

### Schema Pattern 4: Bucket Pattern

Problem: many small events per entity create too many documents or unbounded arrays.

Solution: group events into bounded buckets.

```javascript
{
  _id: "device-7-2026-07-01T10",
  deviceId: "device-7",
  hourStart: ISODate("2026-07-01T10:00:00Z"),
  count: 3,
  measurements: [
    { ts: ISODate("2026-07-01T10:00:01Z"), temp: 71.2 },
    { ts: ISODate("2026-07-01T10:00:15Z"), temp: 71.5 }
  ]
}
```

Use when: time-series-like data needs efficient batch reads.

Avoid when: individual events need independent updates or buckets become huge.

Tradeoff: fewer documents and efficient scans, more complex writes.

### Schema Pattern 5: Outlier Pattern

Problem: most documents fit embedding, but a few extreme cases break limits.

Solution: embed normal data and split outlier overflow.

```javascript
{
  _id: "p1",
  name: "Popular Product",
  recentReviewIds: ["r1", "r2"],
  hasReviewOverflow: true
}
```

Use when: distribution has rare extreme documents.

Avoid when: most documents are outliers.

Tradeoff: keeps common path fast, adds special handling.

### Schema Pattern 6: Computed Pattern

Problem: expensive values are recalculated often.

Solution: store computed fields and update them on writes or asynchronously.

```javascript
{
  _id: "p1",
  ratingCount: 1842,
  ratingSum: 8657,
  averageRating: 4.7
}
```

Use when: reads greatly outnumber writes.

Avoid when: exact real-time accuracy is required and write cost is unacceptable.

Tradeoff: fast reads, consistency complexity.

### Schema Pattern 7: Extended Reference Pattern

Problem: referenced documents need small display fields frequently.

Solution: store reference plus duplicated summary.

```javascript
{
  _id: "order1",
  customer: {
    userId: "u1",
    name: "Asha",
    email: "asha@example.com"
  }
}
```

Use when: display needs related names or labels.

Avoid when: duplicated fields change constantly.

Tradeoff: fewer joins, stale duplicated data risk.

### Schema Pattern 8: Attribute Pattern

Problem: many optional searchable attributes vary by item type.

Solution: represent attributes as key-value array or object.

```javascript
{
  _id: "p1",
  category: "laptop",
  attributes: [
    { k: "ram_gb", v: 32 },
    { k: "cpu", v: "M3" },
    { k: "screen_inches", v: 14 }
  ]
}
```

Use when: product specs vary widely.

Avoid when: fields are stable and first-class.

Tradeoff: flexible search, more complex indexing and querying.

### Schema Pattern 9: Polymorphic Pattern

Problem: related document types share a collection but have type-specific fields.

Solution: store a discriminator field.

```javascript
{
  _id: "evt1",
  type: "PAYMENT_CAPTURED",
  orderId: "o1",
  amountCents: 5000,
  capturedAt: ISODate("2026-07-01T10:00:00Z")
}
```

Use when: events or content blocks have many shapes but common lifecycle.

Avoid when: types need very different indexes, retention, or access control.

Tradeoff: simpler event stream, careful validation needed.

### Schema Pattern 10: Tree Pattern

Problem: hierarchical data such as categories.

Solution: store parent references and optionally ancestor arrays.

```javascript
{
  _id: "cat-keyboards",
  name: "Keyboards",
  parentId: "cat-computer-accessories",
  ancestors: ["cat-electronics", "cat-computer-accessories"]
}
```

Use when: tree depth is moderate and hierarchy queries are common.

Avoid when: graph traversal is deep and arbitrary.

Tradeoff: fast ancestor queries, updates needed when moving subtrees.

### Schema Pattern 11: Materialized Path Pattern

Problem: need fast subtree matching.

Solution: store path string or array.

```javascript
{
  _id: "cat-keyboards",
  path: "/electronics/computer-accessories/keyboards",
  pathParts: ["electronics", "computer-accessories", "keyboards"]
}
```

Use when: category browsing and prefix queries matter.

Avoid when: nodes move frequently.

Tradeoff: fast reads, path update complexity.

### Schema Pattern 12: Pre-Aggregation Pattern

Problem: dashboard queries repeatedly group millions of raw events.

Solution: maintain summary collection.

```javascript
{
  _id: "tenant-t1-2026-07-01",
  tenantId: "t1",
  day: ISODate("2026-07-01"),
  orders: 18201,
  revenueCents: 93500220
}
```

Use when: dashboard reads are frequent.

Avoid when: users need arbitrary ad hoc analytics.

Tradeoff: fast dashboards, summary maintenance.

### Schema Pattern 13: Event Sourcing Pattern

Problem: need immutable history of domain changes.

Solution: store append-only events and derive current state.

```javascript
{
  _id: ObjectId("..."),
  aggregateId: "order1",
  sequence: 17,
  type: "ORDER_SHIPPED",
  payload: { carrier: "UPS", trackingNumber: "1Z..." },
  createdAt: ISODate("2026-07-01T12:00:00Z")
}
```

Use when: auditability and replay matter.

Avoid when: simple CRUD state is enough.

Tradeoff: complete history, more complex reads and migrations.

### Schema Pattern 14: CQRS-Style Read Model Pattern

Problem: write model and read model need different shapes.

Solution: store normalized-ish writes and denormalized read projections.

```javascript
// order_events append-only
{ aggregateId: "order1", type: "ITEM_ADDED", payload: { sku: "SKU-1" } }

// order_read_models optimized for API reads
{ _id: "order1", status: "PAID", items: [{ sku: "SKU-1" }], totalCents: 7999 }
```

Use when: complex workflows need fast reads.

Avoid when: projection lag is unacceptable or system is simple.

Tradeoff: scalability and read speed, eventual consistency.

---

---
