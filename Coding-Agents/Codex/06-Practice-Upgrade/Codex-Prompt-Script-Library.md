# Codex Prompt Script Library

> **Track**: Codex Mastery Track — Group 6: Practice & Upgrade
> **File**: 2 of 6 (Track File #31)
> **Audience**: All levels — copy, customize, and reuse these scripts
> **How to use**: Save as shell scripts; source into your shell profile; call with short commands

---

## Setup

```bash
# Add to ~/.bashrc or ~/.zshrc
export CODEX_SCRIPTS="$HOME/.codex-scripts"
mkdir -p "$CODEX_SCRIPTS"
PATH="$CODEX_SCRIPTS:$PATH"
```

---

## Category 1 — Daily Planning & Review

### `codex-plan` — Morning planning for the day

```bash
#!/bin/bash
# ~/.codex-scripts/codex-plan
codex --model gpt-4.1 --approval-policy suggest \
  "Morning planning:
   Tasks today: ${1:-'[edit: paste your tasks here]'}
   
   For each task:
   1. Suggested order (dependencies? risk? blocking others?)
   2. Files involved
   3. Verification command
   4. Estimated time
   
   Final: ordered task list with justification.
   Do not implement anything."
# Usage: codex-plan "1. Add pagination to GET /orders  2. Fix login test  3. Add docstrings"
```

### `codex-eod` — End-of-day capture

```bash
#!/bin/bash
# ~/.codex-scripts/codex-eod
codex --model gpt-4.1-mini --approval-policy suggest \
  "End-of-day notes from my session today.
   I worked on: ${1:-'[describe work]'}
   
   Generate:
   1. Key wins
   2. Anything that failed or blocked
   3. AGENTS.md update suggestion (if any)
   4. Prompt to reuse tomorrow
   5. First task tomorrow and context needed
   
   Under 200 words total."
# Usage: codex-eod "added pagination to orders endpoint, debugged CI failure"
```

---

## Category 2 — Code Review & Security

### `codex-review` — Security review of staged changes

```bash
#!/bin/bash
# ~/.codex-scripts/codex-review
STAGED=$(git diff --staged)
if [ -z "$STAGED" ]; then
    echo "No staged files. Run: git add <files> first."
    exit 1
fi
codex --approval-policy suggest \
  "Security review of staged changes.
   Check: SQL injection, missing auth, PII in logs, missing tests for new code paths.
   Format: | SEVERITY | ISSUE | FILE:LINE | FIX |
   Final: APPROVED / CHANGES REQUIRED
   
   Diff:
$STAGED"
# Usage: git add src/; codex-review
```

### `codex-pr-review` — Full PR review

```bash
#!/bin/bash
# ~/.codex-scripts/codex-pr-review
BASE="${1:-main}"
DIFF=$(git diff "$BASE"..HEAD)
codex --model gpt-4.1 --approval-policy suggest \
  "Full code review vs $BASE.
   Check: security, correctness, test coverage, architecture, AGENTS.md compliance.
   Format: | SEVERITY | ISSUE | FILE:LINE | FIX |
   Final: APPROVED / APPROVE WITH COMMENTS / CHANGES REQUIRED
   
   Diff:
$DIFF"
# Usage: codex-pr-review main
```

### `codex-security` — Security role review of a file

```bash
#!/bin/bash
# ~/.codex-scripts/codex-security
FILE="$1"
if [ -z "$FILE" ]; then echo "Usage: codex-security <file>"; exit 1; fi
codex --system-prompt \
  "You are a senior application security engineer. Every finding: SEVERITY (CRITICAL/HIGH/MEDIUM/LOW), ATTACK VECTOR, FIX, OWASP category. Final: APPROVED / CHANGES REQUIRED." \
  --approval-policy suggest \
  "Security review of $FILE.
   Check: SQL injection, auth bypass, PII exposure, input validation, error message leakage."
# Usage: codex-security src/auth/service.py
```

---

## Category 3 — Testing

### `codex-fix-tests` — Fix failing tests

```bash
#!/bin/bash
# ~/.codex-scripts/codex-fix-tests
FAILURES=$(pytest "$@" 2>&1)
PASS=$(echo "$FAILURES" | grep -c "passed" || true)
FAIL=$(echo "$FAILURES" | grep -c "failed" || true)
if [ "$FAIL" = "0" ]; then echo "Tests already passing."; exit 0; fi
echo "Failures found. Creating checkpoint..."
git add -A && git commit -m "checkpoint: before codex fix-tests"
codex --approval-policy auto-edit \
  "Fix these pytest failures. Do not modify test files. Fix implementation only.
   
   Failures:
$FAILURES"
# Usage: codex-fix-tests tests/test_orders.py -x
```

### `codex-gen-tests` — Generate tests for a file

```bash
#!/bin/bash
# ~/.codex-scripts/codex-gen-tests
FILE="$1"
if [ -z "$FILE" ]; then echo "Usage: codex-gen-tests <source-file>"; exit 1; fi
TEST_FILE="${FILE/src/tests}"
TEST_FILE="${TEST_FILE%.py}.test.py"
codex --approval-policy auto-edit \
  "Generate tests for $FILE.
   Test file: $TEST_FILE (create if it doesn't exist, append if it does)
   
   Cover: every public function, happy path + all error paths + edge cases (None, empty, max).
   Only mock external dependencies (HTTP, DB driver). Do not mock own code.
   Verification: pytest $TEST_FILE -v"
# Usage: codex-gen-tests src/orders/service.py
```

### `codex-test-gaps` — Find missing test coverage

```bash
#!/bin/bash
# ~/.codex-scripts/codex-test-gaps
FILE="$1"
if [ -z "$FILE" ]; then echo "Usage: codex-test-gaps <source-file>"; exit 1; fi
codex --approval-policy suggest \
  "Test gap analysis for $FILE.
   
   For each public function:
   1. What branches/paths are NOT tested?
   2. What error conditions are NOT tested?
   3. What edge cases are missing?
   
   Priority: highest-risk missing tests first.
   Do not generate tests — analysis only."
# Usage: codex-test-gaps src/payments/service.py
```

---

## Category 4 — Documentation

### `codex-docstring` — Add docstrings to a file

```bash
#!/bin/bash
# ~/.codex-scripts/codex-docstring
FILE="$1"
if [ -z "$FILE" ]; then echo "Usage: codex-docstring <file>"; exit 1; fi
EXT="${FILE##*.}"
STYLE="Google-style Python" && [ "$EXT" = "ts" ] && STYLE="JSDoc TypeScript"
codex --model gpt-4.1-mini --approval-policy auto-edit \
  "Add $STYLE docstrings to all public functions in $FILE.
   Only document what is verifiably in the code.
   Do not invent examples or behavior.
   Verification: python3 -c 'import ast; ast.parse(open(\"$FILE\").read()); print(\"OK\")'"
# Usage: codex-docstring src/services/user_service.py
```

### `codex-readme` — Generate or update README

```bash
#!/bin/bash
# ~/.codex-scripts/codex-readme
codex --model gpt-4.1-mini --approval-policy auto-edit \
  "Generate README.md for this project.
   Base content on: AGENTS.md, package.json or requirements.txt, src/ folder structure.
   Sections: What it does, Quick start, Configuration, API endpoints (if any), Testing.
   Do not invent features. If unsure about something: write [verify with team].
   Keep under 300 lines."
# Usage: codex-readme (run from project root)
```

---

## Category 5 — Debugging

### `codex-debug` — Debug an error

```bash
#!/bin/bash
# ~/.codex-scripts/codex-debug
ERROR="$1"
if [ -z "$ERROR" ]; then echo "Usage: codex-debug 'paste error here'"; exit 1; fi
codex --approval-policy suggest \
  "Debug this error:
$ERROR
   
   Diagnose: root cause (WHY) → minimum fix → test that would have caught it.
   Do not make changes yet — diagnosis only."
# Usage: codex-debug "$(pytest tests/test_orders.py 2>&1 | tail -20)"
```

### `codex-ci` — Debug a CI failure

```bash
#!/bin/bash
# ~/.codex-scripts/codex-ci
CI_OUTPUT="$1"
codex --approval-policy suggest \
  "CI is failing but local tests pass.
   
   CI output:
$CI_OUTPUT
   
   Check: missing env vars, timezone (UTC in CI vs local), file case sensitivity,
   test ordering, Docker environment differences.
   
   Diagnose root cause. Propose minimum fix."
# Usage: codex-ci "$(cat /tmp/ci_failure.txt)"
```

---

## Category 6 — Full-Auto Sessions

### `codex-auto` — Safe full-auto with checkpoint

```bash
#!/bin/bash
# ~/.codex-scripts/codex-auto
TASK="$1"
if [ -z "$TASK" ]; then echo "Usage: codex-auto 'task description'"; exit 1; fi
echo "Creating checkpoint..."
git add -A && git commit -m "checkpoint: before codex-auto: $TASK"
echo "Starting full-auto session..."
codex --approval-policy full-auto "$TASK"
echo ""
echo "=== Review Changes ==="
git diff HEAD~1 --stat
echo "Run 'git diff HEAD~1' to see full diff."
echo "Run 'git reset --hard HEAD~1' to roll back if needed."
# Usage: codex-auto "add pagination to GET /orders following GET /users pattern"
```

---

## Sourcing All Scripts at Once

```bash
# Run once to set execute permissions
chmod +x ~/.codex-scripts/*

# Verify
ls -la ~/.codex-scripts/

# Test
codex-plan "test script"
```
