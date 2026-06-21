# GenAI 2-Week & 6-Week Mastery Roadmap

> **GenAI Mastery Track - Practice File**
> For: Java/backend developers and all learners | Level: beginner to production | Mode: structured study + projects + readiness gates

---

## 1. How To Use This Roadmap

This roadmap turns the GenAI notes into a study plan.

Choose the path based on time:

| Situation | Plan |
|---|---|
| Interview/project in 2 weeks | 2-Week GenAI Sprint |
| You want real production mastery | 6-Week Deep Mastery |
| You already know Python well | Start at Week 2 of 6-week plan |
| You already built simple LLM apps | Start at RAG + agents weeks |

Daily rule:

```text
Read -> implement mini-lab -> write 5 recall answers -> update project
```

Do not only read. GenAI mastery requires running evals and building systems.

---

## 2. Track Files

| Order | File | Purpose |
|---:|---|---|
| 1 | `GenAI-Python-Support-Stack-Index.md` | Orientation and module map |
| 2 | `04-GenAI-Core/LLM-Fundamentals-Prompting-Context-Tokens-Gold-Sheet.md` | LLM basics, prompting, context, tokens |
| 3 | `01-Core-Modules/Pydantic-For-GenAI-Structured-Outputs-Gold-Sheet.md` | Structured outputs and validation |
| 4 | `01-Core-Modules/NumPy-For-GenAI-Embeddings-Vector-Math-Gold-Sheet.md` | Embedding math and top-k retrieval |
| 5 | `04-GenAI-Core/RAG-Retrieval-Augmented-Generation-Mastery-Gold-Sheet.md` | RAG architecture and retrieval quality |
| 6 | `01-Core-Modules/Pandas-For-GenAI-Datasets-Evals-Gold-Sheet.md` | Datasets, eval reports, failure analysis |
| 7 | `04-GenAI-Core/Evals-Safety-Observability-Cost-Guardrails-Gold-Sheet.md` | Evals, safety, metrics, release gates |
| 8 | `02-Production-Modules/GenAI-Supporting-Python-Modules-Gold-Sheet.md` | `httpx`, `asyncio`, retries, logging, tests |
| 9 | `04-GenAI-Core/Agents-Tool-Calling-LangGraph-ADK-MCP-Mastery-Gold-Sheet.md` | Agents, tools, workflows, MCP |
| 10 | `03-Frameworks/GenAI-Ecosystem-LangChain-LangGraph-ADK-MCP-Gold-Sheet.md` | Framework selection and ecosystem map |
| 11 | `04-GenAI-Core/Fine-Tuning-Multimodal-Model-Selection-GenAI-Advanced-Gold-Sheet.md` | Fine-tuning, multimodal, routing, advanced choices |

---

## 3. 2-Week GenAI Sprint

**Total time:** 2 hours/day for 14 days = 28 hours  
**Goal:** Build a working RAG + structured output + eval + agent-aware foundation.

### Day 1 - LLM Fundamentals

| Time | Activity |
|---|---|
| 0:00-0:45 | Read LLM Fundamentals sections 1-8 |
| 0:45-1:15 | Write 3 prompts: classification, extraction, RAG answer |
| 1:15-1:45 | Create a structured prompt with JSON output |
| 1:45-2:00 | Explain tokens, context window, temperature out loud |

Exit gate:

- Can explain why LLMs hallucinate
- Can write a production-style prompt with output schema

---

### Day 2 - Pydantic Structured Outputs

| Time | Activity |
|---|---|
| 0:00-0:45 | Read Pydantic GenAI sheet |
| 0:45-1:20 | Implement ticket classifier schema |
| 1:20-1:45 | Add validators and invalid-output tests |
| 1:45-2:00 | Write Java bridge notes: DTO + Bean Validation mapping |

Exit gate:

- Can validate raw LLM JSON with Pydantic
- Can model tool input schema

---

### Day 3 - NumPy Embedding Math

| Time | Activity |
|---|---|
| 0:00-0:45 | Read NumPy embedding sheet |
| 0:45-1:30 | Implement cosine similarity and top-k retrieval |
| 1:30-1:50 | Compare full sort vs `argpartition` |
| 1:50-2:00 | Explain vector DB need at scale |

Exit gate:

- Can compute cosine similarity
- Can explain normalized vectors and top-k retrieval

---

### Day 4 - RAG Architecture

| Time | Activity |
|---|---|
| 0:00-0:55 | Read RAG sections 1-8 |
| 0:55-1:30 | Design ingestion and query pipeline for 20 markdown docs |
| 1:30-1:50 | Define chunk metadata schema |
| 1:50-2:00 | Explain dense vs sparse vs hybrid retrieval |

