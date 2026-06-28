# GitHub Actions Workflow Architecture Patterns Anti Patterns Gold Sheet

> Track: GitHub Actions Interview Track - Senior / MAANG Architecture Layer

Goal: design maintainable workflow architectures: fan-out/fan-in, dynamic matrix, reusable workflow contracts, artifacts as interfaces, concurrency, event chaining, monorepo orchestration, deployment gates, and anti-patterns.

---

## 0. How To Read This

Use this after foundations, caching/performance, reusable workflows, deployments, and production operations.

Architecture mental model:

```text
event -> planner -> parallel validation -> artifact contract -> gated deploy -> observe/rollback
```

Interview rule:

```text
A production workflow should be designed like a small distributed system: clear inputs, outputs, dependencies, failure handling, and trust boundaries.
```

---

# Topic 1: Workflow Architecture Patterns

## 1. Intuition

Small workflows can be linear.

Large workflows need architecture.

A mature workflow answers:

- what starts it?
- what work can run in parallel?
- what must wait?
- what data passes between jobs?
- what should be cached vs artifacted?
- what happens if one service fails?
- how are stale runs canceled?
- how does deployment get approved?
- how is rollback triggered?

---

## 2. Definition

- Definition: GitHub Actions workflow architecture is the design of workflow triggers, jobs, dependencies, matrices, reusable workflow contracts, artifacts, environments, concurrency, and failure paths to deliver reliable CI/CD at scale.
- Category: CI/CD system design.
- Core idea: organize workflow YAML around dependencies, trust, speed, and operational clarity.

---

## 3. Why It Exists

Without architecture:

- workflows become long copy-paste scripts
- every job waits unnecessarily
- artifacts are unclear or overwritten
- matrix jobs explode in cost
- deployments race each other
- stale PR runs waste capacity
- monorepo workflows build everything
- reusable workflows break callers
- workflow chains hide failure root cause
- untrusted and trusted jobs mix credentials

---

## 4. Pattern: Linear CI

Good for small projects.

```text
checkout -> install -> lint -> test -> build
```

Pros:

- simple
- easy to debug
- low YAML complexity

Cons:

- slow if independent steps could run in parallel
- one job failure hides later issues
- harder to scale with multiple languages/services

Use when repo is small and feedback time is acceptable.

---

## 5. Pattern: Fan-Out / Fan-In

Fan-out runs independent checks in parallel.

```text
          lint
         /
plan -> test -> package -> summarize
         \
          security
```

YAML idea:

```yaml
jobs:
  plan:
    runs-on: ubuntu-latest
    outputs:
      run_backend: ${{ steps.plan.outputs.backend }}

  lint:
    needs: plan
    if: needs.plan.outputs.run_backend == 'true'
    runs-on: ubuntu-latest

  test:
    needs: plan
    runs-on: ubuntu-latest

  summarize:
    needs: [lint, test]
    if: always()
    runs-on: ubuntu-latest
```

Use when:

- checks are independent
- developer feedback time matters
- summary/report job should always run

---

## 6. Pattern: Matrix Builds

Matrix builds test combinations.

```yaml
strategy:
  fail-fast: false
  matrix:
    node: [20, 22]
    os: [ubuntu-latest, macos-latest]
```

Use for:

- language versions
- OS compatibility
- service variants
- package combinations

Avoid matrix explosion:

- test only meaningful combinations
- split PR vs nightly matrix
- use `include`/`exclude`
- cap parallelism with `max-parallel`
- run expensive variants on schedule/release

---

## 7. Pattern: Dynamic Matrix

A planner job computes matrix values.

Use cases:

- monorepo affected services
- changed packages
- selected regions
- test shards

Example shape:

```yaml
jobs:
  plan:
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.set.outputs.matrix }}
    steps:
      - id: set
        run: echo 'matrix={"service":["orders","payments"]}' >> "$GITHUB_OUTPUT"

  test:
    needs: plan
    strategy:
      matrix: ${{ fromJson(needs.plan.outputs.matrix) }}
    runs-on: ubuntu-latest
    steps:
      - run: echo "Testing ${{ matrix.service }}"
```

Caution:

```text
Validate planner output. Do not let untrusted input expand into privileged deployment jobs.
```

---

## 8. Pattern: Artifact Contract

Artifacts should be treated as job interfaces.

Good artifact contract includes:

- artifact name
- producer job
- commit SHA
- run id
- retention
- checksum/provenance if release artifact
- consumer job
- trust level

Example:

```text
build-image-metadata-${{ github.sha }}
frontend-dist-${{ github.sha }}
test-report-${{ github.run_id }}
```

Rule:

```text
Use artifacts for outputs that must cross job boundaries. Use cache only for dependencies or reusable build inputs, not deployable truth.
```

---

## 9. Pattern: Reusable Workflow Contract

Reusable workflows are APIs.

Contract includes:

- inputs
- secrets
- outputs
- permissions expected
- runner assumptions
- versioning policy
- breaking change policy

Example contract questions:

```text
What inputs are required?
What secrets are passed explicitly?
What permissions must caller grant?
What outputs can downstream jobs depend on?
What version should callers pin?
```

Strong answer:

```text
A reusable workflow should have a stable contract like a library API. Breaking it can break every repo using it.
```

---

## 10. Pattern: Concurrency And Cancellation

Use concurrency to avoid stale or racing runs.

PR CI:

```yaml
concurrency:
  group: pr-${{ github.event.pull_request.number }}
  cancel-in-progress: true
```

