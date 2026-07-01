# 26. Datadog Anti-Patterns, Common Bugs, Debugging Traps

## Anti-Pattern 1: High-Cardinality Metric Tags

```text
Mistake:
  statsd.increment("api.request", tags=["user_id:usr-12345", "env:production"])

Why it breaks:
  1 million users = 1 million time series for ONE metric.
  Datadog bills per time series. This causes a cost explosion overnight.

Correct approach:
  statsd.increment("api.request", tags=["env:production", "service:orders"])
  
  High-cardinality user data belongs in TRACES (as span attributes), not metrics.
  span.setTag("user.id", userId)  <- searchable in Trace Explorer without cost explosion
```

---

## Anti-Pattern 2: dd-trace Initialized After Library Imports

```javascript
// WRONG - dd-trace initialized after express.
const express = require('express')   // express is already loaded
const tracer = require('dd-trace').init()  // too late to instrument express

// CORRECT - dd-trace must be the very first import.
const tracer = require('dd-trace').init()  // instruments all subsequent require() calls
const express = require('express')         // now express is intercepted
```

Symptom: Express routes show no spans in Trace Explorer; no auto-instrumentation working.

---

## Anti-Pattern 3: Using Wrong Logger Format For Log Correlation

```java
// WRONG - plain text format, trace IDs not parseable.
<Pattern>%d{HH:mm:ss} %-5level - %msg%n</Pattern>

// WRONG - tries to log trace ID manually without MDC.
logger.info("Processing order - traceId: " + traceId);  // fragile

// CORRECT - JSON encoder reads MDC fields injected by dd-java-agent.
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <includeMdcKeyName>dd.trace_id</includeMdcKeyName>
  <includeMdcKeyName>dd.span_id</includeMdcKeyName>
</encoder>
```

Symptom: Logs and traces appear unrelated in Datadog UI; no "view related trace" button on logs.

---

## Anti-Pattern 4: Missing DD_LOGS_INJECTION Environment Variable

```text
Mistake: developer adds JSON logging and MDC configuration but forgets:
  DD_LOGS_INJECTION=true   <- this is what makes dd-java-agent inject trace IDs into MDC

Symptom: JSON logs are collected, but dd.trace_id field is always missing.

Debug:
  Add DD_LOGS_INJECTION=true to container environment.
  Restart application.
  Check that dd.trace_id appears in log JSON output.
```

---

## Anti-Pattern 5: Agent Not Allowing Non-Local Traffic In Docker/K8s

```text
Mistake: application in one container, Datadog agent in another container.
         Agent only accepts connections from localhost by default.

Symptom: Java/Node/Python app starts fine but NO traces in Datadog.
         Check app logs: "Connection refused to localhost:8126"

Fix: set in datadog.yaml or docker environment:
  apm_config:
    apm_non_local_traffic: true
  
  dogstatsd_non_local_traffic: true

OR environment variable:
  DD_APM_NON_LOCAL_TRAFFIC=true
  DD_DOGSTATSD_NON_LOCAL_TRAFFIC=true
```

---

## Anti-Pattern 6: Trace Context Not Propagated Across Async Threads

```java
// WRONG - starts a new thread without propagating trace context.
executor.submit(() -> {
    // This runs in a new thread. No trace context here.
    // Any span created here will NOT be linked to the parent trace.
    Span span = tracer.buildSpan("async.work").start();
    doWork();
    span.finish();
});

// CORRECT - capture context in calling thread, activate in new thread.
Scope currentScope = GlobalTracer.get().scopeManager().active();
final Span parentSpan = (currentScope != null) ? currentScope.span() : null;

executor.submit(() -> {
    Tracer.SpanBuilder builder = tracer.buildSpan("async.work");
    if (parentSpan != null) {
        builder = builder.asChildOf(parentSpan);
    }
    Span span = builder.start();
    try (Scope scope = tracer.activateSpan(span)) {
        doWork();
    } finally {
        span.finish();
    }
});
```

Symptom: Async work creates orphan spans not connected to the original trace.

---

