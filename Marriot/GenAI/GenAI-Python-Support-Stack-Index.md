# GenAI Python Support Stack & Mastery Track Index

This folder is a GenAI-focused mastery track.

Audience:
- You are a Java/backend developer moving into GenAI engineering.
- You want to know which Python libraries matter for real GenAI applications.
- You also want the core GenAI concepts: LLM fundamentals, prompting, RAG, agents, evals, safety, fine-tuning, multimodal, and production architecture.
- You do not want random Python notes; you want a sequenced track that supports LLM apps, RAG, agents, evals, APIs, and production systems.

Goal:
- Learn the Python modules that sit underneath modern GenAI systems.
- Understand the GenAI architecture patterns those modules support.
- Build practical intuition for LLM prompting, structured outputs, embeddings, vector math, RAG, data preparation, evaluation, API serving, retries, settings, observability, guardrails, agents, and advanced model choices.
- Keep the learning path useful for all levels: beginner -> intermediate -> production GenAI engineer.

---

## 1. What This Track Is

This is not a generic Python course.

This is a **GenAI support stack + mastery** course: the concepts and Python tools you repeatedly use when building:

- LLM chat APIs
- RAG pipelines
- embedding search
- document ingestion
- structured outputs
- tool/function calling
- agent workflows
- evaluation datasets
- prompt test harnesses
- production GenAI services
- internal copilots
- MCP tool servers
- model selection and routing systems
- fine-tuning and multimodal design discussions

The track is split into two layers:

| Layer | Purpose |
|---|---|
| Python support stack | Pydantic, NumPy, Pandas, async clients, retries, settings, logging, tests |
| GenAI mastery layer | LLM fundamentals, RAG, agents, evals/safety, advanced model choices, roadmap/projects |

---

## 2. Learning Order

| Order | File | Why It Matters For GenAI |
|---:|---|---|
| 1 | `04-GenAI-Core/LLM-Fundamentals-Prompting-Context-Tokens-Gold-Sheet.md` | Explains LLM behavior, tokens, context windows, prompting, hallucination, structured output, and tool use |
| 2 | `01-Core-Modules/Pydantic-For-GenAI-Structured-Outputs-Gold-Sheet.md` | Validates LLM inputs/outputs, tool schemas, API contracts, config, structured responses |
| 3 | `01-Core-Modules/NumPy-For-GenAI-Embeddings-Vector-Math-Gold-Sheet.md` | Powers embedding similarity, cosine distance, vector normalization, batching, retrieval scoring |
| 4 | `04-GenAI-Core/RAG-Retrieval-Augmented-Generation-Mastery-Gold-Sheet.md` | Teaches ingestion, chunking, embeddings, vector stores, hybrid retrieval, reranking, citations, and RAG evals |
| 5 | `01-Core-Modules/Pandas-For-GenAI-Datasets-Evals-Gold-Sheet.md` | Cleans datasets, prepares eval sets, analyzes prompt experiments, builds ingestion QA reports |
| 6 | `04-GenAI-Core/Evals-Safety-Observability-Cost-Guardrails-Gold-Sheet.md` | Covers eval datasets, LLM-as-judge, prompt injection tests, guardrails, observability, cost, latency, and release gates |
| 7 | `02-Production-Modules/GenAI-Supporting-Python-Modules-Gold-Sheet.md` | Covers `httpx`, `asyncio`, `tenacity`, settings, logging, `orjson`, `tiktoken`, FastAPI serving, testing |
| 8 | `04-GenAI-Core/Agents-Tool-Calling-LangGraph-ADK-MCP-Mastery-Gold-Sheet.md` | Explains agents, tool schemas, tool safety, explicit state, LangGraph, ADK, MCP, and agent evals |
| 9 | `03-Frameworks/GenAI-Ecosystem-LangChain-LangGraph-ADK-MCP-Gold-Sheet.md` | Explains where LangChain, LangGraph, ADK, MCP, vector DB clients, and eval tools fit |
| 10 | `04-GenAI-Core/Fine-Tuning-Multimodal-Model-Selection-GenAI-Advanced-Gold-Sheet.md` | Covers model selection, fine-tuning, distillation, multimodal, model routing, caching, privacy, and build-vs-buy choices |
| 11 | `05-Practice-Roadmaps/GenAI-2-Week-6-Week-Mastery-Roadmap.md` | Converts the track into 2-week and 6-week plans with capstone projects and readiness gates |

