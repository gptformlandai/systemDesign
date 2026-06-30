# Codex CLI Fundamentals — Gold Sheet

> **Track**: Codex Mastery Track — Group 1: Foundations
> **File**: 3 of 6 (Track File #3)
> **Audience**: Developers who have installed Codex and want to understand how it works
> **Read after**: Codex-Setup-Personal-Machine-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Interactive REPL vs non-interactive — when to use each | ★★★★★ | Devs use interactive mode for everything; scripted non-interactive is faster for CI |
| How Codex reads your repository | ★★★★★ | Understanding this lets you scope Codex correctly; without it, you wonder why output is wrong |
| /compact command — prevents context drift | ★★★★☆ | Long sessions accumulate noise; /compact resets efficiently without losing goals |
| Providing file context explicitly | ★★★★☆ | Relying on auto-discovery misses critical files; explicit context = precise output |
| Reading the plan before approving | ★★★★☆ | Accepting Codex's first execution without reading the plan is the most common mistake |
| Non-interactive with --quiet for CI | ★★★☆☆ | CI scripts need non-interactive mode; devs use interactive mode in automation |

---

## ⭐ Beginner Tier — Start Here

### B1: Your first safe read-only task

```bash
cd your-project
codex "Explain what user_service.py does. List: its inputs, outputs, and 3 things that could go wrong. Do not make changes."
```

Notice:
- Codex read the file automatically
- Output is structured (not freeform)
- Nothing was changed
- You're in full control

### B2: Understanding suggest mode visually

```bash
codex --approval-policy suggest "Add a docstring to the login() function in auth.py"
```

What you'll see:
1. Codex shows you the proposed change (the docstring it wants to add)
2. It asks: "Apply this change? [y/N]"
3. You say `y` to apply, `n` to reject
4. Nothing happens without your explicit approval

This is how Codex should feel for your first week — you see every change before it applies.

---

## 1. Interactive Mode vs Non-Interactive Mode

### Interactive Mode (REPL)

```bash
# Start interactive mode
codex

# What you get:
# - A persistent session with memory
# - Back-and-forth conversation
# - /compact, /help, /quit commands available
# - Best for: exploratory sessions, debugging, back-and-forth refinement
```

Session example:
```
You: Explain the payment flow in this codebase
Codex: [detailed explanation]

You: Now add input validation to create_payment()
Codex: [proposes changes based on prior context — knows the payment flow already]

You: /compact      ← compresses history, frees context
You: Now generate tests for the validated create_payment()
Codex: [generates tests, knowing the function from earlier in the session]
```

### Non-Interactive Mode (Single Task)

```bash
# Single task — exits when done
codex "add input validation to create_payment() in payments/service.py"

# With flags
codex --model gpt-4.1 --approval-policy auto-edit \
  "refactor the auth module to use JWTs instead of sessions"

# Scripted (no prompts — for CI/Makefile)
codex --quiet --approval-policy auto-edit \
  "generate tests for any function without a test file"
```

---

## 2. How Codex Reads Your Repository

### What Codex sees when you run it

```
Working directory → Codex scans for relevant files
AGENTS.md         → Read first, always (project instructions)
Conversation      → Prior messages in this session
Explicit refs     → Files you mention by name in your prompt
```

### File discovery — what Codex reads

```
Codex does NOT read every file blindly. It uses heuristics:
  1. Files most relevant to the task based on the prompt
  2. Files near the files it identified as relevant
  3. Config files (package.json, requirements.txt, pyproject.toml)
  4. Files explicitly referenced in your prompt

To be explicit about scope:
  "In payments/service.py, add input validation to create_payment()"
  ↑ This constrains Codex to the exact file you mean

Vague (risky):
  "Add input validation to the payment creation function"
  ↑ Codex may look in multiple places or pick the wrong function
```

### Context priority order

```
1. AGENTS.md (highest authority — read first)
2. Files you explicitly name in the prompt
3. Files inferred as relevant by Codex
4. General training knowledge (lowest — overridden by context)

Implication: if AGENTS.md says "always raise HTTPException",
Codex will use HTTPException even if its training default would be ValueError.
```

---

## 3. Interactive Mode Commands

```
/help       — List all available commands
/compact    — Compress conversation history into a summary
              Use when: session has been running 30+ minutes
              Effect: reduces token usage, prevents context drift
              Does NOT lose your goals — Codex summarizes what it knows

/quit       — Exit interactive mode (alias: /exit, Ctrl+C)
/clear      — Clear the current context (WARNING: forgets the session)

# Multiline input
\           — Continue your prompt on the next line
              Example:
              codex > Add input validation to create_payment(). \
                      Validate: amount > 0, currency in ['USD','EUR','GBP']. \
                      Raise ValueError for invalid input. \
                      Run: pytest tests/test_payments.py -x
```

---

## 4. Reading Codex's Plan Before Approving

This is the most important habit in this file.

```
When Codex proposes changes, it shows you a plan:
  - Files to modify
  - What changes it will make in each file
  - Commands it will run

ALWAYS read the plan before pressing y to approve.

Red flags to watch for in the plan:
  ⚠ Files outside the stated scope (payments/service.py but also auth/session.py?)
  ⚠ New dependencies added (it wants to install a new package?)
  ⚠ Database or migration files modified
  ⚠ Test files deleted or rewritten
  ⚠ Configuration files changed

If you see a red flag: type n, refine your prompt with explicit constraints,
run again with a more bounded scope.
```

---

## 5. Explicit vs Implicit File Context

### Use explicit file references for precision

```bash
# Explicit — best
codex "In src/api/users.py, add pagination to the list_users() endpoint.
       Follow the pagination pattern in src/api/products.py.
       Run: pytest tests/test_user_api.py -x"

# Implicit — riskier on larger codebases
codex "Add pagination to the users endpoint"
# → Codex may find the wrong file, the wrong function, or create a new file
```

### Pattern: tell Codex which file to follow as a reference

```bash
codex "Add a new GET /orders endpoint following the exact pattern used in src/api/users.py.
       Create it in src/api/orders.py.
       Use the same error handling and response format."
```

---

## 6. The Verification Command Pattern

```bash
# WITHOUT verification command — risky
codex "Fix the failing login test"
# Codex implements a fix and stops. Test may still fail.

# WITH verification command — safe
codex "Fix the failing test in tests/test_auth.py::test_login_invalid_password.
       Verification: pytest tests/test_auth.py::test_login_invalid_password -v
       Iterate until the test passes. Do not modify the test file."
# Codex runs the test after each fix, iterates until it passes.
```

Always include a verification command for implementation tasks.

---

## 7. Session Management Best Practices

```
Session hygiene:
  - One task per non-interactive invocation
  - Interactive sessions: use /compact every 30 minutes
  - Interactive sessions: restart when topic changes significantly
  - Never use full-auto without git checkpoint

When to start a new session:
  - Moving from planning to implementation
  - Moving from implementation to testing
  - After a full-auto session completes (fresh context for review)
  - If Codex starts giving inconsistent answers (context drift)

Context drift signals:
  - Codex contradicts something it said earlier in the session
  - Codex forgets a constraint you specified 20 messages ago
  - Codex's suggestions get less specific or more generic over time
```

---

## 8. What Codex's Output Tells You

```
Good output signals:
  ✅ References specific file names and function names from your codebase
  ✅ Follows patterns it found in your existing code
  ✅ Applies AGENTS.md constraints (uses your error handling, naming, etc.)
  ✅ Asks a clarifying question when it's unsure about scope
  ✅ Reports what it verified (test results, lint output)

Bad output signals:
  ⚠ Generic code that could apply to any codebase
  ⚠ Uses imports or patterns not present in your project
  ⚠ Violates constraints you specified in AGENTS.md
  ⚠ Changes files you didn't mention
  ⚠ Reports "done" without running the verification command

If you see bad signals: stop, check AGENTS.md is loaded, narrow the scope,
and re-run with explicit file references.
```

---

## Interview Traps

```
TRAP: "I use interactive mode for everything"
INSIGHT: Non-interactive mode is better for CI, scripts, and repeatable tasks.
         Interactive mode is better for exploration and iteration.
         Knowing when to use each is a skill.

TRAP: "Codex reads my entire codebase automatically"
TRUTH: Codex uses relevance heuristics. It may miss important context.
       Always provide explicit file references for precision.

TRAP: "I don't need to read the plan — Codex is good"
TRUTH: Even good models make scope errors. Reading the plan before approving
       is the safest way to catch issues before they happen.
```

---

## Revision Checklist

- [ ] Can describe when to use interactive vs non-interactive mode
- [ ] Can explain how Codex discovers and reads project files
- [ ] Can use /compact and explain when and why to use it
- [ ] Can write a prompt with explicit file references (not vague discovery)
- [ ] Can identify red flags in Codex's proposed plan before approving
- [ ] Always includes a verification command for implementation tasks
