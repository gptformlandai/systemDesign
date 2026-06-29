# Custom Instructions — Deep Dive — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: 1 of 7 (Track File #7)
> **Audience**: Developers ready to configure Copilot for their project context
> **Read after**: Copilot-For-Beginners-Quick-Wins-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| `copilot-instructions.md` — what it is and where it goes | ★★★★★ | Devs don't know this file exists; all Copilot responses are generic |
| Path-specific instructions — `*.instructions.md` | ★★★★★ | One global instruction file creates conflicts; path-specific is cleaner |
| Instruction design principles — short, concrete, testable | ★★★★★ | Long instruction files are partially or completely ignored |
| Bad vs good instruction examples | ★★★★★ | Knowing what NOT to write saves hours of wondering why Copilot ignores rules |
| Instruction file conflicts — how to detect and resolve | ★★★★☆ | Multiple instruction files with contradictions produce unpredictable behavior |
| Monorepo instruction strategy | ★★★★☆ | Monorepos need hierarchical instructions by domain, not one global file |
| Testing your instructions work | ★★★★☆ | Devs write instructions but never verify Copilot actually follows them |

---

## 2. What Custom Instructions Are

### Must Know

```
Custom instructions tell Copilot about your project context, conventions, and rules.
They are loaded automatically into every Chat conversation in that workspace.
You never need to repeat "use Python 3.12" or "we use pytest" in every prompt.

Where instructions live:
  .github/copilot-instructions.md         → root-level, applies to all files
  .github/instructions/*.instructions.md  → path-specific, applies to matched files

How Copilot uses them:
  - Loaded into Chat context automatically when you open a conversation
  - Applied to Agent Mode sessions
  - Applied to code review prompts
  - NOT used by inline suggestions (inline uses file content and cursor context only)

What they cannot do:
  - Force Copilot to always be correct
  - Override the model's fundamental behavior
  - Add capabilities Copilot doesn't have
  - Replace clear, specific prompts
  - Guarantee Copilot follows every rule every time
```

---

## 3. Root-Level Instruction File

### File: `.github/copilot-instructions.md`

```markdown
# Project: [Your Project Name]

## What This Project Is
[2-3 sentences describing the project. What it does, who uses it.]
Example: "A FastAPI backend service for managing e-commerce orders. 
Used by internal ops team and exposed via REST API to mobile clients."

## Tech Stack
- Language: Python 3.12
- Framework: FastAPI 0.115+
- Database: PostgreSQL 16 with SQLAlchemy 2.x async
- ORM: SQLAlchemy with asyncpg driver
- Validation: Pydantic v2
- Testing: pytest with pytest-asyncio
- Dependency management: Poetry
- Linting: ruff
- Type checking: mypy (strict mode)

## Architecture Rules
- Service layer handles business logic. Controllers/routers only parse request and call services.
- Repository layer handles all database access. Services never query the DB directly.
- Never use mutable default arguments in function signatures.
- Always use async/await for database operations. Never use sync SQLAlchemy in async routes.

## Coding Standards
- Follow PEP 8. Use ruff for enforcement.
- Type hints are required on all function signatures.
- Use f-strings for string formatting. Never use % or .format().
- Class names: PascalCase. Functions and variables: snake_case. Constants: UPPER_SNAKE_CASE.
- Maximum function length: 30 lines. Extract longer logic into helper methods.

## Testing Rules
- Every public function must have at least one unit test.
- Use pytest fixtures for database sessions and common test data.
- Mock external services (email, SMS, payment) in unit tests.
- Test file location: tests/ mirroring src/ structure.

## Security Rules
- Never log passwords, tokens, or PII.
- Always use parameterized queries. Never use string formatting in SQL.
- Validate all user inputs with Pydantic before processing.
- Never expose internal error messages in API responses.

## Do Not
- Do not suggest print() for logging. Use Python logging module.
- Do not suggest os.system() or shell=True in subprocess calls.
- Do not use bare except clauses.
- Do not generate code that uses deprecated SQLAlchemy 1.x patterns.
```

### How Long Should It Be?

```
Ideal length: 200-400 words / 30-60 lines
Maximum useful length: ~500 words

Why shorter is better:
  The instruction file competes with your prompt and the file context
  for space in the context window. A 2000-word instruction file
  reduces space for actual code context.
  
  More importantly: Copilot does not reliably follow every rule in a long file.
  It follows the most prominent / first-encountered rules most consistently.
  Keep only rules that change Copilot's default behavior.
  Don't add rules for things Copilot already does correctly.
```

---

## 4. Path-Specific Instruction Files

### How They Work

