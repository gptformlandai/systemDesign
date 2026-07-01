# 26. Terraform Anti-Patterns and Common Mistakes

## Anti-Pattern 1: Committing State To Git

```text
MISTAKE:
  git add terraform.tfstate  # NEVER

WHY IT'S BAD:
  - State contains plaintext secrets: DB passwords, private keys, API tokens
  - Merge conflicts in state are catastrophic (JSON corruption)
  - State grows large; slows down git
  - No locking: two developers may commit conflicting state

CORRECT:
  Add *.tfstate and *.tfstate.backup to .gitignore
  Use S3 + DynamoDB, GCS, or TFC as remote backend
  Enable versioning on the state bucket for recovery
```

---

## Anti-Pattern 2: No Remote Backend In Production

```text
MISTAKE:
  # No backend block → state stored in terraform.tfstate on developer's laptop
  terraform apply

WHY IT'S BAD:
  - Lost laptop = lost state = cannot manage infrastructure
  - No team sharing: another developer cannot manage the same infra
  - No locking: concurrent applies corrupt state
  - No backup or versioning

CORRECT:
  Always use a remote backend for any non-throwaway infrastructure.
  Minimum: S3 backend with DynamoDB locking.
```

---

## Anti-Pattern 3: Hardcoded Values In Modules

```hcl
# MISTAKE: hardcoded region, account, and instance type in a module
resource "aws_instance" "web" {
  ami           = "ami-0c55b159cbfafe1f0"  # hardcoded AMI
  instance_type = "t3.micro"               # hardcoded type
  subnet_id     = "subnet-abc123"          # hardcoded subnet ID!
  
  tags = {
    Owner = "john.smith@company.com"       # hardcoded owner
  }
}
```

```hcl
# CORRECT: all configurable values are variables
variable "ami_id"         { type = string }
variable "instance_type"  { type = string; default = "t3.micro" }
variable "subnet_id"      { type = string }
variable "owner"          { type = string }

resource "aws_instance" "web" {
  ami           = var.ami_id
  instance_type = var.instance_type
  subnet_id     = var.subnet_id
  tags = { Owner = var.owner }
}
```

---

## Anti-Pattern 4: Using count For Named Resources

```hcl
# MISTAKE: count for resources that have meaningful names
resource "aws_iam_user" "devs" {
  count = 3
  name  = "dev-user-${count.index}"   # 0, 1, 2 — meaningless
}

# If you change the list order:
# ["alice", "bob", "carol"] → ["alice", "carol"]
# Terraform sees: index 1 changed from bob to carol (update)
#                 index 2 deleted (carol → bob deleted!)
# Result: bob is deleted, carol is renamed to index 1
```

```hcl
# CORRECT: for_each with meaningful keys
resource "aws_iam_user" "devs" {
  for_each = toset(["alice", "bob", "carol"])
  name     = each.key
}
# Removing "bob" only deletes aws_iam_user.devs["bob"]
# alice and carol are untouched
```

---

## Anti-Pattern 5: God Module (Kitchen Sink)

```text
MISTAKE: One module that creates VPC + EKS + RDS + ALB + Route53 + IAM

WHY IT'S BAD:
  - Cannot reuse parts (need VPC but not EKS? Still get everything)
  - One module failure blocks everything
  - Huge blast radius per plan
  - Testing is monolithic

CORRECT:
  Single-responsibility modules: vpc/, eks/, rds/, alb/, iam/
  Root module wires them together
  Each module independently testable and reusable
```

---

## Anti-Pattern 6: Provider Configuration In Child Modules

```hcl
# MISTAKE: provider block inside a child module
# modules/vpc/main.tf
provider "aws" {
  region = "us-east-1"   # hardcoded in module!
}
```

```hcl
# CORRECT: only declare required_providers in child module
# modules/vpc/versions.tf
terraform {
  required_providers {
    aws = { source = "hashicorp/aws" }
  }
}
# No provider {} block — caller's root module configures the provider
```

---

## Anti-Pattern 7: Not Pinning Provider Or Module Versions

```hcl
# MISTAKE: no version constraint
terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
      # no version! Could get aws provider 6.0 which has breaking changes
    }
  }
}

module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  # no version! Gets latest; could break silently
}
```

