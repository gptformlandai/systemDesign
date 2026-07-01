# 19. Scenario: Multi-Environment Dev / Staging / Prod

## The Problem: How Do You Manage Multiple Environments?

```text
Requirements:
  - Same infrastructure topology for dev, staging, prod
  - Different sizes (t3.micro dev vs t3.large prod)
  - Different account IDs (best practice: separate AWS accounts per env)
  - Independent state files (prod change doesn't risk dev)
  - Rollout path: dev → staging → prod

Three approaches:
  A. Terraform Workspaces
  B. Directory-based environments
  C. Variable-based environments (same state, different vars)
```

---

## Approach A: Terraform Workspaces

```text
Same config directory, multiple state files in the same backend.

environments/app/
  main.tf
  variables.tf
  terraform.tfvars      ← defaults (dev)
  dev.tfvars
  staging.tfvars
  prod.tfvars
```

```hcl
# main.tf
resource "aws_instance" "web" {
  ami           = data.aws_ami.app.id
  instance_type = local.config.instance_type
  
  tags = {
    Name        = "web-${terraform.workspace}"
    Environment = terraform.workspace
  }
}

locals {
  config = {
    dev = {
      instance_type = "t3.micro"
      min_count     = 1
      max_count     = 2
    }
    staging = {
      instance_type = "t3.small"
      min_count     = 1
      max_count     = 3
    }
    prod = {
      instance_type = "t3.large"
      min_count     = 3
      max_count     = 10
    }
  }[terraform.workspace]
}
```

```bash
# Working with workspaces
terraform workspace new dev
terraform workspace new staging
terraform workspace new prod

terraform workspace select dev
terraform apply

terraform workspace select prod
terraform apply
```

```text
PROS:
  + Simple: one config directory
  + No code duplication
  + Easy to add new environments

CONS:
  - All environments share the same provider config (same AWS account)
  - Cannot have truly different infrastructure shapes per env
  - State files are in same backend bucket (locking across envs if same bucket+key)
  - terraform.workspace in resource names causes confusion

BEST FOR:
  Short-lived environments, feature branches, throwaway test envs
  NOT for long-lived prod/staging with different AWS accounts
```

---

## Approach B: Directory-Based Environments (Recommended)

```text
Each environment has its own root module. Modules are shared.

repo/
  modules/
    vpc/
    eks/
    rds/
    app/
  environments/
    dev/
      main.tf           ← calls modules
      variables.tf
      versions.tf       ← backend config for dev state
      terraform.tfvars
    staging/
      main.tf
      variables.tf
      versions.tf
      terraform.tfvars
    prod/
      main.tf
      variables.tf
      versions.tf
      terraform.tfvars
```

```hcl
# environments/dev/main.tf
module "network" {
  source             = "../../modules/vpc"
  name               = "myapp-dev"
  cidr_block         = "10.1.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b"]
  # ...
}

module "app" {
  source        = "../../modules/app"
  instance_type = "t3.micro"        # dev: small
  min_size      = 1
  max_size      = 2
  vpc_id        = module.network.vpc_id
  # ...
}
```

```hcl
# environments/prod/main.tf
module "network" {
  source             = "../../modules/vpc"
  name               = "myapp-prod"
  cidr_block         = "10.0.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

module "app" {
  source        = "../../modules/app"
  instance_type = "t3.large"         # prod: large
  min_size      = 3
  max_size      = 20
  vpc_id        = module.network.vpc_id
}
```

```text
PROS:
  + Full isolation between environments
  + Different provider configs per env (different AWS accounts)
  + Different infrastructure shapes if needed
  + Standard in large engineering teams

CONS:
  - Some config duplication across environment directories
    (mitigated by modules)
  - More directories to manage
  - Must run terraform in each directory separately

BEST FOR:
  Production-grade multi-environment setups
  Teams with separate AWS accounts per environment
```

---

## Approach C: Variable Files Only

```text
Single root module, no workspaces, different tfvars per environment.
State key includes env name.

environments/
  dev.tfvars
  staging.tfvars
  prod.tfvars

# Apply dev
terraform apply -var-file=environments/dev.tfvars

# Apply prod
terraform apply -var-file=environments/prod.tfvars
```

```text
PROS:
  + Very simple setup
  + Good for single-account setups

CONS:
  - One state file (or must manually change backend key per env)
  - Easy to accidentally apply prod vars against wrong state
  - No account isolation

BEST FOR:
  Small projects, single AWS account, few environments
```

---

## Comparison Table

| Factor | Workspaces | Directories | Var Files |
|---|---|---|---|
| AWS account isolation | No | Yes | No |
| Code duplication | Low | Medium (modules reduce it) | Low |
| Plan/apply isolation | Workspace state | Separate dirs | Manual |
| Different shapes per env | Difficult | Easy | Difficult |
| Team scalability | Low | High | Low |
| Complexity | Low | Medium | Low |
| Recommended for prod | No | Yes | No |

---

## Rollout Pipeline (Directory-Based)

```text
Step 1: Developer opens PR with infrastructure change
         → terraform plan runs against dev workspace in CI

Step 2: PR merged to main
         → terraform apply runs against dev
         → Automated tests against dev (optional)

Step 3: Manual or automatic gate: promote to staging
         → terraform plan + apply against staging
         → Integration tests

Step 4: Manual approval gate: promote to prod
         → terraform plan → human reviews → terraform apply prod
         → Monitoring + alerting watches for issues
```

---

## Interview Sound Bite

Three patterns for multi-environment Terraform: OSS workspaces (same config, multiple state files — best for short-lived feature branches), directory-based isolation (separate root module per environment with shared modules — best for long-lived production environments with different AWS accounts), and var-file-only approach (one config, `-var-file=prod.tfvars` — only for simple single-account setups). For production, directory-based is the standard: each environment is a separate root module, each with its own backend state key, its own provider config (possibly different `assume_role` ARN for different AWS accounts), and independently deployable without affecting other environments.
