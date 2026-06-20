# Microservices Kubernetes Service Mesh Platform Readiness Platinum Sheet

Target: backend engineers who need to explain how microservices actually run in production.

This sheet connects microservices design to Kubernetes, deployment safety, probes, service
discovery, autoscaling, configuration, secrets, service mesh, and platform ownership.

---

## 0. Why This Matters

Many candidates can draw services on a whiteboard. Senior candidates can explain how those
services are deployed, routed, scaled, secured, observed, and recovered.

Core idea:

```text
Microservice design is incomplete until runtime behavior is clear.
```

---

# 1. Runtime Building Blocks

| Concept | What It Does |
|---|---|
| Container | packages app and dependencies |
| Pod | smallest Kubernetes runtime unit |
| Deployment | manages desired replica count and rollout |
| Service | stable network identity for pods |
| Ingress/Gateway | exposes HTTP traffic from outside cluster |
| ConfigMap | non-secret configuration |
| Secret | sensitive configuration |
| Probe | health signal for lifecycle decisions |
| HPA | scales replicas based on metrics |

Strong answer:

```text
In production, a microservice is not just code. It has deployment, service discovery,
configuration, secrets, probes, autoscaling, logs, metrics, traces, and rollback strategy.
```

---

# 2. Pod And Deployment Mental Model

```text
Deployment -> ReplicaSet -> Pods -> Containers
```

Deployment owns rollout strategy:

- rolling update
- rollback
- replica count
- pod template

Interview point:

```text
I do not route traffic directly to pods because pods are ephemeral. I route to a Kubernetes
Service, which selects healthy pods through labels.
```

---

# 3. Service Discovery

Problem:

```text
Pods come and go. Their IP addresses change.
```

Solution:

```text
Kubernetes Service gives a stable DNS name and load balances to matching pods.
```

Example:

```text
booking-service.default.svc.cluster.local
```

In interviews:

```text
Inside Kubernetes, basic service discovery is usually DNS plus Kubernetes Service. Extra
client-side discovery is not always necessary.
```

---

# 4. Liveness, Readiness, Startup

| Probe | Meaning | Wrong Use |
|---|---|---|
| Startup | app is still starting | using liveness for slow startup |
| Readiness | app can receive traffic | marking ready before DB/client warmup |
| Liveness | app is stuck and should restart | failing liveness for temporary DB outage |

Bad liveness:

```text
/health checks database. DB blip causes every pod to restart.
```

Better:

```text
Liveness checks app process health.
Readiness checks ability to serve traffic.
```

Strong answer:

```text
Readiness removes the pod from traffic. Liveness restarts it. Mixing them can turn a
dependency outage into a restart storm.
```

---

# 5. Graceful Shutdown

A pod may be terminated during rollout, scaling, or node maintenance.

Good shutdown flow:

```text
1. Pod receives termination signal.
2. Readiness becomes false.
3. Load balancer stops sending new requests.
4. App finishes in-flight requests.
5. App closes resources and exits.
```

Spring Boot mapping:

- enable graceful shutdown
- configure request timeout
- stop consuming messages before closing dependencies
- keep termination grace period larger than normal request duration

---

# 6. Resource Requests And Limits

| Setting | Meaning |
|---|---|
| CPU request | scheduling guarantee |
| CPU limit | max CPU usage before throttling |
| memory request | scheduling guarantee |
| memory limit | max memory before OOM kill |

Common issue:

```text
CPU limit too low -> throttling -> p99 latency spike even when average CPU looks okay.
```

Senior answer:

```text
I size requests and limits from real load tests and production metrics, not guesses.
```

---

# 7. Autoscaling

Horizontal Pod Autoscaler can scale replicas based on CPU, memory, or custom metrics.

Good scaling metrics:

- CPU for CPU-bound service
- request rate per pod
- queue length
- Kafka consumer lag
- p95 latency with care

Bad scaling metric:

```text
Scale only on CPU for an I/O-bound service blocked on database connections.
```

Autoscaling checklist:

- app is stateless or state is externalized
- startup time is known
- DB/downstream can handle more clients
- connection pools are sized per replica
- rate limits protect dependencies

---

# 8. Configuration And Secrets

Config should vary by environment without rebuilding the image.

Examples:

- database host
- queue topic
- feature flags
- timeouts
- pool sizes

Secrets:

- database password
- API key
- private key
- OAuth client secret

Platinum rule:

```text
Config changes can cause incidents. Treat high-risk config like code: review, version,
rollout gradually, and audit.
```

---

# 9. Deployment Strategies

| Strategy | How It Works | Use When |
|---|---|---|
| Rolling | gradually replaces pods | common low-risk releases |
| Blue-green | switch all traffic to new environment | fast rollback needed |
| Canary | send small traffic to new version | user impact must be minimized |
| Feature flag | code deployed but behavior controlled | product/risk control |

