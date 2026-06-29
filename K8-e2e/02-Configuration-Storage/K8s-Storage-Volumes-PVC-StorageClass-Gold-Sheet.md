# Kubernetes Storage: Volumes, PVC, PV, and StorageClass Gold Sheet

> Track: K8s Interview Track — Phase 2: Configuration and Storage
> Goal: Understand how Kubernetes handles persistent storage — from ephemeral volumes to dynamically provisioned cloud disks.

---

## 0. How To Read This

Beginner focus:
- emptyDir and hostPath volumes
- PersistentVolumeClaim concept
- Dynamic provisioning overview

Intermediate focus:
- StorageClass, access modes, reclaim policies
- Static vs dynamic provisioning
- StatefulSet with volumeClaimTemplates

Senior / MAANG focus:
- CSI drivers and how they work
- Volume snapshots and cloning
- Multi-zone storage constraints
- ReadWriteMany (RWX) for shared storage (EFS, NFS)
- Storage performance tuning and IOPS provisioning

---

# Topic 1: Volume Types

## 1. Ephemeral Volumes (Not Persisted)

### emptyDir
Created when pod starts, destroyed when pod terminates:

```yaml
volumes:
  - name: cache
    emptyDir: {}           # starts empty; stored on node disk
  - name: shm
    emptyDir:
      medium: Memory       # tmpfs (RAM-backed) — fast, counts against memory limit
      sizeLimit: 512Mi
```

Uses:
- Scratch space between containers in the same pod
- Shared data between main container and sidecar
- Cache that doesn't need to survive pod restart

### hostPath
Mounts a path from the node filesystem:

```yaml
volumes:
  - name: host-logs
    hostPath:
      path: /var/log/myapp
      type: DirectoryOrCreate   # create if missing
```

Host path types:
| Type | Behavior |
|---|---|
| `""` | no check (default) |
| `DirectoryOrCreate` | create dir if missing |
| `Directory` | must exist |
| `FileOrCreate` | create file if missing |
| `File` | must exist |
| `Socket` | must be a Unix socket |

WARNING: hostPath binds pod to specific node. Breaks pod rescheduling. Use only for DaemonSets (log agents, monitoring) or when node access is explicitly required.

### configMap and Secret volumes

Already covered in Configuration Gold Sheet. Both are volume types.

### Projected volumes (combine multiple sources)

```yaml
volumes:
  - name: combined
    projected:
      sources:
        - configMap:
            name: app-config
        - secret:
            name: app-secrets
```

---

# Topic 2: Persistent Storage Model

## 1. The Three-Layer Model

```text
StorageClass   → defines HOW storage is provisioned (cloud disk type, IOPS, zones)
PersistentVolume (PV)  → actual piece of storage (disk, NFS share)
PersistentVolumeClaim (PVC) → pod's request for storage (binds to a PV)

Pod → uses PVC → bound to PV → backed by StorageClass provisioner
```

## 2. Access Modes

| Mode | Short | Description |
|---|---|---|
| ReadWriteOnce | RWO | mounted as read-write by a SINGLE node |
| ReadOnlyMany | ROX | mounted as read-only by MANY nodes |
| ReadWriteMany | RWX | mounted as read-write by MANY nodes |
| ReadWriteOncePod | RWOP | mounted read-write by a SINGLE pod (K8s 1.22+) |

```text
EBS (AWS gp3): RWO only — one node at a time
EFS (AWS): RWX — shared across many pods/nodes
NFS: RWX — shared filesystem
Azure File: RWX
Azure Disk: RWO
```

## 3. Reclaim Policies

| Policy | What happens to PV when PVC is deleted |
|---|---|
| `Retain` | PV kept; data safe; must manually reclaim or reuse |
| `Delete` | PV and backing storage deleted automatically |
| `Recycle` | Deprecated; do not use |

Production recommendation:
- Use `Retain` for important databases (prevents accidental data loss)
- Use `Delete` for ephemeral/reproducible data

