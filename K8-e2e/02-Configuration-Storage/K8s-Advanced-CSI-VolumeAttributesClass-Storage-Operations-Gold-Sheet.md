# Kubernetes Advanced CSI, VolumeAttributesClass, and Storage Operations Gold Sheet

> Track: K8s Interview Track - Phase 2: Configuration and Storage Plus
> Goal: Move beyond PVC basics into production storage operations: CSI, mutable volume attributes, snapshots, cloning, expansion, topology, and failure handling.

---

## 0. How To Read This

Beginner focus:
- PVC asks for storage.
- PV represents allocated storage.
- StorageClass controls provisioning.

Intermediate focus:
- CSI drivers implement cloud/provider storage behavior.
- Snapshots, clones, expansion, and topology change storage operations.
- `WaitForFirstConsumer` prevents wrong-zone volumes.

Senior / MAANG focus:
- Storage is an availability, data integrity, and cost problem.
- Know which operations are Kubernetes-native and which depend on the CSI driver.
- Plan backups, restore tests, encryption, IOPS, expansion, and zone-aware failover.

---

# Topic 1: CSI Mental Model

```text
Kubernetes core:
  Defines PersistentVolume, PersistentVolumeClaim, StorageClass, VolumeSnapshot,
  and scheduling hooks.

CSI driver:
  Talks to the real storage system: EBS, EFS, Ceph, Portworx, NetApp, GCE PD.

External CSI sidecars:
  provisioner, attacher, resizer, snapshotter, health monitor.
```

Request lifecycle:
```text
1. User creates PVC.
2. External provisioner watches PVC.
3. Driver creates real volume in storage backend.
4. Kubernetes creates PV and binds it to PVC.
5. Scheduler chooses node, considering volume topology.
6. Attach/mount path makes volume available to the pod.
7. Kubelet mounts volume into container.
```

---

# Topic 2: StorageClass vs VolumeAttributesClass

## 1. StorageClass

```text
StorageClass answers:
  "What kind of volume should be provisioned?"

Examples:
  gp3 encrypted EBS, io2 EBS, EFS, Ceph block, local SSD.
```

StorageClass is mostly about provisioning-time parameters.

## 2. VolumeAttributesClass

```text
VolumeAttributesClass answers:
  "What mutable storage attributes can be applied to an existing volume?"

Examples:
  Change IOPS class, throughput class, QoS tier, backend policy.
```

Important distinction:
```text
StorageClass name on a PVC is immutable after creation.
VolumeAttributesClass name on a PVC is mutable, but the class parameters are immutable.
Actual behavior depends on CSI driver support.
```

## 3. YAML Example

```yaml
apiVersion: storage.k8s.io/v1
kind: VolumeAttributesClass
metadata:
  name: gp3-high-throughput
driverName: ebs.csi.aws.com
parameters:
  iops: "6000"
  throughput: "500"
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: payment-db-data
spec:
  accessModes: ["ReadWriteOnce"]
  storageClassName: gp3-encrypted
  volumeAttributesClassName: gp3-high-throughput
  resources:
    requests:
      storage: 500Gi
```

Operational use:
```text
Start normal:
  gp3-standard

Incident or peak season:
  Patch PVC to gp3-high-throughput if driver supports it.

After load drops:
  Patch back to cheaper class after performance validation.
```

---

# Topic 3: Expansion, Snapshots, and Clones

## 1. Volume Expansion

Checklist:
```text
1. StorageClass has allowVolumeExpansion: true.
2. CSI driver supports expansion.
3. Patch PVC storage request upward only.
4. Watch PVC conditions and pod filesystem resize.
5. Do not assume every filesystem supports online expansion.
```

```bash
kubectl patch pvc payment-db-data -n prod \
  -p '{"spec":{"resources":{"requests":{"storage":"750Gi"}}}}'

kubectl describe pvc payment-db-data -n prod
```

## 2. Snapshots

```yaml
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: payment-db-snapshot-2026-07-03
  namespace: prod
spec:
  volumeSnapshotClassName: ebs-csi-snapshot
  source:
    persistentVolumeClaimName: payment-db-data
```

Snapshot use cases:
- Before risky schema migration.
- Backup for restore drill.
- Create test data copies.
- Cross-region DR if provider supports snapshot copy.

## 3. PVC Clone

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: payment-db-clone
  namespace: staging
