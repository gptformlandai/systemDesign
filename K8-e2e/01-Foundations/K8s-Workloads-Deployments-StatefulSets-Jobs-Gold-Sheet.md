# Kubernetes Workloads: Deployments, StatefulSets, DaemonSets, Jobs Gold Sheet

> Track: K8s Interview Track — Phase 1: Foundations
> Goal: Master every Kubernetes workload type — when to use each, how they behave, and what interviewers test about them.

---

## 0. How To Read This

Beginner focus:
- Deployment: rolling updates and rollbacks
- ReplicaSet vs Deployment relationship
- DaemonSet: one pod per node

Intermediate focus:
- StatefulSet: stable network identity, ordered scaling
- Job and CronJob: batch workloads
- Update strategies: RollingUpdate vs Recreate
- Pod disruption budgets

Senior / MAANG focus:
- StatefulSet headless service and DNS resolution
- Blue/Green and Canary using Deployments
- Job parallelism and completion patterns
- Workload identity and IRSA on EKS

---

# Topic 1: Deployment

## 1. Intuition

A Deployment manages a ReplicaSet which manages Pods. You never create ReplicaSets directly — you declare a Deployment and Kubernetes handles the ReplicaSet for you.

```text
Deployment → owns → ReplicaSet(s) → owns → Pods

When you update a Deployment:
  new ReplicaSet created
  old ReplicaSet scaled down
  new ReplicaSet scaled up
  old ReplicaSet kept at 0 (for rollback)
```

## 2. Deployment Spec

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
  namespace: prod
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-service   # must match pod template labels
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1       # max pods that can be down during update
      maxSurge: 1             # max extra pods above desired during update
  template:
    metadata:
      labels:
        app: payment-service
        version: v1.2.3
    spec:
      containers:
        - name: payment-service
          image: my-registry/payment-service:v1.2.3
          ports:
            - containerPort: 8080
          resources:
            requests:
              cpu: "250m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
```

## 3. Update Strategies

### RollingUpdate (default)
```text
maxUnavailable: 1, maxSurge: 1, replicas: 3

Start: [v1, v1, v1]

Step 1: create v2 pod (surge)     [v1, v1, v1, v2]  (4 pods, 1 extra)
Step 2: terminate v1 (unavail)    [v1, v1, v2]      (back to 3, 1 less v1)
Step 3: create v2 pod             [v1, v1, v2, v2]
Step 4: terminate v1              [v1, v2, v2]
Step 5: create v2 pod             [v1, v2, v2, v2]
Step 6: terminate v1              [v2, v2, v2]      done
```

### Recreate
```text
Terminate ALL old pods first, then create ALL new pods.
Causes downtime — use only for databases or single-instance workloads
that cannot run two versions simultaneously.
```

## 4. Rollback

```bash
# View rollout history
kubectl rollout history deployment/payment-service

# Rollback to previous version
kubectl rollout undo deployment/payment-service

# Rollback to specific revision
kubectl rollout undo deployment/payment-service --to-revision=3

# Pause/resume rollout (for canary validation)
kubectl rollout pause deployment/payment-service
kubectl rollout resume deployment/payment-service

# Wait for rollout to complete
kubectl rollout status deployment/payment-service
```

## 5. Blue/Green Deployment Pattern

```text
Production Service selector:  app=payment-service, slot=blue
Staging (next) version pods:  app=payment-service, slot=green

When ready to switch:
  kubectl patch service payment-service -p '{"spec":{"selector":{"slot":"green"}}}'

Instant cutover, instant rollback (patch selector back).
No Kubernetes-native "blue/green" — this is achieved with label selectors.
```

## 6. Canary Pattern

```text
payment-service-v1: 10 replicas (90% traffic via label selector)
payment-service-v2: 1 replica  (10% traffic — both match app=payment-service)

Service selector: app=payment-service (matches both)
Traffic is split proportionally by pod count.

For precise traffic percentages, use Service Mesh (Istio VirtualService)
or Ingress NGINX canary annotations.
```

---

# Topic 2: StatefulSet

## 1. Intuition

StatefulSets are for workloads that need stable identity:
- Databases (PostgreSQL, MongoDB, Cassandra)
- Messaging systems (Kafka, ZooKeeper)
- Any app that uses local disk and needs to reconnect to the same storage

```text
Deployment:
  Pods are identical, interchangeable
  Pod names: payment-service-abc123, payment-service-def456

