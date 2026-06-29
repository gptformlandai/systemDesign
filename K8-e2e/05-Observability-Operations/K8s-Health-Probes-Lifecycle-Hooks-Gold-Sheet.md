# Kubernetes Health Probes and Pod Lifecycle Hooks Gold Sheet

> Track: K8s Interview Track — Phase 5: Observability and Operations
> Goal: Configure probes and lifecycle hooks so pods start correctly, handle traffic correctly, and terminate gracefully — preventing the most common K8s production incidents.

---

## 0. How To Read This

Beginner focus:
- Liveness probe: restart unhealthy containers
- Readiness probe: remove from load balancer until ready
- HTTP, TCP, and exec probe types

Intermediate focus:
- Startup probe: slow-starting applications
- Probe tuning: initialDelaySeconds, periodSeconds, failureThreshold
- PreStop hook: graceful shutdown
- PostStart hook: initialization after start

Senior / MAANG focus:
- Probe failure patterns: false positives under load
- Graceful shutdown: SIGTERM + preStop + terminationGracePeriodSeconds
- Zero-downtime rolling updates require both readiness + preStop
- Connection draining patterns
- JVM/Node.js specific startup and readiness considerations

---

# Topic 1: Three Probe Types

## 1. Liveness Probe

```text
Purpose: Is this container alive? If not, kill and restart it.

Use case: deadlock, stuck goroutines, memory leak causing loop.
The process is running but not actually doing work.

If liveness fails failureThreshold times:
  → Container is killed (SIGTERM → SIGKILL)
  → Restarted (based on restartPolicy)

WARNING: Misconfigured liveness probe = cascading restarts under load.
Under high load, app may be slow → probe times out → container killed
→ more load on other pods → they also restart → cascade failure.
```

## 2. Readiness Probe

```text
Purpose: Is this container ready to receive traffic?

If readiness fails:
  → Pod IP removed from Service EndpointSlice
  → No new traffic routed to this pod
  → Pod NOT killed (stays running, just not receiving traffic)

Use case: app is still starting up (cache warming, DB connection establishing).
Also: feature flags, circuit breaker opened, overloaded.
```

## 3. Startup Probe

```text
Purpose: Handle slow-starting containers without killing them.

If startup probe fails failureThreshold times:
  → Container is killed (same as liveness failure)

While startup probe is running:
  → Liveness and readiness probes are DISABLED
  → Container gets startup grace period

Use case: Java apps, apps that do schema migrations at startup.
```

## 4. When To Use Which

```text
All three can coexist:

Startup probe: active from time 0 until success (then disabled)
  failureThreshold * periodSeconds = max startup time
  e.g. failureThreshold=30, periodSeconds=10 → up to 5 minutes to start

After startup probe succeeds:
  Liveness probe: active continuously (restart on failure)
  Readiness probe: active continuously (remove from LB on failure)
```

---

# Topic 2: Probe Configuration

## 1. HTTP Probe (Most Common)

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
    httpHeaders:
      - name: X-Health-Check
        value: kubelet
    scheme: HTTP         # or HTTPS
  initialDelaySeconds: 30   # wait 30s before first probe (after container start)
  periodSeconds: 10         # probe every 10s
  timeoutSeconds: 5         # probe times out after 5s
  successThreshold: 1       # must succeed 1 time to be "healthy" (always 1 for liveness)
  failureThreshold: 3       # fail 3 times in a row before action taken

readinessProbe:
  httpGet:
    path: /ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
  timeoutSeconds: 3
  successThreshold: 1       # must succeed 1 time to be "ready"
  failureThreshold: 3       # fail 3 times to be "not ready"

startupProbe:
  httpGet:
    path: /health
    port: 8080
  failureThreshold: 30      # 30 * 10s = 300s max startup time
  periodSeconds: 10
