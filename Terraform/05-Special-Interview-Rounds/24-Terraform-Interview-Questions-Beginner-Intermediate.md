# 24. Terraform Interview Questions: Beginner to Intermediate

---

## Foundations (Q1–Q10)

**Q1: What is Terraform and how is it different from Ansible?**

Terraform is a declarative IaC tool that manages infrastructure lifecycle (create/update/destroy). Ansible is a procedural automation tool primarily for configuration management. Terraform manages state and can compute diffs. Ansible is agentless and great for software installation but lacks native state management for infrastructure.

---

**Q2: What is the Terraform state file and why is it important?**

The state file (`terraform.tfstate`) is Terraform's mapping from HCL resource declarations to real infrastructure IDs. Without it, Terraform cannot compute diffs, track existing resources, or handle dependencies. State also stores computed attributes like IPs and ARNs that are "known after apply."

---

**Q3: Explain the terraform init, plan, apply workflow.**

`init` downloads providers and initializes the backend. `plan` computes the diff between HCL and current state (no API changes). `apply` executes the plan by calling cloud APIs. In CI/CD: `plan -out=tfplan` saves the plan, `apply tfplan` applies exactly what was reviewed.

---

**Q4: What is a Terraform provider?**

A provider is a plugin that translates HCL resource declarations into API calls for a specific cloud or service. `hashicorp/aws` translates HCL into AWS API calls. Providers are declared in `required_providers`, downloaded by `terraform init`, and locked in `.terraform.lock.hcl`.

---

**Q5: What is a Terraform module?**

A module is a directory with `main.tf`, `variables.tf`, and `outputs.tf`. It's the unit of reuse. Every Terraform config is a root module. Root modules call child modules via `module {}` blocks, passing inputs and consuming outputs.

---

**Q6: What is HCL and what are the main block types?**

HCL (HashiCorp Configuration Language) is Terraform's declarative config language. Main blocks: `resource` (creates infrastructure), `data` (reads existing infrastructure), `variable` (input parameters), `output` (exposed values), `locals` (computed intermediates), `module` (calls another module), `terraform` (version/backend config).

---

**Q7: What is the difference between count and for_each?**

`count` creates N copies identified by integer index. `for_each` creates one resource per map key or set element, identified by that key. `for_each` is better for named resources: removing an element only destroys that key, while removing a `count` element shifts all higher indices causing unnecessary updates.

---

**Q8: What does sensitive = true do on a variable or output?**

It hides the value from `terraform plan`, `terraform apply`, and `terraform output` CLI output. It does NOT protect the value in the state file — sensitive values are still stored in plaintext in state. State must be encrypted and access-controlled.

---

**Q9: How do you pass variable values to Terraform?**

Five methods (highest to lowest precedence): `-var` flag, `-var-file` flag, `*.auto.tfvars` files, `terraform.tfvars`, `TF_VAR_<name>` environment variables.

---

**Q10: What is the .terraform.lock.hcl file?**

The lock file records exact provider versions and checksums after `terraform init`. It should be committed to git to ensure all team members and CI/CD use exactly the same provider binary. To upgrade: `terraform init -upgrade`.

---

## Variables and Expressions (Q11–Q18)

**Q11: What is the difference between a variable and a local?**

Variables are inputs to a module — they can be set by the caller. Locals are computed values internal to the module — they cannot be overridden from outside. Locals are the right place for intermediate expressions that would otherwise be repeated.

---

**Q12: How does Terraform handle dependencies between resources?**

Terraform builds an implicit dependency graph from references: if resource B references `aws_vpc.main.id`, Terraform knows B depends on A and creates A first. Use `depends_on` only when the dependency is side-effectful and not expressed through references.

---

**Q13: What is the lifecycle block and what does create_before_destroy do?**

The `lifecycle` block modifies Terraform's behavior for a resource. `create_before_destroy = true` creates the new resource before destroying the old one during a replacement operation — enabling zero-downtime updates. `prevent_destroy = true` blocks accidental deletion. `ignore_changes` prevents Terraform from treating out-of-band changes as drift.

---

**Q14: How do you create a conditional resource in Terraform?**

Use `count = condition ? 1 : 0`. If count is 0, the resource is not created. To reference a conditional resource elsewhere: `length(aws_eip.nat) > 0 ? aws_eip.nat[0].public_ip : null`.

---

**Q15: What is a dynamic block?**

A `dynamic` block generates repeated nested blocks inside a resource from a list or map variable. Common use: generating multiple `ingress` rules in a security group from a list of ports. The `content {}` block inside uses `<iterator>.value` to access each element.

---

**Q16: What functions would you use to generate subnet CIDRs from a VPC CIDR?**

