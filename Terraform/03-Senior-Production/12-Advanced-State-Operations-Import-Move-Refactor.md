# 12. Advanced State Operations: Import, Move, Refactor

## When Do You Need Advanced State Operations?

```text
1. Existing infrastructure → bring under Terraform management (import)
2. Rename a resource in HCL → update state without destroy/recreate (moved block)
3. Extract resources into a module → update state address (moved block)
4. Remove a resource from Terraform management (state rm)
5. Emergency state recovery (state pull/push)
6. Replace a provider in state (state replace-provider)
```

---

## terraform import (CLI Method)

```bash
# Syntax:
terraform import <RESOURCE_ADDRESS> <REAL_RESOURCE_ID>

# Import an EC2 instance
terraform import aws_instance.web i-0abc123def456789

# Import a VPC
terraform import aws_vpc.main vpc-0abc1234

# Import a security group
terraform import aws_security_group.web sg-0abc1234

# Import a specific index when using count
terraform import "aws_subnet.public[0]" subnet-0abc1111

# Import into a for_each resource
terraform import 'aws_subnet.this["public-1"]' subnet-0abc1111

# Import into a module
terraform import module.vpc.aws_vpc.this vpc-0abc1234
```

### Import Workflow (CLI Method)

```text
1. Write the resource block in HCL (the resource MUST exist in config)
   resource "aws_instance" "web" {
     # at minimum: enough arguments to make it valid
   }

2. Run terraform import
   terraform import aws_instance.web i-0abc123def456789

3. Run terraform plan
   → Shows any differences between HCL and actual state
   → Update HCL until plan shows "No changes"
```

---

## Import Blocks (Terraform 1.5+ — Declarative Import)

```hcl
# import.tf — declare what to import
import {
  to = aws_instance.web
  id = "i-0abc123def456789"
}

import {
  to = aws_vpc.main
  id = "vpc-0abc1234"
}

import {
  to = module.vpc.aws_vpc.this
  id = "vpc-0abc1234"
}
```

```bash
# Plan with import blocks: generates resource config + shows diff
terraform plan -generate-config-out=generated.tf

# Apply: imports resources, writes them to state
terraform apply
```

### Benefits Of Import Blocks

```text
- Declarative: import logic lives in version-controlled HCL
- -generate-config-out flag creates the resource block for you
- Safe to share in team review (unlike ad-hoc CLI commands)
- After apply, remove import blocks (they're one-time use)
```

---

## moved Blocks (Terraform 1.1+ — Declarative State Move)

`moved` blocks tell Terraform "this resource was renamed — don't destroy the old one, just update its address in state."

```hcl
# Rename a resource
moved {
  from = aws_instance.web
  to   = aws_instance.api_server
}

# Move from root to module
moved {
  from = aws_security_group.web
  to   = module.app.aws_security_group.web
}

# Move within for_each (count → for_each migration)
moved {
  from = aws_subnet.public[0]
  to   = aws_subnet.public["us-east-1a"]
}

# Move module to sub-module
moved {
  from = module.old_vpc
  to   = module.network.module.vpc
}
```

```text
When to use moved blocks vs terraform state mv:

  moved blocks (preferred):
    - Committed to git: teammates get the rename applied on their next plan
    - Shows in plan output: "aws_instance.web has moved to aws_instance.api_server"
    - Safe, reviewable
    - Keep them permanently or remove after everyone has applied

  terraform state mv (manual):
    - Immediate, doesn't require an apply
    - Not committed to VCS — others still see the old name
    - One-off emergency use
    - Must be communicated to team
```

---

## terraform state rm

```bash
# Remove from state WITHOUT destroying the real resource
# Use case: you want to manage this resource elsewhere, or un-adopt it

terraform state rm aws_instance.old_worker

# Remove all resources in a module
terraform state rm module.old_module

# Remove a specific for_each resource
terraform state rm 'aws_security_group.this["web"]'
```

---

## terraform state mv

```bash
# Rename resource address in state
terraform state mv aws_instance.web aws_instance.api

# Move resource into a module
terraform state mv aws_vpc.main module.network.aws_vpc.main

# Move between modules
terraform state mv module.app.aws_instance.web module.api.aws_instance.web

# ALWAYS backup state before mv operations:
terraform state pull > backup-$(date +%Y%m%d-%H%M%S).tfstate
```

---

## terraform state pull / push

```bash
# Pull state from remote backend to stdout (for inspection or backup)
terraform state pull > state-backup.json
terraform state pull | python3 -m json.tool | head -100

# Push local state to remote (DANGEROUS — overwrites remote state)
# Only use for emergency recovery after confirming no apply is running
terraform state push state-backup.json

# Push with force (bypasses serial number check — very dangerous)
terraform state push -force state-backup.json
```

---

## Refactoring: Count To for_each Migration

```hcl
# BEFORE: using count
resource "aws_security_group" "web" {
  count  = 3
  name   = "web-sg-${count.index}"
}

# AFTER: using for_each
resource "aws_security_group" "web" {
  for_each = toset(["web-1", "web-2", "web-3"])
  name     = each.key
}

# moved blocks to map old indices to new keys
moved {
  from = aws_security_group.web[0]
  to   = aws_security_group.web["web-1"]
}
moved {
  from = aws_security_group.web[1]
  to   = aws_security_group.web["web-2"]
}
moved {
  from = aws_security_group.web[2]
  to   = aws_security_group.web["web-3"]
}
```

---

## Refactoring: Extract Resources Into A Module

```hcl
# BEFORE: resource in root module
resource "aws_vpc" "main" { ... }
resource "aws_subnet" "public" { ... }

# AFTER: moved into module
module "network" {
  source = "./modules/network"
}

# moved blocks
moved {
  from = aws_vpc.main
  to   = module.network.aws_vpc.main
}
moved {
  from = aws_subnet.public
  to   = module.network.aws_subnet.public
}
```

---

## Interview Sound Bite

Terraform offers two import methods: the CLI (`terraform import <address> <id>`) requires a pre-written HCL block, while `import {}` blocks in Terraform 1.5+ are declarative and can auto-generate the HCL with `-generate-config-out`. For renaming resources without downtime, `moved` blocks are the preferred approach over `terraform state mv` — they live in VCS, show up in plan output, and are applied automatically when teammates run `terraform apply`. `terraform state rm` orphans a resource from Terraform management without deleting it. `state pull/push` are emergency tools for state backup and recovery, used only when no other apply is running.
