# 17. Scenario: Production High Latency — APM Flame Graph Investigation

## Scenario Setup

```text
Alert fires at 14:32 UTC:
  Monitor: "Orders Service P99 Latency > 2000ms"
  Value: P99 = 4,821ms (normal: 280ms)
  Scope: service:orders-service env:production resource:POST /orders
```

---

## Investigation Workflow

### Step 1: Confirm And Scope The Impact

```text
APM -> Services -> orders-service

Check:
  - Request rate: still normal (latency issue, not traffic drop)
  - Error rate: 0.4% (slightly elevated but not a full outage)
  - P99 latency: 4,821ms  P95: 3,100ms  P50: 290ms
    -> Only the slowest requests are affected (long tail, not median)
  - Started: ~14:25 UTC (7 minutes before alert fired due to 5-min eval window)
```

### Step 2: Filter To Slow Traces In Trace Explorer

```text
APM -> Traces

Filter:
  service:orders-service
  resource_name:"POST /orders"
  @duration:>2000000000       (> 2 seconds, in nanoseconds)
  env:production
  Time: last 30 minutes

Sort by duration descending.
Open the slowest trace.
```

### Step 3: Analyze The Flame Graph

```text
Flame graph for trace abc123 (total duration: 6,200ms):

orders-service POST /orders              6,200ms  (root span)
  ├── OrderService.validateInventory       120ms
  ├── OrderService.applyPromoCodes         150ms
  ├── postgres INSERT INTO orders        5,840ms  <-- THIS IS THE PROBLEM
  └── kafka publish OrderCreated            80ms
```

Finding: The `postgres INSERT INTO orders` span is taking 5,840ms. This is clearly the bottleneck.

### Step 4: Check DB Span Details

```text
Click the postgres span.

Span tags:
  db.type: postgresql
  db.name: orders_db
  db.statement: INSERT INTO orders (customer_id, total, items, ...) VALUES (...)
  peer.hostname: orders-db-prod.cluster.local
  peer.port: 5432
  out.host: orders-db-prod.cluster.local
```

The statement is a simple INSERT but taking 5.8 seconds. Suspects:

- Database table lock
- Index maintenance during insert (if index was recently added)
- Database server resource exhaustion

### Step 5: Correlate With Infrastructure Metrics

```text
Navigate to the postgres host:
Infrastructure -> Hosts -> orders-db-prod

Check at time 14:25-14:40:
  system.cpu.user: 98%  <-- CPU is saturated
  system.disk.await: 45ms  <-- disk I/O latency elevated
  postgresql.connections: 498/500  <-- connection pool nearly exhausted
```

Root cause confirmed: Database CPU saturation is causing lock contention and slow INSERTs.

### Step 6: Trace Back To Trigger

```text
Log Explorer:
  service:orders-db-proxy env:production @duration:>1000 time: last 2 hours

Found: 14:21 UTC - "ALTER TABLE orders ADD COLUMN promo_code VARCHAR(50)"

Migration was running during peak traffic time.
ALTER TABLE on a large table causes a full table lock in some PostgreSQL versions.
```

### Step 7: Resolution

```text
Immediate:
  1. Kill the migration query:
     SELECT pg_cancel_backend(pid) FROM pg_stat_activity WHERE state = 'active' AND query LIKE '%ALTER TABLE orders%';
  2. P99 latency returns to normal within 2 minutes after migration killed.

Root fix:
  1. Run migrations outside peak traffic window (scheduled maintenance).
  2. Use pg_repack or online DDL tooling for zero-downtime schema changes.
  3. Add pre-deployment check: block deploys if long-running queries active on DB.
```

### Step 8: Verify Recovery

```text
APM -> orders-service P99 latency graph:
  14:32 alert fires
  14:41 migration killed
  14:43 P99 returns to 290ms -> monitor auto-resolves

SLO impact:
  17 minutes of elevated errors = 0.39% of hourly error budget consumed
  Monthly SLO still within target
```

---

## Key Datadog Views Used

| View | Purpose |
|---|---|
| APM Service page | Initial latency/error rate assessment |
| Trace Explorer | Filter to slow traces with duration filter |
| Flame graph | Identify which span is slow |
| Span detail | Get DB query, hostname, port |
| Infrastructure host | Correlate with CPU/disk/connection metrics |
| Log Explorer | Find the triggering event (migration query) |

---

## Interview Sound Bite

When a latency alert fires, start at the APM service page to confirm scope (P99 elevated, error rate, request rate). Open Trace Explorer filtered by `duration > threshold` to find the slowest traces. Open the flame graph — the widest bar is the bottleneck span. Click the span for DB query text, hostname, and port. Pivot to infrastructure metrics for that DB host to find CPU/disk saturation. Correlate with logs to find the triggering event. In this case, an ALTER TABLE migration during peak traffic caused lock contention and 17 minutes of high latency.
