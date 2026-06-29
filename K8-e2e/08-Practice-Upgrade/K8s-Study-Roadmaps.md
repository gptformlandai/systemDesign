# Kubernetes Study Roadmaps

> Track: K8s Interview Track — Phase 8: Practice Upgrade
> Choose the roadmap that matches your timeline and target role.

---

## Roadmap A: 2-Week Interview Intensive

*Assumes 2-3 hours per day. Target: L5 platform/SRE interview in 2 weeks.*

### Week 1: Foundations to Security

| Day | Focus | Gold Sheet(s) | Active Recall Goal |
|---|---|---|---|
| Day 1 | Core Concepts — Architecture | K8s-Core-Concepts-Pods-Nodes-Clusters | Explain etcd, scheduler, controller-manager without notes |
| Day 2 | Workloads | K8s-Workloads-Deployments-StatefulSets-Jobs | Write a Deployment + StatefulSet YAML from memory |
| Day 3 | Networking Services | K8s-Networking-Services-Ingress-DNS | Trace a request from pod A → Service → pod B end-to-end |
| Day 4 | ConfigMaps + Secrets | K8s-ConfigMaps-Secrets-Environment | Explain ESO vs CSI Secrets driver from memory |
| Day 5 | Storage | K8s-Storage-Volumes-PVC-StorageClass | Explain WaitForFirstConsumer and when it matters |
| Day 6 | Resources + QoS | K8s-Resource-Requests-Limits-QoS | Draw OOM kill sequence and QoS eviction order |
| Day 7 | Review + Q&A drill | Question Bank Q1–Q36 | Target 85% accuracy on Section 1-3 |

### Week 2: Security, Observability, Production

| Day | Focus | Gold Sheet(s) | Active Recall Goal |
|---|---|---|---|
| Day 8 | Scheduling + Autoscaling | K8s-Scheduling-Affinity + K8s-HPA-VPA-KEDA | Write nodeAffinity + TopologySpreadConstraints YAML |
| Day 9 | Network Policies + RBAC | K8s-Network-Policies-CNI + K8s-RBAC | Write default-deny + payment-service NetworkPolicy from memory |
| Day 10 | Pod Security + OPA | K8s-Pod-Security-Standards-OPA-Gatekeeper | Explain PSA enforcement + Kyverno image signing check |
| Day 11 | Observability | K8s-Logging-Metrics + K8s-Health-Probes | Write Prometheus alert rule for CrashLooping pod |
| Day 12 | GitOps + Production | K8s-Helm-Kustomize-GitOps + K8s-EKS-Production | Design 3-environment promotion pipeline verbally |
| Day 13 | Scenario Drills | Scenario Drill Bank (all 10) | Complete Scenarios 1-5 in writing (timed 15 min each) |
| Day 14 | Mock Interview | Mock Interview Scripts Round 1-3 | Record yourself, self-grade with rubric |

---

## Roadmap B: 4-Week Comprehensive Mastery

*Assumes 1-1.5 hours per day. Target: L6 Senior / Staff platform engineering role.*

### Week 1: Core Platform Fundamentals

| Day | Topic | Depth Target |
|---|---|---|
| Day 1-2 | K8s architecture — control plane deep dive | Know etcd quorum, leader election, API server admission chain |
| Day 3-4 | Pod lifecycle — from YAML to running container | Watch Events in real time, explain each phase |
| Day 5 | Workloads comparison — when to use each | Deployment vs StatefulSet vs DaemonSet vs Job decision tree |
| Day 6-7 | Networking internals — packet tracing | Trace request end-to-end: pod → Service → endpoint → pod |

### Week 2: Storage, Scheduling, Resource Management

| Day | Topic | Depth Target |
|---|---|---|
| Day 8-9 | Storage deep dive — CSI, StorageClass, access modes | Write StatefulSet with volumeClaimTemplates YAML |
| Day 10 | Resource management — requests/limits, QoS | Explain all three QoS classes and eviction order |
| Day 11-12 | Scheduling — affinity, taints, topology spread | Write pod with nodeAffinity + TopologySpreadConstraints |
| Day 13-14 | Autoscaling — HPA, VPA, KEDA, Karpenter | Design KEDA ScaledObject for SQS queue |

### Week 3: Security and Observability

