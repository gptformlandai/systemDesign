# AWS Architect 03: Resilience, Disaster Recovery, and Multi-Region Real-World Guide

> Goal: design systems that survive instance failure, AZ failure, regional issues, bad deployments, data corruption, and dependency outages with clear RTO/RPO trade-offs.

---

# Index

| Section | Focus |
|---|---|
| [0. Real Situation](#0-real-situation) | Real Situation |
| [1. Key Definitions](#1-key-definitions) | Key Definitions |
| [2. Resilience Layers](#2-resilience-layers) | Resilience Layers |
| [3. DR Strategy Decision Matrix](#3-dr-strategy-decision-matrix) | DR Strategy Decision Matrix |
| [4. Console Build: Multi-AZ RDS](#4-console-build-multi-az-rds) | Console Build: Multi-AZ RDS |
| [5. Console Build: RDS Read Replica](#5-console-build-rds-read-replica) | Console Build: RDS Read Replica |
| [6. Console Build: S3 Versioning And Replication](#6-console-build-s3-versioning-and-replication) | Console Build: S3 Versioning And Replication |
| [7. Console Build: Route 53 Failover](#7-console-build-route-53-failover) | Console Build: Route 53 Failover |
| [8. Console Build: DynamoDB Global Tables](#8-console-build-dynamodb-global-tables) | Console Build: DynamoDB Global Tables |
| [9. Console Build: Aurora Global Database](#9-console-build-aurora-global-database) | Console Build: Aurora Global Database |
| [10. Console Build: AWS Backup](#10-console-build-aws-backup) | Console Build: AWS Backup |
| [11. GenAI Resilience Scenario](#11-genai-resilience-scenario) | GenAI Resilience Scenario |
| [12. DR Runbook: Region Failure](#12-dr-runbook-region-failure) | DR Runbook: Region Failure |
| [13. Failure Modes](#13-failure-modes) | Failure Modes |
| [14. Production Checklist](#14-production-checklist) | Production Checklist |
| [15. Interview Question](#15-interview-question) | Interview Question |
| [16. Strong Answer](#16-strong-answer) | Strong Answer |
| [17. Revision Notes](#17-revision-notes) | Revision Notes |
| [18. Official Source Notes](#18-official-source-notes) | Official Source Notes |

---

## 0. Real Situation

Your production application is down.

Possible causes:

```text
one ECS task crashed
one EC2 instance died
one AZ has networking issues
RDS primary failed
bad deployment caused 500s
someone deleted data
entire region is impaired
Bedrock model endpoint is throttling
```

A beginner says:

```text
Use Multi-AZ.
```

An architect asks:

```text
What failed?
What is the RTO?
What is the RPO?
Is this HA, backup, or disaster recovery?
What should automatically fail over?
What needs manual approval?
What is the cost of standby capacity?
```

---

## 1. Key Definitions

```text
Availability:
  System stays usable during expected failures.

Backup:
  Copy of data used to restore after loss/corruption.

HA:
  High availability inside normal operating region, usually Multi-AZ.

DR:
  Disaster recovery when primary environment is unusable.

RTO:
  Recovery time objective. How long can we be down?

RPO:
  Recovery point objective. How much data can we lose?
```

Memory:

```text
RTO = time.
RPO = data.
```

---

## 2. Resilience Layers

```text
Process crash:
  ECS service replaces task, ASG replaces instance, Lambda retries.

Instance failure:
  Auto Scaling / ECS / EKS reschedules workload.

AZ failure:
  Multi-AZ compute + database failover.

Bad deployment:
  health checks, rollback, blue-green/canary.

Data corruption:
  backups, PITR, versioning.

Region failure:
  pilot light, warm standby, active-active.

Model/service throttling:
  retries, backoff, fallback model, cross-region inference profile.
```

---

## 3. DR Strategy Decision Matrix

| Strategy | RTO | RPO | Cost | When To Use |
|---|---:|---:|---:|---|
| Backup and restore | Hours to days | Minutes to hours | Low | Non-critical workloads |
| Pilot light | Tens of minutes to hours | Minutes | Medium-low | Critical data, minimal standby compute |
| Warm standby | Minutes | Seconds to minutes | Medium-high | Important apps needing fast recovery |
| Active-active | Seconds to minutes | Seconds | High | Global, mission-critical systems |

Architect rule:

```text
Do not build active-active because it sounds impressive.
Build it only when the business RTO/RPO justifies complexity and cost.
```

---

## 4. Console Build: Multi-AZ RDS

### Console Path

```text
AWS Console -> Search "RDS" -> Databases -> Create database
```

Choose:

```text
Production template
Multi-AZ DB instance or Multi-AZ DB cluster
Private subnets
Encryption enabled
Automated backups enabled
Deletion protection enabled
```

### What Each Click Changes

```text
Production template:
  presets stronger availability and backup defaults.

Multi-AZ:
  creates standby/failover capacity across AZs.

Private subnets:
  removes direct internet exposure.

Encryption:
  protects data at rest with KMS.

Automated backups:
  enables point-in-time recovery within retention window.

Deletion protection:
  prevents accidental database deletion.
```

### What Can Go Wrong

Multi-AZ is not read scaling by itself for classic RDS.

It is primarily:

```text
availability and failover
```

For read scaling:

```text
use read replicas or Aurora reader endpoints.
```

---

## 5. Console Build: RDS Read Replica

### Console Path

```text
RDS -> Databases -> Select DB -> Actions -> Create read replica
```

Choose:

```text
same region for read scaling
different region for DR/read-locality
instance class
storage
network
encryption
```

### What This Click Changes

It creates an asynchronously replicated copy of the primary database.

### Why It Matters

Useful for:

- read traffic offload
- reporting workloads
- regional DR building block

### What Can Go Wrong

Replication lag.

If app reads from replica immediately after write:

```text
user may not see their own latest write.
```

Architect move:

```text
Send strongly consistent reads to primary.
Use replicas for read-only/reporting workloads.
Monitor replica lag.
```

---

## 6. Console Build: S3 Versioning And Replication

### Console Path

```text
S3 -> Buckets -> Select bucket -> Properties -> Bucket Versioning -> Enable
```

### What This Click Changes

S3 keeps old versions of objects when overwritten or deleted.

### Why It Matters

Protects against:

- accidental overwrite
- accidental delete
- some application bugs

Next:

```text
S3 -> Management -> Replication rules -> Create replication rule
```

Choose:

```text
destination bucket in another region/account
IAM role for replication
replicate all objects or prefix
replicate delete markers based on policy
```

### What Each Click Changes

```text
Destination bucket:
  where DR copy lives.

Replication IAM role:
  grants S3 permission to copy objects.

Prefix:
  limits replication to important data.

Delete marker replication:
  controls whether deletes replicate.
```

### What Can Go Wrong

Replicating deletes can turn a mistake into a multi-region mistake.

Architect move:

```text
Use versioning.
Be careful with delete marker replication.
Consider separate backup retention.
```

---

## 7. Console Build: Route 53 Failover

### Console Path

```text
Route 53 -> Hosted zones -> Select domain -> Create record
```

For primary:

```text
Routing policy: Failover
Failover record type: Primary
Alias target: ALB in primary region
Health check: primary ALB/API health check
```

For secondary:

```text
Routing policy: Failover
Failover record type: Secondary
Alias target: ALB/API in DR region
Health check: optional or evaluate target health
```

### What Each Click Changes

```text
Failover policy:
  Route 53 returns primary endpoint when healthy, secondary when unhealthy.

Health check:
  defines what "healthy" means.

Alias target:
  maps DNS name to AWS resource without hardcoded IP.
```

### What Can Go Wrong

Health check tests the wrong thing.

Bad:

```text
Health check returns 200 from /health even when DB is down.
```

Better:

```text
Use shallow health for load balancer target health.
Use deeper canary/alarms for business health.
Do not make health checks so deep that they cause false failovers.
```

---

## 8. Console Build: DynamoDB Global Tables

### Console Path

```text
DynamoDB -> Tables -> Select table -> Global tables -> Create replica
```

Choose:

```text
replica region
capacity mode
encryption settings
```

### What This Click Changes

It creates a multi-region, active-active DynamoDB table replica.

### Why It Matters

Useful for:

- global low-latency reads/writes
- regional resilience
- active-active serverless apps

### What Can Go Wrong

Conflict handling.

If two regions update the same item at nearly the same time:

```text
last writer wins behavior may surprise business logic.
```

Architect move:

```text
Design item ownership, conflict strategy, or region affinity.
```

---

## 9. Console Build: Aurora Global Database

### Console Path

```text
RDS -> Databases -> Create database -> Aurora -> Add AWS Region
```

or for existing cluster:

```text
RDS -> Databases -> Select Aurora cluster -> Actions -> Add AWS Region
```

### What This Click Changes

It creates a global Aurora setup with a primary region and secondary read-only region.

### Why It Matters

Useful when:

- relational database needed
- low-latency global reads needed
- faster regional recovery needed

### What Can Go Wrong

Secondary region is not automatically write-primary unless promoted.

Architect move:

```text
Document failover/promotion runbook.
Test it.
Understand write downtime and application DNS changes.
```

---

## 10. Console Build: AWS Backup

### Console Path

```text
AWS Console -> Search "AWS Backup" -> Backup plans -> Create backup plan
```

Choose:

```text
backup frequency
retention
backup vault
resources by tags
cross-region copy
cross-account copy
```

### What Each Click Changes

```text
Frequency:
  how often recovery points are created.

Retention:
  how long backups are kept.

Vault:
  where backups are stored and protected.

Tags:
  which resources automatically enter backup plan.

Cross-region/cross-account copy:
  protects against account/region-level disaster.
```

### What Can Go Wrong

Backups that are never restored are assumptions, not guarantees.

Architect move:

```text
Run scheduled restore tests.
Measure actual RTO.
Validate application works with restored data.
```

---

## 11. GenAI Resilience Scenario

### Situation

Your customer support chatbot uses Bedrock. During peak hours, the model gets throttled or latency spikes.

### Architecture

```text
API Gateway / ALB
  -> backend service
  -> Bedrock inference profile
  -> primary model
  -> fallback model if allowed
  -> S3/OpenSearch/Aurora vector store for RAG
  -> CloudWatch metrics and logs
```

### Console Path

```text
Bedrock -> Inference profiles -> Create application inference profile
```

Choose:

```text
model
single-region or cross-region profile
tags for cost allocation
CloudWatch logging where appropriate
```

### What This Changes

Inference profiles help:

- track model usage
- tag costs
- route to multiple regions for supported cross-region inference
- use a stable resource reference in applications

### App-Level Controls

```text
exponential backoff
client timeout
request queue for non-interactive jobs
fallback response
fallback model for lower-criticality tasks
human handoff for support cases
token budget per user/session
```

---

## 12. DR Runbook: Region Failure

### Before Failure

You must already have:

- infrastructure templates for secondary region
- replicated data
- DNS failover records
- secrets and KMS strategy
- container images available in secondary region
- tested runbook
- clear owner approval

### During Failure

```text
1. Declare incident.
2. Confirm region impact and business impact.
3. Freeze non-essential deployments.
4. Promote database/read replica if needed.
5. Scale secondary compute.
6. Shift DNS or Route 53 failover.
7. Validate app health.
8. Monitor error rate, latency, data consistency.
9. Communicate status.
```

### After Recovery

```text
1. Reconcile data.
2. Decide failback plan.
3. Review RTO/RPO achieved.
4. Update runbook.
5. Fix automation gaps.
```

---

## 13. Failure Modes

### Failure Mode 1: Backups Exist But Restore Fails

Cause:

```text
No restore testing.
Missing KMS permissions.
Application config assumes old endpoint.
```

Fix:

```text
Automated restore drills.
Document endpoint changes.
Validate KMS/key policies.
```

### Failure Mode 2: Multi-AZ But Single NAT Gateway

Cause:

```text
App spans AZs but outbound path depends on one AZ's NAT Gateway.
```

Fix:

```text
NAT Gateway per AZ for stronger resilience.
Route private subnets to same-AZ NAT.
Use VPC endpoints for AWS services where possible.
```

### Failure Mode 3: Active-Active Data Conflict

Cause:

```text
Both regions accept writes for same entity.
```

Fix:

```text
Region ownership, conflict resolution, idempotency keys, or single-writer model.
```

### Failure Mode 4: DR Environment Is Not Updated

Cause:

```text
Primary evolved, DR templates/config did not.
```

Fix:

```text
Deploy infrastructure through same pipeline to both regions.
Run periodic DR game days.
```

---

## 14. Production Checklist

- RTO/RPO documented per workload
- Multi-AZ compute for production
- data tier Multi-AZ where required
- backups enabled and restore-tested
- S3 versioning for critical buckets
- cross-account/cross-region backup for critical data
- Route 53 failover strategy tested
- health checks test meaningful availability
- deployment rollback tested
- runbooks exist for DB failover and region failover
- GenAI apps have fallback behavior for throttling/latency
- CloudWatch alarms for error rate, latency, saturation, queue depth
- DR game days performed

---

## 15. Interview Question

> Design a highly available and disaster-recoverable AWS architecture for a payment API.

---

## 16. Strong Answer

I would first define RTO and RPO because DR cost and complexity depend on them. For normal high availability, I would run the API across multiple AZs behind an ALB, keep ECS/EKS tasks in private subnets, use RDS/Aurora Multi-AZ, and enable autoscaling and health checks.

For data protection, I would enable automated backups, PITR where available, S3 versioning for object data, and cross-account backup copies for critical data. For regional DR, I would choose pilot light, warm standby, or active-active depending on business RTO/RPO. A payment system usually needs tight recovery, but active-active requires careful data consistency design.

I would use Route 53 failover or controlled DNS shift, test DB promotion, run DR drills, and monitor error rate, latency, queue depth, and payment reconciliation metrics.

---

## 17. Revision Notes

- One-line summary: resilience is designed by failure type, RTO, and RPO.
- Three keywords: Multi-AZ, backup, failover.
- One interview trap: confusing Multi-AZ with multi-region DR.
- Memory trick: "HA keeps running; backup restores; DR relocates."

---

## 18. Official Source Notes

- AWS Well-Architected explains trade-offs for reliable, secure, efficient, cost-effective workloads: <https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html>
- Bedrock inference profiles can track usage/cost and support cross-region inference routing for model invocation: <https://docs.aws.amazon.com/bedrock/latest/userguide/inference-profiles.html>

