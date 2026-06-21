# GitHub Actions Scenario Drill Bank

> Track: GitHub Actions Interview Track - Practice Upgrade  
> Goal: practice CI/CD design, security, debugging, and platform operations under interview pressure.

---

## 1. Answer Format

For design scenarios:

```text
trigger -> jobs/dependencies -> runner -> permissions/secrets -> artifacts/cache -> deploy/rollback -> observability
```

For incident scenarios:

```text
symptom -> blast radius -> recent change -> logs/runner/security checks -> mitigation -> prevention
```

For security scenarios:

```text
event trust -> code trust -> token/secrets -> runner trust -> artifact/cache trust -> safer design
```

---

## 2. Foundation Scenarios

### Scenario 1: Basic PR CI

Prompt:

```text
Design GitHub Actions PR CI for a backend service.
```

Must include:

- `pull_request` trigger
- checkout/setup language
- dependency cache
- lint/test/build
- test reports with `if: always()`
- minimal permissions
- required check in branch protection

---

### Scenario 2: Job Dependency Design

Prompt:

```text
A workflow has lint, unit tests, integration tests, build, and publish. What should depend on what?
```

Must include:

- parallel independent checks
- publish only after validation
- artifacts from build
- summary job with `if: always()`

---

### Scenario 3: Matrix Too Large

Prompt:

```text
A matrix tests 5 OS versions, 4 language versions, and 6 database versions on every PR.
```

Must include:

- reduce PR matrix
- run full matrix nightly/release
- meaningful include/exclude
- `max-parallel`
- cost and feedback trade-off

---

## 3. Backend Frontend CI Scenarios

### Scenario 4: Backend Integration Tests Flaky

Prompt:

```text
Service-container integration tests fail randomly.
```

Must include:

- readiness checks
- isolated test data
- resource constraints
- service logs as artifacts
- retry only as temporary mitigation

---

### Scenario 5: Frontend Preview Cost Explosion

Prompt:

```text
Preview environments are created for every PR but never cleaned up.
```

Must include:

- cleanup on PR close
- TTL job
- concurrency by PR
- labels/manual trigger for expensive previews
- cost dashboard

---

### Scenario 6: CDN Rollback

Prompt:

```text
Frontend deploy is broken after CDN upload.
```

Must include:

- immutable asset hashes
- short-cache HTML shell
- previous artifact retained
- rollback previous version
- source map/error tracking review

---

## 4. Cache Artifact Performance Scenarios

### Scenario 7: Cache Never Hits

Prompt:

```text
CI is slow because dependency cache never restores.
```

Must include:

- inspect cache key
- lockfile hash
- branch/OS/toolchain keys
- restore-key strategy
- language setup built-in cache

---

### Scenario 8: Bad Artifact Deployed

Prompt:

```text
Production deploy used an artifact uploaded by a PR workflow.
```

Must include:

- artifact trust boundary violation
- separate PR validation artifacts from release artifacts
- build trusted release artifact from protected branch/tag
- provenance/checksum

---

### Scenario 9: Monorepo CI Too Slow

Prompt:

```text
A 70-service monorepo builds every service on every PR.
```

Must include:

- changed path detection
- affected service planner
- dynamic matrix
- path filters
- service-level reusable workflows
- full nightly/release sweep

---

## 5. Reusable Workflow Scenarios

### Scenario 10: Reusable Workflow Breaking Change

Prompt:

```text
A platform team changed a reusable workflow and 40 repos broke.
```

Must include:

- versioning/pinning
- changelog
- contract tests
- rollback old version
- migration window
- avoid breaking default behavior

---

### Scenario 11: Composite vs Reusable Workflow

Prompt:

```text
A team wants to share setup steps and entire CI policy. What do you choose?
```

Must include:

- composite action for step bundle
- reusable workflow for job/workflow policy
- inputs/secrets/outputs
- versioning

---

### Scenario 12: Organization Golden Path

Prompt:

```text
Design standard CI templates for 200 repos.
```

Must include:

- reusable workflows
- secure defaults
- least-privilege permissions
- documented inputs
- migration guide
- support channel
- adoption metrics

---

## 6. Docker Registry Release Scenarios

### Scenario 13: Image Tag Collision

Prompt:

```text
Two workflows pushed the same image tag with different content.
```

Must include:

- immutable tags/digests
- tag with SHA/version
- avoid `latest` for deploy
- deployment by digest
- registry audit

---

### Scenario 14: Vulnerability Scan Fails

Prompt:

```text
Container image scan finds critical CVE before release.
```

Must include:

- fail release gate
- triage base image/dependency
- risk acceptance process only if justified
- SBOM/provenance
- rebuild and rescan

---

### Scenario 15: Multi-Region Image Promotion

Prompt:

```text
Promote a tested image to multiple cloud registries.
```

