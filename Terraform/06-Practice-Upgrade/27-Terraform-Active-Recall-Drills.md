# 27. Terraform Active Recall Drills

Test yourself on these drills. Cover the answer, recall it, then reveal.
Rated: [F] = Foundations, [I] = Intermediate, [S] = Senior

---

## Core Workflow

**[F] Q: What are the three core Terraform workflow commands and what does each do?**

`terraform init` — downloads providers, initializes backend, installs modules
`terraform plan` — computes diff between HCL + state + reality; read-only, never modifies
`terraform apply` — executes the plan by calling cloud APIs

---

**[F] Q: What flag saves a Terraform plan to a file, and why is this important in CI/CD?**

`-out=tfplan` saves the plan binary. In CI/CD: `plan -out=tfplan` then `apply tfplan` guarantees exactly the reviewed plan is applied, with no re-plan race condition.

---

**[F] Q: What are the three plan change symbols and what do they mean?**

`+` = create, `~` = update in-place, `-` = destroy. Special: `-/+` = destroy then recreate (replacement).

---

**[F] Q: What exit codes does terraform plan return?**

`0` = no changes, `1` = error, `2` = changes present. Use `-detailed-exitcode` to get exit code 2 (useful for drift detection in CI).

---

## State

**[F] Q: What three things does Terraform state enable?**

1. Maps HCL resource addresses to real infrastructure IDs
2. Enables diff computation during plan
3. Stores computed attributes (IPs, ARNs, IDs) known only after apply

---

**[F] Q: What is state locking and which AWS services implement it?**

State locking prevents two applies from running simultaneously (concurrent writes corrupt state). AWS: S3 stores state, DynamoDB provides locking (one row per LockID).

---

**[I] Q: What is the difference between terraform state mv and a moved block?**

`terraform state mv` is a manual CLI operation — immediate, not in VCS, not visible to teammates.
`moved {}` block is declarative — in HCL, committed to git, shows in plan output as "has moved to", applied to all teammates on next apply.

---

**[I] Q: When would you use terraform state rm?**

To remove a resource from Terraform state WITHOUT deleting the real resource. Use case: moving ownership to a different Terraform config, or un-adopting a resource.

---

**[I] Q: What does terraform plan -refresh-only do?**

Reads actual resource state from cloud APIs and shows what would change in state to match reality. Used for drift detection. `apply -refresh-only` updates state without modifying real resources.

---

**[S] Q: What is the order to recover from a corrupted state file?**

1. `terraform state pull > /tmp/corrupt.json` — capture current (corrupt) state
2. `aws s3api list-object-versions --bucket ... --key ...` — find last good version
3. `aws s3api get-object --version-id <GOOD_ID>` — download good version
4. `terraform state push good-state.json` — restore good state
5. `terraform plan` — verify no unexpected changes

---

## Variables and Types

**[F] Q: What five methods can you use to provide variable values, in order of precedence?**

1. `-var` CLI flag (highest)
2. `-var-file` CLI flag
3. `*.auto.tfvars` files
4. `terraform.tfvars`
5. `TF_VAR_<name>` environment variable (lowest)

---

**[F] Q: What does sensitive = true protect and what does it NOT protect?**

Protects: CLI plan/apply output, terraform output command, CI/CD logs
Does NOT protect: the state file (secrets stored as plaintext in state)

---

**[I] Q: What is the difference between list(string), set(string), and map(string)?**

`list(string)` — ordered, duplicate elements allowed
`set(string)` — unordered, unique elements, used with `for_each`
`map(string)` — key-value pairs, string keys with string values

---

**[I] Q: What is the optional() function in object variables?**

Available in TF 1.3+. `optional(type, default)` marks an object attribute as not required and provides a default: `disk_size_gb = optional(number, 50)`.

---

## Modules

**[F] Q: What three files are the minimum for a reusable module?**

`main.tf` (resources), `variables.tf` (inputs), `outputs.tf` (outputs). `README.md` is strongly recommended.

---

**[I] Q: How do you pass a provider alias into a module?**

Use the `providers` meta-argument:
```hcl
module "eu_resources" {
  source    = "./modules/regional"
  providers = { aws = aws.eu_west }
}
```
The module must declare `configuration_aliases = [aws.primary]` in `required_providers`.

---

**[S] Q: What is the difference between thin root and fat modules?**

Thin root: root module contains only glue code (module calls + output wiring). All resources live in child modules. Fat modules: each module has a narrow, single responsibility. Fat root: anti-pattern — resources defined directly in root, no reuse.

