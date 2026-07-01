# Terraform Mastery Lab — Learning Path

Follow this sequence to build hands-on intuition alongside the Hot Sheets.

---

## Week 1: Foundations (Sheets 1–5)

### Day 1–2: First Terraform Config
1. Install Terraform and AWS CLI
2. Create a simple S3 bucket in `EXAMPLES/aws-basic-vpc/`
3. Practice: `terraform init` → `terraform plan` → `terraform apply` → `terraform destroy`
4. Examine `.terraform/` directory and `.terraform.lock.hcl`

### Day 3: HCL Syntax
1. Add variables to your config (variable, default, type, description)
2. Add outputs (value, description, sensitive)
3. Add locals (computed name prefix, common tags)
4. Practice: `terraform console` → test expressions interactively

### Day 4: State
1. Examine `terraform.tfstate` in a text editor
2. Run `terraform state list` and `terraform state show`
3. Run `terraform state pull` and pipe to `python3 -m json.tool`
4. Optional: set up an S3 backend + DynamoDB table (see Sheet 4)

### Day 5: Providers
1. Add a second provider alias (different region)
2. Create a resource in each region
3. Examine `.terraform.lock.hcl` — verify both providers locked
4. Run `terraform providers lock -platform=linux_amd64`

---

## Week 2: Intermediate (Sheets 6–11)

### Day 1: Modules
1. Extract your VPC into `EXAMPLES/module-example/`
2. Create `variables.tf` and `outputs.tf` for the module
3. Call the module from a root config
4. Practice: `module.vpc.vpc_id`, `module.vpc.subnet_ids`

### Day 2: Meta-Arguments
1. Convert a resource to use `for_each` (map of subnets)
2. Add `lifecycle { prevent_destroy = true }` to the VPC
3. Add `lifecycle { create_before_destroy = true }` to a security group
4. Test `depends_on` with a manual dependency

### Day 3: Functions and Expressions
1. Use `cidrsubnet()` to generate CIDRs dynamically
2. Use a `for` expression to create a map of subnet IDs
3. Use a `dynamic` block for security group ingress rules
4. Test functions in `terraform console`

### Day 4: Data Sources
1. Use `data "aws_ami"` to find latest Amazon Linux AMI
2. Use `data "aws_caller_identity"` and `data "aws_region"`
3. Use `data "aws_iam_policy_document"` to build an IAM policy
4. Use `terraform.workspace` to conditionally size resources

### Day 5: Testing
1. Add a variable validation block with `contains()` check
2. Add a precondition to a resource (verify AMI architecture)
3. Write a `terraform test` unit test (plan-only) for naming conventions

---

## Week 3: Senior Production (Sheets 12–18)

### Day 1: State Operations
1. Practice `terraform state mv` on a test resource (rename it)
2. Write a `moved {}` block and verify in plan output
3. Import an existing resource with `terraform import`
4. Practice `terraform state rm` + re-import

### Day 2: Modules Design
1. Build `EXAMPLES/multi-env-pattern/` with dev/ and prod/ directories
2. Both call the same shared module with different inputs
3. Separate state files (different backend keys)

### Day 3: Remote Backend
1. Set up S3 + DynamoDB backend following Sheet 14
2. Run `terraform init -migrate-state` from local to S3
3. Verify state is in S3: `aws s3 ls s3://...`
4. Test locking: observe DynamoDB item during apply

### Day 4: Performance
1. Run `terraform plan -refresh=false` vs normal plan — time the difference
2. Try `terraform plan -target=` to apply only one resource
3. Split a config into two smaller root modules

### Day 5: CI/CD Simulation
1. Script a local CI/CD simulation: validate → plan → manual confirm → apply
2. Practice `terraform plan -out=tfplan` then `terraform apply tfplan`
3. Use `terraform show -json tfplan | python3 -m json.tool` to examine plan JSON

---

## Week 4: MAANG Level (Sheets 19–29)

### Day 1–2: Full Stack
1. Build the VPC + EKS + RDS example from Sheet 20
2. Practice module composition: VPC outputs → EKS inputs
3. Apply in layers: VPC first → EKS → RDS

### Day 3: Scenario Practice
1. Simulate state drift: manually change a tag in AWS console, run `terraform plan -refresh-only`
2. Simulate partial apply: interrupt a running apply, examine state, recover
3. Run `terraform force-unlock` on a test stale lock

### Day 4: Refactoring
1. Take a flat config (all resources in root main.tf)
2. Extract into modules using `moved {}` blocks
3. Verify plan shows "has moved to" — NOT create/destroy

### Day 5: Review and Mock Interview
1. Review the Active Recall Drills (Sheet 27)
2. Explain each item on the Production Readiness Checklist (Sheet 28)
3. Practice answering the Senior Q&A aloud (Sheet 25)
