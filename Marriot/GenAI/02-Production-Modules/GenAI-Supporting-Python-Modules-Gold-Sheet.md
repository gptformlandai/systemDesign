# GenAI Supporting Python Modules - Gold Sheet

> **GenAI Python Support Stack - File 4 of 5**
> For: Java/backend developers | Level: beginner to production | Mode: practical modules for reliable GenAI services

---

## 1. Why Supporting Modules Matter

GenAI applications are not just prompts.

A production GenAI service needs:

```text
HTTP client -> retries -> timeout -> schema validation -> token control
  -> async concurrency -> logging/tracing -> eval/test harness -> API serving
```

The LLM call is only one piece. Most production failures come from the surrounding engineering:

- no timeout
- no retry policy
- event loop blocked
- malformed JSON accepted
- token budget exceeded
- no request correlation
- API keys hard-coded
- tests call real provider every time
- streaming response not cancelled cleanly
- no cost or latency tracking

This sheet covers the Python modules that make GenAI systems reliable.

---

## 2. Module Priority Meter

| Module | Use In GenAI | Priority |
|---|---|---|
| `httpx` | async LLM/provider API calls | Very high |
| `asyncio` | concurrent calls, streaming, cancellation | Very high |
| `tenacity` | retries with backoff | Very high |
| `pydantic-settings` | typed config and secrets | Very high |
| `orjson` / `json` | fast JSON parsing/serialization | High |
| `tiktoken` / tokenizers | token counting and budget control | High |
| `pathlib` | document ingestion paths | High |
| `hashlib` | content hashes, dedupe, cache keys | High |
| `logging` / `structlog` | structured logs | High |
| OpenTelemetry | traces and metrics | Medium-high |
| `pytest`, `pytest-asyncio` | unit/eval tests | Very high |
| `respx` | mock `httpx` provider calls | High |
| `rich` | local CLI/eval output | Medium |
| `typer` | CLIs for evals and ingestion | Medium |
| `heapq` | top-k local retrieval | Medium |
| `sqlite3` / DuckDB | local eval/result storage | Medium |

---

## 3. Java Developer Bridge

| Java / Spring Tool | Python GenAI Module |
|---|---|
| WebClient / RestTemplate | `httpx.AsyncClient` |
| Resilience4j Retry | `tenacity` |
| `CompletableFuture` | `asyncio.Task`, `asyncio.gather` |
| application.yml / profiles | `pydantic-settings`, env vars |
| Jackson | `json`, `orjson`, Pydantic serialization |
| SLF4J / Logback | `logging`, `structlog` |
| MDC | `ContextVar` |
| Micrometer / OpenTelemetry | OpenTelemetry Python SDK |
| JUnit | pytest |
| Mockito / WireMock | `unittest.mock`, `respx` |
| Picocli | Typer |
| Maven dependency hygiene | pip/Poetry + pinning + `pip check` |

Key shift: Python gives you smaller tools. You compose them deliberately instead of expecting one framework to enforce everything.

---

## 4. `httpx` For LLM API Calls

### Why `httpx`

`httpx` supports sync and async HTTP, connection pooling, timeouts, headers, streaming, and testability.

For FastAPI/GenAI services, prefer one shared async client per application lifecycle.

```python
import httpx

class LLMClient:
    def __init__(self, base_url: str, api_key: str, timeout_seconds: float = 30.0):
        self._client = httpx.AsyncClient(
            base_url=base_url,
            timeout=httpx.Timeout(timeout_seconds),
            headers={"Authorization": f"Bearer {api_key}"},
        )

    async def complete(self, prompt: str) -> dict:
        response = await self._client.post("/chat/completions", json={"prompt": prompt})
        response.raise_for_status()
        return response.json()

    async def close(self) -> None:
        await self._client.aclose()
```

### Rules

- Always set timeouts.
- Reuse clients to reuse connections.
- Close clients during app shutdown.
- Do not create a new client per request.
- Use `raise_for_status()` or explicit error mapping.

### FastAPI Lifespan

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.llm_client = LLMClient(base_url="https://api.provider.com", api_key="...")
    try:
        yield
    finally:
        await app.state.llm_client.close()

app = FastAPI(lifespan=lifespan)
```

---

## 5. `asyncio` For Concurrency

### Sequential vs Concurrent Calls

```python
import asyncio

async def embed(text: str) -> list[float]:
    await asyncio.sleep(0.1)
    return [0.1, 0.2, 0.3]

async def sequential(texts: list[str]):
    results = []
    for text in texts:
        results.append(await embed(text))
    return results

async def concurrent(texts: list[str]):
    return await asyncio.gather(*(embed(text) for text in texts))
```

For 20 independent embedding calls at 100ms each:

```text
sequential: about 2 seconds
concurrent gather: about 100ms plus overhead
```

### Cap Concurrency

Do not fire 10,000 provider requests at once.

```python
import asyncio

semaphore = asyncio.Semaphore(10)

async def safe_embed(text: str) -> list[float]:
    async with semaphore:
        return await embed(text)
```

### Cancellation Rule

```python
try:
    result = await provider_call()
