# Pydantic for GenAI Structured Outputs - Gold Sheet

> **GenAI Python Support Stack - File 1 of 5**
> For: all learners, especially Java/backend developers | Level: beginner to production | Mode: validate every LLM boundary

---

## 1. Why Pydantic Matters In GenAI

LLMs produce text. Production systems need structured, validated data.

Pydantic sits between:

```text
messy external input / LLM output / tool payload
    -> Pydantic validation
        -> trusted Python object
            -> service logic / database / API response / tool execution
```

In GenAI apps, Pydantic is used for:

- structured LLM outputs
- tool/function calling schemas
- request and response DTOs
- RAG chunk metadata
- agent state objects
- eval result schemas
- provider configuration
- safety and guardrail outputs
- JSON parsing and repair boundaries

---

## 2. Mental Model

A Pydantic model is a runtime-validated contract.

```python
from pydantic import BaseModel, Field

class TicketClassification(BaseModel):
    category: str
    priority: int = Field(ge=1, le=5)
    summary: str
```

If input matches the contract, you get a typed object.

```python
data = {"category": "billing", "priority": 4, "summary": "Refund request"}
result = TicketClassification.model_validate(data)
print(result.priority)  # 4
```

If input is invalid, Pydantic raises a clear validation error.

```python
bad = {"category": "billing", "priority": 99, "summary": "Refund request"}
TicketClassification.model_validate(bad)
```

This is exactly what you want around LLM output: trust only after validation.

---

## 3. Java Developer Bridge

| Java / Spring | Pydantic / Python |
|---|---|
| DTO class | `BaseModel` |
| Bean Validation annotations | `Field(...)`, validators |
| Jackson deserialize | `model_validate_json` |
| Jackson serialize | `model_dump_json` |
| `@NotNull`, `@Min`, `@Max` | `Field(min_length=..., ge=..., le=...)` |
| `@ConfigurationProperties` | `pydantic-settings` |
| Compile-time type checks | mypy/pyright, separate from Pydantic runtime validation |

Key difference: Python type hints alone do not validate runtime data. Pydantic does.

---

## 4. Structured LLM Output Pattern

### Schema

```python
from typing import Literal
from pydantic import BaseModel, Field

class SupportTicketOutput(BaseModel):
    category: Literal["billing", "technical", "account", "other"]
    priority: int = Field(ge=1, le=5)
    sentiment: Literal["angry", "neutral", "happy"]
    summary: str = Field(min_length=5, max_length=300)
    needs_human: bool
```

### Prompt Contract

```text
Return ONLY valid JSON matching this schema:
{
  "category": "billing | technical | account | other",
  "priority": 1-5,
  "sentiment": "angry | neutral | happy",
  "summary": "short summary",
  "needs_human": true/false
}
```

### Parse and Validate

```python
import json
from pydantic import ValidationError

raw_llm_text = '''{
  "category": "billing",
  "priority": 4,
  "sentiment": "angry",
  "summary": "Customer is asking for a refund after duplicate charge.",
  "needs_human": true
}'''

try:
    output = SupportTicketOutput.model_validate_json(raw_llm_text)
except ValidationError as exc:
    print("LLM output failed schema validation")
    print(exc)
else:
    print(output.category, output.priority)
```

### Production Rule

Never let raw LLM JSON flow directly into business logic. Validate first.

---

## 5. Tool Calling Schema Pattern

LLM tool/function calling needs precise input schemas.

```python
from pydantic import BaseModel, Field

class SearchKnowledgeBaseArgs(BaseModel):
    query: str = Field(min_length=3, max_length=500)
    top_k: int = Field(default=5, ge=1, le=20)
    include_sources: bool = True
```

A tool wrapper can validate before execution.

```python
def search_knowledge_base(payload: dict) -> dict:
    args = SearchKnowledgeBaseArgs.model_validate(payload)
    results = run_vector_search(args.query, top_k=args.top_k)
    return {
        "query": args.query,
        "results": results,
        "include_sources": args.include_sources,
    }
```

Why this matters:

- LLM may pass wrong types.
- LLM may omit required fields.
- LLM may pass unsafe values.
- Validation gives deterministic failure instead of hidden bad behavior.

---

## 6. Agent State Schema Pattern

Agents need state that survives multiple steps.

```python
from pydantic import BaseModel, Field

class RetrievalState(BaseModel):
    user_question: str
    rewritten_query: str | None = None
    retrieved_doc_ids: list[str] = Field(default_factory=list)
    answer: str | None = None
    tool_errors: list[str] = Field(default_factory=list)
    retry_count: int = 0
```

Rules:

- Use `default_factory=list` for mutable defaults.
- Keep state serializable if you need checkpointing.
- Separate public user input from internal agent state.

---

## 7. RAG Chunk Metadata Schema

