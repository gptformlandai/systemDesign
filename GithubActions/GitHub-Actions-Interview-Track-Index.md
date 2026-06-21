# GitHub Actions Interview Track Index

> Goal: build a one-stop GitHub Actions learning track for DevOps, backend, frontend, platform, and MAANG-level CI/CD interviews.

---

## How To Use This Track

Do not learn GitHub Actions as random YAML snippets.

Learn it as one production delivery system:

```text
trigger
-> workflow
-> jobs
-> runners
-> steps/actions
-> tests/builds/scans
-> artifacts/images
-> approvals/environments
-> deploy
-> observe/rollback
```

For interviews, every strong answer should cover:

1. What event starts the workflow.
2. What jobs run and in what order.
3. What is cached or published.
4. What secrets/permissions are used.
5. How deployment is approved and rolled back.
6. How failures are detected and debugged.

---

## Study Order

| Order | Document | Why It Exists |
|---|---|---|
| 1 | [Foundations and Workflow Syntax](GitHub-Actions-Foundations-Workflow-Syntax-Gold-Sheet.md) | Workflow, job, step, action, event, runner, contexts, expressions, matrix, outputs |
| 2 | [Backend CI and Testing](GitHub-Actions-Backend-CI-Testing-Gold-Sheet.md) | Java/Spring Boot, Node, Python, Go, .NET, service containers, test reports, coverage |
| 3 | [Frontend CI and Preview Deployments](GitHub-Actions-Frontend-CI-Preview-Deployments-Gold-Sheet.md) | React, Angular, Vue, Next.js, Vite, Playwright/Cypress, Storybook, preview URLs, CDN |
| 4 | [Caching, Artifacts, Monorepos, and Performance](GitHub-Actions-Caching-Artifacts-Monorepo-Performance-Gold-Sheet.md) | cache keys, artifacts, matrix tuning, Nx/Turborepo, path filters, workflow speed |
| 5 | [Reusable Workflows and Custom Actions](GitHub-Actions-Reusable-Workflows-Custom-Actions-Gold-Sheet.md) | `workflow_call`, composite actions, JavaScript/Docker actions, versioning, org standardization |
| 6 | [Workflow Architecture Patterns and Anti-Patterns](GitHub-Actions-Workflow-Architecture-Patterns-Anti-Patterns-Gold-Sheet.md) | fan-out/fan-in, dynamic matrix, artifact contracts, reusable workflow contracts, deployment concurrency |
| 7 | [Docker, Containers, and Registry Pipelines](GitHub-Actions-Docker-Containers-Registry-Gold-Sheet.md) | Docker build, tags, GHCR/ECR/ACR/GCR, scanning, SBOM, image promotion |
| 8 | [Deployments, Environments, Kubernetes, and Cloud](GitHub-Actions-Deployments-Environments-Kubernetes-Cloud-Gold-Sheet.md) | environments, approvals, blue-green/canary/rolling, Kubernetes, Helm, rollback |
| 9 | [Security, OIDC, Secrets, and Supply Chain](GitHub-Actions-Security-OIDC-Secrets-Supply-Chain-Gold-Sheet.md) | `GITHUB_TOKEN`, permissions, OIDC, fork PR security, action pinning, CodeQL, attestations |
| 10 | [Advanced Security Threat Model and Untrusted Code](GitHub-Actions-Advanced-Security-Threat-Model-Untrusted-Code-Gold-Sheet.md) | `pull_request_target`, fork PRs, cache/artifact poisoning, command injection, runner trust, third-party actions |
| 11 | [Runners, Networking, Governance, and Scale](GitHub-Actions-Runners-Networking-Governance-Gold-Sheet.md) | hosted/self-hosted runners, ARC, runner groups, private networking, quotas, org controls |
| 12 | [Platform Observability, Cost, and Operating Model](GitHub-Actions-Platform-Observability-Cost-Operating-Model-Gold-Sheet.md) | CI/CD platform ownership, SLIs/SLOs, dashboards, cost controls, golden paths, support model |
| 13 | [Infrastructure as Code and Database Migration Pipelines](GitHub-Actions-IaC-Database-Migration-Gold-Sheet.md) | Terraform plan/apply, drift, policy, DB migrations, approvals, rollback thinking |
| 14 | [Release Engineering and Progressive Delivery](GitHub-Actions-Release-Engineering-Progressive-Delivery-Gold-Sheet.md) | SemVer, tags, changelogs, releases, hotfix, rollback, feature flags, canary |
| 15 | [Production Operations and Scenario Bank](GitHub-Actions-Production-Operations-Scenario-Bank-Gold-Sheet.md) | failed workflow triage, slow CI, compromised runner, OIDC failure, prod rollback, MAANG scenarios |
| 16 | [Interview Stress Concepts and Production Scenarios](GitHub-Actions-Interview-Stress-Concepts-Production-Scenarios-Gold-Sheet.md) | high-pressure interview concepts: `GITHUB_TOKEN`, OIDC, fork PRs, runner trust, rollback, frontend/CDN traps, prod incidents |
| 17 | [Golden Workflow Templates Library](GitHub-Actions-Golden-Workflow-Templates-Library.md) | ready-to-revise YAML patterns for backend, frontend, Docker, Terraform, Kubernetes, security |

---

## Practice Upgrade Layer

After reading each concept sheet, use this layer to convert knowledge into recall, design fluency, and interview performance.

