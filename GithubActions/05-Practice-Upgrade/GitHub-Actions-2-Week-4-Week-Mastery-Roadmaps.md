# GitHub Actions 2 Week 4 Week Mastery Roadmaps

> Track: GitHub Actions Interview Track - Practice Upgrade  
> Goal: convert the GitHub Actions notes into a focused MAANG-level study plan.

Use the 2-week roadmap for interview acceleration. Use the 4-week roadmap for deeper mastery.

---

## 1. Daily Study Loop

Use this loop every day:

1. Read one focused sheet.
2. Sketch one workflow or runbook.
3. Answer 15 active recall questions.
4. Solve 1 scenario or lab.
5. Speak a 3-minute answer aloud.
6. Score with the rubric.

Daily answer mantra:

```text
trigger, jobs, runner, permissions, secrets, artifacts, deploy, rollback, observability
```

---

## 2. 2-Week Interview Acceleration Plan

Target: backend, DevOps, or platform engineer who needs strong GitHub Actions interview readiness quickly.

### Day 1: Foundations Workflow Syntax

Read:

- `GitHub-Actions-Foundations-Workflow-Syntax-Gold-Sheet.md`

Practice:

- Lab 1: minimal PR CI
- Mock 1 foundations
- 25 foundations recall questions

Outcome:

```text
Explain workflow, event, job, step, action, runner, contexts, matrix, outputs, and concurrency.
```

---

### Day 2: Backend CI

Read:

- `GitHub-Actions-Backend-CI-Testing-Gold-Sheet.md`

Practice:

- Lab 2: backend CI with service container
- backend CI scenario drills

Outcome:

```text
Design backend PR CI with test reports, caches, service containers, artifacts, and required checks.
```

---

### Day 3: Frontend CI Preview Deployments

Read:

- `GitHub-Actions-Frontend-CI-Preview-Deployments-Gold-Sheet.md`

Practice:

- Lab 3: frontend CI and preview plan
- preview cost/cleanup scenario

Outcome:

```text
Design frontend CI, browser tests, preview deployments, CDN rollback, and cleanup.
```

---

### Day 4: Caching Artifacts Monorepo Performance

Read:

- `GitHub-Actions-Caching-Artifacts-Monorepo-Performance-Gold-Sheet.md`

Practice:

- Lab 4: cache key design
- Lab 6: dynamic matrix for monorepo
- Mock 3 monorepo performance

Outcome:

```text
Explain cache vs artifact, affected builds, matrix tuning, concurrency, and cost reduction.
```

---

### Day 5: Reusable Workflows Custom Actions

Read:

- `GitHub-Actions-Reusable-Workflows-Custom-Actions-Gold-Sheet.md`

Practice:

- Lab 7: reusable workflow contract
- Lab 8: composite vs reusable workflow
- Mock 4 standardization

Outcome:

```text
Choose reusable workflows, composite actions, custom actions, and templates correctly.
```

---

### Day 6: Docker Registry Pipelines

Read:

- `GitHub-Actions-Docker-Containers-Registry-Gold-Sheet.md`

Practice:

- Lab 9: Docker build scan push
- image tag collision scenario

Outcome:

```text
Build, scan, tag, push, trace, and roll back container images safely.
```

---

### Day 7: Week 1 Review

Practice:

- 60 active recall questions
- 5 scenario drills
- Mock 2 and Mock 5

Pass bar:

- foundations: 5
- CI design: 4+
- cache/artifact: 4+
- reusable workflow: 4+

---

### Day 8: Deployments Environments OIDC

Read:

- `GitHub-Actions-Deployments-Environments-Kubernetes-Cloud-Gold-Sheet.md`
- OIDC portions of security sheet

Practice:

- Lab 10: OIDC cloud deploy
- Lab 12: environment gate deploy
- Mock 6 deployments

Outcome:

```text
Design production deploy workflows with OIDC, environments, approvals, concurrency, and rollback.
```

---

### Day 9: Security Threat Model

Read:

- `GitHub-Actions-Security-OIDC-Secrets-Supply-Chain-Gold-Sheet.md`
- `GitHub-Actions-Advanced-Security-Threat-Model-Untrusted-Code-Gold-Sheet.md`

Practice:

- Lab 11: secure fork PR workflow
- Lab 18: workflow security review
- Mock 7 threat model

Outcome:

```text
Explain event/code/token/secret/runner/artifact trust boundaries and secure workflow patterns.
```

---

### Day 10: Runners Networking Governance

Read:

- `GitHub-Actions-Runners-Networking-Governance-Gold-Sheet.md`

Practice:

- Lab 16: runner fleet design
- runner queue and compromised runner scenarios

Outcome:

```text
Design hosted/self-hosted runner strategy, groups, labels, ARC, isolation, and capacity.
```

---

### Day 11: IaC Database Migrations

Read:

- `GitHub-Actions-IaC-Database-Migration-Gold-Sheet.md`

Practice:

- Lab 13: Terraform plan/apply
- Lab 14: database migration gate
- Mock 9 IaC/database

Outcome:

```text
Design Terraform and DB migration workflows with review gates, drift, approvals, and rollback thinking.
```

---

### Day 12: Release Platform Architecture

Read:

- `GitHub-Actions-Release-Engineering-Progressive-Delivery-Gold-Sheet.md`
- `GitHub-Actions-Workflow-Architecture-Patterns-Anti-Patterns-Gold-Sheet.md`

Practice:

- Lab 5: fan-out/fan-in workflow
- Lab 15: release workflow
- architecture scenarios

Outcome:

```text
Design release pipelines, artifact promotion, workflow architecture, dynamic matrices, and deployment serialization.
```

