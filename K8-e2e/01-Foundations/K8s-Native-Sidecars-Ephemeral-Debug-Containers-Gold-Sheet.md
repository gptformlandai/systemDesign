# Kubernetes Native Sidecars, Ephemeral Containers, and Debugging Gold Sheet

> Track: K8s Interview Track - Phase 1: Foundations Plus
> Goal: Understand modern multi-container pod patterns, native sidecar lifecycle, and safe production debugging without rebuilding images.

---

## 0. How To Read This

Beginner focus:
- Init containers run before app containers.
- Sidecars support the main app container.
- Ephemeral containers are temporary debug containers.

Intermediate focus:
- Native sidecars are restartable init containers.
- Debug containers are added through the `ephemeralcontainers` subresource.
- Multi-container pods share network namespace and can share volumes.

Senior / MAANG focus:
- Use native sidecars for ordered startup and shutdown-sensitive helpers.
- Use ephemeral containers for production debugging when app images are distroless.
- Avoid sidecar patterns that break Jobs, readiness, termination, or resource accounting.

---

# Topic 1: Native Sidecar Containers

## 1. Intuition

```text
Classic init container:
  "Do setup work, exit, then app starts."

Native sidecar:
  "Start before the app, keep running beside it, and terminate after the app."
```

Native sidecars are useful when the main container depends on a helper being available first, such as a log shipper, proxy, certificate refresher, or file sync process.

## 2. Definition

```text
Definition:
  A native sidecar is an init container with container-level restartPolicy: Always.

Category:
  Pod lifecycle and multi-container workload pattern.

Core idea:
  Kubernetes starts it in init order, keeps it running for the pod lifetime,
  and handles it with sidecar lifecycle semantics.
```

## 3. How It Works

```text
1. Kubelet starts regular init containers in order.
2. When it reaches an init container with restartPolicy: Always, kubelet starts it.
3. After that sidecar has started, kubelet continues with the next init container or app container.
4. The sidecar keeps running while app containers run.
5. During pod termination, app containers stop first, then sidecars stop in reverse order.
```

## 4. YAML Example

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: native-sidecar-demo
spec:
  initContainers:
    - name: logshipper
      image: busybox:1.36
      restartPolicy: Always
      command: ["sh", "-c", "tail -F /var/log/app.log"]
      volumeMounts:
        - name: app-logs
          mountPath: /var/log
  containers:
    - name: app
      image: busybox:1.36
      command: ["sh", "-c", "while true; do date >> /var/log/app.log; sleep 5; done"]
      volumeMounts:
        - name: app-logs
          mountPath: /var/log
  volumes:
    - name: app-logs
      emptyDir: {}
```

## 5. When To Use It

Strong fit:
- Helper must start before the app.
- Helper should outlive the app during graceful shutdown.
- A Job needs a sidecar, but the sidecar should not block Job completion.
- Log shipping, secret sync, local proxy, telemetry collector, or file watcher.

Weak fit:
- You only need a one-time setup task. Use a normal init container.
- You need an independent process that should scale separately. Use another Deployment.
- You need a request proxy managed by a mesh. Let the mesh injector handle it.

## 6. Job Trap

```text
Old sidecar pattern:
  Job has app container + sidecar container.
  App exits, sidecar keeps running.
  Job never completes.

Native sidecar pattern:
  Sidecar is a restartable init container.
  Job completion is based on app container completion.
```

## 7. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Putting all helpers in one pod by default | Couples scaling, failure, and release cycles | Use separate Deployments unless namespace sharing is required |
| Using sidecars for one-time migrations | Sidecar keeps running unnecessarily | Use init container or Job |
| Forgetting resource requests | Sidecars consume CPU/memory and affect scheduling | Set requests/limits for every container |
| Assuming sidecars are invisible | They change pod startup, readiness, and termination | Model them in probes, PDB, and SLOs |

---

# Topic 2: Ephemeral Containers

## 1. Intuition

```text
Ephemeral container = a temporary toolbox dropped into an existing pod.

It is for debugging only, not for serving traffic.
```

Production images are often distroless and do not contain shells, curl, tcpdump, or package managers. Ephemeral containers let you debug without rebuilding the application image.

## 2. Definition

```text
Definition:
  A container added to an existing pod through the ephemeralcontainers subresource.

Category:
  Debugging and operational tooling.

Core idea:
  Temporarily join a pod's namespaces for inspection.
```

## 3. Debug Commands

```bash
# Add a debug container to an existing pod
kubectl debug -n prod -it pod/payment-abc123 \
  --image=nicolaka/netshoot \
  --target=payment-service

# Copy a pod template, change command, and run a debug pod
kubectl debug -n prod payment-abc123 \
  --copy-to=payment-debug \
  --container=payment-service \
  --image=nicolaka/netshoot \
  --share-processes

# Debug a node through a privileged helper pod
kubectl debug node/ip-10-0-1-25.ec2.internal -it --image=ubuntu
```

## 4. What You Can Inspect

```text
Network:
  DNS lookup, TCP connection, route table, service connectivity.

Process:
  Process tree when process namespace is shared.

Filesystem:
  Mounted volumes and application files visible to the target namespace.

Environment:
  Runtime DNS, service account token behavior, mounted config, certificates.
```

## 5. Limitations

```text
Ephemeral containers:
  - Are not restarted like normal containers.
  - Are not part of the original pod spec.
  - Should not expose service ports.
  - Should not become a hidden production dependency.
  - Need RBAC permission for pods/ephemeralcontainers.
```

## 6. Security Controls

```text
RBAC:
  Grant pods/ephemeralcontainers only to platform/SRE groups.

Audit:
  Log who injected a debug container, into which pod, and which image was used.

Image policy:
  Use approved debug images only.

Pod Security:
  Avoid privileged debug unless the incident requires node-level access.
```

---

# Topic 3: Multi-Container Pod Decision Table

| Need | Best Fit |
|---|---|
| One-time setup before app starts | Init container |
| Helper must run for pod lifetime and start first | Native sidecar |
| Temporary production troubleshooting | Ephemeral container |
| Independent service with separate scaling | Separate Deployment |
| Per-pod network proxy from service mesh | Injected mesh sidecar |

---

# Topic 4: Interview Scenario

> A payment Job writes reconciliation output to a file and a log shipper container forwards it. The Job never completes. What is wrong?

Strong answer:
```text
I would check whether the log shipper is modeled as a normal app container.
For Jobs, a normal sidecar can keep the pod Running forever even after the
main task exits. I would convert the log shipper to a native sidecar by placing
it under initContainers with restartPolicy: Always, or push logs directly to
stdout if possible. I would also set resource requests, verify termination,
and confirm that the Job completion condition is based on the main task.
```

---

# Topic 5: Revision Notes

- Native sidecar = init container with `restartPolicy: Always`.
- Init container exits before app; native sidecar keeps running.
- Native sidecars are useful for ordered startup and Jobs with helper containers.
- Ephemeral containers are for debugging existing pods.
- Guard ephemeral containers with RBAC, audit, and approved images.
- Sidecars are not free: include them in resources, readiness, shutdown, and cost.

## Official Source Notes

- Sidecar containers: <https://kubernetes.io/docs/concepts/workloads/pods/sidecar-containers/>
- Ephemeral containers: <https://kubernetes.io/docs/concepts/workloads/pods/ephemeral-containers/>

