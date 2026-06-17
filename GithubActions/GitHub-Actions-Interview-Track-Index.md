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
| 6 | [Docker, Containers, and Registry Pipelines](GitHub-Actions-Docker-Containers-Registry-Gold-Sheet.md) | Docker build, tags, GHCR/ECR/ACR/GCR, scanning, SBOM, image promotion |
| 7 | [Deployments, Environments, Kubernetes, and Cloud](GitHub-Actions-Deployments-Environments-Kubernetes-Cloud-Gold-Sheet.md) | environments, approvals, blue-green/canary/rolling, Kubernetes, Helm, rollback |
| 8 | [Security, OIDC, Secrets, and Supply Chain](GitHub-Actions-Security-OIDC-Secrets-Supply-Chain-Gold-Sheet.md) | `GITHUB_TOKEN`, permissions, OIDC, fork PR security, action pinning, CodeQL, attestations |
| 9 | [Runners, Networking, Governance, and Scale](GitHub-Actions-Runners-Networking-Governance-Gold-Sheet.md) | hosted/self-hosted runners, ARC, runner groups, private networking, quotas, org controls |
| 10 | [Infrastructure as Code and Database Migration Pipelines](GitHub-Actions-IaC-Database-Migration-Gold-Sheet.md) | Terraform plan/apply, drift, policy, DB migrations, approvals, rollback thinking |
| 11 | [Release Engineering and Progressive Delivery](GitHub-Actions-Release-Engineering-Progressive-Delivery-Gold-Sheet.md) | SemVer, tags, changelogs, releases, hotfix, rollback, feature flags, canary |
| 12 | [Production Operations and Scenario Bank](GitHub-Actions-Production-Operations-Scenario-Bank-Gold-Sheet.md) | failed workflow triage, slow CI, compromised runner, OIDC failure, prod rollback, MAANG scenarios |
| 13 | [Interview Stress Concepts and Production Scenarios](GitHub-Actions-Interview-Stress-Concepts-Production-Scenarios-Gold-Sheet.md) | high-pressure interview concepts: `GITHUB_TOKEN`, OIDC, fork PRs, runner trust, rollback, frontend/CDN traps, prod incidents |
| 14 | [Golden Workflow Templates Library](GitHub-Actions-Golden-Workflow-Templates-Library.md) | ready-to-revise YAML patterns for backend, frontend, Docker, Terraform, Kubernetes, security |

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
- deployment rollback strategy
- preview environments at scale
- Terraform safe apply workflows
- production incident response
- governance across an engineering organization
- reusable workflow and custom action versioning

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

## Official Source Notes

- GitHub Actions documentation: <https://docs.github.com/en/actions>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- Reusing workflows: <https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows>
- OpenID Connect: <https://docs.github.com/en/actions/concepts/security/openid-connect>
- Security hardening: <https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions>
- GitHub-hosted runners: <https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners>
- Self-hosted runners: <https://docs.github.com/en/actions/hosting-your-own-runners>