spec:
  storageClassName: gp3-encrypted
  dataSource:
    kind: PersistentVolumeClaim
    name: payment-db-data
  accessModes: ["ReadWriteOnce"]
  resources:
    requests:
      storage: 500Gi
```

Clone constraints:
- Usually same namespace unless using cross-namespace data source features.
- Same or compatible StorageClass.
- Target capacity must be at least source capacity.
- Depends on CSI driver implementation.

---

# Topic 4: Generic Ephemeral Volumes

Generic ephemeral volumes are per-pod temporary volumes created from a PVC template.

Use when:
- The pod needs scratch space with CSI features.
- `emptyDir` is not enough.
- You want storage-class-backed temporary space that disappears with the pod.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: batch-sorter
spec:
  containers:
    - name: sorter
      image: example/sorter:1.0
      volumeMounts:
        - name: scratch
          mountPath: /scratch
  volumes:
    - name: scratch
      ephemeral:
        volumeClaimTemplate:
          spec:
            accessModes: ["ReadWriteOnce"]
            storageClassName: gp3-encrypted
            resources:
              requests:
                storage: 100Gi
```

---

# Topic 5: Access Modes and Topology

| Access Mode | Meaning | Common Backend |
|---|---|---|
| `ReadWriteOnce` | One node can mount read-write | EBS, GCE PD |
| `ReadWriteOncePod` | One pod can mount read-write | Strong single-writer safety |
| `ReadOnlyMany` | Many nodes read-only | NFS, object-backed systems |
| `ReadWriteMany` | Many nodes read-write | EFS, NFS, CephFS |

Zone trap:
```text
EBS is zonal.
If a volume is in us-east-1a, a pod scheduled to us-east-1b cannot attach it.
Use volumeBindingMode: WaitForFirstConsumer so the volume is created after
the scheduler knows the pod's zone.
```

---

# Topic 6: Failure Modes

| Failure | User Symptom | Root Cause | Fix |
|---|---|---|---|
| PVC Pending | Pod Pending | No StorageClass, quota, or provisioner issue | Describe PVC, check provisioner logs |
| Multi-attach error | Pod stuck ContainerCreating | RWO volume attached to another node | Drain carefully, wait detach, use StatefulSet identity |
| Wrong-zone volume | Pod unschedulable | Immediate binding created PV in wrong AZ | Use WaitForFirstConsumer |
| Expansion stuck | PVC condition never clears | Driver/filesystem limitation | Check CSI resizer logs, driver docs |
| Snapshot succeeds but restore fails | DR drill fails | SnapshotClass or permissions missing | Test restore, not just backup creation |
| Storage cost spike | Cloud bill jumps | Overprovisioned PVCs or high IOPS class | Track PVC capacity, snapshots, and attributes |

---

# Topic 7: Interview Scenario

> A StatefulSet using EBS PVCs keeps failing during node replacement with multi-attach errors. How do you debug and prevent it?

Strong answer:
```text
I would first identify which PVC and node still hold the attachment by checking
events, VolumeAttachment objects, and the CSI controller logs. EBS is RWO and
zonal, so the same volume cannot be mounted read-write on two nodes. I would
avoid force-deleting pods unless I know the old node is gone. For prevention,
I would use StatefulSet identity, PDBs, graceful termination, WaitForFirstConsumer,
and clear runbooks for detach delays. If the workload needs multi-writer storage,
I would choose EFS or another RWX backend instead of EBS.
```

---

# Topic 8: Revision Notes

- CSI is the contract between Kubernetes and real storage systems.
- StorageClass provisions new volumes; VolumeAttributesClass changes mutable attributes on existing volumes when supported.
- Snapshot creation is not a backup strategy until restore is tested.
- EBS is zonal and usually RWO; EFS is multi-AZ and RWX.
- `WaitForFirstConsumer` prevents wrong-zone PV binding.
- Storage incidents need both Kubernetes events and cloud/provider console evidence.

## Official Source Notes

- Persistent Volumes: <https://kubernetes.io/docs/concepts/storage/persistent-volumes/>
- VolumeAttributesClass: <https://kubernetes.io/docs/concepts/storage/volume-attributes-classes/>
- Volume Snapshots: <https://kubernetes.io/docs/concepts/storage/volume-snapshots/>
- CSI Volume Cloning: <https://kubernetes.io/docs/concepts/storage/volume-pvc-datasource/>

