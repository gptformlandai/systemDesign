# Kubernetes Cluster Upgrades, Backup, and Disaster Recovery Gold Sheet

> Track: K8s Interview Track — Phase 7: Production Architecture
> Goal: Upgrade Kubernetes clusters with zero downtime, back up stateful workloads, and recover from cluster-level failures — the operational excellence exam for senior engineers.

---

## 0. How To Read This

Beginner focus:
- What a K8s version upgrade involves
- Why you can't skip versions
- Velero for backup basics

Intermediate focus:
- Control plane upgrade first, then workers
- Managed node group upgrade strategies
- Velero backup and restore workflows

Senior / MAANG focus:
- Zero-downtime upgrade with PDB and drain strategy
- etcd backup and restore (self-managed clusters)
- Multi-cluster DR with ArgoCD re-deploy
- GitOps as the DR strategy
- RTO/RPO targets for K8s platform

---

# Topic 1: Kubernetes Version Lifecycle

## 1. Version Scheme

```text
Format: MAJOR.MINOR.PATCH
  1.29.3  → major=1, minor=29, patch=3

Minor version: released 3 times per year (~every 4 months)
Support window: ~14 months (current + 2 previous minor versions maintained)

Active versions (example as of 2024):
  1.30 — current
  1.29 — supported
  1.28 — supported (EOL ~July 2025)
  1.27 — EOL (no security patches)
```

## 2. Upgrade Rules

```text
Rule 1: Can only upgrade one minor version at a time.
  ❌ 1.27 → 1.30 (skip 2 minor versions)
  ✅ 1.27 → 1.28 → 1.29 → 1.30

Rule 2: Control plane must be upgraded before worker nodes.
  API server cannot be older than worker node kubelet.

Rule 3: kubelet can be at most 2 minor versions below API server.
  API server: 1.29 → kubelet can be 1.27, 1.28, 1.29

Rule 4: kube-proxy must match kubelet version.

EKS: AWS enforces these rules; GUI/CLI prevents invalid upgrades.
```

## 3. Deprecation and API Changes

```text
K8s deprecates APIs with notice:
  Deprecated: announcement
  Removed: N minor versions later (usually N=2 or N=3)

Common removals:
  1.22: removed Ingress networking.k8s.io/v1beta1 → use networking.k8s.io/v1
  1.25: removed PodSecurityPolicy → use PSA or Gatekeeper
  1.26: removed flowcontrol.apiserver.k8s.io/v1beta1

Check for deprecated APIs before upgrade:
  kubectl convert (for manifests)
  Pluto: https://github.com/FairwindsOps/pluto
  kubent (kube-no-trouble): identifies deprecated API usage
```

---

# Topic 2: EKS Cluster Upgrade Strategy

## 1. Upgrade Order

```text
1. Check: kubectl get nodes -o wide  (confirm current versions)
2. Read: EKS version release notes (check removed APIs, new features)
3. Test: upgrade dev cluster first → validate apps work
4. Backup: etcd snapshot (self-managed) or Velero backup

5. Upgrade control plane (EKS does this automatically):
   aws eks update-cluster-version --name prod-cluster --kubernetes-version 1.29
   Wait: 20-30 minutes (EKS control plane upgrade is zero-downtime)

6. Update add-ons:
   aws eks update-addon --cluster-name prod-cluster --addon-name vpc-cni --addon-version <new-version>
   aws eks update-addon --cluster-name prod-cluster --addon-name coredns --addon-version <new-version>
   aws eks update-addon --cluster-name prod-cluster --addon-name kube-proxy --addon-version <new-version>
   aws eks update-addon --cluster-name prod-cluster --addon-name aws-ebs-csi-driver --addon-version <new-version>

7. Upgrade worker nodes (see node upgrade strategies below)

8. Verify:
   kubectl get nodes -o wide  (all nodes at new version)
   kubectl get pods -A | grep -v Running  (no failures)
```

## 2. Worker Node Upgrade Strategies

### Option A: Managed Node Group Rolling Update

```bash
aws eks update-nodegroup-version \
  --cluster-name prod-cluster \
  --nodegroup-name general-purpose \
  --kubernetes-version 1.29

# EKS automatically:
# 1. Launches new nodes with new AMI
# 2. Drains old nodes (respects PDB)
# 3. Terminates old nodes
# Takes 30-60 minutes per node group
```

### Option B: Blue/Green Node Groups

