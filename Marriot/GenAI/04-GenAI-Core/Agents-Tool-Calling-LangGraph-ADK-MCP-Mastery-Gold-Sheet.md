# Agents, Tool Calling, LangGraph, ADK & MCP Mastery Gold Sheet

> **GenAI Mastery Track - Core File 3**
> For: Java/backend developers and all GenAI learners | Level: intermediate to production | Mode: build controlled agentic systems

---

## 1. What An Agent Is

An agent is an LLM-driven workflow that can decide actions, call tools, observe results, and continue until a goal is complete.

Basic loop:

```text
user goal
  -> model reasons/plans
  -> chooses tool
  -> tool executes
  -> model observes result
  -> repeats or finalizes
```

### Strong Answer

> An agent is not just a chatbot. It is a controlled workflow where the model can choose actions and use tools. Production agent design is about state, tool safety, validation, observability, retries, and human approval for risky actions.

---

## 2. When To Use Agents

Use agents when:

- workflow has multiple steps
- tool choice is dynamic
- plan depends on intermediate results
- user goal is open-ended
- human approval may be needed
- state/checkpoints matter

Do not use agents when:

- one prompt is enough
- deterministic code is better
- a simple RAG pipeline answers the question
- the workflow is safety-critical without approvals
- latency and cost must be minimal

### Senior Rule

Start with deterministic workflow. Add agentic choice only where it creates real value.

---

## 3. Tool Calling

A tool is a function the model can ask the system to execute.

Examples:

- search documents
- query database
- create ticket
- calculate price
- send email draft
- inspect file
- call internal API

### Tool Schema

```python
from pydantic import BaseModel, Field

class SearchDocsInput(BaseModel):
    query: str = Field(min_length=3)
    top_k: int = Field(default=5, ge=1, le=20)
```

### Tool Function

```python
async def search_docs(args: SearchDocsInput) -> list[dict]:
    return await retriever.search(args.query, top_k=args.top_k)
```

### Tool Execution Pattern

```text
LLM proposes args
  -> Pydantic validates args
  -> policy authorizes tool
  -> tool executes
  -> output validated
  -> result returned to model
```

---

## 4. Tool Safety

Tools can create real-world side effects.

Risk levels:

| Tool Type | Risk | Examples |
|---|---|---|
| read-only | low/medium | search docs, get ticket |
| write draft | medium | draft email, create proposed change |
| write action | high | send email, update DB, create payment |
| external irreversible | critical | delete resource, execute trade, change permissions |

Rules:

- validate inputs
- authorize user/action
- require human approval for high-risk tools
- add idempotency keys for writes
- log audit events
- rate limit tools
- return structured errors

---

## 5. Agent State

Agents need explicit state.

```python
from pydantic import BaseModel, Field
from typing import Literal

class ToolCallRecord(BaseModel):
    tool_name: str
    args: dict
    result_summary: str | None = None
    error: str | None = None

class AgentState(BaseModel):
    user_goal: str
    plan: list[str] = Field(default_factory=list)
    tool_calls: list[ToolCallRecord] = Field(default_factory=list)
    current_step: str | None = None
    final_answer: str | None = None
    status: Literal["running", "needs_approval", "done", "failed"] = "running"
```

State makes workflows:

- inspectable
- testable
- checkpointable
- resumable
- debuggable

---

## 6. LangGraph Mental Model

LangGraph models agent workflows as graphs.

```text
node: classify_intent
node: retrieve_docs
node: call_tool
node: human_approval
node: final_answer
edge: classify -> retrieve OR call_tool
```

Use LangGraph for:

- branching logic
- multi-step agents
- retry loops
- checkpointing
- human-in-the-loop
- stateful workflows

Do not use it for a single LLM call.

### Graph Design Rule

Each node should do one clear thing:

```text
bad node: do_everything
better nodes: classify -> retrieve -> generate -> validate -> finalize
```

---

## 7. ADK Mental Model

ADK-style frameworks provide structure for agent apps:

```text
Agent
  instructions
  model
  tools
  state/session
  callbacks
  eval hooks
```

Use ADK when:

- you want an opinionated agent framework
- you need tool registration and sessions
- you are building a larger agent app
- deployment/runtime integration matters

Still keep fundamentals:

- Pydantic schemas
- explicit tool policies
- evals
- logs/traces
- service boundaries

---

## 8. MCP Mental Model

MCP standardizes tool/context integration.

```text
MCP client  <->  MCP server
agent/app        tools/resources/prompts
```

MCP server can expose:

- tools: actions the model can call
- resources: read-only data/context
- prompts: reusable prompt templates

### Why MCP Matters

Without a protocol, every agent-tool integration is custom. MCP makes tools portable across compatible clients.

