# 14. Remote Backend: S3+DynamoDB, GCS, Migration

## Backend Requirements Checklist

```text
For any remote backend, ensure:
  ✓ Versioning enabled         (recover from state corruption)
  ✓ Encryption at rest         (state contains secrets)
  ✓ Access control             (least-privilege IAM)
  ✓ State locking              (prevent concurrent applies)
  ✓ No public access           (never publicly readable)
  ✓ Separate per environment   (prod state ≠ dev state)
```

---

## S3 Backend Full Setup

### Step 1: Create State Infrastructure (Bootstrap)

```hcl
# bootstrap/main.tf — run once, manually or via a separate Terraform config
resource "aws_s3_bucket" "tf_state" {
  bucket = "mycompany-terraform-state-${data.aws_caller_identity.current.account_id}"
  
  tags = {
    Purpose = "terraform-state"
    ManagedBy = "terraform-bootstrap"
  }
}

resource "aws_s3_bucket_versioning" "tf_state" {
  bucket = aws_s3_bucket.tf_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "tf_state" {
  bucket = aws_s3_bucket.tf_state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.tf_state.arn
    }
  }
}

resource "aws_s3_bucket_public_access_block" "tf_state" {
  bucket                  = aws_s3_bucket.tf_state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_kms_key" "tf_state" {
  description             = "KMS key for Terraform state encryption"
  deletion_window_in_days = 30
  enable_key_rotation     = true
}

resource "aws_dynamodb_table" "tf_lock" {
  name         = "terraform-state-lock"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Purpose = "terraform-state-lock"
  }
}
```

### Step 2: Configure Backend In Each Workspace

```hcl
# environments/prod/versions.tf
terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket         = "mycompany-terraform-state-123456789012"
    key            = "environments/prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:us-east-1:123456789012:key/abc123"
    dynamodb_table = "terraform-state-lock"
  }
}
```

### State Key Conventions

```text
Flat structure:
  key = "environments/prod/terraform.tfstate"
  key = "environments/staging/terraform.tfstate"

Service-based structure (for large orgs):
  key = "services/order-api/prod/terraform.tfstate"
  key = "services/order-api/staging/terraform.tfstate"
  key = "platform/network/prod/terraform.tfstate"
  key = "platform/eks/prod/terraform.tfstate"

Convention:
  <layer>/<service>/<environment>/terraform.tfstate
```

---

## Partial Backend Config (For Dynamic Values)

Never put secrets or environment-specific values in the backend block itself. Use partial config with `-backend-config` flags.

```hcl
# versions.tf — partial backend config (no values)
terraform {
  backend "s3" {}
}
```

```bash
# Initialize with backend values from file
terraform init \
  -backend-config="bucket=mycompany-terraform-state-123456789012" \
  -backend-config="key=environments/prod/terraform.tfstate" \
  -backend-config="region=us-east-1" \
  -backend-config="dynamodb_table=terraform-state-lock" \
  -backend-config="encrypt=true"

# Or use a backend config file
terraform init -backend-config=backend-prod.hcl
```

```hcl
# backend-prod.hcl (can be committed; no secrets)
bucket         = "mycompany-terraform-state-123456789012"
key            = "environments/prod/terraform.tfstate"
region         = "us-east-1"
dynamodb_table = "terraform-state-lock"
encrypt        = true
```

---

## GCS Backend (Google Cloud)

```hcl
terraform {
  backend "gcs" {
    bucket  = "mycompany-terraform-state"
    prefix  = "environments/prod"           # creates environments/prod/default.tfstate
    # Locking: built-in via GCS object versioning, no separate table needed
  }
}
```

GCS bucket creation:

```bash
gsutil mb -l us-central1 gs://mycompany-terraform-state
gsutil versioning set on gs://mycompany-terraform-state
gsutil uniformbucketlevelaccess set on gs://mycompany-terraform-state
```

---

## Azure Blob Backend

```hcl
terraform {
  backend "azurerm" {
    resource_group_name  = "terraform-state-rg"
    storage_account_name = "mycompanytfstate"
    container_name       = "tfstate"
    key                  = "prod/terraform.tfstate"
  }
}
```

---

## Backend Migration

```bash
# Scenario: moving from local state to S3

# 1. Add backend "s3" block to versions.tf

# 2. Run init with -migrate-state
terraform init -migrate-state

# Terraform will ask:
# "Do you want to copy existing state to the new backend?"
# Type "yes"

# 3. Verify state is in S3
aws s3 ls s3://mycompany-terraform-state/environments/prod/

# Scenario: moving between S3 buckets (new bucket, new key)
# Update backend block with new values
# Run: terraform init -migrate-state -reconfigure
```

---

## Cross-Account State Access

```text
Scenario:
  Terraform runs in "tooling" account (123456789001)
  State bucket is in "shared-services" account (123456789002)

S3 bucket policy (in shared-services account):
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::123456789001:role/TerraformRole"
  },
  "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"],
  "Resource": [
    "arn:aws:s3:::mycompany-terraform-state/*",
    "arn:aws:s3:::mycompany-terraform-state"
  ]
}

DynamoDB table policy (in shared-services account):
{
  "Effect": "Allow",
  "Principal": {
    "AWS": "arn:aws:iam::123456789001:role/TerraformRole"
  },
  "Action": ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"],
  "Resource": "arn:aws:dynamodb:us-east-1:123456789002:table/terraform-state-lock"
}
```

---

## Backend Workspace Keys

```text
When using OSS Terraform workspaces with an S3 backend:

  Workspace: default  → s3://bucket/key              (base key)
  Workspace: staging  → s3://bucket/env:/staging/key (env:/ prefix)
  Workspace: prod     → s3://bucket/env:/prod/key

Recommendation: avoid OSS workspaces for long-lived environments.
Use separate root modules with separate backend keys per environment.
```

---

## Interview Sound Bite

The S3 remote backend for AWS uses two AWS services: S3 for durable, versioned, encrypted state storage and DynamoDB for distributed locking (one row per LockID prevents concurrent applies from corrupting state). The DynamoDB table must have `LockID` as the hash key and PAY_PER_REQUEST billing. Enable S3 bucket versioning to recover from accidental state corruption. Use partial backend config with `-backend-config` flags to avoid hardcoding environment-specific values in HCL. To migrate from local to remote state: add the backend block and run `terraform init -migrate-state`. GCS backend has built-in locking — no separate table required.
