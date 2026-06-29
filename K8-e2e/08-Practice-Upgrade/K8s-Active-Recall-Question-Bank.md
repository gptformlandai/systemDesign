# Kubernetes Active Recall Question Bank — 80 Questions

> Track: K8s Interview Track — Phase 8: Practice Upgrade
> Technique: Cover each question. Recall the answer from memory. Check. Repeat weak areas.
> Format: Question → Answer (read only after attempting recall)

---

## Section 1: Core Concepts and Architecture (Q1–Q15)

**Q1. What is the smallest deployable unit in Kubernetes?**
A: A Pod. It contains one or more containers that share the same network namespace (same IP) and can share volumes.

**Q2. What are the five phases in the Pod lifecycle?**
A: Pending → Running → Succeeded / Failed / Unknown.

**Q3. What happens to running pods if etcd loses quorum?**
A: Running pods continue running (kubelet manages them on the node). New pods cannot be scheduled, and failed pods cannot be replaced.

**Q4. What is the role of kube-scheduler?**
A: Watches for unscheduled pods (no `spec.nodeName`) and assigns them to a node based on resource availability, affinity rules, taints/tolerations, and scoring.

**Q5. What is the difference between a Pod's init container and its main container?**
A: Init containers run sequentially to completion before any app container starts. All init containers must succeed for app containers to start.

**Q6. Name three things that pods in the same pod share.**
A: Network namespace (same IP and ports), volumes, and lifecycle.

**Q7. What is the reconciliation loop pattern?**
A: Each controller reads desired state from etcd (spec), observes actual state, and takes action to make actual match desired. Runs continuously.

**Q8. What does `kubectl cordon` do? How is it different from `drain`?**
A: Cordon marks a node as unschedulable (no new pods). Drain additionally evicts existing pods (respecting PodDisruptionBudgets) then cordons.

**Q9. What is a Namespace? What does it NOT isolate?**
A: A virtual cluster for isolating workloads. Does NOT isolate: Nodes, PersistentVolumes, ClusterRoles, ClusterRoleBindings, StorageClasses.

**Q10. What do labels do vs what do annotations do?**
A: Labels are for selection (kubectl get pods -l app=foo). Annotations are for metadata only (tools, monitoring); they cannot be used in selectors.

**Q11. What is a sidecar container pattern?**
A: An additional container in the same pod that supports the main container (log shipping, service mesh proxy, secret rotation agent).

**Q12. What is a static pod?**
A: Pods managed directly by kubelet from manifest files in a static path (e.g., /etc/kubernetes/manifests). Control plane components (API server, etcd) run as static pods.

**Q13. What is a PodDisruptionBudget?**
A: A policy that limits how many pods of a workload can be voluntarily disrupted simultaneously during node drains, cluster upgrades, etc.

**Q14. How many etcd nodes do you need to tolerate 2 failures?**
A: 5 nodes. Quorum = (n/2)+1 = 3. With 5 nodes, you need 3 healthy for writes; 2 can fail.

**Q15. What is the difference between `Guaranteed`, `Burstable`, and `BestEffort` QoS?**
A: Guaranteed: limits == requests for all containers. Burstable: at least one request set but not equal to limits. BestEffort: no requests or limits at all.

---

## Section 2: Workloads (Q16–Q25)

**Q16. How does a Deployment use ReplicaSets?**
A: Deployment creates and manages ReplicaSets. On update, a new ReplicaSet is created and scaled up while the old one scales down. Old ReplicaSet is kept at 0 for rollback.

**Q17. What is `maxUnavailable` and `maxSurge` in a RollingUpdate?**
A: maxUnavailable: how many pods can be down during update. maxSurge: how many extra pods above desired count can exist during update.

**Q18. When would you use a StatefulSet instead of a Deployment?**
A: When pods need stable network identity, ordered start/stop, or per-pod persistent storage (databases, Kafka, ZooKeeper).

**Q19. What is the DNS pattern for a StatefulSet pod?**
A: `{pod-name}.{headless-service}.{namespace}.svc.cluster.local` e.g., `postgres-0.postgres-headless.prod.svc.cluster.local`

