# 31. Terragrunt: DRY Patterns and Dependency Management

## What Is Terragrunt?

Terragrunt is a thin wrapper around Terraform (by Gruntwork) that adds:
- **DRY backend config** — generate the backend block once, not in every root module
- **DRY provider config** — share provider and version constraints across all modules
- **`dependency` blocks** — read outputs from other Terragrunt modules (like `terraform_remote_state` but automatic)
- **`run-all` commands** — plan/apply multiple modules in dependency order
- **Hooks** — run scripts before/after terraform commands
- **Auto-retry** — retries on transient API errors

```text
Without Terragrunt (copy-paste problem):
  services/order-api/dev/versions.tf     ← same backend config
  services/order-api/prod/versions.tf    ← same backend config (different key)
  services/payment/dev/versions.tf       ← same backend config (different key)
  services/payment/prod/versions.tf      ← same backend config (different key)
  ... 50 more environments ...

With Terragrunt:
  root.hcl                               ← backend config ONCE
  services/order-api/dev/terragrunt.hcl  ← just: include root + inputs
  services/order-api/prod/terragrunt.hcl ← just: include root + inputs
```

---

## Directory Structure

```text
infra/
├── root.hcl                  ← shared config (backend, provider, remote state gen)
├── modules/
│   ├── vpc/
│   ├── eks/
│   └── rds/
└── environments/
    ├── dev/
    │   ├── vpc/
    │   │   └── terragrunt.hcl   ← dev VPC config
    │   ├── eks/
    │   │   └── terragrunt.hcl
    │   └── rds/
    │       └── terragrunt.hcl
    └── prod/
        ├── vpc/
        │   └── terragrunt.hcl
        ├── eks/
        │   └── terragrunt.hcl
        └── rds/
            └── terragrunt.hcl
```

---

## root.hcl (Shared Configuration)

```hcl
# root.hcl — sits at the repo root, included by all child configs

locals {
  # Parse environment and service from the directory path
  # e.g., /infra/environments/prod/eks/terragrunt.hcl
  parsed  = regex(".*/environments/(?P<env>[^/]+)/(?P<service>[^/]+)/.*", get_terragrunt_dir())
  env     = local.parsed.env
  service = local.parsed.service

  aws_region = "us-east-1"
  account_ids = {
    dev  = "111111111111"
    staging = "222222222222"
    prod = "333333333333"
  }
  account_id = local.account_ids[local.env]
}

# Auto-generate the backend block for every child module
generate "backend" {
  path      = "backend.tf"
  if_exists = "overwrite_terragrunt"
  contents  = <<-EOF
    terraform {
      backend "s3" {
        bucket         = "mycompany-terraform-state-${local.account_id}"
        key            = "environments/${local.env}/${local.service}/terraform.tfstate"
        region         = "${local.aws_region}"
        encrypt        = true
        dynamodb_table = "terraform-state-lock"
      }
    }
  EOF
}

# Auto-generate provider block
generate "provider" {
  path      = "provider.tf"
  if_exists = "overwrite_terragrunt"
  contents  = <<-EOF
    provider "aws" {
      region = "${local.aws_region}"
      assume_role {
        role_arn = "arn:aws:iam::${local.account_id}:role/TerraformDeployRole"
      }
      default_tags {
        tags = {
          Environment = "${local.env}"
          ManagedBy   = "terragrunt"
        }
      }
    }
  EOF
}

# Terraform version constraint for all modules
terraform {
  extra_arguments "retry_lock" {
    commands  = get_terraform_commands_that_need_locking()
    arguments = ["-lock-timeout=20m"]
  }
}

# Remote state helper for dependency blocks
remote_state {
  backend = "s3"
  generate = {
    path      = "backend.tf"
    if_exists = "overwrite_terragrunt"
  }
  config = {
    bucket         = "mycompany-terraform-state-${local.account_id}"
    key            = "environments/${local.env}/${local.service}/terraform.tfstate"
    region         = local.aws_region
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}
```

---

## Child Module terragrunt.hcl

```hcl
# environments/prod/vpc/terragrunt.hcl

# Include the root config (DRY: inherits backend + provider generation)
include "root" {
  path   = find_in_parent_folders("root.hcl")
  expose = true   # allows accessing include.root.locals
}

# Point to the reusable Terraform module
terraform {
  source = "../../../modules//vpc"
  # Double slash separates module path from sub-path (git convention)
}

# Pass inputs to the module (like terraform.tfvars but controlled by Terragrunt)
inputs = {
  name               = "myapp-prod-vpc"
  cidr_block         = "10.0.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]
  enable_nat_gateway = true
}
```

---

## dependency Block

Reads outputs from another Terragrunt module. Replaces manual `terraform_remote_state`.

