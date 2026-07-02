# Kubernetes API Machinery, APF, Server-Side Apply, Audit, and Storage Versions Gold Sheet

> Track: K8s Interview Track - Phase 6: Advanced Patterns Plus
> Goal: Understand Kubernetes as an API platform: request flow, fairness, field ownership, auditability, aggregation, garbage collection, and versioned storage.

---

## 0. How To Read This

Beginner focus:
- Kubernetes stores desired state through the API server.
- Controllers watch API objects and reconcile reality.
- `kubectl apply` changes objects.

Intermediate focus:
- API requests pass through authn, authz, admission, validation, and storage.
- Server-side apply tracks field ownership.
- Finalizers and owner references control deletion behavior.

Senior / MAANG focus:
- Large clusters need API Priority and Fairness, audit policy, admission hygiene, and storage version planning.
- CRD owners must handle conversion, defaulting, status, finalizers, and version migration.
- API server overload can become a platform-wide incident.

---

# Topic 1: API Request Lifecycle

```text
Client request
  -> authentication
  -> authorization
  -> API Priority and Fairness
  -> mutating admission
  -> schema/defaulting/validation
  -> validating admission
  -> etcd write
  -> watch event to controllers and clients
```

Read path:
```text
Client GET/LIST/WATCH -> authn -> authz -> APF -> API server cache/etcd -> response
```

Interview point:
```text
Kubernetes is not just a container scheduler. It is a distributed API system
with many controllers watching and writing objects concurrently.
```

---

# Topic 2: API Priority and Fairness

## 1. Problem

```text
Without fairness:
  A noisy controller or script can flood LIST/WATCH/PATCH requests.
  Critical control-plane traffic is delayed.
  Nodes, controllers, and users observe cascading failures.
```

## 2. APF Idea

```text
API Priority and Fairness:
  Classifies requests into priority levels and flow schemas.
  Allocates concurrency shares.
  Queues lower-priority traffic instead of letting it starve critical traffic.
```

Examples:
- Node heartbeat traffic should not be blocked by a dashboard doing giant list calls.
- System controllers should have priority over ad hoc user scripts.
- Mutating writes may need tighter concurrency than reads.

Commands:
```bash
kubectl get prioritylevelconfigurations.flowcontrol.apiserver.k8s.io
kubectl get flowschemas.flowcontrol.apiserver.k8s.io
kubectl get --raw /metrics | grep apiserver_flowcontrol
```

---

# Topic 3: Server-Side Apply and managedFields

## 1. Client-Side Apply

```text
kubectl apply stores last-applied-configuration annotation.
Merge is calculated client-side.
Can become brittle with multiple writers.
```

## 2. Server-Side Apply

```text
Server-side apply:
  API server calculates merge and tracks field ownership in managedFields.
```

Command:
```bash
kubectl apply --server-side --field-manager=platform-gitops -f deployment.yaml
kubectl get deploy payment -o yaml | yq '.metadata.managedFields'
```

Field conflict example:
```text
GitOps owns spec.replicas.
HPA also wants to update replicas.
Conflict or drift occurs unless ownership is designed carefully.
```

Best practice:
- Let HPA own `spec.replicas`.
- Let GitOps own template, labels, probes, resources.
- Use `ignoreDifferences` in ArgoCD for fields owned by controllers.
- Avoid many controllers fighting over the same field.

---

# Topic 4: Owner References, Garbage Collection, and Finalizers

## 1. Owner References

```text
Deployment owns ReplicaSet.
ReplicaSet owns Pods.
Delete Deployment with cascading deletion -> ReplicaSet and Pods are garbage collected.
```

Check:
```bash
kubectl get pod payment-abc -o jsonpath='{.metadata.ownerReferences}'
```

## 2. Finalizers

```text
Finalizer:
  "Do not delete this object until my controller cleans external state."
```

Use cases:
- Delete cloud load balancer.
- Snapshot a database before deletion.
- Drain a queue subscription.
- Release external DNS record.

Failure mode:
```text
Controller is gone but finalizer remains.
Object is stuck Terminating.
```

Fix carefully:
```bash
kubectl patch <resource> <name> --type=json \
  -p='[{"op":"remove","path":"/metadata/finalizers"}]'
```

Only remove a finalizer manually after confirming external cleanup is safe.

