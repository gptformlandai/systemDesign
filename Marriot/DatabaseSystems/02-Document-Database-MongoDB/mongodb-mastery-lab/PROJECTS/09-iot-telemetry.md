# Project 09: IoT Telemetry

Difficulty: Advanced

Build an IoT telemetry ingestion service for device readings, alerts, device metadata, downsampling, and tenant dashboards.

---

## Goal

Practice high-write data modeling, time-oriented indexes, bucket strategy, alert aggregation, retention, and sharding for device workloads.

---

## Schema Design

Use `deviceReadings` for raw telemetry and `devices` for metadata. For extreme ingestion, use MongoDB time-series collections or bucketed documents.

```javascript
{
  _id: ObjectId(),
  tenantId: 'tenant_iot',
  deviceId: 'dev_1001',
  metric: 'temperature_celsius',
  value: 22.4,
  unit: 'C',
  tags: { building: 'A', floor: '3' },
  observedAt: ISODate('2026-07-01T10:00:00Z'),
  ingestedAt: ISODate('2026-07-01T10:00:01Z')
}
```

---

## Sample Data

```javascript
db.devices.insertMany([
  { _id: 'dev_1001', tenantId: 'tenant_iot', type: 'thermostat', status: 'ACTIVE', location: { building: 'A', floor: '3' }, createdAt: new Date() },
  { _id: 'dev_1002', tenantId: 'tenant_iot', type: 'humidity', status: 'ACTIVE', location: { building: 'A', floor: '4' }, createdAt: new Date() }
])

db.deviceReadings.insertMany([
  { tenantId: 'tenant_iot', deviceId: 'dev_1001', metric: 'temperature_celsius', value: 22.4, unit: 'C', tags: { building: 'A', floor: '3' }, observedAt: ISODate('2026-07-01T10:00:00Z'), ingestedAt: new Date() },
  { tenantId: 'tenant_iot', deviceId: 'dev_1001', metric: 'temperature_celsius', value: 23.1, unit: 'C', tags: { building: 'A', floor: '3' }, observedAt: ISODate('2026-07-01T10:01:00Z'), ingestedAt: new Date() }
])
```

---

## CRUD Operations

Register device:

```javascript
db.devices.insertOne({ _id: 'dev_1003', tenantId: 'tenant_iot', type: 'thermostat', status: 'ACTIVE', location: { building: 'B', floor: '1' }, createdAt: new Date() })
```

Insert readings in batch:

```javascript
db.deviceReadings.insertMany([
  { tenantId: 'tenant_iot', deviceId: 'dev_1003', metric: 'temperature_celsius', value: 21.8, unit: 'C', tags: { building: 'B', floor: '1' }, observedAt: new Date(), ingestedAt: new Date() },
  { tenantId: 'tenant_iot', deviceId: 'dev_1003', metric: 'humidity_percent', value: 41.2, unit: '%', tags: { building: 'B', floor: '1' }, observedAt: new Date(), ingestedAt: new Date() }
])
```

Read latest device readings:

```javascript
db.deviceReadings.find({ tenantId: 'tenant_iot', deviceId: 'dev_1001' }).sort({ observedAt: -1 }).limit(100)
```

Mark device inactive:

```javascript
db.devices.updateOne({ tenantId: 'tenant_iot', _id: 'dev_1002' }, { $set: { status: 'INACTIVE', updatedAt: new Date() } })
```

---

## Indexes

```javascript
db.devices.createIndex({ tenantId: 1, status: 1, type: 1 })
db.deviceReadings.createIndex({ tenantId: 1, deviceId: 1, observedAt: -1 })
db.deviceReadings.createIndex({ tenantId: 1, metric: 1, observedAt: -1 })
db.deviceReadings.createIndex({ observedAt: 1 })
```

---

## Aggregation Queries

Average temperature per device:

```javascript
db.deviceReadings.aggregate([
  { $match: { tenantId: 'tenant_iot', metric: 'temperature_celsius', observedAt: { $gte: ISODate('2026-07-01T00:00:00Z') } } },
  { $group: { _id: '$deviceId', avgValue: { $avg: '$value' }, maxValue: { $max: '$value' }, samples: { $sum: 1 } } },
  { $sort: { avgValue: -1 } }
])
```

Hourly rollup:

```javascript
db.deviceReadings.aggregate([
  { $match: { tenantId: 'tenant_iot', deviceId: 'dev_1001' } },
  { $group: { _id: { $dateTrunc: { date: '$observedAt', unit: 'hour' } }, avgValue: { $avg: '$value' }, minValue: { $min: '$value' }, maxValue: { $max: '$value' } } },
  { $sort: { _id: -1 } }
])
```

---

## Performance Considerations

- Batch inserts from ingestion workers.
- Keep raw telemetry indexes minimal.
- Use time-bounded queries only.
- Downsample old raw readings into hourly/daily summaries.

---

## Scaling Considerations

- Use time-series collections for native time-series optimization.
- Shard by device or tenant/device depending query pattern and tenant skew.
- Use Kafka or MQTT ingestion before MongoDB for backpressure.
- Archive cold data to object storage or OLAP.

---

## Security Considerations

- Authenticate devices with per-device credentials or certificates.
- Validate metric names and units.
- Prevent one compromised device from writing as another device.
- Encrypt sensitive location metadata if required.

---

## Optional API Layer

- `POST /devices`
- `POST /devices/{deviceId}/readings:batch`
- `GET /devices/{deviceId}/readings?from=&to=`
- `GET /metrics/rollups?metric=temperature_celsius`
- `PATCH /devices/{deviceId}`

---

## Interview Discussion Points

- When do you use time-series collections vs normal collections?
- What shard key avoids hot writes?
- How many indexes can a hot telemetry collection tolerate?
- How do you downsample and archive data?
- When would Cassandra or a time-series DB be better?
