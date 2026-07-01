# 13. Module Design Patterns: Composition, Root, Child

## Root vs Child Module

```text
Root module:
  - The directory where you run `terraform` commands
  - Owns provider configuration and backend configuration
  - Calls child modules, wires them together
  - Maintains the "composition" of your infrastructure

Child module:
  - A reusable unit called by root (or by another child) via module block
  - Receives input via variables, returns data via outputs
  - Should NOT configure providers (inherits from caller)
  - Should NOT configure backends (state is root-level concern)
```

---

## Module Design Philosophy

### 1. Thin Root, Fat Modules

```text
Anti-pattern (fat root):
  The root module has hundreds of resources directly.
  Difficult to reuse, test, or reason about.

Pattern (thin root):
  Root module = glue code only.
  Calls vpc module, eks module, rds module, dns module.
  Passes outputs from one module to another.
  All actual resource definitions live in child modules.
```

### 2. Single-Responsibility Modules

```text
vpc/          ← networks only
eks/          ← Kubernetes cluster + node groups
rds/          ← database instances
iam-role/     ← IAM role + policy attachments
alb/          ← load balancer + target groups + listeners
route53/      ← DNS records

NOT: "application-stack" that creates vpc + eks + rds + alb in one module.
  (Too opinionated, hard to reuse, violates SRP)
```

---

## Module Structure Reference

```text
modules/
  vpc/
    main.tf           ← resource definitions
    variables.tf      ← input variable declarations
    outputs.tf        ← output value declarations
    versions.tf       ← optional: required_providers (no backend)
    data.tf           ← optional: data sources
    locals.tf         ← optional: locals for complex expressions
    README.md         ← REQUIRED: usage example + variable table
```

---

## Input Variable Design

```hcl
# Good: explicit types, descriptions, validations
variable "cluster_name" {
  type        = string
  description = "EKS cluster name. Used as a prefix for all cluster resources."

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,38}[a-z0-9]$", var.cluster_name))
    error_message = "cluster_name must be 4-40 lowercase alphanumeric characters or hyphens."
  }
}

# Object type for structured config
variable "node_group" {
  type = object({
    name           = string
    instance_types = list(string)
    min_size       = number
    max_size       = number
    desired_size   = number
    disk_size_gb   = optional(number, 50)   # optional with default (TF 1.3+)
  })
  description = "Configuration for the EKS node group"
}

# Optional variable (has a default)
variable "enable_cluster_autoscaler" {
  type        = bool
  default     = true
  description = "Whether to install Cluster Autoscaler"
}
```

---

## Output Design

```hcl
# outputs.tf — expose everything the caller might need

output "cluster_id" {
  value       = aws_eks_cluster.this.id
  description = "EKS cluster ID"
}

output "cluster_name" {
  value       = aws_eks_cluster.this.name
  description = "EKS cluster name"
}

output "cluster_endpoint" {
  value       = aws_eks_cluster.this.endpoint
  description = "EKS cluster API server endpoint"
}

output "cluster_ca_certificate" {
  value       = aws_eks_cluster.this.certificate_authority[0].data
  description = "Base64-encoded cluster certificate authority data"
  sensitive   = true
}

output "cluster_oidc_issuer_url" {
  value       = aws_eks_cluster.this.identity[0].oidc[0].issuer
  description = "OIDC issuer URL for IRSA configuration"
}

output "node_group_role_arn" {
  value       = aws_iam_role.node_group.arn
  description = "IAM role ARN for node group (needed for aws-auth ConfigMap)"
}
```

---

## Module Composition: Wiring Modules Together

