# Before vs After Prompt Examples — Gold Sheet

> **Track**: Claude Mastery Track — Group 2: Intermediate Power User
> **File**: 7 of 7 (Track File #13)
> **Read after**: Claude-For-Documentation-Gold-Sheet.md

---

## How To Use This Sheet

For each example: read the BEFORE prompt, predict what Claude will produce, then read the AFTER prompt.
The AFTER prompt is better because of specific, identifiable changes.
After reading, identify the pattern — you'll recognize it in your own prompts.

---

## Example 1 — Explain This Code

**BEFORE (vague, no format constraint)**:
```
"What does this code do?"
```

**AFTER (structured, output-directed)**:
```
"Explain @file:src/services/order_service.py

Structure your answer as:
1. Purpose: what this service is responsible for (2 sentences)
2. Key functions: list each public function with a one-sentence description
3. Data flow: how data enters, transforms, and exits this service
4. External dependencies: what it calls that it doesn't own
5. Gotchas: anything non-obvious a developer modifying this file should know"
```

**Pattern**: Give Claude a structure to fill in. Vague prompts get essay-style answers. Structured prompts get scannable, reference-quality output.

---

## Example 2 — Debug an Error

**BEFORE (symptom only)**:
```
"My API is returning 500 errors"
```

**AFTER (error + code + context)**:
```
"Diagnose this 500 error.

Error (from logs):
  sqlalchemy.exc.MissingGreenlet: greenlet_spawn has not been called
  Location: order_service.py line 47

Code: @file:src/services/order_service.py (or select the function)
Stack: @file (or paste the trace)

Context:
  - Framework: FastAPI + SQLAlchemy 2.x async
  - This function worked yesterday; broke after I added async to get_db()

Diagnose:
  1. Root cause — explain WHY this error happens
  2. Fix — show the exact code change
  3. Why it broke now — what change triggered it"
```

**Pattern**: Errors need three inputs — the exact message, the relevant code, and what changed. Without all three, Claude diagnoses the wrong thing.

---

## Example 3 — Implement a Feature

**BEFORE (no constraints)**:
```
"Add caching to the user service"
```

**AFTER (scoped with constraints)**:
```
"Add Redis caching to UserService.get_user(user_id: int).

Caching rules:
  - Cache key: 'user:{user_id}'
  - TTL: 300 seconds
  - Cache on READ (get_user only) — NOT on write operations
  - On cache miss: fetch from DB, then write to cache
  - On user update/delete: invalidate the cache key

Constraints:
  - Use the existing redis_client from @file:src/infrastructure/cache.py
  - Do NOT change the get_user() signature
  - Do NOT add caching to create_user or update_user
  - Add tests in @file:tests/unit/test_user_service.py

Files to modify: @file:src/services/user_service.py only"
```

**Pattern**: Features need scope (what to add), constraints (what NOT to do), and file boundaries (which files to touch). Without constraints, Claude over-engineers.

---

## Example 4 — Generate Tests

**BEFORE (no guidance on coverage)**:
```
"Write tests for the payment service"
```

**AFTER (specific coverage requirements)**:
```
"Generate pytest tests for @file:src/services/payment_service.py

Coverage requirements:
  - process_payment(): happy path, card declined, network timeout, invalid amount
  - refund_payment(): success, order not found, already refunded, partial refund
  - Mocks: PaymentGateway (MagicMock), db (AsyncMock)
  - Do NOT test: _calculate_fee() (it's tested in test_fee_calculator.py)

Test naming: test_[function]_[scenario]_[expected_result]
Style: AAA pattern, one concept per test
File: tests/unit/test_payment_service.py"
```

**Pattern**: Test prompts need explicit coverage requirements, a mock strategy, and naming conventions. Otherwise Claude writes happy-path-only tests.

---

## Example 5 — Code Review

**BEFORE (no focus)**:
```
"Review this code"
```

**AFTER (focused review with priorities)**:
```
"Security-focused review of @file:src/api/auth.py

Check for:
  1. SQL injection (parameterized queries vs string interpolation)
  2. User enumeration (different error messages for 'user not found' vs 'wrong password')
  3. Timing attacks (comparison functions that leak timing info)
  4. Missing rate limiting on login endpoint
  5. JWT expiry: is it checked? Can tokens be replayed?

For each issue:
  - SEVERITY: CRITICAL / HIGH / MEDIUM / LOW
  - What the vulnerability is
  - How it could be exploited
  - The specific fix as a code change
  - OWASP category if applicable

Not interested in: style, naming, or performance for this pass."
```

**Pattern**: Code review prompts need a focus dimension (security vs performance vs maintainability) and an output format. "Review this code" gets generic suggestions.

---

## Example 6 — Refactor Code

**BEFORE (direction without constraints)**:
```
"Refactor this function to be cleaner"
```

**AFTER (specific goal + preservation rules)**:
```
"Refactor the process_order function in @file:src/services/order_service.py

Goal: extract the discount calculation logic into a separate function.

Rules:
  - The new function: calculate_discount(price, customer_type, quantity) → float
  - process_order() must call calculate_discount() instead of inline calculation
  - Do NOT change: process_order()'s public signature or return type
  - Do NOT change: any logic or calculation — only extract, don't modify behavior
  - Tests in test_order_service.py must still pass without modification

After refactoring: run pytest tests/unit/test_order_service.py to confirm."
```

**Pattern**: Refactoring prompts need a specific structural goal and an explicit preservation rule ("do not change behavior"). Without it, Claude changes logic, not just structure.

---

## Example 7 — Architecture Planning

**BEFORE (abstract)**:
```
"How should I design a notification system?"
```

**AFTER (constrained to your system)**:
```
"Design a notification system for this codebase.

Current context: @file:src/models/user.py, @file:src/services/order_service.py
Notification triggers: order confirmed, order shipped, payment failed, refund processed
Channels needed: email, in-app notification (stored in DB), optional SMS

Design constraints:
  - Must not slow down the order transaction (async/decoupled)
  - Email sending can fail without failing the order
  - In-app notifications must be consistent (not lossy)
  - Stack: FastAPI + SQLAlchemy + existing Redis

Produce:
  1. Component diagram (text): what components exist and what they do
  2. Data model: what tables/schemas are needed
  3. Event flow: how an order confirmation triggers notifications
  4. Technology recommendation: why (Redis pub/sub vs message queue vs task queue)
  5. What NOT to build in V1 (what to defer)"
```

**Pattern**: Architecture prompts must include your actual tech stack and constraints. Generic design advice doesn't account for what you already have.

---

## Example 8 — Write a Slash Command

**BEFORE (no specification)**:
```
"Write a slash command to review code"
```

**AFTER (target behavior defined)**:
```
"Create a Claude slash command file: .claude/commands/review.cmd.md

Behavior: when I run /review, Claude should:
  1. Read @file:[selected or open file]
  2. Run these 4 checks in order:
     a. Security: OWASP top 10, no hardcoded creds, no SQL string interpolation
     b. Tests: what error paths are untested?
     c. Error handling: are all exceptions caught and handled?
     d. API breaking changes: does this change break any existing callers?
  3. Format: severity table (CRITICAL/HIGH/MEDIUM/LOW) for each check
  4. End with: 'Approve' / 'Approve with comments' / 'Request changes'

Do not include suggestions about naming or formatting — only functional issues."
```

**Pattern**: Slash commands need their output behavior described precisely. "Review code" is not a specification — a specification is what checks to run and how to format the output.

---

## Example 9 — Onboard to an Unfamiliar Codebase

**BEFORE (passive)**:
```
"Explain this codebase to me"
```

**AFTER (active learning structure)**:
```
"Onboard me to @codebase in 4 steps:

Step 1 — Overview (answer now):
  What does this system do? (2 sentences)
  What pattern is it built on? (layered / hexagonal / MVC / etc.)
  What are the 5 most important files I should read first?

Step 2 — Request lifecycle:
  Trace ONE typical request from API entry to database to response.
  Show the class/function chain. Stop if you need me to clarify the entry point.

Step 3 — Dangerous zone:
  Which 2-3 files are most likely to break the system if changed carelessly?
  Why?

Step 4 — First task:
  Given all of the above, what is the smallest meaningful change I could make
  to learn the codebase hands-on without risking breaking anything?"
```

**Pattern**: Onboarding prompts should progress from overview to depth. Asking for "everything" at once produces an information dump. Sequential questions produce a mental model.

---

## Example 10 — Debug a Test Failure

**BEFORE (just the error)**:
```
"Why is this test failing?"
```

**AFTER (failure + code + expected behavior)**:
```
"Diagnose this test failure:

Failure output:
  FAILED tests/unit/test_order_service.py::test_create_order_returns_confirmed_status
  AssertionError: assert 'pending' == 'confirmed'

Test: @file:tests/unit/test_order_service.py (select the specific test)
Service: @file:src/services/order_service.py

What the test expects: create_order should return status='confirmed' for a standard order.
What's happening: it returns status='pending'.

Diagnose:
  1. Is the test correct or is the behavior spec wrong?
  2. Where in the code is the status set to 'pending' when it should be 'confirmed'?
  3. What's the fix — in the implementation (not the test)?"
```

**Pattern**: Test failures need the full failure output, both the test and the implementation, and a statement of what the correct behavior should be.

---

## The Patterns — Summary

| Pattern | BEFORE | AFTER |
|---------|--------|-------|
| Structure | "What does this do?" | Give Claude headings to fill in |
| Debugging | "It's broken" | Error message + code + what changed |
| Implementation | "Add X" | Scope + constraints + file boundaries |
| Testing | "Write tests" | Coverage requirements + mocking strategy |
| Review | "Review this" | Focus dimension + output format |
| Refactoring | "Clean this up" | Structural goal + preservation rule |
| Architecture | "How should I design X?" | Your stack + constraints + decision needed |
| Commands | "Write a command for Y" | Describe the exact behavior and output format |

---

## Revision Checklist

- [ ] Can identify what makes each BEFORE prompt weak
- [ ] Can apply the structure-filling pattern to any explanation request
- [ ] Always includes all 3 debugging inputs: error + code + context
- [ ] Always includes constraints (what NOT to change) in implementation prompts
- [ ] Uses focus dimensions for code review (security / performance / maintainability)
- [ ] Can write a complete slash command specification