Recommended path:

```text
LLM Fundamentals -> Pydantic -> NumPy -> RAG -> Pandas/Evals -> Production Modules -> Agents -> Frameworks -> Advanced -> Roadmap/Capstone
```

Why this order:

1. LLM fundamentals teach what the model can and cannot do.
2. Pydantic teaches shape, validation, and contracts.
3. NumPy teaches embedding math.
4. RAG teaches how private/current knowledge enters the system.
5. Pandas and evals teach measurement and regression control.
6. Support modules teach production reliability.
7. Agents and frameworks teach orchestration once fundamentals are clear.
8. Advanced topics teach when to fine-tune, route models, or use multimodal workflows.

---

## 3. Module Map By GenAI Use Case

| GenAI Use Case | Primary Modules |
|---|---|
| LLM prompting and context design | prompt templates, tokenizers, provider SDKs/APIs |
| Structured LLM output | Pydantic, json, orjson |
| Tool/function calling | Pydantic, typing, inspect |
| RAG chunk metadata | Pydantic, pathlib, hashlib |
| Embedding similarity | NumPy |
| Top-k retrieval scoring | NumPy, heapq |
| RAG retrieval and citations | vector DB clients, rerankers, Pydantic metadata schemas |
| Offline eval dataset | Pandas, jsonlines, pathlib |
| Prompt experiment tracking | Pandas, rich, matplotlib optional |
| LLM API calls | httpx, asyncio |
| Retry/backoff | tenacity |
| API serving | FastAPI, Pydantic, httpx |
| Streaming responses | FastAPI, Starlette, asyncio |
| Config/secrets | pydantic-settings, python-dotenv, os |
| Token counting | tiktoken or provider tokenizer |
| Observability | logging, structlog, OpenTelemetry |
| CLI tools | typer, rich |
| Tests/evals | pytest, pytest-asyncio, respx |
| Safety and guardrails | Pydantic validation, moderation APIs, policy checks, eval suites |
| RAG framework | LangChain, LlamaIndex |
| Agent workflow | LangGraph, ADK |
| Tool interoperability | MCP |
| Fine-tuning datasets | JSONL, Pandas, Pydantic validation |
| Multimodal document understanding | OCR/parsers, provider multimodal APIs, structured extraction schemas |

---

## 4. Beginner To Production Milestones

### Beginner Milestone

You can:

- explain LLMs as token-based probabilistic systems
- define a Pydantic model for an LLM response
- validate malformed JSON safely
- compute cosine similarity between two vectors
- load an evaluation CSV with Pandas
- call an LLM API with `httpx`
- explain when RAG is needed

### Intermediate Milestone

You can:

- design tool schemas using Pydantic
- batch embedding requests
- normalize vectors and compute top-k retrieval
- build a small RAG eval dataset
- compare dense, sparse, and hybrid retrieval
- define golden eval cases and safety cases
- retry transient LLM errors with backoff
- serve a chat endpoint with FastAPI

### Production Milestone

You can:

- separate API schemas, domain models, provider clients, retrievers, and evaluators
- validate every external boundary
- stream responses safely
- cap concurrency and protect downstream APIs
- track latency, token usage, cost, retrieval quality, and answer quality
- enforce retrieval ACLs before model context assembly
- run regression evals before prompt/model/retrieval changes ship
- build agent workflows with state, retries, checkpoints, and tool safety
- expose tools through MCP or consume MCP tools from an agent
- explain when to use prompting, RAG, tools, fine-tuning, model routing, or multimodal workflows

---

## 5. Java Developer Bridge