except asyncio.CancelledError:
    # cleanup if needed
    raise
```

Always re-raise `CancelledError` after cleanup.

---

## 6. `tenacity` For Retries

### Retry Transient Failures

```python
from tenacity import retry, stop_after_attempt, wait_exponential_jitter, retry_if_exception_type
import httpx

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential_jitter(initial=0.5, max=8),
    retry=retry_if_exception_type((httpx.TimeoutException, httpx.ConnectError)),
)
async def call_provider(client: httpx.AsyncClient, payload: dict) -> dict:
    response = await client.post("/chat/completions", json=payload)
    response.raise_for_status()
    return response.json()
```

### Retry Rules In GenAI

Retry:

- timeouts
- connection resets
- rate-limit responses if allowed
- 502/503 transient provider errors

Do not blindly retry:

- invalid request payload
- auth failures
- safety/policy rejections
- non-idempotent tool writes without idempotency key

### Java Developer Bridge

This maps to Resilience4j retry/backoff. Same production rule: retry only when the operation is safe to retry.

---

## 7. Settings And Secrets

Use `pydantic-settings` for typed configuration.

```python
from functools import lru_cache
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    app_env: str = "local"
    openai_api_key: str = Field(validation_alias="OPENAI_API_KEY")
    model_name: str = "gpt-4o-mini"
    embedding_model: str = "text-embedding-3-small"
    max_concurrent_llm_calls: int = 10
    request_timeout_seconds: float = 30.0
    enable_tracing: bool = False

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

@lru_cache
def get_settings() -> Settings:
    return Settings()
```

Rules:

- no secrets in Git
- validate required config at startup
- use environment variables in deploys
- keep local `.env.example` without real secrets
- use cloud secret managers in production

---

## 8. JSON: `json` vs `orjson`

### Standard Library `json`

```python
import json

payload = json.loads(raw_text)
text = json.dumps(payload)
```

Good enough for most small payloads.

### `orjson`

```python
import orjson

payload = orjson.loads(raw_bytes)
raw = orjson.dumps(payload)
```

`orjson` is faster and returns bytes from `dumps`.

Use `orjson` when:

- high-throughput API responses
- large eval result files
- lots of JSONL parsing/writing
- performance profiling shows JSON overhead

Do not optimize JSON before measuring.

---

## 9. Token Counting

Token counting protects cost and context length.

```python
# provider-specific example shape; exact tokenizer depends on model/provider
import tiktoken

encoding = tiktoken.get_encoding("cl100k_base")
text = "Explain retrieval augmented generation."
tokens = encoding.encode(text)
print(len(tokens))
```

Use token counting for:

- chunk sizing
- prompt budget checks
- cost estimation
- preventing context overflow
- eval cost reports

Rule: use the tokenizer that matches your model/provider when possible.

---

## 10. `pathlib` And `hashlib` For Ingestion

### File Discovery

```python
from pathlib import Path

root = Path("docs")
markdown_files = list(root.rglob("*.md"))
```

### Content Hashing

```python
import hashlib
from pathlib import Path

path = Path("docs/guide.md")
content = path.read_bytes()
content_hash = hashlib.sha256(content).hexdigest()
print(content_hash)
```

Hashes are useful for:

- deduplicating documents
- detecting changed files
- stable chunk IDs
- cache keys for embeddings
- reproducible ingestion pipelines

---

## 11. Structured Logging And Request Context

### Request Context

```python
from contextvars import ContextVar

request_id_var: ContextVar[str] = ContextVar("request_id", default="-")
```

### Logging

```python
import logging

logger = logging.getLogger(__name__)

logger.info(
    "llm_call_completed",
    extra={"model": "gpt-4o-mini", "latency_ms": 820, "input_tokens": 1000},
)
```

Track:

- request ID
- user/tenant when safe
- model name
- prompt version
- latency
- token usage
- cost
- retry count
- retrieval top-k score
- tool call name

Do not log raw sensitive prompts unless policy allows it.

---

## 12. Observability With OpenTelemetry

Use traces to understand a GenAI request across steps:

```text
HTTP /chat
  -> validate request
  -> retrieve documents
  -> call embedding model
  -> vector search
  -> call chat model
  -> stream response
```

Key metrics:

| Metric | Why |
|---|---|
| LLM latency by model | provider performance |
| token usage | cost control |
| retry count | provider instability |
| retrieval hit score | RAG quality |
| tool call failures | agent reliability |
| eval pass rate | quality trend |
| refusal/safety rate | product behavior |
| p95/p99 endpoint latency | user experience |

---

## 13. Testing: `pytest`, `pytest-asyncio`, `respx`

### Async Unit Test

```python
import pytest

@pytest.mark.asyncio
async def test_embedder_returns_vector():
    vector = await embed("hello")
    assert isinstance(vector, list)
```

### Mock HTTPX With `respx`

```python
import httpx
import pytest
import respx

@pytest.mark.asyncio
async def test_provider_call():
    with respx.mock:
        route = respx.post("https://api.provider.com/chat/completions").mock(
            return_value=httpx.Response(200, json={"answer": "ok"})
        )

        async with httpx.AsyncClient(base_url="https://api.provider.com") as client:
            response = await client.post("/chat/completions", json={"prompt": "hi"})

        assert response.json()["answer"] == "ok"
        assert route.called
