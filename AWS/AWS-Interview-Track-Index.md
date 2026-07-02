# AWS Interview Track Index

> Goal: build complete AWS fluency from service basics through MAANG-level architecture design, production operations, and GenAI platform engineering.

---

## How To Use This Track

Do not learn AWS as a list of services.

Learn it as one production system:

```text
compute runtime
-> container orchestration or serverless
-> storage and database
-> security and identity
-> async messaging and event processing
-> observability and operations
-> architecture and governance
-> cost control
-> GenAI capabilities
```

For every design question, the senior answer covers:

1. What workload, users, scale, latency, and compliance requirements?
2. Entry path: Route 53 → CloudFront → ALB → API Gateway.
3. Compute: EC2 / ECS / EKS / Lambda — why this choice?
4. Data: which storage service for which access pattern?
5. Security: IAM roles, KMS, Secrets Manager, WAF.
6. Async: SQS / SNS / EventBridge / Step Functions / Kinesis.
7. Operations: CloudWatch, X-Ray, CloudTrail, alarms, dashboards.
8. Cost: right-sizing, Reserved/Spot, S3 lifecycle, NAT cost, budgets.
9. Trade-offs: what you rejected and why.

---

## Study Order

| Order | Document | Why It Exists |
|---|---|---|
| 1 | [AWS Compute: EC2 and Auto Scaling](01-Foundations/AWS-Compute-EC2-Auto-Scaling-Gold-Sheet.md) | Instance types, pricing, ASG, placement, AMIs, production deployment |
| 2 | [AWS Networking: VPC, ALB, Route 53, CloudFront](01-Foundations/AWS-Networking-VPC-ALB-Route53-Gold-Sheet.md) | VPC design, subnets, security groups, NACLs, ALB, Route 53, CDN |
| 3 | [AWS CLI and Developer Tooling](01-Foundations/AWS-CLI-Developer-Tooling-Gold-Sheet.md) | Install, configure, profiles, MFA, JMESPath queries, scripting, CloudShell, SSO, LocalStack |
| 4 | [AWS Containers: ECS and EKS](02-Containers-Serverless/AWS-ECS-EKS-Container-Platform-Gold-Sheet.md) | ECS Fargate, task definitions, EKS node groups, IRSA, cluster autoscaler |
| 5 | [AWS Serverless: Lambda and API Gateway](02-Containers-Serverless/AWS-Lambda-API-Gateway-Serverless-Gold-Sheet.md) | Lambda cold start, concurrency, layers, API Gateway REST/HTTP/WebSocket |
| 6 | [AWS Storage: S3 and CloudFront](03-Storage-Database/AWS-S3-CloudFront-Storage-Gold-Sheet.md) | S3 storage classes, lifecycle, security, pre-signed URLs, OAC, behaviors |
| 7 | [AWS Database: RDS and Aurora](03-Storage-Database/AWS-RDS-Aurora-Database-Gold-Sheet.md) | RDS engines, Multi-AZ, read replicas, Aurora storage, Aurora Serverless v2 |
| 8 | [AWS NoSQL and Cache: DynamoDB and ElastiCache](03-Storage-Database/AWS-DynamoDB-ElastiCache-Gold-Sheet.md) | DynamoDB single-table, GSI, DAX, DynamoDB Streams; Redis vs Memcached |
| 9 | [AWS Identity: IAM Roles and Policies](04-Security-Identity/AWS-IAM-Roles-Policies-Gold-Sheet.md) | IAM users/roles/policies, resource-based policies, SCP, IRSA, permission boundaries |
| 10 | [AWS Encryption: Secrets Manager and KMS](04-Security-Identity/AWS-Secrets-KMS-Encryption-Gold-Sheet.md) | KMS CMKs, envelope encryption, Secrets Manager vs SSM Parameter Store |
| 11 | [AWS Auth and Protection: Cognito, WAF, Shield](04-Security-Identity/AWS-Cognito-WAF-Shield-Gold-Sheet.md) | Cognito user/identity pools, WAF rules, rate limiting, Shield Advanced |
| 12 | [AWS SSH, EC2 Access, Bastion Hosts, and SSM Session Manager](04-Security-Identity/AWS-SSH-EC2-Access-Bastion-SSM-Gold-Sheet.md) | Key pairs, SSH to EC2, bastion/ProxyJump, EC2 Instance Connect, SSM Session Manager, port forwarding, audit logging |
| 13 | [AWS Messaging: SQS and SNS](05-Messaging-Integration/AWS-SQS-SNS-Messaging-Gold-Sheet.md) | SQS standard/FIFO, DLQ, visibility timeout, SNS fan-out, message filtering |
| 14 | [AWS Events: EventBridge, Step Functions, Kinesis](05-Messaging-Integration/AWS-EventBridge-StepFunctions-Kinesis-Gold-Sheet.md) | EventBridge rules/pipes, Step Functions, Kinesis Data Streams/Firehose |
| 15 | [AWS Observability: CloudWatch and X-Ray](06-Observability-Operations/AWS-CloudWatch-XRay-Observability-Gold-Sheet.md) | Metrics, alarms, dashboards, log insights, X-Ray traces, ServiceLens |
| 16 | [AWS Audit and Operations: CloudTrail, Config, SSM](06-Observability-Operations/AWS-CloudTrail-Config-Systems-Manager-Gold-Sheet.md) | CloudTrail events, Config rules, SSM Session Manager, Parameter Store, Patch |
| 17 | [AWS IaC and CICD Release Engineering](07-Senior-Architecture/AWS-IaC-CICD-Release-Engineering-Gold-Sheet.md) | Terraform, CDK, CloudFormation, GitHub OIDC, blue-green, canary, rollback |
| 18 | [AWS Multi-Region DR and Resilience](07-Senior-Architecture/AWS-Multi-Region-DR-Resilience-Gold-Sheet.md) | RTO/RPO, pilot light, warm standby, active-active, Route 53 failover policies |
| 19 | [AWS Landing Zone and Governance](07-Senior-Architecture/AWS-Landing-Zone-Governance-Gold-Sheet.md) | Organizations, Control Tower, SCPs, centralized logging, account vending |
| 20 | [AWS Advanced Networking and FinOps](07-Senior-Architecture/AWS-Advanced-Networking-FinOps-Gold-Sheet.md) | Transit Gateway, PrivateLink, Direct Connect, hybrid DNS, cost control |
| 21 | [AWS GenAI: Bedrock, RAG, and Agents](08-GenAI-Platform/AWS-Bedrock-RAG-Agents-Gold-Sheet.md) | Bedrock API, model choice, Knowledge Bases, Agents, Guardrails, Flows |
| 22 | [AWS LLMOps: SageMaker AI and Model Production](08-GenAI-Platform/AWS-SageMaker-LLMOps-Gold-Sheet.md) | SageMaker endpoints, pipelines, evals, prompt CI/CD, drift, cost monitoring |
| 23 | [AWS Active Recall Question Bank](09-Practice-Upgrade/AWS-Active-Recall-Question-Bank.md) | Retrieval practice across all AWS topics and architecture decisions |
| 24 | [AWS Scenario Drill Bank](09-Practice-Upgrade/AWS-Scenario-Drill-Bank.md) | Production incident and design scenario drills |
| 25 | [AWS Mock Interview Scripts](09-Practice-Upgrade/AWS-Mock-Interview-Scripts.md) | Timed mock rounds from foundations through MAANG architect capstone |
| 26 | [AWS Interview Scoring Rubrics](09-Practice-Upgrade/AWS-Interview-Scoring-Rubrics.md) | Measurable scoring for service knowledge, design quality, and trade-offs |
| 27 | [AWS 2 Week 4 Week Mastery Roadmaps](09-Practice-Upgrade/AWS-2-Week-4-Week-Mastery-Roadmaps.md) | Structured fast prep and deeper mastery plans |

