# AWS Operations: CloudTrail, Config, and Systems Manager Gold Sheet

> Track: AWS Interview Track — Observability and Operations
> Goal: implement audit trails with CloudTrail, compliance monitoring with Config, and operational management with Systems Manager.

---

## 0. How To Read This

Beginner focus:
- CloudTrail: what it records and why
- AWS Config: what it checks
- Systems Manager basics (Parameter Store, Run Command)

Intermediate focus:
- CloudTrail + CloudWatch Logs integration
- Config rules and remediation
- Systems Manager Session Manager (no SSH required)
- SSM Patch Manager

Senior / MAANG focus:
- CloudTrail multi-region, multi-account aggregation in S3
- Config conformance packs for compliance frameworks
- Systems Manager Automation runbooks
- CloudTrail Insights for API anomaly detection
- Config aggregator for org-wide compliance view
- Operational runbooks with SSM Documents

---

# Topic 1: AWS CloudTrail

## 1. Intuition

CloudTrail records every API call made in your AWS account.

Who did what, from where, when:

```text
Event: ec2:StopInstances
Who: arn:aws:iam::123456789012:user/alice
When: 2025-01-15T10:30:00Z
Where: Source IP 203.0.113.1
What resource: i-12345678
Result: Success
```

Use cases:
- security investigation ("who deleted that S3 bucket?")
- compliance audit ("who accessed these secrets in the last 90 days?")
- change management ("what changed right before the outage?")
- unusual activity alerting (CloudTrail Insights)

## 2. CloudTrail Event Types

| Event Type | What It Records | Cost |
|---|---|---|
| Management Events | control plane: create, modify, delete AWS resources | first trail free |
| Data Events | data plane: S3 GetObject/PutObject, Lambda Invoke, DynamoDB operations | extra cost per event |
| Insights Events | unusual API activity (unusual call rate, error rate) | extra cost |

Management events are always the most important. Enable data events for S3 and Lambda only when you need detailed access auditing.

## 3. Multi-Region Trail

Best practice: create a trail that covers all regions:

```bash
aws cloudtrail create-trail \
  --name org-wide-trail \
  --s3-bucket-name my-cloudtrail-bucket \
  --is-multi-region-trail \
  --enable-log-file-validation \
  --cloud-watch-logs-log-group-arn arn:aws:logs:...:log-group:cloudtrail-logs \
  --cloud-watch-logs-role-arn arn:aws:iam::...:role/cloudtrail-cw-role
```

Log file validation:
- CloudTrail creates SHA-256 hash for each log file
- Detect if log files were tampered or deleted after delivery
- Critical for compliance audits

## 4. CloudTrail → CloudWatch Logs Integration

Stream CloudTrail events to CloudWatch Logs for real-time alerting:

Metric filter for root account usage:

```text
Filter Pattern: { $.userIdentity.type = "Root" && $.userIdentity.invokedBy NOT EXISTS && $.eventType != "AwsServiceEvent" }
Metric Name: RootAccountUsage
Alarm: > 0 -> SNS notification
```

Metric filter for unauthorized API calls:

```text
Filter Pattern: { ($.errorCode = "AccessDenied") || ($.errorCode = "UnauthorizedOperation") }
Metric Name: UnauthorizedAPICalls
Alarm: > 5 per 5 minutes -> investigate
```

Metric filter for security group changes:

```text
Filter Pattern: { ($.eventName = "AuthorizeSecurityGroupIngress") || ($.eventName = "RevokeSecurityGroupIngress") }
Metric Name: SecurityGroupChanges
Alarm: > 0 -> review
```

## 5. CloudTrail Insights

Insights detects unusual API activity:

```text
Baseline: normal API call rate for an action (e.g., ec2:RunInstances)
Insight fires: call rate is 3x or more outside the normal band

Use cases:
- EC2 RunInstances spike (cryptomining, unauthorized resource creation)
- IAM CreateUser spike (compromised admin key)
- DeleteObject spike (ransomware or data destruction)
```

## 6. CloudTrail For Security Investigation

How to investigate: "Who deleted the production database?"

```bash
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=DeleteDBCluster \
  --start-time 2025-01-14T00:00:00 \
  --end-time 2025-01-16T00:00:00

# Output includes:
# Who: userIdentity.arn
# When: eventTime
# Where: sourceIPAddress
# Result: errorCode (absent = success)
```

For complex queries: use CloudTrail Lake or Athena on S3 logs:

```sql
-- Athena query on CloudTrail logs in S3
SELECT eventtime, useridentity.arn, eventname, sourceipaddress
FROM cloudtrail_logs.cloudtrail
WHERE eventname = 'DeleteDBCluster'
  AND eventtime BETWEEN '2025-01-14' AND '2025-01-16'
ORDER BY eventtime;
```

---

# Topic 2: AWS Config

## 1. Intuition

