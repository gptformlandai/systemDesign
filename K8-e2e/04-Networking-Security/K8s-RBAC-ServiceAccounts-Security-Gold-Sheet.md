# Kubernetes RBAC and ServiceAccounts Gold Sheet

> Track: K8s Interview Track — Phase 4: Networking and Security
> Goal: Master Kubernetes access control — who can do what to which resources — from namespace-scoped roles to cluster-wide governance.

---

## 0. How To Read This

Beginner focus:
- What RBAC is: Role, ClusterRole, RoleBinding, ClusterRoleBinding
- ServiceAccount identity for pods
- Why `default` ServiceAccount is dangerous

Intermediate focus:
- Designing least-privilege RBAC roles for applications
- Binding strategies: Role vs ClusterRole per namespace
- ServiceAccount token projection (short-lived tokens)
- Audit logging RBAC decisions

Senior / MAANG focus:
- RBAC at scale: aggregated ClusterRoles
- IRSA (IAM Roles for Service Accounts) on EKS
- Workload Identity on GKE
- OIDC token exchange for cloud resource access
- Admission webhook for RBAC policy enforcement

---

# Topic 1: RBAC Fundamentals

## 1. Intuition

RBAC answers: **Who** can do **what** to **which** resources?

```text
Who:    User | Group | ServiceAccount
What:   verbs (get, list, create, update, patch, delete, watch, exec)
Which:  resources (pods, deployments, secrets, configmaps, etc.)

RBAC = Role (what) + Binding (who gets the role)
```

## 2. Four RBAC Objects

| Object | Scope | Purpose |
|---|---|---|
| `Role` | Namespace | defines permissions within a namespace |
| `ClusterRole` | Cluster-wide | defines permissions cluster-wide (or reusable in namespaces) |
| `RoleBinding` | Namespace | grants Role or ClusterRole to a subject, within a namespace |
| `ClusterRoleBinding` | Cluster-wide | grants ClusterRole to a subject cluster-wide |

```text
Rule of thumb:
  Namespace-scoped work: use Role + RoleBinding
  Cluster-wide read (node info, PVs, namespaces): use ClusterRole + ClusterRoleBinding
  Reusable permissions across namespaces: define ClusterRole, bind with RoleBinding per namespace
```

---

# Topic 2: Roles

## 1. Role (Namespace-Scoped)

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: payment-service-role
  namespace: prod
rules:
  - apiGroups: [""]               # "" = core API group
    resources: ["pods", "services", "endpoints"]
    verbs: ["get", "list", "watch"]
  - apiGroups: ["apps"]           # apps API group
    resources: ["deployments", "replicasets"]
    verbs: ["get", "list", "watch", "update", "patch"]
  - apiGroups: [""]
    resources: ["secrets"]
    resourceNames: ["payment-db-secret"]   # specific secret only
    verbs: ["get"]
```

## 2. ClusterRole

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: node-reader
rules:
  - apiGroups: [""]
    resources: ["nodes"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["namespaces"]
    verbs: ["get", "list"]
  - apiGroups: ["metrics.k8s.io"]   # non-core API group
    resources: ["nodes", "pods"]
    verbs: ["get", "list"]
```

## 3. Verbs Reference

| Verb | HTTP Method | Description |
|---|---|---|
| `get` | GET on specific resource | get single resource by name |
| `list` | GET on collection | list all resources of a type |
| `watch` | GET with watch param | long-poll for changes |
| `create` | POST | create a new resource |
| `update` | PUT | full update (replace) |
| `patch` | PATCH | partial update |
| `delete` | DELETE | delete one resource |
| `deletecollection` | DELETE on collection | bulk delete |
| `exec` | POST to /exec subresource | kubectl exec (requires pods/exec) |
| `proxy` | POST to /proxy subresource | kubectl port-forward |
| `*` | All verbs | superuser for this resource |

---

# Topic 3: Bindings

## 1. RoleBinding

Grants a Role OR ClusterRole to a subject, within a namespace:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: payment-service-binding
  namespace: prod
