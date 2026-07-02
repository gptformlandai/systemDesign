# Kubernetes EKS Pod Identity, Karpenter v1, and Modern EKS Operations Gold Sheet

> Track: K8s Interview Track - Phase 7: Production Architecture Plus
> Goal: Refresh EKS production design with current identity, Karpenter v1 concepts, EKS Auto Mode awareness, and version lifecycle discipline.

---

## 0. How To Read This

Beginner focus:
- EKS manages the Kubernetes control plane.
- Nodes, add-ons, workloads, networking, and IAM design still matter.
- Pods need safe access to AWS services.

Intermediate focus:
- IRSA and EKS Pod Identity map Kubernetes ServiceAccounts to IAM roles.
- Karpenter provisions nodes based on pending pods.
- EKS versions and add-ons must be upgraded proactively.

Senior / MAANG focus:
- Identity, autoscaling, and upgrades are platform contracts.
- Karpenter v1 centers on NodePools, NodeClasses, NodeClaims, disruption, and drift.
- Avoid stale copy-paste install versions; pin compatible versions and manage upgrades intentionally.

---

# Topic 1: EKS Version Discipline

As of the July 2026 official EKS documentation, standard support versions include `1.36`, `1.35`, `1.34`, and `1.33`. Older versions may be in extended support with additional cost and eventual forced control-plane upgrade.

Operational rule:
```text
Do not hard-code a version from old notes.
Before creating or upgrading a cluster, check current supported versions:
  aws eks describe-cluster-versions
```

Upgrade order:
```text
1. Audit deprecated APIs.
2. Upgrade dev/test control plane.
3. Upgrade add-ons: VPC CNI, CoreDNS, kube-proxy, CSI drivers.
4. Upgrade node groups or rotate Karpenter nodes.
5. Validate workloads and SLOs.
6. Repeat for prod one minor version at a time.
```

---

# Topic 2: IRSA vs EKS Pod Identity

## 1. IRSA

```text
IRSA:
  IAM Roles for Service Accounts.
  Uses cluster OIDC provider and projected service account token.
```

Strengths:
- Mature.
- Works well across many existing clusters.
- Explicit trust relationship per cluster OIDC provider.

Costs:
- Requires OIDC provider setup.
- Cross-cluster role reuse can be more operationally heavy.

## 2. EKS Pod Identity

```text
EKS Pod Identity:
  EKS association maps a Kubernetes ServiceAccount to an IAM role.
  Uses the EKS Pod Identity Agent on nodes.
```

Strengths:
- Simpler operational model than IRSA for many teams.
- No OIDC provider setup for each association path.
- IAM role trust can use `pods.eks.amazonaws.com`.
- Scales credential assumptions through the EKS auth service and node agent.

Considerations:
- Requires the EKS Pod Identity Agent unless EKS Auto Mode handles it.
- Linux EC2 worker nodes are the primary supported target.
- Associations are eventually consistent.
- Containers are not a security boundary; node-level isolation still matters.
- Add the Pod Identity Agent link-local IP to `NO_PROXY` when proxies are used.

## 3. Decision Table

| Situation | Choice |
|---|---|
| Existing IRSA estate working well | Keep IRSA, migrate intentionally |
| New EKS platform with many teams | Prefer EKS Pod Identity where supported |
| Need portability outside EKS | IRSA-like OIDC or external identity provider |
| Fargate or unsupported environment | Check current EKS restrictions before choosing |
| Cross-account access | Use IAM role delegation carefully |

---

# Topic 3: Karpenter v1 Concepts

| Concept | Meaning |
|---|---|
| `NodePool` | Scheduling and disruption policy for a group of possible nodes |
| `EC2NodeClass` | AWS-specific node configuration: AMI, subnets, security groups, role, block devices |
| `NodeClaim` | Concrete node request created by Karpenter |
| Disruption | Expiration, consolidation, drift replacement, interruption handling |
| NodeOverlay | Advanced customization layer in newer Karpenter docs |

Mental model:
```text
Pending pod -> Karpenter sees constraints -> picks instance capacity
-> creates NodeClaim -> EC2 instance joins cluster -> pod schedules
-> disruption logic later consolidates, expires, or replaces node.
```

---

# Topic 4: Karpenter v1 NodePool Example

