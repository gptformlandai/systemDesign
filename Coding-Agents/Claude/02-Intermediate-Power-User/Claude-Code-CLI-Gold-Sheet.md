# Claude Code CLI — Mastery Gold Sheet

> **Track**: Claude Mastery Track — Group 2: Intermediate Power User
> **File**: 4 of 7 (Track File #10)
> **Audience**: Developers using Claude Code for real agentic workflows
> **Read after**: Context-Engineering-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| How Claude reads the repo | ★★★★★ | Devs don't understand what Claude sees — over- or under-scoping context |
| File editing mechanics — how Claude changes files | ★★★★★ | Understanding the edit loop prevents blind acceptance |
| Command execution — what Claude can run | ★★★★★ | Most devs don't know Claude can run tests, lint, and build |
| Safety flags — controlling autonomy level | ★★★★★ | Default settings may be too autonomous for some workflows |
| Multi-file project patterns | ★★★★★ | The real power: Claude working across many files in one session |
| Debugging Claude Code behavior | ★★★★☆ | When Claude does the wrong thing, knowing HOW it works helps you fix it |

---

## 2. How Claude Reads Your Repository

### What Claude Sees When You Run `claude`

```
When you run claude in a project directory, Claude:

1. Reads CLAUDE.md at the project root (if it exists)
2. Notes the current directory structure (one level by default)
3. Reads files you explicitly reference with @file:path
4. Reads files IT DECIDES to read while working on a task
   (e.g., imports it follows, tests it runs to verify)
5. Receives any output you paste or that commands produce

What Claude does NOT automatically see:
  - Every file in the project (it reads what it needs)
  - Hidden files starting with . (unless explicitly referenced)
  - Your environment variables (never paste .env content)
  - Binary files, images (unless vision-enabled task)
```

### How Claude Navigates a Codebase

```
When given a task, Claude typically:

1. Reads CLAUDE.md (project rules)
2. Reads files you explicitly reference
3. Follows imports to understand dependencies
4. Reads related files (tests, interfaces, schemas)
5. Makes changes based on patterns it found
6. Runs verification (tests, lint) to confirm

To guide navigation:
  "Start with @file:src/services/user_service.py
  Then read its test file and the UserRepository it depends on."

To limit navigation:
  "Only read @file:src/api/auth.py. Do not read any other files."
```

---

## 3. File Editing Mechanics

### How Claude Makes Changes

```
Claude Code operates in a propose-then-execute loop:

1. Claude proposes a change (shows you the diff)
2. By default with --auto-accept=false:
   → You see the diff and confirm/reject
3. With --auto-accept=true (or in agent mode):
   → Claude applies changes automatically
4. Claude runs verification after changes (tests, lint if available)
5. If verification fails: Claude iterates (fixes the failure, tries again)

The diff format:
  - Lines removed: shown with -
  - Lines added: shown with +
  - Context: unchanged lines for reference

Always read the diff before accepting.
The most dangerous Claude output is the one that looks right but isn't.
```

### Controlling Auto-Accept

```bash
# See all changes, confirm each one:
claude --no-auto-accept "Refactor the payment service"

# Let Claude apply all changes (agent mode — requires checkpoint commit first):
claude "Build the complete user notification feature"

# Apply changes but pause for dangerous operations:
claude --dangerous-commands=ask "Deploy the new feature"
```

---

## 4. Command Execution

### What Claude Can Run

```
Claude Code can execute shell commands to:
  - Run tests: pytest, jest, go test, mvn test
  - Run linting: ruff, eslint, golangci-lint
  - Run type checking: mypy, pyright, tsc
  - Run builds: npm run build, cargo build
  - Run migrations: alembic upgrade head (flag first!)
  - Query databases (read-only queries for debugging)
  - Git operations: git diff, git log, git add, git commit

The verification loop (most powerful pattern):
  Claude makes a change → runs pytest → reads output →
  if failures: fixes them → runs pytest again → repeats until all pass

This loop runs autonomously — you don't need to manually run tests.
```

### Dangerous Command Protection

```bash
# Configure which commands require confirmation:
cat > .claude/hooks/pre_tool_use.sh << 'EOF'
#!/bin/bash
COMMAND="$@"

# Block irrecoverable operations
DANGEROUS_PATTERNS=(
  "rm -rf"
  "DROP TABLE"
  "DROP DATABASE"
  "git push --force"
  "git reset --hard"
  "kubectl delete"
  "terraform destroy"
)

for pattern in "${DANGEROUS_PATTERNS[@]}"; do
  if [[ "$COMMAND" == *"$pattern"* ]]; then
    echo "BLOCKED: Potentially destructive command: $COMMAND"
    echo "Run manually if intended."
    exit 1
  fi
done
exit 0
EOF
chmod +x .claude/hooks/pre_tool_use.sh
```

---

## 5. Multi-File Project Patterns

### Pattern 1 — Feature Scaffold

```
"Build the user notification preferences feature.

Context: this follows our layered pattern (router → service → repository → schema).
Example to follow: @file:src/api/users.py, @file:src/services/user_service.py

Task:
  Create: src/api/notification_preferences.py (router)
  Create: src/services/notification_preferences_service.py (service)
  Create: src/repositories/notification_repo.py (repository)
  Create: src/schemas/notification.py (Pydantic schemas)
  Create: tests/unit/test_notification_service.py (tests)

Do NOT modify any existing files.
Plan first — list each file and its purpose. Wait for approval."
```

### Pattern 2 — Codebase Refactoring

```
"Refactor the payment module to use the repository pattern.

Context:
  Current: payment logic and DB queries in PaymentService
  Target: PaymentRepository handles all DB, PaymentService handles logic

Step 1 (this session):
  Extract all session.execute() calls from @file:src/services/payment_service.py
  into new file: src/repositories/payment_repository.py
  Do NOT change PaymentService yet.
  Run: pytest tests/unit/test_payment_service.py after each change."
```

### Pattern 3 — Test Generation Sweep

```
"Generate unit tests for all public methods in @file:src/services/order_service.py.

Framework: pytest + pytest-asyncio
Mock: AsyncMock(spec=AsyncSession) for all DB, AsyncMock for EmailService
Name: test_<method>_<scenario>_<expected>
Cover: happy path, error case, edge case for each method

After generating: run tests. Fix any that fail.
Target: all tests pass before finishing."
```

---

## 6. Debugging Claude Code Behavior

### When Claude Does the Wrong Thing

```
Problem: Claude edited the wrong files.
Fix:
  git checkout .   (restore from checkpoint)
  "Next time, only modify files I explicitly list:
  Only touch: [file1, file2]. Do NOT touch any other file."
  Add this as a CLAUDE.md "Do NOT" rule if it keeps happening.

Problem: Claude is not following CLAUDE.md rules.
Diagnose:
  claude "What project rules do you have loaded?"
  → If generic answer: CLAUDE.md not loading
  → Fix: verify CLAUDE.md is at project root, reload session

Problem: Claude over-engineered the solution.
Fix:
  "Stop. Remove the [abstraction/class/pattern] you added.
  The simpler solution is sufficient: [describe what you actually want]."
  Add to CLAUDE.md: "Do NOT add [specific abstraction] — use direct approach."

Problem: Tests passing but wrong behavior.
Root cause: Claude changed tests to match wrong implementation.
Fix:
  "Do NOT modify any test file. Tests define correct behavior.
  If a test fails, fix the implementation, not the test."
  Add to CLAUDE.md: "Never modify test files to make tests pass."

Problem: Claude runs migrations automatically.
Fix:
  Add to CLAUDE.md and infra/CLAUDE.md:
  "Never run alembic upgrade or database migrations automatically.
  Flag that migrations are needed and stop."
```

---

## 7. Essential Claude Code CLI Reference

```bash
# Basic interactive session
claude

# One-shot task
claude "Generate tests for src/services/user_service.py"

# Print mode (output only, no interaction)
claude --print "Summarize what changed in the last 3 commits"

# Specify model
claude --model claude-opus-4-5 "Design the database schema"

# Resume last session
claude --continue

# Non-interactive (for CI/scripts)
claude --no-interactive "Run linting and fix any errors"

# Run a slash command directly
claude /plan implement user authentication

# Show what Claude would do without executing
claude --dry-run "Refactor the payment module"

# Config management
claude config set model claude-sonnet-4-5
claude config set auto-accept false
claude config list
```

---

## 8. Revision Checklist

- [ ] Understands what Claude sees when it starts a session
- [ ] Knows how to guide Claude's file navigation with explicit references
- [ ] Understands the propose-then-execute edit loop
- [ ] Has pre_tool_use.sh hook blocking dangerous commands
- [ ] Can run multi-file features using the scaffold and refactor patterns
- [ ] Knows how to debug wrong file edits, CLAUDE.md not loading, over-engineering
- [ ] Has the verification loop pattern established (generate → run tests → iterate)
- [ ] Knows essential CLI flags: --print, --no-auto-accept, --model, --continue