`cidrsubnet(base, newbits, netnum)` — e.g., `cidrsubnet("10.0.0.0/16", 8, 1)` returns `10.0.1.0/24`. Combine with a `for` expression: `[for i, az in local.azs : cidrsubnet(local.vpc_cidr, 8, i)]`.

---

**Q17: What is the difference between a data source and a resource?**

A resource declares infrastructure that Terraform owns and manages (create/update/destroy lifecycle). A data source reads existing infrastructure that Terraform does not own — it is read-only and never modifies the real resource.

---

**Q18: How do you handle secrets in Terraform?**

Read secrets from AWS SSM Parameter Store or Secrets Manager via data sources. Use `TF_VAR_` environment variables for CI/CD (not stored in HCL). Use OIDC dynamic credentials so no static AWS credentials are stored. Encrypt state with KMS. Never commit secrets in tfvars or HCL.

---

## State Management (Q19–Q25)

**Q19: What is a remote backend and why use one?**

A remote backend stores state in a shared, versioned, encrypted location (S3, GCS, TFC). Required for teams: allows concurrent work with state locking, provides backup/recovery via versioning, keeps secrets out of the local filesystem.

---

**Q20: How does state locking work with S3 + DynamoDB?**

When `terraform apply` starts, it writes a record to the DynamoDB table with the LockID as the hash key. DynamoDB conditional writes ensure only one process can hold the lock. When apply finishes, the record is deleted. If another apply tries to lock simultaneously, the conditional write fails and Terraform errors with "state is locked."

---

**Q21: What is the purpose of terraform state mv?**

`terraform state mv` renames a resource's address in state without destroying the real resource. Example: renaming `aws_instance.web` to `aws_instance.api`. The preferred modern approach is `moved {}` blocks, which are declarative, committed to VCS, and applied automatically.

---

**Q22: When would you use terraform state rm?**

When you want Terraform to "forget" a resource without destroying it — for example, if you're moving ownership of a resource to a different Terraform config, or if you want to stop managing a resource with Terraform while keeping it running.

---

**Q23: What is terraform import?**

`terraform import` brings an existing real resource under Terraform management by creating a state entry. You must already have the resource block in your HCL config. After importing, run `plan` to see if HCL matches reality and adjust until `No changes`.

---

**Q24: What are moved blocks and when do you use them?**

`moved {}` blocks tell Terraform that a resource was renamed or moved to a module — update its state address without destroying it. Used when refactoring: extracting resources into modules, renaming resources, migrating from `count` to `for_each`. They show up in plan output as "has moved to" (not create/destroy).

---

**Q25: What is state drift and how do you detect it?**

State drift is when real infrastructure diverges from Terraform state due to manual changes or auto-scaling. Detect with `terraform plan -refresh-only` (reads actual API state, shows diff). Resolve by accepting the drift (`apply -refresh-only`) or reverting it (`apply`).

---

## CI/CD and Collaboration (Q26–Q30)

**Q26: How does Terraform work in a CI/CD pipeline?**

PR → `terraform plan -out=tfplan` → post plan as PR comment → human review → merge → `terraform apply tfplan`. The key: save the plan with `-out` so the exact reviewed plan is applied. Use OIDC credentials (no stored secrets). Run `terraform validate` and `tflint` before plan.

---

**Q27: What is Atlantis?**

Atlantis is an open-source Terraform automation tool. It integrates with GitHub/GitLab/Bitbucket and responds to PR comments (`atlantis plan`, `atlantis apply`). It runs plan on PR open and posts output as a comment. Apply requires PR approval and is triggered by a comment.

---

**Q28: How do you manage Terraform state in a team?**

Remote backend (S3 or TFC) for shared state, DynamoDB for locking, S3 versioning for recovery, IAM least-privilege on state bucket, encryption at rest. All applies through CI/CD (not from developer laptops) to ensure consistent versions and audit trail.

---

**Q29: What version constraint operators does Terraform support?**

`= 1.5.0` (exact), `>= 1.5.0` (minimum), `~> 5.0` (pessimistic: `>= 5.0, < 6.0`), `~> 5.1.0` (patch-only: `>= 5.1.0, < 5.2.0`), `!= 1.5.3` (exclude). The `~>` operator is most common: `~> 5.0` for major pinning, `~> 5.1.0` for minor pinning.

---

**Q30: What is the Terraform Registry?**

`registry.terraform.io` — the public catalog for providers and modules. Providers are organized by tier: official (`hashicorp/*`), partner (vendor-maintained), and community. Modules can be published with semantic versions and called via `source = "terraform-aws-modules/vpc/aws"` with `version = "~> 5.0"`.
