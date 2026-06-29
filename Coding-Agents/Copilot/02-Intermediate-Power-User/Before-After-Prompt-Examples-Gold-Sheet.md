# Before / After Prompt Examples — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: Gap Fill (Track File #8a)
> **Purpose**: Concrete before/after comparisons for every major Copilot task type
> **Read after**: Prompt-Files-Slash-Commands-Gold-Sheet.md

---

## How to Use This Sheet

Each example shows:
- **BEFORE**: how most developers write the prompt (produces mediocre output)
- **WHY IT FAILS**: what's missing
- **AFTER**: the improved version (produces targeted, high-quality output)
- **WHAT CHANGED**: the specific improvement

---

## 1. Code Explanation

**BEFORE — vague, no context:**
```
Help me understand this code
```
*Output*: Generic explanation of what "code" typically does. Copilot didn't see any code.

**WHY IT FAILS**: No code attached, no focus area specified.

**AFTER — specific, context attached:**
```
Explain #selection step by step.
Focus on: why a context manager is used here instead of try/finally.
Point out any non-obvious behavior.
Under 200 words.
```
*Output*: Specific explanation of the selected code, explaining the context manager choice with the "why" not just the "what".

**WHAT CHANGED**: `#selection` attaches the code, focus area is specified, length is bounded.

---

## 2. Bug Fixing

**BEFORE — vague, no error:**
```
My code doesn't work, can you fix it?
```
*Output*: Copilot asks what the error is, or guesses and gets it wrong.

**WHY IT FAILS**: No error message, no code, no description of "doesn't work".

**AFTER — error + code + context:**
```
Fix this bug in #selection:
Error: KeyError: 'stripe_charge_id' at line 87 of payment_service.py
Trigger: occurs when processing a refund for an order created before the Stripe migration
Expected: process_refund should handle orders with no stripe_charge_id gracefully
Do NOT: change the function signature or the caller
```
*Output*: Fix specifically targeting the KeyError on orders missing the key, with defensive access, no signature change.

**WHAT CHANGED**: Error is exact, trigger is described, expected behavior is clear, constraints prevent overcorrection.

---

## 3. Test Generation

**BEFORE — no framework, no coverage:**
```
Write tests for this function
```
*Output*: Tests that may use unittest, Jest, JUnit, or whatever Copilot guesses. Happy-path only.

**WHY IT FAILS**: No framework specified, no coverage requirements, no output format.

**AFTER — framework, coverage, naming:**
```
Generate pytest unit tests for #selection using pytest-asyncio.
Cover:
1. Happy path: valid user_id returns User object with id and active=True
2. user_id doesn't exist → raises UserNotFoundError
3. Database is unavailable → raises DatabaseError (not AttributeError)
4. user_id is None → raises TypeError before DB query
5. user_id is negative → raises ValueError

Mock: AsyncSession using AsyncMock(spec=AsyncSession)
Name pattern: test_get_user_<scenario>_<expected_outcome>
Output: complete test file with imports, ready to run.
```
*Output*: 5 specific tests, correct framework, mock pattern, correct naming, complete file.

**WHAT CHANGED**: Framework explicit, each scenario named, mock pattern specified, output format specified.

---

## 4. Code Review / Security

**BEFORE — category-free:**
```
Review this code for issues
```
*Output*: Mixed bag of style suggestions, possible bugs, and vague "security considerations".

**WHY IT FAILS**: "Issues" is undefined — style? bugs? security? performance? all of them?

**AFTER — security-focused with severity:**
```
Security review of #selection.
Check: SQL injection, hardcoded credentials, PII in logs, missing auth checks, error disclosure.
For each finding:
  SEVERITY: CRITICAL/HIGH/MEDIUM/LOW
  ISSUE: [what it is]
  ATTACK VECTOR: [how exploited]
  FIX: [specific code change — not generic advice]
If no issues in a category: state "No issues found".
```
*Output*: Structured findings with severity, specific attack vectors, and exact code fixes.

**WHAT CHANGED**: Category is security-specific, structure is defined, "no issues" is handled explicitly.

---

## 5. Refactoring

