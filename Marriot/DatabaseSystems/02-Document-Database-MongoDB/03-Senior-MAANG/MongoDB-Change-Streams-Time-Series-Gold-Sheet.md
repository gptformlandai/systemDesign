    # MongoDB Change Streams and Time-Series Collections - Gold Sheet

    > **Track File #16 of 28 - Group 03: Senior MAANG**
    > For: backend/database/system design interviews | Level: advanced backend | Mode: real-time events and telemetry storage

    This sheet builds:
    - Change stream concepts, resume tokens, failure handling
- Time-series collections, metaField, granularity, TTL
- Event-driven and telemetry use cases

Original master-map sections included here:
- 16. Change Streams
- 17. Time-Series Collections

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 16. Change Streams

### What Are Change Streams?

Change streams let applications subscribe to inserts, updates, deletes, and replacements from MongoDB collections, databases, or clusters.

They are built on the oplog and provide a resumable stream of changes.

### Use Cases

- real-time notifications
- cache invalidation
- event-driven architecture
- audit pipeline
- sync search index
- trigger downstream processing
- update materialized views
- stream changes into Kafka

### Node.js Example

```javascript
const collection = client.db("appdb").collection("orders");
const changeStream = collection.watch([
  { $match: { "fullDocument.status": "PAID" } }
], { fullDocument: "updateLookup" });

for await (const change of changeStream) {
  console.log(change.operationType, change.documentKey, change.fullDocument);
  // Store change._id as resume token after successful processing.
}
```

### Python Example

```python
with db.orders.watch(full_document="updateLookup") as stream:
    for change in stream:
        print(change["operationType"], change["documentKey"])
        # Persist change["_id"] as resume token after processing.
```

### Resume Token

Each change event has a resume token in `_id`. Persist it after processing so the consumer can resume after failure.

Concept:

```javascript
const stream = collection.watch([], { resumeAfter: savedResumeToken });
```

### Failure Handling

- Process events idempotently.
- Save resume token only after successful side effects.
- Handle invalidated streams.
- Monitor lag.
- Backoff on transient errors.
- Rebuild derived state from source collection if stream history is no longer available.

### Scaling Consumers

Change streams are not a full replacement for Kafka for all workloads.

For scale:

- partition by tenant/entity after consuming
- use one stream per shard/collection only when justified
- write to queue for fanout
- keep handlers fast
- use idempotency keys

---

---

## 17. Time-Series Collections

### What Are Time-Series Collections?

Time-series collections optimize storage and queries for measurements over time.

Use cases:

- IoT metrics
- monitoring
- financial ticks
- telemetry
- app metrics
- sensor data

### Create Time-Series Collection

```javascript
db.createCollection("deviceMetrics", {
  timeseries: {
    timeField: "ts",
    metaField: "metadata",
    granularity: "seconds"
  },
  expireAfterSeconds: 60 * 60 * 24 * 30
})
```

Fields:

| Field | Meaning |
|---|---|
| `timeField` | Timestamp field for measurement |
| `metaField` | Stable metadata used for grouping, such as device ID |
| `granularity` | Expected precision: seconds, minutes, hours |
| `expireAfterSeconds` | Retention TTL |

### Insert Example

```javascript
db.deviceMetrics.insertOne({
  ts: new Date(),
  metadata: { tenantId: "t1", deviceId: "d1", region: "us" },
  temperature: 71.4,
  humidity: 0.42
})
```

### Bucketing Concept

MongoDB internally groups time-series measurements into buckets to improve compression and query efficiency.

Modeling advice:

- Keep metadata stable and not too large.
- Query by metadata plus time range.
- Do not store high-cardinality rapidly changing values in `metaField`.

### Query Example

```javascript
db.deviceMetrics.find({
  "metadata.deviceId": "d1",
  ts: { $gte: start, $lt: end }
})
```

### Aggregation Example

```javascript
db.deviceMetrics.aggregate([
  { $match: { "metadata.tenantId": "t1", ts: { $gte: start, $lt: end } } },
  {
    $group: {
      _id: {
        deviceId: "$metadata.deviceId",
        hour: { $dateToString: { format: "%Y-%m-%dT%H", date: "$ts" } }
      },
      avgTemp: { $avg: "$temperature" },
      maxTemp: { $max: "$temperature" }
    }
  }
])
```

### Retention

Use TTL for automatic cleanup. For compliance-sensitive data, combine TTL with archival strategy and verification.

---

---
