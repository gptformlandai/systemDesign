# Kubernetes Scheduling: Affinity, Taints, and Tolerations Gold Sheet

> Track: K8s Interview Track — Phase 3: Scheduling and Resource Management
> Goal: Control where pods land in a cluster — from simple node selection to complex multi-dimensional placement constraints.

---

## 0. How To Read This

Beginner focus:
- nodeSelector: basic label-based pod placement
- What taints and tolerations do

Intermediate focus:
- nodeAffinity: flexible rule-based placement
- podAffinity and podAntiAffinity: co-locate or spread pods
- Soft (preferred) vs hard (required) rules

Senior / MAANG focus:
- Topology spread constraints: even distribution across zones/nodes
- Priority classes and preemption
- Descheduler: evict and reschedule imbalanced pods
- Karpenter vs Cluster Autoscaler for node provisioning
- Scheduling bottlenecks at scale (500+ node clusters)

---

# Topic 1: nodeSelector (Simple)

## 1. Intuition

Assign a pod to nodes that have specific labels:

```yaml
# Node labeled:
#   kubectl label node node-1 disktype=ssd
#   kubectl label node node-2 disktype=hdd

# Pod spec
spec:
  nodeSelector:
    disktype: ssd               # only schedule on ssd nodes
    kubernetes.io/os: linux
```

Limitation: only equality match (`key=value`). No OR, no NotIn, no wildcard. Use `nodeAffinity` for complex rules.

---

# Topic 2: Node Affinity

## 1. Required vs Preferred

```yaml
spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:   # HARD rule: pod won't schedule if no match
        nodeSelectorTerms:
          - matchExpressions:
              - key: topology.kubernetes.io/zone
                operator: In
                values:
                  - us-east-1a
                  - us-east-1b
      preferredDuringSchedulingIgnoredDuringExecution:  # SOFT rule: prefer but not required
        - weight: 80
          preference:
            matchExpressions:
              - key: disktype
                operator: In
                values:
                  - ssd
        - weight: 20
          preference:
            matchExpressions:
              - key: instance-type
                operator: In
                values:
                  - m5.2xlarge
```

Operators available:
| Operator | Meaning |
|---|---|
| `In` | value must be in list |
| `NotIn` | value must NOT be in list |
| `Exists` | key must exist (no value check) |
| `DoesNotExist` | key must NOT exist |
| `Gt` | value must be greater than |
| `Lt` | value must be less than |

## 2. IgnoredDuringExecution

```text
Current behavior: "IgnoredDuringExecution"
  If a node label changes AFTER a pod is running on it,
  the running pod is NOT evicted.

Future: "RequiredDuringSchedulingRequiredDuringExecution"
  (not yet available in stable)
  Would evict pods when they no longer match affinity rules.
```

---

# Topic 3: Pod Affinity and AntiAffinity

## 1. Pod Affinity — Co-locate Pods

Schedule a pod near other pods (same node/zone):

```yaml
spec:
  affinity:
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchLabels:
              app: cache-redis    # co-locate with redis pods
          topologyKey: kubernetes.io/hostname   # same NODE
```

Use case: app and its sidecar cache should be on the same node to reduce latency.

## 2. Pod AntiAffinity — Spread Pods

Prevent pods from landing on the same node/zone:

```yaml
spec:
  affinity:
    podAntiAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        - labelSelector:
            matchLabels:
              app: payment-service    # no two payment-service pods on same node
          topologyKey: kubernetes.io/hostname
      preferredDuringSchedulingIgnoredDuringExecution:
        - weight: 100
          podAffinityTerm:
            labelSelector:
              matchLabels:
                app: payment-service  # prefer different zones
            topologyKey: topology.kubernetes.io/zone
```

Use case: high-availability spread. Required antiAffinity ensures each pod is on a different node, preferred antiAffinity encourages different zones.

## 3. Warning: Required AntiAffinity Can Block Scaling

```text
If replicas=5 and only 4 nodes exist:
  Required antiAffinity (hostname) → 5th pod will NEVER schedule (Pending forever)

Preferred antiAffinity is safer for auto-scaling scenarios.
Use TopologySpreadConstraints instead (more expressive).
```

