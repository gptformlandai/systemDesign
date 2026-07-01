# 33. OpenTofu, BSL Licensing, and the Terraform Ecosystem

## The HashiCorp Licensing Change (August 2023)

```text
Before Aug 2023:
  Terraform was licensed under MPL-2.0 (Mozilla Public License 2.0)
  → Open source, permissive: anyone could use Terraform commercially,
    build products on it, redistribute it.

After Aug 10, 2023:
  HashiCorp changed Terraform 1.6+ to BSL 1.1 (Business Source License)
  → Still source-available, but with restrictions:
    ✗ Cannot use Terraform to build a COMPETING PRODUCT to HashiCorp's offerings
      (Terraform Cloud / HCP Terraform)
    ✓ Using Terraform to manage your own infrastructure: still fully allowed
    ✓ Internal tooling: still allowed
    ✗ Building a third-party SaaS that wraps Terraform CLI: restricted
  
  Last MPL-2.0 version: Terraform 1.5.7 (August 2023)
  First BSL version: Terraform 1.6.0 (October 2023)
```

---

## OpenTofu: The Open-Source Fork

```text
Created:   September 2023
Forked at: Terraform 1.5.x (last MPL-2.0 version)
License:   MPL-2.0 (stays open source)
Governance: Linux Foundation (donated to OpenTofu project)
CNCF:      Projects page — listed as CNCF sandbox project
Lead:      Spacelift, Env0, Scalr, Gruntwork, and others
```

### OpenTofu vs Terraform Compatibility

```text
OpenTofu goals:
  - Drop-in replacement for Terraform
  - Fully compatible HCL syntax
  - Same CLI commands (plan/apply/destroy/etc.)
  - Compatible providers and modules (uses the same Terraform Registry)

Differences (as of OpenTofu 1.8+):
  - State file: compatible with Terraform state (can migrate)
  - Native state encryption (built-in, no third-party tooling required)
  - Provider-defined functions (TF 1.8 / OTF 1.7+)
  - for_each enhancements: supports modules with for_each of modules
  - Testing framework enhancements (mock providers ahead of Terraform)
  - Removed features that depend on HCP Terraform proprietary APIs
```

### Migration From Terraform to OpenTofu

```bash
# Step 1: Install OpenTofu
brew install opentofu
# OR: curl -OL https://github.com/opentofu/opentofu/releases/latest

# Step 2: Verify it's a drop-in replacement
tofu version  # instead of: terraform version

# Step 3: Run against existing state (no state migration needed for basic cases)
cd your-terraform-project
tofu init      # reads existing .terraform.lock.hcl
tofu plan
tofu apply

# Step 4: State is fully compatible
# If you were on Terraform 1.5.x, OpenTofu reads the same state file format
# No explicit migration command needed
```

### When To Migrate

```text
Migrate to OpenTofu if:
  → Your company's legal team is concerned about BSL restrictions
  → You build tooling that wraps Terraform CLI for commercial use
  → You want state encryption without Vault/KMS dependencies
  → You want active community governance (Linux Foundation vs single vendor)

Stay on HashiCorp Terraform if:
  → You use Terraform Cloud / HCP Terraform (built for Terraform, not OpenTofu)
  → Your team relies on Terraform-specific TFC features (Sentinel, private registry)
  → License change doesn't affect your use case (just managing own infra)
```

---

## HCP Terraform Rebranding

```text
August 2023: HashiCorp renamed "Terraform Cloud" → "HCP Terraform"
  (HCP = HashiCorp Cloud Platform)

What changed:
  - Product name only
  - Same URLs initially, migrating to app.terraform.io stays working
  - Same features: remote state, VCS integration, private registry, Sentinel
  - "Terraform Enterprise" (self-hosted) still exists as a separate product
  
Interview note: "TFC" and "HCP Terraform" are used interchangeably in most interviews.
```

---

## CDKTF: Cloud Development Kit for Terraform

CDK for Terraform lets you write Terraform configurations using programming languages instead of HCL. The tool synthesizes HCL JSON that Terraform can consume.

```text
Languages supported: TypeScript, Python, Java, C#, Go
Maintained by: HashiCorp
How it works:
  You write code in your language → cdktf synth → generates .json files
  → terraform plan/apply those JSON files
  
Key use case: teams that prefer type safety, IDE completion, and testing
  libraries from a programming language over HCL's limited tooling.
```

