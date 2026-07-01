# 25. Terraform Interview Questions: Senior / MAANG Level

---

**Q1: How would you design a multi-account AWS Terraform architecture for a 50-engineer organization?**

**Answer:**

Hub-and-spoke model: a dedicated "tooling" account runs Terraform (CI/CD runners, state bucket). Each team gets separate AWS accounts per environment (dev, staging, prod) — 50+ accounts in a large org.

Terraform structure:
- Separate root modules per account/environment
- Cross-account provider using `assume_role { role_arn }` for each target account
- Central S3 state bucket in tooling account; DynamoDB for locking
- Module registry (TFC private or GitHub) for shared modules
- OIDC credentials: GitHub Actions → OIDC → STS → temporary credentials per account

Key policies:
- TerraformDeployRole in each account trusts the tooling account
- OIDC trust policy scoped to specific GitHub org/repo/branch
- No static credentials anywhere

State organization: `<layer>/<service>/<account>/<environment>/terraform.tfstate`

---

**Q2: Walk through how you would import 200 existing EC2 instances into Terraform without downtime.**

**Answer:**

1. Write a script to generate import blocks for all 200 instances using `aws ec2 describe-instances` output:
   ```hcl
   import { to = aws_instance.web["i-0abc123"]; id = "i-0abc123" }
   ```

2. Use `terraform plan -generate-config-out=generated.tf` to auto-generate resource blocks from actual state.

3. Review and clean up `generated.tf` — remove computed attributes that Terraform will set, add to modules as appropriate.

4. Run `terraform apply` to execute the import. No downtime because import only updates Terraform state; no API calls that modify resources.

5. Run `terraform plan` afterward — should show "No changes" if HCL matches reality. Adjust HCL for any drift.

6. Delete import blocks from HCL (one-time use).

Phased approach for 200: import in batches of 20, validate each batch before continuing.

---

**Q3: Explain the blast radius problem and how you mitigate it in Terraform.**

**Answer:**

Blast radius = the maximum damage a single `terraform apply` can cause. A 1000-resource root module can destroy everything with one `terraform destroy` or one misconfiguration.

Mitigations:
- **State splitting**: Separate root modules per layer (network, compute, database) or per service. Each state is 50-200 resources.
- **`prevent_destroy = true`** on stateful resources (RDS, S3 with production data)
- **CI/CD gates**: Require human approval before apply in production
- **Policy-as-code**: Sentinel/OPA policies that block destroys of tagged "critical" resources
- **Separate AWS accounts per environment**: A bug in dev can never touch prod state
- **-target** as an emergency tool: only the targeted resource can be affected

In an org with dozens of teams, each team owns its own Terraform state. A bug in the orders team's Terraform cannot touch the payments team's infrastructure.

---

**Q4: How does IRSA (IAM Roles for Service Accounts) work, and how do you provision it with Terraform?**

**Answer:**

IRSA lets a Kubernetes service account assume an AWS IAM role without static credentials. Flow:
1. EKS cluster has an OIDC identity provider
2. IAM role trust policy trusts the EKS OIDC issuer for a specific service account
3. Pod's service account annotation references the IAM role ARN
4. OIDC token projected into the pod; eks-pod-identity-agent exchanges it for temporary AWS credentials

Terraform provisioning:
```hcl
# OIDC provider (done once per cluster)
resource "aws_iam_openid_connect_provider" "eks" {
  url            = module.eks.cluster_oidc_issuer_url
  client_id_list = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks.certificates[0].sha1_fingerprint]
}

# IAM role for a specific service account
resource "aws_iam_role" "app" {
  name = "app-service-account-role"
  assume_role_policy = jsonencode({
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.eks.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:default:my-app"
        }
      }
    }]
    Version = "2012-10-17"
  })
}
```

---

**Q5: What strategies do you use to prevent secrets from appearing in Terraform state?**

**Answer:**

The honest answer: you can't fully prevent secrets from appearing in state. But you can minimize it:

1. **Generate secrets outside Terraform**: Use AWS Secrets Manager rotation, not `aws_db_instance.password`. Terraform doesn't need to know the password — it's set once and rotated externally. Use `ignore_changes = [password]` so Terraform doesn't drift on password changes.

2. **Use data sources instead of variables for secrets**: Pull from SSM/Secrets Manager at apply time — the secret is still in state but wasn't in tfvars or HCL.

3. **For generated secrets (TLS keys)**: `tls_private_key` resource puts private key in state. Better: generate the key externally, store in Secrets Manager, import the public key only.

4. **Encrypt state with KMS**: The state is encrypted at rest; reading it requires KMS decrypt permission.

5. **Restrict state access**: Only the Terraform runner IAM role can read the state S3 bucket. Developers work with outputs, not raw state.