**BEFORE — no constraint:**
```
Refactor this to be better
```
*Output*: Copilot rewrites everything, changes the function signature, adds new abstractions, and calls it an improvement.

**WHY IT FAILS**: "Better" is undefined, no constraints on what to preserve.

**AFTER — goal + constraints + output format:**
```
Refactor #selection:
Goal: extract the email validation logic into a separate EmailValidator class
Preserve: public method signatures on UserService — callers must not change
Preserve: all existing tests must pass unchanged
Do NOT: add new dependencies, create abstract base classes, or refactor beyond this goal
Output: 
  1. New EmailValidator class
  2. Updated UserService (minimal change)
  3. Bullet list of what changed and why
```
*Output*: Focused extraction of one concern, no scope creep, caller-safe, with change explanation.

**WHAT CHANGED**: Goal is specific (one class extraction), two explicit preservations, scope-creep prevention, output format defined.

---

## 6. GitHub Actions Workflow

**BEFORE — too vague:**
```
Create a CI workflow
```
*Output*: Generic workflow that runs on ubuntu-latest, may use @latest for actions (security risk), no caching, no concurrency group.

**WHY IT FAILS**: Language unknown, trigger unknown, no security requirements specified.

**AFTER — stack explicit + security requirements:**
```
Create a GitHub Actions CI workflow for a Python 3.12 project using Poetry.

Trigger: push to main and develop, pull_request targeting main
Steps (in this order):
  1. actions/checkout@v4
  2. actions/setup-python@v5 with Python 3.12
  3. Cache pip/Poetry dependencies (key on poetry.lock hash)
  4. Poetry install --no-root
  5. ruff check . (fail on any error)
  6. mypy src/ (strict mode)
  7. pytest tests/unit/ -v --tb=short (fail if any fail)
  8. Upload coverage report as artifact

Requirements:
  - Pin ALL action versions (not @latest)
  - Add concurrency group to cancel stale PR runs
  - timeout-minutes: 15 per job
  - Descriptive step names
  - Secrets for any credentials via ${{ secrets.NAME }}
```
*Output*: Complete, secure, production-ready workflow with pinned versions, caching, and concurrency.

**WHAT CHANGED**: Stack specified, steps listed in order, security requirements explicit, no @latest.

---

## 7. Agent Mode Task

**BEFORE — open-ended:**
```
Build me a notification system
```
*Output*: Agent Mode creates 20 files, invents patterns not used in the project, doesn't test anything, and takes 15 minutes to go in the wrong direction.

**WHY IT FAILS**: No scope, no constraints, no plan requirement, no success criteria.

**AFTER — scoped with plan requirement:**
```
Context:
  This is a Python FastAPI service. Existing patterns: #file:src/api/users.py (router),
  #file:src/services/user_service.py (service). We use SQLAlchemy async + asyncpg.

Goal:
  Add a user notification preferences API — users can set which notification
  channels they receive (email=true/false, in_app=true/false, sms=true/false).

Requirements:
  - GET /users/{user_id}/notification-preferences → current prefs (404 if user not found)
  - PATCH /users/{user_id}/notification-preferences → update prefs (at least one must remain true)
  - Pydantic validation, SQLAlchemy model, repository pattern
  - Tests for: happy path, all-disabled validation, user not found

Constraints:
  - Follow EXACTLY the pattern in src/api/users.py (router) and src/services/user_service.py (service)
  - Do NOT create any new base classes or abstractions
  - Do NOT modify any existing files outside this feature scope
  - Do NOT run database migrations (flag that alembic needs to be run)

Plan first:
  List the exact files to create/modify, with one sentence per file.
  Wait for my approval before creating anything.
```
*Output*: Agent Mode presents a focused 4-file plan (model, schema, service, router + test) that exactly mirrors the existing pattern, then implements only what's listed.

**WHAT CHANGED**: Context attaches real files (pattern to follow), requirements are specific and testable, constraints prevent scope creep, plan-first prevents drift.

---

## 8. Debugging

**BEFORE — vague:**
```
Why is my code slow?
```
*Output*: Generic "performance tips" — use caching, reduce DB queries, profile your code. Not useful.

