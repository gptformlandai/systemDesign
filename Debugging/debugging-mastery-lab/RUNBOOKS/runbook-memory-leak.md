# Runbook: Java Memory Leak Investigation

## When To Use This Runbook

- Heap memory grows without releasing over time
- OOM error: `java.lang.OutOfMemoryError: Java heap space`
- OOM error: `GC overhead limit exceeded`
- Application restarts itself repeatedly (K8s OOMKilled)
- GC logs show frequent full GC with little memory reclaimed

---

## Step 1: Confirm Memory Leak

```bash
# Check JVM heap usage trend.
jstat -gc <PID> 5000 20
# Output: every 5 seconds, 20 samples.
# Columns: S0C S1C S0U S1U EC EU OC OU MC MU ...
# OC = old gen capacity, OU = old gen used.
# Leak: OU keeps growing, never decreasing even after GC.

# Via actuator.
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap
# Run this every 30 seconds and observe the trend.
```

---

## Step 2: Enable Heap Dump On OOM (Before It Happens)

If not already in startup flags:

```bash
# Add to JVM flags and restart:
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/app/

# Kubernetes: add to JAVA_TOOL_OPTIONS env var in deployment.yaml
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/app/"
```

---

## Step 3: Capture Heap Dump Manually

```bash
# While memory is high (but before OOM).
jmap -dump:live,format=b,file=/tmp/heap-$(date +%Y%m%d-%H%M%S).hprof <PID>

# Via actuator (Spring Boot).
curl http://localhost:8080/actuator/heapdump -o /tmp/heap-$(date +%Y%m%d-%H%M%S).hprof
```

---

## Step 4: Analyze Heap Dump In IntelliJ

```text
1. Open IntelliJ → File → Open → select .hprof file.
2. Wait for analysis to complete.
3. Go to Classes view.
4. Sort by "Retained Size" descending.
5. Top objects by retained size = leak suspects.
```

Look for:
- Object count far larger than expected (e.g., 500,000 Order objects)
- `byte[]` with large retained size (often String/cache data)
- Collections growing without bound (ArrayList, HashMap)

---

## Step 5: Trace References To Leak Root

```text
In IntelliJ Heap Dump Viewer:
  Classes view → click the suspicious class (e.g., Order)
  Instances view → click an instance
  References panel → shows what holds a reference to this object
  Walk UP the reference chain to find the GC root holding everything.
```

Common leak roots:

```text
Static Map/List:           static Map<K, Order> cache = new HashMap<>();
ThreadLocal not removed:   ThreadLocal<DBConnection> conn = ...  (pool threads never remove)
Listener not unregistered: eventBus.register(this)  (never eventBus.unregister)
Circular reference via event listeners in inner classes
```

---

## Step 6: Fix

```java
// Fix 1: Static cache with eviction.
// Replace unbounded HashMap with Caffeine/Guava cache.
Cache<String, Order> cache = Caffeine.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build();

// Fix 2: ThreadLocal cleanup.
ThreadLocal<DBConnection> local = new ThreadLocal<>();
try {
    local.set(getConnection());
    // ... use connection
} finally {
    local.remove();  // always clean up in thread pool threads
}

// Fix 3: Unregister listeners.
eventBus.register(this);
// ... later when component is destroyed:
eventBus.unregister(this);
```

---

## Step 7: Verify Fix

```bash
# Run under load for 30 minutes. Monitor heap.
jstat -gc <PID> 10000 180  # every 10 seconds, 30 minutes

# OU (old gen used) should plateau or decrease after GC.
# If OU grows monotonically: leak not fully fixed.
```

---

## Immediate Mitigation

```bash
# If OOM is imminent and you need time to fix:
# 1. Increase heap size temporarily.
-Xmx4g  # increase max heap

# 2. Configure K8s memory limit higher.
resources:
  limits:
    memory: 4Gi

# 3. Add -XX:+ExitOnOutOfMemoryError so K8s restarts the pod cleanly on OOM.
```

---

## Prevention Checklist

- [ ] `-XX:+HeapDumpOnOutOfMemoryError` in all JVM startup configs
- [ ] No unbounded static collections (use Caffeine/Guava with max size)
- [ ] All ThreadLocal usages have `remove()` in finally blocks
- [ ] All event listener registrations have matching unregister
- [ ] Memory usage metrics tracked (JVM heap used, GC count, GC pause time)
- [ ] Alert when heap >80% for >5 minutes