### Tool Boundary

MCP does not remove the need for:

- authentication
- authorization
- input validation
- output validation
- audit logging
- rate limiting

---

## 9. Planning Patterns

| Pattern | Description | Use Case |
|---|---|---|
| single-step tool call | model chooses one tool | simple lookup/action |
| plan-and-execute | model creates plan then executes | multi-step research |
| ReAct | reason/action/observation loop | dynamic tool use |
| router agent | chooses specialized path | classify task type |
| supervisor-worker | supervisor delegates to specialists | complex workflows |
| human-in-loop | approval before risky action | enterprise safety |

Production warning: More agent complexity means more eval complexity.

---

## 10. Agent Observability

Log/trace every step:

- user request ID
- agent state version
- node name
- tool name
- tool args summary
- tool result summary
- latency
- token usage
- model name
- retry count
- error type
- approval decision

A failed agent without traces is almost impossible to debug.

---

## 11. Agent Evaluation

Evaluate agents at multiple levels:

| Level | What To Measure |
|---|---|
| final answer | correctness, groundedness, safety |
| tool choice | selected correct tool? |
| tool args | valid and minimal? |
| workflow path | followed expected route? |
| recovery | handled tool failure? |
| cost/latency | acceptable? |
| approval | asked human when needed? |

### Eval Record

```python
class AgentEvalRecord(BaseModel):
    scenario_id: str
    expected_tools: list[str]
    actual_tools: list[str]
    final_answer_passed: bool
    tool_path_passed: bool
    safety_passed: bool
    latency_ms: float
    cost_usd: float
```

---

## 12. Common Agent Failure Modes

| Failure | Cause | Fix |
|---|---|---|
| infinite loop | no step limit | max steps + loop detection |
| wrong tool | weak tool descriptions | better schema/descriptions/evals |
| unsafe action | missing policy gate | authorization + human approval |
| bad args | no validation | Pydantic input schemas |
| tool output misread | unstructured results | structured tool outputs |
| high cost | too many loops/model calls | budget caps + simpler workflow |
| untestable behavior | no state/traces | explicit state and eval records |
| prompt injection tool hijack | untrusted content treated as instruction | context isolation and tool policies |

---

## 13. Reference Agent Architecture

```text
api/
  agent_routes.py
agents/
  state.py
  graph.py
  policies.py
  prompts.py
  tools.py
  approvals.py
tools/
  search_docs.py
  ticket_client.py
  calculator.py
evals/
  agent_eval_dataset.py
  agent_eval_runner.py
core/
  logging.py
  tracing.py
```

Dependency rule:

```text
API calls agent service.
Agent service owns graph/workflow.
Tools call external systems.
Policies authorize tools.
Evals test scenarios.
```

---

## 14. Java Developer Bridge

| Java/Enterprise Concept | Agent Concept |
|---|---|
| workflow engine | LangGraph/ADK workflow |
| service method | tool function |
| method args DTO | Pydantic tool schema |
| authorization interceptor | tool policy gate |
| audit log | tool call trace |
| saga/orchestration | multi-step agent graph |
| circuit breaker | tool/provider failure handling |
| integration test | agent scenario eval |

Key shift: agents are partially model-directed workflows. You must design guardrails like you would design controls around any non-deterministic distributed system.

---

## 15. Hot Interview Q&A

**Q1: What is the difference between RAG and an agent?**
> RAG retrieves context and generates an answer. An agent can choose actions/tools over multiple steps. A RAG system may be one tool inside an agent.

**Q2: When should you use LangGraph?**
> When you need explicit stateful workflow control: branching, retries, checkpoints, human approval, or multi-step tool use. Not for a single prompt.

**Q3: What is MCP?**
> MCP is a protocol for exposing tools/resources/prompts to model applications through a standardized interface.

**Q4: How do you make tools safe?**
> Validate args, authorize action, constrain permissions, require approval for risky writes, use idempotency keys, validate output, and audit every call.

**Q5: How do you evaluate an agent?**
> Check final answer, tool choice, tool arguments, workflow path, safety behavior, recovery from failures, latency, and cost.

---

## 16. Final Revision Checklist

- [ ] Can define an agent vs simple LLM call
- [ ] Can explain when not to use agents
- [ ] Can design Pydantic tool schemas
- [ ] Can classify tool risk levels
- [ ] Can model explicit agent state
- [ ] Can explain LangGraph graph workflow
- [ ] Can explain ADK agent structure
- [ ] Can explain MCP client/server/tool model
- [ ] Can compare planning patterns
- [ ] Can list agent observability fields
- [ ] Can design agent eval records
- [ ] Can diagnose common agent failure modes
