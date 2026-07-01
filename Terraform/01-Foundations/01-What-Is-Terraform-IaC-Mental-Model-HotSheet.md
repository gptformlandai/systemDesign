# 01. What Is Terraform: IaC Mental Model

## What Is Infrastructure as Code?

Infrastructure as Code (IaC) means your infrastructure — servers, databases, networks, DNS, IAM roles — is defined in text files that can be version-controlled, reviewed, tested, and automated.

```text
Without IaC:
  Engineer clicks through AWS console → infrastructure exists
  Nobody knows what was clicked → drift accumulates
  Recreating it requires memory or guesswork

With IaC:
  Engineer writes HCL → git commit → PR review → terraform apply
  Every change is tracked, auditable, repeatable
  Any environment can be recreated from the code
```

---

## Why Terraform Specifically

Terraform is the dominant multi-cloud IaC tool because:

```text
Multi-cloud:       One tool for AWS + GCP + Azure + Kubernetes + Datadog + GitHub
Declarative:       You describe desired state; Terraform handles change computation
Large ecosystem:   4,000+ providers in the public registry
State management:  Tracks what it manages; detects drift
Plan before apply: Dry-run shows exactly what will change before any API call
Open source:       Community providers, modules; large Stack Overflow/GitHub coverage
```

---

## Declarative vs Imperative IaC

```text
Imperative (Ansible/shell scripts):
  "Create a VPC with CIDR 10.0.0.0/16"
  "If the VPC already exists, skip"
  "If it has the wrong CIDR, delete it and recreate"
  (You write the logic for every scenario)

Declarative (Terraform):
  resource "aws_vpc" "main" {
    cidr_block = "10.0.0.0/16"
  }
  Terraform figures out: create / update / skip / destroy
  You only describe WHAT you want.
```

---

## The Provider Model

Every Terraform resource is managed by a **provider** — a plugin that wraps a cloud or service API.

```text
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0"
    }
  }
}
```

```text
Provider download location:
  registry.terraform.io/hashicorp/aws
  registry.terraform.io/<namespace>/<name>

terraform init:
  Downloads provider binaries to .terraform/providers/
  Creates .terraform.lock.hcl with exact checksums

Provider responsibilities:
  Authenticate to the API (AWS credentials, GCP service account, etc.)
  Translate HCL resource blocks to API calls
  Report resource attributes back to state
```

---

## Terraform Resource Lifecycle

```text
Create:   resource block added to HCL + terraform apply
            → provider.Create() API call
            → resource created in cloud
            → resource recorded in state

Read:     terraform plan (or terraform refresh)
            → provider.Read() API call for every resource in state
            → compares actual attributes to state
            → reports drift if something changed outside Terraform

Update:   resource block changed in HCL + terraform apply
            → provider.Update() API call (if in-place update is possible)
            → OR destroy + create (if update requires replacement)
            → state updated

Destroy:  resource block removed from HCL + terraform apply
            → provider.Delete() API call
            → resource deleted
            → resource removed from state
```

---

## Terraform vs Other Tools: Decision Guide

```text
Use Terraform when:
  - Multi-cloud or more than one provider needed
  - Team already uses Terraform or wants open-source tooling
  - Need strong state management and plan/apply workflow
  - Want a large module registry (registry.terraform.io)

Use CloudFormation when:
  - AWS only, tight AWS integration needed (StackSets, Service Catalog)
  - No external state backend management (AWS handles it)
  - Team is AWS-native and already familiar

Use Pulumi when:
  - Team prefers TypeScript/Python/Go over HCL
  - Need complex imperative logic (loops, conditionals beyond HCL)
  - Same multi-cloud need as Terraform

Use Ansible when:
  - Configuration management (software install, file deploy) on existing servers
  - Not provisioning new cloud resources from scratch
  - Stateless execution model preferred

Use CDK when:
  - AWS only + team prefers TypeScript/Python
  - Compiles to CloudFormation under the hood
```

---

## Terraform File Layout

```text
Standard project structure:
  main.tf          — resource definitions (primary content)
  variables.tf     — all variable declarations
  outputs.tf       — all output declarations
  providers.tf     — provider + terraform block
  locals.tf        — local value computations (optional)
  versions.tf      — terraform and provider version constraints (often in providers.tf)
  
  terraform.tfvars — variable values (gitignored if contains secrets)
  *.auto.tfvars    — automatically loaded variable value files

Generated files (do not edit):
  .terraform/            — downloaded providers and modules
  .terraform.lock.hcl    — provider dependency lock file (commit this to git)
  terraform.tfstate      — local state (only for dev; use remote state in production)
  terraform.tfstate.backup
```

---

## Key Terraform Concepts At A Glance

| Concept | What It Is |
|---|---|
| Provider | Plugin that talks to a cloud/service API |
| Resource | A piece of infrastructure Terraform manages |
| Data source | Read-only query to existing infrastructure |
| Variable | Input parameter to a config or module |
| Output | Value exposed after apply (for use by other configs or humans) |
| Local | Intermediate computed value in HCL |
| Module | Reusable group of resources |
| State | JSON record of everything Terraform manages |
| Backend | Where state is stored (local file, S3, Terraform Cloud) |
| Workspace | Named state isolation within a backend |

---

## Interview Sound Bite

Terraform is a declarative IaC tool: you describe desired infrastructure state in HCL, and Terraform computes the minimal set of API calls to make the real world match. The provider model makes it multi-cloud — each provider (AWS, GCP, Azure, Kubernetes) is a plugin. The state file is the source of truth about what Terraform manages; without it, Terraform loses track of resources. The plan/apply separation is a safety feature — `terraform plan` is always a read-only diff, never destructive, giving you a human-readable audit of what will change before any API call is made.
