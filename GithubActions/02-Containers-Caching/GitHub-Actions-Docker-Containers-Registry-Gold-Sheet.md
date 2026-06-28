# GitHub Actions Docker, Containers, and Registry Pipelines Gold Sheet

> Goal: build production-grade Docker image pipelines with safe tags, registry pushes, scanning, SBOM/provenance, and image promotion.

---

## 0. How To Read This

Beginner focus:

- Docker build
- image tag
- push to registry
- build artifact

Intermediate focus:

- Buildx
- Docker layer cache
- GHCR/ECR/ACR/GCR
- image scanning
- semantic tags

Senior focus:

- immutable image promotion
- SBOM
- provenance/attestations
- multi-arch builds
- supply-chain risk
- deployment traceability

---

# Topic 1: Docker, Containers, and Registry Pipelines

---

## 1. Intuition

A container pipeline turns source code into a deployable, versioned package.

Good pipeline:

```text
test code
-> build image
-> tag immutably
-> scan
-> push to registry
-> deploy/promote same image
```

Beginner explanation:

GitHub Actions can build a Docker image from your app, tag it with a commit SHA or version, push it to a container registry, and later deploy that exact image.

---

## 2. Definition

- Definition: A container CI/CD pipeline builds, validates, stores, scans, and promotes container images using GitHub Actions and a registry.
- Category: Build and release pipeline
- Core idea: build once, scan once, deploy/promote the same immutable image.

---

## 3. Why It Exists

Without a proper image pipeline:

- each environment may build different artifacts
- rollbacks are unclear
- image tags are overwritten
- vulnerabilities are missed
- production cannot be traced to commit SHA
- deployments depend on local machines

Container pipelines create repeatable deployable artifacts.

---

## 4. Reality

Used for:

- Spring Boot APIs
- Node backends
- Next.js SSR apps
- Kubernetes workloads
- batch jobs
- microservices
- platform tools

Registries:

- GHCR
- AWS ECR
- Azure ACR
- Google Artifact Registry/GCR
- Docker Hub

---

## 5. How It Works

### Part A: Image Build Flow

```text
push to main
-> run tests
-> log in to registry
-> build Docker image
-> tag image with SHA and version
-> scan image
-> push image
-> deploy or promote
```

### Part B: Tag Strategy

Good tags:

- commit SHA: `app:sha-abc123`
- semantic version: `app:v1.4.2`
- branch snapshot: `app:main-abc123`
- environment promotion metadata outside tag

Dangerous tags:

- only `latest`
- mutable `prod`
- overwritten release tags

Rule:

> Deploy immutable tags. Use labels or registry metadata for human meaning.

### Part C: Buildx Pipeline

