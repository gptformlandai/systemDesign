# GitHub Actions Reusable Workflows and Custom Actions Gold Sheet

> Goal: understand when to use reusable workflows, composite actions, JavaScript actions, Docker actions, and organization-level templates for scalable CI/CD platforms.

---

## 0. How To Read This

Beginner focus:

- reusable workflow
- custom action
- composite action
- workflow template

Intermediate focus:

- `workflow_call`
- inputs
- secrets
- outputs
- versioning
- action metadata

Senior focus:

- platform workflow governance
- CI/CD standardization
- action supply-chain risk
- breaking changes
- multi-repo rollout
- template ownership

---

# Topic 1: Reusable Workflows and Custom Actions

---

## 1. Intuition

Reusable automation prevents every repository from inventing its own CI/CD.

Think:

```text
reusable workflow = shared pipeline
composite action = shared set of steps
JavaScript/Docker action = packaged custom tool
workflow template = starter blueprint
```

Beginner explanation:

Reusable workflows let one repository call a standard workflow from another repository. Custom actions package repeated logic so teams do not copy/paste the same steps everywhere.

---

## 2. Definition

- Definition: Reusable workflows and custom actions are GitHub Actions mechanisms for sharing automation across repositories and teams.
- Category: CI/CD platform reuse
- Core idea: standardize common CI/CD behavior while allowing controlled inputs.

---

## 3. Why It Exists

Without reuse:

- every repo copies YAML
- security standards drift
- language setup differs
- deployment patterns fork
- upgrades are painful
- platform team cannot enforce consistency

Reuse makes CI/CD maintainable at organization scale.

---

## 4. Reality

Used for:

- standard Java CI
- standard frontend CI
- Docker build and publish
- Terraform plan/apply
- security scans
- deployment workflows
- release workflows
- internal setup logic

Platform teams usually own reusable workflows; app teams consume them.

---

## 5. How It Works

### Part A: Reusable Workflow

Reusable workflow file:

```yaml
name: Reusable Java CI

on:
  workflow_call:
    inputs:
      working-directory:
        type: string
        required: true
      java-version:
        type: string
        required: false
        default: "21"

jobs:
  test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ${{ inputs.working-directory }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ inputs.java-version }}
          cache: maven
      - run: ./mvnw -B verify
```

Caller:

```yaml
jobs:
  ci:
    uses: org/platform/.github/workflows/java-ci.yml@v1
    with:
      working-directory: services/order
      java-version: "21"
```

### Part B: Inputs, Secrets, Outputs

Reusable workflows can define:

- inputs
- secrets
- outputs

Example secret passing:

```yaml
jobs:
  deploy:
    uses: org/platform/.github/workflows/deploy.yml@v1
    secrets:
      deploy-token: ${{ secrets.DEPLOY_TOKEN }}
```

Do not blindly use `secrets: inherit` for high-risk workflows. Pass only what is needed.

### Part C: Composite Action

Composite action metadata:

```yaml
name: Setup Project
description: Install tools and dependencies
runs:
  using: composite
  steps:
    - shell: bash
      run: echo "setup logic"
```

Use composite actions for:

- repeated step sequences
- local setup
- simple shell-based helpers
- internal consistency

### Part D: JavaScript Action

Use JavaScript actions when:

- logic is more complex
- GitHub API integration is needed
- rich input/output handling is needed
- cross-platform behavior matters

Trade-off:

- requires Node packaging/building
- dependency security matters

### Part E: Docker Action

Use Docker actions when:

- custom runtime/tools are needed
- dependencies are complex
- environment must be packaged

Trade-off:

- slower startup
- Linux-container focused
- image security scanning needed

### Part F: Reusable Workflow vs Composite Action

| Need | Best Fit |
|---|---|
| Reuse entire CI job/pipeline | reusable workflow |
| Reuse a few steps | composite action |
| Needs environment approvals | reusable workflow |
| Needs job permissions | reusable workflow |
| Needs simple shell helper | composite action |
| Needs GitHub API logic | JavaScript action |
| Needs packaged tool runtime | Docker action |

### Part G: Versioning

Call reusable workflows/actions with stable references:

```yaml
uses: org/platform/.github/workflows/java-ci.yml@v1
```

Options:

- tag: stable but mutable if not protected
- SHA: strongest integrity
- branch: convenient but risky for production

For production-critical workflows, use protected tags or SHAs.

### Part H: Breaking Changes

Platform workflow changes can break many repos.

Safe rollout:

1. Create `v2`.
2. Keep `v1` stable.
3. Migrate pilot repos.
4. Publish migration guide.
5. Track adoption.
6. Deprecate old version later.

### Part I: Organization Workflow Templates

Workflow templates are starter files teams can create from.

Use for:

- standard repository onboarding
- recommended CI patterns
- common deploy shapes

Reusable workflows are better for enforcing ongoing consistency. Templates are better for bootstrapping.

### Part J: Governance

Platform team should define:

- owners
- versioning policy
- security review
- update cadence
- support channel
- compatibility guarantees
- deprecation policy

---

## 6. What Problem It Solves

