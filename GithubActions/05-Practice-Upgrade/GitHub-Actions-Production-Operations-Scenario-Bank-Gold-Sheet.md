# GitHub Actions Production Operations and Scenario Bank Gold Sheet

> Goal: answer production CI/CD incident and MAANG-level GitHub Actions questions with calm, structured, senior-level reasoning.

---

## 0. How To Use This

Use this sheet for fast revision.

Every scenario answer should cover:

1. Scope the issue.
2. Identify the failing layer.
3. Protect secrets and production.
4. Restore delivery.
5. Prevent recurrence.

---

# Topic 1: Production Operations and Scenario Bank

---

## 1. Intuition

GitHub Actions production issues are usually one of these:

```text
trigger problem
YAML/expression problem
runner problem
dependency/cache problem
test/flaky problem
permission/secret problem
cloud auth problem
deployment problem
security/supply-chain problem
```

Senior interview sentence:

> I debug CI/CD by isolating whether the failure is trigger, workflow graph, runner, dependency, permission, artifact, deployment target, or external system.

---

## 2. Definition

- Definition: GitHub Actions production operations is the practice of monitoring, debugging, securing, and recovering CI/CD workflows and deployments.
- Category: CI/CD operations and incident response
- Core idea: treat CI/CD as production infrastructure.

---

## 3. Why It Exists

CI/CD failures can block:

- merges
- hotfixes
- releases
- deployments
- security patches
- infrastructure changes

A broken pipeline can become a production incident even if the app is healthy.

---

## 4. Golden Debugging Order

1. Which workflow failed?
2. Which event triggered it?
3. Which job/step failed?
4. Did it fail on all branches or one PR?
5. Did a workflow/code/secret/runner change happen?
6. Is the runner available?
7. Is the dependency registry/cloud/deployment target healthy?
8. Are permissions/secrets correct?
9. Are artifacts/images produced?
10. Is rollback/manual path available?

---

## 5. Scenario Bank

### Scenario 1: Workflow Did Not Trigger

Check:

- event config
- branch filter
- path filter
- workflow file location
- workflow enabled
- PR source branch/fork behavior

Strong answer:

I would inspect the `on` trigger, branch/path filters, and whether the workflow exists on the correct branch. If path filters are used, I would confirm the changed files match. I would also check whether the workflow is disabled or blocked by repository settings.

Trap:

Assuming GitHub Actions is down before checking filters.

---

### Scenario 2: Required Check Is Stuck

Causes:

- workflow skipped by path filter
- required check name changed
- job condition skipped
- merge queue mismatch

Strong answer:

I would compare branch protection required checks with actual job names. If path filters skip jobs, I would use an aggregator required check or ensure skipped workflows report a neutral/successful status where appropriate.

---

### Scenario 3: CI Takes 45 Minutes

Strong answer:

I would profile the workflow: queue time, dependency install, build, tests, Docker, and deployment. Then I would add dependency caching, split independent jobs, use affected-build detection, reduce matrix explosion, cancel stale PR runs, and move heavy tests to scheduled/release workflows.

Trap:

Deleting tests without measuring risk.

---

### Scenario 4: Tests Are Flaky

Strong answer:

I would identify whether flakes are timing, shared state, external dependency, or race condition. I would upload reports/logs, track flake rate, quarantine only with owner and expiry, and fix root cause. Rerunning until green should not be the long-term strategy.

---

### Scenario 5: OIDC Cloud Auth Fails

Check:

- `id-token: write`
- cloud trust policy
- repo/ref/environment claims
- audience
- environment name

Strong answer:

OIDC failures are usually claim or permission mismatches. I would verify workflow permissions, inspect expected token claims, and compare them with cloud role trust policy.

---

### Scenario 6: Secret Missing In Workflow

Check:

- secret scope: repo/org/environment
- environment selected
- fork PR restrictions
- spelling
- required reviewers not approved yet

Strong answer:

I would first check whether the workflow is trusted to access the secret. Fork PRs should not receive sensitive secrets. For deployment secrets, the job must target the correct GitHub environment.

---

### Scenario 7: Production Deploy Failed Halfway

Strong answer:

I would stop further promotion, inspect deployment logs and platform rollout state, and decide rollback versus forward fix based on blast radius. If using Kubernetes/Helm, I would check rollout status, pod events, and health checks. Rollback should redeploy a previous known-good artifact or image digest.

Trap:

Re-running blindly while the platform is partially changed.

---

### Scenario 8: Docker Image Built But Deploy Uses Old Version

Check:

- image tag
- mutable `latest`
- Kubernetes image pull policy
- deployment manifest value
- registry push target
- digest mismatch

Strong answer:

I would deploy immutable SHA/digest tags, not rely on `latest`. The deployment workflow should print and record the exact image digest it deploys.

---

### Scenario 9: Preview Deployment Leaked Secret

Response:

- rotate secret immediately
- inspect build logs/artifacts
- remove secret from frontend build env
- audit preview workflow
- separate public frontend config from backend secrets

