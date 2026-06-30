# Approval Policy Modes — Gold Sheet

> **Track**: Codex Mastery Track — Group 2: Intermediate Power User
> **File**: 4 of 7 (Track File #10)
> **Audience**: Developers moving from suggest to auto-edit and full-auto
> **Read after**: AGENTS-MD-Design-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| The risk spectrum: suggest → auto-edit → full-auto | ★★★★★ | Treating them as equivalent; full-auto on unfamiliar code causes unpredictable changes |
| Git checkpoint as a mandatory pre-condition for full-auto | ★★★★★ | Without it, recovery from unexpected changes requires manual work |
| Scope control in full-auto prompts | ★★★★★ | Vague task + full-auto = Codex makes sweeping changes across many files |
| What to do when full-auto goes wrong | ★★★★☆ | Developers panic; there are systematic recovery steps |
| Sandboxed environment for risky full-auto tasks | ★★★☆☆ | Running full-auto on a branch or clone isolates blast radius |

---

## ⭐ Beginner Tier — Start Here

### B1: Experience all three modes in 10 minutes

```bash
# Use a throwaway file for this exercise
echo "def calculate(x, y): pass" > /tmp/test_modes.py
cd /tmp

# Mode 1: suggest — you approve every individual change
codex --approval-policy suggest "Implement calculate() to return x * y + 10"
# You'll see: proposed change → y/n prompt

# Mode 2: auto-edit — applies file changes, asks before running commands
codex --approval-policy auto-edit "Add a docstring to calculate()"
# Watch: the file changes automatically, no y/n for the edit

# Mode 3: full-auto — completely autonomous
codex --approval-policy full-auto "Add type hints and a docstring to calculate()"
# Watch: everything happens without any prompts

cat test_modes.py   # review the result
rm test_modes.py
```

Observation: full-auto is fastest. The risk is proportional to scope.

---

## 1. The Three Modes Explained

### suggest (safe default)

```bash
codex --approval-policy suggest "task"
# OR in config.yaml: approval_policy: suggest
```

```
What happens:
  - Codex proposes each change (file edit or command) before executing
  - You see a diff of each proposed change
  - You type y to apply, n to skip
  - Nothing happens without explicit approval

When to use:
  - Learning Codex on a new codebase
  - Working in security-sensitive code (auth, crypto, payments)
  - Unfamiliar parts of a large codebase
  - First run on any project — always start here

Overhead:
  - Slowest — every change requires approval
  - Worth it: you catch scope issues before they happen
```

### auto-edit (daily driver)

```bash
codex --approval-policy auto-edit "task"
# OR in config.yaml: approval_policy: auto-edit
```

```
What happens:
  - File edits apply automatically (no approval needed)
  - Commands (pytest, npm run, shell scripts) require your approval
  - You see command proposals with a y/n prompt

When to use:
  - Day-to-day development in familiar code
  - Test generation, docstrings, refactoring
  - Tasks where you trust the file changes but want to control command execution

Risk level: Low
  - File changes are easy to review and revert (git diff / git checkout)
  - You still control every shell command
```

### full-auto (power mode)

```bash
codex --approval-policy full-auto "task"
# OR in config.yaml: approval_policy: full-auto
```

```
What happens:
  - Codex executes everything: file edits AND commands, with no interruptions
  - Codex runs your tests, reads output, iterates, and continues without pausing

When to use:
  - Well-defined, bounded scaffold tasks ("add a new endpoint following X pattern")
  - Known-safe operations on code you fully understand
  - Long verification loops (implement → test → fix → test) where interruption kills flow

Prerequisites — ALL must be true:
  ✅ git checkpoint commit done BEFORE running
  ✅ Task is clearly bounded (specific files, specific goal)
  ✅ AGENTS.md has forbidden actions that protect dangerous operations
  ✅ You'll review the full diff after it completes

Risk level: Medium — proportional to scope
  - Wider scope = more files affected
  - Vague task description = Codex guesses and may guess wrong
```

---

## 2. The Full-Auto Pre-Flight Checklist

Run this before EVERY full-auto session:

```bash
# Step 1: Commit a checkpoint
git add -A
git commit -m "checkpoint: before codex full-auto — [brief description]"

# Step 2: Verify AGENTS.md has forbidden actions
grep -A5 "Forbidden" AGENTS.md

# Step 3: Write a bounded task (fill in this template):
TASK="In [specific directory or file(s)], [verb] [specific thing].
      Forbidden: do not touch [list files/dirs outside scope].
      Verification: [command that proves done]"

# Step 4: Run full-auto
codex --approval-policy full-auto "$TASK"

# Step 5: After — review the diff
git diff HEAD~1 --stat
git diff HEAD~1
```

---

## 3. Full-Auto Task Scope — The Critical Variable

```bash
# WIDE SCOPE (dangerous without careful AGENTS.md)
codex --approval-policy full-auto \
  "Refactor the codebase to use async functions throughout"
# Codex may touch 50+ files; hard to review

# NARROW SCOPE (safe and fast)
codex --approval-policy full-auto \
  "In src/api/orders.py only: add a GET /orders/{id} endpoint.
   Follow the pattern in src/api/users.py.
   Run: pytest tests/test_order_api.py -x
   Do not touch any file outside src/api/orders.py and tests/test_order_api.py."
# Codex knows exactly what to change; easy to review
```

### Scope reduction techniques

```bash
# Technique 1: Explicit file list
"Only modify these files: src/payments/service.py, tests/test_payment_service.py"

# Technique 2: Explicit directory boundary
"Work only within src/notifications/. Do not touch any other directory."

# Technique 3: Single function scope
"Only modify the create_order() function in src/orders/service.py. No other functions."

# Technique 4: Forbidden file list
"Do not touch: src/auth/, src/db/migrations/, any .env file"
```

---

## 4. Recovery When Full-Auto Goes Wrong

```bash
# Scenario: full-auto modified unexpected files
git status          # see all modified files
git diff HEAD~1     # see all changes

# Recovery options:
# Option 1: Undo the entire session
git reset HEAD~1 --soft   # unstages all changes, keeps them for review
git checkout -- .         # discards all unstaged changes (full reset to checkpoint)

# Option 2: Keep good changes, undo specific files
git checkout -- src/auth/  # revert only the auth directory
git checkout -- tests/     # revert only test changes

# Option 3: Cherry-pick the good parts
git stash               # save everything
git stash show -p       # review the stash
# Apply only the good hunks manually

# Option 4: Start over (easiest)
git reset --hard HEAD~1  # complete reset to checkpoint (DESTRUCTIVE — use only if sure)
```

---

## 5. The Approval Policy Decision Tree

```
Task type?
  ├── First time running Codex on this codebase
  │   └── Use: suggest
  │
  ├── Auth, payment, security-sensitive code
  │   └── Use: suggest
  │
  ├── Familiar code, small to medium change (1-3 files)
  │   └── Use: auto-edit
  │
  ├── Test generation or documentation
  │   └── Use: auto-edit
  │
  ├── Multi-step scaffold (new module, new endpoint from pattern)
  │   └── Is the task scope clearly bounded?
  │       ├── YES + git checkpoint done → Use: full-auto
  │       └── NO → Narrow the scope first, then full-auto
  │
  └── Codebase-wide refactor
      └── Break into bounded sub-tasks → each sub-task uses auto-edit or full-auto
```

---

## 6. Monitoring a Running Full-Auto Session

```
Even in full-auto, you can watch the output:
  - Codex prints each action it takes
  - Watch for: unexpected file names, unexpected commands
  - If you see a red flag: Ctrl+C to interrupt

What to watch for:
  ⚠ "Modifying src/auth/..."  — did you authorize auth changes?
  ⚠ "Running: alembic upgrade" — database migration, should be in forbidden actions
  ⚠ "Installing: requests==2.31" — new dependency, was this authorized?
  ⚠ "Deleting: tests/test_..." — test deletion is almost always wrong
  ⚠ "Modifying: .env" — environment file should be protected

Interrupt immediately if you see any red flag.
```

---

## 7. Using full-auto in CI Scripts

```bash
#!/bin/bash
# scripts/codex-scaffold.sh — safe CI-friendly full-auto wrapper

set -e  # exit on any error

# Safety check: must be on a feature branch
BRANCH=$(git branch --show-current)
if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ]; then
  echo "ERROR: refusing to run full-auto on main branch"
  exit 1
fi

# Checkpoint
git add -A
git commit -m "checkpoint: pre-codex $(date +%Y%m%d-%H%M)"

# Run the task
codex --approval-policy full-auto --quiet "$1"

# Verify (fail-fast)
pytest -x

# Report
echo "=== Changes made ==="
git diff HEAD~1 --stat
```

---

## Interview Traps

```
TRAP: "suggest mode is too slow — just use full-auto for everything"
TRUTH: Full-auto without bounded scope produces unexpected changes across many files.
       The time cost of suggest mode is seconds per review. The cleanup cost of an
       unbounded full-auto session is hours. Use suggest for exploration and review.

TRAP: "auto-edit is safe because Codex asks before running commands"
TRUTH: auto-edit applies file changes immediately without asking. It only asks before
       running shell commands. You can accept dozens of file changes before seeing a
       command prompt. Always run git diff after every auto-edit session.

TRAP: "I can switch from suggest to full-auto mid-session to save the work done so far"
TRUTH: The work done in suggest mode has no checkpoint. If you switch to full-auto
       mid-session and it goes wrong, you have no clean rollback point for the session work.
       Commit the suggest-mode output first, then start a new full-auto session.
```

---

## Revision Checklist

- [ ] Have used all three approval modes at least once
- [ ] Can explain the risk difference between auto-edit and full-auto
- [ ] Git checkpoint is automatic before every full-auto session
- [ ] Can write a bounded full-auto task prompt (explicit scope, forbidden, verification)
- [ ] Know the 4 recovery options when full-auto produces unexpected changes
- [ ] AGENTS.md forbidden actions protect against the most dangerous operations
