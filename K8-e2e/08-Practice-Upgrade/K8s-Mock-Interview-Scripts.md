# Kubernetes Mock Interview Scripts

> Track: K8s Interview Track — Phase 8: Practice Upgrade
> How to use: Record yourself answering. Play back and grade against the model answer.
> Have a friend act as interviewer, or use these as self-timed Socratic drills.

---

## Round 1: Foundations + Workloads (30 minutes)

**Setup:** Timer running. Interviewer opens with small talk then pivots to technical.

---

**Q1: Start simple — what is a Kubernetes Pod and why does it matter?**

*(3 min) — Ideal answer structure:*
1. Smallest unit — contains 1+ containers, shared network namespace (same IP), can share volumes
2. Explain WHY: not a single container — enables sidecar pattern (envoy proxy, log agent)
3. Container-to-container communication inside pod: via localhost
4. Pod is ephemeral — gets a new IP each restart

Red flags: saying Pod = container, not mentioning shared network

---

**Q2: Walk me through the Kubernetes control plane. How does a pod get created?**

*(5 min) — Ideal answer structure:*
1. kubectl → API Server (auth, validation, persists to etcd)
2. Scheduler watches: pod has no nodeName → assigns node based on resources, affinity, taints
3. kubelet on target node watches: pulls image, starts container, reports status
4. Controller manager: ReplicaSet controller sees 1 less replica, creates new pod if needed
5. Mention: this is the control loop / reconciliation pattern

Bonus: mention RBAC admission, mutation webhooks in the API server chain

---

**Q3: Your Deployment has 3 replicas. One pod crashes. What happens, step by step?**

*(3 min) — Ideal answer:*
1. kubelet reports pod Failed to API server → etcd updated
2. ReplicaSet controller watches: desired=3, actual=2 → creates new Pod
3. Scheduler assigns new pod to a node
4. kubelet starts new container
5. End-to-end: ~15-30 seconds for replacement (not instant)

---

**Q4: I need a service that persists data and maintains stable identities. Deployment or StatefulSet?**

*(4 min) — Ideal answer:*
- StatefulSet — explain: stable network identity (kafka-0, kafka-1, kafka-2)
- Ordered startup/shutdown (important for clustering protocols)
- Per-pod PVC (each pod gets its own volume, not shared)
- Headless service → DNS: `kafka-0.kafka.prod.svc.cluster.local`
- Example: Kafka, ZooKeeper, PostgreSQL replicas, Elasticsearch nodes

Follow-up likely: "How does StatefulSet handle a pod failure?" → same PVC reattaches to replacement pod

---

**Q5: Explain rolling update and how to tune it to be zero-downtime.**

*(5 min) — Ideal answer:*
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0    # never kill old pod before new pod is ready
    maxSurge: 1          # allow 1 extra pod during update
```
- maxUnavailable: 0 = old pod stays until new pod passes readiness probe
- Add readiness probe (returns 200 only when app is ready)
- Add preStop: sleep 15 (graceful drain window)
- Add terminationGracePeriodSeconds: 60

---

**Q6: How would you do a canary deployment?**

*(5 min) — Ideal answer options:*
Option A (simple, no tools): Two Deployments (v1: 9 replicas, v2: 1 replica) sharing one Service
Option B (Argo Rollouts): Rollout with canary steps, traffic weights, AnalysisTemplate with Prometheus
Option C (Service mesh): Istio VirtualService with weight 95% → stable, 5% → canary

Mention: monitoring during canary (error rate, latency), automatic rollback on threshold breach

---

**Closing Q: What's the difference between `kubectl rollout undo` and re-deploying the previous image tag?**

*(2 min) — Answer:*
Both work but different mechanics. `rollout undo` references the previous ReplicaSet (K8s keeps revision history). Re-deploying sets a new image tag which creates a new revision. Best practice: track image tags in GitOps repo — `rollout undo` is an emergency shortcut but may not match what's in Git.

---

## Round 2: Networking + Security (30 minutes)

---

**Q1: Explain the Kubernetes Service types. When would you use each?**

*(4 min) — Hit all four:*
- ClusterIP: internal only, microservice-to-microservice
- NodePort: external via node IP + port 30000-32767 (dev/testing)
- LoadBalancer: cloud LB (one LB per service — expensive at scale)
- ExternalName: CNAME to external DNS (database outside cluster)
- Bonus: Headless (clusterIP: None) for StatefulSets

---

**Q2: How does a request from pod A reach pod B via a ClusterIP Service?**

*(4 min) — End-to-end:*
1. Pod A sends to Service ClusterIP (virtual IP, e.g., 10.96.0.100)
2. kube-proxy on the node has iptables rules: DNAT 10.96.0.100 → one of the pod IPs
3. Packet routed to the target pod (within node or across nodes via CNI overlay)
4. Response follows reverse path

Mention: iptables = O(n) rules, ipvs = O(1) hash table (better at scale)

---

**Q3: What is a NetworkPolicy and why do you need one? Give me an example.**

*(4 min) — Answer:*
Default K8s: all pods can talk to all pods. NetworkPolicy adds allow-rules (implicit deny).

Example — payment-service can only be reached from orders-service:
```yaml
kind: NetworkPolicy
spec:
  podSelector:
    matchLabels: {app: payment-service}
  ingress:
    - from:
        - podSelector:
            matchLabels: {app: orders-service}
      ports:
        - port: 8080
