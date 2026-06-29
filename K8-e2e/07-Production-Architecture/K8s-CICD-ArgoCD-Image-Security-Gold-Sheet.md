# Kubernetes CI/CD with ArgoCD and Image Security Gold Sheet

> Track: K8s Interview Track — Phase 7: Production Architecture
> Goal: Design an end-to-end GitOps delivery pipeline — from code commit to production deployment — with image signing, security scanning, and automated rollbacks.

---

## 0. How To Read This

Beginner focus:
- CI builds image; CD deploys to cluster
- GitOps pull model vs push model
- ArgoCD Application basics

Intermediate focus:
- GitHub Actions CI pipeline for Kubernetes apps
- ArgoCD Image Updater for automated version bumps
- Image scanning with Trivy or Grype
- Argo Rollouts for progressive delivery

Senior / MAANG focus:
- Supply chain security: SLSA compliance
- Cosign image signing and verification at admission
- Multi-environment promotion pipeline (dev → staging → prod)
- Release engineering: semantic versioning, changelog automation
- FinOps integration: cost checks in pipeline

---

# Topic 1: The Full CI/CD Pipeline

## 1. Architecture Overview

```text
Developer pushes code to Git
  ↓ CI Pipeline (GitHub Actions / GitLab CI / Jenkins)
  1. Unit tests + integration tests
  2. Docker build → push image to registry
  3. Image vulnerability scan (Trivy)
  4. Image sign with Cosign
  5. Update image tag in GitOps repo (git commit)
  ↓ ArgoCD detects GitOps repo change
  6. ArgoCD syncs Application
  7. Argo Rollouts: canary deployment (10% → 50% → 100%)
  8. Analysis: check Prometheus error rate
  9. If error rate OK: promote to 100%
  10. If error rate bad: automatic rollback
  ↓ Done
```

## 2. Two Repos Pattern (App Repo vs Config Repo)

```text
app-repo/           (application code + Dockerfile)
  src/
  Dockerfile
  .github/workflows/ci.yaml

gitops-repo/        (Kubernetes manifests — single source of truth)
  apps/
    payment-service/
      base/
        deployment.yaml
        service.yaml
        kustomization.yaml
      overlays/
        dev/
        staging/
        prod/
          kustomization.yaml  ← CI updates image tag here

Why separate repos:
  - CI runs in app-repo (code changes)
  - ArgoCD watches gitops-repo (config changes)
  - No coupling between code CI and deployment config
  - Different access control: devs push app-repo; ArgoCD reads gitops-repo
```

---

# Topic 2: CI Pipeline (GitHub Actions)

## 1. Full CI Workflow

```yaml
# .github/workflows/ci.yaml
name: Build and Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  REGISTRY: my-registry.example.com
  IMAGE_NAME: payment-service

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run unit tests
        run: |
          go test ./... -v -race -coverprofile=coverage.txt
          
      - name: Run integration tests
        run: docker compose -f docker-compose.test.yaml up --exit-code-from test
      
  build-and-push:
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      id-token: write    # for OIDC/Cosign
      contents: read
      packages: write
    
    outputs:
      image-digest: ${{ steps.build.outputs.digest }}
      image-tag: ${{ steps.meta.outputs.tags }}
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Generate image metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=semver,pattern={{version}}
            type=sha,prefix=,suffix=,format=short
      
      - name: Login to registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ secrets.REGISTRY_USER }}
          password: ${{ secrets.REGISTRY_PASSWORD }}
      
      - name: Build and push
        id: build
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
          provenance: true    # generate SLSA provenance attestation
  
  scan:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Run Trivy vulnerability scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ needs.build-and-push.outputs.image-tag }}
          format: sarif
          severity: CRITICAL,HIGH
          exit-code: 1        # fail pipeline if CRITICAL/HIGH CVEs found
          output: trivy-results.sarif
      
      - name: Upload scan results to GitHub Security
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: trivy-results.sarif
  
  sign:
    needs: [build-and-push, scan]
    runs-on: ubuntu-latest
    permissions:
      id-token: write
    steps:
      - name: Install Cosign
        uses: sigstore/cosign-installer@v3
      
      - name: Sign image with keyless Cosign (OIDC)
        run: |
          cosign sign --yes \
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}@${{ needs.build-and-push.outputs.image-digest }}
  
  update-gitops:
    needs: sign
    runs-on: ubuntu-latest
    steps:
      - name: Update GitOps repo with new image tag
        uses: actions/github-script@v7
        with:
          github-token: ${{ secrets.GITOPS_REPO_TOKEN }}
          script: |
            const { execSync } = require('child_process');
            execSync('git clone https://github.com/myorg/gitops-repo.git');
            process.chdir('gitops-repo');
            
            // Update image tag in Kustomize overlay
            execSync(`
              cd apps/payment-service/overlays/dev &&
              kustomize edit set image payment-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
            `);
            
            execSync('git add -A');
            execSync('git commit -m "chore: update payment-service to ${{ github.sha }}"');
            execSync('git push');
```

