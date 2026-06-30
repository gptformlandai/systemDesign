# Codex CLI Deep Dive — Gold Sheet

> **Track**: Codex Mastery Track — Group 2: Intermediate Power User
> **File**: 3 of 7 (Track File #9)
> **Audience**: Developers who want to master every CLI option and workflow pattern
> **Read after**: Context-Engineering-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| --quiet flag for scripted/CI usage | ★★★★★ | Without it, Codex prompts for input in CI and hangs |
| config.yaml — set-and-forget model and policy | ★★★★★ | Typing --model and --approval-policy every time is wasted effort |
| Wrapping Codex in shell scripts | ★★★★☆ | Repeatable tasks should be scriptable — not retyped every time |
| --system-prompt for role override | ★★★★☆ | Shifting Codex into "security reviewer" or "tech writer" role changes output quality |
| Makefile integration | ★★★☆☆ | make codex-test runs Codex as part of standard development workflow |

---

## ⭐ Beginner Tier — Start Here

### B1: Your first config.yaml (set-and-forget)

```bash
# Create the directory and file
mkdir -p ~/.codex

# Write config
cat > ~/.codex/config.yaml << 'EOF'
model: o4-mini
approval_policy: auto-edit
notify: true
EOF

# Verify it works — no need for flags now
codex "say hello"   # uses o4-mini + auto-edit by default
```

Now you never need to type `--model o4-mini --approval-policy auto-edit` again.

### B2: Your first reusable prompt script

```bash
# Save a common task as a shell script
cat > scripts/codex-review.sh << 'EOF'
#!/bin/bash
# Usage: ./scripts/codex-review.sh src/payments/service.py
FILE=${1:?Usage: $0 <file>}
codex --approval-policy suggest \
  "Review $FILE for: SQL injection, missing input validation, error paths without tests.
   Format: | SEVERITY | ISSUE | LINE | FIX |
   Do not make changes."
EOF
chmod +x scripts/codex-review.sh

# Use it:
./scripts/codex-review.sh src/auth/login.py
./scripts/codex-review.sh src/payments/service.py
```

---

## 1. Complete CLI Reference

```bash
# Positional argument: task (optional — starts interactive if omitted)
codex [OPTIONS] [TASK]

# Core flags
--model MODEL              Use specific model (o4-mini / gpt-4.1 / gpt-4.1-mini)
--approval-policy POLICY   suggest / auto-edit / full-auto
--system-prompt TEXT       Override system prompt for this run
--quiet                    Suppress interactive prompts (for CI/scripting)

# Information flags
--version                  Print version
--help                     Print help
```

---

## 2. The Global Config File

```yaml
# ~/.codex/config.yaml  (Linux/macOS)
# %USERPROFILE%\.codex\config.yaml  (Windows)

# Model to use when --model is not specified
model: o4-mini

# Approval policy when --approval-policy is not specified
approval_policy: auto-edit

# Desktop notification on task completion
notify: true
```

### Overriding the config per-task

```bash
# Config has model: o4-mini — override for architecture work
codex --model gpt-4.1 "design the caching strategy for the search service"

# Config has approval_policy: auto-edit — override to suggest for sensitive code
codex --approval-policy suggest "modify the auth token validation logic"

# Config has approval_policy: auto-edit — override to full-auto for bounded scaffold
git commit -m "checkpoint"
codex --approval-policy full-auto "scaffold the notifications service"
```

---

## 3. Model Selection In Practice

```bash
# o4-mini — your everyday model (90% of tasks)
codex "generate tests for create_order() in src/orders/service.py"
codex "add a docstring to validate_payment()"
codex "fix the failing test: pytest tests/test_auth.py::test_login -v"
codex "add input validation to the create_user endpoint"

# gpt-4.1 — for complex reasoning and planning
codex --model gpt-4.1 "design the event-driven architecture for notifications.
                        Consider: ordering guarantees, retry logic, dead letter queues,
                        and how it integrates with the existing sync service layer."
codex --model gpt-4.1 "review this codebase for architectural anti-patterns"
codex --model gpt-4.1 "plan the migration from monolith to services for the payment module"

# gpt-4.1-mini — for documentation where quality > speed but cost matters
codex --model gpt-4.1-mini "write API documentation for all endpoints in src/api/"
codex --model gpt-4.1-mini "generate a comprehensive README for this project"
```

---

## 4. --system-prompt for Role-Based Sessions

```bash
# Security reviewer role
codex --system-prompt \
  "You are an expert application security engineer.
   Review every code change for OWASP vulnerabilities.
   Always check: SQL injection, auth bypass, missing input validation, PII in logs.
   Report findings in a severity table: CRITICAL/HIGH/MEDIUM/LOW.
   Never approve changes with CRITICAL or HIGH findings without noting them explicitly." \
  "Review src/auth/ for security vulnerabilities"

# Technical writer role
codex --system-prompt \
  "You are a technical writer specializing in developer documentation.
   Write clear, concise documentation for developers who will read this code.
   Use Google-style docstrings for Python. Add practical usage examples.
   Never write documentation that restates the code — explain WHY, not WHAT." \
  "Write documentation for all public functions in src/payments/service.py"

# Code reviewer role
codex --system-prompt \
  "You are a senior engineer conducting a thorough code review.
   Check: correctness, performance, security, maintainability, test coverage.
   Be specific: cite line numbers. Suggest concrete fixes.
   Output: Approved / Approve with Comments / Changes Required." \
  "Review the changes in src/orders/"
```

---

## 5. The --quiet Flag for Scripting

```bash
# Without --quiet: Codex may prompt for confirmation → CI hangs
# With --quiet: non-interactive, uses config defaults

# CI-safe usage
codex --quiet --approval-policy auto-edit \
  "Add docstrings to all undocumented public functions in src/"

# In a Makefile (always use --quiet for make targets)
codex-docs:
	codex --quiet --approval-policy auto-edit \
	  "Write docstrings for functions missing them in $(FILE)"

codex-review:
	codex --quiet --approval-policy suggest \
	  "Review $(FILE) for security issues. Output: table."
```

---

## 6. Shell Script Patterns for Reusable Workflows

### Review script

```bash
#!/bin/bash
# scripts/codex-security-review.sh
# Usage: ./scripts/codex-security-review.sh <file>

FILE=${1:?Usage: $0 <filepath>}

if [ ! -f "$FILE" ]; then
  echo "Error: file not found: $FILE"
  exit 1
fi

codex --approval-policy suggest \
  "Security review of $FILE.
   Check: SQL injection, auth bypass, input validation, sensitive data in logs,
   insecure deserialization, IDOR, missing rate limiting.
   Format: | SEVERITY | ISSUE | LINE | FIX |
   OWASP category for each finding.
   Do NOT make changes — review only."
```

### Test generation script

```bash
#!/bin/bash
# scripts/codex-tests.sh
# Usage: ./scripts/codex-tests.sh <file>

FILE=${1:?Usage: $0 <filepath>}
TEST_CMD="${2:-pytest -x}"

codex --approval-policy auto-edit \
  "Generate tests for $FILE.
   Cover: happy path, all error cases, boundary values (empty, zero, null, max).
   Mock: external dependencies only (HTTP, DB, filesystem).
   Naming: test_[function]_[scenario]_[expected_result]
   Run $TEST_CMD after generating. Fix failures before completing."
```

### Pre-commit review script

```bash
#!/bin/bash
# scripts/codex-precommit.sh
# Run before every git commit

CHANGED=$(git diff --staged --name-only | grep "\.py$")

if [ -z "$CHANGED" ]; then
  echo "No Python files staged."
  exit 0
fi

echo "Running Codex review on: $CHANGED"

for FILE in $CHANGED; do
  codex --approval-policy suggest \
    "Quick review of staged changes in $FILE:
     1. Any security issues (SQL injection, secrets in code, missing auth)?
     2. Any error paths not tested?
     3. Any convention violations vs AGENTS.md?
     Be brief — 3-5 bullet points maximum."
done
```

---

## 7. Makefile Integration

```makefile
# Makefile additions for Codex workflows

# Review a specific file: make codex-review FILE=src/auth/login.py
codex-review:
	@test -n "$(FILE)" || (echo "Usage: make codex-review FILE=<path>"; exit 1)
	codex --quiet --approval-policy suggest \
	  "Review $(FILE) for security issues and missing tests. Output table."

# Generate tests: make codex-test FILE=src/payments/service.py
codex-test:
	@test -n "$(FILE)" || (echo "Usage: make codex-test FILE=<path>"; exit 1)
	codex --quiet --approval-policy auto-edit \
	  "Generate tests for $(FILE). Run: pytest -x"

# Full pre-commit review: make codex-precommit
codex-precommit:
	@CHANGED=$$(git diff --staged --name-only); \
	codex --quiet --approval-policy suggest \
	  "Review staged changes (files: $$CHANGED) for: security issues, missing tests, convention violations"

# Generate docstrings: make codex-docs FILE=src/orders/service.py
codex-docs:
	@test -n "$(FILE)" || (echo "Usage: make codex-docs FILE=<path>"; exit 1)
	codex --quiet --approval-policy auto-edit \
	  "Add Google-style docstrings to all public functions in $(FILE). Run: pytest -x"
```

---

## Interview Traps

```
TRAP: "I always use gpt-4.1 for the best results"
TRUTH: o4-mini handles 90% of tasks at 5-10x lower cost. gpt-4.1 is for architecture
       and complex multi-file planning. Using gpt-4.1 for test generation is wasteful.

TRAP: "I run Codex interactively in my CI pipeline"
TRUTH: CI needs --quiet mode. Interactive Codex in CI hangs waiting for user input
       that never comes. Always --quiet --approval-policy auto-edit (or full-auto) in CI.

TRAP: "I retype the same review prompt every day"
TRUTH: Save it as a shell script. ./scripts/codex-review.sh <file> is faster and consistent.
       Reusable scripts are a sign of a mature Codex workflow.
```

---

## Revision Checklist

- [ ] config.yaml created with model and approval_policy defaults
- [ ] Can use --model, --approval-policy, --system-prompt, --quiet flags
- [ ] Know when to use each model (o4-mini vs gpt-4.1 vs gpt-4.1-mini)
- [ ] Have at least 3 reusable shell scripts for common Codex tasks
- [ ] --quiet used for all scripted/CI usage
- [ ] Makefile has at least one Codex target
