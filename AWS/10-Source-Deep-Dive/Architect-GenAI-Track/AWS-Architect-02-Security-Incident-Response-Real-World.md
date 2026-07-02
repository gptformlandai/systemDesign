# AWS Architect 02: Security Architecture and Incident Response Real-World Guide

> Goal: design AWS security like a production architect: identity, detection, posture management, data protection, incident response, and GenAI-specific security.

---

# Index

| Section | Focus |
|---|---|
| [0. Real Situation](#0-real-situation) | Real Situation |
| [1. Intuition](#1-intuition) | Intuition |
| [2. Core Services Map](#2-core-services-map) | Core Services Map |
| [3. Console Build: IAM Identity Center For Human Access](#3-console-build-iam-identity-center-for-human-access) | Console Build: IAM Identity Center For Human Access |
| [4. Console Build: GuardDuty](#4-console-build-guardduty) | Console Build: GuardDuty |
| [5. Console Build: Security Hub](#5-console-build-security-hub) | Console Build: Security Hub |
| [6. Console Build: AWS Config](#6-console-build-aws-config) | Console Build: AWS Config |
| [7. Console Build: IAM Access Analyzer](#7-console-build-iam-access-analyzer) | Console Build: IAM Access Analyzer |
| [8. Console Build: Inspector](#8-console-build-inspector) | Console Build: Inspector |
| [9. Console Build: Macie](#9-console-build-macie) | Console Build: Macie |
| [10. Console Build: KMS Keys](#10-console-build-kms-keys) | Console Build: KMS Keys |
| [11. Console Build: Bedrock Guardrails](#11-console-build-bedrock-guardrails) | Console Build: Bedrock Guardrails |
| [12. Incident Response Scenario: Leaked Access Key](#12-incident-response-scenario-leaked-access-key) | Incident Response Scenario: Leaked Access Key |
| [13. Incident Response Scenario: Public S3 Bucket](#13-incident-response-scenario-public-s3-bucket) | Incident Response Scenario: Public S3 Bucket |
| [14. Incident Response Scenario: RAG App Leaks Sensitive Data](#14-incident-response-scenario-rag-app-leaks-sensitive-data) | Incident Response Scenario: RAG App Leaks Sensitive Data |
| [15. Production Security Checklist](#15-production-security-checklist) | Production Security Checklist |
| [16. Interview Question](#16-interview-question) | Interview Question |
| [17. Strong Answer](#17-strong-answer) | Strong Answer |
| [18. Revision Notes](#18-revision-notes) | Revision Notes |
| [19. Official Source Notes](#19-official-source-notes) | Official Source Notes |

---

## 0. Real Situation

Your team has a production app on AWS.

One morning:

```text
CloudWatch shows unusual API traffic.
An S3 bucket policy was changed.
GuardDuty reports suspicious credential use.
A developer asks if the Bedrock RAG app can ingest HR documents.
Security asks: who changed what, what data was exposed, and how do we prevent this?
```

Architect answer:

```text
Security is not one service.
It is identity + network + data protection + detection + audit + response + governance.
```

---

## 1. Intuition

AWS security is layered:

```text
Identity:
  who can do what?

Network:
  who can reach what?

Data:
  what is encrypted, classified, and protected?

Detection:
  how do we know something bad happened?

Audit:
  who did what and when?

Response:
  how do we contain and recover?
```

For GenAI add:

```text
What data can the model see?
Can users prompt-inject the app?
Can responses leak PII?
Are outputs grounded in allowed sources?
```

---

## 2. Core Services Map

| Problem | AWS Service |
|---|---|
| Human access to accounts | IAM Identity Center |
| Machine access to AWS | IAM roles |
| High-level account guardrails | SCPs |
| API audit trail | CloudTrail |
| Misconfiguration detection | AWS Config |
| Threat detection | GuardDuty |
| Central security findings | Security Hub |
| Vulnerability scanning | Inspector |
| Sensitive data discovery | Macie |
| External/public access analysis | IAM Access Analyzer |
| Secrets | Secrets Manager / Parameter Store |
| Encryption keys | KMS |
| Web attack filtering | WAF |
| GenAI safety | Bedrock Guardrails |

---

## 3. Console Build: IAM Identity Center For Human Access

### Console Path

```text
AWS Console -> Search "IAM Identity Center" -> Enable -> Choose identity source
```

Options:

```text
Built-in Identity Center directory
External identity provider such as Okta/Azure AD
AWS Managed Microsoft AD
```

### What This Click Changes

It creates a central human login plane for AWS accounts.

### Why It Matters

Humans should not use long-lived IAM users for console access.

Better:

```text
SSO login
temporary credentials
group-based permissions
account-specific assignments
central removal when employee leaves
```

### What Can Go Wrong

If you assign broad admin permission sets everywhere, SSO becomes a faster way to over-permission people.

Architect move:

```text
ReadOnly by default.
NonProdPowerUser for builders.
ProdReadOnly for most engineers.
ProdEmergencyAdmin only through break-glass workflow.
```

---

## 4. Console Build: GuardDuty

### Console Path

```text
AWS Console -> Search "GuardDuty" -> Get started -> Enable GuardDuty
```

In an organization:

```text
GuardDuty -> Settings -> Accounts -> Delegate administrator
```

### What This Click Changes

GuardDuty starts analyzing events and signals such as:

- CloudTrail management events
- VPC Flow Logs metadata
- DNS logs
- selected runtime/data signals depending on configuration

### Why It Matters

GuardDuty detects suspicious behavior like:

- unusual API calls
- credential exfiltration indicators
- crypto mining behavior
- communication with malicious IPs/domains
- suspicious S3 access patterns

### What Can Go Wrong

Enabling findings without alerting creates a silent dashboard.

Production setup:

```text
GuardDuty finding -> EventBridge rule -> SNS/PagerDuty/Slack/SIEM
```

---

## 5. Console Build: Security Hub

### Console Path

```text
AWS Console -> Search "Security Hub" -> Go to Security Hub -> Enable
```

Enable standards:

```text
AWS Foundational Security Best Practices
CIS AWS Foundations Benchmark
PCI DSS if required
```

### What This Click Changes

Security Hub aggregates findings from AWS security services and runs security controls.

### Why It Matters

Security teams need one place to triage:

```text
GuardDuty finding
Inspector vulnerability
Config compliance failure
Macie sensitive data finding
IAM Access Analyzer finding
```

### What Can Go Wrong

Security Hub can produce many findings.

Architect move:

```text
Prioritize critical/high.
Route by account/workload owner tags.
Define SLA by severity.
Suppress only with documented reason.
```

---

## 6. Console Build: AWS Config

### Console Path

```text
AWS Console -> Search "AWS Config" -> Get started -> Record all resources -> Choose S3 bucket -> Add rules
```

Useful managed rules:

```text
s3-bucket-public-read-prohibited
s3-bucket-server-side-encryption-enabled
rds-storage-encrypted
restricted-ssh
cloudtrail-enabled
root-account-mfa-enabled
```

### What This Click Changes

AWS Config records resource configuration history and evaluates rules.

### Why It Matters

CloudTrail says:

```text
Who changed it?
```

AWS Config says:

```text
What did the resource look like before and after?
Is it compliant with policy?
```

### What Can Go Wrong

Recording everything in every account/region has cost impact.

Architect move:

```text
Enable broadly for production and regulated workloads.
Be intentional in sandbox.
Aggregate findings centrally.
```

---

## 7. Console Build: IAM Access Analyzer

### Console Path

```text
AWS Console -> Search "IAM Access Analyzer" -> Create analyzer
```

Choose:

```text
Organization analyzer
```

### What This Click Changes

Access Analyzer checks resource policies for unintended external or cross-account access.

### Why It Matters

It helps catch:

- S3 buckets shared publicly
- KMS keys shared externally
- IAM roles assumable by unknown accounts
- SQS/SNS policies open to other accounts

### What Can Go Wrong

Findings are not automatically fixes.

Production process:

```text
Finding -> validate business need -> remediate policy -> document exception if intentional.
```

---

## 8. Console Build: Inspector

### Console Path

```text
AWS Console -> Search "Inspector" -> Get started -> Enable scanning
```

Enable:

```text
EC2 scanning
ECR container image scanning
Lambda scanning where applicable
```

### What This Click Changes

Inspector scans workloads for software vulnerabilities and exposure.

### Why It Matters

For container platforms:

```text
ECR image vulnerability -> fix base image/library -> rebuild -> redeploy
```

### What Can Go Wrong

Ignoring vulnerability context creates noise.

Architect move:

```text
Patch critical exploitable vulnerabilities quickly.
Use CI gates for critical CVEs.
Track exceptions with expiry dates.
```

---

## 9. Console Build: Macie

### Console Path

```text
AWS Console -> Search "Macie" -> Get started -> Enable Macie
```

Select buckets:

```text
S3 buckets containing documents, uploads, exports, logs, or GenAI ingestion data.
```

### What This Click Changes

Macie discovers sensitive data such as PII in S3.

### Why It Matters

For GenAI RAG:

```text
Before ingesting documents into a knowledge base,
know whether those documents contain PII, PHI, secrets, or regulated data.
```

### What Can Go Wrong

Scanning all buckets at maximum depth can be expensive.

Architect move:

```text
Prioritize sensitive buckets.
Use sampling where appropriate.
Classify buckets by data sensitivity.
```

---

## 10. Console Build: KMS Keys

### Console Path

```text
AWS Console -> Search "KMS" -> Customer managed keys -> Create key
```

Choose:

```text
Symmetric key
Encrypt and decrypt
Alias: alias/app1-prod-data
Key administrators: security/platform roles
Key users: specific workload roles
```

### What This Click Changes

It creates a customer-managed encryption key with explicit access policy.

### Why It Matters

KMS gives:

- encryption control
- key usage audit through CloudTrail
- rotation policy
- separation of data access and key access

### What Can Go Wrong

Bad key policy can lock out admins or overexpose data.

Architect move:

```text
Use least-privilege key policies.
Keep break-glass admin path.
Avoid wildcard key users.
```

---

## 11. Console Build: Bedrock Guardrails

### Console Path

```text
AWS Console -> Search "Bedrock" -> Guardrails -> Create guardrail
```

Configure:

```text
content filters
denied topics
word filters
sensitive information filters
contextual grounding checks
blocked response message
```

### What This Click Changes

It creates a guardrail policy that can evaluate model inputs and outputs.

### Why It Matters

GenAI apps need controls for:

- harmful content
- prompt attacks
- PII leakage
- ungrounded answers in RAG
- disallowed topics such as legal/medical/financial advice depending on app

### What Can Go Wrong

Overly strict filters frustrate users.

Overly weak filters leak data or generate unsafe output.

Architect move:

```text
Test guardrails with real abuse cases.
Version guardrails.
Log blocked reasons.
Tune per use case.
```

---

## 12. Incident Response Scenario: Leaked Access Key

### Situation

CloudTrail and GuardDuty show an access key used from an unusual country.

### Immediate Actions

```text
1. Identify IAM user or role.
2. Disable access key.
3. Check CloudTrail for actions taken.
4. Rotate any secrets touched.
5. Revoke sessions where possible.
6. Review resources created or modified.
7. Add prevention: no long-lived keys for humans.
```

### Console Path

```text
IAM -> Users -> Select user -> Security credentials -> Deactivate access key
CloudTrail -> Event history -> Filter by access key ID
GuardDuty -> Findings -> Investigate finding
Security Hub -> Findings -> Track incident
```

### What Each Click Does

```text
Deactivate access key:
  stops new API calls using that key.

CloudTrail filter:
  builds timeline of what the key did.

GuardDuty finding:
  shows why AWS considered it suspicious.

Security Hub:
  centralizes tracking and severity.
```

### Follow-Up Fix

```text
Replace IAM users with IAM Identity Center for humans.
Use IAM roles for workloads.
Add SCP or detective controls against access key creation if policy allows.
```

---

## 13. Incident Response Scenario: Public S3 Bucket

### Situation

Security Hub reports an S3 bucket is public.

### Console Path

```text
S3 -> Buckets -> Select bucket -> Permissions
```

Check:

```text
Block Public Access
Bucket policy
ACLs
Access Analyzer finding
Macie classification
CloudTrail events
```

### What Each Click Changes

```text
Enable Block Public Access:
  prevents public access through policies/ACLs depending on selected settings.

Edit bucket policy:
  removes public principals such as "*".

Review ACLs:
  catches old object-level access patterns.

Macie:
  determines if sensitive data exists in bucket.

CloudTrail:
  identifies who made the bucket public.
```

### Production Answer

```text
Contain exposure, classify data, audit access, remove public access,
rotate affected secrets if any, notify required teams, and add preventive controls.
```

---

## 14. Incident Response Scenario: RAG App Leaks Sensitive Data

### Situation

User asks a chatbot for salary data. The model returns details from HR documents.

### Immediate Questions

```text
Was the data source allowed for this app?
Was access filtered by user identity?
Did retrieval return documents the user should not see?
Did guardrails detect PII?
Were prompts/responses logged safely?
```

### Console Path

```text
Bedrock -> Knowledge Bases -> Select KB -> Data sources
Bedrock -> Guardrails -> Review sensitive information filters
S3 -> Source bucket -> Permissions
IAM -> Role used by app -> Review permissions
CloudWatch -> Logs -> Search request/session ID
CloudTrail -> Lookup Bedrock/S3 API events
```

### Fix

```text
1. Disable affected data source sync.
2. Restrict source S3 bucket permissions.
3. Add document-level authorization filtering.
4. Enable or tighten PII guardrail.
5. Re-ingest only approved content.
6. Add eval cases for sensitive data leakage.
```

Architect note:

```text
RAG security is not solved by private S3 alone.
Retrieval must respect user authorization and data classification.
```

---

## 15. Production Security Checklist

- IAM Identity Center for human access
- no root user daily use
- MFA on root and privileged users
- no long-lived access keys for humans
- workload IAM roles only
- least-privilege task/instance/pod roles
- CloudTrail organization trail
- GuardDuty delegated admin enabled
- Security Hub enabled with standards
- AWS Config for critical resources
- Access Analyzer organization analyzer
- Inspector for EC2/ECR/Lambda where used
- Macie for sensitive S3 data
- KMS keys scoped by workload
- S3 Block Public Access
- secrets stored in Secrets Manager
- WAF for public HTTP apps
- Bedrock Guardrails for GenAI apps
- incident response runbooks tested

---

## 16. Interview Question

> How would you secure a production AWS workload and detect if something goes wrong?

---

## 17. Strong Answer

I would start with identity and account boundaries: separate accounts by environment, IAM Identity Center for human access, IAM roles for workloads, and SCPs for guardrails. For network security, I would keep compute and databases private, expose only ALB/API Gateway, and use security groups with least privilege.

For data protection, I would use KMS encryption, Secrets Manager, S3 Block Public Access, and Macie for sensitive S3 data. For detection, I would enable organization CloudTrail, GuardDuty, Security Hub, AWS Config, Inspector, and Access Analyzer. Findings would route to an alerting or SIEM workflow with severity-based response.

For GenAI workloads, I would add Bedrock Guardrails, document classification, authorization-aware retrieval, prompt/response logging with redaction, and evaluation cases for prompt injection and data leakage.

---

## 18. Revision Notes

- One-line summary: AWS security is identity, network, data, detection, audit, and response working together.
- Three keywords: least privilege, centralized detection, incident response.
- One interview trap: saying "we use IAM" and ignoring detection/audit.
- Memory trick: "Prevent, detect, respond."

---

## 19. Official Source Notes

- AWS CloudTrail records user, role, and service actions for audit and governance: <https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-user-guide.html>
- AWS Organizations supports central security and monitoring across accounts: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_introduction.html>
- Bedrock Guardrails provide configurable safeguards for content, PII, prompt attacks, grounding, and reasoning checks: <https://docs.aws.amazon.com/bedrock/latest/userguide/guardrails.html>

