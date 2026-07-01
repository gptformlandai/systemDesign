# 16. Performance: Large Terraform, Parallelism, Targeting

## Why Terraform Slows Down At Scale

```text
Root cause 1: terraform plan refreshes ALL resources by default
  → Makes API calls for every resource in state
  → 500 resources × 1 API call each = 500 API calls
  → Throttled by AWS service quotas

Root cause 2: Provider plugin startup overhead
  → Each provider binary starts once per run; not a major issue

Root cause 3: Large state file parsing
  → State file with 10,000 resources takes longer to parse and diff
  
Root cause 4: Sequential applies for resources with dependencies
  → Resources in a dependency chain cannot be parallelized
```

---

## -parallelism Flag

Controls how many resources Terraform creates/modifies/destroys concurrently.

```bash
# Default parallelism: 10 concurrent operations
terraform apply

# Increase for faster applies (watch for API rate limits)
terraform apply -parallelism=20

# Decrease to avoid throttling on rate-limited APIs
terraform apply -parallelism=5

# Use in plan too
terraform plan -parallelism=20 -out=tfplan
```

```text
Increasing parallelism:
  Faster applies when most resources are independent.
  Risk: hitting AWS API rate limits (TooManyRequestsException).
  Test in staging before using high parallelism in prod.

Decreasing parallelism:
  Use when seeing API throttling errors during apply.
  Slower but more reliable.
```

---

## -refresh=false (Skip State Refresh)

```bash
# Default: Terraform refreshes state (calls APIs) before planning
terraform plan              # 500 API calls + diff

# Skip refresh: use cached state only (FAST but risks drift)
terraform plan -refresh=false   # 0 API calls + diff

# When safe to use:
#   - You know nothing changed out of band
#   - CI/CD pipeline is the only way to change infrastructure
#   - You've recently run a refresh-only plan

# When NOT safe:
#   - Manual changes may have been made
#   - Auto-scaling or other services modify resources automatically
```

---

## -target: Partial Applies

```bash
# Apply only specific resources
terraform apply -target=aws_instance.web

# Apply a module
terraform apply -target=module.eks

# Apply multiple targets
terraform apply \
  -target=module.vpc \
  -target=aws_security_group.alb

# Use case: apply a new resource that blocks the rest of the plan
terraform apply -target=aws_iam_role.new_role   # create just the role first
terraform apply                                   # then apply everything else
```

```text
RISKS of -target (use sparingly):

1. State inconsistency:
   If you apply resource A with -target, then plan without -target,
   Terraform might not know that A depends on B being updated.

2. Drift accumulation:
   Repeated targeted applies without a full apply create invisible gaps.
   
3. Module boundary issues:
   Applying -target=module.X doesn't guarantee all module-X resources are applied.

Best practice: NEVER use -target as a long-term workflow.
Use it only to unblock a stuck situation, then run a full apply ASAP.
```

---

## State Splitting

For very large codebases (1000+ resources), split your Terraform state by layer or service.

```text
Monolithic state (anti-pattern for large teams):
  one root module → one state file → 2000 resources
  
  Problems:
    - Every plan refreshes 2000 resources (slow)
    - Team A change blocks Team B apply (state lock contention)
    - One misconfiguration can destroy everything

Split by layer (recommended):
  network/       → state: platform/network/prod.tfstate     (~50 resources)
  eks/           → state: platform/eks/prod.tfstate         (~150 resources)
  database/      → state: services/database/prod.tfstate    (~30 resources)
  order-api/     → state: services/order-api/prod.tfstate   (~100 resources)
  
  Benefits:
    - Fast plans (each layer is small)
    - Teams can apply concurrently (different state locks)
    - Blast radius of each apply is limited
    - terraform_remote_state connects the layers

Split criteria:
  By lifecycle:  change frequency (network changes rarely; apps change daily)
  By team:       network team, platform team, product teams
  By service:    each microservice has its own state
```

---

## terraform providers lock (CI/CD Optimization)

```bash
# Pre-cache provider checksums for all target platforms
# Run this when updating providers, commit the lock file
terraform providers lock \
  -platform=linux_amd64 \
  -platform=linux_arm64 \
  -platform=darwin_amd64 \
  -platform=darwin_arm64 \
  -platform=windows_amd64
```

```text
Problem:
  .terraform.lock.hcl checksums are platform-specific.
  If developer on Mac generates lock file, CI on Linux may fail:
  "This required provider is not locked."

Solution:
  Run terraform providers lock with all platforms before committing.
  CI no longer needs to download providers — uses cache.
```

---

## Plan Performance Tuning

```bash
# Fast plan in CI (use when you trust your state is accurate):
terraform plan \
  -refresh=false \
  -parallelism=30 \
  -compact-warnings \
  -out=tfplan

# Careful plan for production (full refresh):
terraform plan \
  -refresh=true \
  -parallelism=10 \
  -out=tfplan
```

---

## Large Module Strategy

```text
Structure large codebases to keep each root module small:

platform/
  network/           ← 50 resources, rarely changes
    versions.tf
    main.tf
    backend-config.hcl
  eks/               ← 150 resources, changes monthly
    versions.tf
    main.tf
  monitoring/        ← 40 resources, changes occasionally

services/
  order-api/         ← 80 resources, changes daily
    dev/
      versions.tf
      main.tf
    prod/
      versions.tf
      main.tf

Benefit: `terraform plan` in services/order-api/prod only reads 80 resources.
Not all 1000+ in the organization.
```

---

## Avoiding Unnecessary Replacements

```text
Replacements destroy the old resource and create a new one.
They are often preventable:

1. Tags changes:
   Use ignore_changes = [tags] if tags are managed out-of-band.
   OR use default_tags in provider block to apply common tags.

2. User data changes on EC2:
   Use ignore_changes = [user_data] if instance already bootstrapped.

3. EKS addon version pinning:
   Specify addon_version explicitly; AWS updates it automatically otherwise.

4. Auto-generated IDs:
   Use name_prefix instead of name to let AWS generate unique names.
   Avoids conflicts on create_before_destroy scenarios.
```

---

## Interview Sound Bite

Terraform performance at scale: default `-parallelism=10` controls concurrent operations — increase for speed, decrease when hitting API rate limits. `-refresh=false` skips the state refresh (API calls) for fast plans in CI when drift is unlikely. `-target` is a last resort for unblocking stuck applies — never use it as a normal workflow because it creates state inconsistency. The most effective optimization for large teams is state splitting: divide by layer (network, compute, database) or by service so each root module has 50-200 resources. This reduces plan time, limits blast radius, and allows teams to apply concurrently. Use `terraform providers lock -platform=linux_amd64,darwin_amd64` to pre-cache checksums for all platforms.
