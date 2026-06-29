# AWS Senior Architecture: IaC, CI/CD, and Release Engineering Gold Sheet

> Track: AWS Interview Track — Senior Architecture
> Goal: design production-grade deployment pipelines, implement IaC at scale, and explain blue-green and canary deployment patterns with rollback strategies.

---

## 0. How To Read This

Beginner focus:
- Infrastructure as Code basics (Terraform, CloudFormation)
- Deployment pipeline stages
- Blue-green vs rolling deployment

Intermediate focus:
- Terraform vs CDK vs CloudFormation decision
- GitHub Actions OIDC to AWS (no static keys)
- CodePipeline + CodeBuild + CodeDeploy
- Drift detection

Senior / MAANG focus:
- CDK Pipelines for self-mutating pipelines
- Canary deployments with traffic shifting
- GitOps with Argo CD on EKS
- Feature flags for safe releases
- Rollback strategies (immediate vs restore-from-snapshot)
- Multi-environment promotion pattern
- IaC module design and code reuse

---

# Topic 1: Infrastructure as Code

## 1. Terraform vs CDK vs CloudFormation

| Feature | Terraform | CDK | CloudFormation |
|---|---|---|---|
| Language | HCL | Python/TypeScript/Java/Go | YAML/JSON |
| State management | Terraform state file (S3 + DynamoDB lock) | CloudFormation stacks | CloudFormation |
| Multi-cloud | yes (providers for AWS, Azure, GCP) | AWS only | AWS only |
| Abstraction level | medium (resources) | high (constructs, L2/L3) | low (resources) |
| Ecosystem | Terraform Registry, modules | CDK construct hub | limited |
| Drift detection | `terraform plan` | `cdk diff` | CloudFormation |
| Learning curve | moderate | requires programming knowledge | low |
| When to choose | multi-cloud, existing Terraform team | AWS-only, strong typing | simplicity, deep CloudFormation expertise |

## 2. Terraform Best Practices

State management:

```hcl
terraform {
  backend "s3" {
    bucket         = "my-terraform-state"
    key            = "prod/payment-service/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}
```

DynamoDB table for state locking:

```text
Table: terraform-state-lock
Partition Key: LockID (String)
On-demand billing
No TTL (Terraform manages lock lifecycle)
```

Module structure:

```text
modules/
  vpc/                 # reusable VPC module
    main.tf
    variables.tf
    outputs.tf
  rds-aurora/          # reusable Aurora module
  ecs-service/         # reusable ECS service module

environments/
  prod/
    main.tf            # calls modules with prod vars
    terraform.tfvars
  staging/
    main.tf
    terraform.tfvars
```

## 3. AWS CDK

CDK compiles to CloudFormation. Use constructs:

```python
from aws_cdk import (
    aws_ecs as ecs,
    aws_ecs_patterns as ecs_patterns,
    aws_ecr as ecr
)

class PaymentServiceStack(Stack):
    def __init__(self, scope: Construct, id: str, **kwargs):
        super().__init__(scope, id, **kwargs)
        
        # L3 pattern construct — ALB + ECS Fargate service in one
        ecs_patterns.ApplicationLoadBalancedFargateService(
            self, "PaymentService",
            cluster=self.cluster,
            cpu=512,
            memory_limit_mib=1024,
            task_image_options=ecs_patterns.ApplicationLoadBalancedTaskImageOptions(
                image=ecs.ContainerImage.from_ecr_repository(
                    ecr.Repository.from_repository_name(self, "PaymentRepo", "payment-service")
                )
            ),
            public_load_balancer=False
        )
```

CDK Pipelines (self-mutating):

```python
pipeline = CodePipeline(self, "Pipeline",
    pipeline_name="payment-pipeline",
    synth=CodeBuildStep("Synth",
        input=CodePipelineSource.github("myorg/payment-service", "main"),
        commands=["npm ci", "npx cdk synth"]
    )
)

# Add deployment stages
pipeline.add_stage(PaymentServiceStage(self, "Prod",
    env=cdk.Environment(account="123456789012", region="us-east-1")
))
```

CDK Pipelines self-mutate: when you add a new stage to the pipeline code, the pipeline updates itself on the next run.

---

# Topic 2: CI/CD

## 1. GitHub Actions OIDC To AWS (No Static Keys)

The secure way to give GitHub Actions access to AWS:

```yaml
# GitHub Actions workflow
name: Deploy

on:
  push:
    branches: [main]

permissions:
  id-token: write   # required for OIDC
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/GitHubActionsDeployRole
          aws-region: us-east-1
          # NO ACCESS KEY OR SECRET KEY NEEDED
      
      - name: Deploy to ECS
        run: |
          aws ecs update-service \
            --cluster prod \
            --service payment-service \
            --force-new-deployment
```

IAM role trust policy for GitHub OIDC:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
      },
      "StringLike": {
        "token.actions.githubusercontent.com:sub": "repo:myorg/payment-service:ref:refs/heads/main"
      }
    }
  }]
}
```

Interview line:

```text
GitHub Actions OIDC lets GitHub exchange a short-lived OIDC token for temporary AWS
credentials. No static access keys in GitHub secrets. Keys cannot be leaked because
they don't exist. The trust policy scopes access to specific repos and branches.
```

## 2. Deployment Strategies

| Strategy | How It Works | Rollback | Risk | Use Case |
|---|---|---|---|---|
| In-place / Rolling | update instances one by one | manual, slow | medium | ECS rolling, K8s rolling update |
| Blue-Green | launch new env (green), switch traffic, keep old (blue) | instant (flip traffic back) | low | production critical services |
| Canary | shift small % to new version, monitor, gradually increase | instant (set % back to 0) | lowest | high-traffic services |
| Recreate | stop all old, start all new | redeploy old version | high (downtime) | dev/test |

## 3. Blue-Green Deployment With ALB

```text
Blue Environment (current):
  Target Group: blue-tg, registered to ALB listener 100%

