# AWS 2-Week and 4-Week Mastery Roadmaps

> Track: AWS Interview Track — Practice Upgrade
> Goal: structured study plan to go from AWS fundamentals to MAANG-level interview readiness.

---

# Which Roadmap To Follow

| Situation | Roadmap |
|---|---|
| Interview in 2 weeks, some AWS experience | 2-Week Intensive |
| Interview in 4+ weeks, building from solid foundation | 4-Week Deep Dive |
| Less than 1 week | Focus only on: VPC, IAM, Lambda+SQS, RDS vs Aurora, DynamoDB, Multi-Region |

---

# 2-Week Intensive Roadmap

**Assumptions:**
- 2 hours study per day (weekdays)
- 4 hours study per day (weekends)
- Total: ~34 hours

**Priority order:** The items most likely to appear in MAANG interviews first.

---

## Week 1: Foundations + Core Services

### Day 1 (Monday) — 2 hours

**Focus: AWS CLI Orientation + VPC Networking**
- Read: [AWS-CLI-Developer-Tooling-Gold-Sheet.md](../01-Foundations/AWS-CLI-Developer-Tooling-Gold-Sheet.md) sections 0-4
- Read: [AWS-Networking-VPC-ALB-Route53-Gold-Sheet.md](../01-Foundations/AWS-Networking-VPC-ALB-Route53-Gold-Sheet.md)
- Run mentally or in sandbox: `aws --version`, `aws configure list`, `aws sts get-caller-identity`
- Draw the 3-tier VPC from memory (public/private-app/private-data)
- Answer aloud: CLI Q0.1-Q0.8 from Active Recall Bank
- Answer aloud: Q6-Q10 from Active Recall Bank
- Practice: explain security groups vs NACLs in 60 seconds

**Commit to memory:**
- Before every write command: account, region, profile, resource, verification, rollback
- SSO for humans, roles for workloads, OIDC for CI/CD
- 3 subnet tiers × 3 AZs minimum
- NACLs are stateless (ephemeral return ports must be explicitly allowed)
- VPC Gateway Endpoint for S3 and DynamoDB (free)
- NAT Gateway: egress only, $0.045/GB

---

### Day 2 (Tuesday) — 2 hours

**Focus: IAM and Security**
- Read: [AWS-IAM-Roles-Policies-Gold-Sheet.md](../04-Security-Identity/AWS-IAM-Roles-Policies-Gold-Sheet.md)
- Write out policy evaluation logic from memory (no peeking)
- Answer aloud: Q35-Q39 from Active Recall Bank
- Practice: explain cross-account S3 access in 2 minutes

**Commit to memory:**
- Evaluation order: explicit deny → SCP → resource policy → permission boundary → identity policy
- IRSA: pod-level IAM in EKS via OIDC
- PassRole: always scope to specific ARN
- SCPs only restrict, never grant

---

### Day 3 (Wednesday) — 2 hours

**Focus: Compute and Lambda**
- Read: [AWS-Compute-EC2-Auto-Scaling-Gold-Sheet.md](../01-Foundations/AWS-Compute-EC2-Auto-Scaling-Gold-Sheet.md) (first half)
- Read: [AWS-Lambda-API-Gateway-Serverless-Gold-Sheet.md](../02-Containers-Serverless/AWS-Lambda-API-Gateway-Serverless-Gold-Sheet.md) (full)
- Answer aloud: Q1-Q5, Q16-Q20 from Active Recall Bank
- Practice: cold start explanation in 90 seconds

**Commit to memory:**
- Lambda cold start: init phase (init code + runtime + handler init)
- Provisioned concurrency = pre-warm (cost), Reserved = cap (protection)
- API Gateway: HTTP API cheaper; REST API for caching/usage plans/validation
- ReportBatchItemFailures: partial batch success for SQS

---

### Day 4 (Thursday) — 2 hours

