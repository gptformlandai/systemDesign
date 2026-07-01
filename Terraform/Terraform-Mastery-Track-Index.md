# Terraform Mastery Track Index

## What This Track Is

A complete Terraform mastery system from zero to senior/MAANG level. Covers IaC fundamentals, HCL syntax, state management, modules, remote backends, CI/CD automation, multi-account patterns, security, testing, and production operations.

---

## How To Use This Track

```text
Beginner:       Sheets 1 → 5  (Foundations)
Intermediate:   Sheets 6 → 11 (Intermediate Practical)
Senior:         Sheets 12 → 18 (Senior Production)
Interview Prep: Sheets 19 → 29 (Scenarios + Interview + Practice)
```

Read each sheet, run examples in the lab, do the drills.

---

## Core Mental Model

```text
Terraform is a declarative IaC tool:
  You describe WHAT infrastructure you want (desired state).
  Terraform figures out HOW to create/update/delete it (execution plan).
  State file tracks WHAT currently exists.

Workflow:
  Write HCL → terraform init → terraform plan → terraform apply → repeat
  
State = source of truth about what Terraform manages.
Remote state = shared truth for teams.
Modules = reusable infrastructure components.
Providers = plugins that talk to cloud APIs.
```

---

## Terraform vs Other IaC Tools

| Tool | Type | Language | Cloud | State |
|---|---|---|---|---|
| Terraform | Declarative | HCL | Multi-cloud | External |
| CloudFormation | Declarative | JSON/YAML | AWS only | AWS-managed |
| Pulumi | Imperative/Declarative | Python/TS/Go | Multi-cloud | External |
| AWS CDK | Imperative | Python/TS/Java | AWS | CloudFormation |
| Ansible | Procedural | YAML | Multi-cloud | Stateless |

---

## Sheet Directory

### 01-Foundations

| Sheet | Topic | File |
|---|---|---|
| 1 | What Is Terraform: IaC Mental Model | [01-Foundations/01-What-Is-Terraform-IaC-Mental-Model-HotSheet.md](01-Foundations/01-What-Is-Terraform-IaC-Mental-Model-HotSheet.md) |
| 2 | HCL Syntax: Blocks, Variables, Outputs | [01-Foundations/02-HCL-Syntax-Blocks-Variables-Outputs-HotSheet.md](01-Foundations/02-HCL-Syntax-Blocks-Variables-Outputs-HotSheet.md) |
| 3 | Terraform CLI: Init, Plan, Apply Workflow | [01-Foundations/03-Terraform-CLI-Init-Plan-Apply-Workflow.md](01-Foundations/03-Terraform-CLI-Init-Plan-Apply-Workflow.md) |
| 4 | State Management: Local, Remote, Backends | [01-Foundations/04-State-Management-Local-Remote-Backends.md](01-Foundations/04-State-Management-Local-Remote-Backends.md) |
| 5 | Providers: Registry, Version Constraints | [01-Foundations/05-Providers-Registry-Version-Constraints.md](01-Foundations/05-Providers-Registry-Version-Constraints.md) |

### 02-Intermediate-Practical

| Sheet | Topic | File |
|---|---|---|
| 6 | Modules: Structure, Reuse, Best Practices | [02-Intermediate-Practical/06-Modules-Structure-Reuse-Best-Practices.md](02-Intermediate-Practical/06-Modules-Structure-Reuse-Best-Practices.md) |
| 7 | Meta-Arguments: count, for_each, lifecycle | [02-Intermediate-Practical/07-Meta-Arguments-Count-ForEach-DependsOn-Lifecycle.md](02-Intermediate-Practical/07-Meta-Arguments-Count-ForEach-DependsOn-Lifecycle.md) |
| 8 | Expressions, Functions, Dynamic Blocks | [02-Intermediate-Practical/08-Expressions-Functions-Dynamic-Blocks.md](02-Intermediate-Practical/08-Expressions-Functions-Dynamic-Blocks.md) |
| 9 | Data Sources, Locals, Workspace Patterns | [02-Intermediate-Practical/09-Data-Sources-Locals-Workspace-Patterns.md](02-Intermediate-Practical/09-Data-Sources-Locals-Workspace-Patterns.md) |
| 10 | Terraform Cloud, Remote State, Workspaces | [02-Intermediate-Practical/10-Terraform-Cloud-Remote-State-Workspaces.md](02-Intermediate-Practical/10-Terraform-Cloud-Remote-State-Workspaces.md) |
| 11 | Testing, Validation, Checks, Preconditions | [02-Intermediate-Practical/11-Testing-Validation-Checks-Preconditions.md](02-Intermediate-Practical/11-Testing-Validation-Checks-Preconditions.md) |

### 03-Senior-Production

