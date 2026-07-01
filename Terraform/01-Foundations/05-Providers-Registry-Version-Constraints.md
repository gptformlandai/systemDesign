# 05. Providers: Registry, Version Constraints, Configuration

## What Is A Provider?

A provider is a plugin that Terraform uses to interact with a specific cloud or service API. Providers translate HCL resource declarations into API calls.

```text
Terraform Core  →  Provider Plugin  →  Cloud/Service API
  (plan/apply)      (AWS, GCP, K8s)     (REST/gRPC calls)
```

---

## Declaring Providers

```hcl
# providers.tf or versions.tf
terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.20"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
    datadog = {
      source  = "DataDog/datadog"
      version = "~> 3.30"
    }
  }
}
```

### Provider Source Format

```text
<HOSTNAME>/<NAMESPACE>/<TYPE>

registry.terraform.io/hashicorp/aws      ← official HashiCorp provider
registry.terraform.io/DataDog/datadog    ← partner provider
registry.terraform.io/mongodb/mongodbatlas ← community partner

Short form (hostname omitted, defaults to registry.terraform.io):
  hashicorp/aws
  DataDog/datadog
```

---

## Provider Configuration

```hcl
# AWS provider
provider "aws" {
  region = "us-east-1"
  
  # Optional: override default credential chain
  # (usually better to use env vars or IAM roles)
  # access_key = var.aws_access_key  # AVOID: use env or IAM
  # secret_key = var.aws_secret_key  # AVOID: use env or IAM
  
  default_tags {
    tags = {
      ManagedBy   = "terraform"
      Project     = var.project
      Environment = var.environment
    }
  }
}
```

### AWS Credential Chain

```text
Terraform uses the standard AWS credential chain (in order):
  1. provider block: access_key/secret_key (avoid — secrets in state)
  2. Environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
  3. AWS credentials file: ~/.aws/credentials
  4. IAM instance profile / ECS task role / Lambda execution role
  5. Web Identity Token (for EKS IRSA, GitHub Actions OIDC)

Best practice for CI/CD:
  Use OIDC federation (no stored secrets):
  GitHub Actions → OIDC token → AWS STS AssumeRoleWithWebIdentity → temporary credentials
```

---

## Provider Aliases (Multiple Configurations)

Use aliases when you need the same provider with different configurations — for example, two different AWS regions, or two different AWS accounts.

```hcl
# Default provider (no alias)
provider "aws" {
  region = "us-east-1"
}

# Aliased provider for a second region
provider "aws" {
  alias  = "eu_west"
  region = "eu-west-1"
}

# Aliased provider for a different account
provider "aws" {
  alias  = "logging_account"
  region = "us-east-1"
  assume_role {
    role_arn = "arn:aws:iam::999888777666:role/TerraformCrossAccountRole"
  }
}

# Using aliased provider in a resource
resource "aws_s3_bucket" "eu_logs" {
  provider = aws.eu_west   # uses the eu-west-1 provider
  bucket   = "my-eu-logs"
}

resource "aws_cloudtrail" "org_trail" {
  provider = aws.logging_account
  name     = "org-cloudtrail"
  # ...
}
```

---

## Assume Role (Cross-Account)

```hcl
provider "aws" {
  region = "us-east-1"
  
  assume_role {
    role_arn     = "arn:aws:iam::123456789012:role/TerraformDeployRole"
    session_name = "terraform-deploy"
    external_id  = var.external_id   # optional: additional trust policy check
  }
}
```

```text
Pattern for multi-account organizations:
  Terraform runs in a "tooling" account with base credentials.
  For each target account, assume a cross-account role.
  The target account's TerraformDeployRole trusts the tooling account.

IAM trust policy for TerraformDeployRole:
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::TOOLING_ACCOUNT_ID:role/TerraformRunner"
  },
  "Action": "sts:AssumeRole"
}
```

---

## Provider Lock File

`terraform init` creates `.terraform.lock.hcl`. This file records exact provider versions and checksums.

```hcl
# .terraform.lock.hcl (commit to git)
provider "registry.terraform.io/hashicorp/aws" {
  version     = "5.31.0"
  constraints = "~> 5.0"
  hashes = [
    "h1:ABC123...",
    "zh:DEF456...",
  ]
}
```

```text
Why commit the lock file:
  - Ensures all team members and CI/CD use exactly the same provider version
  - Prevents "works on my machine" with different provider behavior
  - Checksums verify provider binary integrity (supply chain security)

To upgrade a provider:
  terraform init -upgrade
  (re-downloads latest version matching constraint, updates lock file)
```

---

## Multiple Provider Versions In One Workspace

```hcl
# Kubernetes provider often needs to depend on EKS cluster being created first
provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
  
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args        = ["eks", "get-token", "--cluster-name", module.eks.cluster_name]
  }
}
```

---

## Provider Tier Levels

```text
Official:   hashicorp/* providers — maintained by HashiCorp
            (aws, google, azurerm, kubernetes, helm, vault, ...)

Partner:    Maintained by the service vendor, verified by HashiCorp
            (DataDog/datadog, mongodb/mongodbatlas, confluentinc/confluent, ...)

Community:  Maintained by community, no HashiCorp verification
            (use with caution; check maintenance/issue history)
```

---

## Terraform Registry vs Custom Registry

```text
Public registry: registry.terraform.io
  - Free access to all official, partner, and community providers
  - terraform init downloads directly

Private registry (Terraform Cloud / Enterprise):
  - Host internal providers or curated approved providers
  - Used when internet access is restricted
  - source = "app.terraform.io/<ORG>/internal-provider"
```

---

## Interview Sound Bite

A Terraform provider is a plugin that wraps a cloud API — `hashicorp/aws` translates HCL into AWS API calls. Providers are declared in the `required_providers` block, downloaded by `terraform init`, and locked to exact versions in `.terraform.lock.hcl` (always commit this file). Provider aliases enable multiple configurations of the same provider — different regions or different accounts. Cross-account AWS access uses `assume_role` in the provider block so Terraform assumes an IAM role in the target account without storing credentials in HCL. The credential chain priority: static keys (avoid) → environment variables → IAM instance profile → OIDC web identity (best for CI/CD).
