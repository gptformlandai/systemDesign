# AWS Architect + GenAI Platinum Track Index

> Goal: move from "I know AWS services" to "I can design, deploy, secure, operate, debug, and cost-control real AWS and GenAI systems like a production architect."

---

# Index

| Section | Focus |
|---|---|
| [0. Why This Track Exists](#0-why-this-track-exists) | Why This Track Exists |
| [1. Study Order](#1-study-order) | Study Order |
| [2. The Architect Mental Model](#2-the-architect-mental-model) | The Architect Mental Model |
| [3. The GenAI Architect Mental Model](#3-the-genai-architect-mental-model) | The GenAI Architect Mental Model |
| [4. Console Path Format Used In This Track](#4-console-path-format-used-in-this-track) | Console Path Format Used In This Track |
| [5. Beginner To Pro Progression](#5-beginner-to-pro-progression) | Beginner To Pro Progression |
| [6. Interview Rule](#6-interview-rule) | Interview Rule |
| [7. Official Source Notes](#7-official-source-notes) | Official Source Notes |

---

## 0. Why This Track Exists

The existing AWS notes cover the core backend/platform foundation:

```text
compute, networking, storage, database, security basics,
messaging, observability, and Spring Boot / React deployment.
```

This track adds the architect layer:

```text
multi-account governance
enterprise security
disaster recovery
IaC and release engineering
advanced networking
FinOps
data platform architecture
Bedrock GenAI
RAG and agents
LLMOps and SageMaker deployment
production incident scenarios
```

The style is intentionally practical:

```text
real situation
-> console path
-> what each click changes
-> CLI / IaC equivalent
-> production validation
-> failure modes
-> interview answer
```

---

## 1. Study Order

| Order | Document | What You Learn |
|---|---|---|
| 1 | [Core Service Console Runbook](AWS-Core-Service-Console-Runbook-Real-World.md) | EC2, VPC, ALB, ECS, EKS, Lambda, S3, RDS, DynamoDB, SQS, SNS, EventBridge, CloudWatch, CloudTrail, Secrets Manager, Bedrock console paths |
| 2 | [Landing Zone, Multi-Account, and Governance](AWS-Architect-01-Landing-Zone-Governance-Real-World.md) | Organizations, Control Tower, account separation, SCPs, centralized logs, account vending |
| 3 | [Security Architecture and Incident Response](AWS-Architect-02-Security-Incident-Response-Real-World.md) | IAM Identity Center, GuardDuty, Security Hub, Inspector, Macie, Config, Access Analyzer, incident response |
| 4 | [Resilience, DR, and Multi-Region Architecture](AWS-Architect-03-Resilience-DR-Multi-Region-Real-World.md) | RTO/RPO, backup vs HA vs DR, pilot light, warm standby, active-active, Route 53 failover |
| 5 | [IaC, CI/CD, and Release Engineering](AWS-Architect-04-IaC-CICD-Release-Engineering-Real-World.md) | Terraform/CDK/CloudFormation, GitHub OIDC, blue-green, canary, rollback, drift |
| 6 | [Advanced Networking, FinOps, and Data Platform](AWS-Architect-05-Advanced-Networking-FinOps-Data-Real-World.md) | Transit Gateway, PrivateLink, hybrid DNS, Direct Connect, cost controls, lakehouse |
| 7 | [Amazon Bedrock, RAG, Agents, and GenAI App Deployment](AWS-GenAI-01-Bedrock-RAG-Agents-Real-World.md) | Bedrock APIs, model choice, RAG, Knowledge Bases, vector stores, Agents, Guardrails, prompt management |
| 8 | [LLMOps, SageMaker AI, Model Evaluation, and Production Deployment](AWS-GenAI-02-LLMOps-SageMaker-Deployment-Real-World.md) | SageMaker AI, endpoints, pipelines, evals, prompt CI/CD, monitoring, cost and drift |
| 9 | [Architect + GenAI Production Scenario Playbook](AWS-Scenario-Playbook-Architect-GenAI.md) | Real incidents and interview prompts with exact action paths |

---

## 2. The Architect Mental Model

An AWS architect thinks in layers:

```text
Business goal
  -> workload requirements
  -> account boundary
  -> network boundary
  -> identity boundary
  -> compute/data choice
  -> deployment path
  -> security controls
  -> observability
  -> cost model
  -> failure recovery
```

The mistake beginners make:

```text
"Which AWS service should I use?"
```

The architect question:

```text
"What operational property do I need, and which AWS service gives it with the least unnecessary complexity?"
```

---

## 3. The GenAI Architect Mental Model

GenAI production systems are not just prompts.

They are systems with:

```text
model access
prompt/version management
retrieval
embeddings
vector search
reranking
tool/action calling
guardrails
evaluation
observability
latency control
token cost control
data privacy
human fallback
deployment and rollback
```

The key design question:

```text
Does the model need to answer from general knowledge,
private company knowledge,
structured data,
or by taking actions in another system?
```

That answer decides:

```text
plain model call
RAG
agent
workflow
fine-tuning/customization
classic ML model
```

---

## 4. Console Path Format Used In This Track

For UI steps, this track uses this format:

```text
AWS Console -> Search "<service>" -> Click "<screen>" -> Choose "<option>"
```

Then each click explains:

```text
What it changes:
  The AWS resource, permission, route, deployment behavior, or security boundary created by that click.

Why it matters:
  The production reason this setting exists.

What can go wrong:
  The common misconfiguration and blast radius.
```

Important:

```text
Use https://console.aws.amazon.com for AWS, not retail amazon.com.
```

---

## 5. Beginner To Pro Progression

### Beginner

You should be able to:

- explain which service solves which problem
- deploy a basic app to ECS or EC2
- put app and DB in private subnets
- use S3 for uploads
- configure basic CloudWatch alarms
- use Secrets Manager instead of hardcoded secrets

### Intermediate

You should be able to:

- design a multi-AZ production app
- choose RDS vs DynamoDB vs S3 correctly
- use SQS/SNS/EventBridge for async flows
- design IAM roles with least privilege
- explain CI/CD and rollback
- debug networking and permission failures
- build a basic Bedrock RAG app

### Pro / Architect

You should be able to:

- design multi-account landing zones
- apply SCPs and centralized audit logging
- design multi-region DR with RTO/RPO
- reason about network routing across accounts and regions
- implement GitHub OIDC to AWS
- design canary/blue-green deployment strategy
- run cost reviews and identify hidden cost traps
- build Bedrock RAG/agent systems with guardrails and evals
- choose Bedrock vs SageMaker AI
- explain LLMOps lifecycle and production controls

---

## 6. Interview Rule

Never answer an AWS architecture question by only naming services.

Strong structure:

```text
1. Clarify requirements.
2. State workload assumptions.
3. Choose services and explain why.
4. Define network/security boundaries.
5. Define data flow.
6. Define scaling and failure handling.
7. Define observability and cost controls.
8. Mention trade-offs and alternatives.
```

For GenAI:

```text
1. Identify answer source: model knowledge, private docs, structured DB, or tools.
2. Choose plain inference, RAG, agent, or workflow.
3. Add guardrails, evals, logging, and token budgets.
4. Define fallback when confidence is low.
5. Track quality, latency, and cost in production.
```

---

## 7. Official Source Notes

This track aligns with official AWS documentation for:

- AWS Well-Architected Framework: <https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html>
- AWS Organizations: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_introduction.html>
- AWS Control Tower: <https://docs.aws.amazon.com/controltower/latest/userguide/what-is-control-tower.html>
- AWS CloudTrail: <https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-user-guide.html>
- Amazon Bedrock: <https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html>
- Bedrock Agents: <https://docs.aws.amazon.com/bedrock/latest/userguide/agents.html>
- Bedrock Knowledge Bases: <https://docs.aws.amazon.com/bedrock/latest/userguide/knowledge-base.html>
- Bedrock Guardrails: <https://docs.aws.amazon.com/bedrock/latest/userguide/guardrails.html>
- Bedrock Prompt Management: <https://docs.aws.amazon.com/bedrock/latest/userguide/prompt-management.html>
- Bedrock Flows: <https://docs.aws.amazon.com/bedrock/latest/userguide/flows.html>
- Bedrock Inference Profiles: <https://docs.aws.amazon.com/bedrock/latest/userguide/inference-profiles.html>
- Amazon SageMaker AI: <https://docs.aws.amazon.com/sagemaker/latest/dg/whatis.html>
- SageMaker AI model deployment: <https://docs.aws.amazon.com/sagemaker/latest/dg/deploy-model.html>
- SageMaker Pipelines: <https://docs.aws.amazon.com/sagemaker/latest/dg/pipelines.html>
