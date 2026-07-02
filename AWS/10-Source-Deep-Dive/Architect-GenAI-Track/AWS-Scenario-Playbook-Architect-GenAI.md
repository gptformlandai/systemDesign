# AWS Architect + GenAI Production Scenario Playbook

> Goal: quickly answer "what do I do now?" for real AWS architect and GenAI production situations.

---

# Index

| Section | Focus |
|---|---|
| [0. How To Use This Playbook](#0-how-to-use-this-playbook) | How To Use This Playbook |
| [1. Scenario: A Developer Needs AWS Access](#1-scenario-a-developer-needs-aws-access) | Scenario: A Developer Needs AWS Access |
| [2. Scenario: S3 Bucket Accidentally Public](#2-scenario-s3-bucket-accidentally-public) | Scenario: S3 Bucket Accidentally Public |
| [3. Scenario: Production ECS Deployment Failing](#3-scenario-production-ecs-deployment-failing) | Scenario: Production ECS Deployment Failing |
| [4. Scenario: RDS CPU 95 Percent](#4-scenario-rds-cpu-95-percent) | Scenario: RDS CPU 95 Percent |
| [5. Scenario: NAT Gateway Bill Exploded](#5-scenario-nat-gateway-bill-exploded) | Scenario: NAT Gateway Bill Exploded |
| [6. Scenario: Need Private API Between Accounts](#6-scenario-need-private-api-between-accounts) | Scenario: Need Private API Between Accounts |
| [7. Scenario: Need Multi-Account Governance](#7-scenario-need-multi-account-governance) | Scenario: Need Multi-Account Governance |
| [8. Scenario: Need Multi-Region DR](#8-scenario-need-multi-region-dr) | Scenario: Need Multi-Region DR |
| [9. Scenario: Build Internal Docs Chatbot](#9-scenario-build-internal-docs-chatbot) | Scenario: Build Internal Docs Chatbot |
| [10. Scenario: RAG Gives Wrong Answers](#10-scenario-rag-gives-wrong-answers) | Scenario: RAG Gives Wrong Answers |
| [11. Scenario: GenAI App Leaks Sensitive Info](#11-scenario-genai-app-leaks-sensitive-info) | Scenario: GenAI App Leaks Sensitive Info |
| [12. Scenario: Bedrock Cost Spike](#12-scenario-bedrock-cost-spike) | Scenario: Bedrock Cost Spike |
| [13. Scenario: Agent Took Wrong Action](#13-scenario-agent-took-wrong-action) | Scenario: Agent Took Wrong Action |
| [14. Scenario: SageMaker Endpoint Too Expensive](#14-scenario-sagemaker-endpoint-too-expensive) | Scenario: SageMaker Endpoint Too Expensive |
| [15. Scenario: Need To Answer AWS Architect Interview](#15-scenario-need-to-answer-aws-architect-interview) | Scenario: Need To Answer AWS Architect Interview |
| [16. Scenario: Need To Answer GenAI Architect Interview](#16-scenario-need-to-answer-genai-architect-interview) | Scenario: Need To Answer GenAI Architect Interview |
| [17. Revision Notes](#17-revision-notes) | Revision Notes |

---

## 0. How To Use This Playbook

For every scenario:

```text
1. Identify blast radius.
2. Check the right console screens.
3. Contain the issue.
4. Verify recovery.
5. Add prevention.
6. Explain trade-offs clearly.
```

Default first checks:

```text
CloudWatch:
  metrics, logs, alarms

CloudTrail:
  who changed what

Security Hub / GuardDuty:
  security findings

Cost Explorer:
  unexpected cost

Service console:
  current resource health/config
```

---

## 1. Scenario: A Developer Needs AWS Access

### Situation

New engineer needs access to dev and read-only production.

### Console Path

```text
IAM Identity Center -> Users/Groups -> Add user to Developers group
IAM Identity Center -> AWS accounts -> Select dev account -> Assign group
IAM Identity Center -> AWS accounts -> Select prod account -> Assign read-only group
```

### Impact

```text
Dev assignment:
  engineer can build in dev.

Prod read-only:
  engineer can investigate without changing production.

Temporary credentials:
  no long-lived IAM user keys.
```

### Prevention

```text
Do not create IAM users for humans.
Use groups and permission sets.
Review access periodically.
```

---

## 2. Scenario: S3 Bucket Accidentally Public

### Console Path

```text
S3 -> Bucket -> Permissions -> Block Public Access
S3 -> Bucket -> Permissions -> Bucket policy
IAM Access Analyzer -> Findings
CloudTrail -> Event history -> filter bucket name
Macie -> Sensitive data findings
```

### Immediate Fix

```text
Enable Block Public Access.
Remove public "*" principals.
Check ACLs.
Classify exposed data.
Review CloudTrail access events.
Notify security/legal if sensitive data exposed.
```

### Prevention

```text
Control Tower/SCP guardrail.
AWS Config rule.
Security Hub finding workflow.
S3 bucket creation module with public access blocked by default.
```

---

## 3. Scenario: Production ECS Deployment Failing

### Console Path

```text
ECS -> Cluster -> Service -> Deployments
ECS -> Tasks -> Stopped tasks
CloudWatch Logs -> ECS log group
CloudWatch Metrics -> ALB Target 5xx / response time
ECR -> Image tag
```

### Immediate Fix

If new deployment is bad:

```text
ECS -> Service -> Update service -> previous task definition revision
```

CLI:

```bash
aws ecs update-service \
  --cluster myapp-prod \
  --service backend \
  --task-definition myapp-backend:42
```

### Prevention

```text
deployment circuit breaker
readiness health check
blue-green/canary for risky changes
backward-compatible DB migrations
rollback runbook
```

---

## 4. Scenario: RDS CPU 95 Percent

### Console Path

```text
RDS -> Databases -> Select DB -> Monitoring
CloudWatch -> Metrics -> RDS
Performance Insights -> Top SQL
CloudWatch Logs -> slow query logs if enabled
```

### Immediate Checks

```text
CPU
connections
read/write IOPS
locks
slow queries
recent deployment
traffic spike
cache hit ratio if available
```

### Fix Options

```text
add index for bad query
scale instance
add read replica for read-heavy workload
add cache for hot reads
fix connection leak
use RDS Proxy for Lambda connection storms
```

### Prevention

```text
Performance Insights enabled
slow query visibility
load testing
connection pool limits
alarms on CPU/connections/storage
```

---

## 5. Scenario: NAT Gateway Bill Exploded

### Console Path

```text
Cost Explorer -> Group by Usage type -> Look for NatGateway-Bytes
VPC -> NAT Gateways
VPC -> Route tables
VPC -> Endpoints
CloudWatch -> NAT Gateway metrics
```

### Fix

```text
Add S3 gateway endpoint.
Add DynamoDB gateway endpoint if used.
Add interface endpoints for ECR, CloudWatch Logs, Secrets Manager, STS if justified.
Reduce cross-AZ routing through NAT.
Review private subnet route tables.
```

### Prevention

```text
VPC endpoint baseline in network module.
Cost budgets.
Cost Explorer review by usage type.
Architecture review for chatty service paths.
```

---

## 6. Scenario: Need Private API Between Accounts

### Decision

If consumer only needs one service:

```text
Use PrivateLink.
```

If networks need broad routing:

```text
Use Transit Gateway.
```

### PrivateLink Console Path

```text
Producer:
  EC2 -> Load Balancers -> Create NLB
  VPC -> Endpoint services -> Create endpoint service
  Add allowed principals

Consumer:
  VPC -> Endpoints -> Create endpoint
  Select endpoint service
  Choose VPC/subnets/security group
```

### Impact

```text
Consumer reaches service privately.
No full VPC route sharing.
No CIDR overlap issue.
Producer controls allowed accounts.
```

---

## 7. Scenario: Need Multi-Account Governance

### Console Path

```text
Organizations -> Create OUs
Organizations -> Create accounts
Control Tower -> Set up landing zone
IAM Identity Center -> Permission sets
CloudTrail -> Organization trail
Security Hub / GuardDuty -> Delegated admin
```

### Target Layout

```text
management
security
log-archive
shared-network
app-dev
app-stage
app-prod
genai-sandbox
genai-prod
```

### Prevention

```text
No workloads in management account.
Central logs immutable.
SCPs for region/logging/public access guardrails.
Budgets per account.
```

---

## 8. Scenario: Need Multi-Region DR

### Console Path

```text
Route 53 -> Health checks / failover records
RDS/Aurora -> Read replica or global database
S3 -> Replication rules
AWS Backup -> Cross-region/cross-account backup
ECR -> Replicate images or push to secondary region
CloudFormation/Terraform -> Deploy secondary region stack
```

### Decide Strategy

```text
Backup/restore:
  low cost, slower recovery.

Pilot light:
  data and minimal infra ready.

Warm standby:
  scaled-down full stack ready.

Active-active:
  both regions serve traffic, highest complexity.
```

### Prevention

```text
DR drills.
RTO/RPO measured.
Failover and failback runbooks.
Data reconciliation plan.
```

---

## 9. Scenario: Build Internal Docs Chatbot

### Console Path

```text
S3 -> Create source document bucket
Macie -> Scan sensitive buckets
Bedrock -> Model access -> Enable approved models
Bedrock -> Knowledge Bases -> Create knowledge base
Choose S3 data source
Choose embedding model
Choose vector store
Bedrock -> Guardrails -> Create guardrail
Bedrock -> Prompt management -> Create prompt/version
```

### Architecture

```text
Frontend
  -> Backend with auth
  -> authorization-aware retrieval
  -> Bedrock Knowledge Base
  -> Bedrock model
  -> Guardrails
  -> citations and answer
```

### Prevention

```text
classify documents before ingestion
metadata filters
do not log sensitive prompts
return citations
eval prompt injection cases
budget alarms for Bedrock usage
```

---

## 10. Scenario: RAG Gives Wrong Answers

### Console Path

```text
Bedrock -> Knowledge Bases -> Test knowledge base
Bedrock -> Knowledge Bases -> Data sources -> Sync status
CloudWatch Logs -> app retrieval logs
S3 -> Source documents -> Check freshness
```

### Debug

```text
Is the document ingested?
Is chunking too large/small?
Is topK too low?
Are metadata filters too strict?
Is prompt ignoring citations/context?
Is the source document itself wrong?
```

### Fix

```text
improve chunking
add metadata
use reranking if available/appropriate
separate knowledge bases by domain
add "answer only from context" instruction
add eval case
```

---

## 11. Scenario: GenAI App Leaks Sensitive Info

### Console Path

```text
Bedrock -> Guardrails -> Sensitive information filters
Bedrock -> Knowledge Bases -> Data sources
S3 -> Bucket permissions
Macie -> Sensitive findings
CloudWatch Logs -> Request ID
CloudTrail -> S3/Bedrock API activity
```

### Immediate Fix

```text
disable problematic data source sync
tighten bucket access
remove sensitive docs from KB
enable/more strictly configure PII filters
add authorization-aware retrieval
rotate leaked secrets if any
```

### Prevention

```text
data classification gate before ingestion
metadata/ACL filters
guardrail eval suite
redacted logging
human review for sensitive domains
```

---

## 12. Scenario: Bedrock Cost Spike

### Console Path

```text
Cost Explorer -> Group by Service/Tag/Linked account
Bedrock -> Inference profiles -> usage metrics
CloudWatch -> app metrics for tokens/request
Budgets -> Create/update budget
```

### Debug

```text
Which app/team?
Which model?
Which prompt version?
Average input/output tokens?
Did topK/context size increase?
Did traffic spike?
Did retries loop?
```

### Fix

```text
token budget
truncate conversation history
reduce topK
summarize retrieved context
use cheaper model for simple tasks
cache deterministic answers
rate limit by user/team
budget alerts
```

---

## 13. Scenario: Agent Took Wrong Action

### Console Path

```text
Bedrock -> Agents -> Select agent -> Test traces
Lambda -> Function logs
CloudTrail -> API calls
CloudWatch Logs -> app request ID
IAM -> Agent/action role permissions
```

### Immediate Fix

```text
disable agent alias or action group
rollback to previous agent version
restrict IAM role
require confirmation for destructive action
add idempotency and approval checks
```

### Prevention

```text
least-privilege action APIs
validate user permission outside the model
separate read actions from write actions
human approval for high-risk operations
trace review and evals
```

---

## 14. Scenario: SageMaker Endpoint Too Expensive

### Console Path

```text
SageMaker AI -> Inference -> Endpoints -> Select endpoint
CloudWatch -> Invocation metrics
Cost Explorer -> SageMaker by usage type
Application Auto Scaling -> endpoint scaling policy
```

### Fix

```text
right-size instance type
enable autoscaling
use serverless inference for spiky workloads
use async inference for long jobs
use batch transform for offline jobs
delete unused endpoints
```

### Prevention

```text
endpoint owner tags
idle endpoint alarms
scheduled shutdown for dev
model deployment review
```

---

## 15. Scenario: Need To Answer AWS Architect Interview

Use this structure:

```text
1. Requirements:
   traffic, latency, data sensitivity, RTO/RPO, cost, users, regions

2. Entry:
   Route 53, CloudFront, ALB/API Gateway

3. Compute:
   ECS/EKS/Lambda/EC2 based on workload

4. Data:
   RDS/DynamoDB/S3/ElastiCache based on access pattern

5. Security:
   IAM roles, private subnets, KMS, Secrets Manager, WAF

6. Reliability:
   Multi-AZ, autoscaling, backups, rollback, DR

7. Async:
   SQS/SNS/EventBridge/Step Functions

8. Observability:
   CloudWatch, X-Ray, CloudTrail, dashboards, alarms

9. Cost:
   right-size, budgets, tags, storage classes, NAT/data transfer

10. Trade-offs:
   alternatives and why chosen
```

---

## 16. Scenario: Need To Answer GenAI Architect Interview

Use this structure:

```text
1. Use case:
   chat, search, summarization, action, workflow, custom model

2. Answer source:
   model knowledge, private docs, structured DB, tools/APIs

3. Architecture:
   Bedrock model, RAG/KB, agent/flow, backend API

4. Security:
   auth-aware retrieval, Guardrails, PII controls, private data

5. Evaluation:
   correctness, groundedness, prompt injection, expected refusals

6. Operations:
   latency, token cost, throttling, logs, feedback

7. Release:
   prompt/model/guardrail versions, canary, rollback

8. Trade-offs:
   Bedrock vs SageMaker, managed KB vs custom RAG, agent vs app tools
```

---

## 17. Revision Notes

- One-line summary: real AWS architecture is knowing where to click, what changes, how to verify, and how to prevent recurrence.
- Three keywords: contain, verify, prevent.
- One interview trap: naming services without operational flow.
- Memory trick: "Console finds the truth; IaC preserves the truth."

