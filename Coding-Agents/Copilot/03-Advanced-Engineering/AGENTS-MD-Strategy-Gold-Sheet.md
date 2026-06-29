# AGENTS.md Strategy — Gold Sheet

> **Track**: Copilot Mastery Track — Group 3: Advanced Engineering
> **File**: 2 of 7 (Track File #15)
> **Audience**: Developers managing multi-agent setups and multi-tool AI workflows
> **Read after**: Custom-Agents-Deep-Dive-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| AGENTS.md vs copilot-instructions.md — key differences | ★★★★★ | Devs mix the two files; one is AI-tool-agnostic, one is Copilot-specific |
| Root vs folder-level AGENTS.md hierarchy | ★★★★★ | Folder-level files override root — this is the power and the confusion |
| Multi-AI-tool compatibility strategy | ★★★★☆ | AGENTS.md is read by Claude, OpenAI Codex agents, and Copilot — keep it generic |
| Portable workspace setup | ★★★★☆ | Workspace that works on any machine with any AI tool is a career asset |
| Domain-specific AGENTS.md patterns | ★★★★☆ | Backend, frontend, testing, security each benefit from specialist instructions |
| Preventing contradictory instructions | ★★★★☆ | Contradictions produce random behavior — prevention is better than debugging |

---

## 2. AGENTS.md vs copilot-instructions.md

### The Key Difference

```
copilot-instructions.md:
  - Copilot-specific
  - Loaded automatically by GitHub Copilot in VS Code
  - Uses Copilot-specific features (path-specific via .github/instructions/)
  - Not read by other AI tools
  - Lives in: .github/copilot-instructions.md

AGENTS.md:
  - Tool-agnostic agent instruction file
  - Read by Copilot Agent Mode, Claude, Codex CLI agents, and other tools
  - Meant to describe how AI agents should behave in THIS repository
  - Lives in: repo root (AGENTS.md) or any subfolder
  - Can exist at multiple levels with folder-level overriding root-level
  - Typically read at session start when an AI agent is invoked in that directory
```

### When to Use Each

```
Use copilot-instructions.md for:
  - Copilot Chat and inline context (loaded in every conversation)
  - Copilot-specific settings (model preferences, slash command guidance)
  - Project conventions for Copilot Chat Ask/Edit sessions

Use AGENTS.md for:
  - Multi-step autonomous agent behavior rules
  - Workflow instructions (what to do before making changes, how to test)
  - Rules that should apply to ANY AI agent tool that reads the repo
  - Safety rules (commit first, test after, never delete without confirmation)
  - Directory-specific behavioral rules
```

---

## 3. Root-Level AGENTS.md

### Template

```markdown
# AGENTS.md — Root Level

This file provides behavioral instructions for AI agents operating in this repository.
Applies to: GitHub Copilot Agent Mode, Claude code agents, OpenAI Codex CLI, and similar tools.

## Repository Overview
[2-3 sentences: what this repo is, what it does, who uses it]

## Tech Stack
[Language, frameworks, database, testing framework — one line each]

## Directory Structure
[Describe what each major directory contains]
  src/           — Application source code
  src/api/       — FastAPI routers and endpoints
  src/services/  — Business logic layer
  src/models/    — SQLAlchemy ORM models
  src/schemas/   — Pydantic request/response schemas
  tests/         — Test suite (mirrors src/ structure)
  .github/       — GitHub Actions, Copilot configuration

## Before Making Changes
1. Run existing tests to establish baseline: pytest tests/ -v
2. Create a git commit with a meaningful message as a checkpoint
3. Plan the changes before implementing: describe files to be modified and why

## Code Standards
[Your non-negotiable coding standards — brief]
- Python 3.12+ syntax (match/case, X | Y unions, list[X] not List[X])
- All async code. Never use sync SQLAlchemy in async context.
- Type hints required on all functions.
- Tests required for all public methods.

## Testing Policy
- Run tests after every file change
- All existing tests must pass before completing a task
- New code requires new tests — do not skip test generation

## What Agents Must NOT Do
- Do not delete files without explicit confirmation in the chat
- Do not modify files in .github/workflows/ without explicit request
- Do not add new third-party dependencies without listing them in pyproject.toml
- Do not run database migrations (alembic upgrade) — flag that this is needed and stop
- Do not commit with --force or --no-verify flags
- Do not run shell commands that modify system configuration

## Handoff Pattern
When a task is complete:
1. Run tests: pytest tests/ -v
2. Report which tests passed and which failed
3. Summarize what was changed and why
4. List any follow-up tasks not implemented
5. Stop — do not start the next task without confirmation
```

---

## 4. Folder-Level AGENTS.md Patterns

### Backend AGENTS.md — `src/AGENTS.md`

```markdown
# src/AGENTS.md

Applies to all agents working within the src/ directory.
Overrides: Nothing in root AGENTS.md — adds to it.

## Architecture Rules
- Services call repositories for DB access. Services NEVER call session.query() directly.
- Routers (API layer) call services only. Routers NEVER access the database directly.
- Models define the database schema only. Models NEVER contain business logic.
- Schemas define API request/response shapes. Schemas NEVER query the database.

## Async Requirements
All new code in src/ must use async/await.
If a function needs to be synchronous for a specific reason, add a comment explaining why.

## Import Order (enforced by ruff)
1. Standard library
2. Third-party packages
3. Internal packages (src.*)
Do not mix these groups. Blank line between each group.

## Error Handling
Custom exceptions live in src/exceptions.py.
Raise custom exceptions from service layer.
Let the exception middleware in src/middleware/error_handler.py convert to HTTP responses.
Do NOT catch-and-swallow exceptions. Always re-raise or convert.
```

### Tests AGENTS.md — `tests/AGENTS.md`

```markdown
# tests/AGENTS.md

Applies to agents working within the tests/ directory.

## Test Strategy
Unit tests: mock all external dependencies (DB, HTTP, email).
Integration tests: use real test database from TEST_DATABASE_URL environment variable.
Never create integration tests that require real external API credentials.

## Fixture Policy
Common fixtures live in tests/conftest.py.
Do not create new fixtures that duplicate existing ones.
Check conftest.py before creating any new fixture.

## Database Fixtures
db_session: provides an AsyncSession with automatic rollback after each test.
test_user: provides a pre-created User object in the test database.
Do NOT use session.commit() in tests that use db_session fixture (rollback handles cleanup).

## Mocking Policy
External HTTP: use respx for httpx mocking.
Time: mock datetime.utcnow() when testing time-dependent logic.
Email service: use AsyncMock(spec=EmailService).
Never let a test make a real HTTP call to an external API.
```

### Security AGENTS.md — `src/api/AGENTS.md`

```markdown
# src/api/AGENTS.md

Applies to agents working within the API layer.

## Authentication Requirement
Every new endpoint must have authentication unless explicitly marked public.
Public endpoints must have a comment: # PUBLIC: accessible without authentication
Authentication: use the Depends(get_current_user) dependency.

## Input Validation
All POST/PUT/PATCH endpoints must use a Pydantic schema as the request body type.
Never access request.body or request.json() directly.
Query parameters: use Annotated types with validators where applicable.

## Response Schemas
All endpoints must have explicit response_model or specify response type annotation.
Never return ORM model objects directly — use Pydantic response schemas.

## Error Responses
Use HTTPException with appropriate status codes:
  400: validation or business rule violation
  401: not authenticated
  403: authenticated but not authorized
  404: resource not found
  409: conflict (e.g., duplicate)
  500: unexpected error (never expose internal details)
```

---

## 5. Multi-AI-Tool Compatibility

```
AGENTS.md is designed to be tool-agnostic.
These AI tools read AGENTS.md:
  - GitHub Copilot (Agent Mode)
  - Anthropic Claude (when used with code tools)
  - OpenAI Codex CLI (--instructions mode)
  - Cursor (reads .cursorrules but also AGENTS.md in some configurations)
  - Cline, Continue, and other VS Code AI extensions

To keep AGENTS.md compatible across all tools:
  1. Use plain language — no Copilot-specific syntax
  2. Avoid referencing specific AI tool features (no "use @codebase variable")
  3. Focus on BEHAVIOR rules (what to do, what not to do)
  4. Focus on PROCESS rules (test before commit, plan before implement)
  5. Keep it under 500 words total (most tools load the full file)

Copilot-specific config: keep in .github/copilot-instructions.md
Tool-agnostic behavior: keep in AGENTS.md
```

---

## 6. Preventing Contradictory Instructions

```
Common contradictions to watch for:

Root AGENTS.md says:     "Use requests for HTTP"
src/AGENTS.md says:      "Use httpx for HTTP"
→ Conflict: which wins?

Resolution rules:
  1. Folder-level files override root for files in that folder
  2. More specific = higher priority (file-level > folder > root)
  3. For contradictions at the SAME level: the security rule wins
  4. Eliminate the contradiction by removing the weaker statement

Prevention:
  - Keep root AGENTS.md to broad behavioral rules only (test policy, safety rules)
  - Keep technology choices in folder-level or domain-level files
  - Review all AGENTS.md files together when adding a new rule
  - Add a comment when a rule is intentionally different from the parent level:
    # Override: this folder uses httpx despite root recommendation of requests
    # Reason: async code requires non-blocking HTTP client
```

---

## 7. Portable Workspace Setup

```
A portable workspace works identically on any machine with any AI tool.

Required files for portability:
  AGENTS.md                         → root behavioral rules
  .github/copilot-instructions.md   → Copilot-specific (auto-loaded by Copilot)
  .github/instructions/*.instructions.md → path-specific rules
  .github/prompts/*.prompt.md       → prompt library (team-shared)
  .github/agents/*.agent.md         → custom agents (team-shared)
  .vscode/settings.json             → VS Code settings (committed for consistency)
  .vscode/extensions.json           → recommended extensions list

What makes a workspace NOT portable:
  - Instruction files that reference local paths: /Users/myname/...
  - Prompt files that hardcode team-specific details
  - Agents that reference external services not everyone has access to
  - Settings that only work on macOS (e.g., macOS-specific font paths)

Test portability:
  Clone the repo on a fresh machine → open in VS Code → verify:
  - Copilot instructions load (check in a Chat response)
  - Prompt files appear as slash commands (type / in Chat)
  - Custom agents appear in the agent picker
  → If all three work: workspace is portable
```

---

## 8. Revision Checklist

- [ ] Can explain the difference between AGENTS.md and copilot-instructions.md
- [ ] Knows folder-level AGENTS.md overrides root-level
- [ ] Can write a root-level AGENTS.md following the template
- [ ] Has created folder-level AGENTS.md for at least backend and tests directories
- [ ] Keeps AGENTS.md tool-agnostic (no Copilot-specific syntax)
- [ ] Can identify and resolve contradictory instructions across levels
- [ ] Has a portable workspace setup that works on any machine