**Focus: Databases**
- Read: [AWS-RDS-Aurora-Database-Gold-Sheet.md](../03-Storage-Database/AWS-RDS-Aurora-Database-Gold-Sheet.md)
- Read: [AWS-DynamoDB-ElastiCache-Gold-Sheet.md](../03-Storage-Database/AWS-DynamoDB-ElastiCache-Gold-Sheet.md) (DynamoDB sections)
- Answer aloud: Q26-Q34 from Active Recall Bank
- Practice: design a DynamoDB table for an order system (access patterns: by orderId, by userId, by status)

**Commit to memory:**
- Aurora: shared storage, 6 copies/3 AZs, failover ~30s, RPO < 1s
- Multi-AZ standby: failover only, NOT for reads
- DynamoDB: hot partition = bad key; single-table = access patterns first
- RDS Proxy: Lambda → RDS connection pool

---

### Day 5 (Friday) — 2 hours

**Focus: Messaging**
- Read: [AWS-SQS-SNS-Messaging-Gold-Sheet.md](../05-Messaging-Integration/AWS-SQS-SNS-Messaging-Gold-Sheet.md)
- Draw SNS fan-out pattern from memory
- Answer aloud: Q45-Q49 from Active Recall Bank
- Practice: SQS DLQ configuration explanation in 60 seconds

**Commit to memory:**
- SQS Standard: at-least-once, unlimited throughput, idempotent consumers required
- SQS FIFO: exactly-once, ordered per MessageGroupId, 300 msg/sec
- SNS fan-out: one topic → multiple SQS queues → independent consumers
- Visibility timeout > max processing time

---

### Day 6 (Saturday) — 4 hours

**Morning (2 hours): Storage and Containers**
- Read: [AWS-S3-CloudFront-Storage-Gold-Sheet.md](../03-Storage-Database/AWS-S3-CloudFront-Storage-Gold-Sheet.md) (S3 sections)
- Read: [AWS-ECS-EKS-Container-Platform-Gold-Sheet.md](../02-Containers-Serverless/AWS-ECS-EKS-Container-Platform-Gold-Sheet.md) (ECS sections)
- Answer aloud: Q11-Q15, Q21-Q25

**Afternoon (2 hours): First Design Practice**
- Scenario 1 from Scenario Drill Bank: E-Commerce Order Processing
- Score yourself with the master rubric (target ≥ 20/30)
- Identify weakest dimension, read that section again

---

### Day 7 (Sunday) — 4 hours

**Morning (2 hours): Security + Observability**
- Read: [AWS-Secrets-KMS-Encryption-Gold-Sheet.md](../04-Security-Identity/AWS-Secrets-KMS-Encryption-Gold-Sheet.md)
- Read: [AWS-CloudWatch-XRay-Observability-Gold-Sheet.md](../06-Observability-Operations/AWS-CloudWatch-XRay-Observability-Gold-Sheet.md)
- Answer aloud: Q40-Q44, Q50-Q53

**Afternoon (2 hours): Mock Interview Round 1**
- Run Round 1: AWS Fundamentals (25 minutes, timed)
- Score all 6 questions with the master rubric
- Write down 3 specific areas to improve in Week 2

---

## Week 2: Advanced Topics + Practice

### Day 8 (Monday) — 2 hours

**Focus: Senior Architecture — DR and Multi-Region**
- Read: [AWS-Multi-Region-DR-Resilience-Gold-Sheet.md](../07-Senior-Architecture/AWS-Multi-Region-DR-Resilience-Gold-Sheet.md)
- Answer aloud: Q62-Q65 from Active Recall Bank
- Practice: explain 4 DR tiers in 2 minutes each

---

### Day 9 (Tuesday) — 2 hours

**Focus: Landing Zone and Governance**
- Read: [AWS-Landing-Zone-Governance-Gold-Sheet.md](../07-Senior-Architecture/AWS-Landing-Zone-Governance-Gold-Sheet.md)
- Answer aloud: Q66-Q69
- Practice: draw org structure (Management, Security, Infrastructure, Workloads OUs) from memory