---

### Day 13: Platform Operating Model

Read:

- `GitHub-Actions-Platform-Observability-Cost-Operating-Model-Gold-Sheet.md`
- production operations and interview stress sheets

Practice:

- Lab 17: Actions platform dashboard
- Lab 19: reusable workflow migration
- Mock 8 runners/platform

Outcome:

```text
Operate Actions as a platform with metrics, SLOs, cost, support, golden paths, and incident response.
```

---

### Day 14: Final Mock Day

Practice:

- Mock 10 MAANG capstone
- 80 active recall questions
- 5 scenario drills
- score each area with rubric

Pass bar:

- foundations: 5
- CI design: 4+
- security threat model: 5
- deployment/OIDC: 4+
- runners/platform: 4+
- capstone: 4+

---

## 3. 4-Week Mastery Plan

Target: deep senior/platform readiness.

---

## Week 1: CI Foundations And Application Pipelines

Focus:

- workflow syntax
- backend CI
- frontend CI
- service containers
- browser tests
- reports and artifacts
- required checks

Files:

- `GitHub-Actions-Foundations-Workflow-Syntax-Gold-Sheet.md`
- `GitHub-Actions-Backend-CI-Testing-Gold-Sheet.md`
- `GitHub-Actions-Frontend-CI-Preview-Deployments-Gold-Sheet.md`

Practice:

- Labs 1, 2, 3
- Mocks 1, 2
- 100 active recall questions

Week gate:

```text
You can design fast, reliable PR CI for backend and frontend services.
```

---

## Week 2: Performance Reuse Containers Releases

Focus:

- caching
- artifacts
- monorepos
- dynamic matrix
- reusable workflows
- custom actions
- Docker build/scan/push
- release workflow

Files:

- `GitHub-Actions-Caching-Artifacts-Monorepo-Performance-Gold-Sheet.md`
- `GitHub-Actions-Reusable-Workflows-Custom-Actions-Gold-Sheet.md`
- `GitHub-Actions-Docker-Containers-Registry-Gold-Sheet.md`
- `GitHub-Actions-Release-Engineering-Progressive-Delivery-Gold-Sheet.md`

Practice:

- Labs 4, 5, 6, 7, 8, 9, 15
- Mocks 3, 4, 5

Week gate:

```text
You can design scalable workflows with reusable contracts, artifacts, images, and release promotion.
```

---

## Week 3: Security Deployments Runners IaC

Focus:

- OIDC
- secrets
- fork PR safety
- threat model
- environments
- Kubernetes/cloud deploys
- runners
- Terraform/DB migrations

Files:

- `GitHub-Actions-Security-OIDC-Secrets-Supply-Chain-Gold-Sheet.md`
- `GitHub-Actions-Advanced-Security-Threat-Model-Untrusted-Code-Gold-Sheet.md`
- `GitHub-Actions-Deployments-Environments-Kubernetes-Cloud-Gold-Sheet.md`
- `GitHub-Actions-Runners-Networking-Governance-Gold-Sheet.md`
- `GitHub-Actions-IaC-Database-Migration-Gold-Sheet.md`

Practice:

- Labs 10, 11, 12, 13, 14, 16, 18
- Mocks 6, 7, 8, 9

Week gate:

```text
You can secure and deploy production workflows without crossing trust boundaries.
```

---

## Week 4: Platform Operations And Capstone

Focus:

- workflow architecture
- platform observability
- cost
- SLOs
- golden paths
- migration strategy
- incident response
- MAANG capstone

Files:

- `GitHub-Actions-Workflow-Architecture-Patterns-Anti-Patterns-Gold-Sheet.md`
- `GitHub-Actions-Platform-Observability-Cost-Operating-Model-Gold-Sheet.md`
- `GitHub-Actions-Production-Operations-Scenario-Bank-Gold-Sheet.md`
- `GitHub-Actions-Interview-Stress-Concepts-Production-Scenarios-Gold-Sheet.md`
- all `05-Practice-Upgrade` files

Practice:

- Labs 17, 19, 20
- Mock 10 twice
- 10 scenario drills
- final rubric review

Week gate:

```text
You can design and operate GitHub Actions as a secure, reliable, cost-aware CI/CD platform.
```

---

## 4. Topic Weighting For Senior Interviews

| Area | Weight |
|---|---:|
| Foundations syntax | 10% |
| Backend/frontend CI | 15% |
| Caching/artifacts/performance | 10% |
| Reusable workflows/custom actions | 10% |
| Docker/release/deployments | 15% |
| Security/OIDC/threat model | 20% |
| Runners/IaC/platform operations | 15% |
| Capstone communication | 5% |

For platform/staff roles, platform operations, governance, and security become heavier.

---

## 5. Final Readiness Checklist

You are ready when you can:

1. Explain Actions syntax and data flow across jobs.
2. Design backend/frontend PR CI with reports and required checks.
3. Design cache and artifact strategy without trust mistakes.
4. Build reusable workflow contracts and migration plans.
5. Build Docker image pipelines with scanning, provenance, and rollback.
6. Deploy with environments, OIDC, approvals, concurrency, and rollback.
7. Threat model fork PRs, `pull_request_target`, self-hosted runners, cache, artifacts, and third-party actions.
8. Design runner isolation and capacity model.
9. Design Terraform and DB migration pipelines safely.
10. Operate Actions as a platform with SLOs, dashboards, cost, support, and golden paths.

---

## 6. Final Message To Remember

```text
GitHub Actions mastery is not writing YAML from memory. It is designing trustworthy automation:
who triggered it, what code ran, what credentials it had, where it ran, what it produced, and how production recovers.
```
