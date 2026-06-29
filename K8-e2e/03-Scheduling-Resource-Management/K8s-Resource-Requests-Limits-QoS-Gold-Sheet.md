# Kubernetes Resource Requests, Limits, and QoS Gold Sheet

> Track: K8s Interview Track — Phase 3: Scheduling and Resource Management
> Goal: Understand how Kubernetes prevents noisy neighbors, handles resource contention, and why OOMKilled is the most common production incident in K8s.

---

## 0. How To Read This

Beginner focus:
- What requests and limits mean
- Why you must always set them

Intermediate focus:
- QoS classes (Guaranteed, Burstable, BestEffort) and eviction order
- LimitRange and ResourceQuota
- OOMKilled root causes and fixes

Senior / MAANG focus:
- How the scheduler uses requests (not limits) for placement
- CPU throttling vs memory OOM — different behaviors
- VPA vs manual tuning
- Right-sizing at scale with Goldilocks/VPA in recommendation mode
- Memory vs CPU contention on node under pressure

---

# Topic 1: Requests vs Limits

## 1. Intuition

```text
Requests:
  "This container needs at LEAST this much to run well"
  Used by scheduler to place pod on a node with enough capacity
  Node allocatable CPU/memory compared to total requests

Limits:
  "This container can use AT MOST this much"
  Enforced by cgroups at runtime
  CPU: throttled if exceeded (slowed down, not killed)
  Memory: OOMKilled if exceeded (container killed instantly)
```

## 2. Container Resource Spec

```yaml
resources:
  requests:
    cpu: "250m"        # 250 millicores = 0.25 CPU
    memory: "256Mi"    # 256 mebibytes
  limits:
    cpu: "500m"        # max 0.5 CPU (throttled if exceeded)
    memory: "512Mi"    # max 512 MiB (OOMKilled if exceeded)
```

CPU notation:
```text
1     = 1 CPU core (1000m)
500m  = 0.5 CPU core
250m  = 0.25 CPU core (25% of 1 core)
100m  = 0.1 CPU core
```

## 3. How Scheduling Uses Requests

```text
Node: 4 CPU, 8 GiB RAM
  Existing pods requesting: 3 CPU, 6 GiB
  Available for scheduling: 1 CPU, 2 GiB

New pod requests: 500m CPU, 512Mi
  Scheduler: 500m ≤ 1 CPU, 512Mi ≤ 2 GiB → Node is viable

New pod requests: 1.5 CPU, 512Mi
  Scheduler: 1.5 CPU > 1 CPU → Node rejected

Key insight: scheduler looks at REQUESTS, not limits, not actual usage.
A pod requesting 100m CPU can burst to 2 CPU if the node has idle capacity.
```

## 4. CPU Behavior: Throttling

When a container exceeds its CPU limit:
```text
CPU time is THROTTLED (slowed down):
  - Container not killed
  - Request latency increases
  - Metric to watch: container_cpu_cfs_throttled_periods_total

This is silent performance degradation.
Very common in Java/Go services that spike on startup.

Fix: increase CPU limit, or remove CPU limit entirely (some argue this is better)
The "no CPU limits" school of thought:
  - Requests still govern scheduling fairness
  - Limits only cause throttling that hurts latency
  - Node over-commitment is acceptable if usage is typically low
```

## 5. Memory Behavior: OOMKilled

When a container exceeds its memory limit:
```text
Container is KILLED immediately by the Linux OOM killer
  - Exit code: 137 (128 + SIGKILL)
  - k8s event: OOMKilled
  - Pod restarts (if restart policy = Always)

This causes service interruption.

Causes:
  1. Limit set too low (fix: increase limit)
  2. Memory leak (fix: debug heap dumps)
  3. JVM not respecting cgroup limits (fix: -XX:+UseContainerSupport in Java 8u191+)
  4. Large request handling (spikes: increase limit or use streaming)
```

---

# Topic 2: QoS Classes

## 1. Three QoS Classes

Kubernetes assigns QoS class to each pod based on requests/limits:

### Guaranteed
```yaml
resources:
  requests:
    cpu: "500m"
    memory: "256Mi"
  limits:
    cpu: "500m"      # limits == requests for ALL containers
    memory: "256Mi"
```

```text
Guaranteed:
  - requests == limits for every container (including init containers)
  - Last to be evicted under node memory pressure
  - Highest priority in Linux OOM killer
  - Use for: critical databases, control plane components
```

### Burstable
```yaml
resources:
  requests:
    cpu: "250m"
    memory: "256Mi"
  limits:
    cpu: "500m"      # limits > requests, OR some containers have no limits
    memory: "512Mi"
```

```text
Burstable:
  - At least one container has requests set
  - Requests != limits (or limits missing for some containers)
  - Evicted after BestEffort, before Guaranteed
  - Use for: most application workloads
```

### BestEffort
```yaml
# No requests or limits defined (default if omitted)
containers:
  - name: my-app
    image: my-app:latest
    # no resources block
```

```text
BestEffort:
  - No requests or limits anywhere in the pod
  - First to be evicted under memory pressure
  - Gets whatever CPU is left over
  - NEVER use in production
```

## 2. Eviction Order Under Memory Pressure

```text
Node is running low on memory (kubelet watches memory.available threshold):

1. Evict BestEffort pods first
2. Evict Burstable pods that exceed their requests
3. Evict Guaranteed pods (last resort, only if node still pressured)

Within same QoS class: highest usage/request ratio evicted first
```

