# GitHub Actions Foundations and Workflow Syntax Gold Sheet

> Goal: understand GitHub Actions from first principles: workflow, event, job, step, action, runner, contexts, expressions, matrix, outputs, concurrency, and reusable workflows.

---

## 0. How To Read This

Beginner focus:

- workflow
- job
- step
- action
- runner
- event

Intermediate focus:

- `needs`
- `if`
- matrix
- outputs
- contexts
- expressions
- artifacts
- concurrency

Senior focus:

- reusable workflows
- least-privilege permissions
- event security
- workflow control
- governance and templates

---

# Topic 1: GitHub Actions Foundations and Workflow Syntax

---

## 1. Intuition

GitHub Actions is an automation engine attached to your repository.

Think of it like this:

- an event rings the bell
- a workflow wakes up
- jobs run on machines called runners
- steps execute commands or actions
- outputs, artifacts, and deployment results flow forward

Beginner explanation:

GitHub Actions runs automation when something happens in GitHub, such as a push or pull request. A workflow contains jobs, each job runs on a runner, and each job contains steps.

---

## 2. Definition

- Definition: GitHub Actions is GitHub's automation platform for CI/CD and operational workflows defined as YAML files under `.github/workflows`.
- Category: CI/CD automation platform
- Core idea: repository events trigger workflows that run jobs on runners and execute steps to test, build, scan, package, and deploy software.

---

## 3. Why It Exists

Without CI/CD automation:

- developers run tests manually
- deployments are inconsistent
- security scans are forgotten
- release steps are tribal knowledge
- environments drift
- rollback is improvised
- teams cannot scale delivery safely

GitHub Actions exists to make delivery repeatable, reviewable, auditable, and automated.

---

## 4. Reality

GitHub Actions is used for:

- backend CI
- frontend CI
- Docker builds
- Kubernetes deployments
- Terraform plan/apply
- release generation
- security scanning
- preview deployments
- scheduled maintenance
- ChatOps/manual operations

MAANG-level expectation:

You should not only write YAML. You should explain security, scale, failure handling, and production rollout behavior.

---

## 5. How It Works

### Part A: Workflow File

Workflow files live here:

```text
.github/workflows/<name>.yml
```

Example:

```yaml
name: Backend CI

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - run: ./mvnw test
```

### Part B: Workflow, Job, Step, Action

| Concept | Meaning |
|---|---|
| Workflow | YAML automation definition |
| Event | Trigger that starts a workflow |
| Job | Unit of work running on a runner |
| Step | Command or action inside a job |
| Action | Reusable packaged automation |
| Runner | Machine that executes jobs |

Mental model:

```text
workflow = plan
job = stage
step = instruction
action = reusable tool
runner = machine
```

### Part C: Events

Common triggers:

```yaml
on:
  push:
  pull_request:
  workflow_dispatch:
  schedule:
    - cron: "0 2 * * *"
```

Important events:

- `push`: code was pushed
- `pull_request`: PR opened/synchronized/reopened
- `workflow_dispatch`: manual trigger
- `schedule`: cron schedule
- `workflow_call`: reusable workflow trigger
- `release`: release activity
- `deployment`: deployment events

Security note:

Be careful with events that run untrusted code, especially forked pull requests.

### Part D: Jobs and Dependencies

Jobs run in parallel by default.

Use `needs` to define order:

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: ./mvnw test

  build:
    runs-on: ubuntu-latest
    needs: test
    steps:
      - run: ./mvnw package
```

Interview sentence:

> Jobs are parallel unless connected with `needs`.

### Part E: Conditions

Use `if` for conditional execution:

```yaml
jobs:
  deploy:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - run: ./deploy.sh
```

Common conditions:

```yaml
if: github.event_name == 'pull_request'
if: github.ref == 'refs/heads/main'
if: success()
if: failure()
if: always()
```

### Part F: Contexts

Contexts are data objects available to workflows.

Common contexts:

| Context | Use |
|---|---|
| `github` | repo, branch, event, actor, SHA |
| `env` | environment variables |
| `vars` | repository/org/environment variables |
| `secrets` | sensitive values |
| `matrix` | matrix values |
| `needs` | outputs from dependent jobs |
| `steps` | outputs from previous steps |
| `runner` | runner details |

Example:

```yaml
- run: echo "Commit is ${{ github.sha }}"
```

### Part G: Expressions

Expressions use `${{ ... }}`.

Examples:

```yaml
if: ${{ github.ref == 'refs/heads/main' }}
run-name: Deploy ${{ github.sha }} to production
```

Common functions:

```yaml
contains()
startsWith()
endsWith()
format()
fromJSON()
hashFiles()
success()
failure()
always()
cancelled()
```

### Part H: Matrix Builds

Matrix runs the same job with multiple combinations.

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: ["17", "21"]
    steps:
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ matrix.java }}
      - run: ./mvnw test
```