**Q20. What restartPolicy should a Job use?**
A: `OnFailure` or `Never`. Jobs cannot use `Always` (which is the default for Deployments).

**Q21. What does ConcurrencyPolicy `Forbid` do on a CronJob?**
A: Skips creating a new Job run if the previous one is still running.

**Q22. What happens to PVCs when a StatefulSet is deleted?**
A: They are NOT automatically deleted. PVCs must be manually deleted. This is intentional to prevent data loss.

**Q23. What is the command to rollback a Deployment to the previous version?**
A: `kubectl rollout undo deployment/my-deployment`

**Q24. What is a DaemonSet used for? Name two real examples.**
A: Ensures exactly one pod per node (or selected nodes). Examples: Fluent Bit log agent, Prometheus node-exporter, Calico/Cilium CNI agent.

**Q25. How do you pause a Deployment rollout?**
A: `kubectl rollout pause deployment/my-deployment`. Resume with `kubectl rollout resume deployment/my-deployment`.

---

## Section 3: Networking (Q26–Q36)

**Q26. What are the four Service types and when do you use each?**
A: ClusterIP (internal only), NodePort (external via node IP), LoadBalancer (cloud LB), ExternalName (CNAME to external DNS). Use LoadBalancer only when you need direct external access without Ingress.

**Q27. What is a headless service?**
A: A service with `clusterIP: None`. DNS returns individual pod IPs instead of a virtual IP. Required for StatefulSets.

**Q28. What does kube-proxy do?**
A: Maintains iptables (or ipvs) rules on every node that route traffic from a Service ClusterIP to actual pod IPs.

**Q29. What is the difference between iptables mode and ipvs mode in kube-proxy?**
A: iptables: O(n) rule lookup (degrades with many services). ipvs: O(1) hash-table lookup (better at scale, 1000+ services). ipvs also supports multiple load balancing algorithms.

**Q30. What is an Ingress controller? Give two examples.**
A: Software that implements the Ingress resource (L7 HTTP/HTTPS routing). Examples: NGINX Ingress Controller, AWS ALB Ingress Controller (AWS Load Balancer Controller), Traefik.

**Q31. What is the DNS pattern for a Service?**
A: `{service-name}.{namespace}.svc.cluster.local`

**Q32. A pod in namespace `orders` tries to reach `payment-service` (no namespace). Does it resolve?**
A: No (in the wrong namespace). Short name `payment-service` resolves to `payment-service.orders.svc.cluster.local` which doesn't exist. Use `payment-service.prod` to reach the payments namespace.

**Q33. What is the Gateway API and why does it exist?**
A: A more expressive, role-separated replacement for Ingress. Supports traffic splitting, header routing, multiple controllers. Platform team owns Gateway; dev teams own HTTPRoute.

**Q34. What does `EndpointSlice` do?**
A: Modern replacement for Endpoints. Breaks large endpoint lists into smaller slices (default 100 pods each) for more efficient updates. Enables zone-aware routing via topology hints.

**Q35. A pod calls a Service but sees no response. What are the three things to check?**
A: 1) `kubectl get endpoints` — are there endpoints (does selector match ready pods)? 2) Is kube-proxy running on the node? 3) Is a NetworkPolicy blocking the traffic?

**Q36. What is the CNI plugin requirement for NetworkPolicy enforcement?**
A: The CNI plugin must support it. Flannel does NOT. Calico, Cilium, AWS VPC CNI (with addon), Weave, Antrea support it.

---

## Section 4: Configuration, Storage, Resources (Q37–Q48)

**Q37. What is the difference between a ConfigMap and a Secret?**
A: ConfigMap: non-sensitive config. Secret: sensitive data; base64-encoded (not encrypted by default); RBAC-controlled; mounted as tmpfs.

**Q38. ConfigMap changes are updated in volume mounts without pod restart. True or false?**
A: True (after kubelet sync period ~1 minute). Env var injections are NOT updated without pod restart.

**Q39. What does `etcd encryption at rest` protect against?**
A: Prevents secrets from being readable in etcd backups or by someone with direct etcd access.

