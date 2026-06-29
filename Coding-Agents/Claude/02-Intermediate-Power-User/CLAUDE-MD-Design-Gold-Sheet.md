# CLAUDE.md Design — Gold Sheet

> **Track**: Claude Mastery Track — Group 2: Intermediate Power User
> **File**: 1 of 7 (Track File #7)
> **Audience**: Developers configuring Claude for consistent expert-level output
> **Read after**: Claude-For-Beginners-Quick-Wins-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| CLAUDE.md anatomy — what goes where | ★★★★★ | Devs write too much (noise) or too little (Claude uses defaults) |
| Root vs subfolder CLAUDE.md hierarchy | ★★★★★ | Subfolder overrides are how you handle monorepos and multi-domain projects |
| What CLAUDE.md cannot do | ★★★★★ | Devs expect CLAUDE.md to do everything — understanding limits prevents frustration |
| Instruction design: concrete > abstract | ★★★★★ | "Follow best practices" adds zero information. "Use httpx not requests" does. |
| Testing your CLAUDE.md | ★★★★★ | Devs write CLAUDE.md and never verify Claude follows it |
| Bad vs good CLAUDE.md examples | ★★★★★ | A single concrete comparison is worth 1000 words of theory |

---

## 2. What CLAUDE.md Is

### Must Know

```
CLAUDE.md is read by Claude Code at the start of every session.
It is your project's persistent memory — the rules Claude carries into every task.

Without CLAUDE.md:
  Claude uses its defaults. For coding: reasonable but generic.
  It doesn't know your stack, your patterns, or your forbidden antipatterns.
  Every session: you repeat "we use pytest" and Claude forgets it next session.

With CLAUDE.md:
  Claude knows your project before you say a word.
  Conventions are applied automatically.
  Forbidden patterns are avoided automatically.
  You never repeat project context in prompts.

Location:
  ./CLAUDE.md           ← root-level, applies to the entire project
  ./src/CLAUDE.md       ← subfolder override, applies only to src/
  ./tests/CLAUDE.md     ← subfolder override, applies only to tests/
  ~/.claude/CLAUDE.md   ← personal defaults, all projects, all machines

Hierarchy: subfolder CLAUDE.md overrides root CLAUDE.md for files in that subfolder.
```

---

## 3. Root CLAUDE.md — Anatomy

### Full Template with Explanation

```markdown
# [Project Name]
<!-- One-line description of what this project is -->

## Project Overview
<!-- 2-3 sentences: what it does, who uses it, current state -->
<!-- This gives Claude the business context that shapes every decision -->

## Tech Stack
<!-- Be specific with versions — Claude will use these when generating code -->
- Language: Python 3.12
- Framework: FastAPI 0.115
- Database: PostgreSQL 16 + SQLAlchemy 2.x async + asyncpg
- Validation: Pydantic v2
- Testing: pytest + pytest-asyncio
- HTTP client: httpx (NOT requests)
- Dependency management: Poetry

## Architecture
<!-- Key structural rules that must be followed -->
- Layered: Router → Service → Repository → DB (strict, no cross-layer access)
- All DB operations: async with AsyncSession. Never use sync SQLAlchemy.
- Error handling: custom exceptions from src/exceptions.py, handled in middleware
- Auth: JWT stateless tokens. No sessions. Dependency: Depends(get_current_user)

## Coding Conventions
<!-- Things Claude might get wrong without explicit instruction -->
- Type hints required on all public functions and methods
- f-strings for string formatting. Never % or .format()
- Classes: PascalCase. Functions/vars: snake_case. Constants: UPPER_SNAKE_CASE
- Max function length: 30 lines. Extract longer logic into helpers.

## Testing Requirements
<!-- How tests should be written in this project -->
- Every public method needs: happy path, error case, edge case
- Mock all external services in unit tests: HTTP, email, SMS
- DB tests: use AsyncMock(spec=AsyncSession) fixture
- Test naming: test_<function>_<scenario>_<expected_outcome>

## Do NOT
<!-- The most important section — prevents Claude's most common wrong turns -->
- Do not use requests library — use httpx
- Do not use print() for logging — use the logging module or structlog
- Do not add new external dependencies without listing them in pyproject.toml
- Do not use bare except: — always catch specific exceptions
- Do not use os.system() — use subprocess.run() with shell=False
- Do not add abstractions not needed for this specific task
- Do not generate code with hardcoded credentials or API keys
- Do not run database migrations — flag that alembic needs to be run
```

---

## 4. Subfolder CLAUDE.md — Override Strategy

### When to Use Subfolder CLAUDE.md

```
Use subfolder CLAUDE.md when:
  - Different conventions apply in different directories
  - You're working in a monorepo with different tech stacks
  - A specific directory has constraints not shared by the project

Examples:
  tests/CLAUDE.md     → test-specific rules (fixtures, mocking, naming)
  src/api/CLAUDE.md   → API-specific rules (auth required, Pydantic schemas)
  infra/CLAUDE.md     → infrastructure rules (never apply changes automatically)
  frontend/CLAUDE.md  → frontend-specific stack (React, TypeScript)
```

### Example: `tests/CLAUDE.md`

```markdown
# Test Directory Rules

This directory contains pytest tests. Apply these rules for all files here.

## Test Framework
- pytest with pytest-asyncio (@pytest.mark.asyncio on async tests)
- fixtures from tests/conftest.py — check before creating new ones

## Mocking Policy
- DB session: AsyncMock(spec=AsyncSession) — always
- HTTP calls: use respx for httpx mocking
- Email/SMS: AsyncMock(spec=EmailService)
- Time: mock datetime.utcnow() for time-dependent tests

## Test Naming
test_<function>_<scenario>_<expected_outcome>

## Do NOT (in tests only)
- Do not call real external APIs
- Do not call session.commit() in tests using db_session fixture (rollback handles it)
- Do not share state between tests
- Do not create new fixtures that duplicate existing ones in conftest.py
```

### Example: `infra/CLAUDE.md`

```markdown
# Infrastructure Directory Rules

This directory contains Terraform and Kubernetes manifests.

## Critical Rules
- NEVER apply infrastructure changes automatically (no terraform apply or kubectl apply)
- ALWAYS plan before change: show me what will change before making it
- ALWAYS flag destructive operations (deleting resources, changing VPC, etc.)

## Review Required
Any change to these files requires human review before execution.
Claude may generate and propose changes, but must not execute them.
```

---

## 5. What Goes in CLAUDE.md — and What Doesn't

### Good Content (puts in CLAUDE.md)

```
✓ Tech stack with specific versions
✓ Architecture rules (layer separation, async requirements)
✓ Library preferences (httpx not requests)
✓ Coding conventions that override Claude's defaults
✓ Testing requirements (framework, naming, mock targets)
✓ Explicit DO NOT rules
✓ Current project state (what's done, what's in progress) — update weekly
✓ Key files to know about ("see src/exceptions.py for error hierarchy")
```

### Bad Content (do not put in CLAUDE.md)

```
✗ Things Claude already does by default
  "Write readable code" → Claude already tries to do this
  "Add comments" → already default behavior
  "Follow PEP 8" → Claude already knows PEP 8

✗ Vague aspirational rules
  "Follow best practices" → meaningless
  "Write clean code" → undefined

✗ Things that belong in prompts (too specific)
  "The current task is to implement payment refunds" → use prompt, not CLAUDE.md
  
✗ Redundant library documentation
  Don't paste SQLAlchemy docs into CLAUDE.md — Claude knows SQLAlchemy

✗ Credentials or secrets of any kind
```

---

## 6. Bad vs Good CLAUDE.md

### Bad CLAUDE.md

```markdown
# My Project

Please write good code that follows best practices and is clean and maintainable.
Use appropriate design patterns. Make sure the code is tested and secure.
Follow the team's conventions. Be helpful and provide explanations when needed.
```

Problems:
- Zero project-specific information
- Claude already tries to do all of this
- Adds no context about stack, patterns, or rules
- No "Do NOT" constraints

### Good CLAUDE.md

```markdown
# Order Processing Service

FastAPI async REST service for e-commerce order management.
PostgreSQL database via SQLAlchemy 2.x async + asyncpg.

## Stack
Python 3.12, FastAPI 0.115, SQLAlchemy 2.x async, Pydantic v2, pytest

## Architecture
- Router → Service → Repository → DB (strict layers, no cross-access)
- All DB: async with AsyncSession. Never sync SQLAlchemy.
- Custom exceptions: src/exceptions.py (AppError base class)

## Conventions
- Type hints: required on all public functions
- Logging: structlog (never print())
- HTTP: httpx only (never requests)

## Testing
- pytest + pytest-asyncio
- Mock: AsyncMock(spec=AsyncSession) for all DB sessions
- Names: test_<function>_<scenario>_<expected>

## Do NOT
- Do not use os.system()
- Do not add new packages without listing in pyproject.toml
- Do not hardcode any values — use config.py for all settings
- Do not run migrations — flag them for manual execution
```

---

## 7. Testing Your CLAUDE.md

### Verification Prompts

```bash
# After creating or updating CLAUDE.md, verify Claude loads it:

# Test 1: confirm loading
claude "What project rules do you have for this codebase?"
# Expected: Claude summarizes your CLAUDE.md conventions

# Test 2: test a specific rule
claude "Generate a function to make an HTTP request to an external API"
# Expected: Claude uses httpx (if in CLAUDE.md "use httpx not requests")
# If uses requests: rule not loading or not specific enough

# Test 3: test a Do NOT rule
claude "Add debug output to trace the order processing flow"
# Expected: Claude uses logging.debug(), not print()
# If uses print(): Do NOT rule not specific enough

# Test 4: test architecture rule
claude "Add a query to get all active orders in the OrderController"
# Expected: Claude says to move DB access to the repository layer
# If writes DB query in controller: architecture rule needs reinforcement
```

---

## 8. CLAUDE.md Maintenance Schedule

```
When to update CLAUDE.md:
  - When you adopt a new library or drop an old one
  - When you establish a new architectural pattern
  - When Claude keeps making the same wrong choice (add a Do NOT rule)
  - Weekly: update "Current State" section (what's done, what's in progress)

When NOT to update:
  - When the issue is a one-time task (use a prompt, not CLAUDE.md)
  - When the rule duplicates what Claude already does by default

Quarterly audit:
  Read every rule in CLAUDE.md:
    Still accurate? → keep
    Changed (we adopted X)? → update
    No longer relevant? → remove
  Short CLAUDE.md > Long CLAUDE.md. Quality > quantity.
```

---

## 9. Revision Checklist

- [ ] Knows CLAUDE.md hierarchy: personal → root → subfolder
- [ ] Has created a root CLAUDE.md for at least one project
- [ ] CLAUDE.md includes: stack (with versions), architecture rules, Do NOT rules
- [ ] CLAUDE.md does NOT include: vague aspirational rules, Claude defaults
- [ ] Has run verification tests to confirm CLAUDE.md is loaded and followed
- [ ] Has subfolder CLAUDE.md for tests/ if testing conventions differ
- [ ] Knows what belongs in CLAUDE.md vs what belongs in a prompt
- [ ] Has a maintenance schedule for CLAUDE.md (weekly state + quarterly audit)