```

Mention: requires CNI support (Calico/Cilium, not Flannel)

Follow-up: "How do you test a NetworkPolicy?" → kubectl exec + curl, look for Connection refused vs timeout

---

**Q4: Tell me about RBAC in Kubernetes. How would you give a team read-only access to their namespace only?**

*(4 min) — Answer:*
```yaml
# Use ClusterRole "view" (built-in) + namespace-scoped RoleBinding
kubectl create rolebinding team-orders-view \
  --clusterrole=view \
  --group=team-orders \
  --namespace=team-orders
```
NOT ClusterRoleBinding (that would give cluster-wide access)

Common mistake: using ClusterRoleBinding for something that should be namespace-scoped

Verify: `kubectl auth can-i list pods --as-group=team-orders --as=user:dummy -n team-payments`

---

**Q5: What is IRSA on EKS and why is it better than putting AWS credentials in Secrets?**

*(4 min) — Answer:*
IRSA = IAM Roles for Service Accounts
1. Pod's ServiceAccount has annotation: `eks.amazonaws.com/role-arn: arn:aws:iam::123:role/payment-service`
2. AWS SDK in pod requests EKS OIDC token (expires in 15 min)
3. AWS STS exchanges token for temp IAM credentials (via OIDC federation)
4. Pod calls DynamoDB/S3 with temp creds (expire, auto-rotate)

Better because: no static credentials, no Secrets rotation overhead, no credentials in etcd

---

**Q6: Pod Security Standards — what are they and how do you enforce them?**

*(4 min) — Answer:*
Three levels: Privileged (nothing blocked), Baseline (common privilege escalation blocked), Restricted (hardened: non-root, drop all capabilities, seccomp)

Enforce via namespace label:
```yaml
pod-security.kubernetes.io/enforce: restricted
pod-security.kubernetes.io/warn: restricted
```

Restricted requires:
- `runAsNonRoot: true`
- `allowPrivilegeEscalation: false`
- `capabilities: {drop: [ALL]}`
- `seccompProfile: {type: RuntimeDefault}`

Bonus: OPA/Gatekeeper for custom policies (required labels, image registry)

---

**Q7: A developer accidentally deleted a ClusterRoleBinding and now ArgoCD can't sync. How do you recover?**

*(3 min) — Answer:*
1. Check what's missing: `kubectl auth can-i list applications --as=system:serviceaccount:argocd:argocd-server`
2. Re-apply the ClusterRoleBinding from GitOps repo (since it's version-controlled)
3. If GitOps is down: `kubectl apply -f argocd-rbac.yaml` from backup
4. Long-term: ArgoCD's RBAC should be managed by ArgoCD itself (app-of-apps pattern), so it's self-healing

---

## Round 3: Scaling + Scheduling (30 minutes)

---

**Q1: Explain the difference between requests and limits. Which does the scheduler use?**

*(3 min) — Answer:*
Requests: guaranteed resources, used by scheduler for placement decisions
Limits: hard caps enforced at runtime (CPU throttled, memory → OOMKill)

Scheduler: uses requests only. A pod with 500m request goes to a node with 500m available, even if limit is 2000m.

---

**Q2: Explain HPA. How does it calculate scale?**

*(4 min) — Answer:*
```
desiredReplicas = ceil[currentReplicas × (currentMetricValue / desiredMetricValue)]
```

Example: 5 pods at 80% CPU, target 50%:
desiredReplicas = ceil[5 × (80/50)] = ceil[8] = 8 pods

Behavior tuning (prevent flapping):
```yaml
behavior:
  scaleDown:
    stabilizationWindowSeconds: 300  # wait 5 min before scale down