6. **Use `sensitive = true`** to prevent leaking to logs; implement log scrubbing in CI/CD.

---

**Q6: How do you handle Terraform at scale when plans take 20+ minutes?**

**Answer:**

Root causes and solutions:

1. **State size**: 2000+ resources in one state. → **Split by layer/service**. Each state is 100-200 resources. Plan time drops to seconds.

2. **Refresh overhead**: 1000 API calls per plan. → Use `-refresh=false` for CI plans when state is trusted (only CI/CD modifies infra). Run full refresh plans on a schedule (drift detection).

3. **Provider throttling**: AWS API rate limits slow refreshes. → Reduce `-parallelism` to 5-10 to avoid throttling; or use provider-level retries.

4. **Module structure**: Huge root config means everything is in one plan. → Thin root, fat modules. Split CI/CD to plan only affected modules (path filters on GitHub Actions).

5. **Dependency chains**: Long sequential dependencies. → Review and parallelize where possible; avoid artificial `depends_on`.

6. **Remote runs on TFC**: Remote execution with persistent runner doesn't need to download providers each time.

---

**Q7: How do you implement GitOps with Terraform across 30+ microservices?**

**Answer:**

Repository structure:
- Infrastructure monorepo: one repo, one directory per service/environment
- OR infra repos per team with a shared modules repo

CI/CD architecture:
- Path-based triggers: only plan/apply the affected service when files in `services/order-api/` change
- GitHub Actions matrix job or separate workflow per service
- Atlantis: each project in `atlantis.yaml` scoped to one service directory

Governance:
- Required reviewers policy: infra changes require platform team approval
- OPA/Sentinel policies enforced before apply
- Drift detection: scheduled plan runs across all services

State organization:
```text
services/<service>/<environment>/terraform.tfstate
```

Each service team has a Terraform workspace they own. Platform team provides the VPC/EKS layer via `terraform_remote_state`.

---

**Q8: Explain how Terraform handles provider upgrades safely in production.**

**Answer:**

1. **Lock file**: `.terraform.lock.hcl` pins the exact version. Production uses the same version as staging.

2. **Test in lower environments first**: Upgrade provider version in dev → validate → staging → prod.

3. **Read provider CHANGELOG**: Major version upgrades often have breaking changes in resource schemas.

4. **`terraform init -upgrade`**: Updates lock file to latest matching constraint. Run in a branch, open PR.

5. **Pessimistic constraint**: `~> 5.0` accepts 5.x but not 6.x. Pin to minor for stability: `~> 5.1`.

6. **Validate with plan**: After upgrading, run `terraform plan` to see if the new provider changes the desired state unexpectedly.

7. **Multi-platform lock**: Run `terraform providers lock -platform=linux_amd64` for all CI/CD platforms before committing the lock file.

---

**Q9: What are the tradeoffs between Terraform Cloud workspaces and directory-based environments?**

**Answer:**

| Factor | TFC Workspaces | Directory-Based |
|---|---|---|
| Variable isolation | Per-workspace variable sets | Separate tfvars files |
| Account isolation | Via variable (role ARN) | Native (separate provider config) |
| Governance | Built-in RBAC, Sentinel | DIY (IAM, OPA) |
| Blast radius | Per-workspace state | Per-directory state |
| Cost | TFC subscription | Self-managed CI/CD |
| Visibility | TFC UI: run history, cost | Custom dashboards |

For large orgs: TFC workspaces + OIDC dynamic credentials per workspace is the cleanest. For startups/self-hosted: directory-based with GitHub Actions is cost-effective. The key is consistent pattern — mixing both creates confusion.

---

**Q10: How do you design a Terraform module for public consumption?**

**Answer:**

1. **Narrow scope**: One job. `terraform-aws-vpc` creates VPCs, not entire stacks.

2. **Complete `variables.tf`**: Every configurable value is a variable with `type`, `description`, `default` (for optional), validation blocks. No hardcoded values.

3. **Generous `outputs.tf`**: Expose every ID, ARN, name that a caller might need. Err on the side of more outputs.

4. **README with working example**: Usage block with all required variables, a module outputs table.

5. **examples/ directory**: `examples/basic/` and `examples/complete/`. These serve as documentation AND test fixtures.

6. **Semantic versioning with CHANGELOG**: Callers pin `version = "~> 2.0"`. Breaking changes bump major.

7. **No provider config in module**: Caller passes provider; module only declares `required_providers`.

8. **`terraform test` suite**: Unit tests for naming/tagging conventions, integration tests that apply examples and validate outputs.

9. **CI/CD for the module itself**: Auto-plan all examples on PR, run `tflint` and `checkov`, publish release on merge.
