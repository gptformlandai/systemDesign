# 19. Scenario: Kubernetes OOMKilled and CrashLoopBackOff Debugging

## Scenario Setup

```text
PagerDuty alert at 03:42 UTC:
  Monitor: "Kubernetes Container Restarts > 5 in 5 minutes"
  Value: 12 restarts on namespace:production service:checkout-worker
  Scope: kube_namespace:production kube_deployment:checkout-worker
```

---

## Investigation Workflow

### Step 1: Check Live Containers View

```text
Infrastructure -> Containers

Filter:
  kube_namespace:production
  kube_deployment:checkout-worker
  status:running OR status:terminated

Current state:
  checkout-worker-pod-a1: Running  Started: 03:41:55 (very recent)
  checkout-worker-pod-b2: Running  Started: 03:42:10 (very recent)
  checkout-worker-pod-c3: CrashLoopBackOff
```

Pod c3 is in CrashLoopBackOff — crashing immediately after start.

### Step 2: Check Container Restart Metric

```text
Dashboard or metric query:
  sum:kubernetes.containers.restarts{kube_namespace:production,kube_deployment:checkout-worker} by {pod_name}

Graph shows:
  03:38 - first restart
  03:39 - 2 restarts
  03:40 - 4 restarts (accelerating)
  03:41 - 8 restarts
  03:42 - 12 restarts (alert fires)
  Pod c3: 5 restarts in 10 minutes -> exponential backoff -> CrashLoopBackOff
```

### Step 3: Check Recent Termination Reasons

```text
Metric:
  kubernetes.containers.last_seen_state{kube_namespace:production,kube_deployment:checkout-worker}
  
Filter by reason:
  @reason:OOMKilled -> 9 terminations
  @reason:Error     -> 3 terminations
```

Primary: OOMKilled. Secondary: some startup crashes after OOM.

### Step 4: Check Container Memory Usage

```text
Infrastructure -> Containers -> click pod c3 (even though terminated, last known metrics visible)

container.memory.usage vs container.memory.limit:
  Memory limit:  512MB
  Memory at kill: 511MB (hit the limit exactly)

container.memory.usage trend (last 6 hours):
  21:00 -> 280MB (normal)
  22:00 -> 320MB
  23:00 -> 390MB
  00:00 -> 420MB
  01:00 -> 450MB
  02:00 -> 480MB
  03:38 -> 511MB -> OOMKill (memory grew linearly over 6 hours)
```

Pattern: slow memory growth over 6 hours, not a sudden spike. Classic memory leak.

### Step 5: Correlate With Application Logs

```text
Log Explorer:
  service:checkout-worker env:production
  time: last 8 hours

Look for:
  - Memory warnings before the kill
  - Any OutOfMemoryError in Java
  - Changes in processing volume

Found at 03:37:
  WARN: "JVM heap: 480MB/512MB - GC overhead limit approaching"
  
Found at 03:38:
  ERROR: "java.lang.OutOfMemoryError: GC overhead limit exceeded"
  ERROR: "java.lang.OutOfMemoryError: Java heap space"
```

### Step 6: Find What Changed In APM

```text
APM -> Services -> checkout-worker -> Deployments (version tracking)

Deployment history:
  2.5.0 deployed at 21:00 UTC (same time memory growth started)
  2.4.2 was stable before

Memory was normal before 21:00.
2.5.0 was deployed at 21:00 (6.5 hours of memory growth = memory leak in new version).
```

### Step 7: Check For Memory Leak In Traces

```text
APM -> Trace Explorer

Filter:
  service:checkout-worker env:production version:2.5.0
  resource_name:"WorkerJob.processCheckout"

Span tags on recent traces:
  @items_processed: 1500 per job
  
Old version (2.4.2) traces:
  @items_processed: 1500 per job
  duration: 2.1s

Pattern analysis:
  2.5.0 spans show growing duration over time (3.1s -> 4.2s -> 5.8s)
  This suggests object accumulation in memory not being garbage collected.
```

### Step 8: Immediate Remediation

```text
1. Roll back deployment to 2.4.2:
   kubectl rollout undo deployment/checkout-worker -n production

2. Verify rollback:
   kubectl rollout status deployment/checkout-worker -n production

3. Monitor memory stabilizes:
   container.memory.usage{kube_deployment:checkout-worker} -> drops to 280MB after rollback
```

### Step 9: Root Cause Analysis For 2.5.0

```text
Developer investigation found:
  - New feature added a static ConcurrentHashMap cache in version 2.5.0
  - Cache had no eviction policy
  - Accumulated checkout session objects indefinitely
  - One entry per checkout attempt: after 6 hours of normal traffic, cache grew to ~400MB
  
Fix in 2.5.1:
  - Add Guava Cache with maximum size limit and TTL expiry
  - Add JVM memory metric to deployment runbook checks
```

---

## CrashLoopBackOff Debugging Checklist

```text
When a pod is in CrashLoopBackOff:

1. Check what the restart reason is:
   OOMKilled: memory issue
   Error (exit code != 0): application crash
   ContainerCannotRun: startup failure (bad config, missing files)

2. Check container logs from the LAST run (not current):
   Live Containers -> pod -> "Previous" logs tab

3. Check if OOMKilled:
   container.memory.usage trend -> growing? sudden spike? at limit?

4. Check if recent deployment:
   APM Deployments view -> version change correlated with start of restarts?

5. Check for config issues:
   Missing env vars, failed readiness probe, bad startup command
```

---

## Key Metrics For K8s Memory Monitoring

```text
container.memory.usage             - current RSS + cache
container.memory.working_set       - active memory (more accurate)
container.memory.limit             - configured limit
container.memory.rss               - heap memory
kubernetes.containers.restarts     - restart count
kubernetes.containers.last_seen_state{reason:OOMKilled}
```

---

## Interview Sound Bite

For OOMKilled and CrashLoopBackOff debugging, start with Live Containers to identify which pods are crashing. Check the restart count metric grouped by pod. Look at container.memory.usage trend — a steady linear growth over hours indicates a memory leak; a sudden spike indicates a traffic burst or large payload. Correlate the memory growth start time with deployment history in APM (version tracking). Check application logs for OutOfMemoryError. Roll back the problematic version as the immediate fix while the memory leak is fixed in code.
