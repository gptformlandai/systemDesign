# Kubernetes Multi-Tenancy and Cluster Design Gold Sheet

> Track: K8s Interview Track — Phase 6: Advanced Patterns
> Goal: Design Kubernetes clusters that serve dozens of teams safely, with proper isolation, cost attribution, and governance — the platform engineering challenge at every large company.

---

## 0. How To Read This

Beginner focus:
- What multi-tenancy means in K8s
- Namespace-based isolation model

Intermediate focus:
- Soft vs hard multi-tenancy
- ResourceQuota and LimitRange per team
- RBAC for team autonomy
- Hierarchical Namespace Controller

Senior / MAANG focus:
- Cluster-per-tenant vs namespace-per-tenant tradeoffs
- Virtual clusters (vCluster) for strong isolation
- Cost attribution and chargeback
- Control plane blast radius
- Compliance and audit for multi-tenant environments

---

# Topic 1: What Multi-Tenancy Means in K8s

## 1. Two Models

### Soft Multi-Tenancy
```text
Multiple teams share one cluster, isolated by namespace.

Isolation via:
  - RBAC: team can only see their namespace
  - ResourceQuota: team's resource usage capped
  - Network Policies: team's pods can't reach other team's pods
  - LimitRange: prevents unbounded pod sizes

Security gaps:
  - Shared etcd (team can see resource names of others via side-channels)
  - Shared kubelet (noisy neighbor on same node)
  - Kernel-level isolation via cgroups (no VM-level isolation)
  - Cluster admin access = access to all namespaces
```

### Hard Multi-Tenancy
```text
Each tenant gets their own cluster (or near-cluster isolation).

Options:
  - Separate clusters per tenant
  - vCluster (virtual clusters inside one physical cluster)
  - Cluster API (provision tenant clusters as K8s objects)

Use when:
  - Regulatory requirements (PCI-DSS, HIPAA — tenant data must not coexist)
  - Untrusted tenants (external customers, not just internal teams)
  - Different SLA requirements (can't let one tenant's activity impact another)
```

---

# Topic 2: Namespace-Based Isolation (Soft Multi-Tenancy)

## 1. One Namespace Per Team (Simple)

```text
team-payments/    — payments team workloads
team-orders/      — orders team workloads
team-identity/    — identity team workloads
shared-monitoring/ — platform team's monitoring stack
```

## 2. Multiple Namespaces Per Team

```text
For teams with separate environments or microservices:

payments-dev/
payments-staging/
payments-prod/

or:

payments-api/
payments-worker/
payments-db/
```

## 3. Hierarchical Namespaces (HNC)

Hierarchical Namespace Controller allows namespace trees with inherited policies:

```text
team-payments (parent)
├── payments-api
├── payments-worker
└── payments-db

Benefits:
  - RBAC role in parent auto-propagates to children
  - Network Policies in parent apply to children
  - ResourceQuota in parent caps total across all children
```

```bash
kubectl hns create payments-api --parent=team-payments
kubectl hns create payments-worker --parent=team-payments
```

---

# Topic 3: Team Provisioning Template

When a new team onboards, apply:

```yaml
# 1. Namespace
apiVersion: v1
kind: Namespace
metadata:
  name: team-payments
  labels:
    team: payments
    cost-center: "CC-1234"
    environment: prod
    pod-security.kubernetes.io/enforce: restricted

---
# 2. ResourceQuota
apiVersion: v1
kind: ResourceQuota
metadata:
  name: team-payments-quota
  namespace: team-payments
spec:
  hard:
    requests.cpu: "20"
    requests.memory: "40Gi"
    limits.cpu: "40"
    limits.memory: "80Gi"
    pods: "100"
    services: "20"
    persistentvolumeclaims: "20"
    services.loadbalancers: "0"   # teams use Ingress, not LB services
    services.nodeports: "0"

---
# 3. LimitRange (defaults if team doesn't set resources)
apiVersion: v1
kind: LimitRange
metadata:
  name: team-payments-limits
  namespace: team-payments
spec:
  limits:
    - type: Container
      default:
        cpu: "500m"
        memory: "512Mi"
      defaultRequest:
        cpu: "100m"
        memory: "128Mi"
      max:
        cpu: "4"
        memory: "8Gi"

---
# 4. RBAC — team gets admin in their namespace
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: payments-team-admin
  namespace: team-payments
subjects:
  - kind: Group
    name: payments-team        # from OIDC/LDAP
    apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: admin                  # built-in namespace admin
  apiGroup: rbac.authorization.k8s.io

---
# 5. Network Policy — deny all; team adds their own allow rules
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: team-payments
spec:
  podSelector: {}
  policyTypes: [Ingress, Egress]
```

---

# Topic 4: Node Isolation Per Team

## 1. Dedicated Node Groups

```text
Problem: noisy neighbor — team-A's CPU-heavy job slows team-B's latency-sensitive service.

Solution: dedicated node groups per team (or workload type).

team-payments node group:
  taint: team=payments:NoSchedule
  label: team=payments

Payments pods have toleration for the taint + nodeSelector for team=payments.
Other pods can't land on these nodes.

Node types:
  compute-optimized: for CPU-intensive workloads (AI/ML training)
  memory-optimized: for in-memory databases, caches
  general: for standard microservices
```

## 2. Karpenter NodePool Per Team

```yaml
apiVersion: karpenter.sh/v1
kind: NodePool
metadata:
  name: team-payments
spec:
  template:
    metadata:
      labels:
        team: payments
    spec:
      taints:
        - key: team
          value: payments
          effect: NoSchedule
      requirements:
        - key: karpenter.sh/capacity-type
          operator: In
          values: ["on-demand"]
        - key: node.kubernetes.io/instance-type
          operator: In
          values: ["m5.xlarge", "m5.2xlarge"]
  limits:
    cpu: 200    # cap on total CPU for this NodePool
```