Exit gate:

- Can draw offline ingestion and online query pipeline
- Can choose chunking strategy and metadata

---

### Day 5 - RAG Evaluation + Pandas

| Time | Activity |
|---|---|
| 0:00-0:40 | Read Pandas eval sheet |
| 0:40-1:20 | Build 20-row golden RAG eval CSV |
| 1:20-1:45 | Compute pass rate by topic and failure reason |
| 1:45-2:00 | Define retrieval vs answer metrics |

Exit gate:

- Can create eval dataset
- Can analyze failures by topic/model/prompt version

---

### Day 6 - Evals, Safety, Guardrails

| Time | Activity |
|---|---|
| 0:00-0:55 | Read evals/safety sheet |
| 0:55-1:25 | Add 5 prompt-injection eval cases |
| 1:25-1:45 | Define release gates for a RAG change |
| 1:45-2:00 | List dashboard metrics |

Exit gate:

- Can explain tests vs evals
- Can design prompt injection safety cases

---

### Day 7 - Production Modules

| Time | Activity |
|---|---|
| 0:00-0:50 | Read support modules sheet |
| 0:50-1:30 | Build async `httpx` LLM client skeleton with timeout/retry |
| 1:30-1:50 | Add Pydantic response validation |
| 1:50-2:00 | Explain `requests` vs `httpx.AsyncClient` in FastAPI |

Exit gate:

- Can design reliable LLM client
- Can explain retries, timeout, concurrency caps

---

### Day 8 - FastAPI GenAI Service

Build a tiny service:

```text
POST /classify
POST /rag/search
POST /eval/run
```

Use:

- FastAPI
- Pydantic schemas
- fake LLM client
- fake retriever
- structured logs

Exit gate:

- routes are thin
- service layer owns logic
- dependencies can be overridden in tests

---

### Day 9 - RAG Project Build

Build local RAG over 10-20 documents:

```text
load docs -> chunk -> fake/real embeddings -> top-k search -> answer prompt
```

Minimum acceptable:

- chunk metadata
- top-k retrieval
- answer with source IDs
- eval dataset with 10 questions

---

### Day 10 - Agents and Tools

| Time | Activity |
|---|---|
| 0:00-0:55 | Read agents sheet |
| 0:55-1:25 | Define 3 tools with Pydantic schemas |
| 1:25-1:45 | Add approval rule for risky tool |
| 1:45-2:00 | Design agent eval record |

Exit gate:

- Can explain when an agent is overkill
- Can make tool execution safe

---

### Day 11 - Framework Ecosystem

| Time | Activity |
|---|---|
| 0:00-0:45 | Read ecosystem sheet |
| 0:45-1:15 | Decide: custom code vs LangChain vs LangGraph vs ADK for your project |
| 1:15-1:45 | Sketch MCP tool server idea |
| 1:45-2:00 | Explain LangChain vs LangGraph vs MCP |

Exit gate:

- Can choose frameworks based on need, not hype

---

### Day 12 - Fine-Tuning, Multimodal, Model Selection

| Time | Activity |
|---|---|
| 0:00-0:55 | Read advanced sheet |
| 0:55-1:25 | Compare prompt vs RAG vs fine-tuning for 5 scenarios |
| 1:25-1:45 | Design model-routing rules |
| 1:45-2:00 | Write privacy checklist |

Exit gate:

- Can explain when fine-tuning is useful and when it is wrong

---

### Day 13 - Full Project Integration

Complete one mini-project:

```text
FastAPI endpoint
  -> Pydantic request
  -> service layer
  -> retriever/LLM client
  -> Pydantic response
  -> eval runner
  -> logging and cost fields
```

Exit gate:

- can demo one end-to-end flow
- can run evals locally

---

### Day 14 - Mock Review and Readiness Gate

Answer these out loud:

1. What is RAG and when should you not use it?
2. How do you evaluate a GenAI system?
3. How do you prevent prompt injection?
4. When should you fine-tune?
5. How do LangGraph and MCP fit?
6. How do you make an LLM client production-safe?
7. How do you control cost and latency?
8. How do you structure a FastAPI GenAI service?

Readiness gate:

- 6/8 answers strong without notes
- mini-project runs
- eval dataset has at least 10 cases
- safety eval has at least 5 cases

---

## 4. 6-Week Deep Mastery Plan

### Week 1 - LLM Foundations + Structured Output

Deliverables:

- 20 classification/extraction prompts
- Pydantic schemas for every output
- validation failure handling
- prompt version naming convention

Files:

- LLM Fundamentals
- Pydantic for GenAI

Milestone:

- You can build deterministic structured-output workflows from probabilistic model output.

---

### Week 2 - Embeddings + RAG Basics

Deliverables:

- cosine similarity implementation
- local top-k retrieval script
- 50-document ingestion pipeline
- chunk metadata schema

Files:

- NumPy for embeddings
- RAG mastery

Milestone:

- You can explain and implement basic retrieval without a framework.

---

### Week 3 - RAG Quality + Evals

Deliverables:

- 50-case golden eval dataset
- retrieval recall@k report
- answer correctness report
- citation accuracy checks
- failure dashboard with Pandas

Files:

- Pandas for evals
- Evals/safety/observability
- RAG mastery

Milestone:

- You can improve RAG based on measurements, not guesses.

---

### Week 4 - Production GenAI Services

Deliverables:

- FastAPI GenAI service
- async provider client
- retries/timeouts/concurrency cap
- request ID logging
- mocked provider tests
- config via settings

Files:

- Supporting Python modules
- Python enterprise FastAPI material from `Marriot/Python`

Milestone:

- You can serve GenAI features as production backend APIs.

---

### Week 5 - Agents, Tools, MCP, Frameworks

Deliverables:

- 5 tool schemas
- agent state model
- graph workflow design
- tool safety policy
- agent eval dataset
- MCP tool idea or prototype design

Files:

- Agents mastery
- Ecosystem frameworks

Milestone:

- You can design controlled agentic workflows and explain when not to use them.

---

### Week 6 - Advanced GenAI + Capstone

Deliverables:

- model selection matrix
- prompt vs RAG vs fine-tune decision table
- multimodal/document AI design
- cost/latency dashboard sketch
- capstone demo and review

Files:

- Fine-tuning/multimodal/model selection
- all previous files for revision

Milestone:

- You can make architecture decisions for real GenAI systems.

---

## 5. Capstone Projects

### Beginner Capstone - Ticket Classifier

Build:

- Pydantic schema
- prompt template
- fake or real provider call
- validation failure handling
- CSV eval dataset

Pass gate:

- 20 eval cases
- 80 percent pass rate
- invalid outputs handled safely

---

### Intermediate Capstone - Internal Docs RAG

Build:

- load markdown/PDF docs
- chunk and metadata
- embeddings or fake embeddings
- vector search
- RAG answer with citations
- eval runner

Pass gate:

- 50 eval questions
- retrieval recall@5 measured
- citation accuracy measured
- prompt injection evals included

---

### Advanced Capstone - Agentic Support Assistant

Build:

- FastAPI endpoint
- RAG tool
- ticket lookup tool
- draft response tool
- human approval gate
- agent state model
- eval suite
- structured traces/logs

Pass gate:

- tool calls validated
- risky actions require approval
- max step count enforced
- cost/latency tracked
- 20 agent scenarios evaluated

---

## 6. Mastery Scoring Rubric

| Dimension | 1 | 3 | 5 |
|---|---|---|---|
| LLM fundamentals | knows API calls only | explains tokens/context/sampling | designs prompt contracts with cost/latency trade-offs |
| Structured output | trusts JSON | validates schemas | handles failures, versions schemas, tests edge cases |
| RAG | knows vector DB buzzwords | builds basic RAG | evaluates retrieval, citations, security, freshness |
| Evals | manual spot checks | golden dataset | regression gates, safety evals, dashboards |
| Production | demo scripts | FastAPI + retries | observable, tested, budgeted, secure service |
| Agents | uses agent framework | safe tools and state | graph workflows, approval gates, agent evals |
| Advanced | knows fine-tuning exists | chooses RAG vs fine-tune | model routing, privacy, multimodal architecture |

Target:

- Beginner readiness: all dimensions at 3
- Production readiness: core dimensions at 4+
- MAANG/product engineer readiness: most dimensions at 5 with capstone proof

---

## 7. Final Revision Checklist

- [ ] Completed 2-week sprint or 6-week plan
- [ ] Built at least one structured output app
- [ ] Built at least one RAG pipeline
- [ ] Created eval dataset with real pass/fail analysis
- [ ] Added prompt injection/safety evals
- [ ] Built or designed an agent workflow with safe tools
- [ ] Can explain LangChain vs LangGraph vs ADK vs MCP
- [ ] Can explain when to fine-tune and when not to
- [ ] Can discuss cost, latency, privacy, and observability
- [ ] Completed at least one capstone project