---

# Topic 3: ArgoCD Image Updater (Alternative to Manual Update)

```text
ArgoCD Image Updater automatically updates image tags in Git
when a new image is pushed to the registry.

Eliminates the "update GitOps repo" step in CI.
```

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: payment-service-dev
  annotations:
    argocd-image-updater.argoproj.io/image-list: |
      payment-service=my-registry/payment-service
    argocd-image-updater.argoproj.io/payment-service.update-strategy: newest-build
    argocd-image-updater.argoproj.io/payment-service.kustomize.image-name: payment-service
    argocd-image-updater.argoproj.io/write-back-method: git
    argocd-image-updater.argoproj.io/git-branch: main
```

Image update strategies:
- `newest-build`: always use the newest image (good for dev)
- `semver`: use latest SemVer tag matching constraint (e.g., `^1.x`)
- `digest`: track by specific digest

---

# Topic 4: Progressive Delivery with Argo Rollouts

## 1. Canary Rollout

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: payment-service
  namespace: prod
spec:
  replicas: 10
  strategy:
    canary:
      canaryService: payment-service-canary
      stableService: payment-service-stable
      trafficRouting:
        nginx:
          stableIngress: payment-service-ingress
      steps:
        - setWeight: 5          # 5% canary
        - pause: {duration: 2m}
        - analysis:             # run analysis for 5 min at 5%
            templates:
              - templateName: success-rate
        - setWeight: 20
        - pause: {duration: 3m}
        - analysis:
            templates:
              - templateName: success-rate
        - setWeight: 50
        - pause: {duration: 5m}
        - setWeight: 100

---
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: success-rate
  namespace: prod
spec:
  args:
    - name: service-name
  metrics:
    - name: success-rate
      interval: 1m
      successCondition: result[0] >= 0.99   # 99% success rate
      failureCondition: result[0] < 0.95    # abort below 95%
      failureLimit: 2
      provider:
        prometheus:
          address: http://prometheus.monitoring:9090
          query: |
            sum(
              rate(http_requests_total{
                service="{{args.service-name}}",
                status!~"5.."
              }[5m])
            ) /
            sum(
              rate(http_requests_total{
                service="{{args.service-name}}"
              }[5m])
            )
```

## 2. Rollout Commands

```bash
# View rollout status
kubectl argo rollouts get rollout payment-service -n prod --watch

# Promote canary (skip pause)
kubectl argo rollouts promote payment-service -n prod

# Abort canary (rollback to stable)
kubectl argo rollouts abort payment-service -n prod

# Undo (rollback to previous version)
kubectl argo rollouts undo payment-service -n prod
```

---

# Topic 5: Multi-Environment Promotion Pipeline

## 1. Promotion Strategy

```text
Code commit → CI build → push image

Environments:
  dev:     Auto-deploy on every commit (ArgoCD automated sync)
  staging: Auto-deploy on every commit (separate staging cluster/namespace)
  prod:    Manual promotion via PR + ArgoCD manual sync

Promotion mechanism:
  1. Dev passes QA: open PR in GitOps repo to update prod overlay image tag
  2. PR requires:
     - 2 reviewer approvals
     - Passing smoke test workflow (runs against staging)
     - Compliance check (change ticket linked)
  3. PR merged → ArgoCD detects change → syncs prod (or manual sync trigger)
```

## 2. Environment Promotion via Kustomize Tags

```text
gitops-repo/apps/payment-service/overlays/
  dev/kustomization.yaml     ← tag: sha-abc123 (auto-updated by CI)
  staging/kustomization.yaml ← tag: sha-abc123 (auto-promoted from dev after tests)
  prod/kustomization.yaml    ← tag: v1.2.3 (manually promoted via PR)
```

