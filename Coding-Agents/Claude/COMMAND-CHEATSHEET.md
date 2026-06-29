# Claude Command Cheatsheet

> Quick reference grouped by task. Everything you need for daily Claude use.

---

## 🔑 Claude Code CLI Essentials

```bash
# Install Claude Code
npm install -g @anthropic-ai/claude-code

# Start an interactive session in a project
claude

# Run a one-shot task
claude "Explain the architecture of this project"

# Run with a specific file in context
claude --file src/services/user_service.py "Refactor this to use the repository pattern"

# Run non-interactively (for CI/scripts)
claude --no-interactive "Run tests and fix any failures"

# Start with a specific model
claude --model claude-opus-4-5

# Print mode (output to stdout, no interactivity)
claude --print "Generate a summary of recent changes"

# Resume last session
claude --continue

# Check version and config
claude --version
claude config
```

---

## 📎 Context Variables (Claude Code)

| Variable | What it references | Best for |
|---|---|---|
| `@file:path/to/file` | Specific file content | Targeted analysis or edit |
| `@src/` | Directory tree | Architecture questions |
| `$(cat file)` | Shell: embed file content | Scripted workflows |
| Git context | Recent commits, diffs | Review and summary tasks |
| CLAUDE.md | Auto-loaded | Persistent project rules |

---

## 🗂️ CLAUDE.md Quick Reference

```bash
# Check CLAUDE.md is being read:
claude "What project rules do you have loaded?"

# CLAUDE.md location:
# Root:      ./CLAUDE.md        ← project-wide rules
# Subfolder: ./src/CLAUDE.md   ← overrides for this directory
# Home:      ~/.claude/CLAUDE.md ← personal defaults for all projects

# CLAUDE.md sections (all optional, only add what changes behavior):
# - ## Project Overview
# - ## Tech Stack
# - ## Architecture Rules
# - ## Coding Conventions
# - ## Testing Requirements
# - ## Do NOT
```

---

## ⚡ Slash Commands (type in Claude Code)

| Command | Purpose | Location |
|---|---|---|
| `/explain` | Deep code explanation with patterns | `.claude/commands/explain.cmd.md` |
| `/debug` | Root cause analysis + fix options | `.claude/commands/debug.cmd.md` |
| `/refactor` | Clean refactoring with diff | `.claude/commands/refactor.cmd.md` |
| `/test` | Comprehensive test generation | `.claude/commands/test.cmd.md` |
| `/review` | Security + quality + test coverage | `.claude/commands/review.cmd.md` |
| `/plan` | Session planning before coding | `.claude/commands/plan.cmd.md` |
| `/optimize` | Performance + token efficiency | `.claude/commands/optimize.cmd.md` |
| `/build` | Scaffold new project/feature | `.claude/commands/build.cmd.md` |
| `/token-efficient` | Rewrite prompt for efficiency | `.claude/commands/token-efficient.cmd.md` |

---

## 🤖 Subagents

| Invoke with | Purpose |
|---|---|
| `Use the @planner agent` | Session and feature planning |
| `Use the @builder agent` | Implementation specialist |
| `Use the @debugger agent` | Structured error diagnosis |
| `Use the @tester agent` | Test generation + gap analysis |
| `Use the @reviewer agent` | Code review specialist |
| `Use the @architect agent` | Architecture planning |
| `Use the @optimizer agent` | Performance specialist |

---

## 🔄 Task → Mode Mapping

| Task | Best Approach | Pattern |
|---|---|---|
| Understand unfamiliar code | Claude Code + `/explain` | `/explain @file:complex_file.py` |
| Fix a bug (obvious cause) | Claude Code one-shot | "Fix: [error]. Code: @file:x.py. Diff only." |
| Fix a bug (subtle) | New session + `/debug` | `/debug` with error + relevant file |
| Generate tests | Claude Code + `/test` | `/test @file:service.py` |
| Multi-file refactor | Agent session + plan-first | Plan → approve → execute |
| New feature end-to-end | Multi-subagent pipeline | planner → builder → tester → reviewer |
| Architecture review | `/review` or @architect | `/review` on key service files |
| PR review | `/review` on diff | Focus security + tests + quality |
| Learn a concept | Chat one-shot | "Explain [concept]. Format: [template]." |
| Daily planning | `/plan` | "Today I'm working on: [task]." |
| Codebase onboarding | Claude Code + #codebase | "5-bullet architecture summary. Under 150 words." |