Use matrix for:

- language versions
- OS versions
- package variants
- browser versions
- service modules

Do not use huge matrices blindly. They can burn minutes and slow feedback.

### Part I: Outputs

Step output:

```yaml
- id: version
  run: echo "value=1.2.3" >> "$GITHUB_OUTPUT"
```

Job output:

```yaml
jobs:
  build:
    outputs:
      version: ${{ steps.version.outputs.value }}
```

Use outputs for:

- image tags
- generated versions
- artifact names
- deployment URLs
- computed module lists

### Part J: Artifacts

Artifacts store files from a workflow run:

- test reports
- coverage reports
- build outputs
- deployment manifests
- screenshots
- Playwright traces

Example:

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: test-reports
    path: target/surefire-reports/
```

Artifacts are for sharing build outputs/results, not dependency caching.

### Part K: Cache

Cache speeds up repeated dependency downloads:

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: maven-${{ runner.os }}-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      maven-${{ runner.os }}-
```

Cache is best-effort. A cache miss should not break the build.

### Part L: Concurrency

Concurrency prevents duplicate runs from fighting.

```yaml
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
```

Use for:

- canceling old PR runs
- one deployment per environment
- preventing two production deploys at once

Production deployment example:

```yaml
concurrency:
  group: deploy-production
  cancel-in-progress: false
```

### Part M: Permissions

Set least privilege:

```yaml
permissions:
  contents: read
```

For deployment with OIDC:

```yaml
permissions:
  contents: read
  id-token: write
```

Avoid relying on broad default permissions.

### Part N: Reusable Workflows

Reusable workflow:

```yaml
on:
  workflow_call:
    inputs:
      java-version:
        required: true
        type: string
```

Caller:

```yaml
jobs:
  ci:
    uses: org/platform/.github/workflows/java-ci.yml@v1
    with:
      java-version: "21"
```

Use reusable workflows for:

- org-wide CI templates
- security scanning standards
- deployment patterns
- Terraform plan/apply
- Docker build/publish

### Part O: Composite Actions

Composite action packages steps into an action:

```yaml
runs:
  using: composite
  steps:
    - shell: bash
      run: echo "hello"
```

Reusable workflow vs composite action:

| Need | Use |
|---|---|
| Standardize whole jobs/workflows | reusable workflow |
| Reuse a sequence of steps | composite action |
| Needs secrets/environments/permissions orchestration | reusable workflow |
| Small local helper | composite action |

---

## 6. What Problem It Solves

- Primary problem solved: repeatable automation for test, build, release, deploy, and operations
- Secondary benefits: auditability, fast feedback, consistent quality gates, governance
- Systems impact: turns delivery from manual procedure into versioned infrastructure

---

## 7. When To Rely On It

Use GitHub Actions when:

- code is hosted in GitHub
- PR checks are needed
- deployments should be traceable
- automation should live close to code
- teams need reusable CI/CD templates
- security scans should run automatically

Interviewer trigger words:

- CI/CD
- GitHub
- pipeline
- pull request checks
- deployment approval
- OIDC
- reusable workflow
- runner
- monorepo

---

## 8. When Not To Use It

Avoid using GitHub Actions as the only tool when:

- workloads are extremely long-running
- complex release orchestration already lives in a mature deployment platform
- high-volume data processing is needed
- secrets/network constraints require another execution platform
- compliance requires dedicated deployment tooling

Alternatives:

- Jenkins
- GitLab CI
- CircleCI
- Azure DevOps
- Argo CD
- Spinnaker
- Tekton
- Buildkite

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Native GitHub integration | YAML can become messy |
| Strong marketplace ecosystem | Supply-chain risk from third-party actions |
| Good for CI/CD and automation | Runner minutes/cost can grow |
| Reusable workflows support platform patterns | Debugging complex expressions can be painful |
| OIDC support reduces static cloud secrets | Self-hosted runners require hardening |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- More workflow reuse:
  Better consistency, but harder local customization.
- Larger matrix:
  More confidence, but slower and costlier CI.
- Aggressive cache:
  Faster builds, but stale cache issues can confuse debugging.
- Self-hosted runners:
  More control/private networking, but more security and maintenance work.
- Broad permissions:
  Easier setup, but larger blast radius.

### Common Mistakes

- Mistake: "Every job runs after the previous job."
  Why it is wrong: jobs run in parallel unless `needs` is used.
  Better approach: define job dependencies explicitly.

