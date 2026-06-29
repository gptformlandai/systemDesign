# Kubernetes kubectl Troubleshooting Playbook Gold Sheet

> Track: K8s Interview Track — Phase 5: Observability and Operations
> Goal: Systematically diagnose any Kubernetes problem — pods not starting, services unreachable, performance issues, cluster-level problems — using kubectl commands.

---

## 0. How To Read This

Beginner focus:
- kubectl get, describe, logs basics
- Reading pod status and events

Intermediate focus:
- CrashLoopBackOff, ImagePullBackOff, Pending, OOMKilled
- Service connectivity debugging
- kubectl exec for live debugging

Senior / MAANG focus:
- Systematic 5-minute debug framework
- etcd and control plane debugging
- Node pressure and eviction debugging
- Network policy debugging
- Multi-container pod debugging

---

# Topic 1: The 5-Minute Debugging Framework

## 1. Tier 1: Identify What's Wrong

```bash
# Overview of unhealthy resources
kubectl get pods -A | grep -v Running | grep -v Completed
kubectl get pods -n prod -o wide   # see which node each pod is on
kubectl get events -n prod --sort-by='.lastTimestamp' | tail -20
```

## 2. Tier 2: Narrow Down

```bash
# Describe the failing pod
kubectl describe pod <pod-name> -n prod

# Key sections in describe output:
#   Status: Pending/Running/Failed/Succeeded
#   Conditions: Ready, ContainersReady, PodScheduled
#   Events: what happened and when
#   Containers → State: Waiting/Running/Terminated
#   Containers → Last State: previous run (for crashes)
```

## 3. Tier 3: Go Deep

```bash
# Logs from current run
kubectl logs <pod-name> -n prod --tail=100

# Logs from previous crashed run
kubectl logs <pod-name> -n prod --previous

# Exec into running container
kubectl exec -it <pod-name> -n prod -- /bin/sh

# Exec into specific container
kubectl exec -it <pod-name> -n prod -c <container-name> -- /bin/bash
```

---

# Topic 2: Pod Status Decoding

## 1. Status: Pending

```bash
kubectl describe pod <pod-name> -n prod
# Look at Events section for FailedScheduling

Common reasons:
  "Insufficient cpu" or "Insufficient memory"
    → Requests too high; cluster out of resources; need to scale up
    → Fix: kubectl describe nodes | grep -A5 Allocated

  "0/5 nodes are available: 5 node(s) had taint..."
    → Pod has no toleration for node taints
    → Fix: add toleration or correct nodeSelector

  "0/5 nodes are available: 5 node(s) didn't match Pod's node affinity"
    → affinity rules too strict or labels missing on nodes
    → Fix: check nodeAffinity; kubectl get nodes --show-labels

  "0/5 nodes are available: 5 Insufficient memory"
    → Cluster at capacity; need more nodes
    → Check: kubectl top nodes

  "persistentvolumeclaim ... not found"
    → PVC doesn't exist or bound
    → Fix: kubectl get pvc -n prod
```

## 2. Status: CrashLoopBackOff

```bash
# Check logs from crashed run
kubectl logs <pod-name> -n prod --previous

# Check events
kubectl describe pod <pod-name> -n prod

Common reasons:
  Application exception at startup:
    → Fix the application code or config
  
  Wrong configuration (bad DB URL, wrong port):
    → kubectl describe pod → check env vars
    → kubectl get secret/configmap → check values
  
  Missing dependency (DB not ready):
    → Add init container to wait for DB
    → Use readiness probe that checks DB
  
  OOMKilled during startup:
    → Memory limit too low for startup phase
    → Increase memory limit or optimize startup
  
  Wrong entrypoint/command:
    → Check `command` and `args` in pod spec
    → kubectl describe pod → Containers → Command

Backoff progression: 10s → 20s → 40s → 80s → 160s → 5m (max)
```

## 3. Status: ImagePullBackOff / ErrImagePull

```bash
kubectl describe pod <pod-name> -n prod
# Events: Failed to pull image "my-registry/app:v1.2": rpc error...

Common reasons:
  Image doesn't exist (wrong tag or typo):
    → kubectl describe pod → Containers → Image
    → Verify image exists in registry
  
  No pull credentials:
    → kubectl get secret -n prod | grep registry
    → Add imagePullSecrets to pod spec or service account
  
  Registry unreachable:
    → kubectl exec -it debug-pod -- curl -v https://my-registry.com
    → Check network policy, VPC routing
  
  Rate limit (Docker Hub):
    → Pull from private registry or use imagePullPolicy: IfNotPresent
```

## 4. Status: OOMKilled