| Day | Topic | Depth Target |
|---|---|---|
| Day 15-16 | RBAC + ServiceAccounts + IRSA | Design RBAC model for 5-team cluster (least privilege) |
| Day 17 | Network Policies | Write default-deny + layered allow policies |
| Day 18-19 | Pod Security Standards + OPA/Gatekeeper | Write ConstraintTemplate for registry allowlist |
| Day 20 | Observability — metrics, logging, tracing | Design kube-prometheus-stack architecture |
| Day 21 | Health probes + lifecycle hooks | Explain all 5 probe types, write zero-downtime config |

### Week 4: Advanced and Production Patterns

| Day | Topic | Depth Target |
|---|---|---|
| Day 22 | GitOps — ArgoCD + Kustomize + Helm | Design two-repo GitOps pipeline |
| Day 23 | Operators + CRDs | Explain controller reconciliation loop, design a CRD |
| Day 24 | Service Mesh — Istio | Write VirtualService canary + PeerAuthentication mTLS |
| Day 25 | Multi-tenancy + EKS production architecture | Design hub/spoke cluster model |
| Day 26-27 | CI/CD pipeline + supply chain security | Design full pipeline with Cosign + Trivy + SBOM |
| Day 28 | Full mock interview (all 5 rounds) | Score with rubric; identify weakest 2 dimensions |

---

## Roadmap C: CKA Exam Preparation Track

*Certified Kubernetes Administrator exam: 2 hours, hands-on tasks in live clusters.*

### CKA Exam Domains (with weight)

| Domain | Weight | Study Priority |
|---|---|---|
| Cluster Architecture, Installation, and Configuration | 25% | HIGH |
| Workloads and Scheduling | 15% | HIGH |
| Services and Networking | 20% | HIGH |
| Storage | 10% | MEDIUM |
| Troubleshooting | 30% | CRITICAL |

### CKA Critical Skills

**Must practice hands-on (not just theory):**

```bash
# Create resources imperatively (faster in exam):
kubectl create deployment nginx --image=nginx --replicas=3
kubectl expose deployment nginx --port=80 --type=ClusterIP
kubectl create serviceaccount my-sa
kubectl create role pod-reader --verb=get,list --resource=pods
kubectl create rolebinding my-binding --role=pod-reader --serviceaccount=default:my-sa

# JSONPath output (common in exam tasks):
kubectl get pods -o jsonpath='{.items[*].metadata.name}'
kubectl get nodes -o jsonpath='{.items[*].status.addresses[?(@.type=="InternalIP")].address}'

# Context switching (multi-cluster exam):
kubectl config get-contexts
kubectl config use-context cluster1
kubectl config set-context --current --namespace=kube-system

# etcd backup (exam favorite):
ETCDCTL_API=3 etcdctl snapshot save /tmp/etcd-backup.db \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key

# etcd restore:
ETCDCTL_API=3 etcdctl snapshot restore /tmp/etcd-backup.db \
  --data-dir=/var/lib/etcd-restored

# kubeadm upgrade:
kubeadm upgrade plan
kubeadm upgrade apply v1.28.0
# Then: apt-mark unhold kubelet kubectl; apt upgrade kubelet kubectl

# Network policy — default deny all:
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
spec:
  podSelector: {}
  policyTypes: [Ingress, Egress]
EOF
```

### CKA Study Schedule (3 weeks)

| Week | Focus |
|---|---|
| Week 1 | Cluster setup (kubeadm), node management, RBAC, upgrades |
| Week 2 | Workloads, Services, Networking, Storage hands-on |
| Week 3 | Troubleshooting scenarios (CrashLoop, PV issues, scheduling), mock exams |

**Practice environments:**
- killer.sh (official CKA simulator — included with exam purchase)
- KodeKloud free labs
- Local: kind, k3d, or minikube

---

## Roadmap D: CKAD Exam Preparation Track

*Certified Kubernetes Application Developer: application-focused, no cluster admin tasks.*

### CKAD Exam Domains

| Domain | Weight | Study Priority |
|---|---|---|
| Application Design and Build | 20% | HIGH |
| Application Deployment | 20% | HIGH |
| Application Environment, Config, Security | 25% | CRITICAL |
| Application Observability and Maintenance | 15% | HIGH |
| Services and Networking | 20% | HIGH |

### CKAD Critical Skills