Strong answer:

Frontend preview builds must never embed secrets into browser bundles. I would rotate the secret and change architecture so secrets remain server-side.

---

### Scenario 10: CDN Serves Old Frontend Assets

Check:

- cache headers
- content-hashed filenames
- `index.html` cache
- invalidation path
- service worker cache

Strong answer:

I would use content-hashed JS/CSS with long cache headers and short-cache or invalidate the HTML shell. Rollback should switch to previous artifact version.

---

### Scenario 11: Self-Hosted Runner Compromised

Response:

- isolate runner
- revoke runner token
- rotate secrets/cloud credentials
- inspect workflow runs
- rebuild runner image
- review runner group access
- block untrusted code on private runners

Strong answer:

I would treat it as a production security incident because self-hosted runners may access internal networks and secrets.

---

### Scenario 12: Fork PR Wants To Run Tests

Strong answer:

I would run untrusted validation with minimal permissions and no secrets. If privileged actions are needed, I would separate them into a trusted workflow triggered by maintainers, not execute fork code with secrets.

---

### Scenario 13: Terraform Apply Fails And Lock Remains

Strong answer:

I would verify no apply is still running before unlocking. Then I would inspect partial changes, rerun plan, and decide whether to complete or revert. Unlocking state blindly can cause corruption.

---

### Scenario 14: Monorepo Only Some Apps Should Build

Strong answer:

I would use affected-project detection rather than simple file paths when shared libraries exist. Changes to `packages/ui` may affect multiple frontend apps. I would keep periodic full CI to catch graph mistakes.

---

### Scenario 15: Supply-Chain Attack Concern

Strong answer:

I would reduce third-party action risk by pinning critical actions, using trusted internal reusable workflows, limiting `GITHUB_TOKEN` permissions, scanning dependencies/images, generating SBOM/provenance, and using artifact attestations for release artifacts.

---

### Scenario 16: Design CI/CD For 200 Microservices

Strong answer:

I would create platform-owned reusable workflows for standard CI, Docker build, security scan, and deployment. Each service would pass inputs such as language, service path, image name, and environment. Branch protection would require standard checks. Builds would use path filters or affected-service detection. Deployments would use environments, OIDC, approvals, and serialized production rollout. Runners would be grouped by trust level and network access.

---

### Scenario 17: Backend And Frontend Full-Stack Deploy

Strong answer:

I would build backend image and frontend artifact separately, run contract compatibility checks, deploy backend first only if backward compatible, then frontend. If frontend depends on new API behavior, I would use feature flags or compatibility windows. CDN invalidation and backend rollback must be coordinated.

---

### Scenario 18: Database Migration During Deployment

Strong answer:

I would use expand/contract migrations. Add compatible schema first, deploy app that handles both old/new, backfill, then remove old schema later. I would not drop columns in the same deployment that still has old app versions running.

---

### Scenario 19: GitHub Actions vs Jenkins

Strong answer:

GitHub Actions is excellent when code and automation live in GitHub, with strong PR integration, reusable workflows, marketplace actions, and OIDC. Jenkins may still fit highly customized legacy build farms or specialized enterprise setups. The decision depends on governance, runner model, plugins, migration cost, and security requirements.

---

### Scenario 20: Emergency Hotfix While CI Is Broken

Strong answer:

I would avoid bypassing all safety blindly. I would identify the broken check, run minimum critical validation manually or through an emergency workflow, get approval, deploy a small hotfix, and immediately repair CI afterward. The exception should be audited.

---

## 6. Production Checklist

Before calling a GitHub Actions system production-ready:

- workflows use explicit permissions
- production deploys use environments and approvals
- cloud auth uses OIDC where possible
- secrets are scoped correctly
- fork PRs cannot access secrets
- artifacts/images are immutable
- rollback workflow exists
- self-hosted runners are isolated
- cache/artifact paths avoid secrets
- required checks are stable
- slow CI has an optimization plan
- logs/reports are uploaded for failures

---

## 7. Thirty-Second Interview Template

```text
I would separate validation, artifact creation, and deployment.
PR workflows run with minimal permissions and no production secrets.
Builds produce immutable artifacts or images tagged by SHA.
Security scans and tests run before deployment.
Deployments use environments, approvals, OIDC, and concurrency.
Rollback redeploys a previous known-good artifact.
For scale, I use reusable workflows, runner groups, caching, and affected builds.
```

---

## 8. Revision Notes

- One-line summary: GitHub Actions operations require debugging triggers, runners, permissions, artifacts, deployments, and security boundaries.
- Three keywords: scope, isolate, recover
- One interview trap: rerunning a failed deployment can worsen partial state.
- One memory trick: failed pipeline means check trigger, runner, permission, artifact, target.

---

## 9. Official Source Notes

- Monitoring and troubleshooting workflows: <https://docs.github.com/en/actions/monitoring-and-troubleshooting-workflows>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- Security hardening: <https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions>

