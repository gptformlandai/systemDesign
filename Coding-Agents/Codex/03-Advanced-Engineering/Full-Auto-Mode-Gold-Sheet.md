# Full-Auto Mode — Gold Sheet

> **Track**: Codex Mastery Track — Group 3: Advanced Engineering
> **File**: 1 of 7 (Track File #14)
> **Audience**: Developers ready to use full-auto for real bounded tasks
> **Read after**: Approval-Policy-Modes-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Pre-conditions — all must be met before full-auto | ★★★★★ | Skipping even one pre-condition multiplies the chance of unexpected changes |
| Task specification — the bounded task prompt template | ★★★★★ | Vague task + full-auto = scope explosion across many files |
| Monitoring during execution — what to watch | ★★★★☆ | Devs start full-auto and walk away; monitoring catches scope drift early |
| Post-session review — mandatory full diff read | ★★★★★ | Not reviewing the diff is how bugs, tech debt, and scope creep get committed |
| Recovery patterns — 4 ways to undo | ★★★★☆ | Without knowing recovery, devs accept bad changes to avoid the hassle |

---

## ⭐ Beginner Tier — Start Here

### B1: Your first safe full-auto task

Use this specific task (safe, bounded, reversible):

```bash
cd your-project

# Pre-condition 1: clean checkpoint
git add -A
git commit -m "checkpoint: before first full-auto test"

# Run a genuinely safe full-auto task
codex --approval-policy full-auto \
  "Add Google-style docstrings to any public function in src/utils/ that doesn't have one.
   Only modify src/utils/ — no other directory.
   Run: pytest tests/ -x after adding docstrings to verify no regressions.
   Stop when all public functions in src/utils/ have docstrings."

# Review
git diff HEAD~1 --stat    # how many files changed?
git diff HEAD~1           # read every change
```

This is safe because: docstrings don't change behavior, tests verify no regressions, scope is narrow.

---

## 1. The Full-Auto Pre-Conditions Checklist

ALL six must be true before running full-auto:

```
[ ] 1. Git checkpoint committed
        git add -A ; git commit -m "checkpoint: [description]"
        Without this: no clean rollback point

[ ] 2. Working on a feature branch (not main)
        git branch --show-current | grep -v "main\|master"
        Without this: checkpoints mix with production history

[ ] 3. Task is bounded (can you describe it in 2 sentences?)
        "Add pagination to GET /users in src/api/users.py.
         Follow the pattern in GET /orders. Verify: pytest -x"
        Not: "improve the codebase"

[ ] 4. Files in scope are listed explicitly
        "Only modify: src/api/users.py and tests/test_user_api.py"
        Without this: Codex decides which files to touch

[ ] 5. AGENTS.md has forbidden actions
        Especially: no migrations, no git push, no .env modification
        Without this: forbidden operations may execute

[ ] 6. Verification command is in the prompt
        "Verification: pytest tests/test_user_api.py -x"
        Without this: Codex stops when it thinks it's done, not when tests pass
```

---

## 2. The Bounded Task Prompt Template

```bash
codex --approval-policy full-auto \
  "Task: [one clear sentence describing what to accomplish]
   
   Scope:
   - Files to modify: [list specific files]
   - Directories in scope: [if more than 2 files, list the directory]
   - Files NOT to touch: [explicitly protect sensitive files]
   
   Implementation guidance:
   - Follow: [reference file or pattern to follow]
   - Use: [specific library or approach]
   
   Forbidden:
   - Do not modify test files
   - Do not run database migrations
   - Do not install new packages without listing them first
   
   Verification: [command that proves the task is complete]
   
   When done: report files modified, tests passed, anything that remains."
```

### Filled example

```bash
codex --approval-policy full-auto \
  "Task: Add pagination to GET /orders endpoint.
   
   Scope:
   - Files to modify: src/api/orders.py, tests/test_order_api.py
   - Do not touch: src/db/, src/services/, any file not listed above
   
   Implementation guidance:
   - Follow: the pagination pattern in GET /users in src/api/users.py
   - Pagination params: page (int, default=1), page_size (int, default=20, max=100)
   - Response: include total_count, page, page_size, items
   
   Forbidden:
   - Do not modify src/db/order_repository.py
   - Do not run database migrations
   - Do not modify test files outside tests/test_order_api.py
   
   Verification: pytest tests/test_order_api.py -x
   
   When done: list files modified and test results."
```

---

## 3. Monitoring During Execution

Even in full-auto, Codex prints its actions. Watch the output.

### What to watch for

```
NORMAL — Expected output:
  "Reading src/api/orders.py..."
  "Modifying src/api/orders.py: adding pagination parameters..."
  "Running: pytest tests/test_order_api.py -x"
  "Test results: 12 passed"

RED FLAGS — Stop immediately (Ctrl+C):
  "Modifying src/auth/..." — auth is outside the stated scope
  "Running: alembic upgrade head" — database migration (should be in forbidden actions)
  "Installing: sqlalchemy-utils" — new package installation
  "Deleting: tests/test_..." — test deletion
  "Modifying: .env" — environment file
  "Running: git push" — pushing to remote
```

### If you see a red flag

```bash
# 1. Press Ctrl+C immediately to stop Codex
# 2. Check what was already changed
git status
git diff

# 3. Decide: rollback or keep?
# Rollback to checkpoint:
git reset HEAD~1 --soft   # unstage everything, keep changes for inspection
git checkout -- .         # discard all changes (full reset)

# 4. Narrow the scope in your prompt before retrying
# Add the red-flag file to "Do not touch" list
```

---

## 4. The Post-Session Review — Non-Negotiable

```bash
# Step 1: Overview — what changed?
git diff HEAD~1 --stat

# Step 2: Read every change (not skim — read)
git diff HEAD~1

# For each changed file, answer:
# ✅ Was this file in the stated scope?
# ✅ Does the change match what I asked for?
# ✅ Are any new imports reasonable? Do I recognize each library?
# ✅ Is there any hardcoded value that should be a config?
# ✅ Is there any function added that I didn't ask for?

# Step 3: Run the verification command
pytest -x   # or your verification command

# Step 4: Commit or rollback
# If review passes: git add -A && git commit -m "[description of what codex did]"
# If review fails: git checkout -- [specific file] or git reset HEAD~1 --soft
```

---

## 5. Recovery Patterns

```bash
# Scenario A: unexpected files modified — rollback specific files
git checkout -- src/auth/service.py   # revert one file
git checkout -- src/auth/             # revert entire directory

# Scenario B: unexpected changes, keep some of them
git diff HEAD~1                        # identify what to keep
git add [files to keep]
git reset HEAD -- [files to discard]
git checkout -- [files to discard]
git commit -m "partial accept: [what was kept]"

# Scenario C: full session was wrong — complete rollback
git reset HEAD~1 --soft              # undo commit, keep changes staged
git checkout -- .                    # discard all staged and unstaged changes

# Scenario D: complete reset (use if you're sure)
git reset --hard HEAD~1              # ← DESTRUCTIVE: discards all changes from session
# Only use this if you have a checkpoint and want a complete reset

# Scenario E: committed the wrong changes, need to undo
git revert HEAD                      # creates a new "undo" commit (safe for shared branches)
```

---

## 6. Full-Auto Task Catalog

Tasks that work well with full-auto (when properly scoped):

```bash
# ✅ Scaffold new endpoint from pattern
codex --approval-policy full-auto \
  "Add GET /products/{id} endpoint to src/api/products.py.
   Follow GET /users/{id} pattern in src/api/users.py exactly.
   Create: src/api/products.py and tests/test_product_api.py.
   Verification: pytest tests/test_product_api.py -x"

# ✅ Add input validation to a function
codex --approval-policy full-auto \
  "Add input validation to create_subscription() in src/billing/service.py.
   Validate: plan_id is a string, start_date is a valid ISO date, amount > 0.
   Raise: ValidationError for each invalid case.
   Only modify src/billing/service.py.
   Verification: pytest tests/test_billing_service.py -x"

# ✅ Generate tests for an existing module
codex --approval-policy full-auto \
  "Generate tests for src/notifications/service.py.
   Cover: all public functions, happy path + 2 error cases each.
   Create: tests/test_notification_service.py.
   Verification: pytest tests/test_notification_service.py -v (all pass)"

# ✅ Add error handling to a service
codex --approval-policy full-auto \
  "Add error handling to src/emails/service.py.
   Wrap each external call (smtp client, template render) in try/except.
   Log warnings for recoverable errors, log errors for unexpected ones.
   Use structlog.
   Verification: pytest tests/test_email_service.py -x"
```

---

## Interview Traps

```
TRAP: "Full-auto is safe because Codex is careful"
TRUTH: Full-auto is powerful but not inherently safe. Safety comes from:
       (a) git checkpoint, (b) bounded scope, (c) forbidden actions in AGENTS.md,
       (d) post-session diff review. None of these are Codex's job — they're yours.

TRAP: "I don't need to review the diff if tests pass"
TRUTH: Tests don't catch: scope creep, wrong abstractions, new tech debt,
       convention violations, security issues. Review is mandatory regardless of test results.

TRAP: "I can use full-auto on the main branch"
TRUTH: Full-auto should run on a feature branch. Commits on main from an agent session
       before human review contaminate production history.
```

---

## Revision Checklist

- [ ] All 6 pre-conditions checked before every full-auto session
- [ ] Can write a bounded task prompt using the template
- [ ] Know the 6 red flags to watch for during execution
- [ ] Post-session review is automatic: git diff HEAD~1, read every change
- [ ] Know all 5 recovery patterns (which to use when)
- [ ] Have run at least 2 full-auto tasks on real code safely
