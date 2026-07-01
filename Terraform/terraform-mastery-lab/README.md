# Terraform Mastery Lab

A hands-on practice workspace for the Terraform Mastery Track.
Use these examples, scripts, and runbooks alongside the 29 Hot Sheets.

---

## Contents

```
terraform-mastery-lab/
├── README.md                     ← this file
├── LEARNING_PATH.md              ← guided practice sequence
├── EXAMPLES/
│   ├── aws-basic-vpc/            ← hands-on VPC + subnets example
│   ├── multi-env-pattern/        ← directory-based environment pattern
│   └── module-example/           ← reusable module practice
├── SCRIPTS/
│   ├── terraform-plan-all.sh     ← plan all environments
│   ├── state-backup.sh           ← backup state from remote
│   └── provider-lock-update.sh   ← update lock file for all platforms
├── CHEATSHEETS/
│   ├── terraform-cli-cheatsheet.md
│   ├── hcl-reference-cheatsheet.md
│   └── state-operations-cheatsheet.md
└── RUNBOOKS/
    ├── runbook-state-lock.md     ← stale lock recovery
    ├── runbook-drift-recovery.md ← state drift detection and fix
    └── runbook-import-existing.md ← import existing resources
```

---

## Prerequisites

```bash
# Install Terraform (macOS via homebrew)
brew tap hashicorp/tap
brew install hashicorp/tap/terraform

# Verify
terraform version

# AWS CLI
brew install awscli
aws configure  # or use OIDC / instance profile
```

---

## Quick Start

```bash
# Clone or navigate to the lab
cd terraform-mastery-lab/EXAMPLES/aws-basic-vpc

# Initialize
terraform init

# Plan (dry run)
terraform plan

# Apply (creates real resources — incurs AWS cost)
terraform apply

# Destroy when done
terraform destroy
```
