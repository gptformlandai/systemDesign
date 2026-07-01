# Terraform Mastery Sheet System

## What Terraform Mastery Means

Terraform mastery is not memorizing CLI flags. It is about:

- Understanding the **plan/apply/state loop** and why it is the foundation of everything
- Writing **modular, reusable HCL** that scales across teams without copy-paste
- Operating **remote state safely** — locking, migration, recovery
- Navigating **multi-account, multi-region** infrastructure without provider confusion
- Knowing which **lifecycle rule** to use and why (`create_before_destroy` is not always right)
- Debugging **drift** between state and real infrastructure
- Running Terraform in **CI/CD** without secrets exposure or blast-radius accidents

---

## The Terraform Execution Model

```text
Terraform execution flow:

  1. terraform init
     → downloads providers (plugins for AWS/GCP/Azure etc.)
     → initializes backend (where state is stored)
     → installs modules

  2. terraform plan
     → reads current state (from backend)
     → calls provider APIs to refresh actual resource state
     → computes diff: desired (HCL) vs actual (state)
     → outputs execution plan (what will be created/updated/destroyed)

  3. terraform apply
     → executes the plan
     → calls provider APIs to create/modify/delete resources
     → writes new state to backend

  4. terraform destroy
     → plan: all resources will be destroyed
     → apply: deletes all managed resources
```

---

## The State File

```text
terraform.tfstate (local) or remote equivalent

Contains:
  - Every resource Terraform manages
  - Resource IDs (aws_instance.web → i-0abc123)
  - All attributes of every resource
  - Dependencies between resources
  - Provider metadata

Why it matters:
  - Terraform ONLY knows about infrastructure it has recorded in state.
  - Resources created outside Terraform are invisible to it (drift).
  - State is the single source of truth for the plan computation.

Never:
  - Edit state manually (use terraform state commands or moved blocks)
  - Commit state to git (it contains secrets)
  - Delete state without thinking (you'll lose the reference to real resources)
```

---

## HCL Building Blocks

```hcl
# Provider — defines the cloud/API to connect to
provider "aws" {
  region = "us-east-1"
}

# Resource — creates a piece of infrastructure
resource "aws_instance" "web" {
  ami           = "ami-0c55b159cbfafe1f0"
  instance_type = "t3.micro"
}

# Data Source — reads existing infrastructure (does not create)
data "aws_ami" "ubuntu" {
  most_recent = true
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-*-22.04-amd64-server-*"]
  }
  owners = ["099720109477"]
}

# Variable — input parameter
variable "instance_type" {
  type        = string
  default     = "t3.micro"
  description = "EC2 instance type"
  validation {
    condition     = contains(["t3.micro", "t3.small", "t3.medium"], var.instance_type)
    error_message = "Must be a valid t3 instance type."
  }
}

# Output — expose values after apply
output "instance_ip" {
  value       = aws_instance.web.public_ip
  description = "Public IP of the web server"
}

# Local — intermediate computed value (no input, not output)
locals {
  name_prefix = "${var.environment}-${var.project}"
  common_tags = {
    Environment = var.environment
    Project     = var.project
    ManagedBy   = "terraform"
  }
}
```

---

## Resource Reference Syntax

```hcl
# Reference: <resource_type>.<name>.<attribute>
aws_instance.web.id
aws_instance.web.public_ip
aws_s3_bucket.data.arn
aws_db_instance.main.endpoint

# Data source reference: data.<type>.<name>.<attribute>
data.aws_ami.ubuntu.id
data.aws_vpc.main.cidr_block

# Module output reference: module.<name>.<output>
module.vpc.vpc_id
module.eks.cluster_endpoint
```

---

## The Four Most Important Concepts

```text
1. State
   Everything Terraform manages is recorded in state.
   Remote state = shared across teams.
   Locking = only one apply at a time.

2. Plan/Apply Separation
   plan = safe read-only diff (never modifies anything)
   apply = actually changes infrastructure
   Always review plans before applying in production.

3. Idempotency
   Running terraform apply multiple times with the same config
   produces the same result. Terraform calculates the minimum
   changes needed each time.

4. Dependency Graph
   Terraform builds a DAG of all resources.
   Resources with no dependency run in parallel.
   depends_on forces explicit ordering when implicit isn't enough.
```

---

## Terraform Version Reference

```text
Terraform 0.12  → HCL2, proper types, for_each
Terraform 0.13  → module providers, count in modules
Terraform 0.14  → sensitive values in state
Terraform 0.15  → stable preconditions API
Terraform 1.0   → stable, backwards compatible guarantee
Terraform 1.1   → moved blocks (rename without destroy)
Terraform 1.2   → preconditions/postconditions on resources
Terraform 1.3   → optional object attributes, null_resource replacement
Terraform 1.4   → provider functions
Terraform 1.5   → check blocks (ongoing health assertions)
Terraform 1.6   → terraform test (unit tests for modules)
Terraform 1.7+  → mock providers for testing

Current (2024/2025): Terraform 1.9.x / OpenTofu 1.8.x (open-source fork)
```

---

## Mental Model: Terraform Is Not A Script

```text
Wrong mental model (imperative):
  "Run this script to create a VPC, then run this to create EC2"

Right mental model (declarative):
  "I want this infrastructure to exist. Terraform makes it so."
  "I want this configuration to change. Terraform figures out the minimal diff."
  "I deleted this block. Terraform will destroy the resource."

The key insight:
  Your HCL code IS the documentation of what exists.
  The state file IS the runtime record.
  terraform plan IS the change audit.
  terraform apply IS the deployment.
```