---

# Topic 5: Audit Policy

Audit logs answer:
```text
Who did what, to which object, from where, and was it allowed?
```

Useful events:
- `create` privileged pod.
- `patch` ClusterRoleBinding.
- `delete` namespace.
- `create` pods/ephemeralcontainers.
- Admission denied events.

Audit policy shape:
```yaml
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
  - level: RequestResponse
    verbs: ["create", "update", "patch", "delete"]
    resources:
      - group: "rbac.authorization.k8s.io"
        resources: ["clusterrolebindings", "rolebindings"]
  - level: Metadata
    resources:
      - group: ""
        resources: ["pods", "secrets", "configmaps"]
  - level: None
    users: ["system:kube-proxy"]
    verbs: ["watch"]
```

Trade-off:
```text
RequestResponse is powerful but expensive and can expose sensitive data.
Use it only for high-risk resources and send logs to protected storage.
```

---

# Topic 6: API Aggregation and CRDs

## 1. CRDs

```text
CRD:
  Extends Kubernetes API with new resource types stored in etcd.
```

Good for:
- Declarative custom objects.
- Controllers that reconcile desired state.
- Platform APIs such as `Database`, `Tenant`, `Environment`.

## 2. API Aggregation

```text
Aggregated API server:
  A separate API server registered behind kube-apiserver.
```

Good for:
- Custom storage backend.
- Specialized authn/authz or API behavior.
- Metrics APIs such as metrics.k8s.io.

Decision:
```text
Use CRDs by default. Use API aggregation only when CRDs cannot express the API
or storage/behavior requirements.
```

---

# Topic 7: Storage Versions and Conversion

```text
Served version:
  API version clients can use.

Storage version:
  API version persisted in etcd.

Conversion webhook:
  Converts between versions when schema changes.
```

CRD maturity path:
```text
v1alpha1:
  Experimental, breaking changes allowed.

v1beta1:
  Mostly stable, feedback still expected.

v1:
  Stable API contract, conversion and deprecation policy required.
```

Migration checklist:
- Add new version while still serving old version.
- Implement conversion if schemas differ.
- Update clients and manifests.
- Migrate stored objects.
- Stop serving old version only after users are migrated.

---

# Topic 8: Admission Webhook Good Practices

```text
1. Keep webhooks fast.
2. Set timeouts.
3. Scope rules narrowly.
4. Avoid calling unstable external services.
5. Avoid side effects during dry-run.
6. Expose metrics for latency, errors, and rejections.
7. Use failurePolicy intentionally.
8. Prefer ValidatingAdmissionPolicy for simple deterministic validations.
```

Webhook outage blast radius:
```text
If failurePolicy: Fail and webhook is down, deployments may be blocked.
If failurePolicy: Ignore, unsafe objects may pass.
```

---

# Topic 9: Interview Scenario

> A platform controller update caused API latency to spike and deployments across the company started timing out. What do you investigate?

Strong answer:
```text
I would treat this as an API server load incident. First I would check API
server latency, inflight requests, APF queues, etcd latency, and audit volume.
Then I would identify the noisy client by user agent and request pattern.
Common causes are controllers doing full LIST loops, excessive PATCH retries,
or high-cardinality watches. I would throttle or roll back the controller,
verify APF protects system traffic, and add client-side rate limiting,
watch-based caches, and focused field selectors before redeploying.
```

---

# Topic 10: Revision Notes

- API server path: authn -> authz -> APF -> admission -> validation -> etcd.
- APF protects critical traffic from noisy clients.
- Server-side apply tracks field ownership in `managedFields`.
- Owner references drive garbage collection; finalizers block deletion until cleanup.
- Audit logs are essential but can be expensive and sensitive.
- CRDs are default API extension; aggregation is for special API server behavior.
- CRD versioning requires conversion and storage migration planning.

## Official Source Notes

- API concepts: <https://kubernetes.io/docs/reference/using-api/api-concepts/>
- Server-side apply: <https://kubernetes.io/docs/reference/using-api/server-side-apply/>
- API Priority and Fairness: <https://kubernetes.io/docs/concepts/cluster-administration/flow-control/>
- Auditing: <https://kubernetes.io/docs/tasks/debug/debug-cluster/audit/>
- API aggregation: <https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/apiserver-aggregation/>

