# AWS Interview Notes - Part 4: DevOps + Monitoring + Architecture + Interview Q&A

> Covers: CI/CD on AWS, CloudWatch, Well-Architected Framework, architecture instincts, and 25 interview Q&As. This final part is meant to sharpen your production thinking and help you answer follow-up questions with senior-level depth.

---

# Table of Contents

1. [CI/CD on AWS](#1-cicd-on-aws)
2. [CloudWatch](#2-cloudwatch)
3. [Well-Architected Framework](#3-well-architected-framework)
4. [Architecture Thinking for Interviews](#4-architecture-thinking-for-interviews)
5. [25 High-Value Interview Q&As](#5-25-high-value-interview-qas)
6. [Final Revision Sheet](#6-final-revision-sheet)

---

# 1. CI/CD on AWS

## 1.1 What Interviewers Want

They want to know whether you can take code from commit to production safely.

A strong answer usually includes:

- source control trigger
- build/test stage
- security/static checks
- artifact creation
- deployment strategy
- rollback plan
- observability after release

## 1.2 Common AWS-Native Pipeline Components

- CodeCommit or GitHub as source
- CodeBuild for build/test
- CodeDeploy for deployments
- CodePipeline for orchestration
- ECR for container registry
- CloudFormation / CDK / Terraform for infrastructure

## 1.3 Typical Container Delivery Flow

```
Git push
  ->
pipeline trigger
  ->
unit/integration tests
  ->
security scan + quality gate
  ->
build Docker image
  ->
push to ECR
  ->
deploy to ECS/EKS
  ->
run smoke tests
  ->
monitor metrics and rollback if needed
```

## 1.4 Deployment Strategies

- Rolling update: replace gradually
- Blue-green: new environment then switch traffic
- Canary: send small percentage first

Interview-safe trade-off:

```
Blue-green reduces deployment risk but doubles environment cost temporarily.
Canary is excellent when you want to detect issues with real traffic gradually.
Rolling is simpler but may expose more users if a bad version passes health checks.
```

## 1.5 Infrastructure as Code

Interviewers expect this.

Why it matters:

- reproducibility
- reviewability
- reduced drift
- safer environment creation

Typical tools:

- CloudFormation
- CDK
- Terraform

## 1.6 Secrets in CI/CD

Never hardcode secrets in pipelines.

Use:

- Secrets Manager
- Parameter Store
- IAM roles

## 1.7 Strong CI/CD Answer

```
My production pipeline should be automated, auditable, and reversible. I want immutable artifacts, environment-specific configuration outside the artifact, progressive deployment where risk justifies it, and post-deploy alarms tied to rollback decisions.
```

---

# 2. CloudWatch

## 2.1 What It Is

CloudWatch is AWS's monitoring and observability service family for metrics, logs, alarms, dashboards, and events.

## 2.2 Core Capabilities

- Metrics
- Logs
- Alarms
- Dashboards
- Log Insights
- Events integration

## 2.3 What to Monitor

Infrastructure:

- CPU
- memory if custom metric
- disk
- network

Application:

- request latency
- error rate
- throughput
- queue depth
- downstream dependency failures

Business:

- orders per minute
- payment failures
- signup conversions

Senior candidates mention all three layers.

## 2.4 Alarms

Good alarms are:

- actionable
- tied to SLOs or meaningful thresholds
- not noisy

Examples:

- ALB 5xx spike
- Lambda errors and throttles
- SQS queue depth growing abnormally
- RDS CPU + connection exhaustion

## 2.5 Logs

CloudWatch Logs is useful for:

- centralized application logs
- Lambda execution logs
- ECS/EKS integrated logging paths
- searching with Logs Insights

## 2.6 Golden Observability Principle

If you cannot answer "what failed, where, and since when?", your monitoring is not mature enough.

## 2.7 Common Mistakes

- monitoring only CPU
- ignoring latency percentiles
- no correlation IDs
- too many useless alerts

---

# 3. X-Ray (Distributed Tracing)

## 3.1 What It Is

AWS X-Ray is a distributed tracing service that helps you analyze and debug applications, especially microservices.

## 3.2 What X-Ray Solves

```
Problem:
  In microservices, one user request touches many services.
  When something is slow or fails, which service is the problem?

Solution:
  X-Ray traces the entire request path across services,
  showing latency breakdown and error points.
```

## 3.3 Key Concepts

- **Trace**: end-to-end request path
- **Segment**: work done by one service
- **Subsegment**: downstream calls (DB, HTTP, AWS SDK)
- **Service Map**: visual graph of service dependencies
- **Annotations**: indexed metadata for filtering
- **Metadata**: non-indexed debug info

## 3.4 How to Enable

```
For Lambda:
  Enable Active Tracing in function config
  SDK auto-instruments AWS SDK calls

For ECS/EKS:
  Run X-Ray daemon as sidecar
  Instrument code with X-Ray SDK

For API Gateway:
  Enable tracing in stage settings
```

## 3.5 X-Ray Service Map Example

```
                                 ┌───────────┐
                                 │   RDS     │ ← avg 45ms
                            ┌──►│ (MySQL)  │
┌───────┐   ┌─────────┐   │    └───────────┘
│ Client│──►│ API GW  │──►│
└───────┘   └─────────┘   │    ┌───────────┐
       avg 5ms       avg 120ms │    │  Lambda   │ ← avg 80ms
                            └──►│  (Order)  │
                                 └─────┬─────┘
                                       │
                                       ▼
                                 ┌───────────┐
                                 │    SQS    │ ← avg 3ms
                                 └───────────┘
```

## 3.6 Interview Gold Answer

```
"For debugging latency in our microservices, I enable X-Ray tracing.
It shows me the service map and latency breakdown per segment.
If an API is slow, I can see whether the bottleneck is the Lambda code,
the database query, or a downstream service call."
```

---

# 4. CloudTrail (Audit)

## 4.1 What It Is

AWS CloudTrail records API calls made in your AWS account.

Every action—console, CLI, SDK, service—is logged.

## 4.2 Why It Matters

```
Security: Who deleted that S3 bucket?
Compliance: Prove that only authorized users accessed data.
Debugging: What changed before the system broke?
Forensics: Investigate security incidents.
```

## 4.3 Key Concepts

- **Management events**: control plane (CreateBucket, RunInstances)
- **Data events**: data plane (S3 GetObject, Lambda Invoke)
- **Trail**: configuration for what to log and where to store
- **Log files**: JSON in S3, can be queried with Athena

## 4.4 Best Practices

```
✓ Enable CloudTrail in all regions
✓ Enable log file integrity validation
✓ Store logs in a separate, locked-down S3 bucket
✓ Enable S3 data events for sensitive buckets
✓ Use Athena to query logs for investigations
✓ Set up CloudWatch alarms for suspicious activity
```

## 4.5 CloudTrail + Athena Pattern

```sql
-- Who deleted objects from sensitive bucket in last 7 days?
SELECT eventTime, userIdentity.arn, requestParameters
FROM cloudtrail_logs
WHERE eventName = 'DeleteObject'
  AND requestParameters LIKE '%sensitive-bucket%'
  AND eventTime > date_add('day', -7, current_date)
```

## 4.6 Interview Gold Answer

```
"CloudTrail is our audit log for AWS. I ensure it's enabled in all regions,
with logs stored in a separate account's S3 bucket for tamper resistance.
For investigations, I query logs with Athena. For real-time alerting on
suspicious activity, I set up CloudWatch Events rules."
```

---

# 5. Well-Architected Framework

AWS Well-Architected Framework pillars:

1. Operational Excellence
2. Security
3. Reliability
4. Performance Efficiency
5. Cost Optimization
6. Sustainability

These names alone are not enough. You need to explain them.

## 3.1 Operational Excellence

- automate operations
- improve via feedback loops
- make changes small and reversible
- learn from incidents

## 3.2 Security

- least privilege
- traceability
- data protection
- secure all layers

## 3.3 Reliability

- recover from failure
- scale to meet demand
- test recovery procedures
- remove single points of failure

## 3.4 Performance Efficiency

- pick the right resource types
- use managed services where appropriate
- monitor and evolve

## 3.5 Cost Optimization

- right-size workloads
- use pricing models wisely
- measure cost by architecture component
- eliminate idle overprovisioning

## 3.6 Sustainability

- efficient resource usage
- reduce waste
- optimize demand and architecture footprint

## 3.7 Strong Interview Pattern

When given any architecture, evaluate it across at least:

- availability
- security
- cost
- operability
- scaling

That is effectively Well-Architected thinking.

---

# 4. Architecture Thinking for Interviews

## 4.1 Start with Requirements

Ask or state:

- expected traffic
- latency target
- availability target
- read/write pattern
- data sensitivity
- regional scope
- budget constraints

## 4.2 Then Design Along These Axes

- compute model
- data model
- network path
- failure handling
- observability
- security
- cost

## 4.3 A Good AWS Answer Sounds Like This

```
For this workload I would keep the stateless API tier behind an ALB across multiple AZs, store transactional data in Multi-AZ RDS, use Redis for hot reads, publish async work to SQS, emit metrics and structured logs to CloudWatch, and keep least-privilege IAM plus KMS-backed encryption enabled by default.
```

## 4.4 What Separates Mid-Level from Senior Answers

Mid-level answer:

- names services

Senior answer:

- names trade-offs
- describes failure modes
- describes security boundaries
- describes scaling behavior
- describes operational implications

---

# 5. 25 High-Value Interview Q&As

## 1. When would you choose ECS over EKS?

Choose ECS when the main requirement is running containers on AWS with lower operational complexity. Choose EKS when Kubernetes APIs, tooling, portability, or platform standardization matter enough to justify the added operational burden.

## 2. What is the difference between Multi-AZ and read replicas in RDS?

Multi-AZ is primarily for high availability and failover. Read replicas are primarily for read scaling and offloading read traffic.

## 3. Why use private subnets for application servers?

Private subnets reduce direct exposure to the internet. Public traffic should terminate at controlled entry points such as ALB or API Gateway, while app servers stay reachable only through internal paths.

## 4. When would Lambda be a poor choice?

For long-running, stateful, low-latency-sensitive, or highly predictable heavy-throughput workloads where container or instance-based compute is operationally and economically better.

## 5. What is the difference between ALB and NLB?

ALB is Layer 7 and supports HTTP-aware routing such as host/path rules. NLB is Layer 4 and is better for TCP/UDP, very high performance, and certain static-IP use cases.

## 6. Why is S3 not suitable as a database?

S3 is object storage, not a transactional query engine. It lacks relational semantics, low-latency row-level updates, and database-style indexing/query behavior.

## 7. When do you choose DynamoDB over RDS?

When access patterns are known, scale is very high, low-latency key-based access matters, and relational joins are not central to the problem.

## 8. What is the danger of a bad DynamoDB partition key?

It creates hot partitions, uneven traffic distribution, throttling, and poor scale behavior.

## 9. Why use ElastiCache?

To reduce database read pressure, improve latency for hot data, and absorb bursty traffic. It should complement, not replace, good data modeling.

## 10. What does least privilege mean in IAM?

Grant only the minimum actions on the minimum resources required for a principal to do its job, and nothing more.

## 11. Why are IAM roles preferred over access keys?

Roles avoid hardcoded long-lived credentials, improve rotation posture, and fit AWS's temporary-credential model for workloads.

## 12. What is envelope encryption in KMS?

Data is encrypted with a data key, and the data key is encrypted with a KMS-managed master key. This scales better than using a master key directly for all data operations.

## 13. What problem does SQS solve?

It decouples producers and consumers, buffers spikes, improves resilience, and allows asynchronous processing.

## 14. Why must SQS consumers be idempotent?

Because standard SQS provides at-least-once delivery, so duplicates can occur and the consumer must handle them safely.

## 15. When is SNS plus SQS a strong pattern?

When one event must fan out to multiple independent consumers and each consumer needs isolated retry and failure handling.

## 16. When is EventBridge better than SNS?

When event routing depends on event content or you want an event-bus model with rule-based dispatch to multiple targets.

## 17. When should Step Functions be used?

When the workflow has multiple steps, branching, retries, compensation, or long-running orchestration that should be explicit rather than buried inside application code.

## 18. How do you design a secure file-upload system on AWS?

Use Cognito or your auth layer for identity, issue pre-signed S3 URLs, keep the bucket private, encrypt with KMS-backed settings, store metadata separately, and serve downloads through controlled access such as CloudFront if needed.

## 19. How would you make a web application highly available in one region?

Deploy compute across multiple AZs behind a load balancer, keep databases in Multi-AZ mode, store static assets in S3, use health checks and autoscaling, and avoid single-instance dependencies.

## 20. What would you monitor for a production API?

Latency, error rate, throughput, saturation metrics, downstream dependency health, queue depth, and key business metrics.

## 21. What is a good rollback strategy for production deployments?

Use immutable artifacts, health checks, deployment stages like canary or blue-green where risk justifies it, and automatic or manual rollback triggered by verified failure signals.

## 22. Why is Infrastructure as Code important?

It makes infrastructure reproducible, reviewable, versioned, and less prone to manual drift or undocumented changes.

## 23. How would you reduce AWS cost without harming reliability?

Right-size compute, use Savings Plans or Reserved capacity where stable, use Spot for fault-tolerant workloads, optimize storage classes, remove idle resources, and cache or tune before brute-force scaling.

## 24. How would you answer "design a scalable notifications system on AWS"?

I would accept requests through API Gateway or app services, publish events to SNS or EventBridge, buffer provider-specific work in SQS queues, process asynchronously with Lambda or containers, persist delivery state in a database, and monitor queue depth, retries, and provider failure rates.

## 25. What is the best way to answer AWS architecture questions in interviews?

Start from requirements, choose services based on workload characteristics, explain trade-offs, call out failure handling and security boundaries, and show how you would observe and operate the system in production.

---

# 6. Final Revision Sheet

## Core Architecture Defaults

- public entry through `ALB` or `API Gateway`
- compute in private subnets
- data tier private and highly available
- async work through `SQS` or event-based routing
- encryption with `KMS`
- least privilege with `IAM`
- metrics/logs/alarms in `CloudWatch`
- distributed tracing with `X-Ray`
- audit trail with `CloudTrail`

## What Interviewers Keep Testing

- Can you compare services instead of just naming them?
- Do you understand failure modes?
- Do you know when to use managed services?
- Can you protect the system properly?
- Can you justify cost/performance trade-offs?
- Can you debug distributed systems? (X-Ray)
- Can you answer "who did what when?" (CloudTrail)

## Decision Quick Reference

```
Debug latency in microservices?        → X-Ray
Audit who changed what?                → CloudTrail  
Application metrics and logs?          → CloudWatch
Alerts on thresholds?                  → CloudWatch Alarms
Visual service dependency map?         → X-Ray Service Map
Query audit logs?                      → Athena on CloudTrail S3
```

## Final Gold Standard Sentence

```
The best AWS architecture is not the one with the most services;
it is the one that meets the workload's reliability, security,
scalability, operability, and cost goals with the least unnecessary
complexity—and includes observability (CloudWatch, X-Ray) and
auditability (CloudTrail) from day one.
```

