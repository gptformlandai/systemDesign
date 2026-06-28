# GitHub Actions Caching, Artifacts, Monorepos, and Performance Gold Sheet

> Goal: make GitHub Actions fast and scalable using correct cache strategy, artifacts, path filters, affected builds, matrix control, and monorepo patterns.

---

## 0. How To Read This

Beginner focus:

- cache vs artifact
- dependency cache
- upload artifacts
- path filters

Intermediate focus:

- cache keys
- restore keys
- matrix optimization
- monorepo affected builds
- Docker layer cache

Senior focus:

- CI cost control
- large monorepo architecture
- reusable workflow performance
- flaky and slow pipeline triage
- branch protection with selective checks

---

# Topic 1: Caching, Artifacts, Monorepos, and Performance

---

## 1. Intuition

CI performance is about not doing unnecessary work.

Ask:

```text
Can I reuse downloaded dependencies?
Can I avoid rebuilding untouched apps?
Can I parallelize safely?
Can I upload useful outputs for debugging?
Can I cancel stale runs?
```

Beginner explanation:

Cache speeds future workflow runs. Artifacts preserve outputs from the current run. Monorepo optimization runs only what is affected by a change.

---

## 2. Definition

- Definition: GitHub Actions performance design is the set of techniques used to reduce workflow time, cost, and noise while preserving confidence.
- Category: CI/CD optimization
- Core idea: reuse safe dependencies, avoid irrelevant work, parallelize wisely, and preserve useful outputs.

---

## 3. Why It Exists

Slow CI causes:

- delayed PR reviews
- developers bypassing checks
- higher runner cost
- slower incident fixes
- merge queues backing up
- low trust in automation

Good CI performance improves developer velocity without weakening quality.

---

## 4. Reality

Performance topics show up in:

- monorepos
- frontend apps
- Maven/Gradle builds
- Docker builds
- large test suites
- matrix builds
- multi-service repositories
- platform teams managing CI cost

MAANG-level interviewers like asking:

> CI takes 45 minutes for every PR. What do you do?

---

## 5. How It Works

### Part A: Cache vs Artifact

| Concept | Purpose | Example |
|---|---|---|
| Cache | speed up future runs | Maven repo, npm cache, pnpm store |
| Artifact | save output from current run | test reports, build zip, screenshots |

Memory trick:

```text
cache = reuse later
artifact = inspect/share this run
```

### Part B: Cache Keys

Good cache key:

```yaml
key: maven-${{ runner.os }}-${{ hashFiles('**/pom.xml') }}
restore-keys: |
  maven-${{ runner.os }}-
```

The key should change when dependencies change.

Common dependency inputs:

- `pom.xml`
- `build.gradle`
- `package-lock.json`
- `pnpm-lock.yaml`
- `yarn.lock`
- `requirements.txt`
- `go.sum`

### Part C: Built-In Setup Action Cache

Prefer language setup cache where available:

```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: "21"
    cache: maven
```

```yaml
- uses: actions/setup-node@v4
  with:
    node-version: "22"
    cache: npm
```

### Part D: Artifacts

Upload artifacts for debugging:

```yaml
- uses: actions/upload-artifact@v4
  if: always()
  with:
    name: test-reports
    path: |
      target/surefire-reports/
      coverage/
```

Important:

- use `if: always()` for failure diagnostics
- avoid uploading secrets
- set retention appropriately
- avoid giant artifacts unless needed

### Part E: Path Filters

Path filters avoid irrelevant workflows:

```yaml
on:
  pull_request:
    paths:
      - "services/order/**"
      - ".github/workflows/order-ci.yml"
```

Use for:

- separate services
- separate frontend apps
- docs-only changes
- infrastructure folders

Trap:

Path filters can accidentally skip required checks if branch protection expects them. Plan required checks carefully.

### Part F: Monorepo Affected Builds

Large monorepos need affected-build logic:

```text
changed files
-> map to projects
-> include dependents
-> run only affected tests/builds
```

Tools:

- Nx
- Turborepo
- Bazel
- Pants
- Gradle build cache
- custom path mapping

### Part G: Matrix Control

Matrix is powerful but can explode:

```yaml
strategy:
  fail-fast: false
  max-parallel: 4
  matrix:
    java: ["17", "21"]
    os: [ubuntu-latest, windows-latest]
```

Use `include` and `exclude` to avoid useless combinations.

### Part H: Concurrency

Cancel stale PR runs:

```yaml
concurrency:
  group: pr-${{ github.event.pull_request.number }}
  cancel-in-progress: true
```

Do not cancel production deploys mid-flight unless the deployment system is safe for that.

### Part I: Split Jobs By Feedback Value

Example:

```text
fast checks:
  lint, typecheck, unit tests

slower checks:
  integration, E2E, visual, performance

release checks:
  full smoke, security scan, deployment validation
```

Fast failures should fail early.

### Part J: Docker Layer Cache

For Docker builds, use buildx cache:

```yaml
cache-from: type=gha
cache-to: type=gha,mode=max
```

Important:

- cache must not hide a broken Dockerfile
- image tags should still be deterministic

### Part K: Merge Queue Consideration

If using merge queues:

- keep required checks stable
- avoid skipped required check confusion
- ensure final tested SHA is the one merged
- use deterministic workflows

---

## 6. What Problem It Solves

- Primary problem solved: reduce CI time and cost without losing confidence
- Secondary benefits: better developer experience, faster incident fixes, scalable monorepos
- Systems impact: keeps CI useful as repositories and teams grow

