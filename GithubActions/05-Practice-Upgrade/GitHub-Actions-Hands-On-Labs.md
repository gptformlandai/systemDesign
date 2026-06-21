# GitHub Actions Hands On Labs

> Track: GitHub Actions Interview Track - Practice Upgrade  
> Goal: turn GitHub Actions concepts into buildable workflow design exercises.

Each lab should take 30-120 minutes.

---

## 1. Lab Output Rules

For every lab, produce:

1. Short design note.
2. Workflow YAML sketch or pseudocode.
3. Trigger and trust boundary.
4. Permissions and secrets/OIDC decision.
5. Artifact/cache decision.
6. Failure/rollback path.
7. 60-second interview explanation.

---

## 2. Lab 1: Minimal PR CI

Build/sketch:

- `pull_request` trigger
- checkout
- setup runtime
- install dependencies
- lint/test/build
- upload test report with `if: always()`
- `permissions: contents: read`

Outcome:

```text
Explain basic workflow/job/step/action/runner syntax.
```

---

## 3. Lab 2: Backend CI With Service Container

Build/sketch:

- Java/Node/Python backend CI
- Postgres or Redis service container
- health check
- dependency cache
- test report artifact

Outcome:

```text
Explain service container readiness and integration test flakiness.
```

---

## 4. Lab 3: Frontend CI And Preview Plan

Design:

- lint/typecheck/test/build
- Playwright or Cypress smoke tests
- build artifact
- PR preview deployment
- cleanup on PR close
- preview concurrency per PR

Outcome:

```text
Explain preview environment lifecycle and cost controls.
```

---

## 5. Lab 4: Cache Key Design

Design cache keys for:

- npm/pnpm/yarn
- Maven/Gradle
- Python pip
- Docker build layers

Must include:

- OS/toolchain
- lockfile hash
- restore key caution
- cache poisoning boundary

Outcome:

```text
Explain cache vs artifact and when cache should not be trusted.
```

---

## 6. Lab 5: Fan-Out Fan-In Workflow

Build/sketch:

```text
plan -> lint/test/security/build -> summary
```

Must include:

- `needs`
- summary job with `if: always()`
- parallel jobs
- required checks decision

Outcome:

```text
Explain workflow dependency architecture.
```

---

## 7. Lab 6: Dynamic Matrix For Monorepo

Build/sketch:

- planner detects changed services
- emits JSON matrix
- service CI runs per changed service
- full matrix runs nightly

Outcome:

```text
Explain affected builds and matrix explosion prevention.
```

---

## 8. Lab 7: Reusable Workflow Contract

Design a reusable backend CI workflow:

- inputs: language version, working directory, test command
- secrets: explicit only if required
- outputs: artifact name, coverage path
- permissions: least privilege
- versioning policy

Outcome:

```text
Explain reusable workflows as APIs.
```

---

## 9. Lab 8: Composite Action vs Reusable Workflow

Create a comparison table for:

- setup language and cache steps
- entire PR CI policy
- Docker build and scan
- deployment approval workflow

Outcome:

```text
Explain when to share steps vs jobs/workflows.
```

---

## 10. Lab 9: Docker Build Scan Push

Build/sketch:

- Docker Buildx
- registry login
- image tags: SHA and SemVer
- labels with source/revision
- vulnerability scan
- SBOM/provenance where appropriate
- push immutable image

Outcome:

```text
Explain image traceability and why not to deploy `latest`.
```

---

## 11. Lab 10: OIDC Cloud Deploy

Design:

- `permissions: id-token: write, contents: read`
- environment: production
- cloud role trust scoped to repo/ref/environment
- no static cloud secrets
- deploy summary

Outcome:

```text
Explain OIDC and its trust policy boundary.
```

---

## 12. Lab 11: Secure Fork PR Workflow

Design two workflows:

1. `pull_request` for untrusted code tests with no secrets.
2. safe `pull_request_target` metadata workflow for labels/comments only.

Outcome:

```text
Explain why checking out PR code in `pull_request_target` is dangerous.
```

---

## 13. Lab 12: Environment Gate Deploy

Build/sketch:

- stage deploy
- production deploy with required reviewers
- environment secrets
- branch/tag restriction
- deployment concurrency
- rollback job or manual dispatch

Outcome:

```text
Explain environment approvals and deploy serialization.
```

---

## 14. Lab 13: Terraform Plan Apply Pipeline

Design:

- PR plan only
- upload plan artifact
- policy checks
- apply only from protected branch/environment
- apply reviewed plan
- drift detection schedule

Outcome:

```text
Explain why untrusted PRs should not apply infrastructure.
```

---

## 15. Lab 14: Database Migration Gate

Design:

- migration lint/check
- expand/contract review
- backup/restore plan
- stage migration
- production approval
- rollback/forward-fix decision

Outcome:

```text
Explain why DB rollback is not the same as app rollback.
```

---

## 16. Lab 15: Release Workflow

Build/sketch:

- tag/release trigger
- build immutable artifact
- scan/sign/attest
- GitHub Release notes
- promote same artifact to environments
- rollback to previous artifact

Outcome:

```text
Explain release traceability from tag to artifact to deployment.
```

---

## 17. Lab 16: Runner Fleet Design

Design runner strategy for:

- public PR checks
- private repo CI
- production deploys
- jobs needing private network
- heavy Docker builds

Must include:

- GitHub-hosted vs self-hosted
- runner groups
- labels
- ephemeral runners
- queue metrics

Outcome:

```text
Explain runner trust and capacity planning.
```

---

## 18. Lab 17: Actions Platform Dashboard

Design dashboard panels:

- p50/p95 workflow duration
- queue time by runner label
- failure and flake rate
- cost by repo/team
- runner utilization
- cache hit rate
- artifact storage
- deployment success rate
- top failing required checks

Outcome:

```text
Explain CI/CD SLIs and SLOs.
```

---

## 19. Lab 18: Workflow Security Review

Review a workflow for:

- trigger trust
- `pull_request_target`
- job permissions
- secrets availability
- third-party action pinning
- cache/artifact trust
- self-hosted runner use
- OIDC trust policy
- shell injection risk

Outcome:

```text
Produce a security review checklist and remediation plan.
```

---

## 20. Lab 19: Reusable Workflow Migration

Plan migration for 100 repos:

- workflow inventory
- target golden paths
- pilot repos
- automated PRs
- deprecation schedule
- adoption dashboard
- exception process

Outcome:

```text
Explain how to standardize without blocking teams.
```

---

## 21. Lab 20: MAANG CI/CD Capstone

Design complete GitHub Actions platform for:

```text
backend services, frontend previews, Docker images, Kubernetes deploys, Terraform, DB migrations, OIDC, self-hosted runners, security scans, releases, rollback, and dashboards.
```

Deliverable:

- architecture diagram in text
- workflow list
- trust boundaries
- runner strategy
- reusable workflows
- deployment/release model
- observability/cost model
- incident runbooks

---

## 22. Completion Gate

You completed the labs when you can:

1. Write safe PR CI YAML from memory.
2. Design secure OIDC deployment workflows.
3. Explain cache/artifact/trust boundaries.
4. Build monorepo dynamic matrix workflow design.
5. Design runner isolation and capacity model.
6. Create Terraform/DB migration release gates.
7. Review workflow security risks.
8. Design an Actions platform operating model.
