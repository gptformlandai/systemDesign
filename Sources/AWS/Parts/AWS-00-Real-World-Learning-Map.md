# AWS Parts Real-World Learning Map

> Goal: group the existing AWS notes by real-world job-to-be-done, so you study AWS like an architect operating production systems, not like a list of services.

---

# Index

| Section | Focus |
|---|---|
| [0. How To Use This Map](#0-how-to-use-this-map) | How To Use This Map |
| [1. Group 1: Core AWS Foundation](#1-group-1-core-aws-foundation) | Group 1: Core AWS Foundation |
| [2. Group 2: Deployment Journey](#2-group-2-deployment-journey) | Group 2: Deployment Journey |
| [3. Group 3: Storage, Database, and Data Placement](#3-group-3-storage-database-and-data-placement) | Group 3: Storage, Database, and Data Placement |
| [4. Group 4: Security, Identity, Secrets, and App Auth](#4-group-4-security-identity-secrets-and-app-auth) | Group 4: Security, Identity, Secrets, and App Auth |
| [5. Group 5: Messaging, Integration, and Observability](#5-group-5-messaging-integration-and-observability) | Group 5: Messaging, Integration, and Observability |
| [6. Group 6: Architect + GenAI Extension](#6-group-6-architect-genai-extension) | Group 6: Architect + GenAI Extension |
| [7. Real-World Answer Template](#7-real-world-answer-template) | Real-World Answer Template |
| [8. Console Click Rule](#8-console-click-rule) | Console Click Rule |

---

## 0. How To Use This Map

Use this file when you are stuck with a practical question:

```text
I need to deploy an app.
I need to debug networking.
I need to choose storage.
I need to secure secrets.
I need to design async processing.
I need to monitor production.
I need to answer an interview architecture question.
```

Important:

```text
Use https://console.aws.amazon.com for AWS Console work.
Do not use retail amazon.com.
```

---

## 1. Group 1: Core AWS Foundation

Read:

- [AWS-01 Compute + Networking](AWS-01-Compute-Networking.md)
- [AWS-06 Networking Story Mode](AWS-06-Networking-Story-Mode.md)

Use when:

```text
You need to understand how traffic reaches an app,
why app servers are private,
how ALB/API Gateway/CloudFront/Route 53 fit,
and how EC2/ECS/EKS/Lambda decisions are made.
```

Real-world questions:

- How does a user request reach my backend?
- Why is the backend not public?
- Why does my private service fail to call the internet?
- When do I use ALB vs API Gateway?
- Why ECS instead of EKS?
- Why Lambda is not always cheaper?

---

## 2. Group 2: Deployment Journey

Read:

- [AWS-05 EC2 ECS EKS Story and Deployment Guide](AWS-05-EC2-ECS-EKS-Story-and-Deployment-Guide.md)
- [AWS-04 DevOps Monitoring Architecture Interview QA](AWS-04-DevOps-Monitoring-Architecture-Interview-QA.md)

Use when:

```text
You have a Spring Boot / React app and want to move from local laptop
to EC2, ECS, or EKS with deployment, scaling, monitoring, and rollback.
```

Real-world questions:

- How do I package a Spring Boot app?
- How do I push Docker images to ECR?
- How do I deploy to ECS?
- How do I expose EKS through an ALB?
- How do I roll back a bad deployment?
- What should I monitor after production release?

---

## 3. Group 3: Storage, Database, and Data Placement

Read:

- [AWS-02 Storage + Database](AWS-02-Storage-Database.md)
- [AWS-07 Storage Story Mode](AWS-07-Storage-Story-Mode.md)

Use when:

```text
You need to decide where data lives:
files, user uploads, relational data, cache, analytics, dashboards,
reports, backups, and failure recovery.
```

Real-world questions:

- Should uploaded files go to S3 or EC2 disk?
- RDS or DynamoDB?
- EBS or EFS or S3?
- When Aurora over RDS?
- When Athena vs Redshift?
- How do I recover deleted or corrupted data?

---

## 4. Group 4: Security, Identity, Secrets, and App Auth

Read:

- [AWS-03 Security + Messaging + Integration](AWS-03-Security-Messaging-Integration.md)
- [AWS-08 Security Story Mode](AWS-08-Security-Story-Mode.md)

Use when:

```text
You need to protect AWS access, app users, secrets, network paths,
data at rest, and data in transit.
```

Real-world questions:

- How should my app access S3 without access keys?
- Where should DB passwords live?
- IAM vs Cognito?
- How does Spring Boot validate JWT from Cognito?
- How do I encrypt RDS/S3/EBS?
- Why should RDS never be public?

---

## 5. Group 5: Messaging, Integration, and Observability

Read:

- [AWS-03 Security + Messaging + Integration](AWS-03-Security-Messaging-Integration.md)
- [AWS-09 Messaging Integration Observability Story Mode](AWS-09-Messaging-Integration-Observability-Story-Mode.md)
- [AWS-04 DevOps Monitoring Architecture Interview QA](AWS-04-DevOps-Monitoring-Architecture-Interview-QA.md)

Use when:

```text
Your synchronous API is becoming slow or brittle,
you need background work, retries, DLQs, fan-out, event routing,
workflow orchestration, metrics, logs, alarms, and tracing.
```

Real-world questions:

- SQS or SNS or EventBridge?
- Why SNS plus SQS?
- Why must consumers be idempotent?
- How do I handle poison messages?
- When Step Functions instead of chained Lambdas?
- How do I debug queue backlog?
- What CloudWatch alarms should exist?

---

## 6. Group 6: Architect + GenAI Extension

Read:

- [Architect + GenAI Platinum Track Index](../Architect-GenAI-Track/AWS-Architect-GenAI-Platinum-Track-Index.md)

Use when:

```text
You want MAANG-level architect depth:
landing zones, governance, multi-account, DR, IaC, FinOps,
Bedrock, RAG, Agents, Guardrails, SageMaker AI, and LLMOps.
```

Real-world questions:

- How should a company structure AWS accounts?
- How do I design multi-region DR?
- How do I deploy using GitHub Actions with OIDC?
- How do I build a RAG chatbot on AWS?
- How do I stop GenAI from leaking sensitive data?
- Bedrock or SageMaker AI?

---

## 7. Real-World Answer Template

For every AWS design question, answer like this:

```text
1. Requirement:
   What workload, users, data, latency, RTO/RPO, scale, compliance?

2. Entry path:
   Route 53, CloudFront, ALB, API Gateway.

3. Compute:
   EC2, ECS, EKS, Lambda based on operational trade-off.

4. Data:
   S3, RDS/Aurora, DynamoDB, ElastiCache, Redshift/Athena.

5. Network:
   VPC, public/private subnets, NAT, endpoints, security groups.

6. Security:
   IAM roles, Secrets Manager, KMS, Cognito, WAF.

7. Async:
   SQS, SNS, EventBridge, Step Functions, Kinesis.

8. Operations:
   CloudWatch, X-Ray, CloudTrail, alarms, dashboards, rollback.

9. Cost:
   right-sizing, storage classes, NAT/data transfer, budgets.

10. Trade-offs:
   why this design and what you rejected.
```

---

## 8. Console Click Rule

Before clicking `Create`, `Save`, `Deploy`, `Delete`, or `Modify`, ask:

```text
What AWS resource changes?
What permission changes?
What network path opens or closes?
What cost begins?
What failure mode is improved?
What failure mode is introduced?
How do I verify it worked?
How do I roll back?
Is this change captured in IaC?
```

This is the difference between using AWS and architecting AWS.

