# 03. Datadog Metrics: Types, DogStatsD, Custom Metrics, Queries

## Goal

Understand metric types, how aggregation works, how to query metrics, and how tags shape the data.

---

## Metric Types

| Type | Description | Use Cases |
|---|---|---|
| Count | Sum of occurrences in a flush interval | requests, errors, events |
| Rate | Count per second (normalized) | requests/sec, errors/sec |
| Gauge | Point-in-time value | queue depth, CPU %, memory |
| Histogram | Distribution: min/max/avg/p50/p75/p95/p99 | response times, payload sizes |
| Distribution | Global percentiles computed server-side | p99 across all hosts combined |
| Set | Count of unique values | distinct active users |

Histogram vs Distribution:

- Histogram: percentiles per agent, then aggregated (may distort p99 across many hosts)
- Distribution: global percentiles computed on all raw values server-side (accurate cross-host p99)

---

## Metric Naming Convention

```text
product.category.what.unit

Examples:
  http.server.requests.count
  http.server.duration.ms
  jvm.heap.used.bytes
  db.queries.slow.count
  queue.messages.pending.count
```

Use lowercase with dots as separators. Avoid high-cardinality tag values in the metric name — use tags instead.

---

## Metric Query Syntax (Metrics Explorer / Dashboards)

```text
# Basic query.
avg:http.server.requests.count{env:production,service:orders-service}

# By tag (breakdown).
avg:http.server.requests.count{env:production} by {service}

# Sum across all.
sum:http.server.requests.count{env:production}

# Rate (per second).
per_second(sum:http.server.requests.count{env:production})

# Arithmetic.
sum:http.server.errors.count{env:production} / sum:http.server.requests.count{env:production} * 100

# Moving average smoothing.
ewma_20(avg:http.server.duration.ms{env:production})

# Rollup: aggregate to N-second buckets.
avg:jvm.heap.used.bytes{env:production}.rollup(avg, 60)
```

---

## Time Aggregation vs Space Aggregation

```text
Time aggregation:   reduce time series into fewer points (avg, sum, max, min over time buckets)
Space aggregation:  combine multiple time series into one (avg, sum, max, min across tag values)

Example:
  avg:cpu.utilization{env:production}
  ^-- time: average across the query time window
  ^-- space: average across all matching hosts
```

---

## Tag-Based Queries

Tags filter and group metrics:

```text
# Filter to specific env and service.
avg:http.request.duration.ms{env:production,service:orders}

# Group by service to see per-service breakdown.
avg:http.request.duration.ms{env:production} by {service}

# Exclude a specific version.
avg:http.request.duration.ms{env:production,!version:1.2.0}

# Wildcard tag value.
avg:http.request.duration.ms{service:orders*}
```

---

## Custom Metric Cardinality Warning

Each unique tag combination creates a separate time series.

```text
service=orders + env=prod + region=us-east       = 1 time series
service=orders + env=prod + region=eu-west       = 1 time series
service=orders + env=staging + region=us-east    = 1 time series

100 services x 3 envs x 5 regions = 1500 time series for one metric
```

If you add user_id or request_id as a tag: 1 million users = 1 million time series for one metric. This causes cardinality explosion and cost spikes. Never add unique IDs as metric tags.

---

## Metric Ingestion Limits

| Metric type | Datadog-submitted metrics | Custom metrics |
|---|---|---|
| Infrastructure metrics | included in host pricing | billed above free quota |
| DogStatsD / code-submitted | counted as custom metrics | billed above free quota |
| APM trace metrics | included in APM pricing | n/a |

Monitor custom metric usage in Organization Settings → Plan and Usage.

---

## Useful Built-In Metrics

```text
# System.
system.cpu.user
system.cpu.idle
system.mem.used
system.disk.in_use
system.net.bytes_sent

# Docker/Kubernetes.
container.cpu.usage
container.memory.usage
kubernetes.cpu.requests.total
kubernetes.pods.running

# JVM (from dd-java-agent or JMX integration).
jvm.heap_memory
jvm.heap_memory_committed
jvm.non_heap_memory
jvm.gc.major_collection_time
jvm.gc.minor_collection_count

# APM (generated from traces).
trace.http.request.hits
trace.http.request.errors
trace.http.request.duration
```

---

## Interview Sound Bite

Datadog metrics have five types: count, rate, gauge, histogram, and distribution. Distribution metrics compute global server-side percentiles, making them more accurate for p99 across many hosts than histograms. Metric queries use `aggregation:metric.name{filter-tags} by {group-tag}` syntax. Custom metric cardinality is the primary driver of Datadog cost: each unique tag combination is a separate time series. High-cardinality values like user IDs must never appear as metric tags.
