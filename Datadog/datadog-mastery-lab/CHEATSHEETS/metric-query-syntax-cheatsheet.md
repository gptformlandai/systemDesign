# Metric Query Syntax Cheatsheet

## Basic Structure

```text
aggregation:metric.name{filter-tags} by {group-tag}
```

## Aggregations

```text
avg:    average across all matching time series
sum:    sum all matching time series
min:    minimum value
max:    maximum value
p50:    50th percentile (distribution metrics only)
p75:    75th percentile (distribution metrics only)
p90:    90th percentile (distribution metrics only)
p95:    95th percentile (distribution metrics only)
p99:    99th percentile (distribution metrics only)
```

## Filters

```text
{env:production}               exact match
{env:production,service:orders} AND match
{service:orders*}              wildcard
{!env:staging}                 NOT match
{*}                            all
```

## Group By

```text
by {service}
by {service,env}
by {host}
by {kube_namespace}
```

## Rate Conversion

```text
metric.as_rate()               counts per second
metric.as_count()              raw count
per_second(sum:metric{*})      equivalent to .as_rate()
```

## Common Error Rate Formula

```text
100 * sum:trace.http.request.errors{env:prod,service:orders}.as_rate()
/     sum:trace.http.request.hits{env:prod,service:orders}.as_rate()
```

## Functions

```text
anomalies(query, 'agile', 2)          anomaly detection (2 std deviations)
forecast(query, 'linear', 60)         forecast 60 minutes ahead
ewma_20(query)                        exponential moving average (20 points)
moving_rollup(query, 300, 'avg')      5-minute rolling average
top(query, N, 'mean', 'desc')         top N series by mean
rollup(avg|sum|max|min, N)            aggregate into N-second buckets
fill(last|zero|interpolate)           fill missing data points
```

## APM Trace Metrics

```text
trace.http.request.hits              request count
trace.http.request.errors            error count
trace.http.request.duration          latency (use p50, p95, p99 prefix)
```

## Infrastructure

```text
system.cpu.user
system.cpu.idle
system.mem.used
system.disk.in_use
container.cpu.usage
container.memory.usage
kubernetes.containers.restarts
kubernetes.pods.running
jvm.heap_memory
jvm.gc.major_collection_time
runtime.node.event_loop.delay
```
