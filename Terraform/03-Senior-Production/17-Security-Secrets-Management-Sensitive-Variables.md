# 17. Security and Secrets Management

## The Core Problem: Secrets In State

```text
terraform.tfstate is a JSON file. It stores the full attributes of every resource.

What appears in state:
  - RDS master password (as plaintext)
  - EC2 key pair private key (if created with tls_private_key resource)
  - IAM access key secret (if created with aws_iam_access_key resource)
  - Any variable value, including those marked sensitive = true

sensitive = true hides values from:
  - terraform plan output
  - terraform apply output
  - terraform output (shows "(sensitive value)")
  - CI/CD logs

sensitive = true does NOT protect from:
  - terraform state show (shows everything)
  - terraform state pull (dumps raw state JSON)
  - Anyone with read access to the state file in S3
```

---

## sensitive Variables And Outputs

```hcl
# Input variable: value hidden in plan/apply output
variable "db_password" {
  type      = string
  sensitive = true
}

# Output: sensitive = true hides value in CLI and in remote state data source
output "db_connection_string" {
  value     = "postgresql://${var.db_user}:${var.db_password}@${aws_db_instance.main.endpoint}/app"
  sensitive = true
}

# Resource attribute: mark generated secrets as sensitive
resource "aws_db_instance" "main" {
  identifier = "prod-db"
  password   = var.db_password  # var is sensitive → attribute is sensitive too
  # ...
  
  lifecycle {
    ignore_changes = [password]  # ignore out-of-band password rotations
  }
}
```

---

## Providing Secrets Without Putting Them In HCL

### Method 1: Environment Variables

```bash
# TF_VAR_<name> environment variables
export TF_VAR_db_password="$(aws ssm get-parameter --name /prod/db/password --with-decryption --query Parameter.Value --output text)"
terraform apply
```

### Method 2: AWS SSM Parameter Store (Data Source)

```hcl
data "aws_ssm_parameter" "db_password" {
  name            = "/prod/db/password"
  with_decryption = true
}

resource "aws_db_instance" "main" {
  password = data.aws_ssm_parameter.db_password.value
  # Still stored in state, but not in HCL or tfvars
}
```

### Method 3: AWS Secrets Manager (Data Source)

```hcl
data "aws_secretsmanager_secret_version" "db_creds" {
  secret_id = "prod/rds/master-credentials"
}

locals {
  db_creds = jsondecode(data.aws_secretsmanager_secret_version.db_creds.secret_string)
}

resource "aws_db_instance" "main" {
  username = local.db_creds["username"]
  password = local.db_creds["password"]
}
```

### Method 4: HashiCorp Vault Provider

```hcl
provider "vault" {
  address = "https://vault.mycompany.com"
  # Token provided via VAULT_TOKEN env var (not in HCL)
}

data "vault_generic_secret" "db" {
  path = "secret/prod/db"
}

resource "aws_db_instance" "main" {
  username = data.vault_generic_secret.db.data["username"]
  password = data.vault_generic_secret.db.data["password"]
}
```

---

## SOPS For Encrypted tfvars

SOPS (Secrets OPerationS) encrypts secrets files with AWS KMS, GCP KMS, or age keys. The encrypted file can be committed to git.

```bash
# Encrypt a tfvars file with KMS
sops --kms arn:aws:kms:us-east-1:123456789:key/abc123 --encrypt secrets.tfvars > secrets.enc.tfvars

# Decrypt and apply (in CI/CD)
sops --decrypt secrets.enc.tfvars > /tmp/decrypted.tfvars
terraform apply -var-file=/tmp/decrypted.tfvars
rm /tmp/decrypted.tfvars  # clean up immediately
```

---

## State Encryption

```text
S3 backend — encrypt = true and KMS:
  backend "s3" {
    bucket     = "..."
    encrypt    = true          # enables AES-256 SSE
    kms_key_id = "arn:..."    # use customer-managed KMS key
  }

Who can decrypt:
  - Any IAM principal with kms:Decrypt + s3:GetObject
  - Enforce least privilege: only the Terraform runner role

Enable S3 access logging to audit who reads state:
  aws_s3_bucket_logging → writes access logs to a separate bucket
```

---

## IAM Least Privilege For Terraform

```text
NEVER give Terraform administrator access to an account.
Use a scoped Terraform role with only what it needs.

For typical app deployment, the Terraform role needs:
  EC2 / VPC: ec2:*, elasticloadbalancing:*
  EKS: eks:*
  RDS: rds:*
  S3: s3:GetObject, s3:PutObject, s3:ListBucket (for state)
  KMS: kms:Decrypt, kms:GenerateDataKey (for state encryption)
  DynamoDB: dynamodb:GetItem, dynamodb:PutItem, dynamodb:DeleteItem (for lock)
  IAM: iam:CreateRole, iam:AttachRolePolicy, iam:PassRole (dangerous — limit carefully)

IAM PassRole risk:
  If Terraform can iam:PassRole + iam:CreateRole + iam:AttachPolicy,
  it can create an admin role and pass it to an EC2 instance.
  Restrict PassRole: allow only passing roles to specific services.
```

---

## OIDC Dynamic Credentials (No Static Keys)

```text
Instead of storing AWS_ACCESS_KEY_ID in CI/CD:

1. Create OIDC Identity Provider in AWS IAM (trusted for GitHub/TFC)
2. Create IAM role with trust policy that validates the OIDC JWT
3. GitHub Actions / TFC requests a JWT from GitHub's OIDC endpoint
4. AWS STS validates JWT and returns temporary credentials (15-60 min)
5. Credentials auto-expire; no rotation needed; no storage needed
```

```yaml
# GitHub Actions OIDC example
jobs:
  terraform:
    permissions:
      id-token: write  # required for OIDC
      contents: read
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789:role/TerraformGitHubRole
          aws-region: us-east-1
          # No access-key-id or secret-access-key needed!
```

---

## Security Anti-Patterns To Avoid

```text
1. Committing terraform.tfstate to git
   → Contains plaintext passwords and keys
   
2. Committing *.tfvars with secrets
   → Even if .gitignored, one mistake exposes everything
   
3. Using admin/root credentials for Terraform
   → If leaked, attacker has full account access
   
4. Outputting sensitive values to logs
   → CI/CD logs are often stored and readable by many engineers
   
5. Using aws_iam_access_key resource
   → Creates long-lived static credentials; prefer OIDC or instance roles
   
6. Passing secrets via -var CLI flags
   → These appear in shell history and process list
   
7. Ignoring state access control
   → Anyone who can read state can read all secrets
```

---

## Interview Sound Bite

The biggest Terraform security mistake is misunderstanding what `sensitive = true` protects — it only hides values from plan/apply CLI output, NOT from the state file itself. State should be treated as a secrets store: encrypt it with KMS, restrict S3 bucket access to the Terraform runner role only, and enable bucket versioning for recovery. For providing secrets to Terraform, use data sources (SSM Parameter Store, Secrets Manager, Vault) instead of tfvars files. For CI/CD credentials, use OIDC federation (GitHub Actions → AWS OIDC trust → temporary STS credentials) — zero stored secrets. Never run Terraform with admin credentials; scope the IAM role to exactly what the configuration needs.
