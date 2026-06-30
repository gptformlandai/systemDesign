# Feature Building Scenarios — Gold Sheet

> **Track**: Copilot Mastery Track — Group 5: Scenario Practice
> **File**: 2 of 4 (Track File #29)
> **Audience**: Developers practicing end-to-end feature delivery with Copilot

---

## ⭐ Beginner Tier — Your First Feature with Copilot (Scenarios B1–B2)

> No framework required. Just a Python (or any language) file and VS Code.

---

### Scenario B1 — Add a Function Using Inline Suggestions (15 minutes)

**Setup**: Create `inventory.py` with this starter:

```python
# inventory.py

items = {}

def add_item(name: str, quantity: int, price: float):
    """Adds an item to inventory. Raises ValueError if quantity or price < 0."""
    if quantity < 0 or price < 0:
        raise ValueError("Quantity and price must be non-negative")
    items[name] = {"quantity": quantity, "price": price}
```

**Task**: Use Copilot (inline suggestions + Chat) to add 3 more functions.

**Step 1** — Type the comment and let inline suggest complete:
```python
# Returns total inventory value (sum of quantity * price for all items)
def total_value(
```

**Step 2** — Type:
```python
# Removes an item by name. Raises KeyError if not found.
def remove_item(
```

**Step 3** — Use Chat to generate a search function:
```
"Add a function to inventory.py:

Function: search_items(keyword: str) → list[str]
Behavior: returns names of all items where keyword appears in the item name (case-insensitive)
If no matches: return empty list

Follow the same style as the existing functions in #file:inventory.py"
```

**Step 4** — Use Edits mode to add tests:
```
Working set: inventory.py (or create test_inventory.py)

"Generate pytest tests for all 4 functions in #file:inventory.py.
For each function: 1 happy path test, 1 error case test.
Test total_value with 0 items and with 2 items."
```

**Success criteria**: 4 functions implemented, all tests pass, you can explain each function.

---

### Scenario B2 — Extend an Existing Function Safely (20 minutes)

**Setup**: Use the `calculate_discount` function from Daily Workflow Scenario B1 (or paste it):

```python
def calculate_discount(price, customer_type, quantity):
    base = price * quantity
    if customer_type == "VIP":
        return base * 0.75
    elif customer_type == "MEMBER":
        return base * 0.90 if quantity >= 10 else base * 0.95
    return base
```

**Task**: Extend it safely without breaking existing behavior.

**Step 1 — Understand first**:
```
"Before I change this function:
#selection

What are all the current behaviors? List each customer_type branch
and what discount it applies. Also: what happens for customer_type='GUEST'?
What happens if price=0?"
```

**Step 2 — Plan the extension**:
```
"I want to add a new customer type: 'STAFF' gets 40% discount on any quantity.
Also: if any order is over $1000 base value, add an extra 5% on top of the discount.

Before implementing: show me the new behavior table for all customer types.
Plan only — no code yet."
```

**Step 3 — Implement**:
```
"Implement the changes we planned to #selection.
Do not change the existing VIP and MEMBER logic.
Add 'STAFF' at 40% discount.
Add the $1000 extra 5% discount layer on top.
Keep all existing tests passing."
```

**Step 4 — Add tests for the new cases**:
```
"Generate tests for the new STAFF discount and the $1000 threshold.
Test: STAFF with quantity 1, STAFF with large quantity,
order exactly at $1000 boundary, order over $1000 for each customer type."
```

**Success criteria**: New behavior added, all old tests still pass, new tests verify new behavior.

---

## Intermediate Tier — Scenarios 1–4

> Pre-requisite: completed 01-Foundations and 02-Intermediate-Power-User tracks.

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
