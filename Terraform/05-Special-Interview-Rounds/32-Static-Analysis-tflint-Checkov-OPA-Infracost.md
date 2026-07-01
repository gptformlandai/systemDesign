# 32. Static Analysis: tflint, Checkov, OPA/Conftest, Infracost

## Why Static Analysis For Terraform?

```text
terraform validate: syntax + type checking only.
Static analysis tools go further:

  tflint         → Terraform linter: unused variables, deprecated syntax,
                   provider-specific best practices (e.g., invalid instance type for AWS)
  
  checkov        → Security + compliance scanner: CIS benchmarks, SOC2, HIPAA,
                   misconfigurations (unencrypted S3, public RDS, open security groups)
  
  trivy          → Vulnerability scanner that includes Terraform misconfig checks
                   (absorbed tfsec; unified tool for containers + IaC)
  
  OPA/Conftest   → Policy-as-code using Rego language; enforce custom rules
                   (no resource without cost_center tag, only approved regions, etc.)
  
  infracost      → Cost estimation: shows monthly cost of plan changes before apply
```

---

## tflint

tflint checks Terraform configurations for errors that `terraform validate` misses — invalid AWS resource arguments, deprecated syntax, naming conventions.

```bash
# Install (macOS)
brew install tflint

# Initialize (downloads provider plugins for rule sets)
tflint --init

# Basic run
tflint

# Recursive (all subdirectories)
tflint --recursive

# Enable only specific rules
tflint --enable-rule=terraform_required_version

# Config file
tflint --config=.tflint.hcl
```

### .tflint.hcl Configuration

```hcl
# .tflint.hcl
config {
  format              = "default"
  call_module_type    = "local"   # also check local modules
  force               = false
  disabled_by_default = false
}

plugin "aws" {
  enabled = true
  version = "0.28.0"
  source  = "github.com/terraform-linters/tflint-ruleset-aws"
}

plugin "google" {
  enabled = true
  version = "0.27.0"
  source  = "github.com/terraform-linters/tflint-ruleset-google"
}

rule "terraform_required_version" {
  enabled = true
}

rule "terraform_documented_variables" {
  enabled = true
}

rule "terraform_documented_outputs" {
  enabled = true
}
```

### What tflint Catches

```text
AWS provider rules (aws plugin):
  aws_instance_invalid_type          ← invalid EC2 instance type
  aws_instance_invalid_ami           ← AMI format validation
  aws_db_instance_invalid_type       ← invalid RDS instance type
  aws_s3_bucket_name                 ← S3 naming convention
  aws_elasticache_cluster_invalid_type

Core Terraform rules:
  terraform_required_version         ← missing required_version
  terraform_required_providers       ← undeclared providers
  terraform_documented_variables     ← missing description on variable
  terraform_documented_outputs       ← missing description on output
  terraform_naming_convention        ← resource naming (configurable regex)
  terraform_typed_variables          ← variables missing type annotation
  terraform_unused_declarations      ← unused variables/locals
```

---

## Checkov

Checkov scans Terraform for security misconfigurations. It maps checks to CIS benchmarks, SOC 2, PCI-DSS, HIPAA.

```bash
# Install
pip install checkov
# OR: brew install checkov

# Scan current directory
checkov -d . --framework terraform

# Scan with specific checks
checkov -d . --check CKV_AWS_18,CKV_AWS_21

# Skip specific checks
checkov -d . --skip-check CKV_AWS_18

# Output formats
checkov -d . --output json
checkov -d . --output sarif    # for GitHub Security tab
checkov -d . --quiet            # only show failures

# Generate SBOM
checkov -d . --output cyclonedx
```

### Key Checkov Checks (AWS)

```text
S3 Buckets:
  CKV_AWS_18   S3 bucket access logging enabled
  CKV_AWS_19   S3 bucket server-side encryption
  CKV_AWS_20   S3 bucket public access
  CKV_AWS_21   S3 versioning enabled
  CKV_AWS_144  S3 cross-region replication

Security Groups:
  CKV_AWS_23   Security group unrestricted SSH (port 22 from 0.0.0.0/0)
  CKV_AWS_24   Security group unrestricted RDP (port 3389)
  CKV_AWS_25   Security group unrestricted ingress

RDS:
  CKV_AWS_16   RDS encryption at rest
  CKV_AWS_17   RDS not publicly accessible
  CKV_AWS_79   RDS deletion protection enabled
  CKV_AWS_129  RDS backup retention > 7 days

IAM:
  CKV_AWS_40   IAM policies attached to roles, not users
  CKV_AWS_274  IAM policy too permissive (*)
  CKV_AWS_111  IAM policy allows write without constraint

EKS:
  CKV_AWS_37   EKS secrets encryption enabled
  CKV_AWS_38   EKS logging enabled
  CKV_AWS_39   EKS node groups in private subnets
```

### .checkov.yml Configuration

```yaml
# .checkov.yml
framework:
  - terraform

skip-check:
  - CKV_AWS_7   # S3 bucket MFA delete (not always feasible)

check:
  - CKV_AWS_16
  - CKV_AWS_17
  - CKV_AWS_18

soft-fail: false
compact: true
```

### Inline Suppression