Strong answer:

```text
For risky changes, I use canary plus SLO-based automated rollback. For schema changes, I
use expand-contract migration so old and new versions run together safely.
```

---

# 10. Service Mesh

A service mesh moves common network concerns into sidecars or ambient infrastructure.

It can provide:

- mTLS
- traffic splitting
- retries/timeouts
- circuit breaking
- telemetry
- policy enforcement

When useful:

- many services
- many languages
- consistent mTLS/policy needed
- traffic shifting and telemetry must be platform-wide

When not useful:

- small system
- team cannot operate mesh complexity
- app-level logic is still unclear
- latency/operational cost is not justified

Strong answer:

```text
A service mesh helps standardize cross-cutting network behavior, but it does not fix bad
service boundaries, missing idempotency, or unclear ownership.
```

---

# 11. Sidecar Pattern

Sidecar means an auxiliary container runs alongside the app container in the same pod.

Examples:

- proxy sidecar
- log shipper
- security agent
- config reloader

Trade-off:

- standardizes capabilities
- increases resource usage and debugging complexity

---

# 12. Platform Readiness Checklist

Every production microservice should define:

| Area | Required |
|---|---|
| Runtime | container image, health probes, graceful shutdown |
| Scaling | requests/limits, HPA metric, load test numbers |
| Config | environment config, secret source, rotation plan |
| Networking | service, gateway route, timeout, retry policy |
| Observability | logs, metrics, traces, dashboards |
| Reliability | rollback, canary, circuit breaker, rate limit |
| Data | migration plan, backup/restore, data ownership |
| Security | service identity, least privilege, mTLS/auth |
| Operations | runbook, owner, SLO, alerts |

---

# 13. Common Kubernetes Failure Scenarios

| Symptom | Likely Cause | Check |
|---|---|---|
| CrashLoopBackOff | app startup failure | logs, env, secrets, dependencies |
| Ready 0/3 | readiness failing | readiness endpoint, dependencies |
| high p99 latency | CPU throttle or downstream wait | CPU throttling, traces, pools |
| OOMKilled | memory limit exceeded | heap, native memory, leaks |
| rollout stuck | new pods not ready | events, readiness, image pull |
| only one version failing | bad deploy/config | compare metrics by version |
| DNS failures | service/core DNS/network issue | service endpoints, DNS logs |

---

# 14. Backend Interview Scenario

> Your Spring Boot service works locally, but in Kubernetes it has high latency and restarts
> during database incidents. How do you fix it?

Strong answer:

```text
I would first inspect pod events, restarts, CPU throttling, memory kills, and readiness/
liveness behavior. If liveness checks the database, I would change it because temporary DB
failure should remove pods from traffic through readiness, not restart the whole fleet. I
would validate CPU/memory requests and limits, check connection pool sizing per replica, and
look at traces to identify queueing or downstream wait. Then I would configure graceful
shutdown, readiness warmup, bounded timeouts, and dashboards for p99, pool wait, restarts,
and throttle rate.
```

---

# 15. Platform vs Application Responsibility

| Concern | Platform Usually Owns | App Usually Owns |
|---|---|---|
| cluster | yes | no |
| base deployment templates | yes | uses |
| business metrics | no | yes |
| domain timeouts | shared | yes |
| service SLO | shared | yes |
| runbook | shared | yes |
| secret backend | yes | consumes safely |
| idempotency | no | yes |

Memory:

```text
Platform gives paved road. Service team still owns correctness.
```

---

# 16. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Liveness checks DB | liveness checks process, readiness checks dependencies |
| No resource requests | size from load tests |
| Autoscale without DB capacity | scale app and dependency budgets together |
| No graceful shutdown | drain traffic and finish in-flight requests |
| Mesh too early | start with app timeouts/observability first |
| Config changed manually | versioned/audited rollout |
| No per-version dashboards | compare canary and stable |

---

# 17. Final Rapid Revision

```text
Pods are ephemeral. Services provide stable identity.
Readiness removes traffic. Liveness restarts.
Startup probe protects slow boot.
Requests affect scheduling. Limits affect throttling/OOM.
Autoscaling must respect downstream capacity.
Canary plus SLO rollback protects users.
Service mesh standardizes network/security, but adds complexity.
Platform readiness means runtime, scaling, config, networking, observability, reliability,
data, security, and ownership are all defined.
```

---

# 18. Official Source Notes

- Kubernetes Services: https://kubernetes.io/docs/concepts/services-networking/service/
- Kubernetes pod lifecycle and probes: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/
- Spring Boot Actuator endpoints: https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
