# Copilot Command Cheatsheet

> Quick reference grouped by task. Keyboard shortcuts for macOS (swap Cmd→Ctrl for Windows/Linux).

---

## 🔑 Essential Keyboard Shortcuts

| Action | Mac | Windows/Linux |
|---|---|---|
| Accept inline suggestion | `Tab` | `Tab` |
| Reject inline suggestion | `Escape` | `Escape` |
| Next suggestion | `Alt+]` | `Alt+]` |
| Previous suggestion | `Alt+[` | `Alt+[` |
| Accept word-by-word | `Cmd+→` | `Ctrl+→` |
| Open Chat panel | `Cmd+Shift+I` | `Ctrl+Shift+I` |
| Open inline Chat | `Cmd+I` | `Ctrl+I` |
| New Chat conversation | `Cmd+L` (in Chat) | `Ctrl+L` (in Chat) |
| Send message | `Enter` | `Enter` |
| New line in message | `Shift+Enter` | `Shift+Enter` |
| Generate commit message | Click ✨ in Source Control | Click ✨ in Source Control |

---

## 📎 Context Variables (type in Chat)

| Variable | What it attaches | Best for |
|---|---|---|
| `#file:path/to/file` | Specific file content | Targeted code questions |
| `#selection` | Currently selected text | Quick operations on selected code |
| `#codebase` | Semantic search of repo index | Architecture questions, finding things |
| `#sym:SymbolName` | A specific class/function/method | Cross-file symbol analysis |
| `#editor` | Full content of active file | When you want all of the current file |
| `#terminalLastCommand` | Full output of last terminal run | Debugging CI/test failures |
| `#terminalSelection` | Selected terminal text | Targeted error from terminal output |
| `#problems` | VS Code Problems panel errors | Batch fixing type errors or lint |

---

## 🎯 Built-In Slash Commands

| Command | What it does |
|---|---|
| `/explain` | Explain selected code |
| `/fix` | Suggest fix for selected code or error |
| `/tests` | Generate unit tests for selected code |
| `/doc` | Add docstring to selected function |
| `/simplify` | Simplify selected code |
| `/new` | Scaffold a new file or project |

---

## 📚 Your Prompt Library Slash Commands

Save prompt files to `.github/prompts/*.prompt.md` — they appear here:

| Slash Command | Purpose |
|---|---|
| `/explain-code` | Deep explanation with patterns and gotchas |
| `/debug-error` | Root cause analysis + ranked fix options |
| `/generate-tests` | Comprehensive unit test suite |
| `/refactor-code` | Clean refactoring with change explanation |
| `/security-review` | OWASP-aligned severity-ranked findings |
| `/architecture-review` | SOLID + coupling + scalability evaluation |
| `/write-pr-description` | Structured PR description from diff |
| `/generate-learning-notes` | Study notes on any topic |
| `/create-github-action` | Complete GitHub Actions workflow |
| `/fix-github-action` | Debug a broken workflow |
| `/modernize-code` | Upgrade to current idioms/APIs |
| `/performance-review` | Find bottlenecks and optimizations |
| `/daily-planner` | Morning session planning |
| `/bootstrap-project` | Scaffold a new project |
| `/commit-message` | Conventional commit message |

---

## 🤖 Your Custom Agents (type @name in Chat)

| Agent | Best for |
|---|---|
| `@codebase-navigator` | "How does X work?" in unfamiliar code |
| `@debugging-tutor` | Structured error diagnosis |
| `@test-engineer` | Test generation + gap analysis |
| `@security-reviewer` | Security audit with OWASP alignment |
| `@architecture-advisor` | Design review and trade-off analysis |
| `@documentation-writer` | READMEs, docstrings, API docs, ADRs |
| `@productivity-assistant` | Session planning and task prioritization |
| `@project-builder` | New project scaffolding with Agent Mode |

---

## 🔄 Task → Mode Mapping

