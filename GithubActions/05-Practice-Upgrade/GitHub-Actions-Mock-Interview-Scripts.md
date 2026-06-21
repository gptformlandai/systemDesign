# GitHub Actions Mock Interview Scripts

> Track: GitHub Actions Interview Track - Practice Upgrade  
> Goal: rehearse GitHub Actions answers for senior DevOps, backend, platform, and MAANG interviews.

---

## 1. How To Run A Mock

Rules:

1. Timebox the round.
2. Start with requirements and trust boundaries.
3. Speak workflow shape before YAML details.
4. Always name permissions, secrets, runner choice, and rollback.
5. Score with the rubric immediately.

Default answer format:

```text
trigger -> jobs/needs -> runner -> permissions/secrets -> artifacts/cache -> deploy/rollback -> observability
```

---

## 2. Mock 1: Foundations Workflow Syntax

Time: 30 minutes.

### Opening

```text
Explain GitHub Actions from event trigger to workflow run.
```

Expected points:

- workflow file in `.github/workflows`
- event trigger
- jobs and steps
- actions vs shell commands
- runner
- contexts/expressions
- matrix
- outputs
- concurrency

### Follow-ups

1. What does `needs` do?
2. How do job outputs work?
3. What is the difference between cache and artifact?
4. What does `permissions` control?
5. When use `workflow_dispatch`?

---

## 3. Mock 2: Backend And Frontend CI

Time: 45 minutes.

### Opening

```text
Design PR CI for backend and frontend repositories.
```

Expected points:

- `pull_request` trigger
- least-privilege permissions
- language setup/cache
- lint/typecheck/test/build
- service containers for backend integration tests
- Playwright/Cypress for frontend smoke tests
- report artifacts with `if: always()`
- branch protection required checks

### Follow-ups

1. How do you handle flaky tests?
2. How do you keep PR feedback fast?
3. How do frontend preview deployments work?
4. What should be cached?
5. What should be uploaded as artifact?

---

## 4. Mock 3: Caching Monorepo Performance

Time: 45 minutes.

### Opening

```text
A monorepo with 100 services takes 90 minutes for every PR. Fix it.
```

Expected points:

- path filters
- affected service planner
- dynamic matrix
- reusable workflow per service
- cache keys by lockfile/toolchain
- cancel stale PR runs
- split PR/nightly/release checks
- queue and duration metrics

### Follow-ups

1. How do you prevent matrix explosion?
2. What is cache poisoning?
3. How do you measure CI cost?
4. What is `concurrency` for PRs?
5. How do you avoid building docs-only changes?

---

## 5. Mock 4: Reusable Workflows And Custom Actions

Time: 45 minutes.

### Opening

```text
How would you standardize CI/CD across 200 repositories?
```

Expected points:

- reusable workflows with `workflow_call`
- composite actions for shared steps
- templates for starter workflows
- secure defaults
- documented inputs/secrets/outputs
- versioning and changelog
- migration plan
- support and adoption metrics

### Follow-ups

1. Composite action vs reusable workflow?
2. How do secrets pass to reusable workflows?
3. What happens if a reusable workflow has a breaking change?
4. How do callers pin versions?
5. What should be centrally controlled vs team-owned?

---

## 6. Mock 5: Docker Release Pipeline

Time: 45 minutes.

### Opening

```text
Design a Docker image build, scan, publish, and promotion workflow.
```

Expected points:

- Buildx
- registry auth
- immutable tags/digests
- image labels with SHA/source
- vulnerability scan
- SBOM/provenance/attestation
- push to registry
- promote same image
- rollback by previous digest

### Follow-ups

1. Why not deploy `latest`?
2. How do you handle multi-arch builds?
3. What permissions are needed for package publish?
4. How do you trace image to commit?
5. What if image scan fails?

---

## 7. Mock 6: Deployments OIDC Environments

Time: 60 minutes.

### Opening

```text
Design a secure production deployment workflow to cloud/Kubernetes.
```

Expected points:

- trusted trigger from protected branch/tag
- environment approval
- environment secrets if needed
- OIDC with narrow cloud trust policy
- `id-token: write`
- deployment concurrency
- Kubernetes/Helm deploy
- health checks
- rollback path
- deployment summary

