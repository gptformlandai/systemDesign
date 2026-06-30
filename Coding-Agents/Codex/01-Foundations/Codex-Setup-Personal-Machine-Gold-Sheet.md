# Codex Setup — Personal Machine Gold Sheet

> **Track**: Codex Mastery Track — Group 1: Foundations
> **File**: 2 of 6 (Track File #2)
> **Audience**: Developers installing Codex for the first time
> **Read after**: Codex-Mental-Model-Gold-Sheet.md

---

## Practical Impact Meter

| Topic | Impact | Why Developers Miss This |
|---|---|---|
| API key setup and security | ★★★★★ | Devs hardcode keys or skip .gitignore; key leaks are immediate security incidents |
| Global config file (~/.codex/config.yaml) | ★★★★★ | Without it, you set --model and --approval-policy flags on every command |
| AGENTS.md in the project root | ★★★★★ | Without it, every Codex session starts with no project context |
| Verification — confirming Codex works correctly | ★★★★☆ | Skipping verification means you discover broken setup during real tasks |
| Model selection defaults | ★★★☆☆ | Defaulting to gpt-4.1 on every task burns 5-10x more cost than o4-mini for same quality |

---

## ⭐ Beginner Tier — Start Here

### B1: The minimal working setup (10 minutes)

```bash
# Step 1: Install Node.js if not present (https://nodejs.org)
node --version    # should be 18+

# Step 2: Install Codex CLI
npm install -g @openai/codex

# Step 3: Verify installation
codex --version

# Step 4: Set your API key (get from platform.openai.com)
export OPENAI_API_KEY="sk-..."
# On Windows PowerShell:
$env:OPENAI_API_KEY = "sk-..."

# Step 5: Test it works
cd /path/to/any/project
codex "List the main files in this project and what each one does. Make no changes."
```

If you see a structured description of your project: setup is working.

### B2: Make the API key permanent

```bash
# Linux / macOS — add to ~/.bashrc or ~/.zshrc
echo 'export OPENAI_API_KEY="sk-..."' >> ~/.bashrc
source ~/.bashrc

# Windows PowerShell — add to profile
notepad $PROFILE
# Add: $env:OPENAI_API_KEY = "sk-..."
# Save and restart PowerShell

# Verify it persists after restart
codex "say hello"
```

---

## 1. Installation

### Prerequisites

```bash
# Node.js 18 or later required
node --version    # >= 18.0.0
npm --version     # >= 9.0.0

# Install
npm install -g @openai/codex

# Upgrade later
npm update -g @openai/codex
```

### Verify installation

```bash
codex --version
codex --help
```

---

## 2. API Key Configuration

### Get your key

```
1. Go to: platform.openai.com
2. Top-right menu → API keys
3. Create new secret key
4. Copy immediately — it won't be shown again
```

### Set the key

```bash
# Temporary (current session only)
export OPENAI_API_KEY="sk-proj-..."

# Permanent — Linux / macOS
echo 'export OPENAI_API_KEY="sk-proj-..."' >> ~/.bashrc
source ~/.bashrc

# Permanent — Windows PowerShell
# In $PROFILE:
$env:OPENAI_API_KEY = "sk-proj-..."

# Permanent — Windows System Environment Variables (GUI)
# Control Panel → System → Advanced → Environment Variables
# Add: OPENAI_API_KEY = sk-proj-...
```

### Security rules for API keys

```
✅ DO:
  - Store in environment variable or password manager
  - Rotate the key if you ever paste it anywhere by accident
  - Use separate keys for development vs production

❌ NEVER:
  - Put the key in a file that gets committed (AGENTS.md, .env without .gitignore)
  - Share the key in Slack, email, or any chat
  - Include the key in a Codex prompt ("my API key is sk-...")
  - Commit a file that has the key anywhere in it

.gitignore must include:
  .env
  .env.local
  *.key
  secrets/
```

---

## 3. Global Configuration File

Create `~/.codex/config.yaml` (Linux/macOS) or `%USERPROFILE%\.codex\config.yaml` (Windows):

```yaml
# ~/.codex/config.yaml

# Default model for all tasks (override with --model flag)
model: o4-mini

# Default approval policy (override with --approval-policy flag)
# suggest: propose only, you approve everything
# auto-edit: apply file edits automatically, ask before commands
# full-auto: fully autonomous (use only with git checkpoint)
approval_policy: auto-edit

# Desktop notifications when a task completes
notify: true
```

### Model guidance for config default

```
Use o4-mini as default — it handles 90% of tasks.
Override to gpt-4.1 for: architecture work, complex multi-file refactors,
  tasks where you got wrong results with o4-mini.

Cost ratio: gpt-4.1 costs roughly 5-10x more per token than o4-mini.
```

---

## 4. Project-Level Setup — AGENTS.md

Create `AGENTS.md` in your project root:

```markdown
# AGENTS.md

## Project
[Name and purpose of this project]

## Tech Stack
- Language: Python 3.11 (or your stack)
- Framework: FastAPI (or your framework)
- Database: PostgreSQL via SQLAlchemy
- Tests: pytest

## Coding Standards
- Functions: snake_case
- Classes: PascalCase
- Errors: raise HTTPException (never raw Python exceptions in API layer)
- SQL: always use parameterized queries (never string interpolation)
- Logging: use structlog — never log PII

## Testing
- Framework: pytest
- Run: pytest -x
- Mock: external dependencies only (HTTP clients, DB, filesystem)
- Coverage target: 80% on new code

## Forbidden Actions
- NEVER run database migrations without explicit confirmation
- NEVER modify .env files
- NEVER install new packages without listing them first
- NEVER log passwords, API keys, or user PII

## Verification
Run `pytest -x` to verify any implementation task is complete.
```

---

## 5. Verification Protocol — Confirm Everything Works

Run each step and confirm it succeeds before moving on:

```bash
# Step 1: API connection
codex "Say 'Codex is ready'. No other output."
# Expected: "Codex is ready"

# Step 2: File reading
cd your-project
codex "List the top 5 files by importance. No changes."
# Expected: a list of your project's key files

# Step 3: auto-edit mode
# Use a throwaway file for this test
echo "def greet(name): pass" > /tmp/test_codex.py
cd /tmp
codex --approval-policy auto-edit \
  "Implement greet() to return 'Hello, {name}!'. File: test_codex.py"
cat test_codex.py
# Expected: implemented function
rm test_codex.py

# Step 4: AGENTS.md loading
cd your-project  # must have AGENTS.md
codex "What are the coding standards for this project? No changes."
# Expected: Codex reads back your AGENTS.md coding standards
```

---

## 6. Multiple Projects Setup

### Per-project AGENTS.md

Every project should have its own AGENTS.md. The global config sets defaults;
AGENTS.md provides project context.

```
project-A/
  AGENTS.md    ← Python/FastAPI project rules
project-B/
  AGENTS.md    ← TypeScript/Express project rules
project-C/
  AGENTS.md    ← Go/Gin project rules
```

### Subfolder AGENTS.md for domain overrides

```
project/
  AGENTS.md              ← root: general project rules
  src/api/AGENTS.md      ← API layer: REST conventions, auth rules
  src/db/AGENTS.md       ← DB layer: migration safety rules
  tests/AGENTS.md        ← testing: what to mock, naming conventions
```

---

## 7. Useful Aliases

```bash
# Add to ~/.bashrc or ~/.zshrc

# Safe mode (suggest — shows everything before applying)
alias codex-safe='codex --approval-policy suggest'

# Edit mode (auto-edit — applies file changes, asks before commands)
alias codex-edit='codex --approval-policy auto-edit'

# Power mode (full-auto — requires prior git checkpoint)
alias codex-auto='codex --approval-policy full-auto'

# Architecture mode (gpt-4.1 for complex planning)
alias codex-arch='codex --model gpt-4.1 --approval-policy suggest'
```

---

## Production Pitfalls

```
PITFALL: API key in AGENTS.md
  - Never put your OPENAI_API_KEY in AGENTS.md. It gets read by Codex's context.
  - AGENTS.md is often in version control. Keys committed to git are compromised.

PITFALL: No git checkpoint before full-auto
  - Full-auto can modify many files. Without a checkpoint, recovery is hard.
  - Rule: git add -A && git commit -m "checkpoint" BEFORE every full-auto session.

PITFALL: No AGENTS.md, generic output
  - Without AGENTS.md, Codex generates code following its training defaults.
  - These may conflict with your team's naming, error handling, or test conventions.

PITFALL: Using gpt-4.1 for every task
  - o4-mini handles ~90% of tasks at 5-10x lower cost.
  - Reserve gpt-4.1 for: architecture, complex multi-file planning, tasks where o4-mini fails.
```

---

## Interview Traps

```
TRAP: "I'll configure AGENTS.md later once I understand Codex better"
TRUTH: AGENTS.md on Day 1 is the highest-ROI setup action. Without it, Codex applies
       generic defaults that conflict with your project's conventions — wrong error types,
       wrong naming, wrong test patterns. Even a 10-line AGENTS.md is better than none.

TRAP: "One global config works for all projects"
TRUTH: Global config sets defaults. A production backend and a side-project CLI may need
       different approval policies and models. Override per-session with --model and
       --approval-policy flags, or use per-project shell aliases.

TRAP: "I can add the API key to AGENTS.md so Codex has the right context"
TRUTH: AGENTS.md is version-controlled. API keys in AGENTS.md get committed to git history.
       An exposed key must be rotated immediately. Store API keys in environment variables only.
```

---

## Revision Checklist

- [ ] Codex CLI installed and `codex --version` returns a version number
- [ ] API key set permanently in shell profile
- [ ] Global config.yaml created with model and approval_policy
- [ ] AGENTS.md created for at least one active project
- [ ] All 4 verification steps passed
- [ ] API key is NOT in any file that could be committed