```

HPA needs metrics-server. Custom metrics need Prometheus Adapter or KEDA.

---

**Q3: When would you use KEDA instead of HPA?**

*(4 min) — Answer:*
KEDA: when scaling trigger is external (not CPU/memory):
- SQS queue depth (scale payment processors when queue grows)
- Kafka consumer lag (scale consumers when lag exceeds threshold)
- Cron-based (scale up before 9 AM peak, down after midnight)
- Scale to zero (no pods when queue empty — save cost)

HPA: CPU/memory of existing pods. HPA can't scale to zero.

KEDA manages an HPA under the hood for external metrics.

---

**Q4: Design the node placement for a latency-critical database and a batch job that runs overnight.**

*(5 min) — Answer:*
```yaml
# Database pods: dedicated nodes, no batch jobs
# Node group: db-nodes, tainted: workload=database:NoSchedule
# DB pod tolerates the taint + nodeAffinity: required nodgroup=db
nodeAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
    nodeSelectorTerms:
      - matchExpressions:
          - key: nodegroup
            operator: In
            values: [db-nodes]
tolerations:
  - key: workload
    value: database
    effect: NoSchedule

# Batch jobs: spot nodes, preemptible, no database taint
# PriorityClass: low (so K8s can preempt if needed)
priorityClassName: batch-low-priority
```

Also: TopologySpreadConstraints across AZs for database HA

---

**Q5: A pod is not being scheduled. How do you debug it?**

*(4 min) — Answer:*
```bash
kubectl describe pod stuck-pod
# Events section: look for FailedScheduling

# Common reasons:
# "0/5 nodes available: 5 insufficient memory"
#   → increase requests or add more nodes

# "0/5 nodes available: node(s) had taint {team=X:NoSchedule}"
#   → pod needs toleration for that taint

# "0/5 nodes available: 3 node affinity, 2 node cordoned"
#   → affinity rules too restrictive or nodes cordoned

# "unbound PVC: no storage available in AZ"
#   → StorageClass uses WaitForFirstConsumer, no node in the right AZ
```

---

**Q6: How do you ensure your payment-service pods are spread across 3 availability zones?**

*(4 min) — Answer:*
```yaml
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels:
        app: payment-service
```

vs `podAntiAffinity`: antiAffinity is binary (schedule or not), topologySpreadConstraints allows maxSkew flexibility

Pair with minReplicas ≥ 3 (one per zone) and PodDisruptionBudget maxUnavailable: 1

---

## Round 4: Production Operations (30 minutes)

---

**Q1: Payment pod keeps crashing with CrashLoopBackOff. Walk me through your debug process.**

*(7 min) — Full debug sequence:*
```bash
# 1. Current state
kubectl get pods -n prod | grep payment

# 2. Check logs from crashed container
kubectl logs payment-service-xxx -n prod --previous

# 3. Describe: events, last state, exit code
kubectl describe pod payment-service-xxx -n prod

# 4. Exit code interpretation:
# 0: clean exit (why is it restarting?)
# 1: app error (check logs)
# 137: OOMKilled (killed by kernel: limit too low or memory leak)
# 143: SIGTERM not handled (app didn't exit on shutdown signal)

# 5. Run debug container if image has no shell
kubectl debug -it payment-service-xxx --image=busybox -n prod

# 6. Check if issue is env vars / secrets missing
kubectl exec payment-service-xxx -- env | grep DB_URL
```

---

**Q2: How do you design a zero-downtime deployment?**

*(6 min) — Checklist:*
1. `maxUnavailable: 0` in RollingUpdate
2. Readiness probe (returns 200 only when app is ready to serve)
3. `preStop: sleep 15` to drain connections before SIGTERM
4. `terminationGracePeriodSeconds: 60` to allow in-flight requests to complete
5. PodDisruptionBudget: minAvailable 50% during node drains
6. TopologySpreadConstraints across zones (zone failure doesn't take everything down)

---

**Q3: Walk me through upgrading an EKS cluster from 1.27 to 1.28.**

*(6 min) — Ordered steps:*
1. Pre-upgrade: `kubent` (check deprecated APIs), run in dev cluster first
2. Review AWS EKS 1.28 changelog for breaking changes
3. Upgrade control plane: `eksctl upgrade cluster --name=prod --version=1.28 --approve`
4. Wait for control plane healthy
5. Upgrade add-ons: CoreDNS, kube-proxy, VPC CNI → via eksctl or console
6. Upgrade worker nodes:
   - Managed node groups: update launch template, roll nodes (one at a time, respects PDB)
   - Or: Karpenter drift detection auto-replaces nodes
7. Validate: all pods running, no CrashLoopBackOff, spot-check app metrics

---

**Q4: Describe how you'd set up monitoring for a Kubernetes production environment.**

*(5 min) — Layer by layer:*
```text
Node level: prometheus-node-exporter (CPU, memory, disk, network per node)
K8s control plane: kube-state-metrics (pod counts, deployment status, PVC status)
Container level: cAdvisor (built into kubelet) — CPU/memory per container
App level: app exposes /metrics, ServiceMonitor scrapes it