```hcl
# main.tf (root module)

# Step 1: Network layer
module "vpc" {
  source = "./modules/vpc"

  name               = "${local.name_prefix}-vpc"
  cidr_block         = "10.0.0.0/16"
  availability_zones = data.aws_availability_zones.available.names
  private_subnet_cidrs = ["10.0.10.0/24", "10.0.11.0/24", "10.0.12.0/24"]
  public_subnet_cidrs  = ["10.0.1.0/24",  "10.0.2.0/24",  "10.0.3.0/24"]
  tags               = local.common_tags
}

# Step 2: EKS cluster (uses VPC outputs)
module "eks" {
  source = "./modules/eks"

  cluster_name        = local.name_prefix
  kubernetes_version  = "1.29"
  subnet_ids          = module.vpc.private_subnet_ids  # VPC output → EKS input
  vpc_id              = module.vpc.vpc_id
  tags                = local.common_tags
}

# Step 3: RDS (uses VPC outputs)
module "rds" {
  source = "./modules/rds"

  identifier    = "${local.name_prefix}-db"
  engine        = "postgres"
  instance_class = "db.t3.medium"
  subnet_ids    = module.vpc.private_subnet_ids
  vpc_id        = module.vpc.vpc_id
  tags          = local.common_tags
}

# Step 4: Route53 DNS record for RDS (uses RDS outputs)
resource "aws_route53_record" "db" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "db.${var.domain}"
  type    = "CNAME"
  ttl     = 300
  records = [module.rds.endpoint]   # RDS output
}
```

---

## Module Anti-Patterns

```text
1. Hardcoded values inside modules
   BAD:  region = "us-east-1"  (inside a module)
   GOOD: let provider or variable set the region

2. No outputs
   Modules that create resources but expose nothing are impossible to compose.
   Always output IDs, ARNs, endpoints.

3. God module
   One module that creates a VPC + EKS + RDS + monitoring + IAM + DNS.
   Hard to test, hard to reuse partial pieces, one failure breaks everything.

4. Provider configuration inside child module
   provider "aws" { ... } inside a child module causes "provider inheritance" issues.
   Only configure providers in root module.

5. Calling modules from inside another child module (deep nesting)
   More than 2 levels of nesting (root → module → module) makes debugging painful.
   Prefer flat composition in root module.

6. Not pinning external module versions
   source = "terraform-aws-modules/eks/aws"  # no version!
   A new version can break your infrastructure.
   Always: version = "~> 20.0"
```

---

## Mono-Repo vs Multi-Repo Module Strategy

```text
Mono-repo:
  All modules in one git repo: github.com/myorg/terraform-modules
    modules/vpc/
    modules/eks/
    modules/rds/
  
  Advantages:
    - Easy cross-module refactoring
    - Single PR to update multiple modules
    - Simple for small/medium teams
  
  Disadvantages:
    - All teams share one repo; noisy PRs
    - A breaking change in vpc affects everyone

Multi-repo:
  Each module has its own git repo + semantic version tags:
    github.com/myorg/terraform-module-vpc    v1.2.0
    github.com/myorg/terraform-module-eks    v3.1.0
  
  Advantages:
    - Independent versioning per module
    - Clean changelog per module
    - Teams own their modules
  
  Disadvantages:
    - Many repos to maintain
    - Cross-module changes require multiple PRs

Decision: mono-repo for teams < 50 engineers; consider multi-repo at scale.
```

---

## Module Testing Strategy

```text
1. Unit tests (plan only, no real cloud):
   terraform test with command = plan
   Verify naming conventions, tag rules, required outputs exist

2. Integration tests (real apply, then destroy):
   terraform test with command = apply
   Verify the module creates what it should, outputs are non-empty

3. Example configurations in examples/ directory:
   examples/basic/     → minimal working example
   examples/complete/  → all features enabled
   These serve as documentation AND test fixtures

4. Terratest (Go):
   For complex modules, write Go tests that apply + validate + destroy
```

---

## Interview Sound Bite

A well-designed module has a single responsibility, exposes everything useful through outputs, validates all inputs, and never configures providers (that belongs in root). Root modules should be thin: they call child modules and wire their outputs to other modules' inputs. The composition pattern passes `module.vpc.private_subnet_ids` directly into `module.eks.subnet_ids`. Module anti-patterns to call out in interviews: hardcoded values, no outputs, god modules, and un-pinned external module versions. For teams, pin external module versions to exact tags in production and document with a README containing a usage example and variable table.
