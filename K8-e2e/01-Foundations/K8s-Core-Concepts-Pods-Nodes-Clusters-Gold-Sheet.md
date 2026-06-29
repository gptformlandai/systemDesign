# Kubernetes Core Concepts: Pods, Nodes, and Clusters Gold Sheet

> Track: K8s Interview Track — Phase 1: Foundations
> Goal: Understand the Kubernetes object model from first principles. Know what every core object is, how they relate, and how the control plane keeps everything running.

---

## 0. How To Read This

Beginner focus:
- What is a Pod, Node, Cluster, Namespace
- Control plane vs worker node roles
- kubectl get/describe/logs basics

Intermediate focus:
- Pod lifecycle states (Pending → Running → Succeeded/Failed)
- Container restart policies
- Init containers and sidecar pattern
- Resource namespacing and isolation

Senior / MAANG focus:
- Control plane component internals (etcd, API server, scheduler, controller-manager)
- Pod scheduling flow (admission → scheduling → binding → kubelet → runtime)
- Static Pods, Mirror Pods
- Pod disruption budgets
- Why etcd is the source of truth and its quorum requirement

---

# Topic 1: Kubernetes Architecture

## 1. Intuition

Kubernetes is a container orchestration platform. You tell it the desired state ("run 3 copies of this container"), and Kubernetes continuously works to make actual state match desired state.

```text
You declare:   I want 3 replicas of payment-service running
Kubernetes:    Creates 3 Pods, watches them, restarts any that crash
```

This is the core loop: **reconciliation** — compare desired state vs actual state, act to close the gap.

## 2. Cluster Components

```text
Control Plane (brain):
  kube-apiserver        — all communication goes through this REST API
  etcd                  — distributed key-value store; single source of truth
  kube-scheduler        — assigns pods to nodes
  kube-controller-manager — runs controllers (Deployment, ReplicaSet, Node, etc.)
  cloud-controller-manager — integrates with cloud provider (AWS, GCP, Azure)

Worker Nodes (muscle):
  kubelet               — runs on each node; ensures containers are running
  kube-proxy            — maintains network rules (iptables/ipvs) for Services
  container runtime     — actually runs containers (containerd, CRI-O)
```

## 3. The Scheduling Flow (Pod Creation)

When you apply a Deployment:

```text
1. kubectl apply → API Server stores desired state in etcd
2. Deployment Controller creates ReplicaSet
3. ReplicaSet Controller creates Pods (status: Pending, no node assigned)
4. Scheduler watches for unscheduled Pods
   → Filters: finds nodes that meet resource requirements
   → Scores: ranks remaining nodes (least utilized, affinity rules)
   → Binds: updates Pod's spec.nodeName in etcd
5. Kubelet on assigned node watches for Pods assigned to it
   → Pulls container image (if not cached)
   → Starts containers via container runtime
   → Reports status back to API Server
```

## 4. etcd — The Source of Truth

etcd is a distributed consensus database using the Raft protocol.

```text
Requirements:
  - Odd number of members: 3 (tolerate 1 failure) or 5 (tolerate 2 failures)
  - Quorum = (n/2) + 1: a 3-node cluster needs 2 healthy for writes

If etcd loses quorum:
  - API Server can still serve reads (cached data)
  - Writes fail (no new state changes)
  - Cluster effectively frozen
  - Kubernetes does NOT delete running pods (they keep running)

Backup:
  etcdctl snapshot save /backup/etcd.db
  etcdctl snapshot restore /backup/etcd.db
```

Interview trap:
```text
"Does etcd going down kill running pods?"
Answer: NO. Pods are managed by kubelet on the node. Running pods continue running.
New pods cannot be scheduled, and failing pods cannot be replaced.
```

---

# Topic 2: Pods

## 1. What A Pod Is

A Pod is the smallest deployable unit in Kubernetes.

```text
Pod = one or more containers + shared:
  - Network namespace (same IP, same ports)
  - Storage volumes
  - Lifecycle (started together, stopped together)

Most pods have one container.
Multi-container pods are used for sidecar patterns.
```

## 2. Pod Lifecycle States

| Phase | Meaning |
|---|---|
| Pending | Pod accepted by cluster, but container(s) not running yet (scheduling, image pull) |
| Running | At least one container is running |
| Succeeded | All containers exited with code 0 (completed successfully) |
| Failed | All containers have stopped; at least one exited with non-zero code |
| Unknown | Pod state cannot be determined (node communication failure) |