```bash
kubectl describe pod <pod-name> -n prod
# Containers → Last State → Exit Code: 137

kubectl top pod <pod-name> -n prod    # current memory usage

Common reasons:
  Memory limit set too low:
    → kubectl get pod -o jsonpath='{.spec.containers[0].resources.limits.memory}'
    → Increase memory limit
  
  Java JVM not respecting cgroup limits (pre Java 8u191):
    → Add: -XX:+UseContainerSupport -XX:MaxRAMPercentage=75
  
  Memory leak in application:
    → Check heap dumps; Prometheus memory growth trend
    → kubectl top pod --containers -n prod | sort -k4
  
  Large memory spike on specific operation:
    → Add circuit breaker or request size limit
    → Profile the spike
```

## 5. Status: Evicted

```bash
kubectl describe pod <pod-name> -n prod
# Status: Failed, Reason: Evicted
# Message: The node was low on resource: memory...

kubectl get nodes
kubectl describe node <node-name>   # look at Conditions and Allocated Resources
kubectl top nodes

Fix:
  Short term: kubectl delete pod -l app=<app> -n prod  # reschedule evicted pods
  Long term: reduce pod memory requests; add more nodes; set proper resource limits
```

## 6. Status: Terminating (Stuck)

```bash
# Pod stuck in Terminating for >10 minutes
kubectl describe pod <pod-name> -n prod

# Force delete if node is gone or finalizer is stuck
kubectl delete pod <pod-name> -n prod --grace-period=0 --force

# Check for finalizers
kubectl get pod <pod-name> -n prod -o jsonpath='{.metadata.finalizers}'
kubectl patch pod <pod-name> -n prod -p '{"metadata":{"finalizers":null}}' --type=merge
```

---

# Topic 3: Service Connectivity Debugging

## 1. Pod Can't Reach Service

```bash
# Step 1: verify service exists and has endpoints
kubectl get svc payment-service -n prod
kubectl get endpoints payment-service -n prod
# If endpoints is empty: no pods match the service selector

# Step 2: check pod is Ready (only Ready pods appear in endpoints)
kubectl get pods -l app=payment-service -n prod

# Step 3: test from inside a pod
kubectl exec -it debug-pod -n prod -- curl http://payment-service.prod.svc:80/health

# Step 4: test DNS resolution
kubectl exec -it debug-pod -n prod -- nslookup payment-service.prod
kubectl exec -it debug-pod -n prod -- cat /etc/resolv.conf

# Step 5: test direct pod IP (bypass service)
kubectl get pod payment-service-abc123 -n prod -o wide   # get pod IP
kubectl exec -it debug-pod -n prod -- curl http://<pod-ip>:8080/health
```

## 2. Debugging Service → Endpoints Chain

```bash
# Check if selector matches pod labels
kubectl get svc payment-service -n prod -o jsonpath='{.spec.selector}'
kubectl get pods -n prod -l app=payment-service  # do these match?

# Check EndpointSlices
kubectl get endpointslices -n prod -l kubernetes.io/service-name=payment-service

# Check kube-proxy is running (DaemonSet)
kubectl get daemonset kube-proxy -n kube-system
kubectl get pods -n kube-system -l k8s-app=kube-proxy

# Test from inside a running pod (install debug tools if needed)
kubectl run debug --image=nicolaka/netshoot -it --rm -n prod
```

## 3. Network Policy Blocking Traffic

```bash
# Check if NetworkPolicy exists in namespace
kubectl get networkpolicies -n prod

# Temporarily add allow-all to verify netpol is the issue
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: temp-allow-all
  namespace: prod
spec:
  podSelector: {}
  ingress: [{}]
  egress: [{}]
  policyTypes: [Ingress, Egress]
EOF

# Test connectivity with allow-all policy in place
# If it works → the original netpol is blocking
# Remove temp policy after debugging
kubectl delete networkpolicy temp-allow-all -n prod
```

---

# Topic 4: Node Debugging

```bash
# Node health overview
kubectl get nodes -o wide
kubectl describe node <node-name>   # look at Conditions and Events

# Node resource usage
kubectl top nodes

# Drain node for maintenance
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# View pods on a specific node
kubectl get pods -A -o wide --field-selector=spec.nodeName=<node-name>

# Check kubelet status (on the node via SSH)
systemctl status kubelet
journalctl -u kubelet -n 100 --no-pager

# Node is NotReady: check kubelet logs for TLS, network, OOM
journalctl -u kubelet | grep -i error | tail -30
```

---

# Topic 5: kubectl Power Commands

## 1. Output Formats

