# Feature Building Scenarios — Gold Sheet

> **Track**: Codex Mastery Track — Group 5: Scenario Practice
> **File**: 2 of 4 (Track File #27)
> **Audience**: Developers who want to deliver complete features using Codex systematically
> **Read after**: Daily-Workflow-Scenarios-Gold-Sheet.md

---

## ⭐ Beginner Tier

### B1: Build a small module test-first (15 minutes)

```bash
# Task: build a simple email validator module

# Step 1: Write tests first (define what "done" means)
codex --approval-policy auto-edit \
  "Create tests/test_email_validator.py.
   Test cases for a validate_email(email: str) -> bool function:
   - valid email: should return True
   - missing @: should return False
   - missing domain: should return False
   - empty string: should return False
   - None input: should return False
   
   Function doesn't exist yet — these tests define the spec.
   Run: pytest tests/test_email_validator.py --collect-only (should collect 5 tests)"

# Step 2: Implement to make tests pass
codex --approval-policy auto-edit \
  "Implement validate_email() in src/utils/email_validator.py.
   Make all tests in tests/test_email_validator.py pass.
   Do not modify the test file.
   Verification: pytest tests/test_email_validator.py -v"
```

### B2: Extend a feature safely

```bash
# Task: add an optional "priority" field to an existing order model

# Explore before touching
codex "I need to add a 'priority' field (optional, values: low/medium/high, default: low) 
       to the Order model in src/orders/models.py.
       
       Before I change anything:
       1. What files reference Order that would need updating?
       2. What tests would break if I add this field?
       3. What's the safest order to make these changes?
       
       Exploration only — no changes."
```

---

## Scenario 1 — Full Feature from Scratch (45 minutes)

**Feature**: "Add a GET /users/{id}/activity endpoint that returns the last 10 actions by a user."

```bash
# Phase 1: Plan (10 min)
codex --model gpt-4.1 --approval-policy suggest \
  "Design GET /users/{id}/activity endpoint.
   
   System context: FastAPI + SQLAlchemy + PostgreSQL
   Auth: JWT token required, user can only see own activity unless admin
   
   Design:
   1. Data model: what table/fields store user activity?
   2. Repository method: what query?
   3. Service method: what business logic? auth check?
   4. API endpoint: request params, response schema, error responses
   5. Tests needed: list specific test cases
   
   Do not implement. Output: structured implementation plan."

# Phase 2: Implement (25 min)
codex --approval-policy auto-edit \
  "Implement GET /users/{id}/activity following this plan: [paste Phase 1 output]
   
   Files: src/db/activity_repository.py, src/services/user_service.py, src/api/users.py
   Follow patterns from: src/db/order_repository.py, src/api/users.py (existing auth check)
   
   Process: implement one layer at a time (db → service → api)
   After each layer: run pytest tests/ -x
   
   Forbidden: do not modify test files, do not run migrations
   Verification: pytest tests/test_user_api.py -x"

# Phase 3: Test gap analysis (10 min)
codex "Gap analysis on tests for GET /users/{id}/activity.
       What is NOT covered?
       Focus on: auth scenarios (own activity vs others'), empty results, large result sets"
```

---

## Scenario 2 — Explore Before Implementing (20 minutes)

**Situation**: new codebase, need to add a feature but don't know the patterns.

```bash
# Step 1: Orient
codex "I'm new to this codebase and need to add a DELETE /orders/{id} endpoint.
       Before I start:
       1. How are existing DELETE endpoints structured? (find an example)
       2. What auth pattern is used for owner-only operations?
       3. What is the error response format for 404 (not found)?
       4. What do existing tests for DELETE endpoints look like?
       
       Show me examples from the codebase. Do not make changes."

# Step 2: Implement with reference
codex --approval-policy auto-edit \
  "Add DELETE /orders/{id} following the exact pattern from [pattern file Codex found].
   Auth: user must own the order OR be admin.
   404 if order not found. 403 if not owner and not admin. 204 on success.
   Tests: add to tests/test_order_api.py covering owner delete, admin delete, 404, 403.
   Verification: pytest tests/test_order_api.py -x"
```

---

## Scenario 3 — Refactor for Testability (25 minutes)

**Situation**: a function is hard to test because it mixes concerns.

```bash
# Example: process_checkout() does: validate → charge payment → update inventory → send email
# Currently untestable because it does 4 things with no separation

# Step 1: Diagnose the testability problem
codex "Analyze process_checkout() in src/orders/service.py.
       1. What does it do? (list each responsibility)
       2. Why is it hard to test? (what would you have to mock?)
       3. What structural change would make it testable?
       
       Do not implement — diagnose only."

# Step 2: Refactor with safety
codex --approval-policy auto-edit \
  "Refactor process_checkout() to separate concerns:
   Extract into: validate_checkout_inputs(), charge_payment(), update_inventory(), send_confirmation()
   
   Constraint: behavior must not change — same inputs → same outputs
   Constraint: do not change the process_checkout() function signature
   
   Process:
   1. Run baseline: pytest tests/test_order_service.py -x
   2. Extract one function at a time
   3. After each extraction: run pytest tests/test_order_service.py -x
   4. All tests from step 1 must pass at every step
   
   Verification: pytest tests/test_order_service.py -x"
```

---

## Self-Assessment

| Scenario | Phases completed? | Tests passed? | Key insight |
|----------|------------------|--------------|-------------|
| B1: Test-first module | | | |
| B2: Extend safely | | | |
| 1: Full feature | | | |
| 2: Explore first | | | |
| 3: Refactor for testability | | | |

---

## Revision Checklist

- [ ] Test-first: defined tests before implementation in at least one scenario
- [ ] Explored the codebase before implementing (not guessing patterns)
- [ ] Ran gap analysis after test generation
- [ ] Refactoring maintained behavior: same tests passed before and after
- [ ] Full feature build used plan → implement → test → review sequence
