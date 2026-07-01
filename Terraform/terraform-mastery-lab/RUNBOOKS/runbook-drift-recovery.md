# Runbook: State Drift Recovery

**Use when:** `terraform plan` shows unexpected changes — resources that were modified, deleted, or created outside Terraform.

---

## What Is Drift?

State drift occurs when real infrastructure diverges from what Terraform state believes exists. Common causes:
- Manual changes via AWS Console
- Auto Scaling Group replacing instances
- Automated tooling modifying tags
- Another team managing the same resources
- Resources deleted outside Terraform

---

## Step 1: Detect Drift

```bash
# Run a refresh-only plan to see what changed in reality vs state
terraform plan -refresh-only -no-color 2>&1 | tee drift-report.txt

# Check exit code
# Exit 0: no drift
# Exit 1: error
# Exit 2: drift detected (use -detailed-exitcode flag)
terraform plan -refresh-only -detailed-exitcode
echo "Exit code: $?"
```

---

## Step 2: Categorize Drift

Review `drift-report.txt`. For each drifted resource, decide:

```text
Category A: Expected out-of-band change
  Examples: tag updates, auto-scaling capacity changes, minor version upgrades
  Action: Accept drift — run `terraform apply -refresh-only`
           AND add `lifecycle { ignore_changes = [attr] }` to prevent future drift noise

Category B: Unexpected change that should be reverted
  Examples: security group rules changed manually, RDS parameter group changed
  Action: Revert — run `terraform apply` to re-apply the desired HCL config

Category C: Resource deleted outside Terraform
  Examples: EC2 instance terminated manually
  Action: If Terraform should recreate it: run `terraform apply`
           If it should stay deleted: run `terraform state rm` + remove from HCL

Category D: Resource created outside Terraform that should be adopted
  Examples: RDS snapshot restored manually as new instance
  Action: `terraform import` to bring it under management
```

---

## Step 3a: Accept Drift (Update State To Match Reality)

```bash
# Preview which state changes will be made (no resource changes)
terraform plan -refresh-only

# Apply the refresh (updates state file only — no AWS API changes to resources)
terraform apply -refresh-only
```

Then add `ignore_changes` to prevent this from showing as drift in the future:

```hcl
resource "aws_instance" "web" {
  # ...
  lifecycle {
    ignore_changes = [
      tags["LastModified"],   # ignore automated tag updates
      user_data,              # ignore changes after initial bootstrap
    ]
  }
}
```

---

## Step 3b: Revert Drift (Re-Apply HCL Config)

```bash
# Preview what will be changed to match HCL config
terraform plan -no-color 2>&1 | tee revert-plan.txt

# Review: confirm only the expected resources will be changed
grep -E "will be updated|will be destroyed|will be created" revert-plan.txt

# Apply (restores desired state)
terraform apply
```

---

## Step 3c: Remove Deleted Resource From State

```bash
# If a resource was deleted externally and should NOT be recreated:
# 1. Remove from HCL (delete the resource block)
# 2. Remove from state
terraform state rm aws_instance.old_worker

# Plan should show no changes now
terraform plan
```

---

## Step 3d: Import Adopted Resource

```bash
# If a resource was created externally and should be managed by Terraform:
# 1. Write the resource block in HCL
# 2. Import
terraform import aws_db_instance.restored db-restored-instance-id

# Plan to verify HCL matches reality
terraform plan   # should show "No changes" after adjusting HCL
```

---

## Prevention

```text
- Enforce "no console changes" policy: all infra changes via Terraform PRs
- Scheduled drift detection: daily CI/CD job running terraform plan -refresh-only -detailed-exitcode
- Alert team if exit code 2 (drift detected)
- Use lifecycle { ignore_changes } for expected automated changes (auto-scaling, tags)
- Enable AWS Config rules to detect and alert on out-of-band changes
```

---

## Scheduled Drift Detection (GitHub Actions)

```yaml
name: Drift Detection
on:
  schedule:
    - cron: '0 6 * * *'  # daily at 6am UTC

jobs:
  drift:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ vars.TERRAFORM_ROLE_ARN }}
          aws-region: us-east-1
      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: "1.7.0"
      - run: terraform init
        working-directory: environments/prod
      - name: Drift check
        run: |
          set +e
          terraform plan -refresh-only -detailed-exitcode -no-color
          exit_code=$?
          if [ $exit_code -eq 2 ]; then
            echo "DRIFT DETECTED in production!"
            exit 1
          fi
        working-directory: environments/prod
```
