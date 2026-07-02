# AWS One-Hood Index

> Goal: keep every primary AWS learning artifact under one top-level folder, from beginner CLI/operator basics to MAANG-level architecture, GenAI, source deep dives, PDFs, and practice drills.

---

## Start Here

| Need | File |
|---|---|
| Main beginner-to-pro track | [AWS Interview Track Index](AWS-Interview-Track-Index.md) |
| AWS CLI and operator tooling | [AWS CLI and Developer Tooling](01-Foundations/AWS-CLI-Developer-Tooling-Gold-Sheet.md) |
| Long-form source notes | [Source Deep Dive Index](10-Source-Deep-Dive/README.md) |
| Generated PDFs | [PDFs](PDFs/) |
| NotebookLM exports | [NotebookLM](NotebookLM/) |

---

## Folder Map

| Folder | Purpose |
|---|---|
| `01-Foundations/` | CLI, EC2, VPC, ALB, Route 53, CloudFront |
| `02-Containers-Serverless/` | ECS, EKS, Lambda, API Gateway |
| `03-Storage-Database/` | S3, CloudFront storage patterns, RDS, Aurora, DynamoDB, ElastiCache |
| `04-Security-Identity/` | IAM, KMS, Secrets Manager, Cognito, WAF, Shield, EC2 access, SSM |
| `05-Messaging-Integration/` | SQS, SNS, EventBridge, Step Functions, Kinesis |
| `06-Observability-Operations/` | CloudWatch, X-Ray, CloudTrail, Config, Systems Manager |
| `07-Senior-Architecture/` | IaC, CI/CD, DR, landing zones, advanced networking, FinOps |
| `08-GenAI-Platform/` | Bedrock, RAG, Agents, SageMaker, LLMOps |
| `09-Practice-Upgrade/` | Recall bank, scenario drills, mock scripts, scoring rubrics, roadmaps |
| `10-Source-Deep-Dive/` | Older long-form AWS source notes consolidated from `Sources/AWS` |
| `PDFs/` | Generated AWS PDFs consolidated from `PDFs/AWS` |
| `NotebookLM/` | NotebookLM AWS exports consolidated from `NotebookLLM nots/AWS` |

---

## Recommended Path

```text
1. AWS CLI and Developer Tooling
2. Compute + Networking
3. Containers + Serverless
4. Storage + Databases
5. Security + Identity
6. Messaging + Observability
7. Senior Architecture + FinOps + DR
8. GenAI Platform
9. Practice Upgrade
10. Source Deep Dive for extra depth
```

Golden operator rule:

```text
Before every AWS write command or console change, verify account, region,
profile/role, target resource, expected change, cost/blast radius,
verification path, and rollback/cleanup path.
```