subjects:
  - kind: ServiceAccount
    name: payment-service-sa
    namespace: prod
  - kind: User                   # for human users (from OIDC/cert CN)
    name: aravind@company.com
    apiGroup: rbac.authorization.k8s.io
  - kind: Group                  # for groups from OIDC
    name: payments-team
    apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role                     # OR ClusterRole
  name: payment-service-role
  apiGroup: rbac.authorization.k8s.io
```

## 2. ClusterRoleBinding

Grants a ClusterRole cluster-wide:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: cluster-admin-binding
subjects:
  - kind: User
    name: platform-admin@company.com
    apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io
```

## 3. Reusing ClusterRole Per Namespace (Preferred Pattern)

```text
Instead of creating identical Roles in every namespace:
  1. Define ClusterRole with permissions
  2. Create RoleBinding in each namespace that references the ClusterRole

Result: one ClusterRole, multiple namespace-scoped bindings

kubectl get clusterrole view    # built-in read-only ClusterRole
kubectl get clusterrole edit    # built-in read-write ClusterRole
kubectl get clusterrole admin   # built-in namespace admin ClusterRole
```

---

# Topic 4: ServiceAccounts

## 1. Why ServiceAccounts Exist

When a pod needs to call the Kubernetes API, it authenticates using a ServiceAccount:

```text
Pod → API Server: "I am ServiceAccount payment-service-sa in namespace prod"
API Server checks: does this SA have permission for the requested action?
```

## 2. Default ServiceAccount Anti-Pattern

```text
Every namespace has a `default` ServiceAccount.
Pods not specifying serviceAccountName use `default` automatically.

Problem: If no RBAC restricts the `default` SA, it may have:
  - Implicit permissions (varies by cluster config)
  - In older K8s: excessive permissions

Fix: Always create dedicated ServiceAccounts per application.
```

## 3. Creating and Using ServiceAccounts

```yaml
# Create ServiceAccount
apiVersion: v1
kind: ServiceAccount
metadata:
  name: payment-service-sa
  namespace: prod
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456:role/payment-service-role  # IRSA

---
# Create Role + RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: payment-service-role
  namespace: prod
rules:
  - apiGroups: [""]
    resources: ["configmaps"]
    resourceNames: ["payment-service-config"]
    verbs: ["get"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: payment-service-rb
  namespace: prod
subjects:
  - kind: ServiceAccount
    name: payment-service-sa
    namespace: prod
roleRef:
  kind: Role
  name: payment-service-role
  apiGroup: rbac.authorization.k8s.io

---
# Use in Deployment
spec:
  template:
    spec:
      serviceAccountName: payment-service-sa
      automountServiceAccountToken: false    # disable if pod doesn't need K8s API access
```

## 4. ServiceAccount Token Auto-Mount

By default, K8s auto-mounts a ServiceAccount token into every pod at:
`/var/run/secrets/kubernetes.io/serviceaccount/token`

Disable when not needed:
```yaml
spec:
  automountServiceAccountToken: false   # in ServiceAccount or Pod spec
```

Modern tokens (K8s 1.22+) are short-lived and audience-bound (projected tokens):
```yaml
volumes:
  - name: sa-token
    projected:
      sources:
        - serviceAccountToken:
            audience: api.myapp.com    # audience claim
            expirationSeconds: 3600    # 1 hour expiry
            path: token
```

---

# Topic 5: IRSA — IAM Roles for Service Accounts (EKS)

## 1. What IRSA Solves

Without IRSA, EKS pods use the node's EC2 IAM role — all pods on the node share the same AWS permissions. This violates least privilege.

IRSA allows **per-pod AWS IAM roles** via OIDC token exchange:

```text
Pod (payment-service-sa) → Kubernetes OIDC token
  → AWS STS AssumeRoleWithWebIdentity
  → Temporary AWS credentials (STS)
  → Access specific S3 buckets, SQS queues, DynamoDB tables

Each microservice gets its own IAM role with exactly the AWS permissions it needs.
```

## 2. IRSA Setup

