# Cassandra Time-Series, TTL, and Bucketing Patterns - Gold Sheet

> Track File #9 of 25 - Group 02: Intermediate Backend
> For: backend/database/system design interviews | Level: intermediate to senior | Mode: time-series schemas, TTL, partition safety

This sheet builds:
- Time-series table design
- Bucketing for bounded partitions
- TTL and tombstone-aware modeling

---

## 1. Why Cassandra Fits Time-Series

Cassandra is often strong for append-heavy time-series workloads because writes can be distributed and rows can be clustered by time inside partitions.

Good examples:

- metrics by device/hour
- audit events by tenant/day
- chat messages by room/day
- notification attempts by user/day
- fraud signals by account/day

---

## 2. Time-Series Table Pattern

```sql
CREATE TABLE metrics_by_device_hour (
  device_id text,
  bucket_hour timestamp,
  metric_ts timestamp,
  metric_name text,
  value double,
  tags map<text, text>,
  PRIMARY KEY ((device_id, bucket_hour), metric_ts, metric_name)
) WITH CLUSTERING ORDER BY (metric_ts DESC, metric_name ASC);
```

Serves:

```sql
SELECT *
FROM metrics_by_device_hour
WHERE device_id = 'd1'
  AND bucket_hour = '2026-07-01T10:00:00Z'
LIMIT 100;
```

---

## 3. Bucket Size Decision

| Bucket | Use When | Risk |
|---|---|---|
| minute | extremely high ingest per key | more partitions to query |
| hour | common metrics/IoT default | good balance for many workloads |
| day | moderate ingest per key | can become too wide for hot devices/rooms |
| week/month | low ingest per key | dangerous if growth surprises you |

Rule:

```text
Choose the largest bucket that keeps partitions safely bounded under peak traffic.
```

---

## 4. TTL

TTL is useful for automatic expiry:

```sql
INSERT INTO metrics_by_device_hour (...)
VALUES (...)
USING TTL 2592000;
```

TTL tradeoffs:

| Benefit | Cost |
|---|---|
| automatic retention | tombstones after expiry |
| simple lifecycle | compaction pressure |
| fits ephemeral data | expiry waves can hurt reads |

Use TimeWindowCompactionStrategy for many time-series TTL workloads so old windows can be compacted and dropped more predictably.

---

## 5. Latest-N Pattern

For latest events:

```sql
CREATE TABLE latest_events_by_tenant_day (
  tenant_id text,
  event_day date,
  event_ts timestamp,
  event_id timeuuid,
  event_type text,
  payload text,
  PRIMARY KEY ((tenant_id, event_day), event_ts, event_id)
) WITH CLUSTERING ORDER BY (event_ts DESC, event_id DESC);
```

Query:

```sql
SELECT *
FROM latest_events_by_tenant_day
WHERE tenant_id = 't1'
  AND event_day = '2026-07-01'
LIMIT 50;
```

---

## 6. Anti-Patterns

- partition key = only `device_id` for data retained for years
- partition key = only `event_day`, causing all writes for a day to concentrate
- TTL on massive partitions without compaction planning
- deleting large ranges repeatedly
- queries that scan many buckets for every user request
- storing high-cardinality tags in collections without query need

---

## 7. Strong Answer

Question:

> How would you model IoT metrics in Cassandra?

Strong answer:

```text
I would model the table by the read pattern, usually metrics_by_device_hour or metrics_by_tenant_device_hour. The partition key would include device or tenant plus a time bucket to distribute writes and bound partition size. Clustering would sort by metric timestamp, usually descending for latest reads. I would use TTL only with tombstone and compaction planning, likely TWCS for time-windowed data, and I would export heavy analytics to an OLAP system rather than scanning Cassandra broadly.
```

---

## 8. Revision Notes

- One-line summary: Time-series Cassandra design is partition key plus time bucket plus clustering order plus TTL discipline.
- Three keywords: bucket, TTL, TWCS.
- One interview trap: unbounded partition per device or room.
- Memory trick: bucket time before time buckets you.