```text
More control, zero downtime:

Step 1: Create new node group with new AMI/version
  aws eks create-nodegroup --cluster-name prod-cluster --nodegroup-name general-v2 ...

Step 2: Wait for new nodes to be Ready
  kubectl get nodes

Step 3: Cordon old nodes (prevent new pods from scheduling there)
  for node in $(kubectl get nodes -l nodegroup=general-v1 -o name); do
    kubectl cordon $node
  done

Step 4: Drain old nodes one by one (respects PDB)
  for node in $(kubectl get nodes -l nodegroup=general-v1 -o name); do
    kubectl drain $node --ignore-daemonsets --delete-emptydir-data --timeout=5m
    sleep 30  # brief pause between drains
  done

Step 5: Verify all pods on new nodes
  kubectl get pods -A -o wide | grep general-v1  # should be empty

Step 6: Delete old node group
  aws eks delete-nodegroup --cluster-name prod-cluster --nodegroup-name general-v1
```

### Option C: Karpenter (Easiest)

```text
With Karpenter:
  Update EC2NodeClass to use new AMI or set expireAfter: 720h
  Karpenter gradually replaces expired nodes with new ones
  PodDisruptionBudgets are respected automatically
```

## 3. PDB — The Safety Net During Upgrades

```text
Without PDB:
  kubectl drain node-1 might evict all pods of payment-service at once
  → Brief outage

With PDB (minAvailable: 2):
  payment-service has 3 replicas
  Drain of node-1 can evict at most 1 pod at a time (keeps 2 available)
  Next pod eviction waits until first is rescheduled and Ready

Always have PDB for production workloads.
```

---

# Topic 3: etcd Backup (Self-Managed Clusters)

## 1. etcd Backup with etcdctl

```bash
# On control plane node
ETCDCTL_API=3 etcdctl snapshot save /backup/etcd-snapshot-$(date +%Y%m%d-%H%M%S).db \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key

# Verify snapshot
ETCDCTL_API=3 etcdctl snapshot status /backup/etcd-snapshot-20240115-120000.db --write-out=table
```

## 2. etcd Restore

```bash
# Step 1: Stop kube-apiserver (move manifest out of static pod dir)
mv /etc/kubernetes/manifests/kube-apiserver.yaml /tmp/

# Step 2: Restore from snapshot
ETCDCTL_API=3 etcdctl snapshot restore /backup/etcd-snapshot-20240115-120000.db \
  --data-dir=/var/lib/etcd-restored \
  --name=master \
  --initial-cluster=master=https://192.168.1.100:2380 \
  --initial-cluster-token=etcd-cluster-1 \
  --initial-advertise-peer-urls=https://192.168.1.100:2380

# Step 3: Update etcd to use restored data dir
# Edit etcd static pod manifest: change --data-dir to /var/lib/etcd-restored

# Step 4: Restart kubelet
systemctl restart kubelet

# Step 5: Restore kube-apiserver
mv /tmp/kube-apiserver.yaml /etc/kubernetes/manifests/
```

## 3. EKS etcd Backup

```text
EKS: AWS manages etcd. You cannot directly access etcd.
AWS takes automatic backups.
"Restore" scenario: work with AWS Support.

Alternative DR strategy for EKS:
  Don't restore etcd — redeploy via GitOps.
  All infrastructure declarative in Git → ArgoCD redeploys everything.
```

---

# Topic 4: Velero — Application Backup and Restore

## 1. What Velero Does

```text
Velero backs up:
  - K8s object metadata (Deployments, Services, ConfigMaps, etc.)
  - PersistentVolume data (via volume snapshots or Restic/Kopia file backup)

Use cases:
  - Disaster recovery: cluster destroyed → restore to new cluster
  - Cluster migration: move workloads from one cluster to another
  - Point-in-time restore: "roll back" to app state from 2 hours ago
  - Namespace backup: backup specific namespace before risky change
```

## 2. Velero Installation on EKS

```bash
# Install with AWS plugin
velero install \
  --provider aws \
  --plugins velero/velero-plugin-for-aws:v1.9.0 \
  --bucket my-velero-backup-bucket \
  --backup-location-config region=us-east-1 \
  --snapshot-location-config region=us-east-1 \
  --use-node-agent    # for file-based PV backup (Restic/Kopia)
```

IRSA for Velero (uses S3 and EBS snapshot APIs):
```bash
eksctl create iamserviceaccount \
  --cluster=prod-cluster \
  --namespace=velero \
  --name=velero-sa \
  --attach-policy-arn=arn:aws:iam::123456:policy/velero-policy \
  --approve
```

## 3. Backup Operations

```bash
# Create on-demand backup
velero backup create prod-backup-20240115 \
  --include-namespaces prod \
  --storage-location aws \
  --wait

# Schedule daily backup
velero schedule create daily-prod-backup \
  --schedule="0 2 * * *" \
  --include-namespaces prod \
  --ttl 720h    # keep for 30 days

# View backups
velero backup get

# Describe backup (check for errors)
velero backup describe prod-backup-20240115 --details

# Check backup logs
velero backup logs prod-backup-20240115
```

