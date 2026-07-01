# 02. HCL Syntax: Blocks, Variables, Outputs, Locals

## HCL Fundamentals

HCL (HashiCorp Configuration Language) is a structured configuration language — not a programming language. It has types, expressions, and functions but no classes or loops in the traditional sense.

```hcl
# Comments: single line
/* block comment */

# Basic value types:
string_val  = "hello"
number_val  = 42
bool_val    = true
list_val    = ["a", "b", "c"]
map_val     = { key = "value", other = "val2" }
null_val    = null
```

---

## Resource Block

```hcl
resource "<TYPE>" "<NAME>" {
  # arguments
}

# Type:  provider_resourcetype  (e.g., aws_instance, google_compute_instance)
# Name:  local label — used to reference this resource in HCL
# Together: aws_instance.web (type.name)

resource "aws_instance" "web" {
  ami           = "ami-0c55b159cbfafe1f0"
  instance_type = var.instance_type

  tags = {
    Name        = "web-server"
    Environment = var.environment
  }
}

# Reference this resource elsewhere:
aws_instance.web.id          # resource ID
aws_instance.web.public_ip   # public IP address
aws_instance.web.arn         # ARN
```

---

## Variables

Variables are the inputs to a module or root configuration.

```hcl
# Declaration (variables.tf)
variable "environment" {
  type        = string
  description = "Deployment environment: dev, staging, prod"
}

variable "instance_type" {
  type        = string
  default     = "t3.micro"
  description = "EC2 instance type"
  
  # Validation rule (Terraform 0.13+)
  validation {
    condition     = contains(["t3.micro", "t3.small", "t3.medium"], var.instance_type)
    error_message = "instance_type must be t3.micro, t3.small, or t3.medium."
  }
}

variable "allowed_cidrs" {
  type        = list(string)
  default     = []
  description = "CIDR blocks allowed to reach the load balancer"
}

variable "tags" {
  type        = map(string)
  default     = {}
  description = "Additional resource tags"
}

# Sensitive variable (value hidden in plan/apply output and logs)
variable "db_password" {
  type      = string
  sensitive = true
}
```

### Variable Types

```hcl
# Primitive types
type = string
type = number
type = bool

# Collection types
type = list(string)        # ordered list, same type
type = set(string)         # unordered, unique, same type
type = map(string)         # string keys, same-type values

# Structural types
type = object({
  name    = string
  port    = number
  enabled = bool
})

type = tuple([string, number, bool])  # fixed-length, mixed types

# Any type (no validation)
type = any
```

### Providing Variable Values

```bash
# 1. terraform.tfvars file (auto-loaded)
environment   = "dev"
instance_type = "t3.micro"

# 2. *.auto.tfvars file (auto-loaded, any name)
# staging.auto.tfvars

# 3. -var flag at CLI
terraform apply -var="environment=prod"

# 4. -var-file flag at CLI
terraform apply -var-file="prod.tfvars"

# 5. Environment variable  TF_VAR_<name>
export TF_VAR_environment=prod

# Precedence (highest to lowest):
# -var / -var-file CLI flags
# *.auto.tfvars (alphabetical)
# terraform.tfvars
# TF_VAR_ environment variables
# variable default value
```

---

## Outputs

Outputs expose values after `terraform apply` — for human inspection, for use by other Terraform configs (via `terraform_remote_state`), or for CI/CD pipelines.

```hcl
# outputs.tf
output "vpc_id" {
  value       = aws_vpc.main.id
  description = "ID of the VPC"
}

output "instance_public_ip" {
  value       = aws_instance.web.public_ip
  description = "Public IP of the web instance"
}

# Sensitive output — value is hidden in CLI output
output "db_connection_string" {
  value     = "postgresql://${var.db_user}:${var.db_password}@${aws_db_instance.main.endpoint}/mydb"
  sensitive = true
}

# Output a map
output "subnet_ids" {
  value       = { for k, v in aws_subnet.private : k => v.id }
  description = "Map of subnet name to ID"
}
```

```bash
# View outputs after apply
terraform output
terraform output vpc_id
terraform output -json    # all outputs as JSON (for CI/CD)
terraform output -raw vpc_id  # raw value without quotes (for shell scripts)
```

---

## Locals

Locals are intermediate computed values — not inputs (unlike variables) and not outputs. They reduce repetition and give names to complex expressions.

```hcl
# locals.tf
locals {
  # Simple string concatenation
  name_prefix = "${var.project}-${var.environment}"
  
  # Common tags applied to all resources
  common_tags = merge(var.tags, {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
    Owner       = var.team
  })
  
  # Conditional value
  is_prod = var.environment == "prod"
  
  # Computed CIDR blocks
  vpc_cidr = var.environment == "prod" ? "10.0.0.0/16" : "10.1.0.0/16"
  
  # Map transformation
  az_to_subnet = { for i, az in var.availability_zones : az => cidrsubnet(local.vpc_cidr, 8, i) }
}

# Usage: local.<name>
resource "aws_vpc" "main" {
  cidr_block = local.vpc_cidr
  tags       = merge(local.common_tags, { Name = "${local.name_prefix}-vpc" })
}
```

---

## Terraform Block (Version Constraints)

```hcl
terraform {
  required_version = ">= 1.5.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"   # ~> 5.0 means >= 5.0, < 6.0
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.20.0"
    }
  }

  # Backend configuration (see Sheet 4 and 14)
  backend "s3" {
    bucket         = "my-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
  }
}
```

### Version Constraint Operators

| Operator | Meaning | Example |
|---|---|---|
| `= 1.5.0` | Exact version only | `= 5.1.0` |
| `>= 1.5.0` | This version or higher | `>= 5.0` |
| `~> 1.5` | Minor updates only (`>= 1.5, < 2.0`) | `~> 5.0` |
| `~> 1.5.0` | Patch updates only (`>= 1.5.0, < 1.6.0`) | `~> 5.1.0` |
| `!= 1.5.3` | Exclude specific version | |

---

## String Interpolation And Expressions

```hcl
# Template string interpolation
name = "web-${var.environment}-${count.index}"

# Multi-line string (heredoc)
user_data = <<-EOT
  #!/bin/bash
  echo "Environment: ${var.environment}" > /etc/env
  apt-get update && apt-get install -y nginx
EOT

# Conditional expression (ternary)
instance_type = var.environment == "prod" ? "t3.large" : "t3.micro"

# Null coalescing via try()
region = try(var.region, "us-east-1")

# Null coalescing via coalesce()
name = coalesce(var.custom_name, local.default_name)
```

---

## Interview Sound Bite

HCL has five main building blocks: `resource` (creates infrastructure), `data` (reads existing infrastructure), `variable` (input parameters), `output` (exposed values), and `locals` (intermediate computed values). Variables have types (string, number, bool, list, map, object), defaults, descriptions, and validation rules. The `sensitive = true` flag hides values from plan/apply output and logs. Locals are the right place for computed values reused across multiple resources — they eliminate repetition without becoming inputs. Version constraints use `~>` for pessimistic constraint: `~> 5.0` means `>= 5.0, < 6.0`.
