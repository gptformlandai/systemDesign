# 14. Datadog Security: CSPM, Cloud SIEM, Cloud Integrations (AWS, GCP, Azure)

## Goal

Understand Datadog's security capabilities: Cloud SIEM for threat detection, CSPM for compliance posture, cloud integrations, and audit trail.

---

## Datadog Security Products

| Product | Purpose |
|---|---|
| Cloud SIEM | Real-time threat detection from logs and cloud events |
| CSPM | Cloud Security Posture Management: compliance and misconfiguration detection |
| Cloud Workload Security (CWS) | Runtime threat detection at OS/container level |
| Application Vulnerability Management | Code-level CVE detection in running services |
| Sensitive Data Scanner | PII and secrets detection in logs |

---

## Cloud SIEM: Threat Detection

Cloud SIEM analyzes logs and cloud activity events to detect threats using detection rules.

### How It Works

```text
Log source (CloudTrail, GuardDuty, Okta, K8s audit, application logs)
  -> Datadog log pipeline
  -> SIEM detection rules evaluate patterns
  -> Signal generated (like an alert)
  -> Triage in Security Signals view
```

### Detection Rule Types

| Rule Type | Description |
|---|---|
| Log Detection | Pattern match on log attributes (e.g., brute force login) |
| Workload Security | File/process/network events from kernel agent |
| Cloud Configuration | Misconfigured cloud resource (CSPM) |
| Application Security | Code-level attack (RASP, WAF) |

### Example Detection Rules

```text
# Brute force login attempt.
Rule: More than 10 failed logins from same IP in 5 minutes
Query: @evt.name:LoginFailure source:okta
Aggregation: count by {network.client.ip} > 10 in 5 min

# Root access in container.
Rule: Process spawned by container with uid=0 that is a shell
Query: @process.user.name:root @process.name:(bash OR sh OR /bin/sh)

# IAM privilege escalation (AWS CloudTrail).
Rule: AttachRolePolicy or PutRolePolicy called
Query: source:cloudtrail @evt.name:(AttachRolePolicy OR PutRolePolicy)
```

---

## CSPM: Cloud Posture Management

CSPM continuously evaluates cloud resource configurations against security frameworks.

### Supported Frameworks

- CIS AWS Foundations Benchmark
- CIS GCP Benchmark
- CIS Azure Benchmark
- HIPAA
- PCI-DSS
- SOC 2
- GDPR

### Example Findings

```text
Finding: S3 bucket allows public read access
Severity: Critical
Resource: arn:aws:s3:::my-production-bucket
Remediation: Enable Block Public Access in S3 bucket settings

Finding: Security group allows unrestricted SSH (0.0.0.0/0 on port 22)
Severity: High
Resource: sg-0abc123def456789

Finding: CloudTrail not enabled in us-west-2
Severity: Medium
Resource: AWS account 123456789012
```

---

## AWS Integration

### IAM Role Setup (Recommended Over Access Keys)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:GetMetricData",
        "cloudwatch:ListMetrics",
        "ec2:DescribeInstances",
        "rds:DescribeDBInstances",
        "s3:GetBucketLocation",
        "s3:ListAllMyBuckets",
        "cloudtrail:GetTrailStatus",
        "cloudtrail:DescribeTrails",
        "logs:DescribeLogGroups"
      ],
      "Resource": "*"
    }
  ]
}
```

### What AWS Integration Provides

```text
CloudWatch metrics -> Datadog metrics (EC2, RDS, ELB, Lambda, SQS, SNS)
CloudTrail events -> Cloud SIEM (API calls, IAM changes)
Lambda logs -> Datadog logs (via Forwarder)
Resource inventory -> Datadog infrastructure list (host map)
Cost data -> Datadog estimated cloud spend (via Cost Management integration)
```

### Enable From Datadog UI

```text
Integrations -> AWS -> Add Account
  - Account ID: 123456789012
  - IAM Role ARN: arn:aws:iam::123456789012:role/DatadogIntegrationRole
  - Regions: us-east-1, us-west-2
  - Services: EC2, RDS, Lambda, ELB, S3, SQS, SNS, CloudFront
```

---

## GCP Integration

```text
Integrations -> GCP -> Add Project
  - Project ID: my-project-id
  - Service Account: datadog@my-project.iam.gserviceaccount.com
  - Roles required: Viewer, Monitoring Viewer, Compute Viewer

Provides:
  - Google Cloud metrics (GCE, GKE, Cloud SQL, Pub/Sub, GCS)
  - Cloud Audit Logs -> SIEM
```

---

## Azure Integration

```text
Integrations -> Azure -> Add Subscription
  - Subscription ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  - App Registration: datadog-monitoring (Client ID + Client Secret)
  - Permissions: Monitoring Reader on subscription

Provides:
  - Azure Monitor metrics (VMs, AKS, App Service, SQL Database)
  - Activity Logs -> SIEM
```

---

## Sensitive Data Scanner

Scan logs for PII and secrets before they are indexed:

```text
Security -> Sensitive Data Scanner -> Add Scanning Rule

Rule: Credit Card Number
  Pattern: (built-in Luhn algorithm detection)
  Action: Redact (replace with ****)
  Applies to: all logs

Rule: AWS Secret Key
  Pattern: AKIA[0-9A-Z]{16}
  Action: Hash (one-way hash for investigation without exposure)
  
Rule: Email Addresses
  Pattern: (built-in email regex)
  Action: Partially Redact (keep first char and domain: j****@example.com)
```

---

## Audit Trail

Audit Trail records all Datadog user and API actions for compliance:

```text
Security -> Audit Trail

Records:
  - Dashboard create/update/delete
  - Monitor create/update/delete
  - API key create/revoke
  - User login/logout
  - Role changes
  - Log pipeline changes

Retention: 90 days (configurable with archive)
Export: CSV or to log archive
```

---

## Interview Sound Bite

Datadog Security consists of Cloud SIEM (log-based threat detection with pattern rules), CSPM (compliance posture against CIS/HIPAA/PCI benchmarks), Cloud Workload Security (runtime OS/container detection), and Sensitive Data Scanner (PII redaction in logs). Cloud integrations (AWS via IAM role, GCP via service account, Azure via app registration) pull CloudWatch/Azure Monitor/Cloud Monitoring metrics and forward audit logs to SIEM. Audit Trail provides tamper-evident records of all Datadog platform actions for SOC 2 compliance.
