# Kubernetes Helm, Kustomize, and GitOps Gold Sheet

> Track: K8s Interview Track — Phase 6: Advanced Patterns
> Goal: Package, configure, and continuously deliver Kubernetes applications using Helm and GitOps workflows — the pattern used at every large-scale K8s deployment.

---

## 0. How To Read This

Beginner focus:
- What Helm charts are and how to use them
- Helm vs Kustomize decision
- GitOps concept overview

Intermediate focus:
- Writing Helm charts from scratch
- Kustomize bases, overlays, patches
- ArgoCD GitOps workflow
- Sync waves and resource hooks

Senior / MAANG focus:
- Helm library charts and DRY patterns
- Helmfile for multi-chart orchestration
- ArgoCD ApplicationSet for multi-cluster/multi-tenant delivery
- GitOps with image updater (automated version bumps)
- Progressive delivery: Argo Rollouts (canary, blue/green)

---

# Topic 1: Helm

## 1. What Helm Does

```text
Problem: Kubernetes YAML is verbose. Same deployment manifest repeated
for dev/staging/prod with only image tag, replicas, and resource values changing.

Helm: package manager for Kubernetes.
  - Chart: package of K8s manifests with Go templates
  - Values: configuration variables injected into templates
  - Release: one installed instance of a chart
  - Repository: central chart registry (like PyPI for Python)
```

## 2. Helm Commands

```bash
# Add chart repository
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Search for charts
helm search repo postgresql

# Install a chart
helm install payment-db bitnami/postgresql \
  --namespace prod \
  --create-namespace \
  -f custom-values.yaml \
  --set auth.password=mysecret \
  --version 12.5.0

# List releases
helm list -n prod

# Upgrade
helm upgrade payment-db bitnami/postgresql \
  -n prod \
  -f custom-values.yaml \
  --version 13.0.0

# Rollback
helm rollback payment-db 1 -n prod    # rollback to revision 1

# View history
helm history payment-db -n prod

# Uninstall
helm uninstall payment-db -n prod

# Render templates (dry run — see what would be created)
helm template payment-service ./my-chart -f prod-values.yaml
helm install payment-service ./my-chart --dry-run --debug
```

## 3. Helm Chart Structure

```text
my-service/
├── Chart.yaml          # chart metadata
├── values.yaml         # default values
├── values-dev.yaml     # environment-specific overrides
├── values-prod.yaml    # prod overrides
├── templates/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── configmap.yaml
│   ├── hpa.yaml
│   ├── serviceaccount.yaml
│   ├── _helpers.tpl    # reusable template functions
│   └── NOTES.txt       # post-install notes
└── charts/             # sub-charts (dependencies)
```

## 4. Chart.yaml

```yaml
apiVersion: v2
name: payment-service
description: Payment microservice Helm chart
type: application         # or library (reusable templates, no install)
version: 1.2.3            # chart version (semantic)
appVersion: "2.0.1"       # app version (informational)
dependencies:
  - name: postgresql
    version: "12.x.x"
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
```

## 5. values.yaml

```yaml
replicaCount: 3

image:
  repository: my-registry/payment-service
  tag: "v1.2.3"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: true
  className: nginx
  host: payment.myapp.com
  tls:
    enabled: true
    secretName: payment-tls

resources:
  requests:
    cpu: 250m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
  targetCPUUtilizationPercentage: 60

postgresql:
  enabled: false    # use external DB in prod

config:
  logLevel: info
  cacheEnabled: true
```

## 6. Template Example

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "payment-service.fullname" . }}
  namespace: {{ .Release.Namespace }}
  labels:
    {{- include "payment-service.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "payment-service.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "payment-service.selectorLabels" . | nindent 8 }}
    spec:
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - containerPort: 8080
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
          env:
            - name: LOG_LEVEL
              value: {{ .Values.config.logLevel | quote }}
          {{- if .Values.ingress.enabled }}
          # ... conditional blocks
          {{- end }}
```

---

# Topic 2: Kustomize

## 1. What Kustomize Does

```text
Kustomize: template-free customization via overlays.
  - No templating language (pure YAML patching)
  - Built into kubectl (kubectl apply -k .)
  - Base: shared YAML for all environments
  - Overlay: environment-specific patches applied on top of base

