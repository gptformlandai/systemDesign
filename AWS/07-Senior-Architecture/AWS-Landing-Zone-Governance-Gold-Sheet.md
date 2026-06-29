# AWS Senior Architecture: Landing Zone and Governance Gold Sheet

> Track: AWS Interview Track — Senior Architecture
> Goal: design a multi-account AWS organization with proper governance, automation, and security guardrails.

---

## 0. How To Read This

Beginner focus:
- AWS Organizations concept
- Management account vs member accounts
- Basic OU structure

Intermediate focus:
- Service Control Policies (SCPs)
- Control Tower and guardrails
- Centralized CloudTrail and logging
- Account vending machine

Senior / MAANG focus:
- OU design for large enterprises
- SCP design (deny vs restrict patterns)
- Centralized Security Hub and GuardDuty
- AWS SSO / IAM Identity Center for cross-account access
- Automated account provisioning with Control Tower or custom Terraform
- FinOps governance (tagging policy, budget alerts at org level)
- Network account and transit gateway hub pattern

---

# Topic 1: AWS Organizations

## 1. Intuition

AWS Organizations groups AWS accounts under a single management structure.

Benefits:
- consolidated billing (one invoice)
- organization-wide policies via SCPs
- centralized security and audit
- account isolation (blast radius containment)

Single account for everything:

```text
Risk: one compromised credential -> blast radius is your ENTIRE infrastructure
Cost: mixed workloads hard to attribute costs to teams
Compliance: dev and prod data mix, hard to audit

Multi-account solves all three.
```

## 2. Organization Structure

```text
Root (Management Account)
├── Security OU
│   ├── Log Archive Account (all CloudTrail, Config logs)
│   └── Security Tooling Account (Security Hub, GuardDuty, Inspector)
├── Infrastructure OU
│   ├── Network Account (Transit Gateway, Direct Connect, shared VPCs)
│   └── Shared Services Account (ECR, Artifact, internal DNS)
├── Workloads OU
│   ├── Dev OU
│   │   ├── Dev Account (team A)
│   │   └── Dev Account (team B)
│   ├── Staging OU
│   │   └── Staging Account
│   └── Prod OU
│       ├── Prod Account (team A)
│       └── Prod Account (team B)
└── Sandbox OU
    └── Sandbox Accounts (individual developer exploration)
```

Management account:
- used only for org management (billing, SCPs, account creation)
- NEVER deploy application workloads in management account

## 3. Key Account Types

| Account Type | Purpose |
|---|---|
| Management | billing, SCPs, account creation only |
| Log Archive | centralized CloudTrail, Config, ALB logs — immutable |
| Security Tooling | Security Hub, GuardDuty, Inspector, Config Aggregator |
| Network | Transit Gateway hub, Direct Connect, shared VPC, DNS |
| Shared Services | ECR, internal APIs, artifact repositories |
| Workload accounts | one per team per environment |

---

# Topic 2: Service Control Policies (SCPs)

## 1. What SCPs Do

SCPs are permission guardrails attached to OUs and accounts:

```text
SCPs do NOT grant permissions.
SCPs set the MAXIMUM permissions that accounts can have.
Even a full admin in a member account CANNOT exceed what SCP allows.
```

## 2. Common Production SCPs

Deny leaving the organization:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "DenyLeaveOrg",
    "Effect": "Deny",
    "Action": "organizations:LeaveOrganization",
    "Resource": "*"
  }]
}
```

Deny root account actions (force IAM users):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "DenyRootUserActions",
    "Effect": "Deny",
    "Action": "*",
    "Resource": "*",
    "Condition": {
      "StringLike": {
        "aws:PrincipalArn": "arn:aws:iam::*:root"
      }
    }
  }]
}
```

Restrict to approved regions:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "DenyNonApprovedRegions",
    "Effect": "Deny",
    "NotAction": [
      "iam:*",
      "organizations:*",
      "route53:*",
      "support:*",
      "sts:*",
      "cloudfront:*"
    ],
    "Resource": "*",
    "Condition": {
      "StringNotEquals": {
        "aws:RequestedRegion": ["us-east-1", "eu-west-1"]
      }
    }
  }]
}
```

Note: `NotAction` exempts global services from the region restriction (IAM, Route 53, CloudFront are global).

Require encryption on S3:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "RequireS3Encryption",
    "Effect": "Deny",
    "Action": "s3:PutObject",
    "Resource": "*",
    "Condition": {
      "Null": {
        "s3:x-amz-server-side-encryption": "true"
      }
    }
  }]
}
```

