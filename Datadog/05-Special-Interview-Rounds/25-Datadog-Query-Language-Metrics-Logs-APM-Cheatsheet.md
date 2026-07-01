# 25. Datadog Query Language: Metrics, Logs, APM, DQL Cheatsheet

## Metric Query Syntax

```text
aggregation:metric.name{filter-tags} by {group-tag}

aggregation:
  avg   -> average across all matching time series
  sum   -> sum of all matching time series
  min   -> minimum value across all matching time series
  max   -> maximum value across all matching time series
  p50/p75/p95/p99  -> percentiles (for distribution metrics)
  count -> count of time series (not values)

filter-tags:
  env:production                 exact match
  service:orders*                wildcard match
  !env:staging                   negation
  env:production,service:orders  AND (comma-separated)
  {*}                            all

group-tag (by):
  by {service}                   one line per service
  by {service,env}               one line per service+env combination

Examples:
  avg:trace.http.request.duration{env:production} by {service}
  sum:trace.http.request.hits{env:production,service:orders}.as_rate()
  p99:trace.http.request.duration{env:production,service:orders}
  min:system.cpu.idle{env:production} by {host}
```

---

## Metric Functions

```text
# Rate functions.
.as_rate()          -> count per second
.as_count()         -> reset to raw count

per_second(query)   -> equivalent to .as_rate()
derivative(query)   -> rate of change between points

# Arithmetic.
query_A / query_B * 100                  -> percentage
query_A - query_B                        -> difference
abs(query)                               -> absolute value

# Statistical.
anomalies(query, 'agile', 2)             -> anomaly detection band (2 stddev)
forecast(query, 'linear', 60)            -> forecast next 60 minutes
outliers(query, 'DBSCAN', 5.0)           -> outlier detection

# Smoothing.
ewma_20(query)                           -> exponentially weighted moving average (20-point)
moving_rollup(query, 300, 'avg')         -> 5-minute rolling average
robust_trend(query)                      -> trend without outlier influence
top(query, 5, 'mean', 'desc')            -> top 5 series by mean value

# Rollup.
query.rollup(avg, 60)                    -> average into 60-second buckets
query.rollup(sum, 300)                   -> sum into 5-minute buckets
query.rollup(max, 3600)                  -> maximum value per hour

# Fill gaps.
query.fill(last)                         -> fill missing points with last known value
query.fill(zero)                         -> fill missing points with 0
query.fill(interpolate)                  -> linear interpolation
```

---

## Log Search Query Syntax

```text
# Free text.
"connection refused"

# Attribute match.
service:orders-service
level:ERROR
@http.status_code:500
@error.type:NullPointerException

# Wildcard.
service:orders*
@http.url_details.path:/orders/*

# Range (numeric measure facets).
@duration:[100000000 TO 500000000]     (100ms to 500ms, in nanoseconds)
@http.response_size_bytes:[1000 TO *]  (above 1000 bytes)

# Boolean.
service:orders AND level:ERROR
service:orders OR service:payments
NOT level:DEBUG
service:orders AND (level:ERROR OR level:WARN)

# Exists.
_exists_:@error.type            (has an error.type field)
-_exists_:@error.type           (does NOT have error.type field)

# Tag search.
env:production AND service:orders-service

# Trace correlation.
@dd.trace_id:8423012345678901234
```

---

## Log Aggregation Queries (Log Analytics)

```text
# Count over time (timeseries widget).
Query: service:orders-service env:production level:ERROR
Group by: none (total count)
Rollup: count, 1 minute buckets

# Top errors by type.
Query: service:orders-service level:ERROR
Group by: @error.type
Measure: count
Sort: descending

# Average duration by endpoint.
Query: service:orders-service @http.status_code:200
Group by: @http.url_details.path
Measure: avg(@duration)

# Percentile latency from logs.
Query: service:orders-service
Group by: service
Measure: p99(@duration)
```

---

## APM Trace Search Syntax

```text
# Service and environment.
service:orders-service env:production

# Resource (endpoint).
resource_name:"GET /orders/{id}"

# HTTP attributes.
@http.status_code:500
@http.url:*/checkout*

# Errors only.
status:error
@error:true

# Duration filter (nanoseconds).
@duration:>1000000000          (> 1 second)
@duration:[500000000 TO 2000000000]  (500ms to 2000ms)

# Custom tags on spans.
@order.id:ORD-99001
@customer.tier:premium

# Version.
version:2.5.0
@deployment.version:2.5.0

# Combined.
service:orders env:production status:error @duration:>2000000000
```

---

## Datadog Query Language (DQL) For Dashboards

DQL is used in newer dashboard formula widgets for cross-signal queries:

```text
# Metrics formula.
a = sum:trace.http.request.hits{env:prod,service:orders}.as_rate()
b = sum:trace.http.request.errors{env:prod,service:orders}.as_rate()
formula: (b / a) * 100
```

---

## Monitor Alert Query Examples

```text
# Error rate threshold.
avg(last_5m):
  sum:trace.http.request.errors{env:production,service:orders}.as_rate()
  / sum:trace.http.request.hits{env:production,service:orders}.as_rate()
  * 100 > 2

# P99 latency.
avg(last_5m):p99:trace.http.request.duration{env:production,service:orders} > 2000000000

# Container restarts.
sum(last_5m):sum:kubernetes.containers.restarts{kube_namespace:production,kube_deployment:orders}.rollup(max) > 5

# Log error count.
logs("service:orders-service env:production level:ERROR").rollup("count").last("5m") > 50

# SLO burn rate.
burn_rate("slo_id", lookback_window_1:"5m", lookback_window_2:"1h", burn_rate_threshold:14.4)
```

---

## Useful Built-In Metric References

```text
# HTTP / APM (generated from traces).
trace.http.request.hits          -> request count
trace.http.request.errors        -> error count
trace.http.request.duration      -> latency (percentiles: p50, p75, p95, p99)

# Infrastructure.
system.cpu.user                  -> user-space CPU %
system.cpu.idle                  -> idle CPU %
system.mem.used                  -> memory used bytes
system.disk.in_use               -> disk usage fraction
system.net.bytes_sent            -> network bytes out
system.net.bytes_rcvd            -> network bytes in

# Kubernetes.
kubernetes.pods.running          -> pod count by namespace/deployment
kubernetes.containers.restarts   -> restart count
kubernetes.cpu.requests.total    -> total CPU requested

# JVM.
jvm.heap_memory                  -> JVM heap used
jvm.heap_memory_committed        -> JVM heap committed
jvm.gc.major_collection_time     -> major GC pause time
jvm.thread.count                 -> active JVM thread count

# Node.js runtime.
runtime.node.heap.used           -> Node.js heap used
runtime.node.event_loop.delay    -> event loop lag
runtime.node.gc.pause.time       -> GC pause time
```

---

## Interview Sound Bite

Datadog metric queries follow the pattern `aggregation:metric.name{filters} by {group}`. Metric functions include rate conversion (`.as_rate()`), anomaly detection (`anomalies()`), forecasting (`forecast()`), and smoothing (`ewma_20()`). Log queries use attribute-based filtering (`service:x level:ERROR`) with boolean operators and range filters. Trace queries support duration filters in nanoseconds, HTTP status codes, and custom span tags. Monitor alert queries specify the aggregation window using `avg(last_5m):` or `sum(last_10m):`.