## Anti-Pattern 7: Monitors Without Recovery Threshold (Flapping)

```text
Mistake: monitor threshold only has alert condition, no recovery.
  Alert: CPU > 80%
  (no recovery threshold)

What happens:
  CPU oscillates between 78% and 82%:
    80.1% -> ALERT  (notification sent)
    79.8% -> OK     (notification sent)
    80.3% -> ALERT  (notification sent)
    79.6% -> OK     (notification sent)
  
  Team gets 30 alert emails in 1 hour for a minor CPU spike.

Fix:
  Alert:    > 80%
  Recovery: < 65%   (hysteresis gap prevents flapping)
```

---

## Anti-Pattern 8: SLO On Wrong Metric

```text
Mistake: SLO monitors the API gateway HTTP 200 rate, but:
  - API gateway returns 200 for all business errors (wrapped in JSON body)
  - Real errors are in the response body as {error: true, code: "PAYMENT_FAILED"}
  - SLO shows 99.99% availability while users are seeing failures

Correct approach:
  1. Define what "good" means from the user's perspective (successful checkout, not just HTTP 200).
  2. Emit a custom metric: orders.checkout.success vs orders.checkout.total.
  3. Base the SLO on the business success rate, not HTTP protocol success.
```

---

## Anti-Pattern 9: Datadog API Key Leaked In Container Images

```text
Mistake:
  # Dockerfile
  ENV DD_API_KEY=abc123yourapikey    <- hardcoded in image layer

Why dangerous:
  - Docker image layers are inspectable
  - If image is pushed to a registry, key is exposed
  - All past layers preserve the key even if removed in a later layer

Correct approach:
  # Inject at runtime only.
  docker run -e DD_API_KEY=$DD_API_KEY ...

  # Kubernetes: use a Secret.
  kubectl create secret generic datadog-api-key --from-literal=api-key=abc123
  
  # In pod spec:
  env:
    - name: DD_API_KEY
      valueFrom:
        secretKeyRef:
          name: datadog-api-key
          key: api-key
```

---

## Anti-Pattern 10: No Version Tag On Deployments

```text
Mistake: service runs without DD_VERSION environment variable.

Impact:
  - APM deployment tracking disabled: cannot see performance diff between v1.2.0 and v1.2.1
  - Error budget analysis cannot correlate regression to specific version
  - No rollback signal: cannot see "error rate increased after version X deployed"

Fix:
  DD_VERSION=1.2.3   (inject from CI/CD pipeline at deploy time)
  
  Best practice: use Git SHA or semantic version.
  DD_VERSION=$(git rev-parse --short HEAD)
```

---

## Debugging Checklist: Traces Not Appearing

```text
1. Is the agent running?
   datadog-agent status | grep -i apm

2. Is APM enabled on the agent?
   apm_config.enabled: true
   DD_APM_ENABLED=true

3. Is the agent accepting non-local traffic?
   DD_APM_NON_LOCAL_TRAFFIC=true (required for Docker/K8s)

4. Is DD_AGENT_HOST set correctly in the application?
   DD_AGENT_HOST=datadog-agent  (or the service name in docker-compose)

5. Is the tracer library first import?
   (Node.js: dd-trace must be required before express)
   (Python: patch_all() must run before Flask/FastAPI import)

6. Is the service/env/version set?
   DD_SERVICE, DD_ENV, DD_VERSION must all be set.

7. Is sampling set to 1.0 for dev/testing?
   DD_TRACE_SAMPLE_RATE=1.0

8. Check agent logs for trace reception:
   datadog-agent logs | grep -i trace
```

---

## Interview Sound Bite

The most common Datadog pitfalls are: initializing dd-trace after imports (Node.js), forgetting DD_LOGS_INJECTION (Java/Python), high-cardinality metric tags causing cost explosions, missing non-local traffic settings in Docker/K8s, and async context not propagated in threaded code. SLOs built on HTTP status codes miss business-logic failures. Monitors without recovery thresholds cause alert flapping. API keys must never be in image layers — inject at runtime via Kubernetes Secrets.
