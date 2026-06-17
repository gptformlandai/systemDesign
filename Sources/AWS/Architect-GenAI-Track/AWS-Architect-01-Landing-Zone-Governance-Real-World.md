# AWS Architect 01: Landing Zone, Multi-Account, and Governance Real-World Guide

> Goal: understand how a real company structures AWS accounts, guardrails, audit logs, access, and environment boundaries before deploying serious workloads.

---

# Index

| Section | Focus |
|---|---|
| [0. Real Situation](#0-real-situation) | Real Situation |
| [1. Intuition](#1-intuition) | Intuition |
| [2. Definition](#2-definition) | Definition |
| [3. Why It Exists](#3-why-it-exists) | Why It Exists |
| [4. Standard Account Layout](#4-standard-account-layout) | Standard Account Layout |
| [5. Console Build: Create Organization](#5-console-build-create-organization) | Console Build: Create Organization |
| [6. Console Build: Create OUs](#6-console-build-create-ous) | Console Build: Create OUs |
| [7. Console Build: Create Accounts](#7-console-build-create-accounts) | Console Build: Create Accounts |
| [8. Console Build: Enable Control Tower](#8-console-build-enable-control-tower) | Console Build: Enable Control Tower |
| [9. Console Build: Add Guardrails / Controls](#9-console-build-add-guardrails-controls) | Console Build: Add Guardrails / Controls |
| [10. Console Build: IAM Identity Center Access](#10-console-build-iam-identity-center-access) | Console Build: IAM Identity Center Access |
| [11. Console Build: Centralized CloudTrail](#11-console-build-centralized-cloudtrail) | Console Build: Centralized CloudTrail |
| [12. Example SCPs](#12-example-scps) | Example SCPs |
| [13. CLI / IaC Equivalent](#13-cli-iac-equivalent) | CLI / IaC Equivalent |
| [14. Real-World Scenario: New GenAI Team Needs AWS](#14-real-world-scenario-new-genai-team-needs-aws) | Real-World Scenario: New GenAI Team Needs AWS |
| [15. Failure Modes](#15-failure-modes) | Failure Modes |
| [16. Production Checklist](#16-production-checklist) | Production Checklist |
| [17. Interview Question](#17-interview-question) | Interview Question |
| [18. Strong Answer](#18-strong-answer) | Strong Answer |
| [19. Revision Notes](#19-revision-notes) | Revision Notes |
| [20. Official Source Notes](#20-official-source-notes) | Official Source Notes |

---

## 0. Real Situation

You join a company where everything runs in one AWS account:

```text
dev workloads
staging workloads
production workloads
data lake
admin users
security logs
experiments
GenAI prototypes
```

Problems start:

- one developer can accidentally touch production
- costs are hard to split by team
- security logs can be modified by workload admins
- experiments can create public S3 buckets
- IAM becomes messy
- production blast radius is too large
- no standard way to create new accounts

Architect answer:

```text
Use a multi-account landing zone with AWS Organizations,
Control Tower, IAM Identity Center, centralized logging,
security accounts, workload accounts, and SCP guardrails.
```

---

## 1. Intuition

An AWS account is not just a billing container.

It is a strong boundary for:

```text
permissions
blast radius
networking
cost
audit
compliance
environment separation
```

Architect mental model:

```text
One account for everything = one giant room.
Multi-account AWS = office building with locked rooms, cameras, rules, and separate budgets.
```

---

## 2. Definition

- Landing zone: a governed AWS foundation with accounts, identity, networking, audit logs, and guardrails.
- AWS Organizations: central account and policy management.
- AWS Control Tower: managed landing zone setup and governance on top of Organizations.
- SCP: service control policy that defines maximum permissions for accounts/OUs.
- OU: organizational unit used to group accounts by environment, workload, or security function.

---

## 3. Why It Exists

Without a landing zone:

- every account is configured differently
- audit logging is inconsistent
- security is reactive
- developer access is manual
- cost ownership is unclear
- production and non-production boundaries blur

With a landing zone:

- accounts are created consistently
- logs go to protected accounts
- security tooling is centralized
- developers get least-privilege access
- production has stricter guardrails
- compliance evidence is easier to produce

---

## 4. Standard Account Layout

For a serious environment:

```text
Management account
  -> owns AWS Organizations and billing
  -> not used for workloads

Security account
  -> GuardDuty admin
  -> Security Hub admin
  -> IAM Access Analyzer
  -> incident response access

Log archive account
  -> centralized CloudTrail logs
  -> VPC Flow Logs
  -> AWS Config history
  -> immutable/audited buckets

Shared services account
  -> shared networking
  -> DNS
  -> CI/CD runners or shared tools
  -> container base images if needed

Workload accounts
  -> app-dev
  -> app-stage
  -> app-prod
  -> data-dev
  -> data-prod
  -> genai-sandbox
  -> genai-prod
```

Rule:

```text
Keep production separate from non-production.
Keep security and log archive separate from workload admins.
```

---

## 5. Console Build: Create Organization

### Console Path

```text
AWS Console -> Search "AWS Organizations" -> Create organization
```

### What This Click Changes

It creates an organization root and turns the current account into the management account.

### Why It Matters

The management account can:

- create member accounts
- group accounts into OUs
- attach SCPs
- consolidate billing
- delegate admin to security services

### What Can Go Wrong

Do not run workloads in the management account.

Why:

```text
The management account is the highest-value account.
If it is compromised, your entire AWS organization is at risk.
```

---

## 6. Console Build: Create OUs

### Console Path

```text
AWS Console -> Organizations -> AWS accounts -> Root -> Actions -> Create organizational unit
```

Create:

```text
Security
Infrastructure
Workloads
Sandbox
Suspended
```

Inside `Workloads`, create:

```text
Dev
Stage
Prod
Data
GenAI
```

### What This Click Changes

It creates policy attachment points.

Policies attached to an OU apply to accounts inside that OU.

### Why It Matters

You can say:

```text
Prod accounts cannot disable CloudTrail.
Sandbox accounts cannot create expensive GPU instances.
GenAI sandbox cannot call unapproved regions.
```

### What Can Go Wrong

Bad OU design causes messy policy exceptions later.

Better:

```text
Group accounts by governance need, not by org chart only.
```

---

## 7. Console Build: Create Accounts

### Console Path

```text
AWS Console -> Organizations -> AWS accounts -> Add an AWS account -> Create an AWS account
```

Create accounts like:

```text
security-prod
log-archive-prod
network-prod
app1-dev
app1-stage
app1-prod
genai-sandbox
genai-prod
```

### What This Click Changes

It creates a new AWS account under the organization.

### Why It Matters

Each account gets:

- its own IAM boundary
- its own cost boundary
- its own service quotas
- its own CloudTrail events
- its own blast radius

### What Can Go Wrong

Creating too many accounts without naming, tags, owners, and budgets creates chaos.

Every account should have:

```text
owner
environment
cost-center
data-classification
support-contact
purpose
```

---

## 8. Console Build: Enable Control Tower

### Console Path

```text
AWS Console -> Search "Control Tower" -> Set up landing zone
```

You choose:

- home region
- additional governed regions
- log archive account
- audit/security account
- IAM Identity Center setup

### What This Click Changes

Control Tower sets up a governed landing zone using AWS Organizations and related services.

It creates or configures:

- OUs
- log archive account
- audit account
- mandatory controls
- Account Factory
- IAM Identity Center integration

### Why It Matters

You get a repeatable account vending and governance baseline instead of manually wiring everything.

### What Can Go Wrong

Do not enable regions casually.

Why:

```text
More governed regions means more audit/control coverage,
but also more places where resources can be created.
```

For many companies:

```text
Start with approved business regions only.
Deny all others through SCP except global services.
```

---

## 9. Console Build: Add Guardrails / Controls

### Console Path

```text
AWS Console -> Control Tower -> Controls -> Browse controls -> Enable control
```

Common controls:

- disallow public S3 buckets
- require CloudTrail
- disallow deleting log buckets
- restrict root user access
- restrict regions
- require encryption

### What This Click Changes

Control Tower applies preventive or detective governance rules.

Preventive:

```text
Blocks action before it happens.
Example: deny disabling CloudTrail.
```

Detective:

```text
Detects drift after it happens.
Example: Config rule finds public S3 bucket.
```

### Why It Matters

Controls reduce human mistakes.

### What Can Go Wrong

Strict controls can block legitimate deployments.

Architect move:

```text
Apply strongest controls to Prod.
Use safer but more flexible controls in Sandbox.
Document exception workflow.
```

---

## 10. Console Build: IAM Identity Center Access

### Console Path

```text
AWS Console -> IAM Identity Center -> Users / Groups -> Permission sets
```

Create groups:

```text
Developers
ReadOnly
PlatformAdmins
SecurityAudit
BillingViewers
GenAIExperimenters
ProdBreakGlass
```

Create permission sets:

```text
DeveloperPowerUserNonProd
ReadOnlyAll
SecurityAudit
BillingReadOnly
ProdReadOnly
ProdEmergencyAdmin
```

Assign:

```text
IAM Identity Center -> AWS accounts -> Select account -> Assign users or groups
```

### What This Click Changes

It grants federated access into selected AWS accounts using temporary credentials.

### Why It Matters

You avoid long-lived IAM users and access keys.

### What Can Go Wrong

Giving broad admin to production as a normal role is dangerous.

Better:

```text
Production write access should be rare, logged, time-bound, and preferably approval-based.
```

---

## 11. Console Build: Centralized CloudTrail

### Console Path

```text
AWS Console -> CloudTrail -> Trails -> Create trail
```

Choose:

- apply trail to all accounts in organization
- multi-region trail
- log destination in log archive account S3 bucket
- log file validation enabled
- CloudWatch Logs integration if needed

### What This Click Changes

It records AWS API activity across accounts and regions.

### Why It Matters

CloudTrail answers:

```text
Who did what?
When?
From where?
Using which role?
Against which resource?
```

### What Can Go Wrong

If workload admins can delete or alter audit logs, the audit trail is weak.

Better:

```text
Send organization trails to a separate log archive account.
Restrict delete permissions.
Enable S3 versioning and retention controls where required.
```

---

## 12. Example SCPs

### Deny Leaving Approved Regions

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyUnapprovedRegions",
      "Effect": "Deny",
      "NotAction": [
        "iam:*",
        "organizations:*",
        "route53:*",
        "cloudfront:*",
        "support:*"
      ],
      "Resource": "*",
      "Condition": {
        "StringNotEquals": {
          "aws:RequestedRegion": [
            "us-east-1",
            "us-west-2"
          ]
        }
      }
    }
  ]
}
```

Impact:

```text
Even if a user has IAM permission, they cannot create resources outside approved regions.
```

### Deny Disabling CloudTrail

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyCloudTrailTampering",
      "Effect": "Deny",
      "Action": [
        "cloudtrail:StopLogging",
        "cloudtrail:DeleteTrail",
        "cloudtrail:UpdateTrail"
      ],
      "Resource": "*"
    }
  ]
}
```

Impact:

```text
Prevents account users from weakening audit logging.
```

---

## 13. CLI / IaC Equivalent

Create OU:

```bash
aws organizations create-organizational-unit \
  --parent-id r-example \
  --name Prod
```

Create account:

```bash
aws organizations create-account \
  --email app1-prod@example.com \
  --account-name app1-prod
```

Create policy:

```bash
aws organizations create-policy \
  --name DenyUnapprovedRegions \
  --type SERVICE_CONTROL_POLICY \
  --content file://deny-unapproved-regions.json
```

Attach policy:

```bash
aws organizations attach-policy \
  --policy-id p-example \
  --target-id ou-example-prod
```

Architect note:

```text
For production, automate account creation through Control Tower Account Factory,
Service Catalog, Terraform, or internal platform workflows.
```

---

## 14. Real-World Scenario: New GenAI Team Needs AWS

### Requirement

A team wants to build a RAG chatbot over internal documents using Bedrock.

### Bad Approach

```text
Give them admin access in the main production account.
Let them upload documents and test models freely.
```

### Architect Approach

Create:

```text
genai-sandbox account
genai-prod account
central log archive
SCP restricting regions
budget alarm for model usage
IAM Identity Center group: GenAIExperimenters
Bedrock model access approved only for allowed models
S3 buckets with encryption and blocked public access
```

### Console Steps

```text
Organizations -> Create account -> genai-sandbox
Organizations -> Move account -> OU: GenAI/Sandbox
Control Tower -> Controls -> enable region restriction
Billing -> Budgets -> create monthly GenAI budget
IAM Identity Center -> Assign GenAIExperimenters group
Bedrock -> Model access -> request/enable approved models
S3 -> Create bucket -> block public access + encryption
```

### What Each Click Does

```text
Create account:
  isolates experimentation blast radius.

Move to GenAI/Sandbox OU:
  applies sandbox-specific guardrails.

Enable region restriction:
  prevents model/data usage in unapproved regions.

Create budget:
  catches token/inference cost surprises.

Assign group:
  grants temporary federated access, not permanent IAM users.

Enable Bedrock models:
  allows only approved foundation model access.

Create encrypted S3 bucket:
  stores documents for ingestion without public exposure.
```

---

## 15. Failure Modes

### Failure Mode 1: Production And Dev In Same Account

User impact:

```text
Dev mistakes can affect prod resources.
```

Fix:

```text
Separate accounts by environment.
Use SCPs and IAM Identity Center assignments.
```

### Failure Mode 2: CloudTrail Logs In Workload Account

Risk:

```text
Compromised workload admin can tamper with logs.
```

Fix:

```text
Organization trail to log archive account.
Restrict delete access.
```

### Failure Mode 3: SCP Blocks Production Deployment

Cause:

```text
Policy too broad or not tested in lower environment.
```

Fix:

```text
Test SCP in sandbox OU.
Use explicit exception process.
Document why exception exists.
```

### Failure Mode 4: GenAI Cost Explosion

Cause:

```text
No budget, no model usage metrics, no throttling.
```

Fix:

```text
Use budgets, tags, inference profiles, application-level rate limits,
and CloudWatch dashboards.
```

---

## 16. Production Checklist

Before allowing workloads:

- management account has no workloads
- organization CloudTrail enabled
- log archive account protected
- security tooling delegated
- IAM Identity Center configured
- no long-lived IAM users for humans
- OUs exist for environment and governance needs
- SCPs tested before prod attachment
- budgets enabled per account/team
- account tags and owners are documented
- sandbox has stricter cost controls
- prod has stricter change controls

---

## 17. Interview Question

> Your company has 20 teams and everything is in one AWS account. How would you redesign the AWS account structure?

---

## 18. Strong Answer

I would move to a multi-account landing zone using AWS Organizations and Control Tower. I would keep the management account free of workloads, create separate security and log archive accounts, and separate workloads by environment such as dev, stage, and prod. I would use OUs to apply governance policies, IAM Identity Center for federated access, SCPs for high-level guardrails, and centralized CloudTrail for audit.

For production, I would enforce stricter controls such as region restrictions, required logging, blocked public S3 access, and limited break-glass access. For GenAI sandbox accounts, I would add budgets, approved model access, and data classification rules because token usage and sensitive document ingestion can create both cost and compliance risk.

---

## 19. Revision Notes

- One-line summary: AWS accounts are architect-level blast-radius boundaries.
- Three keywords: landing zone, SCP, centralized audit.
- One interview trap: using IAM alone and forgetting SCPs, accounts, logs, and governance.
- Memory trick: "Account first, workload second."

---

## 20. Official Source Notes

- AWS Organizations provides central account management, OUs, policies, consolidated billing, and cross-account governance: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_introduction.html>
- AWS Control Tower sets up and governs a multi-account landing zone with controls and Account Factory: <https://docs.aws.amazon.com/controltower/latest/userguide/what-is-control-tower.html>
- AWS CloudTrail records account activity for audit, governance, and compliance: <https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-user-guide.html>