```

## 2. TCP Socket Probe

```yaml
livenessProbe:
  tcpSocket:
    port: 5432    # just checks if TCP connection can be established
  initialDelaySeconds: 15
  periodSeconds: 20
```

Use for: databases, message brokers where HTTP isn't available.

## 3. gRPC Probe (K8s 1.24+)

```yaml
livenessProbe:
  grpc:
    port: 50051
    service: liveness    # gRPC health.v1 service name
  periodSeconds: 10
```

Requires app to implement gRPC Health Checking Protocol.

## 4. Exec Probe (Shell Command)

```yaml
livenessProbe:
  exec:
    command:
      - /bin/sh
      - -c
      - "redis-cli ping | grep PONG"
  periodSeconds: 10
```

Use sparingly — exec probes spawn a process, adding overhead. Avoid in high-pod-count deployments.

---

# Topic 3: Probe Health Endpoints (Application Side)

## 1. Spring Boot

```yaml
# application.properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

management.endpoint.health.probes.enabled=true
# Exposes:
#   /actuator/health/liveness   (for liveness probe)
#   /actuator/health/readiness  (for readiness probe)
```

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```

## 2. Node.js (Express)

```javascript
// Separate endpoints
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'UP' });
});

app.get('/ready', async (req, res) => {
  try {
    await db.query('SELECT 1');     // check DB connection
    await redis.ping();              // check cache connection
    res.status(200).json({ status: 'READY' });
  } catch (err) {
    res.status(503).json({ status: 'NOT READY', error: err.message });
  }
});
```

## 3. What NOT To Do in Health Endpoints

```text
❌ Check external dependencies in LIVENESS probe
   If downstream service is down, all pods restart (not your fault, your problem)

✅ Check external dependencies in READINESS probe
   If downstream is down, pod stops receiving traffic (graceful degradation)

✅ Liveness checks only local state (is the process functioning?)
   Can I acquire a lock? Can I process a simple request?

Common readiness checks:
  - DB connection pool has available connections
  - Cache is connected
  - Required feature flags loaded
  - Warm-up complete (ML model loaded, cache pre-warmed)
```

---

# Topic 4: Pod Lifecycle Hooks

## 1. PostStart Hook

Runs immediately after the container starts (concurrent with container start):

```yaml
lifecycle:
  postStart:
    exec:
      command: ["/bin/sh", "-c", "echo 'container started' >> /tmp/startup.log"]
```

```text
WARNING: PostStart runs concurrently with container ENTRYPOINT.
There's no guarantee which runs first.
Container won't reach Running state until PostStart completes.
If PostStart fails: container is killed and restarted.
```

Use for: initialization that must run after container starts (though init containers are usually better).

## 2. PreStop Hook

Runs before container receives SIGTERM:

```yaml
lifecycle:
  preStop:
    exec:
      command: ["/bin/sh", "-c", "sleep 5"]
    # OR:
    httpGet:
      path: /shutdown
      port: 8080
```

```text
Sequence for graceful shutdown:
  1. Pod deletion triggered (kubectl delete / rolling update)
  2. Pod removed from EndpointSlice (traffic stops flowing to this pod)
     NOTE: kube-proxy/iptables update propagates — may take a few seconds
  3. PreStop hook runs
  4. SIGTERM sent to container
  5. Container has terminationGracePeriodSeconds to shut down
  6. If not stopped: SIGKILL sent
```

---

# Topic 5: Zero-Downtime Shutdown Pattern

## 1. The Race Condition Problem

```text
Problem: iptables/ipvs rules take time to propagate after pod is removed from Endpoints.
During this window (typically 1-5 seconds):
  - Load balancer still sends traffic to the terminating pod
  - Pod receives SIGTERM and starts shutting down
  - Active connections see connection refused or reset errors

Result: 502/503 errors during rolling updates.
```

## 2. Solution: PreStop Sleep

```yaml
lifecycle:
  preStop:
    exec:
      command: ["/bin/sh", "-c", "sleep 15"]
```