Prevent disabling security services:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "ProtectSecurityServices",
    "Effect": "Deny",
    "Action": [
      "cloudtrail:DeleteTrail",
      "cloudtrail:StopLogging",
      "config:DeleteConfigRule",
      "config:DeleteConfigurationRecorder",
      "guardduty:DeleteDetector",
      "securityhub:DisableSecurityHub"
    ],
    "Resource": "*"
  }]
}
```

## 3. SCP Inheritance

SCPs apply at OU level and inherit downward:

```text
Root SCP: deny leaving org
Workloads OU SCP: restrict to us-east-1 + eu-west-1
Prod OU SCP: deny ec2:RunInstances for instance types NOT in approved list

Result for Prod Account:
  - Cannot leave org (Root)
  - Cannot create resources in unapproved regions (Workloads OU)
  - Cannot launch unapproved EC2 types (Prod OU)
  - Subject to FullAWSAccess (the default allow SCP must also exist)
```

---

# Topic 3: AWS Control Tower

## 1. What Control Tower Does

Control Tower provisions a Landing Zone with:
- multi-account setup (management account + Log Archive + Security Tooling)
- baseline SCPs pre-configured
- centralized CloudTrail
- AWS Config enabled in all accounts
- Account Factory for automated account vending
- Guardrails (preventive SCPs + detective Config rules)

## 2. Guardrails

| Guardrail Type | Mechanism | Example |
|---|---|---|
| Preventive | SCP (blocks action) | Disallow changes to CloudTrail |
| Detective | Config rule (detects violation) | Detect public S3 buckets |
| Proactive | CloudFormation hooks (blocks non-compliant IaC) | Block RDS without encryption |

Mandatory guardrails (cannot be disabled):
- Disallow changes to CloudTrail
- Disallow changes to CloudWatch Logs
- Disallow deletion of Log Archive

Strongly recommended:
- Detect public S3 buckets
- Disallow root account usage
- Enable MFA for root

## 3. Account Factory

Account Factory automates account vending:

```text
Self-service: developer submits account request via Service Catalog
Account Factory runs:
  1. Creates new AWS account
  2. Enrolls in Control Tower
  3. Applies baseline SCPs
  4. Configures CloudTrail, Config, Security Hub
  5. Creates standard IAM roles for federated access
  6. Sends notification with account details

New account is compliant from day one.
```

Account Factory for Terraform (AFT):
- account provisioning via Terraform
- customizations via Terraform (not just Service Catalog)
- version-controlled account configurations

---

# Topic 4: Centralized Security And Governance

## 1. Centralized CloudTrail

```text
Organization Trail:
  - Created in management account
  - Records events for ALL accounts in organization
  - Delivers to S3 bucket in Log Archive account
  - Bucket policy: member accounts can write, never delete

Log Archive S3 bucket:
  - SCP: no delete actions on Log Archive account
  - Versioning enabled
  - Object Lock (WORM): records immutable for retention period
  - Glacier lifecycle for older logs
```

## 2. GuardDuty — Threat Detection

GuardDuty uses ML and threat intelligence to detect:
- unusual API calls
- cryptocurrency mining
- compromised credentials
- port scanning from EC2
- DNS exfiltration attempts
- ECS/EKS container threats

Organization setup:

```text
GuardDuty delegated admin account (Security Tooling)
-> automatically enables GuardDuty in all member accounts
-> all findings aggregated in Security Tooling account
-> High/Medium/Low findings -> SNS -> Security team alert
```

## 3. Security Hub

Security Hub aggregates findings from:
- GuardDuty
- Inspector (vulnerability assessment)
- Macie (S3 sensitive data discovery)
- IAM Access Analyzer
- Config
- Third-party tools (Crowdstrike, Palo Alto)

Security standards:
- AWS Foundational Security Best Practices
- CIS AWS Foundations Benchmark
- PCI DSS
- NIST 800-53

Organization setup:

```text
Security Hub delegated admin (Security Tooling account)
-> aggregates all findings across all accounts
-> custom insights (filters + groupings)
-> suppression rules for known false positives
-> automated remediation via EventBridge rules
```

## 4. IAM Identity Center (AWS SSO)

Single sign-on for all AWS accounts:

```text
Identity sources:
  - IAM Identity Center directory (managed by AWS)
  - Active Directory (via AD Connector or AWS Managed AD)
  - External IdP (Okta, Google Workspace, Azure AD) via SAML 2.0

Access:
  - Assign Permission Sets to accounts
  - Permission Set = IAM policies bundled together
  - Example: "Developer" permission set in Dev account
             "ReadOnly" permission set in Prod account

User experience:
  - Developer goes to SSO portal
  - Clicks Dev account -> gets temporary credentials
  - Never has IAM user or static keys
