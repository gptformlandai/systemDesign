# JVM Runtime Tuning: Heap, GC, Threads, and Diagnostics — Gold Sheet

> Topic: JVM memory model, GC selection, container-aware flags, thread pools, and diagnostic tools

---

## 1. Intuition

The JVM is not just a Java runtime — it is a managed runtime with its own memory model, garbage collector, and thread scheduler. Getting it wrong in production means OutOfMemoryErrors, GC pauses that cause timeouts, or containers being OOM-killed by Kubernetes. Getting it right means predictable latency and stable throughput.

Beginner version:

> JVM tuning controls how much memory the app uses and how it cleans up unused objects.

---

## 2. Definition

- Definition: JVM tuning is the process of configuring heap size, garbage collection strategy, thread counts, and runtime flags to match an application's workload and container resource limits.
- Category: Runtime performance engineering.
- Core idea: Untuned JVM in a container = unpredictable OOM kills and GC pauses.

---

## 3. JVM Memory Model

```
JVM Memory Layout:
┌─────────────────────────────────────────────────┐
│  Heap                                           │
│  ┌───────────────────┐  ┌────────────────────┐  │
│  │  Young Generation │  │  Old Generation    │  │
│  │  ┌────┐ ┌───────┐ │  │  (Tenured)         │  │
│  │  │Eden│ │S0 / S1│ │  │                    │  │
│  │  └────┘ └───────┘ │  └────────────────────┘  │
│  └───────────────────┘                          │
├─────────────────────────────────────────────────┤
│  Metaspace (class metadata — not heap)          │
├─────────────────────────────────────────────────┤
│  Thread stacks  (each thread ~1MB by default)   │
├─────────────────────────────────────────────────┤
│  Direct memory / NIO buffers                    │
└─────────────────────────────────────────────────┘
```

- **Young Gen**: where new objects are created. Minor GC is fast and frequent.
- **Old Gen**: long-lived objects promoted from Young Gen. Major GC is slow.
- **Metaspace**: class definitions — can grow unboundedly without limit.
- **Total JVM memory**: Heap + Metaspace + Thread stacks + Direct memory.

---

## 4. Heap Configuration

**Traditional (absolute) heap flags:**

```bash
java -Xms512m -Xmx2g -jar app.jar
# -Xms: initial heap size (set equal to -Xmx to avoid resizing)
# -Xmx: maximum heap size
```

**Container-aware flags (preferred in Kubernetes/Docker):**

```bash
java -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -jar app.jar
# MaxRAMPercentage: cap heap at 75% of container memory limit
# JVM reads container cgroup limits, not host total RAM
```

**Why container-aware matters:**
```
Container memory limit: 1GB
Without flag: JVM sees 16GB host RAM → sets Xmx to 4GB → OOM killed immediately
With MaxRAMPercentage=75: JVM sets Xmx to 768MB → fits within container
```

**Recommended pattern for Kubernetes:**
```yaml
# deployment.yaml
resources:
  requests:
    memory: "1Gi"
  limits:
    memory: "1Gi"          # request == limit = predictable memory

env:
  - name: JAVA_OPTS
    value: "-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
```

---

## 5. Garbage Collector Selection

| GC | Flag | Best For | JDK Version |
|---|---|---|---|
| G1GC | `-XX:+UseG1GC` | General purpose, balanced throughput/latency | JDK 9+ default |
| ZGC | `-XX:+UseZGC` | Ultra-low latency (<10ms pauses), large heaps | JDK 15+ production |
| Shenandoah | `-XX:+UseShenandoahGC` | Ultra-low latency, GraalVM/OpenJDK | JDK 15+ |
| Serial | `-XX:+UseSerialGC` | Small single-threaded apps, CLI tools | All versions |
| Parallel | `-XX:+UseParallelGC` | Max throughput, batch processing | JDK 8 default |

**G1GC (default for Spring Boot apps):**
```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \      # target max pause (not a guarantee)
     -XX:G1HeapRegionSize=16m \
     -XX:MaxRAMPercentage=75 \
     -jar app.jar
```

**ZGC for low-latency APIs:**
```bash
java -XX:+UseZGC \
     -XX:SoftMaxHeapSize=1g \        # ZGC soft limit (helps avoid memory hogging)
     -XX:MaxRAMPercentage=75 \
     -jar app.jar
```

---

## 6. Thread Pool Sizing

**Tomcat (Spring Boot default HTTP server):**
```yaml
# application.yml
server:
  tomcat:
    threads:
      min-spare: 10
      max: 200          # default 200; each thread uses ~1MB stack
    accept-count: 100   # queue depth when threads are exhausted
    connection-timeout: 20000
```

**Rule of thumb:**
- IO-bound service (DB queries, external calls): threads = 2-4× CPU cores
- CPU-bound service: threads = CPU cores + 1
- Each Tomcat thread uses ~1MB stack → 200 threads = 200MB just for stacks

**WebFlux (reactive, Netty):**
```yaml
# Reactive apps use very few threads — event loop model
# Default: CPU cores × 2 event loop threads
spring:
  netty:
    idle-timeout: 20s
```

