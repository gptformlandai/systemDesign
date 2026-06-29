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

## Skills
[These tell Claude to follow the skill files automatically]
- Testing: follow .claude/skills/testing/SKILL.md for all test generation
- Refactoring: follow .claude/skills/refactoring/SKILL.md for refactoring tasks
- Documentation: follow .claude/skills/documentation/SKILL.md for doc generation
- Performance: follow .claude/skills/performance/SKILL.md for optimization tasks

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

## Current State
[CUSTOMIZE: update weekly — helps Claude understand project context]
Updated: [date]
- Done: [list of completed major components]
- In progress: [current focus]
- Next: [upcoming work]