```
File naming: .github/instructions/<name>.instructions.md

The applyTo frontmatter field specifies which files trigger these instructions.
Use glob patterns.

---
applyTo: "**/*.py"
---
[Python-specific instructions here]

---
applyTo: "tests/**"
---
[Test-specific instructions here]

---
applyTo: ".github/workflows/**"
---
[GitHub Actions-specific instructions here]
```

### Example: `python.instructions.md`

```markdown
---
applyTo: "**/*.py"
---
# Python Conventions

## Language Version
Use Python 3.12+ syntax. Use match/case for type dispatching.
Use `X | Y` union syntax instead of `Union[X, Y]`.
Use `list[X]` instead of `List[X]`. Use `dict[str, X]` instead of `Dict[str, X]`.

## Async
All database operations use async/await.
Never use time.sleep() in async functions. Use asyncio.sleep().
Never use requests library in async code. Use httpx.AsyncClient.

## Error Handling
Use custom exception classes that inherit from a base AppException.
Never use bare except. Always catch specific exception types.
Always log exceptions with logger.exception() to include the stack trace.

## Dependencies
Preferred HTTP client: httpx (not requests, not aiohttp)
Preferred data validation: Pydantic v2 (not marshmallow, not voluptuous)
Preferred date/time: datetime from standard library + zoneinfo for timezones
```

### Example: `testing.instructions.md`

```markdown
---
applyTo: "tests/**"
---
# Testing Conventions

## Framework
Use pytest. All tests use pytest-asyncio for async tests.
Decorate async tests with @pytest.mark.asyncio.

## Fixtures
Database sessions: use the db_session fixture from tests/conftest.py.
Test users: use the test_user fixture. Never create real DB records manually in tests.
External services: always mock. Never call real email, SMS, or payment endpoints in tests.

## Test Naming
Pattern: test_<function_name>_<scenario>_<expected_outcome>
Example: test_create_user_duplicate_email_raises_conflict_error

## Assertions
One logical assertion per test. Use pytest.raises() for exception tests.
Never use assert True. Use specific assertion: assert result.status == "active"

## Coverage
Every public method needs at least: happy path, one error case, one edge case.
```

### Example: `security.instructions.md`

```markdown
---
applyTo: "**"
---
# Security Rules

## Input Handling
All user inputs must be validated through Pydantic models before any processing.
Never use eval() or exec() with user input.
Never use shell=True in subprocess calls with user-provided data.

## SQL
Always use SQLAlchemy ORM or parameterized queries.
Never use string concatenation or f-strings to build SQL queries.

## Secrets
Never hardcode credentials, API keys, or passwords in code.
Use environment variables (os.environ.get) or a secrets manager.

## Logging
Never log: passwords, tokens, API keys, SSNs, credit card numbers, or email addresses.
Use structured logging with sanitized fields only.

## Error Responses
API error responses must not include stack traces, internal paths, or system details.
Use generic error messages for 5xx errors.
```

---

## 5. Instruction Design Principles

### Principle 1 — Be Specific, Not General

```
BAD:  "Follow best practices"
      → Copilot already tries to follow best practices. This adds no information.

GOOD: "Use ruff for linting enforcement. Never use pylint."
      → Specific tool, specific prohibition — Copilot knows exactly what to do.

BAD:  "Write clean code"
      → "Clean" means different things in different contexts. Useless instruction.

GOOD: "Maximum function length: 30 lines. Extract longer logic into helper methods."
      → Concrete, measurable, actionable.
```

### Principle 2 — Rules Must Change Default Behavior

```
Don't write rules Copilot follows by default:
  BAD:  "Use proper indentation" (Copilot already does this for Python)
  BAD:  "Add comments to explain complex code" (Copilot already does this)

Write rules that override defaults:
  GOOD: "Do NOT add comments to every line. Only comment non-obvious logic."
  GOOD: "Do NOT use type: ignore comments. Fix the type error instead."
  GOOD: "Use async SQLAlchemy 2.x patterns. Never suggest sync Session or session.query()."
```

### Principle 3 — One Rule Per Line

```
BAD:
  "Use Python 3.12, follow PEP 8, use f-strings, avoid global variables,
  use type hints, test with pytest, mock external services, use ruff..."
  → One massive run-on that Copilot may partially skip.

GOOD:
  - Use Python 3.12+
  - Type hints required on all function signatures
  - Use f-strings for formatting
  - No global mutable state
  - Mock all external services in unit tests
  → Each rule is discrete and easier to follow.
```

### Principle 4 — Negative Rules Are as Important as Positive Rules