---

## 7. JVM Diagnostic Tools

**Check what's running:**
```bash
jps -l                    # list JVM processes with PID
jinfo -flags <pid>        # show all active JVM flags
jstat -gc <pid> 1s        # GC statistics every 1 second
```

**Thread dump (diagnose deadlocks, thread starvation):**
```bash
jstack <pid>              # print all threads + stack traces
# or in container:
kubectl exec -it pod -- jstack 1

# Look for:
# "BLOCKED" threads → lock contention
# Many threads waiting on same monitor → bottleneck
# "WAITING (on object monitor)" → idle thread pool threads (normal)
```

**Heap dump (diagnose OOM, memory leaks):**
```bash
jmap -dump:format=b,file=heap.hprof <pid>
# or add flag to auto-dump on OOM:
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/dumps/heap.hprof \
     -jar app.jar

# Analyze with Eclipse Memory Analyzer (MAT) or IntelliJ heap profiler
```

**GC log (diagnose pause times, allocation pressure):**
```bash
java -Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=20m \
     -jar app.jar
```

---

## 8. OutOfMemoryError Patterns

| OOM Type | Message | Cause |
|---|---|---|
| Heap OOM | `Java heap space` | Too many live objects, heap too small, memory leak |
| Metaspace OOM | `Metaspace` | Too many classes loaded (dynamic proxy, code generation) |
| Direct buffer | `Direct buffer memory` | NIO/Netty direct memory exceeds limit |
| Thread OOM | `unable to create new native thread` | Too many threads, OS limit reached |

**Common causes in Spring Boot:**
```
Heap OOM:
  → Unbounded caches (HashMap growing forever)
  → Hibernate N+1 queries loading huge result sets
  → File uploads held in memory
  → Leaking sessions in stateful apps

Metaspace OOM:
  → Too many dynamic proxies (Hibernate, Spring AOP)
  → Hot reloading in dev (old class loaders not GC'd)
  → Missing -XX:MaxMetaspaceSize limit
```

---

## 9. `-XX:+ExitOnOutOfMemoryError`

```bash
java -XX:+ExitOnOutOfMemoryError -jar app.jar
```

**Why this flag matters in containers:**
Without it: after OOM, the JVM may limp along in a degraded state, serving errors. Kubernetes doesn't restart it because the process is still alive.
With it: the JVM exits immediately on OOM → Kubernetes restarts the pod → fresh clean state.

Combined best practice:
```bash
java \
  -XX:MaxRAMPercentage=75 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/dumps/ \
  -Xlog:gc*:file=/logs/gc.log:time:filecount=3,filesize=10m \
  -jar app.jar
```

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Using `-Xmx` with absolute value in containers | JVM ignores container limits → OOM kill | Use `-XX:MaxRAMPercentage` |
| Not setting `-XX:+ExitOnOutOfMemoryError` | JVM stays alive but degraded after OOM | Add the flag; let K8s restart |
| 200 Tomcat threads × many replicas | Thread stack memory exhaustion | Tune threads per replica; consider WebFlux for high concurrency |
| No GC logging in production | GC pauses invisible during incident | Always enable GC log with rotation |
| Ignoring Metaspace growth | Eventual Metaspace OOM | Set `-XX:MaxMetaspaceSize=256m` |

---

## 11. Interview Insight

Strong answer:

> In containers I always use `-XX:MaxRAMPercentage=75` instead of absolute `-Xmx` — the JVM reads container cgroup limits and allocates 75% of the container's memory limit as heap. For GC, I default to G1GC for Spring Boot APIs but switch to ZGC when I need sub-10ms GC pauses for latency-sensitive services. I also add `-XX:+ExitOnOutOfMemoryError` so Kubernetes can detect and restart a JVM that hit OOM rather than letting it limp in degraded state. For diagnostics, `jstack` gives thread dumps for deadlock/starvation analysis and `jmap` with `-XX:+HeapDumpOnOutOfMemoryError` gives heap snapshots for memory leak analysis.

Follow-up trap:

> Your API is getting 99th-percentile latency spikes of 2-3 seconds every few minutes. How do you diagnose?

Good answer:

> First check GC logs for Major GC pause times — 2-3 second pauses are classic Full GC symptoms. Check heap utilization (`jstat -gc`) to see if Old Gen is near capacity. If the Old Gen is constantly near-full, objects are living too long — likely unbounded caches or Hibernate entity retention. Fix: increase heap (if headroom is available), tune G1GC's region size, investigate object retention with a heap dump in MAT. If GC looks fine, check for thread contention with jstack.

---

## 12. Revision Notes

- One-line summary: JVM tuning in containers requires percentage-based heap, correct GC selection, and `ExitOnOutOfMemoryError` for clean Kubernetes restarts.
- Three keywords: MaxRAMPercentage, G1GC, ExitOnOOM.
- One interview trap: `-Xmx` is dangerous in containers — JVM may see host RAM, not container limit.
- Memory trick: Containers need a percentage lease on RAM, not an absolute claim.