```

### Test Rule

Do not call real providers in normal unit tests. Use integration tests for real provider smoke checks, gated by environment variables.

---

## 14. CLI Tools: `typer` And `rich`

### Typer CLI

```python
import typer

app = typer.Typer()

@app.command()
def run_eval(dataset: str, model: str = "gpt-4o-mini"):
    typer.echo(f"Running eval: dataset={dataset}, model={model}")

if __name__ == "__main__":
    app()
```

### Rich Output

```python
from rich.table import Table
from rich.console import Console

console = Console()
table = Table(title="Eval Results")
table.add_column("Model")
table.add_column("Pass Rate")
table.add_row("gpt-4o-mini", "82%")
console.print(table)
```

Useful for local eval runners, ingestion reports, and debugging tools.

---

## 15. Local Storage: SQLite, DuckDB, JSONL

| Storage | Use Case |
|---|---|
| JSONL | simple append-only eval records |
| SQLite | small local app state, test fixtures |
| DuckDB | analytical queries over CSV/Parquet/eval data |
| Parquet | efficient typed dataset storage |
| Postgres/pgvector | production relational + vector storage |

For early GenAI projects, JSONL + Pandas is often enough. For larger eval analytics, DuckDB is excellent.

---

## 16. Common Production Traps

| Trap | Why It Hurts | Fix |
|---|---|---|
| No timeout on LLM call | requests hang and exhaust workers | set explicit timeout |
| No concurrency cap | provider rate limits / cost spike | semaphore / queue |
| New HTTP client per request | connection churn | shared client lifecycle |
| Retrying everything | duplicate writes, cost explosion | retry only safe errors |
| Hard-coded API key | security incident | env + secret manager |
| Real provider in unit tests | flaky and expensive tests | mock with `respx` |
| Blocking call in `async def` | stalls FastAPI event loop | use async client or executor |
| No token budget | prompt exceeds context | tokenizer check |
| No request ID | impossible debugging | ContextVar + structured logs |
| Logging prompts blindly | privacy/security risk | redaction policy |

---

## 17. Mini-Lab: Reliable LLM Client Skeleton

Create `lab_reliable_llm_client.py`:

```python
import asyncio
import httpx
from tenacity import retry, stop_after_attempt, wait_exponential_jitter

class LLMClient:
    def __init__(self):
        self._client = httpx.AsyncClient(timeout=httpx.Timeout(10.0))

    @retry(stop=stop_after_attempt(3), wait=wait_exponential_jitter(initial=0.5, max=4))
    async def complete(self, prompt: str) -> dict:
        # Replace URL with a mocked endpoint in tests.
        response = await self._client.post("https://example.com/llm", json={"prompt": prompt})
        response.raise_for_status()
        return response.json()

    async def close(self) -> None:
        await self._client.aclose()

async def main():
    client = LLMClient()
    try:
        print("client ready")
    finally:
        await client.close()

asyncio.run(main())
```

Challenge:

- Add a semaphore.
- Add request ID logging.
- Add Pydantic response validation.
- Add a `respx` test.

---

## 18. Hot Interview Q&A

**Q1: What modules do you need around an LLM API call?**
> `httpx` for async HTTP, `tenacity` for safe retries, Pydantic for validation, tokenizer for budget checks, logging/tracing for observability, and pytest/respx for tests.

**Q2: Why use `httpx.AsyncClient` instead of `requests` in FastAPI?**
> `requests` blocks the event loop inside `async def`. `httpx.AsyncClient` awaits network I/O, allowing other requests to run concurrently.

**Q3: How do you prevent provider overload?**
> Use concurrency limits with `asyncio.Semaphore`, rate-limit queues, backoff on rate-limit responses, and track token/request budgets.

**Q4: How do you test GenAI provider clients?**
> Unit tests should mock HTTP responses with tools like `respx`. Real provider calls belong in explicit integration smoke tests, usually gated by environment variables.

**Q5: What should be logged for GenAI calls?**
> Request ID, model, prompt version, latency, token usage, cost, retry count, status, and sanitized error details. Avoid logging sensitive raw prompts unless approved.

---

## 19. Final Revision Checklist

- [ ] Can use `httpx.AsyncClient` with timeout and lifecycle cleanup
- [ ] Can explain why `requests` inside FastAPI async code is bad
- [ ] Can run concurrent calls with `asyncio.gather`
- [ ] Can cap concurrency with `asyncio.Semaphore`
- [ ] Can configure retries with `tenacity`
- [ ] Can define typed settings with `pydantic-settings`
- [ ] Can count tokens for prompt budget control
- [ ] Can hash documents for dedupe/cache keys
- [ ] Can design structured logs for LLM calls
- [ ] Can identify key GenAI observability metrics
- [ ] Can mock provider calls in tests with `respx`
- [ ] Can build CLI eval tools with Typer/Rich
- [ ] Can choose JSONL, SQLite, DuckDB, or Postgres appropriately