---

## Practice Upgrade Layer

Use `09-Practice-Upgrade` after the concept sheets to turn reading into interview performance.

| Practice File | Use It For |
|---|---|
| [AWS Active Recall Question Bank](09-Practice-Upgrade/AWS-Active-Recall-Question-Bank.md) | Daily recall and weak-spot detection |
| [AWS Scenario Drill Bank](09-Practice-Upgrade/AWS-Scenario-Drill-Bank.md) | Production and architecture scenario practice |
| [AWS Mock Interview Scripts](09-Practice-Upgrade/AWS-Mock-Interview-Scripts.md) | Timed spoken interview rehearsals |
| [AWS Interview Scoring Rubrics](09-Practice-Upgrade/AWS-Interview-Scoring-Rubrics.md) | Objective readiness scoring |
| [AWS 2 Week 4 Week Mastery Roadmaps](09-Practice-Upgrade/AWS-2-Week-4-Week-Mastery-Roadmaps.md) | Fast and deep study plans |

Recommended loop:

```text
read one sheet -> sketch architecture -> answer recall -> solve scenario -> speak mock answer -> score with rubric
```

---

## Learning Levels

### Beginner

You should be able to:
- explain which service solves which problem
- deploy a basic app to ECS or EC2 behind an ALB
- put app and DB in private subnets
- use S3 for uploads and pre-signed URLs
- configure basic CloudWatch alarms
- use IAM roles and Secrets Manager

### Intermediate

You should confidently handle:
- design a multi-AZ production app with RDS and ElastiCache
- choose RDS vs DynamoDB vs S3 by access pattern
- design SQS/SNS/EventBridge async workflows with DLQ
- implement IAM least privilege for ECS tasks and Lambda functions
- explain CI/CD with blue-green rollback
- debug networking, IAM, and permission failures
- write Terraform or CDK for core infrastructure

### Pro / MAANG Architect

You should be able to:
- design multi-account landing zones with SCPs and centralized audit
- define RTO/RPO and select pilot light vs warm standby vs active-active
- design network topology across accounts with Transit Gateway and PrivateLink
- implement GitHub OIDC to AWS without static access keys
- design canary/blue-green release strategies with automated rollback
- run FinOps reviews and identify hidden cost traps
- build Bedrock RAG/agent systems with guardrails, evals, and prompt management
- choose Bedrock vs SageMaker AI and justify the trade-off
- explain LLMOps lifecycle including quality drift and token cost control

---

## AWS Interview Architecture Template

For every design question, answer like this:

```text
1. Clarify: users, data volume, latency SLA, availability, compliance, budget
2. Entry path: Route 53 → CloudFront → WAF → ALB → API Gateway
3. Compute: EC2 / ECS Fargate / EKS / Lambda (justify choice)
4. Data: S3, RDS/Aurora, DynamoDB, ElastiCache, Redshift/Athena
5. Security: VPC private subnets, IAM roles, KMS, Secrets Manager, Cognito
6. Async: SQS / SNS / EventBridge / Step Functions / Kinesis
7. Operations: CloudWatch metrics + alarms, X-Ray traces, CloudTrail audit
8. Cost: right-sizing, Reserved/Spot, S3 lifecycle, data transfer awareness
9. Trade-offs: what you rejected and why
```

---

## Official Source Notes

- AWS Well-Architected Framework: <https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html>
- AWS Documentation: <https://docs.aws.amazon.com/>
- AWS Architecture Center: <https://aws.amazon.com/architecture/>
- AWS re:Invent talks: <https://www.youtube.com/@AWSEventsChannel>
