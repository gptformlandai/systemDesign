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
│   ├── K8s-Networking-Services-Ingress-DNS-Gold-Sheet.md
│   └── K8s-Native-Sidecars-Ephemeral-Debug-Containers-Gold-Sheet.md
│
├── 02-Configuration-Storage/
│   ├── K8s-ConfigMaps-Secrets-Environment-Gold-Sheet.md
│   ├── K8s-Storage-Volumes-PVC-StorageClass-Gold-Sheet.md
│   └── K8s-Advanced-CSI-VolumeAttributesClass-Storage-Operations-Gold-Sheet.md
│
├── 03-Scheduling-Resource-Management/
│   ├── K8s-Resource-Requests-Limits-QoS-Gold-Sheet.md
│   ├── K8s-Scheduling-Affinity-Taints-Tolerations-Gold-Sheet.md
│   ├── K8s-HPA-VPA-KEDA-Autoscaling-Gold-Sheet.md
│   └── K8s-Dynamic-Resource-Allocation-GPU-Accelerators-Gold-Sheet.md
│
├── 04-Networking-Security/
│   ├── K8s-Network-Policies-CNI-Gold-Sheet.md
│   ├── K8s-RBAC-ServiceAccounts-Security-Gold-Sheet.md
│   ├── K8s-Pod-Security-Standards-OPA-Gatekeeper-Gold-Sheet.md
│   └── K8s-ValidatingAdmissionPolicy-CEL-Admission-Control-Gold-Sheet.md
│
├── 05-Observability-Operations/
│   ├── K8s-Logging-Metrics-Prometheus-Grafana-Gold-Sheet.md
│   ├── K8s-Health-Probes-Lifecycle-Hooks-Gold-Sheet.md
│   ├── K8s-Kubectl-Troubleshooting-Playbook-Gold-Sheet.md
│   └── K8s-OpenTelemetry-SLOs-Production-Observability-Gold-Sheet.md
│
├── 06-Advanced-Patterns/
│   ├── K8s-Helm-Kustomize-GitOps-Gold-Sheet.md
│   ├── K8s-Operators-CRDs-Custom-Controllers-Gold-Sheet.md
│   ├── K8s-Service-Mesh-Istio-Linkerd-Gold-Sheet.md
│   ├── K8s-Multi-Tenancy-Cluster-Design-Gold-Sheet.md
│   └── K8s-API-Machinery-APF-SSA-Audit-Storage-Version-Gold-Sheet.md
│
├── 07-Production-Architecture/
│   ├── K8s-EKS-Production-Architecture-Gold-Sheet.md
│   ├── K8s-EKS-Pod-Identity-Karpenter-v1-Modern-Operations-Gold-Sheet.md
│   ├── K8s-Cluster-Upgrades-Backup-DR-Gold-Sheet.md
│   ├── K8s-CICD-ArgoCD-Image-Security-Gold-Sheet.md
│   └── K8s-Hands-On-Production-Capstone-Labs-Gold-Sheet.md
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

### Phase 1: Foundations (Days 1-4)
1. Core Concepts — understand what every K8s object is
2. Workloads — deploy and manage applications
3. Networking — connect services inside and outside the cluster
4. Native Sidecars and Ephemeral Debug Containers — modern pod lifecycle and debugging

### Phase 2: Configuration and Storage (Days 5-7)
5. ConfigMaps and Secrets — inject config and credentials
6. Storage — persistent data for stateful workloads
7. Advanced CSI and VolumeAttributesClass — snapshots, clones, expansion, mutable attributes

### Phase 3: Scheduling and Resources (Days 8-11)
8. Resource Requests and Limits — prevent noisy neighbors
9. Scheduling — control where pods land
10. Autoscaling — HPA, VPA, KEDA
11. DRA and GPU Scheduling — accelerators, ResourceClaims, expensive capacity

