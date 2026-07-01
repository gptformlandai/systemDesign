# Project 13: Change Stream Event Pipeline

Difficulty: Pro

Build an event pipeline that watches MongoDB changes, writes outbox events, updates projections, and handles resume tokens safely.

---

## Goal

Practice change streams, outbox pattern, idempotent consumers, resume tokens, projection lag monitoring, and failure recovery.

---

## Schema Design

Use an `outboxEvents` collection for reliable application events and a `consumerCheckpoints` collection for resume state.

```javascript
// outboxEvents
{
  _id: 'outbox_1001',
  tenantId: 'tenant_shop',
  aggregateType: 'ORDER',
  aggregateId: 'ord_1001',
  eventType: 'ORDER_PAID',
  payload: { orderId: 'ord_1001', totalCents: 9719 },
  status: 'NEW',
  createdAt: ISODate('2026-07-01T10:00:00Z'),
  publishedAt: null
}

// consumerCheckpoints
{
  _id: 'analytics-projector',
  resumeToken: { token: 'opaque-change-stream-token' },
  lastProcessedAt: ISODate('2026-07-01T10:00:00Z')
}
```

---

## Sample Data

```javascript
db.outboxEvents.insertMany([
  { _id: 'outbox_1001', tenantId: 'tenant_shop', aggregateType: 'ORDER', aggregateId: 'ord_1001', eventType: 'ORDER_PAID', payload: { totalCents: 9719 }, status: 'NEW', createdAt: new Date(), publishedAt: null },
  { _id: 'outbox_1002', tenantId: 'tenant_shop', aggregateType: 'ORDER', aggregateId: 'ord_1002', eventType: 'ORDER_SHIPPED', payload: {}, status: 'NEW', createdAt: new Date(), publishedAt: null }
])

db.consumerCheckpoints.insertOne({ _id: 'analytics-projector', resumeToken: null, lastProcessedAt: null })
```

---

## CRUD Operations

Write aggregate and outbox event together:

```javascript
db.orders.updateOne({ tenantId: 'tenant_shop', _id: 'ord_1001' }, { $set: { status: 'PAID', updatedAt: new Date() } })
db.outboxEvents.insertOne({ _id: 'outbox_1003', tenantId: 'tenant_shop', aggregateType: 'ORDER', aggregateId: 'ord_1001', eventType: 'ORDER_PAID', payload: { totalCents: 9719 }, status: 'NEW', createdAt: new Date(), publishedAt: null })
```

Claim unpublished event:

```javascript
db.outboxEvents.findOneAndUpdate(
  { status: 'NEW' },
  { $set: { status: 'PROCESSING', lockedAt: new Date() } },
  { sort: { createdAt: 1 }, returnDocument: 'after' }
)
```

Mark event published:

```javascript
db.outboxEvents.updateOne({ _id: 'outbox_1003', status: 'PROCESSING' }, { $set: { status: 'PUBLISHED', publishedAt: new Date() } })
```

Store resume token:

```javascript
db.consumerCheckpoints.updateOne(
  { _id: 'analytics-projector' },
  { $set: { resumeToken: { token: 'opaque-change-stream-token' }, lastProcessedAt: new Date() } },
  { upsert: true }
)
```

---

## Indexes

```javascript
db.outboxEvents.createIndex({ status: 1, createdAt: 1 })
db.outboxEvents.createIndex({ tenantId: 1, aggregateType: 1, aggregateId: 1, createdAt: -1 })
db.outboxEvents.createIndex({ eventType: 1, createdAt: -1 })
db.consumerCheckpoints.createIndex({ lastProcessedAt: -1 })
```

---

## Aggregation Queries

Outbox lag by status:

```javascript
db.outboxEvents.aggregate([
  { $group: { _id: '$status', events: { $sum: 1 }, oldest: { $min: '$createdAt' } } },
  { $sort: { events: -1 } }
])
```

Event volume by type:

```javascript
db.outboxEvents.aggregate([
  { $match: { createdAt: { $gte: ISODate('2026-07-01T00:00:00Z') } } },
  { $group: { _id: '$eventType', events: { $sum: 1 } } },
  { $sort: { events: -1 } }
])
```

---

## Performance Considerations

- Keep outbox event payloads compact.
- Index the publisher claim query.
- Use idempotency in consumers because events can be retried.
- Monitor projection lag and outbox backlog.

---

## Scaling Considerations

- Partition consumers by event type, tenant, or aggregate ID.
- Use Kafka for fanout to many downstream systems.
- Keep resume token storage durable.
- Ensure oplog window is large enough for consumer downtime.

---

## Security Considerations

- Do not put secrets in event payloads.
- Restrict outbox write access to application services.
- Encrypt sensitive event fields if required.
- Audit replay and backfill operations.

---

## Optional API Layer

- `POST /events/replay`
- `GET /events/outbox-lag`
- `POST /workers/outbox/claim`
- `POST /workers/outbox/{eventId}/published`

---

## Interview Discussion Points

- Change streams vs outbox pattern?
- What happens if the consumer loses its resume token?
- How do you make consumers idempotent?
- How do you monitor projection lag?
- When is Kafka needed in addition to MongoDB?
