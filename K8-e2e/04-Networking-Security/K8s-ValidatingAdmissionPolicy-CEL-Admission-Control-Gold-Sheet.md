# Kubernetes ValidatingAdmissionPolicy, CEL, and Admission Control Gold Sheet

> Track: K8s Interview Track - Phase 4: Networking and Security Plus
> Goal: Design built-in admission guardrails using ValidatingAdmissionPolicy and know when to use PSA, CEL, Gatekeeper, Kyverno, or webhooks.

---

## 0. How To Read This

Beginner focus:
- Admission control checks requests before objects are persisted.
- Policies can reject unsafe YAML.
- This prevents bad workloads from entering the cluster.

Intermediate focus:
- ValidatingAdmissionPolicy uses CEL expressions in the API server.
- Bindings attach policies to resources and namespaces.
- Webhooks are still needed for complex external checks.

Senior / MAANG focus:
- Built-in CEL policies reduce webhook operational risk.
- Policy design must handle rollout mode, exemptions, auditability, and failure behavior.
- Admission control is one layer, not a replacement for RBAC, PSA, image scanning, or runtime security.

---

# Topic 1: Admission Chain Mental Model

```text
Request:
  kubectl apply -f pod.yaml

API server pipeline:
  authentication -> authorization -> mutating admission -> object schema validation
  -> validating admission -> etcd persistence
```

Admission is the last gate before Kubernetes accepts desired state.

Examples of rules:
- Pods must define resource requests and limits.
- Images must come from approved registries.
- Ingress hostnames must match allowed domains.
- LoadBalancer Services are forbidden in developer namespaces.
- `hostNetwork`, privileged containers, and hostPath are denied unless approved.

---

# Topic 2: ValidatingAdmissionPolicy

## 1. Definition

```text
ValidatingAdmissionPolicy:
  Built-in Kubernetes validating admission using CEL expressions.

CEL:
  Common Expression Language, used to write deterministic validation rules.
```

Why it matters:
```text
Webhook policy engines add network hops and failure modes.
CEL policies run in-process in the API server for many common validations.
```

## 2. Policy + Binding Example

Require resource requests and limits for every container in selected namespaces:

```yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingAdmissionPolicy
metadata:
  name: require-container-resources
spec:
  failurePolicy: Fail
  matchConstraints:
    resourceRules:
      - apiGroups: [""]
        apiVersions: ["v1"]
        operations: ["CREATE", "UPDATE"]
        resources: ["pods"]
  validations:
    - expression: >
        object.spec.containers.all(c,
          has(c.resources) &&
          has(c.resources.requests) &&
          has(c.resources.requests.cpu) &&
          has(c.resources.requests.memory) &&
          has(c.resources.limits) &&
          has(c.resources.limits.cpu) &&
          has(c.resources.limits.memory))
      message: "Every container must set CPU and memory requests and limits."
---
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingAdmissionPolicyBinding
metadata:
  name: require-container-resources-prod
spec:
  policyName: require-container-resources
  validationActions: ["Deny"]
  matchResources:
    namespaceSelector:
      matchLabels:
        policy-tier: prod
```

Rollout approach:
```text
1. Start with validationActions: ["Audit", "Warn"].
2. Watch audit logs and developer warnings.
3. Fix manifests and exemptions.
4. Move production namespaces to ["Deny"].
```

---

# Topic 3: CEL Examples

## 1. Block Latest Tag

```text
object.spec.containers.all(c, !c.image.endsWith(":latest"))
```

## 2. Require Team Label

```text
has(object.metadata.labels) && has(object.metadata.labels.team)
```

## 3. Restrict Service Type

```text
object.spec.type != "LoadBalancer"
```

## 4. Block Host Network

```text
!has(object.spec.hostNetwork) || object.spec.hostNetwork == false
```

---

# Topic 4: Choosing The Policy Tool

| Need | Best Fit |
|---|---|
| Built-in pod security baseline/restricted | Pod Security Admission |
| Simple deterministic validation | ValidatingAdmissionPolicy + CEL |
| Mutate objects | Kyverno or mutating webhook |
| Inventory-aware constraints across many objects | Gatekeeper/OPA |
| Verify image signatures | Kyverno, Ratify, or Sigstore-aware admission |
| Call external system during admission | Webhook, carefully designed |
| Developer-friendly generate policies | Kyverno |

---

# Topic 5: Failure Policy Design

```text
failurePolicy: Fail
  Safer for critical security rules.
  Risk: API writes fail if policy evaluation infrastructure has issues.

failurePolicy: Ignore
  Higher availability.
  Risk: unsafe objects may be admitted.
```

For ValidatingAdmissionPolicy, CEL runs in API server, so it avoids many webhook availability issues. Still, rules can be too broad or buggy.

Safe rollout checklist:
- Start in warn/audit mode.
- Scope by namespace selector.
- Add break-glass namespace or label with strict audit.
- Test policies in CI using server-side dry run.
- Keep policy rules in GitOps.
- Alert on denied production deploys.

---

# Topic 6: Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Replacing RBAC with admission policy | Admission cannot grant identity permissions | Use RBAC first, admission second |
| Enforcing deny globally on day one | Breaks platform workloads and controllers | Roll out by namespace tier |
| Writing huge CEL expressions | Hard to debug and maintain | Split into focused policies |
| Using webhooks for simple field checks | Adds latency and outage risk | Use built-in CEL policy |
| Forgetting UPDATE operations | Users can create safe object, then patch unsafe fields | Match CREATE and UPDATE |

---

# Topic 7: Interview Scenario

> Platform wants to prevent production pods from using `latest` images and missing resource requests. Would you use Gatekeeper, Kyverno, or ValidatingAdmissionPolicy?

Strong answer:
```text
For simple deterministic checks on pod fields, I would start with
ValidatingAdmissionPolicy because it is built into Kubernetes and avoids webhook
network failure modes. I would create separate policies for no latest tag and
required resources, bind them only to production namespaces, and roll out with
Warn/Audit before Deny. If we also need image signature verification or mutation,
I would add Kyverno or another admission engine. RBAC, PSA, CI scanning, and
runtime monitoring still remain part of the security stack.
```

---

# Topic 8: Revision Notes

- Admission happens after authentication and authorization, before persistence.
- ValidatingAdmissionPolicy is built-in validating admission using CEL.
- Use CEL for simple, deterministic field checks.
- Use PSA for pod security standards, Kyverno/Gatekeeper/webhooks for richer needs.
- Roll out policy gradually: Audit/Warn -> Deny.
- Scope policies by namespace labels and keep them in GitOps.

## Official Source Notes

- ValidatingAdmissionPolicy: <https://kubernetes.io/docs/reference/access-authn-authz/validating-admission-policy/>
- CEL in Kubernetes: <https://kubernetes.io/docs/reference/using-api/cel/>
- Admission controllers: <https://kubernetes.io/docs/reference/access-authn-authz/admission-controllers/>