StatefulSet:
  Pods have stable names: payment-db-0, payment-db-1, payment-db-2
  Each pod gets its own PersistentVolumeClaim: data-payment-db-0, data-payment-db-1
  Pods start/stop in order (0 → 1 → 2)
```

## 2. StatefulSet Spec

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
spec:
  serviceName: postgres-headless   # must reference a headless service
  replicas: 3
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:15
          ports:
            - containerPort: 5432
          env:
            - name: PGDATA
              value: /data/pgdata
          volumeMounts:
            - name: data
              mountPath: /data
  volumeClaimTemplates:                # PVC created per pod
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: gp3
        resources:
          requests:
            storage: 100Gi
```

## 3. Headless Service and DNS

StatefulSets require a headless Service (clusterIP: None):

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-headless
spec:
  clusterIP: None          # headless: no cluster IP
  selector:
    app: postgres
  ports:
    - port: 5432
```

DNS records created:
```text
postgres-0.postgres-headless.prod.svc.cluster.local
postgres-1.postgres-headless.prod.svc.cluster.local
postgres-2.postgres-headless.prod.svc.cluster.local

Pattern: {pod-name}.{service-name}.{namespace}.svc.cluster.local

Used by apps that need to address each replica individually (e.g., Kafka brokers).
```

## 4. Scaling Behavior

```text
Scale UP:   ordered, sequential: 0 → 1 → 2
            waits for each pod to be Running and Ready before creating next

Scale DOWN: reverse order: 2 → 1 → 0
            waits for each pod to terminate before terminating previous

This ensures the "newest" replica is removed first (safer for quorum-based systems).
```

## 5. StatefulSet Interview Traps

```text
Q: Can I scale a StatefulSet with kubectl scale?
A: Yes, but ensure application handles rebalancing (Kafka partition rebalance).

Q: What happens to PVCs when I delete a StatefulSet?
A: PVCs are NOT automatically deleted. You must delete them manually.
   (This is intentional — prevents accidental data loss.)

Q: How do I update a StatefulSet?
A: updateStrategy.type: RollingUpdate (default in ordered fashion)
   Or type: OnDelete (pods only update when manually deleted)
```

---

# Topic 3: DaemonSet

## 1. Intuition

A DaemonSet ensures exactly one pod runs on every node (or selected nodes):

```text
Use cases:
  - Log collection agent (Fluent Bit, Filebeat)
  - Metrics collection (node-exporter for Prometheus)
  - Storage daemon (Ceph OSD)
  - Network plugin (CNI, Calico, Cilium)
  - Security agent (Falco, CrowdStrike sensor)
  - Node-local caching (cluster-local DNS)
```

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluent-bit
  namespace: kube-system
spec:
  selector:
    matchLabels:
      app: fluent-bit
  template:
    metadata:
      labels:
        app: fluent-bit
    spec:
      tolerations:
        - key: node-role.kubernetes.io/control-plane   # also run on control plane
          operator: Exists
          effect: NoSchedule
      containers:
        - name: fluent-bit
          image: fluent/fluent-bit:2.1
          volumeMounts:
            - name: varlog
              mountPath: /var/log
              readOnly: true
      volumes:
        - name: varlog
          hostPath:
            path: /var/log
```

Targeting specific nodes:
```yaml
spec:
  template:
    spec:
      nodeSelector:
        kubernetes.io/os: linux   # only Linux nodes
```

---

# Topic 4: Job and CronJob

## 1. Job — Run to Completion

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: db-migration
spec:
  completions: 1       # how many pods must succeed
  parallelism: 1       # how many pods run at once
  backoffLimit: 3      # retry on failure (max 3 times)
  activeDeadlineSeconds: 300   # kill job if not done in 5 minutes
  template:
    spec:
      restartPolicy: OnFailure   # required for Jobs (not Always)
      containers:
        - name: migration
          image: payment-service:v1.2.3
          command: ["./migrate", "--run"]
```

## 2. Parallel Job Patterns

```text
Pattern 1: Single job (completions=1, parallelism=1)
  Run one task once. Default.

Pattern 2: Fixed completion count (completions=5, parallelism=2)
  Run exactly 5 tasks total, 2 at a time.
  Use for batch processing (process 5 files, one pod per file).

