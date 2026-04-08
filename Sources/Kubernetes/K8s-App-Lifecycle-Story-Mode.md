# Kubernetes App Lifecycle Through Story Mode

> You have a React frontend and a Java Spring Boot backend running fine on your laptop. Then the app grows, traffic increases, deployments become risky, configs differ by environment, and scaling manually becomes painful. This guide explains the full Kubernetes app lifecycle in story mode, focused on the topics that matter most in interviews.

---

# Table of Contents

1. [The App on Your Laptop](#1-the-app-on-your-laptop)
2. [Why Kubernetes Enters the Story](#2-why-kubernetes-enters-the-story)
3. [The Big Mental Model](#3-the-big-mental-model)
4. [Stage 1: Your First Pod](#4-stage-1-your-first-pod)
5. [Stage 2: ReplicaSet - Keep It Running](#5-stage-2-replicaset---keep-it-running)
6. [Stage 3: Deployment - Real App Management](#6-stage-3-deployment---real-app-management)
7. [Stage 4: Service - Stable Networking](#7-stage-4-service---stable-networking)
8. [Stage 5: Ingress - One Entry Point for the App](#8-stage-5-ingress---one-entry-point-for-the-app)
9. [Stage 6: ConfigMaps and Secrets - Separate Code from Configuration](#9-stage-6-configmaps-and-secrets---separate-code-from-configuration)
10. [Stage 7: Probes - Know When the App Is Ready](#10-stage-7-probes---know-when-the-app-is-ready)
11. [Stage 8: HPA - Scale When Traffic Grows](#11-stage-8-hpa---scale-when-traffic-grows)
12. [Stage 9: Rolling Update and Rollback](#12-stage-9-rolling-update-and-rollback)
13. [Stage 10: Namespaces and Production Structure](#13-stage-10-namespaces-and-production-structure)
14. [What Actually Happens on kubectl apply](#14-what-actually-happens-on-kubectl-apply)
15. [Putting It All Together: Full App Lifecycle](#15-putting-it-all-together-full-app-lifecycle)
16. [Common Mistakes and Troubleshooting](#16-common-mistakes-and-troubleshooting)
17. [Interview-Ready Answers](#17-interview-ready-answers)
18. [Quick Revision Sheet](#18-quick-revision-sheet)

---

# 1. The App on Your Laptop

On your laptop, life is simple:

```text
React frontend      → http://localhost:3000
Spring Boot backend → http://localhost:8080
PostgreSQL          → localhost:5432
```

You start things manually:

```text
1. start postgres
2. start Spring Boot
3. start React
4. test the app in the browser
```

This works because:

- only one machine exists
- you know where every process is running
- if something crashes, you restart it manually
- scaling means opening another terminal, which is not real scaling

This is fine for development. It is not enough for production.

---

# 2. Why Kubernetes Enters the Story

Once the app goes to production, new problems appear:

```text
Problem 1: The backend crashes at 2 AM.
           You need it to restart automatically.

Problem 2: Traffic grows.
           One backend instance is not enough anymore.

Problem 3: Pods are recreated frequently.
           Their IP addresses keep changing.
           How will the frontend or other services find them?

Problem 4: Production config is different from local config.
           DB URLs, API URLs, secrets, feature flags all differ.

Problem 5: You need zero-downtime deployments.
           Users should not see errors while a new version rolls out.

Problem 6: During traffic spikes, the app should scale out.
           During quiet periods, it should scale back in.
```

Kubernetes solves these problems by taking your application and managing its full runtime lifecycle.

---

# 3. The Big Mental Model

If you remember only one thing, remember this:

```text
Pod         = one running instance of your app
ReplicaSet  = keeps the desired number of Pods alive
Deployment  = manages ReplicaSets and handles updates/rollbacks
Service     = stable network identity in front of Pods
Ingress     = HTTP entry point into the cluster
ConfigMap   = non-sensitive config
Secret      = sensitive config
Probe       = health checks for startup/readiness/liveness
HPA         = scales number of Pods based on load
Namespace   = logical environment boundary
```

The core hierarchy is:

```text
Deployment
   ↓
ReplicaSet
   ↓
Pods
   ↓
Containers
```

For networking:

```text
Ingress → Service → Pods
```

For configuration:

```text
ConfigMap / Secret → mounted into Pods
```

For scaling:

```text
HPA → changes Deployment replica count → ReplicaSet creates more Pods
```

---

# 4. Stage 1: Your First Pod

## 4.1 The Story

You containerize your Spring Boot app and say:

```text
"I just want this app to run inside Kubernetes."
```

The smallest thing Kubernetes can run is a **Pod**.

Real-life analogy:

```text
A Pod is like one apartment.
Inside the apartment, one or more people live together.
They share the same address, same utilities, and same lifecycle.

If the apartment is destroyed, everyone inside goes with it.
```

For most Java apps:

```text
1 Pod = 1 Spring Boot container
```

## 4.2 Why a Pod Alone Is Not Enough

If you create a Pod directly:

- it runs once
- if the Pod dies, Kubernetes does not recreate it automatically in a controlled higher-level way
- if you want 3 copies, you manually create 3 Pods
- if you want to update the image, you are doing it manually

That is why interviews often say:

```text
"You almost never create Pods directly for application workloads."
```

## 4.3 Minimal Pod Example

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: order-service
  labels:
    app: order-service
spec:
  containers:
    - name: order-service
      image: myrepo/order-service:v1
      ports:
        - containerPort: 8080
```

Good for learning. Not good for production lifecycle management.

---

# 5. Stage 2: ReplicaSet - Keep It Running

## 5.1 The Story

Now you say:

```text
"If one Pod dies, I want another one automatically."
"Actually I want 3 identical Pods always running."
```

That is the job of a **ReplicaSet**.

Real-life analogy:

```text
ReplicaSet is like a shift manager.
The manager says: "I need exactly 3 cashiers on duty."

If one cashier leaves, the manager replaces them.
If there are 4 cashiers by mistake, the manager removes one.
```

## 5.2 What ReplicaSet Really Does

```text
Desired state: 3 Pods
Actual state: 2 Pods
Action: create 1 more Pod
```

This is Kubernetes' reconciliation model.

## 5.3 The Important Interview Point

You should know ReplicaSet well, but you usually do **not** create it directly.

Why?

- ReplicaSet only keeps count correct
- it does not manage application version changes elegantly
- it does not give the clean rollout/rollback experience that Deployment gives

So the interview sentence is:

```text
ReplicaSet maintains the number of Pods.
Deployment manages ReplicaSets and gives rolling updates and rollback.
```

---

# 6. Stage 3: Deployment - Real App Management

## 6.1 The Story

Now the app is real.
You need:

- 3 replicas
- safe upgrades
- rollback to previous version
- controlled rollout

That is where **Deployment** enters.

Real-life analogy:

```text
Deployment is like the operations manager.
It does not directly serve customers.
It supervises the shift manager (ReplicaSet), who supervises workers (Pods).
```

## 6.2 Why Deployment Is the Default for Stateless Apps

For your Spring Boot backend or React frontend container:

- you usually use a Deployment
- it creates and manages ReplicaSets
- it keeps revision history
- it supports rolling updates

## 6.3 The Most Important Hierarchy

```text
Deployment: order-service
   ├─ ReplicaSet: order-service-v1
   │    ├─ Pod A
   │    ├─ Pod B
   │    └─ Pod C
   └─ ReplicaSet: old-revision kept for rollback
```

When image changes from `v1` to `v2`:

```text
Old ReplicaSet scales down
New ReplicaSet scales up
```

That is the heart of rolling update.

## 6.4 Deployment Example

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

## 6.5 What to Say in Interviews

```text
Use Deployment for stateless applications.
It manages ReplicaSets and gives rolling updates, rollback, and declarative scaling.
```

---

# 7. Stage 4: Service - Stable Networking

## 7.1 The Story

You now have 3 backend Pods.
Each Pod gets its own IP.
But Pods are replaceable. Their IPs change.

Problem:

```text
How will the frontend reach the backend reliably?
How will one service call another if Pod IPs keep changing?
```

Answer: **Service**.

Real-life analogy:

```text
Pods are people moving between hotel rooms.
Service is the hotel's front desk number.

You do not call room 312 directly because guests move.
You call the front desk, and it routes you to whoever is currently assigned.
```

## 7.2 What Service Does

Service gives:

- a stable virtual IP
- a stable DNS name
- load balancing across matching Pods

Example:

```text
order-service.default.svc.cluster.local
```

This always points to the current healthy Pods selected by labels.

## 7.3 The Four Types You Must Know

### ClusterIP

```text
Internal only.
Used for service-to-service communication inside the cluster.
This is the default and the most common one.
```

### NodePort

```text
Exposes the service on a port on every node.
Mostly used for quick testing, not ideal for production.
```

### LoadBalancer

```text
Creates a cloud load balancer in AWS/GCP/Azure.
Used for external production access.
```

### Headless Service

```text
No ClusterIP.
Returns Pod IPs directly.
Used mainly for StatefulSets.
```

## 7.4 Service Example

```yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

## 7.5 The Critical Interview Sentence

```text
Pods are ephemeral. Services provide stable discovery and load balancing in front of Pods.
```

---

# 8. Stage 5: Ingress - One Entry Point for the App

## 8.1 The Story

Now you have:

- frontend service
- backend service
- maybe auth service

If every service gets its own external load balancer, cost and complexity grow.

So you introduce **Ingress**.

Real-life analogy:

```text
Ingress is the receptionist at the building entrance.

Visitors say:
  "/api/orders"   → send to backend
  "/"             → send to frontend

The receptionist decides where HTTP traffic should go.
```

## 8.2 What Ingress Does

Ingress provides:

- host-based routing
- path-based routing
- TLS termination
- one public entry for multiple internal services

Flow:

```text
User → Ingress → Service → Pod
```

## 8.3 Important Clarification

Ingress resource by itself does nothing.
You need an **Ingress Controller** like NGINX or AWS ALB Controller.

## 8.4 Example

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
spec:
  rules:
    - host: app.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-service
                port:
                  number: 80
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: order-service
                port:
                  number: 80
```

---

# 9. Stage 6: ConfigMaps and Secrets - Separate Code from Configuration

## 9.1 The Story

Local app config is easy:

```text
application.properties
.env file
hardcoded localhost URLs
```

Production is different:

- prod database URL
- prod Redis host
- JWT secret
- API keys
- feature flags

You do not want to rebuild the image for every environment.

That is why you use **ConfigMaps** and **Secrets**.

## 9.2 ConfigMap

ConfigMap stores non-sensitive configuration.

Examples:

- log level
- API base URL
- feature flags
- Spring profile settings

Real-life analogy:

```text
ConfigMap is the instruction sheet pinned on the office wall.
Everyone can read it. It is operational information, not confidential.
```

Example:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-config
data:
  DB_HOST: postgres-service
  LOG_LEVEL: INFO
```

## 9.3 Secret

Secret stores sensitive values.

Examples:

- DB password
- JWT signing key
- API tokens
- TLS keys

Real-life analogy:

```text
Secret is the office locker.
Only authorized people should access it.
```

Example:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: order-secrets
type: Opaque
stringData:
  DB_PASSWORD: super-secret-password
```

## 9.4 The Important Security Point

This matters a lot in interviews:

```text
Kubernetes Secret is base64 encoded, not encrypted by default.
For stronger security, enable etcd encryption at rest and preferably use an external secret manager.
```

## 9.5 Injecting into the Pod

```yaml
env:
  - name: DB_HOST
    valueFrom:
      configMapKeyRef:
        name: order-config
        key: DB_HOST
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: order-secrets
        key: DB_PASSWORD
```

## 9.6 One More Important Interview Detail

```text
ConfigMap/Secret mounted as files can refresh.
ConfigMap/Secret injected as env vars do not hot reload automatically.
Usually the Pod must restart to pick up env var changes.
```

---

# 10. Stage 7: Probes - Know When the App Is Ready

## 10.1 The Story

Your Spring Boot app takes time to start.
If Kubernetes sends traffic too early, users get failures.
If the app hangs, Kubernetes should restart it.

That is why probes exist.

## 10.2 The Three Probes

### Readiness Probe

```text
Question: Can this Pod receive traffic right now?
If it fails: Pod stays running, but is removed from Service endpoints.
```

This is the most important one for zero-downtime deployments.

### Liveness Probe

```text
Question: Is the process alive and healthy?
If it fails: kubelet restarts the container.
```

### Startup Probe

```text
Question: Has the app finished booting?
If it fails repeatedly: container is restarted.
Used for slow-starting apps like Spring Boot.
```

## 10.3 Real-Life Analogy

```text
Startup probe   = "Has the shop opened for the day?"
Readiness probe = "Can customers enter right now?"
Liveness probe  = "Is the shop still functioning or totally stuck?"
```

## 10.4 Spring Boot-Friendly Example

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 20

startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  failureThreshold: 30
  periodSeconds: 2
```

## 10.5 Interview Gold Point

```text
Readiness protects traffic.
Liveness protects process health.
Startup protects slow application boot.
```

---

# 11. Stage 8: HPA - Scale When Traffic Grows

## 11.1 The Story

Now traffic spikes.
Three Pods are not enough.

You do not want manual scaling like this:

```text
kubectl scale deployment/order-service --replicas=10
```

every time load changes.

So you add **HPA**.

Real-life analogy:

```text
HPA is like a restaurant manager watching customer volume.
If the place gets crowded, the manager adds more staff.
If it becomes quiet, the manager reduces staff.
```

## 11.2 How HPA Works

```text
Metrics Server collects CPU/memory usage
      ↓
HPA checks metrics periodically
      ↓
If usage is above target, HPA increases replicas
      ↓
Deployment updates replica count
      ↓
ReplicaSet creates more Pods
```

## 11.3 Example

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
```

## 11.4 The Big Interview Warning

```text
HPA needs resource requests.
Without CPU/memory requests on the container, HPA cannot calculate utilization correctly.
```

## 11.5 Another Important Reality

HPA is not magic.

- it reacts after load rises
- new Pods need startup time
- burst traffic is not handled instantly

So the mature answer is:

```text
HPA improves elasticity, but it is reactive, not instant.
```

---

# 12. Stage 9: Rolling Update and Rollback

## 12.1 The Story

You deploy version `v2`.
If it breaks, users should still be served by `v1`.

That is the purpose of Deployment rolling updates.

## 12.2 What Happens During Rolling Update

```text
Current state:
  ReplicaSet v1 → 3 Pods

Deploy v2:
  Deployment creates new ReplicaSet v2
  v2 Pods come up gradually
  once ready, old v1 Pods are scaled down gradually
```

Key settings:

```text
maxUnavailable = how many old Pods may be unavailable during rollout
maxSurge       = how many extra new Pods may be created temporarily
```

Best practical setting for many APIs:

```text
maxUnavailable: 0
maxSurge: 1
```

## 12.3 Why Readiness Probe Is Critical Here

Without readiness:

- Kubernetes may think the new Pod is ready too soon
- old Pod gets removed
- users get failed requests

So the interview-quality answer is:

```text
Zero-downtime rollout depends on Deployment strategy plus readiness probe.
```

## 12.4 Rollback

If `v2` is broken:

```bash
kubectl rollout undo deployment/order-service
```

Why rollback is possible:

- Deployment keeps previous ReplicaSet revisions
- rollback means scaling the healthy old ReplicaSet back up

## 12.5 Good Production Extras

- `terminationGracePeriodSeconds`
- graceful shutdown in Spring Boot
- `preStop` hook
- PodDisruptionBudget

These are not the first concepts to memorize, but they make your answer stronger in interviews.

---

# 13. Stage 10: Namespaces and Production Structure

## 13.1 The Story

Now the company has:

- dev environment
- staging environment
- prod environment

You do not want all workloads mixed together.

So you use **Namespaces**.

Real-life analogy:

```text
Namespaces are like separate office floors in the same building.
Each floor has its own teams, configs, and access control.
```

## 13.2 Why Namespaces Matter

They help with:

- logical isolation
- environment separation
- RBAC separation
- quotas and limits
- cleaner operations

Typical model:

```text
dev
staging
production
```

## 13.3 Interview Point

```text
Namespaces are not strong security boundaries by themselves.
They are logical isolation boundaries and work best with RBAC, quotas, and policies.
```

---

# 14. What Actually Happens on kubectl apply

This is one of the strongest senior-level Kubernetes explanations.

## 14.1 The Lifecycle

You run:

```bash
kubectl apply -f deployment.yaml
```

Then:

### Step 1: kubectl sends the request to the API server

```text
kubectl authenticates using kubeconfig and sends the Deployment spec.
```

### Step 2: API server validates and stores it

```text
API server authenticates the caller
API server authorizes via RBAC
Admission controllers may mutate/validate the object
Object is stored in etcd
```

### Step 3: Deployment controller reacts

```text
It sees a new Deployment and creates a ReplicaSet.
```

### Step 4: ReplicaSet controller reacts

```text
It sees desired replicas and creates Pod objects.
```

### Step 5: Scheduler assigns Pods to nodes

```text
Pending Pods are matched to suitable worker nodes.
```

### Step 6: Kubelet runs the containers

```text
The node's kubelet pulls the image, mounts volumes,
injects Secrets/ConfigMaps, starts the container, and reports status.
```

### Step 7: Service starts routing traffic

```text
If a Service selects the Pod labels and readiness passes,
the Pod becomes an endpoint behind the Service.
```

## 14.2 The Senior-Level Mental Model

```text
Kubernetes is declarative and controller-driven.
You submit desired state.
Controllers continuously work to make actual state match it.
```

---

# 15. Putting It All Together: Full App Lifecycle

Let us walk the whole application story end to end.

## 15.1 Stage-by-Stage Evolution

### Stage A: Laptop

```text
React on localhost:3000
Spring Boot on localhost:8080
Everything manual
```

### Stage B: Containerized App

```text
Docker image created for backend and frontend
Now the app is portable
```

### Stage C: Single Pod in Kubernetes

```text
The app can run in the cluster, but there is no real lifecycle management.
```

### Stage D: Deployment with 3 Replicas

```text
Now Kubernetes maintains 3 running copies of your backend.
If one dies, another is created.
Rolling update becomes possible.
```

### Stage E: Service in Front

```text
The backend gets a stable DNS identity.
Frontend and other services no longer care about Pod IP changes.
```

### Stage F: Ingress for External Traffic

```text
Users enter through one HTTP entry point.
Ingress routes to frontend and backend services.
```

### Stage G: ConfigMaps and Secrets

```text
Same Docker image runs in dev/staging/prod.
Only config and secrets change per environment.
```

### Stage H: Probes Added

```text
Traffic is sent only to healthy Pods.
Hung containers restart automatically.
Slow Java startup stops causing false failures.
```

### Stage I: HPA Added

```text
Traffic spike → CPU rises → HPA increases replicas.
Traffic drops → HPA reduces replicas.
```

### Stage J: Safe Release Process

```text
New version rolls out gradually.
If broken, rollback to previous ReplicaSet.
```

## 15.2 Final Production Flow

```text
User
  ↓
Ingress
  ↓
frontend-service → frontend Pods
  ↓
order-service → backend Pods managed by Deployment
  ↓
Pods read ConfigMap and Secret
  ↓
Readiness/Liveness/Startup probes protect lifecycle
  ↓
HPA scales replicas based on load
  ↓
Deployment handles rollout and rollback
```

That is the lifecycle story most interviewers want to hear.

---

# 16. Common Mistakes and Troubleshooting

## 16.1 "We created Pods directly"

```text
Problem: no proper rollout, rollback, or higher-level management.
Fix: use Deployment for stateless apps.
```

## 16.2 "Service is not routing traffic"

Check:

```text
1. Does Service selector match Pod labels?
2. Are Pods passing readiness probe?
3. Does kubectl get endpoints show Pod IPs?
```

## 16.3 "App is in CrashLoopBackOff"

Check:

```text
kubectl describe pod <pod>
kubectl logs <pod> --previous
```

Typical causes:

- wrong config
- bad secret
- DB unreachable
- app startup failure
- liveness probe too aggressive
- OOMKilled

## 16.4 "HPA is not scaling"

Check:

- resource requests exist
- Metrics Server is installed
- current load actually exceeds target
- new Pods are able to start

## 16.5 "New rollout caused errors"

Most common reasons:

- readiness probe missing or wrong
- image broken
- config mismatch
- incompatible DB change

## 16.6 Strong Debug Sequence

```text
Pod issue?        → describe pod, logs, events
Service issue?    → get svc, get endpoints, check labels/readiness
Scheduling issue? → check Pending events, node capacity, affinity/taints
Scaling issue?    → check HPA, metrics, resource requests
Rollout issue?    → rollout status, rollout history, undo if needed
```

---

# 17. Interview-Ready Answers

## 17.1 "What is the difference between Pod, ReplicaSet, and Deployment?"

```text
Pod is the smallest deployable unit and runs the container.
ReplicaSet ensures the desired number of Pods are running.
Deployment manages ReplicaSets and adds rolling updates, rollback, and declarative app management.
```

## 17.2 "Why do we need a Service if Pods already have IPs?"

```text
Pods are ephemeral and their IPs change on recreation.
Service gives a stable virtual IP and DNS name, and load balances across healthy Pods.
```

## 17.3 "What is the difference between ConfigMap and Secret?"

```text
ConfigMap stores non-sensitive configuration.
Secret stores sensitive data like passwords or tokens.
Kubernetes Secrets are base64 encoded, not strongly encrypted by default, so production setups should also use encryption at rest and often external secret managers.
```

## 17.4 "What is the difference between readiness and liveness probe?"

```text
Readiness controls whether the Pod should receive traffic.
Liveness controls whether the container should be restarted.
Startup probe protects slow-booting apps before liveness kicks in.
```

## 17.5 "How does HPA work?"

```text
HPA reads metrics like CPU or memory, compares them to target utilization, and changes the Deployment replica count. The Deployment then creates or removes Pods through its ReplicaSet. It needs resource requests to work correctly.
```

## 17.6 "How do zero-downtime deployments work in Kubernetes?"

```text
Deployment performs a rolling update by creating a new ReplicaSet and scaling it up while scaling the old one down. Readiness probe is critical because traffic should go only to Pods that are truly ready. Rollback is possible because previous ReplicaSets are kept as revision history.
```

## 17.7 "Walk me through kubectl apply internally"

```text
kubectl sends the resource to the API server, the API server validates and stores it in etcd, controllers observe the new desired state and create ReplicaSets and Pods, the scheduler assigns Pods to nodes, kubelet runs the containers, and Services start routing traffic once Pods are ready.
```

---

# 18. Quick Revision Sheet

## One-Line Mapping

```text
Pod         = one running instance of the app
ReplicaSet  = maintains Pod count
Deployment  = rollout, rollback, replica management
Service     = stable identity and load balancing for Pods
Ingress     = HTTP entry point and routing
ConfigMap   = non-sensitive config
Secret      = sensitive config
Readiness   = should this Pod get traffic?
Liveness    = should this container be restarted?
Startup     = has app finished booting?
HPA         = auto-scale replica count
Namespace   = logical isolation boundary
```

## The Lifecycle in One Flow

```text
Developer writes YAML
  ↓
kubectl apply
  ↓
API server stores desired state
  ↓
Deployment creates ReplicaSet
  ↓
ReplicaSet creates Pods
  ↓
Scheduler picks nodes
  ↓
Kubelet starts containers
  ↓
Service routes traffic to ready Pods
  ↓
Ingress exposes app externally
  ↓
HPA scales replicas when load changes
  ↓
Deployment rolls forward or back during releases
```

## Gold Standard Answer

```text
In Kubernetes, I typically run stateless applications using Deployments. The Deployment manages ReplicaSets, and ReplicaSets maintain the desired number of Pods. Services give the Pods a stable network identity, and Ingress exposes the app externally with host or path-based routing. ConfigMaps and Secrets separate configuration from the image, probes protect startup and traffic safety, and HPA scales the number of replicas based on load. During releases, rolling updates and rollback are handled by the Deployment using ReplicaSet revision history.
```