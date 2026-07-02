# 43. Datadog LLM Observability: AI, RAG, Token Cost, Safety

## Goal

Understand how to observe AI applications that call LLM providers, use prompt chains, perform retrieval-augmented generation, and create new cost/safety risks.

---

## Mental Model

Traditional API observability:

```text
request -> service -> database -> response
```

LLM observability:

```text
user request -> prompt assembly -> retrieval -> model call -> tool call -> response -> evaluation
```

The model call is only one span in a larger AI workflow.

---

## Why LLM Apps Need Special Observability

LLM failures are not just 500s:

- Slow model responses.
- Token cost spikes.
- Poor answer quality.
- Hallucinations.
- Unsafe responses.
- Retrieval returning bad context.
- Provider rate limits.
- Tool call loops.
- Prompt injection.
- PII leakage in prompts.

---

## Core Signals

| Signal | Why It Matters |
|---|---|
| Prompt tokens | Input cost and context size |
| Completion tokens | Output cost and latency |
| Total tokens | Billing and model load |
| Model/provider | Compare OpenAI/Anthropic/Bedrock/local |
| Latency | User experience |
| Error/rate limit | Provider reliability |
| Prompt template version | Regression tracking |
| Retrieval documents | RAG quality |
| Safety classification | Policy compliance |
| User feedback | Quality loop |

---

## Recommended Tags

```text
service:ai-support-bot
env:production
version:1.12.0
team:ai-platform
llm.provider:openai
llm.model:gpt-4.1
llm.operation:chat.completion
prompt.template:support-triage-v7
rag.index:kb-prod
customer_tier:enterprise
```

Never use raw prompt text, user IDs, or emails as metric tags.

---

## Trace Shape For RAG

```text
POST /chat
  -> authenticate user
  -> classify intent
  -> embed query
  -> vector search knowledge-base
  -> rerank documents
  -> build prompt
  -> LLM chat completion
  -> safety check
  -> persist conversation metadata
```

Trace each step so you know whether failure is retrieval, model, prompt construction, or downstream storage.

---

## Metrics To Monitor

```text
llm.requests.count
llm.errors.count
llm.latency.p95
llm.tokens.prompt
llm.tokens.completion
llm.tokens.total
llm.cost.estimated
llm.rate_limits.count
llm.safety.violations
llm.tool_calls.count
rag.retrieval.latency
rag.documents.returned
rag.no_result.count
```

Exact metric names depend on instrumentation. The concepts matter more than the names.

---

## Cost Formula

```text
estimated_cost =
  prompt_tokens * prompt_price_per_token
  +
  completion_tokens * completion_price_per_token
```

Track by:

```text
model
provider
service
team
customer_tier
prompt_template
feature
```

Do not group by unbounded user/session/request IDs.

---

## Failure Patterns

| Pattern | Symptom | Investigation |
|---|---|---|
| Provider rate limit | 429 errors | provider, model, retry behavior |
| Context explosion | token cost spike | prompt template, retrieved docs |
| Bad retrieval | low quality answers | retrieval spans and document IDs |
| Tool loop | long latency, many tool spans | agent trace tree |
| Prompt injection | unsafe or unexpected output | safety spans and input classification |
| Model regression | quality drop after model switch | compare model/version/template |

---

## PII And Safety

AI observability must protect data:

```text
Do:
  - redact secrets and PII from prompts/logs
  - store prompt templates separately from user content
  - sample sensitive payloads carefully
  - monitor safety violations
  - track policy decisions as structured metadata

Avoid:
  - indexing raw prompts with PII
  - using prompt text as tags
  - logging credentials sent to tools
```

---

## Monitor Examples

### Token Cost Spike

```text
Alert:
  estimated LLM cost for service:ai-support-bot
  > 2x 7-day baseline
  grouped by prompt.template and model
```

### Provider Latency

```text
Alert:
  p95 llm latency > 8s for model:gpt-4.1
  for 10 minutes
```

### RAG Empty Retrieval

```text
Alert:
  rag.no_result.count / chat.requests > 15%
```

---

## Practical Question

> Your support chatbot cost tripled and answer quality dropped after a release. How would you debug with Datadog?

---

## Strong Answer

I would start by splitting LLM cost by service, model, prompt template, version, and customer tier. Then I would compare prompt tokens, completion tokens, latency, and error rates before and after the release. If prompt tokens increased, I would inspect prompt assembly and RAG retrieval spans to see whether more documents or larger context were injected.

For answer quality, I would inspect retrieval hit rate, no-result rate, reranker behavior, safety violations, and user feedback. I would also compare model/provider and prompt template versions. The likely fixes are prompt template rollback, retrieval limit tuning, chunking changes, model routing changes, or token budget enforcement.

---

## Interview Sound Bite

LLM observability tracks the AI workflow, not just the model call. You need traces for prompt assembly, retrieval, model calls, tools, and safety, plus metrics for tokens, cost, latency, errors, quality, and prompt/model versions.