---

## 7. When To Rely On It

Use optimization when:

- CI exceeds acceptable PR feedback time
- monorepo has many apps/services
- runner cost is high
- large Docker images are built
- matrix builds are expensive
- engineers wait too long for required checks

Interviewer keywords:

- slow CI
- monorepo
- affected builds
- cache miss
- runner cost
- matrix explosion
- flaky pipeline

---

## 8. When Not To Over-Optimize

Avoid optimization that:

- skips important tests incorrectly
- makes workflows unreadable
- creates unreliable caches
- hides failures
- produces inconsistent required checks

Rule:

> First make CI correct. Then make it fast.

---

## 9. Pros and Cons

| Technique | Pros | Cons |
|---|---|---|
| Cache | faster installs | stale/invalid cache confusion |
| Artifacts | better debugging | storage/cost/security risk |
| Path filters | skip irrelevant jobs | can skip needed checks |
| Affected builds | monorepo scale | dependency graph complexity |
| Matrix | compatibility confidence | cost/time explosion |
| Concurrency cancel | faster PR feedback | unsafe for deploys if misused |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Selective CI:
  Faster, but requires correct change detection.
- Full CI:
  Safer, but slower.
- More parallelism:
  Faster wall-clock, but more runner usage.
- More caching:
  Faster, but more invalidation complexity.

### Common Mistakes

- Mistake: "Cache build outputs that should be rebuilt."
  Why it is wrong: stale outputs can hide problems.
  Better approach: cache dependencies, not correctness-critical outputs unless the build tool owns cache validity.

- Mistake: "Use path filters without branch protection design."
  Why it is wrong: required checks may be skipped or stuck.
  Better approach: align path filters with required checks or use aggregator workflows.

- Mistake: "Huge matrix on every PR."
  Why it is wrong: wastes time/cost.
  Better approach: run critical matrix on PR, full matrix nightly/release.

- Mistake: "No artifacts on failure."
  Why it is wrong: debugging is blind.
  Better approach: upload reports with `if: always()`.

---

## 11. Key Numbers

Useful targets:

- PR feedback: ideally under 5 to 10 minutes for common changes
- Heavy E2E/performance: scheduled or release-gated
- Cache key should include dependency lockfiles
- Artifact retention should match debugging/compliance needs
- Matrix max parallel should respect runner quota and downstream capacity

---

## 12. Failure Modes

### Cache Miss Every Run

Causes:

- key includes changing value
- wrong path
- lockfile path mismatch

Fix:

- use stable key with lockfile hash
- check cache path
- use setup action cache when possible

### Path Filter Skips Needed Test

Cause:

- dependency between modules not modeled

Fix:

- affected-build graph
- include shared library paths
- add periodic full CI

### CI Still Slow After Cache

Cause:

- tests are slow
- jobs are serial
- Docker build dominates
- too much matrix

Fix:

- split tests
- parallelize
- Docker layer cache
- affected builds
- profile slow tests

### Artifact Contains Secret

Cause:

- uploaded `.env`, logs, source maps, or config

Fix:

- audit artifact paths
- mask sensitive logs
- avoid uploading secrets
- rotate exposed secret

---

## 13. Scenario

- Product / system: monorepo with 50 backend services and 20 frontend apps
- Why this concept fits: running every test for every PR is too slow and expensive
- What would go wrong without it: developer feedback slows, required checks pile up, and teams bypass CI

---

## 14. Code Sample

PR concurrency and dependency cache:

```yaml
name: Fast PR Checks

on:
  pull_request:

permissions:
  contents: read

concurrency:
  group: pr-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  frontend:
    if: contains(github.event.pull_request.changed_files, 'frontend')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
        working-directory: frontend
      - run: npm test
        working-directory: frontend
```

Note:

The `if` above is simplified for learning. Real monorepos usually use path filter actions, Nx/Turborepo/Bazel, or custom changed-project detection.

---

## 15. Mini Program / Simulation

Affected project mapping:

```python
changes = ["apps/web/src/App.tsx", "packages/ui/Button.tsx"]

projects = {
    "apps/web": ["web"],
    "apps/admin": ["admin"],
    "packages/ui": ["web", "admin"],
}

affected = set()
for path in changes:
    for prefix, impacted in projects.items():
        if path.startswith(prefix):
            affected.update(impacted)

print("affected projects:", sorted(affected))
```

---

## 16. Practical Question

> Your GitHub Actions PR workflow takes 45 minutes in a monorepo. How do you improve it without reducing safety?

---

## 17. Strong Answer

I would first measure where time is spent: dependency install, build, unit tests, integration tests, Docker build, or queue time. Then I would add safe caching for dependency managers, split independent jobs in parallel, and use affected-build detection so untouched services do not run.

I would keep critical fast checks required on PRs and move heavyweight full matrix, E2E, or performance jobs to nightly or release gates. For monorepos, I would model shared dependencies so changes to shared libraries trigger dependent apps. I would use concurrency to cancel stale PR runs and upload artifacts for failures.

The goal is not simply fewer checks; it is faster feedback with the same risk coverage.

---

## 18. Revision Notes

- One-line summary: CI performance is about caching safe inputs, avoiding irrelevant work, and preserving useful outputs.
- Three keywords: cache, artifact, affected build
- One interview trap: path filters can skip required safety checks if dependency graph is wrong.
- One memory trick: cache for speed, artifact for evidence, affected builds for scale.

---

## 19. Official Source Notes

- Caching dependencies: <https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows>
- Artifacts: <https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>

