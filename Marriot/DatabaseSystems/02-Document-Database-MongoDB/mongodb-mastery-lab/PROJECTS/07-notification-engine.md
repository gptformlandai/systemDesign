# Project 07: Notification Engine

Difficulty: Intermediate

Build a notification engine for email, SMS, push, and in-app notifications with delivery state, retries, templates, and user preferences.

---

## Goal

Practice queue-like document design, retryable state transitions, TTL cleanup, user preference lookups, and aggregation for delivery metrics.

---

## Schema Design

Use `notifications` for delivery tasks and `notificationPreferences` for user-level channel preferences.

```javascript
{
  _id: 'notif_1001',
  tenantId: 'tenant_notify',
  userId: 'usr_1001',
  type: 'ORDER_SHIPPED',
  channel: 'email',
  status: 'PENDING',
  priority: 5,
  payload: { orderId: 'ord_1001', trackingNumber: '1Z999' },
  attempts: 0,
  nextAttemptAt: ISODate('2026-07-01T10:00:00Z'),
  lastError: null,
  createdAt: ISODate('2026-07-01T10:00:00Z'),
  updatedAt: ISODate('2026-07-01T10:00:00Z'),
  expiresAt: ISODate('2026-08-01T10:00:00Z')
}
```

---

## Sample Data

```javascript
db.notifications.insertMany([
  { _id: 'notif_1001', tenantId: 'tenant_notify', userId: 'usr_1001', type: 'ORDER_SHIPPED', channel: 'email', status: 'PENDING', priority: 5, payload: { orderId: 'ord_1001' }, attempts: 0, nextAttemptAt: new Date(), lastError: null, createdAt: new Date(), updatedAt: new Date(), expiresAt: ISODate('2026-08-01T00:00:00Z') },
  { _id: 'notif_1002', tenantId: 'tenant_notify', userId: 'usr_1002', type: 'PASSWORD_RESET', channel: 'sms', status: 'SENT', priority: 10, payload: {}, attempts: 1, nextAttemptAt: null, lastError: null, createdAt: new Date(), updatedAt: new Date(), expiresAt: ISODate('2026-08-01T00:00:00Z') }
])

db.notificationPreferences.insertOne({
  tenantId: 'tenant_notify', userId: 'usr_1001', channels: { email: true, sms: false, push: true }, quietHours: { start: '22:00', end: '07:00' }, updatedAt: new Date()
})
```

---

## CRUD Operations

Create notification:

```javascript
db.notifications.insertOne({
  _id: 'notif_1003', tenantId: 'tenant_notify', userId: 'usr_1001', type: 'ORDER_DELIVERED', channel: 'push', status: 'PENDING', priority: 5,
  payload: { orderId: 'ord_1001' }, attempts: 0, nextAttemptAt: new Date(), lastError: null, createdAt: new Date(), updatedAt: new Date(), expiresAt: ISODate('2026-08-01T00:00:00Z')
})
```

Claim next pending notification:

```javascript
db.notifications.findOneAndUpdate(
  { tenantId: 'tenant_notify', status: 'PENDING', nextAttemptAt: { $lte: new Date() } },
  { $set: { status: 'PROCESSING', lockedAt: new Date(), updatedAt: new Date() } },
  { sort: { priority: -1, nextAttemptAt: 1 }, returnDocument: 'after' }
)
```

Mark sent:

```javascript
db.notifications.updateOne(
  { tenantId: 'tenant_notify', _id: 'notif_1003', status: 'PROCESSING' },
  { $set: { status: 'SENT', sentAt: new Date(), updatedAt: new Date() } }
)
```

Retry failure:

```javascript
db.notifications.updateOne(
  { tenantId: 'tenant_notify', _id: 'notif_1001' },
  { $inc: { attempts: 1 }, $set: { status: 'PENDING', nextAttemptAt: ISODate('2026-07-01T10:05:00Z'), lastError: 'provider timeout', updatedAt: new Date() } }
)
```

---

## Indexes

```javascript
db.notifications.createIndex({ tenantId: 1, status: 1, nextAttemptAt: 1, priority: -1 })
db.notifications.createIndex({ tenantId: 1, userId: 1, createdAt: -1 })
db.notifications.createIndex({ tenantId: 1, type: 1, createdAt: -1 })
db.notifications.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 })
db.notificationPreferences.createIndex({ tenantId: 1, userId: 1 }, { unique: true })
```

---

## Aggregation Queries

Delivery rate by channel:

```javascript
db.notifications.aggregate([
  { $match: { tenantId: 'tenant_notify' } },
  { $group: { _id: { channel: '$channel', status: '$status' }, count: { $sum: 1 } } },
  { $sort: { '_id.channel': 1, count: -1 } }
])
```

Retry pressure:

```javascript
db.notifications.aggregate([
  { $match: { tenantId: 'tenant_notify', attempts: { $gt: 0 } } },
  { $group: { _id: '$type', failedOrRetried: { $sum: 1 }, avgAttempts: { $avg: '$attempts' } } },
  { $sort: { failedOrRetried: -1 } }
])
```

---

## Performance Considerations

- The worker claim query must be covered by `{ status, nextAttemptAt, priority }`.
- Keep payloads small; store large rendered content elsewhere.
- Avoid too many secondary indexes on a high-write queue collection.
- Use backoff to avoid retry storms.

---

## Scaling Considerations

- Partition workers by tenant, channel, or notification type.
- Use Kafka or a dedicated queue if strict queue semantics and extreme throughput are required.
- Shard by `{ tenantId: 1, _id: 1 }` or channel-specific strategy for large systems.
- Move long-term delivery analytics to rollup collections.

---

## Security Considerations

- Respect opt-outs and quiet hours.
- Encrypt or redact sensitive payloads.
- Do not log full message bodies for password reset or MFA flows.
- Rate-limit notifications to prevent abuse.

---

## Optional API Layer

- `POST /notifications`
- `GET /users/{userId}/notifications?cursor=`
- `PATCH /users/{userId}/notification-preferences`
- `POST /workers/notifications/claim`
- `POST /workers/notifications/{notificationId}/complete`

---

## Interview Discussion Points

- Is MongoDB enough for a notification queue?
- How do you design idempotent delivery?
- Which index supports worker claiming?
- How do you prevent retry storms?
- When would you introduce Kafka/SQS?
