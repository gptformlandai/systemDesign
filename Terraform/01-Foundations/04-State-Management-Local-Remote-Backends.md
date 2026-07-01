# 04. State Management: Local, Remote, Backends

## What Is Terraform State?

The state file (`terraform.tfstate`) is Terraform's record of what it manages. It is a JSON document mapping your HCL resource blocks to real infrastructure IDs.

```json
{
  "version": 4,
  "terraform_version": "1.6.0",
  "resources": [
    {
      "mode": "managed",
      "type": "aws_instance",
      "name": "web",
      "instances": [
        {
          "schema_version": 1,
          "attributes": {
            "id": "i-0abc123def456789",
            "ami": "ami-0c55b159cbfafe1f0",
            "instance_type": "t3.micro",
            "public_ip": "54.1.2.3",
            "tags": { "Name": "web-server" }
          }
        }
      ]
    }
  ]
}
```

---

## Why State Matters

```text
State is what lets Terraform:
  1. Know that aws_instance.web = i-0abc123def456789
     (so it can update/delete the right instance)
  2. Compute the diff during plan
     (desired HCL vs current state vs actual API)
  3. Track dependencies between resources
  4. Store attribute values computed at apply time
     (IDs, IPs, ARNs that are "known after apply")

Without state:
  Terraform has no idea what it previously created.
  Running apply would try to create everything again.
```

---

## State Rules

```text
NEVER:
  - Edit state manually (use terraform state commands or moved blocks)
  - Commit state to git (it contains secrets, passwords, private keys)
  - Delete state without a backup (you lose the mapping to real resources)
  - Let two applies run simultaneously (race condition corrupts state)

ALWAYS:
  - Use remote state for teams (S3, GCS, Terraform Cloud)
  - Enable state locking (DynamoDB for S3, native locking for GCS/TFC)
  - Back up state before major operations
  - Use state encryption for sensitive values
```

---

## Local Backend (Default)

```hcl
terraform {
  backend "local" {
    path = "terraform.tfstate"  # default; can omit
  }
}
```

```text
Stored in: terraform.tfstate (and terraform.tfstate.backup)
Locking:   file-based lock (only works on the same machine)
Good for:  local development, learning, single developer

Problems:
  - Cannot be shared with a team
  - Not versioned (unless you commit it — don't)
  - No remote locking: two developers can corrupt state simultaneously
```

---

## Remote Backends

Remote backends store state in a shared location, enabling team collaboration.

### S3 Backend (Most Common for AWS)

```hcl
terraform {
  backend "s3" {
    bucket         = "my-company-terraform-state"
    key            = "services/order-api/prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true                        # server-side encryption
    dynamodb_table = "terraform-state-lock"      # locking table
    
    # Optional: assume a role for cross-account state
    role_arn = "arn:aws:iam::123456789:role/TerraformStateRole"
  }
}
```

S3 bucket requirements:

```bash
# Create state bucket
aws s3api create-bucket \
  --bucket my-company-terraform-state \
  --region us-east-1

# Enable versioning (recover from state corruption)
aws s3api put-bucket-versioning \
  --bucket my-company-terraform-state \
  --versioning-configuration Status=Enabled

# Enable server-side encryption
aws s3api put-bucket-encryption \
  --bucket my-company-terraform-state \
  --server-side-encryption-configuration \
    '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

# Block public access
aws s3api put-public-access-block \
  --bucket my-company-terraform-state \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
```

DynamoDB locking table:

```bash
aws dynamodb create-table \
  --table-name terraform-state-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

### GCS Backend (Google Cloud)

```hcl
terraform {
  backend "gcs" {
    bucket  = "my-company-terraform-state"
    prefix  = "services/order-api/prod"
    # Locking is built-in; no separate table needed
  }
}
```

### Terraform Cloud / HCP Terraform Backend

```hcl
terraform {
  cloud {
    organization = "my-org"
    workspaces {
      name = "order-api-prod"
    }
  }
}
```

---

## State Locking

```text
State locking prevents concurrent applies from corrupting state.

