# Before/After Prompt Examples — Gold Sheet

> **Track**: Codex Mastery Track — Group 2: Intermediate Power User
> **File**: 7 of 7 (Track File #13)
> **Audience**: Developers who want to see the difference between weak and strong prompts
> **Read after**: Codex-For-Documentation-Gold-Sheet.md

---

## How to Use This Sheet

For each example:
1. Read the BEFORE prompt and predict what output you'd get
2. Read the AFTER prompt and note the specific improvements made
3. Identify the pattern — each example illustrates one key principle
4. Apply that principle to your own prompts today

---

## Example 1 — Explaining Unfamiliar Code

```bash
# BEFORE
codex "explain this code"

# Problems: no file reference, no scope, no output format
# Output: generic paragraph about what code usually does
```

```bash
# AFTER
codex "Explain user_service.py:
       1. Main purpose (1-2 sentences)
       2. Each public function: what it does, inputs, outputs, error cases
       3. External dependencies it relies on
       4. 3 things that could break in production
       Make no changes."

# Pattern: structure the output format explicitly
# Result: scannable numbered output you can reference back to
```

---

## Example 2 — Debugging a Failing Test

```bash
# BEFORE
codex "fix my failing test"

# Problems: no test name, no error, no constraint about what to fix
# Output: Codex guesses what test, what failure, may modify tests
```

```bash
# AFTER
codex "The test tests/test_auth.py::test_login_wrong_password is failing.
       Error: AssertionError: Expected 401, got 200
       Stack: login() in src/auth/service.py returns 200 even for wrong passwords.
       Root cause is in src/auth/service.py — not in the test.
       Fix: compare hashed passwords correctly using bcrypt.checkpw().
       Constraint: do not modify tests/test_auth.py.
       Verification: pytest tests/test_auth.py::test_login_wrong_password -v"

# Pattern: exact test name + exact error + where to fix + what not to touch + verification
# Result: surgical fix that doesn't touch tests
```

---

## Example 3 — Implementing a Feature

```bash
# BEFORE
codex "add user authentication"

# Problems: no scope (which files?), no tech (JWT? sessions?), no verification
# Output: entire auth system from scratch, may conflict with existing patterns
```

```bash
# AFTER
codex "In src/auth/, add JWT-based token validation for the existing login endpoint.
       Existing: POST /auth/login already exists in src/api/auth.py, returns user data.
       Add: on successful login, also return a signed JWT (HS256, exp: 24h).
       Add: GET /auth/me endpoint that verifies the JWT and returns user profile.
       Use: PyJWT (already in requirements.txt).
       Follow: error handling pattern in src/api/users.py (HTTPException 401 for invalid token).
       Verification: pytest tests/test_auth_api.py -x"

# Pattern: describe what already EXISTS + what to ADD + what to follow
# Result: code that integrates cleanly with the existing codebase
```

---

## Example 4 — Generating Tests

```bash
# BEFORE
codex "write tests for order_service.py"

# Problems: whole file is too broad, no test framework specified, no mocking guidance
# Output: tests that cover happy path only, may use wrong test framework
```

```bash
# AFTER
codex --approval-policy auto-edit \
  "Generate tests for create_order() in src/orders/service.py only.
   Test cases required:
   - Happy path: valid order with 2 items, returns Order with status='pending'
   - Empty items list: should raise ValidationError
   - Invalid quantity (zero): should raise ValidationError
   - Invalid currency (not USD/EUR/GBP): should raise ValidationError
   - Database failure: mock raises DatabaseError, function raises ServiceError
   Use: pytest + unittest.mock
   Mock: database session only (not own service functions)
   Naming: test_create_order_[scenario]_[expected]
   Run: pytest tests/test_order_service.py -v"

# Pattern: list test cases explicitly + specify mocking scope + naming convention
# Result: meaningful tests with correct isolation
```

---

## Example 5 — Code Review

```bash
# BEFORE
codex "review my code"

# Problems: which code? what to check? what format?
# Output: vague "looks good" or generic paragraph
```

```bash
# AFTER
codex "Review the create_payment() function in src/payments/service.py.
       Check specifically:
       1. SQL injection: is the query parameterized?
       2. Auth: does this verify the user owns the order being paid?
       3. Input validation: what inputs are accepted without validation?
       4. Error handling: what exceptions can leak internal details to the API caller?
       5. Test coverage: what paths in this function have no tests?
       
       Output format:
       | SEVERITY | CHECK | FINDING | LINE | FIX |
       
       Severity: CRITICAL / HIGH / MEDIUM / LOW
       Order by severity descending.
       End with: APPROVED / APPROVE WITH COMMENTS / CHANGES REQUIRED"

# Pattern: specify exactly what to check + exact output format + severity scale
# Result: actionable review you can work from directly
```

---

## Example 6 — Refactoring

```bash
# BEFORE
codex "refactor the database code"

# Problems: which database code? what kind of refactoring? will behavior change?
# Output: large-scale restructuring that may change behavior and break tests
```

```bash
# AFTER
codex --approval-policy auto-edit \
  "Refactor get_user_orders() in src/users/repository.py.
   Problem: N+1 query — it calls SELECT * FROM orders WHERE user_id=? in a loop.
   Fix: replace with a single JOIN query.
   Reference: follow the multi-result query pattern in get_products_by_category()
              in src/products/repository.py.
   Constraints:
   - Same function signature
   - Same return type (list of Order objects)
   - No behavior change — existing tests must still pass
   Verification: pytest tests/test_user_repository.py -x"

# Pattern: describe the specific problem + exact fix type + reference pattern + constraints
# Result: targeted refactor with no behavior change, immediately verifiable
```

---

## Example 7 — Architecture Planning

```bash
# BEFORE
codex "help me design the notification system"

# Problems: no context about what exists, no constraints, no output format
# Output: generic notification system that ignores your actual architecture
```

```bash
# AFTER
codex --model gpt-4.1 \
  "Design a notification system for this project.
   Current state:
   - We have src/users/, src/orders/, src/payments/ as the main services
   - We use FastAPI + PostgreSQL + SQLAlchemy
   - We do NOT have any async messaging (no Kafka, no RabbitMQ currently)
   
   Constraints:
   - Must NOT require infrastructure changes in the first version
   - Must support: email and push notifications
   - Must NOT block the API response when sending notifications
   
   Output:
   1. Architecture diagram (text-based)
   2. New files to create and their interfaces
   3. Changes to existing files
   4. Migration plan: how to go from zero to this design in 3 steps
   
   Do NOT implement — plan only."

# Pattern: describe current state + hard constraints + detailed output structure
# Result: a realistic plan that fits your actual system
```

---

## Example 8 — Creating a Reusable Prompt Script

```bash
# BEFORE
codex "create a bash script for running code review"

# Problems: what kind of review? what files? what output?
# Output: generic script that doesn't match your actual workflow
```

```bash
# AFTER
codex "Create a shell script scripts/codex-precommit.sh that:
       1. Gets the list of staged Python files: git diff --staged --name-only | grep .py
       2. For each file, runs a Codex security review:
          codex --quiet --approval-policy suggest
            'Review [file] for: SQL injection, missing auth checks, PII in logs.
             Output: | SEVERITY | ISSUE | LINE | FIX |'
       3. Exits 0 if no CRITICAL or HIGH findings
       4. Exits 1 if any CRITICAL or HIGH findings found
       
       Make the script executable.
       Follow the bash scripting patterns in our existing scripts/ directory."

# Pattern: specify exact behavior + exit codes + follow existing patterns
# Result: a script that integrates with git hooks immediately
```

---

## Example 9 — Onboarding a New Codebase

```bash
# BEFORE
codex "understand the codebase"

# Problems: "understand" is not actionable, no output format, no scope
# Output: vague summary paragraph
```

```bash
# AFTER
codex "Explore this codebase and produce an onboarding guide.
       Structure:
       1. What this system does (1 paragraph)
       2. System architecture: layers, their responsibilities, how they communicate
       3. Key files: top 10 most important files and what each one does
       4. Request lifecycle: trace one API request from HTTP call to database and back
       5. Testing approach: how tests are organized, how to run them, what the fixtures do
       6. Known complexity areas: which parts of the codebase are most complex or risky
       
       Use only information that is verifiably in the codebase.
       Do not invent or assume — if something is unclear, mark it as 'Unknown'.
       Do NOT make changes."

# Pattern: "do not invent" constraint + specific numbered structure
# Result: accurate onboarding guide, not hallucinated summaries
```

---

## Example 10 — Debugging a CI Failure

```bash
# BEFORE
codex "fix the CI failure"

# Problems: what CI? what failure? no output
# Output: generic suggestions that don't match your actual failure
```

```bash
# AFTER
# First: paste the actual CI output
CI_OUTPUT=$(cat ci-failure.log)

codex "CI pipeline failing on GitHub Actions. Here is the exact failure:

$CI_OUTPUT

Environment:
- Python 3.11, pytest 8, Ubuntu 22.04 (CI) vs macOS 14 (local)
- Tests pass locally but fail in CI

Diagnose:
1. What is the root cause? (environment difference? missing fixture? timing issue?)
2. Where exactly is it failing? (file + line)
3. What is the fix?
4. How do I verify it's fixed before pushing again?

Do not modify any test files."

# Pattern: paste real CI output + describe environment difference + ask for specific root cause
# Result: targeted diagnosis, not generic "check your environment variables"
```

---

## Summary: The 6 Prompt Improvement Patterns

| Pattern | BEFORE | AFTER |
|---------|--------|-------|
| **Scope explicitly** | "fix the bug" | "In src/auth/service.py, the login() function..." |
| **State constraints** | (no constraints) | "Do not modify tests. Do not add new dependencies." |
| **Add verification** | (no verification) | "Verification: pytest tests/test_auth.py::test_login -v" |
| **Format the output** | (no format) | "Output: \| SEVERITY \| ISSUE \| LINE \| FIX \|" |
| **Reference a pattern** | "add an endpoint" | "Follow the pattern in src/api/users.py" |
| **Separate plan from execute** | "design and build X" | Step 1: plan only. Step 2: implement from plan. |

---

## Interview Traps

```
TRAP: "The AFTER prompts are too verbose — simpler is always better"
TRUTH: The apparent verbosity serves a function: each element (scope, constraint,
       verification) prevents a specific failure mode. The BEFORE prompts are concise
       but produce high variance output. The AFTER prompts are reliable and repeatable.

TRAP: "I can learn prompt patterns by reading examples without practicing them"
TRUTH: Prompt engineering is a physical skill, not a reading skill. You internalize
       patterns by writing dozens of prompts from scratch. Read the gallery once,
       then practice writing equivalent prompts without looking at the AFTER column.

TRAP: "If the output is mostly right, the prompt was good enough"
TRUTH: 'Mostly right' means you spent time correcting instead of reviewing.
       A precise prompt produces output that needs review, not correction.
       The goal is to shift from correction cycles to review passes.
```

---

## Revision Checklist

- [ ] Can identify which of the 6 patterns is missing from any given prompt
- [ ] Can improve any BEFORE prompt from this sheet without looking at the AFTER
- [ ] Use output format specification on every review and planning prompt
- [ ] Always include "do not modify tests" in implementation and debugging prompts
- [ ] Always include a verification command in implementation prompts