AWS Config continuously records the configuration state of AWS resources and evaluates them against rules.

CloudTrail: who did what (actions/events)
AWS Config: what does it look like now, did it change, is it compliant?

Use cases:
- compliance: "are all S3 buckets encrypted?"
- change history: "what was the security group configuration last Tuesday?"
- drift detection: "is this resource still configured as per our baseline?"

## 2. How Config Works

```text
Config Recorder: records configuration snapshots of all resources
Config Delivery Channel: sends snapshots and change notifications to S3 and/or SNS

Configuration Item: snapshot of resource at a point in time
Configuration History: all Config Items for a resource over time

Config Rule: evaluates whether a resource configuration is compliant
  Result: COMPLIANT | NON_COMPLIANT | NOT_APPLICABLE | ERROR
```

## 3. Config Rules

| Type | How It Evaluates | Example |
|---|---|---|
| Managed Rule | AWS-provided, ready to use | `s3-bucket-server-side-encryption-enabled` |
| Custom Rule | Lambda function you write | check your custom naming convention |
| Service-Linked Rule | managed by an AWS service | Security Hub, Firewall Manager |

Common managed rules:

| Rule | What It Checks |
|---|---|
| `encrypted-volumes` | EBS volumes encrypted |
| `s3-bucket-public-read-prohibited` | no public S3 read access |
| `rds-instance-public-access-check` | no publicly accessible RDS |
| `iam-root-access-key-check` | no active root access keys |
| `multi-region-cloudtrail-enabled` | multi-region trail exists |
| `iam-password-policy` | password policy meets requirements |
| `vpc-flow-logs-enabled` | VPC flow logs enabled |
| `restricted-ssh` | no unrestricted SSH (0.0.0.0/0:22) in security groups |

## 4. Config Remediation

Automatic remediation of non-compliant resources:

```text
Rule: s3-bucket-public-read-prohibited
Non-compliant: my-bucket has public read ACL
Remediation: SSM Automation document AWS-DisableS3BucketPublicReadWrite
  -> automatically removes public ACL when bucket becomes non-compliant
```

Remediation types:
- Automatic: triggers immediately when non-compliant
- Manual: marks for review, you trigger

## 5. Conformance Packs

A bundle of Config rules mapped to a compliance framework:

| Conformance Pack | Compliance Framework |
|---|---|
| AWS-GxP-21-CFR-Part-11 | FDA GxP |
| Operational-Best-Practices-for-CIS | CIS AWS Foundations |
| Operational-Best-Practices-for-PCI-DSS | PCI DSS |
| AWS-HIPAA-Security | HIPAA |
| Operational-Best-Practices-for-SOC2 | SOC 2 |

Deploy a conformance pack to check compliance across all required controls at once.

## 6. Config Aggregator

View compliance across multiple accounts and regions:

```text
Config Aggregator in management account:
  - aggregates Config data from all accounts in AWS Organization
  - single pane: "30 non-compliant resources across org"
  - drill by account, region, resource type

Use with Control Tower for org-wide compliance governance.
```

---

# Topic 3: AWS Systems Manager (SSM)

## 1. What Systems Manager Does

SSM is a unified operational hub for managing EC2 instances (and other resources) at scale:

| SSM Capability | What It Does |
|---|---|
| Session Manager | shell access to EC2 without SSH or bastion |
| Run Command | run scripts across many instances |
| Patch Manager | automated OS patching |
| Parameter Store | store config and secrets |
| Inventory | collect software/config inventory from instances |
| Automation | runbooks for operational tasks |
| Fleet Manager | GUI for managing instances |
| OpsCenter | aggregate and manage OpsItems (incidents) |
| AppConfig | feature flags and configuration deployment |

## 2. Session Manager

Replace SSH with Session Manager:

Benefits:
- no SSH key management (no lost keys, no key rotation)
- no bastion host (no extra EC2 to maintain, patch, secure)
- all sessions logged to CloudWatch Logs and S3
- IAM controls who can start sessions (least privilege)
- works through SSM Agent (pre-installed on Amazon Linux 2, Windows Server)

IAM policy for Session Manager access:

```json
{
  "Effect": "Allow",
  "Action": [
    "ssm:StartSession",
    "ssm:TerminateSession",
    "ssm:ResumeSession"
  ],
  "Resource": [
    "arn:aws:ec2:*:*:instance/*",
    "arn:aws:ssm:*:*:session/${aws:username}-*"
  ]
}
```

Session logging:

```text
All session commands and outputs -> CloudWatch Logs group
CloudTrail: records StartSession, TerminateSession events
Satisfies audit requirement for privileged access management
```

## 3. Parameter Store

Tiered configuration storage:

| Tier | Max Value | TTL | Cost |
|---|---|---|---|
| Standard | 4 KB | no | free |
| Advanced | 8 KB | yes (expire at date) | $0.05/param/month |