Deploy green:
  1. Launch new ECS task with new image version
  2. Register to green-tg
  3. Run health checks until green is healthy
  4. ALB weighted rules: blue=90, green=10 (smoke test)
  5. Monitor: errors, latency on green
  6. If good: blue=0, green=100
  7. If bad: blue=100, green=0 (rollback, instant)
  8. Drain blue tasks and deregister after 15 minutes

CodeDeploy ECS blue-green does this automatically.
```

## 4. Canary Deployment With ALB Weighted Target Groups

```yaml
# CloudFormation - ALB listener rule with weighted forward
ListenerRule:
  Type: AWS::ElasticLoadBalancingV2::ListenerRule
  Properties:
    Actions:
      - Type: forward
        ForwardConfig:
          TargetGroups:
            - TargetGroupArn: !Ref StableTargetGroup
              Weight: 95
            - TargetGroupArn: !Ref CanaryTargetGroup
              Weight: 5
```

Canary progression:
- 5% → observe for 10 minutes
- 20% → observe for 10 minutes
- 50% → observe for 10 minutes
- 100% → stable release

Automatic rollback trigger:

```text
CloudWatch alarm: CanaryErrorRate > 1%
-> CodeDeploy rollback or Shift traffic back to 0% on canary
```

## 5. CodePipeline Architecture

```text
Source: GitHub (CodeStar connection) or CodeCommit
  -> CodeBuild: build, test, Docker build, push to ECR
  -> CodeBuild: run integration tests against staging
  -> Manual Approval: human approval gate for prod
  -> CodeDeploy: blue-green or canary deployment to ECS/EC2
```

## 6. Drift Detection

After manual changes bypass IaC, infrastructure drifts from intended state.

CloudFormation drift detection:

```bash
aws cloudformation detect-stack-drift --stack-name payment-stack
aws cloudformation describe-stack-resource-drifts \
  --stack-name payment-stack \
  --stack-resource-drift-status-filters MODIFIED DELETED
```

Terraform drift:

```bash
terraform plan  # shows diff between state and actual AWS resources
```

Best practice:
- all changes via pipeline, never manual console changes
- weekly drift detection scheduled with CloudWatch Events → Lambda → detect-stack-drift

## 7. Feature Flags For Safe Releases

Decouple deployment from release:

```text
Deploy code with new feature behind a flag (flag = OFF)
Feature is in production but not active
Gradually enable for users:
  1% → 5% → 25% → 100%
Rollback = flip flag OFF (instant, no redeployment)

Implementations:
  - AWS AppConfig: managed feature flags with deployment strategies
  - LaunchDarkly, Flagsmith: external flag services
  - DynamoDB: custom flag store (simple, no external dependency)
```

## 8. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Static AWS access keys in GitHub Secrets | GitHub Actions OIDC — no static keys |
| Terraform state in local file | S3 backend with DynamoDB locking |
| One environment, one state file | separate state files per environment |
| Deploy directly to prod without staging gate | always stage → prod with approval or auto-checks |
| Manual console changes to production | IaC only, enforce via SCP or IAM deny console changes |
| No rollback plan | blue-green with traffic flip = instant rollback |
| Feature on/off by deployment | feature flags: decouple release from deployment |

## 9. Interview Scenario

**Scenario**: "Walk me through how you'd implement a zero-downtime deployment for a critical ECS service."

Strong answer:

```text
I use blue-green deployment with CodeDeploy ECS.

1. CodePipeline triggers on Git push to main:
   - CodeBuild: builds Docker image, tags with git SHA, pushes to ECR
   - CodeBuild: runs unit tests and integration tests
   - Manual approval gate (or automatic based on test pass)
   - CodeDeploy: ECS blue-green deployment

2. CodeDeploy ECS blue-green:
   - Registers new task definition with new image
   - Starts new tasks in green target group
   - Waits for health checks to pass
   - Shifts 10% traffic to green for 5 minutes
   - CloudWatch alarm monitors: error rate, p99 latency
   - If alarms OK: shifts 100% traffic to green
   - If alarms fire: automatically rolls back to blue (instant traffic flip)
   - After 15 minutes with no rollback: drains and terminates blue tasks

3. Zero downtime: traffic only shifts after green is healthy.
   Rollback: sub-second, just flip the ALB target group weights.
```

## 10. Revision Notes

- Terraform: S3 + DynamoDB for state, modules for reuse, `terraform plan` for drift
- CDK: higher-level constructs, compiles to CloudFormation, CDK Pipelines for self-mutating
- GitHub Actions OIDC: no static keys; trust policy scopes to specific repo+branch
- Blue-green: two environments, flip traffic, instant rollback
- Canary: traffic shift in % increments; auto-rollback on alarm
- Feature flags: deploy → flag off; release → flag on; rollback = flag off (no redeploy)
- Drift detection: schedule weekly; enforce no-console-change policy

## 11. Official Source Notes

- GitHub Actions OIDC: <https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services>
- CodeDeploy ECS: <https://docs.aws.amazon.com/codedeploy/latest/userguide/deployment-steps-ecs.html>
- CDK Pipelines: <https://docs.aws.amazon.com/cdk/v2/guide/cdk_pipeline.html>
- Terraform AWS provider: <https://registry.terraform.io/providers/hashicorp/aws/latest/docs>
- AWS AppConfig: <https://docs.aws.amazon.com/appconfig/latest/userguide/what-is-appconfig.html>