```
"Do NOT" instructions are often more valuable:
  - Do not use os.system(). Use subprocess.run() with shell=False.
  - Do not use print() for logging. Use the logging module.
  - Do not create new custom ORM models for existing tables. Use the models in src/models/.
  - Do not suggest migration to a different framework. We are committed to FastAPI.
  - Do not add new dependencies without listing them in pyproject.toml.
```

### Principle 5 — Test That Your Instructions Work

```
After writing instructions, test them:

1. Ask a question that should be affected by the instruction.
2. Check if the response follows the rule.

Test example for "never use requests, use httpx":
  Prompt: "Write a function to make a POST request to an external API with JSON body"
  Expected: Copilot uses httpx.AsyncClient, not requests
  If it uses requests: your instruction isn't clear enough — rewrite it.

Test example for "never log PII":
  Prompt: "Add logging to the login function that takes email and password"
  Expected: Copilot logs the action (e.g., "Login attempt for user") but not the email/password
  If it logs email: strengthen the security instruction.
```

---

## 6. Bad vs Good Instruction Examples

### Bad instruction file (avoid):

```markdown
# Code Quality
Please write good, clean, well-documented code that follows best practices
and is maintainable and readable. Use appropriate design patterns and make
sure the code is tested and secure. Follow the team's coding standards and
conventions. Be helpful and provide explanations when needed. Thank you.
```

Problems:
- Vague: "good, clean, well-documented" has no meaning
- Copilot already tries to do all of this
- Zero project-specific information
- Adds no context about tech stack, patterns, or rules

### Good instruction file:

```markdown
# Order Service

## Stack
Python 3.12, FastAPI 0.115, SQLAlchemy 2.x async, PostgreSQL, Pydantic v2, pytest

## Critical Rules
- All DB access: async with AsyncSession. Never use sync Session.
- Business logic: service layer only. Routers parse requests; services handle logic.
- Validation: Pydantic v2 models at API boundary. No manual isinstance checks.
- Logging: structured with structlog. Never log PII (email, name, payment data).
- Tests: pytest + pytest-asyncio. Mock: all external HTTP calls. Real DB: use test fixtures.

## Do NOT
- Suggest requests library (use httpx)
- Suggest SQLAlchemy 1.x patterns (session.query, session.execute with text without bind)
- Add print() statements
- Use os.system()
- Create inline SQL strings (use ORM or parameterized queries)
```

---

## 7. Monorepo Strategy

```
Problem: One copilot-instructions.md covers the entire monorepo.
A Python backend and a React frontend have different rules.

Solution: Use path-specific instruction files with applyTo globs.

.github/
  copilot-instructions.md              # Team-wide rules ONLY
  instructions/
    python.instructions.md             # applyTo: "backend/**/*.py"
    react.instructions.md              # applyTo: "frontend/**/*.tsx"
    testing.instructions.md            # applyTo: "**/*.test.*", "tests/**"
    github-actions.instructions.md     # applyTo: ".github/workflows/**"
    security.instructions.md           # applyTo: "**"

copilot-instructions.md (root — team-wide only):
  # MonoRepo — Team Rules
  ## Repo Structure
  - backend/: Python FastAPI service
  - frontend/: React/TypeScript SPA
  - shared/: Shared types and contracts (TypeScript)
  - infra/: Terraform and Kubernetes manifests
  
  ## Cross-Cutting Rules
  - All secrets via environment variables or secrets manager
  - No hardcoded localhost URLs — use environment configuration
  - All inter-service communication uses typed contracts from shared/
```

---

## 8. Instruction Conflicts — How to Detect and Resolve

```
Conflict symptom: Copilot gives inconsistent output for similar prompts.

Common conflicts:
  - Root instruction says "use requests"; python.instructions.md says "use httpx"
  - Root instruction says "log all errors"; security.instructions says "never log PII"

Resolution:
  1. Make the more specific instruction win (path-specific > root for path-matched files)
  2. Make the more restrictive security rule always win regardless of conflict
  3. Remove the weaker instruction from the root file

Prevention:
  - Keep the root file to team-wide rules that apply everywhere without exception
  - Keep language-specific rules in path-specific files
  - Review all instruction files together at least quarterly
  - When adding a new rule, search for the opposite rule in other files
```

---

## 9. Revision Checklist

- [ ] Knows where `copilot-instructions.md` lives and what it does
- [ ] Can write a root-level instruction file for a real project
- [ ] Can write path-specific instruction files with correct `applyTo` frontmatter
- [ ] Applies the 5 instruction design principles
- [ ] Can identify the difference between a good and bad instruction
- [ ] Knows the monorepo instruction strategy
- [ ] Can test that instructions are being followed by Copilot
- [ ] Knows how to detect and resolve instruction conflicts
- [ ] Has created at least one instruction file for their practice repo