## 4. Restore Operations

```bash
# Restore entire backup to new cluster
velero restore create prod-restore-20240115 \
  --from-backup prod-backup-20240115

# Restore specific namespace
velero restore create \
  --from-backup prod-backup-20240115 \
  --include-namespaces prod \
  --namespace-mappings prod:prod-restored  # restore to different namespace

# Restore specific resources
velero restore create \
  --from-backup prod-backup-20240115 \
  --include-resources deployments,services

# Watch restore status
velero restore describe prod-restore-20240115 --details
```

---

# Topic 5: GitOps as the DR Strategy

## 1. The Philosophy

```text
"If etcd is lost, we don't need to restore it.
We redeploy everything from Git."

Requirements:
  - All K8s manifests in Git (100% declarative)
  - No manual kubectl changes (everything via GitOps PR)
  - External state (RDS, S3, SQS) restored from AWS Backup separately
  - Stateful K8s data (PVs) restored via Velero

GitOps DR steps:
  1. Create new EKS cluster (terraform apply or eksctl create cluster)
  2. Install ArgoCD on new cluster
  3. Point ArgoCD at GitOps repo
  4. ArgoCD syncs all Applications → deploys all workloads
  5. Velero restores PVCs from S3 snapshots

RTO with GitOps DR: 30-60 minutes (cluster creation + ArgoCD sync)
RTO with etcd restore: faster but more complex
```

## 2. Chaos Engineering — Validate DR

```text
Regular DR drills (quarterly):
  1. Spin up isolated "DR cluster" in a separate account
  2. Restore GitOps state from Git
  3. Restore PV data from Velero backups
  4. Run smoke tests
  5. Document gaps and fix

Tools:
  LitmusChaos: Kubernetes chaos engineering
  AWS Fault Injection Simulator (FIS): simulate AZ failure, node failure
```

---

# Topic 6: Interview Scenarios

**Scenario 1: EKS upgrade with zero downtime**

```text
Approach:
1. Pre-upgrade: run kubent to find deprecated API usage; fix manifests
2. Upgrade dev cluster: test all workloads function correctly on new version
3. Ensure all Deployments have PDB (minAvailable or maxUnavailable)
4. Upgrade control plane first via AWS console or CLI
5. Update EKS add-ons (vpc-cni, coredns, kube-proxy, ebs-csi-driver)
6. Upgrade worker nodes using blue/green node group strategy:
   - New node group with new AMI
   - Gradually drain old nodes (PDB enforced)
7. Monitor metrics during upgrade (HPA, pod restarts, latency)
8. Keep old node group for 30 min after full migration, then delete

RTO target: 0 downtime for applications
Duration: 1-3 hours for full cluster upgrade
```

**Scenario 2: Database PVC data corrupt, need restore**

```text
Approach:
1. Scale down StatefulSet to 0 (no writes to corrupt data)
   kubectl scale statefulset postgres --replicas=0 -n prod

2. Find latest clean Velero backup
   velero backup get | grep postgres

3. Delete corrupt PVC (Retain reclaim policy keeps PV)
   kubectl delete pvc data-postgres-0 -n prod

4. Restore PVC from Velero
   velero restore create postgres-pvc-restore \
     --from-backup daily-prod-backup-20240115 \
     --include-resources persistentvolumeclaims \
     --label-selector app=postgres

5. Scale StatefulSet back up
   kubectl scale statefulset postgres --replicas=3 -n prod

6. Verify data integrity (run app-specific checks)
```

---

# Topic 7: Revision Notes

- K8s upgrades: one minor version at a time; control plane first, then workers
- EKS control plane upgrade: zero-downtime by AWS; takes ~20-30 minutes
- Worker node upgrade: managed node group rolling update or blue/green node group
- PDB: required for zero-downtime drains during node upgrades
- etcd backup: `etcdctl snapshot save`; restore requires stopping kube-apiserver
- EKS etcd: managed by AWS; use GitOps + Velero as DR strategy
- Velero: backs up K8s objects + PV data to S3; restore to same or new cluster
- GitOps as DR: new cluster + ArgoCD sync from Git = full redeploy in 30-60 min
- Chaos engineering: quarterly DR drills; LitmusChaos, AWS FIS for validation

## Official Source Notes

- EKS upgrade: <https://docs.aws.amazon.com/eks/latest/userguide/update-cluster.html>
- etcd backup: <https://kubernetes.io/docs/tasks/administer-cluster/configure-upgrade-etcd/>
- Velero: <https://velero.io/docs/>
- Pluto (deprecated API checker): <https://github.com/FairwindsOps/pluto>
- kubent: <https://github.com/doitintl/kube-no-trouble>
