# Tool Use & Shell Integration — Gold Sheet

> **Track**: Codex Mastery Track — Group 3: Advanced Engineering
> **File**: 5 of 7 (Track File #18)
> **Audience**: Developers who want Codex integrated into their development toolchain
> **Read after**: Token-Context-Optimization-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| Shell command integration — piping tool output to Codex | ★★★★★ | Feeding real output instead of describing problems = much better diagnosis |
| Shell script wrappers for daily workflows | ★★★★★ | One-command access to multi-step prompts = the scripts you run every day |
| Makefile integration | ★★★★☆ | `make review` and `make test-gen` as first-class development commands |
| CI pipeline integration with --quiet | ★★★★☆ | Codex as a pipeline step, not just interactive tool |
| Composing multiple Codex calls in sequence | ★★★☆☆ | Plan → implement → review as a single script execution |

---

## ⭐ Beginner Tier — Start Here

### B1: Pipe your first test failure into Codex

```bash
# Run your failing tests and capture the output
FAILURE=$(pytest tests/test_orders.py -x 2>&1 | tail -40)

# Feed the real output directly into Codex
codex --approval-policy auto-edit \
  "Fix this pytest failure. Do not modify test files.
$FAILURE"
```

This works better than: "My test is failing, it says something about OrderNotFound."
Real failure output = Codex sees exactly what you see.

### B2: Create your first script wrapper

```bash
# Create the script (30 seconds)
mkdir -p ~/.codex-scripts
cat > ~/.codex-scripts/codex-fix << 'SCRIPT'
#!/bin/bash
FAILURES=$(pytest "$@" 2>&1)
git add -A && git commit -m "checkpoint: before codex-fix"
codex --approval-policy auto-edit "Fix failures (no test file changes):
$FAILURES"
SCRIPT
chmod +x ~/.codex-scripts/codex-fix
export PATH="$HOME/.codex-scripts:$PATH"

# Use it:
codex-fix tests/test_orders.py -x
```

You just turned a 3-step workflow into one command. Build 2 more scripts this week.

---

## 1. Piping Real Output Into Codex

```bash
# The pattern: capture tool output, feed to Codex in prompt
# More accurate than describing what the output said

# Pipe: pytest failure
FAILURE=$(pytest tests/ -x 2>&1 | tail -50)
codex "Fix this pytest failure:
$FAILURE
Fix implementation only. Do not modify test files."

# Pipe: lint errors
LINT_ERRORS=$(ruff check src/ 2>&1)
codex "Fix these ruff lint errors:
$LINT_ERRORS
In files: $(ruff check src/ --format json | python3 -c 'import json,sys; files=list(set(i["filename"] for i in json.load(sys.stdin))); print(", ".join(files))')"

# Pipe: git diff for review
DIFF=$(git diff --staged)
codex "Code review for this diff. Check: security, correctness, tests:
$DIFF"

# Pipe: compilation error
BUILD=$(go build ./... 2>&1)
codex "Fix this Go build error:
$BUILD"
```

---

## 2. Shell Script Library

Save these scripts and invoke with short commands.

```bash
# ~/.codex-scripts/codex-review
#!/bin/bash
STAGED=$(git diff --staged)
if [ -z "$STAGED" ]; then
    echo "No staged files. Run: git add <files>"
    exit 1
fi
codex --approval-policy suggest \
  "Pre-commit security review. Check: SQL injection, missing auth, PII in logs, missing tests for new branches.
   Format: | SEVERITY | ISSUE | FILE:LINE | FIX |
   Final: APPROVED / CHANGES REQUIRED
$STAGED"

# Usage: codex-review
```

```bash
# ~/.codex-scripts/codex-fix-tests
#!/bin/bash
FAILURES=$(pytest "$@" 2>&1)
if echo "$FAILURES" | grep -q "passed" && ! echo "$FAILURES" | grep -q "failed"; then
    echo "Tests already passing."
    exit 0
fi
git add -A && git commit -m "checkpoint: before codex fix"
codex --approval-policy auto-edit \
  "Fix these pytest failures. Do not modify test files.
$FAILURES"

# Usage: codex-fix-tests tests/test_orders.py -x
```

```bash
# ~/.codex-scripts/codex-docstring
#!/bin/bash
FILE="$1"
if [ -z "$FILE" ]; then
    echo "Usage: codex-docstring <file>"
    exit 1
fi
codex --model gpt-4.1-mini --approval-policy auto-edit \
  "Add Google-style docstrings to all public functions in $FILE.
   Only document what is verifiably in the code.
   Do not invent examples — use real parameter names.
   Verification: python3 -c 'import ast; ast.parse(open(\"$FILE\").read()); print(\"Parse OK\")'"

# Usage: codex-docstring src/payments/service.py
```

---

## 3. Makefile Integration

```makefile
# Makefile — add to your project root

.PHONY: review fix-tests docstring precommit plan

## codex-review: security review of staged changes
review:
	@STAGED=$$(git diff --staged); \
	codex --approval-policy suggest \
	  "Security review for: $$STAGED. Check: SQL injection, auth bypass, PII leaks. Table: SEVERITY|ISSUE|FIX. Verdict: APPROVED/CHANGES REQUIRED."

## codex-fix: fix failing tests (usage: make fix TESTS=tests/test_orders.py)
fix:
	@FAILURES=$$(pytest $(TESTS) 2>&1); \
	git add -A && git commit -m "checkpoint: before codex fix"; \
	codex --approval-policy auto-edit "Fix these failures (no test file modifications): $$FAILURES"

## codex-plan: plan implementation (usage: make plan TASK="add pagination to GET /orders")
plan:
	@codex --model gpt-4.1 --approval-policy suggest \
	  "Plan only (no implementation): $(TASK). Output: files, approach, test list, risks."

## codex-docstring: add docstrings to a file (usage: make docstring FILE=src/foo.py)
docstring:
	@codex --model gpt-4.1-mini --approval-policy auto-edit \
	  "Add Google-style docstrings to all public functions in $(FILE). No invented content."
```

---

## 4. CI Pipeline Integration

```yaml
# GitHub Actions — Codex as a pipeline step
name: Codex Code Quality Gate
on: [pull_request]

jobs:
  codex-review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 2
      
      - name: Install Codex
        run: npm install -g @openai/codex
      
      - name: Capture diff
        run: |
          git diff HEAD~1 HEAD -- '*.py' > /tmp/pr_diff.txt
          echo "Changed lines: $(wc -l < /tmp/pr_diff.txt)"
      
      - name: Codex Security Review
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        run: |
          DIFF=$(cat /tmp/pr_diff.txt)
          codex --quiet --approval-policy suggest \
            "Review this Python diff. If CRITICAL or HIGH severity findings exist: exit 1.
             Output only CRITICAL and HIGH findings in this format: SEVERITY|ISSUE|FIX
             If no critical/high findings: output only: APPROVED and exit 0
             Diff: $DIFF" || exit 1
```

---

## 5. Composing Multi-Step Workflows

```bash
#!/bin/bash
# codex-feature-pipeline: plan → implement → test → review
FEATURE="$1"
if [ -z "$FEATURE" ]; then echo "Usage: codex-feature-pipeline 'feature description'"; exit 1; fi

set -e  # stop on any error

echo "=== Phase 1: Plan ==="
PLAN=$(codex --quiet --approval-policy suggest \
  "Plan: $FEATURE. Output: files, approach, test list. No implementation.")

echo "$PLAN"
read -p "Approve plan? (y/n): " OK
if [ "$OK" != "y" ]; then echo "Aborted."; exit 1; fi

echo "=== Phase 2: Checkpoint ==="
git add -A && git commit -m "checkpoint: before $FEATURE"

echo "=== Phase 3: Implement ==="
codex --approval-policy auto-edit \
  "Implement: $FEATURE
   Plan: $PLAN
   Verification: pytest -x"

echo "=== Phase 4: Review ==="
DIFF=$(git diff HEAD~1)
codex --approval-policy suggest \
  "Review the implementation: $DIFF
   Focus: security, correctness, test coverage
   Verdict: APPROVED / CHANGES REQUIRED"
```

---

## Interview Traps

```
TRAP: "Codex figures out the right shell syntax for my environment automatically"
TRUTH: Codex doesn't know whether you're on macOS, Linux, or Windows. Shell commands
       that work on one may fail on another. Specify in AGENTS.md: "Use bash syntax"
       or "Use PowerShell cmdlets." Otherwise get environment-specific failures.

TRAP: "Adding Codex to CI will make the pipeline slow and expensive"
TRUTH: Using --quiet mode with suggest policy, Codex CI steps add ~15-30 seconds and
       ~$0.01-0.05 per run for typical PR diffs. For catching CRITICAL security findings
       before merge, this cost/benefit ratio is overwhelmingly favorable.

TRAP: "Shell script wrappers add maintenance burden — just type the prompt each time"
TRUTH: Prompts typed from memory vary in quality each run. A shell script makes the
       prompt reproducible and improvable over time. The 20-minute investment to create
       codex-review pays back every week you use it.
```

---

## Revision Checklist

- [ ] Can pipe pytest failure output directly into a Codex prompt
- [ ] Have at least 3 shell script wrappers for daily tasks (review, fix, docstring)
- [ ] Makefile has Codex targets for common operations
- [ ] Can explain how to use --quiet for CI pipeline integration
- [ ] Know when to compose multi-step Codex pipelines vs single prompts