### CDKTF Example (TypeScript)

```typescript
import { Construct } from "constructs";
import { App, TerraformStack, TerraformOutput } from "cdktf";
import { AwsProvider } from "@cdktf/provider-aws/lib/provider";
import { Instance } from "@cdktf/provider-aws/lib/instance";
import { Vpc } from "@cdktf/provider-aws/lib/vpc";

class MyStack extends TerraformStack {
  constructor(scope: Construct, id: string) {
    super(scope, id);

    new AwsProvider(this, "aws", {
      region: "us-east-1",
    });

    const vpc = new Vpc(this, "main-vpc", {
      cidrBlock: "10.0.0.0/16",
      tags: { Name: "main-vpc", Environment: "prod" },
    });

    const instance = new Instance(this, "web-server", {
      ami: "ami-0c55b159cbfafe1f0",
      instanceType: "t3.micro",
      vpcId: vpc.id,   // ← TypeScript type safety, no string interpolation
      tags: { Name: "web-server" },
    });

    new TerraformOutput(this, "instance-id", {
      value: instance.id,
    });
  }
}

const app = new App();
new MyStack(app, "my-stack");
app.synth();  // generates cdktf.out/my-stack/cdk.tf.json
```

### CDKTF CLI Commands

```bash
# Install
npm install -g cdktf-cli

# Initialize a new TypeScript project
cdktf init --template=typescript --providers=aws

# Synthesize (generates Terraform JSON)
cdktf synth       # outputs to cdktf.out/

# Plan
cdktf diff        # like terraform plan

# Apply
cdktf deploy

# Destroy
cdktf destroy
```

---

## Terraform Stacks (TFC Feature — 2024)

Terraform Stacks is a new orchestration layer in HCP Terraform for managing multiple Terraform configurations together as a single deployable unit.

```text
Problem it solves:
  Deploying 10+ Terraform configurations in the right dependency order
  (similar to Terragrunt run-all, but native to HCP Terraform)

Key concepts:
  .tfstack.hcl   — declare the stack: components + providers
  .tfdeploy.hcl  — deployment configuration per environment

Status: GA in HCP Terraform (not available in CLI-only OSS Terraform)
```

---

## Ecosystem Comparison

| Tool | Type | License | When To Use |
|---|---|---|---|
| Terraform | IaC CLI | BSL 1.1 | Standard Terraform workflow, HCP Terraform users |
| OpenTofu | IaC CLI | MPL-2.0 | License-sensitive orgs, pure open-source stack |
| CDKTF | Code→Terraform | MPL-2.0 | Language-first teams, TypeScript/Python preferred |
| Pulumi | IaC Engine | Apache-2.0 | Python/TS/Go natively, no HCL, different state model |
| AWS CDK | IaC Framework | Apache-2.0 | AWS-only, CloudFormation backend, TypeScript/Python |
| Terragrunt | Terraform wrapper | MIT | DRY multi-env Terraform, dependency graphs |

---

## Pulumi vs CDKTF

```text
Pulumi:
  - Native TypeScript/Python/Go/Java/C# → no HCL at all
  - Own state backend (Pulumi Cloud, or AWS S3)
  - Own provider model (bridges to Terraform providers via bridging)
  - Unit testing with Jest/pytest
  - More mature ecosystem than CDKTF

CDKTF:
  - TypeScript/Python/Java/C#/Go → compiles to Terraform JSON
  - Uses native Terraform providers (full parity)
  - Uses Terraform state backend (same S3 + DynamoDB)
  - Runs on top of Terraform (drop in replacement, familiar commands)
  - Good choice if you want language flexibility but keep Terraform ecosystem
```

---

## Interview Sound Bite

In August 2023, HashiCorp changed Terraform from MPL-2.0 to BSL 1.1, restricting commercial use cases that compete with HashiCorp's own products. OpenTofu (Linux Foundation, forked from Terraform 1.5.x) was created as the MPL-2.0 open-source successor — it's a drop-in replacement: same CLI commands, same provider ecosystem, same HCL syntax, compatible state format. CDKTF takes a different approach: you write TypeScript/Python that synthesizes Terraform JSON, keeping Terraform's provider/state model but replacing HCL with a typed programming language. For most organizations managing their own infrastructure (not reselling Terraform as a service), the BSL change has no practical impact.
