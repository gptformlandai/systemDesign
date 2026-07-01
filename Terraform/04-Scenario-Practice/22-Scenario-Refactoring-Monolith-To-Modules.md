# 22. Scenario: Refactoring a Monolith Into Modules

## The Problem

```text
Starting point: one large flat config with hundreds of resources
  root/
    main.tf           ← 800 lines: VPC, EC2, RDS, IAM, Route53, all together
    variables.tf
    outputs.tf

Problems:
  - All resources in one state file: one mistake can destroy everything
  - No reusability: can't use VPC code in another project
  - Slow plans: 800 resources to refresh
  - Team conflicts: everyone edits the same file
  - Testing is impossible

Goal: extract into modules WITHOUT destroying and recreating resources
     (zero downtime, no resource replacement)
```

---

## Step 1: Plan The New Structure

```text
root/
  main.tf               ← thin root: calls modules, wires outputs
  variables.tf
  outputs.tf
  versions.tf
  modules/
    vpc/
      main.tf           ← extracted VPC resources
      variables.tf
      outputs.tf
    compute/
      main.tf           ← EC2, ASG, ALB
      variables.tf
      outputs.tf
    database/
      main.tf           ← RDS
      variables.tf
      outputs.tf
    iam/
      main.tf           ← IAM roles, policies
      variables.tf
      outputs.tf
```

---

## Step 2: Extract VPC Resources Into A Module

### Before (flat config in main.tf)

```hcl
# main.tf (monolith)
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = { Name = "prod-vpc" }
}

resource "aws_subnet" "public_1" {
  vpc_id     = aws_vpc.main.id
  cidr_block = "10.0.1.0/24"
  # ...
}

resource "aws_subnet" "private_1" {
  vpc_id     = aws_vpc.main.id
  cidr_block = "10.0.11.0/24"
  # ...
}
```

### After (module)

```hcl
# modules/vpc/main.tf
resource "aws_vpc" "this" {
  cidr_block           = var.cidr_block
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = { Name = var.name }
}

resource "aws_subnet" "public" {
  count      = length(var.public_cidrs)
  vpc_id     = aws_vpc.this.id
  cidr_block = var.public_cidrs[count.index]
  # ...
}

# modules/vpc/outputs.tf
output "vpc_id" { value = aws_vpc.this.id }
output "public_subnet_ids" { value = aws_subnet.public[*].id }

# main.tf (root)
module "vpc" {
  source     = "./modules/vpc"
  name       = "prod-vpc"
  cidr_block = "10.0.0.0/16"
  public_cidrs = ["10.0.1.0/24"]
}
```

---

## Step 3: Write moved Blocks (Critical — Prevents Destroy)

Without `moved` blocks, Terraform sees the old address (`aws_vpc.main`) removed from config and plans to destroy the real VPC. The new address (`module.vpc.aws_vpc.this`) doesn't exist in state yet, so Terraform plans to create a new one.

`moved` blocks tell Terraform: "the resource at address A is now at address B — update state only."

```hcl
# moved.tf (or include in main.tf)

# VPC
moved {
  from = aws_vpc.main
  to   = module.vpc.aws_vpc.this
}

# Subnets (flat → count-indexed)
moved {
  from = aws_subnet.public_1
  to   = module.vpc.aws_subnet.public[0]
}

moved {
  from = aws_subnet.private_1
  to   = module.vpc.aws_subnet.private[0]
}

# Route tables
moved {
  from = aws_route_table.public
  to   = module.vpc.aws_route_table.public
}

# Internet gateway
moved {
  from = aws_internet_gateway.this
  to   = module.vpc.aws_internet_gateway.this
}
```

---

## Step 4: Verify With Plan

```bash
# Plan should show ONLY moves, no destroys/creates
terraform plan -no-color 2>&1 | tee refactor-plan.txt

# Look for these lines (correct):
# aws_vpc.main has moved to module.vpc.aws_vpc.this
# aws_subnet.public_1 has moved to module.vpc.aws_subnet.public[0]

# If you see "will be destroyed" — a moved block is missing!
# Never apply if plan shows unexpected destroys during a refactoring.

grep -E "will be destroyed|will be created|moved" refactor-plan.txt
```

---

## Step 5: Apply The Refactoring

```bash
# Backup state before refactoring (belt and suspenders)
terraform state pull > backup-before-refactor.json

# Apply the moved blocks (state-only operation, no API calls for moved resources)
terraform apply

# Verify state addresses are updated
terraform state list | grep module.vpc
# module.vpc.aws_vpc.this
# module.vpc.aws_subnet.public[0]
# module.vpc.aws_subnet.private[0]
```

---

## Step 6: Clean Up moved Blocks (Optional)

```text
moved blocks can remain in the config permanently — they are harmless after apply.
They serve as documentation of the rename history.

If you want to remove them:
  Wait until all team members and all environments have run apply.
  Then remove the moved blocks in a separate PR.
  Removing a moved block BEFORE applying it will cause Terraform to try
  to destroy the old address.
```

---

## Handling Count → for_each During Refactoring

```hcl
# BEFORE: using count
resource "aws_security_group" "web" {
  count = 2
  name  = "web-sg-${count.index}"
}

# AFTER: using for_each with meaningful keys
resource "aws_security_group" "web" {
  for_each = toset(["web-sg-0", "web-sg-1"])
  name     = each.key
}

# moved blocks for count → for_each migration
moved {
  from = aws_security_group.web[0]
  to   = aws_security_group.web["web-sg-0"]
}

moved {
  from = aws_security_group.web[1]
  to   = aws_security_group.web["web-sg-1"]
}
```

---

## Handling Nested Module Extractions

```hcl
# If you already have a flat module and want to nest it:
# Before: root → module.vpc
# After:  root → module.network → module.vpc

moved {
  from = module.vpc
  to   = module.network.module.vpc
}
```

---

## Refactoring Checklist

```text
Before starting:
  ✓ Backup state: terraform state pull > backup.json
  ✓ Ensure no one else is applying (state lock)
  ✓ Create feature branch for the refactoring

During refactoring:
  ✓ Move resource blocks to module
  ✓ Add variables.tf and outputs.tf to module
  ✓ Add module block to root main.tf
  ✓ Write moved block for EVERY resource moved
  ✓ Run terraform plan and verify: only moves, no destroys
  ✓ If plan shows destroys: add missing moved blocks

After applying:
  ✓ Run terraform plan again: "No changes"
  ✓ Update moved.tf: keep blocks for a few weeks, then clean up
  ✓ Update any CI/CD scripts that reference old resource addresses
  ✓ Communicate the change to team: new addresses for terraform state show
```

---

## Interview Sound Bite

Refactoring a monolithic Terraform config into modules is a zero-downtime operation when done with `moved` blocks. The process: create the module directory structure, move resource blocks from root to modules, add a `module {}` call in root, and write a `moved {}` block for every resource that changed address. `terraform plan` with `moved` blocks shows "X has moved to Y" — not create or destroy. If the plan shows any destroy, a `moved` block is missing. Never apply a refactoring plan that shows unexpected destroys. Keep `moved` blocks in VCS after applying — they serve as documentation and are harmless to leave in permanently.
