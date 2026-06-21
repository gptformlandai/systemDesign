# GitHub Actions Active Recall Question Bank

> Track: GitHub Actions Interview Track - Practice Upgrade  
> Mode: answer from memory before checking notes.

Goal: convert GitHub Actions concepts into fast interview recall and production reasoning.

---

## 1. How To Use

Daily loop:

1. Pick 20 questions.
2. Answer aloud without notes.
3. Include one YAML/config detail when relevant.
4. Name the trust, permission, and rollback boundary.
5. Mark Green, Yellow, or Red.
6. Repeat Red questions after 24 hours and 7 days.

Strong answer shape:

```text
trigger -> jobs/needs -> runner -> permissions/secrets -> artifacts/cache -> deploy/rollback -> observability
```

---

## 2. Foundations Workflow Syntax

1. What is a workflow?
2. What is a job?
3. What is a step?
4. What is an action?
5. What is a runner?
6. What does `on:` define?
7. What does `needs` do?
8. What is a matrix strategy?
9. What is `concurrency` used for?
10. What are contexts?
11. What are expressions?
12. What is the difference between `env`, `vars`, and `secrets`?
13. How do job outputs work?
14. How do you pass data between steps?
15. How do you pass data between jobs?

---

## 3. Backend CI

1. What should a backend PR CI workflow validate?
2. When do you use service containers?
3. How do you upload test reports?
4. Why use `if: always()` for reports?
5. What should be cached in Maven/Gradle/npm/pip?
6. What should not be cached?
7. How do you design a Java/Spring Boot CI pipeline?
8. How do you design a Node API CI pipeline?
9. How do you handle integration tests?
10. How do required checks connect to branch protection?
11. What causes flaky backend CI?
12. How do you split fast PR tests from slow release tests?
13. How do you publish a backend artifact?
14. How do you fail fast without hiding useful reports?
15. What metrics prove backend CI is healthy?

---

## 4. Frontend CI Preview Deployments

1. What should frontend CI validate?
2. npm vs yarn vs pnpm cache strategy?
3. What is a preview deployment?
4. How do you clean preview environments?
5. Why should secrets not be bundled into frontend assets?
6. How do source maps affect security?
7. How do Playwright/Cypress tests fit into CI?
8. What should be artifacted for frontend builds?
9. How do you handle CDN rollback?
10. How do you design PR previews at scale?
11. What is visual regression testing?
12. What causes frontend CI slowness?
13. How do you avoid duplicate preview deploys?
14. How do environment variables differ at build/runtime?
15. What should a production frontend deploy preserve for rollback?

---

## 5. Caching Artifacts Monorepo Performance

1. Cache vs artifact?
2. What makes a good cache key?
3. What is a restore key?
4. What are cache poisoning risks?
5. What should artifact names include?
6. What is artifact retention?
7. How do path filters reduce wasted work?
8. What is an affected build?
9. How do you tune a matrix?
10. How do you use concurrency to cancel stale PR runs?
11. How do you measure CI p95 duration?
12. How do you measure runner queue time?
13. How do you avoid monorepo build explosion?
14. What should be uploaded after failed tests?
15. How do you reduce CI cost without weakening signal?

---

## 6. Reusable Workflows Custom Actions

1. What is `workflow_call`?
2. What are reusable workflow inputs?
3. How do reusable workflow secrets work?
4. What are reusable workflow outputs?
5. Composite action vs reusable workflow?
6. JavaScript action vs Docker action?
7. How do you version reusable workflows?
8. Why pin reusable workflows/actions?
9. What makes a reusable workflow contract stable?
10. How do you roll out breaking workflow changes?
11. What belongs in org-level templates?
12. What should not be hidden inside a reusable workflow?
13. How do you test a custom action?
14. How do you document workflow inputs/outputs?
15. What is the risk of copy-paste YAML?

---

## 7. Docker Containers Registry Pipelines

1. What should a container pipeline do?
2. How do you tag container images safely?
3. Why deploy immutable tags or digests?
4. What is Buildx?
5. What is Docker layer caching?
6. How do you push to GHCR/ECR/ACR/GCR?
7. What registry permissions are needed?
8. What is an SBOM?
9. What is image scanning?
10. What is provenance/attestation?
11. Why should image labels include commit SHA/source?
12. How do you promote images between environments?
13. What is wrong with deploying `latest`?
14. How do you roll back a container release?
15. How do you handle multi-arch builds?

---

## 8. Deployments Environments Kubernetes Cloud

1. What are GitHub environments?
2. What do environment reviewers protect?
3. What are environment secrets?
4. How do branch restrictions help production deploys?
5. What is deployment concurrency?
6. Rolling vs blue-green vs canary?
7. How do you deploy to Kubernetes safely?
8. Helm vs Kustomize?
9. How do you design rollback?
10. Why build once and promote?
11. What does `id-token: write` enable?
12. How should OIDC cloud auth be scoped?
13. How do you avoid deploy races?
14. What should be recorded in deployment summary?
15. What should happen if approval reviewer is unavailable?

