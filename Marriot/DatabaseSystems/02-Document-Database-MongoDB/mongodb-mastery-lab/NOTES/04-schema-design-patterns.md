# 04. Schema Design Patterns

MongoDB schema design is access-pattern design. The strongest MongoDB engineers do not ask, "What tables do I need?" first. They ask, "What does the application read and update together?"

## Embed vs Reference

Embed when:

- data is read together
- relationship is one-to-one or one-to-few
- child data is bounded
- same lifecycle
- atomic update needed

Reference when:

- child grows independently
- many-to-many
- child is large
- child updates separately
- child is shared

## Example: Order

Order line items should usually be embedded because they are bounded, owned by the order, and read with the order.

```javascript
{
  orderId: 'ORD-1001',
  customerId: 'u1',
  status: 'PAID',
  items: [
    { sku: 'SKU-1', productName: 'Keyboard', quantity: 1, priceCents: 7999 }
  ],
  totalCents: 7999
}
```

## Example: Product Reviews

Reviews should usually be separate because they grow without bound.

```javascript
// product summary
{ _id: 'p1', averageRating: 4.7, reviewCount: 1842, recentReviews: [] }

// reviews
{ _id: 'r1', productId: 'p1', userId: 'u1', rating: 5, text: 'Great' }
```

## Key Patterns

### Subset Pattern

Embed only recent/top children in parent, store full child records separately.

Use for product reviews, recent messages, latest audit events.

### Bucket Pattern

Group many time-ordered records into bounded buckets.

Use for telemetry, logs, chat messages, and high-volume events.

### Computed Pattern

Store computed values such as count, sum, average, or last activity.

Use when reads outnumber writes.

### Extended Reference Pattern

Store reference ID plus stable display fields.

```javascript
{ customer: { userId: 'u1', name: 'Asha', email: 'asha@example.com' } }
```

Use for order history and read-optimized APIs.

### Attribute Pattern

Represent flexible attributes as key-value pairs.

```javascript
{ attributes: [{ k: 'ram_gb', v: 32 }, { k: 'cpu', v: 'M3' }] }
```

Use for product catalogs with varying specs.

### CQRS Read Model

Use one write model and separate read-optimized documents.

Use for dashboards, feeds, and complex workflow read APIs.

## Schema Evolution

Use `schemaVersion` when documents evolve across releases.

```javascript
{ _id: 'order1', schemaVersion: 3, status: 'PAID' }
```

Deployment pattern:

1. Readers support old and new shapes.
2. Writers emit new shape.
3. Backfill old documents.
4. Tighten validators later.

## Senior Rule

If a schema requires many joins for the normal API path, either the MongoDB model is wrong or the database choice may be wrong.