Container states (within a Pod):
- `Waiting` — not running (pulling image, waiting to start)
- `Running` — executing
- `Terminated` — stopped (with exit code and reason)

## 3. Restart Policies

| Policy | Behavior |
|---|---|
| `Always` | always restart on any exit (default for Deployments) |
| `OnFailure` | restart only on non-zero exit code (Jobs) |
| `Never` | never restart (one-off tasks) |

## 4. Pod Spec Anatomy

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: payment-service
  namespace: prod
  labels:
    app: payment-service
    version: v1.2.3
spec:
  serviceAccountName: payment-service-sa  # RBAC identity
  securityContext:                          # pod-level security
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 2000
  initContainers:
    - name: db-migration
      image: payment-service:v1.2.3
      command: ["./migrate", "--run"]
  containers:
    - name: payment-service
      image: payment-service:v1.2.3
      ports:
        - containerPort: 8080
      resources:
        requests:
          cpu: "250m"
          memory: "256Mi"
        limits:
          cpu: "500m"
          memory: "512Mi"
      livenessProbe:
        httpGet:
          path: /health
          port: 8080
        initialDelaySeconds: 30
        periodSeconds: 10
      readinessProbe:
        httpGet:
          path: /ready
          port: 8080
        initialDelaySeconds: 5
        periodSeconds: 5
      env:
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: payment-db-secret
              key: host
      volumeMounts:
        - name: config
          mountPath: /etc/config
  volumes:
    - name: config
      configMap:
        name: payment-service-config
  restartPolicy: Always
```

## 5. Init Containers

Init containers run to completion before app containers start:

```text
Use cases:
  - Database migration before app starts
  - Wait for dependency to be ready
  - Download config/certificates before app needs them
  - Populate shared volume with data

Properties:
  - Run in order (each must succeed before next starts)
  - App containers don't start until ALL init containers succeed
  - Different image than app container (can use tools not in app image)
  - If init container fails: entire pod restart (based on restart policy)
```

## 6. Sidecar Pattern

Sidecars run alongside the main container in the same Pod:

```yaml
containers:
  - name: payment-service
    image: payment-service:v1
  - name: log-collector        # sidecar
    image: fluent-bit:latest
    volumeMounts:
      - name: logs
        mountPath: /var/log
  - name: envoy-proxy          # sidecar
    image: envoy:v1.28
    ports:
      - containerPort: 9000
```

Common sidecar uses:
- Log shipping (Fluent Bit, Filebeat)
- Service mesh proxies (Envoy/Istio)
- Secret rotation (Vault agent)
- Certificate renewal

---

# Topic 3: Nodes

## 1. Node Components

Every worker node runs:
- `kubelet`: receives pod specs, manages containers, reports health
- `kube-proxy`: maintains iptables/ipvs rules for Service routing
- `container runtime`: runs containers (containerd is standard)

## 2. Node Conditions

```text
kubectl describe node my-node

Conditions:
  Ready           True    node is healthy and ready to accept pods
  MemoryPressure  False   node is not running low on memory
  DiskPressure    False   node is not running low on disk
  PIDPressure     False   too many processes on node
  NetworkUnavailable False node network is configured correctly
```

If `Ready=False`: pods on that node may be evicted (after `pod-eviction-timeout`, default 5 minutes).

## 3. Node Drain and Cordon

```bash
# Cordon: mark node as unschedulable (no new pods)
kubectl cordon node-1

# Drain: evict all pods (respecting PodDisruptionBudget) and cordon
kubectl drain node-1 --ignore-daemonsets --delete-emptydir-data

# Uncordon: allow pods again
kubectl uncordon node-1
```

Use before:
- Node maintenance (OS upgrade, hardware replacement)
- Decommissioning a node

---

# Topic 4: Namespaces

## 1. Purpose

Namespaces provide virtual clusters within a physical cluster:

```text
Use namespaces to isolate:
  - Teams: team-payments, team-orders, team-platform
  - Environments: dev, staging, prod (better: separate clusters for prod)
  - Applications: namespace-per-microservice in large monorepos
