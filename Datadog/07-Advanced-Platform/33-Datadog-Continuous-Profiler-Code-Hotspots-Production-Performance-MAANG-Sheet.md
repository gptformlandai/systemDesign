# 33. Datadog Continuous Profiler: Code Hotspots And Production Performance

## Goal

Understand how Continuous Profiler finds CPU, memory, lock, and wall-time bottlenecks that are invisible from metrics and sometimes invisible from traces.

---

## Mental Model

APM tells you which request or span is slow.

Profiler tells you which code path is consuming resources while the program runs.

```text
APM span: "checkout took 2.4s"
Profiler: "65% of CPU was spent in PromoRuleEvaluator.matchRules()"
```

Traces explain request shape. Profiles explain runtime cost.

---

## Why It Exists

Metrics can show high CPU. Traces can show slow endpoints. Neither always shows which function is burning CPU, allocating memory, or blocking on locks.

Profiler helps answer:

- Which function consumes the most CPU?
- Is latency caused by CPU, I/O wait, garbage collection, or lock contention?
- Which code path allocates the most memory?
- Did a deployment introduce a performance regression?
- Which service wastes the most compute cost?

---

## Profile Types

| Profile Type | What It Shows | Example Root Cause |
|---|---|---|
| CPU | Where CPU cycles are spent | inefficient loop, regex, serialization |
| Wall time | Where elapsed time is spent | blocking I/O, slow lock, network wait |
| Allocation | Where memory is allocated | object churn, large buffers |
| Heap | What is retained in memory | leak, cache growth |
| Lock | Thread contention | synchronized block, mutex contention |
| Exception | Where exceptions are thrown | retry loop, validation storm |

Supported profile types vary by language and runtime.

---

## How Profiling Works

```text
1. The profiler samples running application threads at intervals.
2. Each sample records the current stack trace.
3. Samples are aggregated by service/env/version.
4. Datadog visualizes hot methods as flame graphs.
5. You compare profiles before/after deploys or between versions.
6. You pivot from APM spans to profiles when trace and profile data overlap.
```

Sampling means profiler overhead is controlled. It does not trace every function call.

---

## Java Enablement Example

```bash
java \
  -javaagent:/opt/datadog/dd-java-agent.jar \
  -Ddd.service=orders-service \
  -Ddd.env=production \
  -Ddd.version=1.8.3 \
  -Ddd.profiling.enabled=true \
  -jar orders-service.jar
```

Kubernetes:

```yaml
env:
  - name: DD_SERVICE
    value: orders-service
  - name: DD_ENV
    value: production
  - name: DD_VERSION
    value: "1.8.3"
  - name: DD_PROFILING_ENABLED
    value: "true"
```

---

## Investigation Workflow

```text
Alert:
  service:orders-service CPU > 85% for 15 minutes

Step 1: Check service metrics.
  CPU high, request rate normal, error rate normal.

Step 2: Check APM.
  p95 latency increased from 180ms to 420ms.

Step 3: Open profiler for service/env/version.
  CPU profile shows 58% in JsonSchemaValidator.validate().

Step 4: Compare profile version 1.8.2 vs 1.8.3.
  New version added validation for every item in cart.

Step 5: Fix.
  Cache compiled schema or validate once per request.
```

---

## Reading A Flame Graph

```text
Width  = total resource consumption
Height = stack depth
Color  = visual separation, not severity
```

The widest frame is the expensive code path. Do not chase the tallest stack; chase the widest stack.

---

## Profiler + APM Pivot

Use this pattern:

```text
1. Start from latency or error monitor.
2. Open APM service page and identify slow resource.
3. Open traces for that resource.
4. If spans show "application code" but not why, pivot to profiles.
5. Filter profile by service/env/version and time window.
6. Compare against previous version or normal window.
```

APM narrows the failing request path. Profiler identifies the expensive method inside that path.

---

## Cost Optimization Use Case

Profiler is not only for incidents.

```text
Service: recommendation-service
Monthly compute cost: high
Traffic: stable
CPU profile: 40% spent in JSON serialization
Fix: switch serializer and remove repeated object mapping
Result: CPU drops 25%, autoscaler lowers replica count
```

This is why profiler belongs in FinOps discussions too.

---

## Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Using profiler only during incidents | You miss regressions | Keep it continuously enabled for critical services |
| Ignoring version tags | Cannot compare deploys | Always set `DD_VERSION` |
| Chasing tall stacks | Tall does not mean expensive | Look for wide frames |
| Treating traces as code profiler | Spans are not method samples | Use profiler for code hotspots |
| Profiling only one replica | Hotspot may be uneven | Compare across pods/hosts |

---

## Practical Question

> A service has high CPU and p95 latency doubled after a deploy, but traces only show time inside application code. How do you debug with Datadog?

---

## Strong Answer

I would start with APM to identify the affected service, endpoint, and version. If the slow span is mostly application code and not a downstream dependency, I would open Continuous Profiler filtered to that service, environment, version, and time window.

Then I would compare CPU and wall-time profiles between the previous version and the new version. The widest new frame identifies the expensive code path. If CPU is high, I look for compute-heavy methods. If wall time is high but CPU is not, I look for blocking I/O or locks. The fix depends on the hotspot: caching, algorithm change, reduced serialization, connection pool tuning, or lock removal.

---

## Interview Sound Bite

APM tells me where the request slowed down; profiler tells me which code consumed the resources. For production performance debugging, the strongest workflow is monitor -> APM trace -> profile comparison by service/env/version.
