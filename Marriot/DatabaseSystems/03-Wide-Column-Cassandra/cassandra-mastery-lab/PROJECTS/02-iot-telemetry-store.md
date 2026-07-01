# Project 02: IoT Telemetry Store

Goal: model high-volume time-series metrics in Cassandra.

---

## Requirements

- 20 million devices.
- One metric per minute per device.
- 30-day raw retention.
- Query recent metrics by device and hour.
- Export aggregates to analytics storage.

---

## Table

```text
metrics_by_device_hour ((device_id, bucket_hour), metric_ts, metric_name)
```

---

## Design Decisions

- `device_id + bucket_hour` bounds partitions.
- `metric_ts DESC` supports latest-first reads.
- TTL keeps raw retention manageable.
- TWCS is a good compaction candidate for time-windowed TTL data.

---

## Capacity Worksheet

```text
rows_per_device_hour = metrics_per_minute * 60
bytes_per_partition = rows_per_device_hour * average_row_size
cluster_storage = daily_events * average_row_size * retention_days * RF * overhead
```

---

## Interview Talking Points

- why dashboards should not scan Cassandra broadly
- why hot devices may need finer buckets
- how TTL creates tombstones
- what to monitor: writes, disk, compaction, p99 reads