---

## Meta-Arguments

**[F] Q: What is the key difference between count and for_each for named resources?**

`count` identifies resources by integer index — removing item 0 shifts all higher indices, causing unnecessary updates. `for_each` identifies resources by key — removing a key only destroys that resource, others untouched.

---

**[I] Q: What lifecycle arguments are available and when do you use each?**

`create_before_destroy = true` — zero-downtime replacement (new before old)
`prevent_destroy = true` — guard against accidental deletion of critical resources
`ignore_changes = [attr]` — ignore out-of-band changes to specific attributes
`replace_triggered_by = [...]` — replace resource when another resource changes (TF 1.2+)

---

**[I] Q: When do you need depends_on?**

When a dependency is side-effectful and not expressed through resource references. Example: an IAM policy must be attached before an EC2 instance starts, but the EC2 block doesn't reference the policy attachment directly.

---

## Functions

**[I] Q: What does cidrsubnet("10.0.0.0/16", 8, 3) return?**

`"10.0.3.0/24"` — extends the prefix by 8 bits (making it /24), using netnum 3.

---

**[I] Q: What is the difference between merge() and concat()?**

`merge()` combines maps: `merge({a=1}, {b=2})` → `{a=1, b=2}`. Later maps override earlier on key conflicts.
`concat()` combines lists: `concat(["a"], ["b","c"])` → `["a","b","c"]`.

---

**[I] Q: What does try() do?**

`try(expr1, expr2, ...)` returns the first argument that evaluates without error. If `var.config.name` might not exist: `try(var.config.name, "default")`.

---

## Security

**[S] Q: What are the security properties of Terraform state?**

- Plaintext JSON (NOT encrypted by default)
- Contains ALL resource attributes including "sensitive" ones
- Must be encrypted at rest (S3 SSE + KMS)
- Access must be restricted via IAM (least privilege)
- Never commit to git
- Enable S3 versioning for recovery

---

**[S] Q: What is OIDC dynamic credentials and why is it preferred over static AWS keys?**

OIDC lets GitHub Actions (or TFC) request a JWT from an OIDC provider. AWS validates the JWT via an IAM OIDC trust relationship and returns temporary credentials (15-60 min TTL). No static secrets stored anywhere. Rotation is automatic. Breach impact is minimal — credentials expire automatically.

---

## CI/CD

**[I] Q: Why is terraform plan -out=tfplan important in CI/CD?**

Saves the plan binary. `apply tfplan` applies exactly the reviewed plan without re-planning. Without `-out`, a new plan runs at apply time — which may differ from the reviewed plan if infrastructure changed between plan and apply.

---

**[S] Q: What does Atlantis do and how does it work?**

Atlantis is a self-hosted Terraform PR automation tool. It runs `terraform plan` when a PR is opened and posts the output as a comment. `atlantis apply` triggers `terraform apply` after the PR is approved. The apply is tracked to the PR for audit purposes.

---

## Providers

**[I] Q: What is .terraform.lock.hcl and should it be committed to git?**

Records exact provider versions and checksums. YES — commit to git. Ensures all team members and CI/CD use the same provider binary. Use `terraform init -upgrade` to refresh it.

---

**[I] Q: How do you use two different AWS regions in one Terraform config?**

Use provider aliases:
```hcl
provider "aws" { region = "us-east-1" }
provider "aws" { alias = "eu"; region = "eu-west-1" }
resource "aws_s3_bucket" "eu" { provider = aws.eu }
```

---

**[S] Q: What is the purpose of external_id in assume_role?**

An additional trust condition. The IAM role trust policy requires the caller to provide the exact `external_id` value. Protects against confused deputy attacks where a third-party service assumes your role on behalf of a different customer.

---

## Testing

**[I] Q: What is the difference between terraform validate and variable validation blocks?**

`terraform validate` checks HCL syntax and type compatibility (no API calls, runs on any config).
Variable validation blocks (`validation { condition ... }`) check that input values meet business rules before any resource is created.

---

**[S] Q: What is the difference between preconditions and check blocks in TF 1.5+?**

Precondition: evaluated before resource creation — if it fails, plan/apply errors and stops.
Check block: evaluated as an ongoing assertion — if it fails, a WARNING is posted but apply continues.

---

**[S] Q: How do you test a Terraform module without creating real infrastructure?**

Use `terraform test` with `command = plan` (TF 1.6+) — runs plan only, asserts on planned values.
Or use mock providers (TF 1.7+) — applies against a mock provider, no real API calls.
Or `terratest` with mocked modules.
