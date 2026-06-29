# Kubernetes Pod Security Standards and OPA/Gatekeeper Gold Sheet

> Track: K8s Interview Track — Phase 4: Networking and Security
> Goal: Prevent privilege escalation, enforce security baselines, and implement policy-as-code across every workload in the cluster.

---

## 0. How To Read This

Beginner focus:
- What Pod Security Standards are (Privileged, Baseline, Restricted)
- How Pod Security Admission works
- Why running as root is dangerous

Intermediate focus:
- securityContext at pod and container level
- OPA/Gatekeeper as policy engine
- Admission webhook architecture
- Kyverno as alternative to Gatekeeper

Senior / MAANG focus:
- Constraint Templates and custom policies
- Mutation vs validation webhooks
- Policy audit mode for brownfield clusters
- Supply chain security: image signing, admission verification
- Pod Security at scale: policy libraries, exception management

---

# Topic 1: Pod Security Standards (PSS)

## 1. Three Levels

Kubernetes defines three built-in security levels:

### Privileged
```text
No restrictions whatsoever.
Use: cluster components only (kube-system, CNI, storage drivers).
Never for application workloads.
```

### Baseline
```text
Prevents known privilege escalation attacks.
What it blocks:
  - hostPID, hostIPC, hostNetwork: true
  - hostPath volumes
  - Privileged containers
  - Dangerous capabilities (NET_RAW, SYS_ADMIN, etc.)
  - Host port binding
Use: general application workloads (minimum for prod).
```

### Restricted
```text
Hardened; follows current Pod security best practices.
What it additionally requires:
  - runAsNonRoot: true
  - seccompProfile set (RuntimeDefault or Localhost)
  - Capabilities: drop ALL, only allow NET_BIND_SERVICE
  - No privilege escalation (allowPrivilegeEscalation: false)
  - Volume restrictions (no hostPath, only specific volume types)
Use: security-sensitive workloads; recommended default.
```

## 2. Pod Security Admission (PSA)

PSA enforces PSS at the namespace level via built-in admission controller:

```yaml
# Label namespaces to enforce a security level
apiVersion: v1
kind: Namespace
metadata:
  name: prod
  labels:
    # enforce: reject violating pods
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/enforce-version: v1.28

    # audit: log violations (don't reject)
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/audit-version: v1.28

    # warn: show warnings to kubectl users
    pod-security.kubernetes.io/warn: restricted
    pod-security.kubernetes.io/warn-version: v1.28
```

Modes:
| Mode | Effect |
|---|---|
| `enforce` | Rejects pods that violate the policy |
| `audit` | Allows pod, logs policy violation to audit log |
| `warn` | Allows pod, shows warning to user via API response |

Production migration pattern:
```text
Step 1: Apply audit+warn mode only (no enforce)
Step 2: Review audit logs for violations
Step 3: Fix violating pods
Step 4: Switch to enforce mode
```

---

# Topic 2: securityContext

## 1. Pod-Level securityContext

```yaml
spec:
  securityContext:
    runAsNonRoot: true          # reject if container would run as UID 0
    runAsUser: 1000             # run as UID 1000
    runAsGroup: 3000            # primary group GID 3000
    fsGroup: 2000               # volume ownership (files owned by GID 2000)
    fsGroupChangePolicy: "OnRootMismatch"  # only change ownership if needed
    seccompProfile:
      type: RuntimeDefault      # default seccomp profile (recommended)
    supplementalGroups: [5000]  # additional groups
```

## 2. Container-Level securityContext

```yaml
containers:
  - name: payment-service
    securityContext:
      allowPrivilegeEscalation: false    # prevent setuid/setgid exploits
      readOnlyRootFilesystem: true       # immutable container filesystem
      runAsNonRoot: true
      runAsUser: 1000
      capabilities:
        drop: ["ALL"]                     # drop all capabilities first
        add: ["NET_BIND_SERVICE"]         # add only what's needed (port <1024)
      seccompProfile:
        type: RuntimeDefault
```

