# 28. Terraform Production Readiness Checklist

Use this checklist before deploying any Terraform-managed infrastructure to production.

---

## 1. State Management

- [ ] Remote backend configured (S3, GCS, or TFC — not local)
- [ ] State versioning enabled (S3 bucket versioning ON)
- [ ] State encryption enabled (S3: `encrypt = true` + KMS key)
- [ ] State locking enabled (DynamoDB table with `LockID` hash key)
- [ ] Public access blocked on state S3 bucket
- [ ] State bucket access restricted to Terraform runner role only (least privilege)
- [ ] `*.tfstate` and `*.tfstate.backup` in `.gitignore`
- [ ] State key follows naming convention: `<layer>/<service>/<env>/terraform.tfstate`
- [ ] Separate state files per environment (dev, staging, prod)

---

## 2. Credentials and Security

- [ ] No static AWS credentials in HCL, tfvars, or CI/CD environment variables
- [ ] OIDC dynamic credentials configured (GitHub Actions → AWS STS) OR IAM instance profile
- [ ] Terraform runner IAM role follows least privilege (only what the config needs)
- [ ] `sensitive = true` on all secret variables and outputs
- [ ] No secrets in tfvars files that could be accidentally committed
- [ ] Secrets fetched at apply time via data sources (SSM, Secrets Manager, Vault)
- [ ] CI/CD pipeline scrubs sensitive values from logs

---

## 3. Code Quality

- [ ] All external module versions pinned (exact version or `~> major.minor`)
- [ ] Provider versions pinned in `required_providers`
- [ ] `.terraform.lock.hcl` committed to git (consistent provider versions)
- [ ] `terraform validate` passes with no warnings
- [ ] `terraform fmt -check` passes (all files formatted)
- [ ] `tflint` passes with no violations
- [ ] `checkov` or equivalent security scanner shows no HIGH/CRITICAL violations
- [ ] No hardcoded account IDs, region names, or resource IDs in HCL
- [ ] All variables have `description` and `type` defined
- [ ] All outputs have `description` defined

---

## 4. Module Structure

- [ ] Single-responsibility modules (each module does one job)
- [ ] No provider configuration inside child modules
- [ ] Modules expose all useful outputs (IDs, ARNs, endpoints)
- [ ] Variable validation blocks on all user-provided inputs
- [ ] README with usage example exists for each reusable module
- [ ] `moved {}` blocks used for any resource renames (not `terraform state mv`)

---

## 5. Resource Protection

- [ ] `prevent_destroy = true` on stateful resources: RDS, S3 production data buckets, KMS keys
- [ ] `create_before_destroy = true` on resources that require zero-downtime replacement
- [ ] `name_prefix` used instead of `name` where `create_before_destroy` is set
- [ ] `ignore_changes = [password]` on DB instances (prevent drift from rotation)
- [ ] `ignore_changes = [desired_capacity]` on Auto Scaling Groups (prevent drift from ASG changes)
- [ ] Multi-AZ enabled on production RDS instances
- [ ] Deletion protection enabled on production RDS instances
- [ ] Final snapshot configuration set on RDS instances

---

## 6. CI/CD Pipeline

- [ ] `terraform plan -out=tfplan` used (not just `terraform plan`)
- [ ] `terraform apply tfplan` used (applies exactly the reviewed plan)
- [ ] Plan output posted to PR as a comment for review
- [ ] Production apply requires manual approval gate (not auto-apply)
- [ ] Plan runs on PR; apply runs on merge (not simultaneously)
- [ ] CI/CD uses pinned Terraform version (not `latest`)
- [ ] All applies tracked to PR/commit for audit trail
- [ ] Drift detection scheduled plan runs daily or weekly

---

## 7. Blast Radius Reduction

- [ ] Root module size: fewer than 200 resources per state file
- [ ] State split by layer (network, compute, database) or by service
- [ ] Each team/service has its own state file
- [ ] Separate AWS accounts per environment (dev account cannot affect prod)
- [ ] `-parallelism` tuned (default 10; decrease if seeing API throttling)
- [ ] No use of `-target` in normal workflow

---

## 8. Observability

- [ ] Resource tagging convention enforced (ManagedBy=terraform, Environment, Project, Team)
- [ ] Default tags configured on provider (not duplicated in every resource)
- [ ] Terraform version pinned in CI/CD and in `required_version` block
- [ ] Apply audit trail in CI/CD (who applied, what commit, what changed)
- [ ] S3 access logging enabled on state bucket (audit reads of state)
- [ ] CloudTrail enabled in all accounts (captures API calls Terraform makes)

---

## 9. Disaster Recovery

- [ ] State backup procedure documented and tested (restore from S3 version)
- [ ] State corruption recovery runbook exists (see Sheet 21)
- [ ] Bootstrap configuration documented (how to recreate state bucket if needed)
- [ ] All provider lock files in git (can reproduce exact setup)
- [ ] Critical infrastructure documented with `terraform show` output saved

---

## 10. Testing

- [ ] `terraform test` unit tests cover naming/tagging conventions
- [ ] `terraform test` or Terratest integration tests for each module
- [ ] examples/ directory exists and is tested in CI
- [ ] Preconditions and postconditions on critical resource assumptions
- [ ] Check blocks for ongoing health assertions

---

## Pre-Production Deploy Checklist (Run Before Each Apply)

```bash
# 1. Validate and lint
terraform validate
terraform fmt -check
tflint --recursive

# 2. Security scan
checkov -d . --framework terraform --quiet

# 3. Plan with full refresh
terraform plan -refresh=true -out=tfplan -no-color 2>&1 | tee plan.txt

# 4. Review plan for:
grep -E "will be destroyed|will be created|will be updated|must be replaced" plan.txt

# 5. Confirm: no unexpected destroys, no unexpected replacements
# 6. Get human approval (PR review)

# 7. Apply the exact plan
terraform apply tfplan

# 8. Post-apply verification
terraform state list | wc -l     # sanity check resource count
terraform output                  # verify outputs are populated
```