```hcl
# Suppress a specific check on a resource
resource "aws_s3_bucket" "logs" {
  bucket = "my-log-bucket"

  # checkov:skip=CKV_AWS_21:Log buckets don't need versioning enabled
  # checkov:skip=CKV_AWS_144:Cross-region replication not required for logs
}
```

---

## trivy (Formerly tfsec)

trivy is the unified security scanner from Aqua Security that replaced `tfsec`. It scans containers, container images, and IaC.

```bash
# Install
brew install trivy

# Scan Terraform directory
trivy config .

# Scan with severity filter
trivy config . --severity HIGH,CRITICAL

# Output formats
trivy config . --format json
trivy config . --format sarif

# Ignore file
trivy config . --ignorefile .trivyignore
```

---

## OPA / Conftest: Custom Policy-as-Code

Open Policy Agent (OPA) with Conftest enforces custom organizational policies beyond what tflint/checkov cover.

```bash
# Install Conftest
brew install conftest

# Test Terraform plan JSON against policies
terraform plan -out=tfplan
terraform show -json tfplan > tfplan.json
conftest test tfplan.json --policy policies/
```

### Example OPA Policy

```rego
# policies/terraform.rego
package main

import future.keywords.in

# Deny resources without required tags
deny[msg] {
  some resource in input.resource_changes
  resource.change.actions != ["no-op"]
  not resource.change.after.tags.cost_center
  msg := sprintf("Resource '%s' is missing required tag 'cost_center'", [resource.address])
}

# Deny resources in unapproved regions
deny[msg] {
  some resource in input.resource_changes
  resource.type == "aws_instance"
  not resource.change.after.availability_zone in approved_azs
  msg := sprintf("EC2 instance '%s' must be in an approved AZ", [resource.address])
}

approved_azs := {"us-east-1a", "us-east-1b", "us-east-1c"}

# Warn (not deny) if S3 bucket lifecycle is missing
warn[msg] {
  some resource in input.resource_changes
  resource.type == "aws_s3_bucket"
  resource.change.actions == ["create"]
  not resource.change.after.lifecycle_rule
  msg := sprintf("S3 bucket '%s' has no lifecycle rules configured", [resource.address])
}
```

---

## Infracost: Cost Estimation

```bash
# Install
brew install infracost
infracost auth login  # or set INFRACOST_API_KEY

# Estimate cost of current directory
infracost breakdown --path .

# Show cost diff between current state and proposed changes
infracost diff --path . --compare-to=baseline.json

# Export baseline for comparison
infracost breakdown --path . --format json > baseline.json

# CI/CD: comment cost on PR
infracost comment github \
  --path tfplan.json \
  --repo $GITHUB_REPOSITORY \
  --pull-request $PR_NUMBER \
  --github-token $GITHUB_TOKEN
```

```text
Example output:
  Monthly cost estimate
  ───────────────────────────────────────────
  + aws_instance.web         $8.47/mo (+$8.47)
    instance_type: t3.micro

  + aws_db_instance.main     $24.82/mo (+$24.82)
    instance_class: db.t3.small, multi_az: false

  + aws_nat_gateway.main     $32.94/mo (+$32.94)

  Total: $66.23/mo
```

---

## CI/CD Quality Gate Pipeline

```yaml
# .github/workflows/terraform.yml (quality gates)
- name: Run tflint
  run: |
    tflint --init
    tflint --recursive --format=compact

- name: Run Checkov
  uses: bridgecrewio/checkov-action@v12
  with:
    directory: .
    framework: terraform
    output_format: sarif
    output_file_path: checkov-results.sarif
    soft_fail: false   # fail build on HIGH/CRITICAL

- name: Upload Checkov results to GitHub Security
  uses: github/codeql-action/upload-sarif@v3
  if: always()
  with:
    sarif_file: checkov-results.sarif

- name: Conftest policy check
  run: |
    terraform plan -out=tfplan
    terraform show -json tfplan > tfplan.json
    conftest test tfplan.json --policy policies/

- name: Infracost cost estimate
  uses: infracost/actions/setup@v3
  with:
    api-key: ${{ secrets.INFRACOST_API_KEY }}
- run: infracost diff --path . --format=json --out-file infracost.json
- uses: infracost/actions/comment@v3
  with:
    path: infracost.json
    behavior: update
```

---

## Tool Comparison

| Tool | Focus | Language | Cloud-Specific | Custom Rules |
|---|---|---|---|---|
| tflint | Lint + provider rules | HCL | Yes (via plugins) | Limited |
| checkov | Security compliance | Python | Yes (CIS/SOC2) | Python |
| trivy | Vuln + misconfig | Go | Yes | Limited |
| OPA/Conftest | Custom policy | Rego | No | Full flexibility |
| infracost | Cost estimation | Go | Yes (AWS/GCP/Azure) | No |

---

## Interview Sound Bite

A production Terraform quality gate has four layers: `tflint` for AWS-specific lint (invalid instance types, undocumented variables), `checkov` for security compliance (open security groups, unencrypted RDS, missing S3 logging), `OPA/Conftest` for custom organizational policies (required tags, approved regions), and `infracost` for cost visibility before apply. Run them in CI in order: validate → tflint → checkov → plan → conftest → apply. Checkov produces SARIF output consumable by GitHub Advanced Security's security dashboard. OPA policies operate against `terraform show -json tfplan` — the full plan JSON — so they can enforce invariants across the entire planned changeset.