---

# Topic 5: Cost Attribution

## 1. Label-Based Cost Tracking

```text
Labels on all resources:
  team: payments
  cost-center: CC-1234
  environment: prod
  product: payment-platform

Kubecost:
  - Kubernetes cost monitoring tool
  - Reads labels, calculates cost per team/product/environment
  - Chargeback reports: "team-payments used $5,200 this month"

AWS Cost Explorer:
  - Tag EC2 nodes with team label
  - See EKS node cost per team tag
```

## 2. Showback vs Chargeback

```text
Showback: show teams their usage (no billing)
Chargeback: actually bill teams (internal billing)

Both require:
  - Consistent labeling of all resources
  - Cost tool (Kubecost, OpenCost, cloud native cost)
  - Baseline: split shared resources (control plane, monitoring) proportionally
```

---

# Topic 6: Cluster Sizing and Topology

## 1. One Big Cluster vs Many Small Clusters

| Factor | One Cluster | Many Clusters |
|---|---|---|
| Cost | Cheaper (shared control plane, monitoring) | More overhead per cluster |
| Blast radius | Cluster failure affects all teams | Failure isolated per cluster |
| Isolation | Soft (namespace) | Hard (cluster boundary) |
| Upgrade risk | One upgrade affects all | Upgrade one cluster at a time |
| Ops complexity | Simpler (one thing to manage) | Many clusters to monitor |
| RBAC | Shared etcd; cluster-admin is dangerous | Full isolation |

**MAANG answer**: Hub-and-spoke model:
```text
Hub cluster: ArgoCD, monitoring, shared services
Spoke clusters: one per business unit or environment (prod-payments, prod-orders)
Dev clusters: shared, cost-optimized, with aggressive ResourceQuota
```

## 2. Control Plane Sizing (EKS/GKE Managed)

```text
Managed K8s (EKS, GKE, AKS): control plane sized by cloud provider.
You pay for the control plane (fixed monthly cost).

EKS:
  1,000 pods/cluster: fine on default control plane
  5,000 pods/cluster: watch API server latency; increase etcd disk IOPS
  5,000 nodes (theoretical max): requires multiple API server replicas

Performance considerations:
  - Every pod adds watch events; large clusters = more API server load
  - CRDs with many instances slow down API server
  - Keep etcd lean: delete completed Jobs, old ReplicaSets, events
```

## 3. Node Size Selection

```text
Many small nodes:
  ✅ Better bin packing of varied pod sizes
  ✅ Failure of one node less impactful
  ❌ More nodes = more kubelet overhead
  ❌ More nodes to manage/upgrade

Fewer large nodes:
  ✅ Fewer nodes to manage
  ✅ Better for memory-intensive workloads (large JVM)
  ❌ Larger blast radius on node failure
  ❌ Harder to schedule small pods efficiently

Best practice:
  Use Karpenter with diverse instance types.
  Let Karpenter consolidate: it picks optimal instance per workload.
```

---

# Topic 7: vCluster (Virtual Clusters)

```text
vCluster: a full K8s control plane (API server, etcd, scheduler) running inside a namespace
of a host cluster.

Tenants get:
  - Their own kubectl context
  - Full cluster-admin on their vCluster
  - Can install CRDs without affecting host cluster
  - Isolated API server (separate etcd)

But:
  - Actual pods run on host cluster nodes (shared node pool)
  - vCluster syncs pods down to host namespace

Use case:
  - CI/CD isolated cluster per PR ("preview environments")
  - External customers needing cluster-admin access
  - Testing new K8s versions
  - Developer sandboxes with self-service
```

---

# Topic 8: Multi-Tenancy Governance

## 1. OPA Gatekeeper for Platform Policies

```text
Platform team uses Gatekeeper to enforce:
  - All Deployments must have team label
  - All namespaces must have cost-center annotation
  - No LoadBalancer services (use Ingress)
  - All containers must run as non-root
  - All images must be from approved registry
  - Max replica count per team (prevent runaway scaling)
```

## 2. Admission Policy for Namespace Creation

```text
Tenants cannot create namespaces directly.
Only platform team (via GitOps) can create namespaces.

Enforce via:
  - RBAC: no ClusterRole for namespace creation for teams
  - Gatekeeper: require specific annotations on namespace creation
  - Self-service portal: teams request namespaces via ticketing/GitOps PR
```

---

# Topic 9: Revision Notes

- Soft multi-tenancy: namespace-based isolation; RBAC + ResourceQuota + Network Policy + LimitRange
- Hard multi-tenancy: separate clusters or vCluster; for compliance or untrusted tenants
- HNC (Hierarchical Namespace Controller): namespace trees with inherited policies
- Team template: Namespace + ResourceQuota + LimitRange + RBAC RoleBinding + default-deny NetworkPolicy
- Dedicated node groups: taint per team; prevents noisy neighbor
- Cost attribution: consistent labels (team, cost-center) + Kubecost/OpenCost
- One big cluster vs many: balance cost vs blast radius; Hub-and-spoke is common at MAANG scale
- vCluster: full K8s control plane in a namespace; for dev sandboxes, external customers
- Gatekeeper: enforce cross-namespace policies (required labels, registry allowlist)

## Official Source Notes

- Multi-tenancy: <https://kubernetes.io/docs/concepts/security/multi-tenancy/>
- HNC: <https://github.com/kubernetes-sigs/hierarchical-namespaces>
- vCluster: <https://www.vcluster.com/docs/>
- Kubecost: <https://docs.kubecost.com/>
- Cluster API: <https://cluster-api.sigs.k8s.io/>
