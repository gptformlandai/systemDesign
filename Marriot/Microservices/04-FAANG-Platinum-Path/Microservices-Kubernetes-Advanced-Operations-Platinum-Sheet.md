# Microservices Kubernetes Advanced Operations Platinum Sheet

> Track: Microservices Interview Track - Group 4 FAANG Platinum Path  
> Goal: deepen Kubernetes runtime, rollout, scaling, and failure-mode reasoning for microservices interviews.

Read after the Kubernetes service mesh platform readiness sheet.

---

## 1. Platinum Runtime Mental Model

A microservice is production-ready only when its Kubernetes behavior is safe under:

- rollout
- restart
- node drain
- traffic spike
- dependency outage
- CPU throttling
- memory pressure
- bad config
- secret rotation
- regional or zone failure

Strong answer:

```text
Kubernetes runs the service, but it does not automatically make the service reliable. The
application still needs correct probes, graceful shutdown, resource sizing, autoscaling,
observability, and rollback behavior.
```

---

## 2. Requests, Limits, And Scheduling

Requests influence scheduling.
Limits enforce maximum usage.

| Setting | Meaning | Risk If Wrong |
|---|---|---|
| CPU request | guaranteed scheduling share | pod may not get enough CPU |
| CPU limit | max CPU before throttling | high latency from throttling |
| memory request | scheduling reservation | noisy-neighbor pressure |
| memory limit | OOM kill threshold | restarts if too low |

Senior answer:

```text
CPU limit issues often appear as latency spikes, not obvious CPU errors. Memory limit issues
appear as OOMKilled restarts.
```

---

## 3. CPU Throttling Scenario

Symptom:

```text
Booking Service p99 latency spikes during peak traffic, but average CPU looks acceptable.
```

Debug:

1. Check container CPU throttling metrics.
2. Compare CPU usage to limits.
3. Check p99 latency and thread pool queueing.
4. Check HPA scale-out timing.
5. Remove too-strict CPU limit or raise appropriately after testing.

Strong answer:

```text
CPU throttling can create tail latency even when average CPU dashboards look fine. I check
throttling metrics and p99, not only utilization.
```

---

## 4. Memory Pressure And OOM

Signals:

- pod restart count increasing
- termination reason OOMKilled
- heap usage near limit
- GC pressure
- node memory pressure events

Actions:

- inspect memory usage trend
- check recent deploy for leak or larger cache
- tune heap/container memory relationship
- reduce cache size or batch size
- right-size memory requests/limits
- add memory profiling for repeat issues

Strong answer:

```text
If a pod is OOMKilled, Kubernetes restarts it but does not fix the cause. I investigate heap,
cache, batch size, traffic pattern, and memory limit sizing.
```

---

## 5. Probes Deep Dive

Probe rules:

| Probe | Should Check | Should Not Check |
|---|---|---|
| Startup | app boot complete | every dependency forever |
| Readiness | can safely receive traffic | temporary non-critical dependency blips |
| Liveness | process is stuck/deadlocked | downstream DB availability |

Bad liveness:

```text
Liveness checks database. DB blip causes all pods to restart, making incident worse.
```

Strong answer:

```text
Readiness controls traffic. Liveness controls restarts. Mixing them can turn a dependency
problem into a restart storm.
```

---

## 6. Graceful Shutdown Advanced Flow

Kubernetes termination flow:

```text
1. Pod receives SIGTERM.
2. App marks readiness false.
3. Endpoint removal propagates.
4. App stops accepting new work.
5. App finishes in-flight HTTP requests.
6. App pauses/stops message consumption.
7. App closes DB/broker connections.
8. Process exits before terminationGracePeriodSeconds.
```

Message consumer caution:

```text
A Kafka consumer should stop polling and finish or safely abandon in-flight records before
shutdown, otherwise duplicates or long rebalances can occur.
```

---

## 7. Rolling Updates And Availability

Deployment controls:

```text
maxUnavailable: how many pods can be unavailable during rollout
maxSurge: how many extra pods can be created during rollout
```

Risk:

```text
If readiness is wrong, Kubernetes may send traffic to pods before they are ready or remove
too many healthy pods during rollout.
```

Strong answer:

```text
Safe rollout requires correct readiness, enough replicas, conservative maxUnavailable, and
fast rollback based on version-level metrics.
```

---

## 8. PodDisruptionBudget

PDB protects availability during voluntary disruptions.

Example:

```text
Keep at least 2 Booking Service pods available during node maintenance.
```

Use for:

- node drains
- cluster upgrades
- voluntary disruptions

Does not protect from:

- app crash
- OOM kill
- zone outage by itself
- bad deployment

Strong answer:

```text
A PDB helps prevent maintenance from evicting too many replicas at once, but it is not a
replacement for replicas across zones and safe deployments.
```

---

## 9. Topology Spread And Zone Resilience

Goal:

```text
Avoid all replicas landing on one node or zone.
```

Controls:

- pod anti-affinity
- topology spread constraints
- multi-zone node groups
- PDBs
- readiness and failover testing