```bash
# Wide output (more columns)
kubectl get pods -n prod -o wide

# JSON (full spec)
kubectl get pod <name> -n prod -o json

# YAML
kubectl get pod <name> -n prod -o yaml

# JSONPath (extract specific field)
kubectl get pod <name> -n prod -o jsonpath='{.status.podIP}'
kubectl get pods -n prod -o jsonpath='{range .items[*]}{.metadata.name} {.status.phase}{"\n"}{end}'

# Custom columns
kubectl get pods -n prod \
  -o custom-columns='NAME:.metadata.name,STATUS:.status.phase,NODE:.spec.nodeName'

# Sort by field
kubectl get pods -n prod --sort-by='.metadata.creationTimestamp'
kubectl get pods -n prod --sort-by='.status.containerStatuses[0].restartCount'
```

## 2. Filtering

```bash
# By label selector
kubectl get pods -n prod -l app=payment-service,version=v1.2

# By field selector
kubectl get pods -A --field-selector=status.phase=Pending
kubectl get pods -A --field-selector='status.phase!=Running,status.phase!=Succeeded'
kubectl get pods -A --field-selector=spec.nodeName=node-1

# All namespaces
kubectl get pods -A
```

## 3. Watching and Following

```bash
# Watch resources
kubectl get pods -n prod -w

# Follow logs for all pods with label
kubectl logs -f -l app=payment-service -n prod --max-log-requests=10

# Watch events
kubectl get events -n prod -w
```

## 4. Debug Pod Patterns

```bash
# Run a one-shot debug container in the cluster
kubectl run debug-$(date +%s) \
  --image=nicolaka/netshoot \
  --rm -it \
  -n prod \
  -- /bin/bash

# Copy files from/to pod
kubectl cp prod/payment-service-abc123:/app/logs/app.log ./app.log
kubectl cp ./config.yaml prod/payment-service-abc123:/etc/config.yaml

# Port-forward to a pod (bypass service)
kubectl port-forward pod/payment-service-abc123 8080:8080 -n prod

# Port-forward to a service
kubectl port-forward svc/payment-service 8080:80 -n prod

# Ephemeral debug container (K8s 1.23+)
kubectl debug -it <pod-name> -n prod \
  --image=nicolaka/netshoot \
  --target=<container-name>   # share process namespace
```

---

# Topic 6: Control Plane Debugging

```bash
# Check control plane pods
kubectl get pods -n kube-system
kubectl get pods -n kube-system | grep -v Running

# API server logs
kubectl logs -n kube-system kube-apiserver-<node-name>

# Scheduler logs
kubectl logs -n kube-system kube-scheduler-<node-name>

# Controller manager logs
kubectl logs -n kube-system kube-controller-manager-<node-name>

# etcd health check
kubectl exec -it etcd-<node-name> -n kube-system -- \
  etcdctl --endpoints=https://localhost:2379 \
    --cacert=/etc/kubernetes/pki/etcd/ca.crt \
    --cert=/etc/kubernetes/pki/etcd/server.crt \
    --key=/etc/kubernetes/pki/etcd/server.key \
    endpoint health

# Check API server can reach etcd
kubectl get --raw /healthz
kubectl get --raw /readyz
```

---

# Topic 7: Quick Reference Cheat Sheet

```text
Pod not starting:
  kubectl describe pod → Events section → reason for failure

CrashLoopBackOff:
  kubectl logs --previous → see exception or startup error

ImagePullBackOff:
  kubectl describe pod → check image name + imagePullSecrets

Service not routing:
  kubectl get endpoints → is it populated?
  kubectl get pods -l <selector> → are pods Ready?

DNS not resolving:
  kubectl exec → nslookup servicename.namespace
  kubectl get pods -n kube-system -l k8s-app=kube-dns

Network blocked:
  kubectl get networkpolicies -n prod → check for deny policies

Node NotReady:
  kubectl describe node → Conditions; check kubelet status

Resource exhausted:
  kubectl top nodes → check CPU/mem
  kubectl describe node → Allocated Resources section

OOMKilled:
  kubectl describe pod → exit code 137 in Last State
  kubectl top pod → memory usage
```

---

# Topic 8: Revision Notes

- Tier 1: kubectl get pods -A | grep -v Running; kubectl get events --sort-by lastTimestamp
- CrashLoopBackOff: kubectl logs --previous; check env vars, command, memory limits
- Pending: kubectl describe pod → FailedScheduling event; check resources/affinity/taints
- Service connectivity: check endpoints, selector match, DNS, network policy
- Port-forward: bypass service to test pod directly
- Debug pod: `kubectl run debug --image=nicolaka/netshoot -it --rm`
- Ephemeral containers: `kubectl debug -it pod --image=...` for sidecar debugging
- Control plane: check kube-system pod logs; etcd endpoint health

## Official Source Notes

- Troubleshooting: <https://kubernetes.io/docs/tasks/debug/>
- kubectl reference: <https://kubernetes.io/docs/reference/kubectl/>
- Debug running pods: <https://kubernetes.io/docs/tasks/debug/debug-application/debug-running-pod/>
- Application debugging: <https://kubernetes.io/docs/tasks/debug/debug-application/>