| Practice File | How To Use It |
|---|---|
| [Active Recall Question Bank](05-Practice-Upgrade/GitHub-Actions-Active-Recall-Question-Bank.md) | drill definitions, trade-offs, failure modes, and security boundaries until answers become automatic |
| [Scenario Drill Bank](05-Practice-Upgrade/GitHub-Actions-Scenario-Drill-Bank.md) | practice production-style prompts on slow CI, fork PRs, OIDC failures, runners, releases, and platform governance |
| [Hands-On Labs](05-Practice-Upgrade/GitHub-Actions-Hands-On-Labs.md) | implement workflow patterns: PR CI, service containers, dynamic matrices, OIDC deploys, Terraform, DB migrations, dashboards |
| [Mock Interview Scripts](05-Practice-Upgrade/GitHub-Actions-Mock-Interview-Scripts.md) | rehearse structured interview answers from foundations through MAANG capstone design |
| [Interview Scoring Rubrics](05-Practice-Upgrade/GitHub-Actions-Interview-Scoring-Rubrics.md) | score readiness across syntax, CI, security, deployments, runners, IaC, platform ops, and capstone communication |
| [2 Week 4 Week Mastery Roadmaps](05-Practice-Upgrade/GitHub-Actions-2-Week-4-Week-Mastery-Roadmaps.md) | follow a focused acceleration plan or deeper mastery plan |

Recommended loop:

```text
read -> sketch workflow -> answer recall -> solve scenario -> run lab -> mock -> score -> revise
```

---

## Learning Levels

### Beginner

You should be able to explain:

- workflow vs job vs step
- action vs shell command
- runner
- workflow trigger
- `needs`
- artifacts vs cache
- secrets vs variables
- basic CI pipeline

### Intermediate

You should be able to design:

- backend CI with tests and service containers
- frontend CI with lint/typecheck/test/build
- Docker build and push pipeline
- environment-based deployment with approval
- cache strategy
- matrix builds
- reusable workflow
- composite/custom actions
- basic security hardening

### Senior / MAANG Level

You should be able to lead discussions on:

- CI/CD for hundreds of services
- monorepo affected builds
- OIDC cloud authentication without static credentials
- self-hosted runner isolation
- supply-chain security and action pinning
- untrusted code threat modeling for fork PRs and `pull_request_target`
- cache poisoning, artifact poisoning, and command injection prevention
- workflow architecture patterns: fan-out/fan-in, dynamic matrix, artifact contracts, deployment concurrency
- deployment rollback strategy
- preview environments at scale
- Terraform safe apply workflows
- production incident response
- governance across an engineering organization
- reusable workflow and custom action versioning
- GitHub Actions platform SLOs, dashboards, cost controls, golden paths, and migration strategy

---

## Master Map

```text
Core workflow side:
  events
  jobs
  steps
  actions
  runners
  contexts
  expressions
  outputs
  matrix
  concurrency
  reusable workflows
  custom actions

Workflow architecture side:
  fan-out / fan-in
  dynamic matrix
  artifact contracts
  reusable workflow contracts
  workflow chaining
  concurrency and cancellation
  deployment serialization
  summary/reporting jobs

Backend CI side:
  Java / Spring Boot
  Node / Python / Go / .NET
  unit tests
  integration tests
  service containers
  coverage
  test reports

Frontend CI side:
  React / Angular / Vue / Next.js / Vite
  npm / yarn / pnpm
  lint / typecheck / test / build
  Playwright / Cypress
  Storybook / visual regression
  Lighthouse / accessibility
  preview deployments

Delivery side:
  Docker
  registries
  Kubernetes
  Helm / Kustomize
  cloud deployment
  environments
  approvals
  rollback

Security side:
  secrets
  variables
  GITHUB_TOKEN
  permissions
  OIDC
  fork PR safety
  pull_request_target risk
  untrusted code
  cache poisoning
  artifact poisoning
  command injection
  CodeQL
  dependency review
  SBOM
  artifact attestations

Platform side:
  reusable workflows
  composite actions
  JavaScript / Docker actions
  self-hosted runners
  ARC
  private networking
  org templates
  quotas
  governance
  platform SLOs
  dashboards
  cost controls
  golden paths
  migration strategy
  support model
```

---

## Interview Rule

Never present a workflow as production-ready unless you can answer:

- What permissions does it need?
- How are secrets protected?
- What happens on fork PRs?
- What is cached and why?
- What artifact/image is produced?
- Who approves production?
- How do we rollback?
- How do we know it failed?

---

## MAANG Completion Definition

This track is complete only when you can:

1. Explain GitHub Actions syntax from trigger to job graph to runner execution.
2. Design backend and frontend PR CI with reports, artifacts, caches, and required checks.
3. Tune monorepo workflows with path filters, affected builds, dynamic matrices, and concurrency.
4. Build reusable workflow and custom action contracts with versioning and migration plans.
5. Build Docker image pipelines with immutable tags, scans, SBOM/provenance, and rollback by digest.
6. Deploy safely with environments, approvals, OIDC, cloud/Kubernetes auth, health checks, and rollback.
7. Threat model events, untrusted code, tokens, secrets, runners, caches, artifacts, and third-party actions.
8. Explain safe and unsafe `pull_request_target` patterns.
9. Design hosted/self-hosted runner strategy with isolation, labels, groups, autoscaling, and private networking.
10. Design Terraform and database migration workflows with reviewed plans, policy checks, compatibility, and recovery.
11. Operate GitHub Actions as a platform with SLOs, dashboards, cost controls, golden paths, support, and governance.
12. Speak a capstone answer that covers requirements, trust boundaries, workflow architecture, delivery, rollback, and operations.

---

## Official Source Notes

- GitHub Actions documentation: <https://docs.github.com/en/actions>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- Reusing workflows: <https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows>
- OpenID Connect: <https://docs.github.com/en/actions/concepts/security/openid-connect>
- Security hardening: <https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions>
- GitHub-hosted runners: <https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners>
- Self-hosted runners: <https://docs.github.com/en/actions/hosting-your-own-runners>