```hcl
# environments/prod/eks/terragrunt.hcl

include "root" {
  path = find_in_parent_folders("root.hcl")
}

terraform {
  source = "../../../modules//eks"
}

# Declare a dependency on the vpc module
dependency "vpc" {
  config_path = "../vpc"    # relative path to the terragrunt.hcl dir

  # Mock outputs for plan without applying VPC first
  mock_outputs = {
    vpc_id             = "vpc-mock"
    private_subnet_ids = ["subnet-mock-1", "subnet-mock-2"]
  }
  mock_outputs_allowed_terraform_commands = ["validate", "plan"]
}

inputs = {
  cluster_name        = "myapp-prod"
  kubernetes_version  = "1.29"
  vpc_id              = dependency.vpc.outputs.vpc_id              # reads VPC output
  subnet_ids          = dependency.vpc.outputs.private_subnet_ids  # reads VPC output
  instance_types      = ["t3.large"]
  desired_size        = 3
  min_size            = 2
  max_size            = 10
}
```

```hcl
# environments/prod/rds/terragrunt.hcl

dependency "vpc" {
  config_path = "../vpc"
  mock_outputs = { vpc_id = "vpc-mock", private_subnet_ids = ["subnet-mock"] }
  mock_outputs_allowed_terraform_commands = ["validate", "plan"]
}

dependency "eks" {
  config_path = "../eks"
  mock_outputs = { node_security_group_id = "sg-mock" }
  mock_outputs_allowed_terraform_commands = ["validate", "plan"]
}

inputs = {
  identifier                 = "myapp-prod-db"
  vpc_id                     = dependency.vpc.outputs.vpc_id
  subnet_ids                 = dependency.vpc.outputs.private_subnet_ids
  eks_node_security_group_id = dependency.eks.outputs.node_security_group_id
}
```

---

## run-all Commands

```bash
# Plan ALL modules in dependency order (prod environment)
cd environments/prod
terragrunt run-all plan

# Apply ALL modules in dependency order
# Builds dependency graph: vpc → eks → rds (deps applied first)
terragrunt run-all apply

# Destroy in reverse dependency order
terragrunt run-all destroy

# Apply only changed modules (compares git diff)
terragrunt run-all apply --terragrunt-modules-that-include root.hcl

# Single module
cd environments/prod/eks
terragrunt plan
terragrunt apply
```

---

## Hooks (Before/After Terraform Commands)

```hcl
# In terragrunt.hcl or root.hcl

terraform {
  # Run before init
  before_hook "before_init" {
    commands = ["init"]
    execute  = ["echo", "Initializing ${path_relative_to_include()}..."]
  }

  # Run after apply
  after_hook "notify_slack" {
    commands     = ["apply"]
    execute      = ["bash", "${get_repo_root()}/scripts/notify-slack.sh"]
    run_on_error = false
  }

  # Run before plan to refresh AWS credentials
  before_hook "refresh_credentials" {
    commands = ["plan", "apply"]
    execute  = ["aws", "sts", "get-caller-identity"]
  }
}
```

---

## Terragrunt Built-In Functions

```hcl
get_terragrunt_dir()        # absolute path to current terragrunt.hcl directory
get_parent_terragrunt_dir() # parent directory
find_in_parent_folders()    # search upward for a file (used for root.hcl)
get_repo_root()             # git repo root
path_relative_to_include()  # relative path from include to current config
get_env("VAR_NAME", "default")  # read environment variable

# Parsing
run_cmd("aws", "sts", "get-caller-identity")  # run command and capture output
read_terragrunt_config("../other/terragrunt.hcl")  # read another config
```

---

## Terragrunt vs Native Terraform

| Factor | Native Terraform | Terragrunt |
|---|---|---|
| Backend DRY | Copy-paste `backend` block | `generate "backend"` in root.hcl |
| Dependencies | `terraform_remote_state` (manual) | `dependency` block (automatic) |
| Multi-env | Separate directories (still verbose) | DRY directory structure |
| `run-all` | Manual ordering required | Automatic dependency-ordered |
| Learning curve | Low | Medium |
| Maintenance | HashiCorp | Gruntwork (open-source) |
| Version | Terraform binary | Terragrunt wrapper (wraps Terraform) |

---

## When To Use Terragrunt

```text
Use Terragrunt when:
  - 10+ environments / stacks with the same backend pattern
  - Multiple teams sharing Terraform modules
  - You need dependency graphs across stacks
  - You want run-all apply for full environment deploys

Stick with native Terraform when:
  - Small number of environments (< 5)
  - Single team, single state file approach
  - You want to minimize tool surface area
  - Using Terraform Cloud (TFC handles some Terragrunt use cases natively)
```

---

## Interview Sound Bite

Terragrunt is a thin wrapper around Terraform that solves the DRY problem: instead of copying the `backend {}` block and `provider {}` block into every root module, you define them once in `root.hcl` and `generate` them automatically via `include`. The `dependency {}` block reads outputs from another Terragrunt module automatically — no manual `terraform_remote_state` data source needed. `terragrunt run-all apply` builds a dependency graph and applies modules in the correct order. The tradeoff: you add Terragrunt to your toolchain — useful when managing 20+ environments, less necessary for small setups where native Terraform directories suffice.
