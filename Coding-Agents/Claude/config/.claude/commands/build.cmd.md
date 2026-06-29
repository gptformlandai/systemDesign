---
description: Scaffold a new project or feature using plan-first approach
---

Build the following:

$ARGUMENTS

Process — ALWAYS follow this order:

## Phase 1: PLAN (before any file creation)
List exactly:
1. Files to create (exact paths + one-sentence purpose each)
2. Files to modify (exact paths + one-sentence change each)
3. Build order (what must exist before what)
4. Assumptions you're making
5. Dependencies on existing code

Post the plan. Wait for my approval before creating any file.

## Phase 2: BUILD (after explicit approval)
Create files in dependency order:
- After each file: run available lint and tests
- Fix any failures before moving to the next file

## Phase 3: VERIFY
1. Run full test suite
2. Run linting
3. Report: test count, lint status, any remaining issues

Quality bar:
- Production-starting-point quality (not hello world)
- Type hints on all public functions
- Error handling for expected failures
- At least one test per new file
- No hardcoded values — all configurable via environment
- README commands must be copy-paste runnable
