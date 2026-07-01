# 15. Provider Deep-Dive: Aliases, Assume Role, Cross-Account

## Provider Configuration Inheritance

```text
Terraform automatically assigns the default provider to all resources matching
that provider type. No explicit assignment needed for the common case.

provider "aws" { region = "us-east-1" }  ← default provider

resource "aws_vpc" "main" { ... }         ← implicitly uses the default aws provider
resource "aws_s3_bucket" "data" { ... }   ← implicitly uses the default aws provider
```

---

## Provider Aliases: Multiple Configurations

Use when you need two or more configurations of the same provider — different region, different account, different credentials.

```hcl
# providers.tf
provider "aws" {
  region = "us-east-1"
  # No alias = default provider
}

provider "aws" {
  alias  = "us_west"
  region = "us-west-2"
}

provider "aws" {
  alias  = "eu_west"
  region = "eu-west-1"
}

provider "aws" {
  alias  = "logging_account"
  region = "us-east-1"
  assume_role {
    role_arn = "arn:aws:iam::999888777666:role/TerraformRole"
  }
}
```

### Assigning Aliases To Resources

```hcl
# Uses default (us-east-1) provider
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
}

# Explicitly uses the eu-west alias
resource "aws_s3_bucket" "eu_archive" {
  provider = aws.eu_west    # format: <provider_type>.<alias>
  bucket   = "my-eu-archive"
}

resource "aws_cloudwatch_log_group" "app" {
  provider = aws.eu_west
  name     = "/app/logs"
}

# Cross-account resource in logging account
resource "aws_cloudtrail" "org_trail" {
  provider = aws.logging_account
  name     = "organization-trail"
  # ...
}
```

---

## assume_role: Cross-Account Terraform

```hcl
# providers.tf — full cross-account setup
# Terraform runner has credentials for the "tooling" account.
# It assumes roles into target accounts for each provider.

# Default provider: tooling account (where Terraform runner lives)
provider "aws" {
  region = "us-east-1"
}

# Provider for dev account
provider "aws" {
  alias  = "dev"
  region = "us-east-1"
  assume_role {
    role_arn     = "arn:aws:iam::${var.dev_account_id}:role/TerraformDeployRole"
    session_name = "terraform-${var.environment}-deploy"
    external_id  = var.external_id   # extra trust policy check (optional)
  }
}

# Provider for prod account
provider "aws" {
  alias  = "prod"
  region = "us-east-1"
  assume_role {
    role_arn     = "arn:aws:iam::${var.prod_account_id}:role/TerraformDeployRole"
    session_name = "terraform-prod-deploy"
  }
}
```

### Trust Policy For Assumed Roles

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::TOOLING_ACCOUNT_ID:role/TerraformRunnerRole"
      },
      "Action": "sts:AssumeRole",
      "Condition": {
        "StringEquals": {
          "sts:ExternalId": "my-external-id-123"
        }
      }
    }
  ]
}
```

---

## Passing Providers Into Modules

When a module uses a non-default provider or an aliased provider, you must pass it explicitly.

```hcl
# main.tf (root module)
module "eu_resources" {
  source = "./modules/regional-resources"

  providers = {
    aws = aws.eu_west    # pass the eu_west alias to the module
  }
  
  name = "eu-region-resources"
}

# modules/regional-resources/main.tf
terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

resource "aws_s3_bucket" "this" {
  # Uses whatever provider was passed in via providers = {}
  bucket = "${var.name}-bucket"
}
```

### Multiple Provider Aliases Into One Module

```hcl
# Root calling a module that needs two AWS providers (e.g., cross-region replication)
module "s3_replication" {
  source = "./modules/s3-replication"

  providers = {
    aws.primary   = aws.us_east
    aws.replica   = aws.eu_west
  }
}

# modules/s3-replication/main.tf
terraform {
  required_providers {
    aws = {
      source                = "hashicorp/aws"
      configuration_aliases = [aws.primary, aws.replica]
    }
  }
}

resource "aws_s3_bucket" "primary" {
  provider = aws.primary
  bucket   = "primary-bucket"
}

resource "aws_s3_bucket" "replica" {
  provider = aws.replica
  bucket   = "replica-bucket"
}
```

---

## Provider Meta-Argument Per Resource

```hcl
resource "aws_ecr_repository" "app" {
  provider = aws.us_east   # override provider for this specific resource
  name     = "my-app"
}
```

---

## Kubernetes Provider — Dynamic Configuration

```hcl
# Common pattern: Kubernetes/Helm provider depends on EKS cluster output
provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
  
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args = [
      "eks", "get-token",
      "--cluster-name", module.eks.cluster_name,
      "--region", var.aws_region
    ]
  }
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args = [
        "eks", "get-token",
        "--cluster-name", module.eks.cluster_name
      ]
    }
  }
}
```

---

## Multi-Cloud Provider Setup

```hcl
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

provider "google" {
  project = var.gcp_project
  region  = "us-central1"
}

provider "azurerm" {
  features {}
  subscription_id = var.azure_subscription_id
}
```

---

## Interview Sound Bite

Provider aliases enable multiple configurations of the same provider: `provider "aws" { alias = "eu_west" region = "eu-west-1" }` creates a second AWS provider, assigned to resources with `provider = aws.eu_west`. Cross-account access uses `assume_role { role_arn = "..." }` inside the provider block — Terraform assumes an IAM role in the target account, so no static credentials for that account are stored anywhere. Modules that need aliased providers must have providers passed in via the `providers` meta-argument: `providers = { aws = aws.eu_west }`. Modules declare `configuration_aliases` in `required_providers` when they expect multiple provider aliases. Never configure providers inside child modules — it breaks reusability.