Dashboards (Grafana):
  - Node resources overview
  - Namespace resource usage (for quota planning)
  - Per-service: request rate, error rate, latency (RED)
  - Per-service: CPU/memory utilization

Alerts (PrometheusRule):
  - PodCrashLooping (restartCount increases fast)
  - Container OOMKilled
  - Node disk >85%
  - Deployment replica count < desired
  - PVC usage >80%
```

---

**Q5: An SRE alert fires: "certificate expired on ingress." How do you handle it?**

*(5 min) — Answer:*
```bash
# Immediate: identify which cert expired
kubectl get certificates -A  # cert-manager
kubectl get secret -n prod | grep tls

# Check expiry
kubectl get certificate payment-tls -n prod -o jsonpath='{.status.notAfter}'

# Cert-manager: manual force renewal
kubectl annotate certificate payment-tls \
  cert-manager.io/force-renewal=true -n prod

# Without cert-manager: update TLS secret with new cert from CA
kubectl create secret tls payment-tls --cert=new.crt --key=new.key \
  -n prod --dry-run=client -o yaml | kubectl apply -f -

# Verify
kubectl describe certificate payment-tls -n prod
```

Prevention:
- cert-manager with Let's Encrypt auto-renewal (30-day buffer)
- Alert on `certmanager_certificate_expiration_timestamp_seconds < now() + 30 days`
- Runbook in on-call docs

---

## Round 5: System Design with Kubernetes (45 minutes)

**Scenario: Design a real-time payment processing system on Kubernetes.**

*Requirements (tell candidate):*
- Process 10,000 TPS at peak
- < 200ms P99 latency
- Zero downtime deployments
- PCI-DSS compliance (payment card industry)
- Multi-AZ HA, RPO=0 RTO<5min
- 50+ dev teams deploying services

---

*(Allow 5 minutes for the candidate to think and sketch)*

---

**Expected areas to cover:**

**Cluster Design:**
```text
Production EKS clusters: 2 (us-east-1 + us-west-2 for DR)
Dedicated node groups:
  - general: m6i.2xlarge, on-demand (payment-api, order-service)
  - db-proxy: r6i.xlarge, dedicated (PgBouncer, Redis proxy)
  - batch: m6i.xlarge, spot (report generation, reconciliation)
```

**Payment API:**
```text
Deployment: 20+ replicas (auto-scale HPA target 60% CPU)
Readiness probe: /health with DB connectivity check
TopologySpreadConstraints: maxSkew 1 across 3 AZs
PDB: maxUnavailable 10% (never lose more than 2 pods at once)
Canary deployments: Argo Rollouts, 5% → 20% → 50% → 100%
Analysis: Prometheus success rate > 99.9% at each step
```

**Security (PCI-DSS):**
```text
NetworkPolicy: payment pods only accept from API gateway (port 8443)
PSS: Restricted mode enforced on payment namespace
IRSA: each service has its own IAM role
No static credentials anywhere in cluster
Image signing: Cosign required for prod, verified by Kyverno
Secrets: External Secrets Operator pulling from AWS Secrets Manager
etcd encryption at rest (EncryptionConfiguration)
Audit logging: all API server access logged to CloudWatch
```

**Zero-downtime deployments:**
```text
preStop sleep, terminationGracePeriodSeconds, maxUnavailable: 0
Canary with automatic rollback on error rate threshold
Feature flags (LaunchDarkly/Unleash) for gradual feature rollout
DB migrations: backward-compatible (additive only, no column drops)
```

**HA and DR:**
```text
RTO < 5min: GitOps (ArgoCD) deploys to backup cluster within minutes
RPO = 0: synchronous replication for PostgreSQL (or Aurora Global with <1s lag)
Kafka: RF=3, min-ISR=2; consumers can replay from offset
Velero backup: daily PVC snapshots to S3 with cross-region replication
Route53 health checks: auto-failover to us-west-2 if us-east-1 fails
```

---

*Interviewer follow-up: "You said Argo Rollouts. How does automatic rollback work?"*

```text
AnalysisTemplate queries Prometheus every 60 seconds.
If successRate < 99.9% for 2 consecutive checks → AnalysisRun fails.
Rollout controller detects failed analysis → aborts rollout → traffic reverts to stable.
Operator gets Slack notification with error rate metric.
```

*"What's your biggest risk in this design?"*
```text
Honest answer: database is the hardest part.
Application tier is stateless and easy to scale/failover.
PostgreSQL failover (even with Aurora) can have a brief write unavailability window.
For true RPO=0 RTO<1min: need active-active database (CockroachDB, YugabyteDB, or Spanner).
Those have different consistency trade-offs — worth discussing with the team.
```