**Q40. What are the three PVC access modes?**
A: ReadWriteOnce (one node), ReadOnlyMany (many nodes read-only), ReadWriteMany (many nodes read-write).

**Q41. What is `WaitForFirstConsumer` in a StorageClass?**
A: Delays PV provisioning until a pod that uses the PVC is scheduled. Ensures the disk is created in the same AZ as the pod.

**Q42. What is the difference between a PV and a PVC?**
A: PV (PersistentVolume) is the actual storage resource (cluster-scoped). PVC (PersistentVolumeClaim) is the pod's request for storage (namespace-scoped).

**Q43. What is the reclaim policy `Retain` vs `Delete`?**
A: Retain: PV is kept when PVC is deleted (data safe, manual cleanup required). Delete: PV and backing storage are deleted automatically when PVC is deleted.

**Q44. What causes an `OOMKilled` error in K8s?**
A: Container exceeded its memory limit. Linux OOM killer kills the process. Exit code 137. Fix: increase memory limit or fix memory leak.

**Q45. Scheduler uses `requests` or `limits` for pod placement?**
A: Requests. The scheduler compares requests to available node capacity. Limits are enforced at runtime but not used for scheduling decisions.

**Q46. What QoS class is evicted first under node memory pressure?**
A: BestEffort (no requests/limits). Then Burstable. Guaranteed pods are last.

**Q47. What does a `LimitRange` do?**
A: Sets default and max resource values per container in a namespace. Containers without resources get the defaults; containers exceeding max are rejected.

**Q48. What does a `ResourceQuota` do?**
A: Caps the total resource consumption (CPU, memory, pods, PVCs, etc.) for a namespace. New resources are rejected if they would exceed the quota.

---

## Section 5: Security (Q49–Q58)

**Q49. What are the three Pod Security Standard levels?**
A: Privileged (no restrictions), Baseline (blocks known privilege escalation), Restricted (hardened, requires non-root, drop all capabilities, seccomp).

**Q50. How do you enforce Pod Security Standards on a namespace?**
A: Label the namespace: `pod-security.kubernetes.io/enforce: restricted`

**Q51. What does `allowPrivilegeEscalation: false` prevent?**
A: Prevents setuid/setgid exploitation — the container process cannot gain more privileges than its parent process.

**Q52. What is the difference between a `Role` and a `ClusterRole`?**
A: Role is namespace-scoped. ClusterRole is cluster-scoped (or reusable across namespaces via RoleBinding).

**Q53. What is the difference between `RoleBinding` and `ClusterRoleBinding`?**
A: RoleBinding grants permissions within a specific namespace. ClusterRoleBinding grants permissions cluster-wide.

**Q54. Why is the `default` ServiceAccount dangerous?**
A: All pods without explicit serviceAccountName use it. If any RBAC grants permissions to `default`, all pods get those permissions. Always use dedicated ServiceAccounts.

**Q55. What is IRSA (IAM Roles for Service Accounts)?**
A: Per-pod AWS IAM roles via OIDC token exchange. Pod gets a K8s token; AWS STS exchanges it for temporary IAM credentials. No static credentials.

**Q56. What does OPA/Gatekeeper provide that PSA doesn't?**
A: Custom policies (required labels, image registry allowlist, max replicas, no latest tag). PSA only covers security profiles.

**Q57. What is the admission webhook execution order?**
A: Mutation webhooks run first (can modify object). Then validation webhooks (can reject). Then object is stored in etcd.

**Q58. What does `failurePolicy: Fail` mean on a webhook?**
A: If the webhook server is unavailable, the admission request is rejected. Safer but may cause issues if webhook is down during deployments.

---

## Section 6: Scheduling and Autoscaling (Q59–Q67)

**Q59. What is the difference between nodeAffinity and nodeSelector?**
A: nodeSelector: simple key=value equality. nodeAffinity: supports operators (In, NotIn, Exists), required vs preferred rules.

**Q60. What is a Taint? What is a Toleration?**
A: Taint: repels pods from a node. Toleration: allows a pod to run on a tainted node. Taints go on nodes; tolerations go on pods.