| Task | Best Mode | Typical Prompt Pattern |
|---|---|---|
| Understand unfamiliar code | Chat Ask + `#selection` or `#sym` | "Explain what #selection does step by step" |
| Quick bug fix (1 line) | Inline / Inline Chat (`Cmd+I`) | Select → Cmd+I → "Fix the bug where..." |
| Multi-file refactor (3-5 files) | Edits | Set working set → describe change + constraints |
| New feature (many files) | Agent Mode | Full task template: Context/Goal/Req/Constraints/Plan First |
| Generate tests | Chat / `/generate-tests` | "Generate pytest tests for #selection covering..." |
| Review before PR | Chat Ask | Run `/security-review` + `/architecture-review` |
| Debug CI failure | Chat + `#terminalLastCommand` | "Fix the failure in #terminalLastCommand" |
| Write documentation | Chat / `/generate-learning-notes` | "/doc" inline or structured prompt |
| Understand error | Chat Ask + stack trace | "Explain this error: [paste] Code: #selection" |
| Generate commit message | Source Control ✨ | Click sparkle icon in Source Control |
| Daily planning | Chat / `/daily-planner` | "Today I'm working on: [task]. Plan it." |

---

## ⚡ Power Patterns (copy-paste ready)

### Pattern: Resume yesterday's context
```
Resume context: implementing [feature].
Done: [list of completed steps]
Next: [specific next step]
Files so far: #file:path1, #file:path2
Constraint: [key rule that must not be broken]
Now: [what you want Copilot to do]
```

### Pattern: Plan before code (Agent Mode)
```
Plan only (no code yet):
1. Which files to create or modify (list exact paths)
2. Approach for each component
3. Assumptions you are making
4. Any clarifying questions before starting

Task: [your goal]
Constraints: [what must not change]
```

### Pattern: Compact refactor
```
Refactor #selection:
Goal: [one-line goal]
Keep: public API identical, existing tests pass
Show: unified diff only (no unchanged code)
```

### Pattern: Token-efficient debug
```
Error: [paste only the 3-5 relevant lines of error + stack trace]
Code: #selection [select only the failing function]
Root cause and fix — under 150 words.
```

### Pattern: Test generation
```
Tests for #selection using [framework]:
Cover: happy path, [error type 1], [error type 2], None input, empty input
Mock: [external dependency 1], [external dependency 2]
Name pattern: test_<function>_<scenario>_<expected>
```

### Pattern: Security-first review
```
Security review #selection:
Check: injection, auth bypass, PII in logs, hardcoded creds, error disclosure
Severity label for each: CRITICAL/HIGH/MEDIUM/LOW
Fix: specific code change (not generic advice)
```

---

## 🛠️ Quick Fixes for Common Issues

| Problem | Quick Fix |
|---|---|
| No inline suggestions | `Cmd+Shift+P` → "GitHub Copilot: Enable" |
| Chat not responding | Check VPN; sign out/in; reload window |
| Instructions not followed | Ask: "What instructions do you have?" to verify loading |
| Prompt file missing from `/` picker | Check: `.github/prompts/` location + `.prompt.md` extension |
| Agent not in `@` picker | Check: `.github/agents/` location + `.agent.md` extension |
| Agent Mode wrong changes | Stop → `git checkout .` → re-prompt with tighter constraints |
| Context window exceeded | `Cmd+L` to start new chat; summarize previous context |
| Hallucinated API method | Check docs; tell Copilot: "X doesn't exist, correct is Y" |

---

## 📋 Pre-PR Checklist (quick version)

```
[ ] /security-review on changed files — no CRITICAL or HIGH unfixed
[ ] /generate-tests run for new code — tests pass
[ ] /write-pr-description generated and reviewed
[ ] Full test suite passes
[ ] No hardcoded values, no PII in logs, no shell=True with user input
[ ] Diff reviewed — no unexpected changes, no deleted error handling
```

---

## 🔁 Git Commands for Copilot Workflows

```bash
# Checkpoint before Agent Mode
git add . && git commit -m "checkpoint: before agent mode - [task]"

# Recover from bad Agent Mode session
git checkout .

# See all changes Agent Mode made
git diff

# Copilot-generated commit message
# Click ✨ in VS Code Source Control panel

# Tag before major AI-assisted refactoring
git tag pre-refactor-[date]

# Compare working tree to last commit (what changed this session)
git diff HEAD

# Stash when you need to try an Agent Mode idea without committing
git stash && git stash pop
```

---

## 🔒 Safety Reminders (never skip these)

```
NEVER paste into Copilot:
  ✗ API keys, tokens, passwords, private keys
  ✗ Real customer emails, names, SSNs, payment data
  ✗ .env file contents with real values
  ✗ Database connection strings with passwords

ALWAYS before accepting:
  ✓ Read the diff
  ✓ Run tests
  ✓ Check new imports
  ✓ Verify generated commands before running

ALWAYS before Agent Mode:
  ✓ git add . && git commit -m "checkpoint: ..."
```
