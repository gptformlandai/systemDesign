# Kubernetes Interview Master Guide Through Story Mode

> This is the combined Kubernetes guide that ties application lifecycle, networking, storage, scaling, and security into one end-to-end flow. The baseline is a React frontend plus a Java Spring Boot backend. The goal is not to memorize random objects. The goal is to understand why each Kubernetes object appears as the application grows.

---

# Table of Contents

1. [The App on Your Laptop](#1-the-app-on-your-laptop)
2. [Why Kubernetes Exists for This App](#2-why-kubernetes-exists-for-this-app)
3. [The Big Mental Model](#3-the-big-mental-model)
4. [Stage 1: Packaging the App](#4-stage-1-packaging-the-app)
5. [Stage 2: Pod, ReplicaSet, Deployment - The Runtime Lifecycle](#5-stage-2-pod-replicaset-deployment---the-runtime-lifecycle)
6. [Stage 3: Services, DNS, and Ingress - The Traffic Lifecycle](#6-stage-3-services-dns-and-ingress---the-traffic-lifecycle)
7. [Stage 4: ConfigMaps, Secrets, and ServiceAccounts - The Configuration and Identity Lifecycle](#7-stage-4-configmaps-secrets-and-serviceaccounts---the-configuration-and-identity-lifecycle)
8. [Stage 5: PV, PVC, StorageClass, and StatefulSet - The Data Lifecycle](#8-stage-5-pv-pvc-storageclass-and-statefulset---the-data-lifecycle)
9. [Stage 6: Probes, Requests, Limits, HPA, and Cluster Autoscaler - The Scale Lifecycle](#9-stage-6-probes-requests-limits-hpa-and-cluster-autoscaler---the-scale-lifecycle)
10. [Stage 7: Security - The Protection Lifecycle](#10-stage-7-security---the-protection-lifecycle)
11. [Stage 8: Zero-Downtime Delivery and Rollback](#11-stage-8-zero-downtime-delivery-and-rollback)
12. [The Full End-to-End Production Flow](#12-the-full-end-to-end-production-flow)
13. [How the YAML Objects Relate to Each Other](#13-how-the-yaml-objects-relate-to-each-other)
14. [What Happens When You Run kubectl apply](#14-what-happens-when-you-run-kubectl-apply)
15. [Common Interview Traps](#15-common-interview-traps)
16. [Interview-Ready Answers](#16-interview-ready-answers)
17. [Quick Revision Sheet](#17-quick-revision-sheet)

---

# 1. The App on Your Laptop

Your local setup is usually:

```text
Browser
  ↓
React app on localhost:3000
  ↓
Spring Boot app on localhost:8080
  ↓
PostgreSQL on localhost:5432
```

This feels easy because:

- one machine exists
- one network exists
- one disk exists
- restart means restart the process yourself
- scaling is not real yet

On your laptop, there is no meaningful distinction between lifecycle, traffic, storage, scaling, and security. In Kubernetes, they become separate concerns.

---

# 2. Why Kubernetes Exists for This App

As soon as the app moves to production, five classes of problems appear.

```text
1. Runtime problem:
   How do I keep the app running if a container or node fails?

2. Traffic problem:
   How do users reach the app, and how do services find each other?

3. Data problem:
   How do I persist database or file data across Pod restarts?

4. Scale problem:
   How do I scale Pods and nodes under real traffic?

5. Security problem:
   How do I separate identity, secrets, permissions, and network access?
```

Kubernetes is valuable because it gives a declarative control plane for all five.

---

# 3. The Big Mental Model

This is the simplest master map.

```text
Container      = your packaged process
Pod            = one running instance of that container
ReplicaSet     = keeps Pod count correct
Deployment     = manages ReplicaSets and app rollouts
Service        = stable network identity for Pods
Ingress        = external HTTP entry into Services
ConfigMap      = non-sensitive configuration
Secret         = sensitive configuration
ServiceAccount = Pod identity for calling cluster APIs
PVC            = app request for durable storage
PV             = actual persistent storage resource
StatefulSet    = stateful controller with stable identity and storage
Probe          = health and traffic readiness signals
HPA            = scales Pod count
Cluster Autoscaler = scales node count
RBAC           = permission model
NetworkPolicy  = Pod-level traffic firewall
```

If you want the whole app in one picture:

```text
Internet
  ↓
Ingress
  ↓
Service
  ↓
Deployment-managed Pods
  ↓
ConfigMap + Secret + ServiceAccount injected into Pods
  ↓
Pod calls database or storage through stable endpoints
  ↓
PVC/PV or managed database preserve data
  ↓
HPA scales Pods, Cluster Autoscaler scales nodes
  ↓
RBAC and NetworkPolicy constrain permissions and traffic
```

---

# 4. Stage 1: Packaging the App

Before Kubernetes can run anything, the application must be packaged.

For Spring Boot, that usually means a Docker image.

Real-life analogy:

```text
Your source code is the recipe.
The container image is the sealed lunch box.
Kubernetes only runs lunch boxes, not raw ingredients.
```

Typical flow:

```text
code → JAR → container image → registry → Kubernetes pulls image
```

This matters because every later object points to the image as its executable artifact.

---

# 5. Stage 2: Pod, ReplicaSet, Deployment - The Runtime Lifecycle

## 5.1 Pod

Pod is the smallest runnable unit.

For most Spring Boot services:

```text
1 Pod = 1 Spring Boot container
```

But a Pod alone is not enough for real application management.

## 5.2 ReplicaSet

ReplicaSet says:

```text
"I need exactly N copies of this Pod running."
```

If one Pod dies, ReplicaSet replaces it.

## 5.3 Deployment

Deployment manages ReplicaSets and adds:

- rolling updates
- rollback
- revision history
- declarative scaling

Real-life analogy:

```text
Pod         = worker
ReplicaSet  = shift manager
Deployment  = operations manager
```

For stateless Spring Boot APIs, Deployment is the normal controller.

## 5.4 The Key Interview Distinction

```text
Pod runs the app.
ReplicaSet keeps the number of Pods correct.
Deployment manages updates and rollback by controlling ReplicaSets.
```

---

# 6. Stage 3: Services, DNS, and Ingress - The Traffic Lifecycle

## 6.1 Why Services Exist

Pods are ephemeral.
Pod IPs change.
Traffic cannot safely target Pod IPs directly.

So Kubernetes introduces a Service.

Service provides:

- stable virtual IP
- stable DNS name
- load balancing across selected healthy Pods

For example:

```text
frontend Pod → http://backend-service
```

not:

```text
frontend Pod → 10.244.2.8
```

## 6.2 DNS

CoreDNS resolves names like:

```text
backend-service
postgres-service
redis-service
```

That is how Spring Boot should find internal dependencies.

Example:

```properties
inventory.base-url=http://inventory-service:8080
spring.datasource.url=jdbc:postgresql://postgres-service:5432/appdb
```

## 6.3 Ingress

Ingress handles external HTTP entry.

Flow:

```text
Browser
  ↓
Ingress Controller
  ↓
Service
  ↓
Pod
```

Important distinction:

```text
Ingress routes to Services.
Services route to Pods.
```

## 6.4 Egress

When Spring Boot calls Stripe, Twilio, or some external API, that is egress.

```text
Pod → external destination
```

Egress is a traffic direction, not usually one single Kubernetes resource like Ingress.

## 6.5 The Interview Summary

```text
Ingress = outside to inside
Service = stable internal access to Pods
DNS = name resolution for Services and Pods
Egress = inside to outside
```

---

# 7. Stage 4: ConfigMaps, Secrets, and ServiceAccounts - The Configuration and Identity Lifecycle

## 7.1 ConfigMaps

ConfigMap stores non-sensitive config.

Examples:

- environment-specific URLs
- log level
- feature flags
- Spring profiles

## 7.2 Secrets

Secret stores sensitive values.

Examples:

- DB password
- JWT key
- API tokens
- TLS data

Important interview nuance:

```text
Secrets are base64 encoded, not strongly encrypted by default.
You should enable etcd encryption at rest and often use an external secret manager.
```

## 7.3 ServiceAccount

ServiceAccount is the identity of the Pod when it talks to the Kubernetes API.

Important point:

```text
Do not let production Pods casually use the default ServiceAccount.
Use dedicated ServiceAccounts with least privilege.
```

## 7.4 Why These Belong Together

Because when a Pod starts, it needs:

- configuration
- secrets
- identity

These are different concerns and should not be hardcoded into the image.

---

# 8. Stage 5: PV, PVC, StorageClass, and StatefulSet - The Data Lifecycle

## 8.1 Why Data Changes Everything

Stateless Pods can die freely.
Stateful workloads cannot.

Container filesystems are not durable storage contracts.

## 8.2 PV, PVC, and StorageClass

```text
PV           = actual durable storage resource
PVC          = application request for that storage
StorageClass = defines how the storage gets provisioned
```

Flow:

```text
Pod mounts PVC
PVC binds PV
PV points to actual cloud or on-prem storage
```

## 8.3 Deployment vs StatefulSet

Use Deployment when replicas are interchangeable.

Use StatefulSet when replicas need:

- stable identity
- stable per-Pod storage
- ordered startup/shutdown
- deterministic peer discovery

## 8.4 The Spring Boot Distinction

This is one of the most important interview points:

```text
Spring Boot API with external PostgreSQL is usually still a Deployment.
The database itself may be a StatefulSet or a managed external service.
```

Do not convert every app that touches a database into a StatefulSet.

## 8.5 Headless Service

StatefulSets often pair with a Headless Service so Pods get stable DNS names like:

```text
postgres-0.postgres-headless
postgres-1.postgres-headless
postgres-2.postgres-headless
```

---

# 9. Stage 6: Probes, Requests, Limits, HPA, and Cluster Autoscaler - The Scale Lifecycle

## 9.1 Probes

There are three probes.

```text
Startup   = has the app finished booting?
Readiness = should it receive traffic now?
Liveness  = is it alive or should kubelet restart it?
```

For Spring Boot, startup and readiness are especially important because JVM apps start slower than many lighter runtimes.

## 9.2 Requests and Limits

Requests are used by the scheduler.
Limits are enforced at runtime.

Critical distinction:

```text
CPU overuse     → throttling
Memory overuse  → OOMKilled
```

## 9.3 HPA

HPA scales Pod count based on metrics.

Flow:

```text
traffic rises
  ↓
CPU or custom metric rises
  ↓
HPA increases Deployment replicas
  ↓
ReplicaSet creates more Pods
```

Important requirement:

```text
HPA needs resource requests to calculate utilization correctly.
```

## 9.4 Cluster Autoscaler

If HPA creates more Pods but there is no room on nodes:

```text
Pods stay Pending
  ↓
Cluster Autoscaler adds nodes
  ↓
new nodes join cluster
  ↓
Pending Pods get scheduled
```

## 9.5 The Scale Story in One Line

```text
HPA scales Pods. Cluster Autoscaler scales nodes.
```

---

# 10. Stage 7: Security - The Protection Lifecycle

Security in Kubernetes is layered.

## 10.1 RBAC

RBAC controls who can do what on which resources.

Objects:

- Role
- ClusterRole
- RoleBinding
- ClusterRoleBinding

This is the permission system for users, service accounts, and controllers.

## 10.2 Pod Security

At the Pod level, you harden containers using `securityContext`.

Typical production settings:

- run as non-root
- drop Linux capabilities
- read-only root filesystem
- disallow privilege escalation

## 10.3 NetworkPolicy

NetworkPolicy is the Pod-level firewall.

It restricts:

- ingress traffic into Pods
- egress traffic out of Pods

Important distinction:

```text
Ingress resource handles external HTTP routing.
NetworkPolicy ingress handles Pod-level traffic permission.
```

These are not the same thing.

## 10.4 Secrets Security

Secrets should not be treated as magically secure just because they are named Secret.

Hardening means:

- encryption at rest
- least privilege RBAC
- careful mounting method
- external secret managers when possible

---

# 11. Stage 8: Zero-Downtime Delivery and Rollback

A production-ready rollout needs more than changing the image tag.

## 11.1 Rolling Update

Key settings:

```text
maxUnavailable: 0
maxSurge: 1
```

That means new Pods come up before old Pods are removed.

## 11.2 Why Readiness Matters

Without readiness probes, Kubernetes may route traffic to a Pod too early.

So the true zero-downtime pattern is:

```text
rolling update + readiness probe + graceful shutdown + preStop + PDB
```

## 11.3 Rollback

Deployments keep old ReplicaSets.
Rollback means scaling the healthy old revision back up.

```bash
kubectl rollout undo deployment/order-service
```

---

# 12. The Full End-to-End Production Flow

This is the combined story.

```text
1. Developer pushes code.
2. CI builds the Spring Boot JAR and container image.
3. CD updates Deployment image tag in Kubernetes.
4. API server stores the new desired state.
5. Deployment creates a new ReplicaSet.
6. ReplicaSet creates Pods.
7. Scheduler places Pods on nodes using requests and constraints.
8. kubelet pulls image, mounts ConfigMaps, Secrets, and PVCs, and starts containers.
9. Startup probe protects boot phase.
10. Readiness probe passes, so Service includes the Pod in endpoints.
11. Ingress sends user traffic to the Service, which routes to healthy Pods.
12. Spring Boot calls internal dependencies via Service DNS names.
13. Spring Boot calls external systems via egress path.
14. HPA scales Pods if metrics rise.
15. Cluster Autoscaler adds nodes if Pods cannot be scheduled.
16. Stateful workloads preserve identity and storage via StatefulSet + PVC/PV.
17. RBAC, ServiceAccounts, Secrets, Pod security, and NetworkPolicies enforce security.
18. If the new version fails readiness, rollout stalls and rollback remains possible.
```

That is the entire Kubernetes application story in one flow.

---

# 13. How the YAML Objects Relate to Each Other

This relationship map is the bridge between theory and manifests.

```text
Namespace
  ├─ ServiceAccount
  ├─ ConfigMap
  ├─ Secret
  ├─ Deployment
  │    └─ Pod template
  │         ├─ containers
  │         ├─ env from ConfigMap/Secret
  │         ├─ probes
  │         ├─ resources
  │         └─ serviceAccountName
  ├─ Service
  │    └─ selects Pods by labels from Deployment template
  ├─ Ingress
  │    └─ routes external traffic to Service
  ├─ HPA
  │    └─ targets Deployment
  ├─ NetworkPolicy
  │    └─ governs traffic to/from selected Pods
  ├─ PVC
  │    └─ binds to PV
  └─ StatefulSet
       ├─ Pod template
       ├─ Headless Service for stable DNS
       └─ volumeClaimTemplates create one PVC per Pod
```

If you understand that map, most Kubernetes YAML becomes much easier to reason about.

---

# 14. What Happens When You Run kubectl apply

This is the internal control-plane story.

```text
kubectl sends desired state to kube-apiserver
  ↓
API server authenticates and authorizes the request
  ↓
admission controllers validate or mutate it
  ↓
object is stored in etcd
  ↓
controllers observe the change
  ↓
Deployment controller creates/updates ReplicaSet
  ↓
ReplicaSet controller creates Pods
  ↓
Scheduler assigns Pods to nodes
  ↓
kubelet starts containers and mounts storage/config
  ↓
Service endpoints update when readiness passes
  ↓
Ingress and Service routing start sending traffic
```

The senior-level principle is:

```text
Kubernetes is declarative and reconciliation-driven.
You declare desired state. Controllers converge actual state toward it.
```

---

# 15. Common Interview Traps

## 15.1 "Every app with a DB should be a StatefulSet"

Wrong.

```text
Usually the API is a Deployment.
The database may be a StatefulSet or managed external service.
```

## 15.2 "Ingress and NetworkPolicy ingress are the same"

Wrong.

```text
Ingress resource = external HTTP routing
NetworkPolicy ingress = Pod firewall rule
```

## 15.3 "Secrets are encrypted by default"

Wrong.

They are base64 encoded unless stronger controls are enabled.

## 15.4 "HPA alone solves scaling"

Wrong.

If nodes are full, HPA can create Pending Pods. Cluster Autoscaler may be needed.

## 15.5 "Service is a process"

Wrong.

Service is an abstraction backed by endpoint lists and kube-proxy rules.

---

# 16. Interview-Ready Answers

## 16.1 "Walk me through a production Spring Boot app on Kubernetes"

```text
I package the app as a container image and run it using a Deployment because the API itself is stateless. The Deployment manages ReplicaSets, which maintain the desired number of Pods. A Service provides stable internal access to those Pods, and Ingress exposes the application externally. ConfigMaps and Secrets provide configuration and sensitive values, while a dedicated ServiceAccount gives the Pod identity. Health probes control startup, readiness, and liveness. Resource requests and limits define scheduling and runtime boundaries. HPA scales Pods, and Cluster Autoscaler scales nodes if needed. For databases or other stateful systems, I use managed services or StatefulSets with PVCs and PVs. RBAC, Pod security, Secrets handling, and NetworkPolicies enforce security.
```

## 16.2 "How do all the main K8s objects relate to each other?"

```text
Deployment manages ReplicaSets and Pods. Service selects those Pods by labels and gives them stable access. Ingress routes external traffic to Services. ConfigMaps and Secrets inject configuration into Pods. ServiceAccounts give Pods identity. HPA targets a Deployment to adjust replica count. PVCs attach durable storage to Pods and bind to PVs. StatefulSet is used instead of Deployment when Pods need stable identity and per-Pod storage.
```

## 16.3 "What are the top things that make a K8s setup production-ready?"

```text
Health probes, resource requests and limits, rolling update strategy, graceful shutdown, PDBs, least-privilege ServiceAccounts, Secrets handling, NetworkPolicies, observability, and a clear scaling model with HPA plus node autoscaling where needed.
```

---

# 17. Quick Revision Sheet

## The Core Chain

```text
Image → Pod → ReplicaSet → Deployment
Client → Ingress → Service → Pod
Pod → PVC → PV
HPA → Deployment
NetworkPolicy → selected Pods
RBAC → identities and resource permissions
```

## The Decision Rules

```text
Stateless app                → Deployment
Stateful clustered system    → StatefulSet
Internal stable access       → Service
External HTTP entry          → Ingress
Durable storage request      → PVC
Runtime scale                → HPA
Node scale                   → Cluster Autoscaler
Pod identity to K8s API      → ServiceAccount
Permission model             → RBAC
Pod traffic firewall         → NetworkPolicy
```

## Gold Standard Summary

```text
Kubernetes manages an application through connected control loops. Deployments manage stateless application rollout and scaling through ReplicaSets and Pods. Services and Ingress manage traffic into and within the cluster. ConfigMaps, Secrets, and ServiceAccounts provide configuration and identity. PVCs, PVs, StorageClasses, and StatefulSets handle durable state when needed. Probes, resource requests, limits, HPA, and Cluster Autoscaler manage health and scale. RBAC, Pod security, Secrets handling, and NetworkPolicies secure the system. Understanding how these pieces connect is what turns Kubernetes from a list of objects into one coherent production platform.
```