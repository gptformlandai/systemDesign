# Runbook: Import Existing Resources Into Terraform

**Use when:** Resources were created manually or by another tool and you want Terraform to manage them going forward.

---

## Decision: CLI Import vs Import Blocks

```text
CLI import (`terraform import`):
  - Ad-hoc, immediate
  - You must write the HCL resource block first
  - Good for importing one or two resources

Import blocks (TF 1.5+):
  - Declarative, committed to git
  - Can auto-generate HCL with -generate-config-out
  - Good for bulk imports or team-reviewed imports
```

---

## Method 1: CLI Import

### Step 1: Write The Resource Block

The resource block must exist in HCL before importing. Write a minimal block with at least the required attributes.

```hcl
# main.tf
resource "aws_vpc" "shared" {
  # Minimum required; will be updated by plan after import
  cidr_block = "10.0.0.0/16"
}
```

### Step 2: Find The Resource ID

```bash
# AWS CLI to find resource IDs:
aws ec2 describe-vpcs --query 'Vpcs[*].[VpcId,CidrBlock,Tags[?Key==`Name`].Value|[0]]' --output table
aws ec2 describe-instances --query 'Reservations[*].Instances[*].[InstanceId,State.Name,Tags[?Key==`Name`].Value|[0]]' --output table
aws rds describe-db-instances --query 'DBInstances[*].[DBInstanceIdentifier,DBInstanceStatus]' --output table
aws s3 ls
aws iam list-roles --query 'Roles[*].[RoleName,RoleId]' --output table
```

### Step 3: Run The Import

```bash
# General format:
terraform import <RESOURCE_ADDRESS> <REAL_RESOURCE_ID>

# Common examples:
terraform import aws_vpc.shared vpc-0abc1234
terraform import aws_subnet.public subnet-0abc1234
terraform import aws_instance.web i-0abc123def456789
terraform import aws_security_group.alb sg-0abc1234
terraform import aws_s3_bucket.data my-bucket-name
terraform import aws_iam_role.app my-role-name
terraform import aws_db_instance.main my-rds-identifier
terraform import aws_eks_cluster.main my-cluster-name
terraform import aws_route53_zone.main ZONE_ID

# For_each resources:
terraform import 'aws_subnet.public["us-east-1a"]' subnet-0abc1111

# Module resources:
terraform import module.vpc.aws_vpc.this vpc-0abc1234
```

### Step 4: Reconcile HCL With State

```bash
# Run plan — will show attributes that differ from HCL
terraform plan -no-color 2>&1 | head -100

# Adjust HCL to match actual resource attributes
# (add missing attributes, remove incorrect ones)
# Repeat until plan shows "No changes"
```

---

## Method 2: Import Blocks (Terraform 1.5+)

### Step 1: Create import.tf

```hcl
# import.tf
import {
  to = aws_vpc.shared
  id = "vpc-0abc1234"
}

import {
  to = aws_subnet.public
  id = "subnet-0abc1111"
}

import {
  to = module.vpc.aws_vpc.this
  id = "vpc-0abc1234"
}
```

### Step 2: Generate HCL Automatically

```bash
# This reads actual resource attributes and generates the resource block
terraform plan -generate-config-out=generated.tf -no-color

# Review generated.tf — it contains the resource blocks with all attributes
# Move/merge these into your proper .tf files
# Remove or adjust attributes that Terraform will manage
```

### Step 3: Apply The Import

```bash
# Apply imports the resources into state AND writes generated config
terraform apply

# After apply: remove the import blocks from import.tf (they're one-time use)
# Run plan to confirm no changes needed
terraform plan   # should show "No changes"
```

---

## Bulk Import Script

```bash
#!/bin/bash
# Example: import all subnets in a VPC

VPC_ID="vpc-0abc1234"

# Get all subnet IDs
SUBNETS="$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query 'Subnets[*].SubnetId' \
  --output text)"

# Generate import blocks
for subnet_id in $SUBNETS; do
  echo "import {"
  echo "  to = aws_subnet.imported[\"$subnet_id\"]"
  echo "  id = \"$subnet_id\""
  echo "}"
done > import-subnets.tf

# Then run: terraform plan -generate-config-out=generated-subnets.tf
```

---

## Common Resource ID Formats

| Resource | Import ID format |
|---|---|
| `aws_vpc` | `vpc-0abc1234` |
| `aws_subnet` | `subnet-0abc1234` |
| `aws_instance` | `i-0abc123def456789` |
| `aws_security_group` | `sg-0abc1234` |
| `aws_s3_bucket` | `bucket-name` |
| `aws_iam_role` | `role-name` |
| `aws_iam_policy` | `arn:aws:iam::123:policy/my-policy` |
| `aws_db_instance` | `db-identifier` |
| `aws_eks_cluster` | `cluster-name` |
| `aws_route53_zone` | `ZONE_ID` (e.g., `Z1ABCDEFGHIJ`) |
| `aws_route53_record` | `ZONE_ID_NAME_TYPE` |
| `aws_lb` | `arn:aws:elasticloadbalancing:...` |
| `aws_lb_target_group` | `arn:aws:elasticloadbalancing:...` |
| `aws_kms_key` | `key-arn` or `key-id` |

---

## Post-Import Checklist

```text
After importing all resources:
  ✓ terraform plan shows "No changes" for all imported resources
  ✓ terraform state list shows all expected resource addresses
  ✓ Remove import blocks from HCL (one-time use)
  ✓ Add lifecycle { prevent_destroy = true } to critical imported resources
  ✓ Add lifecycle { ignore_changes = [...] } for expected out-of-band attributes
  ✓ Commit HCL to git with message "feat: import existing X resources into Terraform"
```
