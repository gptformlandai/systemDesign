# GitHub Actions Deployments, Environments, Kubernetes, and Cloud Gold Sheet

> Goal: design safe GitHub Actions deployment pipelines for dev, stage, prod, Kubernetes, cloud platforms, approvals, progressive delivery, and rollback.

---

## 0. How To Read This

Beginner focus:

- deploy job
- environment
- secret
- approval
- rollback

Intermediate focus:

- GitHub environments
- deployment protection rules
- Kubernetes
- Helm/Kustomize
- concurrency
- smoke tests

Senior focus:

- artifact promotion
- canary/blue-green/rolling
- production freeze
- blast radius
- deployment observability
- rollback strategy

---

# Topic 1: Deployments, Environments, Kubernetes, and Cloud

---

## 1. Intuition

CI asks:

```text
Is the code good?
```

CD asks:

```text
Can we safely put this exact artifact into an environment and recover if it fails?
```

Beginner explanation:

A deployment workflow takes a tested artifact or image and releases it to an environment such as dev, stage, or production, often with approvals and rollback support.

---

## 2. Definition

- Definition: A GitHub Actions deployment pipeline automates controlled release of artifacts to environments with approvals, secrets, concurrency, validation, and rollback.
- Category: Continuous Delivery / Continuous Deployment
- Core idea: deploy the same verified artifact through environments safely and observably.

---

## 3. Why It Exists

Without deployment automation:

- prod deploys are manual
- release steps differ by engineer
- approvals are informal
- rollback is slow
- audit trail is weak
- environment secrets are mishandled
- deployments overlap

Deployment workflows make releases repeatable and traceable.

---

## 4. Reality

Deployment targets:

- Kubernetes
- AWS ECS/EKS/Lambda/S3/CloudFront
- Azure App Service/AKS/Static Web Apps
- GCP Cloud Run/GKE
- Vercel/Netlify/Firebase
- on-prem/self-hosted servers

Production-grade deployment includes:

- artifact identity
- environment approval
- serialized prod deploys
- smoke tests
- monitoring
- rollback

---

## 5. How It Works

### Part A: Deployment Flow

```text
main branch or release tag
-> build artifact/image
-> scan
-> deploy to dev
-> run smoke tests
-> promote to stage
-> approval
-> deploy to prod
-> verify
-> rollback if needed
```

### Part B: GitHub Environments

Environments let you define:

- environment secrets
- environment variables
- required reviewers
- deployment protection rules
- deployment history

Example:

```yaml
jobs:
  deploy-prod:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - run: ./deploy-prod.sh
```

### Part C: Deployment Concurrency

Prevent two production deploys at once:

```yaml
concurrency:
  group: deploy-production
  cancel-in-progress: false
```

For PR preview deploys:

```yaml
concurrency:
  group: preview-${{ github.event.pull_request.number }}
  cancel-in-progress: true
```

### Part D: Kubernetes Deploy

Common tools:

- `kubectl`
- Helm
- Kustomize
- Argo CD
- Flux

Direct deploy example:

```yaml
- run: kubectl set image deployment/order-service order-service=ghcr.io/org/order-service:sha-${{ github.sha }}
- run: kubectl rollout status deployment/order-service
```

Senior note:

For larger production platforms, GitOps tools such as Argo CD/Flux may be preferable. GitHub Actions updates desired state; the cluster reconciles it.

### Part E: Helm

```yaml
- run: |
    helm upgrade --install order-service ./charts/order-service \
      --namespace prod \
      --set image.tag=sha-${{ github.sha }} \
      --wait
```

Helm is useful for:

- templated Kubernetes resources
- environment-specific values
- release history
- rollback support

### Part F: Kustomize

Kustomize is useful when:

- overlays differ by environment
- you want YAML-native patching
- GitOps repo stores final desired state

### Part G: Cloud Deployment

Use OIDC when possible:

```yaml
permissions:
  contents: read
  id-token: write
```

Avoid:

- long-lived cloud keys
- broad admin roles
- production secrets in PR workflows

### Part H: Deployment Strategies

| Strategy | Meaning | Use |
|---|---|---|
| Rolling | gradually replace pods/instances | common default |
| Blue-green | switch traffic from old to new environment | fast rollback |
| Canary | send small traffic to new version | risk control |
| Feature flag | enable feature gradually | app-level control |

### Part I: Smoke Tests

After deployment:

- health endpoint
- version endpoint
- login/basic flow
- database connectivity
- queue/event processing
- frontend asset load

Example:

```yaml
- run: curl --fail https://api.example.com/health
```

### Part J: Rollback

Rollback options:

- redeploy previous image digest
- Helm rollback
- Kubernetes rollout undo
- switch blue/green traffic
- disable feature flag
- rollback frontend asset version

Senior answer:

> A deployment is not production-ready unless rollback is designed before the release.

---