| Java / Spring Concept | GenAI Python Equivalent |
|---|---|
| DTO / Bean Validation | Pydantic model + validators |
| Jackson JSON mapping | Pydantic + `model_validate_json` / `model_dump_json` |
| Service layer | plain Python class/function |
| Repository | vector store / document store adapter |
| WebClient | `httpx.AsyncClient` |
| Resilience4j retry | `tenacity` |
| application.yml | `pydantic-settings` + environment variables |
| SLF4J MDC | `ContextVar` + structured logging |
| JUnit | pytest |
| Mockito | `unittest.mock`, `respx` for HTTP mocking |
| Spring AI / LangChain4j | LangChain, LangGraph, ADK, LlamaIndex |
| Tool/function interface | Pydantic schema + callable + MCP tool definition |

### Key Mental Shift

In Java, frameworks often enforce structure. In Python GenAI systems, structure is mostly discipline:

```text
API layer -> application service -> LLM client -> retriever/tool/evaluator
```

Keep boundaries explicit or the app becomes a pile of prompts, globals, and untestable provider calls.

---

## 6. Suggested Mini-Project Path

### Project 1: Structured Output Extractor

Build:
- Input: raw support ticket text
- Output: Pydantic `TicketClassification`
- Validate malformed provider output
- Save results to JSONL

Modules:
- Pydantic
- httpx
- tenacity
- orjson

### Project 2: Embedding Search Notebook/Script

Build:
- Load 100 documents
- Create fake or real embeddings
- Normalize with NumPy
- Compute cosine similarity
- Return top 5 chunks

Modules:
- NumPy
- Pandas
- pathlib
- heapq

### Project 3: Local RAG System

Build:
- Load 20-50 documents
- Chunk by heading or token count
- Store metadata: source, section, chunk index, hash
- Retrieve top-k chunks
- Generate answer with citations
- Return unknown when context is insufficient

Modules:
- Pydantic
- NumPy or vector DB client
- pathlib
- httpx/provider client

### Project 4: RAG Eval Dataset

Build:
- CSV with question, expected answer, retrieved context, model answer
- Pandas analysis of pass/fail rate
- Group by topic and failure reason
- Add prompt injection and citation accuracy cases

Modules:
- Pandas
- Pydantic
- pytest

### Project 5: FastAPI GenAI Service

Build:
- `/chat`
- `/rag/search`
- `/eval/run`
- request IDs, timeout, retry, structured logs

Modules:
- FastAPI
- Pydantic
- httpx
- asyncio
- tenacity
- logging

### Project 6: Agent With Tools

Build:
- Agent state schema
- Search tool
- Calculator tool
- Human approval gate
- MCP tool exposure or MCP client usage
- Agent eval scenarios

Modules:
- LangGraph or ADK
- Pydantic
- MCP
- pytest

### Project 7: Advanced Decision Review

Build:
- Prompt vs RAG vs fine-tuning decision table
- Model selection matrix
- Cost and latency budget
- Privacy/data-governance checklist
- Multimodal/document AI architecture sketch

Modules:
- Pandas
- Pydantic
- provider SDK/API docs
- evaluation reports

---

## 7. Final Revision Checklist

- [ ] Can explain why Pydantic matters for structured LLM output
- [ ] Can explain LLM tokens, context windows, temperature, and hallucination
- [ ] Can compute cosine similarity with NumPy
- [ ] Can use Pandas for eval datasets and failure analysis
- [ ] Can call LLM APIs with `httpx.AsyncClient`
- [ ] Can retry transient failures with `tenacity`
- [ ] Can count/estimate tokens before sending large prompts
- [ ] Can serve GenAI endpoints with FastAPI without blocking the event loop
- [ ] Can structure a RAG app into loader, chunker, embedder, retriever, generator, evaluator
- [ ] Can evaluate retrieval, groundedness, citation accuracy, safety, latency, and cost
- [ ] Can design prompt injection evals and guardrails
- [ ] Can explain when to use LangChain vs LangGraph vs ADK vs MCP
- [ ] Can design safe agent tools with authorization and approval gates
- [ ] Can explain when to fine-tune and when not to
- [ ] Can discuss model routing, multimodal pipelines, privacy, and build-vs-buy choices
- [ ] Can test GenAI code without calling real providers on every test run
