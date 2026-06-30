# Codex For Beginners — Quick Wins Gold Sheet

> **Track**: Codex Mastery Track — Group 1: Foundations
> **File**: 6 of 6 (Track File #6)
> **Audience**: Developers who want high-ROI Codex tasks for their first week
> **Read after**: Safe-Usage-Principles-Gold-Sheet.md

---

## Practical Impact Meter

| Quick Win | ROI | Time to First Result |
|---|---|---|
| Explain unfamiliar code | ★★★★★ | 30 seconds |
| Generate tests for existing functions | ★★★★★ | 2 minutes |
| Debug a failing test with error message | ★★★★★ | 1 minute |
| Add input validation | ★★★★☆ | 3 minutes |
| Write docstrings for a file | ★★★★☆ | 2 minutes |
| Generate a README from codebase | ★★★★☆ | 3 minutes |
| Review code for security issues | ★★★★☆ | 2 minutes |
| Scaffold a new endpoint from existing pattern | ★★★★☆ | 5 minutes |
| Add error handling to a service | ★★★☆☆ | 3 minutes |
| Fix a linting error | ★★★☆☆ | 30 seconds |

---

## Quick Win #1 — Explain Unfamiliar Code (30 seconds)

Use this every time you open an unfamiliar file. Zero risk.

```bash
codex "Explain what user_service.py does:
       1. Main purpose
       2. Key functions and what each does
       3. External dependencies it calls
       4. 3 things that could break
       Make no changes."
```

Beginner tip: Use this before every Codex task. Explain before touching.
If Codex's explanation seems wrong — it's a signal that the file needs better comments or
that Codex needs more context. Fix the understanding before writing code.

---

## Quick Win #2 — Generate Tests (2 minutes)

```bash
codex --approval-policy auto-edit \
  "Generate tests for the create_order() function in src/orders/service.py.
   Cover: happy path, invalid amount (zero and negative), missing required fields,
   database connection failure.
   Use pytest. Mock database calls.
   Verification: pytest tests/test_order_service.py -v"
```

**What to review after**: Read each generated test. Are they asserting the right things?
A test that calls the function and asserts it didn't raise an exception is almost always useless.
Good tests assert specific return values or specific exception types.

---

## Quick Win #3 — Debug a Failing Test (1 minute)

```bash
# Copy the exact error message from your terminal
codex "The test tests/test_auth.py::test_login_valid_credentials is failing.
       Error: AssertionError: 401 != 200
       The login() function in src/auth/service.py is returning 401 for valid credentials.
       Find the root cause. Fix only the implementation (not the test).
       Verification: pytest tests/test_auth.py::test_login_valid_credentials -v"
```

**Key constraint**: "Fix only the implementation (not the test)."
Without this, Codex may change the expected status code in the test to match wrong behavior.

---

## Quick Win #4 — Add Input Validation (3 minutes)

```bash
codex --approval-policy auto-edit \
  "Add input validation to create_user() in src/users/service.py.
   Validate:
   - email: must be a valid email format (use regex or email-validator library)
   - age: must be integer between 0 and 150
   - name: must be 1-100 characters, not empty
   Raise ValueError with descriptive message for each invalid case.
   Verification: pytest tests/test_user_service.py -x"
```

---

## Quick Win #5 — Write Docstrings (2 minutes)

```bash
codex --approval-policy auto-edit \
  "Write Google-style docstrings for every public function in src/orders/service.py.
   Each docstring must include: description, Args section, Returns section, Raises section.
   Do not change any function logic.
   Verification: no test failures (run: pytest tests/test_order_service.py -x)"
```

---

## Quick Win #6 — Generate a README (3 minutes)

```bash
codex "Generate a README.md for this project.
       Sections: Project overview, Tech stack, Setup, Running locally, Running tests,
       API endpoints (list them), Environment variables required, Contributing.
       Do not invent information — only use what is verifiably in the codebase.
       Output the README content — do not create the file yet."
```

Review the output, then:
```bash
# If it looks correct:
codex --approval-policy auto-edit "Create README.md with this exact content: [paste the reviewed output]"
```

---

## Quick Win #7 — Security Review (2 minutes)

```bash
codex "Review src/auth/login.py for security vulnerabilities.
       Check specifically:
       - SQL injection (is the query parameterized?)
       - Password handling (is bcrypt used? is the password logged anywhere?)
       - Brute force protection (is there rate limiting?)
       - Error messages (do they reveal too much info to attackers?)
       Format: | SEVERITY | ISSUE | LINE | FIX |
       Do not make changes — review only."
```

---

## Quick Win #8 — Scaffold a New Endpoint (5 minutes)

```bash
codex --approval-policy auto-edit \
  "Add a GET /products/{id} endpoint to src/api/products.py.
   Follow the exact pattern used by GET /users/{id} in src/api/users.py:
   same auth check, same error handling (404 for not found, 401 for unauthorized),
   same response model format.
   Create the endpoint and corresponding repository method.
   Verification: pytest tests/test_product_api.py -x"
```

The key: "Follow the exact pattern used by [existing endpoint]."
This produces idiomatic code for your specific codebase every time.

---

## Quick Win #9 — Add Error Handling (3 minutes)

```bash
codex --approval-policy auto-edit \
  "The send_notification() function in src/notifications/service.py has no error handling.
   Add error handling:
   - Catch ConnectionError → log warning, return False (don't crash the caller)
   - Catch Timeout → log warning, return False
   - Catch any unexpected exception → log error with traceback, re-raise
   Use structlog for logging.
   Constraint: do not change the function signature.
   Verification: pytest tests/test_notification_service.py -x"
```

---

## Quick Win #10 — Fix a Linting Error (30 seconds)

```bash
codex --approval-policy auto-edit \
  "Fix all linting errors in src/users/service.py.
   Use: ruff check src/users/service.py
   Fix each error. Do not change logic — only formatting and style.
   Verification: ruff check src/users/service.py (must report 0 errors)"
```

---

## The First-Week Habit Loop

```
Monday: Quick Win #1 on each file you'll touch this week (explain before touching)
Tuesday: Quick Win #2 on the function you implemented Monday (generate tests)
Wednesday: Quick Win #7 on any auth or SQL code from this week (security review)
Thursday: Quick Win #5 on any undocumented files you worked on (docstrings)
Friday: Quick Win #6 if README is outdated; Quick Win #10 if lint errors accumulated

By end of week 1:
  ✅ You have experience with safe Codex usage (suggest + auto-edit)
  ✅ Your new code has tests and docstrings
  ✅ You've done a security review on sensitive code
  ✅ Your project README is current
```

---

## When Quick Wins Are Not Enough

```
Quick wins are isolated, targeted, and reversible.

You need full agentic workflows (later in this track) when:
  - The task spans 5+ files
  - You need to iterate: implement → test → fix → test again
  - You're doing a codebase-wide refactor
  - You're scaffolding an entire new module

For now: start with quick wins. Build the habit of:
  1. Read the plan before approving
  2. Review the diff after applying
  3. Run the verification command
  4. If something looks wrong: reject it and narrow the scope
```

---

## Interview Traps

```
TRAP: "Quick wins are for beginners — I should jump straight to full-auto"
TRUTH: Quick wins build the mental model that makes full-auto effective. Developers who
       skip this foundation make larger, harder-to-debug mistakes in full-auto sessions.
       The 10 quick wins are also the 10 tasks you'll use Codex for most frequently.

TRAP: "If the verification command passes, the quick win is complete"
TRUTH: The verify step tells you tests pass — it doesn't tell you the code does what
       your product needs. Always read the diff. Quick wins are also about learning
       what Codex does and doesn't understand about your codebase.

TRAP: "Use auto-edit for all quick wins to save time"
TRUTH: Start in suggest mode for quick wins. Reading the proposed changes before they're
       applied teaches you Codex's behavior patterns. This knowledge makes every future
       session faster and catches mis-directed output early.
```

---

## Revision Checklist

- [ ] Run Quick Win #1 on at least one unfamiliar file today
- [ ] Run Quick Win #2 on at least one function with no tests
- [ ] Run Quick Win #7 on at least one auth or database function
- [ ] Can run all 10 quick wins without looking at this sheet
- [ ] Every quick win included a verification command
- [ ] Reviewed the diff after every auto-edit task