```python
from pydantic import BaseModel, Field

class ChunkMetadata(BaseModel):
    document_id: str
    source_path: str
    page_number: int | None = Field(default=None, ge=1)
    chunk_index: int = Field(ge=0)
    content_hash: str
    token_count: int = Field(ge=1)
```

This prevents silent bad metadata such as:

- negative page numbers
- missing document IDs
- invalid chunk index
- empty content hash
- wrong token counts

Bad metadata leads to bad citations and bad evals.

---

## 8. Validators For GenAI Rules

```python
from pydantic import BaseModel, field_validator

class PromptTemplate(BaseModel):
    name: str
    template: str

    @field_validator("template")
    @classmethod
    def must_contain_question(cls, value: str) -> str:
        if "{question}" not in value:
            raise ValueError("Prompt template must contain {question}")
        return value
```

Use validators for rules that are more specific than type checks.

Good validator examples:

- prompt must include required variables
- tool name must be lowercase snake_case
- model name must be in approved list
- top_k must be lower for expensive retrievers
- output must contain at least one citation

---

## 9. Pydantic Settings For GenAI Apps

```python
from functools import lru_cache
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    app_env: str = "local"
    openai_api_key: str = Field(validation_alias="OPENAI_API_KEY")
    model_name: str = "gpt-4o-mini"
    embedding_model: str = "text-embedding-3-small"
    request_timeout_seconds: float = Field(default=30.0, gt=0)
    max_concurrent_requests: int = Field(default=10, ge=1, le=100)

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

@lru_cache
def get_settings() -> Settings:
    return Settings()
```

Production rules:

- validate config at startup
- never hard-code API keys
- keep model names configurable
- set timeout/concurrency defaults explicitly
- use secret managers in real deployments

---

## 10. Common Pydantic Traps In GenAI

| Trap | Why It Hurts | Fix |
|---|---|---|
| Trusting raw LLM JSON | malformed or unsafe output reaches app | `model_validate_json` |
| Using `dict` everywhere | no contract, no validation | define `BaseModel` |
| Mutable default `[]` | shared state bugs | `Field(default_factory=list)` |
| One schema for API, DB, LLM, and domain | tight coupling | separate schemas by boundary |
| Catching all validation errors and continuing | hides provider drift | log and fail safely |
| Overly strict schema in early prompt experiments | slows iteration | strict at boundary, flexible in drafts |
| No version field in eval output | old and new results mix | include schema/model version |

---

## 11. Mini-Lab: Validate LLM Output

Create `lab_pydantic_llm_output.py`:

```python
from typing import Literal
from pydantic import BaseModel, Field, ValidationError

class EvalResult(BaseModel):
    question_id: str
    passed: bool
    score: int = Field(ge=1, le=5)
    failure_reason: Literal["none", "missing_context", "wrong_answer", "unsafe"]

samples = [
    '{"question_id":"q1","passed":true,"score":5,"failure_reason":"none"}',
    '{"question_id":"q2","passed":false,"score":9,"failure_reason":"wrong_answer"}',
    '{"question_id":"q3","passed":false,"score":2,"failure_reason":"unknown"}',
]

for raw in samples:
    try:
        result = EvalResult.model_validate_json(raw)
        print("VALID", result)
    except ValidationError as exc:
        print("INVALID")
        print(exc.errors())
```

Expected:

```text
VALID question_id='q1' passed=True score=5 failure_reason='none'
INVALID
...
INVALID
...
```

---

## 12. Hot Interview Q&A

**Q1: Why is Pydantic important in GenAI systems?**
> LLM output is probabilistic text. Pydantic gives a deterministic validation boundary so the application only accepts data that matches the required schema.

**Q2: Are Python type hints enough?**
> No. Type hints help static tools and readers, but they do not validate runtime data. Pydantic validates runtime input and converts it into typed objects.

**Q3: How do you handle invalid LLM JSON?**
> Treat it as a provider/model output failure. Validate with Pydantic, log the validation error with the prompt/model/version, optionally retry with a repair prompt, and fail safely if it still does not validate.

**Q4: Why not use one Pydantic model for everything?**
> API contracts, LLM outputs, domain models, and database models change for different reasons. Serious systems separate those boundaries to avoid coupling.

**Q5: What is the equivalent of Java Bean Validation?**
> Pydantic `Field` constraints and validators. For example `Field(ge=1, le=5)` maps roughly to `@Min(1)` and `@Max(5)`.

---

## 13. Final Revision Checklist

- [ ] Can define a Pydantic model for structured LLM output
- [ ] Can parse JSON with `model_validate_json`
- [ ] Can explain type hints vs runtime validation
- [ ] Can design a tool-calling args schema
- [ ] Can use `Field(default_factory=list)` correctly
- [ ] Can create validators for prompt/tool rules
- [ ] Can separate API schema, LLM output schema, and domain model
- [ ] Can configure GenAI apps with `pydantic-settings`
- [ ] Can explain Pydantic to a Java developer using DTO/Bean Validation mapping