**Q61. What happens with `taint effect: NoExecute`?**
A: Pods without matching toleration are evicted from the node (not just prevented from scheduling). Can add `tolerationSeconds` for graceful eviction window.

**Q62. What is `TopologySpreadConstraints`?**
A: Spreads pods across topology domains (zones, nodes) with `maxSkew` control. More flexible than antiAffinity for HA distribution.

**Q63. What does HPA use to calculate desired replicas?**
A: `desiredReplicas = ceil[currentReplicas * (currentMetricValue / targetMetricValue)]`

**Q64. Why can't HPA and VPA target CPU at the same time?**
A: VPA increases CPU requests → HPA sees lower utilization (bigger denominator) → HPA scales down → loop. Use different metrics for each.

**Q65. What is KEDA and when would you use it?**
A: Kubernetes Event-Driven Autoscaling. Scales based on external events (SQS depth, Kafka lag) rather than CPU/memory. Supports scale-to-zero for cost savings.

**Q66. What is Karpenter's advantage over Cluster Autoscaler?**
A: Karpenter calls EC2 Fleet API directly (not ASG); provisions in 30-60 seconds (vs 3-5 min); picks optimal instance type per pod requirements; supports consolidation.

**Q67. What is a `PriorityClass`? How does it work?**
A: Assigns numerical priority to pods. Higher-priority pods preempt lower-priority pods when the cluster is full. Built-in: system-cluster-critical (2000001000).

---

## Section 7: Observability and Operations (Q68–Q75)

**Q68. What is the difference between a liveness probe and a readiness probe?**
A: Liveness: kill and restart container if fails (for deadlocks). Readiness: remove pod from Service endpoints if fails (not killed, just no traffic).

**Q69. What is a startup probe used for?**
A: Grace period for slow-starting containers. Disables liveness/readiness until startup succeeds. Max startup time = failureThreshold * periodSeconds.

**Q70. Why add `preStop: exec sleep 15` to a pod?**
A: Creates a window between pod removal from EndpointSlice and SIGTERM. Allows iptables/kube-proxy to propagate routing changes, preventing traffic to terminating pods.

**Q71. What command shows logs from a crashed container's previous run?**
A: `kubectl logs <pod-name> --previous`

**Q72. What does `kubectl describe pod` show that `kubectl get pod` doesn't?**
A: Events (scheduling history, image pulls, probe failures), detailed container state, volume mounts, resource requests/limits, node assignment.

**Q73. How does Prometheus know which pods to scrape?**
A: ServiceMonitor CRD selects Services by label; Prometheus Operator configures Prometheus to scrape those Services' endpoints.

**Q74. What command do you use to test network connectivity from inside a pod?**
A: `kubectl exec -it <pod> -- curl http://target-service.namespace:port`

**Q75. A Service has 0 endpoints. What is the most likely cause?**
A: Pod selector in Service doesn't match pod labels, OR matching pods are not in Ready state (failing readiness probe).

---

## Section 8: Advanced and Production (Q76–Q80)

**Q76. What is an Operator?**
A: A custom controller with a CRD that encodes domain-specific operational knowledge (e.g., database failover, backup scheduling, cluster scaling) as Kubernetes automation.

**Q77. What is a Finalizer in a K8s resource?**
A: A mechanism that prevents a resource from being deleted until the controller has completed cleanup actions (draining queues, deleting cloud resources). Removed by controller after cleanup.

**Q78. What is the GitOps pull model vs the push model?**
A: Push: CI pipeline calls kubectl apply (needs cluster credentials in CI). Pull: ArgoCD watches Git and applies changes from inside the cluster (no CI credentials needed).

**Q79. What is the cluster upgrade order?**
A: Control plane first (etcd, API server, scheduler, controller-manager), then add-ons (CoreDNS, kube-proxy, CNI), then worker nodes.

**Q80. What is the supply chain security role of Cosign in a K8s pipeline?**
A: Signs container images after build. Admission policies (Kyverno) verify the signature at deploy time. Prevents unauthorized or tampered images from running in the cluster.