```yaml
apiVersion: karpenter.sh/v1
kind: NodePool
metadata:
  name: general-purpose
spec:
  template:
    metadata:
      labels:
        workload-tier: general
    spec:
      nodeClassRef:
        group: karpenter.k8s.aws
        kind: EC2NodeClass
        name: default
      requirements:
        - key: kubernetes.io/arch
          operator: In
          values: ["amd64", "arm64"]
        - key: karpenter.sh/capacity-type
          operator: In
          values: ["spot", "on-demand"]
        - key: topology.kubernetes.io/zone
          operator: In
          values: ["us-east-1a", "us-east-1b", "us-east-1c"]
      expireAfter: 720h
  disruption:
    consolidationPolicy: WhenEmptyOrUnderutilized
    consolidateAfter: 5m
  limits:
    cpu: "1000"
    memory: 4000Gi
---
apiVersion: karpenter.k8s.aws/v1
kind: EC2NodeClass
metadata:
  name: default
spec:
  amiFamily: AL2023
  role: karpenter-node-role
  subnetSelectorTerms:
    - tags:
        karpenter.sh/discovery: prod-cluster
  securityGroupSelectorTerms:
    - tags:
        karpenter.sh/discovery: prod-cluster
  blockDeviceMappings:
    - deviceName: /dev/xvda
      ebs:
        volumeSize: 100Gi
        volumeType: gp3
        encrypted: true
```

Install guidance:
```text
Use the Karpenter compatibility matrix and pin a current compatible v1.x chart.
Do not copy old v0.x install commands from notes.
```

---

# Topic 5: NodePool Design

Recommended split:
```text
system-on-demand:
  CoreDNS, CNI, observability, ingress controllers. On-demand only.

general-spot:
  Stateless app workloads. Spot + fallback strategy.

critical-on-demand:
  Payment, auth, low-latency APIs. Tainted and protected by PDBs.

gpu-accelerated:
  ML and inference workloads. Quota and expensive-capacity controls.
```

Guardrails:
- Keep system add-ons away from highly volatile spot pools.
- Use topology spread across AZs.
- Use PDBs for workloads Karpenter may disrupt.
- Set budgets and disruption policies.
- Monitor consolidation events and node churn.
- Use ODCRs or Capacity Blocks for reserved high-value capacity when needed.

---

# Topic 6: EKS Auto Mode Awareness

```text
EKS Auto Mode:
  AWS-managed approach that can automate parts of cluster infrastructure,
  including compute and add-on operations depending on configuration.
```

Interview stance:
```text
I would evaluate Auto Mode when the organization wants less platform ownership.
I would still design namespaces, RBAC, network policy, workload identity,
observability, cost allocation, GitOps, and application SLOs. Managed automation
does not remove architecture responsibility.
```

---

# Topic 7: EKS Production Checklist

```text
Identity:
  One ServiceAccount per workload.
  EKS Pod Identity or IRSA, no static AWS keys in Secrets.

Networking:
  Private nodes, restricted API endpoint, VPC CNI prefix delegation as needed.
  Security Groups for Pods only for workloads that need SG-level isolation.

Compute:
  Karpenter NodePools by workload class.
  Managed Node Group or static capacity for critical system add-ons if desired.

Storage:
  EBS CSI, EFS CSI where RWX is needed, snapshots tested by restore.

Security:
  PSA, RBAC, NetworkPolicy, admission policy, image signing.

Observability:
  Control plane logs, workload RED metrics, node USE metrics, SLO alerts.

Upgrades:
  One minor version at a time, add-ons and nodes included, no stale versions.
```

---

# Topic 8: Interview Scenario

> You are building a new multi-team EKS platform. Would you use IRSA, EKS Pod Identity, Cluster Autoscaler, or Karpenter?

Strong answer:
```text
For workload identity, I would choose EKS Pod Identity for new Linux EC2-based
EKS clusters if it is supported by our constraints, because it simplifies
associations and avoids per-cluster OIDC provider operations. I would keep IRSA
where the existing estate already relies on it or where Pod Identity restrictions
do not fit. For node scaling, I would use Karpenter v1 with separated NodePools:
system on-demand, critical on-demand, general spot, and GPU-specific pools.
I would pin compatible chart versions, monitor NodeClaims and disruption, and
protect critical workloads with PDBs and topology spread.
```

---

# Topic 9: Revision Notes

- EKS versions move quickly; check supported versions before cluster creation or upgrade.
- EKS Pod Identity is simpler than IRSA for many new EKS workloads, but has restrictions.
- Karpenter v1 concepts: NodePool, EC2NodeClass, NodeClaim, disruption, drift, consolidation.
- Split NodePools by workload criticality and capacity economics.
- Auto Mode can reduce infrastructure ownership but does not replace platform design.

## Official Source Notes

- EKS Kubernetes versions: <https://docs.aws.amazon.com/eks/latest/userguide/kubernetes-versions.html>
- EKS Pod Identity: <https://docs.aws.amazon.com/eks/latest/userguide/pod-identities.html>
- Karpenter docs: <https://karpenter.sh/docs/>
- EKS Best Practices: <https://aws.github.io/aws-eks-best-practices/>