## 3. Full Restricted-Compliant Container

```yaml
containers:
  - name: payment-service
    image: payment-service:v1.2.3
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      runAsNonRoot: true
      runAsUser: 10000            # high UID to avoid conflicts
      capabilities:
        drop: ["ALL"]
      seccompProfile:
        type: RuntimeDefault
    volumeMounts:
      - name: tmp              # need writable dir? mount tmpfs
        mountPath: /tmp
      - name: cache
        mountPath: /app/cache
volumes:
  - name: tmp
    emptyDir: {}
  - name: cache
    emptyDir: {}
```

`readOnlyRootFilesystem: true` forces apps to write to explicitly mounted volumes, not the container filesystem. Prevents runtime code modification.

---

# Topic 3: OPA/Gatekeeper (Policy Engine)

## 1. Why Gatekeeper?

Pod Security Standards cover basic security but can't enforce:
- Required labels on all workloads
- Specific image registry allowlist
- Cost tags on namespaces
- Specific ingress class
- Max replica count per team

Gatekeeper uses OPA (Open Policy Agent) to enforce custom policies via admission webhook.

## 2. Architecture

```text
kubectl apply -f deployment.yaml
    ↓
Kubernetes API Server
    ↓ (ValidatingAdmissionWebhook)
Gatekeeper (webhook server)
    ↓ (evaluates OPA Rego policies)
    ↓ allows or denies
API Server stores object or rejects request
```

## 3. ConstraintTemplate

Defines the policy schema and OPA Rego logic:

```yaml
apiVersion: templates.gatekeeper.sh/v1
kind: ConstraintTemplate
metadata:
  name: k8srequiredlabels
spec:
  crd:
    spec:
      names:
        kind: K8sRequiredLabels     # custom resource type created from this template
      validation:
        openAPIV3Schema:
          type: object
          properties:
            labels:
              type: array
              items:
                type: string
  targets:
    - target: admission.k8s.gatekeeper.sh
      rego: |
        package k8srequiredlabels

        violation[{"msg": msg, "details": {"missing_labels": missing}}] {
          provided := {label | input.review.object.metadata.labels[label]}
          required := {label | label := input.parameters.labels[_]}
          missing := required - provided
          count(missing) > 0
          msg := sprintf("Missing required labels: %v", [missing])
        }
```

## 4. Constraint (Instantiating the Template)

```yaml
apiVersion: constraints.gatekeeper.sh/v1beta1
kind: K8sRequiredLabels       # the kind defined in ConstraintTemplate
metadata:
  name: prod-namespace-labels
spec:
  enforcementAction: deny     # deny | warn | dryrun
  match:
    kinds:
      - apiGroups: [""]
        kinds: ["Namespace"]
    namespaceSelector:
      matchLabels:
        environment: prod
  parameters:
    labels: ["team", "cost-center", "environment"]
```

## 5. Common Gatekeeper Policies

### Require trusted image registry

```yaml
# ConstraintTemplate (summary)
# Reject pods with images not from allowed registries

parameters:
  registries: ["my-registry.example.com", "gcr.io/google-containers"]

rego: |
  violation[{"msg": msg}] {
    container := input.review.object.spec.containers[_]
    not startswith(container.image, input.parameters.registries[_])
    msg := sprintf("Image %v not from allowed registry", [container.image])
  }
```

### Block latest tag

```yaml
violation[{"msg": msg}] {
  container := input.review.object.spec.containers[_]
  endswith(container.image, ":latest")
  msg := sprintf("Container %v uses 'latest' tag — use immutable tag", [container.name])
}
```

### Require resource limits

```yaml
violation[{"msg": msg}] {
  container := input.review.object.spec.containers[_]
  not container.resources.limits.memory
  msg := sprintf("Container %v missing memory limit", [container.name])
}
```

