# Feature Building Scenarios — Gold Sheet

> **Track**: Copilot Mastery Track — Group 5: Scenario Practice
> **File**: 2 of 4 (Track File #29)
> **Audience**: Developers practicing end-to-end feature delivery with Copilot

---

## Scenario 1 — Feature from Scratch (Language-Agnostic)

**Task**: Build a "user notification preferences" feature.
Users can configure which notification types they receive (email, in-app, SMS).

**Time limit**: 45 minutes

**Phase 1 — Plan (5 min)**
```
"Plan the implementation for a notification preferences feature.
Using #codebase to understand our existing patterns:

1. What files will I create and modify?
2. What data model is needed?
3. What API endpoints?
4. What business rules? (e.g., can a user disable all notifications?)
5. What tests are needed?

Plan only — no code yet."
```

**Phase 2 — Schema and model (10 min)**
```
"Create the data model for notification preferences.
Follow the same schema/model pattern as #file:[existing model file].
Types of notifications: email, in_app, sms. Each enabled/disabled.
Do not create any API endpoints yet — model and schema only."
```

**Phase 3 — Business logic (15 min)**
```
"Implement the notification preferences service.
[Reference the model from Phase 2]
Operations: get_preferences(user_id), update_preferences(user_id, preferences).
Business rule: at least one channel must remain enabled.
Follow the service pattern in #file:[existing service file]."
```

**Phase 4 — API and tests (15 min)**
```
"Add API endpoints for notification preferences:
GET /users/{user_id}/notification-preferences
PATCH /users/{user_id}/notification-preferences

Generate tests covering:
- Fetch preferences for existing user
- Update with valid preferences
- Update that disables all channels → validation error
- Update for non-existent user → 404"
```

**Success criteria**: 
- Full feature implemented across model/schema/service/API/tests
- All tests pass
- Pattern consistent with existing codebase

---

## Scenario 2 — Codebase Exploration Before Implementing

**Setup**: You're new to a codebase and need to add authentication logging.

**Phase 1 — Understand before touching**
```
"Using #codebase, help me understand the authentication flow:
1. Which file handles login requests?
2. Where are auth tokens generated?
3. Where are auth failures handled?
4. Is there existing logging infrastructure?
5. What would be affected if I add a log entry on auth failure?"
```

**Phase 2 — Plan with full context**
```
"Based on what you found: plan adding auth failure logging.
For each failed login attempt, log: timestamp, IP address (anonymized), failure reason.
Do NOT log: username, password, or user email.

Where exactly should the log statement go?
What does the log entry look like?
Show me the plan — no implementation yet."
```

**Phase 3 — Implement**
```
"Implement auth failure logging per the plan.
Test: add a test that verifies a log entry is created on failed login.
The test should check: log level, presence of IP, absence of password."
```

**Success criteria**: Auth logging added without touching untested code paths; privacy-preserving (no PII logged).

---

## Scenario 3 — Extending an Existing Feature Safely

**Setup**: An existing `OrderService.create_order` method needs to send a confirmation email after successful order creation. The email service already exists.

```
"I need to add email confirmation to order creation.

Existing code: #file:src/services/order_service.py (or equivalent)
Email service: #file:src/services/email_service.py (or equivalent)

Requirements:
- Email sends AFTER the order is committed (not during the transaction)
- If email sending fails: order is NOT rolled back (email failure ≠ order failure)
- If email fails: log the error with the order ID for retry
- Unit tests: email is called with correct args, email failure doesn't fail the order

Modify #file:[order service] to add email sending.
Use test: mock email service, verify call and failure isolation."
```

**Success criteria**: Email integrated, transaction safety preserved, email failure isolated.

---

## Scenario 4 — Refactoring for Testability Before Adding Tests

**Setup**: A legacy function mixes business logic, database access, and external calls in one block. You need tests before adding a new feature.

```
"This function in #selection is untestable as-is:
It mixes business logic, database access, and an HTTP call in one block.

Step 1: Refactor ONLY for testability — do not change behavior.
Plan first:
- What should be extracted into separate functions/classes?
- What dependencies should be injected instead of created internally?
- After refactoring, what will be easy to mock in tests?
Show the plan. No code yet."

[After plan review:]
"Implement the refactoring per the plan.
Run any existing tests to confirm behavior is unchanged."

[After refactoring:]
"Now generate unit tests for the refactored code.
Mock: database session, HTTP client.
Cover: happy path, database error, HTTP call failure, edge cases."
```

**Success criteria**: Behavior unchanged (any existing tests pass), new tests cover all paths.
