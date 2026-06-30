# Context Engineering — Gold Sheet

> **Track**: Codex Mastery Track — Group 2: Intermediate Power User
> **File**: 2 of 7 (Track File #8)
> **Audience**: Developers who want to stop getting generic output and start getting precise output
> **Read after**: AGENTS-MD-Design-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Context window size and what it means | ★★★★★ | Devs dump the entire codebase; relevant precision outperforms volume every time |
| Session drift — when to use /compact | ★★★★★ | Long sessions accumulate noise and contradictions; drift is invisible until output degrades |
| Explicit file references vs auto-discovery | ★★★★★ | Auto-discovery guesses; explicit references guarantee the right context |
| What to exclude from context | ★★★★☆ | Noise (generated files, vendor code, unrelated modules) degrades output quality |
| Session startup — priming Codex correctly | ★★★★☆ | Starting without orientation means Codex spends tokens figuring out what you mean |

---

## ⭐ Beginner Tier — Start Here

### B1: The minimum context experiment

```bash
# Run the same task twice — first with vague context, then with targeted context

# Version 1: no explicit context
codex "add error handling to the service"

# Version 2: explicit context
codex "In src/orders/service.py, add error handling to create_order():
       - DatabaseError → log warning, raise ServiceError('Database unavailable')
       - ValidationError → re-raise as-is
       - Any other exception → log error + re-raise
       Reference: follow the pattern in src/users/service.py (same error structure)
       Verification: pytest tests/test_order_service.py -x"

# Compare the outputs. Version 2 produces code that matches your codebase.
```

### B2: Know when your session has drifted

Signs your session needs /compact:
```
□ Codex contradicts advice it gave 20 messages ago
□ Codex forgets constraints you specified earlier
□ Responses are getting longer and less specific
□ Session has been running for more than 30 minutes

Fix: type /compact in interactive mode
Effect: Codex summarizes what it knows and continues fresh with less noise
```

---

## 1. The Context Window Mental Model

```
Codex processes everything in a context window — a fixed-size token budget.

What goes into the context window:
  1. AGENTS.md content
  2. Your prompts (current session)
  3. Codex's responses (current session)
  4. File contents it reads
  5. Command output it reads (test results, lint output)
  6. System prompt

The context window is finite.
More relevant context → better output.
More irrelevant context → diluted signal, worse output.
Full context window → /compact or start a new session.

Practical implication:
  Do NOT: add every file in the repo to context
  DO: add exactly the files Codex needs for this specific task
```

---

## 2. High-Signal vs Low-Signal Context

### High-signal: what improves output

```
✅ The specific file being modified
✅ A reference file that shows the pattern to follow
✅ The failing test (shows expected behavior)
✅ The error message and stack trace (for debugging)
✅ The function signature (tells Codex what exists)
✅ AGENTS.md rules (architecture, naming, forbidden actions)
```

### Low-signal: what dilutes output

```
❌ Entire directories when only 2 files are relevant
❌ Generated files (build output, compiled assets, vendor code)
❌ Unrelated modules (adding the entire auth module when you're working on payments)
❌ Long conversation history about previous unrelated tasks
❌ Boilerplate config files (pyproject.toml, package-lock.json) unless they're relevant
```

---

## 3. Explicit File References — The Core Skill

```bash
# Auto-discovery (implicit) — Codex guesses what's relevant
codex "fix the payment validation bug"
# Codex searches the codebase for payment-related code — may miss or pick wrong files

# Explicit reference — you tell Codex exactly what's in scope
codex "Fix the input validation bug in src/payments/service.py::create_payment().
       The function accepts zero-amount payments (amount <= 0) which should raise ValueError.
       Related test: tests/test_payment_service.py::test_create_payment_zero_amount
       Verification: pytest tests/test_payment_service.py::test_create_payment_zero_amount -v"
```

### Explicit reference patterns

```bash
# By file name
"In src/users/repository.py, ..."

# By function
"In the handle_login() function in src/auth/service.py, ..."

# By pattern reference
"Follow the pattern in src/api/users.py when implementing ..."

# By test reference
"The failing test is tests/test_auth.py::test_login_expired_token. Fix the implementation."
```

---

## 4. Session Startup — Priming for Precision

```bash
# Option 1: Task-scoped startup (most common)
codex "Context: working on the payment processing module (src/payments/).
       Today's task: add webhook signature verification.
       Constraint: only modify src/payments/webhook.py and tests/test_webhook.py.
       Verification: pytest tests/test_webhook.py -x"

# Option 2: Exploration startup (when unfamiliar with a codebase)
codex "Summarize the architecture of this codebase:
       1. Main modules and their responsibilities
       2. How requests flow from API to database
       3. Testing approach
       Do not make changes."
# Use the output to understand what files to reference in subsequent prompts

# Option 3: Pattern learning startup
codex "Show me an example of:
       1. How API endpoints are structured in this project
       2. How service layer functions handle errors
       3. How tests are organized for a service function
       Do not make changes — just explain the patterns."
```

---

## 5. Managing Context Drift in Long Sessions

```
Context drift happens when:
  - The session has many back-and-forth exchanges
  - You switched tasks mid-session
  - Prior conversation contradicts current instructions
  - The context window is approaching its limit

Symptoms of context drift:
  - Codex uses patterns from earlier tasks in the current task
  - Codex forgets explicit constraints you set
  - Codex starts giving generic answers instead of codebase-specific ones
  - Codex proposes approaches it already tried and abandoned

Fixes:
  1. /compact — compresses history into a summary (interactive mode only)
     Use when: drift detected, session > 30 min, want to continue the conversation

  2. Start a new session — clean context
     Use when: task changes significantly, starting a new implementation phase,
     after a full-auto session completes, when drift is severe

  3. Restate context explicitly — remind Codex of key constraints
     "Reminder: we're in the payments module, constraint: no new dependencies"
     Use when: minor drift, short correction needed
```

---

## 6. Context Scoping by Task Type

```bash
# Debugging task — minimal, focused context
codex "Stack trace: [paste exact stack trace]
       File: src/auth/session.py, function: validate_token(), line 87
       Error: KeyError: 'user_id' when token is valid but user was deleted
       Diagnosis + fix. Constraint: do not change the token validation logic."

# Implementation task — file + reference pattern
codex "Add rate limiting to POST /auth/login in src/api/auth.py.
       Reference: look at how we handle request timeouts in src/api/middleware.py.
       Rate limit: 5 failed attempts per minute per IP.
       Constraint: do not change the login logic itself."

# Refactoring task — specific function, test file
codex "Refactor get_user_orders() in src/users/service.py.
       Issue: N+1 query — it calls get_order() in a loop.
       Fix: use a single JOIN query following the pattern in get_user_products().
       Verification: pytest tests/test_user_service.py::test_get_user_orders -x
       Constraint: same function signature, same return format."

# Test generation task — just the function
codex "Generate tests for validate_payment() in src/payments/validator.py.
       Test cases: valid amount, zero amount, negative amount, non-numeric, missing currency,
       valid currency (USD/EUR/GBP), invalid currency string.
       Run: pytest tests/test_payment_validator.py -v"
```

---

## 7. The /compact Command — When and How

```
When to use /compact (in interactive mode):
  □ Session running 30+ minutes with multiple tasks completed
  □ Context drift detected (Codex forgetting earlier constraints)
  □ Transitioning from planning to implementation (new phase, new context)
  □ After completing a major task before starting the next one

What /compact does:
  - Summarizes the conversation history into a compact representation
  - Keeps the goals and key decisions
  - Drops the verbose back-and-forth
  - Frees token budget for the next task

What /compact does NOT do:
  - Does not forget what AGENTS.md says (read separately)
  - Does not forget the current task goal
  - Does not reset the file state

Usage:
  > /compact
  > Now implement the next component: [describe next task]
```

---

## Interview Traps

```
TRAP: "I give Codex access to the whole codebase for maximum context"
TRUTH: More files = more noise. Precise, relevant file references produce better output
       than dumping the entire repo. Test this yourself: same task, scoped vs unscoped.

TRAP: "I restart the session every time I start a new task"
INSIGHT: Session restart loses context you've built up. /compact preserves the important
         parts. Use /compact for transitions; restart for complete task changes.

TRAP: "Codex is producing generic output — the model isn't good enough"
TRUTH: Generic output is almost always a context problem, not a model problem.
       Add explicit file references, a pattern reference, and AGENTS.md rules.
       Rerun the same task with better context — the difference is dramatic.
```

---

## Revision Checklist

- [ ] Can explain why explicit file references outperform auto-discovery
- [ ] Can identify the 5 types of high-signal context
- [ ] Can identify the 5 types of low-signal noise to exclude
- [ ] Can use /compact correctly and explain what it does and doesn't do
- [ ] Can prime a Codex session for precision on the first message
- [ ] Can recognize and fix context drift symptoms