| Sheet | Topic | File |
|---|---|---|
| 12 | Advanced State: Import, Move, Refactor | [03-Senior-Production/12-Advanced-State-Operations-Import-Move-Refactor.md](03-Senior-Production/12-Advanced-State-Operations-Import-Move-Refactor.md) |
| 13 | Module Design Patterns: Composition | [03-Senior-Production/13-Modules-Design-Patterns-Composition-Root-Child.md](03-Senior-Production/13-Modules-Design-Patterns-Composition-Root-Child.md) |
| 14 | Remote Backend: S3, DynamoDB Locking, GCS | [03-Senior-Production/14-Remote-Backend-S3-Locking-DynamoDB-GCS.md](03-Senior-Production/14-Remote-Backend-S3-Locking-DynamoDB-GCS.md) |
| 15 | Provider Deep Dive: Aliases, Cross-Account | [03-Senior-Production/15-Provider-Deep-Dive-Aliases-Assume-Role-CrossAccount.md](03-Senior-Production/15-Provider-Deep-Dive-Aliases-Assume-Role-CrossAccount.md) |
| 16 | Performance: Large Terraform, Parallelism | [03-Senior-Production/16-Performance-Large-Terraform-Parallelism-Targeting.md](03-Senior-Production/16-Performance-Large-Terraform-Parallelism-Targeting.md) |
| 17 | Security: Secrets, Sensitive Variables | [03-Senior-Production/17-Security-Secrets-Management-Sensitive-Variables.md](03-Senior-Production/17-Security-Secrets-Management-Sensitive-Variables.md) |
| 18 | CI/CD: Pipelines, Atlantis, TFC | [03-Senior-Production/18-CICD-Terraform-Pipelines-Atlantis-TFC.md](03-Senior-Production/18-CICD-Terraform-Pipelines-Atlantis-TFC.md) |

### 04-Scenario-Practice

| Sheet | Topic | File |
|---|---|---|
| 19 | Scenario: Multi-Environment Dev/Staging/Prod | [04-Scenario-Practice/19-Scenario-Multi-Environment-Dev-Staging-Prod.md](04-Scenario-Practice/19-Scenario-Multi-Environment-Dev-Staging-Prod.md) |
| 20 | Scenario: AWS VPC + EKS + RDS Full Stack | [04-Scenario-Practice/20-Scenario-AWS-VPC-EKS-RDS-Full-Stack.md](04-Scenario-Practice/20-Scenario-AWS-VPC-EKS-RDS-Full-Stack.md) |
| 21 | Scenario: State Corruption Recovery | [04-Scenario-Practice/21-Scenario-State-Corruption-Recovery-Runbook.md](04-Scenario-Practice/21-Scenario-State-Corruption-Recovery-Runbook.md) |
| 22 | Scenario: Refactoring Monolith to Modules | [04-Scenario-Practice/22-Scenario-Refactoring-Monolith-To-Modules.md](04-Scenario-Practice/22-Scenario-Refactoring-Monolith-To-Modules.md) |
| 23 | Scenario: Zero-Downtime Infrastructure Changes | [04-Scenario-Practice/23-Scenario-Zero-Downtime-Infrastructure-Changes.md](04-Scenario-Practice/23-Scenario-Zero-Downtime-Infrastructure-Changes.md) |

### 05-Special-Interview-Rounds

| Sheet | Topic | File |
|---|---|---|
| 24 | Interview Questions: Beginner–Intermediate | [05-Special-Interview-Rounds/24-Terraform-Interview-Questions-Beginner-Intermediate.md](05-Special-Interview-Rounds/24-Terraform-Interview-Questions-Beginner-Intermediate.md) |
| 25 | Interview Questions: Senior–MAANG | [05-Special-Interview-Rounds/25-Terraform-Interview-Questions-Senior-MAANG.md](05-Special-Interview-Rounds/25-Terraform-Interview-Questions-Senior-MAANG.md) |
| 26 | Anti-Patterns and Common Mistakes | [05-Special-Interview-Rounds/26-Terraform-Anti-Patterns-Common-Mistakes.md](05-Special-Interview-Rounds/26-Terraform-Anti-Patterns-Common-Mistakes.md) |

### 06-Practice-Upgrade

| Sheet | Topic | File |
|---|---|---|
| 27 | Active Recall Drills | [06-Practice-Upgrade/27-Terraform-Active-Recall-Drills.md](06-Practice-Upgrade/27-Terraform-Active-Recall-Drills.md) |
| 28 | Production Readiness Checklist | [06-Practice-Upgrade/28-Terraform-Production-Readiness-Checklist.md](06-Practice-Upgrade/28-Terraform-Production-Readiness-Checklist.md) |
| 29 | Cheatsheet: All Commands + HCL Reference | [06-Practice-Upgrade/29-Terraform-Cheatsheet-All-Commands-HCL-Reference.md](06-Practice-Upgrade/29-Terraform-Cheatsheet-All-Commands-HCL-Reference.md) |

---

## Lab

[terraform-mastery-lab/README.md](terraform-mastery-lab/README.md) — runnable examples, scripts, cheatsheets, runbooks

---

## Learning Arc

```text
Week 1: Foundations (Sheets 1-5)
  → Understand IaC, write your first HCL, run init/plan/apply
  → Understand state and providers

Week 2: Intermediate (Sheets 6-11)
  → Write and use modules
  → Master for_each, dynamic blocks, functions
  → Set up remote backend, Terraform Cloud

Week 3: Senior Production (Sheets 12-18)
  → State operations: import, moved, refactor
  → Multi-account providers, cross-account patterns
  → Secrets management, CI/CD integration

Week 4: Mastery (Sheets 19-29)
  → Full scenarios: multi-env, AWS full stack
  → State recovery runbooks
  → Interview Q&A, active recall, production checklist
```
