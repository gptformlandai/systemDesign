# OpenAI Codex CLI — Command Cheat Sheet

> Quick reference for all Codex CLI commands, flags, and patterns.
> Bookmark this. Return to it daily.

---

## Installation & Setup

```bash
# Install
npm install -g @openai/codex

# Set API key (add to .bashrc / .zshrc / PowerShell profile)
export OPENAI_API_KEY="sk-..."

# Verify installation
codex --version
codex --help

# Global config file location
~/.codex/config.yaml       # Linux/macOS
%USERPROFILE%\.codex\config.yaml   # Windows
```

---

## Config File (~/.codex/config.yaml)

```yaml
model: o4-mini              # default model
approval_policy: suggest    # suggest | auto-edit | full-auto
notify: true                # desktop notifications on completion
```

---

## Running Codex

```bash
# Interactive mode (REPL)
codex

# Non-interactive — single task
codex "add input validation to create_user endpoint"

# Non-interactive with specific model
codex --model gpt-4.1 "refactor the auth module"

# Non-interactive with approval policy
codex --approval-policy auto-edit "generate tests for user_service.py"
codex --approval-policy full-auto "scaffold the payments module"

# With a system prompt override
codex --system-prompt "You are a security-focused code reviewer" "review auth.py"

# Quiet mode (no interactive prompts)
codex --quiet "fix the failing tests"
```

---

## Approval Policy Reference

| Policy | File Edits | Command Execution | Best For |
|--------|-----------|-------------------|----------|
| `suggest` | Propose only | Propose only | Learning, sensitive code, first run on new codebase |
| `auto-edit` | Automatic | Must approve | Day-to-day development (recommended default) |
| `full-auto` | Automatic | Automatic | Bounded autonomous tasks after git checkpoint |

```bash
# Safe progression
git add -A ; git commit -m "checkpoint"   # always before full-auto
codex --approval-policy full-auto "task"
git diff                                  # review everything after
```

---

## Interactive Mode Commands

```
/help           — show available commands
/compact        — compress conversation history (reduces token usage)
/quit           — exit interactive mode
/clear          — clear current context

# Multiline input
\               — continue prompt on next line (backslash at end of line)
```

---

## Model Selection Guide

| Task | Recommended Model | Why |
|------|------------------|-----|
| Explaining code, quick fixes | `o4-mini` | Fast, low cost |
| Test generation, debugging | `o4-mini` | Sufficient reasoning |
| Architecture, complex refactoring | `gpt-4.1` | Stronger planning |
| Documentation | `gpt-4.1-mini` | Balanced quality/cost |
| Long context analysis (entire codebase) | `gpt-4.1` | Largest context window |

```bash
codex --model gpt-4.1 "design the caching layer for the API"
codex --model o4-mini "generate tests for user.py"
```

---

## AGENTS.md Reference

```markdown
# AGENTS.md — place in project root or any subfolder

## Project Context
[What this codebase is and its main components]

## Tech Stack
[Language versions, frameworks, key libraries]

## Coding Standards
[Naming, error handling, patterns to follow]

## Testing
[Framework, test command, what to test, what to mock]

## Forbidden Actions
- NEVER commit database migration files
- NEVER run `rm -rf` without explicit user approval
- NEVER modify .env files
- NEVER log PII

## Architecture Rules
[Key architectural decisions that must be respected]

## Verification Command
[Command that proves the task is done: e.g. `pytest -x`]
```

---

## Common Prompt Patterns

```bash
# Explain before touching
codex "Explain what user_service.py does and its dependencies. Do not make changes."

# Plan first
codex "Plan the implementation of user pagination. List steps and files affected. Do not implement."

# Implement with verification
codex --approval-policy auto-edit \
  "Add pagination to GET /users. Verify with: pytest tests/test_user_api.py -x"

# Test generation
codex --approval-policy auto-edit \
  "Generate tests for user_service.py covering: happy path, empty input, invalid type, unauthorized"

# Security review
codex "Review auth.py for OWASP vulnerabilities. Report: severity, attack vector, fix for each finding."

# Pre-commit review
codex "Review all staged changes for: logic errors, missing tests, security issues, convention violations."
```

---

## CI / Scripted Usage

```bash
# In Makefile
codex-test:
	codex --approval-policy auto-edit --quiet \
	  "Generate tests for any file with no corresponding test file. Run pytest and fix failures."

# In shell script
#!/bin/bash
set -e
git add -A
git commit -m "checkpoint: pre-codex"
codex --approval-policy full-auto "$1"
pytest -x
git diff --stat HEAD~1
```

---

## Safety Rules (Always Apply)

```
1. NEVER put API keys, passwords, or secrets in Codex prompts
2. NEVER run full-auto without a prior git commit checkpoint
3. ALWAYS review the full diff before committing from a full-auto session
4. ALWAYS specify a verification command for agent tasks
5. NEVER include real PII (names, emails, IDs) in prompts — use synthetic data
6. On new codebases: always start with suggest mode, then escalate
```

---

## Debugging Codex

```bash
# If Codex seems confused about the codebase:
codex "Summarize the project structure and main components. Do not make changes."
# → Use this output to verify Codex understood correctly before proceeding

# If full-auto made unexpected changes:
git diff                    # review everything
git checkout -- .           # discard all unstaged changes
git reset HEAD~1 --soft     # undo last commit (keeps changes staged)

# If Codex is running too slow:
# Switch to o4-mini for the task
codex --model o4-mini "task"

# If output is too verbose:
# Add to your prompt: "Be concise. Output: [specify format]"
```
