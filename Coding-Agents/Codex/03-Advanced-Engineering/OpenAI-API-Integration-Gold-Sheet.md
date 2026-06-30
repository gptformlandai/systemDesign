# OpenAI API Integration — Gold Sheet

> **Track**: Codex Mastery Track — Group 3: Advanced Engineering
> **File**: 6 of 7 (Track File #19)
> **Audience**: Developers who want to go beyond the CLI and integrate Codex reasoning directly
> **Read after**: Tool-Use-and-Shell-Integration-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| CLI vs API — when each is the right tool | ★★★★★ | CLI for interactive sessions; API for programmatic integration and automation |
| Streaming responses for long tasks | ★★★★☆ | Without streaming: the API call blocks until full response — poor UX for long code gen |
| System message vs user message architecture | ★★★★☆ | Wrong separation produces weaker output and harder-to-maintain prompts |
| Rate limiting and retry strategy | ★★★★☆ | API-integrated automation fails silently without proper error handling |
| When NOT to use the API directly | ★★★☆☆ | The CLI handles session management, file discovery, approval — don't reinvent this |

---

## ⭐ Beginner Tier — Start Here

### B1: Make your first API call

```python
import os
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])  # never hardcode

response = client.chat.completions.create(
    model="o4-mini",
    messages=[
        {"role": "system", "content": "You are a code reviewer. Be concise."},
        {"role": "user", "content": "Review this: def add(a, b): return a + b"},
    ],
)
print(response.choices[0].message.content)
```

Key facts: API key from environment variable only. System message = role. User message = task.

### B2: Decide: CLI or API?

```
For each scenario, choose CLI or API:

1. Debugging a specific failing test in your project   → [CLI: it reads your files]
2. Batch-generating docstrings for 50 files in a loop  → [API: programmatic batch]
3. Interactive implementation session with AGENTS.md    → [CLI: session management]
4. Building a tool that embeds code review in your app  → [API: programmatic integration]
5. Adding lint fixes to today's changed files           → [CLI: auto-edit mode]
```

If you can answer all 5 correctly: you understand CLI vs API.

---

## 1. CLI vs API — When to Use Each

```
Codex CLI:
  ✅ Interactive development sessions
  ✅ Multi-file context discovery (Codex does this automatically)
  ✅ File editing with approval workflow
  ✅ Session-long context via AGENTS.md
  ✅ Non-interactive scripting (--quiet mode)
  ✅ Best for: 95% of coding workflows

OpenAI API (direct):
  ✅ Programmatic integration — calling Codex reasoning inside your own app/tool
  ✅ Custom approval and review pipelines you control entirely
  ✅ Batch processing — generate many code snippets without interactive session
  ✅ When you need the raw JSON response for downstream processing
  ✅ When you are building a tool ON TOP of Codex capabilities
  ✅ Best for: building your own Codex-powered tooling

The wrong call:
  ❌ Using the API directly to replicate what the CLI already does well
     (file discovery, approval, session management) — you'd be rebuilding the CLI
```

---

## 2. Basic API Call Pattern (Python)

```python
# pip install openai
import os
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])  # never hardcode

# Simple code review call
def review_code(code: str, language: str) -> str:
    response = client.chat.completions.create(
        model="o4-mini",
        messages=[
            {
                "role": "system",
                "content": (
                    "You are a code reviewer. Check for: security, correctness, "
                    "performance. Format: | SEVERITY | ISSUE | FIX |. "
                    "Final verdict: APPROVED or CHANGES REQUIRED."
                ),
            },
            {
                "role": "user",
                "content": f"Review this {language} code:\n\n```{language}\n{code}\n```",
            },
        ],
        max_tokens=1000,
    )
    return response.choices[0].message.content


# Usage
code = open("src/payments/service.py").read()
result = review_code(code, "python")
print(result)
```

---

## 3. Streaming for Long Code Generation

```python
import os
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

def generate_code_streaming(task: str, context: str) -> str:
    """Generate code with streaming — shows output as it arrives."""
    collected = []
    
    with client.chat.completions.stream(
        model="o4-mini",
        messages=[
            {
                "role": "system",
                "content": "Generate production Python code. Google-style docstrings. Type hints required.",
            },
            {
                "role": "user",
                "content": f"Context:\n{context}\n\nTask: {task}",
            },
        ],
        max_tokens=2000,
    ) as stream:
        for text in stream.text_stream:
            print(text, end="", flush=True)
            collected.append(text)
    
    print()  # newline after stream
    return "".join(collected)


# Usage
context = open("src/api/users.py").read()
result = generate_code_streaming(
    task="Add a GET /users/{id}/profile endpoint following the same pattern",
    context=context,
)
```

---

## 4. Rate Limiting and Retry Strategy

```python
import time
import os
from openai import OpenAI, RateLimitError, APITimeoutError

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

def call_with_retry(messages: list, model: str = "o4-mini", max_retries: int = 3) -> str:
    """API call with exponential backoff for rate limiting."""
    for attempt in range(max_retries):
        try:
            response = client.chat.completions.create(
                model=model,
                messages=messages,
                max_tokens=1000,
                timeout=30,
            )
            return response.choices[0].message.content
        
        except RateLimitError:
            if attempt == max_retries - 1:
                raise
            wait = 2 ** attempt  # 1s, 2s, 4s
            print(f"Rate limited. Retrying in {wait}s...")
            time.sleep(wait)
        
        except APITimeoutError:
            if attempt == max_retries - 1:
                raise
            print(f"Timeout on attempt {attempt + 1}. Retrying...")
            time.sleep(2)
    
    raise RuntimeError("Max retries exceeded")  # unreachable but explicit
```

---

## 5. Batch Code Processing Pattern

```python
import os
from pathlib import Path
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

def add_docstrings_to_file(filepath: str) -> str:
    """Add docstrings to all public functions in a file via API."""
    code = Path(filepath).read_text(encoding="utf-8")
    
    response = client.chat.completions.create(
        model="gpt-4.1-mini",  # cheaper for doc tasks
        messages=[
            {
                "role": "system",
                "content": (
                    "Add Google-style docstrings to all public functions. "
                    "Only document what is verifiably in the code. "
                    "Return the complete file with docstrings added — nothing else."
                ),
            },
            {"role": "user", "content": code},
        ],
        max_tokens=4000,
    )
    return response.choices[0].message.content


# Batch process all service files
for filepath in Path("src/").rglob("*service*.py"):
    print(f"Processing: {filepath}")
    result = add_docstrings_to_file(str(filepath))
    filepath.write_text(result, encoding="utf-8")
    print(f"Done: {filepath}")
```

---

## 6. When NOT to Use the API

```
Building a Codex-powered pipeline?   → API is right
Interactive coding session?          → Use the CLI
Want file editing with approval?      → Use the CLI (--approval-policy)
Multi-file context discovery?         → Use the CLI (Codex does this automatically)
Session memory (AGENTS.md)?           → Use the CLI
Batch processing many files?          → API makes sense
Embedding in your own tool?           → API is right
Debugging a specific failing test?    → Use the CLI
```

---

## Interview Traps

```
TRAP: "Using the API directly is more powerful than the CLI"
TRUTH: The CLI wraps the API and adds: AGENTS.md context loading, multi-file discovery,
       approval workflows, session management, and interactive mode. Using the API directly
       for tasks the CLI handles well means rebuilding what the CLI already provides.

TRAP: "Always use gpt-4.1 in API calls — it's the most capable model"
TRUTH: gpt-4.1 is 5-10x more expensive than o4-mini. For documentation-only batch tasks,
       gpt-4.1-mini produces equivalent quality at lower cost and latency. Match model to
       task complexity: documentation → gpt-4.1-mini, implementation → o4-mini,
       architecture → gpt-4.1.

TRAP: "Streaming is just a UX feature — it doesn't affect correctness"
TRUTH: Streaming lets you detect wrong direction early and cancel the call before wasting
       tokens on a response you'll discard. For long code generation, early detection of
       misaligned output is both a cost saving and a feedback mechanism.
```

---

## Revision Checklist

- [ ] Can explain CLI vs API trade-offs — know when to use each
- [ ] Can write a basic API call with system message and user message correctly separated
- [ ] Can implement streaming for long code generation tasks
- [ ] Can write retry logic with exponential backoff for rate limiting
- [ ] Know which model to use for which task type in API calls
- [ ] API keys stored in environment variables — never in code
