# Prompt Engineering for Codex — Gold Sheet

> **Track**: Codex Mastery Track — Group 1: Foundations
> **File**: 4 of 6 (Track File #4)
> **Audience**: Developers who can run Codex but want consistently better output
> **Read after**: Codex-CLI-Fundamentals-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Constraint-first prompting | ★★★★★ | Adding constraints after the goal is too late — Codex starts executing with its defaults |
| Explicit file + function scope | ★★★★★ | Vague scope → wrong file edits; explicit scope → precise changes |
| Verification command in every implementation prompt | ★★★★★ | Without it, "done" means "I stopped" not "tests pass" |
| Separation of plan vs execute | ★★★★☆ | Mixing planning and execution in one prompt produces worse output for both |
| Output format specification | ★★★★☆ | Codex produces freeform output by default; specifying format saves parsing time |
| Reference existing patterns explicitly | ★★★☆☆ | "Follow the pattern in X" outperforms describing the pattern in words |

---

## ⭐ Beginner Tier — Start Here

### B1: The bad prompt vs good prompt side-by-side

```bash
# BAD — vague, no scope, no verification
codex "fix the authentication bug"

# GOOD — scoped, constrained, verifiable
codex "In src/auth/login.py, the login() function returns 200 even for invalid passwords.
       Fix: compare the hashed password correctly using bcrypt.checkpw().
       Constraint: do not modify the test file.
       Verification: pytest tests/test_auth.py::test_login_invalid_password -v"
```

The good prompt contains 4 things the bad one lacks:
- **Where**: `src/auth/login.py`
- **What exactly**: the specific bug (returns 200 for invalid password)
- **Constraint**: do not touch tests
- **Verification**: the command that proves it's fixed

### B2: The 60-second prompt improvement drill

Take any prompt you'd normally write. Apply this checklist before sending:
```
[ ] Does it reference the specific file and function?
[ ] Does it state at least one constraint (what NOT to do)?
[ ] Does it include a verification command?
[ ] Is it under 100 words? (shorter = less ambiguity)
```
If all 4 are yes: send it. If not: fix the ones that failed.

---

## 1. The Four Ingredients of a Good Codex Prompt

```
1. SCOPE  — What file(s), what function(s), what layer
2. GOAL   — What outcome you want (not how to get there)
3. CONSTRAINTS — What not to do, what must be preserved, rules to follow
4. VERIFICATION — The command that proves the task is done

Example with all 4:
  SCOPE:        "In src/api/orders.py, the create_order() function"
  GOAL:         "Add input validation: amount must be positive, currency must be in ['USD','EUR']"
  CONSTRAINTS:  "Raise ValueError for invalid input. Do not modify any other functions."
  VERIFICATION: "Run: pytest tests/test_orders.py -x"
```

---

## 2. Constraint-First Prompting

```bash
# Standard (goal first) — Codex starts with its defaults
codex "Add a caching layer to the user service"

# Constraint-first — Codex plans with your rules from the start
codex "Add a caching layer to the user service.
       Constraints:
       - Use Redis with TTL=300s (not in-memory cache)
       - Cache only GET /users/{id} (not list or write operations)
       - Do not modify existing tests
       - Do not add new external dependencies beyond redis-py
       Run: pytest -x after implementing"
```

Why constraint-first works:
- Codex reads the full prompt before starting to plan
- Constraints shape the plan, not just execution
- Common defaults Codex would use (in-memory cache, TTL=60s) are overridden up front

---

## 3. Explicit Scope Reference

```bash
# Vague scope — multiple valid interpretations
codex "refactor the database access code"

# Explicit scope — one correct interpretation
codex "Refactor src/db/user_repository.py only.
       Extract the raw SQL query strings into named constants at the top of the file.
       Follow the pattern in src/db/product_repository.py.
       No behavior changes — tests must still pass: pytest tests/test_user_repo.py -x"
```

### "Follow the pattern in X" — most underused instruction

```bash
codex "Add a DELETE /orders/{id} endpoint to src/api/orders.py.
       Follow the exact pattern used in DELETE /users/{id} in src/api/users.py:
       same error handling, same auth check, same response format."
```

This produces more idiomatic output than describing the pattern in words.

---

## 4. Separating Plan from Execute

```bash
# BAD: mixed plan + execute
codex "Design and implement the notification service"
# Codex will make assumptions in the plan and execute them all at once

# GOOD: plan first (safe), execute second (scoped)

# Step 1: Plan only
codex "Design the notification service for this project.
       Output: implementation plan with files to create, interfaces, and dependencies.
       Do NOT implement anything. Plan only."

# Review the plan. Correct any wrong assumptions.

# Step 2: Execute with the agreed plan
codex --approval-policy auto-edit \
  "Implement the notification service using the plan: [paste plan here].
   Follow only the plan exactly. Run: pytest -x after each component."
```

---

## 5. Output Format Specification

```bash
# No format specified — Codex prose output
codex "Review auth.py for security issues"
# Returns: freeform paragraph with issues mixed together

# Format specified — structured, scannable
codex "Review auth.py for security issues.
       Format your response as a table:
       | SEVERITY | ISSUE | FILE:LINE | FIX |
       Use severity: CRITICAL / HIGH / MEDIUM / LOW
       Order by severity descending."

# For planning tasks
codex "Plan the implementation of the orders API.
       Format: numbered steps. Each step: file to modify, what changes, estimated complexity."

# For code review
codex "Review the changes in [file].
       Format: Approved / Approve with Comments / Changes Required.
       List each issue: severity + description + specific fix."
```

---

## 6. Before/After Prompt Gallery

### Gallery 1: Implementing a feature

```bash
# BEFORE (vague)
codex "add user authentication"

# AFTER (precise)
codex "In src/auth/, implement JWT-based authentication:
       1. POST /auth/login: validate credentials, return signed JWT (exp: 24h)
       2. GET /auth/me: verify JWT, return user profile
       Use: PyJWT library (already in requirements.txt)
       Constraints: use bcrypt for password hashing, never log passwords
       Reference: follow the pattern in src/api/users.py for error handling
       Verification: pytest tests/test_auth.py -x"
```

### Gallery 2: Debugging

```bash
# BEFORE (vague)
codex "fix the broken test"

# AFTER (precise)
codex "The test tests/test_payments.py::test_create_payment_with_zero_amount is failing.
       Error: AssertionError: 400 != 200 (expected 400 for zero-amount validation)
       The create_payment() function in src/payments/service.py does not validate amount > 0.
       Fix: add validation that raises HTTPException(400) if amount <= 0.
       Constraint: do not modify the test.
       Verification: pytest tests/test_payments.py::test_create_payment_with_zero_amount -v"
```

### Gallery 3: Refactoring

```bash
# BEFORE (risky)
codex "refactor the service layer"

# AFTER (bounded)
codex "Refactor src/services/user_service.py only.
       Extract the email validation logic (lines 45-67) into a separate function: validate_email().
       No behavior changes — all existing tests must still pass.
       Do not modify any other files.
       Verification: pytest tests/test_user_service.py -x"
```

### Gallery 4: Code review

```bash
# BEFORE (generic)
codex "review my code"

# AFTER (structured)
codex "Review the function create_order() in src/orders/service.py.
       Check specifically:
       1. SQL injection risk (is the query parameterized?)
       2. Missing input validation (what inputs are unchecked?)
       3. Error paths without tests (which exceptions are not tested?)
       4. Convention violations vs AGENTS.md
       Output: table with severity, issue, file:line, fix."
```

---

## 7. The Minimal Viable Prompt (< 60 words)

For small targeted tasks, shorter is often better:

```bash
# Under 60 words — specific, bounded, complete
codex "In src/users/repository.py, add a get_user_by_email(email: str) method.
       Follow the pattern of get_user_by_id().
       Use parameterized query (not string formatting).
       Return User | None.
       Verify: pytest tests/test_user_repo.py -x"
```

Count: 39 words. Contains scope, goal, pattern reference, constraint, verification.

---

## 8. Anti-Patterns — What Not to Do

```
ANTI-PATTERN                     PROBLEM
"Fix the bug"                    What bug? Which file? What behavior?
"Make it faster"                 Faster how? What's the bottleneck?
"Add tests"                      For what? Happy path only?
"Refactor everything"            Everything = scope explosion
"Do it the right way"            Codex will pick its default "right way"
"Also add logging while you're at it"  Multi-goal prompts produce worse output for each goal
"Improve the code"               "Improve" is not a definition — it's a preference
```

---

## Interview Traps

```
TRAP: "Codex understood what I meant — I don't need to be explicit"
TRUTH: Codex generates plausible output for vague prompts, but plausible ≠ correct.
       High-quality prompts are explicit about scope, goal, constraints, and verification.
       The quality difference between precise and vague is not subtle — it's first-pass success
       rate vs 3-5 correction cycles.

TRAP: "More context = better output — paste the whole codebase"
TRUTH: Noise degrades output quality. 3-5 highly relevant files outperforms
       loading 30 files where 25 are irrelevant to the task.
       Codex can't reason about everything simultaneously — scope precisely.

TRAP: "Combining plan and implement in one prompt is more efficient"
TRUTH: Separating plan (suggest mode) from implement (auto-edit) catches wrong direction
       before it's committed to code. A bad plan discovered in planning = 2 minutes lost.
       A bad plan discovered after implementation = 30+ minutes of rework.
```

---

## Revision Checklist

- [ ] Can write a prompt with all 4 ingredients: scope, goal, constraints, verification
- [ ] Can use the "follow the pattern in X" instruction
- [ ] Can write a plan-only prompt that produces no code changes
- [ ] Can specify output format for review and planning tasks
- [ ] Can recognize and fix the 7 anti-patterns
- [ ] Can produce a good prompt under 60 words for a targeted task
