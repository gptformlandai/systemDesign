# AWS Architect 04: IaC, CI/CD, and Release Engineering Real-World Guide

> Goal: deploy AWS systems repeatably, safely, and audibly using infrastructure as code, CI/CD, deployment strategies, and rollback plans.

---

# Index

| Section | Focus |
|---|---|
| [0. Real Situation](#0-real-situation) | Real Situation |
| [1. Intuition](#1-intuition) | Intuition |
| [2. Tool Decision](#2-tool-decision) | Tool Decision |
| [3. Console Build: Create GitHub OIDC Provider In AWS](#3-console-build-create-github-oidc-provider-in-aws) | Console Build: Create GitHub OIDC Provider In AWS |
| [4. Console Build: Create Deployment Role](#4-console-build-create-deployment-role) | Console Build: Create Deployment Role |
| [5. GitHub Actions Example: ECS Deployment](#5-github-actions-example-ecs-deployment) | GitHub Actions Example: ECS Deployment |
| [6. Console Build: ECS Deployment Circuit Breaker](#6-console-build-ecs-deployment-circuit-breaker) | Console Build: ECS Deployment Circuit Breaker |
| [7. Console Build: CodeDeploy Blue-Green For ECS](#7-console-build-codedeploy-blue-green-for-ecs) | Console Build: CodeDeploy Blue-Green For ECS |
| [8. Console Build: CloudFormation Stack](#8-console-build-cloudformation-stack) | Console Build: CloudFormation Stack |
| [9. Terraform / CDK Production Pattern](#9-terraform-cdk-production-pattern) | Terraform / CDK Production Pattern |
| [10. Release Strategy Decision](#10-release-strategy-decision) | Release Strategy Decision |
| [11. Real Scenario: Bad Deployment](#11-real-scenario-bad-deployment) | Real Scenario: Bad Deployment |
| [12. Real Scenario: Schema Migration Without Downtime](#12-real-scenario-schema-migration-without-downtime) | Real Scenario: Schema Migration Without Downtime |
| [13. GenAI Release Engineering](#13-genai-release-engineering) | GenAI Release Engineering |
| [14. Production Checklist](#14-production-checklist) | Production Checklist |
| [15. Interview Question](#15-interview-question) | Interview Question |
| [16. Strong Answer](#16-strong-answer) | Strong Answer |
| [17. Revision Notes](#17-revision-notes) | Revision Notes |
| [18. Official Source Notes](#18-official-source-notes) | Official Source Notes |

---

## 0. Real Situation

Your team deploys manually:

```text
someone clicks in the console
someone edits security groups
someone updates ECS service by hand
someone changes Lambda environment variables
no one knows which change broke production
```

Architect answer:

```text
Console is for learning, investigation, and emergency visibility.
Production infrastructure should be created and changed through versioned IaC and CI/CD.
```

---

## 1. Intuition

Infrastructure as Code is Git for cloud architecture.

```text
Console click:
  fast but easy to forget

IaC:
  reviewed, versioned, repeatable, auditable

CI/CD:
  turns source changes into safe deployments

Release engineering:
  controls blast radius when new versions go live
```

---

## 2. Tool Decision

| Need | Strong Choice |
|---|---|
| AWS-native declarative templates | CloudFormation |
| AWS-native code abstraction | CDK |
| Multi-cloud / popular platform workflow | Terraform |
| Kubernetes resource deployment | Helm / Kustomize / GitOps |
| App deployment to ECS/EC2/Lambda | CodeDeploy, ECS deploy, GitHub Actions |
| CI automation | GitHub Actions, CodeBuild, Jenkins |

Architect rule:

```text
Pick one primary IaC path per platform.
Avoid mixing manual console changes with IaC-managed resources.
```

---

## 3. Console Build: Create GitHub OIDC Provider In AWS

### Real Situation

GitHub Actions needs to deploy to AWS.

Bad approach:

```text
Store AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY as long-lived GitHub secrets.
```

Better:

```text
Use OIDC federation so GitHub gets short-lived AWS credentials.
```

### Console Path

```text
AWS Console -> IAM -> Identity providers -> Add provider
```

Choose:

```text
Provider type: OpenID Connect
Provider URL: https://token.actions.githubusercontent.com
Audience: sts.amazonaws.com
```

### What This Click Changes

It allows AWS STS to trust identity tokens issued by GitHub Actions.

### Why It Matters

No static AWS keys in GitHub.

### What Can Go Wrong

If trust policy is too broad, any repo/branch may assume the role.

Strong trust condition:

```json
{
  "Condition": {
    "StringEquals": {
      "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
    },
    "StringLike": {
      "token.actions.githubusercontent.com:sub": "repo:my-org/my-repo:ref:refs/heads/main"
    }
  }
}
```

Impact:

```text
Only workflows from my-org/my-repo on main can assume this role.
```

---

## 4. Console Build: Create Deployment Role

### Console Path

```text
IAM -> Roles -> Create role -> Web identity -> Select GitHub OIDC provider
```

Attach permissions:

```text
ECR push/pull
ECS update service
CloudFormation deploy
S3 deploy bucket access
CloudWatch logs read if needed
```

### What This Click Changes

It creates a role GitHub Actions can assume during deployment.

### Why It Matters

Your CI/CD system gets temporary, scoped permissions.

### What Can Go Wrong

Do not attach `AdministratorAccess` casually.

Better:

```text
separate roles:
  dev-deploy-role
  stage-deploy-role
  prod-deploy-role

prod role:
  only from protected branch/environment
  maybe requires manual approval in GitHub Environment
```

---

## 5. GitHub Actions Example: ECS Deployment

```yaml
name: deploy-ecs

on:
  push:
    branches: [main]

permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/github-prod-deploy-role
          aws-region: us-east-1

      - name: Login to ECR
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build and push image
        run: |
          IMAGE=123456789012.dkr.ecr.us-east-1.amazonaws.com/myapp:${{ github.sha }}
          docker build -t "$IMAGE" .
          docker push "$IMAGE"

      - name: Deploy service
        run: |
          aws ecs update-service \
            --cluster myapp-prod \
            --service backend \
            --force-new-deployment
```

What matters:

```text
id-token: write:
  allows GitHub to request OIDC token.

role-to-assume:
  AWS role with scoped deploy permission.

image tag = github.sha:
  immutable deploy artifact tied to Git commit.

environment: production:
  can enforce approval and environment secrets in GitHub.
```

---

## 6. Console Build: ECS Deployment Circuit Breaker

### Console Path

```text
ECS -> Clusters -> Select cluster -> Services -> Create/Update service
```

Under deployment options:

```text
Deployment type: Rolling update
Deployment circuit breaker: Enable
Rollback on failure: Enable
```

### What This Click Changes

ECS monitors whether new tasks become healthy.

If deployment fails:

```text
ECS can stop rollout and roll back to previous working task set.
```

### Why It Matters

Bad image or broken health checks do not keep rolling forward forever.

### What Can Go Wrong

Bad health check path causes false rollback.

Architect move:

```text
Use /actuator/health/readiness for readiness.
Give Spring Boot enough startPeriod.
Do not route traffic before app is ready.
```

---

## 7. Console Build: CodeDeploy Blue-Green For ECS

### Console Path

```text
AWS Console -> CodeDeploy -> Applications -> Create application
```

Choose:

```text
Compute platform: Amazon ECS
Deployment group: ECS service + ALB target groups
```

Configure:

```text
blue target group
green target group
listener rules
traffic shifting
rollback alarms
```

### What Each Click Changes

```text
Blue target group:
  current production tasks.

Green target group:
  new version tasks.

Traffic shifting:
  controls percentage of user traffic sent to new version.

Rollback alarms:
  define when deployment is automatically stopped/reversed.
```

### Why It Matters

You can test green before full cutover.

### What Can Go Wrong

Database migrations may not be backward compatible.

Architect move:

```text
Use expand/contract migrations.
Deploy DB-compatible code first.
Avoid destructive schema changes during rollout.
```

---

## 8. Console Build: CloudFormation Stack

### Console Path

```text
CloudFormation -> Stacks -> Create stack -> With new resources
```

Upload:

```text
template file
parameters
tags
capabilities acknowledgement for IAM
```

### What Each Click Changes

```text
Template:
  desired state of resources.

Parameters:
  environment-specific values.

Tags:
  ownership and cost tracking.

Capabilities:
  allows template to create/modify IAM resources.
```

### What Can Go Wrong

Manual edits create drift.

Console path:

```text
CloudFormation -> Stack -> Stack actions -> Detect drift
```

Architect move:

```text
If drift is intentional, update IaC.
If drift is accidental, revert through IaC.
```

---

## 9. Terraform / CDK Production Pattern

### Recommended Repo Structure

```text
infra/
  modules/
    vpc/
    ecs-service/
    rds/
    bedrock-rag/
  envs/
    dev/
    stage/
    prod/
app/
  src/
  Dockerfile
.github/workflows/
  plan.yml
  deploy-dev.yml
  deploy-prod.yml
```

### Promotion Model

```text
dev:
  auto deploy from feature/main branch

stage:
  deploy release candidate

prod:
  manual approval
  immutable artifact
  change ticket if required
  rollback plan
```

### Terraform Commands

```bash
terraform fmt
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
```

### CDK Commands

```bash
cdk diff
cdk synth
cdk deploy
```

Architect rule:

```text
Plan/diff must be reviewed before production apply.
```

---

## 10. Release Strategy Decision

| Strategy | How It Works | Use When |
|---|---|---|
| Rolling | Replace tasks gradually | normal low-risk changes |
| Blue-green | run old and new environments, shift traffic | safer backend releases |
| Canary | send small percentage first | high-risk user-facing changes |
| Feature flag | deploy code disabled, turn on gradually | business behavior changes |
| Shadow traffic | send copied traffic to new version without user impact | validation of new system |

---

## 11. Real Scenario: Bad Deployment

### Situation

New ECS deployment causes 20 percent 500 errors.

### Immediate Checks

```text
CloudWatch -> Metrics -> ALB 5xx, Target 5xx, TargetResponseTime
ECS -> Cluster -> Service -> Deployments
ECS -> Tasks -> Stopped reason / health
CloudWatch Logs -> app logs by task ID
CodeDeploy -> Deployment status if blue-green
```

### Rollback Paths

Rolling ECS:

```text
ECS -> Service -> Update service -> choose previous task definition revision
```

CLI:

```bash
aws ecs update-service \
  --cluster myapp-prod \
  --service backend \
  --task-definition myapp-backend:42
```

Blue-green:

```text
CodeDeploy -> Deployments -> Stop deployment -> Roll back
```

### What The Click Changes

```text
Previous task definition:
  tells ECS to launch containers from older known-good image/config.

Stop and roll back:
  shifts traffic back to blue target group.
```

---

## 12. Real Scenario: Schema Migration Without Downtime

Bad:

```text
Deploy code that expects new column.
Then add new column.
Old code breaks or new code breaks.
```

Better expand/contract:

```text
1. Add nullable new column.
2. Deploy code that writes both old and new fields.
3. Backfill data.
4. Switch reads to new field.
5. Stop writing old field.
6. Drop old column in later release.
```

Why:

```text
During rolling or blue-green deploy, old and new code may run simultaneously.
```

---

## 13. GenAI Release Engineering

GenAI changes are not only code changes.

They include:

```text
prompt changes
model changes
temperature/max token changes
embedding model changes
chunking strategy changes
retrieval topK changes
guardrail changes
agent action changes
knowledge base data refresh
```

Production pattern:

```text
prompt version in Git or Bedrock Prompt Management
eval dataset for regression
canary small traffic to new prompt/model
track quality, latency, token cost, refusals, grounding failures
rollback to previous prompt/model/version
```

Console path:

```text
Bedrock -> Prompt management -> Create prompt -> Create version
Bedrock -> Guardrails -> Create version
Bedrock -> Flows -> Publish version -> Create alias
```

What this changes:

```text
Version:
  immutable snapshot you can deploy or roll back to.

Alias:
  stable application reference pointing to a chosen version.
```

---

## 14. Production Checklist

- all infra is in IaC
- production changes go through PR review
- CI runs tests/security scans
- AWS access from CI uses OIDC, not static keys
- deployment artifact is immutable
- image tags use commit SHA or release version
- prod deploy requires approval
- deployment strategy is documented
- rollback command is known and tested
- database migrations are backward compatible
- CloudWatch alarms can stop/rollback deploys
- drift detection is run periodically
- prompt/model/guardrail changes are versioned
- GenAI evals run before release

---

## 15. Interview Question

> How would you design a production deployment pipeline for an ECS service on AWS?

---

## 16. Strong Answer

I would store infrastructure in IaC, build the application in CI, create an immutable Docker image tagged with the Git SHA, push it to ECR, and deploy to ECS through a pipeline using GitHub OIDC or an AWS-native CI system. The deploy role would be least privilege and environment-specific.

For production, I would require approval, run smoke tests, use ECS deployment circuit breaker or CodeDeploy blue-green/canary depending on risk, and monitor CloudWatch alarms for 5xx, latency, and task health. Rollback would use the previous task definition or CodeDeploy rollback. Database migrations would follow expand/contract to remain compatible during rolling deployments.

For GenAI features, I would version prompts, guardrails, knowledge base data, and model choices, then run evals before rollout and canary traffic after rollout.

---

## 17. Revision Notes

- One-line summary: production AWS changes should be reviewed, versioned, deployed, monitored, and reversible.
- Three keywords: IaC, OIDC, rollback.
- One interview trap: storing long-lived AWS keys in CI.
- Memory trick: "Click to learn, code to operate."

---

## 18. Official Source Notes

- AWS Well-Architected recommends operating workloads using reliable, secure, cost-aware, measurable practices: <https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html>
- Bedrock Prompt Management supports reusable prompts, variables, variants, testing, and versions: <https://docs.aws.amazon.com/bedrock/latest/userguide/prompt-management.html>
- Bedrock Flows support versioned workflows and aliases for application deployment: <https://docs.aws.amazon.com/bedrock/latest/userguide/flows.html>

