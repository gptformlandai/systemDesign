# GitHub Actions Infrastructure as Code and Database Migration Gold Sheet

> Goal: design safe Terraform/IaC and database migration workflows with plans, approvals, state locking, drift detection, policy checks, rollback thinking, and production guardrails.

---

## 0. How To Read This

Beginner focus:

- Terraform plan
- Terraform apply
- migration script
- approval

Intermediate focus:

- remote state
- state locking
- PR plan comment
- environment secrets
- drift detection

Senior focus:

- policy as code
- multi-environment IaC
- destructive change review
- database expand/contract migrations
- rollback limitations
- compliance/audit

---

# Topic 1: IaC and Database Migration Pipelines

---

## 1. Intuition

Infrastructure and database changes are production changes.

Treat them like code, but with extra caution:

```text
show the plan
review the blast radius
approve
apply once
verify
recover if needed
```

Beginner explanation:

GitHub Actions can run Terraform plans on pull requests and apply approved infrastructure changes after merge. It can also run database migrations carefully during deployment.

---

## 2. Definition

- Definition: IaC and database migration workflows automate infrastructure and schema changes through reviewable plans, approval gates, controlled execution, and verification.
- Category: DevOps / platform delivery
- Core idea: infrastructure and schema changes must be predictable, auditable, and reversible where possible.

---

## 3. Why It Exists

Without controlled workflows:

- infrastructure changes are manual
- Terraform drift goes unnoticed
- destructive changes are missed
- database migrations break production
- state corruption can occur
- approvals are informal
- rollback is unclear

IaC pipelines make changes visible before they happen.

---

## 4. Reality

Common tools:

- Terraform
- OpenTofu
- Pulumi
- CloudFormation
- Helm
- Liquibase
- Flyway
- Alembic
- Prisma migrations

Common workflows:

- PR plan
- merge apply
- manual production apply
- drift detection
- policy checks
- database migration before/after deploy

---

## 5. How It Works

### Part A: Terraform PR Flow

```text
pull_request
-> terraform fmt
-> terraform validate
-> terraform plan
-> comment plan summary
-> reviewer checks blast radius
```

### Part B: Terraform Apply Flow

```text
merge to main or manual trigger
-> authenticate with OIDC
-> select environment
-> acquire state lock
-> terraform plan
-> approval if prod
-> terraform apply
-> output summary
```

### Part C: Remote State and Locking

Production IaC needs:

- remote state
- state locking
- restricted access
- encryption
- backup/versioning

Do not store state files as workflow artifacts.

### Part D: Terraform Plan Workflow

```yaml
name: Terraform Plan

on:
  pull_request:
    paths:
      - "infra/**"

permissions:
  contents: read
  pull-requests: write
  id-token: write

jobs:
  plan:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: infra
    steps:
      - uses: actions/checkout@v4
      - run: terraform fmt -check
      - run: terraform init
      - run: terraform validate
      - run: terraform plan -no-color
```

### Part E: Production Apply

```yaml
jobs:
  apply:
    runs-on: ubuntu-latest
    environment: production
    concurrency:
      group: terraform-production
      cancel-in-progress: false
    steps:
      - uses: actions/checkout@v4
      - run: terraform init
      - run: terraform plan -out=tfplan
      - run: terraform apply -auto-approve tfplan
```

Senior note:

Production apply should be serialized. Never let two applies fight over state.

### Part F: Policy as Code

Policy checks can block:

- public S3 buckets
- wide-open security groups
- unencrypted databases
- missing tags
- oversized instances
- forbidden regions

Tools:

- OPA/Conftest
- Checkov
- tfsec
- Sentinel
- cloud-native policy tools

### Part G: Drift Detection

Drift means real infrastructure differs from desired state.

Scheduled workflow:

```text
nightly terraform plan
-> if changes detected
-> alert platform/team
```

Do not auto-apply drift blindly.

### Part H: Database Migration Flow

Migrations should be:

- versioned
- reviewed
- tested
- backward compatible when possible
- run once
- observable

Common deployment order:

```text
expand schema
deploy app compatible with old and new
backfill data
switch reads/writes
contract old schema later
```

### Part I: Expand/Contract Pattern

Bad:

```text
rename column and deploy app at same time
```

Better:

```text
1. add new column
2. deploy app writing both
3. backfill
4. deploy app reading new
5. remove old column later
```

### Part J: Migration Rollback

Database rollback is not always possible.

Safer approach:

- forward fix
- backups
- reversible migrations where practical
- avoid destructive migration in same deploy
- test restore process

---

## 6. What Problem It Solves

- Primary problem solved: safe, reviewable infrastructure and schema change management
- Secondary benefits: audit, drift visibility, policy enforcement, release confidence
- Systems impact: reduces outage risk from infra and DB changes

---

## 7. When To Rely On It

Use IaC workflows when:

- cloud infrastructure is code-managed
- multiple environments exist
- approvals are needed
- drift matters
- compliance requires audit

Use migration workflows when:

- deployment changes DB schema
- multiple app versions may run at once
- rollback matters
- zero downtime is required

---

## 8. When Not To Automate Blindly

Do not auto-apply:

- destructive infra changes
- production DB schema changes
- drift plans without review
- changes with unknown blast radius

Automation should add safety, not remove judgment.

---

## 9. Pros and Cons

| Practice | Pros | Cons |
|---|---|---|
| PR plan | review before change | plan can expose sensitive values if careless |
| Apply after approval | controlled prod changes | slower |
| Drift detection | catches manual changes | can be noisy |
| Policy as code | prevents bad patterns | policy maintenance |
| Expand/contract DB | zero-downtime friendly | more steps |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- More approvals:
  Safer prod, slower change.
- Auto-apply:
  Faster, but dangerous for destructive changes.
- Strict policy:
  Better compliance, more false positives.
- DB backward compatibility:
  Safer deploys, more migration stages.

### Common Mistakes

- Mistake: "Terraform apply on every PR."
  Why it is wrong: unmerged code can change real infrastructure.
  Better approach: plan on PR, apply after merge/approval.

- Mistake: "No state locking."
  Why it is wrong: concurrent applies can corrupt state.
  Better approach: use remote backend with locking.

- Mistake: "Drop column during same deploy."
  Why it is wrong: old app version may still use it.
  Better approach: expand/contract migration.

- Mistake: "Treat DB rollback like app rollback."
  Why it is wrong: data changes may not be reversible.
  Better approach: design forward-compatible migrations.

---

## 11. Key Numbers

Rules of thumb:

- production Terraform applies should be serialized
- DB migrations should run once per release
- destructive changes need explicit review
- drift detection often runs daily or weekly
- migration rollback should be tested before critical releases

---

## 12. Failure Modes

### State Lock Stuck

Cause:

- failed apply
- interrupted workflow

Fix:

- inspect backend lock
- verify no active apply
- unlock carefully with owner approval

### Plan Leaks Secret

Cause:

- sensitive output not marked
- plan posted publicly

Fix:

- mark sensitive values
- restrict PR plan comments
- avoid exposing plan to untrusted forks

### Migration Breaks Old App

Cause:

- destructive schema change

Fix:

- restore if possible
- forward fix
- use expand/contract next time

### Concurrent Applies

Cause:

- no concurrency group
- no state lock

Fix:

- remote locking
- GitHub concurrency

---

## 13. Scenario

- Product / system: Terraform-managed AWS infrastructure and Spring Boot database migrations
- Why this concept fits: both infra and schema changes can break production
- What would go wrong without it: unreviewed destructive changes and non-backward-compatible migrations

---

## 14. Code Sample

Terraform production apply skeleton:

```yaml
name: Terraform Apply

on:
  workflow_dispatch:
    inputs:
      environment:
        required: true
        type: choice
        options: [stage, production]

permissions:
  contents: read
  id-token: write

concurrency:
  group: terraform-${{ inputs.environment }}
  cancel-in-progress: false

jobs:
  apply:
    runs-on: ubuntu-latest
    environment: ${{ inputs.environment }}
    defaults:
      run:
        working-directory: infra
    steps:
      - uses: actions/checkout@v4
      - run: echo "Authenticate to cloud with OIDC"
      - run: terraform init
      - run: terraform plan -out=tfplan
      - run: terraform apply -auto-approve tfplan
```

---

## 15. Mini Program / Simulation

Migration compatibility:

```python
def migration_safe(old_app_reads_old_column, migration_drops_old_column):
    if old_app_reads_old_column and migration_drops_old_column:
        return "unsafe: old app can break"
    return "safer"

print(migration_safe(True, True))
print(migration_safe(True, False))
```

---

## 16. Practical Question

> How would you design Terraform and database migration workflows in GitHub Actions?

---

## 17. Strong Answer

For Terraform, I would run `fmt`, `validate`, and `plan` on pull requests, then apply only after merge or manual approval. Production apply would use a GitHub environment, OIDC cloud authentication, remote state with locking, and a concurrency group so only one apply runs per environment.

For databases, I would use versioned migrations and follow expand/contract for zero-downtime changes. Destructive migrations would be separated from app deploys and require explicit review. I would test migrations in CI against a real database container and keep rollback or forward-fix plans ready.

---

## 18. Revision Notes

- One-line summary: IaC and DB workflows must show the plan, control apply, and protect state/data.
- Three keywords: plan, lock, expand/contract
- One interview trap: database rollback is not as simple as app rollback.
- One memory trick: infra changes need plan; DB changes need compatibility.

---

## 19. Official Source Notes

- Environments: <https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment>
- OpenID Connect: <https://docs.github.com/en/actions/concepts/security/openid-connect>
- Workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>