---

# Topic 6: Image Security and Supply Chain

## 1. Supply Chain Security (SLSA)

```text
SLSA (Supply chain Levels for Software Artifacts):
  Level 0: No guarantees
  Level 1: Build script (Dockerfile exists)
  Level 2: Build service (GitHub Actions builds image, not local machine)
  Level 3: Hardened build (no secrets in build, signed provenance)
  Level 4: Verified build (two-party review, hermetic builds)

Tools:
  Cosign: image signing + verification
  SBOM: Software Bill of Materials (list of all dependencies)
  Syft: generate SBOM
  Grype: vulnerability scan against SBOM
  In-toto: supply chain attestation framework
```

## 2. SBOM Generation and Attestation

```bash
# Generate SBOM with Syft
syft my-registry/payment-service:v1.2.3 -o spdx-json > sbom.json

# Attach SBOM as attestation to image
cosign attest --predicate sbom.json --type spdx \
  my-registry/payment-service:v1.2.3

# Verify SBOM attestation
cosign verify-attestation --type spdx \
  my-registry/payment-service:v1.2.3 | jq .
```

## 3. Admission Verification (Kyverno + Cosign)

```yaml
apiVersion: kyverno.io/v1
kind: ClusterPolicy
metadata:
  name: verify-image-signature-and-sbom
spec:
  rules:
    - name: verify-image
      match:
        any:
          - resources:
              kinds: ["Pod"]
              namespaces: ["prod", "staging"]
      verifyImages:
        - imageReferences:
            - "my-registry.example.com/*"
          attestors:
            - entries:
                - keyless:
                    subject: "https://github.com/myorg/payment-service/.github/workflows/ci.yaml@refs/heads/main"
                    issuer: "https://token.actions.githubusercontent.com"
          attestations:
            - predicateType: https://spdx.dev/Document    # verify SBOM exists
```

---

# Topic 7: Interview Scenarios

**Scenario: How would you design a GitOps pipeline for 50 microservices?**

```text
Answer:
1. Monorepo for GitOps configs (one repo, folders per service)
2. ArgoCD ApplicationSet generates Application per service per environment
   (matrix generator: services x environments)
3. CI pipeline per service (GitHub Actions) updates image tag via PR bot
4. Staged promotion: dev (auto) → staging (auto after tests) → prod (manual PR)
5. Argo Rollouts for prod canary with Prometheus-based analysis
6. Kyverno enforces: signed images, SBOM attestation, no `latest` tags
7. ArgoCD project per team for access isolation
8. Notifications: ArgoCD → Slack on deploy events, failures, rollbacks
```

**Scenario: Canary bad, how to rollback?**

```text
Immediate (if caught in analysis):
  Analysis auto-aborts → Argo Rollouts reverts to stable automatically

Manual abort:
  kubectl argo rollouts abort payment-service -n prod
  → Traffic returns to stable (old) pods instantly

Or via ArgoCD:
  Revert the GitOps repo commit (git revert)
  ArgoCD detects and re-syncs
  Rolling update back to previous version
```

---

# Topic 8: Revision Notes

- CI/CD: CI builds/tests/scans/signs image; CD deploys via GitOps (no direct kubectl push)
- Two-repo pattern: app-repo (code) + gitops-repo (manifests); prevents coupling
- Image scanning: Trivy; fail pipeline on CRITICAL/HIGH CVEs
- Cosign: signs image with OIDC keyless; verified at admission by Kyverno
- ArgoCD Image Updater: auto-update image tags in Git when new image pushed
- Argo Rollouts: canary with traffic weights; AnalysisTemplate with Prometheus; auto-abort on bad metrics
- Multi-env promotion: dev=auto, staging=auto, prod=manual PR approval
- SLSA: supply chain security levels; SBOM generation (Syft); attestation (Cosign + in-toto)
- GitOps security: gitops-repo requires PR approval; ArgoCD reads-only from Git

## Official Source Notes

- ArgoCD: <https://argo-cd.readthedocs.io/>
- Argo Rollouts: <https://argoproj.github.io/rollouts/>
- ArgoCD Image Updater: <https://argocd-image-updater.readthedocs.io/>
- Cosign: <https://docs.sigstore.dev/cosign/>
- SLSA: <https://slsa.dev/>
- Kyverno image verify: <https://kyverno.io/docs/writing-policies/verify-images/>
