# AWS GenAI 02: LLMOps, SageMaker AI, Model Evaluation, and Production Deployment Real-World Guide

> Goal: operate GenAI and ML systems in production: evaluation, deployment, monitoring, cost, rollback, SageMaker AI endpoints, and LLMOps lifecycle.

---

# Index

| Section | Focus |
|---|---|
| [0. Real Situation](#0-real-situation) | Real Situation |
| [1. Bedrock vs SageMaker AI](#1-bedrock-vs-sagemaker-ai) | Bedrock vs SageMaker AI |
| [2. LLMOps Lifecycle](#2-llmops-lifecycle) | LLMOps Lifecycle |
| [3. Evaluation Dataset](#3-evaluation-dataset) | Evaluation Dataset |
| [4. Metrics That Matter](#4-metrics-that-matter) | Metrics That Matter |
| [5. Console Build: Bedrock Prompt Release](#5-console-build-bedrock-prompt-release) | Console Build: Bedrock Prompt Release |
| [6. Console Build: Bedrock Model/Prompt Cost Tracking](#6-console-build-bedrock-modelprompt-cost-tracking) | Console Build: Bedrock Model/Prompt Cost Tracking |
| [7. Console Build: SageMaker AI Real-Time Endpoint](#7-console-build-sagemaker-ai-real-time-endpoint) | Console Build: SageMaker AI Real-Time Endpoint |
| [8. SageMaker Inference Options](#8-sagemaker-inference-options) | SageMaker Inference Options |
| [9. Console Build: SageMaker Pipelines](#9-console-build-sagemaker-pipelines) | Console Build: SageMaker Pipelines |
| [10. Model Registry Promotion Pattern](#10-model-registry-promotion-pattern) | Model Registry Promotion Pattern |
| [11. LLM Deployment Pattern](#11-llm-deployment-pattern) | LLM Deployment Pattern |
| [12. Observability For GenAI](#12-observability-for-genai) | Observability For GenAI |
| [13. Failure Modes](#13-failure-modes) | Failure Modes |
| [14. Production Checklist](#14-production-checklist) | Production Checklist |
| [15. Interview Question](#15-interview-question) | Interview Question |
| [16. Strong Answer](#16-strong-answer) | Strong Answer |
| [17. Revision Notes](#17-revision-notes) | Revision Notes |
| [18. Official Source Notes](#18-official-source-notes) | Official Source Notes |

---

## 0. Real Situation

Your prototype chatbot worked in a demo.

Then production happened:

```text
answers became inconsistent
prompt edits broke old behavior
latency jumped
token costs exploded
retrieval quality degraded after new documents
model version changed
users found prompt injection tricks
no one knows whether the new prompt is better
custom ML model needs hosting
```

Architect answer:

```text
GenAI needs LLMOps:
versioning, evals, deployment stages, monitoring, rollback, cost control,
data governance, and model/prompt lifecycle management.
```

---

## 1. Bedrock vs SageMaker AI

| Need | Better Fit |
|---|---|
| Use managed foundation models quickly | Bedrock |
| RAG over private docs with managed workflow | Bedrock Knowledge Bases |
| Agent can call actions/APIs | Bedrock Agents |
| Prompt/flow versioning in Bedrock ecosystem | Bedrock Prompt Management / Flows |
| Train/customize/deploy your own ML model | SageMaker AI |
| Host custom container/model endpoint | SageMaker AI |
| ML pipeline for data prep, training, eval, deploy | SageMaker Pipelines |
| Need full model artifact control | SageMaker AI |

Architect rule:

```text
Use Bedrock when managed FM access is enough.
Use SageMaker AI when you own the model lifecycle or need custom training/hosting.
```

---

## 2. LLMOps Lifecycle

```text
1. Define use case and risk level.
2. Choose model and architecture.
3. Build prompt/RAG/agent.
4. Create eval dataset.
5. Run offline evaluation.
6. Add safety tests.
7. Deploy to dev/stage/prod.
8. Canary or limited rollout.
9. Monitor quality, latency, cost, safety.
10. Capture user feedback.
11. Iterate with versioning.
12. Roll back when regression appears.
```

---

## 3. Evaluation Dataset

A production GenAI app needs test cases:

```text
normal user questions
edge cases
ambiguous questions
out-of-scope questions
prompt injection attacks
PII extraction attempts
unauthorized data requests
expected citation cases
expected refusal cases
latency/cost stress cases
```

Example eval row:

```json
{
  "id": "refund-policy-001",
  "input": "Can enterprise customers get refund after 45 days?",
  "expected_behavior": "Answer only from refund policy docs with citation.",
  "must_include": ["enterprise", "45 days"],
  "must_not_include": ["invented exception"],
  "risk": "medium"
}
```

---

## 4. Metrics That Matter

### Product Quality

```text
answer correctness
groundedness
citation accuracy
task completion rate
human escalation rate
user feedback score
```

### Safety

```text
PII leakage
prompt injection success rate
unsafe output rate
unauthorized retrieval rate
guardrail block rate
```

### Operations

```text
p50/p95/p99 latency
timeouts
throttling
model error rate
retrieval error rate
tokens per request
cost per conversation
fallback rate
```

### RAG

```text
retrieval precision
retrieval recall
topK hit quality
chunk freshness
documents with stale embeddings
```

---

## 5. Console Build: Bedrock Prompt Release

### Console Path

```text
Bedrock -> Prompt management -> Create prompt
```

Then:

```text
Test prompt -> Create variant -> Compare outputs -> Create version
```

### What Each Click Changes

```text
Create prompt:
  reusable prompt resource.

Variant:
  test alternate wording/model/settings.

Compare:
  human review of behavior differences.

Create version:
  immutable prompt snapshot for deployment.
```

### Production Pattern

```text
prompt-v1:
  current prod

prompt-v2:
  candidate

eval:
  run offline tests

canary:
  route small traffic

rollback:
  app points back to prompt-v1
```

---

## 6. Console Build: Bedrock Model/Prompt Cost Tracking

### Console Path

```text
Bedrock -> Inference profiles -> Create application inference profile
```

Add tags:

```text
project = support-chatbot
environment = prod
team = customer-support
cost-center = cc-1234
```

### What This Click Changes

It gives you a stable invocation resource for tracking usage and cost attribution.

### Why It Matters

Token usage is a product cost.

Architect move:

```text
Track cost per team, feature, user tier, and workflow.
```

---

## 7. Console Build: SageMaker AI Real-Time Endpoint

### When You Need It

You trained or brought a custom model and need low-latency inference.

### Console Path

```text
AWS Console -> Search "SageMaker AI" -> Inference -> Models -> Create model
```

Choose:

```text
model artifact in S3
container image
execution role
VPC/network isolation if required
```

Then:

```text
Inference -> Endpoint configurations -> Create endpoint configuration
Inference -> Endpoints -> Create endpoint
```

Choose:

```text
instance type
initial instance count
autoscaling policy
data capture if monitoring
```

### What Each Click Changes

```text
Create model:
  registers model artifact and inference container.

Endpoint config:
  defines compute and deployment settings.

Endpoint:
  provisions live HTTPS inference endpoint.

Autoscaling:
  changes capacity based on traffic.

Data capture:
  stores request/response samples for monitoring.
```

### What Can Go Wrong

Always-on endpoints can be expensive.

Architect move:

```text
Use real-time endpoint for low latency steady use.
Use serverless endpoint for spiky/idle workloads that tolerate cold starts.
Use async endpoint for large payloads or long processing.
Use batch transform for offline bulk inference.
```

---

## 8. SageMaker Inference Options

| Option | Best For | Watch Out |
|---|---|---|
| Real-time endpoint | low-latency interactive inference | always-on cost |
| Serverless endpoint | spiky traffic, idle periods | cold starts, limits |
| Async inference | large payloads, long processing | near-real-time, queueing |
| Batch transform | offline bulk jobs | not interactive |

Architect rule:

```text
Do not host every model as an always-on endpoint.
Match inference mode to traffic pattern.
```

---

## 9. Console Build: SageMaker Pipelines

### Real Situation

ML team wants repeatable model lifecycle:

```text
data processing
training
evaluation
approval
deployment
monitoring
```

### Console Path

```text
SageMaker AI -> Pipelines -> Create pipeline
```

Build steps:

```text
processing step
training step
evaluation step
condition step
model registration
deployment step
```

### What Each Step Changes

```text
Processing:
  cleans/transforms data.

Training:
  creates model artifact.

Evaluation:
  computes quality metrics.

Condition:
  blocks model if metrics are below threshold.

Model registry:
  stores approved model version.

Deployment:
  promotes model to endpoint/environment.
```

### Why It Matters

SageMaker Pipelines gives automation, lineage, and repeatability for ML workflows.

---

## 10. Model Registry Promotion Pattern

```text
Train model
  -> evaluate
  -> register candidate
  -> approve for stage
  -> deploy to stage endpoint
  -> run integration tests
  -> approve for prod
  -> deploy canary
  -> monitor
  -> full rollout
```

Decision gate example:

```text
Accuracy >= 0.92
P95 latency <= 250 ms
Bias metric within policy
No critical safety regression
Cost per 1k predictions acceptable
```

---

## 11. LLM Deployment Pattern

For Bedrock:

```text
prompt version
model/inference profile
retrieval config
guardrail version
app code
eval dataset
```

All should be tracked.

Release artifact:

```json
{
  "app_version": "2026.06.17-1",
  "model": "approved-model",
  "prompt_version": "support-summary-v4",
  "guardrail_version": "gr-prod-v2",
  "knowledge_base": "kb-support-prod",
  "retrieval_top_k": 5,
  "temperature": 0.2
}
```

Why:

```text
When output quality regresses, you need to know what changed.
```

---

## 12. Observability For GenAI

Log safely:

```text
request ID
user/account ID hash
model/profile
prompt version
guardrail version
retrieval document IDs
latency
input/output token counts
finish reason
blocked category
fallback used
user feedback
```

Avoid logging:

```text
raw secrets
full PII
access tokens
private documents
full prompts unless approved and redacted
```

Dashboards:

```text
cost per day
tokens per feature
p95 latency
error rate
throttle count
guardrail blocks
retrieval no-hit rate
thumbs down rate
human escalation rate
```

---

## 13. Failure Modes

### Failure Mode 1: Prompt Regression

Symptom:

```text
New prompt gives more confident but less accurate answers.
```

Fix:

```text
rollback prompt version
run evals before release
add regression case
```

### Failure Mode 2: Retrieval Drift

Symptom:

```text
After new docs ingest, chatbot retrieves wrong policy.
```

Fix:

```text
review chunking/metadata
add reranking
separate knowledge bases by domain
improve eval set
```

### Failure Mode 3: Token Cost Spike

Symptom:

```text
Cost triples after increasing context/topK.
```

Fix:

```text
token budget
shorter chunks
summarized context
limit conversation history
cost alarms
```

### Failure Mode 4: Custom Model Endpoint Idle Cost

Symptom:

```text
SageMaker endpoint runs 24/7 but receives little traffic.
```

Fix:

```text
serverless inference
async inference
scheduled scaling
batch transform
```

---

## 14. Production Checklist

- eval dataset exists
- safety evals exist
- prompt/model/guardrail versions tracked
- RAG config tracked
- release has rollback path
- token budgets enforced
- cost by app/team visible
- p95/p99 latency monitored
- guardrail block rate monitored
- retrieval quality monitored
- human feedback captured
- sensitive logs redacted
- SageMaker endpoints right-sized
- model deployment mode matches traffic
- pipeline gates block bad models
- model registry or equivalent approval exists

---

## 15. Interview Question

> How do you productionize a GenAI feature on AWS?

---

## 16. Strong Answer

I would treat the GenAI feature like a production system, not a prompt demo. First I would define the use case, risk level, expected answer sources, and success metrics. If managed foundation models are enough, I would use Bedrock. If I need private knowledge, I would use RAG through Bedrock Knowledge Bases or a custom vector pipeline. If I own the model lifecycle, I would use SageMaker AI.

Before release, I would build an evaluation dataset with normal cases, edge cases, prompt injection, PII leakage attempts, and expected refusals. I would version prompts, guardrails, model choices, and retrieval config. In production, I would track latency, errors, token cost, guardrail blocks, retrieval quality, and user feedback. Rollback would point back to a previous prompt/model/guardrail/config version.

For custom ML models, I would use SageMaker Pipelines for processing, training, evaluation, registration, and deployment, choosing real-time, serverless, async, or batch inference based on traffic pattern.

---

## 17. Revision Notes

- One-line summary: LLMOps is release engineering plus evaluation and observability for GenAI behavior.
- Three keywords: evals, versions, rollback.
- One interview trap: deploying prompt changes without regression tests.
- Memory trick: "If it can change output, version it."

---

## 18. Official Source Notes

- Amazon SageMaker AI is a managed ML service for building, training, and deploying ML/foundation models: <https://docs.aws.amazon.com/sagemaker/latest/dg/whatis.html>
- SageMaker AI supports real-time, serverless, asynchronous, and batch inference options: <https://docs.aws.amazon.com/sagemaker/latest/dg/deploy-model.html>
- SageMaker Pipelines automates ML workflows with steps for data processing, training, evaluation, deployment, and lineage: <https://docs.aws.amazon.com/sagemaker/latest/dg/pipelines.html>
- Bedrock Prompt Management supports prompt variables, variants, testing, and versions: <https://docs.aws.amazon.com/bedrock/latest/userguide/prompt-management.html>
- Bedrock inference profiles support usage/cost tracking and cross-region routing where supported: <https://docs.aws.amazon.com/bedrock/latest/userguide/inference-profiles.html>

