# 34. Kubernetes Debugging: Pods, CrashLoopBackOff, OOMKilled, DNS, Probes

## Goal

Debug Kubernetes workloads using events, logs, pod state, probes, resource limits, DNS checks, ephemeral debug containers, and safe remediation.

---

## First Commands

```bash
kubectl get pods -n production
kubectl describe pod <pod> -n production
kubectl logs <pod> -n production --previous
kubectl get events -n production --sort-by=.lastTimestamp
kubectl top pod <pod> -n production
kubectl get deploy <deploy> -n production -o yaml
```

`describe` tells you what Kubernetes did. Logs tell you what the app did.

---

## Pod Status Map

| Status | Meaning | First Check |
|---|---|---|
| Pending | not scheduled | node capacity, taints, PVC, image pull |
| ImagePullBackOff | image cannot pull | registry, tag, secret |
| CrashLoopBackOff | container starts then exits | previous logs, exit code |
| Running but not Ready | readiness probe failing | probe path, dependency health |
| OOMKilled | memory limit exceeded | memory usage, heap dump, limits |
| Evicted | node pressure | node memory/disk pressure |
| Completed | job finished | expected for batch jobs |

---

## CrashLoopBackOff Workflow

```bash
kubectl describe pod <pod> -n production
kubectl logs <pod> -n production --previous
kubectl get pod <pod> -n production -o jsonpath='{.status.containerStatuses[*].lastState}'
```

Look for:

```text
exitCode
reason
startedAt / finishedAt
last log line
missing env var
bad config
startup dependency unavailable
migration failure
permission error
```

Do not only restart the pod. It will crash again if the startup cause remains.

---

## OOMKilled Workflow

```bash
kubectl describe pod <pod> -n production
kubectl top pod <pod> -n production
kubectl logs <pod> -n production --previous
```

Check:

- container memory limit
- language heap limit vs cgroup limit
- recent traffic increase
- deployment version
- cache growth
- request payload size
- thread count
- heap dump availability

Java container rule:

```text
Xmx must leave room for metaspace, threads, direct buffers, JIT, native memory.
```

Bad:

```text
container limit = 1Gi
Xmx = 1Gi
```

Better:

```text
container limit = 1Gi
Xmx = 700Mi-800Mi
```

---

## Probe Debugging

| Probe | Purpose | Common Failure |
|---|---|---|
| Startup | app has started | timeout too low for cold start |
| Readiness | app can receive traffic | dependency check too strict |
| Liveness | app should be restarted | liveness checks dependency and causes restart storm |

Rules:

- Liveness should answer: "is this process unrecoverably stuck?"
- Readiness should answer: "should this pod receive traffic?"
- Startup should protect slow boot from premature liveness restarts.

---

## Service DNS Debugging

Run a temporary debug pod:

```bash
kubectl run net-debug -n production --rm -it --image=nicolaka/netshoot -- bash
```

Inside:

```bash
nslookup orders-service.production.svc.cluster.local
dig orders-service.production.svc.cluster.local
curl -v http://orders-service.production.svc.cluster.local:8080/health
nc -vz orders-service 8080
```

Check:

```bash
kubectl get svc,endpoints,endpointslice -n production
kubectl get networkpolicy -n production
```

If service has no endpoints, pods are not Ready or selectors do not match.

---

## Ephemeral Debug Containers

Use when the app container has no shell/tools.

```bash
kubectl debug -it pod/<pod> -n production --image=nicolaka/netshoot --target=<container> -- bash
```

Use for:

- DNS checks
- network connectivity
- file/process inspection
- certificate checks
- environment inspection

Do not mutate app state unless you are deliberately mitigating.

---

## Config And Secret Drift

Common symptoms:

```text
works in staging, fails in prod
only new pods fail
restart fixes nothing
app says missing key
cert/auth failures after rotation
```

Check:

```bash
kubectl describe deploy <deploy> -n production
kubectl get configmap <name> -n production -o yaml
kubectl get secret <name> -n production -o yaml
kubectl rollout history deploy/<deploy> -n production
```

Remember: environment variables from ConfigMaps/Secrets usually require pod restart to update.

---

## Immediate Mitigation

| Situation | Mitigation |
|---|---|
| bad deployment | `kubectl rollout undo deploy/<name>` |
| resource limit too low | raise limit temporarily |
| readiness too strict | relax readiness while preserving safety |
| dependency outage | route to fallback or disable feature |
| bad config | roll back ConfigMap/Secret and restart pods |
| one bad node | cordon/drain node after evidence |

---

## Practical Question

> A service is in CrashLoopBackOff after a deploy. What do you check?

---

## Strong Answer

I would run `kubectl describe pod` to inspect container state, restart count, exit code, and events. Then I would fetch `kubectl logs --previous` because the current container may already be a new failed attempt. I would compare the deployment version, environment variables, ConfigMaps, Secrets, command/args, and recent rollout history.

If the reason is `OOMKilled`, I would inspect memory limits, runtime heap settings, and previous logs. If probes are failing, I would distinguish startup, readiness, and liveness behavior. I would mitigate with rollback if the new version is correlated, then preserve evidence for root cause.

---

## Interview Sound Bite

Kubernetes debugging starts with pod state, events, previous logs, and resource/probe configuration. `CrashLoopBackOff` is an app-start failure loop; `OOMKilled` is cgroup memory enforcement; `Running but not Ready` often means readiness or endpoints. Debug the pod, the controller, the service, and the cluster event stream together.