---

# Topic 3: StorageClass

## 1. What StorageClass Does

StorageClass defines a "class" of storage with specific characteristics:

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-encrypted
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"    # default if PVC doesn't specify
provisioner: ebs.csi.aws.com         # CSI driver that provisions this storage
volumeBindingMode: WaitForFirstConsumer  # don't bind until pod is scheduled (zone-aware)
reclaimPolicy: Delete
allowVolumeExpansion: true
parameters:
  type: gp3
  encrypted: "true"
  kmsKeyId: arn:aws:kms:us-east-1:123456789:key/my-key
  throughput: "250"     # MB/s
  iops: "3000"
```

```yaml
# EFS StorageClass for RWX
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: efs-sc
provisioner: efs.csi.aws.com
parameters:
  provisioningMode: efs-ap           # EFS Access Point per PVC
  fileSystemId: fs-12345678
  directoryPerms: "700"
  basePath: /dynamic-provisioning
```

## 2. VolumeBindingMode

```text
Immediate (default):
  PV provisioned when PVC is created
  Problem: disk provisioned in wrong AZ if pod lands in different AZ

WaitForFirstConsumer (recommended):
  PV provisioned when pod is scheduled
  Disk created in same AZ as the node the pod lands on
  Prevents cross-AZ attachment errors
```

---

# Topic 4: PersistentVolumeClaim

## 1. PVC Spec

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: payment-db-data
  namespace: prod
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: gp3-encrypted
  resources:
    requests:
      storage: 100Gi
  volumeMode: Filesystem    # or Block (raw block device)
```

## 2. PVC Lifecycle and Binding

```text
Phase: Pending   → waiting for PV to bind
Phase: Bound     → bound to a PV; ready to use
Phase: Released  → PVC deleted; PV reclaim in progress
Phase: Failed    → automatic reclamation failed

When PVC is created:
  1. If storageClass has WaitForFirstConsumer: stays Pending until pod is scheduled
  2. Provisioner creates EBS/EFS volume
  3. PV object created in K8s
  4. PV and PVC bound (one-to-one)
```

## 3. Using PVC in Pod

```yaml
spec:
  containers:
    - name: postgres
      image: postgres:15
      volumeMounts:
        - name: data
          mountPath: /var/lib/postgresql/data
  volumes:
    - name: data
      persistentVolumeClaim:
        claimName: payment-db-data    # must be in same namespace
```

## 4. Volume Expansion (Resize)

```bash
# Patch PVC to request more storage
kubectl patch pvc payment-db-data -p '{"spec":{"resources":{"requests":{"storage":"200Gi"}}}}'

# StorageClass must have allowVolumeExpansion: true
# For filesystem resize: restart pod (or use online expansion if supported)
```

---

# Topic 5: Static Provisioning vs Dynamic Provisioning

## 1. Static Provisioning

Admin pre-creates PV objects. PVC binds to matching PV:

```yaml
# Admin creates PV (pointing to existing NFS share)
apiVersion: v1
kind: PersistentVolume
metadata:
  name: nfs-pv-01
spec:
  capacity:
    storage: 100Gi
  accessModes:
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  nfs:
    server: nfs-server.example.com
    path: /exports/data01

---
# Developer creates PVC (binds to matching PV)
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-nfs-claim
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 100Gi
  storageClassName: ""   # empty: match only static PVs
```

## 2. Dynamic Provisioning

PV created automatically when PVC is created (via StorageClass + CSI driver):

```text
Developer creates PVC with storageClassName: gp3-encrypted
→ K8s calls EBS CSI driver via StorageClass
→ CSI driver calls AWS API to create EBS volume
→ PV object created automatically
→ PVC bound to PV
→ Pod scheduled → EBS volume attached to node → mounted to pod
```

No admin intervention needed. This is the standard in cloud-native K8s.

---

