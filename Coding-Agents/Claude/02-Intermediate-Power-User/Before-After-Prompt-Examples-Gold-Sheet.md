# Before-After Prompt Examples — Gold Sheet

> **Track**: Claude Mastery Track — Group 2: Intermediate Power User
> **File**: 7 of 7 (Track File #13)
> **Read after**: Claude-Code-CLI-Gold-Sheet.md

---

## How to Read These Examples

Each example shows:
- **BEFORE**: how 90% of developers write the prompt
- **Why it fails**: the specific problem
- **AFTER**: the production-grade version
- **What changed**: the exact improvements applied

---

## 1. Debugging

```
BEFORE: "My payment function isn't working. Can you help debug it?"
WHY: No error, no code, no symptom. Claude asks 5 clarifying questions.

AFTER:
"Error at line 87 of payment_service.py, process_refund():
  AttributeError: 'NoneType' object has no attribute 'stripe_charge_id'
  Call stack: order = await self.order_repo.get_order_for_refund(order_id) → None
Code: @file:src/services/payment_service.py — process_refund() function
Already ruled out: order_id existence (verified in DB)

Root cause (1 sentence). Fix as unified diff. Under 100 words."

WHAT CHANGED: exact error + file + function + what was ruled out + output format + length limit
```

---

## 2. Code Generation

```
BEFORE: "Write a function to validate user input"
WHY: "validate" and "user input" are undefined. Claude writes a generic string validator.

AFTER:
"Write validate_notification_preferences(prefs: dict) -> NotificationPrefs.

Requirements:
- Input: dict with keys email, in_app, sms (all bool, all optional)
- Missing keys default to True
- At least one must be True; raise ValidationError if all False
- Return: NotificationPrefs dataclass
- Raise: ValidationError with 'At least one channel must be enabled'

Pattern: follow the validation style in @file:src/schemas/user.py
Type hints required. Under 20 lines. No class needed — standalone function."

WHAT CHANGED: exact function signature, return type, validation rules, error type, pattern reference, line limit
```

---

## 3. Refactoring

```
BEFORE: "Refactor this code to be cleaner"
WHY: "cleaner" is undefined. Claude adds 3 base classes and a factory pattern.

AFTER:
"Refactor @file:src/services/order_service.py — process_order() method only.

Goal: extract the email notification logic into a separate _send_order_notification() helper.
Keep: process_order() public signature identical
Keep: all existing tests passing (run pytest tests/unit/test_order_service.py before starting)
Do NOT: create any new classes, modify tests, add new imports beyond smtplib
Output: unified diff only — no prose"

WHAT CHANGED: specific method + specific goal + explicit keeps + explicit forbidden + test requirement
```

---

## 4. Code Review

```
BEFORE: "Review this PR"
WHY: "review" without criteria gives random feedback that varies every run.

AFTER:
"Security review of @file:src/api/auth.py.

Check exactly these categories:
- SQL injection (any string-formatted queries)
- Hardcoded credentials (API keys, secrets in code)
- Missing auth checks (endpoints callable without authentication)
- PII in logs (email, names, payment data in log statements)

Format (one line per finding, no prose):
[CRITICAL|HIGH|MEDIUM|LOW] line:N — what the issue is — specific fix

CRITICAL first. Omit LOW-severity findings. Under 200 words total."

WHAT CHANGED: specific categories + output format defined + severity ordered + word limit
```

---

## 5. Architecture Question

```
BEFORE: "Is this architecture good?"
WHY: "good" is undefined. Claude says "yes, it looks good" or gives generic SOLID advice.

AFTER:
"You are a senior staff engineer.
Architecture review of @file:src/services/order_service.py.

Evaluate ONLY:
1. Does process_order() have more than one responsibility? (SRP)
2. Can OrderService be unit-tested without a real database? (testability)
3. If I need to add a new payment method, which files change? (OCP)

For each: cite the specific line, state the consequence, show the concrete fix.
Format: 3-row table. Under 250 words."

WHAT CHANGED: role + specific questions + evidence required (cite line) + consequence + fix + table format
```

---

## 6. Test Generation

```
BEFORE: "Write tests for this function"
WHY: No framework, no coverage requirements, no mocking guidance. 2 happy-path tests.

AFTER:
"Generate pytest tests for process_refund() in @file:src/services/payment_service.py.

Cover:
1. Successful refund: amount < order.total → RefundResult with status='approved'
2. Overage refund: amount > order.total → InsufficientFundsError
3. Zero amount: → ValidationError('amount must be > 0')
4. Order not found: order_id doesn't exist → OrderNotFoundError
5. Stripe failure: Stripe client raises StripeError → re-raises StripeError

Framework: pytest + pytest-asyncio (@pytest.mark.asyncio)
Mock: AsyncMock(spec=AsyncSession) for DB, AsyncMock(spec=StripeClient) for Stripe
Names: test_process_refund_<scenario>_<expected>
Output: complete test file with all imports, run immediately after generation"

WHAT CHANGED: 5 specific scenarios named + exact exceptions + framework + mocks + naming + run instruction
```

---

## 7. Agent Mode Task

```
BEFORE: "Build a user notification preferences system"
WHY: Scope undefined. Claude builds 15 files with abstractions you didn't ask for.

AFTER:
"Use @planner agent.

Feature: notification preferences (email, in_app, sms toggles per user)
Constraints from CLAUDE.md: layered architecture, async SQLAlchemy, Pydantic v2
Existing pattern: @file:src/api/users.py (router), @file:src/services/user_service.py (service)

Plan output:
1. Files to create (exact paths + one-sentence purpose)
2. Build order (dependency sequence)
3. Assumptions you're making
4. Which agent handles each phase (planner → builder → tester → reviewer)

Wait for my approval before any implementation.
Hard limit: 5 files maximum — request more only if functionally required."

WHAT CHANGED: specific agent + existing pattern reference + explicit constraints + file limit + approval gate
```

---

## 8. Codebase Understanding

```
BEFORE: "How does this codebase work?"
WHY: Claude reads 40 files, gives a 2000-word essay with low signal.

AFTER:
"Using @file:src/ give me a 5-bullet architecture overview:
1. What this system does (1 sentence)
2. Request flow: entry → service → repository → DB (one step per →)
3. Key domain models (3 max, with the table/collection each maps to)
4. How auth works (1 sentence)
5. Most important file I should read first

Under 150 words. Cite specific file names for each bullet."

WHAT CHANGED: structured format with exact bullet count + file citation requirement + word limit
```

---

## 9. Learning Note Generation

```
BEFORE: "Explain async/await to me"
WHY: 2000-word essay starting with the history of async programming.

AFTER:
"Create revision notes on Python async/await for a developer who knows threading.

Format exactly (use these headers, in this order):
## What It Is (2 sentences — no history)
## How It Differs from threading (3 bullet points)
## When to use it vs threading (decision rule in 1 sentence)
## Code Example (show one bad pattern → one correct pattern, under 20 lines total)
## 3 Common Mistakes (bad code → good code for each)
## 5 Revision Questions (applied, not definitional)

Under 500 words. Code examples must run."

WHAT CHANGED: exact headers in order + format constraints + example format + runnable code requirement
```

---

## 10. Performance Problem

```
BEFORE: "Why is this slow?"
WHY: No data, no measurement, no scope. Claude guesses with generic advice.

AFTER:
"Performance analysis of @file:src/repositories/order_repo.py — get_orders_for_user()

Evidence:
- p95 latency: 8 seconds for users with >100 orders
- DB server CPU: 5% during the slow requests (not DB-bound)
- py-spy shows: 80% time in ORM iteration at line 67

Analyze:
1. Is there an N+1 query pattern? (trace the ORM relationships)
2. Are we loading more columns than needed?
3. Are sequential awaits parallelizable?

Format: | Issue | Location | Impact | Fix | Expected Improvement |
Show code for the highest-impact fix only. Verify with a timing measurement."

WHAT CHANGED: evidence provided + specific lines + specific analysis questions + format + fix requirement
```

---

## Revision Checklist

- [ ] Never writes "is this good?" — always specifies evaluation criteria
- [ ] Never writes "write tests" — always specifies framework, scenarios, mocks
- [ ] Never writes "fix this" — always specifies error + file + function
- [ ] Never writes "review this" — always specifies categories and output format
- [ ] Every prompt has an output format (diff / table / bulleted list / under N words)
- [ ] Every prompt has at least one "Do NOT" constraint
- [ ] Complex tasks use file references (@file:) not descriptions
