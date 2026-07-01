# 09. Data Sources, Locals, Workspace Patterns

## Data Sources

Data sources let you **read** existing infrastructure — things not managed by the current Terraform config. They query provider APIs and return attributes you can use in your HCL.

```hcl
# Syntax
data "<TYPE>" "<NAME>" {
  # filter arguments
}

# Reference: data.<TYPE>.<NAME>.<ATTRIBUTE>
```

---

### Common AWS Data Sources

```hcl
# Latest Amazon Linux 2 AMI
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "web" {
  ami           = data.aws_ami.amazon_linux.id
  instance_type = "t3.micro"
}
```

```hcl
# Existing VPC by tag
data "aws_vpc" "shared" {
  filter {
    name   = "tag:Name"
    values = ["shared-vpc"]
  }
}

# Subnets in that VPC
data "aws_subnets" "private" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.shared.id]
  }
  filter {
    name   = "tag:Type"
    values = ["private"]
  }
}

data "aws_subnets" "private" {
  tags = {
    Tier = "Private"
  }
}
```

```hcl
# Current AWS caller identity
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name
}

output "current_account" {
  value = data.aws_caller_identity.current.account_id
}
```

```hcl
# IAM policy document (JSON generation without heredoc)
data "aws_iam_policy_document" "s3_read" {
  statement {
    effect  = "Allow"
    actions = ["s3:GetObject", "s3:ListBucket"]
    resources = [
      aws_s3_bucket.data.arn,
      "${aws_s3_bucket.data.arn}/*"
    ]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_policy" "s3_read" {
  name   = "s3-read-policy"
  policy = data.aws_iam_policy_document.s3_read.json
}
```

```hcl
# Route53 hosted zone
data "aws_route53_zone" "main" {
  name         = "mycompany.com"
  private_zone = false
}

resource "aws_route53_record" "api" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "api.mycompany.com"
  type    = "A"
  # ...
}
```

```hcl
# SSM Parameter (read secrets from Parameter Store)
data "aws_ssm_parameter" "db_password" {
  name            = "/prod/db/password"
  with_decryption = true  # decrypt SecureString
}

resource "aws_db_instance" "main" {
  password = data.aws_ssm_parameter.db_password.value
  # ...
}
```

---

### Remote State Data Source

```hcl
# Read outputs from another Terraform state file
data "terraform_remote_state" "network" {
  backend = "s3"
  config = {
    bucket = "my-terraform-state"
    key    = "network/terraform.tfstate"
    region = "us-east-1"
  }
}

resource "aws_instance" "app" {
  subnet_id = data.terraform_remote_state.network.outputs.private_subnet_ids[0]
}
```

---

## Locals In Depth

Locals define computed values that are reused multiple times across the configuration. They reduce copy-paste and make complex expressions readable.

```hcl
locals {
  # Derived from input variables
  name_prefix = "${var.project}-${var.environment}"
  is_prod     = var.environment == "prod"

  # Common tags merged with module-specific tags
  common_tags = merge(var.tags, {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
    Team        = var.team
  })

  # Conditional sizing
  instance_type = local.is_prod ? "t3.large" : "t3.micro"
  min_size      = local.is_prod ? 3 : 1
  max_size      = local.is_prod ? 10 : 3

  # Computed from data sources
  account_id = data.aws_caller_identity.current.account_id
  region     = data.aws_region.current.name

  # Derived CIDR math
  vpc_cidr      = "10.${local.is_prod ? 0 : 1}.0.0/16"
  public_cidrs  = [for i, az in var.availability_zones : cidrsubnet(local.vpc_cidr, 8, i)]
  private_cidrs = [for i, az in var.availability_zones : cidrsubnet(local.vpc_cidr, 8, i + 10)]

  # Processed map (useful for for_each)
  subnet_config = {
    for i, az in var.availability_zones : "subnet-${i}" => {
      az   = az
      cidr = local.private_cidrs[i]
    }
  }
}
```

---

## Workspaces

Workspaces let you use the same Terraform configuration with different state files — useful for short-lived environments or feature branches.

```bash
# List workspaces
terraform workspace list
# * default
#   staging
#   feature-xyz

# Create a new workspace
terraform workspace new staging

# Switch to a workspace
terraform workspace select prod

# Show current workspace name
terraform workspace show
# prod

# Delete a workspace (must not be current, must be empty)
terraform workspace delete feature-xyz
```

The built-in `terraform.workspace` value returns the current workspace name.

```hcl
# Use workspace name in resource names to isolate resources
resource "aws_s3_bucket" "app_data" {
  bucket = "${var.project}-${terraform.workspace}-data"
}

# Workspace-conditional sizing (common pattern)
locals {
  instance_type = terraform.workspace == "prod" ? "t3.large" : "t3.micro"
  replicas      = terraform.workspace == "prod" ? 3 : 1
}

# Workspace-specific variable overrides using lookup
variable "instance_types" {
  default = {
    default = "t3.micro"
    staging = "t3.small"
    prod    = "t3.large"
  }
}

locals {
  instance_type = lookup(var.instance_types, terraform.workspace, var.instance_types["default"])
}
```

---

## Multi-Environment Patterns: When To Use Workspaces

```text
WORKSPACE APPROACH (OSS Terraform workspaces):
  Same config directory, multiple state files in same backend.
  Good for: feature branch environments, short-lived testing
  Bad for:  long-lived environments with different provider configs,
            different account IDs, or significantly different infrastructure

DIRECTORY APPROACH (separate root configs):
  environments/
    dev/   → main.tf, variables.tf, dev.tfvars
    staging/ → ...
    prod/  → ...
  
  Good for: long-lived environments, different provider configs per env
  Bad for:  code duplication unless you use modules

COMBINED APPROACH (most common in production):
  Use modules for reusable components.
  Separate state files per environment (directory or TFC workspace).
  Workspace per environment in TFC (not OSS workspaces).
```

---

## Data Source vs Resource Decision

```text
Use data source when:
  - Infrastructure was created outside Terraform (manually, by another team)
  - Infrastructure is managed by a different Terraform root module
  - You need to look up AWS-managed resources (AMIs, availability zones, regions)
  - You want to query but not own the resource

Use resource when:
  - Terraform should own and manage the full lifecycle (create/update/destroy)
  - You are building new infrastructure

Examples:
  data → aws_ami, aws_vpc (shared network), aws_route53_zone, aws_ssm_parameter
  resource → new EC2, new VPC you own, new RDS database
```

---

## Interview Sound Bite

Data sources are read-only queries — `data "aws_ami" "latest" {}` finds existing AMIs, `data "aws_vpc" "shared" {}` reads a VPC another team owns. `terraform_remote_state` lets you read outputs from another workspace's state, enabling cross-stack references. Locals are computed values local to the current module — not inputs, not outputs, just named intermediates that prevent copy-paste. Terraform workspaces let the same config manage multiple state files: the built-in `terraform.workspace` string returns the current workspace name, which you can use to size resources or name buckets differently per environment. For long-lived prod/staging environments, directory-based isolation (separate root modules) is more robust than OSS workspaces.