# Topic 6: StatefulSet Storage (volumeClaimTemplates)

Each StatefulSet pod gets its own PVC via `volumeClaimTemplates`:

```yaml
spec:
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: gp3-encrypted
        resources:
          requests:
            storage: 100Gi
```

```text
StatefulSet: postgres with 3 replicas

PVCs created automatically:
  data-postgres-0 (bound to EBS volume in AZ-1)
  data-postgres-1 (bound to EBS volume in AZ-2)
  data-postgres-2 (bound to EBS volume in AZ-3)

If postgres-0 is deleted and rescheduled:
  → same PVC (data-postgres-0) reattached
  → same data, same AZ (pod must land in same AZ or cross-AZ attach)

If StatefulSet is deleted:
  → PVCs NOT deleted automatically
  → Data preserved
  → Must manually delete PVCs to release volumes
```

---

# Topic 7: Volume Snapshots and Cloning

## 1. Volume Snapshot

```yaml
# Create snapshot
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: postgres-backup-2024-01-15
spec:
  volumeSnapshotClassName: csi-aws-vsc
  source:
    persistentVolumeClaimName: data-postgres-0

---
# Restore from snapshot
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-restored
spec:
  accessModes: ["ReadWriteOnce"]
  storageClassName: gp3-encrypted
  resources:
    requests:
      storage: 100Gi
  dataSource:
    name: postgres-backup-2024-01-15
    kind: VolumeSnapshot
    apiGroup: snapshot.storage.k8s.io
```

## 2. PVC Cloning

```yaml
# Clone an existing PVC (CSI driver must support)
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-clone
spec:
  accessModes: ["ReadWriteOnce"]
  storageClassName: gp3-encrypted
  resources:
    requests:
      storage: 100Gi
  dataSource:
    name: data-postgres-0    # source PVC
    kind: PersistentVolumeClaim
```

---

# Topic 8: Interview Traps and Pitfalls

| Trap | Reality |
|---|---|
| "RWX means multiple pods on same node" | RWX = multiple NODES; use RWO if only same-node multi-pod access needed |
| "EBS supports RWX" | EBS is RWO only; use EFS or NFS for RWX |
| "Deleting StatefulSet deletes PVCs" | PVCs survive StatefulSet deletion; must delete manually |
| "PVC in namespace A can use PV in namespace B" | PVs are cluster-scoped; PVCs are namespace-scoped; PVC binds to PV across namespaces |
| "StorageClass Immediate mode works for zone-specific disks" | Use WaitForFirstConsumer for zone-aware provisioning |
| "Volume expansion works without pod restart" | Depends on CSI driver and filesystem; most require pod restart for filesystem resize |

## 9. Revision Notes

- emptyDir: ephemeral scratch space; tmpfs for speed; deleted with pod
- hostPath: mounts node directory; binds pod to node; use only for DaemonSets
- StorageClass: defines storage type; dynamic provisioning; WaitForFirstConsumer for zone-safety
- PV: actual storage resource; cluster-scoped
- PVC: pod's claim to storage; namespace-scoped; one-to-one with PV
- Access modes: RWO (single node, EBS), RWX (multi-node, EFS/NFS), ROX
- Reclaim policies: Retain (keep data) vs Delete (destroy with PVC)
- StatefulSet: per-pod PVC via volumeClaimTemplates; PVCs not deleted on StatefulSet delete
- Volume snapshots: backup and restore PVCs; requires snapshot-controller

## 10. Official Source Notes

- Volumes: <https://kubernetes.io/docs/concepts/storage/volumes/>
- PersistentVolumes: <https://kubernetes.io/docs/concepts/storage/persistent-volumes/>
- StorageClasses: <https://kubernetes.io/docs/concepts/storage/storage-classes/>
- Volume Snapshots: <https://kubernetes.io/docs/concepts/storage/volume-snapshots/>
- CSI drivers: <https://kubernetes-csi.github.io/docs/>
