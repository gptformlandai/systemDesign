# Kubernetes Scenario Drill Bank — 10 Production Scenarios

> Track: K8s Interview Track — Phase 8: Practice Upgrade
> How to use: Read scenario. Think for 3 minutes. Write your answer. Then read the model answer.
> These are the types of scenarios asked at L5–L7 MAANG platform/SRE interviews.

---

## Scenario 1: CrashLoopBackOff on Payment Service After Deploy

**Situation:**
You deployed a new version of `payment-service` to production. Within 5 minutes, PagerDuty fires — payment failure rate is 40%. You check the pods and see:

```
payment-service-7f9d4b-abc  0/1  CrashLoopBackOff  5  3m
payment-service-7f9d4b-def  0/1  CrashLoopBackOff  4  3m
payment-service-7f9d4b-ghi  1/1  Running           0  3m
```

One pod is running, two are crashing. **Walk through your diagnosis and resolution.**

**Model Answer:**

**Step 1 — Get crash logs:**
```bash
kubectl logs payment-service-7f9d4b-abc -n prod --previous
# Look for: exception, startup error, port conflict, missing env var
```

**Step 2 — Check events:**
```bash
kubectl describe pod payment-service-7f9d4b-abc -n prod
# Key: events section, Last State exit code, OOMKilled?
```

**Step 3 — Hypothesis: new version fails on some pods but not others**
```text
Why would 2/3 crash? Possible causes:
  a) Different nodes have different environment (mounted secrets missing on some nodes)
  b) OOMKilled: memory limit too tight; some pods handle more requests
  c) DB connection pool exhaustion on two specific pods
  d) App reads a node-local file path that only exists on one node
```

**Step 4 — Immediate mitigation:**
```bash
# Rollback to previous version instantly
kubectl rollout undo deployment/payment-service -n prod
# Monitor: payment failure rate should drop
kubectl rollout status deployment/payment-service -n prod
```

**Step 5 — Root cause investigation:**
```bash
# Check if OOMKilled
kubectl describe pod payment-service-7f9d4b-abc | grep -A5 "Last State"
# Check node differences
kubectl get pods -o wide | grep payment-service  # which nodes?
```

**Step 6 — Fix and re-deploy:**
```text
After finding root cause:
  - Fix the code or configuration
  - Test in staging (reproduce the crash)
  - Deploy with slow canary: 5% → 20% → 50% → 100%
  - Watch error rate at each stage before promoting
```

**Key interview insight:** Always rollback immediately, then investigate. Don't debug production with live traffic hitting broken pods.

---

## Scenario 2: Pods Pending for 20 Minutes — Black Friday Traffic

**Situation:**
It's Black Friday. KEDA has scaled payment-worker from 5 to 50 pods to handle SQS queue depth. But `kubectl get pods` shows 30 pods in `Pending` state. The SQS queue is growing. **What is happening and how do you fix it?**

**Model Answer:**

**Step 1 — Diagnose pending pods:**
```bash
kubectl describe pod payment-worker-xxx -n prod
# Events: 0/8 nodes available: insufficient CPU
# or: 0/8 nodes available: node had taint {team=payments:NoSchedule}
```

**Step 2 — Check node capacity:**
```bash
kubectl top nodes         # actual usage
kubectl describe nodes | grep -A5 "Allocated resources"  # requested vs capacity
```

**Step 3 — Root cause — Cluster can't provision nodes fast enough:**
```text
Scenario A: Cluster Autoscaler (slow — 2-5 minutes per node)
  - 30 pods pending × 5 min per node = 10+ minutes before capacity available
  - Queue keeps growing in the meantime

Scenario B: Karpenter NodePool maxReplicas too low or limits.cpu hit
Scenario C: EC2 capacity constraint (spot unavailable in the AZ)
```

**Step 4 — Immediate actions:**

```bash
# If Karpenter: check if NodePool CPU limit hit
kubectl get nodepools -o yaml | grep "limits:" -A3

# Increase temporarily:
kubectl patch nodepool payment-workers \
  --type='merge' -p '{"spec":{"limits":{"cpu":"2000"}}}'

# If Cluster Autoscaler: check status
kubectl -n kube-system logs deployment/cluster-autoscaler | tail -50
```

