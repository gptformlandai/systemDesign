# AWS GenAI 01: Amazon Bedrock, RAG, Agents, and GenAI App Deployment Real-World Guide

> Goal: build production GenAI applications on AWS using Bedrock, RAG, Knowledge Bases, Agents, Guardrails, Prompt Management, Flows, and real deployment controls.

---

# Index

| Section | Focus |
|---|---|
| [0. Real Situation](#0-real-situation) | Real Situation |
| [1. GenAI Decision Tree](#1-genai-decision-tree) | GenAI Decision Tree |
| [2. Bedrock Core Concepts](#2-bedrock-core-concepts) | Bedrock Core Concepts |
| [3. Console Build: Enable Model Access](#3-console-build-enable-model-access) | Console Build: Enable Model Access |
| [4. Console Build: First Model Call Test](#4-console-build-first-model-call-test) | Console Build: First Model Call Test |
| [5. Backend Pattern: Plain Bedrock Call](#5-backend-pattern-plain-bedrock-call) | Backend Pattern: Plain Bedrock Call |
| [6. Console Build: Bedrock Knowledge Base For RAG](#6-console-build-bedrock-knowledge-base-for-rag) | Console Build: Bedrock Knowledge Base For RAG |
| [7. RAG Chunking Strategy](#7-rag-chunking-strategy) | RAG Chunking Strategy |
| [8. Console Build: Bedrock Guardrails](#8-console-build-bedrock-guardrails) | Console Build: Bedrock Guardrails |
| [9. Console Build: Prompt Management](#9-console-build-prompt-management) | Console Build: Prompt Management |
| [10. Console Build: Bedrock Agent](#10-console-build-bedrock-agent) | Console Build: Bedrock Agent |
| [11. Console Build: Bedrock Flow](#11-console-build-bedrock-flow) | Console Build: Bedrock Flow |
| [12. Console Build: Inference Profile](#12-console-build-inference-profile) | Console Build: Inference Profile |
| [13. Production Architecture: Support Chatbot](#13-production-architecture-support-chatbot) | Production Architecture: Support Chatbot |
| [14. Production Checklist](#14-production-checklist) | Production Checklist |
| [15. Interview Question](#15-interview-question) | Interview Question |
| [16. Strong Answer](#16-strong-answer) | Strong Answer |
| [17. Revision Notes](#17-revision-notes) | Revision Notes |
| [18. Official Source Notes](#18-official-source-notes) | Official Source Notes |

---

## 0. Real Situation

Product asks:

```text
Can we build a chatbot that answers from our internal docs,
creates support tickets,
summarizes customer calls,
and never leaks sensitive information?
```

Bad answer:

```text
Call an LLM API from the backend and hope the prompt is good.
```

Architect answer:

```text
Use Bedrock for managed foundation model access,
RAG for private knowledge,
Agents or Flows for actions/workflows,
Guardrails for safety,
Prompt Management for versioning,
inference profiles for usage/cost tracking,
and evals/observability before production.
```

---

## 1. GenAI Decision Tree

```text
User asks a general question:
  -> plain Bedrock model call

User asks about company documents:
  -> RAG / Bedrock Knowledge Base

User asks the system to perform an action:
  -> Bedrock Agent with action group or app-controlled tool calling

User needs multi-step workflow:
  -> Bedrock Flow or Step Functions + model calls

You need custom ML model:
  -> SageMaker AI

You need strong safety controls:
  -> Bedrock Guardrails + app authorization + logging redaction
```

---

## 2. Bedrock Core Concepts

```text
Foundation model:
  model used for text, image, embeddings, or multimodal tasks.

Inference:
  calling a model with prompt/messages and parameters.

Converse API:
  unified conversational API for supported models.

InvokeModel:
  lower-level model invocation API.

Knowledge Base:
  managed RAG workflow over private data.

Agent:
  model-powered planner that can call APIs/action groups and knowledge bases.

Guardrail:
  safety and privacy policy applied to model inputs/outputs.

Prompt Management:
  reusable, versioned prompts with variables and variants.

Flow:
  visual/workflow composition of prompts, models, knowledge bases, and Lambda.

Inference Profile:
  resource for tracking usage/cost and routing to one or more regions where supported.
```

---

## 3. Console Build: Enable Model Access

### Console Path

```text
AWS Console -> Search "Bedrock" -> Model access -> Modify model access
```

Choose:

```text
approved model providers
approved models
submit/enable access
```

### What This Click Changes

It allows your AWS account to invoke selected foundation models.

### Why It Matters

Model access is account/region controlled.

### What Can Go Wrong

Enabling every model creates governance and cost confusion.

Architect move:

```text
Approve models per use case.
Document data classification.
Track cost by app/team.
Create sandbox vs prod model access separately.
```

---

## 4. Console Build: First Model Call Test

### Console Path

```text
Bedrock -> Playgrounds -> Chat/Text -> Choose model -> Enter prompt -> Run
```

### What Each Click Changes

```text
Choose model:
  selects latency, cost, context, reasoning, and quality profile.

Inference parameters:
  control output behavior such as randomness and length.

Run:
  invokes the model and generates billable usage.
```

### Why It Matters

Playground is for exploration.

Production apps should call:

```text
Converse API
InvokeModel API
Agent runtime
Knowledge Base retrieve-and-generate
Flow invocation
```

---

## 5. Backend Pattern: Plain Bedrock Call

### Use Case

Summarize customer support text.

### Architecture

```text
React
  -> Spring Boot API
  -> Bedrock Runtime
  -> response
```

### Java-Style Pseudocode

```java
public SummaryResponse summarize(String transcript) {
    String prompt = """
        Summarize the following support conversation.
        Return:
        - issue
        - customer sentiment
        - next action

        Conversation:
        %s
        """.formatted(transcript);

    return bedrockClient.converse("approved-model-id", prompt);
}
```

Production additions:

```text
input size validation
PII redaction if needed
timeouts
retries with backoff
guardrail
token budget
logging without sensitive content
```

---

## 6. Console Build: Bedrock Knowledge Base For RAG

### Real Situation

Users ask:

```text
What is our refund policy for enterprise customers?
How do I deploy service X?
What does the incident runbook say?
```

The model cannot answer reliably from general knowledge.

Use RAG.

### Console Path

```text
Bedrock -> Knowledge Bases -> Create knowledge base
```

Choose:

```text
name
IAM service role
data source: S3
embedding model
vector store
sync data source
```

### What Each Click Changes

```text
Name:
  creates the managed RAG resource.

IAM service role:
  allows Bedrock to read source data and write embeddings.

S3 data source:
  location of private documents.

Embedding model:
  converts text chunks into vectors.

Vector store:
  stores embeddings for semantic search.

Sync:
  ingests/updates document index.
```

### Why It Matters

RAG improves factuality by retrieving relevant private context before generation.

### What Can Go Wrong

RAG can still hallucinate if:

- retrieval returns weak chunks
- chunks are too large/small
- documents are stale
- answer is not grounded
- user lacks permission for retrieved docs

Architect move:

```text
Use citations.
Use metadata filters.
Use guardrail grounding checks.
Use eval set with real questions.
Show "I do not know" when retrieval is weak.
```

---

## 7. RAG Chunking Strategy

Bad:

```text
One vector per entire 200-page PDF.
```

Also bad:

```text
One vector per sentence with no context.
```

Better starting point:

```text
chunk size: 300-800 tokens
overlap: 50-150 tokens
metadata: document_type, team, sensitivity, owner, updated_at, ACL
```

Production decision:

```text
Chunking is not theoretical.
It directly affects retrieval quality, cost, and hallucination risk.
```

---

## 8. Console Build: Bedrock Guardrails

### Console Path

```text
Bedrock -> Guardrails -> Create guardrail
```

Configure:

```text
content filters
denied topics
word filters
sensitive information filters
contextual grounding checks
blocked message
```

### What Each Click Changes

```text
Content filters:
  block harmful categories.

Denied topics:
  block business-disallowed subjects.

Sensitive information:
  masks or blocks PII/secrets.

Contextual grounding:
  catches answers not supported by retrieved context.

Blocked message:
  controls user-facing refusal response.
```

### Production Example

For HR chatbot:

```text
Block salary disclosure to unauthorized users.
Mask SSNs.
Ground answers only in HR policy docs.
Refuse legal/medical advice.
Log blocked category but not secret value.
```

---

## 9. Console Build: Prompt Management

### Console Path

```text
Bedrock -> Prompt management -> Create prompt
```

Add:

```text
system instructions
variables
model selection
inference parameters
variants
test values
version
```

### What Each Click Changes

```text
Variables:
  make prompt reusable across requests.

Model selection:
  ties prompt test to a model/profile.

Variant:
  lets you compare prompt/model/settings.

Version:
  freezes a tested prompt for deployment.
```

### Why It Matters

Prompt changes are production changes.

They need:

```text
review
evals
versioning
rollback
cost tracking
```

---

## 10. Console Build: Bedrock Agent

### Real Situation

User says:

```text
Create a support ticket for this failed deployment and assign it to platform team.
```

This requires action, not just text.

### Console Path

```text
Bedrock -> Agents -> Create agent
```

Configure:

```text
foundation model
instructions
action group
Lambda function or API schema
knowledge base association
guardrail
test
create alias/version
```

### What Each Click Changes

```text
Instructions:
  define agent behavior and boundaries.

Action group:
  defines what external actions the agent can take.

Lambda/API schema:
  implementation for the action.

Knowledge base:
  gives private context.

Guardrail:
  adds safety policy.

Alias/version:
  deploys a stable agent reference to the app.
```

### What Can Go Wrong

Overpowered agent.

Bad:

```text
Agent can call any admin API with broad IAM role.
```

Better:

```text
Allow only specific action group APIs.
Validate user authorization in backend.
Require confirmation before destructive actions.
Log action requests and results.
Use idempotency keys.
```

---

## 11. Console Build: Bedrock Flow

### Use Case

Generate a weekly incident report:

```text
fetch incidents
summarize trends
query runbook knowledge base
generate report
send to Slack/email
```

### Console Path

```text
Bedrock -> Flows -> Create flow
```

Add nodes:

```text
prompt node
knowledge base node
Lambda node
condition node
output node
```

Prepare/test:

```text
Prepare flow -> Test flow -> Publish version -> Create alias
```

### What Each Click Changes

```text
Node:
  one step in workflow.

Connection:
  passes output from one step to next.

Prepare:
  makes draft runnable.

Version:
  immutable workflow snapshot.

Alias:
  stable endpoint your app calls.
```

---

## 12. Console Build: Inference Profile

### Console Path

```text
Bedrock -> Inference profiles -> Create application inference profile
```

Choose:

```text
model or cross-region system profile
name
tags
```

### What This Click Changes

It creates a resource your app can use to:

- track usage metrics
- allocate costs through tags
- route to one or more regions where supported

### Why It Matters

For production:

```text
You need to know which app/team/user path is spending Bedrock money.
```

---

## 13. Production Architecture: Support Chatbot

```text
React UI
  -> API Gateway / ALB
  -> Spring Boot Chat API
  -> AuthN/AuthZ
  -> request validation
  -> prompt version lookup
  -> Bedrock Guardrail input check
  -> Knowledge Base retrieve
  -> Bedrock model generation
  -> Guardrail output check
  -> citations returned
  -> CloudWatch metrics/logs
  -> feedback stored in DB
```

Data stores:

```text
S3:
  source docs

OpenSearch Serverless / vector store:
  embeddings

RDS/DynamoDB:
  conversations, feedback, permissions, audit metadata

CloudWatch:
  latency, errors, token usage, guardrail blocks
```

---

## 14. Production Checklist

- approved model access only
- model choice documented
- prompt versions controlled
- guardrail attached
- source data classified
- RAG source bucket encrypted/private
- vector store access scoped
- document metadata includes sensitivity/owner
- retrieval respects user authorization
- citations returned
- no sensitive prompts/responses in logs
- token budget per request/user/team
- retry/backoff/timeouts
- fallback response when model fails
- eval dataset before release
- user feedback loop
- cost dashboard by app/team

---

## 15. Interview Question

> Design a production RAG chatbot on AWS for internal company documents.

---

## 16. Strong Answer

I would use Amazon Bedrock for managed foundation model access and Bedrock Knowledge Bases or a custom RAG pipeline over encrypted S3 documents and a vector store. I would classify documents before ingestion, attach metadata such as team, sensitivity, owner, and ACL, and make retrieval authorization-aware so users only retrieve documents they are allowed to see.

The application would authenticate users, validate input, retrieve relevant chunks, call Bedrock with a versioned prompt, enforce Guardrails for PII and grounding, return citations, and log metrics without storing sensitive prompt content. I would track latency, error rate, token usage, retrieval quality, refusal rate, and user feedback. Prompt/model changes would go through evals and canary release.

If the app needs to take actions, I would use an Agent or app-controlled tool calling with least-privilege action APIs and confirmation for destructive actions.

---

## 17. Revision Notes

- One-line summary: production GenAI is model + data + retrieval + safety + evals + operations.
- Three keywords: RAG, guardrails, evals.
- One interview trap: saying RAG solves authorization automatically.
- Memory trick: "Retrieve only what user can see; answer only what retrieval supports."

---

## 18. Official Source Notes

- Amazon Bedrock provides managed foundation model access for building and scaling GenAI applications: <https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html>
- Bedrock Knowledge Bases support RAG over private data with retrieval and generated responses: <https://docs.aws.amazon.com/bedrock/latest/userguide/knowledge-base.html>
- Bedrock Agents can orchestrate FMs, data sources, APIs, and user conversations: <https://docs.aws.amazon.com/bedrock/latest/userguide/agents.html>
- Bedrock Guardrails provide content, PII, prompt attack, grounding, and reasoning checks: <https://docs.aws.amazon.com/bedrock/latest/userguide/guardrails.html>
- Prompt Management supports reusable prompts, variants, testing, and versions: <https://docs.aws.amazon.com/bedrock/latest/userguide/prompt-management.html>
- Bedrock Flows compose prompts, models, knowledge bases, Lambda, versions, and aliases: <https://docs.aws.amazon.com/bedrock/latest/userguide/flows.html>
- Inference profiles support usage/cost tracking and cross-region inference routing where supported: <https://docs.aws.amazon.com/bedrock/latest/userguide/inference-profiles.html>

