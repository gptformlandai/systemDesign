# 18. CI/CD: Terraform Pipelines, Atlantis, TFC

## The Problem With Manual terraform apply

```text
Without CI/CD:
  - Developers apply from laptops with different Terraform versions
  - No plan review before apply
  - No audit trail of who applied what when
  - "It works on my machine" state drift
  - No enforcement of company security policies

With CI/CD:
  - Consistent Terraform version across all applies
  - Plan output reviewed in PR (automation + human)
  - Full audit log: every apply tied to a commit and a person
  - Credentials never stored on developer machines
  - Policy gates (tflint, checkov, OPA) before apply
```

---

## GitHub Actions Workflow

```yaml
# .github/workflows/terraform.yml
name: Terraform

on:
  push:
    branches: [main]
    paths: ["environments/prod/**"]
  pull_request:
    branches: [main]
    paths: ["environments/prod/**"]

permissions:
  id-token: write    # for OIDC
  contents: read
  pull-requests: write  # to post plan comments

jobs:
  terraform:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: environments/prod

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789:role/TerraformGitHubRole
          aws-region: us-east-1

      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: "1.7.0"

      - name: Terraform Init
        run: terraform init

      - name: Terraform Validate
        run: terraform validate

      - name: Run tflint
        uses: terraform-linters/setup-tflint@v4
      - run: tflint --recursive

      - name: Run Checkov (security scan)
        uses: bridgecrewio/checkov-action@v12
        with:
          directory: environments/prod
          framework: terraform

      - name: Terraform Plan
        id: plan
        run: |
          terraform plan -no-color -out=tfplan 2>&1 | tee plan_output.txt
          echo "exitcode=$?" >> $GITHUB_OUTPUT
        continue-on-error: true  # don't fail workflow; post result to PR instead

      - name: Post Plan To PR
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const output = require('fs').readFileSync('environments/prod/plan_output.txt', 'utf8');
            const body = `## Terraform Plan\n\`\`\`\n${output.substring(0, 65000)}\n\`\`\``;
            github.rest.issues.createComment({
              owner: context.repo.owner,
              repo: context.repo.repo,
              issue_number: context.issue.number,
              body
            });

      - name: Terraform Apply
        if: github.ref == 'refs/heads/main' && github.event_name == 'push'
        run: terraform apply -auto-approve tfplan
```

---

## Saving And Applying Plans Safely

```bash
# In CI: save plan to binary file
terraform plan -out=tfplan -no-color 2>&1 | tee plan_output.txt

# Show plan as human-readable (for PR comments)
terraform show -no-color tfplan

# Show plan as JSON (for parsing/policy checks)
terraform show -json tfplan > tfplan.json

# Apply the EXACT plan that was reviewed (no re-plan)
terraform apply tfplan
# (no -auto-approve needed when using a saved plan file)
```

---

## Branch Strategy

```text
feature/my-change  →  PR  →  Plan only (review)
                              ↓ merge
main               →  push  →  Plan + Apply (prod)
                              
Separate workflows per environment:
  PR to main   →  plan dev + plan staging
  Merge to main →  apply dev → apply staging (after manual gate) → apply prod
```

---

## Atlantis

Atlantis is an open-source Terraform automation tool that responds to PR comments to run plan and apply.

```yaml
# atlantis.yaml (repository root)
version: 3
automerge: false
delete_source_branch_on_merge: false

projects:
  - name: prod-network
    dir: environments/prod/network
    workspace: default
    terraform_version: v1.7.0
    autoplan:
      enabled: true
      when_modified: ["*.tf", "*.tfvars", "../../modules/**/*.tf"]
    apply_requirements: [approved, mergeable]

  - name: prod-app
    dir: environments/prod/app
    workspace: default
    terraform_version: v1.7.0
    autoplan:
      enabled: true
      when_modified: ["*.tf", "*.tfvars"]
    apply_requirements: [approved]
```

### Atlantis PR Workflow

```text
1. Developer opens PR
2. Atlantis: autoplan triggered → runs terraform plan
3. Plan output posted as PR comment
4. Reviewer reviews code + plan
5. Reviewer approves PR
6. Developer comments: atlantis apply
7. Atlantis: runs terraform apply → posts apply output to PR
8. Developer merges PR

Commands in PR comments:
  atlantis plan        → trigger plan manually
  atlantis apply       → trigger apply after approval
  atlantis plan -target=module.eks  → targeted plan
  atlantis unlock      → release Atlantis lock on the PR
```

---

## Terraform Cloud (TFC) VCS Workflow

```text
Setup:
  1. Connect TFC workspace to GitHub repo
  2. Set working directory (e.g., environments/prod)
  3. Configure variables (Terraform vars + env vars)
  4. Set Trigger: only run on changes to working directory

Workflow:
  PR opened  → TFC runs speculative plan → posts status check
  PR merged  → TFC runs confirmed apply (or requires manual approval)

Run confirmation options:
  Auto-apply: applies immediately after plan succeeds (dev environments)
  Manual apply: requires a human to click "Confirm & Apply" in TFC UI (prod)
```

---

## Linting And Static Analysis

```bash
# tflint: Terraform linter (provider-aware rules)
tflint --init
tflint --recursive

# checkov: security and compliance scanner
checkov -d environments/prod --framework terraform

# trivy: vulnerability scanner including Terraform misconfigs
trivy config environments/prod

# terrascan: policy-as-code scanner
terrascan scan -i terraform -d environments/prod

# infracost: cost estimation
infracost breakdown --path environments/prod
```

---

## Handling Drift In CI/CD

```bash
# Scheduled plan to detect drift (run daily in CI)
terraform plan -refresh=true -detailed-exitcode
# Exit codes:
#   0 = no changes (no drift)
#   1 = error
#   2 = changes needed (drift detected)

# If exit code 2 in scheduled run: alert the team
```

---

## Interview Sound Bite

The gold-standard Terraform CI/CD pipeline: PR opened → `terraform plan` runs automatically → plan output posted as PR comment → human reviews both code and plan → PR merged → `terraform apply tfplan` applies the EXACT saved plan. Using `plan -out=tfplan` and `apply tfplan` guarantees no race between review and apply. OIDC credentials (GitHub OIDC → AWS STS → temporary credentials) replace stored secrets. Atlantis adds PR comment commands (`atlantis apply`) for GitOps without a SaaS dependency. TFC VCS integration does this natively. Always run `tflint` and `checkov` in CI to catch lint issues and security misconfigs before apply.