```yaml
name: Docker Build

on:
  push:
    branches: [main]

permissions:
  contents: read
  packages: write

jobs:
  image:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3

      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ghcr.io/my-org/my-app:sha-${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### Part D: Multi-Stage Dockerfile

Good Dockerfiles:

- build in one stage
- run in smaller runtime stage
- avoid secrets in image layers
- use non-root user where possible
- pin base image thoughtfully
- add labels for traceability

Example labels:

```dockerfile
LABEL org.opencontainers.image.revision=$GIT_SHA
LABEL org.opencontainers.image.source=$REPO_URL
```

### Part E: Image Scanning

Scan for:

- OS package vulnerabilities
- language dependency vulnerabilities
- secrets in image
- bad base images
- license risk if required

Tools:

- Trivy
- Grype
- Docker Scout
- registry-native scanners

Decision:

- fail on critical exploitable vulnerabilities
- allow documented exceptions with expiry
- avoid blocking on noisy low-risk findings without process

### Part F: SBOM and Provenance

SBOM answers:

```text
What is inside this image?
```

Provenance answers:

```text
Who built it, from what source, using what workflow?
```

Use for:

- supply-chain security
- audits
- incident response
- vulnerability impact analysis

### Part G: Build Once, Promote Many

Bad:

```text
build image separately for dev, stage, prod
```

Better:

```text
build once
push immutable image
deploy same digest to dev
promote same digest to stage
promote same digest to prod
```

This prevents "works in stage but prod had a different build."

### Part H: Registry Authentication

GHCR:

- can use `GITHUB_TOKEN` with package permissions

Cloud registries:

- prefer OIDC to assume cloud role
- avoid long-lived registry passwords where possible

---

## 6. What Problem It Solves

- Primary problem solved: creates traceable, immutable deployable container artifacts
- Secondary benefits: rollback, scanning, reproducibility, promotion, audit
- Systems impact: decouples build from deployment and improves release safety

---

## 7. When To Rely On It

Use container pipelines when:

- deploying to Kubernetes
- running backend services
- using standardized runtime images
- multiple environments need same artifact
- rollback must be quick
- image scanning is required

---

## 8. When Not To Overdo It

Avoid Docker complexity when:

- app is purely static and a simpler artifact deploy works
- platform builds containers automatically
- local development and production shapes diverge too much

Still keep:

- artifact immutability
- versioning
- scanning
- rollback plan

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Immutable deployable artifact | Image build can be slow |
| Same artifact across environments | Registry auth/security required |
| Easy rollback by digest/tag | Vulnerability noise needs process |
| Works well with Kubernetes | Dockerfile quality matters |
| Supports SBOM/provenance | Multi-arch builds add cost |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- SHA tags:
  Great traceability, less human-friendly.
- SemVer tags:
  Human-friendly, but need release discipline.
- Multi-arch:
  More compatibility, slower builds.
- Strict scan gates:
  Stronger security, possible delivery friction.

### Common Mistakes

- Mistake: "Deploy `latest`."
  Why it is wrong: not immutable or auditable.
  Better approach: deploy SHA or digest.

- Mistake: "Rebuild in each environment."
  Why it is wrong: prod may not match tested artifact.
  Better approach: build once, promote the same image.

- Mistake: "Put secrets in Docker build args."
  Why it is wrong: secrets may remain in layers/history.
  Better approach: use secret mounts or runtime secrets.

- Mistake: "Ignore image scanning."
  Why it is wrong: vulnerable base images enter production.
  Better approach: scan and manage exceptions.

---

## 11. Key Numbers

Useful targets:

- image tag includes commit SHA
- production deploy records image digest
- image scan runs before promotion
- old images are retained according to rollback/compliance policy
- Docker cache should reduce rebuild time but not hide changes

---

## 12. Failure Modes

### Registry Push Fails

Causes:

- missing permission
- wrong registry login
- package visibility issue
- cloud role trust failure

Fix:

- check `packages: write`
- validate registry URL
- validate OIDC/IAM role

### Image Works Locally But Fails In Kubernetes

Causes:

- missing env vars
- wrong port
- root filesystem assumptions
- platform architecture mismatch

Fix:

- run container smoke test in CI
- document runtime config
- use health checks

### Vulnerability Scan Blocks Release

Fix:

- classify severity and exploitability
- patch base image/dependencies
- document temporary exception with expiry
- never ignore silently

---

## 13. Scenario

- Product / system: Spring Boot service deployed to Kubernetes
- Why this concept fits: CI must produce a versioned image that can be promoted and rolled back
- What would go wrong without it: environments may deploy different builds and rollback becomes guesswork

---

## 14. Code Sample

Build and push to GHCR:

```yaml
name: Build Container

on:
  push:
    branches: [main]

permissions:
  contents: read
  packages: write

jobs:
  docker:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ghcr.io/my-org/order-service:sha-${{ github.sha }}
          labels: |
            org.opencontainers.image.revision=${{ github.sha }}
            org.opencontainers.image.source=${{ github.repositoryUrl }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

---

## 15. Mini Program / Simulation

Tag safety check:

```python
def is_safe_prod_tag(tag):
    return tag.startswith("sha-") or tag.startswith("v") or "@sha256:" in tag


for tag in ["latest", "prod", "sha-abc123", "v1.2.3"]:
    print(tag, "safe-ish:", is_safe_prod_tag(tag))
```

---

## 16. Practical Question

> How would you design a GitHub Actions Docker pipeline for a production backend service?

---

## 17. Strong Answer

I would run tests first, then build a Docker image using Buildx. The image would be tagged with the commit SHA and optionally a semantic version for releases. I would push it to a registry such as GHCR or ECR, scan it for vulnerabilities, and record the image digest.

I would not rebuild separately per environment. The same immutable image should be promoted from dev to stage to prod. Production deployments should reference the digest or SHA tag, and rollback should redeploy a previous known-good image. For cloud registries, I would prefer OIDC-based auth over long-lived credentials.

---

## 18. Revision Notes

- One-line summary: Docker pipelines produce immutable, scanned, traceable deployable images.
- Three keywords: tag, scan, promote
- One interview trap: `latest` is not a production release strategy.
- One memory trick: build once, scan once, promote many.

---

## 19. Official Source Notes

- Publishing Docker images: <https://docs.github.com/en/actions/use-cases-and-examples/publishing-packages/publishing-docker-images>
- GitHub Packages permissions: <https://docs.github.com/en/packages/learn-github-packages/about-permissions-for-github-packages>
- Artifact attestations: <https://docs.github.com/en/actions/security-for-github-actions/using-artifact-attestations>