- Mistake: "Cache and artifact are the same."
  Why it is wrong: cache speeds future runs; artifact preserves outputs from this run.
  Better approach: cache dependencies, upload build/test results as artifacts.

- Mistake: "Use secrets in PR workflows from forks."
  Why it is wrong: fork PR code is untrusted.
  Better approach: separate untrusted validation from trusted deployment/secrets workflows.

- Mistake: "Use default token permissions everywhere."
  Why it is wrong: unnecessary write permissions increase risk.
  Better approach: set `permissions` explicitly.

- Mistake: "Copy YAML into every repo."
  Why it is wrong: standards drift.
  Better approach: reusable workflows and governed templates.

---

## 11. Key Numbers

Use these as reasoning anchors:

- Keep PR feedback ideally under 5 to 10 minutes for common changes.
- Keep production deploy workflows serialized per environment.
- Use matrix builds only where compatibility matters.
- Cache should reduce dependency install time, but builds must pass without cache.
- Reusable workflows should be versioned with tags or SHAs for stability.

Exact limits and runner sizes change over time. Always check official GitHub docs for current limits.

---

## 12. Failure Modes

### Workflow Does Not Trigger

Causes:

- wrong event
- branch/path filter mismatch
- workflow file not on expected branch
- YAML syntax issue
- workflow disabled

Fix:

- inspect workflow event config
- verify branch/path filters
- check Actions tab
- validate YAML

### Job Runs Before Dependency

Cause:

- missing `needs`

Fix:

- add explicit job dependency
- pass outputs through `needs`

### Deployment Runs Twice

Cause:

- missing concurrency group
- multiple triggers overlap

Fix:

- use environment-specific concurrency
- separate PR validation from production deployment

### Secret Not Available

Causes:

- running on fork PR
- secret configured at wrong scope
- environment secret requires environment
- typo in name

Fix:

- verify event trust boundary
- verify repo/org/environment secret scope
- check environment selection

### Permission Denied

Cause:

- `GITHUB_TOKEN` lacks required permission
- OIDC `id-token: write` missing
- cloud trust policy mismatch

Fix:

- set explicit permissions
- validate cloud role trust conditions

---

## 13. Scenario

- Product / system: Pull request CI for a backend and frontend monorepo
- Why this concept fits: every PR needs repeatable quality gates before merge
- What would go wrong without it: manual testing, inconsistent builds, secret exposure, and broken main branch

---

## 14. Code Sample

Foundation workflow:

```yaml
name: PR Checks

on:
  pull_request:
    branches: [main]

permissions:
  contents: read

concurrency:
  group: pr-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: ["17", "21"]
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ matrix.java }}
          cache: maven

      - run: ./mvnw test

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-reports-java-${{ matrix.java }}
          path: target/surefire-reports/
```

---

## 15. Mini Program / Simulation

This simple mental simulation shows job dependency order.

```python
jobs = {
    "lint": [],
    "test": [],
    "build": ["lint", "test"],
    "deploy": ["build"],
}


def can_run(job, completed):
    return all(dep in completed for dep in jobs[job])


completed = set()
while len(completed) < len(jobs):
    runnable = [job for job in jobs if job not in completed and can_run(job, completed)]
    print("can run now:", runnable)
    completed.update(runnable)
```

Interview takeaway:

`needs` creates the dependency graph. Without it, independent jobs run in parallel.

---

## 16. Practical Question

> You are designing GitHub Actions for a repository. How would you structure PR checks and production deployment safely?

---

## 17. Strong Answer

I would separate PR validation from deployment. PR workflows would run with minimal `contents: read` permissions, no production secrets, and fast jobs such as lint, tests, typecheck, build, and security checks. I would use concurrency to cancel stale PR runs.

For deployment, I would trigger from `main`, use GitHub environments for stage/prod, require approvals for production, and serialize deployments with a concurrency group. Cloud authentication would use OIDC rather than long-lived static credentials. Artifacts or container images would be built once, scanned, and promoted.

I would make workflows reusable where possible so standards do not drift across repositories.

---

## 18. Revision Notes

- One-line summary: GitHub Actions turns repository events into runner-executed workflow jobs.
- Three keywords: workflow, runner, permissions
- One interview trap: jobs run in parallel unless `needs` is set.
- One memory trick: event wakes workflow, job rents runner, step does work.

---

## 19. Official Source Notes

- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- Contexts: <https://docs.github.com/en/actions/learn-github-actions/contexts>
- Expressions: <https://docs.github.com/en/actions/reference/workflows-and-actions/expressions>
- Reusable workflows: <https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows>

