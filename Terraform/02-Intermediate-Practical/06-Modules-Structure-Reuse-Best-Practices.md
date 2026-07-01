# 06. Modules: Structure, Reuse, Best Practices

## What Is A Module?

A module is a container for a group of related resources. Every Terraform configuration is technically a module — the "root" module. Child modules are called by the root module or by other modules.

```text
Root module:         Your main configuration (the directory where you run terraform)
Child module:        A reusable component called via a module block
Published module:    Available on the Terraform Registry (registry.terraform.io/modules)
```

---

## Module Directory Structure

```text
modules/
  vpc/
    main.tf         ← resource definitions
    variables.tf    ← input variables
    outputs.tf      ← output values
    versions.tf     ← provider version constraints (optional in child modules)
    README.md       ← documentation
```

### Example: VPC Module

```hcl
# modules/vpc/variables.tf
variable "name" {
  type        = string
  description = "VPC name"
}

variable "cidr_block" {
  type        = string
  description = "VPC CIDR block"
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  type        = list(string)
  description = "List of AZs to create subnets in"
}

variable "private_subnet_cidrs" {
  type        = list(string)
  description = "CIDRs for private subnets (must match AZ count)"
}

variable "public_subnet_cidrs" {
  type        = list(string)
  description = "CIDRs for public subnets (must match AZ count)"
}

variable "tags" {
  type    = map(string)
  default = {}
}
```

```hcl
# modules/vpc/main.tf
resource "aws_vpc" "this" {
  cidr_block           = var.cidr_block
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = merge(var.tags, { Name = var.name })
}

resource "aws_subnet" "public" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.this.id
  cidr_block        = var.public_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
  map_public_ip_on_launch = true
  tags = merge(var.tags, {
    Name = "${var.name}-public-${count.index + 1}"
    Type = "public"
  })
}

resource "aws_subnet" "private" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
  tags = merge(var.tags, {
    Name = "${var.name}-private-${count.index + 1}"
    Type = "private"
  })
}
```

```hcl
# modules/vpc/outputs.tf
output "vpc_id" {
  value       = aws_vpc.this.id
  description = "ID of the VPC"
}

output "public_subnet_ids" {
  value       = aws_subnet.public[*].id
  description = "IDs of the public subnets"
}

output "private_subnet_ids" {
  value       = aws_subnet.private[*].id
  description = "IDs of the private subnets"
}
```

---

## Calling A Module

```hcl
# main.tf (root module)
module "vpc" {
  source = "./modules/vpc"   # local path

  name                 = "prod-vpc"
  cidr_block           = "10.0.0.0/16"
  availability_zones   = ["us-east-1a", "us-east-1b", "us-east-1c"]
  public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  private_subnet_cidrs = ["10.0.11.0/24", "10.0.12.0/24", "10.0.13.0/24"]
  tags                 = local.common_tags
}

# Use module outputs in other resources
resource "aws_lb" "web" {
  name            = "web-alb"
  subnets         = module.vpc.public_subnet_ids
  security_groups = [aws_security_group.alb.id]
}
```

---

## Module Sources

```hcl
# Local directory
source = "./modules/vpc"
source = "../shared-modules/vpc"

# Terraform Registry (public)
source  = "terraform-aws-modules/vpc/aws"
version = "~> 5.0"

# GitHub
source = "github.com/myorg/terraform-modules//modules/vpc"

# GitHub with tag/branch/commit
source = "github.com/myorg/terraform-modules//modules/vpc?ref=v1.2.0"
source = "github.com/myorg/terraform-modules//modules/vpc?ref=main"

# Generic Git
source = "git::https://gitlab.com/myorg/terraform-modules.git//modules/vpc?ref=v1.0"

# S3 bucket (private, for enterprise)
source = "s3::https://s3.amazonaws.com/my-tf-modules/vpc.zip"

# Terraform Cloud private registry
source  = "app.terraform.io/my-org/vpc/aws"
version = "~> 1.0"
```

---

## Module Versioning Best Practices

```hcl
# Always pin module version in production (prevents accidental upgrades)
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.1.2"   # exact version for production
  # ...
}

# In development: allow minor updates
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.1"
  # ...
}
```

---

## Module Output References

```hcl
# After calling a module, reference its outputs via:
module.<MODULE_NAME>.<OUTPUT_NAME>

module.vpc.vpc_id
module.vpc.private_subnet_ids
module.eks.cluster_endpoint
module.rds.db_instance_endpoint
```

---

## Module Meta-Arguments

```hcl
module "app" {
  source = "./modules/app"

  # count: create multiple instances of a module
  count = var.environment == "prod" ? 2 : 1

  # for_each: create a module per key
  for_each = toset(["us-east-1", "eu-west-1"])
  region   = each.value

  # depends_on: explicit ordering when implicit dependency isn't detected
  depends_on = [module.vpc]

  # providers: pass specific provider aliases into the module
  providers = {
    aws = aws.eu_west
  }
}
```

---

## Module Best Practices

```text
1. Single responsibility
   Each module does one job: vpc, eks, rds, iam-role.
   Avoid "kitchen sink" modules that provision an entire stack.

2. Expose inputs for everything likely to change
   Don't hardcode instance types, CIDR blocks, or region.

3. Use outputs generously
   Modules should output everything the caller might need.
   IDs, ARNs, endpoints, names.

4. Version-pin all external modules
   Use exact versions in production configs.

5. Document with README and variable descriptions
   description field in every variable and output.

6. Avoid provider configuration in child modules
   Let the root module configure providers and pass them to children.
   (Provider configs in child modules make them less reusable.)

7. Don't use provider-specific resources in "generic" modules
   A "tagging" module should not import AWS-specific tagging resources.
```

---

## Popular Public Modules (AWS)

```text
terraform-aws-modules/vpc/aws              ← VPC with all subnets, NAT, IGW
terraform-aws-modules/eks/aws              ← EKS cluster + node groups
terraform-aws-modules/rds/aws              ← RDS instances/clusters
terraform-aws-modules/s3-bucket/aws        ← S3 with policies/lifecycle
terraform-aws-modules/iam/aws              ← IAM roles/policies
terraform-aws-modules/security-group/aws   ← Security groups with rules
terraform-aws-modules/alb/aws              ← Application Load Balancer
```

---

## Interview Sound Bite

Modules are the unit of reuse in Terraform — a directory with `variables.tf`, `main.tf`, and `outputs.tf`. The root module calls child modules via `module` blocks, passing input variables and consuming outputs. Module sources can be local paths, the public Terraform Registry, or private Git repos. Always pin module versions with exact tags in production configs. Child modules should not configure providers — that belongs in the root module. Pass provider aliases to modules via the `providers` meta-argument for cross-account or cross-region use.
