# GitHub Actions Interview Scoring Rubrics

> Track: GitHub Actions Interview Track - Practice Upgrade  
> Goal: make GitHub Actions readiness measurable.

Use this after every scenario, lab, or mock.

---

## 1. Score Scale

| Score | Meaning |
|---|---|
| 1 | fragmented YAML recall, unsafe or incomplete production thinking |
| 2 | basic workflow knowledge but weak security/deployment boundaries |
| 3 | solid CI design with some senior gaps |
| 4 | strong senior answer with trust, permissions, rollback, and operations |
| 5 | MAANG-level platform answer with architecture, security, scale, cost, and governance |

Target:

- mid-level: mostly 3s
- senior DevOps/backend/platform: mostly 4s
- MAANG/platform/staff: consistent 4s with multiple 5s

---

## 2. Universal Rubric

| Dimension | 1 | 3 | 5 |
|---|---|---|---|
| Workflow model | names YAML pieces only | explains trigger/jobs/steps/runner | designs full event-to-deploy data/control flow |
| CI quality | basic test job | lint/test/build/cache/report | fast, reliable, layered CI with flake and branch protection strategy |
| Security | ignores trust | mentions secrets/permissions | models event/code/token/secret/runner/artifact trust boundaries |
| Delivery | deploy command only | environment approval and rollback | artifact promotion, OIDC, concurrency, health checks, rollback/runbook |
| Scale | single repo thinking | reusable workflows/caching | monorepo, runners, cost, SLOs, golden paths, migration plan |
| Communication | scattered | mostly structured | concise architecture with trade-offs and failure modes |

---

## 3. Foundations Rubric

5-point answer includes:

- workflow/job/step/action/runner
- events
- contexts and expressions
- `needs`
- matrix
- outputs
- environment variables
- concurrency
- cache vs artifact
- permissions basics

Deductions:

| Issue | Deduct |
|---|---|
| cannot explain job vs step | -1 |
| no runner concept | -1 |
| confuses cache and artifact | -2 |
| no `needs`/dependency model | -1 |
| ignores permissions | -1 |

---

## 4. Backend Frontend CI Rubric

5-point answer includes:

- PR trigger
- least-privilege permissions
- dependency setup/cache
- lint/typecheck/test/build
- service containers where needed
- test reports with `if: always()`
- frontend previews/cleanup where relevant
- required checks and flake strategy

Deductions:

| Issue | Deduct |
|---|---|
| no reports/artifacts | -1 |
| no cache strategy | -1 |
| slow everything-on-every-PR design | -1 |
| secrets in ordinary PR CI | -2 |
| no branch protection connection | -1 |

---

## 5. Caching Artifacts Performance Rubric

5-point answer includes:

- good cache keys
- lockfile/toolchain/OS awareness
- cache poisoning awareness
- artifact naming/retention
- path filters
- affected builds
- matrix tuning
- concurrency cancellation
- cost and duration metrics

Deductions:

| Issue | Deduct |
|---|---|
| deploys from cache | -3 |
| cache key too broad | -1 |
| no artifact retention thinking | -1 |
| no stale-run cancellation | -1 |
| no monorepo affected logic | -1 |

---

## 6. Reusable Workflow Rubric

5-point answer includes:

- `workflow_call`
- inputs/secrets/outputs
- composite vs reusable distinction
- custom action types
- versioning/pinning
- contract and changelog
- rollout/migration plan
- secure defaults

Deductions:

| Issue | Deduct |
|---|---|
| treats reusable workflow as just copy-paste | -1 |
| no versioning | -2 |
| secrets passed implicitly without control | -1 |
| no breaking-change plan | -1 |
| wrong composite vs reusable choice | -1 |

---

## 7. Docker Registry Rubric

5-point answer includes:

- Buildx
- registry auth/permissions
- immutable tags/digests
- image labels with SHA/source
- scan/SBOM/provenance
- artifact/image promotion
- rollback by digest
- avoids `latest` for deploy

Deductions:

| Issue | Deduct |
|---|---|
| deploys mutable `latest` | -3 |
| no scan/security gate | -1 |
| no traceability to commit | -1 |
| rebuilds differently per environment | -2 |
| no rollback image | -1 |

---

## 8. Deployment OIDC Environment Rubric

5-point answer includes:

- trusted trigger
- protected branch/tag
- environment reviewers/secrets
- OIDC with narrow cloud trust
- job-level `id-token: write`
- deploy concurrency
- health checks
- rollback or roll-forward path
- deployment summary/audit

Deductions:

| Issue | Deduct |
|---|---|
| static long-lived cloud keys by default | -2 |
| broad OIDC trust policy | -2 |
| no environment approval for prod | -1 |
| deploy races possible | -1 |
| no rollback path | -2 |

---

## 9. Security Threat Model Rubric

5-point answer includes:

- event trust
- code trust
- token permissions
- secrets availability
- runner trust
- cache/artifact trust
- `pull_request_target` risk
- self-hosted runner isolation
- third-party action pinning
- command injection prevention

Deductions:

| Issue | Deduct |
|---|---|
| runs fork PR code with secrets | -4 |
| unsafe `pull_request_target` pattern | -3 |
| broad workflow write permissions | -2 |
| untrusted code on internal runner | -3 |
| no OIDC trust boundary | -1 |
| deploys untrusted artifact | -3 |

---

## 10. Runner Platform Rubric

5-point answer includes:

- hosted vs self-hosted choice
- runner groups
- labels by capability/trust
- ephemeral runner preference for sensitive workloads
- ARC/autoscaling where useful
- private networking boundary
- queue/utilization metrics
- compromised runner response

Deductions:

| Issue | Deduct |
|---|---|
| self-hosted for all workloads without isolation | -2 |
| fork PRs on internal runners | -3 |
| no runner groups/labels | -1 |
| no capacity metrics | -1 |
| no compromise response | -2 |

---

## 11. IaC Database Rubric

5-point answer includes:

- PR plan and protected apply split
- reviewed plan artifact
- policy checks
- OIDC/cloud auth
- environment approval
- state protection
- drift detection
- expand/contract DB migrations
- rollback/forward-fix thinking

Deductions:

| Issue | Deduct |
|---|---|
| applies Terraform from PR | -3 |
| apply does not use reviewed plan | -2 |
| no state protection | -1 |
| destructive DB migration without compatibility plan | -3 |
| no approval gate | -1 |

---

## 12. Platform Operating Model Rubric

5-point answer includes:

- workflow inventory and ownership
- SLOs/SLIs
- dashboards
- cost by repo/team
- runner capacity plan
- golden paths
- policy guardrails
- support model
- migration strategy
- incident response

Deductions:

| Issue | Deduct |
|---|---|
| no ownership model | -1 |
| only pass/fail metrics | -1 |
| no cost visibility | -1 |
| no migration path before enforcement | -2 |
| no reusable/golden path strategy | -1 |

---

## 13. Workflow Architecture Rubric

5-point answer includes:

- fan-out/fan-in
- `needs` dependency clarity
- dynamic matrix when useful
- artifact contracts
- reusable workflow API contract
- PR/deploy concurrency difference
- monorepo orchestration
- trusted artifact promotion
- summary/observability job

Deductions:

| Issue | Deduct |
|---|---|
| one giant workflow with no boundaries | -1 |
| no artifact contract | -1 |
| deploys from untrusted workflow chain | -3 |
| no concurrency | -1 |
| matrix explosion | -1 |

---

## 14. Capstone Rubric

A 5-point MAANG answer includes:

1. Requirements: repo count, services, deploy targets, trust, compliance, scale.
2. Workflow architecture: triggers, jobs, reusable workflows, matrices.
3. CI: backend/frontend quality gates and reports.
4. Security: OIDC, token permissions, fork PR safety, action pinning.
5. Delivery: artifact/image promotion, environments, approvals, rollback.
6. Runners: hosted/self-hosted, groups, labels, ephemeral, capacity.
7. IaC/DB: plan/apply, migration safety, drift.
8. Observability: duration, queue, flake, cost, deployment health.
9. Governance: golden paths, ownership, policies, support.
10. Incident response: runner compromise, bad deploy, reusable workflow rollback.

Red flags:

- production deploy from PR workflow
- no permission model
- no runner trust model
- no rollback
- no cost/observability for platform-scale answer

---

## 15. Readiness Matrix

| Area | Target Score |
|---|---|
| Foundations | 5 |
| Backend/frontend CI | 4-5 |
| Caching/performance | 4 |
| Reusable workflows/actions | 4 |
| Docker/release | 4 |
| Deployments/OIDC | 4-5 |
| Security threat model | 5 |
| Runners/platform | 4-5 |
| IaC/database | 4 |
| Workflow architecture | 4-5 |
| Capstone | 4-5 |

Final readiness rule:

```text
You are not senior-ready in GitHub Actions until you can explain who triggered the workflow, what code ran, what credentials it had, where it ran, what it produced, and how deployment is approved and reversed.
```