Helm vs Kustomize:
  Helm:      powerful templates, packaging, versioning, release management
  Kustomize: simpler, no learning curve, great for config variants of same app

Use Helm for: third-party charts, complex parameterization, package distribution
Use Kustomize for: in-house apps with environment overlays, GitOps declarative configs
Both: use Helm to install, Kustomize to patch Helm output
```

## 2. Directory Structure

```text
k8s/
├── base/
│   ├── kustomization.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── configmap.yaml
└── overlays/
    ├── dev/
    │   ├── kustomization.yaml
    │   └── patch-replicas.yaml
    ├── staging/
    │   ├── kustomization.yaml
    │   └── patch-resources.yaml
    └── prod/
        ├── kustomization.yaml
        ├── patch-replicas.yaml
        └── patch-resources.yaml
```

## 3. Base kustomization.yaml

```yaml
# k8s/base/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml
  - service.yaml
  - configmap.yaml
commonLabels:
  app: payment-service
```

## 4. Overlay kustomization.yaml

```yaml
# k8s/overlays/prod/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: prod
bases:
  - ../../base
patchesStrategicMerge:
  - patch-replicas.yaml
  - patch-resources.yaml
images:
  - name: payment-service
    newTag: "v1.2.3"              # update image tag per environment
configMapGenerator:
  - name: payment-service-config
    literals:
      - LOG_LEVEL=info
      - APP_ENV=production
```

## 5. Patch Files

```yaml
# k8s/overlays/prod/patch-replicas.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 5    # override base's replicas

---
# k8s/overlays/prod/patch-resources.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  template:
    spec:
      containers:
        - name: payment-service
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "1"
              memory: "1Gi"
```

## 6. Apply Kustomize

```bash
# Dry run (preview)
kubectl kustomize k8s/overlays/prod | kubectl apply --dry-run=client -f -

# Apply
kubectl apply -k k8s/overlays/prod

# Build (just render, don't apply)
kubectl kustomize k8s/overlays/prod > rendered-prod.yaml
```

---

# Topic 3: ArgoCD GitOps

## 1. GitOps Principles

```text
GitOps = Git as the single source of truth for infrastructure and applications.

Principles:
  1. Declarative: all desired state in Git (K8s YAML, Helm charts, Kustomize)
  2. Versioned: Git history = change history + instant rollback
  3. Pulled: controller (ArgoCD) pulls from Git and applies (not CI push)
  4. Reconciled: ArgoCD continuously ensures cluster state matches Git state

Benefits:
  - Rollback = git revert + ArgoCD auto-syncs
  - Audit trail = git log
  - No kubectl access needed for developers (only Git access)
  - Disaster recovery = re-apply Git repo to new cluster
```

## 2. ArgoCD Application

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: payment-service-prod
  namespace: argocd
spec:
  project: prod              # ArgoCD project for access control
  source:
    repoURL: https://github.com/myorg/k8s-gitops
    targetRevision: main
    path: apps/payment-service/overlays/prod    # Kustomize overlay path
    # OR for Helm:
    # chart: payment-service
    # repoURL: https://my-helm-registry.example.com
    # targetRevision: 1.2.3
    # helm:
    #   valueFiles: [values-prod.yaml]

  destination:
    server: https://kubernetes.default.svc    # target cluster
    namespace: prod

  syncPolicy:
    automated:
      prune: true           # delete resources no longer in Git
      selfHeal: true        # re-sync if cluster drifts from Git
      allowEmpty: false     # never sync empty config (safety)
    syncOptions:
      - CreateNamespace=true
      - PrunePropagationPolicy=foreground
      - ApplyOutOfSyncOnly=true    # only apply changed resources
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

## 3. ArgoCD ApplicationSet (Multi-Cluster / Multi-App)

```yaml
# Deploy same app to multiple clusters from one definition
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: payment-service-all-clusters
  namespace: argocd
