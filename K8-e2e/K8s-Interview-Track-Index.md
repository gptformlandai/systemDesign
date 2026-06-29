# Kubernetes (K8s) Interview Track — Master Index

> Goal: Beginner to Production-Expert mastery. Covers every concept tested in MAANG Kubernetes interviews, SRE/Platform Engineering roles, and CKA/CKAD certification prep.
> Format: Each Gold Sheet = intuition-first, production reasoning, interview traps, scenario answers, revision notes, official source links.

---

## Track Structure

```
K8-e2e/
├── K8s-Interview-Track-Index.md                ← You are here
│
├── 01-Foundations/
│   ├── K8s-Core-Concepts-Pods-Nodes-Clusters-Gold-Sheet.md
│   ├── K8s-Workloads-Deployments-StatefulSets-Jobs-Gold-Sheet.md
│   └── K8s-Networking-Services-Ingress-DNS-Gold-Sheet.md
│
├── 02-Configuration-Storage/
│   ├── K8s-ConfigMaps-Secrets-Environment-Gold-Sheet.md
│   └── K8s-Storage-Volumes-PVC-StorageClass-Gold-Sheet.md
│
├── 03-Scheduling-Resource-Management/
│   ├── K8s-Resource-Requests-Limits-QoS-Gold-Sheet.md
│   ├── K8s-Scheduling-Affinity-Taints-Tolerations-Gold-Sheet.md
│   └── K8s-HPA-VPA-KEDA-Autoscaling-Gold-Sheet.md
│
├── 04-Networking-Security/
│   ├── K8s-Network-Policies-CNI-Gold-Sheet.md
│   ├── K8s-RBAC-ServiceAccounts-Security-Gold-Sheet.md
│   └── K8s-Pod-Security-Standards-OPA-Gatekeeper-Gold-Sheet.md
│
├── 05-Observability-Operations/
│   ├── K8s-Logging-Metrics-Prometheus-Grafana-Gold-Sheet.md
│   ├── K8s-Health-Probes-Lifecycle-Hooks-Gold-Sheet.md
│   └── K8s-Kubectl-Troubleshooting-Playbook-Gold-Sheet.md
│
├── 06-Advanced-Patterns/
│   ├── K8s-Helm-Kustomize-GitOps-Gold-Sheet.md
│   ├── K8s-Operators-CRDs-Custom-Controllers-Gold-Sheet.md
│   ├── K8s-Service-Mesh-Istio-Linkerd-Gold-Sheet.md
│   └── K8s-Multi-Tenancy-Cluster-Design-Gold-Sheet.md
│
├── 07-Production-Architecture/
│   ├── K8s-EKS-Production-Architecture-Gold-Sheet.md
│   ├── K8s-Cluster-Upgrades-Backup-DR-Gold-Sheet.md
│   └── K8s-CICD-ArgoCD-Image-Security-Gold-Sheet.md
│
└── 08-Practice-Upgrade/
    ├── K8s-Active-Recall-Question-Bank.md
    ├── K8s-Scenario-Drill-Bank.md
    ├── K8s-Mock-Interview-Scripts.md
    ├── K8s-Interview-Scoring-Rubrics.md
    └── K8s-Study-Roadmaps.md
```

---

## Reading Order (Beginner → Expert)

### Phase 1: Foundations (Days 1-3)
1. Core Concepts — understand what every K8s object is
2. Workloads — deploy and manage applications
3. Networking — connect services inside and outside the cluster

### Phase 2: Configuration and Storage (Days 4-5)
4. ConfigMaps and Secrets — inject config and credentials
5. Storage — persistent data for stateful workloads

### Phase 3: Scheduling and Resources (Days 6-8)
6. Resource Requests and Limits — prevent noisy neighbors
7. Scheduling — control where pods land
8. Autoscaling — HPA, VPA, KEDA

### Phase 4: Security (Days 9-11)
9. Network Policies — East-West traffic control
10. RBAC — who can do what
11. Pod Security — prevent privilege escalation

### Phase 5: Observability and Operations (Days 12-14)
12. Logging and Metrics — Prometheus, Grafana, Loki
13. Health Probes — liveness, readiness, startup
14. kubectl Troubleshooting — debug any issue systematically

### Phase 6: Advanced Patterns (Days 15-18)
15. Helm and GitOps — package and deploy
16. Operators and CRDs — extend Kubernetes
17. Service Mesh — mTLS, traffic management
18. Multi-Tenancy — multi-team cluster design

### Phase 7: Production Architecture (Days 19-21)
19. EKS Production — Karpenter, IRSA, managed node groups
20. Cluster Upgrades and DR — zero-downtime upgrades, Velero
21. CI/CD with ArgoCD — GitOps delivery pipeline

### Phase 8: Practice (Days 22-25)
22. Active Recall: 80 questions
23. Scenario Drill Bank: 10 production scenarios
24. Mock Interview Scripts: 5 timed rounds
25. Scoring Rubrics + Study Roadmaps

---

## Quick Reference: Most-Tested K8s Topics At MAANG

| Rank | Topic | Gold Sheet |
|---|---|---|
| 1 | Pod lifecycle, restarts, crash loops | Core Concepts |
| 2 | Services and Ingress routing | Networking |
| 3 | Resource limits, OOMKilled, eviction | Resource Management |
| 4 | HPA and KEDA autoscaling | Autoscaling |
| 5 | RBAC and ServiceAccount design | RBAC Security |
| 6 | Liveness/readiness probes | Health Probes |
| 7 | Helm vs Kustomize decision | Helm + GitOps |
| 8 | PVC, PV, StorageClass | Storage |
| 9 | Affinity, taints, tolerations | Scheduling |
| 10 | Network Policies (zero-trust) | Network Policies |
| 11 | ArgoCD GitOps pipeline | CI/CD |
| 12 | Operators and CRDs | Operators |
| 13 | Cluster upgrade strategy | Cluster Operations |
| 14 | Pod Security Standards | Pod Security |
| 15 | Multi-tenancy patterns | Cluster Design |

---

## MAANG Interview Depth Levels

| Level | What They Expect |
|---|---|
| L4 (Junior SRE/Platform) | Pod, Deployment, Service, ConfigMap, basic kubectl, HPA |
| L5 (Mid SRE/Platform) | RBAC, Network Policies, PVC, Helm, Probes, Scheduling |
| L6 (Senior) | Operators, Service Mesh, Multi-tenancy, EKS Karpenter, GitOps |
| L7 (Staff) | Custom controllers, cluster design for 100s of teams, security hardening |

---

## Related Tracks In This Workspace

- AWS Track: `AWS/` — EKS deep dive in `07-Senior-Architecture/`
- Kafka Track: `Kafka/` — messaging for event-driven K8s workloads
- GitHub Actions Track: `GithubActions/` — CI pipeline that deploys to K8s