## 3. Node Conditions and Thresholds

```text
kubelet eviction thresholds (configurable in kubelet config):

memory.available < 100Mi → eviction starts
memory.available < 200Mi → "hard eviction threshold"
memory.available < 500Mi → "soft eviction threshold" (eviction after grace period)

nodefs.available < 10% → node disk pressure
imagefs.available < 15% → image storage pressure
```

---

# Topic 3: LimitRange

LimitRange sets default and max resource values per namespace:

```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: default-limits
  namespace: team-payments
spec:
  limits:
    - type: Container
      default:             # applied to containers with no limits set
        cpu: "500m"
        memory: "512Mi"
      defaultRequest:      # applied to containers with no requests set
        cpu: "100m"
        memory: "128Mi"
      max:                 # container cannot exceed this
        cpu: "2"
        memory: "4Gi"
      min:                 # container must request at least this
        cpu: "50m"
        memory: "64Mi"
    - type: PersistentVolumeClaim
      max:
        storage: 20Gi
      min:
        storage: 1Gi
```

Effect: containers without requests/limits get `default` and `defaultRequest` applied. If container exceeds `max`, pod admission is rejected.

---

# Topic 4: ResourceQuota

ResourceQuota limits total resource consumption per namespace:

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: team-payments-quota
  namespace: team-payments
spec:
  hard:
    # Compute
    requests.cpu: "8"
    requests.memory: "16Gi"
    limits.cpu: "16"
    limits.memory: "32Gi"
    # Object count
    pods: "50"
    services: "20"
    replicationcontrollers: "5"
    secrets: "50"
    configmaps: "50"
    persistentvolumeclaims: "10"
    services.loadbalancers: "2"
    services.nodeports: "0"      # disallow NodePort services
```

Effect: new pod is rejected if it would exceed quota. Developers must clean up before deploying.

---

# Topic 5: Production Right-Sizing Patterns

## 1. The Goldilocks Problem

```text
Too small requests:
  → Pod gets evicted under pressure (never gets its minimum)
  → Scheduler misplaces pods (thinks node has room when it doesn't)

Too large requests:
  → Nodes appear full even when actual usage is low
  → Poor bin packing → wasted money

Too small limits:
  → OOMKilled on spike traffic
  → CPU throttled → latency spikes

Too large limits:
  → Pods can consume far more than expected
  → One noisy pod starves others
```

## 2. Right-Sizing Process

```text
Step 1: Deploy with generous limits and requests estimates

Step 2: Run for 1-2 weeks, collect actual usage metrics from Prometheus:
  container_memory_usage_bytes (peak over 2 weeks)
  container_cpu_usage_seconds_total (rate, p99)

Step 3: Set:
  memory request = p95 memory usage
  memory limit = p99 memory usage * 1.2 (20% headroom)
  cpu request = p50 cpu usage
  cpu limit = p99 cpu usage * 1.5 (or remove CPU limit)

Step 4: Enable VPA in recommendation mode:
  VPA reads actual usage and recommends new values
  Apply VPA recommendations to Deployment (not auto mode in prod initially)
```

## 3. VPA Overview

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: payment-service-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-service
  updatePolicy:
    updateMode: "Off"    # Off=recommendation only; Auto=restart pods to apply
  resourcePolicy:
    containerPolicies:
      - containerName: payment-service
        minAllowed:
          cpu: "100m"
          memory: "128Mi"
        maxAllowed:
          cpu: "2"
          memory: "2Gi"
```

VPA modes:
- `Off`: calculates recommendations only (safe for production analysis)
- `Initial`: applies on pod creation only
- `Auto`: updates running pods (causes restarts — use with PDB)

## 4. Interview Traps

| Trap | Reality |
|---|---|
| "Limits guarantee that much resource" | Limits are maximums; the scheduler only uses requests |
| "CPU OOM kills the pod" | CPU is throttled (slowdown); only MEMORY OOM kills |
| "BestEffort pods run fine" | They're evicted first under pressure — never use in production |
| "Guaranteed QoS means pods are never evicted" | They're evicted last, but can still be evicted if node is critically pressured |
| "VPA and HPA can run together safely" | They conflict on CPU metric; HPA on custom metrics + VPA on memory is safer |

## 5. Revision Notes

- requests: minimum needed; used by scheduler for pod placement; CPU/memory
- limits: maximum allowed; CPU throttled if exceeded; memory OOMKilled if exceeded
- QoS classes: Guaranteed (requests==limits) > Burstable > BestEffort
- Eviction order: BestEffort first, Burstable second, Guaranteed last
- LimitRange: default and max per container; applied when pod created in namespace
- ResourceQuota: total cap on resources per namespace; admission denied if exceeded
- OOMKilled: exit code 137; fix by increasing memory limit or fixing memory leak
- VPA: recommends right-sized requests/limits based on actual usage history

## 6. Official Source Notes

- Resource management: <https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/>
- QoS classes: <https://kubernetes.io/docs/concepts/workloads/pods/pod-qos/>
- LimitRange: <https://kubernetes.io/docs/concepts/policy/limit-range/>
- ResourceQuota: <https://kubernetes.io/docs/concepts/policy/resource-quotas/>
- VPA: <https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler>