```

---

# Topic 5: FinOps Governance

## 1. Cost Allocation

Tags as cost attribution:

```json
{
  "Team": "payments",
  "Environment": "prod",
  "CostCenter": "engineering-123"
}
```

SCP to require tags on resource creation:

```json
{
  "Effect": "Deny",
  "Action": "ec2:RunInstances",
  "Resource": "*",
  "Condition": {
    "Null": {
      "aws:RequestTag/Team": "true"
    }
  }
}
```

## 2. Budget Alerts

```text
AWS Budgets:
  Account-level budget: $10,000/month for prod account
  Alert: 80% → email notification
  Alert: 100% → email + SNS → Slack + PagerDuty
  Alert: 110% (forecasted) → email early warning

Organization Budget:
  Total org spend budget
  Per-OU budget
```

## 3. Cost Explorer And Savings

Cost Explorer daily report:
- view by account, service, region, tag
- 12-month forecast

Savings Plans:
- Compute Savings Plans: covers EC2, Lambda, Fargate
- EC2 Instance Savings Plans: specific family

Reserved Instances vs Savings Plans:
- Savings Plans: more flexible (applies to any compatible compute)
- Reserved Instances: more rigid, but can cover DynamoDB, ElastiCache, RDS specifically

## 4. Common Mistakes

| Mistake | Better Approach |
|---|---|
| All workloads in management account | management account for billing/org management only |
| No SCP to protect security services | SCP: deny GuardDuty, CloudTrail, Config disable |
| SCPs thought to grant permissions | SCPs only restrict, never grant; must keep FullAWSAccess SCP |
| No account vending machine | manual account creation = inconsistent baselines |
| Single shared AWS account for all teams | account per team per environment = blast radius isolation |
| No centralized security findings | Security Hub + GuardDuty org-wide delegated admin |
| No tagging policy + enforcement | SCP to require tags at resource creation |
| IAM users for developers | IAM Identity Center (SSO) for human access, roles for services |

## 5. Interview Scenario

**Scenario**: "How would you structure AWS accounts for a 500-person engineering org with 20 teams?"

Strong answer:

```text
Multi-account landing zone with Control Tower:

OU structure:
  Root
  ├── Security OU: Log Archive, Security Tooling
  ├── Infrastructure OU: Network (Transit Gateway), Shared Services (ECR, artifacts)
  ├── Dev OU: one account per team (20 dev accounts)
  ├── Staging OU: one account per product area (3-5 staging accounts)
  ├── Prod OU: one account per product area (3-5 prod accounts)
  └── Sandbox OU: individual developer exploration

SCPs:
  Root: deny leaving org, deny disabling security services, require S3 encryption
  Prod OU: deny launch of instance types outside approved list, require tags
  All accounts: restrict to approved regions

Account vending:
  Account Factory for Terraform (AFT)
  Developers self-service via Service Catalog
  New account: baseline in < 30 minutes with all guardrails applied

Access:
  IAM Identity Center with Okta SAML integration
  Developer permission set: developer power in Dev; read-only in Prod
  On-call permission set: temporary elevated access to Prod

Security:
  GuardDuty and Security Hub aggregated in Security Tooling account
  Centralized CloudTrail → Log Archive account (Object Lock, immutable)
  Config Aggregator: org-wide compliance dashboard

Billing:
  Consolidated billing under management account
  Budget alerts per account and per OU
  Tags enforced via SCP: Team, CostCenter, Environment
```

## 6. Revision Notes

- Management account: billing and org only; never deploy workloads
- SCPs: restrict only, never grant; FullAWSAccess must also be in place
- Control Tower: Landing Zone + guardrails + Account Factory
- Log Archive: immutable, Object Lock, centralized; protect with SCP
- GuardDuty + Security Hub: org-wide via delegated admin in Security Tooling account
- IAM Identity Center: SSO for humans; roles for services; no IAM users
- Account vending: AFT (Terraform) or Control Tower Account Factory; consistent baseline
- SCP with NotAction for global services when restricting by region

## 7. Official Source Notes

- AWS Organizations: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_introduction.html>
- SCPs: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps.html>
- Control Tower: <https://docs.aws.amazon.com/controltower/latest/userguide/what-is-control-tower.html>
- Account Factory for Terraform: <https://docs.aws.amazon.com/controltower/latest/userguide/aft-overview.html>
- GuardDuty: <https://docs.aws.amazon.com/guardduty/latest/ug/what-is-guardduty.html>
- Security Hub: <https://docs.aws.amazon.com/securityhub/latest/userguide/what-is-securityhub.html>
- IAM Identity Center: <https://docs.aws.amazon.com/singlesignon/latest/userguide/what-is.html>