How it works (S3 + DynamoDB):
  1. terraform apply starts
  2. Writes a lock entry to DynamoDB: { LockID: "bucket/key", Info: {...} }
  3. Runs the apply
  4. Deletes the lock entry when done

If another apply runs simultaneously:
  → It tries to write the same LockID to DynamoDB
  → DynamoDB conditional write fails
  → Terraform errors: "Error locking state: ConditionalCheckFailedException"
  → User sees: "Terraform acquire a state lock"

If a process crashed mid-apply and left a stale lock:
  terraform force-unlock <LOCK_ID>
  # Only do this if you are certain no apply is running!
```

---

## State Operations

### List Resources In State

```bash
terraform state list

# Output:
# aws_vpc.main
# aws_subnet.public[0]
# aws_subnet.public[1]
# module.eks.aws_eks_cluster.this
# module.eks.aws_eks_node_group.workers
```

### Inspect A Resource

```bash
terraform state show aws_vpc.main

# Output:
# resource "aws_vpc" "main" {
#   arn                              = "arn:aws:ec2:us-east-1:123456:vpc/vpc-0abc1234"
#   cidr_block                       = "10.0.0.0/16"
#   id                               = "vpc-0abc1234"
#   ...
# }
```

### Remove A Resource From State (Without Deleting It)

```bash
# Use case: you want Terraform to "forget" a resource (e.g., manage it elsewhere)
# The real resource is NOT deleted.
terraform state rm aws_instance.web

# Remove a module
terraform state rm module.old_module
```

### Move A Resource In State

```bash
# Rename without destroying:
terraform state mv aws_instance.web aws_instance.api

# Move into a module:
terraform state mv aws_instance.web module.compute.aws_instance.web

# Preferred modern approach: moved block in HCL (see Sheet 12)
```

### Backup State Before Dangerous Operations

```bash
# Pull current remote state to local backup
terraform state pull > backup-$(date +%Y%m%d-%H%M%S).tfstate

# Push state back (use only for recovery)
terraform state push backup-20240101-120000.tfstate
```

---

## Sensitive Values In State

```text
WARNING: State is not a secrets store.

Values marked sensitive = true in variables/outputs are still stored in
plaintext in the state file. This includes:
  - Database passwords
  - API keys passed as variables
  - Generated private keys (tls_private_key resource)
  - Any computed secret attribute

Mitigations:
  1. Encrypt state at rest (S3 SSE, GCS encryption — always enable)
  2. Restrict access to state bucket to only Terraform execution roles
  3. Use Vault provider or AWS Secrets Manager to store and retrieve secrets
     (not pass them as Terraform variables)
  4. Never print state output in CI/CD logs
```

---

## State Drift

Drift = difference between actual infrastructure and Terraform state.

```bash
# Detect drift (refresh state from actual APIs, show diff)
terraform plan -refresh-only

# Apply the refresh (update state to match reality, no resource changes)
terraform apply -refresh-only

# Ignore drift for a specific attribute
resource "aws_instance" "web" {
  lifecycle {
    ignore_changes = [tags, user_data]  # ignore out-of-band tag changes
  }
}
```

---

## Interview Sound Bite

Terraform state is the JSON mapping from your HCL resource blocks to real infrastructure IDs. Without it, Terraform cannot compute diffs or track existing resources. For teams, remote state in S3 with DynamoDB locking is the standard AWS pattern: S3 stores the state (versioned, encrypted), DynamoDB provides distributed locking to prevent concurrent applies from corrupting it. State should never be committed to git (it contains secrets) and should never be edited manually — use `terraform state mv`, `moved` blocks, or `terraform state rm`. State drift (real infrastructure changed outside Terraform) is detected with `terraform plan -refresh-only`.
