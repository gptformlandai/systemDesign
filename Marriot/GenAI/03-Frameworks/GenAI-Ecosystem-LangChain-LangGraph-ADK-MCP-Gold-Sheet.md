# GenAI Ecosystem - LangChain, LangGraph, ADK, MCP & Frameworks - Gold Sheet

> **GenAI Python Support Stack - File 5 of 5**
> For: Java/backend developers | Level: beginner to production | Mode: know where each GenAI framework fits

---

## 1. Why This Sheet Exists

The GenAI ecosystem is noisy.

You will hear:

- LangChain
- LangGraph
- LlamaIndex
- Semantic Kernel
- AutoGen
- CrewAI
- Google ADK
- MCP
- vector databases
- rerankers
- eval frameworks
- tracing platforms

This sheet gives you the map.

The goal is not to memorize every framework. The goal is to know:

```text
What problem does this tool solve?
When should I use it?
When is it overkill?
How does it fit with Pydantic, FastAPI, NumPy, Pandas, and production modules?
```

---

## 2. Ecosystem Priority Meter

| Tool / Area | What It Solves | Priority |
|---|---|---|
| LangChain | LLM app building blocks and integrations | High |
| LangGraph | stateful agent workflows and graph orchestration | Very high |
| LlamaIndex | document ingestion and RAG data framework | High |
| ADK | agent development kit / structured agent apps | High |
| MCP | standard protocol for tools/context between models and apps | Very high |
| Vector DB clients | production retrieval | Very high |
| Eval tools | quality measurement | Very high |
| Observability tools | traces, cost, prompt debugging | High |
| Guardrails | safety and schema enforcement | High |
| FastAPI | serving GenAI APIs | Very high |

---

## 3. Mental Model: GenAI App Layers

```text
User/API
  -> FastAPI route
    -> Application service
      -> Prompt builder
      -> LLM provider client
      -> Retriever/vector store
      -> Tools/functions
      -> Agent workflow
      -> Evaluator/logging/tracing
```

Frameworks fit into different parts:

| Layer | Tools |
|---|---|
| API serving | FastAPI |
| schema validation | Pydantic |
| provider calls | httpx, provider SDKs |
| retrieval math | NumPy, vector DBs |
| dataset/eval analysis | Pandas |
| RAG orchestration | LangChain, LlamaIndex |
| agent workflows | LangGraph, ADK |
| tool protocol | MCP |
| tracing/evals | LangSmith, OpenTelemetry, custom evals |

---

## 4. Java Developer Bridge

| Java / Enterprise Concept | GenAI Python Ecosystem Equivalent |
|---|---|
| Spring AI / LangChain4j | LangChain, LlamaIndex, Semantic Kernel |
| Spring StateMachine / workflow engine | LangGraph graph state machine |
| Controller/service/repository | FastAPI/service/retriever or tool layer |
| Interface + implementation | `Protocol`, Pydantic schema, callable tools |
| External system adapter | tool function / MCP server |
| DTO schema | Pydantic model |
| Integration test | mocked provider + eval dataset |
| Observability stack | OpenTelemetry, LangSmith-style traces |

Key shift: GenAI apps are probabilistic. Architecture must include evaluation and tracing, not just request/response correctness.

---

## 5. LangChain

### What It Is

LangChain provides building blocks for LLM apps:

- prompts
- chat models
- output parsers
- retrievers
- document loaders
- text splitters
- tool wrappers
- chains
- integrations with vector stores/providers

### When To Use

Use LangChain when:

- you need many provider/vector store integrations
- you want quick RAG prototypes
- you need standard retriever/document abstractions
- you want ecosystem examples and recipes

### When Not To Use

Avoid or minimize LangChain when:

- your app only needs one simple provider call
- you need very strict control over every HTTP request
- framework abstraction hides errors/cost/latency
- your team is still learning fundamentals

### Simple Mental Model

```text
PromptTemplate -> Model -> OutputParser
Retriever -> Documents -> Prompt -> Model -> Answer
```

### Production Advice

Even if you use LangChain, keep boundaries:

```text
api -> service -> rag_pipeline -> provider/retriever adapters
```

Do not let framework objects leak everywhere.

---

## 6. LangGraph

### What It Is

LangGraph is for stateful, graph-based agent workflows.

It is useful when the app is not a simple chain but a workflow:

```text
classify request
  -> retrieve context
  -> decide whether tool needed
  -> call tool
  -> validate result
  -> retry or ask human
  -> final answer
```

### Why It Matters

Agents need state and control flow. LangGraph makes the control flow explicit.

Useful features:

- graph nodes
- conditional edges
- state object
- retries
- checkpoints
- human-in-the-loop
- durable execution patterns

