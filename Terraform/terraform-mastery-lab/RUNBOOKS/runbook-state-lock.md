# Runbook: State Lock Recovery

**Use when:** `terraform apply` or `terraform plan` fails with a state lock error.

---

## Symptoms

```
Error: Error locking state: Error acquiring the state lock: ConditionalCheckFailedException

Lock Info:
  ID:        abc123-def456-ghi789
  Path:      s3://my-bucket/prod/terraform.tfstate
  Operation: OperationTypeApply
  Who:       user@hostname
  Version:   1.6.0
  Created:   2024-01-15 10:00:00 UTC
```

---

## Decision Tree

```text
Is the lock recent (< 5 min old)?
  → YES: An apply may be running. Wait 5-10 min. Check CI/CD pipeline status.
  → NO: Likely stale. Proceed to investigation.

Is a CI/CD pipeline currently running?
  → YES: Wait for it to complete or cancel it first.
  → NO: Lock is stale. Safe to force-unlock.

Is a teammate running terraform apply locally?
  → YES: Ask them to wait; do NOT force-unlock.
  → NO: Force-unlock is safe.
```

---

## Resolution Steps

### Step 1: Verify No Apply Is Running

```bash
# Check CI/CD pipeline status (GitHub Actions, TFC, Atlantis)
# Check with teammates
# Check DynamoDB lock table directly
aws dynamodb get-item \
  --table-name terraform-state-lock \
  --key '{"LockID": {"S": "s3://my-bucket/prod/terraform.tfstate"}}' \
  --region us-east-1
```

### Step 2: Force-Unlock

```bash
# Use the Lock ID from the error message (NOT from the command below)
terraform force-unlock abc123-def456-ghi789
```

Output:
```text
Do you really want to force-unlock?
  Terraform will remove the lock on the remote state.
  This will allow local Terraform commands to modify this state, even if another
  command is currently modifying it.

  Lock ID: abc123-def456-ghi789

  Enter a value: yes

Terraform state has been successfully unlocked!
```

### Step 3: Verify State Is Clean

```bash
# Verify plan runs without lock error
terraform plan

# Verify state is not corrupted
terraform state list
```

### Step 4: Investigate Root Cause

```text
Common causes of stale locks:
  - CI/CD pipeline was killed mid-apply (no cleanup)
  - Terminal was closed during terraform apply
  - Network interruption to DynamoDB during lock cleanup
  - EC2 runner running Terraform was terminated

Fix for CI/CD:
  Add a cleanup step that runs terraform force-unlock if the apply fails.
  Better: use TFC or Atlantis which handle lock cleanup automatically.
```

---

## Prevention

```text
- Use CI/CD for all production applies (not local laptops)
- Set timeouts on CI/CD pipelines (if apply takes > 30 min, something is wrong)
- TFC and Atlantis clean up locks automatically on job failure
- Never Ctrl+C during an apply — let it finish or recover gracefully
```
