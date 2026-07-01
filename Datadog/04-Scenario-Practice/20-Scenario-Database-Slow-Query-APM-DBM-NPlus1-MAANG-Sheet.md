# 20. Scenario: Database Slow Query, APM Database Monitoring, N+1 Detection

## Scenario Setup

```text
Alert fires at 11:30 UTC:
  Monitor: "Orders Service P50 Latency > 500ms"
  Value: P50 = 780ms (normal: 120ms)
  Note: P50 is slow (not just P99) -> affects MOST requests, not just outliers
  Service: orders-service env:production resource:GET /orders/history
```

---

## Investigation Workflow

### Step 1: APM Service Overview

```text
APM -> orders-service -> GET /orders/history

Metrics:
  P50: 780ms  P95: 2,100ms  P99: 3,800ms
  Error rate: 0.2% (within normal range)
  Request rate: 450 req/min (normal)

Conclusion: This is a pure performance issue, not an error.
Started: ~11:20 UTC
```

### Step 2: Flame Graph Analysis

```text
Open a slow trace (duration > 2000ms).

Flame graph:
  GET /orders/history                           2,340ms total
    ├── OrderHistoryService.getHistory           2,280ms  <-- most of the time
    │    ├── postgres SELECT user details          12ms
    │    ├── postgres SELECT order (id=1001)        8ms
    │    ├── postgres SELECT order (id=1002)        9ms
    │    ├── postgres SELECT order (id=1003)       10ms
    │    ├── ... (47 more SELECT spans)          ...
    │    └── postgres SELECT order (id=1050)        9ms  <-- 50 queries total!
    └── response serialization                    60ms
```

N+1 Problem identified:

```text
Instead of:
  1 query to get user's order IDs (1 query)
  1 query to get all those orders by IDs (1 query, total = 2 queries)

Code is doing:
  1 query to get user's order IDs -> returns 50 IDs
  50 individual queries to get each order by ID (1+50 = 51 queries)
```

### Step 3: Database Monitoring (DBM) Deep Dive

```text
APM -> Database Monitoring -> click the postgres span

DBM provides:
  Query: SELECT * FROM orders WHERE id = ?
  Normalized query hash: abc123xyz
  Execution time: 8-12ms per query
  Executions in last 1 hour: 87,450 times
  
  Query explain plan:
    Seq Scan on orders
    Filter: (id = 1001)
    Rows: 1 (estimated), 1 (actual)
    -> Seq scan on a table with 5M rows = expensive for each of 50 lookups
```

Issue confirmed: Sequential scan instead of index scan, plus N+1 query pattern.

### Step 4: Check Database Metrics

```text
Infrastructure -> Hosts -> orders-db-prod

postgresql.bgwriter.buffers_clean:  normal
postgresql.connections:             320/500 (high but not at limit)
postgresql.queries.time:            avg 9ms/query (query itself is fast)
postgresql.queries.count:           89,000/min (87% increase from normal 48,000/min)

The query itself is not slow — the VOLUME of queries is the problem.
```

### Step 5: Confirm N+1 In Code

```text
Log Explorer:
  service:orders-service env:production @span.resource_name:*SELECT*
  group by @span.resource_name

Top queries by count:
  "SELECT * FROM orders WHERE id = ?" -> 87,450 executions in last hour
  "SELECT * FROM orders WHERE user_id = ?" -> 1,749 executions (one per API call)
  Ratio: 87,450 / 1,749 = ~50 DB calls per API call (N+1 confirmed)
```

### Step 6: Root Cause

```java
// BEFORE: N+1 query problem
public List<Order> getOrderHistory(Long userId) {
    // 1 query: get order IDs
    List<Long> orderIds = orderRepository.findIdsByUserId(userId);
    
    // N queries: fetch each order individually
    return orderIds.stream()
        .map(id -> orderRepository.findById(id).orElseThrow())  // N=50 queries
        .collect(Collectors.toList());
}
```

```java
// AFTER: Single query with IN clause
public List<Order> getOrderHistory(Long userId) {
    // 1 query: get all orders for user
    return orderRepository.findAllByUserId(userId);
    // SQL: SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC
    // + Add index on (user_id, created_at) for efficiency
}
```

### Step 7: Index Analysis

```sql
-- Check existing indexes on orders table.
SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'orders';

-- Missing index found: no index on user_id
-- Add index:
CREATE INDEX CONCURRENTLY idx_orders_user_id_created_at
  ON orders(user_id, created_at DESC);
```

### Step 8: Resolution And Verification

```text
Fix deployed: version 2.4.1 with N+1 fix and new index.

APM -> orders-service -> GET /orders/history after deploy:
  P50: 95ms   (was 780ms, 88% improvement)
  P95: 230ms  (was 2,100ms)
  P99: 450ms  (was 3,800ms)

DB queries:
  "SELECT * FROM orders WHERE user_id = ?" -> 1,750 executions/hour
  "SELECT * FROM orders WHERE id = ?" -> 0 executions/hour (N+1 eliminated)
```

---

## Database Monitoring Setup

```yaml
# In datadog.yaml or PostgreSQL integration config.
init_config:

instances:
  - dbm: true                          # enable Database Monitoring
    host: orders-db-prod.cluster.local
    port: 5432
    username: datadog
    password: your-password
    dbname: orders_db
    tags:
      - env:production
      - service:orders-db
```

DBM requires PostgreSQL 10+ and the `datadog` user to have `pg_monitor` role:

```sql
CREATE USER datadog WITH PASSWORD 'your-password';
GRANT pg_monitor TO datadog;
GRANT SELECT ON pg_stat_database TO datadog;
```

---

## N+1 Detection Pattern

```text
N+1 symptoms in Datadog:
  1. Flame graph shows many identical small spans (same DB resource name, similar duration)
  2. DB query count >> API call count (ratio > 10:1 is suspicious)
  3. P50 latency is high (affects all requests, not just outliers)
  4. DB query volume spike without a traffic spike

To detect automatically:
  Monitor: sum:postgresql.queries.count{env:production,resource:*SELECT*orders*}
           / sum:trace.http.request.hits{env:production,resource:"GET /orders/history"}
           > 20  (alert if more than 20 DB queries per API call)
```

---

## Interview Sound Bite

When P50 latency is high (not just P99), it affects all users, suggesting a structural code problem rather than an outlier. The flame graph reveals N+1: dozens of identical small DB spans per request. Database Monitoring provides the exact normalized query, execution count, and explain plan. The fix is always to replace individual-record fetches in a loop with a single batch query (SQL IN clause or JOIN). Monitor DB query count per API call ratio to detect N+1 regressions in CI or production.