```bash
# 1. Create OIDC provider for EKS cluster
eksctl utils associate-iam-oidc-provider --cluster=my-cluster --approve

# 2. Create IAM role with trust policy (allow specific SA to assume it)
# Trust policy:
{
  "Principal": {
    "Federated": "arn:aws:iam::123456:oidc-provider/oidc.eks.us-east-1.amazonaws.com/..."
  },
  "Action": "sts:AssumeRoleWithWebIdentity",
  "Condition": {
    "StringEquals": {
      "oidc...sub": "system:serviceaccount:prod:payment-service-sa"
    }
  }
}

# 3. Annotate ServiceAccount
kubectl annotate serviceaccount payment-service-sa \
  eks.amazonaws.com/role-arn=arn:aws:iam::123456:role/payment-service-role \
  -n prod
```

## 3. How It Works

```text
1. Pod starts → projected SA token mounted
2. AWS SDK in pod calls STS with the token
3. STS validates: token from EKS OIDC provider, matches expected SA
4. STS returns temporary credentials (15min-12hr)
5. AWS SDK uses credentials for API calls
6. On expiry: SDK automatically refreshes (token rotated by kubelet)
```

---

# Topic 6: Aggregated ClusterRoles

```yaml
# Built-in aggregation label
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: monitoring-view
  labels:
    rbac.authorization.k8s.io/aggregate-to-view: "true"  # adds to built-in 'view' role
rules:
  - apiGroups: ["monitoring.coreos.com"]
    resources: ["prometheusrules", "servicemonitors"]
    verbs: ["get", "list", "watch"]
```

Any ClusterRole with `aggregate-to-view: "true"` is automatically merged into the `view` ClusterRole. No rebinding needed.

---

# Topic 7: RBAC Audit and Debugging

```bash
# Check what a ServiceAccount can do
kubectl auth can-i get pods --as=system:serviceaccount:prod:payment-service-sa -n prod
kubectl auth can-i '*' '*' --as=system:serviceaccount:prod:payment-service-sa  # superuser check

# List all permissions a SA has (across all bindings)
kubectl get rolebindings,clusterrolebindings -A \
  -o json | jq '.items[] | select(.subjects[]?.name=="payment-service-sa")' 

# Check who has access to secrets in prod namespace
kubectl get rolebindings -n prod -o json | \
  jq '.items[] | select(.roleRef.name | contains("secret"))'

# View API server audit log for RBAC denials
# audit log entry for denied request:
{
  "verb": "get",
  "user": {"username": "system:serviceaccount:prod:payment-service-sa"},
  "objectRef": {"resource": "secrets", "name": "prod-api-key"},
  "responseStatus": {"code": 403, "reason": "Forbidden"}
}
```

---

# Topic 8: RBAC Best Practices

| Practice | Why |
|---|---|
| One ServiceAccount per application | Least privilege; no shared identity |
| `automountServiceAccountToken: false` for pods that don't call K8s API | Remove attack surface |
| Use short-lived projected tokens | Projected tokens expire; old auto-mounted tokens don't |
| Use `resourceNames:` to restrict to specific resources | Principle of least privilege |
| Never use `cluster-admin` for applications | Too broad; security risk |
| Avoid `*` verbs or `*` resources in roles | Use explicit allowlists |
| Use IRSA/Workload Identity for cloud access | No static credentials in pods |
| Audit RBAC permissions regularly | Roles accumulate over time |

---

# Topic 9: Revision Notes

- RBAC: Role + RoleBinding (namespace) or ClusterRole + ClusterRoleBinding (cluster-wide)
- Role: permissions; Binding: who gets the permissions
- ServiceAccount: pod identity for K8s API access; one per application
- `default` SA: avoid; disable automountServiceAccountToken
- IRSA: per-pod AWS IAM role via OIDC → STS AssumeRoleWithWebIdentity
- Aggregated ClusterRoles: auto-merge roles using labels; extend built-in roles
- `kubectl auth can-i`: test RBAC permissions before deploying
- Audit: enable API server audit log for RBAC decision logging

## Official Source Notes

- RBAC: <https://kubernetes.io/docs/reference/access-authn-authz/rbac/>
- ServiceAccounts: <https://kubernetes.io/docs/concepts/security/service-accounts/>
- IRSA on EKS: <https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html>
- Built-in roles: <https://kubernetes.io/docs/reference/access-authn-authz/rbac/#default-roles-and-role-bindings>