### Follow-ups

1. How does OIDC replace static cloud secrets?
2. How do environment reviewers help?
3. What if OIDC assumption fails?
4. How do you prevent deploy races?
5. How do database migrations change rollback?

---

## 8. Mock 7: Security Threat Model

Time: 60 minutes.

### Opening

```text
Threat model a GitHub Actions workflow for a public repository accepting fork PRs.
```

Expected points:

- event trust
- untrusted PR code
- no secrets in PR validation
- read-only token
- avoid self-hosted internal runners
- careful with `pull_request_target`
- third-party action pinning
- cache/artifact trust boundaries
- command injection from untrusted input

### Follow-ups

1. What is safe use of `pull_request_target`?
2. How can cache poisoning happen?
3. How can artifact poisoning happen?
4. Why pin actions to SHA?
5. What should a workflow security review include?

---

## 9. Mock 8: Runners Platform Operations

Time: 60 minutes.

### Opening

```text
Operate GitHub Actions for a company with heavy CI, private networking, and production deploys.
```

Expected points:

- hosted vs self-hosted runner split
- runner groups
- labels by capability/trust
- ephemeral runners
- ARC/autoscaling
- queue time metrics
- runner isolation
- production deploy runner separation
- patching/monitoring

### Follow-ups

1. Why avoid fork PRs on self-hosted runners?
2. How do you debug queued jobs?
3. What dashboard do you build?
4. How do you reduce cost?
5. What is your incident response for compromised runner?

---

## 10. Mock 9: IaC Database Release

Time: 60 minutes.

### Opening

```text
Design GitHub Actions for Terraform and database migrations.
```

Expected points:

- PR plan only
- reviewed plan artifact
- policy checks
- protected apply from trusted branch/environment
- OIDC cloud auth
- state backend protection
- DB expand/contract pattern
- migration approvals
- rollback/forward-fix plan

### Follow-ups

1. Why should PRs not apply infra?
2. How do you handle drift?
3. Why apply reviewed plan?
4. What if migration fails halfway?
5. How do DB migrations affect app rollback?

---

## 11. Mock 10: MAANG Capstone

Time: 75 minutes.

### Prompt

```text
Design a GitHub Actions platform for 500 repos: backend services, frontend previews, Docker images, Kubernetes deploys, Terraform, DB migrations, OIDC, self-hosted runners, security scans, releases, rollback, and cost controls.
```

Strong answer includes:

1. Workflow architecture and triggers.
2. Reusable workflow golden paths.
3. CI for backend/frontend.
4. Monorepo affected builds and dynamic matrix.
5. Docker build/scan/sign/promote.
6. Environments, approvals, OIDC, deployment concurrency.
7. IaC/DB migration gates.
8. Runner strategy by trust and capability.
9. Security threat model for fork PRs and workflow files.
10. Observability: duration, queue, flake, cost, deploy health.
11. Support model and migration plan.
12. Incident/rollback runbooks.

### Staff-Level Follow-ups

1. What do you standardize first?
2. What do teams still own?
3. How do you migrate safely?
4. How do you enforce policy without blocking delivery?
5. What metrics prove success after 90 days?

---

## 12. Self Review Questions

After each mock, ask:

1. Did I identify trigger and trust boundary?
2. Did I name permissions and secrets?
3. Did I choose runner type intentionally?
4. Did I distinguish cache and artifact?
5. Did I cover rollback or recovery?
6. Did I include observability?
7. Did I avoid unsafe `pull_request_target` or self-hosted runner claims?
8. Did I handle cost/platform scale where relevant?

---

## 13. Completion Gate

You are mock-ready when:

1. Foundations answer fits in 5 minutes.
2. CI design includes tests, reports, caching, and branch protection.
3. Security answer names event/code/token/secret/runner/artifact trust.
4. Deployment answer includes OIDC, environments, concurrency, and rollback.
5. Platform answer includes SLOs, metrics, cost, support, and governance.
6. Capstone answer is structured and trade-off aware.