Pattern 3: Work queue (completions not set, parallelism=5)
  Workers process from a queue until queue is empty.
  Pods communicate with external work queue (SQS, Redis).
```

## 3. CronJob

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: nightly-report
spec:
  schedule: "0 2 * * *"       # 2:00 AM UTC every day
  timeZone: "America/New_York" # optional timezone (K8s 1.27+)
  concurrencyPolicy: Forbid    # skip if previous still running
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 1
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: reporter
              image: reporting-service:v2.0
              command: ["./generate-report", "--date=yesterday"]
```

ConcurrencyPolicy options:
- `Allow`: multiple runs can overlap (default)
- `Forbid`: skip new run if previous is still running
- `Replace`: terminate current run and start new

## 4. Common Job Mistakes

| Mistake | Correct Approach |
|---|---|
| `restartPolicy: Always` in Job | Use `OnFailure` or `Never` |
| No `backoffLimit` (default is 6) | Set appropriate limit for your job |
| No `activeDeadlineSeconds` | Always set a deadline to prevent zombie jobs |
| CronJob with `concurrencyPolicy: Allow` for long-running jobs | Use `Forbid` to prevent overlap |
| No `successfulJobsHistoryLimit` | Old completed Jobs clutter etcd |

---

# Topic 5: Pod Disruption Budget (PDB)

## 1. What It Is

A PDB limits how many pods of a workload can be voluntarily disrupted simultaneously:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: payment-service-pdb
spec:
  minAvailable: 2    # OR: maxUnavailable: 1
  selector:
    matchLabels:
      app: payment-service
```

## 2. When PDB Matters

Voluntary disruptions that PDB blocks:
- `kubectl drain` for node maintenance
- Cluster auto-scaler removing a node
- Rolling update (Deployment respects PDB)

Involuntary disruptions (PDB does NOT apply):
- Node hardware failure
- OOM kill
- Kernel panic

```text
Example:
  Deployment replicas: 3
  PDB minAvailable: 2

  kubectl drain node-1
    → Kubernetes checks: will evicting pods violate PDB?
    → If 2 pods are on node-1 and 1 on node-2: evicting both from node-1 = 1 remaining < 2 min
    → Drain blocked until pods redistributed
```

## 3. Revision Notes

- Deployment: manages ReplicaSets; supports RollingUpdate and Recreate; keeps old RS for rollback
- RollingUpdate: maxUnavailable + maxSurge control speed/safety of rollout
- StatefulSet: stable pod names, ordered lifecycle, per-pod PVC, headless service for DNS
- StatefulSet DNS: `{pod-name}.{service-name}.{namespace}.svc.cluster.local`
- DaemonSet: one pod per node; tolerations needed to run on control plane
- Job: run to completion; `restartPolicy: OnFailure`; set `backoffLimit` and `activeDeadlineSeconds`
- CronJob: scheduled jobs; `concurrencyPolicy` controls overlap behavior
- PDB: limits voluntary disruptions; protects quorum-based apps

## 4. Interview Scenarios

**Scenario 1: Blue/Green with zero downtime**
```text
Approach:
  1. Deploy v2 pods alongside v1 (same label selector on Service)
  2. Verify v2 health in staging namespace or via shadow traffic
  3. Switch Service selector to v2 pods (instant, atomic)
  4. Monitor error rate for 5-10 min
  5. If ok: delete v1 Deployment
  6. If bad: switch selector back to v1 (instant rollback)
```

**Scenario 2: StatefulSet rolling update with Cassandra**
```text
Approach:
  1. Set updateStrategy: RollingUpdate with partition to stage rollout
     (only pods >= partition index get updated)
  2. Update partition=2 (only pod-2 updated first)
  3. Verify pod-2 joins cluster, repairs data
  4. Reduce partition=1, then partition=0
  5. PDB ensures min 2 of 3 nodes always available during rollout
```

## 5. Official Source Notes

- Deployments: <https://kubernetes.io/docs/concepts/workloads/controllers/deployment/>
- StatefulSets: <https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/>
- DaemonSets: <https://kubernetes.io/docs/concepts/workloads/controllers/daemonset/>
- Jobs: <https://kubernetes.io/docs/concepts/workloads/controllers/job/>
- CronJobs: <https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/>
- PDB: <https://kubernetes.io/docs/concepts/workloads/pods/disruptions/>
