# 03. Terraform CLI: Init, Plan, Apply Workflow

## The Core Workflow

```text
Write HCL
    │
    ▼
terraform init        ← download providers, initialize backend
    │
    ▼
terraform plan        ← compute diff: desired vs actual state
    │
    ▼
(review plan)         ← human or CI approval gate
    │
    ▼
terraform apply       ← execute changes against cloud API
    │
    ▼
terraform destroy     ← (when done) delete all managed resources
```

---

## terraform init

Downloads providers, initializes the backend, installs modules.

```bash
terraform init

# Re-initialize after provider version changes
terraform init -upgrade

# Initialize without checking remote backend (for testing)
terraform init -backend=false

# Specify backend config at init time (for dynamic backend config)
terraform init \
  -backend-config="bucket=my-tf-state" \
  -backend-config="key=prod/terraform.tfstate" \
  -backend-config="region=us-east-1"

# Migrate state to a new backend
terraform init -migrate-state

# Reconfigure backend (e.g., bucket changed) without migration
terraform init -reconfigure
```

What `terraform init` creates:

```text
.terraform/
  providers/
    registry.terraform.io/hashicorp/aws/5.x.x/darwin_arm64/
      terraform-provider-aws_v5.x.x_x5   ← provider binary
  modules/
    vpc/                                   ← downloaded module source
      
.terraform.lock.hcl   ← exact provider versions + checksums (commit to git)
```

---

## terraform plan

Computes the execution plan. **Never modifies anything.** Safe to run repeatedly.

```bash
terraform plan

# Save plan to file (use for apply in CI/CD)
terraform plan -out=tfplan

# Show what a destroy would do
terraform plan -destroy

# Target specific resources (use sparingly)
terraform plan -target=aws_instance.web
terraform plan -target=module.vpc

# Pass variable values
terraform plan -var="environment=prod" -var="instance_type=t3.large"
terraform plan -var-file="prod.tfvars"

# Refresh state from real API before planning
terraform plan -refresh=true   # default; reads actual resource state
terraform plan -refresh=false  # skip refresh (faster, uses cached state)

# Compact output
terraform plan -compact-warnings
```

### Reading Plan Output

```text
Terraform will perform the following actions:

  # aws_instance.web will be created         ← NEW resource
  + resource "aws_instance" "web" {
      + ami                     = "ami-0c55b159cbfafe1f0"
      + instance_type           = "t3.micro"
      + (known after apply)     = (computed)
    }

  # aws_security_group.web will be updated   ← IN-PLACE UPDATE
  ~ resource "aws_security_group" "web" {
      ~ description = "old" -> "new"
    }

  # aws_db_instance.main must be replaced    ← DESTROY + RECREATE
-/+ resource "aws_db_instance" "main" {
      ~ identifier = "old-db" -> "new-db"   # forces replacement
    }

  # aws_s3_bucket.logs will be destroyed     ← DELETION
  - resource "aws_s3_bucket" "logs" {
      - bucket = "my-logs-bucket"
    }

Plan: 1 to add, 1 to change, 1 to destroy.
```

Symbols:
- `+` = create
- `-` = destroy
- `~` = update in-place
- `-/+` = destroy and recreate (replacement)
- `<=` = data source read

---

## terraform apply

Executes the plan against the cloud API.

```bash
# Interactive: shows plan, asks for confirmation
terraform apply

# Apply a saved plan file (no confirmation needed — used in CI/CD)
terraform apply tfplan

# Skip confirmation prompt (dangerous without review)
terraform apply -auto-approve

# Apply with variable overrides
terraform apply -var="environment=prod"

# Target a specific resource (partial apply — use with care)
terraform apply -target=aws_instance.web
```

### Apply Output

```text
aws_vpc.main: Creating...
aws_vpc.main: Creation complete after 3s [id=vpc-0abc1234]
aws_subnet.public[0]: Creating...
aws_subnet.public[1]: Creating...
aws_subnet.public[0]: Creation complete after 1s [id=subnet-0abc1111]
aws_subnet.public[1]: Creation complete after 1s [id=subnet-0abc2222]

Apply complete! Resources: 3 added, 0 changed, 0 destroyed.

Outputs:
  vpc_id = "vpc-0abc1234"
```

---

## terraform destroy

Deletes all resources managed by the current configuration.

```bash
# Interactive confirm
terraform destroy

# Skip confirmation
terraform destroy -auto-approve

# Destroy specific resource
terraform destroy -target=aws_instance.web

# Plan before destroying
terraform plan -destroy
```

---

## Other Essential Commands

### terraform fmt

```bash
# Format all .tf files in current directory
terraform fmt

# Recursive (subdirectories too)
terraform fmt -recursive

# Check only (exit 1 if files need formatting, no changes)
terraform fmt -check

# Show diff
terraform fmt -diff
```

### terraform validate

```bash
# Check HCL syntax and internal consistency (no API calls)
terraform validate

# JSON output for tooling
terraform validate -json
```

### terraform show

```bash
# Show current state in human-readable format
terraform show

# Show a saved plan
terraform show tfplan

# Show state as JSON
terraform show -json
```

### terraform output

```bash
# Show all outputs
terraform output

# Show specific output
terraform output vpc_id

# JSON format (for CI/CD)
terraform output -json

# Raw value (no quotes, for shell scripts)
terraform output -raw instance_ip
```

### terraform refresh (Deprecated in 1.x)

```bash
# Update state to match actual infrastructure (use plan -refresh-only instead)
terraform apply -refresh-only
terraform plan -refresh-only     # see what drift exists
```

### terraform console

```bash
# Interactive REPL for evaluating expressions
terraform console

# Inside console:
> var.environment
"dev"
> aws_instance.web.public_ip
"1.2.3.4"
> cidrsubnet("10.0.0.0/16", 8, 0)
"10.0.0.0/24"
> length(var.availability_zones)
3
```

---

## terraform state Commands

```bash
# List all resources in state
terraform state list

# Show details of a specific resource
terraform state show aws_instance.web

# Remove a resource from state (without destroying the real resource)
terraform state rm aws_instance.web

# Move a resource to a new address (rename without destroy)
terraform state mv aws_instance.web aws_instance.api

# Pull state from remote backend to stdout
terraform state pull > local-state-backup.json

# Push local state to remote backend (use with extreme caution)
terraform state push local-state-backup.json
```

---

## Environment Variables

```bash
# Disable interactive confirmation
TF_INPUT=false terraform apply

# Log level for debugging
TF_LOG=DEBUG terraform apply   # TRACE, DEBUG, INFO, WARN, ERROR

# Log to file
TF_LOG_PATH=/tmp/terraform.log

# Provide variable values
TF_VAR_environment=prod
TF_VAR_instance_type=t3.large

# Disable color output (for CI)
TF_CLI_ARGS="-no-color"

# Disable checkpoint (usage reporting to HashiCorp)
CHECKPOINT_DISABLE=1
```

---

## Interview Sound Bite

The core Terraform workflow: `init` downloads providers and initializes the backend; `plan` computes the diff between desired HCL and actual state — always read-only, never destructive; `apply` executes the plan by calling cloud APIs. In CI/CD: save the plan with `-out=tfplan` and apply with `terraform apply tfplan` to guarantee exactly what was reviewed gets applied. `-target` is a scalpel for partial applies but creates state inconsistency if overused. `terraform console` is an interactive REPL for testing HCL expressions and functions without applying anything.