**WHY IT FAILS**: No code, no metrics, no symptoms.

**AFTER — specific symptom + code:**
```
Performance problem in #selection:
  Symptom: /api/orders endpoint returns 500 orders in 8 seconds. Expected: < 200ms.
  Observation: CPU stays low during the 8 seconds, so it's likely I/O not compute.
  
Diagnose:
1. Is there an N+1 query pattern? (ORM lazy loading producing one query per order)
2. Any sequential awaits that could run in parallel?
3. Any missing indexes visible from the query pattern?

Show: the specific line causing the problem and the fix.
Under 200 words.
```
*Output*: Identifies the N+1 pattern in the ORM relationship loading, shows exactly which line uses lazy loading, and provides the selectinload fix.

**WHAT CHANGED**: Symptom is measured (8 seconds vs 200ms target), observation narrows the category (I/O not CPU), diagnostic questions guide Copilot to the right root cause.

---

## 9. Architecture Question

**BEFORE — opinion-seeking:**
```
Is my architecture good?
```
*Output*: Generic "yes, looks good!" or generic design principles not specific to the code.

**WHY IT FAILS**: "Good" is undefined, no code attached, no specific concern.

**AFTER — specific concern + code + evaluation criteria:**
```
Architecture review of #file:src/services/order_service.py:

Evaluate specifically:
1. Does any method do more than one thing? (single responsibility)
2. Would adding a new payment method require modifying this file? (open/closed)
3. Can process_order() be unit tested without a real database? (testability)

For each issue found:
  - Name the SOLID principle violated
  - Show the specific method/line
  - Suggest a concrete refactoring (1-3 lines of change, not a full rewrite)

Under 300 words.
```
*Output*: Identifies that process_order() directly calls session.commit() (making it untestable), suggests injecting a unit of work, and flags that adding a payment method requires modifying the orchestration method (OCP violation).

**WHAT CHANGED**: Evaluation criteria specific, code attached, output format defined, length bounded.

---

## 10. Documentation

**BEFORE — format-free:**
```
Write a README for this project
```
*Output*: A README with inconsistent sections, commands that might not run, and vague descriptions that don't match the actual project.

**WHY IT FAILS**: No structure defined, no audience specified, no source of truth for content.

**AFTER — structure + source + audience:**
```
Generate README.md for this project.

Source of truth: #file:.copilot-context.md + #codebase structure

Required sections (in this order):
## What This Does [2 sentences max]
## Prerequisites [bullet list with versions]
## Installation [numbered steps — every command must be copy-paste runnable]
## Configuration [env vars table: name | required | description | example value]
## Running Locally [exact command + where to access the result]
## Running Tests [exact command]
## Project Structure [directory tree with one-line description per dir]
## Contributing [3-bullet summary]

Rules:
- Every command: exact, runnable, no "set up your environment first"
- Config table: list ALL env vars in .env.example
- Target reader: developer who has never seen this repo before
- Max: 400 words
```
*Output*: Structured README with runnable commands, accurate env var table sourced from actual files, appropriate length.

**WHAT CHANGED**: Structure is defined, source of truth is specified, commands must be runnable, length is bounded.

---

## Quick Comparison Table

| Task | Worst Pattern | Best Pattern |
|---|---|---|
| Explain code | "Explain this" (no code) | `Explain #selection. Focus on: [aspect]. Under 200 words.` |
| Fix bug | "My code doesn't work" | Error + code + trigger + expected behavior + constraints |
| Generate tests | "Write tests" | Framework + scenarios + mock targets + naming + output format |
| Security review | "Check for issues" | Specific categories + severity labels + fix format |
| Refactor | "Make this better" | Goal + what to preserve + what NOT to do + output format |
| GitHub Actions | "Create a CI" | Stack + trigger + steps + security requirements |
| Agent Mode | "Build X" | Context + goal + requirements + constraints + plan-first |
| Performance | "Why is it slow?" | Measured symptom + code + diagnostic questions |
| Architecture | "Is this good?" | Specific concerns + code + evaluation criteria |
| Documentation | "Write a README" | Structure + source of truth + audience + length |