```text
Sequence with preStop sleep:
  1. Pod removed from Endpoints
  2. PreStop hook starts: sleep 15s
     → During sleep, iptables rules propagate across all nodes
     → No new traffic routed to this pod
  3. After 15s: SIGTERM sent
  4. App handles SIGTERM: closes connections gracefully
  5. App exits cleanly

SIGTERM handler in app:
  - Stop accepting new connections
  - Complete in-flight requests
  - Close DB connection pool
  - Flush metrics/logs
  - Exit with code 0
```

## 3. terminationGracePeriodSeconds

```yaml
spec:
  terminationGracePeriodSeconds: 60    # default is 30s
```

```text
Timer starts when pod deletion is triggered.
Both preStop AND container shutdown must complete within this window.
If exceeded: SIGKILL is sent (immediate, no cleanup).

Formula: terminationGracePeriodSeconds > preStop duration + max request duration
Example:
  preStop sleep: 15s
  Max long-running request: 30s
  Set terminationGracePeriodSeconds: 60 (15 + 30 + 15 buffer)
```

## 4. Full Production-Ready Probe + Lifecycle Config

```yaml
spec:
  terminationGracePeriodSeconds: 60
  containers:
    - name: payment-service
      image: payment-service:v1.2.3
      
      startupProbe:
        httpGet:
          path: /health
          port: 8080
        failureThreshold: 30     # 30 * 10 = 5 min max startup
        periodSeconds: 10
      
      livenessProbe:
        httpGet:
          path: /health
          port: 8080
        initialDelaySeconds: 0    # startup probe handles delay
        periodSeconds: 10
        timeoutSeconds: 3
        failureThreshold: 3
      
      readinessProbe:
        httpGet:
          path: /ready
          port: 8080
        initialDelaySeconds: 0
        periodSeconds: 5
        timeoutSeconds: 3
        successThreshold: 1
        failureThreshold: 3
      
      lifecycle:
        preStop:
          exec:
            command: ["/bin/sh", "-c", "sleep 15"]
```

---

# Topic 6: Common Probe Mistakes

| Mistake | Problem | Fix |
|---|---|---|
| Same endpoint for liveness and readiness | Liveness kills pod when readiness should just remove from LB | Separate `/health` (liveness) and `/ready` (readiness) |
| External dependency in liveness probe | Downstream failure causes mass pod restarts | External checks belong in readiness only |
| `initialDelaySeconds` too short | Probe fires before app is ready → false liveness failure → restart loop | Use startup probe instead |
| `initialDelaySeconds` too long | Real crashes not caught quickly | Use startup probe for slow starts, low delay for liveness |
| No preStop hook | Traffic hits terminating pod during routing table update | Add `preStop: sleep 15` |
| `terminationGracePeriodSeconds` shorter than preStop | SIGKILL interrupts preStop | Ensure terminationGracePeriodSeconds > preStop + request timeout |
| Exec probe for high-pod-count workloads | Each probe forks a process; overhead adds up | Prefer HTTP probes |

---

# Topic 7: Revision Notes

- Liveness: restart failed containers; check local process health only
- Readiness: remove from Service load balancer; check if ready to serve traffic (including deps)
- Startup: grace period for slow-starting apps; disables liveness/readiness until success
- `failureThreshold * periodSeconds` = effective timeout for startup probe
- PreStop: runs before SIGTERM; use `sleep 15` to let routing tables propagate
- terminationGracePeriodSeconds: must be > preStop duration + max request duration
- Zero-downtime rolling update requires: readiness probe (new pods ready before old terminate) + preStop (old pods drain gracefully)

## Official Source Notes

- Probes: <https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/>
- Pod lifecycle: <https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/>
- Container hooks: <https://kubernetes.io/docs/concepts/containers/container-lifecycle-hooks/>
- gRPC health: <https://grpc.io/docs/guides/health-checking/>