---

# Topic 4: Topology Spread Constraints

Modern replacement for complex antiAffinity patterns:

```yaml
spec:
  topologySpreadConstraints:
    - maxSkew: 1               # max difference in pod count across topology domains
      topologyKey: topology.kubernetes.io/zone    # spread across zones
      whenUnsatisfiable: DoNotSchedule           # hard rule (or ScheduleAnyway for soft)
      labelSelector:
        matchLabels:
          app: payment-service
    - maxSkew: 1
      topologyKey: kubernetes.io/hostname         # also spread across nodes
      whenUnsatisfiable: ScheduleAnyway           # soft rule
      labelSelector:
        matchLabels:
          app: payment-service
```

```text
Example: 3 AZs, 9 pods

Without constraints: all 9 could land in us-east-1a
With maxSkew=1 across zones: 3/3/3 or 4/3/2 at most

maxSkew=1 means: if zone-a has 4 pods, no zone can have fewer than 3 (or more than 5)
```

`minDomains` (K8s 1.25+):
```yaml
minDomains: 3    # require pods to spread across at least 3 zone domains
```

---

# Topic 5: Taints and Tolerations

## 1. Intuition

Taints are on NODES. Tolerations are on PODS.

```text
Taint = "I don't want random pods" (set on node by admin)
Toleration = "I'm OK with that restriction" (set on pod)

A pod can only run on a tainted node if it has a matching toleration.
```

## 2. Applying Taints to Nodes

```bash
# Add taint
kubectl taint nodes node-gpu-1 gpu=true:NoSchedule
kubectl taint nodes node-spot-1 spot=true:NoExecute

# Remove taint
kubectl taint nodes node-gpu-1 gpu=true:NoSchedule-
```

Taint effects:
| Effect | Behavior |
|---|---|
| `NoSchedule` | new pods without toleration won't schedule; existing pods unaffected |
| `PreferNoSchedule` | scheduler tries to avoid; not guaranteed |
| `NoExecute` | pods without toleration are EVICTED; new pods won't schedule |

## 3. Pod Tolerations

```yaml
spec:
  tolerations:
    - key: "gpu"
      operator: "Equal"
      value: "true"
      effect: "NoSchedule"    # tolerate the GPU taint
    - key: "spot"
      operator: "Exists"      # tolerate any spot taint regardless of value
      effect: "NoExecute"
      tolerationSeconds: 120  # tolerate NoExecute for 120s (graceful drain on spot termination)
```

## 4. Built-in Taints (Kubernetes Adds Automatically)

```text
node.kubernetes.io/not-ready:NoExecute           — node not Ready
node.kubernetes.io/unreachable:NoExecute          — node unreachable
node.kubernetes.io/memory-pressure:NoSchedule     — node memory pressure
node.kubernetes.io/disk-pressure:NoSchedule       — node disk pressure
node.kubernetes.io/pid-pressure:NoSchedule        — node PID pressure
node.kubernetes.io/unschedulable:NoSchedule       — node cordoned
node.kubernetes.io/network-unavailable:NoSchedule — node network issue
```

System pods (DaemonSets like kube-proxy, CNI) have tolerations for all these built in.

## 5. Taint Use Cases

```text
GPU nodes:
  taint: nvidia.com/gpu=true:NoSchedule
  toleration: added to GPU-workload pods only
  Effect: GPU nodes reserved exclusively for ML workloads

Spot/Preemptible nodes:
  taint: cloud.google.com/gke-spot=true:NoSchedule
  toleration: added to batch/non-critical pods
  tolerationSeconds: 300 (for NoExecute on termination)

Control plane nodes:
  taint: node-role.kubernetes.io/control-plane:NoSchedule
  Effect: no user workloads on control plane nodes
  DaemonSets tolerate this to run on all nodes

Dedicated node groups:
  taint: team=payments:NoSchedule
  toleration: added to payment-team pods
  nodeSelector: team=payments
  Effect: payment team's pods on dedicated nodes
```

---

