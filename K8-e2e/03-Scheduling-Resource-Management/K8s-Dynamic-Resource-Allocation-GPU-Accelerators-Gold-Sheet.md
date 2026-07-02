# Kubernetes Dynamic Resource Allocation, GPUs, and Accelerators Gold Sheet

> Track: K8s Interview Track - Phase 3: Scheduling and Resource Management Plus
> Goal: Understand how Kubernetes schedules scarce hardware such as GPUs, FPGAs, NICs, and accelerator pools using classic extended resources and modern Dynamic Resource Allocation.

---

## 0. How To Read This

Beginner focus:
- Pods request CPU and memory.
- Some pods also need special devices such as GPUs.
- The scheduler must place those pods on nodes with matching devices.

Intermediate focus:
- Classic device plugins expose extended resources such as `nvidia.com/gpu`.
- Dynamic Resource Allocation adds API objects for richer device requests.
- Device scheduling must consider health, topology, capacity, and isolation.

Senior / MAANG focus:
- Accelerators are expensive and scarce; poor scheduling wastes money.
- DRA is for device classes, claim lifecycle, driver-specific allocation, and better control-plane visibility.
- Security and tenancy matter: GPU sharing, MIG, device health, and admin access need explicit policy.

---

# Topic 1: Classic GPU Scheduling

## 1. Mental Model

```text
Device plugin:
  "Node has 8 GPUs. I advertise nvidia.com/gpu=8 to kubelet."

Pod:
  "I request nvidia.com/gpu: 1."

Scheduler:
  "Place this pod on a node with at least 1 unallocated GPU."
```

Example:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: trainer
spec:
  containers:
    - name: trainer
      image: example/trainer:1.0
      resources:
        limits:
          nvidia.com/gpu: 1
```

Strength:
- Simple.
- Mature.
- Good for whole-device allocation.

Limitations:
- Limited device metadata in the scheduling API.
- Harder to express complex device classes and constraints.
- Sharing, partitioning, and health-aware scheduling are driver-specific.

---

# Topic 2: Dynamic Resource Allocation

## 1. Definition

```text
Dynamic Resource Allocation (DRA):
  Kubernetes API and scheduling model for allocating specialized resources
  through ResourceClaims, DeviceClasses, ResourceSlices, and DRA drivers.
```

Core API objects:
| Object | Owner | Purpose |
|---|---|---|
| `DeviceClass` | Admin or driver | Defines a class of devices users can request |
| `ResourceClaim` | User/workload | Requests one or more devices |
| `ResourceClaimTemplate` | Workload author | Generates claims for replicated pods |
| `ResourceSlice` | Driver | Publishes available devices and attributes |
| DRA driver | Vendor/platform | Allocates, prepares, and unprepares devices |

## 2. Flow

```text
1. Admin installs DRA driver.
2. Driver publishes ResourceSlices with device inventory and attributes.
3. Admin defines DeviceClasses or approves driver-provided classes.
4. Workload creates ResourceClaim or ResourceClaimTemplate.
5. Scheduler considers claim requirements during scheduling.
6. Driver prepares the device on the selected node.
7. Pod starts with allocated device information.
8. When pod/claim is deleted, driver releases the device.
```

---

# Topic 3: DRA YAML Shape

The exact selectors and driver fields depend on the installed DRA driver. This example shows the control-plane shape.

```yaml
apiVersion: resource.k8s.io/v1
kind: ResourceClaim
metadata:
  name: trainer-gpu
spec:
  devices:
    requests:
      - name: gpu
        exactly:
          deviceClassName: example-device-class
          allocationMode: All
          selectors:
            - cel:
                expression: |-
                  device.attributes["driver.example.com"].type == "gpu" &&
                  device.attributes["driver.example.com"].model == "a100"
---
apiVersion: v1
kind: Pod
metadata:
  name: trainer
spec:
  resourceClaims:
    - name: gpu
      resourceClaimName: trainer-gpu
  containers:
    - name: trainer
      image: example/trainer:1.0
      resources:
        claims:
          - name: gpu