Parameter types:
- String: plain text value
- StringList: comma-separated list
- SecureString: encrypted with KMS (CMK or AWS-managed)

Naming convention (hierarchical):

```text
/myapp/prod/db-host
/myapp/prod/db-port
/myapp/prod/feature-flag-new-checkout
/myapp/staging/db-host
```

Fetch by path:

```bash
# Get all prod parameters
aws ssm get-parameters-by-path --path /myapp/prod/ --recursive
```

Access from EC2 or Lambda:

```python
ssm = boto3.client('ssm')
response = ssm.get_parameter(Name='/myapp/prod/db-host')
db_host = response['Parameter']['Value']
```

## 4. Run Command

Execute scripts across fleets of instances:

```bash
# Restart Apache on all tagged instances
aws ssm send-command \
  --document-name "AWS-RunShellScript" \
  --targets "Key=tag:Role,Values=WebServer" \
  --parameters 'commands=["sudo systemctl restart httpd"]' \
  --output-s3-bucket-name my-run-command-output
```

Use cases:
- ad-hoc operational tasks (restart service, clear cache)
- pre-/post-deployment steps
- diagnostics (collect logs, check disk usage)

## 5. Patch Manager

Automated OS patch management:

```text
Patch Baseline: which patches to approve (Critical + Important for prod)
Patch Group: tag-based grouping of instances
Maintenance Window: schedule for patching (e.g., Sunday 2 AM)

Workflow:
  Patch Manager -> scans instances for missing patches (Scan operation)
  Reports non-compliant instances to AWS Config
  Applies approved patches during maintenance window (Install operation)
```

Strategies:
- Scan only: report compliance without patching (visibility first)
- Scan + Install: automated patching during maintenance windows

## 6. SSM Automation

Runbooks for operational procedures:

Common built-in documents:
- `AWS-RestartEC2Instance`: safe restart
- `AWS-StopEC2Instance`: stop instance
- `AWS-CreateImage`: create AMI
- `AWS-DisableS3BucketPublicReadWrite`: remediate public S3

Custom automation document for incident response:

```yaml
schemaVersion: "0.3"
description: "Restart payment service"
mainSteps:
  - name: stopService
    action: aws:runCommand
    inputs:
      DocumentName: AWS-RunShellScript
      Targets:
        - Key: tag:Service
          Values: ["payment"]
      Parameters:
        commands:
          - "sudo systemctl stop payment-service"
  - name: clearCache
    action: aws:runCommand
    inputs:
      DocumentName: AWS-RunShellScript
      Targets:
        - Key: tag:Service
          Values: ["payment"]
      Parameters:
        commands:
          - "sudo rm -rf /tmp/payment-cache/*"
  - name: startService
    action: aws:runCommand
    inputs:
      DocumentName: AWS-RunShellScript
      Targets:
        - Key: tag:Service
          Values: ["payment"]
      Parameters:
        commands:
          - "sudo systemctl start payment-service"
```

## 7. Common Mistakes

| Mistake | Better Approach |
|---|---|
| CloudTrail only in one region | enable multi-region trail to cover all regions |
| No CloudTrail log file validation | enable to detect tampering |
| Config rules without remediation | add auto-remediation for critical rules |
| Session Manager without logging | configure CloudWatch Logs for all sessions |
| SSM Parameter Store without hierarchy | use /app/env/param hierarchy for organized access |
| Patch Manager with no patch groups | tag instances with Patch Group tag for organized patching |
| No CloudTrail → CloudWatch Logs metric filters | add at minimum: root usage, unauthorized access, security group changes |

## 8. Revision Notes

- CloudTrail: every API call; management events (free), data events (paid)
- Multi-region trail + log validation = audit-ready
- CloudTrail → CloudWatch Logs: real-time alerting on root usage, unauthorized calls
- Config: records resource state; rules check compliance; conformance packs for frameworks
- Config aggregator: org-wide compliance view from management account
- Session Manager: no SSH, no bastion, full audit logging, IAM-controlled
- Parameter Store: config by hierarchy (/app/env/key); SecureString for sensitive values
- Patch Manager: patch baseline + patch group + maintenance window = automated patching
- Run Command: fleet-wide script execution, output to S3

## 9. Official Source Notes

- CloudTrail: <https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-user-guide.html>
- CloudTrail Insights: <https://docs.aws.amazon.com/awscloudtrail/latest/userguide/logging-insights-events-with-cloudtrail.html>
- AWS Config: <https://docs.aws.amazon.com/config/latest/developerguide/WhatIsConfig.html>
- Config conformance packs: <https://docs.aws.amazon.com/config/latest/developerguide/conformance-packs.html>
- Systems Manager: <https://docs.aws.amazon.com/systems-manager/latest/userguide/what-is-systems-manager.html>
- Session Manager: <https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html>
