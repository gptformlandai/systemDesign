# Kubernetes Storage, StatefulSets, PV, PVC, and Databases Through Story Mode

> Your Spring Boot app works fine when everything is stateless. Then you add PostgreSQL, file storage, caching, search, and background jobs that write data. Now containers restarting is no longer a harmless event. This guide explains Kubernetes storage and StatefulSets in story mode, with a Spring Boot application as the baseline and a strong focus on interview decisions.

---

# Table of Contents

1. [How Storage Feels on Your Laptop](#1-how-storage-feels-on-your-laptop)
2. [What Breaks in Kubernetes](#2-what-breaks-in-kubernetes)
3. [The Big Mental Model](#3-the-big-mental-model)
4. [Stage 1: Stateless App with a Deployment](#4-stage-1-stateless-app-with-a-deployment)
5. [Stage 2: Why Container Filesystems Are Not Real Persistence](#5-stage-2-why-container-filesystems-are-not-real-persistence)
6. [Stage 3: Volumes, PV, PVC, and StorageClass](#6-stage-3-volumes-pv-pvc-and-storageclass)
7. [Stage 4: Databases Enter the Story](#7-stage-4-databases-enter-the-story)
8. [Stage 5: Why Deployments Are Wrong for Many Databases](#8-stage-5-why-deployments-are-wrong-for-many-databases)
9. [Stage 6: StatefulSet - Stable Identity and Stable Storage](#9-stage-6-statefulset---stable-identity-and-stable-storage)
10. [When to Use StatefulSet Instead of Deployment](#10-when-to-use-statefulset-instead-of-deployment)
11. [When Deployment Is Still the Right Choice](#11-when-deployment-is-still-the-right-choice)
12. [Database Patterns for Spring Boot Apps](#12-database-patterns-for-spring-boot-apps)
13. [StatefulSet Networking and Headless Services](#13-statefulset-networking-and-headless-services)
14. [Access Modes, Reclaim Policies, and Storage Tradeoffs](#14-access-modes-reclaim-policies-and-storage-tradeoffs)
15. [Migrations, Backups, and Operations](#15-migrations-backups-and-operations)
16. [Common Mistakes and Troubleshooting](#16-common-mistakes-and-troubleshooting)
17. [Interview-Ready Answers](#17-interview-ready-answers)
18. [Quick Revision Sheet](#18-quick-revision-sheet)

---

# 1. How Storage Feels on Your Laptop

On your laptop, your Spring Boot app usually looks like this:

```text
React frontend        → localhost:3000
Spring Boot backend   → localhost:8080
PostgreSQL            → localhost:5432
uploads folder        → ./uploads or local disk
```

Local persistence feels easy because:

- your process and your disk are on the same machine
- `localhost` never changes
- restarting the app does not wipe your database files if they are on your machine
- you rarely think about volume attachment, node movement, or data identity

That creates a false sense of simplicity.

---

# 2. What Breaks in Kubernetes

In Kubernetes, containers are ephemeral.

That means:

```text
Pod dies → new Pod may come up on a different node
container restarts → writable layer is reset
Pod name may change → network identity may change
```

If your Spring Boot app writes important data into the container filesystem:

```text
/tmp/uploads
/var/lib/postgresql/data
/app/generated-files
```

that data is not safely persistent.

So the moment you introduce databases or durable files, the storage model must change.

---

# 3. The Big Mental Model

If you remember only one storage model, remember this:

```text
Volume        = storage mounted into a Pod
PV            = actual persistent storage resource in the cluster
PVC           = a request for persistent storage
StorageClass  = template/rules for dynamic storage provisioning
Deployment    = best for stateless workloads
StatefulSet   = best for stateful workloads needing stable identity and stable storage
Headless Svc  = gives each StatefulSet Pod stable DNS identity
```

The core flow is:

```text
Pod needs storage
   ↓
Pod mounts a PVC
   ↓
PVC binds to a PV
   ↓
PV points to actual storage like EBS, EFS, NFS, GCE PD, Azure Disk
```

For StatefulSets:

```text
StatefulSet
   ↓
volumeClaimTemplates
   ↓
one PVC per Pod
   ↓
stable Pod identity + stable disk identity
```

---

# 4. Stage 1: Stateless App with a Deployment

Start with the easy part.

Your Spring Boot API is stateless.
It reads and writes data to an external database, but the app itself does not own durable local state.

That means a `Deployment` is perfect.

Real-life analogy:

```text
A stateless app server is like a call center agent.
If one agent goes home, another agent can immediately take over.
The real customer record is stored in a central system, not in the agent's notebook.
```

That is why most Spring Boot APIs run as Deployments.

Example:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: myrepo/order-service:v1
          ports:
            - containerPort: 8080
```

So the first interview question is always:

```text
Is this workload actually stateful, or is it only consuming an external stateful system?
```

If it is only consuming one, Deployment is usually enough.

---

# 5. Stage 2: Why Container Filesystems Are Not Real Persistence

## 5.1 The Story

A developer says:

```text
"We can just store uploaded files inside the container."
```

It works in testing.
Then the Pod restarts.
Files disappear.

That is the first storage lesson in Kubernetes.

## 5.2 Why This Happens

Every container has a writable filesystem layer, but it is tied to the container lifecycle.

If the Pod is recreated:

- that layer is not a durable storage contract
- the new Pod may run on a different node
- your data may be gone

## 5.3 The Right Mental Model

```text
Container filesystem = temporary workspace
Persistent Volume    = real durable storage
```

This is why:

- temp files may use `emptyDir`
- important files should use persistent storage
- databases should not rely on plain container writable layers

---

# 6. Stage 3: Volumes, PV, PVC, and StorageClass

## 6.1 Volume

A volume is storage mounted into a Pod.

Important types:

- `emptyDir` for temporary data shared inside a Pod
- `configMap` and `secret` volumes for configuration files
- persistent volumes through PVCs for durable data

## 6.2 Persistent Volume (PV)

Real storage resource in the cluster.

Examples:

- EBS volume in AWS
- NFS share
- EFS filesystem
- Azure Disk
- GCE Persistent Disk

Real-life analogy:

```text
PV is the actual apartment unit.
It exists independently of the tenant.
```

## 6.3 Persistent Volume Claim (PVC)

PVC is what the application asks for.

Example:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-data
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```

Real-life analogy:

```text
PVC is the rental application.
The app says: I need 20Gi of durable storage with this access mode.
```

## 6.4 PV and PVC Relationship

```text
App does not usually mount PV directly.
App mounts PVC.
PVC binds to PV.
```

That indirection matters because it separates application intent from infrastructure implementation.

## 6.5 StorageClass

StorageClass defines how storage should be dynamically created.

Example:

```text
gp3 on AWS
standard SSD on GCP
premium managed disk on Azure
```

Without StorageClass, admins may create PVs manually.
With StorageClass, PVC creation can trigger automatic disk provisioning.

## 6.6 The Most Useful Interview Line

```text
PV is the actual storage resource, PVC is the application's request for storage, and StorageClass defines how that storage should be provisioned.
```

---

# 7. Stage 4: Databases Enter the Story

Now your Spring Boot app adds PostgreSQL.

At first, you think:

```text
"Let us run PostgreSQL in a Pod and be done with it."
```

But databases are different from stateless apps because they care about:

- durable storage
- consistent identity
- startup order
- replication roles
- controlled failover

Your application server only needs a stable database endpoint.
The database itself may need stable node identity.

That is exactly where StatefulSet appears.

---

# 8. Stage 5: Why Deployments Are Wrong for Many Databases

## 8.1 What Deployment Assumes

Deployment assumes Pods are interchangeable.

That is perfect for stateless workloads.
It is often wrong for clustered databases and brokers.

## 8.2 What Goes Wrong with a Deployment for a Database

If you run a replicated database with a Deployment:

- Pod names are not stable
- Pod identity is not stable
- storage attachment is not naturally one-volume-per-replica
- startup and shutdown order are not guaranteed the way a clustered data system often needs

Real-life analogy:

```text
Deployment treats all workers like interchangeable support staff.
Databases often need named roles like primary, replica-1, replica-2.
Those are not interchangeable identities.
```

## 8.3 Important Nuance

A single-instance database Pod with a PVC can technically be run via Deployment.
But for serious stateful systems, StatefulSet is usually the better model.

That nuance makes your interview answer stronger.

---

# 9. Stage 6: StatefulSet - Stable Identity and Stable Storage

## 9.1 The Story

You now need a PostgreSQL cluster or a Kafka cluster.
Each node has its own identity.
Each node must get its own disk.

That is where StatefulSet fits.

Real-life analogy:

```text
StatefulSet is like assigning fixed offices to named executives.

CEO office = room 1
CFO office = room 2
CTO office = room 3

If the CTO leaves and comes back, they return to the same office.
You do not randomly reassign everything every time.
```

## 9.2 What StatefulSet Guarantees

StatefulSet provides:

- stable Pod names like `postgres-0`, `postgres-1`, `postgres-2`
- stable network identity for each Pod
- stable storage per Pod through `volumeClaimTemplates`
- ordered startup and shutdown

## 9.3 StatefulSet Example

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
spec:
  serviceName: postgres-headless
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
          image: postgres:16
          ports:
            - containerPort: 5432
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes:
          - ReadWriteOnce
        resources:
          requests:
            storage: 50Gi
```

## 9.4 What This Creates

```text
postgres-0 → PVC data-postgres-0 → PV disk A
postgres-1 → PVC data-postgres-1 → PV disk B
postgres-2 → PVC data-postgres-2 → PV disk C
```

If `postgres-1` dies:

```text
new postgres-1 Pod comes back
it reattaches to data-postgres-1
same identity, same storage
```

That is the central StatefulSet promise.

---

# 10. When to Use StatefulSet Instead of Deployment

Use StatefulSet when the workload needs one or more of these:

## 10.1 Stable Pod Identity

Examples:

- PostgreSQL replica sets
- MongoDB replica members
- Kafka brokers
- ZooKeeper nodes
- Elasticsearch nodes

If a node must be known as `node-0` or `broker-2`, StatefulSet is the right fit.

## 10.2 Stable Per-Pod Storage

If each replica must keep its own data volume:

- StatefulSet

If all replicas are stateless and can be replaced freely:

- Deployment

## 10.3 Ordered Startup and Shutdown

Some systems care about bringing nodes up in sequence.

Examples:

- clustered databases
- consensus systems
- brokers with leader/follower coordination

## 10.4 Stateful Peer Discovery

If each replica must discover the others by deterministic DNS names:

```text
db-0.db-headless
db-1.db-headless
db-2.db-headless
```

use StatefulSet.

## 10.5 Strong Interview Sentence

```text
I choose StatefulSet when Pods are not interchangeable and each replica needs stable identity, stable storage, or ordered lifecycle semantics.
```

---

# 11. When Deployment Is Still the Right Choice

This is just as important.

Do not overuse StatefulSet.

Use Deployment when:

- app instances are interchangeable
- no per-Pod durable identity is needed
- storage is external to the app
- scaling can happen in arbitrary order
- rolling updates should treat replicas as generic stateless workers

For most Spring Boot business APIs:

```text
Deployment for the API
Managed database outside the cluster or dedicated stateful data tier
```

Examples where Deployment remains right:

- Spring Boot REST API
- React frontend
- auth gateway
- worker that consumes queue messages but stores state elsewhere
- cache client app that itself is stateless

Important nuance:

```text
Your Spring Boot app using PostgreSQL does NOT mean your app should become a StatefulSet.
Usually only the database would need StatefulSet, not the API.
```

That distinction matters a lot.

---

# 12. Database Patterns for Spring Boot Apps

## 12.1 The Most Common Production Pattern

```text
Spring Boot app        → Deployment
PostgreSQL/MySQL       → managed cloud DB or StatefulSet
Redis                  → managed service or StatefulSet depending on architecture
File uploads           → object storage, not local Pod disk
```

This is usually the cleanest design.

## 12.2 Pattern A: Managed Database Outside Kubernetes

This is often the best answer in interviews when the company is on cloud.

Why?

- operationally simpler
- backups, failover, patching often handled by provider
- application remains fully stateless in Kubernetes

Spring Boot config example:

```properties
spring.datasource.url=jdbc:postgresql://my-rds-endpoint:5432/appdb
spring.datasource.username=appuser
spring.datasource.password=${DB_PASSWORD}
```

## 12.3 Pattern B: Database Inside Kubernetes via StatefulSet

Use this when:

- self-managed data platform is required
- on-prem or hybrid environment
- you need in-cluster data services
- operators exist to manage replication/failover/backups

Examples:

- PostgreSQL operator
- MongoDB operator
- Strimzi for Kafka

## 12.4 Pattern C: Single Instance Dev/Test Database

For dev or test, a simple database with one PVC may be enough.

But be careful not to treat that as production architecture.

---

# 13. StatefulSet Networking and Headless Services

StatefulSets almost always pair with a **Headless Service**.

## 13.1 Why

Normal Service gives one virtual IP.
That is great when you want generic load balancing.

But stateful clustered systems often need to know each peer individually.

That is why Headless Service exists.

## 13.2 Example

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-headless
spec:
  clusterIP: None
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
```

This gives DNS entries like:

```text
postgres-0.postgres-headless.default.svc.cluster.local
postgres-1.postgres-headless.default.svc.cluster.local
postgres-2.postgres-headless.default.svc.cluster.local
```

That is how replicas discover each other.

## 13.3 Interview Summary

```text
Deployment usually wants generic Service load balancing.
StatefulSet often wants Headless Service for stable per-Pod DNS identities.
```

---

# 14. Access Modes, Reclaim Policies, and Storage Tradeoffs

## 14.1 Access Modes

### ReadWriteOnce (RWO)

```text
One node mounts read-write.
Very common for block storage like EBS.
Common for single database instance or one Pod per volume.
```

### ReadOnlyMany (ROX)

```text
Many nodes mount read-only.
Less common for application write workloads.
```

### ReadWriteMany (RWX)

```text
Many nodes mount read-write.
Used with shared filesystems like EFS or NFS.
Useful when multiple Pods need the same shared files.
```

## 14.2 Reclaim Policies

### Delete

```text
When PVC is deleted, underlying storage is deleted too.
Convenient, but risky if used carelessly for important data.
```

### Retain

```text
Storage stays even after PVC deletion.
Safer for critical data because accidental PVC deletion does not immediately destroy the disk.
```

## 14.3 Interview Tradeoff Examples

```text
Database data volume   → often RWO, often Retain
Shared uploads folder  → often RWX
Temporary scratch data → emptyDir, not PV/PVC
```

---

# 15. Migrations, Backups, and Operations

Storage is not only about mounting disks.
The lifecycle also includes operations.

## 15.1 Schema Migrations

For Spring Boot, schema migrations usually use:

- Flyway
- Liquibase

Best practice:

```text
Do not make every app replica fight to run migrations.
Use a controlled migration step, init container, Job, or deployment pipeline stage.
```

## 15.2 Backups

A PVC is not a backup.

That is a critical interview sentence.

```text
Persistent storage preserves data across Pod restarts.
Backup protects against corruption, accidental deletion, and disaster.
These are different concerns.
```

For databases, you still need:

- snapshots
- logical backups
- point-in-time recovery strategy
- tested restore process

## 15.3 Scaling State

Scaling a Deployment from 3 to 10 is straightforward.
Scaling a StatefulSet is more operationally sensitive because data systems have replication and consistency concerns.

That is why stateful systems often need operators or specialized tooling.

## 15.4 Operators Matter

For serious stateful platforms, operators are often the right production answer.

Examples:

- PostgreSQL operator
- MongoDB operator
- Kafka operator

They automate:

- backups
- failover
- replication setup
- rolling maintenance
- cluster-aware upgrades

---

# 16. Common Mistakes and Troubleshooting

## 16.1 "We stored important data inside the container filesystem"

```text
Problem: data disappears after restart or reschedule.
Fix: move durable data to PV/PVC or external managed storage.
```

## 16.2 "We used Deployment for a clustered stateful system"

```text
Problem: no stable identity, poor storage mapping, awkward peer discovery.
Fix: use StatefulSet and usually a Headless Service.
```

## 16.3 "PVC is Pending"

Check:

- matching `StorageClass` exists
- requested size/access mode can be satisfied
- dynamic provisioner is installed and working
- cluster/cloud permissions allow disk provisioning

## 16.4 "Pod cannot mount volume"

Check:

- event messages in `kubectl describe pod`
- storage class and CSI driver status
- node zone compatibility for attached disk
- access mode mismatch

## 16.5 "Database came back but with wrong data behavior"

Check:

- is the same PVC actually reattached?
- was volume accidentally recreated?
- was reclaim policy `Delete` and data destroyed?
- is replication correctly configured?

## 16.6 Useful Commands

```bash
kubectl get pv
kubectl get pvc
kubectl describe pvc postgres-data
kubectl describe pod postgres-0
kubectl get storageclass
kubectl get statefulset
kubectl get pods -o wide
```

---

# 17. Interview-Ready Answers

## 17.1 "What is the difference between PV and PVC?"

```text
PV is the actual persistent storage resource available to the cluster. PVC is the application's request for storage. The Pod mounts the PVC, and Kubernetes binds that PVC to a suitable PV.
```

## 17.2 "What is a StatefulSet and how is it different from Deployment?"

```text
Deployment is for stateless, interchangeable Pods. StatefulSet is for Pods that need stable identity, stable per-Pod storage, and ordered lifecycle behavior. StatefulSet is commonly used for databases, brokers, and distributed systems.
```

## 17.3 "When would you use StatefulSet instead of Deployment?"

```text
I use StatefulSet when replicas are not interchangeable and each Pod needs stable identity or dedicated storage. Examples include PostgreSQL clusters, MongoDB replica sets, Kafka brokers, ZooKeeper, and Elasticsearch nodes. If the workload is stateless and only consumes external state, I use Deployment.
```

## 17.4 "Should a Spring Boot app with a database run as a StatefulSet?"

```text
Usually no. The Spring Boot API itself is typically stateless and should run as a Deployment. The database may run outside the cluster as a managed service, or if self-hosted in Kubernetes, it may run as a StatefulSet.
```

## 17.5 "Why does StatefulSet use a Headless Service?"

```text
Because stateful replicas often need stable per-Pod DNS names rather than generic load balancing. A Headless Service gives direct DNS records like db-0.db-headless, db-1.db-headless, and db-2.db-headless so replicas can identify each other predictably.
```

## 17.6 "Is a PVC enough for database safety?"

```text
No. A PVC gives persistence across Pod restarts, but it is not a backup strategy. You still need backups, snapshots, replication where appropriate, and tested restore procedures.
```

---

# 18. Quick Revision Sheet

## One-Line Mapping

```text
Deployment    = stateless app controller
StatefulSet   = stateful app controller with stable identity/storage
Volume        = storage mounted into a Pod
PV            = actual persistent storage resource
PVC           = request for persistent storage
StorageClass  = rules for dynamic provisioning
Headless Svc  = stable per-Pod DNS for stateful workloads
RWO           = one node read-write
RWX           = many nodes read-write
Retain        = keep storage after PVC deletion
Delete        = delete storage after PVC deletion
```

## The Decision Rule

```text
If Pods are interchangeable → Deployment
If Pods need stable identity or per-Pod durable storage → StatefulSet
```

## The Storage Flow

```text
App needs disk
  ↓
Pod mounts PVC
  ↓
PVC binds PV
  ↓
PV points to actual storage
```

## Gold Standard Answer

```text
In Kubernetes, I use Deployments for stateless Spring Boot services because replicas are interchangeable and state lives outside the Pod. For durable storage, Pods mount PVCs, which bind to PVs, often provisioned dynamically through a StorageClass. I use StatefulSets when the workload itself is stateful and each replica needs stable identity, stable network naming, or dedicated persistent storage, such as PostgreSQL, MongoDB, Kafka, or ZooKeeper. A PVC provides persistence, but it does not replace backups or proper database operations.
```