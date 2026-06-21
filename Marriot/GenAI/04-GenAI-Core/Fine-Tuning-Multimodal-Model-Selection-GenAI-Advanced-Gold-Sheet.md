# Fine-Tuning, Multimodal, Model Selection & Advanced GenAI - Gold Sheet

> **GenAI Mastery Track - Core File 5**
> For: all learners | Level: intermediate to advanced | Mode: know when to go beyond prompting and RAG

---

## 1. Why This Sheet Matters

Many learners jump to fine-tuning too early.

In production GenAI, you usually consider solutions in this order:

```text
better prompt
  -> better structured output
  -> better retrieval/RAG
  -> better tool use
  -> better model selection
  -> fine-tuning
  -> custom model training
```

Fine-tuning is powerful, but it is not a magic fix for missing knowledge, bad retrieval, or weak evals.

---

## 2. Model Selection

Choose a model based on task, not hype.

| Task | Model Need |
|---|---|
| classification | cheap, reliable, low temperature |
| extraction | structured output accuracy |
| RAG answer | grounding, context handling, citations |
| coding | strong reasoning and code ability |
| agent/tool use | tool-call reliability |
| multimodal | image/document/audio support |
| high-risk domain | stronger model + guardrails + human review |
| batch processing | cost efficiency |

### Selection Criteria

- quality
- latency
- cost
- context length
- structured output support
- tool support
- multimodal capability
- data privacy
- deployment region
- provider reliability
- eval performance

---

## 3. Prompting vs RAG vs Fine-Tuning

| Need | Best First Option |
|---|---|
| change response format | prompt + structured schema |
| answer private/current facts | RAG |
| use live systems | tools/function calling |
| improve style/tone | prompt or fine-tuning |
| learn repeated classification boundary | fine-tuning may help |
| reduce long prompt examples | fine-tuning may help |
| fix hallucination from missing context | RAG, not fine-tuning |
| enforce business rule | deterministic code/guardrail |

### Strong Answer

> I do not use fine-tuning to inject frequently changing knowledge. I use RAG for that. Fine-tuning is better for style, format, task behavior, and repeated patterns when I have enough high-quality examples and evals.

---

## 4. Fine-Tuning

Fine-tuning updates model behavior using training examples.

Good use cases:

- consistent output style
- domain-specific classification
- extraction patterns
- reducing prompt length
- teaching formatting preferences
- repeated task behavior

Bad use cases:

- frequently changing facts
- access-controlled knowledge
- exact database lookup
- real-time information
- replacing evals/guardrails

### Dataset Shape

Fine-tuning data often looks like:

```json
{"messages":[{"role":"system","content":"You classify tickets."},{"role":"user","content":"I was charged twice"},{"role":"assistant","content":"{\"category\":\"billing\",\"priority\":\"high\"}"}]}
```

### Rules

- start with eval dataset before training dataset
- clean labels manually
- split train/validation/test
- track data version
- evaluate against baseline model
- watch for overfitting
- do not train on secrets unless policy allows it

---

## 5. RAG + Fine-Tuning Together

They solve different problems.

```text
RAG: supplies facts
Fine-tuning: changes behavior/style/task pattern
```

Example:

- RAG retrieves policy documents.
- Fine-tuned model writes answers in company-approved support tone.

Do not fine-tune a model to memorize a policy PDF that changes every month.

---

## 6. Distillation

Distillation means using a stronger model to create training/eval data for a smaller model.

Use cases:

- reduce cost
- reduce latency
- deploy smaller model
- standardize behavior

Risks:

- teacher model errors become training data
- quality ceiling may drop
- safety behavior may degrade
- licensing/provider terms must be respected

Rule: evaluate distilled model against human-approved golden data.

---

## 7. Multimodal GenAI

Multimodal models process more than text:

- images
- PDFs/screenshots
- audio
- video frames
- charts/tables

Use cases:

- document understanding
- invoice/receipt extraction
- chart interpretation
- UI screenshot analysis
- image moderation
- voice agents
- call summarization

### Multimodal Pipeline

```text
file upload
  -> validate file type/size
  -> extract/convert if needed
  -> model input with image/audio/document
  -> structured output validation
  -> human review for high-risk outputs
```

---

## 8. Document AI vs RAG

For PDFs and forms, you may need extraction before retrieval.

```text
PDF -> OCR/layout extraction -> structured fields/table extraction -> chunks/metadata -> embeddings -> RAG
```

If the document has tables, images, or scanned text, plain text splitting may fail.

Use specialized parsers/OCR when:

- scanned PDFs
- invoices
- forms
- tables
- diagrams
- multi-column layouts

---

## 9. Synthetic Data

Synthetic data can help create training/eval examples.

Good uses:

- expand edge cases
- generate paraphrases
- create negative examples
- simulate user phrasing
- bootstrap evals

Risks:

- unrealistic examples
- model bias copied into data
- leakage from prompts
- eval becomes too similar to training

Rule: mix synthetic data with human-reviewed real examples.

---

## 10. Model Routing

Model routing sends different tasks to different models.

```text
simple classification -> cheap small model
hard reasoning -> stronger model
unsafe/high-risk -> strong model + human review
long document -> large context model
```

Benefits:

- lower cost
- lower latency
- better specialization

Risks:

- routing mistakes
- more eval complexity
- inconsistent behavior

### Router Schema

```python
from pydantic import BaseModel
from typing import Literal

class RouteDecision(BaseModel):
    task_type: Literal["simple", "complex", "safety_sensitive", "long_context"]
    selected_model: str
    reason: str
```

---

## 11. Caching

GenAI caching can reduce cost and latency.

Cache candidates:

- embeddings for unchanged chunks
- retrieval results for repeated queries
- LLM outputs for deterministic prompts
- tool results with TTL
- eval results by prompt/model/dataset version

Cache key should include:

```text
model name + prompt version + input hash + relevant settings
```

Do not cache personalized or sensitive outputs without policy.

---

## 12. Privacy And Data Governance

Questions to ask:

- Is user data sent to provider?
- Is data retained by provider?
- Is training on customer data disabled?
- Which region processes the data?
- Are prompts/logs redacted?
- Can user data be deleted?
- Are source document permissions enforced?

Production rule: legal/security requirements can dominate model choice.

---

## 13. Build vs Buy vs Open Source

| Option | Pros | Cons |
|---|---|---|
| hosted API | fast, strong models | cost, privacy, provider dependency |
| cloud managed model | enterprise controls | platform lock-in |
| open-source hosted by you | control/privacy | ops burden, quality trade-offs |
| fine-tuned hosted model | behavior fit | data prep/eval complexity |
| custom training | maximum control | very expensive and rarely needed |

Most teams should start with hosted models + RAG + evals, then optimize.

---

## 14. Advanced Failure Modes

| Failure | Example | Mitigation |
|---|---|---|
| model drift | provider changes behavior | version pinning where possible, eval monitoring |
| eval leakage | training data includes eval cases | strict dataset separation |
| overfitting fine-tune | great train score, poor real use | validation/test split |
| synthetic bias | fake data too clean | human-reviewed real data |
| multimodal extraction error | chart/table misread | human review, specialized parser |
| privacy breach | sensitive prompt logged | redaction and logging policy |
| routing error | cheap model handles hard case | router eval and fallback |

---

## 15. Java Developer Bridge

| Java/ML Platform Concept | GenAI Equivalent |
|---|---|
| model selection by SLA | choose LLM by quality/cost/latency |
| cache key design | prompt/model/input hash cache key |
| batch job training data | fine-tune dataset preparation |
| validation/test split | eval train/test separation |
| OCR/document pipeline | multimodal/document AI preprocessing |
| compliance review | provider privacy/data governance |
| feature flag rollout | prompt/model version rollout |

Key shift: model behavior is a dependency. Treat model, prompt, retrieval config, and eval dataset as versioned production artifacts.

---

## 16. Hot Interview Q&A

**Q1: When should you fine-tune?**
> When you need consistent task behavior, style, classification boundaries, or shorter prompts and you have high-quality labeled data. Do not fine-tune for frequently changing private facts; use RAG.

**Q2: What is the difference between RAG and fine-tuning?**
> RAG supplies external facts at request time. Fine-tuning changes model behavior using training examples. They can be combined.

**Q3: What is model routing?**
> Sending different requests to different models based on task complexity, safety risk, cost, latency, or context needs.

**Q4: What are multimodal GenAI use cases?**
> Document extraction, image understanding, chart interpretation, invoice processing, screenshot analysis, audio transcription/summarization, and moderation.

**Q5: What should you check before sending enterprise data to an LLM provider?**
> Data retention, training policy, region, encryption, access controls, logging/redaction, deletion requirements, and compliance constraints.

---

## 17. Final Revision Checklist

- [ ] Can choose model based on task criteria
- [ ] Can compare prompting, RAG, tools, and fine-tuning
- [ ] Can explain when fine-tuning is useful
- [ ] Can explain why fine-tuning is not for changing facts
- [ ] Can describe fine-tuning dataset rules
- [ ] Can explain distillation benefits and risks
- [ ] Can describe multimodal pipelines
- [ ] Can explain document AI preprocessing before RAG
- [ ] Can use synthetic data carefully
- [ ] Can design model routing and caching strategies
- [ ] Can discuss privacy and data governance
- [ ] Can compare hosted, managed, open-source, and custom model options
