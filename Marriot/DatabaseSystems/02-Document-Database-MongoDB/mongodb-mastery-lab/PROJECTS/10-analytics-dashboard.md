# Project 10: Analytics Dashboard

Difficulty: Advanced

Build a real-time analytics dashboard using raw events, summary collections, change streams, and rebuildable rollups.

---

## Goal

Practice materialized views, aggregation pipelines, dashboard freshness, idempotent summary updates, and avoiding expensive raw scans on every dashboard request.

---

## Schema Design

Store raw events separately from dashboard summaries. Dashboard APIs should read summary collections.

```javascript
// raw events
{
  _id: 'evt_1001',
  tenantId: 'tenant_shop',
  eventType: 'ORDER_PAID',
  entityId: 'ord_1001',
  amountCents: 9719,
  occurredAt: ISODate('2026-07-01T10:00:00Z'),
  ingestedAt: ISODate('2026-07-01T10:00:01Z')
}

// minute summary
{
  _id: 'tenant_shop:2026-07-01T10:00:00Z:ORDER_PAID',
  tenantId: 'tenant_shop',
  bucketStart: ISODate('2026-07-01T10:00:00Z'),
  granularity: 'minute',
  eventType: 'ORDER_PAID',
  count: 42,
  amountCents: 120000,
  updatedAt: ISODate('2026-07-01T10:00:15Z')
}
```

---

## Sample Data

```javascript
db.analyticsEvents.insertMany([
  { _id: 'evt_1001', tenantId: 'tenant_shop', eventType: 'ORDER_PAID', entityId: 'ord_1001', amountCents: 9719, occurredAt: ISODate('2026-07-01T10:00:00Z'), ingestedAt: new Date() },
  { _id: 'evt_1002', tenantId: 'tenant_shop', eventType: 'ORDER_PAID', entityId: 'ord_1002', amountCents: 9138, occurredAt: ISODate('2026-07-01T10:01:00Z'), ingestedAt: new Date() }
])

db.analyticsMinuteStats.insertOne({ _id: 'tenant_shop:2026-07-01T10:00:00Z:ORDER_PAID', tenantId: 'tenant_shop', bucketStart: ISODate('2026-07-01T10:00:00Z'), granularity: 'minute', eventType: 'ORDER_PAID', count: 2, amountCents: 18857, updatedAt: new Date() })
```

---

## CRUD Operations

Insert event:

```javascript
db.analyticsEvents.insertOne({ _id: 'evt_1003', tenantId: 'tenant_shop', eventType: 'CART_ABANDONED', entityId: 'cart_1001', amountCents: 8999, occurredAt: new Date(), ingestedAt: new Date() })
```

Idempotent summary update:

```javascript
db.analyticsMinuteStats.updateOne(
  { _id: 'tenant_shop:2026-07-01T10:00:00Z:CART_ABANDONED' },
  { $setOnInsert: { tenantId: 'tenant_shop', bucketStart: ISODate('2026-07-01T10:00:00Z'), granularity: 'minute', eventType: 'CART_ABANDONED' }, $inc: { count: 1, amountCents: 8999 }, $set: { updatedAt: new Date() } },
  { upsert: true }
)
```

Read dashboard:

```javascript
db.analyticsMinuteStats.find({ tenantId: 'tenant_shop', bucketStart: { $gte: ISODate('2026-07-01T10:00:00Z') } }).sort({ bucketStart: 1 })
```

Rebuild one day:

```javascript
db.analyticsEvents.aggregate([
  { $match: { tenantId: 'tenant_shop', occurredAt: { $gte: ISODate('2026-07-01T00:00:00Z'), $lt: ISODate('2026-07-02T00:00:00Z') } } },
  { $group: { _id: { bucketStart: { $dateTrunc: { date: '$occurredAt', unit: 'minute' } }, eventType: '$eventType' }, count: { $sum: 1 }, amountCents: { $sum: '$amountCents' } } }
])
```

---

## Indexes

```javascript
db.analyticsEvents.createIndex({ tenantId: 1, eventType: 1, occurredAt: -1 })
db.analyticsEvents.createIndex({ tenantId: 1, entityId: 1 })
db.analyticsMinuteStats.createIndex({ tenantId: 1, bucketStart: 1, eventType: 1 })
```

---

## Aggregation Queries

Dashboard series:

```javascript
db.analyticsMinuteStats.aggregate([
  { $match: { tenantId: 'tenant_shop', eventType: 'ORDER_PAID', bucketStart: { $gte: ISODate('2026-07-01T10:00:00Z') } } },
  { $project: { _id: 0, bucketStart: 1, count: 1, amountCents: 1 } },
  { $sort: { bucketStart: 1 } }
])
```

Event mix:

```javascript
db.analyticsMinuteStats.aggregate([
  { $match: { tenantId: 'tenant_shop' } },
  { $group: { _id: '$eventType', count: { $sum: '$count' }, amountCents: { $sum: '$amountCents' } } },
  { $sort: { count: -1 } }
])
```

---

## Performance Considerations

- Do not aggregate raw events per dashboard refresh.
- Keep summaries small and indexed by tenant/time.
- Use idempotency to prevent duplicate event rollups.
- Show dashboard freshness timestamp.

---

## Scaling Considerations

- Use change streams or Kafka consumers to maintain summaries.
- Partition summary workers by tenant or time bucket.
- Store raw long-term events in OLAP/object storage for historical analytics.
- Use separate collections for minute, hour, and day summaries.

---

## Security Considerations

- Enforce tenant filters on all dashboard reads.
- Validate event schemas before ingestion.
- Restrict raw event access because payloads may contain sensitive metadata.
- Audit dashboard export actions.

---

## Optional API Layer

- `POST /analytics/events`
- `GET /dashboards/revenue?from=&to=&granularity=minute`
- `GET /dashboards/events/mix`
- `POST /analytics/rebuild?day=2026-07-01`

---

## Interview Discussion Points

- Why use summary collections instead of raw aggregation?
- How do you rebuild corrupted summaries?
- How fresh does real-time need to be?
- How do you prevent duplicate event counting?
- When should analytics move out of MongoDB?