### When To Use

Use LangGraph when:

- agent has multiple steps
- tool calls are conditional
- state must be inspectable
- you need retries or checkpoints
- human approval may be required
- workflow correctness matters

### When Not To Use

Do not use LangGraph for a single prompt call. It is a workflow tool, not a requirement for every LLM feature.

### Pydantic Fit

Use Pydantic or typed state schemas for graph state.

```python
from pydantic import BaseModel, Field

class AgentState(BaseModel):
    user_input: str
    retrieved_docs: list[str] = Field(default_factory=list)
    tool_errors: list[str] = Field(default_factory=list)
    final_answer: str | None = None
```

---

## 7. LlamaIndex

### What It Is

LlamaIndex focuses on data ingestion and retrieval for LLM apps.

It provides:

- document readers
- indexes
- retrievers
- query engines
- connectors to vector stores
- RAG-focused abstractions

### When To Use

Use LlamaIndex when:

- your main problem is connecting documents to LLMs
- you need data connectors
- you want quick RAG ingestion
- you need query engines over document collections

### Difference From LangChain

Simplified view:

```text
LangChain: broad LLM app orchestration and integrations
LlamaIndex: data/RAG-centric ingestion and querying
LangGraph: stateful agent workflow graphs
```

These can be combined, but do not combine them just to look advanced.

---

## 8. ADK - Agent Development Kit

### What It Is

ADK-style frameworks help structure agent applications:

- agent definitions
- tool registration
- state/session handling
- model configuration
- evaluation hooks
- deployment/runtime integration

### When To Use

Use ADK when:

- you want a structured framework for agents
- your agent needs tools, memory, sessions, and evals
- you are building a larger agentic application, not a one-off script

### Core Concepts

```text
Agent
  - instructions
  - model
  - tools
  - state/session
  - callbacks/evals
```

### Production Rule

Even with ADK, keep tool inputs/outputs validated with Pydantic and keep external system calls behind adapters.

---

## 9. MCP - Model Context Protocol

### What It Is

MCP standardizes how models/apps connect to tools and context providers.

Instead of every agent integrating every tool differently, MCP gives a common protocol for:

- tools
- resources
- prompts
- context
- tool discovery

### Mental Model

```text
MCP client  <->  MCP server
agent/app        exposes tools/resources
```

Example tool categories:

- file search
- database query
- ticket lookup
- GitHub issue search
- internal docs retrieval
- browser automation
- cloud resource inspection

### Why It Matters

MCP is valuable because tool integration becomes portable. A tool server can be reused by different agents or model clients.

### Pydantic Fit

Use schemas for tool arguments and outputs.

```python
from pydantic import BaseModel, Field

class SearchTicketsInput(BaseModel):
    query: str = Field(min_length=3)
    status: str | None = None
    limit: int = Field(default=10, ge=1, le=50)
```

### Java Developer Bridge

MCP feels like a typed service interface/protocol for AI tools. Think of it as a standardized integration boundary, not just a Python library.

---

## 10. Vector Databases And Retrieval Stores

### Common Options

| Tool | Use Case |
|---|---|
| FAISS | local/in-process vector index |
| Chroma | local/dev vector DB and simple apps |
| pgvector | Postgres + vector similarity |
| Pinecone | managed vector DB |
| Weaviate | managed/self-hosted vector DB with metadata |
| Milvus | scalable open-source vector DB |
| Elasticsearch/OpenSearch vector search | hybrid search in search stack |

### Decision Guide

Use local NumPy/FAISS when:

- learning
- small datasets
- local prototypes
- offline eval experiments

Use pgvector when:

- your data already lives in Postgres
- you need relational filters + vector search
- scale is moderate

Use managed vector DB when:

- scale/ops matter
- low-latency search matters
- team wants managed infrastructure

### RAG Rule

Vector database choice does not fix bad chunking, bad metadata, bad evals, or bad prompts.

---

## 11. Evaluation Tools

### Why Evals Matter

You cannot productionize GenAI by reading a few sample answers.

Track:

- answer correctness
- retrieval quality
- citation correctness
- groundedness
- safety
- latency
- cost
- tool success rate
- regression across prompt/model changes

### Tooling Options

| Approach | Use Case |
|---|---|
| Pandas + pytest | simple custom evals |
| DeepEval / Ragas-style tools | RAG and LLM evaluation metrics |
| promptfoo-style tools | prompt/model comparison |
| LangSmith-style traces/evals | framework-integrated observability |
| custom human review UI | high-risk outputs |

### Minimum Eval Record