**Step 5 — Longer term fixes:**
```text
1. Pre-scale before known traffic spikes (KEDA cron trigger at 5AM Black Friday)
2. Switch from Cluster Autoscaler to Karpenter (30-60s vs 3-5 min)
3. Use Spot + On-Demand blend with Karpenter (less capacity constraints)
4. Set minReplicas=10 (not 5) for payment-worker during holiday season
5. Implement predictive scaling based on historical patterns
```

---

## Scenario 3: Service Intermittently Returns 503

**Situation:**
`orders-service` is returning HTTP 503 to 2% of requests, but only during deployments of `payment-service`. Both services are in the `prod` namespace. **Explain the likely cause and fix.**

**Model Answer:**

**Root Cause — Traffic hits terminating pods:**

```text
When payment-service updates (rolling update):
  1. Old pod receives SIGTERM → starts shutting down
  2. Pod still in Service Endpoints for a few seconds (iptables propagation delay)
  3. orders-service sends request to the terminating payment-service pod
  4. Payment-service rejects: connection closed → 503 to orders

This is the classic rolling update connection drain problem.
```

**Fix — Add preStop hook:**
```yaml
lifecycle:
  preStop:
    exec:
      command: ["/bin/sh", "-c", "sleep 15"]
terminationGracePeriodSeconds: 60
```

```text
How it helps:
  1. Pod removed from Endpoints
  2. preStop sleep: 15 seconds for iptables rules to propagate
  3. SIGTERM: app starts graceful shutdown, finishing in-flight requests
  4. App exits cleanly
  Total: ~25-30 seconds safe drain window
```

**Also add readiness probe:**
```text
readiness probe ensures new pods only get traffic when they're ready.
Without readiness probe: rolling update routes to new (not-ready) pods → 503.
```

**Verify the fix:**
```text
  1. Apply preStop + readiness to payment-service Deployment
  2. Run a deployment
  3. Measure error rate during rollout (should drop to 0%)
```

---

## Scenario 4: Memory Leak Investigation

**Situation:**
Prometheus alert: `payment-service memory usage has grown from 400MB to 1.8GB over the past 12 hours. memory limit is 2GB. OOMKill predicted in 2 hours.` **How do you handle this?**

**Model Answer:**

**Step 1 — Buy time (immediate):**
```bash
# Increase memory limit temporarily to avoid OOMKill while investigating
kubectl set resources deployment/payment-service \
  -c payment-service \
  --limits=memory=4Gi \
  -n prod
```

**Step 2 — Identify the growing pods:**
```bash
kubectl top pods -n prod --sort-by=memory | head -20
```

**Step 3 — Get heap dump (Java example):**
```bash
# Find the pod name
POD=$(kubectl get pods -n prod -l app=payment-service -o name | head -1)

# Take heap dump inside container
kubectl exec -n prod $POD -- jmap -dump:format=b,file=/tmp/heapdump.hprof 1

# Copy to local machine for analysis
kubectl cp prod/$POD:/tmp/heapdump.hprof ./heapdump.hprof

# Analyze with Eclipse MAT, VisualVM, or jmap histogram
kubectl exec -n prod $POD -- jmap -histo:live 1 | head -50
```

**Step 4 — Check for common patterns:**
```text
Memory leak signatures:
  - HashMap growing unboundedly (cache without eviction)
  - ThreadLocal not cleared
  - Event listener not deregistered
  - Connection pool (JDBC connections not returned)
  - Off-heap memory (DirectByteBuffer for Netty/Kafka)
```

**Step 5 — Prometheus query to verify growth:**
```promql
rate(container_memory_working_set_bytes{pod=~"payment-service.*"}[1h])
```

**Step 6 — Long term:**
```text
  - Fix the leak in code (review PR for unbounded data structures)
  - Add memory metric to custom dashboard (working_set_bytes trend)
  - Alert on memory growth rate (not just absolute threshold)
  - Consider JVM heap settings: -XX:+UseG1GC -Xmx1500m
```

