# Autonomous Workflows — Gold Sheet

> **Track**: Codex Mastery Track — Group 4: Pro / Production Level
> **File**: 5 of 5 (Track File #25)
> **Audience**: Developers ready to use Codex for end-to-end autonomous feature delivery
> **Read after**: Verification-Driven-Workflows-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Safe autonomy checklist — 7 conditions | ★★★★★ | One missing condition can derail an entire autonomous session |
| Full autonomous feature build template | ★★★★★ | The most powerful workflow in this track — execute to deliver complete features |
| Recovery from wrong-direction autonomous session | ★★★★☆ | Knowing how to recover quickly makes full-auto less scary |
| Autonomous bug fix workflow | ★★★★☆ | Codex can reproduce + fix + verify bugs without human in the loop |
| What autonomous workflows enable (the compound benefit) | ★★★☆☆ | The point isn't the tool — it's the leverage and review focus it enables |

---

## ⭐ Beginner Tier — Start Here

### B1: The checkpoint discipline — build the habit first

Before learning full autonomous workflows, this habit must be automatic:

```bash
# This sequence must be muscle memory before attempting autonomous workflows
function codex-auto() {
    git add -A
    git commit -m "checkpoint: before codex $1"
    codex --approval-policy full-auto "$1"
    echo "=== Changes ===" && git diff HEAD~1 --stat
}

# Usage:
codex-auto "add pagination to GET /users"
```

If this habit is not automatic: practice it on small tasks for a week before moving to
full autonomous feature builds.

---

## 1. The Safe Autonomy Checklist — All 7 Required

```
Before every autonomous (full-auto) session:

[ ] 1. Working on a feature branch (not main or master)
        git branch --show-current | grep -v "main\|master\|prod"

[ ] 2. Clean git checkpoint committed
        git add -A && git commit -m "checkpoint: [description]"

[ ] 3. Task is bounded — can describe in 2 sentences
        Bad: "improve the codebase"
        Good: "add GET /orders/{id} endpoint following GET /users/{id} pattern"

[ ] 4. Explicit file scope in the prompt
        "Only modify: src/api/orders.py, tests/test_order_api.py"

[ ] 5. Forbidden actions explicit
        "Do not touch: src/auth/, src/db/migrations/, any .env file"

[ ] 6. Verification command specified
        "Verification: pytest tests/test_order_api.py -x"

[ ] 7. Post-session review planned
        "After this completes: run git diff HEAD~1 and read every change"
```

---

## 2. Full Autonomous Feature Build Template

```bash
#!/bin/bash
# The complete template for a full autonomous feature delivery

# Pre-session
git add -A
git commit -m "checkpoint: before autonomous build — [feature name]"

# Run
codex --approval-policy full-auto \
  "Autonomous feature build: [feature name]
   
   OBJECTIVE:
   [One sentence: what the feature does]
   
   IMPLEMENTATION SCOPE:
   Files to create: [list]
   Files to modify: [list]
   Do NOT touch: [list protected files/dirs]
   
   IMPLEMENTATION REFERENCE:
   Follow the patterns in: [reference file 1], [reference file 2]
   
   PROCESS (execute in this order):
   1. Read and understand the reference files
   2. Implement [component 1]
   3. Run: [test command] — fix failures before continuing
   4. Implement [component 2]
   5. Run: [test command] — fix failures before continuing
   6. Run: [lint command] — fix all issues
   7. Run full test suite: [full test command]
   
   FORBIDDEN ACTIONS:
   - Do not modify test files
   - Do not run database migrations
   - Do not install new packages without listing them first
   - Do not modify files outside the stated scope
   
   DONE WHEN:
   - All stated test commands pass
   - Lint passes
   - Full test suite shows no regressions
   
   FINAL REPORT:
   - Files created/modified
   - Test results (N passed, M failed)
   - Anything that requires human decision before merge"
```

### Filled example

```bash
codex --approval-policy full-auto \
  "Autonomous feature build: order pagination
   
   OBJECTIVE:
   Add pagination to the GET /orders endpoint, returning page, page_size, total_count, items.
   
   IMPLEMENTATION SCOPE:
   Files to modify: src/api/orders.py, tests/test_order_api.py
   Do NOT touch: src/db/, src/services/user_service.py, any file not listed
   
   IMPLEMENTATION REFERENCE:
   Follow the GET /users pagination pattern in src/api/users.py exactly.
   Same response format: {page, page_size, total_count, items}
   Same query params: page (default=1), page_size (default=20, max=100)
   
   PROCESS:
   1. Read src/api/users.py to understand the existing pagination pattern
   2. Add pagination to GET /orders in src/api/orders.py
   3. Run: pytest tests/test_order_api.py -x
   4. Add pagination tests to tests/test_order_api.py (default values, max, page 0)
   5. Run: pytest tests/test_order_api.py -v
   6. Run: ruff check src/api/orders.py
   7. Run: pytest -x (full suite)
   
   FORBIDDEN ACTIONS:
   - Do not modify src/db/order_repository.py
   - Do not modify src/services/order_service.py  
   - Do not run database migrations
   
   DONE WHEN:
   - pytest tests/test_order_api.py -v: all pass
   - ruff check src/api/orders.py: 0 errors
   - pytest -x: 0 failures
   
   FINAL REPORT: list all changes + test results"
```

---

## 3. Autonomous Bug Fix Workflow

```bash
# Step 1: Capture the exact reproduction
REPRO="pytest tests/test_payments.py::test_create_payment_concurrent -v 2>&1"
FAILURE=$(eval $REPRO)

# Step 2: Run autonomous fix
git commit -m "checkpoint: before autonomous bug fix"

codex --approval-policy full-auto \
  "Autonomous bug fix:
   
   FAILING TEST:
   tests/test_payments.py::test_create_payment_concurrent
   
   FAILURE OUTPUT:
   $FAILURE
   
   REPRODUCTION COMMAND:
   pytest tests/test_payments.py::test_create_payment_concurrent -v
   
   PROCESS:
   1. Read the failing test to understand expected behavior
   2. Read the implementation in src/payments/service.py
   3. Diagnose: what is the root cause?
   4. Fix the implementation (not the test)
   5. Run: pytest tests/test_payments.py::test_create_payment_concurrent -v
   6. If still failing: try a different fix approach
   7. Once passing: run full suite pytest -x to check regressions
   
   FORBIDDEN:
   - Do not modify test files
   - Do not change the function signature
   
   DONE WHEN:
   - pytest tests/test_payments.py::test_create_payment_concurrent passes
   - pytest -x: no new failures"
```

---

## 4. Autonomous Refactoring

```bash
# Only for refactors with a clear before/after contract: behavior must not change
git commit -m "checkpoint: before autonomous refactor"

codex --approval-policy full-auto \
  "Autonomous refactor: [describe structural change]
   
   FILE IN SCOPE: [one file or small set]
   
   STRUCTURAL CHANGE:
   [Exactly what to restructure — e.g., 'extract database calls into repository methods']
   
   BEHAVIOR CONSTRAINT:
   All existing tests must still pass after the refactor.
   No behavior changes allowed — only structural changes.
   
   PROCESS:
   1. Run: pytest [test file] -x — record baseline (all must pass)
   2. Make ONE structural change
   3. Run: pytest [test file] -x — must still pass
   4. Repeat until all planned structural changes are complete
   5. If any test fails after a change: undo that specific change and try differently
   6. Run: ruff check [file] — fix lint
   7. Run full suite: pytest -x
   
   FORBIDDEN:
   - Do not modify test files
   - Do not change public function signatures
   - Do not change behavior — only structure
   
   DONE WHEN:
   - Same tests that passed before refactor still pass after
   - Lint passes
   - No regressions"
```

---

## 5. Recovery Patterns (When Autonomous Goes Wrong)

```bash
# Pattern 1: Wrong direction — stop immediately
# Ctrl+C to interrupt Codex mid-session

# Pattern 2: Session completed but output is wrong
git diff HEAD~1 --stat    # how many files changed?
git diff HEAD~1            # read every change

# Full rollback:
git reset --hard HEAD~1   # returns to checkpoint exactly (DESTRUCTIVE)

# Partial rollback (keep good parts):
git checkout -- src/auth/    # revert just the auth directory
git checkout -- tests/       # revert test changes

# Pattern 3: Session modified unexpected files — surgical revert
git checkout -- [unexpected-file]  # revert one file, keep the rest

# Pattern 4: Committed the wrong result
git revert HEAD             # safe undo commit (creates new "undo" commit)

# Pattern 5: Multiple commits need undoing
git reset HEAD~3 --soft    # undo 3 commits, keep changes staged for inspection
```

---

## 6. What Autonomous Workflows Enable

```
Without autonomous workflows:
  - You implement → you test → you debug → you test again
  - Implementation + iteration time = your time
  - Cognitive overhead: you think about implementation details

With autonomous workflows:
  - You specify → Codex implements + tests + iterates
  - Your time: review output, decide next step
  - Cognitive overhead: evaluating output quality, catching wrong direction

The compound benefit:
  - Your time shifts from: "how do I implement this?" to "is this the right implementation?"
  - This is the same shift as: intern → senior engineer → tech lead
  - The most valuable skill: specification + review, not implementation mechanics

Where your time goes instead:
  - Architecture decisions (Codex doesn't know your long-term system goals)
  - Security review (Codex doesn't always catch its own security bugs)
  - Product decisions (Codex doesn't know your users)
  - Code review (the standard for review doesn't change because Codex wrote the code)
```

---

## Interview Traps

```
TRAP: "Full-auto is autonomous so I don't need to review the output"
TRUTH: The review is MORE important for full-auto because Codex had more freedom.
       Post-session mandatory: git diff HEAD~1 (every line), full test suite, scope
       verification (only expected files changed). Autonomy doesn't replace review.

TRAP: "Load the entire codebase for context in full-auto mode — more is better"
TRUTH: Wide scope + full-auto = highest probability of unexpected changes to unexpected files.
       Autonomous sessions need the MOST precise scoping of any workflow. The bounded task
       template's explicit IN-scope and OUT-of-scope lists exist for this reason.

TRAP: "The 7-condition pre-flight checklist is conservative overkill for small tasks"
TRUTH: Each condition addresses a specific failure mode from real usage.
       Feature branch = recovery path. Checkpoint = rollback. Bounded task = scope control.
       Explicit forbidden list = protects auth and migrations.
       Missing one condition from a session that goes wrong = hours of cleanup.
```

---

## Revision Checklist

- [ ] All 7 safe autonomy conditions checked before every full-auto session
- [ ] Can write a complete autonomous feature build prompt using the template
- [ ] Can run an autonomous bug fix with correct reproduction and constraints
- [ ] Know all 5 recovery patterns and which to use in each scenario
- [ ] Checkpoint commit is automatic — no autonomous session without one
- [ ] Post-session diff review is mandatory — tests passing ≠ review complete