spec:
  generators:
    - clusters:
        selector:
          matchLabels:
            environment: prod        # all clusters labeled environment=prod
  template:
    metadata:
      name: 'payment-service-{{name}}'   # {{name}} = cluster name
    spec:
      project: prod
      source:
        repoURL: https://github.com/myorg/k8s-gitops
        targetRevision: main
        path: 'apps/payment-service/overlays/{{metadata.labels.region}}'
      destination:
        server: '{{server}}'         # cluster API server URL
        namespace: prod
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
```

## 4. ArgoCD Sync Waves and Hooks

Control the order of resource creation during sync:

```yaml
# Wave -1: create CRDs first
metadata:
  annotations:
    argocd.argoproj.io/sync-wave: "-1"

# Wave 0 (default): everything else

# Wave 1: after all wave-0 resources are healthy
metadata:
  annotations:
    argocd.argoproj.io/sync-wave: "1"
```

Hooks:
```yaml
# PreSync: run migration Job before sync
metadata:
  annotations:
    argocd.argoproj.io/hook: PreSync
    argocd.argoproj.io/hook-delete-policy: HookSucceeded

# PostSync: run smoke tests after sync
metadata:
  annotations:
    argocd.argoproj.io/hook: PostSync
```

---

# Topic 4: Argo Rollouts (Progressive Delivery)

```text
Argo Rollouts extends Kubernetes with advanced deployment strategies:
  - Canary: gradual traffic shifting (5% → 20% → 50% → 100%)
  - Blue/Green: instant traffic switch with verification
  - Analysis: automated metric analysis during rollout (abort if error rate spikes)
```

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: payment-service
spec:
  replicas: 10
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
  strategy:
    canary:
      steps:
        - setWeight: 10         # send 10% traffic to canary
        - pause: {duration: 5m} # wait 5 minutes, analyze metrics
        - setWeight: 30
        - pause: {duration: 5m}
        - setWeight: 60
        - pause: {duration: 5m}
        - setWeight: 100        # full rollout
      canaryService: payment-service-canary
      stableService: payment-service-stable
      trafficRouting:
        nginx:
          stableIngress: payment-service-ingress
      analysis:
        templates:
          - templateName: success-rate
        startingStep: 1
        args:
          - name: service-name
            value: payment-service-canary
```

---

# Topic 5: Interview Scenarios

**Scenario: Deploy a new version to 100 teams' clusters with different configs**

```text
Answer:
1. Define base chart in Helm (versioned, published to OCI registry)
2. Each team has Kustomize overlay for their namespace/config
3. ArgoCD ApplicationSet generates one Application per cluster/team
4. CI pipeline:
   a. Build image → push to registry
   b. Update image tag in GitOps repo (git commit)
   c. ArgoCD detects git change → syncs all matching Applications
5. Progressive rollout via Argo Rollouts (canary first for critical services)
6. Rollback: git revert commit → ArgoCD reconciles automatically
```

**Scenario: Helm vs Kustomize decision**

```text
Use Helm when:
  - Distributing to external users (charted, versioned)
  - Complex conditional logic needed
  - Third-party app with many configuration options

Use Kustomize when:
  - Internal team's own app
  - Simple environment differentiation
  - Already have plain YAML, just need overlays
  - Want no template language overhead

Use both: Helm renders base chart, Kustomize patches output
  helm template app ./chart | kustomize build -
```

---

# Topic 6: Revision Notes

- Helm: chart (template + values), release (installed instance), value overrides per environment
- `helm upgrade --install`: idempotent install+upgrade in CI
- Kustomize: base + overlay; no templates; `patchesStrategicMerge` for YAML patches; `images:` for tag updates
- GitOps: Git = source of truth; controller pulls and reconciles; ArgoCD implements GitOps
- ArgoCD Application: watches Git path; syncs to cluster; `automated.selfHeal` re-syncs on drift
- ApplicationSet: generates Applications from list/cluster/git generator; multi-cluster delivery
- Sync waves: control order of resource application; `argocd.argoproj.io/sync-wave: "1"`
- Argo Rollouts: canary/blue-green with traffic shifting; analysis templates; integrated with Ingress controllers

## Official Source Notes

- Helm: <https://helm.sh/docs/>
- Kustomize: <https://kustomize.io/>
- ArgoCD: <https://argo-cd.readthedocs.io/>
- Argo Rollouts: <https://argoproj.github.io/rollouts/>