---

### Day 10 (Wednesday) — 2 hours

**Focus: IaC and CI/CD + EventBridge**
- Read: [AWS-IaC-CICD-Release-Engineering-Gold-Sheet.md](../07-Senior-Architecture/AWS-IaC-CICD-Release-Engineering-Gold-Sheet.md)
- Read: [AWS-EventBridge-StepFunctions-Kinesis-Gold-Sheet.md](../05-Messaging-Integration/AWS-EventBridge-StepFunctions-Kinesis-Gold-Sheet.md) (EventBridge + Kinesis sections)
- Answer aloud: Q58-Q61, Q78

---

### Day 11 (Thursday) — 2 hours

**Focus: Cognito + WAF + Advanced Networking**
- Read: [AWS-Cognito-WAF-Shield-Gold-Sheet.md](../04-Security-Identity/AWS-Cognito-WAF-Shield-Gold-Sheet.md)
- Read: [AWS-Advanced-Networking-FinOps-Gold-Sheet.md](../07-Senior-Architecture/AWS-Advanced-Networking-FinOps-Gold-Sheet.md) (Transit Gateway + PrivateLink)
- Answer aloud: Q70-Q73

---

### Day 12 (Friday) — 2 hours

**Focus: GenAI**
- Read: [AWS-Bedrock-RAG-Agents-Gold-Sheet.md](../08-GenAI-Platform/AWS-Bedrock-RAG-Agents-Gold-Sheet.md)
- Answer aloud: Q74-Q77
- Practice: explain RAG in 90 seconds

---

### Day 13 (Saturday) — 4 hours

**Morning (2 hours): Design Practice**
- Scenario 2 (URL Shortener) from Scenario Drill Bank
- Scenario 9 (Monolith Migration) from Scenario Drill Bank
- Score both: target ≥ 22/30

**Afternoon (2 hours): Security Deep Dive**
- Run Round 3: Security Deep Dive (20 minutes, timed)
- Run Round 4: Behavioral (15 minutes)

---

### Day 14 (Sunday) — 4 hours

**Full Simulation Day**
- Morning (2 hours): Full Loop Simulation (45 minutes, healthcare platform)
  Then rest + review gaps

- Afternoon (2 hours):
  - All 78 active recall questions in order (review ❌ and ⚠️ only)
  - Quick revision notes review for 3 weakest topics
  - Finalize answer to: "Tell me about a system you designed on AWS"

---

# 4-Week Deep Dive Roadmap

---

## Week 1: Foundations (All Gold Sheet reading)

| Day | Topics |
|---|---|
| Mon | AWS CLI + Developer Tooling, VPC (full), EC2 + Auto Scaling |
| Tue | Lambda + API Gateway (full) |
| Wed | S3 + CloudFront |
| Thu | ECS + EKS (full) |
| Fri | RDS + Aurora |
| Sat | DynamoDB + ElastiCache |
| Sun | Active Recall: Q1-Q34, score and mark gaps |

---

## Week 2: Security + Integration + Observability

| Day | Topics |
|---|---|
| Mon | IAM (full) + Secrets Manager + KMS |
| Tue | Cognito + WAF + Shield |
| Wed | SQS + SNS (full) |
| Thu | EventBridge + Step Functions + Kinesis |
| Fri | CloudWatch + X-Ray (full) |
| Sat | CloudTrail + Config + Systems Manager |
| Sun | Active Recall: Q35-Q57 + 2 design scenarios |

---

## Week 3: Senior Architecture + GenAI

| Day | Topics |
|---|---|
| Mon | IaC + CI/CD + Release Engineering |
| Tue | Multi-Region + DR (full) |
| Wed | Landing Zone + Governance (full) |
| Thu | Advanced Networking + FinOps |
| Fri | Bedrock + RAG + Agents |
| Sat | SageMaker + LLMOps |
| Sun | Active Recall: Q58-Q78 + Round 2 Design Mock |