---

## Scenario 5: Multi-Tenant Cluster — Team A Is Affecting Team B

**Situation:**
Platform team gets a complaint: `orders-service` in team-orders namespace has 3x latency spikes every day between 2-4 PM. `payment-service` in team-payments has no issues. They share the same node group. **Investigate and resolve.**

**Model Answer:**

**Step 1 — Identify the noisy neighbor:**
```bash
# Check CPU usage on nodes during the spike window (2-4 PM yesterday)
# Prometheus:
sum(rate(container_cpu_usage_seconds_total{namespace!="kube-system"}[5m])) by (namespace, pod)
# Filter for 2-4 PM range
```

**Step 2 — Find the culprit:**
```bash
kubectl top pods -A --sort-by=cpu  # run at 2 PM
# Identify namespace consuming most CPU
```

**Step 3 — Likely cause: nightly batch job runs at 2 PM?**
```bash
kubectl get cronjobs -A
# Check CronJobs in team-payments or other namespaces
```

**Step 4 — Immediate fix — Add ResourceQuota to the culprit namespace:**
```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: team-payments-quota
  namespace: team-payments
spec:
  hard:
    limits.cpu: "20"    # cap their total CPU usage
    limits.memory: "40Gi"
```

**Step 5 — Long-term fix — Node isolation per team:**
```text
Option A: Dedicated node groups with taints
  taint: team=payments:NoSchedule on payment nodes
  payment pods tolerate the taint + nodeSelector: team=payments

Option B: Priority classes
  orders-service: high PriorityClass (payment-critical: 1000000)
  batch jobs: low PriorityClass (batch: 100)
  Under contention: K8s throttles lower-priority workloads

Option C: Karpenter NodePool per team (separate EC2 fleet per team)
```

---

## Scenario 6: StatefulSet Kafka Node Won't Come Back

**Situation:**
Kafka StatefulSet has 3 replicas. Node hosting `kafka-2` failed. `kafka-2` pod is now `Pending`. **Walk through the diagnosis and ensure zero data loss.**

**Model Answer:**

**Step 1 — Assess the situation:**
```bash
kubectl get pods -n kafka -o wide  # kafka-2 is Pending
kubectl describe pod kafka-2 -n kafka  # FailedScheduling: AZ mismatch?
kubectl get pvc -n kafka  # PVC data-kafka-2 should be Bound
```

**Step 2 — The storage is the key concern:**
```text
kafka-2's PVC is backed by an EBS volume in AZ us-east-1b (where the failed node was).
EBS volumes are AZ-specific — kafka-2 can only reschedule to a node in us-east-1b.

If pod tries to schedule on us-east-1a node → EBS attachment fails.
```

**Step 3 — Force rescheduling in the right AZ:**
```bash
# Check which AZ the PV is in
kubectl get pv $(kubectl get pvc data-kafka-2 -n kafka -o jsonpath='{.spec.volumeName}') \
  -o jsonpath='{.spec.nodeAffinity}'

# Ensure new node exists in that AZ
kubectl get nodes --show-labels | grep us-east-1b

# If no nodes in us-east-1b: Karpenter will provision one
# Watch for new node
kubectl get nodes -w
```

**Step 4 — After pod reschedules:**
```bash
# Wait for pod to be Running
kubectl get pods kafka-2 -n kafka -w

# Check Kafka cluster health
kubectl exec kafka-0 -n kafka -- \
  kafka-topics.sh --bootstrap-server kafka-0.kafka:9092 --describe
# All topics should have proper replication
```

**Step 5 — Data integrity:**
```text
Kafka replication factor = 3 (assumed):
  kafka-0 and kafka-1 were running → they have all partition data
  kafka-2 rejoins → Kafka replication syncs missing data to kafka-2
  No data loss (RF=3 means can tolerate 1 broker failure)

Risk: if RF=1 and kafka-2 had the only copy of some partitions → data loss
Prevention: always use RF≥2 for important topics
```