```

Default namespaces:
- `default` — where objects land if no namespace specified
- `kube-system` — control plane and cluster infrastructure components
- `kube-public` — public readable data (cluster info)
- `kube-node-lease` — node heartbeat lease objects

## 2. What Namespaces Isolate vs What They Don't

| Isolated By Namespace | NOT Isolated By Namespace |
|---|---|
| Pods, Services, Deployments, ConfigMaps, Secrets | Nodes |
| ResourceQuota and LimitRange | PersistentVolumes (cluster-scoped) |
| RBAC roles (Role, RoleBinding) | ClusterRoles, ClusterRoleBindings |
| Network Policies (if CNI supports) | StorageClasses |

Network traffic: pods in different namespaces can communicate by default (unless Network Policy blocks it).

## 3. ResourceQuota Per Namespace

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: team-payments-quota
  namespace: team-payments
spec:
  hard:
    requests.cpu: "10"
    requests.memory: "20Gi"
    limits.cpu: "20"
    limits.memory: "40Gi"
    pods: "50"
    persistentvolumeclaims: "10"
```

---

# Topic 5: Labels, Selectors, and Annotations

## 1. Labels

Key-value pairs attached to objects, used for selection:

```yaml
labels:
  app: payment-service
  version: v1.2.3
  environment: prod
  team: payments
  tier: backend
```

Selectors filter objects by label:

```bash
kubectl get pods -l app=payment-service,environment=prod
kubectl get pods -l "version in (v1.2.3, v1.2.4)"
kubectl get pods -l "environment notin (dev,staging)"
```

## 2. Annotations

Key-value pairs for non-identifying metadata (tools, monitoring, policies):

```yaml
annotations:
  kubernetes.io/created-by: "payment-team"
  prometheus.io/scrape: "true"
  prometheus.io/port: "9090"
  deployment.kubernetes.io/revision: "3"
  kubectl.kubernetes.io/last-applied-configuration: "..."
```

Annotations are for metadata, NOT for selection. Selectors only work on labels.

## 3. Label Design Best Practices

```text
Recommended labels (from kubernetes.io/docs):
  app.kubernetes.io/name: payment-service
  app.kubernetes.io/version: v1.2.3
  app.kubernetes.io/component: api
  app.kubernetes.io/part-of: payment-platform
  app.kubernetes.io/managed-by: helm
  app.kubernetes.io/instance: payment-service-prod
```

---

# Topic 6: The Control Loop Pattern

## 1. How Kubernetes Controllers Work

Every Kubernetes resource has a controller. Controllers run an infinite loop:

```text
loop:
  desired := read desired state from etcd (spec)
  actual  := observe actual state (status)
  if desired != actual:
    take action to reconcile
  sleep or watch for changes
```

Example — ReplicaSet controller:

```text
desired: 3 replicas of payment-service
actual:  2 pods running

action:  create 1 new Pod
```

This is why Kubernetes is self-healing: controllers always notice divergence and fix it.

## 2. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Running workloads as root in containers | set `runAsNonRoot: true` in securityContext |
| No resource requests/limits on pods | always set requests and limits |
| `default` namespace for all workloads | dedicated namespace per team/environment |
| No labels for selection | always label: app, version, environment |
| Using Pod directly (not via Deployment) | bare Pods are not rescheduled on failure |
| `latest` image tag | always use immutable versioned tags |
| No init containers for migration | use init container, not application startup logic |

## 3. Revision Notes

- Control plane: API server (REST gateway), etcd (state store), scheduler (pod placement), controller-manager (reconciliation loops)
- Pod: one or more containers with shared network + storage; smallest schedulable unit
- Pod phases: Pending → Running → Succeeded/Failed/Unknown
- Init containers run sequentially to completion before app containers start
- etcd quorum: 3 nodes tolerate 1 failure; (n/2)+1 needed for writes
- Namespaces: isolate workloads per team/environment; don't isolate Nodes, PVs
- Labels: for selection; Annotations: for metadata only
- Control loop: every controller continuously reconciles desired vs actual state

## 4. Official Source Notes

- Pod spec: <https://kubernetes.io/docs/concepts/workloads/pods/>
- Cluster architecture: <https://kubernetes.io/docs/concepts/architecture/>
- Init containers: <https://kubernetes.io/docs/concepts/workloads/pods/init-containers/>
- Namespaces: <https://kubernetes.io/docs/concepts/overview/working-with-objects/namespaces/>