---

## 9. Security OIDC Secrets Supply Chain

1. What is `GITHUB_TOKEN`?
2. What does `permissions` control?
3. Why use job-level least privilege?
4. What is OIDC?
5. How is OIDC safer than static cloud keys?
6. What makes an OIDC trust policy safe?
7. Why are fork PRs risky?
8. Why is `pull_request_target` dangerous?
9. How do you protect workflow files?
10. Why pin actions?
11. What is dependency review?
12. What is CodeQL?
13. What are artifact attestations?
14. What is secret scanning?
15. What is the safest response to a leaked secret?

---

## 10. Runners Networking Governance Scale

1. GitHub-hosted vs self-hosted runners?
2. When use self-hosted runners?
3. What is ARC?
4. What are runner groups?
5. What are runner labels?
6. Why avoid untrusted code on internal runners?
7. What are ephemeral runners?
8. How do you diagnose runner queue issues?
9. What is a runner capacity plan?
10. How do private networking needs affect runner choice?
11. How do quotas affect CI design?
12. What org controls matter for Actions?
13. How do you standardize workflows across teams?
14. How do you isolate production deployment runners?
15. How do you monitor runner fleet health?

---

## 11. IaC Database Migrations

1. How should Terraform plan/apply be split?
2. Why upload Terraform plan as artifact?
3. Why should apply use the reviewed plan?
4. What is drift detection?
5. What is policy-as-code?
6. Why do DB migrations need deployment gates?
7. Expand/contract migration pattern?
8. Why are destructive migrations dangerous?
9. How do you roll back infrastructure changes?
10. How do you handle secrets for IaC?
11. What should be manual vs automated?
12. What should a migration pipeline verify?
13. How do you avoid applying from untrusted PRs?
14. How do environments protect IaC?
15. What is the right response to failed migration mid-deploy?

---

## 12. Release Engineering Progressive Delivery

1. What is SemVer?
2. What is a GitHub Release?
3. What should release notes include?
4. What is artifact promotion?
5. What is a canary deploy?
6. What is blue-green deploy?
7. How do feature flags fit release workflows?
8. Rollback vs roll forward?
9. Why retain old artifacts/images?
10. What is hotfix workflow?
11. What is release provenance?
12. What should be approved before production?
13. What deployment metrics matter?
14. How do you recover from bad release?
15. How do you know exactly what version is running?

---

## 13. Advanced Threat Model

1. What is event trust?
2. What is code trust?
3. What is token trust?
4. What is runner trust?
5. What is artifact trust?
6. How can cache poisoning happen?
7. How can artifact poisoning happen?
8. How can command injection happen in workflow scripts?
9. Why treat issue titles, branch names, labels, and inputs as untrusted?
10. What is safe use of `pull_request_target`?
11. Why separate PR validation and deployment workflows?
12. How do third-party actions create supply-chain risk?
13. What does commit SHA pinning improve?
14. How do self-hosted runners change threat model?
15. What should a security review checklist include?

---

## 14. Platform Operations

1. What metrics show Actions platform health?
2. What is workflow ownership metadata?
3. What is a CI SLO?
4. How do you measure flaky failure rate?
5. How do you measure cost by team/repo?
6. What is a golden path workflow?
7. How do you migrate hundreds of workflows?
8. What support model should a platform team provide?
9. What should be on an Actions dashboard?
10. How do you handle a shared reusable workflow incident?
11. How do you deprecate old templates?
12. How do you balance governance with developer speed?
13. What is runner utilization?
14. How do artifact retention policies affect cost/compliance?
15. What should an Actions platform maturity model include?

---

## 15. Workflow Architecture

1. Linear workflow vs fan-out/fan-in?
2. What is a planner job?
3. What is a dynamic matrix?
4. What is an artifact contract?
5. How do reusable workflows act like APIs?
6. What should a reusable workflow contract document?
7. How should concurrency differ for PR CI vs prod deploy?
8. How does `workflow_run` change trust boundaries?
9. What is monorepo orchestration?
10. How do you avoid matrix explosion?
11. Why use a final summary job?
12. Why build once and promote same artifact?
13. What is the anti-pattern of one giant workflow?
14. Why not deploy artifacts from untrusted workflows?
15. How do you design deploy serialization per service/environment?

---

## 16. Final Readiness Gate

You are ready when you can answer without notes:

1. Design PR CI for backend and frontend projects.
2. Explain workflow syntax and data flow across jobs.
3. Design cache/artifact strategy and avoid poisoning risks.
4. Build secure Docker/image/release pipelines.
5. Use environments, OIDC, approvals, and rollback safely.
6. Explain fork PR, `pull_request_target`, token, and runner trust boundaries.
7. Design self-hosted runner isolation and capacity planning.
8. Design Terraform/DB migration pipelines with review gates.
9. Operate GitHub Actions as a platform with metrics, cost, SLOs, and golden paths.
10. Architect monorepo workflows with dynamic matrix, reusable contracts, and deployment serialization.