---

## Scenario 7: Cluster Upgrade Gone Wrong

**Situation:**
During EKS upgrade from 1.27 to 1.28, you notice that `payment-ingress` is no longer routing traffic. The Ingress resource still exists but returns 502. **What happened?**

**Model Answer:**

**Root Cause — API version removed:**
```text
Most likely scenario:
  Ingress resource was using networking.k8s.io/v1beta1 (removed in K8s 1.22)
  But this version may still have a conversion layer.

More likely for 1.28:
  Custom annotations or IngressClass behavior changed.
  Or: NGINX Ingress Controller version incompatibility with 1.28.
```

**Diagnosis:**
```bash
# Check NGINX Ingress Controller pods
kubectl get pods -n ingress-nginx  # are they Running?

# Check NGINX Ingress Controller version compatibility
kubectl describe pod -n ingress-nginx | grep Image

# Check IngressClass
kubectl get ingressclass
kubectl get ingress payment-ingress -n prod -o yaml

# Check NGINX controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx --tail=50
```

**Resolution:**
```bash
# Upgrade NGINX Ingress Controller to K8s 1.28-compatible version
helm upgrade ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --version 4.9.0    # check compatibility matrix

# Or: re-apply Ingress with updated annotations
kubectl replace -f payment-ingress.yaml
```

**Prevention:**
```text
Pre-upgrade checklist:
  1. Run kubent/Pluto to find deprecated API usage
  2. Check all Helm chart compatibility matrices
  3. Upgrade dev cluster 1 week before prod
  4. Test Ingress routing on dev after upgrade
  5. Keep rollback plan: can't downgrade K8s, but can re-deploy old Ingress controller
```

---

## Scenario 8: RBAC — Developer Can See Another Team's Secrets

**Situation:**
Security audit finds that developers in `team-payments` group can list secrets in `team-orders` namespace. **Trace the root cause using RBAC tools and fix it.**

**Model Answer:**

**Step 1 — Find the RBAC binding:**
```bash
# Check all bindings for team-payments group
kubectl get rolebindings,clusterrolebindings -A \
  -o json | jq '.items[] | select(.subjects[]?.name=="team-payments")'

# Check what ClusterRoleBinding team-payments has
kubectl describe clusterrolebinding team-payments-cluster-view
# Likely: ClusterRole "view" bound cluster-wide
```

**Step 2 — Identify the problem:**
```text
ClusterRoleBinding: binds ClusterRole "view" to team-payments group cluster-wide
Result: team-payments can "view" all resources in ALL namespaces

What should exist:
  RoleBinding (namespace-scoped) binding ClusterRole "view" to team-payments
  Only in team-payments namespace — not cluster-wide
```

**Step 3 — Fix:**
```bash
# Delete the broad ClusterRoleBinding
kubectl delete clusterrolebinding team-payments-cluster-view

# Create namespace-scoped RoleBinding instead
kubectl create rolebinding team-payments-view \
  --clusterrole=view \
  --group=team-payments \
  --namespace=team-payments
```

**Step 4 — Verify:**
```bash
kubectl auth can-i list secrets --as-group=team-payments \
  --as=user:dummy -n team-orders
# Should return: no

kubectl auth can-i list secrets --as-group=team-payments \
  --as=user:dummy -n team-payments
# Should return: yes
```

**Step 5 — Prevent recurrence:**
```text
Add Gatekeeper constraint:
  ClusterRoleBinding to broad roles (cluster-admin, view) requires platform team approval
  Developer RBAC changes go through GitOps PR review
  Regular RBAC audit script (quarterly)
```

---

## Scenario 9: Zero-Downtime Migration From Kubernetes 1.x to New Cluster

**Situation:**
You need to migrate all workloads from an old EKS cluster (1.26, end of support) to a new EKS cluster (1.29). You have 30 microservices and stateful databases. **Design the migration strategy.**

**Model Answer:**