```

Interview-safe caveat:
```text
In a real cluster, always check the DRA driver's documentation because device
attributes, selectors, allocation modes, and feature gates vary by driver and
Kubernetes version.
```

---

# Topic 4: DeviceClass Design

Good DeviceClasses:
```text
gpu-a100:
  High-end training workloads.

gpu-l4:
  Inference workloads.

gpu-shared-small:
  Low-cost development and notebook workloads.

rdma-fast:
  Low-latency distributed training.
```

Bad DeviceClasses:
```text
gpu:
  Too vague. Users cannot express cost/performance intent.

prod-gpu:
  Encodes environment instead of device capability.
```

---

# Topic 5: Scheduling and Cost Strategy

| Workload | Strategy |
|---|---|
| Interactive notebook | Shared or cheaper GPU class, quota-limited namespace |
| Batch training | Queue-backed Jobs, preemptible/spot nodes if checkpointed |
| Real-time inference | Dedicated GPU class, PDB, topology spread, on-demand nodes |
| Distributed training | Gang scheduling, RDMA-aware nodes, topology constraints |
| Regulated workload | Dedicated node pool, audit, encrypted artifacts |

Cost controls:
- Namespace `ResourceQuota` for accelerator claims.
- Team budgets and approval for high-end GPU classes.
- Karpenter NodePools separated by accelerator family.
- Idle GPU alerts.
- Job checkpointing before spot/preemptible use.

---

# Topic 6: Failure Modes

| Failure | Symptom | Debug Path |
|---|---|---|
| No compatible device | Pod Pending | Describe pod, claim, DeviceClass, ResourceSlices |
| Driver not running | Claims never allocate | Check DRA driver pods and RBAC |
| Device unhealthy | Pod evicted or claim blocked | Check device health and node events |
| Wrong node pool | Pending despite devices elsewhere | Check node selectors, taints, Karpenter constraints |
| Oversharing GPU | Latency spikes | Enforce class policy and per-workload limits |
| No checkpointing | Spot interruption loses work | Add checkpoint storage and restart policy |

---

# Topic 7: DRA vs Device Plugin

| Need | Device Plugin | DRA |
|---|---|---|
| Simple whole GPU request | Good fit | Also possible |
| Rich device selection | Limited | Strong fit |
| Claim lifecycle visible to API | Limited | Strong fit |
| Device health and allocation status | Driver-specific | Better API surface |
| Complex sharing or partitioning | Driver-specific | Stronger direction |
| Broad maturity today | Strong | Growing, version-sensitive |

---

# Topic 8: Interview Scenario

> Your ML platform has A100, L4, and shared development GPUs. Teams complain that cheap notebooks consume A100 capacity needed for production training. How would you redesign scheduling?

Strong answer:
```text
I would stop exposing all GPUs through one generic path. I would create separate
device classes or node pools for A100 training, L4 inference, and shared dev.
Then I would apply namespace quota and policy so notebook namespaces cannot
request A100 claims. Production training jobs get higher PriorityClass, PDBs if
serving, checkpointing for interruption safety, and observability for idle GPU
time. If the cluster uses DRA, I would model these as DeviceClasses and
ResourceClaims. If it is still on classic device plugins, I would use labels,
taints, tolerations, extended resources, and admission policy to enforce the
same intent.
```

---

# Topic 9: Revision Notes

- Device plugin exposes extended resources; DRA models device allocation as API objects.
- DRA stable base is modern Kubernetes, but individual features and drivers remain version-sensitive.
- Main DRA objects: `DeviceClass`, `ResourceClaim`, `ResourceClaimTemplate`, `ResourceSlice`.
- Accelerator scheduling is about cost, scarcity, topology, health, and security.
- Always pair GPU scheduling with quota, observability, checkpointing, and node pool design.

## Official Source Notes

- Dynamic Resource Allocation: <https://kubernetes.io/docs/concepts/scheduling-eviction/dynamic-resource-allocation/>
- Allocate devices with DRA: <https://kubernetes.io/docs/tasks/configure-pod-container/assign-resources/allocate-devices-dra/>
- Assign devices to containers and pods: <https://kubernetes.io/docs/tasks/configure-pod-container/assign-pod-node/>
- Resource quotas: <https://kubernetes.io/docs/concepts/policy/resource-quotas/>
