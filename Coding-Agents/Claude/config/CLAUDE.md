# [Project Name]
# Copy this file to: CLAUDE.md at your project root
# Customize every section marked [CUSTOMIZE]
# Delete sections you don't need — shorter is better

## Project Overview
[CUSTOMIZE: 2-3 sentences: what this project does, who uses it, current state]

## Tech Stack
[CUSTOMIZE: be specific with versions]
- Language: Python 3.12
- Framework: FastAPI 0.115
- Database: PostgreSQL 16 + SQLAlchemy 2.x async + asyncpg
- Validation: Pydantic v2
- Testing: pytest + pytest-asyncio
- HTTP: httpx (NOT requests — this is important)
- Dependency management: Poetry

## Architecture
[CUSTOMIZE: key structural rules]
- Layered: Router → Service → Repository → DB (strict, no cross-layer access)
- All DB operations: async with AsyncSession only — never sync SQLAlchemy
- Error handling: custom exceptions from src/exceptions.py, handled in middleware
- Authentication: JWT stateless — Depends(get_current_user) on protected routes

## Coding Conventions
[CUSTOMIZE: things Claude might get wrong without explicit instruction]
- Type hints: required on all public functions
- String formatting: f-strings only (never % or .format())
- Naming: Classes=PascalCase, functions/vars=snake_case, constants=UPPER_SNAKE_CASE
- Max function length: 30 lines. Extract longer logic into named helpers.

## Testing Requirements
[CUSTOMIZE: how tests should be written in this project]
- Framework: pytest + pytest-asyncio (@pytest.mark.asyncio on async tests)
- Mock targets: AsyncMock(spec=AsyncSession) for DB, AsyncMock for external services
- Naming: test_<function>_<scenario>_<expected_outcome>
- Coverage: every public method needs happy path + error case + edge case

## Agents
# These specialist agents are available. Use them for focused tasks.
# @planner  → before any implementation (design and file list)
# @builder  → execute approved plan, one file at a time with verification
# @debugger → root cause analysis, never guess
# @tester   → generate tests with fresh context (no author bias)
# @reviewer → security + correctness + coverage review before merge
# @architect → architecture review and design trade-offs
# @optimizer → performance analysis with evidence (never speculate)
#
# Pipeline for features: @planner → @builder → @tester → @reviewer
# Use a separate Claude session for each agent (context isolation)

## Hooks
# Validation hooks are active in .claude/hooks/
# pre_tool_use.sh  → blocks: rm -rf, DROP TABLE, git push --force, migrations, production URLs
# post_tool_use.sh → checks modified files for secrets, runs quick regression tests
# on_error.sh      → logs errors, provides recovery guidance
#
# Never disable hooks. If a legitimate command is blocked: update the hook allowlist.

## Skills
# These skills auto-invoke when Claude detects the relevant task type.
# Testing    → .claude/skills/testing/SKILL.md    (when: test generation, gap analysis)
# Refactoring → .claude/skills/refactoring/SKILL.md (when: refactor, extract, simplify)
# Documentation → .claude/skills/documentation/SKILL.md (when: README, docstring, ADR)
# Performance → .claude/skills/performance/SKILL.md (when: slow, bottleneck, N+1)

## Key Files
# [CUSTOMIZE: list 3-5 files developers must know about]
# src/exceptions.py  → custom exception hierarchy (all exceptions inherit from AppError)
# src/config.py      → all configuration via pydantic-settings (no hardcoded values)
# tests/conftest.py  → shared test fixtures (db_session, test_user, mock_email_service)

## Do NOT
[CUSTOMIZE: antipatterns specific to this project — this is the most important section]
- Do not use requests library — use httpx
- Do not use print() for debugging — use the logging module
- Do not add new external dependencies without listing them in pyproject.toml
- Do not use bare except: — always catch specific exception types
- Do not use os.system() — use subprocess.run() with shell=False
- Do not add abstractions not needed for the specific task requested
- Do not generate code with hardcoded credentials or API keys
- Do not run database migrations — flag that alembic needs to be run manually
- Do not modify test files to make tests pass — fix the implementation
- Do not add new classes or abstractions without explicit request

## Current State
[CUSTOMIZE: update weekly — helps Claude understand project context]
Updated: [date]
- Done: [list of completed major components]
- In progress: [current focus]
- Next: [upcoming work]
- Blockers: [any open architectural or technical questions]