```bash
# Multi-container pods (CKAD loves this):
# sidecar container, initContainer, ephemeral containers

# ConfigMap from literal:
kubectl create configmap app-config \
  --from-literal=DB_URL=postgres://localhost:5432/app \
  --from-literal=LOG_LEVEL=info

# Secret from literal:
kubectl create secret generic db-creds \
  --from-literal=username=admin \
  --from-literal=password=s3cur3p4ss

# Resource limits (quick imperative):
kubectl set resources deployment/myapp \
  --requests=cpu=100m,memory=128Mi \
  --limits=cpu=500m,memory=256Mi

# Probes (add to existing deployment):
kubectl set probe deployment/myapp \
  --readiness --get-url=http://:8080/health \
  --initial-delay-seconds=5 --period-seconds=10

# Horizontal pod autoscaling:
kubectl autoscale deployment myapp --cpu-percent=70 --min=2 --max=10

# NetworkPolicy — allow only specific pod:
# Write from YAML (imperative creation not supported for NetworkPolicy)

# Jobs and CronJobs:
kubectl create job data-migration --image=migrate-tool -- /migrate.sh
kubectl create cronjob cleanup --schedule="0 2 * * *" \
  --image=cleanup-tool -- /cleanup.sh

# Rollout management:
kubectl rollout history deployment/myapp
kubectl rollout undo deployment/myapp --to-revision=2
kubectl rollout pause deployment/myapp
kubectl rollout resume deployment/myapp
```

### CKAD Study Schedule (2 weeks)

| Week | Focus |
|---|---|
| Week 1 | Pod design (multi-container, init), config injection, security contexts |
| Week 2 | Deployments, Services, probes, NetworkPolicy, Jobs/CronJobs |

---

## Topic Priority Matrix by Role

| Topic | SRE | Platform Engineer | DevOps | Backend Dev | Score |
|---|---|---|---|---|---|
| Pod Lifecycle and Control Plane | High | High | Medium | Low | **Core** |
| Workloads (Deployment, StatefulSet) | High | High | High | High | **Core** |
| Networking and Services | High | High | High | Medium | **Core** |
| RBAC and Security | High | High | Medium | Low | **Core** |
| Storage and PVC | High | Medium | Medium | Low | Medium |
| HPA, VPA, KEDA | High | High | Medium | Low | Medium |
| Scheduling and Affinity | Medium | High | Low | Low | Medium |
| Observability (Prometheus, Grafana) | **Critical** | High | Medium | Low | High |
| Troubleshooting | **Critical** | High | High | Medium | **Core** |
| GitOps (ArgoCD, Kustomize) | Medium | **Critical** | **Critical** | Low | High |
| Helm | Medium | **Critical** | **Critical** | Medium | High |
| Service Mesh (Istio) | High | High | Medium | Low | Medium |
| Operators and CRDs | Low | **Critical** | Low | Low | Advanced |
| EKS Production Architecture | High | **Critical** | High | Low | High |
| Cluster Upgrades and DR | **Critical** | **Critical** | Medium | Low | High |
| Supply Chain Security | Medium | High | High | Low | Medium |

---

## Quick Study Tips

**The night before an interview:**
```text
1. Re-read K8s-Interview-Track-Index.md (30 min)
2. Review Question Bank Sections you struggled with (30 min)
3. Say the 5-minute debug framework out loud (5 min)
4. Do NOT try to learn new topics — consolidate what you know
5. Sleep 8 hours
```

**High-yield topics (covers 80% of questions):**
```text
1. Deployment rolling update + zero-downtime config
2. StatefulSet vs Deployment: when and why
3. Service types and DNS resolution
4. RBAC: Role vs ClusterRole vs bindings (+ common mistake: ClusterRoleBinding vs RoleBinding)
5. HPA formula + KEDA for event-driven
6. Liveness vs Readiness vs Startup probe
7. Pod QoS and eviction order
8. 5-step pod debug sequence (describe → logs --previous → exec → endpoints → network)
9. NetworkPolicy: default deny + allow pattern
10. GitOps pull model: why it's better than push
```

**Phrases that impress interviewers:**
```text
✓ "I'd rollback immediately, then investigate — availability first"
✓ "The control loop ensures eventual consistency..."
✓ "preStop sleep is needed because iptables propagation isn't instant"
✓ "StatefulSets don't delete PVCs to prevent accidental data loss"
✓ "I'd use WaitForFirstConsumer to avoid cross-AZ EBS mounts"
✓ "This works for 100 services, but at 1000 we'd hit iptables scale limits"
✓ "I'd start with a PodDisruptionBudget to protect SLA during this operation"
```
