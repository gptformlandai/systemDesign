# Kubernetes Hands-On Production Capstone Labs Gold Sheet

> Track: K8s Interview Track - Phase 7/8 Bridge
> Goal: Convert Kubernetes theory into runnable muscle memory with progressive labs, broken-cluster drills, and a production-style capstone.

---

## 0. How To Read This

Beginner focus:
- Practice creating and debugging core objects.
- Use `kubectl describe`, `logs`, `events`, and YAML edits daily.

Intermediate focus:
- Build a full app path: Deployment, Service, Ingress/Gateway, ConfigMap, Secret, PVC, HPA, NetworkPolicy.
- Break it intentionally and recover.

Senior / MAANG focus:
- Prove production readiness: GitOps, policy, observability, upgrade plan, backup/restore, SLOs, cost controls.
- Practice explaining every trade-off like an interview.

---

# Lab 0: Local Cluster Setup

Choose one:
```bash
# kind
kind create cluster --name k8s-mastery

# minikube
minikube start --cpus=4 --memory=8192

# k3d
k3d cluster create k8s-mastery --agents 2
```

Install basics:
```bash
kubectl get nodes
kubectl create namespace prod
kubectl create namespace staging
kubectl label namespace prod policy-tier=prod
```

Deliverable:
```text
Screenshot or saved output:
  kubectl get nodes -o wide
  kubectl get ns --show-labels
```

---

# Lab 1: Deploy and Debug a Broken App

Create:
- Deployment with 3 replicas.
- ClusterIP Service.
- ConfigMap for `LOG_LEVEL`.
- Secret for `DB_PASSWORD`.
- Readiness and liveness probes.

Break intentionally:
```text
1. Wrong image tag -> ImagePullBackOff.
2. Bad command -> CrashLoopBackOff.
3. Wrong Service selector -> zero endpoints.
4. Readiness path wrong -> pod Running but no traffic.
5. CPU request too high -> Pending.
```

Debug runbook:
```bash
kubectl get pods -n prod
kubectl describe pod <pod> -n prod
kubectl logs <pod> -n prod --previous
kubectl get events -n prod --sort-by=.lastTimestamp
kubectl get endpointslices -n prod
```

Pass condition:
```text
Explain root cause and fix for all five failures without notes.
```

---

# Lab 2: Zero-Trust Namespace

Build:
- Default deny ingress and egress NetworkPolicy.
- Allow frontend -> backend.
- Allow backend -> database.
- Allow DNS egress to kube-dns.

Test:
```bash
kubectl run curl -n prod --rm -it --image=curlimages/curl -- sh
```

Pass condition:
```text
Only intended service paths work.
Random pod-to-pod traffic fails.
DNS still works.
```

Interview explanation:
```text
NetworkPolicy is allow-list based once a pod is selected by a policy.
Default-deny creates the baseline; explicit allow rules restore intended paths.
```

---

# Lab 3: Storage and Stateful Recovery

Build:
- StatefulSet with `volumeClaimTemplates`.
- StorageClass using `WaitForFirstConsumer` where supported.
- PVC expansion if local driver supports it.
- VolumeSnapshot if snapshot CRDs and driver are installed.

Failure drills:
```text
1. Delete a pod and verify it reattaches the same PVC.
2. Scale StatefulSet down and up.
3. Simulate bad migration and restore from snapshot where supported.
4. Explain RWO vs RWX and when EFS/NFS is needed.
```

Pass condition:
```text
You can explain what happens to PVCs when the StatefulSet is deleted, scaled,
or rescheduled.
```

---

# Lab 4: Admission and Security Guardrails

Build:
- Pod Security Admission labels on namespace.
- ValidatingAdmissionPolicy to require resources or block `latest`.
- RBAC Role and RoleBinding for a developer group.
- ServiceAccount per app.

Test:
```bash
kubectl auth can-i create pods -n prod --as=user:developer
kubectl auth can-i create clusterrolebindings --as=user:developer
kubectl apply --server-side --dry-run=server -f bad-pod.yaml
```

Pass condition:
```text
Bad workloads are blocked before entering the cluster.
Allowed workloads deploy without cluster-admin permissions.
```

---

# Lab 5: Observability and SLO

Build:
- kube-prometheus-stack or lightweight Prometheus.
- ServiceMonitor for one app.
- PrometheusRule for availability or restart alert.
- Grafana dashboard with RED metrics.
- OpenTelemetry Collector if tracing is in scope.

Drill:
```text
1. Introduce a 500 error rate.
2. Watch error-rate alert trigger.
3. Follow logs and traces to root cause.
4. Roll back or patch.
5. Write a five-line postmortem.
```

Pass condition:
```text
Alert points to user impact, not only CPU or restart symptoms.
```

---

# Lab 6: GitOps Release Flow

Build:
- App manifests in Git.
- Kustomize overlays: dev, staging, prod.
- ArgoCD Application.
- Image tag promotion by PR.
- Rollback by reverting Git.

Pass condition:
```text
No manual kubectl apply for production app changes.
Cluster state converges from Git.
Rollback path is documented and tested.
```

---

# Lab 7: Upgrade and DR Tabletop

Tabletop inputs:
```text
Cluster version:
  Current - 2 minor versions.

Workloads:
  25 stateless services, 3 StatefulSets, 2 external databases.

Constraints:
  99.9% availability, 15-minute RTO for stateless, 1-hour RTO for stateful.
```

Deliver:
- Deprecated API audit plan.
- Upgrade order.
- PDB and drain strategy.
- Add-on upgrade plan.
- Node rotation plan.
- Backup and restore test.
- Rollback or rebuild strategy.

Pass condition:
```text
You can answer "what if the upgrade fails halfway?" with a concrete mitigation.
```

---

# Final Capstone: Production Platform Slice

Build a small but complete platform slice:

```text
Namespaces:
  platform, prod, staging

Workloads:
  frontend, api, worker, postgres or redis

Delivery:
  Helm or Kustomize, ArgoCD, image tags by environment

Security:
  RBAC, ServiceAccounts, NetworkPolicy, PSA, admission policy

Operations:
  Probes, PDBs, HPA/KEDA where useful, logs, metrics, SLO alert

Storage:
  PVC, snapshot/restore explanation, backup plan

Architecture:
  Upgrade plan, DR plan, cost notes, failure modes
```

Scoring:
| Area | Points |
|---|---|
| Correct manifests | 20 |
| Debuggability | 15 |
| Security guardrails | 20 |
| Observability and SLO | 15 |
| Production operations | 20 |
| Communication | 10 |

Strong pass:
```text
You can rebuild the environment from Git, intentionally break three things,
debug them in under 20 minutes, and explain trade-offs clearly.
```

---

# Revision Notes

- Theory is not mastery until you can debug it under time pressure.
- Every major concept should have a broken-state drill.
- Production readiness means deploy, secure, observe, recover, and explain.
- Keep a personal runbook of commands that solved real lab failures.

## Official Source Notes

- kubectl debug: <https://kubernetes.io/docs/tasks/debug/debug-application/debug-running-pod/>
- NetworkPolicy: <https://kubernetes.io/docs/concepts/services-networking/network-policies/>
- StatefulSets: <https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/>
- Argo CD: <https://argo-cd.readthedocs.io/>
- Prometheus Operator: <https://prometheus-operator.dev/>

