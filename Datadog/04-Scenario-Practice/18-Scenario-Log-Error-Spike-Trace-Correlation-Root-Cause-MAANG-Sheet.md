# 18. Scenario: Log Error Spike + Trace Correlation Root Cause

## Scenario Setup

```text
Alert fires at 09:15 UTC:
  Monitor: "Orders Service ERROR Log Count > 50 in 5 minutes"
  Value: 347 ERROR logs in last 5 minutes
  Normal baseline: 2-3 ERROR logs per 5 minutes
  Service: orders-service env:production
```

---

## Investigation Workflow

### Step 1: Open Log Explorer And Filter

```text
Logs -> Explorer

Query: service:orders-service env:production level:ERROR
Time: last 30 minutes
```

Immediate observations:

```text
Total ERROR count: 347 in 5 minutes (started at 09:13)
Before 09:13: 2 errors per 5 minutes

Top error messages (group by @message):
  1. "Connection refused: payments-service:8080"  (312 occurrences, 90%)
  2. "Timeout after 5000ms calling payments-service"  (35 occurrences, 10%)
```

Finding: All errors are related to calls to payments-service. Orders-service cannot reach it.

### Step 2: Examine One Error Log In Detail

```text
Click one error log.

Fields:
  @timestamp: 2024-01-15T09:13:22.451Z
  level: ERROR
  message: "Connection refused: payments-service:8080"
  service: orders-service
  env: production
  version: 2.3.1
  dd.trace_id: 7712345678901234567
  dd.span_id: 8812345678901234567
  @error.type: java.net.ConnectException
  @error.message: Connection refused (Connection refused)
  @error.stack: at sun.nio.ch.Net.connect0...
```

The log contains `dd.trace_id`. Click it → "View related trace".

### Step 3: View The Trace

```text
Trace Explorer opens at trace 7712345678901234567.

Trace timeline:
  orders-service POST /checkout              5,023ms  ERROR
    ├── OrderService.validateCart               50ms
    ├── InventoryService.checkStock             90ms
    └── PaymentService.charge                4,883ms  ERROR
          └── HTTP POST payments-service:8080    (connection refused)
```

The payment call spans shows a connection refused error after 5 seconds. The HTTP client retried and eventually returned a ConnectException.

### Step 4: Check payments-service Health

```text
APM -> Services -> payments-service

Check:
  Service status: No data after 09:12 UTC
  Request count: dropped to 0 at 09:12
  Last trace: 09:12:47 UTC

Conclusion: payments-service went down at 09:12.
```

### Step 5: Check Infrastructure

```text
Infrastructure -> Containers -> filter: service:payments-service

Container list:
  payments-service-abc123: Terminated (reason: OOMKilled) at 09:12:45 UTC
  payments-service-def456: Terminated (reason: OOMKilled) at 09:12:47 UTC
  payments-service-ghi789: Running (started 09:14:30 UTC - restarted by K8s)
```

Both payments-service pods were OOMKilled simultaneously at 09:12:45. The Kubernetes readiness probe failed, removing them from the service endpoints. Orders-service was getting connection refused for ~2 minutes until K8s restarted and readiness probes passed.

### Step 6: Find Why OOMKilled

```text
Log Explorer:
  service:payments-service env:production level:WARN
  Time: 09:10 to 09:12

Found at 09:10:30:
  "WARNING: JVM heap usage 95% - GC pressure building"
  "WARNING: JVM heap usage 98% - major GC triggered"

And at 09:11:50:
  "ERROR: Unable to acquire memory for large batch operation - batch_size=50000"

Root cause: payments-service was processing a large batch job at 09:11
that allocated 50,000 records into heap simultaneously,
causing heap exhaustion and OOMKill.
```

### Step 7: Resolution

```text
Immediate:
  1. payments-service pods restarted by K8s at 09:14.
  2. Error rate returns to normal at 09:15.
  3. No orders were lost (orders-service returned 503, frontend handled gracefully).

Root fix:
  1. Increase payments-service JVM heap limit from 512MB to 1GB.
  2. Add JVM heap monitor: alert at 80% heap usage.
  3. Refactor large batch to process in chunks of 1,000 records.
  4. Add circuit breaker in orders-service to fail fast when payments-service is unavailable.
```

### Step 8: Timeline Summary

```text
09:10:30  payments-service heap pressure warning
09:11:50  large batch starts (50K records in heap)
09:12:45  payments-service pods OOMKilled
09:12:45  orders-service starts receiving connection refused errors
09:13:00  ERROR log count spike begins
09:15:00  Datadog monitor fires (5-min evaluation window)
09:15:30  K8s restarts complete, readiness probes pass
09:16:00  error rate returns to normal
09:16:30  monitor auto-resolves
```

---

## Key Datadog Views Used

| View | Purpose |
|---|---|
| Log Explorer | Find error pattern and top error messages |
| Trace from log dd.trace_id | Jump from log to trace |
| APM service page (payments-service) | Confirm service went down |
| Infrastructure Containers | Find OOMKill events |
| Log Explorer (payments-service) | Find heap exhaustion warnings |

---

## Interview Sound Bite

Start with Log Explorer filtered to the service and ERROR level. Group by message to identify the dominant error pattern. Pick one log with dd.trace_id and click "view related trace" to jump to the flame graph. The trace shows which downstream call failed. Check that service's APM page for request dropoff. Pivot to Infrastructure Containers to find OOMKill events. Correlate with that service's logs to find the memory exhaustion cause. This click-chain from log to trace to infrastructure is what Datadog log correlation makes possible.