---

# Topic 4: Kyverno (Alternative Policy Engine)

Kyverno uses YAML policies instead of Rego (easier for K8s users):

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: require-labels
spec:
  validationFailureAction: Enforce    # Enforce | Audit
  rules:
    - name: check-team-label
      match:
        any:
          - resources:
              kinds: ["Deployment", "StatefulSet"]
      validate:
        message: "Deployment must have 'team' label"
        pattern:
          metadata:
            labels:
              team: "?*"    # must exist and not be empty

---
# Kyverno mutation: auto-add labels
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: add-default-labels
spec:
  rules:
    - name: add-managed-by
      match:
        any:
          - resources:
              kinds: ["Deployment"]
      mutate:
        patchStrategicMerge:
          metadata:
            labels:
              +(managed-by): kyverno   # add if not present
```

Kyverno also supports:
- Generate: create related resources (e.g., NetworkPolicy for every new namespace)
- Verify Images: check image signatures (Cosign integration)

---

# Topic 5: Image Security

## 1. Image Signing with Cosign

```bash
# Sign image after build
cosign sign --key cosign.key my-registry.com/payment-service:v1.2.3

# Verify signature
cosign verify --key cosign.pub my-registry.com/payment-service:v1.2.3
```

## 2. Admission Policy for Signed Images

Kyverno verifies image signatures at admission time:

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: verify-image-signatures
spec:
  rules:
    - name: check-image-signature
      match:
        any:
          - resources:
              kinds: ["Pod"]
      verifyImages:
        - imageReferences:
            - "my-registry.com/*"
          attestors:
            - entries:
                - keys:
                    publicKeys: |-
                      -----BEGIN PUBLIC KEY-----
                      MFkwEwYHKoZIzj0CAQY...
                      -----END PUBLIC KEY-----
```

---

# Topic 6: Admission Webhook Architecture

```text
Two types of admission webhooks:
  ValidatingAdmissionWebhook:  can accept or reject a request
  MutatingAdmissionWebhook:    can modify the object before it's stored

Order of execution:
  1. Authentication
  2. Authorization (RBAC)
  3. Mutation admission webhooks (object may be modified)
  4. Schema validation
  5. Validation admission webhooks (accept or reject)
  6. Object stored in etcd

Gatekeeper: ValidatingAdmissionWebhook
Kyverno:    Both Mutating + Validating
```

Webhook failure policy:
```yaml
failurePolicy: Fail    # reject request if webhook is unavailable (safe default)
# vs
failurePolicy: Ignore  # allow request if webhook is unavailable (availability over security)
```

---

# Topic 7: Revision Notes

- Pod Security Standards: Privileged (anything), Baseline (no priv escalation), Restricted (hardened)
- PSA: enforced via namespace labels; modes: enforce/audit/warn
- securityContext: runAsNonRoot, readOnlyRootFilesystem, drop ALL capabilities, allowPrivilegeEscalation: false
- OPA/Gatekeeper: custom policy engine; ConstraintTemplate (logic) + Constraint (instance)
- Kyverno: YAML-native policies; validate/mutate/generate/verify images
- Image signing: Cosign; verified at admission via Kyverno or Gatekeeper
- Admission webhook order: mutation → validation → storage
- `failurePolicy: Fail`: safer (deny if webhook is down); `Ignore`: available but insecure

## Official Source Notes

- Pod Security Standards: <https://kubernetes.io/docs/concepts/security/pod-security-standards/>
- Pod Security Admission: <https://kubernetes.io/docs/concepts/security/pod-security-admission/>
- securityContext: <https://kubernetes.io/docs/tasks/configure-pod-container/security-context/>
- OPA/Gatekeeper: <https://open-policy-agent.github.io/gatekeeper/>
- Kyverno: <https://kyverno.io/docs/>
- Cosign: <https://docs.sigstore.dev/cosign/overview/>
