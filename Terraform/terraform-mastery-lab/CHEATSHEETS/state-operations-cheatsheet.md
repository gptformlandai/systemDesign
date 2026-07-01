# State Operations Cheatsheet

## Read State

```bash
terraform state list                    # list all resource addresses
terraform state show <address>          # inspect full resource attributes
terraform state pull                    # dump raw state JSON to stdout
terraform state pull | python3 -m json.tool | head -100  # pretty-print
```

## Modify State (No API Calls)

```bash
# Rename resource address (manual, not in VCS)
terraform state mv aws_instance.web aws_instance.api
terraform state mv aws_vpc.main module.vpc.aws_vpc.this
terraform state mv 'aws_subnet.public[0]' 'module.vpc.aws_subnet.public[0]'

# Remove from state without destroying real resource
terraform state rm aws_instance.old
terraform state rm module.old_module
terraform state rm 'aws_sg.this["web"]'
```

## moved Blocks (Preferred Over state mv)

```hcl
# In moved.tf — committed to VCS, shown in plan output
moved {
  from = aws_instance.web
  to   = aws_instance.api
}

moved {
  from = aws_vpc.main
  to   = module.vpc.aws_vpc.this
}

moved {
  from = aws_subnet.public[0]
  to   = module.vpc.aws_subnet.public[0]
}
```

## Import Existing Resources

```bash
# CLI method (need resource block in HCL first)
terraform import aws_vpc.main vpc-0abc1234
terraform import aws_instance.web i-0abc123def456789
terraform import 'aws_subnet.this["public-1"]' subnet-0abc1111
terraform import module.vpc.aws_vpc.this vpc-0abc1234
```

```hcl
# Declarative method (TF 1.5+) — in import.tf
import {
  to = aws_vpc.main
  id = "vpc-0abc1234"
}
```

```bash
# Generate HCL from existing resource (TF 1.5+)
terraform plan -generate-config-out=generated.tf
terraform apply  # imports + writes to state
```

## Backup and Restore

```bash
# Backup current state
terraform state pull > backup-$(date +%Y%m%d-%H%M%S).json

# Restore from backup (DANGEROUS: overwrites remote state)
terraform state push backup-20240115-120000.json

# List S3 versions (find last good version)
aws s3api list-object-versions \
  --bucket <BUCKET> \
  --prefix <KEY> \
  --query 'Versions[*].[VersionId,LastModified]' \
  --output table

# Restore specific S3 version
aws s3api get-object \
  --bucket <BUCKET> \
  --key <KEY> \
  --version-id <VERSION_ID> \
  recovered-state.json
terraform state push recovered-state.json
```

## State Locking

```bash
# Release stale lock (ONLY when certain no apply is running)
terraform force-unlock <LOCK_ID>

# Lock ID shown in error:
# "Error locking state: ... Lock Info: ID: abc-123-def-456"

# Check DynamoDB lock table
aws dynamodb scan \
  --table-name terraform-state-lock \
  --query 'Items[*].{ID:LockID,Who:Info}'
```

## Drift Detection

```bash
# Detect drift (updates state from real APIs, shows diff)
terraform plan -refresh-only -detailed-exitcode
# Exit 0: no drift
# Exit 2: drift detected

# Accept drift into state (no resource changes)
terraform apply -refresh-only

# Revert drift (re-apply desired config)
terraform apply
```

## Common Address Formats

```text
aws_vpc.main                              # root resource
module.vpc.aws_vpc.this                   # resource in module
aws_subnet.public[0]                      # count-indexed
aws_subnet.public["us-east-1a"]           # for_each key
module.vpc.aws_subnet.public[0]           # count in module
module.vpc.aws_subnet.public["us-east-1a"] # for_each in module
module.network.module.vpc.aws_vpc.this    # nested module
```