---

## Week 4: Practice Intensive

| Day | Focus |
|---|---|
| Mon | Mock Round 1 (fundamentals) + debrief |
| Tue | Mock Round 3 (security) + debrief |
| Wed | 3 design scenarios from drill bank |
| Thu | Mock Round 2 (full architecture design) |
| Fri | Behavioral prep + behavioral mock (Round 4) |
| Sat | Full 45-minute loop simulation + scoring |
| Sun | Revision day: only weak topics, quick notes, all recall questions |

---

# Daily Study Checklist

Before ending each study session:

- [ ] Ran or rehearsed `aws sts get-caller-identity` for the intended profile/account
- [ ] Read the Gold Sheet section fully (no skimming)
- [ ] Drew or wrote the architecture from memory
- [ ] Answered 5+ active recall questions aloud
- [ ] Scored myself on 1 practice scenario or question
- [ ] Wrote down 1 specific thing I learned or corrected today
- [ ] Reviewed yesterday's ❌ items

---

# Key Numbers To Memorize

| Number | Meaning |
|---|---|
| 30s | Aurora Multi-AZ failover time |
| < 1s | Aurora Global Database replication lag |
| 15 min | Lambda maximum execution timeout |
| 1,000 | Lambda default concurrent executions per region |
| 1 MB/s | Kinesis write throughput per shard |
| 2 MB/s | Kinesis read throughput per shard |
| 256 KB | SQS max message size |
| 14 days | SQS max retention |
| 300 | SQS FIFO messages per second (base) |
| $0.045/GB | NAT Gateway data processing cost |
| $0.40/month | Secrets Manager cost per secret |
| 11 nines | S3 durability (99.999999999%) |
| 128 KB | DynamoDB max item size limit (400 KB total, but 128 KB practical) |
| 400 KB | DynamoDB actual max item size |
| 6 copies | Aurora storage copies across 3 AZs |
| 15 replicas | Aurora maximum read replicas |
| 128 TB | Aurora maximum storage |

---

# Final Day Checklist (Day Before Interview)

- [ ] Review the AWS Interview Architecture Template (9 steps)
- [ ] Review CLI safety rule: profile, region, identity, target resource, verification, rollback
- [ ] Run through key numbers above without looking
- [ ] Review all items marked ❌ in Active Recall Bank
- [ ] Practice "tell me about yourself" + "why AWS" + "system you designed"
- [ ] Prepare 2-3 STAR stories about AWS architecture decisions
- [ ] Get 8 hours of sleep (cognitive performance > last-minute studying)

**The interview goal: clarity, structure, and showing how you think under ambiguity.**

---

# Quick Reference: AWS Architecture Template

For every design question, follow this structure:

```
1. CLARIFY: "Before I design, let me clarify requirements..."
   - Who are the users? How many?
   - What is the expected traffic? Peak RPS?
   - What are the latency and availability SLAs?
   - Any compliance requirements (HIPAA, PCI, SOC2)?

2. ESTIMATE: "At 100K users, 1% concurrent = 1K users, each doing 10 RPS = 10K RPS"

3. HIGH-LEVEL: "Here are the main components..."
   [3-4 boxes with arrows]

4. DEEP DIVE: "Let me walk through each component..."
   [Service choice + why + specific config]

5. FAILURE: "Here are the failure modes I've designed for..."
   [2-3 specific failures + mitigations]

6. SECURITY: "For security..."
   [IAM roles, encryption, network isolation, secrets]

7. OBSERVABILITY: "For monitoring..."
   [Specific metrics, alarms, traces]

8. COST: "For cost..."
   [1-2 specific optimizations]

9. TRADE-OFFS: "The main trade-off I made was..."
   [Honest comparison: what you chose vs alternative]
```