# Topic 6: Priority Classes and Preemption

## 1. PriorityClass

```yaml
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: critical-production
value: 1000000        # higher = higher priority
globalDefault: false
description: "Production critical workloads"

---
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: batch-low
value: 100
```

## 2. Using PriorityClass in Pod

```yaml
spec:
  priorityClassName: critical-production
```

## 3. Preemption

```text
High-priority pod is Pending (no room):
  Scheduler looks for nodes where evicting low-priority pods would make room
  Preempts (evicts) lower-priority pods
  High-priority pod then schedules

This is why critical pods schedule even when cluster is full.
Low-priority batch jobs are preempted when prod traffic spikes.
```

Built-in critical priority:
```text
system-cluster-critical (2000001000) — used by core K8s components
system-node-critical    (2000000000) — DaemonSets, node-level services
```

---

# Topic 7: Karpenter vs Cluster Autoscaler

## 1. Cluster Autoscaler (Traditional)

```text
Works with pre-defined node groups (Auto Scaling Groups in AWS).
When pods are Pending: scales up the right node group.
When nodes are underutilized: scales down.

Limitations:
  - Must pre-configure node group sizes and types
  - Scaling is reactive (pods pending for ~minutes)
  - Can't provision spot + on-demand mix dynamically
```

## 2. Karpenter (Modern, AWS-native)

```text
Karpenter watches Pending pods and provisions exactly the right instance:
  - Any instance type from EC2 fleet (no pre-defined groups)
  - Provisions in seconds (API call, not ASG scaling)
  - Consolidation: replaces expensive instances with cheaper ones
  - Spot interruption handling built-in

NodePool (Karpenter CRD):
  Defines which instance types, zones, and capacity types are allowed.
```

```yaml
apiVersion: karpenter.sh/v1
kind: NodePool
metadata:
  name: default
spec:
  template:
    spec:
      requirements:
        - key: karpenter.sh/capacity-type
          operator: In
          values: ["spot", "on-demand"]
        - key: node.kubernetes.io/instance-type
          operator: In
          values: ["m5.large", "m5.xlarge", "m5.2xlarge", "m6i.large"]
        - key: topology.kubernetes.io/zone
          operator: In
          values: ["us-east-1a", "us-east-1b", "us-east-1c"]
  disruption:
    consolidationPolicy: WhenUnderutilized
    consolidateAfter: 30s
  limits:
    cpu: 1000
```

---

# Topic 8: Revision Notes and Interview Answers

## 1. Key Points

- nodeSelector: simple label equality match; use nodeAffinity for complex rules
- nodeAffinity: `required` (hard) vs `preferred` (soft); supports In/NotIn/Exists operators
- podAntiAffinity: spread pods across nodes/zones; required can block scaling
- TopologySpreadConstraints: preferred over antiAffinity for HA spread; `maxSkew` controls max imbalance
- Taint: on nodes; NoSchedule/PreferNoSchedule/NoExecute
- Toleration: on pods; allows pod to run on tainted node; `tolerationSeconds` for graceful eviction
- PriorityClass: high-priority pods preempt low-priority ones when cluster is full
- Karpenter: provisions exactly right EC2 instance per pod requirement; faster than Cluster Autoscaler

## 2. Common Interview Question

**Q: How do you spread payment-service across 3 AZs with at least 1 pod per zone?**

```text
Answer:
1. TopologySpreadConstraint with maxSkew=1, topologyKey=topology.kubernetes.io/zone, whenUnsatisfiable=DoNotSchedule
2. Set replicas >= 3 (at least 1 per zone)
3. Combine with podAntiAffinity preferred across nodes for within-zone spread

YAML:
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels:
        app: payment-service
```

## 3. Official Source Notes

- Node Affinity: <https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/>
- Taints and Tolerations: <https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/>
- Topology Spread: <https://kubernetes.io/docs/concepts/workloads/pods/pod-topology-spread-constraints/>
- Priority and Preemption: <https://kubernetes.io/docs/concepts/scheduling-eviction/pod-priority-preemption/>
- Karpenter: <https://karpenter.sh/docs/>
