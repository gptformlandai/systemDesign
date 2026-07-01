# Project 14: Time-Series Metrics Store

Difficulty: Pro

Build a time-series metrics store for application metrics, host metrics, service-level objectives, rollups, and retention.

---

## Goal

Practice MongoDB time-series modeling, metadata fields, retention, rollups, SLO queries, and the tradeoff between MongoDB and specialized metrics systems.

---

## Schema Design

For MongoDB time-series collections, keep time in a `timestamp` field and stable dimensions in a metadata field.

```javascript
{
  timestamp: ISODate('2026-07-01T10:00:00Z'),
  metadata: {
    tenantId: 'tenant_ops',
    service: 'checkout-api',
    region: 'us-east-1',
    instanceId: 'i-001'
  },
  metric: 'http.server.duration.p95',
  value: 184,
  unit: 'ms'
}
```

Create a time-series collection:

```javascript
db.createCollection('serviceMetrics', {
  timeseries: { timeField: 'timestamp', metaField: 'metadata', granularity: 'minutes' },
  expireAfterSeconds: 2592000
})
```

---

## Sample Data

```javascript
db.serviceMetrics.insertMany([
  { timestamp: ISODate('2026-07-01T10:00:00Z'), metadata: { tenantId: 'tenant_ops', service: 'checkout-api', region: 'us-east-1', instanceId: 'i-001' }, metric: 'http.server.duration.p95', value: 184, unit: 'ms' },
  { timestamp: ISODate('2026-07-01T10:01:00Z'), metadata: { tenantId: 'tenant_ops', service: 'checkout-api', region: 'us-east-1', instanceId: 'i-001' }, metric: 'http.server.duration.p95', value: 260, unit: 'ms' }
])

db.metricHourlyRollups.insertOne({ tenantId: 'tenant_ops', service: 'checkout-api', metric: 'http.server.duration.p95', hour: ISODate('2026-07-01T10:00:00Z'), avgValue: 222, maxValue: 260, samples: 2 })
```

---

## CRUD Operations

Insert metric point:

```javascript
db.serviceMetrics.insertOne({ timestamp: new Date(), metadata: { tenantId: 'tenant_ops', service: 'checkout-api', region: 'us-east-1', instanceId: 'i-002' }, metric: 'http.requests', value: 120, unit: 'count' })
```

Read service series:

```javascript
db.serviceMetrics.find({ 'metadata.tenantId': 'tenant_ops', 'metadata.service': 'checkout-api', metric: 'http.server.duration.p95', timestamp: { $gte: ISODate('2026-07-01T10:00:00Z') } }).sort({ timestamp: 1 })
```

Create hourly rollup:

```javascript
db.serviceMetrics.aggregate([
  { $match: { 'metadata.tenantId': 'tenant_ops', timestamp: { $gte: ISODate('2026-07-01T10:00:00Z'), $lt: ISODate('2026-07-01T11:00:00Z') } } },
  { $group: { _id: { service: '$metadata.service', metric: '$metric', hour: { $dateTrunc: { date: '$timestamp', unit: 'hour' } } }, avgValue: { $avg: '$value' }, maxValue: { $max: '$value' }, samples: { $sum: 1 } } },
  { $merge: { into: 'metricHourlyRollups', on: '_id', whenMatched: 'replace', whenNotMatched: 'insert' } }
])
```

Delete old rollups manually if policy changes:

```javascript
db.metricHourlyRollups.deleteMany({ hour: { $lt: ISODate('2025-07-01T00:00:00Z') } })
```

---

## Indexes

```javascript
db.serviceMetrics.createIndex({ 'metadata.tenantId': 1, 'metadata.service': 1, metric: 1, timestamp: -1 })
db.metricHourlyRollups.createIndex({ tenantId: 1, service: 1, metric: 1, hour: -1 })
```

---

## Aggregation Queries

SLO breach windows:

```javascript
db.serviceMetrics.aggregate([
  { $match: { 'metadata.tenantId': 'tenant_ops', metric: 'http.server.duration.p95', value: { $gt: 250 } } },
  { $group: { _id: { service: '$metadata.service', minute: { $dateTrunc: { date: '$timestamp', unit: 'minute' } } }, breaches: { $sum: 1 }, maxLatency: { $max: '$value' } } },
  { $sort: { '_id.minute': -1 } }
])
```

Average by service:

```javascript
db.metricHourlyRollups.aggregate([
  { $match: { tenantId: 'tenant_ops', hour: { $gte: ISODate('2026-07-01T00:00:00Z') } } },
  { $group: { _id: '$service', avgP95: { $avg: '$avgValue' }, worstP95: { $max: '$maxValue' } } },
  { $sort: { worstP95: -1 } }
])
```

---

## Performance Considerations

- Use time-series collections for storage and query efficiency.
- Keep metadata cardinality reasonable.
- Roll up old high-resolution data.
- Avoid ad hoc dashboards over raw points for long ranges.

---

## Scaling Considerations

- Partition metrics by tenant/service if volume is extreme.
- Move long-term observability analytics to Prometheus, Mimir, ClickHouse, or another metrics platform if needed.
- Use retention tiers: raw minutes, hourly rollups, daily rollups.
- Watch hot services with very high cardinality labels.

---

## Security Considerations

- Metrics can leak endpoint names, tenant IDs, and infrastructure topology.
- Restrict cross-tenant metrics views.
- Avoid labels containing user IDs, tokens, or PII.
- Audit admin access to operational metrics.

---

## Optional API Layer

- `POST /metrics:batch`
- `GET /metrics/series?service=&metric=&from=&to=`
- `GET /metrics/slo-breaches`
- `POST /metrics/rollups/hourly`

---

## Interview Discussion Points

- When are MongoDB time-series collections a good fit?
- What label/cardinality mistakes hurt metrics systems?
- How do rollups change query cost?
- How would you shard metrics?
- When would Prometheus or ClickHouse be better?