- Primary problem solved: CI/CD standardization across many repositories
- Secondary benefits: security consistency, lower maintenance, faster onboarding
- Systems impact: turns CI/CD into a platform instead of scattered YAML

---

## 7. When To Rely On It

Use reusable workflows when:

- many repos share the same CI/CD pattern
- security standards must be consistent
- deployment logic should be centrally governed
- platform team owns CI/CD standards

Use composite actions when:

- repeated steps appear inside many workflows
- logic is simple
- no job-level orchestration is needed

Use custom JS/Docker actions when:

- behavior is complex enough to package as a tool

---

## 8. When Not To Use It

Avoid over-abstracting when:

- only one repo needs the logic
- every service has truly unique requirements
- abstraction hides important deployment behavior
- debugging becomes harder than copying a few clear steps

Rule:

> Reuse should make standards clearer, not make pipelines mysterious.

---

## 9. Pros and Cons

| Approach | Pros | Cons |
|---|---|---|
| Reusable workflow | standardizes whole pipeline | version migration needed |
| Composite action | simple step reuse | limited job-level control |
| JavaScript action | powerful GitHub API logic | dependency/package security |
| Docker action | packaged runtime | slower and image maintenance |
| Template | easy onboarding | can drift after copy |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Centralization:
  Consistent and secure, but teams may need flexibility.
- Version pinning:
  Stable, but upgrades require coordination.
- `secrets: inherit`:
  Convenient, but can pass too much.
- Templates:
  Easy start, but not ongoing enforcement.

### Common Mistakes

- Mistake: "Use branch `main` for shared workflow."
  Why it is wrong: a platform change can break all consumers instantly.
  Better approach: version with protected tags or SHA.

- Mistake: "Reusable workflow hides all details."
  Why it is wrong: teams cannot debug.
  Better approach: document inputs, outputs, permissions, and failure modes.

- Mistake: "Pass all secrets."
  Why it is wrong: expands blast radius.
  Better approach: pass only required secrets.

- Mistake: "Composite action for deployment approval."
  Why it is wrong: approvals/environments are job/workflow-level concepts.
  Better approach: reusable workflow.

---

## 11. Key Numbers

Useful governance targets:

- use semantic versions for shared workflows/actions
- keep old major versions during migration windows
- track which repos consume each reusable workflow
- review production-critical reusable workflows like product code

---

## 12. Failure Modes

### Shared Workflow Breaks Many Repos

Cause:

- unversioned workflow reference
- breaking change under same tag/branch

Fix:

- rollback tag
- publish patched version
- use versioned releases

### Secret Exposure Through Reuse

Cause:

- broad `secrets: inherit`
- reusable workflow logs sensitive data

Fix:

- pass explicit secrets
- audit logging
- rotate if exposed

### Debugging Becomes Hard

Cause:

- abstraction hides commands
- poor output messages

Fix:

- document workflow contract
- print safe summaries
- expose useful outputs

---

## 13. Scenario

- Product / system: company with 200 Spring Boot services
- Why this concept fits: every repo needs consistent CI, image build, scanning, and deployment
- What would go wrong without it: copy/paste drift and security gaps across services

---

## 14. Code Sample

Reusable Docker build workflow:

```yaml
name: Reusable Docker Build

on:
  workflow_call:
    inputs:
      image-name:
        required: true
        type: string
      context:
        required: false
        type: string
        default: "."
    outputs:
      image:
        value: ${{ jobs.build.outputs.image }}

permissions:
  contents: read
  packages: write

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image: ${{ steps.meta.outputs.image }}
    steps:
      - uses: actions/checkout@v4
      - id: meta
        run: echo "image=ghcr.io/${{ github.repository_owner }}/${{ inputs.image-name }}:sha-${{ github.sha }}" >> "$GITHUB_OUTPUT"
      - run: echo "Build and push ${{ steps.meta.outputs.image }}"
```

---

## 15. Mini Program / Simulation

Version migration:

```python
repos = {"service-a": "v1", "service-b": "v1", "service-c": "v2"}

for repo, version in repos.items():
    if version == "v1":
        print(repo, "needs migration")
```

---

## 16. Practical Question

> How would you standardize GitHub Actions across 200 repositories?

---

## 17. Strong Answer

I would create platform-owned reusable workflows for common patterns: backend CI, frontend CI, Docker build, security scan, Terraform plan/apply, and deployment. Repositories would call those workflows using versioned references and pass inputs such as service path, language version, image name, and environment.

For small repeated step logic, I would use composite actions. For complex GitHub API behavior, I would use JavaScript actions. Production-critical shared workflows would be versioned, documented, reviewed, and migrated with major versions rather than changing behavior under every repo.

I would avoid broad secret inheritance and keep permissions least privilege inside the reusable workflows.

---

## 18. Revision Notes

- One-line summary: Reusable workflows standardize pipelines; custom actions package repeated logic.
- Three keywords: `workflow_call`, composite, versioning
- One interview trap: using `main` for shared production workflows can break every repo at once.
- One memory trick: workflow for pipeline, composite for steps, JS/Docker for tools.

---

## 19. Official Source Notes

- Reusing workflows: <https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows>
- Creating actions: <https://docs.github.com/en/actions/sharing-automations/creating-actions>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>

