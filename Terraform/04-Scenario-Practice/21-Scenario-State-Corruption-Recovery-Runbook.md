# 21. Scenario: State Corruption Recovery Runbook

## State Issue Categories

```text
Category 1: Stale lock (apply crashed mid-run)
  Symptom: "Error locking state: state is locked by another process"
  Action:  terraform force-unlock <LOCK_ID>

Category 2: State drift (resource changed outside Terraform)
  Symptom: Plan wants to update/destroy things you didn't touch
  Action:  terraform plan -refresh-only → investigate → apply -refresh-only

Category 3: Partial apply (apply failed halfway)
  Symptom: Some resources created, some not; state inconsistent
  Action:  Diagnose with plan → targeted apply to finish

Category 4: State corruption (invalid JSON, corrupt file)
  Symptom: "Failed to read state" or JSON parse errors
  Action:  Restore from S3 versioned backup

Category 5: Accidentally deleted resource from state
  Symptom: Plan wants to recreate a resource that already exists
  Action:  terraform import or import block
```

---

## Runbook 1: Stale State Lock

### Symptoms

```text
│ Error: Error locking state: Error acquiring the state lock: ConditionalCheckFailedException
│
│ Lock Info:
│   ID:        abc123-def456-...
│   Path:      s3://my-bucket/prod/terraform.tfstate
│   Operation: OperationTypeApply
│   Who:       user@machine
│   Version:   1.6.0
│   Created:   2024-01-15 10:00:00 UTC
│   Info:
```

### Resolution Steps

```bash
# Step 1: Verify no apply is actually running
# Check CI/CD pipelines, check with teammates
# Look at Created timestamp — is it recent?

# Step 2: Force-unlock using the Lock ID from the error message
terraform force-unlock abc123-def456-...

# Step 3: Confirm lock is released
terraform plan  # should work without lock error now

# Step 4: Verify state is intact
terraform state list
terraform plan   # should show expected state
```

```text
CAUTION:
  Only force-unlock if you are CERTAIN no apply is running.
  If an apply IS running and you unlock it, two applies can run
  simultaneously → state corruption.

If uncertain: wait 10-15 minutes, check CI logs, ask team.
```

---

## Runbook 2: State Drift Recovery

### Symptoms

```text
terraform plan shows unexpected changes:
  ~ aws_instance.web   tags updated (out-of-band tag change)
  - aws_security_group.extra   (someone deleted this manually)
  + aws_route53_record.new     (state refresh discovered it was deleted)
```

### Resolution Steps

```bash
# Step 1: Run refresh-only plan to see what changed
terraform plan -refresh-only -no-color 2>&1 | tee drift-report.txt

# Step 2: Review drift report
# Decide for each drift item:
#   a) Accept it: run terraform apply -refresh-only (update state to match reality)
#   b) Revert it: run terraform apply to re-apply the desired configuration
#   c) Ignore it: add ignore_changes to the resource lifecycle block

# Step 3a: Accept drift (state reflects reality)
terraform apply -refresh-only

# Step 3b: Revert drift (re-apply HCL config)
terraform apply   # restores deleted resources, reverts unwanted changes
```

---

## Runbook 3: Partial Apply Recovery

### Symptoms

```text
Apply failed mid-run with an error.
Some resources were created successfully, others were not.
Running plan shows inconsistent state.
```

### Resolution Steps

```bash
# Step 1: Examine what state shows
terraform state list

# Step 2: Run full plan to see what's missing / broken
terraform plan -no-color 2>&1 | tee recovery-plan.txt

# Step 3: Fix the root cause (IAM permission error, quota exceeded, etc.)

# Step 4: Re-apply (Terraform is idempotent — already-created resources are skipped)
terraform apply

# If specific resources are blocking progress:
terraform apply -target=aws_iam_role.new_role   # fix dependency
terraform apply                                   # full apply
```

---

## Runbook 4: State File Corruption

### Symptoms

```text
Error: Failed to load root module
Error reading state file: invalid character 'x' looking for beginning of value
Error loading state: error reading state: EOF
```

### Resolution Steps

```bash
# Step 1: Verify the state file is corrupted
terraform state pull > /tmp/current-state.json
python3 -m json.tool /tmp/current-state.json  # will fail if invalid JSON

# Step 2: List S3 versions of the state file
aws s3api list-object-versions \
  --bucket mycompany-terraform-state \
  --prefix environments/prod/terraform.tfstate \
  --query 'Versions[*].[VersionId,LastModified]' \
  --output table

# Step 3: Download the last known good version
aws s3api get-object \
  --bucket mycompany-terraform-state \
  --key environments/prod/terraform.tfstate \
  --version-id <GOOD_VERSION_ID> \
  /tmp/state-recovery.json

# Step 4: Validate the recovered state
python3 -m json.tool /tmp/state-recovery.json

# Step 5: Push the recovered state
terraform state push /tmp/state-recovery.json

# Step 6: Verify
terraform state list
terraform plan  # should show minimal/no unexpected changes
```

---

## Runbook 5: Resource Accidentally Deleted From State

### Symptoms

```text
terraform state list  ← resource is missing
terraform plan        ← shows "will create" for a resource that already exists
```

### Resolution Steps

```bash
# Method 1: Import the existing resource (CLI method)
terraform import aws_instance.web i-0abc123def456789

# Method 2: Import block (declarative, TF 1.5+)
# Add to import.tf:
# import {
#   to = aws_instance.web
#   id = "i-0abc123def456789"
# }
# Then: terraform apply

# After import: run plan to confirm no changes needed
terraform plan   # should show "No changes" if HCL matches reality
```

---

## Runbook 6: Wrong State After Workspace Switch

### Symptoms

```text
Applied in prod workspace but state shows dev resources.
Or: applied with wrong backend configuration.
```

### Resolution Steps

```bash
# Step 1: Check current workspace
terraform workspace show

# Step 2: Check which state backend you're pointing to
terraform state pull | python3 -m json.tool | grep '"serial"'

# Step 3: If in wrong workspace, switch
terraform workspace select prod

# Step 4: Verify state looks correct
terraform state list
terraform plan
```

---

## Prevention Checklist

```text
Prevent state corruption:
  ✓ Remote backend with versioning (S3 versioning enabled)
  ✓ State locking (DynamoDB)
  ✓ Never edit state manually
  ✓ Always backup before state surgery: terraform state pull > backup.json
  ✓ One apply at a time (CI/CD pipeline, not multiple developers)
  ✓ Encryption at rest (state contains secrets)

Prevent stale locks:
  ✓ Use CI/CD (not long-running laptop applies that can be interrupted)
  ✓ Set apply timeouts in CI/CD pipelines

Prevent drift:
  ✓ All infrastructure changes via Terraform (no manual console changes)
  ✓ Daily scheduled drift detection plan in CI/CD
  ✓ lifecycle { ignore_changes } for expected out-of-band changes (auto-scaling, tags)
```

---

## Interview Sound Bite

State recovery has a clear hierarchy: stale lock → `terraform force-unlock <LOCK_ID>` (only when NO apply is running); state drift → `terraform plan -refresh-only` to see the diff, then decide to accept (`apply -refresh-only`) or revert (`apply`); partial apply → just re-run `terraform apply` (Terraform is idempotent); state corruption → restore from S3 versioned backup via `aws s3api get-object --version-id` then `terraform state push`. Prevention is the real answer: remote state with S3 versioning, DynamoDB locking, CI/CD-only applies (no laptop applies), and never manually editing the state file.