```hcl
# CORRECT: pin versions
required_providers {
  aws = {
    source  = "hashicorp/aws"
    version = "~> 5.0"
  }
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.1.2"   # exact pin for production; ~> 5.1 acceptable for dev
}
```

---

## Anti-Pattern 8: Using -target In Normal Workflow

```text
MISTAKE: Regular use of terraform apply -target as a workflow
  terraform apply -target=module.eks   # just apply EKS today
  terraform apply -target=module.rds   # just RDS tomorrow

WHY IT'S BAD:
  - State becomes inconsistent: Terraform doesn't know about changes
    to non-targeted resources
  - Plan output lies: shows "No changes" for things it hasn't checked
  - Drift accumulates silently
  - Teammate runs full apply → unexpected large changeset

CORRECT:
  Use -target only to unblock a stuck situation (create a missing dependency).
  Always follow up with a full terraform apply to reconcile state.
```

---

## Anti-Pattern 9: No Outputs From Modules

```hcl
# MISTAKE: module creates VPC and subnets but exposes nothing
# modules/vpc/outputs.tf (empty or missing)

# Now the caller cannot use module.vpc.vpc_id or module.vpc.subnet_ids
# Forced to use data sources or hardcode IDs — defeats the purpose of modules
```

```hcl
# CORRECT: expose everything that callers might need
output "vpc_id"             { value = aws_vpc.this.id }
output "public_subnet_ids"  { value = aws_subnet.public[*].id }
output "private_subnet_ids" { value = aws_subnet.private[*].id }
output "vpc_cidr_block"     { value = aws_vpc.this.cidr_block }
```

---

## Anti-Pattern 10: Misusing terraform_remote_state

```text
MISTAKE:
  Using terraform_remote_state as the only way to share data between stacks.
  Creates tight coupling: Stack B cannot plan unless Stack A has applied.

ALTERNATIVES:
  1. SSM Parameter Store: Stack A writes outputs to SSM; Stack B reads them via data source
     → Works even if stacks are in different accounts
     → Outputs are queryable without Terraform access

  2. Hard-code non-volatile values (VPC ID rarely changes):
     Just reference it as a variable in each stack

  3. Use terraform_remote_state selectively:
     → Only for frequently-changing outputs
     → Only between stacks where tight coupling is acceptable
```

---

## Anti-Pattern 11: Storing Secrets In tfvars

```text
MISTAKE:
  prod.tfvars:
    db_password = "super-secret-password"
  
  Even with .gitignore, one git add -A can commit it.
  Secrets appear in CI/CD logs when passed as -var arguments.

CORRECT:
  Use TF_VAR_db_password environment variable (set from secrets manager in CI/CD)
  OR use data sources (SSM, Secrets Manager, Vault) to pull secrets at apply time
  OR use SOPS to encrypt the tfvars file before committing
```

---

## Anti-Pattern 12: Running terraform apply From Laptops In Production

```text
MISTAKE:
  Developer runs terraform apply prod from their laptop

WHY IT'S BAD:
  - No audit trail
  - Inconsistent Terraform version between developers
  - Local state of provider cache may differ
  - No policy checks or security scans
  - Credentials may be overly broad

CORRECT:
  All production applies through CI/CD pipeline
  Pipeline uses OIDC credentials (no stored secrets)
  Pipeline runs validate + tflint + checkov before plan
  Plan output reviewed in PR before apply
```

---

## Quick-Reference Anti-Pattern Table

| Anti-Pattern | Risk | Fix |
|---|---|---|
| State in git | Secret exposure | Remote backend + .gitignore |
| No remote backend | Data loss, no team sharing | S3 + DynamoDB |
| Hardcoded values in modules | Not reusable | Variables for everything |
| count for named resources | Unnecessary destroy/recreate | for_each with meaningful keys |
| God module | High blast radius | Single-responsibility modules |
| Provider in child module | Not reusable | Provider in root only |
| No version pins | Breaking upgrades | `~> major.minor` constraints |
| Regular -target use | Silent state drift | Full apply after each targeted apply |
| No module outputs | Not composable | Output every useful attribute |
| Secrets in tfvars | Credential leak | TF_VAR_ env vars or data sources |