```python
from pydantic import BaseModel

class EvalRecord(BaseModel):
    question_id: str
    prompt_version: str
    model: str
    answer: str
    passed: bool
    score: int
    failure_reason: str | None = None
    latency_ms: float
    cost_usd: float
```

---

## 12. Guardrails And Safety

Guardrails can include:

- schema validation
- output moderation
- prompt injection checks
- allowlisted tools
- human approval for risky actions
- retrieval source restrictions
- max token/cost limits
- rate limits
- audit logs

### Tool Safety Pattern

```text
LLM proposes tool call
  -> validate args with Pydantic
  -> authorize action
  -> execute tool
  -> validate tool output
  -> log audit event
```

Never let an agent directly perform irreversible actions without authorization and audit.

---

## 13. Framework Selection Guide

| Scenario | Recommended Starting Point |
|---|---|
| One LLM API call | `httpx` + Pydantic |
| Structured output API | FastAPI + Pydantic + provider SDK/httpx |
| Simple RAG | LlamaIndex or small custom pipeline |
| RAG with many integrations | LangChain or LlamaIndex |
| Stateful multi-step agent | LangGraph or ADK |
| Tool interoperability | MCP |
| Local eval analysis | Pandas + pytest |
| Embedding experiments | NumPy + FAISS/Chroma |
| Production observability | OpenTelemetry + structured logs + traces |

### Senior Rule

Start simple. Add frameworks when they remove real complexity, not because they are popular.

---

## 14. Reference Architecture

```text
app/
  api/
    chat_routes.py
  schemas/
    chat.py
    tool.py
  services/
    chat_service.py
    rag_service.py
    eval_service.py
  genai/
    prompts.py
    provider_client.py
    output_parsers.py
    token_budget.py
  retrieval/
    chunker.py
    embedder.py
    vector_store.py
    reranker.py
  agents/
    state.py
    graph.py
    tools.py
  evals/
    datasets.py
    metrics.py
    runner.py
  core/
    config.py
    logging.py
    tracing.py
```

Dependency direction:

```text
api -> services -> genai/retrieval/agents/evals -> infrastructure/provider clients
```

Do not let prompts, provider SDKs, and vector DB calls scatter through route handlers.

---

## 15. Common Ecosystem Traps

| Trap | Why It Hurts | Better Approach |
|---|---|---|
| Starting with heavy framework before fundamentals | hard to debug | learn Pydantic/httpx/NumPy/Pandas first |
| No evals | cannot know if change improved quality | create eval dataset early |
| Agent for simple workflow | unnecessary latency and unpredictability | simple chain/service first |
| Tool calls without validation | unsafe actions | Pydantic schema + authorization |
| No tracing | impossible to debug multi-step failure | structured traces/logs |
| Vector DB treated as magic | poor retrieval still poor | improve chunking, metadata, evals |
| Mixing framework objects into API everywhere | tight coupling | keep service boundaries |
| No provider abstraction | hard to swap/test models | provider client interface |

---

## 16. Hot Interview Q&A

**Q1: When should you use LangGraph instead of LangChain chains?**
> Use LangGraph when the workflow is stateful and branching: conditional tool calls, retries, human approval, checkpoints, or multi-step agent state. A simple prompt -> model -> parser flow does not need LangGraph.

**Q2: What is MCP?**
> MCP is a protocol for connecting models/apps to tools and context providers in a standardized way. It lets tools be discovered and called through a common interface rather than custom integration for every agent.

**Q3: What is the difference between LlamaIndex and LangChain?**
> LlamaIndex is more data/RAG-centric: ingestion, indexes, retrievers, query engines. LangChain is broader LLM app orchestration and integration. They overlap, but the starting mental model differs.

**Q4: Should every GenAI app use a framework?**
> No. For simple structured output or one provider call, plain `httpx`, Pydantic, and FastAPI are often cleaner. Use frameworks when they remove real orchestration or integration complexity.

**Q5: What makes a GenAI system production-ready?**
> Validation, timeouts, retries, concurrency limits, evals, tracing, cost tracking, safe tool execution, prompt/model versioning, and clear service boundaries.

---

## 17. Final Revision Checklist

- [ ] Can explain LangChain's role in LLM apps
- [ ] Can explain why LangGraph is useful for stateful agents
- [ ] Can explain where LlamaIndex fits in RAG
- [ ] Can explain ADK as an agent app structure
- [ ] Can explain MCP as a tool/context protocol
- [ ] Can choose between custom code, LangChain, LlamaIndex, LangGraph, and ADK
- [ ] Can list vector DB options and when to use each
- [ ] Can define a minimum eval record
- [ ] Can describe guardrails for tool execution
- [ ] Can design a clean GenAI project structure
- [ ] Can avoid framework-first overengineering