Strong answer:

```text
If all replicas run in one zone, a zone issue can take the service down even though replica
count looks healthy.
```

---

## 10. HPA Deep Dive

HPA scales pod count based on metrics.

Common metrics:

- CPU utilization
- memory utilization
- request rate
- queue depth
- custom latency/saturation metrics
- Kafka consumer lag, carefully

Scaling trap:

```text
Scaling consumers above partition count will not increase Kafka parallelism for one consumer
group.
```

Strong answer:

```text
Autoscaling should follow the bottleneck. For HTTP services, request rate or CPU may work.
For consumers, lag and processing rate matter, but partition count and downstream capacity
limit useful scaling.
```

---

## 11. VPA And Right-Sizing

Vertical Pod Autoscaler recommends or changes requests/limits.

Use carefully:

- helpful for right-sizing long-running services
- may restart pods depending on mode
- not a replacement for load testing
- coordinate with HPA to avoid conflicting signals

Strong answer:

```text
VPA helps right-size resources, while HPA changes replica count. They solve different parts
of capacity management.
```

---

## 12. Cluster Autoscaler

HPA adds pods, but cluster needs nodes.

Flow:

```text
HPA wants more replicas -> pods pending -> cluster autoscaler adds nodes -> scheduler places pods
```

Risk:

```text
Scale-out is not instant. During sudden spikes, pending pods and cold starts can still hurt
latency.
```

Mitigation:

- capacity buffer
- warm pods for critical paths
- predictive scaling if justified
- load shedding
- queue-based smoothing

---

## 13. Service Mesh Traffic Policies

Mesh can control:

- timeouts
- retries
- circuit breaking/outlier detection
- traffic splitting
- mTLS
- authorization policy
- fault injection

Danger:

```text
Retries in service mesh plus retries in application can multiply traffic.
```

Strong answer:

```text
Traffic policy must be coordinated across app and mesh. I avoid hidden retry amplification
by defining retry budgets and ownership for retry behavior.
```

---

## 14. Canary With Mesh Or Gateway

Canary flow:

```text
1. Deploy new version with small traffic percentage.
2. Compare old vs new metrics: error rate, latency, saturation, business success.
3. Increase gradually if healthy.
4. Roll back quickly if SLO burn or errors spike.
```

Required metrics:

- version-labeled request rate
- version-labeled error rate
- version-labeled latency
- business conversion or booking success
- dependency errors

Strong answer:

```text
Canary is only safe if metrics are separated by version. Otherwise the new version can fail
inside aggregate averages.
```

---

## 15. Config And Secret Rollout

Config failure examples:

- wrong payment endpoint
- invalid timeout value
- missing feature flag default
- secret path typo
- incompatible schema registry URL

Controls:

- config validation at startup
- readiness false if required config missing
- canary config changes
- separate deploy/config audit trail
- rollback for config as well as code

Strong answer:

```text
Config changes can be as risky as code deploys. I track, validate, canary, and roll back
config changes with the same seriousness.
```

---

## 16. Node And Cluster Failure Scenarios

| Scenario | Expected Handling |
|---|---|
| node drain | PDB and replicas preserve service |
| node crash | pods rescheduled, some in-flight requests fail |
| zone outage | replicas in other zones serve traffic |
| DNS issue | service discovery failure visible in metrics/logs |
| image pull failure | rollout blocked, old version should remain |
| bad readiness | traffic sent too early or removed incorrectly |

Interview answer:

```text
I think through node, pod, dependency, and zone failures separately because Kubernetes handles
some recovery automatically but application correctness still needs explicit design.
```

---

## 17. Backend Ownership Split

Application team owns:

- endpoint behavior
- timeouts and idempotency
- business metrics
- logs/traces
- readiness/liveness endpoint semantics
- graceful shutdown behavior
- DB/broker connection management

Platform team owns:

- cluster lifecycle
- ingress/gateway platform
- service mesh platform
- secret manager integration
- base observability platform
- node pools and policies
- deployment tooling

Shared responsibility:

- resource sizing
- autoscaling metrics
- rollout strategy
- incident runbooks

---

## 18. Common Interview Traps

| Trap | Better Answer |
|---|---|
| "Kubernetes makes it highly available" | only if replicas, probes, zones, and app behavior are correct |
| "Liveness checks DB" | DB outage should not restart every pod |
| "HPA solves all scale" | bottleneck may be DB, partition count, or downstream quota |
| "Canary means deploy one pod" | canary needs traffic split and version metrics |
| "Mesh retries are free" | retries can amplify traffic and duplicate side effects |
| "OOM restart fixes memory" | restart hides symptom; root cause still exists |

---

## 19. Strong Closing Answer

```text
For Kubernetes microservices, I design runtime behavior explicitly: correct probes, graceful
shutdown, right-sized resources, autoscaling tied to real bottlenecks, disruption budgets,
zone spreading, canary rollouts with version metrics, service mesh policies that avoid retry
amplification, and clear ownership between application and platform teams.
```