### Phase 4: Security (Days 12-15)
12. Network Policies — East-West traffic control
13. RBAC — who can do what
14. Pod Security — prevent privilege escalation
15. ValidatingAdmissionPolicy and CEL — built-in admission guardrails

### Phase 5: Observability and Operations (Days 16-19)
16. Logging and Metrics — Prometheus, Grafana, Loki
17. Health Probes — liveness, readiness, startup
18. kubectl Troubleshooting — debug any issue systematically
19. OpenTelemetry and SLOs — production-grade telemetry and burn-rate alerting

### Phase 6: Advanced Patterns (Days 20-24)
20. Helm and GitOps — package and deploy
21. Operators and CRDs — extend Kubernetes
22. Service Mesh — mTLS, traffic management
23. Multi-Tenancy — multi-team cluster design
24. API Machinery — APF, SSA, audit, storage versions, aggregation

### Phase 7: Production Architecture (Days 25-29)
25. EKS Production — managed control plane, VPC CNI, identity, HA
26. Modern EKS Operations — EKS Pod Identity, Karpenter v1, Auto Mode awareness
27. Cluster Upgrades and DR — zero-downtime upgrades, Velero
28. CI/CD with ArgoCD — GitOps delivery pipeline
29. Hands-On Capstone Labs — build, break, debug, secure, observe, recover

### Phase 8: Practice (Days 30-35)
30. Active Recall: expanded question bank
31. Scenario Drill Bank: production scenarios
32. Mock Interview Scripts: timed rounds
33. Scoring Rubrics + Study Roadmaps
34. Re-run Capstone: fix weak areas
35. Final Staff-level mock: design + debug + security + operations

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
| 16 | ValidatingAdmissionPolicy and CEL | Admission Control |
| 17 | Native sidecars and ephemeral debug containers | Native Sidecars |
| 18 | OpenTelemetry, SLOs, cardinality | Production Observability |
| 19 | Karpenter v1 and EKS Pod Identity | Modern EKS Operations |
| 20 | API machinery: APF, SSA, audit, storage versions | API Machinery |

---

## MAANG Interview Depth Levels

| Level | What They Expect |
|---|---|
| L4 (Junior SRE/Platform) | Pod, Deployment, Service, ConfigMap, basic kubectl, HPA |
| L5 (Mid SRE/Platform) | RBAC, Network Policies, PVC, Helm, Probes, Scheduling |
| L6 (Senior) | Operators, Service Mesh, Multi-tenancy, EKS Karpenter, GitOps, SLOs |
| L7 (Staff) | Custom controllers, API machinery, cluster design for 100s of teams, security hardening, upgrade/DR strategy |

---

## Current Source Anchors

- Kubernetes docs current version family: <https://kubernetes.io/docs/home/>
- Kubernetes version skew policy: <https://kubernetes.io/releases/version-skew-policy/>
- ValidatingAdmissionPolicy: <https://kubernetes.io/docs/reference/access-authn-authz/validating-admission-policy/>
- Dynamic Resource Allocation: <https://kubernetes.io/docs/concepts/scheduling-eviction/dynamic-resource-allocation/>
- VolumeAttributesClass: <https://kubernetes.io/docs/concepts/storage/volume-attributes-classes/>
- Native sidecars: <https://kubernetes.io/docs/concepts/workloads/pods/sidecar-containers/>
- EKS version lifecycle: <https://docs.aws.amazon.com/eks/latest/userguide/kubernetes-versions.html>
- EKS Pod Identity: <https://docs.aws.amazon.com/eks/latest/userguide/pod-identities.html>
- Karpenter: <https://karpenter.sh/docs/>

---

## Related Tracks In This Workspace

- AWS Track: `AWS/` — EKS deep dive in `07-Senior-Architecture/`
- Kafka Track: `Kafka/` — messaging for event-driven K8s workloads
- GitHub Actions Track: `GithubActions/` — CI pipeline that deploys to K8s
