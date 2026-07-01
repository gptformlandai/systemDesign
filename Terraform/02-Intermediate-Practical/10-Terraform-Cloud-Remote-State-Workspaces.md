# 10. Terraform Cloud, Remote State, Workspaces

## Terraform Cloud vs OSS Terraform

```text
Open Source Terraform (OSS):
  - Free, local execution
  - State: local file or self-managed backend (S3, GCS, etc.)
  - Workspaces: multiple state files in the same backend bucket
  - No built-in UI, RBAC, or policy enforcement
  - You manage the CI/CD pipeline

Terraform Cloud (TFC) / HCP Terraform:
  - Free tier + paid plans
  - State: managed by HashiCorp, versioned, encrypted, access-controlled
  - Remote execution: plans and applies run on TFC infrastructure (or local agent)
  - Workspaces: the primary organizational unit (one workspace = one state)
  - Built-in: VCS integration, RBAC, Sentinel/OPA policies, variable sets
  - Plan output as PR comments out of the box

Terraform Enterprise:
  - Self-hosted Terraform Cloud for organizations with data residency requirements
```

---

## Configuring The Cloud Backend (TF 1.1+)

```hcl
terraform {
  cloud {
    organization = "my-company"

    workspaces {
      name = "order-api-prod"    # single workspace
    }
  }
}
```

```hcl
# Or use a workspace tag to run the same config in multiple workspaces
terraform {
  cloud {
    organization = "my-company"

    workspaces {
      tags = ["order-api"]  # runs in any workspace tagged "order-api"
    }
  }
}
```

```bash
# Login to TFC (saves token to ~/.terraform.d/credentials.tfrc.json)
terraform login

# Initialize with TFC backend
terraform init
```

---

## TFC Workspaces vs OSS Workspaces

```text
CONCEPT          OSS Workspaces                   TFC Workspaces
────────────     ──────────────────────────────   ──────────────────────────────
What they are    Multiple state files in one       Full-featured environments:
                 backend (same bucket, diff key)   state + run history + vars + RBAC

Primary use      Feature branches, quick           Per-environment (dev/staging/prod)
                 environment copies                per-team, per-service

Variables        Same tfvars for all workspaces    Each workspace has its own
                                                   variables and sensitive variables

Isolation        Low: same provider config,        High: different AWS accounts,
                 same credentials                   different credentials per workspace

Access control   None                              RBAC per workspace/team

Execution        Local                             Remote (TFC runners) or local agent
```

---

## TFC Variable Types

```text
In TFC UI or via API, set:
  Terraform variables    → equivalent to terraform.tfvars (HCL or string)
  Environment variables  → set as env vars for the Terraform process
                            AWS_ACCESS_KEY_ID, TF_LOG, etc.
  Sensitive flag         → value is write-only; never shown in UI again
  Variable sets          → shared variable collections applied to multiple workspaces
```

---

## Remote Execution: API-Driven Workflow

```text
VCS-Driven workflow (most common):
  1. Developer pushes a branch / opens PR
  2. TFC: auto-triggers terraform plan on that workspace
  3. Plan output posted as PR comment
  4. Reviewer approves PR
  5. Merge to main → TFC triggers terraform apply

CLI-Driven workflow:
  1. Developer runs terraform plan locally
  2. Plan runs on TFC (uploads config, runs remotely)
  3. Developer runs terraform apply
  4. Apply runs on TFC, developer watches streaming output

API-Driven workflow (for complex CI/CD):
  1. CI/CD calls TFC API to create a run
  2. Polls for plan/apply status
  3. Confirms apply via API
```

---

## Run Triggers (Cross-Workspace)

```text
Trigger workspace B's plan when workspace A applies:
  - Workspace B "watches" workspace A
  - When A's apply completes, B is automatically queued for a plan

Use case:
  Network workspace (VPC) triggers App workspace (EKS)
  App workspace triggers Ingress workspace (ALB/DNS)
```

---

## Cross-Workspace Outputs: terraform_remote_state

```hcl
# Reading outputs from another TFC workspace
data "terraform_remote_state" "network" {
  backend = "remote"
  config = {
    organization = "my-company"
    workspaces = {
      name = "network-prod"
    }
  }
}

locals {
  vpc_id             = data.terraform_remote_state.network.outputs.vpc_id
  private_subnet_ids = data.terraform_remote_state.network.outputs.private_subnet_ids
}
```

---

## OIDC Dynamic Credentials (No Static Keys)

```text
Old approach (insecure):
  Store AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY as workspace variables.
  Long-lived, high-blast-radius if leaked.

OIDC dynamic credentials (recommended):
  1. TFC requests a short-lived JWT from HashiCorp's OIDC provider
  2. AWS STS validates the JWT against the IAM OIDC Identity Provider
  3. STS returns temporary credentials (15-60 min TTL)
  4. Terraform uses temporary credentials for the run
  No static secrets stored anywhere.
```

```hcl
# AWS IAM OIDC provider for TFC
resource "aws_iam_openid_connect_provider" "tfc" {
  url = "https://app.terraform.io"
  client_id_list  = ["aws.workload.identity"]
  thumbprint_list = ["9e99a48a9960b14926bb7f3b02e22da2b0ab7280"]
}

resource "aws_iam_role" "tfc_deploy" {
  name = "tfc-deploy-role"
  
  assume_role_policy = jsonencode({
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.tfc.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "app.terraform.io:aud" = "aws.workload.identity"
        }
        StringLike = {
          "app.terraform.io:sub" = "organization:my-company:project:*:workspace:order-api-*:run_phase:*"
        }
      }
    }]
    Version = "2012-10-17"
  })
}
```

---

## Sentinel Policies (Governance)

```text
Sentinel is an OPA-like policy-as-code framework built into TFC (paid plans).
Policies run after plan but before apply.

Common policies:
  - Require all resources to have a cost_center tag
  - Disallow EC2 instance types larger than t3.medium in dev
  - Require S3 buckets to have encryption enabled
  - Deny resources in unapproved regions

Policy enforcement levels:
  advisory:    pass/fail reported but apply proceeds
  soft-mandatory:  fail blocks apply, but workspace admin can override
  hard-mandatory:  fail always blocks apply, no override
```

---

## Private Module Registry (TFC)

```text
Upload reusable modules to TFC's private registry:
  - Module source: app.terraform.io/<ORG>/<MODULE_NAME>/<PROVIDER>
  - Version-pinned, access-controlled
  - Visible to all workspaces in the organization

module "vpc" {
  source  = "app.terraform.io/my-company/vpc/aws"
  version = "~> 1.2"
  # ...
}
```

---

## Interview Sound Bite

Terraform Cloud is HashiCorp's managed platform for running Terraform at scale. TFC workspaces are richer than OSS workspaces — each workspace has its own state, variable set, run history, and RBAC. The VCS-driven workflow posts plan output as PR comments and auto-applies on merge to main. For credentials, OIDC dynamic credentials are the gold standard — TFC requests a short-lived JWT, AWS validates it via an OIDC trust relationship, and issues temporary credentials. Cross-workspace references use `terraform_remote_state` data source to read another workspace's outputs. Sentinel policies enforce governance rules (tagging, region restrictions, resource size limits) between plan and apply.