**Phase 1 — Preparation (2 weeks before)**
```text
1. Audit deprecated APIs: run kubent on old cluster
2. Update all Helm charts and manifests for 1.29 API changes
3. Set up new EKS cluster 1.29 with same VPC, same node sizes
4. Deploy ArgoCD to new cluster pointing to GitOps repo
5. Set up new cluster networking:
   - Same subnets (pods need VPC connectivity between clusters)
   - AWS Load Balancer Controller, VPC CNI, EBS CSI driver
```

**Phase 2 — Stateless Services Migration**
```text
1. ArgoCD syncs all Deployments to new cluster → all pods running
2. For each service:
   a. Route 10% of traffic to new cluster (via weighted DNS or ALB target groups)
   b. Validate metrics (error rate, latency) in new cluster
   c. Increase to 50%, then 100%
   d. Remove service from old cluster

Use AWS Route53 weighted routing:
  old-cluster-alb: weight 90 → 50 → 10 → 0
  new-cluster-alb: weight 10 → 50 → 90 → 100
```

**Phase 3 — Stateful Services (Databases)**
```text
Kafka:
  1. Set up Kafka on new cluster
  2. Mirror topics from old to new (Kafka MirrorMaker 2)
  3. Update consumer groups to new cluster one by one
  4. Cut over producers when consumers are caught up
  5. Old Kafka stays as backup for 1 week

PostgreSQL (read replicas approach):
  1. Create read replica of old DB pointing to new region/cluster
  2. Promote replica when ready to cut over
  3. Update app connection string to new DB
  4. Use Velero to backup old PVCs → restore to new cluster as safety net
```

**Phase 4 — DNS Cutover**
```text
Update Route53 to point 100% to new cluster ALB
Monitor: error rate, latency for 30 minutes
If OK: decommission old cluster
If bad: instant rollback (update Route53 back to old cluster ALB)
```

---

## Scenario 10: Design a K8s Platform for 200 Teams

**Situation:**
Your company is adopting Kubernetes. 200 developer teams, each team has 10-50 microservices. Design the platform. **What would you build?**

**Model Answer:**

**Cluster Topology:**
```text
Hub cluster (management):
  - ArgoCD (GitOps for all clusters)
  - Prometheus + Grafana + Alertmanager (aggregated metrics)
  - Vault (secrets management)
  - Backstage (developer portal / self-service)

Spoke clusters (workloads):
  - prod-us-east: production East Coast
  - prod-us-west: production West Coast (DR)
  - staging: shared staging cluster
  - dev: shared dev cluster (heavy ResourceQuota, spot nodes)

Per-team namespace on spoke clusters:
  - team-{name}-dev, team-{name}-staging, team-{name}-prod
  - Or namespace in shared dev cluster, dedicated app in prod
```

**Onboarding Automation:**
```text
Developer opens PR in "team-onboarding" repo:
  - New namespace definition (team name, budget, contacts)
  - PR reviewed by platform team, merged
  - ArgoCD applies: Namespace + ResourceQuota + LimitRange + RBAC + NetworkPolicy
  - ArgoCD ApplicationSet generates Application for team's GitOps repo
  - Backstage shows team their cluster URL, namespace, CI/CD setup
```

**Security by Default:**
```text
All namespaces: pod-security.kubernetes.io/enforce: baseline
Critical namespaces: restricted
Gatekeeper: required labels, image allowlist, no LoadBalancer services
IRSA: per-service AWS IAM roles via ESO (External Secrets Operator)
Image signing: Cosign required for prod
```

**Cost and Governance:**
```text
Kubecost: per-team cost dashboards and monthly reports
Karpenter: consolidation runs nightly (replace underutilized nodes)
Spot instances for dev and non-critical workloads
ResourceQuota alerts when teams hit 80% quota utilization
```

**Developer Experience:**
```text
Self-service portal (Backstage) for:
  - Creating new services (scaffold from template)
  - Viewing cluster status and pod health
  - Requesting quota increases (automated approval for small increases)
  - Viewing cost trends
GitHub Actions template: standard CI pipeline (test, scan, sign, push, update gitops)
```
