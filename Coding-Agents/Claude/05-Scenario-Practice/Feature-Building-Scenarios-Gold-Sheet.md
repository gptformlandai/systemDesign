# Feature Building Scenarios — Gold Sheet

> **Track**: Claude Mastery Track — Group 5: Scenario Practice
> **File**: 2 of 4 (Track File #27)
> **Audience**: Developers practicing end-to-end feature delivery with Claude

---

## ⭐ Beginner Tier — First Feature with Claude (B1–B2)

> No framework required. Just Python and the habit of planning before building.

---

### Scenario B1 — Build a Simple Module with Claude (20 min)

**Setup**: Create a blank file `todo.py`.

**Task**: Build a small to-do list module using Claude for each step.

**Phase 1 — Plan first (5 min)**:
```
"I want to build a simple to-do list in Python.
Operations needed:
  - add_task(title: str) → adds a task
  - complete_task(task_id: int) → marks a task as done
  - list_tasks(show_completed=False) → returns list of tasks

Before any code:
1. What data structure should I use to store tasks?
2. What fields does each task need?
3. What should list_tasks return when no tasks exist?
4. What should complete_task do if the task_id doesn't exist?"
```

**Phase 2 — Tests first (5 min)**:
```
"Based on the plan:
Generate pytest tests for todo.py BEFORE I write any implementation.
Cover: add_task normal, list_tasks with/without completed, complete_task success,
complete_task with invalid id.
The file todo.py does not exist yet — tests will fail on import."
```

**Phase 3 — Implement (10 min)**:
```
"Now implement todo.py to make these tests pass:
@file:tests/test_todo.py

Process:
1. Implement add_task
2. Run: pytest tests/test_todo.py -x
3. Fix any failures
4. Add complete_task, run tests again
5. Add list_tasks, run tests again
Done when all tests pass."
```

**Success**: Working module, all tests pass, you understand every line.

---

### Scenario B2 — Extend Existing Code Safely (20 min)

**Setup**: Use the todo.py from B1.

**Task**: Add a priority field without breaking existing behavior.

**Step 1 — Plan the extension**:
```
"I want to add a 'priority' field to each task in todo.py.
Priority values: 'low', 'medium', 'high'. Default: 'medium'.

Before implementing:
1. Which existing tests will break with this change?
2. Does add_task's signature need to change?
3. How should list_tasks sort by priority?
4. What new tests do I need?
Show the plan — no code yet."
```

**Step 2 — Write new tests first**:
```
"Generate tests for the priority feature:
- add_task with explicit priority
- add_task with default priority (no priority arg)
- list_tasks sorted by priority (high first)
- Invalid priority value should raise ValueError"
```

**Step 3 — Implement**:
```
"Implement the priority feature in @file:todo.py to pass all tests including the new ones.
Original tests must still pass — do NOT change them."
```

---

## Intermediate Tier — Scenarios 1–4

> Pre-requisite: 01-Foundations and 02-Intermediate complete.

---

## Scenario 1 — Feature from Scratch (45 min)

**Task**: Build a "user notification preferences" feature.

**Phase 1 — Plan (5 min)**:
```
/plan

"Plan the notification preferences feature.
Using @codebase to understand existing patterns:
1. Files to create and modify
2. Data model (what fields, what table/schema)
3. API endpoints
4. Business rules (e.g., can user disable ALL channels?)
5. Test plan

Plan only — no code."
```

**Phase 2 — Schema/model (10 min)**:
```
"Create the data model for notification preferences.
Follow the same schema pattern as @file:[existing model].
Channels: email, in_app, sms — each enabled/disabled.
No API endpoints yet — schema only."
```

**Phase 3 — Business logic (15 min)**:
```
"Implement the notification preferences service.
Operations: get_preferences(user_id), update_preferences(user_id, preferences).
Business rule: at least one channel must remain enabled.
Follow the pattern in @file:[existing service]."
```

**Phase 4 — API + tests (15 min)**:
```
"Add API endpoints for notification preferences.
GET /users/{id}/notification-preferences
PATCH /users/{id}/notification-preferences

Tests:
- Fetch for existing user
- Update with valid preferences
- Update that disables all → validation error
- Update for non-existent user → 404"
```

---

## Scenario 2 — Explore Before Implement (25 min)

**Setup**: Adding authentication failure logging to an unfamiliar codebase.

**Phase 1 — Understand (10 min)**:
```
"Using @codebase:
1. Which file handles login requests?
2. Where are auth tokens generated?
3. Where are auth failures currently handled?
4. Is there existing logging infrastructure?
5. What would be affected if I log on auth failure?"
```

**Phase 2 — Plan with context (5 min)**:
```
"Plan adding auth failure logging.
Log on failed login: timestamp, anonymized IP, failure reason.
Do NOT log: username, password, or email.
Where does the log statement go? What does it look like?
Plan only."
```

**Phase 3 — Implement (10 min)**:
```
"Implement auth failure logging per the plan.
Test: verify a log entry is created on failed login.
Check: log contains IP, does NOT contain password."
```

---

## Scenario 3 — Extend Existing Feature Safely (20 min)

**Setup**: `OrderService.create_order` needs to send a confirmation email after commit.

```
"Add email confirmation to order creation.

Existing: @file:src/services/order_service.py
Email service: @file:src/services/email_service.py

Requirements:
  - Email sends AFTER the order commits (not during transaction)
  - Email failure does NOT roll back the order
  - Email failure must be logged with the order ID for retry
  - Test: email called with correct args; email failure doesn't fail the order

Modify order_service only. Mock email_service in tests."
```

---

## Scenario 4 — Refactor for Testability (30 min)

**Setup**: Legacy function mixes DB access, HTTP calls, and business logic.

```
Step 1 — Plan refactoring:
"This function in @selection is untestable as-is.
Plan refactoring for testability ONLY — do not change behavior.
What to extract? What to inject?
Show the plan — no code yet."

Step 2 — Implement refactoring:
"Implement the refactoring per the plan.
Run existing tests after each change to confirm behavior is unchanged."

Step 3 — Generate tests:
"Now generate unit tests for the refactored code.
Mock: DB session, HTTP client.
Cover: happy path, DB error, HTTP failure, edge cases."
```

---

## Revision Checklist

- [ ] Always plans before implementing (plan-first discipline)
- [ ] Writes tests before implementation for new features
- [ ] Explores the codebase before touching unfamiliar code
- [ ] Extends existing features with explicit preservation rules
- [ ] Can refactor for testability without changing behavior