---

## ⚡ Power Patterns (copy-paste ready)

### Plan-first for any agent session
```
Plan only (no code yet):
1. Files to create or modify (exact paths)
2. One-sentence change per file
3. Dependencies (what must exist before what)
4. Assumptions you're making

Task: [goal]
Wait for my approval before writing any code.
```

### Verification loop
```
[After Claude generates code]
"Run the tests. Fix any failures. Iterate until all tests pass.
After each iteration: show me the test output before fixing."
```

### Token-efficient debug
```
"Error: [3-5 relevant lines only]
Code: @file:[failing function file]
Root cause + fix. Under 100 words. Diff only."
```

### Subagent handoff
```
"Hand off to the @tester agent.
Context: I implemented [feature] in [files].
Task: generate comprehensive tests.
Constraint: mock all external dependencies."
```

### Resume pattern (between sessions)
```
"Resume: implementing [feature].
Done: [list of completed files]
Files created: @file:f1, @file:f2
Next: [specific next step]
Constraint: [key rule]
Now: [specific action]"
```

### Scope-limited refactor
```
"Refactor @file:[target].
Goal: [one sentence]
Keep: public API identical, tests pass
Do NOT: change any other file, add new dependencies
Output: unified diff only"
```

---

## 🛠️ CLAUDE.md Template (minimum viable)

```markdown
# [Project Name]

## What This Is
[2-3 sentences: what it does, current state]

## Stack
- Language: [version]
- Framework: [version]
- Database: [version]
- Testing: [framework]

## Architecture Rules
- [Rule 1]
- [Rule 2]

## Do NOT
- [Antipattern 1]
- [Antipattern 2]
```

---

## 🔒 Hook Quick Reference

```bash
# Hooks location: .claude/hooks/
# pre_tool_use.sh   — runs before every tool call
# post_tool_use.sh  — runs after every tool call
# on_error.sh       — runs when Claude encounters an error

# Check hooks are loaded:
cat .claude/hooks/pre_tool_use.sh

# Common pre_tool_use pattern:
#!/bin/bash
TOOL=$1
# Block dangerous commands
if [[ "$TOOL" == *"rm -rf"* ]] || [[ "$TOOL" == *"DROP TABLE"* ]]; then
    echo "BLOCKED: Potentially destructive command"
    exit 1
fi
exit 0
```

---

## 🔁 Git Commands for Claude Workflows

```bash
# Checkpoint before any agent session
git add . && git commit -m "checkpoint: before claude agent - [task]"

# See all changes Claude made in the session
git diff HEAD

# Interactive staging — read each hunk before staging
git add -p

# Recover from bad agent session
git checkout .

# Tag before major autonomous refactoring
git tag pre-claude-refactor-$(date +%Y%m%d)

# Review what Claude changed in last session
git diff HEAD~1

# Stash before trying an experimental Claude approach
git stash
```

---

## 🔒 Safety Reminders

```
NEVER paste into Claude:
  ✗ API keys, tokens, passwords, private keys
  ✗ Real customer emails, names, SSNs, payment data
  ✗ .env file contents with real values
  ✗ Production database connection strings

ALWAYS before accepting:
  ✓ Read the diff (git diff)
  ✓ Run tests
  ✓ Check new imports
  ✓ Verify any shell commands before running

ALWAYS before agent sessions:
  ✓ git add . && git commit -m "checkpoint: ..."
  ✓ Scope the task explicitly (what to change and what NOT to)
```

---

## 📋 Pre-Commit Checklist (quick)

```
[ ] /review on staged changes — no CRITICAL/HIGH unfixed
[ ] Tests pass (run the test command)
[ ] No hardcoded values, PII, or credentials in diff
[ ] Diff reviewed line by line (git diff --staged)
[ ] Commit message follows conventional format
[ ] I can explain every line Claude wrote
```