Must include:

- build once
- sign/scan/attest
- copy/promote digest
- region-specific deploy jobs
- rollback by previous digest

---

## 7. Deployment IaC Migration Scenarios

### Scenario 16: Production Deploy Race

Prompt:

```text
Two production deployments ran at the same time and overwrote each other.
```

Must include:

- deployment concurrency group
- environment gate
- deployment queue
- release ordering
- audit/summary

---

### Scenario 17: OIDC Failure

Prompt:

```text
Deploy job cannot assume cloud role through OIDC.
```

Must include:

- `id-token: write`
- trust policy repo/ref/environment/audience
- environment name mismatch
- branch/tag restriction
- cloud IAM logs

---

### Scenario 18: Terraform Apply From Wrong Plan

Prompt:

```text
Terraform apply did not use the reviewed plan artifact.
```

Must include:

- split plan/apply
- upload immutable plan artifact
- environment approval before apply
- apply exact reviewed plan
- protect state backend

---

### Scenario 19: DB Migration Blocks Rollback

Prompt:

```text
App rollback fails because migration removed a column.
```

Must include:

- expand/contract pattern
- backward-compatible migrations
- separate destructive cleanup later
- migration approvals
- rollback test

---

## 8. Security Scenarios

### Scenario 20: `pull_request_target` Exposes Secrets

Prompt:

```text
Workflow uses `pull_request_target`, checks out fork PR code, and deploy secrets are available.
```

Must include:

- untrusted code in trusted context
- split PR validation from trusted deploy
- no checkout/execution of PR code in target context
- minimal permissions

---

### Scenario 21: Self-Hosted Runner Compromised

Prompt:

```text
A self-hosted runner used for deployments may be compromised.
```

Must include:

- isolate runner
- rotate credentials/tokens
- inspect recent runs
- revoke cloud sessions
- rebuild runner image
- move to ephemeral runners
- audit deployments

---

### Scenario 22: Cache Poisoning

Prompt:

```text
Trusted branch restored dependency cache produced by untrusted PR.
```

Must include:

- separate trust domains
- cache key strategy
- no deploy from cache
- rebuild release artifact from trusted ref
- review restore keys

---

### Scenario 23: Third-Party Action Compromised

Prompt:

```text
A popular action used in release workflows is compromised.
```

Must include:

- identify affected workflows/runs
- rotate exposed credentials
- pin/review/replace action
- audit artifacts/releases
- approved action catalog

---

## 9. Platform Operations Scenarios

### Scenario 24: Required Checks Outage

Prompt:

```text
Required PR checks cannot run because runner queue is stuck.
```

Must include:

- determine GitHub-hosted vs self-hosted issue
- runner group/label health
- queue time metrics
- temporary capacity/workaround
- communicate merge impact
- postmortem and capacity fix

---

### Scenario 25: CI Cost Spike

Prompt:

```text
GitHub Actions bill doubled this month.
```

Must include:

- cost by repo/team/workflow
- runner minutes and artifact/cache storage
- matrix expansion
- preview cleanup
- stale run cancellation
- scheduled workflow review

---

### Scenario 26: Flaky Required Checks

Prompt:

```text
Required checks fail randomly and developers rerun until green.
```

Must include:

- measure rerun pass rate
- identify flaky tests/infrastructure
- quarantine with owner/SLA
- keep required checks meaningful
- fix root cause

---

### Scenario 27: Organization Workflow Migration

Prompt:

```text
Move 300 repos from copy-paste YAML to reusable workflows.
```

Must include:

- inventory
- target golden paths
- pilot
- automated PRs
- migration docs
- adoption dashboard
- phased enforcement

---

## 10. Capstone Scenarios

### Scenario 28: MAANG CI/CD Platform

Prompt:

```text
Design GitHub Actions for a company with 500 repos, monorepos, cloud deploys, IaC, frontend previews, self-hosted runners, and strict security.
```

Must include:

- workflow architecture
- reusable workflows
- runner strategy
- OIDC/cloud auth
- branch/environments approvals
- security scanning
- artifact provenance
- platform observability
- cost controls
- support model

---

### Scenario 29: Incident Week

Prompt:

```text
In one week, CI is slow, a self-hosted runner is compromised, a deploy race breaks prod, and a reusable workflow breaks 50 repos. Build a response plan.
```

Must include:

- incident triage
- isolate runner and rotate credentials
- serialize deploys
- rollback reusable workflow
- communicate status
- add dashboards/runbooks
- long-term platform fixes

---

## 11. Completion Gate

You are ready when you can solve:

1. 5 CI workflow design scenarios.
2. 5 security/trust-boundary scenarios.
3. 4 deployment/release/IaC scenarios.
4. 4 runner/platform operations scenarios.
5. 3 monorepo/performance scenarios.
6. 2 full platform capstones.