Production deploy:

```yaml
concurrency:
  group: production-deploy
  cancel-in-progress: false
```

Interpretation:

- PR validation: cancel stale runs.
- Production deploy: serialize deployments; do not cancel running deploys casually.

---

## 11. Pattern: Environment Gates

Environments add deployment control.

Use for:

- stage/prod separation
- required reviewers
- branch/tag restrictions
- environment secrets
- deployment records

Example:

```yaml
jobs:
  deploy:
    environment: production
    permissions:
      id-token: write
      contents: read
```

Rule:

```text
Environment gates protect deployment jobs, but only if credentials and runners are also scoped correctly.
```

---

## 12. Pattern: Workflow Chaining

Common chaining methods:

| Method | Use |
|---|---|
| `needs` | jobs in same workflow |
| reusable workflow | standardized workflow call |
| `workflow_run` | trigger workflow after another workflow completes |
| `repository_dispatch` | cross-repo event integration |
| `workflow_dispatch` | manual operator trigger |

Caution:

```text
Workflow chains can hide trust boundaries. Do not let an untrusted producer workflow trigger a trusted deployment with untrusted artifacts.
```

---

## 13. Pattern: Monorepo Orchestration

Monorepo workflow shape:

```text
changed paths -> affected services matrix -> service CI -> shared checks -> optional deploy previews
```

Key choices:

- path filters for quick skip
- planner job for affected services
- service-level reusable workflows
- shared dependency cache strategy
- per-service artifacts
- CODEOWNERS and required checks by path
- concurrency by PR/service
- preview environment TTL cleanup

Anti-trap:

```text
Do not run every service's full pipeline on every README or docs change.
```

---

## 14. Pattern: Deployment Promotion

Good release architecture:

```text
build once -> scan/sign/attest -> promote same artifact -> deploy stage -> approve -> deploy prod
```

Avoid:

```text
Rebuilding separate artifacts per environment from different source states.
```

Why:

- traceability breaks
- rollback becomes unclear
- environment drift appears
- debugging gets harder

---

## 15. Pattern: Summary And Reporting Job

Use a final job to report outcomes even on failure.

```yaml
summary:
  needs: [lint, test, build]
  if: always()
  runs-on: ubuntu-latest
  steps:
    - run: echo "Summarize results"
```

Good for:

- test report aggregation
- PR comments
- deployment summary
- artifact links
- failure triage hints

Caution:

```text
Summary jobs should not hide failed required checks. They complement, not replace, real checks.
```

---

## 16. Anti-Patterns

| Anti-Pattern | Better Approach |
|---|---|
| one giant workflow with no job boundaries | separate jobs by dependency/trust/output |
| every job has write permissions | job-level least privilege |
| deploy job depends on untrusted artifact | build release artifact from trusted ref |
| matrix includes every possible combination on PR | PR subset plus nightly/full release matrix |
| no concurrency | cancel stale PR runs and serialize deploys |
| cache used as release artifact | use artifacts/images with provenance |
| reusable workflow with undocumented inputs | versioned contract and changelog |
| workflow_run deploys from any upstream run | restrict producer workflow/ref/artifact trust |
| monorepo builds everything always | affected build planning |
| environment approval but broad OIDC role | restrict cloud trust to repo/ref/environment |

---

## 17. Design Checklist

Before finalizing a workflow architecture:

1. What event triggers it?
2. Which jobs are independent?
3. Which jobs need outputs from earlier jobs?
4. What data crosses job boundaries?
5. What is cache vs artifact?
6. What permissions does each job need?
7. Which jobs run untrusted code?
8. Which jobs can deploy or mutate state?
9. Should stale runs cancel?
10. Should deployments serialize?
11. Is a matrix needed? Can it be smaller?
12. Does monorepo affected logic avoid wasted work?
13. Does environment approval protect the right job?
14. Are reusable workflows versioned?
15. Is there a rollback/manual path?

---

## 18. Scenario

Prompt:

```text
Design GitHub Actions for a monorepo with 80 services. PR checks are slow, deployments race each other, and teams copy-paste workflows.
```

Strong answer:

```text
I would add a planner job that detects affected services and emits a dynamic matrix. Each service would call a versioned reusable workflow with a clear input/output contract. Independent lint/test/build/security jobs would fan out, then a summary job would aggregate reports. PR runs would use concurrency to cancel stale runs by PR and service. Deployments would use immutable artifacts or images, environment gates, and deployment concurrency per environment/service so production deploys do not race. I would standardize this as golden-path reusable workflows rather than letting teams copy YAML.
```

---

## 19. Revision Notes

- Workflow architecture is about dependencies, trust, outputs, speed, and recovery.
- Fan-out/fan-in improves feedback time.
- Dynamic matrix is powerful for monorepos but planner output must be trusted/validated.
- Artifacts are job interfaces; cache is not deployment truth.
- Reusable workflows are APIs with contracts and versioning.
- PR concurrency should cancel stale work; production concurrency should serialize safely.
- Environment gates must align with credentials and runner trust.
- Build once and promote the same artifact.

---

## 20. Official Source Notes

- Workflow syntax: https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax
- Matrix jobs: https://docs.github.com/en/actions/using-jobs/using-a-matrix-for-your-jobs
- Passing job outputs: https://docs.github.com/en/actions/using-jobs/defining-outputs-for-jobs
- Reusing workflows: https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows
- Workflow concurrency: https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#concurrency
- Deployment environments: https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment
