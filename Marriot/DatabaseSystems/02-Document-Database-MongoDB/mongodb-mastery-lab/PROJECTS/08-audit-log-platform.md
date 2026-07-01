# Project 08: Audit Log Platform

Difficulty: Intermediate to Advanced

Build an append-only audit log platform for compliance investigations, tenant activity history, retention, legal hold, and operational search.

---

## Goal

Practice append-only modeling, investigation indexes, retention strategy, immutable write paths, and high-volume time-oriented data access.

---

## Schema Design

Audit logs should be immutable facts. Store actor, action, target, request context, metadata, and timestamp.

```javascript
{
  _id: 'audit_1001',
  tenantId: 'tenant_acme',
  actor: { type: 'USER', id: 'usr_1001' },
  action: 'ORDER_REFUNDED',
  target: { type: 'ORDER', id: 'ord_1001' },
  request: { requestId: 'req_abc', ip: '203.0.113.10', userAgent: 'Mozilla/5.0' },
  metadata: { amountCents: 2500, reason: 'customer_request' },
  severity: 'INFO',
  createdAt: ISODate('2026-07-01T10:00:00Z'),
  retentionClass: 'STANDARD',
  legalHold: false
}
```

---

## Sample Data

```javascript
db.auditLogs.insertMany([
  { _id: 'audit_1001', tenantId: 'tenant_acme', actor: { type: 'USER', id: 'usr_1001' }, action: 'ORDER_REFUNDED', target: { type: 'ORDER', id: 'ord_1001' }, request: { requestId: 'req_abc', ip: '203.0.113.10' }, metadata: { amountCents: 2500 }, severity: 'INFO', createdAt: new Date(), retentionClass: 'STANDARD', legalHold: false },
  { _id: 'audit_1002', tenantId: 'tenant_acme', actor: { type: 'SYSTEM', id: 'billing-worker' }, action: 'PAYMENT_CAPTURED', target: { type: 'PAYMENT', id: 'pay_1001' }, request: { requestId: 'req_def', ip: '10.0.0.5' }, metadata: {}, severity: 'INFO', createdAt: new Date(), retentionClass: 'COMPLIANCE', legalHold: false }
])
```

---

## CRUD Operations

Create audit record:

```javascript
db.auditLogs.insertOne({
  _id: 'audit_1003', tenantId: 'tenant_acme', actor: { type: 'USER', id: 'usr_admin' }, action: 'USER_ROLE_CHANGED',
  target: { type: 'USER', id: 'usr_1001' }, request: { requestId: 'req_xyz', ip: '203.0.113.15' }, metadata: { from: 'USER', to: 'ADMIN' },
  severity: 'WARN', createdAt: new Date(), retentionClass: 'COMPLIANCE', legalHold: false
})
```

Search by target:

```javascript
db.auditLogs.find({ tenantId: 'tenant_acme', 'target.type': 'ORDER', 'target.id': 'ord_1001' }).sort({ createdAt: -1 }).limit(50)
```

Search by actor:

```javascript
db.auditLogs.find({ tenantId: 'tenant_acme', 'actor.id': 'usr_1001' }).sort({ createdAt: -1 }).limit(50)
```

Apply legal hold:

```javascript
db.auditLogs.updateMany(
  { tenantId: 'tenant_acme', 'target.id': 'ord_1001' },
  { $set: { legalHold: true } }
)
```

---

## Indexes

```javascript
db.auditLogs.createIndex({ tenantId: 1, createdAt: -1 })
db.auditLogs.createIndex({ tenantId: 1, 'actor.id': 1, createdAt: -1 })
db.auditLogs.createIndex({ tenantId: 1, 'target.type': 1, 'target.id': 1, createdAt: -1 })
db.auditLogs.createIndex({ tenantId: 1, action: 1, createdAt: -1 })
```

---

## Aggregation Queries

Actions by day:

```javascript
db.auditLogs.aggregate([
  { $match: { tenantId: 'tenant_acme' } },
  { $group: { _id: { day: { $dateTrunc: { date: '$createdAt', unit: 'day' } }, action: '$action' }, count: { $sum: 1 } } },
  { $sort: { '_id.day': -1, count: -1 } }
])
```

Top actors:

```javascript
db.auditLogs.aggregate([
  { $match: { tenantId: 'tenant_acme', severity: { $in: ['WARN', 'ERROR'] } } },
  { $group: { _id: '$actor.id', events: { $sum: 1 } } },
  { $sort: { events: -1 } }
])
```

---

## Performance Considerations

- Audit collections are write-heavy; keep indexes intentional.
- Use time-bounded investigation queries.
- Project only required metadata fields for list views.
- Use archival jobs for cold audit data.

---

## Scaling Considerations

- Shard by `{ tenantId: 1, createdAt: 1 }` or bucketed tenant/time key depending tenant skew.
- Export older records to object storage for long-term compliance retention.
- Use separate collections by retention class if policies diverge strongly.
- Precompute compliance dashboards instead of scanning raw audit logs.

---

## Security Considerations

- Make audit writes append-only at the application role level.
- Restrict update permissions to legal hold fields if needed.
- Redact secrets from metadata before write.
- Use majority write concern for compliance-critical events.

---

## Optional API Layer

- `POST /audit-events`
- `GET /audit-events?actorId=&targetType=&targetId=&cursor=`
- `POST /audit-events/legal-hold`
- `GET /audit-events/export`

---

## Interview Discussion Points

- How immutable is MongoDB audit storage really?
- Which indexes support investigations?
- How do you handle retention and legal hold conflict?
- What is the write concern for compliance logs?
- When should audit logs move to object storage or SIEM?