## 6. What Problem It Solves

- Primary problem solved: safe movement of verified artifacts into real environments
- Secondary benefits: auditability, approval, rollback, consistency, release confidence
- Systems impact: reduces deployment risk and human error

---

## 7. When To Rely On It

Use deployment workflows when:

- releases are frequent
- multiple environments exist
- approvals are required
- audit trails matter
- rollback must be fast
- deployments should be repeatable

---

## 8. When Not To Directly Deploy From Actions

Consider GitOps or deployment platforms when:

- many clusters/environments exist
- cluster credentials should not live in CI
- progressive delivery is complex
- platform already uses Argo CD/Flux/Spinnaker
- compliance requires stronger release orchestration

GitHub Actions can still build, scan, and update desired state.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Traceable deployments | YAML complexity |
| GitHub environment approvals | Requires secret/permission discipline |
| Works with many targets | Direct cluster credentials can be risky |
| Easy to connect with CI | Rollback must be designed |
| Good audit trail | Complex progressive delivery may need extra tools |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Automatic deploy:
  Faster, but needs strong tests and rollback.
- Manual approval:
  Safer governance, but slower.
- Direct Kubernetes deploy:
  Simple, but CI holds cluster access.
- GitOps:
  Safer separation, but adds another system.

### Common Mistakes

- Mistake: "Build separately for prod."
  Why it is wrong: prod artifact may differ from tested artifact.
  Better approach: build once, promote.

- Mistake: "No deployment concurrency."
  Why it is wrong: overlapping deploys create race conditions.
  Better approach: serialize environment deployments.

- Mistake: "No smoke test."
  Why it is wrong: workflow may succeed even if app is unhealthy.
  Better approach: verify after deploy.

- Mistake: "Rollback is manual tribal knowledge."
  Why it is wrong: incidents need speed.
  Better approach: codify rollback workflow.

---

## 11. Key Numbers

Useful targets:

- production deploys should be serialized
- rollback should be faster than forward fix for common failures
- canary starts with small traffic percentage
- deployment timeout should match platform rollout behavior
- smoke tests should be fast and reliable

---

## 12. Failure Modes

### Deployment Hangs

Causes:

- pods not ready
- bad image
- migration issue
- missing secret

Fix:

- rollout status
- pod events/logs
- rollback previous image

### Approval Stuck

Causes:

- wrong environment
- required reviewer unavailable
- deployment protection rule issue

Fix:

- verify environment config
- clarify approval ownership

### Smoke Test Fails

Action:

- stop promotion
- rollback if prod
- inspect logs/metrics
- keep artifact for debugging

### Two Deploys Collide

Cause:

- missing concurrency

Fix:

- environment-specific concurrency group

---

## 13. Scenario

- Product / system: Kubernetes microservice production deployment
- Why this concept fits: image must be promoted through environments with approval and rollback
- What would go wrong without it: overlapping deploys, unapproved prod changes, and unclear rollback

---

## 14. Code Sample

Production deployment skeleton:

```yaml
name: Deploy Production

on:
  workflow_dispatch:
    inputs:
      image:
        required: true
        type: string

permissions:
  contents: read
  id-token: write

concurrency:
  group: deploy-production
  cancel-in-progress: false

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4
      - run: echo "Authenticate to cloud with OIDC here"
      - run: |
          helm upgrade --install order-service ./charts/order-service \
            --namespace prod \
            --set image.tag=${{ inputs.image }} \
            --wait
      - run: curl --fail https://api.example.com/health
```

---

## 15. Mini Program / Simulation

Deployment decision:

```python
def should_promote(smoke_ok, error_rate_ok, approval_ok):
    return smoke_ok and error_rate_ok and approval_ok

print(should_promote(True, True, True))
print(should_promote(True, False, True))
```

---

## 16. Practical Question

> How would you design GitHub Actions deployment to production for a Kubernetes backend?

---

## 17. Strong Answer

I would build and scan the image once, tag it immutably, and promote the same image digest through dev, stage, and production. Production deployment would use a GitHub environment with required reviewers and environment-specific secrets or OIDC role.

I would serialize production deployments using concurrency, deploy with Helm or GitOps, wait for rollout, and run smoke tests. Rollback would redeploy the previous image digest or use Helm/Kubernetes rollback. I would avoid long-lived cloud credentials and make deployment logs/artifacts traceable to the commit SHA.

---

## 18. Revision Notes

- One-line summary: Deployment workflows promote verified artifacts into environments with approval, verification, and rollback.
- Three keywords: environment, promotion, rollback
- One interview trap: deployment success is not the same as application health.
- One memory trick: build once, approve carefully, deploy safely, verify, rollback fast.

---

## 19. Official Source Notes

- Deployments and environments: <https://docs.github.com/en/actions/deployment>
- OpenID Connect: <https://docs.github.com/en/actions/concepts/security/openid-connect>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>

