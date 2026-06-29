# Copilot For CI — GitHub Actions — Gold Sheet

> **Track**: Copilot Mastery Track — Group 2: Intermediate Power User
> **File**: 6 of 7 (Track File #12)
> **Audience**: Developers using Copilot to create and debug CI/CD pipelines
> **Read after**: Copilot-For-Testing-Gold-Sheet.md

---

## 1. Practical Impact Meter

| Topic | Impact | Why Developers Struggle Here |
|---|---|---|
| Workflow generation prompt — the right constraints | ★★★★★ | Generated YAML with @latest or unpinned actions is insecure |
| Debugging broken workflows with Copilot | ★★★★★ | "Why is my workflow failing" without context gets generic answers |
| PR quality gate pattern | ★★★★★ | Teams without quality gates merge broken code — Copilot makes them easy to add |
| Caching strategy for faster CI | ★★★★☆ | Uncached workflows are slow; Copilot knows the right cache keys |
| Secrets and env variable handling | ★★★★★ | Hardcoded values in workflows = security incident |
| Concurrency groups to cancel stale PR runs | ★★★★☆ | Without this, every commit triggers a new run, wasting CI minutes |
| GitHub Actions instruction file | ★★★★☆ | Without it, Copilot generates workflows with wrong patterns for your org |

---

## 2. GitHub Actions Instruction File

Create this so Copilot always generates correct workflows for your setup:

```markdown
---
applyTo: ".github/workflows/**"
---
# GitHub Actions Rules

## Action Versions
Always pin action versions to full SHA or at minimum major+minor tag.
Use: actions/checkout@v4 (not @main, @latest, or @master)
Use: actions/setup-python@v5 (not @v3 or @latest)

## Secrets Handling
Never hardcode credentials, tokens, or API keys in workflow files.
Use: ${{ secrets.SECRET_NAME }} for all sensitive values.
Use: ${{ vars.VAR_NAME }} for non-sensitive configuration variables.

## Runner
Default: ubuntu-latest unless specific OS testing is required.

## Python
Default Python version: 3.12 unless specified otherwise.
Dependency installation: use pip with requirements files or Poetry.
Always cache pip dependencies.

## Concurrency
Always add concurrency group to cancel stale runs on PR pushes:
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

## Step Names
Use descriptive step names. Never: "Run step", "Execute command".
Good: "Install Python dependencies", "Run ruff linting", "Execute pytest"

## Required checks
CI must include: lint → test → build (in that order).
Fail fast: true (stop on first failure in matrix jobs).
```

---

## 3. Workflow Generation Prompts

### Standard CI Workflow

```
"Generate a GitHub Actions CI workflow for a Python FastAPI project.

Requirements:
- Trigger: push to main and develop, pull_request targeting main
- Runner: ubuntu-latest
- Python: 3.12
- Steps in order:
  1. Checkout code (actions/checkout@v4)
  2. Set up Python (actions/setup-python@v5)
  3. Cache pip dependencies (key on requirements hash)
  4. Install dependencies (pip install -r requirements.txt -r requirements-dev.txt)
  5. Run ruff linting (fail on any error)
  6. Run mypy type checking
  7. Run pytest with coverage (fail if < 80%)
  8. Upload coverage report as artifact

Rules:
- Pin all action versions to v4/v5 tags (not @latest or @main)
- Add concurrency group to cancel stale PR runs
- Use descriptive step names
- Store PYTHONPATH in environment
- Secrets: DATABASE_URL from ${{ secrets.TEST_DATABASE_URL }}
- Do NOT use shell: bash everywhere — only where needed

Output: complete workflow YAML file"
```

### PR Quality Gate Workflow

```
"Generate a GitHub Actions PR quality gate workflow.

Purpose: Block PR merging if any of these checks fail.

Checks (run in parallel jobs where possible):
- lint: ruff check . && ruff format --check .
- type-check: mypy src/ --strict
- unit-tests: pytest tests/unit/ -v --tb=short
- integration-tests: pytest tests/integration/ -v (only if DB secrets available)
- security-scan: pip audit (fail on HIGH or CRITICAL vulnerabilities)

Requirements:
- Required status checks: lint, type-check, unit-tests must all pass
- Integration tests: only run if ${{ secrets.TEST_DATABASE_URL }} is set
- PR comment: post a summary of which checks passed/failed
- Concurrency: cancel previous run when new commit pushed to same PR
- Timeout: each job maximum 10 minutes

Actions: use github-script for PR comment posting.
Pin all action versions."
```

---

## 4. Debugging Broken Workflows

### The Debugging Prompt Pattern

```
Provide all three pieces of information for best results:

1. The broken workflow YAML (paste the relevant section)
2. The exact error from the GitHub Actions log
3. What the workflow should accomplish

Prompt:
"This GitHub Actions workflow is failing. Help me diagnose and fix it.

Workflow section that is failing:
[paste the relevant jobs/steps section]

Error from the Actions log:
[paste the exact error message — not a description, the actual error]

What this workflow should do:
[1-2 sentence description of the expected behavior]

Diagnose: root cause of the failure and the fix."
```

### Common Failure Patterns and Copilot Prompts

```
Pattern 1 — Step fails silently with exit code 0 but wrong output:
"My lint step passes (exit code 0) but doesn't actually fail on lint errors.
The ruff command is: [show command]
How do I make the step fail if ruff finds any issues?"

Pattern 2 — Cache miss every run:
"My pip cache step never hits the cache. Cache key: [show key].
How do I write a cache key that invalidates when requirements.txt changes
but hits the cache for the same requirements?"

Pattern 3 — Environment variable not available in step:
"I set an env var in one step but it's not available in the next step.
How do I pass values between steps in GitHub Actions?"

Pattern 4 — Matrix job fails but other matrix jobs continue:
"I want my matrix job to fail the entire workflow as soon as one matrix entry fails.
Currently all matrix entries run even after one fails. How do I change this?"
```

---

## 5. Secret Scanning Workflow

```yaml
# .github/workflows/secret-scan.yml
# Generated with Copilot — review before committing

name: Secret Scan

on:
  push:
    branches: [main, develop]
  pull_request:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  gitleaks:
    name: Detect Secrets with Gitleaks
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history needed for gitleaks scan

      - name: Run Gitleaks
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 6. Copilot For GitHub Actions — Best Practices

### What to Always Ask Copilot to Include

```
When generating any workflow, always add these requirements:
  1. "Pin all action versions to specific tags (not @latest)"
  2. "Add concurrency group to cancel stale runs"
  3. "Use secrets for all credentials — never hardcode"
  4. "Add timeout-minutes to each job"
  5. "Add descriptive step names"
  6. "Cache dependencies"
```

### After Generation — Review Checklist

```
After Copilot generates a workflow:
[ ] All action versions are pinned (not @latest)
[ ] No hardcoded credentials or tokens
[ ] Concurrency group present
[ ] Descriptive step names (not "Run command")
[ ] Jobs have timeout-minutes set
[ ] Secrets are properly quoted: ${{ secrets.NAME }}
[ ] Cache keys will actually hit on repeat runs
[ ] Required status checks match what the repo expects
[ ] The workflow logic is actually correct (doesn't just look right)
```

---

## 7. Revision Checklist

- [ ] Has created a `.github/instructions/github-actions.instructions.md`
- [ ] Can generate a complete CI workflow with the generation prompt
- [ ] Knows the 3 pieces to provide for debugging (YAML, error, expected behavior)
- [ ] Knows the critical rules: pin versions, no hardcoded secrets, add concurrency
- [ ] Can generate a PR quality gate workflow with parallel checks
- [ ] Knows how to review a generated workflow before committing
- [ ] Can write a secret scanning workflow
